/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package model.timeline.sparse;

import model.timeline.array.TimeContainer;
import model.timeline.array.TimeLineManhole;
import model.topology.Manhole;
import model.topology.StorageVolume;

/**
 * If timeline values are needed, they are loaded on time from a dataprovider
 * (e.g. database). safes lots of space.
 *
 * @author saemann
 */
public class SparseTimelineManhole implements TimeLineManhole {

    private long manholeManualID;

    private String manholeName;

    private SparseTimeLineManholeContainer container;

    /**
     * Waterlevel above sea level [m üNN].
     */
    private float[] waterheight;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    private float[] flux;

    public SparseTimelineManhole(SparseTimeLineManholeContainer container, StorageVolume manhole) {
        this.container = container;
        this.manholeManualID = manhole.getManualID();
        if (manhole instanceof Manhole) {
            this.manholeName = ((Manhole) manhole).getName();
        }
    }

    public SparseTimelineManhole(SparseTimeLineManholeContainer container, long pipeManualID, String manholeName) {
        this.manholeManualID = pipeManualID;
        this.manholeName = manholeName;
        this.container = container;
    }

    public void setSpilloutFlux(float[] flux) {
        this.flux = flux;
    }

    public void setWaterHeight(float[] waterlevel) {
        this.waterheight = waterlevel;
    }

    private float getValue_DoubleIndex(float[] array, double temporalIndexDouble) {
        float i = (float) temporalIndexDouble;
        if (((int) i) >= array.length - 1) {
            return array[array.length - 1];
        }
        float v0 = array[(int) i];
        float v1 = array[(int) i + 1];
        float ratio = i % 1;
        return (v0 + (v1 - v0) * ratio);
    }

    @Override
    public float getWaterZ(int temporalIndex) {
        if (waterheight == null) {
            container.loadTimelineWaterHeight(this, manholeManualID, manholeName);
        }
        return waterheight[temporalIndex];
    }

    @Override
    public boolean isWaterlevelIncreasing() {
        double d = container.getActualTimeIndex_double();
        return (waterheight[(int) d] < waterheight[(int) d + 1]);
    }

    @Override
    public float getActualWaterZ() {
        if (waterheight == null) {
            container.loadTimelineWaterHeight(this, manholeManualID, manholeName);
        }
        return getValue_DoubleIndex(waterheight, container.getActualTimeIndex_double());
    }

    @Override
    public float getFlowToSurface(int temporalIndex) {
        if (flux == null) {
            container.loadTimelineSpilloutFlux(this, manholeManualID, manholeName);
        }
        return flux[temporalIndex];
    }

    @Override
    public float getActualFlowToSurface() {
        if (flux == null) {
            container.loadTimelineSpilloutFlux(this, manholeManualID, manholeName);
        }
        return getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    @Override
    public TimeContainer getTimeContainer() {
        return container;
    }
    
    

}
