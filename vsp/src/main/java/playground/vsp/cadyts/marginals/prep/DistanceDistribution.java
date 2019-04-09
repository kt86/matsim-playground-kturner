/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.marginals.prep;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils.DistanceDistributionFileLabels;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils.DistanceUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by amit on 21.02.18.
 */

public class DistanceDistribution {

    private static final Logger LOG = Logger.getLogger(DistanceDistribution.class);

    private final SortedMap<Id<ModalDistanceBinIdentifier>, DistanceBin> mode2DistanceBins = new TreeMap<>();
    private final Map<Id<ModalDistanceBinIdentifier>, ModalDistanceBinIdentifier> modalBinMappings = new HashMap<>();
    private final Map<String, Double> modeToBeelineDistanceFactor = new HashMap<>();
    private final Map<String, Double> modeToScalingFactor = new HashMap<>();
    private boolean locked = false;
    private boolean distributionAdded = false;

    public DistanceDistribution (){
        LOG.warn("If simulating sampled population, do not forget to add scaling factors for each mode. In general, this can be same as count scale factors.");
    }

    public void fillDistanceDistribution(String inputFile, DistanceUnit distanceUnit, String itemSeparator) {
        distributionAdded =true;
        if (locked) {
            throw new RuntimeException("Can't add any other data to distribution.");
        }

        LOG.info("Generating distance distribution from file :" + inputFile);
        LOG.info("Header of the file must contain following labels (order is not important): " + Arrays.stream(
                DistanceDistributionFileLabels.values()).map(e -> e.toString() + "\t").reduce("", String::concat));
        // let's assume that input file has headers as
        try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
            String line = reader.readLine();
            List<String> labels = null;

            while (line != null) {
                String[] parts = line.split(itemSeparator);

                if (labels == null) {
                    labels = Arrays.asList(parts);
                    if (labels.size() != DistanceDistributionFileLabels.values().length) {
                        LOG.warn("Labels in the files are " + labels + ". However, desired labels are " + Arrays.stream(
                                DistanceDistributionFileLabels.values()).map(Enum::toString).reduce(",", String::concat));
                    }
                } else {
                    DistanceBin.DistanceRange range = new DistanceBin.DistanceRange(
                            getMultiplierToConvertInMeter(distanceUnit) * Double.valueOf(parts[labels.indexOf(
                                    DistanceDistributionFileLabels.distanceLowerLimit.toString())]),
                            getMultiplierToConvertInMeter(distanceUnit) * Double.valueOf(parts[labels.indexOf(
                                    DistanceDistributionFileLabels.distanceUpperLimit.toString())])
                    );

                    String mode = parts[labels.indexOf(DistanceDistributionFileLabels.mode.toString())];

                    Id<ModalDistanceBinIdentifier> modalBinId = DistanceDistributionUtils.getModalBinId(mode, range);

                    DistanceBin bin = this.mode2DistanceBins.getOrDefault(modalBinId, new DistanceBin(range));
                    bin.addToCount(Double.valueOf(parts[labels.indexOf(DistanceDistributionFileLabels.measuredCount.toString())]));
                    this.mode2DistanceBins.put(modalBinId, bin);
                    //
                    if (this.modalBinMappings.get(modalBinId) == null) {
                        ModalDistanceBinIdentifier identifier = new ModalDistanceBinIdentifier(mode, range);
                        identifier.setScalingFactor(this.modeToScalingFactor.getOrDefault(mode,1.0));
                        this.modalBinMappings.put(modalBinId,identifier);
                    } else{
                        // assuming, only one file will be passed.
                        throw new RuntimeException("The modalBin id "+modalBinId+" already exists.");
                    }
                }

                line = reader.readLine();

            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    // would be used during simulation
    public void addToDistribution(String mode, DistanceBin.DistanceRange distanceRange, double standardDeviation, double val) {
        distributionAdded =true;
        if (locked) {
            throw new RuntimeException("Can't add any other data to distribution.");
        }

        Id<ModalDistanceBinIdentifier> id = DistanceDistributionUtils.getModalBinId(mode, distanceRange);
        DistanceBin bin = this.mode2DistanceBins.getOrDefault(id, new DistanceBin(distanceRange, standardDeviation));
        bin.addToCount(val);
        this.mode2DistanceBins.put(id, bin);
        //
        if (this.modalBinMappings.get(id) == null) {
            ModalDistanceBinIdentifier identifier = new ModalDistanceBinIdentifier(mode, distanceRange);
            identifier.setScalingFactor(this.modeToScalingFactor.getOrDefault(mode,1.0));
            this.modalBinMappings.put(id, identifier);
        }

    }

    public void setModeToScalingFactor(String mode, double factor){
        if (locked) {
            throw new RuntimeException("Can't add any other data to distribution.");
        }

        if (distributionAdded){
            throw new RuntimeException("Add mode-specific scaling factor before adding distribution.");
        }

        this.modeToScalingFactor.put(mode, factor);
    }

    public Set<DistanceBin.DistanceRange> getDistanceRanges(String mode) {
        // don't let anyone change the distribution after this call.
        locked = true;
        return this.modalBinMappings.values()
                                    .stream()
                                    .filter(m -> m.getMode().equals(mode))
                                    .map(ModalDistanceBinIdentifier::getDistanceRange)
                                    .collect(Collectors.toSet());
    }

    public Map<Id<ModalDistanceBinIdentifier>, ModalDistanceBinIdentifier> getModalBins() {
        return this.modalBinMappings;
    }

    public Map<Id<ModalDistanceBinIdentifier>, DistanceBin> getModalBinToDistanceBin() {
        return this.mode2DistanceBins;
    }

    private double getMultiplierToConvertInMeter(DistanceUnit distanceUnit) {
        switch (distanceUnit) {
            case meter:
                return 1.;
            case kilometer:
                return 1000.;
            default:
                throw new RuntimeException("not implemented yet.");
        }
    }

    public void setBeelineDistanceFactorForNetworkModes(String mode, double beelineDistanceFactorForNetworkModes) {
        this.modeToBeelineDistanceFactor.put(mode,beelineDistanceFactorForNetworkModes);
    }

    public Map<String, Double> getModeToBeelineDistanceFactor() {
        return modeToBeelineDistanceFactor;
    }

    public Map<String, Double> getModeToScalingFactor() {
        return modeToScalingFactor;
    }
}
