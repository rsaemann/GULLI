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

import com.saemann.gulli.core.control.Controller;
import java.util.ArrayList;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.measurement.ParticleMeasurement;

/**
 * This Thread creates measurements for the actual timestep.
 *
 * @author saemann
 */
public class SynchronizationThreadPipe extends Thread {

    private final ThreadBarrier barrier;
    boolean runendless = true;
    public boolean allFinished = false;
    public long actualSimulationTime = 0;

    public int lastMeasurementImeIndex = -1;
    private int measurementsCounter = 0;

    private long nextOpenTime = 0;
    private long nextMeasurementTime = 0;
    private int writeindex = -1;
    private int lastWriteIndex = -1;
    private boolean openMeasurements = false;

    private int writeindexSurface = -1;

    protected Controller control;
    public int status = -1;

    private ArrayList<ParticleMeasurement> messung;

    private Pipe[] pipes;

//    private long lastVelocityFreez, nextVelocityDefreez;

    private MeasurementContainer mcp;
    private SurfaceMeasurementRaster smr;

    public SynchronizationThreadPipe(String string, ThreadBarrier barrier, Controller control) {
        super(string);
        this.barrier = barrier;
        this.control = control;
    }

    @Override
    public void run() {
        //is initialized now
        barrier.initialized(this);

        try {
            mcp = control.getScenario().getMeasurementsPipe();
//            if (mcp != null) {
//                if (mcp.isTimespotmeasurement()) {
//                    mcp.measurementsActive = false;
//                    this.openMeasurements = false;
//                } else {
//                    mcp.measurementsActive = true;
//                }
//            }
            smr = control.getScenario().getMeasurementsSurface();
            if (smr != null) {
                if (smr.continousMeasurements) {
                    smr.measurementsActive = true;
                } else {
                    smr.measurementsActive = false;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        };

        //if woken up start the normal loop
        while (runendless) {
            if (true) {
                try {
                    actualSimulationTime = barrier.getStepStartTime();
                    // Schreibe die Gesammelten Werte in die Mess-Zeitreihe der Rohre
                    mcp = control.getScenario().getMeasurementsPipe();
                    if (mcp != null) {
                        if (mcp.measurementsActive) {
                            mcp.measurementTimes[writeindex] = barrier.getStepEndTime();
                            mcp.samplesInTimeInterval[writeindex]++;
//                            System.out.println("Measure pipes index "+writeindex+" at "+barrier.getStepEndTime() +" in step "+control.getThreadController().getSteps());
                            for (Pipe pipe : pipes) {
                                if (pipe.getMeasurementTimeLine() != null) {
                                    if (pipe.getMeasurementTimeLine().getNumberOfParticlesInTimestep()> 0) {
                                        pipe.getMeasurementTimeLine().addMeasurement(writeindex, (float) pipe.getFluidVolume());
                                    }
                                    pipe.getMeasurementTimeLine().resetNumberOfParticles();
                                }
                            }
                        }
                        if (messung != null) {
                            for (ParticleMeasurement pm : messung) {
                                try {
                                    pm.writeCounter(actualSimulationTime);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    //Surface
                    if (control.getSurface() != null) {
                        smr = control.getSurface().getMeasurementRaster();

                        if (smr != null) {
                            if (smr.measurementsActive) {
                                smr.measurementsInTimeinterval[writeindexSurface]++;
                                smr.durationInTimeinterval[writeindexSurface]+=ThreadController.getDeltaTime();
                                smr.measurementTimestamp[writeindexSurface] = barrier.getStepEndTime();
//                            System.out.println("Written on surface index "+writeindexSurface+" \tfor time: "+barrier.getStepEndTime());
                            }
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

    public void addParticlemeasurement(ParticleMeasurement pm) {
        if (messung == null) {
            messung = new ArrayList<>();
        }
        this.messung.add(pm);
    }

    public void setPipes(Pipe[] pipes) {
        this.pipes = pipes;
    }

    public Pipe[] getPipes() {
        return pipes;
    }

    /**
     * Checks, if the network and surface measurements have to be enabled for
     * the upcoming loop. THis is the case, if the loop ends INTO a new
     * measurement timestep that should be recorded.
     *
     * @return
     */
    public boolean checkMeasurementsBeforeParticleLoop() {
        //Pipe
        boolean changed = false;
        if (control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
            MeasurementContainer mp =  control.getScenario().getMeasurementsPipe();
            if (mp.isTimespotmeasurement()) {
                //SAmple only at timespots
                int actual = mp.getIndexForTime(barrier.stepStartTime);
                int next = mp.getIndexForTime(barrier.stepEndTime);
//                System.out.println("prepareSamples in step "+control.getThreadController().getSteps()+": indexnow: "+actual+" :-> "+next);
                if (writeindex > next||writeindex<0) {
                    //After reset of a simulation, we need to reset the writeindex
                    writeindex = next;
                    mp.measurementsActive = true;
                    changed = true;
                } else if (actual != next) {
                    writeindex = next;
                    mp.measurementsActive = true;
                    changed = true;
                } else {
                    if (mp.measurementsActive) {
                        mp.measurementsActive = false;
                        changed = true;
                    }
                }

            } else {
                writeindex = mp.getIndexForTime(barrier.stepEndTime);
                //Sample all the time
                if (!mp.measurementsActive) {
                    mp.measurementsActive = true;
                    changed = true;
                }
            }
        }

        //Surface
        if (control.getSurface() != null) {
            SurfaceMeasurementRaster smr = control.getSurface().getMeasurementRaster();

            if (smr != null) {
                if (smr.continousMeasurements) {
                    if (!smr.measurementsActive) {
                        changed = true;
                        smr.measurementsActive = true;
                    }
                    writeindexSurface = smr.getIndexContainer().getTimeIndex(barrier.getStepEndTime());
                    smr.setWriteIndex(writeindexSurface);
                } else {
                    int actual = smr.getIndexContainer().getTimeIndex(barrier.getStepStartTime());
                    int next = smr.getIndexContainer().getTimeIndex(barrier.getStepEndTime());
                    if (actual != next) {
                        //Enable the sampling, because we want to collect the information at the end of this timestep
                        smr.measurementsActive = true;

                        writeindexSurface = next;
                        smr.setWriteIndex(writeindexSurface);
                        changed = true;
                    } else if (writeindexSurface > actual || writeindexSurface < 0) {
                        //We had a reset and need put this back to 0
                        smr.measurementsActive = true;
                        writeindexSurface = actual;
                        smr.setWriteIndex(writeindexSurface);
                        changed = true;
                    } else {
                        if (smr.measurementsActive) {
                            smr.measurementsActive = false;
                            changed = true;
                        }
                    }
                    smr.getIndexContainer().setActualTime(barrier.getStepEndTime());
                }
            }
        }
        return changed;
    }

}
