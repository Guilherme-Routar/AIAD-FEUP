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
import Environment.Water;
import SIMLauncher.SIMLauncher;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.core.behaviours.CyclicBehaviour;


import jade.core.AID;

public class Sensor extends Agent implements Drawable{

	private int x, y;
	private Color color;
	private SIMLauncher launcher;

	private STATUS status;
	private double battery;
	private double stdDev, maxAdh, maxLead;
	private boolean leader;
	private AID nodeLeaderOfMe;
	private int sleepCounter;

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
	public static enum STATUS {ON, OFF, SLEEP};

	public Sensor(int x, int y, Color color, SIMLauncher launcher) {
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
		this.nodeLeaderOfMe = null;

		this.pollutionSamples = new Vector<Double>();
		this.neighboursLastSampleMap = new HashMap<AID, Double>();
		this.neighboursAdherenceMap = new HashMap<AID, Double>();
		this.dependantNeighbours = new TreeSet<AID>();
	}

	@Override
	public void setup() {
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
			if (status == STATUS.ON) {
				sampleEnvironment();
				consumeBattery();
			}
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

			send(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Builds the list of neighbours of the current node
	public void getNeighbours() {
		for (Sensor sensor : launcher.getSENSORS()) {
			if (!sensor.getLocalName().equals(getLocalName())) {
				if (distBetweenSensors(sensor) <= 10)
					neighboursAdherenceMap.put(sensor.getAID(), 0.0);
			}
		}
	}

	public double distBetweenSensors(Sensor S) {
		double deltaX = S.getX() - this.x;
		double deltaY = S.getY() - this.y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}	

	private void sleep() {
		status = STATUS.SLEEP;
		sleepCounter = Random.uniform.nextIntFromTo(200, 500);
	}

	public void consumeBattery() {
		battery--;
	}

	public double getLastPollutionSample() {
		return pollutionSamples.get(pollutionSamples.size() - 1);
	}

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

						//leader wants out
						if (content instanceof Break){
							if (leader)
								System.err.println("BREAK received and I am a leader!");

							/*
							 * If a leader breaks its relationship with me, I no
							 * longer have a leader.
							 */
							nodeLeaderOfMe = null;

						}
						//dependant wants out
						else if (content instanceof Withdraw) {
							if (!leader || nodeLeaderOfMe != null)
								System.err.println("WITHDRAW received and I am not a leader");

							// remove node from my dependants list
							dependantNeighbours.remove(msg.getSender());

							/*
							 * If I have no more dependant nodes, I am no longer
							 * a leader.
							 */
							if (dependantNeighbours.isEmpty())
								leader = false;
						}
					}
				}
				else 
					block(); 
			}
		}); 
	}
	
	public void handle_INFORM_Sample(ACLMessage msg, Sample S) {
		double receivedSample = ((Sample) S).getContent();

		neighboursLastSampleMap.put(msg.getSender(), receivedSample);

		double adherence = calcAdherence(receivedSample);

		// updateOwnMaxAdherence();
		if (maxAdh < adherence) {
			maxAdh = adherence;

			neighboursAdherenceMap.put(msg.getSender(), maxAdh);

			try {
				// inform(me, al, maxAdh, t);
				ACLMessage reply = msg.createReply();
				reply.setContentObject(new Adherence(maxAdh));
				send(reply);
				System.out.println("Sent ADH");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handle_INFORM_Adh(ACLMessage msg, Adherence adh) {
		double leadership = calcLeadership(((Adherence) adh).getContent());

		try {
			// inform(me, ar, lead);
			ACLMessage reply = msg.createReply();
			reply.setContentObject(new Leadership(leadership));
			send(reply);
			System.out.println("Sent LEAD");
		} catch (IOException e) {
			e.printStackTrace();
		}

		double adherence = calcAdherence(
				neighboursLastSampleMap.get(msg.getSender()));

		// updateOwnMaxAdherence();
		if (maxAdh < adherence) {
			maxAdh = adherence;

			neighboursAdherenceMap.put(msg.getSender(), maxAdh);

			try {
				// inform(me, al, maxAdh, t);
				ACLMessage reply = msg.createReply();
				reply.setContentObject(new Adherence(maxAdh));
				send(reply);
				System.out.println("Sent ADH");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handle_INFORM_Lead(ACLMessage msg, Leadership lead) {
		((Leadership) lead).getContent();

		// if I have no leader
		if (nodeLeaderOfMe == null) {
			if (maxLead < ((Leadership) lead).getContent()) {
				maxLead = ((Leadership) lead).getContent();

				// firmAdherence(me, al);
				ACLMessage reply = msg.createReply();
				reply.setPerformative(FIRM_ADHERENCE); 
				send(reply);
				System.out.println("Sent FIRM");
			}
		}
	}

	public void handle_FIRM_ADHERENCE(ACLMessage msg) {
		// If I can become a leader (or keep being one)
		if (nodeLeaderOfMe == null) {
			// ackAdherence(me, ar);
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACK_ADHERENCE);
			send(reply);

			// updateOwnLeadValue();
			leader = true;

			// updateDependentGroup();
			dependantNeighbours.add(msg.getSender());
			System.out.println("Sent ACK");
		}
	}

	public void handle_ACK_ADHERENCE(ACLMessage msg) {
		/*
		 * If I am not a leader and I already have a leader
		 * different than the one I want to adhere too.
		 */
		if (!leader && nodeLeaderOfMe != null && nodeLeaderOfMe != msg.getSender()) {
			// withdraw(me, aL);
			ACLMessage withdrawMsg = new ACLMessage(ACLMessage.CANCEL);
			try {
				withdrawMsg.setContentObject(new Withdraw());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			withdrawMsg.addReceiver(nodeLeaderOfMe);
			send(withdrawMsg);
			System.out.println("Sent Withdraw");
		}

		/*
		 * If I am a leader and there are nodes dependant on
		 * me.
		 */
		if (leader && !dependantNeighbours.isEmpty()) {
			ACLMessage breakMsg = new ACLMessage(ACLMessage.CANCEL);

			// break(me, ap);
			for (AID aid : dependantNeighbours)
				breakMsg.addReceiver(aid);

			send(breakMsg);

			dependantNeighbours.clear();
			System.out.println("Sent Break");
		}

		/*
		 * I become dependant, I now have a leader, and I
		 * can go to sleep.
		 */
		leader = false;
		nodeLeaderOfMe = msg.getSender();
		sleep();
	}

	private double calcLeadership(double negotiatingNeighbourMaxAdherence) {
		double prestigeSum = 0;

		for (AID dependantAID : dependantNeighbours) prestigeSum += neighboursAdherenceMap.get(dependantAID);
		prestigeSum += calcAdherence(getLastPollutionSample());
		prestigeSum += negotiatingNeighbourMaxAdherence;

		double prestige = prestigeSum / (dependantNeighbours.size() + 2);

		double capacity = (battery - SECURITY_BATTERY) / MAX_BATTERY;

		Vector<Double> groupSamples = new Vector<Double>(neighboursLastSampleMap.values());
		groupSamples.add(getLastPollutionSample());

		double groupSamplesMean = calcMean(groupSamples);

		double representativeness = 1 / (Math.pow(Math.E, Math.abs(getLastPollutionSample() - groupSamplesMean)
				* calcCV(new Vector<Double>(neighboursAdherenceMap.values()))));

		return prestige * capacity * representativeness;

	}

	private double calcAdherence(double pollutionSample) {
		double Hj = calcEntropy(stdDev);
		double Hmax = calcEntropy(MAX_STD_DEV);
		double Hmin = calcEntropy(MIN_STD_DEV);
		double variableModelCertainty = 1 - ((Math.pow(Math.E, Hj) - Math.pow(Math.E, Hmin)) 
				/ 
				(Math.pow(Math.E, Hmax) - Math.pow(Math.E, Hmin)));

		double pollutionSamplesMean = calcMean(pollutionSamples);
		double valuesSimilarity = 
				calcNormalDistribution(pollutionSample, pollutionSamplesMean, stdDev)
				/
				calcNormalDistribution(pollutionSamplesMean, pollutionSamplesMean, stdDev);

		return valuesSimilarity * variableModelCertainty;
	}

	public static final double calcNormalDistribution(double x, double u, double stdDev) {
		double leftHand = 1 / (stdDev * Math.sqrt(2 * Math.PI));
		double rightHand = Math.pow(Math.E, ((-1 * Math.pow(x - u, 2)) 
											 / 
											 (2 * Math.pow(stdDev, 2))));
		return leftHand * rightHand;
	}

	public static final double calcMean(Vector<Double> Vec) {
		double sum = 0;
		for (double val : Vec) sum += val;
		return sum / Vec.size();
	}

	public static final double calcEntropy(double stdDeviation) {
		return Math.log(stdDeviation * Math.sqrt(2 * Math.PI * Math.E));
	}

	public static final double calcCV(Vector<Double> Vec) {
		return calcStdDev(Vec) / calcMean(Vec);
	}

	public static final double calcStdDev(Vector<Double> Vec) {
		return Math.sqrt(calcVariance(Vec));
	}

	public static final double calcVariance(Vector<Double> Vec) {
		double mean = calcMean(Vec);
		double sum = 0;
		for (double val : Vec)
			sum += (mean - val) * (mean - val);
		return sum / Vec.size();
	}

	@Override
	public void draw(SimGraphics sim) {
		if (status == STATUS.ON || status == STATUS.SLEEP) {
			if (nodeLeaderOfMe != null && nodeLeaderOfMe != getAID())
				sim.drawFastRect(launcher.getSensorCoalitionColor(nodeLeaderOfMe));
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
}
