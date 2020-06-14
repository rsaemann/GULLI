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

import com.vividsolutions.jts.geom.Coordinate;
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
    
    public boolean measurementsActive=true;
    
    public boolean continousMeasurements=true;
    
    public int[] measurementsInTimeinterval;

    protected TimeIndexContainer times;

    public abstract void measureParticle(long time, Particle particle, int threadIndex);

    public abstract void setNumberOfMaterials(int numberOfMaterials);
    
    public abstract int getNumberOfCells();
    
    public abstract boolean isCellContaminated(int cellIndex);
    
    public abstract Coordinate getCenterOfCell(int cellindex);
    
    public abstract double getMassInCell(int cellIndex,int timeindex,int materialIndex);

    public abstract void setTimeContainer(TimeIndexContainer times);

    public TimeIndexContainer getIndexContainer() {
        return times;
    }

    public int status = -1;

    public int[] statuse;// = new int[8];
   
    public abstract void reset();
    
    /**
     * Initialize the threadsafe component.
     * @param threadCount 
     */
    public abstract void setNumberOfThreads(int threadCount);

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
