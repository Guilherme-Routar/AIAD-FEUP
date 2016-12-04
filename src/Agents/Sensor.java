package Agents;
import sajas.core.AID;
import sajas.core.Agent;
import uchicago.src.repastdemos.genetic.GeneticChangeModel;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import java.awt.Color;
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
	
	private float battery;

	public Sensor(int x, int y, Color color, SIMLauncher launcher) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.launcher = launcher;
		this.battery = 100;
	}
	
	@Override
	public void setup() {
		addBehaviour(new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(new AID("S0", AID.ISLOCALNAME));
				msg.setLanguage("English");
				msg.setOntology("Battery status");
				msg.setContent("Battery Life: " + battery + "%. \nTrasmission Over.");
				send(msg);
				
				
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
				
				//sampleEnvironment();
			}
		});
	}
	
	public void sampleEnvironment() {
		
		
		Vector<?> vec = this.launcher.getRIVER().getMooreNeighbors(10, 10, false);
		System.out.println("Sensor " + this.getLocalName() + " is sampling..");
		for (Object cell : vec) 
			if (cell instanceof Water)
				System.out.println("Retrieved " + ((Water) cell).getPollution());
		
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
