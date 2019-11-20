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
     * Seed used for generating the same random numbers for each run.
     */
//    protected long seed = 0;
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
    public boolean getTestSolutionForAnaComparison = false;

    /**
     * When active particles can go to the pipe system through inlets and
     * manholes.
     */
    public static boolean allowWashToPipesystem = true;

    public static int numberOfErrors = 0;

    /////Arrays to be filled from other functions. so no extra allocation is needed.
    private double[] particlevelocity = new double[2];
    private final double[] temp_barycentricWeights = new double[3];
    private final double[][] tempVertices = new double[3][3];
    private double[] tempDiff = new double[2];

    private double posxalt, posyalt, posxneu, posyneu, totalvelocity;

    private DecimalFormat df = new DecimalFormat("0.0000", DecimalFormatSymbols.getInstance(Locale.US));

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
        try {
//            status = 0;
            checkSurrounding(p);
//            status = 10;
            moveParticle2(p);
//            status = 20;
            if (allowWashToPipesystem) {
//                status = 21;
                washToPipesystem(p, p.surfaceCellID);
            }
//            status = 30;
            if (p.isOnSurface()) {
//                status = 31;
                surface.getMeasurementRaster().measureParticle(simulationtime, p, threadindex);
            }
//            status = 40;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * One of the internal transport functions. the 'moveParticle'-function can
     * call this method. For 2D particle transport
     *
     * @param p
     */
    private void moveParticle2(Particle p) throws Exception {

        // get the particle velocity (most computation time used here)
        particlevelocity = surface.getParticleVelocity2D(p, p.surfaceCellID, particlevelocity, temp_barycentricWeights);

        totalvelocity = testVelocity(particlevelocity);
        p.addMovingLength(totalvelocity * dt);

        posxalt = p.getPosition3d().x;
        posyalt = p.getPosition3d().y;

        if (enableDiffusion) {
            // calculate diffusion
            if (totalvelocity > 0.0000001f) {
                //Optimized version already gives the squarerooted values. (To avoid squareroot operations [very slow]
                tempDiff = D.calculateDiffusionSQRT(particlevelocity[0], particlevelocity[1], surface, p.surfaceCellID, tempDiff);
                double sqrt2dtDx = sqrt2dt * tempDiff[0];
                double sqrt2dtDy = sqrt2dt * tempDiff[1];

                // random walk simulation
                double z2 = nextRandomGaussian();           // random number to simulate random walk (lagrangean transport)
                double z1 = nextRandomGaussian();
//                if(p.getId()==0){
//                    System.out.println(p.getId()+":  "+getRandomIndex());
//                }

                // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
                posxneu = (posxalt + particlevelocity[0] * dt + (particlevelocity[0] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[1] / totalvelocity) * z2 * sqrt2dtDy));
                posyneu = (posyalt + particlevelocity[1] * dt + (particlevelocity[1] / totalvelocity) * z1 * sqrt2dtDx + ((particlevelocity[0] / totalvelocity) * z2 * sqrt2dtDy));
            } else {
                posxneu = posxalt;
                posyneu = posyalt;
                return;
            }
        } else {
            posxneu = (posxalt + particlevelocity[0] * dt);// only advection
            posyneu = (posyalt + particlevelocity[1] * dt);// only advection
        }

        // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
        // da eine Veränderung durch Modellränder vorkommen kann
        p.surfaceCellID = surface.getTargetTriangleID(p, p.surfaceCellID, posxalt, posyalt, posxneu, posyneu, 10, temp_barycentricWeights, tempVertices);

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

    private void washToPipesystem(Particle p, int triangleID) {
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
                    return;
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
            }
        }
    }

    private double nextRandomGaussian() {
        return random.nextGaussian();
    }
    
    private int getRandomIndex(){
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
        if (surface.getMeasurementRaster().statuse != null && surface.getMeasurementRaster().monitor != null && surface.getMeasurementRaster().monitor[threadindex] != null) {
            return "Status:" + status + "  Raster:" + surface.getMeasurementRaster().statuse[threadindex] + " monitor: " + surface.getMeasurementRaster().monitor[threadindex] + "   mass: " + surface.getMeasurementRaster().monitor[threadindex].totalParticleCount() + "  isused:" + surface.getMeasurementRaster().monitor[threadindex].lock.toString();
        } else {
            return "Status:" + status;
        }
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
