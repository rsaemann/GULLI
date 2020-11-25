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
package com.saemann.gulli.core.model.timeline.sparse;

import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.StorageVolume;

/**
 * If timeline values are needed, they are loaded on time from a dataprovider
 * (e.g. database). safes lots of space.
 *
 * @author saemann
 */
public class SparseTimelineManhole implements TimeLineManhole {

    private final long manholeManualID;

    private final String manholeName;

    private final SparseTimeLineManholeContainer container;

    /**
     * Waterlevel above sea level [m üNN].
     */
    private float[] waterheight;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    private float[] flux;

    private final float soleheight;

    private float actualWaterHeight, actualFlowToSurface, actualWaterlevel;
    private long actualTimestamp;
    private boolean initialized = false;

    public SparseTimelineManhole(SparseTimeLineManholeContainer container, StorageVolume manhole) {
        this.container = container;
        this.manholeManualID = manhole.getManualID();
        this.soleheight = manhole.getSole_height();
        this.manholeName=manhole.getName();
    }

    public SparseTimelineManhole(SparseTimeLineManholeContainer container, long manholeManualID, String manholeName, float soleHeight) {
        this.manholeManualID = manholeManualID;
        this.manholeName = manholeName;
        this.container = container;
        this.soleheight = soleHeight;
    }

    public void setSpilloutFlux(float[] flux) {
        this.flux = flux;
        initialized = flux != null && waterheight != null;
    }

    public void setWaterHeight(float[] waterheight) {
        this.waterheight = waterheight;
        initialized = flux != null && waterheight != null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private float getValue_DoubleIndex(float[] array, double temporalIndexDouble) {
        float i = (float) temporalIndexDouble;
        if (((int) i) >= array.length - 1) {
            return array[array.length - 1];
        }
        float v0 = array[(int) i];
        float v1 = array[(int) i + 1];
        float ratio = i % 1f;
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
        if (actualTimestamp != container.getActualTime()) {
            updateValues();
        }
        return actualWaterHeight;
    }

    @Override
    public float getActualWaterLevel() {
        if (actualTimestamp != container.getActualTime()) {
            updateValues();
        }
        return actualWaterlevel;
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

        if (actualTimestamp != container.getActualTime()) {
            updateValues();
        }
        return actualFlowToSurface;
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    @Override
    public TimeContainer getTimeContainer() {
        return container;
    }

    private void updateValues() {
//        System.out.println(Thread.currentThread().getName()+" try to access SparseTimeline for "+manholeName);
        synchronized (this) {
//            System.out.println(Thread.currentThread().getName()+" accesses SparseTimeline for "+manholeName);

            if (actualTimestamp == container.getActualTime()) {
                //Another thread seems to have calculated the new values while this thread was waiting for the lock release.
                return;
            }
            if (!initialized) {
//                System.out.println(Thread.currentThread().getName()+" load spillout for "+manholeName);
                container.fillTimeline(this, manholeManualID, manholeName);
//                container.loadTimelineSpilloutFlux(this, manholeManualID, manholeName);
            }
//            if (waterheight == null) {
////                System.out.println(Thread.currentThread().getName()+" load waterheight for "+manholeName);
//
//                container.loadTimelineWaterHeight(this, manholeManualID, manholeName);
//            }
            this.actualFlowToSurface = getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
            this.actualWaterHeight = getValue_DoubleIndex(waterheight, container.getActualTimeIndex_double());
            this.actualWaterlevel = actualWaterHeight - soleheight;
            this.actualTimestamp = container.getActualTime();
//            System.out.println(Thread.currentThread().getName()+" leaves SparseTimeline for "+manholeName);

        }
    }

}
