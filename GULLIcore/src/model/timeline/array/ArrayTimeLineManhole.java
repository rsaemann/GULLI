package model.timeline.array;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineManhole implements TimeLineManhole {

    public final ArrayTimeLineManholeContainer container;

    private final int startIndex;
    private float h_max;

    public ArrayTimeLineManhole(ArrayTimeLineManholeContainer container, int spatialIndex) {
        this.container = container;
        this.startIndex = spatialIndex * container.getNumberOfTimes();
    }

    /**
     * Calculates the Index in the container's array representing the timeinde
     * in this Timeline. (adding an Offset to this index that is needed to find
     * listed values of this timeline in the 1D Array of the container.)
     *
     * @param temporalIndex
     * @return index to be used in the container's array.
     */
    private int getIndex(int temporalIndex) {
        return startIndex + temporalIndex;
    }

    /**
     * Get water height above sealevel Wasserstand über NN.
     *
     * @param temporalIndex
     * @return
     */
    @Override
    public float getWaterZ(int temporalIndex) {
        return container.waterZ[getIndex(temporalIndex)];
    }

    @Override
    public boolean isWaterlevelIncreasing() {
        int index = container.getActualTimeIndex();
        if (index >= container.getNumberOfTimes() - 1) {
            return false;
        }
        return container.waterZ[startIndex + index + 1] > container.waterZ[startIndex + index];
    }

    /**
     * The waterheight above sealevel at the current set timestamp. Actual time
     * is set by calling setActualTime on the Container.
     *
     * @return
     */
    @Override
    public float getActualWaterZ() {
        try {
            if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
                return (float) this.getValue_DoubleIndex(container.waterZ, (float) container.getActualTimeIndex_double());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
                return this.getWaterZ(container.getActualTimeIndex());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
                return h_max;
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
                return h_max;
            }
            return this.getWaterZ(container.getActualTimeIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public float getActualWaterLevel() {
        return (float) this.getValue_DoubleIndex(container.waterZ, (float) container.getActualTimeIndex_double());
    }

    /**
     * Outflow from manhole to surface in m³/s.
     *
     * @param temporalIndex
     * @return
     */
    @Override
    public float getFlowToSurface(int temporalIndex) {
        return container.toSurfaceFlow[getIndex(temporalIndex)];
    }

    /**
     * The Flow from manhole to surface at the current set timestamp. Actual
     * time is set by calling setActualTime on the Container.
     *
     * @return
     */
    @Override
    public float getActualFlowToSurface() {
        try {
            if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, (float) container.getActualTimeIndex_double());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
                return this.getFlowToSurface(container.getActualTimeIndex());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, (float) container.getActualTimeIndex_double());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, (float) container.getActualTimeIndex_double());
            }
            return this.getFlowToSurface(container.getActualTimeIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Set the height of water above sealevel.
     *
     * @param value
     * @param temporalIndex
     */
    public void setWaterZ(float value, int temporalIndex) {
        container.waterZ[getIndex(temporalIndex)] = value;
    }

    /**
     * Set the height of water above soleheight.
     *
     * @param value
     * @param temporalIndex
     */
    public void setWaterLevel(float value, int temporalIndex) {
        container.waterLevel[getIndex(temporalIndex)] = value;
    }

    /**
     * Set the Flux from Manhole to surface in m³/s. Inflow to the manhole has
     * negative values.
     *
     * @param value
     * @param timeindex
     */
    public void setFluxToSurface(float value, int timeindex) {
        container.toSurfaceFlow[getIndex(timeindex)] = value;
    }

    private float getValue_DoubleIndex(float[] array, float temporalIndexDouble) {
        float i = startIndex + temporalIndexDouble;
        float v0 = array[(int) i];
        float v1 = array[(int) i + 1];
        float ratio = i % 1f;
        return (v0 + (v1 - v0) * ratio);
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    @Override
    public TimeIndexContainer getTimeContainer() {
        return container;
    }

}
