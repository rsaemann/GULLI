/*
 * The MIT License
 *
 * Copyright 2021 Robert Sämann.
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
package com.saemann.gulli.core.model.timeline;

import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;

/**
 * An interface to hold common methods for Array and Sparse Measurement Timeline
 *
 * @author Robert Sämann
 */
public interface MeasurementTimeline {

    /**
     * Add a particle to the measurement storage for the current time. The
     * container must be measurementActive=true to add this particle.
     *
     * @param particleToCount
     * @param dtfactor fraction of time spend on this capacity in relation to
     * the whole timestep.
     */
    public void addParticle(Particle particleToCount, float dtfactor);

    /**
     * Sumup and write the collected data so far into the storage array for the
     * given timeindex.
     *
     * @param timeindex
     * @param volume [m^3] in the Pipe at current
     * @param velocity [m/s] mean velocity in pipe
     */
    public void addMeasurement(int timeindex, double volume, double velocity);

    /**
     * Clears all counters to start a new sampling action.
     */
    public void resetNumberOfParticles();

    public boolean hasValues(int timeIndex);

    public float getConcentration(int temporalIndex);

    public float getConcentrationOfType(int temporalIndex, int materialIndex);

    public float getMass(int temporalIndex);

    /**
     * The mass in kg in the whole pipe at this timestamp. If continuous
     * sampling is enabled, this calculates the mean mass of the sampling
     * interval.
     *
     * @param temporalIndex
     * @param materialIndex
     * @return mass [kg] of this material
     */
    public float getMass(int temporalIndex, int materialIndex);

    /**
     * Massflux in [kg/s]
     *
     * @param temporalIndex
     * @param materialIndex
     * @return massflux
     */
    public float getMassFlux(int temporalIndex, int materialIndex);

    /**
     * Massflux of all materials summarized in [kg/s]
     *
     * @param temporalIndex
     * @return massflux
     */
    public float getMassFlux(int temporalIndex);

    /**
     * Mean volume sampled during the ith time interval.
     *
     * @param temporalIndex
     * @return m^3
     */
    public float getVolume(int temporalIndex);

    /**
     * Total passed mass. Can be negative, if mass flux is directed reverse to
     * pipe orientation.
     *
     * @param tl Timeline containing the velocity information
     * @param pipeLength in meter
     * @return kg of passed mass during the whole simulation.
     */
    public float getTotalMass(TimeLinePipe tl, float pipeLength);

    /**
     * Get mean number of particles in the element during the ith time period.
     *
     * @param temporalIndex
     * @return
     */
    public float getParticles(int temporalIndex);

    public int getNumberOfParticlesUntil(int timeindex);

    public double getNumberOfParticlesInTimestep();

    public double getParticleMassInTimestep();

    /**
     * Accumulated number of particles sampled during the interval;
     *
     * @param temporalIndex
     * @return
     */
    public int getParticles_Visited(int temporalIndex);

    public void resetVisitedParticlesStorage();

    public TimeContainer getTimes();

    public MeasurementContainer getContainer();
    
    public float getReferenceLength();
}
