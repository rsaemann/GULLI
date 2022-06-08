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

import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.ParticleListener;
import com.saemann.gulli.core.control.multievents.PipeResultData;
import com.saemann.gulli.core.control.particlecontrol.injection.ArealInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.ManholeInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.ParticleInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.PipeInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.SubArealInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.SurfaceInjection;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.HEAreaInflow1DInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionArealInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionInflowInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.control.scenario.injection.InjectionSubArealInformation;
import com.saemann.gulli.core.control.threads.ParticleThread;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.io.extran.HE_Database;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.particle.HistoryParticle;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseMeasurementContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLinePipeContainer;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.measurement.ParticleMeasurementSegment;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.Position3D;
import com.saemann.gulli.core.model.topology.measurement.ParticleMeasurementSection;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 * This class is the interlink between model and view.
 *
 * @author saemann
 */
public class Controller implements SimulationActionListener, LoadingActionListener {

    /**
     * The pipe network elements (Manholes, Pipes)
     */
    private Network network;
    /**
     * The surface topology (Triangle cell elements, street Inlets)
     */
    private Surface surface;
    /**
     * Stores information about the event time and the timestep size of the
     * timelines
     */
    private Scenario scenario;
    /**
     * Holds the timeline values for the network elements
     */
    private PipeResultData pipeResultData;

    private final ThreadController threadController;
    private final LoadingCoordinator loadingCoordinator;

    private final StoringCoordinator storingCoordinator;

    private final ArrayList<LoadingActionListener> actionListener = new ArrayList<>(2);
    private final ArrayList<ParticleListener> particleListener = new ArrayList<>(2);

    public static boolean verbose = true;

    public Action currentAction = new Action("", null, false);

    private boolean requestRecalculationOfInjections = false;

    /**
     * Every Xth particle becomes a HistoryParticle. 0=off
     */
    public int intervallHistoryParticles = 0;

    protected boolean traceParticles = false;

    protected int tracerParticleCount = 0;

    /**
     * Create all basic classes needed for the calculation. Automatically uses
     * the maximum number of cores for the parallel threads.
     */
    public Controller() {
        this(Runtime.getRuntime().availableProcessors());

    }

