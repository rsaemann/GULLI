package io;

import com.jmatio.io.MatFileFilter;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLSingle;
import com.vividsolutions.jts.geom.Coordinate;
import control.Controller;
import control.StartParameters;
import control.scenario.injection.InjectionInformation;
import control.scenario.Scenario;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import model.GeoTools;
import model.particle.Material;
import model.timeline.array.ArrayTimeLineManhole;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.timeline.array.TimeContainer;
import model.timeline.array.TimeLineManhole;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.profile.CircularProfile;
import model.topology.profile.RectangularProfile;

/**
 *
 * @author saemann
 */
public class MatlabIO {

    MatFileReader mfr;

    private Scenario scenario;

    public long[] times;

//    private boolean swapbytes = true;
    public MatlabIO(File f) throws IOException {

        long start = System.currentTimeMillis();

        MatFileFilter filter = new MatFileFilter(new String[]{"x1", "t2", "vs", "hs", "phis", "m1s", "m2s"});

        mfr = new MatFileReader();
        System.out.println((System.currentTimeMillis() - start) + "ms\tinit MatlabFileReader...");
        mfr.read(f, filter, 2);
        System.out.println((System.currentTimeMillis() - start) + "ms\t MatlabFileReader loaded.");
//        System.exit(-1);
    }

    public Network readNetwork(File f, int numberOfParticles) throws IOException, Exception {
        long start = System.currentTimeMillis();
//        int numberOfParticles = 100000;
//        Particle.massPerParticle =1./10.;
        //Zeitvektor t2 (sekunden) aufbauen
//        GregorianCalendar cal = new GregorianCalendar();
//        cal.set(Calendar.HOUR_OF_DAY, 0);
//        cal.set(Calendar.MINUTE, 0);
//        cal.set(Calendar.SECOND, 0);
//        cal.set(Calendar.MILLISECOND, 0);

        MLArray t2a = mfr.getMLArray("t2");
        MLArray m1s = mfr.getMLArray("m1s");
        MLArray m2s = mfr.getMLArray("m2s");
//        System.out.println((System.currentTimeMillis() - start) + "ms\tType of 't2' is " + t2a.getClass());
        MLDouble t2 = (MLDouble) t2a;
        MLDouble m1 = (MLDouble) m1s;
        MLDouble m2 = (MLDouble) m2s;
//        System.out.println("first time: " + new Date(cal.getTimeInMillis()) + "   + times(1): " + t2.get(0));
        times = new long[t2.getSize() + 1];
        for (int i = 0; i < times.length - 1; i++) {
            times[i + 1] = (long) (((t2.get(i)) * 1000));
        }

        System.out.println("starttime 0 " + times[0] + " , " + times[1] + " , " + times[2]);
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
        final ArrayList<InjectionInformation> injections = new ArrayList<>(lengthX);
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
        };

