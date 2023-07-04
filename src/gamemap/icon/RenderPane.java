package gamemap.icon;

import java.awt.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;

public class RenderPane extends PaneBase {
	private static final Color	COLOR_DRAG_ORIGINAL	= new Color(0x800000FF, true);
	private static final Color	COLOR_DRAG_CURRENT	= new Color(0x8000CC00, true);
	private static final Color	COLOR_OVERLAP		= new Color(0x80FF0000, true);
	
	public static final int		SNAP_TOP_LEFT		= 0;
	public static final int		SNAP_TOP_CENTER		= 1;
	public static final int		SNAP_TOP_RIGHT		= 2;
	public static final int		SNAP_CENTER_LEFT	= 3;
	public static final int		SNAP_CENTER_RIGHT	= 4;
	public static final int		SNAP_BOTTOM_LEFT	= 5;
	public static final int		SNAP_BOTTOM_CENTER	= 6;
	public static final int		SNAP_BOTTOM_RIGHT	= 7;

	BufferedImage				img;
	IterableListModel<Icon>		icons;
	private Icon				draggingIcon;
	private Point2D.Double		draggingOffset		= new Point2D.Double();
	private Point				draggingPos			= new Point();
	private double				imgScale			= 1;
	private boolean				renderDragging		= false;
	Color						imgBackground		= new Color(0, true);

