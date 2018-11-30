package model.surface;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import model.topology.Manhole;

/**
 *
 * @author saemann
 */
public class SurfaceLocalMinimumArea {

    private static int laufID = 0;

    public final int id;

    public MultiPoint geomUTM;

    public Geometry geomUTMOuterBoundary;

    public final ArrayList<SurfaceOverrun> overruns = new ArrayList<>(1);
    public SurfaceOverrun massgebendOverrun = null;

    public double waterHeight;
    public FloodArea floodArea = null;
    public FlowArea flowArea = null;

    public List<Manhole> manholeList;
//    public Pipe einlaufPipe;

    public LinkedList<LocalMinimumPoint> points = new LinkedList<>();

    public SurfaceLocalMinimumArea() {
        this.id = laufID++;
    }

    public SurfaceLocalMinimumArea(int id) {
        this.id = id;
    }
    
    public LocalMinimumPoint getLowestPoint(){
        return points.getFirst();
    }

}
