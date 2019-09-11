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
package control.threads;

import control.Action.Action;
import control.Controller;
import control.listener.LoadingActionListener;
import control.listener.SimulationActionListener;
import control.listener.ParticleListener;
import control.scenario.injection.InjectionInformation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.Particle;
import model.surface.Surface;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.topology.Network;
import model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class ThreadController implements ParticleListener, SimulationActionListener, LoadingActionListener {

    protected Controller control;
//    public final MultiThreadBarrier<HydrodynamicsThread> barrier_hydrodynamics;
    public final MultiThreadBarrier<ParticleThread> barrier_particle;
    public final SingleThreadBarrier<LocationThread> barrier_positionUpdate;
    public final MultiThreadBarrier<SynchronizationThread> barrier_sync;
//    private final SynchronizationThread syncThread;

    private final ArrayList<SimulationActionListener> listener = new ArrayList<>(2);

    private boolean verbose_threadstatus = false;

    /**
     * difference between the start of the Day (0:00) and the Beginn of Scenario
     * Used to transform the Day time Back to Simulationtime of the scenario.
     */
    private long startOffset;

    public static enum BARRIERS {

        HYDRODYNAMICS, PARTICLE, SYNC, POSITION
    }
    private int numberParallelParticleThreads = 2;
    private int numberParallelSyncThreads = 2;
    private boolean run = false;
    private boolean initialized = false;
//    private MapViewer mapViewer;
//    private long loopstartTime;
    private static double deltaTime = 0.1;//seconds
    private int steps = 0;

    public boolean paintOnMap = true;
    public int paintingInterval = 250;
    public int surfaceupdateInterval = 300;
    public int videoFrameInterval = 100;
//    private int hydrodynamicsInterval = 1000000;

    private final long[] calculationTimeHistory = new long[10];
//    private int calculationtimeIndex = 0;
    private long calculationLoopStarttime;
    private long calculationStartTime;
    private long calculationTimeElapsed = 0;
    private double averageCalculationTime;
    private boolean calculationFinished = false;
    private static long simulationTimeMS = 0; //ms
    private long simulationTimeStart = 0;
    private long simulationTimeEnd = Long.MAX_VALUE;

    /**
     * Pauses the Simulation when all particles reached an outlet manhole.
     */
    public boolean pauseWhenParticlesDone = true;

    /**
     * Flag to see if the {@link pauseWhenParticlesDone} cause has been reached.
     * If it is set to true the simulation will no longer check if the particles
     * have reached an outlet manhole and pause the simulation.
     */
    private boolean particlesReachedOutlet = false;

//    public AnimatedGIFencoder videoGIFencoder;
//    public float framerateGIF = 12;
//    public boolean video = false;
    public boolean useTimelineInput = true;
//    private CapacityTimelinePanel timelinePanel;
//    private SpacelinePanel spacelinePanel;

    private ControlThread controlThread;

    private Object calledObject = null;

    private Thread statusThread = initStatusThread();

    public long seed = 100;

    public ThreadController(int threads, final Controller control) {
        this.control = control;
        this.control.addParticleListener(this);
        this.control.addActioListener(this);
        numberParallelParticleThreads = threads;
        //Start n threads 
        barrier_particle = new MultiThreadBarrier("ParticleBarrier", this);
        for (int i = 0; i < numberParallelParticleThreads; i++) {
            ParticleThread pt = new ParticleThread("ParticleThread[" + i + "]", seed + i, barrier_particle);
            pt.setDeltaTime(deltaTime);
            barrier_particle.addThread(pt);
        }
        barrier_particle.initialize();
        barrier_positionUpdate = new SingleThreadBarrier("LocationBarrier", this);
//        for (int i = 0; i < numberParallelThreads; i++) {
        LocationThread put = new LocationThread("LocationThread", barrier_positionUpdate);
        barrier_positionUpdate.setThread(put);
//            barrier_positionUpdate.addThread(put);
//        }
        barrier_positionUpdate.initialize();
        barrier_sync = new MultiThreadBarrier<>("SyncBarrier", this);
        for (int i = 0; i < numberParallelSyncThreads; i++) {
            SynchronizationThread syncThread = new SynchronizationThread("SyncThread[" + i + "]", barrier_sync, control);
            barrier_sync.addThread(syncThread);
        }

        barrier_sync.initialize();

        controlThread = new ControlThread();
        controlThread.start();
    }

    public void start() {
        run = true;
        calculationFinished = false;
        calculationStartTime = System.currentTimeMillis();

        initialize();
        calculationFinished = false;

        Pipe[] fullarray = control.getNetwork().getPipes().toArray(new Pipe[control.getNetwork().getPipes().size()]);
        int fromIndex = 0;
        int numberofPipes = fullarray.length / barrier_sync.getThreads().size();
        for (int i = 0; i < barrier_sync.getThreads().size(); i++) {
            SynchronizationThread st = barrier_sync.getThreads().get(i);
            st.allFinished = false;
            st.setPipes(Arrays.copyOfRange(fullarray, fromIndex, Math.min(fullarray.length, fromIndex + numberofPipes)));
            fromIndex += numberofPipes + 1;
        }

        barrier_sync.setSimulationtime(simulationTimeMS);
        barrier_particle.setSimulationtime(simulationTimeMS);
        if (ArrayTimeLineMeasurementContainer.instance != null) {
            ArrayTimeLineMeasurementContainer.instance.setActualTime(simulationTimeMS);
        }
        if (control.getScenario() != null) {
            control.getScenario().setActualTime(simulationTimeMS);
        }

        calculationLoopStarttime = System.currentTimeMillis();
        calledObject = barrier_sync;
        barrier_sync.startover();
    }

    public void stop() {
        run = false;
    }

    /**
     * seed for each ParticleThread is shifted by +1
     *
     * @param seed
     */
    public void setSeed(long seed) {
//        System.out.println(getClass()+": setSeed("+seed+")");
        this.seed = seed;
        for (int i = 0; i < numberParallelParticleThreads; i++) {
            barrier_particle.getThreads().get(i).setSeed(seed + i);
        }
    }

    public static long getSimulationTimeMS() {
        return simulationTimeMS;
    }

    public void finishedLoop(ThreadBarrier barrier) {
        synchronized (this) {
            if (!run) {
                return;
            }
//            int state = 0;
            if (true) {
                if (barrier == barrier_particle) {
//                    state = 2;
                    controlThread.setBarrier(BARRIERS.PARTICLE);
                    return;
                } else if (barrier == barrier_sync) {
                    controlThread.setBarrier(BARRIERS.SYNC);
                    return;
                } else {
                    controlThread.setBarrier(BARRIERS.POSITION);
                }
                return;
            }
        }

    }

    public void updatePositions() {
        if (!barrier_positionUpdate.isinitialized) {
            return;
        }
        calledObject = barrier_sync;
        barrier_positionUpdate.startover();
    }

    public void initializingFinished(ThreadBarrier barrier) {
        synchronized (this) {
            if (verbose_threadstatus) {
                System.out.println("ThreadController says: " + barrier.name + " initialized");
                System.out.println("Particles initialized=" + barrier_particle.isinitialized);
                System.out.println("Locations initialized=" + barrier_positionUpdate.isinitialized);
            }
            if (barrier_particle != null && barrier_particle.isinitialized && barrier_positionUpdate != null && barrier_positionUpdate.isinitialized) {
                initialized = true;
                if (verbose_threadstatus) {
                    System.out.println("ThreadController initialized: " + this.initialized);
                }
                startParticles();
            }
        }
    }

    protected void startParticles() {
        //Only start the next loop, if the controller allows this action
        calculationLoopStarttime = System.currentTimeMillis();
        if (run) {
            new Thread() {
                @Override
                public void run() {
                    calledObject = barrier_sync;
                    barrier_sync.startover();
                }
            }.start();
        }
    }

    public void setDeltaTime(double deltaTimeSeconds) {
        if (run) {
            throw new SecurityException("Can not change the delta time interval while Threads are running, inconsistency warning!");
        }
        ThreadController.deltaTime = deltaTimeSeconds;
        if (ArrayTimeLineMeasurementContainer.isInitialized()) {
            ArrayTimeLineMeasurementContainer.instance.messungenProZeitschritt = ArrayTimeLineMeasurementContainer.instance.getDeltaTime() / ThreadController.getDeltaTime();
        }
        for (ParticleThread thread : barrier_particle.getThreads()) {
            thread.setDeltaTime(deltaTimeSeconds);
        }
    }

    /**
     * Assign particles to particle Threads.
     *
     * @param particles
     */
    private void setParticles(Collection<Particle> particles) {
        //Divide the List of Particles in equal pices and fill them into the Threads.
        int threadCount = barrier_particle.getThreads().size();

        ArrayList<Particle>[] threadlists = new ArrayList[threadCount];
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadlists.length; i++) {
            threadlists[i] = new ArrayList<>(particles.size() / threadCount + 1);
        }
        int counter = 0;
        for (Particle particle : particles) {
            threadlists[counter % threadCount].add(particle);
            counter++;
        }

        for (int i = 0; i < threadlists.length; i++) {
            barrier_particle.getThreads().get(i).clearParticles();
            barrier_particle.getThreads().get(i).addParticles(threadlists[i]);
        }
        barrier_positionUpdate.getThread().setParticles(particles.toArray(new Particle[particles.size()]));
    }

    public double getAverageCalculationTime() {
        long sum = 0;
        for (int i = 0; i < calculationTimeHistory.length; i++) {
            sum += calculationTimeHistory[i];
        }
        averageCalculationTime = sum / (double) calculationTimeHistory.length;
        return averageCalculationTime;
    }

    /**
     *
     * @return dt in seconds
     */
    public static double getDeltaTime() {
        return deltaTime;
    }

    /**
     * Milliseconds of simulationtime.
     *
     * @return
     */
    public long getSimulationTime() {
        return simulationTimeMS;
    }

    public int getSteps() {
        return steps;
    }

    public void step() {
        run = false;
        initialize();
        calculationLoopStarttime = System.currentTimeMillis();
        calledObject = barrier_sync;
        barrier_sync.startover();
    }

    public void reset() {
        run = false;
        simulationTimeMS = simulationTimeStart;
        initialize();
        barrier_particle.setSimulationtime(simulationTimeMS);
        if (control.getScenario() != null) {
            //Reset Timelines in scenarios:
            control.getScenario().setActualTime(simulationTimeMS);
            //See if particles need to be added differently
            if (control.getScenario().getInjections() != null) {
                boolean needRecalculation = false;
                for (InjectionInformation injection : control.getScenario().getInjections()) {
                    if (injection.isChanged()) {
                        needRecalculation = true;
                        break;
                    }
                }
                if (needRecalculation) {
                    control.recalculateInjections();
                    if (control.getSurface() != null) {
                        control.getSurface().setNumberOfMaterials(control.getScenario().getMaxMaterialID() + 1);
                    }
                }
                for (InjectionInformation injection : control.getScenario().getInjections()) {
                    injection.resetChanged();
                }
            }
        }
        for (ParticleThread thread : barrier_particle.getThreads()) {
            thread.reset();
        }
        if (control.getSurface() != null) {
            control.getSurface().reset();
        }
        // Reset calculation of particlepositions.
        barrier_positionUpdate.getThread().updateParticlePositions();
        calculationTimeElapsed = 0;
        steps = 0;

        for (SimulationActionListener l : listener) {
            l.simulationRESET(this);
        }
    }

    /**
     * Break locks of hanging Threads to reenable correct working of simulation.
     */
    public void breakBarrierLocks() {
        //break locks of threadbarriers
        barrier_positionUpdate.startover();
        barrier_particle.startover();
        barrier_sync.startover();

    }

    private void initialize() {
        if (!initialized) {
            barrier_particle.initialize();
            barrier_positionUpdate.initialize();
            barrier_sync.initialize();
            return;
        }
    }

    public void setSimulationStartTime(long simulationTimeStart) {
        this.simulationTimeStart = simulationTimeStart;
        simulationTimeMS = simulationTimeStart;
        barrier_particle.setSimulationtime(simulationTimeMS);

        Date d3 = new Date(simulationTimeStart);
        GregorianCalendar dnorm = new GregorianCalendar();
        dnorm.setTimeInMillis(d3.getTime());
        dnorm.set(Calendar.MILLISECOND, 0);
        dnorm.set(Calendar.SECOND, 0);
        dnorm.set(Calendar.MINUTE, 0);
        dnorm.set(Calendar.HOUR_OF_DAY, 0);
        startOffset = simulationTimeStart; //d3.getTime() - dnorm.getTimeInMillis();
    }

    public long getSimulationStartTime() {
        return this.simulationTimeStart;
    }

    public void setSimulationTimeEnd(long simulationTimeEnd) {
        this.simulationTimeEnd = simulationTimeEnd;
    }

    public long getSimulationTimeEnd() {
        return simulationTimeEnd;
    }

    public long getElapsedCalculationTime() {
        return calculationTimeElapsed;
    }

    public ParticleThread[] getParticleThreads() {
        ParticleThread[] a = new ParticleThread[numberParallelParticleThreads];
        int i = 0;
        for (ParticleThread thread : barrier_particle.getThreads()) {
            a[i] = thread;
            i++;
        }
        return a;
    }

    public boolean isSimulating() {
        return run;
    }

    public class ControlThread extends Thread {

        BARRIERS barrier;
        private final String monitor = "LA";

        @Override
        public void run() {
            while (true) {

                finishedLoop(barrier);
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        public void setBarrier(BARRIERS barrier) {
            this.barrier = barrier;
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        private void finishedLoop(BARRIERS finishedBarrier) {
            if (!run) {
                return;
            }
            switch (finishedBarrier) {
                case PARTICLE:
                    /*@todo move particle thread after synchr.-thread. */
                    barrier_sync.setSimulationtime(simulationTimeMS);
                    calledObject = barrier_sync;
                    barrier_sync.startover();
                    return;
                case SYNC:

                    long calcStepTime = System.currentTimeMillis() - calculationLoopStarttime;
                    calculationTimeHistory[steps % calculationTimeHistory.length] = calcStepTime;

                    calculationTimeElapsed += calcStepTime;
                    calculationLoopStarttime = System.currentTimeMillis();
                    steps++;
                    simulationTimeMS += deltaTime * 1000;

                    if (simulationTimeMS > simulationTimeEnd) {
                        System.out.println("Simulation time end reached!");
                        calculationFinished = true;
//                        for (SimulationActionListener l : listener) {
//                            l.simulationFINISH(true, particlesReachedOutlet);
//                        }
                    }

                    //Send new Timeinformation to all timelines pipe/manhole/surface/soil
                    control.getScenario().setActualTime(simulationTimeMS);
                    if (ArrayTimeLineMeasurementContainer.instance != null) {
                        ArrayTimeLineMeasurementContainer.instance.setActualTime(simulationTimeMS);
                    }

                    particlesReachedOutlet = true;
                    for (ParticleThread thread : barrier_particle.getThreads()) {
                        if (!thread.allParticlesReachedOutlet) {
                            particlesReachedOutlet = false;
                            break;
                        }
                    }
                    if (particlesReachedOutlet) {
                        System.out.println("SYNC :: All particles reached outlet");
                        calculationFinished = true;
//                        System.out.println("Stopped after " + (System.currentTimeMillis() - calculationStartTime) / 1000 + "sec computation time. \telapsed calculation time=" + calculationTimeElapsed + "ms");
//                        for (SimulationActionListener l : listener) {
//                            l.simulationFINISH(simulationTimeMS >= simulationTimeEnd, particlesReachedOutlet);
//                        }
                    }
                    if (steps % paintingInterval == 0) {
                        barrier_positionUpdate.setSimulationtime(simulationTimeMS);
                        calledObject = barrier_positionUpdate;
                        barrier_positionUpdate.startover();
                        return;
                    }
                case POSITION:
                    for (SimulationActionListener l : listener) {
                        l.simulationSTEPFINISH(steps, this);
                    }

                    if (calculationFinished) {
                        run = false;
                        System.out.println("Stopped after " + (System.currentTimeMillis() - calculationStartTime) / 1000 + "sec computation time.\telapsed calculation time=" + calculationTimeElapsed + "ms");

                        for (SimulationActionListener l : listener) {
                            l.simulationFINISH(simulationTimeMS >= simulationTimeEnd, particlesReachedOutlet);
                        }
                        return;
                    }

                case HYDRODYNAMICS:
                    barrier_particle.setSimulationtime(simulationTimeMS);
                    calledObject = barrier_particle;
                    barrier_particle.startover();
                    return;
                default:
                    break;
            }
            System.out.println("ThreadController.finishedLoop(): this should never be reached.");
        }

    }

    /**
     * Cleans all Threads from Particles of an old simulation network/scenario
     */
    public void cleanFromParticles() {
        for (ParticleThread thread : barrier_particle.getThreads()) {
            thread.clearParticles();
            thread.reset();
        }

    }

    public long getStartOffset() {
        return startOffset;
    }

    public int getNumberOfActiveParticles() {
        int sum = 0;
        for (ParticleThread thread : barrier_particle.getThreads()) {
            sum += thread.getNumberOfActiveParticles();
        }
        return sum;
    }

    public int getNumberOfTotalParticles() {
        int sumT = 0;
        for (ParticleThread thread : barrier_particle.getThreads()) {
            sumT += thread.getNumberOfTotalParticles();
        }
        return sumT;
    }

    public void addSimulationListener(SimulationActionListener listen) {
        this.listener.add(listen);
    }

    public boolean removeSimulationListener(SimulationActionListener listen) {
        return this.listener.remove(listen);
    }

//    public void setSeed(long seed) {
//        for (ParticleThread t : barrier_particle.getThreads()) {
//            t.pc.randDist.setRandomGenerator(new Random(seed+));
//        }
//    }
    public Thread initStatusThread() {
        final ThreadController tc = this;

        statusThread = new Thread("StatusThreadController") {
            private int laststep = -1;

            @Override
            public void run() {
                while (true) {
                    try {
                        if (tc.barrier_particle != null) {
                            int running = 0, waiting = 0;
                            if (run && steps == laststep) {
                                // something is incredibly slow. prepare output to console
                                StringBuilder str = new StringBuilder("--" + getClass() + "--detected hanging at loop " + steps + "  barrier: " + (calledObject) + "   :");
                                for (ParticleThread pt : tc.getParticleThreads()) {
                                    if (pt != null) {
                                        str.append("\n ");
                                        str.append(pt.getClass().getSimpleName() + " " + pt.getState() + ":" + (pt.isActive() ? "calculating" : "waiting") + " status:" + pt.status + "  Particles waiting:" + pt.numberOfWaitingParticles + " active:" + pt.numberOfActiveParticles + "  completed:" + pt.numberOfCompletedParticles);
                                        if (pt.particle != null) {
                                            str.append("   Particle: " + pt.particleID + " in " + pt.particle.getSurrounding_actual() + " : status=" + pt.particle.status);
                                        }
                                        if (pt.getSurfaceComputing() != null) {
                                            str.append("    surfComputing: status=" + pt.getSurfaceComputing().reportCalculationStatus());
                                        }
//                                        System.out.println(pt.getName() + ": " + pt.getState() + "  " + pt.particle + " p.st:" + pt.particle.status + "\t pipeC: particleID" + pt.particleID + " surfC: " + pt.surfcomp.status + "\t Surface: " + control.getSurface().status + " v2nb:" + control.getSurface().vstatus);

                                    }
                                }

                                System.out.println(str.toString());
                                breakBarrierLocks();
                            }
                            laststep = steps;
                        }
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

        };
        statusThread.start();
        return statusThread;
    }

    @Override
    public void setParticles(Collection<Particle> particles, Object source) {
        this.setParticles(particles);
    }

    @Override
    public void clearParticles(Object source) {
        this.cleanFromParticles();
    }

    @Override
    public void simulationINIT(Object caller) {
        this.initialize();
    }

    @Override
    public void simulationSTART(Object caller) {
        this.start();
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {

    }

    @Override
    public void simulationPAUSED(Object caller) {

    }

    @Override
    public void simulationRESUMPTION(Object caller) {
        this.start();
    }

    @Override
    public void simulationSTOP(Object caller) {
        this.stop();
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
    }

    @Override
    public void simulationRESET(Object caller) {
        this.reset();
    }

    @Override
    public void actionFired(Action action, Object source) {

    }

    @Override
    public void loadNetwork(Network network, Object caller) {

    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
        for (ParticleThread pt : barrier_particle.getThreads()) {
            pt.setSurface(surface);
        }
    }
}
