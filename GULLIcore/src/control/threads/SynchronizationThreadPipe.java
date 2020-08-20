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
import model.surface.measurement.SurfaceMeasurementRaster;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.topology.Pipe;
import model.topology.measurement.ParticleMeasurement;

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

    private long lastVelocityFreez, nextVelocityDefreez;

    private ArrayTimeLineMeasurementContainer mcp;
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
                                    if (pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
                                        pipe.getMeasurementTimeLine().addMeasurement(writeindex, (float) pipe.getFluidVolume());
                                    }
                                    pipe.getMeasurementTimeLine().resetNumberOfParticles();
                                }
                            }

                        }
//                        if (false && mcp.isTimespotmeasurement()) {
//                            if (openMeasurements) {
//                                //Were open during the current step. We can write the samples and reset the counter
//                                if (writeindex >= mcp.getNumberOfTimes()) {
//                                    writeindex = 0;
//                                }
//                                for (Pipe pipe : pipes) {
//                                    ArrayTimeLineMeasurement tl = pipe.getMeasurementTimeLine();
//                                    if (tl != null) {
//                                        if (tl.getNumberOfParticles() > 0) {
//                                            tl.addMeasurement(writeindex, (float) pipe.getFluidVolume());
//                                        }
//                                        tl.resetNumberOfParticles();
//
//                                    }
//                                }
//                                System.out.println("take sample at " + writeindex + ", in loop " + control.getThreadController().getSteps() + "   simtime: " + actualSimulationTime);
//                                mcp.measurementTimes[writeindex] = actualSimulationTime;
//                                mcp.measurementsActive = false;
//                                mcp.samplesInTimeInterval[writeindex]++;
//                                openMeasurements = false;
//                                lastWriteIndex = writeindex;
//                                writeindex = mcp.getActualTimeIndex() + 1;
//                                if (writeindex < mcp.getNumberOfTimes()) {
//                                    nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
//                                } else {
//                                    //Do not sample any more. there are no more slots left to write samples
//                                    nextMeasurementTime = Long.MAX_VALUE;
//                                }
//                            } else {
//                                if (mcp.getActualTimeIndex() != lastWriteIndex) {
////                                    System.out.println("need to set writeindex back from " + writeindex + " to " + (mcp.getActualTimeIndex() + 1) + " to sample at time " + mcp.getTimeMillisecondsAtIndex(mcp.getActualTimeIndex() + 1));
//                                    if (mcp.getActualTimeIndex() == 0 && actualSimulationTime <= ThreadController.getDeltaTime() * 1000) {
//                                        //Try to open measurements for the very first sampling at time 0 (+1simulation step)
//                                        writeindex = 0;
//                                        nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
//                                    } else {
//                                        writeindex = mcp.getActualTimeIndex() + 1;
//                                        nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
//                                    }
//                                }
//
//                            }
//                            if (actualSimulationTime + ThreadController.getDeltaTime() * 1000 >= nextMeasurementTime) {
//                                //Need to open the timelines to collect samples and store them in the next timestep
//                                openMeasurements = true;
//                                for (Pipe pipe : pipes) {
//                                    if (pipe.getMeasurementTimeLine() != null) {
////    pipe.getMeasurementTimeLine().active = true;
//                                        pipe.getMeasurementTimeLine().resetNumberOfParticles();
//                                    }
//                                }
//                                mcp.measurementsActive = true;
//                            }
//                        } else if (pipes != null) {
//                            int timeindex = mcp.getIndexForTime(actualSimulationTime);
//                            if (timeindex >= mcp.getNumberOfTimes()) {
//                                timeindex = mcp.getNumberOfTimes() - 1;
//                            }
//                            //Also count up, if the interval is only internally filled. the final value is written, when the index changes = when a new interval is going to start
//                            measurementsCounter++;
//                            if (lastMeasurementImeIndex != timeindex) {
//                                lastMeasurementImeIndex = timeindex;
//                                for (Pipe pipe : pipes) {
//                                    if (pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
//                                        pipe.getMeasurementTimeLine().addMeasurement(timeindex, (float) pipe.getFluidVolume());
//                                        pipe.getMeasurementTimeLine().resetNumberOfParticles();
//                                    }
//                                }
//                                mcp.measurementTimes[timeindex] = actualSimulationTime;
//                                mcp.samplesInTimeInterval[timeindex] = measurementsCounter;
//                                measurementsCounter = 0;
//                            }
//
//                        }

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
            ArrayTimeLineMeasurementContainer mp = control.getScenario().getMeasurementsPipe();
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
