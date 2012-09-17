/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.router.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * @author thibautd
 */
public class TimeAllocationMutatorModule extends AbstractMultithreadedModule {
	private final MultiLegRoutingControler controler;
	private final StageActivityTypes blackList;

	private final double mutationRange;
	private boolean useActivityDurations = true;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange"),
	 * or the default value of 1800 (seconds) if there is no value specified in
	 * the configuration.
	 *
	 * @param controler the controler from which to get the config and the {@link StageActivityTypes}
	 */
	public TimeAllocationMutatorModule(final Controler controler) {
		this( controler , null );
	}

	/**
	 * @param controler the controler from which to get the config
	 * @param blackList the {@link StageActivityTypes}. If null, an instance is obtained
	 * from the {@link TripRouter} created by the controler's factory when needed.
	 */
	public TimeAllocationMutatorModule(final Controler controler , final StageActivityTypes blackList) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;
		this.blackList = blackList;

		Config config = controler.getConfig();
		this.mutationRange = config.timeAllocationMutator().getMutationRange();

		if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.minOfDurationAndEndTime ) ) {
			useActivityDurations = true ;
		}
		else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.endTimeOnly ) ) {
			useActivityDurations = false ;
		}
		else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.tryEndTimeThenDuration ) ) {
			throw new UnsupportedOperationException( "need to clarify the correct setting here.  Probably not a big deal, but not done yet.  kai, aug'10") ;
		}
		else {
			throw new IllegalStateException( "beahvior not defined for this configuration setting") ;
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		BlackListedTimeAllocationMutator mutator =
			new BlackListedTimeAllocationMutator(
					blackList != null ? blackList : controler.getTripRouterFactory().createTripRouter().getStageActivityTypes(),
					this.mutationRange,
					MatsimRandom.getLocalInstance());
		mutator.setUseActivityDurations(this.useActivityDurations);
		return mutator;
	}
}
