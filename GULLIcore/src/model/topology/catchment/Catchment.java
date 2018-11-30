/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.catchment;

import java.util.List;
import model.GeoPosition2D;
import model.topology.Capacity;

/**
 *
 * @author saemann
 */
public class Catchment {
    
    protected String name;
    protected Capacity outlet;
    protected List<GeoPosition2D> geometry;
    protected double area=-1;
    protected double imperviousRate=-1;
    protected double slope=-1;

    public Catchment(String name, Capacity outlet, List<GeoPosition2D> area) {
        this.name = name;
        this.outlet = outlet;
        this.geometry = area;
    }

    public Catchment(String name) {
        this.name = name;
    }

    public void setGeometry(List<GeoPosition2D> area) {
        this.geometry = area;
    }

    public String getName() {
        return name;
    }

    public List<GeoPosition2D> getGeometry() {
        return geometry;
    }

    public void setOutlet(Capacity inlet) {
        this.outlet = inlet;
    }

    public void setImperviousRate(double imperviousRate) {
        this.imperviousRate = imperviousRate;
    }

    public double getImperviousRate() {
        return imperviousRate;
    }

    public Capacity getOutlet() {
        return outlet;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }
    
    

    
}

