package Environment;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

public class Water implements Drawable{

	public static final int MAX_POLLUTION = 1000;
	private double POLLUTION;

	private int x, y, Rvalue;
	private Color color;

	public Water(int x, int y) {
		this.x = x;
		this.y = y;
		this.POLLUTION = 0;
	}

	@Override
	public void draw(SimGraphics sim) {
		sim.drawFastRect(color);
	}

	public void setPollution(double pollution) {
		if (pollution > MAX_POLLUTION) this.POLLUTION = MAX_POLLUTION;
		else this.POLLUTION = pollution;
		
		Rvalue = (int) this.POLLUTION * 255 / MAX_POLLUTION;
		this.color = new Color(Rvalue, 0, 255);
	}

	public double getPollution() {
		return POLLUTION;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}
}
