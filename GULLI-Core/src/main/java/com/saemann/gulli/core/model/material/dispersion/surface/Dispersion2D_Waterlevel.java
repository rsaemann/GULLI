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
public class Dispersion2D_Waterlevel implements Dispersion2D_Calculator {

//    // choose between "Fisher", "Lin" and "D1" for diffusion calculation:
//    public enum DIFFTYPE {
//
//        FISCHER, LIN, D, tenH, H, Htenth, NO
//    };
//    public DIFFTYPE diffType = DIFFTYPE.D;
    /**
     * The factor in [m/s] for multiplication with the waterlevel to get the
     * Dispersion coefficient Dispersion = waterlevel * factor
     */
    protected double factor;
//    public double Dxx;       //Diffusionkoeffizient in xx direction
//    public double Dyy;       // -"- in yy direction
//    public double Dxy;       // -"- in xy direction
//    public double[] D;      // Diffusionkoeffizients vector
//
//    private double al = 5.91;       //longitudinal dispersivity (Elder: 5.93, Lin: 13)
//    private double at = 0.15;       //transversal dispersitivy (Fischer: 0.15, Lin: 1.2)
//    private double C;        //Chezy roughness
//    private final double wg = Math.sqrt(9.81);
//
//    public double[] directD = new double[]{0.01, 0.01, 0};
//    public double[] directSqrtD = new double[]{0.1, 0.1, 0};

    /**
     * Calculates the dispersion from the waterlevel in the particles's cell.
     * With a factor [m/s] : D=factor*waterlevel
     * 
     * A good approximation is between 0.1-1 m/s
     *
     * @param factor
     */
    public Dispersion2D_Waterlevel(double factor) {
        this.factor = factor;
    }

//    public double[] calculateDiffusion(double vx, double vy, Surface surface, int triangleID) {
//        return calculateDiffusion(vx, vy, surface, triangleID, null);
//    }
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
        tofill[0] = Math.sqrt(surface.getActualWaterlevel(triangleID) * factor);
        tofill[1] = tofill[0];
//                retur[0] = Math.sqrt(h * .1);
//                retur[1] = retur[0];
//        double[] retur = tofill;
//        if (retur == null) {
//            retur = new double[3];
//        }
//        double h;
//        switch (diffType) {
//            case FISCHER:
//                al = 5.91;
//                at = 0.15;
//                break;
//            case LIN:
//                al = 13;
//                at = 1.2;
//                break;
//            case D:
//                return directSqrtD;
//            case tenH:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = Math.sqrt(h * 10.);
//                retur[1] = retur[0];
//                return retur;
//            case H:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = Math.sqrt(h);
//                retur[1] = retur[0];
//                return retur;
//            case Htenth:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = Math.sqrt(h * .1);
//                retur[1] = retur[0];
//                return retur;
//
//            default:
//                D[0] = 0;
//                D[1] = 0;
//        }
//        double h = surface.getActualWaterlevel(triangleID);
//        C = surface.getkst() * Math.pow(h, 1. / 6.); //
//        Dxx = ((al * (vx * vx) + at * (vy * vy)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        Dyy = ((al * (vy * vy) + at * (vx * vx)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        System.out.println("Dxx: "+Dxx+"   Dyy="+Dyy);
        //Dxx = 1;
        //Dyy = 1;

