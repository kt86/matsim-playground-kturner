/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.management.InvalidAttributeValueException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;

import playground.kturner.utils.MergeFileVisitor;
import playground.kturner.utils.MoveDirVisitor;

/**
 * Kurzfassung:
 * Implementation einer Frachtsimulation in MATSim. (Masterarbeit, KT)
 * Erstellt auf Basis eines Carriers und Ihren Fahrzeugtypen ein VRP und löst 
 * dieses mit jsprit.
 * Berücksichtigt eine fahrzeugtypspezifische Maut und die Änderungen des Netzwerks per
 * NetWorkChangeEvents, sofern gewünscht und Konfiguriert
 * 
 * Es wurde die Funktionalität zur Erstellung von Umschlagpunkten implementiert.
 * 
 * Optional kann im Anschluss eine MATSim-Simulation der zuvor ermittleten Tourenpläne erfolgen
 * 
 * @author kturner
 * 
 */

/**
 * @author kturner
 *
 */
public class KTFreight_v3 {

	private static final Logger log = Logger.getLogger(KTFreight_v3.class);
	private static final Level loggingLevel = Level.INFO; 		//Set to info to avoid all Debug-Messages, e.g. from VehicleRountingAlgorithm, but can be set to other values if needed. KMT feb/18. 


	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleTours/IVa-UCCE/" ;
	private static final String TEMP_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/Temp/";
	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";

	//Dateinamen
	private static final String NETFILE_NAME = "network.xml" ;
	private static final String VEHTYPEFILE_NAME = "vehicleTypes.xml" ;
//	private static final String CARRIERFILE_NAME = "carrierLEH_v2_withFleet.xml" ; //Hat keine Eletrofzg zur Verfügung
//	private static final String CARRIERFILE_NAME = "carrierLEH_v2_withFleet_withElectro.xml"; // With elektrovehicles available.
	private static final String CARRIERFILE_NAME = "CarriersWShipments/IVa-UCCE_carrierLEH_v2_withFleet_Shipment.xml"; //Based on shipments for multiple tours
	private static final String ALGORITHMFILE_NAME = "mdvrp_algorithmConfig_2.xml" ;
	private static final String TOLLFILE_NAME = "toll_cordon20.xml";		//Zur Mautberechnung (Fzgtypen unten auswählen "onlytollVehTypes"
//	private static final String TOLLFILE_NAME = "toll_cordon1000.xml";		//Maut zur Sperrung der Innenstadt (Fzgtypen unten auswählen "onlytollVehTypes"
	
	private static final String LEZAREAFILE_NAME = "lez_area.xml";  //Zonendefinition (Links) für Umweltzone anhand eines Maut-Files -> Services hier werden im UCC-Case von den UCC beliefert. !File dient NICHT der Mautberechnung
	//Prefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId).
	private static final String uccC_prefix = "UCC-";	

