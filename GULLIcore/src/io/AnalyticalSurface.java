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
import control.scenario.Scenario;
import control.scenario.SpillScenario;
import control.scenario.injection.InjectionInformation;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.Material;
import model.particle.Particle;
import model.surface.Surface;
import model.surface.SurfaceVelocityLoader;
import model.surface.SurfaceWaterlevelLoader;
import model.surface.measurement.SurfaceMeasurementRectangleRaster;
import model.timeline.array.TimeIndexContainer;
import model.topology.Position;
import model.topology.Position3D;
import org.opengis.referencing.operation.TransformException;

/**
 * Surface class to test the analytical solution of 2d-random walk approach. Use
 * this surface class together with the Starter main class AnalyticalSurfaceRun
 *
 * @author Robert Sämann
 */
public class AnalyticalSurface extends Surface {

    protected double waterlevel;
    protected double vx, vy;
    protected double[] velocity;
    protected double[] maxWL;

    /**
     * [0]=Dxx [1]=Dyy
     */
    public double[] diffusionCoefficient;

    protected Scenario scenario;

    protected Position3D r_position;

    protected Material injectionMaterial = new Material("Solute", 1000, true, 0);

    public SurfaceMeasurementRectangleRaster rectraster;

    /**
     * [x][y][time]=mass;
     */
    public double[][][][] analyticalMass;

