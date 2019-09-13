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

import io.SparseTimeLineDataProvider;
import model.timeline.array.TimeContainer;
import model.timeline.array.TimeIndexContainer;

/**
 * This container only loads some of the needed timeseries. Others can be loaded
 * if neccessary
 *
 * @author saemann
 */
public class SparseTimeLinePipeContainer extends TimeIndexContainer {

    /**
     * If true, detailed information about a loading request for empty timelines
     * will be printed to console out.
     */
    public static boolean verboseRequests = false;
    /**
     * IO class to load distinct timeline information when needed.
     */
    private SparseTimeLineDataProvider dataprovider;

    private boolean hasReferencePollution = false;

    public SparseTimeLinePipeContainer(SparseTimeLineDataProvider dataprovider) {
        super(dataprovider.loadTimeStepsNetwork());
        this.dataprovider = dataprovider;
        if (dataprovider != null) {
            hasReferencePollution = dataprovider.hasTimeLineMass();
        }
    }

    public SparseTimeLinePipeContainer(long[] times) {
        super(times);
    }

    public SparseTimeLinePipeContainer(TimeContainer cont) {
        super(cont);
    }

    public void setDataprovider(SparseTimeLineDataProvider dataprovider) {
        this.dataprovider = dataprovider;
        if (dataprovider != null) {
            hasReferencePollution = dataprovider.hasTimeLineMass();
        }
    }

    public void loadTimelineVelocity(SparseTimelinePipe tl, long pipeManualId, String pipeName) {
        if (verboseRequests) {
            System.out.println(getClass() + ": request loading Velocity timeline for pipe " + pipeName + " / " + pipeManualId);
        }
        dataprovider.fillTimelinePipe(pipeManualId, pipeName, tl);
//        tl.setVelocity(dataprovider.loadTimeLineVelocity(pipeManualId, pipeName,getNumberOfTimes()));
    }

    public void loadTimelineWaterlevel(SparseTimelinePipe tl, long pipeManualId, String pipeName) {
        if (verboseRequests) {
            System.out.println(getClass() + ": request loading Waterlevel timeline for pipe " + pipeName + " / " + pipeManualId);
        }
//        System.out.println("Load SParseTimeline: Waterleveltimeline");
        dataprovider.fillTimelinePipe(pipeManualId, pipeName, tl);
//        tl.setWaterlevel(dataprovider.loadTimeLineWaterlevel(pipeManualId, pipeName, getNumberOfTimes()));
    }

    public void loadTimelineFlux(SparseTimelinePipe tl, long pipeManualId, String pipeName) {
        if (verboseRequests) {
            System.out.println(getClass() + ": request loading Flux timeline for pipe " + pipeName + " / " + pipeManualId);
        }

        dataprovider.fillTimelinePipe(pipeManualId, pipeName, tl);
//        tl.setFlux(dataprovider.loadTimeLineFlux(pipeManualId, pipeName, getNumberOfTimes()));
    }

    public void loadTimelineMass(SparseTimelinePipe tl, long pipeManualId, String pipeName) {
        if (verboseRequests) {
            System.out.println(getClass() + ": request loading Mass timeline for pipe " + pipeName + " / " + pipeManualId);
        }
        tl.setMass_reference(dataprovider.loadTimeLineMass(pipeManualId, pipeName, getNumberOfTimes()));
    }
    
    public void loadTimelineConcentration(SparseTimelinePipe tl, long pipeManualId, String pipeName) {
        if (verboseRequests) {
            System.out.println(getClass() + ": request loading Concentration timeline for pipe " + pipeName + " / " + pipeManualId);
        }
        tl.setConcentration_reference(dataprovider.loadTimeLineConcentration(pipeManualId, pipeName, getNumberOfTimes()));
    }

    public boolean hasReferencePollution() {
        return hasReferencePollution;
    }

}
