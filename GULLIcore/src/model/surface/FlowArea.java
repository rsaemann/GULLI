package model.surface;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import java.util.LinkedList;

/**
 *
 * @author saemann
 */
public class FlowArea {

    private static int laufID = 0;

    public final int id;

    public MultiPoint geomUTM;

    public Geometry geomUTMOuterBoundary;
    
    public LinkedList<SurfaceLocalMinimumArea> surfaces=new LinkedList<>();
//     public LinkedList<LocalMinimumPoint> points = new LinkedList<>();

    

    /**
     * Wasserstand üNN, oberhalb dessen das Wasser weiter in eine andere Area
     * läuft.
     */
//    public double ueberlaufWasserstand_uNN = Double.POSITIVE_INFINITY;

    /**
     * Wassertiefe vom tiefsten Punkt, oberhalb dessen das Wasser weiter in eine
     * andere Area läuft.
     */
//    public double ueberlaufWassertiefe = Double.POSITIVE_INFINITY;
    /**
     * ID der surfacearea, in welche das Wasser bei überschreiten des
     * Wasserstandes abgegeben wird.
     */
//    public int surfaceIDUeberlauf = -1;

    public FlowArea() {
        this.id = laufID++;
    }

    public FlowArea(int id) {
        this.id = id;
    }

   
}
