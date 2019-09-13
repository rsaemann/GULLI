package control.threads;

import control.particlecontrol.ParticlePipeComputing;
import control.particlecontrol.ParticleSurfaceComputing;
import control.particlecontrol.ParticleSurfaceComputing1D;
import control.particlecontrol.ParticleSurfaceComputing2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import model.particle.HistoryParticle;
import model.particle.Particle;
import model.surface.Surface;
import model.surface.SurfaceTriangle;
import model.topology.measurement.ParticleMeasurement;

/**
 *
 * @author saemann
 */
public class ParticleThread extends Thread {

    private static int runningIndex = 0;

    public final int id = runningIndex++;

    /**
     * Field off all particles to be treated by this Thread. Field should be
     * sorted according to release time.
     */
    protected Particle[] particles;

    /**
     * Index to sa where to start the calculation of the particles. Frome here
     * till waitingindex.
     */
    protected int activeIndex = 0;
    /**
     * Index of the first particle to be released nex. Since particles are
     * ordered accoring to their release time one can easily go from left to
     * right.
     */
    protected int waitingindex = 0;
    /**
     * Number of particles waiting to be released.
     */
    protected int numberOfWaitingParticles = 0;
    /**
     * Number of released and simualting particles in the simulation domain.
     */
    protected int numberOfActiveParticles = 0;
    /**
     * Number of Particles already reached an outlet and are not part of the
     * simulation anymore.
     */
    protected int numberOfCompletedParticles = 0;
    public final LinkedList<Particle> waitingList = new LinkedList<>();

    public final LinkedList<Particle> activeList = new LinkedList<>();
    public final LinkedList<Particle> completedList = new LinkedList<>();

    public ParticlePipeComputing pc;
    private final ThreadBarrier<ParticleThread> barrier;
    private final ArrayList<ParticleMeasurement> messung = new ArrayList<>(4);
    boolean runendless = true;

    public int particleID = -1;
    public Particle particle;

    private ParticleSurfaceComputing surfcomp;
    public int status = -1;

    private long simulationTime;
    private boolean activeCalculation = false;
    public boolean allParticlesReachedOutlet = false;

    public ThreadController threadController;

    public ParticleThread(String string, long seed, ThreadBarrier<ParticleThread> barrier) {
        super(string);
        pc = new ParticlePipeComputing(seed, this);
//        surfcomp = new ParticleSurfaceComputing1D(null, seed);
        surfcomp = new ParticleSurfaceComputing2D(null, seed);
        this.barrier = barrier;
    }

    public void setDeltaTime(double seconds) {
        this.pc.setDeltaTime(seconds);
    }

    public void setSurface(Surface surface) {
        this.surfcomp.setSurface(surface);
        this.pc.setSurface(surface, surface != null);
    }

    public void setSurfaceComputing1D() {
        if (surfcomp == null) {
            surfcomp = new ParticleSurfaceComputing1D(null, Thread.activeCount());
        } else if (!(surfcomp instanceof ParticleSurfaceComputing1D)) {
            surfcomp = new ParticleSurfaceComputing1D(surfcomp.getSurface(), surfcomp.getSeed());
        }
    }

    public void setSurfaceComputing2D() {
        if (surfcomp == null) {
            surfcomp = new ParticleSurfaceComputing2D(null, Thread.activeCount());
        } else if (!(surfcomp instanceof ParticleSurfaceComputing2D)) {
            surfcomp = new ParticleSurfaceComputing2D(surfcomp.getSurface(), surfcomp.getSeed());
        }
    }

    public void setSurfaceComputing(ParticleSurfaceComputing sc) {
        this.surfcomp = sc;
    }

    /**
     * Object to calculate the movement of each particle.
     *
     * @return
     */
    public ParticleSurfaceComputing getSurfaceComputing() {
        return surfcomp;
    }

