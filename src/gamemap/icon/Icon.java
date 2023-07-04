package gamemap.icon;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

public class Icon {
	public static final int STANDARD_RENDERED_SIZE = 24;

	private static boolean canBlend(int[] px) {
		int[] comp = new int[3];
		for(int pixel : px) {
			if(pixel >>> 24 == 0) continue;

			for(int i = 0; i < 3; i++) {
				comp[i] = (pixel >>> (i * 8)) & 0xFF;
			}
			int avg = (comp[0] + comp[1] + comp[2]) / 3;
			// allow for a slight bit of toleance with blending
			for(int i = 0; i < 3; i++) {
				if(Math.abs(comp[0] - avg) > 8) return false;
			}
		}
		return true;
	}
	
	private static int getInsetAmount(int[] px, int first,
			int minorChange, int majorChange, int minorCount, int majorCount)
	{
		for(int y = 0; y < majorCount; y++) {
			int base = first + majorChange * y;
			for(int x = 0; x < minorCount; x++) {
				int p = px[base + x * minorChange];
				if(p >>> 24 != 0) return y;
			}
		}
		// reaching this point means the image was empty
		return 0;
	}
	
	transient BufferedImage	sprite;
	transient BufferedImage croppedSprite;
	transient boolean		croppedWithMargin;
	
	Insets					sprite_insets;
	Point					sheet_pos = new Point();
	/** offset of center in relation to uncropped version of original, standalone image */
	Point					center_offset = new Point();
	Dimension				render_size;
	boolean					blend;
	boolean					has_stem;
	/** whether or no to put a 1px margin around the image for linear filtering purposes */
	boolean					pad_image = true;
	/** don't include this icon in the list of selectable ones */
	boolean					hidden;
	String					id;
	/** friendly name to show in the icon list */
	// could technically have null/empty = hidden, but that's less clear
	String					name;
	File					path;
	String					filter;
	
	Icon() {}
	
	Icon(File f) {
		path = f;
	}
	
	void loadImage() {
		try {
			sprite = ImageIO.read(path);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	void cropImage() {
		croppedWithMargin = hasInsetsOnAllSides();
		Dimension dim = getSpriteSize();
		if(croppedWithMargin) {
			croppedSprite = sprite.getSubimage(sprite_insets.left - 1,
					sprite_insets.top - 1, dim.width + 2, dim.height + 2);
		} else {			
			croppedSprite = sprite.getSubimage(sprite_insets.left,
					sprite_insets.top, dim.width, dim.height);
		}
	}
	
	Dimension getSpriteSize() {
		return new Dimension(
				sprite.getWidth() - (sprite_insets.left + sprite_insets.right),
				sprite.getHeight() - (sprite_insets.top + sprite_insets.bottom));
	}
	
	Dimension getSpriteSizeWithMargin() {
		Dimension size = getSpriteSize();
		if(pad_image) {
			size.width += 2;
			size.height += 2;
		}
		return size;
	}
	
	Rectangle getBounds() {
		Dimension size = getSpriteSize();
		return new Rectangle(sheet_pos.x, sheet_pos.y, size.width, size.height);
	}
	
	Rectangle getBoundsWithMargin() {
		Rectangle r = getBounds();
		if(pad_image) {
			r.x--;
			r.y--;
			r.width += 2;
			r.height += 2;
		}
		return r;
	}
	
	boolean hasInsetsOnAllSides() {
		return sprite_insets.top != 0 && sprite_insets.bottom != 0 &&
				sprite_insets.left != 0 && sprite_insets.right != 0;
	}
	
	boolean hasInsetOnAnySide() {
		return sprite_insets.top != 0 || sprite_insets.bottom != 0 ||
				sprite_insets.left != 0 || sprite_insets.right != 0;
	}
	
	void generateDefaultPropertiesFromImage() {
		// shittiest attempt at auto-generating names and ids
		String tmpName = path.getName();
		int idx = tmpName.lastIndexOf('.');
		if(idx != -1) tmpName = tmpName.substring(0, idx);
		name = tmpName;
		id = tmpName.toLowerCase().replace(' ', '_');
		
		final int w = sprite.getWidth();
		final int h = sprite.getHeight();
		int[] px = sprite.getRGB(0, 0, w, h, null, 0, w);
		blend = canBlend(px);
		int left = getInsetAmount(px, 0, w, 1, h, w);
		int top = getInsetAmount(px, 0, 1, w, w, h);
		int right = getInsetAmount(px, w - 1, w, -1, h, w);
		int btm = getInsetAmount(px, w * (h - 1), 1, -w, w, h);
		sprite_insets = new Insets(top, left, btm, right);
		cropImage();
	}
	
	Dimension determineDefaultSize(Iterable<Icon> icons) {
		Dimension thisSize = getSpriteSize();
		int defaultSize = -1;
		for(Icon icon : icons) {
			Dimension size = icon.getSpriteSize();
			if(thisSize.equals(size)) {
				return new Dimension(icon.render_size);
			}
			if(icon.render_size.width == STANDARD_RENDERED_SIZE &&
					icon.render_size.height == STANDARD_RENDERED_SIZE)
			{
				defaultSize = size.width;
			}
		}
		if(defaultSize == -1) {
			defaultSize = Math.min(thisSize.width, thisSize.height);
		}
		return new Dimension(thisSize.width * STANDARD_RENDERED_SIZE / defaultSize,
				thisSize.height * STANDARD_RENDERED_SIZE / defaultSize);
	}
	
	void setDefaultInitialPos(RenderPane pane, Iterable<Icon> icons, int maxWidth) {
		Rectangle r = new Rectangle(getSpriteSizeWithMargin());
		List<Rectangle> bounds = new ArrayList<>();
		for(Icon icon : icons) {
			bounds.add(icon.getBoundsWithMargin());
		}
		List<Point> points = pane.getSnapPoints(this)[RenderPane.SNAP_TOP_LEFT];
		Collections.sort(points, (Point a, Point b) -> {
			int dif = a.y - b.y;
			if(dif == 0) dif = a.x - b.x;
			return dif;
		});
		outer:
		for(Point p : points) {
			r.setLocation(p);
			if(r.x + r.width > maxWidth) continue;
			
			for(Rectangle bound : bounds) {
				if(bound.x < 0 || bound.y < 0) continue;
				if(bound.intersects(r)) continue outer;
			}
			sheet_pos.x = r.x;
			sheet_pos.y = r.y;
			if(pad_image) {
				sheet_pos.x++;
				sheet_pos.y++;
			}
			break;
		}
	}
	
	@Override
	public String toString() {
		return id + ": " + name;
	}
}