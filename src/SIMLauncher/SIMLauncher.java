package SIMLauncher;
import Agents.SensorAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;

import sajas.core.Agent;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;

import uchicago.src.sim.engine.SimInit;

public class SIMLauncher extends Repast3Launcher {
	
	//Parameters
	private ContainerController mainContainer;
	private int numOfSensors = 10;
	//-----
	
	@Override
	public String[] getInitParam() {
		return new String[0];
	}

	@Override
	public String getName() {
		return "WSN Optimization";
	}

	@Override
	protected void launchJADE() {
		
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);
		
		launchAgents();
	}
	
	private void launchAgents() {
		
		try {
			
			int i;
			for (i = 0; i < numOfSensors; i++) {
				SensorAgent agent = new SensorAgent();
				mainContainer.acceptNewAgent("S" + (i + 1), agent).start();
			}
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void setup() {
		super.setup();
	}

	//Runs after playing simulation
	@Override
	public void begin() {
		super.begin();		
	}

	/**
	 * Launching Repast3
	 * @param args
	 */
	public static void main(String[] args) {
		boolean BATCH_MODE = false; //false - gui sim enabled
		
		SimInit init = new SimInit();
		init.setNumRuns(1);   // works only in batch mode
		init.loadModel(new SIMLauncher(), null, BATCH_MODE);
	}

}
