package gamemap.icon;

import java.awt.Color;

import javax.swing.JPanel;

public class PaneBase extends JPanel {
	public void cycleBackgroundColor() {
		setBackground(getNextBackgroundColor());
		repaint();
	}
	
	private Color getNextBackgroundColor() {
		Color col = getBackground();
		if(Color.BLACK.equals(col)) return Color.WHITE;
		if(Color.WHITE.equals(col)) return Color.MAGENTA;
		if(Color.MAGENTA.equals(col)) return null;
		return Color.BLACK;
	}
}