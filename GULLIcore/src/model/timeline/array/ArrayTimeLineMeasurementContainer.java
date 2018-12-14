package model.timeline.array;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineMeasurementContainer {

    public static ArrayTimeLineMeasurementContainer instance;

    private final TimeContainer times;
    public  int[] particles;
    /**
     * Mass of contaminants in total [timeindex]
     */
    public  float[] mass_total;
    /**
     * mass of different types of contaminants [timeindex][contaminantIndex]
     */
    public  float[][] mass_type;
    public  int[] particles_visited;
    public  float[] volumes;
    public  int[] counts;

    /**
     * Is it only recorded once per timeindex?
     */
    private boolean timespotmeasurement = false;

    public double messungenProZeitschritt;
    /**
     * Distance from an injection point to calculate the momentum of
     * concentration.
     */
    public static float[] distance;

    private int actualTimeIndex = 0;
    private final int numberOfCapacities;
    private int numberOfContaminants;

    public static double maxConcentration_global = 0;

    public static boolean isInitialized() {
        return instance != null;
    }

    public static ArrayTimeLineMeasurementContainer init(long[] times, int numberOfPipes, int numberOfContaminantTypes) {
        return init(new TimeContainer(times), numberOfPipes, numberOfContaminantTypes);
    }

    public static ArrayTimeLineMeasurementContainer init(TimeContainer times, int numberOfPipes, int numberOfContaminantTypes) {
        ArrayTimeLineMeasurementContainer container = new ArrayTimeLineMeasurementContainer(times, numberOfPipes, numberOfContaminantTypes);
        instance = container;
        return container;
    }

    public ArrayTimeLineMeasurementContainer(long[] times, int numberOfCapacities, int numberOfContaminantTypes) {
        this(new TimeContainer(times), numberOfCapacities, numberOfContaminantTypes);
    }

    public ArrayTimeLineMeasurementContainer(TimeContainer times, int numberOfPipes, int numberOfContaminantTypes) {
        this.times = times;
        this.numberOfCapacities = numberOfPipes;
        this.numberOfContaminants=numberOfContaminantTypes;
        this.counts = new int[numberOfPipes * times.getNumberOfTimes()];
        this.particles = new int[numberOfPipes * times.getNumberOfTimes()];
        this.particles_visited = new int[numberOfPipes * times.getNumberOfTimes()];
        this.volumes = new float[numberOfPipes * times.getNumberOfTimes()];
        this.mass_total = new float[numberOfPipes * times.getNumberOfTimes()];

        this.mass_type = new float[numberOfPipes * times.getNumberOfTimes()][numberOfContaminantTypes];
    }

    public void setActualTime(long time) {
        int neuerIndex = getIndexForTime(time);
        actualTimeIndex = neuerIndex;
    }

    public int getActualTimeIndex() {
        return actualTimeIndex;
    }

    public void OnlyRecordOncePerTimeindex() {
        this.timespotmeasurement = true;
        this.messungenProZeitschritt = 1;
    }
    
    public void setRecordsPerTimeindex(int recordsPerTimeindex){
        this.timespotmeasurement=false;
        this.messungenProZeitschritt=recordsPerTimeindex;
    }

    /**
     *
     * @return seconds
     */
    public double getDeltaTime() {
        return times.getDeltaTime();
    }

    public double getMomentum1_xc(int timeIndex) {
        double zaehler = 0;
        double nenner = 0;
        double c;
        int index = 0;
        int mittlungsradius = 5;
        for (int i = 0; i < Math.min(distance.length, numberOfCapacities); i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;
                if (particles[index] < 1) {
                    continue;
                }
                c = (((double) mass_total[index] * (double) counts[index]) / (volumes[index] * messungenProZeitschritt));

                zaehler += (distance[i]) * c;
                nenner += c;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public double getMomentum2_xc(int timeIndex, double moment1) {
        double zaehler = 0;
        double nenner = 0;
        double c;
        int index = 0;
        for (int i = 0; i < Math.min(distance.length, numberOfCapacities); i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;

                if (particles[index] < 1) {
                    continue;
                }
//                //ArrayTimeLinePipe.concentration_reference[index];//
                c = (((double) mass_total[index] * (double) counts[index]) / (volumes[index] * messungenProZeitschritt));

                zaehler += (distance[i] - moment1) * (distance[i] - moment1) * c;
                nenner += c;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public long getTimeMillisecondsAtIndex(int timeIndex) {
        return times.getTimeMilliseconds(timeIndex);
    }

    public int getIndexForTime(long time) {
        return times.getTimeIndex(time);
    }

    public int getNumberOfTimes() {
        return times.getNumberOfTimes();
    }

    public long getStartTime() {
        return times.getTimeMilliseconds(0);
    }

    public long getEndTime() {
        return times.getTimeIndex(times.getNumberOfTimes() - 1);
    }

    public void clearValues() {
//        System.out.println("Clear Measurement values for "+numberOfCapacities+" pipes.");
        long start=System.currentTimeMillis();
      
         this.counts = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.particles = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.particles_visited = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.volumes = new float[numberOfCapacities * times.getNumberOfTimes()];
        this.mass_total = new float[numberOfCapacities * times.getNumberOfTimes()];

        this.mass_type = new float[numberOfCapacities * times.getNumberOfTimes()][numberOfContaminants];
        
//        for (int i = 0; i < particles.length; i++) {
//            particles[i] = 0;
//            particles_visited[i] = 0;
//            volumes[i] = 0;
//            counts[i] = 0;
//            mass_total[i] = 0;
//            for (int j = 0; j < mass_type.length; j++) {
//                for (int k = 0; k < mass_type[j].length; k++) {
//                    mass_type[j][k]=0;                    
//                }
//            }
//        }
        maxConcentration_global = 0;
//        System.out.println(((System.currentTimeMillis()-start)/1000)+" s");
    }

    public float[] getMassForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((mass_total[i * times.getNumberOfTimes() + timeIndex]) / (messungenProZeitschritt));
//            System.out.println(getClass()+" t="+timeIndex);
        }
        return r;
    }

    public float[] getConcentrationForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((mass_total[i * times.getNumberOfTimes() + timeIndex] * counts[i * times.getNumberOfTimes() + timeIndex]) / (volumes[i * times.getNumberOfTimes() + timeIndex] * messungenProZeitschritt));
        }
        return r;
    }

    public float[] getNumberOfParticlesForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((particles[i * times.getNumberOfTimes() + timeIndex]) / (float) (messungenProZeitschritt));
        }
        return r;
    }

    public int[] getNumberOfMeasurementsPerTimestepForTimeIndex(int timeIndex) {
        int[] r = new int[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = counts[i * times.getNumberOfTimes() + timeIndex];
        }
        return r;
    }

//    public double getMaxMass() {
//        return maxMass;
//    }
//    public double getMaxConcentration() {
//        return maxConcentration;
//    }
    public static double getMaxConcentration_global() {
        return maxConcentration_global;
    }

//    /**
//     * The number of particles in this timeintervall without timecorrection
//     * factor.
//     *
//     * @param temporalIndex
//     * @return
//     */
//    public int getParticlesPur(int temporalIndex) {
//        int index = getIndex(temporalIndex);
//        return (particles[index]);
//    }
    public int getNumberOfCapacities() {
        return numberOfCapacities;
    }

    public boolean isTimespotmeasurement() {
        return timespotmeasurement;
    }
    
    

}
