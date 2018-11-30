/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author saemann
 */
public class LocalMinimumPoint {

    public final int id;

    public final int indexX, indexY;
    
    public float minZ;

    public LocalMinimumPoint lowerPoint;

    public LocalMinimumPoint lowerPointFlood;

    public Coordinate coordUTM;

    public ArrayList<LocalMinimumPoint> hoehere = new ArrayList<>(3);

//    public ArrayList<LocalMinimumPoint> hoehereFlood = new ArrayList<>(3);

    public SurfaceLocalMinimumArea surfaceArea;

//    public FlowArea flowArea;

    public LocalMinimumPoint(int id, int indexX, int indexY) {
        this.id = id;
        this.indexX = indexX;
        this.indexY = indexY;
    }

    public LinkedList<LocalMinimumPoint> addAllHoehereSurface(LinkedList<LocalMinimumPoint> list) {
        for (LocalMinimumPoint hoehere1 : hoehere) {
            list.add(hoehere1);
            hoehere1.addAllHoehereSurface(list);
        }
        return list;
    }

//    public LinkedList<LocalMinimumPoint> addAllHoehereFlood(LinkedList<LocalMinimumPoint> list) {
//        for (LocalMinimumPoint hoehere1 : hoehereFlood) {
//            list.add(hoehere1);
//            hoehere1.addAllHoehereFlood(list);
//        }
//        return list;
//    }

}
