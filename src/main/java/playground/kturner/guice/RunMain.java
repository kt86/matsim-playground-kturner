/* *********************************************************************** *
 * project: org.matsim.*
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
  
package playground.kturner.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RunMain {
	
	public static SomeInterface someInterface;		//Old stuff

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new BasicModule());
		SomeClass someClasss = injector.getInstance(SomeClass.class);
		someClasss.run();

//		//Old way of doing it.
//		someInterface = new someClassImplemenation2();
//		SomeClass someClass2 = new SomeClass();
//		someClass2.run();
//		
		
	}

}
