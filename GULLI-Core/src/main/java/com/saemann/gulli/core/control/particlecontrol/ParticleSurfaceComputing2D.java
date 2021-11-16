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
package com.saemann.gulli.core.control.particlecontrol;

import com.saemann.gulli.core.control.maths.GeometryTools;
import com.saemann.gulli.core.control.maths.RandomGenerator;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Dispersivity;
import com.saemann.gulli.core.model.particle.HistoryParticle;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.SurfaceTriangle;
import com.saemann.gulli.core.model.surface.SurfaceTrianglePath;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.topology.Inlet;
import com.saemann.gulli.core.model.topology.Manhole;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 * Dealing with the transport of particles on a surface. Particles move in 2d
 * space continuum. Must be one per ParticleThread. is not threadsave when used
 * by multiple htreads.
 *
 * @author riss,saemann
 */
public class ParticleSurfaceComputing2D implements ParticleSurfaceComputing {

    public static boolean verbose = false;

    public static boolean gridFree = false;

    public static boolean decoupledDispersivity = false;

    /**
     * Deltatime for timestep in [second]. Set by ThreadController.
     */
    protected float dt = 1;

    public int threadindex = 0;

    private float sqrt2dt = (float) Math.sqrt(2 * dt);

    private long simulationtime;

    protected Surface surface;

    /**
     * A random variable to generate decisions. Should be reset at each new
     * start of a simulation.
     */
    protected RandomGenerator random;

    /**
     * Stutus variable for debugging. Increase after every important step to see
     * where surface computing is hanging.
     */
    public int status = -1;

    /**
     * Calculates Diffusion dependent on the surrounding of the particle.
     */
//    protected Dispersion2D_Calculator D = new Dispersion2D_Constant();
    public boolean enableDiffusion = true;
    public boolean enableminimumDiffusion = false;
    public boolean getTestSolutionForAnaComparison = false;

    /**
     * if true the velocity in a cell is calculated at the entrance time. This
     * is slower than using the time of the movement step start.
     */
//    public static boolean useSubdividedTimestamps = true;
    /**
     * If a velocity is larger than this value, the speed is limited to this
     * value [meters / second]
     */
    public static double maxVelocity = 5;

    /**
     * minimum velocity [m/s] if particles are in cells with slower velocity,
     * the slope direction is used with this speed.
     */
    public static double dryFlowVelocity = 0.05;

    public static double dryWaterlevel = 0.01;

    public static int maxNumberOfIterationLoops = 100;

    /**
     * When active particles can go to the pipe system through inlets and
     * manholes.
     */
    public static boolean allowWashToPipesystem = true;

    /**
     * If true, the particle will not enter cells, where the absolute velocity
     * (in x) is 0.
     */
    public static boolean preventEnteringDryCell = false;

    /**
     * If no velocity is set, the hill slope is used for direction and 0.01 m/s
     * are applied.
     */
    public static boolean gradientFlowForDryCells = true;

    /**
     * BLock very slow dry flow particles.
     */
    public static boolean blockVerySlow = true;

    public static float minimumDistanceBeforeBlock = (float) (dryFlowVelocity * 1 * 0.0001);

    /**
     * if true, the particle is moving with gradient flow and can enter dry
     * cells.
     */
    private boolean gradientFlowstateActual = false;

    /**
     * Use the projected length along an edge if the edge cannot be passed by a
     * particle. Otherwise, the movement stops abrupt at the edge which causes
     * particles to be trapped when very close to an edge.
     */
    public static boolean slidealongEdges = true;
//    /**
//     * if false(default), the random variable for the random walk is only
//     * generated at the begin of the particle step and is kept if multiple cells
//     * are visited. if true , the random number is generated new on every
//     * visited cell during the timestep.
//     */
//    public static boolean multiTimeRandomisation = false;

    public static int numberOfErrors = 0;

    /**
     * The trace of history particles will only be updated (extended) if the
     * position changed more than this distance.
     */
    public static double minTraceDistance = 10;

    /**
     * If velocities of two sides are competititve (particle will always swap
     * between sides), then the mean velocity is used.
     */
    public static boolean meanVelocityAtZigZag = true;

    /**
     * If true, not the start of the current timestep is used but the mean value
     * in this timestep
     */
    public enum TIMEINTEGRATION {
        EXPLICIT, STEPSPLICIT, CRANKNICOLSON
    };
    public static TIMEINTEGRATION timeIntegration = TIMEINTEGRATION.STEPSPLICIT;

    /**
     * actual timeindex for the particle in loop. A sub-step time of the
     * surface's snapshots. Used for more accurate interpolation if timesteps
     * are large.
     */
    private double actualisedTime;
    /**
     * Create new random numbers for the random walk step every X seconds
     */
//    public static float randomizeAfterSeconds = Float.POSITIVE_INFINITY;

    /////Arrays to be filled from other functions. so no extra allocation is needed.
    private double[] particlevelocity = new double[2];
    private final double[] temp_barycentricWeights = new double[3];
    private final double[] temp_barycentricWeightsOld = new double[3];
//    private final double[][] tempVertices = new double[3][3];
    private double[] tempDiff = new double[2];
    private double[] tempProjection = new double[2];
    private float[] tempVelocity = new float[2];
    private float[] tempVelocity2 = new float[2];
    private double[] tempVelocityD = new double[2];
    private double[] tempVelocityD2 = new double[2];

    private double[] tempPosLow = new double[3], tempPosUp = new double[3];

    private double posxalt, posyalt, posxneu, posyneu, totalvelocity;
    private float timeLeft;
    private double surfaceTimeIndexDoubleStart = 0;
    private double surfaceTimeIndexDoubleEnd = 0;
    private double surfaceActualFrac = 0;
    private int surfaceActualIndexInt = 0;
    private long startCell = -1;
    private boolean wasInFreeflow = false; //Indicate if a particle has been outside dry condition during its simulation step

