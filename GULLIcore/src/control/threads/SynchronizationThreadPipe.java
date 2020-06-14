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
    private int writeindex = 0;
    private int lastWriteIndex = -1;
    private boolean openMeasurements = false;

    private int lastwriteindexSurface = -1;

    protected Controller control;
    public int status = -1;

    private ArrayList<ParticleMeasurement> messung;

    private Pipe[] pipes;

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
            ArrayTimeLineMeasurementContainer mcp = control.getScenario().getMeasurementsPipe();
            if (mcp != null) {
                if (mcp.isTimespotmeasurement()) {
//                    for (Pipe pipe : pipes) {
//                        pipe.getMeasurementTimeLine().active = false;
//                    }
                    mcp.measurementsActive = false;
                    this.openMeasurements = false;
//                System.out.println("closed measurements");
                } else {
//                    for (Pipe pipe : pipes) {
//                        if (pipe.getMeasurementTimeLine() != null) {
//                            pipe.getMeasurementTimeLine().active = true;
//                        }
//                    }
                    mcp.measurementsActive = true;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        };

        //if woken up start the normal loop
        while (runendless) {
            if (true) {
                try {
                    actualSimulationTime = barrier.getSimulationtime();
                    // Schreibe die Gesammelten Werte in die Mess-Zeitreihe der Rohre
                    ArrayTimeLineMeasurementContainer mcp = control.getScenario().getMeasurementsPipe();
                    if (mcp != null) {
                        if (mcp.isTimespotmeasurement()) {
                            if (openMeasurements) {
                                //Were open during the current step. We can write the samples and reset the counter
                                for (Pipe pipe : pipes) {
                                    ArrayTimeLineMeasurement tl = pipe.getMeasurementTimeLine();
                                    if (tl != null) {
                                        if (tl.getNumberOfParticles() > 0) {
                                            tl.addMeasurement(writeindex, (float) pipe.getFluidVolume());
                                        }
                                        tl.resetNumberOfParticles();

                                    }
                                }
                                mcp.measurementTimes[writeindex] = actualSimulationTime;
                                mcp.measurementsActive = false;
                                mcp.samplesInTimeInterval[writeindex]++;
                                openMeasurements = false;
                                lastWriteIndex = writeindex;
                                writeindex = mcp.getActualTimeIndex() + 1;
                                if (writeindex < mcp.getNumberOfTimes()) {
                                    nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
                                } else {
                                    //Do not sample any more. there are no more slots left to write samples
                                    nextMeasurementTime = Long.MAX_VALUE;
                                }
                            } else {
                                if (mcp.getActualTimeIndex() != lastWriteIndex) {
//                                    System.out.println("need to set writeindex back from " + writeindex + " to " + (mcp.getActualTimeIndex() + 1) + " to sample at time " + mcp.getTimeMillisecondsAtIndex(mcp.getActualTimeIndex() + 1));
                                    if (mcp.getActualTimeIndex() == 0 && actualSimulationTime <= ThreadController.getDeltaTime() * 1000) {
                                        //Try to open measurements for the very first sampling at time 0 (+1simulation step)
                                        writeindex = 0;
                                        nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
                                    } else {
                                        writeindex = mcp.getActualTimeIndex() + 1;
                                        nextMeasurementTime = mcp.getTimeMillisecondsAtIndex(writeindex);
                                    }
                                }

                            }
                            if (actualSimulationTime + ThreadController.getDeltaTime() * 1000 >= nextMeasurementTime) {
                                //Need to open the timelines to collect samples and store them in the next timestep
                                openMeasurements = true;
                                for (Pipe pipe : pipes) {
                                    if (pipe.getMeasurementTimeLine() != null) {
//    pipe.getMeasurementTimeLine().active = true;
                                        pipe.getMeasurementTimeLine().resetNumberOfParticles();
                                    }
                                }
                                mcp.measurementsActive = true;
                            }
                        } else if (pipes != null) {
                            int timeindex = mcp.getIndexForTime(actualSimulationTime);
                            if (timeindex >= mcp.getNumberOfTimes()) {
                                timeindex = mcp.getNumberOfTimes() - 1;
                            }
                            //Also count up, if the interval is only internally filled. the final value is written, when the index changes = when a new interval is going to start
                            measurementsCounter++;
                            if (lastMeasurementImeIndex != timeindex) {
                                lastMeasurementImeIndex = timeindex;
                                for (Pipe pipe : pipes) {
                                    if (pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
                                        pipe.getMeasurementTimeLine().addMeasurement(timeindex, (float) pipe.getFluidVolume());
                                        pipe.getMeasurementTimeLine().resetNumberOfParticles();
                                    }
                                }
                                mcp.measurementTimes[timeindex] = actualSimulationTime;
                                mcp.samplesInTimeInterval[timeindex] = measurementsCounter;
                                measurementsCounter = 0;
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
                    SurfaceMeasurementRaster smr = control.getSurface().getMeasurementRaster();
                    if (smr != null) {
                        if (smr.measurementsActive) {
                            if (lastwriteindexSurface >= 0) {
                                smr.measurementsInTimeinterval[lastwriteindexSurface]++;
                            }
                        }
                        if (smr.continousMeasurements) {
                            smr.measurementsActive = true;

                        } else {
                            if (smr.getIndexContainer().getActualTimeIndex() != lastwriteindexSurface) {
                                smr.measurementsActive = true;
                                lastwriteindexSurface = smr.getIndexContainer().getActualTimeIndex();
                            } else {
                                smr.measurementsActive = false;
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

}
