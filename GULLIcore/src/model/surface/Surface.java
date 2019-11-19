package model.surface;

import model.surface.measurement.SurfaceMeasurementRaster;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;
import control.StartParameters;
import control.maths.GeometryTools;
import io.extran.HE_GDB_IO;
import io.extran.HE_InletReference;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.GeoPosition2D;
import model.GeoTools;
import model.particle.Particle;
import model.surface.measurement.SurfaceMeasurementTriangleRaster;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.TimeIndexCalculator;
import model.timeline.array.TimeIndexContainer;
import model.topology.Capacity;
import model.topology.Connection;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position3D;
import model.topology.graph.Pair;
import model.topology.profile.CircularProfile;
import org.opengis.referencing.operation.TransformException;

/**
 * Containing all information about surface topology. - triangle indices,
 * positions, neumannNeighbours. - 1D transport Paths.
 *
 * @author saemann
 */
public class Surface extends Capacity implements TimeIndexCalculator {

    public File fileTriangles, fileWaterlevels;
    public boolean loadingTriangles, loadingWaterlevels;

    public final double[][] vertices;  //UTM
    public final int[][] triangleNodes;
    private double[][] triangleMids;

    /**
     * Indices of von Neumann (always 3) Neighbour Triangles.
     */
    public int[][] neumannNeighbours;

    /**
     * Variable number of triangles that have at least one node in common with
     * the indexed triangle [triangleindex][variable number of neighbours]:
     * neighbour triangle id
     */
    public int[][] mooreNeighbours;
    /**
     * [nodeindex][Neighbour number] : triangleIndex
     */
    private int[][] NodeNeighbours;
    public double[][] weight;

    public boolean calculateWeighted = false;

    private float[][] neighbourDistances;
    public final HashMap<Integer, SurfaceTrianglePath[]> paths;

    volatile protected SurfaceMeasurementRaster measurementRaster;

    /**
     * edgelength between neumannNeighbours. [triangleId][nbIndex0-2]: length in
     * meter.
     */
    private float[][] edgeLength;

    private float[] triangleArea;

//    public final HashMap<Integer, SurfaceTriangle> triangleCapacity;
    public SurfaceTriangle[] triangleCapacitys;
    public final HashMap<Integer, Integer> mapIndizes;

    private int numberOfTimestamps;

    private float[][] waterlevels; //[triangle][timeindex]
    private double[] maxWaterLevels;

    public SurfaceWaterlevelLoader waterlevelLoader;

    public SurfaceVelocityLoader velocityLoader;

    /**
     * [triangle][neighbour 3][timeindex]
     */
    private float[][][] neighbourvelocity;
    /**
     * [triangle][timeindex][direction(0:x,1:y)]
     */
    private float[][][] triangleVelocity;
    /**
     * [triangle][neighbour index(0,1,2)]
     */
    private float[][] maxNeighbourVelocity;
    /**
     * [timeindex][triangle] resultdirection
     */
    private float[] maxTriangleVelocity;
    /**
     * [vertices][times][2 (x,y)]
     */
    private float[][][] velocityNodes;

    private float[] zeroVelocity = new float[2];

    public int status = -1, vstatus = -1;

    /**
     * Velocities for actual timestep
     */
//    private final HashMap<Integer, float[]> actualVelocity = new HashMap<>(0);
    /**
     * time when a new timeindex needs to be calculated.
     */
    private long nextRecalculation;

    /**
     * Within this timespan the velocities are constant, because the timeindex
     * is not recalculated. Used to save computation time of recalculating
     * velocities.
     */
    private long recalculationIntervallMS = 1000;// * 60 * 2;

    protected TimeIndexContainer times;

    public boolean timeInterpolatedValues = true;

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
//    protected ConcurrentHashMap<Integer, Inlet> inlets;
    protected Inlet[] inletArray;//an array is larger but generates much less overhead compared to a hashmap. And an array is faster
    /**
     * possible Manhole on/for every triangle ID
     */
    protected Manhole[] manholes;

    public HashMap<Capacity, Integer> sourcesForSpilloutParticles = new HashMap<>();

    public boolean detailedInformationAboutIDNotFound = true;

    public HashMap<String, Capacity> capacityNames;

