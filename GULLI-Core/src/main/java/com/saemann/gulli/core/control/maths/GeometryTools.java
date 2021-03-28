/*
 * The MIT License
 *
 * Copyright 2018 saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saemann.gulli.core.control.maths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.Polygonizer;

/**
 *
 * @author saemann
 */
public class GeometryTools {

    /**
     * @deprecated Get / create a valid version of the geometry given. If the
     * geometry is a polygon or multi polygon, self intersections /
     * inconsistencies are fixed. Otherwise the geometry is returned.
     *
     * @param geom
     * @return a geometry
     */
    public static Geometry validate(Geometry geom) {
        if (geom instanceof Polygon) {
            if (geom.isValid()) {
                geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
                return geom; // If the polygon is valid just return it
            }
            Polygonizer polygonizer = new Polygonizer();
            addPolygon((Polygon) geom, polygonizer);
            return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
        } else if (geom instanceof MultiPolygon) {
            if (geom.isValid()) {
                geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
                return geom; // If the multipolygon is valid just return it
            }
            Polygonizer polygonizer = new Polygonizer();
//            for (int n = geom.getNumGeometries(); n-- > 0;) {
//                if(geom.getGeometryN(n).getClass().equals(MultiPolygon.class)){
//                    System.out.println("Polygon to be polygonized is Multipolygon");
//                }
//                addPolygon((Polygon) geom.getGeometryN(n), polygonizer);
//            }
            for (int n = 0; n < geom.getNumGeometries(); n++) {
                if (geom.getGeometryN(n).getClass().equals(MultiPolygon.class)) {
                    System.out.println("Polygon to be polygonized is Multipolygon");
                }
                addPolygon((Polygon) geom.getGeometryN(n), polygonizer);
            }
            return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
        } else {
            return geom; // In my case, I only care about polygon / multipolygon geometries
        }
    }

