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

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.multievents.PipeResultData;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.SpillScenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.control.scenario.Setup;
import com.saemann.gulli.core.control.scenario.injection.HEAreaInflow1DInformation;
import com.saemann.gulli.core.control.scenario.injection.HEInjectionInformation;
import com.saemann.gulli.core.control.scenario.injection.HE_MessdatenInjection;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.io.FileContainer;
import com.saemann.gulli.core.io.extran.CSV_IO;
import com.saemann.gulli.core.io.extran.HE_Database;
import com.saemann.gulli.core.io.SHP_IO_GULLI;
import com.saemann.gulli.core.io.Setup_IO;
import com.saemann.gulli.core.io.SparseTimeLineDataProvider;
import com.saemann.gulli.core.io.Surface_CSV_IO;
import com.saemann.gulli.core.io.bentley.BentleyDatabase;
import com.saemann.gulli.core.io.extran.GdalIO;
import com.saemann.gulli.core.io.swmm.SWMM_IO;
import com.saemann.gulli.core.io.extran.HE_SurfaceIO;
import com.saemann.gulli.core.io.extran.HE_GDB_IO;
import com.saemann.gulli.core.io.extran.HE_InletReference;
import com.saemann.gulli.core.io.swmm.SWMM_Out_Reader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.SurfaceVelocityLoader;
import com.saemann.gulli.core.model.surface.SurfaceWaterlevelLoader;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelinePipe;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelineManhole;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;
import com.saemann.gulli.core.model.topology.graph.GraphSearch;
import com.saemann.gulli.core.model.topology.graph.Pair;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import main.java.io.zrz.jgdb.GeoDBException;
import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class LoadingCoordinator {

    private Scenario scenario;
    private boolean changedSurface;

    /**
     * Types of input files, than can be handled.
     */
    public enum FILETYPE {
        HYSTEM_EXTRAN_7, HYSTEM_EXTRAN_8, SWMM_5_1, BENTLEY, COUD_CSV
    };

    protected FILETYPE filetype;

    public enum LOADINGSTATUS {

        NOT_REQUESTED, REQUESTED, LOADING, LOADED, ERROR
    }

    private File fileSurfaceCoordsDAT, fileSurfaceTriangleIndicesDAT, FileTriangleNeumannNeighboursDAT,
            fileSurfaceReferenceSystem, fileSurfaceManholes, fileSurfaceInlets;
//            fileSufaceNode2Triangle, fileTriangleMooreNeighbours;
    private File fileSurfaceWaterlevels;
    private LOADINGSTATUS loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
    private LOADINGSTATUS loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;

    private String setupFile = null;

    private File fileNetwork;
    private String crsNetwork = null, crsSurface = null;

    private HE_Database modelDatabase, resultDatabase;
    private static HE_Database tempFBDB;

    /**
     * enables the console logging output.
     */
    public static boolean verbose = false;

    private boolean isLoading = false;

    public final Action action = new Action("LoadingCoordinator", null, false);

    /**
     * Load only neccessary timelines for pipes.
     */
    public boolean sparsePipeLoading = false;

    /**
     * Load only neccessary timelines for surface.
     */
    public boolean sparseSurfaceLoading = true;

    /**
     * Save measurements in a sparse timeline (SparseTimeLineMeasurement) Else
     * save in an always-fully initialized ArrayTimelineMeasurement.
     */
    public boolean sparsePipeMeasurements = true;

    /**
     * Use weights from NODE2TRIANGLe.dat for weighting the nodes' velocities
     * from triangle velocities.
     */
    public boolean weightedSurfaceVelocities = false;

    /**
     * to read the currently performed action.
     */
//    public String currentAction = "";
    private LOADINGSTATUS loadingpipeNetwork = LOADINGSTATUS.NOT_REQUESTED;
    private LOADINGSTATUS loadingPipeResult = LOADINGSTATUS.NOT_REQUESTED;

    private boolean clearOtherResults = false;
    private File fileStreetInletsSHP;
    private LOADINGSTATUS loadingStreetInlets = LOADINGSTATUS.NOT_REQUESTED;

    private Thread threadLoadingRequests;
    private boolean requestLoading = false;
    private LinkedList<Pair<File, Boolean>> list_loadingPipeResults = new LinkedList<>();
    private File fileMainPipeResult = null;
//    private boolean loadOnlyMainFile = true;

    private boolean loadGDBVelocity = true;
    private boolean changedPipeNetwork = false;
    private boolean cancelLoading = false;

    /**
     * Number of Milliseconds that are added at the end of the simulation to let
     * particles be transported after the scenario has ended providing flow
     * field information.
     */
    public long additionalMilliseconds = 0;

    /**
     * Holder of actual "in use" objects (network/surface)
     */
    private final Controller control;

    private String resultName = "";

    /**
     * Contains definition of injections that are added manually by the user to
     * be part of the simulation
     */
    private final ArrayList<InjectionInfo> manualInjections = new ArrayList<>();

    /**
     * Add Injections from read from the result (e.g. HE
     * Schmutzfrachteinleitung) as Injectionspills of this scenario.
     */
    public boolean loadResultInjections = true;

    /**
     * List contains manual and scenario injection, loaded from result files.
     * This list will be used in the scenario.
     */
    private final ArrayList<InjectionInfo> totalInjections = new ArrayList<>();

    public final ArrayList<Runnable> loadingFinishedListener = new ArrayList<>(2);

    /**
     * Matches manholes to surface triangle ids
     */
    private ArrayList<Pair<String, Integer>> manhRefs;

    /**
     * Matches Surface Inlets from triangle ID to network pipe.
     */
    private ArrayList<HE_InletReference> inletRefs;

    private final ArrayList<LoadingActionListener> listener = new ArrayList<>(3);

    protected Surface surface;

    protected Network network;

    public static DecimalFormat df1k;

    /**
     * If true, each event starts at 0 ms if false, the real time of the event
     * is used;
     */
    public boolean zeroTimeStart = true;

    public LoadingCoordinator(Controller control) {
        this.control = control;

        DecimalFormatSymbols dfsymb = new DecimalFormatSymbols(StartParameters.formatLocale);
        dfsymb.setGroupingSeparator(' ');
        df1k = new DecimalFormat("#,##0", dfsymb);
        df1k.setGroupingSize(3);

        this.addActioListener(control);

    }

    public boolean addActioListener(LoadingActionListener liste) {
        if (!listener.contains(liste)) {
            return listener.add(liste);
        }
        return false;
    }

    public boolean removeActionListener(LoadingActionListener listene) {
        return listener.remove(listene);
    }

    /**
     * Starts a thread, that works a list of requested loading operations. Files
     * have to be requested prior to this call.
     *
     * @param asThread
     */
    public void startLoadingRequestedFiles(boolean asThread) {
        if (asThread && threadLoadingRequests != null && threadLoadingRequests.isAlive()) {
            //Thread is already running.
            //Mark it for rechecking the requests after finishing loop.
            requestLoading = true;
            return;
        }
        threadLoadingRequests = new Thread("LoadingRequestedFiles") {

            @Override
            public void run() {
                long startLoadingtime = System.currentTimeMillis();
                requestLoading = true;
                isLoading = true;
                action.child = null;
                action.description = "Loading";
                action.hasProgress = false;
                action.progress = 0;
                while (requestLoading) {
                    if (verbose) {
                        System.out.println("start Loading loop.");
                    }
                    cancelLoading = false;
                    requestLoading = false;
                    changedPipeNetwork = false;
                    changedSurface = false;
                    if (loadingpipeNetwork == LOADINGSTATUS.REQUESTED) {
                        network = loadPipeNetwork();
                        changedPipeNetwork = true;
                        if (network == null) {
                            loadingpipeNetwork = LOADINGSTATUS.ERROR;
                            System.err.println("Network could not be loaded.");
                        }
                    }
                    if (isInterrupted()) {
                        break;
                    }
                    scenario = null;

                    if (loadingPipeResult == LOADINGSTATUS.REQUESTED) {
                        loadPipeVelocities(network, zeroTimeStart);
                    }
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    if (network != null) {
                        for (LoadingActionListener ll : listener) {
                            ll.loadNetwork(network, this);
                        }
                    }
                    // Loading Surface topology
                    if (loadingSurface == LOADINGSTATUS.REQUESTED) {
                        manhRefs = null;
                        inletRefs = null;
                        surface = loadSurface();
                        changedSurface = true;

                    }
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    // Connect Surface and pipesystem
                    if (changedSurface || changedPipeNetwork) {
                        if (surface != null && network != null) {
                            try {
                                if (verbose) {
                                    System.out.println("ReMapping surface&network");
                                }
                                surface.clearNamedCapacityMap();
                                mapManholes(surface, network);
                                mapStreetInlets(surface, network);
                            } catch (TransformException ex) {
                                Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    if (changedSurface) {
                        for (InjectionInfo inj : manualInjections) {
                            if (inj.spillOnSurface()) {
                                inj.setCapacity(surface);
                            }
                        }
                    }
                    if (changedPipeNetwork) {
                        for (InjectionInfo inj : manualInjections) {
                            if (inj.spillInManhole()) {
                                inj.setCapacity(null);
                            }
                            if (inj instanceof HEAreaInflow1DInformation) {
                                ((HEAreaInflow1DInformation) inj).setNetwork(network);
                            }
                        }
                    }
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    // Start loading Waterlevels and calculate surface triangle velocities 
                    if (loadingSurfaceVelocity == LOADINGSTATUS.REQUESTED) {
                        if (surface != null) {
                            action.progress = 0f;
                            loadSurfaceVelocity(surface);
                        } else {
                            loadingSurfaceVelocity = LOADINGSTATUS.ERROR;

                        }
                    }
                    if (surface != null && surface.waterlevelLoader == null) {
//                        System.out.println("Waterlevelloader is null try to use gradient calculation filetype: " + filetype);
                        if (filetype == FILETYPE.SWMM_5_1) {
                            if (surface.triangle_downhilldirection == null) {
                                surface.calculateDownhillSlopes();
                            }
                            if (scenario != null && scenario.getEndTime() != 0) {
                                //Use a copy of the exisiting pipe 
                                surface.setTimeContainer(new TimeIndexContainer(new long[]{scenario.getStartTime(), scenario.getEndTime()}));
                            } else {
                                surface.setTimeContainer(new TimeIndexContainer(new long[]{0, Long.MAX_VALUE}));

                            }
                            surface.waterlevelLoader = new SurfaceWaterlevelLoader() {
                                @Override
                                public float[] loadWaterlevlvalues(int triangleID) {
                                    return new float[]{0.1f, 0.1f};
                                }

                                @Override
                                public float loadZElevation(int triangleID) {
                                    return 0;
                                }
                            };
                            surface.velocityLoader = new SurfaceVelocityLoader() {
                                @Override
                                public float[][] loadVelocity(int triangleID) {
                                    float[] v = new float[2];
                                    v[0] = surface.triangle_downhilldirection[triangleID][0] * surface.triangle_downhillIntensity[triangleID] * 10f;
                                    v[1] = surface.triangle_downhilldirection[triangleID][1] * surface.triangle_downhillIntensity[triangleID] * 10f;
                                    float[][] tl = new float[2][2];
                                    tl[0] = v;
                                    tl[1] = v;
                                    return tl;
                                }
                            };
                            surface.initSparseTriangleVelocityLoading(surface.velocityLoader, true, false);
                            try {
                                surface.setMeasurementRaster(new SurfaceMeasurementTriangleRaster(surface, 1, new TimeIndexContainer(TimeContainer.byIntervallMilliseconds(surface.getStartTime(), surface.getEndTime(), 900000)), control.getThreadController().getParticleThreads().length));

                            } catch (Exception e) {
                            }
                            if (verbose) {
                                System.out.println("Created a constant downstream flow velocity loader");
                            }
                            fileSurfaceWaterlevels = fileSurfaceCoordsDAT;
                            loadingSurfaceVelocity = LOADINGSTATUS.LOADED;
                        } else if (filetype == FILETYPE.COUD_CSV) {
                            //DO nothing. the velocity and scenario have already been initialized.
                        } else {
                            long start = 0;
                            long end = 0;
                            TimeIndexContainer ticsurf = createTimeContainer(start, end, 2);
                            surface.setTimeContainer(ticsurf);
                            surface.setMeasurementRaster(new SurfaceMeasurementTriangleRaster(surface, 1, ticsurf, control.getThreadController().getNumberOfParallelThreads()));
                            if (scenario != null) {
                                scenario.setStatusTimesSurface(surface);
                                scenario.setMeasurementsSurface(surface.getMeasurementRaster());
                            }
                            surface.initSparseTriangleVelocityLoading(new SurfaceVelocityLoader() {
                                @Override
                                public float[][] loadVelocity(int triangleID) {
                                    float[][] f = new float[2][2];
                                    f[0] = surface.triangle_downhilldirection[triangleID];
                                    f[1] = f[0];
                                    return f;
                                }
                            }, true, false);

                        }
                    }

                    if (loadingSurfaceVelocity == LOADINGSTATUS.LOADED) {
                        ParticlePipeComputing.spillOutToSurface = true;
                    } else {
                        ParticlePipeComputing.spillOutToSurface = false;
                        System.err.println("Disabled Spill to surface because no Velocity field was loaded.");
                    }

                    //Send information to UserInterface if GUI exists
                    for (LoadingActionListener listener1 : listener) {
                        action.description = "Surface inform " + listener1;
                        listener1.loadSurface(surface, this);
                        action.description = "completed Surface inform " + listener1;
                        fireLoadingActionUpdate();
                    }

                    if (scenario != null) {
                        action.description = "Load Scenario";
                        action.progress = 0f;
                        fireLoadingActionUpdate();
                        for (LoadingActionListener ll : listener) {
                            ll.loadScenario(scenario, this);
                        }
                    }
                    //Inform controller about updated timecontainer.
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    if (requestLoading) {
                        if (verbose) {
                            System.out.println(getClass() + " is requested to load again.");
                        }
                    }
                }
                isLoading = false;
                action.description = "Loading completed";
                action.progress = 1f;
                fireLoadingActionUpdate();

                if (!this.isInterrupted()) {
                    for (Runnable r : loadingFinishedListener) {
                        if (r == null) {
                            continue;
                        }
                        r.run();
                    }
                } else {
                    System.out.println("Loading thread interrupted. Do not inform listeners at the end");
                }

                System.out.println("Loading took " + (System.currentTimeMillis() - startLoadingtime) + " ms in total.");
            }
        };

        if (asThread) {
            threadLoadingRequests.start();
        } else {
            threadLoadingRequests.run();
        }
    }

    private Network loadPipeNetwork() {
        loadingpipeNetwork = LOADINGSTATUS.LOADING;
        action.description = "Load network";
        if (verbose) {
            System.out.println("went into loading Network '" + fileNetwork + "'");
        }
        fireLoadingActionUpdate();

        if (!fileNetwork.exists()) {
            action.description = "File does not exists @" + fileNetwork;
            loadingpipeNetwork = LOADINGSTATUS.ERROR;
            return null;
        }

        try {
            Network nw;
            if (fileNetwork.getName().endsWith(".idbf") || fileNetwork.getName().endsWith(".idbm") || fileNetwork.getName().endsWith(".idbr")) {
                if (modelDatabase == null) {
                    if (tempFBDB != null && tempFBDB.getDatabaseFile().equals(fileNetwork)) {
                        modelDatabase = tempFBDB;
                    } else {
                        modelDatabase = new HE_Database(fileNetwork, true);
                    }
                } else if (!modelDatabase.getDatabaseFile().getAbsolutePath().equals(fileNetwork.getAbsolutePath())) {
                    modelDatabase = new HE_Database(fileNetwork, true);
                }
                String crsCode = null;

                try {
                    crsCode = modelDatabase.loadCoordinateReferenceSystem();
                    if (verbose) {
                        System.out.println("Model Database's CRS: " + crsCode);
                    }
                } catch (Exception exception) {
                    System.err.println("Problem with Coordinate Reference System in Model file " + modelDatabase.getDatabaseFile().getAbsolutePath());
//                    exception.printStackTrace();
                }
                if (crsCode == null) {
                    if (crsNetwork != null && !crsNetwork.isEmpty()) {
                        crsCode = crsNetwork;
                    } else {
                        crsCode = "EPSG:25832";
                        System.err.println("No CRS information found. Use " + crsCode + " as CRS for Network geometry.");
                    }
                } else {
                    crsNetwork = crsCode;
                }
                nw = modelDatabase.loadNetwork(CRS.decode(crsCode));//HE_Database.loadNetwork(fileNetwork);

                if (fileNetwork.getName().toLowerCase().endsWith("idbf")) {
                    this.filetype = FILETYPE.HYSTEM_EXTRAN_7;
                } else {
                    this.filetype = FILETYPE.HYSTEM_EXTRAN_8;
                }

            } else if (fileNetwork.getName().endsWith(".inp")) {
                nw = SWMM_IO.readNetwork(fileNetwork, crsNetwork);
                this.filetype = FILETYPE.SWMM_5_1;
            } else if (fileNetwork.getName().endsWith(".sqlite")) {
                BentleyDatabase bentleyDB = new BentleyDatabase(fileNetwork);
                if (crsNetwork == null || crsNetwork.isEmpty()) {
                    crsNetwork = "EPSG:2178";
                }
                nw = bentleyDB.loadNetwork(crsNetwork);
                this.filetype = FILETYPE.BENTLEY;
            } else {
                loadingpipeNetwork = LOADINGSTATUS.ERROR;
                throw new Exception("File extension not known for '" + fileNetwork.getName() + "'");
            }
            if (cancelLoading) {
                loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
                return null;
            }
            changedPipeNetwork = true;
            if (verbose) {
                System.out.println("Loaded Network from file '" + fileNetwork + "'");
            }

            //Reset capacity reference from Injections because the object only exist in the old network and has to be found again in the new one
            for (InjectionInfo injection : manualInjections) {
                if (injection == null) {
                    continue;
                }
                injection.setCapacity(null);
            }
            for (InjectionInfo injection : totalInjections) {
                if (injection == null) {
                    continue;
                }
                injection.setCapacity(null);
            }

            loadingpipeNetwork = LOADINGSTATUS.LOADED;
            return nw;
        } catch (Exception ex) {
            loadingpipeNetwork = LOADINGSTATUS.ERROR;
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Decodes the result file for pipe velocities and fills the
     * discharge/velocity/massflux values in the pipes' and manholes' timelines.
     *
     * returns true if all information has been loaded successfully.
     *
     * @param nw
     */
    private boolean loadPipeVelocities(Network nw, boolean startAtZeroTime) {
        loadingPipeResult = LOADINGSTATUS.LOADING;
        action.description = "Load pipe velocities";
        action.progress = 0;
        fireLoadingActionUpdate();
        String scenarioName = "";
//        if (loadOnlyMainFile) {
        //Main File
        if (nw == null) {
            loadingPipeResult = LOADINGSTATUS.ERROR;
        } else {
            boolean loaded = false;
            try { //clear other results
                if (clearOtherResults) {
//                    control.getMultiInputData().clear();
                    control.getThreadController().cleanFromParticles();
                }

                TimeIndexContainer timeContainerPipe = null;
                TimeIndexContainer timeContainerManholes = null;
                //Load manualInjections. Neede to calculate transport paths.

                if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {
                    action.description = "Open HE Database";
                    if (resultDatabase == null || resultDatabase.getDatabaseFile() == null || !resultDatabase.getDatabaseFile().getAbsolutePath().equals(fileMainPipeResult.getAbsolutePath())) {
                        if (tempFBDB != null && tempFBDB.getDatabaseFile().equals(fileMainPipeResult)) {
                            resultDatabase = tempFBDB;
                        } else {
                            resultDatabase = new HE_Database(fileMainPipeResult, true);
                        }
                    }
                    resultDatabase.loadingAction = this.action;
                    ArrayList<HEInjectionInformation> he_injection = null;
                    resultName = resultDatabase.readResultname();
                    scenarioName = resultName;
                    action.description = "Load spill events";
//                    System.out.println("load file injections? " + loadResultInjections);
                    totalInjections.clear();
                    //Read definition of washoff parameters as they might be needed for the injection definition (e.g. HEAreaInflow1D)
                    try {
                        ArrayList<String> washoffList = resultDatabase.readWashoffParametersets();
                        String[] washoffParameters = new String[washoffList.size() + 1];
                        washoffParameters[0] = "All";
                        int i = 1;
                        for (String string : washoffList) {
                            washoffParameters[i] = string;
                            i++;
                        }
                        HEAreaInflow1DInformation.runoffParameterList = washoffParameters;
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    if (this.loadResultInjections) {
                        he_injection = resultDatabase.readInjectionInformation(startAtZeroTime);
                        ArrayList<HEAreaInflow1DInformation> areainjections = resultDatabase.readInjectionFrom1DArea();
                        totalInjections.addAll(areainjections);
                        System.out.println("loaded " + (he_injection.size() + areainjections.size()) + " injections from HE Result DB file.");
                    } else {
                        he_injection = new ArrayList<>(0);
                    }
//                    System.out.println("loaded " + he_injection.size() + " from database");
                    long[] times = resultDatabase.loadTimeStepsNetwork(startAtZeroTime);
                    long scenariostart = times[0];
                    long scenarioEnd = times[times.length - 1];

                    long starttime = System.currentTimeMillis();
                    int materialnumber = 0;
                    for (HEInjectionInformation in : he_injection) {
                        Capacity c = null;
                        if (network == null) {
                            System.err.println("No network loaded. Can not apply Injection Information to Capacity '" + in.capacityName + "'.");
                            continue;
                        }
                        c = network.getCapacityByName(in.capacityName);
                        if (c == null) {
                            System.err.println("Could not find Capacity '" + in.capacityName + "' in Network.");
                            continue;
                        }
                        double start = (in.stattime - scenariostart) / 1000;
                        double duration = (in.endtime - scenariostart) / 1000 - start;
                        Material mat = in.material;
                        if (mat == null) {
                            mat = new Material("Schmutz " + materialnumber++, 1000, true);
                        }
                        int particlenumber = 10000;///he_injection.size();
                        InjectionInformation info;
                        if (in instanceof HE_MessdatenInjection) {
                            HE_MessdatenInjection mess = (HE_MessdatenInjection) in;
                            info = new InjectionInformation(c, scenariostart, scenarioEnd, mess.timedValues, mat, mess.getConcentration(), particlenumber);
                        } else {
                            info = new InjectionInformation(c, 0, in.mass, particlenumber, mat, start, duration);
                        }

                        if (c instanceof Pipe) {
                            info.setPosition1D(in.relativePosition);
                            info.setPosition(c.getPosition3D(info.getPosition1D()));
                        }
                        if (totalInjections.contains(info)) {
                            totalInjections.remove(info);
                        }
                        if (verbose) {
                            System.out.println("Add injection: " + info.getMass() + "kg @" + in.capacityName + "  start:" + info.getStarttimeSimulationsAfterSimulationStart() + "s  last " + info.getDurationSeconds() + "s");
                        }
                        totalInjections.add(info);
                    }

                    //Find injection manholes
                    ArrayList<Capacity> injManholes = new ArrayList<>(he_injection.size());
                    if (verbose) {
                        System.out.println("Injections: " + manualInjections.size());
                    }
                    for (InjectionInfo inj : manualInjections) {
                        if (inj instanceof InjectionInformation) {
                            if (inj.getCapacity() == null && inj.spillInManhole() && ((InjectionInformation) inj).getCapacityName() != null && nw != null) {
                                //Search for injection reference
                                Capacity c = network.getCapacityByName(((InjectionInformation) inj).getCapacityName());
                                inj.setCapacity(c);
                            }
                            if (inj.getCapacity() != null) {
                                Capacity c = inj.getCapacity();
                                if (c != null && !injManholes.contains(c)) {
                                    injManholes.add(c);
                                }
                            }
                            if (inj.getCapacity() == null && inj.spillInManhole() && inj.getPosition() != null) {
                                if (verbose) {
                                    System.out.println("Search for Manhole near position " + inj.getPosition());
                                }
                                inj.setCapacity(network.getManholeNearPositionLatLon(((InjectionInformation) inj).getPosition()));
                                if (verbose) {
                                    System.out.println("found " + inj.getCapacity());
                                }
                            }
                            if (totalInjections.contains(inj)) {
                                totalInjections.remove(inj);
                            }
                            if (verbose) {
                                System.out.println("Add injection: " + inj.getMass() + "kg @" + ((InjectionInformation) inj).getCapacityName() + "  start:" + inj.getStarttimeSimulationsAfterSimulationStart() + "s  last " + inj.getDurationSeconds() + "s");
                            }
                            totalInjections.add(inj);
                        }
                    }
                    if (sparsePipeLoading) {
                        //load minmax velocity
                        action.description = "Load Maximum velocity";
                        float[][] minmax = resultDatabase.getMinMaxVelocity();
                        LinkedList<Pipe> pipesToLoad = new LinkedList<>();
                        LinkedList<StorageVolume> manholesToLoad = new LinkedList<>();
                        action.description = "Calculate downstream graph";
                        //Search downstream pipes
                        for (Capacity cap : injManholes) {
                            StorageVolume mh;
                            if (cap instanceof StorageVolume) {
                                mh = (Manhole) cap;
                            } else if (cap instanceof Pipe) {
                                mh = ((Pipe) cap).getStartConnection().getManhole();
                            } else {
                                System.out.println(getClass() + ": Can not find downstream transport paths for " + cap);
                                continue;
                            }
                            Pipe[] path = GraphSearch.findDownstreamPipes(nw, mh, minmax);
                            for (Pipe p : path) {
                                if (!pipesToLoad.contains(p)) {
                                    pipesToLoad.add(p);
                                    if (!manholesToLoad.contains(p.getStartConnection().getManhole())) {
                                        manholesToLoad.add(p.getStartConnection().getManhole());
                                    }
                                    if (!manholesToLoad.contains(p.getEndConnection().getManhole())) {
                                        manholesToLoad.add(p.getEndConnection().getManhole());
                                    }
                                }
                            }
                        }

                        StringBuilder str = new StringBuilder();
                        ArrayList<Pipe> list = new ArrayList<>(pipesToLoad);
                        Collections.sort(list, new Comparator<Pipe>() {
                            @Override
                            public int compare(Pipe t, Pipe t1) {
                                if (t.getManualID() == t1.getManualID()) {
                                    return 0;
                                }
                                if (t.getManualID() < t1.getManualID()) {
                                    return -1;
                                }
                                return 1;
                            }
                        });
                        if (verbose) {
                            System.out.println("Request Pipes for SparseTimeline: " + pipesToLoad.size() + "/" + nw.getPipes().size() + " : " + str.toString());
                        }
                        if (verbose) {
                            System.out.println("Request Manholes  SparseTimeline: " + manholesToLoad.size() + "/" + nw.getManholes().size());
                        }
                        action.description = "Load downstream pipe velocities";
                        //Request Timelines for pipes and manholes & Create Timeindex container
                        Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> cs = sparseLoadTimelines(nw, resultDatabase, pipesToLoad, manholesToLoad, zeroTimeStart, additionalMilliseconds);
                        if (verbose) {
                            System.out.println(getClass() + ": Loaded sparsecontainer " + ((System.currentTimeMillis() - starttime) + "ms."));
                        }
                        timeContainerPipe = cs.first;
                        timeContainerManholes = cs.second;
                        loaded = true;
                        resultDatabase.close();
                    }
                } else if (fileMainPipeResult.getName().endsWith("out")) {
                    //SWMM 5 output file
                    action.description = "Open out file";
                    SWMM_Out_Reader reader = new SWMM_Out_Reader(fileMainPipeResult);

                    Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> cs = sparseLoadTimelines(network, reader, new ArrayList(0), new ArrayList(0), zeroTimeStart, additionalMilliseconds);
                    timeContainerPipe = cs.first;
                    timeContainerManholes = cs.second;
                    if (loadResultInjections) {
                        //Not implemented
                    }

                    loaded = true;
                }

                if (!loaded) {
                    //Load all values for all pipes/manholes.
                    Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> p;
                    action.description = "Loading all pipe velocities";
                    if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {
                        if (zeroTimeStart) {
                            long start = resultDatabase.getStartTimeNetwork();
                            p = resultDatabase.applyTimelines(nw, start, additionalMilliseconds);//HE_Database.readTimelines(fileMainPipeResult, control.getNetwork());
                        } else {
                            p = resultDatabase.applyTimelines(nw, 0, additionalMilliseconds);
                        }
                        scenarioName = resultDatabase.readResultname();
                    } else if (fileMainPipeResult.getName().endsWith(".rpt")) {
                        scenarioName = fileMainPipeResult.getName();
                        p = SWMM_IO.readTimeLines(fileMainPipeResult, nw);
                    } else {
                        throw new Exception("Not known filetype '" + fileMainPipeResult.getName() + "'");
                    }
                    timeContainerPipe = p.first;
                    timeContainerManholes = p.second;
                    PipeResultData data = new PipeResultData(fileMainPipeResult, fileMainPipeResult.getName(), p.first, p.second);
                    loaded = true;
                    //Add only this result information
                    control.setPipeResultData(data);
                }

                if (cancelLoading) {
                    loadingPipeResult = LOADINGSTATUS.REQUESTED;
                    return false;
                }

                if (manualInjections != null) {
                    for (InjectionInfo mi : manualInjections) {
                        if (!totalInjections.contains(mi)) {
                            totalInjections.add(mi);
                        }
                    }
                }

                for (int i = 0; i < totalInjections.size(); i++) {
                    totalInjections.get(i).setId(i);
                }

                action.description = "Load create scenario";
                if (scenario == null) {
                    scenario = new SpillScenario(timeContainerPipe, totalInjections);
                }
                scenario.setStatusTimesPipe(timeContainerPipe);
                scenario.setTimesManhole(timeContainerManholes);
                scenario.setName(scenarioName);

                loadingPipeResult = LOADINGSTATUS.LOADED;
                return true;
            } catch (Exception ex) {
                Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                loadingPipeResult = LOADINGSTATUS.ERROR;
            }
        }
        return false;
    }

    private Surface loadSurface() {
        loadingSurface = LOADINGSTATUS.LOADING;
        action.child = null;
        action.hasProgress = false;
        action.progress = 0;
        action.description = "Load surface grid";
        fireLoadingActionUpdate();
        long start = System.currentTimeMillis();
        if (this.surface != null) {
            this.surface.freeMemory();
            this.surface = null;
            control.loadSurface(surface, this);
            if (scenario != null) {
                scenario.setMeasurementsSurface(null);
                scenario.setStatusTimesSurface(null);
            }
            System.gc();
        }
        if (fileSurfaceCoordsDAT != null) {
            Surface surf = null;
            if (fileSurfaceCoordsDAT.getName().endsWith(".dat")) {
                //HYSTEM EXTRAN SURFACE           
                try {

                    HE_SurfaceIO.loadingAction = action;
                    surf = HE_SurfaceIO.loadSurface(fileSurfaceCoordsDAT, fileSurfaceTriangleIndicesDAT, FileTriangleNeumannNeighboursDAT, fileSurfaceReferenceSystem);
                    crsSurface = surf.getSpatialReferenceCode();

//            System.gc();
                } catch (Exception ex) {
                    loadingSurface = LOADINGSTATUS.ERROR;
                    action.description = ex.getLocalizedMessage();
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else if (fileSurfaceCoordsDAT.getName().endsWith(".csv")) {
                try {
                    //CoUD Labs File format
                    if (Surface_CSV_IO.is_readable_scheme(fileSurfaceCoordsDAT)) {
                        surf = Surface_CSV_IO.createTriangleSurfaceGeometry(fileSurfaceCoordsDAT, 2, 8);
                        filetype = FILETYPE.COUD_CSV;
                    }
                } catch (Exception ex) {
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (cancelLoading) {
                loadingSurface = LOADINGSTATUS.REQUESTED;
                return null;
            }
//            start = System.currentTimeMillis();

            //Reset triangle IDs from Injections because the coordinate might have changed
            for (InjectionInfo injection : manualInjections) {
                if (injection.getPosition() != null) {
                    if (injection instanceof InjectionInformation) {
                        ((InjectionInformation) injection).setTriangleID(-1);
                    }
                }
            }
            changedSurface = true;
            action.description = "Surface loaded";
            fireLoadingActionUpdate();
            loadingSurface = LOADINGSTATUS.LOADED;
            return surf;
        }
        System.gc();
        return null;
    }

    private boolean loadSurfaceVelocity(Surface surface) {
        loadingSurfaceVelocity = LOADINGSTATUS.LOADING;
        action.description = "Load surface velocities";
        fireLoadingActionUpdate();
        try {
            if (surface != null) {
                if (fileSurfaceWaterlevels.getName().endsWith(".gdb")) {
                    if (HE_GDB_IO.isReadyInstalled()) {
                        //Do nothing, shall be loaded with the working driver.
                        //does not need to be converted via GDAL command line .
                    } else {
                        System.err.println("Can not read *.gdb files because the rGDB.jar is not correctly installed.");
                        loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                    }
                }
                // Now start loading waterlevels to surface
                long starttime = System.currentTimeMillis();
                action.description = "Load surface velocities";
                fireLoadingActionUpdate();

                String lowername = fileSurfaceWaterlevels.getName().toLowerCase();
                if (verbose) {
                    System.out.println("loading waterlevels from " + fileSurfaceWaterlevels.getParentFile().getName() + "/" + fileSurfaceWaterlevels.getName());
                }
                SurfaceVelocityLoader velocityLoader = null;
                SurfaceWaterlevelLoader waterlevelLoader = null;
                boolean surfaceVelocitiescompletelyLoaded = false;

                if (lowername.endsWith("shp")) {
                    SHP_IO_GULLI.readTriangleFileToSurface(fileSurfaceWaterlevels, surface, verbose);
                } else if (lowername.endsWith("csv")) {

                    //First attempt is to load CoUD Labs csv scheme
                    if (Surface_CSV_IO.is_readable_scheme(fileSurfaceWaterlevels)) {
                        filetype = FILETYPE.COUD_CSV;
                        sparseSurfaceLoading = false;
                        Surface_CSV_IO.readSurfaceCellVelocities(surface, fileSurfaceWaterlevels);
                        TimeIndexContainer tic = new TimeIndexContainer(createTimeContainer(0, 3 * 60 * 60 * 1000, 2));
                        surface.setTimeContainer(tic);
                        if (scenario == null) {
                            scenario = new SpillScenario(surface, totalInjections);
                            scenario.setStatusTimesSurface(surface);
                        } else {

                            scenario.setStatusTimesSurface(surface);
                        }

                        scenario.setName(fileSurfaceWaterlevels.getParent() + "/" + fileSurfaceWaterlevels.getName());

                    } else {
                        //If this does not fit (wrong header), we try to go with the old GDAL csv scheme
                        CSV_IO.readTriangleWaterlevels(surface, fileSurfaceWaterlevels);
                    }
                } else if (lowername.endsWith("gdb")) {
                    action.description = "Reading GDB surface";
                    fireLoadingActionUpdate();
//                    try {
                    HE_GDB_IO gdb = new HE_GDB_IO(fileSurfaceWaterlevels);

                    velocityLoader = gdb;
                    waterlevelLoader = gdb;
                    //System.out.println("GDB is: " + gdb + "  " + gdb.getLayerVelocity() + "  isresult? " + gdb.isResultDB() + " has velocities? " + gdb.hasVelocities());
                    if (gdb.isResultDB()) {
                        if (gdb.hasVelocities()) {
                            if (gdb.isLayerVelocityDirectlyReadable()) {
                                if (gdb.getNumberOfVelocityTimeSteps() < 0) {
                                    //This seems to be a database with unreadable velocity table (too big or decoded)
                                    //We need to extract the data using GDAL and read the CSV instead.
                                    throw new GeoDBException("version 9 or 10 hasvelocities, but number of timesteps is: " + gdb.getNumberOfVelocityTimeSteps());
                                }
                                long start = System.currentTimeMillis();
                                action.description = "Reading GDB surface velocities";
                                fireLoadingActionUpdate();
                                if (sparseSurfaceLoading) {
                                    int numberTriangles = surface.getTriangleMids().length;
                                    int times = gdb.getNumberOfVelocityTimeSteps();
                                    surface.waterlevelLoader = gdb;
                                    surface.initVelocityArrayForSparseLoading(numberTriangles, times);
                                } else {
                                    gdb.applyWaterlevelsToSurface(surface);
                                    surface.waterlevelLoader = gdb;
                                    if (verbose) {
                                        System.out.println("Loading GDB Waterlevels took " + ((System.currentTimeMillis() - start) / 100) + " s.");
                                    }
                                }
                            } else {
                                //File is not directly readable. Requires a transformation to CSV via the GDAL library
                                System.out.println("Problem with GDB detected.");
                                File velocityFile = GdalIO.getCSVFileVelocity(fileSurfaceWaterlevels);
                                System.out.println("  Read surface velocities from CSV " + velocityFile);
                                if (velocityFile == null || velocityFile.length() < 100) {
                                    //Need to create file
                                    boolean installed = GdalIO.testGDALInstallation(true);
                                    if (!installed) {
                                        action.description = "GDAL not installed";
                                    } else {
                                        action.description = "Decode GDB to CSV...";
                                        fireLoadingActionUpdate();
                                        action.hasProgress = false;
                                        velocityFile = GdalIO.exportVelocites(fileSurfaceWaterlevels, surface, false);
                                        action.description = "CSV velocities complete";
                                    }
                                }
                                if (velocityFile != null) {
                                    System.out.println("Surface velocity reading in...");
                                    loadingSurfaceVelocity = LOADINGSTATUS.LOADING;
                                    action.description = "Load velocities from file...";
                                    fireLoadingActionUpdate();
                                    CSV_IO.readCellVelocitiesVariableLength(surface, velocityFile, action);
                                    action.description = "Loading velocities complete";
                                    loadingSurfaceVelocity = LOADINGSTATUS.LOADED;
                                    surfaceVelocitiescompletelyLoaded = true;
                                    fireLoadingActionUpdate();
                                    action.hasProgress = false;
                                } else {
                                    System.out.println("Surface velocity file not found.");
                                }
                                fireLoadingActionUpdate();
                            }

                        } else {
                            if (gdb.hasWaterlevels()) {
                                long start = System.currentTimeMillis();
                                action.description = "Reading GDB surface waterlevels";
                                System.err.println("Surface GDB does only provide waterlevels but no velocities.");
                                if (sparseSurfaceLoading) {
                                    int numberTriangles = surface.getTriangleMids().length;
                                    int times = gdb.getNumberOfWaterlevelTimeSteps();
                                    surface.waterlevelLoader = gdb;
                                    surface.initVelocityArrayForSparseLoading(numberTriangles, times);
                                } else {
                                    gdb.applyWaterlevelsToSurface(surface);
                                    surface.waterlevelLoader = gdb;
                                    if (verbose) {
                                        System.out.println("Loading GDB Waterlevels took " + ((System.currentTimeMillis() - start) / 100) + " s.");
                                    }
                                }
                            } else {
                                loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                                action.description = "GDB has no water levels";
                                System.err.println(action.description);
                            }
                        }
                        if (cancelLoading) {
                            System.out.println("   LoadingThread is interrupted -> break");
                            loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
                            return false;
                        }
                        if (loadGDBVelocity && gdb.hasVelocities() && !sparseSurfaceLoading) {
                            long start = System.currentTimeMillis();
                            action.description = "Reading GDB surface velocities";
                            fireLoadingActionUpdate();
                            gdb.applyVelocitiesToSurface(surface);
                            gdb.close();
                            if (verbose) {
                                System.out.println("Loading GDB Velocities took " + ((System.currentTimeMillis() - start) / 100) + " s.");
                            }
                        }
                    } else {
                        loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                        action.description = "GDB is not a result database.";
                        System.err.println(action.description);
                    }
//                    }
//                    catch (GeoDBException ioe) {
//                        System.out.println("Error while loading GDB velocity:" + ioe.getLocalizedMessage());
//                        if (ioe.getMessage().contains("version 9 or 10")) {
//                            //Malformed GDB file cannot be read wit the rGDB driver.
//                            //Need to fallback to CSV values.
//                            
//                        }
//                    }
                } else {

                    loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                    action.description = "Unknown file format of water-levels-file '" + fileSurfaceWaterlevels + "'.";
                    if (filetype == FILETYPE.HYSTEM_EXTRAN_7 || filetype == FILETYPE.HYSTEM_EXTRAN_8) {
                        System.err.println(action.description);
                    }
                }
                if (cancelLoading) {
                    System.out.println("   LoadingThread is interrupted -> break");
                    loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
                    return false;
                }
                //Calculate times for surface waterlevel/velocity
                // Zeiten festlegen
                starttime = System.currentTimeMillis();

                // Calculate surface velocites from waterlevels.
                if (filetype == FILETYPE.HYSTEM_EXTRAN_8 || filetype == FILETYPE.HYSTEM_EXTRAN_7) {
                    if (!loadGDBVelocity) {
                        surface.calcNeighbourVelocityFromTriangleVelocity();
                    }
                }

                if (sparseSurfaceLoading) {
                    // Sparse loading
                    // Prepare for surfacecomputing2D
                    surface.waterlevelLoader = waterlevelLoader;
                    if (!surfaceVelocitiescompletelyLoaded) {
                        if (isLoadGDBVelocity()) {
                            surface.initSparseTriangleVelocityLoading(velocityLoader, true, false);
                        } else {
                            surface.initSparseTriangleVelocityLoading(null, true, false);
                        }
                    }
                } else {
                }
                if (scenario != null) {
                    long surfaceTimestepMS = 0;
                    if (filetype == FILETYPE.HYSTEM_EXTRAN_7 || filetype == FILETYPE.HYSTEM_EXTRAN_8) {
                        if (resultDatabase != null) {
                            surfaceTimestepMS = (long) (resultDatabase.read2DExportTimeStep() * 60000);
                        }
                        TimeIndexContainer tc;
                        if (surfaceTimestepMS > 0) {
                            tc = createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), surface.getNumberOfTimestamps(), surfaceTimestepMS);
                        } else {
                            tc = createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), surface.getNumberOfTimestamps());
                        }
                        surface.setTimeContainer(tc);
                        scenario.setStatusTimesSurface(surface);
                    } else if (filetype == FILETYPE.COUD_CSV) {
                        TimeIndexContainer tic = (TimeIndexContainer) surface.getTimes();
                        scenario.setStatusTimesPipe(null);
                        scenario.setTimesManhole(null);
                        scenario.setStatusTimesSurface(tic);
                        scenario.setMeasurementsSurface(new SurfaceMeasurementTriangleRaster(this.surface, 1, tic, control.getThreadController().getNumberOfParallelThreads()));
                        scenario.setStatusTimesSurface(tic);
                        surface.setTimeContainer(tic);
                    } else {
                        if (surface.getNumberOfTimestamps() < 2) {
                            surface.setTimeContainer(createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), 2));
                            scenario.setStatusTimesSurface(surface);

                        } else {
                            TimeIndexContainer tc;
                            if (surfaceTimestepMS > 0) {
                                tc = createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), surface.getNumberOfTimestamps(), surfaceTimestepMS);
                            } else {
                                tc = createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), surface.getNumberOfTimestamps());
                            }
                            surface.setTimeContainer(tc);
                            scenario.setStatusTimesSurface(surface);
                        }
                    }
                } else {
                    System.err.println("No Scenario loaded, can not calculate timeintervalls for surface waterheight and velocities.");
                }

//                //Check if timeindexcontainer is initialized
//                System.out.println(LoadingCoordinator.class + "::loadSurface    Surface=" + surface + "\tScenario=" + scenario);
//                System.out.println(LoadingCoordinator.class + "::loadSurface    scenario.timecontainer=" + scenario.getStatusTimesSurface());
//                System.out.println(LoadingCoordinator.class + "::loadSurface    Surface.timecontainer=" + surface.getTimes());
//                System.out.println(LoadingCoordinator.class + "::loadSurface    Surface.measurementraster=" + surface.getMeasurementRaster());
                loadingSurfaceVelocity = LOADINGSTATUS.LOADED;
                action.description = "Surface velocity loaded";
                action.progress = 1f;
                return true;
            } else {
                loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                action.description = "Surface is null when applying velocities";
                System.err.println(action.description);
                new Exception("Surface is null when applying velocities").printStackTrace();
            }
        } catch (Exception ex) {
            loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
            action.description = ex.getLocalizedMessage();
            System.err.println(action.description);
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean mapManholes(Surface surface, Network network) {
        if (manhRefs == null) {
            if (fileSurfaceManholes != null) {
                try {
                    action.description = "Load Manhole locations";
                    fireLoadingActionUpdate();
                    manhRefs = HE_SurfaceIO.loadManholeToTriangleReferences(fileSurfaceManholes);
                    if (verbose) {
                        System.out.println("loaded " + manhRefs.size() + " manhole references");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.err.println("Cannot apply manholes. File not set. " + fileSurfaceManholes);
            }
        } else {
            if (verbose) {
                System.out.println("manhole references already loaded: " + manhRefs.size());
            }
        }

        if (manhRefs != null) {
            action.description = "Mapping manhole - surface links";
            fireLoadingActionUpdate();
            long startt = System.currentTimeMillis();
            surface.applyManholeRefs(network, manhRefs);
            if (verbose) {
                System.out.println("Mapping Manholes to surface took " + ((System.currentTimeMillis() - startt)) + "ms");
            }
            return true;
        } else {
            System.err.println("Manhole references not loaded.");
        }
        return false;
    }

    public boolean mapStreetInlets(Surface sf, Network nw) throws TransformException {
        if ((inletRefs == null || changedPipeNetwork || changedSurface) && fileSurfaceInlets != null) {
            action.description = "Load Inlet locations";
            fireLoadingActionUpdate();
            try {
                inletRefs = HE_SurfaceIO.loadStreetInletsReferences(fileSurfaceInlets);
            } catch (IOException ex) {
                Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (inletRefs != null) {
            action.description = "Mapping Inlet - Pipe links";
            fireLoadingActionUpdate();
            long startt = System.currentTimeMillis();
            sf.applyStreetInlets(nw, inletRefs);
            if (verbose) {
                System.out.println("Mapping Inlets to surface took " + ((System.currentTimeMillis() - startt)) + "ms");
            }
            return true;
        }
        return false;
    }

    public void cancelLoading() {
        System.out.println("Request cancel loadingthread. (" + threadLoadingRequests + ")");
        isLoading = false;

        fireLoadingActionUpdate();

        if (threadLoadingRequests != null && threadLoadingRequests.isAlive()) {
            System.out.println("send interrupt signal");
            threadLoadingRequests.interrupt();
        }
        if (list_loadingPipeResults != null) {
            list_loadingPipeResults.clear();
        }
        threadLoadingRequests = null;
        if (loadingPipeResult == LOADINGSTATUS.LOADING) {
            loadingPipeResult = LOADINGSTATUS.REQUESTED;
        }
        if (loadingStreetInlets == LOADINGSTATUS.LOADING) {
            loadingStreetInlets = LOADINGSTATUS.REQUESTED;
        }
        if (loadingSurface == LOADINGSTATUS.LOADING) {
            loadingSurface = LOADINGSTATUS.REQUESTED;
        }
        if (loadingSurfaceVelocity == LOADINGSTATUS.LOADING) {
            loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
        }
        if (loadingpipeNetwork == LOADINGSTATUS.LOADING) {
            loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
        }

        isLoading = false;
        fireLoadingActionUpdate();
    }

    public boolean isLoadGDBVelocity() {
        return loadGDBVelocity;
    }

    public void setUseGDBVelocity(boolean loadGDBVelocity) {
        if (loadGDBVelocity == this.loadGDBVelocity) {
            return;
        }
        try {
            this.loadGDBVelocity = loadGDBVelocity;
            if (surface == null) {
                return;
            }
            if (this.loadGDBVelocity) {
                if (surface.getTriangleVelocity() == null || surface.getTriangleVelocity().length == 0) {
                    this.loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
                    fireLoadingActionUpdate();
                } else {
                    //surface has already loaded triangle velocity
                    // calculate neighbour velocity from triangle velocity

                    //surface.calcNeighbourVelocityFromTriangleVelocity();
                }
            } else {
                //requested laoding of neighbour velocities from Waterlevels
                surface.calculateNeighbourVelocitiesFromWaterlevels();
            }
        } catch (Exception e) {
            loadingSurface = LOADINGSTATUS.ERROR;
            fireLoadingActionUpdate();
            e.printStackTrace();
        }
    }

    public void startReloadingAll(boolean asThread) {
        if (this.loadingPipeResult != LOADINGSTATUS.NOT_REQUESTED) {
            loadingPipeResult = LOADINGSTATUS.REQUESTED;
        }
        if (this.loadingStreetInlets != LOADINGSTATUS.NOT_REQUESTED) {
            loadingStreetInlets = LOADINGSTATUS.REQUESTED;
        }
        if (this.loadingSurface != LOADINGSTATUS.NOT_REQUESTED) {
            loadingSurface = LOADINGSTATUS.REQUESTED;
        }
        if (this.loadingSurfaceVelocity != LOADINGSTATUS.NOT_REQUESTED) {
            loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
        }
        if (this.loadingpipeNetwork != LOADINGSTATUS.NOT_REQUESTED) {
            loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
        }
        this.scenario = null;
        this.surface = null;
        this.network = null;
//        this.totalInjections.clear();
        this.startLoadingRequestedFiles(asThread);
    }

    public boolean addManualInjection(InjectionInfo inj) {
        if (manualInjections.contains(inj)) {
            if (verbose) {
                System.out.println("Loadingcoordinator already contains injection @" + inj.getPosition());
            }
            manualInjections.remove(inj);
            manualInjections.add(inj);
//            if (control.getScenario() != null) {
//                control.getScenario().getInjections().remove(inj);
//                control.getScenario().getInjections().add(inj);
//            }
//            return false;
        }
        if (totalInjections.contains(inj)) {
            if (verbose) {
                System.out.println("Loadingcoordinator already contains injection @" + inj.getPosition());
            }
            totalInjections.remove(inj);
            totalInjections.add(inj);

            return false;
        } else {
            totalInjections.add(inj);
        }
        if (control.getScenario() != null) {
            control.getScenario().getInjections().remove(inj);
            control.getScenario().getInjections().add(inj);
        }
        return manualInjections.add(inj);
    }

    public void removeInjection(InjectionInfo info) {
        manualInjections.remove(info);
        totalInjections.remove(info);
        if (scenario != null && scenario.getInjections() != null) {
            scenario.getInjections().remove(info);
        }
    }

    public ArrayList<InjectionInfo> getInjections() {
        return totalInjections;
    }

    public void clearManualInjections() {
        this.manualInjections.clear();
    }

    public void clearInjections() {
        this.totalInjections.clear();
    }

    public void setPipeNetworkFile(File networkFile) {
        this.fileNetwork = networkFile;
        if (networkFile != null) {
            this.loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
        } else {
            this.loadingpipeNetwork = LOADINGSTATUS.NOT_REQUESTED;
        }
    }

    public void setPipeResultsFile(File pipeResultFile, boolean clearOtherFiles) {
        this.fileMainPipeResult = pipeResultFile;
        this.loadingPipeResult = LOADINGSTATUS.REQUESTED;
        if (pipeResultFile != null) {
            this.clearOtherResults = clearOtherFiles;
//            this.loadOnlyMainFile = true;
        } else {
            loadingPipeResult = LOADINGSTATUS.NOT_REQUESTED;
        }
    }

    public void addPipeResultsFile(File pipeResultFile, boolean asMainFile) {

        if (pipeResultFile != null) {
            list_loadingPipeResults.add(new Pair<>(pipeResultFile, asMainFile));
            this.clearOtherResults = false;
            this.loadingPipeResult = LOADINGSTATUS.REQUESTED;
//            this.loadOnlyMainFile = false;
        }
    }

    public void setSurfaceTopologyFile(File surfaceTopologyFile) throws FileNotFoundException {
        if (surfaceTopologyFile == null) {
            this.loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
            this.fileSurfaceCoordsDAT = null;
            this.fileSurfaceTriangleIndicesDAT = null;
            this.FileTriangleNeumannNeighboursDAT = null;
            this.fileSurfaceReferenceSystem = null;
            this.fileSurfaceInlets = null;
            this.fileSurfaceManholes = null;
//            this.fileSufaceNode2Triangle = null;
//            this.fileTriangleMooreNeighbours = null;
            return;
        }
        if (surfaceTopologyFile.isDirectory()) {
            setSurfaceTopologyDirectory(surfaceTopologyFile);
            return;
        }
        if (surfaceTopologyFile.getName().endsWith(".dat")) {
            setSurfaceTopologyDirectory(surfaceTopologyFile.getParentFile());
            return;
        }
        if (surfaceTopologyFile.getName().endsWith(".csv")) {
            this.fileSurfaceTriangleIndicesDAT = null;
            this.FileTriangleNeumannNeighboursDAT = null;
            this.fileSurfaceReferenceSystem = null;
            this.fileSurfaceInlets = null;
            this.fileSurfaceManholes = null;
            this.fileSurfaceCoordsDAT = surfaceTopologyFile;
            this.fileSurfaceTriangleIndicesDAT = surfaceTopologyFile;
            this.loadingSurface = LOADINGSTATUS.REQUESTED;
            return;
        }
    }

    public void setSurfaceTopologyDirectory(File surfaceTopologyDirectory) throws FileNotFoundException {
        if (surfaceTopologyDirectory == null) {
            this.loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
            this.fileSurfaceCoordsDAT = null;
            this.fileSurfaceTriangleIndicesDAT = null;
            this.FileTriangleNeumannNeighboursDAT = null;
            this.fileSurfaceReferenceSystem = null;
            this.fileSurfaceInlets = null;
            this.fileSurfaceManholes = null;
//            this.fileSufaceNode2Triangle = null;
//            this.fileTriangleMooreNeighbours = null;
            return;
        }

        if (!surfaceTopologyDirectory.isDirectory()) {
            File olddir = surfaceTopologyDirectory;
            surfaceTopologyDirectory = surfaceTopologyDirectory.getParentFile();
        }
        File fileCoordinates = new File(surfaceTopologyDirectory, "X.dat");
        if (!fileCoordinates.exists()) {
            throw new FileNotFoundException("File for Coordinates could not be found: " + fileCoordinates.getAbsolutePath());
        }
        File fileTriangle = new File(surfaceTopologyDirectory, "TRIMOD2.dat");
        if (!fileTriangle.exists()) {
            throw new FileNotFoundException("File for Triangleindizes could not be found: " + fileTriangle.getAbsolutePath());
        }
        File fileNeighbours = new File(surfaceTopologyDirectory, "TRIMOD1.dat");
        if (!fileNeighbours.exists()) {
            throw new FileNotFoundException("File for Neighbours could not be found: " + fileNeighbours.getAbsolutePath());
        }

        // Files with information about the coordinate reference system.
        File fileCoordinateReference = new File(surfaceTopologyDirectory, "polyg.xml");
        if (!fileCoordinateReference.exists()) {
            fileCoordinateReference = new File(surfaceTopologyDirectory, "city.xml");
            if (!fileNeighbours.exists()) {
                throw new FileNotFoundException("File for CoordinateReference could not be found: " + fileNeighbours.getAbsolutePath());
            }
        }

        // Files for calculating velocities on 2D surface ******
//        File fileNode2Triangle = new File(surfaceTopologyDirectory, "NODE2TRIANGLE.dat");
//        if (!fileNode2Triangle.exists()) {
//            System.err.println("File for Nodes' triangles could not be found: " + fileNode2Triangle.getAbsolutePath());
//            this.fileSufaceNode2Triangle = null;
//        } else {
//            this.fileSufaceNode2Triangle = fileNode2Triangle;
//        }
//        File mooreFile = new File(surfaceTopologyDirectory, "MOORE.dat");
//        if (!mooreFile.exists()) {
//            if (verbose) {
//                System.err.println("File for triangles' neumann neighbours could not be found: " + mooreFile.getAbsolutePath());
//            }
//            this.fileTriangleMooreNeighbours = null;
//        } else {
//            this.fileTriangleMooreNeighbours = mooreFile;
//        }
        //Files for merging Surface and Pipenetwork out-/inlets ******
        File fileStreetInlets = new File(surfaceTopologyDirectory, "SURF-SEWER_NODES.dat");
        if (!fileStreetInlets.exists()) {
            if (verbose) {
                System.err.println("File for Streetinlets could not be found: " + fileStreetInlets.getAbsolutePath());
            }
            this.fileSurfaceInlets = null;
        } else {
            this.fileSurfaceInlets = fileStreetInlets;
        }

        File fileManhole2Surface = new File(surfaceTopologyDirectory, "SEWER-SURF_NODES.dat");
        if (!fileManhole2Surface.exists()) {
            System.err.println("File for Manhole position could not be found: " + fileManhole2Surface.getAbsolutePath());
            this.fileSurfaceManholes = null;
        } else {
            this.fileSurfaceManholes = fileManhole2Surface;
        }

        this.fileSurfaceCoordsDAT = fileCoordinates;
        this.fileSurfaceTriangleIndicesDAT = fileTriangle;
        this.FileTriangleNeumannNeighboursDAT = fileNeighbours;
        this.fileSurfaceReferenceSystem = fileCoordinateReference;
        this.loadingSurface = LOADINGSTATUS.REQUESTED;
    }

    public File getSurfaceTopologyDirectory() {
        if (fileSurfaceCoordsDAT == null) {
            return null;
        }
        return fileSurfaceCoordsDAT.getParentFile();
    }

    public void setSurfaceFlowfieldFile(File wlFile) {
        if (wlFile == null) {
            this.loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;
            this.fileSurfaceWaterlevels = null;
            return;
        }
        this.fileSurfaceWaterlevels = wlFile;
        this.loadingSurfaceVelocity = LOADINGSTATUS.REQUESTED;
    }

    public boolean isLoading() {
        return isLoading || loadingpipeNetwork == LOADINGSTATUS.LOADING || loadingPipeResult == LOADINGSTATUS.LOADING || loadingSurface == LOADINGSTATUS.LOADING || loadingSurfaceVelocity == LOADINGSTATUS.LOADING;
    }

    public boolean isLoadingNetwork() {
        return loadingpipeNetwork == LOADINGSTATUS.LOADING || loadingPipeResult == LOADINGSTATUS.LOADING;
    }

    public boolean isLoadingSurface() {
        return loadingSurface == LOADINGSTATUS.LOADING;
    }

    public boolean isLoadingWaterlevels() {
        return loadingSurfaceVelocity == LOADINGSTATUS.LOADING;
    }

    public File getFileSurfaceTriangleIndicesDAT() {
        return fileSurfaceTriangleIndicesDAT;
    }

    public File getFileStreetInletsSHP() {
        return fileStreetInletsSHP;
    }

    public File getFileSurfaceWaterlevels() {
        return fileSurfaceWaterlevels;
    }

    public File getFileNetwork() {
        return fileNetwork;
    }

    public File getFilePipeFlowfield() {
        return fileMainPipeResult;
    }

    public void setFileStreetInletsSHP(File fileStreetInletsSHP) {
        this.fileStreetInletsSHP = fileStreetInletsSHP;
        loadingStreetInlets = LOADINGSTATUS.REQUESTED;
    }

    public FileContainer findDependentFiles(File pipeResult, boolean requestSurface) throws IOException, SQLException {

        boolean oldFilecorrespondsPipeNetwork = false, oldFilecorrespondsSurfaceTopology = false, oldFilecorrespondsSurfaceWaterlevel = false;
        File bestFilePipeNetwork = null, bestFileSurfacdirectory = null, bestFileSurfaceHydraulics = null, bestPipeResultFile = pipeResult;

        if (pipeResult.isDirectory()) {
            bestPipeResultFile = null;
            for (File listFile : pipeResult.listFiles()) {
                if (listFile.isFile()) {
                    String name = listFile.getName().toLowerCase();
                    if (name.endsWith("idbr")) {
                        //HE 8+ result
                        bestPipeResultFile = listFile;
                        break;
                    } else if (name.endsWith("idbf")) {
                        //HE 7 result
                        bestPipeResultFile = listFile;
                        break;
                    } else if (name.endsWith("rpt")) {
                        //SWMM result
                        bestPipeResultFile = listFile;
                        break;
                    }
                }
            }
        }
        if (bestPipeResultFile == null) {
            throw new FileNotFoundException(pipeResult + " is not a File with hydraulic results of the pipesystem.");
        }

        try {
            bestFilePipeNetwork = LoadingCoordinator.findCorrespondingPipeNetworkFile(bestPipeResultFile);
            if (verbose) {
                System.out.println("found best fit to " + pipeResult.getName() + "  to be " + bestFilePipeNetwork);
            }
            if (bestFilePipeNetwork != null && (bestFilePipeNetwork.getName().endsWith("idbf") || bestFilePipeNetwork.getName().endsWith("idbr") || bestFilePipeNetwork.getName().endsWith("idbm"))) {
                if (bestFilePipeNetwork != null && bestFilePipeNetwork.exists()) {
                    if (control.getNetwork() != null && control.getNetwork().getPipes() != null) {
                        if (control.getNetwork().getPipes().size() == HE_Database.readNumberOfPipes(bestFilePipeNetwork)) {
                            oldFilecorrespondsPipeNetwork = true;
                        }
                    }
                } else {
                    bestFilePipeNetwork = null;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        //Find corresponding surface topology file
        if (requestSurface) {
            try {
                bestFileSurfacdirectory = LoadingCoordinator.findCorrespondingSurfaceDirectory(bestPipeResultFile);
                if (verbose) {
                    System.out.println("Surface directory: " + bestFileSurfacdirectory);
                }
                if (bestFileSurfacdirectory != null) {
                    //Test if actual file is already loaded
                    if (control.getSurface() != null) {
                        int oldnumber = control.getSurface().size();
                        File trimod2File = new File(bestFileSurfacdirectory.getAbsolutePath() + File.separator + "trimod2.dat");
                        int newnumber = HE_SurfaceIO.readNumberOfTriangles(trimod2File);
                        if (oldnumber == newnumber) {
                            //Seems to be identical to the already loaded surface.
                            oldFilecorrespondsSurfaceTopology = true;
                            if (verbose) {
                                System.out.println("Number of Triangles identical, no need to load again.");
                            }
                            //Do not need to be loaded again
                        } else {
                            if (verbose) {
                                System.out.println("Surface: loaded: " + oldnumber + " triangles, new:" + newnumber);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            try {
                bestFileSurfaceHydraulics = LoadingCoordinator.findCorrespondingWaterlevelFile(bestPipeResultFile);
                if (verbose) {
                    System.out.println("Surface hydraulics: " + bestFileSurfaceHydraulics);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        FileContainer fc = new FileContainer(bestPipeResultFile, bestFilePipeNetwork, bestFileSurfaceHydraulics, bestFileSurfacdirectory, null);
        fc.setPipeNetworkLoaded(oldFilecorrespondsPipeNetwork);
        fc.setPipeResultLoaded(false);
        fc.setSurfaceResultLoaded(oldFilecorrespondsSurfaceWaterlevel);
        fc.setSurfaceTopologyLoaded(oldFilecorrespondsSurfaceTopology);
        return fc;
    }

    public void requestDependentFiles(File pipeResult, boolean requestSurface, boolean clearOtherResults) throws SQLException, IOException {
        this.setPipeResultsFile(pipeResult, clearOtherResults);
        if (pipeResult == null) {
            System.out.println("Result File to request dependend files is null.");
            return;
        }
        File nwf = findCorrespondingPipeNetworkFile(pipeResult);
        if (nwf != null) {
            this.setPipeNetworkFile(nwf);
        }

        if (requestSurface) {
            File sf = findCorrespondingSurfaceDirectory(pipeResult);
            if (sf != null && sf.exists()) {
                setSurfaceTopologyDirectory(sf);
            }
            File wlf = findCorrespondingWaterlevelFile(pipeResult);
            setSurfaceFlowfieldFile(wlf);

        }
    }

    private void fireLoadingActionUpdate() {
        action.updateProgress();
        for (LoadingActionListener ll : listener) {
            ll.actionFired(action, this);
        }
    }

    public LOADINGSTATUS getLoadingStreetInlets() {
        return loadingStreetInlets;
    }

    public LOADINGSTATUS getLoadingSurface() {
        return loadingSurface;
    }

    public LOADINGSTATUS getLoadingSurfaceVelocity() {
        return loadingSurfaceVelocity;
    }

    public LOADINGSTATUS getLoadingpipeNetwork() {
        return loadingpipeNetwork;
    }

    public LOADINGSTATUS getLoadingPipeResult() {
        return loadingPipeResult;
    }

    public static File findCorrespondingPipeNetworkFile(File resultFile) throws SQLException, IOException {
        if (resultFile.getName().endsWith("idbf")) {
            //HYSTEM EXTRAN Result file 
            if (tempFBDB == null || !tempFBDB.getDatabaseFile().equals(resultFile)) {
                tempFBDB = new HE_Database(resultFile, true);
            }
            String filePath = resultFile.getParentFile().getParent() + File.separator + tempFBDB.readModelnamePipeNetwork();//HE_Database.readModelnamePipeNetwork(resultFile);
            if (verbose) {
                System.out.println("Network corresponding file: " + filePath);
            }
            File f = new File(filePath);
            if (f.exists()) {

                return f;
            }
            f = new File(filePath.replace(".idbf", ".idbm")); //Maybe it is now converted to HE version 8+ which has another postfix
            if (f.exists()) {
                System.out.println("   does exist as new HE file with ending *.idbm");
                return f;
            }
            System.out.println("Network file " + filePath + "   does not exist. use result file to load network");
            return resultFile; //information about the pipe network can also be found inside the result.
        } else if (resultFile.getName().endsWith("idbr")) {
            //SQLite Result file
            if (tempFBDB == null || !tempFBDB.getDatabaseFile().equals(resultFile)) {
                tempFBDB = new HE_Database(resultFile, true);
            }
            try {
                String modelfilename = tempFBDB.readModelnamePipeNetwork();
                File candidateFile = new File(resultFile.getParentFile(), modelfilename);
                if (candidateFile.exists()) {
                    if (verbose) {
                        System.out.println("Network corresponding file: " + candidateFile);
                    }
                    return candidateFile;
                }
                //One directory higher
                candidateFile = new File(resultFile.getParentFile().getParentFile(), modelfilename);
                if (candidateFile.exists()) {
                    if (verbose) {
                        System.out.println("Network corresponding file: " + candidateFile);
                    }
                    return candidateFile;
                }
                //One directory higher
                candidateFile = new File(resultFile.getParentFile().getParentFile().getParent(), modelfilename);
                if (candidateFile.exists()) {
                    if (verbose) {
                        System.out.println("Network corresponding file: " + candidateFile);
                    }
                    return candidateFile;
                }
            } catch (Exception exception) {
            }

            return resultFile; //information about the pipe network can also be found inside the result. Use this as a backup solution
        } else if (resultFile.getName().endsWith("rpt")) {
            // SWMM Report file
            String newfile = resultFile.getAbsolutePath();
            newfile = newfile.replaceAll("results_", "inputfile_");
            newfile = newfile.substring(0, newfile.length() - 3) + "inp";

            File file = new File(newfile);
            if (file.exists()) {
                if (verbose) {
                    System.out.println("Corresponding SWMM network '" + file.getAbsolutePath() + "'");
                }
                return file;
            }
            System.out.println("Corresponding SWMM network not found @" + file.getAbsolutePath());
        } else if (resultFile.getName().endsWith("out")) {
            try {
                // SWMM output file
                File f = new File(resultFile.getParentFile(), resultFile.getName().replace(".out", ".inp"));

                if (f.exists()) {
                    if (verbose) {
                        System.out.println("Corresponding SWMM network '" + f.getAbsolutePath() + "'");
                    }
                    return f;
                }
                //test in parent directory
                f = new File(resultFile.getParentFile().getParentFile(), f.getName());
                if (f.exists()) {
                    return f;
                }
                //Search for the first *.inp file to find
                for (File fi : resultFile.getParentFile().listFiles()) {
                    if (fi.isFile() && fi.getName().toLowerCase().endsWith(".inp")) {
                        return fi;
                    }
                }

                System.out.println("Corresponding SWMM network not found @" + f.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Tests if two network objects have identical name, number of manholes and
     * pipes. No deep check is performed.
     *
     * @param nw1
     * @param nw2
     * @return true if two networks share same attributes.
     */
    public static boolean isNetworkEquals(Network nw1, Network nw2) {
        if (nw1 == null) {
            return false;
        }
        if (nw2 == null) {
            return false;
        }
        if (nw1 == nw2) {
            return true;
        }

        if (!nw1.getName().equals(nw2.getName())) {
            return false;
        }
        if (nw1.getManholes().size() != nw2.getManholes().size()) {
            return false;
        }
        if (nw1.getPipes().size() != nw2.getPipes().size()) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param resultFile
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static File findCorrespondingSurfaceDirectory(File resultFile) throws SQLException, IOException {
        if (resultFile.getName().endsWith("rpt")) {
            return null;
        }
        if (resultFile.getName().endsWith("out")) {
            return null;
        }
        if (tempFBDB == null || !resultFile.equals(tempFBDB.getDatabaseFile())) {
            tempFBDB = new HE_Database(resultFile, true);
        }
        String surfModelName = tempFBDB.readSurfaceModelname();
        if (surfModelName != null) {
            String surfaceFiles = resultFile.getParentFile().getParent() + File.separator + surfModelName;//HE_Database.readSurfaceModelname(resultFile) + ".model";
            File directorySurfaceFiles = new File(surfaceFiles);
            if (!directorySurfaceFiles.exists()) {
                surfaceFiles = resultFile.getParentFile().getParent() + File.separator + surfModelName + ".model";
                directorySurfaceFiles = new File(surfaceFiles);
            }
            if (directorySurfaceFiles.exists()) {
                //Find trimod.dat file containing information about triangle indices
                File trimod2File = new File(directorySurfaceFiles.getAbsolutePath() + File.separator + "trimod2.dat");
                if (trimod2File.exists()) {
                    return directorySurfaceFiles;
                }
            } else {
                System.err.println("No surface model found at " + directorySurfaceFiles.getAbsolutePath());
            }
        } else {
            //No surface model set. seems to be a non-HE2D simulation.
        }
        return null;
    }

    public static Network readNetwork(File f) throws FileNotFoundException, IOException, Exception {
        if (f == null) {
            throw new NullPointerException();
        }
        if (!f.exists()) {
            throw new FileNotFoundException("File '" + f.getAbsolutePath() + "' not found.");
        }
        if (f.getName().toLowerCase().endsWith(".inp")) {
            return SWMM_IO.readNetwork(f);
        }
        if (f.getName().toLowerCase().endsWith(".idbf") || f.getName().toLowerCase().endsWith(".idbm") || f.getName().toLowerCase().endsWith(".idbr")) {
            if (tempFBDB == null || !tempFBDB.getDatabaseFile().equals(f)) {
                tempFBDB = new HE_Database(f, true);
            }
            return tempFBDB.loadNetwork();//HE_Database.loadNetwork(f);
        }
        if (f.getName().toLowerCase().endsWith(".txt")) {
            return CSV_IO.loadNetwork(f);
        }
        if (f.isDirectory()) {
            return CSV_IO.loadNetwork(f);
        }
        throw new Exception("Fileformat '" + f.getName() + "' not known.");
    }

    public static File findCorrespondingWaterlevelFile(File idbfFile) throws FileNotFoundException {
        File resultGDB = new File(idbfFile.getParent() + File.separator + "Result2D.gdb");
        if (resultGDB.exists()) {
            if (HE_GDB_IO.isReadyInstalled()) {
                return resultGDB;
            }
        } else {
            System.err.println("Can not find Surface dynamics at " + resultGDB.getAbsolutePath());
        }
        return null;
    }

    public static TimeIndexContainer createTimeContainer(long start, long end, int numbertimesteps) {
        long dt = (end - start) / (numbertimesteps - 1);
        long[] times = new long[numbertimesteps];
        for (int i = 0; i < times.length; i++) {
            times[i] = start + i * dt;
        }
        TimeIndexContainer tc = new TimeIndexContainer(times);
        return tc;
    }

    public static TimeIndexContainer createTimeContainer(long start, long end, int numbertimesteps, long regularInterval) {
        long dt = regularInterval;//(end - start) / (numbertimesteps - 1);
        long[] times = new long[numbertimesteps];
        for (int i = 0; i < times.length; i++) {
            times[i] = start + i * dt;
        }
        if (times[times.length - 1] < end) {
            times[times.length - 1] = end;
        }
        TimeIndexContainer tc = new TimeIndexContainer(times);
        return tc;
    }

    public static Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> sparseLoadTimelines(Network network, SparseTimeLineDataProvider dataprovider, Collection<Pipe> pipes, Collection<StorageVolume> manholes) throws Exception {
        return sparseLoadTimelines(network, dataprovider, pipes, manholes, false, 0);
    }

    public static Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> sparseLoadTimelines(Network network, SparseTimeLineDataProvider dataprovider, Collection<Pipe> pipes, Collection<StorageVolume> manholes, boolean zeroTimeStart, long additionalMilliseconds) throws Exception {
        long[] tp = dataprovider.loadTimeStepsNetwork(zeroTimeStart);
        tp[tp.length - 1] += additionalMilliseconds;
        SparseTimeLinePipeContainer container = new SparseTimeLinePipeContainer(tp);
        container.setDataprovider(dataprovider);
        //clear all old timelines
        for (Pipe pipe : network.getPipes()) {
            pipe.setStatusTimeLine(null);
        }
        dataprovider.loadTimelinePipes(pipes, container);
        //Set the timeline for all other pipes
        for (Pipe pipe : network.getPipes()) {
            if (pipe.getStatusTimeLine() == null) {
                pipe.setStatusTimeLine(new SparseTimelinePipe(container, pipe));
            }
        }
        tp = dataprovider.loadTimeStepsNetwork(zeroTimeStart);
        tp[tp.length - 1] += additionalMilliseconds;
        SparseTimeLineManholeContainer mcontainer = new SparseTimeLineManholeContainer(tp);
        mcontainer.setDataprovider(dataprovider);
        //clear all old timelines
        for (Manhole manhole : network.getManholes()) {
            manhole.setStatusTimeline(null);
        }
        dataprovider.loadTimelineManholes(manholes, mcontainer);
        //Set the timeline for all other pipes
        for (Manhole m : network.getManholes()) {
            if (m.getStatusTimeLine() == null) {
                m.setStatusTimeline(new SparseTimelineManhole(mcontainer, m));
            }
        }
        return new Pair<>(container, mcontainer);
    }

    /**
     * Apply ArrayTimelines to Network elements. Arraytimelines are more compact
     * and fast than Sparsetimelines.
     *
     * @throws java.io.IOException
     * @throws java.text.ParseException
     * @throws java.sql.SQLException
     */
    public void loadTimelinesOfAllElements() throws IOException, ParseException, SQLException {
        Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> p;

        if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {
            p = resultDatabase.applyTimelines(control.getNetwork());//HE_Database.readTimelines(fileMainPipeResult, control.getNetwork());

        } else if (fileMainPipeResult.getName().endsWith(".rpt")) {
            p = SWMM_IO.readTimeLines(fileMainPipeResult, control.getNetwork());
        } else {
            throw new IllegalArgumentException("Not known filetype '" + fileMainPipeResult.getName() + "'");
        }
        control.getScenario().setTimesManhole(p.second);
        control.getScenario().setStatusTimesPipe(p.first);
    }

    public void stopLoadingSurfaceGrid() {
        this.loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
    }

    public void stopLoadingSurfaceVelocity() {
        this.loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;
    }

    public boolean loadSetup(File file) {
        try {
            Setup setup = Setup_IO.load(file);
            if (setup != null) {
                this.applySetup(setup);
                if (scenario != null && scenario.getName() == null) {
                    scenario.setName(file.getName());
                }
                for (LoadingActionListener lal : listener) {
                    lal.actionFired(new Action("Setup load", null, false), this);
                }
                setupFile = file.getAbsolutePath();
                return true;
            }
        } catch (Exception ex) {
            System.err.println("Problem loading Project setup file " + file);
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Prepare everything for loading a setup with information about files and
     * manualInjections.
     *
     * @param setup
     * @throws java.sql.SQLException
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void applySetup(Setup setup) throws SQLException, FileNotFoundException, IOException {

        this.setFilesToLoad(setup.getFiles());
        this.loadResultInjections = setup.isLoadResultInjections();

        this.manualInjections.clear();
        if (setup.injections != null && setup.injections.size() > 0) {
            for (InjectionInfo in : setup.injections) {
                addManualInjection(in);
            }
        }
        if (this.scenario == null) {
            if (setup.scenario != null) {
                this.scenario = setup.scenario;
            }
        }

        if (surface != null) {
            if (surface.getMeasurementRaster() != null) {
                SurfaceMeasurementRaster sr = surface.getMeasurementRaster();
                sr.spatialConsistency = setup.isSurfaceMeasurementSpatialConsistent();
                sr.continousMeasurements = setup.isSurfaceMeasurementTimeContinuous();
            }
        }
        SurfaceMeasurementRaster.synchronizeMeasures = setup.isSurfaceMeasurementSynchronize();
        SurfaceMeasurementRaster.continousMeasurements = setup.isSurfaceMeasurementTimeContinuous();
        SurfaceMeasurementRaster.spatialConsistency = setup.isSurfaceMeasurementSpatialConsistent();

        ParticlePipeComputing.measureOnlyFinalCapacity = !setup.isPipeMeasurementSpatialConsistent();
        MeasurementContainer.synchronizeMeasures = setup.isPipeMeasurementSynchronize();
        MeasurementContainer.timecontinuousMeasures = setup.isPipeMeasurementTimeContinuous();

        try {
            if (scenario != null && scenario.getMeasurementsPipe() != null) {
                if (scenario.getMeasurementsPipe() instanceof ArrayTimeLineMeasurementContainer) {
                    ((ArrayTimeLineMeasurementContainer) scenario.getMeasurementsPipe()).setIntervalSeconds(setup.getPipeMeasurementtimestep(), scenario.getStartTime(), scenario.getEndTime());
                } else {
                    System.out.println("LoadingCoordinator cannot set a new Timeinterval width to MeasurmentContainer " + scenario.getMeasurementsPipe());
                }
            }
        } catch (Exception e) {
        }

        if (scenario != null) {
//            scenario.getInjections().addAll(totalInjections);
            if (setup.materials != null) {
                scenario.setMaterials(new ArrayList<Material>(setup.materials));
            }
        }
        control.getThreadController().setDeltaTime(setup.getTimestepTransport());
        ParticleSurfaceComputing2D.timeIntegration = setup.getTimeIntegration();

        if (setup.getIntervalTraceParticles() > 0) {
            control.setTraceParticles(true);
            control.intervallHistoryParticles = setup.getIntervalTraceParticles();
        } else {
            control.setTraceParticles(false);
        }

        control.getLoadingCoordinator().sparsePipeLoading = setup.isSparsePipeVelocity();

        //Simulationparameters
        ParticleSurfaceComputing2D.blockVerySlow = setup.isStopSlow();
        ParticleSurfaceComputing2D.dryFlowVelocity = setup.getDryVelocity();
        ParticleSurfaceComputing2D.gradientFlowForDryCells = setup.isDryMovement();
        ParticleSurfaceComputing2D.meanVelocityAtZigZag = setup.isSmoothZigZag();
        ParticleSurfaceComputing2D.slidealongEdges = setup.isSlideAlongEdge();
        ParticleSurfaceComputing2D.preventEnteringDryCell = !setup.isEnterDryCells();

        fireLoadingActionUpdate();
    }

    /**
     * The name of the current displayed Simulation.
     *
     * @return
     */
    public String getResultName() {
        return resultName;
    }

    public void setFilesToLoad(FileContainer files) {
        if (files.pipeNetwork != null) {
            this.setPipeNetworkFile(files.pipeNetwork);
            changedPipeNetwork = true;
        } else {
            if (this.fileNetwork != null) {
                this.setPipeNetworkFile(null);
                changedPipeNetwork = true;
            }
        }

        if (this.fileMainPipeResult != files.pipeResult) {
            this.setPipeResultsFile(files.pipeResult, true);
        }

        if (files.surfaceGeometry != null) {
            if (this.fileSurfaceCoordsDAT != files.surfaceGeometry) {
                try {
                    this.setSurfaceTopologyFile(files.surfaceGeometry);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (files.surfaceResult != null) {
            this.setSurfaceFlowfieldFile(files.surfaceResult);
        }
        if (files.getCrsPipes() != null && !files.getCrsPipes().isEmpty()) {
            if (this.crsNetwork == null || !crsNetwork.equals(files.getCrsPipes())) {
                crsNetwork = files.getCrsPipes();
                changedPipeNetwork = true;
            }
        } else {
            crsNetwork = null;
        }
        if (files.getCrsSurface() != null && !files.getCrsSurface().isEmpty()) {
            if (this.crsSurface == null || !crsSurface.equals(files.getCrsSurface())) {
                crsSurface = files.getCrsSurface();
                changedSurface = true;
            }
        } else {
            crsSurface = null;
        }
    }

    public boolean saveSetup(File file) throws IOException {
        //Create new setup and store the information
        Setup setup = new Setup();
        setup.files = new FileContainer(fileMainPipeResult, fileNetwork, fileSurfaceWaterlevels, null, null);
        if (fileSurfaceTriangleIndicesDAT != null) {
            setup.files.surfaceGeometry = fileSurfaceCoordsDAT;
        }
        setup.files.setCrsPipes(crsNetwork);
        setup.files.setCrsSurface(crsSurface);

        setup.setLoadResultInjections(loadResultInjections);

        setup.injections = manualInjections;
        setup.scenario = scenario;

        setup.setTimestepTransport(ThreadController.getDeltaTime());

        if (control.isTraceParticles()) {
            setup.setIntervalTraceParticles(control.intervallHistoryParticles);
        } else {
            setup.setIntervalTraceParticles(0);
        }

        if (control.getScenario() != null) {
            MeasurementContainer mp = control.getScenario().getMeasurementsPipe();
            if (mp != null) {
                setup.setPipeMeasurementtimestep(mp.getTimes().getDeltaTimeMS() / 1000.);
                setup.setPipeMeasurementTimeContinuous(MeasurementContainer.timecontinuousMeasures);//!mp.isTimespotmeasurement());
            }
            setup.setPipeMeasurementSpatialConsistent(!ParticlePipeComputing.measureOnlyFinalCapacity);
            setup.setPipeMeasurementSynchronize(MeasurementContainer.synchronizeMeasures);

            SurfaceMeasurementRaster sr = control.getScenario().getMeasurementsSurface();
            if (sr != null) {
                setup.setSurfaceMeasurementtimestep((sr.getIndexContainer().getTimeMilliseconds(1) - sr.getIndexContainer().getTimeMilliseconds(0)) / 1000.);
                setup.setSurfaceMeasurementTimeContinuous(sr.continousMeasurements);
                setup.setSurfaceMeasurementSpatialConsistent(sr.spatialConsistency);
            }
            setup.setSurfaceMeasurementSynchronize(SurfaceMeasurementRaster.synchronizeMeasures);
        }
        setup.setDryMovement(ParticleSurfaceComputing2D.gradientFlowForDryCells);
        setup.setDryVelocity(ParticleSurfaceComputing2D.dryFlowVelocity);
        setup.setEnterDryCells(!ParticleSurfaceComputing2D.preventEnteringDryCell);
        setup.setSlideAlongEdge(ParticleSurfaceComputing2D.slidealongEdges);
        setup.setSmoothZigZag(ParticleSurfaceComputing2D.meanVelocityAtZigZag);
        setup.setStopSlow(ParticleSurfaceComputing2D.blockVerySlow);

        setup.setSparsePipeVelocity(control.getLoadingCoordinator().sparsePipeLoading);

        if (Setup_IO.saveScenario(file, setup)) {
            setupFile = file.getAbsolutePath();
            return true;
        }
        return false;
    }

    /**
     * The Software, that generated the flow field.
     *
     * @return enum FILETYPE
     */
    public FILETYPE getFiletype() {
        return filetype;
    }

    public String getCrsSurface() {
        return crsSurface;
    }

    public String getCrsNetwork() {
        return crsNetwork;
    }

    public String getCurrentSetupFilepath() {
        return setupFile;
    }

    public void setCrsNetwork(String crsNetwork) {
        if (crsNetwork == null && this.crsNetwork == null) {
            //nothing changed
            return;
        }
        if (crsNetwork != null && this.crsNetwork != null) {
            if (crsNetwork.equals(this.crsNetwork)) {
                //nothing changed
                return;
            }
        }
        this.crsNetwork = crsNetwork;
        this.requestLoading = true;
        this.loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
    }

    public void setCrsSurface(String crsS) {
        if (crsS == null && this.crsSurface == null) {
            //nothing changed
            return;
        }
        if (crsS != null && this.crsSurface != null) {
            if (crsS.equals(this.crsSurface)) {
                //nothing changed
                return;
            }
        }
        this.crsSurface = crsS;
        this.requestLoading = true;
        this.loadingSurface = LOADINGSTATUS.REQUESTED;
    }

    public HE_Database requestHE_ResultDatabase() {
        if (resultDatabase != null) {
            return resultDatabase;
        }
        return null;
    }

    public void createNewSetup() {
        this.setupFile = null;
        this.FileTriangleNeumannNeighboursDAT = null;
        this.crsNetwork = null;
        this.crsSurface = null;
        this.fileMainPipeResult = null;
        this.fileNetwork = null;
        this.fileStreetInletsSHP = null;
        this.fileSurfaceCoordsDAT = null;
        this.fileSurfaceInlets = null;
        this.fileSurfaceManholes = null;
        this.fileSurfaceReferenceSystem = null;
        this.fileSurfaceTriangleIndicesDAT = null;
        this.fileSurfaceWaterlevels = null;

        loadingPipeResult = LOADINGSTATUS.NOT_REQUESTED;
        loadingStreetInlets = LOADINGSTATUS.NOT_REQUESTED;
        loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
        loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;
        loadingpipeNetwork = LOADINGSTATUS.NOT_REQUESTED;

        this.inletRefs = null;
        this.manhRefs = null;

        this.isLoading = false;
        this.list_loadingPipeResults = null;

        this.manualInjections.clear();
        if (this.modelDatabase != null) {
            try {
                this.modelDatabase.close();
            } catch (Exception ex) {
                Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.modelDatabase = null;
        this.network = null;
        this.requestLoading = false;
        if (this.resultDatabase != null) {
            try {
                this.resultDatabase.close();
            } catch (Exception ex) {
                Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.resultDatabase = null;
        }
        this.resultName = "";
        if (this.scenario != null) {
            this.scenario.reset();
            this.scenario.setMeasurementsPipe(null);
            this.scenario.setMeasurementsSurface(null);
            this.scenario.setMaterials(null);
        }
        this.scenario = null;
        this.surface = null;
        this.totalInjections.clear();
        System.gc();
    }

}
