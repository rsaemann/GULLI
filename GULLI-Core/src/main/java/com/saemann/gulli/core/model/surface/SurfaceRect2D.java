package com.saemann.gulli.core.model.surface;

import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.maths.GeometryTools;
import com.saemann.gulli.core.io.extran.HE_GDB_IO;
import com.saemann.gulli.core.io.extran.HE_InletReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.MeasurementTimeline;
import com.saemann.gulli.core.model.timeline.array.TimeIndexCalculator;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Connection;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Inlet;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position3D;
import com.saemann.gulli.core.model.topology.graph.Pair;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import org.locationtech.jts.geom.CoordinateXY;
import org.opengis.referencing.operation.TransformException;

/**
 * Containing all information about surface topology. - triangle indices,
 * positions, neumannNeighbours. - 1D transport Paths.
 *
 * @author saemann
 */
public class SurfaceRect2D extends Capacity implements TimeIndexCalculator {

    /**
     * [x][y][3]
     */
    public final double[][][] vertices;  //UTM
    /**
     * [id][0:x,1:y,2:z]
     */
    private double[][] cellMids;

    protected double lowerLeftX, lowerLeftY;

    /**
     * Number of Cells in X direction
     */
    protected int numberCellsX;

    /**
     * NumberOfCells in Y direction
     */
    protected int numberCellsY;

    /**
     * Total number of cells = [number in X direction] * [number in Y direction]
     */
    protected int numberCellsTotal;

    protected double cellsizeX, cellsizeY;

    protected float[] cellAreas;

    /**
     * Slope of cell directed downwards. Used to calculate potential direction
     * of particles, if no velocity information is given.
     */
    public float[][] cell_downhill;

    public boolean timeInterpolatedValues = true;

    volatile protected SurfaceMeasurementRaster measurementRaster;

    private int numberOfTimestamps;

    private float[][] waterlevels; //[triangle][timeindex]
    private double[] maxWaterLevels;

    public SurfaceWaterlevelLoader waterlevelLoader;

    public SurfaceVelocityLoader velocityLoader;

    /**
     * [cellID][timeindex][direction(0:x,1:y)]
     */
    private float[][][] cellVelocity;

    private float[] zeroVelocity = new float[2];

    public int status = -1, vstatus = -1;

    protected TimeIndexContainer times;

    protected long actualTime;

    /**
     * Number of different materials that are injected and should be counted in
     * measurements seperately.
     */
    protected int numberOfMaterials = 1;

    /**
     * This timeindex is used internally to store the actual timeindex and not
     * need to calculate it new every request.
     */
    protected double timeIndex = 0;

    /**
     * Fraction of time between the last and the next timestep [0,1];
     */
    protected double timeFrac = 0;
    protected double timeinvFrac = 1;

    /**
     * Timeindex as int.
     */
    protected int timeIndexInt = 0;

    /**
     * Roughness used when calculating neighbour velocities from waterlevels.
     */
    private double kst = 50; //Beton/Asphalt

    /**
     * Geotools for converting from the local utm to wgs84
     */
    private GeoTools geotools;

    /**
     * Contains information of used particle paths. Used for combining same
     * paths.
     */
    private final HashSet<SurfacePathStatistics> statistics = new HashSet<>();

    /**
     * Code to tell the GeoTools which Reference System is used in coordinates
     * of this surface. Form is like "EPSG:28532"
     */
    private String spatialReferenceCode;

    /**
     * possible Inlet on/for every triangle ID
     */
    protected Inlet[] inletArray;//an array is larger but generates much less overhead compared to a hashmap. And an array is faster
    /**
     * possible Manhole on/for every triangle ID
     */
    protected Manhole[] manholes;

    public HashMap<Capacity, Integer> sourcesForSpilloutParticles = new HashMap<>();

    public boolean detailedInformationAboutIDNotFound = true;

    public HashMap<String, Capacity> capacityNames;

    private double[][] actualVelocity;
    private boolean[] actualVelocitySet;
    private boolean actualVelocityUsed = false;
    private float[] maxCellAbsVelocity;

