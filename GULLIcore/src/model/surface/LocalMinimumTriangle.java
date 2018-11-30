package model.surface;

import com.vividsolutions.jts.geom.MultiPolygon;
import java.util.ArrayList;

/**
 *
 * @author saemann
 */
public class LocalMinimumTriangle {

    private static int laufID = 0;
    public final int id ;

    public LocalMinimumTriangleArea area;
    public LocalMinimumTriangle lowerTriangle;
    public float z;
    public int nachbarn=0;

    public MultiPolygon geom;
    public ArrayList<LocalMinimumTriangle> hoehere = new ArrayList<>(2);

    public LocalMinimumTriangle() {
        this.id = laufID++;
    }

    public LocalMinimumTriangle(int id) {
        this.id = id;
    }
    
    
    

    public void inNeueArea(LocalMinimumTriangleArea neueArea) {
        boolean success=this.area.removeTriangle(this);
        if(!success){
            System.out.println("Dreieck"+this.id+" konnte nicht aus Area"+this.area.id+" entfernt werden.");
        }
        neueArea.addTriangle(this);
        for (LocalMinimumTriangle hoehere1 : hoehere) {
            hoehere1.inNeueArea(neueArea);
        }
    }

}
