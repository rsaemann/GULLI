package com.saemann.gulli.core.io;

import com.jmatio.io.MatFileFilter;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLSingle;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineManhole;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.gulli.core.model.topology.profile.RectangularProfile;
import org.locationtech.jts.geom.Coordinate;

/**
 * Reads MET files with the help of the old and slow jmatio.jar.
 * @author saemann
 */
public class MatlabIO_jmatio {

    MatFileReader mfr;

    private Scenario scenario;

    public long[] times;

//    private boolean swapbytes = true;
    public MatlabIO_jmatio(File f) throws IOException {

        long start = System.currentTimeMillis();

//        MatFileFilter filter = new MatFileFilter(new String[]{"x1", "t2", "vs", "hs", "phis", "m1s", "m2s","m1s_2","m2s_2"});
 MatFileFilter filter = new MatFileFilter(new String[]{"x1", "t2", "vs", "hs", "phis", "m1s_c", "m2s_c","m1s_m","m2s_m"});

        mfr = new MatFileReader();
        System.out.println((System.currentTimeMillis() - start) + "ms\tinit MatlabFileReader...");
        mfr.read(f, filter, 2);
        System.out.println((System.currentTimeMillis() - start) + "ms\t MatlabFileReader loaded.");
//        System.exit(-1);
    }
    public Network readNetwork(File f, int numberOfParticles) throws IOException, Exception {
        return readNetwork(f, numberOfParticles, -1);
    }
    
