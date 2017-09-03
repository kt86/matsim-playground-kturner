package playground.balac.freefloating.routerparkingmodule;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class FreeFloatingRoutingModule implements RoutingModule {
	
	public FreeFloatingRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		final Leg leg = PopulationUtils.createLeg("walk_ff");
		Route route = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg.setRoute(route);
		trip.add( leg );
		
		final Leg leg1 = PopulationUtils.createLeg("freefloating");
		NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg1.setRoute(route1);
		trip.add( leg1 );	
		
		final Leg leg3 = PopulationUtils.createLeg("freefloatingparking");
		NetworkRoute route3 = RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg3.setRoute(route3);
		trip.add( leg3 );
		
		final Leg leg2 = PopulationUtils.createLeg("walk_ff");
		Route route2 = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg2.setRoute(route2);
		trip.add( leg2 );
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		
		return EmptyStageActivityTypes.INSTANCE;
	}
	
	
}
