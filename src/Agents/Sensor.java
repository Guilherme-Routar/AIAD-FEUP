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

	private float MAX_BATTERY;
	private float BATTERY;
	private float SECURITY_BATTERY;
	
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
	
	private double STD_DEV;
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

		this.MAX_BATTERY = 1000;
		this.BATTERY = MAX_BATTERY;
		this.SECURITY_BATTERY = 100; //To fix value
	
		this.STATUS = Status.ON;
		neighbours = new ArrayList<Sensor>();
		pollutionSamples = new ArrayList<Water>();
		
		this.LEADER = false;
		this.LEADEROFME = null;
		this.DEPENDANT = false;
		dependantNeighbours = new ArrayList<Sensor>();
		
		this.STD_DEV = 0;
		this.MIN_STD_DEV = 0;
		this.MAX_STD_DEV = 100;

	}

	@Override
	public void setup() {
		
		getNeighbours();

		addBehaviour(new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;
			@Override
			public void action() {
					updateSensor();
			}
		});
		
		msgHandler();
	}

	//Retrieves pollution sample on the sensor's position
	public void sampleEnvironment() {
		Water sampleCell = (Water) launcher.getRIVER().getObjectAt(this.x, this.y);
		sendSample(sampleCell);
	}

	public void sendSample(Water sampleCell) {

		//Creating sample
		Sample sample = new Sample(sampleCell.getPollution());
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM); //Performatives.INFORM
		for (Sensor sensor : neighbours) {
			msg.addReceiver(sensor.getAID());
		}
		
		try {
			msg.setContentObject(sample);
			pollutionSamples.add(sampleCell);
			send(msg);

		} catch (Exception e) {
			System.out.println("Failed to send sample");
		}
		
		System.out.print(this.getLocalName() + " sent sample to Agent ");
		for (Sensor sensor : neighbours) {
			System.out.print(sensor.getLocalName() + " ");
		}//System.out.println("");
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

			@SuppressWarnings("unused")
			@Override
			public void action() {
				
				ACLMessage msg = myAgent.receive();
			
				int Performative = msg.getPerformative();
				
				if (msg != null) {
					
					Object content = null;

					//INFORM
					if (Performative == ACLMessage.INFORM) {
						
						try {
							content = msg.getContentObject();
						} catch (UnreadableException e) {
							System.out.println("Error processing message content");
						}

						//Sensor received inform sample message
						if (content instanceof Sample) {
							
							((Sample) content).getContent();
							
							try {
								ACLMessage reply = msg.createReply();
								reply.setContent("Received sample, sending adherence = 10");
								reply.setContentObject(new Adherence(10));
								send(reply);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							System.out.println("Received sample, sending adherence = 10");
							
							//Sample received
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
							
							((Adherence) content).getContent();
							
							try {
								ACLMessage reply = msg.createReply();
								reply.setContent("Received adh value, sending leadership = 10");
								reply.setContentObject(new Leadership(10));
								send(reply);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							System.out.println("Received adh value, sending leadership = 10");
							//Adherence reply
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
							
							((Leadership) content).getContent();
							
							try {
								ACLMessage reply = msg.createReply();
								reply.setPerformative(FIRM_ADHERENCE);
								reply.setContent("received lead value, sending firm_adherence msg");
								send(reply);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							System.out.println("received lead value, sending firm_adherence msg");
							
							//Leadership reply
							//#3 receive LEADERSHIP
							//if checkAgainstOwnLead then
							//firmAdherence(me, a l );
							//end
						}
					}
					//Sensor received firm_adherence message
					//FIRM_ADHERENCE
					else if (Performative == FIRM_ADHERENCE) {
						
						try {
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACK_ADHERENCE);
							reply.setContent("Received firm_adh msg, sending ack_adherence msg");
							send(reply);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						System.out.println("Received firm_adh msg, sending ack_adherence msg");
						
						//if checkAgainstOwnLead then
						//ackAdherence(me, a r );
						//updateOwnLeadValue();
						//updateDependentGroup();
						//end

					}
					//Sensor received ack_adherence message
					//ACK_ADHERENCE
					else if (Performative == ACK_ADHERENCE) {
						
						try {
							ACLMessage reply = msg.createReply();
							reply.setContent("Received ack_adh, now forming coalition");
							send(reply);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						System.out.println("Received ack_adh, now forming coalition");
						
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
						
						try {
							content = msg.getContentObject();
						} catch (UnreadableException e) {
							System.out.println("Error processing message content");
							e.printStackTrace();
						}
						
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
		
		double Hj = calcEntropy(STD_DEV);
		double Hmax = calcEntropy(MAX_STD_DEV);
		double Hmin = calcEntropy(MIN_STD_DEV);
		
		double pollutionSamplesMean = calcMean();
		
		double valuesSimilarity = 
				calcNormalDistribution(pollutionSample, pollutionSamplesMean, STD_DEV)
				/
				calcNormalDistribution(pollutionSamplesMean, pollutionSamplesMean, STD_DEV);
		
		
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
		/*
		//Prestige calculation
		double prestige = 0;
		
		for (Sensor sensor : dependantNeighbours) {
			//calcAdherence(pollutionSamp);
		}
		
		//Capacity calculation
		double capacity = (BATTERY - SECURITY_BATTERY) / MAX_BATTERY;
		
		//Representativeness calculation
		
		double b = Math.pow(Math.E, 10);
		double a = 1 / (b);
		
		double representativeness = 0;
		
		return prestige * capacity * representativeness;
		*/
		return 0;
	}
	
	public double CV(ArrayList<Double> List) {
		return calcStdDev(List) / calcMean(List);
	}
	
	public double calcStdDev(ArrayList<Double> List) {
		return Math.sqrt(calcVariance(List));
	}
	
	public double calcVariance(ArrayList<Double> List) {
		double mean = calcMean(List);
		double sum = 0;
		for (Double val : List) {
			sum += (mean - val) * (mean - val); 
		}
		return sum / List.size();
	}
	
	public double calcMean(ArrayList<Double> List) {
		double sum = 0;
		for (Double val : List) {
			sum += val;
		}
		return sum;
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
