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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
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
    double maximumVolume;
    PollutionDischargeInterval[] ordered;
    ArrayList<PollutionDischargeInterval> intervals;
    boolean[] maximumIntervals;

    double maximumMass, containedMass;

    public OutletMinimizer(Pipe pipe, double maximumVolume) {
        this.pipe = pipe;
        this.maximumVolume = maximumVolume;
        analyseIntervals();
    }

    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
        ordered = null;
        intervals = null;
        maximumIntervals = null;
        maximumMass = 0;
        containedMass = 0;
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
        intervals = new ArrayList<>(pipe.getMeasurementTimeLine().getContainer().getNumberOfTimes());

        ArrayTimeLineMeasurement tm = pipe.getMeasurementTimeLine();
        TimeLinePipe tl = pipe.getStatusTimeLine();
        ArrayTimeLineMeasurementContainer c = tm.getContainer();
        ordered = new PollutionDischargeInterval[c.getNumberOfTimes() - 1];
        maximumMass = 0;
        for (int i = 1; i < c.getNumberOfTimes(); i++) {
            long start = c.getMeasurementTimestampAtTimeIndex(i - 1);
            long ende = c.getMeasurementTimestampAtTimeIndex(i);

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

            int tlIndexStart = tl.getTimeContainer().getTimeIndex(start);
            int tlIndexEnd = tl.getTimeContainer().getTimeIndex(ende - 1);
            double dischargeS = tl.getDischarge(tlIndexStart);
            double dischargeE = tl.getDischarge(tlIndexEnd);

            double volume = (dischargeE + dischargeS) * 0.5 * durationS;
            double mass = (concentrationS + concentrationE) * 0.5 * volume;
            maximumMass += mass;
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
        System.out.println("Analysed intervals of Pipe " + pipe.getName());
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
        DecimalFormat df4=new DecimalFormat("0.####");
        for (PollutionDischargeInterval interval : intervals) {
            sumvolume += interval.volume;
            System.out.println(df4.format(interval.volume) + " m³ \t c: " + df4.format(interval.max_concentration)+"  mass: "+df4.format(interval.pollutionMass) + " kg in interval " + interval.intervalIndex + "    " + sumvolume + "  < " + maximumVolume);
            if (sumvolume <= maximumVolume) {
                InSet[interval.intervalIndex] = true;
                containedMass += ordered[interval.intervalIndex].pollutionMass;
            } else {
                break;
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
            System.out.println("Very last element is also in interval with start " + ordered[ordered.length - 1].start + " and end: " + ordered[ordered.length - 1].end);
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

}
