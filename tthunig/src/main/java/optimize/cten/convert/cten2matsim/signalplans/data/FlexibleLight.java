/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package optimize.cten.convert.cten2matsim.signalplans.data;

import org.matsim.api.core.v01.Id;

import optimize.cten.data.DgGreen;

/**
 * @author tthunig
 */
public class FlexibleLight {

	private Id<DgGreen> id;
	private int greenStart;
	private int greenEnd;
	
	public FlexibleLight(Id<DgGreen> id, int greenStart, int greenEnd) {
		this.id = id;
		this.greenStart = greenStart;
		this.greenEnd = greenEnd;
	}

	public Id<DgGreen> getId() {
		return id;
	}

	public int getGreenStart() {
		return greenStart;
	}

	public int getGreenEnd() {
		return greenEnd;
	}
	
}
