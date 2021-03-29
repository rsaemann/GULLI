/*
 * The MIT License
 *
 * Copyright 2021 B1.
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
package com.saemann.gulli.core.model.timeline;

import com.saemann.gulli.core.model.timeline.array.TimeContainer;

/**
 * Holds global information important for all Sparse Measurement Timeline
 * Onjects
 *
 * @author Robert Sämann
 */
public abstract class MeasurementContainer {

    public static boolean synchronizeMeasures = true;

    public static boolean timecontinuousMeasures = true;

    public static boolean spatialConsistentMeasures = true;

    /**
     * Samples are only taken if this is true. Can be switched of, to save
     * computation cost. SYnchronisation Thread switches this flag on and off.
     */
    public boolean measurementsActive = true;

    /**
     * Is it only recorded once per timeindex?
     */
//    protected boolean timespotmeasurement = false;
    protected int numberOfMaterials = 1;

    protected TimeContainer times;

    protected int actualTimeIndex = 0;

    public long[] measurementTimes;

//    /**
//     * Indicates how many samples are taken during one sampling interval. This
//     * variable is only for debugging and is not used for the calculation.
//     */
//    public double samplesPerTimeinterval = 1;
    /**
     * Number of samples in this timeinterval. Important if the global number
     * does not fit the interval number. E.g in the very first and very last
     * interval;
     */
    public int[] samplesInTimeInterval;

    public MeasurementContainer() {
    }

    public MeasurementContainer(TimeContainer times, int numberOfMaterials) {
        this.times = times;
        this.numberOfMaterials = numberOfMaterials;
    }

    public TimeContainer getTimes() {
        return times;
    }

    public long getTimeMillisecondsAtIndex(int timeIndex) {
        return times.getTimeMilliseconds(timeIndex);
    }

    public int getIndexForTime(long time) {
        return times.getTimeIndex(time);
    }

    public void setActualTime(long timeMS) {
        actualTimeIndex = times.getTimeIndex(timeMS);
    }

    public int getActualTimeIndex() {
        return actualTimeIndex;
    }

    public int getNumberOfMaterials() {
        return numberOfMaterials;
    }

//    public boolean isTimespotmeasurement() {
//        return !timecontinuousMeasures;
//    }
    public void setNumberOfMaterials(int numberOfMaterials) {
        this.numberOfMaterials = numberOfMaterials;
    }

//    public void OnlyRecordOncePerTimeindex() {
//        timecontinuousMeasures = false;
////        this.samplesPerTimeinterval = 1;
//    }
//    public void setSamplesPerTimeindex(double recordsPerTimeindex) {
//        timecontinuousMeasures= true;
////        this.samplesPerTimeinterval = recordsPerTimeindex;
//    }
    /**
     * Measurement timestamps can vary from the time when they should be taken.
     * This returns the simulationtime when the samples were taken.
     *
     * @param timeindex
     * @return timestamp of the sample
     */
    public long getMeasurementTimestampAtTimeIndex(int timeindex) {
        return measurementTimes[timeindex];
    }

    public int getSamplesInTimeInterval(int timeIndex) {
        return samplesInTimeInterval[timeIndex];
    }

    /**
     *
     * @param seconds
     * @param startTime
     * @param endTime
     */
    public abstract void setIntervalSeconds(double seconds, long startTime, long endTime);

    /**
     * Clear all values before startng a new simulation run.
     */
    public abstract void clearValues();
}
