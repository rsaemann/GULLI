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

import control.maths.UniformDistribution;
import java.util.Random;
import model.particle.Particle;
import model.topology.Capacity;
import model.topology.Connection_Manhole;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Connection_Manhole_Surface;
import model.topology.Manhole;

/**
 * This class calculates the probabilities of particle movement. Where they
 * might go and which velocity_m2p they will have.
 *
 * @author saemann
 */
public class FlowCalculatorMixed implements FlowCalculator {

    public static boolean verbose = false;
    public static boolean useFlowProportionalityOutflow = true;

    /**
     * Waterlevels below this value are treted as dry / immovable.
     */
    public static float dryWaterlevel = 0.001f;

    public static int numberOfWettedConnections(Manhole mh) {
        int number = 0;
        /*Connections are ordered from bottom to top. Therefore we can easily go
         from bottom to top and check if the connection is filled with water
         */
        double h = mh.getWaterHeight();

        for (int i = 0; i < mh.getConnections().length; i++) {
            if (mh.getConnections()[i].getHeight() > h) {
                number = i;
                //a number of 0 means, that no outflow occurs at the moment.
                break;
            }
        }
//        System.out.println(" number="+number);
        if (verbose) {
            System.out.println("FlowCalculator: Number of possible connections:" + number + "    Waterheight:" + mh.getWaterHeight());
        }
        return number;
    }

    @Override
    public boolean particleIsDepositing(Particle particle, Capacity capacity, UniformDistribution random) {
        return false;
    }

    @Override
    public boolean particleIsEroding(Particle particle, Capacity capacity, UniformDistribution random) {
        return true;
    }

    @Override
    public Connection_Manhole whichConnection(Manhole mh, Random probability, double ds) {

        if (mh.getWaterlevel() < dryWaterlevel) {
            if (verbose) {
                System.out.println("waterlevel<"+dryWaterlevel+"m\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + " --> null");
            }
            return null;
        }

        float h = (float) mh.getWaterHeight();
        int counter = 0;
        if (ds > 0) {
            //Positive Flow, Particle will leave in direction of flow
            float qsum = 0;

            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.getHeight() <= h) {
                    if (connection.isFlowInletToPipe()) {
                        //Water in this connection
                        qsum += Math.abs(connection.getPipe().getFlowActual());
                        counter++;
                    }
                } else {
                    //Connections are odereder first-low.
                    // -> if one is dry all following are dry, too.
                    break;
                }
            }
            float spillthreashold = qsum;
//            //Test Top connection
            if (ParticlePipeComputing.spillOutToSurface) {
                if (mh.getStatusTimeLine().getActualFlowToSurface() > 0) {
                    qsum += Math.abs(mh.getStatusTimeLine().getActualFlowToSurface());
//                    System.out.println("add outflow to surface "+mh.getStatusTimeLine().getActualFlowToSurface());
                }
            }
            if (counter == 0 || qsum < 0.00001) {
                if (verbose) {
                    System.out.println("qsum=0\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + "\tqsum:" + qsum + "  --> null");
                }

                return null;
            }
            float p = probability.nextFloat();
            float threashold = p * qsum;

            if (threashold > spillthreashold) {
//                System.out.println("spill to surface through connection " + mh.getTopConnection());
                //Spill out to surface
                if (mh.getTopConnection() == null) {
                    Connection_Manhole_Surface ch = new Connection_Manhole_Surface(mh, mh.getPosition3D(0), mh.getSurfaceTriangleID(), null);
                    mh.setTopConnection(ch);
                }
                return mh.getTopConnection();
            }
            //search in outgoing pipes.
            qsum = 0;
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.getHeight() < h && connection.isFlowInletToPipe()) {
                    //Water in this connection
                    qsum += Math.abs(connection.getPipe().getFlowActual());
                    if (qsum > threashold) {
                        if (verbose) {
                            System.out.println("qsum:" + qsum + " > " + threashold + " threashold --> " + connection);
                        }
                        return connection;
                    }
                }
            }
            if (verbose) {
                System.err.println(this.getClass() + "::whichConnection()::No outflow connection found! qsum=" + qsum + " threshold:" + threashold);
            }
        } else {
            //Negative flow, Particle will go to an upstream pipe
            //Positive Flow, Particle will leave in direction of flow
            float qsum = 0;
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.getHeight() < h) {
                    if (connection.isFlowOutletFromPipe()) {
                        //Water in this connection
                        qsum += Math.abs(connection.getPipe().getFlowActual());
                        counter++;
                    }
                } else {
                    //Connections are odereder first-low.
                    // -> if one is dry all following are dry, too.
                    break;
                }
            }

//            float spillthreashold = qsum;
//            //Test Top connection
//            if ( mh.getStatusTimeLine().getActualFlowToSurface() < 0) {
//                qsum += Math.abs(mh.getStatusTimeLine().getActualFlowToSurface());
//            }
            if (counter == 0 || qsum < 0.00001) {
                if (verbose) {
                    System.out.println("-qsum=0\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + "\tqsum:" + qsum);
                }

                return null;
            }
            float p = probability.nextFloat();
            float threashold = p * qsum;
//            if (threashold > spillthreashold) {
//                //Spill out to surface
//                return mh.getTopConnection();
//            }
            //search in outgoing pipes.
            qsum = 0;
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.getHeight() < h && connection.isFlowOutletFromPipe()) {
                    //Water in this connection
                    qsum += Math.abs(connection.getPipe().getFlowActual());
                    if (qsum > threashold) {
                        return connection;
                    }
                }
            }
            System.err.println(this.getClass() + "whichConnection()::No inflow connection found! qsum=" + qsum + " threshold:" + threashold);
        }
        if (verbose) {
            System.out.println("weiß auch nicht\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight());
        }

        return null;
    }
}
