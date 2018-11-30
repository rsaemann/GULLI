package model.timeline.array;

import java.util.Date;

/**
 * Holder of performant storing Arrays for ArrayTimeLines.
 *
 * @author saemann
 */
public class ArrayTimeLinePipeContainer extends TimeIndexContainer {

//    public static ArrayTimeLinePipeContainer instance;
//    private TimeContainer time;
//    private long actualTime;
//    private int actualTimeIndex;
//    private double actualTimeIndex_double;
//    private int actualTimeIndex_shift;

    /**
     * Velocity for pipes in [m/s].
     */
    public float[] velocity;
    /**
     * Waterlevel above pipe sole [m]
     */
    public float[] waterlevel;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    public float[] flux;
    /**
     * Reference mass if given in scenario. May be null
     */
    public float[] mass_reference;

    private int numberOfPipes;

    public float[] moment1, moment2;
    public float[] distance;

    

    

    public enum CALCULATION {

        LINEAR_INTERPOLATE, STEPS, MEAN, MAXIMUM
    }
    public CALCULATION calculaion_Method = CALCULATION.LINEAR_INTERPOLATE;

    public ArrayTimeLinePipeContainer(TimeContainer time, int numberOfPipes) {
        super(time);
        this.numberOfPipes = numberOfPipes;
        this.velocity = new float[numberOfPipes * time.getNumberOfTimes()];
        this.flux = new float[numberOfPipes * time.getNumberOfTimes()];
        this.waterlevel = new float[numberOfPipes * time.getNumberOfTimes()];
//        this.mass_reference = new float[numberOfPipes * time.getNumberOfTimes()];
    }

    public ArrayTimeLinePipeContainer(long[] times, int numberOfPipes) {
        this(new TimeContainer(times), numberOfPipes);
    }

    public float[] getVelocityForTimeIndex(int timeIndex) {
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = velocity[i * getNumberOfTimes() + timeIndex];
        }
        return r;
    }

    public float[] getWaterlevelsForTimeIndex(int timeIndex) {
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = waterlevel[i * getNumberOfTimes() + timeIndex];
        }
        return r;
    }

    public float[] getMassForTimeIndex(int timeIndex) {
        if (mass_reference == null) {
            throw new NullPointerException("No reference mass in scenario applied to " + this.getClass());
        }
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = mass_reference[i * getNumberOfTimes() + timeIndex];
        }
        return r;
    }

    public double getMomentum1_xc(int timeIndex) {
        if (mass_reference == null) {
            throw new NullPointerException("No reference mass in scenario applied to " + this.getClass());
        }
        double zaehler = 0;
        double nenner = 0;
        double c = 0;
        for (int i = 0; i < distance.length; i++) {
            c = mass_reference[i * getNumberOfTimes() + timeIndex];
            zaehler += distance[i] * c;
            nenner += c;
        }
        return zaehler / nenner;
    }

    public double getMomentum2_xc(int timeIndex) {
        return moment2[timeIndex];
    }
    
    @Override
    public long getStartTime() {
        return getFirstTime();
    }

    @Override
    public long getEndTime() {
        return getLastTime();
    }
    
    void initMass_Reference() {
         this.mass_reference = new float[velocity.length];
    }

//    @Override
//    public void setActualTime(long actualTime) {
//        System.out.println(getClass()+":: set time to "+new Date(actualTime));
//        super.setActualTime(actualTime); //To change body of generated methods, choose Tools | Templates.
//    }
    
    
}
