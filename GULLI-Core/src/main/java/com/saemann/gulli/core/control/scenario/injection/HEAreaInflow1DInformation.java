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
import com.saemann.gulli.core.model.timeline.RainGauge;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

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

    public enum RUNOFF_CONTROL {
        /**
         * Use raw precipitation information to calculate washoff from an
         * initial pollutant reservoir. (Does not consider retention)
         */
        PRECIPITATION,
        /**
         * Use a constant pollutant concentration. in the inflow at the
         * manholes.
         */
        CONCENTRATION,
        /**
         * Use the information of the inflow at the manholes to calculate the
         * precipitation and a washoff with the given initial massload.
         */
        INFLOW_WASHOFF
    };

    public RUNOFF_CONTROL inflowtype = RUNOFF_CONTROL.PRECIPITATION;

    public Material material;
    public double relativePosition;
    private boolean changed = false;
    private int materialID;

    private RainGauge precipitation;

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
            double upper = ars.area * ars.fractionUpper;
            double lower = ars.area * (1. - ars.fractionUpper);

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
        if (inflowtype == RUNOFF_CONTROL.INFLOW_WASHOFF || inflowtype == RUNOFF_CONTROL.PRECIPITATION) {

            //Statistics counter (for tooltips in GUI)
            effectiveArea = 0;
            effectiveVolume = 0;
            numberAreaObjects = 0;

            ArrayList<Particle> particles = new ArrayList<>(numberOfParticles);
            //Calculate total washed off mass
            double totalwashoffMass = 0;
            boolean considerwashofftype = false;
            if (runoffParameterName == null || runoffParameterName.isEmpty() || runoffParameterName.equals("All")) {
                considerwashofftype = false;
            } else {
                considerwashofftype = true;
            }
//            System.out.println("Check runoff type " + considerwashofftype + " for " + runoffParameterName);
            DecimalFormat df3 = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));
            for (AreaRunoffSplit ars : areaRunoffSplit) {
                if (considerwashofftype) {
                    if (!ars.washoffParameter.equals(runoffParameterName)) {
                        continue;
                    }
                }
//            totalwashoffarea += ars.area;
//            double totalprecipitation = ars.runoffVolume / ars.area * 1000;//-> mm precipitation
//            
                double areamasspotential = ars.area * massload;
                double washoffFraction = (Math.min(1, washoffConstant * ars.runofffraction * ars.totalPrecipitationMM));
                double washoffMass = areamasspotential * washoffFraction;// = areamass * washoffFraction;
                totalwashoffMass += washoffMass;//ars.washoffMass;
//                System.out.println("Potential mass:"+areamasspotential+"kg /"+ars.washoffMass+"kg \t washoff:"+washoffMass);
                ars.massUpper = /*washoffMass*/ areamasspotential * ars.fractionUpper;
                ars.massLower = /*washoffMass*/ areamasspotential * (1 - ars.fractionUpper);
                effectiveArea += ars.area;
                effectiveVolume += ars.runoffVolume;
                numberAreaObjects++;

//                System.out.println(ars.areaName + "/" + ars.pipename + ": calculated mass= " + df3.format(washoffMass) + " \t<database: " + df3.format(ars.washoffMass) + " bei " + (int) (ars.runofffraction * 100) + "%  volumen= " + df3.format(ars.runoffVolume) + "m³ to " + ars.upperManholeName + " & " + ars.lowerManholeName);
            }
//            System.out.println("total washoff="+totalwashoffMass);
            mass = totalwashoffMass;
//        System.out.println("Mass for washoff: " + washoffMass + "  on " + totalwashoffarea + " m² (" + areaRunoffSplit.size() + " areas)");
            //Calculate fraction of mass per particle
            //@TODO check integration of washed off mass 
            double massperParticle = totalwashoffMass / (double) numberOfParticles;
//        System.out.println("Mass per particle: " + massperParticle);
            //Create particles for each area by considering the linkes area as a fraction of the whole inflow
