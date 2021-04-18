/*
 * The MIT License
 *
 * Copyright 2020 B1.
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
package com.saemann.gulli.core.model.timeline.analysis;

import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.timeline.MeasurementTimeline;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.Pipe;

/**
 * Analyses the pipe discharge and gives hints when to redirect the outflow to a
 * treatment plant to minimize CSO / pollution outflow
 *
 * @author saemann
 */
public class OutletMinimizer {

    Pipe pipe;
//    double maximumVolume;
    PollutionDischargeInterval[] ordered;
    ArrayList<PollutionDischargeInterval> intervals;
    boolean[] maximumIntervals;

    double maximumMass, maximumConcentration;

    double containedMass;
    double emittedMass;

    /**
     * Volume [m^3] of hold back (included in the intervals) discharge
     */
    double containedVolume;

    double containedConcentrationMaximum;

    /**
     * Maximum concentration [kg/m^3] that is outside of the intervals (will be
     * emitted downstream)
     */
    double emittedConcentrationMaximum;

    public OutletMinimizer(Pipe pipe) {
        this.pipe = pipe;
//        this.maximumVolume = maximumVolume;
        analyseIntervals();
    }

    public static double totalVolumeDischarge(TimeLinePipe tlp) {
        double volumeSum = 0;
        double q = 0;
        for (int i = 1; i < tlp.getNumberOfTimes(); i++) {
            q = (tlp.getDischarge(i - 1) + tlp.getDischarge(i)) * 0.5;
            volumeSum += q * (tlp.getTimeContainer().getTimeMilliseconds(i) - tlp.getTimeContainer().getTimeMilliseconds(i - 1)) / 1000.;
        }
        return volumeSum;
    }

