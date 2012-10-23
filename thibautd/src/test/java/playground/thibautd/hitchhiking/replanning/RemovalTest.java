/* *********************************************************************** *
 * project: org.matsim.*
 * RemovalTest.java
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
package playground.thibautd.hitchhiking.replanning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.replanning.HitchHikingRemovalAlgorithm;

/**
 * @author thibautd
 */
public class RemovalTest {
	private List<Plan> plans;

	@Before
	public void initPlans() {
		plans = new ArrayList<Plan>();

		Id link1 = new IdImpl( "link1" );
		Id link2 = new IdImpl( "link2" );

		PlanImpl plan = new PlanImpl( new PersonImpl( new IdImpl( "one passenger trip" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl( new PersonImpl( new IdImpl( "one driver trip" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.DRIVER_MODE );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl( new PersonImpl( new IdImpl( "one tour with one passenger trip" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl( new PersonImpl( new IdImpl( "one tour with one driver trip" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.DRIVER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl( new PersonImpl( new IdImpl( "two tours with one passenger trip each" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl( new PersonImpl( new IdImpl( "two tours with one and two passenger trips" ) ) );
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "h" , link1 );
	}

	@Test
	public void testRemoval() throws Exception {
		HitchHikingRemovalAlgorithm testee = new HitchHikingRemovalAlgorithm( new Random( 1 ) );
		for (Plan plan : plans) {
			int n = countHhTrips( plan );
			testee.run( plan );
			assertEquals(
					"unexpected number of Hh trips after removal in "+plan,
					n -1,
					countHhTrips( plan ));
		}
	}

	private static int countHhTrips(final Plan plan) {
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			String m = ((Leg) pe).getMode();
			if ( m.equals( HitchHikingConstants.PASSENGER_MODE ) ||
					m.equals( HitchHikingConstants.DRIVER_MODE ) ) {
				c++;
			}
		}
		return c;
	}
}

