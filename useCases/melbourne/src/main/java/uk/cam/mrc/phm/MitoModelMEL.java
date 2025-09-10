package uk.cam.mrc.phm;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.DataSetImpl;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.io.PersonsReader7days;
import de.tum.bgu.msm.io.input.readers.*;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.ImplementationConfig;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.calculators.ModeChoiceCalibrationDataMEL;
import uk.cam.mrc.phm.io.*;
import uk.cam.mrc.phm.io.BicycleOwnershipReaderAndModel;

import java.util.Random;

/**
 * Implements the Microsimulation Transport Orchestrator (MITO)
 *
 * @author Rolf Moeckel
 * Created on Sep 18, 2016 in Munich, Germany
 * <p>
 * To run MITO, the following data need either to be passed in from another program or
 * need to be read from files and passed in (using method initializeStandAlone):
 * - zones
 * - autoTravelTimes
 * - transitTravelTimes
 * - timoHouseholds
 * - retailEmplByZone
 * - officeEmplByZone
 * - otherEmplByZone
 * - totalEmplByZone
 * - sizeOfZonesInAcre
 */
public final class MitoModelMEL {

    private static final Logger logger = LogManager.getLogger(MitoModelMEL.class);
    private final String scenarioName;

    private DataSet dataSet;

    private MitoModelMEL(DataSet dataSet, String scenarioName) {
        this.dataSet = dataSet;
        this.scenarioName = scenarioName;
        MitoUtil.initializeRandomNumber();
        MitoUtil.loadHdf5Lib();
    }

    public static MitoModelMEL standAloneModel(String propertiesFile, ImplementationConfig config) {
        logger.info(" Creating standalone version of MITO ");
        Resources.initializeResources(propertiesFile);
        MitoModelMEL model = new MitoModelMEL(new DataSetImpl(), Resources.instance.getString(Properties.SCENARIO_NAME));
        model.readStandAlone(config);
        return model;
    }

    public static MitoModelMEL initializeModelFromSilo(String propertiesFile, DataSet dataSet, String scenarioName) {
        logger.info(" Initializing MITO from SILO");
        Resources.initializeResources(propertiesFile);
        MitoModelMEL model = new MitoModelMEL(dataSet, scenarioName);
        new OmxSkimsReaderMEL(dataSet).read();
        model.readAdditionalData();
        return model;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Started the Microsimulation Transport Orchestrator (MITO)");

        TravelDemandGeneratorMEL ttd = new TravelDemandGeneratorMEL.Builder(dataSet).build();
        ttd.generateTravelDemand(scenarioName);
        printOutline(startTime);
    }

    private void readStandAlone(ImplementationConfig config) {
        dataSet.setYear(Resources.instance.getInt(Properties.SCENARIO_YEAR));
        new ZonesReaderMEL(dataSet).read();
        if (Resources.instance.getBoolean(Properties.REMOVE_TRIPS_AT_BORDER)) {
            new BorderDampersReader(dataSet).read();
        }
        new JobReaderMEL(dataSet, config.getJobTypeFactory()).read();
        new SchoolsReader(dataSet).read();
        new HouseholdsReader(dataSet).read();
        new HouseholdsCoordReaderMEL(dataSet).read();
        new PersonsReader7days(dataSet).read();
        dataSet.setTravelTimes(new SkimTravelTimes());
        new OmxSkimsReaderMEL(dataSet).read();
        readAdditionalData();
    }

    private void readAdditionalData() {
        dataSet.setModeChoiceCalibrationData(new ModeChoiceCalibrationDataMEL());
        new CalibrationDataReader(dataSet).read();
        new CalibrationRegionMapReader(dataSet).read();
        //TODO: bike model for Manchester?
        new BicycleOwnershipReaderAndModel(dataSet).read();
    }

    private void printOutline(long startTime) {
        String trips = MitoUtil.customFormat("  " + "###,###", dataSet.getTrips().size());
        logger.info("A total of " + trips.trim() + " microscopic trips were generated");
        logger.info("Completed the Microsimulation Transport Orchestrator (MITO)");
        float endTime = MitoUtil.rounder(((System.currentTimeMillis() - startTime) / 60000.f), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes.");
    }

    public DataSet getData() {
        return dataSet;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setRandomNumberGenerator(Random random) {
        MitoUtil.initializeRandomNumber(random);
    }



}
