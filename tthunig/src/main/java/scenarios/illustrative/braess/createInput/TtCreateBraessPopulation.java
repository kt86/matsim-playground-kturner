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
package scenarios.illustrative.braess.createInput;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

/**
 * Class to create a population for the braess scenario.
 * 
 * Choose the number of persons you like to simulate, 
 * their starting times and their initial routes 
 * before calling the method createPersons(...)
 * 
 * @author tthunig
 */
public final class TtCreateBraessPopulation {
	
	/** ALL means: initialize all 3 routes, select the outer ones */
	public enum InitRoutes{
		ALL, ONLY_MIDDLE, ONLY_OUTER, NONE, EVERY_FOURTH_Z_REST_BYPASS, EVERY_SECOND_OUTER_REST_BYPASS
	}
	
	private Population population;
	private Network network;
	
	private int numberOfPersons; // per hour
	private int simulationPeriod = 1; // in hours. default is one hour.
	private double simulationStartTime = 0.0; // seconds from midnight. default is midnight 
	
	private boolean simulateInflowCap23 = false;
	private boolean simulateInflowCap24 = false;
	private boolean simulateInflowCap45 = false;
	private boolean middleLinkExists = true;
	
	private boolean writePopFile = false;
	private String pathToPopFile = "C:/Users/Theresa/Desktop/braess/plans3600.xml";
	
	public TtCreateBraessPopulation(Population pop, Network net) {
		this.population = pop;
		this.network = net;
		
		checkNetworkProperties();
	}

	/**
	 * Checks several properties of the network.
	 */
	private void checkNetworkProperties() {
		
		// check whether the network simulates inflow capacity
		if (this.network.getNodes().containsKey(Id.createNodeId(23)))
			this.simulateInflowCap23 = true;
		if (this.network.getNodes().containsKey(Id.createNodeId(24)))
			this.simulateInflowCap24 = true;
		if (this.network.getNodes().containsKey(Id.createNodeId(45)))
			this.simulateInflowCap45 = true;
		
		// check whether the network contains the middle link
		if (!this.network.getLinks().containsKey(Id.createLinkId("3_4")))
			this.middleLinkExists = false;
	}

	/**
	 * Fills a population container with the given number of persons. All
	 * persons travel from the left to the right through the network as in
	 * Braess's original paradox.
	 * 
	 * All agents start uniformly distributed between simulationStartTime and 
	 * (simulationStartTime + simulationPeriod) am.
	 * 
	 * If initRouteSpecification is NONE, all agents are initialized with no initial
	 * routes. 
	 * If it is ONLY_MIDDLE, all agents are initialized with the middle route.
	 * If it is ALL they are initialized with all three routes in this
	 * scenario, whereby every second agent gets the upper and every other agent
	 * the lower route as initial selected route. 
	 * If it is ONLY_OUTER, all agents are initialized with both outer routes, 
	 * whereby they are again alternately selected.
	 * 
	 * @param initRouteSpecification
	 *            specification which routes should be used as initial routes
	 *            (see enum RouteInitialization for the possibilities)
	 * @param initPlanScore
	 *            initial score for all plans the persons will get. Use null for
	 *            no scores.
	 */
	public void createPersons(InitRoutes initRouteSpecification, Double initPlanScore) {
		
		if (!this.middleLinkExists && 
				(initRouteSpecification.equals(InitRoutes.ONLY_MIDDLE) || initRouteSpecification.equals(InitRoutes.EVERY_FOURTH_Z_REST_BYPASS))){
			throw new IllegalArgumentException("You are trying to create agents with an initial middle route, although no middle link exists.");
		}
		
		for (int i = 0; i < this.numberOfPersons * this.simulationPeriod; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(i));

			// create a start activity at link 0_1
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("0_1"));
			// distribute agents uniformly between simulationStartTime and (simulationStartTime + simulationPeriod) am.
			startAct.setEndTime(simulationStartTime + (double)(i)/numberOfPersons * 3600);
		
