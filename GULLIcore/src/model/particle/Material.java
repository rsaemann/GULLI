/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.particle;

import control.particlecontrol.FlowCalculator;
import control.particlecontrol.FlowCalculatorMixed;
import control.GlobalParameter;
import control.particlecontrol.FlowCalculator_Heterogene;
import java.util.Objects;

/**
 * Material of physical information to be attached to a particle.
 *
 * @author saemann
 */
public class Material {

    protected String name;

    /**
     * Density at 20°C [kg/m³] (example: Water=1000 kg/m³)
     */
    protected final double density;

    /**
     * Weight is density x gravity [N/m³]
     */
    protected final double weight;

    /**
     * True if this material is homogenious dissolved in the water. False for
     * seperated material that swims on top or bottom .
     */
    protected boolean solute;

    /**
     * A FlowCalculator decides for a particle of this Material where to get
     * transported to in case of a junction.
     */
    protected FlowCalculator flowCalculator;

    /**
     * Index to identify the material. Index for storing in measurement
     * timelines for counting particles of this material.
     */
    public int materialIndex = -1;

    /**
     *
     * @param name
     * @param density kg/m^3 e.g. 1000 for water
     * @param solute true if completely mixed in drainage water.
     */
    public Material(String name, double density, boolean solute) {
        this.name = name;
        this.density = density;
        this.weight = this.density * GlobalParameter.GRAVITY;
        this.solute = solute;
        if (solute) {
            flowCalculator = new FlowCalculatorMixed();
        } else {
            flowCalculator = new FlowCalculator_Heterogene(0.1, 0.1);
        }
    }

    public Material(String name, double density, boolean solute, int surfaceindex) {
        this(name, density, solute);
        this.materialIndex = surfaceindex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    

    public double getDensity() {
        return density;
    }

    /**
     * Get weight of this material in [N/m³] as it is density x gravity.
     *
     * @return weight
     */
    public double getWeight() {
        return weight;
    }

    public FlowCalculator getFlowCalculator() {
        return flowCalculator;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.name);
        hash = 17 * hash + this.materialIndex;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Material other = (Material) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (Double.doubleToLongBits(this.density) != Double.doubleToLongBits(other.density)) {
            return false;
        }
        if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
            return false;
        }
        if (this.solute != other.solute) {
            return false;
        }
        return Objects.equals(this.flowCalculator, other.flowCalculator);
    }

    @Override
    public String toString() {
        return "Material{["+materialIndex + "] '" + name + "', " + density + "kg/m³, " + (solute?"dissolved":"non-solute") +'}';
    }

    
    
}
