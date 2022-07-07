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
import com.saemann.gulli.core.control.particlecontrol.injection.ParticleInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.PipeInjection;
import com.saemann.gulli.core.io.extran.HE_Database.AreaRunoffSplit;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.RainGauge;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
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

    public static boolean verbose = true;

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
        INFLOW_WASHOFF,
        Anh1_UP_LOW, Anh1_CENTER
    };

    public RUNOFF_CONTROL inflowtype = RUNOFF_CONTROL.PRECIPITATION;

    public Material material;
    public double relativePosition;
    private boolean changed = false;
    private int materialID;

    private RainGauge precipitation;

    private Network network;

    public int numberOfParticles;

    public HashMap<String, Double> effectiveRunoffVolume;

    public HashMap<String, Double> areaPerPipe;
    public HashMap<String, Double> areaPerManhole;
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

    public Collection<AreaRunoffSplit> areaRunoffSplit;
    private int id;
    private boolean active = true;
    private double mass = 0;

    private boolean initilized = false;

    public int numberAreaObjects = 0;
    public double effectiveArea = 0;
    public double effectiveVolume = 0;

    DecimalFormat df3 = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));
    DecimalFormat df5 = new DecimalFormat("0.00000", DecimalFormatSymbols.getInstance(Locale.US));

    public HEAreaInflow1DInformation(String runoffParameter, Material mat, int numberofparticles) {
        this.runoffParameterName = runoffParameter;
        this.numberOfParticles = numberofparticles;
        this.material = mat;
    }

    public void calculateManholesArea() {
        if (effectiveRunoffVolume != null) {
            effectiveRunoffVolume.clear();
        } else {
            effectiveRunoffVolume = new HashMap<>(30);
        }
        if (areaPerPipe != null) {
            areaPerPipe.clear();
        } else {
            areaPerPipe = new HashMap<>(30);
        }
        if (areaPerManhole != null) {
            areaPerManhole.clear();
        } else {
            areaPerManhole = new HashMap<>(30);
        }
        this.mass = 0;
        for (AreaRunoffSplit ars : areaRunoffSplit) {
            double upper = ars.runoffVolume * ars.fractionUpper;
            double lower = ars.runoffVolume * (1. - ars.fractionUpper);
            mass += ars.area * this.massload;
            Double up = effectiveRunoffVolume.get(ars.upperManholeName);
            if (up == null) {
                effectiveRunoffVolume.put(ars.upperManholeName, upper);
            } else {
                effectiveRunoffVolume.put(ars.upperManholeName, upper + up);
            }
            Double low = effectiveRunoffVolume.get(ars.lowerManholeName);
            if (low == null) {
                effectiveRunoffVolume.put(ars.lowerManholeName, lower);
            } else {
                effectiveRunoffVolume.put(ars.lowerManholeName, lower + low);
            }

            Double area = areaPerPipe.get(ars.pipename);
            if (area == null) {
                areaPerPipe.put(ars.pipename, ars.area);
            } else {
                areaPerPipe.put(ars.pipename, area + ars.area);
            }

            Double areaUp = areaPerManhole.get(ars.upperManholeName);
            if (areaUp == null) {
                areaPerManhole.put(ars.upperManholeName, ars.area * ars.fractionUpper);
            } else {
                areaPerManhole.put(ars.upperManholeName, areaUp + ars.area * ars.fractionUpper);
            }

            Double areaLow = areaPerManhole.get(ars.lowerManholeName);
            if (areaLow == null) {
                areaPerManhole.put(ars.lowerManholeName, ars.area * (1 - ars.fractionUpper));
            } else {
                areaPerManhole.put(ars.lowerManholeName, areaLow + ars.area * (1 - ars.fractionUpper));
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
        //Calculate total washed off mass
        double totalwashoffMass = 0;
        boolean considerwashofftype = false;
        if (runoffParameterName == null || runoffParameterName.isEmpty() || runoffParameterName.equals("All")) {
            considerwashofftype = false;
        } else {
            considerwashofftype = true;
        }
        for (AreaRunoffSplit ars : areaRunoffSplit) {
            if (considerwashofftype) {
                if (!ars.washoffParameter.equals(runoffParameterName)) {
                    continue;
                }
            }
            double areamasspotential = ars.area * massload;
            double washoffFraction = (Math.min(1, washoffConstant * ars.runofffraction * ars.totalPrecipitationMM));
            double washoffMass = areamasspotential * washoffFraction;
            totalwashoffMass += washoffMass;
            ars.massUpper = areamasspotential * ars.fractionUpper;
            ars.massLower = areamasspotential * (1 - ars.fractionUpper);
            effectiveArea += ars.area;
            effectiveVolume += ars.runoffVolume;
            numberAreaObjects++;
        }
        mass = totalwashoffMass;
        if (inflowtype == RUNOFF_CONTROL.INFLOW_WASHOFF || inflowtype == RUNOFF_CONTROL.PRECIPITATION) {

            ArrayList<Particle> particles = new ArrayList<>(numberOfParticles);

//            System.out.println("Check runoff type " + considerwashofftype + " for " + runoffParameterName);
            //Calculate fraction of mass per particle
            double massperParticle = totalwashoffMass / (double) numberOfParticles;
            //Create particles for each area by considering the linkes area as a fraction of the whole inflow
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
                    double areafractionUP = 0;
                    double areafractionLOW = 0;
                    try {
                        areafractionUP = ars.runoffVolume * ars.fractionUpper / effectiveRunoffVolume.get(ars.upperManholeName);
                        areafractionLOW = ars.runoffVolume * (1 - ars.fractionUpper) / effectiveRunoffVolume.get(ars.lowerManholeName);
                    } catch (Exception e) {
                        System.err.println("Cannot find area of " + ars.upperManholeName + " or " + ars.lowerManholeName);
                        continue;
                    }
                    double masstorelease = 0;
                    double released = 0;
                    if (inflowtype == RUNOFF_CONTROL.INFLOW_WASHOFF) {
                        //Calculates the particles based on the inflow timeline of the connected manholes (uses the inflow of the upper manhole.
                        //Sends particles to both manholes.
                        TimeLineManhole tlUP = mhUP.getStatusTimeLine();
                        TimeLineManhole tlLOW = mhDW.getStatusTimeLine();
                        for (int i = 0; i < tlUP.getNumberOfTimes() - 1; i++) {
                            if (massreservoir < 0) {
                                break;
                            }
                            // m^3/s
                            float inflowUPCurrent = tlUP.getInflow(i);
                            float inflowUPNext = tlUP.getInflow(i + 1);
                            float inflowLOWCurrent = tlLOW.getInflow(i);
                            float inflowLOWNext = tlLOW.getInflow(i + 1);
                            //Intervallduration [s]
                            double duration = (tlUP.getTimeContainer().getTimeMilliseconds(i + 1) - tlUP.getTimeContainer().getTimeMilliseconds(i)) / 1000.;
                            //m^3
                            double volumeUP = (inflowUPCurrent + inflowUPNext) * 0.5 * duration * areafractionUP;
                            double volumeLOW = (inflowLOWCurrent + inflowLOWNext) * 0.5 * duration * areafractionLOW;

                            double volumeOnArea = (volumeUP + volumeLOW) * 0.5;
                            double eff_precipitation = volumeOnArea * 1000. / (ars.area /**
                                     * ars.fractionUpper
                                     */
                                    ); // m->mm
//                            System.out.println("Inflow "+volumeUP+"m³ / "+volumeLOW+"m³  -> "+eff_precipitation+"mm"+"\t Abweichung Upper/lower: "+(int)(((volumeUP-volumeLOW)*100)/volumeUP)+"%");
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
                                        long insertiontime = (long) (tlUP.getTimeContainer().getTimeMilliseconds(i) + (j + 0.5) * timedistance * 1000);
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
                                    long insertiontime = (long) (0.5 * (tlUP.getTimeContainer().getTimeMilliseconds(i) + tlUP.getTimeContainer().getTimeMilliseconds(i + 1)));
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
                                    Particle p = new Particle(material, mhiUP, (float) (masstorelease * ars.fractionUpper), insertiontime);
                                    particles.add(p);
                                    massreservoir -= p.getParticleMass();
                                    masstorelease -= p.getParticleMass();
                                    p = new Particle(material, mhiDW, (float) (masstorelease * (1 - ars.fractionUpper)), insertiontime);
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
            if (verbose) {
                System.out.println("Releasemass=" + totalrelesemass + " kg for " + runoffParameterName);
            }

            return particles;
        } else if (inflowtype == RUNOFF_CONTROL.Anh1_UP_LOW || inflowtype == RUNOFF_CONTROL.Anh1_CENTER) {
           
            double totalmass = 0;
            for (AreaRunoffSplit ars : areaRunoffSplit) {
                totalmass += ars.area * massload;
            }

            double massperParticle = mass / numberOfParticles;
            if (verbose) {
                System.out.println("Anh1 totalmass:" + totalmass + "kg --> ~" + massperParticle + " kg/Particle");
            }
            ArrayList<Particle> particles = new ArrayList<>(numberOfParticles);
            for (AreaRunoffSplit ars : areaRunoffSplit) {
                if (considerwashofftype) {
                    if (!ars.washoffParameter.equals(runoffParameterName)) {
                        continue;
                    }
                }
                if (verbose) {
                    System.out.println("Area " + ars.areaName + " A=" + ars.area + "m^2");
                }
                Pipe pipe = network.getPipeByName(ars.pipename);
                //erst oben
                Manhole mh = network.getManholeByName(ars.upperManholeName);
                if (mh == null) {
                    System.out.println("Can not find Manhole " + ars.upperManholeName);
                    continue;
                }
                ParticleInjection inj = null;
                if (inflowtype == RUNOFF_CONTROL.Anh1_UP_LOW) {
                    inj = new ManholeInjection(mh);
                } else if (inflowtype == RUNOFF_CONTROL.Anh1_CENTER) {
                    inj = new PipeInjection(pipe, pipe.getLength() * 0.5);
                } else {
                    System.err.println("unknown inflow Type " + inflowtype);
                    inj = new ManholeInjection(mh);
                }
                TimeLineManhole tlUP = mh.getStatusTimeLine();
                TimeContainer times = tlUP.getTimeContainer();
                double pollutantreservoirUP = ars.area * massload * ars.fractionUpper; //kg pollutat mass
                double pollutantreservoirLOW = ars.area * massload * (1 - ars.fractionUpper); //kg pollutat mass
                if (verbose) {

                    System.out.println("Mass to upper manhole: " + pollutantreservoirUP + " kg (" + ars.fractionUpper + " of total area), Lower Manhole:" + pollutantreservoirLOW + " kg");
                }
                // Calculate pollutant washoff at UPPER manhole: 

                double Nup = 0;
                for (int i = 0; i < tlUP.getNumberOfTimes() - 1; i++) {
                    double inflow = (tlUP.getInflow(i) + tlUP.getInflow(i + 1)) * 0.5; // m^3/s
                    double duration = (times.getTimeMilliseconds(i + 1) - times.getTimeMilliseconds(i)) / 1000; // s
                    double inflowVolume = inflow * duration; //m^3
                    double effectivePrecipitation = inflowVolume / areaPerManhole.get(ars.upperManholeName) * 1000; // mm
                    Nup += effectivePrecipitation;
                    double washoffMass = effectivePrecipitation * washoffConstant * pollutantreservoirUP;
                    if (verbose) {
                        int pindex = i / 5;
                        double realprecipitation = -1;
                        try {
                            realprecipitation = precipitation.getPrecipitation()[pindex] / 5.;
                        } catch (Exception e) {
                        }
                        System.out.println(i + ": InflowUP:" + df3.format(inflow) + " m^3/s-> volume=" + df3.format(inflowVolume) + " m³ =>\tNw=" + df3.format(effectivePrecipitation) + " mm / N= " + realprecipitation + " mm ->\tmPab=" + df3.format(washoffMass) + "kg , Pv=" + df5.format(pollutantreservoirUP) + "kg");
                    }
                    if (washoffMass > 0) {
                        int particles_toRelease = (int) (washoffMass / massperParticle);
                        if (particles_toRelease > 0) {
                            float massperparticle = (float) (washoffMass / particles_toRelease);
                            double timedistance = duration / (double) particles_toRelease;
                            for (int j = 0; j < particles_toRelease; j++) {
                                long insertiontime = (long) (times.getTimeMilliseconds(i) - times.getTimeMilliseconds(0) + (j + 0.5) * timedistance * 1000);
                                Particle p = new Particle(material, inj, (float) (massperparticle), insertiontime);
                                particles.add(p);
                            }
                        } else {
                            //Create a single particle, carrying the emitted mass of this interval
                            long insertiontime = (long) (0.5 * (times.getTimeMilliseconds(i) + times.getTimeMilliseconds(i)) - precipitation.getTimes()[0]);
                            Particle p = new Particle(material, inj, (float) (washoffMass), insertiontime);
                            particles.add(p);
                        }
                        pollutantreservoirUP -= washoffMass;
                    }
                }
                // Calculate pollutant washoff at LOWER manhole: 
                //jetzt unteres Manhole 
                mh = network.getManholeByName(ars.lowerManholeName);
                if (mh == null) {
                    System.out.println("Can not find Manhole " + ars.lowerManholeName);
                    continue;
                }
                if (inflowtype == RUNOFF_CONTROL.Anh1_UP_LOW) {
                    inj = new ManholeInjection(mh);
                } else if (inflowtype == RUNOFF_CONTROL.Anh1_CENTER) {
//                    inj = new PipeInjection(pipe, pipe.getLength() * 0.5);
                } else {
                    System.err.println("unknown inflow Type " + inflowtype);
                    inj = new ManholeInjection(mh);
                }
                TimeLineManhole tlLOW = mh.getStatusTimeLine();
                times = tlLOW.getTimeContainer();
                double Nlow = 0;
                for (int i = 0; i < tlLOW.getNumberOfTimes() - 1; i++) {
                    double inflow = (tlLOW.getInflow(i) + tlLOW.getInflow(i + 1)) * 0.5; // m^3/s
                    double duration = (times.getTimeMilliseconds(i + 1) - times.getTimeMilliseconds(i)) / 1000; // s
                    double inflowVolume = inflow * duration; //m^3
                    double effectivePrecipitation = inflowVolume / areaPerManhole.get(ars.lowerManholeName) * 1000; // mm
                    Nlow += effectivePrecipitation;
                    double washoffMass = effectivePrecipitation * washoffConstant * pollutantreservoirLOW;
                    if (verbose) {
                        System.out.println(i + ": InflowLOW:" + df3.format(inflow) + " m^3/s-> volume=" + df3.format(inflowVolume) + " m³ =>\tNw=" + df3.format(effectivePrecipitation) + "mm ->\tmPab=" + df3.format(washoffMass) + "kg , Pv=" + df5.format(pollutantreservoirLOW) + "kg");
                    }

                    if (washoffMass > 0) {
                        int particles_toRelease = (int) (washoffMass / massperParticle);
                        if (particles_toRelease > 0) {
                            float massperparticle = (float) (washoffMass / particles_toRelease);
                            double timedistance = duration / (double) particles_toRelease;
                            for (int j = 0; j < particles_toRelease; j++) {
                                long insertiontime = (long) (times.getTimeMilliseconds(i) - times.getTimeMilliseconds(0) + (j + 0.5) * timedistance * 1000);
                                Particle p = new Particle(material, inj, (float) (massperparticle), insertiontime);
                                particles.add(p);
                            }
                        } else {
                            //Create a single particle, carrying the emitted mass of this interval
                            long insertiontime = (long) (0.5 * (times.getTimeMilliseconds(i) + times.getTimeMilliseconds(i)) - precipitation.getTimes()[0]);
                            Particle p = new Particle(material, inj, (float) (washoffMass), insertiontime);
                            particles.add(p);
                        }
                        pollutantreservoirLOW -= washoffMass;
                    }
                }
                if (verbose) {
                    System.out.println("Effective Precipitation: Upper: " + Nup + " mm, Lower: " + Nlow + " mm");
                }
            }
            return particles;
        }
        if (verbose) {
            System.err.println("Inflow type " + inflowtype + " not yet implemented");
        }

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
        changed = true;
    }

    @Override
    public int getMaterialID() {
        return this.materialID;//material.materialIndex;
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
        if (material != null) {
            this.materialID = material.materialIndex;
        } else {
            this.materialID = -1;
        }
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

    public void setNetwork(Network network) {
        this.network = network;
        this.initilized = false;
        changed = true;
    }

}
