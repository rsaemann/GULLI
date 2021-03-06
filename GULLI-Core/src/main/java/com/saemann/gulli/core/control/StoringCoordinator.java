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
package com.saemann.gulli.core.control;

import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.output.OutputIntention;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;
import java.io.File;
import java.util.ArrayList;

/**
 * Managaes the user defined outputs. Keeps track of the right time to create
 * output files
 *
 * @author saemann
 */
public class StoringCoordinator implements SimulationActionListener {

    public static boolean verbose = false;

    public enum FileFormat {
        CSV, SHP, GeoPKG, GeoJSON
    }

    private Controller control;

    private File fileRoot;

    /**
     * interbal flag to prevent setting of parameters while this controller is
     * still writing to files.
     */
    private boolean writing = false;

    /**
     * List of user defined outputs that should be processed, after the
     * simulation has finished.
     */
    private ArrayList<OutputIntention> finalOutputs = new ArrayList<>(3);

    private ArrayList<Thread> writerThreads = new ArrayList<>(3);

    public StoringCoordinator(Controller control) {
        this.control = control;
    }

    @Override

    public void simulationINIT(Object caller) {
    }

    @Override
    public void simulationSTART(Object caller) {
        if (writing) {
            System.err.println("New Simulation started, but not all values of the old simulation are written to the filesystem. This might cause corrupt files.");
        }
        if (control != null && control.getLoadingCoordinator().getFilePipeFlowfield() != null) {
            if (!control.getLoadingCoordinator().getFilePipeFlowfield().getParentFile().equals(fileRoot)) {
                fileRoot = control.getLoadingCoordinator().getFilePipeFlowfield().getParentFile();
                System.out.println("set output directory to " + fileRoot.getAbsolutePath());
            }
        }
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
    }

    @Override
    public void simulationPAUSED(Object caller) {
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
    }

    @Override
    public void simulationSTOP(Object caller) {
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
        writeFinalOutputs();
    }

    @Override
    public void simulationRESET(Object caller) {
    }

    public File getFileRoot() {
        return fileRoot;
    }

    public Material getMaterial(int materialIndex) {
        return control.getScenario().getMaterialByIndex(materialIndex);
    }

    public Surface getSurface() {
        return control.getSurface();
    }

    public Network getNetwork() {
        return control.getNetwork();
    }
    
    public Particle[] getParticles(){
        return control.getThreadController().getParticles();
    }

    private void writeFinalOutputs() {
        writing = true;
        for (OutputIntention fo : finalOutputs) {
            try {
                final Thread th = new Thread("Write " + fo.toString()) {
                    @Override
                    public void run() {
                        File f = fo.writeOutput(StoringCoordinator.this);
                        if (f != null && verbose) {
                            System.out.println("Output written to " + f.getAbsolutePath());
                        }
//                        StoringCoordinator.this.writerThreads.remove(th);
                    }

                };
                th.start();
                writerThreads.add(th);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        writing = false;
    }

    public ArrayList<OutputIntention> getFinalOutputs() {
        return finalOutputs;
    }

    public boolean addFinalOuput(OutputIntention output) {
        if (!finalOutputs.contains(output)) {
            return finalOutputs.add(output);
        }
        return false;
    }

}
