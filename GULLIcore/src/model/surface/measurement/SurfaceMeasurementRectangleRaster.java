/*
 * The MIT License
 *
 * Copyright 2018 saemann.
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
package model.surface.measurement;

import com.vividsolutions.jts.geom.Coordinate;
import model.particle.Particle;
import model.surface.Surface;
import model.timeline.array.TimeIndexContainer;

/**
 * Raster represneting surface in rectangles for very fast index finding
 *
 * @author saemann
 */
public class SurfaceMeasurementRectangleRaster extends SurfaceMeasurementRaster {

    /**
     * [x-index][y-index][timeindex][materialindex]:mass
     */
    public double[][][][] mass;

    /**
     * [x-index][y-index][timeindex][materialindex]:counter
     */
    public long[][][][] particlecounter;

    int numberXIntervals, numberYIntervals, numberOfMaterials;
    double xIntervalWidth, YIntervalHeight;
    double xmin, ymin;

    public SurfaceMeasurementRectangleRaster(double xmin, double ymin, int numberXIntervals, int numberYIntervals, double xIntervalWidth, double yIntervalHeight, int numberMaterials, TimeIndexContainer time) {
        this.times = time;
        this.numberOfMaterials = numberMaterials;
        this.xIntervalWidth = xIntervalWidth;
        this.YIntervalHeight = yIntervalHeight;
        this.xmin = xmin;
        this.ymin = ymin;
        this.numberXIntervals = numberXIntervals;
        this.numberYIntervals = numberYIntervals;

        mass = new double[numberXIntervals][][][];
        particlecounter = new long[numberXIntervals][][][];
        measurementsInTimeinterval = new int[times.getNumberOfTimes()];
        measurementTimestamp = new long[measurementsInTimeinterval.length];
    }

    public static SurfaceMeasurementRectangleRaster SurfaceMeasurementRectangleRaster(Surface surf, int numberXInterval, int numberYInterval) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (double[] vertex : surf.getVerticesPosition()) {
            minX = Math.min(minX, vertex[0]);
            maxX = Math.max(maxX, vertex[0]);
            minY = Math.min(minY, vertex[1]);
            maxY = Math.max(maxY, vertex[1]);
        }

