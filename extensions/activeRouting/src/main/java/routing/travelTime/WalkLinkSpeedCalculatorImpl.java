package routing.travelTime;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;
import routing.WalkConfigGroup;
import routing.components.Gradient;

public class WalkLinkSpeedCalculatorImpl implements WalkLinkSpeedCalculator {

    @Inject
    private WalkConfigGroup walkConfigGroup;

    @Inject
    public WalkLinkSpeedCalculatorImpl( Config config ) {
        this.walkConfigGroup = ConfigUtils.addOrGetModule( config, WalkConfigGroup.class );
    }

    @Override
    public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {
        if (qVehicle.getVehicle().getType().getNetworkMode().equals(walkConfigGroup.getMode())){
            return getMaximumVelocityForLink( link, qVehicle.getVehicle() );
        } else {
            return Double.NaN;
            // (this now works because the link speed calculator returns the default for all combinations of (vehicle, link, time) that
            // are not answered by a specialized link speed calculator.  kai, jun'23)
        }
    }

    @Override
    public double getMaximumVelocityForLink(Link link, Vehicle vehicle) {
        double gradient = Math.min(1,Math.max(-1, Gradient.getGradient(link)));
        double maxWalkSpeed = vehicle == null ? walkConfigGroup.getDefaultMaxWalkSpeed() : vehicle.getType().getMaximumVelocity();
        return maxWalkSpeed * Math.exp(-3.5*Math.abs(gradient + 0.05));
    }

}