    private boolean shouldReRandomize = true;

//    private int trackid = 634504;
    private DecimalFormat df = new DecimalFormat("0.0000", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Temporaryly used integers for the neighbour search
     */
    private int node0, node1, node2;
    private double[] vertex0, vertex1, vertex2;

    private int cellID;
    private int loopcounter;
    private double z1, z2;
    private double sqrt2dtDx, sqrt2dtDy;
    private double[] st01 = new double[2], st12 = new double[2], st20 = new double[2];
    private double lengthfactor = 1;
    private double temp_distance;
    private int bwindex = -1;
    private int checkbwindex = -1;
    private int cellIDnew;

    private double dx, dy;
    private int vstatus = 0;
    private int shortLengthCounter = 0;
    private int oldCellID1 = -1, oldCellID2 = -2, startCellID = -3;

    private boolean calculateVelocityPosition = true;
    private boolean isprojecting = false;
    private boolean zigzag = false;

    public ParticleSurfaceComputing2D(Surface surface, int threadIndex) {
        this.surface = surface;
        this.threadindex = threadIndex;
    }

//    public void setDiffusionCalculation(Dispersion2D_Constant D) {
//        this.D = D;
//    }
//
//    public Dispersion2D_Calculator getDiffusionCalculator() {
//        return D;
//    }
    /**
     * Reset this module to prepare for a start of an identical new simulation.
     */
    @Override
    public void reset() {
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public void setSimulationtime(long simulationtime) {
        this.simulationtime = simulationtime;
        if (surface == null) {
            return;
        }
        if (null == timeIntegration) {
            surfaceTimeIndexDoubleStart = surface.getActualTimeIndex_double();
            surfaceTimeIndexDoubleEnd = surfaceTimeIndexDoubleStart;
        } else {
            switch (timeIntegration) {
                case CRANKNICOLSON:
                    surfaceTimeIndexDoubleStart = surface.getActualTimeIndex_double();
                    surfaceTimeIndexDoubleEnd = surface.getTimes().getTimeIndexDouble((long) (simulationtime + dt * 1000));
                    surfaceActualFrac = surfaceTimeIndexDoubleStart % 1.;
                    surfaceActualIndexInt = (int) surfaceTimeIndexDoubleStart;
                    break;
                case STEPSPLICIT:
                    surfaceTimeIndexDoubleStart = surface.getActualTimeIndex_double();
                    surfaceTimeIndexDoubleEnd = surface.getTimes().getTimeIndexDouble((long) (simulationtime + dt * 1000));
                    surfaceActualFrac = surfaceTimeIndexDoubleStart % 1.;
                    surfaceActualIndexInt = (int) surfaceTimeIndexDoubleStart;
                    break;
                default:
                    surfaceTimeIndexDoubleStart = surface.getActualTimeIndex_double();
                    surfaceTimeIndexDoubleEnd = surfaceTimeIndexDoubleStart;
                    surfaceActualFrac = surfaceTimeIndexDoubleStart % 1.;
                    surfaceActualIndexInt = (int) surfaceTimeIndexDoubleStart;
                    break;
            }
        }
    }

    /**
     * Called from Threadcontroller for each Particle. Delegates to the actual
     * selected transport function.
     *
     * @param p
     * @param dt simulation timestep [s]
     */
    @Override
    public void moveParticle(Particle p, double dt) {
        checkSurrounding(p);
        shortLengthCounter = 0;

        cellID = p.surfaceCellID;

        shouldReRandomize = true;
        moveParticleCellIterative2(p, (float) dt);

        if (p.isOnSurface() && !surface.getMeasurementRaster().spatialConsistency) {
            //Only measure once at the end, if spatial consistency is disabled.
            // (measurements will not be taken inside the iterative process)
            surface.getMeasurementRaster().measureParticle(simulationtime, p, 1, threadindex);
        }

        if (p.tracing() && p.isOnSurface()) {
            if (posxalt != posxneu) {
                if (Math.abs(posxneu - ((HistoryParticle) p).getLastUTMX()) > minTraceDistance || Math.abs(posyneu - ((HistoryParticle) p).getLastUTMy()) > minTraceDistance) {
                    tempProjection[0] = posxneu;
                    tempProjection[1] = posyneu;

                    try {
                        surface.getGeotools().toGlobal(tempProjection, tempProjection, true);
                        ((HistoryParticle) p).addToHistory(new Coordinate(tempProjection[0], tempProjection[1]), posxneu, posyneu);
                    } catch (TransformException ex) {
                        Logger.getLogger(ParticleSurfaceComputing2D.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Calculates the new position based on the current cell flow field. Stores
     * the result in the posxneu & posyneu variable.
     *
     * @param p
     * @param timeLeft
     * @param dt
     * @return false if particle does not move;
     */
    public boolean calcPrePosition(Particle p, float timeLeft, float dt) {
        switch (timeIntegration) {
            case CRANKNICOLSON:
                surface.getTriangleVelocity(cellID, surfaceTimeIndexDoubleStart, tempVelocityD);
                surface.getTriangleVelocity(cellID, surfaceTimeIndexDoubleEnd, tempVelocityD2);
                particlevelocity[0] = (tempVelocityD[0] + tempVelocityD2[0]) * 0.5;
                particlevelocity[1] = (tempVelocityD[1] + tempVelocityD2[1]) * 0.5;
                break;
            case STEPSPLICIT:
                actualisedTime = surfaceTimeIndexDoubleEnd + (surfaceTimeIndexDoubleStart - surfaceTimeIndexDoubleEnd) * (timeLeft / dt);
                tempVelocity = surface.getTriangleVelocity(cellID, (int) actualisedTime);
                tempVelocity2 = surface.getTriangleVelocity(cellID, (int) actualisedTime + 1);
                particlevelocity[0] = tempVelocity[0] + (tempVelocity2[0] - tempVelocity[0]) * (actualisedTime % 1.);
                particlevelocity[1] = tempVelocity[1] + (tempVelocity2[1] - tempVelocity[1]) * (actualisedTime % 1.);
                break;
            default:
                tempVelocity = surface.getTriangleVelocity(cellID, surfaceActualIndexInt);
                tempVelocity2 = surface.getTriangleVelocity(cellID, surfaceActualIndexInt + 1);
                particlevelocity[0] = tempVelocity[0] + (tempVelocity2[0] - tempVelocity[0]) * (surfaceActualFrac);
                particlevelocity[1] = tempVelocity[1] + (tempVelocity2[1] - tempVelocity[1]) * (surfaceActualFrac);

                break;
        }

        totalvelocity = testVelocity(particlevelocity);

        if (p.getMaterial().getDispersionCalculatorSurface() != null) {
            // calculate with diffusion 
            if (totalvelocity < dryFlowVelocity) {
                if (gradientFlowForDryCells) {
                    gradientFlowstateActual = true;
                    totalvelocity = dryFlowVelocity;
                    particlevelocity[0] = surface.getTriangle_downhilldirection()[cellID][0] * totalvelocity;
                    particlevelocity[1] = surface.getTriangle_downhilldirection()[cellID][1] * totalvelocity;
                    if (Double.isNaN(surface.getTriangle_downhilldirection()[cellID][0])) {
                        particlevelocity[0] = 0;
                        particlevelocity[1] = 0;
                    }

                    posxneu = (posxalt + particlevelocity[0] * timeLeft);// only advection
                    posyneu = (posyalt + particlevelocity[1] * timeLeft);// only advection

                } else {
                    posxneu = posxalt;
                    posyneu = posyalt;
                }
                p.setDrySurfaceMovement(true);
            } else {
                gradientFlowstateActual = false;

                p.setDrySurfaceMovement(false);
                //Optimized version already gives the squarerooted values. (To avoid squareroot operations [very slow]
                try {
                    p.getMaterial().getDispersionCalculatorSurface().calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);//D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);
                } catch (Exception e) {
                    System.out.println("Particle: " + p);
                    System.out.println("Material: " + p.getMaterial());
                    System.out.println("Dispersion:" + p.getMaterial().getDispersionCalculatorSurface());
                }
                sqrt2dtDx = sqrt2dt * tempDiff[0];
                sqrt2dtDy = sqrt2dt * tempDiff[1];

                if (shouldReRandomize) {
                    z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
                    z1 = nextRandomGaussian();
                    shouldReRandomize = false;
                }

                // random walk step
                if (decoupledDispersivity) {
                    Dispersion2D_Dispersivity f = (Dispersion2D_Dispersivity) p.getMaterial().getDispersionCalculatorSurface();

                    dx = Math.sqrt(2 * particlevelocity[0] * f.getLongitudinalDispersivity() * dt) * z1 + Math.sqrt(2 * particlevelocity[1] * f.getTransversalDispersivity() * dt) * z2;
                    dy = Math.sqrt(2 * particlevelocity[0] * f.getTransversalDispersivity() * dt) * z1 + Math.sqrt(2 * particlevelocity[1] * f.getLongitudinalDispersivity() * dt) * z2;
                    posxneu = (posxalt + (particlevelocity[0] + dx / dt) * timeLeft);
                    posyneu = (posyalt + (particlevelocity[1] + dy / dt) * timeLeft);
                } else {

                    if (p.getMaterial().getDispersionCalculatorSurface().isIsotropic()) {
                        dx = z1 * sqrt2dtDx;

                        dy = (z2 * sqrt2dtDy);
                    } else {
                        // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991" with different values for 
                        dx = (particlevelocity[0] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[1] / totalvelocity) * z2 * sqrt2dtDy);

                        dy = (particlevelocity[1] / totalvelocity) * z1 * sqrt2dtDx - ((particlevelocity[0] / totalvelocity) * z2 * sqrt2dtDy);
                    }
                    posxneu = (posxalt + (particlevelocity[0] + dx / dt) * timeLeft);
                    posyneu = (posyalt + (particlevelocity[1] + dy / dt) * timeLeft);
                }
                if (Double.isNaN(posxneu)) {
                    System.out.println("Got a NaN x Position old was: " + posxalt + "  dt=" + dt + "  dx=" + dx + "  vx=" + particlevelocity[0] + "  totalV=" + totalvelocity + "  wurzelX:" + sqrt2dtDx + "  wurzelY:" + sqrt2dtDy + " tempDiffx:" + tempDiff[0]);
                }
            }
        } else {
            //No diffusion/dispersion
            if (totalvelocity < dryFlowVelocity) {
                if (gradientFlowForDryCells) {
                    gradientFlowstateActual = true;
                    totalvelocity = dryFlowVelocity;
                    particlevelocity[0] = surface.getTriangle_downhilldirection()[cellID][0] * totalvelocity;
                    particlevelocity[1] = surface.getTriangle_downhilldirection()[cellID][1] * totalvelocity;
                }
                p.setDrySurfaceMovement(true);
            } else {

                p.setDrySurfaceMovement(false);
                gradientFlowstateActual = false;
            }
            posxneu = (posxalt + particlevelocity[0] * timeLeft);// only advection
            posyneu = (posyalt + particlevelocity[1] * timeLeft);// only advection
//            vstatus = 6;
        }

        return true;
    }

    /**
     * @deprecated @param p
     * @param dt
     */
    private void moveParticleCellIterative(Particle p, float dt) {

        // get the particle velocity (most computation time used here)
        posxalt = p.getPosition3d().x;
        posyalt = p.getPosition3d().y;
        timeLeft = dt;

        loopcounter = 0;
//        status = 0;
        calculateVelocityPosition = true;
        isprojecting = false;
        gradientFlowstateActual = false;
        while (timeLeft > 0) {
            loopcounter++;
            if (loopcounter > maxNumberOfIterationLoops) {
                if (verbose) {
                    System.out.println("exceeded max loops (" + loopcounter + ") for particle " + p.getId() + " in cell " + cellID + " V=" + totalvelocity + "\t time left:" + timeLeft + "\t status=" + status + "  lengthfactor=" + lengthfactor + " \tvstatus:" + vstatus + "  projecting?" + isprojecting);
                }
                break;
            }

            if (calculateVelocityPosition) {

                if (!calcPrePosition(p, timeLeft, dt)) {
                    if (verbose) {
                        System.out.println("Particle " + p.getId() + " does not move.");
                    }
                    return;
                }
                if (gridFree) {
                    p.setPosition3D(posxneu, posyneu);
                    return;
                }
            }
            calculateVelocityPosition = true;

            // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
            // da eine Veränderung durch Modellränder vorkommen kann
            node0 = surface.getTriangleNodes()[cellID][0];
            node1 = surface.getTriangleNodes()[cellID][1];
            node2 = surface.getTriangleNodes()[cellID][2];

            vertex0 = surface.getVerticesPosition()[node0];
            vertex1 = surface.getVerticesPosition()[node1];
            vertex2 = surface.getVerticesPosition()[node2];

            GeometryTools.fillBarycentricWeighting(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
            if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
                //Stays inside this cell
                p.surfaceCellID = cellID;
                if (allowWashToPipesystem) {
                    washToPipesystem(p, cellID);
                }
                break;
            }
            /**
             * The lengthfactor multiplied with the movement vector hits exactly
             * the edge of the cell.
             */
            lengthfactor = 1;
            bwindex = -1;
//            outOfTriangleCounter = 0;
            st01 = GeometryTools.lineIntersectionST(st01, posxalt, posyalt, posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1]);
            st12 = GeometryTools.lineIntersectionST(st12, posxalt, posyalt, posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1]);
            st20 = GeometryTools.lineIntersectionST(st20, posxalt, posyalt, posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1]);

            //if barycentric weight index 1 is negative, the partivle crossed edge opposite side 0-2 into neighbour no.1 and so on...
            if (st12[0] >= 0 && st12[0] <= 1) {
                //Search for intersection between travl path and first edge
                lengthfactor = st12[0];
                bwindex = 0;
            }
            if (st20[0] >= 0 && st20[0] <= 1) {
                if (lengthfactor > st20[0]) {
                    bwindex = 1;
                    lengthfactor = st20[0];
                }
            }
            if (st01[0] >= 0 && st01[0] <= 1) {
                if (lengthfactor > st01[0]) {
                    lengthfactor = st01[0];
                    bwindex = 2;
                }
            }
            if (lengthfactor >= 1) {
                //No intersection. Particle can stay inside this cell.
                //But Barycentric weighting said, it is outside the cell.
                // So we have to move it back towards the cell where it belongs.
//                System.out.println("moveto center in loop " + loopcounter + "  " + st12[0] + ", " + st20[0] + ", " + st01[0] + ";\t bw:" + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
                posxneu = posxneu * 0.9 + surface.getTriangleMids()[cellID][0] * 0.1;
                posyneu = posyneu * 0.9 + surface.getTriangleMids()[cellID][1] * 0.1;
                GeometryTools.fillBarycentricWeighting(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
                    if (verbose) {
                        System.out.println("reset to cell center of " + cellID + " in loop " + loopcounter + "  BWs were:" + temp_barycentricWeights[0] + ", " + temp_barycentricWeights[1] + ", " + temp_barycentricWeights[2] + " \t ST:" + st12[0] + ", " + st20[0] + ", " + st01[0] + ", ");
                    }
                    //Particle is somewhere far away from its cell. reset the position to cell center.
                    posxneu = surface.getTriangleMids()[cellID][0];
                    posyneu = surface.getTriangleMids()[cellID][1];
                }
                if (allowWashToPipesystem) {
                    washToPipesystem(p, cellID);
                }
//                status = 30;
                break;
            } else {
                //The new cell can be easily found by checking the edgeindex, where the particle has left the cell.
                cellIDnew = surface.getNeighbours()[cellID][bwindex];//This only is correct, if the neighbours are constructed in the same order as the edges are defined. Otherwise comment in the section above.
//                      status=2;
                if (cellIDnew >= 0) {
//                    status = 1;
//                    test for velocity in this cell
                    if (preventEnteringDryCell && !gradientFlowstateActual) {
//                        status=11;
                        int timeIndex = surface.getTimeIndex((long) (surface.getActualTime() + (dt - timeLeft) * 1000));
                        tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex);
                        if (tempVelocity[0] == 0 && tempVelocity[1] == 0) {
                            if (timeIndex < surface.getNumberOfTimestamps() - 1) {
                                tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex + 1);
                            }
//                            status=12;
                            if (tempVelocity[0] == 0 && tempVelocity[1] == 0) {
                                //PArticle tries to move over the edge to a cell where it will get stuck
//                                status=13;
                                if (slidealongEdges) {
//                                    status=14;
                                    double f;
                                    //Detect the edge and project the a-priori position back onto the edge.
                                    if (bwindex == 0) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1], tempProjection);
                                        tempPosLow = vertex1;
                                        tempPosUp = vertex2;
                                    } else if (bwindex == 1) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1], tempProjection);
                                        tempPosLow = vertex2;
                                        tempPosUp = vertex0;
                                    } else if (bwindex == 2) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1], tempProjection);
                                        tempPosLow = vertex0;
                                        tempPosUp = vertex1;
                                    } else {
                                        System.err.println("wrong edge index " + bwindex);
                                        f = 0;
                                    }
                                    if (f < 0) {
                                        if (isprojecting) {
                                            //Both edges are inpermeable. Stay in the edge of this triangle
                                            posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                            posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                            return;
                                        } else {
                                            //Point is left of the edge
                                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 2) % 3];
                                            if (cellIDnew >= 0) {
                                                //transfere a bit to the direction of the new cell center
                                                posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                                posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                                            } else {
                                                //Shift would end in an empty neighbour cell
                                                calculateVelocityPosition = false;
                                                isprojecting = true;
//                                                status = 1001;
                                                posxneu = tempProjection[0];
                                                posyneu = tempProjection[1];
                                                //This can be handled in the next loop
                                                continue;
                                            }
                                        }
                                    } else if (f > 1) {
                                        if (isprojecting) {
                                            //Both edges are inpermeable. Stay in the edge of this triangle
                                            posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                            posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                            return;
                                        } else {
                                            //Point is right of the edge
                                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 1) % 3];
                                            if (cellIDnew >= 0) {
                                                //transfere a bit to the direction of the new cell center
                                                posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                                posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                                            } else {
                                                calculateVelocityPosition = false;
                                                isprojecting = true;
//                                                status = 1002;
                                                posxneu = tempProjection[0];
                                                posyneu = tempProjection[1];
                                                continue;
                                            }
                                        }
                                    } else {
                                        //Point is on the edge between the nodes
                                        //Stay inside this cell. move abit away from the edge
                                        posxneu = tempProjection[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                        posyneu = tempProjection[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                        p.surfaceCellID = cellID;
                                        p.setPosition3D(posxneu, posyneu);
                                        return;
                                    }

                                    calculateVelocityPosition = false;
                                    isprojecting = true;
//                                    status = 100;
                                    continue;
                                } else {
                                    // Stop here at the edge, because we cannot go into the target cell.
                                    temp_distance = totalvelocity * timeLeft;
                                    lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                                    lengthfactor *= 0.95;
                                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                                    if (surface.getMeasurementRaster().spatialConsistency) {
                                        p.setPosition3D(posxneu, posyneu);
                                        p.surfaceCellID = cellID;
                                        surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                                    }
                                    timeLeft -= (1. - lengthfactor);
                                    break;
                                }
                            }
                        }
                    }

                    if (!isprojecting) {
                        lengthfactor *= 1.01;//Make sure the particle is in the new cell
                    }
                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                    if (surface.getMeasurementRaster().spatialConsistency) {
                        p.setPosition3D(posxneu, posyneu);
                        p.surfaceCellID = cellID;
                        surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                    }
                    timeLeft *= (1. - lengthfactor);
                    if (allowWashToPipesystem) {
                        if (washToPipesystem(p, cellIDnew)) {
                            return;
                        }
                    }

                    //test if new cell contains position
                    node0 = surface.getTriangleNodes()[cellIDnew][0];
                    node1 = surface.getTriangleNodes()[cellIDnew][1];
                    node2 = surface.getTriangleNodes()[cellIDnew][2];

                    vertex0 = surface.getVerticesPosition()[node0];
                    vertex1 = surface.getVerticesPosition()[node1];
                    vertex2 = surface.getVerticesPosition()[node2];
                    GeometryTools.fillBarycentricWeighting(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                    if (temp_barycentricWeights[0] < 0 || temp_barycentricWeights[1] < 0 || temp_barycentricWeights[2] < 0) {
                        validateBarycentricWeights(temp_barycentricWeights, temp_barycentricWeights, 0.01);
                        GeometryTools.calcPositionFromBarycentric(tempProjection, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], temp_barycentricWeights[0], temp_barycentricWeights[1], temp_barycentricWeights[2]);
                        posxneu = tempProjection[0];
                        posyneu = tempProjection[1];
                    }
                    cellID = cellIDnew;
                    if (cellIDnew == oldCellID1 || cellID == oldCellID2) {
                        if (blockVerySlow) {
                            p.blocked = true;
                            p.blockVelocity = totalvelocity;
                        }
                        break;

                    }
                    //is in new
                    if (lengthfactor < 0.0001) {
                        if (shortLengthCounter > 3) {
                            break;
                        }
                        shortLengthCounter++;

                    } else {
                        shortLengthCounter = 0;
                    }
                    isprojecting = false;
                } else {
                    if (cellIDnew == -2) {
                        // goes over a trespassable boundary
                        p.setInactive();
//                        System.out.println("Particle "+p.getId()+" moved across the domain boundary and became inactive. from cell "+cellID);
                        return;
                    }
                    //PArticle tries to move over the edge into an undefined area
                    if (slidealongEdges && !isprojecting) {
                        double f;
                        if (bwindex == 0) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1], tempProjection);
                            tempPosLow = vertex1;
                            tempPosUp = vertex2;
                        } else if (bwindex == 1) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1], tempProjection);
                            tempPosLow = vertex2;
                            tempPosUp = vertex0;
                        } else if (bwindex == 2) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1], tempProjection);
                            tempPosLow = vertex0;
                            tempPosUp = vertex1;
                        } else {
                            System.err.println("wrong edge index " + bwindex);
                            f = 0;
                        }
                        if (f < 0) {
                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 2) % 3];
                            if (cellIDnew >= 0) {
                                //transfere a bit to the direction of the cell center
                                posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                            } else { //Stay inside this cell. move abit away from the edge
                                calculateVelocityPosition = false;
                                isprojecting = true;
//                                status = 2001;
                                posxneu = tempProjection[0];
                                posyneu = tempProjection[1];
                                if (Double.isNaN(posxneu)) {
                                    System.out.println("Position becomes NaN when sliding along edge to null neighbour");
                                }
                                continue;
                            }
                        } else if (f > 1) {
                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 1) % 3];
                            if (cellIDnew >= 0) {
                                //transfere a bit to the direction of the cell center
                                posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                            } else {
                                calculateVelocityPosition = false;
                                isprojecting = true;
//                                status = 2002;
                                posxneu = tempProjection[0];
                                posyneu = tempProjection[1];
                                continue;
                            }
                        } else {
                            //Stay inside this cell. move abit away from the edge
                            posxneu = tempProjection[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                            posyneu = tempProjection[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                            p.surfaceCellID = cellID;
                            p.setPosition3D(posxneu, posyneu);
                            return;
                        }
                        calculateVelocityPosition = false;
                        isprojecting = true;
                        continue;
                    } else {
                        temp_distance = totalvelocity * timeLeft;
                        lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                        lengthfactor *= 0.95;
                        posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                        posyneu = posyalt + (posyneu - posyalt) * lengthfactor;

                        if (surface.getMeasurementRaster().spatialConsistency) {
                            p.setPosition3D(posxneu, posyneu);
                            p.surfaceCellID = cellID;
                            surface.getMeasurementRaster().measureParticle(simulationtime, p, timeLeft, threadindex);
                        }
                        timeLeft -= (1. - lengthfactor);
                        break;
                    }
                }
            }
            posxalt = posxneu;
            posyalt = posyneu;
        }

        //Test if new position is inside the old cell
        moveToSurroundingCell(p, vertex0, vertex1, vertex2);

        p.setPosition3D(posxneu, posyneu);
        p.surfaceCellID = cellID;

        if (surface.getMeasurementRaster().spatialConsistency) {
            surface.getMeasurementRaster().measureParticle(simulationtime, p, timeLeft, threadindex);
        }
    }

    private void moveParticleCellIterative2(Particle p, float dt) {

        // get the particle velocity (most computation time used here)
        posxalt = p.getPosition3d().x;
        posyalt = p.getPosition3d().y;
        oldCellID1 = p.surfaceCellID;
        oldCellID2 = oldCellID1;
        startCellID = cellID;
        lengthfactor = 1;
        totalvelocity = 0;
        timeLeft = dt;
        wasInFreeflow = false;
        loopcounter = 0;
//        boolean 
        zigzag = false;
//        status = 0;
        calculateVelocityPosition = true;
        isprojecting = false;
        gradientFlowstateActual = false;
        while (timeLeft > 0) {
            loopcounter++;
            if (loopcounter > maxNumberOfIterationLoops) {
                if (verbose) {
                    System.out.println("exceeded max loops (" + loopcounter + ") for particle " + p.getId() + " in cell " + cellID + "  to Cell " + cellIDnew + " came from cell " + p.surfaceCellID + " V=" + totalvelocity + "\t time left:" + timeLeft + "\t status=" + status + "  lengthfactor=" + lengthfactor + " \tvstatus:" + vstatus + "  " + (isprojecting ? "projecting " : "") + (zigzag ? "zigzag" : ""));
                }
//                p.blocked = true;
                break;
            }

            if (calculateVelocityPosition) {
                if (!calcPrePosition(p, timeLeft, dt)) {
                    if (verbose) {
                        System.out.println("Particle " + p.getId() + " does not move.");
                    }
                    return;
                }

                if (gridFree) {
                    p.setPosition3D(posxneu, posyneu);
                    return;
                }
            }
            if (blockVerySlow) {
                if (p.blocked == true) {
                    if (Math.abs(totalvelocity - p.blockVelocity) < minimumDistanceBeforeBlock) {
//                    System.out.println("Particle " + p.getId() + " is blocked here");
                        if (surface.getMeasurementRaster().spatialConsistency) {
                            surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                        }
                        return;
                    } else {
                        p.blocked = false;
                        zigzag = false;
                    }
                } else {
                    if (!wasInFreeflow && !gradientFlowstateActual) {
                        wasInFreeflow = true;
                    }
                }
            } else if (p.blocked) {
                p.blocked = false;
            }
            calculateVelocityPosition = true;

            // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
            // da eine Veränderung durch Modellränder vorkommen kann
            node0 = surface.getTriangleNodes()[cellID][0];
            node1 = surface.getTriangleNodes()[cellID][1];
            node2 = surface.getTriangleNodes()[cellID][2];

            vertex0 = surface.getVerticesPosition()[node0];
            vertex1 = surface.getVerticesPosition()[node1];
            vertex2 = surface.getVerticesPosition()[node2];

            GeometryTools.fillBarycentricWeighting(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
            if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
                //Stays inside this cell
                p.surfaceCellID = cellID;
                if (allowWashToPipesystem) {
                    surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                    washToPipesystem(p, cellID);
                }
                break;
            }
            /**
             * The lengthfactor multiplied with the movement vector hits exactly
             * the edge of the cell.
             */
            lengthfactor = 1;
            bwindex = -1;
//            checkbwindex = -1;
//            outOfTriangleCounter = 0;

            if (true) {
                //New (more performant) version, where only one section is calculated.
                /**
                 * Only need to test those, where the barycentric weight is
                 * lowest
                 */
                if (temp_barycentricWeights[0] < temp_barycentricWeights[1]) {
                    if (temp_barycentricWeights[2] < temp_barycentricWeights[0]) {
                        bwindex = 2;
                    } else {
                        bwindex = 0;
                    }
                } else {
                    if (temp_barycentricWeights[2] < temp_barycentricWeights[1]) {
                        bwindex = 2;
                    } else {
                        bwindex = 1;
                    }
                }
                if (bwindex == 0) {
                    lengthfactor= GeometryTools.lineIntersectionS( posxalt, posyalt, posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1]);
                    
//                 double fraction0 = temp_barycentricWeights[2] / (temp_barycentricWeights[1] + temp_barycentricWeights[2]);
//                System.out.println("Weightindex: "+bwindex+"   "+st12[0]+"/"+st12[1]+" <- "+fraction0+"   Weights:"+temp_barycentricWeights[0]+","+temp_barycentricWeights[1]+","+temp_barycentricWeights[2]);
                } else if (bwindex == 1) {
                    lengthfactor =GeometryTools.lineIntersectionS( posxalt, posyalt, posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1]);
                    
                } else {
                    lengthfactor = GeometryTools.lineIntersectionS( posxalt, posyalt, posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1]);
                    
                }
            } else {
                //Old (not so high performance version, wehere all sections were calculated
                st01 = GeometryTools.lineIntersectionST(st01, posxalt, posyalt, posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1]);
                st12 = GeometryTools.lineIntersectionST(st12, posxalt, posyalt, posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1]);
                st20 = GeometryTools.lineIntersectionST(st20, posxalt, posyalt, posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1]);
                bwindex = 0;
                lengthfactor = 1;
                //if barycentric weight index 1 is negative, the partivle crossed edge opposite side 0-2 into neighbour no.1 and so on...
                if (st12[0] >= 0 && st12[0] <= 1) {
                    //Search for intersection between travl path and first edge
                    lengthfactor = st12[0];
                    bwindex = 0;
                }
                if (st20[0] >= 0 && st20[0] <= 1) {
                    if (lengthfactor > st20[0]) {
                        bwindex = 1;
                        lengthfactor = st20[0];
                    }
                }
                if (st01[0] >= 0 && st01[0] <= 1) {
                    if (lengthfactor > st01[0]) {
                        lengthfactor = st01[0];
                        bwindex = 2;
                    }
                }
            }