        double xwidth = (maxX - minX) / (double) numberXInterval;
        double ywidth = (maxY - minY) / (double) numberYInterval;
        return new SurfaceMeasurementRectangleRaster(minX, minY, numberXInterval, numberYInterval, xwidth, ywidth, surf.getNumberOfMaterials(), surf.getTimes());
    }

    public static SurfaceMeasurementRaster SurfaceMeasurementRectangleRaster(Surface surface, int numberXIntervals, int numberYIntervals, int numberMaterials) {

        SurfaceMeasurementRectangleRaster smr = SurfaceMeasurementRectangleRaster(surface, numberXIntervals, numberYIntervals);
        smr.setNumberOfMaterials(numberMaterials);
        return smr;
    }

    public static SurfaceMeasurementRectangleRaster SurfaceMeasurementRectangleRaster(Surface surf, double dx, double dy) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (double[] vertex : surf.getVerticesPosition()) {
            minX = Math.min(minX, vertex[0]);
            maxX = Math.max(maxX, vertex[0]);
            minY = Math.min(minY, vertex[1]);
            maxY = Math.max(maxY, vertex[1]);
        }

        int numx = (int) ((maxX - minX) / dx + 1);
        int numy = (int) ((maxY - minY) / dy + 1);
        return new SurfaceMeasurementRectangleRaster(minX, minY, numx, numy, dx, dy, surf.getNumberOfMaterials(), surf.getTimes());
    }

    /**
     *
     * @param x
     * @param y
     * @param toCellCenter if true the center of the given point will always be
     * in the center of a cell. otherwise it will in the edge point of 4 cells.
     * @param dx
     * @param dy
     * @param numberXIntervals
     * @param numberYIntervals
     * @param numberMaterials
     * @param timec
     * @return
     */
    public static SurfaceMeasurementRectangleRaster RasterFocusOnPoint(double x, double y, boolean toCellCenter, double dx, double dy, int numberXIntervals, int numberYIntervals, int numberMaterials, TimeIndexContainer timec) {

        double xmin, ymin;
        if (toCellCenter) {
            //place focus point in the center of a raster cell
            xmin = x - dx * 0.5 - dx * (int) (numberXIntervals / 2);
            ymin = y - dy * 0.5 - dy * (int) (numberYIntervals / 2);
        } else {
            xmin = x - dx * (int) (numberXIntervals / 2);
            ymin = y - dy * (int) (numberYIntervals / 2);
        }

        SurfaceMeasurementRectangleRaster smr = new SurfaceMeasurementRectangleRaster(xmin, ymin, numberXIntervals, numberYIntervals, dx, dy, numberMaterials, timec);

        return smr;
    }

    @Override
    public void measureParticle(long time, Particle particle, int index) {
        if (!continousMeasurements && !measurementsActive) {
            return;
        }
        if (particle.getPosition3d() == null) {
            return;
        }
        if (particle.getTravelledPathLength() < minTravelLengthToMeasure) {
            return;
        }
        if(measureSpilloutParticlesOnly&&particle.toSurface==null){
            return;
        }
        if (this.times == null) {
            throw new NullPointerException("TimeContainer in " + getClass() + " not set.");
        }
        int timeIndex = writeIndex;// this.times.getTimeIndex(time);
        int xindex = (int) ((particle.getPosition3d().x - xmin) / xIntervalWidth);
        int yindex = (int) ((particle.getPosition3d().y - ymin) / YIntervalHeight);

        if (xindex < 0 || yindex < 0 || xindex >= mass.length) {
            return;
        }
        if (yindex >= numberYIntervals) {
            return;
        }
        try {
//create counters if non existing
            if (mass[xindex] == null) {
                synchronized (mass) {
                    if (mass[xindex] == null) {
                        mass[xindex] = new double[numberYIntervals][][];
                        particlecounter[xindex] = new long[numberYIntervals][][];
                    }
                }
            }
            if (mass[xindex][yindex] == null) {
                synchronized (mass) {
                    if (mass[xindex][yindex] == null) {
                        mass[xindex][yindex] = new double[times.getNumberOfTimes()][numberOfMaterials];
                        particlecounter[xindex][yindex] = new long[times.getNumberOfTimes()][numberOfMaterials];
                    }
                }
            }
//count particle
            if (synchronizeMeasures) {
                synchronized (mass[xindex][yindex][timeIndex]) {

                    mass[xindex][yindex][timeIndex][particle.getMaterial().materialIndex] += particle.getParticleMass();
                    try {
                        particlecounter[xindex][yindex][timeIndex][particle.getMaterial().materialIndex]++;
                    } catch (Exception e) {
                        //this arrays seems not to be initialized by another thread. wait some time for completion.
                        Thread.sleep(20);
                        if (particlecounter[xindex][yindex] != null) {
                            particlecounter[xindex][yindex][timeIndex][particle.getMaterial().materialIndex]++;
                        }
                    }
                }
            } else {
                mass[xindex][yindex][timeIndex][particle.getMaterial().materialIndex] += particle.getParticleMass();
                particlecounter[xindex][yindex][timeIndex][particle.getMaterial().materialIndex]++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("X index: " + xindex + "    pos.x=" + particle.getPosition3d().x + "    xmin=" + xmin + "     diff=" + ((particle.getPosition3d().x - xmin) + "    xIwidth=" + xIntervalWidth));
        }
    }

    @Override
    public void setNumberOfMaterials(int numberOfMaterials) {
        if (this.numberOfMaterials == numberOfMaterials) {
            return;
        }
        this.numberOfMaterials = numberOfMaterials;
    }

    @Override
    public void setTimeContainer(TimeIndexContainer times) {
        this.times = times;
        measurementsInTimeinterval = new int[times.getNumberOfTimes()];

        measurementTimestamp = new long[measurementsInTimeinterval.length];
    }

    public int getNumberOfMaterials() {
        return numberOfMaterials;
    }

    public double getXmin() {
        return xmin;
    }

    public double getYmin() {
        return ymin;
    }

    public double getYIntervalHeight() {
        return YIntervalHeight;
    }

    public double getxIntervalWidth() {
        return xIntervalWidth;
    }

    public int getNumberXIntervals() {
        return numberXIntervals;
    }

    public int getNumberYIntervals() {
        return numberYIntervals;
    }

    /**
     * [x-index][y-index][timeindex][materialindex]:counter
     *
     * @return number of particles in raster
     */
    public long[][][][] getParticlecounter() {
        return particlecounter;
    }

    /**
     * [x-index][y-index][timeindex][materialindex]:mass
     *
     * @return mass in raster
     */
    public double[][][][] getMass() {
        return mass;
    }

    public long getParticlesCounted(int xindex, int yindex, int timeindex, int materialindex) {
        if (particlecounter == null) {
            return 0;
        }
        if (particlecounter[xindex] == null) {
            return 0;
        }
        if (particlecounter[xindex][yindex] == null) {
            return 0;
        }
        if (particlecounter[xindex][yindex][timeindex] == null) {
            return 0;
        }
        return particlecounter[xindex][yindex][timeindex][materialindex];

    }

    public int getParticlesCountedMaterialSum(int xindex, int yindex, int timeindex) {
        if (particlecounter == null) {
            return 0;
        }
        if (particlecounter[xindex] == null) {
            return 0;
        }
        if (particlecounter[xindex][yindex] == null) {
            return 0;
        }
        if (particlecounter[xindex][yindex][timeindex] == null) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < numberOfMaterials; i++) {
            sum += particlecounter[xindex][yindex][timeindex][i];
        }
        return sum;
    }

    public int getMaxParticleCount() {
        int maxSum = 0;
        for (int x = 0; x < numberXIntervals; x++) {
            if (particlecounter[x] == null) {
                continue;
            }
            for (int y = 0; y < numberYIntervals; y++) {
                if (particlecounter[x][y] == null) {
                    continue;
                }
                for (int t = 0; t < times.getNumberOfTimes(); t++) {
                    int sum = 0;
                    for (int m = 0; m < numberOfMaterials; m++) {
                        sum += particlecounter[x][y][t][m];
                    }
                    maxSum = Math.max(maxSum, sum);
                }
            }
        }
        return maxSum;
    }

    public double getMaxMassPerCell() {
        double maxSum = 0;
        for (int x = 0; x < numberXIntervals; x++) {
            if (mass[x] == null) {
                continue;
            }
            for (int y = 0; y < numberYIntervals; y++) {
                if (mass[x][y] == null) {
                    continue;
                }
                for (int t = 0; t < times.getNumberOfTimes(); t++) {
                    double sum = 0;
                    for (int m = 0; m < numberOfMaterials; m++) {
                        sum += mass[x][y][t][m];
                    }
                    maxSum = Math.max(maxSum, sum);
                }
            }
        }
        return maxSum;
    }

    public Coordinate getMidCoordinate(int xindex, int yindex) {
        return new Coordinate(xmin + (xindex + 0.5) * xIntervalWidth, ymin + (yindex + 0.5) * YIntervalHeight);
    }

    public int getXindexFor(double xcoord) {
        return (int) ((xcoord - xmin) / xIntervalWidth);
    }

    public int getYIndexFor(double yCoord) {
        return (int) ((yCoord - ymin) / YIntervalHeight);
    }

    /**
     * Returns 4 coordinates
     *
     * @param xindex
     * @param yindex
     * @return
     */
    public Coordinate[] getRectangleBound(int xindex, int yindex) {
        Coordinate[] cs = new Coordinate[4];
        cs[0] = new Coordinate(xmin + (xindex) * xIntervalWidth, ymin + (yindex) * YIntervalHeight);
        cs[2] = new Coordinate(xmin + (xindex + 1) * xIntervalWidth, ymin + (yindex + 1) * YIntervalHeight);
        cs[1] = new Coordinate(cs[2].x, cs[0].y);
        cs[3] = new Coordinate(cs[0].x, cs[2].y);

        return cs;
    }

    /**
     * Returns 5 coordinates
     *
     * @param xindex
     * @param yindex
     * @return
     */
    public Coordinate[] getRectangleBoundClosed(int xindex, int yindex) {
        Coordinate[] cs = new Coordinate[5];
        cs[0] = new Coordinate(xmin + (xindex) * xIntervalWidth, ymin + (yindex) * YIntervalHeight);
        cs[2] = new Coordinate(xmin + (xindex + 1) * xIntervalWidth, ymin + (yindex + 1) * YIntervalHeight);
        cs[1] = new Coordinate(cs[2].x, cs[0].y);
        cs[3] = new Coordinate(cs[0].x, cs[2].y);
        cs[4] = cs[0];
        return cs;
    }

    public int getNumberOfTimes() {
        if (this.times == null) {
            return 0;
        }
        return this.times.getNumberOfTimes();
    }

    @Override
    public void reset() {
        mass = new double[numberXIntervals][][][];
        particlecounter = new long[numberXIntervals][][][];
        measurementsInTimeinterval = new int[times.getNumberOfTimes()];

        measurementTimestamp = new long[measurementsInTimeinterval.length];
    }

    @Override
    public void breakAllLocks() {
        throw new UnsupportedOperationException("RectangularRaster is working with 'synchronize' that cannot be unlocked"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void synchronizeMeasurements() {
        //Not needed        
    }

    @Override
    public void setNumberOfThreads(int threadCount) {

    }

    @Override
    public int getNumberOfCells() {
        return numberXIntervals * numberYIntervals;
    }

    @Override
    public Coordinate getCenterOfCell(int cellindex) {
        int x = cellindex / numberYIntervals;
        int y = cellindex % numberYIntervals;
        return getMidCoordinate(x, y);
    }

    @Override
    public double getMassInCell(int cellIndex, int timeindex, int materialIndex) {
        int x = cellIndex / numberYIntervals;
        int y = cellIndex % numberYIntervals;
        return mass[x][y][timeindex][materialIndex];
    }

    @Override
    public boolean isCellContaminated(int cellIndex) {
        if (particlecounter == null) {
            return false;
        }
        int x = cellIndex / numberYIntervals;

        if (particlecounter[x] == null) {
            return false;
        }
        int y = cellIndex % numberYIntervals;
        if (particlecounter[x][y] == null) {
            return false;
        }
        return true;
    }

    public double getTotalMassInTimestep(int timeindex, int materialindex) {
        double sum = 0;
        for (int i = 0; i < mass.length; i++) {
            if (mass[i] == null) {
                continue;
            }
            for (int j = 0; j < mass[i].length; j++) {
                if (mass[i][j] == null) {
                    continue;
                }
                try {
                    sum += mass[i][j][timeindex][materialindex];
                } catch (Exception e) {
                }
            }
        }
        return sum;
    }

    public double[] getCenterOfMass(int timeindex, int materialindex) {
        double sumMass = 0;
        double weightX = 0;
        double weightY = 0;

//        int xindex = (int) ((particle.getPosition3d().x - xmin) / xIntervalWidth);
//        int yindex = (int) ((particle.getPosition3d().y - ymin) / YIntervalHeight);
        double timeFactor = 1.0 / (double) measurementsInTimeinterval[timeindex];

        for (int i = 0; i < mass.length; i++) {
            double x = ((i + 0.5) * xIntervalWidth) + xmin;
            if (mass[i] == null) {
                continue;
            }
            for (int j = 0; j < mass[i].length; j++) {
                double y = ((j + 0.5) * YIntervalHeight) + ymin;
                if (mass[i][j] == null) {
                    continue;
                }
                try {
                    double m = mass[i][j][timeindex][materialindex] * timeFactor;
                    sumMass += m;
                    weightX += m * x;
                    weightY += m * y;
                } catch (Exception e) {
                }
            }
        }
        return new double[]{weightX / sumMass, weightY / sumMass};
    }

    public double[] getVarianceOfPlume(int timeindex, int materialindex, double centreX, double centreY) {
        double sumMass = 0;
        double weightX = 0;
        double weightY = 0;

//        int xindex = (int) ((particle.getPosition3d().x - xmin) / xIntervalWidth);
//        int yindex = (int) ((particle.getPosition3d().y - ymin) / YIntervalHeight);
        double centerX = 0, centerY = 0;

        double timeFactor = 1.0 / (double) measurementsInTimeinterval[timeindex];

        for (int i = 0; i < mass.length; i++) {

            if (mass[i] == null) {
                continue;
            }
            double x = ((i + 0.5) * xIntervalWidth) + xmin;
            double dxsq = (x - centreX) * (x - centreX);
            for (int j = 0; j < mass[i].length; j++) {

                if (mass[i][j] == null) {
                    continue;
                }
                double y = ((j + 0.5) * YIntervalHeight) + ymin;
                double dysq = (y - centreY) * (y - centreY);
                try {
                    double m = mass[i][j][timeindex][materialindex] * timeFactor;
                    sumMass += m;
                    weightX += m * dxsq;
                    weightY += m * dysq;
                    centerX += m * x;
                    centerY += m * y;
                } catch (Exception e) {
                }
            }
        }
//        System.out.println("Variance center, given: "+centreX+","+centreY+"\tcalculated:"+(centerX/sumMass)+", "+(centerY/sumMass));
//        System.out.println("Raster varaince: weightX" + weightX + ", centerX=" + centreX);
        return new double[]{weightX / sumMass, weightY / sumMass};
    }

    @Override
    public double getNumberOfParticlesInCell(int cellIndex, int timeindex, int materialIndex) {

        int x = cellIndex / numberYIntervals;
        int y = cellIndex % numberYIntervals;
        return particlecounter[x][y][timeindex][materialIndex];

    }

}