        double tempMassSum = 0;
        int counterParticles = 0;
        int numberOfTimes= t2.getSize();

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
            distancesX[i - 1] = i+0.5f;// - 1f;

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
                for (int t = 1; t <numberOfTimes; t++) {
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
                    try {
                        double particlemass = phis.get(1, i + 5) * hs.get(1, i) * dx * channelwidth;
                        tempMassSum += particlemass;
                        int particles = (int) ((tempMassSum/mass) * numberOfParticles); //Number of particles that have to be released up to here
                        if (particles > counterParticles) {
                            //Insert the amount of missing number of particles
                            int nparticles = particles - counterParticles;
                            InjectionInformation inj = new InjectionInformation(p, 0*dx / 2., nparticles * massPerParticle/* particlemass*/, nparticles, material, 0, 0);//p.getLength() * 0.5,material,particlemass,  
                            counterParticles += nparticles;
                            injections.add(inj);
                        }
                    } catch (Exception e) {
                        System.err.println("Buffer exeption for index "+i+"+5 / "+phis.getSize());
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
        System.out.println("inserted " + counterParticles + " particles with total " + tempMassSum + " kg / "+mass);

        //Test total mass of particles
        double masssum = 0;
        for (InjectionInformation injection : injections) {
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
        for (int i = 1; i < times.length - 1; i++) {
            pipeTLcontainer.moment1[i] = m1.get(i).floatValue();
            pipeTLcontainer.moment2[i] = m2.get(i).floatValue();
        }
//        System.out.println("refM1TL(0)=" + pipeTLcontainer.moment1[0]);
//        System.out.println("refM1TL(1)=" + pipeTLcontainer.moment1[1]);
//        System.out.println("refM1TL(2)=" + pipeTLcontainer.moment1[2]);

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

//    public static void main(String[] args) {
//        File f = new File("J:/welle_klein.mat");//"X:\\Testmodell\\MATLAB\\welle_klein5000.mat");
//        try {
//            ParticlePipeComputing.setDispersionCoefficient(100);
//
//            ArrayTimeLineMeasurement.useIDsharpParticleCounting = false;
//            MatlabIO mio = new MatlabIO(f);
//            final Network nw = mio.readNetwork(f);
//            Controller c = new Controller(true);
//            c.importNetwork(nw);
//            c.getThreadController().setDeltaTime(1);
//            c.loadScenario(mio.getScenario());
//            System.out.println("measurementTimeline gets " + mio.times.length + " intervals.");
//            c.initMeasurementTimelines(mio.getScenario(), 240);
//            c.getMapFrame().getMapViewer().recalculateShapes();
//            c.recalculateInjections();
//
//            //Calculate particles total mass
//            double pmass = 0;
//            for (ParticleThread pT : c.getThreadController().getParticleThreads()) {
//                for (Particle p : pT.waitingList) {
//                    pmass += p.particleMass;
//                }
//            }
//            System.out.println("Total particle mass is " + pmass + " kg");
//
//            c.openSpatialLineFrame((ArrayTimeLinePipe) nw.getPipes().iterator().next().getStatusTimeLine());
////            c.getMapFrame().getMapViewer().zoomToFitLayer();
//            Position pos = nw.getManholes().iterator().next().getPosition();
//            c.getMapFrame().getMapViewer().setDisplayPositionByLatLon(pos.getLatitude(), pos.getLongitude(), 15);
//            c.getMapFrame().repaint();
////            ArrayTimeLineMeasurementContainer.instance.OnlyRecordOncePerTimeindex();
//            System.out.println("messungen pro zeitschritt: " + ArrayTimeLineMeasurementContainer.instance.samplesPerTimeinterval);
////            System.out.println("surrounding:"+c.getThreadController().barrier_particle.getThreads().get(0).waitingList.getFirst().injectionSurrounding);
//            c.start();
//
////            JFrame frame=new JFrame("Spaceline");
////            frame.setBounds(300,300,500,300);
////            SpacelinePanel sPanel=new SpacelinePanel(null, null);
////            frame.add(sPanel);
////            frame.setVisible(true);
////            c.calculateParticlesFromTimestep(tl.getStamp(1).getTimeStamp(), 5.);
//        } catch (IOException ex) {
//            Logger.getLogger(MatlabIO.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (Exception ex) {
//            Logger.getLogger(MatlabIO.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//    public static void main2(String[] args) {
//        FileReader fr = null;
//        try {
//            System.out.println("Matlab .mat File decoding");
//            File file = new File(".\\input\\matlab_klein5000.mat");
//            fr = new FileReader(file);
//            DataInputStream dis = new DataInputStream(new FileInputStream(file));
//            BufferedReader br = new BufferedReader(fr);
//            byte[] header = new byte[128];
//            dis.read(header);
////            br.read(header);
//            System.out.println(new String(header));
//            while (dis.available() > 0) {
//                boolean compressed = false;
//                int datatype = Integer.reverseBytes(dis.readInt());
//                int numberofBytes = Integer.reverseBytes(dis.readInt());
//                System.out.print("Format: " + datatype + " number: " + numberofBytes + " content: ");
//                int[] content = new int[numberofBytes / 4];
//                if (datatype == 15) {
//                    compressed = true;
//                    content = new int[numberofBytes - 4];
//                    System.out.print("Compressed!  ");
//                    //Inner header
////                    int bytes=(dis.readShort());
////                    int data=dis.readShort();
//                    System.out.print("Bytes:" + dis.readByte() + "," + dis.readByte() + ", data:" + dis.readByte() + "," + dis.readByte() + "  >>");
//                }
//
//                for (int i = 0; i < content.length; i++) {
//                    if (compressed) {
//                        content[i] = (dis.readByte());
//                    } else {
//                        content[i] = Integer.reverseBytes(dis.readInt());
//                    }
//                    System.out.print(content[i] + ",");
//                }
//                System.out.println(" <<<");
////               int datatype2=Integer.reverse(dis.readByte());
////               int datatype3=Integer.reverse(dis.readByte());
////                System.out.println("Datatype: "+datatype0+" "+datatype1+" "+datatype2+" "+datatype3+" ");
////                
////                
//////                System.out.println(dis.readInt());
////                int numbers0=Integer.reverseBytes(dis.readByte());
////               int numbers1=Integer.reverseBytes(dis.readByte());
////               int numbers2=Integer.reverseBytes(dis.readByte());
////               int numbers3=Integer.reverseBytes(dis.readByte());
////                System.out.println("Numbers: "+numbers0+" "+numbers1+" "+numbers2+" "+numbers3+" ");
////                while(true){
////                    System.out.print(dis.readByte());
////                }
//
////                for (int i = 0; i < dataTag.length; i++) {
////                    System.out.println("dataTag: "+dataTag[i]+" = "+new String(dataTag));
////                    
////                }
////                break;
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(MatlabIO.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(MatlabIO.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                fr.close();
//            } catch (IOException ex) {
//                Logger.getLogger(MatlabIO.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
}
