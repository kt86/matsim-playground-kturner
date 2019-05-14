/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.wum.malin;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToScore;

import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop4.sampersutilities.SampersScoringFunctionFactory;
import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AnalysisRunner {

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("STARTED ...");

		final String zonesShapeFileName = "/Users/GunnarF/OneDrive - VTI/My Data/ihop2/ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_network.xml.gz");
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(
				"/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_transitSchedule.xml.gz");

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		zonalSystem.addNetwork(scenario.getNetwork(), StockholmTransformationFactory.WGS84_SWEREF99);
		final InterZonalStatistics zoneStats = new InterZonalStatistics(zonalSystem, scenario);

		zoneStats.addOrigin("720113");
		zoneStats.addOrigin("720112");
		zoneStats.addOrigin("720111");
		zoneStats.addOrigin("720103");

		zoneStats.addDestination("720113");
		zoneStats.addDestination("720112");
		zoneStats.addDestination("720111");
		zoneStats.addDestination("720103");

		final EventsManager manager = EventsUtils.createEventsManager();

		final EventsToLegs events2legs = new EventsToLegs(scenario);
		events2legs.addLegHandler(zoneStats);
		manager.addHandler(events2legs);

		final EventsToActivities events2acts = new EventsToActivities();
		events2acts.addActivityHandler(zoneStats);
		manager.addHandler(events2acts);

		// >>>>> 2019-04-24 >>>>>

		// events to scores requires to load the entire population -> servers
//		final EventsToScore events2scores = EventsToScore.createWithoutScoreUpdating(scenario,
//				new SampersScoringFunctionFactory(), manager);
//		manager.addHandler(events2scores);
		
		
		
		// <<<<< 2019-04-24 <<<<<

		EventsUtils.readEvents(manager, "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		System.out.println("valid: " + zoneStats.getValidCnt());
		System.out.println("invalid: " + zoneStats.getInvalidCnt());

		zoneStats.toFolder(new File("./malin"));

		System.out.println("... DONE");
	}

}
