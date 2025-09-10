package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.input.readers.TripAttractionRatesReader;
import de.tum.bgu.msm.modules.tripGeneration.AttractionCalculator;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable.HH;

public class AttractionCalculatorMCR implements AttractionCalculator {

    private final Purpose purpose;

    private static final Logger logger = LogManager.getLogger(AttractionCalculatorMCR.class);

    private final DataSet dataSet;

    public AttractionCalculatorMCR(DataSet dataSet, Purpose purpose) {
        this.dataSet = dataSet;
        this.purpose = purpose;
        new TripAttractionRatesReader(dataSet, purpose).read();
    }

    @Override
    public void run() {
        logger.info("  Calculating trip attractions for purpose " + purpose.toString());

        // Zone level attractions
        for (MitoZone zone : dataSet.getZones().values()) {
            double tripAttraction = 0;
            for (ExplanatoryVariable variable : ExplanatoryVariable.values()) {
                double attribute;
                if(HH.equals(variable)) {
                    attribute = zone.getNumberOfHouseholds();
                }else if(zone.getPoiWeightsByType().get(variable.toString())==null) {
                    //logger.warn("Zone has no explanatory variable " + variable + " defined.");
                    continue;
                }else{
                    attribute = zone.getPoiWeightsByType().get(variable.toString());
                }
                Double rate = purpose.getTripAttractionForVariable(variable);
                if(rate == null) {
                    throw new RuntimeException("Purpose " + purpose + " does not have an attraction" +
                            " rate for variable " + variable + " registered.");
                }
                tripAttraction += attribute * rate;
            }
            zone.setTripAttraction(purpose, tripAttraction);


            // Weight for specific buildings
            List<MicroLocation> microDestinations = zone.getMicroDestinations();
            if(microDestinations.size() > 0) {
                double[] microDestinationWeights = new double[microDestinations.size()];
                for(int i = 0 ; i < microDestinations.size() ; i++) {
                    MicroLocation loc = microDestinations.get(i);
                    double weight;
                    ExplanatoryVariable code;
                    if(loc instanceof MitoPoi) {
                        weight = ((MitoPoi) loc).getWeight();
                        code = ((MitoPoi) loc).getCode();
                    } else if (loc instanceof MitoVacantDwelling || loc instanceof MitoHousehold) {
                        weight = zone.getVacancyRate();
                        code = HH;
                    } else {
                        throw new RuntimeException("Unrecognised MicroLocation class " + loc.getClass().getName() + " for trip distribution");
                    }
                    microDestinationWeights[i] = weight * purpose.getTripAttractionForVariable(code);
                }
                zone.setMicroDestinationWeightsByPurpose(purpose, microDestinationWeights);
            }

        }
    }
}