//            if (bwindex != checkbwindex) {
//                System.out.println(p.getId() + "  calc: " + bwindex + "  smallest: " + checkbwindex + "\t Weights:" + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
////           
//                bwindex = checkbwindex;
//                if (bwindex == 0) {
//                    st12 = GeometryTools.lineIntersectionST(st12, posxalt, posyalt, posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1]);
//                    lengthfactor = st12[0];
//                    double fraction0 = temp_barycentricWeights[2] / (temp_barycentricWeights[1] + temp_barycentricWeights[2]);
//                    System.out.println("Weightindex: " + bwindex + "   " + st12[0] + "/" + st12[1] + " <- " + fraction0 + "   Weights:" + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
//                } else if (bwindex == 1) {
//                    st20 = GeometryTools.lineIntersectionST(st20, posxalt, posyalt, posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1]);
//                    lengthfactor = st20[0];
//                } else {
//                    st01 = GeometryTools.lineIntersectionST(st01, posxalt, posyalt, posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1]);
//                    lengthfactor = st01[0];
//                }
//
//            }
            if (lengthfactor < 1 && bwindex < 0) {
                System.out.println("lengthfactor: " + lengthfactor + " bwindex:" + bwindex + "   Weights:" + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);

            }

            //Test if the barycnetric weight are enough to calculate the position
