/*
 * The MIT License
 *
 * Copyright 2019 saemann.
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
package com.saemann.gulli.core.control.maths;

import java.util.SplittableRandom;

/**
 * Returns uniform and normal distributed numbers, that have been
 * pre-initialized and do not need to be generated for every call.
 *
 * @author saemann
 */
public class RandomGenerator {

    private boolean haveNextFastGaussian;
    private double nextFastGaussian;

    protected final long seed;
    protected SplittableRandom sr;
    public static int numberOfGaussLoops = 0;
    public static int numberOfDoubleLoops = 0;
    private double v1, v2;
    private final double pi2 = Math.PI * 2;
//    private double sqrtlogv1;

    public RandomGenerator(long seed) {
        this.seed = seed;
        sr = new SplittableRandom(seed);
    }

    public double nextGaussian() {
        return nextfastGaussian();
    }

    public double nextDouble() {
        return sr.nextDouble();
    }

    public void reset() {
        sr = new SplittableRandom(seed);
        haveNextFastGaussian = false;
        nextFastGaussian = 0;
    }

    public boolean hasEqualValues(RandomGenerator other) {
        return other.seed == this.seed;
    }

    public long getSeed() {
        return seed;
    }

    //inspired by https://gist.github.com/brendano/4561065
    protected double nextfastGaussian() {
        if (!haveNextFastGaussian) {
            v1 = sr.nextDouble();
            v2 = sr.nextDouble();
            double sqrtlogv1 = Math.sqrt(-2 * Math.log(v1));
            nextFastGaussian = sqrtlogv1 * Math.sin(pi2 * v2);
            haveNextFastGaussian = true;
            return sqrtlogv1 * Math.cos(pi2 * v2);
        } else {
            haveNextFastGaussian = false;
            return nextFastGaussian;
        }
    }

}
