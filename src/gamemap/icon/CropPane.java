package gamemap.icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class CropPane extends PaneBase {
	Icon icon;
	
	public CropPane() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3) {
					cycleBackgroundColor();
				}
			}
		});
		setMinimumSize(new Dimension(0, 0));
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		if(icon == null) return;

		// determine scaling
		BufferedImage img = icon.sprite;
		int ow = img.getWidth();
		int oh = img.getHeight();
		int iw = ow;
		int ih = oh;
		int pw = getWidth();
		int ph = getHeight();
		float sw = pw / (float) iw;
		float sh = ph / (float) ih;
		float scale = Math.min(sw, sh);
		
		iw *= scale;
		ih *= scale;
		int x = (pw - iw) / 2;
		int y = (ph - ih) / 2;
		
		// draw the uncropped sprite
		g.drawImage(img, x, y, iw, ih, null);
		
		Color lineColor = Color.MAGENTA.equals(getBackground()) ? Color.ORANGE : Color.RED;
		g.setColor(lineColor);
		
		// draw cropping box 
		Insets margin = icon.sprite_insets;
		int rx = (int) (margin.left * scale) + x;
		int ry = (int) (margin.top * scale) + y;
		int rw = (int) ((ow - (margin.left + margin.right)) * scale);
		int rh = (int) ((oh - (margin.top + margin.bottom)) * scale);
		g.drawRect(rx, ry, rw, rh);

		// draw center
		int cx = x + (int) ((ow / 2 + icon.center_offset.x) * scale);
		int cy = y + (int) ((oh / 2 + icon.center_offset.y) * scale);
		int ext = Math.round(2 * scale);
		g.drawLine(cx - ext, cy, cx + ext, cy);
		g.drawLine(cx, cy - ext, cx, cy + ext);
	}
	
	public Dimension getPreferredSize() {
		if(icon == null) return super.getPreferredSize();
		int w = icon.sprite.getWidth();
		int h = icon.sprite.getHeight();
		int scale = 192 / w;
		if(scale < 0) scale = 1;
		return new Dimension(10, h * scale);
	}
}