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

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.VehicleType;

/**
 * @author kturner
 * Simple Pickup and Delivery example from jsprit -> see https://github.com/graphhopper/jsprit/blob/v1.6/jsprit-examples/src/main/java/jsprit/examples/SimpleEnRoutePickupAndDeliveryExample.java
 */
public class FreightWithShipments {

	private static final Logger log = Logger.getLogger(FreightWithShipments.class);

	////Beginn Namesdefinition KT Für Test-Szenario (Grid)
	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Grid_Szenario/" ;
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/Shipments/grid/convertServicesToShipments/";

	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";

	//Dateinamen
	private static final String NETFILE_NAME = "grid-network.xml" ;
	private static final String VEHTYPEFILE_NAME = "grid-vehTypesCap3.xml" ;

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;


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


		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;

		//Create carrier with shipments
		Carriers carriers = new Carriers() ;

		Carrier carrier = CarrierImpl.newInstance(Id.create("Carrier", Carrier.class));
		carrier.getShipments().add(createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1)); 
		carrier.getShipments().add(createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2));
		//		carrier.getShipments().add(createMatsimShipment("shipment3", "i(6,0)", "i(4,7)", 2));
		//		carrier.getShipments().add(createMatsimShipment("shipment4", "i(6,0)", "i(4,5)", 2));

		//		carrier.getServices().add(createMatsimService("Service1", "i(7,4)R", 1));
		carrier.getServices().add(createMatsimService("Service2", "i(3,9)", 2));
		carrier.getServices().add(createMatsimService("Service3", "i(4,9)", 2));

		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("gridType", VehicleType.class)).build();
		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();

		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance() 
				.addType(carrierVehType)
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);				
		carrier.setCarrierCapabilities(ccBuilder.build());

		carriers.addCarrier(carrier);

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;	

		//load Network
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETFILE);


		//Convert to jsprit VRP
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

		//Network for VRP
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		vrpBuilder.setRoutingCost(netBasedCosts) ;

		VehicleRoutingProblem problem = vrpBuilder.build();

		/*
		 * get the algorithm out-of-the-box. 
		 */
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

		/*
		 * and search a solution
		 */
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
		 * get the best 
		 */
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR + "mixed-solution-wo-route.xml");

		//Routing bestPlan to Network
		CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
		NetworkRouter.routePlan(newPlan,netBasedCosts) ;
		carrier.setSelectedPlan(newPlan) ;

		/*
		 * write out problem and solution to xml-file
		 */
		new CarrierPlanXmlWriterV2(carriers).write( OUTPUT_DIR+ "mixed_jsprit_plannedCarriers.xml") ; 

		/*
		 * print nRoutes and totalCosts of bestSolution
		 */
		SolutionPrinter.print(bestSolution);

		/*
		 * plot problem without solution
		 */
		Plotter problemPlotter = new Plotter(problem);
		problemPlotter.plotShipments(true);
		problemPlotter.plot(OUTPUT_DIR + "mixed_simpleEnRoutePickupAndDeliveryExample_problem.png", "services and shipments w/o solution");

		/*
		 * plot problem with solution
		 */
		Plotter solutionPlotter = new Plotter(problem,Arrays.asList(Solutions.bestOf(solutions).getRoutes().iterator().next()));
		solutionPlotter.plotShipments(true);
		solutionPlotter.plot(OUTPUT_DIR + "mixed_simpleEnRoutePickupAndDeliveryExample_solution.png", "services and shipments with solution");

		new GraphStreamViewer(problem).setRenderShipments(true).display();
		new GraphStreamViewer(problem, Solutions.bestOf(solutions)).setRenderDelay(50).display();

		/*
		 * ### Now transform to a only shipment VRP
		 */
		log.info("#### Starting with new VRP based on shipments only ####");

		//Convert to jsprit VRP
		Carriers carriers1 = FreightUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriers);
		Carrier carrier1 = carriers1.getCarriers().get(Id.create("Carrier", Carrier.class));			//TODO: geht hier, weil derzeit nur 1 Carrier mit dem Namen....

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers1).loadVehicleTypes(vehicleTypes) ;	

		VehicleRoutingProblem.Builder vrpBuilder1 = MatsimJspritFactory.createRoutingProblemBuilder(carrier1, network);

		//Network for VRP
		Builder netBuilder1 = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts1 = netBuilder1.build() ;
		netBuilder1.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		vrpBuilder1.setRoutingCost(netBasedCosts1) ;

		VehicleRoutingProblem problem1 = vrpBuilder1.build();

		/*
		 * get the algorithm out-of-the-box. 
		 */
		VehicleRoutingAlgorithm algorithm1 = new SchrimpfFactory().createAlgorithm(problem1);

		/*
		 * and search a solution
		 */
		Collection<VehicleRoutingProblemSolution> solutions1 = algorithm1.searchSolutions();

		/*
		 * get the best 
		 */
		VehicleRoutingProblemSolution bestSolution1 = Solutions.bestOf(solutions1);

		new VrpXMLWriter(problem1, solutions1).write(OUTPUT_DIR + "shipment-solution-wo-route.xml");

		//Routing bestPlan to Network
		CarrierPlan newPlan1 = MatsimJspritFactory.createPlan(carrier1, bestSolution1) ;
		NetworkRouter.routePlan(newPlan1,netBasedCosts1) ;
		carrier.setSelectedPlan(newPlan1) ;

		/*
		 * write out problem and solution to xml-file
		 */
		new CarrierPlanXmlWriterV2(carriers1).write( OUTPUT_DIR+ "shipment-jsprit_plannedCarriers.xml") ; 

		/*
		 * print nRoutes and totalCosts of bestSolution
		 */
		SolutionPrinter.print(bestSolution1);

		/*
		 * plot problem without solution
		 */
		Plotter problemPlotter1 = new Plotter(problem1);
		problemPlotter1.plotShipments(true);
		problemPlotter1.plot(OUTPUT_DIR + "simpleEnRoutePickupAndDeliveryExample_problem.png", "only shipments w/o solution");

		/*
		 * plot problem with solution
		 */
		Plotter solutionPlotter1 = new Plotter(problem1,Arrays.asList(Solutions.bestOf(solutions1).getRoutes().iterator().next()));
		solutionPlotter1.plotShipments(true);
		solutionPlotter1.plot(OUTPUT_DIR + "simpleEnRoutePickupAndDeliveryExample_solution.png", "only shipments with solution");

		new GraphStreamViewer(problem1).setRenderShipments(true).display();
		new GraphStreamViewer(problem1, Solutions.bestOf(solutions1)).setRenderDelay(50).display();



		log.info("#### Finished ####");
		/*
		 * close logging
		 */
		OutputDirectoryLogging.closeOutputDirLogging();

	}

	private static Location loc(Coordinate coordinate) {
		return Location.Builder.newInstance().setCoordinate(coordinate).build();
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
				.setServiceDuration(30.0)
				.setServiceStartTimeWindow(TimeWindow.newInstance(3600.0, 36000.0))
				.build();
	}

}
