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
package run;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.LoadingCoordinator;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.StoringCoordinator;
import com.saemann.gulli.core.control.listener.SimulationActionAdapter;
import com.saemann.gulli.core.control.output.ContaminationParticles;
import com.saemann.gulli.core.control.output.ContaminationShape;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.topology.Manhole;
import java.io.File;

/**
 *
 * @author saemann
 */
public class TestRun {

    /**
     * Programm wird mit dieser Mainmethode gestartet. Start der
     * Benutzeroberfläche zum Starten einer Partikelausbreitungssimulation.
     *
     * @param args No effect
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //Der Controller koordiniert alle einzelnen Module und startet die Benutzeroberfläche.
        //Control links all model and io components and stores the mesh and all simulation-related information
        final Controller control = new Controller();

        //Zeitschrittweite für Partikalbewegung setzen (Sekunden)
        //Set Deltatime for the particlesimulation (in seconds)
        control.getThreadController().setDeltaTime(1);

        //////////
        // Loading Simulation input files.
        //   they are coordinated by the LoadingCooordinator, that can search for related and consistent files.
        final LoadingCoordinator lc = control.getLoadingCoordinator();

        //If set to yes, injection spills from the input scenario (e.g. HYSTEM EXTRAN Schmtzfracht eInzeleinleiter) is listed as contamination source.
        lc.loadResultInjections = true;
        //Automatisches Suchen und einlesen der Inputfiles, die mit dem HE-Result verknüpft sind.
        //Start file can be set in the GULLI.ini after first start in the main folder.
        // Give the path to the HYSTEM EXTRAN RESULT FILE under the Key "StartFile=".
        File startFile = new File(StartParameters.getStartFilePath());

        if (startFile == null || !startFile.exists()) {
            //Fallback, if nothing was set in the GULLi.ini
//        startFile = new File("F:\\GULLI_Input\\Modell2018_HE81\\E2D1T50_mehrFlaechen.result\\Ergebnis.idbr");
        }
        if (startFile.exists()) {
            //Try to crawl all dependent files from the information stored in the He result file.
            lc.requestDependentFiles(startFile, true, true);
        } else {
            System.out.println("startfile does not exist");
        }

        //Otherwise set all the input files manually
//                lc.setPipeNetworkFile(NETWORKFILE);
//                lc.setPipeResultsFile(NETWORKVELOCITIES);
//                lc.setSurfaceTopologyDirectory(SURFACE DIRECTORY);
//                lc.setSurfaceFlowfieldFile(SURFACEWATERLEVELANDVELOCITY);
//Loading finisher sorgt dafür, dass nach erfolgreichem Ladevorgang der Input Dateien automatisch ein Simulatiomnslauf gestartet wird.
        //Load the topography files to prepare the simulation
        System.out.println("Loading the domain & pipe system geometries.");
        lc.startLoadingRequestedFiles(false);

        System.out.println("Geometry of the simulation domain loaded.");

        try {
            if (control.getScenario() != null && control.getScenario().getMeasurementsPipe() == null) {
                control.initMeasurementTimelines(control.getScenario());
            }
            if (control.getNetwork() == null) {
                System.err.println("There is no Pipe network for the simulation.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Define sampling parameters
        //1) Pipe system sampling
        control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();
        System.out.println("Changed sampling to simgel sample at end of interval");
        //2) Surface sampling
        ParticleSurfaceComputing2D.gradientFlowForDryCells = false;
        SurfaceMeasurementRaster.synchronizeMeasures = true;
        control.getScenario().getMeasurementsSurface().continousMeasurements = false;//false: Only take samples at the end of a sample interval (take only snapshots)

        //Define injections for this scenario 
        int anzahl = 100_000 / 3;
        // Spreading distributed equal all over the domain
        InjectionInformation diffusiveSurfacePollution = InjectionInformation.DIFFUSIVE_ON_SURFACE(1000, 100_000, new Material("Dust", 1000, true, 0));
        lc.addManualInjection(diffusiveSurfacePollution);

        if (false) {
            Manhole mh = control.getNetwork().getManholeByName("RI09S515");
            if (mh != null) {
                System.out.println("add 3 Injection at " + mh);
                try {
                    lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl, new Material("K_1_" + anzahl, 1000, true, 1), 1 * 60, 0));
                    lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl, new Material("K_2_" + anzahl, 1000, true, 2), 5 * 60, 0));
                    lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl + 1, new Material("K_3_" + anzahl, 1000, true, 3), 10 * 60, 0));
                } catch (NullPointerException nullPointerException) {
                    System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                }
            }
        }

        control.recalculateInjections();
        control.resetScenario();

        //Define the output files that are written after the simulation has finished.
        control.getStoringCoordinator().addFinalOuput(new ContaminationShape(StoringCoordinator.FileFormat.GeoPKG, 1, true));
        control.getStoringCoordinator().addFinalOuput(new ContaminationShape(StoringCoordinator.FileFormat.GeoJSON, 1, true));
        control.getStoringCoordinator().addFinalOuput(new ContaminationParticles());
        StoringCoordinator.verbose = true; // inform us after each file has been written.

        // Create an ouput, that writes every 10% progress to the console.
        control.addSimulationListener(new SimulationActionAdapter() {
            int last10percent = -1;
            long duration = control.getScenario().getEndtime() - control.getScenario().getStartTime();

            @Override
            public void simulationSTEPFINISH(long loop, Object caller) {
                int per = (int) ((10 * ThreadController.getSimulationTimeMS()) / (duration));
                if (per > last10percent) {
                    last10percent = per;
                    System.out.println("  " + (per * 10) + "%");
                }
            }
        });

        //Start the simulation calculation
        System.out.println("Start simulation with "+control.getThreadController().getNumberOfTotalParticles()+" particles.");
        control.start();
    }
}
