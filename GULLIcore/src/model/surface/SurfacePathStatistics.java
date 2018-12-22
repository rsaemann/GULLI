package model.surface;

import model.topology.Inlet;
import model.topology.Manhole;

/**
 *
 * @author saemann
 */
public class SurfacePathStatistics {

    private static int laufId = 0;
    public final int id = laufId++;

    public int number_of_particles = 0;

    public double sum_mass = 0.;

    public long sum_traveltime = 0;

    public double sum_travelLength = 0;

//    public double distance;
    public double minTravelTime = Double.POSITIVE_INFINITY;
    public double maxTravelTime = 0;

    public double minTravelLength = Double.POSITIVE_INFINITY;
    public double maxTravelLength = 0;

    private int startTriangleID;
    private Inlet endInlet;
    private Manhole endManhole;

    public SurfacePathStatistics(int startTriangleID, Inlet endInlet) {
        this.startTriangleID = startTriangleID;
        this.endInlet = endInlet;
//        this.distance = start.getPosition3D(0).distance(endInlet.getPosition());
    }

    public SurfacePathStatistics(int startTriangleID, Manhole endManhole) {
        this.startTriangleID = startTriangleID;
        this.endManhole = endManhole;
//        this.distance = start.getPosition3D(0).distance(endManhole.getPosition());
    }

    public int getNumber_of_particles() {
        return number_of_particles;
    }

    public double getSum_mass() {
        return sum_mass;
    }

    public long getSum_traveltime() {
        return sum_traveltime;
    }

//    public double getDistance() {
//        return distance;
//    }
//
    public int getStart() {
        return startTriangleID;
    }

    public Inlet getEndInlet() {
        return endInlet;
    }

    public Manhole getEndManhole() {
        return endManhole;
    }

    public boolean pathEquals(int startTriangleID, Inlet toInlet) {
        if (endInlet == null || toInlet == null) {
            return false;
        }
        if (this.startTriangleID != startTriangleID) {
            return false;
        }
        return (endInlet.equals(toInlet));
    }

    public boolean pathEquals(int startTriangleID, Manhole toManhole) {
        if (endManhole == null || toManhole == null) {
            return false;
        }
        if (this.startTriangleID != startTriangleID) {
            return false;
        }
        return (endManhole.equals(toManhole));
    }
}
