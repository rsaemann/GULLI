/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.underground;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import model.GeoPosition2D;
import model.GeoTools;
import model.timeline.array.TimeContainer;
import model.underground.obstacle.Obstacle3D;
import org.opengis.referencing.operation.TransformException;

/**
 * Representing a 3D Volume, with velocity information at nodes. Can Represent
 * soil or air volume.
 *
 * @author saemann
 */
public class Domain3D {

    /**
     * Converter to translate UTM cartesian coordinates to WGS84 (lat/lon)
     */
    public final GeoTools geotools;

    /**
     * Coordinates (UTM) for all nodes
     */
    public final Coordinate[] position;

    /**
     * Number of Nodes
     */
    public int NoP;
    
    /**
     * Number of Cells
     */
    public int NoC;

    /**
     * velocity field at every node [timeindex][nodeindex][0:x/1:y/2:z]
     */
    public final float[][][] velocity;

    /**
     * Distance to groundwater for every node (positive value= node above GWT)
     */
    public final float[] groundwaterDistance;

    /**
     * Bounding Value of z of all nodes.
     */
    public final double maxZ, minZ;

    /**
     * Timecontainer holds information about timesteps. Get the Timeindex from
     * here to calculate velocities at given time.
     */
    public TimeContainer time;
    
    /**
     * Obstacles prevent particles from moving through certain areas.
     */
    public final ArrayList<Obstacle3D> obstacles=new ArrayList<>(0);

    /**
     * Create a 3D Domain
     *
     * @param position Array of UTM Coordinates
     * @param velocity Array of velocities[timeindex][nodeindex][x,y,zComponent]
     * @param groundwaterDistance Distance to GWT for each node
     * @param geotools to convert UTM/WGS84 coordinates
     * @param time Timecontainer to store timeinformation about the velocity
     * field
     */
    public Domain3D(Coordinate[] position, float[][][] velocity, float[] groundwaterDistance, GeoTools geotools, TimeContainer time) {
        this(position, velocity, groundwaterDistance, geotools);
        this.time = time;
    }

    /**
     * @deprecated Because no timecontianer is set shich can cause errors.
     * @param position Array of UTM Coordinates
     * @param velocity Array of velocities[timeindex][nodeindex][x,y,zComponent]
     * @param groundwaterDistance Distance to GWT for each node
     * @param geotools to convert UTM/WGS84 coordinates
     */
    public Domain3D(Coordinate[] position, float[][][] velocity, float[] groundwaterDistance, GeoTools geotools) {
        this.position = position;
        this.velocity = velocity;
        this.groundwaterDistance = groundwaterDistance;
        this.geotools = geotools;
        double[] mz = this.findZBounds(position);
        minZ = mz[0];
        maxZ = mz[1];
    }

    /**
     * Find nearest Node for the given Coordinate
     *
     * @param c Coordinate in same CRS as nodes
     * @return
     */
    public int getNearestCoordinateIndex(Coordinate c) {
        int bestindex = -1;
        double bestdistance = Double.POSITIVE_INFINITY;
        float maxdistanceXY = 50;
        float maxdistanceZ = 250;
        int i = -1;
        for (Coordinate p : position) {
            i++;
            if (Math.abs(p.x - c.x) > maxdistanceXY) {
                continue;
            }
            if (Math.abs(p.y - c.y) > maxdistanceXY) {
                continue;
            }
            if (Math.abs(p.z - c.z) > maxdistanceZ) {
                continue;
            }
            double dx = (p.x - c.x);
            double dy = (p.y - c.y);
            double dz = (p.z - c.z);
            double distance = dx * dx + dy * dy + dz * dz;//No need for sqrt as it only slows the process
            if (distance < bestdistance) {
                bestdistance = distance;
                bestindex = i;
            }
        }
//        System.out.println(getClass()+" bestdistance: "+bestdistance);
        return bestindex;
    }