	//Select retailers/carriers for simulation. (begin of CarrierId); null if all should be used.
	private static final ArrayList<String> selectRetailers = null;
//			new ArrayList<String>(Arrays.asList("aldi")); 
	//Location of UCC
	private static final ArrayList<String> uccDepotsLinkIdsString = 
			new ArrayList<String>(Arrays.asList("6874", "3058", "5468")); 
	// VehicleTypes die vom Maut betroffen seien sollen. null, wenn alle (ohne Einschränkung) bemautet werden sollen
	private static final ArrayList<String> onlyTollVehTypes = 
							new ArrayList<String>(Arrays.asList("heavy40t", "heavy26t", "heavy26t_frozen", "medium18t", "light8t", "light8t_frozen")); 
//			new ArrayList<String>(Arrays.asList("heavy40t", "heavy26t", "heavy26t_frozen"));
	//Ende  Namesdefinition Berlin


//	////Beginn Namesdefinition KT Für Test-Szenario (Grid)
//	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Grid_Szenario/" ;
//	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/studies/testing/Grid/Base/" ;
//	private static final String TEMP_DIR = "../../OutputKMT/projects/freight/studies/testing/Temp/";
//	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";
//	
//	
//	//Dateinamen
//	private static final String NETFILE_NAME = "grid-network.xml" ;
//	private static final String VEHTYPEFILE_NAME = "grid-vehTypes_kt.xml" ;
//	private static final String CARRIERFILE_NAME = "grid-carrier_kt.xml" ;
//	private static final String ALGORITHMFILE_NAME = "mdvrp_algorithmConfig_2.xml" ;
//	private static final String TOLLFILE_NAME = "grid-tollCordon.xml";
//	private static final String LEZAREAFILE_NAME = "grid-tollArea.xml"; 
//	//Prefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId). Vermeide "_", 
//	//um die Analyse der MATSIMEvents einfacher zu gestalten (Dort ist "_" als Trennzeichen verwendet.
//	private static final String uccC_prefix = "UCC-";		
//	// All retailer/carrier to handle in UCC-Case. (begin of CarrierId); null if all should be used.
//	private static final ArrayList<String> selectRetailers =//null ;
//			new ArrayList<String>(Arrays.asList("gridCarrier3"));
////		= new ArrayList<String>("gridCarrier", "gridCarrier1", "gridCarrier2", "gridCarrier3"); 
//	//Location of UCC
//	private static final ArrayList<String> uccDepotsLinkIdsString = new ArrayList<String>(Arrays.asList("j(0,5)", "j(10,5)")); 
//	// VehicleTypes die vom Maut betroffen seien sollen. null, wenn alle (ohne Einschränkung) bemautet werden sollen
//	private static final ArrayList<String> onlyTollVehTypes =  null;
////		new ArrayList<String>(Arrays.asList("gridType01", "gridType03", "gridType05", "gridType10")); 
////	//Ende Namesdefinition Grid


	private static final String RUN = "Run_" ;
	private static int runIndex = 0;

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
	private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;
	private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHMFILE_NAME;
	private static final String TOLLFILE = INPUT_DIR + TOLLFILE_NAME;
	private static final String LEZAREAFILE = INPUT_DIR + LEZAREAFILE_NAME;


	// Einstellungen für den Run	
	private static final boolean addingCongestion = true ;  //uses NetworkChangeEvents to reduce freespeed.
	private static final boolean addingToll = true;  //added, kt. 07.08.2014
	private static final boolean usingUCC = false;	 //Using Transshipment-Center, added kt 30.04.2015
	private static final boolean runMatsim = true;	 //when false only jsprit run will be performed
	private static final int LAST_MATSIM_ITERATION = 0;  //only one iteration for writing events.
	private static final int MAX_JSPRIT_ITERATION = 10000;
	private static final int NU_OF_TOTAL_RUNS = 1;	

	//temporär zum Programmieren als Ausgabe
	private static WriteTextToFile textInfofile; 

	//da immer wieder benutzt.
	private static RoadPricingSchemeImpl rpscheme = RoadPricingUtils.createDefaultScheme();
	private static VehicleTypeDependentRoadPricingCalculator rpCalculator = 
			new VehicleTypeDependentRoadPricingCalculator();


	public static void main(String[] args) throws IOException, InvalidAttributeValueException {
		Logger.getRootLogger().setLevel(loggingLevel);
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);
//		copyInputFilesToOutputDirectory();
		for (int i = 1; i<=NU_OF_TOTAL_RUNS; i++) {
			//Damit jeweils neu besetzt wird; sonst würde es sich aufkumulieren.
			rpscheme = RoadPricingUtils.createDefaultScheme();
			rpCalculator = new VehicleTypeDependentRoadPricingCalculator();	
			runIndex = i;	
			multipleRun(args);	
		}
		writeRunInfo();	
		
		try {
			Files.walkFileTree(FileSystems.getDefault().getPath(TEMP_DIR), new MoveDirVisitor(FileSystems.getDefault().getPath(TEMP_DIR), FileSystems.getDefault().getPath(OUTPUT_DIR), StandardCopyOption.REPLACE_EXISTING));
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("#### End of all runs ####");
		OutputDirectoryLogging.closeOutputDirLogging(); 

		//Merge logfiles
		Files.walkFileTree(FileSystems.getDefault().getPath(LOG_DIR), new MergeFileVisitor(new File(LOG_DIR + "logfile.log"), true) );
		Files.walkFileTree(FileSystems.getDefault().getPath(LOG_DIR), new MergeFileVisitor(new File(LOG_DIR + "logfileWarningsErrors.log"), true) );
		System.out.println("#### Finished ####");
	}

