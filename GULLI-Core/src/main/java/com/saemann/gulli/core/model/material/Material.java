/*
 * The MIT License
 *
 * Copyright 2018 Robert Saemann.
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
package com.saemann.gulli.core.model.material;

import com.saemann.gulli.core.model.material.routing.Routing_Homogene;
import com.saemann.gulli.core.control.GlobalParameter;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Calculator;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Constant;
import com.saemann.gulli.core.model.material.routing.Routing_Heterogene;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Calculator;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Constant;
import java.util.Objects;
import com.saemann.gulli.core.model.material.routing.Routing_Calculator;

/**
 * Material of physical information to be attached to a particle. This includes
 * the routing: advection,dispersion,flow-decision parameters, that have to be
 * defined in every simulation loop for every particle, but can differ because
 * of the physical material.
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
     * A Routing_Calculator decides for a particle of this Material where to get
 transported to in case of a junction.
     */
    protected Routing_Calculator routing;
    
    public enum ROUTING{HOMOGENEOUS,HETEROGENEOUS};

    /**
     * Calculates the dispersion coefficient of a particle in the pipe system
     */
    protected Dispersion1D_Calculator dispersionPipe;
    
    public enum DISPERSION_PIPE{NONE,CONSTANT};//,DISPERSIVITY};

    /**
     * Calculates the dispersion coefficient of a particle on the surface
     */
    protected Dispersion2D_Calculator dispersionSurface;
    
    public enum DISPERSION_SURFACE{NONE,CONSTANT,FISCHER,WATERLEVEL}

    /**
     * Index to identify the material. Index for storing in measurement
     * timelines for counting particles of this material.
     */
    public int materialIndex = -1;
    
    public float travellengthToMeasure=-1;

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
            routing = new Routing_Homogene();
        } else {
            routing = new Routing_Heterogene(0.1, 0.1);
        }
        this.dispersionPipe=new Dispersion1D_Constant();
        this.dispersionSurface=new Dispersion2D_Constant(0.1, 0.1);
    }

    public Material(String name, double density, boolean solute, int surfaceindex) {
        this(name, density, solute);
        this.materialIndex = surfaceindex;
    }
    
    public Material(String name, double density, int index, Routing_Calculator routing, Dispersion1D_Calculator dispersion_pipe,Dispersion2D_Calculator dispersion_surface){
        this.name=name;
        this.density=density;
        this.weight = this.density * GlobalParameter.GRAVITY;
        this.materialIndex=index;
        this.routing=routing;
        this.dispersionPipe=dispersion_pipe;
        this.dispersionSurface=dispersion_surface;
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

    public Routing_Calculator getRoutingCalculator() {
        return routing;
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
        return Objects.equals(this.routing, other.routing);
    }

    public Dispersion2D_Calculator getDispersionCalculatorSurface() {
        return dispersionSurface;
    }

    public Dispersion1D_Calculator getDispersionCalculatorPipe() {
        return dispersionPipe;
    }

    public void setDispersionCalculatorPipe(Dispersion1D_Calculator dispersionCalculatorPipe) {
        this.dispersionPipe = dispersionCalculatorPipe;
    }

    public void setDispersionCalculatorSurface(Dispersion2D_Calculator dispersionCalculatorSurface) {
        this.dispersionSurface = dispersionCalculatorSurface;
    }

    public void setRoutingCalculator(Routing_Calculator flowCalculator) {
        this.routing = flowCalculator;
    }

    @Override
    public String toString() {
        return "Material{[" + materialIndex + "] '" + name + "', " + density + "kg/m³, " + (solute ? "dissolved" : "non-solute") + '}';
    }

}
