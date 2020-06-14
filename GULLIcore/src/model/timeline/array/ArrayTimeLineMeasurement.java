package model.timeline.array;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import model.particle.Particle;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineMeasurement {

    protected ArrayTimeLineMeasurementContainer container;

    /**
     * If set to true, HashSets for PArticleIDs will be created to store the
     * passed particles by their ID. This is slowing the Multithread performance
     * since the HashSet has to be synchronised.
     */
    public static boolean useIDsharpParticleCounting = false;

    /**
     * Using synchronized measurements is slower because Particle threas writing
     * on this list have to wait on each other. Enabling this results in a more
     * accurate counting because adding a value is not threadsave.
     */
    public static boolean synchronizeMeasures = true;
    private Lock lock = new ReentrantLock();

    private double maxMass = 0;
    private double maxConcentration = 0;
    private final HashSet<Particle> particles;//new HashSet<>(0);

    /**
     * Number of particles in this pipe at the actual timestep.
     */
    private final int startIndex;
    /**
     * Number of particles which were affecting the gathering in this timestep.
     * Reset after every writing of measurements
     */
    private int numberOfParticlesInTimestep;
    /**
     * Total mass of particles which passed in the current timestep. Reset after
     * every writing of measurements
     */
    private double particleMassInTimestep = 0;

    private double[] particleMassPerTypeinTimestep;

//    /**
//     * if not active, then no information is collected and stored.
//     */
//    public boolean active = true;
    
    private int spatialIndex;

    public ArrayTimeLineMeasurement(ArrayTimeLineMeasurementContainer container, int spatialIndex) {
        this.container = container;
        this.spatialIndex=spatialIndex;
        this.startIndex = container.getNumberOfTimes() * spatialIndex;
        if (useIDsharpParticleCounting) {
            particles = new HashSet<>(0);
        } else {
            particles = null;
        }
    }

    private int getIndex(int temporalIndex) {
        int i = container.getNumberOfTimes() * spatialIndex+temporalIndex;//startIndex + temporalIndex;
//        if (i >= container.getn) {
//            System.err.println(this.getClass() + ":Index out of Bounds: temporalIndex:" + temporalIndex + " + startindex: " + startIndex + " = " + i + ">= " + ArrayTimeLineMeasurement.counts.length);
//        }
        return i;
    }

    public float getConcentration(int temporalIndex) {
        int index = getIndex(temporalIndex);

        return (float) (container.mass_total[index] / (container.volumes[index] * container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/));
        /*container.particles[index] * Particle.massPerParticle*/
        /**
         * container.counts[index]
         */
    }

    public float getConcentrationOfType(int temporalIndex, int materialIndex) {
        int index = getIndex(temporalIndex);

        return (float) (container.mass_type[index][materialIndex] / (container.volumes[index] * container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/));
        /*container.particles[index] * Particle.massPerParticle*/
        /**
         * container.counts[index]
         */
    }

    /**
     * get total mass of all materials
     *
     * @param temporalIndex
     * @return
     */
    public float getMass(int temporalIndex) {
        int index = getIndex(temporalIndex);
        return (float) (/*container.particles[index] * Particle.massPerParticle*/container.mass_total[index] / (container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/));

    }

    public float getMass(int temporalIndex, int materialIndex) {
        int index = getIndex(temporalIndex);
        float mass = (float) (container.mass_type[index][materialIndex] / (container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/));
//        System.out.println("mass at ti="+temporalIndex+": mass="+container.mass_type[index][materialIndex]+", smples: "+container.samplesInTimeInterval[temporalIndex]);
        return mass;

    }

    public float getParticles(int temporalIndex) {
        int index = getIndex(temporalIndex);
        return (float) (container.particles[index] / (float) container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/);
    }

    public float getVolume(int temporalIndex) {
        int index = getIndex(temporalIndex);
        try {
            return container.volumes[index] / container.counts[index];
        } catch (Exception e) {
            System.err.println("index= " + index + "   volumes.length=" + container.volumes.length + "\tcounts.length=" + container.counts.length);
        }
        return 0;
    }

    public boolean hasValues(int timeIndex) {
        return container.counts[getIndex(timeIndex)] > 0;
    }

    public void addMeasurement(int timeindex,/* int particleCount, double mass,*/ double volumeValue) {
        int index = getIndex(timeindex);

//        if (timeindex < 5) {
//            if (particleMassInTimestep > 0) {
//                System.out.println("Add measurement "+timeindex+":  " + getContainer().getTimeMillisecondsAtIndex(timeindex));
//            }
//        }
        try {
            container.particles[index] += numberOfParticlesInTimestep;

            if (useIDsharpParticleCounting) {
                container.particles_visited[index] = particles.size();
            }
            container.volumes[index] += volumeValue;
            container.mass_total[index] += particleMassInTimestep;

            if (particleMassPerTypeinTimestep != null) {
                try {
                    for (int i = 0; i < particleMassPerTypeinTimestep.length; i++) {
                        container.mass_type[index][i] += (float) (particleMassPerTypeinTimestep[i]);
                    }
                } catch (Exception e) {
                    System.err.println("Index problem with material " + (particleMassPerTypeinTimestep.length - 1) + ", length of array: " + container.mass_type[index].length);
                }
            }

            container.counts[index]++;
            double tempmass = (particleMassInTimestep / (container.samplesInTimeInterval[timeindex]/*samplesPerTimeinterval*/));
            if (tempmass > maxMass) {
                maxMass = tempmass;
            }
//            System.out.println("store mass at time "+timeindex +" counts: "+container.counts[index]);
            double temp_c = tempmass / volumeValue;//container.volumes[index];// (tempmass * container.counts[index] / (container.volumes[index]));
            if (!Double.isInfinite(temp_c) && !Double.isNaN(temp_c)) {
                if (temp_c > maxConcentration) {
                    maxConcentration = temp_c;
                }
                if (temp_c > container.maxConcentration_global) {
                    container.maxConcentration_global = temp_c;
                }
            }
        } catch (Exception e) {
            System.out.println(this.getClass() + "::addMeasurements(timindex=" + timeindex + " (/" + container.getNumberOfTimes() + ")=>" + index + ", particles=" + numberOfParticlesInTimestep + ", volume=" + volumeValue + ")");
            e.printStackTrace();
        }
    }

    public int getNumberOfParticlesUntil(int timeindex) {
        int ptcount = 0;
        for (int i = 0; i < timeindex; i++) {
            int id = getIndex(i);
            ptcount += container.particles[id];
        }
        return ptcount;
    }

    public int getNumberOfParticles() {
        return numberOfParticlesInTimestep;
    }

    public double getParticleMassInTimestep() {
        return particleMassInTimestep;
    }

    public int getNumberOfVisitedParticles() {
        if (particles == null) {
            return 0;
        }
        return particles.size();
    }

    private void addParticleMassperMaterial(int materialID, double mass) {
        if (particleMassPerTypeinTimestep == null || particleMassPerTypeinTimestep.length != container.getNumberOfContaminants()) {
            particleMassPerTypeinTimestep = new double[container.getNumberOfContaminants()];
        }
        particleMassPerTypeinTimestep[materialID] += mass;
    }

    public void addParticle(Particle particleToCount) {
        if (!container.measurementsActive) {
            return;
        }
        addParticle(particleToCount, 1f);
    }

    /**
     *
     * @param particleToCount
     * @param dtfactor fraction of time spend on this capacity in relation to
     * the whole timestep.
     */
    public void addParticle(Particle particleToCount, float dtfactor) {
        if (!container.measurementsActive && container.isTimespotmeasurement()) {
            //Skip if the paticles should only be sampled at the end of an interval and the sampling is not enabled for that last step.
            return;
        }
        if (synchronizeMeasures) {
            if (useIDsharpParticleCounting) {
                synchronized (particles) {
                    if (!particles.contains(particleToCount)) {
                        particles.add(particleToCount);
                        synchronized (this) {
                            this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
                            this.numberOfParticlesInTimestep++;
                            addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
                        }
                    }
                }
            } else {
//                synchronized (this) {
                lock.lock();
                try {
                    this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
                    this.numberOfParticlesInTimestep++;
                    addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
                } finally {
                    lock.unlock();
                }
//                }
            }
        } else {
            if (useIDsharpParticleCounting) {
                synchronized (particles) {
                    if (!particles.contains(particleToCount)) {
                        particles.add(particleToCount);
                    }
                }
            }
            this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
            this.numberOfParticlesInTimestep++;
            addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
        }
    }

    /**
     * Clears all counters to start a new sampling action.
     */
    public void resetNumberOfParticles() {
        this.numberOfParticlesInTimestep = 0;
        this.particleMassInTimestep = 0.;
        if (particleMassPerTypeinTimestep != null) {
            for (int i = 0; i < particleMassPerTypeinTimestep.length; i++) {
                particleMassPerTypeinTimestep[i] = 0;
            }
        }
    }

    public void resetVisitedParticlesStorage() {
        if (particles != null) {
            this.particles.clear();
        }
    }

    /**
     * True if samples have been taken sinze the last reset of the counter. This
     * is indicated by a number of counted particles >0.
     *
     * @param timeIndex
     * @return
     */
    public boolean hasMeasurements(int timeIndex) {
        return container.counts[startIndex + timeIndex] > 0;
    }

    public double getMaxMass() {
        return maxMass;
    }

    public double getMaxConcentration() {
        return maxConcentration;
    }

    public double getMaxConcentration_global() {
        return container.maxConcentration_global;
    }

    /**
     * The number of particles in this timeintervall without timecorrection
     * factor.
     *
     * @param temporalIndex
     * @return
     */
    public int getParticles_Visited(int temporalIndex) {
        int index = getIndex(temporalIndex);
        return (container.particles_visited[index]);
    }

    public ArrayTimeLineMeasurementContainer getContainer() {
        return container;
    }

}
