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
package com.saemann.gulli.core.io.extran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.java.io.zrz.jgdb.FileGDBFactory;
import main.java.io.zrz.jgdb.GeoDB;
import main.java.io.zrz.jgdb.GeoFeature;
import main.java.io.zrz.jgdb.GeoField;
import main.java.io.zrz.jgdb.GeoFieldValue;
import main.java.io.zrz.jgdb.GeoLayer;
import main.java.io.zrz.jgdb.GeoTable;
import main.java.io.zrz.jgdb.shape.GeometryValue;
import main.java.io.zrz.jgdb.shape.MultiPoint;
import main.java.io.zrz.jgdb.shape.Point;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.SurfaceVelocityLoader;
import com.saemann.gulli.core.model.surface.SurfaceWaterlevelLoader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Read from GDB files. Pure native JAVA.
 *
 * needs rGDB.jar extended version of Theo Zourzouvillys' git project
 *
 * @author saemann
 */
public class HE_GDB_IO implements SurfaceWaterlevelLoader, SurfaceVelocityLoader {

    public static boolean verbose = false;

    public boolean opened = false;
    public boolean analysed = false;

    private File directory;

    /**
     * Represent the layer name of interesting content. null if not found.
     */
    private String layerWaterHeight = null, layerVelocity = null;
    private String layerSpatialReference = null;

    private boolean layerVelocityUnreadable = false;

    /**
     * In new versions, there is no x/y velocity component. Then, we need to
     * calculate the X/Y components from the velocity intensity and the
     * direction.
     */
    private boolean velocities_directed_only = false;

    /**
     * Number of timesteps for velocity & waterheight output; -1 if not found.
     */
    private int velocityTimeSteps = -1, waterheightTimeSteps = -1;

    /**
     * Index of column containing the value of maximum Velocity. -1 if not found
     */
    private int indexVMax = -1;

    private int indexWLmax = -1;

    private int indexVX0 = -1, indexVY0 = -1, indexWL0 = -1, indexWLZ = -1, indexVX1 = -1;

    private int indexV_0 = -1, indexV_DIR_0 = -1;

    private int indexVid = -1, indexWLid = -1;

    private int numberOftriangles = -1;
    private long maxTriangleID = -1;

    private double degtoRad = Math.PI / 180.;

    private boolean resultDB = false, modelDB = false;

    private GeoDB db;

    public static int counterGetFeature = 0;

    public static long sqlRequestTime = 0;
    public static int sqlRequestCount = 0;

    public static long waitingForRequestTime = 0;
    public static int waitingForRequestCount = 0;

    private String spatialReferenceProjection = null;

    private GeoLayer layerWaterlevels;
    private GeoLayer layerVelocities;

    final private ArrayList<GDBHandle> velocityHandles = new ArrayList<>(8);
    final private ArrayList<GDBHandle> waterlevelHandles = new ArrayList<>(8);

    public static boolean isReadyInstalled() {
        try {
            FileGDBFactory f = new FileGDBFactory();
            return true;
        } catch (Exception e) {
            return false;
        }

    }
    /**
     * Number of maximum attempts to find a specific ID.
     */
    public int maxTryFindCount = 200;

    /**
     * A single Handle is used for parallel access on a gdb table file
     */
    public class GDBHandle {

        boolean busy = false;
        int requestedID = -1;
        GeoTable table;
        Thread th = null;

        public GDBHandle(GeoTable table) {
            this.table = table;
        }

    }

    public HE_GDB_IO(File directory) {
//        GeoDirectFile.verbose=true;
//        GeoTable.verbose=true;
        this.directory = directory;
        this.db = FileGDBFactory.open(this.directory.toPath());
        opened = true;
        analyseHEDatabase();
        analysed = true;
//        System.out.println(getClass() + "::created " + directory.getParentFile().getName() + " velocity timesteps:" + velocityTimeSteps + "   wlTimesteps:" + waterheightTimeSteps);
    }

    @Override
    public String toString() {
        return getClass() + "{" + directory.getParentFile().getName() + " velocity_timesteps:" + velocityTimeSteps + "   wlTimesteps:" + waterheightTimeSteps + ",@ " + directory.getAbsolutePath() + "}";
    }

