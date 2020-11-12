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
package control.particlecontrol;

import control.maths.RandomArray;
import control.maths.RandomGenerator;
import model.particle.Particle;
import model.topology.Capacity;
import model.topology.Connection_Manhole;
import model.topology.Manhole;

/**
 *
 * @author saemann
 */
public interface FlowCalculator {

    /**
     *
     * @param mh
     * @param probability
     * @param forward in advective direction, ds < 0 : anti advection direction 
     * @return
     */
    public abstract Connection_Manhole whichConnection(Manhole mh, RandomGenerator probability, boolean forward);

    /**
     * Returns if a particle is changing to immobilize state. Does not affect
     * the particle state directly inside this method.
     *
     * @param particle
     * @param capacity
     * @param random
     * @return
     */
    public abstract boolean particleIsDepositing(Particle particle, Capacity capacity,  RandomGenerator random);

    /**
     * Returns wheather a particle is changing back to mobile state (from
     * immobilized state). Does not effect the particle state directly inside
     * this method.
     *
     * @param particle
     * @param capacity
     * @param random
     * @return
     */
    public abstract boolean particleIsEroding(Particle particle, Capacity capacity, RandomGenerator random);

}
