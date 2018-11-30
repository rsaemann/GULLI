package model.surface;

import com.vividsolutions.jts.geom.Geometry;
import java.util.LinkedList;

/**
 *
 * @author saemann
 */
public class FloodArea {

    private static int laufID = 0;

    public final int id;

    public Geometry geomUTM;

    public SurfaceLocalMinimumArea surfaceArea;

    public final float waterheight;

    public LocalMinimumPoint lowestPoint;

    public LinkedList<LocalMinimumPoint> floodPoints=new LinkedList<>();

    public FloodArea(float waterheight) {
        this(laufID++, waterheight);
    }

    public FloodArea(int id, float waterheight) {
        this.id = id;
        this.waterheight = waterheight;
    }

    public FloodArea(SurfaceLocalMinimumArea floodArea, double waterheight) {
        this.id = floodArea.id;
        this.surfaceArea = floodArea;
        this.lowestPoint = floodArea.points.getFirst();
        this.waterheight = (float) waterheight;
    }

}
