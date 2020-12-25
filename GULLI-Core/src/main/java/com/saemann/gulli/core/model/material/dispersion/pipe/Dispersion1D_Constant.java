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
package com.saemann.gulli.core.model.material.dispersion.pipe;

import com.saemann.gulli.core.model.particle.Particle;

/**
 * Returns a constsnt value (default 2m^2/s)
 *
 * @author robert saemann
 */
public class Dispersion1D_Constant implements Dispersion1D_Calculator {

    /**
     * Dispersion coefficient [m^2/s]
     */
    protected double dispersion = 2;

    protected double sqrt = Math.sqrt(dispersion);

    public Dispersion1D_Constant() {
    }

    public Dispersion1D_Constant(double dispersionCoefficient) {
        this.dispersion=dispersionCoefficient;
        this.sqrt=Math.sqrt(dispersionCoefficient);
    }

    @Override
    public double getDispersionCoefficient(Particle p) {
        return dispersion;
    }

    @Override
    public double getSQRTDispersionCoefficient(Particle p) {
        return sqrt;
    }

    /**
     * The constant dispersion [m^2/s] Must be positive.
     *
     * @param dispersion
     */
    public void setDispersion(double dispersion) {
        this.dispersion = dispersion;
        this.sqrt = Math.sqrt(dispersion);
    }

}
