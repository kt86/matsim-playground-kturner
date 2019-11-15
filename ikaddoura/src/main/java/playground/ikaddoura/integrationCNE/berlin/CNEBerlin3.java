/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.integrationCNE.berlin;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.integrationCNE.CNEIntegration;
import playground.ikaddoura.integrationCNE.CNEIntegration.CongestionTollingApproach;
import playground.ikaddoura.moneyTravelDisutility.data.BerlinAgentFilter;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * run class for DZ's berlin scenario
 * 
 * @author ikaddoura
 *
 */

public class CNEBerlin3 {
	private static final Logger log = Logger.getLogger(CNEBerlin3.class);
	
	// TODO: move to config file (emissions)
	private final double xMin = 4565039.;
	private final double xMax = 4632739.; 
	private final double yMin = 5801108.; 
	private final double yMax = 5845708.; 
	
	// TODO: move to config file (emissions)
	private final Double timeBinSize = 3600.;
	
	// TODO: move to config file (emissions)
	private final int noOfTimeBins = 30;

	private static String outputDirectory;
	private static String configFile;

	private static boolean congestionPricing;
	private static boolean noisePricing;
	private static boolean airPollutionPricing;
	
	private static double sigma;
	
	private static CongestionTollingApproach congestionTollingApproach;
	private static double kP;
	private static double toleratedDelay;
		
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			outputDirectory = args[0];
			log.info("Output directory: " + outputDirectory);
			
			configFile = args[1];
			log.info("Config file: " + configFile);
			
			congestionPricing = Boolean.parseBoolean(args[2]);
			log.info("Congestion Pricing: " + congestionPricing);
			
			noisePricing = Boolean.parseBoolean(args[3]);
			log.info("Noise Pricing: " + noisePricing);
			
			airPollutionPricing = Boolean.parseBoolean(args[4]);
			log.info("Air poullution Pricing: " + airPollutionPricing);
			
			sigma = Double.parseDouble(args[5]);
			log.info("Sigma: " + sigma);
			
			String congestionTollingApproachString = args[6];
			
			if (congestionTollingApproachString.equals(CongestionTollingApproach.QBPV3.toString())) {
				congestionTollingApproach = CongestionTollingApproach.QBPV3;
			} else if (congestionTollingApproachString.equals(CongestionTollingApproach.QBPV9.toString())) {
				congestionTollingApproach = CongestionTollingApproach.QBPV9;
			} else if (congestionTollingApproachString.equals(CongestionTollingApproach.DecongestionPID.toString())) {
				congestionTollingApproach = CongestionTollingApproach.DecongestionPID;
			} else if (congestionTollingApproachString.equals(CongestionTollingApproach.DecongestionBangBang.toString())) {
				congestionTollingApproach = CongestionTollingApproach.DecongestionBangBang;
			} else {
				throw new RuntimeException("Unknown congestion pricing approach. Aborting...");
			}
			log.info("Congestion Tolling Approach: " + congestionTollingApproach);
			
			kP = Double.parseDouble(args[7]);
			log.info("kP: " + kP);
			
			toleratedDelay = Double.parseDouble(args[8]);
			log.info("toleratedDelay" + toleratedDelay);
			
		} else {
			
			outputDirectory = "../../../runs-svn/cne/berlin-dz-1pct/output/test/";
			configFile = "../../../runs-svn/cne/berlin-dz-1pct/input/config_m_r.xml";
			
			congestionPricing = true;
			noisePricing = true;
			airPollutionPricing = true;
			
			sigma = 0.;
			
			congestionTollingApproach = CongestionTollingApproach.DecongestionPID;
			kP = 2 * ( 10 / 3600. );	
			toleratedDelay = 30.;
		}
				
		CNEBerlin3 cnControler = new CNEBerlin3();
		cnControler.run();
	}

	public void run() {
						
		Config config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup(), new NoiseConfigGroup(), new DecongestionConfigGroup());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		if (outputDirectory != null) {
			controler.getScenario().getConfig().controler().setOutputDirectory(outputDirectory);
		}
		
		// air pollution Berlin settings
		
		int noOfXCells = 677;
		int noOfYCells = 446;
		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);

		EmissionsConfigGroup emissionsConfigGroup =  (EmissionsConfigGroup) controler.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
		emissionsConfigGroup.setConsideringCO2Costs(true);
		
		// noise Berlin settings
		
		NoiseConfigGroup noiseParameters =  (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
		log.info("noise settings: " + noiseParameters.toString());
		
		// decongestion pricing Berlin settings
		
		final DecongestionConfigGroup decongestionSettings = (DecongestionConfigGroup) controler.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);

		if (congestionTollingApproach.toString().equals(CongestionTollingApproach.DecongestionPID.toString())) {
			
			decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
			decongestionSettings.setKp(kP);
			decongestionSettings.setKi(0.);
			decongestionSettings.setKd(0.);
						
			decongestionSettings.setMsa(true);
			
			decongestionSettings.setToleratedAverageDelaySec(toleratedDelay);
			decongestionSettings.setRunFinalAnalysis(false);
			decongestionSettings.setWriteLinkInfoCharts(false);
			decongestionSettings.setWriteOutputIteration(controler.getConfig().controler().getLastIteration());

		} else if (congestionTollingApproach.toString().equals(CongestionTollingApproach.DecongestionBangBang.toString())) {

			decongestionSettings.setDecongestionApproach(DecongestionApproach.BangBang);
			decongestionSettings.setInitialToll(0.01);
			decongestionSettings.setTollAdjustment(1.0);
			
			decongestionSettings.setMsa(false);
			
			decongestionSettings.setToleratedAverageDelaySec(toleratedDelay);
			decongestionSettings.setRunFinalAnalysis(false);
			decongestionSettings.setWriteLinkInfoCharts(false);
			decongestionSettings.setWriteOutputIteration(controler.getConfig().controler().getLastIteration());
			
		} else {
			// for V3, V9 and V10: no additional settings
		}
		
		// CNE Integration
		
		CNEIntegration cne = new CNEIntegration(controler, gt, rgt);
		cne.setCongestionPricing(congestionPricing);
		cne.setNoisePricing(noisePricing);
		cne.setAirPollutionPricing(airPollutionPricing);
		cne.setSigma(sigma);
		cne.setCongestionTollingApproach(congestionTollingApproach);
		cne.setAgentFilter(new BerlinAgentFilter());

		controler = cne.prepareControler();
				
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		
		String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.run();
		
		{
			final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
	
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
		}
		
		{
			final String[] labels = { "consideredAgentUnits" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" };
	
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
		}
		
		{
			final String[] labels = {"damages_receiverPoint" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
	
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
		}
		
		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = controler.getConfig().controler().getOutputDirectory();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			log.info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}
	}
	
}
