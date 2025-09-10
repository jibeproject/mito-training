package routing.travelTime;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class BicycleTravelTime implements TravelTime {

    @Inject
    private BicycleLinkSpeedCalculator linkSpeedCalculator;

    @Inject
    BicycleTravelTime() {
    }

    public BicycleTravelTime(BicycleLinkSpeedCalculator linkSpeedCalculator) {
        this.linkSpeedCalculator = linkSpeedCalculator;
    }

    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / this.linkSpeedCalculator.getMaximumVelocityForLink(link, vehicle);
    }
}
