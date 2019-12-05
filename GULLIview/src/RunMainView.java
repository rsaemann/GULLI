
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import control.Controller;
import control.LoadingCoordinator;
import control.ShapeTools;
import control.StartParameters;
import control.listener.SimulationActionAdapter;
import control.particlecontrol.ParticlePipeComputing;
import control.particlecontrol.ParticleSurfaceComputing2D;
import control.scenario.injection.InjectionInformation;
import io.GeoJSON_IO;
import io.SHP_IO_GULLI;
import io.extran.HE_SurfaceIO;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import model.GeoPosition;
import model.particle.Material;
import model.topology.Manhole;
import org.jfree.ui.action.ActionMenuItem;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.gui.jmapviewer.source.MyOSMTileSource;
import view.MapViewer;
import view.SimpleMapViewerFrame;
import view.ViewController;

/**
 * The Run Class to start the GUI with a Simulation. After the run, shapefiles
 * with the contaminated area are created inside the input folder.
 * Generic_unsteady_lagrangian_locatIng
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
//        try {
//            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
//            UIManager.getSystemLookAndFeelClassName());
//        }

//            
        //Der Controller koordiniert alle einzelnen Module und startet die Benutzeroberfläche.
        //Control links all model and io components and stores the mesh and all simulation-related information
        final Controller control = new Controller();
        //ViewController links the Controller to the GUI
        ViewController vcontroller = new ViewController(control);
        //The Main Frame containing the Map.
        final SimpleMapViewerFrame frame = vcontroller.getMapFrame();

        //Zeitschrittweite für Partikalbewegung setzen (Sekunden)
        //Set Deltatime for the particlesimulation (in seconds)
        control.getThreadController().setDeltaTime(1);

        //Benutzeroberfläche Hintergrundkarte anpassen.
        //Set the Background map for the main frame.
        MapViewer.verboseExceptions = true;
        vcontroller.getMapViewer().setBaseLayer(MyOSMTileSource.BaseLayer.CARTO_LIGHT.getSource());
        vcontroller.getMapViewer().recomputeCopyright();

        //////////
        // Loading Simulation input files.
        //   they are coordinated by the LoadingCooordinator, that can search for related and consistent files.
        final LoadingCoordinator lc = control.getLoadingCoordinator();

        //If set to yes, injection spills from the input scenario (e.g. HYSTEM EXTRAN Schmtzfracht eInzeleinleiter) is listed as contamination source.
        lc.loadInputInjections = true;
        //Automatisches Suchen und einlesen der Inputfiles, die mit dem HE-Result verknüpft sind.
        //Start file can be set in the GULLI.ini after first start in the main folder.
        // Give the path to the HYSTEM EXTRAN RESULT FILE under the Key "StartFile=".
        File startFile = new File(StartParameters.getStartFilePath());

//        if (startFile == null || !startFile.exists()) {
        //Fallback, if nothing was set in the GULLi.ini
//            startFile = new File("L:\\GULLI_Input\\Modell2017Mai\\2D_Model\\Extr2D_E2D1T50_mBK.result\\Ergebnis.idbf");
//        startFile = new File("L:\\GULLI_Input\\Modell2017Mai\\2D_Model\\Model-Ex_E2DiT50_Schadstoff_EXT.idbr");
//        startFile = new File("L:\\ViktorPaper\\Ricklingen_Referenzloesung_Mai18\\Petristrasse_Obs_79_noGW.result\\Ergebnis.idbf");
        startFile = new File("C:\\Users\\saemann\\Documents\\Hystem-Extran 8.1\\Hystem-Extran\\he81-Beispiel-Schmutzfracht-ungleichmäßig-E.idbr");
//        startFile = new File("Y:\\EVUS\\Modelle\\Hannover\\EVUS_Hannover_gesamt2DAB\\HE2D_20190620_RobertDach.result\\Ergebnis.idbr");
//        startFile = new File("L:\\Model-Extr_BLD3V1,2_EXT.idbr");
//        startFile = new File("L:\\Model-Extr_BLD3V1,2_EXT.idbr");
//         startFile = new File("F:\\muster-modelldatenbank_janina-Janina_EXT.idbr");
//        }
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
//                lc.setSurfaceWaterlevelFile(SURFACEWATERLEVELANDVELOCITY);
        //Loading finisher sorgt dafür, dass nach erfolgreichem Ladevorgang der Input Dateien automatisch ein Simulatiomnslauf gestartet wird.
        lc.loadingFinishedListener.add(new Runnable() {
            @Override
            public void run() {
                try {
                    control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                control.getSurface().setMeasurementRaster(new SurfaceMeasurementTriangleRaster(control.getSurface(), 3, null, control.getThreadController().getParticleThreads().length));
//                control.getSurface().setMeasurementRaster(SurfaceMeasurementRectangleRaster.SurfaceMeasurementRectangleRaster(control.getSurface(), 40, 40, 3));
                //Automatic start after loading loop has finished.   
                if (true) {
                    //Test
                    if (true) {
                        //~90sekunden
                        ParticleSurfaceComputing2D.allowWashToPipesystem = true;
                        ParticlePipeComputing.spillOutToSurface = true;

                        //3 injections scenario 
                        int anzahl = 100000 / 3;
                        Manhole mh = control.getNetwork().getManholeByName("RI09S515");
                        System.out.println("add 3 Injection at " + mh);
                        try {
//                            lc.addInjectionInformation(new InjectionInformation(226429, 10, 100000, new Material("K_1_" + anzahl, 1000, true, 0), 1 * 60, 0));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("K_1_" + anzahl, 1000, true, 0), 1 * 60, 0));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("K_2_" + anzahl, 1000, true, 1), 5 * 60, 0));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("K_3_" + anzahl, 1000, true, 2), 10 * 60, 0));
//                            for (InjectionInformation injection : lc.getInjections()) {
//                                injection.setSpillPipesystem(false);
//                            }
                        } catch (NullPointerException nullPointerException) {
                            System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                        }
                    }

                    if (false) {
                        //~30 sek
                        //Viktor Paper scenario 
                        Manhole mh = control.getNetwork().getManholeByName("RI05S516");
                        try {
                            Coordinate pos = control.getSurface().getGeotools().toUTM(mh.getPosition().lonLatCoordinate(), true);
//                            control.getSurface().setMeasurementRaster(SurfaceMeasurementRectangleRaster.RasterFocusOnPoint(pos.x, pos.y, true, 10, 10, 60, 50, 1, control.getSurface().getTimes()));
//SurfaceMeasurementRectangleRaster.SurfaceMeasurementRectangleRaster(control.getSurface(), 100, 100));

                        } catch (TransformException ex) {
                            Logger.getLogger(RunMainView.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        int anzahl = 100000;
                        mh = control.getNetwork().getManholeByName("MU08S561");
                        System.out.println("add 1 Injection at " + mh);
                        try {
                            lc.addInjectionInformation(new InjectionInformation(new GeoPosition(52.341954, 9.697130), false, 10, anzahl, new Material("Viktor_+20m_" + anzahl, 1000, true, 0), 20 * 60));
//                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Viktor_1_" + anzahl, 1000, true, 0), 1 * 60, 0));

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
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+0", 1000, true, 0), 0, 7 * 60));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+10", 1000, true, 1), 9 * 60, 7 * 60));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+20", 1000, true, 2), 19 * 60, 7 * 60));

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
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+0", 1000, true, 0), 0 * 60, 1));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+15", 1000, true, 1), 15 * 60, 1));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Buch_" + anzahl + "+30", 1000, true, 2), 30 * 60, 1));

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

                if (control.getScenario() != null) {
                    // Start the simulation.
                    control.start();
                }

                final Runnable r = this;

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
                        lc.loadingFinishedListener.remove(r);
                    }
                }.start();
            }
        });

        //Start loading the set files. 
        lc.startLoadingRequestedFiles(true);

        //Write results after finishing
        //This function is called after a simulation has finished.
        //  here the result shapefiles (shp & geojson) are created.
        control.getThreadController().addSimulationListener(new SimulationActionAdapter() {
            @Override
            public void simulationFINISH(boolean timeOut, boolean particlesOut
            ) {
                try {
                    //Create shapefiles
                    new Thread("Shapefile creator after simulation") {

                        @Override
                        public void run() {
                            try {
                                if (control.getSurface() == null) {
                                    return;
                                }
                                for (int m = -1; m < control.getSurface().getNumberOfMaterials(); m++) {

                                    int materialindex = m; //<0 = all
                                    ArrayList<Geometry> shapes = null;
                                    try {
                                        //Create surrounding shape of contaminated areas.
                                        //Only show a triangle as contaminated, if there area at least 20 counts of particles a triangle.
                                        shapes = ShapeTools.createShapesWGS84(control.getSurface(), 1, 1, materialindex, 20);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                        continue;
                                    }
                                    if (shapes == null || shapes.isEmpty()) {
                                        System.out.println("No Shape files created.");
                                        continue;
                                    }
                                    String parent = control.getLoadingCoordinator().getFilePipeResultIDBF().getParent() + File.separator;

                                    //Search for injection with this index
                                    String materialName = "unknown" + materialindex;
                                    if (materialindex < 0) {
                                        materialName = "all";
                                    } else {
                                        for (InjectionInformation injection : control.getScenario().getInjections()) {
                                            if (injection.getMaterial().materialIndex == materialindex) {
                                                materialName = injection.getMaterial().getName();
                                            }
                                        }
                                    }

                                    String name = "Cont_" + materialName;
                                    File shpfile = new File(parent + name + ".shp");
                                    File shxfile = new File(parent + name + ".shx");
                                    File jsonfile = new File(parent + name + ".json");
                                    File csvFile = new File(parent + name + ".csv");
                                    //Delete old files
                                    if (shpfile.exists()) {
                                        shpfile.delete();
                                    }
                                    if (shxfile.exists()) {
                                        shxfile.delete();
                                    }
                                    SHP_IO_GULLI.writeWGS84(shapes, shpfile.getAbsolutePath(), name, !control.getSurface().getGeotools().isGloablLongitudeFirst());
                                    System.out.println("Shapefiles written to " + shpfile.getAbsolutePath());
                                    HE_SurfaceIO.writeSurfaceContaminationCSV(csvFile, control.getSurface());

                                    GeoJSON_IO geojson = new GeoJSON_IO();
                                    geojson.setMaximumFractionDigits(5);
                                    //GeoJSON convention is: longitude first;
                                    geojson.swapXY = !StartParameters.JTS_WGS84_LONGITUDE_FIRST;
                                    Geometry[] mercator = new Geometry[shapes.size()];
                                    for (int i = 0; i < mercator.length; i++) {
                                        Geometry geom = shapes.get(i);
                                        mercator[i] = geom;

                                        GeoJSON_IO.JSONProperty[] props = new GeoJSON_IO.JSONProperty[3];
                                        props[0] = new GeoJSON_IO.JSONProperty("stoff", materialName);
                                        props[1] = new GeoJSON_IO.JSONProperty("eventid", 0);
                                        props[2] = new GeoJSON_IO.JSONProperty("part", i);
                                        mercator[i].setUserData(props);
                                    }
                                    geojson.write(jsonfile, mercator);
                                    System.out.println("Shapefiles written to " + jsonfile.getAbsolutePath());

                                }
                            } catch (IOException ex) {
                                Logger.getLogger(Controller.class
                                        .getName()).log(Level.SEVERE, null, ex);
                            } catch (Exception ex) {
                                Logger.getLogger(Controller.class
                                        .getName()).log(Level.SEVERE, null, ex);

                            }
                        }

                    }.start();
                } catch (Exception ex) {
                    Logger.getLogger(RunMainView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        //Find correct menu to add more Tileservers
        JMenu tilesMenu = null;
        for (Component component : frame.getJMenuBar().getComponents()) {
            if (component instanceof JMenu) {
                JMenu menu = (JMenu) component;
                if (menu.getText().equals("Background")) {
                    tilesMenu = menu;
                    break;
                }
            }
        }

        if (tilesMenu != null) {
            tilesMenu.add(new JSeparator());

            JMenuItem itemTonerNoLabel = new ActionMenuItem("Toner No Label");
            tilesMenu.add(itemTonerNoLabel);
            itemTonerNoLabel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    frame.getMapViewer().setBaseLayer(new MyOSMTileSource("Toner No Label", "http://a.tile.stamen.com/toner-background/", 18) {
                    });
                }
            });

            JMenuItem itemThunderforest = new ActionMenuItem("Thunderforest");
            tilesMenu.add(itemThunderforest);
            itemThunderforest.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    frame.getMapViewer().setBaseLayer(new MyOSMTileSource("Thunderforest", " 	http://tile.thunderforest.com/landscape/", 18) {
                    });
                }
            });

            tilesMenu.revalidate();
        }
    }
}
