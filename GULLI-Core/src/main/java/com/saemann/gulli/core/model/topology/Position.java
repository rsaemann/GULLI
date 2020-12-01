/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.GeoPosition2D;
import java.awt.geom.Point2D;
import org.locationtech.jts.geom.Coordinate;

/**
 * Position with longitude & latitude and a UTM x,y component. To store an
 * elevation (z component), please use Position3D.
 *
 * @author saemann
 */
public class Position extends Point2D.Double implements GeoPosition2D {

    final double longitude, latitude;

    public Position(Position position) {
        this.x = position.x;
        this.y = position.y;
        this.longitude = (float) position.getLongitude();
        this.latitude = (float) position.getLatitude();

    }

    public Position(Position3D position) {
        this.x = position.x;
        this.y = position.y;
        this.longitude = (float) position.getLongitude();
        this.latitude = (float) position.getLatitude();

    }

    public Position(GeoPosition2D position) {
        this.longitude = (float) position.getLongitude();
        this.latitude = (float) position.getLatitude();

    }

    public Position(double longitude, double latitude) {
        this.longitude = (float) longitude;
        this.latitude = (float) latitude;
    }

    public Position(double longitude, double latitude, double utmX, double utmY) {
        super(utmX, utmY);
        this.longitude = (float) longitude;
        this.latitude = (float) latitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    /**
     * Coordinates with WGS84 x:Longitude, y:latitude
     *
     * @return
     */
    public Coordinate lonLatCoordinate() {
        return new Coordinate(longitude, latitude);
    }

    public Position getInterpolatedPosition(double ratio, Position p1) {
        double lat = this.latitude + (ratio * (p1.getLatitude() - latitude));
        double lon = this.longitude + (ratio * (p1.getLongitude() - longitude));
        double newX = this.x + (ratio * (p1.getX() - this.x));
        double newY = this.y + (ratio * (p1.getY() - this.y));
        return new Position(lon, lat, newX, newY);
    }

    @Override
    public String toString() {
        return "Position(lon:" + longitude + ",lat:" + latitude + " ; x:" + x + ",y:" + y + ")";
    }

}