    public Network readNetwork(File f, int numberOfParticles,int maxSeconds) throws IOException, Exception {
        long start = System.currentTimeMillis();

        MLArray t2a = mfr.getMLArray("t2");
//        MLArray m1s = mfr.getMLArray("m1s_2");
//        MLArray m2s = mfr.getMLArray("m2s_2");
        
        MLArray m1s = mfr.getMLArray("m1s_m");
        MLArray m2s = mfr.getMLArray("m2s_m");
        
        MLDouble t2 = (MLDouble) t2a;
        MLDouble m1 = (MLDouble) m1s;
        MLDouble m2 = (MLDouble) m2s;
        
        if(m1==null){
            System.out.println("Moment1 is null");
        }else if (m1.isEmpty()){
            System.out.println("Moment1 is empty");
        }else{
            System.out.println("Moment1 has "+m1.getSize()+" elements");
        }
        
        if(m2==null){
            System.out.println("Moment2 is null");
        }else if (m2.isEmpty()){
            System.out.println("Moment2 is empty");
        }else{
            System.out.println("Moment2 has "+m2.getSize()+" elements");
        }
        
        int numberOfTimes = t2.getSize();
        if(maxSeconds>0){
            for (int i = 0; i < numberOfTimes; i++) {
                if(t2.get(i)>maxSeconds){
                    numberOfTimes=i;
                    System.out.println("found last index is:"+i+" : "+t2.get(i)+"s>"+maxSeconds+"s");
                    break;
                }
            }
        }
        
//        System.out.println("first time: " + new Date(cal.getTimeInMillis()) + "   + times(1): " + t2.get(0));
        times = new long[numberOfTimes];
        for (int i = 0; i < times.length-1; i++) {
            times[i + 1] = (long) (((t2.get(i)) * 1000));
        }
        System.out.println("times: use "+times.length+" of "+t2.getSize());
        System.out.println("starttime 0 " + times[0] + " , " + times[1] + " , " + times[2]+" .... "+times[times.length-1]);
//        t2a = null;
        //Fließwerte für t2 auslesen
        MLSingle vs = (MLSingle) mfr.getMLArray("vs");
        MLSingle hs = (MLSingle) mfr.getMLArray("hs");
        MLSingle phis = (MLSingle) mfr.getMLArray("phis");

        MLSingle b = (MLSingle) mfr.getMLArray("b"); //width [m]
        float channelwidth = 1;
        if (b != null) {
            channelwidth = b.get(0);
        }

        //Ortsdiskretisierung aufbauen
        MLArray x1 = mfr.getMLArray("x1");
        System.out.println((System.currentTimeMillis() - start) + "ms\tType of 'x1' is " + x1.getClass());
        MLDouble x1d = (MLDouble) x1;
        System.out.println("x1 size: " + x1d.getSize());
        RectangularProfile rec = new RectangularProfile(1, 10);
        CircularProfile circ = new CircularProfile(1);
        int lengthX = x1d.getSize();
        double letztesX = x1d.get(0);

        System.out.println("x :" + x1.getM() + "x" + x1.getN());
        System.out.println("vs:" + vs.getM() + "x" + vs.getN());
        System.out.println("hs:" + hs.getM() + "x" + hs.getN());
        System.out.println("phi:" + phis.getM() + "x" + phis.getN());

        int numberOfManholes = Math.min(x1d.getN(), phis.getN());

        ArrayList<Manhole> manholes = new ArrayList<>(numberOfManholes + 1);
        ArrayList<Pipe> pipes = new ArrayList<>(numberOfManholes);
        // create StatusTimeline for pipes
        System.out.println("create timelines");
        final ArrayTimeLinePipeContainer pipeTLcontainer = new ArrayTimeLinePipeContainer(times, numberOfManholes - 1, 1);

        System.out.println("pipes ok, start manholes");
        final ArrayTimeLineManholeContainer manholeTLcontainer = new ArrayTimeLineManholeContainer(pipeTLcontainer, numberOfManholes);
        System.out.println("ok");
        GeoTools gt = new GeoTools("EPSG:4326", "EPSG:31467", StartParameters.JTS_WGS84_LONGITUDE_FIRST);// WGS84(lat,lon) <-> Gauss Krüger Zone 3(North,East)
        double dx = x1d.get(1) - x1d.get(0);
        System.out.println("dx= " + dx + "m");
        System.out.println("x(0)=" + x1d.get(0) + "\t x(size(x)-1)=" + x1d.get(x1d.getSize() - 1));
        double x = -(x1d.get(1) - x1d.get(0)) * 0.5;
        double y = 0;
        Coordinate coord = new Coordinate(x, y);
        coord = gt.toGlobal(coord);
        Position neuePos = new Position(coord.x, coord.y, x, y);
        Position letztePos = neuePos;
        Manhole letztesMH = new Manhole(letztePos, "MH_Start", circ);
        manholes.add(letztesMH);

//        ManholeStamp[] letzte_mhstamps = null;
//        CustomValue matlabConcentration = new CustomValue("c_Matlab", "c_mat", "kg/m³");
        Material material = new Material("Matlab", 1000, true);
        final ArrayList<InjectionInfo> injections = new ArrayList<>(lengthX);
//        System.out.println("Load x from 0 to "+lengthX);
        int skipped = 0;
        float[] distancesX = new float[numberOfManholes - 1];
        ArrayTimeLineMeasurementContainer.distance = distancesX;
        pipeTLcontainer.distance = distancesX;
        double mass = 0;
        System.out.println(" phis: " + phis.getNDimensions() + " M:" + phis.getM() + "  N:" + phis.getN() + "   x: " + distancesX.length);
        for (int i = 0; i < numberOfManholes; i++) {
            if (phis.get(1, i) > 0) {
                mass += phis.get(1, i) * hs.get(1, i) * dx * channelwidth;
            }
        }
        double massPerParticle = mass / (double) numberOfParticles;
        System.out.println("Matlab mass: " + mass + " kg => \t" + massPerParticle + " kg/particle");

        TimeLineManhole tlmh = new TimeLineManhole() {

            @Override
            public float getWaterZ(int temporalIndex) {
                return 2;
            }

            @Override
            public boolean isWaterlevelIncreasing() {
                return false;
            }

            @Override
            public float getActualWaterZ() {
                return 2;
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
                return times.length;
            }

            @Override
            public TimeContainer getTimeContainer() {
                return pipeTLcontainer;
            }

            @Override
            public float getActualWaterLevel() {
                return 2f;
            }

            @Override
            public float getInflow(int temporalIndex) {
                return 0;
            }
        };

        double tempMassSum = 0;
        int counterParticles = 0;
        

        for (int i = 1; i < numberOfManholes; i++) {
//            if (i > 1000) {
//                System.out.println("break loading pipes at pipe nr " + i);
//                break;
//            }
            double neuesX = x1d.get(i);
            double posX = (neuesX + letztesX) * 0.5;
            coord = new Coordinate(posX, y);
            coord = gt.toGlobal(coord);
            neuePos = new Position(coord.x, coord.y, posX, y);
            distancesX[i - 1] = i + 0.5f;// - 1f;

            Manhole neuesMH = new Manhole(neuePos, "MH_" + i, circ);
            neuesMH.setTop_height(1);
            neuesMH.setSole_height(0);
            neuesMH.setSurface_height(2);
            manholes.add(neuesMH);

            neuesMH.setStatusTimeline(new ArrayTimeLineManhole(manholeTLcontainer, i));
            Connection_Manhole_Pipe c_start = new Connection_Manhole_Pipe(letztesMH, 0);
            Connection_Manhole_Pipe c_end = new Connection_Manhole_Pipe(neuesMH, 0);
            Pipe p = new Pipe(c_start, c_end, rec);
            letztesMH.addConnection(c_start);
            neuesMH.addConnection(c_end);
            p.setManualID(i);
            p.setName("Pipe_" + i);
            p.setLength((float) dx);

            try {
                ArrayTimeLinePipe tl = new ArrayTimeLinePipe(pipeTLcontainer, i - 1);
                tl.setVelocity(vs.get(1, i), 0);
                tl.setWaterlevel(hs.get(1, i), 0);
                tl.setDischarge(vs.get(1, i) * hs.get(1, i) * channelwidth, 0);
                tl.setVolume(hs.get(1, i) * channelwidth * p.getLength(), 0);
                tl.setMassflux_reference(phis.get(1, i) * vs.get(1, i) * hs.get(1, i) * channelwidth, 0, 0);
                for (int t = 1; t < numberOfTimes; t++) {
                    float v = vs.get(t, i);
                    tl.setVelocity(v, t);
                    float h = hs.get(t, i);
                    tl.setWaterlevel(h, t);
                    float vol = (float) (h * channelwidth * p.getLength());
                    tl.setVolume(vol, t);
                    tl.setDischarge((float) (v * h * channelwidth), t);
                    float phi = phis.get(t, i); // kg?
                    tl.setMassflux_reference((float) (phi * v * h * channelwidth), t, 0);
                    tl.setConcentration_Reference(phi, t, 0);
                }
                if (i < numberOfManholes - 6) {
                    //Move the injection position upstream, so the intital mass is equal to the matlab reference injection.
                    try {
                        double particlemass = phis.get(1, i + 5) * hs.get(1, i) * dx * channelwidth;
                        tempMassSum += particlemass;
                        int particles = (int) ((tempMassSum / mass) * numberOfParticles); //Number of particles that have to be released up to here
                        if (particles > counterParticles) {
                            //Insert the amount of missing number of particles
                            int nparticles = particles - counterParticles;
                            InjectionInformation inj = new InjectionInformation(p, 0 * dx / 2., nparticles * massPerParticle/* particlemass*/, nparticles, material, 0, 0);//p.getLength() * 0.5,material,particlemass,  
                            counterParticles += nparticles;
                            injections.add(inj);
                        }
                    } catch (Exception e) {
                        System.err.println("Buffer exeption for index " + i + "+5 / " + phis.getSize());
                    }
                }

                p.setStatusTimeLine(tl);
                tl.calculateMaxMeanValues();

                letztesMH.setStatusTimeline(tlmh);

                pipes.add(p);

                letztesMH = neuesMH;
                letztesX = neuesX;

            } catch (Exception e) {
                System.err.println("Matlab Array too big @x=" + i);
                e.printStackTrace();

                break;
            }

        }
        System.out.println("inserted " + counterParticles + " particles with total " + tempMassSum + " kg / " + mass);

        //Test total mass of particles
        double masssum = 0;
        for (InjectionInfo injection : injections) {
            masssum += injection.getMass();
        }
        System.out.println("sum of injections mass in matlab file: " + masssum + " kg;");

        pipeTLcontainer.moment1 = new float[times.length];
        pipeTLcontainer.moment2 = new float[times.length];
        pipeTLcontainer.moment1[0] = m1.get(1).floatValue();
        pipeTLcontainer.moment2[0] = m2.get(1).floatValue();
//        System.out.println("refM1(0)=" + m1.get(0).floatValue());
//        System.out.println("refM1(1)=" + m1.get(1).floatValue());
//        System.out.println("refM1(2)=" + m1.get(2).floatValue());
//        System.out.println("refM1(3)=" + m1.get(3).floatValue());
        for (int i = 1; i < times.length; i++) {
            try {
                pipeTLcontainer.moment1[i] = m1.get(i).floatValue();
                pipeTLcontainer.moment2[i] = m2.get(i).floatValue();
            } catch (Exception e) {
            }
        }
//        System.out.println("refM1TL(0)=" + pipeTLcontainer.moment1[0]);
//        System.out.println("refM1TL(1)=" + pipeTLcontainer.moment1[1]);
//        System.out.println("refM1TL(2)=" + pipeTLcontainer.moment1[2]);

        letztesMH.setAsOutlet(true);

        System.gc();
        Network nw = new Network(pipes, manholes);
        this.scenario = new Scenario() {

            @Override
            public ArrayList<InjectionInfo> getInjections() {
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

        scenario.setStatusTimesPipe(pipeTLcontainer);

        return nw;
    }

    public Scenario getScenario() {
        if (scenario == null) {
            throw new IllegalStateException("Scenario is not yet ready to load. Call 'readNetwork' before asking for the scenario.");
        }
        return scenario;
    }
}
