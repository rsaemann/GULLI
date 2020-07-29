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

import com.vividsolutions.jts.geom.Coordinate;
import control.maths.GeometryTools;
import control.maths.RandomArray;
import control.threads.ThreadController;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.Particle;
import model.surface.Surface;
import model.surface.SurfaceTriangle;
import model.surface.SurfaceTrianglePath;
import model.surface.measurement.SurfaceMeasurementTriangleRaster;
import model.topology.Inlet;
import model.topology.Manhole;
import org.opengis.referencing.operation.TransformException;

/**
 * Dealing with the transport of particles on a surface. Particles move in 2d
 * space continuum. Must be one per ParticleThread. is not threadsave when used
 * by multiple htreads.
 *
 * @author riss,saemann
 */
public class ParticleSurfaceComputing2D implements ParticleSurfaceComputing {

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
    protected RandomArray random;

    /**
     * Stutus variable for debugging. Increase after every important step to see
     * where surface computing is hanging.
     */
    public int status = -1;

//    Coordinate cor = new Coordinate();
    /**
     * Calculates Diffusion dependent on the surrounding of the particle.
     */
    protected DiffusionCalculator2D D = new DiffusionCalculator2D();
    public boolean enableDiffusion = true;
    public boolean enableminimumDiffusion = true;
    public boolean getTestSolutionForAnaComparison = false;

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

    public static int numberOfErrors = 0;

    /////Arrays to be filled from other functions. so no extra allocation is needed.
    private double[] particlevelocity = new double[2];
    private final double[] temp_barycentricWeights = new double[3];
    private final double[][] tempVertices = new double[3][3];
    private double[] tempDiff = new double[2];

    private double posxalt, posyalt, posxneu, posyneu, totalvelocity, timeLeft;

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
    private double[] st01, st12, st20;
    private double lengthfactor = 1;
    private int bwindex = -1;
    private int cellIDnew;

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
//        this.random = new Random(seed);//.setRandomGenerator(new Random(seed));

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
//        try {
//            System.out.println("move particle");
//            status = 0;
            checkSurrounding(p);
//            status = 10;
//            moveParticle2(p);
            moveParticleCellIterative(p);

