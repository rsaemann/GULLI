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

import control.listener.SimulationActionListener;
import control.particlecontrol.ParticlePipeComputing;
import control.Action.Action;
import control.listener.LoadingActionListener;
import control.listener.ParticleListener;
import control.multievents.PipeResultData;
import control.scenario.injection.InjectionInformation;
import control.scenario.Scenario;
import control.threads.ParticleThread;
import control.threads.ThreadController;
import io.NamedPipe_IO;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.HistoryParticle;
import model.particle.Material;
import model.particle.Particle;
import model.surface.Surface;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.topology.Capacity;
import model.topology.Connection;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.measurement.ParticleMeasurementSegment;
import model.topology.Pipe;
import model.topology.graph.GraphSearch;
import model.topology.graph.Pair;
import model.topology.measurement.ParticleMeasurementSection;
import org.opengis.referencing.operation.TransformException;

/**
 * This class is the interlink between model and view.
 *
 * @author saemann
 */
public class Controller implements SimulationActionListener, LoadingActionListener {

    private Network network;
    private Surface surface;
    private Scenario scenario;

    private final ThreadController threadController;
    private final LoadingCoordinator loadingCoordinator;
//    private GeoTools geoTools;

    private final ArrayList<PipeResultData> multiInputData = new ArrayList<>(1);

    private final ArrayList<LoadingActionListener> actionListener = new ArrayList<>(2);
    private final ArrayList<ParticleListener> particleListener = new ArrayList<>(2);

    public static boolean verbose = true;

    public Action currentAction = new Action("", null, false);

    /**
     * Every Xth particle becomes a HistoryParticle.
     */
    public int intervallHistoryParticles = 0;

