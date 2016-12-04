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
	private ArrayList<Sensor> neighbours;
	
	
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
		
		this.BATTERY = 100;
		this.STATUS = Status.ON;
	}
	
	@Override
	public void setup() {
		addBehaviour(new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				
				//System.out.println(this.getAgent().getLocalName());
				
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(new AID("S1", AID.ISLOCALNAME));
				msg.setContent("Battery Life: " + BATTERY);
				send(msg);
				
				
				//Message handler
				addBehaviour(new CyclicBehaviour() {
					private static final long serialVersionUID = 1L;
					@Override
					public void action() {
						ACLMessage msg = myAgent.receive();
						
						if (msg != null) {
							System.out.println("Sensor " + msg.getSender().getLocalName() + " sent the following msg to Sensor S0:\n" + msg.getContent() + "\n");
						}
						else {
							block();
						}
					}
				});
				
				//Sampling handler
				
				addBehaviour(new CyclicBehaviour() {
					private static final long serialVersionUID = 1L;
					@Override
					public void action() {
						if (BATTERY > 0) {
							sampleEnvironment();
							updateBattery();
						}
						else {
							//System.out.println(msg.getSender().getLocalName() + " died @ " + BATTERY + "%");
							doDelete();
							color = new Color(0, 0, 255);
						}
					}
				});
			}
		});
	}
	
	public void sampleEnvironment() {
		
		
		Vector<?> vec = this.launcher.getRIVER().getMooreNeighbors(10, 10, false);
		//System.out.println("Sensor " + this.getLocalName() + " is sampling..");
		//for (Object cell : vec) 
			//if (cell instanceof Water)
				//System.out.println("Retrieved " + ((Water) cell).getPollution());
		
	}
	
	public void updateBattery() {
		
		if (BATTERY < 20) color = Color.RED;
		else if (BATTERY < 50) color = Color.BLACK;
		else color = Color.GREEN;
		
		BATTERY = BATTERY - 0.1f;
	}
	
	//Not tested
	public void getNeighbours() {
		
		float dist = -1;
		ArrayList<Sensor> sensors = launcher.getSENSORS();
		for (Sensor sensor : sensors) {
			dist = (float) Math.sqrt(
					(sensor.getX() - this.x)^2
					+
					(sensor.getY() - this.y)^2
					);
			
			if (dist < 20) neighbours.add(sensor);
		}
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
