package uk.cam.mrc.phm.calculators;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.google.common.math.LongMath;
import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.input.readers.TimeOfDayDistributionsReader;
import de.tum.bgu.msm.modules.Module;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.List;


public final class TimeOfDayChoiceMCR extends Module {

    private static final Logger logger = LogManager.getLogger(TimeOfDayChoiceMCR.class);

    private EnumMap<Purpose, DoubleMatrix1D> arrivalMinuteCumProbByPurpose;
    private EnumMap<Purpose, DoubleMatrix1D> durationMinuteCumProbByPurpose;
    private EnumMap<Purpose, DoubleMatrix1D> departureMinuteCumProbByPurpose;

    private long counter = 0;
    private int issues = 0;

    public TimeOfDayChoiceMCR(DataSet dataSet, List<Purpose> purposes) {
        super(dataSet, purposes);
        new TimeOfDayDistributionsReader(dataSet, purposes).read();
    }

    @Override
    public void run() {
        arrivalMinuteCumProbByPurpose = dataSet.getArrivalMinuteCumProbByPurpose();
        durationMinuteCumProbByPurpose = dataSet.getDurationMinuteCumProbByPurpose();
        departureMinuteCumProbByPurpose = dataSet.getDepartureMinuteCumProbByPurpose();

        chooseDepartureTimes();
        logger.info("Time of day choice completed");

    }

    private void chooseDepartureTimes() {

        for (Purpose purpose : purposes){
            dataSet.getModelledHouseholds().values().forEach(hh-> {
                List<MitoTrip> trips = hh.getTripsForPurpose(purpose);
                for (MitoTrip trip : trips){
                    if (trip.getTripOrigin() != null && trip.getTripDestination() != null
                            && trip.getTripMode() != null) {
                        int departureTimeInMinutes;
                        int arrivalTimeInMinutes = 0;
                        if (trip.getTripPurpose().equals(Purpose.AIRPORT) &&
                                trip.getTripOrigin().equals(dataSet.getZones().get(Resources.instance.getInt(Properties.AIRPORT_ZONE)))){
                            departureTimeInMinutes = chooseDepartureTime(trip);
                        } else if (trip.getTripPurpose().equals(Purpose.RRT)){
                            departureTimeInMinutes = chooseDepartureTime(trip);
                        } else {
                            arrivalTimeInMinutes = chooseArrivalTime(trip);
                            int estimatedTravelTime = (int) estimateTravelTimeForDeparture(trip, arrivalTimeInMinutes);
                            departureTimeInMinutes = arrivalTimeInMinutes - estimatedTravelTime;
                        }
                        //if departure is before midnight
                        if (departureTimeInMinutes < 0) {
                            departureTimeInMinutes = departureTimeInMinutes + 24 * 60;
                        }
                        trip.setDepartureInMinutes(departureTimeInMinutes);
                        if (trip.isHomeBased()) {
                            trip.setDepartureInMinutesReturnTrip(chooseDepartureTimeForReturnTrip(trip, arrivalTimeInMinutes));
                        }

                    } else {
                        issues++;
                    }
                    counter++;
                    if (LongMath.isPowerOfTwo(counter)) {
                        logger.info(counter + " times of day assigned");
                    }
                }
            });
        }
        logger.warn(issues + " trips have no time of day since they have no origin, destination or mode");
    }

    private int chooseDepartureTime(MitoTrip mitoTrip) {
        return MitoUtil.select(departureMinuteCumProbByPurpose.get(mitoTrip.getTripPurpose()).toArray(), MitoUtil.getRandomObject());
    }

    private int chooseArrivalTime(MitoTrip mitoTrip) {
        Purpose tripPurpose = mitoTrip.getTripPurpose();
        if(tripPurpose == Purpose.HBW || tripPurpose == Purpose.HBE) {
            MitoOccupation occupation = mitoTrip.getPerson().getOccupation();
            if(occupation != null){
                return occupation.getStartTime_min().orElseGet(() -> MitoUtil.select(arrivalMinuteCumProbByPurpose.get(tripPurpose).toArray(), MitoUtil.getRandomObject()));
            }
        }
        return MitoUtil.select(arrivalMinuteCumProbByPurpose.get(tripPurpose).toArray(), MitoUtil.getRandomObject());
    }

    private int chooseDepartureTimeForReturnTrip(MitoTrip mitoTrip, int arrivalTime) {
        Purpose tripPurpose = mitoTrip.getTripPurpose();
        int departureTimeInReturn;
        int duration = MitoUtil.select(durationMinuteCumProbByPurpose.get(tripPurpose).toArray(), MitoUtil.getRandomObject());
        if(tripPurpose == Purpose.HBW || tripPurpose == Purpose.HBE) {
            MitoOccupation occupation = mitoTrip.getPerson().getOccupation();
            if(occupation != null) {
                if(occupation.getEndTime_min().isPresent()){
                    departureTimeInReturn = occupation.getEndTime_min().getAsInt();
                    duration = departureTimeInReturn - arrivalTime;
                }else{
                    departureTimeInReturn = arrivalTime + duration;
                }
            } else {
                departureTimeInReturn = arrivalTime + duration;
            }
        } else {
            departureTimeInReturn = arrivalTime + duration;
        }

        mitoTrip.setActivityDurationInMinutes(duration);

        //if departure is after midnight
        if (departureTimeInReturn > 24 * 60) {
            return departureTimeInReturn - 24 * 60;
        } else {
            return departureTimeInReturn;
        }
    }

    private double estimateTravelTimeForDeparture(MitoTrip trip, double arrivalInMinutes) {
        //currently time of day is not being used here. we only have peak_hour skim matrix for car,walk,bike
        if (trip.getTripMode().equals(Mode.walk)) {
            return dataSet.getTravelTimes().getTravelTime(trip.getTripOrigin(), trip.getTripDestination(), arrivalInMinutes * 60, "walk");
        } else if (trip.getTripMode().equals(Mode.bicycle)) {
            return dataSet.getTravelTimes().getTravelTime(trip.getTripOrigin(), trip.getTripDestination(), arrivalInMinutes * 60, "bike");
        } else {
            //both transit and car use here travel times by car
            return dataSet.getTravelTimes().getTravelTime(trip.getTripOrigin(), trip.getTripDestination(), arrivalInMinutes * 60, "car");
        }
    }
}
