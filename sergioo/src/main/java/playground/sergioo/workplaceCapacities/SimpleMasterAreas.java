package playground.sergioo.workplaceCapacities;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class SimpleMasterAreas {

	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final String AREAS_MAP_FILE = "./data/facilities/auxiliar/areasMP.map";
	private static final String POLYGONS_FILE = "./data/facilities/Masterplan_Areas.shp";
	private static final String BUILDINGS_FILE = "./data/facilities/auxiliar/buildings.xml";
	
	private static HashMap<String, Double> workerAreas = new HashMap<String, Double>();
	private static SortedMap<Id, MPAreaData> dataMPAreas = new TreeMap<Id, MPAreaData>();
	private static SortedMap<Id, Double> mPAreasPlotRatio = new TreeMap<Id, Double>();
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		ResultSet mPAreasR = dataBaseAux.executeQuery("SELECT * FROM masterplan_areas WHERE use_for_generation = 1");
		while(mPAreasR.next()) {
			ResultSet mPAreasR2 = dataBaseAux.executeQuery("SELECT ZoneID,`Pu/Pr` FROM DCM_mplan_zones_modeshares WHERE objectID="+mPAreasR.getInt(1));
			mPAreasR2.next();
			dataMPAreas.put(new IdImpl(mPAreasR.getString(1)), new MPAreaData(new IdImpl(mPAreasR.getString(1)), coordinateTransformation.transform(new CoordImpl(mPAreasR.getDouble(6), mPAreasR.getDouble(7))), mPAreasR.getString(2), mPAreasR.getDouble(5), new IdImpl(mPAreasR2.getInt(1)), mPAreasR2.getDouble(2)));
			mPAreasPlotRatio.put(new IdImpl(mPAreasR.getString(1)), mPAreasR.getDouble(3));
		}
		mPAreasR.close();
		//Load polygons
		ShapeFileReader shapeFileReader =  new ShapeFileReader();
		Set<Feature> features = shapeFileReader.readFileAndInitialize(POLYGONS_FILE);
		for(Feature feature:features) {
			MPAreaData area = dataMPAreas.get(new IdImpl((Integer) feature.getAttribute(1)));
			if(area!=null)
				area.setPolygon((Polygon) ((MultiPolygon)feature.getDefaultGeometry()).getGeometryN(0));
		}
		ResultSet typesResult = dataBaseAux.executeQuery("SELECT * FROM masterplan_areas_types");
		while(typesResult.next())
			workerAreas.put(typesResult.getString(1), typesResult.getDouble(2));
		typesResult.close();
		ActivityFacilitiesImpl facilities= new ActivityFacilitiesImpl();
		ResultSet buildingsR = dataBaseAux.executeQuery("SELECT objectid, mpb.x as xcoord, mpb.y as ycoord, perc_surf as area_perc, fea_id AS id_building, postal_code as postal_code, st_area_sh FROM work_facilities_aux.masterplan_areas mpa LEFT JOIN work_facilities_aux.masterplan_building_perc mpb ON mpa.objectid = mpb.object_id  WHERE use_for_generation = 1");
		Collection<Link> noCarLinks = new ArrayList<Link>();
		while(buildingsR.next()) {
			Id areaId =  new IdImpl(buildingsR.getString(1));
			MPAreaData mPArea = dataMPAreas.get(areaId);
			Id id = new IdImpl((int)(buildingsR.getFloat(5)));
			if(facilities.getFacilities().get(id)!=null)
				continue;
			ActivityFacilityImpl building = facilities.createFacility(id, new CoordImpl(buildingsR.getDouble(2), buildingsR.getDouble(3)));
			building.setDesc(buildingsR.getString(6)+":"+mPArea.getType().replaceAll("&", "AND"));
			double capacity = buildingsR.getDouble(7)*mPAreasPlotRatio.get(areaId)/workerAreas.get(mPArea.getType());
			if(capacity>0) {
				ActivityOption activityOption = new ActivityOptionImpl("work", building);
				activityOption.setCapacity(capacity);
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wkday, 0, 24*3600));
				building.getActivityOptions().put(activityOption.getType(), activityOption);
			}
		}
		dataBaseAux.close();
		new FacilitiesWriter(facilities).write(BUILDINGS_FILE);
		/*
		String[] schedules = new String[]{"work"};
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		WorkersBSPainter painter = new WorkersBSPainter(scenario.getNetwork());
		painter.setData(facilities, schedules);
		new BSSimpleNetworkWindow("Building capacities", painter).setVisible(true);
		PrintWriter printWriter = new PrintWriter(new File("./data/workCapacities3.txt"));
		printWriter.print("x,y");
		for(String schedule:schedules)
			printWriter.print(","+schedule);
		printWriter.println(",total");
		for(ActivityFacility facility:facilities.getFacilities().values()) {
			printWriter.print(facility.getCoord().getX()+","+facility.getCoord().getY());
			double total = 0;
			for(String schedule:schedules) {
				ActivityOption option = facility.getActivityOptions().get(schedule);
				if(option==null)
					printWriter.print(","+0);
				else {
					printWriter.print(","+option.getCapacity());
					total += option.getCapacity();
				}
			}
			printWriter.println(","+total);
		}
		printWriter.close();*/
	}

	//Attributes

	//Methods

}
