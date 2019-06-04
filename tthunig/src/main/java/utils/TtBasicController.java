/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
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
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.sylvia.SylviaConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import analysis.TtSubnetworkAnalysisWriter;
import analysis.TtSubnetworkAnalyzer;
import analysis.signals.SignalAnalysisListener;
import analysis.signals.SignalAnalysisWriter;
import analysis.signals.TtQueueLengthAnalysisTool;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;


/**
 * @author tthunig
 *
 */
public class TtBasicController {
	
	private static final boolean VIS = false;

	/**
	 * @param args the config file
	 */
	public static void main(String[] args) {
		String configFileName = args[0];
		// put e.g. "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/shape_files/signal_systems/bounding_box.shp"
		String subnetAreaPath = (args.length > 1)? args[1] : null;
		prepareBasicControler(configFileName, subnetAreaPath).run();
	}

	static Controler prepareBasicControler(String configFileName, String subnetAreaPath) {
		Config config = ConfigUtils.loadConfig(configFileName) ;
		// add config groups for adaptive signals
		ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		// add config group for decongestion
		ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
		
		// adjustments for live visualization
		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfvisConfig.setDrawTime(true);
		otfvisConfig.setAgentSize(80f);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
        
		// add the signals module if signal systems are used
//		SignalsModule signalsModule = new SignalsModule();
		Signals.Configurator configurator = new Signals.Configurator( controler ) ;
		// the signals module works for planbased, sylvia and laemmer signal controller
		// by default and is pluggable for your own signal controller like this:
		configurator.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
				DownstreamPlanbasedSignalController.DownstreamFactory.class);
		configurator.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
		configurator.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);
//		controler.addOverridingModule(signalsModule);

		// bind gershenson config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});
		
		// add live visualization module
		if (VIS) { 
			controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}
				
		// add additional bindings (analysis tools and classes that are necessary for
		// your own implementations, e.g. your own signal controllers, as e.g. the
		// config for Gershenson)
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				gershensonConfig.setMinimumGREENtime(5);
				// ... set parameters as you like
				bind(GershensonConfig.class).toInstance(gershensonConfig);
				
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
					this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
				}
				
				// bind subnetwork analysis
				if (subnetAreaPath != null) {
					TtSubnetworkAnalyzer subNetAnalyzer = new TtSubnetworkAnalyzer(subnetAreaPath, scenario.getNetwork());
					this.addEventHandlerBinding().toInstance(subNetAnalyzer);
					this.bind(TtSubnetworkAnalyzer.class).toInstance(subNetAnalyzer);
					this.addControlerListenerBinding().to(TtSubnetworkAnalysisWriter.class);
				}

			}
		});
		return controler;
	}

}
