/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
package io;

import com.vividsolutions.jts.geom.Coordinate;
import control.Controller;
import control.StartParameters;
import control.scenario.Scenario;
import control.scenario.SpillScenario;
import control.scenario.injection.InjectionInformation;
import java.io.IOException;
import java.util.ArrayList;
import model.GeoTools;
import model.particle.Material;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.timeline.array.TimeContainer;
import model.timeline.array.TimeIndexContainer;
import model.timeline.array.TimeLineManhole;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.profile.CircularProfile;
import model.topology.profile.RectangularProfile;

/**
 * Create simple channels for comparison between numerical Code and analytical
 * solution.
 *
 * @author Robert Sämann
 */
public class AnalyticalChannel {

    public float slope = 0, width = 1, totallength = 1000, segmentlength = 1;
    public float waterlevel = 1, velocity = 1;
    public float disp = 0; //[m^2/s]
    int numberOfChannelElements, numberOfManholes;

    int numberOfTimeIntervals, timeintervallengthMS;

//    public int numberOfParticles = 10000;
    //Injection parameter for analytical solution, rectangular spill:
    float totalmassKG = 10;
    float spillMidM = 60;
    float spillWidthM = 20;

    float[][] c;
    float[] distancesX;

    private Scenario scenario;
    TimeIndexContainer times;

    private ArrayList<InjectionInformation> injections;
    private ArrayTimeLinePipe[] timelinesPipe;
    private ArrayTimeLinePipeContainer pipeTLcontainer;

    ArrayList<Manhole> manholes;
    ArrayList<Pipe> pipes;

    public float massPerParticle = 1;

    Material material;

    public AnalyticalChannel() {
        material = new Material("Analytical solution", 1000, true, 0);
    }

    public AnalyticalChannel(float width, float length, int simulationDurationS, float timeIntervalDurationS) {
        this();
        this.slope = slope;
        this.width = width;
        this.totallength = length;

        this.numberOfTimeIntervals = (int) (simulationDurationS / timeIntervalDurationS);
        this.timeintervallengthMS = (int) (timeIntervalDurationS * 1000);
        injections = new ArrayList<InjectionInformation>();
        System.out.println("create timelines");

        long[] times_ = new long[numberOfTimeIntervals + 1];
        for (int i = 0; i < times_.length; i++) {
            times_[i] = (long) (i * timeintervallengthMS);
        }
        times = new TimeIndexContainer(times_);
        scenario = new SpillScenario(times, injections);
        scenario.setTimesManhole(times);
    }

