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

import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgProgram;

/**
 * @author tthunig
 */
public class FixCrossingSolution implements CtenCrossingSolution {

	private Id<DgCrossing> id;
	private int offset;
	private Id<DgProgram> programId;	
	
	public FixCrossingSolution(Id<DgCrossing> id, int offset, Id<DgProgram> programId) {
		this.id = id;
		this.offset = offset;
		this.programId = programId;
	}
	
	@Override
	public Id<DgCrossing> getId() {
		return this.id;
	}

	public int getOffset() {
		return offset;
	}

	public Id<DgProgram> getProgramId() {
		return programId;
	}

}
