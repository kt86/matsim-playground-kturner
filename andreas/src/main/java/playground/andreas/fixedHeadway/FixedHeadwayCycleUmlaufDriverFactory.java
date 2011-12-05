package playground.andreas.fixedHeadway;

import org.matsim.pt.Umlauf;
import org.matsim.pt.qsim.AbstractTransitDriver;
import org.matsim.pt.qsim.AbstractTransitDriverFactory;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;

/**
 * @author aneumann
 */
public class FixedHeadwayCycleUmlaufDriverFactory implements AbstractTransitDriverFactory {

	@Override
	public AbstractTransitDriver createTransitDriver(Umlauf umlauf, TransitStopAgentTracker thisAgentTrackerVehicle, MobsimEngine trEngine) {
		return new FixedHeadwayCycleUmlaufDriver(umlauf, thisAgentTrackerVehicle, trEngine);
	}

}
