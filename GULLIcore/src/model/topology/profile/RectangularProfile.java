/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.profile;

/**
 *
 * @author saemann
 */
public class RectangularProfile extends Profile{
    
    protected double width,maxHeight;

    public double getMaxHeight() {
        return maxHeight;
    }

    public double getWidth() {
        return width;
    }
    
    

    public RectangularProfile(double width, double maxHeight) {
        this.width = width;
        this.maxHeight = maxHeight;
    }

    @Override
    public double getTotalArea() {
        return width*maxHeight;
    }

    @Override
    public double getFlowArea(double water_level_in_pipe) {
        return width*water_level_in_pipe;
    }

    @Override
    public double getWettedPerimeter(double water_level_in_pipe) {
        return 2*width+2*water_level_in_pipe;
    }

    @Override
    public double getHydraulicDiameter(double water_level_in_pipe) {
        return 4*getFlowArea(water_level_in_pipe)/getWettedPerimeter(water_level_in_pipe);
    }

    @Override
    public double getHydraulicRadius(double water_level_in_pipe) {
        return 2*getFlowArea(water_level_in_pipe)/getWettedPerimeter(water_level_in_pipe);
    }

    @Override
    public double getTopChannelWidth(double water_level_in_pipe) {
        return width;
    }

    @Override
    public double getFlow(double water_level_in_pipe, double k, double hydraulicGradient, double fr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double symmetricBoundaryCoordinate(double z) {
        return width*0.5;
    }

    @Override
    public boolean isFreeflow(double water_level_in_pipe) {
        return true;
    }

    @Override
    public double getStreamlineVelocity(double y, double z, double water_level_in_pipe, double roughnessK, double average_shear_velocity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getLaminarAverageBoundaryShearStress(double water_level_in_pipe, double decline, double weight) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getLaminarVelocity(double ywidth, double zheight, double water_level_in_pipe, double gradH, double weight, double dynamicViscosity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getLaminarUniformVelocity(double ywidth, double zheight, double water_level_in_pipe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getWaterLevel_byFlowArea(double flowArea) {
        return flowArea/width;
    }

    @Override
    public String toString() {
        return "Rectangle (width:"+width+" m)";
    }

    @Override
    public double getFillRate(double waterlevel) {
        return waterlevel/maxHeight;
    }
    
}
