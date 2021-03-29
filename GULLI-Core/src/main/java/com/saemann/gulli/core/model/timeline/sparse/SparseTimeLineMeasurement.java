/*
 * The MIT License
 *
 * Copyright 2021 B1.
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
package com.saemann.gulli.core.model.timeline.sparse;

import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.MeasurementTimeline;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A timeline to store measurements for a pipe object.
 *
 * @author Robert Sämann
 */
public class SparseTimeLineMeasurement implements MeasurementTimeline {

    private boolean initialized = false;

    public float[] particles;
    /**
     * Mass of contaminants in total [timeindex]
     */
    public float[] mass_total;
    /**
     * mass of different types of contaminants [timeindex][contaminantIndex] raw
     * value. must be divided by the number of samples to get the value for the
     * interval
     */
    public float[][] mass_type;
    public float[] volumes;

    protected SparseMeasurementContainer container;

//    /**
//     * Samples are only taken if this is true. Can be switched of, to save
//     * computation cost. SYnchronisation Thread switches this flag on and off.
//     */
//    public boolean measurementsActive = true;
    /**
     * Number of samples in this timeinterval. Important if the global number
     * does not fit the interval number. E.g in the very first and very last
     * interval;
     */
    public int[] samplesInTimeIntervalPipe;

//    /**
//     * Indicates how many samples are taken during one sampling interval. This
//     * variable is only for debugging and is not used for the calculation.
//     */
//    public float samplesPerTimeinterval = 1;
    public static double maxConcentration_global = 0;

    /**
     * Number of particles which were affecting the gathering in this timestep.
     * Reset after every writing of measurements
     */
    private float numberOfParticlesInTimestep;
    /**
     * Total mass of particles which passed in the current timestep. Reset after
     * every writing of measurements
     */
    private float particleMassInTimestep = 0;

    private float[] particleMassPerTypeinTimestep;

    private float[] particleMassPerTypeLastTimestep;

    private final Lock lock = new ReentrantLock();

    public SparseTimeLineMeasurement(SparseMeasurementContainer container) {
        this.container = container;
    }

    private void prepareWritable() {
        if (!initialized) {
            initArrays(container.getTimes().getNumberOfTimes(), container.getNumberOfMaterials());
        }
    }

    private void initArrays(int numberOfTimes, int numberOfMaterials) {
        particles = new float[numberOfTimes];
        samplesInTimeIntervalPipe = new int[numberOfTimes];
        mass_total = new float[numberOfTimes];
        mass_type = new float[numberOfTimes][numberOfMaterials];
        volumes = new float[numberOfTimes];
        particleMassPerTypeLastTimestep = new float[numberOfMaterials];
        particleMassPerTypeinTimestep = new float[numberOfMaterials];
        initialized = true;
    }

    /**
     * accumulated concentration of all pollutions together [kg/m^3]
     *
     * @param temporalIndex of the measurement intervals
     * @return [kg/m^3]
     */
    @Override
    public float getConcentration(int temporalIndex) {
        if (mass_total == null) {
            return 0;
        }
        //mass and volume are both sums of the interval samples and therefore do not have to be divided by the number of samples
        float c = (float) (mass_total[temporalIndex] / (volumes[temporalIndex]));
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
        if (mass_type == null) {
            return 0;
        }

        return (float) (mass_type[temporalIndex][materialIndex] / (volumes[temporalIndex]));
    }

