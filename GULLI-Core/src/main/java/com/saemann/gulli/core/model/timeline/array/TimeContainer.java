/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.timeline.array;

/**
 * Holds information about the scenario's time. Start/end, timeline's
 * timeinterval
 *
 * @author saemann
 */
public class TimeContainer {

//    public static boolean useDoubleTimeIndizes=true;
    protected long[] times;
    protected int duration;

    public TimeContainer(long[] times) {
        this.times = times;
        if (times == null || times.length < 2) {
            this.duration = Integer.MAX_VALUE;
        } else {
            this.duration = (int) (times[times.length - 1] - times[0]);
        }
    }

    /**
     *
     * @param starttime long milliseconds since 1.1.1970
     * @param endtime long milliseconds since 1.1.1970
     * @param timespots number of entries of the time array
     */
    public TimeContainer(long starttime, long endtime, int timespots) {
        this.times = new long[timespots];
        for (int i = 0; i < times.length; i++) {
            times[i] = (long) (starttime + (endtime - starttime) * (double) (i / timespots));
        }
        this.duration = (int) (endtime - starttime);
    }

    public static TimeContainer byIntervallMilliseconds(long starttime, long endtime, long intervallMS) {
        int numberOfEntries = (int) ((endtime - starttime) / intervallMS + 1);
        long[] times = new long[numberOfEntries];
        for (int i = 0; i < times.length; i++) {
            times[i] = (long) (starttime + i * intervallMS);
        }
        return new TimeContainer(times);
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
        int i = (int) (((time - times[0]) * (times.length - 1)) / duration);
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
        double i = (((time - times[0]) * (times.length - 1)) / (double) (duration));
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

    public long getDeltaTimeMS() {
        return times[1] - times[0];
    }

    public long getFirstTime() {
        return times[0];
    }

    public long getLastTime() {
        return times[times.length - 1];
    }

}
