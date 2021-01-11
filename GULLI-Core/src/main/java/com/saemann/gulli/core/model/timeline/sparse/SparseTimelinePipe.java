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
package com.saemann.gulli.core.model.timeline.sparse;

import java.util.concurrent.locks.ReentrantLock;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.Pipe;

/**
 * If timeline values are needed, they are loaded on time from a dataprovider
 * (e.g. database). safes lots of space.
 *
 * @author saemann
 */
public class SparseTimelinePipe implements TimeLinePipe {

    private Pipe pipe;

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
     * Fluidvolume in pipe [m³]
     */
    private float[] volume;
    /**
     * VolumeFlow q in pipe in [m³/s]
     */
    private float[] flux;
    /**
     * Reference mass if given in scenario. May be null [times][materialtypes]
     */
    private float[][] mass_reference;

    /**
     * Reference concentration if given in scenario. May be null
     */
    private float[][] concentration_reference;

    private float actualVelocity, actualWaterlevel, actualDischarge, actualVolume;
    private long actualTimestamp;

    private ReentrantLock lock = new ReentrantLock();

    public SparseTimelinePipe(SparseTimeLinePipeContainer container, Pipe pipe) {
        this.pipe = pipe;
        this.container = container;
        this.pipeManualID = pipe.getManualID();
        this.pipeName = pipe.getName();
    }

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
    public float getVolume(int temporalIndex) {
        if (volume == null) {
            if (waterlevel == null) {
                //calculate fluid volume
                container.loadTimelineWaterlevel(this, pipeManualID, pipeName);
            }
            volume = new float[waterlevel.length];
            for (int i = 0; i < waterlevel.length; i++) {
                volume[i] = (float) (pipe.getProfile().getFlowArea(waterlevel[i]) * pipe.getLength());
            }
        }
        return volume[temporalIndex];
    }

    @Override
    public float getDischarge(int temporalIndex) {
        if (flux == null) {
            container.loadTimelineDischarge(this, pipeManualID, pipeName);
        }
        return flux[temporalIndex];
    }

    @Override
    public float getMassflux_reference(int temporalIndex, int material) {
        if (mass_reference == null) {
            this.container.loadTimelineMassflux(this, pipeManualID, pipeName);
        }
        if(mass_reference==null)return 0;
        return mass_reference[temporalIndex][material];
    }

    @Override
    public float getConcentration_reference(int temporalIndex, int material) {
        if (concentration_reference == null) {
            this.container.loadTimelineConcentration(this, pipeManualID, pipeName);
        }
        return concentration_reference[temporalIndex][material];
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    private void calculateActualValues() {
//        if(lock.isLocked()){
//            System.out.println(pipeName+"Lock is locked , queued "+lock.getQueueLength()+"  threads queued?"+lock.hasQueuedThreads());
//        }
//        else{
//            System.out.println("Lock is free on "+pipeName);
//        }
        lock.lock();
//        System.out.println(pipeName+"entered , waiting: "+lock.getQueueLength());
       
        try {
            if (this.actualTimestamp == container.getActualTime()) {
//                System.out.println(pipeName+"already calculated ");
                return;//Already calculated by another thread.
            }
//            System.out.println(pipeName+"  calculate ");
            if (velocity == null) {
                container.loadTimelineVelocity(this, pipeManualID, pipeName);
            }
            if (flux == null) {
                container.loadTimelineDischarge(this, pipeManualID, pipeName);
            }
            if (waterlevel == null) {
                container.loadTimelineWaterlevel(this, pipeManualID, pipeName);

            }
            if (volume == null&&waterlevel!=null) {
                //calculate fluid volume
                volume = new float[waterlevel.length];
                for (int i = 0; i < waterlevel.length; i++) {
                    volume[i] = (float) (pipe.getProfile().getFlowArea(waterlevel[i]) * pipe.getLength());
                }
            }
            this.actualVelocity = getValue_DoubleIndex(velocity, container.getActualTimeIndex_double());
            this.actualWaterlevel = getValue_DoubleIndex(waterlevel, container.getActualTimeIndex_double());
            this.actualDischarge = getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
            this.actualVolume = getValue_DoubleIndex(volume, container.getActualTimeIndex_double());
            this.actualTimestamp = container.getActualTime();
//            System.out.println(pipeName+"  finished calculation actual values on ");
        } finally {
//            System.out.println(pipeName+"unlock , waiting: "+lock.getQueueLength());
            lock.unlock();
        }
    }

    @Override
    public float getVelocity() {

        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualVelocity;//getValue_DoubleIndex(velocity, container.getActualTimeIndex_double());
    }

    @Override
    public double getDischarge() {

        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualDischarge;//return getValue_DoubleIndex(flux, container.getActualTimeIndex_double());
    }

    @Override
    public double getWaterlevel() {

        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualWaterlevel;//return getValue_DoubleIndex(waterlevel, container.getActualTimeIndex_double());
    }

    @Override
    public double getVolume() {
        if (actualTimestamp != container.getActualTime()) {
            calculateActualValues();
        }
        return this.actualVolume;//return getValue_DoubleIndex(waterlevel, container.getActualTimeIndex_double());
    }

    public void setFlux(float[] flux) {

        this.flux = flux;
    }

    public void setMassflux_reference(float[][] mass_reference) {
        this.mass_reference = mass_reference;
    }

    public void setConcentration_reference(float[][] concentration_reference) {
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
    public boolean hasMassflux_reference() {
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

    @Override
    public String[] getMaterialNames() {
        return container.namesMaterials;
    }

}
