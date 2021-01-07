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

import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import org.locationtech.jts.geom.Coordinate;

/**
 * Measures the contamination of surface
 *
 * @author saemann
 */
public abstract class SurfaceMeasurementRaster {

    /**
     * if particles have lower travel length than this, they are not measured.
     */
    public static double minTravelLengthToMeasure = -1;

    /**
     * If enabled, only particles, that have been to the pipesystem before are
     * measured on the surface.
     */
    public static boolean measureSpilloutParticlesOnly = false;

    /**
     * if false, a particle is only sampled, when it enters the cell. Stying
     * inside this cell would then prevent it from beeon counted
     */
    public static boolean countStayingParticle = true;

    /**
     * if true, parallel threads have to wait when writing samples at the same
     * sample index. When disabled, the writing is faster, but overriding
     * samples might occur.
     */
    public static boolean synchronizeMeasures = true;

    /**
     * This is set to true by the Threadcontroller if sampling of particles
     * should be active during the current simulation loop.
     */
    public boolean measurementsActive = true;

    /**
     * This is set by the user, if samples should be taken at the end of each
     * simulation loop. If false, the samples are only taken at discrete
     * timesteps. When this is enabled, the number of particles is increased in
     * every loop. To calculate the mean value during this sampling period, you
     * will have to divide the particle counter by the number of measurements
     * taken in the sample period.
     */
    public boolean continousMeasurements = true;

    /**
     * If enabled, particles will be counted on every cell they visit, Otherwise
     * they are only counted in the final cell.
     */
    public boolean spatialConsistency = true;

    /**
     * Number of samples taken during the Sampling interval. This is usually 1
     * if continuous sampling is disabled. The counter is increased at the end
     * of each sampling.
     */
    public int[] measurementsInTimeinterval;
    
    /**
     * Seconds sampled during the Sampling interval.  The counter is increased at the end
     * of each sampling.
     */
    public double[] durationInTimeinterval;

    /**
     * The LAST timestamp of a sampling (end of simulation loop) for the current
     * writeindex.
     */
    public long[] measurementTimestamp;

    protected TimeIndexContainer times;

    /**
     * Indicates at which timeindex the current sample should be written. This
     * is set by the ThreadController at the begin of a simulation loop.
     */
    protected int writeIndex = 0;

    /**
     *
     * @param time current time at the moment of sampling
     * @param particle the particle to sample
     * @param residenceTime in seconds
     * @param threadIndex the number of the thread, which performs this sampling
     * call
     */
    public abstract void measureParticle(long time, Particle particle, double residenceTime, int threadIndex);

    public abstract void setNumberOfMaterials(int numberOfMaterials);

    public abstract int getNumberOfMaterials();

    /**
     * The total number of possible cells on the surface. This is needed for
     * displaying purpose.
     *
     * @return
     */
    public abstract int getNumberOfCells();

    public abstract boolean isCellContaminated(int cellIndex);

    public abstract Coordinate getCenterOfCell(int cellindex);

    /**
     * The mass (in kg) of the materiel (with materialIndex). Note that this
     * method returns the mean value during sampling intervals of continuous
     * sampling. If continuous sampling is disabled, the snapshot at the
     * timeindex is returned.
     *
     * @param cellIndex
     * @param timeindex
     * @param materialIndex
     * @return mass in kg in the cell
     */
    public abstract double getMassInCell(int cellIndex, int timeindex, int materialIndex);

    /**
     * The mass (in kg) of the materiel (with materialIndex). Note that this
     * method returns the total counted mass during intervals of continuous
     * sampling. This need to be divided by the number of samples to get the
     * mean mass during the sampling period. If continuous sampling is disabled,
     * the snapshot at the timeindex is returned.
     *
     * @param cellIndex
     * @param timeindex
     * @param materialIndex
     * @return mass in kg in the cell
     */
    public abstract double getRawMassInCell(int cellIndex, int timeindex, int materialIndex);

    public abstract double getRawNumberOfParticlesInCell(int cellIndex, int timeindex, int materialIndex);

    public abstract void setTimeContainer(TimeIndexContainer times);

    public TimeIndexContainer getIndexContainer() {
        return times;
    }

    public int status = -1;

    public int[] statuse;// = new int[8];

    public abstract void reset();

    /**
     * Initialize the threadsafe component.
     *
     * @param threadCount
     */
    public abstract void setNumberOfThreads(int threadCount);

    /**
     * This is called, when a Thread blocks the simulation.
     */
    public abstract void breakAllLocks();

    /**
     * CAlls the raster to count and store its measurements at the end of each
     * loop. Some Rasters might need a synchronization step at the end of each
     * simulation loop.
     */
    public abstract void synchronizeMeasurements();

    public void setIntervalSeconds(double seconds, long startTime, long endTime) {
//          System.out.println("change raster interval to "+seconds);
        if (times != null && times.getDeltaTimeMS() / 1000. == seconds) {
//            System.out.println("do not change interval, same length");
            //Nothing changed
            return;
        }
        //Create timecontainer
        double oldduration = (endTime - startTime) / 1000.;
        int numberOfTimes = (int) (oldduration / seconds + 1);
        long[] t = new long[numberOfTimes];
        for (int i = 0; i < t.length; i++) {
            t[i] = (long) (startTime + i * seconds * 1000);
        }
//        TimeContainer tc = new TimeContainer(t);
//        double samplesPerTimeinterval = (tc.getDeltaTimeMS() / 1000.) / ThreadController.getDeltaTime();
        this.setTimeContainer(new TimeIndexContainer(t));

    }

    public void setWriteIndex(int writeIndex) {
        this.writeIndex = writeIndex;
    }

}
