/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.profile;

import java.text.DecimalFormat;

/**
 *
 * @author saemann
 */
public class CircularProfile extends Profile {

    /**
     * Diameter [m]
     *
     * Durchmesser [meter]
     */
    protected final float diameter;

    /**
     * inner radius [m]
     *
     * innen Radius [meter]
     */
    protected final double radius;
    
    protected final double totalarea;

    /*storage variables to speed up calculation of partly filled pipes*/
    private double lastFlowAreaWL = -1;
    private double lastAlphaWaterlevel = -1;
    private double lastCalculatedAlpha = 0;
    private double lastCalculatedArea = 0;
    private double lastCalculatedHydraulicRadius = 0;
    private double lastCalculatedHydraulicRadiusWL = -1;
    
    //Lookup tables for area
//    private double[] flowarea;

    DecimalFormat df = new DecimalFormat("0.0000");
    
    public static boolean verbose=false;

    public CircularProfile(float diameter_meter) {
        this.diameter = diameter_meter;
        this.radius = diameter_meter * 0.5;
        this.totalarea=Math.PI*radius*radius;
//        initLookupTable();
    }

    public CircularProfile(double diameter_meter) {
        this.diameter = (float) diameter_meter;
        this.radius = diameter_meter * 0.5;
        this.totalarea=Math.PI*radius*radius;
//        initLookupTable();
    }
    
//    public void initLookupTable(){
//        //Flowarea by waterheight 0-100%
//        this.flowarea=new double[101];
//        for (int i = 0; i < flowarea.length; i++) {
//            double h=i*diameter/(flowarea.length-1);
//             //Calculate angle of hydraulic U
//            double angle = 2 * getAlpha(h);
//            //Calculate hydraulic area
//            double area=diameter * diameter * 0.125 * (angle - Math.sin(angle));
//            flowarea[i]=area;
//        }
//    }

    /**
     * According to Guo Alpha is the angle from plumb height-axis at the center
     * around the middle point at the center radius-line, that connects middle-
     * point and free-surface to pipe-wall contact.
     *
     * Water surface angle according to Guo (2015)
     *
     * @param water_level_in_pipe
     * @return water surface angle [rad]
     */
    private double getAlpha(double water_level_in_pipe) {
//        if (water_level_in_pipe == lastAlphaWaterlevel) {
//            return lastCalculatedAlpha;
//        }
//        lastCalculatedAlpha = Math.acos(1 - water_level_in_pipe / radius);
//        lastAlphaWaterlevel = water_level_in_pipe;
//        return lastCalculatedAlpha;
        return Math.acos(1 - water_level_in_pipe / radius);
    }

    @Override
    public double getTotalArea() {
        return totalarea;
    }

    @Override
    public double getFlowArea(double water_level_in_pipe) {
        if(verbose)System.out.println(getClass()+"::Lookup FlowArea");
        if (water_level_in_pipe <= 0) {
            return 0;
        }
        if (water_level_in_pipe >= diameter) {
            return totalarea;
        } else {
            
            //Calculate angle of hydraulic U
            double angle = 2 * getAlpha(water_level_in_pipe);
            //Calculate hydraulic area
            return diameter * diameter * 0.125 * (angle - Math.sin(angle));
        }
    }

