/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.ihop4.sampersutilities;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

/**
 * TODO Make sure that String instances of enums defined here are used when
 * creating the population.
 *
 * @author Gunnar Flötteröd
 * 
 * 
 */
public class SampersParameterUtils {

	// -------------------- CONSTANTS --------------------

	public enum Purpose {
		work, recreation, regularShopping, rareShopping, gymnasium, adultEducation, visit, businessFromHome, giveARide, service, other
	};

	public enum PersonAttribute {
		income_SEK_yr
	};

	public enum ActivityAttribute {
		duration_h, opens_h, closes_h
	}

	// -------------------- DO NOT INSTANTIATE --------------------

	private SampersParameterUtils() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public static int getIncome_sek_yr(final Person person) {
		return (Integer) person.getCustomAttributes().get(PersonAttribute.income_SEK_yr.toString());
	}

	public static double getActivityDuration_min(final Activity act) {
		return 60.0 * (Double) act.getAttributes().getAttribute(ActivityAttribute.duration_h.toString());
	}

	public static double getActivityOpens_min(final Activity act) {
		return 60.0 * (Double) act.getAttributes().getAttribute(ActivityAttribute.opens_h.toString());
	}

	public static double getActivityCloses_min(final Activity act) {
		return 60.0 * (Double) act.getAttributes().getAttribute(ActivityAttribute.closes_h.toString());
	}

	public static double getActivityStart_min(final Activity act) {
		return act.getStartTime() / 60.0;
	}

	public static double getActivityEnd_min(final Activity act) {
		return act.getEndTime() / 60.0;
	}

}
