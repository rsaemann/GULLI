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
package com.saemann.gulli.core.model.surface.measurement;

import com.saemann.gulli.core.control.Controller;
import org.locationtech.jts.geom.Coordinate;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;

/**
 * Standard Surface Measurement Raster thats measurements locations are the same
 * as the surface raster.
 *
 * @author saemann
 */
public class SurfaceMeasurementTriangleRaster extends SurfaceMeasurementRaster {

    protected Surface surf;
    /**
     * Particle Measurements for surface triangle ID
     */
    protected TriangleMeasurement[] measurements;

    volatile public TriangleMeasurement[] monitor;// = new TriangleMeasurement[8];

    protected int numberOfMaterials = 1;

    protected int numberOfParticleThreads;

    /**
     * store the time index for one timestep so it does not need the calculation
     * of the timecontainer each time.
     */
    protected int timeindex = 0;
    protected long lastIndexTime = 0;

    private boolean usedInCurrentStep = false;

    public SurfaceMeasurementTriangleRaster(Surface surf, int numberOfMaterials, TimeIndexContainer time, int numberOfParticleThreads) {
        this.surf = surf;
        this.times = time;
        this.numberOfMaterials = numberOfMaterials;
        this.measurements = new TriangleMeasurement[surf.getTriangleNodes().length];
        this.numberOfParticleThreads = numberOfParticleThreads;
        this.measurementsInTimeinterval = new int[time.getNumberOfTimes()];
        this.durationInTimeinterval = new double[time.getNumberOfTimes()];

        measurementTimestamp = new long[measurementsInTimeinterval.length];
    }

    public static SurfaceMeasurementTriangleRaster init(Controller c) {
        int numberMaterials = 0;
        if (c.getScenario() != null) {
            numberMaterials = c.getScenario().getMaxMaterialID() + 1;
            if (c.getScenario().getMaterials() != null) {
                numberMaterials = Math.max(numberMaterials, c.getScenario().getMaterials().size());
            }
        }
        TimeIndexContainer tic;
        if (c.getSurface().getMeasurementRaster() != null) {
            tic = c.getSurface().getMeasurementRaster().getIndexContainer();
        } else {
            System.out.println(SurfaceMeasurementTriangleRaster.class+"::init    surface.times="+c.getSurface().getTimes());
            tic = new TimeIndexContainer(c.getSurface().getTimes());
        }
        SurfaceMeasurementTriangleRaster smr = new SurfaceMeasurementTriangleRaster(c.getSurface(), numberMaterials, tic, c.getThreadController().getNumberOfParallelThreads());
        return smr;
    }