//            double fraction0 = temp_barycentricWeights[2] / (temp_barycentricWeights[1] + temp_barycentricWeights[2]);
//            double fraction1 = temp_barycentricWeights[0] / (temp_barycentricWeights[0] + temp_barycentricWeights[2]);
//            double fraction2 = temp_barycentricWeights[1] / (temp_barycentricWeights[1] + temp_barycentricWeights[0]);
//            if (bwindex == 0) {
//                System.out.println("BW Index 0: edgefactor: " + st12[1] + "   weights: " + fraction0 + " / " + fraction1 + " / " + fraction2 + " \tDiff: " + (fraction0 - st12[1]));
//            } else if (bwindex == 1) {
//                System.out.println("BW Index 1: edgefactor: " + st20[1] + "   weights: " + fraction0 + " / " + fraction1 + " / " + fraction2 + " \tDiff: " + (fraction1 - st20[1]));
//            } else /*if (bwindex == 2)*/ {
//                System.out.println("BW Index 2: edgefactor: " + st01[1] + "   weights: " + fraction0 + " / " + fraction1 + " / " + fraction2 + " \tDiff: " + (fraction2 - st01[1]));
//            }
            if (lengthfactor >= 1) {
                //No intersection. Particle can stay inside this cell.
                //But Barycentric weighting said, it is outside the cell.

                if (allowWashToPipesystem) {
                    if (washToPipesystem(p, cellID)) {
                        return;
                    }
                }
                // So we have to move it back towards the cell where it belongs.
                // will be done in the checking step at the end of this function
                break;
            } else {
                //The new cell can be easily found by checking the edgeindex, where the particle has left the cell.
                if (cellID < 0 || bwindex < 0) {
                    System.out.println("Cell" + cellID + "  lengthfactor: " + lengthfactor + " bwindex:" + bwindex + "   Weights:" + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
                }

                cellIDnew = surface.getNeighbours()[cellID][bwindex];//This only is correct, if the neighbours are constructed in the same order as the edges are defined. Otherwise comment in the section above.

//                      status=2;
                if (cellIDnew >= 0) {
                    if (meanVelocityAtZigZag) {
                        if ((cellIDnew == oldCellID1 || cellIDnew == oldCellID2)) {
                            //Come into zigzag condition
                            //will enter a cell where it already have been
                            // calculate the mean velocity + direction between the previous and the next cell
                            tempVelocity = surface.getTriangleVelocity(cellIDnew, surface.getActualTimeIndex());
                            particlevelocity[0] = (particlevelocity[0] + tempVelocity[0]) / 2.;
                            particlevelocity[1] = (particlevelocity[1] + tempVelocity[1]) / 2.;
                            calculateVelocityPosition = false;
                            isprojecting = true;

                            if (!zigzag || (loopcounter % 3 == 0)) {
                                cellID = cellIDnew;
                                //standard approach: particle goes into the desired cell
                            }//else: particle stays in thiscell
                            zigzag = true;
                            //Calculate position on edge
                            //move a bit more into the other cell
                            lengthfactor *= 1.01;
                            posxalt = posxalt * (1 - lengthfactor) + posxneu * lengthfactor;
                            posyalt = posyalt * (1 - lengthfactor) + posyneu * lengthfactor;

                            posxneu = posxalt + particlevelocity[0] * timeLeft;
                            posyneu = posyalt + particlevelocity[1] * timeLeft;
                            if (loopcounter > 5) {
                                posxalt = /*posxneu * 0.0 +*/ surface.getTriangleMids()[cellID][0] * 1.0;
                                posyalt = /*posyneu * 0.0 +*/ surface.getTriangleMids()[cellID][1] * 1.0;
                            }
                            lengthfactor = 1;
                            //This can be handled in the next loop
//                            status = 329;
                            continue;

                        } else {
                            if (zigzag) {
                                zigzag = false;
                            }
                        }
                    }
//                    status = 1;
//                    test for velocity in the new cell
                    if (preventEnteringDryCell && !gradientFlowstateActual) {
//                        status=11;
                        int timeIndex = surface.getTimes().getTimeIndex((long) (surface.getActualTime() + (dt - timeLeft) * 1000));
                        tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex);
                        float totalNeighbourVelocity = tempVelocity[0] * tempVelocity[0] + tempVelocity[1] * tempVelocity[1];
                        if (totalNeighbourVelocity == 0 || Math.sqrt(totalNeighbourVelocity) <= dryFlowVelocity/*tempVelocity[0] == 0 && tempVelocity[1] == 0*/) {
                            //Particle is about to enter a dry cell
//                            if (timeIndex < surface.getNumberOfTimestamps() - 1) {
//                                tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex + 1);
//                            }
//                            status=12;
                            if (!gradientFlowstateActual/*tempVelocity[0] == 0 && tempVelocity[1] == 0*/) {
                                //PArticle tries to move over the edge to a cell where it will get stuck
//                                status=13;
                                if (slidealongEdges) {
//                                    status=14;
                                    double f;
                                    //Detect the edge and project the a-priori position back onto the edge.
                                    if (bwindex == 0) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1], tempProjection);
                                        tempPosLow = vertex1;
                                        tempPosUp = vertex2;
                                    } else if (bwindex == 1) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1], tempProjection);
                                        tempPosLow = vertex2;
                                        tempPosUp = vertex0;
                                    } else if (bwindex == 2) {
                                        f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1], tempProjection);
                                        tempPosLow = vertex0;
                                        tempPosUp = vertex1;
                                    } else {
                                        System.err.println("wrong edge index " + bwindex);
                                        f = 0;
                                    }
                                    if (f < 0) {
                                        if (isprojecting) {
                                            //Both edges are inpermeable. Stay in the edge of this triangle
                                            posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                            posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                            if (blockVerySlow) {
                                                p.blocked = true;
                                                p.blockVelocity = totalvelocity;//testVelocity(particlevelocity);
                                            }
                                            return;
                                        } else {
                                            //Point is left of the edge
                                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 2) % 3];
                                            if (cellIDnew >= 0) {
                                                //transfere a bit to the direction of the new cell center
                                                posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                                posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];

                                            } else {
                                                //Shift would end in an empty neighbour cell
                                                calculateVelocityPosition = false;
                                                isprojecting = true;
//                                                status = 1001;
                                                posxneu = tempProjection[0];
                                                posyneu = tempProjection[1];

                                                //This can be handled in the next loop
                                                continue;
                                            }
                                        }
                                    } else if (f > 1) {
                                        if (isprojecting) {
                                            //Both edges are inpermeable. Stay in the edge of this triangle
                                            posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                            posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                            if (blockVerySlow) {
                                                p.blocked = true;
                                                p.blockVelocity = totalvelocity;//testVelocity(particlevelocity);
                                            }
                                            return;
                                        } else {
                                            //Point is right of the edge
                                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 1) % 3];

                                            if (cellIDnew >= 0) {
                                                //transfere a bit to the direction of the new cell center
                                                posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                                posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                                            } else {
                                                calculateVelocityPosition = false;
                                                isprojecting = true;
//                                                status = 1002;
                                                posxneu = tempProjection[0];
                                                posyneu = tempProjection[1];
                                                continue;
                                            }
                                        }
                                    } else {
                                        //Point is on the edge between the nodes
                                        //Stay inside this cell. move abit away from the edge
                                        posxneu = tempProjection[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                                        posyneu = tempProjection[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                                        p.surfaceCellID = cellID;
                                        p.setPosition3D(posxneu, posyneu);
                                        if (blockVerySlow) {
                                            p.blocked = true;
                                            p.blockVelocity = totalvelocity;//testVelocity(particlevelocity);
                                        }
                                        return;
                                    }

                                    calculateVelocityPosition = false;
                                    isprojecting = true;
//                                    status = 100;
                                    continue;
                                } else {
//                                        System.out.println("Stop at edge loop " + loopcounter + "   lengthfactor:" + lengthfactor + "  timeleft:" + timeLeft);
                                    // Stop here at the edge, because we cannot go into the target cell.
                                    temp_distance = totalvelocity * timeLeft;
                                    lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                                    lengthfactor *= 0.99;
                                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                                    if (surface.getMeasurementRaster().spatialConsistency) {
                                        p.setPosition3D(posxneu, posyneu);
                                        p.surfaceCellID = cellID;
                                        surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                                    }
                                    timeLeft -= (1. - lengthfactor);
                                    break;
                                }
                            } else {
                                //we are already coming from a dry state flow
                                // Stop here at the edge, because we cannot go into the target cell.
                                temp_distance = totalvelocity * timeLeft;
                                lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                                lengthfactor *= 0.99;
                                posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                                posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                                if (surface.getMeasurementRaster().spatialConsistency) {
                                    p.setPosition3D(posxneu, posyneu);
                                    p.surfaceCellID = cellID;
                                    surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                                }
                                timeLeft -= (1. - lengthfactor);
                                break;
                            }
                        }
                    }

                    if (!isprojecting) {
                        lengthfactor *= 1.01;//Make sure the particle is in the new cell
                    }
                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;

                    if (surface.getMeasurementRaster().spatialConsistency) {
                        p.setPosition3D(posxneu, posyneu);
                        p.surfaceCellID = cellID;
                        surface.getMeasurementRaster().measureParticle(simulationtime, p, lengthfactor * timeLeft, threadindex);
                    }
                    timeLeft *= (1. - lengthfactor);
                    if (allowWashToPipesystem) {
                        if (washToPipesystem(p, cellIDnew)) {
                            return;
                        }
                    }

                    //test if new cell contains position
                    node0 = surface.getTriangleNodes()[cellIDnew][0];
                    node1 = surface.getTriangleNodes()[cellIDnew][1];
                    node2 = surface.getTriangleNodes()[cellIDnew][2];

                    vertex0 = surface.getVerticesPosition()[node0];
                    vertex1 = surface.getVerticesPosition()[node1];
                    vertex2 = surface.getVerticesPosition()[node2];
                    GeometryTools.fillBarycentricWeighting(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                    if (temp_barycentricWeights[0] < 0 || temp_barycentricWeights[1] < 0 || temp_barycentricWeights[2] < 0) {
                        validateBarycentricWeights(temp_barycentricWeights, temp_barycentricWeights, 0.01);
                        GeometryTools.calcPositionFromBarycentric(tempProjection, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], temp_barycentricWeights[0], temp_barycentricWeights[1], temp_barycentricWeights[2]);
                        posxneu = tempProjection[0];
                        posyneu = tempProjection[1];
                    }
                    cellID = cellIDnew;
                    isprojecting = false;
                    //is in new
                    if (lengthfactor < 0.0001) {
                        if (cellID == oldCellID1 || cellID == oldCellID2) {
                            tempVelocity = surface.getTriangleVelocity(cellIDnew, surface.getActualTimeIndex());
                            particlevelocity[0] = tempVelocity[0];
                            particlevelocity[1] = tempVelocity[1];
                            break;
                        }
                        if (shortLengthCounter > 3) {
                            break;
                        }
                        shortLengthCounter++;
                    } else {
                        shortLengthCounter = 0;
                    }

                } else {
                    //Goes into cell id <0
                    if (cellIDnew == -2) {
                        // goes over a trespassable boundary
                        p.setLeftSimulation();
//                        System.out.println("Particle "+p.getId()+" moved across the domain boundary and became inactive. from cell "+cellID);
                        return;
                    }
                    //PArticle tries to move over the edge into an undefined area
                    if (slidealongEdges && !isprojecting) {
                        double f;
//                        status = 120;
                        if (bwindex == 0) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1], tempProjection);
                            tempPosLow = vertex1;
                            tempPosUp = vertex2;
                        } else if (bwindex == 1) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1], tempProjection);
                            tempPosLow = vertex2;
                            tempPosUp = vertex0;
                        } else if (bwindex == 2) {
                            f = GeometryTools.projectPointToLine(posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1], tempProjection);
                            tempPosLow = vertex0;
                            tempPosUp = vertex1;
                        } else {
                            System.err.println("wrong edge index " + bwindex);
                            f = 0;
                        }
                        if (f < 0) {
                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 2) % 3];

                            if (cellIDnew >= 0) {
                                //transfere a bit to the direction of the cell center
                                posxneu = tempPosLow[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                posyneu = tempPosLow[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                            } else { //Stay inside this cell. move abit away from the edge
                                calculateVelocityPosition = false;
                                isprojecting = true;
//                                status = 2001;
                                posxneu = tempProjection[0];
                                posyneu = tempProjection[1];
                                if (Double.isNaN(posxneu)) {
                                    System.out.println("Position becomes NaN when sliding along edge to null neighbour");
                                }
                                continue;
                            }
                        } else if (f > 1) {
                            cellIDnew = surface.getNeighbours()[cellID][(bwindex + 1) % 3];
                            if (cellIDnew >= 0) {
                                //transfere a bit to the direction of the cell center
                                posxneu = tempPosUp[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][0];
                                posyneu = tempPosUp[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellIDnew][1];
                            } else {
                                calculateVelocityPosition = false;
                                isprojecting = true;
//                                status = 2002;
                                posxneu = tempProjection[0];
                                posyneu = tempProjection[1];
                                continue;
                            }
                        } else {
                            //Stay inside this cell. move abit away from the edge
                            posxneu = tempProjection[0] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][0];
                            posyneu = tempProjection[1] * 0.99 + 0.01 * surface.getTriangleMids()[cellID][1];
                            if (Double.isNaN(posxneu)) {
                                System.out.println("2Set Position to NaN length:" + lengthfactor + "  posAlt:" + posxalt + " neu: " + posxneu);
                            }
                            p.surfaceCellID = cellID;
                            p.setPosition3D(posxneu, posyneu);
                            break;
                        }
                        calculateVelocityPosition = false;
                        isprojecting = true;
                        continue;
                    } else {
//                        status = 123;
                        temp_distance = totalvelocity * timeLeft;
                        lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                        lengthfactor *= 0.95;
                        posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                        posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
//                        if (cellID == trackid) {
//                            System.out.println("PARTICLE " + p.getId() + " loop: " + loopcounter + " 11 " + (slidealongEdges ? "slide" : "") + (isprojecting ? "projecting" : ""));
//                        }

                        if (Double.isNaN(posxneu)) {
                            System.out.println("1Set Position to NaN length:" + lengthfactor + "  posAlt:" + posxalt + " neu: " + posxneu);
                        }
                        if (surface.getMeasurementRaster().spatialConsistency) {
                            p.setPosition3D(posxneu, posyneu);
                            p.surfaceCellID = cellID;
                            surface.getMeasurementRaster().measureParticle(simulationtime, p, timeLeft, threadindex);
                        }
                        if (gradientFlowstateActual) {
                            p.blocked = true;
                            p.blockVelocity = totalvelocity;
                        }
//                        p.blockXdir = particlevelocity[0];//.setVelocity1d(totalvelocity);
                        timeLeft -= (1. - lengthfactor);
                        break;
                    }
                }
            }
            p.addMovingLength(Math.abs(posxneu-posxalt)+Math.abs(posyneu-posyalt));
            posxalt = posxneu;
            posyalt = posyneu;
            oldCellID2 = oldCellID1;
            oldCellID1 = cellID;
