package Agents;
import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.util.Random;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import ACLMsgContentObjects.CANCEL.Break;
import ACLMsgContentObjects.CANCEL.Withdraw;
import ACLMsgContentObjects.INFORM.Adherence;
import ACLMsgContentObjects.INFORM.Leadership;
import ACLMsgContentObjects.INFORM.Sample;
import COSA_Calculations.COSA;
import Environment.Water;
import SIMLauncher.SIMLauncher;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.core.behaviours.CyclicBehaviour;


import jade.core.AID;
import sajas.core.behaviours.TickerBehaviour;

public class Sensor extends Agent implements Drawable{

	private int x, y;
	private Color color;
	private SIMLauncher launcher;

	private STATUS status;
	private double battery, stdDev, maxAdh, maxLead;
	private boolean leader;
	private AID leaderSensor;
	private boolean dependant;
	private int sleepCounter;
	private COSA_STRATEGY strategy;
	private SinkNode sinkNode;

	private Vector<Double> pollutionSamples;
	private HashMap<AID, Double> neighboursLastSampleMap, neighboursAdherenceMap;
	private Set<AID> dependantNeighbours;

	//###### FLAGS #######
	private final static double MAX_BATTERY = 1000;
	private final static double SECURITY_BATTERY = 20;
	private final static double MIN_STD_DEV = 0.0005;
	private final static double MAX_STD_DEV = 6;
	private final static int FIRM_ADHERENCE = ACLMessage.ACCEPT_PROPOSAL;
	private final static int ACK_ADHERENCE = ACLMessage.CONFIRM;
	private static enum STATUS {ON, OFF, SLEEP};
	private static enum COSA_STRATEGY {COSA, COSA_SF};

	public Sensor(int x, int y, Color color, SinkNode sinkNode, SIMLauncher launcher) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.launcher = launcher;

		this.status = STATUS.ON;
		this.battery = MAX_BATTERY;
		this.stdDev = Random.uniform.nextDoubleFromTo(MIN_STD_DEV, MAX_STD_DEV);
		this.maxAdh = 0;
		this.maxLead = 0;
		this.leader = false;
		this.leaderSensor = null;
	    this.dependant = false;
		this.strategy = COSA_STRATEGY.COSA;
		this.sinkNode = sinkNode;

