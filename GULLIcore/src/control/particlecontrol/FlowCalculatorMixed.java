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
import model.topology.Manhole;

/**
 * This class calculates the probabilities of particle movement. Where they
 * might go and which velocity_m2p they will have.
 *
 * @author saemann
 */
public class FlowCalculatorMixed implements FlowCalculator {

//    public static boolean useTimeSeries = false;
    public static boolean verbose = false;
    public static boolean useFlowProportionalityOutflow = true;

//    @Override
//    public Connection_Manhole_Pipe whichConnection2(Manhole mh, Random probability, double ds) {
//        int number;
//        Connection_Manhole[] cons;
//
//        if (ds >= 0) {
//            //positive direction
//            number = numberOfWettedOutgoingConnections(mh);
//
//            if (number == 0) {
//                return null;
//            }
//            cons = wettedOutgoingConnections(mh, number);
//            if (cons.length == 1) {
//                return cons[0];
//            }
//        } else {
//            //negative direction
//            number = numberOfWettedIncomingConnections(mh);
//            if (number == 0) {
//                return null;
//            }
//            cons = wettedIncomingConnections(mh, number);
//            if (cons.length == 1) {
//                return cons[0];
//            }
//        }
//        //Difficult decision: more than 1 possible exit for particle
//        double r = probability.nextDouble();
//        if (useFlowProportionalityOutflow) {
//            double sum = 0;
////            String str="";
//            for (Connection_Manhole_Pipe con : cons) {
//                sum += Math.abs(con.getPipe().getFlowActual());
////                str+=(Math.abs(con.getPipe().getFlowActual())+" + ");
//            }
////            str+=" = "+sum+"\n";
//            double threshold = r * sum;
////            System.out.println("schwelle (r="+((int)(r*100))+"%) = "+threshold+"/"+sum);
//            double newsum = 0;
//            for (Connection_Manhole_Pipe con : cons) {
//                newsum += Math.abs(con.getPipe().getFlowActual());
////                str+=(Math.abs(con.getPipe().getFlowActual())+" + ");
//                if (newsum >= threshold) {
////                    str+=(" found");
//                    return con;
//                }
//            }
////            str+=" = "+newsum+"\n";
////            System.out.println(str);
//            System.out.println("   proportionaly sum was not reached " + sum + "*" + ((int) (r * 100)) + "% =" + threshold + "   maxsum=" + newsum);
//        }
//
//        //Default: Random:
//        int index = (int) (r * cons.length);
////        System.out.println("Fallback to Randomly connectionselection index= " + (index+1) + " / " + cons.length + "\trand=" + r);
//        return cons[index];
//    }
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

//    public static int numberOfWettedOutgoingConnections(Manhole mh) {
//        int number = 0;
//        /*Connections are ordered from bottom to top. Therefore we can easily go
//         from bottom to top and check if the connection is filled with water
//         */
//        double h = mh.getWaterHeight();
//
//        for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//            if (connection.getHeight() <= h) {
//                if (connection.isFlowInletToPipe()) {
//                    number++;
//                }
//                //a number of 0 means, that no outflow occurs at the moment.
//
//            } else {
//                break;
//            }
//        }
////        System.out.println(" number="+number);
//        if (verbose) {
//            System.out.println("FlowCalculator: Number of possible outgoing connections:" + number + "    Waterheight:" + mh.getWaterHeight());
//        }
//        return number;
//    }
//    public static int numberOfWettedIncomingConnections(Manhole mh) {
//        int number = 0;
//        /*Connections are ordered from bottom to top. Therefore we can easily go
//         from bottom to top and check if the connection is filled with water
//         */
//        double h = mh.getWaterHeight();
//
//        for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//            if (connection.getHeight() <= h) {
//                if (connection.isFlowOutletFromPipe()) {
//                    number++;
//                }
//                //a number of 0 means, that no outflow occurs at the moment.
//
//            } else {
//                break;
//            }
//        }
////        System.out.println(" number="+number);
//        if (verbose) {
//            System.out.println("FlowCalculator: Number of possible incoming connections:" + number + "    Waterheight:" + mh.getWaterHeight());
//        }
//        return number;
//    }
//
//    public static Connection_Manhole_Pipe[] wettedOutgoingConnections(Manhole mh, int numberOfConnections) {
//
//        int index = 0;
//        Connection_Manhole_Pipe[] cons = new Connection_Manhole_Pipe[numberOfConnections];
//        for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//            if (connection.isFlowInletToPipe()) {
//                cons[index] = connection;
//                index++;
//                if (index >= numberOfConnections) {
//                    break;
//                }
//            }
//        }
////        System.out.println(" number="+number);
//        if (verbose) {
//            System.out.println("FlowCalculator: Number of possible outgoing connections:" + numberOfConnections + "    Waterheight:" + mh.getWaterHeight());
//        }
//        return cons;
//    }
//
//    public static Connection_Manhole_Pipe[] wettedIncomingConnections(Manhole mh, int numberOfConnections) {
//
//        int index = 0;
//        Connection_Manhole_Pipe[] cons = new Connection_Manhole_Pipe[numberOfConnections];
//        for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//            if (connection.isFlowOutletFromPipe()) {
//                cons[index] = connection;
//                index++;
//                if (index >= numberOfConnections) {
//                    break;
//                }
//            }
//        }
////        System.out.println(" number="+number);
//        if (verbose) {
//            System.out.println("FlowCalculator: Number of possible incoming connections:" + numberOfConnections + "    Waterheight:" + mh.getWaterHeight());
//        }
//        return cons;
//    }
//    public Connection_Manhole_Pipe connectionRandomly(Manhole mh, Random probability, double particle_position) {
//        int number = numberOfWettedConnections(mh);
//        if (number == 0) {
////            System.out.println("no connection above waterlevel " + mh.getWaterlevel());
//            return null;
//        }
//        if (number == 1) {
//            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//                if (particle_position >= 0) {
//                    if (connection.isFlowInletToPipe()) {
//                        return connection;
//                    }
//                } else {
//                    if (connection.isFlowOutletFromPipe()) {
//                        return connection;
//                    }
//                }
//            }
//            return null;
//        }
//        //there are more than 1 outgoing connection?
//        int n = probability.nextInt(number);
//        if (particle_position > 0) {
//            if (mh.getConnections()[n].isFlowInletToPipe()) {
//                return mh.getConnections()[n];
//            }
//            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//                if (connection.isFlowInletToPipe()) {
//                    return connection;
//                }
//            }
//        } else {
//            if (mh.getConnections()[n].isFlowOutletFromPipe()) {
//                return mh.getConnections()[n];
//            }
//            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
//                if (connection.isFlowOutletFromPipe()) {
//                    return connection;
//                }
//            }
//        }
//        return null;
//    }
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
        // is manhole waterlevel so low that it is nearly empty?
//        boolean verbose = false;
//        if (mh.getAutoID() == 161) {
//            verbose = true;
//        }
        if (mh.getWaterlevel() < 0.001) {
            if (verbose) {
                System.out.println("waterlevel<0.001\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + " --> null");
            }
            return null;
        }
        float p = probability.nextFloat();
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
            if (mh.getTopConnection() != null && mh.getTopConnection().getHeight() <= mh.getWaterHeight() && mh.getTopConnection().getSurfaceTriangle() != null && mh.getStatusTimeLine().getActualFlowToSurface() > 0) {
                qsum += Math.abs(mh.getStatusTimeLine().getActualFlowToSurface());
            }
            if (counter == 0 || qsum < 0.00001) {
                if (verbose) {
                    System.out.println("qsum=0\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + "\tqsum:" + qsum + "  --> null");
                }

                return null;
            }
            float threashold = p * qsum;

            if (threashold > spillthreashold) {
                //Spill out to surface
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

            float spillthreashold = qsum;
            //Test Top connection
            if (mh.getTopConnection() != null && mh.getTopConnection().getHeight() <= mh.getWaterHeight() && mh.getTopConnection().getSurfaceTriangle() != null && mh.getStatusTimeLine().getActualFlowToSurface() < 0) {
                qsum += Math.abs(mh.getStatusTimeLine().getActualFlowToSurface());
            }
            if (counter == 0 || qsum < 0.00001) {
                if (verbose) {
                    System.out.println("-qsum=0\t wL:" + mh.getWaterlevel() + "\t h:" + mh.getWaterHeight() + "\tqsum:" + qsum);
                }

                return null;
            }
            float threashold = p * qsum;
            if (threashold > spillthreashold) {
                //Spill out to surface
                return mh.getTopConnection();
            }
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
