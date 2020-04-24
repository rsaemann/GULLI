package model.timeline.array;

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
     * Watervolume in Pipe [m³]
     */
    public float[] volume;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    public float[] discharge;
    /**
     * Reference massflux [kg/s] if given in scenario. May be null
     */
    public float[][] massflux_reference;
    /**
     * Reference concentration [kg/m³] if given in scenario. May be null
     */
    public float[][] concentration_reference;

    private int numberOfPipes, numberOfMaterials;

    public float[] moment1, moment2;
    public float[] distance;

    public enum CALCULATION {

        LINEAR_INTERPOLATE, STEPS, MEAN, MAXIMUM
    }
    public CALCULATION calculaion_Method = CALCULATION.LINEAR_INTERPOLATE;

    public ArrayTimeLinePipeContainer(TimeContainer time, int numberOfPipes) {
        this(time, numberOfPipes, 0);
    }

    public ArrayTimeLinePipeContainer(TimeContainer time, int numberOfPipes, int numberOfMaterials) {
        super(time);
//        System.out.println("  number of times:"+time.getNumberOfTimes());
        this.numberOfPipes = numberOfPipes;
//        System.out.println("  velocities: ");
        this.velocity = new float[numberOfPipes * time.getNumberOfTimes()];
        this.volume = new float[numberOfPipes * time.getNumberOfTimes()];
        this.discharge = new float[numberOfPipes * time.getNumberOfTimes()];
//        System.out.println("  waterlevels: ");
        this.waterlevel = new float[numberOfPipes * time.getNumberOfTimes()];
        this.numberOfMaterials = numberOfMaterials;
        if (numberOfMaterials > 0) {
//            System.out.println("  massflux for "+numberOfMaterials+" materials");
            this.massflux_reference = new float[velocity.length][numberOfMaterials];
//            System.out.println("   ref: "+massflux_reference.length);
            this.concentration_reference = new float[velocity.length][numberOfMaterials];
        }
//        System.out.println("  all finished: ");
    }

    public ArrayTimeLinePipeContainer(long[] times, int numberOfPipes) {
        this(new TimeContainer(times), numberOfPipes,0);
    }
    
     public ArrayTimeLinePipeContainer(long[] times, int numberOfPipes,int numberOfMaterials) {
        this(new TimeContainer(times), numberOfPipes,numberOfMaterials);
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

    public float[] getVolumesForTimeIndex(int timeIndex) {
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = volume[i * getNumberOfTimes() + timeIndex];
        }
        return r;
    }

    public float[] getMassFluxForTimeIndex(int timeIndex, int materialIndex) {
        if (massflux_reference == null) {
            throw new NullPointerException("No reference mass in scenario applied to " + this.getClass());
        }
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = massflux_reference[i * getNumberOfTimes() + timeIndex][materialIndex];
        }
        return r;
    }

    public float[] getConcentrationForTimeIndex(int timeIndex, int materialIndex) {
        if (concentration_reference == null) {
            throw new NullPointerException("No reference concentration in scenario applied to " + this.getClass());
        }
        float[] r = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            r[i] = concentration_reference[i * getNumberOfTimes() + timeIndex][materialIndex];
        }
        return r;
    }

//    public double getMomentum1_xc(int timeIndex) {
//        if (massflux_reference == null) {
//            throw new NullPointerException("No reference mass in scenario applied to " + this.getClass());
//        }
//        double zaehler = 0;
//        double nenner = 0;
//        double c = 0;
//        for (int i = 0; i < distance.length; i++) {
//            c = massflux_reference[i * getNumberOfTimes() + timeIndex][0];
//            zaehler += distance[i] * c;
//            nenner += c;
//        }
//        return zaehler / nenner;
//    }

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

    public void setNumberOfMaterials(int numberOfMaterials) {
        this.numberOfMaterials = numberOfMaterials;
        initMass_Reference();
    }

    void initMass_Reference() {
        this.massflux_reference = new float[velocity.length][numberOfMaterials];
    }

    void initConcentration_Reference() {
        this.concentration_reference = new float[velocity.length][numberOfMaterials];
    }

//    @Override
//    public void setActualTime(long actualTime) {
//        System.out.println(getClass()+":: set time to "+new Date(actualTime));
//        super.setActualTime(actualTime); //To change body of generated methods, choose Tools | Templates.
//    }
}
