/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingSearchIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withinDay_v_STRC.identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.parking.core.events.ParkingSearchEvent;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.fullParkingStrategies.OptimalParkingStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.fullParkingStrategies.ParkAgentPaperStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;

public class ParkingSearchIdentifier_v2 extends DuringLegIdentifier implements MobsimInitializedListener {
	
	private final ParkingAgentsTracker_v2 parkingAgentsTracker;
	private final ParkingInfrastructure_v2 parkingInfrastructure;
	private final Map<Id, MobsimAgent> agents;
	private final EventsManager eventsManager;
	
	public ParkingSearchIdentifier_v2(ParkingAgentsTracker_v2 parkingAgentsTracker_v2, ParkingInfrastructure parkingInfrastructure, 
			LinkedList<FullParkingSearchStrategy> list, EventsManager eventsManager) {
		this.parkingAgentsTracker = parkingAgentsTracker_v2;
		this.parkingInfrastructure = (ParkingInfrastructure_v2) parkingInfrastructure;
		this.agents = new HashMap<Id, MobsimAgent>();
		this.eventsManager = eventsManager;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		/*
		 * Get all agents that are searching and have entered a new link in the last
		 * time step.
		 */
		Set<Id> linkEnteredAgents = this.parkingAgentsTracker.getLinkEnteredAgents();		
		Set<MobsimAgent> identifiedAgents = new HashSet<MobsimAgent>();
		
		for (Id agentId : linkEnteredAgents) {
			MobsimAgent agent = this.agents.get(agentId);
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
//				// say, that search time has started
//				// TODO: allow adding here custom code per strategy!!!!
//				HashMap<Id, Double> parkingSearchStartTime = ((ParkingScoreManager) this.parkingAgentsTracker).getParkingSearchStartTime();
//				if (!parkingSearchStartTime.containsKey(agentId)){
//					parkingSearchStartTime.put(agentId, time);
//				}
				
				
				
				FullParkingSearchStrategy parkingStrategyForCurrentLeg = getParkingStrategyForCurrentLeg(agent);
				this.eventsManager.processEvent(new ParkingSearchEvent(time, agent.getId(), parkingStrategyForCurrentLeg.getStrategyName()));
				
				if (parkingStrategyForCurrentLeg instanceof OptimalParkingStrategy){
					Id facilityId = ((OptimalParkingStrategy) parkingStrategyForCurrentLeg).reserveBestParking(agent, parkingAgentsTracker, time);
					
					if (facilityId != null) {
						parkingAgentsTracker.setSelectedParking(agentId, facilityId, false);
						identifiedAgents.add(agent);
						continue;
					}
				}
				
				if (parkingStrategyForCurrentLeg instanceof ParkAgentPaperStrategy){
					
					ParkAgentPaperStrategy parkAgentPaperStrategy = (ParkAgentPaperStrategy) parkingStrategyForCurrentLeg;
					
					parkAgentPaperStrategy.handleAgent(agent,time);
				}
				
				
				
				Id linkId = agent.getCurrentLinkId();
				List<Id> facilityIds = parkingInfrastructure.getFreeParkingFacilitiesOnLink(linkId);
				if (facilityIds != null && facilityIds.size() > 0) {
					Id facilityId = facilityIds.get(0);

					/*
					 * If the agent accepts the parking, select it.
					 * The parkingAgentsTracker then reserves the parking lot.
					 */
					if (acceptParking(agent, facilityId)) { 
						this.parkingAgentsTracker.setSelectedParking(agentId, facilityId, false);						
					}
				}
				
				identifiedAgents.add(agent);
			}
		}
		
		return identifiedAgents;
	}
	
	private boolean acceptParking(MobsimAgent agent, Id facilityId) {
		getParkingStrategyForCurrentLeg(agent).acceptParking(agent, facilityId);
		return true;
	}

	private FullParkingSearchStrategy getParkingStrategyForCurrentLeg(MobsimAgent agent) {
		return this.parkingAgentsTracker.getParkingStrategyManager().getParkingStrategyForCurrentLeg(agent);
	}
	
	/*
	 * If no parking is selected for the current agent, the agent requires
	 * a replanning.
	 */
	private boolean requiresReplanning(MobsimAgent agent) {
		return parkingAgentsTracker.getSelectedParking(agent.getId()) == null;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.agents.clear();
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), agent);
		}
	}

}