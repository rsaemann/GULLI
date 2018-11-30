/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.timeline.array;

/**
 * Holds information about the scenario's time. Start/end, timeline's
 * timeinterval
 *
 * @author saemann
 */
public class TimeContainer {

//    public static boolean useDoubleTimeIndizes=true;
    protected long[] times;

    public TimeContainer(long[] times) {
        this.times = times;
    }

    /**
     * Copyconstructor copies the long[] timearray.
     *
     * @param cont
     */
    public TimeContainer(TimeContainer cont) {
        this(cont.times);
    }

    public long getTimeMilliseconds(int timeIndex) {
        return times[timeIndex];
    }

    public int getTimeIndex(long time) {
        int i = (int) (((time - times[0]) * times.length) / (times[times.length - 1] - times[0]));
        if (i < 0) {
            return 0;
        }
        if (i >= times.length) {
            i = times.length - 1;
        }
        return i;
    }

    /**
     * Returns the time index as doublevalue regarding the fractional ratio
     * between integer index values. Gibt den Index als Double Zahl zurück, die
     * auch den Anteil zwischen den Ganzzahligen Indize berücksichtigt.
     *
     * @param time
     * @return
     */
    public double getTimeIndexDouble(long time) {
        double i = (((time - times[0]) * (times.length)) / (double) (times[times.length - 1] - times[0]));
        if (i < 0) {
//            System.out.println("time index <0 : "+i+"    time: "+time+"   starttime:"+times[0]+"  maxtime="+times[times.length - 1]);
            return 0.000000001;
        }
        if (i >= times.length - 1) {
            i = times.length - 1.000001;
        }
        return i;
    }

    public int getNumberOfTimes() {
        return times.length;
    }

    public long getDeltaTime() {
        return times[1] - times[0];
    }

    public long getFirstTime() {
        return times[0];
    }

    public long getLastTime() {
        return times[times.length - 1];
    }

}