    /**
     * get accumulated mass (physical, not raw) of all materials in kg
     *
     * @param temporalIndex
     * @return [kg]
     */
    @Override
    public float getMass(int temporalIndex) {
        if (mass_total == null) {
            return 0;
        }
        return (float) (mass_total[temporalIndex] / (container.samplesInTimeInterval[temporalIndex]));

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
        if (mass_total == null) {
            return 0;
        }

        float mass = (float) (mass_type[temporalIndex][materialIndex] / (container.samplesInTimeInterval[temporalIndex]));
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
        for (int i = 1; i < container.getTimes().getNumberOfTimes(); i++) {
            if (samplesInTimeIntervalPipe[i] < 1) {
                continue;
            }
            float temp_mass = (float) (mass_total[i] / (container.samplesInTimeInterval[i]));
            float discharge = tl.getVelocity(tl.getTimeContainer().getTimeIndex(container.getTimes().getTimeMilliseconds(i))) / pipeLength;
            float dt = (container.getTimes().getTimeMilliseconds(i) - container.getTimes().getTimeMilliseconds(i - 1)) / 1000.f;
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
        if (particles == null) {
            return 0;
        }
        return (float) (particles[temporalIndex] / (float) container.samplesInTimeInterval[temporalIndex]/*samplesPerTimeinterval*/);
    }

    /**
     * Mean volume sampled during the ith time interval.
     *
     * @param temporalIndex
     * @return m^3
     */
    @Override
    public float getVolume(int temporalIndex) {
        if (volumes == null) {
            return 0;
        }
        try {
            return volumes[temporalIndex] / samplesInTimeIntervalPipe[temporalIndex];
        } catch (Exception e) {
            System.err.println("index= " + temporalIndex + "   volumes.length=" + volumes.length + "\t samples.length=" + samplesInTimeIntervalPipe.length);
        }
        return 0;
    }

    @Override
    public boolean hasValues(int timeIndex) {
        if (samplesInTimeIntervalPipe == null) {
            return false;
        }
        return samplesInTimeIntervalPipe[timeIndex] > 0;
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
        if (numberOfParticlesInTimestep == 0) {
            //Nothing to do here. No information from trespassing particles gathered during the last simulation loop.
            return;
        }

        try {
            //Prepare and initialize the Arrays for storing, if they do not exist.
            prepareWritable();

            samplesInTimeIntervalPipe[timeindex]++;
            particles[timeindex] += numberOfParticlesInTimestep;
            volumes[timeindex] += volume;
            mass_total[timeindex] += particleMassInTimestep;

            if (particleMassPerTypeinTimestep != null) {
                try {
                    for (int i = 0; i < particleMassPerTypeinTimestep.length; i++) {
                        mass_type[timeindex][i] += (float) (particleMassPerTypeinTimestep[i]);
                    }
                } catch (Exception e) {
                    System.err.println("Index problem with material " + (particleMassPerTypeinTimestep.length - 1) + ", length of array: " + mass_type[timeindex].length);
                }
            }

            

        } catch (Exception e) {
            System.out.println(this.getClass() + "::addMeasurements(timindex=" + timeindex + " (/" + container.getTimes().getNumberOfTimes() + ")=>" + timeindex + ", particles=" + numberOfParticlesInTimestep + ", volume=" + volume + ")");
            e.printStackTrace();
        }
    }

    @Override
    public int getNumberOfParticlesUntil(int timeindex) {
        if (particles == null) {
            return 0;
        }
        int ptcount = 0;
        for (int i = 0; i < timeindex; i++) {

            ptcount += particles[i];
        }
        return ptcount;
    }

    public double getNumberOfParticles() {
        return numberOfParticlesInTimestep;
    }

    @Override
    public double getParticleMassInTimestep() {
        return particleMassInTimestep;
    }

    public double getNumberOfVisitedParticles() {
        if (particles == null) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < particles.length; i++) {
            sum += particles[i];

        }
        return sum;
    }

    private void addParticleMassperMaterial(int materialID, double mass) {
        if (particleMassPerTypeinTimestep == null) {
            particleMassPerTypeinTimestep = new float[container.getNumberOfMaterials()];
            particleMassPerTypeLastTimestep = new float[particleMassPerTypeinTimestep.length];
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
        if (!container.measurementsActive/* && !container.timecontinuousMeasures*/) {
            //Skip if the paticles should only be sampled at the end of an interval and the sampling is not enabled for that last step.
            return;
        }
        if (MeasurementContainer.synchronizeMeasures) {
            lock.lock();
            try {

                this.particleMassInTimestep += particleToCount.particleMass * dtfactor;
                this.numberOfParticlesInTimestep += dtfactor;
                addParticleMassperMaterial(particleToCount.getMaterial().materialIndex, particleToCount.getParticleMass() * dtfactor);
            } finally {
                lock.unlock();
            }
        } else {
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
        this.particleMassInTimestep = 0.f;
        if (particleMassPerTypeinTimestep != null) {
            for (int i = 0; i < particleMassPerTypeinTimestep.length; i++) {
                particleMassPerTypeLastTimestep[i] = (float) particleMassPerTypeinTimestep[i];
                particleMassPerTypeinTimestep[i] = 0;
            }
        }
    }

    @Override
    public void resetVisitedParticlesStorage() {
//        if (particles != null) {
//            this.particles.clear();
//        }
    }

    /**
     * True if samples have been taken sinze the last reset of the counter. This
     * is indicated by a number of counted particles >0.
     *
     * @param timeIndex
     * @return
     */
    public boolean hasMeasurements(int timeIndex) {
        if (samplesInTimeIntervalPipe == null) {
            return false;
        }
        return samplesInTimeIntervalPipe[timeIndex] > 0;
    }

    public double getMaxMass() {
        return -1;
    }

    public double getMaxConcentration() {
        return -1;
    }

    public double getMaxConcentration_global() {
        return -1;
    }

    /**
     * The number of particles in this timeintervall without timecorrection
     * factor.
     *
     * @param temporalIndex
     * @return
     */
    @Override
    public int getParticles_Visited(int temporalIndex) {

        if (particles == null) {
            return 0;
        }
        return (int) (particles[temporalIndex]);
    }

    @Override
    public MeasurementContainer getContainer() {
        return container;
    }

    public void clearValue() {
        initialized = false;
        mass_total = null;
        mass_type = null;
//        measurementTimes=null;
        particleMassPerTypeinTimestep = null;
        particleMassPerTypeLastTimestep = null;
//        particles_visited=null;
        particles = null;
        samplesInTimeIntervalPipe = null;
        volumes = null;
        maxConcentration_global = 0;
        numberOfParticlesInTimestep = 0;
        particleMassInTimestep = 0;
//        samplesPerTimeinterval=0;
    }

    @Override
    public TimeContainer getTimes() {
        return container.getTimes();
    }

    @Override
    public double getNumberOfParticlesInTimestep() {
        return numberOfParticlesInTimestep;
    }

    public double getMassLastTimestep(int materialIndex) {
        return particleMassPerTypeLastTimestep[materialIndex];
    }
}
