package routing.travelTime;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class WalkTravelTime implements TravelTime {

    @Inject
    private WalkLinkSpeedCalculator linkSpeedCalculator;

    @Inject WalkTravelTime() {
    }

    public WalkTravelTime(WalkLinkSpeedCalculator linkSpeedCalculator) {
        this.linkSpeedCalculator = linkSpeedCalculator;
    }


    @Override
    public double getLinkTravelTime(Link link, double v, Person person, Vehicle vehicle) {
        return link.getLength() / linkSpeedCalculator.getMaximumVelocityForLink(link, vehicle);
    }
}

