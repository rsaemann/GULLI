package com.saemann.gulli.core.io.extran;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.LoadingCoordinator;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.maths.GeometryTools;
import com.saemann.gulli.core.io.NumberConverter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.core.model.topology.Inlet;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.graph.Pair;
import java.util.AbstractMap;
import java.util.Map;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Load surface information of HYSTEM EXTRAN simulations
 *
 * @author saemann
 */
public class HE_SurfaceIO {

    /**
     * @deprecated A polygon filter. If it is set, only parts of the surfac,e
     * that lay inside the polygon will be loaded. For Debug processes!
     */
    private static MultiPolygon testfilter = null;//initPolygon();

//    /**
//     * Read MOORE Neighbours from MOORE_Neighbours.dat file if exists when
//     * loading the Surface definition. default=false.
//     */
//    public static boolean autoLoadMooreNeighbours = false;
    /**
     * Object, which indicates the current status of loading. This can be set by
     * the GUI to receive information of the current loading state.
     */
    public static Action loadingAction = null;

    /**
     * Reorder the array of vertices, if there are unused ones. Exclude them and
     * set new indices in the triangle reference. This is performed in the
     * loadSurface step.
     */
    public static boolean condenseUnusedVertices = true;

    public static boolean calculateSlopes = true;

//    public static MultiPolygon initPolygon() {
//        GeometryFactory gf = new GeometryFactory();
//        Coordinate[] cs = new Coordinate[5];
//        cs[0] = new Coordinate(546041, 5800896);
//        cs[1] = cs[0];//new Coordinate(551126, 5800896);
//        cs[2] = new Coordinate(551126, 5797948);
//        cs[3] = new Coordinate(546041, 5797948);
//        cs[4] = cs[0];
//        Polygon polygon = gf.createPolygon(cs);
//        return gf.createMultiPolygon(new Polygon[]{polygon});
//    }
    /**
     * Create the complete surface topography and topology (neighbour
     * references) The required files will be searched from the given directory.
     * The directory should include the X.dat, trimod1.dat and trimad2.dat
     *
     * @param directory
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Surface loadSurface(File directory) throws FileNotFoundException, IOException {
        if (!directory.isDirectory()) {
            if (directory.isFile()) {
                directory = directory.getParentFile();
            } else {
                throw new FileNotFoundException("Is not a directory to find Surface information: " + directory.getAbsolutePath());
            }
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
            fileManhole2Surface = null;
        }

        Surface surf = loadSurface(fileCoordinates, fileTriangle, fileNeighbours, fileCoordinateReference);

//        try {
//            if (autoLoadMooreNeighbours) {
//                File neumannFile = new File(directory, "MOORE_NEIGHBOURS.dat");
//                if (neumannFile.exists()) {
//                    surf.mooreNeighbours=(readMooreNeighbours(neumannFile));
//                }
//            }
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
        if (fileManhole2Surface != null && fileManhole2Surface.exists()) {
            ArrayList<Pair<String, Integer>> manholeRef = loadManholeToTriangleReferences(fileManhole2Surface);
            System.out.println("found " + manholeRef.size() + " manhole refs.");
        }
        if (fileStreetInlets != null && fileStreetInlets.exists()) {
            ArrayList<Pair<String, Integer>> inletRef = loadStreetInletReferences(fileStreetInlets);
            System.out.println("found " + inletRef.size() + " inlet refs.");
        }

        return surf;
    }

    public static Surface loadSurfaceFiltered(File fileCoordinates, File fileTriangleIndizes, File fileNeighbours, File coordReferenceXML, MultiPolygon filter) throws FileNotFoundException, IOException {
        System.out.println("Filter surface loading with " + filter);
//Coordinates   //X.dat
        FileReader fr = new FileReader(fileCoordinates);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        String[] values;
        int numberofVertices = Integer.parseInt(line.split(" ")[0]);
        LinkedList<double[]> verticesL = new LinkedList<>();

        HashMap<Integer, Integer> verticesIndex = new HashMap<>();
//        float[][] vertices = new float[numberofVertices][3];
        int index = 0;
        int counter = 0;
        GeometryFactory gf = new GeometryFactory();
        Point p;
        while (br.ready()) {
            line = br.readLine();
            values = line.split(" ");
            double x = Double.parseDouble(values[0]);
            double y = Double.parseDouble(values[1]);
            double ele = Double.parseDouble(values[values.length - 1]);
            p = gf.createPoint(new Coordinate(x, y));
            if (filter.contains(p)) {
                verticesL.add(new double[]{x, y, ele});
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
        double[][] vertices = new double[verticesL.size()][3];
        counter = 0;
        for (double[] c : verticesL) {
            vertices[counter] = c;
            counter++;
        }
        verticesL.clear();
//        System.gc();
        //fileTriangleIndizes  //TRIMOD2.dat
        fr = new FileReader(fileTriangleIndizes);
        br = new BufferedReader(fr);
        line = br.readLine();
//        int numberofTriangles = Integer.parseInt(line.split(" ")[0]);
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

//            if (first != indizes[0] || second != indizes[1] || third != indizes[2]) {
////                System.out.println("Triangleindizes not correct");
//            }
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
        verticesIndex.clear();
        verticesIndex = null;
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
//        System.gc();

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
        return loadSurface(fileCoordinates, fileTriangleIndizes, fileNeighbours, coordReferenceXML, null);
    }

    public static double[][] readVertices(File xdatFile) throws IOException {
        FileReader fr = new FileReader(xdatFile);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        String[] values;
        //Number of vertices is the first entry in the first line.
        int numberofVertices = Integer.parseInt(line.split(" ")[0]);
        double[][] vertices = new double[numberofVertices][3];
        int index = 0;

        //Here comes information about the coordinates
        if (loadingAction != null) {
            loadingAction.progress = 0.f;
            loadingAction.hasProgress = true;
            loadingAction.description = "Surface: " + LoadingCoordinator.df1k.format(numberofVertices) + " Vertices...";
            loadingAction.updateProgress();
        }

        NumberConverter nc = new NumberConverter(br);
//        double[] dataparts = new double[3];
        while (br.ready()) {
            if (nc.readNextLineDoubles(vertices[index])) {//    
//                vertices[index][0] = dataparts[0];
//                vertices[index][1] = dataparts[1];
//                vertices[index][2] = dataparts[2];
                index++;
                if (index % 300 == 0 && loadingAction != null) {
                    loadingAction.progress = index / (float) numberofVertices;
                    loadingAction.updateProgress();
                }
            }
            if (index >= vertices.length) {
                break;
            }
        }
        br.close();
        fr.close();
        if (loadingAction != null) {
            loadingAction.progress = 1;
            loadingAction.updateProgress();
        }
        return vertices;
    }

    /**
     * Read the node index for each triangle
     *
     * @param trimod2dat
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static int[][] readTriangleNodes(File trimod2dat) throws FileNotFoundException, IOException {
        int[][] trianglenodes;
//        try (FileReader fr = new FileReader(trimod2dat); BufferedReader br = new BufferedReader(fr)) {
//            String line = br.readLine();
//            int index = 0;
//            String[] values = line.split(" ");
//            int numberOfTriangles = Integer.parseInt(values[0]);
//            trianglenodes = new int[numberOfTriangles][3];
//            while (br.ready()) {
//
//                line = br.readLine();
//                values = line.split(" ");
//                trianglenodes[index][0] = Integer.parseInt(values[0]);
//                trianglenodes[index][1] = Integer.parseInt(values[1]);
//                trianglenodes[index][2] = Integer.parseInt(values[2]);
//                index++;
//            }
//        }
//        return trianglenodes;

        FileReader fr = new FileReader(trimod2dat);
        BufferedReader br = new BufferedReader(fr);
        NumberConverter nc = new NumberConverter(br);
//        int[] integerParts = new int[3];
//        int first, second, third;
        int[] temp = new int[3];
        nc.readNextLineInteger(temp);
        int numberOfTriangles = temp[0];
        trianglenodes = new int[numberOfTriangles][3];
        int index = 0;
        while (br.ready()) {
            nc.readNextLineInteger(trianglenodes[index++]);
            if (index >= numberOfTriangles) {
                break;
            }
        }
        return trianglenodes;
    }

    /**
     * Read the groundtype and slope type from the ztriang.dat file.
     *
     * @param ztriangfile
     * @param numberOftriangles
     */
    public static int[] readGroundtype(File ztriangfile, int numberOftriangles) throws IOException {
        int[] groundtype = new int[numberOftriangles];
        FileReader fr = new FileReader(ztriangfile);
        BufferedReader br = new BufferedReader(fr);
        String[] split;
        int lineindex = 0;
        while (br.ready()) {
            split = br.readLine().replaceAll("\\s+", " ").trim().split(" ");
            if (split.length > 1) {
                groundtype[lineindex] = Integer.parseInt(split[1]);
                lineindex++;
            }
        }
        return groundtype;
    }

