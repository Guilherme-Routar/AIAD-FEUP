package Agents;
import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import java.awt.Color;

public class Sensor extends Agent implements Drawable{
	
	private int x;
	private int y;
	private Object2DGrid river;
	private Color color;

	public Sensor(int x, int y, Object2DGrid river, Color color) {
		this.x = x;
		this.y = y;
		this.river = river;
		this.color = color;
	}
	
	public void setup() {
		river.putObjectAt(this.x, this.y, this); 
	}

	@Override
	public void draw(SimGraphics sim) {
		sim.drawFastCircle(color);
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
