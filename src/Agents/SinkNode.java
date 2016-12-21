package Agents;


import ACLMsgContentObjects.INFORM.Sample;
import Environment.Water;
import SIMLauncher.SIMLauncher;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;

public class SinkNode extends Agent{

	private int x, y;
	private SIMLauncher launcher;
	private float samplesPollutionLevelsSum;
	private float actualPollutionLevelsSum;
	private double lastSamplePollutionLevels;
	

	public SinkNode(SIMLauncher launcher) {
		this.x = launcher.getRIVER_WIDTH() - 1;
		this.y = launcher.getRIVER_HEIGHT() / 2;
		this.launcher = launcher;
		
		this.samplesPollutionLevelsSum = 0;
		this.actualPollutionLevelsSum = 0;
		this.lastSamplePollutionLevels = 0;
	}
	
	@Override
	public void setup() {	
		msgHandler();
	}
	
	public void msgHandler() {
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;
			@Override
			public void action() {
				
				//Getting pollution level for every instant 
				double actualPollutionLevel = 0;
				for (Sensor sensor : launcher.getSENSORS()) {
					if (sensor.isAlive()) {
						actualPollutionLevel = ((Water) launcher.getRIVER().getObjectAt(sensor.getX(), sensor.getY())).getPollution();
						actualPollutionLevelsSum += actualPollutionLevel;
					}
				}
				
				//Getting last pollution levels received
				ACLMessage msg = myAgent.receive();
				Object content = null;				
				if (msg != null) {

					try {
						content = msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					
					lastSamplePollutionLevels = ((Sample) content).getContent();
					samplesPollutionLevelsSum += lastSamplePollutionLevels;
				}
				else
					samplesPollutionLevelsSum += lastSamplePollutionLevels;
			}
		});
	}
	
	public double calcErrorPercentage() {
		return Math.abs(1 - (actualPollutionLevelsSum / (actualPollutionLevelsSum - samplesPollutionLevelsSum))) * 1000;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public AID getSinkNodeAID() {
		return this.getAID();
	}
}
