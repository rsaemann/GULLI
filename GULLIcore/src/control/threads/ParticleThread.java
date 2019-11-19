package control.threads;

import control.maths.RandomArray;
import control.particlecontrol.ParticlePipeComputing;
import control.particlecontrol.ParticleSurfaceComputing;
import control.particlecontrol.ParticleSurfaceComputing1D;
import control.particlecontrol.ParticleSurfaceComputing2D;
import java.util.ArrayList;
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
    private final ArrayList<ParticleMeasurement> messung = new ArrayList<>(1);
    boolean runendless = true;

    public int particleID = -1;
    public Particle particle;

    private ParticleSurfaceComputing surfcomp;
    public int status = -1;

    private long simulationTime;
    private boolean activeCalculation = false;
    public boolean allParticlesReachedOutlet = false;

    public ThreadController threadController;
    public int threadIndex = 0;

    public ParticleThread(String string, int index, ThreadBarrier<ParticleThread> barrier) {
        super(string);
        this.threadIndex = index;
        pc = new ParticlePipeComputing();
        surfcomp = new ParticleSurfaceComputing2D(null, index);

        this.barrier = barrier;
    }

    public ParticleThread(ParticleThread ptOld) {
        super(ptOld.getName() + "+");
        this.threadController = ptOld.threadController;
        this.threadIndex = ptOld.threadIndex;
        this.activeCalculation = false;
        this.runendless = ptOld.runendless;
        this.barrier = ptOld.barrier;
        pc = ptOld.pc;
        surfcomp = ptOld.surfcomp;
        this.simulationTime = ptOld.simulationTime;
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
            surfcomp = new ParticleSurfaceComputing1D(surfcomp.getSurface());
        }
        if (surfcomp != null) {
            surfcomp.setDeltaTimestep(pc.getDeltaTime());
        }
    }

    public void setSurfaceComputing2D() {
        if (surfcomp == null) {
            surfcomp = new ParticleSurfaceComputing2D(null, threadIndex);
        } else if (!(surfcomp instanceof ParticleSurfaceComputing2D)) {
            surfcomp = new ParticleSurfaceComputing2D(surfcomp.getSurface(), threadIndex);
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
        status = 10;
        if (barrier.isinitialized) {
            barrier.loopfinished(this);
        } else {
            barrier.initialized(this);
            status = 20;
        }
        //if woken up start the normal loop
        Particle p;
        int[] fromto = null;
        while (runendless) {
            try {
//                activeCalculation = true;
//                status = 0;
                status = 30;
                fromto = threadController.getNextParticlesToTreat(fromto);
                status = 31;
                if (fromto == null || fromto[0] < 0) {
                    //finished loop fot his timestep
//                    particleID = -5;
//                    activeCalculation = false;
                    status = 33;
                    barrier.loopfinished(this);
                    status = 34;
                } else {
                    status = 35;
                    //Got valid order to threat particles.
                    this.simulationTime = barrier.getSimulationtime();
                    this.surfcomp.setActualSimulationTime(simulationTime);
                    int from = fromto[0];
                    int toExcld = fromto[1];
//                    if (fromto[2] >= threadController.randomNumberGenerators.length) {
//                        System.err.println("wrong index " + fromto[2] + " for particles " + fromto[0] + "-" + fromto[1] + " of total " + threadController.randomNumberGenerators.length + "   waitingindex: " + threadController.waitingParticleIndex);
//                    }
                    RandomArray random = threadController.randomNumberGenerators[fromto[2]];
                    this.pc.setRandomNumberGenerator(random);
                    this.surfcomp.setRandomNumberGenerator(random);
//                    status = 2;
                    for (int i = from; i < toExcld; i++) {
                        try {
                            p = threadController.particles[i];
                        } catch (Exception e) {
                            System.err.println("tc:"+threadController);
                            System.err.println("tc.particles:"+threadController.particles);
                            e.printStackTrace();
                            continue;
                        }
//                        this.particleID = p.getId();
                        if (p.isWaiting()) {
                            if (p.getInsertionTime() > this.simulationTime) {
                                //this particle is still waiting for its initialization.
                                //All further particles area also waiting. Break the loop here.
                                break;
                            } else {
                                if (p.injectionSurrounding.getClass().equals(Surface.class)) {
                                    p.setOnSurface();
                                    p.surfaceCellID = p.getInjectionCellID();
                                    double[] pos = ((Surface) p.injectionSurrounding).getTriangleMids()[p.getInjectionCellID()];
                                    p.setPosition3D(pos[0], pos[1]);
                                } else if (p.injectionSurrounding.getClass().equals(SurfaceTriangle.class)) {
                                    p.setOnSurface();
                                    p.surfaceCellID = p.getInjectionCellID();
                                    p.setPosition3d(p.injectionSurrounding.getPosition3D(0));
                                } else {
                                    p.setInPipenetwork();
                                    p.setPosition1d_actual(p.injectionPosition1D);
                                }
                                p.setSurrounding_actual(p.injectionSurrounding);
                            }
                        }
                        //check if it has been initialized from waiting list yet
                        if (p.isActive()) {
//                            particleID = p.getId();
//                            particle = p;

                            if (p.isInPipeNetwork()) {
//                                status = 4;
                                pc.moveParticle(p);
                            } else if (p.isOnSurface()) {
//                                status = 5;
                                surfcomp.moveParticle(p);
                            } else {
//                                status = 6;
                                System.out.println(getClass() + ":: undefined status (" + p.status + ") of particle (" + p.getId() + "). Surrounding=" + p.getSurrounding_actual());
                            }
//                            status = 7;
                        }
//                        particle = null;
//                        particleID = -1;
                    }
//                    status = 100;
//                    this.allParticlesReachedOutlet = false;
//                    activeCalculation = false;
                }
                status = 50;
            } catch (Exception ex) {
                activeCalculation = false;
                this.allParticlesReachedOutlet = false;
                ex.printStackTrace();
                status = 200;
                barrier.loopfinished(this);
            }
        }
    }

//    public void setSeed(long seed) {
////        surfcomp.setSeed(seed);
////        pc.setSeed(seed);
//        if()
//    }
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
