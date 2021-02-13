/*
 * The MIT License
 *
 * Copyright 2018 saemann.
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
package com.saemann.gulli.core.control.scenario;

import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Constant;
import com.saemann.gulli.core.io.FileContainer;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import java.util.Collection;

/**
 * information about one simulation setup and its scenario
 *
 * @author saemann
 */
public class Setup {

    public FileContainer files;

    public boolean useSurface = true;

    public Scenario scenario;

    public Collection<InjectionInfo> injections;
    public Collection<Material> materials;
    
    private boolean loadResultInjections = true;

    protected double timestepTransport = 1;
    protected Dispersion2D_Constant diffusion;
//    protected double networkdispersion = 2;
    protected boolean routingSurfaceEnterDryCells = true;
    protected double routingSurfaceDryflowVelocity = 0.005;

    protected double pipeMeasurementtimestep = 300;
    protected boolean pipeMeasurementSynchronize = true;
    protected boolean pipeMeasurementSpatialConsistent = true;
    protected boolean pipeMeasurementTimeContinuous = false;

    protected double surfaceMeasurementtimestep = 300;
    protected boolean surfaceMeasurementSynchronize = true;
    protected boolean surfaceMeasurementTimeContinuous = true;
    protected boolean surfaceMeasurementSpatialConsistent = true;
    protected SurfaceMeasurementRaster surfaceMeasurementRasterClass;

    public FileContainer getFiles() {
        return files;
    }

    public void setFiles(FileContainer files) {
        this.files = files;
    }

    public boolean isUseSurface() {
        return useSurface;
    }

    public void setUseSurface(boolean useSurface) {
        this.useSurface = useSurface;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public Collection<InjectionInfo> getInjections() {
        return injections;
    }

    public void setInjections(Collection<InjectionInfo> injections) {
        this.injections = injections;
    }

    public void setMaterials(Collection<Material> materials) {
        this.materials = materials;
    }
    
    

    public double getTimestepTransport() {
        return timestepTransport;
    }

    public void setTimestepTransport(double timestepTransport) {
        this.timestepTransport = timestepTransport;
    }

    public double getPipeMeasurementtimestep() {
        return pipeMeasurementtimestep;
    }

    public void setPipeMeasurementtimestep(double pipeMeasurementtimestep) {
        this.pipeMeasurementtimestep = pipeMeasurementtimestep;
    }

    public boolean isPipeMeasurementSynchronize() {
        return pipeMeasurementSynchronize;
    }

    public void setPipeMeasurementSynchronize(boolean pipeMeasurementSynchronize) {
        this.pipeMeasurementSynchronize = pipeMeasurementSynchronize;
    }

    public boolean isPipeMeasurementSpatialConsistent() {
        return pipeMeasurementSpatialConsistent;
    }

    public void setPipeMeasurementSpatialConsistent(boolean pipeMeasurementSpatialConsistent) {
        this.pipeMeasurementSpatialConsistent = pipeMeasurementSpatialConsistent;
    }

    public boolean isPipeMeasurementTimeContinuous() {
        return pipeMeasurementTimeContinuous;
    }

    public void setPipeMeasurementTimeContinuous(boolean pipeMeasurementTimeContinuous) {
        this.pipeMeasurementTimeContinuous = pipeMeasurementTimeContinuous;
    }

    public double getSurfaceMeasurementtimestep() {
        return surfaceMeasurementtimestep;
    }

    public void setSurfaceMeasurementtimestep(double surfaceMeasurementtimestep) {
        this.surfaceMeasurementtimestep = surfaceMeasurementtimestep;
    }

    public boolean isSurfaceMeasurementSynchronize() {
        return surfaceMeasurementSynchronize;
    }

    public void setSurfaceMeasurementSynchronize(boolean surfaceMeasurementSynchronize) {
        this.surfaceMeasurementSynchronize = surfaceMeasurementSynchronize;
    }

    public boolean isSurfaceMeasurementTimeContinuous() {
        return surfaceMeasurementTimeContinuous;
    }

    public void setSurfaceMeasurementTimeContinuous(boolean surfaceMeasurementTimeContinuous) {
        this.surfaceMeasurementTimeContinuous = surfaceMeasurementTimeContinuous;
    }

    public boolean isSurfaceMeasurementSpatialConsistent() {
        return surfaceMeasurementSpatialConsistent;
    }
    
    public void setSurfaceMeasurementSpatialConsistent(boolean surfaceMeasurementSpatialConsistent) {
        this.surfaceMeasurementSpatialConsistent = surfaceMeasurementSpatialConsistent;
    }

    public boolean isLoadResultInjections() {
        return loadResultInjections;
    }

    public void setLoadResultInjections(boolean loadResultInjections) {
        this.loadResultInjections = loadResultInjections;
    }

    public Dispersion2D_Constant getSurfaceDiffusion() {
        return diffusion;
    }

    public void setSurfaceDiffusion(Dispersion2D_Constant diffusion) {
        this.diffusion = diffusion;
    }

//    public double getNetworkdispersion() {
//        return networkdispersion;
//    }

//    public void setNetworkdispersion(double networkdispersion) {
//        this.networkdispersion = networkdispersion;
//    }

    public SurfaceMeasurementRaster getSurfaceMeasurementRasterClass() {
        return surfaceMeasurementRasterClass;
    }

    public void setSurfaceMeasurementRasterClass(SurfaceMeasurementRaster surfaceMeasurementRasterClass) {
        this.surfaceMeasurementRasterClass = surfaceMeasurementRasterClass;
    }

    public boolean isRoutingSurfaceEnterDryCells() {
        return routingSurfaceEnterDryCells;
    }

    public void setRoutingSurfaceEnterDryCells(boolean routingSurfaceEnterDryCells) {
        this.routingSurfaceEnterDryCells = routingSurfaceEnterDryCells;
    }

    public double getRoutingSurfaceDryflowVelocity() {
        return routingSurfaceDryflowVelocity;
    }

    public void setRoutingSurfaceDryflowVelocity(double routingSurfaceDryflowVelocity) {
        this.routingSurfaceDryflowVelocity = routingSurfaceDryflowVelocity;
    }

}
