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
import control.threads.ThreadController;
import java.util.Random;
import model.particle.Particle;
import model.surface.Surface;
import model.surface.SurfaceTriangle;
import model.surface.SurfaceTrianglePath;
import model.topology.Inlet;
import model.topology.Manhole;

/**
 * Dealing with the transport of particles on a surface. Particles move in 2d
 * space continuum.
 *
 * @author riss,saemann
 */
public class ParticleSurfaceComputing2D implements ParticleSurfaceComputing {

    /**
     * Deltatime for timestep in [second]. Set by ThreadController.
     */
    protected float dt = 1;

    protected Surface surface;

    /**
     * A random variable to generate decisions. Should be reset at each new
     * start of a simulation.
     */
    protected Random random;

    /**
     * Seed used for generating the same random numbers for each run.
     */
    protected long seed = 0;

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

    public ParticleSurfaceComputing2D(Surface surface) {
        this(surface, 0);
    }

    public ParticleSurfaceComputing2D(Surface surface, long seed) {
        this.surface = surface;
        this.seed = seed;
        this.random = new Random(seed);
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
        this.random = new Random(seed);//.setRandomGenerator(new Random(seed));

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
            moveParticle2(p);
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
        int triangleID = p.surfaceCellID;
        Coordinate pos = p.getPosition3d();

        if (pos != null && triangleID >= 0) {
            //Everything ok.
        } else {
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
                    Coordinate utm = surface.getGeotools().toUTM(st.getPosition3D(0).lonLatCoordinate(), false);
                    pos = new Coordinate(utm.x, utm.y, st.getPosition3D(0).z);
                }
            } else if ((p.getSurrounding_actual() instanceof SurfaceTrianglePath)) {
                System.out.println("particle is on 1d surface path ... something is wrong!");
                return;
            } else if (p.getSurrounding_actual() instanceof Manhole) {
                Manhole mh = (Manhole) p.getSurrounding_actual();
                triangleID = (int) mh.getSurfaceTriangleID();
                if (pos == null) {
                    Coordinate utm = surface.getGeotools().toUTM(mh.getPosition().lonLatCoordinate(), false);
                    System.out.println("Converted manhole to " + utm);
                    pos = new Coordinate(utm.x, utm.y, mh.getSurface_height());
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
        // get the particle velocity
        double[] velo;// = new double[2];

        velo = surface.getParticleVelocity2D(p, triangleID);
        double u = Math.sqrt((velo[0] * velo[0]) + (velo[1] * velo[1]));
        if (u > 5 || u < -5) {
            System.out.println("velocity (" + u + ")is implausible. set to +/- 5m/s");
            double veloverhaeltnis = velo[0] / velo[1];
            if (veloverhaeltnis < 1) {
                velo[1] = 5;
                velo[0] = 5 * veloverhaeltnis;
            } else {
                velo[0] = 5;
                velo[1] = 5 * (1. / veloverhaeltnis);
            }
        }

        p.addMovingLength(u * dt);

        double posxalt = p.getPosition3d().x;
        double posyalt = p.getPosition3d().y;

        if (!enableDiffusion) {
            pos.x += (float) velo[0] * dt;// only advection
            pos.y += (float) velo[1] * dt;// only advection
        } else {
            // calculate diffusion
            if (u > 0.000001f) {
                double[] Diff = D.calculateDiffusion(velo[0], velo[1], surface, triangleID);
                double sqrt2dtDx = Math.sqrt(2 * dt * Diff[0]);
                double sqrt2dtDy = Math.sqrt(2 * dt * Diff[1]);

                // random walk simulation
                double z2 = random.nextGaussian();           // random number to simulate random walk (lagrangean transport)
                double z1 = random.nextGaussian();

            // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
                pos.x += velo[0] * dt + (velo[0] / u) * z1 * sqrt2dtDx + ((velo[1] / u) * z2 * sqrt2dtDy);
                pos.y += velo[1] * dt + (velo[1] / u) * z1 * sqrt2dtDx + ((velo[0] / u) * z2 * sqrt2dtDy);
            }
        }

        // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
        // da eine Veränderung durch Modellränder vorkommen kann
        int id;
        try {
            id = surface.getTargetTriangleID(p, triangleID, posxalt, posyalt, pos.x, pos.y, 20);
        } catch (Surface.BoundHitException boundHitException) {
            pos.x = boundHitException.correctedPosition[0];
            pos.y = boundHitException.correctedPosition[1];
            id = boundHitException.id;
        }

        p.surfaceCellID = id;

        if (allowWashToPipesystem) {
            //Check if particle can go back to pipe system.
            Inlet inlet = surface.getInlet(id);
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
            Manhole m = surface.getManhole(id);
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
                    return;
                }
            }
        }
        if (p.isOnSurface()) {
            surface.getMeasurementRaster().measureParticle(ThreadController.getSimulationTimeMS(), p);
        }
//        }
    }

    public String getDiffusionString() {
        return D.getDiffusionString();
    }

    @Override
    public void setDeltaTimestep(double seconds) {
        this.dt = (float) seconds;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    @Override
    public Surface getSurface() {
        return this.surface;
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public String reportCalculationStatus() {
        return "Status:" + status;
    }

}
