
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import control.Controller;
import control.LoadingCoordinator;
import control.ShapeTools;
import control.StartParameters;
import control.listener.SimulationActionAdapter;
import control.scenario.injection.InjectionInformation;
import io.GeoJSON_IO;
import io.SHP_IO_GULLI;
import io.extran.HE_SurfaceIO;
import io.web.SFTP_Client;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.GeoPosition;
import model.GeoPosition2D;
import model.particle.Material;
import model.surface.measurement.SurfaceMeasurementLine;
import model.surface.measurement.SurfaceMeasurementRaster;
import model.timeline.array.TimeIndexContainer;
import model.topology.Manhole;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.gui.jmapviewer.source.MyOSMTileSource;
import view.ColorHolder;
import view.MapViewer;
import view.PaintManager;
import view.SimpleMapViewerFrame;
import view.ViewController;
import view.shapes.Layer;
import view.shapes.LinePainting;

/**
 * Generic_unsteady_lagrangian_locatIng
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

        final Controller control = new Controller();
        ViewController vcontroller = new ViewController(control);
        final SimpleMapViewerFrame frame = vcontroller.getMapFrame();
        final PaintManager paintManager = vcontroller.getPaintManager();

        //Zeitschrittweite für Partikalbewegung setzen (Sekunden)
        control.getThreadController().setDeltaTime(1);

        //Benutzeroberfläche Hintergrundkarte anpassen.
        MapViewer.verboseExceptions = true;
        vcontroller.getMapViewer().setBaseLayer(MyOSMTileSource.BaseLayer.CARTO_LIGHT.getSource());
        vcontroller.getMapViewer().recomputeCopyright();

        final LoadingCoordinator lc = control.getLoadingCoordinator();

        //Automatisches Suchen und einlesen der Inputfiles, die mit dem HE-Result verknüpft sind.
        if (lc.getFilePipeResultIDBF() == null) {
            //Start file can be set in the GULLI.ini after first start in the main folder.
            File startFile = new File(StartParameters.getStartFilePath());

            if (!startFile.exists()) {
                //Fallback, if nthing was set in the GULLi.ini
                startFile = new File("L:\\GULLI_Input\\Modell2017Mai\\2D_Model\\Extr2D_E2D1T50_mBK.result\\Ergebnis.idbf");
            }
            startFile = new File("L:\\EVUS_Hannover_gesamt2DAB\\EVUS_Hannover_gesamt2DAB\\He2D_RegenRaster_22_06_2017_v2.result\\Ergebnis.idbr");
            if (startFile.exists()) {
                //Try to crawl all dependent files from the information stored in the He result file.
                lc.requestDependentFiles(startFile, true, true);

                //Otherwise set them manually
//                lc.setPipeNetworkFile(NETWORKFILE);
//                lc.setPipeResultsFile(NETWORKVELOCITIES);
//                lc.setSurfaceTopologyDirectory(SURFACE DIRECTORY);
//                lc.setSurfaceWaterlevelFile(SURFACEWATERLEVELANDVELOCITY);
            } else {
                System.out.println("startfile does not exist");
            }
        }
        //Loading finisher sorgt dafür, dass nach erfolgreichem Ladevorgang der Input Dateien automatisch ein Simulatiomnslauf gestartet wird.
        lc.loadingFinishedListener.add(new Runnable() {
            @Override
            public void run() {
                try {
                    control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Automatic start after loading loop has finished.   
                if (false) {
                    if (control.getSurface() != null && frame != null) {
                        try {
                            //Apply Measurement line
                            long startTime = System.currentTimeMillis();
                            SurfaceMeasurementLine ms = SurfaceMeasurementLine.createLine(control.getSurface(), 300, 3003, new TimeIndexContainer(new long[0]));
//                            System.out.println("Created " + ms.getMeasurements().length + " triangle measurements in " + (System.currentTimeMillis() - startTime) + "ms.");
                            ColorHolder ch = new ColorHolder(Color.red, "Measurements");
                            LinePainting lp = new LinePainting(-1, new GeoPosition2D[]{ms.getStart(), ms.getEnd()}, ch);
                            frame.getMapViewer().addPaintInfoToLayer("MELI", lp);
                            for (int i = 0; i < ms.getMeasurements().length; i++) {
//                            System.out.println(ms.getMeasurements()[i].getTriangleID() + "");
                                double[] xy = control.getSurface().getTriangleMids()[ms.getMeasurements()[i].getTriangleID()];
                                Coordinate ll = control.getSurface().getGeotools().toGlobal(new Coordinate(xy[0], xy[1]));
//                            NodePainting np = new NodePainting(i, ll, ch);
//                            control.getMapFrame().getMapViewer().addPaintInfoToLayer("MELI", np);
                                paintManager.addTriangleMeasurement(ms.getMeasurements()[i], new GeoPosition(ll));
                            }
                            frame.getMapViewer().recalculateShapes();
                        } catch (TransformException ex) {
                            Logger.getLogger(RunMainView.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                if (true) {
                    //Sensitivity Paper 
                    if (true) {
                        //Paper scenario 
                        int anzahl = 30000;
                        Manhole mh = control.getNetwork().getManholeByName("MU04S503");
//                    lc.addInjectionInformation(new InjectionInformation(mh, 10, 20000, new Material("Schmutz0", 1000, true), 0, 1*60*60));
                        System.out.println(getClass() + ". add 3 Injections");
                        try {
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+0", 1000, true, 0), 0, 7 * 60));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+10", 1000, true, 1), 9 * 60, 7 * 60));
                            lc.addInjectionInformation(new InjectionInformation(mh, 0, 10, anzahl, new Material("Munz_" + anzahl + "+20", 1000, true, 2), 19 * 60, 7 * 60));

                        } catch (NullPointerException nullPointerException) {
                            System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                        }
                    }

                    if (true) {
                        if (false) {
                            //Same Position
                            //Spatial sensitivity injection  3 times at same positions at circle west Bückeburger allee  B65 ("MU04N519") // Hamelner Chaussee (MU08S546) //Ginsterbusch (RI09S519) //Munzeler straße
                            try {
                                control.getThreadController().setDeltaTime(1);
                                SurfaceMeasurementRaster.countStayingParticle = false;
                                PaintManager.maximumNumberOfParticleShapes = 100000;
                                double factor = 100000.;
                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_100k_s6_0+0_10", 1000, true, 0), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_1+0_10", 1000, true, 1), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_2+0_10", 1000, true, 2), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_3+0_10", 1000, true, 3), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_4+0_10", 1000, true, 4), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_5+0_10", 1000, true, 5), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_6+0_10", 1000, true, 6), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_7+0_10", 1000, true, 7), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_8+0_10", 1000, true, 8), 0 * 60, 1 * 60));
//                                lc.addInjectionInformation(new InjectionInformation("MU04S503", 0, 10, (int) (factor), new Material("same_Munz_10k_s6_9+0_10", 1000, true, 9), 0 * 60, 1 * 60));

                            } catch (NullPointerException nullPointerException) {
                                System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                            }
                        }

                        if (false) //Spatial sensitivity injection at 3 positions along Bückeburger allee alongside B65
                        {
                            try {
                                int anzahl = 100000;

                                lc.addInjectionInformation(new InjectionInformation("MU04N505", 0, 10, anzahl, new Material("s_B65_west+10", 1000, true, 0), 10 * 60, 1 * 60));
                                lc.addInjectionInformation(new InjectionInformation("RI01N503", 0, 10, anzahl, new Material("s_B65_midd+10", 1000, true, 1), 10 * 60, 1 * 60));
                                lc.addInjectionInformation(new InjectionInformation("RI01S519", 0, 10, anzahl, new Material("s_B65_east+10", 1000, true, 2), 10 * 60, 1 * 60));

                            } catch (NullPointerException nullPointerException) {
                                System.out.println("RunMain: " + nullPointerException.getLocalizedMessage());
                            }

                        }
                    }
                }

                //Zoom to Area
                if (frame != null) {
                    if (frame.getMapViewer().getZoom() < 15) {
//                        System.out.println();
                        frame.getMapViewer().zoomToFitLayer();
                    }
                }

                control.recalculateInjections();

                control.resetScenario();

                if (control.getScenario() != null) {
                    control.start();
                }
                final Runnable r = this;

                new Thread() {

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
        control.getThreadController().addSimulationListener(new SimulationActionAdapter() {
            @Override
            public void simulationFINISH(boolean timeOut, boolean particlesOut
            ) {
                try {
                    if (control.getSurface() != null) {
                        int particleCount = 0;
//                        for (InjectionInformation injection : control.getScenario().getInjections()) {
//                            particleCount += injection.getNumberOfParticles();
//                        }
//                        File surfaceOutCsv = new File(control.getLoadingCoordinator().getFilePipeResultIDBF().getParentFile(), "surfaceContamination" + particleCount + ".csv");
//                        HE_SurfaceIO.writeSurfaceContaminationCSV(surfaceOutCsv, control.getSurface());
//                        System.out.println("Contamination on Surface written to " + surfaceOutCsv.getAbsolutePath());

                    }
                    //Draw shortcuts
//                    if (control.getMapFrame() != null && control.getPaintManager() != null) {
//                        control.getPaintManager().drawShortcuts();
//                    }

                    if (false) {
                        //paper ausschnitt
                        paintManager.setSurfaceShow(PaintManager.SURFACESHOW.CONTAMINATIONCLUSTER);

                        frame.getMapViewer().setZoom(17);
                        frame.getMapViewer().setDisplayPositionByLatLon(52.3475, 9.7222, 17);
                        frame.setBounds(300, 100, 500, 450);
                        frame.getMapViewer().showLegend = false;
                        frame.getMapViewer().setShowLegendInExportFile(false);

                        for (Layer layer : frame.getMapViewer().getLayers()) {
//                            System.out.println(layer + " ");
                            if (layer.getKey().contains("MH")) {
                                layer.setVisibleInMap(false);
                            } else if (layer.getKey().contains("Pip")) {
                                layer.setVisibleInMap(false);
                            } else if (layer.getKey().contains("INL")) {
                                layer.setVisibleInMap(false);
                            } else if (layer.getKey().toLowerCase().contains("ptc")) {
                                layer.setVisibleInMap(false);
                            }
                        }
                    }
                    //Create shapefiles
                    new Thread() {

                        @Override
                        public void run() {
                            try {
                                if (control.getSurface() == null) {
                                    return;
                                }
                                SFTP_Client sftp = null;
//                                try {
//                                    sftp = SFTP_Client.FromFile(StartParameters.getProgramDirectory() + "\\SFTP.ini");
//                                } catch (Exception iOException) {
//                                    iOException.printStackTrace();
//                                } 
                                for (int m = -1; m < control.getSurface().getNumberOfMaterials(); m++) {

                                    int materialindex = m; //<0 = all
                                    ArrayList<Geometry> shapes = null;
                                    try {
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
//                    JSON_IO.writeWGS84_Filtered(shapes,jsonfile);

//                    GeoTools gt = new GeoTools("EPSG:4326", "EPSG:3857");
                                    GeoJSON_IO geojson = new GeoJSON_IO();
                                    geojson.setMaximumFractionDigits(5);
                                    geojson.swapXY = true;
                                    Geometry[] mercator = new Geometry[shapes.size()];
                                    for (int i = 0; i < mercator.length; i++) {
                                        Geometry geom = shapes.get(i);
//                        System.out.println(i + ") " + geom.getGeometryType() + "  number of geometries: " + geom.getNumGeometries());
                                        mercator[i] = geom;//gt.toUTM(geom);
//                        if (geom instanceof Polygon) {
//                            Polygon poly = (Polygon) geom;
////                            System.out.println("   polygon: interiors:" + poly.getNumInteriorRing());
//                        }
                                        GeoJSON_IO.JSONProperty[] props = new GeoJSON_IO.JSONProperty[3];
                                        props[0] = new GeoJSON_IO.JSONProperty("stoff", "Benzin");
                                        props[1] = new GeoJSON_IO.JSONProperty("eventid", 0);
                                        props[2] = new GeoJSON_IO.JSONProperty("part", i);
                                        mercator[i].setUserData(props);
                                    }
                                    geojson.write(jsonfile, mercator);
                                    System.out.println("Shapefiles written to " + jsonfile.getAbsolutePath());

                                    //Upload to official Server
                                    try {
                                        if (sftp != null) {
                                            sftp.upload(jsonfile, "isu2");
                                        }
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
//                                    //Upload to FTP Test server
//                                    try {
//                                        FTP_Client ftp = new FTP_Client();
//                                        FTPClient client = ftp.getClient();
//                                        if (client.isConnected()) {
//                                            client.upload(jsonfile, 0);
//                                            System.out.println("Uploaded "+jsonfile.getName()+" to "+client.getHost());
//                                        } else {
//                                            System.err.println("Client not connected. Can not upload geojson files to webserver.");
//
//                                        }
//                                    } catch (java.net.SocketTimeoutException ex) {
//                                        ex.printStackTrace();
////                        System.err.println("No connection to server established.");
//                                    } catch (FileNotFoundException ex) {
//                                        Logger.getLogger(Controller.class
//                                                .getName()).log(Level.SEVERE, null, ex);
//                                    } catch (FTPDataTransferException ex) {
//                                        Logger.getLogger(Controller.class
//                                                .getName()).log(Level.SEVERE, null, ex);
//                                    } catch (FTPAbortedException ex) {
//                                        Logger.getLogger(Controller.class
//                                                .getName()).log(Level.SEVERE, null, ex);
//                                    } catch (NoClassDefFoundError err) {
//                                        Logger.getLogger(Controller.class
//                                                .getName()).log(Level.SEVERE, null, err);
//                                    }
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
    }
}
