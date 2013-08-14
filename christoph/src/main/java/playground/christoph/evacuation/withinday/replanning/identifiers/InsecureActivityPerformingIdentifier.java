/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureActivityPerformingIdentifiers.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

import playground.christoph.evacuation.config.EvacuationConfig;

public class InsecureActivityPerformingIdentifier extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(InsecureActivityPerformingIdentifier.class);
	
	protected ActivityReplanningMap activityReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
	protected WithinDayAgentUtils withinDayAgentUtils;
		
	/*package*/ InsecureActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
		this.withinDayAgentUtils = new WithinDayAgentUtils();
	}
	
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		Set<Id> activityPerformingAgents = activityReplanningMap.getActivityPerformingAgents();
		Map<Id, MobsimAgent> mapping = activityReplanningMap.getPersonAgentMapping();
		
		// apply filter to remove agents that should not be replanned
		this.applyFilters(activityPerformingAgents, time);
		
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());
		
		for (Id id : activityPerformingAgents) {
			
			MobsimAgent agent = mapping.get(id);
			
			/*
			 *  Get the current PlanElement and check if it is an Activity
			 */
			Activity currentActivity;
			PlanElement currentPlanElement = this.withinDayAgentUtils.getCurrentPlanElement(agent);
			if (currentPlanElement instanceof Activity) {
				currentActivity = (Activity) currentPlanElement;
			} else continue;
			/*
			 * Remove the Agent from the list, if the performed Activity is in a secure Area.
			 */
			double distance = CoordUtils.calcDistance(currentActivity.getCoord(), centerCoord);
			if (distance > secureDistance) {
				continue;
			}
			
			/*
			 * Add the Agent to the Replanning List
			 */
			agentsToReplan.add(agent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + activityPerformingAgents.size() + " Agents performing an Activity in an insecure area.");
		
		return agentsToReplan;
	}

}
