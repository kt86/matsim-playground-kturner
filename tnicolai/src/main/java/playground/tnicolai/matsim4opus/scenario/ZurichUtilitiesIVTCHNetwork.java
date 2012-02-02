package playground.tnicolai.matsim4opus.scenario;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

public class ZurichUtilitiesIVTCHNetwork extends ZurichUtilities{
	
	/** logger */
	private static final Logger log = Logger.getLogger(ZurichUtilitiesIVTCHNetwork.class);
	
	/**
	 * This modifies the MATSim network according to the given test parameter in
	 * the MATSim config file (from UrbanSim)
	 */
	public static void modifyNetwork(final Network network, final String[] scenarioArray) {

		for (int i = 0; i < scenarioArray.length; i++) {

			if (scenarioArray[i].equalsIgnoreCase(CLOSE_SCHWAMENDINGERTUNNEL))
				removeSchwamendingerTunnel(network);
			else 
				log.error("Identifier " + scenarioArray[i] + " not found. This means that no road or tunnel will be closed!");
		}
	}
	
	/**
	 * removes the Schwamendingertunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeSchwamendingerTunnel(final Network network) {
		log.info("Closing/removing Schwamendingertunnel from IVTCH-OSM network ...");

		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(103727));
		linksToRemove.add(new IdImpl(103728));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Schwamendingertunnel!");
	}
}