    public static void main(String[] args) {
        try {
//            System.out.println("init new controller");
            Controller c = new Controller();
//            System.out.println("controller initialized");
        } catch (Exception ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public final ArrayList<InjectionInformation> injections = new ArrayList<>(1);
    public Controller() throws Exception {
//        geoTools = new GeoTools("EPSG:4326", "EPSG:31467", StartParameters.JTS_WGS84_LONGITUDE_FIRST);

        threadController = new ThreadController(8, this);

        threadController.addSimulationListener(this);
        loadingCoordinator = new LoadingCoordinator(this);

    }

//    private void importSurface(Surface surface) {
//        
//        System.out.println("++++++Controllr.importSurface");
//        this.surface = surface;
//
//        //Apply timecontainer for surface
//        if (surface != null) {
//            if (surface.getNumberOfTimestamps() > 1) {
//
//                long[] times = new long[surface.getNumberOfTimestamps()];
//                if (scenario != null) {
//                    currentAction.description = "Setup Times for Surface";
//                    currentAction.hasProgress = false;
//                    currentAction.progress = 0;
//                    fireAction(currentAction);
//                    long dt = (scenario.getEndTime() - scenario.getStartTime()) / (surface.getNumberOfTimestamps() - 1);
//                    for (int i = 0; i < times.length; i++) {
//                        times[i] = scenario.getStartTime() + i * dt;
//                    }
//
//                    TimeIndexContainer tc = new TimeIndexContainer(times);
//                    surface.setTimeContainer(tc);
//                    currentAction.progress = 1;
//                } else {
//                }
//            } else {
//                System.err.println("Surface hat nur " + surface.getNumberOfTimestamps() + " Timestamps");
//            }
//            currentAction.description = "Threadcontroller load Surface";
//            currentAction.hasProgress = false;
//            currentAction.progress = 0;
//            fireAction(currentAction);
//            threadController.loadSurface(surface, this);
//            currentAction.progress = 1;
//        }
//    }
    public boolean addActioListener(LoadingActionListener listener) {
        if (!actionListener.contains(listener)) {
            return actionListener.add(listener);
        }
        return false;
    }

    public boolean removeActionListener(LoadingActionListener listener) {
        return actionListener.remove(listener);
    }

    public boolean addParticleListener(ParticleListener listener) {
        if (!particleListener.contains(listener)) {
            return particleListener.add(listener);
        }
        return false;
    }

    public boolean removeParticleListener(ParticleListener listener) {
        return particleListener.remove(listener);
    }

    public void addSimulationListener(SimulationActionListener listener) {
        threadController.addSimulationListener(listener);
    }

    public boolean removeSimulationListener(SimulationActionListener listener) {
        return threadController.removeSimulationListener(listener);
    }

    protected void fireAction(Action action) {
        for (LoadingActionListener al : actionListener) {
            al.actionFired(action, this);
        }
    }

    protected void importNetwork(Network newNetwork) {
        currentAction.description = "import network";
        currentAction.startTime = System.currentTimeMillis();
        currentAction.hasProgress = false;
        currentAction.progress = 0;
        fireAction(currentAction);
        this.network = newNetwork;

        

        //Reference Injections, if Capacity was only referenced ba its name.
        for (InjectionInformation injection : loadingCoordinator.getInjections()) {
            if (injection.spillInManhole() && injection.getCapacity() == null) {
                Capacity c = network.getCapacityByName(injection.getCapacityName());
                injection.setCapacity(c);
            }
        }
        currentAction.progress = 1;
        fireAction(currentAction);
//        if (paintManager != null) {
//            paintManager.setNetwork(network);
//        }
//        if (controllFrame != null) {
//            controllFrame.getSingleControl().updateGUI();
//        }
    }

    public LoadingCoordinator getLoadingCoordinator() {
        return loadingCoordinator;
    }

    public void setDispersionCoefficientPipe(double K) {
        ParticlePipeComputing.setDispersionCoefficient(K);
    }

    public void loadScenario(Scenario sce) {
        currentAction.description = "load scenario";
        currentAction.startTime = System.currentTimeMillis();
        currentAction.hasProgress = false;
        currentAction.progress = 0;
        fireAction(currentAction);

        this.scenario = sce;
        this.scenario.init(this);
        currentAction.description = "load scenario: recalculate Injections";
        recalculateInjections();
        if (surface != null) {
            surface.setNumberOfMaterials(scenario.getMaxMaterialID() + 1);
        }
        threadController.setSimulationStartTime(sce.getStartTime());
        threadController.setSimulationTimeEnd(sce.getEndTime());
//        System.out.println("Simulation DateTime: " + new Date(threadController.getSimulationStartTime()) + " (" + sce.getStartTime() + ")" + " till " + new Date(threadController.getSimulationTimeEnd()) + " (" + threadController.getSimulationTimeEnd() + ") with " + threadController.getNumberOfTotalParticles() + " particles.");

        if (sce.getTimesPipe() != null) {
            currentAction.description = "load scenario: init measurement timelines";
            initMeasurementTimelines(sce, sce.getTimesPipe().getNumberOfTimes() - 1);

        }
        currentAction.description = "load scenario";
        currentAction.progress = 1;
    }

//    public PaintManager getPaintManager() {
//        return paintManager;
//    }
    /**
     * @param numberOfParticles
     * @param startCapacity
     * @param position1d
     * @param material
     * @return
     */
    private ArrayList<Particle> createParticlesAtStart(int numberOfParticles, Capacity startCapacity, double position1d, Material material) {
        if (startCapacity == null) {
            throw new IllegalArgumentException("Initial Capacity is null!");
        }
        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
        for (int i = 0; i < numberOfParticles; i++) {

            Particle p;
            if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                p = new HistoryParticle(startCapacity, position1d);
            } else {
                p = new Particle(startCapacity, position1d);
            }
            p.setSurrounding_actual(startCapacity);
            p.setPosition1d_actual(position1d);
            p.setPosition3d(p.getSurrounding_actual().getPosition3D(position1d));
            p.setMaterial(material);

            list.add(p);
        }
//        threadController.setParticles(list);
//        paintManager.setParticles(list);
        return list;
    }

    /**
     * @param numberOfParticles
     * @param massPerParticle
     * @param startCapacity
     * @param position1d
     * @param material
     * @param injectionDateTime
     * @return
     */
    private ArrayList<Particle> createParticlesAtTime(int numberOfParticles, double massPerParticle, Capacity startCapacity, double position1d, Material material, long injectionDateTime) {
        if (startCapacity == null) {
            throw new IllegalArgumentException("Initial Capacity is null!");
        }
        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
        for (int i = 0; i < numberOfParticles; i++) {
            Particle p;
            if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                p = new HistoryParticle(startCapacity, position1d, injectionDateTime, (float) massPerParticle);
            } else {
                p = new Particle(startCapacity, position1d, injectionDateTime, (float) massPerParticle);
            }
            p.setMaterial(material);
            list.add(p);
        }
        return list;
    }

