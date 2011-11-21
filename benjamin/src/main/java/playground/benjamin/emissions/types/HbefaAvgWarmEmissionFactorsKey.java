/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaAvgWarmEmissionFactorsKey.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.emissions.types;

/**
 * @author benjamin
 *
 */
public class HbefaAvgWarmEmissionFactorsKey{
	
	private HbefaVehicleCategory hbefaVehicleCategory;
	private WarmPollutant hbefaComponent;
	private String hbefaRoadCategory;
	private HbefaTrafficSituation hbefaTrafficSituation;
	
	public HbefaAvgWarmEmissionFactorsKey(){
	}

	public HbefaVehicleCategory getHbefaVehicleCategory() {
		return hbefaVehicleCategory;
	}

	public void setHbefaVehicleCategory(HbefaVehicleCategory hbefaVehicleCategory) {
		this.hbefaVehicleCategory = hbefaVehicleCategory;
	}

	public WarmPollutant getHbefaComponent(){
		return this.hbefaComponent;
	}
	
	public void setHbefaComponent(WarmPollutant warmPollutant) {
		this.hbefaComponent = warmPollutant;
	}

	public String getHbefaRoadCategory() {
		return hbefaRoadCategory;
	}

	public void setHbefaRoadCategory(String hbefaRoadCategory) {
		this.hbefaRoadCategory = hbefaRoadCategory;
	}

	public HbefaTrafficSituation getHbefaTrafficSituation() {
		return hbefaTrafficSituation;
	}

	public void setHbefaTrafficSituation(HbefaTrafficSituation hbefaTrafficSituation) {
		this.hbefaTrafficSituation = hbefaTrafficSituation;
	}
	
	/* need to implement the "equals" method in order to be able to construct an "equal" key
	 later on (e.g. from data available in the simulation)*/
	@Override
	public boolean equals(Object obj) {
	        if(this == obj) {
	              return true;
	         }
	         if (!(obj instanceof HbefaAvgWarmEmissionFactorsKey)) {
	                return false; 
	         }
	         HbefaAvgWarmEmissionFactorsKey key = (HbefaAvgWarmEmissionFactorsKey) obj;
	         return hbefaVehicleCategory.equals(key.getHbefaVehicleCategory())
	         && hbefaComponent.equals(key.getHbefaComponent())
	         && hbefaRoadCategory.equals(key.getHbefaRoadCategory())
	         && hbefaTrafficSituation.equals(key.getHbefaTrafficSituation());
	  }

	// if "equals" is implemented, "hashCode also needs to be implemented
	@Override
	public int hashCode(){
		return toString().hashCode();
	}

	// needed for "hashCode" method
	@Override
	public String toString(){
		return hbefaVehicleCategory + "; " + hbefaComponent + "; " + hbefaRoadCategory + "; " + hbefaTrafficSituation;
	}
}
