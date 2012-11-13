/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.analysis.modules.bvgAna.anaLevel2.agentId2stopDifference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.collections.Tuple;

import playground.vsp.analysis.modules.bvgAna.anaLevel0.AgentId2PlannedDepartureTimeMapData;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.PersonId2DelayAtStopData;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.PersonId2DelayAtStopHandler;

/**
 * Calculates the difference between realized time and planned time spent waiting at a stop for a given set of agent ids.<br>
 * Returns negative values for agents being faster than planned.<br>
 * Returns positive values for agents being slower than planned.<br>
 * <br>
 * Realized time: PersonEntersVehicleEvent minus AgentDepartureEvent "pt interaction"<br>
 * Planned time: vehicle departs as scheduled minus "pt interaction" activity ends
 *
 * @author aneumann
 *
 */
public class AgentId2StopDifferenceAnalysis {

	private final Logger log = Logger.getLogger(AgentId2StopDifferenceAnalysis.class);
	private final Level logLevel = Level.OFF;

	private Population pop;
	private Map<Id, List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>> plannedDepartureTimeMap;
	private VehicleDeparturesAnalysis vehDepartures;
	private PersonId2DelayAtStopHandler agentDelayHandler;
	private Map<Id,List<Tuple<Id,Double>>> agentId2StopDifferenceMap = null;
	private Map<Id,List<Tuple<Id,Integer>>> agentIds2MissedVehMap = null;

	public AgentId2StopDifferenceAnalysis(Population pop, Map<Id, List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>>> plannedDepartureTimeMap, VehicleDeparturesAnalysis vehDepartures, PersonId2DelayAtStopHandler personDelayHandler){
		this.log.setLevel(this.logLevel);
		this.pop = pop;

		this.agentDelayHandler = personDelayHandler;
		this.vehDepartures = vehDepartures;
		this.plannedDepartureTimeMap = plannedDepartureTimeMap;
	}

	private void compare(){
		this.agentId2StopDifferenceMap = new TreeMap<Id,List<Tuple<Id,Double>>>();
		this.agentIds2MissedVehMap = new TreeMap<Id, List<Tuple<Id,Integer>>>();

		for (Person person : this.pop.getPersons().values()) {
			Id agentId = person.getId();
			
			if(this.agentId2StopDifferenceMap.get(agentId) == null){
				this.agentId2StopDifferenceMap.put(agentId, new ArrayList<Tuple<Id,Double>>());
			}

			if(this.agentIds2MissedVehMap.get(agentId) == null){
				this.agentIds2MissedVehMap.put(agentId, new ArrayList<Tuple<Id,Integer>>());
			}

			List<Tuple<Id,Double>> agentsDiffs = this.agentId2StopDifferenceMap.get(agentId);
			List<Tuple<Id, Integer>> agentsMissedVehicles = this.agentIds2MissedVehMap.get(agentId);
			List<Tuple<Id, AgentId2PlannedDepartureTimeMapData>> plannedDepartures = this.plannedDepartureTimeMap.get(agentId);

			for (int i = 0; i < plannedDepartures.size(); i++) {

				Id stopId = plannedDepartures.get(i).getFirst();
				AgentId2PlannedDepartureTimeMapData depContainer = plannedDepartures.get(i).getSecond();
				double plannedDepartureTime = depContainer.getPlannedDepartureTime();

				if (depContainer.getStopId() == null) {
					continue; // likely, no route could be calculated, thus this information is missing
				}

				// get next possible departure as scheduled
				double nextPlannedVehDeparture = this.vehDepartures.getNextPlannedDepartureTime(stopId, plannedDepartureTime, depContainer.getLineId(), depContainer.getRouteId());

				// calculate resulting time spent waiting
				double plannedTimeWaiting = nextPlannedVehDeparture - plannedDepartureTime;

				// get realized values
				PersonId2DelayAtStopData agentData = this.agentDelayHandler.getPersonId2DelayAtStopMap().get(agentId);
				if (agentData == null){
					continue;
				}
				if(agentData.getAgentDepartsPTInteraction().size() == 0 || this.agentDelayHandler.getPersonId2DelayAtStopMap().get(agentId).getAgentEntersVehicle().size() == 0){
					break;
				}
				if (agentData.getAgentDepartsPTInteraction().size() <= i || agentData.getAgentEntersVehicle().size() <= i) {
					break; // agent could realize less legs than planned
				}
				double realizedDepartureTime = agentData.getAgentDepartsPTInteraction().get(i).doubleValue();
				double realizedVehEntersTime = agentData.getAgentEntersVehicle().get(i).doubleValue();

				// calculate resulting time spent waiting
				this.log.debug("Realized time waiting at the stop is counted by calculating \"agent enters vehicle\" minus \"agent departs pt interaction\"." +
						"Maybe this should be done by taking the corresponding VehicleDepartsAtFacilityEvent instead of the PersonEntersVehicleEvent.");
				double realizedTimeWaiting = realizedVehEntersTime - realizedDepartureTime;

				// calculate difference, so that agents faster than planned get negative values and slower agents positive values
				double difference = realizedTimeWaiting - plannedTimeWaiting;

				// put the resulting difference in the map
				agentsDiffs.add(new Tuple<Id, Double>(stopId, new Double(difference)));

				//--------------
				// Calculate missed vehicle
				agentsMissedVehicles.add(new Tuple<Id, Integer>(stopId, new Integer(this.vehDepartures.getNumberOfMissedVehicles(stopId, plannedDepartureTime, realizedDepartureTime, depContainer.getLineId(), depContainer.getRouteId()))));
			}
		}
	}

	/**
	 * Returns resulting difference between planned and realized time spent waiting at each stop
	 *
	 * @return A map containing a list of the resulting difference for each agent
	 */
	public Map<Id,List<Tuple<Id, Double>>> getAgentId2StopDifferenceMap(){
		if(this.agentId2StopDifferenceMap == null){
			compare();
		}
		return this.agentId2StopDifferenceMap;
	}

	/**
	 * Returns the number of vehicles the agent missed or took earlier than scheduled for each stop
	 *
	 * @return A map containing a list of the resulting number of stops for each agent
	 */
	public Map<Id, List<Tuple<Id, Integer>>> getNumberOfMissedVehiclesMap(){
		if(this.agentIds2MissedVehMap == null){
			compare();
		}
		return this.agentIds2MissedVehMap;
	}
}
