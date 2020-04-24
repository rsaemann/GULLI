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
package control.particlecontrol;

import control.maths.RandomArray;
import control.threads.ThreadController;
import model.particle.HistoryParticle;
import model.particle.Particle;
import model.surface.Surface;
import model.topology.Capacity;
import model.topology.Connection_Manhole;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Connection_Manhole_Surface;
import model.topology.Manhole;
import model.topology.Pipe;
import model.topology.StorageVolume;

/**
 * A Controller, that threads the movement of a set of particles during a
 * timestep.
 *
 * @author saemann
 */
public class ParticlePipeComputing {

    private static int autoIncrement = 0;

    protected int id = autoIncrement++;
    public static int maxloopsPerParticle = 1000;

    public enum COMPUTING {

        RANDOMWALK
    };

    public enum RANDOM_DISTRIBUTION {

        NORMAL_GAUSS, UNIFORM, TRIANGLE
    }

    public enum TURBULENT_DIFFUSION {

        FIX_DIFFUSION_COEFFICIENT, FIX_DIFFUSIVE_COEFFICIENT;
    }

//    private final RANDOM_DISTRIBUTION distributionForm;
//    private UniformDistribution uniformDistribution;
//    private final VelocityDistribution velocityFunction;
//    private long seed;
    public COMPUTING computing = COMPUTING.RANDOMWALK;

//    public static boolean useDynamicVelocity = true;
    public static boolean useDynamicDispersion = false;

    /**
     * Determines the computational effort for the diffusive
     */
    public TURBULENT_DIFFUSION turbulentCalculation = TURBULENT_DIFFUSION.FIX_DIFFUSION_COEFFICIENT;

    /**
     * Replace Dispersion by varying advective velocity [0,1.25]*meanVelocity.
     */
    public static boolean useStreamlineVelocity = false;
    /**
     * [m^2/s]
     */
    private static double diffusionturbulentCoefficient = 2;//m^2/s

    /**
     * Diffusivity [m]
     */
    private static double diffusivityTurbulentCoefficient = 2;//m

//    /**
//     * [m^2/s] up to 30% fill rate.
//     */
////    public double dispersionCoefficient_low = 0.015;//m^2/s
    protected static float dt;

    private static double diffusionDistance = getDispersionDistance(dt, diffusionturbulentCoefficient);

//    protected double dispersionDistance_low = getDispersionDistance(dt, dispersionCoefficient_low);
    /**
     * [m/s] minimum Velocity even in perfect horizontal pipes
     */
//    public double minimumVelocity = 0.009;
    private RandomArray rand;

//    public RandomDistribution randDist;
//    private ParticleThread thread;
    public static int[] passedPipesCounter;

    /**
     * If set to false, particles will not leave the pipe system towards the
     * surface domain.
     */
    public static boolean spillOutToSurface = false;

    private Surface surface;

    /**
     * If true PArticles may deposite at low velocities and activate on high
     * velocities.
     */
    public boolean useDeposition = false;

    /**
     * Only measure a particle in the capacity at the end of a timestep.
     * Otherwise measure in every passed capacity.
     */
    public boolean measureOnlyInDestination = true;

    public int status = -1;

    public ParticlePipeComputing() {
    }

    public void setSurface(Surface surf, boolean enableSpill) {
        this.surface = surf;
        ParticlePipeComputing.spillOutToSurface = enableSpill;
    }

    public void setDeltaTime(double deltaTime) {
        ParticlePipeComputing.dt = (float) deltaTime;
//        System.out.println("deltatime set new to " + deltaTime);
        ParticlePipeComputing.diffusionDistance = getDispersionDistance(deltaTime, diffusionturbulentCoefficient);
//        this.dispersionDistance_low = getDispersionDistance(deltaTime, dispersionCoefficient_low);
    }

    protected static double getDispersionDistance(double dt, double dispersionCoefficient) {
//        System.out.println("Max dispersion-distance: " + Math.sqrt(dt * dispersionCoefficient) + " m,\tdt: " + dt + " s,\t D: " + dispersionCoefficient + " m²/s");
        return Math.sqrt(2. * dt * dispersionCoefficient);
    }

    /**
     * m²/s
     *
     * @return
     */
    public static double getDispersionCoefficient() {
        return diffusionturbulentCoefficient;
    }

    public double getDispersionDistance() {
        return diffusionDistance;
    }

    public static void setDispersionCoefficient(double dispersionCoefficient) {
        ParticlePipeComputing.diffusionturbulentCoefficient = dispersionCoefficient;
        ParticlePipeComputing.diffusionDistance = getDispersionDistance(dt, dispersionCoefficient);
    }

    private double getTraveldistance(Particle p) {
        double ds = 0;
        Capacity c = p.getSurrounding_actual();
        if (c.getClass().equals(Pipe.class)) {
            p.setVelocity1d(((Pipe) c).averageVelocity_actual());
            ds = p.getVelocity1d() * dt;
        }

        double gauss = rand.nextGaussian();
        double dk = diffusionDistance * gauss;
        ds += dk;

        return ds;
    }

    public void moveParticle(Particle p) {
//        moveParticle4_transfersensitive(p);
        moveParticle5_celltransmission(p);
    }

    /**
     * @deprecated @param p
     */
    public void moveParticle1(Particle p) {

        double position1d = p.getPosition1d_actual();

        double ds = getTraveldistance(p);

        Capacity capa = p.getSurrounding_actual();

//        p.setTravelledPathLength(p.getTravelledPathLength() + Math.abs(ds));
        p.addMovingLength(ds);
        position1d += ds;

        if (p.getSurrounding_actual() == null) {
            return;
        }

        for (int i = 0; i < 50; i++) {
            if (capa.getClass().equals(Pipe.class)) {
                //   System.out.println(" is in Pipe");
                Pipe pipe = (Pipe) capa;
//                needCalculation = true;

                if (position1d > pipe.getLength()) {
                    /**
                     * Particle has reached the end of the pipe. Particle need a
                     * recalculation In which pipe and what velocity to asign to
                     * it.
                     */
                    capa = pipe.getEndConnection().getManhole();
                    position1d = position1d - pipe.getLength();
//                    p.setPosition1d_actual(position1d);
                } else if (position1d < 0) {
                    capa = pipe.getStartConnection().getManhole();
//                    position1d = -(position1d);

                    //Always set a positive "still to walk" position, when the 
                    //  particle reached a storageVolume
//                System.out.println("set reached Manhole backwards "+nextC);
//                    p.setSurrounding_actual(nextC);
//                    p.setPosition1d_actual(position1d);
                } else {
                    //Normal transportation in Pipe
                    p.setSurrounding_actual(capa);
                    p.setPosition1d_actual(position1d);
                    break;
                }
            }

            /*
             If the particle is in a storage volume. Try to find the next Pipe to enter
             */
            if (capa.getClass().equals(Manhole.class)) {
                Manhole actualC = (Manhole) capa;
                Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) p.getMaterial().getFlowCalculator().whichConnection(actualC, rand, true);
                if (con == null) {
                    if (actualC.isSetAsOutlet()) {
                        p.setLeftSimulation();

                    } else {
//                        needCalculation = true;
                        System.out.println("no outgoing from " + actualC);
                    }
                    //Particle stays in here 

                    p.setVelocity1d(0);
                    p.setPosition1d_actual(0);
                    position1d = 0;
                    break;
                } else {
                    if (position1d > 0) {
                        Pipe next = con.getPipe();
//                        p.setSurrounding_actual(next);
                        capa = next;
//                        p.setVelocity1d(next.averageVelocity_actual());
                        if (con.isFlowInletToPipe()) {
                            //Pipe is orientated in Flow direction-> regular positive x                   
//                            p.setPosition1d_actual(position1d);
//                        System.out.println("Particle flows to a pipe inlet.");
                        } else {
//                        continue;
                            System.err.println("Particle flows to a pipe outlet.");
                            position1d = next.getLength() + position1d;
//                            p.setPosition1d_actual(position1d);
                        }
                    } else {
                        // position1d <0 
                        //Rückströmendes Partikel
                        Pipe next = con.getPipe();
                        capa = next;
//                        p.setSurrounding_actual(next);
//                        p.setVelocity1d(next.averageVelocity_actual());
                        if (con.isStartOfPipe()) {
                            //Pipe is orientated in Flow direction-> regular positive x                   
                            position1d = next.getLength() + position1d;
                            System.out.println("Particle flows reverse to a pipe inlet.");
                        } else {
//                        continue;
//                            System.err.println("Particle flows to a pipe outlet.");
                            position1d = next.getLength() - Math.abs(position1d);
                        }
                    }
                }
            }
//            if(position1d<0)System.out.println(this.getClass()+" ERROR pos1d<0\t pos1d="+position1d+" \t "+p.getSurrounding_actual());
        }
        p.setPosition1d_actual(position1d);
        p.setSurrounding_actual(capa);

