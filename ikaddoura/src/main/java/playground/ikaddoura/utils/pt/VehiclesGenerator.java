/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorScheduleVehiclesGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.utils.pt;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * @author Ihab
 *
 */
public class VehiclesGenerator {
	private final static Logger log = Logger.getLogger(VehiclesGenerator.class);
	
	private Vehicles veh = VehicleUtils.createVehiclesContainer();

	public void createVehicles(TransitSchedule schedule, List<Id> lineIDs, int busSeats, int standingRoom, double length, Id vehTypeId, double egressSeconds, double accessSeconds, DoorOperationMode doorOperationMode) {
		
		for (Id transitLineId : lineIDs){
			log.info("Creating transit vehicles for transit line " + transitLineId);
			List<Id> vehicleIDs = new ArrayList<Id>();
			
			for (TransitRoute transitRoute : schedule.getTransitLines().get(transitLineId).getRoutes().values()){
				
				for (Departure dep : transitRoute.getDepartures().values()){
					
					if (vehicleIDs.contains(dep.getVehicleId())){
						// vehicle Id already in list
					} else {
						vehicleIDs.add(dep.getVehicleId());
					}
				}
			}
		
			VehicleType type = veh.getFactory().createVehicleType(vehTypeId);
			VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
			cap.setSeats(busSeats);
			cap.setStandingRoom(standingRoom);
			type.setCapacity(cap);
			type.setLength(length);
			type.setAccessTime(accessSeconds);
			type.setEgressTime(egressSeconds);
			type.setDoorOperationMode(doorOperationMode);
			
			veh.getVehicleTypes().put(vehTypeId, type); 
			
			if (vehicleIDs.isEmpty()){
				throw new RuntimeException("At least 1 Bus is expected. Aborting...");
			} else {
				for (Id vehicleId : vehicleIDs){
					Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
					veh.getVehicles().put(vehicleId, vehicle);
				}
			}
		}
	}

	public Vehicles getVehicles() {
		return this.veh;
	}
	
}