    public Network createNetwork(float segmentLength) throws IOException, Exception {
        long start = System.currentTimeMillis();
        this.segmentlength = segmentLength;
        //Ortsdiskretisierung aufbauen
        RectangularProfile rec = new RectangularProfile(width, 10);
        CircularProfile circ = new CircularProfile(1);//for manholes
        numberOfChannelElements = (int) (totallength / segmentLength);
        numberOfManholes = numberOfChannelElements + 1;
        double letztesX = 0;

        manholes = new ArrayList<>(numberOfChannelElements + 1);
        pipes = new ArrayList<>(numberOfChannelElements);
        // create StatusTimeline for pipes

        pipeTLcontainer = new ArrayTimeLinePipeContainer(times, numberOfChannelElements, 1);
        System.out.println("pipes ok, start manholes");
        final ArrayTimeLineManholeContainer manholeTLcontainer = new ArrayTimeLineManholeContainer(pipeTLcontainer, numberOfManholes);
        System.out.println("ok");
        GeoTools gt = new GeoTools("EPSG:4326", "EPSG:31467", StartParameters.JTS_WGS84_LONGITUDE_FIRST);// WGS84(lat,lon) <-> Gauss Krüger Zone 3(North,East)
        double dx = segmentLength;
        System.out.println("dx= " + dx + "m");
        double x = 0;
        double y = 0;
        Coordinate coord = new Coordinate(x, y);
        coord = gt.toGlobal(coord);
        Position neuePos = new Position(coord.x, coord.y, x, y);
        Position letztePos = neuePos;
        Manhole letztesMH = new Manhole(letztePos, "MH_Start", circ);
        letztesMH.setAsOutlet(true);
        manholes.add(letztesMH);

        Material material = new Material("Matlab", 1000, true);
//        final ArrayList<InjectionInformation> injections = new ArrayList<>(lengthX);
//        System.out.println("Load x from 0 to "+lengthX);
        int skipped = 0;
        distancesX = new float[numberOfChannelElements - 1];
        ArrayTimeLineMeasurementContainer.distance = distancesX;
        pipeTLcontainer.distance = distancesX;

        c = new float[numberOfChannelElements][numberOfTimeIntervals];

        //  double massPerParticle = totalmassKG / (double) numberOfParticles;
//        System.out.println("Matlab mass: " + mass + " kg => \t" + massPerParticle + " kg/particle");
        timelinesPipe = new ArrayTimeLinePipe[numberOfChannelElements];

        double tempMassSum = 0;
        int counterParticles = 0;
        float c = 0;

        ArrayTimeLinePipe tl = new ArrayTimeLinePipe(pipeTLcontainer, 0);

        for (int t = 0; t < times.getNumberOfTimes(); t++) {
            float v = velocity;
            tl.setVelocity(v, t);
            float h = waterlevel;
            tl.setWaterlevel(h, t);
            float vol = (float) (h * width * dx);
            tl.setVolume(vol, t);
            tl.setDischarge((float) (v * h * width), t);
//            for (int i = 1; i < numberOfChannelElements; i++) {
//                pipeTLcontainer.velocity[i*numberOfTimeIntervals+t]=v;
//                pipeTLcontainer.waterlevel[i*numberOfTimeIntervals+t]=h;
//                pipeTLcontainer.volume[i*numberOfTimeIntervals+t]=vol;
//            }
        }

        TimeLineManhole tlmh = new TimeLineManhole() {

            @Override
            public float getWaterZ(int temporalIndex) {
                return waterlevel;
            }

            @Override
            public boolean isWaterlevelIncreasing() {
                return false;
            }

            @Override
            public float getActualWaterZ() {
                return waterlevel;
            }

            @Override
            public float getFlowToSurface(int temporalIndex) {
                return 0;
            }

            @Override
            public float getActualFlowToSurface() {
                return 0;
            }

            @Override
            public int getNumberOfTimes() {
                return numberOfTimeIntervals;
            }

            @Override
            public TimeContainer getTimeContainer() {
                return pipeTLcontainer;
            }

            @Override
            public float getActualWaterLevel() {
                return waterlevel;
            }
        };

        for (int i = 1; i < numberOfChannelElements; i++) {

            double posX = i * dx;
            coord = new Coordinate(posX, y);
            coord = gt.toGlobal(coord);
            neuePos = new Position(coord.x, coord.y, posX, y);
            distancesX[i - 1] = (i - 1) * segmentLength;//-0.5*segmentLength;

            Manhole neuesMH = new Manhole(neuePos, "MH_" + i, circ);
            neuesMH.setTop_height(1);
            neuesMH.setSole_height(0);
            neuesMH.setSurface_height(2);
            manholes.add(neuesMH);

            neuesMH.setStatusTimeline(tlmh);//new ArrayTimeLineManhole(manholeTLcontainer, i));
            Connection_Manhole_Pipe c_start = new Connection_Manhole_Pipe(letztesMH, 0);
            Connection_Manhole_Pipe c_end = new Connection_Manhole_Pipe(neuesMH, 0);
            Pipe p = new Pipe(c_start, c_end, rec);
            letztesMH.addConnection(c_start);
            neuesMH.addConnection(c_end);
            p.setManualID(i);
            p.setName("Pipe_" + i);
            p.setLength((float) dx);

            try {
//                ArrayTimeLinePipe tl = new ArrayTimeLinePipe(pipeTLcontainer, i - 1);
                tl = new ArrayTimeLinePipe(pipeTLcontainer, i-1);

                for (int t = 0; t < times.getNumberOfTimes(); t++) {
                    float v = velocity;
                    tl.setVelocity(v, t);
                    float h = waterlevel;
                    tl.setWaterlevel(h, t);
                    float vol = (float) (h * width * dx);
                    tl.setVolume(vol, t);
                    tl.setDischarge((float) (v * h * width), t);
//            for (int i = 1; i < numberOfChannelElements; i++) {
//                pipeTLcontainer.velocity[i*numberOfTimeIntervals+t]=v;
//                pipeTLcontainer.waterlevel[i*numberOfTimeIntervals+t]=h;
//                pipeTLcontainer.volume[i*numberOfTimeIntervals+t]=vol;
//            }
                }
                timelinesPipe[i-1] = tl;
//                for (int t = 0; t < times.getNumberOfTimes(); t++) {
//                    float v = velocity;
//                    tl.setVelocity(v, t);
//                    float h = waterlevel;
//                    tl.setWaterlevel(h, t);
//                    float vol = (float) (h * width * p.getLength());
//                    tl.setVolume(vol, t);
//                    tl.setDischarge((float) (v * h * width), t);
//                }

                p.setStatusTimeLine(tl);
                tl.calculateMaxMeanValues();

                letztesMH.setStatusTimeline(tlmh);

                pipes.add(p);

                letztesMH = neuesMH;
                letztesX = posX;

            } catch (Exception e) {
                System.err.println("Matlab Array too big @x=" + i);
                e.printStackTrace();

                break;
            }

        }
        manholes.get(manholes.size() - 1).setAsOutlet(true);
        letztesMH.setAsOutlet(true);

        System.gc();
        Network nw = new Network(pipes, manholes);
        this.scenario = new Scenario() {

            @Override
            public ArrayList<InjectionInformation> getInjections() {
                return injections;
            }

            @Override
            public void init(Controller c) {

            }

            @Override
            public void reset() {
            }

        };
        scenario.setTimesManhole(manholeTLcontainer);

        scenario.setTimesPipe(pipeTLcontainer);

        return nw;
    }