    @Override
    public void run() {
        //is initialized now
        barrier.initialized(this);
        //if woken up start the normal loop
        Particle p;
        while (runendless) {
            activeCalculation = true;
            if (ThreadController.particlesInThreads) {
                particleID = -2;
                // Copy particles to more performant array
                if (particles == null || particles.length != waitingList.size()) {
                    synchronized (waitingList) {
                        waitingList.addAll(0, activeList);
                        waitingList.addAll(completedList);
                        activeList.clear();
                        completedList.clear();
                        sort();
                        particles = waitingList.toArray(new Particle[waitingList.size()]);
                        numberOfWaitingParticles = particles.length;
                    }
                }

                simulationTime = barrier.getSimulationtime();
                status = 0;
                for (int i = waitingindex; i < particles.length; i++) {
                    p = particles[i];
                    if (p == null) {
                        continue;
                    }
                    particleID = p.getId();
                    if (p.getInsertionTime() <= simulationTime) {
                        p.setSurrounding_actual(p.injectionSurrounding);

                        if (p.injectionSurrounding instanceof SurfaceTriangle) {
                            p.setOnSurface();
                            p.surfaceCellID = p.getInjectionCellID();
                            p.setPosition3d(p.injectionSurrounding.getPosition3D(0));
                        } else if (p.injectionSurrounding instanceof Surface) {
                            p.setOnSurface();
                            p.surfaceCellID = p.getInjectionCellID();
                            double[] pos = ((Surface) p.injectionSurrounding).getTriangleMids()[p.getInjectionCellID()];
                            p.setPosition3D(pos[0], pos[1]);
                        } else {
                            p.setInPipenetwork();
                            p.setPosition1d_actual(p.injectionPosition1D);
                        }
                        //Start at this index in the next timestep
                        numberOfActiveParticles++;
                        numberOfWaitingParticles--;
                        waitingindex = i + 1;
                    } else {
                        //Since all particles are ordered asc by their insertion time
                        // the search can be canceled if the first is not to be activated.
                        break;
                    }
                }
                //Finished activation of particles.
                //Start treating movement
                status = 3;
                particleID = -20;
                for (int i = activeIndex; i < waitingindex; i++) {
                    p = particles[i];

                    if (p == null || p.isInactive()) {
                        if (activeIndex == i) {
                            activeIndex++;
                        }
                        continue;
                    }

                    particleID = p.getId();
                    particle = p;

                    if (p.isInPipeNetwork()) {
                        status = 4;
                        pc.moveParticle(p);
                    } else if (p.isOnSurface()) {
                        status = 5;
                        surfcomp.moveParticle(p);

                    } else {
                        status = 6;

                        System.out.println(getClass() + ":: undefined status (" + p.status + ") of particle (" + p.getId() + "). Surrounding=" + p.getSurrounding_actual());
                    }

                    status = 7;

                    //Became inactive during this step?
                    if (p.isInactive()) {
                        numberOfActiveParticles--;
                        numberOfCompletedParticles++;
                    }

                }
                this.allParticlesReachedOutlet = activeIndex == particles.length;//loopTestAllParticlesReachedOutlet;
                particleID = -5;
                activeCalculation = false;
                status = 20;
                //wait until barrier wakes up this Thread again.
                barrier.loopfinished(this);
                status = 21;

                particleID = -6;
            } else {
                int[] fromto = threadController.getNextParticlesToTreat();
                if (fromto == null) {
                    //finished loop fot his timestep
                    particleID = -5;
                    activeCalculation = false;
                    status = 20;
                    barrier.loopfinished(this);
                } else {
                    //Got valid order to threat particles.
                    this.simulationTime = barrier.getSimulationtime();
                    int from = fromto[0];
                    int to = fromto[1];
                    int active = 0;
                    int finished = 0;
//                    System.out.println("Paticlethread " + id + " soll von " + from + " bis " + to + " berechnen.");
                    for (int i = from; i <= to; i++) {
                        p = threadController.particles[i];
                        this.particleID = p.getId();

//                        System.out.println("particle "+p.getId()+" is "+(p.isActive()?"active":"inactive")+"\t "+(p.getInsertionTime()>this.simulationTime?("waiting another"+(p.getInsertionTime()-this.simulationTime)/1000+"s"):"activating..."));
                        if (!p.isActive()) {
                            if (p.getInsertionTime() > this.simulationTime) {
                                //this particle is still waiting for its initialization.
                                //All further particles area also waiting. Break the loop here.
                                break;
//                                continue;
                            } else {
                                if (p.getSurrounding_actual() != null) {
                                    if (p.getSurrounding_actual() == p.injectionSurrounding) {
                                        if (p.injectionSurrounding instanceof SurfaceTriangle) {
                                            p.setOnSurface();
                                            p.surfaceCellID = p.getInjectionCellID();
                                            p.setPosition3d(p.injectionSurrounding.getPosition3D(0));
                                        } else if (p.injectionSurrounding instanceof Surface) {
                                            p.setOnSurface();
                                            p.surfaceCellID = p.getInjectionCellID();
                                            double[] pos = ((Surface) p.injectionSurrounding).getTriangleMids()[p.getInjectionCellID()];
                                            p.setPosition3D(pos[0], pos[1]);
                                        } else {
                                            p.setInPipenetwork();
                                            p.setPosition1d_actual(p.injectionPosition1D);
                                        }
                                    }
                                }
                            }
                        }
                        //check if it has been initialized from waiting list yet
                        if (p.isActive()) {
                            particleID = p.getId();
                            particle = p;

                            if (p.isInPipeNetwork()) {
                                status = 4;
                                pc.moveParticle(p);
                            } else if (p.isOnSurface()) {
                                status = 5;
                                surfcomp.moveParticle(p);

                            } else {
                                status = 6;
                                System.out.println(getClass() + ":: undefined status (" + p.status + ") of particle (" + p.getId() + "). Surrounding=" + p.getSurrounding_actual());
                            }
                        }
                    }
                    this.allParticlesReachedOutlet = false;
                }
            }
        }
    }

