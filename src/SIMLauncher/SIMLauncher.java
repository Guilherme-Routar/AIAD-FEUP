package SIMLauncher;
import java.awt.Color;
import java.util.ArrayList;

import Agents.SensorAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;

import sajas.core.Agent;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.util.Random;

public class SIMLauncher extends Repast3Launcher {

	//Parameters
	private ContainerController mainContainer;
	private ArrayList<SensorAgent> sensorsList;
	//Simulation elements
	private Object2DGrid grid;
	private DisplaySurface surface;
	private OpenSequenceGraph plot;
	//Values
	private int NUM_SENSORS = 10;
	private int GRID_SIZE = 100;
	private static boolean BATCH_MODE = false;
	//-----

	public SIMLauncher() { 
		super(); 
	}
	
	//Get and Set functions -----
	public int getNUM_SENSORS() {
		return NUM_SENSORS;
	}
	
	public void setNUM_SENSORS(int NUM_SENSORS) {
		this.NUM_SENSORS = NUM_SENSORS;
	}
	
	public int getGRID_SIZE() {
		return GRID_SIZE;
	}
	
	public void setGRID_SIZE(int GRID_SIZE) {
		this.GRID_SIZE = GRID_SIZE;
	}
	//-----
	
	@Override
	public String[] getInitParam() {
		return new String[] {"NUM_SENSORS", "GRID_SIZE"};
	}
	
	@Override
	public String getName() {
		return "WSN Optimization";
	}
	
	@Override
	public void setup() {
		super.setup();
		
		if (surface != null) surface.dispose();

		surface = new DisplaySurface(this, "Sensors environment");
		registerDisplaySurface("Sensors environment", surface);
	}

	@Override
	protected void launchJADE() {

		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);

		launchSensors();
	}

	private void launchSensors() {

		try {
			int i;
			for (i = 0; i < NUM_SENSORS; i++) 
				mainContainer.acceptNewAgent("S" + (i + 1), createSensor()).start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	SensorAgent createSensor() {

		int x, y;
		do {
			x = Random.uniform.nextIntFromTo(0, grid.getSizeX() - 1);
			y = Random.uniform.nextIntFromTo(0, grid.getSizeY() - 1);
		} while (grid.getObjectAt(x, y) != null);

		Color color = new Color(Random.uniform.nextIntFromTo(0, 255), 
				Random.uniform.nextIntFromTo(0, 255), 
				Random.uniform.nextIntFromTo(0, 255));

		SensorAgent sensor = new SensorAgent(x, y, grid, color);
		grid.putObjectAt(x, y, sensor);
		sensorsList.add(sensor);

		return sensor;
	}

	//Runs after playing simulation
	@Override
	public void begin() {
		buildModel();
		buildDisplay();
		super.begin();	
		buildSchedule();
	}

	//Create and store agents; Create space, data recorders
	private void buildModel() {
		sensorsList = new ArrayList<SensorAgent>();
		grid = new Object2DGrid(GRID_SIZE, GRID_SIZE);
	}

	//Create displays, charts
	private void buildDisplay() {

		//Grid 
		Object2DDisplay sensorsDisplay = new Object2DDisplay(grid);
		sensorsDisplay.setObjectList(sensorsList);
		surface.addDisplayableProbeable(sensorsDisplay, "Sensors");
		addSimEventListener(surface);
		surface.display();

		//Graph
		plot = new OpenSequenceGraph("Number of Sensors", this);
		plot.setAxisTitles("time", "n");

		// plot number of different existing colors
		plot.addSequence("Number of sensors", new Sequence() {
			public double getSValue() {
				return sensorsList.size();
			}
		});
		plot.display();
	}

	//Build the schedule
	private void buildSchedule() {
		getSchedule().scheduleActionAtInterval(1, surface, "updateDisplay", Schedule.LAST);
		getSchedule().scheduleActionAtInterval(1, plot, "step", Schedule.LAST);
	}
	
	//Launching Repast3
	public static void main(String[] args) {
		SimInit init = new SimInit();
		init.setNumRuns(1);   // works only in batch mode
		init.loadModel(new SIMLauncher(), null, BATCH_MODE);
	}

}
