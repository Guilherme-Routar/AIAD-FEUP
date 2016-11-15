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
	private ArrayList<Object> riverCells; 
	//Simulation elements
	private Object2DGrid river;
	private DisplaySurface surface;
	private OpenSequenceGraph plot;
	//Values
	private int NUM_SENSORS = 10;
	private int RIVER_WIDTH = 100;
	private int RIVER_HEIGHT = 20;
	private static boolean BATCH_MODE = true;
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
			int i;
			for (i = 0; i < NUM_SENSORS; i++) 
				mainContainer.acceptNewAgent("S" + (i + 1), createSensor(i)).start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	Sensor createSensor(int pos) {

		int x, y;
		x = pos * (river.getSizeX() / getNUM_SENSORS());
		y = river.getSizeY() / 2;
		
		Color color = new Color(Random.uniform.nextIntFromTo(0, 255), 
				Random.uniform.nextIntFromTo(0, 255), 
				Random.uniform.nextIntFromTo(0, 255));
		
		Sensor sensor = new Sensor(x, y, river, color);
		river.putObjectAt(x, y, sensor);
		riverCells.add(sensor);

		return sensor;
	}
	
	private void launchWater() {
		
		for (int i = 0; i < RIVER_WIDTH; i++) {
			for (int j = 0; j < RIVER_HEIGHT; j++) {
				Water waterCell = new Water(i,j);
				river.putObjectAt(i, j, waterCell);
				riverCells.add(waterCell);
			}
		}
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
		riverCells = new ArrayList<Object>();
		river = new Object2DGrid(RIVER_WIDTH, RIVER_HEIGHT);
	}

	//Create displays, charts
	private void buildDisplay() {

		//Grid 
		Object2DDisplay sensorsDisplay = new Object2DDisplay(river);
		sensorsDisplay.setObjectList(riverCells);
		surface.addDisplayableProbeable(sensorsDisplay, "Sensors");
		addSimEventListener(surface);
		surface.display();

		//Graph
		plot = new OpenSequenceGraph("Number of Sensors", this);
		plot.setAxisTitles("time", "n");

		// plot number of different existing colors
		plot.addSequence("Number of sensors", new Sequence() {
			public double getSValue() {
				return riverCells.size();
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
