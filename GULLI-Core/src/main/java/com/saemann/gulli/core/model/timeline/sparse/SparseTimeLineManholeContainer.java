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

import com.saemann.gulli.core.io.SparseTimeLineDataProvider;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;

/**
 * This container only loads some of the needed timeseries. Others can be loaded
 * if neccessary
 *
 * @author saemann
 */
public class SparseTimeLineManholeContainer extends TimeIndexContainer {

    public static boolean verboseLaodingRequests = false;

    /**
     * IO class to load distinct timeline information when needed.
     */
    private SparseTimeLineDataProvider dataprovider;

    public SparseTimeLineManholeContainer(SparseTimeLineDataProvider dataprovider, boolean shiftTimesToZero) {
        super(dataprovider.loadTimeStepsNetwork(shiftTimesToZero));
        this.dataprovider = dataprovider;
    }

    public SparseTimeLineManholeContainer(SparseTimeLineDataProvider dataprovider, TimeContainer timeC) {
        super(timeC);
        this.dataprovider = dataprovider;
    }

    public SparseTimeLineManholeContainer(long[] times) {
        super(times);
    }

    public SparseTimeLineManholeContainer(TimeContainer cont) {
        super(cont);
    }

    public void setDataprovider(SparseTimeLineDataProvider dataprovider) {
        this.dataprovider = dataprovider;
    }

    public void loadTimelineWaterHeight(SparseTimelineManhole tl, long manholeManualId, String manholeName) {
        if (verboseLaodingRequests) {
            System.out.println(getClass() + ": request loading Waterlevel timeline for manhole '" + manholeName + "' / " + manholeManualId);
        }

        tl.setWaterHeight(dataprovider.loadTimeLineWaterheightManhole(manholeManualId, manholeName, getNumberOfTimes()));

    }

    public void loadTimelineSpilloutFlux(SparseTimelineManhole tl, long manholeManualId, String manholeName) {
        if (verboseLaodingRequests) {
            System.out.println(getClass() + ": request loading Flux timeline for manhole '" + manholeName + "' / " + manholeManualId);
        }

        tl.setSpilloutFlux(dataprovider.loadTimeLineSpilloutFlux(manholeManualId, manholeName, getNumberOfTimes()));
    }

    public void fillTimeline(SparseTimelineManhole tl, long manholeManualId, String manholeName) {
        if (verboseLaodingRequests) {
            System.out.println(getClass() + ": request fill timeline for manhole '" + manholeName + "' / " + manholeManualId);
        }
        dataprovider.fillTimelineManhole(manholeManualId, manholeName, tl);
    }

}