//            status=1;
        }

        //Test if new position is inside the old cell
        moveToSurroundingCell(p, vertex0, vertex1, vertex2);

        if (Double.isNaN(posxneu)) {
            System.out.println("0Set Position to NaN of particle " + p.getId() + " in cell " + cellID + "   vx:" + particlevelocity[0] + ", vy=" + particlevelocity[1] + " loop:" + loopcounter + " length:" + lengthfactor + " timeleft:" + timeLeft);
            posxneu = surface.getTriangleMids()[cellID][0];
            posyneu = surface.getTriangleMids()[cellID][1];
        }
        p.setPosition3D(posxneu, posyneu);
        if (blockVerySlow && gradientFlowstateActual) {
            if (!wasInFreeflow) {//If the particle is almost immobile, skip the continuous calculation and park it in blocking state
                if (startCellID == cellID) {
                    if (Math.abs(posxneu - posxalt) + Math.abs(posyalt - posyneu) < minimumDistanceBeforeBlock) {
                        p.blocked = true;
                        p.blockVelocity = testVelocity(particlevelocity);
//                    p.blockVelocity = totalvelocity;
                    }
                }
            }
        }
        p.surfaceCellID = cellID;
        if (surface.getMeasurementRaster().spatialConsistency) {
            surface.getMeasurementRaster().measureParticle(simulationtime, p, timeLeft, threadindex);
        }

    }

    public void moveToSurroundingCell(Particle p, double[] vertex0, double[] vertex1, double[] vertex2) {
        GeometryTools.fillBarycentricWeighting(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
        if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
            validateBarycentricWeights(temp_barycentricWeightsOld, temp_barycentricWeights, 0.01);
            GeometryTools.calcPositionFromBarycentric(tempProjection, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], temp_barycentricWeights[0], temp_barycentricWeights[1], temp_barycentricWeights[2]);
            posxneu = tempProjection[0];
            posyneu = tempProjection[1];
//            if (false) {
//            posxneu = posxneu * 0.8 + surface.getTriangleMids()[cellID][0] * 0.2;
//            posyneu = posyneu * 0.8 + surface.getTriangleMids()[cellID][1] * 0.2;
//                GeometryTools.fillBarycentricWeighting(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
//                if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
//                    posxneu = posxneu * 0.5 + surface.getTriangleMids()[cellID][0] * 0.5;
//                    posyneu = posyneu * 0.5 + surface.getTriangleMids()[cellID][1] * 0.5;
//                    if (verbose) {
//                        System.out.println("loop:" + loopcounter + " will not be in " + temp_barycentricWeightsOld[0] + ", " + temp_barycentricWeightsOld[1] + ", " + temp_barycentricWeightsOld[2] + "  " + (zigzag ? "Zigzag" : ""));
//                    }
//                    GeometryTools.fillBarycentricWeighting(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
//                    if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
//                        if (verbose) {
//                            System.out.println("Particle " + p.getId() + " loop:" + loopcounter + " will still not be in cell:" + cellIDnew + "  " + temp_barycentricWeightsOld[0] + ", " + temp_barycentricWeightsOld[1] + ", " + temp_barycentricWeightsOld[2] + "  " + (zigzag ? "Zigzag" : ""));
//                        }
//                        posxneu = surface.getTriangleMids()[cellID][0];
//                        posyneu = surface.getTriangleMids()[cellID][1];
//                    }
//                }
//            }
        }
    }

    public void checkSurrounding(Particle p) {

        if (p.getPosition3d() != null && p.surfaceCellID >= 0) {
            //Everything ok.
            return;
        } else {
            int triangleID = p.surfaceCellID;
            Coordinate pos = p.getPosition3d();
            System.err.println("particle lost on surface");
            if (p.getSurrounding_actual() instanceof Surface) {
                p.setOnSurface();
                if (p.surfaceCellID >= 0) {
                    double[] xy = surface.getTriangleMids()[p.surfaceCellID];
                    pos = new Coordinate(xy[0], xy[1], xy[2]);
                } else {
                    System.err.println("no information of surface ID for Particle " + p);
                    return;
                }
            } else if (p.getSurrounding_actual() instanceof SurfaceTriangle) {
//                p.setOnSurface();
                SurfaceTriangle st = (SurfaceTriangle) p.getSurrounding_actual();
                triangleID = (int) st.getManualID();
                if (pos == null) {
                    try {
                        Coordinate utm = surface.getGeotools().toUTM(st.getPosition3D(0).lonLatCoordinate(), false);
                        pos = new Coordinate(utm.x, utm.y, st.getPosition3D(0).z);

                    } catch (TransformException ex) {
                        Logger.getLogger(ParticleSurfaceComputing2D.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if ((p.getSurrounding_actual() instanceof SurfaceTrianglePath)) {
                System.out.println("particle is on 1d surface path ... something is wrong!");
                return;
            } else if (p.getSurrounding_actual() instanceof Manhole) {
                Manhole mh = (Manhole) p.getSurrounding_actual();
                triangleID = (int) mh.getSurfaceTriangleID();
                if (pos == null) {
                    try {
                        Coordinate utm = surface.getGeotools().toUTM(mh.getPosition().lonLatCoordinate(), false);
                        System.out.println("Converted manhole to " + utm);
                        pos = new Coordinate(utm.x, utm.y, mh.getSurface_height());

                    } catch (TransformException ex) {
                        Logger.getLogger(ParticleSurfaceComputing2D.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                System.out.println("Particle " + p + " is in unknown Capacity " + p.getSurrounding_actual());
            }
            p.setPosition3D(pos);
            p.setSurrounding_actual(surface);
            p.surfaceCellID = triangleID;
        }
        if (p.surfaceCellID < 0) {
            System.out.println("Problem with particle on surface (surfacecell:" + p.surfaceCellID + ", onsurface:" + p.isOnSurface());
        }
    }

    public double testVelocity(double[] particlevelocity) {
//        if (particlevelocity[0] == 0 && particlevelocity[1] == 0) {
//            return 0;
//        }
//        double u = Math.abs(particlevelocity[0]) + Math.abs(particlevelocity[1]);
        totalvelocity = Math.sqrt((particlevelocity[0] * particlevelocity[0]) + (particlevelocity[1] * particlevelocity[1]));
        if (totalvelocity > maxVelocity) {
            //Normalise velocity to maximum posible 5 m/s;
            double factor = maxVelocity / totalvelocity;
            particlevelocity[0] *= factor;
            particlevelocity[1] *= factor;
            totalvelocity = maxVelocity;
//            System.out.println("velocity (" + u + ")is implausible. set to +/- 3m/s [" + particlevelocity[0] + ", " + particlevelocity[1] + "] = " + Math.sqrt((particlevelocity[0] * particlevelocity[0]) + (particlevelocity[1] * particlevelocity[1])));
//            double veloverhaeltnis = particlevelocity[0] / particlevelocity[1];
//            if (veloverhaeltnis < 1) {
//                particlevelocity[1] = u * Math.signum(particlevelocity[1]);
//                particlevelocity[0] = u * veloverhaeltnis * Math.signum(particlevelocity[0]);
//            } else {
//                particlevelocity[0] = u * Math.signum(particlevelocity[0]);;
//                particlevelocity[1] = u * (1. / veloverhaeltnis) * Math.signum(particlevelocity[1]);
//            }

        }
        return totalvelocity;
    }

    /**
     * Returns true if the particle is washed to the pipe system and is now
     * waiting for the next loop to start. Then it will be treated as a
     * pipe-particle.
     *
     * @param p
     * @param triangleID
     * @return washed to pipe system successful
     */
    public boolean washToPipesystem(Particle p, int triangleID) {
        //Check if particle can go back to pipe system.
        Inlet inlet = surface.getInlet(triangleID);
        if (inlet != null) {
            // Check if Inlet is flooded
            if (inlet.getNetworkCapacity() != null) {
                double fillrate = inlet.getNetworkCapacity().getProfile().getFillRate(inlet.getNetworkCapacity().getWaterlevel());
                if (fillrate < 0.9) {
                    //Pipe is not flooded. Particles can enter pipenetwork here.
                    p.setSurrounding_actual(inlet.getNetworkCapacity());
                    p.setPosition1d_actual(inlet.getPipeposition1d());
                    p.setInPipenetwork();
                    p.toPipenetwork = inlet.getNetworkCapacity();
                    //Create Shortcut
                    if (p.toSurface != null) {
                        surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangleID(), inlet, null, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
                    }
                    return true;
                }
            } else {
                System.out.println("Inlet " + inlet.toString() + " has no pipe.");
            }
        }
        Manhole m = surface.getManhole(triangleID);
        if (m != null) {
            if (m.getStatusTimeLine().getActualFlowToSurface() <= 0) {
                //Water can flow back into pipe network
                p.setSurrounding_actual(m);
                p.setPosition1d_actual(0);
                p.setInPipenetwork();
                p.toPipenetwork = m;

                if (p.toSurface != null) {
                    surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangleID(), null, m, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
                }
                return true;
            }
        }
        return false;
    }

    public double nextRandomGaussian() {
        return random.nextGaussian();
    }

//    public String getDiffusionString() {
//        return D.getDiffusionString();
//    }
    @Override
    public void setDeltaTimestep(double seconds) {
        this.dt = (float) seconds;

//        if (randomizeAfterSeconds < dt) {
//            this.sqrt2dt = (float) Math.sqrt(2 * randomizeAfterSeconds);
//        } else {
        this.sqrt2dt = (float) Math.sqrt(2 * dt);
        minimumDistanceBeforeBlock = (float) (dryFlowVelocity * dt * 0.0001);
//        }
    }

    @Override
    public Surface getSurface() {
        return this.surface;
    }

    /**
     *
     * @return
     */
    @Override
    public String reportCalculationStatus() {
        if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
            SurfaceMeasurementTriangleRaster smtr = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();

            if (smtr.statuse != null && smtr.monitor != null && smtr.monitor[threadindex] != null) {
                return "Status:" + status + "  Raster:" + surface.getMeasurementRaster().statuse[threadindex] + " monitor: " + smtr.monitor[threadindex] + "   mass: " + smtr.monitor[threadindex].totalParticleCount() + "  isused:" + smtr.monitor[threadindex].lock.toString();
            }
        }
        return "Status:" + status;

    }

    @Override
    public void setRandomNumberGenerator(RandomGenerator rd) {
        this.random = rd;
    }

    @Override
    public void setActualSimulationTime(long timeMS) {
        this.setSimulationtime(timeMS);
    }

    /**
     * Corrects the barycentric weights, that are below 0 and above 1, Sum will
     * be 1.
     *
     * @param original invalid weights
     * @param corrected corrected weights
     * @param minimum minimum allowed value (applied for values <0
     */
    public static void validateBarycentricWeights(double[] original, double[] corrected, double minimum) {
        double sum = 0;
        for (int i = 0; i < 3; i++) {
            if (original[i] < minimum) {
                corrected[i] = minimum;
                sum += minimum;
            } else if (original[i] > (1 - minimum)) {
                corrected[i] = (1 - minimum);
                sum += corrected[i];
            } else {
                corrected[i] = original[i];
                sum += corrected[i];
            }
        }
        sum = 1. / sum;//is now the multiplication factor
//        double test = 0;
        for (int i = 0; i < 3; i++) {
            corrected[i] *= sum;
//            test += corrected[i];

        }
//        if (Math.abs(test - 1) > 0.001) {
//            System.out.println("testsum=" + test + " in " + ParticleSurfaceComputing2D.class 
//+ ":validateBarycentricWeights");
//        }
    }

}