        if (p.getSurrounding_actual().getClass().equals(Pipe.class)) {
            //   System.out.println(" is in Pipe");
            Pipe pipe = (Pipe) p.getSurrounding_actual();
            if (position1d > pipe.getLength()) {
                System.out.println("p: ParticleComputing loop (50) is not sufficient. Particle " + p + " is in " + p.getSurrounding_actual() + " with a Position of " + p.getPosition1d_actual());
                /**
                 * Particle has reached the end of the pipe. Particle need a
                 * recalculation In which pipe and what velocity to asign to it.
                 */
                StorageVolume nextC = pipe.getEndConnection().getManhole();
                position1d = position1d - pipe.getLength();
                p.setSurrounding_actual(nextC);
                p.setPosition1d_actual(0);
            } else if (position1d < 0) {
                StorageVolume nextC = pipe.getStartConnection().getManhole();
                p.setSurrounding_actual(nextC);
                System.out.println("Rückgeströmtes Partikelchen wird gestoppt.");
                p.setPosition1d_actual(0);
            } else {
                p.setVelocity1d(pipe.averageVelocity_actual());
            }

        } else {
            if (p.getPosition1d_actual() > 0) {
                System.out.println("mh:Particle " + p + " is in " + p.getSurrounding_actual() + " with a Position of " + p.getPosition1d_actual());
            }
        }
//        if (!foundNaN && Double.isNaN(position1d)) {
//            System.out.println("Position is NaN , particle " + p.getAutoID() + " v=" + p.getVelocity1d());
//            foundNaN = true;
//        }
    }

    /**
     * @deprecated @param p
     */
    private void moveParticle2(Particle p) {
        double position1d = p.getPosition1d_actual();
        Capacity c = p.getSurrounding_actual();
        if (c == null || c.isSetAsOutlet()) {
            p.setLeftSimulation();
            p.setPosition1d_actual(0);
            p.setVelocity1d(0);
            return;
        }
//        needCalculation = true;
        /**
         * calculate spatial distance in this timestep.
         */
        double adv = 0;
        if (c.getClass().equals(Pipe.class)) {
            adv = Math.abs(((Pipe) c).getVelocity()) * (dt/*-timeSpend*/);
            p.setVelocity1d(((Pipe) c).getVelocity());
        }
        double diff = calcDistanceTurbulentDiffusion(p.getVelocity1d()) * rand.nextGaussian();
//        boolean advectiveDominant = Math.abs(adv / diff) > 1;
        /**
         * if ds is positive : advective dominant else diffusiv has higher
         * negative influence
         */
        double ds = (adv + diff);
//        ds=getTraveldistance(p);
//        System.out.println("Particle " + p + "  adv.= " + adv + "\t diff.= " + diff + "\t =ds: " + ds+"\tpos1d:"+p.getPosition1d_actual());
        /**
         * First step directing particle to next manhole
         */
        if (c.getClass().equals(Pipe.class)) {
            Pipe pipe = (Pipe) c;
            if (pipe.getVelocity() > 0) {
                double neuePosition = position1d + ds;
//                    System.out.println("  pipe.velocity>0  position1d: "+position1d+"  ds="+ds);
                if (neuePosition < 0) {
//                        System.out.println(  " C <0 = "+position1d+"+"+ds);
                    // Particle flowed to the start of the pipe and reached the manhole
                    c = pipe.getStartConnection().getManhole();
                    ds += position1d;
                    position1d = 0;
//                        System.out.println("\t-> ds:"+ds+"\t pos1d:"+position1d+"   in "+c);
                } else if (neuePosition <= pipe.getLength()) {
                    // Nothing to do, if particle stays inside this Pipe
                    position1d = neuePosition;
//                    ds = 0;
                    p.setPosition1d_actual(position1d);
                    p.setSurrounding_actual(c);
//                    System.out.println("  after preparing1 now in " + c + " ds=" + ds + "   pos1d=" + position1d);
                    return;
                } else {
                    //Über dieses Rohr hinaus geschossen.
                    //if (position1d + ds >= pipe.getLength()) {
                    // particle reached end of pipe
//                        System.out.println(  "  >L = pos1d:"+position1d+"+ ds:"+ds);
                    ds = neuePosition - pipe.getLength();
                    position1d = 0;
                    c = pipe.getEndConnection().getManhole();
//                        System.out.println("\t-> ds:"+ds+"\t pos1d:"+position1d+"   in "+c);
                }
//                else {
//                    System.out.println(this.getClass() + ":: this should never happenn Position: " + position1d + " ds=" + ds + "   =" + (position1d + ds));
//                }
            } else {
//                System.out.println("in Pipe"+pipe.getName()+" with negative velocity v="+pipe.getVelocity());
                /**
                 * Negative velocity*
                 */
                if (position1d - ds < 0) {
                    // Particle flowed to the start of the pipe and reached the manhole
                    c = pipe.getStartConnection().getManhole();
                    ds += position1d;
                    position1d = 0;
                } else if (position1d - ds < pipe.getLength()) {
                    // Nothing to do, if particle stays inside this Pipe
                    position1d -= ds;
//                    ds = 0;
                    p.setPosition1d_actual(position1d);
                    p.setSurrounding_actual(c);
//                     System.out.println("  after preparing2 now in " + c + " ds=" + ds + "   pos1d=" + position1d);
                    return;
                } else if (position1d - ds >= pipe.getLength()) {
                    // particle reached end of pipe
                    ds = ds - (pipe.getLength() - position1d);
                    position1d = 0;
                    c = pipe.getEndConnection().getManhole();
                } else {
                    System.out.println("  what a strange case 2 ds: " + ds + " pos1d: " + position1d);
                }
            }
        }
        /**
         * Start loop with particles, that start in a Manhole
         */
//        System.out.println("  prepared in "+c+" pos1d="+position1d+" ds:"+ds);
        int loops = 0;
        if (c.getClass().equals(Manhole.class)) {
            for (int i = 0; i < 30; i++) {
                loops++;
                /**
                 * 1. Case: capacity is in a Manhole
                 */
                try {
                    Manhole mh = (Manhole) c;
                    if (c.isSetAsOutlet()) {
                        p.setLeftSimulation();
                        p.setPosition1d_actual(0);
                        p.setSurrounding_actual(c);
                        return;
                    }
                    /**
                     * search only for outflowing connections
                     */
                    Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) p.getMaterial().getFlowCalculator().whichConnection(mh, rand, ds > 0);
                    if (con == null) {
//                        System.out.println(this.getClass() + "::  Flowcalculator returned connection " + con + " for (" + mh + ",ds:" + ds + ")");
//                        p.status = false;
                        p.setSurrounding_actual(mh);
                        p.setPosition1d_actual(0);
                        return;
                    }
//                    System.out.println("ds= "+ds+"  is start: "+con.isStartOfPipe());
                    Pipe pipe = con.getPipe();
                    c = pipe;
                    if (con.isStartOfPipe()) {
                        position1d = 0;
//                    System.out.println("   downstream Pipe. pos1d="+position1d+" ds="+ds+" in "+c);
                        if (pipe.getVelocity() >= 0) {
                            //Downstream Pipe
                            if (ds < 0) {
                                System.out.println("  # ds <0   = " + ds + "  v=" + pipe.getVelocity());
                            }
                            if (ds > pipe.getLength()) {
                                ds -= pipe.getLength();
                                c = pipe.getEndConnection().getManhole();
                                continue;
                            } else {
                                // Particle will end this timestep in this pipe.
                                if (ds < 0) {
                                    System.out.println("  downstream in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                }
                                position1d = ds;
                                ds = 0;

                                break;
                            }
                        } else {
                            // reverse flown Pipe
//                            System.out.println("vorwärts in rückwärts fließendes Rohr.");
                            if (ds > 0) {
                                System.out.println(" ## ds >0 in reverse flown forward pipe. ds=" + ds);
                            }
                            if (-ds > pipe.getLength()) {
                                ds += pipe.getLength();
                                c = pipe.getStartConnection().getManhole();
                                continue;
                            } else {
                                if (pipe.getLength() - ds < 0) {
                                    System.out.println(" backwardin start in reverse flown pipe ds=" + ds);
                                }
                                position1d = pipe.getLength() - ds;
                                ds = 0;
//                            System.out.println("  reverse1 in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                break;
                            }
                        }
                    } else {
                        // con is End of Pipe
//                        System.out.println("con is end of pipe adv:"+adv+"\t diff:"+diff+"\t ds:"+ds);
                        if (pipe.getVelocity() >= 0) {
                            if (ds > 0) {
                                System.out.println(" ##falschherum ds=" + ds + " in End of positive flown Pipe " + pipe);
                            }
                            if (pipe.getLength() <= -ds) {
//                                System.out.println("pipe.length: "+pipe.getLength()+"< -ds:"+-ds);
                                c = pipe.getStartConnection().getManhole();
                                ds += pipe.getLength();
//                                System.out.println("    neues ds:"+ds);
                                continue;
                            } else {
                                position1d = pipe.getLength() + ds;
                                if (position1d < 0) {
                                    System.out.println(" ### position<0: ds=" + ds);
                                }
                                p.setPosition1d_actual(position1d);
                                p.setSurrounding_actual(c);
//                                System.out.println("  reverse2  now in " + c + " ds=" + ds + "   pos1d=" + position1d );
                                return;
                            }
                        } else {
                            // reverse flown Pipe
                            if (ds < 0) {
                                System.out.println(" ##falschherum ds=" + ds + " in End of negative flown Pipe " + pipe);
                            }
//                            System.out.println("rückwärts in rückwärts fließendes Rohr.");
//                            if (ds < 0) {
//                                System.out.println("-ds <0 . ds=" + ds);
//                            }
                            if (ds > pipe.getLength()) {
                                ds -= pipe.getLength();
                                c = pipe.getStartConnection().getManhole();
                                continue;
                            } else {
                                if (pipe.getLength() - ds < 0) {
                                    System.out.println("  reverse in reverse Pipe in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                }
                                position1d = pipe.getLength() - ds;
                                ds = 0;

                                break;
                            }
                        }
//                        position1d = con.getPipe().getLength();
//                        System.out.println("   upstream Pipe. pos1d=" + position1d + "   ds=" + ds + " in " + c);
                    }
                } catch (Exception e) {
                    System.out.println(this.getClass() + ":: Unsupported Capacity " + c.getClass() + " to move particle here. loop " + loops + " startcapacity:" + p.getSurrounding_actual());
                    e.printStackTrace();
                    break;
                }
            }
        }

//        if (c.getClass().equals(Pipe.class)) {
//            Pipe pipe = (Pipe) c;
//            if (position1d > pipe.getLength()) {
//                System.out.println("Number of loops is not enogh position1d: " + position1d + " / pipelength=" + pipe.getLength());
//                position1d = pipe.getLength();
//            } else if (position1d < 0) {
//                System.out.println("Number of loops is not enogh position1d: " + position1d + " < 0 ");
//                position1d = 0;
//            } else {
////                System.out.println("  after loop " + loops + " now in " + c + " ds=" + ds + "   pos1d=" + position1d);
//            }
//        }
        if (ds != 0) {
            System.out.println("after " + loops + " loops not reached end of spatial traveldistance. ds=" + ds + " \t pos1d=" + position1d + "  " + p + " in " + c);
        }

        p.setSurrounding_actual(c);

        p.setPosition1d_actual(position1d);
    }

    /**
     * @deprecated @param p
     */
    private void moveParticle3(Particle p) {

        double position1d = p.getPosition1d_actual();
        Capacity c = p.getSurrounding_actual();
        if (c == null || c.isSetAsOutlet()) {
            p.setLeftSimulation();
            p.setPosition1d_actual(0);
            p.setVelocity1d(0);
            return;
        }
//        needCalculation = true;
        /**
         * calculate spatial distance in this timestep.
         */
        double adv = 0;
        double diff = 0;
        if (c.getClass().equals(Pipe.class)) {
            p.setVelocity1d(((Pipe) c).getVelocity());
            if (useStreamlineVelocity) {
                double f = rand.nextGaussian();
                adv = Math.abs(((Pipe) c).getVelocity() * dt * f * 3);
//                System.out.println("f=" + f + "\t u=" + adv + "m/s");
                if (adv < 0.00001) {
                    return;
                }
            } else {
                adv = Math.abs(((Pipe) c).getVelocity()) * (dt/*-timeSpend*/);
                diff = calcDistanceTurbulentDiffusion(p.getVelocity1d()) * rand.nextGaussian();
            }

        } else {
            diff = calcDistanceTurbulentDiffusion(p.getVelocity1d()) * rand.nextGaussian();
        }
//        boolean advectiveDominant = Math.abs(adv / diff) > 1;
        /**
         * if ds is positive : advective dominant else diffusiv has higher
         * negative influence
         */
        double ds = (adv + diff);
        p.addMovingLength(ds);
//        p.addTravelledPathLength(adv);
//        p.ds = ds;
//        ds=getTraveldistance(p);
//        System.out.println("Particle " + p + "  adv.= " + adv + "\t diff.= " + diff + "\t =ds: " + ds+"\tpos1d:"+p.getPosition1d_actual());
        /**
         * First step directing particle to next manhole
         */
        if (c.getClass().equals(Pipe.class)) {
            Pipe pipe = (Pipe) c;
            if (pipe.getVelocity() > 0) {
                double neuePosition = position1d + ds;
//                    System.out.println("  pipe.velocity>0  position1d: "+position1d+"  ds="+ds);
                if (neuePosition < 0) {
//                        System.out.println(  " C <0 = "+position1d+"+"+ds);
                    // Particle flowed to the start of the pipe and reached the manhole
                    c = pipe.getStartConnection().getManhole();
                    ds += position1d;
                    position1d = 0;
//                        System.out.println("\t-> ds:"+ds+"\t pos1d:"+position1d+"   in "+c);
                } else if (neuePosition <= pipe.getLength()) {
                    // Nothing to do, if particle stays inside this Pipe
                    position1d = neuePosition;
                    p.setPosition1d_actual(position1d);
                    p.setSurrounding_actual(c);
                    pipe.getMeasurementTimeLine().addParticle(p);
//                    pipe.getMeasurementTimeLine().addParticle();
//                    System.out.println("  after preparing1 now in " + c + " ds=" + ds + "   pos1d=" + position1d);
                    return;
                } else {
                    //Über dieses Rohr hinaus geschossen.
                    //if (position1d + ds >= pipe.getLength()) {
                    // particle reached end of pipe
//                        System.out.println(  "  >L = pos1d:"+position1d+"+ ds:"+ds);
                    ds = neuePosition - pipe.getLength();
                    position1d = 0;
                    c = pipe.getEndConnection().getManhole();
//                        System.out.println("\t-> ds:"+ds+"\t pos1d:"+position1d+"   in "+c);
                }
//                else {
//                    System.out.println(this.getClass() + ":: this should never happenn Position: " + position1d + " ds=" + ds + "   =" + (position1d + ds));
//                }
            } else {
//                System.out.println("in Pipe"+pipe.getName()+" with negative velocity v="+pipe.getVelocity());
                /**
                 * Negative velocity*
                 */
                if (position1d - ds < 0) {
                    // Particle flowed to the start of the pipe and reached the manhole
                    c = pipe.getStartConnection().getManhole();
//                     if(pipe.getAutoID()==1514){
//                        System.out.print("particle set start manhole \t"+c+"\tds="+ds+"\t p.pos="+position1d);
//                    }
                    ds -= position1d;
                    position1d = 0;
//                    if(pipe.getAutoID()==1514){
//                        System.out.println("\t=>ds="+ds);
//                    }
                } else if (position1d - ds < pipe.getLength()) {
                    // Nothing to do, if particle stays inside this Pipe
                    position1d -= ds;
//                    ds = 0;
                    p.setPosition1d_actual(position1d);
                    p.setSurrounding_actual(c);
                    pipe.getMeasurementTimeLine().addParticle(p);
//                    if(pipe.getAutoID()==1514){
//                        System.out.println("particle set to position \t"+position1d);
//                    }
//                     System.out.println("  after preparing2 now in " + c + " ds=" + ds + "   pos1d=" + position1d);
//                    pipe.getMeasurementTimeLine().addParticle();
                    return;
                } else if (position1d - ds >= pipe.getLength()) {
                    // particle reached end of pipe
                    c = pipe.getEndConnection().getManhole();
//                    if(pipe.getAutoID()==1514){
//                        System.out.println("particle set end  manhole \t"+c+"\tds="+ds+"\t p.pos="+position1d);
//                    }
                    ds = ds - (pipe.getLength() - position1d);
                    position1d = 0;

                } else {
                    System.out.println("  what a strange case 2 ds: " + ds + " pos1d: " + position1d);
                }
            }
        }
        /**
         * Start loop with particles, that start in a Manhole
         */
//        System.out.println("  prepared in "+c+" pos1d="+position1d+" ds:"+ds);

        int loops = 0;
        if (c.getClass().equals(Manhole.class)) {

//            if (c.getAutoID() == 638 && p.getAutoID() < 10) {
//                System.out.println("particle " + p.getAutoID() + " in " + c + "\tv=" + p.getVelocity1d() + "\tds=" + ds);
//            }
            position1d = 0;
//            if(p.getAutoID()==3){
//                System.out.println("position= "+c+"\t pos="+p.getPosition1d_actual()+"\tp.ds="+p.ds);
//            }
            if (ds >= 0) {
                for (int i = 0; i < 30; i++) {
//                    passedPipes++;
                    loops++;
                    /**
                     * 1. Case: capacity is in a Manhole
                     */
                    try {
                        Manhole mh = (Manhole) c;
                        if (c.isSetAsOutlet()) {
                            p.setLeftSimulation();
//                            p.setPosition1d_actual(0);
                            p.setSurrounding_actual(c);
                            return;
                        }
                        /**
                         * search only for outflowing connections
                         */
                        Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) p.getMaterial().getFlowCalculator().whichConnection(mh, rand, true);
//                        if (mh.getAutoID() == 638 && p.getAutoID() < 10) {
//                            System.out.println("particle " + p.getAutoID() + " in " + mh);
//                            if (con != null) {
//                                System.out.println("  Connection : " + con.getPipe() + "\t ds=" + ds);
//                            } else {
//                                System.out.println("  Connection : " + con + "\t ds=" + ds);
//                            }
//                        }
                        if (con == null) {
//                            System.out.println("+Found 0/"+mh.getConnections().length+" Connections in "+mh+" lvl="+mh.getWaterlevel()+" ds="+ds);
//                            for (Connection_Manhole_Pipe conn : mh.getConnections()) {
//                                System.out.println("    "+conn.water_level_in_connection+"    soleheight:"+conn.getHeight()+"/ waterheight:"+mh.getWaterHeight());
//                            }
//                            System.out.println("   ");
                            c = mh;
                            p.setSurrounding_actual(mh);
                            p.setPosition1d_actual(0);

//                            if(mh.getAutoID()==638){
//                                System.out.println("p.capacity="+p.getSurrounding_actual()+"\t pos="+p.getPosition1d_actual()+"\treturn");
//                            }
                            return;
                        }
//                        if (mh.getAutoID() == 638 && p.getAutoID() < 10) {
//                            System.out.println("weitergehts");
//                        }
                        Pipe pipe = con.getPipe();
//                        pipe.getMeasurementTimeLine().addParticle();

                        c = pipe;
                        if (con.isStartOfPipe()) {
                            if (pipe.getVelocity() >= 0) {
                                //Downstream Pipe
                                if (ds < 0) {
                                    System.out.println("  # ds <0   = " + ds + "  v=" + pipe.getVelocity());
                                }
                                if (ds > pipe.getLength()) {
                                    ds -= pipe.getLength();
                                    c = pipe.getEndConnection().getManhole();
                                    continue;
                                } else {
                                    // Particle will end this timestep in this pipe.
                                    if (ds < 0) {
                                        System.out.println("  downstream in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                    }
                                    position1d = ds;
                                    ds = 0;
                                    if (position1d > pipe.getLength()) {
                                        System.out.println(" +toofar1 ");
                                    }
                                    break;
                                }
                            } else {
                                System.out.println("##+ Fehler v=" + pipe.getVelocity());
                            }
                        } else {
                            // con is End of Pipe
//                        System.out.println("con is end of pipe adv:"+adv+"\t diff:"+diff+"\t ds:"+ds);
                            if (pipe.getVelocity() < 0) {
                                // reverse flown Pipe
                                if (ds < 0) {
                                    System.out.println(" ##falschherum ds=" + ds + " in End of negative flown Pipe " + pipe);
                                }
//                            System.out.println("rückwärts in rückwärts fließendes Rohr.");
//                            if (ds < 0) {
//                                System.out.println("-ds <0 . ds=" + ds);
//                            }
                                if (ds > pipe.getLength()) {
                                    ds -= pipe.getLength();
                                    c = pipe.getStartConnection().getManhole();
                                    continue;
                                } else {
//                                    if (pipe.getLength() - ds < 0) {
//                                        System.out.println("  reverse in reverse Pipe in " + c + " ds=" + ds + "   pos1d=" + position1d);
//                                    }
                                    position1d = pipe.getLength() - ds;
                                    ds = 0;
                                    if (position1d > pipe.getLength()) {
                                        System.out.println(" +toofar2 ");
                                    }
                                    break;
                                }
                            } else {
                                System.out.println(" ##++ Fehler in Pipe v::" + pipe.getVelocity());
                            }
//                        position1d = con.getPipe().getLength();
//                        System.out.println("   upstream Pipe. pos1d=" + position1d + "   ds=" + ds + " in " + c);
                        }
                    } catch (Exception e) {
                        System.out.println(this.getClass() + ":: Unsupported Capacity " + c.getClass() + " to move particle here. loop " + loops + " startcapacity:" + p.getSurrounding_actual());
                        e.printStackTrace();
                        break;
                    }
                }
            } else {
                //###################################################################
                // ds<0
                //Negative directed ds because of great negative Diffusion influence
                //-> backward (against velocitiy) travelling
                for (int i = 0; i < 30; i++) {
//                    passedPipes++;
                    loops++;
                    /**
                     * 1. Case: capacity is in a Manhole
                     */

                    Manhole mh = (Manhole) c;
                    if (c.isSetAsOutlet()) {
                        p.setLeftSimulation();
                        p.setPosition1d_actual(0);
                        p.setSurrounding_actual(c);
                        return;
                    }
                    Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) p.getMaterial().getFlowCalculator().whichConnection(mh, rand, false);
//                    if (c.getAutoID() == 638 && p.getAutoID() < 10) {
//                        System.out.println("ds<=0: particle " + p.getAutoID() + " in " + c + "\tv=" + p.getVelocity1d() + "\tds=" + ds + " con=" + con + "\tstart?" + con.isStartOfPipe() + "\t pipe.v=" + con.getPipe().getVelocity());
//                    }
                    if (con == null || con.getPipe() == null) {
                        /*Aktuell kein Ausgang zu finden. Bleibe hier, bis sich 
                         später die Wasserstände ändern*/
//                        System.out.println("-Found 0/"+mh.getConnections().length+" Connections in "+mh+" lvl="+mh.getWaterlevel()+" ds="+ds);
                        p.setPosition1d_actual(0);
                        p.setSurrounding_actual(c);
                        return;
                    }
                    Pipe pipe = con.getPipe();
//                    pipe.getMeasurementTimeLine().addParticle();
//                    pipe.getMeasurementTimeLine().addParticle();
                    c = pipe;
                    if (con.isStartOfPipe()) {
                        if (pipe.getVelocity() <= 0) {
                            // reverse flown Pipe
//                            System.out.println("vorwärts in rückwärts fließendes Rohr.");
                            if (ds > 0) {
                                System.out.println(" ## ds >0 in reverse flown forward pipe. ds=" + ds);
                            }
                            if (-ds > pipe.getLength()) {
                                ds += pipe.getLength();
                                c = pipe.getEndConnection().getManhole();
                                continue;
                            } else {
//                                if (pipe.getLength() + ds < 0) {
//                                    System.out.println(" backwardin start in reverse flown pipe ds=" + ds);
//                                }
                                position1d = -ds;//pipe.getLength() + ds;
//                                System.out.println("strangething happened in "+c);
                                ds = 0;
                                if (position1d > pipe.getLength()) {
                                    System.out.println(" -toofar1 ");
                                }
//                                System.out.println("  reverse1 in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                break;
                            }

                        } else {
                            System.out.println("###Fehler v=" + pipe.getVelocity() + " ds=" + ds + " is flowincoming?" + con.isFlowInletToPipe());
                        }
                    } else {
                        //Connection is end of pipe
                        if (pipe.getVelocity() >= 0) {
                            if (ds > 0) {
                                System.out.println(" ##falschherum ds=" + ds + " in End of positive flown Pipe " + pipe);
                            }
                            if (pipe.getLength() <= -ds) {
//                                System.out.println("pipe.length: "+pipe.getLength()+"< -ds:"+-ds);
                                c = pipe.getStartConnection().getManhole();
                                ds += pipe.getLength();
//                                System.out.println("    neues ds:"+ds);
                                continue;
                            } else {
                                position1d = pipe.getLength() + ds;
                                if (position1d < 0) {
                                    System.out.println(" ### position<0: ds=" + ds);
                                }
                                if (position1d > pipe.getLength()) {
                                    System.out.println(" ### position>Pipe.length: ds=" + ds);
                                }
                                p.setPosition1d_actual(position1d);
                                p.setSurrounding_actual(c);
                                ds = 0;
//                                System.out.println("  reverse2  now in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                break;
                            }
                        } else {
                            System.out.println("##- Fehler v=" + pipe.getVelocity());
                        }
                    }
                }
            }

        }

//        if (c.getClass().equals(Pipe.class)) {
//            Pipe pipe = (Pipe) c;
//            if (position1d > pipe.getLength()) {
//                System.out.println("Number of loops is not enogh position1d: " + position1d + " / pipelength=" + pipe.getLength());
//                position1d = pipe.getLength();
//            } else if (position1d < 0) {
//                System.out.println("Number of loops is not enogh position1d: " + position1d + " < 0 ");
//                position1d = 0;
//            } else {
////                System.out.println("  after loop " + loops + " now in " + c + " ds=" + ds + "   pos1d=" + position1d);
//            }
//        }
        if (ds != 0) {
            System.out.println("after " + loops + " loops not reached end of spatial traveldistance. ds=" + ds + " \t pos1d=" + position1d + "  " + p + " in " + c);
        }

        p.setSurrounding_actual(c);

        p.setPosition1d_actual(position1d);
        if (c.getClass().equals(Pipe.class)) {
//            System.out.println("letzte is Pipe loops="+loops);
            ((Pipe) c).getMeasurementTimeLine().addParticle(p);
        }
    }

    /**
     * Version uses advective speed in every visited pipe
     *
     * @param p
     */
    private void moveParticle4_transfersensitive(Particle p) {
        double position1d = p.getPosition1d_actual();
        Capacity c = p.getSurrounding_actual();
//        status = 2;
        if (Double.isNaN(position1d)) {
            position1d = 0;
        }
//        status = 3;
        if (c == null || c.isSetAsOutlet()) {
//            p.setInactive();
            p.setLeftSimulation();
            p.setPosition1d_actual(0);
            p.setVelocity1d(0);
            return;
        }
//        status = 4;
        //Skip dry condition 
        if (c.getWaterlevel() < FlowCalculatorMixed.dryWaterlevel) {
            return;
        }
//        status=104;
//        int oldCapacityID = (int) c.getAutoID();

        if (useDeposition) {
//Test for deposition
            if (p.isDeposited()) {
                if (p.getMaterial().getFlowCalculator().particleIsEroding(p, c, rand)) {
                    p.setDeposited(false);
                } else {
                    //Stays immobile
                    //Do nothing
                    return;
                }
            } else {
                if (p.getMaterial().getFlowCalculator().particleIsDepositing(p, c, rand)) {
                    p.setDeposited(true);
                    //Do nothing anymore from here.
                    return;
                } else {
                    //Stays mobile

                }
            }
        }

        /**
         * calculate spatial distance in this timestep.
         */
        double adv = 0;
//        status = 5;
        if (c.getClass().equals(Pipe.class)) {
//            status = 6;
            adv = Math.abs(((Pipe) c).getVelocity()) * (dt/*-timeSpend*/);
            p.setVelocity1d(((Pipe) c).getVelocity());
//            status = 7;
        } else {
//            status = 8;
            if (c.getConnections().length > 0) {
//                status = 9;
//                System.out.println("water: "+c.getWaterHeight()+",   connection: "+c.getConnections()[0].getHeight());
                if (c.getConnections()[0].getHeight() > c.getWaterHeight()) {
//                    status = 10;
                    //Nothing can flow out-> break
                    return;
                }
            }
        }
//        status = 11;
        double diff = calcDistanceTurbulentDiffusion(p.getVelocity1d()) * rand.nextGaussian();
//        status = 12;
        /**
         * if ds is positive : advective dominant else diffusiv has higher
         * negative influence
         */
        double remaining_dt = dt;
        double ds;
        float ds_adv = 0;
        for (int lengthtype = 0; lengthtype < 2; lengthtype++) {
            //Do the advektive step first and the diffusive step second to distinguish between the two travel types for the total travel distance.
            if (lengthtype == 0) {
                ds = (float) Math.abs(adv);
            } else {
                ds = (float) diff;
            }
//            status = 13;
            /**
             * First step directing particle to next manhole
             */
            if (c.getClass().equals(Pipe.class)) {
//                status = 14;
                Pipe pipe = (Pipe) c;
                if (pipe.getVelocity() > 0) {
                    double neuePosition = position1d + ds;
//                    status = 15;
                    if (neuePosition < 0) {
                        // Particle flowed to the start of the pipe and reached the manhole
//                        status = 16;
                        c = pipe.getStartConnection().getManhole();
                        remaining_dt -= Math.abs(position1d / pipe.getVelocity());
                        if (lengthtype == 0) {
                            ds_adv += Math.abs(position1d);
                        }
//                        status = 17;
                        ds += position1d;
                        position1d = 0;
                    } else if (neuePosition <= pipe.getLength()) {
                        // Nothing to do, if particle stays inside this Pipe
                        position1d = neuePosition;
//                        status = 18;
                        if (lengthtype == 0) {
                            ds_adv += Math.abs(ds);
                        }
                        remaining_dt = 0;
                        continue;
                    } else {
                        //Über dieses Rohr hinaus geschossen.
                        // particle reached end of pipe
//                        status = 20;
                        remaining_dt -= Math.abs((position1d - pipe.getLength()) / pipe.getVelocity());
                        ds = neuePosition - pipe.getLength();
                        if (lengthtype == 0) {
                            ds_adv += (pipe.getLength());
                        }
                        position1d = 0;
                        c = pipe.getEndConnection().getManhole();
                        if (p.getClass().equals(HistoryParticle.class)) {
                            ((HistoryParticle) p).addToHistory(c);
                        }
                    }
                } else {
                    /**
                     * Negative velocity*
                     */
//                    status = 30;
                    if (position1d - ds < 0) {
                        // Particle flowed to the start of the pipe and reached the manhole
                        c = pipe.getStartConnection().getManhole();
                        if (lengthtype == 0) {
                            ds_adv += Math.abs(position1d);
                        }
                        ds -= position1d;
                        position1d = 0;
                    } else if (position1d - ds < pipe.getLength()) {
                        // Nothing to do, if particle stays inside this Pipe
                        position1d -= ds;
                        if (lengthtype == 0) {
                            ds_adv += Math.abs(ds);
                        }
                        ds = 0;
                        remaining_dt = 0;
                        continue;
                    } else if (position1d - ds >= pipe.getLength()) {
                        // particle reached end of pipe
                        c = pipe.getEndConnection().getManhole();
                        remaining_dt -= Math.abs((pipe.getLength() - position1d) / pipe.getVelocity());
                        ds = ds - (pipe.getLength() - position1d);
                        if (lengthtype == 0) {
                            ds_adv += Math.abs(pipe.getLength());
                        }
                        position1d = 0;
                        if (p.getClass().equals(HistoryParticle.class)) {
                            ((HistoryParticle) p).addToHistory(c);
                        }
                    } else {
                        System.out.println("  what a strange case 2 ds: " + ds + " pos1d: " + position1d + "  in " + c);
                    }
                }
            }
//            status = 40;
            /**
             * Start loop with particles, that start in a Manhole
             */
            int loops = 0;
            if (c.getClass().equals(Manhole.class)) {
                position1d = 0;
                if (ds >= 0) {
                    for (int i = 0; i < 30; i++) {
//                        passedPipes++;
                        loops++;
                        /**
                         * 1. Case: particle is in a Manhole
                         */
                        if (lengthtype == 0 && remaining_dt <= 0) {
                            break;
                        }
                        try {
                            Manhole mh = (Manhole) c;
                            if (c.isSetAsOutlet()) {
                                p.setLeftSimulation();
                                p.setPosition1d_actual(0);
                                p.setSurrounding_actual(c);
                                return;
                            }
                            /**
                             * search only for outflowing connections
                             */
                            Connection_Manhole connection = p.getMaterial().getFlowCalculator().whichConnection(mh, rand, true);

                            if (connection == null) {
                                //particle stays inside this manhole
                                c = mh;
                                p.setSurrounding_actual(mh);
                                p.setPosition1d_actual(0);
                                break;
                            } else if (connection.getClass().equals(Connection_Manhole_Surface.class)) {
                                //spill to surface
                                p.surfaceCellID = mh.getSurfaceTriangleID();
                                p.setSurrounding_actual(surface);
                                c = surface;
//                                double[] tripos = surface.getTriangleMids()[mh.getSurfaceTriangleID()];
                                p.setPosition3D(mh.getPosition3D(0));
//                                p.setPosition3D(tripos[0], tripos[1]);
                                p.setOnSurface();
                                p.toSurfaceTimestamp = ThreadController.getSimulationTimeMS();
                                p.toSurface = mh;
                                p.posToSurface = (float) p.getTravelledPathLength();
                                if (p.getClass().equals(HistoryParticle.class)) {
                                    ((HistoryParticle) p).addToHistory(surface);
                                }
//                                            System.out.println("Particle " + p.getId() + " spilled out to triangle " + mh.getSurfaceTriangleID());
                                break;
                            }
                            //else is going into a pipe.
                            Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) connection;

                            Pipe pipe = con.getPipe();
                            c = pipe;
                            if (lengthtype == 0) {
                                ds = Math.abs((float) (remaining_dt * (pipe.getVelocity())));
                            }
                            if (con.isStartOfPipe()) {
                                if (pipe.getVelocity() >= 0) {
                                    //Downstream Pipe
                                    if (ds < 0) {
                                        System.out.println("  # ds <0   = " + ds + "  v=" + pipe.getVelocity());
                                    }
                                    if (ds > pipe.getLength()) {
                                        ds -= pipe.getLength();
                                        remaining_dt -= Math.abs(pipe.getLength() / pipe.getVelocity());
                                        c = pipe.getEndConnection().getManhole();
//                                        pipe.getMeasurementTimeLine().addParticle(p);
                                        ds_adv += Math.abs(pipe.getLength());
                                        if (p.getClass().equals(HistoryParticle.class)) {
                                            ((HistoryParticle) p).addToHistory(c);
                                        }
                                        continue;
                                    } else {
                                        // Particle will end this timestep in this pipe.
                                        if (ds < 0) {
                                            System.out.println("  downstream in " + c + " ds=" + ds + "   pos1d=" + position1d);
                                        }
                                        position1d = ds;
                                        if (lengthtype == 0) {
                                            ds_adv += Math.abs(ds);
                                        }
                                        ds = 0;
                                        if (position1d > pipe.getLength()) {
                                            System.out.println(" +toofar1 ");
                                        }
                                        break;
                                    }
                                } else {
                                    System.out.println("##+ Fehler v=" + pipe.getVelocity());
                                }
                            } else {
                                // con is End of Pipe
                                if (pipe.getVelocity() < 0) {
                                    // reverse flown Pipe
                                    if (ds < 0) {
                                        System.out.println(" ##falschherum ds=" + ds + " in End of negative flown Pipe " + pipe + "  ds from " + lengthtype);
                                    }

                                    if (ds > pipe.getLength()) {
                                        //Verbleibender Schritt ist größer als die Rohrlänge
                                        ds -= pipe.getLength();
                                        remaining_dt -= Math.abs(pipe.getLength() / pipe.getVelocity());
//                                        pipe.getMeasurementTimeLine().addParticle(p);
                                        c = pipe.getStartConnection().getManhole();
                                        if (p.getClass().equals(HistoryParticle.class)) {
                                            ((HistoryParticle) p).addToHistory(c);
                                        }
                                        if (lengthtype == 0) {
                                            ds_adv += Math.abs(pipe.getLength());
                                        }
                                        continue;
                                    } else {
                                        position1d = pipe.getLength() - ds;
                                        if (lengthtype == 0) {
                                            ds_adv += Math.abs(ds);
                                        }
                                        ds = 0;
                                        if (position1d > pipe.getLength()) {
                                            System.out.println(" +toofar2 ");
                                        }
                                        break;
                                    }
                                } else {
                                    System.out.println(" ##++ Fehler in Pipe v::" + pipe.getVelocity());
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(this.getClass() + ":: Unsupported Capacity " + c.getClass() + " to move particle here. loop " + loops + " startcapacity:" + p.getSurrounding_actual());
                            e.printStackTrace();
                            break;
                        }
                    }
                } else {
                    //###################################################################
                    // ds<0
                    //Negative directed ds because of great negative Diffusion influence
                    //-> backward (against velocitiy) travelling
                    for (int i = 0; i < 30; i++) {
                        loops++;
//                        passedPipes++;
                        /**
                         * 1. Case: capacity is in a Manhole
                         */
                        if (lengthtype == 0 && remaining_dt < 0) {
                            break;
                        }
                        Manhole mh = (Manhole) c;
                        if (c.isSetAsOutlet()) {
                            p.setLeftSimulation();
                            p.setPosition1d_actual(0);
                            p.setSurrounding_actual(c);
                            return;
                        }
                        Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) p.getMaterial().getFlowCalculator().whichConnection(mh, rand, false);
                        if (con == null || con.getPipe() == null) {
                            /*Aktuell kein Ausgang zu finden. Bleibe hier, bis sich 
                             später die Wasserstände ändern*/
                            position1d = 0;
                            break;
                        }
                        Pipe pipe = con.getPipe();
                        if (lengthtype == 0) {
                            ds = Math.abs((float) (remaining_dt * (pipe.getVelocity())));
                        }
                        c = pipe;
                        if (con.isStartOfPipe()) {
                            if (pipe.getVelocity() <= 0) {
                                // reverse flown Pipe
                                if (ds > 0) {
                                    System.out.println(" ## ds >0 in reverse flown forward pipe. ds=" + ds);
                                }
                                if (-ds > pipe.getLength()) {
                                    ds += pipe.getLength();
                                    remaining_dt -= Math.abs(pipe.getLength() / pipe.getVelocity());
                                    c = pipe.getEndConnection().getManhole();
//                                    pipe.getMeasurementTimeLine().addParticle(p);
                                    if (lengthtype == 0) {
                                        ds_adv += Math.abs(pipe.getLength());
                                    }
                                    if (p.getClass().equals(HistoryParticle.class)) {
                                        ((HistoryParticle) p).addToHistory(c);
                                    }
                                    continue;
                                } else {
                                    position1d = -ds;
                                    if (lengthtype == 0) {
                                        ds_adv += Math.abs(ds);
                                    }
                                    ds = 0;
                                    if (position1d > pipe.getLength()) {
                                        System.out.println(" -toofar1 ");
                                    }
                                    break;
                                }

                            } else {
                                System.out.println("###Fehler v=" + pipe.getVelocity() + " ds=" + ds + " is flowincoming?" + con.isFlowInletToPipe());
                            }
                        } else {
                            //Connection is end of pipe
                            if (pipe.getVelocity() >= 0) {
                                if (ds > 0) {
                                    System.out.println(" ##falschherum ds=" + ds + " in End of positive flown Pipe " + pipe);
                                }
                                if (pipe.getLength() <= -ds) {
                                    c = pipe.getStartConnection().getManhole();
//                                    pipe.getMeasurementTimeLine().addParticle(p);
                                    remaining_dt -= Math.abs(pipe.getLength() / pipe.getVelocity());
                                    ds += pipe.getLength();
                                    if (lengthtype == 0) {
                                        ds_adv += Math.abs(pipe.getLength());
                                    }
                                    if (p.getClass().equals(HistoryParticle.class)) {
                                        ((HistoryParticle) p).addToHistory(c);
                                    }
                                    continue;
                                } else {
                                    position1d = pipe.getLength() + ds;
                                    if (lengthtype == 0) {
                                        ds_adv += Math.abs(ds);
                                    }
                                    if (position1d < 0) {
                                        System.out.println(" ### position<0: ds=" + ds);
                                    }
                                    if (position1d > pipe.getLength()) {
                                        System.out.println(" ### position>Pipe.length: ds=" + ds);
                                    }
                                    ds = 0;
                                    break;
                                }
                            } else {
                                System.out.println("##- Fehler v=" + pipe.getVelocity());
                            }
                        }
                    }
                }
            }
        }
//        status = 50;
        p.setSurrounding_actual(c);
        p.setPosition1d_actual(position1d);
//        p.addMovingLength(ds_adv + diff);
//        status = 51;
//        c.getMeasurementTimeLine().addParticle(p);
        if (c.getClass().equals(Pipe.class)) {
            Pipe pipe = (Pipe) c;
//            status = 52;

            pipe.getMeasurementTimeLine().addParticle(p);

//            p.setVelocity1d(pipe.getVelocity());
        }
//        status = 53;
    }

    /**
     * Version uses advective speed in every visited pipe
     *
     * @param p
     */
    private void moveParticle5_celltransmission(Particle p) {
        float position1d = p.getPosition1d_actual();
        Capacity c = p.getSurrounding_actual();
        if (Float.isNaN(position1d)) {
            position1d = 0;
        }
        if (c == null || c.isSetAsOutlet()) {
//            p.setInactive();
            p.setLeftSimulation();
            p.setPosition1d_actual(0);
            p.setVelocity1d(0);
            return;
        }
        //Skip dry condition 
        if (c.getWaterlevel() < FlowCalculatorMixed.dryWaterlevel) {
            return;
        }

        if (useDeposition) {
            //Test for deposition
            if (p.isDeposited()) {
                if (p.getMaterial().getFlowCalculator().particleIsEroding(p, c, rand)) {
                    p.setDeposited(false);
                } else {
                    //Stays immobile
                    //Do nothing
                    return;
                }
            } else {
                if (p.getMaterial().getFlowCalculator().particleIsDepositing(p, c, rand)) {
                    p.setDeposited(true);
                    //Do nothing anymore from here.
                    return;
                } else {
                    //Stays mobile

                }
            }
        }

        /**
         * calculate spatial distance in this timestep.
         */
        float distance_adv = 0;
        float distance_diff = 0;//(float) (diffusionDistance*rand.nextGaussian());
        float distance_total = 0;
        float resultVelocity = 0;
        boolean reverseDispersion = false; //flag true if dispersion is dominant and reverses advective direction

        /**
         * if ds is positive : advective dominant else diffusiv has higher
         * negative influence
         */
        float remaining_dt = dt;
        float timespend = 0;
        float moved = 0;
        float neuePosition = position1d;

        // Move particles to the end of their pipe because it is easier to start them at a manhole
        if (c.getClass().equals(Pipe.class)) {
            Pipe pipe = ((Pipe) c);
            float v = (float) pipe.getVelocity();
            distance_adv = v * (dt);
            p.setVelocity1d(v);
            distance_diff = (float) (calcDistanceTurbulentDiffusion(v) * rand.nextGaussian());
//            if (distance_diff < 0) {
//                distance_diff *= -1;
//            }
            distance_total = distance_adv + distance_diff;
            resultVelocity = distance_total / dt;
            reverseDispersion = distance_adv * distance_total < 0;//!(distance_total < 0 ^ v < 0);
//            if(!forward){
//                System.out.println(forward+": "+(distance_total<0)+", "+(v<0));
//            }

            neuePosition = position1d + distance_total;
            if (neuePosition < 0) {
                moved = position1d;
                neuePosition -= position1d;
                timespend = Math.abs(moved / resultVelocity);
                remaining_dt -= timespend;
                c = pipe.getStartConnection().getManhole();
                pipe.getMeasurementTimeLine().addParticle(p, timespend / dt);
            } else if (neuePosition > pipe.getLength()) {
                moved = pipe.getLength() - position1d;
                neuePosition -= pipe.getLength();
                timespend = Math.abs(moved / resultVelocity);
                remaining_dt -= timespend;
                c = pipe.getEndConnection().getManhole();
                pipe.getMeasurementTimeLine().addParticle(p, timespend / dt);
            } else {
                moved = distance_total;
                remaining_dt = 0;
                c = pipe;
                pipe.getMeasurementTimeLine().addParticle(p, 1f);
            }

//            pipe.getMeasurementTimeLine().addParticle(p);
            p.addMovingLength(moved);
        } else {
            if (c.getConnections().length > 0) {
                if (c.getConnections()[0].getHeight() > c.getWaterHeight()) {
                    //Nothing can flow out-> break
                    return;
                }
            }
        }

        /**
         * Start loop with particles, that start in a Manhole
         */
        int loops = 0;

        if (c.getClass().equals(Manhole.class)) {
            position1d = 0;
            while (remaining_dt > 0) {
                if (loops > maxloopsPerParticle) {
                    System.out.println(p + " in " + c + " exceeded maximum number of " + maxloopsPerParticle + " loops per timestep, remain:" + remaining_dt + "\tdiffusion:" + distance_diff);
                    break;
                }
                loops++;
                try {
                    Manhole mh = (Manhole) c;
                    if (c.isSetAsOutlet()) {
                        p.setLeftSimulation();
                        p.setPosition1d_actual(0);
                        p.setSurrounding_actual(c);
                        return;
                    }
                    /**
                     * search only for outflowing connections
                     */
                    Connection_Manhole connection = p.getMaterial().getFlowCalculator().whichConnection(mh, rand, !reverseDispersion);

                    if (connection == null) {
                        //particle stays inside this manhole
                        c = mh;
                        p.setSurrounding_actual(mh);
                        p.setPosition1d_actual(0);
                        break;
                    } else if (connection.getClass().equals(Connection_Manhole_Surface.class)) {
                        //spill to surface
                        p.surfaceCellID = mh.getSurfaceTriangleID();
                        p.setSurrounding_actual(surface);
                        c = surface;
                        p.setPosition3D(mh.getPosition3D(0));
                        p.setOnSurface();
                        p.toSurfaceTimestamp = ThreadController.getSimulationTimeMS();
                        p.toSurface = mh;
                        p.posToSurface = (float) p.getTravelledPathLength();
                        if (p.getClass().equals(HistoryParticle.class)) {
                            ((HistoryParticle) p).addToHistory(surface);
                        }
                        break;
                    }
                    //else is going into a pipe.
                    Connection_Manhole_Pipe con = (Connection_Manhole_Pipe) connection;
                    Pipe pipe = con.getPipe();

                    c = pipe;
                    float v = (float) pipe.getVelocity();
                    distance_adv = v * (remaining_dt);
                    p.setVelocity1d(v);
                    //TODO: FIX: Reassigning of next dispersive step causes too low velocities
                    //distance_diff  =(float) (calcDistanceTurbulentDiffusion(v) * rand.nextGaussian());//(float) (Math.sqrt(2*diffusionturbulentCoefficient*remaining_dt)*rand.nextGaussian());//float) (calcDistanceTurbulentDiffusion(v) * rand.nextGaussian() * remaining_dt / dt);
                    distance_total = distance_adv + (distance_diff * remaining_dt / dt);
                    resultVelocity = distance_total / remaining_dt;
                    reverseDispersion = distance_adv * distance_total < 0;//!(distance_total < 0 ^ v < 0);

                    if (con.isStartOfPipe()) {
                        position1d = 0;
                    } else {
                        position1d = pipe.getLength();
                    }
                    neuePosition = position1d + distance_total;

                    //Downstream Pipe
                    if (neuePosition < 0) {
                        //System.out.println(position1d+" +" +distance_adv+"+"+distance_diff+" = \t"+distance_total);
                        moved = position1d;
                        timespend = Math.abs(moved / resultVelocity);
//                        if (timespend <= 0) {
//                            System.out.println("negative teimespend 123");
//                            timespend = -timespend;
//                        }
                        remaining_dt -= timespend;
                        neuePosition = 0;
                        c = pipe.getStartConnection().getManhole();
                        //if (moved > 0) {
                        pipe.getMeasurementTimeLine().addParticle(p, timespend / dt);
                        // }
                    } else if (neuePosition > pipe.getLength()) {
                        //rushed through this pipe and is now inside the next manhole

                        moved = pipe.getLength() - position1d;
                        timespend = moved / resultVelocity;
//                        if (timespend <= 0) {
//                            System.out.println("negative teimespend 134");
//                            timespend = -timespend;
//                        }
                        remaining_dt -= timespend;
                        c = pipe.getEndConnection().getManhole();
                        neuePosition = 0;
                        if (p.getClass().equals(HistoryParticle.class)) {
                            ((HistoryParticle) p).addToHistory(c);
                        }
                        //  if (moved > 0) {
                        pipe.getMeasurementTimeLine().addParticle(p, timespend / dt);
                        //  }
                    } else {
                        // Particle will end this timestep in this pipe.
                        moved = Math.abs(neuePosition - position1d);
                        timespend = Math.abs(moved / resultVelocity);
//                        if (timespend <= 0) {
//                            System.out.println("negative teimespend 154");
//                            timespend = -timespend;
//                        }
                        remaining_dt -= timespend;
//                        if(remaining_dt>0){
//                            System.out.println("remaining: "+remaining_dt+" s");
//                        }
                        //position1d = neuePosition;
                        remaining_dt = 0;
                        pipe.getMeasurementTimeLine().addParticle(p, timespend / dt);
                    }
                    if (timespend == 0) {
                        break;
                    }

                    p.addMovingLength(moved);
                } catch (Exception e) {
                    System.out.println(this.getClass() + ":: Unsupported Capacity " + c.getClass() + " to move particle here. loop " + loops + " startcapacity:" + p.getSurrounding_actual());
                    e.printStackTrace();
                    break;
                }
            }
        }

        p.setSurrounding_actual(c);

        p.setPosition1d_actual(neuePosition);

//        if (c.getClass().equals(Pipe.class)) {
//            Pipe pipe = (Pipe) c;
//        }
    }

    public double getDeltaTime() {
        return dt;
    }

    public double calcDistanceTurbulentDiffusion(double velocity) {
        if (turbulentCalculation == TURBULENT_DIFFUSION.FIX_DIFFUSION_COEFFICIENT) {
            return diffusionDistance;
        }
        return dt * diffusivityTurbulentCoefficient * velocity;
    }

    public void setSpillOutToSurface(boolean spillOutToSurface) {
        this.spillOutToSurface = spillOutToSurface;
    }

    public boolean isSpillingOutToSurface() {
        return spillOutToSurface;
    }

    public void setRandomNumberGenerator(RandomArray rd) {
        this.rand = rd;
//        this.randDist.setRandomGenerator(rand);
    }

}
