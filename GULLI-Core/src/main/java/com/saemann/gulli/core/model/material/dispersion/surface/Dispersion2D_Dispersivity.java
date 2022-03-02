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
 * actual situation. Uses the Fischer parameters to create a elipsoidal
 * dispersion field that uses the velocity direction of the particle.
 *
 * @author riss, saemann
 */
public class Dispersion2D_Dispersivity implements Dispersion2D_Calculator {


    private double al = 0.1;       //longitudinal dispersivity (Elder: 5.93, Lin: 13)
    private double at = 0.01;       //transversal dispersitivy (Fischer: 0.15, Lin: 1.2)

    public Dispersion2D_Dispersivity() {
    }
    
    public Dispersion2D_Dispersivity(double longDisp, double transvDisp) {
        this.al=longDisp;
        this.at=transvDisp;
    }

    /**
     * Calculate sqrt(Dxx) and sqrt(Dyy) for the given particle surrounding.
     * Already the sqrt is calculated to optimize further calculation with these
     * values.
     *
     * @param vx
     * @param vy
     * @param surface
     * @param triangleID
     * @param tofill
     */
    @Override
    public void calculateDiffusionSQRT(double vx, double vy, Surface surface, int triangleID, double[] tofill) {

//        double h = surface.getActualWaterlevel(triangleID);
//        if (h == 0) {
//            tofill[0] = 0;
//            tofill[1] = 0;
//            return;
//        }
        
        double v=(Math.sqrt(vx * vx + vy * vy) );
        double Dl=v*al;
        double Dt=v*at;
        
        tofill[0] = Math.sqrt(Dl);
        tofill[1] = Math.sqrt(Dt);

    }

    @Override
    public void calculateDiffusion(double vx, double vy, Surface surface, int triangleID, double[] tofill) {
        double h = surface.getActualWaterlevel(triangleID);
        if (h == 0) {
            tofill[0] = 0;
            tofill[1] = 0;
            return;
        }
        
        double v=(Math.sqrt(vx * vx + vy * vy) );
        double Dl=v*al;
        double Dt=v*at;
        
        tofill[0] = Dl;
        tofill[1] = Dt;
    }

    @Override
    public String getDiffusionString() {
        return "Dispersivity(" + al + ";" + at + ")";
    }

    @Override
    public String[] getParameterOrderDescription() {
        return new String[]{"lateral Dispersivity", "transversal Dispersivity"};
    }

    @Override
    public double[] getParameterValues() {
        return new double[]{al, at};
    }

    @Override
    public void setParameterValues(double[] parameter) {
        if (parameter != null && parameter.length > 1) {
            this.al = parameter[0];
            this.at = parameter[1];
        }
    }

    @Override
    public String[] getParameterUnits() {
        return new String[]{"m", "m"};
    }

    @Override
    public boolean isIsotropic() {
        return false;
    }

    /**
     * [m]
     * @return 
     */
    public double getLongitudinalDispersivity() {
        return al;
    }

    /**
     * [m]
     * @return 
     */
    public double getTransversalDispersivity() {
        return at;
    }
    
    
    
    

}
