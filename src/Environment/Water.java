package Environment;

import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

public class Water implements Drawable{
	
	private int x, y;
	private Color color;
	
	public Water(int x, int y) {
		this.x = x;
		this.y = y;
		this.color = new Color(20, 80, 155);
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
}
