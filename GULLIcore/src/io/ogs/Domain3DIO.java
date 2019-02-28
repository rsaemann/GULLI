/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.ogs;

import com.vividsolutions.jts.geom.Coordinate;
import control.StartParameters;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.GeoTools;
import model.underground.Domain3D;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class Domain3DIO {

//    public static void main(String[] args) {
//        long starttime = System.currentTimeMillis();
//        File file = new File(".\\Knotengeschwindigkeiten_QuasiSteadyState.vtu");
//
//        try {
//            final Domain3D ug = read3DFlowFieldVTU(file, "EPSG:3857", "EPSG:25832");
//            System.out.println("Max velocity: " + ug.getHighestVelocity() + " m/s");
//            System.out.println("Min velocity: " + ug.getLowestVelocity() + " m/s");
//
//            final GeoTools gt = new GeoTools("EPSG:4326", "EPSG:25832", StartParameters.JTS_WGS84_LONGITUDE_FIRST);
//            SimpleMapViewerFrame frame = new SimpleMapViewerFrame();
//            final MapViewer viewer = frame.getMapViewer();
//            Coordinate wgs84;
//            int i = 0;
//            String layer = "UG";
//            String layerr = "UGR";
//            ColorHolder ch = new ColorHolder(Color.yellow, "Underground");
//            ColorHolder chr = new ColorHolder(Color.red, "Underground");
//            for (Coordinate position : ug.position) {
//                try {
//                    wgs84 = gt.toGlobal(position);
////                System.out.println(position+" \t "+wgs84);
//                    NodePainting np;
//
////                    if(i%6995==0){
////                        np= new NodePainting(i, wgs84, chr);
////                         viewer.addPaintInfoToLayer(layer, np);
////                    }else{
//                    np = new NodePainting(i, wgs84, ch);
//                    viewer.addPaintInfoToLayer(layer, np);
////                    }
//                    i++;
//
////                if(i>1000){
////                    System.out.println("i="+i);
////                    viewer.zoomToFitLayer();
////                    break;
////                }
//                } catch (Exception exception) {
//                    System.err.println("position=" + position);
//                    exception.printStackTrace();
//                }
//            }
//
//            viewer.zoomToFitLayer();
//
//            int nodecount=0;
//            //// Partickel laufen lassen
//            for (int j = 0; j < ug.position.length; j++) {
//
//                int startindex = j;//ug.position.length - 3000;
//                double dt = 60 * 60 * 24 * 365 * 10;
//                double time = 0;
//                int loopindex = -1;
//                Coordinate startC = ug.position[startindex];
//                Coordinate c = new Coordinate(startC.x, startC.y, startC.z);
//                ColorHolder chp = new ColorHolder(Color.blue, "Particle");
//                LinkedList<Coordinate> history = new LinkedList<>();
//                history.add(gt.toGlobal(c));
//                while (loopindex < 100) {
//                    long start = System.currentTimeMillis();
//                    //Find index to this coordinate
//                    int index = ug.getNearestCoordinateIndex(c);
//                    if (index < 0) {
//                        System.out.println("no near coordinate found for " + c);
//                        break;
//                    }
//                    float[] velocity = ug.velocity[0][index];
//                    c.x += velocity[0] * dt;
//                    c.y += velocity[1] * dt;
//                    c.z += velocity[2] * dt;
//                    if (c.z < ug.minZ) {
//                        c.z = ug.minZ;
//                    }
//                    if (c.z > ug.maxZ) {
//                        c.z = ug.maxZ;
//                    }
//                    time += dt;
//                    loopindex++;
//                    history.add(gt.toGlobal(c));
////                    if (loopindex % 10 == 0) {
////                        System.out.println(loopindex + "   " + time + " s: " + (System.currentTimeMillis() - start) + "ms)   v:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t Pos:" + index + " :" + c);
//                        viewer.addPaintInfoToLayer("Particle", new NodePainting(nodecount++, gt.toGlobal(c), chp));
//                        viewer.recalculateShapes();
//                        viewer.repaint();
////                    }
//
//                }
//                viewer.addPaintInfoToLayer("History", new LinePainting(0, history.toArray(new Coordinate[history.size()]), new ColorHolder(Color.orange, "Trace")));
//            }
//            //Input GUI
////            final JPopupMenu popup = new JPopupMenu("Input");
////            JMenuItem itemInput = new JMenuItem("New Trace");
////            popup.add(itemInput);
////            viewer.add(popup);
////            viewer.setComponentPopupMenu(popup);
//            viewer.addMouseListener(new MouseAdapter() {
//                java.awt.Point mousedown;
//
//                @Override
//                public void mousePressed(MouseEvent me) {
//
//                    mousedown = me.getPoint();
//
//                }
//
//                @Override
//                public void mouseReleased(MouseEvent me) {
//                    if (me.isPopupTrigger()) {
//                        if (Math.abs(me.getPoint().x - mousedown.x) < 3 && Math.abs(me.getPoint().y - mousedown.y) < 3) {
//                            try {
//                                //                            System.out.println("Mouse Released");
////                            popup.setLocation(me.getXOnScreen(), me.getYOnScreen());
//                                //Search for near Nodes
//                                Point2D.Double latlon = viewer.getPosition(me.getPoint());
//                                final Coordinate utm = gt.toUTM(new Coordinate(latlon.x, latlon.y));
//
//                                System.out.println("clicked @" + latlon + " ---> " + utm);
//                                //InputCoordinate
//                                utm.z = ug.maxZ;//Auf oberfläche einsetzen
//                                //Finde geländehöhe
//                                double mindistance = 100;
//                                double maxHoehe = Double.NEGATIVE_INFINITY;
//                                for (Coordinate p : ug.position) {
//                                    if (Math.abs(p.x - utm.x) < mindistance && Math.abs(p.y - utm.y) < mindistance) {
//                                        maxHoehe = Math.max(maxHoehe, p.z);
//                                    }
//                                }
//                                utm.z = maxHoehe;
//                                System.out.println("Einsetzen @ " + utm);
//
//                                new Thread() {
//
//                                    @Override
//                                    public void run() {
//
//                                        try {
//                                            double dt = 60 * 60 * 24 * 365 * 1;
//                                            double time = 0;
//                                            int loopindex = -1;
//                                            Coordinate startC = utm;
//                                            Coordinate c = new Coordinate(startC.x, startC.y, startC.z);
//                                            ColorHolder chp = new ColorHolder(Color.blue, "Particle");
//                                            LinkedList<Coordinate> history = new LinkedList<>();
//                                            history.add(gt.toGlobal(c));
//                                            while (loopindex < 300000) {
//                                                long start = System.currentTimeMillis();
//                                                //Find index to this coordinate
//                                                int index = ug.getNearestCoordinateIndex(c);
//                                                if (index < 0) {
//                                                    System.out.println("no near coordinate found for " + c);
//                                                    break;
//                                                }
//                                                float[] velocity = ug.velocity[0][index];
//                                                c.x += velocity[0] * dt;
//                                                c.y += velocity[1] * dt;
//                                                c.z += velocity[2] * dt;
//                                                if (c.z < ug.minZ) {
//                                                    c.z = ug.minZ;
//                                                }
//                                                if (c.z > ug.maxZ) {
//                                                    c.z = ug.maxZ;
//                                                }
//                                                time += dt;
//                                                loopindex++;
//                                                history.add(gt.toGlobal(c));
//                                                if (loopindex % 1 == 0) {
//                                                    viewer.addPaintInfoToLayer("Particle", new NodePainting(loopindex, gt.toGlobal(c), chp));
////                                                    viewer.recalculateShapes();
////                                                    viewer.repaint();
//                                                }
//                                                if (loopindex % 100 == 0) {
//                                                    System.out.println(loopindex + "   " + time + " s: " + (System.currentTimeMillis() - start) + "ms)   v:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t Pos:" + index + " :" + c);
//
//                                                    viewer.recalculateShapes();
//                                                    viewer.repaint();
//                                                }
//                                            }
//                                            viewer.addPaintInfoToLayer("History", new LinePainting(0, history.toArray(new Coordinate[history.size()]), new ColorHolder(Color.orange, "Trace")));
//                                        } catch (TransformException ex) {
//                                            Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
//                                        }
//                                    }
//                                }.start();
//                            } catch (TransformException ex) {
//                                Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        }
//                    } else {
////                        popup.setVisible(false);
//                    }
//                }
//
//            });
//
//        } catch (Exception ex) {
//            Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        System.out.println((System.currentTimeMillis() - starttime) / 1000 + "  s für alles.");
//
//    }
    public static Domain3D read3DFlowFieldVTU(File fileCSV, String fileCRS, String targetMetricCRS) throws FactoryException, FileNotFoundException, IOException, TransformException {
        GeoTools gt = new GeoTools(fileCRS, targetMetricCRS, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        //Count number of lines
        FileReader fr = new FileReader(fileCSV);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        int numberofpoints = -1;
        int numberofcells = -1;
        long starttime = System.currentTimeMillis();
        while (br.ready()) {
            line = br.readLine();
            //#####################################################################
            // EINLESEN DER ANZAHL DER PUNKTE
            //#####################################################################            
            if (line.contains("NumberOfPoints")) {
                String npstr = line.substring(line.indexOf("NumberOfPoints") + 16);
                npstr = npstr.substring(0, npstr.indexOf("\""));
                numberofpoints = Integer.parseInt(npstr);
            }
            //#####################################################################
            // EINLESEN DER ANZAHL DER ZELLEN
            //#####################################################################
            if (line.contains("NumberOfCells")) {
                String ncstr = line.substring(line.indexOf("NumberOfCells") + 15);
                ncstr = ncstr.substring(0, ncstr.indexOf("\""));
                numberofcells = Integer.parseInt(ncstr);
            }
            if (line.contains("<DataArray")) {
                break;//Parse Nodes in another loop
            }
        }
        int lineindex = 0;
        String[] parts;
        Coordinate[] coordinates = new Coordinate[numberofpoints];
        Coordinate file = new Coordinate(0, 0, 0), utm = new Coordinate(0, 0);
        while (br.ready()) {
            line = br.readLine();

            if (line.contains("</DataArray>")) {
                //Parse Nodes beendet
                break;
            }
            parts = line.trim().split(" ");
            file.x = Float.parseFloat(parts[0]);
            file.y = Float.parseFloat(parts[1]);

            coordinates[lineindex] = gt.toUTM(file);
            coordinates[lineindex].z = Float.parseFloat(parts[2]);
            lineindex++;
        }

        // ####################################################################
        // EINLESEN VON ZELLEN cellpoints[ZellID][points][coordinates]
        // MIT DAZUGEHÖRIGEN PUNKTEN points[PunktIDs]
        // MIT DEN KOORDINATEN DER PUNKTE coordinates[].xyz
        // sechs Punkte bilden eine Zelle (in der Regel..? Datensatz lässt Abweichungen vermuten)
        // ####################################################################
        //voranschreiten bis zum Block "connectivity"
        while (br.ready()) {
            line = br.readLine();
            if (line.contains("Name=\"connectivity")) {
                break;
            }
        }
        // notwendige Variablen erstellen
        int[][] cellpoints = new int[numberofcells][];
        int cellakt = 0; // zu betrachtende aktuelle Zelle
        String pointsakt[]; //die Punkte der aktuellen Zelle in Textform

        // reihenweises Einlesen und Zuordnen der
        // Punkte cellpoints[][points] zu  Zellen cellpoints[cell][]
        while (br.ready()) {
            line = br.readLine();
            if (line.contains("</DataArray>")) {
                break;
            }
            pointsakt = line.trim().split(" ");
            //zum Durchlaufen der einzelnen Punkte[0] bis [letzter Punkt]
            cellpoints[cellakt] = new int[pointsakt.length];
            for (int p = 0; p < pointsakt.length; p++) {
                // aktuellen Punkt hinzufügen zu Punkte-Liste der aktuellen Zelle
                cellpoints[cellakt][p] = Integer.parseInt(pointsakt[p]);
            }
            cellakt++;
        }

        // ####################################################################
        // EINLESEN VON GESCHWINDIGKEITEN velocities[Zeitschritt][point][Richtung]
        // ####################################################################
        while (br.ready()) {
            line = br.readLine();

            if (line.contains("</DataArray>")) {
                //Parse Elements /Cells ended
                break;
            }
        }
        float[][][] velocities = null;
        float[] gwdistance = null;
        while (br.ready()) {
            line = br.readLine();

            //Geschwindigkeiten in xyz-Richtung eines jeden Punktes auslesen
            // so in SZENARIO RICKLINGEN
            if (line.contains("Name=\"GL_NODAL_VELOCITY1")) {
                line = br.readLine();
                parts = line.trim().split(" ");
                int index = 0;
                velocities = new float[1][numberofpoints][3];
                for (float[] velocitie : velocities[0]) {
                    for (int j = 0; j < 3; j++) {
                        velocitie[j] = Float.parseFloat(parts[index]);
                        index++;
                    }
                }
            } else //Geschwindigkeiten in xyz-Richtung eines jeden Punktes auslesen
            // so in BENCHMARK RWTP
            if (line.contains("Name=\"NODAL_VELOCITY1")) {
                line = br.readLine();
                parts = line.trim().split(" ");
                int index = 0;
                velocities = new float[1][numberofpoints][3];
                for (float[] velocitie : velocities[0]) {
                    for (int j = 0; j < 3; j++) {
                        velocitie[j] = Float.parseFloat(parts[index]);
                        index++;
                    }
                }
            } else //#####################################################################
            // EINLESEN DER DRUCKHÖHE und BERECHNUNG DER GRUNDWASSERHÖHE
            //#####################################################################
            if (line.contains("Name=\"PRESSURE")) {
                line = br.readLine();
                parts = line.trim().split(" ");
                gwdistance = new float[numberofpoints];
                float rhog = 999.7f * 9.81f;
                for (int i = 0; i < gwdistance.length; i++) {
                    gwdistance[i] = Float.parseFloat(parts[i]) / rhog;
                }
            }
        }
        // ####################################################################
        // BODENMATERIAL AUSLESEN
        //#####################################################################
        int[] material = new int[numberofcells];

//        File fileGroundwater = new File(StartParameters.getPathUndergroundVTU());
        material = read.material(numberofcells, fileCSV);
        //#####################################################################

        System.out.println("soil eingelesen");

        // ####################################################################
        // DATEN STRUKTURIEREN: pointcells[PunktID][dazugehörige CellIDs]
        // ####################################################################
        int[][] pointcells = new int[numberofpoints][161];
        for (int[] pointcell : pointcells) {
            for (int j = 0; j < pointcell.length; j++) {
                pointcell[j] = -1;
            }
        }
        int[] stand = new int[numberofpoints];
        for (int i = 0; i < stand.length; i++) {
            stand[i] = 0;
        }
        int[] standmax = new int[163];
        for (int i = 0; i < standmax.length; i++) {
            standmax[i] = 0;
        }
        for (int c = 0; c < numberofcells; c++) {
            for (int p = 0; p < cellpoints[c].length; p++) {
                pointcells[cellpoints[c][p]][stand[cellpoints[c][p]]] = c;
                stand[cellpoints[c][p]]++;
                standmax[stand[cellpoints[c][p]]]++;
            }
        }

        gt = new GeoTools("EPSG:4326", targetMetricCRS, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        Domain3D domain = new Domain3D(coordinates, velocities, gwdistance, gt);
        domain.NoP = numberofpoints;
        domain.NoC = numberofcells;

        br.close();
        fr.close();

        return domain;
    }

    /**
     * Read FLUENT velocity field
     *
     * @param fileDX
     * @param fileCRS
     * @param targetMetricCRS
     * @return
     * @throws FactoryException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws TransformException
     */
    public static Domain3D read3DFlowFieldDX(File fileDX, String fileCRS, String targetMetricCRS) throws FactoryException, FileNotFoundException, IOException, TransformException {
        GeoTools gt = new GeoTools(fileCRS, targetMetricCRS, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        //Count number of lines
//        FileReader fr = new FileReader(fileDX);
//        for (Map.Entry<String, Charset> c : Charset.availableCharsets().entrySet()) {
//            System.out.println(""+c.getKey());
//        }

        FileInputStream fis = new FileInputStream(fileDX);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-16");
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        int numberofpoints = -1;
        int numberofcells = -1;
        long starttime = System.currentTimeMillis();
        while (br.ready()) {
            line = br.readLine();
            //#####################################################################
            // EINLESEN DER ANZAHL DER PUNKTE
            //#####################################################################            
            if (line.contains("Positions")) {
                line = br.readLine();
                String npstr = line.substring(line.indexOf("items ") + 6);
                npstr = npstr.substring(0, npstr.indexOf("data") - 1);
                numberofpoints = Integer.parseInt(npstr);
                break;
            }
        }
        // Lesen Vertices
        Coordinate[] coordinates = new Coordinate[numberofpoints];
        int index = 0;
        while (br.ready()) {
            line = br.readLine().trim();
            if (line.length() < 15) {
                break;
            }
            line = line.replaceAll("  ", " ");
            String[] parts = line.split(" ");
            coordinates[index] = new Coordinate(Double.parseDouble(parts[0]), -Double.parseDouble(parts[2]), Double.parseDouble(parts[1]));
            index++;
        }

        //#####################################################################
        // EINLESEN DER Geschwindigkeiten
        //#####################################################################      
        while (br.ready()) {
            line = br.readLine();

            if (line.contains("Velocities")) {
                line = br.readLine();
                String npstr = line.substring(line.indexOf("items ") + 6);
                npstr = npstr.substring(0, npstr.indexOf("data") - 1);
                numberofpoints = Integer.parseInt(npstr);
                break;
            }
        }
        float[][][] velocities = new float[1][numberofpoints][3];
        index = 0;
        while (br.ready()) {
            line = br.readLine().trim();
            if (line.length() < 15) {
                break;
            }
            if (line.contains("attribute")) {
                break;
            }
            line = line.replaceAll("  ", " ");
            String[] parts = line.split(" ");
            velocities[0][index][0] = Float.parseFloat(parts[0]);
            velocities[0][index][1] = -Float.parseFloat(parts[2]);
            velocities[0][index][2] = Float.parseFloat(parts[1]);
            index++;
        }

        gt = new GeoTools("EPSG:4326", targetMetricCRS, StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        Domain3D domain = new Domain3D(coordinates, velocities, null, gt);
        domain.NoP = numberofpoints;
        domain.NoC = 0;

        br.close();
        isr.close();
        fis.close();

        return domain;
    }

    public static void main(String[] args) {
        File file = new File("X:\\Abschlussarbeiten\\Li_Xinrui\\Masterarbeit_FLUENT_velocity.vtu");
        try {
            Domain3D domain = read3DFlowFieldDX(file, "EPSG:4647", "EPSG:4647");
        } catch (FactoryException ex) {
            Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformException ex) {
            Logger.getLogger(Domain3DIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Domain3D read3DFlowFieldCSV(File fileCSV, String fileCRS, String targetMetricCRS) throws Exception {
        GeoTools gt = new GeoTools(fileCRS, targetMetricCRS, StartParameters.JTS_WGS84_LONGITUDE_FIRST);

        //Count number of lines
        FileReader fr = new FileReader(fileCSV);
        BufferedReader br = new BufferedReader(fr);
        int lines = 0;
        long starttime = System.currentTimeMillis();
        while (br.ready()) {
            br.readLine();
            lines++;
        }
        br.close();
        fr.close();
        System.out.println((System.currentTimeMillis() - starttime) + ": lines=" + lines);
        Coordinate[] position = new Coordinate[lines - 1];
        float[][][] velocity = new float[1][lines - 1][3];
        float[] gwDistance = new float[lines - 1];// +-> gw above coordinate, - -> gw below coordinate
        fr = new FileReader(fileCSV);
        br = new BufferedReader(fr);

        String line = "";
        String[] values;
        float posX, posY, posZ, Vx, Vy, Vz, dgw;
        Coordinate posNeu;
        br.readLine();
        lines = 0;
        while (br.ready()) {
            line = br.readLine();
            if (line.isEmpty()) {
                continue;
            }
            values = line.split(",");
            posX = Float.parseFloat(values[5]);
            posY = Float.parseFloat(values[6]);
            posZ = Float.parseFloat(values[7]);
            Vx = Float.parseFloat(values[2]);
            Vy = Float.parseFloat(values[3]);
            Vz = Float.parseFloat(values[4]);
            if (!values[8].equals("#WERT!")) {
                dgw = Float.parseFloat(values[8]);
            } else {
                dgw = 0;
            }
            posNeu = gt.toUTM(new Coordinate(posX, posY, posZ));
            posNeu.z = posZ;
            position[lines] = posNeu;
            velocity[0][lines][0] = Vx;
            velocity[0][lines][1] = Vy;
            velocity[0][lines][2] = Vz;

            lines++;
        }
        System.out.println((System.currentTimeMillis() - starttime) + ": =" + lines);
        return new Domain3D(position, velocity, gwDistance, gt);
    }
}
