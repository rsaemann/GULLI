package com.saemann.gulli.core.control.threads;

import com.saemann.gulli.core.control.maths.RandomGenerator;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.particlecontrol.injection.ManholeInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.PipeInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.SurfaceInjection;
import java.util.ArrayList;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.measurement.ParticleMeasurement;

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

//    public int particleID = -1;

    private ParticleSurfaceComputing2D surfcomp;
    public int status = -1;

    private long simulationTime;
    private boolean activeCalculation = false;
    public boolean allParticlesReachedOutlet = false;

    public ThreadController threadController;
    public int threadIndex = 0;

    // For inside the simulation loop.
    private Particle p;
    private int[] fromto = null;
    private int from;
    private int toExcld;
    private RandomGenerator random;

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

    public void setSurfaceComputing(ParticleSurfaceComputing2D sc) {
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
    public ParticleSurfaceComputing2D getSurfaceComputing() {
        return surfcomp;
    }

    @Override
    public void run() {
        //is initialized now
        if (barrier.isinitialized) {
            barrier.loopfinished(this);
        } else {
            barrier.initialized(this);
        }
        //if woken up start the normal loop

        while (runendless) {
            try {
                
                fromto = threadController.getNextParticlesToTreat(fromto);
                if (fromto == null || fromto[0] < 0) {
                    //finished loop fot his timestep
                    barrier.loopfinished(this);
                } else {
                    activeCalculation=true;
                    //Got valid order to threat particles.
                    this.simulationTime = barrier.getStepStartTime();
                    this.surfcomp.setActualSimulationTime(simulationTime);
                    from = fromto[0];
                    toExcld = fromto[1];

                    random = threadController.randomNumberGenerators[fromto[2]];
                    this.pc.setRandomNumberGenerator(random);
                    this.surfcomp.setRandomNumberGenerator(random);

                    for (int i = from; i < toExcld; i++) {
                        try {
                            p = threadController.particles[i];
                        } catch (Exception e) {
                            System.err.println("tc:" + threadController);
                            if (threadController.particles == null) {
                                break;
                            }
                            System.err.println("tc.particles:" + threadController.particles);
                            e.printStackTrace();
                            break;
                        }
//                        this.particleID = p.getId();
                        if (p.isWaiting()) {
                            if (p.getInsertionTime() > this.simulationTime) {
                                //this particle is still waiting for its initialization.
                                //All further particles area also waiting. Break the loop here.
                                break;
                            } else {
                                status=1;
//                                System.out.println("Injectioninformation:: "+p.getInjectionInformation()+" surface? "+p.getInjectionInformation().spillOnSurface());
                                if (p.getInjectionInformation().spillOnSurface()) {
                                    SurfaceInjection si = (SurfaceInjection) p.getInjectionInformation();
                                    p.setSurrounding_actual(si.getInjectionCapacity());
//                                    System.out.println("Inject Particle on Surface "+si.getInjectionCapacity());
                                    p.surfaceCellID = (int) si.getInjectionCellID(p.getId());
                                    p.setPosition3D(si.getInjectionPosition(p.getId()));
                                    p.setOnSurface();
                                } else if (p.getInjectionInformation().spillinPipesystem()) {
                                    if (p.getInjectionInformation().getClass() == PipeInjection.class) {
                                        PipeInjection pi = (PipeInjection) p.getInjectionInformation();
                                        p.setSurrounding_actual(pi.getInjectionCapacity());
                                        p.setPosition1d_actual(pi.getDistanceAlongPipeMeter());
                                        p.setInPipenetwork();
                                    } else if (p.getInjectionInformation().getClass() == ManholeInjection.class) {
                                        ManholeInjection mhi = (ManholeInjection) p.getInjectionInformation();
                                        p.setSurrounding_actual(mhi.getInjectionCapacity());
                                        p.setPosition1d_actual(0);
                                        p.setPosition3D(mhi.getInjectionCapacity().getPosition());
                                        p.setInPipenetwork();
                                    } else {
                                        System.err.println("Do not know injection information " + p.getInjectionInformation());
                                    }
                                } else {
                                    System.err.println("Do not know where to spill " + p.getInjectionInformation());
                                }
                                status=0;
                            }
                        }
                        //check if it has been initialized from waiting list yet
                        if (p.isActive()) {
                            if (p.isInPipeNetwork()) {
                                pc.moveParticle(p);
                            } else if (p.isOnSurface()) {
                                surfcomp.moveParticle(p);
                            } else {
                                System.out.println(getClass() + ":: undefined status (" + p.status + ") of particle (" + p.getId() + "). Surrounding=" + p.getSurrounding_actual());
                            }
                        }
                    }
                    p=null;
                    activeCalculation=false;
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
    
    /**
     * Simulation time step seconds
     * @return seconds
     */
    public double getDeltatime() {
        return pc.getDeltaTime();
    }

//    public void reset() {
//    }

    public void addParticlemeasurement(ParticleMeasurement pm) {
        this.messung.add(pm);
    }
    
    public Particle getActualParticle(){
        return p;
    }
    
    public int getActualParticleBlockStartIndex(){
        if(fromto==null)return -1;
        return fromto[0];
    }

}
