package Agents;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import ACLMsgContentObjects.INFORM.Sample;
import Environment.Water;
import SIMLauncher.SIMLauncher;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import uchicago.src.sim.gui.SimGraphics;

public class SinkNode extends Agent{

	private int x, y;
	private Color color;
	private SIMLauncher launcher;
	private float errorSum;
	private float actualPollutionLevelsSum;
	private double lastSamplePollutionLevels;

	public SinkNode(SIMLauncher launcher) {
		this.x = launcher.getRIVER_WIDTH() - 1;
		this.y = launcher.getRIVER_HEIGHT() / 2;
		this.color = Color.black;
		this.launcher = launcher;
		this.errorSum = 0;
		this.actualPollutionLevelsSum = 0;
		this.lastSamplePollutionLevels = 0;
	}
	
	@Override
	public void setup() {	
		msgHandler();
	}
	
	public void msgHandler() {
		
		addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void action() {
				
				//Getting pollution level for every instant 
				double actualPollutionLevel = 0;
				for (Sensor sensor : launcher.getSENSORS()) {
					if (sensor.getLocalName().equals("S1")) {
						actualPollutionLevel = ((Water) launcher.getRIVER().getObjectAt(sensor.getX(), sensor.getY())).getPollution();
						actualPollutionLevelsSum += actualPollutionLevel;
					}
				}
				
				//Getting last pollution level sent by agent S1
				double error;
				ACLMessage msg = myAgent.receive();
				Object content = null;
				
				if (msg != null) {
					try {
						content = msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					
					lastSamplePollutionLevels = ((Sample) content).getContent();
					error = Math.abs(actualPollutionLevel - lastSamplePollutionLevels);
					errorSum += error;
				}
				else {
					error = Math.abs(actualPollutionLevel - lastSamplePollutionLevels);
					errorSum += error;
				}
			}
		});
	}
	
	public double calcErrorPercentage() {
		if (errorSum == 0 && actualPollutionLevelsSum == 0)
			return 0;
		else {
			if (errorSum > actualPollutionLevelsSum)
				return (double) ((1 - (actualPollutionLevelsSum / errorSum)) * 100);
			else
				return (double) ((1 - (errorSum / actualPollutionLevelsSum)) * 100);
		}		
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
	
	public AID getSinkNodeAID() {
		return this.getAID();
	}
}
