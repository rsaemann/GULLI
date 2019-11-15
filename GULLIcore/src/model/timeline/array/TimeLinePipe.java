/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package model.timeline.array;

/**
 *
 * @author saemann
 */
public interface TimeLinePipe {

    /**
     * Velocity at the temporal index in [m/s]
     *
     * @param temporalIndex
     * @return
     */
    public float getVelocity(int temporalIndex);

    /**
     * Waterlevel in the pipe in meter.
     *
     * @param temporalIndex
     * @return
     */
    public float getWaterlevel(int temporalIndex);

    /**
     * Volume Flow in pipe in [m³/s]
     *
     * @param temporalIndex
     * @return
     */
    public float getDischarge(int temporalIndex);

    /**
     * Reference massflux [kg/s] of solute in pipe volume if given.
     *
     * @param temporalIndex
     * @return
     */
    public float getMassflux_reference(int temporalIndex);
 /**
     * Reference Concentration [kg/m³] of pollution in pipe volume if given.
     *
     * @param temporalIndex
     * @return
     */
    public float getConcentration_reference(int temporalIndex);

    /**
     * Does timeline contain information about mass reference;
     *
     * @return
     */
    public boolean hasMassflux_reference();

    /**
     * Number of timestamps where values are known.
     *
     * @return
     */
    public int getNumberOfTimes();

    /**
     * Velocity in pipe at actual timestep.
     *
     * @return
     */
    public float getVelocity();

    /**
     * Volume Flow at actual timestep.
     *
     * @return
     */
    public double getDischarge();

    /**
     * Waterlevel in pipe at actual timestep.
     *
     * @return
     */
    public double getWaterlevel();
    
     /**
     * FluidVolume(water) in pipe at actual timestep.
     *
     * @return
     */
    public double getVolume();

    /**
     * Holds information about stored timestamps
     *
     * @return
     */
    public TimeContainer getTimeContainer();

}
