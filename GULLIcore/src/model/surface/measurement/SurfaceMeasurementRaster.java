/*
 * The MIT License
 *
 * Copyright 2018 saemann.
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
package model.surface.measurement;

import model.particle.Particle;
import model.timeline.array.TimeIndexContainer;

/**
 * Measures the contamination of surface
 *
 * @author saemann
 */
public abstract class SurfaceMeasurementRaster {

    /**
     * if particles have lower travel length than this, they are not measured.
     */
    public static double minTravelLengthToMeasure = 0;

    public static boolean countStayingParticle = true;

    public static boolean synchronizeMeasures = true;

    protected TimeIndexContainer times;

    public abstract void measureParticle(long time, Particle particle, int threadIndex);

    public abstract void setNumberOfMaterials(int numberOfMaterials);

    public abstract void setTimeContainer(TimeIndexContainer times);

    public TimeIndexContainer getIndexContainer() {
        return times;
    }

    public int status = -1;

    public int[] statuse = new int[8];
    volatile public TriangleMeasurement[] monitor = new TriangleMeasurement[8];

    public abstract void reset();

    /**
     * This is called, when a Thread blocks the simulation.
     */
    public abstract void breakAllLocks();

    /**
     * CAlls the raster to count and store its measurements at the end of each
     * loop. Some Rasters might need a synchronization step at the end of each
     * simulation loop.
     */
    public abstract void synchronizeMeasurements();
}