	//TODO: Erstellen
//	private static void copyInputFilesToOutputDirectory() throws IOException {
//		File saveInputDirectory = new File(OUTPUT_DIR + "Input");
//		createDir(saveInputDirectory);
//		Files.copy(new File(NETFILE).toPath(), saveInputDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING);
//		
////		private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
////		private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
////		private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;
////		private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHMFILE_NAME;
////		private static final String TOLLFILE = INPUT_DIR + TOLLFILE_NAME;
////		private static final String LEZAREAFILE = INPUT_DIR + TOLLAREAFILE_NAME;
//		
//	}


	//### KT 03.12.2014 multiple run for testing the variaty of the jsprit solutions (especially in terms of costs). 
	private static void multipleRun (String[] args) throws IOException, InvalidAttributeValueException{	
		OutputDirectoryLogging.closeOutputDirLogging();	//close old Log
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR + "log_" + runIndex);	//create new log
		log.info("#### Starting Run: " + runIndex + " of: "+ NU_OF_TOTAL_RUNS);
		createDir(new File(OUTPUT_DIR + RUN + runIndex));
		createDir(new File(TEMP_DIR + RUN + runIndex));	

		// ### config stuff: ###	
		Config config = createConfig(args);
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");
		textInfofile = new WriteTextToFile(new File(TEMP_DIR + "#TextInformation.txt"), null);

		if ( addingCongestion ) { //erst config vorbereiten....Config ist fix, sobald in Scenario!!!! KT, 11.12.14
			config.network().setTimeVariantNetwork(true);
		}

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		if ( addingCongestion ) {
			configureTimeDependentNetwork(scenario);
		}

		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014
		Carriers carriers = jspritRun(config, scenario.getNetwork());

