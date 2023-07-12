package com.saemann.gulli.view.run;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.LoadingCoordinator;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.SimulationActionAdapter;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.view.ViewController;
import com.saemann.rgis.tileloader.source.MyOSMTileSource;
import com.saemann.rgis.tileloader.source.OSMTileSource;
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

        control.intervallHistoryParticles = 10;
        
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
        
        if(vcontroller.getMapViewer().getTileSource() instanceof OSMTileSource){
            vcontroller.getMapViewer().setMaxZoom(30);
            System.out.println("Maxzoom set to 30");
        }

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

        if (StartParameters.isAutoStartatStartup()) {
            final Runnable r = new Runnable() {
                @Override
                public void run() {


                    control.recalculateInjections();
                    control.start();
                    Runnable runner = this;
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

        //Start loading the set files. 
        if (lc.getFileNetwork() != null || lc.getFilePipeFlowfield() != null || lc.getFileSurfaceTriangleIndicesDAT() != null) {
            if (StartParameters.isAutoLoadatStartup()) {
                lc.startLoadingRequestedFiles(true);
            }
        }

        //Add some additional output at the end of a Simulation
        control.addSimulationListener(new SimulationActionAdapter() {
            @Override
            public void simulationFINISH(boolean timeOut, boolean particlesOut) {
                //Inform about the Time that it took to load information from the Database
                try {
//                    System.out.println(control.getThreadController().reportLoadingTimes());
                    //Disabled debugging information. Can be enabled for information about stuck particles on the surface
                    if (true) {
//                        System.out.println(control.getThreadController().reportTravelStatistics());                        
                    }
//                    System.out.println("-----");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
