/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.GeoPosition2D;
import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * @author saemann
 */
public class Position3D extends Position {

    public double z;

    public Position3D(Position3D position) {
        this(position.longitude, position.latitude, position.x, position.y, position.z);
    }
    
    public Position3D(Position position) {
        this(position.longitude, position.latitude, position.x, position.y,0);
    }

    public Position3D(GeoPosition2D position) {
        this(position, 0);
    }
    
    public Position3D(Position position, double z) {
        super(position);
        this.z = z;
    }

    public Position3D(GeoPosition2D position, double z) {
        super(position);
        this.z = z;
    }

    public Position3D(double longitude, double latitude, double z) {
        super(longitude, latitude);
        this.z = z;
    }

    public Position3D(double longitude, double latitude, double utmX, double utmY, double z) {
        super(longitude, latitude, utmX, utmY);
        this.z = z;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Coordinate get3DCoordinate() {
        return new Coordinate(x, y, z);
    }

    /**
     * Coordinate with x:longitude, y:latitude, z:altitude.
     *
     * @return
     */
    public Coordinate lonLatZCoordinate() {
        Coordinate c = lonLatCoordinate();
        c.z = this.z;
        return c;
    }

    public Position3D getInterpolatedPosition(double ratio, Position3D p1) {
        double lat = this.latitude + (ratio * (p1.getLatitude() - latitude));
        double lon = this.longitude + (ratio * (p1.getLongitude() - longitude));
        double newX = this.x + (ratio * (p1.getX() - this.x));
        double newY = this.y + (ratio * (p1.getY() - this.y));
        double newZ = this.z + (ratio * (p1.getZ() - this.z));
        return new Position3D(lon, lat, newX, newY, newZ);
    }

    public double distanceUTM3D(Position3D p) {
        return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z) * (z - p.z));
    }

    public double distanceUTM3D(double x, double y, double z) {
        return Math.sqrt((this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) + (this.z - z) * (this.z - z));
    }

    public double distanceUTM3D(float x, float y, float z) {
        return Math.sqrt((this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) + (this.z - z) * (this.z - z));
    }

    @Override
    public Position3D clone() {
        Position3D p3 = new Position3D(this.longitude, this.latitude, this.x, this.y, this.z);
        return p3;
    }

}
