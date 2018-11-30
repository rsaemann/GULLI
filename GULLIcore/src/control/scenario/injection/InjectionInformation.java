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
package control.scenario.injection;

import model.GeoPosition2D;
import model.particle.Material;
import model.topology.Capacity;
import model.topology.Manhole;
import model.topology.Pipe;

/**
 * InjectionInformation manages the appearance of a spill event. Gives
 * Information of Location, time and material. Controller and Threadcontroller
 * organize the creation of particles for the simulation based on this
 * information.
 *
 * @author saemann
 */
public class InjectionInformation implements InjectionInfo {

//    private String pipename;
    private Capacity capacity;
    private double mass;
    private int numberOfParticles;
    private Material material;
    private double start, duration;//Seconds
    private double position1D;
    private boolean spillPipesystem;
    private GeoPosition2D position;
    private int triangleID = -1;

    private boolean changed = false;

    private String capacityName = null;

    /**
     * Injection after X seconds after Scenario (rain) start
     *
     * @param position location of spill
     * @param pipesystem injection directly to pipe system? surface if true
     * @param mass Mass to be spilled out [kg]
     * @param numberofParticles particles to represent the solute
     * @param material
     * @param startoffsetSeconds [sec] Start of spill after the simulation(rain)
     * start
     * @param duration [sec] duration of spilling at this location
     */
    public InjectionInformation(GeoPosition2D position, boolean pipesystem, double mass, int numberofParticles, Material material, double startoffsetSeconds, double duration) {
        this.position = position;
        this.mass = mass;
        this.material = material;
        this.start = startoffsetSeconds;
        this.duration = duration;
        this.numberOfParticles = numberofParticles;
        this.spillPipesystem = pipesystem;
    }

    /**
     * Instantan injection at seconds
     *
     * @param pipename
     * @param pipesystem
     * @param position
     * @param intensity
     * @param numberofParticles
     * @param material
     * @param startoffsetSeconds
     */
    public InjectionInformation(GeoPosition2D position, boolean pipesystem, double intensity, int numberofParticles, Material material, double startoffsetSeconds) {
        this(position, pipesystem, intensity, numberofParticles, material, startoffsetSeconds, 0);//Instantan injection
    }

    /**
     * Reference the Spillcapacity by its name. Can be added, if network is not
     * yet loaded.
     *
     * @param capacityName
     * @param position1D
     * @param mass
     * @param numberOfParticles
     * @param material
     * @param startoffsetSeconds
     * @param duration
     */
    public InjectionInformation(String capacityName, double position1D, double mass, int numberOfParticles, Material material, double startoffsetSeconds, double duration) {
        this.mass = mass;
        this.numberOfParticles = numberOfParticles;
        this.material = material;
        this.spillPipesystem = true;
        this.capacityName = capacityName;
        this.start = startoffsetSeconds;
        this.duration = duration;
        this.position1D = position1D;
    }

    public InjectionInformation(Capacity c, double position1D, double mass, int numberofParticles, Material material, double startoffsetSeconds, double duration) {
        this(null, false, mass, numberofParticles, material, startoffsetSeconds, duration);
        this.position1D = position1D;
        if (c != null) {
            this.capacity = c;
            this.capacityName = c.toString();
            this.position = capacity.getPosition3D(0);
            if (capacity instanceof Manhole || capacity instanceof Pipe) {
                spillPipesystem = true;
            }
        }
    }

    public InjectionInformation(int triangleID, double mass, int numberOfParticles, Material material, double startOffsetSeconds, double duration) {
        this.mass = mass;
        this.duration = duration;
        this.material = material;
        this.numberOfParticles = numberOfParticles;
        this.spillPipesystem = false;
        this.start = startOffsetSeconds;
        this.triangleID = triangleID;
    }

