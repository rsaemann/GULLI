/*
 * The MIT License
 *
 * Copyright 2022 robert sämann.
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
package com.saemann.gulli.core.io;

import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.SurfaceVelocityLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads Geometry and Velocity field from Raster based CSV Files and creates a
 * triangle-cell-based surface. Creates in 2022 to read velocity fields from
 * camera based recordings.
 *
 * @author robert sämann
 */
public class Surface_CSV_IO implements SurfaceVelocityLoader {

    public static Surface createTriangleSurfaceGeometry(File f) throws Exception {
        float[][][] coordinates = null;
        try (FileReader fr = new FileReader(f)) {
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();//Header
            if (!line.contains("HEADER")) {
                System.err.println("First line does not contain 'Header' key.");
                System.out.println("First line: '" + line + "'");
                throw new Exception("First line does not include 'HEADER'. This is not the correct fileformat for raster CSV files.");
            }

            while (br.ready()) {
                line = br.readLine();
                if (line.contains("*DATA*")) {
                    break;
                }
                if (line.contains("Version")) {

                }
                if (line.contains("GridSize")) {
                    int width = Integer.parseInt(line.substring(line.indexOf("dth=") + 4, line.indexOf(",")));
                    int height = Integer.parseInt(line.substring(line.indexOf("ght=") + 4, line.indexOf("}")));
                    coordinates = new float[width][height][2];
                }
            }
            if (coordinates == null) {
                throw new Exception("No width and height information of the raster given. Abort creation of surface raster.");
            }
            //Data section
            line = br.readLine();// Columnames
            int indexXcoord = 4;
            int indexYcoord = 5;
            int indexU = 8; //Velocity in x direction (m/s)
            int indexV = 7; //Velocity in y direction (m/s)

            //Create Vertices
            float xfactor = 1;
            float yfactor = 1;

            String[] parts;
            while (br.ready()) {
                line = br.readLine();
                parts = line.split(",");

                int ix = Integer.parseInt(parts[0]);
                int iy = Integer.parseInt(parts[1]);

                float x = Float.parseFloat(parts[indexXcoord]);
                float y = Float.parseFloat(parts[indexYcoord]);

                coordinates[ix][iy][0] = x * xfactor;
                coordinates[ix][iy][1] = y * yfactor;
            }

            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (coordinates == null) {
            throw new Exception("No width and height information of the raster given. Abort creation of surface raster.");
        }
        //Transform to required data types
        double[][] vertices = new double[(coordinates.length + 1) * (coordinates[0].length + 1)][3];
        int[][] triangleNodes = new int[coordinates.length * coordinates[0].length * 2][3];
        int[][] neighbours = new int[triangleNodes.length][3];

        //Vertices
        int iv;
        for (int ix = 0; ix < coordinates.length; ix++) {
            for (int iy = 0; iy < coordinates[0].length; iy++) {
                iv = ix * coordinates[0].length + iy;
                vertices[iv][0] = coordinates[ix][iy][0];
                vertices[iv][1] = coordinates[ix][iy][1];
                vertices[iv][2] = 0;
//                System.out.println("x="+ix+", y="+iy+" \t#"+iv+" : ("+vertices[iv][0]+",\t "+vertices[iv][1]+" ,\t "+vertices[iv][2] +"') " );

            }
        }
        //Triangles
        int it, iVll;
        for (int ix = 0; ix < coordinates.length - 1; ix++) {
            for (int iy = 0; iy < coordinates[0].length - 1; iy++) {
                iVll = ix * coordinates[0].length + iy;
                it = (ix * (coordinates[0].length - 1) + iy) * 2;
                triangleNodes[it][0] = iVll;
                triangleNodes[it][1] = iVll + 1;
                triangleNodes[it][2] = iVll + 1 + coordinates[0].length;

                triangleNodes[it + 1][0] = iVll;
                triangleNodes[it + 1][1] = iVll + coordinates[0].length + 1;
                triangleNodes[it + 1][2] = iVll + coordinates[0].length;

                //Neighbours
                neighbours[it][0] = it + 3;
                neighbours[it][1] = it + 1;
                neighbours[it][2] = it - 2 * (coordinates[0].length - 1) + 1;

                neighbours[it + 1][0] = it;// - 2 * (coordinates[0].length - 1)+1 ;//
                neighbours[it + 1][1] = it - 2;
                neighbours[it + 1][2] = it+ 2 * (coordinates[0].length - 1);

//                System.out.println("x="+ix+", y="+iy+" \t#"+it+" : v0="+triangleNodes[it][0]+", v1="+triangleNodes[it][1]+" , v2="+triangleNodes[it][2] +"\t " );
            }
        }
        //TriangleMids
        double[][] triangleMids = new double[triangleNodes.length][3];
        for (int t = 0; t < triangleNodes.length; t++) {
            for (int v = 0; v < 3; v++) {
                for (int d = 0; d < 3; d++) {
                    triangleMids[t][d] += vertices[triangleNodes[t][v]][d] * 0.3333;
                }

            }

        }

        Surface surf = new Surface(vertices, triangleNodes, neighbours, null, "EPSG:3857");
        surf.setTriangleMids(triangleMids);
        return surf;
    }

    /**
     *
     * @param f
     * @return [cellindex][timeindex][0=x;1=y] m/s
     * @throws Exception
     */
    public static float[][][] loadVelocityField(File f) throws Exception {
        float[][][] velocity = null;
        int width = 0;
        int height = 0;
        //COunt number of Timestamps
        int timestamps = 0;
        try (FileReader fr = new FileReader(f)) {
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();//Header
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("TimeStamp:")) {
                    timestamps++;
                }
            }
            br.close();
            fr.close();
        }
        boolean onlyOneTimestep = false;
        if (timestamps < 2) {
            onlyOneTimestep = true;
            timestamps = 2;
        }
        //Read grid size 
        try (FileReader fr = new FileReader(f)) {
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();//Header
            if (!line.contains("HEADER")) {
                System.err.println("First line does not contain 'Header' key.");
                System.out.println("First line: '" + line + "'");
                throw new Exception("First line does not include 'HEADER'. This is not the correct fileformat for raster CSV files.");
            }

            while (br.ready()) {
                line = br.readLine();
                if (line.contains("*DATA*")) {
                    break;
                }
                if (line.contains("Version")) {

                }
                if (line.contains("GridSize")) {
                    width = Integer.parseInt(line.substring(line.indexOf("dth=") + 4, line.indexOf(",")));
                    height = Integer.parseInt(line.substring(line.indexOf("ght=") + 4, line.indexOf("}")));
                    velocity = new float[width * height * 2][timestamps][2];
                }
            }
            if (velocity == null) {
                throw new Exception("No width and height information of the raster given. Abort creation of surface raster.");
            }
            //Data section
            line = br.readLine();// Columnames
            int indexXcoord = 4;
            int indexYcoord = 5;
            int indexU = 8; //Velocity in x direction (m/s)
            int indexV = 7; //Velocity in y direction (m/s)

            //Create Vertices
            float xfactor = 1;
            float yfactor = 1;

            String[] parts;
            int timestep = 0;
            int iT = -1;
            while (br.ready()) {
                line = br.readLine();
                parts = line.split(",");

                int ix = Integer.parseInt(parts[0]);
                int iy = Integer.parseInt(parts[1]);

                float vx = Float.parseFloat(parts[indexU]);
                float vy = Float.parseFloat(parts[indexV]);

                try {
                    iT = (ix * height + iy) * 2;
                    velocity[iT][timestep][0] = vx;
                    velocity[iT][timestep][1] = vy;
                    velocity[iT + 1][timestep][0] = vx;
                    velocity[iT + 1][timestep][1] = vy;
//                    System.out.println("#"+iT +" V=("+vx+", "+vy+")");
                } catch (Exception e) {

                    System.err.println("Index: " + iT + "   max: " + velocity[0].length);
                }

            }

            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (onlyOneTimestep) {
            //Create a second timestep to enable a scenario
            //System.out.println("create a second timestep with identical values as first timestep");
            for (int i = 0; i < velocity.length; i++) {
                velocity[i][1] = velocity[i][0];

            }
        }

        return velocity;
    }

