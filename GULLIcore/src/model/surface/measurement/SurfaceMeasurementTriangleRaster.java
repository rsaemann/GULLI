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

import model.particle.Particle;
import model.surface.Surface;
import static model.surface.measurement.TriangleMeasurement.minTravelLengthToMeasure;
import model.timeline.array.TimeIndexContainer;

/**
 * Standard Surface Measurement Raster thats measurements locations are the same
 * as the surface raster.
 *
 * @author saemann
 */
public class SurfaceMeasurementTriangleRaster extends SurfaceMeasurementRaster {

    protected Surface surf;
    /**
     * Particle Measurements for surface triangle ID
     */
    protected TriangleMeasurement[] measurements;

    protected int numberOfMaterials = 1;

    public SurfaceMeasurementTriangleRaster(Surface surf, int numberOfMaterials, TimeIndexContainer time) {
        this.surf = surf;
        this.times = time;
        this.numberOfMaterials = numberOfMaterials;
        this.measurements = new TriangleMeasurement[surf.getTriangleNodes().length];
    }

    @Override
    public void measureParticle(long time, Particle particle) {
        if (particle != null && particle.getSurrounding_actual() != null) {
            if (particle.getTravelledPathLength() < minTravelLengthToMeasure) {
                //for risk map do not show inertial particles
                return;
            }

            int id = particle.surfaceCellID;

            if (id < 0) {
                return;
            }
            if (!countStayingParticle) {
                if (particle.surfaceCellID == particle.lastSurfaceCellID) {
                    //Do not count particle, if it only stays at the same cell
                    return;
                } else {
                    particle.lastSurfaceCellID = particle.surfaceCellID;
                }
            }
            if (measurements[id] == null) {
                createMeasurement(id);
            }
            TriangleMeasurement m = null;
            int timeindex = times.getTimeIndex(time);

            try {
                m = measurements[id];
                m.mass[particle.getMaterial().materialIndex][timeindex] += particle.particleMass;
                m.particlecounter[particle.getMaterial().materialIndex][timeindex]++;

            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                System.err.println(getClass() + "::Request t=" + timeindex + " m=" + particle.getMaterial().materialIndex + "   mass.length: " + m.mass.length + "   times.length=" + times.getNumberOfTimes());
            }
        }
    }

    /**
     * Creates a new Triangle Measurment, puts it into the hashMap and connects
     * it to a SurfaceTriangle if exitent.
     *
     * @param triangleID Id of the triangle
     * @return
     */
    public TriangleMeasurement createMeasurement(int triangleID) {
        TriangleMeasurement tm = new TriangleMeasurement(times, triangleID, numberOfMaterials);
        this.measurements[triangleID] = tm;
        return tm;
    }

    @Override
    public void setNumberOfMaterials(int numberOfMaterials) {
        if (this.numberOfMaterials == numberOfMaterials) {
            return;
        }
        this.numberOfMaterials = numberOfMaterials;
        for (int i = 0; i < measurements.length; i++) {
            if (measurements[i] != null) {
                createMeasurement(i);
            }
        }
    }

    public TriangleMeasurement[] getMeasurements() {
        return measurements;
    }

    @Override
    public void setTimeContainer(TimeIndexContainer times) {
        this.times = times;
//        System.out.println(getClass() + "::setTimeContainer to " + this.times.getNumberOfTimes());
        for (int i = 0; i < measurements.length; i++) {
            if (measurements[i] != null) {
                createMeasurement(i);
            }
        }
    }

    @Override
    public void reset() {
        measurements = new TriangleMeasurement[surf.getTriangleNodes().length];
    }

    public Surface getSurface() {
        return surf;
    }

    public int getNumberOfMaterials() {
        return numberOfMaterials;
    }

}
