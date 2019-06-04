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

/**
 * 
 */
package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;
import playground.ikaddoura.savPricing.SAVPricingModule;

/**
 * @author ikaddoura
 *
 */
public class PersonTripAnalysis {
	private static final Logger log = Logger.getLogger(PersonTripAnalysis.class);
	
	public void printAvgValuePerParameter(String csvFile, SortedMap<Double, List<Double>> parameter2values) {
		String fileName = csvFile;
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for (Double parameter : parameter2values.keySet()) {
				double sum = 0.;
				int counter = 0;
				for (Double value : parameter2values.get(parameter)) {
					sum = sum + value;
					counter++;
				}
				
				bw.write(String.valueOf(parameter) + ";" + sum / counter);
				bw.newLine();
			}
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printPersonInformation(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}

		String fileName = outputPath + "person_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write( "person Id;"
					+ "number of " + mode + " trips;"
					+ "at least one stuck and abort " + mode + " trip (yes/no);"
					+ "number of stuck and abort events (day);"
					+ mode + " total travel time (day) [sec];"
					+ mode + " total in-vehicle time (day) [sec];"
					+ mode + " total waiting time (for taxi/pt) (day) [sec];"
					+ mode + " total travel distance (day) [m];"
					
					+ "travel related user benefits (based on the selected plans score) [monetary units];"
					+ "total money payments (day) [monetary units];"
					+ "caused noise cost (day) [monetary units];"
					+ "affected noise cost (day) [monetary units]"					
					);
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				double userBenefit = Double.NEGATIVE_INFINITY;
				if (personId2userBenefit.containsKey(id)) {
					userBenefit = personId2userBenefit.get(id);
				}
				int mode_trips = 0;
				String mode_stuckAbort = "no";
				int numberOfStuckAndAbortEvents = 0;
				double mode_travelTime = 0.;
				double mode_inVehTime = 0.;
				double mode_waitingTime = 0.;
				double mode_travelDistance = 0.;
				
				double tollPayments = 0.;
				double causedNoiseCost = 0.;
				double affectedNoiseCost = 0.;
				
