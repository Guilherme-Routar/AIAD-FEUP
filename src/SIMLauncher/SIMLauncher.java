package SIMLauncher;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import Agents.Sensor;
import Agents.SinkNode;
import Environment.Water;
import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.TextDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.util.Random;

public class SIMLauncher extends Repast3Launcher {

	private static boolean BATCH_MODE = false;
	//Parameters
	private ContainerController mainContainer;
	private ArrayList<Water> waterCells; 
	private ArrayList<Sensor> sensors;
	private SinkNode sinkNode;
	//Simulation elements
	private Object2DGrid river;
	private DisplaySurface surface;
	//Environment variables
	private int NUM_SENSORS;
	private static int CELLS_PER_KM;
	private int RIVER_WIDTH, RIVER_HEIGHT;
	//Scenarios (Sensors allocation throughout the river)
	private Scenario SCENARIO;
	public static enum Scenario {
		CHAINALONGRIVER, 
		ENDOFRIVER, 
		RANDOM
	};
	//Pollution propagation elements
	private int POLLUTION_Y_VALUE = RIVER_HEIGHT / 3;
	private float SEDIMENTATION, ALPHA, BETA, GAMMA;
		
	private OpenSequenceGraph batteryPlot;
	private OpenSequenceGraph errorPlot;

	//Initialization 
	public SIMLauncher() { 
		initRiverDimensions();
		initPollutionValues();
		initScenario();
	}

	public void initRiverDimensions() {
		CELLS_PER_KM = 10;
		RIVER_WIDTH = 50 * CELLS_PER_KM;
		RIVER_HEIGHT = 2 * CELLS_PER_KM;
	}

	public void initPollutionValues() {
		POLLUTION_Y_VALUE = RIVER_HEIGHT / 3;
		SEDIMENTATION = 0.99f;
		ALPHA = 0.1f; 
		BETA = 0.8f;
		GAMMA = 0.1f;
	}

	@SuppressWarnings("unchecked")
	public void initScenario() {
		SCENARIO = Scenario.RANDOM;
		Vector<Scenario> vecScenarios = new Vector<Scenario>();
		for (int i = 0; i < Scenario.values().length; i++) vecScenarios.add(Scenario.values()[i]);
		descriptors.put("SCENARIO", new ListPropertyDescriptor("SCENARIO", vecScenarios));
	}
	
	//-----

