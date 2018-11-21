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
package playground.ikaddoura.integrationCNE;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs.LinkWeightMethod;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class MunichSpatialPlots {

//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run0_muc_bc";
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run0b_muc_bc";
	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run3_muc_c_DecongestionPID";
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run3b_muc_c_DecongestionPID";
//	
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4_muc_cne_DecongestionPID";
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4b_muc_cne_DecongestionPID";
//
//	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run7_muc_n";
	private final String runDir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run7b_muc_n";

	private final double countScaleFactor = 100;
	private static double gridSize ;
	private static double smoothingRadius ;
	private final int noOfBins = 1;
	
	private static final double xMin = 4452550.;
	private static final double xMax = 4479550.;
	private static final double yMin = 5324955.;
	private static final double yMax = 5345755.;
	
//	private static double xMin=4452550.25;
//	private static double xMax=4479483.33;
//	private static double yMin=5324955.00;
//	private static double yMax=5345696.81;
	
	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:31468");
//	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");

	public static void main(String[] args) {
		
		gridSize = 100;
//		gridSize = 500;
		smoothingRadius = 500;
		
		MunichSpatialPlots plots = new MunichSpatialPlots();
		plots.writeEmissionToCells();
	}

	public void writeEmissionToCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line, runDir);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.SQUARE, gridSize);
		inputs.setSmoothingRadius(smoothingRadius);

		SpatialInterpolation plot = new SpatialInterpolation(inputs, runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/");

		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEventsFile, noOfBins);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

		EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
		writer.openWriter(runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/"+"viaData_NOX_"+GridType.SQUARE+"_"+gridSize+"_"+smoothingRadius+"_line.txt");

		for(double time :linkEmissionsBau.keySet()){
			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();

				if(plot.isInResearchArea(l)){
					double emiss = 0;
					if(inputs.isComparing){
						double linkEmissionBau =0;
						double linkEmissionPolicy =0;

						if(linkEmissionsBau.get(time).containsKey(id) && linkEmissionsPolicy.get(time).containsKey(id)) {
							linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NOX.toString());
							linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NOX.toString());
						} else if(linkEmissionsBau.get(time).containsKey(id)){
							linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NOX.toString());
						} else if(linkEmissionsPolicy.get(time).containsKey(id)){
							linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NOX.toString());
						}
						emiss = linkEmissionPolicy - linkEmissionBau;

					} else {
						if(linkEmissionsBau.get(time).containsKey(id)) emiss = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NOX.toString());
						else emiss =0;
					}

					plot.processLink(l,  emiss);
					
				}
			}
			writer.writeData(time, plot.getCellWeights());
			//			plot.writeRData("NO2_"+(int)time/3600+"h",isWritingGGPLOTData);
			plot.reset();
		}
		writer.closeWriter();
	}

	private class EmissionTimebinDataWriter{

		BufferedWriter writer;
		public void openWriter (final String outputFile){
			writer = IOUtils.getBufferedWriter(outputFile);
			try {
				writer.write("timebin\t centroidX \t centroidY \t weight \n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void writeData(final double timebin, final Map<Point,Double> cellWeights){
			try {
				for(Point p : cellWeights.keySet()){
					writer.write(timebin+"\t"+p.getCentroid().getX()+"\t"+p.getCentroid().getY()+"\t"+cellWeights.get(p)+"\n");
				}
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void closeWriter (){
			try {
				writer.close();	
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}
	}
}