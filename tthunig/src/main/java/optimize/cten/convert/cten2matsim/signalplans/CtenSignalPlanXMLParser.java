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
package optimize.cten.convert.cten2matsim.signalplans;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import optimize.cten.convert.cten2matsim.signalplans.data.CtenCrossingSolution;
import optimize.cten.convert.cten2matsim.signalplans.data.FixCrossingSolution;
import optimize.cten.convert.cten2matsim.signalplans.data.FlexCrossingSolution;
import optimize.cten.convert.cten2matsim.signalplans.data.FlexibleLight;
import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgGreen;
import optimize.cten.data.DgProgram;

/**
 * @author tthunig
 */
public class CtenSignalPlanXMLParser extends MatsimXmlParser {
	
	private final static Logger LOG = Logger.getLogger(CtenSignalPlanXMLParser.class);

	private final static String ID = "id";
	private final static String LIGHT = "light"; // as ID and start tag
	private final static String FLEXCROSSING = "flexCrossing";
	private final static String FIXEDCROSSING = "fixedCrossing";
	private final static String OFFSET = "offset";
	private final static String PROG = "prog";
	private final static String GREEN_START = "greenStart";
	private final static String GREEN_END = "greenEnd";
	
	private final static String TOTAL_COST = "totalCost";
	private final static String TOTAL_DRIVING_TIME = "totalDrivingTime";
	private final static String TOTAL_WAITING_TIME = "totalWaitingTime";
	private final static String CYCLE_TIME = "cycleTime";
	private final static String STEP_TIME = "stepTime";
	
	private Map<Id<DgCrossing>, CtenCrossingSolution> crossings = new HashMap<>();
	
	private FlexCrossingSolution currentFlexCrossing;
	
	private double totalCost;
	private double totalDrivingTime;
	private double totalWaitingTime;
	private int cycleTime;
	private int stepTime;
	
	public CtenSignalPlanXMLParser() {
		this.setValidating(false);
	}
	
	@Override
	public void endTag(String name, String characterContent, Stack<String> context) {
		if (name.equals(TOTAL_COST)) {
			totalCost = Double.parseDouble(characterContent);
		} else if (name.equals(TOTAL_DRIVING_TIME)) {
			totalDrivingTime = Double.parseDouble(characterContent);
		} else if (name.equals(TOTAL_WAITING_TIME)) {
			totalWaitingTime = Double.parseDouble(characterContent);
		} else if (name.equals(CYCLE_TIME)) {
			cycleTime = Integer.parseInt(characterContent);
		} else if (name.equals(STEP_TIME)) {
			stepTime = Integer.parseInt(characterContent);
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {		
		if (name.equals(FIXEDCROSSING)) {
			// read the crossings program offset
			FixCrossingSolution fixCrossing = new FixCrossingSolution(
					Id.create(atts.getValue(ID), DgCrossing.class), 
					Integer.parseInt(atts.getValue(OFFSET)),
					Id.create(atts.getValue(PROG), DgProgram.class));
			crossings.put(fixCrossing.getId(), fixCrossing);
		} else if (name.equals(FLEXCROSSING)) {
			currentFlexCrossing = new FlexCrossingSolution(Id.create(atts.getValue(ID), DgCrossing.class));
			crossings.put(currentFlexCrossing.getId(), currentFlexCrossing);
		} else if (name.equals(LIGHT)) {
			FlexibleLight light = new FlexibleLight(Id.create(atts.getValue(ID), DgGreen.class), 
					Integer.parseInt(atts.getValue(GREEN_START)), Integer.parseInt(atts.getValue(GREEN_END)));
			currentFlexCrossing.addLight(light);
		} 
	}
	
	public Map<Id<DgCrossing>, CtenCrossingSolution> getCrossings() {
		return crossings;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public double getTotalDrivingTime() {
		return totalDrivingTime;
	}

	public double getTotalWaitingTime() {
		return totalWaitingTime;
	}

	public int getCycleTime() {
		return cycleTime;
	}

	public int getStepTime() {
		return stepTime;
	}
}