    public Scenario getScenario() {
        if (scenario == null) {
            throw new IllegalStateException("Scenario is not yet ready to load. Call 'readNetwork' before asking for the scenario.");
        }
        return scenario;
    }

    public void addContaminationSuperposition(int injectionElementIndex, int injectionTimeIndex, float mass) {
        double x_inj = distancesX[injectionElementIndex];// injectionElementIndex * segmentlength;// + segmentlength * 0.5;
        double t_injMS = injectionTimeIndex * timeintervallengthMS;

        double v_e = width * waterlevel * segmentlength;
        double c_ini = mass / v_e;
        //System.out.println("c_ini= " + c_ini + " kg/m³  from " + mass + " kg in " + width + "x" + waterlevel + "x" + segmentlength + "=" + v_e + "m^3 volume");

        for (int it = injectionTimeIndex; it < numberOfTimeIntervals; it++) {
            float t = (float) ((it * timeintervallengthMS - t_injMS) * 0.001); //Seconds since injection
//            System.out.println(it+": "+t+"s");
            if (t == 0) {
                t = 0.001f;
//                //only set the initial concentration at the initial position
                c[injectionElementIndex][injectionTimeIndex] += c_ini;//* 0.5;
//                c[injectionElementIndex + 1][injectionTimeIndex] += c_ini * 0.5;

                continue;
            }

            double sqrt4pidt = Math.sqrt(4 * Math.PI * disp * t);
            if (disp == 0) {
                //System.out.println("Dispersion D=0. Analytical solution  cannot be calculated (div by 0)");
                double x = x_inj + velocity * t;
                double fraction = (x / segmentlength);
                int indexFloor = (int) fraction;
                c[indexFloor][it] += c_ini * (1 - (fraction % 1));
                c[indexFloor + 1][it] += c_ini * fraction % 1;

            } else {
                for (int i = 0; i < numberOfChannelElements; i++) {
                    float x = (float) ((i) * segmentlength - x_inj);
                    float addC = (float) ((c_ini / (sqrt4pidt)) * Math.exp(-(x - velocity * t) * (x - velocity * t) / (4 * disp * t)));
                    c[i][it] += addC * 1;
                }
            }
        }

//        System.out.println("Add "+mass+"kg to X="+distancesX[injectionElementIndex]);
        injections.add(new InjectionInformation(pipes.get(injectionElementIndex), 0.5 * segmentlength, (float) mass, (int) (mass / massPerParticle), material, 0/*(injectionTimeIndex * timeintervallengthMS) / 1000.*/, 0/*timeintervallengthMS * 0.001*/));
//        injections.add(new InjectionInformation(pipes.get(injectionElementIndex-1),  0*segmentlength, (float) mass*0.5f, (int) (mass*0.5 / massPerParticle), material, (injectionTimeIndex * timeintervallengthMS) / 1000., 0/*timeintervallengthMS * 0.001*/));
    }

