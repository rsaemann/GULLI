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
package com.saemann.gulli.core.control.scenario.injection;

import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.topology.Capacity;

/**
 *
 * @author saemann
 */
public interface InjectionInfo {

    public double getStarttimeSimulationsAfterSimulationStart();

    public double getDurationSeconds();

    public double getMass();
    
    

    public Material getMaterial();

    public boolean spillOnSurface();

    public boolean spillInManhole();

    public GeoPosition2D getPosition();

    /**
     * May be NULL!
     *
     * @return
     */
    public Capacity getCapacity();

    int getNumberOfIntervals();

    /**
     * Start of interval in seconds after event start.
     *
     * @param interval
     * @return
     */
    double getIntervalStart(int interval);

    /**
     * End of interval in seconds after event start.
     *
     * @param interval
     * @return
     */
    double getIntervalEnd(int interval);

    /**
     * Duration of interval in seconds
     *
     * @param interval
     * @return
     */
    double getIntervalDuration(int interval);

    double massInInterval(int interval);

    int particlesInInterval(int interval);
    
    public double getIntensity(int intervalIndex);

    public void setCapacity(Capacity capacity);

    public void setId(int i);
    
    public boolean hasChanged();
    
    public boolean isActive();
    
    public void setActive(boolean active);

    public int getMaterialID();

    public void setMaterial(Material material);
    
    public int getNumberOfParticles();
    
    public void setNumberOfParticles(int number);

    public void resetChanged();

    public int getId();
}
