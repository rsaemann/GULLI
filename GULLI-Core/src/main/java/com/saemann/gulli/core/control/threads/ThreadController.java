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
package com.saemann.gulli.core.control.threads;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.listener.ParticleListener;
import com.saemann.gulli.core.control.maths.RandomGenerator;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.particle.HistoryParticle;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class ThreadController implements ParticleListener, SimulationActionListener, LoadingActionListener {

    protected Controller control;
    public final MultiThreadBarrier<ParticleThread> barrier_particle;
//    public final SingleThreadBarrier<LocationThread> barrier_positionUpdate;
    public final SingleThreadBarrier<SynchronizationThreadPipe> barrier_sync;
    public final SynchronizationThreadPipe syncThread_pipes;

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
//    private int numberParallelParticleThreads;
//    private int numberParallelSyncThreads = 2;
    private boolean run = false;
    private boolean initialized = false;
    private static double deltaTime = 1;//seconds
    private static int deltatimeMS = (int) (deltaTime * 1000);
    private int steps = 0;

    public boolean paintOnMap = true;
    public int paintingInterval = 250;
//    public int surfaceupdateInterval = 300;
    public int videoFrameInterval = 100;

    private final long[] calculationTimeHistory = new long[10];

    private long calculationLoopStarttime;
    private long calculationStartTime;
    private long calculationTimeElapsed = 0;
    private double averageCalculationTime;
    private boolean calculationFinished = false;
    private static long simulationTimeMS = 0; //ms
    private long simulationNextTimeMS = 0;
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

    public boolean useTimelineInput = true;

    private ControlThread controlThread;

    private Object calledObject = null;

    private ThreadBarrier lastFinishedBarrier = null;

    private Thread statusThread = initStatusThread();

    public static boolean pauseRevokerThread = false;

    private Thread lockbreaker;

    private long seed = 100;

    ////
    //everything to do if the particles are stored here centralised
    protected Particle[] particles;
    /**
     * Generating Random numbers must always happen for the same particles.
     */
    protected RandomGenerator[] randomNumberGenerators;
    private ReentrantLock lock = new ReentrantLock();
    //number of particles to be treted by one thread
    protected int treatblocksize = 1000;
    public int waitingParticleIndex = 0;//first waiting (for injection)  particle index
    public int nextParticleBlockStartIndex = 0;//Index for the tretstart for the next requesting Thread
    public int nextRandomNumberBlockStartIndex = 0;//Index for the random number for the next requesting Thread

    public ThreadController(int numberParticleThreads, final Controller control) {
        this.control = control;
        this.control.addParticleListener(this);
        this.control.addActioListener(this);

        //initialize number of threads on the surface 
        if (control.getSurface() != null && control.getSurface().getMeasurementRaster() != null) {
            control.getSurface().getMeasurementRaster().setNumberOfThreads(numberParticleThreads);
//            control.getSurface().getMeasurementRaster().monitor = new TriangleMeasurement[numberParticleThreads];
//            control.getSurface().getMeasurementRaster().statuse = new int[numberParticleThreads];

        }
//        numberParallelParticleThreads = threads;
        //Start n threads 
        barrier_particle = new MultiThreadBarrier("ParticleBarrier", this);
        for (int i = 0; i < numberParticleThreads; i++) {
            ParticleThread pt = new ParticleThread("ParticleThread[" + i + "]", i, barrier_particle);
            pt.setDeltaTime(deltaTime);
            pt.threadController = this;
            barrier_particle.addThread(pt);
        }
        barrier_particle.initialize();
        barrier_sync = new SingleThreadBarrier<>("SyncBarrier", this);
        syncThread_pipes = new SynchronizationThreadPipe("SyncThread", barrier_sync, control);
        barrier_sync.setThread(syncThread_pipes);

        barrier_sync.initialize();

        controlThread = new ControlThread();
        controlThread.start();
    }

    /**
     * Start the simulation from last stop position. To Restart the simulation
     * from beginning, call Reset() first.
     */
    public void start() {
        run = true;
        calculationFinished = false;
        calculationStartTime = System.currentTimeMillis();

//        initialize();
        calculationFinished = false;

        if (control.getNetwork() != null) {
            Pipe[] fullarray = control.getNetwork().getPipes().toArray(new Pipe[control.getNetwork().getPipes().size()]);
            syncThread_pipes.setPipes(fullarray);
        }

        //initialize number of threads on the surface 
        if (control.getSurface() != null && control.getSurface().getMeasurementRaster() != null) {
            control.getSurface().getMeasurementRaster().setNumberOfThreads(barrier_particle.getThreads().size());
        }

        barrier_sync.setStepStartTime(simulationTimeMS);
        barrier_sync.setStepEndTime(simulationNextTimeMS);
        barrier_particle.setStepStartTime(simulationTimeMS);
        barrier_particle.setStepEndTime(simulationNextTimeMS);
        if (ArrayTimeLineMeasurementContainer.instance != null) {
            ArrayTimeLineMeasurementContainer.instance.setActualTime(simulationTimeMS);
        }
        if (control.getScenario() != null) {
            control.getScenario().setActualTime(simulationTimeMS);
        }
        checkWaitingParticleIndex();
        calculationLoopStarttime = System.currentTimeMillis();

        barrier_sync.getThread().checkMeasurementsBeforeParticleLoop();
        calledObject = barrier_particle;
        for (SimulationActionListener l : listener) {
            try {
                l.simulationSTART(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        barrier_particle.startover();
    }

    /**
     * Stop the simulation after the next finished loop. Stopped simulations can
     * be resumed.
     */
    public void stop() {
        run = false;
        for (SimulationActionListener l : listener) {
            try {
                l.simulationSTOP(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * seed for each ParticleThread is shifted by +1
     *
     * @param seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
        if (randomNumberGenerators != null) {
            Random r = new Random(seed);
            for (int i = 0; i < randomNumberGenerators.length; i++) {
                RandomGenerator newGen = new RandomGenerator(r.nextLong());// newField = new RandomArray(r.nextLong(), (int) (treatblocksize * 10 + 19));
//                RandomArray newGen=new RandomArray(r.nextLong(), 100007);

                randomNumberGenerators[i] = newGen;
            }
        }
    }

    public static long getSimulationTimeMS() {
        return simulationTimeMS;
    }

    /**
     * Threadbarriers call this method, when they have finished one step.
     *
     * @param barrier
     */
    public void finishedLoop(ThreadBarrier barrier) {

        synchronized (this) {
            lastFinishedBarrier = barrier;
            if (!run) {
                return;
            }
            if (barrier == barrier_particle) {
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

    /**
     * Threadbarriers call this, when they have finished the initialization
     * call.
     *
     * @param barrier
     */
    public void initializingFinished(ThreadBarrier barrier) {
        synchronized (this) {
            if (barrier_particle != null && barrier_particle.isinitialized/* && barrier_positionUpdate != null && barrier_positionUpdate.isinitialized*/) {
                initialized = true;
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
                    nextParticleBlockStartIndex = 0;
                    nextRandomNumberBlockStartIndex = 0;
                    checkWaitingParticleIndex();
                    barrier_sync.startover();
                }
            }.start();
        }
    }

    /**
     * Set a new deltatime (seconds) to all threads.
     *
     * @param deltaTimeSeconds
     */
    public void setDeltaTime(double deltaTimeSeconds) {
        if (run) {
            throw new SecurityException("Can not change the delta time interval while Threads are running, inconsistency warning!");
        }
        ThreadController.deltaTime = deltaTimeSeconds;
        ThreadController.deltatimeMS = (int) (deltaTimeSeconds * 1000);
        if (ArrayTimeLineMeasurementContainer.isInitialized()) {
            ArrayTimeLineMeasurementContainer.instance.samplesPerTimeinterval = ArrayTimeLineMeasurementContainer.instance.getDeltaTimeS() / ThreadController.getDeltaTime();
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
        Comparator<Particle> comp = new Comparator<Particle>() {
            @Override
            public int compare(Particle t, Particle t1) {
                if (t.getInsertionTime() == t1.getInsertionTime()) {
                    return 0;
                }
                if (t.getInsertionTime() < t1.getInsertionTime()) {
                    return -1;
                }
                return 1;
            }
        };

        this.particles = particles.toArray(new Particle[particles.size()]);
        Arrays.sort(this.particles, comp);

        //Generate random numbers
        recalculateRandomNumberGenerators();
        setSeed(seed);
        this.waitingParticleIndex = 0;
        this.nextParticleBlockStartIndex = 0;
        this.nextRandomNumberBlockStartIndex = 0;
    }

    private void recalculateRandomNumberGenerators() {
        if (particles != null) {
            randomNumberGenerators = new RandomGenerator[this.particles.length / treatblocksize + 1];

        }
    }

    /**
     * calculates the average time needed to propagate one simulation step.
     * Average value over the past 10 steps.
     *
     * @return
     */
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
     * Milliseconds of stepStartTime. Attention: does not start with 0 at event
     * start. Gives the real time of the event.
     *
     * @return
     */
    public long getSimulationTime() {
        return simulationTimeMS;
    }

    /**
     * Number of completed simulation steps.
     *
     * @return
     */
    public int getSteps() {
        return steps;
    }

    /**
     * Process one single simulation step.
     */
    public void step() {
        run = false;
//        initialize();
        calculationLoopStarttime = System.currentTimeMillis();
        calledObject = barrier_particle;
        checkWaitingParticleIndex();
        barrier_sync.getThread().checkMeasurementsBeforeParticleLoop();
        barrier_particle.startover();
    }

    /**
     * Reset particles, threads and measurements to initial state to start a
     * clean simulation run.
     */
    public void reset() {
        run = false;
        simulationTimeMS = simulationTimeStart;
        simulationNextTimeMS = simulationTimeMS + deltatimeMS;
        nextParticleBlockStartIndex = 0;
        nextRandomNumberBlockStartIndex = 0;
        waitingParticleIndex = 0;
//        initialize();
        barrier_particle.setStepStartTime(simulationTimeMS);
        barrier_particle.setStepEndTime(simulationNextTimeMS);
        barrier_sync.setStepStartTime(simulationTimeMS);
        barrier_sync.setStepEndTime(simulationNextTimeMS);
        if (control.getScenario() != null) {
            //Reset Timelines in scenarios:
            control.getScenario().setActualTime(simulationTimeMS);
            //See if particles need to be added differently
            if (control.getScenario().getInjections() != null) {
                boolean needRecalculation = false;
                for (InjectionInfo injection : control.getScenario().getInjections()) {
                    if (injection.hasChanged()) {
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
                for (InjectionInfo injection : control.getScenario().getInjections()) {
                    injection.resetChanged();
                }
            }
        }
        if (particles != null) {
            for (Particle p : particles) {
                if (p == null) {
                    continue;
                }
                p.setSurrounding_actual(null);
//                p.setPosition1d_actual(p.injectionPosition1D);
//                p.surfaceCellID = p.getInjectionCellID();
                if (p.getClass().equals(HistoryParticle.class)) {
                    ((HistoryParticle) p).clearHistory();
                }
//                p.deposited = false;
                p.toPipenetwork = null;
                p.toSoil = null;
                p.toSurface = null;
//                p.posToSurface = 0;
                p.resetMovementLengths();
                p.setWaiting();
            }
        }
//        setSeed(seed);
        try {
            if (randomNumberGenerators == null) {
                recalculateRandomNumberGenerators();
                setSeed(seed);
            }
            if (randomNumberGenerators != null) {
                for (RandomGenerator rng : randomNumberGenerators) {
                    rng.reset();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (control.getSurface() != null) {
            control.getSurface().reset();
        }
        // Reset calculation of particlepositions.
//        barrier_positionUpdate.getThread().updateParticlePositions();
        calculationTimeElapsed = 0;
        steps = 0;
        nextRandomNumberBlockStartIndex = 0;
        waitingParticleIndex = 0;
        checkWaitingParticleIndex();

        for (SimulationActionListener l : listener) {
            try {
                l.simulationRESET(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Break locks of hanging Threads to reenable correct working of simulation.
     */
    public void breakBarrierLocks() {
        int actualLoop = steps;
        System.out.println("call to break barrier locks in step " + steps);
        stop();

        //that was not enough. threads are still blocking
        for (int i = 0; i < barrier_particle.getThreads().size(); i++) {
            ParticleThread pt = barrier_particle.getThreads().get(i);
            if (pt.getState() == Thread.State.BLOCKED) {
                System.out.println("Replace blocked Thread " + i);
                ParticleThread ptnew = new ParticleThread(pt);
                barrier_particle.getThreads().set(i, ptnew);
                ptnew.start();

                try {
                    pt.stopThread();
                    pt.interrupt();
                    pt.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (pt.getState() == Thread.State.RUNNABLE) {
                if (control.getSurface().getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                    TriangleMeasurement tm = ((SurfaceMeasurementTriangleRaster) control.getSurface().getMeasurementRaster()).monitor[pt.threadIndex];
                    System.out.println("Break lock of " + pt + "  on " + tm);
                    try {
                        if (tm != null) {
                            tm.lock.unlock();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("restart simulation");
        start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (actualLoop != steps) {
            System.out.println("Stop & start solved the problem");
            return;
        }

        //check if the synchronization thread is blocked
        if (barrier_sync.getLock().isLocked()) {
            System.out.println("try to unlock barriersync queued by synchread_pipes " + barrier_sync.getLock().hasQueuedThread(syncThread_pipes));
            System.out.println("call awake on the sync barrier");
//            try {
//                synchronized (barrier_sync) {
//                    barrier_sync.notifyAll();
//                }
////                barrier_sync.getLock().unlock();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            stop();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
            }
            start();
            if (actualLoop != steps) {
                System.out.println("Continue simulation after SynchronizationBarrier lock was released.");
            } else {
                System.out.println("try to reinitialize Synchronization thread");
            }
        }
        System.out.println("irgendwas mit oberfläche");
        //Go through all triangle measurements and free locks
        Surface surf = control.getSurface();
        if (surf != null && surf.getMeasurementRaster() != null) {
            System.out.println("try to break surface measurementRaster locks.");
            try {
                surf.getMeasurementRaster().breakAllLocks();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            //wait until hopefully all threads have finished their simulation
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (actualLoop != steps) {
            //that was enough. simulation is running again. 
            System.out.println("Continue Simulation after Surface Measurements were unlocked.");
            return;
        }
        //that was not enough. threads are still blocking
        for (int i = 0; i < barrier_particle.getThreads().size(); i++) {
            ParticleThread pt = barrier_particle.getThreads().get(i);
            if (pt.getState() == Thread.State.BLOCKED) {
                System.out.println("Replace blocked Thread " + i);
                ParticleThread ptnew = new ParticleThread(pt);
                barrier_particle.getThreads().set(i, ptnew);
                ptnew.start();

                try {
                    pt.stopThread();
                    pt.interrupt();
                    pt.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (pt.getState() == Thread.State.RUNNABLE) {
                if (control.getSurface().getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                    TriangleMeasurement tm = ((SurfaceMeasurementTriangleRaster) control.getSurface().getMeasurementRaster()).monitor[pt.threadIndex];
                    System.out.println("Break lock of " + pt + "  on " + tm);
                    if (tm != null) {
                        tm.lock.unlock();
                    }
                }
            }
        }
    }

    /**
     * Initialize Threads and Threadbarriers.
     */
    private void initialize() {
        if (!initialized) {
            barrier_particle.initialize();
//            barrier_positionUpdate.initialize();
            barrier_sync.initialize();
            for (SimulationActionListener l : listener) {
                try {
                    l.simulationINIT(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return;
        }
    }

    public void setSimulationStartTime(long simulationTimeStart) {
        this.simulationTimeStart = simulationTimeStart;
        simulationTimeMS = simulationTimeStart;
        simulationNextTimeMS = simulationTimeMS + deltatimeMS;
        barrier_particle.setStepStartTime(simulationTimeMS);
        barrier_particle.setStepEndTime(simulationNextTimeMS);
        barrier_sync.setStepStartTime(simulationTimeMS);
        barrier_sync.setStepEndTime(simulationNextTimeMS);

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
        ParticleThread[] a = new ParticleThread[barrier_particle.getThreads().size()];
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

    /**
     * The control thread receives finishing-signals of the working thread
     * barriers and desides which threadbarriers to start next. This is for
     * synchronization of the loop based calculation.
     */
    public class ControlThread extends Thread {

        BARRIERS barrier;
        private int status = 0;
        Object lastenvokenListener = null;

        public ControlThread() {
            super("ThreadController");
        }

        @Override
        public void run() {
            while (true) {
//                status = 1;

                synchronized (this) {
//                    status = 2;
                    finishedLoop(barrier);
//                    status = 3;
                    try {
                        this.wait();
//                        status = 99;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ThreadController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
//                status = 100;
            }
        }

        /**
         * Which barrier has just finished its calculatio. Revokes the control
         * thread to handle ongiong calculation.
         *
         * @param barrier
         */
        private void setBarrier(BARRIERS barrier) {
            //set the barrier, that has send theifinish-signal and start theto start next.            
            synchronized (this) {
                this.barrier = barrier;
                this.notifyAll();
            }
        }

        private void finishedLoop(BARRIERS finishedBarrier) {
            if (!run) {
                return;
            }
//            status = 4;
            switch (finishedBarrier) {
                case PARTICLE:
//                    status = 10;
                    /*@todo move particle thread after synchr.-thread. */
                    barrier_sync.setStepStartTime(simulationTimeMS);
                    barrier_sync.setStepEndTime(simulationNextTimeMS);
                    calledObject = barrier_sync;
                    steps++;
//                    System.out.println("finisched step " + steps + " ended at " + simulationNextTimeMS / 1000 + "s\t " + getNumberOfActiveParticles() + " active particles.");
//                    status = 11;
                    barrier_sync.startover();
//                    status = 12;
                    return;
                case SYNC:
//                    status = 20;
                    long calcStepTime = System.currentTimeMillis() - calculationLoopStarttime;
                    calculationTimeHistory[steps % calculationTimeHistory.length] = calcStepTime;

                    calculationTimeElapsed += calcStepTime;
                    calculationLoopStarttime = System.currentTimeMillis();

                    simulationTimeMS = simulationNextTimeMS;
                    simulationNextTimeMS += deltatimeMS;
//                    status = 22;
                    if (simulationTimeMS >= simulationTimeEnd) {
                        System.out.println("Simulation time end reached!");
                        calculationFinished = true;
                    }
                    checkWaitingParticleIndex();
//                    status = 24;
                    //Send new Timeinformation to all timelines pipe/manhole/surface/soil
                    control.getScenario().setActualTime(simulationTimeMS);
                    if (ArrayTimeLineMeasurementContainer.instance != null) {
                        ArrayTimeLineMeasurementContainer.instance.setActualTime(simulationTimeMS);
                    }
//                    status = 28;
                case POSITION:
//                    status = 30;
                    try {
                        for (SimulationActionListener l : listener) {
                            try {
                                lastenvokenListener = l;
                                l.simulationSTEPFINISH(steps, this);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    status = 31;
                    if (calculationFinished) {
                        run = false;
                        System.out.println("Stopped after " + (System.currentTimeMillis() - calculationStartTime) / 1000 + "sec computation time.\telapsed calculation time=" + calculationTimeElapsed + "ms,  loops:" + steps);

                        for (SimulationActionListener l : listener) {
                            l.simulationFINISH(simulationTimeMS >= simulationTimeEnd, particlesReachedOutlet);
                        }
                        return;
                    }
//                    status = 32;
                case HYDRODYNAMICS:
                    barrier_sync.setStepStartTime(simulationTimeMS);
                    barrier_sync.setStepEndTime(simulationNextTimeMS);
                    barrier_sync.getThread().checkMeasurementsBeforeParticleLoop();

                    barrier_particle.setStepStartTime(simulationTimeMS);
                    barrier_particle.setStepEndTime(simulationNextTimeMS);
                    calledObject = barrier_particle;
                    nextParticleBlockStartIndex = 0;
                    nextRandomNumberBlockStartIndex = 0;
                    barrier_particle.startover();
                    return;
                default:
                    status = 50;
                    break;
            }
            System.out.println("ThreadController.finishedLoop(): this should never be reached.");
        }
    }

    /**
     * Cleans all Threads from Particles of an old simulation network/scenario
     */
    public void cleanFromParticles() {
        this.particles = null;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public int getNumberOfActiveParticles() {
        if (this.particles == null) {
            return 0;
        }
        int active = 0;
        for (Particle particle : particles) {
            if (particle.isActive()) {
                active++;
            }
        }
        return active;
    }

    /**
     * Number of particles that have not yet been released into the simulation.
     *
     * @return
     */
    public int getNumberOfWaitingParticles() {
        if (this.particles == null) {
            return 0;
        }
        int waiting = 0;
        for (Particle particle : particles) {
            if (particle.isWaiting()) {
                waiting++;
            }
        }
        return waiting;
    }

    public int getNumberOfTotalParticles() {
        if (this.particles == null) {
            return -1;
        }
        return particles.length;
    }

    private void checkWaitingParticleIndex() {
        if (particles != null) {
            for (int i = waitingParticleIndex; i < particles.length; i++) {
                if (particles[i].getInsertionTime() <= simulationTimeMS) {
                    waitingParticleIndex = i + 1;
                } else {
                    waitingParticleIndex = i;
                    break;
                }
            }
        }
    }

    public void addSimulationListener(SimulationActionListener listen) {
        this.listener.add(listen);
    }

    public boolean removeSimulationListener(SimulationActionListener listen) {
        return this.listener.remove(listen);
    }

    /**
     * Starts the Statusthread, that can revoke barriers, when they come to a
     * hanging lock problem. (Problem should be solved by october 2019). Checks
     * the running state of the simulation every 10 seconds.
     *
     * @return
     */
    public Thread initStatusThread() {
        final ThreadController tc = this;
//        boolean informed = false;

        statusThread = new Thread("StatusThreadController") {
            private int laststep = -1;
            long lastworkingTimestamp = 0;

            @Override
            public void run() {
                boolean informed = false;
                while (true) {
                    try {

                        if (tc.barrier_particle != null) {
                            int running = 0, waiting = 0;
                            if (!pauseRevokerThread && steps > 0 && (run && steps == laststep)) {
                                // something is incredibly slow. prepare output to console
                                StringBuilder str = new StringBuilder("--" + getClass() + "--detected hanging at loop " + steps + " called barrier: " + (calledObject) + "   :");
                                str.append("\n lastfinishedBarrier: " + lastFinishedBarrier + "  :  " + controlThread.barrier + ",," + "\t control.status=" + controlThread.status + " (" + controlThread.getState() + ")  listener: " + controlThread.lastenvokenListener);
                                int someoneblocked = -1;
                                int someoneRunning = -1;

                                for (Object thread : barrier_particle.getThreads()) {
                                    try {
                                        if (thread instanceof ParticleThread) {
                                            ParticleThread pt = (ParticleThread) thread;
                                            str.append("\n ");
                                            str.append(pt.threadIndex + ". " + pt.getClass().getSimpleName() + " " + pt.getState() + ":" + (pt.isActive() ? "calculating" : "waiting") + " status:" + pt.status + ", index:" + pt.getActualParticleBlockStartIndex());// + "  Particles waiting:" + pt.numberOfWaitingParticles + " active:" + pt.numberOfActiveParticles + "  completed:" + pt.numberOfCompletedParticles);
                                            if (pt.getActualParticle() != null) {
                                                Particle p = pt.getActualParticle();
                                                str.append("   Particle: " + p.getId() + " in " + p.getSurrounding_actual() + " : status=" + p.status);
                                            }
                                            if (pt.pc != null) {
                                                str.append("  PipeComp.status=" + pt.pc.status);
                                            }
                                            if (pt.getSurfaceComputing() != null) {
                                                try {
                                                    str.append("    surfComputing: " + pt.getSurfaceComputing().reportCalculationStatus());
                                                } catch (Exception e) {
                                                }
                                            }
                                            if (pt.getState() == State.BLOCKED) {
                                                someoneblocked = pt.threadIndex;
                                            } else if (pt.getState() == State.RUNNABLE) {
                                                someoneRunning = pt.threadIndex;
                                            }
                                        }
                                    } catch (Exception exception) {
                                        System.err.println("Exception " + exception.getLocalizedMessage());
                                    }

                                }

                                SynchronizationThreadPipe st = syncThread_pipes;
                                str.append("\n ");
                                str.append(st.getClass().getSimpleName()).append(" ").append(st.getState()).append(", status=").append(st.status).append(", Lock is ").append((barrier_sync.getLock().isLocked() ? ("locked, waiting:" + barrier_sync.getLock().getQueueLength()) : "free"));

                                if (someoneRunning >= 0) {
                                    System.out.println("Slow simulation, but Thread " + someoneRunning + " is still working.");
                                }

                                System.out.println(str.toString());
                                int actualloop = steps;
                                if (someoneblocked >= 0 && someoneRunning < 0 && !informed) {
//                                    JOptionPane.showMessageDialog(null, "Blocked Thread " + someoneblocked, "Blovked Thread", JOptionPane.INFORMATION_MESSAGE);// str.append(" Restart Thread "+pt.threadIndex);
//                                    informed = true;
                                }
                                if (someoneRunning < 0 && steps == actualloop) {
                                    if (lockbreaker == null || lockbreaker.getState() == State.TERMINATED) {
                                        lockbreaker = new Thread("Lockbreaker") {
                                            @Override
                                            public void run() {
                                                breakBarrierLocks();
                                            }
                                        };
                                        lockbreaker.start();
                                    } else {
                                        System.out.println("Lockbreaker: " + lockbreaker.getState());
                                        if (lockbreaker.getState() == State.BLOCKED || lockbreaker.getState() == State.WAITING) {
                                            lockbreaker.interrupt();
                                        }
                                    }
                                }
                            } else {
                                lastworkingTimestamp = System.currentTimeMillis();
                            }
                            laststep = steps;
                        }
                        Thread.sleep(15000);

                    } catch (Exception ex) {
                        Logger.getLogger(ThreadController.class
                                .getName()).log(Level.SEVERE, null, ex);
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
//        System.out.println(getClass()+"::loadSurface "+surface+", caller:"+caller);
        for (ParticleThread pt : barrier_particle.getThreads()) {
            pt.setSurface(surface);
        }
    }

    @Override
    public void loadScenario(Scenario scenario, Object caller) {

    }

    ////////////////If Particles are stored and controlled in this object
    /**
     * returns the first and last index of particles to be thretened by the
     * requesting thread. return null if the Thread can go to sleep because
     * there is nothing to do at the moment.; [0] first index of particle from
     * ThreadController to calculate; [1] last index (exclude) of particle to
     * calculate; [2] index of Random number generator to select to calculate
     * the particles
     *
     * @param values optional array to be filled and returned. insert the
     * 3-element long array of the previous step to prevent allocation of new
     * space.
     * @return [0]=-1 if there is nothing to do and thread should wait.
     */
    public int[] getNextParticlesToTreat(int[] values) {
        int[] retur = values;
        if (retur == null) {
            retur = new int[3];
        }
        lock.lock();
        try {
            if (nextParticleBlockStartIndex >= waitingParticleIndex) {
//                System.out.println("return null,  start:" + nextParticleBlockStartIndex + ",  waiting: " + waitingParticleIndex);
                retur[0] = -1;
                return retur;
            }

            retur[0] = nextParticleBlockStartIndex;
            nextParticleBlockStartIndex += treatblocksize;
            if (nextParticleBlockStartIndex >= waitingParticleIndex) {
                nextParticleBlockStartIndex = waitingParticleIndex;
            }
            retur[1] = nextParticleBlockStartIndex; //exclude this index
            retur[2] = nextRandomNumberBlockStartIndex;
            nextRandomNumberBlockStartIndex++;
//            System.out.println("return " + retur[0] + ", " + retur[1] + ", " + retur[2] + " waiting: " + waitingParticleIndex);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            lock.unlock();
        }
        return retur;
    }

    public long getSeed() {
        return seed;
    }

    public Particle[] getParticles() {
        return particles;
    }

    /**
     * Number of particles processed by a Particlethread before asking for the
     * next block.
     *
     * @return
     */
    public int getParticleblocksize() {
        return treatblocksize;
    }

    /**
     * Set number of particles that are processed by a Thread in one call. This
     * should be about 1/20 of the total particle number to allow effective
     * multithreading without waiting for each other of the ParticleThreads.
     *
     * @param treatblocksize
     */
    public void setParticleblocksize(int treatblocksize) {
        this.treatblocksize = treatblocksize;
        recalculateRandomNumberGenerators();
        setSeed(seed);
    }

    public int getNumberOfParallelThreads() {
        if (this.barrier_particle == null) {
            return 0;
        }
        return this.barrier_particle.getThreads().size();
    }

}
