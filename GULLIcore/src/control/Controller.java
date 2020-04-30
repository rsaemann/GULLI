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

import com.vividsolutions.jts.geom.Coordinate;
import control.listener.SimulationActionListener;
import control.particlecontrol.ParticlePipeComputing;
import control.Action.Action;
import control.listener.LoadingActionListener;
import control.listener.ParticleListener;
import control.multievents.PipeResultData;
import control.particlecontrol.DiffusionCalculator2D;
import control.particlecontrol.ParticleSurfaceComputing2D;
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
import model.surface.measurement.SurfaceMeasurementTriangleRaster;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.timeline.array.TimeIndexCalculator;
import model.timeline.array.TimeIndexContainer;
import model.timeline.sparse.SparseTimeLinePipeContainer;
import model.topology.Capacity;
import model.topology.Connection;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.measurement.ParticleMeasurementSegment;
import model.topology.Pipe;
import model.topology.Position;
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

    private final ArrayList<PipeResultData> multiInputData = new ArrayList<>(1);

    private final ArrayList<LoadingActionListener> actionListener = new ArrayList<>(2);
    private final ArrayList<ParticleListener> particleListener = new ArrayList<>(2);

    public static boolean verbose = true;

    public Action currentAction = new Action("", null, false);

    private boolean requestRecalculationOfInjections = false;

    /**
     * Every Xth particle becomes a HistoryParticle.
     */
    public int intervallHistoryParticles = 0;

    public Controller() throws Exception {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
//        numberOfCores=1;
        threadController = new ThreadController(Math.max(1, numberOfCores), this);

        threadController.addSimulationListener(this);
        loadingCoordinator = new LoadingCoordinator(this);

    }

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
    }

    public LoadingCoordinator getLoadingCoordinator() {
        return loadingCoordinator;
    }

    public void setDispersionCoefficientPipe(double K) {
        ParticlePipeComputing.setDispersionCoefficient(K);
    }

    /**
     * Sets the diffusion coefficient to all particlesurfce computing objects in
     * their directD calculation array.
     *
     * @param K
     */
    public void setDispersionCoefficientSurface(double K) {
        try {
            for (ParticleThread particleThread : threadController.getParticleThreads()) {
                DiffusionCalculator2D d = ((ParticleSurfaceComputing2D) particleThread.getSurfaceComputing()).getDiffusionCalculator();
                d.directD = new double[]{K, K, K};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadScenario(Scenario sce, Object caller) {
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

//        if (sce.getTimesPipe() != null) {
//            currentAction.description = "load scenario: init measurement timelines";
//            initMeasurementTimelines(sce, sce.getTimesPipe().getNumberOfTimes() - 1);
//
//        }
        currentAction.description = "load scenario";
        currentAction.progress = 1;
    }

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

    /**
     * @deprecated used for former comparison between particles and analystical
     * solution
     * @param c_ini
     * @param dispersionCoefficient
     * @param startCapacity
     * @param position1d
     * @param material
     * @param starttime
     * @param endtime
     */
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

    /**
     * Linear interpolates the intensity of the particles
     *
     * @param numberOfParticles
     * @param massPerParticle [kg/particle]
     * @param startCapacity
     * @param material
     * @param starttimeAfterScenarioStart [seconds]
     * @param duration [seconds]
     * @param startIntensity [kg/s]
     * @param endIntensity [kg/s]
     * @return
     */
    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, Capacity startCapacity, Material material, double starttimeAfterScenarioStart, double duration, double startIntensity, double endIntensity) {
        if (startCapacity == null) {
            throw new IllegalArgumentException("Initial Capacity is null!");
        }
        long scenarioStarttime = 0;
        if (scenario != null) {
            scenarioStarttime = scenario.getStartTime();
        }
        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
        if (duration < 0.01) {
            //Instant injection
            for (int i = 0; i < numberOfParticles; i++) {
                Particle particle;
                if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(startCapacity, 0, (long) (scenarioStarttime + (starttimeAfterScenarioStart) * 1000L), (float) massPerParticle);
                } else {
                    long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart) * 1000L);
                    particle = new Particle(startCapacity, 0, injectiontime, (float) massPerParticle);
                }
                particle.setMaterial(material);
                particle.setWaiting();
                list.add(particle);
            }
            return list;
        }
        double s = (endIntensity - startIntensity) / duration;
        //if slope is 0 we can use the constant injection
        if (Math.abs(s) < 0.000001) {
            return createParticlesOverTimespan(numberOfParticles, massPerParticle, startCapacity, material, starttimeAfterScenarioStart, duration);
        }
        if (s > 0) {
            //increasing injection
            double p = startIntensity / s;
//            System.out.println("q1-q0/dt = " + s + "\tp=" + p);
            double maxQ = -numberOfParticles * 2. * massPerParticle / Math.abs(s);
            double maxT = (-p + Math.sqrt(p * p - maxQ));
            double factor = duration / maxT;
//            System.out.println("factor:" + factor);
//            System.out.println("q1-q0/dt = " + s + "\tp=" + p + "\tfactor:" + factor + "  mass per PArticle=" + massPerParticle);
            for (int i = 0; i < numberOfParticles; i++) {
                Particle particle;
                double q = -i * 2. * massPerParticle / (s);
                double t = (-p + Math.sqrt(p * p - q)) * factor;
                if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(startCapacity, 0, (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L), (float) massPerParticle);
                } else {
//                    System.out.println(i + ": " + t + "\t q=" + q + "  mass/P:" + massPerParticle + "   duration:" + duration);
                    long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L);
                    particle = new Particle(startCapacity, 0, injectiontime, (float) massPerParticle);
                }
                particle.setMaterial(material);
                particle.setWaiting();
                list.add(particle);
            }
        } else {
            //decreasing injection
            double p = startIntensity / s;
//            System.out.println("q1-q0/dt = " + s + "\tp=" + p);
            double maxQ = -numberOfParticles * 2. * massPerParticle / Math.abs(s);
            double maxT = (-p - Math.sqrt(p * p - maxQ));
            double factor = duration / maxT;
//            System.out.println("factor:" + factor);
//            System.out.println("q1-q0/dt = " + s + "\tp=" + p + "\tfactor:" + factor + "  mass per PArticle=" + massPerParticle);
            for (int i = 0; i < numberOfParticles; i++) {
                Particle particle;
                double q = -(numberOfParticles - i) * massPerParticle * 2. / (Math.abs(s));
                double t = duration - (-p - Math.sqrt(p * p - q)) * factor;
                if (intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(startCapacity, 0, (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L), (float) massPerParticle);
                } else {
//                    System.out.println(i + ": " + t + "\t q=" + q + "  mass/P:" + massPerParticle + "   duration:" + duration);
                    long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L);
                    particle = new Particle(startCapacity, 0, injectiontime, (float) massPerParticle);
                }
                particle.setMaterial(material);
                particle.setWaiting();
                list.add(particle);
            }
        }

        return list;
    }

    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, Capacity startCapacity, Material material, double starttimeAfterScenarioStart, double duration) {
        if (startCapacity == null) {
            throw new IllegalArgumentException("Initial Capacity is null!");
        }
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
                p.setMaterial(material);
                p.setWaiting();
                list.add(p);
                t += dt;
            }
        }

        return list;
    }

    /**
     * Send information about particles to all listener
     *
     * @param particles
     */
    private void setParticles(List<Particle> particles) {
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

    public void addParticleSegmentMeasurement(Pipe p, double positionAbsolute) {
        ParticleMeasurementSegment pms = new ParticleMeasurementSegment(p, positionAbsolute);
        for (ParticleThread pt : threadController.barrier_particle.getThreads()) {
            pt.addParticlemeasurement(pms);
        }
        threadController.syncThread_pipes.addParticlemeasurement(pms);
    }

    public void addParticleSectionMeasurement(Pipe p, double positionAbsolute) {
        ParticleMeasurementSection pms = new ParticleMeasurementSection(p, positionAbsolute);
        for (ParticleThread pt : threadController.barrier_particle.getThreads()) {
            pt.addParticlemeasurement(pms);
        }
        threadController.syncThread_pipes.addParticlemeasurement(pms);
    }

    public Network getNetwork() {
        return network;
    }

    public ThreadController getThreadController() {
        return threadController;
    }

    public void resetScenario() {
        currentAction.description = "Reset scenario";
        currentAction.hasProgress = false;
        currentAction.progress = 0;
        fireAction(currentAction);
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
            surface.reset();
        }
        currentAction.description = "Reset scenario, Clean Pipe Measurements";
        fireAction(currentAction);
        boolean informed = false;
        if (network != null) {
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
        } else {
            System.err.println("Network is not yet loaded. Cannot reset MeasurementTimelines of Pipes.");
        }

        if (NamedPipe_IO.instance != null) {
            NamedPipe_IO.instance.reset();
        }
        currentAction.progress = 1f;
        fireAction(currentAction);
        currentAction.description = "";
    }
    
    /**
     * use information of this scenario to initialise the sampling interval.
     * The measuremnt timeline will have the exact same intervals as the pipes timeline.
     * @param scenario 
     */
    public void initMeasurementTimelines(Scenario scenario){
        initMeasurementTimelines(scenario, scenario.getTimesPipe().getNumberOfTimes()-1);
    }
    
     public void initMeasurementTimelines(Scenario scenario, TimeIndexContainer times, int numberOfContaminants) {
//        System.out.println(getClass() + "::initMeasurmenetTimeline with " + network.getPipes().size() + " Pipes");
        ArrayTimeLineMeasurementContainer container_m = ArrayTimeLineMeasurementContainer.init(times, network.getPipes().size(), numberOfContaminants);
        scenario.setMeasurementsPipe(container_m);
        ArrayTimeLineMeasurementContainer.instance = container_m;
        container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
        System.out.println("Simulation step: "+ThreadController.getDeltaTime()+"s\t sampleinterval:"+container_m.getDeltaTimeS()+" \t-> "+ArrayTimeLineMeasurementContainer.instance.samplesPerTimeinterval+" samples per interval");
 
        int number = 0;
        for (Pipe p : network.getPipes()) {
            p.setMeasurementTimeLine(new ArrayTimeLineMeasurement(container_m, number));
            number++;
        }
    }

    public void initMeasurementTimelines(Scenario scenario, long[] times, int numberOfContaminants) {
//        System.out.println(getClass() + "::initMeasurmenetTimeline with " + network.getPipes().size() + " Pipes");
        ArrayTimeLineMeasurementContainer container_m = ArrayTimeLineMeasurementContainer.init(times, network.getPipes().size(), numberOfContaminants);
        scenario.setMeasurementsPipe(container_m);
        ArrayTimeLineMeasurementContainer.instance = container_m;
        container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
        System.out.println("Simulation step: "+ThreadController.getDeltaTime()+"s\t sampleinterval:"+container_m.getDeltaTimeS()+" \t-> "+ArrayTimeLineMeasurementContainer.instance.samplesPerTimeinterval+" samples per interval");
 
        int number = 0;
        for (Pipe p : network.getPipes()) {
            p.setMeasurementTimeLine(new ArrayTimeLineMeasurement(container_m, number));
            number++;
        }
    }

    /**
     * Set the number of sampling intervals by setting the duration of a single
     * interval.
     *
     * @param scenario with definition of start and end time
     * @param secondsPerInterval duration of an interval in seconds
     * @return the number of intervals
     */
    public int initMeasurementsTimelinesBySeconds(Scenario scenario, double secondsPerInterval) {
        int numberIntervals = (int) ((scenario.getEndTime() - scenario.getStartTime()) / (1000*secondsPerInterval));
        initMeasurementTimelines(scenario, numberIntervals);
        return numberIntervals;
    }

    /**
     * Set the number of sample intervals by definition of the number of
     * intervals
     *
     * @param scenario
     * @param numberOfIntervalls
     * @return seconds per interval
     */
    public double initMeasurementTimelines(Scenario scenario, int numberOfIntervalls) {

        int n = numberOfIntervalls;//(int) ((this.scenario.getEndTime() - this.scenario.getStartTime()) / dt + 1);
        double dt = (scenario.getEndTime() - scenario.getStartTime()) / ((n));
        System.out.println("sample dt= "+dt+"ms.  duration:"+(scenario.getEndTime() - scenario.getStartTime()));
        long[] times = new long[n + 1];

        for (int i = 0; i < times.length; i++) {
            times[i] = this.scenario.getStartTime() + (long) (i * dt);
        }
        //count different types of contaminants
        int numberContaminantTypes = 1;
        try {
            for (InjectionInformation injection : scenario.getInjections()) {
                numberContaminantTypes = Math.max(numberContaminantTypes, injection.getMaterial().materialIndex+1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMeasurementTimelines(scenario, times, numberContaminantTypes);
        return dt;
    }
    
    /**
     * Set the number of sample intervals by definition of the number of
     * intervals
     *
     * @param scenarioStarttime
     * @param deltatimeSeconds
     * @param numberOfIntervalls
     * @return seconds per interval
     */
    public double initMeasurementTimelines(long scenarioStarttime, int numberOfIntervalls, double deltatimeSeconds) {

        int n = numberOfIntervalls;//(int) ((this.scenario.getEndTime() - this.scenario.getStartTime()) / dt + 1);
        double dt = deltatimeSeconds*1000; //in MS
        long[] times = new long[n + 1];

        for (int i = 0; i < times.length; i++) {
            times[i] = scenarioStarttime + (long) (i * dt);
        }
//count different types of contaminants
        int numberContaminantTypes = 1;
        try {
            for (InjectionInformation injection : scenario.getInjections()) {
                numberContaminantTypes = Math.max(numberContaminantTypes, injection.getMaterial().materialIndex+1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMeasurementTimelines(scenario, times, numberContaminantTypes);
        return dt;
    }

    /**
     * Tells the Threadcontroller to start the simulation.
     */
    public void start() {

        if (scenario == null) {
            throw new NullPointerException("No Scenario loaded.");
        }
        
        if(scenario.getMeasurementsPipe()==null){
            System.out.println("Initialize sampling intervals with input interval length as no user defined sampling was set.");
            initMeasurementTimelines(scenario);
        }
        
        if (requestRecalculationOfInjections) {
            recalculateInjections();
        }
        threadController.start();
    }

    /**
     * Clears and Reinitialize the particles based on the Injectioninformation
     * in the scenario.
     */
    public void recalculateInjections() {
        if (threadController.isSimulating()) {
            requestRecalculationOfInjections = true;
            return;
        }
        requestRecalculationOfInjections = false;
//        System.out.println("recalculateInjections");
        for (ParticleListener pl : particleListener) {
            pl.clearParticles(this);
        }

        currentAction.description = "Injection spill";
        currentAction.hasProgress = true;
        currentAction.progress = 0f;
        fireAction(currentAction);

        if (scenario == null || scenario.getInjections() == null) {
            return;
        }

        int totalNumberParticles = 0;
        int maxMaterialID = -1;
        ArrayList<Material> indexedMaterials = new ArrayList<>();
        for (InjectionInformation injection : scenario.getInjections()) {
            totalNumberParticles += injection.getNumberOfParticles();
            maxMaterialID = Math.max(maxMaterialID, injection.getMaterial().materialIndex);
            if (!indexedMaterials.contains(injection.getMaterial())) {
                indexedMaterials.add(injection.getMaterial());
            }
        }
        for (int i = 0; i < indexedMaterials.size(); i++) {
            indexedMaterials.get(i).materialIndex = i;
        }
        maxMaterialID = indexedMaterials.size() - 1;

        ArrayList<Particle> allParticles = new ArrayList<>(totalNumberParticles);
        int counter = 0;
        for (InjectionInformation injection : scenario.getInjections()) {
            counter++;
            currentAction.description = "Injection spill " + counter + "/" + scenario.getInjections().size();
            currentAction.hasProgress = true;
            currentAction.progress = counter / (float) scenario.getInjections().size();
            fireAction(currentAction);

            //find capacity
            Capacity c = null;
            int surfaceCell = -1;
            Position position = null;
            double pipeposition = 0;
            if (injection.spillOnSurface()) {
                if (getSurface() == null) {
                    continue;
                }
                c = getSurface();
                if (injection.getPosition() != null) {
                    Coordinate utm;
                    try {
                        utm = getSurface().getGeotools().toUTM(injection.getPosition());
                        position = new Position(injection.getPosition().getLongitude(), injection.getPosition().getLatitude(), utm.x, utm.y);
                    } catch (TransformException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (injection.getTriangleID() >= 0) {
                    surfaceCell = injection.getTriangleID();
                } else if (injection.getCapacityName() != null) {
                    if (network != null) {
                        Capacity tempC = network.getPipeByName(injection.getCapacityName());
                        if (tempC == null) {
                            tempC = network.getCapacityByName(injection.getCapacityName());
                        }
                        if (tempC != null) {
                            if (tempC instanceof Manhole) {
                                Manhole mh = (Manhole) tempC;
                                if (mh.getSurfaceTriangleID() >= 0) {
                                    surfaceCell = mh.getSurfaceTriangleID();
                                }
                            }
                        }
                    }
                }
                if (surfaceCell < 0) {
                    if (position != null) {
                        //Try to find correct triangle at existing coordinates
                        int id = getSurface().findContainingTriangle(position.x, position.y, 50);
                        if (id >= 0) {
                            surfaceCell = id;
                        }
                    }
                }
                if (position == null || surfaceCell >= 0) {
                    try {
                        double[] utm = getSurface().getTriangleMids()[surfaceCell];
                        Coordinate wgs84 = getSurface().getGeotools().toGlobal(new Coordinate(utm[0], utm[1]), true);
                        position = new Position(wgs84.x, wgs84.y, utm[0], utm[1]);
                    } catch (TransformException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (c == null) {
                    System.err.println("Cannot find surface for injection " + injection);
                    continue;
                }
                if (position == null) {
                    System.err.println("Cannot find position for injection " + injection);
                    continue;
                }
                if (surfaceCell < 0) {
                    System.err.println("Cannot find surface cell for injection " + injection);
                    continue;
                }
            } else {
                //Spill to pipesystem
                if (getNetwork() == null) {
                    continue;
                }
                pipeposition = injection.getPosition1D();
                if (injection.getCapacity() != null) {
                    c = injection.getCapacity();
                } else {
                    //Need to find capacity 
                    // by name
                    if (injection.getCapacityName() != null && !injection.getCapacityName().isEmpty()) {
                        c = getNetwork().getCapacityByName(injection.getCapacityName());
                        if (c == null) {
                            System.err.println("Cannot find Capacity with name '" + injection.getCapacityName() + "' for injection " + injection);
                        }
                    }
                    if (c == null && injection.getPosition() != null) {
                        c = getNetwork().getManholeNearPositionLatLon(injection.getPosition());
                    }
                }
                if (c == null) {
                    System.err.println("Cannot find Capacity for injection " + injection);
                    continue;
                }
            }
            //Create particles over time
            if (injection.isActive()) {
                for (int i = 0; i < injection.getNumberOfIntervals(); i++) {
                    if (injection.spillOnSurface()) {
                        ArrayList<Particle> ps = createParticlesOverTimespan(injection.particlesInInterval(i), injection.massInInterval(i) / (double) injection.particlesInInterval(i), c, injection.getMaterial(), injection.getIntervalStart(i), injection.getIntervalDuration(i), injection.getIntensity(i), injection.getIntensity(i + 1));
                        for (Particle p : ps) {
                            p.setInjectionCellID(surfaceCell);
                        }
                        allParticles.addAll(ps);
                    } else {
                        ArrayList<Particle> ps = createParticlesOverTimespan(injection.particlesInInterval(i), injection.massInInterval(i) / (double) injection.particlesInInterval(i), c, injection.getMaterial(), injection.getIntervalStart(i), injection.getIntervalDuration(i), injection.getIntensity(i), injection.getIntensity(i + 1));
                        for (Particle p : ps) {
                            p.injectionPosition1D = (float) injection.getPosition1D();
                        }
                        allParticles.addAll(ps);
                    }

                }
            }
            injection.resetChanged();
        }
        if (surface != null) {
            surface.setNumberOfMaterials(maxMaterialID + 1);
        }
        if (scenario != null && scenario.getMeasurementsPipe() != null) {
            scenario.getMeasurementsPipe().setNumberOfMaterials(maxMaterialID + 1);
            if (scenario.getTimesPipe() != null && scenario.getTimesPipe() instanceof SparseTimeLinePipeContainer) {
                ((SparseTimeLinePipeContainer) scenario.getTimesPipe()).numberOfMaterials = maxMaterialID + 1;
            }
        }

        this.setParticles(allParticles);
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
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

    public Surface getSurface() {
        return surface;
    }

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
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
    }

    @Override
    public void simulationINIT(Object caller) {
    }

    @Override
    public void actionFired(Action action, Object source) {

    }

    @Override
    public void loadNetwork(Network network, Object caller) {
        //Repeater
        this.importNetwork(network);
        for (LoadingActionListener ll : actionListener) {
            ll.loadNetwork(network, caller);
        }
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
        this.surface = surface;
        if (surface != null) {
            if (surface.getMeasurementRaster() == null) {
                surface.setMeasurementRaster(new SurfaceMeasurementTriangleRaster(surface, 0, surface.getTimes(), threadController.getParticleThreads().length));
            }
        }
        for (LoadingActionListener ll : actionListener) {
            currentAction.description = "contrl. loadsurface inform " + ll;
            currentAction.progress = 0f;
            fireAction(currentAction);
            ll.loadSurface(surface, caller);
        }
    }

}
