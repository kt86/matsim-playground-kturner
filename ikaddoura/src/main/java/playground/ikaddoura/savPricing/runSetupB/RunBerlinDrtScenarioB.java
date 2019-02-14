/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing.runSetupB;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.run.RunBerlinScenario;
import org.matsim.sav.DailyRewardHandlerSAVInsteadOfCar;
import org.matsim.sav.SAVPassengerTracker;
import org.matsim.sav.SAVPassengerTrackerImpl;
import org.matsim.sav.ServiceAreaRequestValidator;
import org.matsim.sav.prepare.BerlinShpUtils;
import org.matsim.sav.prepare.PersonAttributesModification;
import org.matsim.sav.runDRT.RunBerlinDrtScenarioA;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.ikaddoura.savPricing.runSetupB.prepare.BerlinNetworkModification;
import playground.ikaddoura.savPricing.runSetupB.prepare.BerlinPlansModificationSplitTrips;

/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- The private car mode is no longer allowed in the inner-city area (see input shape file) and may only be used for trips outside the inner-city area (network mode: 'car_bb').
 * 	- Initial plans are modified in the following way:
 * 		- Car trips within the Berlin area are replaced by DRT trips.
 * 		- Car trips from Brandenburg to Berlin or the other way round are replaced by 4 alternatives: a direct pt trip and 2 park-and-ride trips (car_bb + S / car_bb + DRT) 
 * 
 * @author ikaddoura
 */

public final class RunBerlinDrtScenarioB {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenarioB.class);

	static final String drtServiceAreaAttribute = "drtServiceArea";

	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");
	private final String inputPersonAttributesSubpopulationPerson = "person";

	public static final String modeToReplaceCarTripsInBrandenburg = "car_bb"; // needs to match the mode specifications in the config file
	private final String modeToReplaceCarTripsInBerlin = TransportMode.drt;
	private final String modeToReplaceCarTripsToFromBerlin = TransportMode.pt;
	private final String taxiNetworkMode = TransportMode.car; // needs to match the mode specification in the config file
	
	private final boolean splitTripsS = true; 
	private final boolean splitTripsTaxi = true; 
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;
	
	private final String transitStopCoordinatesSFile;
	private final String carRestrictedAreaShapeFile;
	private final String drtServiceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;
	
	private double dailyRewardDrtInsteadOfPrivateCar;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String carRestrictedAreaShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt1-v5.2-1pct.config.xml"; // berlin 1pct
			overridingConfigFileName = null;
			carRestrictedAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-ring.csv";
		}		
		
		new RunBerlinDrtScenarioB( configFileName, overridingConfigFileName, carRestrictedAreaShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile).run() ;
	}
	
	public RunBerlinDrtScenarioB( String configFileName, String overridingConfigFileName, String carRestrictedAreaShapeFile, String drtServiceAreaShapeFile, String transitStopCoordinatesSFile) {
		this.transitStopCoordinatesSFile = transitStopCoordinatesSFile;
		this.carRestrictedAreaShapeFile = carRestrictedAreaShapeFile;
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;	
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		// drt + dvrp module
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(
				DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));

		// reject drt requests outside the service area
		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(config).getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class)
						.toInstance(new ServiceAreaRequestValidator(RunBerlinDrtScenarioA.drtServiceAreaAttribute));
			}
		});
		
		// TODO: Add drt-specific fare module
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
//			}
//		});
		
		if (dailyRewardDrtInsteadOfPrivateCar != 0.) {
			// rewards for no longer owning a car
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					
					this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(dailyRewardDrtInsteadOfPrivateCar, modeToReplaceCarTripsInBrandenburg));
					
					SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(TransportMode.drt);		
					this.bind(SAVPassengerTracker.class).toInstance(tracker);
					this.addEventHandlerBinding().toInstance(tracker);
				}
			});
		}
		
		// different modes for different subpopulations
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				List<String> availableModesArrayList = new ArrayList<>();
				availableModesArrayList.add("bicycle");
				availableModesArrayList.add("pt");
				availableModesArrayList.add("walk");
				availableModesArrayList.add(modeToReplaceCarTripsInBrandenburg);
				
				final String[] availableModes = availableModesArrayList.toArray(new String[availableModesArrayList.size()]);
				
				addPlanStrategyBinding("SubtourModeChoice_no-potential-sav-user").toProvider(new Provider<PlanStrategy>() {
										
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_no-potential-sav-user" + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {modeToReplaceCarTripsInBrandenburg, "bicycle"};

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, 
								0.5, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});			
			}
		});
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		scenario = berlin.prepareScenario();
		
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(carRestrictedAreaShapeFile, drtServiceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVandReplaceCarMode(scenario, taxiNetworkMode, modeToReplaceCarTripsInBrandenburg, drtServiceAreaAttribute);	
		new BerlinPlansModificationSplitTrips(transitStopCoordinatesSFile,
				shpUtils,
				inputPersonAttributesSubpopulationPerson,
				modeToReplaceCarTripsInBerlin,
				modeToReplaceCarTripsInBrandenburg,
				modeToReplaceCarTripsToFromBerlin,
				stageActivities,
				parkAndRideActivity,
				parkAndRideDuration,
				splitTripsS,
				splitTripsTaxi).run(scenario);	
		new PersonAttributesModification(shpUtils, stageActivities).run(scenario);

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		
		// dvrp, drt config groups
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new DrtConfigGroup());
		// TODO: Add drt-specific fare config group
		
		List<ConfigGroup> modules = new ArrayList<>();		
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}	
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}
		
		config = berlin.prepareConfig(modulesToAdd);			
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
		
		hasPreparedConfig = true ;
		return config ;
	}
	
	 public void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
		}
		controler.run();
		log.info("Done.");
	}

}

