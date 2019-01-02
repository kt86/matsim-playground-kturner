/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.pt.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.pt.strategy.ChangeSingleLegModeWithPredefinedFromModesModule;

/**
 * @author jbischoff
 */
public class RunRWPTComboBerlincase {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new RuntimeException("Wrong arguments");
		}
		String configfile = args[0];

		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new VariableAccessConfigGroup(), new TaxiFareConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		String mode = TaxiConfigGroup.get(config).getMode();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addQSimModule(new TaxiQSimModule());
		controler.addOverridingModule(DvrpModule.createModuleWithDefaultDvrpModeQSimModule(mode));

		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		controler.addOverridingModule(new ChangeSingleLegModeWithPredefinedFromModesModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		controler.run();
	}
}
