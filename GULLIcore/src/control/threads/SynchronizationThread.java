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
import java.util.Date;
import model.timeline.array.ArrayTimeLineMeasurement;
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

    private long nextOpenTime = 0;
    private int writeindex = 0;
    private boolean openMeasurements = false;

    protected Controller control;
    public int status = -1;

    private final ArrayList<ParticleMeasurement> messung = new ArrayList<>(1);

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

        //if woken up start the normal loop
        while (runendless) {
            if (true) {
                try {
//                    status = 0;
                    actualSimulationTime = barrier.getSimulationtime();

                    // Schreibe die Gesammelten Werte in die Mess-Zeitreihe der Rohre
                    ArrayTimeLineMeasurementContainer mcp = control.getScenario().getMeasurementsPipe();

                    if (mcp.isTimespotmeasurement()) {
                        if (openMeasurements) {
                            for (Pipe pipe : pipes) {
                                ArrayTimeLineMeasurement tl = pipe.getMeasurementTimeLine();
                                if (tl != null && tl.getNumberOfParticles() > 0) {
                                    tl.addMeasurement(writeindex, (float) pipe.getFluidVolume());
                                    tl.resetNumberOfParticles();
                                    tl.active = false;
                                }
                            }
                            int timeindex = mcp.getIndexForTime(actualSimulationTime);
                            if (timeindex >= mcp.getNumberOfTimes()) {
                                timeindex = mcp.getNumberOfTimes() - 1;
                            }
                            if (mcp.getNumberOfTimes() > timeindex + 1) {
                                if (mcp.getNumberOfTimes() == timeindex + 2) {
                                    nextOpenTime = (long) (mcp.getEndTime() - ThreadController.getDeltaTime() * 2000);
                                    writeindex = timeindex + 1;
                                } else {
                                    nextOpenTime = mcp.getTimeMillisecondsAtIndex(timeindex + 1) - (int) (ThreadController.getDeltaTime() * 1000);
                                    writeindex = timeindex + 1;
                                }
                            }
                        }
                        openMeasurements = false;
                    } else if (pipes != null) {
                        int timeindex = mcp.getIndexForTime(actualSimulationTime);
                        if (timeindex >= mcp.getNumberOfTimes()) {
                            timeindex = mcp.getNumberOfTimes() - 1;
                        }
                        for (Pipe pipe : pipes) {
                            if (pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
                                pipe.getMeasurementTimeLine().addMeasurement(timeindex, (float) pipe.getFluidVolume());
                                pipe.getMeasurementTimeLine().resetNumberOfParticles();
                            }
                        }
                        lastMeasurementImeIndex = timeindex;
                    }

//                    status = 4;
                    for (ParticleMeasurement pm : messung) {
                        try {
                            pm.writeCounter(actualSimulationTime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
//                    status = 5;
                    if (mcp.isTimespotmeasurement()) {
                        //test if next timestep has to be used for value collection & writing
                        if (actualSimulationTime >= nextOpenTime) {
                            openMeasurements = true;
                            for (Pipe pipe : pipes) {
                                if (pipe.getMeasurementTimeLine() != null) {
                                    pipe.getMeasurementTimeLine().active = true;
                                    pipe.getMeasurementTimeLine().resetNumberOfParticles();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            status = 6;
            /**
             * Synchronization finished, give control back to the
             * Threadcontroller via the barrier
             */
            barrier.loopfinished(this);
            status = 7;
        }
    }

    /**
     * Causes this Thread to run off after the current loop.
     */
    public void stopThread() {
        this.runendless = false;
    }

    public void addParticlemeasurement(ParticleMeasurement pm) {
        this.messung.add(pm);
    }

    public void setPipes(Pipe[] pipes) {
        this.pipes = pipes;
    }

    public Pipe[] getPipes() {
        return pipes;
    }

}
