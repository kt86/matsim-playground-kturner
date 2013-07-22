/* *********************************************************************** *
 * project: org.matsim.*
 * RunOTFVis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.tacontrol.testisolatedcrossing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;


/**
 * @author dgrether
 *
 */
public class RunOTFVis {

	public static void main(String[] args) {
		double lambdaWestEast = 0.5;
		Scenario scenario = new SingleCrossingScenario().createScenario(lambdaWestEast);
		OTFVis.playScenario(scenario);
	}

}
