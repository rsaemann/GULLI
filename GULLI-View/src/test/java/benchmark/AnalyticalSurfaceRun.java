/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package benchmark;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.listener.SimulationActionAdapter;
import com.saemann.gulli.core.control.maths.RandomArray;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Constant;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.threads.ParticleThread;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.io.AnalyticalSurface;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.view.ViewController;
import com.saemann.gulli.view.timeline.AxisKey;
import com.saemann.gulli.view.timeline.SeriesKey;
import com.saemann.gulli.view.timeline.SpacelinePanel;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;
import javax.swing.JFrame;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

/**
 * a simple benchmark for a radial dispersive espansion of a point injected
 * solute. Direction and speed of the homogeneous flow field can be set as well
 * as the dispersion intensity.
 *
 * This benchmark can be used to test the implementation of a dispersion model.
 * The path finding on the surface is not affected by this benchmark, because no
 * triangles are defined.
 *
 *
 * @author Sämann
 */
public class AnalyticalSurfaceRun {

    public static void main(String[] args) throws Exception {

        ThreadController.pauseRevokerThread = true;
        ArrayTimeLineMeasurement.useIDsharpParticleCounting = false;
        ParticleSurfaceComputing2D.gridFree=true;

        final double vx = 1;// [m/s] velocity in x direction
        final double vy = 0;// [m/s] velocity in y direction

        double Dxx = 1;// [m^2/s] dispersion coefficient in x direction longitudinal
        double Dyy = 1;// [m^2/s] dispersion coefficient in y direction longitudinal

        double duration = 1 * 3601;//[s] simulation duration;

        //Measurement grid resolution:
        double dx = 10;//[m] sample cell width
        double dy = 10;//[m] sample cell height

        final double lengthX = duration * vx + 50 + 20 * Math.sqrt(Dxx * duration);// m
        final double lengthY = 15 * Math.sqrt(Dyy * duration);// m characteristic length for diffusion (x2 for sample quality)

        final int numberOfParticles = 100000; //total number of particles for injection
        final double mass = 1;// [kg] total particle mass

        final double timestep = 10;

        System.out.println("build surface");
        final AnalyticalSurface surface = new AnalyticalSurface(vx, vy, 1);
        System.out.println("initRaster");
        surface.initRectangularMeasurement(duration * vx * 0.5 + Math.sqrt(Dxx * duration), duration * vy * 0.5, lengthX, lengthY, dx, dy, duration, 150);
        surface.getMeasurementRaster().continousMeasurements = false;

        System.out.println("define Diffusion");
        double[] diffusion = new double[]{Dxx, Dyy};

        surface.diffusionCoefficient = diffusion;
        System.out.println("init analytical solution");
        /**
         * Define injection position here
         */
        surface.addInjection(100, 100, 0, numberOfParticles, mass, timestep);

//        System.out.println("change diffusioncalculators");
        final Controller c = new Controller();
        for (ParticleThread particleThread : c.getThreadController().getParticleThreads()) {
            ParticleSurfaceComputing2D sc;
            if (particleThread.getSurfaceComputing() instanceof ParticleSurfaceComputing2D) {
                sc = (ParticleSurfaceComputing2D) particleThread.getSurfaceComputing();
            } else {
                sc = new ParticleSurfaceComputing2D(surface, particleThread.threadIndex);
                particleThread.setSurfaceComputing(sc);
            }
            sc.enableDiffusion = true;
            Dispersion2D_Constant dc2d = new Dispersion2D_Constant();
            dc2d.D = diffusion;
            dc2d.directD = diffusion;
            dc2d.directSqrtD = new double[]{Math.sqrt(diffusion[0]), Math.sqrt(diffusion[1]), 0};
            dc2d.Dxx = diffusion[0];
            dc2d.Dyy = diffusion[1];
            sc.setSurface(surface);
            particleThread.setSurfaceComputing(sc);
        }
        c.getThreadController().setSeed(1);
        final ViewController vc = new ViewController(c);
        c.loadSurface(surface, c);
        c.getThreadController().setDeltaTime(timestep);
        c.loadScenario(surface.getScenario(), c);
        ParticlePipeComputing.measureOnlyFinalCapacity = true;
        vc.getMapViewer().recalculateShapes();

        c.recalculateInjections();

        //Calculate particles total mass
        double pmass = 0;
        for (Particle particle : c.getThreadController().getParticles()) {
            pmass += particle.particleMass;
        }
        System.out.println("Total particle mass is " + pmass + " kg");

        Position pos = surface.getPosition3D(0);
        vc.getMapViewer().setDisplayPositionByLatLon(pos.getLatitude(), pos.getLongitude(), 16);

        c.resetScenario();
        c.start();

        c.addSimulationListener(new SimulationActionAdapter() {

            @Override
            public void simulationFINISH(boolean timeOut, boolean particlesOut) {
                int threads = c.getThreadController().getParticleThreads().length;
                System.out.println("ArrayLists looped " + RandomArray.numberOfGaussLoops / threads + "  ,  " + RandomArray.numberOfDoubleLoops / threads + " where there are " + threads + " threads.");

                //Create corresponding particle counter
                double particlesPerMass = numberOfParticles / mass;
                double[][][][] particleAnalytical = new double[surface.analyticalMass.length][surface.analyticalMass[0].length][surface.analyticalMass[0][0].length][1];
                for (int i = 0; i < particleAnalytical.length; i++) {
                    for (int j = 0; j < particleAnalytical[0].length; j++) {
                        for (int t = 0; t < particleAnalytical[0][0].length - 1; t++) {
                            particleAnalytical[i][j][t][0] = (surface.analyticalMass[i][j][t] * particlesPerMass);

                        }
                    }

                }

                surface.rectraster.particlecounter = particleAnalytical;

                TimeSeries tspM0 = new TimeSeries(new SeriesKey("M0 ptcl", "", "kg", Color.red, new AxisKey("M0", "Mass")));
                TimeSeries tsaM0 = new TimeSeries(new SeriesKey("M0 ana", "", "kg", Color.orange, new AxisKey("M0", "Mass")));
                TimeSeries tsdM0 = new TimeSeries(new SeriesKey("M0 delta", "", "kg", Color.blue, new AxisKey("M0", "Mass")));
                TimeSeries tsdrM0 = new TimeSeries(new SeriesKey("M0 relative delta", "", "", Color.black, new AxisKey("Mrel", "rel")));

                TimeSeries tspM1x = new TimeSeries(new SeriesKey("M1x ptcl", "", "m", Color.red, new AxisKey("M1", "x")));
                TimeSeries tsaM1x = new TimeSeries(new SeriesKey("M1x ana", "", "m", Color.orange, new AxisKey("M1", "x")));
                TimeSeries tsdM1x = new TimeSeries(new SeriesKey("M1x delta", "", "m", Color.blue, new AxisKey("M1", "x")));
                TimeSeries tsdrM1x = new TimeSeries(new SeriesKey("M1x relative delta", "", "", Color.black, new AxisKey("Mrel", "rel")));

                TimeSeries tspM1y = new TimeSeries(new SeriesKey("M1y ptcl", "", "m", Color.red, new AxisKey("M1y", "y")));
                TimeSeries tsaM1y = new TimeSeries(new SeriesKey("M1y ana", "", "m", Color.orange, new AxisKey("M1y", "y")));
                TimeSeries tsdM1y = new TimeSeries(new SeriesKey("M1y delta", "", "m", Color.blue, new AxisKey("M1y", "y")));
                TimeSeries tsdrM1y = new TimeSeries(new SeriesKey("M1y relative delta", "", "", Color.black, new AxisKey("Mrel", "rel")));

                TimeSeries tspM2x = new TimeSeries(new SeriesKey("M2x ptcl", "", "m²", Color.red, new AxisKey("M2", "x")));
                TimeSeries tsaM2x = new TimeSeries(new SeriesKey("M2x ana", "", "m²", Color.orange, new AxisKey("M2", "x")));
                TimeSeries tsdM2x = new TimeSeries(new SeriesKey("M2x delta", "", "m²", Color.blue, new AxisKey("M2", "x")));
                TimeSeries tsdrM2x = new TimeSeries(new SeriesKey("M2x relative delta", "", "", Color.black, new AxisKey("Mrel", "rel")));

                TimeSeries tspM2y = new TimeSeries(new SeriesKey("M2y ptcl", "", "m²", Color.red, new AxisKey("M2y", "y")));
                TimeSeries tsaM2y = new TimeSeries(new SeriesKey("M2y ana", "", "m²", Color.orange, new AxisKey("M2y", "y")));
                TimeSeries tsdM2y = new TimeSeries(new SeriesKey("M2y delta", "", "m²", Color.blue, new AxisKey("M2y", "y")));
                TimeSeries tsdrM2y = new TimeSeries(new SeriesKey("M2y relative delta", "", "", Color.black, new AxisKey("Mrel", "rel")));

                double factorPerTimeindex = ThreadController.getDeltaTime() * 1000. / surface.getTimes().getDeltaTimeMS();

                TimeIndexContainer tc = surface.getTimes();
                TimeIndexContainer tcm = surface.getMeasurementRaster().getIndexContainer();
                for (int im = 0; im < tcm.getNumberOfTimes(); im++) {
                    long timestamp = surface.getMeasurementRaster().measurementTimestamp[im];
                    double fi = tc.getTimeIndexDouble(timestamp);//tcm.getTimeMilliseconds(im));
                    double timefactor = fi % 1.;
                    int i = (int) fi;
//                    System.out.println("analytic i="+i+"/"+tc.getNumberOfTimes()+" -> measures="+im+"/"+tcm.getNumberOfTimes()+",   time="+tc.getTimeMilliseconds(i)/1000+"s. ");
                    factorPerTimeindex = 1. / (double) surface.getMeasurementRaster().measurementsInTimeinterval[im];

                    RegularTimePeriod rtp = new Second(new Date(timestamp));//tcm.getTimeMilliseconds(im)));

                    double pm0 = surface.rectraster.getTotalMassInTimestep(im, 0) * factorPerTimeindex;
                    double massAnalytical = surface.getMassAnalytical(i) * (1 - timefactor) + surface.getMassAnalytical(i + 1) * timefactor;
                    if (!Double.isNaN(massAnalytical)) {
                        tsaM0.addOrUpdate(rtp, massAnalytical);
                    }
                    if (!Double.isNaN(pm0)) {
                        tspM0.addOrUpdate(rtp, pm0);
                        tsdM0.addOrUpdate(rtp, pm0 - massAnalytical);
                        tsdrM0.addOrUpdate(rtp, (pm0 - massAnalytical) / massAnalytical);
                    }

                    //Moment 1
                    double[] centre = surface.rectraster.getCenterOfMass(im, 0);
                    //x
                    double[] centreAna = surface.getCenterOfMassAnalytical(i);
                    double[] centerAnaNext = surface.getCenterOfMassAnalytical(i + 1);
//                    System.out.println("ana center mass [t=" + i + "]= " + centreAna[0] + " / " + centreAna[1]+"\t  particles:"+centre[0]+","+centre[1]);
                    double xana = centreAna[0] * (1. - timefactor) + centerAnaNext[0] * timefactor;// vx * tc.getTimeMilliseconds(i) / 1000 ;

                    if (!Double.isNaN(xana)) {
                        tsaM1x.addOrUpdate(rtp, xana);
                    }

                    if (!Double.isNaN(centre[1]) && !Double.isNaN(xana)) {
                        tspM1x.addOrUpdate(rtp, centre[0]);
                        tsdM1x.addOrUpdate(rtp, centre[0] - xana);
                        tsdrM1x.addOrUpdate(rtp, (centre[0] - xana) / xana);
                    }

//                    System.out.println("analytic i="+i+"/"+tc.getNumberOfTimes()+" -> measures="+im+"/"+tcm.getNumberOfTimes()+",   time="+tc.getTimeMilliseconds(i)/1000+"s. \tx="+centre[0]+"\t"+rtp+" factor="+factorPerTimeindex+"  number:"+surface.getMeasurementRaster().measurementsInTimeinterval[im]);
                    //y
                    double yana = centreAna[1] * (1. - timefactor) + centerAnaNext[1] * timefactor;//vy * tc.getTimeMilliseconds(i) / 1000 ;
                    if (!Double.isNaN(yana)) {
                        tsaM1y.addOrUpdate(rtp, yana);
                    }
                    if (!Double.isNaN(centre[1])) {
                        tspM1y.addOrUpdate(rtp, centre[1]);
                        tsdM1y.addOrUpdate(rtp, centre[1] - yana);
                        tsdrM1y.addOrUpdate(rtp, (centre[1] - yana) / yana);
                    }

                    //Moment 2
                    double[] variance = surface.rectraster.getVarianceOfPlume(im, 0, centre[0], centre[1]);
                    //x
                    double[] varAna = surface.getVarianceAnalytical(i, centreAna[0], centreAna[1]);
                    double[] varAnaNext = surface.getVarianceAnalytical(i + 1, centerAnaNext[0], centerAnaNext[1]);

//                    System.out.println("Var[t=" + i + "]= ana" + varAna[0] + "/ " + varAna[1] + "\tptcl:" + variance[0] + "/ " + variance[1]);
                    double vAnaX = varAna[0] * (1 - timefactor) + varAnaNext[0] * timefactor;
                    tsaM2x.addOrUpdate(rtp, vAnaX);
                    if (!Double.isNaN(variance[0])) {
                        tspM2x.addOrUpdate(rtp, variance[0]);
                        tsdM2x.addOrUpdate(rtp, variance[0] - vAnaX);
                        tsdrM2x.addOrUpdate(rtp, (variance[0] - vAnaX) / vAnaX);
                    }
                    //y
                    double vAnaY = varAna[1] * (1 - timefactor) + varAnaNext[1] * timefactor;
                    tsaM2y.addOrUpdate(rtp, vAnaY);
                    if (!Double.isNaN(variance[1])) {
                        tspM2y.addOrUpdate(rtp, variance[1]);
                        tsdM2y.addOrUpdate(rtp, variance[1] - vAnaY);
                        tsdrM2y.addOrUpdate(rtp, (variance[1] - vAnaY) / vAnaY);
                    }

                }
                for (int i = 0; i < surface.getNumberOfTimes(); i++) {
                    long time = surface.getTimes().getTimeMilliseconds(i);
                    RegularTimePeriod rtp = new Second(new Date(time));
                    double[] centre = surface.getCenterOfMassAnalytical(i);
                    tsaM1x.addOrUpdate(rtp, centre[0]);
                    tsaM1y.addOrUpdate(rtp, centre[1]);

                }
                vc.timelinePanel.showSimulationTime = true;
                vc.timelinePanel.collection.removeAllSeries();

                vc.timelinePanel.collection.addSeries(tspM0);
                vc.timelinePanel.collection.addSeries(tsaM0);
                vc.timelinePanel.collection.addSeries(tsdM0);
                vc.timelinePanel.collection.addSeries(tsdrM0);

                vc.timelinePanel.collection.addSeries(tspM1x);
                vc.timelinePanel.collection.addSeries(tsaM1x);
                vc.timelinePanel.collection.addSeries(tsdM1x);
                vc.timelinePanel.collection.addSeries(tsdrM1x);

                vc.timelinePanel.collection.addSeries(tspM1y);
                vc.timelinePanel.collection.addSeries(tsaM1y);
                vc.timelinePanel.collection.addSeries(tsdM1y);
                vc.timelinePanel.collection.addSeries(tsdrM1y);

                vc.timelinePanel.collection.addSeries(tspM2x);
                vc.timelinePanel.collection.addSeries(tsaM2x);
                vc.timelinePanel.collection.addSeries(tsdM2x);
                vc.timelinePanel.collection.addSeries(tsdrM2x);

                vc.timelinePanel.collection.addSeries(tspM2y);
                vc.timelinePanel.collection.addSeries(tsaM2y);
                vc.timelinePanel.collection.addSeries(tsdM2y);
                vc.timelinePanel.collection.addSeries(tsdrM2y);

                for (Object sery : vc.timelinePanel.collection.getSeries()) {
                    ((SeriesKey) ((TimeSeries) sery).getKey()).setVisible(false);
                }

                vc.timelinePanel.updateDateAxis(tc);
                vc.timelinePanel.updateCheckboxPanel();
                vc.timelinePanel.updateShownTimeSeries();
                vc.timeLineFrame.getTablePanel().getTable().updateTableByCollection();

            }

        });

        SpacelinePanel spacePlotPanel = new SpacelinePanel(null, (ArrayTimeLineMeasurementContainer) c.getScenario().getMeasurementsPipe(), "Time domain");
        JFrame spaceFrame = new JFrame("Time Domain");
        spaceFrame.add(spacePlotPanel);
        spaceFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        spaceFrame.setBounds(250, 300, 600, 800);
        spaceFrame.setVisible(true);
        spacePlotPanel.setDividerlocation(0.6);
        vc.getPaintManager().addCapacitySelectionListener(spacePlotPanel);
        DecimalFormat df = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));
        vc.getMapFrame().setTitle("2D analytical: " + numberOfParticles + " particles, vx= " + df.format(vx) + " m/s , vy= " + df.format(vy) + " m/s , D=" + df.format(Dxx) + " , " + df.format(Dyy) + " m²/s , t=" + df.format((duration / 60)) + " min");
    }

}