    /**
     * @deprecated Add all line strings from the polygon given to the
     * polygonizer given
     *
     * @param polygon polygon from which to extract line strings
     * @param polygonizer polygonizer
     */
    static void addPolygon(Polygon polygon, Polygonizer polygonizer) {
        addLineString(polygon.getExteriorRing(), polygonizer);
//        for (int n = polygon.getNumInteriorRing(); n-- > 0;) {
//            addLineString(polygon.getInteriorRingN(n), polygonizer);
//        }
        for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
            addLineString(polygon.getInteriorRingN(n), polygonizer);
        }
    }

    /**
     * @deprecated Add the linestring given to the polygonizer
     *
     * @param linestring line string
     * @param polygonizer polygonizer
     */
    static void addLineString(LineString lineString, Polygonizer polygonizer) {

        if (lineString instanceof LinearRing) { // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing
            lineString = lineString.getFactory().createLineString(lineString.getCoordinateSequence());
        }

        // unioning the linestring with the point makes any self intersections explicit.
        Point point = lineString.getFactory().createPoint(lineString.getCoordinateN(0));
        Geometry toAdd = lineString.union(point);

        //Add result to polygonizer
        polygonizer.add(toAdd);
    }

    /**
     * @deprecated Get a geometry from a collection of polygons.
     *
     * @param polygons collection
     * @param factory factory to generate MultiPolygon if required
     * @return null if there were no polygons, the polygon if there was only
     * one, or a MultiPolygon containing all polygons otherwise
     */
    static Geometry toPolygonGeometry(Collection<Polygon> polygons, GeometryFactory factory) {
        switch (polygons.size()) {
            case 0:
                return null; // No valid polygons!
            case 1:
                return polygons.iterator().next(); // single polygon - no need to wrap
            default:
                //polygons may still overlap! Need to sym difference them
                Iterator<Polygon> iter = polygons.iterator();
                Geometry ret = iter.next();
                while (iter.hasNext()) {
                    Geometry toSubstract = iter.next();
                    ret = ret.symDifference(toSubstract);
                }
                return ret;
        }
    }

    public static Geometry validateGeometry(Geometry geom) {
        if (geom.getClass().equals(Polygon.class)) {
            Polygon p = (Polygon) geom;
            if (p.isValid()) {
                p.normalize();
                return geom;
            }
            return validatePolygon(p);
        } else if (geom.getClass().equals(MultiPolygon.class)) {
            MultiPolygon mp = (MultiPolygon) geom;
            if (mp.getNumGeometries() == 1) {
                return validatePolygon((Polygon) mp.getGeometryN(0));
            }
            if (mp.isValid()) {
                mp.normalize();
                return mp;
            }
            return validateMultipolygon(mp);
//        } else if (geom.getClass().equals(GeometryCollection.class)) {
//
//            return validateGeometry(geom);
        } else {
            System.err.println("Can not validate Geometry of type " + geom.getGeometryType());
        }
        return geom;
    }

    public static MultiPolygon validateMultipolygon(MultiPolygon mp) {
        Polygon[] ps = new Polygon[mp.getNumGeometries()];
        for (int i = 0; i < ps.length; i++) {
            Polygon p = (Polygon) mp.getGeometryN(i);
//            if (p.isValid()) {
//                //Directly to array
//                ps[i] = p;
//            } else {
//                //validate first
            ps[i] = validatePolygon(p);
//            }
        }
        return mp.getFactory().createMultiPolygon(ps);
    }

    public static Polygon validatePolygon(Polygon p) {
        LineString outer = p.getExteriorRing();

        if (outer instanceof LinearRing) { // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing
            outer = outer.getFactory().createLineString(outer.getCoordinateSequence());
        }
        Point pointt = outer.getFactory().createPoint(outer.getCoordinateN(0));
        Geometry union = outer.union(pointt);
        LinearRing outerring = outer.getFactory().createLinearRing(union.getCoordinates());

        ArrayList<LinearRing> innerRings = new ArrayList<>(p.getNumInteriorRing());
        Point point = null;
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            LineString inner = p.getInteriorRingN(i);
            if (inner.isValid()) {
                inner.normalize();
            } else {
                if (inner instanceof LinearRing) { // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing                        
//                    System.out.println(i + "  convert inner LinearRing to LineString");
                    inner.normalize();
                    inner = inner.getFactory().createLineString(inner.getCoordinateSequence());
                    point = inner.getFactory().createPoint(inner.getCoordinateN(0));
                    Geometry uni = inner.union(point);
                    if (uni.getClass().equals(MultiLineString.class)) {
                        MultiLineString mls = (MultiLineString) uni;
//                            System.out.println(i + "  is " + mls.getGeometryType() + " with " + mls.getNumGeometries() + " geometries.");

                        for (int j = 0; j < mls.getNumGeometries(); j++) {
                            LineString ls = (LineString) mls.getGeometryN(j);

//                                System.out.println("  " + i + " " + j + " : " + ls.getGeometryType() + " with " + ls.getNumPoints() + " points.");
                            LinearRing lr = null;
                            try {
                                lr = p.getFactory().createLinearRing(ls.getCoordinateSequence());
                                if (!lr.isValid()) {
//                                        System.out.println("    " + i + " " + j + " is still invalid " + lr.getNumPoints() + "  skip");
                                    continue;
                                } else {
                                    innerRings.add(lr);
                                }

                            } catch (Exception e) {
                                //Unclosed linear ring exception. skip this 
//                                    System.out.println("    " + i + " " + j + " could not be build as inner hole...  skip");
                            }

                        }
                        //Valid party already have been put to holes list.
                        continue;
                    }
                } else {
                    inner.normalize();
                }
            }
            LinearRing lr = p.getFactory().createLinearRing(inner.getCoordinateSequence());
            if (!lr.isValid()) {
                continue;
            }
            innerRings.add(lr);
        }
        Polygon poly = p.getFactory().createPolygon(outerring, innerRings.toArray(new LinearRing[innerRings.size()]));
        return poly;
    }

    public static ArrayList<Polygon> splitGeometryToPolygons(Geometry mp) {
        ArrayList<Polygon> polys = new ArrayList<>(mp.getNumGeometries());
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            Geometry g = mp.getGeometryN(i);
            if (g.getClass().equals(Polygon.class)) {
                polys.add((Polygon) g);
            } else if (g.getClass().equals(GeometryCollection.class)) {
                polys.addAll(splitGeometryToPolygons(g));
            } else {
                System.out.println("Can not convert " + g + " to Polygon.");
            }
        }
        return polys;
    }

    public static ArrayList<Polygon> splitGeometryToPolygons(Collection<Geometry> gs) {
        ArrayList<Polygon> polys = new ArrayList<>(gs.size());
        for (Geometry mp : gs) {
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Geometry g = mp.getGeometryN(i);
                if (g.getClass().equals(Polygon.class)) {
                    polys.add((Polygon) g);
                } else if (g.getClass().equals(GeometryCollection.class)) {
                    polys.addAll(splitGeometryToPolygons(g));
                } else {
                    System.out.println("Can not convert " + g + " to Polygon.");
                }
            }
        }
        return polys;
    }

    public static MultiPolygon toMultipolygon(Collection<Polygon> polys) {
        Polygon[] pol = new Polygon[polys.size()];
        int index = 0;
        Iterator<Polygon> it = polys.iterator();
        while (it.hasNext()) {
            pol[index] = it.next();
            index++;
        }
        return pol[0].getFactory().createMultiPolygon(pol);

    }

    /**
     *
     * @param p0_x
     * @param p0_y
     * @param p1_x
     * @param p1_y
     * @param p2_x
     * @param p2_y
     * @param p3_x
     * @param p3_y
     * @return [0:factor along p0-p1,1:factor along p2-p3]
     */
    public static double[] lineIntersectionST(double p0_x, double p0_y, double p1_x, double p1_y,
            double p2_x, double p2_y, double p3_x, double p3_y) {
        double s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        double s, t;
        t = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        s = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        return new double[]{s, t};
    }

    /**
     *
     * @param tempReturn
     * @param p0_x
     * @param p0_y
     * @param p1_x
     * @param p1_y
     * @param p2_x
     * @param p2_y
     * @param p3_x
     * @param p3_y
     * @return [0:factor along p0-p1,1:factor along p2-p3]
     */
    public static double[] lineIntersectionST(double[] tempReturn, double p0_x, double p0_y, double p1_x, double p1_y,
            double p2_x, double p2_y, double p3_x, double p3_y) {
        double s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        tempReturn[1] = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        tempReturn[0] = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        return tempReturn;
    }

    /**
     *
     * @param p0_x
     * @param p0_y
     * @param p1_x
     * @param p1_y
     * @param p2_x
     * @param p2_y
     * @param p3_x
     * @param p3_y
     * @param tofill
     * @return [0:factor along p0-p1,1:factor along p2-p3]
     */
    public static double[] lineIntersectionST(double p0_x, double p0_y, double p1_x, double p1_y,
            double p2_x, double p2_y, double p3_x, double p3_y, double[] tofill) {

        double s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        double s, t;
        t = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        s = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (tofill == null) {
            System.out.println("new double[] for lineintersectionTS");
            return new double[]{s, t};
        } else {
            tofill[0] = s;
            tofill[1] = t;
            return tofill;
        }

    }

    /**
     * returns the length factor of line a-b, to reach the crossing point with
     * line c-d.
     *
     * @param ax
     * @param ay
     * @param bx
     * @param by
     * @param cx
     * @param cy
     * @param dx
     * @param dy
     * @return length factor s.
     */
    public static double lineIntersectionS(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        return ((dx - cx) * (ay - cy) - (dy - cy) * (ax - cx)) / (-(dx - cx) * (by - ay) + (bx - ax) * (dy - cy));
    }

    /**
     * returns the length factor of line a-b, to reach the crossing point with
     * line c-d.
     *
     * @param ax
     * @param ay
     * @param cx
     * @param cy
     * @param bx
     * @param by
     * @param dx
     * @param dy
     * @return length factor s.
     */
    public static double lineIntersectionS(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        return ((dx - cx) * (ay - cy) - (dy - cy) * (ax - cx)) / (-(dx - cx) * (by - ay) + (bx - ax) * (dy - cy));

    }

    /**
     *
     * @param x1
     * @param x2
     * @param x3
     * @param y1
     * @param y2
     * @param y3
     * @param px
     * @param py
     * @return
     */
    public static double[] getBarycentricWeighing(double x1, double x2, double x3, double y1, double y2, double y3, double px, double py) {
        // barycentric koordinate weighing for velocity calculation
        // x, y = triangle coordinates, p = searched point
        double[] w = new double[3];

        w[0] = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        w[1] = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        w[2] = 1 - w[0] - w[1];

        double wenorm_ges = w[0] + w[1] + w[2];
        if (wenorm_ges > 1.01 || wenorm_ges < 0.99) {
            System.err.println("weighting is not 1!");
        }
        return w;
    }

    /**
     *
     * @param tofill
     * @param x1
     * @param x2
     * @param x3
     * @param y1
     * @param y2
     * @param y3
     * @param px
     * @param py
     */
    public static void fillBarycentricWeighting(double[] tofill, double x1, double x2, double x3, double y1, double y2, double y3, double px, double py) {
        // barycentric koordinate weighing for velocity calculation
        // x, y = triangle coordinates, p = searched point
//        double[] w = tofill;

        tofill[0] = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        tofill[1] = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        tofill[2] = 1 - tofill[0] - tofill[1];
    }
    
    public static void calcPositionFromBarycentric(double[] toFillPosition,double x1, double x2, double x3, double y1, double y2, double y3, double bw1, double bw2, double bw3){
        toFillPosition[0]=x1*bw1+x2*bw2+x3*bw3;
        toFillPosition[1]=y1*bw1+y2*bw2+y3*bw3;
    }

    /**
     * Test if a triangle is contianing a point.
     *
     * @param x1 x ccordinate of first triangle node
     * @param x2 x ccordinate of second triangle node
     * @param x3
     * @param y1 y ccordinate of first triangle node
     * @param y2
     * @param y3
     * @param px x ccordinate of point
     * @param py
     * @return
     */
    public static boolean triangleContainsPoint(double x1, double x2, double x3, double y1, double y2, double y3, double px, double py) {
        // barycentric koordinate weighing for velocity calculation
        // x, y = triangle coordinates, p = searched point

        double w0 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (w0 < 0) {
            return false;
        }
        double w1 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (w1 < 0) {
            return false;
        }
        double w2 = 1 - w0 - w1;
        if (w2 < 0) {
            return false;
        }
        return true;
//        double wenorm_ges = w[0] + w[1] + w[2];
//        if (wenorm_ges > 1.01 || wenorm_ges < 0.99) {
//            System.err.println("weighting is not 1!");
//        }
//        return w;
    }

    public static double trianglesArea(double x0, double y0, double x1, double y1, double x2, double y2) {
        return Math.abs(x0 * (y1 - y2) + x1 * (y2 - y0) + x2 * (y0 - y1)) / 2.;
    }

    public static double distancePointToLine(double x1, double y1, double x2, double y2, double xp, double yp) {
        double ax = x2 - x1;
        double ay = y2 - y1;
        double bx = xp - x1;
        double by = yp - y1;

        return Math.abs((ax * by - ay * bx) / Math.sqrt(ax * ax + ay * ay));
    }

    public static double distancePointToLineSegment(double x1, double y1, double x2, double y2, double xp, double yp) {
        double seg = distancePointToLine(x1, y1, x2, y2, xp, yp);
        double dp1 = Math.sqrt((xp - x1) * (xp - x1) - (yp - y1) * (yp - y1));
        double dp2 = Math.sqrt((xp - x2) * (xp - x2) - (yp - y2) * (yp - y2));
        return Math.min(seg, Math.min(dp2, dp1));
    }

    public static double distancePointAlongLine(double x1, double y1, double x2, double y2, double xp, double yp) {
        double ax = x2 - x1;
        double ay = y2 - y1;
        double bx = xp - x1;
        double by = yp - y1;
        double sqralength = (ax * ax + ay * ay);
        double afactor = (ax * bx + ay * by) / sqralength;

        return afactor * Math.sqrt(sqralength);
    }

    /**
     *
     * @param pointX
     * @param pointY
     * @param lineStartX
     * @param lineStartY
     * @param lineEndX
     * @param lineEndY
     * @param projectedpoint double[2] array to be filled with x and y
     * coordinate of the projection
     * @return length factor along the line, where the projected point is
     * located
     */
    public static double projectPointToLine(double pointX, double pointY, double lineStartX, double lineStartY, double lineEndX, double lineEndY, double[] projectedpoint) {
        double ex = pointX - lineStartX;
        double ey = pointY - lineStartY;

        double ax = lineEndX - lineStartX;
        double ay = lineEndY - lineStartY;

        double f = (ex * ax + ey * ay) / (ax * ax + ay * ay);

//        double sum=ex*ax+ey*ay;
//        
//        double length=Math.sqrt(ax*ax+ay*ay);
//        
//        double f=sum/length;
//        
        projectedpoint[0] = lineStartX + ax * f;
        projectedpoint[1] = lineStartY + ay * f;

//        projectedpoint[0]=
        return f;

    }

//    public static void main(String[] args) {
//        double[] p0 = new double[]{0, 0};
//        double[] p1 = new double[]{0, 2};
//        double[] p2 = new double[]{-1, 1};
//        double[] p3 = new double[]{0, 1};
//
//        double s = GeometryTools.lineIntersectionS(p0[0], p0[1], p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
//        System.out.println("s=" + s);
//
//        double[] s2 = GeometryTools.lineIntersectionST(p0[0], p0[1], p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
//        System.out.println("s=" + s2[0] + "  /  " + s2[1]);
//    }
}
