package model.timeline.array;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineManholeContainer extends TimeIndexContainer {

    /**
     * Waterheight [above sea level/ üNN].
     */
    public final float[] waterZ;
    /**
     * NettoFlux from pipe system to surface [m³/s]. Nettofluss aus dem Schacht
     * an die Oberfläche.
     */
    public final float[] toSurfaceFlow;

    public ArrayTimeLinePipeContainer.CALCULATION calculaion_Method = ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE;

    public ArrayTimeLineManholeContainer(TimeContainer time, int numberOfManholes) {
        super(time);
        this.waterZ = new float[numberOfManholes * time.getNumberOfTimes()];
        this.toSurfaceFlow = new float[numberOfManholes * time.getNumberOfTimes()];
    }

    public ArrayTimeLineManholeContainer(long[] time, int numberOfManholes) {
        this(new TimeContainer(time), numberOfManholes);
    }
}
