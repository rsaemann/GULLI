/*
 * The MIT License
 *
 * Copyright 2021 Saemann.
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
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Capacity;

/**
 * Relative load of diffusive pollutant per area (kg/m^2) information will be
 * calculated to total mass for injection
 *
 * @author saemann
 */
public class InjectionArealInformation implements InjectionInfo {

    protected Material material;

    protected int materialID = -1;

    protected int id = -1;

    /**
     * kg
     */
    protected double totalMass;

    /**
     * kg/m^2
     */
    protected double load;

    protected Surface surf;

    protected int numberOfParticles;

    protected boolean active = true;

    protected boolean changed = true;

    protected double startSeconds = 0;

    protected double durationSeconds = 0;

    /**
     *
     * @param material
     * @param surface
     * @param totalmass kg
     * @param numberOfParticles
     */
    public InjectionArealInformation(Material material, Surface surface, double totalmass, int numberOfParticles) {
        this.material = material;
        this.totalMass = totalmass;
//        this.load = load;
        this.surf = surface;
        if (surface != null) {
            this.load = totalmass / surface.calcTotalTriangleArea();
//            this.totalMass = load * surface.calcTotalTriangleArea();
        }
        this.numberOfParticles = numberOfParticles;
    }

    public static InjectionArealInformation LOAD(Material mat, Surface surf, double arealLoad, int numberOfParticles) {
        InjectionArealInformation iai = new InjectionArealInformation(mat, surf, 0, numberOfParticles);
        iai.setLoad(arealLoad);
        return iai;
    }

    public static InjectionArealInformation TOTALMASS(Material mat, Surface surf, double totalMass, int numberOfParticles) {
        InjectionArealInformation iai = new InjectionArealInformation(mat, surf, totalMass, numberOfParticles);
        return iai;
    }

    /**
     * kg/m^2
     *
     * @return
     */
    public double getLoad() {
        return load;
    }

    /**
     *
     * @param load kg/m^2
     */
    public void setLoad(double load) {
        if (this.load == load) {
            return;
        }
        this.load = load;
        if (surf != null) {
            this.totalMass = load * surf.calcTotalTriangleArea();
        }
        changed = true;
    }

    @Override
    public double getStarttimeSimulationsAfterSimulationStart() {
        return startSeconds;
    }

    @Override
    public double getDurationSeconds() {
        return 0;
    }

    @Override
    public double getMass() {
        return totalMass;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean spillOnSurface() {
        return true;
    }

    @Override
    public boolean spillInManhole() {
        return false;
    }

    @Override
    public GeoPosition2D getPosition() {
        if (surf == null) {
            return null;
        }
        return surf.getPosition3D(0);
    }

    @Override
    public Capacity getCapacity() {
        return surf;
    }

    @Override
    public int getNumberOfIntervals() {
        return 1;
    }

    @Override
    public double getIntervalStart(int interval) {
        return 0;
    }

    @Override
    public double getIntervalEnd(int interval) {
        return 0;
    }

    @Override
    public double getIntervalDuration(int interval) {
        return 0;
    }

    @Override
    public double massInInterval(int interval) {
        return totalMass;
    }

    @Override
    public int particlesInInterval(int interval) {
        return numberOfParticles;
    }

    @Override
    public double getIntensity(int intervalIndex) {
        return totalMass;
    }

    @Override
    public void setCapacity(Capacity capacity) {
        if (capacity == surf) {
            return;
        }
        if (capacity instanceof Surface) {
            surf = ((Surface) capacity);
            if (load == 0 && totalMass > 0) {
                this.load = totalMass / surf.calcTotalTriangleArea();
            } else {
                this.totalMass = this.surf.calcTotalTriangleArea() * load;
            }
            changed = true;
        }
    }

    @Override
    public void setId(int id) {
        this.id = id;
        changed = true;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    /**
     * Set the change flag to false. Should be called, after particles have been
     * addapted to the new values.
     */
    @Override
    public void resetChanged() {
        changed = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public int getMaterialID() {
        return materialID;
    }

    @Override
    public void setMaterial(Material material) {
        if (this.material == material) {
            return;
        }
        this.material = material;
        changed = true;
    }

    @Override
    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setMaterialID(int mid) {
        if (this.materialID == mid) {
            return;
        }
        this.materialID = mid;
        this.material = null;
        changed = true;
    }

    public void setStart(double s) {
        if (startSeconds == s) {
            return;
        }
        this.startSeconds = s;
        changed = true;
    }

    public void setDuration(double d) {
        if (durationSeconds == d) {
            return;
        }
        this.durationSeconds = d;
        changed = true;
    }

    @Override
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        changed = true;
    }

    @Override
    public void setNumberOfParticles(int number) {
        if (this.numberOfParticles == number) {
            return;
        }
        this.numberOfParticles = number;
        changed = true;
    }

    public void setMass(double mass) {
        if (this.totalMass == mass) {
            return;
        }
        this.totalMass = mass;
        if (surf != null) {
            load = totalMass / surf.calcTotalTriangleArea();
        }
        changed = true;
    }

    public Surface getSurface() {
        return surf;
    }

}