    public static double totalMassDischarge(TimeLinePipe tlp, MeasurementTimeline tlm) {
        double massSum = 0;
        double m = 0;
        for (int i = 1; i < tlp.getNumberOfTimes(); i++) {
            double q0 = tlp.getDischarge(i - 1);
            double q1 = tlp.getDischarge(i);
            double c0 = tlm.getConcentration(tlm.getContainer().getIndexForTime(tlp.getTimeContainer().getTimeMilliseconds(i - 1)));
            double c1 = tlm.getConcentration(tlm.getContainer().getIndexForTime(tlp.getTimeContainer().getTimeMilliseconds(i)));

            m = (c0 * q0 + c1 * q1) * 0.5;
            if (Double.isFinite(m)) {
                massSum += m * (tlp.getTimeContainer().getTimeMilliseconds(i) - tlp.getTimeContainer().getTimeMilliseconds(i - 1)) / 1000.;
            } else {
//                System.out.println("Something is not finit in mass calculation index["+i+"]: q0=" + q0 + "\tq1=" + q1+",  c0="+c0+", c1="+c1);
            }
        }
//        System.out.println("TotalMass: "+massSum+" kg");
        return massSum;
    }

    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
        ordered = null;
        intervals = null;
        maximumIntervals = null;
        maximumMass = 0;
        containedMass = 0;
        emittedMass = 0;
        containedVolume = 0;
        maximumConcentration = 0;
    }

    public void analyseIntervals() {
        if (intervals != null) {
            intervals.clear();
        }
        if (pipe == null) {
            return;
        }
        if (pipe.getStatusTimeLine() == null) {
            return;
        }
        if (pipe.getMeasurementTimeLine() == null) {
            throw new NullPointerException("No Measurement Timeline in Pipe " + pipe.getName() + ". Cannot analyse discharge.");
        }
        intervals = new ArrayList<>(pipe.getMeasurementTimeLine().getTimes().getNumberOfTimes());

        MeasurementTimeline tm = pipe.getMeasurementTimeLine();
        TimeLinePipe tl = pipe.getStatusTimeLine();
        MeasurementContainer c = tm.getContainer();
        ordered = new PollutionDischargeInterval[c.getTimes().getNumberOfTimes() - 1];
        maximumMass = 0;
        maximumConcentration = 0;
        for (int i = 1; i < c.getTimes().getNumberOfTimes(); i++) {
            long start = c.getTimeMillisecondsAtIndex(i-1);
            long ende = c.getTimeMillisecondsAtIndex(i);
            double statusTimeStartIndex=tl.getTimeContainer().getTimeIndexDouble(start);
            double statusTimeEndIndex=tl.getTimeContainer().getTimeIndexDouble(ende);
            

            long durationMS = ende - start;
            double durationS = durationMS / 1000.;
            double concentrationS = tm.getConcentration(i - 1);
            if (Double.isNaN(concentrationS)) {
                concentrationS = 0;
            }
            double concentrationE = tm.getConcentration(i);
            if (Double.isNaN(concentrationE)) {
                concentrationE = 0;
            }
            maximumConcentration = Math.max(maximumConcentration, concentrationS);

//            double tlIndexStart = tl.getTimeContainer().getTimeIndexDouble(start);
//            double tlIndexEnd = tl.getTimeContainer().getTimeIndexDouble(ende);
            double dischargeS = tl.getDischarge_DoubleIndex(statusTimeStartIndex);
            double dischargeE = tl.getDischarge_DoubleIndex(statusTimeEndIndex);
//            double statusTimeIndexDouble=tl.getTimeContainer().getTimeIndexDouble((long) ((c.getMeasurementTimestampAtTimeIndex(i)+c.getMeasurementTimestampAtTimeIndex(i-1))/2.));
//            double discharge = tl.getDischarge_DoubleIndex((statusTimeStartIndex+statusTimeEndIndex)/2.);

            double volume = (dischargeE + dischargeS) * 0.5  *durationS;
            double mass = (concentrationS * dischargeS + concentrationE * dischargeE) * 0.5 * durationS;
            if (!Double.isFinite(mass)) {
                System.out.println("something is not finite: c:" + concentrationS + "/" + concentrationE + ",   discharge: " + dischargeS + "/" + dischargeE);
                mass = 0;
            }
            maximumMass += mass;
//            System.out.println("MINI: "+i+"\t mf:"+mass +" -> total:\t"+maximumMass+" c1:"+concentrationS+"\tcE:"+concentrationE+"\tdischE:"+dischargeE);
            PollutionDischargeInterval pdi = new PollutionDischargeInterval();
            pdi.max_concentration = Math.max(concentrationS, concentrationE);
            pdi.duration = durationMS;
            pdi.discharge = (dischargeE + dischargeS) * 0.5;
            pdi.volume = volume;
            pdi.intervalIndex = i - 1;
            pdi.pollutionMass = mass;
            pdi.start = start;
            pdi.end = ende;
            ordered[i - 1] = pdi;
            intervals.add(pdi);
        }
//        System.out.println("Analysed intervals of Pipe " + pipe.getName());
    }

    public void orderByConcentration() {
        Collections.sort(intervals, new Comparator<PollutionDischargeInterval>() {
            @Override
            public int compare(PollutionDischargeInterval o1, PollutionDischargeInterval o2) {
                if (o1.max_concentration < o2.max_concentration) {
                    return 1;
                }
                if (o1.max_concentration > o2.max_concentration) {
                    return -1;
                }
                if (o1.pollutionMass < o2.pollutionMass) {
                    return 1;
                }
                return -1;
            }
        });
    }

    public void orderByPollutionMass() {
        Collections.sort(intervals, new Comparator<PollutionDischargeInterval>() {
            @Override
            public int compare(PollutionDischargeInterval o1, PollutionDischargeInterval o2) {
                if (o1.pollutionMass < o2.pollutionMass) {
                    return 1;
                }
                if (o1.pollutionMass > o2.pollutionMass) {
                    return -1;
                }
                if (o1.max_concentration < o2.max_concentration) {
                    return 1;
                }
                return -1;
            }
        });
    }

    public void findMaximumIntervals(double maximumVolume) {
        double sumvolume = 0;
        boolean[] InSet = new boolean[intervals.size()];
        containedMass = 0;
        emittedMass = 0;
        containedVolume = 0;
        containedConcentrationMaximum = 0;
        emittedConcentrationMaximum = 0;
//        DecimalFormat df4 = new DecimalFormat("0.####");
        for (PollutionDischargeInterval interval : intervals) {
            sumvolume += interval.volume;
//            System.out.println(df4.format(interval.volume) + " m³ \t c: " + df4.format(interval.max_concentration) + "  mass: " + df4.format(interval.pollutionMass) + " kg in interval " + interval.intervalIndex + "    " + sumvolume + "  < " + maximumVolume);
            if (sumvolume <= maximumVolume) {
                InSet[interval.intervalIndex] = true;
                containedMass += ordered[interval.intervalIndex].pollutionMass;
                containedVolume += interval.volume;
                containedConcentrationMaximum = Math.max(containedConcentrationMaximum, interval.max_concentration);
            } else {
                emittedMass += ordered[interval.intervalIndex].pollutionMass;
                emittedConcentrationMaximum = Math.max(emittedConcentrationMaximum, interval.max_concentration);
            }
        }
        maximumIntervals = InSet;
    }

    public ArrayList<PollutionDischargeInterval> getMaximumIntervals() {
        PollutionDischargeInterval active = null;
        ArrayList<PollutionDischargeInterval> list = new ArrayList<>(intervals.size() / 5);
        if (maximumIntervals[0]) {
            active = new PollutionDischargeInterval();
            active.start = ordered[0].start;
            active.volume = ordered[0].volume;
            active.intervalIndex = 1;
            active.pollutionMass = ordered[0].pollutionMass;
            active.max_concentration = ordered[0].max_concentration;
        }
        for (int i = 1; i < maximumIntervals.length; i++) {
            if (maximumIntervals[i]) {
                if (active != null) {
                    //add this to the current active interval
                    active.intervalIndex++;
                    active.max_concentration = Math.max(active.max_concentration, ordered[i].max_concentration);
                    active.volume += ordered[i].volume;
                    active.pollutionMass += ordered[i].pollutionMass;
                    if (ordered[i].duration > 0) {
                        active.end = ordered[i].end;
                    }
                } else {
                    //Create new 
                    active = new PollutionDischargeInterval();
                    active.start = ordered[i].start;
                    active.volume = ordered[i].volume;
                    active.intervalIndex = 1;
                    active.pollutionMass = ordered[i].pollutionMass;
                    active.max_concentration = ordered[i].max_concentration;
                }
            } else {
                if (active != null) {
                    //Previous was the last n line. close this information and add it to the list
                    //add this to the current active interval
                    if (ordered[i - 1].duration > 0) {
                        active.end = ordered[i - 1].end;
                    }
                    active.duration = active.end - active.start;
                    list.add(active);
                    active = null;
                }
            }
        }
        if (active != null) {
//            System.out.println("Very last element is also in interval with start " + ordered[ordered.length - 1].start + " and end: " + ordered[ordered.length - 1].end);
            //Previous was the last n line. close this information and add it to the list
            //add this to the current active interval
            if (ordered[ordered.length - 1].duration > 0) {
                active.end = ordered[ordered.length - 1].end;
            }
            active.duration = active.end - active.start;
            list.add(active);
            active = null;
        }
        return list;
    }

    /**
     * Total mass trespassed of the pollution.
     *
     * @return kg
     */
    public double getMaximumMass() {
        return maximumMass;
    }

    /**
     * Mass contained in the selected intervals
     *
     * @return kg
     */
    public double getContainedMass() {
        return containedMass;
    }

    public class PollutionDischargeInterval {

        public int intervalIndex;
        public long start, end, duration;
        public double max_concentration;
        public double discharge, volume;
        public double pollutionMass;
    }

    /**
     * Total volume in the intervals until the target value
     *
     * @return
     */
    public double getContainedVolume() {
        return containedVolume;
    }

    /**
     * Maximum Concentration [kg/m^3] in the intervals below target volume.
     *
     * @return
     */
    public double getContainedConcentrationMaximum() {
        return containedConcentrationMaximum;
    }

    /**
     * Maximum Concentration [kg/m^3] outside of the intervals below target
     * volume.
     *
     * @return
     */
    public double getEmittedConcentrationMaximum() {
        return emittedConcentrationMaximum;
    }

    public double getEmittedMass() {
        return emittedMass;
    }

}
