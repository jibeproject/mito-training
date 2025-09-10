package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.DataSetImpl;
import de.tum.bgu.msm.data.travelDistances.MatrixTravelDistances;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.io.input.AbstractOmxReader;
import de.tum.bgu.msm.io.input.readers.SkimsReader;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OmxSkimsReaderMCR extends AbstractOmxReader implements SkimsReader {

    private static final Logger LOGGER = LogManager.getLogger(OmxSkimsReaderMCR.class);

    public OmxSkimsReaderMCR(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        LOGGER.info("Reading skims");
        readTravelTimeSkims();
        readTravelDistances();
    }

    public void readSkimDistancesAuto(){
        IndexedDoubleMatrix2D distanceSkimAuto = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.AUTO_TRAVEL_DISTANCE_SKIM).toString(),"distanceByTime", 1. / 1000.);
        dataSet.setTravelDistancesAuto(new MatrixTravelDistances(distanceSkimAuto));
    }

    public void readSkimDistancesNMT(){
        IndexedDoubleMatrix2D distanceSkimWalk = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.WALK_DIST_SKIM).toString(),
                Resources.instance.getString(Properties.WALK_DIST_SKIM_MATRIX), 1. / 1000.);
        ((DataSetImpl)dataSet).setTravelDistancesWalk(new MatrixTravelDistances(distanceSkimWalk));
        IndexedDoubleMatrix2D distanceSkimBike = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.BIKE_DIST_SKIM).toString(),
                Resources.instance.getString(Properties.BIKE_DIST_SKIM_MATRIX), 1. / 1000.);
        ((DataSetImpl)dataSet).setTravelDistancesBike(new MatrixTravelDistances(distanceSkimBike));
    }

    public void readOnlyTransitTravelTimes(){
        //todo has to be probably in silo
        SkimTravelTimes skimTravelTimes;
        skimTravelTimes = (SkimTravelTimes) dataSet.getTravelTimes();
        skimTravelTimes.readSkim("pt", Resources.instance.getRelativePath(Properties.PT_PEAK_SKIM).toString(),
                Resources.instance.getString(Properties.PT_PEAK_SKIM_MATRIX), 1/60.);
    }

    private void readTravelTimeSkims() {
        // Travel time skims
        readTravelTimeSkim("car",Properties.AUTO_PEAK_SKIM,Properties.AUTO_PEAK_SKIM_MATRIX);
        readTravelTimeSkim("carCongested",Properties.AUTO_PEAK_SKIM,Properties.AUTO_PEAK_CONGESTED_SKIM_MATRIX);
        readTravelTimeSkim("pt",Properties.PT_PEAK_SKIM,Properties.PT_PEAK_SKIM_MATRIX);
        readTravelTimeSkim("bike",Properties.ACTIVE_TIME_SKIM,Properties.BIKE_COST_SKIM_MATRIX);
        readTravelTimeSkim("walk",Properties.ACTIVE_TIME_SKIM,Properties.WALK_COST_SKIM_MATRIX);

        // HBW
        readTravelTimeSkim("bike_HBW",Properties.ACTIVE_COST_HBW_SKIM,Properties.BIKE_COST_SKIM_MATRIX);
        readTravelTimeSkim("bike_HBW_female",Properties.ACTIVE_COST_HBW_SKIM,Properties.BIKE_COST_FEMALE_SKIM_MATRIX);
        readTravelTimeSkim("walk_HBW",Properties.ACTIVE_COST_HBW_SKIM,Properties.WALK_COST_SKIM_MATRIX);
        
        // HBE
        readTravelTimeSkim("bike_HBE",Properties.ACTIVE_COST_HBE_SKIM,Properties.BIKE_COST_SKIM_MATRIX);
        readTravelTimeSkim("walk_HBE",Properties.ACTIVE_COST_HBE_SKIM,Properties.WALK_COST_SKIM_MATRIX);
        
        // HBA
        readTravelTimeSkim("walk_HBA",Properties.ACTIVE_COST_HBA_SKIM,Properties.WALK_COST_SKIM_MATRIX);
        
        // HBD
        readTravelTimeSkim("bike_HBD",Properties.ACTIVE_COST_HBD_SKIM,Properties.BIKE_COST_SKIM_MATRIX);
        readTravelTimeSkim("bike_HBD_child",Properties.ACTIVE_COST_HBD_SKIM,Properties.BIKE_COST_CHILD_SKIM_MATRIX);
        readTravelTimeSkim("walk_HBD",Properties.ACTIVE_COST_HBD_SKIM,Properties.WALK_COST_SKIM_MATRIX);
        readTravelTimeSkim("walk_HBD_child",Properties.ACTIVE_COST_HBD_SKIM,Properties.WALK_COST_CHILD_SKIM_MATRIX);
        readTravelTimeSkim("walk_HBD_elderly",Properties.ACTIVE_COST_HBD_SKIM,Properties.WALK_COST_ELDERLY_SKIM_MATRIX);

        // NHBO
        readTravelTimeSkim("walk_NHBO",Properties.ACTIVE_COST_NHBO_SKIM,Properties.WALK_COST_SKIM_MATRIX);
    }

    private void readTravelTimeSkim(String name, String omxFilePath, String matrix) {
        // convert second to min, because time is translated to min in mode choice estimation, also min is used in time of day choice
        ((SkimTravelTimes) dataSet.getTravelTimes()).readSkim(name, Resources.instance.getRelativePath(omxFilePath).toString(),
                Resources.instance.getString(matrix), 1/60.);
    }

    private void readTravelDistances(){
        IndexedDoubleMatrix2D distanceSkimAuto = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.AUTO_TRAVEL_DISTANCE_SKIM).toString(),
                Resources.instance.getString(Properties.AUTO_TRAVEL_DISTANCE_SKIM_MATRIX), 1. / 1000.); //meter to km
        dataSet.setTravelDistancesAuto(new MatrixTravelDistances(setMinimumIntrazonalDistances(distanceSkimAuto)));
        IndexedDoubleMatrix2D distanceSkimWalk = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.WALK_DIST_SKIM).toString(),
                Resources.instance.getString(Properties.WALK_DIST_SKIM_MATRIX), 1. / 1000.);
        ((DataSetImpl)dataSet).setTravelDistancesWalk(new MatrixTravelDistances(setMinimumIntrazonalDistances(distanceSkimWalk)));
        dataSet.setTravelDistancesNMT(new MatrixTravelDistances(setMinimumIntrazonalDistances(distanceSkimWalk)));
        IndexedDoubleMatrix2D distanceSkimBike = AbstractOmxReader.readAndConvertToDoubleMatrix(Resources.instance.getRelativePath(Properties.BIKE_DIST_SKIM).toString(),
                Resources.instance.getString(Properties.BIKE_DIST_SKIM_MATRIX), 1. / 1000.);
        ((DataSetImpl)dataSet).setTravelDistancesBike(new MatrixTravelDistances(setMinimumIntrazonalDistances(distanceSkimBike)));
    }

    private IndexedDoubleMatrix2D setMinimumIntrazonalDistances(IndexedDoubleMatrix2D distanceMatrix){
        for (int i = 0; i < Math.min(distanceMatrix.rows(), distanceMatrix.columns()); i++) {
            double value = distanceMatrix.getIndexed(i, i);

            // Set the minimum intrazonal distance to 33 meter
            if (value < 33./1000.) {
                distanceMatrix.setIndexed(i, i, 33./1000.);
            }
        }

        return distanceMatrix;
    }
}
