/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
package com.saemann.gulli.core.io;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Calculator;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.Setup;
import com.saemann.gulli.core.control.scenario.SpillScenario;
import com.saemann.gulli.core.control.scenario.injection.HEAreaInflow1DInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionArealInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionInflowInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.control.scenario.injection.InjectionSubArealInformation;
import com.saemann.gulli.core.model.GeoPosition;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Calculator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.saemann.gulli.core.model.material.routing.Routing_Calculator;
import com.saemann.gulli.core.model.material.routing.Routing_Homogene;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load and save definitions of scenarios
 *
 * @author Robert Sämann
 */
public class Setup_IO {

    public static boolean saveScenario(File file, Setup setup) throws IOException {

        Scenario scenario = setup.scenario;
        if (scenario == null) {
            scenario = new Scenario() {
                @Override
                public void init(Controller c) {
                }

                @Override
                public void reset() {
                }
            };
            scenario.setName(file.getName());
        }
        FileWriter fw = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("<Scenario>");
        bw.newLine();
        bw.write("<Name>" + scenario.getName() + "</>");
        bw.newLine();
        bw.write("<InputFiles>");
        bw.newLine();
        FileContainer files = setup.files;
        if (files != null) {
            bw.write("\t<Network>");
            bw.newLine();
            bw.write("\t\t<NetworkTopology>" + files.getPipeNetwork() + "</>");
            bw.newLine();
            try {
                bw.write("\t\t<RelativeNetworkTopology>" + file.getParentFile().toPath().relativize(files.getPipeNetwork().toPath()) + "</>");
                bw.newLine();
            } catch (Exception iOException) {
            }
            bw.write("\t\t<CRS>" + files.getCrsPipes() + "</>");
            bw.newLine();
            bw.write("\t\t<NetworkFlowField>" + files.getPipeResult() + "</>");
            bw.newLine();
            try {
                bw.write("\t\t<RelativeNetworkFlowField>" + file.getParentFile().toPath().relativize(files.getPipeResult().toPath()) + "</>");
                bw.newLine();
            } catch (Exception exception) {
            }
            bw.write("\t\t<Sparse>" + setup.isSparsePipeVelocity() + "</>");
            bw.newLine();
            bw.write("\t</Network>");
            bw.newLine();
            bw.write("\t<Surface>");
            bw.newLine();
            bw.write("\t\t<SurfaceTopology>" + files.getSurfaceDirectory() + "</>");
            bw.newLine();
            if (files.getSurfaceDirectory() != null) {
                try {
                    bw.write("\t\t<RelativeSurfaceTopology>" + file.getParentFile().toPath().relativize(files.getSurfaceDirectory().toPath()) + "</>");
                    bw.newLine();
                    bw.write("\t\t<CRS>" + files.getCrsSurface() + "</>");
                    bw.newLine();
                } catch (Exception exception) {
                }
            }

            bw.write("\t\t<SurfaceFlowField>" + files.getSurfaceResult() + "</>");
            bw.newLine();
            if (files.getSurfaceResult() != null) {
                try {
                    bw.write("\t\t<RelativeSurfaceFlowField>" + file.getParentFile().toPath().relativize(files.getSurfaceResult().toPath()) + "</>");
                    bw.newLine();
                } catch (Exception exception) {
                }
            }
            bw.write("\t</Surface>");
            bw.newLine();
        }
        bw.write("</InputFiles>");
        bw.newLine();
        bw.write("<SimulationParameters>");
        bw.newLine();
        bw.write("\t<Timestep unit='s'>" + setup.getTimestepTransport() + "</>");
        bw.newLine();
        bw.write("\t<Timeintegration>" + setup.getTimeIntegration().name() + "</>");
        bw.newLine();
        bw.write("\t<Surface>");
        bw.newLine();
        bw.write("\t\t<EnterDry>" + setup.isEnterDryCells() + "</>");
        bw.newLine();
        bw.write("\t\t<DryMovement>" + setup.isDryMovement() + "</>");
        bw.newLine();
        bw.write("\t\t<DryVelocity unit='m/s'>" + setup.getDryVelocity() + "</>");
        bw.newLine();
        bw.write("\t\t<SmoothZigZag>" + setup.isSmoothZigZag() + "</>");
        bw.newLine();
        bw.write("\t\t<SlideAlongEdge>" + setup.isSlideAlongEdge() + "</>");
        bw.newLine();
        bw.write("\t\t<StopVerySlow>" + setup.isStopSlow() + "</>");
        bw.newLine();
        bw.write("\t</Surface>");
        bw.newLine();
        bw.write("</SimulationParameters>");
        bw.newLine();
        bw.write("<Measuring>");
        bw.newLine();
        bw.write("\t\t<Network>");
        bw.newLine();
        bw.write("\t\t\t<Interval unit='s'>" + setup.getPipeMeasurementtimestep() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Timecontinuous>" + setup.isPipeMeasurementTimeContinuous() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Spatialconsistent>" + setup.isPipeMeasurementSpatialConsistent() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Synchronize Writing>" + setup.isPipeMeasurementSynchronize() + "</>");
        bw.newLine();
        bw.write("\t\t</Network>");
        bw.newLine();
        bw.write("\t\t<Surface>");
        bw.newLine();
        if (setup.getSurfaceMeasurementRasterClass() != null) {
            try {
                bw.write("\t\t\t<Type>" + setup.getSurfaceMeasurementRasterClass().getClass().getName() + "</>");
                bw.newLine();
            } catch (Exception exception) {
            }
        }

        bw.write("\t\t\t<Interval unit='s'>" + setup.getSurfaceMeasurementtimestep() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Timecontinuous>" + setup.isSurfaceMeasurementTimeContinuous() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Spatialconsistent>" + setup.isPipeMeasurementSpatialConsistent() + "</>");
        bw.newLine();
        bw.write("\t\t\t<Synchronize Writing>" + setup.isSurfaceMeasurementSynchronize() + "</>");
        bw.newLine();
        bw.write("\t\t</Surface>");
        bw.newLine();
        bw.write("\t\t<Trace>");
        bw.newLine();
        bw.write("\t\t\t<Interval>" + setup.getIntervalTraceParticles() + "</>");
        bw.newLine();
        bw.write("\t\t</Trace>");
        bw.newLine();
        bw.write("</Measuring>");
        bw.newLine();
        bw.write("<Materials>");
        bw.newLine();
        if (scenario.getInjections() != null && !scenario.getInjections().isEmpty()) {
            for (int mi = 0; mi <= scenario.getMaxMaterialID(); mi++) {
                Material m = scenario.getMaterialByIndex(mi);
                if (m != null) {
                    bw.write("\t<Material id=" + mi + ">");
                    bw.newLine();
                    bw.write("\t\t<Name>" + m.getName() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Density unit='kg/m^3'>" + m.getDensity() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Flowcalculator>" + m.getRoutingCalculator().getClass().getName() + "</>");
                    bw.newLine();
                    Dispersion1D_Calculator disp1d = m.getDispersionCalculatorPipe();
                    bw.write("\t\t<Network>");
                    bw.newLine();
                    bw.write("\t\t\t<Dispersion>");
                    bw.newLine();
                    if (disp1d == null) {
                        bw.write("\t\t\t\t<Type Parameters=0>NONE</>");
                        bw.newLine();
                    } else {
                        bw.write("\t\t\t\t<Type Parameters=" + disp1d.getNumberOfParameters() + ">" + disp1d.getClass().getName() + "</>");
                        bw.newLine();
                        String[] descriptions = disp1d.getParameterDescription();
                        String[] units = disp1d.getParameterUnits();
                        double[] paramValues = disp1d.getParameterValues();
                        for (int i = 0; i < disp1d.getNumberOfParameters(); i++) {
                            bw.write("\t\t\t\t<" + descriptions[i] + " unit='" + units[i] + "'>" + paramValues[i] + "</>");
                            bw.newLine();
                        }
                    }
                    bw.write("\t\t\t</Dispersion>");
                    bw.newLine();
                    bw.write("\t\t</Network>");
                    bw.newLine();

                    bw.write("\t\t<Surface>");
                    bw.newLine();
                    bw.write("\t\t\t<Dispersion>");
                    bw.newLine();
                    Dispersion2D_Calculator disp2d = m.getDispersionCalculatorSurface();
                    if (disp2d == null) {
                        bw.write("\t\t\t\t<Type Parameters=0>NONE</>");
                        bw.newLine();
                    } else {

                        String[] descriptions = disp2d.getParameterOrderDescription();
                        String[] units = disp2d.getParameterUnits();
                        double[] paramValues = disp2d.getParameterValues();
                        bw.write("\t\t\t\t<Type Parameters=" + descriptions.length + ">" + disp2d.getClass().getName() + "</>");
                        bw.newLine();

                        for (int i = 0; i < paramValues.length; i++) {
                            bw.write("\t\t\t\t<" + descriptions[i] + " unit='" + units[i] + "'>" + paramValues[i] + "</>");
                            bw.newLine();
                        }

                    }
                    bw.write("\t\t\t</Dispersion>");
                    bw.newLine();
                    bw.write("\t\t\t<DryFlow unit='m/s'>" + setup.getRoutingSurfaceDryflowVelocity() + "</>");
                    bw.newLine();
                    bw.write("\t\t\t<EnterDry>" + setup.isRoutingSurfaceEnterDryCells() + "</>");
                    bw.newLine();
                    bw.write("\t\t</Surface>");
                    bw.newLine();
                    bw.write("\t</Material>");
                    bw.newLine();
                }
            }
        }
        bw.write("</Materials>");
        bw.newLine();
        bw.write("<Injections>");
        bw.newLine();
        bw.write("\t<FromNetworkResult>" + setup.isLoadResultInjections() + "</>");
        bw.newLine();
        if (scenario.getInjections() != null) {
            for (InjectionInfo inj : scenario.getInjections()) {
                bw.write("\t<Injection id=" + inj.getId() + ">");
                bw.newLine();
                bw.write("\t\t<Type>" + inj.getClass().getSimpleName() + "</>");
                bw.newLine();
                if (inj instanceof InjectionInformation) {
                    InjectionInformation injection = (InjectionInformation) inj;

                    bw.write("\t\t<OnSurface>" + injection.spillOnSurface() + "</>");
                    bw.newLine();

                    if (injection.spillOnSurface()) {
//                if (injection.spilldistributed) {
//                    bw.write("\t\t<Diffusive>true</>");
//                    bw.newLine();
//                } else {
                        if (injection.getCapacityID() >= 0) {
                            bw.write("\t\t<Surfacecell>" + injection.getCapacityID() + "</>");
                            bw.newLine();
                        }
//                }
                    } else if (injection.spillInManhole()) {
                        if (injection.getCapacity() != null) {
                            bw.write("\t\t<Capacity id=" + injection.getCapacity().getManualID() + ">" + injection.getCapacityName() + "</>");
                            bw.newLine();
                        } else {
                            bw.write("\t\t<Capacity>" + injection.getCapacityName() + "</>");
                            bw.newLine();
                        }
                    }
                    if (injection.getPosition() != null) {
                        bw.write("\t\t<Position>");
                        bw.newLine();
                        bw.write("\t\t\t<Latitude>" + injection.getPosition().getLatitude() + "</>");
                        bw.newLine();
                        bw.write("\t\t\t<Longitude>" + injection.getPosition().getLongitude() + "</>");
                        bw.newLine();
                        bw.write("\t\t</Position>");
                        bw.newLine();
                    }
                } else if (inj instanceof InjectionArealInformation) {
                    InjectionArealInformation ai = (InjectionArealInformation) inj;
                    bw.write("\t\t<Diffusive>true</>");
                    bw.newLine();
                    bw.write("\t\t<Load unit='kg/m^2'>" + ai.getLoad() + "</>");
                    bw.newLine();
                } else if (inj instanceof InjectionSubArealInformation) {
                    InjectionSubArealInformation ai = (InjectionSubArealInformation) inj;
                    bw.write("\t\t<Diffusive>true</>");
                    bw.newLine();
                    bw.write("\t\t<Filter>" + ai.getNameFilter() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Load unit='kg/m^2'>" + ai.getLoad() + "</>");
                    bw.newLine();
                } else if (inj instanceof HEAreaInflow1DInformation) {
                    HEAreaInflow1DInformation ai = (HEAreaInflow1DInformation) inj;
                    bw.write("\t\t<Diffusive>false</>");
                    bw.newLine();
                    bw.write("\t\t<Substance_Parameter>" + ai.getSubstanceParameterName() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Runoff_Parameter>" + ai.getRunoffParameterName() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Washoff_type>" + ai.inflowtype + "</>");
                    bw.newLine();
                    bw.write("\t\t<Load unit='kg/m^2'>" + ai.getMassload() + "</>");
                    bw.newLine();
                    bw.write("\t\t<Washoff unit='1/mm'>" + ai.getWashoffConstant() + "</>");
                    bw.newLine();
                } else if (inj instanceof InjectionInflowInformation) {
                    InjectionInflowInformation ai = (InjectionInflowInformation) inj;
                    bw.write("\t\t<Diffusive>false</>");
                    bw.newLine();
                    bw.write("\t\t<Concentration unit='kg/m^3'>" + ai.getConcentration() + "</>");
                    bw.newLine();
                }
                bw.write("\t\t<Start unit='s'>" + inj.getStarttimeSimulationsAfterSimulationStart() + "</>");
                bw.newLine();
                bw.write("\t\t<Duration unit='s'>" + inj.getDurationSeconds() + "</>");
                bw.newLine();
                bw.write("\t\t<Material id=" + inj.getMaterial().materialIndex + ">" + inj.getMaterial().getName() + "</>");
                bw.newLine();
                bw.write("\t\t<Mass unit='kg'>" + inj.getMass() + "</>");
                bw.newLine();
                bw.write("\t\t<Particles>" + inj.getNumberOfParticles() + "</>");
                bw.newLine();
                bw.write("\t\t<Active>" + inj.isActive() + "</>");
                bw.newLine();
                bw.write("\t</Injection>");
                bw.newLine();

            }
        }

        bw.write("</Injections>");
        bw.newLine();
        bw.write("<Outputs>");
        bw.newLine();
        bw.write("</Outputs>");
        bw.newLine();
        bw.write("</Scenario>");
        bw.flush();
        bw.close();
        fw.close();
        return true;
    }

    public static Setup load(File file) throws FileNotFoundException, IOException {
        Setup setup = new Setup();
        setup.files = new FileContainer(null, null, null, null, null);
        ArrayList<InjectionInfo> injections = new ArrayList<>();
        SpillScenario scenario = new SpillScenario(null, injections);
        setup.scenario = scenario;
        int state = -1;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        boolean networkRelation = false, surfaceRelation = false, trace = false;

        //Material temperary storage
        HashMap<Integer, Material> materials = new HashMap<>();
//        Material mat=null;
        int materialID = 0;
//        double materialDensity;
        String materialName = null;
        Routing_Calculator materialFlowCalculator = null;
        Dispersion1D_Calculator materialDispersionCalculatorPipe = null;
        Dispersion2D_Calculator materialDispersionCalculatorSurface = null;

        //Injection temporary storage
        int injectionID;
        String injectionType = null;
        GeoPosition injectionPosition = null;
        double injectionLatitude = Double.NaN, injectionLongitude = Double.NaN;
        boolean injectionOnSurface = false;
        boolean injectionDiffusive = false;
        int injectionCapacityID = -1;
        String injectionCapacityName = null;
        String injectionFilterString = null;
        int injection_materialID = 0;
        double injection_washoff = 0;
        String injection_washofftype = null;
        String injection_substance = null;
        String injection_runoff = null;
        double injection_load = 0;

        double injectionStart = 0;
        double injectionDuration = 0;
        double injectionMass = 1000;
        double injectionConcentration = 1;
        int injectionParticles = 10000;
        boolean injectionActive = true;

        String line = "";
        while (br.ready()) {
            try {
                line = br.readLine();
                if (state == -1) {
                    if (line.contains("<Name")) {
                        state = 1;
                    } else if (line.contains("<InputFiles")) {
                        state = 2;
                    } else if (line.contains("<SimulationParameters")) {
                        state = 3;
                    } else if (line.contains("<Materials")) {
                        state = 4;
                    } else if (line.contains("<Injections")) {
                        state = 5;
                    } else if (line.contains("<Measuring")) {
                        state = 6;
                    }
                }
                if (state == 1) {
                    if (line.contains("<Name")) {
                        String name = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        scenario.setName(name);
                        state = -1;
                        continue;
                    }
                } else if (state == 2) {
                    //InputFiles
                    if (line.contains("<NetworkTopology>")) {
                        String pathNetworkTopology = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        File f = new File(pathNetworkTopology);
                        setup.files.pipeNetwork = f;
                    } else if (line.contains("<RelativeNetworkTopology>")) {
                        String pathNetworkTopology = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        File f = new File(file.getParentFile(), pathNetworkTopology);
                        if (f.exists()) {
                            setup.files.pipeNetwork = f;
                        }
                    } else if (line.contains("<NetworkFlowField>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        File f = new File(path);
                        setup.files.pipeResult = f;
                    } else if (line.contains("<RelativeNetworkFlowField>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        File f = new File(file.getParentFile(), path);
                        if (f.exists()) {
                            setup.files.pipeResult = f;
                        }
                    } else if (line.contains("<SurfaceTopology>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        if (path.isEmpty() || path.equals("null")) {
                            setup.files.surfaceDirectory = null;
                        } else {
                            File f = new File(path);
                            setup.files.surfaceDirectory = f;
                        }
                    } else if (line.contains("<RelativeSurfaceTopology>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        if (!path.isEmpty() && (!path.equals("null"))) {
                            File f = new File(file.getParentFile(), path);
                            if (f.exists()) {
                                setup.files.surfaceDirectory = f;
                            }
                        }
                    } else if (line.contains("<SurfaceFlowField>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        if (path.isEmpty() || path.equals("null")) {
                            setup.files.surfaceResult = null;
                        } else {
                            File f = new File(path);
                            setup.files.surfaceResult = f;
                        }
                    } else if (line.contains("<RelativeSurfaceFlowField>")) {
                        String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        if (!path.isEmpty() && (!path.equals("null"))) {
                            File f = new File(file.getParentFile(), path);
                            if (f.exists()) {
                                setup.files.surfaceResult = f;
                            }
                        }
                    } else if (line.contains("<Sparse>")) {
                        Boolean sparse = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (networkRelation) {
                            setup.setSparsePipeVelocity(sparse);
                        }
                    } else if (line.contains("<CRS>")) {
                        String crs = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                        if (networkRelation) {
                            setup.files.setCrsPipes(crs);
                        }
                        if (surfaceRelation) {
                            setup.files.setCrsSurface(crs);
                        }
                    } else if (line.contains("<Network>")) {
                        networkRelation = true;
                        surfaceRelation = false;
                    } else if (line.contains("<Surface>")) {
                        networkRelation = false;
                        surfaceRelation = true;
                    }
                    if (line.contains("/InputFiles")) {
                        state = -1;
                    }
                } else if (state == 3) {
                    //Simulationparameters
                    if (line.contains("Timestep")) {
                        Double dt = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (dt > 0) {
                            setup.setTimestepTransport(dt);
                        }
                    } else if (line.contains("Timeintegration")) {
                        try {
                            String name = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            setup.setTimeIntegration(ParticleSurfaceComputing2D.TIMEINTEGRATION.valueOf(name));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (line.contains("EnterDry")) {
                        setup.setEnterDryCells(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("DryMovement")) {
                        setup.setDryMovement(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("SmoothZigZag")) {
                        setup.setSmoothZigZag(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("SlideAlongEdge")) {
                        setup.setSlideAlongEdge(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("StopVerySlow")) {
                        setup.setStopSlow(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("DryVelocity")) {
                        setup.setDryVelocity(Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    }
                    if (line.contains("/SimulationParameters")) {
                        state = -1;
                    }
                } else if (state == 4) {
                    //Materials
                    if (line.contains("/Materials")) {
                        setup.setMaterials(materials.values());
                        state = -1;
                        continue;
                    }
                    if (line.contains("/Material")) {
                        if (materialFlowCalculator == null) {
                            materialFlowCalculator = new Routing_Homogene();
//                            System.err.println("Created default " + materialFlowCalculator.getClass().getSimpleName() + " for material " + materialName + " (" + materialID + ")");
                        }
//                        if (materialDispersionCalculatorPipe == null) {
//                            materialDispersionCalculatorPipe = new Dispersion1D_Constant();
//                        }
//                        if (materialDispersionCalculatorSurface == null) {
//                            materialDispersionCalculatorSurface = new Dispersion2D_Constant();
//                        }

                        Material mat = new Material(materialName, 1000, materialID, materialFlowCalculator, materialDispersionCalculatorPipe, materialDispersionCalculatorSurface);
                        materials.put(mat.materialIndex, mat);
                        materialName = null;
                        materialFlowCalculator = null;
                        materialDispersionCalculatorPipe = null;
                        continue;
                    }

                    if (line.contains("<Surface")) {
                        surfaceRelation = true;
                        networkRelation = false;
                    } else if (line.contains("</Surface")) {
                        surfaceRelation = false;
                    } else if (line.contains("<Network")) {
                        surfaceRelation = false;
                        networkRelation = true;
                    } else if (line.contains("</Network")) {
                        networkRelation = false;
                    }

                    if (line.contains("<Material ")) {
                        materialID = Integer.parseInt(line.substring(line.indexOf("id=") + 3, line.indexOf(">")));
                    } else if (line.contains("<Name")) {
                        materialName = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    } else if (line.contains("<Dispersion")) {
                        if (networkRelation) {
                            line = br.readLine();
                            String type = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            int numparam = Integer.parseInt(line.substring(line.indexOf("Param") + 11, line.indexOf(">")));
                            double[] paramvalues = new double[numparam];
                            for (int i = 0; i < numparam; i++) {
                                line = br.readLine();
                                paramvalues[i] = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            }
                            if (type.equals("NONE")) {
                                materialDispersionCalculatorPipe = null;
                            } else {
                                try {
                                    materialDispersionCalculatorPipe = (Dispersion1D_Calculator) Class.forName(type).newInstance();
                                    materialDispersionCalculatorPipe.setParameterValues(paramvalues);
                                } catch (Exception ex) {
                                    Logger.getLogger(Setup_IO.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                        if (surfaceRelation) {
                            line = br.readLine();
                            String type = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            int numparam = Integer.parseInt(line.substring(line.indexOf("Param") + 11, line.indexOf(">")));
                            double[] paramvalues = new double[numparam];
                            for (int i = 0; i < numparam; i++) {
                                line = br.readLine();
                                paramvalues[i] = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            }
                            if (type.equals("NONE")) {
                                materialDispersionCalculatorSurface = null;
                            } else {
                                try {
                                    materialDispersionCalculatorSurface = (Dispersion2D_Calculator) Class.forName(type).newInstance();
                                    materialDispersionCalculatorSurface.setParameterValues(paramvalues);
                                } catch (Exception ex) {
                                    Logger.getLogger(Setup_IO.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                } else if (state == 5) {
                    //Injections
                    if (line.contains("</Injections>")) {
                        state = -1;
                        continue;
                    }

                    if (line.contains("<FromNetworkResult>")) {
                        setup.setLoadResultInjections(Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                    } else if (line.contains("<Injection ")) {
                        injectionID = Integer.parseInt(line.substring(line.indexOf("id=") + 3, line.indexOf(">")));
                        while (br.ready()) {
                            line = br.readLine();
                            if (line.contains("</Injection")) {
                                //Create Injection
                                InjectionInfo inj = null;
                                Material mat = materials.get(injection_materialID);
                                if (mat == null) {
                                    System.err.println("No material found for injection " + injectionID + ": " + injectionCapacityName + "   materials:" + materials.size());
                                } else {
                                    if (injectionType != null && injectionType.equals(InjectionInformation.class.getSimpleName())) {
                                        if (injectionOnSurface) {

                                            inj = new InjectionInformation(new GeoPosition(injectionLatitude, injectionLongitude), false, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);

                                        } else {
                                            if (injectionPosition != null) {
                                                inj = new InjectionInformation(injectionPosition, true, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else if (injectionCapacityName != null) {
                                                inj = new InjectionInformation(injectionCapacityName, 0, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else if (injectionCapacityID >= 0) {
                                                inj = new InjectionInformation(injectionCapacityID, true, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else if (!Double.isNaN(injectionLatitude)) {
                                                inj = new InjectionInformation(new GeoPosition(injectionLatitude, injectionLongitude), true, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else {
                                                System.err.println("No information about the Manhole to inject position:" + inj);
                                            }
                                        }
                                    } else if (injectionType != null && injectionType.equals(InjectionArealInformation.class.getSimpleName())) {
                                        InjectionArealInformation ainj = new InjectionArealInformation(mat, null, injectionMass, injectionParticles);
                                        ainj.setMass(injectionMass);
                                        ainj.setDuration(injectionDuration);
                                        inj = ainj;
                                    } else if (injectionType != null && injectionType.equals(InjectionSubArealInformation.class.getSimpleName())) {
                                        InjectionSubArealInformation ainj = new InjectionSubArealInformation(mat, null, injectionFilterString, injectionMass, injectionParticles);
                                        ainj.setDuration(injectionDuration);
                                        ainj.setMass(injectionMass);
                                        inj = ainj;
                                    } else if (injectionType != null && injectionType.equals(InjectionInflowInformation.class.getSimpleName())) {
                                        InjectionInflowInformation ainj = new InjectionInflowInformation(mat, null, injectionConcentration, injectionParticles);
                                        ainj.setDuration(injectionDuration);
                                        ainj.setMass(injectionMass);
                                        inj = ainj;
                                    } else if (injectionType != null && injectionType.equals(HEAreaInflow1DInformation.class.getSimpleName())) {
                                        HEAreaInflow1DInformation ainj = new HEAreaInflow1DInformation(injection_runoff, mat, injectionParticles);
                                        ainj.setActive(injectionActive);
                                        ainj.setWashoffConstant(injection_washoff);
                                        if (injection_washofftype != null) {
                                            ainj.setInflowtype(HEAreaInflow1DInformation.RUNOFF_CONTROL.valueOf(injection_washofftype));
                                        } else {
                                            ainj.setInflowtype(HEAreaInflow1DInformation.RUNOFF_CONTROL.INFLOW_WASHOFF);
                                            System.out.println("Washoff type not recognised. use standard " + ainj.inflowtype);

                                        }
                                        ainj.setSubstanceParameterName(injection_substance);
                                        ainj.setMassload(injection_load);
                                        inj = ainj;
                                    } else {
                                        if (injectionOnSurface) {

                                            if (injectionDiffusive) {
                                                inj = new InjectionArealInformation(mat, null, injectionMass, injectionParticles);//InjectionInformation.DIFFUSIVE_ON_SURFACE(injectionMass, injectionParticles, mat);
                                            } else {
                                                inj = new InjectionInformation(new GeoPosition(injectionLatitude, injectionLongitude), false, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            }
                                        } else {
                                            if (injectionPosition != null) {
                                                inj = new InjectionInformation(injectionPosition, true, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else if (injectionCapacityName != null) {
                                                inj = new InjectionInformation(injectionCapacityName, 0, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else if (injectionCapacityID >= 0) {
                                                inj = new InjectionInformation(injectionCapacityID, true, injectionMass, injectionParticles, mat, injectionStart, injectionDuration);
                                            } else {
                                                System.err.println("No information about the Manhole to inject");
                                            }
                                        }
                                    }
                                    if (inj != null) {
                                        inj.setActive(injectionActive);
                                        injections.add(inj);
//                                        System.out.println("Add "+inj);
                                    } else {
                                        System.err.println("Could not create Injection for " + injectionID + ": " + injectionCapacityName);
                                    }
                                }

                                injectionCapacityID = -1;
                                injectionCapacityName = null;
                                injectionDuration = 0;
                                injectionPosition = null;
                                injectionDiffusive = false;
                                injectionLatitude = Double.NaN;
                                injectionLongitude = Double.NaN;
                                break;
                            }

                            if (line.contains("<Type>")) {
                                injectionType = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            } else if (line.contains("<Diffusive>")) {
                                injectionDiffusive = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("<OnSurface>")) {
                                injectionOnSurface = Boolean.parseBoolean(line.substring(line.indexOf("<OnSurface") + 11, line.indexOf("</")));
                            } else if (line.contains("Latitude")) {
                                injectionLatitude = Double.parseDouble(line.substring(line.indexOf("Latitude") + 9, line.indexOf("</")));
                            } else if (line.contains("Longitude")) {
                                injectionLongitude = Double.parseDouble(line.substring(line.indexOf("Longitude") + 10, line.indexOf("</")));
                            } else if (line.contains("Start")) {
                                injectionStart = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Duration")) {
                                injectionDuration = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Mass")) {
                                injectionMass = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Concentration")) {
                                injectionConcentration = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Particles")) {
                                injectionParticles = Integer.parseInt(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Filter")) {
                                injectionFilterString = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            } else if (line.contains("Materi")) {
                                injection_materialID = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.indexOf(">")));
                            } else if (line.contains("Capacity")) {
                                if (line.contains("id=")) {
                                    injectionCapacityID = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.indexOf(">")));
                                } else {
                                    injectionCapacityID = -1;
                                }
                                injectionCapacityName = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                                if (injectionCapacityName != null && (injectionCapacityName.equals("null") || injectionCapacityName.isEmpty())) {
                                    injectionCapacityName = null;
                                }
                            } else if (line.contains("Active")) {
                                injectionActive = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Load")) {
                                injection_load = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Washoff ")) {
                                injection_washoff = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Washoff_type")) {
                                injection_washofftype = (line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            } else if (line.contains("Substance_Parameter")) {
                                injection_substance = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            } else if (line.contains("Runoff_Parameter")) {
                                injection_runoff = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                            }

                        }
                    }

                } else if (state == 6) {
                    //Measuring
                    if (line.contains("/Measuring")) {
                        state = -1;
                        continue;
                    }

                    if (line.contains("<Surface")) {
                        surfaceRelation = true;
                        networkRelation = false;
                        trace = false;
                    } else if (line.contains("</Surface")) {
                        surfaceRelation = false;
                    } else if (line.contains("<Network")) {
                        surfaceRelation = false;
                        networkRelation = true;
                        trace = false;
                    } else if (line.contains("</Network")) {
                        networkRelation = false;
                    } else if (line.contains("<Trace")) {
                        trace = true;
                        networkRelation = false;
                        surfaceRelation = false;
                    } else if (line.contains("</Trace")) {
                        trace = false;
                    }
                    /*<Interval unit='s'>60.0</>
			<Timecontinuous>true</>
			<Spatialconsistent>true</>
			<Synchronize Writing>false</>*/
                    if (line.contains("Interval")) {
                        double seconds = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (surfaceRelation) {
                            setup.setSurfaceMeasurementtimestep(seconds);
                        }
                        if (networkRelation) {
                            setup.setPipeMeasurementtimestep(seconds);
                        }
                        if (trace) {
                            setup.setIntervalTraceParticles(Integer.parseInt(line.substring(line.indexOf(">") + 1, line.indexOf("</"))));
                        }
                    } else if (line.contains("Timecontinuous")) {
                        boolean timeconti = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (networkRelation) {
                            setup.setPipeMeasurementTimeContinuous(timeconti);
                        }
                        if (surfaceRelation) {
                            setup.setSurfaceMeasurementTimeContinuous(timeconti);
                        }
                    } else if (line.contains("Spatialconsistent")) {
                        boolean spatialContio = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (networkRelation) {
                            setup.setPipeMeasurementSpatialConsistent(spatialContio);
                        }
                        if (surfaceRelation) {
                            setup.setSurfaceMeasurementSpatialConsistent(spatialContio);
                        }
                    } else if (line.contains("Synchronize")) {
                        boolean sync = Boolean.parseBoolean(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                        if (networkRelation) {
                            setup.setPipeMeasurementSynchronize(sync);
                        }
                        if (surfaceRelation) {
                            setup.setSurfaceMeasurementSynchronize(sync);
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        br.close();

        fr.close();

        setup.setInjections(injections);
        return setup;
    }

}
