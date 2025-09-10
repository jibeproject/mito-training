package uk.cam.mrc.phm;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import io.SpeedsReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;
import routing.*;
import routing.components.Gradient;
import routing.components.JctStress;
import routing.components.LinkAmbience;
import routing.components.LinkStress;
import uk.cam.mrc.phm.util.ManchesterImplementationConfig;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class RunMatsimActiveMode {

    private static final Logger logger = LogManager.getLogger(RunMatsimActiveMode.class);

    public static final String ACTIVE_SPEEDS = "C:\\Users\\Corin Staves\\git\\manchester\\input\\maxSpeeds.csv";
    private static final String MATSIM_NETWORK = "C:\\Users\\Corin Staves\\git\\manchester\\input\\mito\\trafficAssignment\\network.xml";
    private static final String MATSIM_PLAN = "C:\\Users\\Corin Staves\\git\\manchester\\scenOutput\\base_fix_7\\2021\\matsimPlans_thursday.xml.gz";
    private static final List<Day> MATSIM_DAY =new ArrayList<>(Collections.singleton(Day.thursday));

    public static void main(String[] args) {
        logger.info("Started the Microsimulation Transport Orchestrator (MITO) based on 2017 models");
        MitoModelMCR model = MitoModelMCR.standAloneModel(args[0], ManchesterImplementationConfig.get());
        //model.run();
        final DataSet dataSet = model.getData();


        boolean runAssignment = Resources.instance.getBoolean(Properties.RUN_TRAFFIC_ASSIGNMENT, false);
        runAssignment = Boolean.TRUE;
        if (runAssignment) {

            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            new PopulationReader(scenario).readFile(MATSIM_PLAN);

            //Load population by mode by day
            Map<Day, Population> populationBikePedByDay = new HashMap<>();
            Map<Day, Population> populationCarByDay = new HashMap<>();

            MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
            for (Person person : scenario.getPopulation().getPersons().values()){
                Day day = Day.valueOf((String)person.getAttributes().getAttribute("day"));
                String mode = mainModeIdentifier.identifyMainMode(TripStructureUtils.getLegs(person.getSelectedPlan()));
                switch (mode) {
                    case "car":
                        populationCarByDay.computeIfAbsent(day, p -> PopulationUtils.createPopulation(ConfigUtils.createConfig())).addPerson(person);
                        break;
                    case "bike":
                    case "walk":
                        populationBikePedByDay.computeIfAbsent(day, p -> PopulationUtils.createPopulation(ConfigUtils.createConfig())).addPerson(person);
                        break;
                    default:
                        continue;
                }
            }


            //initial bike, ped simulation config
            Config bikePedConfig = ConfigUtils.createConfig();
            bikePedConfig.addModule(new BicycleConfigGroup());
            bikePedConfig.addModule(new WalkConfigGroup());
            fillBikePedConfig(bikePedConfig);


            //simulate bikePed by day
            for (Day day : MATSIM_DAY) {
                logger.info("Starting " + day.toString().toUpperCase() + " MATSim simulation");
                String outputSubDirectory = "scenOutput/" + model.getScenarioName() + "/" + dataSet.getYear();
                bikePedConfig.controller().setOutputDirectory(Resources.instance.getBaseDirectory().toString() + "/" + outputSubDirectory + "/trafficAssignment/" + day + "/bikePed/");
                bikePedConfig.controller().setRunId(String.valueOf(dataSet.getYear()));


                //initialize scenario
                MutableScenario matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(bikePedConfig);
                matsimScenario.setPopulation(populationBikePedByDay.get(day));
                logger.info("total population " + day + " | Bike Walk: " + populationBikePedByDay.get(day).getPersons().size());

                // set vehicles
                EnumMap<Mode, EnumMap<MitoGender, Map<Integer,Double>>> allSpeeds = SpeedsReader.readData(ACTIVE_SPEEDS);
                VehiclesFactory fac = VehicleUtils.getFactory();
                for(MitoGender gender : MitoGender.values()) {
                    for(int age = 0 ; age <= 100 ; age++) {
                        VehicleType walk = fac.createVehicleType(Id.create(TransportMode.walk + gender + age, VehicleType.class));
                        walk.setMaximumVelocity(allSpeeds.get(Mode.walk).get(gender).get(age));
                        walk.setNetworkMode(TransportMode.walk);
                        walk.setPcuEquivalents(0.);
                        matsimScenario.getVehicles().addVehicleType(walk);

                        VehicleType bicycle = fac.createVehicleType(Id.create(TransportMode.bike + gender + age, VehicleType.class));
                        bicycle.setMaximumVelocity(allSpeeds.get(Mode.bicycle).get(gender).get(age));
                        bicycle.setNetworkMode(TransportMode.bike);
                        bicycle.setPcuEquivalents(0.);
                        matsimScenario.getVehicles().addVehicleType(bicycle);
                    }
                }

                // Create vehicle for each person (i.e., trip)
                for(Person person : matsimScenario.getPopulation().getPersons().values()) {
                    MitoGender gender = (MitoGender) person.getAttributes().getAttribute("sex");
                    int age = (int) person.getAttributes().getAttribute("age");
                    String mode = (String) person.getAttributes().getAttribute("mode");
                    Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());
                    VehicleType vehicleType = matsimScenario.getVehicles().getVehicleTypes().get(Id.create(mode + gender + age, VehicleType.class));
                    Vehicle veh = fac.createVehicle(vehicleId,vehicleType);
                    Map<String,Id<Vehicle>> modeToVehicle = new HashMap<>();
                    modeToVehicle.put(mode,vehicleId);
                    VehicleUtils.insertVehicleIdsIntoPersonAttributes(person,modeToVehicle);
                    matsimScenario.getVehicles().addVehicle(veh);
                }

                matsimScenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
                matsimScenario.getConfig().qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
                matsimScenario.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);
//                VehicleUtils.writeVehicles(matsimScenario.getVehicles(),"testActiveVehicles.xml");

                // Create active mode networks
                Network activeNetwork = extractModeSpecificNetwork(MATSIM_NETWORK,new HashSet<>(Arrays.asList(TransportMode.bike, TransportMode.walk)));

                matsimScenario.setNetwork(activeNetwork);
//                NetworkUtils.writeNetwork(activeNetwork, "F:\\models\\silo_manchester\\input/mito/trafficAssignment/network_active_cleaned.xml");
                //ConfigUtils.writeMinimalConfig(matsimScenario.getConfig(),"F:\\models\\silo_manchester\\input/mito/trafficAssignment/config_min.xml");

                //set up controler
                final Controler controlerBikePed = new Controler(matsimScenario);
                controlerBikePed.addOverridingModule(new WalkModule());
                controlerBikePed.addOverridingModule(new BicycleModule());


                controlerBikePed.run();
            }
        }
    }

    private static void fillBikePedConfig(Config bikePedConfig) {
        // set input file and basic controler settings
        bikePedConfig.controller().setLastIteration(0);
        bikePedConfig.controller().setWritePlansInterval(Math.max(bikePedConfig.controller().getLastIteration(), 1));
        bikePedConfig.controller().setWriteEventsInterval(Math.max(bikePedConfig.controller().getLastIteration(), 1));
        bikePedConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        // set qsim - passingQ
        bikePedConfig.qsim().setFlowCapFactor(1.);
        bikePedConfig.qsim().setStorageCapFactor(1.);
        bikePedConfig.qsim().setEndTime(24*60*60);
        bikePedConfig.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        // set routing modes
        List<String> mainModeList = new ArrayList<>();
        mainModeList.add(TransportMode.bike);
        mainModeList.add(TransportMode.walk);
        bikePedConfig.qsim().setMainModes(mainModeList);
        bikePedConfig.routing().setNetworkModes(mainModeList);
        bikePedConfig.routing().removeTeleportedModeParams("bike");
        bikePedConfig.routing().removeTeleportedModeParams("walk");
        bikePedConfig.routing().removeTeleportedModeParams("pt");


        // BIKE ATTRIBUTES
        List<ToDoubleFunction<Link>> bikeAttributes = new ArrayList<>();
        bikeAttributes.add(l -> Math.max(Math.min(Gradient.getGradient(l),0.5),0.));
        bikeAttributes.add(l -> LinkStress.getStress(l,TransportMode.bike));

        // Bike weights
        Function<Person,double[]> bikeWeights = p -> {
            switch((Purpose) p.getAttributes().getAttribute("purpose")) {
                case HBW -> {
                    if(p.getAttributes().getAttribute("sex").equals(MitoGender.FEMALE)) {
                        return new double[] {35.9032908,2.3084587 + 2.7762033};
                    } else {
                        return new double[] {35.9032908,2.3084587};
                    }
                }
                case HBE -> {
                    return new double[] {0,4.3075357};
                }
                case HBS, HBR, HBO -> {
                    if((int) p.getAttributes().getAttribute("age") < 15) {
                        return new double[] {57.0135325,1.2411983 + 6.4243251};
                    } else {
                        return new double[] {57.0135325,1.2411983};
                    }
                }
                default -> {
                    return null;
                }
            }
        };

        // Bicycle config group
        BicycleConfigGroup bicycle = (BicycleConfigGroup) bikePedConfig.getModules().get(BicycleConfigGroup.GROUP_NAME);
        bicycle.setAttributes(bikeAttributes);
        bicycle.setWeights(bikeWeights);

        // WALK ATTRIBUTES
        List<ToDoubleFunction<Link>> walkAttributes = new ArrayList<>();
        walkAttributes.add(l -> Math.max(0.,0.81 - LinkAmbience.getVgviFactor(l)));
        walkAttributes.add(l -> Math.min(1.,l.getFreespeed() / 22.35));
        walkAttributes.add(l -> JctStress.getStressProp(l,TransportMode.walk));

        // Walk weights
        Function<Person,double[]> walkWeights = p -> {
            switch ((Purpose) p.getAttributes().getAttribute("purpose")) {
                case HBW -> {
                    return new double[]{0.3307472, 0, 4.9887390};
                }
                case HBE -> {
                    return new double[]{0, 0, 1.0037846};
                }
                case HBS, HBR, HBO -> {
                    if ((int) p.getAttributes().getAttribute("age") < 15) {
                        return new double[]{0.7789561, 0.4479527 + 2.0418898, 5.8219067};
                    } else if ((int) p.getAttributes().getAttribute("age") >= 65) {
                        return new double[]{0.7789561, 0.4479527 + 0.3715017, 5.8219067};
                    } else {
                        return new double[]{0.7789561, 0.4479527, 5.8219067};
                    }
                }
                case HBA -> {
                    return new double[]{0.6908324, 0, 0};
                }
                case NHBO -> {
                    return new double[]{0, 3.4485883, 0};
                }
                default -> {
                    return null;
                }
            }
        };

        // Walk config group
        WalkConfigGroup walkConfigGroup = (WalkConfigGroup) bikePedConfig.getModules().get(WalkConfigGroup.GROUP_NAME);
        walkConfigGroup.setAttributes(walkAttributes);
        walkConfigGroup.setWeights(walkWeights);

        ActivityParams homeActivity = new ActivityParams("home").setTypicalDuration(12 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(homeActivity);

        ActivityParams workActivity = new ActivityParams("work").setTypicalDuration(8 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(workActivity);

        ActivityParams educationActivity = new ActivityParams("education").setTypicalDuration(8 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(educationActivity);

        ActivityParams shoppingActivity = new ActivityParams("shopping").setTypicalDuration(1 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(shoppingActivity);

        ActivityParams recreationActivity = new ActivityParams("recreation").setTypicalDuration(1 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(recreationActivity);

        ActivityParams otherActivity = new ActivityParams("other").setTypicalDuration(1 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(otherActivity);

        ActivityParams airportActivity = new ActivityParams("airport").setTypicalDuration(1 * 60 * 60);
        bikePedConfig.scoring().addActivityParams(airportActivity);


        bikePedConfig.transit().setUsingTransitInMobsim(false);
        bikePedConfig.controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

    }

    public static Network extractModeSpecificNetwork(String networkFile, Set<String> transportModes) {

        Network network = NetworkUtils.readNetwork(networkFile);
        Network modeSpecificNetwork = NetworkUtils.createNetwork();

        new TransportModeNetworkFilter(network).filter(modeSpecificNetwork, transportModes);
        NetworkUtils.runNetworkCleaner(modeSpecificNetwork);
        return modeSpecificNetwork;
    }
}
