package model;

import com.vividsolutions.jts.geom.Coordinate;
import java.awt.geom.Point2D;

public class GeoPosition implements GeoPosition2D {

    double lat, lon;

    public GeoPosition(double latitude, double longitude) {
        super();
        this.lat = latitude;
        this.lon = longitude;
    }

    public GeoPosition(Point2D point) {
        super();
        this.lat = point.getY();
        this.lon = point.getX();
    }

    public GeoPosition(GeoPosition2D geoPosition2D) {
        super();
        this.lat = geoPosition2D.getLatitude();
        this.lon = geoPosition2D.getLongitude();
    }

    public GeoPosition(Coordinate coordinate) {
        super();
        this.lat = coordinate.y;
        this.lon = coordinate.x;
    }

    public GeoPosition(com.vividsolutions.jts.geom.Point com_vividsolutions_jts_geom_Point, boolean switchcoordinates) {
        super();
        if (!switchcoordinates) {
            this.lat = com_vividsolutions_jts_geom_Point.getY();
            this.lon = com_vividsolutions_jts_geom_Point.getX();
        } else {
            this.lat = com_vividsolutions_jts_geom_Point.getX();
            this.lon = com_vividsolutions_jts_geom_Point.getY();
        }
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lon;
    }

    @Override
    public String toString() {
        return "GeoPosition(lat:" + lat + ",lon:" + lon + ")";
    }

}