    /**
     *
     * @param vertices positions of nodes [x][y][3]
     * @param spatialReferenceSystem
     */
    public SurfaceRect2D(double[][][] vertices, String spatialReferenceSystem) {
        super(new CircularProfile(1));
        this.vertices = vertices;
        numberCellsX = vertices.length - 1;
        numberCellsY = vertices[0].length - 1;
        numberCellsTotal = numberCellsX * numberCellsY;
        if (numberCellsX > 1) {
            cellsizeX = (this.vertices[numberCellsX][0][0] - this.vertices[0][0][0]) / (double) numberCellsX;
        }
        if (numberCellsY > 1) {
            cellsizeY = (this.vertices[0][numberCellsY][1] - this.vertices[0][0][1]) / (double) numberCellsY;
        }
        this.spatialReferenceCode = spatialReferenceSystem;
        try {
            this.geotools = new GeoTools("EPSG:4326", /*"EPSG:25832"*/ spatialReferenceSystem, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        } catch (Exception ex) {
            Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
        }

        actualVelocity = new double[numberCellsTotal][2];
        actualVelocitySet = new boolean[actualVelocity.length];

        calculateDownhillSlopes();
    }

    public void initSparseTriangleVelocityLoading(SurfaceVelocityLoader velocityLoader, boolean initTriangleVelocity, boolean initNodeVelocity) {
        this.velocityLoader = velocityLoader;
        if (initTriangleVelocity) {
            this.cellVelocity = new float[numberCellsTotal][][];
        }
    }

    public void getCellIndices(double[] position, int[] toFill) {
        toFill[0] = (int) ((position[0] - lowerLeftX) / cellsizeX);
        toFill[1] = (int) ((position[1] - lowerLeftY) / cellsizeY);
    }

    public int[] getCellIndices(double[] position) {
        int[] toFill = new int[2];
        toFill[0] = (int) ((position[0] - lowerLeftX) / cellsizeX);
        toFill[1] = (int) ((position[1] - lowerLeftY) / cellsizeY);
        return toFill;
    }

    public int[] getCellIndicesForID(int cellID) {
        int[] toFill = new int[2];
        toFill[0] = cellID % numberCellsX;
        toFill[1] = cellID / numberCellsX;
        return toFill;
    }

    public int getCellIDforIndices(int[] indicesXY) {
        //0=lowerleft, 1: one more to the right
        return indicesXY[1] * numberCellsX + indicesXY[0];
    }

    public int getCellIDforPosition(double[] position) {
        //0=lowerleft, 1: one more to the right
        return (int) ((position[1] - lowerLeftY) / cellsizeY) * numberCellsX + (int) ((position[0] - lowerLeftX) / cellsizeX);
    }

    public int getCellIDforPosition(double x, double y) {
        //0=lowerleft, 1: one more to the right
        return (int) ((y - lowerLeftY) / cellsizeY) * numberCellsX + (int) ((x - lowerLeftX) / cellsizeX);
    }

//    /**
//     * Creates and returns a Triangle Capacity representing a triangle.
//     *
//     * @param id
//     * @return
//     */
//    public SurfaceTriangle requestSurfaceTriangle(int id) {
//        if (triangleCapacitys == null) {
//            triangleCapacitys = new SurfaceTriangle[triangleNodes.length];
//        }
//        SurfaceTriangle tri = triangleCapacitys[id];//triangleCapacity.get(id);
//        if (tri == null) {
//            synchronized (triangleCapacitys) {
//                tri = triangleCapacitys[id];
//                //Test if another Thread has created this Triangle while this Threas was waiting.
//                if (tri == null) {
//                    tri = new SurfaceTriangle(id);
//                    Coordinate c = new Coordinate(cellMids[id][0], cellMids[id][1]);
//                    try {
//                        Coordinate cll = geotools.toGlobal(c);
//                        tri.setPosition(new Position3D(cll.x, cll.y, c.x, c.y, cellMids[id][2]));
//                    } catch (Exception ex) {
//                        Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    triangleCapacitys[id] = tri;
//                }
//            }
//        }
//        return tri;
//    }
//    /**
//     * Creates and returns a Triangle Capacity representing a travle path
//     * between two triangles. Is Threadsafe.
//     *
//     * @param starttriangle
//     * @param neighbourIndex
//     * @return
//     */
//    public SurfaceTrianglePath requestSurfacePath(int starttriangle, int neighbourIndex) {
////        return null;
////        status = 1;
//        synchronized (paths) {
////            status = 2;
//            SurfaceTrianglePath[] localpaths = paths.get(starttriangle);
////            status = 3;
//            if (localpaths == null) {
////                status = 4;
//                localpaths = new SurfaceTrianglePath[3];
//                paths.put(starttriangle, localpaths);
////                status = 5;
//                int targetID = neumannNeighbours[starttriangle][neighbourIndex];
//                if (targetID < 0) {
//                    return null;
//                }
////                status = 6;
//                SurfaceTrianglePath path = new SurfaceTrianglePath(starttriangle, targetID, neighbourDistances[starttriangle][neighbourIndex], this);
//                Coordinate c = new Coordinate(cellMids[starttriangle][0], cellMids[starttriangle][1]);
//                try {
////                    status = 7;
//                    Coordinate cll = geotools.toGlobal(c);
//                    path.start = new Position3D(cll.x, cll.y, c.x, c.y, cellMids[starttriangle][2]);
//                } catch (Exception ex) {
//                    Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
//                }
////                status = 8;
//                path.neighbourIndex = neighbourIndex;
//                localpaths[neighbourIndex] = path;
////                status = 9;
//                return path;
////            
//            }
////            status = 10;
//            if (localpaths[neighbourIndex] == null) {
//                int targetID = neumannNeighbours[starttriangle][neighbourIndex];
////                status = 11;
//                if (targetID < 0) {
//                    return null;
//                }
////                status = 12;
//                SurfaceTrianglePath path = new SurfaceTrianglePath(starttriangle, targetID, neighbourDistances[starttriangle][neighbourIndex], this);
//                Coordinate c = new Coordinate(cellMids[starttriangle][0], cellMids[starttriangle][1]);
//                try {
//                    Coordinate cll = geotools.toGlobal(c);
////                    System.out.println(getClass()+"requestsurfacepath  cll.x="+cll.x);
//                    path.start = new Position3D(cll.x, cll.y, c.x, c.y, cellMids[starttriangle][2]);
//                } catch (Exception ex) {
//                    Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
//                }
////                status = 13;
//                path.neighbourIndex = neighbourIndex;
//                localpaths[neighbourIndex] = path;
////                status = 14;
//                return path;
//            }
////            status = 15;
//            return localpaths[neighbourIndex];
//        }
//    }
//    /**
//     * Calculate distances between neumannNeighbours at initialization.
//     */
//    protected void calculateDistances() {
//        neighbourDistances = new float[neumannNeighbours.length][3];
//
//        for (int i = 0; i < neumannNeighbours.length; i++) {
//
//            double x0 = cellMids[i][0];
//            double y0 = cellMids[i][1];
//
//            for (int j = 0; j < 3; j++) {
//                if (neumannNeighbours[i][j] < 0) {
//                    continue;
//                }
//                try {
//                    double x1 = cellMids[neumannNeighbours[i][j]][0];
//                    double y1 = cellMids[neumannNeighbours[i][j]][1];
//
//                    float distance = (float) Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));
//                    neighbourDistances[i][j] = distance;
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//        }
//    }
    public float getActualWaterlevel(int ID) {
        if (waterlevels[ID] == null) {
            if (waterlevelLoader != null) {
                try {
                    waterlevels[ID] = waterlevelLoader.loadWaterlevlvalues(ID);
                } catch (HE_GDB_IO.IDnotFoundException e) {
                    if (detailedInformationAboutIDNotFound) {
                        e.printStackTrace();
                        detailedInformationAboutIDNotFound = false;//Only do this the first time
                    } else {
                        System.err.println(e.getMessage());
                    }
                    waterlevels[ID] = new float[numberOfTimestamps];
                } catch (Exception e) {
                    e.printStackTrace();
                    waterlevels[ID] = new float[numberOfTimestamps];
                }
            }
        }
        if (timeInterpolatedValues) {
            return (float) (waterlevels[ID][timeIndexInt] + (waterlevels[ID][timeIndexInt + 1] - waterlevels[ID][timeIndexInt]) * timeFrac);
        } else {
            return waterlevels[ID][timeIndexInt];
        }
    }

    public double getkst() {
        return kst;
    }

//    public float[][] getEdgeLength() {
//        return edgeLength;
//    }
//
//    public void calcEdgeLengths() {
//        edgeLength = new float[triangleNodes.length][3];
//        for (int id = 0; id < triangleNodes.length; id++) {
//            for (int n = 0; n < 3; n++) {
//                int nbID = neumannNeighbours[id][n];
//                if (nbID < 0) {
//                    edgeLength[id][n] = 0;
//                    continue;
//                }
//                int v0 = -1;
//                int v1 = -1;
//                for (int i = 0; i < 3; i++) {
//                    int t0index = triangleNodes[id][i];
//                    for (int j = 0; j < 3; j++) {
//                        int n0index = triangleNodes[nbID][j];
//                        if (t0index == n0index) {
//                            if (v0 < 0) {
//                                v0 = t0index;
//                                break;
//                            } else if (v1 < 0) {
//                                v1 = t0index;
//                                break;
//                            }
//                        }
//                    }
//                    if (v1 >= 0) {
//                        break;
//                    }
//                }
//                if (v0 >= 0 && v1 >= 0) {
//                    //Two common points found. calculate distance
//                    double[] pos0 = vertices[v0];
//                    double[] pos1 = vertices[v1];
//                    double distance = Math.sqrt((pos0[0] - pos1[0]) * (pos0[0] - pos1[0]) + (pos0[1] - pos1[1]) * (pos0[1] - pos1[1]));
//                    edgeLength[id][n] = (float) distance;
//                }
//            }
//        }
//    }
    /**
     * Calculates the negative gradient direction (normalised) for each triangle
     * This can be used for a flow direction under dry weather condition.
     */
    public void calculateDownhillSlopes() {
        cell_downhill = new float[numberCellsTotal][2];
        System.err.println("Downhill slopes are not yet calculated in " + getClass().getSimpleName());
//        double[] v0, v1, v2, a = new double[2], b = new double[2];
//        for (int i = 0; i < cell_downhill.length; i++) {
//            try {
//                v0 = vertices[triangleNodes[i][0]];
//                v1 = vertices[triangleNodes[i][1]];
//                v2 = vertices[triangleNodes[i][2]];
//
//                a[0] = v2[0] - v0[0];
//                a[1] = v1[1] - v0[1];
//                b[0] = v1[0] - v0[0];
//                b[1] = v1[1] - v0[1];
//
//                double asquare = a[0] * a[0] + a[1] * a[1];
//                double bsquare = b[0] * b[0] + b[1] * b[1];
//
//                double x = (0.5) * (((v2[2] - v0[2]) * a[0] / asquare) + (v1[2] - v0[2]) * b[0] / bsquare);
//                double y = (0.5) * (((v2[2] - v0[2]) * a[1] / asquare) + (v1[2] - v0[2]) * b[1] / bsquare);
//
//                double length = Math.sqrt((x * x) + (y * y));
//
//                cell_downhill[i][0] = (float) (-x / length);
//                cell_downhill[i][1] = (float) (-y / length);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    public float[] getCellAreas() {
        return cellAreas;
    }

    public void calcCellAreas() {
        cellAreas = new float[numberCellsTotal];
        float area = (float) (cellsizeX * cellsizeY);
        for (int i = 0; i < cellAreas.length; i++) {
            cellAreas[i] = area;
        }
    }

    public double calcTriangleArea(int id) {
        return cellsizeX * cellsizeY;
    }

    /**
     * Set maximum Water level for each triangle.
     *
     * @param maxWaterlvl
     */
    public void setMaxWaterlvl(double[] maxWaterlvl) {
        this.maxWaterLevels = maxWaterlvl;
    }

    /**
     * Maximum Waterlevel for each triangle.
     *
     * @return
     */
    public double[] getMaxWaterlvl() {
        return maxWaterLevels;
    }

    /**
     * Number of timesteps of waterheights.
     *
     * @return
     */
    public int getNumberOfTimestamps() {
        return numberOfTimestamps;
//        if (waterlevels == null || waterlevels.length < 1) {
//            return 0;
//        }
//        return waterlevels[0].length;
    }

    /**
     * Number of Cells.
     *
     * @return
     */
    public int size() {
        return numberCellsTotal;
    }

//    /**
//     * Neighbour triangle index for each triangle. Might be -1 for Noneighbour.
//     * [triangleIndex][1st Neighbour,2nd NB,3rd NB]
//     *
//     * @return
//     */
//    public int[][] getNeighbours() {
//        return neumannNeighbours;
//    }
//
//    public void setNeighbours(int[][] neighbours) {
//        this.neumannNeighbours = neighbours;
//    }
    /**
     * waterlevels [triangleindex][timeindex]
     *
     * @return
     */
    public float[][] getWaterlevels() {
        return waterlevels;
    }

    /**
     * waterlevels [triangleindex][timeindex]
     *
     * @param waterlevels
     */
    public void setWaterlevels(float[][] waterlevels) {
        this.waterlevels = waterlevels;
        if (waterlevels[0] != null) {
            this.numberOfTimestamps = waterlevels[0].length;
        }
    }

    public double[] getMaxWaterLevels() {
        return maxWaterLevels;
    }

    public void setMaxWaterLevels(double[] maxWaterLevels) {
        this.maxWaterLevels = maxWaterLevels;
    }

    /**
     * Returns the mid point for each traingle. [triangle index][x,y,z]
     *
     * @return
     */
    public double[][] getTriangleMids() {
        return cellMids;
    }

    /**
     * Set center points of triangles as they may vary to calculated ones for
     * the velocity calculation.
     *
     * @param triangleMids
     */
    public void setTriangleMids(double[][] triangleMids) {
        if (triangleMids.length != numberCellsTotal) {
            System.out.println("Surface: Cell Mid Points trying to be set (" + triangleMids.length + ") have not the same size as number of cells (" + this.numberCellsTotal + ")");
        }
        this.cellMids = triangleMids;
    }

    public void initVelocityArrayForSparseLoading(int numberOfTriangles, int numberOfTimes) {
        this.numberOfTimestamps = numberOfTimes;
        this.waterlevels = new float[numberOfTriangles][];
        cellVelocity = new float[numberOfTriangles][numberOfTimes][2];
    }

    /**
     * Calculates velocity with diffusive wave approximation
     *
     * @param ds ground distance between two points (without respect to z)
     * @param elevation0
     * @param elevation1
     * @param waterdepth0
     * @param waterdepth1
     * @return velocity from 0 to 1 in [m/s]
     */
    private double velocity(double ds, double elevation0, double elevation1, double waterdepth0, double waterdepth1) {
        double a = -(elevation1 - elevation0 + waterdepth1 - waterdepth0) / (ds);
        double ksth = kst * Math.pow((waterdepth0 + waterdepth1) * 0.5, 0.6666666);
        double v;
        if (a < 0) {
            v = -Math.sqrt(-a) * ksth;
        } else {
            v = Math.sqrt(a) * ksth;
        }
        return v;
    }

//    /**
//     * @param time
//     * @param triangleIndex
//     * @param neighbourIndex
//     * @return
//     */
//    public float getVelocityToNeighbour(long time, int triangleIndex, int neighbourIndex) {
////        System.out.println("513: getvelocitytoNeighbour long");
//        double index = times.getTimeIndexDouble(time);
//        if (neighbourvelocity[triangleIndex] == null) {
//            initVelocityToNeighbours(triangleIndex);
//        }
//        float v;
//        if (index >= neighbourvelocity.length - 1) {
//            v = (float) neighbourvelocity[triangleIndex][neighbourIndex][neighbourvelocity[triangleIndex][neighbourIndex].length - 1];
//        } else {
//            v = (float) (neighbourvelocity[triangleIndex][neighbourIndex][(int) index] * (1 - index % 1) + neighbourvelocity[triangleIndex][neighbourIndex][(int) index + 1] * (index % 1));
//        }
//
//        return v;
//    }
//    /**
//     * Velocity to neighbour at given timeindex [m/s]
//     *
//     * @param timeIndex
//     * @param triangleIndex
//     * @param neighbourIndex
//     * @return
//     */
//    public float getVelocityToNeighbour(int timeIndex, int triangleIndex, int neighbourIndex) {
////        System.out.println("537: getvelocitytoNeighbour int");
//        if (neighbourvelocity[triangleIndex] == null) {
//            initVelocityToNeighbours(triangleIndex);
//        }
//        try {
//            float v = neighbourvelocity[triangleIndex][neighbourIndex][timeIndex];
//
//            return v;
//        } catch (IndexOutOfBoundsException e) {
//            System.err.println("triangleIndex:" + triangleIndex + "/" + neighbourvelocity.length + "   neighbour:" + neighbourIndex + "/" + neighbourvelocity[0].length + "   time:" + timeIndex + "/" + neighbourvelocity[0][0].length);
//            System.err.println(neighbourvelocity.length + "," + neighbourvelocity[triangleIndex].length + "   time:" + timeIndex + neighbourvelocity[triangleIndex][neighbourIndex].length);
//
//            return 0;
//        }
//    }
    public float[] loadWaterlevels(int triangleID) {
        if (waterlevels == null) {
            waterlevels = new float[numberCellsTotal][];
        }
        if (waterlevelLoader == null) {
            float[] wlsNB = new float[numberOfTimestamps];
            this.waterlevels[triangleID] = wlsNB;
            return wlsNB;
        }
        float[] wlsNB = waterlevelLoader.loadWaterlevlvalues(triangleID);
        if (wlsNB == null) {
            wlsNB = new float[numberOfTimestamps];
        }
        this.waterlevels[triangleID] = wlsNB;
        try {
            this.cellMids[triangleID][2] = waterlevelLoader.loadZElevation(triangleID);
        } catch (NullPointerException e) {
            //Triangle with this id is not found in database.
        }

        return wlsNB;
    }

    /**
     * Get the velocity for this triangle. if it is not yet known. the
     * information is loaded via the velocityloader connected for this surface.
     *
     * @param triangleID
     * @return float[times][2:x,y] velocity (m/s)
     */
    private float[][] getTriangleVelocity(int triangleID) {
        if (cellVelocity[triangleID] == null) {
            try {
                cellVelocity[triangleID] = velocityLoader.loadVelocity(triangleID);
            } catch (Exception e) {
                //Id Not found or equal exception. This triangle has no velocity information -> set everything to zero.
                cellVelocity[triangleID] = new float[numberOfTimestamps][2];
            }
        }
        return cellVelocity[triangleID];
    }

    public float[] getTriangleVelocity(int triangleID, double indexDouble) {
        return getTriangleVelocity(triangleID, indexDouble, new float[2]);
    }

    public float[] getTriangleVelocity(int triangleID, double indexDouble, float[] tofill) {
        if (tofill == null) {
            System.out.println("create new float[2] to return interpolated velocity.");
            tofill = new float[2];
        }
        float[] lower = getTriangleVelocity(triangleID)[(int) indexDouble];
        float[] upper = getTriangleVelocity(triangleID)[(int) indexDouble + 1];
        float frac = (float) (indexDouble % 1f);
        tofill[0] = (lower[0] + (upper[0] - lower[0]) * frac);
        tofill[1] = (lower[1] + (upper[1] - lower[1]) * frac);
        return tofill;
    }

    public void getTriangleVelocity(int triangleID, double indexDouble, double[] tofill) {

        float[] lower = getTriangleVelocity(triangleID)[(int) indexDouble];
        float[] upper = getTriangleVelocity(triangleID)[(int) indexDouble + 1];
        float frac = (float) (indexDouble % 1f);
        tofill[0] = (lower[0] + (upper[0] - lower[0]) * frac);
        tofill[1] = (lower[1] + (upper[1] - lower[1]) * frac);
    }

    public float[] getTriangleVelocity(int triangleID, int timeindexInt, float frac, float[] tofill) {
        if (tofill == null) {
            System.out.println("create new float[2] to return interpolated velocity.");
            tofill = new float[2];
        }
        float[] lower = getTriangleVelocity(triangleID)[timeindexInt];
        float[] upper = getTriangleVelocity(triangleID)[timeindexInt + 1];
        tofill[0] = (lower[0] + (upper[0] - lower[0]) * frac);
        tofill[1] = (lower[1] + (upper[1] - lower[1]) * frac);
        return tofill;
    }

    /**
     * Velocity[x,y] at the center of the triangle at the given timeindex.
     *
     * @param triangleID
     * @param indexInteger
     * @return float[2] 0:x; 1:y velocity (m/s)
     */
    public float[] getTriangleVelocity(int triangleID, int indexInteger) {
        return getTriangleVelocity(triangleID)[indexInteger];
    }

    /**
     * Indices of nodes of the cell
     *
     * @return 4 ids of the vertices defining this cell.
     */
    public int[] getCellNodes(int cellID) {
        int[] indices = new int[4];
        indices[0] = cellID;
        indices[1] = cellID + 1;
        indices[2] = cellID + 1 + numberCellsX;
        indices[0] = cellID + numberCellsX;
        return indices;
    }

    /**
     * Vertices' coordinates
     *
     * @return [xcolumn][Yrow][x,y,z]
     */
    public double[][][] getVerticesPosition() {
        return vertices;
    }

    public Polygon calcConvexHullUTM() {
        Coordinate[] set = new Coordinate[5];
        set[0] = (new CoordinateXY(lowerLeftX, lowerLeftY));
        set[1] = (new CoordinateXY(lowerLeftX + numberCellsX * cellsizeX, lowerLeftY));
        set[2] = (new CoordinateXY(lowerLeftX + numberCellsX * cellsizeX, lowerLeftY + numberCellsY * cellsizeY));
        set[3] = (new CoordinateXY(lowerLeftX, lowerLeftY + numberCellsY * cellsizeY));
        set[4] = set[0];

        GeometryFactory gf = new GeometryFactory();
        return gf.createPolygon(set);
    }

    public Polygon calcConvexHullWGS84() throws TransformException {
        Polygon utm = calcConvexHullUTM();
        return (Polygon) geotools.toGlobal(utm);
    }

    /**
     * Set Simulation Time so the surface can interpolate velocities based on
     * this entered simulation time.
     *
     * @param time
     */
    public void setSimulationTime(long time) {
//        if (time > nextRecalculation) {
//            nextRecalculation = time + recalculationIntervallMS;
//        }
        this.actualTime = time;
        this.timeIndex = times.getTimeIndexDouble(time);
        this.timeFrac = timeIndex % 1.;
        this.timeinvFrac = 1. - timeFrac;
        this.timeIndexInt = (int) this.timeIndex;
//        if (actualVelocity != null) {
//            actualVelocity = new double[triangleNodes.length][];
//        }
        if (actualVelocityUsed && actualVelocitySet != null) {
            for (int i = 0; i < actualVelocitySet.length; i++) {
                if (actualVelocitySet[i]) {
                    actualVelocitySet[i] = false;
                }
            }
//            actualVelocitySet = new boolean[triangleNodes.length];
            actualVelocityUsed = false;
        }
    }

    public GeoTools getGeotools() {
        return geotools;
    }

    /**
     * Reset surface after one simulation: clears particlepaths statisitics,
     * clears triangle capacities, set all measured values to zero. Does not
     * delete positions of measurements.
     */
    public void reset() {
//        nextRecalculation = 0L;
        this.timeIndex = 0;
        this.statistics.clear();
        if (measurementRaster != null) {
            measurementRaster.reset();
        }
        sourcesForSpilloutParticles.clear();
    }

    /**
     * Set number of different materials that are injected.
     *
     * @param numberOfMaterials
     */
    public void setNumberOfMaterials(int numberOfMaterials) {
        if (numberOfMaterials == this.numberOfMaterials) {
            return;
        }
        this.numberOfMaterials = numberOfMaterials;
        if (this.measurementRaster != null) {
            this.measurementRaster.setNumberOfMaterials(numberOfMaterials);
        }
    }

    public int getNumberOfMaterials() {
        return numberOfMaterials;
    }
//
//    /**
//     * @deprecated use applyStreetInlets(Network network,
//     * ArrayList<HE_InletReference> inletRefs) instead
//     * @param nw
//     * @param pipeANDTriangleIDs
//     */
//    public void applyStreetInlets(Network nw, Collection<Pair<String, Integer>> pipeANDTriangleIDs) {
////        inlets = new ConcurrentHashMap<>(pipeANDTriangleIDs.size());//new Inlet[triangleNodes.length];
//        inletArray = new Inlet[getMaxTriangleID() + 1];
//        ArrayList<Inlet> inletList = new ArrayList<>(pipeANDTriangleIDs.size());
//
//        for (Pair<String, Integer> pipeANDTriangleID : pipeANDTriangleIDs) {
//            String pipename = pipeANDTriangleID.first;
//            int triangleID = pipeANDTriangleID.second;
//
//            //1. check if pipe with this name exists
//            Capacity cap = null;
//            try {
//                cap = nw.getCapacityByName(pipename);
//
//            } catch (NullPointerException nullPointerException) {
//            }
//            if (cap == null) {
//                System.err.println("Could not find Pipe with name '" + pipename + "' to apply a streetinlet next to it. (deprectaed call)");
//                continue;
//            }
//            //Transform Pipe to surface coordinates
//            double[] tpos = cellMids[triangleID];
//
//            double distancealongPipe = 0;
//            if (cap instanceof Pipe) {
//                try {
//                    Pipe pipe = (Pipe) cap;
//                    Coordinate start = geotools.toUTM(pipe.getStartConnection().getPosition());
//                    Coordinate end = geotools.toUTM(pipe.getEndConnection().getPosition());
//                    distancealongPipe = GeometryTools.distancePointAlongLine(start.x, start.y, end.x, end.y, tpos[0], tpos[1]);
//                } catch (TransformException ex) {
//                    Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            } else if (cap instanceof Manhole) {
//                //MAp inlet to manholes pipe
//                Manhole mh = (Manhole) cap;
//                boolean found = false;
//                for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//                    if (connection.isEndOfPipe()) {
//                        cap = connection.getPipe();
//                        distancealongPipe = connection.getPipe().getLength();
//                        found = true;
//                    }
//                }
//                if (found == false) {
//                    for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//                        if (connection.isStartOfPipe()) {
//                            cap = connection.getPipe();
//                            distancealongPipe = 0;
//                        }
//                    }
//                }
//
//            } else {
//                System.out.println("Inlet references " + cap);
//                continue;
//            }
//
//            try {
//                Coordinate longlat = geotools.toGlobal(new Coordinate(tpos[0], tpos[1]), true);
//                Inlet inlet = new Inlet(new Position3D(longlat.x, longlat.y, tpos[0], tpos[1], tpos[3]), (Pipe) cap, distancealongPipe);
////                tri.inlet = inlet;
//                inletList.add(inlet);
////                inlets.put(triangleID, inlet);
//                inletArray[triangleID] = inlet;
////                inlets[triangleID] = inlet;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        nw.setStreetInlets(inletList);
////        this.inlets = inlets;
//    }

    public void applyStreetInlets(Network network, ArrayList<HE_InletReference> inletRefs) throws TransformException {
//        inlets = new ConcurrentHashMap<>(inletRefs.size());//new Inlet[triangleNodes.length];

        ArrayList<Inlet> inletList = new ArrayList<>(inletRefs.size());
        manholes = new Manhole[numberCellsTotal];
        inletArray = new Inlet[manholes.length];
        for (HE_InletReference inletRef : inletRefs) {
            String capacityName = inletRef.capacityName;
            int triangleID = inletRef.triangleID;

            //1. check if pipe with this name exists
            Capacity cap = null;
            cap = capacityNames.get(capacityName);

            if (cap == null) {
                System.err.println("Could not find Pipe with name '" + capacityName + "' to apply a streetinlet next to it.");
                continue;
            }

            //Transform Pipe to surface coordinates
            double distancealongPipe = 0;
            double[] tpos = getTriangleMids()[triangleID];
            Coordinate tposUTM = new Coordinate(tpos[0], tpos[1], tpos[2]);
            Coordinate tposWGS84 = geotools.toGlobal(tposUTM, true);

            if (cap instanceof Pipe) {

                Pipe pipe = (Pipe) cap;
                Coordinate start = geotools.toUTM(pipe.getStartConnection().getPosition());
                Coordinate end = geotools.toUTM(pipe.getEndConnection().getPosition());

                distancealongPipe = GeometryTools.distancePointAlongLine(start.x, start.y, end.x, end.y, tposUTM.x, tposUTM.y);

            } else if (cap instanceof Manhole) {
                //MAp inlet to manholes pipe
                Manhole mh = (Manhole) cap;
                boolean found = false;
                for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                    if (connection.isEndOfPipe()) {
                        cap = connection.getPipe();
                        distancealongPipe = connection.getPipe().getLength();
                        found = true;
                    }
                }
                if (found == false) {
                    for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                        if (connection.isStartOfPipe()) {
                            cap = connection.getPipe();
                            distancealongPipe = 0;
                        }
                    }
                }

            } else {
                System.out.println("Inlet references " + cap);
                continue;
            }

            try {
                Inlet inlet = new Inlet(new Position3D(tposWGS84.x, tposWGS84.y, tposUTM.x, tposUTM.y, tposUTM.z), cap, distancealongPipe);
                inletList.add(inlet);
//                inlets.put(triangleID, inlet);
                inletArray[triangleID] = inlet;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        network.setStreetInlets(inletList);
    }

    public HashMap<String, Capacity> buildNamedCapacityMap(Network nw) {
        HashMap<String, Capacity> map = new HashMap<>(nw.getManholes().size() + nw.getPipes().size());
        for (Pipe pipe : nw.getPipes()) {
            map.put(pipe.getName(), pipe);
        }
        for (Manhole manhole : nw.getManholes()) {
            map.put(manhole.getName(), manhole);
        }
        return map;
    }

    public void clearNamedCapacityMap() {
        if (capacityNames != null) {
            capacityNames.clear();
        }
    }

    public void applyManholeRefs(Network network, ArrayList<Pair<String, Integer>> manhRefs) {

        if (capacityNames == null || capacityNames.isEmpty()) {
            capacityNames = buildNamedCapacityMap(network);
        }
        manholes = new Manhole[numberCellsTotal];
        for (Pair<String, Integer> mr : manhRefs) {
            Manhole mh = (Manhole) capacityNames.get(mr.first);//network.getManholeByName(mr.first);
            if (mh != null) {
                manholes[mr.second] = mh;
                mh.setSurfaceTriangle(mr.second);
            }
        }
    }

    public void addStatistic(Particle p, int startTriangleID, Inlet inlet, Manhole manhole, long traveltime) {
        if (true) {
            return;
        }
        SurfacePathStatistics stat = null;
        if (inlet != null) {
            synchronized (statistics) {
                for (SurfacePathStatistics st : statistics) {
                    if (st.pathEquals(startTriangleID, inlet)) {
                        stat = st;
                        break;
                    }
                }
            }
        }
        if (stat == null && manhole != null) {
            synchronized (statistics) {
                for (SurfacePathStatistics st : statistics) {
                    if (st.pathEquals(startTriangleID, manhole)) {
                        stat = st;
                        break;
                    }
                }
            }
        }

        if (stat == null) {
            //Not yet in Set . Create new one
            if (inlet != null) {
                stat = new SurfacePathStatistics(startTriangleID, inlet);
            } else if (manhole != null) {
                stat = new SurfacePathStatistics(startTriangleID, manhole);
            }
            if (stat != null) {
                synchronized (statistics) {
                    statistics.add(stat);
                }
            } else {
                throw new NullPointerException("Statistics to create has neither a target inlet nor manhole.");
            }
        }
        stat.number_of_particles++;
        stat.sum_traveltime += traveltime;
        stat.sum_mass += p.particleMass;
//        stat.sum_travelLength += p.getTravelledPathLength() - p.posToSurface;

        stat.minTravelTime = Math.min(stat.minTravelTime, traveltime);
        stat.maxTravelTime = Math.max(stat.maxTravelTime, traveltime);

//        stat.minTravelLength = Math.min(stat.minTravelLength, p.getTravelledPathLength() - p.posToSurface);
//        stat.maxTravelLength = Math.max(stat.maxTravelLength, p.getTravelledPathLength() - p.posToSurface);
        if (sourcesForSpilloutParticles.containsKey(p.getInjectionInformation().getInjectionCapacity())) {//.injectionSurrounding)) {
            Integer counter = sourcesForSpilloutParticles.get(p.getInjectionInformation().getInjectionCapacity());
            int ineu = counter + 1;
            sourcesForSpilloutParticles.put(p.getInjectionInformation().getInjectionCapacity(), ineu);
        } else {
            sourcesForSpilloutParticles.put(p.getInjectionInformation().getInjectionCapacity(), 1);
        }
    }

    public HashSet<SurfacePathStatistics> getStatistics() {
        return statistics;
    }

    /**
     * Inlet for the requested Triangle ID. null if not set or no input.
     *
     * @param triangleID
     * @return
     */
    public Inlet getInlet(int triangleID) {
        if (inletArray == null) {
            return null;
        }
        return inletArray[triangleID];
//        if (inlets == null) {
//            return null;
//        }
//        return inlets.get(triangleID);
//        return inlets[triangleID];
    }

    /**
     * Manhole at the requested Triangle ID. null if not set or no input.
     *
     * @param triangleID
     * @return Manhole at triangle
     */
    public Manhole getManhole(int triangleID) {
        if (manholes == null) {
            return null;
        }
        return manholes[triangleID];
    }

    public Manhole[] getManholes() {
        return manholes;
    }

    /**
     * Finds smallest triangle area of all triangles.
     *
     * @return smallest triangle area [m²]
     */
    public float calcSmallestCelleArea() {
        return (float) (cellsizeX * cellsizeY);
    }

    /**
     * Runs over all triangles and finds the one with maximum area.
     *
     * @return area of max traingle [m²]
     */
    public float calcLargestCellArea() {
        return (float) (cellsizeX * cellsizeY);
    }

    /**
     * Runs over all triangles and sums up all areas.
     *
     * @return total area of this surface [m²]
     */
    public double calcTotalArea() {
        return (float) (cellsizeX * numberCellsX * cellsizeY * numberCellsY);
    }

    /**
     * Runs over all triangles and calculates a mean value for the area of ONe
     * triangle in m²
     *
     * @return mean triangle area [m²]
     */
    public double calcMeanCellArea() {
        return calcSmallestCelleArea();
    }

    /**
     * velocities on triangle [triangle][timeindex][direction(0:x,1:y)]
     *
     * @return
     */
    public float[][][] getTriangleVelocity() {
        return cellVelocity;
    }

    /**
     * Set velocities on triangle [triangle][timeindex][direction(0:x,1:y)]
     *
     * @param triangleVelocity
     */
    public void setTriangleVelocity(float[][][] triangleVelocity) {
        this.cellVelocity = triangleVelocity;
        if (triangleVelocity[0] != null) {
            int timesNew = triangleVelocity[0].length;
            if (numberOfTimestamps > 0 && timesNew != numberOfTimestamps) {
                System.err.println(getClass() + " number of timestamps from triangleVelocity (" + timesNew + ") is not the same as existing number of timestamps (" + numberOfTimestamps + ")");
            }
            numberOfTimestamps = timesNew;
        }
//        if (!spatialInterpolationVelocity && timeInterpolatedValues) {
//            this.actualTriangleVelocity = new float[cellVelocity.length][2];
//            this.actualTriangleVelocityInitialized = new boolean[actualTriangleVelocity.length];
//        }
    }

    /**
     * Sets the undirected maximum velocity [m/s]
     *
     * @param vmax
     */
    public void setTriangleMaxVelocity(float[] vmax) {
        this.maxCellAbsVelocity = vmax;
    }

    /**
     * The maximum velocity on this triangle. Direction is not given.
     *
     * @param triangle index
     * @return velocity [m/s]
     */
    public float getTriangleMaxVelocity(int triangle) {
        return maxCellAbsVelocity[triangle];
    }

    /**
     *
     * @return
     */
    @Override
    public SurfaceRect2D clone() {

        SurfaceRect2D surf = new SurfaceRect2D(vertices.clone(), this.spatialReferenceCode);
        if (this.waterlevels != null && this.waterlevels.length > 0) {
            surf.waterlevels = this.waterlevels.clone();
            if (this.maxWaterLevels != null) {
                surf.maxWaterLevels = this.maxWaterLevels.clone();
            }
        }
        surf.geotools = this.geotools;
        surf.kst = this.kst;
        surf.numberOfTimestamps = this.numberOfTimestamps;
//        surf.recalculationIntervallMS = this.recalculationIntervallMS;
        surf.times = this.times;
        if (this.cellMids != null) {
            surf.cellMids = this.cellMids.clone();
        }

        return surf;
    }

    /**
     * If Midpoints are not set from reading a resultFile one can Calculate them
     * from Vertices' coordinates. Attention: This differs from results' read in
     * values! I do not know why.
     *
     * @return [cellID][0:x,1:y,2:z] Coordinates
     */
    public float[][] calcMidPointsFromVertexPoints() {
        float[][] mids = new float[numberCellsTotal][3];
        double fourth = 1. / 4.;
        for (int i = 0; i < numberCellsX; i++) {
            for (int j = 0; j < numberCellsY; j++) {
                for (int n = 0; n < 3; n++) {
                    mids[i + j * numberCellsX][n] += vertices[i][j][n] * fourth;
                    mids[i + j * numberCellsX][n] += vertices[i + 1][j][n] * fourth;
                    mids[i + j * numberCellsX][n] += vertices[i + 1][j + 1][n] * fourth;
                    mids[i + j * numberCellsX][n] += vertices[i][j + 1][n] * fourth;
                }

            }
        }
        return mids;
    }

    @Override
    public void setActualTime(long timelong) {
        this.setSimulationTime(timelong);
    }

    @Override
    public long getActualTime() {
        return actualTime;
    }

    @Override
    public int getActualTimeIndex() {
        return timeIndexInt;
    }

    @Override
    public double getActualTimeIndex_double() {
        return timeIndex;
    }

    @Override
    public int getTimeIndex(long timemilliseconds) {
        return times.getTimeIndex(timemilliseconds);
    }

    @Override
    public double getTimeIndexDouble(long time) {
        return times.getTimeIndexDouble(time);
    }

    @Override
    public long getStartTime() {
        return times.getFirstTime();
    }

    @Override
    public long getEndTime() {
        return times.getLastTime();
    }

    @Override
    public int getNumberOfTimes() {
        return numberOfTimestamps;
//        return times.getNumberOfTimes();
    }

    public int calcContainingTriangle(double x, double y) {
        return crawlNearestTriangle(x, y, 0);
    }

    public int calcContainingTriangleLongLat(double longitude, double latitude) {
        try {
            Coordinate utm = geotools.toUTM(longitude, latitude);
            return calcContainingTriangle(utm.x, utm.y);
        } catch (TransformException ex) {
            Logger.getLogger(SurfaceRect2D.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    /**
     * Checks every triangle for contining the given point. The test via
     * barycnetric coordinates is only done, when the midpoint distance is below
     * the minDistance parameter.
     *
     * @param x
     * @param y
     * @param minDistance should be around maximum edge length
     * @return
     */
    public int findContainingTriangle(double x, double y, double minDistance) {
        int id = getCellIDforPosition(x,y);
        if(id<0||id>=numberCellsTotal)return -1;
        return id;
    }

    public int crawlNearestTriangle(double x, double y, int startTriangleID) {
       return findContainingTriangle(x, y, 1);
    }

    public TimeIndexContainer getTimes() {
        return times;
    }

    public int getMaxTriangleID() {
        return numberCellsTotal-1;
    }

    /**
     * * Calculates the velocity [x,y] on the surface at the position of the
     * particle.
     *
     * @param p
     * @param triangleID
     * @return
     */
    public double[] getParticleVelocity2D(Particle p, int triangleID) {
        return getParticleVelocity2D(p, triangleID, null, null);
    }

    /**
     * Calculates the velocity [x,y] on the surface at the position of the
     * particle.
     *
     * @param p
     * @param triangleID
     * @param tofillVelocity can be given to prevent allocation
     * @param tofillBarycentric can be given to prevent allocation
     * @return
     */
    public double[] getParticleVelocity2D(Particle p, int triangleID, double[] tofillVelocity, double[] tofillBarycentric) {
            if (timeInterpolatedValues) {
//                if (actualVelocity == null) {
//                    actualVelocity = new double[triangleNodes.length][];
//                }
                if (!actualVelocitySet[triangleID]) {
                    synchronized (actualVelocity[triangleID]) {
                        if (!actualVelocitySet[triangleID]) {
                            float[] vt_t = getTriangleVelocity(triangleID, timeIndexInt);//triangleID, timeIndexInt, (float) timeFrac, toFillSurfaceVelocity[0][0]);
                            float[] vt_tp = getTriangleVelocity(triangleID, timeIndexInt + 1);//triangleID, timeIndexInt, (float) timeFrac, toFillSurfaceVelocity[0][0]);
//                            tofillVelocity[0] = vt_t[0] + (vt_tp[0] - vt_t[0]) * timeFrac;
//                            tofillVelocity[1] = vt_t[1] + (vt_tp[1] - vt_t[1]) * timeFrac;
                            actualVelocity[triangleID][0] = vt_t[0] + (vt_tp[0] - vt_t[0]) * timeFrac;
                            actualVelocity[triangleID][1] = vt_t[1] + (vt_tp[1] - vt_t[1]) * timeFrac;

//                            actualVelocity[triangleID][0] = tofillVelocity[0];
//                            actualVelocity[triangleID][1] = tofillVelocity[1];
                            actualVelocitySet[triangleID] = true;
                            actualVelocityUsed = true;
                        }
                    }
                }
                tofillVelocity[0] = actualVelocity[triangleID][0];
                tofillVelocity[1] = actualVelocity[triangleID][1];

            } else {
                float[] vt_t = getTriangleVelocity(triangleID, timeIndexInt);
                tofillVelocity[0] = vt_t[0];
                tofillVelocity[1] = vt_t[1];
            }
            return tofillVelocity;
    }

    /**
     * Returns the target triangle id or a BoundHitException with corrected
     * coordinates if the particle could not move out of the triangle
     *
     * @param p Particle
     * @param id startTriangleID
     * @param xold
     * @param yold
     * @param x calculated new Position with unknown particle id
     * @param y
     * @param leftIterations max number of iterations that shall be computet
     * @param bw temporary array for barycentric weight caching [3]
     * @param t temporary array for triangle vertex coords [3][3]
     * @return id of the new triangle
     */
    public int getTargetTriangleID(Particle p, int id, double xold, double yold, double x, double y, int leftIterations, double[] bw, double[][] t) /*throws BoundHitException*/ {
        return findContainingTriangle(x, y, 1);
    }

//    private double[] getParticleBoundaryIntersection(double[] a, double bx, double by, double[] c, double dx, double dy) {
//        double t = ((dx - bx) * (by - a[1]) + (dy - by) * (bx - a[0])) / ((c[1] - a[1]) * (dx - bx) - (c[0] - a[0]) * (dy - by));
////        double t = ((dx - bx) * (by - a[1]) + (dy - by) * (a[0] - bx)) / ((c[1] - a[1]) * (dx - bx) - (c[0] - a[0]) * (dy - by));
//        double[] s = new double[2];
//        double[] s_out = new double[2];
//        s[0] = bx + t * (dx - bx);
//        s[1] = by + t * (dy - by);
//        s_out[0] = bx + t * 0.9 * (dx - bx);
//        s_out[1] = by + t * 0.9 * (dy - by);
//        // check if intersection is on triangle side and if its in reach of the particlepath:
//        if (a[0] < c[0] && s[0] >= a[0] && s[0] <= c[0]) {
//            if (bx < dx && s[0] >= bx && s[0] <= dx) {
//                return s_out;
//            } else if (bx > dx && s[0] >= dx && s[0] <= bx) {
//                return s_out;
//            } else {
//                s[0] = -1;
//                return s;
//            }
//        } else if (a[0] > c[0] && s[0] >= c[0] && s[0] <= a[0]) {
//            if (bx > dx && s[0] >= dx && s[0] <= bx) {
//                return s_out;
//            } else if (bx < dx && s[0] >= bx && s[0] <= dx) {
//                return s_out;
//            } else {
//                s[0] = -1;
//                return s;
//            }
//        } else {
//            s[0] = -1;
//            return s;
//        }
//    }


    public Inlet[] getInlets() {
        return inletArray;
    }
    public SurfaceMeasurementRaster getMeasurementRaster() {
        return measurementRaster;
    }

   

    public void setTimeContainer(TimeIndexContainer times) {
//        System.err.println("Surface.setTimeContainer: "+times);
        this.times = times;
        if (this.measurementRaster != null) {
            this.measurementRaster.setTimeContainer(times);
        }
    }

    public void setMeasurementRaster(SurfaceMeasurementRaster measurementRaster) {
        this.measurementRaster = measurementRaster;
        if (measurementRaster != null) {
            if (measurementRaster.getIndexContainer() == null && times != null) {
                this.measurementRaster.setTimeContainer(times);
            }
            this.measurementRaster.setNumberOfMaterials(numberOfMaterials);
        }
    }

    @Override
    public Connection[] getConnections() {
        return new Connection[0];
    }

    @Override
    public double getCapacityVolume() {
        return 0;
    }

    @Override
    public double getFluidVolume() {
        return 0;
    }

    @Override
    public void setMeasurementTimeLine(MeasurementTimeline tl) {

    }

    @Override
    public MeasurementTimeline getMeasurementTimeLine() {
        return null;
    }

    @Override
    public double getWaterHeight() {
        return 0;
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return new Position3D(0, 0, 0, 0, 0);
    }

    @Override
    public double getWaterlevel() {
        return 0;

    }

    @Override
    public String getName() {
       return "Surface";
    }

    public class BoundHitException extends Exception {

        public final int id;
        public final double correctedPositionX, correctedPositionY;

        public BoundHitException(int id, double correctedPositionX, double correctedPositionY) {
            this.id = id;
            this.correctedPositionX = correctedPositionX;
            this.correctedPositionY = correctedPositionY;
        }

    }

    public int triangleIDNear(GeoPosition2D geoposition) throws TransformException {
        Coordinate utm = geotools.toUTM(geoposition);
        return triangleIDNear(utm.x, utm.y);
    }

    public int triangleIDNear(double x, double y) {
        if (cellMids == null) {
            return -2;
        }
        int bestID = -1;
        double bestdistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < cellMids.length; i++) {
            double[] tm = cellMids[i];
            double tempdist = Math.abs(tm[0] - x) + Math.abs(tm[1] - y);
            if (tempdist < bestdistance) {
                bestdistance = tempdist;
                bestID = i;
            }
        }
        return bestID;
    }

    /**
     * The negative gradient of the surface. This is the flow direction under
     * gravitation influence. This is used for transport on dry surface
     * condition. (when there is no velocity calculated. The values are
     * normalised to a length of 1.
     *
     * @return
     */
    public float[][] getTriangle_downhilldirection() {
        return cell_downhill;
    }

}
