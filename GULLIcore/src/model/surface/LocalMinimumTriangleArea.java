/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.LinkedList;

/**
 *
 * @author saemann
 */
public class LocalMinimumTriangleArea {

    private static int laufID = 0;
    public final int id = laufID++;

    public static GeometryFactory factory = new GeometryFactory();

    public Geometry polygon;
    public Geometry convexHull;
    public LinkedList<LocalMinimumTriangle> dreiecke;
    public double lowestZ = 0;
    public Geometry centroid;
    public double umfang = 0;

    public LocalMinimumTriangleArea(LocalMinimumTriangle startdreieck) {
        dreiecke = new LinkedList<>();
        dreiecke.add(startdreieck);
        polygon = factory.createPolygon(startdreieck.geom.getCoordinates());
        convexHull = polygon;
        startdreieck.area = this;
        lowestZ = startdreieck.z;
        centroid = convexHull.getCentroid();
        umfang = polygon.getLength();
        if (polygon == null) {
            System.out.println("Polygon is null in Constructor");
        }
    }

    public void union(LocalMinimumTriangleArea area) {
        this.dreiecke.addAll(area.dreiecke);
        area.dreiecke.clear();
        area.polygon = null;
        for (LocalMinimumTriangle d : dreiecke) {
            d.area = this;
        }
        this.rebuildPolygon();
    }

    public void addTriangle(LocalMinimumTriangle triangle) {
        dreiecke.add(triangle);

        polygon = polygon.union(triangle.geom);

        triangle.area = this;
        if (polygon == null) {
            System.out.println("Polygon is null in addTriangle");
        }
        lowestZ = Math.min(lowestZ, triangle.z);
        rebuildPolygon();
    }

    public void rebuildPolygon() {
        polygon = null;
        lowestZ = Double.POSITIVE_INFINITY;
        if (dreiecke.isEmpty()) {
            return;
        }
        for (LocalMinimumTriangle d : dreiecke) {
            d.area = this;
            lowestZ = Math.min(lowestZ, d.z);
            if (polygon == null) {
                polygon = factory.createPolygon(d.geom.getCoordinates());
            } else {
                polygon = polygon.union(d.geom);
            }
        }
        polygon.union();
        convexHull = polygon.convexHull();
        centroid = convexHull.getCentroid();
        umfang = convexHull.getLength();
//        polygon=polygon.getBoundary();
//        if(polygon==null){
//            throw new NullPointerException();
//            System.out.println("Polygon is null after rebuilding Polygon");
//        }
    }

    public boolean removeTriangle(LocalMinimumTriangle t) {
        if (dreiecke.remove(t)) {
//            rebuildPolygon();
//            System.out.println("    Dreieck"+t.id+" aus Area"+this.id+" entfernt. nochmal enthalten?"+dreiecke.contains(t));
            return true;
        }
//        System.out.println("    Dreieck"+t.id+" NICHT GEFUNDEN in Area"+this.id);
        return false;
    }

}
