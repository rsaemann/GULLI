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

import com.saemann.gulli.core.control.particlecontrol.injection.ManholeInjection;
import com.saemann.gulli.core.io.extran.HE_Database.AreaRunoffSplit;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * This is a preObject that contains Injection information from a HYSTEM EXTRAN
 * SCENARIO. As it only contains the Name of a Pipe/Manhole and the Simulation
 * start time, it has to be converted to a usable Injection form
 *
 * @author saemann
 */
public class HEAreaInflow1DInformation implements InjectionInfo {

//    public final String capacityName;
//    public final long stattime, endtime;
//    public final double mass;
    /**
     * List of possible Runoff parameters to chose. These are loaded when
     * opening the HE Database. (set by LoadingCoordinator)
     */
    public static String[] runoffParameterList = new String[]{"All"};

    public Material material;
    public double relativePosition;
    private boolean changed = false;
    private int materialID;

//    public HashMap<String, Manhole> manholeMap;
    public Network network;

    public int numberOfParticles;

    public HashMap<String, Double> effectiveRunoffArea;
    /**
     * HE Parameter: Abflussparameter
     */
    public String runoffParameterName;

    /**
     * HE Stoffparameter Name
     */
    public String substanceParameterName;

    /**
     * kg/m^2
     */
    public double massload;

    /**
     * 1/mm Abtragsrate
     */
    public double washoffConstant;

//    /**
//     * [mm]
//     */
//    public double totalprecipitation;
    public Collection<AreaRunoffSplit> areaRunoffSplit;
    private int id;
    private boolean active = true;
    private double mass = 0;

    private boolean initilized = false;

    public int numberAreaObjects = 0;
    public double effectiveArea = 0;
    public double effectiveVolume = 0;

    public HEAreaInflow1DInformation(String runoffParameter, Material mat, int numberofparticles) {
        this.runoffParameterName = runoffParameter;
        this.numberOfParticles = numberofparticles;
        this.material = mat;
    }

    public void calculateManholesArea() {
        if (effectiveRunoffArea != null) {
            effectiveRunoffArea.clear();
        } else {
            effectiveRunoffArea = new HashMap<>(30);
        }
        for (AreaRunoffSplit ars : areaRunoffSplit) {
            double upper = ars.effectiveRunoffArea * ars.fractionUpper;
            double lower = ars.effectiveRunoffArea * (1. - ars.fractionUpper);

            Double up = effectiveRunoffArea.get(ars.upperManholeName);
            if (up == null) {
                effectiveRunoffArea.put(ars.upperManholeName, upper);
            } else {
                effectiveRunoffArea.put(ars.upperManholeName, upper + up);
            }
            Double low = effectiveRunoffArea.get(ars.lowerManholeName);
            if (low == null) {
                effectiveRunoffArea.put(ars.lowerManholeName, lower);
            } else {
                effectiveRunoffArea.put(ars.lowerManholeName, lower + low);
            }
        }
        initilized = true;
    }

    public boolean isInitilized() {
        return initilized;
    }

    public void SetToReinitilize() {
        initilized = false;
    }

