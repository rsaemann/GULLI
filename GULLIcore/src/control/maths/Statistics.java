/*
 * The MIT License
 *
 * Copyright 2020 saemann.
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

/**
 * Helping class to perform basic statisitcs analysis and calculation
 *
 * @author saemann
 */
public class Statistics {

    public static double mean(double[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum / (double) values.length;
    }

    public static double var(double[] values) {
        double mean = mean(values);
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += (values[i] - mean) * (values[i] - mean);
        }
        return sum / (double) values.length;
    }

    public static double standardDeviation(double[] values) {
        return Math.sqrt(var(values));
    }
    
    public static double median(double[] orderedValues){
        if(orderedValues.length%2==0){
            //2 values to evaluate
            return (orderedValues[(int)orderedValues.length/2]+orderedValues[(int)orderedValues.length/2+1])*0.5;
        }else{
            //evaluate the mid value
            return orderedValues[(int)orderedValues.length/2];
        }
    }

    public static void orderAscending(double[] values) {
        boolean changed;
        for (int i = 1; i < values.length; i++) {
            changed = false;
            for (int j = 0; j < values.length - i; j++) {
                if (values[j] > values[j + 1]) {
                    double temp = values[j];
                    values[j] = values[j + 1];
                    values[j + 1] = temp;
                    changed = true;
                }
            }
            if (!changed) {
                return;
            }
        }
    }

}
