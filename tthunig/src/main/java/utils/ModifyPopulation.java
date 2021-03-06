/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * Class to modify a population file.
 * It removes all link information of activities and all routes.
 * 
 * @author tthunig
 */
public class ModifyPopulation {

//	private static final String INPUT_BASE_DIR = "../../runs-svn/berlin_scenario_2016/be_218/";
//	private static final String INPUT_BASE_DIR = "../../runs-svn/cottbus/createNewBC/2018-11-19-12-14-4_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap07/";
	private static final String INPUT_BASE_DIR = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/cb_spn_gemeinde_nachfrage_landuse_woMines/";
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(INPUT_BASE_DIR + "be_218.output_network.xml.gz");
//		config.plans().setInputFile(INPUT_BASE_DIR + "be_218.output_plans.xml.gz");
		
		config.global().setCoordinateSystem("EPSG:25833");
//		config.network().setInputFile(INPUT_BASE_DIR + "1000.output_network.xml.gz");
//		config.plans().setInputFile(INPUT_BASE_DIR + "1000.output_plans.xml.gz");
		config.plans().setInputFile(INPUT_BASE_DIR + "commuter_population_wgs84_utm33n.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		new PopulationWriter(removeRoutesLeaveFirstPlan(scenario.getPopulation())).write(INPUT_BASE_DIR + "plans_woRoutes.xml");	
		
//		new PopulationWriter(removeAllLinkInfos(scenario.getPopulation())).write(INPUT_BASE_DIR + "commuter_population_wgs84_utm33n_woLinks.xml.gz");		
		
//		new PopulationWriter(onlyKeepSelectedPlanAndCarUsers(scenario.getPopulation())).write(INPUT_BASE_DIR + "be_218.output_plans_selected_carOnly.xml.gz");
		
//		new PopulationWriter(onlyKeepMorningPeak(scenario.getPopulation())).write(INPUT_BASE_DIR + "1000.output_plans_morningPeak.xml.gz");
	}
	
	public static Population onlyKeepMorningPeak(Population population) {
		Population newPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		// remove work-home trips. keep only home-work as dummy-dummy
		for (Person p : population.getPersons().values()){
			Person newP = newPop.getFactory().createPerson(p.getId());
			newPop.addPerson(newP);
			Plan reducedPlan = newPop.getFactory().createPlan();
			newP.addPlan(reducedPlan);
			
			// consider only the selected plan
			Plan plan = p.getSelectedPlan();			
			boolean firstLeg = true;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.getType().equals("home") && firstLeg) {
						act.setType("dummy");
						reducedPlan.addActivity(act);
					} else if (act.getType().equals("work")) {
						act.setType("dummy");
						reducedPlan.addActivity(act);
					}
				} else if (pe instanceof Leg && firstLeg){
					reducedPlan.addLeg((Leg) pe);
					
					firstLeg = false;
				}
			}
		}
		return newPop;
	}
	
	public static Population onlyKeepSelectedPlanAndCarUsers(Population population){
		Population carPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (Person p : population.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(TransportMode.car)){
						Person carP = carPop.getFactory().createPerson(p.getId());
						carP.addPlan(plan);
						carPop.addPerson(carP);
						break;
					}
				}
			}
		}
		return carPop;
	}
	
	public static Population removeAllLinkInfos(Population population){
		for (Person p : population.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						// remove link
						((Activity) pe).setLinkId(null);
					}
					if (pe instanceof Leg) {
						// remove route
						((Leg) pe).setRoute(null);
					}
				}
			}
		}
		return population;
	}
	
	public static Population removeRoutesLeaveFirstPlan(Population population){
		
		for (Person person : population.getPersons().values()){
			boolean firstPlanHandled = false;
			for (Plan plan : person.getPlans()){
				// remove all plans except the first
				if (firstPlanHandled){
					person.removePlan(plan);
				} else {
					// remove route of the first plan
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Leg) {
							((Leg) pe).setRoute(null);
						}
					}
				}
			}
		}
		return population;
	}
	
	public static void copyEachPerson(Population originalPop, int numberOfCopiesPerPerson){
		Population scaledPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory fac = scaledPop.getFactory();
		
		// create copies of each person and save them in the new population object
		for (Person originalPerson : originalPop.getPersons().values()){
			for (int i = 1; i <= numberOfCopiesPerPerson; i++) {
				Person clone = fac.createPerson(Id.createPersonId(originalPerson.getId() + "_" + (i+1)));
				for (Plan plan : originalPerson.getPlans()) {
					throw new UnsupportedOperationException("Plan Utils have been used here but import does not work anymore. Fix me!");
//					clone.addPlan(PlanUtils.createCopy(plan));
				}
				scaledPop.addPerson(clone);
			}
		}
		
		// add all the copied persons to the original population object
		for (Person clone : scaledPop.getPersons().values()){
			originalPop.addPerson(clone);
		}
	}

}