            if (p.isOnSurface()) {
//                status = 31;
                surface.getMeasurementRaster().measureParticle(simulationtime, p, threadindex);
            }
//            status = 20;
//            if (allowWashToPipesystem) {
////                status = 21;
//                washToPipesystem(p, p.surfaceCellID);
//            }
//            status = 30;

//            status = 40;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

//    /**
//     * One of the internal transport functions. the 'moveParticle'-function can
//     * call this method. For 2D particle transport
//     *
//     * @param p
//     */
//    private void moveParticle2(Particle p) throws Exception {
//
//        // get the particle velocity (most computation time used here)
//        particlevelocity = surface.getParticleVelocity2D(p, p.surfaceCellID, particlevelocity, temp_barycentricWeights);
//
////        System.out.println("particle.velocity="+particlevelocity[0]+", "+particlevelocity[1]);
//        posxalt = p.getPosition3d().x;
//        posyalt = p.getPosition3d().y;
//
//        if (enableDiffusion) {
//            // calculate diffusion
//            if (particlevelocity[0] != 0 && particlevelocity[1] != 0) {
//                totalvelocity = testVelocity(particlevelocity);
//                p.addMovingLength(totalvelocity * dt);
//                //Optimized version already gives the squarerooted values. (To avoid squareroot operations [very slow]
//                tempDiff = D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);
//                double sqrt2dtDx = sqrt2dt * tempDiff[0];
//                double sqrt2dtDy = sqrt2dt * tempDiff[1];
//
//                // random walk simulation
//                double z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
//                double z1 = nextRandomGaussian();
////                if(p.getId()==0){
////                    System.out.println(p.getId()+":  "+getRandomIndex());
////                }
//
//                // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
//                posxneu = (posxalt + particlevelocity[0] * dt + (particlevelocity[0] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[1] / totalvelocity) * z2 * sqrt2dtDy));
//                posyneu = (posyalt + particlevelocity[1] * dt + (particlevelocity[1] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[0] / totalvelocity) * z2 * sqrt2dtDy));
//            } else {
//                posxneu = posxalt;
//                posyneu = posyalt;
//                return;
//            }
//        } else {
//            posxneu = (posxalt + particlevelocity[0] * dt);// only advection
//            posyneu = (posyalt + particlevelocity[1] * dt);// only advection
//        }
//
//        // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
//        // da eine Veränderung durch Modellränder vorkommen kann
//        p.surfaceCellID = surface.getTargetTriangleID(p, p.surfaceCellID, posxalt, posyalt, posxneu, posyneu, 10, temp_barycentricWeights, tempVertices);
//
//    }
    private void moveParticleCellIterative(Particle p) {

        // get the particle velocity (most computation time used here)
//        System.out.println("particle.velocity="+particlevelocity[0]+", "+particlevelocity[1]);
        posxalt = p.getPosition3d().x;
        posyalt = p.getPosition3d().y;

        timeLeft = dt;
        cellID = p.surfaceCellID;
        loopcounter = 0;
        z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
        z1 = nextRandomGaussian();
        while (timeLeft > 0) {
            particlevelocity = surface.getParticleVelocity2D(p, cellID, particlevelocity, temp_barycentricWeights);
            if (enableDiffusion) {
                // calculate diffusion 
//                if (particlevelocity[0] != 0 && particlevelocity[1] != 0) {
                totalvelocity = testVelocity(particlevelocity);
                if (Math.abs(totalvelocity) > 0.001) {
//                    p.addMovingLength(totalvelocity * dt);
                    //Optimized version already gives the squarerooted values. (To avoid squareroot operations [very slow]
                    tempDiff = D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);
                    sqrt2dtDx = sqrt2dt * tempDiff[0] * timeLeft / dt;
                    sqrt2dtDy = sqrt2dt * tempDiff[1] * timeLeft / dt;

                    // random walk step
//                posxneu = (posxalt + particlevelocity[0] * timeLeft + (2 * z1 * sqrt2dtDx));
//                posyneu = (posyalt + particlevelocity[1] * timeLeft + (2 * z2 * sqrt2dtDy));
                    // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
                    posxneu = (posxalt + particlevelocity[0] * timeLeft + (particlevelocity[0] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[1] / totalvelocity) * z2 * sqrt2dtDy));
                    posyneu = (posyalt + particlevelocity[1] * timeLeft + (particlevelocity[1] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[0] / totalvelocity) * z2 * sqrt2dtDy));
                } else {
                    if (gradientFlowForDryCells) {
                        totalvelocity = 0.001f;
                        particlevelocity[0] = surface.getTriangle_downhilldirection()[cellID][0] * totalvelocity;
                        particlevelocity[1] = surface.getTriangle_downhilldirection()[cellID][1] * totalvelocity;
                        posxneu = (posxalt + particlevelocity[0]);
                        posyneu = (posyalt + particlevelocity[1]);
//                        timeLeft -= 0.1 * dt;
                    }
                    if (enableminimumDiffusion) {
                        //tempDiff = D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);
                        sqrt2dtDx = sqrt2dt * 0.001 * timeLeft / dt;
                        sqrt2dtDy = sqrt2dt * 0.001 * timeLeft / dt;

                        // random walk simulation
//                        double z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
//                        double z1 = nextRandomGaussian();
                        posxneu = (posxalt + particlevelocity[0] * timeLeft + (2 * z1 * sqrt2dtDx));
                        posyneu = (posyalt + particlevelocity[1] * timeLeft + (2 * z2 * sqrt2dtDy));
                        timeLeft -= 0.5 * dt;
                    } else {
                        if (!gradientFlowForDryCells) {
                            posxneu = posxalt;
                            posyneu = posyalt;
                            break;
                        }
                    }
                }
            } else {
                posxneu = (posxalt + particlevelocity[0] * timeLeft);// only advection
                posyneu = (posyalt + particlevelocity[1] * timeLeft);// only advection
            }

            // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
            // da eine Veränderung durch Modellränder vorkommen kann
            node0 = surface.getTriangleNodes()[cellID][0];
            node1 = surface.getTriangleNodes()[cellID][1];
            node2 = surface.getTriangleNodes()[cellID][2];

            vertex0 = surface.getVerticesPosition()[node0];
            vertex1 = surface.getVerticesPosition()[node1];
            vertex2 = surface.getVerticesPosition()[node2];

            //Test if new position is inside the old cell
            GeometryTools.fillBarycentricWeighing(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
            if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
                //Stays inside this cell
                p.surfaceCellID = cellID;
                p.setPosition3D(posxneu, posyneu);
                if (allowWashToPipesystem) {
                    washToPipesystem(p, cellID);
                }
                break;
            }
            if (Double.isNaN(temp_barycentricWeights[0])) {
                //on an edge
                posxneu = surface.getTriangleMids()[p.surfaceCellID][0];
                posyneu = surface.getTriangleMids()[p.surfaceCellID][1];
                cellID = p.surfaceCellID;
                if (allowWashToPipesystem) {
                    washToPipesystem(p, cellID);
                }
                break;

//                System.out.println(vertex0[0] + ", " + vertex1[0] + ", " + vertex2[0] + "\t y: " + vertex0[1] + ", " + vertex1[1] + ", " + vertex2[1] + "\t Px " + posxneu + ", " + posyneu + "\t timeleft:" + timeLeft + ", V=" + totalvelocity);
            }
            //else it is somewhere outside the cell. We have to transform it back to the edge
            /**
             * The lengthfactor multiplied with the movement vector hits exactly
             * the edge of the cell.
             */
            lengthfactor = 1;
            bwindex = -1;
            st01 = GeometryTools.lineIntersectionST(posxalt, posyalt, posxneu, posyneu, vertex0[0], vertex0[1], vertex1[0], vertex1[1]);
            st12 = GeometryTools.lineIntersectionST(posxalt, posyalt, posxneu, posyneu, vertex1[0], vertex1[1], vertex2[0], vertex2[1]);
            st20 = GeometryTools.lineIntersectionST(posxalt, posyalt, posxneu, posyneu, vertex2[0], vertex2[1], vertex0[0], vertex0[1]);

            //if barycentric weight index 1 is negative, the partivle crossed edge opposite side 0-2 into neighbour no.1 and so on...
            if (st12[0] > 0 && st12[0] < 1) {
                //Search for intersection between travl path and first edge
                lengthfactor = st12[0];
                bwindex = 0;
            }
            if (st20[0] > 0 && st20[0] < 1) {
                if (lengthfactor > st20[0]) {
                    bwindex = 1;
                    lengthfactor = st20[0];
                }
            }
            if (st01[0] > 0 && st01[0] < 1) {
                if (lengthfactor > st01[0]) {
                    lengthfactor = st01[0];
                    bwindex = 2;
                }
            }
            if (lengthfactor >= 1) {
//                System.out.println("STRANGE  " + bw[0] + "," + bw[1] + "," + bw[2]);
                //No intersection. Particle can stay inside this cell.
                posxneu = surface.getTriangleMids()[p.surfaceCellID][0];
                posyneu = surface.getTriangleMids()[p.surfaceCellID][1];
                cellID = p.surfaceCellID;
                if (allowWashToPipesystem) {
                    washToPipesystem(p, cellID);
                }
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
                cellIDnew = surface.getNeighbours()[cellID][bwindex];//This only is correct, if the neighbours are constructed in the same order as the edges are defined. Otherwise comment in the section above.

                if (cellIDnew >= 0) {
//                    test for velocity in this cell
                    if (preventEnteringDryCell && surface.getTriangleVelocity(cellIDnew, surface.getActualTimeIndex())[0] == 0.0) {
                        //PArticle tries to move over the edge to a cell where it will get stuck
                        lengthfactor *= 0.95;
                        posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                        posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                        break;
                    }

                    lengthfactor *= 1.001;//Make sure the particle is in the new cell
                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                    timeLeft *= (1 - lengthfactor);
                    cellID = cellIDnew;
                    if (allowWashToPipesystem) {
                        boolean transferedToPipe = washToPipesystem(p, cellID);
                        if (transferedToPipe) {
                            return;
                        }
                    }

                    //test if new cell contains position
                    node0 = surface.getTriangleNodes()[cellID][0];
                    node1 = surface.getTriangleNodes()[cellID][1];
                    node2 = surface.getTriangleNodes()[cellID][2];

                    vertex0 = surface.getVerticesPosition()[node0];
                    vertex1 = surface.getVerticesPosition()[node1];
                    vertex2 = surface.getVerticesPosition()[node2];
                    GeometryTools.fillBarycentricWeighing(temp_barycentricWeights, vertex0[0], vertex1[0], vertex2[0], vertex0[1], vertex1[1], vertex2[1], posxneu, posyneu);
                    if (temp_barycentricWeights[0] > 0 && temp_barycentricWeights[1] > 0 && temp_barycentricWeights[2] > 0) {
                        //is in new

                    } else {

                        if (temp_barycentricWeights[0] > -0.001 && temp_barycentricWeights[1] > -0.001 & temp_barycentricWeights[2] > -0.001) {
                            //Particle is very close to this cell, but not yet inside.
                            //give it a small jump towards the center
                            posxneu = posxneu * 0.9 + surface.getTriangleMids()[cellID][0] * 0.1;
                            posyneu = posyneu * 0.9 + surface.getTriangleMids()[cellID][1] * 0.1;

                        } else {
                            //Particle is completely outside of this cell. Transfer it to the cell center.
//                            System.out.println(loopcounter + ", p" + p.getId() + " is not in new one " + df.format(bwnew[0]) + ", " + df.format(bwnew[1]) + ", " + df.format(bwnew[2]));
                            posxneu = surface.getTriangleMids()[cellID][0];
                            posyneu = surface.getTriangleMids()[cellID][1];
                            break;
                        }
                    }

                } else {
                    //PArticle tries to move over the edge into an undefined area
                    //Has to stay inside this cell!
                    lengthfactor *= 0.95;
                    posxneu = posxalt + (posxneu - posxalt) * lengthfactor;
                    posyneu = posyalt + (posyneu - posyalt) * lengthfactor;
                    break;
                }

            }
            loopcounter++;
            if (loopcounter > 50) {
//                System.out.println("exceeded max loops (" + loopcounter + ") for particle " + p.getId() + " in cell " + cellID + " V=" + totalvelocity+"\t time left:"+timeLeft);
                break;
            }
            posxalt = posxneu;
            posyalt = posyneu;
