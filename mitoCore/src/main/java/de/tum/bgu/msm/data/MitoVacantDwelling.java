package de.tum.bgu.msm.data;

import org.locationtech.jts.geom.Coordinate;

public class MitoVacantDwelling implements MicroLocation, Id {

    private final int ddId;

    protected MitoZone dwellingZone;

    protected Coordinate dwellingLocation;

    public MitoVacantDwelling(int ddId, MitoZone zone, Coordinate coordinate) {
        this.ddId = ddId;
        this.dwellingZone = zone;
        this.dwellingLocation = coordinate;
    }


    @Override
    public int getZoneId() {
        return dwellingZone.getId();
    }

    @Override
    public Coordinate getCoordinate() {
        return dwellingLocation;
    }

    @Override
    public int getId() {
        return ddId;
    }
}
