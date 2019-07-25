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
package model.timeline.sparse;

import model.timeline.array.TimeContainer;
import model.timeline.array.TimeLinePipe;
import model.topology.Pipe;

/**
 * If timeline values are needed, they are loaded on time from a dataprovider
 * (e.g. database). safes lots of space.
 *
 * @author saemann
 */
public class SparseTimelinePipe implements TimeLinePipe {

    private long pipeManualID;

    private String pipeName;

    private SparseTimeLinePipeContainer container;

    /**
     * Velocity for pipes in [m/s].
     */
    private float[] velocity;
    /**
     * Waterlevel above pipe sole [m]
     */
    private float[] waterlevel;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    private float[] flux;
    /**
     * Reference mass if given in scenario. May be null
     */
    private float[] mass_reference;
    
     /**
     * Reference concentration if given in scenario. May be null
     */
    private float[] concentration_reference;

    private float actualVelocity, actualWaterlevel, actualFlux;
    private long actualTimestamp;

    public SparseTimelinePipe(SparseTimeLinePipeContainer container, Pipe pipe) {
        this.container = container;
        this.pipeManualID = pipe.getManualID();
        this.pipeName = pipe.getName();
    }

    public SparseTimelinePipe(SparseTimeLinePipeContainer container, long pipeManualID, String pipeName) {
        this.pipeManualID = pipeManualID;
        this.pipeName = pipeName;
        this.container = container;
    }

//    public void setPipeManualID(int pipeManualID) {
//        this.pipeManualID = pipeManualID;
//    }
//
//    public void setPipeName(String pipeName) {
//        this.pipeName = pipeName;
//    }
    @Override
    public float getVelocity(int temporalIndex) {
        if (velocity == null) {
            container.loadTimelineVelocity(this, pipeManualID, pipeName);
        }
        return velocity[temporalIndex];
    }

    @Override
    public float getWaterlevel(int temporalIndex) {
        if (waterlevel == null) {
            container.loadTimelineWaterlevel(this, pipeManualID, pipeName);
        }
        return waterlevel[temporalIndex];
    }

    @Override
    public float getFlux(int temporalIndex) {
        if (flux == null) {
            container.loadTimelineFlux(this, pipeManualID, pipeName);
        }
        return flux[temporalIndex];
    }

    @Override
    public float getMass_reference(int temporalIndex) {
        if (mass_reference == null) {
            this.container.loadTimelineMass(this, pipeManualID, pipeName);
        }
        return mass_reference[temporalIndex];
    }
    
     @Override
    public float getConcentration_reference(int temporalIndex) {
        if (concentration_reference == null) {
            this.container.loadTimelineConcentration(this, pipeManualID, pipeName);
        }
        return concentration_reference[temporalIndex];
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    private void calculateActualValues() {
        this.actualVelocity = getValue_DoubleIndex(velocity, container.getActualTimeIndex_double());
        this.actualWaterlevel = getValue_DoubleIndex(waterlevel, container.getActualTimeIndex_double());
        this.actualFlux = getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
        this.actualTimestamp = container.getActualTime();
    }

    @Override
    public float getVelocity() {
        if (velocity == null) {
            container.loadTimelineVelocity(this, pipeManualID, pipeName);
        }
        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualVelocity;//getValue_DoubleIndex(velocity, container.getActualTimeIndex_double());
    }

    @Override
    public double getFlux() {
        if (flux == null) {
            container.loadTimelineFlux(this, pipeManualID, pipeName);
        }
        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualFlux;//return getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
    }

    @Override
    public double getWaterlevel() {
        if (waterlevel == null) {
            container.loadTimelineWaterlevel(this, pipeManualID, pipeName);
        }
        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualWaterlevel;//return getValue_DoubleIndex(waterlevel, container.getActualTimeIndex_double());
    }

    public void setFlux(float[] flux) {

        this.flux = flux;
    }

    public void setMass_reference(float[] mass_reference) {
        this.mass_reference = mass_reference;
    }
    
    public void setConcentration_reference(float[] concentration_reference) {
        this.concentration_reference = concentration_reference;
    }

    public void setVelocity(float[] velocity) {
        this.velocity = velocity;
    }

    public void setWaterlevel(float[] waterlevel) {
        this.waterlevel = waterlevel;
    }

    private float getValue_DoubleIndex(float[] array, double temporalIndexDouble) {
        float i = (float) temporalIndexDouble;
        if (((int) i) >= array.length - 1) {
            return array[array.length - 1];
        }
        float v0 = array[(int) i];
        float v1 = array[(int) i + 1];
        float ratio = i % 1;
        return (v0 + (v1 - v0) * ratio);
    }

    @Override
    public TimeContainer getTimeContainer() {
        return container;
    }

    @Override
    public boolean hasMass_reference() {
        return container.hasReferencePollution();
//        return mass_reference != null;
        //Also return false if the mass timeline is NOT YET loaded from datasource.
    }

    /**
     * Are arrays for velocity, flux and waterlevel initialized?
     *
     * @return
     */
    public boolean isInitialized() {
        return velocity != null && flux != null && waterlevel != null;
    }

}
