package model.timeline.array;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineManhole implements TimeLineManhole {

    public final ArrayTimeLineManholeContainer container;

//    public static float[] waterZ;
//
//    private static long[] times;
//    private static int numberOfPipes;
//
//    private static int actualTimeIndex = 0;
//    private final int spatialIndex;
    private final int startIndex;
    private float h_max;

//    public static void initStaticInstance(long[] times, int numberOfManholes) {
//        ArrayTimeLineManhole.times = times;
//        ArrayTimeLineManhole.numberOfPipes = numberOfManholes;
//        ArrayTimeLineManhole.waterZ = new float[numberOfManholes * times.length];
//    }
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
    public float getWaterZ(int temporalIndex) {
        return container.waterZ[getIndex(temporalIndex)];
    }

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
    public float getActualWaterZ() {
        try {
            if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
                return (float) this.getValue_DoubleIndex(container.waterZ, container.getActualTimeIndex_double());
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

    /**
     * Outflow from manhole to surface in m³/s.
     *
     * @param temporalIndex
     * @return
     */
    public float getFlowToSurface(int temporalIndex) {
        return container.toSurfaceFlow[getIndex(temporalIndex)];
    }

    /**
     * The Flow from manhole to surface at the current set timestamp. Actual
     * time is set by calling setActualTime on the Container.
     *
     * @return
     */
    public float getActualFlowToSurface() {
        try {
            if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, container.getActualTimeIndex_double());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
                return this.getFlowToSurface(container.getActualTimeIndex());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, container.getActualTimeIndex_double());
            } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
                return (float) this.getValue_DoubleIndex(container.toSurfaceFlow, container.getActualTimeIndex_double());
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
     * Set the Flux from Manhole to surface in m³/s. Inflow to the manhole has
     * negative values.
     *
     * @param value
     * @param timeindex
     */
    public void setFluxToSurface(float value, int timeindex) {
        container.toSurfaceFlow[getIndex(timeindex)] = value;
    }

    private double getValue_DoubleIndex(float[] array, double temporalIndexDouble) {
        double i = startIndex + temporalIndexDouble;
        double v0 = array[(int) i];
        double v1 = array[(int) i + 1];
        double ratio = i % 1;
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
