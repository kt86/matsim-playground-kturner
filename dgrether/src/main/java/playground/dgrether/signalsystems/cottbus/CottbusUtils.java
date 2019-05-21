/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus;

import java.util.Collection;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author dgrether
 */
public class CottbusUtils {
	
	public static Tuple<CoordinateReferenceSystem, SimpleFeature> loadCottbusFeature(String shapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shapeReader.readFileAndInitialize(shapeFile);
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		for (SimpleFeature feature : features) {
			if (feature.getAttribute("NAME").equals("Cottbus")){
				return new Tuple<>(crs, feature);
			}
		}
		return null;
	}

	public static Tuple<CoordinateReferenceSystem, SimpleFeature> loadFeature(String shapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shapeReader.readFileAndInitialize(shapeFile);
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		SimpleFeature feature = features.iterator().next();
		return new Tuple<>(crs, feature);
	}

	
}
