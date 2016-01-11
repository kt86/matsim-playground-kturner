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

/**
 * 
 */
package playground.johannes.gsv.sim;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.johannes.coopsim.analysis.*;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.analysis.CountsCompareAnalyzer;
import playground.johannes.gsv.analysis.PkmGeoTask;
import playground.johannes.gsv.sim.cadyts.CadytsContext;
import playground.johannes.gsv.sim.cadyts.CadytsScoring;
import playground.johannes.gsv.sim.cadyts.ODAdjustorListener;
import playground.johannes.gsv.synPop.Proxy2Matsim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class Simulator {

	public static final String GSV_CONFIG_MODULE_NAME = "gsv";
	
	private static final Logger logger = Logger.getLogger(Simulator.class);
	
	public static void main(String[] args) throws IOException {
		final Controler controler = new Controler(args);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setDumpDataAtEnd(false);

		boolean replanCandidates = Boolean.parseBoolean(controler.getConfig().getParam(GSV_CONFIG_MODULE_NAME, "replanCandidates"));
		final MobsimConnectorFactory mobSimFac = new MobsimConnectorFactory(replanCandidates);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobSimFac.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		
		/*
		 * setup mutation module
		 */
		final Random random = new XORShiftRandom(controler.getConfig().global().getRandomSeed());

		logger.info("Setting up activity location strategy...");
		StrategySettings settings = new StrategySettings(Id.create(1, StrategySettings.class));
		settings.setStrategyName("activityLocations");
		final int numThreads = controler.getConfig().global().getNumberOfThreads();
		final double mutationError = Double.parseDouble(controler.getConfig().getParam(GSV_CONFIG_MODULE_NAME, "mutationError"));
		final double threshold = Double.parseDouble(controler.getConfig().getParam(GSV_CONFIG_MODULE_NAME, "distThreshold"));
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final javax.inject.Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("activityLocations").toProvider(new ActivityLocationStrategyFactory(random, numThreads, "home", controler,
						mutationError, threshold, tripRouterProvider));
			}
		});

		settings = new StrategySettings(Id.create(2, StrategySettings.class));
		settings.setStrategyName("doNothing");

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("doNothing").toProvider(new javax.inject.Provider<PlanStrategy>() {

					@Override
					public PlanStrategy get() {
						return new PlanStrategy() {

							@Override
							public void run(HasPlansAndId<Plan, Person> person) {
							}

							@Override
							public void init(ReplanningContext replanningContext) {
							}

							@Override
							public void finish() {
							}
						};
					}
				});
			}
		});
	/*
		 * setup scoring and cadyts integration
		 */
		logger.info("Setting up cadyts...");
		boolean disableCadyts = Boolean.parseBoolean(controler.getConfig().getModule(GSV_CONFIG_MODULE_NAME).getValue("disableCadyts"));
		LinkOccupancyCalculator calculator = new LinkOccupancyCalculator(controler.getScenario().getPopulation());
		controler.getEvents().addHandler(calculator);
		if (!disableCadyts) {
			CadytsContext context = new CadytsContext(controler.getScenario().getConfig(), null, calculator);
			mobSimFac.setCadytsContext(context);
			controler.setScoringFunctionFactory(new ScoringFactory(context, controler.getConfig(), controler.getScenario().getNetwork()));

			controler.addControlerListener(context);
			controler.addControlerListener(new CadytsRegistration(context));
		}
		
		controler.addControlerListener(new ControllerSetup());
		
		Config config = controler.getConfig();
		String countsFile = config.findParam(GSV_CONFIG_MODULE_NAME, "countsfile");
		double factor = Double.parseDouble(config.findParam("counts", "countsScaleFactor"));

		DTVAnalyzer dtv = new DTVAnalyzer(controler.getScenario().getNetwork(), controler, controler.getEvents(), countsFile, calculator, factor);
		controler.addControlerListener(dtv);

		controler.addControlerListener(new CountsCompareAnalyzer(calculator, countsFile, factor));

		logger.info("Setting up services modules...");
		controler.setModules(new AbstractModule() {
			@Override
			public void install() {
			    install(new DefaultMobsimModule());
                install(new CharyparNagelScoringFunctionModule());
				// include(new TravelTimeCalculatorModule());
				install(new TravelDisutilityModule());
				install(new TripRouterModule());
				install(new StrategyManagerModule());
				// include(new LinkStatsModule());
				// include(new VolumesAnalyzerModule());
				// include(new LegHistogramModule());
				// include(new LegTimesModule());
				// include(new ScoreStatsModule());
				// include(new CountsModule());
				// include(new PtCountsModule());
				// include(new VspPlansCleanerModule());
				// include(new SignalsModule());

				bind(TravelTime.class).toInstance(MobsimConnectorFactory.getTravelTimeCalculator(1.5));

			}
		});

		// services.addOverridingModule(abstractModule);
		/*
		 * load person attributes
		 */
		logger.info("Loading person attributes...");
		ObjectAttributesXmlReader oaReader = new ObjectAttributesXmlReader(controler.getScenario().getPopulation().getPersonAttributes());
		oaReader.putAttributeConverter(ArrayList.class, new Proxy2Matsim.Converter());
		oaReader.parse(controler.getConfig().getParam(GSV_CONFIG_MODULE_NAME, "attributesFile"));
		
		controler.run();

	}

	private static class ControllerSetup implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			MatsimServices controler = event.getServices();
			/*
			 * connect facilities to links
			 */
			logger.info("Connecting facilities to links...");
			NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
			for (ActivityFacility facility : controler.getScenario().getActivityFacilities().getFacilities().values()) {
				Coord coord = facility.getCoord();
				Link link = NetworkUtils.getNearestLink(network, coord);
				((ActivityFacilityImpl) facility).setLinkId(link.getId());
			}
			
			/*
			 * setup analysis modules
			 */
			logger.info("Setting up analysis modules...");
			TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
			task.addTask(new TripGeoDistanceTask(controler.getScenario().getActivityFacilities()));
