package control.threads;

import java.util.Collection;
import model.timeline.array.ArrayTimeLinePipe;
import model.topology.Network;
import model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class HydrodynamicsThread extends Thread {

    private final ThreadBarrier<HydrodynamicsThread> barrier;
    boolean runendless = true;
    double dt; //seconds
    private Network network;
//    private long actualTime;
    public boolean useTimelines = true;
    /**
     * Always update ALL pipevalues in every loop.
     */
    public boolean usePipeFilter = false;
    public Collection<Pipe> updatingPipes = null;
    public int allValuesUpdateLoop = 50;

    public boolean writeReynolds = true;
    private int loopcounter = 0;

    public HydrodynamicsThread(String string, long seed, ThreadBarrier<HydrodynamicsThread> barrier, Network network) {
        super(string);
        this.barrier = barrier;
        this.network = network;
    }

    public void setDeltaTime(double seconds) {
        this.dt = seconds;
    }

    public void setAffectedPipes(Collection<Pipe> pipes) {
        this.updatingPipes = pipes;
    }

    public void setTimelineValues(long actualTime) {
//        for (Manhole c : network.getManholes()) {
//            ManholeStamp s = c.getStatusTimeLine().getEarlierUntilNow(actualTime);
//            if (s == null) {
//                System.err.println(this.getClass() + ":: ManholeStamp is null for " + new Date(actualTime).toString() + " for " + c);
//                continue;
//            }
//            c.setWater_height(s.getH());
//
            
//        }
//        int timeIndex = ArrayTimeLinePipe.getTimeIndex(actualTime);
//        ArrayTimeLinePipe.setActualTime(actualTime);
        if (usePipeFilter && updatingPipes != null) {
            for (Pipe p : updatingPipes) {
//                SimplePipeStamp s = c.getStatusTimeLine().getEarlierUntilNow(actualTime);
//                if (s == null) {
//                    System.err.println(this.getClass() + ":: Pipestamp is null for " + new Date(actualTime).toString() + " for " + c);
//                    continue;
//                }
//                if (c.getActualValues() == s) {
//                    continue;
//                }
//                p.setActualValues(p.getStatusTimeLine().get);
//                p.getStartConnection().water_level_in_connection = p.getStartConnection().getManhole().getWaterHeight() - p.getStartConnection().getHeight();
//                p.getEndConnection().water_level_in_connection = p.getEndConnection().getManhole().getWaterHeight() - p.getEndConnection().getHeight();
            }

        } else {
            for (Pipe c : network.getPipes()) {
//                SimplePipeStamp s = c.getStatusTimeLine().getEarlierUntilNow(actualTime);
//                if (s == null) {
//                    System.err.println(this.getClass() + ":: Pipestamp is null for " + new Date(actualTime).toString() + " for " + c);
//                    continue;
//                }
//                if (c.getActualValues() == s) {
//                    continue;
//                }
//                c.setActualValues(s);
//                c.getStartConnection().water_level_in_connection = c.getStartConnection().getManhole().getWaterHeight() - c.getStartConnection().getHeight();
//                c.getEndConnection().water_level_in_connection = c.getEndConnection().getManhole().getWaterHeight() - c.getEndConnection().getHeight();
//                
            }
////            System.out.println(this.getClass()+" :: loop: "+loopcounter+" :: updated Values for all Pipes @"+new Date(actualTime));
        }
        }

        @Override
        public void run
        
            () {
        if (useTimelines) {
                if (network != null) {
                    setTimelineValues(barrier.getSimulationtime());
                }
            }
            barrier.initialized(this);
            //if woken up start the normal loop
            if (useTimelines) {
                while (runendless) {
                    loopcounter++;
                    
                    if (usePipeFilter && loopcounter % allValuesUpdateLoop == 0) {
                        usePipeFilter = false;
                        setTimelineValues(barrier.getSimulationtime());
                        usePipeFilter = true;
                    } else {
                        setTimelineValues(barrier.getSimulationtime());
                    }
                    barrier.loopfinished(this);
                }
            } else {
                while (runendless) {
                    loopcounter++;
                    if (true) {
//                        for (Manhole c : network.getManholes()) {
//                            c.setWater_height(c.getConnections()[c.getConnections().length - 1].getHeight() + 0.1);
//                        }
//                        for (Pipe c : network.getPipes()) {
//                            c.getFlowInletConnection().water_level_in_connection = 0.4;
//                        }
                    }
                }
                barrier.loopfinished(this);
            }
        }
        /**
         * Causes this Thread to run off after the current loop.
         */
    public void stopThread() {
        this.runendless = false;
    }

    void setNetwork(Network network) {
        this.network = network;
        try {
            this.checkTimelines();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getDispersionCoefficient(Pipe p) {
        double re = p.getReynoldsNumber_Actual();
        return 0.000003595 * Math.pow(Math.abs(re), 0.764);
    }

    private void checkTimelines() {
        for (Pipe pipe : network.getPipes()) {
            if (pipe.getStatusTimeLine() == null) {
                throw new NullPointerException("Timeline in " + pipe.toString() + " is null.");
            }
        }

//        for (Manhole mh : network.getManholes()) {
//            if (mh.getStatusTimeLine() == null) {
//                throw new NullPointerException("Timeline in " + mh.toString() + " is null.");
//            }
//        }
    }
}