	public RenderPane() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON3) {
					cycleBackgroundColor();
				} else if(e.getButton() == MouseEvent.BUTTON1) {
					Icon icon = getIconAtPos(e.getX(), e.getY());
					if(icon != null) {
						IconProjEditor.iconTab.setIcon(icon);
						IconProjEditor.iconList.setSelectedValue(icon, true);
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					Icon icon = getIconAtPos(e.getX(), e.getY());
					if(icon != null) {
						draggingIcon = icon;
						draggingPos.setLocation(icon.sheet_pos);
						draggingOffset.setLocation(e.getX() / imgScale - draggingPos.x,
								e.getY() / imgScale - draggingPos.y);
						repaint();
					}
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1 && draggingIcon != null) {
					if(!draggingPos.equals(draggingIcon.sheet_pos) &&
							draggingPos.x >= 0 && draggingPos.y >= 0)
					{
						Point oldPos = new Point(draggingIcon.sheet_pos);
						int changedCount = 0;
						Icon lastChanged = null;
						draggingIcon.sheet_pos.setLocation(draggingPos);
						Rectangle bounds = draggingIcon.getBoundsWithMargin();
						for(Icon icon : icons) {
							if(icon == draggingIcon) continue;
							Rectangle r = icon.getBoundsWithMargin();
							if(bounds.intersects(r)) {
								lastChanged = icon;
								changedCount++;
								icon.sheet_pos.setLocation(-1, -1);
							}
						}
						
						boolean updateComplex = true;
						// see if we can just swap the positions of the two icons
						if(changedCount == 1) {
							Dimension aDim = lastChanged.getSpriteSizeWithMargin();
							if(aDim.width <= bounds.width && aDim.height <= bounds.height) {
								lastChanged.sheet_pos.setLocation(oldPos);
								updateComplex = false;
							}
						}
						if(updateComplex) {
							int maxWidth = Math.max(img.getWidth(), 256);
							for(Icon icon : icons) {
								if(icon.sheet_pos.x == -1) {
									icon.setDefaultInitialPos(RenderPane.this, icons, maxWidth);
								}
							}
						}
						renderImage();
					}
					
					draggingIcon = null;
					renderDragging = false;
					repaint();
				}
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if(draggingIcon == null) return;

				renderDragging = true;
				Point2D p = new Point2D.Double(e.getX() / imgScale - draggingOffset.x,
						e.getY() / imgScale - draggingOffset.y);
				List<Point>[] snapPoints = getSnapPoints(draggingIcon);
				Dimension dim = draggingIcon.getSpriteSize();
				final int margin = draggingIcon.pad_image ? 1 : 0;
				Point[] iconPoints = {
					// top
					new Point(margin, margin),
					new Point(dim.width / -2, margin),
					new Point(-(dim.width + margin), margin),
					// middle
					new Point(margin, dim.height / -2),
					new Point(-(dim.width + margin), dim.height / -2),
					// bottom
					new Point(margin, -(dim.height + margin)),
					new Point(dim.width / -2, -(dim.height + margin)),
					new Point(-(dim.width + margin), -(dim.height + margin))
				};
				double mostOff = 5;
				Point offPointSheet = null;
				for(int side = 0; side < snapPoints.length; side++) {
					List<Point> sidePoints = snapPoints[side];
					Point sideOff = iconPoints[side];
					for(Point p2 : sidePoints) {
						p2.x += sideOff.x;
						p2.y += sideOff.y;
						double x = p2.x - p.getX();
						double y = p2.y - p.getY();
						double dist = Math.abs(x) + Math.abs(y);
						if(dist < mostOff) {
							offPointSheet = p2;
							mostOff = dist;
						} 
					}
				}
				if(offPointSheet != null) {
					p.setLocation(offPointSheet);
				}
				draggingPos.setLocation(p.getX(), p.getY());
				
				repaint();
			}
		});
	}
	
	public List<Point>[] getSnapPoints(Icon exclude) {
		@SuppressWarnings("unchecked")
		List<Point>[] corners = new List[8];
		for(int i = 0; i < corners.length; i++) {
			corners[i] = new ArrayList<>();
		}
		corners[SNAP_TOP_LEFT].add(new Point(0, 0));
		for(Icon icon : icons) {
			if(icon == exclude) continue;
			if(icon.sheet_pos.x < 0 || icon.sheet_pos.y < 0) continue;
			
			Rectangle bounds = icon.getBoundsWithMargin();
			int xRight = bounds.x + bounds.width;
			int yBottom = bounds.y + bounds.height;
			int xCenter = (xRight + bounds.x) / 2;
			int yMiddle = (yBottom + bounds.y) / 2;
			// aligned with right side of existing
			{
				corners[SNAP_TOP_LEFT].add(new Point(xRight, bounds.y));
				corners[SNAP_CENTER_LEFT].add(new Point(xRight, yMiddle));
				corners[SNAP_BOTTOM_LEFT].add(new Point(xRight, yBottom));
			}
			// aligned with left side of existing
			if(bounds.x > 0) {
				corners[SNAP_TOP_RIGHT].add(new Point(bounds.x, bounds.y));
				corners[SNAP_CENTER_RIGHT].add(new Point(bounds.x, yMiddle));
				corners[SNAP_BOTTOM_RIGHT].add(new Point(bounds.x, yBottom));
			}
			// aligned with bottom side of existing
			{
				corners[SNAP_TOP_LEFT].add(new Point(bounds.x, yBottom));
				corners[SNAP_TOP_CENTER].add(new Point(xCenter, yBottom));
				corners[SNAP_TOP_RIGHT].add(new Point(xRight, yBottom));
			}
			// aligned with top side of existing
			if(bounds.y > 0) {
				corners[SNAP_BOTTOM_LEFT].add(new Point(bounds.x, bounds.y));
				corners[SNAP_BOTTOM_CENTER].add(new Point(xCenter, bounds.y));
				corners[SNAP_BOTTOM_RIGHT].add(new Point(xRight, bounds.y));
			}
		}
		return corners;
	}
	
	public void renderImage() {
		Dimension dim = getImageSize();
		if(dim.width == 0 || dim.height == 0) {
			img = null;
			return;
		}
		
		BufferedImage newImg = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImg.createGraphics();
		
		g.setColor(imgBackground);
		//Composite comp = g.getComposite();
		g.setComposite(AlphaComposite.Src);
		g.fillRect(0, 0, dim.width, dim.height);
		//g.setComposite(comp);
		
		for(Icon icon : icons) {
			if(icon.croppedWithMargin) {
				g.drawImage(icon.croppedSprite, icon.sheet_pos.x - 1, icon.sheet_pos.y - 1, null);
			} else {
				g.drawImage(icon.croppedSprite, icon.sheet_pos.x, icon.sheet_pos.y, null);
			}
		}
		g.dispose();
		img = newImg;
		repaint();
	}
	
	public Dimension getImageSize() {
		int width = 0;
		int height = 0;
		for(Icon icon : icons) {
			Rectangle r = icon.getBoundsWithMargin();
			int x = r.x + r.width;
			int y = r.y + r.height;
			if(x > width) width = x;
			if(y > height) height = y;
		}
		return new Dimension(width, height);
	}
	
	private void determineScale(BufferedImage img) {
		if(img == null) {
			imgScale = 1;
			return;
		}
		
		int w = getWidth();
		int h = getHeight();
		double iw = Math.max(256, img.getWidth()) + 64;
		double ih = Math.max(256, img.getHeight()) + 64;
		double wRatio = w / iw;
		double hRatio = h / ih;
		imgScale = Math.min(wRatio, hRatio);
	}
	
	public Icon getIconAtPos(int x, int y) {
		Point2D p = new Point2D.Double(x / imgScale, y / imgScale);
		for(Icon icon : icons) {
			Rectangle rect = icon.getBoundsWithMargin();
			if(rect.contains(p)) return icon;
		}
		return null;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		BufferedImage img = this.img;
		determineScale(img);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if(img != null) {
			Image scaled = img.getScaledInstance((int) Math.round(img.getWidth() * imgScale),
					(int) Math.round(img.getHeight() * imgScale), Image.SCALE_SMOOTH);
			g.drawImage(scaled, 0, 0, null);
		}
		
		g.setColor(Color.MAGENTA.equals(getBackground()) ? Color.BLUE : Color.RED);
		for(Icon icon : icons) {
			Rectangle r = icon.getBoundsWithMargin();
			if(imgScale != 1) {
				r.x *= imgScale;
				r.y *= imgScale;
				r.width *= imgScale;
				r.height *= imgScale;
			}
			g2.draw(r);
		}
		
		if(draggingIcon != null && renderDragging) {
			if(imgScale != 1) {
				g2.scale(imgScale, imgScale);
			}
			
			Rectangle dragBounds = new Rectangle(draggingPos, draggingIcon.getSpriteSizeWithMargin());
			if(draggingIcon.pad_image) {
				dragBounds.x -= 1;
				dragBounds.y -= 1;
			}
			
			for(Icon icon : icons) {
				Rectangle bounds = icon.getBoundsWithMargin();
				Color color = null;
				if(icon == draggingIcon) {
					color = COLOR_DRAG_ORIGINAL;
				} else if(bounds.intersects(dragBounds)) {
					color = COLOR_OVERLAP;
				}
				if(color != null) {
					g.setColor(color);
					g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				}
			}
			g.setColor(COLOR_DRAG_CURRENT);
			g.fillRect(dragBounds.x, dragBounds.y, dragBounds.width, dragBounds.height);
		}
	}
}