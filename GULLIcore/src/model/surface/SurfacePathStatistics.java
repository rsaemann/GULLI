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
    
    public double sum_travelLength=0;

    public double distance;
    
    public double minTravelTime=Double.POSITIVE_INFINITY;
    public double maxTravelTime=0;
    
    public double minTravelLength=Double.POSITIVE_INFINITY;
    public double maxTravelLength=0;

    private SurfaceTriangle start;
    private Inlet endInlet;
    private Manhole endManhole;

    public SurfacePathStatistics(SurfaceTriangle start, Inlet endInlet) {
        this.start = start;
        this.endInlet = endInlet;
        this.distance = start.getPosition3D(0).distance(endInlet.getPosition());
    }

    public SurfacePathStatistics(SurfaceTriangle start, Manhole endManhole) {
        this.start = start;
        this.endManhole = endManhole;
        this.distance = start.getPosition3D(0).distance(endManhole.getPosition());
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

    public double getDistance() {
        return distance;
    }

    public SurfaceTriangle getStart() {
        return start;
    }

    public Inlet getEndInlet() {
        return endInlet;
    }

    public Manhole getEndManhole() {
        return endManhole;
    }

    public boolean pathEquals(SurfaceTriangle start, Inlet toInlet) {
        if (endInlet == null || toInlet == null) {
            return false;
        }
        if (this.start.getTriangleID() != start.getTriangleID()) {
            return false;
        }
        return (endInlet.equals(toInlet));
    }

    public boolean pathEquals(SurfaceTriangle start, Manhole toManhole) {
        if (endManhole == null || toManhole == null) {
            return false;
        }
        if (this.start.getTriangleID() != start.getTriangleID()) {
            return false;
        }
        return (endManhole.equals(toManhole));
    }
}
