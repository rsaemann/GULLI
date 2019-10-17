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
        surfcomp = new ParticleSurfaceComputing2D(null, seed);
        this.barrier = barrier;
    }

    public void setDeltaTime(double seconds) {
        this.pc.setDeltaTime(seconds);
        if (surfcomp != null) {
            surfcomp.setDeltaTimestep(seconds);
        }
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
        if (surfcomp != null) {
            surfcomp.setDeltaTimestep(pc.getDeltaTime());
        }
    }

    public void setSurfaceComputing2D() {
        if (surfcomp == null) {
            surfcomp = new ParticleSurfaceComputing2D(null, Thread.activeCount());
        } else if (!(surfcomp instanceof ParticleSurfaceComputing2D)) {
            surfcomp = new ParticleSurfaceComputing2D(surfcomp.getSurface(), surfcomp.getSeed());
        }
        if (surfcomp != null) {
            surfcomp.setDeltaTimestep(pc.getDeltaTime());
        }
    }

    public void setSurfaceComputing(ParticleSurfaceComputing sc) {
        this.surfcomp = sc;
        if (surfcomp != null) {
            surfcomp.setDeltaTimestep(pc.getDeltaTime());
        }
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
            try {
                activeCalculation = true;
                int[] fromto = threadController.getNextParticlesToTreat();
                if (fromto == null) {
                    //finished loop fot his timestep
                    particleID = -5;
                    activeCalculation = false;
                    status = 20;
                    barrier.loopfinished(this);
                    status=21;
                } else {
                    //Got valid order to threat particles.
                    this.simulationTime = barrier.getSimulationtime();
                    int from = fromto[0];
                    int to = fromto[1];
                    for (int i = from; i <= to; i++) {
                        p = threadController.particles[i];
                        this.particleID = p.getId();
                        if (p.isWaiting()) {
                            if (p.getInsertionTime() > this.simulationTime) {
                                //this particle is still waiting for its initialization.
                                //All further particles area also waiting. Break the loop here.
                                break;
                            } else {
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
                                p.setSurrounding_actual(p.injectionSurrounding);
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
                        particle = null;
                        particleID = -1;
                    }
                    this.allParticlesReachedOutlet = false;
                    activeCalculation = false;
                }
            } catch (Exception ex) {
                activeCalculation = false;
                this.allParticlesReachedOutlet = false;
                ex.printStackTrace();
                status = 200;
                barrier.loopfinished(this);
            }
        }
    }

    public void setSeed(long seed) {
        surfcomp.setSeed(seed);
        pc.setSeed(seed);
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

    public double getDeltatime() {
        return pc.getDeltaTime();
    }

    public void reset() {
    }

    public void addParticlemeasurement(ParticleMeasurement pm) {
        this.messung.add(pm);
    }

}