//			task.addTask(new SpeedFactorTask(services.getScenario().getActivityFacilities()));
//			task.addTask(new ScoreTask());
			task.addTask(new PkmGeoTask(controler.getScenario().getActivityFacilities()));
			task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 0));
			task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 0.5));
			task.addTask(new PkmRouteTask(event.getServices().getScenario().getNetwork(), 1));
			// task.addTask(new ModeShareTask());
			// task.addTask(new ActivityDurationTask());
			// task.addTask(new ActivityLoadTask());
			// task.addTask(new LegLoadTask());
//			task.addTask(new TripDurationTask());
//			task.addTask(new TripPurposeShareTask());
			// task.addTask(new LegFrequencyTask());
			task.addTask(new TripCountTask());

			AnalyzerListiner listener = new AnalyzerListiner();
			listener.task = task;
			listener.controler = controler;
			listener.interval = 5;
			listener.notifyStartup(event);

			controler.addControlerListener(listener);
			/*
			 * Setup ODAdjustor
			 */
			logger.info("Setting up ODAdjustor...");
			ODAdjustorListener odAdjustor = new ODAdjustorListener(controler);
			controler.addControlerListener(odAdjustor);
		}

	}

	private static class CadytsRegistration implements AfterMobsimListener {

		private CadytsContext context;

		public CadytsRegistration(CadytsContext context) {
			this.context = context;
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			Population population = event.getServices().getScenario().getPopulation();
			for (Person person : population.getPersons().values()) {
				context.getCalibrator().addToDemand(context.getPlansTranslator().getPlanSteps(person.getSelectedPlan()));
			}

		}

	}

	private static class ScoringFactory implements ScoringFunctionFactory {

		// private ScoringFunction function;

		private CadytsContext context;

		private Config config;

		public ScoringFactory(CadytsContext context, Config config, Network network) {
			this.context = context;
			this.config = config;
		}

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			SumScoringFunction sum = new SumScoringFunction();
			CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, context);
			sum.addScoringFunction(scoringFunction);
			return sum;
		}

	}

	private static class AnalyzerListiner implements IterationEndsListener, IterationStartsListener, StartupListener {

		private MatsimServices controler;

		private TrajectoryAnalyzerTask task;

		private TrajectoryEventsBuilder builder;
		
		private int interval;

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				if (event.getIteration() % interval == 0) {
					TrajectoryAnalyzer.analyze(builder.trajectories(), task, controler.getControlerIO().getIterationPath(event.getIteration()));
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			builder.reset(event.getIteration());
		}

		@Override
		public void notifyStartup(StartupEvent event) {

			Set<Person> person = new HashSet<Person>(controler.getScenario().getPopulation().getPersons().values());
			builder = new TrajectoryEventsBuilder(person);
			controler.getEvents().addHandler(builder);
		}
	}
}
