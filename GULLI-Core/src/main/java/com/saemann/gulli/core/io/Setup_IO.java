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

import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Calculator;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Constant;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.Setup;
import com.saemann.gulli.core.control.scenario.SpillScenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.model.material.Material;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Load and save definitions of scenarios
 *
 * @author Robert Sämann
 */
public class Setup_IO {

    public static boolean saveScenario(File file, Setup setup) throws IOException {

        Scenario scenario = setup.scenario;
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
            bw.write("\t\t<NetworkFlowField>" + files.getPipeResult() + "</>");
            bw.newLine();
            bw.write("\t</Network>");
            bw.newLine();
            bw.write("\t<Surface>");
            bw.newLine();
            bw.write("\t\t<SurfaceTopology>" + files.getSurfaceDirectory() + "</>");
            bw.newLine();
            bw.write("\t\t<SurfaceFlowField>" + files.getSurfaceResult() + "</>");
            bw.newLine();
            bw.write("\t</Surface>");
            bw.newLine();
        }
        bw.write("</InputFiles>");
        bw.newLine();
        bw.write("<SimulationParameters>");
        bw.newLine();
        bw.write("\t<Timestep unit='s'>" + setup.getTimestepTransport() + "</>");
        bw.newLine();
        bw.write("\t<Dispersion>");
        bw.newLine();
        bw.write("\t\t<Network>");
        bw.newLine();
        bw.write("\t\t\t<Dispersion unit='m^2/s'>" + setup.getNetworkdispersion() + "</>");
        bw.newLine();
        bw.write("\t\t</Network>");
        bw.newLine();
        bw.write("\t\t<Surface>");
        bw.newLine();
        if (setup.getSurfaceDiffusion() != null) {
            Dispersion2D_Calculator disp = setup.getSurfaceDiffusion();
            bw.write("\t\t\t<Class>" + disp.getClass().getName() + "</>");
            bw.newLine();
            bw.write("\t\t\t<Type>" + disp.getDiffusionString() + "</>");
            bw.newLine();
            for (int i = 0; i < disp.getParameterOrderDescription().length; i++) {              
                bw.write("\t\t\t<"+disp.getParameterOrderDescription()[i]+">" + disp.getParameterValues()[i] + "</>");
                bw.newLine();
            }
            bw.newLine();
            bw.write("\t\t\t<DryFlow unit='m/s'>" + setup.getRoutingSurfaceDryflowVelocity() + "</>");
            bw.newLine();
            bw.write("\t\t\t<EnterDry>" + setup.isRoutingSurfaceEnterDryCells() + "</>");
            bw.newLine();
        }
        bw.write("\t\t</Surface>");
        bw.newLine();
        bw.write("\t</Dispersion>");
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
        bw.write("\t\t\t<Synchronize Writing>" + setup.isSurfaceMeasurementSynchronize() + "</>");
        bw.newLine();
        bw.write("\t\t</Surface>");
        bw.newLine();
        bw.write("</Measuring>");
        bw.newLine();
        bw.write("<Materials>");
        bw.newLine();
        for (int i = 0; i <= scenario.getMaxMaterialID(); i++) {
            Material m = scenario.getMaterialByIndex(i);
            if (m != null) {
                bw.write("\t<Material id=" + i + ">");
                bw.newLine();
                bw.write("\t\t<Name>" + m.getName() + "</>");
                bw.newLine();
                bw.write("\t\t<Density unit='kg/m^3'>" + m.getDensity() + "</>");
                bw.newLine();
                bw.write("\t\t<Flowcalculator>" + m.getFlowCalculator().getClass().getName() + "</>");
                bw.newLine();
                bw.write("\t</Material>");
                bw.newLine();
            }
        }
        bw.write("</Materials>");
        bw.newLine();
        bw.write("<Injections>");
        bw.newLine();
        bw.write("\t\t<FromNetworkResult>" + setup.isLoadResultInjections() + "</>");
        bw.newLine();
        for (InjectionInformation injection : scenario.getInjections()) {
            bw.write("\t<Injection id=" + injection.getId() + ">");
            bw.newLine();

            bw.write("\t\t<OnSurface>" + injection.spillOnSurface() + "</>");
            bw.newLine();

            if (injection.spillOnSurface()) {
                if (injection.getTriangleID() >= 0) {
                    bw.write("\t\t<Surfacecell>" + injection.getTriangleID() + "</>");
                    bw.newLine();
                }
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
            bw.write("\t\t<Start unit='s'>" + injection.getStarttimeSimulationsAfterSimulationStart() + "</>");
            bw.newLine();
            bw.write("\t\t<Duration unit='s'>" + injection.getDurationSeconds() + "</>");
            bw.newLine();
            bw.write("\t\t<Material id=" + injection.getMaterial().materialIndex + ">" + injection.getMaterial().getName() + "</>");
            bw.newLine();

            bw.write("\t</Injection>");
            bw.newLine();
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
        ArrayList<InjectionInformation> injections = new ArrayList<>();
        SpillScenario scenario = new SpillScenario(null, injections);
        setup.scenario = scenario;
        int state = -1;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        boolean networkRelation = false, surfaceRelation = false;

        String line = "";
        while (br.ready()) {
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
                }
            }
            if (state == 1) {
                if (line.contains("<Name")) {
                    String name = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    scenario.setName(name);
                    state = -1;
                }
            }
            if (state == 2) {
                //InputFiles
                if (line.contains("<NetworkTopology>")) {
                    String pathNetworkTopology = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    File f = new File(pathNetworkTopology);
                    setup.files.pipeNetwork = f;
                } else if (line.contains("<NetworkFlowField>")) {
                    String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    File f = new File(path);
                    setup.files.pipeResult = f;
                } else if (line.contains("<SurfaceTopology>")) {
                    String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    File f = new File(path);
                    setup.files.surfaceDirectory = f;
                } else if (line.contains("<SurfaceFlowField>")) {
                    String path = line.substring(line.indexOf(">") + 1, line.indexOf("</"));
                    File f = new File(path);
                    setup.files.surfaceResult = f;
                }
                if (line.contains("/InputFiles")) {
                    state = -1;
                }
            }
            if (state == 3) {
                //Simulationparameters
                if (line.contains("Timestep")) {
                    Double dt = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                    if (dt > 0) {
                        setup.setTimestepTransport(dt);
                    }
                }
                if (line.contains("<Network")) {
                    networkRelation = true;
                    surfaceRelation = false;
                } else if (line.contains("<Surface")) {
                    networkRelation = false;
                    surfaceRelation = true;
                }
                if (line.contains("<Dispersion")) {
                    if (line.contains("</")) {
                        try {
                            double d = Double.parseDouble(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                            if (networkRelation) {
                                setup.setNetworkdispersion(d);
                            }
                            if (surfaceRelation) {
                                Dispersion2D_Constant dc=new Dispersion2D_Constant();
                                dc=new Dispersion2D_Constant();
                                dc.Dxx=d;
                                dc.Dyy=d;
                                dc.D=new double[]{dc.Dxx,dc.Dyy,dc.Dyy};
                            
                                setup.setSurfaceDiffusion(dc);
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }

                if (line.contains("/SimulationParameters")) {
                    state = -1;
                }
            }
        }

        br.close();
        fr.close();
        return setup;
    }

}
