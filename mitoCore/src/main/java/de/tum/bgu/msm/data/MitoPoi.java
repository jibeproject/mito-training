package de.tum.bgu.msm.data;

import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import org.locationtech.jts.geom.Coordinate;

/**
 * Holds POI objects for the Microsimulation Transport Orchestrator (MITO)
 * @author Corin Staves
 * Created on March 5, 2025 in Munich, Germany
 */

public class MitoPoi implements Id, MicroLocation {

    private final int id;
    private final MitoZone zone;
    private final Coordinate coordinate;
    private final ExplanatoryVariable code;
    private final double weight;

    public MitoPoi(int id, MitoZone zone, Coordinate coordinate, ExplanatoryVariable code, double weight) {
        this.id = id;
        this.zone = zone;
        this.coordinate = coordinate;
        this.code = code;
        this.weight = weight;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public int getZoneId() {
        return zone.getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    public ExplanatoryVariable getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof MitoPoi) {
            return id == ((MitoPoi) o).getId();
        } else {
            return false;
        }
    }

    public double getWeight() {
        return this.weight;
    }
}