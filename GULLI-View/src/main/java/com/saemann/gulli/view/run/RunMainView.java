package com.saemann.gulli.view.run;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.LoadingCoordinator;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.StoringCoordinator;
import com.saemann.gulli.core.control.listener.LoadingActionAdapter;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.output.ContaminationParticles;
import com.saemann.gulli.core.control.output.ContaminationShape;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.GeoPosition;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.view.ViewController;
import com.saemann.rgis.tileloader.source.MyOSMTileSource;
import com.saemann.rgis.view.SimpleMapViewerFrame;

/**
 * The Run Class to start the GUI with a Simulation. After the run, shapefiles
 * with the contaminated area are created inside the input folder.
 * Generic_Urban_transport_for
 * pollution_with_Lagrangian_aspect_for_Location_tracIng
 *
 *
 *
 * @author saemann
 */
public class RunMainView {

    Controller control;

    public RunMainView() throws Exception {
        this.control = new Controller();
    }

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
        //ViewController links the Controller to the GUI
        final ViewController vcontroller = new ViewController(control);
        //The Main Frame containing the Map.
        final SimpleMapViewerFrame frame = vcontroller.getMapFrame();

        //Zeitschrittweite für Partikalbewegung setzen (Sekunden)
        //Set Deltatime for the particlesimulation (in seconds)
        control.getThreadController().setDeltaTime(1);
//        ThreadController.pauseRevokerThread = true;

        //Benutzeroberfläche Hintergrundkarte anpassen.
        //Set the Background map for the main frame.
        vcontroller.getMapViewer().setBaseLayer(MyOSMTileSource.BaseLayer.CARTO_LIGHT.getSource());
        vcontroller.getMapViewer().recomputeCopyright();

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

        if (startFile != null && startFile.exists()) {
            if (startFile.getName().endsWith("xml")) {
                //This seems to be a project definition xml file
                System.out.println("load project XML " + startFile);
                lc.loadSetup(startFile);
            } else {
                System.out.println("load event result " + startFile);
                //Try to crawl all dependent files from the information stored in the He result file.
                lc.requestDependentFiles(startFile, true, true);
            }
        } else {
            System.out.println("startfile does not exist");
        }

