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
package control.scenario;

import control.scenario.injection.InjectionInformation;
import control.Controller;
import java.util.ArrayList;
import model.particle.Material;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.timeline.array.TimeIndexCalculator;

/**
 * Information about the time of Simulation. Scenarios handle the input
 * velocity/waterlevel and output pipe measurements.
 *
 * @author saemann
 */
public abstract class Scenario {

    /**
     * Information about start/end and observed timesteps for velocity of pipes
     */
    protected TimeIndexCalculator timesPipe;

    /**
     * Information about start/end and observed timesteps for velocity of pipes
     */
    protected TimeIndexCalculator timesManhole;

    /**
     * Information about start/end and observed timesteps for velocity of
     * surface
     */
    protected TimeIndexCalculator timesSurface;

    /**
     * Information about start/end and observed timesteps for velocity of soil
     */
    protected TimeIndexCalculator timesSoil;

    protected ArrayTimeLineMeasurementContainer measurementsPipe;

//    protected ArrayList<Material> materials;
    protected long starttime;

    protected long endtime;
    
    protected String name;

    public long getStartTime() {
        return starttime;
    }

    public long getEndTime() {
        return endtime;
    }

    public void setActualTime(long time) {
        if (timesPipe != null) {
            timesPipe.setActualTime(time);
        }
        if (timesManhole != null) {
            timesManhole.setActualTime(time);
        }
        if (timesSurface != null) {
            timesSurface.setActualTime(time);
        }
        if (timesSoil != null) {
            timesSoil.setActualTime(time);
        }
        if (measurementsPipe != null) {
            measurementsPipe.setActualTime(time);
        }
    }

    public ArrayList<InjectionInformation> getInjections() {
        return null;
    }

    public abstract void init(Controller c);

    public abstract void reset();

    public void setTimesPipe(TimeIndexCalculator timesPipe) {
        this.timesPipe = timesPipe;
        recalculateStartEndTime();
    }

    public void setTimesSurface(TimeIndexCalculator timesSurface) {
        this.timesSurface = timesSurface;
        recalculateStartEndTime();
    }

    public void setTimesSoil(TimeIndexCalculator timesSoil) {
        this.timesSoil = timesSoil;
        recalculateStartEndTime();
    }

    /**
     * Calculates the Scenario's start and end time from the used timearrays for
     * connected domains.
     */
    protected void recalculateStartEndTime() {
        long newStart = Long.MAX_VALUE;
        long newEnd = 0;
        if (timesPipe != null) {
            newStart = Math.min(newStart, timesPipe.getStartTime());
            newEnd = Math.max(newEnd, timesPipe.getEndTime());
        }
        if (timesManhole != null) {
            newStart = Math.min(newStart, timesManhole.getStartTime());
            newEnd = Math.max(newEnd, timesManhole.getEndTime());
        }
        if (timesSurface != null) {
            newStart = Math.min(newStart, timesSurface.getStartTime());
            newEnd = Math.max(newEnd, timesSurface.getEndTime());
        }
        if (timesSoil != null) {
            newStart = Math.min(newStart, timesSoil.getStartTime());
            newEnd = Math.max(newEnd, timesSoil.getEndTime());
        }
        this.starttime = newStart;
        this.endtime = newEnd;
    }

    public TimeIndexCalculator getTimesPipe() {
        return timesPipe;
    }

    public TimeIndexCalculator getTimesSurface() {
        return timesSurface;
    }

    public TimeIndexCalculator getTimesSoil() {
        return timesSoil;
    }

    public long getStarttime() {
        return starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public ArrayTimeLineMeasurementContainer getMeasurementsPipe() {
        return measurementsPipe;
    }

    public void setMeasurementsPipe(ArrayTimeLineMeasurementContainer measurementsPipe) {
        this.measurementsPipe = measurementsPipe;
    }

    public TimeIndexCalculator getTimesManhole() {
        return timesManhole;
    }

    public void setTimesManhole(TimeIndexCalculator timesManhole) {
        this.timesManhole = timesManhole;
        recalculateStartEndTime();
    }

    public int getMaxMaterialID() {
        int max = -1;
        for (InjectionInformation injection : getInjections()) {
            if (injection != null && injection.getMaterial() != null) {
                max = Math.max(max, injection.getMaterial().materialIndex);
            }
        }
        return max;
    }

    public Material getMaterialByIndex(int materialIndex) {
        ArrayList<InjectionInformation> injs = getInjections();
        for (InjectionInformation inj : injs) {
            if (inj.getMaterial() != null && inj.getMaterial().materialIndex == materialIndex) {
                return inj.getMaterial();
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    

}