				if (noiseHandler != null) {
					if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(id)) {
						affectedNoiseCost = affectedNoiseCost + noiseHandler.getPersonId2affectedNoiseCost().get(id);
					}
				}
				
				if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
					for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
						
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							tollPayments = tollPayments + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
						}
						
						if (noiseHandler != null) {
							if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
								causedNoiseCost = causedNoiseCost + noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip);
							}
						}
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_stuckAbort = "yes";
								}
							}
							
							if (basicHandler.getPersonId2stuckAndAbortEvents().containsKey(id)) {
								numberOfStuckAndAbortEvents = basicHandler.getPersonId2stuckAndAbortEvents().get(id);
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_travelTime = mode_travelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
								mode_waitingTime = mode_waitingTime + basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_travelDistance = mode_travelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}			
						}
					}
				}
				
				bw.write(id + ";"
						+ mode_trips + ";"
						+ mode_stuckAbort + ";"
						+ numberOfStuckAndAbortEvents + ";"
						+ mode_travelTime + ";"
						+ mode_inVehTime + ";"
						+ mode_waitingTime + ";"
						+ mode_travelDistance + ";"
						
						+ userBenefit + ";"
						+ tollPayments + ";"
						+ causedNoiseCost + ";"
						+ affectedNoiseCost
						);
				
						bw.newLine();		
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void printTripInformation(String outputPath,
			String mode,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler,
			PersonMoneyLinkHandler moneyHandler
			) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
				
		String fileName = outputPath + "trip_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write( "person Id;"
					+ "trip no.;"
					+ "mode;"
					+ "stuck and abort trip (yes/no);"
					+ "departure time (trip) [sec];"
					+ "enter vehicle time (trip) [sec];"
					+ "leave vehicle time (trip) [sec];"
					+ "arrival time (trip) [sec];"
					+ "travel time (trip) [sec];"
					+ "in-vehicle time (trip) [sec];"
					+ "waiting time (for taxi/pt) (trip) [sec];"
					+ "travel distance (trip) [m];"
					+ "toll payments (trip) [monetary units];"
					+ "congestion toll payments (trip) [monetary units];"
					+ "noise toll payments (trip) [monetary units];"
					+ "air pollution toll payments (trip) [monetary units];"
					+ "approximate caused noise cost (trip) [monetary units]"); // TODO make this accurate?!
			
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getPersonId2tripNumber2legMode().keySet()) {
				
				for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
										
					if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
						
						String transportModeThisTrip = basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip);
						
						String stuckAbort = "no";
						if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
							if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
								stuckAbort = "yes";
							}
						}
						
						String departureTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2departureTime().containsKey(id) && basicHandler.getPersonId2tripNumber2departureTime().get(id).containsKey(trip)) {
							departureTime = String.valueOf(basicHandler.getPersonId2tripNumber2departureTime().get(id).get(trip));
						}
						
						String enterVehicleTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2enterVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2enterVehicleTime().get(id).containsKey(trip)) {
							enterVehicleTime = String.valueOf(basicHandler.getPersonId2tripNumber2enterVehicleTime().get(id).get(trip));
						}
						
						String leaveVehicleTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2leaveVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2leaveVehicleTime().get(id).containsKey(trip)) {
							leaveVehicleTime = String.valueOf(basicHandler.getPersonId2tripNumber2leaveVehicleTime().get(id).get(trip));
						}
						
						String arrivalTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2arrivalTime().containsKey(id) && basicHandler.getPersonId2tripNumber2arrivalTime().get(id).containsKey(trip)){
							arrivalTime = String.valueOf(basicHandler.getPersonId2tripNumber2arrivalTime().get(id).get(trip));
						}
						
						String travelTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
							travelTime = String.valueOf(basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip));
						}
						
						String inVehTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
							inVehTime = String.valueOf(basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip));
						}
						
						String waitingTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
							waitingTime = String.valueOf(basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip));
						}
						
						String travelDistance = "unknown";
						if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
							travelDistance = String.valueOf(basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip));
						}
						
						String tollPayment = "unknown";
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							tollPayment = String.valueOf(basicHandler.getPersonId2tripNumber2payment().get(id).get(trip));
						}
						
						String congestionTollPayment = "unknown";						
						if (moneyHandler != null) {
							if (moneyHandler.getPersonId2tripNumber2congestionPayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2congestionPayment().get(id).containsKey(trip)) {
								congestionTollPayment = String.valueOf(moneyHandler.getPersonId2tripNumber2congestionPayment().get(id).get(trip));
							}
						}
						
						String noiseTollPayment = "unknown";
						if (moneyHandler != null) {
							if (moneyHandler.getPersonId2tripNumber2noisePayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2noisePayment().get(id).containsKey(trip)) {
								noiseTollPayment = String.valueOf(moneyHandler.getPersonId2tripNumber2noisePayment().get(id).get(trip));
							}
						}
						
						String airPollutionTollPayment = "unknown";
						if (moneyHandler != null) {
							if (moneyHandler.getPersonId2tripNumber2airPollutionPayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2airPollutionPayment().get(id).containsKey(trip)) {
								airPollutionTollPayment = String.valueOf(moneyHandler.getPersonId2tripNumber2airPollutionPayment().get(id).get(trip));
							}
						}
						
						String causedNoiseCost = "unknown";
						if (noiseHandler != null) {
							if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
								causedNoiseCost = String.valueOf(noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip));
							}
						}
						
						bw.write(id + ";"
						+ trip + ";"
						+ transportModeThisTrip + ";"
						+ stuckAbort + ";"
						+ departureTime + ";"
						+ enterVehicleTime + ";"
						+ leaveVehicleTime + ";"
						+ arrivalTime + ";"
						+ travelTime + ";"
						+ inVehTime + ";"
						+ waitingTime + ";"
						+ travelDistance + ";"
						+ tollPayment + ";"
						+ congestionTollPayment + ";"
						+ noiseTollPayment + ";"
						+ airPollutionTollPayment + ";"
						+ causedNoiseCost
						);
						bw.newLine();						
					}
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printAggregatedResults(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
	
		String fileName = outputPath + "aggregated_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			double userBenefits = 0.;
			for (Double userBenefit : personId2userBenefit.values()) {
				userBenefits = userBenefits + userBenefit;
			}
			
			int mode_trips = 0;
			int mode_StuckAndAbortTrips = 0;
			int stuckAndAbortEvents = 0;
			double mode_TravelTime = 0.;
			double mode_inVehTime = 0.;
			double mode_waitingTime = 0.;
			double mode_TravelDistance = 0.;			
			double affectedNoiseCost = 0.;
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				if (noiseHandler != null) {
					if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(id)) {
						affectedNoiseCost = affectedNoiseCost + noiseHandler.getPersonId2affectedNoiseCost().get(id);
					}
				}
				
				if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
					
					for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
						
						// only for the predefined mode
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_StuckAndAbortTrips++;
								}
							}
							
							if (basicHandler.getPersonId2stuckAndAbortEvents().containsKey(id)) {
								stuckAndAbortEvents = basicHandler.getPersonId2stuckAndAbortEvents().get(id);
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
								mode_waitingTime = mode_waitingTime + basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}		
						}
					}
				}	
			}
			
			bw.write("path;" + outputPath);
			bw.newLine();

			bw.newLine();
			
			bw.write("number of " + mode + " trips (sample size);" + mode_trips);
			bw.newLine();
			
			bw.write("number of " + mode + " stuck and abort trip (sample size);" + mode_StuckAndAbortTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort events (sample size);" + stuckAndAbortEvents);
			bw.newLine();
			
			bw.newLine();
						
			bw.write(mode + " travel distance (sample size) [km];" + mode_TravelDistance / 1000.);
			bw.newLine();
			
			bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
			bw.newLine();
			
			bw.write(mode + " in-vehicle time (sample size) [hours];" + mode_inVehTime / 3600.);
			bw.newLine();
			
			bw.write(mode + " waiting time (for taxi/pt) (sample size) [hours];" + mode_waitingTime / 3600.);
			bw.newLine();
		
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void printAggregatedResults(String outputPath,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler,
			PersonMoneyLinkHandler moneyHandler,
			DelayAnalysis delayAnalysis
			) {
	
		String fileName = outputPath + "aggregated_info.csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("path;" + outputPath);
			bw.newLine();
			
			bw.write("-----------------------------");
			bw.newLine();
			bw.write("## Mode-specific analysis ##");
			bw.newLine();
			bw.write("-----------------------------");
			bw.newLine();

			
			for (String mode : basicHandler.getScenario().getConfig().planCalcScore().getModes().keySet()) {
				int mode_trips = 0;
				int mode_StuckAndAbortTrips = 0;
				double mode_TravelTime = 0.;
				double mode_inVehTime = 0.;
				double mode_waitingTime = 0.;
				double mode_TravelDistance = 0.;
				
				for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
					
					if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
						
						for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
														
							// only for the predefined mode
							
							if (basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
								
								mode_trips++;
								
								if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
									if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
										mode_StuckAndAbortTrips++;
									}
								}
								
								if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
									mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
									mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
									mode_waitingTime = mode_waitingTime + basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip);
								}
								
								if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
									mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
								}		
							}
						}
					}	
				}
				
				double vtts_mode_traveling = (basicHandler.getScenario().getConfig().planCalcScore().getPerforming_utils_hr() - basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling()) / basicHandler.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
				double vtts_mode_waiting = 0.;
				
				bw.write("number of " + mode + " trips (sample size);" + mode_trips);
				bw.newLine();
				
				bw.write(mode + " mode specific costs (sample size) [monetary units];" + mode_trips * basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getConstant() * (-1));
				bw.newLine();
				
				bw.write("number of " + mode + " stuck and abort trip (sample size);" + mode_StuckAndAbortTrips);
				bw.newLine();
				
				bw.write("vtts traveling / in-vehicle " + mode + " [monetary units / hour];" + vtts_mode_traveling);
				bw.newLine();
				
				bw.write("vtts waiting " + mode + "[monetary units / hour];" + vtts_mode_waiting);
				bw.newLine();
				
				bw.write(mode + " travel distance (sample size) [km];" + mode_TravelDistance / 1000.);
				bw.newLine();
				
				bw.write(mode + " distance costs (sample size) [monetary units];" + (-1) * mode_TravelDistance * basicHandler.getScenario().getConfig().planCalcScore().getModes().get(mode).getMonetaryDistanceRate() );
				bw.newLine();
				
				bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
				bw.newLine();
				
				bw.write(mode + " travel time costs (sample size) [monetary units];" + (mode_TravelTime / 3600.) * vtts_mode_traveling);
				bw.newLine();
				
				bw.write(mode + " in-vehicle time (sample size) [hours];" + mode_inVehTime / 3600.);
				bw.newLine();
				
				bw.write(mode + " in-vehicle time costs (sample size) [monetary units];" + (mode_inVehTime / 3600.) * vtts_mode_traveling);
				bw.newLine();
				
				bw.write(mode + " waiting time (for taxi/pt) (sample size) [hours];" + mode_waitingTime / 3600.);
				bw.newLine();
				
				bw.write(mode + " waiting time costs (sample size) (considered to be 0 EUR / hour) [monetary units];" + (mode_waitingTime / 3600.) * vtts_mode_waiting);
				bw.newLine();	
				
				bw.write("-----------------------------");
				bw.newLine();	

			}	

			double userBenefitsIncludingMonetaryPayments = 0.;
			for (Double userBenefit : personId2userBenefit.values()) {
				userBenefitsIncludingMonetaryPayments = userBenefitsIncludingMonetaryPayments + userBenefit;
			}
			
			double taxiVehicleDistance = 0.;
			int taxiVehicles = 0;
			for (Id<Vehicle> vehicleId : basicHandler.getTaxiVehicleId2totalDistance().keySet()) {
				taxiVehicles++;
				taxiVehicleDistance = taxiVehicleDistance + basicHandler.getTaxiVehicleId2totalDistance().get(vehicleId);
			}
			
			double carVehicleDistance = 0.;
			int carVehicles = 0;
			for (Id<Vehicle> vehicleId : basicHandler.getCarVehicleId2totalDistance().keySet()) {
				carVehicles++;
				carVehicleDistance = carVehicleDistance + basicHandler.getCarVehicleId2totalDistance().get(vehicleId);
			}
			
			int allTrips = 0;
			int allStuckAndAbortTrips = 0;
			double affectedNoiseCost = 0.;
			double moneyPaymentsByUsers = 0.;
			double congestionPayments = 0.;
			double noisePayments = 0.;
			double airPollutionPayments = 0.;
			double causedNoiseCost = 0.;
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				if (noiseHandler != null) {
					if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(id)) {
						affectedNoiseCost = affectedNoiseCost + noiseHandler.getPersonId2affectedNoiseCost().get(id);
					}
				}
				
				if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
					
					for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
						
						// for all modes
						
						allTrips++;
						
						if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
							if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
								allStuckAndAbortTrips++;
							}
						}
						
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							moneyPaymentsByUsers = moneyPaymentsByUsers + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
						}
						
						if (moneyHandler != null) {
							if (moneyHandler.getPersonId2tripNumber2congestionPayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2congestionPayment().get(id).containsKey(trip)) {
								congestionPayments = congestionPayments + moneyHandler.getPersonId2tripNumber2congestionPayment().get(id).get(trip);
							}
							
							if (moneyHandler.getPersonId2tripNumber2noisePayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2noisePayment().get(id).containsKey(trip)) {
								noisePayments = noisePayments + moneyHandler.getPersonId2tripNumber2noisePayment().get(id).get(trip);
							}
							
							if (moneyHandler.getPersonId2tripNumber2airPollutionPayment().containsKey(id) && moneyHandler.getPersonId2tripNumber2airPollutionPayment().get(id).containsKey(trip)) {
								airPollutionPayments = airPollutionPayments + moneyHandler.getPersonId2tripNumber2airPollutionPayment().get(id).get(trip);
							}
						}
						
						if (noiseHandler != null) {
							if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
								causedNoiseCost = causedNoiseCost + noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip);
							}
						}
						
					}
				}
		
			}
			
			bw.write("-----------------------------");
			bw.newLine();
			bw.write("## Analysis for all modes ##");
			bw.newLine();
			bw.write("-----------------------------");
			bw.newLine();
			
			bw.write("car vehicle distance (sample size) [vehicle-km];" + carVehicleDistance / 1000.);
			bw.newLine();
			
			bw.write("taxi vehicle distance (sample size) [vehicle-km];" + taxiVehicleDistance / 1000.);
			bw.newLine();
			
			bw.write("number of taxi vehicles (sample size);" + taxiVehicles);
			bw.newLine();
			
			bw.write("number of car vehicles (sample size);" + carVehicles);
			bw.newLine();
			
			bw.write("-----------");
			bw.newLine();
									
			bw.write("number of trips (sample size, all modes);" + allTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort trips (sample size, all modes);" + allStuckAndAbortTrips);
			bw.newLine();
			
			bw.write("number of persons with at least one stuck and abort event (sample size, all modes);" + basicHandler.getPersonId2stuckAndAbortEvents().size());
			bw.newLine();
			
			bw.write("-----------");
			bw.newLine();
			
			bw.write("total payments (sample size) (including pt and taxi drivers) [monetary units];" + basicHandler.getTotalPayments());
			bw.newLine();
			
			bw.write("total rewards (sample size) (including pt and taxi drivers) [monetary units];" + basicHandler.getTotalRewards());
			bw.newLine();
			
			bw.write("total amounts (sample size) (including pt and taxi drivers) [monetary units];" + basicHandler.getTotalAmounts());
			bw.newLine();
			
			bw.write("caused noise damage costs (sample size) (caused by car users or passengers) [monetary units];" + causedNoiseCost);
			bw.newLine();
			
			bw.write("congestion toll payments (sample size) (payed by private car users) [monetary units];" + congestionPayments);
			bw.newLine();
			
			bw.write("noise toll payments (sample size) (payed by private car users) [monetary units];" + noisePayments);
			bw.newLine();
			
			bw.write("air pollution toll payments (sample size) (payed by private car users) [monetary units];" + airPollutionPayments);
			bw.newLine();
			
			if (delayAnalysis != null) {
				bw.write("total delay (sample size) [hours];" + delayAnalysis.getTotalDelay() / 3600.);
				bw.newLine();
			}
			
			bw.write("-----------");
			bw.newLine();
						
			double paymentsSAVUserFormerCarUser = 0.;

			int savUsersFormerCarUsers = 0;
			int savUsersFormerNonCarUsers = 0;
			
			// TODO: other money payments by users?
			
			bw.write("number of taxi users (former car users) (sample size);" + savUsersFormerCarUsers);
			bw.newLine();
			
			bw.write("number of taxi users (former non-car users) (sample size);" + savUsersFormerNonCarUsers);
			bw.newLine();
			
			bw.write("total reward of all taxi users (former car users) (sample size) [monetary units];" + paymentsSAVUserFormerCarUser);
			bw.newLine();
			
			bw.write("total reward of all transport users (sample size) [monetary units];" + basicHandler.getTotalRewardsByPersons());
			bw.newLine();
			
			bw.write("-----------");
			bw.newLine();
			bw.write("-----------");
			bw.newLine();
			
			bw.write("travel related user benefits (sample size) (including toll payments) [monetary units];" + userBenefitsIncludingMonetaryPayments);
			bw.newLine();
			
			bw.write("affected noise damage costs (sample size) [monetary units];" + affectedNoiseCost);
			bw.newLine();

			double distanceBasedSAVoCost = 0.;
			if (basicHandler.getScenario().getConfig().planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER) != null) {
				distanceBasedSAVoCost = (-1) * taxiVehicleDistance * basicHandler.getScenario().getConfig().planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER).getMonetaryDistanceRate();
				bw.write("taxi operating costs (sample size) [monetary units];" + distanceBasedSAVoCost);
				bw.newLine();
			}
			
			bw.write("revenues (sample size) (tolls/fares paid by private car users or passengers) [monetary units];" + moneyPaymentsByUsers);
			bw.newLine();
			
			bw.write("revenues (sample size) (tolls/fares paid by private car users or passengers) [monetary units];" + basicHandler.getTotalPaymentsByPersons());
			bw.newLine();
			
			double welfare = moneyPaymentsByUsers - distanceBasedSAVoCost + userBenefitsIncludingMonetaryPayments  - affectedNoiseCost;
			bw.write("system welfare (sample size) [monetary units];" + welfare);
			bw.newLine();
		
			bw.write("-----------");
			bw.newLine();
			bw.write("-----------");
			bw.newLine();
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SortedMap<Double, List<Double>> getParameter2Values(
			String mode, String[] excludedIdPrefixes,
			BasicPersonTripAnalysisHandler basicHandler,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2parameter,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2value,
			double intervalLength, double finalInterval) {
		
		Map<Id<Person>, Map<Integer, String>> personId2tripNumber2legMode = basicHandler.getPersonId2tripNumber2legMode();
		
		SortedMap<Double, List<Double>> parameter2values = new TreeMap<>();
		Map<Integer, List<Double>> nr2values = new HashMap<>();
		
		for (Id<Person> id : personId2tripNumber2legMode.keySet()) {
			
			if (excludePerson(id, excludedIdPrefixes)) {

			} else {
				
				for (Integer trip : personId2tripNumber2legMode.get(id).keySet()) {
					
					if (personId2tripNumber2legMode.get(id).get(trip).equals(mode)) {
						
						double departureTime = personId2tripNumber2parameter.get(id).get(trip);
						int nr = (int) (departureTime / intervalLength) + 1;
						
						double value = 0.;
						if (personId2tripNumber2value.containsKey(id) && personId2tripNumber2value.get(id).containsKey(trip)) {
							value = personId2tripNumber2value.get(id).get(trip);
						}
						
						if (nr2values.containsKey(nr)) {
							List<Double> values = nr2values.get(nr);
							values.add(value);
							nr2values.put(nr, values);
						} else {
							List<Double> values = new ArrayList<>();
							values.add(value);
							nr2values.put(nr, values);
						}				
					}
				}
			}
		}
		for (Integer nr : nr2values.keySet()) {
			parameter2values.put(nr * intervalLength, nr2values.get(nr));
		}
		return parameter2values;
	}
	
	private boolean excludePerson(Id<Person> id, String[] excludedIdPrefixes) {
		
		boolean excludePerson = false;
		
		for (String prefix : excludedIdPrefixes) {
			if (id.toString().startsWith(prefix)) {
				excludePerson = true;
			}
		}
		return excludePerson;
	}

	public SortedMap<Double, List<Double>> getParameter2Values(
			String mode,
			BasicPersonTripAnalysisHandler basicHandler,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2parameter,
			Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2value,
			double intervalLength, double finalInterval) {
		
		Map<Id<Person>, Map<Integer, String>> personId2tripNumber2legMode = basicHandler.getPersonId2tripNumber2legMode();
		
		SortedMap<Double, List<Double>> parameter2values = new TreeMap<>();
		Map<Integer, List<Double>> nr2values = new HashMap<>();
		
		for (Id<Person> id : personId2tripNumber2legMode.keySet()) {
			
			for (Integer trip : personId2tripNumber2legMode.get(id).keySet()) {
				
				if (personId2tripNumber2legMode.get(id).get(trip).equals(mode)) {
					
					double departureTime = personId2tripNumber2parameter.get(id).get(trip);
					int nr = (int) (departureTime / intervalLength) + 1;
					
					double value = 0.;
					if (personId2tripNumber2value.containsKey(id) && personId2tripNumber2value.get(id).containsKey(trip)) {
						value = personId2tripNumber2value.get(id).get(trip);
					}
					
					if (nr2values.containsKey(nr)) {
						List<Double> values = nr2values.get(nr);
						values.add(value);
						nr2values.put(nr, values);
					} else {
						List<Double> values = new ArrayList<>();
						values.add(value);
						nr2values.put(nr, values);
					}				
				}
			}
		}
		for (Integer nr : nr2values.keySet()) {
			parameter2values.put(nr * intervalLength, nr2values.get(nr));
		}
		return parameter2values;
	}
}
