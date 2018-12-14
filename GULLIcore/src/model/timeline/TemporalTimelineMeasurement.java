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
package model.timeline;

import java.util.HashMap;
import model.particle.Particle;

/**
 * Measurement for Pipe timelines for each Particlethread to prevent
 * synchronization locks while writing particle's information. This Object
 * stores the information temporal for each ParticleThread and a
 * SynchronizationThread will then merge all information.
 *
 * @author saemann
 */
public class TemporalTimelineMeasurement {

    /**
     * key: capacity id value: float[] values in timestep: 0: numberofParticles,
     * 1: mass, 2: # immigrated Particles, 3: # immigrated mass
     */
    private final HashMap<Integer, float[]> map = new HashMap<>(500);

    /**
     *
     * @param capacityID
     * @param p particle
     * @param immigrated Is this particle new in the current capacity?
     */
    public void addParticle(int capacityID, Particle p, boolean immigrated) {
        float[] values = map.get(capacityID);
        if (values == null) {
            values = new float[4];
            map.put(capacityID, values);
        }
        values[0]++;
        values[1] += p.particleMass;
        if (immigrated) {
            values[2]++;
            values[3] += p.particleMass;
        }
    }

    /**
     * Set all counting values to 0. Does NOT cler the key-value pairs.
     */
    public void clean() {
        for (float[] value : map.values()) {
            for (int i = 0; i < value.length; i++) {
                value[i] = 0;
            }
        }
    }

    /**
     * Clear all values from the map. Also clear the key-value pairs.
     */
    public void clear() {
        this.map.clear();
    }

    /**
     * Get the map with key: capacityID Values : float[] 0: number of counts 1:
     * mass 2: number of immigrated particles 3: mass of immigrated particles
     *
     * @return
     */
    public HashMap<Integer, float[]> getMap() {
        return map;
    }

}
