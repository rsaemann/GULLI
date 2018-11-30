/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import control.LocationIDListener;
import control.StartParameters;
import io.extran.HE_Database;
import io.ogs.Domain3DIO;
import io.ogs.read;
import io.ogs.write;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.JFrame;
import model.GeoTools;
import model.GeoPosition;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position3D;
import model.underground.CellInfo;
import model.underground.Domain3D;
import model.underground.PointInfo;
import view.ColorHolder;
import view.MapViewer;
import view.SimpleMapViewerFrame;
import view.shapes.LabelPainting;
import view.shapes.NodePainting;

/**
 *
 * @author saemann
 */
public class FindLeakagePipes_K {

    public static void main(String[] args) throws Exception {

//        GeometryFactory gff=new GeometryFactory();
//        
//        System.out.println(gff.createPoint(new Coordinate(1,2,3)).toText());
//        System.exit(0);

        /*
         !! Dateipfade im Abschnitt "SZENARIOS, WERTE" und "FIND LEACKAGE PIPES" anpassen !!
         */
        //#####################################################################
        // SZENARIO AUSWÄHLEN (manuelle Eintragungen erforderlich)
        //#####################################################################
        /**
         * 1: Ricklingen 2: 3: Benchmark veri1000
         */
        int szenario = 1;

        int numberOfParticles = 200;

        /**
         * neue_position = alte_position * vorfaktor * sqrt(2*D*dt);
         *
         * 0: keine Dispersion, nur Advektion 1: Vorfaktor ist -1 ODER +1 2:
         * Vorfaktor ist gaußverteilt VON -1 BIS +1
         */
        int disp_method = 2;

        // !! Zeiten sollten ohne Nachkommastelle dividierbar sein, da sonst die Erstellung des Arrays particlehistory nicht klappt
        int simdays = 3666; // so viele Tage sollen simuliert werden

        int outafter = 12 * 28; // nach so vielen Tagen, soll ein Zwischenwert ausgegeben werden

        int dt = 86400 * 14; // Zeitschritt in Sekunden. 1 Tag: 86400 || defaults: 1: *14 , 2: *x , 3: *1

        int ps = 10; // Anzahl der outflowPunkte (nur in Benchmark verwendet) (!! max: 100, sonst muss die ic-Datei angepasst werden)

        int do_output = 0; // Output-Datei erzeugen || 1: ja

        //#####################################################################
        //#####################################################################
        //#####################################################################
        // SZENARIOS, WERTE
        //#####################################################################
//<editor-fold defaultstate="collapsed" desc="SZENARIOS">   
        System.out.println("Szenario:           " + szenario);
        System.out.println("Dispersionsmethode: " + disp_method);
        System.out.println("Simulationszeit:    " + simdays + " Tage");
        System.out.println("Wertanzeige alle:   " + outafter + " Tage");
        System.out.println("Zeitschrittgröße:   " + (dt / 60 / 60 / 24) + " Tage");

        File fileGroundwater = null; // .vtu
        Domain3D soil = null; // Koordinatensystem definieren
        GeoTools gt = null; // Koordinatensystem definieren
        double[] k = null; // Permeabilität (manuell eingeben)
        double n = 0; // Porosität (manuell eingeben)
        int dim = 0;

        boolean isBenchmark = false; // true: Benchmark    false: keine Benchmark=Ricklingen
        System.out.println("--- load Domain ---");

        String fileic = ""; // File mit den outflow-Punkten
        switch (szenario) {
            case 1: // Ricklingen
                fileGroundwater = new File("Y:\\\\EVUS\\Knotengeschwindigkeiten_GWModell\\3D_RICHARDS_FLOW36_august2017.vtu");
                soil = Domain3DIO.read3DFlowFieldVTU(fileGroundwater, "EPSG:3857", "EPSG:25832");
                gt = new GeoTools("EPSG:4326", "EPSG:25832", StartParameters.JTS_WGS84_LONGITUDE_FIRST);
                dim = 3; // 3D Domain
                k = new double[5];
                k[0] = 0.000000000124517170;
                k[1] = 0.000000001126314570;
                k[2] = 0.000000000897462229;
                k[3] = 0.000000000001990781;
                k[4] = 0.000000000002177419;
                n = 0.2;
                isBenchmark = false;
                fileic = "";
                break;
            case 2: // 
                isBenchmark = true;
                break;
            case 3: // veri1000
                fileGroundwater = new File("C:\\Users\\Karsten\\Documents\\Studium\\Master WUK\\Masterarbeit\\JavaCode\\quad_homo\\quad_homo\\input\\RWPT\\quad_homo_1.vtu");
                soil = Domain3DIO.read3DFlowFieldVTU(fileGroundwater, "EPSG:3857", "EPSG:3857");
                gt = new GeoTools("EPSG:4326", "EPSG:3857");
                dim = 2; // 2D Domain
                k = new double[1];
                k[0] = 1.114 * Math.pow(10, -11);
                n = 0.333;
                isBenchmark = true;
                fileic = "C:\\Users\\Karsten\\Documents\\Studium\\Master WUK\\Masterarbeit\\JavaCode\\quad_homo\\quad_homo\\input\\RWPT\\ic.txt";
                break;
            default:
                System.out.println("Kein gültiges Szenario gewählt");
                System.exit(0);
        }

        //#####################################################################
        // mögliche Koordinatenumformungen
//        Domain3D soil = Domain3DIO.read3DFlowFieldVTU(fileGroundwater, "EPSG:3857", "EPSG:25832");
//        GeoTools gt = new GeoTools("EPSG:4326", "EPSG:25832");
//        Domain3D soil = Domain3DIO.read3DFlowFieldVTU(fileGroundwater, "EPSG:3857", "EPSG:3857");
//        GeoTools gt = new GeoTools("EPSG:4326", "EPSG:3857"); 
        //#####################################################################        
        //#####################################################################
        System.out.println("Anzahl Punkte: " + soil.NoP);
        System.out.println("Anzahl Zellen: " + soil.NoC);

        //#####################################################################
        // fixWERTE und VARIABLEN erstellen
        //#####################################################################
//        int numberOfParticles = 200; // 50 Partikel sollen pro Outflow simuliert werden
        int nextout = 60 * 60 * 24 * outafter; // Partikelposition ausgeben nach outafter Tagen [sek]
        int maxT = 60 * 60 * 24 * simdays; // Simulation beenden nach simday Tagen [sek]
        int timesteps = maxT / nextout; // Anzahl der auszugebenden Zeitschritte inkl Startpunkt
        int stepstosave = timesteps + 1; // Zeitschritte + Anfangszustand
        int objectid = 0;
        int historyId = 0;
        double[] d = new double[5]; // Korndurchmesser [BodenArt]
        int NumberOfPoints = soil.NoP;
        int NumberOfCells = soil.NoC;
        GeometryFactory gf = new GeometryFactory();
        double[][][] D = new double[k.length][3][3]; // Dispersionskoeffizient [Bodenart][x y z][long transh transv]
        double[][][] alpha = new double[k.length][3][2]; // Dispersivität [BodenArt][x y z][long trans]        

        int[] material = new int[NumberOfCells]; // BodenArt [für diesen Punkt]
        int[][] cellpoints = new int[NumberOfCells][]; // zu einer Zelle zugehörige Punkte [Zelle][PunktNummer innerhalb der Zelle]
        int[][] pointcells = new int[NumberOfPoints][]; // zu einem Punkt zugehörige Zellen [Punkt][ZellNummer indiv. für diesen Punkt]
        double[][][] particlehistory = new double[numberOfParticles][stepstosave][3]; // [ParticleID][Anzahl Zeitschritte][x y z]
        int[][] cellparticle = new int[stepstosave][NumberOfCells]; // in einer Zelle vorbeigekommene Anzahl Partikel pro Zeit
        for (int i = 0; i < stepstosave; i++) {
            for (int j = 0; j < NumberOfCells; j++) {
                cellparticle[i][j] = 0;
            }
        }
        Random random = new Random();

        //#####################################################################
        // KORNDURCHMESSER d einer [Bodenart] BERECHNEN nach Kozeny-Carman
        //#####################################################################        
        for (int i = 0; i < k.length; i++) {
            d[i] = Math.sqrt(k[i] * (180 * Math.pow(1 - n, 2)) / Math.pow(n, 3));
        }

        //#####################################################################
        // DISPERSIVITÄT alpha FESTLEGEN für [Bodenart][x y z][long trans] auf Grundlage Korndurchmesser
        //#####################################################################        
        switch (szenario) {
            case 1: // Ricklingen
                for (int i = 0; i < k.length; i++) {
                    for (int j = 0; j < 3; j++) {
                        alpha[i][j][0] = d[i];
                        alpha[i][j][1] = d[i] / 10;
                    }
                }
                break;
            default: // Benchmark (ohne Berechnung, hier manuell eintragen)
                for (int i = 0; i < k.length; i++) {
                    for (int j = 0; j < 3; j++) {
                        alpha[i][j][0] = 0.1; // longitudianle Dispersivität [m]
                        alpha[i][j][1] = 0.1; // transversale Dispersivität [m]
                    }
                }
        }
        //#####################################################################
        // BODENMATERIAL AUSLESEN bzw. FESTELGEN
        //#####################################################################
        switch (szenario) {
            case 1: // Ricklingen
                material = read.material(NumberOfCells, fileGroundwater);
                break;
            default:
                for (int i = 0; i < material.length; i++) {
                    material[i] = 0;
                }
                break;
        }

        //#####################################################################
        // ZUSAMMENHÄNGE VON PUNKTEN, ZELLEN UND MATERIAL ORDNEN
        //#####################################################################
        cellpoints = read.cellpoints(NumberOfCells, fileGroundwater);
        pointcells = PointInfo.cellpoints_to_pointcells(NumberOfPoints, NumberOfCells, cellpoints);

        CellInfo cellinfo = new CellInfo(NumberOfCells, material, cellpoints);
        PointInfo pointinfo = new PointInfo(NumberOfPoints, soil.position, soil.velocity, pointcells);

//</editor-fold>        
        //#####################################################################
        //#####################################################################
        // BENUTZEROBERFLÄCHE erstellen
        //#####################################################################
//<editor-fold defaultstate="collapsed" desc="BENUTZEROBERFLÄCHE">     
        DecimalFormat df = new DecimalFormat("0.###");

        final SimpleMapViewerFrame frame = new SimpleMapViewerFrame();
        frame.setVisible(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, 800, 400);
        ColorHolder chIn = new ColorHolder(Color.green, "inflow");
        ColorHolder chOut = new ColorHolder(Color.red, "outflow");
        ColorHolder chTrace = new ColorHolder(Color.orange, "Trace");
        ColorHolder chp = new ColorHolder(Color.blue, "Years");

        String layerIn = "In", layerOut = "Out", layerTrace = "Trace";
        LinkedList<Coordinate> outflows = new LinkedList<>();

        final Point p = new Point();
        MouseMotionAdapter mml = new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent me) {
                p.x = me.getX();
                p.y = me.getY();
            }
        };

        frame.getMapViewer()
                .addMouseMotionListener(mml);