//            p.surfaceCellID = surface.getTargetTriangleID(p, p.surfaceCellID, posxalt, posyalt, posxneu, posyneu, 10, temp_barycentricWeights, tempVertices);
//            break;
        }
        p.setPosition3D(posxneu, posyneu);
        p.surfaceCellID = cellID;
    }

    private void checkSurrounding(Particle p) {

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
                } else if (p.getInjectionCellID() >= 0) {
                    p.surfaceCellID = p.getInjectionCellID();
                    System.out.println("Had to set surfac eCell id to injection for particle " + p);
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
                        Logger.getLogger(ParticleSurfaceComputing2D.class.getName()).log(Level.SEVERE, null, ex);
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
                        Logger.getLogger(ParticleSurfaceComputing2D.class.getName()).log(Level.SEVERE, null, ex);
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

    private double testVelocity(double[] particlevelocity) {
        double u = Math.sqrt((particlevelocity[0] * particlevelocity[0]) + (particlevelocity[1] * particlevelocity[1]));
        if (u > 3 || u < -3) {
            System.out.println("velocity (" + u + ")is implausible. set to +/- 3m/s [" + particlevelocity[0] + ", " + particlevelocity[1] + "] = " + Math.sqrt((particlevelocity[0] * particlevelocity[0]) + (particlevelocity[1] * particlevelocity[1])));
            double veloverhaeltnis = particlevelocity[0] / particlevelocity[1];
            if (veloverhaeltnis < 1) {
                particlevelocity[1] = 3;
                particlevelocity[0] = 3 * veloverhaeltnis;
            } else {
                particlevelocity[0] = 3;
                particlevelocity[1] = 3 * (1. / veloverhaeltnis);
            }

            u = 3;
        }
        return u;
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
    private boolean washToPipesystem(Particle p, int triangleID) {
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

    private double nextRandomGaussian() {
        return random.nextGaussian();
    }

    private int getRandomIndex() {
        return random.getIndex();
    }

    public String getDiffusionString() {
        return D.getDiffusionString();
    }

    @Override
    public void setDeltaTimestep(double seconds) {
        this.dt = (float) seconds;
        this.sqrt2dt = (float) Math.sqrt(2 * dt);
    }

//    @Override
//    public void setSeed(long seed) {
//        this.seed = seed;
//        random.setSeed(seed);
//    }
    @Override
    public Surface getSurface() {
        return this.surface;
    }

//    @Override
//    public long getSeed() {
//        return this.seed;
//    }
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
    public void setRandomNumberGenerator(RandomArray rd) {
        this.random = rd;
    }

    @Override
    public void setActualSimulationTime(long timeMS) {
        this.setSimulationtime(timeMS);
    }

}
