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
import model.topology.Position3D;

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

    public ParticleSurfaceComputing2D(Surface surface) {
        this(surface, 0);
    }

    public ParticleSurfaceComputing2D(Surface surface, long seed) {
        this.surface = surface;
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
        Position3D pos = p.getPosition2d_actual();

        if (p.isOnSurface() && triangleID >= 0 && pos != null) {
            //Everything ok.
        } else {
            if (p.getSurrounding_actual() instanceof Surface) {
//                p.setOnSurface();
                if (p.surfaceCellID >= 0) {
                    double[] xy = surface.getTriangleMids()[p.surfaceCellID];
                    pos = new Position3D(0, 0, xy[0], xy[1], xy[2]);
                } else if (p.getInjectionCellID() >= 0) {
                    p.surfaceCellID = p.getInjectionCellID();
                    System.out.println("Had to set surfac eCell id to injection for particle " + p);
                    double[] xy = surface.getTriangleMids()[p.surfaceCellID];
                    pos = (new Position3D(0, 0, xy[0], xy[1], xy[2]));
                } else {
                    System.err.println("no information of surface ID for Particle " + p);
                    return;
                }
            } else if (p.getSurrounding_actual() instanceof SurfaceTriangle) {
//                p.setOnSurface();
                SurfaceTriangle st = (SurfaceTriangle) p.getSurrounding_actual();
                triangleID = (int) st.getManualID();
                if (pos == null) {
                    Coordinate utm = surface.getGeotools().toUTM(st.getPosition3D(0).latLonCoordinate());
                    pos = new Position3D(st.getPosition3D(0).getLongitude(), st.getPosition3D(0).getLatitude(), utm.x, utm.y, st.getPosition3D(0).z);
                }
            } else if ((p.getSurrounding_actual() instanceof SurfaceTrianglePath)) {
                System.out.println("particle is on 1d surface path ... something is wrong!");
                return;
            } else if (p.getSurrounding_actual() instanceof Manhole) {
                Manhole mh = (Manhole) p.getSurrounding_actual();
                triangleID = (int) mh.getSurfaceTriangle().getManualID();
                if (pos == null) {
                    Coordinate utm = surface.getGeotools().toUTM(mh.getPosition().latLonCoordinate());
                    pos = new Position3D(mh.getPosition().getLongitude(), mh.getPosition().getLatitude(), utm.x, utm.y, mh.getSurface_height());
                }
            } else {
                System.out.println("Particle " + p + " is in unknown Capacity " + p.getSurrounding_actual());
            }
            p.setPosition2d_actual(pos);
            p.setOnSurface();
            p.setSurrounding_actual(surface);
            p.surfaceCellID = triangleID;
        }
//        //Not correctly initialized since transfer from pipe system.
//        if (triangleID < 0) {
//            System.out.println("Particles triangle<0: " + p + "  status:" + p.status + "  surfaceElement:" + p.surfaceCellID + "  capacity:" + p.getSurrounding_actual());
//            if (p.toSurface != null) {
//                if (p.toSurface instanceof Manhole) {
//                    triangleID = (int) ((Manhole) p.toSurface).getManualID();
//                } else if (p.toSurface instanceof SurfaceTriangle) {
//                    triangleID = (int) ((SurfaceTriangle) p.toSurface).getManualID();
//                }
//            }
//        }
//
//        if (triangleID < 0) {
//            System.out.println("Particle is not OK: " + p + "  status:" + p.status + "  surfaceElement:" + p.surfaceCellID + "  capacity:" + p.getSurrounding_actual());
//            return;
//        }

//        // Convert the position in UTM coordinates
//        if (pos == null) {
//            System.out.println("UTM Position für Particle " + p + " muss berechnet werden :" + p.getPosition2d_actual() + "    capacity:" + p.getSurrounding_actual());
//            Coordinate utm = surface.getGeotools().toUTM(p.toSurface.getPosition3D(0).latLonCoordinate());
//            pos = new Position3D(p.toSurface.getPosition3D(0).getLongitude(), p.toSurface.getPosition3D(0).getLatitude(), utm.x, utm.y, p.toSurface.getPosition3D(0).z);
////            pos = new Position3D(p.toSurface.getPosition3D(0));
////            p.setPosition2d_actual(pos);
//        }
        // get the particle velocity
        double[] velo;// = new double[2];

//        if (getTestSolutionForAnaComparison) {
//            velo[0] = (float) 0.05;
//            velo[1] = 0;
//        } else {
        velo = surface.getParticleVelocity2D(p, triangleID);
        double u = Math.sqrt((velo[0] * velo[0]) + (velo[1] * velo[1]));
        if (u > 5 || u < -5) {
            System.out.println("velocity is implausible. set to +/- 5m/s");
            double veloverhaeltnis = velo[0] / velo[1];
            if (veloverhaeltnis < 1) {
                velo[1] = 5;
                velo[0] = 5 * veloverhaeltnis;
            } else {
                velo[0] = 5;
                velo[1] = 5 * (1. / veloverhaeltnis);
            }
        }
//        }

        p.addMovingLength(u * dt);
        double h = surface.getActualWaterlevel(triangleID);

        double posxalt = p.getPosition2d_actual().x;
        double posyalt = p.getPosition2d_actual().y;

        if (!enableDiffusion) {
            pos.x += (float) velo[0] * dt;// only advection
            pos.y += (float) velo[1] * dt;// only advection
//                D.setDiffusionType("Adv2D");
        } else {
//            if (velo[0] != 0 || velo[1] != 0) {
//            if (Math.abs(u) > 0.0001) {
            // calculate diffusion

//                if (getTestSolutionForAnaComparison) {
//                    h = 1;
//                }
            double[] Diff = D.calculateDiffusion(velo[0], velo[1], h, surface.getkst());
            double sqrt2dtDx = Math.sqrt(2 * dt * Diff[0]);
            double sqrt2dtDy = Math.sqrt(2 * dt * Diff[1]);

            // random walk simulation
            double z2 = random.nextGaussian();           // random number to simulate random walk (lagrangean transport)
            double z1 = random.nextGaussian();

            // random walk in 2 dimsensions as in "Kinzelbach and Uffing, 1991"
            pos.x += velo[0] * dt + (velo[0] / u) * z1 * sqrt2dtDx + ((velo[1] / u) * z2 * sqrt2dtDy);
            pos.y += velo[1] * dt + (velo[1] / u) * z1 * sqrt2dtDx + ((velo[0] / u) * z2 * sqrt2dtDy);
//            }
        }

        // Berechnung: welches ist das neue triangle, die funktion "getTargetTriangleID" setzt ggf. auch die x und y werte der position2d neu
        // da eine Veränderung durch Modellränder vorkommen kann
        int id;
//            System.out.println("la");
        try {
            id = surface.getTargetTriangleID(p, triangleID, posxalt, posyalt, pos.x, pos.y, 20);
        } catch (Surface.BoundHitException boundHitException) {

            pos.x = boundHitException.correctedPosition[0];
            pos.y = boundHitException.correctedPosition[1];
//            if (pos.x < 0) {
//                System.out.println("transform back to triangle " + boundHitException.id + "   pos: " + pos);
//            }
            id = boundHitException.id;
        }

        p.surfaceCellID = id;
//        p.setPosition2d_actual(pos);

        //Check if particle can go back to pipe system.
        Inlet inlet = surface.getInlet(id);
        if (inlet != null) {
//                status = 7;
//                Inlet inlet = triangle.getInlet();
            // Check if Inlet is flooded
            if (inlet.getNetworkCapacity() != null) {
                double fillrate = inlet.getNetworkCapacity().getProfile().getFillRate(inlet.getNetworkCapacity().getWaterlevel());
//                    System.out.println(inlet.getNetworkCapacity() + "  fillrate: " + fillrate + "\t timeindex:" + ((TimeIndexCalculator) inlet.getNetworkCapacity().getStatusTimeLine().getTimeContainer()).getActualTimeIndex_double());
                if (fillrate < 0.9) {

                    //Only go into the inlet for 30%
                        /*if (random.nextFloat() < 0.3f)*/ {

                        //Pipe is not flooded. Particles can enter pipenetwork here.
                        p.setSurrounding_actual(inlet.getNetworkCapacity());
//                p.setPosition1d_actual(triangle.getInlet().getPipeposition1d());
                        p.setPosition1d_actual(inlet.getPipeposition1d());
                        p.setPosition2d_actual(null);
                        p.setInPipenetwork();
                        p.toPipenetwork = inlet.getNetworkCapacity();
//                    status = 8;
                        //Create Shortcut
                        if (p.toSurface != null) {
                            surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangle(), inlet, null, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
                        }
//                    Shortcut sc = new Shortcut(p.toSurface, triangle, inlet.getNetworkCapacity().getStatusTimeLine().container.getActualTime() - p.toSurfaceTimestamp);
//                    p.usedShortcuts.add(sc);

//                System.out.println("Particle " + p.getId() + " back through Inlet to pipe " + p.getSurrounding_actual());
                        return;
                    }
                }
            } else {
                System.out.println("Inlet " + inlet.toString() + " has no pipe.");
            }
        }
        Manhole m = surface.getManhole(id);
        if (m != null) {
//                Manhole m = triangle.getManhole();
            //Check if manhole is spillout
//                status = 9;
            if (m.getStatusTimeLine().getActualFlowToSurface() <= 0) {
                //Water can flow back into pipe network
                p.setSurrounding_actual(m);
                p.setPosition1d_actual(0);
                p.setPosition2d_actual(null);
                p.setInPipenetwork();
                p.toPipenetwork = m;

                if (p.toSurface != null) {
                    surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangle(), null, m, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
                }
//                    status = 10;
                return;
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
