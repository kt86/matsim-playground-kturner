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
package signals.xvis.control.events;



/**
 * @author dgrether
 *
 */
public abstract class SelectionEvent implements ControlEvent {

	private boolean overwriteSelection = false;

	public SelectionEvent(boolean overwrite){
		this.overwriteSelection = overwrite;
	}
	
	public boolean doOverwriteSelection(){
		return this.overwriteSelection;
	}
	
}
