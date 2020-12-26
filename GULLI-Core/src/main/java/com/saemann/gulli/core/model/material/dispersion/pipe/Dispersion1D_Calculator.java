/*
 * The MIT License
 *
 * Copyright 2020 Robert Saemann.
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
 * Definition of the dispersion in the pipe system. Used by the
 * ParticlePipeComputing to quantify the Random Walk jump.
 *
 * @author saemann
 */
public interface Dispersion1D_Calculator {

    /**
     * Dispersion coefficient [m^2/s]
     *
     * @param p
     * @return
     */
    public double getDispersionCoefficient(Particle p);

    /**
     * The squareroot of the Dispersion coefficient [m/(s^(1/2)]
     *
     * @param p
     * @return
     */
    public double getSQRTDispersionCoefficient(Particle p);

    public int getNumberOfParameters();

    public double[] getParameterValues();

    public void setParameterValues(double[] newparameters);

    public String[] getParameterDescription();

    public String[] getParameterUnits();
}
