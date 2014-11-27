/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.noise2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author ikaddoura
 *
 */

public class NoiseTimeTracker implements LinkEnterEventHandler, ActivityEndEventHandler , ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(NoiseTimeTracker.class);
	
	private final NoiseContext noiseContext;
	private final String outputDirectory;
	private final EventsManager events;
	private final List<String> consideredActivityTypes = new ArrayList<String>();
	
	// time interval overlapping information
	private double currentTimeIntervalEnd = Double.NEGATIVE_INFINITY;
	private Map<Id<Person>, Integer> personId2currentActNr = new HashMap<Id<Person>, Integer>();
	
	private double totalCausedNoiseCost = 0.;
	private double totalAffectedNoiseCost = 0.;
	
	private boolean collectNoiseEvents = true;
	private List<NoiseEventCaused> noiseEventsCaused = new ArrayList<NoiseEventCaused>();
	private List<NoiseEventAffected> noiseEventsAffected = new ArrayList<NoiseEventAffected>();
	
	// time interval specific information
	private final Map<Id<Link>,List<Id<Vehicle>>> linkId2enteringVehicleIds = new HashMap<Id<Link>, List<Id<Vehicle>>>();
	private final Map<Id<Link>, Integer> linkId2Cars = new HashMap<Id<Link>, Integer>();
	private final Map<Id<Link>, Integer> linkId2Hgv = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>, Double> linkId2damageCost = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2damageCostPerCar = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2damageCostPerHgv = new HashMap<Id<Link>, Double>();
		
	public NoiseTimeTracker(NoiseContext noiseContext, EventsManager events, String outputDirectory) {
		this.noiseContext = noiseContext;
		this.outputDirectory = outputDirectory;
		this.events = events;
		
		String[] consideredActTypesArray = noiseContext.getNoiseParams().getConsideredActivities();
		for (int i = 0; i < consideredActTypesArray.length; i++) {
			this.consideredActivityTypes.add(consideredActTypesArray[i]);
		}
		
		if (this.consideredActivityTypes.size() == 0) {
			log.warn("Not considering any activity type for the noise damage computation.");
		}	
		
		setFirstActivities();

	}

	@Override
	public void reset(int iteration) {

		this.personId2currentActNr.clear();
		this.currentTimeIntervalEnd = Double.NEGATIVE_INFINITY;
		
		setFirstActivities();
				
	}
	
	private void resetCurrentTimeIntervalInfo() {
		this.linkId2Cars.clear();
		this.linkId2Hgv.clear();
		this.linkId2enteringVehicleIds.clear();
		this.linkId2damageCost.clear();
		this.linkId2damageCostPerCar.clear();
		this.linkId2damageCostPerHgv.clear();
	}
	
	private void setFirstActivities() {
		
		log.info("Receiving first activities from the selected plans...");
		for (Person person : this.noiseContext.getScenario().getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			if (!plan.getPlanElements().isEmpty() && plan.getPlanElements().get(0) instanceof Activity) {
				Activity firstActivity = (Activity) plan.getPlanElements().get(0);

				if (this.consideredActivityTypes.contains(firstActivity.getType())) {
					Id<ReceiverPoint> rpId = noiseContext.getActivityCoord2receiverPointId().get(firstActivity.getCoord());
					double newConsideredAgentUnits = this.noiseContext.getReceiverPoints().get(rpId).getConsideredAgentUnitsNextTimeInterval() + (noiseContext.getNoiseParams().getScaleFactor() * 1.);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsNextTimeInterval(newConsideredAgentUnits);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsCurrentTimeInterval(newConsideredAgentUnits);
				}
			}
		}
		log.info("Receiving first activities from the selected plans... Done.");
		
		NoiseWriter2.writePersonActivityInfoPerHour(noiseContext, outputDirectory, 0.);

	}

	private void checkTime(double time) {
		
		if (this.currentTimeIntervalEnd <= 0.) {
			updateCurrentTimeInterval(time);
			log.info("The first time interval is set to " + this.currentTimeIntervalEnd);
		}
		
		if (time > this.currentTimeIntervalEnd) {
			log.info("+++++++++++++++++++++++++++++++++++++++++++++++");
			log.info("Computing noise for time interval " + Time.writeTime(this.currentTimeIntervalEnd, Time.TIMEFORMAT_HHMM) + "...");
			
			computeCurrentTimeInterval();
			// Maybe write out all time intervals instead of only those with at least one link enter event...
			updateCurrentTimeInterval(time);
			
			for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
				rp.setConsideredAgentUnitsCurrentTimeInterval(rp.getConsideredAgentUnitsNextTimeInterval());
			}
			
			resetCurrentTimeIntervalInfo(); // TODO!
		}
	}
	
	private void updateCurrentTimeInterval(double time) {
		for (int i = 0; i < 30 ; i++) {
			double timeIntervalEnd = (i+1) * noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
			if ((timeIntervalEnd - time) < noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) {
				this.currentTimeIntervalEnd = timeIntervalEnd;
			}
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		checkTime(event.getTime());
		
		if (!(noiseContext.getScenario().getPopulation().getPersons().containsKey(event.getVehicleId()))) {
			// probably public transit
			
		} else {
		
			if (event.getVehicleId().toString().startsWith(this.noiseContext.getNoiseParams().getHgvIdPrefix())) {
				// HGV
				
				if (linkId2Hgv.containsKey(event.getLinkId())) {
					int hgv = this.linkId2Hgv.get(event.getLinkId());
					hgv++;
					this.linkId2Hgv.put(event.getLinkId(), hgv); // TODO: remove this line?!
					
				} else {
					linkId2Hgv.put(event.getLinkId(), 1);
				}
				
			} else {
				// car
				
				if (linkId2Cars.containsKey(event.getLinkId())) {
					int cars = this.linkId2Cars.get(event.getLinkId());
					cars++;
					linkId2Cars.put(event.getLinkId(), cars); // TODO: remove this line?!
					
				} else {
					linkId2Cars.put(event.getLinkId(), 1);
				}
			}
			
			// for all vehicle types
			if (linkId2enteringVehicleIds.containsKey(event.getLinkId())) {
				List<Id<Vehicle>> listTmp = linkId2enteringVehicleIds.get(event.getLinkId());
				listTmp.add(event.getVehicleId());
				linkId2enteringVehicleIds.put(event.getLinkId(), listTmp);
				
			} else {
				List<Id<Vehicle>> listTmp = new ArrayList<Id<Vehicle>>();
				listTmp.add(event.getVehicleId());
				linkId2enteringVehicleIds.put(event.getLinkId(), listTmp);
			}
		}
		
	}

	public void handleEvent(ActivityStartEvent event) {
		
		checkTime(event.getTime());
		
		if (!(this.noiseContext.getScenario().getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
		
			if (!event.getActType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				
				if (this.consideredActivityTypes.contains(event.getActType())) {
					
					if (this.personId2currentActNr.containsKey(event.getPersonId())){
						int newActNr = this.personId2currentActNr.get(event.getPersonId()) + 1;
						this.personId2currentActNr.put(event.getPersonId(), newActNr);
					} else {
						// the second activity in the agent's plan
						this.personId2currentActNr.put(event.getPersonId(), 1);
					}
					
					Coord coord = noiseContext.getPersonId2listOfCoords().get(event.getPersonId()).get(this.personId2currentActNr.get(event.getPersonId()));
					Id<ReceiverPoint> rpId = noiseContext.getActivityCoord2receiverPointId().get(coord);
					
					// current time interval
					double unitsToAdd = ( this.currentTimeIntervalEnd - event.getTime() ) / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
					double newConsideredAgentUnitsCurrentTimeInterval = this.noiseContext.getReceiverPoints().get(rpId).getConsideredAgentUnitsCurrentTimeInterval() + (noiseContext.getNoiseParams().getScaleFactor() * unitsToAdd);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsCurrentTimeInterval(newConsideredAgentUnitsCurrentTimeInterval);
				
					// for the next time interval
					double newConsideredAgentUnitsPreviousTimeInterval = this.noiseContext.getReceiverPoints().get(rpId).getConsideredAgentUnitsNextTimeInterval() + (noiseContext.getNoiseParams().getScaleFactor() * 1.);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsNextTimeInterval(newConsideredAgentUnitsPreviousTimeInterval);
				
					// TODO: set the activity infos
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		checkTime(event.getTime());
		
		if (!(this.noiseContext.getScenario().getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
			
			if (!event.getActType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				
				if (this.consideredActivityTypes.contains(event.getActType())) {
					
					Coord coord = null; 
					if (this.personId2currentActNr.containsKey(event.getPersonId())){
						// not the first activity
						coord = noiseContext.getPersonId2listOfCoords().get(event.getPersonId()).get(this.personId2currentActNr.get(event.getPersonId()));
					} else {
						// the first activity
						coord = noiseContext.getPersonId2listOfCoords().get(event.getPersonId()).get(0);						
					}
					Id<ReceiverPoint> rpId = noiseContext.getActivityCoord2receiverPointId().get(coord);
					
					double unitsToDeduct = ( this.currentTimeIntervalEnd - event.getTime() ) / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
					
					// current time interval
					double newConsideredAgentUnitsCurrentTimeInterval = this.noiseContext.getReceiverPoints().get(rpId).getConsideredAgentUnitsCurrentTimeInterval() - (noiseContext.getNoiseParams().getScaleFactor() * unitsToDeduct);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsCurrentTimeInterval(newConsideredAgentUnitsCurrentTimeInterval);
					
					// for the next time interval
					double newConsideredAgentUnitsNextTimeInterval = this.noiseContext.getReceiverPoints().get(rpId).getConsideredAgentUnitsNextTimeInterval() - (noiseContext.getNoiseParams().getScaleFactor() * 1.);
					this.noiseContext.getReceiverPoints().get(rpId).setConsideredAgentUnitsNextTimeInterval(newConsideredAgentUnitsNextTimeInterval);
				
					// TODO: set the activity infos
				}
			} 
		}		
	}

	private void computeCurrentTimeInterval() {
		
		log.info("Calculating noise emissions...");
		Map<Id<Link>, Double> emissions = calculateNoiseEmission();
		NoiseWriter2.writeNoiseEmissionStatsPerHour(emissions, this.linkId2Cars, this.linkId2Hgv, this.noiseContext, outputDirectory, this.currentTimeIntervalEnd);
		log.info("Calculating noise emissions... Done.");
		
		log.info("Calculating noise immissions...");
		calculateNoiseImmission(emissions);
		NoiseWriter2.writeNoiseImmissionStatsPerHour(noiseContext, outputDirectory, currentTimeIntervalEnd);
		log.info("Calculating noise immissions... Done.");
		
		NoiseWriter2.writePersonActivityInfoPerHour(noiseContext, outputDirectory, currentTimeIntervalEnd);
		
		log.info("Calculating noise damage costs and throwing noise events...");
		calculateNoiseDamageCosts();
		NoiseWriter2.writeDamageInfoPerHour(noiseContext, outputDirectory, currentTimeIntervalEnd);
		log.info("Calculating noise damage costs and throwing noise events... Done.");
	
	}

	private void calculateNoiseDamageCosts() {
		
		log.info("Calculating noise exposure costs for each receiver point...");
		calculateDamagePerReceiverPoint();
		log.info("Calculating noise exposure costs for each receiver point... Done.");

		log.info("Allocating the total exposure cost (per receiver point) to the relevant links...");
		calculateCostSharesPerLinkPerTimeInterval();
		log.info("Allocating the total exposure cost (per receiver point) to the relevant links... Done.");
		
		log.info("Allocating the exposure cost per link to the vehicle categories and vehicles...");
		calculateCostsPerVehiclePerLinkPerTimeInterval();
		log.info("Allocating the exposure cost per link to the vehicle categories and vehicles... Done.");
		
		log.info("Throwing noise events (caused)...");
		throwNoiseEventsCaused();
		log.info("Throwing noise events (caused)... Done.");

		log.info("Throwing noise events (affected)...");
		throwNoiseEventsAffected();
		log.info("Throwing noise events (affected)... Done.");
		
	}
	
	private void calculateDamagePerReceiverPoint() {
		int counter = 0;
		log.info("Calculating noise exposure costs for a total of " + this.noiseContext.getReceiverPoints().size() + " receiver points.");
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
				
			double noiseImmission = rp.getFinalImmission();
				
			double affectedAgentUnits = rp.getConsideredAgentUnitsCurrentTimeInterval();
				
			double damageCost = NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, this.currentTimeIntervalEnd, this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
			double damageCostPerAffectedAgentUnit = NoiseEquations.calculateDamageCosts(noiseImmission, 1., this.currentTimeIntervalEnd, this.noiseContext.getNoiseParams().getAnnualCostRate(), this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation());
				
			rp.setDamageCosts(damageCost);
			rp.setDamageCostsPerAffectedAgentUnit(damageCostPerAffectedAgentUnit);
			
			counter++;
		}
	}

	private void calculateCostSharesPerLinkPerTimeInterval() {
		
		Map<Id<ReceiverPoint>, Map<Id<Link>, Double>> rpId2linkId2costShare = new HashMap<Id<ReceiverPoint>, Map<Id<Link>,Double>>();

		int prepCounter = 0;
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (prepCounter % 10000 == 0) {
				log.info("receiver point # " + prepCounter);
			}
										
			Map<Id<Link>,Double> linkId2costShare = new HashMap<Id<Link>, Double>();
			
			double resultingNoiseImmission = rp.getFinalImmission();
				
			if (rp.getDamageCosts() != 0.) {
				for (Id<Link> linkId : rp.getLinkId2IsolatedImmission().keySet()) {
						
						double noiseImmission = rp.getLinkId2IsolatedImmission().get(linkId);
						double costs = 0.;
						
						if (!(noiseImmission == 0.)) {
							double costShare = NoiseEquations.calculateShareOfResultingNoiseImmission(noiseImmission, resultingNoiseImmission);
							costs = costShare * rp.getDamageCosts();	
						}
						linkId2costShare.put(linkId, costs);
					}
				}
			
			rpId2linkId2costShare.put(rp.getId(), linkId2costShare);
			prepCounter++;
		}
		
		//summing up the link-based-costs
		log.info("Going through all receiver points... Total number: " + this.noiseContext.getReceiverPoints().keySet().size());
		int counter = 0;
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {

			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}

			for (Id<Link> linkId : this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().keySet()) {
		
				if(rp.getDamageCosts() != 0.) {
					
					if (linkId2damageCost.containsKey(linkId)) {
						double sum = linkId2damageCost.get(linkId) + rpId2linkId2costShare.get(rp.getId()).get(linkId);
						linkId2damageCost.put(linkId, sum);
					} else {
						linkId2damageCost.put(linkId, rpId2linkId2costShare.get(rp.getId()).get(linkId));
					}
				}
				
			}
			counter++;
		}
		log.info("Going through all receiver points... Done.");
	}

	private void calculateCostsPerVehiclePerLinkPerTimeInterval() {
		
		log.info("Going through all links... Total number: " + this.noiseContext.getScenario().getNetwork().getLinks().keySet().size());
		int counter = 0;
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			
			if (counter % 10000 == 0) {
				log.info("link # " + counter);
			}
			
			double damageCostPerCar = 0.;
			double damageCostPerHgv = 0.;
			
			double damageCostSum = 0.;
				
			if (linkId2damageCost.containsKey(linkId)) {					
				damageCostSum = linkId2damageCost.get(linkId);
			}
				
			int nCar = 0;
			if (this.linkId2Cars.containsKey(linkId)) {
				nCar = this.linkId2Cars.get(linkId);
			}
			
			int nHdv = 0;
			if (this.linkId2Hgv.containsKey(linkId)) {
				nHdv = this.linkId2Hgv.get(linkId);
			}
			
			double vCar = (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
			double vHdv = vCar;
				
			// If different speeds for different vehicle types have to be considered, adapt the calculation here.
			// For example, a maximum speed for hdv-vehicles could be set here (for instance for German highways) 
				
			double lCar = NoiseEquations.calculateLCar(vCar);
			double lHdv = NoiseEquations.calculateLHdv(vHdv);
				
			double shareCar = 0.;
			double shareHdv = 0.;
				
			if ((nCar > 0) || (nHdv > 0)) {
				shareCar = NoiseEquations.calculateShare(nCar, lCar, nHdv, lHdv);
				shareHdv = NoiseEquations.calculateShare(nHdv, lHdv, nCar, lCar);
			}
			
			double damageCostSumCar = shareCar * damageCostSum;
			double damageCostSumHdv = shareHdv * damageCostSum;
				
			if (!(nCar == 0)) {
				damageCostPerCar = damageCostSumCar/nCar;
			}
				
			if (!(nHdv == 0)) {
				damageCostPerHgv = damageCostSumHdv/nHdv;
			}
			
			linkId2damageCostPerCar.put(linkId, damageCostPerCar);
			linkId2damageCostPerHgv.put(linkId, damageCostPerHgv);
			
			counter++;
		}
	}

	private void throwNoiseEventsCaused() {
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()) {
			double amountCar = (linkId2damageCostPerCar.get(linkId)) / (this.noiseContext.getNoiseParams().getScaleFactor());
			double amountHdv = (linkId2damageCostPerHgv.get(linkId)) / (this.noiseContext.getNoiseParams().getScaleFactor());
								
			if (this.linkId2enteringVehicleIds.containsKey(linkId)){
				for(Id<Vehicle> id : this.linkId2enteringVehicleIds.get(linkId)) {
					
					double amount = 0.;
					boolean isHdv = false;
					
					if(!(id.toString().startsWith(this.noiseContext.getNoiseParams().getHgvIdPrefix()))) {
						amount = amountCar;
					} else {
						amount = amountHdv;
						isHdv = true;
					}
					
					if (amount != 0.) {
						NoiseVehicleType carOrHdv = NoiseVehicleType.car;
						if (isHdv == true) {
							carOrHdv = NoiseVehicleType.hgv;
						}
						
						// The person Id is assumed to be equal to the vehicle Id.
						NoiseEventCaused noiseEvent = new NoiseEventCaused(this.currentTimeIntervalEnd, Id.create(id, Person.class), id, amount, linkId, carOrHdv);
						events.processEvent(noiseEvent);
						
						if (this.collectNoiseEvents) {
							this.noiseEventsCaused.add(noiseEvent);
						}
						
						totalCausedNoiseCost = totalCausedNoiseCost + amount;
					}
				}
			}
		}
	}
	
	private void throwNoiseEventsAffected() {
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (!(rp.getActInfos().isEmpty())) {
				for (PersonActivityInfo actInfo : rp.getActInfos()) {
					double factor = actInfo.getDurationWithinInterval() / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();

					if (!(factor == 0.)) {
													
						double amount = factor * rp.getDamageCostsPerAffectedAgentUnit();
						
						if (amount != 0.) {
							NoiseEventAffected noiseEventAffected = new NoiseEventAffected(this.currentTimeIntervalEnd, actInfo.getPersonId(), amount, rp.getId(), actInfo.getActivityType());
							events.processEvent(noiseEventAffected);
							
							if (this.collectNoiseEvents) {
								this.noiseEventsAffected.add(noiseEventAffected);
							}
							
							totalAffectedNoiseCost = totalAffectedNoiseCost + amount;
						}					
					}
				}
			}	
		}
	}

	private void calculateNoiseImmission(Map<Id<Link>, Double> emissions) {
		int counter = 0;
		
		for (ReceiverPoint rp : this.noiseContext.getReceiverPoints().values()) {
			
			if (counter % 10000 == 0) {
				log.info("receiver point # " + counter);
			}
					
			Map<Id<Link>, Double> linkId2isolatedImmission = new HashMap<Id<Link>, Double>();
			
			for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()) {
				if (this.noiseContext.getNoiseParams().getTunnelLinkIDs().contains(linkId)) {
			 		// the immission resulting from this link is zero
					linkId2isolatedImmission.put(linkId, 0.);
			 			
			 	} else {
					double noiseImmission = 0.;
					if (!(emissions.get(linkId) == 0.)) {
						noiseImmission = emissions.get(linkId)
								+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2distanceCorrection().get(linkId)
								+ this.noiseContext.getReceiverPoints().get(rp.getId()).getLinkId2angleCorrection().get(linkId)
								;
						
						if (noiseImmission < 0.) {
							noiseImmission = 0.;
						}
					}
					linkId2isolatedImmission.put(linkId, noiseImmission);
			 	}
			}
			
			double finalNoiseImmission = 0.;
			if (!linkId2isolatedImmission.isEmpty()) {
				finalNoiseImmission = NoiseEquations.calculateResultingNoiseImmission(linkId2isolatedImmission.values());
			}
			
			rp.setFinalImmission(finalNoiseImmission);
			rp.setLinkId2IsolatedImmission(linkId2isolatedImmission);
			counter ++;
		}
	}
	
	private Map<Id<Link>, Double> calculateNoiseEmission() {
		
		Map<Id<Link>, Double> linkID2emission = new HashMap<Id<Link>, Double>();
		
		for (Id<Link> linkId : this.noiseContext.getScenario().getNetwork().getLinks().keySet()){
			
			double vCar = (this.noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
			double vHdv = vCar;
			
			double noiseEmission = 0.;

			int n_car = 0;
			if (this.linkId2Cars.containsKey(linkId)) {
				n_car = this.linkId2Cars.get(linkId);
			}
			
			int n_hgv = 0;
			if (this.linkId2Hgv.containsKey(linkId)) {
				n_hgv = this.linkId2Hgv.get(linkId);
			}
			int n = n_car + n_hgv;
			double p = 0.;
				
			if(!(n == 0)) {
				p = n_hgv / ((double) n);
			}
				
			if(!(n == 0)) {
					
				// correction for a sample, multiplicate the scale factor
				n = (int) (n * (this.noiseContext.getNoiseParams().getScaleFactor()));
					
				// correction for intervals unequal to 3600 seconds (= one hour)
				n = (int) (n * (3600. / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()));
					
				double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
				double Dv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
				noiseEmission = mittelungspegel + Dv;					
			}	
			
			linkID2emission.put(linkId, noiseEmission);			
		}
		return linkID2emission;
	}
	
	public void computeFinalTimeInterval() {
		log.info("+++++++++++++++++++++++++++++++++++++++++++++++");
		log.info("Computing noise for final time interval " + Time.writeTime(this.currentTimeIntervalEnd, Time.TIMEFORMAT_HHMM) + "...");
		
		computeCurrentTimeInterval();
		resetCurrentTimeIntervalInfo();
	}

	public List<NoiseEventCaused> getNoiseEventsCaused() {
		return noiseEventsCaused;
	}

	public List<NoiseEventAffected> getNoiseEventsAffected() {
		return noiseEventsAffected;
	}
	
}