    @Override
    public double getHydraulicRadius(double water_level_in_pipe) {
        if(verbose)System.out.println(getClass()+"::getHydraulicRadius");
        synchronized (this) {
            if (water_level_in_pipe == lastCalculatedHydraulicRadiusWL) {
//                if (lastCalculatedHydraulicRadius < 0.001) {
//                    System.out.println("Hydraulic Radius: " + df.format(lastCalculatedHydraulicRadius) + "   h=" + water_level_in_pipe + "   bekannt d=" + diameter);
//                }
                return lastCalculatedHydraulicRadius;
            }

            lastCalculatedHydraulicRadiusWL = water_level_in_pipe;
            if (water_level_in_pipe > diameter) {
                lastCalculatedHydraulicRadius = 0.25 * diameter;
//                if (lastCalculatedHydraulicRadius < 0.001) {
//                    System.out.println("Hydraulic Radius: setze" + df.format(lastCalculatedHydraulicRadius) + "   h=" + water_level_in_pipe + "   h>d d=" + diameter);
//                }
                return lastCalculatedHydraulicRadius;
            }

            //Calculate angle of hydraulic U
            double angle = 2 * getAlpha(water_level_in_pipe); //4 * Math.asin(Math.sqrt(water_level_in_pipe / diameter));
            //Calculate hydraulic area
            lastCalculatedHydraulicRadius = diameter * 0.25 * (1 - Math.sin(angle) / angle);
//            if (lastCalculatedHydraulicRadius < 0.001) {
//                System.out.println("Hydraulic Radius setze: " + df.format(lastCalculatedHydraulicRadius) + "   h=" + water_level_in_pipe + "   angle=" + angle);
//            }
            return lastCalculatedHydraulicRadius;
        }
    }

    @Override
    public double getFlow(double water_level_in_pipe, double k, double hydraulicGradient, double fr) {
        if (water_level_in_pipe > diameter * 0.9) {
            //Rohrströmung
        } else {
            //Gerinneströmung

        }
        double rh = getHydraulicRadius(water_level_in_pipe);
        //   System.out.println("r_h = " + rh + " m");
        //   System.out.println("A_h = " + getFlowArea(water_level_in_pipe) + "m²");
        return 4 * Math.log10(4 * fr * rh / (k * 1000)) * 4.429447 * Math.sqrt(rh * Math.abs(hydraulicGradient)) * getFlowArea(water_level_in_pipe);
    }

    @Override
    public String toString() {
        return "Circ. DN " + (int) (diameter * 1000);
    }

    @Override
    public double getWettedPerimeter(double water_level_in_pipe) {
        if (water_level_in_pipe <= 0) {
            return 0;
        }
        if (water_level_in_pipe >= diameter) {
            return diameter * Math.PI;
        }
        return diameter * getAlpha(water_level_in_pipe);
    }

    @Override
    public double getTopChannelWidth(double water_level_in_pipe) {
        if (water_level_in_pipe <= 0) {
            return 0;
        }
        if (water_level_in_pipe >= diameter) {
            return 0;
        }
        return diameter * Math.sin(getAlpha(water_level_in_pipe));
    }

    @Override
    public double symmetricBoundaryCoordinate(double z) {
        if (z <= 0) {
//            System.out.println("Z= "+z+" is smaller than 0.");
            return 0;
        }
        if (z >= diameter) {
//            System.out.println("Z= "+z+" is greater than diameter "+diameter+".");
            return 0;
        }
        double r = diameter * 0.5;
        return r * Math.sqrt((2 - z / r) * z / r);
    }

    @Override
    public double getHydraulicDiameter(double water_level_in_pipe) {
        if(verbose)System.out.println(getClass()+"::getHydraulicDiameter");
        if (water_level_in_pipe <= 0) {
//            System.out.println("h<0");
            return 0;
        }
        if (water_level_in_pipe >= diameter) {
//            System.out.println("h>d");
            return diameter;
        }
        double ah=getFlowArea(water_level_in_pipe);
        double p= getWettedPerimeter(water_level_in_pipe);
        double dh=4. *  ah/p;
//        System.out.println("h="+water_level_in_pipe+"\tAh="+ah+"\tP="+p+"\tDh="+dh);
        
        return dh;//0.25 * (1 - Math.sin(getAlpha(water_level_in_pipe) * 2) / (getAlpha(water_level_in_pipe) * 2)) * diameter;
    }

    @Override
    public boolean isFreeflow(double water_level_in_pipe) {
        return water_level_in_pipe < diameter * 0.95;
    }

    /**
     *
     * @param y position in y direction (horizontal) from zenterline
     * @param yb position of boundary from center line
     * @param roughnessK
     * @return
     */
    public double velocity_defect(double y, double yb, double roughnessK) {
        return -(Math.log(1 - Math.abs(y / yb)) + 1. / 3. * (1 - Math.pow(1 - Math.abs(y / yb), 3))) / roughnessK;
    }

