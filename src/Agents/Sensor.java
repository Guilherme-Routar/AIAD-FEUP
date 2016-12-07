package Agents;
import sajas.core.AID;
import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;
import java.util.ArrayList;

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

public class Sensor extends Agent implements Drawable{

	private int x;
	private int y;
	private Color color;
	private SIMLauncher launcher;

	private float BATTERY;
	private Status STATUS;
	private ArrayList<Sensor> neighbours;
	private int sleepCounter;

	private ArrayList<Water> pollutionSamples;

	
	private boolean LEADER;
	private Sensor LEADEROFME;
	private boolean DEPENDANT;
	private ArrayList<Sensor> dependantNeighbours;
	
	// -----
	int FIRM_ADHERENCE = ACLMessage.ACCEPT_PROPOSAL;
	int ACK_ADHERENCE = ACLMessage.CONFIRM;
	// -----
	
	private double MIN_STD_DEV;
	private double MAX_STD_DEV;
	
	//Enums

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
		neighbours = new ArrayList<Sensor>();
		pollutionSamples = new ArrayList<Water>();
		
		this.LEADER = false;
		this.LEADEROFME = null;
		this.DEPENDANT = false;
		dependantNeighbours = new ArrayList<Sensor>();
		
		this.MIN_STD_DEV = 0;
		this.MAX_STD_DEV = 100;

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
		Water sampleCell = (Water) launcher.getRIVER().getObjectAt(this.x, this.y);
		sendSample(sampleCell);
	}

	public void sendSample(Water sampleCell) {

		try {
			
			//Creating sample
			Sample sample = new Sample(sampleCell.getPollution());
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM); //Performatives.INFORM
			for (Sensor sensor : neighbours) {
				msg.addReceiver(sensor.getAID());
			}

			msg.setContentObject(sample);
			pollutionSamples.add(sampleCell);

		} catch (Exception e) {
			System.out.println("Failed to send sample");
		}

	}

	//Builds the list of neighbours of the current node
	public void getNeighbours() {
		double dist = 16;
		ArrayList<Sensor> sensors = launcher.getSENSORS();
		for (Sensor sensor : sensors) {
			dist = getDist(sensor);
			if (dist <= 15 && this != sensor) neighbours.add(sensor);
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
		if (STATUS == Status.SLEEP) color = Color.GRAY;
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
	
	public Water getLastPollutSample() {
		return pollutionSamples.get(pollutionSamples.size() - 1);
	}

	//Messages handling -----

	public void msgHandler() {
		
		addBehaviour(new CyclicBehaviour(this) {

			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				
				ACLMessage msg = myAgent.receive();
				Object content = null;
				try {
					content = msg.getContentObject();
				} catch (UnreadableException e) {
					System.out.println("Error processing message content");
					e.printStackTrace();
				}
				
				//if (msg.getPerformative() == ACLMessage.INFORM) 
				int Performative = msg.getPerformative();
				
				if (msg != null) {

					//INFORM
					if (Performative == ACLMessage.INFORM) {

						//Sensor received inform sample message
						if (content instanceof Sample) {
							
							//Sample received
							((Sample) content).getContent();
							//#1 receive SAMPLE
							//updateNeighbourInfo();
							//adherence2NeighbourEvaluation();
							//updateOwnMaxAdherence();
							//if changesOnOwnMaxAdherence
							//then
							//inform(me, a l , maxAdh, t);
							//end
						}
						//Sensor received inform adherence message
						else if (content instanceof Adherence) {
							
							//Adherence reply
							((Adherence) content).getContent();
							//#2 receive ADHERENCE
							//inform(me, a r , lead); //Send leadership inform reply
							//updateNeighbourInfo();
							//adherence2NeighbourEvaluation();
							//updateOwnMaxAdherence();
							//if changesOnOwnMaxAdherence
							//then
							//inform(me, a l , maxAdh, t);
							//end
						}
						//Sensor received inform leadership message
						else if (content instanceof Leadership) {
							
							//Leadership reply
							((Leadership) content).getContent();
							//#3 receive LEADERSHIP
							//if checkAgainstOwnLead then
							//firmAdherence(me, a l );
							//end
						}
					}
					//Sensor received firm_adherence message
					//FIRM_ADHERENCE
					else if (Performative == FIRM_ADHERENCE) {

						//if checkAgainstOwnLead then
						//ackAdherence(me, a r );
						//updateOwnLeadValue();
						//updateDependentGroup();
						//end

					}
					//Sensor received ack_adherence message
					//ACK_ADHERENCE
					else if (Performative == ACK_ADHERENCE) {
						//if !leader ∧ a l ! = a L then
						//	withdraw(me, a L );
						//	end
						//	if leader ∧ D(me)! = ∅ then
						//	while D(me)! = ∅ do
						//	break(me, a p );
						//	end
						//	end
						//	updateRoleState(dependant);
						//	sleep(t);
					}
					//Sensor received break or withdraw message
					//BREAK | WITHDRAW
					else if (Performative == ACLMessage.CANCEL) {
						
						//leader wants out
						if (content instanceof Break){
							//updateRoleState(leader);
						}
						//dependant wants out
						else if (content instanceof Withdraw) {
							//D(me) ← D(me)\a p ;
							//updateRoleState(leader);
						}
					}
				}
				else 
					block();
			}
		});
	}
	
	// ---------------------------------------------------
	//                 COSA CALCULATIONS
	// ---------------------------------------------------
	
	public double calcAdherence(double pollutionSample) {
		
		
		//Vars to-be-initialized
		double stdDev = 0;
		// --------------------
		
		double Hj = calcEntropy(stdDev);
		double Hmax = calcEntropy(MAX_STD_DEV);
		double Hmin = calcEntropy(MIN_STD_DEV);
		
		double pollutionSamplesMean = calcMean();
		
		double valuesSimilarity = 
				calcNormalDistribution(pollutionSample, pollutionSamplesMean, stdDev)
				/
				calcNormalDistribution(pollutionSamplesMean, pollutionSamplesMean, stdDev);
		
		
		double variableModelCertainty = 1 - ((Math.pow(Math.E, Hj) - Math.pow(Math.E, Hmin)) 
											/ 
											(Math.pow(Math.E, Hmax) - Math.pow(Math.E, Hmin)));
		 
		
		return valuesSimilarity * variableModelCertainty;
	}
	
	public double calcMean() {
		double sum = 0;
		for (Water sample : pollutionSamples) {
			sum += sample.getPollution();
		}
		return sum / pollutionSamples.size();
		
	}
	
	public double calcNormalDistribution(double x, double u, double stdDev) {
		
		double leftHand = 1 / (stdDev * Math.sqrt(2 * Math.PI));
		double rightHand = Math.pow(Math.E, ((-1 * Math.pow(x - u, 2)) 
											 / 
											 (2 * Math.pow(stdDev, 2))));
		return leftHand * rightHand;
				
	}
	
	public double calcEntropy(double stdDev) {
		return Math.log(stdDev * Math.sqrt(2 * Math.PI * Math.E));
	}
	
	public double calcLead() {
		return 0;
	}
	
	// ---------------------------------------------------

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
