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
package com.saemann.gulli.core.io;

import java.util.Collection;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelineManhole;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelinePipe;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;

/**
 * Loads neccessary timeline information for SparseTimeLinepipe
 *
 * @author saemann
 */
public interface SparseTimeLineDataProvider {

    public float[] loadTimeLineVelocity(long pipeMaualID, String pipeName, int numberOfTimes);

    public float[] loadTimeLineWaterlevel(long pipeMaualID, String pipeName, int numberOfTimes);

    public float[] loadTimeLineWaterheightManhole(long ManholeID, String manholeName, int numberOfTimes);

    public float[] loadTimeLineFlux(long pipeMaualID, String pipeName, int numberOfTimes);

    public float[] loadTimeLineSpilloutFlux(long ManholeID, String manholeName, int numberOfTimes);

    public float[][] loadTimeLineMass(long pipeMaualID, String pipeName, int numberOfTimes);

    public float[][] loadTimeLineConcentration(long pipeMaualID, String pipeName, int numberOfTimes);

    public String[] loadNamesMaterials();

    /**
     * Used for filling existing empty timelines on demand.
     *
     * @param manholeManualID
     * @param manholeName
     * @param timeline
     * @return
     */
    public boolean fillTimelineManhole(long manholeManualID, String manholeName, SparseTimelineManhole timeline);

    /**
     * Returns true if the result data contains information about mass
     * concentration.
     *
     * @return
     */
    public boolean hasTimeLineMass();

    public void loadTimelineManholes(Collection<StorageVolume> manholes, SparseTimeLineManholeContainer container);

//    public SparseTimelinePipe loadTimelinePipe(long pipeManualID, String pipeName, SparseTimeLinePipeContainer container);
    public SparseTimelinePipe loadTimelinePipe(Pipe pipe, SparseTimeLinePipeContainer container);

    /**
     * Used for filling existing empty timelines on demand.
     *
     * @param pipeManualID
     * @param pipeName
     * @param timeline
     * @return
     */
    public boolean fillTimelinePipe(long pipeManualID, String pipeName, SparseTimelinePipe timeline);

    /**
     * Used when initially loading timelines for preselected pipes.
     *
     * @param pipesToLoad
     * @param container
     */
    public void loadTimelinePipes(Collection<Pipe> pipesToLoad, SparseTimeLinePipeContainer container);

    /**
     * Fill timeline arrays of an existing timeline on demand
     *
     * @param manholeManualID
     * @param manholeName
     * @param soleheight ground of manhole above sealevel
     * @param container
     * @return
     */
    public SparseTimelineManhole loadTimelineManhole(long manholeManualID, String manholeName, float soleheight, SparseTimeLineManholeContainer container);

    /**
     * Return the timesteps of the output timeseries of a scenario.
     *
     * @param startAtZero shift all timesteps by the very first timestep to
     * start the event at zero.
     * @return
     */
    public long[] loadTimeStepsNetwork(boolean startAtZero);

}