    /**
     * Searches for the nearest nodes and gives the its velocity at given time.
     *
     * @param utm find nearest Node to this coordinate.
     * @param time in ms as of Date
     * @return
     */
    public float[] getVelocity(Coordinate utm, long time) {
        int index = getNearestCoordinateIndex(utm);
        double interpolation = this.time.getTimeIndexDouble(time);
        int timeindex = (int) interpolation;
        if (timeindex >= this.time.getNumberOfTimes()) {
            return this.velocity[this.time.getNumberOfTimes() - 1][index];
        }
        double fraction = interpolation % 1;
//        System.out.println(timeindex+ " + "+fraction);
        float[] v = new float[3];
        for (int i = 0; i < 3; i++) {
            v[i] = (float) (this.velocity[timeindex][index][i] + fraction * this.velocity[timeindex + 1][index][i] - this.velocity[timeindex][index][i]);
        }
        return v;
    }

    /**
     * Finds Node next to this (Lat/lon)-position and returns its index.
     *
     * @param latlon position given in (Lat/Lon) cordinates
     * @param z elevation.
     * @return index of nearest node
     * @throws TransformException
     */
    public int getNearestCoordinateIndex(GeoPosition2D latlon, double z) throws TransformException {
        Coordinate c = geotools.toUTM(latlon.getLongitude(), latlon.getLatitude());
        System.out.println("Search converted from " + latlon + "\tto " + c);
        c.z = z;
        int bestindex = -1;
        double bestdistance = Double.POSITIVE_INFINITY;
        float maxdistanceXY = 50;
        float maxdistanceZ = 50;
        int i = -1;
        for (Coordinate p : position) {
            i++;

            if (Math.abs(p.x - c.x) > maxdistanceXY) {
                continue;
            }
            if (Math.abs(p.y - c.y) > maxdistanceXY) {
                continue;
            }
            if (Math.abs(p.z - c.z) > maxdistanceZ) {
                continue;
            }
            double dx = (p.x - c.x);
            double dy = (p.y - c.y);
            double dz = (p.z - c.z);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < bestdistance) {
                bestdistance = distance;
                bestindex = i;
            }

        }

        System.out.println("index= " + bestindex + "  best distance=" + bestdistance + "m  nothing found");

        return bestindex;
    }

    /**
     * [min,max] Z-Elevation of the domain.
     *
     * @param position
     * @return [minZ,maxZ] Array
     */
    private double[] findZBounds(Coordinate[] position) {
        double maxZ = Float.MIN_VALUE;
        double minZ = Float.MAX_VALUE;
        for (Coordinate p : position) {
            if (p.z < minZ) {
                minZ = p.z;
            }
            if (p.z > maxZ) {
                maxZ = p.z;
            }
        }
        return new double[]{minZ, maxZ};
    }

    /**
     * Search for lowest resulting velocity.
     *
     * @return
     */
    public double getLowestVelocity() {
        double vlow = Double.POSITIVE_INFINITY;
        for (int t = 0; t < velocity.length; t++) {
            for (int i = 0; i < velocity[0].length; i++) {
                vlow = Math.min(vlow, Math.sqrt(velocity[t][i][0] * velocity[t][i][0] + velocity[t][i][1] * velocity[t][i][1] + velocity[t][i][2] * velocity[t][i][2]));
            }
        }
        return vlow;
    }

    /**
     * Search for highest resulting velocity.
     *
     * @return
     */
    public double getHighestVelocity() {
        double vmax = Double.NEGATIVE_INFINITY;
        for (int t = 0; t < velocity.length; t++) {
            for (int i = 0; i < velocity[0].length; i++) {
                vmax = Math.max(vmax, Math.sqrt(velocity[t][i][0] * velocity[t][i][0] + velocity[t][i][1] * velocity[t][i][1] + velocity[t][i][2] * velocity[t][i][2]));
            }
        }
        return vmax;
    }

}
