package Agents;
import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import java.awt.Color;

public class Sensor extends Agent implements Drawable{
	
	private int x;
	private int y;
	private Color color;

	public Sensor(int x, int y, Object2DGrid river, Color color) {
		this.x = x;
		this.y = y;
		this.color = color;
	}
	
	@Override
	public void draw(SimGraphics sim) {
		sim.drawFastRect(color);
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}
	
	public Color getColor() {
		return color;
	}
}
