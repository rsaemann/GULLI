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
package com.saemann.gulli.core.model.material.dispersion.surface;

import com.saemann.gulli.core.model.surface.Surface;

/**
 * Calculates the dispersion/diffusion on a 2D surface for a particle for the
 * actual situation.
 *
 * @author saemann
 */
public interface Dispersion2D_Calculator {

     /**
     * Calculate Dxx and Dyy [m^2/s] for the given particle surrounding.
     * Already the sqrt is calculated to optimize further calculation with these
     * values.
     *
     * @param vx
     * @param vy
     * @param surface
     * @param triangleID
     * @param tofill double[2] array storing Dxx and Dyy
     * @return
     */
    public double[] calculateDiffusion(double vx, double vy, Surface surface, int triangleID, double[] tofill);

    /**
     * Calculate sqrt(Dxx) and sqrt(Dyy) [m/s^(1/2)] for the given particle surrounding.
     * Already the sqrt is calculated to optimize further calculation with these
     * values.
     *
     * @param vx
     * @param vy
     * @param surface
     * @param triangleID
     * @param tofill
     * @return
     */
    public double[] calculateDiffusionSQRT(double vx, double vy, Surface surface, int triangleID, double[] tofill);

    /**
     * A description to identify the type of dispersion calculation
     *
     * @return
     */
    public String getDiffusionString();

    /**
     * An array of descriptions for all parameters, that can be modified.
     *
     * @return descriptions
     */
    public String[] getParameterOrderDescription();

    /**
     * The values of the parameters that could be modified. Description and
     * order corresponds to those in getParameterOrderDescription().
     *
     * @return values
     */
    public double[] getParameterValues();

    /**
     * Set values of the parameters. order corresponds to those in
     * getParameterOrderDescription()
     *
     * @param parameter parameters to set.
     */
    public void setParameterValues(double[] parameter);

}
