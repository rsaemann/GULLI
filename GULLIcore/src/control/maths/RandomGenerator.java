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
package control.maths;

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

    private final long seed;
    private SplittableRandom sr;
    public static int numberOfGaussLoops = 0;
    public static int numberOfDoubleLoops = 0;
    private double v1, v2, x1;

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
    }
    
    public boolean hasEqualValues(RandomGenerator other) {
        return other.seed==this.seed;
    }

    public long getSeed() {
        return seed;
    }

    //inspired by https://gist.github.com/brendano/4561065
    public double nextfastGaussian() {
        if (!haveNextFastGaussian) {
            v1 = sr.nextDouble();
            v2 = sr.nextDouble();
            x1 = Math.sqrt(-2 * Math.log(v1)) * Math.cos(2 * Math.PI * v2);
            nextFastGaussian = Math.sqrt(-2 * Math.log(v1)) * Math.sin(2 * Math.PI * v2);
            haveNextFastGaussian = true;
            return x1;
        } else {
            haveNextFastGaussian = false;
            return nextFastGaussian;
        }
    }

}