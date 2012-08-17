/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.southAfrica.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;

import playground.droeder.southAfrica.FixedPtSubModeControler;

/**
 * @author droeder
 *
 */
public class ReRouteFixedPtSubModeStrategy implements PlanStrategyModule{
	private Controler c;
	
	//TODO[dr] make it multithreadded again!
	/**
	 * Main <code>PlanStrategyModule</code> for <code>PlanStrategy</code> <code>PlanStrategyReRoutePtFixedSubMode</code>.
	 * Aborts if the controler is not an instance of instance of <code>FixedPtSubModeControler</code>
	 * @param c
	 */
	public ReRouteFixedPtSubModeStrategy(Controler c) {
//		super(c.getConfig().global());
		if(!(c instanceof FixedPtSubModeControler)){
			throw new IllegalArgumentException("If you want to use this replanning-strategy you are forced to use the PtSubModeControler...");
		}
		this.c = c;
	}
//
//	@Override
//	public PlanAlgorithm getPlanAlgoInstance() {
//		return this.c.createRoutingAlgorithm();
//	}

	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlePlan(Plan plan) {
		this.c.createRoutingAlgorithm().run(plan);
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub
		
	}

}
