/* *********************************************************************** *
 * project: org.matsim.*
 * IatbrPlanBuilder.java
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

package playground.southafrica.population.facilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

public class IatbrPlanBuilder {
	private final static Logger LOG = Logger.getLogger(IatbrPlanBuilder.class);
	private static QuadTree<Coord> spot5QT;
	private static int spotId = 0;
	private static Map<Coord, Id> facilityIdMap = new HashMap<Coord, Id>();
	private static ActivityFacilitiesImpl activityFacilities;
	private static QuadTree<ActivityFacilityImpl> sacscQT;
	private static QuadTree<ActivityFacilityImpl> amenityQT;
	private static QuadTree<ActivityFacilityImpl> educationQT;
	private static QuadTree<ActivityFacilityImpl> shoppingQT;
	private static QuadTree<ActivityFacilityImpl> leisureQT;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IatbrPlanBuilder.class.toString(), args);
		
		String configFile = args[0];
		String plansFile = args[1];
		String sacscFile = args[2];
		String sacscAttributeFile = args[3];
		String spot5File = args[4];
		String amenityFile = args[5];
		
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		ConfigUtils.loadConfig(config, configFile);		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(config);
		activityFacilities = new ActivityFacilitiesImpl();
		activityFacilities.setName("IATBR facilities for Nelson Mandela Bay Metropolitan");
		
		/* Build QuadTree from Spot 5 facilities. */
		spot5QT = buildSpot5QuadTree(spot5File);
		
		/* READ THE VARIOUS INPUT FILES */	
		/* Read SACSC shopping facilities */
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(sacscFile);
		processSacscQT(sc);
		
		/* Read SACSC shopping facility attributes */
		ObjectAttributes facilityAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader or = new ObjectAttributesXmlReader(facilityAttributes);
		or.parse(sacscAttributeFile);
		
		/* Read the general amenities file. */
		ScenarioImpl scAmenities = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader mfr = new MatsimFacilitiesReader(scAmenities);
		mfr.parse(amenityFile);
		processAmenities(scAmenities);
		
		/* Read network */
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		nr.parse(sc.getConfig().network().getInputFile());
		/* Read plans */
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(sc);
		pr.parse(plansFile);
		createPrimaryActivityFacilities(sc.getPopulation());
		
		
		/* Write the facilities to the same file as what the config specifies. 
		 * This is necessary since the controller will later read in this file 
		 * when executing. */
		FacilitiesWriter fw = new FacilitiesWriter(activityFacilities);
		fw.write(sc.getConfig().facilities().getInputFile());
		
		/* Write the population to the same file as what the config specifies. 
		 * This is necessary since the controller will later read in this file 
		 * when executing. */		PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		pw.writeFileV5(sc.getConfig().plans().getInputFile());
		
		
		/* Try location choice */
		ConfigUtils.loadConfig(sc.getConfig(), configFile);
		Controler controler = new Controler(sc);