        //altenative: D after Bear(1972) for groundwater:
        //double vabs = Math.sqrt(vx*vx+vy*vy);
        //Dxx = al*vabs + dm;
        //Dyy = at*vabs + dm;
//        tofill[0] = Math.sqrt(Dxx);
//        tofill[1] = Math.sqrt(Dyy);
//        return tofill;
    }

    @Override
    public void calculateDiffusion(double vx, double vy, Surface surface, int triangleID, double[] tofill) {
        tofill[0] = surface.getActualWaterlevel(triangleID) * factor;
        tofill[1] = tofill[0];
//        if (diffType == DIFFTYPE.D) {
//            return directD;
//        }
//        double[] retur = tofill;
//        if (retur == null) {
//            System.out.println("new double[3] for diffusion");
//            retur = new double[3];
//        }
//        double h = 0;
//        switch (diffType) {
//            case FISCHER:
//                break;
//            case LIN:
//                al = 13;
//                at = 1.2;
//                break;
//            case D:
//                return directD;
//            case tenH:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = h * 10.;
//                retur[1] = retur[0];
//                return retur;
//            case H:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = h;
//                retur[1] = h;
//                return retur;
//            case Htenth:
//                h = surface.getActualWaterlevel(triangleID);
//                retur[0] = h * .1;
//                retur[1] = retur[0];
//                return retur;
//
//            default:
//                D[0] = 0;
//                D[1] = 0;
//        }
//        double h = surface.getActualWaterlevel(triangleID);
//        C = surface.getkst() * Math.pow(h, 1. / 6.); //
//        Dxx = ((al * (vx * vx) + at * (vy * vy)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        Dyy = ((al * (vy * vy) + at * (vx * vx)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        System.out.println("Dxx: "+Dxx+"   Dyy="+Dyy);
        //Dxx = 1;
        //Dyy = 1;

        //altenative: D after Bear(1972) for groundwater:
        //double vabs = Math.sqrt(vx*vx+vy*vy);
        //Dxx = al*vabs + dm;
        //Dyy = at*vabs + dm;
//        tofill[0] = Dxx;
//        tofill[1] = Dyy;
//        tofill[2] = Dxy;

//        return tofill;
    }

// Diffusionsberechnung nach Lin (1997) : tidal flow and transport modeling using ultimate quickest scheme.
//    public double[] calculateDiffusion(double vx, double vy, double h, double kst) throws NoDiffusionStringException {
//        if (null == s) {
//            throw new NoDiffusionStringException();
//        } else {
//        switch (diffType) {
//            case tenH:
//                return new double[]{h * 10., h * 10.};
//            case H:
//                return new double[]{h, h};
//            case Htenth:
//                return new double[]{h * 0.1, h * 0.1};
//            case FISCHER:
//                al = 5.91;
//                at = 0.15;
//                break;
//            case LIN:
//                al = 13;
//                at = 1.2;
//                break;
//            case D:
//                return directD;
//            default:
//                D[0] = 0;
//                D[1] = 0;
//        }
//        C = kst * Math.pow(h, 1. / 6.); //
//        Dxx = ((al * (vx * vx) + at * (vy * vy)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        Dyy = ((al * (vy * vy) + at * (vx * vx)) * h * wg) / (Math.sqrt(vx * vx + vy * vy) * C);
//        System.out.println("Dxx: "+Dxx+"   Dyy="+Dyy);
    //Dxx = 1;
    //Dyy = 1;
    //altenative: D after Bear(1972) for groundwater:
    //double vabs = Math.sqrt(vx*vx+vy*vy);
    //Dxx = al*vabs + dm;
    //Dyy = at*vabs + dm;
//        D[0] = Dxx;
//        D[1] = Dyy;
//        D[2] = Dxy;
//
//        return D;
//    }
    @Override
    public String getDiffusionString() {
        return "Waterlevel(Factor:" + factor + ")";
    }

    @Override
    public String[] getParameterOrderDescription() {
        return new String[]{"Factor"};
    }

    @Override
    public double[] getParameterValues() {
        return new double[]{factor};
    }

    @Override
    public void setParameterValues(double[] parameter) {
        if (parameter != null && parameter.length > 0) {
            this.factor = parameter[0];
        }
    }

    @Override
    public String[] getParameterUnits() {
        return new String[]{"m/s"};
    }

    public class NoDiffusionStringException extends Exception {

        public NoDiffusionStringException() {
            System.err.println("no Diffusion type was set.");
        }
    }

}
