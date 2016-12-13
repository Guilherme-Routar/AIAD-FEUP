package Agents;

import java.awt.Color;

import SIMLauncher.SIMLauncher;
import sajas.core.Agent;
import uchicago.src.sim.gui.SimGraphics;

public class SinkNode extends Agent{

	private int x, y;
	private Color color;
	private SIMLauncher launcher;
	
	public SinkNode() {
		this.x = 10;
		this.y = 3;
		this.color = Color.black;
	}
	
	public void setup() {
		System.out.println("We alive?");
	}
	
	public void draw(SimGraphics sim) {
		sim.drawFastOval(color);
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public Color getColor() {
		return color;
	}
}