//        System.out.println("Runoff areas defined for " + area.size() + " manholes.");
            float totalrelesemass = 0;
            for (AreaRunoffSplit ars : areaRunoffSplit) {
                if (considerwashofftype) {
                    if (!ars.washoffParameter.equals(runoffParameterName)) {
                        continue;
                    }
                }
                //Upper manhole
                Manhole mhUP = network.getManholeByName(ars.upperManholeName);
                ManholeInjection mhiUP = new ManholeInjection(mhUP);
                Manhole mhDW = network.getManholeByName(ars.lowerManholeName);
                ManholeInjection mhiDW = new ManholeInjection(mhDW);
                if (mhUP == null) {
                    System.err.println("Cannot find Manhole " + ars.upperManholeName);
                } else if (mhDW == null) {
                    System.err.println("Cannot find Manhole " + ars.lowerManholeName);
                } else {

                    double massreservoir = ars.massUpper + ars.massLower;
                    totalrelesemass += massreservoir;
                    double areafraction = 0;
                    try {
                        areafraction = ars.area * ars.fractionUpper / effectiveRunoffArea.get(ars.upperManholeName);
                    } catch (Exception e) {
                        System.err.println("Cannot find area of " + ars.upperManholeName);
                        continue;
                    }
                    double masstorelease = 0;
                    double released = 0;
                    if (inflowtype == RUNOFF_CONTROL.INFLOW_WASHOFF) {
                        TimeLineManhole tl = mhUP.getStatusTimeLine();
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
                            double eff_precipitation = volumeOnArea * 1000. / (ars.area * ars.fractionUpper); // m->mm
//                            System.out.println("Vol="+volumeOnArea+"m^3  / "+ars.area+"m^2 = "+eff_precipitation+"mm");
                            double masswashoffinInterval = massreservoir * washoffConstant * eff_precipitation;
                            if (masswashoffinInterval > massreservoir) {
                                masswashoffinInterval = massreservoir;
                            }
                            massreservoir -= masswashoffinInterval;
                            masstorelease += masswashoffinInterval;
                            //Number of particles to release in interval:
                            int particles_toRelease = (int) (masstorelease / (massperParticle * 2));
                            //Only release total number of particles in interval
                            if (masstorelease > 0) {
                                if (particles_toRelease > 0) {
                                    float massperparticle = (float) (masswashoffinInterval / particles_toRelease);
                                    double timedistance = duration / (double) particles_toRelease;
                                    for (int j = 0; j < particles_toRelease; j++) {
                                        long insertiontime = (long) (tl.getTimeContainer().getTimeMilliseconds(i) + (j + 0.5) * timedistance * 1000);
                                        Particle p = new Particle(material, mhiUP, (float) (massperparticle * ars.fractionUpper), insertiontime);
                                        particles.add(p);
                                        masstorelease -= p.getParticleMass();
                                        released += p.getParticleMass();
                                        //Lower Manhole
                                        Particle p_ = new Particle(material, mhiDW, (float) (massperparticle * (1 - ars.fractionUpper)), insertiontime);
                                        particles.add(p_);
                                        masstorelease -= p_.getParticleMass();
                                        released += p_.getParticleMass();
                                    }
                                } else {
                                    //Create a single particle, carrying the emitted mass of this interval
                                    long insertiontime = (long) (0.5 * (tl.getTimeContainer().getTimeMilliseconds(i) + tl.getTimeContainer().getTimeMilliseconds(i + 1)));
                                    Particle p = new Particle(material, mhiUP, (float) (masstorelease * ars.fractionUpper), insertiontime);
                                    particles.add(p);
                                    masstorelease -= p.getParticleMass();
                                    released += p.getParticleMass();
                                    //Lower Manhole
                                    p = new Particle(material, mhiDW, (float) (masstorelease * (1 - ars.fractionUpper)), insertiontime);
                                    particles.add(p);
                                    masstorelease -= p.getParticleMass();
                                    released += p.getParticleMass();
                                }
                            }

                        }
//                        System.out.println("Released "+released+" /"+ars.washoffMass+"kg");
//                System.out.println("Left " + massreservoir + "/" + ars.massUpper + " (" + masstorelease + ") on area " + ars.areaName);
                    } else if (inflowtype == RUNOFF_CONTROL.PRECIPITATION) {
                        for (int i = 0; i < precipitation.getTimes().length - 1; i++) {
                            if (massreservoir < 0) {
                                break;
                            }
                            //Intervallduration [s]
                            double duration = (precipitation.getTimes()[i + 1] - precipitation.getTimes()[i]) / 1000.;
                            //m^3
                            double eff_precipitation = precipitation.getPrecipitation()[i];

                            double masswashoffinInterval = massreservoir * washoffConstant * eff_precipitation;
                            if (masswashoffinInterval > massreservoir) {
                                masswashoffinInterval = massreservoir;
                            }
                            masstorelease += masswashoffinInterval;
                            //Number of particles to release in interval:
                            int particles_toRelease = (int) (masstorelease / (2 * massperParticle));
                            //Only release total number of particles in interval
                            if (masstorelease > 0) {
                                if (particles_toRelease > 0) {
                                    float massperparticle = (float) (masswashoffinInterval / particles_toRelease);
                                    double timedistance = duration / (double) particles_toRelease;
                                    for (int j = 0; j < particles_toRelease; j++) {
                                        long insertiontime = (long) (precipitation.getTimes()[i] - precipitation.getTimes()[0] + (j + 0.5) * timedistance * 1000);
                                        Particle p = new Particle(material, mhiUP, (float) (massperparticle * ars.fractionUpper), insertiontime);
                                        particles.add(p);
                                        massreservoir -= p.getParticleMass();
                                        masstorelease -= p.getParticleMass();
                                        p = new Particle(material, mhiDW, (float) (massperparticle * (1 - ars.fractionUpper)), insertiontime);
                                        particles.add(p);
                                        massreservoir -= p.getParticleMass();
                                        masstorelease -= p.getParticleMass();
                                    }
                                } else {
                                    //Create a single particle, carrying the emitted mass of this interval
                                    long insertiontime = (long) (0.5 * (precipitation.getTimes()[i] + precipitation.getTimes()[i]) - precipitation.getTimes()[0]);
                                    Particle p = new Particle(material, mhiUP, (float) ( masstorelease*ars.fractionUpper), insertiontime);
                                    particles.add(p);
                                    massreservoir -= p.getParticleMass();
                                    masstorelease -= p.getParticleMass();
                                    p = new Particle(material, mhiDW, (float) ( masstorelease*(1-ars.fractionUpper)), insertiontime);
                                    particles.add(p);
                                    massreservoir -= p.getParticleMass();
                                    masstorelease -= p.getParticleMass();
                                }
                            }

                        }
                    } else {
                        System.err.println("Inflow type " + inflowtype + " not yet implemented. Only " + RUNOFF_CONTROL.INFLOW_WASHOFF + " and " + RUNOFF_CONTROL.PRECIPITATION + " supported.");
                    }
                }

            }
            System.out.println("Releasemass=" + totalrelesemass + " kg for " + runoffParameterName);
