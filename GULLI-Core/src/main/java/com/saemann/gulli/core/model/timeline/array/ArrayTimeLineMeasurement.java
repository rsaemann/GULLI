package com.saemann.gulli.core.model.timeline.array;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.MeasurementTimeline;
import com.saemann.gulli.core.model.timeline.MeasurementContainer;

/**
 *
 * @author saemann
 */
public class ArrayTimeLineMeasurement implements MeasurementTimeline{

    protected ArrayTimeLineMeasurementContainer container;

    /**
     * If set to true, HashSets for PArticleIDs will be created to store the
     * passed particles by their ID. This is slowing the Multithread performance
     * since the HashSet has to be synchronised.
     */
    public static boolean useIDsharpParticleCounting = false;

//    /**
//     * Using synchronized measurements is slower because Particle threas writing
//     * on this list have to wait on each other. Enabling this results in a more
//     * accurate counting because adding a value is not threadsave.
//     */
//    public static boolean synchronizeMeasures = true;
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
    private double numberOfParticlesInTimestep;
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
        this.spatialIndex = spatialIndex;
        this.startIndex = container.getNumberOfTimes() * spatialIndex;
        if (useIDsharpParticleCounting) {
            particles = new HashSet<>(0);
        } else {
            particles = null;
        }
    }

    private int getIndex(int temporalIndex) {
        int i = container.getNumberOfTimes() * spatialIndex + temporalIndex;//startIndex + temporalIndex;
//        if (i >= container.getn) {
//            System.err.println(this.getClass() + ":Index out of Bounds: temporalIndex:" + temporalIndex + " + startindex: " + startIndex + " = " + i + ">= " + ArrayTimeLineMeasurement.counts.length);
//        }
        return i;
    }

    /**
     * accumulated concentration of all pollutions together [kg/m^3]
     *
     * @param temporalIndex
     * @return [kg/m^3]
     */
    @Override
    public float getConcentration(int temporalIndex) {
        int index = getIndex(temporalIndex);
        //mass and volume are both sums of the interval samples and therefore do not have to be divided by the number of samples
        float c = (float) (container.mass_total[index] / (container.volumes[index]));
        return c;
    }

    /**
     * The physical (not raw) concentration of the material at the given
     * timeindex [kg/m^3]
     *
     * @param temporalIndex
     * @param materialIndex
     * @return [kg/m^3]
     */
    @Override
    public float getConcentrationOfType(int temporalIndex, int materialIndex) {
        int index = getIndex(temporalIndex);

        return (float) (container.mass_type[index][materialIndex] * container.counts[index] / (container.volumes[index] * container.getSamplesInTimeInterval(temporalIndex)));
    }

    /**
     * get accumulated mass (physical, not raw) of all materials in kg
     *
     * @param temporalIndex
     * @return [kg]
     */
    @Override
    public float getMass(int temporalIndex) {
        int index = getIndex(temporalIndex);
        return (float) (container.mass_total[index] / (container.samplesInTimeInterval[temporalIndex]));

    }

    /**
     * The mass in kg in the whole pipe at this timestamp. If continuous
     * sampling is enabled, this calculates the mean mass of the sampling
     * interval.
     *
     * @param temporalIndex
     * @param materialIndex
     * @return mass [kg] of this material
     */
    @Override
    public float getMass(int temporalIndex, int materialIndex) {
        int index = getIndex(temporalIndex);
        float mass = (float) (container.mass_type[index][materialIndex] / (container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/));
//        System.out.println("mass at ti="+temporalIndex+": mass="+container.mass_type[index][materialIndex]+", smples: "+container.samplesInTimeInterval[temporalIndex]);
        return mass;

    }

    /**
     * Total passed mass. Can be negative, if mass flux is directed reverse to
     * pipe orientation.
     *
     * @param tl Timeline containing the velocity information
     * @param pipeLength in meter
     * @return kg of passed mass during the whole simulation.
     */
    @Override
    public float getTotalMass(TimeLinePipe tl, float pipeLength) {
        float massSum = 0;
        for (int i = 1; i < container.getNumberOfTimes(); i++) {
            if (container.samplesInTimeInterval[i] < 1) {
                continue;
            }
            int index = getIndex(i);
            float temp_mass = (float) (container.mass_total[index] / (container.samplesInTimeInterval[i]));
            float discharge = tl.getVelocity(tl.getTimeContainer().getTimeIndex(container.getTimeMillisecondsAtIndex(i))) / pipeLength;
            float dt = (container.getMeasurementTimestampAtTimeIndex(i) - container.getMeasurementTimestampAtTimeIndex(i - 1)) / 1000.f;
            massSum += temp_mass * discharge * dt;

        }
        return massSum;
    }

    /**
     * Get mean number of particles in the element during the ith time period.
     *
     * @param temporalIndex
     * @return
     */
    @Override
    public float getParticles(int temporalIndex) {
        int index = getIndex(temporalIndex);
        return (float) (container.particles[index] / (float) container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/);
    }

    /**
     * Mean volume sampled during the ith time interval.
     *
     * @param temporalIndex
     * @return m^3
     */
    @Override
    public float getVolume(int temporalIndex) {
        int index = getIndex(temporalIndex);
        try {
            return container.volumes[index] / container.counts[index];
        } catch (Exception e) {
            System.err.println("index= " + index + "   volumes.length=" + container.volumes.length + "\tcounts.length=" + container.counts.length);
        }
        return 0;
    }

    @Override
    public boolean hasValues(int timeIndex) {
        return container.counts[getIndex(timeIndex)] > 0;
    }

    /**
     * Sumup and write the collected data so far into the storage array for the
     * given timeindex.
     *
     * @param timeindex
     * @param volume in the Pipe at current
     */
    @Override
    public void addMeasurement(int timeindex, double volume) {
        int index = getIndex(timeindex);

        try {
            container.particles[index] += numberOfParticlesInTimestep;

            if (useIDsharpParticleCounting) {
                container.particles_visited[index] = particles.size();
            }
            container.volumes[index] += volume;
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
            double temp_c = tempmass / volume;//container.volumes[index];// (tempmass * container.counts[index] / (container.volumes[index]));
            if (!Double.isInfinite(temp_c) && !Double.isNaN(temp_c)) {
                if (temp_c > maxConcentration) {
                    maxConcentration = temp_c;
                }
                if (temp_c > container.maxConcentration_global) {
                    container.maxConcentration_global = temp_c;
                }
            }
        } catch (Exception e) {
            System.out.println(this.getClass() + "::addMeasurements(timindex=" + timeindex + " (/" + container.getNumberOfTimes() + ")=>" + index + ", particles=" + numberOfParticlesInTimestep + ", volume=" + volume + ")");
            e.printStackTrace();
        }
    }

    @Override
    public int getNumberOfParticlesUntil(int timeindex) {
        int ptcount = 0;
        for (int i = 0; i < timeindex; i++) {
            int id = getIndex(i);
            ptcount += container.particles[id];
        }
        return ptcount;
    }

    public double getNumberOfParticlesInTimestep() {
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
     * Add a particle to the measurement storage for the current time. The
     * container must be measurementActive=true to add this particle.
     *
     * @param particleToCount
     * @param dtfactor fraction of time spend on this capacity in relation to
     * the whole timestep.
     */
    @Override
    public void addParticle(Particle particleToCount, float dtfactor) {
        if (!container.measurementsActive && container.isTimespotmeasurement()) {
            //Skip if the paticles should only be sampled at the end of an interval and the sampling is not enabled for that last step.
            return;
        }
        if (MeasurementContainer.synchronizeMeasures) {
            if (useIDsharpParticleCounting) {
                synchronized (particles) {
                    if (!particles.contains(particleToCount)) {
                        particles.add(particleToCount);
                        synchronized (this) {
                            this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
                            this.numberOfParticlesInTimestep += dtfactor;
                            addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
                        }
                    }
                }
            } else {
//                synchronized (this) {
                lock.lock();
                try {
                    this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
                    this.numberOfParticlesInTimestep += dtfactor;
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
            this.numberOfParticlesInTimestep += dtfactor;
            addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
        }
    }

    /**
     * Clears all counters to start a new sampling action.
     */
    @Override
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

    @Override
    public TimeContainer getTimes() {
        return container.getTimes();
    }

}
