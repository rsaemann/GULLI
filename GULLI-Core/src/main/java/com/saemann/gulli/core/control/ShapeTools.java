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
package com.saemann.gulli.core.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.surface.SurfaceTriangle;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.Position3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class ShapeTools {

    /**
     * Creates clusters of contaminated areas.
     *
     * @param surf
     * @param contaminated
     * @param clusterCount
     * @param loops
     * @return
     */
    public static ArrayList<LinkedList<Integer>> calcTriangleGroupContaminated(Surface surf, Collection<Integer> contaminated, int clusterCount, int loops) {
        if (contaminated == null || contaminated.isEmpty()) {
            return null;
        }
        long starttime = System.currentTimeMillis();
        ArrayList<LinkedList<Integer>> clusters = new ArrayList<>(clusterCount);
        //Find boundary of map
        double maxX = Float.NEGATIVE_INFINITY, minX = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        for (Integer t : contaminated) {
            double[] mid = surf.getTriangleMids()[t];

            maxX = Math.max(maxX, mid[0]);
            maxY = Math.max(maxY, mid[1]);
            minX = Math.min(minX, mid[0]);
            minY = Math.min(minY, mid[1]);
        }
        Coordinate[] clusternodes = new Coordinate[clusterCount];

        //Cluster entlang der tatsächlichen Nodes anordnen
        ArrayList<Integer> t = new ArrayList<>(contaminated);
        for (int i = 0; i < clusternodes.length; i++) {
            int sft = t.get((int) (t.size() * (i / (double) clusternodes.length)));
            double[] mid = surf.getTriangleMids()[sft];
            clusternodes[i] = new Coordinate(mid[0], mid[1]);

        }

        //fertig: gleichmäßiges cluster Raster erstellt 
        //1. Runde Positionen zuordnen
        for (int j = 0; j < clusterCount; j++) {
            clusters.add(new LinkedList<Integer>());
        }

        for (Integer tri : contaminated) {
            double distance = Double.POSITIVE_INFINITY;
            int bestIndex = -1;
            for (int j = 0; j < clusternodes.length; j++) {
                double[] mid = surf.getTriangleMids()[j];

//                double tempDist = clusternodes[j].distance(new Coordinate(mid[0], mid[1]));
                double tempDist = (clusternodes[j].x - mid[0]) * (clusternodes[j].x - mid[0]) + (clusternodes[j].y - mid[1]) * (clusternodes[j].y - mid[1]);
                if (tempDist < distance) {
                    distance = tempDist;
                    bestIndex = j;
                }
            }
            if (bestIndex >= 0) {
                clusters.get(bestIndex).add(tri);
            }
        }

        //End first sorting
//        System.out.println("  Raster sorting: " + (System.currentTimeMillis() - starttime) + "ms.");
        long starttime2 = System.currentTimeMillis();
        //Reorganize clusterpoints
        double x = 0, y = 0;
        for (int l = 0; l < loops; l++) {

            for (int j = 0; j < clusternodes.length; j++) {
                x = 0;
                y = 0;
                for (Integer tri : clusters.get(j)) {
                    double[] mid = surf.getTriangleMids()[tri];
                    x += mid[0];
                    y += mid[1];
                }
                clusternodes[j] = new Coordinate(x / (double) clusters.get(j).size(), y / (double) clusters.get(j).size());
                clusters.get(j).clear();
            }
            //2. Runde Positionen zuordnen

            for (Integer tri : contaminated) {
                double distance = Double.POSITIVE_INFINITY;
                int bestIndex = -1;
                for (int j = 0; j < clusternodes.length; j++) {
                    double[] mid = surf.getTriangleMids()[j];
                    //double tempDist = clusternodes[j].distance(new Coordinate(mid[0], mid[1]));
                    double tempDist = (clusternodes[j].x - mid[0]) * (clusternodes[j].x - mid[0]) + (clusternodes[j].y - mid[1]) * (clusternodes[j].y - mid[1]);

                    if (tempDist < distance) {
                        distance = tempDist;
                        bestIndex = j;
                    }
                }
                if (bestIndex >= 0) {
                    clusters.get(bestIndex).add(tri);
                }
            }
        }
        //End second sorting
//        System.out.println("  Raster  sorting: " + (System.currentTimeMillis() - starttime) + "ms.");

        return clusters;
    }

    /**
     * Creates clusters of contaminated areas.
     *
     * @param tris
     * @param clusterCount target number of clusters (for k means)
     * @param loops loops for k means search
     * @return
     */
    public static ArrayList<LinkedList<SurfaceTriangle>> calcTriangleGroupContaminated(Collection<SurfaceTriangle> tris, int clusterCount, int loops) {
        if (tris == null || tris.isEmpty()) {
            return null;
        }
        long starttime = System.currentTimeMillis();
        ArrayList<LinkedList<SurfaceTriangle>> clusters = new ArrayList<>(clusterCount);
        //Find boundary of map
        double maxX = Float.NEGATIVE_INFINITY, minX = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        for (SurfaceTriangle t : tris) {
            Position3D pos = t.getPosition3D(0);
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
        }
        Position[] clusternodes = new Position[clusterCount];
        //Clusterpositionen möglichst gleichverteilt anordnen
//        int rows = (int) (Math.sqrt(clusterCount) + 1);
//        int colls = clusterCount / rows;
//        int i = 0;
//        System.out.println("Raster: " + rows + " x " + colls + "    : clusters:" + clusterCount);
//        double dx = (maxX - minX) / (double) colls;
//        double dy = (maxY - minY) / (double) rows;
//        for (int r = 0; r < rows - 1; r++) {
//            for (int c = 0; c < colls; c++) {
//                clusternodes[i] = new Position(0, 0, minX + c * dx, minY + r * dy);
//                i++;
//            }
//        }
//        //verbleibende knoten in der letzten zeile
//        colls = clusterCount - ((rows - 1) * colls);
//        dx = (maxX - minX) / (double) colls;
//        double y = maxY - dy;
//        for (int c = 0; c < colls; c++) {
//            clusternodes[i] = new Position(0, 0, minX + c * dx, y);
//            i++;
//        }
        //Cluster entlang der tatsächlichen Nodes anordnen
        ArrayList<SurfaceTriangle> t = new ArrayList<>(tris);
        for (int i = 0; i < clusternodes.length; i++) {
            SurfaceTriangle sft = t.get((int) (t.size() * (i / (double) clusternodes.length)));
            clusternodes[i] = sft.getPosition3D(0);

        }

        //fertig: gleichmäßiges cluster Raster erstellt 
        //1. Runde Positionen zuordnen
        for (int j = 0; j < clusterCount; j++) {
            clusters.add(new LinkedList<SurfaceTriangle>());
        }

        for (SurfaceTriangle tri : tris) {
            double distance = Double.POSITIVE_INFINITY;
            int bestIndex = -1;
            for (int j = 0; j < clusternodes.length; j++) {
                double tempDist = clusternodes[j].distance(tri.getPosition3D(0));
                if (tempDist < distance) {
                    distance = tempDist;
                    bestIndex = j;
                }
            }
            if (bestIndex >= 0) {
                clusters.get(bestIndex).add(tri);
            }
        }

        //End first sorting
//        System.out.println("  Raster sorting: " + (System.currentTimeMillis() - starttime) + "ms.");
        long starttime2 = System.currentTimeMillis();
        //Reorganize clusterpoints
        double x = 0, y = 0;
        for (int l = 0; l < loops; l++) {

            for (int j = 0; j < clusternodes.length; j++) {
                x = 0;
                y = 0;
                for (SurfaceTriangle tri : clusters.get(j)) {
                    x += tri.getPosition3D(0).getX();
                    y += tri.getPosition3D(0).getY();
                }
                clusternodes[j] = new Position(0, 0, x / (double) clusters.get(j).size(), y / (double) clusters.get(j).size());
                clusters.get(j).clear();
            }
            //2. Runde Positionen zuordnen

            for (SurfaceTriangle tri : tris) {
                double distance = Double.POSITIVE_INFINITY;
                int bestIndex = -1;
                for (int j = 0; j < clusternodes.length; j++) {
                    double tempDist = clusternodes[j].distance(tri.getPosition3D(0));
                    if (tempDist < distance) {
                        distance = tempDist;
                        bestIndex = j;
                    }
                }
                if (bestIndex >= 0) {
                    clusters.get(bestIndex).add(tri);
                }
            }
        }
        //End second sorting
//        System.out.println("  Raster  sorting: " + (System.currentTimeMillis() - starttime) + "ms.");

        return clusters;
    }

    /**
     *
     * @param surface
     * @param clusterCount
     * @param clusterloops
     * @param materialIndex select -1 for all
     * @param minimumCount exclude triangles with less than this amount of
     * particlecount
     * @return
     * @throws TransformException
     */
    public static ArrayList<Geometry> createShapesWGS84(Surface surface, int clusterCount, int clusterloops, int materialIndex, int minimumCount) throws TransformException {
        return createShapesWGS84(surface, clusterCount, clusterloops, materialIndex, minimumCount, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
    }

    /**
     *
     * @param surface
     * @param clusterCount
     * @param clusterloops
     * @param materialIndex select -1 for all
     * @param minimumCount exclude triangles with less than this amount of
     * partic
     * @param longitudeFirst transform to (longitude,latitude) wgs84 coordinates
     * @return
     * @throws TransformException
     */
    public static ArrayList<Geometry> createShapesWGS84(Surface surface, int clusterCount, int clusterloops, int materialIndex, int minimumCount, boolean longitudeFirst) throws TransformException {
        if (surface.getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
            SurfaceMeasurementRectangleRaster raster = (SurfaceMeasurementRectangleRaster) surface.getMeasurementRaster();
            GeometryFactory gf = new GeometryFactory();
            LinkedList<Polygon> rectangles = new LinkedList<>();
            for (int x = 0; x < raster.getParticlecounter().length; x++) {
                long[][][] dim1 = raster.getParticlecounter()[x];

                if (dim1 == null) {
                    continue;
                }
                for (int y = 0; y < dim1.length; y++) {
                    long[][] dim2 = dim1[y];

                    if (dim2 == null) {
                        continue;
                    }
                    if (materialIndex < 0) {//count all
                        int sum = 0;
                        for (int t = 0; t < dim2.length; t++) {
                            for (int m = 0; m < dim2[t].length; m++) {
                                sum += dim2[t][m];
                            }
                            if (sum >= minimumCount) {
                                //Add this cell to outputgeometry
                                rectangles.add(gf.createPolygon(raster.getRectangleBoundClosed(x, y)));
                            }
                        }
                    } else {//count only material 
                        int sum = 0;
                        for (int t = 0; t < dim2.length; t++) {
                            sum += dim2[t][materialIndex];
                            if (sum >= minimumCount) {
                                //Add this cell to outputgeometry
                                rectangles.add(gf.createPolygon(raster.getRectangleBoundClosed(x, y)));
                            }
                        }
                    }
                }
            }
            //Build groups from single rectangles
            Geometry union2 = CascadedPolygonUnion.union(rectangles);

            if (union2 == null) {
                throw new NullPointerException("constructed CascadedPolygonUnion is null for materialindex " + materialIndex + "   rectangles: " + rectangles.size());
            }
            union2 = surface.getGeotools().toGlobal(union2, longitudeFirst);
//             System.out.println("ShapeTools: cascaded union with rectangleRaster created "+union2.getGeometryType()+" with "+union2.getNumGeometries()+" geometries.");
            if (union2 instanceof MultiPolygon) {
                MultiPolygon mp = (MultiPolygon) union2;
                ArrayList<Geometry> geolist = new ArrayList<>(mp.getNumGeometries());
                for (int i = 0; i < union2.getNumGeometries(); i++) {
                    Geometry g2 = union2.getGeometryN(i);
                    geolist.add(g2);
                }
                return geolist;
            } else if (union2 instanceof Polygon) {
                ArrayList<Geometry> geolist = new ArrayList<>(1);
                geolist.add(union2);
                return geolist;
            } else {

                throw new TransformException("constructed Geometry is " + union2.getGeometryType() + " with " + union2.getNumGeometries() + " geometries.");
            }

        } else if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {

            long starttime = System.currentTimeMillis();
//        LinkedList<SurfaceTriangle> contaminated = new LinkedList<>();
            LinkedList<Integer> contaminated = new LinkedList<>();
            SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();
            for (int m = 0; m < raster.getMeasurements().length; m++) {
                TriangleMeasurement measurement = raster.getMeasurements()[m];
                if (measurement == null || measurement.getParticlecount() == null || measurement.getParticlecount().length == 0) {
                    continue;
                }
                if (materialIndex >= 0) {
                    if (measurement.getParticlecount().length <= materialIndex) {
                        //index out of bounds
                        continue;
                    }
                    int[] particles = measurement.getParticlecount()[materialIndex];
                    int sum = 0;
                    for (int i = 0; i < particles.length; i++) {
                        sum += particles[i];
                        if (sum > minimumCount) {
                            break;
                        }
                    }
                    if (sum < minimumCount) {
                        continue;
                    }
                }

                contaminated.add(m);
            }
            ArrayList<LinkedList<Integer>> c = calcTriangleGroupContaminated(surface, contaminated, clusterCount, clusterloops);
            if (c == null || c.isEmpty()) {
                return null;
            }
//            System.out.println(c.size() + " groups of triangleclusters.");
            GeometryFactory gf = new GeometryFactory();
            ArrayList<Geometry> retur = new ArrayList<>(c.size());
            for (LinkedList<Integer> l : c) {
                ArrayList<Polygon> triangles = new ArrayList<>(l.size());

                for (Integer tri : l) {
                    int[] nodes = surface.getTriangleNodes()[(int) tri];
                    Coordinate[] coords = new Coordinate[4];
                    for (int j = 0; j < 3; j++) {
                        coords[j] = new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]);
                    }
                    coords[3] = coords[0];//Close ring
                    triangles.add((Polygon) gf.createPolygon(coords).buffer(0.0001, 1));
                }

                Geometry union = null;
                try {
                    union = CascadedPolygonUnion.union(triangles);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (union == null) {
//                    System.err.println("ShapeTools::createShapesWGS84: union of " + triangles.size() + " triangles is null.");
                } else {
//                    System.out.println((System.currentTimeMillis() - starttime) + "ms  Geometrytype: " + union.getGeometryType() + "   nGeometries:" + union.getNumGeometries() + "  triangles:" + l.size());

                    try {
                        retur.add(surface.getGeotools().toGlobal(union, longitudeFirst));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }

//            System.out.println("Centroid: " + union.getCentroid().getCoordinate() + "   -> " + surface.getGeotools().toGlobal(union.getCentroid().getCoordinate()));
            }
//            System.out.println("**************************");
//            System.out.println("    retur: "+retur.size());
            Geometry union2 = CascadedPolygonUnion.union(retur);
//            System.out.println("   union:"+union2);
            if (union2 instanceof MultiPolygon) {
                MultiPolygon mp = (MultiPolygon) union2;
                ArrayList<Geometry> geolist = new ArrayList<>(mp.getNumGeometries());
                for (int i = 0; i < union2.getNumGeometries(); i++) {
                    Geometry g2 = union2.getGeometryN(i);
                    geolist.add(g2);
//                if (g2 instanceof Polygon) {
//                    Polygon p2 = (Polygon) g2;
//                    geolist.add(p2.getExteriorRing());
//                } else {
//                    System.out.println("Type of GeometryN(" + i + ") is " + g2.getGeometryType());
//                    geolist.add(g2);
//                }
                }
//            System.out.println((System.currentTimeMillis() - starttime) + "ms until Multipolygon return");
                return geolist;
            }
//        System.out.println((System.currentTimeMillis() - starttime) + "ms until Geometry return");
            return retur;
        } else {
            throw new UnsupportedOperationException("Can not handly Raster Type " + surface.getMeasurementRaster().getClass() + " to build contamination shape.");
        }

    }

    /**
     *
     * @param surf
     * @param triangleIDs
     * @return
     */
    public static Geometry createPolygon(Surface surf, Collection<Integer> triangleIDs) {
        ArrayList<Geometry> triangles = new ArrayList<>(triangleIDs.size());
        GeometryFactory gf = new GeometryFactory();

        for (Integer id : triangleIDs) {
            int[] n = surf.getTriangleNodes()[id];
            Coordinate[] c = new Coordinate[4];
            for (int j = 0; j < 3; j++) {
                c[j] = new Coordinate(surf.getVerticesPosition()[n[j]][0], surf.getVerticesPosition()[n[j]][1]);//, surf.getVerticesPosition()[n[j]][2]);
            }
            c[3] = c[0];
            triangles.add(gf.createPolygon(c).buffer(0.0001, 1));
        }
//        return gf.buildGeometry(triangles);

        Geometry union = CascadedPolygonUnion.union(triangles);
        return union;
    }

    public static Geometry createMaxWaterLevelShapes(Surface surf, float[] maxWaterlevels, float minHeight) {
        LinkedList<Geometry> triangles = new LinkedList<>();
        GeometryFactory gf = new GeometryFactory();

        for (int i = 0; i < maxWaterlevels.length; i++) {
            float l = maxWaterlevels[i];

            if (l >= minHeight) {
                int[] n = surf.getTriangleNodes()[i];
                Coordinate[] c = new Coordinate[4];
                for (int j = 0; j < 3; j++) {
                    c[j] = new Coordinate(surf.getVerticesPosition()[n[j]][0], surf.getVerticesPosition()[n[j]][1]);
                }
                c[3] = c[0];
                triangles.add(gf.createPolygon(c));
            }
        }
        System.out.println("Found " + triangles.size() + " triangles >" + minHeight + "m.\n Union...");
        Geometry union = CascadedPolygonUnion.union(triangles);

        System.out.println("->" + union);
        return union;
    }

}