//            System.out.println("precipitation.time [0] = " + precipitation.getTimes()[0] + "   beginn=" + precipitation.getBeginn()+" -> offset:");
//            for (int i = 0; i < precipitation.getPrecipitation().length; i++) {
//                System.out.println("Niederschlag ["+i+"]="+precipitation.getPrecipitation()[i]);
//                
//            }
//            if (particles != null && !particles.isEmpty()) {
//                System.out.println("First particle to be released at " + particles.get(0).getInsertionTime() + " ms after simulation start");
//            }
            return particles;
        }
        System.err.println("Inflow type " + inflowtype + " not yet implemented");

        return null;
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
    public double getIntervalStart(int interval
    ) {
        return 0;
    }

    @Override
    public double getIntervalEnd(int interval
    ) {
        return 0;
    }

    @Override
    public double getIntervalDuration(int interval
    ) {
        return 0;
    }

    @Override
    public double massInInterval(int interval
    ) {
        return 0;
    }

    @Override
    public int particlesInInterval(int interval
    ) {
        return 0;
    }

    @Override
    public double getIntensity(int intervalIndex
    ) {
        return 0;
    }

    @Override
    public void setCapacity(Capacity capacity
    ) {

    }

    @Override
    public void setId(int i
    ) {
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
    public void setActive(boolean active
    ) {
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

    public void setPrecipitation(RainGauge precipitation) {
        this.precipitation = precipitation;
        changed = true;
    }

    public void setInflowtype(RUNOFF_CONTROL inflowtype) {
        this.inflowtype = inflowtype;
        changed = true;
    }

}
