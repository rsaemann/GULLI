package io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import control.StartParameters;
import control.maths.GeometryTools;
import io.extran.HE_InletReference;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.GeoTools;
import model.surface.Surface;
import model.surface.SurfaceLocalMinimumArea;
import model.surface.measurement.SurfaceMeasurementTriangleRaster;
import model.surface.SurfaceTriangle;
import model.surface.measurement.SurfaceMeasurementRaster;
import model.surface.measurement.TriangleMeasurement;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Position3D;
import model.topology.graph.Pair;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Load surface information of HYSTEM EXTRAN simulations
 *
 * @author saemann
 */
public class SurfaceIO {

    private static MultiPolygon testfilter = null;//initPolygon();

    public static boolean autoLoadNeumannNeighbours = false;

    public static MultiPolygon initPolygon() {
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] cs = new Coordinate[5];
        cs[0] = new Coordinate(546041, 5800896);
        cs[1] = cs[0];//new Coordinate(551126, 5800896);
        cs[2] = new Coordinate(551126, 5797948);
        cs[3] = new Coordinate(546041, 5797948);
        cs[4] = cs[0];
        Polygon polygon = gf.createPolygon(cs);
        return gf.createMultiPolygon(new Polygon[]{polygon});
    }

    public static Surface loadSurface(File directory) throws FileNotFoundException, IOException {
        if (!directory.isDirectory()) {
            throw new FileNotFoundException("Is not a directory to find Surface information: " + directory.getAbsolutePath());
        }
        File fileCoordinates = new File(directory.getAbsolutePath() + File.separator + "X.dat");
        if (!fileCoordinates.exists()) {
            throw new FileNotFoundException("File for Coordinates could not be found: " + fileCoordinates.getAbsolutePath());
        }
        File fileTriangle = new File(directory.getAbsolutePath() + File.separator + "TRIMOD2.dat");
        if (!fileTriangle.exists()) {
            throw new FileNotFoundException("File for Triangleindizes could not be found: " + fileTriangle.getAbsolutePath());
        }
        File fileNeighbours = new File(directory.getAbsolutePath() + File.separator + "TRIMOD1.dat");
        if (!fileNeighbours.exists()) {
            throw new FileNotFoundException("File for Neighbours could not be found: " + fileNeighbours.getAbsolutePath());
        }
        File fileCoordinateReference = new File(directory.getAbsolutePath() + File.separator + "polyg.xml");
        if (!fileCoordinateReference.exists()) {
            fileCoordinateReference = new File(directory.getAbsolutePath() + File.separator + "city.xml");
            if (!fileNeighbours.exists()) {
                throw new FileNotFoundException("File for CoordinateReference could not be found: " + fileCoordinateReference.getAbsolutePath());
            }
        }

        File fileStreetInlets = new File(directory.getAbsolutePath(), "SURF-SEWER_NODES.dat");
        if (!fileNeighbours.exists()) {
            System.err.println("File for Streetinlets could not be found: " + fileStreetInlets.getAbsolutePath());
        }

        File fileManhole2Surface = new File(directory.getAbsolutePath(), "SEWER-SURF_NODES.dat");
        if (!fileManhole2Surface.exists()) {
            System.err.println("File for Manhole position could not be found: " + fileManhole2Surface.getAbsolutePath());
        }

        Surface surf = loadSurface(fileCoordinates, fileTriangle, fileNeighbours, fileCoordinateReference);

        try {
            if (autoLoadNeumannNeighbours) {
                File neumannFile = new File(directory, "NEUMANN.dat");
                if (neumannFile.exists()) {
                    surf.setNeumannNeighbours(readMooreNeighbours(neumannFile));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (fileManhole2Surface != null) {
            ArrayList<Pair<String, Integer>> manholeRef = loadManholeToTriangleReferences(fileManhole2Surface);
            System.out.println("found " + manholeRef.size() + " manhole refs.");
        }
        if (fileStreetInlets != null) {
            ArrayList<Pair<String, Integer>> inletRef = loadStreetInletReferences(fileStreetInlets);
            System.out.println("found " + inletRef.size() + " inlet refs.");
        }

        return surf;
    }

    public static Surface loadSurface(File fileCoordinates, File fileTriangleIndizes, File fileNeighbours, File coordReferenceXML, MultiPolygon filter) throws FileNotFoundException, IOException {
        System.out.println("Filter surface loading with " + filter);
//Coordinates   //X.dat
        FileReader fr = new FileReader(fileCoordinates);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        String[] values;
        int numberofVertices = Integer.parseInt(line.split(" ")[0]);
        LinkedList<float[]> verticesL = new LinkedList<>();

        HashMap<Integer, Integer> verticesIndex = new HashMap<>();
//        float[][] vertices = new float[numberofVertices][3];
        int index = 0;
        int counter = 0;
        GeometryFactory gf = new GeometryFactory();
        Point p;
        while (br.ready()) {
            line = br.readLine();
            values = line.split(" ");
            float x = (Float.parseFloat(values[0]));
            float y = Float.parseFloat(values[1]);
            float ele = Float.parseFloat((values[values.length - 1]));
            p = gf.createPoint(new Coordinate(x, y));
            if (filter.contains(p)) {
                verticesL.add(new float[]{x, y, ele});
                verticesIndex.put(index, counter);
                counter++;
            }
            index++;
        }
        br.close();
        fr.close();

        //Load coordinate reference System
        String epsgCode = "EPSG:23632"; //Use this standard code for Hannover
        if (coordReferenceXML != null && coordReferenceXML.exists() && coordReferenceXML.canRead()) {
            epsgCode = loadSpatialReferenceCode(coordReferenceXML);
            if (epsgCode.equals("102329")) {
                epsgCode = "EPSG:4647";
                System.out.println("use EPSG:4647 instead of 102329");
//                for (float[] vertice : verticesL) {
//                    vertice[0] -= 32000000;
//                }
            } else {
                epsgCode = "EPSG:" + epsgCode;
            }
        }
        System.out.println(verticesL.size() + " Vertices to Linkedlist");
        float[][] vertices = new float[verticesL.size()][3];
        counter = 0;
        for (float[] c : verticesL) {
            vertices[counter] = c;
            counter++;
        }
        verticesL.clear();
        System.gc();
        //fileTriangleIndizes  //TRIMOD2.dat
        fr = new FileReader(fileTriangleIndizes);
        br = new BufferedReader(fr);
        line = br.readLine();
        int numberofTriangles = Integer.parseInt(line.split(" ")[0]);
        LinkedList<int[]> triangleIndizesL = new LinkedList<>();
        LinkedList<double[]> triangleMidPointsL = new LinkedList<>();
        HashMap<Integer, Integer> mapTriangleIndizes = new HashMap<>();//Maps the original index to the filtered index.

        index = -1;
        counter = 0;
        double oneThird = 1. / 3.;
        while (br.ready()) {
            index++;
            line = br.readLine();
            values = line.split(" ");
            int first = Integer.parseInt(values[0]);
            if (!verticesIndex.containsKey(first)) {
                //Node is not inside Filter, so do not use this triangle
                continue;
            }

            int second = Integer.parseInt(values[1]);
            if (!verticesIndex.containsKey(second)) {
                continue;
            }

            int third = Integer.parseInt(values[2]);
            if (!verticesIndex.containsKey(third)) {
                continue;
            }

            int[] indizes = new int[]{verticesIndex.get(first), verticesIndex.get(second), verticesIndex.get(third)};
            triangleIndizesL.add(indizes);

            if (first != indizes[0] || second != indizes[1] || third != indizes[2]) {
//                System.out.println("Triangleindizes not correct");
            }

//            triangleIndizes[index][0] = verticesIndex.get(first);
//            triangleIndizes[index][1] = verticesIndex.get(second);
//            triangleIndizes[index][2] = verticesIndex.get(third);
//            triangleMidPoints[index][0] = (vertices[first][0] + vertices[second][0] + vertices[third][0]) / 3.;
//            triangleMidPoints[index][1] = (vertices[first][1] + vertices[second][1] + vertices[third][1]) / 3.;
            double[] mids = new double[3];
            for (int i = 0; i < 3; i++) {
                mids[i] = (vertices[indizes[0]][i] * oneThird + vertices[indizes[1]][i] * oneThird + vertices[indizes[2]][i] * oneThird);
            }
            triangleMidPointsL.add(mids);

            mapTriangleIndizes.put(index, counter);

            counter++;
        }
        br.close();
        fr.close();
//        System.out.println(triangleIndizesL.size() + " triangleindizes to Linkedlist");
        int[][] triangleIndizes = new int[triangleIndizesL.size()][3];
        counter = 0;
        for (int[] ti : triangleIndizesL) {
            triangleIndizes[counter] = ti;
            counter++;
        }

        double[][] triangleMidPoints = new double[triangleMidPointsL.size()][3];
        counter = 0;
        for (double[] ti : triangleMidPointsL) {
            triangleMidPoints[counter] = ti;
            counter++;
        }
        triangleIndizesL.clear();
        triangleMidPointsL.clear();
        System.gc();

        //fileNeighbours
        fr = new FileReader(fileNeighbours);
        br = new BufferedReader(fr);

        LinkedList<int[]> neighboursL = new LinkedList<>();
        index = -1;
        while (br.ready()) {
            index++;
            line = br.readLine();
            //Only need to be parsed if this triangle lies inside the filter and has already been build
            if (!mapTriangleIndizes.containsKey(index)) {
                continue;
            }

            values = line.split(" ");
            int first = Integer.parseInt(values[0]);
            if (first < 0 || !mapTriangleIndizes.containsKey(first)) {
                first = -1;
            }
            int second = Integer.parseInt(values[1]);
            if (second < 0 || !mapTriangleIndizes.containsKey(second)) {
                second = -1;
            }
            int third = Integer.parseInt(values[2]);
            if (third < 0 || !mapTriangleIndizes.containsKey(third)) {
                third = -1;
            }
            int[] nb = new int[3];
            if (first < 0) {
                nb[0] = -1;
            } else {
                nb[0] = mapTriangleIndizes.get(first);
                if (first != nb[0]) {
//                    System.out.println("1.indizes from " + first + " to " + nb[0]);
                }
            }
            if (second < 0) {
                nb[1] = -1;
            } else {
                nb[1] = mapTriangleIndizes.get(second);
                if (second != nb[1]) {
//                    System.out.println("2.indizes from " + second + " to " + nb[1]);
                }
            }
            if (third < 0) {
                nb[2] = -1;
            } else {
                nb[2] = mapTriangleIndizes.get(third);
                if (third != nb[2]) {
//                    System.out.println("3.indizes from " + third + " to " + nb[2]);
                }
            }
//            neumannNeighbours[index][0] = first;
//            neumannNeighbours[index][1] = second;
//            neumannNeighbours[index][2] = third;
//
//            index++;
            neighboursL.add(nb);
        }
        br.close();
        fr.close();
//        System.out.println(neighboursL.size() + " Neighbours to Linkedlist");
        int[][] neighbours = new int[neighboursL.size()][3];
        counter = 0;
        for (int[] nb : neighboursL) {
            neighbours[counter] = nb;
            counter++;
        }

        Surface surf = new Surface(vertices, triangleIndizes, neighbours, mapTriangleIndizes, epsgCode);
        surf.setTriangleMids(triangleMidPoints);
        surf.fileTriangles = fileCoordinates.getParentFile();

//        System.out.println("Smallest: " + surf.calcSmallestTriangleArea() + "m²\t largest: " + surf.calcLargestTriangleArea() + "m²\t mean: " + surf.calcMeanTriangleArea() + "m²");
        return surf;
    }

    public static Surface loadSurface(File fileCoordinates, File fileTriangleIndizes, File fileNeighbours, File coordReferenceXML) throws FileNotFoundException, IOException {
        if (testfilter != null) {
            return loadSurface(fileCoordinates, fileTriangleIndizes, fileNeighbours, coordReferenceXML, testfilter);
        }

//        System.out.println("Load surface without filter");
//Coordinates   //X.dat
        FileReader fr = new FileReader(fileCoordinates);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        String[] values;
        int numberofVertices = Integer.parseInt(line.split(" ")[0]);
        float[][] vertices = new float[numberofVertices][3];
        int index = 0;
        while (br.ready()) {
            line = br.readLine();
            values = line.split(" ");
            float x = Float.parseFloat(values[0]);
            float y = Float.parseFloat(values[1]);
            float ele = Float.parseFloat((values[values.length - 1]));
            vertices[index][0] = x;
            vertices[index][1] = y;
            vertices[index][2] = ele;
            index++;
        }
        br.close();
        fr.close();

        //Load coordinate reference System
        String epsgCode = "EPSG:25832"; //Use this standard code for Hannover
        if (coordReferenceXML != null && coordReferenceXML.exists() && coordReferenceXML.canRead()) {
            epsgCode = loadSpatialReferenceCode(coordReferenceXML);
            if (epsgCode.equals("102329")) {
                epsgCode = "EPSG:4647";
                System.out.println("use EPSG:4647 instead of 102329");
//                for (float[] vertice : verticesL) {
//                    vertice[0] -= 32000000;
//                }
            } else {
                epsgCode = "EPSG:" + epsgCode;
            }
        }

        //fileTriangleIndizes  //TRIMOD2.dat
        fr = new FileReader(fileTriangleIndizes);
        br = new BufferedReader(fr);
        line = br.readLine();
        int numberofTriangles = Integer.parseInt(line.split(" ")[0]);
        int[][] triangleIndizes = new int[numberofTriangles][3];
        double[][] triangleMidPoints = new double[numberofTriangles][3];
        index = 0;
        double oneThird = 1. / 3.;
        while (br.ready()) {
            line = br.readLine();
            values = line.split(" ");
            int first = Integer.parseInt(values[0]);
            int second = Integer.parseInt(values[1]);
            int third = Integer.parseInt(values[2]);

            triangleIndizes[index][0] = first;
            triangleIndizes[index][1] = second;
            triangleIndizes[index][2] = third;

            triangleMidPoints[index][0] = (vertices[first][0] * oneThird + vertices[second][0] * oneThird + vertices[third][0] * oneThird);
            triangleMidPoints[index][1] = (vertices[first][1] * oneThird + vertices[second][1] * oneThird + vertices[third][1] * oneThird);
            triangleMidPoints[index][2] = (vertices[first][2] * oneThird + vertices[second][2] * oneThird + vertices[third][2] * oneThird);

            index++;
        }
        br.close();
        fr.close();

        //fileNeighbours
        fr = new FileReader(fileNeighbours);
        br = new BufferedReader(fr);
        int[][] neighbours = new int[numberofTriangles][3];
        index = 0;
        while (br.ready()) {
            line = br.readLine();
            values = line.split(" ");
            int first = Integer.parseInt(values[0]);
            int second = Integer.parseInt(values[1]);
            int third = Integer.parseInt(values[2]);

            neighbours[index][0] = first;
            neighbours[index][1] = second;
            neighbours[index][2] = third;

            index++;
        }
        br.close();
        fr.close();

        Surface surf = new Surface(vertices, triangleIndizes, neighbours, null, epsgCode);
        surf.setTriangleMids(triangleMidPoints);
        surf.fileTriangles = fileCoordinates.getParentFile();
//        System.out.println("Smallest: " + surf.calcSmallestTriangleArea() + "m²\t largest: " + surf.calcLargestTriangleArea() + "m²\t mean: " + surf.calcMeanTriangleArea() + "m²");
        return surf;
    }

   

    /**
     * Load spatial reference code (e.g. "4326" for WGS84) as defined in polygon
     * generation xml (polyg.xml)
     *
     * @param xmlfile
     * @return
     */
    public static String loadSpatialReferenceCode(File xmlfile) {
        String code = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(xmlfile));
            String line;
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("<spatial_reference_code>")) {
                    line = line.substring(line.indexOf(">") + 1);
                    line = line.substring(0, line.indexOf("<"));
                    code = line;
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return code;
    }

    /**
     * Load spatial reference Name (e.g. "ETRS_1989_UTM_Zone_32N_8stellen" ) as
     * defined in polygon generation xml (polyg.xml)
     *
     * @param xmlfile
     * @return
     */
    public static String loadSpatialReferenceName(File xmlfile) {
        String code = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(xmlfile));
            String line;
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("<spatial_reference_name>")) {
                    line = line.substring(line.indexOf(">") + 1);
                    line = line.substring(0, line.indexOf("<"));
                    code = line;
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return code;
    }

    public static int readNumberOfTriangles(File trimod2File) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(trimod2File);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        int numberOftriangles = Integer.parseInt(line.split(" ")[0]);

        br.close();
        fr.close();
        return numberOftriangles;
    }

    /**
     * @param nw
     * @param surface
     * @throws Exception
     */
    public static void mapManholes(Network nw, Surface surface) throws Exception {
        long starttime = System.currentTimeMillis();
        GeoTools geotools = surface.getGeotools();
        Coordinate[] m = new Coordinate[nw.getManholes().size()];
        long start = System.currentTimeMillis();

        CRSAuthorityFactory af = CRS.getAuthorityFactory(StartParameters.JTS_WGS84_LONGITUDE_FIRST);

        CoordinateReferenceSystem crsWGS84 = af.createCoordinateReferenceSystem("EPSG:4326");
        CoordinateReferenceSystem crsUTM = af.createCoordinateReferenceSystem("EPSG:25832");

        MathTransform transform = CRS.findMathTransform(crsWGS84, crsUTM);
        MathTransform transformBack = CRS.findMathTransform(crsUTM, crsWGS84);
//        MathTransform transform2UTM = CRS.findMathTransform(Network.crsWGS84, nw.crsUTM);
        int index = 0;
        for (Manhole manhole : nw.getManholes()) {
            Coordinate cll = manhole.getPosition().latLonCoordinate();
            //switch coordinates
            m[index] = geotools.toUTM(manhole.getPosition());
            index++;
        }
        index = 0;

        surface.clearTriangleCapacities();

        //Manholes & inlets to array for faster access
        Manhole[] manholes = nw.getManholes().toArray(new Manhole[nw.getManholes().size()]);

        int counterManholes = 0;//, counterInlets = 0;

//        starttime = System.currentTimeMillis();
        for (int i = 0; i < surface.triangleNodes.length; i++) {
//            poly = null;
            Coordinate c0 = new Coordinate(surface.vertices[surface.triangleNodes[i][0]][0], surface.vertices[surface.triangleNodes[i][0]][1]);
            Coordinate c1 = new Coordinate(surface.vertices[surface.triangleNodes[i][1]][0], surface.vertices[surface.triangleNodes[i][1]][1]);
            Coordinate c2 = new Coordinate(surface.vertices[surface.triangleNodes[i][2]][0], surface.vertices[surface.triangleNodes[i][2]][1]);

            for (int j = 0; j < m.length; j++) {
                Coordinate m1 = m[j];
//                Point p1 = null;
                if (Math.abs(c0.x - m1.x) < 30) {
                    if (Math.abs(c0.y - m1.y) < 30) {
                        if (c0.distance(m1) < 30) {
                            //Closer look
                            boolean contains = GeometryTools.triangleContainsPoint(c0.x, c1.x, c2.x, c0.y, c1.y, c2.y, m1.x, m1.y);
                            if (contains) {
                                SurfaceTriangle tri = surface.triangleCapacitys[i];
                                if (tri == null) {
                                    tri = new SurfaceTriangle(i);
                                    surface.triangleCapacitys[i] = tri;
                                    Coordinate c = new Coordinate(surface.getTriangleMids()[i][0], surface.getTriangleMids()[i][1]);
                                    try {
                                        Coordinate cll = geotools.toGlobal(c);
//                                    System.out.println("x:"+cll.x+" y:"+cll.y);
                                        tri.setPosition(new Position3D(cll.x, cll.y, c.x, c.y, surface.getTriangleMids()[i][2]));

                                    } catch (TransformException ex) {
                                        Logger.getLogger(Surface.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                Manhole mh = manholes[j];
                                tri.manhole = mh;
                                mh.setSurfaceTriangle(tri);
                                counterManholes++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("found manholes and inlets after " + (System.currentTimeMillis() - start) / 1000 + "s. Manholes:" + counterManholes + "/" + nw.getManholes().size());
    }

    public static void mapStreetInlets(Network nw, Surface surface) throws TransformException, FactoryException {
        int index = 0;
        Coordinate[] in;
        if (nw.getStreetInlets() != null) {
            in = new Coordinate[nw.getStreetInlets().size()];
            for (Inlet inlet : nw.getStreetInlets()) {
                in[index] = surface.getGeotools().toUTM(inlet.getPosition().latLonCoordinate());
                index++;
            }
        } else {
            in = new Coordinate[0];
            System.err.println("No inlets found in Network.");
            return;
        }
        index = 0;
        int counterManholes = 0, counterInlets = 0;
        Inlet[] inlets = new Inlet[0];
        if (nw.getStreetInlets() != null) {
            inlets = nw.getStreetInlets().toArray(new Inlet[nw.getStreetInlets().size()]);
        }

        for (int i = 0; i < surface.triangleNodes.length; i++) {
            Coordinate c0 = new Coordinate(surface.vertices[surface.triangleNodes[i][0]][0], surface.vertices[surface.triangleNodes[i][0]][1]);
            Coordinate c1 = new Coordinate(surface.vertices[surface.triangleNodes[i][1]][0], surface.vertices[surface.triangleNodes[i][1]][1]);
            Coordinate c2 = new Coordinate(surface.vertices[surface.triangleNodes[i][2]][0], surface.vertices[surface.triangleNodes[i][2]][1]);

            for (int j = 0; j < in.length; j++) {
                Coordinate m1 = in[j];
                Point p1 = null;
                if (Math.abs(c0.x - m1.x) < 20) {
                    if (Math.abs(c0.y - m1.y) < 20) {
                        if (c0.distance(m1) < 20) {
                            //Closer look

                            boolean contains = GeometryTools.triangleContainsPoint(c0.x, c1.x, c2.x, c0.y, c1.y, c2.y, m1.x, m1.y);
                            if (contains) {
                                SurfaceTriangle tri = surface.triangleCapacitys[i];
                                if (tri == null) {
                                    tri = new SurfaceTriangle(i);
                                    surface.triangleCapacitys[i] = tri;
                                    Coordinate c = new Coordinate(surface.getTriangleMids()[i][0], surface.getTriangleMids()[i][1]);
                                    try {
                                        Coordinate cll = surface.getGeotools().toGlobal(c);
                                        tri.setPosition(new Position3D(cll.x, cll.y, c.x, c.y, surface.getTriangleMids()[i][2]));

                                    } catch (TransformException ex) {
                                        Logger.getLogger(Surface.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                Inlet mh = inlets[j];
                                tri.inlet = mh;
                                counterInlets++;
                                if (true) {
                                    //Neighbour Triangles also get a reference to this inlet
                                    if (surface.neumannNeighbours != null) {
                                        for (int k = 0; k < 3; k++) {
                                            int nbi = surface.neumannNeighbours[i][k];
                                            if (nbi < 0) {
                                                continue;
                                            }
                                            tri = surface.triangleCapacitys[nbi];
                                            if (tri == null) {
                                                tri = new SurfaceTriangle(nbi);
                                                surface.triangleCapacitys[nbi] = tri;
                                                Coordinate c = new Coordinate(surface.getTriangleMids()[nbi][0], surface.getTriangleMids()[nbi][1]);
                                                try {
                                                    Coordinate cll = surface.getGeotools().toGlobal(c);
                                                    tri.setPosition(new Position3D(cll.x, cll.y, c.x, c.y, surface.getTriangleMids()[nbi][2]));

                                                } catch (TransformException ex) {
                                                    Logger.getLogger(Surface.class
                                                            .getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }
                                            tri.inlet = inlets[j];

                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void writeSurfaceContaminationCSV(File outputFile, Surface surface) throws IOException {
        if (surface == null) {
            throw new NullPointerException("No Surface set. No output file written.");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if (surface.fileTriangles != null) {
                bw.write("Surface:" + surface.fileTriangles.getParentFile().getName() + "/" + surface.fileTriangles.getName());
                bw.newLine();
            }
            bw.write("Reduced Net:" + (surface.mapIndizes != null && !surface.mapIndizes.isEmpty()));
            bw.newLine();
            bw.write("Max Contaminated Triangles:" + surface.triangleCapacitys.length);
            bw.newLine();
            int categories = surface.getNumberOfMaterials();

            bw.write("Contaminant categories:" + categories);
            bw.newLine();
            bw.write("***");
            bw.newLine();
            if (categories < 1) {
                return;
            }
            if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();
                for (int mID = 0; mID < raster.getMeasurements().length; mID++) {
                    TriangleMeasurement measurement = raster.getMeasurements()[mID];
                    if (measurement != null && measurement.getParticlecount() != null && measurement.getParticlecount().length > 0) {
                        bw.write(mID + "");
                        int timesteps = measurement.getTimes().getNumberOfTimes();
                        for (int i = 0; i < categories; i++) {
                            int sum = 0;
                            for (int j = 0; j < timesteps; j++) {
                                sum += measurement.getParticlecount()[i][j];
                            }
                            bw.write(";" + sum);
                        }
                        bw.newLine();
                    }
                }
            } else {
                throw new UnsupportedOperationException("Type of Surface Raster " + surface.getMeasurementRaster().getClass().getSimpleName() + " is not known to be handled for output.");
            }

        }
    }

    public static ArrayList<Integer>[] findNodesTriangleIDs(int[][] triangleNotes, int vertexcount) {
        ArrayList<Integer>[] nodeVertices = new ArrayList[vertexcount];
        int zehntel = triangleNotes.length / 10;
        for (int i = 0; i < triangleNotes.length; i++) {
            for (int j = 0; j < 3; j++) {
                int vID = triangleNotes[i][j];
                ArrayList<Integer> list = nodeVertices[vID];
                if (list != null) {
                    list.add(i);
                } else {
                    ArrayList<Integer> l = new ArrayList<>(6);
                    l.add(i);
                    nodeVertices[vID] = l;
                }
            }
            if (i % zehntel == 0) {
                System.out.println((i * 100) / triangleNotes.length + " %");
            }
        }
        return nodeVertices;
    }

    public static int[][] findMooreTriangleNeighbours(int[][] triangleNotes, ArrayList<Integer>[] nodeVertices) {

        int zehntel = triangleNotes.length / 10;
        int count0 = 0;
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < nodeVertices.length; i++) {
            ArrayList<Integer> l = nodeVertices[i];
            if (l != null) {
                if (l.size() < 2) {
                    count1++;
                }
            } else {
                count0++;
            }
        }
        System.out.println(count0 + " vertices are not part of a triangle, additional " + count1 + " are only part of 1 triangle.");

        // find per triangle all triangles on its vertices
        int[][] neumann = new int[triangleNotes.length][];
        ArrayList<Integer> triangleIDs;
        for (int i = 0; i < triangleNotes.length; i++) {
            triangleIDs = new ArrayList<>(20);
            for (int j = 0; j < 3; j++) {
                int vertexID = triangleNotes[i][j];
                ArrayList<Integer> l = nodeVertices[vertexID];
                if (l != null) {
                    triangleIDs.addAll(l);
                }
            }
            //Erase duplicate entries
            Collections.sort(triangleIDs);
            int lastID = -1;
            Iterator<Integer> it = triangleIDs.iterator();
            while (it.hasNext()) {
                int n = it.next();
                if (n == lastID) {
                    it.remove();
                } else if (n == i) {
                    it.remove();
                }
                lastID = n;
            }
            int[] list = new int[triangleIDs.size()];
            int index = 0;
            for (Integer triangleID : triangleIDs) {
                list[index] = triangleID;
                index++;
            }
            neumann[i] = list;
        }
        return neumann;
    }

    public static void writeMooreTriangleNeighbours(int[][] triangleNotes, int vertexcount, File outputfile) throws IOException {
        ArrayList<Integer>[] node2Tri = findNodesTriangleIDs(triangleNotes, vertexcount);
        int[][] n = findMooreTriangleNeighbours(triangleNotes, node2Tri);
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
        bw.write(n.length + "");
        bw.newLine();
        for (int i = 0; i < n.length; i++) {
            bw.append("" + i + ",");
            if (n[i] != null) {
                for (int j = 0; j < n[i].length; j++) {
                    bw.append(" " + n[i][j]);

                }
            }
            bw.newLine();
            bw.flush();
        }
    }

    public static int[][] readMooreNeighbours(File file) throws FileNotFoundException, IOException {
        int[][] neumann = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = "";
            line = br.readLine();
            int numberOfTriangles = Integer.parseInt(line);
            neumann = new int[numberOfTriangles][];
            String[] split;
            while (br.ready()) {
                try {
                    line = br.readLine();
                    split = line.split(",");
                    if (split.length < 2) {
                        continue;
                    }
                    int id = Integer.parseInt(split[0]);
                    split = split[1].trim().split(" ");
                    int[] entries = new int[split.length];
                    neumann[id] = entries;
                    for (int i = 0; i < split.length; i++) {
                        if (split[i].isEmpty()) {
                            System.err.println("can not read line '" + line + "' i=" + i);
                        } else {
                            entries[i] = Integer.parseInt(split[i]);
                        }
                    }
                } catch (Exception exception) {
                    System.err.println("Problem in Moore line '" + line);
                    exception.printStackTrace();
                }
            }
        }
        return neumann;
    }

    public static void writeNodesTraingleIDs(ArrayList<Integer>[] nodesTraingles, File outputfile) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new BufferedWriter(new FileWriter(outputfile)))) {
            bw.write("Nodes:" + nodesTraingles.length);
            bw.newLine();
            bw.write("Node;#Triangles;TriangleIDs...");
            bw.newLine();
            for (int i = 0; i < nodesTraingles.length; i++) {
                ArrayList<Integer> tris = nodesTraingles[i];
                if (tris == null) {
                    continue;
                }
                bw.write(i + ";" + tris.size());
                for (Integer tri : tris) {
                    bw.write(";" + tri);
                }
                bw.newLine();
                bw.flush();
            }
            bw.flush();
        }
    }

    /**
     * Load the allocation of nodes. A node is part of which triangles from
     * file.
     *
     * @param file
     * @return [nodeID][#assigned triangles] = triangleID
     * @throws FileNotFoundException
     */
    public static int[][] loadNodesTriangleIDs(File file) throws FileNotFoundException, IOException {
        int[][] node2TriangleIDs;
        //Read number of nodes to get the size of array
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            //Read number of nodes to get the size of array
            String line = br.readLine();
            String[] str = line.split(":");
            int size = Integer.parseInt(str[1]);
            node2TriangleIDs = new int[size][];
            //Skip header
            br.readLine();
            //Read content
            while (br.ready()) {
                line = br.readLine();
                str = line.split(";");
                int nodeID = Integer.parseInt(str[0]);
                //Length=number of triangle IDs assigned to this Node.
                int length = Integer.parseInt(str[1]);
                node2TriangleIDs[nodeID] = new int[length];
                for (int i = 0; i < length; i++) {
                    node2TriangleIDs[nodeID][i] = Integer.parseInt(str[i + 2]);
                }
            }
        }
        return node2TriangleIDs;
    }

    public static int[] findlocalMinimumPointsNeumann(double[][] triangleMids, int[][] neumannNeighbours) {
        LinkedList<Integer> localminimum = new LinkedList<>();
        for (int i = 0; i < neumannNeighbours.length; i++) {
            double tZ = triangleMids[i][2];
            boolean foundDeeper = false;
            for (int j = 0; j < neumannNeighbours[i].length; j++) {
                int id = neumannNeighbours[i][j];
//                System.out.println("zref=" + tZ + "  found: " + triangleMids[id][2]);
                if (triangleMids[id][2] < tZ) {
                    foundDeeper = true;
                    break;
                }
            }
            if (foundDeeper) {
                continue;
            }
            localminimum.add(i);
        }
        int[] retur = new int[localminimum.size()];
        int index = 0;
        for (Integer id : localminimum) {
            retur[index] = id;
            index++;
        }
        return retur;
    }

    public static int[] findlocalMinimumPointsMoore(double[][] triangleMids, int[][] mooreNeighbours) {
        LinkedList<Integer> localminimum = new LinkedList<>();
        for (int i = 0; i < mooreNeighbours.length; i++) {
            double tZ = triangleMids[i][2];
            boolean foundDeeper = false;
            for (int j = 0; j < mooreNeighbours[i].length; j++) {
                int id = mooreNeighbours[i][j];
                if (id < 0) {
                    continue;
                }
                if (triangleMids[id][2] < tZ) {
                    foundDeeper = true;
                    break;
                }
            }
            if (foundDeeper) {
                continue;
            }
            localminimum.add(i);
        }
        int[] retur = new int[localminimum.size()];
        int index = 0;
        for (Integer id : localminimum) {
            retur[index] = id;
            index++;
        }
        return retur;
    }

    /**
     *
     * @param fileManhole2Surface
     * @return List<PipeName,SurfaceTriangleID>
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<Pair<String, Integer>> loadStreetInletReferences(File fileManhole2Surface) throws FileNotFoundException, IOException {
        ArrayList<Pair<String, Integer>> streetInlets;
        try (BufferedReader br = new BufferedReader(new FileReader(fileManhole2Surface))) {
            String line;
            line = br.readLine();
            String numberOfManholesStr = line.replaceAll("[^0-9.,]+", "").trim();
            int numberOfStreetInlets = 10;
            try {
                numberOfStreetInlets = Integer.parseInt(numberOfManholesStr);
            } catch (Exception exception) {
            }
            streetInlets = new ArrayList<>(numberOfStreetInlets);
            while (br.ready()) {
                line = br.readLine();
                String[] seperated = line.split("%");
                String[] values = seperated[2].replaceAll("  ", " ").split(" ");

                Pair<String, Integer> pair = new Pair<>(values[1], Integer.parseInt(values[3]));
                streetInlets.add(pair);
            }
        }
        return streetInlets;
    }

    /**
     *
     * @param fileManhole2Surface
     * @return List<PipeName,SurfaceTriangleID>
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static ArrayList<HE_InletReference> loadStreetInletsReferences(File fileManhole2Surface) throws FileNotFoundException, IOException {
        ArrayList<HE_InletReference> streetInlets;
        try (BufferedReader br = new BufferedReader(new FileReader(fileManhole2Surface))) {
            String line;
            line = br.readLine();
            String numberOfManholesStr = line.replaceAll("[^0-9.,]+", "").trim();
            int numberOfStreetInlets = 10;
            try {
                numberOfStreetInlets = Integer.parseInt(numberOfManholesStr);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            streetInlets = new ArrayList<>(numberOfStreetInlets);
            int lineindex = 1;
            while (br.ready()) {
                line = br.readLine();
                String[] seperated = line.split("%");
                // String[] values = seperated[2].replaceAll("  ", " ").split(" ");
                // name and coordinates
                String[] lc = seperated[0].replaceAll("  ", " ").split(" ");

                String inletName = lc[0];
                double x = 0;
                double y = 0;
                float ele = 0;
                try {
                    x = Double.parseDouble(lc[1]);
                    y = Double.parseDouble(lc[2]);

                    //Height / z / elevation
                    ele = Float.parseFloat(seperated[1].split(" ")[1]);
                } catch (NumberFormatException numberFormatException) {
                    numberFormatException.printStackTrace();
                }

                //Capacity
                String part3 = seperated[2].trim();
                String capacity = part3.split(" 1")[0];
                //Traingle 
                int triangleID = Integer.parseInt(part3.split(" 1 ")[1].split("  ")[0]);
                HE_InletReference ref = new HE_InletReference(inletName, x, y, lineindex, ele, capacity, triangleID);
                streetInlets.add(ref);
            }
        }
        return streetInlets;
    }

    public static ArrayList<Pair<String, Integer>> loadManholeToTriangleReferences(File fileManhole2Surface) throws FileNotFoundException, IOException {
        ArrayList<Pair<String, Integer>> manhole2Triangle;
        try (BufferedReader br = new BufferedReader(new FileReader(fileManhole2Surface))) {
            String line;
            line = br.readLine();
            String numberOfManholesStr = line.replaceAll("[^0-9.,]+", "").trim();
            int numberOfManholes = 10;
            try {
                numberOfManholes = Integer.parseInt(numberOfManholesStr);
            } catch (Exception exception) {
            }
            manhole2Triangle = new ArrayList<>(numberOfManholes);
            while (br.ready()) {
                line = br.readLine();
                String[] seperated = line.split("%");
                String[] values = seperated[2].replaceAll("  ", " ").split(" ");

                Pair<String, Integer> pair = new Pair<>(seperated[0].split(" ")[0], Integer.parseInt(values[2]));
                manhole2Triangle.add(pair);
            }
        }
        return manhole2Triangle;
    }

    public static void referenceManholes(Collection<Pair<String, Integer>> references, Network network, Surface surface) {
        for (Pair<String, Integer> ref : references) {
            try {
                Manhole mh = network.getManholeByName(ref.first);
                if (mh == null) {
                    System.err.println(SurfaceIO.class + "::could not find Manhole " + ref.first + " to connect to surface.");
                    continue;
                }
                SurfaceTriangle tri = surface.requestSurfaceTriangle(ref.second);
                if (tri == null) {
                    System.err.println(SurfaceIO.class + "::could not find Triangle " + ref.second + " to connect to Manhole.");
                    continue;
                }
                tri.manhole = mh;
                mh.setSurfaceTriangle(tri);
            } catch (NullPointerException nullPointerException) {
                nullPointerException.printStackTrace();
            }
        }
    }

//    public static void referenceInlets(Collection<Pair<String, Integer>> references, Network network, Surface surface) {
//        ArrayList<Inlet> inlets=new ArrayList<>(references.size());
//        for (Pair<String, Integer> ref : references) {
//            Pipe p = network.getPipeByName(ref.first);
//            if (p == null) {
//                System.err.println(SurfaceIO.class + "::could not find Pipe " + ref.first + " to connect to surfaceinlet.");
//                continue;
//            }
//            SurfaceTriangle tri = surface.requestSurfaceTriangle(ref.second);
//            if (tri == null) {
//                System.err.println(SurfaceIO.class + "::could not find Triangle " + ref.second + " to connect inlet to Pipe.");
//                continue;
//            }
//            Inlet inlet = new Inlet(tri.getPosition3D(0), p, 0.5f * p.getLength());
//            tri.inlet = inlet;
//            network.setCapacities(null);
//            inlets.add(inlet);
//        }
//        network.setStreetInlets(inlets);
//        surface.
//    }
    public static void writeRasterContaminationCSV(SurfaceMeasurementRaster raster, File outFile) throws IOException, TransformException {
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(raster.getClass().getSimpleName());
        bw.newLine();
        DecimalFormat dfLong = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfShort = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));
        if (raster instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementTriangleRaster tr = (SurfaceMeasurementTriangleRaster) raster;
            bw.write("measurements:" + tr.getMeasurements().length);
            bw.newLine();
            //Count contaminations
            int cont = 0;
            for (TriangleMeasurement measurement : tr.getMeasurements()) {
                if (measurement == null) {
                    continue;
                }
                cont++;
            }
            bw.write("Contaminations:" + cont);
            bw.newLine();
            bw.write("EPSG:4326; Latitude, Longitude, Elevation");
            bw.newLine();
            bw.write("CELLID;mass,");
            bw.newLine();
            double[] mass = new double[tr.getNumberOfMaterials()];
            for (int i = 0; i < tr.getMeasurements().length; i++) {
                TriangleMeasurement m = tr.getMeasurements()[i];
                if (m == null) {
                    continue;
                }
                bw.write(i + ";");
                for (int j = 0; j < mass.length; j++) {
                    mass[j] = 0;
                    for (int t = 0; t < tr.getIndexContainer().getNumberOfTimes(); t++) {
                        mass[j] += m.getMass()[j][t];
                    }
                    bw.write(dfLong.format(mass[j]) + ",");
                }
                bw.newLine();
                bw.flush();
            }
        } else {
            System.err.println("ContaminationRaster of type " + raster.getClass().getSimpleName() + " can not be exported yet.");
        }
        bw.flush();
        bw.close();
        fw.close();
    }

    public static void writeRasterGeometriesCSV(SurfaceMeasurementRaster raster, File outFile) throws IOException, TransformException {
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(raster.getClass().getSimpleName());
        bw.newLine();
        DecimalFormat dfLong = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat dfShort = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));
        if (raster instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementTriangleRaster tr = (SurfaceMeasurementTriangleRaster) raster;
            bw.write("measurements:" + tr.getMeasurements().length);
            bw.newLine();
            bw.write("EPSG:4326; Latitude, Longitude, Elevation");
            bw.newLine();
            bw.write("CELLID;x0,y0,z0;x1,y1,z1;x2,y2,z2;midX,midY,midZ");
            bw.newLine();
            Surface s = tr.getSurface();
            for (int i = 0; i < s.triangleNodes.length; i++) {
                bw.write(i + ";");
                for (int j = 0; j < 3; j++) {
                    float[] v0 = s.vertices[s.triangleNodes[i][j]];
                    Coordinate c = new Coordinate(v0[0], v0[1]);
                    Coordinate latlon = s.getGeotools().toGlobal(c, false);
                    bw.write(dfLong.format(latlon.x) + "," + dfLong.format(latlon.y) + "," + dfShort.format(v0[2]) + ";");
                }
                double[] v0 = s.getTriangleMids()[i];
                Coordinate c = new Coordinate(v0[0], v0[1]);
                Coordinate latlon = s.getGeotools().toGlobal(c, false);
                bw.write(dfLong.format(latlon.x) + "," + dfLong.format(latlon.y) + "," + dfShort.format(v0[2]) + ";");
                bw.newLine();
                bw.flush();
            }
        }
        bw.flush();
        bw.close();
        fw.close();

    }

    public static void mergeContaminationFilesParticleCount(File[] tomerge, File outputFile) throws FileNotFoundException, IOException {
        int numberOfsurfaceCells = -1;
        int materials = -1;
        /**
         * [cellid][0:allsum, 1...* materialsum]
         */
        int[][] counter = null;

        for (File f : tomerge) {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("Max")) {
                    int maxContaminatedTriangles = Integer.parseInt(line.substring(line.indexOf(":") + 1));
                    if (numberOfsurfaceCells < 0) {
                        //This is the first file. use this as reference
                        numberOfsurfaceCells = maxContaminatedTriangles;
                    } else {
                        //Tst if this file has the same reference raster
                        if (numberOfsurfaceCells != maxContaminatedTriangles) {
                            System.err.println("File " + f + " has another number of cells (" + maxContaminatedTriangles + ") than the other files " + numberOfsurfaceCells);
                            break;
                        }
                    }

                } else if (line.startsWith("Contaminant")) {
                    int mat = Integer.parseInt(line.substring(line.indexOf(":") + 1));
                    if (materials < 0) {
                        //This is the first file. use this as reference
                        materials = mat;
                    } else {
                        //Tst if this file has the same reference raster
                        if (materials != mat) {
                            System.err.println("File " + f + " has another number of materials (" + mat + ") than the other files " + materials);
                            break;
                        }
                    }
                } else if (line.startsWith("***")) {
                    //Last header line
                    break;
                }
            }
            //Prepare counter array
            if (counter == null) {
                counter = new int[numberOfsurfaceCells][materials + 1];
            }

            //Start reading values
            String[] splits;
            while (br.ready()) {
                line = br.readLine();
                splits = line.split(";");
                int id = Integer.parseInt(splits[0]);
                for (int i = 1; i < splits.length; i++) {
                    int number = Integer.parseInt(splits[i]);
                    counter[id][i] += number;
                    counter[id][0] += number;
                }
            }
            br.close();
            fr.close();
        }

        //Write to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            bw.write("Max Contaminated Triangles:" + numberOfsurfaceCells);
            bw.newLine();
            bw.write("Contaminant categories:" + materials);
            bw.newLine();
            bw.write("***");
            bw.newLine();
            if (materials < 1) {
                return;
            }
            for (int i = 0; i < counter.length; i++) {
                if (counter[i][0] < 1) {
                    continue;
                }
                bw.write(i + "");

                for (int m = 1; m <= materials; m++) {

                    bw.write(";" + counter[i][m]);
                }
                bw.newLine();
            }
        }
    }
}
