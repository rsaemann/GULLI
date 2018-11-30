/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package test.model.surface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saemann
 */
public class TriangleToRectangleMap {

    /**
     * Triangles of interest.
     */
    private int[] triangleIDs;
    /**
     * Maps triangleid [key] to the rectangle id where the triangle is contained
     * mostly.
     */
    int[] triangleToRectangle;

    /**
     * Z (ground) height of the Rectangle.
     */
    float[] rectangleHeight;

    private float[] heightTriangle;
    private int[] rectangleID;
    private float[] maxweight;
    private float[] zdiffTriangleToRect;

    public TriangleToRectangleMap(int[] triangleIDs) {
        this.triangleIDs = triangleIDs;
        Arrays.sort(triangleIDs);
    }

    public TriangleToRectangleMap(File fileTrianglesOfInterest) {
        this(readTriangleIDs(fileTrianglesOfInterest));
    }

    /**
     * Read Ids of Triangles that shall be used for comparing the waterlevels
     *
     * @param file
     * @return
     */
    public static int[] readTriangleIDs(File file) {
        LinkedList<Integer> list = new LinkedList<>();
        //Skip first line
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            //Skip first line
            br.readLine();
            String line;
            while (br.ready()) {
                line = br.readLine();
                list.add(Integer.parseInt(line.split(",")[0]));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TriangleToRectangleMap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TriangleToRectangleMap.class.getName()).log(Level.SEVERE, null, ex);
        }

        int[] array = new int[list.size()];
        int i = 0;
        for (Integer id : list) {
            array[i] = id;
            i++;
        }
        return array;
    }

    /**
     * Load the Height-Z-Component of the rectangle corresponding to the
     * TriangleID
     *
     * @param triangleIDs IDs of triangles of interest
     * @param weightsFile containing ID_rectangle,ID_triangle, weight, altitude
     * @param rectangleHeight contains ID_rectangle,latitude,longitude,altitude
     * @throws java.io.FileNotFoundException
     */
    public void loadTrianglesRectangleZ(File weightsFile, File rectangleHeight) throws FileNotFoundException, IOException {
        heightTriangle = new float[triangleIDs.length];
        rectangleID = new int[triangleIDs.length];
        maxweight = new float[triangleIDs.length];
        BufferedReader br = new BufferedReader(new FileReader(weightsFile));
        String[] split;
        br.readLine();//skip header

        while (br.ready()) {
            split = br.readLine().split(",");
            //Decode triangle ID
            int tID = Integer.parseInt(split[1]);
            //Is traingleID in requested IDs?
            for (int i = 0; i < triangleIDs.length; i++) {
                if (triangleIDs[i] == tID) {
                    // found ID, test, if this weight is higher than a previous found weight
                    float w = Float.parseFloat(split[2]);
                    if (maxweight[i] < w) {
                        //Use this rectangle
                        rectangleID[i] = Integer.parseInt(split[0]);
                        maxweight[i] = w;
                        heightTriangle[i] = Float.parseFloat(split[3]);
                    }
                    break; //DO not check other (higher) IDs.
                }
            }
        }
        br.close();

        //Read rectangle Height
        br = new BufferedReader(new FileReader(rectangleHeight));
        br.readLine();//Skip header
        this.rectangleHeight = new float[triangleIDs.length];
        this.zdiffTriangleToRect = new float[triangleIDs.length];
        while (br.ready()) {
            split = br.readLine().split(",");
            //read rectangle_id
            int IDr = Integer.parseInt(split[0]);
            //Search for this ID in array
            for (int i = 0; i < rectangleID.length; i++) {
                if (rectangleID[i] == IDr) {
                    this.rectangleHeight[i] = Float.parseFloat(split[3]);
                    this.zdiffTriangleToRect[i] = this.rectangleHeight[i] - this.heightTriangle[i];
                }

            }
        }
        br.close();
    }

    public static void main(String[] args) {

        System.out.println("load Ids of interest");
        long start = System.currentTimeMillis();
        int[] tris = TriangleToRectangleMap.readTriangleIDs(new File("C:\\Users\\saemann\\Documents\\NetBeansProjects\\GULLI\\input\\suzansReferenzPunkte.txt"));
        System.out.println("  took " + ((System.currentTimeMillis() - start)) + "ms.");
        TriangleToRectangleMap map = new TriangleToRectangleMap(tris);
        try {
            System.out.println("Load RectangleZ");
            start = System.currentTimeMillis();
            map.loadTrianglesRectangleZ(new File("Y:\\RobertS\\vonSimon\\Neuer Ordner\\weights.csv"), new File("Y:\\RobertS\\vonSimon\\Neuer Ordner\\Koordinaten.csv"));
            System.out.println("  took " + ((System.currentTimeMillis() - start)) + "ms.");
//
//            for (int i = 0; i < tris.length; i++) {
//                System.out.println(tris[i] + ": " + map.rectangleID[i] + "\tw:" + map.maxweight[i] + "\ttz:" + map.heightTriangle[i] + "\trz:" + map.rectangleHeight[i]);
//
//            }
            System.out.println("Calculate Waterlevels on triangles.");
            start = System.currentTimeMillis();
            float[] wl = map.getTriangleWaterlevel(new File("Y:\\RobertS\\vonSimon\\t2.csv"));
            for (int i = 0; i < wl.length; i++) {
                System.out.println(i + ": " + map.triangleIDs[i] + " in " + map.rectangleID[i] + "\twl: " + wl[i]);

            }
            System.out.println("  took " + ((System.currentTimeMillis() - start)) + "ms.");
        } catch (IOException ex) {
            Logger.getLogger(TriangleToRectangleMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns the waterlevel on triangles of interest, calculated from the
     * waterlevel on rectangles in the given file. Index of waterlevel
     * corresponds to index of triangle in {triangleIDs}.
     *
     * @param fileRectangleWaterlevel RectID, mean waterlevel On rectangle
     * @return waterlevel on triangles of interest.
     */
    public float[] getTriangleWaterlevel(File fileRectangleWaterlevel) throws FileNotFoundException, IOException {
        float[] wl = new float[this.triangleIDs.length];
        BufferedReader br = new BufferedReader(new FileReader(fileRectangleWaterlevel));
        String line = br.readLine();//Skip header;
        String[] split;
        while (br.ready()) {
            split = br.readLine().split(",");
            int rectID = Integer.parseInt(split[0]);
            //Search in rectIds to find corresponding index
            for (int i = 0; i < this.rectangleID.length; i++) {
                if (rectID == this.rectangleID[i]) {
                    wl[i] = rectangleHeight[i] + Float.parseFloat(split[1]) - heightTriangle[i];
                }
            }

        }
        br.close();
        return wl;
    }

}