    public void resetConcentration() {
        this.c = new float[numberOfChannelElements][numberOfTimeIntervals];
    }

    public void addRectangularProfile(float startX, float stopX, float concentration, int timeindex) {
        float totalmass = concentration * segmentlength * width * waterlevel;
        for (int i = 0; i < numberOfChannelElements; i++) {
            float from = i * segmentlength;
            float to = from + segmentlength;
            if (from > stopX) {
                continue;
            }
            if (to < startX) {
                continue;
            }
            boolean total = false;
            if (from >= startX && to <= stopX) {
                total = true;
            }
            if (total) {
                addContaminationSuperposition(i, timeindex, totalmass);
            } else {
                //Calculate fraction of mass to be injected.
                float freespace = 0;
                if (startX > from && startX < to) {
                    freespace = startX - from;
                }
                if (stopX < to && stopX > from) {
                    freespace += to - stopX;
                }
                addContaminationSuperposition(i, timeindex, totalmass * (segmentlength - freespace) / segmentlength);
            }
        }
    }

    public void fillPipeTimelinesWithAnalyticalValues() {
        float[] momentum1 = new float[numberOfTimeIntervals];
        float[] momentum2 = new float[momentum1.length];
        float[] sumC = new float[numberOfTimeIntervals];
        timelinesPipe[0].container.moment1 = momentum1;
        timelinesPipe[0].container.moment2 = momentum2;

        for (int i = 0; i < numberOfChannelElements; i++) {
            ArrayTimeLinePipe tl = timelinesPipe[i];
            if (tl == null) {
                System.out.println("Timeline[" + i + "} is null");
                continue;
            }
            float[] cp = c[i];
            if (cp == null) {
                System.out.println("analytical concentration [" + i + "] is null");
                continue;
            }
            float x = (float) ((i) * segmentlength);
            for (int j = 0; j < numberOfTimeIntervals; j++) {
                tl.setConcentration_Reference(cp[j], j, 0);
                tl.setMassflux_reference(cp[j] * tl.getDischarge(j), j, 0);
                momentum1[j] += cp[j] * x;
                sumC[j] += cp[j];
            }
        }
        for (int i = 0; i < numberOfTimeIntervals; i++) {
            momentum1[i] = momentum1[i] / sumC[i];
        }
        //2. Momentum
        for (int t = 0; t < numberOfTimeIntervals; t++) {
            for (int j = 0; j < numberOfChannelElements; j++) {
                float x = (float) ((j) * segmentlength);
                momentum2[t] += c[j][t] * (x - momentum1[t]) * (x - momentum1[t]);
            }
            momentum2[t] = momentum2[t] / sumC[t];
        }

    }

}