    @Override
    public float[][] loadVelocity(int triangleID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void readSurfaceCellVelocities(Surface surf, File csvFile) throws Exception {
        surf.setTriangleVelocity(loadVelocityField(csvFile));        
    }

    /**
     * Returns true if the file contains a csv scheme, which is readle by this
     * class for camera based raster information orginally created by the CoUD
     * Project
     *
     * @param f
     * @return true if this class cn handle the file data.
     */
    public static boolean is_readable_scheme(File f) {
        boolean hasheader = false;
        boolean hasdata = false;
        boolean hasGridSize = false;
        boolean hasVersion = false;

        try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
            String line = br.readLine();//Header
            if (line.contains("HEADER")) {
                hasheader = true;
            }

            if (!hasheader) {
                //Firs line should be >>>*HEADER*<<< otherwise it is not the correct csv scheme for this reader class
                return false;
            }

            int linecounter = 0;
            while (br.ready()) {
                line = br.readLine();
                linecounter++;
                if (line.contains("*DATA*")) {
                    hasdata = true;
                    break;
                }
                if (line.contains("Version")) {
                    hasVersion = true;
                }
                if (line.contains("GridSize")) {
                    hasGridSize = true;
                }
                if (linecounter > 20) {
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hasheader && hasGridSize && hasVersion && hasdata;
    }

    public static void main(String[] args) {
        try {
            File f = new File("D:/CoUD/Test 1 16X16.csv");
            Surface surf = createTriangleSurfaceGeometry(f);

            float[][][] v = loadVelocityField(f);
            surf.setTriangleVelocity(v);
        } catch (Exception ex) {
            Logger.getLogger(Surface_CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
