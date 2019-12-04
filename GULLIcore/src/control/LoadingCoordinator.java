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
package control;

import control.Action.Action;
import control.listener.LoadingActionListener;
import control.multievents.PipeResultData;
import control.scenario.SpillScenario;
import control.scenario.injection.InjectionInformation;
import control.scenario.Setup;
import control.scenario.injection.HEInjectionInformation;
import control.scenario.injection.HE_MessdatenInjection;
import io.extran.CSV_IO;
import io.extran.HE_Database;
import io.SHP_IO_GULLI;
import io.SparseTimeLineDataProvider;
import io.swmm.SWMM_IO;
import io.extran.HE_SurfaceIO;
import io.extran.HE_GDB_IO;
import io.extran.HE_InletReference;
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
import javax.swing.JOptionPane;
import model.particle.Material;
import model.surface.Surface;
import model.surface.SurfaceVelocityLoader;
import model.surface.SurfaceWaterlevelLoader;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.timeline.sparse.SparseTimeLinePipeContainer;
import model.timeline.sparse.SparseTimelinePipe;
import model.timeline.array.TimeIndexContainer;
import model.timeline.sparse.SparseTimeLineManholeContainer;
import model.timeline.sparse.SparseTimelineManhole;
import model.topology.Capacity;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.StorageVolume;
import model.topology.graph.GraphSearch;
import model.topology.graph.Pair;
import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class LoadingCoordinator implements LoadingActionListener {

    private SpillScenario scenario;
    private boolean changedSurface;

    public enum LOADINGSTATUS {

        NOT_REQUESTED, REQUESTED, LOADING, LOADED, ERROR
    }

    private File fileSurfaceCoordsDAT, fileSurfaceTriangleIndicesDAT, FileTriangleNeumannNeighboursDAT,
            fileSurfaceReferenceSystem, fileSurfaceManholes, fileSurfaceInlets,
            fileSufaceNode2Triangle, fileTriangleMooreNeighbours;
    private File fileSurfaceWaterlevels;
    private LOADINGSTATUS loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
    private LOADINGSTATUS loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;

    private File fileNetwork;

    private HE_Database modelDatabase, resultDatabase;
    private static HE_Database tempFBDB;

    /**
     * enables the console logging output.
     */
    public static boolean verbose = false;

    /**
     * Add Injections from the scenario (e.g. HE Schmutzfrachteinleitung) as
     * Injectionspills of this scenario.
     */
    public boolean loadInputInjections = true;

    private boolean isLoading = false;

    public final Action action = new Action("LoadingCoordinator", null, false);

    /**
     * Load only neccessary timelines for pipes.
     */
    public boolean sparsePipeLoading = true;

    /**
     * Load only neccessary timelines for surface.
     */
    public boolean sparseSurfaceLoading = true;

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
    private boolean loadOnlyMainFile = true;

    private boolean loadGDBVelocity = true;
    private boolean changedPipeNetwork = false;
    private boolean cancelLoading = false;

    /**
     * Holder of actual "in use" objects (network/surface)
     */
    private final Controller control;

    private String resultName = "";

    private final ArrayList<InjectionInformation> injections = new ArrayList<>();

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

    public LoadingCoordinator(Controller control) {
        this.control = control;
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
                        loadPipeVelocities(network);
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
                        surface = loadSurface();
                    }
                    if (isInterrupted()) {
                        System.out.println("   LoadingThread is interrupted -> break");
                        break;
                    }
                    // Connect Surface and pipesystem
                    if (changedSurface || changedPipeNetwork) {
                        if (surface != null && network != null) {
                            try {
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
                    // Start loading Waterlevels and calculate surface triangle velocities 
                    if (loadingSurfaceVelocity == LOADINGSTATUS.REQUESTED) {
                        if (surface != null) {
                            action.progress = 0f;
                            loadSurfaceVelocity(surface);
                        } else {
                            loadingSurfaceVelocity = LOADINGSTATUS.ERROR;

                        }
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

                        control.loadScenario(scenario);
                    }
                    //Inform controller about updated timecontainer.
//                    action.description = "GC clean up";
//                    System.gc();
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
                    exception.printStackTrace();

                }
                if (crsCode == null) {
                    crsCode = "EPSG:25832";
                    System.err.println("No CRS information found. Use " + crsCode + " as CRS for Network geometry.");
                }
                nw = modelDatabase.loadNetwork(CRS.decode(crsCode));//HE_Database.loadNetwork(fileNetwork);
            } else if (fileNetwork.getName().endsWith(".inp")) {
                nw = SWMM_IO.readNetwork(fileNetwork);
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
            loadingpipeNetwork = LOADINGSTATUS.LOADED;
            return nw;
        } catch (Exception ex) {
            loadingpipeNetwork = LOADINGSTATUS.ERROR;
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private boolean loadPipeVelocities(Network nw) {
        loadingPipeResult = LOADINGSTATUS.LOADING;
        action.description = "Load pipe velocities";
        action.progress = 0;
        fireLoadingActionUpdate();

        if (loadOnlyMainFile) {
            //Main File
            if (nw == null) {
                loadingPipeResult = LOADINGSTATUS.ERROR;
            } else {
                boolean loaded = false;
                try { //clear other results
                    if (clearOtherResults) {
                        control.getMultiInputData().clear();
                        control.getThreadController().cleanFromParticles();
                    }

                    TimeIndexContainer timeContainerPipe = null;
                    TimeIndexContainer timeContainerManholes = null;
                    //Load injections. Neede to calculate transport paths.
                    ArrayList<HEInjectionInformation> injection = null;
                    if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {
                        action.description = "Open Database";
                        if (resultDatabase == null || !resultDatabase.getDatabaseFile().getAbsolutePath().equals(fileMainPipeResult.getAbsolutePath())) {
                            if (tempFBDB != null && tempFBDB.getDatabaseFile().equals(fileMainPipeResult)) {
                                resultDatabase = tempFBDB;
                            } else {
                                resultDatabase = new HE_Database(fileMainPipeResult, true);
                            }
                        }
                        resultName = resultDatabase.readResultname();
                        action.description = "Load spill events";
                        if (this.loadInputInjections) {
                            injection = resultDatabase.readInjectionInformation();
                        } else {
                            injection = new ArrayList<>(0);
                        }

                        long starttime = System.currentTimeMillis();
                        //Find injection manholes
                        ArrayList<Capacity> injManholes = new ArrayList<>(injection.size());
                        if (verbose) {
                            System.out.println("Injections: " + injections.size());
                        }
                        for (InjectionInformation inj : injections) {
                            if (inj.getCapacity() == null && inj.spillInManhole() && inj.getCapacityName() != null && nw != null) {
                                //Search for injection reference
                                Capacity c = network.getCapacityByName(inj.getCapacityName());
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
                                inj.setCapacity(network.getManholeNearPositionLatLon(inj.getPosition()));
                                if (verbose) {
                                    System.out.println("found " + inj.getCapacity());
                                }
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
                            Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> cs = sparseLoadTimelines(nw, resultDatabase, pipesToLoad, manholesToLoad);
                            if (verbose) {
                                System.out.println(getClass() + ": Loaded sparsecontainer " + ((System.currentTimeMillis() - starttime) + "ms."));
                            }
                            timeContainerPipe = cs.first;
                            timeContainerManholes = cs.second;
                            loaded = true;
                            resultDatabase.close();
                        }
                    }

                    if (!loaded) {
                        //Load all values for all pipes/manholes.
                        Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> p;
                        action.description = "Load all pipe velocities";
                        if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {

                            p = resultDatabase.applyTimelines(nw);//HE_Database.readTimelines(fileMainPipeResult, control.getNetwork());
                        } else if (fileMainPipeResult.getName().endsWith(".rpt")) {
                            p = SWMM_IO.readTimeLines(fileMainPipeResult, nw);
                        } else {
                            throw new Exception("Not known filetype '" + fileMainPipeResult.getName() + "'");
                        }
                        timeContainerPipe = p.first;
                        timeContainerManholes = p.second;
                        PipeResultData data = new PipeResultData(fileMainPipeResult, fileMainPipeResult.getName(), p.first, p.second);
                        //Add only this result information
                        control.getMultiInputData().add(0, data);
                    }

                    if (cancelLoading) {
                        loadingPipeResult = LOADINGSTATUS.REQUESTED;
                        return false;
                    }

                    if (fileMainPipeResult.getName().endsWith(".idbf") || fileMainPipeResult.getName().endsWith(".idbr")) {
                        //Load spill events from database
                        if (injection != null && !injection.isEmpty()) {
                            action.description = "Apply spill events";
                            int materialnumber = 0;
                            for (HEInjectionInformation in : injection) {
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
                                double start = (in.stattime - timeContainerPipe.getFirstTime()) / 1000;
                                double duration = (in.endtime - timeContainerPipe.getFirstTime()) / 1000 - start;
                                Material mat = new Material("Schmutz " + materialnumber++, 1000, true);
                                int particlenumber = 20000;
                                InjectionInformation info;
                                if (in instanceof HE_MessdatenInjection) {
                                    HE_MessdatenInjection mess = (HE_MessdatenInjection) in;

                                    info = new InjectionInformation(c, timeContainerPipe.getFirstTime(), mess.timedValues, mat, mess.getConcentration(), particlenumber);

                                } else {
                                    info = new InjectionInformation(c, 0, in.mass, particlenumber, mat, start, duration);
                                }
                                if (c instanceof Pipe) {
                                    info.setPosition1D(((Pipe) c).getLength() * 0.5f);
//                                    System.out.println("loadc set position to " + info.getPosition1D());
                                }
                                if (injections.contains(info)) {
                                    injections.remove(info);
                                }
                                if (verbose) {
                                    System.out.println("Add injection: " + info.getMass() + "kg @" + in.capacityName + "  start:" + info.getStarttimeSimulationsAfterSimulationStart() + "s  last " + info.getDurationSeconds() + "s");
                                }

                                injections.add(info);
                            }
                        }
                    }
                    action.description = "Load create scenario";
                    if (scenario == null) {
                        scenario = new SpillScenario(timeContainerPipe, injections);
                    }
                    scenario.setTimesPipe(timeContainerPipe);
                    scenario.setTimesManhole(timeContainerManholes);
                    loadingPipeResult = LOADINGSTATUS.LOADED;
                } catch (Exception ex) {
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    loadingPipeResult = LOADINGSTATUS.ERROR;
                }
            }
        } else {
            while (!list_loadingPipeResults.isEmpty()) {
                if (cancelLoading) {
                    System.out.println("   LoadingThread is interrupted -> break");
                    return false;
                }
                Pair<File, Boolean> file = null;
                try {
                    file = list_loadingPipeResults.removeFirst();
                    if (verbose) {
                        System.out.println("try load " + file.first);
                    }
                    Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> p = resultDatabase.applyTimelines(nw);//HE_Database.readTimelines(file.first, control.getNetwork());
                    PipeResultData data = new PipeResultData(file.first, file.first.getName(), p.first, p.second);

                    if (file.second) {
                        if (clearOtherResults) {
                            //Remove all other timelines
                        }
                        control.getMultiInputData().add(0, data);
                        control.getThreadController().cleanFromParticles();
                        //Scenario laden only as mainresult
                        ArrayList<HEInjectionInformation> he_injection = resultDatabase.readInjectionInformation();//HE_Database.readInjectionInformation(file.first/*, 20000*/);
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
                            double start = (in.stattime - p.first.getFirstTime()) / 1000;
                            double duration = (in.endtime - p.first.getFirstTime()) / 1000 - start;
                            Material mat = new Material("Schmutz " + materialnumber++, 1000, true);
                            int particlenumber = 20000;
                            InjectionInformation info;
                            if (in instanceof HE_MessdatenInjection) {
                                HE_MessdatenInjection mess = (HE_MessdatenInjection) in;

                                info = new InjectionInformation(c, p.first.getFirstTime(), mess.timedValues, mat, mess.getConcentration(), particlenumber);

                            } else {
                                info = new InjectionInformation(c, 0, in.mass, particlenumber, mat, start, duration);
                            }
                            if (c instanceof Pipe) {
                                info.setPosition1D(((Pipe) c).getLength() * 0.5f);
//                                System.out.println("loadc set position to " + info.getPosition1D());
                            }
                            if (injections.contains(info)) {
                                injections.remove(info);
                            }
                            if (verbose) {
                                System.out.println("Add injection: " + info.getMass() + "kg @" + in.capacityName + "  start:" + info.getStarttimeSimulationsAfterSimulationStart() + "s  last " + info.getDurationSeconds() + "s");
                            }

                            injections.add(info);
                        }
                        if (scenario == null) {
                            scenario = new SpillScenario(p.first, injections);
                            scenario.setTimesPipe(p.first);
                            scenario.setTimesManhole(p.second);
                        }
                    } else {
                        control.getMultiInputData().add(data);
                    }

                    loadingPipeResult = LOADINGSTATUS.LOADED;
                    return true;
                } catch (Exception ex) {
                    if (file != null && file.first != null) {
                        System.err.println("Problem with File " + file.first + "   main? " + file.second);
                    }
                    Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    int n = JOptionPane.showConfirmDialog(null, "Load Pipenetwork with topological\ninformation of the result file?", "Network and Result not consistent.", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (n == JOptionPane.YES_OPTION) {
                        requestLoading = true;
                        setPipeNetworkFile(file.first);
                        list_loadingPipeResults.addFirst(file);
                        loadingPipeResult = LOADINGSTATUS.REQUESTED;
                        fireLoadingActionUpdate();
                        break;
                    }
                    loadingPipeResult = LOADINGSTATUS.ERROR;
                }
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
        try {
            long start = System.currentTimeMillis();
            Surface surf = HE_SurfaceIO.loadSurface(fileSurfaceCoordsDAT, fileSurfaceTriangleIndicesDAT, FileTriangleNeumannNeighboursDAT, fileSurfaceReferenceSystem);
            start = System.currentTimeMillis();
            //load neighbour definitions
            {
                if (fileSufaceNode2Triangle != null && fileSufaceNode2Triangle.exists()) {
                    surf.setNodeNeighbours(HE_SurfaceIO.loadNodesTriangleIDs(fileSufaceNode2Triangle), weightedSurfaceVelocities);
                } else {
                    //Need to create this reference file
                    action.description = "Create Node-Triangle reference File NODE2TRIANGLE.dat";
                    fireLoadingActionUpdate();
                    File outNodeTriangles = new File(fileSurfaceCoordsDAT.getParent(), "NODE2TRIANGLE.dat");
                    if (!outNodeTriangles.exists()) {
                        ArrayList<Integer>[] n2t = HE_SurfaceIO.findNodesTriangleIDs(surf.triangleNodes, surf.vertices.length);
                        HE_SurfaceIO.writeNodesTraingleIDs(n2t, outNodeTriangles);
                        fileSufaceNode2Triangle = outNodeTriangles;
                        surf.setNodeNeighbours(HE_SurfaceIO.loadNodesTriangleIDs(fileSufaceNode2Triangle), weightedSurfaceVelocities);
                    }
                }
            }
            start = System.currentTimeMillis();
            if (fileTriangleMooreNeighbours != null && fileTriangleMooreNeighbours.exists()) {
                surf.mooreNeighbours = HE_SurfaceIO.readMooreNeighbours(fileTriangleMooreNeighbours);
            } else {
                //Create neumann neighbours
                action.description = "Create Moore Neighbours File MOORE.dat";
                fireLoadingActionUpdate();
                fileTriangleMooreNeighbours = new File(fileSurfaceCoordsDAT.getParent(), "MOORE.dat");
                if (!fileTriangleMooreNeighbours.exists()) {
                    System.out.println("Create new Moore Neighbours File @" + fileTriangleMooreNeighbours);
                    HE_SurfaceIO.writeMooreTriangleNeighbours(surf.getTriangleNodes(), surf.getVerticesPosition().length, fileTriangleMooreNeighbours);
                    System.out.println("Created new Moore Neighbours File " + fileTriangleMooreNeighbours);
                    surf.mooreNeighbours = HE_SurfaceIO.readMooreNeighbours(fileTriangleMooreNeighbours);
                }
            }
            //Reset triangle IDs from Injections because the coordinate might have changed
            for (InjectionInformation injection : injections) {
                injection.setTriangleID(-1);
            }

            if (cancelLoading) {
                loadingSurface = LOADINGSTATUS.REQUESTED;
//                System.gc();
                return null;
            }
            changedSurface = true;
            action.description = "Surface loaded";
            fireLoadingActionUpdate();
            loadingSurface = LOADINGSTATUS.LOADED;
//            System.gc();
            return surf;
        } catch (Exception ex) {
            loadingSurface = LOADINGSTATUS.ERROR;
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
//        System.gc();
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
                if (lowername.endsWith("shp")) {
                    SHP_IO_GULLI.readTriangleFileToSurface(fileSurfaceWaterlevels, surface, verbose);
                } else if (lowername.endsWith("csv")) {
                    CSV_IO.readTriangleWaterlevels(surface, fileSurfaceWaterlevels);
                } else if (lowername.endsWith("gdb")) {
                    action.description = "Reading GDB surface";
                    fireLoadingActionUpdate();
                    HE_GDB_IO gdb = new HE_GDB_IO(fileSurfaceWaterlevels);
                    velocityLoader = gdb;
                    waterlevelLoader = gdb;
                    if (gdb.isResultDB()) {
                        if (gdb.hasWaterlevels()) {
                            long start = System.currentTimeMillis();
                            action.description = "Reading GDB surface waterlevels";
                            fireLoadingActionUpdate();
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
                        action.description = "GDB has not a result database.";
                        System.err.println(action.description);
                    }
                } else {
                    loadingSurfaceVelocity = LOADINGSTATUS.ERROR;
                    action.description = "Unknown file format of water-levels-file '" + fileSurfaceWaterlevels + "'.";
                    System.err.println(action.description);
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
                if (!loadGDBVelocity) {
                    surface.calcNeighbourVelocityFromTriangleVelocity();
                }
                if (!sparseSurfaceLoading) {
                    surface.calculateNeighbourVelocitiesFromWaterlevels();
                    surface.calculateVelocities2d();
                } else {
                    // Sparse loading
                    // Prepare for surfacecomputing2D
                    surface.waterlevelLoader = waterlevelLoader;
                    if (isLoadGDBVelocity()) {
                        surface.initSparseTriangleVelocityLoading(velocityLoader, true, true);
                    } else {
                        surface.initSparseTriangleVelocityLoading(null, true, true);
                    }
                }
                if (scenario != null) {
                    surface.setTimeContainer(createTimeContainer(scenario.getStartTime(), scenario.getEndTime(), surface.getNumberOfTimestamps()));
                    scenario.setTimesSurface(surface);
                } else {
                    System.err.println("No Scenario loaded, can not calculate timeintervalls for surface waterheight and velocities.");
                }

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
            System.out.println("manhole references already loaded: " + manhRefs.size());
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
        list_loadingPipeResults.clear();
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

                    surface.calcNeighbourVelocityFromTriangleVelocity();
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
        this.scenario=null;
        this.surface=null;
        this.network=null;
        this.injections.clear();
        this.startLoadingRequestedFiles(asThread);
    }

    public boolean addInjectionInformation(InjectionInformation inj) {
        if (injections.contains(inj)) {
            if (verbose) {
                System.out.println("Loadingcoordinator already contains injection @" + inj.getPosition());
            }
            injections.remove(inj);
            injections.add(inj);
            if (control.getScenario() != null) {
                control.getScenario().getInjections().remove(inj);
                control.getScenario().getInjections().add(inj);
            }
            return false;
        }
        return injections.add(inj);
    }

    public ArrayList<InjectionInformation> getInjections() {
        return injections;
    }

    public void clearInjections() {
        this.injections.clear();
    }

    public void setPipeNetworkFile(File networkFile) {
        this.fileNetwork = networkFile;
        this.loadingpipeNetwork = LOADINGSTATUS.REQUESTED;
    }

    public void setPipeResultsFile(File pipeResultFile, boolean clearOtherFiles) {
        this.fileMainPipeResult = pipeResultFile;
        this.loadingPipeResult = LOADINGSTATUS.REQUESTED;
        if (pipeResultFile != null) {
            this.clearOtherResults = clearOtherFiles;
            this.loadOnlyMainFile = true;
        } else {
            loadingPipeResult = LOADINGSTATUS.NOT_REQUESTED;
        }
    }

    public void addPipeResultsFile(File pipeResultFile, boolean asMainFile) {

        if (pipeResultFile != null) {
            list_loadingPipeResults.add(new Pair<>(pipeResultFile, asMainFile));
            this.clearOtherResults = false;
            this.loadingPipeResult = LOADINGSTATUS.REQUESTED;
            this.loadOnlyMainFile = false;
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
            this.fileSufaceNode2Triangle = null;
            this.fileTriangleMooreNeighbours = null;
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
        if (!fileNeighbours.exists()) {
            fileCoordinateReference = new File(surfaceTopologyDirectory, "city.xml");
            if (!fileNeighbours.exists()) {
                throw new FileNotFoundException("File for CoordinateReference could not be found: " + fileNeighbours.getAbsolutePath());
            }
        }

        // Files for calculating velocities on 2D surface ******
        File fileNode2Triangle = new File(surfaceTopologyDirectory, "NODE2TRIANGLE.dat");
        if (!fileNode2Triangle.exists()) {
            System.err.println("File for Nodes' triangles could not be found: " + fileNode2Triangle.getAbsolutePath());
            this.fileSufaceNode2Triangle = null;
        } else {
            this.fileSufaceNode2Triangle = fileNode2Triangle;
        }

        File mooreFile = new File(surfaceTopologyDirectory, "MOORE.dat");
        if (!mooreFile.exists()) {
            System.err.println("File for triangles' neumann neighbours could not be found: " + mooreFile.getAbsolutePath());
            this.fileTriangleMooreNeighbours = null;
        } else {
            this.fileTriangleMooreNeighbours = mooreFile;
        }

        //Files for merging Surface and Pipenetwork out-/inlets ******
        File fileStreetInlets = new File(surfaceTopologyDirectory, "SURF-SEWER_NODES.dat");
        if (!fileStreetInlets.exists()) {
            System.err.println("File for Streetinlets could not be found: " + fileStreetInlets.getAbsolutePath());
        } else {
            this.fileSurfaceInlets = fileStreetInlets;
        }

        File fileManhole2Surface = new File(surfaceTopologyDirectory, "SEWER-SURF_NODES.dat");
        if (!fileManhole2Surface.exists()) {
            System.err.println("File for Manhole position could not be found: " + fileManhole2Surface.getAbsolutePath());
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

    public void setSurfaceWaterlevelFile(File wlFile) {
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

    public File getFilePipeResultIDBF() {
        return fileMainPipeResult;
    }

    public void setFileStreetInletsSHP(File fileStreetInletsSHP) {
        this.fileStreetInletsSHP = fileStreetInletsSHP;
        loadingStreetInlets = LOADINGSTATUS.REQUESTED;
    }

    public FileContainer findDependentFiles(File pipeResult, boolean requestSurface) throws IOException, SQLException {

        boolean oldFilecorrespondsPipeNetwork = false, oldFilecorrespondsSurfaceTopology = false, oldFilecorrespondsSurfaceWaterlevel = false;
        File bestFilePipeNetwork = null, bestFileSurfacdirectory = null, bestFileSurfaceWaterlevel = null, bestPipeResultFile = pipeResult;

        try {
            bestFilePipeNetwork = LoadingCoordinator.findCorrespondingPipeNetworkFile(bestPipeResultFile);
            if (verbose) {
                System.out.println("found best fit to " + pipeResult.getName() + "  to be " + bestFilePipeNetwork);
            }
            if (bestFilePipeNetwork != null && (bestFilePipeNetwork.getName().endsWith("idbf") || bestFilePipeNetwork.getName().endsWith("idbr"))) {
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
        try {
            bestFileSurfacdirectory = LoadingCoordinator.findCorrespondingSurfaceDirectory(bestPipeResultFile);
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
            bestFileSurfaceWaterlevel = LoadingCoordinator.findCorrespondingWaterlevelFile(bestPipeResultFile);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        FileContainer fc = new FileContainer(pipeResult, bestFilePipeNetwork, bestFileSurfaceWaterlevel, bestFileSurfacdirectory, null);
        fc.setPipeNetworkLoaded(oldFilecorrespondsPipeNetwork);
        fc.setPipeResultLoaded(false);
        fc.setSurfaceResultLoaded(oldFilecorrespondsSurfaceWaterlevel);
        fc.setSurfaceTopologyLoaded(oldFilecorrespondsSurfaceTopology);
        return fc;
    }

    public void requestDependentFiles(File pipeResult, boolean requestSurface, boolean clearOtherResults) throws SQLException, IOException {
        this.setPipeResultsFile(pipeResult, clearOtherResults);
        File nwf = findCorrespondingPipeNetworkFile(pipeResult);
        if (nwf != null) {
            this.setPipeNetworkFile(nwf);
        }

        if (requestSurface) {
            File sf = findCorrespondingSurfaceDirectory(pipeResult);
            setSurfaceTopologyDirectory(sf);

            File wlf = findCorrespondingWaterlevelFile(pipeResult);
            setSurfaceWaterlevelFile(wlf);

        }
    }

    private void fireLoadingActionUpdate() {
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
            System.out.println("   does not exist. use result file to loa dnetwork");
            return resultFile; //information about the pipe network can also be found inside the result.
        } else if (resultFile.getName().endsWith("idbr")) {
            //SQLite Result file
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
            return resultFile; //information about the pipe network can also be found inside the result.
        } else if (resultFile.getName().endsWith("rpt")) {
            // SWMM Result file
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
        }
        return null;
    }

    public static File findCorrespondingSurfaceDirectory(File resultFile) throws SQLException, IOException {
        if (resultFile.getName().endsWith("rpt")) {
            return null;
        }
        if (tempFBDB == null || !resultFile.equals(tempFBDB.getDatabaseFile())) {
            tempFBDB = new HE_Database(resultFile, true);
        }
        String surfModelName = tempFBDB.readSurfaceModelname();
        if (surfModelName != null) {
            String surfaceFiles = resultFile.getParentFile().getParent() + File.separator + surfModelName + ".model";//HE_Database.readSurfaceModelname(resultFile) + ".model";
            File directorySurfaceFiles = new File(surfaceFiles);
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
        if (f.getName().toLowerCase().endsWith(".idbf") || f.getName().toLowerCase().endsWith(".idbr")) {
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

    /**
     * Stores all Files that are connected for an event.
     */
    public class FileContainer {

        private File pipeResult, pipeNetwork;

        private File surfaceResult;
        private File surfaceDirectory;
        private File inlets;

        private boolean pipeResultLoaded, pipeNetworkLoaded, surfaceResultLoaded, surfaceTopologyLoaded;

        public FileContainer(File pipeResult, File pipeNetwork, File surfaceResult, File surfaceDirectory, File inlets) {
            this.pipeResult = pipeResult;
            this.pipeNetwork = pipeNetwork;
            this.surfaceResult = surfaceResult;
            this.surfaceDirectory = surfaceDirectory;
            this.inlets = inlets;
        }

        public boolean isPipeResultLoaded() {
            return pipeResultLoaded;
        }

        public void setPipeResultLoaded(boolean pipeResultLoaded) {
            this.pipeResultLoaded = pipeResultLoaded;
        }

        public boolean isPipeNetworkLoaded() {
            return pipeNetworkLoaded;
        }

        public void setPipeNetworkLoaded(boolean pipeNetworkLoaded) {
            this.pipeNetworkLoaded = pipeNetworkLoaded;
        }

        public boolean isSurfaceResultLoaded() {
            return surfaceResultLoaded;
        }

        public void setSurfaceResultLoaded(boolean surfaceResultLoaded) {
            this.surfaceResultLoaded = surfaceResultLoaded;
        }

        public boolean isSurfaceTopologyLoaded() {
            return surfaceTopologyLoaded;
        }

        public void setSurfaceTopologyLoaded(boolean surfaceTopologyLoaded) {
            this.surfaceTopologyLoaded = surfaceTopologyLoaded;
        }

        public File getPipeResult() {
            return pipeResult;
        }

        public File getPipeNetwork() {
            return pipeNetwork;
        }

        public File getSurfaceResult() {
            return surfaceResult;
        }

        public File getSurfaceDirectory() {
            return surfaceDirectory;
        }

        public File getInlets() {
            return inlets;
        }
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

    public static Pair<SparseTimeLinePipeContainer, SparseTimeLineManholeContainer> sparseLoadTimelines(Network network, SparseTimeLineDataProvider dataprovider, Collection<Pipe> pipes, Collection<StorageVolume> manholes) throws Exception {
        SparseTimeLinePipeContainer container = new SparseTimeLinePipeContainer(dataprovider.loadTimeStepsNetwork());
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
        SparseTimeLineManholeContainer mcontainer = new SparseTimeLineManholeContainer(dataprovider.loadTimeStepsNetwork());
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
        control.getScenario().setTimesPipe(p.first);
    }

    public void stopLoadingSurfaceGrid() {
        this.loadingSurface = LOADINGSTATUS.NOT_REQUESTED;
    }

    public void stopLoadingSurfaceVelocity() {
        this.loadingSurfaceVelocity = LOADINGSTATUS.NOT_REQUESTED;
    }

    /**
     * Dataprovider for reading information about the current network elements.
     *
     * @return
     */
    public SparseTimeLineDataProvider getSparsePipeDataProvider() {
        if (!sparsePipeLoading) {
            return null;
        }
        if (resultDatabase != null) {
            return resultDatabase;
        }
        return tempFBDB;
    }

    /**
     * Prepare everything for loading a setup with information about files and
     * injections.
     *
     * @param setup
     */
    public void applySetup(Setup setup) {
        this.setPipeResultsFile(setup.resultFile_HE, true);
        try {
            this.requestDependentFiles(setup.resultFile_HE, setup.useSurface, true);
        } catch (SQLException ex) {
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LoadingCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.injections.clear();
        if (setup.injections != null && setup.injections.length > 0) {
            for (InjectionInformation in : setup.injections) {
                this.injections.add(in);
            }
        }
    }

    /**
     * The name of the current displayed Simulation.
     *
     * @return
     */
    public String getResultName() {
        return resultName;
    }

    @Override
    public void actionFired(Action action, Object source) {
    }

    @Override
    public void loadNetwork(Network network, Object caller) {

    }

    @Override
    public void loadSurface(Surface surface, Object caller) {

    }

}