    /**
     * Find content of waterlevel and velocity if existent and sets needed
     * column indexes for faster access.
     */
    private void analyseHEDatabase() {
        for (String layer : db.getLayers()) {
            if (layer.matches("Topo_decimated")) {
                layerWaterHeight = layer;
                this.resultDB = true;
            } else if (layer.equals("Velocity")) {
                layerVelocity = layer;
                this.resultDB = true;
            } else if (layer.equals("Topo")) {
                this.modelDB = true;
            } else if (layer.equals("GDB_SpatialRefs")) {
                this.layerSpatialReference = layer;
            }
        }
        //Read spatial projection 
        if (layerSpatialReference != null) {
            GeoLayer l = db.layer(layerSpatialReference);
            for (int i = 1; i <= l.getFeatureCount(); i++) {
                GeoFeature f = l.getFeature(i);
                //Hope to find something like "PROJCS["ETRS_1989_UTM_Zone_32N",GEOGCS["GCS_ETRS_1989",DATUM["D_ETRS_1989",SPHEROID["GRS_1980",6378137.0,298.257222101]],..."
                String identificationString = f.getValue(0).toString();
//                System.out.println(f);
                if (identificationString.startsWith("PROJ")) {
                    this.spatialReferenceProjection = identificationString;//.substring(identificationString.indexOf("\"") + 1, identificationString.indexOf(",") - 1);
                }
            }
        }

        // Layers found
        // Analyse timesteps
        // 1) waterlevels
        if (layerWaterHeight != null) {
            this.resultDB = true;
            layerWaterlevels = db.layer(layerWaterHeight);
            int i = 0;
            for (GeoField field : layerWaterlevels.getFields()) {
                String name = field.getName();
                if (name.equals("WL_0")) {
                    indexWL0 = i;
                } else if (name.equals("ID")) {
                    indexWLid = i;
                } else if (name.equals("Z")) {
                    indexWLZ = i;
                } else if (name.equals("WLevelMax")) {
                    indexWLmax = i;
                }
                if (name.startsWith("WL_")) {
                    try {
                        int number = Integer.parseInt(name.substring(3));
                        waterheightTimeSteps = Math.max(waterheightTimeSteps, number + 1);
                    } catch (NumberFormatException numberFormatException) {
                    }
                }
                i++;
            }
            numberOftriangles = layerWaterlevels.getFeatureCount();
            if (layerWaterlevels.getFeatureCount() < 1) {
                //No water levels. 
                this.layerWaterHeight = null;
            } else {
                GeoFeature maxfeature = layerWaterlevels.getFeature(layerWaterlevels.getFeatureCount());
                if (maxfeature == null) {
                    System.out.println(getClass() + ":: can not get maxfeature " + layerWaterlevels.getFeatureCount());
                    maxfeature = layerWaterlevels.getFeature(layerWaterlevels.getFeatureCount());
                    System.out.println("use " + (layerWaterlevels.getFeatureCount()) + " : " + maxfeature);
                }
                GeoFieldValue mavalue = maxfeature.getValue(indexWLid);
                maxTriangleID = mavalue.intValue();
            }
        }
        // 2) velocities
        if (layerVelocity != null) {
            try {
                resultDB = true;
                GeoLayer layer = db.layer(layerVelocity);
                layerVelocities = layer;
//            System.out.println("Layer velocities: "+layerVelocities.getClass());
                int numberDirectionVelocity = -1;
                int numberComponentVelocity = -1;
                int i = 0;
                for (GeoField field : layer.getFields()) {
                    String name = field.getName();
//                    System.out.println("Field " + i + ": " + name);
                    if (name == null) {
                        continue;
                    }
                    //Find indices for fast access
                    if (name.equals("V_Max")) {
                        indexVMax = i;
                    } else if (name.equals("ID")) {
                        indexVid = i;
                    } else if (name.equals("V_X_0")) {
                        indexVX0 = i;
                    } else if (name.equals("V_X_1")) {
                        indexVX1 = i;
                    } else if (name.equals("V_Y_0")) {
                        indexVY0 = i;
                    } else if (name.equals("V_0")) {
                        indexV_0 = i;
                    } else if (name.equals("V_DIR_0")) {
                        indexV_DIR_0 = i;
                    }

                    if (name.startsWith("V_X_")) {
                        try {
                            int number = Integer.parseInt(name.substring(4)) + 1;
                            numberComponentVelocity = Math.max(numberComponentVelocity, number);
                        } catch (NumberFormatException numberFormatException) {
                            numberFormatException.printStackTrace();
                        }
                    }
                    if (name.startsWith("V_DIR_")) {
                        try {
                            int number = Integer.parseInt(name.substring(6)) + 1;
                            numberDirectionVelocity = Math.max(numberDirectionVelocity, number);
                        } catch (NumberFormatException numberFormatException) {
                            numberFormatException.printStackTrace();
                        }
                    }
                    i++;
                }
//                System.out.println("Velocities XY:" + numberComponentVelocity + "   Dirs:" + numberDirectionVelocity);
                if (numberComponentVelocity < 1 && numberDirectionVelocity > 0) {
                    velocities_directed_only = true;
                    velocityTimeSteps = numberDirectionVelocity;
                } else {
                    velocityTimeSteps = numberComponentVelocity;
                }

//                System.out.println("directed velocities only? " + velocities_directed_only);

                int numberOfFeatures = layer.getFeatureCount();
                if (maxTriangleID <= 0) {
                    maxTriangleID = layer.getFeature(layer.getFeatureCount()).getValue(indexVid).intValue();
//                System.out.println("velocity maxID="+maxTriangleID);
                }

                if (this.numberOftriangles > 0 && this.numberOftriangles != numberOfFeatures) {
                    System.err.println("Number of Triangles in Waterlevels (" + this.numberOftriangles + ") is not equal to Number in Velocities (" + numberOfFeatures + ").");
                } else {
                    this.numberOftriangles = numberOfFeatures;
                }
            } catch (Exception ex) {
                if (ex.getLocalizedMessage().contains("Only support version 9 or 10")) {
                    layerVelocityUnreadable = true;
                }
            }
        }

    }

    /**
     * The Projection of coordinates. Does not contain an EPSG Code :-(
     *
     * @return
     */
    public String getSpatialReferenceProjection() {
        return spatialReferenceProjection;
    }

    /**
     * Close reader lock on all tablefiles. Strongly recommended to be used
     * after finishing reading of file.
     */
    public void close() {
        db.close();
    }

    /**
     * Does the database contain information about velocities on triangles.
     *
     * @return
     */
    public boolean hasVelocities() {
        return layerVelocity != null;
    }

