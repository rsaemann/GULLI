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

import control.Controller;
import java.util.ArrayList;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.topology.Pipe;
import model.topology.measurement.ParticleMeasurement;

/**
 * This Thread creates measurements for the actual timestep.
 *
 * @author saemann
 */
public class SynchronizationThread extends Thread {

    private final ThreadBarrier<SynchronizationThread> barrier;
    boolean runendless = true;
    public boolean allFinished = false;
    public long actualSimulationTime = 0;

    public int lastMeasurementImeIndex = -1;
    protected Controller control;

    private final ArrayList<ParticleMeasurement> messung = new ArrayList<>(1);

//    private ParticleThread[] particleThreads;
    private Pipe[] pipes;

    public SynchronizationThread(String string, ThreadBarrier<SynchronizationThread> barrier, Controller control) {
        super(string);
        this.barrier = barrier;
        this.control = control;
    }

    @Override
    public void run() {
        //is initialized now
        barrier.initialized(this);

//        double factor = 0;
        //if woken up start the normal loop
        while (runendless) {
            if (true) {
                try {
                    actualSimulationTime = barrier.getSimulationtime();
//                    if (!tempList.isEmpty()) {
                        /*Add waiting Particles into the list before iterating the list to
                     prevent Concurrent Modification Exceptions.*/
//                        particles.addAll(tempList);
//                        tempList.clear();
//                    }
                    /*
                     * Tell the particles this timestep is finished Set the actual
                     * parameters as the past, to prepare the particles for the next
                     * simulation step.
                     */
//                particleCount.clear();
//                    particlePipes.clear();
//                    boolean stopCalculating = true;
//                    for (ParticleThread pT : control.getThreadController().getParticleThreads()) {
//                    for (ParticleThread pT : particleThreads) {
//                        for (Particle p : pT.getParticles()) {
//                            if(p.isInactive())continue;
////                            if (p.isOnSurface()) {
////                                control.getSurface().getMeasurementRaster().measureParticle(actualSimulationTime, p);
////                            }
//                        }
//
//                        if (!pT.allParticlesReachedOutlet) {
//                            stopCalculating = false;
//                        }
//                    }
//                    for (Particle particle : particles) {
//                        if (particle.active) {
//                            Capacity c = particle.getSurrounding_actual();
//                            if (c != null && c.getClass().equals(Pipe.class)) {
//                                Pipe p = (Pipe) c;
//                                p.getMeasurementTimeLine().addParticle();
//                                particlePipes.add(p);
////                        if (particleCount.containsKey(p)) {
////                            int anzahl = particleCount.get(p);                            
////                            anzahl++;
////                            particleCount.put(p, anzahl);
////                        } else {
////                            particleCount.put(p, 1);
////                        }
//                            }
//                            if (stopCalculating) {
//                            // Suche Partikel, die noch behandelt werden müssen.
//                                // Sobald eines gefunden ist, nicht weiter suchen.
//                                try {
//                                    if (particle.getSurrounding_actual() == null) {
//                                        continue;
//                                    }
//                                    if (!particle.getSurrounding_actual().isSetAsOutlet()) {
//                                        stopCalculating = false;
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    }
//                    if (stopCalculating) {
//                        allFinished = true;
//                    }

                    // Schreibe die Gesammelten Werte in die Mess-Zeitreihe der Rohre
                    ArrayTimeLineMeasurementContainer mcp = control.getScenario().getMeasurementsPipe();
//                    System.out.println(getClass()+":: getMeasurementContainerPipe:"+mcp);
                    int timeindex = mcp.getIndexForTime(actualSimulationTime);
                    if (timeindex >= mcp.getNumberOfTimes()) {
                        timeindex = mcp.getNumberOfTimes() - 1;
                    }
                    if (!mcp.isTimespotmeasurement() || timeindex != lastMeasurementImeIndex && pipes != null) {
//                        for (Pipe pipe : barrier.getThreadController().control.getNetwork().getPipes()) {
                        for (Pipe pipe : pipes) {
                            if (pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
                                pipe.getMeasurementTimeLine().addMeasurement(timeindex,/* pipe.getMeasurementTimeLine().getNumberOfParticles(),pipe.getMeasurementTimeLine().getParticleMassInTimestep(),*/ (float) pipe.getFluidVolume());
                                pipe.getMeasurementTimeLine().resetNumberOfParticles();
                            }
                        }
                    }
                    lastMeasurementImeIndex = timeindex;

                    for (ParticleMeasurement pm : messung) {
                        try {
                            pm.writeCounter(actualSimulationTime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (mcp.isTimespotmeasurement()) {
//                        for (Pipe pipe : barrier.getThreadController().control.getNetwork().getPipes()) {
                        for (Pipe pipe : pipes) {
                            pipe.getMeasurementTimeLine().resetNumberOfParticles();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * Synchronization finished, give control back to the
             * Threadcontroller via the barrier
             */
            barrier.loopfinished(this);

        }
    }

    /**
     * Causes this Thread to run off after the current loop.
     */
    public void stopThread() {
        this.runendless = false;
    }

//    public void addParticle(Particle p) {
//        this.tempList.add(p);
//    }
//
//    public void setParticles(Collection<Particle> p) {
//        this.tempList.addAll(p);
//    }
    public void addParticlemeasurement(ParticleMeasurement pm) {
        this.messung.add(pm);
    }

//    public void setParticleThreads(ParticleThread[] particleThreads) {
//        this.particleThreads = particleThreads;
//    }
    public void setPipes(Pipe[] pipes) {
        this.pipes = pipes;
    }

    public Pipe[] getPipes() {
        return pipes;
    }

//    public int getNumberOfParticles() {
//        int n = 0;
//        if (this.particles != null) {
//            n += this.particles.size();
//        }
//        if (this.tempList != null) {
//            n += this.tempList.size();
//        }
//        return n;
//    }
//    public ParticleThread[] getParticleThreads() {
//        return particleThreads;
//    }
}
