package de.tum.bgu.msm;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class VolumeEventAnalysis {

    private static final String MATSIM_NETWORK = "/home/qin/models/manchester/input/mito/trafficAssignment/network.xml";
    private static final String scenarioName = "silo_mito_matsim_base_UT";
    private static final String day = "sunday";
    private static final String MATSIM_EVENT_ACTIVE = "/home/qin/models/manchester/scenOutput/" + scenarioName + "/matsim/2021/" + day + "/bikePed/2021.output_events.xml.gz";
    private static final String MATSIM_VEHICLES_ACTIVE = "/home/qin/models/manchester/scenOutput/" + scenarioName + "/matsim/2021/" + day + "/bikePed/2021.output_vehicles.xml.gz";
    private static final String OUTPUT_PATH_ACTIVE = "/home/qin/models/manchester/scenOutput/"+ scenarioName + "/matsim/2021/dailyVolume_bikePed_" + day + ".csv";
    private static final String MATSIM_EVENT_CAR = "/home/qin/models/manchester/scenOutput/"+ scenarioName + "/matsim/2021/" + day + "/car/2021.output_events.xml.gz";
    private static final String MATSIM_VEHICLES_CAR = "/home/qin/models/manchester/scenOutput/"+ scenarioName + "/matsim/2021/" + day + "/car/2021.output_vehicles.xml.gz";
    private static final String OUTPUT_PATH_CAR = "/home/qin/models/manchester/scenOutput/"+ scenarioName + "/matsim/2021/dailyVolume_carTruck_" + day + ".csv";
    private static final int SCALE_FACTOR_ACTIVE = 1;
    private static final int SCALE_FACTOR_CAR = 10;

    public static void main(String[] args) {

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(MATSIM_NETWORK);

        Vehicles activeVehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(activeVehicles).readFile(MATSIM_VEHICLES_ACTIVE);

        EventsManager eventsManager = new EventsManagerImpl();
        DailyVolumeEventHandler volumeEventHandler = new DailyVolumeEventHandler(activeVehicles);
        eventsManager.addHandler(volumeEventHandler);
        EventsUtils.readEvents(eventsManager,MATSIM_EVENT_ACTIVE);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(OUTPUT_PATH_ACTIVE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder header = new StringBuilder();
        header.append("linkId,edgeId,osmId,bike,ped");
        pw.println(header);


        for (Link link :  network.getLinks().values()) {
            String linkId = link.getId().toString();
            int edgeId = (int) link.getAttributes().getAttribute("edgeID");
            int osmId = (int) link.getAttributes().getAttribute("osmID");
            int bikeVolumes = volumeEventHandler.getBikeVolumes().getOrDefault(link.getId(),0)*SCALE_FACTOR_ACTIVE;
            int pedVolumes = volumeEventHandler.getPedVolumes().getOrDefault(link.getId(),0)*SCALE_FACTOR_ACTIVE;

            pw.println(linkId + "," + edgeId + "," + osmId + "," + bikeVolumes + "," + pedVolumes);
        }

        pw.close();

        //car truck flow
        Vehicles motorVehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(motorVehicles).readFile(MATSIM_VEHICLES_CAR);

        EventsManager eventsManagerCar = new EventsManagerImpl();
        DailyVolumeEventHandler volumeEventHandlerCar = new DailyVolumeEventHandler(motorVehicles);
        eventsManagerCar.addHandler(volumeEventHandlerCar);
        EventsUtils.readEvents(eventsManagerCar,MATSIM_EVENT_CAR);

        PrintWriter pwCar = null;
        try {
            pwCar = new PrintWriter(new File(OUTPUT_PATH_CAR));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder headerCar = new StringBuilder();
        headerCar.append("linkId,edgeId,osmId,car,truck");
        pwCar.println(headerCar);


        for (Link link :  network.getLinks().values()) {
            String linkId = link.getId().toString();
            int edgeId = (int) link.getAttributes().getAttribute("edgeID");
            int osmId = (int) link.getAttributes().getAttribute("osmID");
            int carVolumes = volumeEventHandlerCar.getCarVolumes().getOrDefault(link.getId(),0) * SCALE_FACTOR_CAR;
            int truckVolumes = volumeEventHandlerCar.getTruckVolumes().getOrDefault(link.getId(),0) * SCALE_FACTOR_CAR;

            pwCar.println(linkId + "," + edgeId + "," + osmId + "," + carVolumes + "," + truckVolumes);
        }

        pwCar.close();

    }
}