    @Override
    public void measureParticle(long time, Particle particle, double residenceDuration, int threadIndex) {
        if (!continousMeasurements && !measurementsActive) {
            return;
        }
//        statuse[threadIndex] = 0;
        if (particle.getTravelledPathLength() < minTravelLengthToMeasure) {
            //for risk map do not show inertial particles
            return;
        }

        if (particle.getTravelledPathLength() < particle.getMaterial().travellengthToMeasure) {
            return;
        }
//        statuse[threadIndex] = 1;
        int id = particle.surfaceCellID;

        if (id < 0) {
            return;
        }

//        statuse[threadIndex] = 2;
        if (!countStayingParticle) {
//            statuse[threadIndex] = 3;
            if (id == particle.lastSurfaceCellID) {
                //Do not count particle, if it only stays at the same cell
                return;
            } else {
                particle.lastSurfaceCellID = id;
            }
        }
        if (time != this.lastIndexTime) {
            //calculate the new index
            this.timeindex = times.getTimeIndex(time);
            this.lastIndexTime = time;
        }
        if (!usedInCurrentStep) {
            usedInCurrentStep = true;
        }
        try {
            int materialIndex = particle.getMaterial().materialIndex;
            TriangleMeasurement m = measurements[id];
            if (m == null) {
                m = createMeasurement(id);
            }
//            if (synchronizeOnlyAtEnd) {
//                m.massresidence[materialIndex][threadIndex] += particle.getParticleMass();
//                m.particlecounter[materialIndex][threadIndex]++;
//                if (!m.used) {
//                    m.used = true;
//                }
//
//            } else 
            if (synchronizeMeasures) {
                if (m == null) {
                    System.err.println("monitor object is null for cell triangle " + id);
                } else {
                    monitor[threadIndex] = m;
                    m.lock.lock();
                    try {
                        m.massresidence[materialIndex][timeindex] += particle.particleMass * residenceDuration;
                        m.particlecounter[materialIndex][timeindex] += residenceDuration;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        m.lock.unlock();
                        monitor[threadIndex] = null;
                    }
                }
            } else {
                m.massresidence[materialIndex][timeindex] += particle.particleMass * residenceDuration;
                m.particlecounter[materialIndex][timeindex] += residenceDuration;
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            System.err.println(getClass() + "::Request t=" + timeindex + " m=" + particle.getMaterial().materialIndex + "  times.length=" + times.getNumberOfTimes());
        } catch (Exception ex) {
            ex.printStackTrace();
        } catch (Error er) {
            er.printStackTrace();
        }
    }

    /**
     * Creates a new Triangle Measurment, puts it into the hashMap and connects
     * it to a SurfaceTriangle if exitent.
     *
     * @param triangleID Id of the triangle
     * @return
     */
    public TriangleMeasurement createMeasurement(int triangleID) {

        synchronized (measurements) {
            if (this.measurements[triangleID] != null) {
                return this.measurements[triangleID];
            } else {
                TriangleMeasurement tm = new TriangleMeasurement(triangleID, times.getNumberOfTimes(), numberOfMaterials, numberOfParticleThreads);
                this.measurements[triangleID] = tm;
                return tm;
            }
        }

    }

    @Override
    public void synchronizeMeasurements() {
//        if (/*!synchronizeOnlyAtEnd*/true) {
//            return;
//        }
//        if (!usedInCurrentStep) {
//            return;//no need to go through the whole list.
//        }
////        for (TriangleMeasurement measurement : measurements) {
////            if (measurement == null) {
////                continue;
////            }
////            measurement.synchronizeMeasurements(timeindex);
////        }
//        usedInCurrentStep = false;
    }

    @Override
    public void setNumberOfMaterials(int numberOfMaterials) {
        if (this.numberOfMaterials == numberOfMaterials) {
            return;
        }
        this.numberOfMaterials = numberOfMaterials;
        for (int i = 0; i < measurements.length; i++) {
            if (measurements[i] != null) {
                createMeasurement(i);
            }
        }
    }

    public TriangleMeasurement[] getMeasurements() {
        return measurements;
    }

    @Override
    public void setTimeContainer(TimeIndexContainer times) {
        this.times = times;
//        System.out.println(getClass() + "::setTimeContainer to " + this.times.getNumberOfTimes());
        for (int i = 0; i < measurements.length; i++) {
            if (measurements[i] != null) {
                createMeasurement(i);
            }
        }
        measurementsInTimeinterval = new int[times.getNumberOfTimes()];
        measurementTimestamp = new long[measurementsInTimeinterval.length];
        durationInTimeinterval = new double[measurementsInTimeinterval.length];
    }

    @Override
    public void reset() {
        usedInCurrentStep = false;
        measurements = new TriangleMeasurement[surf.getTriangleNodes().length];
        measurementsInTimeinterval = new int[times.getNumberOfTimes()];
        measurementTimestamp = new long[measurementsInTimeinterval.length];
        durationInTimeinterval = new double[measurementsInTimeinterval.length];
    }

    public Surface getSurface() {
        return surf;
    }

    @Override
    public int getNumberOfMaterials() {
        return numberOfMaterials;
    }

    @Override
    public void breakAllLocks() {
        if (measurements != null) {
            for (int i = 0; i < measurements.length; i++) {
                TriangleMeasurement measurement = measurements[i];
                if (measurement == null) {
                    continue;
                }
                if (measurement.lock != null) {
                    continue;
                }
                if (measurement.lock.isLocked()) {
                    System.out.println("Free lock on cell " + i + " , where " + measurement.lock.getHoldCount() + " threads are waiting.");
                    measurement.lock.unlock();
                }
            }
        }
    }

    @Override
    public void setNumberOfThreads(int threadCount) {
        this.monitor = new TriangleMeasurement[threadCount];
        this.statuse = new int[threadCount];
    }

    @Override
    public int getNumberOfCells() {
        return surf.getTriangleMids().length;
    }

    @Override
    public Coordinate getCenterOfCell(int cellindex) {
        double[] xyz = surf.getTriangleMids()[cellindex];
        return new Coordinate(xyz[0], xyz[1], xyz[2]);
    }

    @Override
    public double getMassInCell(int cellIndex, int timeindex, int materialIndex) {
        if (measurementsInTimeinterval[timeindex] == 0) {
            return 0;
        }
        return measurements[cellIndex].massresidence[materialIndex][timeindex] / durationInTimeinterval[timeindex];
    }

    @Override
    public double getRawMassInCell(int cellIndex, int timeindex, int materialIndex) {
        return measurements[cellIndex].massresidence[materialIndex][timeindex];
    }

    /**
     *
     * @param cellIndex
     * @return
     */
    @Override
    public boolean isCellContaminated(int cellIndex) {
        return measurements[cellIndex] != null;
    }

    @Override
    public double getRawNumberOfParticlesInCell(int cellIndex, int timeindex, int materialIndex) {
        return measurements[cellIndex].particlecounter[materialIndex][timeindex] / durationInTimeinterval[timeindex];
    }

}