    /**
     * Decoded, very large files cannot be read directly with this server.
     * Instead, use the GdalIO class to extract and read the information.
     *
     * @return true if the velocities can be read in with this class, false if a
     * prior extraction is necessary.
     */
    public boolean isLayerVelocityDirectlyReadable() {
        //This is checked during the analyseDatabase step.
        return !layerVelocityUnreadable;
    }

    /**
     * Does the database has a table with waterlevels on triangles.
     *
     * @return
     */
    public boolean hasWaterlevels() {
        return layerWaterHeight != null;
    }

    public int getNumberOfWaterlevelTimeSteps() {
        return waterheightTimeSteps;
    }

    public int getNumberOfVelocityTimeSteps() {
        return velocityTimeSteps;
    }

    public GeoDB getDb() {
        return db;
    }

    public String getLayerWaterHeight() {
        return layerWaterHeight;
    }

    public String getLayerVelocity() {
        return layerVelocity;
    }

    /**
     * Reads the waterlevels for all triangles for the given timeindex (starting
     * with 0).
     *
     * @param timeindex
     * @return
     */
    public double[] readWaterlevels(int timeindex) {
        if (layerWaterHeight == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Waterlevel information.");
        }
        if (maxTriangleID >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Max triangle ID " + maxTriangleID + " is greater than Inetger.maxvalue. -> Cannot create array for all triangles.");
        }
        if (timeindex >= waterheightTimeSteps) {
            throw new IllegalArgumentException("Requested timeindex " + timeindex + " cannot be found in waterlevel layer(size:" + waterheightTimeSteps + ").");
        }
        //Normal Surface with all triangles.
        //initialize waterlevel history storage
        double[] wl = new double[(int) (maxTriangleID + 1)];
        GeoLayer layer = db.layer(layerWaterHeight);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexWLid).intValue();
            if (id >= wl.length) {
                throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (wl.length - 1) + ")");
            }
//            wlmax[id] = f.getValue(indexWLmax).doubleValue();
            wl[id] = f.getValue(indexWL0 + timeindex).doubleValue();
        }

        return wl;
    }

    public double[] readWaterheight(int timeindex) {
        if (layerWaterHeight == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Waterlevel information.");
        }
        if (maxTriangleID >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Max triangle ID " + maxTriangleID + " is greater than Inetger.maxvalue. -> Cannot create array for all triangles.");
        }
        if (timeindex >= waterheightTimeSteps) {
            throw new IllegalArgumentException("Requested timeindex " + timeindex + " cannot be found in waterlevel layer(size:" + waterheightTimeSteps + ").");
        }
        //Normal Surface with all triangles.
        //initialize waterlevel history storage
        double[] h = new double[(int) (maxTriangleID + 1)];
//        double[] wlmax = new double[wl.length];
        GeoLayer layer = db.layer(layerWaterHeight);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexWLid).intValue();
            if (id >= h.length) {
                throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (h.length - 1) + ")");
            }
//            wlmax[id] = f.getValue(indexWLmax).doubleValue();
            double znew = f.getValue(indexWLZ).doubleValue();
            h[id] = f.getValue(indexWL0 + timeindex).doubleValue() + znew;
        }

        return h;
    }

    public double[] readWaterheightMaximum() {
        if (layerWaterHeight == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Waterlevel information.");
        }
        if (maxTriangleID >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Max triangle ID " + maxTriangleID + " is greater than Inetger.maxvalue. -> Cannot create array for all triangles.");
        }

        //Normal Surface with all triangles.
        //initialize waterlevel history storage
        double[] h = new double[(int) (maxTriangleID + 1)];
//        double[] wlmax = new double[wl.length];
        GeoLayer layer = db.layer(layerWaterHeight);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexWLid).intValue();
            if (id >= h.length) {
                throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (h.length - 1) + ")");
            }
