/*
 * The MIT License
 *
 * Copyright 2019 saemann.
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
package com.saemann.gulli.core.control.scenario.injection;

import com.saemann.gulli.core.model.particle.Material;
import com.saemann.gulli.core.model.timeline.TimedValue;

/**
 *
 * @author saemann
 */
public class HE_MessdatenInjection extends HEInjectionInformation {

    public TimedValue[] timedValues;
    /**
     * Start seconds of each interval after event start.
     */
    private double[] timesteps;
    /**
     * Spilled mass of each interval [kg]
     */
    private double[] spillMass;

    /**
     * m³
     */
    private double totalmass;
    /**
     * kg
     */
    private double totalVolume;

    private double concentration;

    /**
     *
     * @param capacityName
     * @param mat
     * @param eventStart
     * @param timedValues
     * @param concentration kg/m³
     */
    public HE_MessdatenInjection(String capacityName, Material mat, long eventStart, TimedValue[] timedValues, double concentration) {
        super(capacityName, mat, eventStart, timedValues[timedValues.length - 1].time, 0);
        this.timedValues = timedValues;
        this.concentration = concentration;
        calculateMass(timedValues, eventStart, concentration);
//        calculateNumberOfIntervalParticles(numberOfParticles);
    }

    private void calculateMass(TimedValue[] timedValues, long eventStart, double concentration) {
        this.timesteps = new double[timedValues.length + 1];
        this.spillMass = new double[timesteps.length];
//        this.number_particles = new int[timesteps.length];

        double volume = 0;
        double lastInterval = 0;
        for (int i = 1; i < timedValues.length; i++) {
            TimedValue start = timedValues[i - 1];
            TimedValue end = timedValues[i];
            timesteps[i - 1] = (start.time - eventStart) / 1000.;

            double seconds = (end.time - start.time) / 1000.;
            if (start.value < 0) {
                continue;
            }
            double dV = (start.value + end.value) * 0.5 * seconds;
            spillMass[i - 1] = dV * concentration;
            lastInterval = seconds;
            volume += dV;
        }
        //last calue for the duration of the last interval
        double d = timedValues[timedValues.length - 1].value;
        if (d > 0) {
            volume += d * lastInterval;
        }
        this.totalVolume = volume;
        this.totalmass = totalVolume * concentration;
    }

    public double getStarttimeSimulationsAfterSimulationStart() {
        return timesteps[0];
    }

    public double getDurationSeconds() {
        return (timesteps[timesteps.length - 1] - timesteps[0]);
    }

    public double getMass() {
        return totalmass;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public double getConcentration() {
        return concentration;
    }

    public int getNumberOfIntervals() {
        return timesteps.length - 1;
    }

    public double getIntervalStart(int interval) {
        return timesteps[interval];
    }

    public double getIntervalEnd(int interval) {
        return timesteps[interval + 1];
    }

    public double massInInterval(int interval) {
        return spillMass[interval];
    }

}