    public ArrayList<Particle> createParticles() {
        if (!initilized) {
            calculateManholesArea();
        }
        //Statistics counter (for tooltips in GUI)
        effectiveArea = 0;
        effectiveVolume = 0;
        numberAreaObjects = 0;

        ArrayList<Particle> particles = new ArrayList<>(numberOfParticles);
        //Calculate total washed off mass
        double washoffMass = 0;
        boolean considerwashofftype = false;
        if (runoffParameterName == null || runoffParameterName.isEmpty() || runoffParameterName.equals("All")) {
            considerwashofftype = false;
        } else {
            considerwashofftype = true;
        }
        System.out.println("Check runoff type "+considerwashofftype+" for "+runoffParameterName);
        for (AreaRunoffSplit ars : areaRunoffSplit) {
            if (considerwashofftype) {
                if (!ars.washoffParameter.equals(runoffParameterName)) {
                    continue;
                }
            }
//            totalwashoffarea += ars.effectiveRunoffArea;
            double totalprecipitation = ars.volume / ars.effectiveRunoffArea * 1000;//-> mm precipitation
            double washoffFraction = (Math.min(1, totalprecipitation * washoffConstant));
            double am = ars.effectiveRunoffArea * massload;
            double wom = am * washoffFraction;
            washoffMass += wom;
            ars.massUpper = wom * ars.fractionUpper;
            ars.massLower = wom * (1 - ars.fractionUpper);
            effectiveArea += ars.effectiveRunoffArea;
            effectiveVolume += ars.volume;
            numberAreaObjects++;
        }
        
        mass = washoffMass;
//        System.out.println("Mass for washoff: " + washoffMass + "  on " + totalwashoffarea + " m² (" + areaRunoffSplit.size() + " areas)");
        //Calculate fraction of mass per particle
        //@TODO check integration of washed off mass 
        double massperParticle = washoffMass / (double) numberOfParticles;
//        System.out.println("Mass per particle: " + massperParticle);
        //Create particles for each area by considering the linkes area as a fraction of the whole inflow
//        System.out.println("Runoff areas defined for " + effectiveRunoffArea.size() + " manholes.");
        for (AreaRunoffSplit ars : areaRunoffSplit) {
            if (considerwashofftype) {
                if (!ars.washoffParameter.equals(runoffParameterName)) {
                    continue;
                }
            }
            //Upper manhole
            Manhole mh = network.getManholeByName(ars.upperManholeName);
            ManholeInjection mhi = new ManholeInjection(mh);
            if (mh == null) {
//                System.err.println("Cannot find Manhole " + ars.upperManholeName);
            } else {
                TimeLineManhole tl = mh.getStatusTimeLine();
                double massreservoir = ars.massUpper;
                double areafraction = 0;
                try {
                    areafraction = ars.effectiveRunoffArea * ars.fractionUpper / effectiveRunoffArea.get(ars.upperManholeName);
                } catch (Exception e) {
                    System.err.println("Cannot find area of " + ars.upperManholeName);
                    continue;
                }
                double masstorelease = 0;
                for (int i = 0; i < tl.getNumberOfTimes() - 1; i++) {
                    if (massreservoir < 0) {
                        break;
                    }
                    // m^3/s
                    float inflowCurrent = tl.getInflow(i);
                    float inflowNext = tl.getInflow(i + 1);
                    //Intervallduration [s]
                    double duration = (tl.getTimeContainer().getTimeMilliseconds(i + 1) - tl.getTimeContainer().getTimeMilliseconds(i)) / 1000.;
                    //m^3
                    double volume = (inflowCurrent + inflowNext) * 0.5 * duration;
                    double volumeOnArea = volume * areafraction;
                    double eff_precipitation = volumeOnArea / (ars.effectiveRunoffArea * ars.fractionUpper);

                    double masswashoffinInterval = massreservoir * washoffConstant * eff_precipitation;
                    masstorelease += masswashoffinInterval;
                    //Number of particles to release in interval:
                    int particles_toRelease = (int) (masstorelease / massperParticle);
//                    System.out.println(masstorelease+" / "+massperParticle+" -> "+particles_toRelease+" particles");
                    //Only release total number of particles in interval
                    if (masstorelease > 0) {
                        if (particles_toRelease > 0) {
                            float massperparticle = (float) (masswashoffinInterval / particles_toRelease);
                            double timedistance = duration / (double) particles_toRelease;
                            for (int j = 0; j < particles_toRelease; j++) {
                                long insertiontime = (long) (tl.getTimeContainer().getTimeMilliseconds(i) + (j + 0.5) * timedistance * 1000);
                                Particle p = new Particle(material, mhi, massperparticle, insertiontime);
                                particles.add(p);
                                massreservoir -= p.getParticleMass();
                                masstorelease -= p.getParticleMass();
                            }
                        } else {
                            //Create a single particle, carrying the emitted mass of this interval
                            long insertiontime = (long) (0.5 * (tl.getTimeContainer().getTimeMilliseconds(i) + tl.getTimeContainer().getTimeMilliseconds(i + 1)));
                            Particle p = new Particle(material, mhi, (float) masstorelease, insertiontime);
                            particles.add(p);
                            massreservoir -= p.getParticleMass();
                            masstorelease -= p.getParticleMass();
                        }
                    }

                }
//                System.out.println("Left " + massreservoir + "/" + ars.massUpper + " (" + masstorelease + ") on area " + ars.areaName);
            }
        }

        return particles;
    }

    @Override
    public double getStarttimeSimulationsAfterSimulationStart() {
        return 0;
    }

    @Override
    public double getDurationSeconds() {
        return 0;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean spillOnSurface() {
        return false;
    }

    @Override
    public boolean spillInManhole() {
        return true;
    }

    @Override
    public GeoPosition2D getPosition() {
        return null;
    }

    @Override
    public Capacity getCapacity() {
        return null;
    }

    @Override
    public int getNumberOfIntervals() {
        return -1;
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
        return 0;
    }

    @Override
    public int particlesInInterval(int interval) {
        return 0;
    }

    @Override
    public double getIntensity(int intervalIndex) {
        return 0;
    }

    @Override
    public void setCapacity(Capacity capacity) {

    }

    @Override
    public void setId(int i) {
        this.id = i;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public int getMaterialID() {
        return material.materialIndex;
    }

    public void setMaterialID(int materialID) {
        if (material == null || material.materialIndex != materialID) {
            this.materialID = materialID;

            changed = true;
        }
    }

    @Override
    public void setMaterial(Material material) {
        this.material = material;
        changed = true;
    }

    @Override
    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    @Override
    public void setNumberOfParticles(int number) {
        this.numberOfParticles = number;
        this.changed = true;

    }

    @Override
    public void resetChanged() {
        this.changed = false;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getRunoffParameterName() {
        return runoffParameterName;
    }

    public void setRunoffParameterName(String runoffParameterName) {
        this.runoffParameterName = runoffParameterName;
        changed = true;
    }

    public String getSubstanceParameterName() {
        return substanceParameterName;
    }

    public void setSubstanceParameterName(String substanceParameterName) {
        this.substanceParameterName = substanceParameterName;
        changed = true;
    }

    /**
     * kg/m^3
     *
     * @return
     */
    public double getMassload() {
        return massload;
    }

    /**
     * kg/m^3
     *
     * @param massload
     */
    public void setMassload(double massload) {
        this.massload = massload;
        this.changed = true;
    }

    public double getWashoffConstant() {
        return washoffConstant;
    }

    public void setWashoffConstant(double washoffConstant) {
        this.washoffConstant = washoffConstant;
        changed = true;
    }

    public class HaltungHYS2 {

        /**
         * Total are in squaremeter
         */
        public double area_total;

        /**
         *
         */
        public double total_inflow;
    }

}