//            wlmax[id] = f.getValue(indexWLmax).doubleValue();
            double znew = f.getValue(indexWLZ).doubleValue();
            h[id] = f.getValue(indexWLmax).doubleValue() + znew;
        }

        return h;
    }

    /**
     * Read the maximum waterlevel (from sole) of all triangles ordered by their
     * triangle ID (not database reference id). Returned array therefore might
     * contain 0 also if no triangle with that id exists.
     *
     * @return
     */
    public double[] readWaterLevelMaximum() {
        if (layerWaterHeight == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Waterlevel information.");
        }
        if (maxTriangleID >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Max triangle ID " + maxTriangleID + " is greater than Inetger.maxvalue. -> Cannot create array for all triangles.");
        }

        //Normal Surface with all triangles.
        //initialize waterlevel history storage
        double[] h = new double[(int) (maxTriangleID + 1)];
        GeoLayer layer = db.layer(layerWaterHeight);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexWLid).intValue();
            if (id >= h.length) {
                throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (h.length - 1) + ")");
            }
            h[id] = f.getValue(indexWLmax).doubleValue();
        }

        return h;
    }

    /**
     * Read the maximum velocity of all triangles ordered by their triangle ID
     * (not database reference id). Returned array therefore might contain 0
     * also if no triangle with that id exists.
     *
     * @return
     */
    public double[] readVelocityMaximum() {
        if (layerVelocity == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Velocity information.");
        }
        if (maxTriangleID >= Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Max triangle ID " + maxTriangleID + " is greater than Inetger.maxvalue. -> Cannot create array for all triangles.");
        }

        //Normal Surface with all triangles.
        //initialize waterlevel history storage
        double[] h = new double[(int) (maxTriangleID + 1)];
        GeoLayer layer = db.layer(layerVelocity);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexVid).intValue();
            if (id >= h.length) {
                throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (h.length - 1) + ")");
            }
            h[id] = f.getValue(indexVMax).doubleValue();
        }

        return h;
    }

    public void applyWaterlevelsToSurface(Surface surf) {
        if (layerWaterHeight == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Waterlevel information.");
        }

        GeoLayer layer = db.layer(layerWaterHeight);
        if (surf.mapIndizes != null && !surf.mapIndizes.isEmpty()) {
            float[][] wl = new float[surf.mapIndizes.size()][waterheightTimeSteps];
            double[] wlmax = new double[surf.mapIndizes.size()];
            for (GeoFeature f : layer) {
                int id = f.getValue(indexWLid).intValue();
                if (surf.mapIndizes.containsKey(id)) {
                    int index = surf.mapIndizes.get(id);
                    for (int i = 0; i < waterheightTimeSteps; i++) {
                        wl[index][i] = (float) f.getValue(indexWL0 + i).doubleValue();
                    }
                    wlmax[index] = f.getValue(indexWLmax).doubleValue();
                    double znew = f.getValue(indexWLZ).doubleValue();
//                    double zold = surf.getTriangleMids()[index][2];
//                    System.out.println("Old z=" + zold + "\tnew:" + znew + "\t diff:" + (zold - znew));
                    surf.getTriangleMids()[index][2] = znew;
                }

            }
            surf.setWaterlevels(wl);
            surf.setMaxWaterLevels(wlmax);

        } else {
            //Normal Surface with all triangles.
            //initialize waterlevel history storage
            float[][] wl = new float[surf.getTriangleNodes().length][waterheightTimeSteps];
            double[] wlmax = new double[surf.getTriangleNodes().length];
            for (GeoFeature f : layer) {
                int id = f.getValue(indexWLid).intValue();
                if (id >= wl.length) {
                    throw new UnsatisfiedLinkError("Id of read id (" + id + ") is higher than max surface.id(" + (wl.length - 1) + ")");
                }
                double znew = f.getValue(indexWLZ).doubleValue();
                double zold = surf.getTriangleMids()[id][2];
//                System.out.println("Old z=" + zold + " \tnew:" + znew + "\t diff:" + (zold - znew));
                surf.getTriangleMids()[id][2] = znew;
                wlmax[id] = f.getValue(indexWLmax).doubleValue();
                for (int j = 0; j < waterheightTimeSteps; j++) {
                    wl[id][j] = (float) f.getValue(indexWL0 + j).doubleValue();
                }
            }
            surf.setWaterlevels(wl);
            surf.setMaxWaterlvl(wlmax);
        }
    }

    /**
     * Reads information about stored velocities and saves it directly to the
     * surface.
     *
     * @param surf
     */
    public void applyVelocitiesToSurface(Surface surf) {
        if (layerVelocity == null) {
            throw new UnsupportedOperationException("GDB '" + directory.getAbsolutePath() + "' does not contain Velocity information.");
        }

        GeoLayer layer = db.layer(layerVelocity);
        int jump = indexVX1 - indexVX0;
//        System.out.println("Vx space="+jump);
        if (surf.mapIndizes != null && !surf.mapIndizes.isEmpty()) {
            float[][][] v = new float[surf.mapIndizes.size()][velocityTimeSteps][2];
            float[] vmax = new float[surf.mapIndizes.size()];
            for (GeoFeature f : layer) {
                int id = f.getValue(indexVid).intValue();
                if (surf.mapIndizes.containsKey(id)) {
                    int index = surf.mapIndizes.get(id);
                    vmax[index] = (float) f.getValue(indexVMax).doubleValue();
                    if (velocities_directed_only) {
                        fillVelocityByDirectedVelocity(v, f, id);
                    } else {
                        for (int t = 0; t < velocityTimeSteps; t++) {
                            v[index][t][0] = (float) f.getValue(indexVX0 + t * jump).doubleValue();
                            v[index][t][1] = (float) f.getValue(indexVY0 + t * jump).doubleValue();
                        }
                    }

                    //Z position not needed for velocities because they do not need to be calculated.
                }

            }
            surf.setTriangleVelocity(v);
            surf.setTriangleMaxVelocity(vmax);
        } else {
            //Normal Surface with all triangles.
            //initialize waterlevel history storage
            int triangleCount = surf.getTriangleNodes().length;
            float[][][] v = new float[triangleCount][velocityTimeSteps][2];
            float[] vmax = new float[surf.getTriangleNodes().length];
            for (GeoFeature f : layer) {
                int id = f.getValue(indexVid).intValue();
                if (id >= triangleCount) {
                    throw new UnsatisfiedLinkError("Id of read v.id (" + id + ") is higher than max surface.id(" + (v[0].length - 1) + ")");
                }
                vmax[id] = (float) f.getValue(indexVMax).doubleValue();
                if (velocities_directed_only) {
                    fillVelocityByDirectedVelocity(v, f, id);
                } else {
                    for (int t = 0; t < velocityTimeSteps; t++) {
                        v[id][t][0] = (float) f.getValue(indexVX0 + t * jump).doubleValue();
                        v[id][t][1] = (float) f.getValue(indexVY0 + t * jump).doubleValue();
                    }
                }
            }
            surf.setTriangleVelocity(v);
            surf.setTriangleMaxVelocity(vmax);
        }
    }

    public void fillVelocityByDirectedVelocity(float[][][] v, GeoFeature f, int id) {
        double v_total = 0;
        double dir1 = 0, dir2 = 0;
        for (int t = 0; t < velocityTimeSteps; t++) {
            v_total = f.getValue(indexV_0 + t * 2).doubleValue();
            dir1 = f.getValue(indexV_DIR_0 + t * 2).doubleValue();
            dir2 = dir1 * degtoRad;
            v[id][t][0] = (float) (Math.cos(dir2) * v_total);
            v[id][t][1] = (float) (Math.sin(dir2) * v_total);

//            System.out.println(dir1+"° v="+v_total+"   -> v="+v[id][t][0]+"    / "+v[id][t][1]);
        }
    }

    public void fillVelocityByDirectedVelocity(float[][] v, GeoFeature f) {
        double v_total = 0;
        double dir1, dir2;
        for (int t = 0; t < velocityTimeSteps; t++) {
            try {
                v_total = f.getValue(indexV_0 + t * 2).doubleValue();
                dir1 = f.getValue(indexV_DIR_0 + t * 2).doubleValue();
                dir2 = dir1 * degtoRad;
                v[t][0] = (float) (Math.cos(dir2) * v_total);
                v[t][1] = (float) (Math.sin(dir2) * v_total);
//                System.out.println(dir1 + "° v=" + v_total + "   -> v=" + v[t][0] + "    / " + v[t][1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param layer
     * @param IDindex column index of the feature where the id can be found
     * @param id
     * @return
     */
    public GeoFeature getFeature(GeoLayer layer, int IDindex, int id) throws IDnotFoundException {

        if (id < 0) {
            //Quatsch, nonsense
            throw new IDnotFoundException(id);
        }
        if (id > maxTriangleID) {
            // requested id can not be in this database.
            throw new IDnotFoundException(id);
        }
        counterGetFeature = 0;
        GeoFieldValue gv = null;
        GeoTable table = (GeoTable) layer;
//        table.open();

        long upperID = maxTriangleID;
        int lowerBound = 0;
        long lowerID = 0;
        int requestedFeatureID = 0;
        int gfID = -1;
        try {
            synchronized (layer) {
                int upperBound = layer.getFeatureCount();
                if (id > layer.getFeatureCount()) {
                    requestedFeatureID = layer.getFeatureCount();
                    upperBound = requestedFeatureID;
                    gv = table.readFeatureValue(requestedFeatureID, IDindex);
                } else {
                    requestedFeatureID = (int) (id * (layer.getFeatureCount() / (float) maxTriangleID));
                    if (requestedFeatureID < 1) {
                        requestedFeatureID = 1;
                    }
                    try {
                        gv = table.readFeatureValue(requestedFeatureID, IDindex);
                    } catch (Exception e) {
                        System.err.println("Requested feature ID: " + requestedFeatureID);
                        e.printStackTrace();
                    }
                }
                gfID = gv.intValue();
                while (gfID != id) {
                    if (gfID > id) {
                        //requested Geofeature lies somewhere BEFORE the current one.
                        upperBound = requestedFeatureID;
                        upperID = gfID;

                        if (upperBound - lowerBound < 1) {// WAS 2!
                            // Bounds touched without finding the right value.GF was not found in this table.
                            throw new IDnotFoundException(id);
                        }

                        // -> pointer go back 
                        int idgap = id - gfID;
                        if (idgap < 200) {
                            requestedFeatureID = (int) (requestedFeatureID + idgap * ((upperBound - lowerBound) / (double) (upperID - lowerID)));
                        } else {
                            requestedFeatureID = (requestedFeatureID + idgap);
                        }
                        if (requestedFeatureID <= lowerBound) {
                            //Seems to be near to the requested id. go in low steps
                            requestedFeatureID = (int) (lowerBound + (upperBound - lowerBound) * 0.3) + 1;
                            if (upperBound - lowerBound < 10) {
                                requestedFeatureID = (int) (lowerBound + 1);
                            }

                        }

                    } else {
                        //requested Geofeature lies somewhere HIGHER/AFTER the current one.
                        lowerBound = requestedFeatureID;
                        lowerID = gfID;
                        if (upperBound - lowerBound < 1) { // WAS 2!
                            // Bounds touched without finding the right value.GF was not found in this table.
                            throw new IDnotFoundException(id);
                        }

                        int idgap = id - gfID;
                        if (idgap > 100) {
                            double add = idgap * ((upperBound - lowerBound) / (double) (upperID - lowerID));
                            requestedFeatureID = (int) (requestedFeatureID + add);
                        } else {
                            requestedFeatureID = (requestedFeatureID + idgap);
                        }
                        if (requestedFeatureID >= upperBound) {
                            //Seems to be near to the requested id. go in low steps
                            requestedFeatureID = (int) (upperBound - (upperBound - lowerBound) * 0.7) - 1;
                            if (upperBound - lowerBound < 10) {
                                requestedFeatureID = (int) (upperBound - 1);
                            }

                        }
                    }
                    if (counterGetFeature >= maxTryFindCount) {
                        //Break if it is too timeconsuming
                        break;
                    }

                    //Request the calculated feature for testing it next loop.
                    gv = table.readFeatureValue(requestedFeatureID, IDindex);
                    if (gv == null) {
                        throw new IDnotFoundException(id);
                    }
                    gfID = gv.intValue();
                    counterGetFeature++;
                }
                if (gv == null || gv.intValue() != id) {
                    throw new IDnotFoundException(id);
                }
                GeoFeature ft = table.getFeature(requestedFeatureID);
//                table.close();
                return ft;
            }
        } catch (IOException ex) {
            throw new IDnotFoundException(id);
        }

    }

    public static void main0(String[] args) {
        File file = new File(".\\2D_Model\\Extr2D_E2D1T50_mBK.result\\Result2D.gdb");
        HE_GDB_IO.verbose = true;
        HE_GDB_IO db = new HE_GDB_IO(file);
        System.out.println("has velocities=" + db.hasVelocities());
        System.out.println("has waterlevel=" + db.hasWaterlevels());
        System.out.println("maxTriangleID=" + db.maxTriangleID);
        System.out.println("numTriangles =" + db.getNumberOfTriangles());

        GeoLayer layerV = db.db.layer(db.layerVelocity);
        int featureID = layerV.getFeatureCount() / 2;
        System.out.println("searching for featureID:" + featureID);
        GeoFeature lastFeature = layerV.getFeature(featureID);
        for (GeoField field : lastFeature.getFields()) {
            System.out.println(field.name + "  " + field.type + ":\t " + lastFeature.getValue(field.name));
        }

        float[][] velocities = db.loadVelocity(596794);
        for (float[] velocity : velocities) {
            System.out.println(velocity[0] + ", " + velocity[1]);
        }
    }

    public static void main1(String[] args) throws IOException {

        File file = new File(".\\2D_Model\\2DModell_10cm_3m2_Mit_BK.model\\Model2D.gdb");//der nicht so gute Fall
        HE_GDB_IO db = new HE_GDB_IO(file);
        System.out.println("Triangles: " + db.getNumberOfTriangles());
        System.out.println("Velocities:" + db.hasVelocities());
        System.out.println("Waterlevel:" + db.hasWaterlevels());
        System.out.println("waterlevels: " + db.waterheightTimeSteps);
        System.out.println("WL ID=" + db.indexWLid);
        System.out.println("WL Z =" + db.indexWLZ);

        //Load reference points
        File pfile = new File(".\\input\\Gesamt_Referenzpunkte_1.txt");
        LinkedList<Integer> list;
        //Skip first line
        try (BufferedReader br = new BufferedReader(new FileReader(pfile))) {
            //Skip first line
            br.readLine();
            String line;
            list = new LinkedList<>();
            while (br.ready()) {
                line = br.readLine();
                list.add(Integer.parseInt(line.split(",")[0]));
            }
        }

        int[] array = new int[list.size()];
        {
            int i = 0;
            for (Integer id : list) {
                array[i] = id;
                i++;
            }
        }
        if (true) {
            int[] indices = array;
            GeoLayer layer = db.getDb().layer(db.layerWaterHeight);
            int indexID = db.getIndexWLid();
            for (int i = 0; i < indices.length; i++) {
                long start = System.currentTimeMillis();
                GeoFeature gf = db.getFeature(layer, indexID, indices[i]);
                if (gf == null) {
                    System.out.println("could not find " + indices[i]);
                    System.out.println("***");
                } else {
                    System.out.println("found " + indices[i] + " = " + gf.getValue(indexID) + " @" + gf.getFeatureId() + "\tafter " + counterGetFeature + " steps (" + ((System.currentTimeMillis() - start) / 1000) + "s)");
                }
            }
            System.exit(0);
        }

        Surface surf = HE_SurfaceIO.loadSurface(new File(".\\2D_Model\\2DModell_10cm_3m2_ohne_BK.model"));
//        System.out.println(" max=" + db.maxTriangleID + "   size:" + db.numberOftriangles);
//        GeoLayer layer = db.getDb().layer(db.layerVelocity);
//        System.out.println("entry: " + layer.getFeature(4000));
//        System.out.println("Tables:");
//        for (String layer : db.getDb().getLayers()) {
//            System.out.println("   " + layer);
//        }
        long start = System.currentTimeMillis();
        db.applyWaterlevelsToSurface(surf);
        System.out.println("Read Waterlvl values of gdb in " + ((System.currentTimeMillis() - start)) + "ms.");
        start = System.currentTimeMillis();
        db.applyVelocitiesToSurface(surf);
        System.out.println("Read Velocity values of gdb in " + ((System.currentTimeMillis() - start)) + "ms.");

        //Vergleiche CSV Import und GDB Import
        if (false) {
            try {
                Surface surfCSV = HE_SurfaceIO.loadSurface(surf.fileTriangles);
                File fileCSV = new File(file.getAbsolutePath().replace("2D.gdb", "2D.csv"));
                start = System.currentTimeMillis();
                CSV_IO.readTriangleWaterlevels(surfCSV, fileCSV);
                System.out.println("Read Waterlevels values in CSV took" + ((System.currentTimeMillis() - start)) + "ms.");
                System.out.println("wlmax length: gdb=" + surf.getMaxWaterLevels().length + "\t csv=" + surfCSV.getMaxWaterLevels().length);
                for (int i = 0; i < surf.getMaxWaterLevels().length; i++) {
                    if (surf.getMaxWaterLevels()[i] != surfCSV.getMaxWaterLevels()[i]) {
                        System.out.println("Difference with maxwaterlevels of triangle " + i + "  gdb: " + surf.getMaxWaterLevels()[i] + "\tcsv:" + surfCSV.getMaxWaterLevels()[i]);
                    }
                }
                System.out.println("Done maxwaterlevels");
                for (int i = 0; i < surf.getWaterlevels().length; i++) {
                    for (int j = 0; j < surf.getWaterlevels()[0].length; j++) {
                        if (surf.getWaterlevels()[i][j] == surfCSV.getWaterlevels()[i][j]) {
//                            System.out.println("Same with waterlevels of triangle " + i + ","+j+ "  gdb: " + surf.getWaterlevels()[i][j] + "\tcsv:" + surfCSV.getWaterlevels()[i][j]);
                        } else {
                            System.out.println("DIFF with waterlevels of triangle " + i + "," + j + "  gdb: " + surf.getWaterlevels()[i][j] + "\tcsv:" + surfCSV.getWaterlevels()[i][j]);

                        }
                    }
                }
                System.out.println("done wl comparison");
                System.out.println("calc velocities");
//                surf.calculateNeighbourVelocitiesFromWaterlevels();
                start = System.currentTimeMillis();
//                surfCSV.calculateNeighbourVelocitiesFromWaterlevels();
                System.out.println("Calculating csv velocities took " + (System.currentTimeMillis() - start) + "ms.");
                for (int i = 0; i < surf.getTriangleNodes().length; i++) {
                    for (int j = 0; j < 3; j++) {
                        for (int t = 0; t < surf.getTriangleVelocity().length; t++) {

                            if (surf.getVelocityToNeighbour(i, j) == surfCSV.getVelocityToNeighbour(i, j)) {
//                            System.out.println("Same with velocities of triangle " + i + ","+j+ "  gdb: " + surf.getWaterlevels()[i][j] + "\tcsv:" + surfCSV.getWaterlevels()[i][j]);
                            } else {
                                System.out.println("DIFF with velocities of triangle " + i + "," + j + "  gdb: " + surf.getVelocityToNeighbour(i, j) + "\tcsv:" + surfCSV.getVelocityToNeighbour(i, j));
                            }

                            float vgdb = surf.calcNeighbourVelocityFromTriangleVelocity(t, i, j);
                            float vcsv = surfCSV.getVelocityToNeighbour(t, i, j);

                            if (vgdb != vcsv) {
                                System.out.println(t + "," + i + "," + j + "\tgdb= " + vgdb + "\t== csv=" + vcsv + "\t diff: " + Math.abs(vgdb - vcsv) + "\t" + Math.abs(vgdb - vcsv) / vgdb);
                            } else {
                                System.out.println(t + "," + i + "," + j + "\tgdb= " + vgdb + "\t<> csv=" + vcsv + "\t diff: " + Math.abs(vgdb - vcsv) + "\t" + Math.abs(vgdb - vcsv) / vgdb);
                            }
                        }
                    }
                    if (i > 1000) {
                        break;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(HE_GDB_IO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        db.close();
    }

    public int getNumberOfTriangles() {
        return numberOftriangles;
    }

    public long getMaxTriangleID() {
        return maxTriangleID;
    }

    /**
     * Is this database holding topology information about a HE2D surface. Such
     * surface information is not applicable to load the surface, becaue no
     * neighbour information is contained in this database.
     *
     * @return
     */
    public boolean isModelDB() {
        return modelDB;
    }

    /**
     * Is this database storing results of a HE2D simulation?
     *
     * @return
     */
    public boolean isResultDB() {
        return resultDB;
    }

    public int getIndexWLid() {
        return indexWLid;
    }

    public int getIndexWLmax() {
        return indexWLmax;
    }

    public int getIndexWLZ() {
        return indexWLZ;
    }

    public int getIndexWL0() {
        return indexWL0;
    }

    public HashMap<Integer, Long> getWaterlevelFeatureIDs(int[] triangleIDs) {
        HashMap<Integer, Long> map = new HashMap<>(triangleIDs.length);
        GeoLayer layer = db.layer(layerWaterHeight);
        for (GeoFeature f : layer) {
            int id = f.getValue(indexWLid).intValue();
            for (int tid : triangleIDs) {
                if (id == tid) {
                    map.put(tid, f.getFeatureId());
                    break;
                }
            }
        }
        return map;
    }

    /**
     *
     * @param triangleID
     * @return
     */
    @Override
    public float[] loadWaterlevlvalues(int triangleID) {
        GDBHandle handle = null;
        long starttime = System.currentTimeMillis();
        //Search for free handles on this file.
        synchronized (waterlevelHandles) {
            for (GDBHandle h : waterlevelHandles) {
                if (!h.busy) {
                    handle = h;
                    handle.busy = true;
                    handle.th = Thread.currentThread();
                    break;
                }
            }
        }
        if (handle == null) {
            synchronized (waterlevelHandles) {
                //create new handle

                GeoTable tableWL = new GeoTable((GeoTable) layerWaterlevels);
                tableWL.open();
                handle = new GDBHandle(tableWL);
                handle.busy = true;
                waterlevelHandles.add(handle);

                if (verbose) {
                    System.out.println("New Handle for WaterlevelGDB " + waterlevelHandles.size());
                }
            }
        }
        try {
            GeoFeature feature = getFeature(handle.table, indexWLid, triangleID);
            handle.busy = false;
            float[] wl = new float[waterheightTimeSteps];

            for (int i = 0; i < wl.length; i++) {
                wl[i] = (float) feature.getValue(indexWL0 + i).doubleValue();
            }
            sqlRequestCount++;
            sqlRequestTime += System.currentTimeMillis() - starttime;
            return wl;
        } catch (NullPointerException e) {
            handle.busy = false;
            //The Database does not contain information on triangle with this id.
        }
        return new float[waterheightTimeSteps];
    }

    /**
     *
     * @param triangleID
     * @return
     * @throws IDnotFoundException
     */
    @Override
    public float loadZElevation(int triangleID) throws IDnotFoundException {
        GeoFeature feature = getFeature(layerWaterlevels, indexWLid, triangleID);
        if (feature == null) {
            throw new IDnotFoundException(triangleID);
        }
        return (float) feature.getValue(indexWLZ).doubleValue();

    }

    public float loadMaxVelocity(int triangleID) throws IDnotFoundException {

        GeoFeature feature = getFeature(layerVelocities, indexVid, triangleID);
        float vmax = (float) feature.getValue(indexVMax).doubleValue();
        return vmax;
    }

    public float loadMaxWaterlevel(int triangleID) throws IDnotFoundException {

        GeoFeature feature = getFeature(layerWaterlevels, indexWLid, triangleID);
        float wlmax = (float) feature.getValue(indexWLmax).doubleValue();
        return wlmax;
    }

    @Override
    public float[][] loadVelocity(int triangleID) throws IDnotFoundException {
        long starttime = System.currentTimeMillis();
        GDBHandle handle = null;
        //Search for free handles on this file.
        synchronized (velocityHandles) {
            for (GDBHandle h : velocityHandles) {
                if (!h.busy) {
                    handle = h;
                    handle.busy = true;
                    handle.th = Thread.currentThread();
                    break;
                }
            }
        }
        if (handle == null) {
            synchronized (velocityHandles) {
                //create new handle
                GeoTable tableVelocity = new GeoTable((GeoTable) layerVelocities);
                tableVelocity.open();
                handle = new GDBHandle(tableVelocity);
                handle.busy = true;
//                handle.table = tableVelocity;
                velocityHandles.add(handle);

                if (verbose) {
                    System.out.println("New Handle for VelocityGDB " + velocityHandles.size());
                }
            }
        }

        try {

            GeoFeature feature = getFeature(handle.table, indexVid, triangleID);
            handle.busy = false;
            float[][] v = new float[velocityTimeSteps][2];
//            System.out.println("loadvelocity directed velocities only? "+velocities_directed_only);

            if (velocities_directed_only) {
//                System.out.println("fill");
                fillVelocityByDirectedVelocity(v, feature);
            } else {
                for (int i = 0; i < v.length; i++) {
                    v[i][0] = (float) feature.getValue(indexVX0 + i * 4).doubleValue();
                    v[i][1] = (float) feature.getValue(indexVX0 + i * 4 + 1).doubleValue();
                }
            }

            sqlRequestCount++;
            sqlRequestTime += System.currentTimeMillis() - starttime;
            return v;
        } catch (IDnotFoundException iDnotFoundException) {
            handle.busy = false;
            throw iDnotFoundException;
        }
    }

    public class IDnotFoundException extends NullPointerException {

        public final int id;

        public IDnotFoundException(int id) {
            super("ID " + id + " not found in GDB " + directory);
            this.id = id;
        }

    }

    public Geometry loadGeometry(int triangleID) {
        GeoFeature feature = getFeature(layerWaterlevels, indexWLid, triangleID);
        GeometryValue gv = feature.getValue(0).geometryValue();
        if (gv.getClass().equals(MultiPoint.class)) {
            MultiPoint mp = (MultiPoint) gv;
            Coordinate[] coords = new Coordinate[mp.points[0].x.length];
            Point p = mp.points[0];
            for (int i = 0; i < coords.length; i++) {
//                System.out.println(i + ". point has " + p.x.length + " x entries");
                coords[i] = new Coordinate(p.x[i], p.y[i], p.z[i]);
            }
            GeometryFactory gf = new GeometryFactory();
            return gf.createMultiPoint(coords);

        } else if (gv.getClass().equals(Point.class)) {
            Point p = (Point) gv;
            GeometryFactory gf = new GeometryFactory();
            return gf.createPoint(new Coordinate(p.x[0], p.y[0], p.z[0]));
        } else {
            System.err.println("unknown Geometry type " + gv.getClass() + " in GDB");
        }
        return null;
    }

    public static Geometry convertGeometry(GeometryValue gv) {
        if (gv.getClass().equals(MultiPoint.class)) {
            MultiPoint mp = (MultiPoint) gv;

            Coordinate[] coords = new Coordinate[mp.points[0].x.length];
            Point p = mp.points[0];
            for (int i = 0; i < coords.length; i++) {
                if (p.z == null) {
                    coords[i] = new Coordinate(p.x[i], p.y[i]);
                } else {
                    coords[i] = new Coordinate(p.x[i], p.y[i], p.z[i]);
                }
            }
            GeometryFactory gf = new GeometryFactory();
            return gf.createMultiPoint(coords);

        } else if (gv.getClass().equals(Point.class)) {
            Point p = (Point) gv;
            GeometryFactory gf = new GeometryFactory();
            return gf.createPoint(new Coordinate(p.x[0], p.y[0], p.z[0]));
        } else {
            System.err.println("unknown Geometry type " + gv.getClass() + " in GDB");
        }
        return null;
    }

    public File getDirectory() {
        return directory;
    }

    public static void resetRequestBenchmark() {
        sqlRequestCount = 0;
        sqlRequestTime = 0;
        waitingForRequestCount = 0;
        waitingForRequestTime = 0;
    }

    public static String getRequestbenchmarkString() {
        return "HE GDB Benchmark: GDB Requests: " + sqlRequestCount + " with total " + sqlRequestTime / 1000 + " s (" + (sqlRequestTime / sqlRequestCount) + "ms/query) these can be parallel requests.";//\tWaiting threads: " + waitingForRequestCount + " with total pausing time:" + waitingForRequestTime + "ms";
    }

}