    private void computeAnalyticalSolution(double c_ini, double dispersionCoefficient, Capacity startCapacity, double position1d, Material material, long starttime, long endtime) {
        //Calculate the analytical solution
        double numberOfParticles = c_ini;
        Manhole root;
        if (startCapacity instanceof Manhole) {
            root = (Manhole) startCapacity;
        } else if (startCapacity instanceof Pipe) {
            root = (Manhole) ((Pipe) startCapacity).getStartConnection().getManhole();
        } else {
            System.out.println("Can not compute analytical Solution for Capacity of type " + startCapacity.getClass());
            return;
        }
        ArrayList<Pair<Manhole, Double>> tree = GraphSearch.findLongestPaths(root);
        double v;// = 0.1965;//stamp.getValues().getVelocity();
        double dt = (-starttime + endtime) / 1000.;

        double K = dispersionCoefficient;
        for (Pair<Manhole, Double> tree1 : tree) {
            for (Connection connection : tree1.first.getConnections()) {
                Connection_Manhole_Pipe connectionP = (Connection_Manhole_Pipe) connection;

                if (connectionP.isStartOfPipe()) {
                    Pipe p = connectionP.getPipe();

                    double x0 = tree1.second; //Distanz von Start
                    double x1 = tree1.second + p.getLength() * 0.5; //Distanz von Start
                    double x2 = tree1.second + p.getLength(); //Distanz von Start

                }
            }
        }
    }

    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, Capacity startCapacity, Material material, double starttimeAfterScenarioStart, double duration) {
        if (startCapacity == null) {
            throw new IllegalArgumentException("Initial Capacity is null!");
        }
//        System.out.println("Threadcontroller has " + threadController.getNumberOfTotalParticles() + ".\t Adding " + numberOfParticles + " particles.");
        long scenarioStarttime = 0;
        if (scenario != null) {
            scenarioStarttime = scenario.getStartTime();
        }
        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
        {
            double dt = duration / (Math.max(1, numberOfParticles - 1));
            double t = 0;

            for (int i = 0; i < numberOfParticles; i++) {
                Particle p;
                if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    p = new HistoryParticle(startCapacity, 0, (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L), (float) massPerParticle);
                } else {
                    p = new Particle(startCapacity, 0, (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L), (float) massPerParticle);
                }
                p.setSurrounding_actual(startCapacity);
                p.setPosition1d_actual(0);
                p.setPosition3d(p.getSurrounding_actual().getPosition3D(0));
                p.setMaterial(material);
                list.add(p);
                t += dt;
            }
        }

//        if (calculateAnalyticalSolution) {
//            computeAnalyticalSolution(numberOfParticles, ParticlePipeComputing.getDispersionCoefficient(), startCapacity, position1d, material, starttime, endtime);
//        }
        return list;
    }

    public void setParticles(List<Particle> particles) {
        currentAction.description = "add and sort particles";
        currentAction.hasProgress = false;
        currentAction.progress = 0f;
        fireAction(currentAction);
        for (ParticleListener pl : particleListener) {
            pl.setParticles(particles, this);
        }
        currentAction.progress = 1;
        fireAction(currentAction);
    }

