/* *********************************************************************** *
 * project: org.matsim.*
 * DgCrossing
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package optimize.cten.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class DgCrossing {

	private static final Logger log = Logger.getLogger(DgCrossing.class);
	
	private Id<DgCrossing> id;
	private Map<Id<DgCrossingNode>, DgCrossingNode> nodes = new HashMap<>();
	private Map<Id<DgStreet>, DgStreet> lights = new HashMap<>();
	private Map<Id<DgProgram>, DgProgram> programs = new HashMap<>();
	private Map<Id<DgStreet>, TtRestriction> restrictions = new HashMap<>();
	private String type; // use TtCrossingType
	private int cycle;
	private int clearTime = 2;

	public DgCrossing(Id<DgCrossing> id) {
		this.id = id;
	}

	public Id<DgCrossing> getId() {
		return this.id;
	}

	public void addNode(DgCrossingNode crossingNode) {
		if (this.nodes.containsKey(crossingNode.getId())){
			log.warn("CrossingNode " + crossingNode.getId() +" already exists.");
		}
		this.nodes.put(crossingNode.getId(), crossingNode);
	}
	
	public Map<Id<DgCrossingNode>, DgCrossingNode> getNodes(){
		return this.nodes;
	}
	
	public Map<Id<DgStreet>, DgStreet> getLights(){
		return this.lights;
	}

	public void addLight(DgStreet light) {
		this.lights.put(light.getId(), light);
	}
	
	public void addProgram(DgProgram p){
		if (this.programs.containsKey(p.getId())){
			log.warn("Program " + p.getId() + " already exists!");
		}
		this.programs.put(p.getId(), p);
	}
	
	public Map<Id<DgProgram>, DgProgram> getPrograms(){
		return this.programs;
	}
	
	public void addRestriction(TtRestriction r){
		if (this.restrictions.containsKey(r.getLightId())){
			log.warn("A restriction for light " + r.getLightId() + " already exists!");
		}
		this.restrictions.put(r.getLightId(), r);
	}
	
	public Map<Id<DgStreet>, TtRestriction> getRestrictions(){
		return this.restrictions;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getCycle() {
		return cycle;
	}

	public void setCycle(int cycle) {
		this.cycle = cycle;
	}

	public int getClearTime() {
		return clearTime;
	}

	public void setClearTime(int clearTime) {
		this.clearTime = clearTime;
	}
}
