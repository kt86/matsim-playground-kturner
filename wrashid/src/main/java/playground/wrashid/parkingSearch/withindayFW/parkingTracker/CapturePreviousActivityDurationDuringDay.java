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

package playground.wrashid.parkingSearch.withindayFW.parkingTracker;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;

import playground.wrashid.lib.GeneralLib;

//If several activities between two parking activities, count sum of all activities

public class CapturePreviousActivityDurationDuringDay implements ActivityStartEventHandler, ActivityEndEventHandler {
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Integer> firstParkingActivityPlanElemIndex;
	private Map<Id, Integer> lastParkingActivityPlanElemIndex;
	private Map<Id, Double> activityDurationTmpValue;

	public CapturePreviousActivityDurationDuringDay(Map<Id, ExperimentalBasicWithindayAgent> agents,
			Map<Id, Integer> firstParkingActivityPlanElemIndex, Map<Id, Integer> lastParkingActivityPlanElemIndex) {
		this.agents = agents;
		this.firstParkingActivityPlanElemIndex = firstParkingActivityPlanElemIndex;
		this.lastParkingActivityPlanElemIndex = lastParkingActivityPlanElemIndex;
		this.activityDurationTmpValue = new HashMap<Id, Double>();
	}

	// Precondition: only invoke during scoring (at the end of the parking
	// activity)
	public Double getDuration(Id personId) {
		return activityDurationTmpValue.get(personId);
	}

	@Override
	public void reset(int iteration) {
		activityDurationTmpValue.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (isPlanElementDuringDay(personId, planElementIndex)) {
			Activity nextAct = (Activity) executedPlan.getPlanElements().get(planElementIndex + 2);

			if (nextAct.getType().equals("parking")) {
				activityDurationTmpValue.put(personId,
						GeneralLib.getIntervalDuration(activityDurationTmpValue.get(personId), event.getTime()));
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();

		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		if (isPlanElementDuringDay(personId, planElementIndex)) {
			Activity previousAct = (Activity) executedPlan.getPlanElements().get(planElementIndex - 2);

			if (previousAct.getType().equals("parking")) {
				double activityStartTime = event.getTime();
				activityDurationTmpValue.put(personId, activityStartTime);
			}

		}

	}

	private boolean isPlanElementDuringDay(Id personId, int planElementIndex) {
		return planElementIndex > firstParkingActivityPlanElemIndex.get(personId)
				&& planElementIndex < lastParkingActivityPlanElemIndex.get(personId);
	}
}
