/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.incidentWithinDayReplanning;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import playground.ikaddoura.incidents.NetworkChangeEventsUtils;

/**
* @author ikaddoura
* 
*/
public class IncidentWithinDayReplanning {

// ############################################################################################################################################

	private final String day = "2016-02-11";
//	private final String day = "2016-03-15";
	
	private final String configFile = "/Users/ihab/Documents/workspace/runs-svn/incidents-longterm-shortterm/input/config_short-term.xml";
	private final String runOutputBaseDirectory = "/Users/ihab/Documents/workspace/runs-svn/incidents-longterm-shortterm/output/output_departureTimeInterval_";
	private final String runId = "run1";
	
	private final boolean reducePopulationToAffectedAgents = false;
	private final String reducedPopulationFile = "path-to-reduced-population.xml.gz";
	
	private final boolean applyNetworkChangeEvents = true;
	private final boolean applyWithinDayReplanning = true;
	private final int withinDayReplanInterval = 300;
	
	private final String crs = TransformationFactory.DHDN_GK4;
		
// ############################################################################################################################################
	
	private final Logger log = Logger.getLogger(IncidentWithinDayReplanning.class);
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			throw new RuntimeException("Not implemented. Aborting...");
		}
		
		IncidentWithinDayReplanning incidentWithinDayReplanning = new IncidentWithinDayReplanning();
		incidentWithinDayReplanning.run();
	}

	private void run() {
		
		final Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setRunId(runId);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);

		config.controler().setOutputDirectory(runOutputBaseDirectory + day
				+ "_networkChangeEvents-" + applyNetworkChangeEvents
				+ "_withinDayReplanning-" + applyWithinDayReplanning
				+ "_replanInterval-" + withinDayReplanInterval
				+ "/");
		
		if (applyNetworkChangeEvents) {
			if (config.network().getChangeEventsInputFile() == null) {
				throw new RuntimeException("No network change events file provided. Aborting...");
			}
			config.network().setTimeVariantNetwork(true);			
		} else {
			log.info("Not considering any network change events.");
			config.network().setChangeEventsInputFile(null);
			config.network().setTimeVariantNetwork(false);			
		}
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		
		if (reducePopulationToAffectedAgents) {
			log.warn("Reduced population should only be used for testing purposes.");
			
			log.info("Reducing the population size from " + scenario.getPopulation().getPersons().size() + "...");

			Set<Id<Link>> incidentLinkIds = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
			Set<Id<Person>> personIdsToKeepInPopulation = NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, incidentLinkIds);
			NetworkChangeEventsUtils.filterPopulation(scenario, personIdsToKeepInPopulation);

			log.info("... to " + scenario.getPopulation().getPersons().size() + " agents (= those agents driving along incident links).");
			PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
			writer.write(reducedPopulationFile);
		} else {
			log.info("Using the normal population.");
		}
		
		if (applyWithinDayReplanning) {
			
			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final WithinDayTravelTime travelTime = new WithinDayTravelTime(controler.getScenario(), analyzedModes);
		
//			Set<Id<Link>> links = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
//			Set<Id<Person>> personIds = NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, links);
			Set<Id<Person>> personIds = scenario.getPopulation().getPersons().keySet();
			
			WithinDayReplanningDepartureTimeIntervals incidentMobsimListener = new WithinDayReplanningDepartureTimeIntervals(personIds, withinDayReplanInterval);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
					
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					this.addEventHandlerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);			
				}
			}) ;
			
		} else {
			if (applyNetworkChangeEvents) {
				log.warn("Applying network change events without within-day replanning.");
			}
		}
				
		controler.run();		
	}
	
}

