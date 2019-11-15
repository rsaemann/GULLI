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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores number/mass of particles on a surface triangle
 *
 * @author saemann
 */
public class TriangleMeasurement {

    int triangleID;
    /**
     * [material index][timeindex]
     */
    double[][] mass;
    /**
     * [material index][timeindex]
     */
    int[][] particlecounter;

    public ReentrantLock lock = new ReentrantLock();

    public TriangleMeasurement(int triangleID, int numberOfTimes, int numberOfMaterials) {

        this.triangleID = triangleID;
        this.mass = new double[numberOfMaterials][numberOfTimes];
        this.particlecounter = new int[numberOfMaterials][numberOfTimes];
    }

    public int getTriangleID() {
        return triangleID;
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

    @Override
    public String toString() {
        return "TriangleMeasurement " + triangleID + " " + super.toString();
    }

    public int totalParticleCount() {
        int counter = 0;
        for (int i = 0; i < particlecounter.length; i++) {
            for (int j = 0; j < particlecounter[i].length; j++) {
                counter += particlecounter[i][j];
            }
        }
        return counter;
    }

}
