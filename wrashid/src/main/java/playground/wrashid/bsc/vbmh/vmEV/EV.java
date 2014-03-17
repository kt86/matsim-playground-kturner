package playground.wrashid.bsc.vbmh.vmEV;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.matsim.api.core.v01.network.Link;
public class EV {
	
	private String id;
	private String ownerPersonId;
	public String evType;
	public double stateOfCharge; //Absolut in KWh 
	public double batteryCapacity; //KWh
	public double consumptionPerHundredKlicks; //  KWh/(100Km)
	@XmlTransient private double enteredLinkTime;
	
	public EV(){
		
	}
	

	public String getId() {
		return id;
	}
	
	
	public void setId(String id) {
		this.id = id;
	}

	@XmlElement (name="OwnerId")
	public String getOwnerPersonId() {
		return ownerPersonId;
	}


	public void setOwnerPersonId(String ownerPersonId) {
		this.ownerPersonId = ownerPersonId;
	}
	
	public double calcEnergyConsumption(Link link, double leftLinkTime){
		double consumption = 0; //KWh
		double timeOnLink = leftLinkTime - this.enteredLinkTime;
		double length = link.getLength();
		double velocity = length/timeOnLink; // !! Im moment nicht verwendet, sollte eingebaut werden.
		consumption = length*(this.consumptionPerHundredKlicks/100000);
		
		return consumption;
		
	}
	
	
	public int discharge(double value){
		this.stateOfCharge=this.stateOfCharge-value;
		if(this.stateOfCharge<0){
			return -1;
		}
		else {
			return 0;
		}
	}


	public void setEnteredLinkTime(double enteredLinkTime) {
		this.enteredLinkTime = enteredLinkTime;
	}
	
	public void setStateOfChargePercentage (double percentage){
		this.stateOfCharge=this.batteryCapacity*percentage/100;
	}
	
	public double getStateOfChargePercentage (){
		return 100*this.stateOfCharge/this.batteryCapacity;
	}
	
	
	
	
	public double calcNewStateOfCharge(double chargingRate, double time){
		double stateOfCharge = this.stateOfCharge;
		double effChargingRate = chargingRate * 0.93;
		double timeNeededFor80p = (this.batteryCapacity*0.8-stateOfCharge)/effChargingRate;
		
		//Laden bis 80%
		if(timeNeededFor80p>0){
			if(time<timeNeededFor80p){
				stateOfCharge+=time*effChargingRate;
				time=0;
			}else{
				stateOfCharge+=timeNeededFor80p*effChargingRate;
				time-=timeNeededFor80p;
			}
		}
		//Laden ab 80%
		effChargingRate = 0.81 * chargingRate;
		stateOfCharge+=effChargingRate*time;
		if(stateOfCharge>batteryCapacity){
			stateOfCharge=batteryCapacity;
		}
		
		return stateOfCharge;
	}


	public void setStateOfCharge(double stateOfCharge) {
		this.stateOfCharge = stateOfCharge;
	}
	

}
