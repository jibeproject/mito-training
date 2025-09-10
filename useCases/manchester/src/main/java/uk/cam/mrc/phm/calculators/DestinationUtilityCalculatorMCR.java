package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DestinationUtilityCalculatorMCR extends AbstractDestinationUtilityCalculator {

    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.1039144842353891, -0.04284696640348192, -0.02963187571928102};
    private final static double IMPEDANCE_PARAM_HBW = 9;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {-0.057144215295112294, -0.046947496113805755, -0.04811119386540696};
    private final static double IMPEDANCE_PARAM_HBE = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = {-0.12696460526884276, -0.03831977013136865, -0.036709100642771816};
    private final static double IMPEDANCE_PARAM_HBA = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.12796183749710527, -0.07047313788533176, -0.06168685242617758};
    private final static double IMPEDANCE_PARAM_HBS = 14.5;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = {-0.09416103189160788, -0.046123749070903455, -0.031241748892175885};
    private final static double IMPEDANCE_PARAM_HBR = 20;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = {-0.028605043869105784, -0.013551373550717408, -0.009975671835593246};
    private final static double IMPEDANCE_PARAM_HBO = 53;

    private final static double[] TRAVEL_DISTANCE_PARAM_RRT = {-0.12078707624647383}; //redundant?
    private final static double IMPEDANCE_PARAM_RRT = 20; //redundant?

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = {-0.3801587104545035, -0.05574431339524572, -0.02990590504345456};
    private final static double IMPEDANCE_PARAM_NHBW = 15.1;

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = {-0.2560855143331614, -0.047285587124733915, -0.04117865752170845};
    private final static double IMPEDANCE_PARAM_NHBO = 20;


    public DestinationUtilityCalculatorMCR(Purpose purpose) {

         switch (purpose) {
            case HBW:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBW;
                impedanceParam = IMPEDANCE_PARAM_HBW;
                break;
            case HBE:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBE;
                impedanceParam = IMPEDANCE_PARAM_HBE;
                break;
             case HBA:
                 distanceParams = TRAVEL_DISTANCE_PARAM_HBA;
                 impedanceParam = IMPEDANCE_PARAM_HBA;
                 break;
            case HBS:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBS;
                impedanceParam = IMPEDANCE_PARAM_HBS;
                break;
            case HBR:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBR;
                impedanceParam = IMPEDANCE_PARAM_HBR;
                break;
            case HBO:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBO;
                impedanceParam = IMPEDANCE_PARAM_HBO;
                break;
            case RRT:
                distanceParams = TRAVEL_DISTANCE_PARAM_RRT;
                impedanceParam = IMPEDANCE_PARAM_RRT;
                break;
            case NHBW:
                distanceParams = TRAVEL_DISTANCE_PARAM_NHBW;
                impedanceParam = IMPEDANCE_PARAM_NHBW;
                break;
            case NHBO:
                distanceParams = TRAVEL_DISTANCE_PARAM_NHBO;
                impedanceParam = IMPEDANCE_PARAM_NHBO;
                break;
            case AIRPORT:
            default:
                throw new RuntimeException("not implemented!");
        }
    }

    @Override
    public List<Predicate<MitoPerson>> getCategories() {
        List<Predicate<MitoPerson>> filters = new ArrayList<>();
        filters.add(0,p -> p.getHousehold().getAutos() == 0);
        filters.add(1,p -> p.getHousehold().getAutosPerAdult() > 0 && p.getHousehold().getAutosPerAdult() < 1);
        filters.add(2,p -> p.getHousehold().getAutosPerAdult() >= 1);
        return filters;
    }
}
