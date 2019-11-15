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

import java.util.Random;

/**
 * Returns uniform and normal distributed numbers, that have been
 * pre-initialized and do not need to be generated for every call.
 *
 * @author saemann
 */
public class RandomArray {

    private final double[] gaussians, uniform;
    private int index = 0;
    private Random r;

    public RandomArray(Random randomNumberGenerator, int numberOfValues) {
        this.r = randomNumberGenerator;
        gaussians = new double[numberOfValues];
        uniform = new double[numberOfValues];
        for (int i = 0; i < gaussians.length; i++) {
            gaussians[i] = randomNumberGenerator.nextGaussian();
            uniform[i] = randomNumberGenerator.nextDouble();
        }
        index = 0;
    }

    public double nextGaussian() {
//        if(true)return r.nextGaussian();
        index++;
        if (index >= gaussians.length) {
            index = 0;
        }

        return gaussians[index];
    }

    public double nextDouble() {
//        if(true)return r.nextDouble();
        index++;
        if (index >= uniform.length) {
            index = 0;
        }

        return uniform[index];
    }

    public void resetIndex() {
        this.index = 0;
    }

    public void testEquals(RandomArray other) {
        boolean equal = true;
        for (int i = 0; i < uniform.length; i++) {
            if (uniform[i] != other.uniform[i]) {
                equal = false;
                System.out.println("Entry uniform[" + i + "] is not equal :" + this.uniform[i] + " != " + other.uniform[i]);
            }
        }
//        if (equal) {
//            System.out.println("Entries are equal");
//        }
    }

}
