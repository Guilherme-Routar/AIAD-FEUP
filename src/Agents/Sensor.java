package Agents;
import sajas.core.AID;
import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import Environment.Water;
import SIMLauncher.SIMLauncher;
import jade.lang.acl.ACLMessage;
import sajas.core.behaviours.CyclicBehaviour;

public class Sensor extends Agent implements Drawable{

	private int x;
	private int y;
	private Color color;
	private SIMLauncher launcher;

	private float BATTERY;
	private Status STATUS;
	private boolean LEADER;
	private ArrayList<String> neighbours;
	private int sleepCounter;


	public static enum Status {
		ON, 
		OFF, 
		SLEEP 
	};

	public Sensor(int x, int y, Color color, SIMLauncher launcher) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.launcher = launcher;

		this.BATTERY = 1000;
		this.STATUS = Status.ON;
		neighbours = new ArrayList<String>();
	}

	@Override
	public void setup() {
		//Sampling handler
		addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
			public void action() {
				if (BATTERY > 0) updateSensor();
				else {
					doDelete();
					color = new Color(0, 0, 255);
				}
			}
		});
	}

	//Retrieves pollution sample on the sensor's position
	public void sampleEnvironment() {
		System.out.println(this.getLocalName() + " scanned:");
		Water cell = (Water) launcher.getRIVER().getObjectAt(this.x, this.y);
		System.out.println(cell.getPollution());
	}

	//Builds the list of neighbours of the current node
	public void getNeighbours() {
		double dist = 16;
		ArrayList<Sensor> sensors = launcher.getSENSORS();
		for (Sensor sensor : sensors) {
			dist = getDist(sensor);
			if (dist <= 15 && this != sensor) neighbours.add(sensor.getLocalName());
		}
	}

	//Returns the distance between 2 sensors
	public double getDist(Sensor S) {
		double deltaX = S.getX() - this.x;
		double deltaY = S.getY() - this.y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}

	//Updates sensor's status and battery
	public void updateSensor() {

		if (BATTERY <= 0) {
			STATUS = Status.OFF;
			return;
		}

		if (STATUS == Status.ON) {
			sampleEnvironment();
			consumeBattery();
		}
		else if (STATUS == Status.SLEEP) {
			sleepCounter--;
			if (sleepCounter <= 0) STATUS = Status.ON;
		}
		else System.out.println("Sensor is dead or statless");

		updateColor();
	}
	
	public void updateColor() {
		if (STATUS == Status.SLEEP) color = Color.BLACK;
		else if (BATTERY < 150) color = Color.RED;
		else if (BATTERY < 400) color = Color.YELLOW;
		else color = Color.GREEN;
	}

	public void sleep() {
		STATUS = Status.SLEEP;
		sleepCounter = 150;
	}

	public void consumeBattery() {
		BATTERY--;
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

	public float getBattery() {
		return BATTERY;
	}
}
