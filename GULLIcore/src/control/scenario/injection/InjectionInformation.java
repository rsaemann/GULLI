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

import java.util.Arrays;
import java.util.Objects;
import model.GeoPosition2D;
import model.particle.Material;
import model.timeline.TimedValue;
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

//    private static int runningID = 0;
    private int id = -1;

    public boolean spillOnSurface = false;

    /**
     * number of particles released in each interval.
     */
    protected int[] number_particles;

    /**
     * Start seconds of each interval after event start.
     */
    protected double[] timesteps;
    /**
     * Spilled mass of each interval [kg]
     */
    protected double[] spillMass;

    /**
     * Intensity at begin of interval [kg/s]
     */
    protected double[] intensity;

    /**
     * m³
     */
    protected double totalmass;
    /**
     * kg
     */
    protected double totalVolume;

    protected int totalNumberParticles;

    protected Material material;
    protected String capacityName;
    protected Capacity capacity;
    protected GeoPosition2D position;
    protected int triangleID = -1;
    protected double position1D;
    protected boolean changed = false;

    protected boolean active = true;

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
        this(startoffsetSeconds, duration, mass, numberofParticles);
        this.position = position;
        this.material = material;
        this.spillOnSurface = !pipesystem;
    }

    public InjectionInformation(double startoffsetSeconds, double duration, double mass, int numberOfParticles) {
        this.totalmass = mass;
        this.timesteps = new double[]{startoffsetSeconds, startoffsetSeconds + duration};
        this.spillMass = new double[]{mass, 0};
        this.intensity = new double[]{mass / duration, 0};
        this.number_particles = new int[]{numberOfParticles, 0};
        this.totalNumberParticles = numberOfParticles;
    }

    /**
     * Instantan injection at seconds
     *
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
        this(startoffsetSeconds, duration, mass, numberOfParticles);
        this.position1D = position1D;
        this.material = material;
        this.spillOnSurface = false;
        this.capacityName = capacityName;
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
                this.spillOnSurface = false;
            }
        }
    }

    public InjectionInformation(int triangleID, double mass, int numberOfParticles, Material material, double startOffsetSeconds, double duration) {
        this(startOffsetSeconds, duration, mass, numberOfParticles);
        this.material = material;
        this.spillOnSurface = true;
        this.triangleID = triangleID;
    }

    public InjectionInformation(InjectionInfo info, double offsetSeconds) {
        this(info.getStarttimeSimulationsAfterSimulationStart() + offsetSeconds, info.getDurationSeconds(), info.getMass(), ((InjectionInformation) info).getNumberOfParticles());

        this.capacity = info.getCapacity();
        this.material = info.getMaterial();
        position = info.getPosition();
        this.spillOnSurface = info.spillOnSurface();

        if (info instanceof InjectionInformation) {
            InjectionInformation ii = (InjectionInformation) info;
            this.triangleID = ii.triangleID;
            this.position1D = ii.position1D;
            this.capacityName = ii.capacityName;
        }
    }

    /**
     *
     * @param capacity
     * @param eventStart
     * @param eventEnd
     * @param material
     * @param timedValues
     * @param numberOfParticles
     * @param concentration kg/m³
     */
    public InjectionInformation(Capacity capacity, long eventStart, long eventEnd, TimedValue[] timedValues, Material material, double concentration, int numberOfParticles) {
        this.material = material;
        this.capacity = capacity;
        calculateMass(timedValues, eventStart, eventEnd, concentration);
        calculateNumberOfIntervalParticles(numberOfParticles);
    }

    private void calculateMass(TimedValue[] timedValues, long eventStart, long eventend, double concentration) {
        int timesInsimulationtime = 0;
        for (int i = 0; i < timedValues.length; i++) {
            TimedValue timedValue = timedValues[i];
            if (timedValue.time < eventStart) {
                continue;
            }
            timesInsimulationtime++;
            if (timedValue.time > eventend) {
                break;
            }

        }
        this.timesteps = new double[timesInsimulationtime];
        this.spillMass = new double[timesInsimulationtime];
        this.intensity = new double[timesInsimulationtime];
//        this.number_particles = new int[timesteps.length];

//        System.out.println("Messdaten injection only uses "+timesInsimulationtime+" of "+timedValues.length+" timesteps");
        double volume = 0;
//        double lastInterval = 0;
        totalmass = 0;
        int index = 0;
        for (int i = 0; i < timedValues.length - 1; i++) {
            TimedValue start = timedValues[i];
            if (start.time < eventStart) {
                continue;
            }
            if (start.time > eventend) {
                break;
            }
            timesteps[index] = (start.time - eventStart) / 1000.;
            TimedValue end = timedValues[i + 1];

            double seconds = (end.time - start.time) / 1000.;
            if (start.value < 0) {
                continue;
            }
            intensity[index] = timedValues[i].value;
            timesteps[index + 1] = timesteps[index] + seconds;

            double dV = (start.value + timedValues[i + 1].value) * 0.5 * seconds;
//            System.out.println("\tspill "+dV+" between "+timesteps[index]/60+"min and "+(timesteps[index]+seconds)/60+"min");
            spillMass[index] = dV * concentration;
            totalmass += dV * concentration;
//            lastInterval = seconds;
            volume += dV;
            index++;
        }

        this.totalVolume = volume;
    }

    private void calculateNumberOfIntervalParticles(int particles) {
//        double particlesPerMass = particles / totalmass;
        this.number_particles = new int[spillMass.length];
        double mass = 0;
        int particlesSoFar = 0;
        for (int i = 0; i < this.number_particles.length; i++) {
            mass += spillMass[i];
            double fraction = mass / totalmass;
            int particlesthisInterval = (int) (particles * fraction) - particlesSoFar;
            number_particles[i] = particlesthisInterval;
//            System.out.println(i+": mass:"+spillMass[i]+", frac:"+fraction+" -> particles: "+particlesSoFar+" + "+particlesthisInterval+"="+number_particles[i]+"  should be "+fraction*particles);
            particlesSoFar += particlesthisInterval;
        }
        int count = 0;
        for (int i = 0; i < number_particles.length; i++) {
            count += number_particles[i];
        }
//        System.out.println(count + "/" + particles + " angeforderte particel in Messdaten Spill.");
        this.totalNumberParticles = count;
    }

    @Override
    public double getStarttimeSimulationsAfterSimulationStart() {
        return timesteps[0];
    }

    @Override
    public double getDurationSeconds() {
        return (timesteps[timesteps.length - 1] - timesteps[0]);
    }