//    /**
//     * @deprecated @param numberOfParticles
//     * @param massPerParticle
//     * @param startCapacity
//     * @param position1d
//     * @param material
//     * @param starttime
//     * @param endtime
//     * @param calculateAnalyticalSolution
//     * @return
//     */
//    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, Capacity startCapacity, double position1d, Material material, long starttime, long endtime, boolean calculateAnalyticalSolution) {
//        if (startCapacity == null) {
//            throw new IllegalArgumentException("Initial Capacity is null!");
//        }
//        System.out.println("Threadcontroller has " + threadController.getNumberOfTotalParticles() + ".\t Adding " + numberOfParticles + " particles.");
//        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
//        {
//            float dt = (float) (endtime - starttime) / (float) (Math.max(1, numberOfParticles - 1));
//            float t = 0;
//
//            for (int i = 0; i < numberOfParticles; i++) {
//                Particle p;
//                if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
//                    p = new HistoryParticle(startCapacity, position1d, starttime + (int) t, (float) massPerParticle);
//                } else {
//                    p = new Particle(startCapacity, position1d, starttime + (int) t, (float) massPerParticle);
//                }
//                p.setSurrounding_actual(startCapacity);
//                p.setPosition1d_actual(position1d);
//                p.setPosition3d(p.getSurrounding_actual().getPosition3D(position1d));
//                p.setMaterial(material);
//                list.add(p);
//                t += dt;
//            }
//        }
////        threadController.setParticles(list);
////        paintManager.setParticles(list);
////
////        if (calculateAnalyticalSolution) {
////            computeAnalyticalSolution(numberOfParticles, ParticlePipeComputing.getDispersionCoefficient(), startCapacity, position1d, material, starttime, endtime);
////        }
//
//        return list;
//    }
    public void addParticleSegmentMeasurement(Pipe p, double positionAbsolute) {
        ParticleMeasurementSegment pms = new ParticleMeasurementSegment(p, positionAbsolute);
        for (ParticleThread pt : threadController.barrier_particle.getThreads()) {
            pt.addParticlemeasurement(pms);
        }
        threadController.barrier_sync.getThreads().get(0).addParticlemeasurement(pms);
    }

    public void addParticleSectionMeasurement(Pipe p, double positionAbsolute) {
        ParticleMeasurementSection pms = new ParticleMeasurementSection(p, positionAbsolute);
        for (ParticleThread pt : threadController.barrier_particle.getThreads()) {
            pt.addParticlemeasurement(pms);
        }
        threadController.barrier_sync.getThreads().get(0).addParticlemeasurement(pms);
    }