		this.pollutionSamples = new Vector<Double>();
		this.neighboursLastSampleMap = new HashMap<AID, Double>();
		this.neighboursAdherenceMap = new HashMap<AID, Double>();
		this.dependantNeighbours = new TreeSet<AID>();
	}

	@Override
	public void setup() {	
		int samplingFrequency = 10;
		if (strategy == COSA_STRATEGY.COSA_SF)
			if (leader && dependantNeighbours.size() >= 4)
				samplingFrequency = 1;
		
		initBehaviours(samplingFrequency);
	}
	
	public void initBehaviours(int samplingFrequency) {
		addBehaviour(new TickerBehaviour(this, samplingFrequency) {
			
			private static final long serialVersionUID = 1L;
			@Override
			protected void onTick() {
				if (status == STATUS.ON) {
					sampleEnvironment();
				}
			}
		}); 
		addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
			public void action() {
				updateSensor();
			}
		});
		msgHandler();
	}

	public void updateSensor() {
		if (battery > 0) {	
			if (status == STATUS.ON)
				consumeBattery();
			else if (status == STATUS.SLEEP) {
				sleepCounter--;
				if (sleepCounter <= 0) {
					status = STATUS.ON;
					java.util.Random rand = new java.util.Random();
					color = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
				}
			}
		}
		else status = STATUS.OFF;
	}

	//Retrieves pollution sample on the sensor's position
	public void sampleEnvironment() {
		Water pollutionSample = (Water) launcher.getRIVER().getObjectAt(x, y);
		pollutionSamples.add(pollutionSample.getPollution());

		//Sending sample
		try {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContentObject(new Sample(getLastPollutionSample()));

			for (AID aid : neighboursAdherenceMap.keySet())
				msg.addReceiver(aid);

			msg.addReceiver(sinkNode.getAID());
			
			send(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Builds the sensors' neighbours map
	public void findNeighbours() {
		for (Object sensor : launcher.getSENSORS()) {
			Sensor sensorObj = (Sensor) sensor;
			if (!sensorObj.getLocalName().equals(getLocalName())) {
				if (distBetweenSensors(sensorObj) <= 10)
					neighboursAdherenceMap.put(sensorObj.getAID(), 0.0);
			}
		}
		
	}

	//Calculates the distance between the current sensor and another one
	public double distBetweenSensors(Sensor S) {
		double deltaX = S.getX() - this.x;
		double deltaY = S.getY() - this.y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}	

	//Puts the sensor to sleep
	private void sleep() {
		status = STATUS.SLEEP;
		sleepCounter = Random.uniform.nextIntFromTo(100, 300);
	}

	//Decrements the battery level
	public void consumeBattery() {
		battery--;
	}
	
	//##############################################################
	//                      MESSAGES HANDLER
	//##############################################################

	public void msgHandler() {

		addBehaviour(new CyclicBehaviour(this) {

			private static final long serialVersionUID = 1L;
			@Override
			public void action() {

				ACLMessage msg = myAgent.receive();

				if (msg != null) {

					Object content = null;
					int Performative = msg.getPerformative();

					if (Performative == ACLMessage.INFORM) {

						try {
							content = msg.getContentObject();
						} catch (UnreadableException e) {
							System.out.println("Error processing message content");
						}

						if (content instanceof Sample) handle_INFORM_Sample(msg, (Sample) content);
						else if (content instanceof Adherence) handle_INFORM_Adh(msg, (Adherence) content);
						else if (content instanceof Leadership) handle_INFORM_Lead(msg, (Leadership) content);
						else System.out.println("ERROR");
					}
					else if (Performative == FIRM_ADHERENCE) 
						handle_FIRM_ADHERENCE(msg);

					else if (Performative == ACK_ADHERENCE)
						handle_ACK_ADHERENCE(msg);

					else if (Performative == ACLMessage.CANCEL) {

						try {
							content = msg.getContentObject();
						} catch (UnreadableException e) {
							System.out.println("Error processing message content");
							e.printStackTrace();
						}

						if (content instanceof Break){
							if (leader) System.err.println("BREAK received");
							leaderSensor = null;
						}
						else if (content instanceof Withdraw) {
							//!leader || leaderSensor != null
							dependantNeighbours.remove(msg.getSender());
							if (dependantNeighbours.isEmpty()) leader = false;
						}
					}
				}
				else 
					block(); 
			}
		}); 
	}
	
	public void handle_INFORM_Sample(ACLMessage msg, Sample S) {
		double samplePollutionValue = ((Sample) S).getContent();

		neighboursLastSampleMap.put(msg.getSender(), samplePollutionValue);

		double adherence = calcAdherence(samplePollutionValue);
		if (maxAdh < adherence) {
			maxAdh = adherence;
			neighboursAdherenceMap.put(msg.getSender(), maxAdh);

			// inform(me, al, maxAdh, t); - Sending adherence value
			try {
				ACLMessage reply = msg.createReply();
				reply.setContentObject(new Adherence(maxAdh));
				send(reply);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handle_INFORM_Adh(ACLMessage msg, Adherence adh) {
		double leadership = calcLeadership(((Adherence) adh).getContent());

		// inform(me, ar, lead);
		try {
			ACLMessage reply = msg.createReply();
			reply.setContentObject(new Leadership(leadership));
			send(reply);
		} catch (IOException e) {
			e.printStackTrace();
		}

		double adherence = calcAdherence(neighboursLastSampleMap.get(msg.getSender()));

		if (maxAdh < adherence) {
			maxAdh = adherence;
			neighboursAdherenceMap.put(msg.getSender(), maxAdh);

			// inform(me, al, maxAdh, t);
			try {
				ACLMessage reply = msg.createReply();
				reply.setContentObject(new Adherence(maxAdh));
				send(reply);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handle_INFORM_Lead(ACLMessage msg, Leadership lead) {
		// if I have no leadera
		if (leaderSensor == null) {
			if (maxLead < ((Leadership) lead).getContent()) {
				maxLead = ((Leadership) lead).getContent();

				// firmAdherence(me, al);
				ACLMessage reply = msg.createReply();
				reply.setPerformative(FIRM_ADHERENCE); 
				send(reply);
			}
		}
	}

	public void handle_FIRM_ADHERENCE(ACLMessage msg) {
		if (leaderSensor == null) {
			// ackAdherence(me, ar);
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACK_ADHERENCE);
			send(reply);

			leader = true;
			dependantNeighbours.add(msg.getSender());
		}
	}

	public void handle_ACK_ADHERENCE(ACLMessage msg) {

		// withdraw(me, aL);
		if (!leader && leaderSensor != null && leaderSensor != msg.getSender()) {
			ACLMessage withdrawMsg = new ACLMessage(ACLMessage.CANCEL);
			try {
				withdrawMsg.setContentObject(new Withdraw());
			} catch (IOException e) {
				e.printStackTrace();
			}
			withdrawMsg.addReceiver(leaderSensor);
			send(withdrawMsg);
		}

		if (leader && !dependantNeighbours.isEmpty()) {
			ACLMessage breakMsg = new ACLMessage(ACLMessage.CANCEL);

			// break(me, ap);
			for (AID aid : dependantNeighbours)
				breakMsg.addReceiver(aid);

			send(breakMsg);

			dependantNeighbours.clear();
		}

		leader = false;
		leaderSensor = msg.getSender();
		sleep();
	}
	
	//##############################################################
	//             LEADERSHIP & ADHERENCE CALCULATIONS
	//##############################################################
	
	private double calcLeadership(double negotiatingNeighbourMaxAdherence) {
		//Prestige calculation
		double prestigeSum = 0;

		for (AID dependantAID : dependantNeighbours) prestigeSum += neighboursAdherenceMap.get(dependantAID);
		prestigeSum += calcAdherence(getLastPollutionSample());
		prestigeSum += negotiatingNeighbourMaxAdherence;

		double prestige = prestigeSum / (dependantNeighbours.size() + 2);

		//Capacity calculation
		double capacity = (battery - SECURITY_BATTERY) / MAX_BATTERY;

		//Representativeness calculation
		Vector<Double> groupSamples = new Vector<Double>(neighboursLastSampleMap.values());
		groupSamples.add(getLastPollutionSample());

		double groupSamplesMean = COSA.calcMean(groupSamples);

		double representativeness = 1 / (Math.pow(Math.E, Math.abs(getLastPollutionSample() - groupSamplesMean)
				* COSA.calcCV(new Vector<Double>(neighboursAdherenceMap.values()))));

		return prestige * capacity * representativeness;
	}

	private double calcAdherence(double pollutionSample) {
		double Hj = COSA.calcEntropy(stdDev);
		double Hmax = COSA.calcEntropy(MAX_STD_DEV);
		double Hmin = COSA.calcEntropy(MIN_STD_DEV);
		double variableModelCertainty = 1 - ((Math.pow(Math.E, Hj) - Math.pow(Math.E, Hmin)) 
				/ 
				(Math.pow(Math.E, Hmax) - Math.pow(Math.E, Hmin)));

		double pollutionSamplesMean = COSA.calcMean(pollutionSamples);
		double valuesSimilarity = 
				COSA.calcNormalDistribution(pollutionSample, pollutionSamplesMean, stdDev)
				/
				COSA.calcNormalDistribution(pollutionSamplesMean, pollutionSamplesMean, stdDev);

		return valuesSimilarity * variableModelCertainty;
	}
	
	//##############################################################

	@Override
	public void draw(SimGraphics sim) {
		if (status == STATUS.ON || status == STATUS.SLEEP) {
			if (leaderSensor != null && leaderSensor != getAID())
				sim.drawFastRect(launcher.getSensorCoalitionColor(leaderSensor));
			else if (leader)
				sim.drawHollowFastOval(color);
			else
				sim.drawFastRect(color);
		}
		else if (status == STATUS.OFF) sim.drawFastRect(Color.BLACK);
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

	public double getBattery() {
		return battery;
	}
	
	public double getLastPollutionSample() {
		return pollutionSamples.get(pollutionSamples.size() - 1);
	}
	
	public void setDependant() {
		this.dependant = true;
	}
}
