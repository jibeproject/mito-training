package de.tum.bgu.msm.data;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.tum.bgu.msm.data.jobTypes.Category;
import de.tum.bgu.msm.data.jobTypes.JobType;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.SeededRandomPointsBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.*;

/**
 * Created by Nico on 7/7/2017.
 */
public class MitoZone implements Id, Location {

    private final int zoneId;

    /**
     * Gemeindeschluessel?
     */
    private int ags = -1;

    private float reductionAtBorderDamper = 0;
    private int numberOfHouseholds = 0;
    private double householdVacancyRate = 1;
    private int schoolEnrollment = 0;

    private final EnumMap<Purpose, Double> tripAttraction = new EnumMap<>(Purpose.class);
    private final Multiset<JobType> employeesByType = HashMultiset.create();

    //TODO: Manchester use case
    private final HashMap<String, Double> poiWeightsByType = new HashMap<>();

    private MitoZoneCentroid centroid;
    private final List<MicroLocation> microDestinations = new ArrayList<>();
    private final EnumMap<Purpose,double[]> microDestinationWeightsByPurpose = new EnumMap<>(Purpose.class);
    private final AreaTypes.SGType areaTypeSG;
    private AreaTypes.RType areaTypeR;

    private float distanceToNearestRailStop;
    private Geometry geometry;


    public MitoZone(int id, AreaTypes.SGType areaType) {
        this.zoneId = id;
        this.areaTypeSG = areaType;
    }

    //TODO: is this supposed to stay in master?
    //TODO: this decision should be handled by some other class that takes zone candidates as an argument
    public boolean isMunichZone() {
         return 9162000 == ags;
    }

    public AreaTypes.SGType getAreaTypeSG() {
        return areaTypeSG;
    }

    public void setVacancyRate(double vacancyRate) {
        this.householdVacancyRate = vacancyRate;
    }

    public double getVacancyRate() {
        return householdVacancyRate;
    }

    public void addPoi(MitoPoi poi) {
        microDestinations.add(poi);
        poiWeightsByType.merge(poi.getCode().toString(), poi.getWeight(), Double::sum);
    }

    public List<MicroLocation> getMicroDestinations() {
        return microDestinations;
    }

    public void setMicroDestinationWeightsByPurpose(Purpose purpose, double[] weights) {
        microDestinationWeightsByPurpose.put(purpose, weights);
    }

    public Location getRandomBuilding(Purpose purpose, Random random) {
        if(microDestinations.isEmpty()) {
            return centroid;
        } else {
            double[] destinationWeights = microDestinationWeightsByPurpose.get(purpose);
            if(MitoUtil.getSum(destinationWeights) > 0) {
                int idx = MitoUtil.select(destinationWeights,random);
                return microDestinations.get(idx);
            } else {
                return centroid;
            }
        }
    }

    public AreaTypes.RType getAreaTypeR() {
        return areaTypeR;
    }

    public void setAreaTypeR(AreaTypes.RType areaTypeR) {
        this.areaTypeR = areaTypeR;
    }

    public void setCentroid(Coordinate coordinate) {
        centroid = new MitoZoneCentroid(zoneId,coordinate);
    }

    public MitoZoneCentroid getCentroid() {
        return centroid;
    }

    public float getDistanceToNearestRailStop() {
        return distanceToNearestRailStop;
    }

    /**
     * Sets distance to nearest rail stop
     *
     * @param distanceToNearestRailStop distance in km
     */
    public void setDistanceToNearestRailStop(float distanceToNearestRailStop) {
        this.distanceToNearestRailStop = distanceToNearestRailStop;
    }

    @Override
    public int getId() {
        return this.zoneId;
    }

    public float getReductionAtBorderDamper() {
        return reductionAtBorderDamper;
    }

    public void setReductionAtBorderDamper(float damper) {
        this.reductionAtBorderDamper = damper;
    }

    public int getNumberOfHouseholds() {
        return this.numberOfHouseholds;
    }

    public void addHousehold(MitoHousehold hh) {
        this.numberOfHouseholds++;
        this.microDestinations.add(hh);
    }

    public void addVacantDwelling(MitoVacantDwelling dd) {
        this.microDestinations.add(dd);
    }

    public int getSchoolEnrollment() {
        return schoolEnrollment;
    }

    public void addSchoolEnrollment(int schoolEnrollment) {
        this.schoolEnrollment += schoolEnrollment;
    }

    public void addEmployeeForType(JobType type) {
        this.employeesByType.add(type);
    }

    public int getNumberOfEmployeesForType(JobType type) {
        return this.employeesByType.count(type);
    }

    public int getTotalEmpl() {
        return this.employeesByType.size();
    }

    public void setTripAttraction(Purpose purpose, double tripAttractionRate) {
        this.tripAttraction.put(purpose, tripAttractionRate);
    }

    public double getTripAttraction(Purpose purpose) {
        Double rate = this.tripAttraction.get(purpose);
        if (rate == null) {
            throw new RuntimeException("No trip attraction rate set for zone " + zoneId + ". Please make sure to only call " +
                    "this method after trip generation module!");
        }
        return rate;
    }

    public int getEmployeesByCategory(Category category) {
        int sum = 0;
        Multiset<JobType> jobTypes = employeesByType;
        for (JobType distinctType : jobTypes.elementSet()) {
            if (category == distinctType.getCategory()) {
                sum += jobTypes.count(distinctType);
            }
        }
        return sum;
    }

    @Override
    public String toString() {
        return "[MitoZone " + zoneId + "]";
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        return this.geometry;
    }

    public void setAGS(int ags) {
        this.ags = ags;
    }

    public Coordinate getRandomCoord(Random random) {
        SeededRandomPointsBuilder randomPointsBuilder = new SeededRandomPointsBuilder(new GeometryFactory(), random);
        randomPointsBuilder.setNumPoints(1);
        randomPointsBuilder.setExtent(geometry);
        Coordinate coordinate = randomPointsBuilder.getGeometry().getCoordinates()[0];
        Point p = MGC.coordinate2Point(coordinate);
        return new Coordinate(p.getX(), p.getY());
    }

    @Override
    public int hashCode() {
        return zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof MitoZone) {
            return zoneId == ((MitoZone) o).getId();
        } else {
            return false;
        }
    }

    @Override
    public int getZoneId() {
        return zoneId;
    }

    public HashMap<String, Double> getPoiWeightsByType() {
        return poiWeightsByType;
    }

}
