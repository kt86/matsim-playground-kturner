/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package playground.kturner.freightKt;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.util.Collection;

public class FreightUtilsITPreRunForResults {
	private static final Logger log = Logger.getLogger(FreightUtilsITPreRunForResults.class);
	
	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Grid_Szenario/" ;
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/Shipments/grid/ShipmentsIT/";
	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";
	
	private static final Id<Carrier> CARRIER_SERVICES_ID = Id.create("CarrierWServices", Carrier.class);
	private static final Id<Carrier> CARRIER_SHIPMENTS_ID = Id.create("CarrierWShipments", Carrier.class);
	
	private static Carriers carriersWithServicesAndShpiments;
	private static Carrier carrierWServices;
	private static Carrier carrierWShipments;
	
	private static Carriers carriersWithShipmentsOnly;
	
	public static void main(String[] args) throws IOException {
		/*
		 * some preparation - set logging level
		 */
//		Logger.getRootLogger().setLevel(Level.DEBUG);
		Logger.getRootLogger().setLevel(Level.INFO);

		/*
		 * some preparation - create output folder
		 */
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);

		
		//Create carrier with services and shipments
		carriersWithServicesAndShpiments = new Carriers() ;
		carrierWServices = CarrierUtils.createCarrier( CARRIER_SERVICES_ID );
		carrierWServices.getServices().add(createMatsimService("Service1", "i(3,9)", 2));
		carrierWServices.getServices().add(createMatsimService("Service2", "i(4,9)", 2));
		
		//Create carrier with shipments
		carrierWShipments = CarrierUtils.createCarrier( CARRIER_SHIPMENTS_ID );
		carrierWShipments.getShipments().add(createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1)); 
		carrierWShipments.getShipments().add(createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2));

		//Create vehicle for Carriers
//		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("gridType", VehicleType.class))
		VehicleType carrierVehType = VehicleUtils.createVehicleType( Id.create("gridType", VehicleType.class ) )
//				.setCapacity(3)
				.setMaximumVelocity(10)
				.setCostPerDistanceUnit(0.0001)
				.setCostPerTimeUnit(0.001)
				.setFixCost(130)
//				.setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015))
//				.build();
		;
		carrierVehType.getCapacity().setOther( 3 );
		carrierVehType.getEngineInformation().setFuelType( FuelType.diesel );
		carrierVehType.getEngineInformation().setFuelConsumption( 0.015 ) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);
		
		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance() 
				.addType(carrierVehType)
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);				
		carrierWServices.setCarrierCapabilities(ccBuilder.build());
		carrierWShipments.setCarrierCapabilities(ccBuilder.build());

		// Add both carriers
		carriersWithServicesAndShpiments.addCarrier(carrierWServices);
		carriersWithServicesAndShpiments.addCarrier(carrierWShipments);

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithServicesAndShpiments).loadVehicleTypes(vehicleTypes) ;

		//load Network and build netbasedCosts for jsprit
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(INPUT_DIR + "grid-network.xml"); 
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		
		for (Carrier carrier : carriersWithServicesAndShpiments.getCarriers().values()) {
			//Build VRP
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();
	
				// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
	
				//Routing bestPlan to Network
			CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
			carrier.setSelectedPlan(carrierPlanServicesAndShipments) ;
			
			new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR + "servicesAndShipments_solutions_" + carrier.getId().toString() + ".xml");
		}
		new CarrierPlanXmlWriterV2(carriersWithServicesAndShpiments).write( OUTPUT_DIR+ "servicesAndShipments_jsprit_plannedCarriers.xml") ; 
		
		/*
		 * Now convert it to a only shipment-based VRP.
		 */

		//Convert to jsprit VRP
		carriersWithShipmentsOnly = FreightUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriersWithServicesAndShpiments);

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithShipmentsOnly).loadVehicleTypes(vehicleTypes) ;	
		
		for (Carrier carrier : carriersWithShipmentsOnly.getCarriers().values()) {
			//Build VRP
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();
	
				// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
	
				//Routing bestPlan to Network
			CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
			carrier.setSelectedPlan(carrierPlanServicesAndShipments) ;
			
			new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR + "shipmentsOnly_solutions_" + carrier.getId().toString() + ".xml");
		
		}
		new CarrierPlanXmlWriterV2(carriersWithShipmentsOnly).write( OUTPUT_DIR+ "shipmentsOnly_jsprit_plannedCarriers.xml") ; 
		
		log.info("#### Finished ####");
		/*
		 * close logging
		 */
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	private static CarrierShipment createMatsimShipment(String id, String from, String to, int size) {
		Id<CarrierShipment> shipmentId = Id.create(id, CarrierShipment.class);
		Id<Link> fromLinkId = null; 
		Id<Link> toLinkId= null;

		if(from != null ) {
			fromLinkId = Id.create(from, Link.class);
		} 
		if(to != null ) {
			toLinkId = Id.create(to, Link.class);
		}

		return CarrierShipment.Builder.newInstance(shipmentId, fromLinkId, toLinkId, size)
				.setDeliveryServiceTime(30.0)
				.setDeliveryTimeWindow(TimeWindow.newInstance(3600.0, 36000.0))
				.setPickupServiceTime(5.0)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		return CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
				.setCapacityDemand(size)
				.setServiceDuration(31.0)
				.setServiceStartTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
				.build();
	}

}
