package playground.mkillat.pt_test;


import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

public class MyEventFileReaderPt {

	public static List <CompleteTransitRoute> EventFileReader (String configFile, String inputFile){
		
		Id idBus = new IdImpl("pt_bus_1_line_Blue Line");
		
		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		EventsManager events = new EventsUtils().createEventsManager();
		MyEventHandlerCompleteTransitInformation handler = new MyEventHandlerCompleteTransitInformation(scenario, idBus);
		events.addHandler(handler);

		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println("Event file "+inputFile+" wurde gelesen!");

		return handler.getCompleteTransitRoute();

	}
	
}