			// create a drain activity at link 5_6
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("5_6"));
			
			// create a dummy leg
			Leg leg1 = population.getFactory().createLeg(TransportMode.car);
			// fill the leg if necessary
			switch (initRouteSpecification){
			case ONLY_MIDDLE:
			case EVERY_FOURTH_Z_REST_BYPASS:
			case ALL:
				leg1 = createMiddleLeg();
				break;
			case ONLY_OUTER:
			case EVERY_SECOND_OUTER_REST_BYPASS:
				leg1 = createUpperLeg();
				break;
			default:
				break;
			}
			
			// create a plan for the person that contains all this information
			Plan plan1 = createPlan(startAct, leg1, drainAct, initPlanScore);
			// store information in population
			person.addPlan(plan1);
			
			if (initRouteSpecification.equals(InitRoutes.ONLY_OUTER) && (i % 2 == 1)){
				// select plan1 for every second person (with odd id) if only outer routes are initialized
				person.setSelectedPlan(plan1);
			} else if ((initRouteSpecification.equals(InitRoutes.EVERY_FOURTH_Z_REST_BYPASS) || initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS)) && (i % 4 == 0)) {
				// select plan1 for every fourth person. with 7200 veh/h this results in 1800 veh on this route
				person.setSelectedPlan(plan1);
			}
			
			population.addPerson(person);

			// create further plans if different routes should be initialized
			if (initRouteSpecification.equals(InitRoutes.ONLY_OUTER) 
					|| initRouteSpecification.equals(InitRoutes.ALL)
					|| initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS)
					|| initRouteSpecification.equals(InitRoutes.EVERY_FOURTH_Z_REST_BYPASS)) {
				
				// create a second plan for the person (with the same start and
				// end activity but a different leg)
				Leg leg2 = createLowerLeg();
				if (initRouteSpecification.equals(InitRoutes.EVERY_FOURTH_Z_REST_BYPASS)) {
					leg2 = createByPassLeg();
				}
				Plan plan2 = createPlan(startAct, leg2, drainAct, initPlanScore);	
				person.addPlan(plan2);
				
				if (initRouteSpecification.equals(InitRoutes.EVERY_FOURTH_Z_REST_BYPASS) && (i % 4 != 0)) {
					// select bypass for 3 of 4 persons
					person.setSelectedPlan(plan2);
				} else if (initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS) && (i % 4 == 1)) {
					// select lower route for every other fourth person. with 7200 veh/h this results in 1800 veh on this route
					person.setSelectedPlan(plan2);
				} else if ((initRouteSpecification.equals(InitRoutes.ALL) || initRouteSpecification.equals(InitRoutes.ONLY_OUTER))
						&& (i % 2 == 0)) {
					// select plan2 for every second person (with even id)
					person.setSelectedPlan(plan2);
				}

				if (initRouteSpecification.equals(InitRoutes.ALL) || initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS)) {
					// create a third plan for the person (with the same start and end activity but a different leg)
					Leg leg3 = createUpperLeg();
					if (initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS)) {
						leg3 = createByPassLeg();
					}
					Plan plan3 = createPlan(startAct, leg3, drainAct, initPlanScore);
					person.addPlan(plan3);

					if (initRouteSpecification.equals(InitRoutes.ALL) && i % 2 == 1) {
						// select plan3 for every second person (with odd id)
						person.setSelectedPlan(plan3);
					} else if (initRouteSpecification.equals(InitRoutes.EVERY_SECOND_OUTER_REST_BYPASS) && i % 4 != 0 && i % 4 != 1) {
						// select bypass for 2 of 4 persons
						person.setSelectedPlan(plan3);
					}
				}
			}
		}
		
		// write population file if flag is enabled
		if (this.writePopFile){
			PopulationWriter popWriter = new PopulationWriter(population);
			popWriter.write(this.pathToPopFile);
		}
	}

	private Plan createPlan(Activity startAct, Leg leg, Activity drainAct,
			Double initPlanScore) {
		
		Plan plan = population.getFactory().createPlan();

		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(drainAct);
		plan.setScore(initPlanScore);
		
		return plan;
	}

	/**
	 * Creates a leg with the middle path.
	 */
	private Leg createMiddleLeg() {
		Leg legZ = population.getFactory()
				.createLeg(TransportMode.car);
		
		List<Id<Link>> pathZ = new ArrayList<>();
		pathZ.add(Id.createLinkId("1_2"));
		if (!this.simulateInflowCap23){
			pathZ.add(Id.createLinkId("2_3"));
		}
		else{
			pathZ.add(Id.createLinkId("2_23"));
			pathZ.add(Id.createLinkId("23_3"));
		}
		pathZ.add(Id.createLinkId("3_4"));
		if (!this.simulateInflowCap45){
			pathZ.add(Id.createLinkId("4_5"));
		}
		else{
			pathZ.add(Id.createLinkId("4_45"));
			pathZ.add(Id.createLinkId("45_5"));
		}
		
		Route routeZ = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0_1"), pathZ, Id.createLinkId("5_6"));
		
		legZ.setRoute(routeZ);
		return legZ;
	}

	/**
	 * Creates a leg with the upper path.
	 */
	private Leg createUpperLeg() {
		Leg legUp = population.getFactory()
				.createLeg(TransportMode.car);
		
		List<Id<Link>> pathUp = new ArrayList<>();
		pathUp.add(Id.createLinkId("1_2"));
		if (!this.simulateInflowCap23){
			pathUp.add(Id.createLinkId("2_3"));
		}
		else{
			pathUp.add(Id.createLinkId("2_23"));
			pathUp.add(Id.createLinkId("23_3"));
		}
		pathUp.add(Id.createLinkId("3_5"));
		
		Route routeUp = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0_1"), pathUp, Id.createLinkId("5_6"));
		
		legUp.setRoute(routeUp);
		return legUp;
	}

	/**
	 * Creates a leg with the lower path. 
	 */
	private Leg createLowerLeg() {
		Leg legDown = population.getFactory().createLeg(
				TransportMode.car);
		
		List<Id<Link>> pathDown = new ArrayList<>();
		pathDown.add(Id.createLinkId("1_2"));
		if (!this.simulateInflowCap24) {
			pathDown.add(Id.createLinkId("2_4"));
		} else {
			pathDown.add(Id.createLinkId("2_24"));
			pathDown.add(Id.createLinkId("24_4"));
		}
		if (!this.simulateInflowCap45) {
			pathDown.add(Id.createLinkId("4_5"));
		} else {
			pathDown.add(Id.createLinkId("4_45"));
			pathDown.add(Id.createLinkId("45_5"));
		}
		
		Route routeDown = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0_1"), pathDown, Id.createLinkId("5_6"));
		
		legDown.setRoute(routeDown);
		return legDown;
	}

	private Leg createByPassLeg() {
		Leg legByPass = population.getFactory().createLeg(TransportMode.car);
		
		List<Id<Link>> pathByPass = new ArrayList<>();
		pathByPass.add(Id.createLinkId("1_7"));
		pathByPass.add(Id.createLinkId("7_8"));
		pathByPass.add(Id.createLinkId("8_5"));
		Route routeDown = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0_1"), pathByPass, Id.createLinkId("5_6"));
		
		legByPass.setRoute(routeDown);
		return legByPass;
	}

	public void setNumberOfPersons(int numberOfPersons) {
		this.numberOfPersons = numberOfPersons;
	}

	public void writePopulation(String file) {
		new PopulationWriter(population).write(file);
	}

	public void setSimulationPeriod(int simulationPeriod) {
		this.simulationPeriod = simulationPeriod;
	}

	public void setSimulationStartTime(double simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

}
