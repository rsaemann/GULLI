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

import com.vividsolutions.jts.geom.Coordinate;
import com.saemann.gulli.core.control.maths.GeometryTools;
import com.saemann.gulli.core.control.maths.RandomGenerator;
import com.saemann.gulli.core.control.threads.ThreadController;
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
    protected DiffusionCalculator2D D = new DiffusionCalculator2D();
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
    public static double dryFlowVelocity = 0.005;

    public static double dryWaterlevel = 0.005;

    public static int maxNumberOfIterationLoops = 1000;

    /**
     * When active particles can go to the pipe system through inlets and
     * manholes.
     */
    public static boolean allowWashToPipesystem = true;

    /**
     * If true, the particle will not enter cells, where the absolute velocity
     * (in x) is 0.
     */
    public static boolean preventEnteringDryCell = true;

    /**
     * If no velocity is set, the hill slope is used for direction and 0.01 m/s
     * are applied.
     */
    public static boolean gradientFlowForDryCells = false;
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
    public static boolean slidealongEdges = false;
//    /**
//     * if false(default), the random variable for the random walk is only
//     * generated at the begin of the particle step and is kept if multiple cells
//     * are visited. if true , the random number is generated new on every
//     * visited cell during the timestep.
//     */
//    public static boolean multiTimeRandomisation = false;

    public static int numberOfErrors = 0;

    /**
     * Create new random numbers for the random walk step every X seconds
     */
    public static float randomizeAfterSeconds = Float.POSITIVE_INFINITY;

    /////Arrays to be filled from other functions. so no extra allocation is needed.
    private double[] particlevelocity = new double[2];
    private final double[] temp_barycentricWeights = new double[3];
    private final double[] temp_barycentricWeightsOld = new double[3];
    private final double[][] tempVertices = new double[3][3];
    private double[] tempDiff = new double[2];
    private double[] tempProjection = new double[2];
    private float[] tempVelocity = new float[2];

    private double[] tempPosLow = new double[3], tempPosUp = new double[3];

    private double posxalt, posyalt, posxneu, posyneu, totalvelocity;
    private float timeLeft;

    private boolean shouldReRandomize = true;

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
    private int cellIDnew;

    private double dx, dy;
    private int vstatus = 0;
    private int shortLengthCounter = 0;

    private boolean calculateVelocityPosition = true;
    private boolean isprojecting = false;

    public ParticleSurfaceComputing2D(Surface surface, int threadIndex) {
        this.surface = surface;
        this.threadindex = threadIndex;
    }

    public void setDiffusionCalculation(DiffusionCalculator2D D) {
        this.D = D;
    }

    public DiffusionCalculator2D getDiffusionCalculator() {
        return D;
    }

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

    public void getDiffusionStatus() {
        if (enableDiffusion) {
            System.out.println("2D surface dispersion = " + enableDiffusion + "_" + getDiffusionString());
        } else {
            System.out.println("2D surface dispersion = " + enableDiffusion);
        }
    }

    public void setSimulationtime(long simulationtime) {
        this.simulationtime = simulationtime;
    }

    /**
     * Called from Threadcontroller for each Particle. Delegates to the actual
     * selected transport function.
     *
     * @param p
     */
    @Override
    public void moveParticle(Particle p) {
        checkSurrounding(p);
        shortLengthCounter = 0;
//        if (randomizeAfterSeconds < dt) {
//            int numberOfloops = (int) (dt / randomizeAfterSeconds);
//            cellID = p.surfaceCellID;
//            for (int i = 0; i < numberOfloops; i++) {
//                shouldReRandomize = true;
//                moveParticleCellIterative(p, randomizeAfterSeconds);
//            }
//        } else {
        cellID = p.surfaceCellID;
        shouldReRandomize = true;
        moveParticleCellIterative(p, dt);
//        }
        if (p.isOnSurface()) {
            surface.getMeasurementRaster().measureParticle(simulationtime, p, threadindex);
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
//        if (useSubdividedTimestamps) {
//            double timeIndexD = surface.getTimeIndexDouble((long) (surface.getActualTime() + (dt - timeLeft) * 1000));
//            surface.getTriangleVelocity(cellID, timeIndexD, particlevelocity);//triangleID, timeIndexInt, (float) timeFrac, toFillSurfaceVelocity[0][0]);           
//        } else {
        particlevelocity = surface.getParticleVelocity2D(p, cellID, particlevelocity, temp_barycentricWeights);
//        }

        totalvelocity = testVelocity(particlevelocity);

        if (enableDiffusion) {
            // calculate with diffusion 
            if (totalvelocity < dryFlowVelocity) {
                if (gradientFlowForDryCells) {
                    gradientFlowstateActual = true;
                    totalvelocity = dryFlowVelocity;
                    particlevelocity[0] = surface.getTriangle_downhilldirection()[cellID][0] * totalvelocity;
                    particlevelocity[1] = surface.getTriangle_downhilldirection()[cellID][1] * totalvelocity;
                    posxneu = (posxalt + particlevelocity[0] * timeLeft);// only advection
                    posyneu = (posyalt + particlevelocity[1] * timeLeft);// only advection
                }

                p.setDrySurfaceMovement(true);
            } else {
                gradientFlowstateActual = false;

                p.setDrySurfaceMovement(false);
//
//            if (totalvelocity > 0.001) {

                //Optimized version already gives the squarerooted values. (To avoid squareroot operations [very slow]
                tempDiff = D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);

                sqrt2dtDx = sqrt2dt * tempDiff[0];
                sqrt2dtDy = sqrt2dt * tempDiff[1];

                if (shouldReRandomize) {
                    z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
                    z1 = nextRandomGaussian();
                    shouldReRandomize = false;
                }

                // random walk step
                // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
                dx = (particlevelocity[0] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[1] / totalvelocity) * z2 * sqrt2dtDy);

                dy = (particlevelocity[1] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[0] / totalvelocity) * z2 * sqrt2dtDy);

                posxneu = (posxalt + (particlevelocity[0] + dx / dt) * timeLeft);
                posyneu = (posyalt + (particlevelocity[1] + dy / dt) * timeLeft);

            }
        } else {
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
            vstatus = 6;
        }

        return true;
    }

    private void moveParticleCellIterative(Particle p, float dt) {

        // get the particle velocity (most computation time used here)
        posxalt = p.getPosition3d().x;
        posyalt = p.getPosition3d().y;
        timeLeft = dt;

        loopcounter = 0;
        status = 0;
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

            GeometryTools.fillBarycentricWeighing(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
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
                GeometryTools.fillBarycentricWeighing(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
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
                status = 30;
                break;
            } else {
                //this is the edge to the new cell no.?
//                 cellIDnew = -1;
//                for (int i_nb = 0; i_nb < 3; i_nb++) {
//                    int nbcell = surface.getNeighbours()[cellID][i_nb];
//                    if (nbcell < 0) {
//                        //This is a no-flow boundary. Particle cannot go there
//                        continue;
//                    }
//                    int[] nbnodes = surface.getTriangleNodes()[nbcell];
//
//                    int testNode1 = -1;
//                    int testNode2 = -1;
//                    if (bwindex == 0) {
//                        testNode1 = node1;
//                        testNode2 = node2;
//                    } else if (bwindex == 1) {
//                        testNode1 = node0;
//                        testNode2 = node2;
//                    } else if (bwindex == 2) {
//                        testNode1 = node0;
//                        testNode2 = node1;
//                    } else {
//                        System.out.println("BWNodeindex is out of range");
//                    }
//
//                    int shareNodeCounter = 0;
//                    for (int i = 0; i < 3; i++) {
//                        if (nbnodes[i] == testNode1) {
//                            shareNodeCounter++;
//                        } else if (nbnodes[i] == testNode2) {
//                            shareNodeCounter++;
//                        }
//                    }
//                    if (shareNodeCounter > 1) {
//                        cellIDnew = nbcell;
//                        System.out.println("BW index " + bwindex + " -> Nb Index " + i_nb);
//                        break;
//                    }
//                }

                //The new cell can be easily found by checking the edgeindex, where the particle has left the cell.
                cellIDnew = surface.getNeighbours()[cellID][bwindex];//This only is correct, if the neighbours are constructed in the same order as the edges are defined. Otherwise comment in the section above.
status=2;
                if (cellIDnew >= 0) {
                    status = 1;
//                    test for velocity in this cell
                    if (preventEnteringDryCell && !gradientFlowstateActual) {
                        status=11;
                        int timeIndex = surface.getTimeIndex((long) (surface.getActualTime() + (dt - timeLeft) * 1000));
                        tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex);
                        if (tempVelocity[0] == 0 && tempVelocity[1] == 0) {
                            if (timeIndex < surface.getNumberOfTimestamps() - 1) {
                                tempVelocity = surface.getTriangleVelocity(cellIDnew, timeIndex + 1);
                            }
                            status=12;
                            if (tempVelocity[0] == 0 && tempVelocity[1] == 0) {
                                //PArticle tries to move over the edge to a cell where it will get stuck
                                status=13;
                                if (slidealongEdges) {
                                    status=14;
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
                                                status = 1001;
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
                                                status = 1002;
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
//                                        if (verbose) {
//                                            System.out.println("Stay here, move a bit from the edge");
//                                        }
                                        return;
                                    }

                                    calculateVelocityPosition = false;
                                    isprojecting = true;
                                    status = 100;
                                    continue;
                                } else {
                                    // Stop here at the edge, because we cannot go into the target cell.
                                    temp_distance = totalvelocity * timeLeft;//Math.sqrt((posxalt - posxneu) * (posxalt - posxneu) + (posyalt - posyneu) * (posyalt - posyneu));

                                    lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                                    lengthfactor *= 0.95;
                                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                                    timeLeft -= (1. - lengthfactor);
                                    status = 11;
//                                    if (verbose) {
//                                        System.out.println("Stay here, keep distance from the edge");
//                                    }
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
                    timeLeft *= (1. - lengthfactor);
//                    cellID = cellIDnew;
//                    status = 2;
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
                    GeometryTools.fillBarycentricWeighing(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                    if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
                        cellID = cellIDnew;
                        //is in new
                        if (lengthfactor < 0.0001) {
                            if (shortLengthCounter > 3) {
                               
                                if (verbose) {
                                    System.out.println(">>>" + p.getId() + " very short length moved. leftover time: " + timeLeft + "s to new Cell " + cellIDnew+" target v="+tempVelocity[0]+","+tempVelocity[1]+" iteration:"+loopcounter+" actual v:"+totalvelocity+" status:"+status+"  length");
                                } 
                                status = 25;
                                break;
                            }
                            shortLengthCounter++;

                        } else {
                            shortLengthCounter = 0;
                        }
                        isprojecting = false;
                        status = 3;
                    } else {
                        shortLengthCounter = 0;//false;
//                        System.out.println("is not yet in new cell");
                        //SOmetimes the particles jump more than the possible length inside a triangle, because only on edge is considered (e.g. at sharp angle corners. -> need to transfer the particle back into the triangle.
//                        System.out.println("out of target cell: " + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
                        if (temp_barycentricWeights[0] > -0.1 && temp_barycentricWeights[1] > -0.1 & temp_barycentricWeights[2] > -0.1) {
                            //Particle is very close to this cell, but not yet inside.
                            //give it a small jump towards the center
                            posxneu = posxneu * 0.95 + surface.getTriangleMids()[cellIDnew][0] * 0.05;
                            posyneu = posyneu * 0.95 + surface.getTriangleMids()[cellIDnew][1] * 0.05;
                            GeometryTools.fillBarycentricWeighing(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                            if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
//                                System.out.println("slightly pushed towards new center");
                                cellID = cellIDnew;
                                status = 4;
                            } else {
                                if (verbose) {
                                    System.out.println("roughly pushed to new center " + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2]);
                                }
                                posxneu = posxneu * 0.8 + surface.getTriangleMids()[cellIDnew][0] * 0.2;
                                posyneu = posyneu * 0.8 + surface.getTriangleMids()[cellIDnew][1] * 0.2;
                                status = 5;
                                cellID = cellIDnew;
                            }
                        } else {
                            if (verbose) {
                                System.out.println("totally out of target. set to cell center " + temp_barycentricWeights[0] + "," + temp_barycentricWeights[1] + "," + temp_barycentricWeights[2] + "  was peojected?" + isprojecting);
                            }
                            cellID = cellIDnew;
                            //Particle is completely outside of this cell. Transfer it to the cell center.
//                            System.out.println(loopcounter + ", p" + p.getId() + " is not in new one " + df.format(bwnew[0]) + ", " + df.format(bwnew[1]) + ", " + df.format(bwnew[2]));
//                            posxneu = surface.getTriangleMids()[cellID][0];
//                            posyneu = surface.getTriangleMids()[cellID][1];
                            status = 6;
                            break;
                        }
                    }

                } else {
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
                                status = 2001;
                                posxneu = tempProjection[0];
                                posyneu = tempProjection[1];
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
                                status = 2002;
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
                        status = 123;
                        continue;
                    } else {
                        temp_distance = totalvelocity * timeLeft;//Math.sqrt((posxalt - posxneu) * (posxalt - posxneu) + (posyalt - posyneu) * (posyalt - posyneu));

                        lengthfactor -= 0.01 / temp_distance; //always have a distance of 1cm from the boundary;
                        lengthfactor *= 0.95;
                        posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                        posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                        timeLeft -= (1. - lengthfactor);
                        status = 15;
                        break;
                    }
                }
            }
            posxalt = posxneu;
            posyalt = posyneu;
        }

        //Test if new position is inside the old cell
        GeometryTools.fillBarycentricWeighing(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
        if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
            posxneu = posxneu * 0.8 + surface.getTriangleMids()[cellID][0] * 0.2;
            posyneu = posyneu * 0.8 + surface.getTriangleMids()[cellID][1] * 0.2;
            GeometryTools.fillBarycentricWeighing(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
            if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
                posxneu = posxneu * 0.5 + surface.getTriangleMids()[cellID][0] * 0.5;
                posyneu = posyneu * 0.5 + surface.getTriangleMids()[cellID][1] * 0.5;
                GeometryTools.fillBarycentricWeighing(temp_barycentricWeightsOld, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                if (temp_barycentricWeightsOld[0] < 0 || temp_barycentricWeightsOld[1] < 0 || temp_barycentricWeightsOld[2] < 0) {
                    if (verbose) {
                        System.out.println(loopcounter + "  will still not be in " + temp_barycentricWeightsOld[0] + ", " + temp_barycentricWeightsOld[1] + ", " + temp_barycentricWeightsOld[2]);
                    }
                    posxneu = surface.getTriangleMids()[cellID][0];
                    posyneu = surface.getTriangleMids()[cellID][1];
                }
            }
        }

        p.setPosition3D(posxneu, posyneu);
        p.surfaceCellID = cellID;
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
                } //                else if (p.getInjectionCellID() >= 0) {
                //                    p.surfaceCellID = p.getInjectionCellID();
                //                    System.out.println("Had to set surfac eCell id to injection for particle " + p);
                //                    double[] xy = surface.getTriangleMids()[p.surfaceCellID];
                //                    pos = new Coordinate(xy[0], xy[1], xy[2]);
                //                } 
                else {
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
     * @return
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

    public String getDiffusionString() {
        return D.getDiffusionString();
    }

    @Override
    public void setDeltaTimestep(double seconds) {
        this.dt = (float) seconds;

        if (randomizeAfterSeconds < dt) {
            this.sqrt2dt = (float) Math.sqrt(2 * randomizeAfterSeconds);
        } else {
            this.sqrt2dt = (float) Math.sqrt(2 * dt);
        }
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

}
