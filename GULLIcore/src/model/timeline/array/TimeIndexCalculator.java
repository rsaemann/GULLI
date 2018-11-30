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
 * Calculates and provides an index in time for a given timestamp. Timeline
 * Container implement this interface the ThreadController only has to send the
 * actualt simulationtime here and timelines can easily grab their value from
 * the container.
 *
 * @author saemann
 */
public interface TimeIndexCalculator {

    /**
     * Give the actual simulation timestep. This object will generate a
     * timeindex based on this timestamp.
     *
     * @param timelong in milliseconds from 1970
     */
    public void setActualTime(long timelong);

    /**
     * Return the last set timestep.
     *
     * @return timestep in milliseconds since 1970.
     */
    public long getActualTime();

    /**
     * Gives the integer index calculated from the last set actual timestep.
     *
     * @return index of the timestep in a TimeContainer's array.
     */
    public int getActualTimeIndex();

    /**
     * The decimal gives the floor index of a timearray. The fraction gives the
     * linear interpolation (0..1) till next index.
     *
     * @return
     */
    public double getActualTimeIndex_double();

    /**
     * Returns but does NOT set the timeindex of a given timestamp
     *
     * @param timemilliseconds sinze 1970
     * @return
     */
    public int getTimeIndex(long timemilliseconds);

    /**
     * Returns the time index as doublevalue regarding the fractional ratio
     * between integer index values. Gibt den Index als Double Zahl zurück, die
     * auch den Anteil zwischen den Ganzzahligen Indize berücksichtigt.
     *
     * @param time
     * @return
     */
    public double getTimeIndexDouble(long time);

    /**
     * First timestamp.
     *
     * @return in milliseconds sinze 1970
     */
    public long getStartTime();

    /**
     * Last timestamp.
     *
     * @return in milliseconds sinze 1970
     */
    public long getEndTime();
    
    /**
     * Number of timestamps.
     * @return 
     */
    public int getNumberOfTimes();
}