        //Otherwise set all the input files manually
//                lc.setPipeNetworkFile(NETWORKFILE);
//                lc.setPipeResultsFile(NETWORKVELOCITIES);
//                lc.setSurfaceTopologyDirectory(SURFACE DIRECTORY);
//                lc.setSurfaceFlowfieldFile(SURFACEWATERLEVELANDVELOCITY);
        //Define here, if the samples of different Threads should wait for eachother (true: slow) (false: faster but some samples might be overwritten)
//        SurfaceMeasurementRaster.synchronizeMeasures = true;
//        ArrayTimeLineMeasurement.synchronizeMeasures = true;
//Loading finisher sorgt dafür, dass nach erfolgreichem Ladevorgang der Input Dateien automatisch ein Simulatiomnslauf gestartet wird.
        ParticleSurfaceComputing2D.gradientFlowForDryCells = true;
        if (false) {
            lc.loadingFinishedListener.add(new Runnable() {
                @Override
                public void run() {

                    try {
                        if (control.getScenario() != null && control.getScenario().getMeasurementsPipe() == null) {
                            control.initMeasurementTimelines(control.getScenario());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Automatic start after loading loop has finished.   
                    if (true) {
                        //Test
                        if (true) {
                            //~90sekunden
                            ParticleSurfaceComputing2D.allowWashToPipesystem = true;
                            ParticleSurfaceComputing2D.gradientFlowForDryCells = false;
                            ParticlePipeComputing.spillOutToSurface = true;

                            control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();

                            if (control.getSurface() != null) {
                                control.getScenario().getMeasurementsSurface().continousMeasurements = true;
                                System.out.println("Changed sampling to simgel sample at end of interval");

                                //3 injections scenario 
                                if (control.getNetwork() == null) {
                                    System.err.println("There is no Pipe network for the simulation.");
                                    return;
                                }
                                int anzahl = 100_000 / 3;
                                Manhole mh = control.getNetwork().getManholeByName("RI09S515");
                                if (mh != null) {
                                    System.out.println("add 3 Injection at " + mh);
                                    try {
                                        lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl, new Material("K_1_" + anzahl, 1000, true, 0), 1 * 60, 0));
                                        lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl, new Material("K_2_" + anzahl, 1000, true, 1), 5 * 60, 0));
                                        lc.addManualInjection(new InjectionInformation(mh, 0, 1000, anzahl + 1, new Material("K_3_" + anzahl, 1000, true, 2), 10 * 60, 0));
                                    } catch (NullPointerException nullPointerException) {
                                        System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                        if (false) {
                            //~30 sek
                            //Viktor Paper scenario 
                            Manhole mh = control.getNetwork().getManholeByName("RI05S516");
                            int anzahl = 100000;
                            mh = control.getNetwork().getManholeByName("MU08S561");
                            System.out.println("add 1 Injection at " + mh);
                            try {
                                lc.addManualInjection(new InjectionInformation(new GeoPosition(52.341954, 9.697130), false, 10, anzahl, new Material("Viktor_+20m_" + anzahl, 1000, true, 0), 20 * 60));
//                            lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Viktor_1_" + anzahl, 1000, true, 0), 1 * 60, 0));

                            } catch (NullPointerException nullPointerException) {
                                System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                            }
                        }

                        //Sensitivity Paper 
                        if (false) {
                            //Paper scenario 
                            int anzahl = 3000;
                            Manhole mh = control.getNetwork().getManholeByName("MU04S503");
                            System.out.println(getClass() + ". add 3 Injections");
                            try {
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+0", 1000, true, 0), 0, 7 * 60));
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+10", 1000, true, 1), 9 * 60, 7 * 60));
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+20", 1000, true, 2), 19 * 60, 7 * 60));

                            } catch (NullPointerException nullPointerException) {
                                System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                            }
                        }

                        //Buchkapitel
                        if (false) {
                            //Paper scenario 
                            int anzahl = 100000 / 3;
                            Manhole mh = control.getNetwork().getManholeByName("RI09S515");
                            System.out.println("add 3 Injection at " + mh);
                            try {
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+0", 1000, true, 0), 0 * 60, 1));
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+15", 1000, true, 1), 15 * 60, 1));
                                lc.addManualInjection(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+30", 1000, true, 2), 30 * 60, 1));

                            } catch (NullPointerException nullPointerException) {
                                System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                            }
                        }
                    }

                    //Zoom Window to Area of Pipes
                    if (frame != null) {
                        if (frame.getMapViewer().getZoom() < 15) {
//                        System.out.println();
                            frame.getMapViewer().zoomToFitLayer();
                        }
                    }

                    control.recalculateInjections();
                    control.resetScenario();

//                    if (StartParameters.isAutoStartatStartup()) {
//                        System.out.println("Autostart simulation");
//                        if (control.getScenario() != null) {
//                            // Start the simulation.
//                            control.start();
//                            final Runnable r = this;
//
//                            new Thread("Loading finished listener remover") {
//                                @Override
//                                public void run() {
//                                    try {
//                                        //Wait some time to enable a repaint of GUI.
//                                        //Otherwise some drawing Exceptions occure.
//                                        sleep(2000);
//                                    } catch (InterruptedException ex) {
//                                        Logger.getLogger(RunMainView.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
//                                    lc.loadingFinishedListener.remove(r);
//                                }
//                            }.start();
//                        }
//                    }

                }
            });
        }

        if (StartParameters.isAutoStartatStartup()) {
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                        control.recalculateInjections();
                        control.start();
                        Runnable runner=this;
                        new Thread("Loading finished listener remover") {
                                @Override
                            public void run() {
                                try {
                                    //Wait some time to enable a repaint of GUI.
                                    //Otherwise some drawing Exceptions occure.
                                    sleep(2000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(RunMainView.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                lc.loadingFinishedListener.remove(runner);
                            }
                        }.start();
                    
                }
            };
            lc.loadingFinishedListener.add(r);
        }

        control.getStoringCoordinator().addFinalOuput(new ContaminationShape(StoringCoordinator.FileFormat.GeoPKG, -1, true));
        StoringCoordinator.verbose = true;
        control.getStoringCoordinator().addFinalOuput(new ContaminationShape(StoringCoordinator.FileFormat.GeoJSON, -1, true));
        control.getStoringCoordinator().addFinalOuput(new ContaminationParticles());

        //Start loading the set files. 
        if (lc.getFileNetwork() != null || lc.getFilePipeResultIDBF() != null || lc.getFileSurfaceTriangleIndicesDAT() != null) {
            if (StartParameters.isAutoLoadatStartup()) {
                lc.startLoadingRequestedFiles(true);
            }
        }

    }
}
