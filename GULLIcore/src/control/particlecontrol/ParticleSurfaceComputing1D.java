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

import control.threads.ThreadController;
import java.util.Random;
import model.particle.HistoryParticle;
import model.particle.Particle;
import model.surface.Surface;
import model.surface.SurfaceTriangle;
import model.surface.SurfaceTrianglePath;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.graph.Pair;

/**
 * Dealing with the transport of particles on a surface.
 *
 * @author saemann
 */
public class ParticleSurfaceComputing1D implements ParticleSurfaceComputing {

    /**
     * Deltatime for timestep in [second]. Set by ThreadController.
     */
    public float dt = 1;

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

    /**
     * turbulent Diffusion/dispersion m²/s
     */
    public double dispersion = 0;

    public ParticleSurfaceComputing1D(Surface surface) {
        this(surface, 0);
    }

    public ParticleSurfaceComputing1D(Surface surface, long seed) {
        this.surface = surface;
        this.random = new Random(seed);// new UniformDistribution(new Random(seed), 0.5, 1);
//        this.random.setRandomGenerator(new Random(seed));
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

    /**
     * Called from Threadcontroller for each Particle. Delegates to the actal
     * selected transport function.
     *
     * @param p
     */
    @Override
    public void moveParticle(Particle p) {
        try {
//            status = 0;
            moveParticle1(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * One of the internal transport functions. the 'moveParticle'-function can
     * call this method.
     *
     * @param p
     */
    private void moveParticle1(Particle p) {
        float remaining_dt = dt;

        // Move PArticle to end of surfacePath
//        System.out.println("Particle " + p.getId() + " is in " + p.getSurrounding_actual());
//        status = 1;
        float dispDS = (float) (random.nextDouble() * Math.sqrt(dispersion * 2 * dt));
        float dispV = dispDS / dt;
        float ds = 0;
        if (p.getSurrounding_actual() instanceof SurfaceTrianglePath) {
//            status = 2;
            SurfaceTrianglePath path = (SurfaceTrianglePath) p.getSurrounding_actual();
            //Velocity
            float v = (float) path.surface.getVelocityToNeighbour((int) path.getStartTriangleID(), path.neighbourIndex);

//            status = 3;
            ds = (v + dispV) * dt;

            double s = p.getPosition1d_actual() + ds;
            if (s < 0) {
//                status = 31;
                remaining_dt *= Math.abs(p.getPosition1d_actual() / ds);
                SurfaceTriangle triangle = surface.requestSurfaceTriangle((int) path.getStartTriangleID());
                p.setPosition1d_actual(0);
                p.setPosition3D(triangle.getPosition3D(0));
                p.setSurrounding_actual(triangle);

//                status = 32;
//                return;
            } else if (s < path.distance) {

                p.setPosition1d_actual(s);
//                status = 33;
                return;
            } else {
//                status = 34;
                //Particles position exceeded distance between triangle mids. 
                // say this particle has finished its travel path
                p.addMovingLength(path.getDistance());

                // -> set to triangle. following downhill paths will be calculated later.
                remaining_dt *= Math.abs((path.distance - p.getPosition1d_actual()) / ds);
                int target = (int) path.getTargetTriangleID();
//                status = 34444;
                SurfaceTriangle triangle = surface.requestSurfaceTriangle(target);
//                status = 35;
                if (triangle != null) {
//                    if (triangle.pariclecount == null) {
//                        triangle.pariclecount = new int[3];
//                    }
//                    triangle.measurement.measureParticle(seed, p);
                    //Particle measurement has moved to sync thread.
                    if (p.getClass().equals(HistoryParticle.class)) {
//                        status = 36;
                        ((HistoryParticle) p).addToHistory(triangle);
                    }
                }
                p.setSurrounding_actual(triangle);
//                status = 4;
            }
        }

        if (!(p.getSurrounding_actual() instanceof SurfaceTriangle)) {
//            status = 5;
            return;
        }
        //Now start loop. Every loop start the particle should be at a surfaceTriangle.
//        status = 6;
        for (int i = 0; i < 10; i++) {
            SurfaceTriangle triangle = (SurfaceTriangle) p.getSurrounding_actual();

            surface.getMeasurementRaster().measureParticle(ThreadController.getSimulationTimeMS(), p);

//            if (triangle.measurement != null) {
//                triangle.measurement.measureParticle(ThreadController.getSimulationTimeMS(), p);
//            }
            Inlet inlet = surface.getInlet((int) p.getSurrounding_actual().getManualID());
            if (inlet != null) {
//                status = 7;

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
                            p.setInPipenetwork();
                            p.toPipenetwork = triangle;
//                    status = 8;
                            //Create Shortcut
                            if (p.toSurface != null) {
                                surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangleID(), inlet, null, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
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
            if (triangle.getManhole() != null) {
                Manhole m = triangle.getManhole();
                //Check if manhole is spillout
//                status = 9;
                if (m.getStatusTimeLine().getActualFlowToSurface() <= 0) {
                    //Water can flow back into pipe network
                    p.setSurrounding_actual(m);
                    p.setPosition1d_actual(0);
                    p.setInPipenetwork();
                    p.toPipenetwork = m;

                    if (p.toSurface != null) {
                        surface.addStatistic(p, ((Manhole) p.toSurface).getSurfaceTriangleID(), null, m, ThreadController.getSimulationTimeMS() - p.toSurfaceTimestamp);
                    }
//                    status = 10;
                    return;
                }
            }
//            status = 10;
            //Calculate outgoing path
            Pair<SurfaceTrianglePath, Float> pair = calcOutgoingPath((int) triangle.getTriangleID(), ds);

            if (pair == null || pair.first == null) {
                //Particle can not escape this triangle for the moment. stay here
//                status = 11;
                return;
            }
//            status = 12;
            if (remaining_dt < 0) {
                System.out.println("remaindt=" + remaining_dt);
                return;
            }

//            status = 125;
            ds = pair.second * remaining_dt + dispV * remaining_dt;
            if (p.getPosition1d_actual() + ds < 0) {
//                System.out.println("Negative traveldistance");
                p.setPosition1d_actual(0);
                p.setSurrounding_actual(triangle);
                remaining_dt *= Math.abs(p.getPosition1d_actual() / ds);
            } else if (ds < pair.first.distance) {
                //Particle stays on this path at the end of this timestep.
                p.setPosition1d_actual(ds);
                p.setSurrounding_actual(pair.first);
//                status = 13;
                return;
            } else {
                //Particle passes this path and reaches next triangle during this loop
                remaining_dt *= pair.first.distance / ds;
//                status = 14;
                SurfaceTriangle tri = surface.requestSurfaceTriangle((int) pair.first.getTargetTriangleID());
                p.addMovingLength(pair.first.getDistance());
//                status = 15;
                if (tri != null) {
//                    if (triangle.pariclecount == null) {
//                        triangle.pariclecount = new int[3];
//                    }
//                    triangle.pariclecount[p.getMaterial().surfaceCountIndex]++;
                    //Particle measurement has moved to sync thread
                    if (p.getClass().equals(HistoryParticle.class)) {
                        ((HistoryParticle) p).addToHistory(tri);
                    }
                }
                p.setSurrounding_actual(tri);
            }
//            status = 16;
        }

    }

    /**
     *
     * @param triangleID
     * @param ds positive for particles in flow direction. negative -> uphill
     * @return
     */
    private Pair<SurfaceTrianglePath, Float> calcOutgoingPath(int triangleID, double ds) {

        int outgoing = 0;

//        status = 30;
        outgoing = (int) (random.nextInt(3));
//        status = 100 + outgoing;
        if (surface.getNeighbours()[triangleID][outgoing] < 0) {
            //Hit wall (no neighbour here)
            return null;
        }
        double v = surface.getVelocityToNeighbour(triangleID, outgoing);
//        status = 31;
        if (v <= 0 && ds > 0) {
            return null;
        }
        if (v >= 0 && ds < 0) {
            return null;
        }
//        status = 32;
        SurfaceTrianglePath path = surface.requestSurfacePath(triangleID, outgoing);
//        status = 33;
        if (path == null || path.getTargetTriangleID() < 0) {
            return null;
        }
//        status = 34;
        return new Pair<>(path, (float) v);

    }

    @Override
    public Surface getSurface() {
        return surface;
    }

    @Override
    public void setDeltaTimestep(double seconds) {
        this.dt = (float) seconds;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        this.random.setSeed(seed);
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public String reportCalculationStatus() {
        return status + "";
    }

}
