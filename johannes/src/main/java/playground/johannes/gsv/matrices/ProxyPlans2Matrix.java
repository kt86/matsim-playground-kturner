/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.zones.ODMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.ODMatrixXMLWriter;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.util.ProgressLogger;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author johannes
 * 
 */
public class ProxyPlans2Matrix {

	private final Predicate modePredicate = new ModePredicate("car");
	
	public ODMatrix run(Collection<ProxyPlan> plans, ZoneCollection zones, ActivityFacilities facilities) {
		ODMatrix m = new ODMatrix();
		
//		MathTransform transform = null;
//		try {
//			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
//		} catch (FactoryException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		int noZones = 0;
		
		ProgressLogger.init(plans.size(), 2, 10);
		
		for (ProxyPlan plan : plans) {
			for (int i = 1; i < plan.getLegs().size(); i ++) {
				ProxyObject leg = plan.getLegs().get(i);
				ProxyObject prev = plan.getActivities().get(i);
				ProxyObject next = plan.getActivities().get(i+1);
				
				if (modePredicate.test(leg, prev, next)) {
					
					Id<ActivityFacility> origId = Id.create(prev.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					ActivityFacility origFac = facilities.getFacilities().get(origId);

					Id<ActivityFacility> destId = Id.create(next.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					ActivityFacility destFac = facilities.getFacilities().get(destId);

//					Point origPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(origFac.getCoord()), transform);
//					Point destPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(destFac.getCoord()), transform);
					
//					Zone origZone = zones.get(origPoint.getCoordinate());
//					Zone destZone = zones.get(destPoint.getCoordinate());

					Zone origZone = zones.get(new Coordinate(origFac.getCoord().getX(), origFac.getCoord().getY()));
					Zone destZone = zones.get(new Coordinate(destFac.getCoord().getX(), destFac.getCoord().getY()));

					if (origZone != null && destZone != null) {
						Double val = m.get(origZone, destZone);
						if(val == null) {
							val = new Double(0);
						}
						
						m.set(origZone, destZone, ++val);
					} else {
						noZones++;
					}
				}
			}
			ProgressLogger.step();
		}

		System.err.println(noZones + " zones not found.");

		return m;
	}

	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[1]);

//		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read(args[2]);
//		ZoneCollection zones = ZoneCollectionSHPReader.read(args[2]);
		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get(args[2])));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));

		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		
		Set<ProxyPlan> plans = new HashSet<>(parser.getPersons().size());
		for(ProxyPerson person : parser.getPersons()) {
			plans.add(person.getPlans().get(0));
		}
		
		ODMatrix m = new ProxyPlans2Matrix().run(plans, zones, scenario.getActivityFacilities());

		ODMatrixXMLWriter writer = new ODMatrixXMLWriter();
		writer.write(m, args[3]);
	}
}
