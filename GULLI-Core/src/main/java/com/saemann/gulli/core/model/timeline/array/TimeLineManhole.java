package com.saemann.gulli.core.model.timeline.array;

/**
 *
 * @author saemann
 */
public interface TimeLineManhole {

    /**
     * Get water height above sealevel Wasserstand über NN.
     *
     * @param temporalIndex
     * @return
     */
    public float getWaterZ(int temporalIndex);
    
    public float getInflow(int temporalIndex);

    public boolean isWaterlevelIncreasing();

    /**
     * The waterheight above sealevel at the current set timestamp. Actual time
     * is set by calling setActualTime on the Container.
     *
     * @return
     */
    public float getActualWaterZ();

    /**
     * The waterheight above sole at the current set timestamp. Actual time is
     * set by calling setActualTime on the Container.
     *
     * @return
     */
    public float getActualWaterLevel();

    /**
     * Outflow from manhole to surface in m³/s.
     *
     * @param temporalIndex
     * @return
     */
    public float getFlowToSurface(int temporalIndex);

    /**
     * The Flow from manhole to surface at the current set timestamp. Actual
     * time is set by calling setActualTime on the Container.
     *
     * @return
     */
    public float getActualFlowToSurface();

    /**
     * Number of timestamps where values are known.
     *
     * @return
     */
    public int getNumberOfTimes();

    /**
     * Holds information about stored timestamps
     *
     * @return
     */
    public TimeContainer getTimeContainer();

}