    public void setSeed(long seed) {
        surfcomp.setSeed(seed);
        pc.setSeed(seed);
    }

    /**
     * @deprecated
     */
    private void runWithArrays() {
        //is initialized now
        barrier.initialized(this);
        //if woken up start the normal loop
        Iterator<Particle> it;
        Particle p;
        while (runendless) {
            particleID = -2;
            simulationTime = barrier.getSimulationtime();
//            boolean loopTestAllParticlesReachedOutlet = true;
            status = 0;
            if (!waitingList.isEmpty()) {
                status = 1;
//                loopTestAllParticlesReachedOutlet = false;
                it = waitingList.iterator();
                while (it.hasNext()) {
                    p = it.next();
                    if (p.getInsertionTime() <= simulationTime) {
//                        p.setActive(true);
                        it.remove();
//                        tempList.add(p);
                        activeList.add(p);
                        p.setSurrounding_actual(p.injectionSurrounding);
                        p.setPosition1d_actual(p.injectionPosition1D);

                        if (p.injectionSurrounding instanceof SurfaceTriangle) {
                            p.setOnSurface();
                            p.surfaceCellID = p.getInjectionCellID();
                            p.setPosition3d(p.injectionSurrounding.getPosition3D(0));
                        } else if (p.injectionSurrounding instanceof Surface) {
                            p.setOnSurface();
                            p.surfaceCellID = p.getInjectionCellID();
                            double[] pos = ((Surface) p.injectionSurrounding).getTriangleMids()[p.getInjectionCellID()];
                            p.setPosition3D(pos[0], pos[1]);
                        } else {
                            p.setInPipenetwork();
                            p.setPosition1d_actual(p.injectionPosition1D);
                        }
                    } else {
                        //Since all particles are ordered asc by their insertion time
                        // the search can be canceled if the first is not to be activated.
                        break;
                    }
                }
            }
            status = 3;
            particleID = -20;
            it = activeList.iterator();
            synchronized (waitingList) {
                while (it.hasNext()) {
                    p = it.next();
                    if (p == null) {
                        continue;
                    }
                    particleID = p.getId();
                    particle = p;

                    if (p.isInactive()) {
                        //Aus der aktiv-Liste entfernen, damit es nicht mehr detektiert wird.
                        status = 8;
                        completedList.add(p);
                        it.remove();
                        continue;
                    }

                    if (p.isInPipeNetwork()) {
                        status = 4;
//                    this.setName("ParticleThread in pipes");
                        pc.moveParticle(p);
                    } else if (p.isOnSurface()) {
//                    this.setName("ParticleThread on surface ");
                        status = 5;
                        surfcomp.moveParticle(p);

                    } else {
                        status = 6;

                        System.out.println(getClass() + ":: undefined status (" + p.status + ") of particle (" + p.getId() + "). Surrounding=" + p.getSurrounding_actual());
                    }
                    status = 7;

                }
            }
            particleID = -3;
            status = 10;
            try {
                for (ParticleMeasurement pm : messung) {
                    status = 11;
                    pm.measureParticles(activeList);//pc.getParticles());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            particleID = -4;
            status = 12;
            if (waitingList.isEmpty() && activeList.isEmpty()) {
                status = 13;
                this.allParticlesReachedOutlet = true;
            } else {
                status = 14;
                this.allParticlesReachedOutlet = false;
            }
            particleID = -5;
            //System.out.println("     "+toString()+" finished run()");
            activeCalculation = false;
            status = 20;
            //wait until barrier wakes up this Thread again.
            barrier.loopfinished(this);
            status = 21;
            activeCalculation = true;
            particleID = -6;
        }
    }

    /**
     * Returns true if this Thread is inside its calculation loop and not
     * waiting.
     *
     * @return
     */
    public boolean isActive() {
        return activeCalculation;
    }

    /**
     * Causes this Thread to run off after the current loop.
     */
    public void stopThread() {
        this.runendless = false;
    }

    public void addParticle(Particle p) {
        synchronized (waitingList) {
            this.waitingList.add(p);
            sort();
        }
    }

    public void addParticles(Collection<Particle> p) {
        synchronized (waitingList) {
            this.waitingList.addAll(p);
            sort();
        }
    }

    public double getDeltatime() {
        return pc.getDeltaTime();
    }

    public void reset() {
        synchronized (waitingList) {
            waitingList.addAll(activeList);
            waitingList.addAll(completedList);
            activeList.clear();
            completedList.clear();
            allParticlesReachedOutlet = false;
            waitingindex = 0;
            activeIndex = 0;
            numberOfActiveParticles = 0;
            if (particles != null) {
                numberOfWaitingParticles = particles.length;
            }
            numberOfCompletedParticles = 0;
            sort();
            allParticlesReachedOutlet = false;
            for (Particle p : particles) {
                if (p == null) {
                    continue;
                }
                p.setInactive();
                p.setPosition1d_actual(p.injectionPosition1D);
                p.surfaceCellID = p.getInjectionCellID();
                if (p.getClass().equals(HistoryParticle.class)) {
                    ((HistoryParticle) p).clearHistory();
                }
                p.deposited = false;
                p.toPipenetwork = null;
                p.toSoil = null;
                p.toSurface = null;
                p.posToSurface = 0;
                p.resetMovementLengths();
                p.setInactive();
            }
            pc.resetRandomDistribution();
            if (surfcomp != null) {
                surfcomp.reset();
            }
        }
    }

    public void addParticlemeasurement(ParticleMeasurement pm) {
        this.messung.add(pm);
    }

    public void sort() {
        Collections.sort(waitingList, new Comparator<Particle>() {
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
        });
    }

    public Particle[] getParticles() {
        return particles;
    }

    public int getNumberOfActiveParticles() {
        return numberOfActiveParticles;
    }

    public int getNumberOfTotalParticles() {
        if (particles == null) {
            return 0;
        }
        return particles.length;//waitingList.size() + activeList.size() + completedList.size();
    }

    public void clearParticles() {
        particles = new Particle[0];
        waitingList.clear();
        activeList.clear();
        completedList.clear();
    }
    
}