//    public Position getPositionFromLatLon(double longitude, double latitude) throws TransformException {
//        Coordinate c = new Coordinate(latitude, longitude);
//        c = geoTools.toUTM(c);
//        return new Position(longitude, latitude, c.x, c.y);
//    }
//
//    public Position getPositionFromGK(double x, double y) throws TransformException {
//        Coordinate c = new Coordinate(x, y);
//        c = geoTools.toGlobal(c);
//        return new Position(c.y, c.x, x, y);
//    }

    public Network getNetwork() {
        return network;
    }

    public ThreadController getThreadController() {
        return threadController;
    }

    public void resetScenario() {
//        System.out.println("resetScenario");
        currentAction.description = "Reset scenario";
        currentAction.hasProgress = false;
        currentAction.progress = 0;
        fireAction(currentAction);
//        if (this.controllFrame != null) {
//            this.controllFrame.getSingleControl().updateGUI();
//        }
        if (scenario != null) {
            scenario.reset();
        } else {
            System.out.println("No Scenario loaded.");
        }
        currentAction.description = "Reset scenario, Clear MeasurementTLs";
        fireAction(currentAction);
        if (scenario != null && scenario.getMeasurementsPipe() != null) {
            scenario.getMeasurementsPipe().clearValues();
        }
        currentAction.description = "Reset scenario, Set # Materials";
        fireAction(currentAction);
        if (surface != null) {
            surface.setNumberOfMaterials(scenario.getMaxMaterialID() + 1);
        }
        currentAction.description = "Reset scenario, Clean Pipe Measurements";
        fireAction(currentAction);
        boolean informed = false;
        for (Pipe pipe : network.getPipes()) {
            if (pipe.getMeasurementTimeLine() != null) {
                pipe.getMeasurementTimeLine().resetVisitedParticlesStorage();
            } else {
                if (!informed) {
                    System.err.println("Pipes do not have an initialized Measurement Timeline.");
                    informed = true;
                }
            }
        }

        if (NamedPipe_IO.instance != null) {
            NamedPipe_IO.instance.reset();
        }
        currentAction.progress = 1f;
        fireAction(currentAction);
        currentAction.description = "";
    }

    public void initMeasurementTimelines(Scenario scenario, long[] times, int numberOfContaminants) {
//        System.out.println(getClass() + "::initMeasurmenetTimeline with " + network.getPipes().size() + " Pipes");
        ArrayTimeLineMeasurementContainer container_m = ArrayTimeLineMeasurementContainer.init(times, network.getPipes().size(), numberOfContaminants);
        scenario.setMeasurementsPipe(container_m);
        ArrayTimeLineMeasurementContainer.instance = container_m;
        container_m.messungenProZeitschritt = container_m.getDeltaTime() / ThreadController.getDeltaTime();
        int number = 0;
        for (Pipe p : network.getPipes()) {
            p.setMeasurementTimeLine(new ArrayTimeLineMeasurement(container_m, number));
            number++;
        }
    }

    public void initMeasurementTimelines(Scenario scenario, int numberOfIntervalls) {

//        // USE statustimeline's dt
//        double dt = (scenario.getEndTime() - scenario.getStartTime()) / ((numberOfIntervalls));
//        if (dt > 5 * 60 * 1000) {
//            dt = 5 * 60 * 1000;
//        }
//        //Manual input 1min:
//        dt = 60 * 1000;
//
        int n = numberOfIntervalls;//(int) ((this.scenario.getEndTime() - this.scenario.getStartTime()) / dt + 1);
        double dt = (scenario.getEndTime() - scenario.getStartTime()) / ((n));
        long[] times = new long[n + 1];

        for (int i = 0; i < times.length; i++) {
            times[i] = this.scenario.getStartTime() + (long) (i * dt);
        }
//count different types of contaminants
        int numberContaminantTypes = 1;
        try {
            for (InjectionInformation injection : scenario.getInjections()) {
                numberContaminantTypes = Math.max(numberContaminantTypes, injection.getMaterial().materialIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMeasurementTimelines(scenario, times, numberContaminantTypes);
//        if (verbose) {
//            System.out.println(ArrayTimeLineMeasurement.class + " initilized.");
//        }
    }

//    public SimpleMapViewerFrame getMapFrame() {
//        return mapFrame;
//    }
//    public void openTimelineFrame(ArrayTimeLinePipeContainer[] timelineContainer) {
//        timelineFrame = new EditorTableFrame(null, this, timelineContainer);
//        timelineFrame.setTitle("Timeline");
//        timelineFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//
////        timelinePanel = new CapacityTimelinePanel("", this, timelineContainer);
////        timelineFrame.add(timelinePanel);
//        timelineFrame.setBounds(this.mapFrame.getX() + this.mapFrame.getWidth() + 30, this.mapFrame.getY(), 750, 700);
//        timelineFrame.setVisible(true);
//        timelinePanel = timelineFrame.getTimelinePanel();
//        threadController.addTimelinePanel(timelinePanel);
//    }
//    public void openTimelineFrame(PipeResultData input) {
//        openTimelineFrame(new PipeResultData[]{input});
//    }
//
//    public void openTimelineFrame(Collection<PipeResultData> input) {
//        openTimelineFrame(input.toArray(new PipeResultData[input.size()]));
//    }
//    public void openTimelineFrame(PipeResultData[] input) {
//        timelineFrame = new EditorTableFrame(null, this, input);
//        timelineFrame.setTitle("Timeline");
//        timelineFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//
////        timelinePanel = new CapacityTimelinePanel("", this, timelineContainer);
////        timelineFrame.add(timelinePanel);
//        timelineFrame.setBounds(this.mapFrame.getX() + this.mapFrame.getWidth() + 30, this.mapFrame.getY(), 750, 700);
//        timelineFrame.setVisible(true);
//        timelinePanel = timelineFrame.getTimelinePanel();
//        threadController.addTimelinePanel(timelinePanel);
//    }
//
//    public void openSpatialLineFrame(ArrayTimeLinePipe timeline) {
//        spacelineFrame = new JFrame("Spatial lines");
//        spacelineFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        spacelinePanel = new SpacelinePanel(timeline, "Timeline");
//        spacelineFrame.add(spacelinePanel);
//        spacelineFrame.setBounds(this.mapFrame.getX() + this.mapFrame.getWidth() + 100, this.mapFrame.getY() + 100, 750, 500);
//        spacelineFrame.setVisible(true);
//        spacelinePanel.setSplitDivider(0.8);
////        JFrame pframe = new JFrame("Pipes Timeline");
////        pframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
////        mptPanel = new MultiplePipesTimelinesPanel(null, mapFrame.getMapViewer(), network);
////        pframe.add(mptPanel);
////        pframe.setBounds(this.mapFrame.getX() + this.mapFrame.getWidth() + 30, this.mapFrame.getY() + 50, 800, 700);
////        pframe.setVisible(true);
//    }
//    private void showTimeline(Capacity c) {
//
//        if (timelineFrame == null || !timelineFrame.isVisible()) {
//            if (c == null) {
//                return;
//            } else {
//                openTimelineFrame(getMultiInputData());
//            }
//        }
//
//        if (c == null) {
//            timelineFrame.setStorage(null, "");
//
//            return;
//        }
//        timelineFrame.setStorage(c, c.toString());
////        spacelinePanel.setTimeToShow(c, c.toString());
//    }
//    private void createTimeLineListener() {
//        LocationIDListener listener = new LocationIDListener() {
//
//            @Override
//            public void selectLocationID(Object o, String layer, long l) {
////                System.out.println("Controller: selectLocationID "+o+", "+layer+", "+l);
//                try {
//                    if (o == this) {
//                        return;
//                    }
////                if (network == null) {
////                    return;
////                }
////                System.out.println("search for "+l);
//                    if (layer.equals(PaintManager.layerPipes)) {
//                        for (Pipe pipe : network.getPipes()) {
//                            if (pipe.getAutoID() == l) {
//                                showTimeline(pipe);
//                                return;
//                            }
//                        }
//
//                    } else if (layer.equals(PaintManager.layerManhole)) {
//                        for (Manhole pipe : network.getManholes()) {
//                            if (pipe.getAutoID() == l) {
//                                showTimeline(pipe);
//                                return;
//                            }
//                        }
//
//                    } else if (layer.equals(PaintManager.layerTraingleMeasurement)) {
//                        showTimeline(surface.triangleCapacitys[(int) l]);
//                        return;
//
//                    } else if (layer.startsWith(PaintManager.layerTriangle)) {
//                        showTimeline(surface.triangleCapacitys[(int) l]);
//                        return;
//
//                    } else {
//                        showTimeline(null);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//        };
//
//        this.mapFrame.getMapViewer()
//                .addListener(listener);
//    }
//    public void showStateAtTime(long time, boolean repaint) {
//        this.paintManager.showOverspillingManholes(time);
//        this.paintManager.showPipeFillRate(time);
//        if (repaint) {
//            this.mapFrame.getMapViewer().repaint();
//        }
//    }
//    public File getDatabaseFile() {
//        return databaseFile;
//    }
    public void start() {

        if (scenario == null) {
            throw new NullPointerException("No Scenario loaded.");
        }
        threadController.start();
    }

    public void recalculateInjections() {
        for (ParticleListener pl : particleListener) {
            pl.clearParticles(this);
        }

        if (network == null) {
            System.err.println("No network loaded yet. Can not find capacities to inject particles.");
        } else {
            if (scenario == null) {
                return;
            }
            if (scenario.getInjections() != null) {
                int numberofParticles = 0;
                for (InjectionInformation in : scenario.getInjections()) {
                    numberofParticles += in.getNumberOfParticles();
                }
                ArrayList<Particle> particles = new ArrayList<>(numberofParticles);
                for (InjectionInformation in : scenario.getInjections()) {

                    if (in.spillOnSurface()) {
//                         if (in.spillOnSurface()) {
                        if (in.getTriangleID() < 0) {

                            if (in.getPosition() != null) //search for correct triangle
                            {
                                int triangleID = -1;
                                try {
                                    triangleID = surface.triangleIDNear(in.getPosition());
                                } catch (TransformException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                if (triangleID >= 0) {
                                    in.setTriangleID(triangleID);
                                    System.out.println("Found triangle " + triangleID + " for injection @" + in.getPosition());
                                } else {
                                    System.out.println("Could NOT find a triangle for injection @" + in.getPosition());
                                }
                            }
                        }
//                            }

                        ArrayList<Particle> p = this.createParticlesOverTimespan(in.getNumberOfParticles(), in.getMass() / (double) in.getNumberOfParticles(), getSurface(), in.getMaterial(), in.getStarttimeSimulationsAfterSimulationStart(), in.getDurationSeconds());
                        for (Particle p1 : p) {
                            p1.setInjectionCellID(in.getTriangleID());
                            p1.setOnSurface();
                        }
                        particles.addAll(p);
                    } else {
                        try {
                            Capacity c = null;

                            c = in.getCapacity();

                            if (c == null && in.getPosition() != null) {
                                //Search by Position
                                c = network.getManholeNearPositionLatLon(in.getPosition().getLatitude(), in.getPosition().getLongitude());
                            }

                            if (c == null && in.getCapacityName() != null) {
                                //Search by Name
                                c = network.getCapacityByName(in.getCapacityName());
                            }

                            if (c == null) {
                                System.err.println("Could not find capacity '" + in.getCapacity() + "' to inject particles.");
                                continue;
                            }
                            in.setChanged(false);//Everything was set.
                            ArrayList<Particle> p = this.createParticlesOverTimespan(in.getNumberOfParticles(), in.getMass() / (double) in.getNumberOfParticles(), c, in.getMaterial(), in.getStarttimeSimulationsAfterSimulationStart(), in.getDurationSeconds());
                            particles.addAll(p);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
                this.setParticles(particles);
            }
        }
        if (surface != null) {
            surface.setNumberOfMaterials(scenario.getMaxMaterialID() + 1);
//            System.out.println("new number of materials: "+surface.getNumberOfMaterials());
        }
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
//        System.out.println("Counter of manhole reaching per timestep:");
//        for (int i = 0; i < ParticlePipeComputing.passedPipesCounter.length; i++) {
//            System.out.println(i + ": " + ParticlePipeComputing.passedPipesCounter[i]);
//
//        }

//        if (mapFrame != null) {
//            mapFrame.toFront();
//        }
//        if (timelineFrame != null) {
//            timelineFrame.toFront();
//        }
    }

    public ArrayList<PipeResultData> getMultiInputData() {
        return multiInputData;
    }

    public void updatedInputData() {
        if (multiInputData.isEmpty()) {
            return;
        }
    }

    public PipeResultData getSingleEventInputData() {
        if (multiInputData.isEmpty()) {
            return null;
        }
        return multiInputData.get(0);
    }

//    public CapacityTimelinePanel getTimelinePanel() {
//        return timelinePanel;
//    }
    public Surface getSurface() {
        return surface;
    }

//    public ControllFrame getControllFrame() {
//        return controllFrame;
//    }
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public void simulationSTART(Object caller) {
    }

    @Override
    public void simulationPAUSED(Object caller) {
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
    }

    @Override
    public void simulationSTOP(Object caller) {
    }

    @Override
    public void simulationRESET(Object caller) {
//        recalculateInjections();
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
    }

    @Override
    public void simulationINIT(Object caller) {
//        recalculateInjections();
    }

    @Override
    public void actionFired(Action action, Object source) {

    }

    @Override
    public void loadNetwork(Network network, Object caller) {
        //Repeater
        this.importNetwork(network);
//        System.out.println("controller load Network");
//        if (caller instanceof LoadingCoordinator) {
        for (LoadingActionListener ll : actionListener) {
//                System.out.println("inform "+ll+" about loaded network");
            ll.loadNetwork(network, caller);
        }
//        }
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
//        if (caller instanceof LoadingCoordinator) {
        this.surface = surface;
        for (LoadingActionListener ll : actionListener) {
            currentAction.description = "contrl. loadsurface inform " + ll;
            currentAction.progress = 0f;
            fireAction(currentAction);
            ll.loadSurface(surface, caller);
        }
//        }
    }

}