//</editor-fold>  
        //#####################################################################
        //#####################################################################
        // FIND LEAKAGE PIPES (nur Szenario Ricklingen)
        //#####################################################################
//<editor-fold defaultstate="collapsed" desc="PIPENETZWERK">
        HashMap<Coordinate, Pipe> pipeMap = null;
        if (isBenchmark == false) {
            System.out.println("--- FindLeakagePipes ---");

            File fileNetwork = new File("C:\\Users\\saemann\\Documents\\NetBeansProjects\\GULLI\\input\\Modell2017Mai\\2D_Model\\Model.idbf");
            Network network = HE_Database.loadNetwork(fileNetwork);
            System.out.println("pipe-network loaded");
            System.out.println("Anzahl Pipes: " + network.getPipes().size());
            final HashMap<Long, String> pipeInfo = new HashMap<>(network.getPipes().size());
            pipeMap = new HashMap<>(network.getPipes().size());
            for (Pipe pipe : network.getPipes()) { // für jedes Rohr
                StringBuilder infoString = new StringBuilder(pipe.getName() + ";");
                Position3D pos = pipe.getPosition3D(pipe.getLength() * 0.5);
//            System.out.println("Pipe: "+pipe.getName()+"\t"+pipe.getPosition3D(0));
                Coordinate utm = gt.toUTM(new Coordinate(pos.getLongitude(), pos.getLatitude()));
                infoString.append("hig: ").append(df.format(pipe.getStartConnection().getPosition().z)).append(";");
                infoString.append("mid: ").append(df.format(pos.z)).append(";");
                infoString.append("low: ").append(df.format(pipe.getEndConnection().getPosition().z)).append(";");

                utm.z = pos.z;
                int index = soil.getNearestCoordinateIndex(utm);
                if (index < 0) {
                    continue;
                }
//            //List points at same location but in different heights
                //All points along z axis show the same GW height.
//            LinkedList<Integer> indices = new LinkedList<>();
//            Coordinate ref = soil.position[index];
//            for (int i = 0; i < soil.position.length; i++) {
//                Coordinate c = soil.position[i];
//                if (Math.abs(ref.x - c.x) < 0.1 && Math.abs(ref.y - c.y) < 0.1) {
//                    indices.add(i);
//                }
//            }
//            System.out.println(pipe.toString()+" found " + indices.size());
//            for (Integer ix : indices) {
//                System.out.println(ix + ": z=" + df.format(soil.position[ix].z) + "\tdGW=" + df.format(soil.groundwaterDistance[ix]) + "\t GWz=" + df.format(soil.position[ix].z + soil.groundwaterDistance[ix]));
//            }

                if (index < 0 || index >= soil.groundwaterDistance.length) {
                    System.out.println("Could not find Position near " + utm);
                    continue;
                }
                //Tiefe des Wasserstandes an diesem Knoten:
                float gwheight = (float) (soil.position[index].z + soil.groundwaterDistance[index]);
                Coordinate coordinate = soil.position[index];
//            System.out.println("convert " + pos + "\tto " + ll+" \tnearest: "+coordinate);
                Coordinate global = gt.toGlobal(coordinate);

                infoString.append(";");
                infoString.append("Pipep:  " + df.format(utm.x) + " / " + df.format(utm.y)).append(";");
                infoString.append("Gridp:  " + df.format(coordinate.x) + " / " + df.format(coordinate.y)).append(";");
                infoString.append("GridZ:  " + df.format(coordinate.z) + ";");
                infoString.append("GridIdx:" + index + ";");

                infoString.append("GW ds:  " + soil.groundwaterDistance[index] + ";");
                infoString.append("GW hi:  " + gwheight).append(";");
                infoString.append("leckage:" + (gwheight < pos.z));
                pipeInfo.put(pipe.getAutoID(), infoString.toString());

                // wenn das Rohr UNTER der Grundwasseroberfläche liegt
                if (gwheight > pos.z) {
                    NodePainting np = new NodePainting(pipe.getAutoID(), global, chIn);
                    np.setRadius(4);
                    frame.getMapViewer().addPaintInfoToLayer(layerIn, np);

                    // wenn das Rohr ÜBER der Grundwasseroberfläche liegt --> als outflow-Punkt definieren
                } else {
                    NodePainting np = new NodePainting(pipe.getAutoID(), global, chOut);
                    np.setRadius(3);
                    np.setShapeRound(true);
                    frame.getMapViewer().addPaintInfoToLayer(layerOut, np);
                    outflows.add(coordinate);
                    pipeMap.put(coordinate, pipe);
                }
            }
            System.out.println("Anzahl outflows: " + outflows.size());

            frame.getMapViewer()
                    .addListener(new LocationIDListener() {
                        @Override
                        public void selectLocationID(Object o, String string, long l
                        ) {
                            String info = pipeInfo.get(l);
                            Point2D.Double ll = frame.mapViewer.getPosition(p);
                            frame.getMapViewer().addPaintInfoToLayer(frame.getMapViewer().LAYER_KEY_LABEL, new LabelPainting(0, MapViewer.COLORHOLDER_LABEL,  new GeoPosition(ll.x, ll.y), info.split(";")));
                            frame.getMapViewer().recalculateShapes();
                            frame.getMapViewer().repaint();
                        }
                    });

            frame.getMapViewer().recomputeLegend();
            frame.getMapViewer().zoomToFitLayer();

        } else //</editor-fold>
        //oder
        //#####################################################################
        // BENCHMARK Anfangsbedingungen "outflows"  einlesen
        //#####################################################################
        //<editor-fold defaultstate="collapsed" desc="BENCHMARK">
        if (isBenchmark == true) {
            System.out.println("--- outflows einlesen ---");
//            outflows.clear();
            // Anfangsbedingungen einlesen
            double[][] ic = read.concentration(fileic, ps); //[Anzahl Punkte][PunktID x y z concentration]
            // Koordinaten der Anfangsbedingungen zu outflows hinzufügen
            for (int i = 0; i < ic.length; i++) {
                Coordinate coordic = new Coordinate(ic[i][0], ic[i][1], ic[i][2]);
                outflows.add(i, coordic);
            }
            // Koordinaten der outflows NodePainten
            for (int i = 0; i < outflows.size(); i++) {
                Coordinate np2c = new Coordinate(outflows.get(i));
                NodePainting np2 = new NodePainting(i, gt.toGlobal(np2c), chOut);
                frame.getMapViewer().addPaintInfoToLayer(layerOut, np2);
            }
            // Koordinaten des Simulationsfeldes NodePainten
            for (int i = 0; i < soil.position.length; i++) {
                Coordinate np3c = new Coordinate(soil.position[i]);
                NodePainting np3 = new NodePainting(i, gt.toGlobal(np3c), chIn);
                frame.getMapViewer().addPaintInfoToLayer(layerIn, np3);
            }
            System.out.println("Anzahl outflows: " + outflows.size());
            // Karte zentrieren
            frame.getMapViewer().recomputeLegend();
            frame.getMapViewer().zoomToFitLayer();
        }