    /**
     * Read the edgetype for each edge of a triangle from the ztriang.dat file.
     *
     * @param ztriangfile
     * @param numberOftriangles
     * @return
     * @throws java.io.IOException
     */
    public static int[][] readEdgetype(File ztriangfile, int numberOftriangles) throws IOException {
        int[][] edgetype = new int[numberOftriangles][3];
        FileReader fr = new FileReader(ztriangfile);
        BufferedReader br = new BufferedReader(fr);
        String[] split;
        int lineindex = 0;
        while (br.ready()) {
            split = br.readLine().replaceAll("\\s+", " ").trim().split(" ");
            if (split.length > 1) {
                edgetype[lineindex][0] = Integer.parseInt(split[2]);
                edgetype[lineindex][1] = Integer.parseInt(split[3]);
                edgetype[lineindex][2] = Integer.parseInt(split[4]);
                lineindex++;
            }
        }
        return edgetype;
    }

    /**
     *
     * @param fileCoordinates Vertex coordinates e.g. "X.dat"
     * @param fileTriangleIndizes Indices of verties to form triangles
     * "TRIMOD2.dat"
     * @param fileNeighbours Neighbours per triangle e.g. "TRIMOD1.dat"
     * @param coordReferenceXML File where the coordinate reference is defined
     * @param crs Manual override of CoordinateReference e.g. "EPSG:25832"
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Surface loadSurface(File fileCoordinates, File fileTriangleIndizes, File fileNeighbours, File coordReferenceXML, String crs) throws FileNotFoundException, IOException {

        if (loadingAction != null) {
            loadingAction.progress = 0.f;
            loadingAction.hasProgress = true;
            loadingAction.description = "Init surface topography loading";
            loadingAction.updateProgress();
        }
//Coordinates   //X.dat
        double[][] vertices = readVertices(fileCoordinates);
        int numberofVertices = vertices.length;
//        System.out.println("  Reading Coords took " + (System.currentTimeMillis() - start) + "ms.");
        //Load coordinate reference System
        String epsgCode = crs;//"EPSG:25832"; //Use this standard code for Hannover
        if (coordReferenceXML != null && coordReferenceXML.exists() && coordReferenceXML.canRead()) {
            epsgCode = loadSpatialReferenceCode(coordReferenceXML);
            if (epsgCode.equals("102329")) {
                epsgCode = "EPSG:4647";
            } else {
                epsgCode = "EPSG:" + epsgCode;
            }

        }
//        System.out.println("   Decoding CRS took "+(System.currentTimeMillis()-start)+"ms");
        //fileTriangleIndizes  //TRIMOD2.dat
//        start = System.currentTimeMillis();
        FileReader fr = new FileReader(fileTriangleIndizes);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        int numberofTriangles = Integer.parseInt(line.split(" ")[0]);
        int[][] triangleIndizes = new int[numberofTriangles][3];
        double[][] triangleMidPoints = new double[numberofTriangles][3];

        boolean[] verticesUsed = new boolean[numberofVertices];
        int index = 0;
        double oneThird = 1. / 3.;
        NumberConverter nc = new NumberConverter(br);
        int[] integerParts = new int[3];
        int first, second, third;

        if (loadingAction != null) {
            loadingAction.progress = 0.f;
            loadingAction.hasProgress = true;
            loadingAction.description = "Surface: " + LoadingCoordinator.df1k.format(numberofTriangles) + " Triangles...";
            loadingAction.updateProgress();
        }

        while (br.ready()) {
            try {
                if (nc.readNextLineInteger(integerParts)) {
                    first = integerParts[0];
                    second = integerParts[1];
                    third = integerParts[2];

                    if (condenseUnusedVertices && !verticesUsed[first]) {
                        verticesUsed[first] = true;
                    }
                    if (condenseUnusedVertices && !verticesUsed[second]) {
                        verticesUsed[second] = true;
                    }
                    if (condenseUnusedVertices && !verticesUsed[third]) {
                        verticesUsed[third] = true;
                    }

                    triangleIndizes[index][0] = first;
                    triangleIndizes[index][1] = second;
                    triangleIndizes[index][2] = third;

                    triangleMidPoints[index][0] = (vertices[first][0] + vertices[second][0] + vertices[third][0]) * oneThird;
                    triangleMidPoints[index][1] = (vertices[first][1] + vertices[second][1] + vertices[third][1]) * oneThird;
                    triangleMidPoints[index][2] = (vertices[first][2] + vertices[second][2] + vertices[third][2]) * oneThird;

                    index++;

                    if (index % 300 == 0 && loadingAction != null) {
                        loadingAction.progress = index / (float) numberofTriangles;
                        loadingAction.updateProgress();
                    }
                }
            } catch (Exception exception) {
                System.err.println("Problem reading Node " + index + " : " + exception.getLocalizedMessage() + "  from file " + fileTriangleIndizes);
                System.out.println("TrianglesArray.length: " + triangleIndizes.length + "\t VerticesArray.length=" + vertices.length);
                exception.printStackTrace();
                break;
            }
        }
        br.close();
        fr.close();
        if (loadingAction != null) {
            loadingAction.progress = 1;
            loadingAction.updateProgress();
        }
//        System.out.println("   Building triangles took " + (System.currentTimeMillis() - start) + "ms");

        int used = 0;
        for (int i = 0; i < verticesUsed.length; i++) {
            if (verticesUsed[i]) {
                used++;
            }
        }
        if (condenseUnusedVertices) {
            System.out.println(used + " / " + verticesUsed.length + " Vertices are used by triangles (" + ((int) (used * 100 / (double) verticesUsed.length)) + "%)");
        }

        if (used < verticesUsed.length && condenseUnusedVertices) {
            long start = System.currentTimeMillis();

            if (loadingAction != null) {
                loadingAction.progress = 0.f;
                loadingAction.hasProgress = true;
                loadingAction.description = "Surface: Exclude " + LoadingCoordinator.df1k.format(verticesUsed.length - used) + " unused Vertices...";
                loadingAction.updateProgress();
            }
            double[][] verticesNew = new double[used][3];
            HashMap<Integer, Integer> oldToNew = new HashMap<>(used);
            int newIndex = 0;
            //Reorder Vertices
            for (int i = 0; i < verticesUsed.length; i++) {
                if (verticesUsed[i]) {
                    oldToNew.put(i, newIndex);
                    verticesNew[newIndex] = vertices[i];
                    newIndex++;
                }
                if (i % 10000 == 0 && loadingAction != null) {
                    loadingAction.progress = i / (verticesUsed.length * 0.5f);
                    loadingAction.updateProgress();
                }
            }

            //Reorder Triangle indices
            for (int i = 0; i < triangleIndizes.length; i++) {
                triangleIndizes[i][0] = oldToNew.get(triangleIndizes[i][0]);
                triangleIndizes[i][1] = oldToNew.get(triangleIndizes[i][1]);
                triangleIndizes[i][2] = oldToNew.get(triangleIndizes[i][2]);
                if (i % 10000 == 0 && loadingAction != null) {
                    loadingAction.progress = 0.5f + i / (numberofTriangles * 0.5f);
                    loadingAction.updateProgress();
                }
            }
            numberofVertices = used;
            vertices = null;
            vertices = verticesNew;
            verticesUsed = null;
            oldToNew.clear();
            oldToNew = null;
            //System.out.println("Reordering vertices took " + ((System.currentTimeMillis() - start)) + " ms.");
            if (loadingAction != null) {
                loadingAction.progress = 1;
                loadingAction.updateProgress();
            }
        }

        //fileNeighbours
        int[][] neighbours = null;
        if (fileNeighbours != null && fileNeighbours.exists()) {
            if (loadingAction != null) {
                loadingAction.progress = 0.f;
                loadingAction.hasProgress = true;
                loadingAction.description = "Surface: " + LoadingCoordinator.df1k.format(numberofTriangles) + " Neighbours...";
                loadingAction.updateProgress();
            }
//        start = System.currentTimeMillis();
            fr = new FileReader(fileNeighbours);
            br = new BufferedReader(fr);
//        System.out.println("Number of triangles: "+numberofTriangles);
            neighbours = new int[numberofTriangles][3];
            index = 0;
            integerParts = new int[9];
            int lines = 0;
            try {
                nc.setReader(br);
                while (br.ready()) {
                    lines++;
                    if (nc.readNextLineInteger(integerParts)) {
                        neighbours[index][0] = integerParts[0];
                        neighbours[index][1] = integerParts[1];
                        neighbours[index][2] = integerParts[2];

                        // -1 = House: no flow boundary
                        // -2 = outflow boundary / trespassable
                        if (integerParts[0] < 0) {
                            if (integerParts[6] == 1) {
                                neighbours[index][0] = -2;
                            }
                        }
                        if (integerParts[1] < 0) {
                            if (integerParts[7] == 1) {
                                neighbours[index][1] = -2;
                            }
                        }
                        if (integerParts[2] < 0) {
                            if (integerParts[8] == 1) {
                                neighbours[index][2] = -2;
                            }
                        }
                        index++;
                        if (index % 300 == 0 && loadingAction != null) {
                            loadingAction.progress = index / (float) numberofTriangles;
                            loadingAction.updateProgress();
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Problem in line " + lines + " of " + fileNeighbours);
                ex.printStackTrace();
            }
            br.close();
            fr.close();
        }
//        System.out.println("HE_SurfaceIO.loadSurface parsed " + lines + " into " + parts + " split parts.");
//        System.out.println("   Building Neighbours took " + (System.currentTimeMillis() - start) + "ms");
//        start = System.currentTimeMillis();
        if (loadingAction != null) {
            loadingAction.progress = 1f;
            loadingAction.hasProgress = false;
            loadingAction.description = "Completing Surface Topography...";
            loadingAction.updateProgress();
        }
        Surface surf = new Surface(vertices, triangleIndizes, neighbours, null, epsgCode, calculateSlopes);
        surf.setTriangleMids(triangleMidPoints);
        surf.fileTriangles = fileCoordinates.getParentFile();

        if (loadingAction != null) {
            loadingAction.progress = 1f;
            loadingAction.hasProgress = true;
            loadingAction.description = "Surface Topography completed";
            loadingAction.updateProgress();
        }
//        System.out.println("  Building Surface Object took " + (System.currentTimeMillis() - start) + "ms.");
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
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
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
     * Very slow version of mapping manholes and surface triangles. it is better
     * to load the references instead of looping over every triangle.
     *
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
//            Coordinate cll = manhole.getPosition().lonLatCoordinate();
            //switch coordinates
            m[index] = geotools.toUTM(manhole.getPosition());
            index++;
        }
        index = 0;

//        surface.clearTriangleCapacities();
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
                                Manhole mh = manholes[j];
                                mh.setSurfaceTriangle(i);
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

    /**
     * @deprecated @param nw
     * @param surface
     * @throws TransformException
     * @throws FactoryException
     */
    public static void mapStreetInlets(Network nw, Surface surface) throws TransformException, FactoryException {
        int index = 0;
        Coordinate[] in;
        if (nw.getStreetInlets() != null) {
            in = new Coordinate[nw.getStreetInlets().size()];
            for (Inlet inlet : nw.getStreetInlets()) {
                in[index] = surface.getGeotools().toUTM(inlet.getPosition().lonLatCoordinate());
                index++;
            }
        } else {
            in = new Coordinate[0];
            System.err.println("No inlets found in Network.");
            return;
        }
        index = 0;
//        int counterManholes = 0, counterInlets = 0;
//        Inlet[] inlets = new Inlet[0];
//        if (nw.getStreetInlets() != null) {
//            inlets = nw.getStreetInlets().toArray(new Inlet[nw.getStreetInlets().size()]);
//        }

        for (int i = 0; i < surface.triangleNodes.length; i++) {
            Coordinate c0 = new Coordinate(surface.vertices[surface.triangleNodes[i][0]][0], surface.vertices[surface.triangleNodes[i][0]][1]);
            Coordinate c1 = new Coordinate(surface.vertices[surface.triangleNodes[i][1]][0], surface.vertices[surface.triangleNodes[i][1]][1]);
            Coordinate c2 = new Coordinate(surface.vertices[surface.triangleNodes[i][2]][0], surface.vertices[surface.triangleNodes[i][2]][1]);

            for (int j = 0; j < in.length; j++) {
                Coordinate m1 = in[j];
//                Point p1 = null;
                if (Math.abs(c0.x - m1.x) < 20) {
                    if (Math.abs(c0.y - m1.y) < 20) {
                        if (c0.distance(m1) < 20) {
                            //Closer look
                            boolean contains = GeometryTools.triangleContainsPoint(c0.x, c1.x, c2.x, c0.y, c1.y, c2.y, m1.x, m1.y);
                            if (contains) {
//                                counterInlets++;
                                if (true) {
                                    //Neighbour Triangles also get a reference to this inlet
                                    if (surface.neumannNeighbours != null) {
                                        for (int k = 0; k < 3; k++) {
                                            int nbi = surface.neumannNeighbours[i][k];
                                            if (nbi < 0) {
                                                continue;
                                            }
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
                        int timesteps = raster.getIndexContainer().getNumberOfTimes();
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
            } else if (surface.getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
                SurfaceMeasurementRectangleRaster raster = (SurfaceMeasurementRectangleRaster) surface.getMeasurementRaster();
                for (int y = 0; y < raster.getNumberYIntervals(); y++) {
                    for (int x = 0; x < raster.getNumberXIntervals(); x++) {
                        int count = 0;
                        for (int t = 0; t < raster.getNumberOfTimes(); t++) {
                            count += raster.getParticlesCountedMaterialSum(x, y, t);
                        }
                        bw.write(count + ";");
                    }
                    bw.newLine();
                }
            } else {
                throw new UnsupportedOperationException("Type of Surface Raster " + surface.getMeasurementRaster().getClass().getSimpleName() + " is not known to be handled for output.");
            }
        }
    }

    public static void writeSurfaceContaminationDynamicMassCSV(File outputFile, Surface surface, int materialIndex) throws IOException {
        if (surface == null) {
            throw new NullPointerException("No Surface set. No output file written.");
        }
        int categories = surface.getNumberOfMaterials();
        if (categories < 1) {
            throw new NullPointerException("No Surface Material Categories. No output file written.");
        }
        DecimalFormat df4 = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if (surface.fileTriangles != null) {
                bw.write("Surface:" + surface.fileTriangles.getParentFile().getName() + "/" + surface.fileTriangles.getName());
                bw.newLine();
            }
            bw.write("Reduced Net:" + (surface.mapIndizes != null && !surface.mapIndizes.isEmpty()));
            bw.newLine();

            bw.write("Contaminant category:" + materialIndex);
            bw.newLine();
            bw.write("Timesteps:" + surface.getMeasurementRaster().getIndexContainer().getNumberOfTimes());
            bw.newLine();
            bw.write("Stepduration:" + surface.getMeasurementRaster().getIndexContainer().getDeltaTimeMS() + "ms");
            bw.newLine();
            bw.write("TriangleID;[Coordinate];mMax;m0;m1;m2;...");
            bw.newLine();
            bw.write("***");
            bw.newLine();
            StringBuffer buffer = new StringBuffer(80);
//            if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementRaster raster = /*(SurfaceMeasurementTriangleRaster)*/ surface.getMeasurementRaster();
            int cellcount = raster.getNumberOfCells();
            int timesteps = raster.getIndexContainer().getNumberOfTimes();
            for (int mID = 0; mID < cellcount; mID++) {
                if (raster.isCellContaminated(mID)) {
                    buffer.delete(0, buffer.length());

                    double max = 0;
                    for (int j = 0; j < timesteps; j++) {
                        double mass = raster.getMassInCell(mID, j, materialIndex);
                        max = Math.max(max, mass);
                        buffer.append(";").append(df4.format(mass));
                    }
                    if (max > 0) {//Only write cell information, if there is content
                        bw.write(mID + ";");
                        Coordinate coord = raster.getCenterOfCell(mID);
                        bw.write("[" + df4.format(coord.x) + "," + df4.format(coord.y) + "," + df4.format(coord.z) + "]");
                        bw.write(";" + max);
                        bw.write(buffer.toString());
                        bw.newLine();
                    }
                }
            }
        }
    }

    public static void writeSurfaceContaminationDynamicParticlesCSV(File outputFile, Surface surface, int materialIndex, int numberOfParticles) throws IOException {
        if (surface == null) {
            throw new NullPointerException("No Surface set. No output file written.");
        }
        int categories = surface.getNumberOfMaterials();
        if (categories < 1) {
            throw new NullPointerException("No Surface Material Categories. No output file written.");
        }
        //Number of particles can be a decimal number if continuous measurements is enabled
        DecimalFormat df2 = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        DecimalFormat df3 = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));
        int[] samplesTaken = surface.getMeasurementRaster().measurementsInTimeinterval;
        int numberofIntervals = samplesTaken.length;
        if (samplesTaken[samplesTaken.length - 1] < 1) {
            numberofIntervals--;
        }
//        double sum=0;
//        for (int c = 0; c < surface.getMeasurementRaster().getNumberOfCells(); c++) {
//            for (int i = 0; i < surface.getMeasurementRaster().measurementTimestamp.length; i++) {
//                double d=surface.getMeasurementRaster().getRawNumberOfParticlesInCell(c, i, materialIndex);
//                sum+=d;
//            }
//        }
//        System.out.println("Total mass on surface: "+sum);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if (surface.fileTriangles != null) {
                bw.write("Surface:" + surface.fileTriangles.getParentFile().getName() + "/" + surface.fileTriangles.getName());
                bw.newLine();
            }
            bw.write("Reduced Net:" + (surface.mapIndizes != null && !surface.mapIndizes.isEmpty()));
            bw.newLine();
            bw.write("CellCount:" + surface.getMeasurementRaster().getNumberOfCells());
            bw.newLine();
            bw.write("ParticleCount:" + numberOfParticles);
            bw.newLine();

            bw.write("Contaminant category:" + materialIndex);
            bw.newLine();
            bw.write("Timesteps:" + numberofIntervals);
            bw.newLine();
            bw.write("Samples taken:");
            for (int i = 0; i < numberofIntervals; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(samplesTaken[i] + "");
            }
            bw.newLine();
            bw.write("Timestamps:");
            for (int i = 0; i < numberofIntervals; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(surface.getMeasurementRaster().measurementTimestamp[i] + "");
            }
            bw.newLine();
            bw.write("Stepduration:" + surface.getMeasurementRaster().getIndexContainer().getDeltaTimeMS() + "ms");
            bw.newLine();
            bw.write("TriangleID;[Coordinate];#Max;#0;#1;#2;...");
            bw.newLine();
            bw.write("***");
            bw.newLine();
            StringBuffer buffer = new StringBuffer(80);
//            if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementRaster raster = /*(SurfaceMeasurementTriangleRaster)*/ surface.getMeasurementRaster();
            int cellcount = raster.getNumberOfCells();
            int timesteps = raster.getIndexContainer().getNumberOfTimes();
            for (int mID = 0; mID < cellcount; mID++) {
                if (raster.isCellContaminated(mID)) {
                    buffer.delete(0, buffer.length());

                    double max = 0;
                    for (int j = 0; j < timesteps; j++) {
                        double particles = raster.getRawNumberOfParticlesInCell(mID, j, materialIndex);
                        max = Math.max(max, particles);
                        buffer.append(";").append(df3.format(particles));
                    }
                    if (max >= 1) {//Only write cell information, if there is content
                        bw.write(mID + ";");
                        Coordinate coord = raster.getCenterOfCell(mID);
                        bw.write("[" + df2.format(coord.x) + "," + df2.format(coord.y) + "," + df2.format(coord.z) + "]");
                        bw.write(";" + max);
                        bw.write(buffer.toString());
                        bw.newLine();
                    }
                }
            }
        }

    }

    public static void writeSurfaceMassCSV(File outputFile, Surface surface, int materialIndex, int numberOfParticles, int decimalDigits) throws IOException {
        if (surface == null) {
            throw new NullPointerException("No Surface set. No output file written.");
        }
        int categories = surface.getNumberOfMaterials();
        if (categories < 1) {
            throw new NullPointerException("No Surface Material Categories. No output file written.");
        }
        //Number of particles can be a decimal number if continuous measurements is enabled
        DecimalFormat df2 = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));
        df2.setMaximumFractionDigits(decimalDigits);
        double sampleDuration = 0;
        int[] samplesTaken = surface.getMeasurementRaster().measurementsInTimeinterval;
        int numberofIntervals = samplesTaken.length;
        if (samplesTaken[samplesTaken.length - 1] < 1) {
            numberofIntervals--;

        }
        for (int i = 0; i < surface.getMeasurementRaster().durationInTimeinterval.length; i++) {
            sampleDuration += surface.getMeasurementRaster().durationInTimeinterval[i];
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            bw.write("Mass on surface cells [kg]");
            bw.newLine();
            if (surface.fileTriangles != null) {
                bw.write("Surface:" + surface.fileTriangles.getParentFile().getName() + "/" + surface.fileTriangles.getName());
                bw.newLine();
            }
            bw.write("Reduced Net:" + (surface.mapIndizes != null && !surface.mapIndizes.isEmpty()));
            bw.newLine();
            bw.write("CellCount:" + surface.getMeasurementRaster().getNumberOfCells());
            bw.newLine();
            bw.write("ParticleCount:" + numberOfParticles);
            bw.newLine();
            bw.write("Contaminant category:" + materialIndex);
            bw.newLine();
            bw.write("Timesteps:" + numberofIntervals);
            bw.newLine();
            bw.write("Samples taken:");
            for (int i = 0; i < numberofIntervals; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(samplesTaken[i] + "");
            }
            bw.newLine();
            bw.write("Total sample duration [s]:" + sampleDuration);
            bw.newLine();
            bw.write("Sampling duration [s]:");
            for (int i = 0; i < numberofIntervals; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(surface.getMeasurementRaster().durationInTimeinterval[i] + "");
            }
            bw.newLine();
            bw.write("Timestamps:");
            for (int i = 0; i < numberofIntervals; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(surface.getMeasurementRaster().measurementTimestamp[i] + "");
            }
            bw.newLine();
            bw.write("Stepduration:" + surface.getMeasurementRaster().getIndexContainer().getDeltaTimeMS() + "ms");
            bw.newLine();
            bw.write("TriangleID;[Coordinate];#Max;#0;#1;#2;...");
            bw.newLine();
            bw.write("***");
            bw.newLine();
            StringBuffer buffer = new StringBuffer(80);
//            if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementRaster raster = /*(SurfaceMeasurementTriangleRaster)*/ surface.getMeasurementRaster();
            int cellcount = raster.getNumberOfCells();
            int timesteps = raster.getIndexContainer().getNumberOfTimes();
            for (int mID = 0; mID < cellcount; mID++) {
                if (raster.isCellContaminated(mID)) {
                    buffer.delete(0, buffer.length());

                    double max = 0;
                    for (int j = 0; j < timesteps; j++) {
                        double particles = raster.getMassInCell(mID, j, materialIndex);
                        max = Math.max(max, particles);
                        buffer.append(";").append(df2.format(particles));
                    }
                    if (max > 0) {//Only write cell information, if there is content
                        bw.write(mID + ";");
                        Coordinate coord = raster.getCenterOfCell(mID);
                        bw.write("[" + df2.format(coord.x) + "," + df2.format(coord.y) + "," + df2.format(coord.z) + "]");
                        bw.write(";" + max);
                        bw.write(buffer.toString());
                        bw.newLine();
                    }
                }
            }
        }
    }