//		controler.getConfig().locationchoice().setAlgorithm("bestResponse");
//		CharyparNagelScoringFunctionFactory cn = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), sc.getNetwork());
//		controler.setScoringFunctionFactory(cn);
//		controler.setLeastCostPathCalculatorFactory(new FastDijkstraFactory());
//		controler.getConfig().locationchoice().setScaleFactor("100");
//		controler.getConfig().locationchoice().setMaxRecursions("20");
//		controler.getConfig().locationchoice().setFlexibleTypes("s,l,o");
//		controler.getConfig().locationchoice().setEpsilonScaleFactors("0.5,0.5,0.5");
//		LocationChoice lc = new LocationChoice(sc.getNetwork(), controler);
//		lc.prepareReplanning();
//		int count = 0;
//		Iterator<Id> planIterator = sc.getPopulation().getPersons().keySet().iterator();
//		while(count++ < 10 && planIterator.hasNext()){
//			Person person = sc.getPopulation().getPersons().get(planIterator.next());
//			LOG.info("Doing location choice for person " + person.getId());
//			lc.handlePlan(person.getSelectedPlan());
//		}
//		lc.finishReplanning();
		
		/* Run the simulation for one run. */
		Config configRun = ConfigUtils.createConfig();
		configRun.addCoreModules();
		ConfigUtils.loadConfig(configRun, configFile);		
		controler = new Controler(configRun);
		controler.setOverwriteFiles(true);
		controler.run();
		
		Header.printFooter();
	}
	
	
	/**
	 * Building a {@link QuadTree} from the Spot 5 satellite image facilities.
	 * @param filename
	 * @return
	 */
	private static QuadTree<Coord> buildSpot5QuadTree(String filename){

		/* READ THE SPOT Facilities */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		List<Coord> spot5Coords = mfr.readCoords(filename);
		LOG.info("Number of Spot 5 facilities: " + spot5Coords.size());
		
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		for(Coord c : spot5Coords){
			xMin = Math.min(xMin, c.getX());
			xMax = Math.max(xMax, c.getX());
			yMin = Math.min(yMin, c.getY());
			yMax = Math.max(yMax, c.getY());
		}
		QuadTree<Coord> qt = new QuadTree<Coord>(xMin, yMin, xMax, yMax);
		for(Coord c : spot5Coords){
			qt.put(c.getX(), c.getY(), c);
		}		
		return qt;
	}
	
	private static void createPrimaryActivityFacilities(Population population){
		LOG.info("Assigning Spot 5 facilities to activities...");
		LOG.info("Number of people (unaltered):     " + population.getPersons().size());
		double workAtAmenityThreshold = 20;
		double workAtShoppingThreshold = 500;
		
		List<Id> agentsToRemove = new ArrayList<Id>();
		Iterator<Id> iterator = population.getPersons().keySet().iterator();
		while(iterator.hasNext()){
			Id id = iterator.next();
			PersonImpl person = (PersonImpl) population.getPersons().get(id);
			ActivityImpl firstActivity = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0));
			ActivityImpl lastActivity = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1));
			if(firstActivity.getType().equalsIgnoreCase("h") && lastActivity.getType().equalsIgnoreCase("h")){

				/* Check for home activity */
				ActivityFacilityImpl home = null;

				for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if(pe instanceof Activity){
						ActivityImpl act = (ActivityImpl) pe;

						if(act.getType().equalsIgnoreCase("h")){
							if(home == null){
								Coord closestBuilding = spot5QT.get(act.getCoord().getX(), act.getCoord().getY());
								if(!facilityIdMap.containsKey(closestBuilding)){
									Id newFacility = new IdImpl("spot_" + spotId++);
									ActivityFacilityImpl afi = activityFacilities.createFacility(newFacility, closestBuilding);
									afi.createActivityOption("h");
									home = afi;
									facilityIdMap.put(closestBuilding, newFacility);
								} else{
									home = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuilding));
								}
							}
							act.setFacilityId(home.getId());
							act.setCoord(home.getCoord());
						} else if(act.getType().equalsIgnoreCase("w")){
							/* Check for work activity */
							ActivityFacilityImpl closestMall = sacscQT.get(act.getCoord().getX(), act.getCoord().getY());
							/* First check closest mall, and choose if it is within the threshold. */
							if(closestMall.calcDistance(act.getCoord()) <= workAtShoppingThreshold){
								act.setFacilityId(closestMall.getId());
								act.setCoord(closestMall.getCoord());
							} else{
								/* Second, check closest amenity, and choose if it is within the threshold. */
								ActivityFacilityImpl closestAmenity = amenityQT.get(act.getCoord().getX(), act.getCoord().getY());
								if(closestAmenity.calcDistance(act.getCoord()) <= workAtAmenityThreshold){
									act.setFacilityId(closestAmenity.getId());
									act.setCoord(closestAmenity.getCoord());
								} else{
									/* Just choose the closest Spot 5 building */
									ActivityFacilityImpl work;
									Coord closestBuildingCoord = spot5QT.get(act.getCoord().getX(), act.getCoord().getY());
									if(!facilityIdMap.containsKey(closestBuildingCoord)){
										Id newFacility = new IdImpl("spot_" + spotId++);
										ActivityFacilityImpl afi = activityFacilities.createFacility(newFacility, closestBuildingCoord);
										afi.createActivityOption("w");
										facilityIdMap.put(closestBuildingCoord, newFacility);
										work = afi;
									} else{
										work = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuildingCoord));
									}
									act.setFacilityId(work.getId());
									act.setCoord(work.getCoord());
								}
							}
						} else if(act.getType().startsWith("e")){
							/* Education activity. */
							ActivityFacilityImpl closestSchool = educationQT.get(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestSchool.getId());
							act.setCoord(closestSchool.getCoord());
						} else if(act.getType().equalsIgnoreCase("s")){
							/* Pick the closest shopping facility */
							ActivityFacilityImpl closestShop = shoppingQT.get(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestShop.getId());
							act.setCoord(closestShop.getCoord());							 
						} else if(act.getType().equalsIgnoreCase("l")){
							/* Pick the closest leisure facility. */
							ActivityFacilityImpl closestLeisure = leisureQT.get(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestLeisure.getId());
							act.setCoord(closestLeisure.getCoord());							 
						} else{
							/* Just pick the closest amenity facility. 
							 * I (JWJ Jun '12) realized that the LocationChoice 
							 * algorithm NEEDS all activities to be assigned a
							 * facility.*/
							ActivityFacilityImpl closestAmenity = amenityQT.get(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestAmenity.getId());
							act.setCoord(closestAmenity.getCoord());
						}
					}
				}
			} else {
				agentsToRemove.add(person.getId());
			} 
		}
		LOG.info("Removing " + agentsToRemove.size() + " agents whose plans do not start with `h'");
		for(Id id : agentsToRemove){
			population.getPersons().remove(id);
		}
		LOG.info("Number of facilities      : " + facilityIdMap.size());
 		LOG.info("Number of people (altered):     " + population.getPersons().size());
	}
	
	
	private static void processSacscQT(ScenarioImpl sc){
		sacscQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		leisureQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		shoppingQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		
		for(Id id : sc.getActivityFacilities().getFacilities().keySet()){
			ActivityFacilityImpl af = (ActivityFacilityImpl) sc.getActivityFacilities().getFacilities().get(id);
			ActivityFacilityImpl afNew = activityFacilities.createFacility(new IdImpl("sacsc_" + id.toString()), af.getCoord());
			afNew.getActivityOptions().putAll(af.getActivityOptions());
			sacscQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
			shoppingQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
			leisureQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
		}
	}


	private static void processAmenities(ScenarioImpl sc){
		amenityQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		educationQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		int droppedCounter = 0;
		double amenityToShoppingCentreThreshold = 100;
		for(Id id: sc.getActivityFacilities().getFacilities().keySet()){
			ActivityFacilityImpl af = (ActivityFacilityImpl) sc.getActivityFacilities().getFacilities().get(id);
			ActivityFacilityImpl closestMall = sacscQT.get(af.getCoord().getX(), af.getCoord().getY());
			if(af.calcDistance(closestMall.getCoord()) > amenityToShoppingCentreThreshold){
				/* Add it as an independent amenity. */
				Coord closestBuildingCoord = spot5QT.get(af.getCoord().getX(), af.getCoord().getY());
				ActivityFacilityImpl afNew;
				if(!facilityIdMap.containsKey(closestBuildingCoord)){
					afNew = activityFacilities.createFacility(new IdImpl("osm_" + id.toString()), closestBuildingCoord);
					afNew.getActivityOptions().putAll(af.getActivityOptions());
					facilityIdMap.put(closestBuildingCoord, afNew.getId());
				} else{
					afNew = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuildingCoord));
					afNew.getActivityOptions().putAll(af.getActivityOptions());				
				}
				amenityQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				if(afNew.getActivityOptions().containsKey("e")){
					educationQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
				if(afNew.getActivityOptions().containsKey("s")){
					shoppingQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
				if(afNew.getActivityOptions().containsKey("l")){
					leisureQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
			} else{
				droppedCounter++;
			}
		}
		LOG.info("Number of OSM amenities dropped in favour of SACSC malls: " + droppedCounter);
	}

}

