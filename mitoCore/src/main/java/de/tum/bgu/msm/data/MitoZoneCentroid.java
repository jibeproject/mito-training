package de.tum.bgu.msm.data;

import org.locationtech.jts.geom.Coordinate;

public class MitoZoneCentroid implements MicroLocation {

    final int zoneId;

    final Coordinate coordinate;

    public MitoZoneCentroid(int zoneId, Coordinate coordinate) {
        this.zoneId = zoneId;
        this.coordinate = coordinate;
    }
    @Override
    public int getZoneId() {
        return zoneId;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }
}