    public static void writeSurfaceWaterlevelDynamicsCSV(File outputFile, Surface surface) throws IOException {
        if (surface == null) {
            throw new NullPointerException("No Surface set. No output file written.");
        }
        DecimalFormat df3 = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));
        float[][] waterlevels = surface.getWaterlevels();
        double[] maxWaterlevels = surface.getMaxWaterLevels();
        double maxWL = 0;
        if (maxWaterlevels != null) {
            for (int i = 0; i < maxWaterlevels.length; i++) {
                maxWL = Math.max(maxWL, maxWaterlevels[i]);
            }
        } else {
            //Need to calculated everything first
            int count = 0;
            maxWaterlevels = new double[waterlevels.length];
            for (int i = 0; i < waterlevels.length; i++) {
                if (waterlevels[i] != null) {
                    for (int j = 0; j < waterlevels[i].length; j++) {
                        maxWL = Math.max(maxWL, waterlevels[i][j]);
                        maxWaterlevels[i] = Math.max(maxWaterlevels[i], waterlevels[i][j]);
                    }
                    count++;
                }
            }
            System.out.println("waterlevels for " + count + " triangles");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if (surface.fileTriangles != null) {
                bw.write("Surface:" + surface.fileTriangles.getParentFile().getName() + "/" + surface.fileTriangles.getName());
                bw.newLine();
            }
            bw.write("Reduced Net:" + (surface.mapIndizes != null && !surface.mapIndizes.isEmpty()));
            bw.newLine();

            bw.write("Waterlevels:" + maxWaterlevels.length);
            bw.newLine();
            bw.write("Maximum:" + maxWL);
            bw.newLine();
            bw.write("Timesteps:" + surface.getNumberOfTimes());
            bw.newLine();
            bw.write("Stepduration:" + surface.getTimes().getDeltaTimeMS() + "ms");
            bw.newLine();
            bw.write("TriangleID;[Coordinate];max;t0;t1;t2;...");
            bw.newLine();
            bw.write("***");
            int timesteps = surface.getNumberOfTimes();
            for (int mID = 0; mID < waterlevels.length; mID++) {
                if (maxWaterlevels[mID] >= 0.001) {
                    bw.newLine();
                    bw.write(mID + ";");
                    double[] xyz = surface.getTriangleMids()[mID];
                    bw.write("[" + df3.format(xyz[0]) + "," + df3.format(xyz[1]) + "," + df3.format(xyz[2]) + "]");
                    bw.write(";" + maxWaterlevels[mID]);
                    for (int j = 0; j < timesteps; j++) {
                        bw.append(";").append(df3.format(waterlevels[mID][j]));
                    }
                    bw.flush();
                }
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
        FileReader fr = new FileReader(file);
        try (BufferedReader br = new BufferedReader(fr)) {

            String line = br.readLine();
            int numberOfTriangles = Integer.parseInt(line);
            neumann = new int[numberOfTriangles][];
//            NumberConverter nc = new NumberConverter(br);
            char c;
//        boolean lastWasSplitter = true;
            int linelength;
            char splitID = ',';
            char splitNeighbours = ' ';
            int indexComma;
            int neighbours;
            int index;
            char[] buffer = new char[256];
            NumberConverter nc = new NumberConverter(br);
            while (br.ready()) {
                neighbours = 0;
                indexComma = -1;
                linelength = -1;
                for (int i = 0; i < buffer.length; i++) {
                    if (br.ready()) {
                        c = (char) br.read();
                        if (c == 10 || c == 13) {
                            if (i == 0) {
                                //this LF still is part of the old line. we need to skip this to get the next line
                                i--;
                                continue;
                            }
                            linelength = i;
                            break;//\n & \r
                        } else if (c == splitID) {
                            indexComma = i;
                        } else if (c == splitNeighbours) {
                            neighbours++;
                        }
                        buffer[i] = c;
                    }
                }

                if (indexComma < 0) {
                    if (linelength > 0) {
                        //return number of elements
                        int[][] retur = new int[1][1];
                        retur[0][0] = nc.parseIntegerFromToInclude(buffer, 0, linelength);
                        return retur;
                    } else {
                        //no line here, it was the end of the file
                        return null;
                    }
                }
                int[] neighbourIDs = new int[neighbours];
                int lastBlank = indexComma + 1;
                index = 0;
                for (int i = lastBlank + 1; i < linelength; i++) {
                    if (buffer[i] == splitNeighbours) {
                        neighbourIDs[index] = nc.parseIntegerFromToInclude(buffer, lastBlank + 1, i - 1);
                        index++;
                        lastBlank = i;
                    }
                }
                int triangleID = nc.parseIntegerFromToInclude(buffer, 0, indexComma - 1);
                neumann[triangleID] = neighbourIDs;
            }
            fr.close();
//            line = null;
//            split = null;
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
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            NumberConverter nc = new NumberConverter(br);
            //Read number of nodes to get the size of array
            String line = br.readLine();
            String[] str = line.split(":");
            int size = Integer.parseInt(str[1]);
            node2TriangleIDs = new int[size][];
            //Skip header
            br.readLine();
            //Read content
            while (br.ready()) {
                int[][] data = nc.getNodeToTriangleLine();
                if (data == null || data.length < 2) {
                    break;
                }
                node2TriangleIDs[data[0][0]] = data[1];
            }
//            Pattern splitter = Pattern.compile(";");
//            while (br.ready()) {
//                line = br.readLine();
//                str = splitter.split(line);//line.split(";");
//                int nodeID = Integer.parseInt(str[0]);
//                //Length=number of triangle IDs assigned to this Node.
//                int length = Integer.parseInt(str[1]);
//                node2TriangleIDs[nodeID] = new int[length];
//                for (int i = 0; i < length; i++) {
//                    node2TriangleIDs[nodeID][i] = Integer.parseInt(str[i + 2]);
//                }
//            }
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
            if (line == null || line.isEmpty()) {
                return new ArrayList<>(0);
            }
            String numberOfManholesStr = line.replaceAll("[^0-9.,]+", "").trim();
            int numberOfManholes = 10;
            try {
                numberOfManholes = Integer.parseInt(numberOfManholesStr);
            } catch (Exception exception) {
            }
            manhole2Triangle = new ArrayList<>(numberOfManholes);
            while (br.ready()) {
                line = br.readLine();
                if (line.length() < 20) {
                    continue;
                }
                String[] seperated = line.split("%");
                String[] values = null;
                try {
                    values = seperated[2].replaceAll("  ", " ").split(" ");

                    Pair<String, Integer> pair = new Pair<>(seperated[0].split(" ")[0], Integer.parseInt(values[2]));
                    manhole2Triangle.add(pair);
                } catch (Exception e) {
                    System.out.println(" problem with line '" + line + "'");
                }
            }
        }
        return manhole2Triangle;
    }

    /**
     * Read the height information of the triangles' center position given in
     * the Ztriang.dat file.
     *
     * @param filetriangleZ Path to Ztriang.dat file
     * @param numberOftriangles
     * @return
     */
    public static double[] loadTriangleZ(File filetriangleZ, int numberOftriangles) {
        double[] z = new double[numberOftriangles];
        try (FileReader fr = new FileReader(filetriangleZ); BufferedReader br = new BufferedReader(fr)) {
            int index = 0;
            String[] s = null;
            while (br.ready()) {
                s = br.readLine().split(" ");
                if (s != null & s.length > 1) {
                    z[index] = Double.parseDouble(s[1]);
                    index++;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return z;
    }

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
                        mass[j] += m.getMassResidence()[j][t];
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
                    double[] v0 = s.vertices[s.triangleNodes[i][j]];
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

    /**
     * Creates X.dat, TRIMOD1.dat, TRIMOD2.dat in the given directory.
     *
     * @param surface
     * @param directory
     * @throws java.io.IOException
     */
    public static void writeSurfaceFiles(Surface surface, File directory) throws IOException {
        File fileVertices = new File(directory, "X.dat");
        if (fileVertices.exists()) {
            throw new IOException("File " + fileVertices + " already exists. Delete it before creating a new one.");
        }
        File fileTriangles = new File(directory, "TRIMOD2.dat");
        if (fileTriangles.exists()) {
            throw new IOException("File " + fileTriangles + " already exists. Delete it before creating a new one.");
        }
        File fileNeumannNeighbours = new File(directory, "TRIMOD1.dat");
        if (fileNeumannNeighbours.exists()) {
            throw new IOException("File " + fileNeumannNeighbours + " already exists. Delete it before creating a new one.");
        }

//        DecimalFormat df5 = new DecimalFormat("#.00000", DecimalFormatSymbols.getInstance(Locale.US));
//        FileWriter fw;
//        BufferedWriter bw;
        //Write Vertices
        writeVertices(surface, fileVertices);
//        fw = new FileWriter(fileVertices);
//        bw = new BufferedWriter(fw);
//        bw.write(surface.vertices.length + " " + surface.vertices.length + " " + surface.vertices.length);
//        bw.newLine();
//        for (int i = 0; i < surface.vertices.length; i++) {
//            bw.write(df5.format(surface.vertices[i][0]) + " " + df5.format(surface.vertices[i][1]) + " " + df5.format(surface.vertices[i][2]));
//            bw.newLine();
//            bw.flush();
//        }
//        bw.flush();
//        bw.close();
//        fw.close();
        System.out.println("Vertices written to " + fileVertices.getAbsolutePath());

        //Write Triangles to File TRIMOD2.dat
        writeTRIMOD2dat(surface, fileTriangles);
//        fw = new FileWriter(fileTriangles);
//        bw = new BufferedWriter(fw);
//        //Header number of triangles
//        bw.write(surface.triangleNodes.length + " " + surface.triangleNodes.length + " " + surface.triangleNodes.length);
//        bw.newLine();
//        for (int i = 0; i < surface.triangleNodes.length; i++) {
//            bw.write(surface.triangleNodes[i][0] + " " + surface.triangleNodes[i][1] + " " + surface.triangleNodes[i][2]);
//            bw.newLine();
//            bw.flush();
//        }
//        bw.flush();
//        bw.close();
//        fw.close();
        System.out.println("Triangles written to " + fileTriangles.getAbsolutePath());

        //Write Neighbours (vonNeumann) to TRIMOD1.dat
        writeTRIMOD1dat(surface, fileNeumannNeighbours);
//        fw = new FileWriter(fileNeumannNeighbours);
//        bw = new BufferedWriter(fw);
//
//        for (int i = 0; i < surface.neumannNeighbours.length; i++) {
//            bw.write(surface.neumannNeighbours[i][0] + " " + surface.neumannNeighbours[i][1] + " " + surface.neumannNeighbours[i][2]);
//            bw.write("   " + (surface.neumannNeighbours[i][0] < 0 ? "-1" : "0") + " " + (surface.neumannNeighbours[i][1] < 0 ? "-1" : "0") + " " + (surface.neumannNeighbours[i][2] < 0 ? "-1" : "0"));
//            //bw.write("    " + (surface.neumannNeighbours[i][0] < 0 ? "1" : "-1") + " " + (surface.neumannNeighbours[i][1] < 0 ? "1" : "-1") + " " + (surface.neumannNeighbours[i][2] < 0 ? "1" : "-1"));
//            //bw.write("    ");
//            bw.newLine();
//            bw.flush();
//        }
//        bw.flush();
//        bw.close();
//        fw.close();
        System.out.println("Neumannneighbours written to " + fileNeumannNeighbours.getAbsolutePath());
    }

    /**
     * Write the TRIMOD1.dat file with information about 3 neighbours and the
     * neighbour orientation.
     *
     * @param surface
     * @param fileTRIMOD1dat
     * @throws IOException
     */
    public static void writeTRIMOD1dat(Surface surface, File fileTRIMOD1dat) throws IOException {
        try ( //Write Neighbours (vonNeumann) to TRIMOD1.dat
                FileWriter fw = new FileWriter(fileTRIMOD1dat); BufferedWriter bw = new BufferedWriter(fw)) {
            int[] neighboursNeighbours;
            for (int id = 0; id < surface.neumannNeighbours.length; id++) {
                bw.append(surface.neumannNeighbours[id][0] + " " + surface.neumannNeighbours[id][1] + " " + surface.neumannNeighbours[id][2] + "  ");
                //+ " " + (surface.neumannNeighbours[i][1] < 0 ? "-1" : "0") + " " + (surface.neumannNeighbours[i][2] < 0 ? "-1" : "0"));

                //NB Orientation
                for (int nb = 0; nb < 3; nb++) {
                    int nbid = surface.neumannNeighbours[id][nb];
                    if (nbid < 0) {
                        //open boundary
                        bw.append(" -1");
                    } else {
                        //If we are close to the boundary, we need an open boundary.

                        neighboursNeighbours = surface.neumannNeighbours[nbid];
                        for (int i = 0; i < neighboursNeighbours.length; i++) {
                            if (neighboursNeighbours[i] == id) {
                                bw.append(" " + i);
                                break;
                            }
                        }
                    }

                }
                bw.newLine();
                bw.flush();
            }
            bw.flush();
        }
    }

    public static void writeTRIMOD2dat(Surface surface, File fileTRIMOD2dat) throws IOException {
        FileWriter fw = new FileWriter(fileTRIMOD2dat);
        BufferedWriter bw = new BufferedWriter(fw);
        //Header number of triangles
        bw.write(surface.triangleNodes.length + " " + surface.triangleNodes.length + " " + surface.triangleNodes.length);
        bw.newLine();
        for (int i = 0; i < surface.triangleNodes.length; i++) {
            bw.write(surface.triangleNodes[i][0] + " " + surface.triangleNodes[i][1] + " " + surface.triangleNodes[i][2]);
            bw.newLine();
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
    }

    /**
     * Write the vertices in the X.dat format.
     *
     * @param surface
     * @param fileVertices
     * @throws IOException
     */
    public static void writeVertices(Surface surface, File fileVertices) throws IOException {
        DecimalFormat df5 = new DecimalFormat("#.00000", DecimalFormatSymbols.getInstance(Locale.US));

        //Write Vertices
        FileWriter fw = new FileWriter(fileVertices);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(surface.vertices.length + " " + surface.vertices.length + " " + surface.vertices.length);
        bw.newLine();
        for (int i = 0; i < surface.vertices.length; i++) {
            bw.write(df5.format(surface.vertices[i][0]) + " " + df5.format(surface.vertices[i][1]) + " " + df5.format(surface.vertices[i][2]));
            bw.newLine();
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
    }

    /**
     * Writes the z coorztriang.dat content to the given file.
     *
     * @param triangleMids
     * @param outputFile
     * @throws IOException
     */
    public static void writeTriangleZ(double[][] triangleMids, File outputFile) throws IOException {
        if (outputFile.exists()) {
            throw new IOException("File '" + outputFile + " already exists.");
        }
        FileWriter fw = new FileWriter(outputFile);
        BufferedWriter bw = new BufferedWriter(fw);
        DecimalFormat df3 = new DecimalFormat("#.000", DecimalFormatSymbols.getInstance(Locale.US));
        for (int i = 0; i < triangleMids.length; i++) {
            double[] triangleMid = triangleMids[i];
            bw.write(" " + df3.format(triangleMid[2]) + "   0   -1   -1   -1");// " 101   -1   -1   -1   0    0");
            bw.newLine();
            bw.flush();
        }
        bw.close();
        fw.close();
        System.out.println("Triangle heights written to " + outputFile);
    }

    /**
     * Writes the z coorztriang.dat content to the given file.
     *
     * @param triangleZs
     * @param outputFile
     * @throws IOException
     */
    public static void writeTriangleZ(double[] triangleZs, File outputFile) throws IOException {
        if (outputFile.exists()) {
            throw new IOException("File '" + outputFile + " already exists.");
        }
        try (FileWriter fw = new FileWriter(outputFile)) {
            BufferedWriter bw = new BufferedWriter(fw);
            DecimalFormat df3 = new DecimalFormat("#.000", DecimalFormatSymbols.getInstance(Locale.US));
            for (int i = 0; i < triangleZs.length; i++) {
                bw.write(" " + df3.format(triangleZs[i]) + "   0   -1   -1   -1");// " 101   -1   -1   -1   0    0");
                bw.newLine();
                bw.flush();
            }
            bw.close();
        }
        System.out.println("Triangle heights written to " + outputFile);
    }

    /**
     * Writes the z coorztriang.dat content to the given file.
     *
     * @param triangleZs
     * @param outputFile
     * @throws IOException
     */
    public static void writeTriangleZ(double[] triangleZs, int[] segmentclass, int[][] neighbours, File outputFile) throws IOException, Exception {
        if (outputFile.exists()) {
            throw new IOException("File '" + outputFile + " already exists.");
        }
        if (triangleZs.length != segmentclass.length) {
            throw new Exception("Length of trianglez (" + triangleZs.length + ") and segmentclasses (" + segmentclass.length + ") are not equal.");
        }
        if (triangleZs.length != neighbours.length) {
            throw new Exception("Length of trianglez (" + triangleZs.length + ") and neighbours (" + neighbours.length + ") are not equal.");
        }
        try (FileWriter fw = new FileWriter(outputFile)) {
            BufferedWriter bw = new BufferedWriter(fw);
            DecimalFormat df3 = new DecimalFormat("#.000", DecimalFormatSymbols.getInstance(Locale.US));
            for (int i = 0; i < triangleZs.length; i++) {
                bw.append(" " + df3.format(triangleZs[i]) + "   " + segmentclass[i]);
                for (int j = 0; j < 3; j++) {
                    if (neighbours[i][j] < 0) {
                        bw.append("   0");
                    } else {
                        bw.append("  -1");
                    }
                }

                bw.newLine();
                bw.flush();
            }
            bw.close();
        }
        System.out.println("Triangle heights written to " + outputFile);
    }

    public static void writeMeshPly(Surface surf, File output) throws IOException {
        if (output.exists()) {
            throw new IOException("File " + output + " already exists. Will not overwrite. terminate here.");
        }
        FileWriter fw = new FileWriter(output);
        BufferedWriter bw = new BufferedWriter(fw);
        //write header
        bw.write("ply\n"
                + "format ascii 1.0\n"
                + "element vertex " + surf.vertices.length + "\n"
                + "property float32 x\n"
                + "property float32 y\n"
                + "property float32 z\n"
                + "property uint8 red\n"
                + "property uint8 green\n"
                + "property uint8 blue\n"
                + "element face " + surf.triangleNodes.length + "\n"
                + "property uint8 intensity\n"
                + "property list uint8 int32 vertex_indices\n"
                + "end_header");
        bw.newLine();
        DecimalFormat df4 = new DecimalFormat("#.0000", DecimalFormatSymbols.getInstance(Locale.US));
        for (int i = 0; i < surf.vertices.length; i++) {
            double[] vertex = surf.vertices[i];
            bw.write(df4.format(vertex[0]) + " " + df4.format(vertex[1]) + "  " + df4.format(vertex[2]) + " 0 0 0 \n");
            bw.flush();
        }
        for (int i = 0; i < surf.triangleNodes.length; i++) {
            int[] nodes = surf.triangleNodes[i];
            bw.write("128 3 " + nodes[0] + " " + nodes[1] + " " + nodes[2] + " \n");
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
        System.out.println("Mesh written to " + output.getAbsolutePath());
    }

    /**
     * No idea, what this file is for, but it says "-1 0" for every triangle.
     *
     * @param numberOfTriangles
     * @param output
     * @throws IOException
     */
    public static void writeDecimDat(int numberOfTriangles, File output) throws IOException {
        if (output.exists()) {
            throw new IOException("File " + output + " already exists. Will not overwrite. terminate here.");
        }
        FileWriter fw = new FileWriter(output);
        BufferedWriter bw = new BufferedWriter(fw);
        for (int i = 0; i < numberOfTriangles; i++) {
            bw.write("-1 0\n");
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
        System.out.println("Decim written to " + output.getAbsolutePath());
    }

    public static void writeTRIBOUNDARYdat(int numberOfTriangles, File output) throws IOException {
        if (output.exists()) {
            throw new IOException("File " + output + " already exists. Will not overwrite. terminate here.");
        }
        FileWriter fw = new FileWriter(output);
        BufferedWriter bw = new BufferedWriter(fw);
        for (int i = 0; i < numberOfTriangles; i++) {
            bw.write("0\n");
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
        System.out.println("TRIBOUNDARY written to " + output.getAbsolutePath());
    }

    public static Polygon loadBoundary(File fileBoundaryXY) throws FileNotFoundException, IOException {
        LinkedList<Coordinate> list = new LinkedList<>();
        FileReader fr = new FileReader(fileBoundaryXY);
        BufferedReader br = new BufferedReader(fr);
        while (br.ready()) {
            String[] values = br.readLine().trim().split(" ");
            if (values.length != 2) {
                continue;
            }
            double x = Double.parseDouble(values[0]);
            double y = Double.parseDouble(values[1]);
            Coordinate c = new Coordinate(x, y);
            list.add(c);
        }
        br.close();
        fr.close();
        if (!list.getFirst().equals2D(list.getLast())) {
            list.addLast(list.getFirst());
        }
        GeometryFactory gf = new GeometryFactory();
        return gf.createPolygon(list.toArray(new Coordinate[list.size()]));
    }

    public static MultiPoint loadBoundaryAsMultiPoint(File fileBoundaryXY) throws FileNotFoundException, IOException {
        LinkedList<Coordinate> list = new LinkedList<>();
        FileReader fr = new FileReader(fileBoundaryXY);
        BufferedReader br = new BufferedReader(fr);
        while (br.ready()) {
            String[] values = br.readLine().trim().split(" ");
            if (values.length != 2) {
                continue;
            }
            double x = Double.parseDouble(values[0]);
            double y = Double.parseDouble(values[1]);
            Coordinate c = new Coordinate(x, y);
            list.add(c);
        }
        br.close();
        fr.close();
        if (!list.getFirst().equals2D(list.getLast())) {
            list.addLast(list.getFirst());
        }
        GeometryFactory gf = new GeometryFactory();
        return gf.createMultiPointFromCoords(list.toArray(new Coordinate[list.size()]));
    }

    public static File find_HYSTEM_DAT(File rootFile) {
        if (rootFile.getName().endsWith(".dat")) {
            rootFile = rootFile.getParentFile();
        }
        return new File(rootFile, "hystem.dat");
    }

    /**
     * Check with small letters for containing area names in the file and return
     * all triangle ids that's area contains the string.
     *
     * @param contained a String that should be contained in the name of the
     * subarea
     * @param hystem_dat The file containing the mapping SUbareaname -> CellIDs
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static long[] getTriangleIDsOnAreas(String contained, File hystem_dat) throws FileNotFoundException, IOException {
        contained = contained.toLowerCase();
        LinkedList<Long> triangleIDs = new LinkedList<>();
        try (FileReader in = new FileReader(hystem_dat); BufferedReader br = new BufferedReader(in)) {
            String line;
            String check;
            String[] values;
            while (br.ready()) {
                line = br.readLine();
                if (line.length() < 3) {
                    continue;
                }
                try {
                    int pos = line.indexOf(" ");
                    if (pos < 1) {
                        continue;
                    }
                    check = line.substring(0, pos).toLowerCase();
                    if (check.contains(contained)) {
                        values = line.split(" ");
                        for (int i = 1; i < values.length; i++) {
                            triangleIDs.add(Long.parseLong(values[i]));
                        }
                    }
                } catch (Exception exception) {
                    System.err.println("'" + line + "'");
                    exception.printStackTrace();
                }
            }
        }
        long[] array = new long[triangleIDs.size()];
        int index = 0;
        for (Long triangleID : triangleIDs) {
            array[index] = triangleID;
            index++;
        }
        return array;
    }

    public static int[][] readHystemDatTriangles(File hystemdatfile, String[] areas) throws FileNotFoundException, IOException {
        if (!hystemdatfile.exists()) {
            return null;
        }
        FileReader fr = new FileReader(hystemdatfile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        String[] splits;
        int counterfound = 0;
        String area;
        String gap = " ";
        boolean foundname;
        int[][] triangles = new int[areas.length][];
        int areaIndex = 0;
        int gappos = 0;
        while (br.ready()) {
            line = br.readLine();
            gappos = line.indexOf(gap);
            if (gappos < 0) {
                continue;
            }
            area = line.substring(0, gappos);
            //check if this area is in the list of interesting areas
            foundname = false;
            areaIndex = 0;
            for (String a : areas) {
                if (area.equals(a)) {
                    foundname = true;
                    break;
                }
                areaIndex++;
            }
            if (foundname) {
                splits = line.split(gap);
                if (splits.length > 1) {
                    int[] triangleIDs = new int[splits.length - 1];
                    for (int i = 0; i < splits.length - 1; i++) {
                        triangleIDs[i] = Integer.parseInt(splits[i + 1]);
                    }
                    triangles[areaIndex] = triangleIDs;
                } else {
                    triangles[areaIndex] = new int[0];
                }
                counterfound++;
                if (counterfound >= areas.length) {
                    return triangles;
                }
            }
        }
        br.close();
        fr.close();
        return triangles;
    }

    public static HashMap<String, int[]> loadHystemDatTriangles(File hystemdatfile) throws FileNotFoundException, IOException {
        if (!hystemdatfile.exists()) {
            return null;
        }
        FileReader fr = new FileReader(hystemdatfile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        String[] splits;
        int counterfound = 0;
        String areaname;
        String gap = " ";
        boolean foundname;

        HashMap<String, int[]> map = new HashMap<>();
        int areaIndex = 0;
        int gappos = 0;
        while (br.ready()) {
            line = br.readLine();
            gappos = line.indexOf(gap);
            if (gappos < 0) {
                continue;
            }
            areaname = line.substring(0, gappos);
            //check if this area is in the list of interesting areas
            foundname = false;
            areaIndex = 0;

            splits = line.split(gap);
            if (splits.length > 1) {
                int[] triangleIDs = new int[splits.length - 1];
                for (int i = 0; i < splits.length - 1; i++) {
                    triangleIDs[i] = Integer.parseInt(splits[i + 1]);
                }
                map.put(areaname, triangleIDs);
            } else {
                map.put(areaname, new int[0]);
            }
            counterfound++;

        }
        br.close();
        fr.close();
        return map;
    }

}
