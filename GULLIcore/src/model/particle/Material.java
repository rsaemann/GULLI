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
    public int materialIndex = 0;

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

}