	@Override
	public void setup() {
		super.setup();
		if (surface != null) surface.dispose();
		surface = new DisplaySurface(this, "Sensors river");
		registerDisplaySurface("Sensors river", surface);
	}

	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);
		
		launchSinkNode();
		launchSensors();
		getSensorsNeighbours();
		launchWater();
		
	}
	
	private void getSensorsNeighbours() {
		for (Sensor sensor : sensors) sensor.findNeighbours();
	}

	private void launchSensors() {
		try {
			Sensor sensor;
			int y, x;
			if (SCENARIO == Scenario.CHAINALONGRIVER) {
				setNUM_SENSORS(50);
				for (int i = 0; i < NUM_SENSORS; i++) {
					x = i * (RIVER_WIDTH / getNUM_SENSORS());
					y = RIVER_HEIGHT / 2;
					sensor = allocateSensor(x, y);
					mainContainer.acceptNewAgent("S" + i, sensor).start();
				}
			}
			else if (SCENARIO == Scenario.ENDOFRIVER) {
				setNUM_SENSORS(30);
				for (int i = 0; i < NUM_SENSORS / 3; i++) {
					for (int j = 0; j < 3; j++) {
						x = (i * (RIVER_WIDTH / getNUM_SENSORS())) / 4;
						y = (j * 4) + 6; //Hardcoded for 10 cells per km
						sensor = allocateSensor(x, y);
						mainContainer.acceptNewAgent("S" + (i * 3 + j), sensor).start();
					}
				}
			}
			else if (SCENARIO == Scenario.RANDOM) {
				setNUM_SENSORS(50);
				for (int i = 0; i < NUM_SENSORS; i++) {
					x = Random.uniform.nextIntFromTo(0, RIVER_WIDTH - 1);
					y = Random.uniform.nextIntFromTo(0, RIVER_HEIGHT - 1);
					sensor = allocateSensor(x, y);
					mainContainer.acceptNewAgent("S" + i, sensor).start();
				}
			}
		}
		catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	private void launchSinkNode() {
		SinkNode sinkNode = new SinkNode(this);
		this.sinkNode = sinkNode;
		river.putObjectAt(sinkNode.getX(), sinkNode.getY(), sinkNode);
		try {
			mainContainer.acceptNewAgent("S", sinkNode).start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	private Sensor allocateSensor(int x, int y) {
		java.util.Random rand = new java.util.Random();
		Color c = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
		Sensor sensor = new Sensor(x, y, c, sinkNode, this);
		river.putObjectAt(x, y, sensor);
		sensors.add(sensor);
		return sensor;
	}

	private void launchWater() {
		for (int x = 0; x < RIVER_WIDTH; x++) {
			for (int y = 0; y < RIVER_HEIGHT; y++) {
				Water waterCell = new Water(x, y);
				river.putObjectAt(x, y, waterCell);
				waterCells.add(waterCell);
			}
		}
	}

	
	//Pollution handling -----
	
	public void updateRiver() {
		double pollution;
		for (int x = 0; x < RIVER_WIDTH; x++) {
			for (int y = 0; y < RIVER_HEIGHT; y++) {
				pollution = pollutionFlow(x, y);
				((Water) river.getObjectAt(x, y)).setPollution(pollution);
			}
		}
	}

	//River flow
	private double pollutionFlow(int x, int y) {
		
		/* Pollution propagation
		 *  _____________
		 * |___|___|alpha|
		 * |___|POS|beta |
		 * |___|___|gamma|
		 */
		return 
				(1 - SEDIMENTATION) * pollutionAt(x, y) + 
				SEDIMENTATION * (
						ALPHA * pollutionAt(x + 1, y - 1) +
						BETA * pollutionAt(x + 1, y) + 
						GAMMA * pollutionAt(x + 1, y + 1));
	}

	public double pollutionAt(int x, int y) {
		
		double pollution = 0;

		//Scanning within river boundaries
		if (x >= 0 && x < RIVER_WIDTH && y >= 0 && y < RIVER_HEIGHT)
			pollution = ((Water) river.getObjectAt(x, y)).getPollution();

		//Scanning outside river boundaries
		else if (x == RIVER_WIDTH && Math.abs(POLLUTION_Y_VALUE - y) < RIVER_HEIGHT / 4) {
			float oscillation = (float) Math.sin(getTickCount() / 3);
			if (oscillation <= 0) pollution = 0;
			else pollution = oscillation * Water.MAX_POLLUTION;
		}

		return pollution;
	}
	
	//-----

	//Runs after playing simulation
	@Override
	public void begin() {
		buildModel();
		if (!BATCH_MODE) {
			buildDisplay();	
			buildSchedule();
		}
		super.begin();
	}

	//Create and store agents; Create space, data recorders
	private void buildModel() {
		river = new Object2DGrid(RIVER_WIDTH, RIVER_HEIGHT);
		waterCells = new ArrayList<Water>();
		sensors = new ArrayList<Sensor>();
	}

	//Create displays, charts
	private void buildDisplay() {
		//Water cells display
		Object2DDisplay waterDisplay = new Object2DDisplay(river);
		waterDisplay.setObjectList(waterCells);
		surface.addDisplayableProbeable(waterDisplay, "River");
		addSimEventListener(surface);

		//Sensors displlay
		Object2DDisplay sensorsDisplay = new Object2DDisplay(river);
		sensorsDisplay.setObjectList(sensors); 
		surface.addDisplayableProbeable(sensorsDisplay, "Sensors");
		addSimEventListener(surface);
		
		//Sink Node representation
		TextDisplay td = new TextDisplay(970, 30, Color.yellow); td.addLine("SN");
		surface.addDisplayableProbeable(td, "SN"); 
		
		surface.display();
		
		initBatteryPlot();
		initErrorPlot();
	}

	//Build the schedule
	private void buildSchedule() {
		getSchedule().scheduleActionBeginning(1, this, "updateRiver");
		getSchedule().scheduleActionAtInterval(1, surface, "updateDisplay", Schedule.LAST);
		getSchedule().scheduleActionAtInterval(1, batteryPlot, "step", Schedule.LAST);
		getSchedule().scheduleActionAtInterval(1, errorPlot, "step", Schedule.LAST);
	}
	
	
	private void initErrorPlot() {
		
		if (errorPlot != null) errorPlot.dispose();

		errorPlot = new OpenSequenceGraph("Error %", this);
		errorPlot.setAxisTitles("Time", "Error");

		errorPlot.addSequence("Error %", new Sequence() {

			@Override
			public double getSValue() {
				ArrayList<Double> errorPercentage = new ArrayList<Double>();

				errorPercentage.add(sinkNode.calcErrorPercentage());

				Collections.sort(errorPercentage);

				int listSize = errorPercentage.size();
				int listMiddle = listSize / 2;

				return listSize % 2 == 0 ? (errorPercentage.get(listMiddle) + errorPercentage.get(listMiddle - 1)) / 2
						: errorPercentage.get(listMiddle);
			}

		});

		errorPlot.display();
	}

	private void initBatteryPlot() {
		
		if (batteryPlot != null) batteryPlot.dispose();

		batteryPlot = new OpenSequenceGraph("Agents Battery Life", this);
		batteryPlot.setAxisTitles("Time", "Battery");

		batteryPlot.addSequence("Battery median", new Sequence() {

			@Override
			public double getSValue() {
				List<Double> batteryLevels = new ArrayList<Double>();

				for (Sensor sensor : sensors)
					batteryLevels.add(sensor.getBattery());

				Collections.sort(batteryLevels);

				int listSize = batteryLevels.size();
				int listMiddle = listSize / 2;

				return listSize % 2 == 0 ? (batteryLevels.get(listMiddle) + batteryLevels.get(listMiddle - 1)) / 2
						: batteryLevels.get(listMiddle);
			}

		});

		batteryPlot.display();
	}


	// -------------------------------------------------------------------------------------


	//Launching Repast3
	public static void main(String[] args) {
		SimInit init = new SimInit();
		init.setNumRuns(1);   // works only in batch mode
		init.loadModel(new SIMLauncher(), null, BATCH_MODE);
	}

	//Get and Set functions -----
	@Override
	public String[] getInitParam() {
		return new String[] {"RIVER_WIDTH", "RIVER_HEIGHT", "SCENARIO", "SEDIMENTATION", "ALPHA", "BETA", "GAMMA"};
	}	

	@Override
	public String getName() {
		return "WSN Optimization";
	}
	
	public Object2DGrid getRIVER() {
		return river;
	}

	public int getNUM_SENSORS() {
		return NUM_SENSORS;
	}

	public void setNUM_SENSORS(int NUM_SENSORS) {
		this.NUM_SENSORS = NUM_SENSORS;
	}

	public int getRIVER_WIDTH() {
		return RIVER_WIDTH;
	}

	public void setRIVER_WIDTH(int RIVER_WIDTH) {
		this.RIVER_WIDTH = RIVER_WIDTH;
	}

	public int getRIVER_HEIGHT() {
		return RIVER_HEIGHT;
	}

	public void setRIVER_HEIGHT(int RIVER_HEIGHT) {
		this.RIVER_HEIGHT = RIVER_HEIGHT;
	}

	public Scenario getSCENARIO() {
		return SCENARIO;
	}

	public void setSCENARIO(Scenario SCENARIO) {
		this.SCENARIO = SCENARIO;
	}
	
	public ArrayList<Sensor> getSENSORS() {
		return sensors;
	}
	
	public float getSEDIMENTATION() {
		return SEDIMENTATION;
	}
	
	public void setSEDIMENTATION(float val) {
		this.SEDIMENTATION = val;
	}
	
	public float getALPHA() {
		return ALPHA;
	}
	
	public void setALPHA(float val) {
		this.ALPHA = val;
	}
	
	public float getBETA() {
		return BETA;
	}
	
	public void setBETA(float val) {
		this.BETA = val;
	}
	
	public float getGAMMA() {
		return GAMMA;
	}
	
	public void setGAMMA(float val) {
		this.GAMMA = val;
	}
	
	public Color getSensorCoalitionColor(AID aid) {
		for (Sensor sensor : sensors)
			if (sensor.getAID().equals(aid))
				return sensor.getColor();
		return null;
	}
	//-----
}
