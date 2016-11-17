package SIMLauncher;
import java.awt.Color;
import java.util.ArrayList;
import Agents.Sensor;
import Environment.Water;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.util.Random;

public class SIMLauncher extends Repast3Launcher {

	//Parameters
	private ContainerController mainContainer;
	private ArrayList<Water> waterCells; 
	private ArrayList<Sensor> sensors;
	//Simulation elements
	private Object2DGrid river;
	private DisplaySurface surface;
	//private OpenSequenceGraph plot;
	//Values
	private int NUM_SENSORS;
	private static int CELLS_PER_KM = 5;
	private int RIVER_WIDTH = 50 * CELLS_PER_KM;
	private int RIVER_HEIGHT = 2 * CELLS_PER_KM;
	private static boolean BATCH_MODE = true;
	//Scenarios (Sensors allocation throughout the river)
	private String allocation;
	public enum Scenarios {
		CHAINALONGRIVER, ENDOFRIVER, RANDOM
	};
	//-----

	public SIMLauncher() { 
		super(); 

		//Sensors allocation throughout the river
		Scenarios scenario = Scenarios.ENDOFRIVER;
		if (scenario.equals(Scenarios.ENDOFRIVER)) setNUM_SENSORS(30);
		else setNUM_SENSORS(50);
		allocation = scenario.toString();
	}

	//Get and Set functions -----
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
	//-----

	@Override
	public String[] getInitParam() {
		return new String[] {"NUM_SENSORS", "RIVER_WIDTH", "RIVER_HEIGHT"};
	}	

	@Override
	public String getName() {
		return "WSN Optimization";
	}

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

		launchWater();
		launchSensors();
	}

	private void launchSensors() {

		try {
			Sensor sensor;
			int y, x;
			if (allocation == "CHAINALONGRIVER") {
				for (int i = 0; i < NUM_SENSORS; i++) {
					x = i * (river.getSizeX() / getNUM_SENSORS());
					y = river.getSizeY() / 2;
					sensor = allocateSensor(x, y);
					mainContainer.acceptNewAgent("S-" + i, sensor).start();
				}
			}
			else if (allocation == "ENDOFRIVER") {
				for (int i = 0; i < NUM_SENSORS / 3; i++) {
					for (int j = 0; j < 3; j++) {
						x = (i * (river.getSizeX() / getNUM_SENSORS())) / 4;
						y = (j * 3) + 2;
						sensor = allocateSensor(x, y);
						mainContainer.acceptNewAgent("S-" + (i * 3 + j), sensor).start();
					}
				}
			}
			else if (allocation == "RANDOM") {
				for (int i = 0; i < NUM_SENSORS; i++) {
					x = Random.uniform.nextIntFromTo(0, river.getSizeX() - 1);
					y = Random.uniform.nextIntFromTo(0, river.getSizeY() - 1);
					sensor = allocateSensor(x, y);
					mainContainer.acceptNewAgent("S-" + i, sensor).start();
				}
			}
		}
		catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	private Sensor allocateSensor(int x, int y) {

		Sensor sensor = new Sensor(x, y, river, Color.BLACK);
		river.putObjectAt(x, y, sensor);
		sensors.add(sensor);
		return sensor;
	}

	private void launchWater() {

		for (int i = 0; i < RIVER_WIDTH; i++) {
			for (int j = 0; j < RIVER_HEIGHT; j++) {
				Water waterCell = new Water(i,j);
				river.putObjectAt(i, j, waterCell);
				waterCells.add(waterCell);
			}
		}
	}

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
		waterCells = new ArrayList<Water>();
		sensors = new ArrayList<Sensor>();
		river = new Object2DGrid(RIVER_WIDTH, RIVER_HEIGHT);
	}

	//Create displays, charts
	private void buildDisplay() {

		Object2DDisplay waterDisplay = new Object2DDisplay(river);
		waterDisplay.setObjectList(waterCells);
		surface.addDisplayableProbeable(waterDisplay, "River");
		addSimEventListener(surface);

		Object2DDisplay sensorsDisplay = new Object2DDisplay(river);
		sensorsDisplay.setObjectList(sensors);
		surface.addDisplayableProbeable(sensorsDisplay, "Sensors");
		addSimEventListener(surface);

		surface.display();

		//Graph
		/*
		plot = new OpenSequenceGraph("Number of River Cells", this);
		plot.setAxisTitles("time", "n");

		// plot number of different existing colors
		plot.addSequence("River Cells", new Sequence() {
			public double getSValue() {
				return riverCells.size();
			}
		});
		plot.display();
		 */
	}

	//Build the schedule
	private void buildSchedule() {
		getSchedule().scheduleActionAtInterval(1, surface, "updateDisplay", Schedule.LAST);
		//getSchedule().scheduleActionAtInterval(1, plot, "step", Schedule.LAST);
	}

	//Launching Repast3
	public static void main(String[] args) {
		SimInit init = new SimInit();
		init.setNumRuns(1);   // works only in batch mode
		init.loadModel(new SIMLauncher(), null, BATCH_MODE);
	}
}