    public AnalyticalSurface(double vx, double vy, double h) {
        super(null, null, null, null, "EPSG:25832");
        Coordinate c = null;
        try {
            c = this.getGeotools().toGlobal(new Coordinate(0, 0, 0), true);
            r_position = new Position3D(c.x, c.y, 0, 0, 0);
        } catch (TransformException ex) {
            Logger.getLogger(AnalyticalSurface.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.waterlevel = h;
        maxWL = new double[]{h};
        this.vx = vx;
        this.vy = vy;
        this.velocity = new double[]{vx, vy};
    }

    public void initRectangularMeasurement(double centerX,double centerY,double width, double height, double cellwidthx, double cellwidthy, double totalSeconds, double intervalSeconds) {
        TimeIndexContainer times = null;
        if (times == null && scenario != null) {
            if (scenario.getTimesSurface() != null) {
                if (scenario.getTimesSurface() instanceof TimeIndexContainer) {
                    times = (TimeIndexContainer) scenario.getTimesSurface();
                }
            }
        }
        if (times == null) {
            long[] time = new long[(int) (totalSeconds / intervalSeconds + 1)];
            for (int i = 0; i < time.length; i++) {
                time[i] = (long) ((i * intervalSeconds) * 1000);
            }
            times = new TimeIndexContainer(time);
            if (scenario != null) {
                scenario.setTimesSurface(times);
            }
            this.setTimeContainer(times);

        }

        rectraster = SurfaceMeasurementRectangleRaster.RasterFocusOnPoint(centerX, centerY, true, cellwidthx, cellwidthy, (int) (width / cellwidthx), (int) (height / cellwidthy), 1, times);
        this.measurementRaster = rectraster;
    }

    public void addInjection(double x, double y, double timeSeconds, int numberOfParticles, double mass) {
        try {
            Scenario sc = this.scenario;
            if (sc == null) {
                sc = new SpillScenario(times, new ArrayList<InjectionInformation>());
                this.scenario = sc;
                sc.setTimesSurface(times);
            }
            Coordinate lonlat = getGeotools().toGlobal(new Coordinate(x, y), true);
            InjectionInformation inj = new InjectionInformation(new Position(lonlat.x, lonlat.y, x, y), false, mass, numberOfParticles, injectionMaterial, timeSeconds);
            inj.setCapacity(this);
            inj.spillOnSurface = true;
            inj.setTriangleID(0);
            sc.getInjections().add(inj);
//            System.out.println("call compute anasolution");
            calculateAnalyticalSolution(x, y, timeSeconds, mass, diffusionCoefficient,1);

//            rectraster.mass=analyticalMass;
        } catch (TransformException ex) {
            Logger.getLogger(AnalyticalSurface.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void calculateAnalyticalSolution(double x, double y, double injectiontime, double mass, double[] diffusionCoefficient,double firsttimestepS) {
        if (analyticalMass == null) {
            System.out.println("initialize mass analytical raster "+rectraster.getNumberXIntervals()+", "+rectraster.getNumberYIntervals()+", "+times.getNumberOfTimes());
            analyticalMass = new double[rectraster.getNumberXIntervals()][rectraster.getNumberYIntervals()][times.getNumberOfTimes()][1];
        }
        double dx = rectraster.getxIntervalWidth();
        double dy = rectraster.getYIntervalHeight();
        for (int it = 0; it < rectraster.getNumberOfTimes(); it++) {
//            System.out.println("t="+it+" / "+rectraster.getNumberOfTimes());
//            double maxM=0;
//            int maxX=0,maxY=0;
            for (int ix = 0; ix < rectraster.getNumberXIntervals(); ix++) {

                for (int iy = 0; iy < rectraster.getNumberYIntervals(); iy++) {

                    double t = times.getTimeMilliseconds(it) / 1000. - injectiontime;
                    double powx = rectraster.getXmin() + (ix) * dx-x - velocity[0] * (t+firsttimestepS);//- x;
                    double powy = rectraster.getYmin() + (iy) * dy-y - velocity[1] * (t+ firsttimestepS);//- y ;
//                    System.out.println("ix=" + ix + " -> dx=" + powx);
                    if (t == 0) {
                        t = 1;// 0.001;
                    }
                    double mt = (mass / (4 * Math.PI * (t) * Math.sqrt(diffusionCoefficient[0] * diffusionCoefficient[1]))) * Math.exp(-(powx * powx) / (4 * diffusionCoefficient[0] * (t)) - (powy * powy) / (4 * diffusionCoefficient[1] * t));
//                    System.out.println("it="+it+" -> t="+t);
                    analyticalMass[ix][iy][it][0] += mt*dx*dy;
                    
//                    if(maxM<mt){
//                        maxM=mt;
//                        maxX=ix;
//                        maxY=iy;
//                    }
                }
            }
//            System.out.println("center at it="+it+"= "+maxX+", "+maxY+"\tm="+maxM);
        }
    }

    public double getMassAnalytical(int timeIndex) {
        double sumMass = 0;
        for (int i = 0; i < analyticalMass.length; i++) {
            for (int j = 0; j < analyticalMass[i].length; j++) {
                double m = analyticalMass[i][j][timeIndex][0];
                sumMass += m;
            }
        }
        return sumMass;
    }

    public double[] getCenterOfMassAnalytical(int timeindex) {
        double sumMass = 0;
        double weightX = 0;
        double weightY = 0;
        double dx = rectraster.getxIntervalWidth();
        double dy = rectraster.getYIntervalHeight();
//        int xindex = (int) ((particle.getPosition3d().x - xmin) / xIntervalWidth);
//        int yindex = (int) ((particle.getPosition3d().y - ymin) / YIntervalHeight);
        for (int i = 0; i < analyticalMass.length; i++) {
            double x = ((i) * dx) + rectraster.getXmin();
            for (int j = 0; j < analyticalMass[i].length; j++) {
                double y = ((j) * dy) + rectraster.getYmin();
                double m = analyticalMass[i][j][timeindex][0];
                sumMass += m;
                weightX += m * x;
                weightY += m * y;

            }
        }
        return new double[]{weightX / sumMass, weightY / sumMass};
    }

    public double[] getVarianceAnalytical(int timeIndex, double centerX, double centerY) {
        double sumMass = 0;
        double weightX = 0;
        double weightY = 0;
        double dx = rectraster.getxIntervalWidth();
        double dy = rectraster.getYIntervalHeight();

//        double centerTestX = 0;
//        double centerTestY = 0;
//        int xindex = (int) ((particle.getPosition3d().x - xmin) / xIntervalWidth);
//        int yindex = (int) ((particle.getPosition3d().y - ymin) / YIntervalHeight);
        for (int i = 0; i < analyticalMass.length; i++) {
            double x = ((i) * dx) + rectraster.getXmin();
            double dxsq = (x - centerX) * (x - centerX);
//            System.out.println(i+" dxsq="+dxsq+"    x="+x+"   center:"+centerX);
            for (int j = 0; j < analyticalMass[i].length; j++) {
                double y = ((j) * dy) + rectraster.getYmin();
                double dysq = (y - centerY) * (y - centerY);
                double m = analyticalMass[i][j][timeIndex][0];
//                System.out.println("mass["+i+","+j+","+timeIndex+"]= "+m);
                sumMass += m;
                weightX += m * dxsq;
                weightY += m * dysq;
//                centerTestX += m * (x - centerX);
//                centerTestY += m * (y - centerY);
            }
        }
        if (sumMass == 0) {
            return new double[2];
        }
//        System.out.println("Set center: "+centerX+","+centerY+"  calculated center:"+(centerTestX/sumMass)+", "+(centerTestY/sumMass));
        //System.out.println("mass["+timeIndex+"]="+sumMass+" weightX="+weightX);
        return new double[]{weightX / sumMass, weightY / sumMass};
    }

    @Override
    public float getActualWaterlevel(int ID) {
        return (float) waterlevel;
    }

    @Override
    public long getAutoID() {
        return 0;
    }

    @Override
    public double[] getMaxWaterlvl() {
        return maxWL;
    }

    @Override
    public int getMaxTriangleID() {
        return 0;
    }

    @Override
    public double[] getMaxWaterLevels() {
        return maxWL;
    }

    public SurfaceWaterlevelLoader getWaterlevelLoader() {
        return waterlevelLoader;
    }

    public SurfaceVelocityLoader getVelocityLoader() {
        return velocityLoader;
    }

    @Override
    public int calcContainingTriangle(double x, double y) {
        return 0;
    }

    @Override
    public int triangleIDNear(double x, double y) {
        return 0;
    }

    @Override
    public int findContainingTriangle(double x, double y, double minDistance) {
        return 0;
    }

    @Override
    public int getTargetTriangleID(Particle p, int id, double xold, double yold, double x, double y, int leftIterations, double[] bw, double[][] t) {
        p.setPosition3D(x, y);
        return 0;
    }

    @Override
    public double calcTriangleArea(int id) {
        return 1;
    }

    @Override
    public double[][] getTriangleMids() {
        double[][] mid = new double[1][3];
        return mid;
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return r_position;
    }

    @Override
    public double[] getParticleVelocity2D(Particle p, int triangleID) {
//        System.out.println("getvelocity 2d");
        return velocity;
    }

    @Override
    public double[] getParticleVelocity2D(Particle p, int triangleID, double[] tofillVelocity, double[] tofillBarycentric) {
//        System.out.println("getvelocity with barycentric ");
        return velocity;
    }

    public Scenario getScenario() {
        return scenario;
    }

    protected class AnalyticWaterLevelLoader implements SurfaceWaterlevelLoader {

        @Override
        public float[] loadWaterlevlvalues(int triangleID) {
            return null;
        }

        @Override
        public float loadZElevation(int triangleID) {
            return 0;
        }

    }

    protected class AnalyticVelocityLoader implements SurfaceVelocityLoader {

        @Override
        public float[][] loadVelocity(int triangleID) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}
