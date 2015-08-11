/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.postprocess;

import playground.johannes.gsv.matrices.episodes2matrix.Episodes2Matrix;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class ScaleMatrices {

    private static final double SCALE_FACTOR = 11.3;

    private static final double DIAGONAL_FACTOR = 1.3;

    public static final String ALL_PATTERN = "all\\.all";

    public static final void main(String args[]) throws IOException {
        String fractionsFile = "";
        String root = "";

        Map<String, Double> upscaleFactors = new HashMap<>();
        upscaleFactors.put(ALL_PATTERN, 1.0);

        Map<String, Double> dayFactors = new HashMap<>();
        dayFactors.put(Episodes2Matrix.SUMMER, 0.998);
        dayFactors.put(Episodes2Matrix.WINTER, 1.002);

        dayFactors.put(CommonKeys.MONDAY, 1.02);
        dayFactors.put(Episodes2Matrix.DIMIDO, 1.07);
        dayFactors.put(CommonKeys.FRIDAY, 1.15);
        dayFactors.put(CommonKeys.SATURDAY, 0.95);
        dayFactors.put(CommonKeys.SUNDAY, 0.67);

        BufferedReader reader = new BufferedReader(new FileReader(fractionsFile));
        String line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split("\t");
            double volume = Double.parseDouble(tokens[2]);
            String pattern = String.format("%s\\.%s", tokens[0], tokens[1]);
            upscaleFactors.put(pattern, volume);
        }
        reader.close();

        File rootDir = new File(root);
        for(File file : rootDir.listFiles()) {
            KeyMatrix m = new KeyMatrix();
            KeyMatrixTxtIO.read(m, file.getAbsolutePath());

            double factor = getFactor(file.getName(), upscaleFactors, dayFactors);

            MatrixOperations.applyFactor(m, SCALE_FACTOR);
            MatrixOperations.applyDiagonalFactor(m, DIAGONAL_FACTOR);
            MatrixOperations.applyFactor(m, factor);
        }
    }

    private static double getFactor(String filename, Map<String, Double> factors, Map<String, Double> dayFactors) {
        double volume = 0;
        for(Map.Entry<String, Double> entry : factors.entrySet()) {
            if(filename.matches(entry.getKey())) {
                volume = entry.getValue();
            }
        }

        if(volume == 0) throw new RuntimeException("Cannot find factor for " + filename);

        double sum = factors.get(ALL_PATTERN);
        double upscaleFactor = sum/volume;

        double dayFactor = 1;
        for(Map.Entry<String, Double> entry : dayFactors.entrySet()) {
            String pattern = String.format(".*\\.%s\\..*", entry.getKey());
            if(filename.matches(pattern)) {
                dayFactor *= entry.getValue();
            }
        }

        return upscaleFactor * dayFactor;
    }
}