//    public void setMaterial(Material material) {
//        this.material = material;
//        this.changed = true;
//    }
//
//    public int getTriangleID() {
//        return triangleID;
//    }
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final InjectionInformation other = (InjectionInformation) obj;
//        if (Double.doubleToLongBits(this.totalmass) != Double.doubleToLongBits(other.totalmass)) {
//            return false;
//        }
//        if (Double.doubleToLongBits(this.timesteps[0]) != Double.doubleToLongBits(other.timesteps[0])) {
//            return false;
//        }
//        if (this.triangleID != other.triangleID) {
//            return false;
//        }
//
//        if (this.capacityName != null && other.capacityName != null) {
//            if (!this.capacityName.equals(other.capacityName)) {
//                return false;
//            }
//        }
//        if (this.material != null && other.material != null) {
//            if (!this.material.equals(other.material)) {
//                return false;
//            }
//        }
//
////        if (Double.doubleToLongBits(this.position1D) != Double.doubleToLongBits(other.position1D)) {
////            return false;
////        }
//        return true;
//    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InjectionInformation other = (InjectionInformation) obj;
        if (this.spillOnSurface != other.spillOnSurface) {
            return false;
        }
        if (!Arrays.equals(this.timesteps, other.timesteps)) {
            return false;
        }
        if (!Arrays.equals(this.spillMass, other.spillMass)) {
            return false;
        }
        if (!Objects.equals(this.material, other.material)) {
            return false;
        }
        if (!Objects.equals(this.capacityName, other.capacityName)) {
            return false;
        }
        if (!Objects.equals(this.position, other.position)) {
            return false;
        }
        return this.triangleID == other.triangleID;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + this.triangleID;
        return hash;
    }

    @Override
    public boolean spillInManhole() {
        return !spillOnSurface;
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

    public void setCapacityName(String capacityName) {
        this.capacityName = capacityName;
    }


    /**
     * Position along pipe axis in meter
     *
     * @return
     */
    public double getPosition1D() {
        return position1D;
    }

    public void setPosition1D(double position1D) {
        this.position1D = position1D;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public double getMass() {
        return totalmass;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean spillOnSurface() {
        return spillOnSurface;
    }

    @Override
    public int getNumberOfIntervals() {
        return timesteps.length - 1;
    }

    @Override
    public double getIntervalStart(int interval) {
        return timesteps[interval];
    }

    @Override
    public double getIntervalEnd(int interval) {
        return timesteps[interval + 1];
    }

    @Override
    public double massInInterval(int interval) {
        return spillMass[interval];
    }

    @Override
    public int particlesInInterval(int interval) {
        return number_particles[interval];
    }

    public int getNumberOfParticles() {
        int counter = 0;
        for (int i = 0; i < number_particles.length; i++) {
            counter += number_particles[i];
        }
        return counter;
    }

    public void setNumberOfParticles(int numberOfParticles) {
        calculateNumberOfIntervalParticles(numberOfParticles);
        changed = true;
    }

    public void setStart(double start) {
        if (timesteps[0] == start) {
            return;
        }
//        System.out.println("Change starttime from "+this.start+"\tto "+start+" s after start.");
        double duration = getDurationSeconds();
        this.timesteps[0] = start;
        this.timesteps[1] = start + duration;
        this.changed = true;
    }

    public void setDuration(double duration) {
        if (this.timesteps[1] == timesteps[0] + duration) {
            return;
        }
        this.timesteps[1] = timesteps[0] + duration;
        this.changed = true;
    }

    public void setSpillPipesystem(boolean spillPipesystem) {
        this.spillOnSurface = !spillPipesystem;
        this.changed = true;
    }

    public int getTriangleID() {
        return triangleID;
    }

    public void setTriangleID(int triangleID) {
        if (this.triangleID == triangleID) {
            return;
        }
        this.triangleID = triangleID;
        spillOnSurface = triangleID >= 0;
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

    @Override
    public double getIntervalDuration(int interval) {
        return timesteps[interval + 1] - timesteps[interval];
    }

    @Override
    public String toString() {
        return "InjectionInformation{" + id + ", OnSurface=" + spillOnSurface + ", #particles=" + totalNumberParticles + ", totalmass=" + totalmass + ", totalVolume=" + totalVolume + ", material=" + material + (spillOnSurface ? ", SurfaceTriangle=" + triangleID : ", capacityName=" + capacityName) + '}';
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public double getIntensity(int intervalIndex) {
        return intensity[intervalIndex];
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.changed = true;
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

}
