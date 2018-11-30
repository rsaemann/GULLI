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
import model.timeline.array.TimeIndexContainer;

/**
 * Stores number/mass of particles on a surface triangle
 *
 * @author saemann
 */
public class TriangleMeasurement {
    
    /**
     * if particles have lower travel length than this, they are not measured.
     */
    public static double minTravelLengthToMeasure=0;

    TimeIndexContainer times;
    int triangleID;
    /**
     * [material index][timeindex]
     */
    double[][] mass;
    /**
     * [material index][timeindex]
     */
    int[][] particlecounter;

//    int[] timerequestCount;

//    long lastRequestTime = 0;

    public TriangleMeasurement(TimeIndexContainer times, int triangleID, int numberOfMaterials) {
        this.times = times;
        this.triangleID = triangleID;
        this.mass = new double[numberOfMaterials][times.getNumberOfTimes()];
        this.particlecounter = new int[numberOfMaterials][times.getNumberOfTimes()];
//        this.timerequestCount = new int[times.getNumberOfTimes()];
    }

    public int getTriangleID() {
        return triangleID;
    }

    public void measureParticle(long time, Particle particle) {
        if (particle.getTravelledPathLength() < minTravelLengthToMeasure) {
            //for risk map do not show inertial particles
//            System.out.println("Do not track particle "+particle.getTravelledPathLength());
            return;
        }
        int timeindex = times.getTimeIndex(time);
        try {
            mass[particle.getMaterial().materialIndex][timeindex] += particle.particleMass;
            particlecounter[particle.getMaterial().materialIndex][timeindex]++;
//            if (time != lastRequestTime) {
//                timerequestCount[timeindex]++;
//                lastRequestTime = time;
//            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println(getClass() + "::Request t=" + timeindex + " m=" + particle.getMaterial().materialIndex + "   length: " + mass.length + "   times.length=" + times.getNumberOfTimes());
        }
    }

    public void reset() {
        this.mass = new double[mass.length][times.getNumberOfTimes()];
        this.particlecounter = new int[particlecounter.length][times.getNumberOfTimes()];
//        this.timerequestCount = new int[times.getNumberOfTimes()];
    }

    public void reset(int numberOfMaterials) {
        this.mass = new double[numberOfMaterials][times.getNumberOfTimes()];
        this.particlecounter = new int[numberOfMaterials][times.getNumberOfTimes()];
//        this.timerequestCount = new int[times.getNumberOfTimes()];
    }

    public void setTimecontainer(TimeIndexContainer container) {
        this.times = container;
        reset();
    }

    public TimeIndexContainer getTimes() {
        return times;
    }

    /**
     * [material index][timeindex]
     *
     * @return
     */
    public double[][] getMass() {
        return mass;
    }

    /**
     * [material index][timeindex]
     *
     * @return
     */
    public int[][] getParticlecount() {
        return particlecounter;
    }

//    public int requestInTimestep(int timeIndex) {
//        return 0;//timerequestCount[timeIndex];
//    }

}