    public InjectionInformation(InjectionInfo info, double offsetSeconds) {
        this.capacity = info.getCapacity();
        this.duration = info.getDurationSeconds();
        this.mass = info.getMass();
        this.material = info.getMaterial();
        position = info.getPosition();
        this.spillPipesystem = info.spillInManhole();
        this.start = info.getStarttimeSimulationsAfterSimulationStart() + offsetSeconds;

        if (info instanceof InjectionInformation) {
            InjectionInformation ii = (InjectionInformation) info;
            this.triangleID = ii.triangleID;
            this.position1D = ii.position1D;
            this.numberOfParticles = ii.numberOfParticles;
            this.capacityName = ii.capacityName;
        }
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

//    public long getStart() {
//        return start;
//    }
//
//    public long getEnd() {
//        return end;
//    }
//    public double getPosition1D() {
//        return position1D;
//    }
    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    public void setNumberOfParticles(int numberOfParticles) {
        this.numberOfParticles = numberOfParticles;
        changed=true;
    }

    public void setMaterial(Material material) {
        this.material = material;
        this.changed=true;
    }
    
    
    

    public int getTriangleID() {
        return triangleID;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.mass) ^ (Double.doubleToLongBits(this.mass) >>> 32));
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.start) ^ (Double.doubleToLongBits(this.start) >>> 32));
//        hash = 79 * hash + (int) (Double.doubleToLongBits(this.position1D) ^ (Double.doubleToLongBits(this.position1D) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InjectionInformation other = (InjectionInformation) obj;
        if (Double.doubleToLongBits(this.mass) != Double.doubleToLongBits(other.mass)) {
            return false;
        }
        if (Double.doubleToLongBits(this.start) != Double.doubleToLongBits(other.start)) {
            return false;
        }
        if (this.triangleID != other.triangleID) {
            return false;
        }

        if (this.capacityName != null && other.capacityName != null) {
            if (!this.capacityName.equals(other.capacityName)) {
                return false;
            }
        }
        if (this.material != null && other.material != null) {
            if (!this.material.equals(other.material)) {
                return false;
            }
        }

//        if (Double.doubleToLongBits(this.position1D) != Double.doubleToLongBits(other.position1D)) {
//            return false;
//        }
        return true;
    }

    @Override
    public double getStarttimeSimulationsAfterSimulationStart() {
        return start;
    }

    @Override
    public double getDurationSeconds() {
        return duration;
    }

    @Override
    public boolean spillOnSurface() {
        return !this.spillPipesystem;
    }

    @Override
    public boolean spillInManhole() {
        return this.spillPipesystem;
    }

    @Override
    public GeoPosition2D getPosition() {
        return position;
    }

    public void setPosition(GeoPosition2D position) {
        this.position = position;
        this.changed = true;
    }

    @Override
    public Capacity getCapacity() {
        return this.capacity;
    }

    public void setCapacity(Capacity capacity) {
        this.capacity = capacity;
        this.changed = true;
    }

    public String getCapacityName() {
        return capacityName;
    }

    public void setStart(double start) {
        if (this.start == start) {
            return;
        }
//        System.out.println("Change starttime from "+this.start+"\tto "+start+" s after start.");
        this.start = start;
        this.changed = true;
    }

    public void setDuration(double duration) {
        if (this.duration == duration) {
            return;
        }
        this.duration = duration;
        this.changed = true;
    }

    public void setSpillPipesystem(boolean spillPipesystem) {
        this.spillPipesystem = spillPipesystem;
        this.changed = true;
    }

    /**
     * Return status of user Interaction since last threadcontroller particle
     * initialization.
     *
     * @return
     */
    public boolean isChanged() {
        return changed;
    }

    public void resetChanged() {
        this.changed = false;
    }

    /**
     * Position along pipe axis in meter
     *
     * @return
     */
    public double getPosition1D() {
        return position1D;
    }

    public void setTriangleID(int triangleID) {
        this.triangleID = triangleID;
        changed = true;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

}