    @Override
    public double getStreamlineVelocity(double y, double z, double water_level_in_pipe, double roughnessK, double average_shear_velocity) {
        if (z >= water_level_in_pipe) {
            return Double.NaN;
        }
        double yb = symmetricBoundaryCoordinate(z);
        double z0 = roughnessK * 0.5;
        double inner = 1.1 / roughnessK * (Math.log(z / z0) - 1. / 3. * Math.pow(z / getVelocity_dip_position(water_level_in_pipe), 3));
        double last = velocity_defect(y, yb, roughnessK);
        double v = average_shear_velocity * (inner - last);
//        System.out.println("v: "+v+"  by inner: "+inner+",\t last: "+last+"\tyb: "+yb);
        return v;
    }

    @Override
    public double getLaminarAverageBoundaryShearStress(double water_level_in_pipe, double decline, double weight) {
        double tau = weight * getFlowArea(water_level_in_pipe) * Math.abs(decline) / (getAlpha(water_level_in_pipe) * this.diameter);
        return tau;
    }

    @Override
    public double getLaminarVelocity(double ywidth, double zheight, double water_level_in_pipe, double gradH, double weight, double dynamicViscosity) {
        if (zheight > water_level_in_pipe) {
            return 0;
        }
        boolean filled = water_level_in_pipe > diameter * 0.9;
//        if (filled) {
        double r2 = radius * radius;
        double z = zheight - radius;
        double velocity = (weight * Math.abs(gradH) * 0.25) * (r2 - ((ywidth * ywidth) + (z * z))) / dynamicViscosity;
        return velocity;
//        } else {
//            throw new UnsupportedOperationException("Not yet implemented for partially filled circular profiles. Sorry");
//        }
    }

    @Override
    public double getLaminarUniformVelocity(double ywidth, double zheight, double water_level_in_pipe) {
        double x = ywidth;
        double y = zheight - water_level_in_pipe;
        double alpha = getAlpha(water_level_in_pipe);
        double u = 1 - ((x / radius) * (x / radius) + Math.pow((y / radius - Math.cos(alpha)), 2));
        return u * radius * radius;
    }

    public double getRadius() {
        return radius;
    }

    public float getDiameter() {
        return diameter;
    }

    @Override
    public double getWaterLevel_byFlowArea(double flowArea) {
        double A = flowArea;
        double r = radius;
        double r2 = r * r;

        //Iterate alpha
        double gap = 2 * A / r2;
//        double lastAlpha = Double.MAX_VALUE;
        double lastAlpha2 = Double.MAX_VALUE;
        double lastAlpha = 2 * Math.PI * flowArea / getTotalArea();
        double newAlpha = Math.sin(lastAlpha) + gap;
        double loops = 15;

        while (loops > 0 && Math.abs(lastAlpha - newAlpha) > 0.001) {
            lastAlpha2 = lastAlpha;
            lastAlpha = newAlpha;

            newAlpha = gap + (Math.sin(lastAlpha) * 4. + Math.sin(lastAlpha2)) * 0.2;
            loops--;
//            System.out.println("new alpha= " + newAlpha + "  delta=" + Math.abs(lastAlpha - newAlpha));
        }
//        System.out.println("Raus bei alpha = " + newAlpha);

        return r * (1 - Math.cos(newAlpha * 0.5));
    }

    public static void main1(String[] args) {
        CircularProfile cp = new CircularProfile(1);
        for (int i = 0; i < 20; i++) {
            System.out.println("h=" + i * 0.05 + "  -> " + cp.getWaterLevel_byFlowArea(cp.getFlowArea(i * 0.05)));
        }
//        System.out.println("Af=0.392m² -> h=" + cp.getWaterLevel_byFlowArea(0.392));

    }

    @Override
    public double getFillRate(double waterlevel) {
        return waterlevel/diameter;
    }
}
