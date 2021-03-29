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
package com.saemann.gulli.core.model.timeline.sparse;

import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Sämann
 */
public class SparseMeasurementContainer extends MeasurementContainer {

    protected final ArrayList<SparseTimeLineMeasurement> timelines = new ArrayList<>();

    protected double deltaTimeS;

    public SparseMeasurementContainer(TimeContainer times) {
        super();
        this.times = times;
        deltaTimeS = times.getDeltaTimeMS() / 1000.;
        measurementTimes = new long[times.getNumberOfTimes()];
        samplesInTimeInterval = new int[times.getNumberOfTimes()];
    }

    public SparseMeasurementContainer(TimeContainer times, int numberOfMaterials) {
        super(times, numberOfMaterials);
        deltaTimeS = times.getDeltaTimeMS() / 1000.;
        measurementTimes = new long[times.getNumberOfTimes()];
        samplesInTimeInterval = new int[times.getNumberOfTimes()];
    }

    @Override
    public void setIntervalSeconds(double seconds, long startTime, long endTime) {
        //Create timecontainer
        double oldduration = (endTime - startTime) / 1000.;
        int numberOfTimes = (int) (oldduration / seconds + 1);
        long[] t = new long[numberOfTimes];
        for (int i = 0; i < t.length; i++) {
            t[i] = (long) (startTime + i * seconds * 1000);
        }
        TimeContainer tc = new TimeContainer(t);
        times = tc;
        deltaTimeS = seconds;
        clearValues();
    }
    
    

    @Override
    public void setNumberOfMaterials(int numberOfMaterials) {
        super.setNumberOfMaterials(numberOfMaterials); //To change body of generated methods, choose Tools | Templates.
        clearValues();
    }

    @Override
    public void clearValues() {
        measurementTimes = new long[times.getNumberOfTimes()];
        samplesInTimeInterval = new int[times.getNumberOfTimes()];
    
        for (SparseTimeLineMeasurement timeline : timelines) {
            timeline.clearValue();
        }
    }

    /**
     * Replace the old collection of timelines with the new one.
     *
     * @param measurementTimelines
     */
    public void setTimelines(Collection<SparseTimeLineMeasurement> measurementTimelines) {
        clearValues();
        this.timelines.clear();
        this.timelines.addAll(measurementTimelines);
    }

    public double getDeltaTimeS() {
        return deltaTimeS;
    }

}
