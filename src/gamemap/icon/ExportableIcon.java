package gamemap.icon;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;

public class ExportableIcon {
	private static boolean isCenterOffsetStillCentered(Icon icon) {
		Point off = icon.center_offset;
		Insets margin = icon.sprite_insets;
		// positive offset moves down/right
		// setup table
		//	top	btm	off	centered
		//	2	0	1	y			off + (btm - top) / 2
		//	2	2	0	y
		boolean centeredX = (off.x + (margin.right - margin.left) / 2) == 0;
		boolean centeredY = (off.y + (margin.bottom - margin.top) / 2) == 0;
		return centeredX && centeredY;
	}
	
	String		id;
	Point		sheet_pos	= new Point();
	Dimension	sheet_size	= new Dimension();
	Boolean		blend		= false;
	Boolean		has_stem	= false;
	Point		render_offset;
	Dimension	render_size;
	String		name;
	String		filter;
	Boolean		hidden;
	
	public ExportableIcon(Icon icon) {
		id = icon.id;
		sheet_pos = icon.sheet_pos;
		sheet_size = icon.getSpriteSize();
		blend = icon.blend;
		has_stem = icon.has_stem;
		
		render_size = icon.render_size;
		name = icon.name;
		filter = icon.filter;
		hidden = icon.hidden;

		// done next to last so we can make use of other set variables
		// before they're possibly wiped by minification
		if(!isCenterOffsetStillCentered(icon)) {
			setRenderOffset(icon);
		}

		minify();
	}
	
	private void setRenderOffset(Icon icon) {
		// icon.center_offset is the offset of the center 
		// from the uncropped standalone image in image-space coordinates
		// render_offset should be the offset of the hotspot
		// from the top left corner of the icon in render-space coordinates
		
		Point off = icon.center_offset;
		Insets margin = icon.sprite_insets;
		int unscaledX = icon.sprite.getWidth() / 2 + off.x - margin.left;
		int unscaledY = icon.sprite.getHeight() / 2 + off.y - margin.top;

		int scaledX = unscaledX;
		int scaledY = unscaledY;
		// we'd probably be fine without the equal size check
		// but it doesn't hurt to skip the math anyways
		// this whole bit is only done once per icon on export
		// also, we're trying to keep coordinate values as integers
		if(sheet_size.width != render_size.width) {
			scaledX = scaledX * render_size.width / sheet_size.width;
		}
		if(sheet_size.height != render_size.height) {
			scaledY = scaledY * render_size.height / sheet_size.height;
		}
		render_offset = new Point(scaledX, scaledY);
	}
	
	public void minify() {
		if(blend == false) blend = null;
		if(has_stem == false) has_stem = null;
		if(sheet_size.equals(render_size)) render_size = null;
		if(hidden == false) hidden = null;
	}
}