    /**
     *
     * @param vertices positions of nodes [nodecount][3]
     * @param triangleNodes nodes for each triangle [trianglecount][3]
     * @param neighbours neumannNeighbours for each triangle [trianglecount][3]
     * @param mapIndizes
     * @param spatialReferenceSystem
     */
    public Surface(double[][] vertices, int[][] triangleNodes, int[][] neighbours, HashMap<Integer, Integer> mapIndizes, String spatialReferenceSystem) {
        super(new CircularProfile(1));
        this.vertices = vertices;
        this.triangleNodes = triangleNodes;
        this.neumannNeighbours = neighbours;
        this.mapIndizes = mapIndizes;
        this.spatialReferenceCode = spatialReferenceSystem;
        try {
            this.geotools = new GeoTools("EPSG:4326", /*"EPSG:25832"*/ spatialReferenceSystem, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        } catch (Exception ex) {
            Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
        }
//        this.triangleCapacity = new HashMap<>(100);
//        
        measurementRaster = new SurfaceMeasurementTriangleRaster(this, numberOfMaterials, null);//new TriangleMeasurement[triangleNodes.length]; SurfaceMeasurementRectangleRaster.SurfaceMeasurementRectangleRaster(this, 1000, 1000);//

        this.paths = new HashMap<>(100);
    }

    public void initSparseTriangleVelocityLoading(SurfaceVelocityLoader velocityLoader, boolean initTriangleVelocity, boolean initNodeVelocity) {
        this.velocityLoader = velocityLoader;
        if (initTriangleVelocity) {
            this.triangleVelocity = new float[triangleNodes.length][][];//[numberOfTimestamps][2];
        }
        if (initNodeVelocity) {
            this.velocityNodes = new float[vertices.length][][];
        }
    }

    /**
     * Creates and returns a Triangle Capacity representing a triangle.
     *
     * @param id
     * @return
     */
    public SurfaceTriangle requestSurfaceTriangle(int id) {
        if (triangleCapacitys == null) {
            triangleCapacitys = new SurfaceTriangle[triangleNodes.length];
        }
        SurfaceTriangle tri = triangleCapacitys[id];//triangleCapacity.get(id);
        if (tri == null) {
            synchronized (triangleCapacitys) {
                tri = triangleCapacitys[id];
                //Test if another Thread has created this Triangle while this Threas was waiting.
                if (tri == null) {
                    tri = new SurfaceTriangle(id);
                    Coordinate c = new Coordinate(triangleMids[id][0], triangleMids[id][1]);
                    try {
                        Coordinate cll = geotools.toGlobal(c);
                        tri.setPosition(new Position3D(cll.x, cll.y, c.x, c.y, triangleMids[id][2]));
                    } catch (Exception ex) {
                        Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    triangleCapacitys[id] = tri;
                }
            }
        }
        return tri;
    }

    /**
     * Creates and returns a Triangle Capacity representing a travle path
     * between two triangles. Is Threadsafe.
     *
     * @param starttriangle
     * @param neighbourIndex
     * @return
     */
    public SurfaceTrianglePath requestSurfacePath(int starttriangle, int neighbourIndex) {
//        return null;
//        status = 1;
        synchronized (paths) {
//            status = 2;
            SurfaceTrianglePath[] localpaths = paths.get(starttriangle);
//            status = 3;
            if (localpaths == null) {
//                status = 4;
                localpaths = new SurfaceTrianglePath[3];
                paths.put(starttriangle, localpaths);
//                status = 5;
                int targetID = neumannNeighbours[starttriangle][neighbourIndex];
                if (targetID < 0) {
                    return null;
                }
//                status = 6;
                SurfaceTrianglePath path = new SurfaceTrianglePath(starttriangle, targetID, neighbourDistances[starttriangle][neighbourIndex], this);
                Coordinate c = new Coordinate(triangleMids[starttriangle][0], triangleMids[starttriangle][1]);
                try {
//                    status = 7;
                    Coordinate cll = geotools.toGlobal(c);
                    path.start = new Position3D(cll.x, cll.y, c.x, c.y, triangleMids[starttriangle][2]);
                } catch (Exception ex) {
                    Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
                }
//                status = 8;
                path.neighbourIndex = neighbourIndex;
                localpaths[neighbourIndex] = path;
//                status = 9;
                return path;
//            
            }
//            status = 10;
            if (localpaths[neighbourIndex] == null) {
                int targetID = neumannNeighbours[starttriangle][neighbourIndex];
//                status = 11;
                if (targetID < 0) {
                    return null;
                }
//                status = 12;
                SurfaceTrianglePath path = new SurfaceTrianglePath(starttriangle, targetID, neighbourDistances[starttriangle][neighbourIndex], this);
                Coordinate c = new Coordinate(triangleMids[starttriangle][0], triangleMids[starttriangle][1]);
                try {
                    Coordinate cll = geotools.toGlobal(c);
//                    System.out.println(getClass()+"requestsurfacepath  cll.x="+cll.x);
                    path.start = new Position3D(cll.x, cll.y, c.x, c.y, triangleMids[starttriangle][2]);
                } catch (Exception ex) {
                    Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
                }
//                status = 13;
                path.neighbourIndex = neighbourIndex;
                localpaths[neighbourIndex] = path;
//                status = 14;
                return path;
            }
//            status = 15;
            return localpaths[neighbourIndex];
        }
    }

    /**
     * Calculate distances between neumannNeighbours at initialization.
     */
    protected void calculateDistances() {
        neighbourDistances = new float[neumannNeighbours.length][3];

        for (int i = 0; i < neumannNeighbours.length; i++) {

            double x0 = triangleMids[i][0];
            double y0 = triangleMids[i][1];

            for (int j = 0; j < 3; j++) {
                if (neumannNeighbours[i][j] < 0) {
                    continue;
                }
                try {
                    double x1 = triangleMids[neumannNeighbours[i][j]][0];
                    double y1 = triangleMids[neumannNeighbours[i][j]][1];

                    float distance = (float) Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));
                    neighbourDistances[i][j] = distance;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

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

    public float[][] getEdgeLength() {
        return edgeLength;
    }

    public void calcEdgeLengths() {
        edgeLength = new float[triangleNodes.length][3];
        for (int id = 0; id < triangleNodes.length; id++) {
            for (int n = 0; n < 3; n++) {
                int nbID = neumannNeighbours[id][n];
                if (nbID < 0) {
                    edgeLength[id][n] = 0;
                    continue;
                }
                int v0 = -1;
                int v1 = -1;
                for (int i = 0; i < 3; i++) {
                    int t0index = triangleNodes[id][i];
                    for (int j = 0; j < 3; j++) {
                        int n0index = triangleNodes[nbID][j];
                        if (t0index == n0index) {
                            if (v0 < 0) {
                                v0 = t0index;
                                break;
                            } else if (v1 < 0) {
                                v1 = t0index;
                                break;
                            }
                        }
                    }
                    if (v1 >= 0) {
                        break;
                    }
                }
                if (v0 >= 0 && v1 >= 0) {
                    //Two common points found. calculate distance
                    double[] pos0 = vertices[v0];
                    double[] pos1 = vertices[v1];
                    double distance = Math.sqrt((pos0[0] - pos1[0]) * (pos0[0] - pos1[0]) + (pos0[1] - pos1[1]) * (pos0[1] - pos1[1]));
                    edgeLength[id][n] = (float) distance;
                }
            }
        }
    }

    public float[] getTraingleAreas() {
        return triangleArea;
    }

    public void calcTriangleAreas() {
        triangleArea = new float[triangleNodes.length];
        for (int i = 0; i < triangleNodes.length; i++) {
            triangleArea[i] = (float) GeometryTools.trianglesArea(vertices[triangleNodes[i][0]][0], vertices[triangleNodes[i][0]][1], vertices[triangleNodes[i][1]][0], vertices[triangleNodes[i][1]][1], vertices[triangleNodes[i][2]][0], vertices[triangleNodes[i][2]][1]);
        }
    }

    public double calcTriangleArea(int id) {
        double a = 0.5 * (vertices[triangleNodes[id][0]][0] * (vertices[triangleNodes[id][1]][1] - vertices[triangleNodes[id][2]][1]) + vertices[triangleNodes[id][1]][0] * (vertices[triangleNodes[id][2]][1] - vertices[triangleNodes[id][0]][1]) + vertices[triangleNodes[id][2]][0] * (vertices[triangleNodes[id][0]][1] - vertices[triangleNodes[id][1]][1]));
        return a;
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
     * Number of triangles.
     *
     * @return
     */
    public int size() {
        return triangleNodes.length;
    }

    /**
     * Neighbour triangle index for each triangle. Might be -1 for Noneighbour.
     * [triangleIndex][1st Neighbour,2nd NB,3rd NB]
     *
     * @return
     */
    public int[][] getNeighbours() {
        return neumannNeighbours;
    }

    public void setNeighbours(int[][] neighbours) {
        this.neumannNeighbours = neighbours;
    }

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
        return triangleMids;
    }

    /**
     * Set center points of triangles as they may vary to calculated ones for
     * the velocity calculation.
     *
     * @param triangleMids
     */
    public void setTriangleMids(double[][] triangleMids) {
        if (triangleMids.length != this.triangleNodes.length) {
            System.out.println("Surface: Triangle Mid Points trying to be set (" + triangleMids.length + ") have not the same size as number of triangles (" + this.triangleNodes.length + ")");
        }
        this.triangleMids = triangleMids;
        calculateDistances();
    }

    public void calculateNeighbourVelocitiesFromWaterlevels() {
//        System.out.println(getClass() + "::CalculateNeighborVelocityFromWaterlevels");
        int wltimes = waterlevels[0].length;
        if (times != null) {
            int nbtimes = times.getNumberOfTimes();
            if (wltimes != nbtimes) {
                System.out.println("Waterlevels.times.length(" + wltimes + ")!= surface.times.length(" + nbtimes + ")");
            }
        }
        //Calculate velocities for maximum waterlevel
        maxNeighbourVelocity = new float[triangleMids.length][3];
        if (maxWaterLevels != null) {
            for (int i = 0; i < maxNeighbourVelocity.length; i++) {
                for (int j = 0; j < 3; j++) {
                    int neighbourIndex = neumannNeighbours[i][j];
                    if (neighbourIndex < 0) {
                        //No neighbout-> no velocity / Noflow Boundary
                        maxNeighbourVelocity[i][j] = 0;
                        continue;
                    }
                    if (maxNeighbourVelocity[i][j] > 0) {
                        continue;//Already calculated
                    }

                    //Abstand zwischen Dreiecken
                    double ds = Math.sqrt((triangleMids[i][0] - triangleMids[neighbourIndex][0]) * (triangleMids[i][0] - triangleMids[neighbourIndex][0]) + (triangleMids[i][1] - triangleMids[neighbourIndex][1]) * (triangleMids[i][1] - triangleMids[neighbourIndex][1]));

                    // Wasserstand
                    double d0 = maxWaterLevels[i];
                    double dnb = maxWaterLevels[neighbourIndex];
                    // Sohle
                    double z0 = triangleMids[i][2];
                    double znb = triangleMids[neighbourIndex][2];

                    double v = velocity(ds, z0, znb, d0, dnb);
                    maxNeighbourVelocity[i][j] = (float) v;
                }
            }
        }

        // time steps velocity
        if (waterlevels[0] != null) {
            numberOfTimestamps = waterlevels[0].length;
        }

        neighbourvelocity = new float[triangleMids.length][3][numberOfTimestamps];
        DecimalFormat df = new DecimalFormat("0.0000");
        for (int i = 0; i < maxNeighbourVelocity.length; i++) {
            initVelocityToNeighbours(i);
            for (int j = 0; j < 3; j++) {

                int neighbourIndex = neumannNeighbours[i][j];
                if (neighbourIndex < 0) {
                    continue;
                }
                //Abstand zwischen Dreiecken
                double ds = Math.sqrt((triangleMids[i][0] - triangleMids[neighbourIndex][0]) * (triangleMids[i][0] - triangleMids[neighbourIndex][0]) + (triangleMids[i][1] - triangleMids[neighbourIndex][1]) * (triangleMids[i][1] - triangleMids[neighbourIndex][1]));

                // Sohle
                double z0 = triangleMids[i][2];
                double znb = triangleMids[neighbourIndex][2];

                if (waterlevels[i] != null && waterlevels[neighbourIndex] != null) {
                    for (int t = 0; t < numberOfTimestamps; t++) {

                        // Wasserstand
                        double d0 = waterlevels[i][t];
                        double dnb = waterlevels[neighbourIndex][t];

                        double v = velocity(ds, z0, znb, d0, dnb);

//                        System.out.println("diff: " + df.format(neighbourvelocity[i][j][t] - v) + "\t dynamic:" + df.format(neighbourvelocity[i][j][t]) + "\tall:" + df.format(v));
                        neighbourvelocity[i][j][t] = (float) v;
                    }
                }
            }
        }
    }

    public void initVelocityArrayForSparseLoading(int numberOfTriangles, int numberOfTimes) {
//        this.neighbourvelocity = new float[numberOfTriangles][3][numberOfTimes];
        this.numberOfTimestamps = numberOfTimes;
        this.neighbourvelocity = new float[numberOfTriangles][][];
        this.waterlevels = new float[numberOfTriangles][];
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

    private float[] getVelocityOutflowTupelXY(long time, int triangleIndex) {
        float[] v = new float[2];

        float[] vnb = getVelocityToNeighbours(time, triangleIndex);
        for (int i = 0; i < 3; i++) {
            int nb = neumannNeighbours[triangleIndex][i];
            if (nb < 0) {
                continue;
            }
            if (v[i] <= 0) {
                continue;
            }
            double length = Math.sqrt((triangleMids[triangleIndex][0] - triangleMids[nb][0]) * (triangleMids[triangleIndex][0] - triangleMids[nb][0]) + (triangleMids[triangleIndex][1] - triangleMids[nb][1]) * (triangleMids[triangleIndex][1] - triangleMids[nb][1]));
            double x = (triangleMids[nb][0] - triangleMids[triangleIndex][0]) / length;
            double y = (triangleMids[nb][1] - triangleMids[triangleIndex][1]) / length;
            v[0] += vnb[i] * x;
            v[1] += vnb[i] * y;
        }

//        v[0]/=3.;
//        v[1]/=3.;        
        return v;

    }

    /**
     * @param time
     * @param triangleIndex
     * @param neighbourIndex
     * @return
     */
    public float getVelocityToNeighbour(long time, int triangleIndex, int neighbourIndex) {
//        System.out.println("513: getvelocitytoNeighbour long");
        double index = times.getTimeIndexDouble(time);
        if (neighbourvelocity[triangleIndex] == null) {
            initVelocityToNeighbours(triangleIndex);
        }
        float v;
        if (index >= neighbourvelocity.length - 1) {
            v = (float) neighbourvelocity[triangleIndex][neighbourIndex][neighbourvelocity[triangleIndex][neighbourIndex].length - 1];
        } else {
            v = (float) (neighbourvelocity[triangleIndex][neighbourIndex][(int) index] * (1 - index % 1) + neighbourvelocity[triangleIndex][neighbourIndex][(int) index + 1] * (index % 1));
        }

        return v;
    }

    /**
     * Velocity to neighbour at given timeindex [m/s]
     *
     * @param timeIndex
     * @param triangleIndex
     * @param neighbourIndex
     * @return
     */
    public float getVelocityToNeighbour(int timeIndex, int triangleIndex, int neighbourIndex) {
//        System.out.println("537: getvelocitytoNeighbour int");
        if (neighbourvelocity[triangleIndex] == null) {
            initVelocityToNeighbours(triangleIndex);
        }
        try {
            float v = neighbourvelocity[triangleIndex][neighbourIndex][timeIndex];

            return v;
        } catch (IndexOutOfBoundsException e) {
            System.err.println("triangleIndex:" + triangleIndex + "/" + neighbourvelocity.length + "   neighbour:" + neighbourIndex + "/" + neighbourvelocity[0].length + "   time:" + timeIndex + "/" + neighbourvelocity[0][0].length);
            System.err.println(neighbourvelocity.length + "," + neighbourvelocity[triangleIndex].length + "   time:" + timeIndex + neighbourvelocity[triangleIndex][neighbourIndex].length);

            return 0;
        }
    }

    private void initVelocityToNeighbours(int triangleIndex) {
        //Load velocitiy values
        if (waterlevelLoader != null) {
            synchronized (waterlevelLoader) {
                if (neighbourvelocity[triangleIndex] != null) {
                    //Seems to be loaded from another thread while this one was waiting to finish.
                    //Everything should be on its place. Can Return 
                    return;
                }
                int id = triangleIndex;
                float[] wls = this.waterlevels[id];
                if (wls == null) {
                    try {
                        wls = waterlevelLoader.loadWaterlevlvalues(id);
                        this.waterlevels[id] = wls;
                        this.triangleMids[id][2] = waterlevelLoader.loadZElevation(id);
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.waterlevels[id] = new float[numberOfTimestamps];
                    }
                }
                neighbourvelocity[id] = new float[3][numberOfTimestamps];
                if (wls == null) {
                    //This triangle Id has net been in the database. Initialize as 0 values
//                    System.err.println("waterlevels on "+id+" == null");
                } else {
                    //Neighbour values
                    for (int n = 0; n < 3; n++) {
                        int nbID = neumannNeighbours[id][n];
                        if (nbID < 0) {
                            continue;
                        }

                        //Load neighbour if it not yet loaded.
                        float[] wlsNB = this.waterlevels[nbID];
                        if (wlsNB == null) {
                            wlsNB = waterlevelLoader.loadWaterlevlvalues(nbID);
                            this.waterlevels[nbID] = wlsNB;
                            try {
                                this.triangleMids[nbID][2] = waterlevelLoader.loadZElevation(nbID);
                            } catch (NullPointerException e) {
                                //Triangle with this id is not found in database.
                            }
                        }
                        double ds = neighbourDistances[id][n];
//                ds = Math.sqrt((triangleMids[id][0] - triangleMids[nbID][0]) * (triangleMids[id][0] - triangleMids[nbID][0]) + (triangleMids[id][1] - triangleMids[nbID][1]) * (triangleMids[id][1] - triangleMids[nbID][1]));

                        //Calculate velocities
                        for (int t = 0; t < wls.length; t++) {
                            neighbourvelocity[id][n][t] = (float) velocity(ds, triangleMids[id][2], triangleMids[nbID][2], wls[t], wlsNB[t]);
                        }
                    }
                }
            }
        }
    }

    public float[] loadWaterlevels(int triangleID) {
        if (waterlevels == null) {
            waterlevels = new float[triangleNodes.length][];
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
            this.triangleMids[triangleID][2] = waterlevelLoader.loadZElevation(triangleID);
        } catch (NullPointerException e) {
            //Triangle with this id is not found in database.
        }

        return wlsNB;
    }

    /**
     * von Neumann Neighbours' distances , triangles midpoint
     *
     * @return [triangleid][nb index]: midpoint-distance
     */
    public float[][] getNeighbourDistances() {
        return neighbourDistances;
    }

    /**
     * Velocity to neighbour at actual time. [m/s]
     *
     * @param triangleIndex
     * @param neighbourIndex
     * @return
     */
    public double getVelocityToNeighbour(int triangleIndex, int neighbourIndex) {
        if (neighbourvelocity[triangleIndex] == null) {
            initVelocityToNeighbours(triangleIndex);
        }
        return (neighbourvelocity[triangleIndex][neighbourIndex][(int) timeIndex] * (1 - timeIndex % 1) + neighbourvelocity[triangleIndex][neighbourIndex][(int) timeIndex + 1] * (timeIndex % 1));

    }

    /**
     * Velocity to neighbour at given Simulationtime
     *
     * @param time
     * @param triangleIndex
     * @return float[3] has to be divided by 3
     */
    private float[] getVelocityToNeighbours(long time, int triangleIndex) {
        float[] entry = new float[3];
        double index = times.getTimeIndexDouble(time);
        if (neighbourvelocity[triangleIndex] == null) {
            initVelocityToNeighbours(triangleIndex);
        }

        for (int i = 0; i < entry.length; i++) {
            if (entry[i] == 0) {
                float v = (float) (neighbourvelocity[triangleIndex][i][(int) index] * (1 - index % 1) + neighbourvelocity[triangleIndex][i][(int) index + 1] * (index % 1));
                entry[i] = v;
            }
        }
        return entry;
    }

//    public float[][][] getNeighbourVelocity() {
//        return neighbourvelocity;
//    }
    public boolean isNeighbourVelocityLoaded() {
        return neighbourvelocity != null;
    }

    /**
     * Velocity to neighbour at given timeindex
     *
     * @param timeIndex
     * @param triangleIndex
     * @return [3] velocity to neighbour.
     */
    private float[] getVelocityToNeighbours(double timeIndex, int triangleIndex) {
        float[] entry = new float[3];
        double index = timeIndex;
        for (int i = 0; i < entry.length; i++) {
            if (entry[i] == 0) {
                float v = (float) (neighbourvelocity[triangleIndex][i][(int) index] * (1 - index % 1) + neighbourvelocity[triangleIndex][i][(int) index + 1] * (index % 1));
                entry[i] = v;
            }
        }
        return entry;
    }

    /**
     * Get three velocities to neumannNeighbours at actual time.
     *
     * @param triangleIndex
     * @return [3] velocities to neighbour.
     */
    private float[] getVelocityToNeighbours(int triangleIndex) {
        float[] entry = new float[3];
        double index = this.timeIndex;
        for (int i = 0; i < entry.length; i++) {
            if (entry[i] == 0) {
                float v = (float) (neighbourvelocity[triangleIndex][i][(int) index] * (1 - index % 1) + neighbourvelocity[triangleIndex][i][(int) index + 1] * (index % 1));
                entry[i] = v;
            }
        }
        return entry;
    }

    /**
     * @deprecated @param relativetime
     * @return
     */
    public float[][] getVelocityTupelXYall(double relativetime) {
        float[][] v = new float[triangleMids.length][2];
        if (relativetime < 0 || relativetime > numberOfTimestamps - 1) {
            return v;
        }
        int timeindex = (int) relativetime;
        double anteil0 = 1 - relativetime % 1, anteil1 = relativetime % 1;

        for (int triangleIndex = 0; triangleIndex < v.length; triangleIndex++) {

            for (int i = 0; i < 3; i++) {
                int nb = neumannNeighbours[triangleIndex][i];
                if (nb < 0) {
                    continue;
                }
                double length = Math.sqrt((triangleMids[triangleIndex][0] - triangleMids[nb][0]) * (triangleMids[triangleIndex][0] - triangleMids[nb][0]) + (triangleMids[triangleIndex][1] - triangleMids[nb][1]) * (triangleMids[triangleIndex][1] - triangleMids[nb][1]));
                double x = (triangleMids[nb][0] - triangleMids[triangleIndex][0]) / length;
                double y = (triangleMids[nb][1] - triangleMids[triangleIndex][1]) / length;
                v[triangleIndex][0] += (neighbourvelocity[triangleIndex][i][timeindex] * anteil0 + neighbourvelocity[triangleIndex][i][timeindex + 1] * anteil1) * x;
                v[triangleIndex][1] += (neighbourvelocity[triangleIndex][i][timeindex] * anteil0 + neighbourvelocity[triangleIndex][i][timeindex + 1] * anteil1) * y;
            }
        }
        return v;
    }

    /**
     * Calculate Triangle Velocities (on mid point) from neighbour velocities.
     */
    public void calcTriangleVelocityFromNeighbourVelocity() {
        float[][][] velocity = new float[triangleNodes.length][numberOfTimestamps][2];
        double nxLength, nyLength;//sum of normal in x/y direction
        double[][] normals = new double[3][2]; //[neighbourindex][0:x,1:y];
//        double[] v=new double[3];
        for (int i = 0; i < triangleNodes.length; i++) {
            nxLength = 0;
            nyLength = 0;
            //First calculate lengths of normal vector
            for (int n = 0; n < 3; n++) {
                int nb = neumannNeighbours[i][n];
                if (nb < 0) {
//                    normals[n][0] = 0;
//                    normals[n][1] = 0;

                    continue;
                }
                double dx = triangleMids[nb][0] - triangleMids[i][0];
                double dy = triangleMids[nb][1] - triangleMids[i][1];
                //Normalisieren
                double length = Math.sqrt(dx * dx + dy * dy);
                dx = dx / length;
                dy = dy / length;
                normals[n][0] = dx;
                normals[n][1] = dy;
                nxLength += Math.abs(dx);
                nyLength += Math.abs(dy);
            }
            //Second add weighted x/y velocity components
            for (int n = 0; n < 3; n++) {
                int nb = neumannNeighbours[i][n];
                if (nb < 0) {
                    continue;
                }
                //weight of this component 
                double fx = Math.abs(normals[n][0]) / nxLength;
                double fy = Math.abs(normals[n][1]) / nyLength;

                //Calculate velocities for every timeindex
                for (int t = 0; t < numberOfTimestamps; t++) {
                    float vnb = neighbourvelocity[i][n][t];
                    velocity[i][t][0] += vnb * normals[n][0] * fx;
                    velocity[i][t][1] += vnb * normals[n][1] * fy;
                }

            }
        }
        this.triangleVelocity = velocity;
    }

    /**
     * Calculate Triangle's Velocities (on mid point) from neighbour velocities
     * for all timesteps. Result ist stored in the triangleVelocity array.
     *
     * @param triangleID triangle, thats velocity is calculated from neighbours.
     */
    public void calcTriangleVelocityFromNeighbourVelocity(int triangleID) {

        if (triangleVelocity[triangleID] == null) {
            triangleVelocity[triangleID] = new float[numberOfTimestamps][2];
        }

        double nxLength, nyLength;//sum of normal in x/y direction
        double[][] normals = new double[3][2]; //[neighbourindex][0:x,1:y];
//        double[] v=new double[3];
        int i = triangleID;
        nxLength = 0;
        nyLength = 0;
        //First calculate lengths of normal vector
        for (int n = 0; n < 3; n++) {
            int nb = neumannNeighbours[i][n];
            if (nb < 0) {
//                    normals[n][0] = 0;
//                    normals[n][1] = 0;

                continue;
            }
            double dx = triangleMids[nb][0] - triangleMids[i][0];
            double dy = triangleMids[nb][1] - triangleMids[i][1];
            //Normalisieren
            double length = Math.sqrt(dx * dx + dy * dy);
            dx = dx / length;
            dy = dy / length;
            normals[n][0] = dx;
            normals[n][1] = dy;
            nxLength += Math.abs(dx);
            nyLength += Math.abs(dy);
        }
        //Second add weighted x/y velocity components
        for (int n = 0; n < 3; n++) {
            int nb = neumannNeighbours[i][n];
            if (nb < 0) {
                continue;
            }
            //weight of this component 
            double fx = Math.abs(normals[n][0]) / nxLength;
            double fy = Math.abs(normals[n][1]) / nyLength;

            //Calculate velocities for every timeindex
            for (int t = 0; t < numberOfTimestamps; t++) {
                float vnb = neighbourvelocity[i][n][t];
                triangleVelocity[i][t][0] += vnb * normals[n][0] * fx;
                triangleVelocity[i][t][1] += vnb * normals[n][1] * fy;
            }

        }

    }

//    /**
//     * Calculates velocity on given triangle from its to-neighbour-velocities
//     *
//     * @param timeindex
//     * @param triangleindex
//     * @return float[x,y] velocity in x/y direction
//     */
//    public float[] calcTriangleVelocityFromNeighbourVelocity(int timeindex, int triangleindex) {
//        int vtimes = neighbourvelocity.length;
//        int nbtimes = times.getNumberOfTimes();
//        if (vtimes != nbtimes) {
//            System.out.println("neighbourvelocity.times.length(" + vtimes + ")!= surface.times.length(" + nbtimes + ")");
//        }
//
//        float[] v = new float[2];// x/y
//        int numberOfNeighbours = 0;
//        for (int i = 0; i < 3; i++) {
//            //Every neighbour 
//            int nb = neumannNeighbours[triangleindex][i];
//            if (nb < 0) {
//                //no neighbour
//                continue;
//            }
//            numberOfNeighbours++;
//            //Find distance
//            double dx = triangleMids[nb][0] - triangleMids[triangleindex][0];
//            double dy = triangleMids[nb][1] - triangleMids[triangleindex][1];
//            //Normalisieren
//            double length = Math.sqrt(dx * dx + dy * dy);
//            dx = dx / length;
//            dy = dy / length;
//            //Geschwindigkeiten aufsummieren
//            float vnb = neighbourvelocity[triangleindex][i][timeindex];
//            v[0] += dx * vnb;
//            v[1] += dy * vnb;
//        }
//        v[0] = v[0] / (float) numberOfNeighbours;
//        v[1] = v[1] / (float) numberOfNeighbours;
//
//        return v;
//    }
    /**
     *
     * @param timeindex
     * @param triangleindex
     * @param neighbourindex 0,1,2
     * @return
     */
    public float calcNeighbourVelocityFromTriangleVelocity(int timeindex, int triangleindex, int neighbourindex) {
        int nb = neumannNeighbours[triangleindex][neighbourindex];
        if (nb < 0) {
            return 0; //Noflow boundary
        }
        float dx = (float) (triangleMids[nb][0] - triangleMids[triangleindex][0]);
        float dy = (float) (triangleMids[nb][1] - triangleMids[triangleindex][1]);
//        double length=Math.sqrt(dx*dx+dy*dy);
//        //Normalize
//        dx/=length;
//        dy/=length;
        float[] velocity0 = triangleVelocity[triangleindex][timeindex];
        float[] velocityNB = triangleVelocity[nb][timeindex];

        float vx = (velocity0[0] + velocityNB[0]) * 0.5f;
        float vy = (velocity0[1] + velocityNB[1]) * 0.5f;
        //Projektion auf abstandsvektor
        float factor = ((vx * dx + vy * dy) / (dx * dx + dy * dy));
        double[] vres = new double[]{factor * dx, factor * dy};
        //Orientation of new vektor to directed normal vector
        float signum = 1;
        if (vx != 0 || vy != 0) {
            float angle = (float) (vx * dx + vy * dy);
//            System.out.println("angle: " + angle);
            signum = Math.signum(angle);
        }
        return (float) (signum * Math.sqrt(vres[0] * vres[0] + vres[1] * vres[1]));
    }

    /**
     * Converts triangle centered velocities to neighbour velocities. overrides
     * neighbour velocities if existant.
     */
    public void calcNeighbourVelocityFromTriangleVelocity() {
//        this.numberOfTimestamps = this.triangleVelocity[0].length;
        this.neighbourvelocity = new float[triangleNodes.length][3][numberOfTimestamps];
        for (int t = 0; t < numberOfTimestamps; t++) {
            for (int i = 0; i < triangleNodes.length; i++) {
                for (int j = 0; j < 3; j++) {
                    this.neighbourvelocity[i][j][t] = calcNeighbourVelocityFromTriangleVelocity(t, i, j);
                }
            }
        }
    }

    // get velocities at the triangle nodes via neighbouring weights 
    public void calculateVelocities2d() {
        long start = System.currentTimeMillis();
        calcTriangleVelocityFromNeighbourVelocity();
        long dura = System.currentTimeMillis() - start;
        //System.out.println("triangleVeloCalc took: "+ dura/1000 + " s. With " + triangleVelocity);

        velocityNodes = new float[NodeNeighbours.length][numberOfTimestamps][2];
        //System.out.println(getClass() + ":: calculateVelocity2d: Nodeneighbours:" + NodeNeighbours);
        for (int j = 0; j < NodeNeighbours.length; j++) {                   //welche vertice
            if (NodeNeighbours[j] == null) {
                for (int t = 0; t < numberOfTimestamps; t++) {//wann
                    for (int k = 0; k < 2; k++) {  //x und y                      
                        velocityNodes[j][t][k] = 0; //triangleVelo: [triangle][timeindex][direction(0:x,1:y)]
                    }
                }
                continue;
            }
            for (int n = 0; n < NodeNeighbours[j].length; n++) {        //welches triangle an vertice
                if (NodeNeighbours[j][n] >= 0) {
                    int nbindex;
                    if (this.mapIndizes == null) {
                        nbindex = NodeNeighbours[j][n];
                    } else {
                        Integer ninteger = mapIndizes.get(NodeNeighbours[j][n]);
                        if (ninteger == null) {
                            continue;
                        }
                        nbindex = ninteger.intValue();
                        if (nbindex < 0) {
                            continue;
                        }
                    }
                    for (int t = 0; t < numberOfTimestamps; t++) {//wann
                        for (int k = 0; k < 2; k++) {  //x und y                      
                            velocityNodes[j][t][k] += triangleVelocity[nbindex][t][k] * weight[j][n]; //triangleVelo: [triangle][timeindex][direction(0:x,1:y)]
                        }
                    }
                }
            }
        }
        //getmeanvelo2dNodes(velocityNodes);
    }

    /**
     * Get the velocity for this triangle. if it is not yet known. the
     * information is loaded via the velocityloader connected for this surface.
     *
     * @param triangleID
     * @return float[times][2:x,y] velocity (m/s)
     */
    private float[][] getTriangleVelocity(int triangleID) {
        if (triangleVelocity[triangleID] == null) {
            try {
                triangleVelocity[triangleID] = velocityLoader.loadVelocity(triangleID);
            } catch (Exception e) {
                //Id Not found or equal exception. This triangle has no velocity information -> set everything to zero.
                triangleVelocity[triangleID] = new float[numberOfTimestamps][2];
            }
        }
        return triangleVelocity[triangleID];
    }

    public float[] getTriangleVelocity(int triangleID, double indexDouble) {
        return getTriangleVelocity(triangleID, indexDouble, null);
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
     * get velocities at the triangle nodes via neighbouring weights
     *
     * @param nodeID
     */
    public void loadSparseNodeVelocity2D(int nodeID) {
        if (velocityNodes == null) {
            velocityNodes = new float[NodeNeighbours.length][][];
        }
        for (int n = 0; n < NodeNeighbours[nodeID].length; n++) {        //welches triangle an vertice
            if (NodeNeighbours[nodeID][n] >= 0) {
                int triangleID;
                if (this.mapIndizes == null) {
                    triangleID = NodeNeighbours[nodeID][n];
                } else {
                    Integer ninteger = mapIndizes.get(NodeNeighbours[nodeID][n]);
                    if (ninteger == null) {
                        continue;
                    }
                    triangleID = ninteger.intValue();
                    if (triangleID < 0) {
                        continue;
                    }
                }
                if (triangleVelocity[triangleID] == null) {

                    if (velocityLoader != null) {
                        getTriangleVelocity(triangleID);
                    } else if (waterlevelLoader != null) {
                        //Load waterlevel and calculate velocity
                        initVelocityToNeighbours(triangleID);
                        calcTriangleVelocityFromNeighbourVelocity(triangleID);
                    }
                }
                velocityNodes[nodeID] = new float[numberOfTimestamps][2];
                for (int t = 0; t < numberOfTimestamps; t++) {//wann
                    for (int k = 0; k < 2; k++) {  //x und y         
//                        System.out.println("weight.length="+weight[nodeID].length);
//                        System.out.println("velocitynodes.length1: "+velocityNodes[nodeID].length+"\t 2: "+velocityNodes[nodeID][0].length);
//                        System.out.println("triangleVelocity[triangleID][t][k]:"+triangleVelocity[triangleID].length+"\t2: "+triangleVelocity[triangleID][t].length);
//                        System.out.println("triangleNode "+nodeID+"  ="+velocityNodes[nodeID][t][k]+" + "+triangleVelocity[triangleID][t][k]+" * "+weight[nodeID][n]);
                        velocityNodes[nodeID][t][k] += triangleVelocity[triangleID][t][k] * weight[nodeID][n]; //triangleVelo: [triangle][timeindex][direction(0:x,1:y)]
                    }
                }
            }
        }

//        }
        //getmeanvelo2dNodes(velocityNodes);
    }

    /**
     * Project the Traingle velocity on the neighbour-normal vector by using
     * rectangular angle on the triangle-velocity vector. Gives higher values
     * than a projection rectangular on the neighbour-vector.
     *
     * @param timeindex
     * @param triangleindex
     * @param neighbourindex 0,1,2
     * @return
     */
    public float calcNeighbourVelocityFromTriangleVelocityOuterProjection(int timeindex, int triangleindex, int neighbourindex) {
        int nb = neumannNeighbours[triangleindex][neighbourindex];
        if (nb < 0) {
            return 0; //Noflow boundary
        } //Triangle velocity 
        float[] velocity0 = triangleVelocity[timeindex][triangleindex];
        float[] velocityNB = triangleVelocity[timeindex][nb];
        float vx = (velocity0[0]);// + velocityNB[0]) * 0.5f;
        float vy = (velocity0[1]);// + velocityNB[1]) * 0.5f;
        float lengthVT = (float) Math.sqrt(vx * vx + vy * vy);
        if (lengthVT == 0) {
            return 0;
        }

        //Normalvector to neighbour
        float nx = (float) (triangleMids[nb][0] - triangleMids[triangleindex][0]);
        float ny = (float) (triangleMids[nb][1] - triangleMids[triangleindex][1]);
        float lengthNN = (float) Math.sqrt(nx * nx + ny * ny);
        //Normalize
//        nx /= lengthNN;
//        ny /= lengthNN;

        //Angle between normal and triangle velocity
        float cos = (nx * vx + ny * vy) / (lengthNN * lengthVT);
        float factorNonT;
        float lengthVN;
        float lengthPN;

        if (true/*cos < 0.6f&&cos>-0.6*/) {
            //angle is very stub. small projection
            factorNonT = (vx * nx + vy * ny) / (nx * nx + ny * ny);
            lengthVN = lengthNN * factorNonT;
        } else {

            //Projection of neighbour onto triangle-velocity vector (orthogonal to triangle-velocity vector)
            //searnch
            factorNonT = (vx * nx + vy * ny) / (vx * vx + vy * vy);
            float px = factorNonT * vx;
            float py = factorNonT * vy;

            lengthPN = lengthVT * factorNonT;

            //Length VN
            lengthVN = lengthNN * lengthVT / lengthPN;
            if (lengthVN > 10 || lengthVN < -10) {
                System.out.println("v=" + lengthVN + " m/s\tcos:" + cos);
            }
        }
        if (Float.isNaN(lengthVN)) {
            System.out.println(getClass() + ":: velovity:" + lengthVN + "  lengthNN=" + lengthNN + "   lengthVT=" + lengthVT + "  TriangleID:" + triangleindex + " NBindex:" + nb + " x0 " + triangleMids[triangleindex][0] + " xn=" + triangleMids[nb][0] + " y0 " + triangleMids[triangleindex][1] + " yn=" + triangleMids[nb][1]);
        }

        return lengthVN;
    }

    /**
     * Project the Traingle velocity on the neighbour-normal vector by using
     * rectangular angle on the triangle-velocity vector. Gives higher values
     * than a projection rectangular on the neighbour-vector.
     */
    public void calcNeighbourVelocityFromTriangleVelocityOuterProjection() {
        this.numberOfTimestamps = this.triangleVelocity.length;
        this.neighbourvelocity = new float[triangleNodes.length][3][numberOfTimestamps];

        for (int i = 0; i < triangleNodes.length; i++) {
            for (int t = 0; t < numberOfTimestamps; t++) {
                for (int j = 0; j < 3; j++) {
                    this.neighbourvelocity[i][j][t] = calcNeighbourVelocityFromTriangleVelocityOuterProjection(t, i, j);
                }
            }
        }
    }

    /**
     * Maximum velocity between neumannNeighbours [triangleIndex][Neighbourindex
     * 0/1/2]
     *
     * @return
     */
    public float[][] getMaxNeighbourVelocity() {
        return maxNeighbourVelocity;
    }

    /**
     * Indices of nodes of triangle [triangleindex][0,1,2]
     *
     * @return
     */
    public int[][] getTriangleNodes() {
        return triangleNodes;
    }

    /**
     * Vertices' coordinates
     *
     * @return [vertex][x,y,z]
     */
    public double[][] getVerticesPosition() {
        return vertices;
    }

    public Polygon calcConvexHullUTM() {
        HashSet<Coordinate> set = new HashSet<>();
        for (int i = 0; i < neumannNeighbours.length; i++) {
            if (neumannNeighbours[i][0] < 0) {
                int vertIndex = triangleNodes[i][0];
                set.add(new Coordinate(vertices[vertIndex][0], vertices[vertIndex][1]));
            }
        }
        GeometryFactory gf = new GeometryFactory();
        MultiPoint mp = gf.createMultiPoint(set.toArray(new Coordinate[set.size()]));
        return (Polygon) mp.convexHull();
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
        if (time > nextRecalculation) {
            nextRecalculation = time + recalculationIntervallMS;
        }
        this.actualTime = time;
        this.timeIndex = times.getTimeIndexDouble(time);
        this.timeFrac = timeIndex % 1.;
        this.timeinvFrac = 1. - timeFrac;
        this.timeIndexInt = (int) this.timeIndex;
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
        nextRecalculation = 0L;
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

    /**
     * @deprecated use applyStreetInlets(Network network,
     * ArrayList<HE_InletReference> inletRefs) instead
     * @param nw
     * @param pipeANDTriangleIDs
     */
    public void applyStreetInlets(Network nw, Collection<Pair<String, Integer>> pipeANDTriangleIDs) {
//        inlets = new ConcurrentHashMap<>(pipeANDTriangleIDs.size());//new Inlet[triangleNodes.length];
        inletArray = new Inlet[getMaxTriangleID() + 1];
        ArrayList<Inlet> inletList = new ArrayList<>(pipeANDTriangleIDs.size());

        for (Pair<String, Integer> pipeANDTriangleID : pipeANDTriangleIDs) {
            String pipename = pipeANDTriangleID.first;
            int triangleID = pipeANDTriangleID.second;

            //1. check if pipe with this name exists
            Capacity cap = null;
            try {
                cap = nw.getCapacityByName(pipename);

            } catch (NullPointerException nullPointerException) {
            }
            if (cap == null) {
                System.err.println("Could not find Pipe with name '" + pipename + "' to apply a treetinlet next to it.");
                continue;
            }
            //Transform Pipe to surface coordinates
            double[] tpos = triangleMids[triangleID];

            double distancealongPipe = 0;
            if (cap instanceof Pipe) {
                try {
                    Pipe pipe = (Pipe) cap;
                    Coordinate start = geotools.toUTM(pipe.getStartConnection().getPosition());
                    Coordinate end = geotools.toUTM(pipe.getEndConnection().getPosition());
                    distancealongPipe = GeometryTools.distancePointAlongLine(start.x, start.y, end.x, end.y, tpos[0], tpos[1]);
                } catch (TransformException ex) {
                    Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
                }
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
                Coordinate longlat = geotools.toGlobal(new Coordinate(tpos[0], tpos[1]), true);
                Inlet inlet = new Inlet(new Position3D(longlat.x, longlat.y, tpos[0], tpos[1], tpos[3]), (Pipe) cap, distancealongPipe);
//                tri.inlet = inlet;
                inletList.add(inlet);
//                inlets.put(triangleID, inlet);
                inletArray[triangleID] = inlet;
//                inlets[triangleID] = inlet;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        nw.setStreetInlets(inletList);
//        this.inlets = inlets;
    }

    public void applyStreetInlets(Network network, ArrayList<HE_InletReference> inletRefs) throws TransformException {
//        inlets = new ConcurrentHashMap<>(inletRefs.size());//new Inlet[triangleNodes.length];

        ArrayList<Inlet> inletList = new ArrayList<>(inletRefs.size());
        manholes = new Manhole[triangleNodes.length];
        inletArray = new Inlet[manholes.length];
        for (HE_InletReference inletRef : inletRefs) {
            String capacityName = inletRef.capacityName;
            int triangleID = inletRef.triangleID;

            //1. check if pipe with this name exists
            Capacity cap = null;
            cap = capacityNames.get(capacityName);

            if (cap == null) {
                System.err.println("Could not find Pipe with name '" + capacityName + "' to apply a treetinlet next to it.");
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
        manholes = new Manhole[triangleNodes.length];
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
        stat.sum_travelLength += p.getTravelledPathLength() - p.posToSurface;

        stat.minTravelTime = Math.min(stat.minTravelTime, traveltime);
        stat.maxTravelTime = Math.max(stat.maxTravelTime, traveltime);

        stat.minTravelLength = Math.min(stat.minTravelLength, p.getTravelledPathLength() - p.posToSurface);
        stat.maxTravelLength = Math.max(stat.maxTravelLength, p.getTravelledPathLength() - p.posToSurface);

        if (sourcesForSpilloutParticles.containsKey(p.injectionSurrounding)) {
            Integer counter = sourcesForSpilloutParticles.get(p.injectionSurrounding);
            int ineu = counter + 1;
            sourcesForSpilloutParticles.put(p.injectionSurrounding, ineu);
        } else {
            sourcesForSpilloutParticles.put(p.injectionSurrounding, 1);
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

    /**
     * Finds smallest triangle area of all triangles.
     *
     * @return smallest triangle area [m²]
     */
    public float calcSmallestTriangleArea() {
        double minA = Double.POSITIVE_INFINITY;

        for (int i = 0; i < triangleNodes.length; i++) {
            double a = 0.5f
                    * (vertices[triangleNodes[i][0]][0] * (vertices[triangleNodes[i][1]][1] - vertices[triangleNodes[i][2]][1])
                    + vertices[triangleNodes[i][1]][0] * (vertices[triangleNodes[i][2]][1] - vertices[triangleNodes[i][0]][1])
                    + vertices[triangleNodes[i][2]][0] * (vertices[triangleNodes[i][0]][1] - vertices[triangleNodes[i][1]][1]));
            if (a < 0.1) {
                System.out.println("Triangle " + i + " is only " + a + " m² small.");
            }
            minA = Math.min(Math.abs(a), minA);
        }
        return (float) minA;
    }

    /**
     * Runs over all triangles and finds the one with maximum area.
     *
     * @return area of max traingle [m²]
     */
    public float calcLargestTriangleArea() {
        float maxA = Float.NEGATIVE_INFINITY;

        for (int[] triangleNode : triangleNodes) {
            float a = (float) (0.5f * (vertices[triangleNode[0]][0] * (vertices[triangleNode[1]][1] - vertices[triangleNode[2]][1]) + vertices[triangleNode[1]][0] * (vertices[triangleNode[2]][1] - vertices[triangleNode[0]][1]) + vertices[triangleNode[2]][0] * (vertices[triangleNode[0]][1] - vertices[triangleNode[1]][1])));
            maxA = Math.max(Math.abs(a), maxA);
        }
        return maxA;
    }

    /**
     * Runs over all triangles and sums up all areas.
     *
     * @return total area of this surface [m²]
     */
    public double calcTotalTriangleArea() {
        double surf = 0;

        for (int[] triangleNode : triangleNodes) {
            double a = 0.5f * (vertices[triangleNode[0]][0] * (vertices[triangleNode[1]][1] - vertices[triangleNode[2]][1]) + vertices[triangleNode[1]][0] * (vertices[triangleNode[2]][1] - vertices[triangleNode[0]][1]) + vertices[triangleNode[2]][0] * (vertices[triangleNode[0]][1] - vertices[triangleNode[1]][1]));
            surf += Math.abs(a);
        }
        return surf;
    }

    /**
     * Runs over all triangles and calculates a mean value for the area of ONe
     * triangle in m²
     *
     * @return mean triangle area [m²]
     */
    public double calcMeanTriangleArea() {
        double sumA = 0;

        for (int[] triangleNode : triangleNodes) {
            double a = 0.5 * (vertices[triangleNode[0]][0] * (vertices[triangleNode[1]][1] - vertices[triangleNode[2]][1]) + vertices[triangleNode[1]][0] * (vertices[triangleNode[2]][1] - vertices[triangleNode[0]][1]) + vertices[triangleNode[2]][0] * (vertices[triangleNode[0]][1] - vertices[triangleNode[1]][1]));
            sumA += a;
        }
        return sumA / (double) triangleNodes.length;
    }

    /**
     * If this surface is only a submesh of the original surface, this map
     * contains the new positions of triangle ids.
     *
     * @return HashMap<Original Triangle ID, index now used>
     */
    public HashMap<Integer, Integer> getMapIndizes() {
        return mapIndizes;
    }

    /**
     * velocities on triangle [triangle][timeindex][direction(0:x,1:y)]
     *
     * @return
     */
    public float[][][] getTriangleVelocity() {
        return triangleVelocity;
    }

    /**
     * Set velocities on triangle [triangle][timeindex][direction(0:x,1:y)]
     *
     * @param triangleVelocity
     */
    public void setTriangleVelocity(float[][][] triangleVelocity) {
        this.triangleVelocity = triangleVelocity;
        if (triangleVelocity[0] != null) {
            int timesNew = triangleVelocity[0].length;
            if (numberOfTimestamps > 0 && timesNew != numberOfTimestamps) {
                System.err.println(getClass() + " number of timestamps from triangleVelocity (" + timesNew + ") is not the same as existing number of timestamps (" + numberOfTimestamps + ")");
            }
            numberOfTimestamps = timesNew;
        }
    }

    /**
     * Sets the undirected maximum velocity [m/s]
     *
     * @param vmax
     */
    public void setTriangleMaxVelocity(float[] vmax) {
        this.maxTriangleVelocity = vmax;
    }

    /**
     * The maximum velocity on this triangle. Direction is not given.
     *
     * @param triangle index
     * @return velocity [m/s]
     */
    public float getTriangleMaxVelocity(int triangle) {
        return maxTriangleVelocity[triangle];
    }

    /**
     *
     * @return
     */
    @Override
    public Surface clone() {

        Surface surf = new Surface(vertices.clone(), triangleNodes.clone(), neumannNeighbours.clone(), mapIndizes != null ? ((HashMap<Integer, Integer>) mapIndizes.clone()) : null, this.spatialReferenceCode);
        if (this.waterlevels != null && this.waterlevels.length > 0) {
            surf.waterlevels = this.waterlevels.clone();
            if (this.maxWaterLevels != null) {
                surf.maxWaterLevels = this.maxWaterLevels.clone();
            }
        }
        surf.geotools = this.geotools;
        surf.kst = this.kst;
        surf.numberOfTimestamps = this.numberOfTimestamps;
        surf.recalculationIntervallMS = this.recalculationIntervallMS;
        surf.times = this.times;
        if (this.triangleMids != null) {
            surf.triangleMids = this.triangleMids.clone();
        }

        return surf;
    }

    /**
     * If Midpoints are not set from reading a resultFile one can Calculate them
     * from Vertices' coordinates. Attention: This differs from results' read in
     * values! I do not know why.
     *
     * @return [triangleID][0:x,1:y,2:z] Coordinates
     */
    public float[][] calcMidPointsFromVertexPoints() {
        float[][] mids = new float[triangleNodes.length][3];
        double third = 1. / 3.;
        for (int i = 0; i < mids.length; i++) {
            for (int j = 0; j < 3; j++) {
                double[] vertex = vertices[triangleNodes[i][j]];
                for (int k = 0; k < 3; k++) {
                    mids[i][k] += vertex[k] * third;
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

    public void setNeumannNeighbours(int[][] neumannNeighbours) {
        this.mooreNeighbours = neumannNeighbours;
    }

    /**
     * Lists Neumann neighbour triangle Id for each triangle (every row varies
     * in length)
     *
     * @return [number of triangles][variable count of triangles]
     */
    public int[][] getNeumannNeighbours() {
        return mooreNeighbours;
    }

    public int calcContainingTriangle(double x, double y) {
        return crawlNearestTriangle(x, y, 0);
    }

    public int calcContainingTriangleLongLat(double longitude, double latitude) {
        try {
            Coordinate utm = geotools.toUTM(longitude, latitude);
            return calcContainingTriangle(utm.x, utm.y);
        } catch (TransformException ex) {
            Logger.getLogger(Surface.class.getName()).log(Level.SEVERE, null, ex);
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
        for (int i = 0; i < triangleMids.length; i++) {
            double[] mid = triangleMids[i];
            if (Math.abs(mid[0] - x) > minDistance) {
                continue;
            }
            if (Math.abs(mid[1] - y) > minDistance) {
                continue;
            }
            //is coordinate in triangle?
            int[] nodeIDs = triangleNodes[i];
            double[] p0 = vertices[nodeIDs[0]];
            double[] p1 = vertices[nodeIDs[1]];
            double[] p2 = vertices[nodeIDs[2]];
            if (GeometryTools.triangleContainsPoint(p0[0], p1[0], p2[0], p0[1], p1[1], p2[1], x, y)) {
                return i;
            }
        }
        return -1;
    }

    public int crawlNearestTriangle(double x, double y, int startTriangleID) {
        int maxGrowingCounter = 300;

        int growCounter = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        //start with first triangle
        int istID = startTriangleID;
        double[] istPos = triangleMids[istID];
        double dist = Math.abs(istPos[0] - x) * Math.abs(istPos[1] - y);
        while (true) {

            if (dist < bestDistance) {
                bestDistance = dist;
                growCounter = 0;
                //Find nearest neighbour
                int[] neighbours;
                if (mooreNeighbours != null) {
                    neighbours = mooreNeighbours[istID];
                } else {
                    neighbours = this.neumannNeighbours[istID];
                }
                double bestDist = Double.POSITIVE_INFINITY;
                int bestID = -1;
                for (int nbID : neighbours) {
                    if (nbID < 0) {
                        continue;
                    }
                    double[] nbPos = triangleMids[nbID];
                    double nbDist = Math.abs(nbPos[0] - x) * Math.abs(nbPos[1] - y);
                    if (nbDist < bestDist) {
                        bestDist = nbDist;
                        bestID = nbID;
                    }
                }
                if (bestID == -1) {
                    System.err.println("Something wrong with crawling near triangle ");
                    break;
                }
                if (bestID == istID) {
                    //Found this triangle to be the best
                    break;
                }
                istID = bestID;
                dist = bestDist;

            } else {
                growCounter++;
                break;
            }
        }
        //Found nearest triangle on the direct way. If there is a hole in the grid, this might block the path
        //Test, if point is in triangle

        //If not, do some grow search
        return istID;
    }

    public TimeIndexContainer getTimes() {
        return times;
    }

    public int getMaxTriangleID() {
        return triangleNodes.length + 1;
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

        double[] velocityParticle = tofillVelocity;
        if (tofillVelocity == null) {
//            System.out.println("create new double[2] for particle velocity on surface");
            velocityParticle = new double[2];
        }
        //Get node IDs for the triangle
        int t0 = triangleNodes[triangleID][0];
        int t1 = triangleNodes[triangleID][1];
        int t2 = triangleNodes[triangleID][2];

        // get utm coordinates of the actual triangle
//        float x1 = (float) vertices[t0][0];
//        float x2 = (float) vertices[t1][0];
//        float x3 = (float) vertices[t2][0];
//        float y1 = (float) vertices[t0][1];
//        float y2 = (float) vertices[t1][1];
//        float y3 = (float) vertices[t2][1];
        // barycentric koordinate weighing for velocity calculation
        if (tofillBarycentric == null) {
//            System.out.println("create new double[3] for barycentric coordinates");
            tofillBarycentric = new double[3];
        }
        getBarycentricWeighing_FillArray(vertices[t0][0], vertices[t1][0], vertices[t2][0], vertices[t0][1], vertices[t1][1], vertices[t2][1], p.getPosition3d().x, p.getPosition3d().y, tofillBarycentric);
        double[] w = tofillBarycentric;
        if (calculateWeighted && velocityNodes != null) {
            if (velocityNodes[t0] == null) {
                loadSparseNodeVelocity2D(t0);
            }
            if (velocityNodes[t1] == null) {
                loadSparseNodeVelocity2D(t1);
            }
            if (velocityNodes[t2] == null) {
                loadSparseNodeVelocity2D(t2);
            }

            // particle velocity in x and y direction at given timeindex
            try {
                velocityParticle[0] = w[0] * velocityNodes[t0][timeIndexInt][0] + w[1] * velocityNodes[t1][timeIndexInt][0] + w[2] * velocityNodes[t2][timeIndexInt][0];
                velocityParticle[1] = w[0] * velocityNodes[t0][timeIndexInt][1] + w[1] * velocityNodes[t1][timeIndexInt][1] + w[2] * velocityNodes[t2][timeIndexInt][1];
            } catch (Exception e) {
                System.err.println("velocity nodes.length=" + velocityNodes.length + "  triangleNodes.length=" + triangleNodes.length + "\t tN0:" + triangleNodes[triangleID][0] + "\t1:" + triangleNodes[triangleID][1] + "\t2:" + triangleNodes[triangleID][2]);
                e.printStackTrace();
            }
        } else {
            //If no weights are calculated, just use mean velocity from neighbouring triangle nodes.
            if (timeInterpolatedValues) {
//                if (toFillSurfaceVelocity == null) {
//                    toFillSurfaceVelocity = new float[4][2][2]; //4times 2 directions
//                }
                float[] vt_t = getTriangleVelocity(triangleID, timeIndexInt);//triangleID, timeIndexInt, (float) timeFrac, toFillSurfaceVelocity[0][0]);
                float[] vt_tp = getTriangleVelocity(triangleID, timeIndexInt + 1);//triangleID, timeIndexInt, (float) timeFrac, toFillSurfaceVelocity[0][0]);

                //neighbours:
                int nb0 = neumannNeighbours[triangleID][0];
                int nb1 = neumannNeighbours[triangleID][1];
                int nb2 = neumannNeighbours[triangleID][2];

                float[] v0_t;
                float[] v0_tp;
                if (nb0 < 0) {
                    v0_t = zeroVelocity;
                    v0_tp = zeroVelocity;
                } else {
                    v0_t = getTriangleVelocity(nb0, timeIndexInt);
                    v0_tp = getTriangleVelocity(nb0, timeIndexInt + 1);
                }
                float[] v1_t, v1_tp;
                if (nb1 < 0) {
                    v1_t = zeroVelocity;
                    v1_tp = zeroVelocity;
                } else {
                    v1_t = getTriangleVelocity(nb1, timeIndexInt);
                    v1_tp = getTriangleVelocity(nb1, timeIndexInt + 1);
                }
                float[] v2_t, v2_tp;
                if (nb2 < 0) {
                    v2_t = zeroVelocity;
                    v2_tp = zeroVelocity;
                } else {
                    v2_t = getTriangleVelocity(nb2, timeIndexInt);
                    v2_tp = getTriangleVelocity(nb2, timeIndexInt + 1);
                }

                velocityParticle[0] = ((1 - w[0]) * (v0_t[0] * timeinvFrac + v0_tp[0] * timeFrac) + (1 - w[1]) * (v1_t[0] * timeinvFrac + v1_tp[0] * timeFrac) + (1 - w[2]) * (v2_t[0] * timeinvFrac + v2_tp[0] * timeFrac) + (vt_t[0] * timeinvFrac + vt_tp[0] * timeFrac)) * 0.333f;
                velocityParticle[1] = ((1 - w[0]) * (v0_t[1] * timeinvFrac + v0_tp[1] * timeFrac) + (1 - w[1]) * (v1_t[1] * timeinvFrac + v1_tp[1] * timeFrac) + (1 - w[2]) * (v2_t[1] * timeinvFrac + v2_tp[1] * timeFrac) + (vt_t[1] * timeinvFrac + vt_tp[1] * timeFrac)) * 0.333f;

            } else {
                float[] vt = getTriangleVelocity(triangleID)[timeIndexInt];
//                float[] v0 = getTriangleVelocity(t0)[timeIndexInt];
//                float[] v1 = getTriangleVelocity(t1)[timeIndexInt];
//                float[] v2 = getTriangleVelocity(t2)[timeIndexInt];
                //neighbours:
                int nb0 = neumannNeighbours[triangleID][0];
                int nb1 = neumannNeighbours[triangleID][1];
                int nb2 = neumannNeighbours[triangleID][2];

                float[] v0;
                if (nb0 < 0) {
                    v0 = zeroVelocity;
                } else {
                    v0 = getTriangleVelocity(nb0, timeIndexInt);
                }
                float[] v1;
                if (nb1 < 0) {
                    v1 = zeroVelocity;
                } else {
                    v1 = getTriangleVelocity(nb1, timeIndexInt);
                }
                float[] v2;
                if (nb2 < 0) {
                    v2 = zeroVelocity;
                } else {
                    v2 = getTriangleVelocity(nb2, timeIndexInt);
                }
//                velocityParticle[0] = ((1 - w[0]) * (v0[0] + vt[0]) + (1 - w[1]) * (v1[0] + vt[0]) + (1 - w[2]) * (v2[0] + vt[0])) * 0.25;
//                velocityParticle[1] = ((1 - w[0]) * (v0[1] + vt[1]) + (1 - w[1]) * (v1[1] + vt[1]) + (1 - w[2]) * (v2[1] + vt[1])) * 0.25;

                velocityParticle[0] = ((1 - w[0]) * (v0[0] + vt[0]) + (1 - w[1]) * (v1[0] + vt[0]) + (1 - w[2]) * (v2[0] + vt[0])) * 0.25;
                velocityParticle[1] = ((1 - w[0]) * (v0[1] + vt[1]) + (1 - w[1]) * (v1[1] + vt[1]) + (1 - w[2]) * (v2[1] + vt[1])) * 0.25;

//                //Use only the samllest weight to interpolate velocity
//                float[] v1t;
//                double w1t = 0;
//                if (w[0] < w[1]) {
//                    if (w[0] < w[2]) {
//                        w1t = w[0];
//                        v1t = v0;
//                    } else {
//                        w1t = w[2];
//                        v1t = v2;
//                    }
//                } else {
//                    if (w[1] < w[2]) {
//                        w1t = w[1];
//                        v1t = v1;
//                    } else {
//                        w1t = w[2];
//                        v1t = v2;
//                    }
//                }
//                velocityParticle[0]=vt[0]-(1-w1t)*(v1t[0]-vt[0])*0.5;
//                velocityParticle[1]=vt[1]-(1-w1t)*(v1t[1]-vt[1])*0.5;
//                System.out.println("benutze kleinste gewichtete richtung");
            }
        }
        return velocityParticle;
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
     * @param barycentric can be given to prevent new allocation
     * @param temp_vertexcoordsarray can be given to prevent new allocation
     * @return id of the new triangle
     */
    public int getTargetTriangleID(Particle p, int id, double xold, double yold, double x, double y, int leftIterations, double[] bw, double[][] t) /*throws BoundHitException*/ {

        // is particle still in start triangle? use barycentric weighing to check.
        int node0 = triangleNodes[id][0];
        int node1 = triangleNodes[id][1];
        int node2 = triangleNodes[id][2];

        if (t == null) {
            System.out.println("new double[][] for triangle vertex coords in Surface");
            t = new double[3][];
        }
        t[0] = vertices[node0];
        t[1] = vertices[node1];
        t[2] = vertices[node2];

        getBarycentricWeighing_FillArray(t[0][0], t[1][0], t[2][0], t[0][1], t[1][1], t[2][1], x, y, bw);

//        int outs = 0;
        double smallest = 0;
        int bestEdge = -1;
        int secondBest = -1;
        for (int i = 0; i < 3; i++) {
            if (bw[i] < 0) {
                if (bw[i] < smallest) {
                    if (bestEdge >= 0) {
                        secondBest = bestEdge;
                        bestEdge = i;
                    } else {
                        bestEdge = i;
                    }
                } else {
                    secondBest = i;
                }
            }
        }
        if (bestEdge < 0) {
            //Particle stays in this triangle;
            p.setPosition3D(x, y);
            return id;
        }

        int nextID = -1;
        //find triangle with same edge 
        boolean foundFirstEdgeNode;
        int testNode0, testNode1;
        boolean found = false;
        if (bestEdge == 0) {
            testNode0 = node0;
            testNode1 = node1;
        } else if (bestEdge == 1) {
            testNode0 = node1;
            testNode1 = node2;
        } else {
            testNode0 = node2;
            testNode1 = node0;
        }

        for (int i = 0; i < 3; i++) {
            int neighbourID = neumannNeighbours[id][i];
            if (neighbourID < 0) {
                //boundary found, but we are not yet sure if this is the target triangle. Do NOT set the found-boundary flag.
                continue;
            }
            //Test nodes of the surrounding triangles to find a matching edge.
            foundFirstEdgeNode = false;
            for (int j = 0; j < 3; j++) {
                int testEdgeNode = triangleNodes[neighbourID][j];
                if (testEdgeNode == testNode0 || testEdgeNode == testNode1) {
                    if (foundFirstEdgeNode) {
                        found = true;
                        nextID = neighbourID;
                        break;
                    }
                    foundFirstEdgeNode = true;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found && secondBest >= 0) {
            //try it with the other possible neighbour
            if (secondBest == 0) {
                testNode0 = node0;
                testNode1 = node1;
            } else if (secondBest == 1) {
                testNode0 = node1;
                testNode1 = node2;
            } else {
                testNode0 = node2;
                testNode1 = node0;
            }

            for (int i = 0; i < 2; i++) {
                int neighbourID = neumannNeighbours[id][i];
                if (neighbourID < 0) {
                    //boundary found, but we are not yet sure if this is the target triangle. Do NOT set the found-boundary flag.
                    continue;
                }
                foundFirstEdgeNode = false;
                for (int j = 0; j < 2; j++) {
                    int testEdgeNode = triangleNodes[neighbourID][j];
                    if (testEdgeNode == testNode0 || testEdgeNode == testNode1) {
                        if (foundFirstEdgeNode) {
                            found = true;
                            nextID = neighbourID;
                            break;
                        }
                        foundFirstEdgeNode = true;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        if (nextID >= 0 && leftIterations > 0) {
            leftIterations--;
            return getTargetTriangleID(p, nextID, xold, yold, x, y, leftIterations, bw, t);
        }

        //no outgoing triangle
        //Projection of triangle back to triangle boundary
        int startindex = bestEdge;
        double ax = t[startindex][0];
        double ay = t[startindex][1];
        double bx = t[(startindex + 1) % 3][0];
        double by = t[(startindex + 1) % 3][1];

        double s = GeometryTools.lineIntersectionS(xold, yold, x, y, ax, ay, bx, by);

        if (s < 0 || s > 1) {

//            System.err.println("Bad backprojection on intersection edge");
            //Something Bad happened, particle seems not to go via this edge. Check the other node, if a second edge was found
            double paralleldiff = ((y - yold) / (x - xold)) - ((by - ay) / (bx - ax));
            //Check for parallel 
            if (Math.abs(paralleldiff) < 0.0001) {
                //Parallel movement& boundary -> Set to this tringle's midpoint
//                System.out.println(" parallel movement p=" + paralleldiff);
                p.surfaceCellID = id;
                p.setPosition3D(triangleMids[id][0], triangleMids[id][1]);
//                throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
                return id;
            } else {
                if (secondBest >= 0) {
                    //Try to find a better calculation for the other possible neighbour
                    startindex = secondBest;// vertexIndex[1];
                    ax = t[startindex][0];
                    ay = t[startindex][1];
                    bx = t[(startindex + 1) % 3][0];
                    by = t[(startindex + 1) % 3][1];
                    double[] st1 = GeometryTools.lineIntersectionST(xold, yold, x, y, ax, ay, bx, by, bw);

                    s = st1[0];
                    if (s < 0 || s > 1) {
                        //throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
                        p.surfaceCellID = id;
                        p.setPosition3D(triangleMids[id][0], triangleMids[id][1]);
                        return id;
                        //
                    }
                } else {
                    p.surfaceCellID = id;
                    p.setPosition3D(triangleMids[id][0], triangleMids[id][1]);
                    return id;
                    // throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
                }
            }
        }
        //Scale s so that the particle does not get nearer than 1cm to the boundary
//        double distance = Math.sqrt((x - xold) * (x - xold) + (y - yold) * (y - yold));
//        s -= 0.01 / distance;
        //Calculate new position
        double xneu = xold + s * (x - xold);
        double yneu = yold + s * (y - yold);
        //Fire Message with the new position.
        p.surfaceCellID = id;
        p.setPosition3D(xneu, yneu);
        return id;
        //throw new BoundHitException(id, xneu, yneu);
    }

//    /**
//     * Returns the target triangle id or a BoundHitException with corrected
//     * coordinates if the particle could not move out of the triangle
//     *
//     * @param p Particle
//     * @param id startTriangleID
//     * @param xold
//     * @param yold
//     * @param x calculated new Position with unknown particle id
//     * @param y
//     * @param leftIterations max number of iterations that shall be computet
//     * @return id of the new triangle
//     * @throws model.surface.Surface.BoundHitException
//     */
//    public int getTargetTriangleIDorg(Particle p, int id, double xold, double yold, double x, double y, int leftIterations) throws BoundHitException {
//        //if particle didn't move return old id
////        if (xold == x && yold == y) {
////
////            return id;
////        }
//
////        searchcounter++;
////        if (searchcounter % 100 == 0) {
////            System.out.println(" gettargettriangle calls " + leftIterations);
////            System.out.println("need to search for Triangle " + (searchcounter));
////        }
//        // is particle still in start triangle? use barycentric weighing to check.
//        double[][] t = new double[3][];
//        t[0] = vertices[triangleNodes[id][0]];
//        t[1] = vertices[triangleNodes[id][1]];
//        t[2] = vertices[triangleNodes[id][2]];
////        float[] pos = new float[]{(float) x, (float) y};
//
//        double[] bw = getBarycentricWeighing(t[0][0], t[1][0], t[2][0], t[0][1], t[1][1], t[2][1], x, y);
//
//        int outs = 0;
//        for (double w : bw) {
//            if (w < 0) {
//                outs++;
//            }
//        }
//        if (outs == 0) {
//            //Particle stays in this triangle;
//            return id;
//        } else {
//
//            //Search for direction of outgoing
//            int[][] edgeIndices = new int[outs][];
////            int[] edgeIndices2=null;
//
//            int[] vertexIndex = new int[outs];
////            int vertexIndex2=-1;
//            boolean firstFound = false;
//            for (int i = 0; i < bw.length; i++) {
//                if (bw[i] < 0) {
//                    if (firstFound) {
//                        edgeIndices[1] = new int[]{triangleNodes[id][i], triangleNodes[id][(i + 1) % 3]};
//                        vertexIndex[1] = i;
//                        break;
//                    } else {
//                        edgeIndices[0] = new int[]{triangleNodes[id][i], triangleNodes[id][(i + 1) % 3]};
//                        vertexIndex[0] = i;
//                        firstFound = true;
//                    }
//                }
//            }
//            if (outs > 1) {
//                //Prefere the one with larger area (higher negative value)
//                if (bw[vertexIndex[1]] < bw[vertexIndex[0]]) {
//                    //Switch indizes
//                    int[] edgeTemp = edgeIndices[0];
//                    edgeIndices[0] = edgeIndices[1];
//                    edgeIndices[1] = edgeTemp;
//                    int vertexTemp = vertexIndex[0];
//                    vertexIndex[0] = vertexIndex[1];
//                    vertexIndex[1] = vertexTemp;
//                }
//            }
//
//            //Search for neighbour, that contains these points
//            /*Id of Neighbourtriangle that shall be investigated next.*/
//            int nextID = -1;
//            //First try to find triangle that holds first edge
//            boolean boundaryFound = false;
//            for (int n = 0; n < 3; n++) {
//                int neighbourID = neumannNeighbours[id][n];
//                if (neighbourID < 0) {
//                    boundaryFound = true;
//                    continue;
//                }
//                //Check triangle nodes
//                boolean foundOne = false;
////                 System.out.println("in Triangle "+n+" : "+triangleNodes[neighbourID][0]+" - "+triangleNodes[neighbourID][1]+" - "+triangleNodes[neighbourID][2]);
//                for (int i = 0; i < edgeIndices[0].length; i++) {
//                    for (int nbn = 0; nbn < 3; nbn++) {
//                        if (edgeIndices[0][i] == triangleNodes[neighbourID][nbn]) {
//                            if (foundOne) {
//                                //Found second matching edge node.
//                                nextID = neighbourID;
//                                break;
//                            }
//                            //Found first matching edgenode
//                            foundOne = true;
//                        }
//                    }
//                    if (nextID >= 0) {
//                        break;
//                    }
//                }
//                if (nextID >= 0) {
//                    break;
//                }
//            }
//            //If nothing was found, try to find not so good edge
//            if (nextID < 0 && outs > 1) {
//                for (int n = 0; n < 3; n++) {
//                    int neighbourID = neumannNeighbours[id][n];
//                    if (neighbourID < 0) {
//                        boundaryFound = true;
//                        continue;
//                    }
//
//                    boolean foundOne = false;
//
//                    for (int i = 0; i < edgeIndices[1].length; i++) {
//                        for (int nbn = 0; nbn < 3; nbn++) {
//                            if (edgeIndices[1][i] == triangleNodes[neighbourID][nbn]) {
//                                if (foundOne) {
//                                    //Found second matching edge node.
//                                    nextID = neighbourID;
//                                    break;
//                                }
//                                //Found first matching edgenode
//                                foundOne = true;
//                            }
//                        }
//                        if (nextID >= 0) {
//                            break;
//                        }
//                    }
//                    if (nextID >= 0) {
//                        break;
//                    }
//                }
//            }
//
//            if (nextID >= 0 && leftIterations > 0) {
//                leftIterations--;
//                return getTargetTriangleID(p, nextID, xold, yold, x, y, leftIterations, null, null);
//            }
//
//            {
//                //no outgoing triangle
//                //Projection of triangle back to triangle boundary
//                int startindex = vertexIndex[0];
//                double ax = t[startindex][0];
//                double ay = t[startindex][1];
//                double bx = t[(startindex + 1) % 3][0];
//                double by = t[(startindex + 1) % 3][1];
//
////                double distanceT = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
////                double s = GeometryTools.lineIntersectionS(xold, yold, x, y, ax, ay, bx, by);
//                double s = GeometryTools.lineIntersectionS(xold, yold, x, y, ax, ay, bx, by);
////                double s = st0[0];
//                if (s < 0 || s > 1) {
//                    //Something Bad happened, particle seems not to go via this edge. Check the other node, if a second edge was found
//                    double paralleldiff = ((y - yold) / (x - xold)) - ((by - ay) / (bx - ax));
////                    System.out.println("WRONG intersection : " + st + "\tdistance:" + distance + " / " + distanceT + "\tslopediff:" + paralleldiff + "  outs:" + outs);
//                    //Check for parallel 
//                    if (Math.abs(paralleldiff) < 0.0001) {
////                        System.out.println("PARALLEL");
//                        //Parallel movement& boundary -> Set to this tringle's midpoint
//                        //TODO 
//                        throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
//                    } else {
//                        if (outs > 1) {
//                            //Try to find a better calculation for the other possible neighbour
//                            startindex = vertexIndex[1];
//                            ax = t[startindex][0];
//                            ay = t[startindex][1];
//                            bx = t[(startindex + 1) % 3][0];
//                            by = t[(startindex + 1) % 3][1];
//                            double[] st1 = GeometryTools.lineIntersectionST(xold, yold, x, y, ax, ay, bx, by);
//                            s = st1[0];
//                            if (s < 0 || s > 1) {
//                                System.out.println("line1: " + xold + " | " + yold + "  to " + x + " | " + y + "  \t crosses " + ax + " | " + ay + " to " + bx + " | " + by);
//                                System.out.println("BAD: s=" + st1[0] + "|" + st1[1] + "\t edges: " + outs + "   left:" + leftIterations + "\tweights: " + bw[0] + ", " + bw[1] + ", " + bw[2] + "  " + (boundaryFound ? "Boundary hit!" : ""));
//                                throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
//                            }
//                        } else {
//                            throw new BoundHitException(id, triangleMids[id][0], triangleMids[id][1]);
//                        }
//                    }
//                }
//                //Scale s so that the particle does not get nearer than 1cm to the boundary
//                double distance = Math.sqrt((x - xold) * (x - xold) + (y - yold) * (y - yold));
//                s -= 0.01 / distance;
//                //Calculate new position
//                double xneu = xold + s * (x - xold);
//                double yneu = yold + s * (y - yold);
//                //Fire Message with the new position.
//                throw new BoundHitException(id, xneu, yneu);
//            }
//        }
////        throw new BoundHitException(id, new double[]{triangleMids[id][0], triangleMids[id][1]});
//    }
    /**
     * @deprecated @param p
     * @param id
     * @param xold
     * @param yold
     * @param x
     * @param y
     * @return
     * @throws model.surface.Surface.BoundHitException
     */
    public int getTargetTriangleID_RIss(Particle p, int id, double xold, double yold, double x, double y) throws BoundHitException {

        //if particle didn't move return old id
        if (xold == x && yold == y) {
            return id;
        }
        // is particle still in start triangle? use barycentric weighing to check.
        //w ;= new double[3];
        double[] xTri = new double[3];
        double[] yTri = new double[3];

        for (int i = 0; i < 3; i++) {
            xTri[i] = vertices[triangleNodes[id][i]][0];
            yTri[i] = vertices[triangleNodes[id][i]][1];
        }
        double[] w = getBarycentricWeighing(xTri[0], xTri[1], xTri[2], yTri[0], yTri[1], yTri[2], x, y);
        int wnegative = 0;
        for (int i = 0; i < 3; i++) {
            if (w[i] < 0) {
                wnegative = 1;
                break;
            }
        }
        if (wnegative == 0) {
            //System.out.println("particle stayed in start triangle!");
            return id;
        }

        //check if start triangle has less than 3 neumannNeighbours and if so, if particle left model area
        getBoundaryIntersection(id, xold, yold, x, y);

        //is particle in neighbouring triangles of closest node to new particle position?
        double[][] dist = new double[3][3]; // nur für Startdreieck: [Ecke][x,y,resultierende distanz]

        int minDistIndex = 0; // Index which triangle node has smallest distance to particle
        double minDist = Float.POSITIVE_INFINITY;
        int[][] triangleHistory = new int[2][2]; // take triangle ids that have been tested, to skip them in further search [triangleID][Position in TrianglesAtNode]
        triangleHistory[0][0] = id;
        triangleHistory[1][0] = id;
        triangleHistory[0][1] = 0;
        triangleHistory[1][1] = 0;

        //find start triangle node with minimal distance to particle position
        for (int i = 0; i < 3; i++) {
            dist[i][0] = Math.abs(xTri[i] - x);
            dist[i][1] = Math.abs(yTri[i] - y);
            dist[i][2] = Math.sqrt((dist[i][0] * dist[i][0]) + (dist[i][1] * dist[i][1]));
            if (dist[i][2] < minDist) {
                minDist = dist[i][2];
                minDistIndex = i;
            }
        }
        int closeNode = triangleNodes[id][minDistIndex]; // closest triangle node to new particle position
        int nbOfTrianglesAtNode = NodeNeighbours[closeNode].length;
        int[] TrianglesAtNode = new int[nbOfTrianglesAtNode]; // triangle ids that are on closenode
        double[][][] distNodes = new double[nbOfTrianglesAtNode][3][2]; // [NodeNeighbour][Ecke][x,y]
        int zähler = 0;   // for loop counter
        int closeNodeOld; // to track where the closest node was in previous steps
        int IDIndex = 0; // triangle ID Index (Index at TrianglesAtNodes) with closest node to particle

        //
        //___________________start for loop to test next nearest node until particle is found_____________________________________________________________________________________________
        //
        for (int f = 0; f < 25; f++) {
            nbOfTrianglesAtNode = NodeNeighbours[closeNode].length;
            TrianglesAtNode = new int[nbOfTrianglesAtNode]; // triangle ids that are on closenode
            distNodes = new double[nbOfTrianglesAtNode][3][2]; // [NodeNeighbour][Ecke][x,y]
            // get neighbouring triangles for testing if particle is inside
            for (int j = 0; j < nbOfTrianglesAtNode; j++) {
                try {
                    TrianglesAtNode[j] = NodeNeighbours[closeNode][j];
                    for (int k = 0; k < 2; k++) {                           //save already checked triangles in history
                        if (TrianglesAtNode[j] == triangleHistory[k][0]) {
                            triangleHistory[k][1] = j;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("NodeNeighbours[" + closeNode + "][" + j + "]    length:" + NodeNeighbours[closeNode].length + "    nb:" + nbOfTrianglesAtNode);
                    e.printStackTrace();
                }
            }

            // test if particle is in one of the neighbouring triangles
            for (int i = 0; i < nbOfTrianglesAtNode; i++) {
                if (TrianglesAtNode[i] > -1) {
                    for (int j = 0; j < 3; j++) {
                        xTri[j] = vertices[triangleNodes[TrianglesAtNode[i]][j]][0];
                        yTri[j] = vertices[triangleNodes[TrianglesAtNode[i]][j]][1];
                        distNodes[i][j][0] = Math.abs(xTri[j] - x);
                        distNodes[i][j][1] = Math.abs(yTri[j] - y);
                    }
                    // if triangle has not been tested before, check if particle is inside
                    if (TrianglesAtNode[i] != triangleHistory[0][0] || TrianglesAtNode[i] != triangleHistory[1][0]) {

                        w = getBarycentricWeighing(xTri[0], xTri[1], xTri[2], yTri[0], yTri[1], yTri[2], x, y);

                        wnegative = 0;
                        for (int j = 0; j < 3; j++) {
                            if (w[j] < 0) {
                                wnegative = -1;
                            }
                        }
                        if (wnegative == 0) {
                            //System.out.println("particle is in triangle: " + TrianglesAtNode[i] + " was in triangle: " + id);
                            return TrianglesAtNode[i];
                        }
                        //check if particle left modelarea from this triangle:
                        getBoundaryIntersection(TrianglesAtNode[i], xold, yold, x, y);
                    }
                    // set distance to particle to Infinity if no nodeneighbour is available in the slot
                } else {
                    for (int j = 0; j < 3; j++) {
                        distNodes[i][j][0] = Float.POSITIVE_INFINITY;
                        distNodes[i][j][1] = Float.POSITIVE_INFINITY;
                    }
                }
            }
            // get the closest node of this triangles:
            minDist = Float.POSITIVE_INFINITY;
            minDistIndex = 0;
            IDIndex = 0;
            for (int h = 0; h < nbOfTrianglesAtNode; h++) {
                for (int i = 0; i < 3; i++) {
                    if (distNodes[h][i][0] != Float.POSITIVE_INFINITY) {
                        if (Math.sqrt((distNodes[h][i][0] * distNodes[h][i][0]) + (distNodes[h][i][1] * distNodes[h][i][1])) < minDist) {
                            minDist = Math.sqrt((distNodes[h][i][0] * distNodes[h][i][0]) + (distNodes[h][i][1] * distNodes[h][i][1]));
                            IDIndex = h;
                            minDistIndex = i;
                            //System.out.println("mindist " + minDist + " bei: " + IDIndex + " und " + minDistIndex);
                        }
                    }
                }
            }
            closeNodeOld = closeNode;
            double distnodex = distNodes[IDIndex][minDistIndex][0];
            closeNode = triangleNodes[TrianglesAtNode[IDIndex]][minDistIndex];

            // update triangle history for next steps
            triangleHistory[0][0] = TrianglesAtNode[IDIndex];
            triangleHistory[0][1] = IDIndex;

            for (int j = 0; j < nbOfTrianglesAtNode; j++) {
                for (int i = 0; i < 3; i++) {
                    if (distNodes[j][i][0] == distnodex && TrianglesAtNode[j] != triangleHistory[0][0]) {
                        triangleHistory[1][0] = TrianglesAtNode[j];
                        triangleHistory[1][1] = j;
                    }
                }
            }

            if (closeNodeOld == closeNode) {
                throw new BoundHitException(id, xold, yold);
            }
            zähler += 1;
        }
        // warum hat er immer noch nicht das triangle gefunden?
        //System.out.println("after " + zähler + " loops -> still no triangle found. particle id: " + p.getId() + ", position: " + p.getPosition3D() + ", old triID: " + id + ", new triID: " + triangleHistory[0][0]);
        //System.out.println("distnodex " + distNodes[IDIndex][minDistIndex][0]);
        //id = -1;
        throw new BoundHitException(id, xold, yold);
    }

    /**
     * @deprecated @param p
     * @param id
     * @param xold
     * @param yold
     * @param x
     * @param y
     * @return
     * @throws model.surface.Surface.BoundHitException
     */
    public int getTargetTriangleID_ORGRiss(Particle p, int id, double xold, double yold, double x, double y) throws BoundHitException {

        //if particle didn't move return old id
        if (xold == x && yold == y) {
            return id;
        }
        // is particle still in start triangle? use barycentric weighing to check.
        double[] w = new double[3];
        double[] xTri = new double[3];
        double[] yTri = new double[3];

        for (int i = 0; i < 3; i++) {
            xTri[i] = vertices[triangleNodes[id][i]][0];
            yTri[i] = vertices[triangleNodes[id][i]][1];
        }
        w = getBarycentricWeighing(xTri[0], xTri[1], xTri[2], yTri[0], yTri[1], yTri[2], x, y);
        int wnegative = 0;
        for (int i = 0; i < 3; i++) {
            if (w[i] < 0) {
                wnegative = 1;
            }
        }
        if (wnegative == 0) {
            //System.out.println("particle stayed in start triangle!");
            return id;
        }

        //check if start triangle has less then 3 neumannNeighbours and if so, if particle left model area
        getBoundaryIntersection(id, xold, yold, x, y);

        //is particle in neighbouring triangles of closest node to new particle position?
        double[][] dist = new double[3][3]; // nur für Startdreieck: [Ecke][x,y,resultierende distanz]

        int minDistIndex = 0; // Index which triangle node has smallest distance to particle
        double minDist = Float.POSITIVE_INFINITY;
        int[][] triangleHistory = new int[2][2]; // take triangle ids that have been tested, to skip them in further search [triangleID][Position in TrianglesAtNode]
        triangleHistory[0][0] = id;
        triangleHistory[1][0] = id;
        triangleHistory[0][1] = 0;
        triangleHistory[1][1] = 0;

        //find start triangle node with minimal distance to particle position
        for (int i = 0; i < 3; i++) {
            dist[i][0] = Math.abs(xTri[i] - x);
            dist[i][1] = Math.abs(yTri[i] - y);
            dist[i][2] = Math.sqrt((dist[i][0] * dist[i][0]) + (dist[i][1] * dist[i][1]));
            if (dist[i][2] < minDist) {
                minDist = dist[i][2];
                minDistIndex = i;
            }
        }
        int closeNode = triangleNodes[id][minDistIndex]; // closest triangle node to new particle position
        int nbOfTrianglesAtNode = NodeNeighbours[closeNode].length;
        int[] TrianglesAtNode = new int[nbOfTrianglesAtNode]; // triangle ids that are on closenode
        double[][][] distNodes = new double[nbOfTrianglesAtNode][3][2]; // [NodeNeighbour][Ecke][x,y]
        int zähler = 0;   // for loop counter
        int closeNodeOld; // to track where the closest node was in previous steps
        int IDIndex = 0; // triangle ID Index (Index at TrianglesAtNodes) with closest node to particle

        //
        //___________________start for loop to test next nearest node until particle is found_____________________________________________________________________________________________
        //
        for (int f = 0; f < 25; f++) {
            nbOfTrianglesAtNode = NodeNeighbours[closeNode].length;
            TrianglesAtNode = new int[nbOfTrianglesAtNode]; // triangle ids that are on closenode
            distNodes = new double[nbOfTrianglesAtNode][3][2]; // [NodeNeighbour][Ecke][x,y]
            // get neighbouring triangles for testing if particle is inside
            for (int j = 0; j < nbOfTrianglesAtNode; j++) {
                try {
                    TrianglesAtNode[j] = NodeNeighbours[closeNode][j];
                    for (int k = 0; k < 2; k++) {                           //save already checked triangles in history
                        if (TrianglesAtNode[j] == triangleHistory[k][0]) {
                            triangleHistory[k][1] = j;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("NodeNeighbours[" + closeNode + "][" + j + "]    length:" + NodeNeighbours[closeNode].length + "    nb:" + nbOfTrianglesAtNode);
                    e.printStackTrace();
                }
            }

            // test if particle is in one of the neighbouring triangles
            for (int i = 0; i < nbOfTrianglesAtNode; i++) {
                if (TrianglesAtNode[i] > -1) {
                    for (int j = 0; j < 3; j++) {
                        xTri[j] = vertices[triangleNodes[TrianglesAtNode[i]][j]][0];
                        yTri[j] = vertices[triangleNodes[TrianglesAtNode[i]][j]][1];
                        distNodes[i][j][0] = Math.abs(xTri[j] - x);
                        distNodes[i][j][1] = Math.abs(yTri[j] - y);
                    }
                    // if triangle has not been tested before, check if particle is inside
                    if (TrianglesAtNode[i] != triangleHistory[0][0] || TrianglesAtNode[i] != triangleHistory[1][0]) {

                        w = getBarycentricWeighing(xTri[0], xTri[1], xTri[2], yTri[0], yTri[1], yTri[2], x, y);

                        wnegative = 0;
                        for (int j = 0; j < 3; j++) {
                            if (w[j] < 0) {
                                wnegative = -1;
                            }
                        }
                        if (wnegative == 0) {
                            //System.out.println("particle is in triangle: " + TrianglesAtNode[i] + " was in triangle: " + id);
                            return TrianglesAtNode[i];
                        }
                        //check if particle left modelarea from this triangle:
                        getBoundaryIntersection(TrianglesAtNode[i], xold, yold, x, y);
                    }
                    // set distance to particle to Infinity if no nodeneighbour is available in the slot
                } else {
                    for (int j = 0; j < 3; j++) {
                        distNodes[i][j][0] = Float.POSITIVE_INFINITY;
                        distNodes[i][j][1] = Float.POSITIVE_INFINITY;
                    }
                }
            }
            // get the closest node of this triangles:
            minDist = Float.POSITIVE_INFINITY;
            minDistIndex = 0;
            IDIndex = 0;
            for (int h = 0; h < nbOfTrianglesAtNode; h++) {
                for (int i = 0; i < 3; i++) {
                    if (distNodes[h][i][0] != Float.POSITIVE_INFINITY) {
                        if (Math.sqrt((distNodes[h][i][0] * distNodes[h][i][0]) + (distNodes[h][i][1] * distNodes[h][i][1])) < minDist) {
                            minDist = Math.sqrt((distNodes[h][i][0] * distNodes[h][i][0]) + (distNodes[h][i][1] * distNodes[h][i][1]));
                            IDIndex = h;
                            minDistIndex = i;
                            //System.out.println("mindist " + minDist + " bei: " + IDIndex + " und " + minDistIndex);
                        }
                    }
                }
            }
            closeNodeOld = closeNode;
            double distnodex = distNodes[IDIndex][minDistIndex][0];
            closeNode = triangleNodes[TrianglesAtNode[IDIndex]][minDistIndex];

            // update triangle history for next steps
            triangleHistory[0][0] = TrianglesAtNode[IDIndex];
            triangleHistory[0][1] = IDIndex;

            for (int j = 0; j < nbOfTrianglesAtNode; j++) {
                for (int i = 0; i < 3; i++) {
                    if (distNodes[j][i][0] == distnodex && TrianglesAtNode[j] != triangleHistory[0][0]) {
                        triangleHistory[1][0] = TrianglesAtNode[j];
                        triangleHistory[1][1] = j;
                    }
                }
            }

            if (closeNodeOld == closeNode) {
                throw new BoundHitException(id, xold, yold);
            }
            zähler += 1;
        }
        // warum hat er immer noch nicht das triangle gefunden?
        //System.out.println("after " + zähler + " loops -> still no triangle found. particle id: " + p.getId() + ", position: " + p.getPosition3D() + ", old triID: " + id + ", new triID: " + triangleHistory[0][0]);
        //System.out.println("distnodex " + distNodes[IDIndex][minDistIndex][0]);
        //id = -1;
        throw new BoundHitException(id, xold, yold);
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
    private double[] getParticleBoundaryIntersection(double[] a, double bx, double by, double[] c, double dx, double dy) {
        double t = ((dx - bx) * (by - a[1]) + (dy - by) * (bx - a[0])) / ((c[1] - a[1]) * (dx - bx) - (c[0] - a[0]) * (dy - by));
//        double t = ((dx - bx) * (by - a[1]) + (dy - by) * (a[0] - bx)) / ((c[1] - a[1]) * (dx - bx) - (c[0] - a[0]) * (dy - by));
        double[] s = new double[2];
        double[] s_out = new double[2];
        s[0] = bx + t * (dx - bx);
        s[1] = by + t * (dy - by);
        s_out[0] = bx + t * 0.9 * (dx - bx);
        s_out[1] = by + t * 0.9 * (dy - by);
        // check if intersection is on triangle side and if its in reach of the particlepath:
        if (a[0] < c[0] && s[0] >= a[0] && s[0] <= c[0]) {
            if (bx < dx && s[0] >= bx && s[0] <= dx) {
                return s_out;
            } else if (bx > dx && s[0] >= dx && s[0] <= bx) {
                return s_out;
            } else {
                s[0] = -1;
                return s;
            }
        } else if (a[0] > c[0] && s[0] >= c[0] && s[0] <= a[0]) {
            if (bx > dx && s[0] >= dx && s[0] <= bx) {
                return s_out;
            } else if (bx < dx && s[0] >= bx && s[0] <= dx) {
                return s_out;
            } else {
                s[0] = -1;
                return s;
            }
        } else {
            s[0] = -1;
            return s;
        }
    }

    /**
     * Throws BoundHitException that defines the actual triangle and position of
     * the particle.
     *
     * @param triangleID
     * @param xold position of particle befor emoving.
     * @param yold
     * @param x actual position of the particle
     * @param y
     * @throws model.surface.Surface.BoundHitException
     */
    private void getBoundaryIntersection(int triangleID, double xold, double yold, double x, double y) throws BoundHitException {
        //check if triangle has less then 3 neumannNeighbours and if so, if particle left model area
        for (int n = 0; n < 3; n++) {
            if (neumannNeighbours[triangleID][n] < 0) {
                // check if intersection between triangle sides and particle path leads to model boundary
                // with a-c triangle side vector and b-d particle path vector
//                int[] min = {0, 1, 2}; // variation der dreiecksknoten um die drei seiten zu testen
//                int[] max = {1, 2, 0};
                for (int t = 0; t < 3; t++) {           // every triangle side is tested
                    double[] a = vertices[triangleNodes[triangleID][t % 3]];  //vertices[triangleNodes[triangleID][min[t]]];
                    double[] c = vertices[triangleNodes[triangleID][(t + 1) % 3]];

                    // check if lines are paralell, if not get intersection point if existing
                    if (Math.abs((y - yold) / (x - xold) - (c[1] - a[1]) / (c[0] - a[0])) > 0.0000001) {
                        double[] intersec = getParticleBoundaryIntersection(a, xold, yold, c, x, y);
                        // check if behind this intersection a neighbour is available:
                        if (intersec[0] >= 0) {
                            for (int v = 0; v < 3; v++) {                   //neighbour 
                                if (neumannNeighbours[triangleID][v] < 0) {
                                    continue;
                                }
                                for (int v2 = 0; v2 < 3; v2++) {            //first matching neighbour vertice
                                    for (int v3 = 0; v3 < 3; v3++) {        //second matching neighbour vertice

                                        if (vertices[triangleNodes[neumannNeighbours[triangleID][v]][v2]] == vertices[triangleNodes[triangleID][t % 3]]) {
                                            if (vertices[triangleNodes[neumannNeighbours[triangleID][v]][v3]] == vertices[triangleNodes[triangleID][(t + 1) % 3]]) {
                                                //System.out.println("neighbour is available. no particle transport outside of boundary.");
                                                intersec[0] = -1;
                                            }
                                        }
                                    }
                                }

                            }
                            if (intersec[0] >= 0) {
                                //System.out.println("particle was set back into triangle: " + id + " because it overstepped boundary.");
                                throw new BoundHitException(triangleID, intersec[0], intersec[1]);
                            }
                        }
                    } else {
                        //Lines are parallel
                    }
                }
            }
        }

    }

    private double[] getBarycentricWeighing(double x0, double x1, double x2, double y0, double y1, double y2, double px, double py) {
        // barycentric koordinate weighing for velocity calculation
        // x, y = triangle coordinates, p = searched point
        double[] w = new double[4];
        double atri = ((y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2));
        w[1] = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / atri;
        w[2] = ((py - y2) * (x0 - x2) + (x2 - px) * (y0 - y2)) / atri;
        w[0] = 1. - w[1] - w[2];

        w[3] = atri;
        return w;
    }

    private void getBarycentricWeighing_FillArray(double x0, double x1, double x2, double y0, double y1, double y2, double px, double py, double[] barycentricWeights) {
        // barycentric koordinate weighing for velocity calculation
        // x, y = triangle coordinates, p = searched point
        double[] w = barycentricWeights;
        double atri = ((y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2));
        w[1] = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / atri;
        w[2] = ((py - y2) * (x0 - x2) + (x2 - px) * (y0 - y2)) / atri;
        w[0] = 1. - w[1] - w[2];
    }

    public Inlet[] getInlets() {
        return inletArray;
//        if (inlets == null) {
//            return null;
//        }
//        return inlets.values().toArray(new Inlet[inlets.size()]);
    }

//    public TriangleMeasurement[] getTriangleMeasurements() {
//        return measurementRaster;
//    }
    public SurfaceMeasurementRaster getMeasurementRaster() {
        return measurementRaster;
    }

    public int[][] getNodeNeighbours() {
        return NodeNeighbours;
    }

    public void setNodeNeighbours(int[][] node2Tri, boolean calculateWeights) {
        this.NodeNeighbours = node2Tri;

        if (calculateWeights) {
            this.weight = new double[node2Tri.length][];
            for (int i = 0; i < node2Tri.length; i++) {
                if (node2Tri[i] == null) {
                    this.weight[i] = new double[0];
                } else {
                    double[] w = new double[node2Tri[i].length];
                    double f = 1. / (double) w.length;
                    for (int j = 0; j < w.length; j++) {
                        w[j] = f;
                    }
                    this.weight[i] = w;
                }
            }
        }
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
    public void setMeasurementTimeLine(ArrayTimeLineMeasurement tl) {

    }

    @Override
    public ArrayTimeLineMeasurement getMeasurementTimeLine() {
        return null;
    }

    @Override
    public double getWaterHeight() {
        return 0;
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return null;
    }

    @Override
    public double getWaterlevel() {
        return 0;

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
        if (triangleMids == null) {
            return -2;
        }
        int bestID = -1;
        double bestdistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < triangleMids.length; i++) {
            double[] tm = triangleMids[i];
            double tempdist = Math.abs(tm[0] - x) + Math.abs(tm[1] - y);
            if (tempdist < bestdistance) {
                bestdistance = tempdist;
                bestID = i;
            }
        }
        return bestID;
    }

}