		if ( runMatsim){
			matsimRun(scenario, carriers);	//final MATSim configurations and start of the MATSim-Run
			OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR + "/log_" + runIndex +"a");	//MATSim closes log at the end. therefore we need a new one to log the rest of this iteration
		}
			
		writeAdditionalRunOutput(config, carriers);	//write some final Output
	} 

	private static Config createConfig(String[] args) {
		Config config = ConfigUtils.createConfig() ;

		if ((args == null) || (args.length == 0)) {
			config.controler().setOutputDirectory(OUTPUT_DIR + RUN + runIndex);
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controler().setOutputDirectory( args[0]+"/" );
		}

		config.controler().setLastIteration(LAST_MATSIM_ITERATION);	
		config.network().setInputFile(NETFILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ); 
				
		//Some config stuff to comply to vsp-defaults even there is currently only 1 MATSim iteration and 
		//therefore no need for e.g. a strategy! KMT jan/18
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
//		config.qsim().setUsePersonIdForMissingVehicleId(false);		//TODO: Doesn't work here yet: "java.lang.IllegalStateException: NetworkRoute without a specified vehicle id." KMT jan/18
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
		StrategySettings stratSettings1 = new StrategySettings();
		stratSettings1.setStrategyName("ChangeExpBeta");
		stratSettings1.setWeight(0.1);
		config.strategy().addStrategySettings(stratSettings1);
		
		StrategySettings stratSettings2 = new StrategySettings();
		stratSettings2.setStrategyName("BestScore");
		stratSettings2.setWeight(0.9);
		config.strategy().addStrategySettings(stratSettings2);
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		
		return config;
	}  //End createConfig

	private static Carriers jspritRun(Config config, Network network) throws InvalidAttributeValueException {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();

		Carriers carriers = createCarriers(vehicleTypes);

		carriers = new UccCarrierCreator().extractCarriers(carriers, selectRetailers);

		/*
		 * Wenn UCC verwendent werden, dann muss das Problem geteilt werden.
		 * Es erfolgt eine seperate Berechnung der Touren für die (neuen
		 * UCC-Carrier, welche innerhalb der Umweltzone liefern und den
		 * bisherigen Carriern, die die anderen Services außerhalb der Zone 
		 * übernehmen. Hinzu kommt noch der Transport der Güter für die UCC-Carrier
		 * von den Depots, welcher ebenfalls von den bisherigen Carriern im Rahmen
		 * ihrer Tour mit übernommen wird.	
		 */
		if (usingUCC) {		
			ArrayList<Id<Link>> uccDepotsLinkIds = new ArrayList<Id<Link>>();	//Location of UCC
			if (uccDepotsLinkIdsString == null){
				throw new InvalidAttributeValueException("null value for UCC-locations");
			}
			
			if (uccDepotsLinkIdsString.size() == 0){
				throw new InvalidAttributeValueException("no UCC-locations defined");
			}
			
			for (String linkId : uccDepotsLinkIdsString){
				if (network.getLinks().containsKey(Id.createLinkId(linkId))){
					uccDepotsLinkIds.add(Id.createLinkId(linkId));
				} else {
					throw new InvalidAttributeValueException("UCC-Location is not part of the network: " + linkId);
				}
			}

			UccCarrierCreator uccCarrierCreator = new UccCarrierCreator(carriers, vehicleTypes, LEZAREAFILE, uccC_prefix, selectRetailers, uccDepotsLinkIds, 0.0, 0.0 );
			uccCarrierCreator.createSplittedUccCarrriers();
			carriers = uccCarrierCreator.getSplittedCarriers();

			Carriers uccCarriers = new Carriers();
			Carriers nonUccCarriers = new Carriers();
			for (Carrier c : carriers.getCarriers().values()){
				if (c.getId().toString().startsWith(uccC_prefix)){		//Wenn Carrier ID mit UCC beginnt.
					uccCarriers.addCarrier(c);
				} else {
					nonUccCarriers.addCarrier(c);
				};
			}
			generateCarrierPlans(network, uccCarriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs für die UCC-Carriers

			// Services für die Belieferung der Umschlagpunkte erstellen
			nonUccCarriers = uccCarrierCreator.createServicesToUCC(uccCarriers, nonUccCarriers);  
			generateCarrierPlans(network, nonUccCarriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs für die NonUCC-Carriers

		} else {  // ohne UCCs 
			carriers = new UccCarrierCreator().extractCarriers(carriers, selectRetailers);
			carriers = new UccCarrierCreator().renameVehId(carriers);
			generateCarrierPlans(network, carriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs
		}

		checkServiceAssignment(carriers);

		//### Output nach Jsprit Iteration
		new CarrierPlanXmlWriterV2(carriers).write( TEMP_DIR +  RUN + runIndex + "/jsprit_plannedCarriers.xml") ; //Muss in Temp, da OutputDir leer sein muss // setOverwriteFiles gibt es nicht mehr; kt 05.11.2014

		new WriteCarrierScoreInfos(carriers, new File(TEMP_DIR +  "#JspritCarrierScoreInformation.txt"), runIndex);

		return carriers;
	}

	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERFILE) ;

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		return carriers;
	}

	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;
		return vehicleTypes;
	}

	/**
	 * Erstellt und löst das VRP mit Hilfe von jsprit
	 * @param network
	 * @param carriers
	 * @param vehicleTypes
	 * @param config
	 */
	private static void generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes, Config config) {
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );

		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.

		if (addingToll){		 //Added, KT, 07.08.2014
			generateRoadPricingCalculator(netBuilder, config, carriers);
		}

		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem vrp = vrpBuilder.build() ;

			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, ALGORITHMFILE);
			algorithm.setMaxIterations(MAX_JSPRIT_ITERATION);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;

			carrier.setSelectedPlan(newPlan) ;

			//Plot der Jsprit-Lösung
			Plotter plotter = new Plotter(vrp,solution);
			plotter.plot(TEMP_DIR + RUN + runIndex + "/jsprit_solution_" + carrier.getId().toString() +".png", carrier.getId().toString());

			//Ausgabe der Ergebnisse auf der Console
			//SolutionPrinter.print(vrp,solution,Print.VERBOSE);

		}
	}


	/**
	 * Prüft für die Carriers, ob alle Services auch in den geplanten Touren vorkommen, d.h., ob sie auch tatsächlich geplant wurden.
	 * Falls nicht: log.warn und Ausgabe einer Datei: "#UnassignedServices.txt" mit den Service-Ids.
	 * @param carriers
	 */
	//TODO: Ausgabe der unassigned Services in Run-Verzeichnis und dafür in der Übersicht nur eine Nennung der Anzahl unassignedServices je Run 
	//TODO: multiassigned analog.
	private static void checkServiceAssignment(Carriers carriers) {
		for (Carrier c :carriers.getCarriers().values()){
			ArrayList<CarrierService> assignedServices = new ArrayList<CarrierService>();
			ArrayList<CarrierService> multiassignedServices = new ArrayList<CarrierService>();
			ArrayList<CarrierService> unassignedServices = new ArrayList<CarrierService>();

			log.info("### Check service assignements of Carrier: " +c.getId());
			//Erfasse alle einer Tour zugehörigen (-> stattfindenden) Services 
			for (ScheduledTour tour : c.getSelectedPlan().getScheduledTours()){
				for (TourElement te : tour.getTour().getTourElements()){
					if (te instanceof  ServiceActivity){
						CarrierService assignedService = ((ServiceActivity) te).getService();
						if (!assignedServices.contains(assignedService)){
							assignedServices.add(assignedService);
							log.debug("Assigned Service: " +assignedServices.toString());
						} else {
							multiassignedServices.add(assignedService);
							log.warn("Service " + assignedService.getId().toString() + " has already been assigned to Carrier " + c.getId().toString() + " -> multiple Assignment!");
						}
					}
				}
			}

			//Check, if all Services of the Carrier were assigned
			for (CarrierService service : c.getServices()){
				if (!assignedServices.contains(service)){
					unassignedServices.add(service);
					log.warn("Service " + service.getId().toString() +" will NOT be served by Carrier " + c.getId().toString());
				} else {
					log.debug("Service was assigned: " +service.toString());
				}
			}

			//Schreibe die mehrfach eingeplanten Services in Datei
			if (!multiassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(new File(TEMP_DIR + "#MultiAssignedServices.txt"), true);
					writer.write("#### Multi-assigned Services of Carrier: " + c.getId().toString() + System.getProperty("line.separator"));
					for (CarrierService s : multiassignedServices){
						writer.write(s.getId().toString() + System.getProperty("line.separator"));
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			} else {
				log.info("No service(s)of " + c.getId().toString() +" were assigned to a tour more then one times.");
			}
				

			//Schreibe die nicht eingeplanten Services in Datei
			if (!unassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(new File(TEMP_DIR + "#UnassignedServices.txt"), true);
					writer.write("#### Unassigned Services of Carrier: " + c.getId().toString() + System.getProperty("line.separator"));
					for (CarrierService s : unassignedServices){
						writer.write(s.getId().toString() + System.getProperty("line.separator"));
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			} else {
				log.info("All service(s) of " + c.getId().toString() +" were assigned to at least one tour");
			}

		}//for(carriers)

	}

	//Ausgangspunkt für die MATSim-Simulation
	private static void matsimRun(Scenario scenario, Carriers carriers) {
		final Controler controler = new Controler( scenario ) ;

		if (addingToll){		 //Add roadpricingScheme to MATSIM-Controler Added, KT, 02.12.2014
			controler.setModules(new ControlerDefaultsWithRoadPricingModule(rpscheme));
		}

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;
		
//		//TODO: Added from KN: Prototype for injection of timeDependent and replanning with jsprit.
//		controler.addOverridingModule(new AbstractModule() {
//			@Override public void install() {
//				
//				this.addControlerListenerBinding().toInstance( new ReplanningListener(){
//					@Inject Config config ;
//					@Inject Network originalNetwork ;
//					@Inject TravelTime travelTime ;
//					@Override
//					public void notifyReplanning(ReplanningEvent event) {
//						
//						Network network = NetworkUtils.createNetwork( config ) ;
//						
//						for ( Node node : originalNetwork.getNodes().values() ) {
//							network.addNode(node); // careful, is not a copy; uses the original node; do not modify!!
//						}
//						for ( Link link : originalNetwork.getLinks().values() ){
//							Link newLink = new MyLink( link, travelTime ) ;
//							network.addLink( newLink );
//						}
//						
//						// copy originalNetwork into that new network somehow.
//						
//						try {
//							Carriers carriers = jspritRun(config, network);
//						} catch (InvalidAttributeValueException e) {
//							e.printStackTrace();
//						}
//						
//						// TODO yyyyyy somehow get the generated carriers back into matsim
//						
//					}
//				}) ;
//
//			}
//		});

		controler.run();
	}

	/**
	 * Konfiguration eines TimeDependentNetworks mit Hilfe von NetworkChangeEvents.
	 * Es wird aktuell der Berufsverkehr von 7-10 Uhr und 16:30 bis 19 Uhr simuliert:
	 *  Für alle Kanten mit freespeed > 25 km/h wird dieser auf 50% reduziert.
	 */
	private static void configureTimeDependentNetwork(Scenario scenario) {
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;

			final double threshold = 25./3.6;		//25km/h
			if ( speed > threshold ) {
				{
					NetworkChangeEvent event = new NetworkChangeEvent(7*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.FACTOR,  0.5 )); 
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
				{
					NetworkChangeEvent event = new NetworkChangeEvent(10*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
				{
					NetworkChangeEvent event = new NetworkChangeEvent(16.5*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.FACTOR,  0.5 )); 
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
				{
					NetworkChangeEvent event = new NetworkChangeEvent(19*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					event.addLink(link);
					final NetworkChangeEvent event1 = event;
					NetworkUtils.addNetworkChangeEvent(((Network)scenario.getNetwork()),event1);
				}
			}
		}
	}


	/**
	 * @author: KT
	 * Hinzufügen des RoadPricing-Calculators --> Maut wird berücksichtigt
	 * Die Maut kann Fahrzeugtyp-spezifisch definiert werden
	 * Beachte: Wird das Mautschema mehrfach hinzugefügt, so wird die Maut mehrfach erhoben
	 * 
	 * @param netBuilder
	 * @param config
	 * @param carriers
	 */
	static void generateRoadPricingCalculator(final Builder netBuilder, final Config config, final Carriers carriers) {

		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);

		final RoadPricingSchemeImpl scheme = RoadPricingUtils.createDefaultScheme();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModules().get(RoadPricingConfigGroup.GROUP_NAME);
			rpConfig.setTollLinksFile(TOLLFILE);
			rpReader.readFile(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Collection<Id<VehicleType>> vehTypesAddedToRPS = new ArrayList<Id<VehicleType>>();
		//keine Einschränkung eingegeben -> alle bemauten
		if (onlyTollVehTypes == null) {
			for(Carrier c : carriers.getCarriers().values()){
				for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
					Id<VehicleType> typeId = v.getVehicleType().getId();
					if (!vehTypesAddedToRPS.contains(typeId)) {
						vehTypesAddedToRPS.add(typeId);
						rpCalculator.addPricingScheme(typeId, scheme);
					}
				}
			}
		} else { //nur die angegebenen Fahrzeugtypene bemauten
			for(Carrier c : carriers.getCarriers().values()){
				for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
					Id<VehicleType> typeId = v.getVehicleType().getId();
					if (onlyTollVehTypes.contains(typeId.toString()) & !vehTypesAddedToRPS.contains(typeId)){
						vehTypesAddedToRPS.add(typeId);
						rpCalculator.addPricingScheme(typeId, scheme);
					}
				}
			}
		}

		netBuilder.setRoadPricingCalculator(rpCalculator);

		rpscheme = scheme;

		//Writing Info
		for(Id<VehicleType> vehTypId: rpCalculator.getSchemes().keySet()){
			textInfofile.writeTextLineToFile(vehTypId.toString());
			textInfofile.writeTextLineToFile(rpCalculator.getPricingSchemes(vehTypId).toString());
		}

	}




	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig, hier zunächst eine "leere" Factory
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager(){
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}

	/*
	 * Nutze die von KT geschriebene CarrierScoringFunction
	 * TODO:  Activity: Kostensatz mitgeben, damit klar ist, wo er herkommt... oder vlt geht es in dem Konstrukt doch aus den Veh-Eigenschaften?? (KT, 17.04.15)
	 */
	private static CarrierScoringFunctionFactoryImpl_KT createMyScoringFunction2 (final Scenario scenario) {

		//textInfofile.writeTextLineToFile("createMyScoringFunction2 aufgerufen");

		return new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;

				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
				sumSf.addScoringFunction(fixCost);

				LegScoring legScoring = new LegScoring(carrier);
				sumSf.addScoringFunction(legScoring);

				//Score Activity w/o correction of waitingTime @ 1st Service.
				//			ActivityScoring actScoring = new ActivityScoring(carrier);
				//			sumSf.addScoringFunction(actScoring);

				//Alternativ:
				//Score Activity with correction of waitingTime @ 1st Service.
				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
				sumSf.addScoringFunction(actScoring);

				TollScoring tollScoring = new TollScoring(carrier, scenario.getNetwork(), rpCalculator) ;
				sumSf.addScoringFunction(tollScoring);

				return sumSf;
			}
		};
	}

	private static void writeAdditionalRunOutput(Config config, Carriers carriers) {
		// ### some final output: ###
		if (runMatsim){		//makes only sence, when MATSimrRun was performed KT 06.04.15
			new WriteCarrierScoreInfos(carriers, new File(OUTPUT_DIR + "#MatsimCarrierScoreInformation.txt"), runIndex);
		}		
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml") ;
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml.gz") ;
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controler().getOutputDirectory() + "/output_vehicleTypes.xml");
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controler().getOutputDirectory() + "/output_vehicleTypes.xml.gz");
		
		
		//TODO: Wirte all InputFiles in an "Input"-Directory with the Run-dir?
	}

	/**
	 * Schreibe die Informationen über die der Simulation zu Grunde liegenden Daten zusammen.
	 */
	private static void writeRunInfo() {
		File file = new File(OUTPUT_DIR + "#RunInformation.txt");
		try {
			FileWriter writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!
			writer.write("System date and time writing this file: " + LocalDateTime.now() + System.getProperty("line.separator") + System.getProperty("line.separator"));
			
			writer.write("##Inputfiles:" +System.getProperty("line.separator"));
			writer.write("Input-Directory: " + INPUT_DIR);
			writer.write("Net: \t \t" + NETFILE_NAME +System.getProperty("line.separator"));
			writer.write("Carrier:  \t" + CARRIERFILE_NAME +System.getProperty("line.separator"));
			writer.write("VehType: \t" + VEHTYPEFILE_NAME +System.getProperty("line.separator"));
			writer.write("Algorithm: \t" + ALGORITHMFILE_NAME +System.getProperty("line.separator"));
			writer.write("Toll: \t" + TOLLFILE_NAME +System.getProperty("line.separator"));
			writer.write("LowEmissionZone: \t" + LEZAREAFILE_NAME +System.getProperty("line.separator"));

			writer.write(System.getProperty("line.separator"));
			writer.write("##Run Settings:" +System.getProperty("line.separator"));
			writer.write("addingCongestion: \t" + addingCongestion +System.getProperty("line.separator"));
			writer.write("addingToll: \t \t" + addingToll +System.getProperty("line.separator"));
			writer.write("usingUCC: \t \t" + usingUCC +System.getProperty("line.separator"));
			writer.write("runMatsim: \t \t" + runMatsim +System.getProperty("line.separator"));
			writer.write("Last Matsim Iteration: \t" + LAST_MATSIM_ITERATION +System.getProperty("line.separator"));
			writer.write("Max Jsprit Iteration: \t" + MAX_JSPRIT_ITERATION +System.getProperty("line.separator"));
			writer.write("Number of Runs: \t" + NU_OF_TOTAL_RUNS +System.getProperty("line.separator"));
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}

	//Ergänzung kt: 1.8.2014 Erstellt das angegebene Verzeichnis. Falls es bereits exisitert, geschieht nichts
	private static void createDir(File file) {
		if (!file.exists()){
			log.debug("Create directory: " + file + " : " + file.mkdirs());
		} else
			log.warn("Directory already exists! Check for older stuff: " + file.toString());
	}
	
	
}