    /**
     * Create all basic classes needed for the calculation.
     *
     * @param numberOfThreads to be used for parallel particle computing
     */
    public Controller(int numberOfThreads) {
        if (numberOfThreads == 0) {
            numberOfThreads = Runtime.getRuntime().availableProcessors();
        } else if (numberOfThreads < 0) {
            numberOfThreads = Runtime.getRuntime().availableProcessors() + numberOfThreads;
        }
        threadController = new ThreadController(Math.max(1, numberOfThreads), this);

        threadController.addSimulationListener(this);
        loadingCoordinator = new LoadingCoordinator(this);
        storingCoordinator = new StoringCoordinator(this);
        addSimulationListener(storingCoordinator);
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

    /**
     * Import the loaded network and sitributes this information to all
     * simulation modules that need to know about
     *
     * @param newNetwork
     */
    protected void importNetwork(Network newNetwork) {
        currentAction.description = "import network";
        currentAction.startTime = System.currentTimeMillis();
        currentAction.hasProgress = false;
        currentAction.progress = 0;
        fireAction(currentAction);
        this.network = newNetwork;

        //Reference Injections, if Capacity was only referenced ba its name.
        for (InjectionInfo injection : loadingCoordinator.getInjections()) {
            if (injection.spillInManhole() && injection.getCapacity() == null) {
                if (injection instanceof InjectionInformation) {
                    Capacity c = network.getCapacityByName(((InjectionInformation) injection).getCapacityName());
                    injection.setCapacity(c);
                }
            }
        }
        currentAction.progress = 1;
        fireAction(currentAction);
    }

    /**
     * Manager for input and loading.
     *
     * @return
     */
    public LoadingCoordinator getLoadingCoordinator() {
        return loadingCoordinator;
    }

    /**
     * Manager for outputs
     *
     * @return
     */
    public StoringCoordinator getStoringCoordinator() {
        return storingCoordinator;
    }

//    public void setDispersionCoefficientPipe(double K) {
//        ParticlePipeComputing.setDispersionCoefficient(K);
//    }
    /**
     * Sets the dispersion coefficient to all particlesurfce computing objects
     * in their directD calculation array. The number and order of parameters
     * depends on the used Dispersion2D_Calculator.
     *
     * @param dispParameters
     */
    public void setDispersionCoefficientSurface(double[] dispParameters) {
        try {
            for (int i = 0; i <= scenario.getMaxMaterialID(); i++) {
                Material mat = scenario.getMaterialByIndex(i);
                if (mat != null) {
                    mat.getDispersionCalculatorSurface().setParameterValues(dispParameters);
                }
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
        requestRecalculationOfInjections = true;
        if (surface != null) {
            surface.setNumberOfMaterials(scenario.getMaxMaterialID() + 1);
        }
        threadController.setSimulationStartTime(sce.getStartTime());
        threadController.setSimulationTimeEnd(sce.getEndTime());

        if (sce.getStatusTimesPipe() != null && network != null) {
            currentAction.description = "load scenario: init measurement timelines";
            initMeasurementTimelines(sce, loadingCoordinator.sparsePipeMeasurements);
        }
        currentAction.description = "load scenario";
        currentAction.progress = 1;
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
    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, ParticleInjection injectionCapacityInformation, Material material, double starttimeAfterScenarioStart, double duration, double startIntensity, double endIntensity) {

        long scenarioStarttime = 0;
        if (scenario != null) {
            scenarioStarttime = scenario.getStartTime();
        }
        ArrayList<Particle> list = new ArrayList<>(numberOfParticles);
        if (duration < 0.1) {
            //Instant injection
            System.out.println("   instantInjection");
            for (int i = 0; i < numberOfParticles; i++) {
                Particle particle;
                long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart) * 1000L);
                if (traceParticles && intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                    tracerParticleCount++;
                } else {
                    particle = new Particle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                }
//                particle.setMaterial(material);
                particle.setWaiting();
                list.add(i, particle);
            }
            return list;
        }
        double s = (endIntensity - startIntensity) / duration;
        //if slope is 0 we can use the constant injection
        if (Math.abs(s) < 0.000001) {
            System.out.println("   constant Injection");
            return createParticlesOverTimespan(numberOfParticles, massPerParticle, injectionCapacityInformation, material, starttimeAfterScenarioStart, duration);
        }
        if (s > 0) {
            System.out.println("   gradient inc Injection");
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
                long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L);
                if (traceParticles && intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                    tracerParticleCount++;
                } else {
                    particle = new Particle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                }
                particle.setMaterial(material);
                particle.setWaiting();
                list.add(particle);
            }
        } else {
            System.out.println("   gradient decr. Injection");
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
                long injectiontime = (long) (scenarioStarttime + (starttimeAfterScenarioStart + t) * 1000L);
                if (traceParticles && intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    particle = new HistoryParticle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                    tracerParticleCount++;
                } else {
                    particle = new Particle(material, injectionCapacityInformation, (float) massPerParticle, injectiontime);
                }
                particle.setMaterial(material);
                particle.setWaiting();
                list.add(particle);
            }
        }

        return list;
    }

    private ArrayList<Particle> createParticlesOverTimespan(int numberOfParticles, double massPerParticle, ParticleInjection injectionCapacityInformation, Material material, double starttimeAfterScenarioStart, double duration) {
        long scenarioStarttime = 0;
        if (scenario != null) {
            scenarioStarttime = scenario.getStartTime();
        }
        scenarioStarttime += starttimeAfterScenarioStart * 1000;
        ArrayList<Particle> list = new ArrayList(numberOfParticles);
        {
//            double dt = duration / (Math.max(1, numberOfParticles - 1));
//            double t = 0;

            for (int i = 0; i < numberOfParticles; i++) {
                Particle p;
                long time = (long) (scenarioStarttime + (i / (double) numberOfParticles) * duration * 1000L);
                if (traceParticles && intervallHistoryParticles > 0 && i % intervallHistoryParticles == 0) {
                    p = new HistoryParticle(material, injectionCapacityInformation, (float) massPerParticle, time);
                    tracerParticleCount++;
                } else {
                    p = new Particle(material, injectionCapacityInformation, (float) massPerParticle, time);
                }
//                p.setMaterial(material);
                p.setWaiting();
                list.add(p);
//                t += dt;
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

        if (surface != null) {
            currentAction.description = "Reset scenario, Set # Materials";
            fireAction(currentAction);
            if (scenario != null) {
                surface.setNumberOfMaterials(scenario.getMaxMaterialID() + 1);
            }
            surface.reset();
        }

        if (network != null) {
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
            for (Manhole manhole : network.getManholes()) {
                manhole.passedMass = 0;
            }
        } else {
//            System.err.println("Network is not yet loaded. Cannot reset MeasurementTimelines of Pipes.");
        }

        currentAction.progress = 1f;
        fireAction(currentAction);
        currentAction.description = "";
    }

    /**
     * use information of this scenario to initialise the sampling interval. The
     * measuremnt timeline will have the exact same intervals as the pipes
     * timeline.
     *
     * @param scenario
     */
    public void initMeasurementTimelines(Scenario scenario, boolean sparse) {
        if (scenario.getStatusTimesPipe() != null) {
            if (scenario.getStatusTimesPipe() instanceof TimeContainer) {
                TimeContainer tc = (TimeContainer) scenario.getStatusTimesPipe();
                long duration = scenario.getEndtime() - scenario.getStartTime();
                int numberIntervals = (int) (duration / tc.getDeltaTimeMS());

                if (numberIntervals > 0) {
                    initMeasurementTimelines(scenario.getStartTime(), numberIntervals, tc.getDeltaTimeMS() / 1000., sparse);
                } else {
                    System.err.println("Time container Pipe not correctly initialised.");
                    System.err.println("   duration: " + duration + " / " + tc.getDeltaTimeMS() + " = " + numberIntervals);
                }
            } else {
                initMeasurementTimelines(scenario, scenario.getStatusTimesPipe().getNumberOfTimes() - 1, sparse);
            }
        } else {
            System.out.println("No reference times to use");
        }

    }

    public void initMeasurementTimelines(Scenario scenario, TimeIndexContainer times, int numberOfContaminants, boolean sparse) {

        if (sparse) {
            SparseMeasurementContainer container_m = new SparseMeasurementContainer(times, numberOfContaminants);
//            container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
            ArrayList<SparseTimeLineMeasurement> list = new ArrayList<>(network.getPipes().size());
            for (Pipe pipe : network.getPipes()) {
                SparseTimeLineMeasurement tlm = new SparseTimeLineMeasurement(container_m, pipe.getLength());
                pipe.setMeasurementTimeLine(tlm);
                list.add(tlm);
            }
            container_m.setTimelines(list);
            scenario.setMeasurementsPipe(container_m);

        } else {
            ArrayTimeLineMeasurementContainer container_m = ArrayTimeLineMeasurementContainer.init(times, network.getPipes().size(), numberOfContaminants);
            scenario.setMeasurementsPipe(container_m);
//        ArrayTimeLineMeasurementContainer.instance = container_m;
//            container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
            if (verbose) {
                System.out.println("Simulation step: " + ThreadController.getDeltaTime() + "s\t sampleinterval:" + container_m.getDeltaTimeS());// + " \t-> " + container_m.samplesPerTimeinterval + " samples per interval");
            }

            int number = 0;
            for (Pipe p : network.getPipes()) {
                p.setMeasurementTimeLine(new ArrayTimeLineMeasurement(container_m, number, p.getLength()));
                number++;
            }
        }

        if (surface != null && surface.getMeasurementRaster() != null) {
            scenario.setMeasurementsSurface(surface.getMeasurementRaster());
        }
    }

    public void initMeasurementTimelines(Scenario scenario, long[] times, int numberOfContaminants, boolean sparse) {

        if (sparse) {
            SparseMeasurementContainer container_m = new SparseMeasurementContainer(new TimeContainer(times), numberOfContaminants);
//            container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
            ArrayList<SparseTimeLineMeasurement> list = new ArrayList<>(network.getPipes().size());
            for (Pipe pipe : network.getPipes()) {
                SparseTimeLineMeasurement tlm = new SparseTimeLineMeasurement(container_m, pipe.getLength());
                pipe.setMeasurementTimeLine(tlm);
                list.add(tlm);
            }
            container_m.setTimelines(list);
            scenario.setMeasurementsPipe(container_m);

        } else {
            ArrayTimeLineMeasurementContainer container_m = ArrayTimeLineMeasurementContainer.init(times, network.getPipes().size(), numberOfContaminants);
            scenario.setMeasurementsPipe(container_m);
//        ArrayTimeLineMeasurementContainer.instance = container_m;
//            container_m.setSamplesPerTimeindex(container_m.getDeltaTimeS() / ThreadController.getDeltaTime());
            if (verbose) {
                System.out.println("Simulation step: " + ThreadController.getDeltaTime() + "s\t sampleinterval:" + container_m.getDeltaTimeS());// + " \t-> " + container_m.samplesPerTimeinterval + " samples per interval");
            }

            int number = 0;
            for (Pipe p : network.getPipes()) {
                p.setMeasurementTimeLine(new ArrayTimeLineMeasurement(container_m, number, p.getLength()));
                number++;
            }
        }
        if (surface != null && surface.getMeasurementRaster() != null) {
            scenario.setMeasurementsSurface(surface.getMeasurementRaster());
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
    public int initMeasurementsTimelinesBySeconds(Scenario scenario, double secondsPerInterval, boolean sparse) {
        int numberIntervals = (int) ((scenario.getEndTime() - scenario.getStartTime()) / (1000 * secondsPerInterval));
        initMeasurementTimelines(scenario, numberIntervals, sparse);
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
    public double initMeasurementTimelines(Scenario scenario, int numberOfIntervalls, boolean sparse) {

        int n = numberOfIntervalls;
        double dt = (scenario.getEndTime() - scenario.getStartTime()) / ((n));
        if (verbose) {
            System.out.println("sample dt= " + dt + "ms.  duration:" + (scenario.getEndTime() - scenario.getStartTime()));
        }
        long[] times = new long[n + 1];

        for (int i = 0; i < times.length; i++) {
            times[i] = this.scenario.getStartTime() + (long) (i * dt);
        }
        //count different types of contaminants
        int numberContaminantTypes = 1;
        try {
            for (InjectionInfo injection : scenario.getInjections()) {
                numberContaminantTypes = Math.max(numberContaminantTypes, injection.getMaterial().materialIndex + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMeasurementTimelines(scenario, times, numberContaminantTypes, sparse);
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
    public double initMeasurementTimelines(long scenarioStarttime, int numberOfIntervalls, double deltatimeSeconds, boolean sparse) {

        int n = numberOfIntervalls;
        double dt = deltatimeSeconds * 1000; //in MS
        long[] times = new long[n + 1];

        for (int i = 0; i < times.length; i++) {
            times[i] = scenarioStarttime + (long) (i * dt);
        }
        //count different types of contaminants
        int numberContaminantTypes = 1;
        try {
            for (InjectionInfo injection : scenario.getInjections()) {
                numberContaminantTypes = Math.max(numberContaminantTypes, injection.getMaterial().materialIndex + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initMeasurementTimelines(scenario, times, numberContaminantTypes, sparse);
        return dt;
    }

    /**
     * Tells the Threadcontroller to start the simulation.
     */
    public void start() {

        if (scenario == null) {
            throw new NullPointerException("No Scenario loaded.");
        }

        if (network != null && scenario.getMeasurementsPipe() == null) {
            System.out.println("Initialize sampling intervals with input interval length as no user defined sampling was set.");
            initMeasurementTimelines(scenario, loadingCoordinator.sparsePipeMeasurements);
        }

        if (requestRecalculationOfInjections) {
            recalculateInjections();
        }
        threadController.start();
    }

    /**
     * Before the next start, the injections must be recalculated. this can be
     * called when further editing of the injections is planned and performing
     * the real re-calculation call takes too much time.
     */
    public void requestRecalculationOfInjectionsBeforeNextStart() {
        requestRecalculationOfInjections = true;
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
//        long start = System.currentTimeMillis();

        int totalNumberParticles = 0;
        tracerParticleCount = 0;
        int maxMaterialID = -1;
        ArrayList<Material> indexedMaterials = new ArrayList<>();
        for (InjectionInfo injection : scenario.getInjections()) {
            totalNumberParticles += injection.getNumberOfParticles();
            if (injection.getMaterial() == null) {
                //Search for the correct material or create one
                for (Material material : scenario.getMaterials()) {
                    if (material.materialIndex == injection.getMaterialID()) {
                        injection.setMaterial(material);
                        break;
                    }
                }
            }
            if (injection.getMaterial() == null) {
                Material mat = new Material("neu " + (maxMaterialID + 1), 1000, true, maxMaterialID + 1);

                injection.setMaterial(mat);
                indexedMaterials.add(mat);
            }
            maxMaterialID = Math.max(maxMaterialID, injection.getMaterial().materialIndex);
            if (!indexedMaterials.contains(injection.getMaterial())) {
                indexedMaterials.add(injection.getMaterial());
            }
        }
        for (int i = 0; i < indexedMaterials.size(); i++) {
            indexedMaterials.get(i).materialIndex = i;
        }
        scenario.setMaterials(indexedMaterials);
        maxMaterialID = indexedMaterials.size() - 1;

//        System.out.println("Calculaing number of particles: " + totalNumberParticles + ", materials:" + (maxMaterialID + 1) + " took " + (System.currentTimeMillis() - start) + "ms. ");
        ArrayList<Particle> allParticles = new ArrayList<>(totalNumberParticles);
        int counter = 0;
        Particle.resetCounterID();
        for (InjectionInfo injection_ : scenario.getInjections()) {
            counter++;
            currentAction.hasProgress = true;
            currentAction.progress = counter / (float) scenario.getInjections().size();
            fireAction(currentAction);
            //find capacity
            Capacity c = null;
            int surfaceCell = -1;
            Position position = null;
            double pipeposition = 0;
            if (injection_ instanceof InjectionArealInformation) {
                if (surface != null) {
                    InjectionArealInformation injection = (InjectionArealInformation) injection_;
                    if (injection.getSurface() == null || injection.getSurface() != surface) {
                        injection.setCapacity(surface);
                    }
                    if (injection.isActive()) {
                        ArealInjection ai = new ArealInjection(surface);
                        ArrayList<Particle> particles = createParticlesOverTimespan(injection.getNumberOfParticles(), injection.getMass() / (double) injection.getNumberOfParticles(), ai, injection.getMaterial(), injection.getStarttimeSimulationsAfterSimulationStart(), injection.getDurationSeconds());
                        allParticles.addAll(particles);
                        ai.setParticleIDs(particles.get(0).getId(), particles.get(particles.size() - 1).getId());
                    }
                    injection.resetChanged();
                } else {
                    injection_.setActive(false);
                }
            } else if (injection_ instanceof HEAreaInflow1DInformation) {
//                System.out.println("Create particles of " + injection_);
                HEAreaInflow1DInformation injection = (HEAreaInflow1DInformation) injection_;

                if (injection.isActive()) {
                    if (!injection.isInitilized()) {
                        if (network != null) {
                            injection.network = network;
                        }
                        HE_Database he = loadingCoordinator.requestHE_ResultDatabase();
                        if (he != null) {
                            try {
                                    injection.areaRunoffSplit = he.readRunoffSplit(null,null);
                            } catch (SQLException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (Exception ex) {

                            }
                            try {
                                    injection.setPrecipitation(he.readRegenreihe());
                            } catch (SQLException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (Exception ex) {

                            }
                            he.verbose = false;
                            if (injection.effectiveRunoffVolume != null) {
                                injection.calculateManholesArea();
                            }
                        }
                    }else{
//                        System.out.println("is initialized");
                    }

                    ArrayList<Particle> particles = injection.createParticles();
                    float mass=0;
                    for (Particle particle : particles) {
                        mass+=particle.getParticleMass();
                    }
                    System.out.println("Created " + particles.size() + " particles for washoff '"+injection.runoffParameterName+"' with "+mass+"kg");
//                        ArealInjection ai = new ArealInjection(surface);
//                        ArrayList<Particle> particles = createParticlesOverTimespan(injection.getNumberOfParticles(), injection.getMass() / (double) injection.getNumberOfParticles(), ai, injection.getMaterial(), injection.getStarttimeSimulationsAfterSimulationStart(), injection.getDurationSeconds());
                    allParticles.addAll(particles);
//                        ai.setParticleIDs(particles.get(0).getId(), particles.get(particles.size() - 1).getId());
                }
                injection.resetChanged();

            } else if (injection_ instanceof InjectionSubArealInformation) {
                if (surface != null) {
                    InjectionSubArealInformation injection = (InjectionSubArealInformation) injection_;
                    if (injection.getSurface() == null || injection.getSurface() != surface) {
                        injection.setCapacity(surface);
                    }
                    if (injection.isActive() && injection.getCellIDs() != null) {
                        SubArealInjection ai = new SubArealInjection(surface, injection.getCellIDs());
                        ArrayList<Particle> particles = createParticlesOverTimespan(injection.getNumberOfParticles(), injection.getMass() / (double) injection.getNumberOfParticles(), ai, injection.getMaterial(), injection.getStarttimeSimulationsAfterSimulationStart(), injection.getDurationSeconds());
                        allParticles.addAll(particles);
                        ai.setParticleIDs(particles.get(0).getId(), particles.get(particles.size() - 1).getId());
                    }
                    injection.resetChanged();
                } else {
                    injection_.setActive(false);
                }
            } else if (injection_ instanceof InjectionInflowInformation) {
                if (injection_.isActive()) {
                    InjectionInflowInformation injection = (InjectionInflowInformation) injection_;
                    if (injection.getNetwork() != network) {
                        //Need to update the injections for a new network
                        injection.setNetwork(network);
                    }

                    for (int i = 0; i < injection.getManholes().length; i++) {
                        Manhole manhole = injection.getManholes()[i];
                        float volume = injection.getVolumePerManhole()[i];
                        if (volume < 0.001) {
                            continue;
                        }
                        double mass = volume * injection.getConcentration();
                        int particles = (int) (injection.getNumberOfParticles() * volume / injection.getTotalvolume());
                        particles = Math.max(particles, 1);
                        ManholeInjection mi = new ManholeInjection(manhole);
                        ArrayList<Particle> ps = createParticlesOverTimespan(particles, mass / (double) particles, mi, injection.getMaterial(), injection.getStarttimeSimulationsAfterSimulationStart(), injection.getDurationSeconds());
                        allParticles.addAll(ps);
                    }
                    injection.resetChanged();
                }

            } else if (injection_ instanceof InjectionInformation) {
//                System.out.println("  Injectioninformation");
                InjectionInformation injection = (InjectionInformation) injection_;
                if (injection.spillOnSurface()) {
                    if (getSurface() == null) {
                        continue;
                    }
                    c = getSurface();
                    injection.setCapacity(c);
                    if (injection.getPosition() != null) {
                        Coordinate utm;
                        try {
                            utm = getSurface().getGeotools().toUTM(injection.getPosition());
                            position = new Position(injection.getPosition().getLongitude(), injection.getPosition().getLatitude(), utm.x, utm.y);
//                            System.out.println("   neue Position erzeugt");
                        } catch (TransformException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    if (injection.getCapacityID() >= 0) {
                        surfaceCell = injection.getCapacityID();
//                        System.out.println("      existing cell by id ");
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
//                            System.out.println("   search for surface cell");
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
//                            System.out.println("      new position form triangle mid");
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
//                    }
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
                            } else {
                                injection.setCapacity(c);
                            }

                        }
                        if (c == null && injection.getPosition() != null) {
                            c = getNetwork().getManholeNearPositionLatLon(injection.getPosition());
                            injection.setCapacity(c);
                        }
                        if (c == null && injection.getCapacityID() >= 0) {
                            c = getNetwork().getManholeByManualID(injection.getCapacityID());
                            if (c != null) {
                                injection.setCapacity(c);
                            }
                        }
                    }
                    if (c == null) {
                        System.err.println("Cannot find Capacity for injection " + injection);
                        continue;
                    } else {
                        injection.setCapacityName(c.getName());
                    }
                }
                ///////////////////////////////////////////////////////////////
                //Create particles over time

                if (injection.isActive()) {
//                    System.out.println("  create particles");
//                    long startcreation = System.currentTimeMillis();
                    Position3D injectionposition = null;
                    if (injection.spillOnSurface() && injection.getPosition() != null) {
                        if (injection.getPosition() instanceof Position) {
                            Position pos = (Position) injection.getPosition();
                            if (!(pos.getLongitude() != 0 ^ pos.x != 0)) {
                                // utm does not match the latlon coordinates. 
                                if (pos.getLongitude() == 0) {
                                    //only utm coordinates are given -> OK
                                    injectionposition = new Position3D(pos);
                                }

                            }
                        }
                        if (injectionposition == null) {
                            //transform to utm
                            try {
                                //only latlong is set, need to convert to utm
                                Coordinate utm = surface.getGeotools().toUTM(injection.getPosition());
                                injectionposition = new Position3D(injection.getPosition().getLongitude(), injection.getPosition().getLatitude(), utm.x, utm.y, utm.z);
                            } catch (TransformException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        injection.setPosition(injectionposition);
                    }
                    ParticleInjection pi = null;
                    if (injection.spillOnSurface()) {
                        SurfaceInjection si = new SurfaceInjection(surface, surfaceCell);
                        pi = si;
                    } else if (injection.spillInManhole()) {
                        if (injection.getCapacity() instanceof Manhole) {
                            ManholeInjection mhi = new ManholeInjection((Manhole) injection.getCapacity());
                            pi = mhi;
                        } else if (injection.getCapacity() instanceof Pipe) {
                            PipeInjection ppi = new PipeInjection((Pipe) injection.getCapacity(), injection.getPosition1D());
                            pi = ppi;
                        }
                    }
                    if (pi == null) {
                        System.err.println("Do not know how to inject " + injection);
                        continue;
                    }
                    for (int i = 0; i < injection.getNumberOfIntervals(); i++) {
                        if (i == injection.getNumberOfIntervals() - 1) {
                            //last one constant
//                            long startconstant = System.currentTimeMillis();
                            ArrayList<Particle> ps = createParticlesOverTimespan(injection.particlesInInterval(i), injection.massInInterval(i) / (double) injection.particlesInInterval(i), pi, injection.getMaterial(), injection.getIntervalStart(i), injection.getIntervalDuration(i), injection.getIntensity(i), injection.getIntensity(i));
//                            System.out.println("Creating particle objects constant: " + (System.currentTimeMillis() - startconstant) + "ms.");
                            allParticles.addAll(ps);
//                            System.out.println("Creating particle objects constant and addtolist: " + (System.currentTimeMillis() - startconstant) + "ms.");
                        } else {
                            ArrayList<Particle> ps = createParticlesOverTimespan(injection.particlesInInterval(i), injection.massInInterval(i) / (double) injection.particlesInInterval(i), pi, injection.getMaterial(), injection.getIntervalStart(i), injection.getIntervalDuration(i), injection.getIntensity(i), injection.getIntensity(i + 1));
                            allParticles.addAll(ps);
                        }

                    }
//                    System.out.println("Create particles for injection in " + (System.currentTimeMillis() - startcreation));
                }
                injection.resetChanged();
            }
        }
        if (surface != null) {
//            System.out.println(" set number of particles to surface");
            surface.setNumberOfMaterials(maxMaterialID + 1);
        }
        if (scenario != null && scenario.getMeasurementsPipe() != null) {
            scenario.getMeasurementsPipe().setNumberOfMaterials(maxMaterialID + 1);
            if (scenario.getStatusTimesPipe() != null && scenario.getStatusTimesPipe() instanceof SparseTimeLinePipeContainer) {
                ((SparseTimeLinePipeContainer) scenario.getStatusTimesPipe()).numberOfMaterials = maxMaterialID + 1;
            }
        }
//        System.out.println(" sort particles");
//        long startsort = System.currentTimeMillis();
        //Order PArticles by insertion time
        allParticles.sort(new Comparator<Particle>() {
            @Override
            public int compare(Particle o1, Particle o2) {
                if (o1.getInsertionTime() == o2.getInsertionTime()) {
                    return 0;
                }
                if (o1.getInsertionTime() > o2.getInsertionTime()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
//        System.out.println(" sorting took " + (System.currentTimeMillis() - startsort) + "ms");
//        System.out.println("Recalculating particles took " + ((System.currentTimeMillis() - start)) + "ms");

        this.setParticles(allParticles);
        currentAction.description = "Injections recalculated";
        currentAction.hasProgress = true;
        currentAction.progress = 1;
        fireAction(currentAction);
//        System.out.println("Informing about recalculation finished. total: " + ((System.currentTimeMillis() - start)) + "ms");
    }

    @Override

    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
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
            if (surface.getMeasurementRaster() == null && surface.getTimes() != null) {
                surface.setMeasurementRaster(new SurfaceMeasurementTriangleRaster(surface, 0, surface.getTimes(), threadController.getNumberOfParallelThreads()));
            }
        }
        for (LoadingActionListener ll : actionListener) {
            currentAction.description = "contrl. loadsurface inform " + ll;
            currentAction.progress = 0f;
            fireAction(currentAction);
            ll.loadSurface(surface, caller);
        }
    }

    public void setPipeResultData(PipeResultData data) {
        this.pipeResultData = data;
    }

    public PipeResultData getPipeResultData() {
        return this.pipeResultData;
    }

    public boolean isTraceParticles() {
        return traceParticles;
    }

    public void setTraceParticles(boolean traceParticles) {
        if (traceParticles == this.traceParticles) {
            return;
        }
        this.traceParticles = traceParticles;
        requestRecalculationOfInjections = true;

    }

    public void setHistoryParticleInterval(int interval, boolean active) {
        this.intervallHistoryParticles = interval;
        if (this.traceParticles == false && active == false) {
            return;
        }
        this.traceParticles = true;
        requestRecalculationOfInjections = true;
    }

    /**
     * Total number of tracked particles (Historyparticle)
     *
     * @return
     */
    public int getNumberTracerParticles() {
        return tracerParticleCount;
    }

}