//</editor-fold>
        //#####################################################################
        //#####################################################################
        // PARTIKELSIMULATION
        //#####################################################################    
        System.out.println("--- Beginne Simulation ---");

//<editor-fold defaultstate="collapsed" desc="PARTIKELSIMULATION">
        //#####################################################################
        // OUTFLOW-PUNKTE DURCHGEHEN
        //#####################################################################
        int outflowid = 0;

        for (Coordinate outflow : outflows) {
//            if (outflowid != 200){// spezielle OutflowID wählen
//                outflowid++;
//                continue;
//            }
            try {
                Pipe pipe = pipeMap.get(outflow);
                if (pipe != null) {
                    File outputFile = new File("L:\\MeMoOut\\Underground\\pipe_" + pipe.getName() + "_HE" + pipe.getManualID() + ".txt");
                    if (outputFile.exists()) {
                        System.out.println(outputFile.getAbsolutePath()+" already exists.");
                        // Do not search for Pipeleackages that alredy have been investigated.
                        outflowid++;
                        continue;
                    }
                }

//                System.out.println("    OutflowID: " + outflowid);
                long starttimeoutflow = System.currentTimeMillis();
                Coordinate[] particles = new Coordinate[numberOfParticles];
                ArrayList<Coordinate>[] yearSnapshots = new ArrayList[simdays / 365 + 1];
                //initArray
                for (int i = 0; i < yearSnapshots.length; i++) {
                    yearSnapshots[i] = new ArrayList<>(numberOfParticles);
                }
                int lastyear = 0;

//            LinkedList<Coordinate>[] historys = new LinkedList[numberOfParticles];
                for (int i = 0; i < particles.length; i++) {
                    particles[i] = new Coordinate(outflow);
//                historys[i] = new LinkedList<>();
                }

                //#####################################################################
                // PARTIKEL DURCHGEHEN
                //#####################################################################
                for (int i = 0; i < numberOfParticles; i++) {
                    lastyear = 0;
//                    if ((i + 1) % 10 == 0) {
//                        System.out.println("        PartikelID: " + i);
//                    }
                    Coordinate c = particles[i]; //Koordinate c des Partikels i
//                double cz_old = c.z;// Höhe z des Zeitschritts hinterlegen

//                LinkedList<Coordinate> history = historys[i];
//                history.add(gt.toGlobal(c));
                    //#####################################################################
                    // ZEITSCHRITTE DURCHRECHNEN
                    //#####################################################################
                    int time = 0;
                    int outputindex = 0;

                    while (time <= maxT) {
                        // Zelle suche, in der das Partikel i ist
                        int cellid = -1;
                        double cell_vol = -1;
                        // finde Zelle, in dem sich der Punkt c anfangs befindet
                        for (int j = 0; j < NumberOfCells; j++) {
                            if (CellInfo.isinCell(c, j, cellinfo, pointinfo, dim) == true) {
                                cellid = j;
//                                System.out.println("cellid: "+ cellid);
                                break;
                            }
                        }

                        if (cellid == -1) {
                            break;
                        }

                        //#####################################################################
                        // bei erreichen der Ausgabenzeit:
                        // POSITION ZUM SPEICHERN HINTERLEGEN
                        // ANZAHL DER BISHERIGEN PARTIKEL IN DER ZELLE ERHÖHEN (~Konzentration)
                        // AUSGABEQUADRÄTCHEN ENTSPRECHNEND DER HÖHE z EINFÄRBEN UND AUSGEBEN
                        //#####################################################################
//                    if (time == (nextout * outputindex)) {
//                        // Speichere von Partikel i zum Zeitschritt pht die Position xyz
//                        particlehistory[i][outputindex][0] = c.x;
//                        particlehistory[i][outputindex][1] = c.y;
//                        particlehistory[i][outputindex][2] = c.z;
//                        // Wert der Anzahl der Partikel in Zelle cell zu diesem Zeitpunkt um eins erhöhen
//                        cellparticle[outputindex][cellid]++;
//
//                        if (false) {
//                            //<editor-fold defaultstate="collapsed" desc="Partikelfarbe hinterlegen">
//                            try {
//                                double zc = c.z;
//
//                                // Bezugspunkt z_old ist die Position zum vorherigen Zeitschritt, Skala: 0 +- fak [Meter]
//                                double fak = 0.25;
//                                final int r = (int) Math.min(((Math.max(Math.signum(cz_old - zc) * Math.abs((cz_old - zc) / (cz_old - (cz_old - fak))), 0)) * 255), 255);
//                                final int g = 0;
//                                final int b = (int) Math.min(((Math.max(Math.signum(zc - cz_old) * Math.abs((zc - cz_old) / (cz_old - (cz_old - fak))), 0)) * 255), 255);
//
////                            System.out.println("cz_old:"+cz_old+"  zc:"+zc+"  r:"+r+"  g:"+g+"  b:"+b);
//                                NodePainting np = new NodePainting(historyId++, gt.toGlobal(c), chp) {
//                                    @Override
//                                    public boolean paint(Graphics2D g2) {
//                                        g2.setColor(new Color(65536 * r + 256 * g + b));
//                                        return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
//                                    }
//                                };
//                                frame.getMapViewer().addPaintInfoToLayer("Particle", np);
//
//                                history.add(gt.toGlobal(c));
//                            } catch (Exception exception) {
//                                exception.printStackTrace();
//                            }
//                            // jetzige zPosition bis zur nächsten Ausgabe hinterlegen
//                            cz_old = c.z;
////</editor-fold>
//                        }
//                        outputindex++;
//                    }
                        //Zeit für Jahresausgabe?
                        int yearIndex = time / 31536000;
                        if (yearIndex > lastyear) {
                            if (!Double.isNaN(c.x) && !Double.isNaN(c.y)) {
                                Coordinate cs = new Coordinate(c);

                                yearSnapshots[yearIndex].add(cs);
//                            System.out.println("Add particle "+i+" in year "+yearIndex+".  contains: "+yearSnapshots[yearIndex.size());
                            }
                            lastyear = yearIndex;
                        }

                        //<editor-fold defaultstate="collapsed" desc="neue Partikelposition berechnen">
//                    //#####################################################################
//                    // GESCHWINDIGKEIT DES NÄCHSTGELEGENEN PUNKTES VERWENDEN
//                    //#####################################################################
//                    int pointindex = soil.getNearestCoordinateIndex(c);
//                    if (pointindex < 0) {
//                        System.out.println("no near coordinate found for " + c);
//                        break;
//                    }
//                    velocity = soil.velocity[0][pointindex]; //timeindex 0: is stationary flowfield
                        //#####################################################################
                        // GESCHWINDIGKEIT AUS DEN PUNKTEN DER ZELLE cell FÜR KOORDINATE c INTERPOLIEREN
                        //#####################################################################
                        double[] weightpj = new double[cellinfo.PointIDs[cellid].length]; // [Zellpunkte]
                        double weighttot = 0;
                        double[][] velopj = new double[cellinfo.PointIDs[cellid].length][3]; // [Zellpunkte][x y z]
                        float[] velocity = new float[3]; // [x y z]
                        // Wichtung der Werte jeden Punktes ermitteln
                        for (int j = 0; j < cellinfo.PointIDs[cellid].length; j++) {
                            Coordinate pj = pointinfo.Position[cellinfo.PointIDs[cellid][j]];
                            weightpj[j] = Math.abs(Math.sqrt((c.x * c.x) + (c.y * c.y) + (c.z * c.z)) - Math.sqrt((pj.x * pj.x) + (pj.y * pj.y) + (pj.z * pj.z)));
                            weighttot += weightpj[j];
                            velopj[j][0] = pointinfo.Velocity[0][cellinfo.PointIDs[cellid][j]][0];
                            velopj[j][1] = pointinfo.Velocity[0][cellinfo.PointIDs[cellid][j]][1];
                            velopj[j][2] = pointinfo.Velocity[0][cellinfo.PointIDs[cellid][j]][2];
                        }
                        velocity[0] = 0;
                        velocity[1] = 0;
                        velocity[2] = 0;

                        // berechne Geschwindigkeit bei c aus den gewichteten Geschwindigkeiten der ZellPunkte
                        for (int j = 0; j < cellinfo.PointIDs[cellid].length; j++) { // Geschwindigkeit an Punkt c berechnen
                            Coordinate cj = pointinfo.Position[cellinfo.PointIDs[cellid][j]];
                            velocity[0] += velopj[j][0] * weightpj[j] / weighttot;
                            velocity[1] += velopj[j][1] * weightpj[j] / weighttot;
                            velocity[2] += velopj[j][2] * weightpj[j] / weighttot;
                        }

                        //#####################################################################
                        // DISPERSIONSKOEFFIZIENTEN BERECHNEN
                        //#####################################################################            
                        for (int j = 0; j < k.length; j++) {
                            D[j][0][0] = alpha[j][0][0] * Math.abs(velocity[0]);
                            D[j][0][1] = alpha[j][0][1] * Math.abs(velocity[0]);
                            D[j][0][2] = alpha[j][0][1] * Math.abs(velocity[0]);
                            D[j][1][0] = alpha[j][1][1] * Math.abs(velocity[1]);
                            D[j][1][1] = alpha[j][1][0] * Math.abs(velocity[1]);
                            D[j][1][2] = alpha[j][1][1] * Math.abs(velocity[1]);
                            D[j][2][0] = alpha[j][2][1] * Math.abs(velocity[2]);
                            D[j][2][1] = alpha[j][2][1] * Math.abs(velocity[2]);
                            D[j][2][2] = alpha[j][2][0] * Math.abs(velocity[2]);
                        }

                        //#####################################################################
                        // ADVEKTIVEN ANTEIL BERECHNEN UND VERRECHNEN
                        //#####################################################################
                        c.x += velocity[0] * dt;
                        c.y += velocity[1] * dt;
                        c.z += velocity[2] * dt;

                        //#####################################################################
                        // DISPERSIVEN ANTEIL je nach gewählter Methode BERECHNEN UND VERRECHNEN
                        //#####################################################################
                        switch (disp_method) {
                            case 0:// keine Dispersion
                                break;
                            case 1: // Dispersions"Vorfaktor": -1 oder +1
                                for (int j = 0; j < 2; j++) { // für jede Richtung j = x y z
                                    c.x += Math.signum(random.nextGaussian()) * Math.sqrt(2 * D[material[cellid]][j][0] * dt);// aus x-y-z-Richtung je die x-Komponente
                                    c.y += Math.signum(random.nextGaussian()) * Math.sqrt(2 * D[material[cellid]][j][1] * dt);// aus x-y-z-Richtung je die y-Komponente
                                    c.z += Math.signum(random.nextGaussian()) * Math.sqrt(2 * D[material[cellid]][j][2] * dt);// aus x-y-z-Richtung je die z-Komponente
                                }
                                break;
                            case 2: // Dispersions"Vorfaktor" gaußverteilt von -1 bis +1
                                for (int j = 0; j < 2; j++) { // für jede Richtung j = x y z
                                    c.x += random.nextGaussian() * Math.sqrt(2 * D[material[cellid]][j][0] * dt);
                                    c.y += random.nextGaussian() * Math.sqrt(2 * D[material[cellid]][j][1] * dt);
                                    c.z += random.nextGaussian() * Math.sqrt(2 * D[material[cellid]][j][2] * dt);
                                }
                                break;
                            default:
                                System.out.println("Keine gültige Dispersionsmethode gewählt");
                                System.exit(0);
                        }

                        //#####################################################################
                        // ggf. Partikel von außerhalb von "soil" an den soil-Rand hineinsetzen
                        if (c.z < soil.minZ) {
                            c.z = soil.minZ;
//                        System.out.println("  below min");
                        }
                        if (c.z > soil.maxZ) {
                            c.z = soil.maxZ;
//                        System.out.println("  above max");
                        }
//</editor-fold>

                        time += dt; // Zeitschritt voranschreiten
                    }//while (time <= maxT)

//                // Linie/Pfeilkopf zwischen den Punkten zeichnen
//                LinePainting lp = new LinePainting(objectid++ * numberOfParticles + i, history.toArray(new Coordinate[history.size()]), chTrace);
//                lp.arrowheadvisibleFromZoom = 20; // je höher die Zahl, desto näher muss man reinzoomen, dass Pfeilkopf angezeigt werden
//                frame.getMapViewer().addPaintInfoToLayer(layerTrace, lp);
                    frame.getMapViewer().recalculateShapes();
                    frame.getMapViewer().repaint();

                }//for (int i = 0; i < numberOfParticles; i++)

                System.out.println("    Calculating " + outflowid + " took " + ((System.currentTimeMillis() - starttimeoutflow) / 1000) + "s.");

//            System.out.println("    Outflow " + outflowid + " abgeschlossen.");
                // Koordinaten der Partikel vom  Outflow mit Zeitpunkten speichern
                // Konzentraionen der Zellen speichern
                if (do_output == 1) { // nur output erzeugen, wenn gewünscht
                    write.write_particlehistory(outflowid, particlehistory, "outflow"); // für Matlab-Plot
                } else {
                }

                if (do_output == 1) {
                    write.write_concentration(cellparticle, cellinfo, pointinfo, "concentrations, outflow " + outflowid);
                }

                //Write output of geometries
                //Create shape containing the whole clod
                pipe = pipeMap.get(outflow);
                if (pipe != null) {
                    File outputFile = new File("L:\\MeMoOut\\Underground\\pipe_" + pipe.getName() + "_HE" + pipe.getManualID() + ".txt");
                    if (!outputFile.exists()) {
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
                            bw.write(pipe.toString());
                            bw.newLine();
                            bw.write("Particles:" + numberOfParticles);
                            bw.newLine();
                            bw.write("Timestep:" + dt + "s");
                            bw.newLine();
                            bw.write("Outputs:" + (yearSnapshots.length - 1));
                            bw.newLine();
                            bw.write("CRS:EPSG:4326");
                            bw.newLine();
                            Coordinate o = gt.toGlobal(outflow);
                            bw.write("Source: POINT(" + o.x + " " + o.y + " " + o.z + ")");
                            bw.newLine();
                            for (int i = 1; i < yearSnapshots.length; i++) {
                                bw.write("Year:" + i);
                                bw.newLine();
                                MultiPoint mp = (MultiPoint) gt.toGlobal(gf.createMultiPoint(yearSnapshots[i].toArray(new Coordinate[yearSnapshots[i].size()])));
//                                Geometry ch = mp.convexHull();
//
//                                Geometry g2 = mp.union(gf.createPoint(outflow));
//                                Geometry ch2 = g2.convexHull();
//                                Geometry boundary = gt.toGlobal(ch);
//                            bw.write(gt.toGlobal(mp).toString());//boundary.toString());
                                bw.write("MULTIPOINT (");
                                for (int j = 0; j < mp.getCoordinates().length - 1; j++) {
                                    Coordinate c = mp.getCoordinates()[j];
                                    bw.write("(" + c.x + " " + c.y + " " + c.z + "),");
                                }
                                //Last one without comma
                                Coordinate c = mp.getCoordinates()[mp.getCoordinates().length - 1];
                                bw.write("(" + c.x + " " + c.y + " " + c.z + "))");
//                            
                                bw.newLine();
                                bw.flush();
                            }
                            bw.flush();
                        }
                        System.out.println("Wrote file " + outputFile.getAbsolutePath() + "\t" + outflowid + "/" + outflows.size());
                    } else {
                        System.out.println("File already exists: " + outputFile.getAbsolutePath() + "\t" + outflowid + "/" + outflows.size());
                    }
                } else {
                    System.err.println("No pipe found at position " + outflow + ". No outputfile can be generated.");
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            outflowid++;

        }//for (Coordinate outflow : outflows)

        // Koordinaten der Partikel vom  Outflow mit Zeitpunkten speichern
        // Konzentraionen der Zellen speichern
        if (do_output == 1) { // nur output erzeugen, wenn gewünscht
            write.write_concentration(cellparticle, cellinfo, pointinfo, "concentrations"); // für Matlab-Plot
        } else {
        }

//</editor-fold>
        System.out.println("--- Simulation von Szenario " + szenario + " beendet ---");

        System.exit(0);
    }
}
