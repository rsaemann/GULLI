/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io;

import com.vividsolutions.jts.geom.Coordinate;
import control.StartParameters;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import model.GeoTools;
import model.timeline.array.TimeContainer;
import model.underground.Domain3D;


/**
 *
 * @author saemann
 */
public class KLAM_IO {

    public static boolean verbose = false;

    public static Domain3D loadDomain(File directory, float xoffset, float yoffset) throws FileNotFoundException, IOException, Exception {
        //Count number of layers
        int layer = 0;
        File selectedDirectory = null;
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                layer++;
                selectedDirectory = f;
            }
        }
        System.out.println("Directory contains of " + layer + " layers.");

        //search for inital information in one of these layers
//search for uz files
        LinkedList<File> ufiles = new LinkedList<>();
        LinkedList<File> vfiles = new LinkedList<>();
        for (File f : selectedDirectory.listFiles()) {
            if (f.getName().startsWith("uz")) {
                ufiles.add(f);
            } else if (f.getName().startsWith("vz")) {
                vfiles.add(f);
            }
        }

        if (ufiles.size() == vfiles.size()) {

        } else {
            System.err.println("ufiles(" + ufiles.size() + ") and vfiles(" + vfiles.size() + ") do not have the same length. -> break");
            return null;
        }
        ArrayList<Integer> timesteps = new ArrayList<>(ufiles.size());
        for (File ufile : ufiles) {
            Integer step = Integer.parseInt(ufile.getName().substring(2, ufile.getName().length() - 4));
            timesteps.add(step);
        }
        Collections.sort(timesteps);
        System.out.println(timesteps.size() + " Times:");
        for (Integer timestep : timesteps) {
            System.out.print(timestep + " s, ");
        }
        System.out.println("");
        //Create Timecatalog
        long[] times = new long[timesteps.size()];
        for (int i = 0; i < timesteps.size(); i++) {
            times[i] = timesteps.get(i) * 1000L;
        }
        TimeContainer timeCatalog = new TimeContainer(times);

        //Create first lattice
        System.out.println("read information from file " + ufiles.getFirst().getAbsolutePath());
        FileReader fr = new FileReader(ufiles.getFirst());
        BufferedReader br = new BufferedReader(fr);
        String line = "";//br.readLine();
        //Header lesen
        int ncols = -1, nrows = -1, xllcorner = -1, yllcorner = -1;
        float cellsize = -1;
        while (br.ready()) {
            line = br.readLine();
            if (line.startsWith("ncols")) {
                ncols = Integer.parseInt(line.replaceAll(" ", "").substring(5));
            } else if (line.startsWith("* Anzahl Spalten")) {
                ncols = Integer.parseInt(line.replaceAll(" ", "").substring(14));
            } else if (line.startsWith("nrows")) {
                nrows = Integer.parseInt(line.replaceAll(" ", "").substring(5));
            } else if (line.startsWith("* Anzahl Zeilen")) {
                nrows = Integer.parseInt(line.replaceAll(" ", "").substring(13));
            } else if (line.startsWith("xllcorner")) {
                xllcorner = Integer.parseInt(line.replaceAll(" ", "").substring(9));
            } else if (line.startsWith("yllcorner")) {
                yllcorner = Integer.parseInt(line.replaceAll(" ", "").substring(9));
            } else if (line.startsWith("cellsize")) {
                cellsize = Integer.parseInt(line.replaceAll(" ", "").substring(8));
            } else if (line.startsWith("* Zellgr")) {
//                System.out.println(line);
                String format = line.replaceAll(" ", "").substring(10);
                if (format.contains("m")) {
                    format = format.substring(0, format.indexOf("m"));

                }
                cellsize = Float.parseFloat(format);
            }
        }
        System.out.println("ncols=" + ncols);
        System.out.println("nrows=" + nrows);
        System.out.println("x=" + xllcorner);
        System.out.println("y=" + yllcorner);
        System.out.println("size=" + cellsize);
        if (xllcorner < 0) {
            xllcorner = (int) xoffset;
        }
        if (yllcorner < 0) {
            yllcorner = (int) yoffset;
        }

        float yulcorner = yllcorner + nrows * cellsize;

        Coordinate[] positions = new Coordinate[ncols * nrows * layer];
        float[][][] velocities = new float[times.length][positions.length][3];

        //Go through each Layer and get information
        int layernumber = 0;
        int nodesPerLayer = ncols * nrows;
        for (File dir : directory.listFiles()) {
            if (!dir.isDirectory()) {
                continue;
            }
            //Calculate nodePositions
            float elevation = -1;
            String[] nameparts = dir.getName().split("_");
            for (String n : nameparts) {
                if (n.contains("m") && !n.contains("min")) {
                    elevation = Float.parseFloat(n.substring(0, n.indexOf("m")));
                }
            }
            if (elevation < 0) {
                System.err.println("Could not extract Elevation of level from directoryname '" + dir.getName() + "'.");
                continue;
            }
            //Create positions of nodes for this layer
            for (int i = 0; i < nrows; i++) {
                for (int j = 0; j < ncols; j++) {
                    int index = layernumber * nodesPerLayer + i * ncols + j;
                    positions[index] = new Coordinate(xllcorner + j * cellsize, yulcorner - i * cellsize, elevation);
                }
            }
            //Read velocity values for this layer
            ufiles.clear();
            vfiles.clear();
            for (File f : dir.listFiles()) {
                if (f.getName().startsWith("uz")) {
                    ufiles.add(f);
                } else if (f.getName().startsWith("vz")) {
                    vfiles.add(f);
                }
            }

            if (ufiles.size() != vfiles.size()) {
                System.err.println("ufiles(" + ufiles.size() + ") and vfiles(" + vfiles.size() + ") do not have the same length. -> break");
                return null;
            }

            for (File ufile : ufiles) {
                Integer step = Integer.parseInt(ufile.getName().substring(2, ufile.getName().length() - 4));
                //Find temporal index of this layer.
                int tindex = timesteps.indexOf(step);
                int cellindex = layernumber * nodesPerLayer;
                float[][] field = loadField(ufile);
                for (int i = 0; i < field.length; i++) {
                    for (int j = 0; j < field[0].length; j++) {
//                        System.out.println(i+","+j+": "+field[i][j]);
                        velocities[tindex][cellindex][0] = field[i][j];
                        cellindex++;
                    }
                }
            }
            for (File vfile : vfiles) {
                Integer step = Integer.parseInt(vfile.getName().substring(2, vfile.getName().length() - 4));
                //Find temporal index of this layer.
                int tindex = timesteps.indexOf(step);
                int cellindex = layernumber * nodesPerLayer;
                float[][] field = loadField(vfile);
                for (int i = 0; i < field.length; i++) {
                    for (int j = 0; j < field[0].length; j++) {
                        velocities[tindex][cellindex][1] = field[i][j];
                        velocities[tindex][cellindex][2] = 0; //No z-directed velocity in KLAM.
                        cellindex++;
                    }
                }
            }

            layernumber++;

        }

//        System.out.println("Information found in Files:");
//        for (File vfile : ufiles) {
//            System.out.println(vfile);
//        }
//        for (File vfile : vfiles) {
//            System.out.println(vfile);
//        }
//Create Raster 
        System.out.println(positions.length + " Positions");
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == null) {
                System.out.println(" index " + i + " is null");
            }

        }
//        for (int i = 0; i < velocities.length; i++) {
//            for (int j = 0; j < velocities[0].length; j++) {
//                for (int k = 0; k < 3; k++) {
//                    if(velocities[i][j][k]!=0)System.out.println(i+","+j+": "+velocities[i][j][0]+","+velocities[i][j][1]+","+velocities[i][j][2]);
//                    
//                }
//                
//            }
//            
//        }

        return new Domain3D(positions, velocities, null, new GeoTools("EPSG:4326", "EPSG:25832",StartParameters.JTS_WGS84_LONGITUDE_FIRST), timeCatalog);
    }

    public static float[][] loadField(File file) throws IOException {

        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        int linennumber = 0;
        br.mark(1);
        String line = "";//br.readLine();
        //Header lesen
        int ncols = -1, nrows = -1, xllcorner = -1, yllcorner = -1, rastersize = -1;
        float cellsize = -1;
        String nodatavalue = "";
        while (br.ready()) {
            linennumber++;
            line = br.readLine();
            if (line.startsWith("ncols")) {
                ncols = Integer.parseInt(line.replaceAll(" ", "").substring(5));
            } else if (line.startsWith("* Anzahl Spalten")) {
                ncols = Integer.parseInt(line.replaceAll(" ", "").substring(14));
            } else if (line.startsWith("nrows")) {
                nrows = Integer.parseInt(line.replaceAll(" ", "").substring(5));
            } else if (line.startsWith("* Anzahl Zeilen")) {
                nrows = Integer.parseInt(line.replaceAll(" ", "").substring(13));
            } else if (line.startsWith("xllcorner")) {
                xllcorner = Integer.parseInt(line.replaceAll(" ", "").substring(9));
            } else if (line.startsWith("yllcorner")) {
                yllcorner = Integer.parseInt(line.replaceAll(" ", "").substring(9));
            } else if (line.startsWith("cellsize")) {
                cellsize = Integer.parseInt(line.replaceAll(" ", "").substring(8));
            } else if (line.startsWith("* Zellgr")) {
//                System.out.println(line);
                String format = line.replaceAll(" ", "").substring(10);
                if (format.contains("m")) {
                    format = format.substring(0, format.indexOf("m"));

                }
                cellsize = Float.parseFloat(format);

            } else if (line.startsWith("NODATA")) {
                nodatavalue = line.substring(12).replaceAll(" ", "");
                rastersize = nodatavalue.length();
                //Usually the last value before REAL data value.
                break;
            } else if (line.startsWith("* Zeichen pro Wert")) {
                rastersize = Integer.parseInt(line.substring(18).replaceAll(" ", ""));
                //Usually the last value before REAL data value.

            } else if (line.startsWith("* HWlinksunten")) {
                //Last header entry in german version
                break;
            }

        }
        if (verbose) {
            System.out.println("ncols=" + ncols);
            System.out.println("nrows=" + nrows);
            System.out.println("x=" + xllcorner);
            System.out.println("y=" + yllcorner);
            System.out.println("size=" + cellsize);
            System.out.println("nodata='" + nodatavalue + "'");
        }

        //initialize return field size;
        float[][] values = new float[nrows][ncols];
        int rownumber = 0;
        //Read values
        try {
            while (br.ready()) {
                linennumber++;
                line = br.readLine();
                if (line.length() < rastersize) {
                    break; //Line is not long enough. seems to be after the last value data line.
                }
                for (int i = 0; i < ncols; i++) {
                    String str = line.substring(i * rastersize, (i + 1) * rastersize);
                    if (!str.equals(nodatavalue)) {
                        values[rownumber][i] = Float.parseFloat(str);
                    }
                }
                rownumber++;
            }
        } catch (IOException iOException) {
            iOException.printStackTrace();
        } catch (NumberFormatException numberFormatException) {
            numberFormatException.printStackTrace();
        } catch (Exception exc) {
            System.err.println("File: " + file.getAbsolutePath() + "\n Line:'" + line + "'");
            exc.printStackTrace();
        }
//        for (int i = 0; i < values.length; i++) {
//            for (int j = 0; j < values[0].length; j++) {
//                System.out.println(i+","+j+": "+values[i][j]);
//            }
//            
//        }

        return values;
    }

    public boolean writeConcentrations(File file, Domain3D domain, float[][] values) throws IOException {
        FileWriter fw = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fw);
        StringBuffer str = new StringBuffer(values[0].length * 5);
        bw.write("*Abgesetzte Partikelanzahl am Ende des Simulationslaufs\n"
                + "* Anzahl Spalten        " + fixedLengthString(values[0].length + "", 5) + "\n"
                + "* Anzahl Zeilen         " + fixedLengthString(values.length + "", 5) + "\n"
                + "* Zeichen pro Wert          5\n"
                + "* Zellgröße             10.00 m\n"
                + "* RWlinksunten         593000 m\n"
                + "* HWlinksunten        5791731 m");
        bw.newLine();
        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[y].length; x++) {
                bw.append(fixedLengthString(values[x][y] + "", 5));
            }
            bw.newLine();
            bw.flush();
        }
        return true;
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }

//    public static void main1(String[] args) throws Exception {
//
//        File fileRootDirectory = new File("xxxx");
//
//        Domain3D wind = KLAM_IO.loadDomain(fileRootDirectory, 593000, 5791731);
//        GeoTools gt = new GeoTools("EPSG:4326", "EPSG:25832",StartParameters.JTS_WGS84_LONGITUDE_FIRST);
//        System.out.println("Windfield loaded loaded");
//        SimpleMapViewerFrame frame = new SimpleMapViewerFrame();
//        frame.setVisible(true);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setBounds(200, 100, 500, 400);
//        ColorHolder chIn = new ColorHolder(Color.green, "inflow");
//        ColorHolder chOut = new ColorHolder(Color.red, "outflow");
//        ColorHolder chTrace1 = new ColorHolder(Color.black, "Trace 1m");
//        ColorHolder chTrace5 = new ColorHolder(Color.white, "Trace 5m");
//        ColorHolder chp = new ColorHolder(Color.blue, "Years");
//        String layerTrace1 = "TRACE1";
//        String layerTrace2 = "TRACE2";
//        String layerPosition = "POS";
//
//        for (int i = 0; i < wind.position.length / 3; i++) {
//            Coordinate pos = wind.position[i];
//
//            Coordinate ll = gt.toGlobal(pos);
////            double temp=ll.y;
////            ll.y=ll.x;
////            ll.x=temp;
////            System.out.println("pos:" + pos + "\t ll:" + ll);
//            NodePainting np = new NodePainting(i, ll, chOut);
//            frame.getMapViewer().addPaintInfoToLayer(layerPosition, np);
//        }
//
//        DecimalFormat df = new DecimalFormat("0.##");
//
//        /**
//         * Define Path start points ############################################
//         */
//        LinkedList<Coordinate> outflows = new LinkedList<>();
//        for (int i = 1; i < 80; i++) {
//            int index = i * 98 - 1;
//            Coordinate c = (Coordinate) wind.position[index].clone();
//            if (c.z < 3) {
//                outflows.add(c);
//            }
//            index = i * 98 - 1 + 50;
//            c = (Coordinate) wind.position[index].clone();
//            if (c.z < 3) {
//                outflows.add(c);
//            }
//            index = i * 98 - 1 + 40;
//            c = (Coordinate) wind.position[index].clone();
//            if (c.z < 3) {
//                outflows.add(c);
//            }
////            index = i * 98 - 1 + 30;
////            c = (Coordinate) wind.position[index].clone();
////            if (c.z < 3) {
////                outflows.add(c);
////            }
//        }
//        //All 1m also as 5 m particles
//        ArrayList new5 = new ArrayList(outflows.size());
//        for (Coordinate outflow : outflows) {
//            Coordinate ele5 = new Coordinate(outflow);
//            ele5.z = 5;
//            new5.add(ele5);
//        }
//        outflows.addAll(new5);
//
//        frame.getMapViewer().recomputeLegend();
//        frame.getMapViewer().zoomToFitLayer();
//        double dt = 1000;               //eine Sekunde
//        double intervallNodes = 1000 * 60; //eine Minute
//        double maxT = 1000 * 60 * 60 * 2; //2 stunden
//        int objectid = 0;
//        int historyId = 0;
//
//        for (Coordinate outflow : outflows) {
//            long start = System.currentTimeMillis();
//            //Find index to this coordinate
//            long time = 900000;
//            Coordinate c = new Coordinate(outflow);
//            boolean is1m = c.z < 4;
//            LinkedList<Coordinate> history = new LinkedList<>();
//            history.add(gt.toGlobal(c));
//            int historyIndex = 0;
//            int loopindex = 0;
//            double distance = 0;
//            float[] velocity = null;
//            while (time < maxT) {
//
//                try {
//                    velocity = wind.getVelocity(c, time); //timeindex 0: is stationary flowfield
//                    c.x += velocity[0] * dt / 100.;
//                    c.y += velocity[1] * dt / 100.;
//                    c.z += velocity[2] * dt / 100.;
//                    distance += Math.sqrt((velocity[0] * dt) * ((velocity[0] * dt)) + (velocity[1] * dt) * ((velocity[1] * dt)) + (velocity[2] * dt) * ((velocity[2] * dt)));
//                } catch (Exception e) {
//                    System.err.println(e.getLocalizedMessage());
//                    break;
//                }
//                if (c.z < wind.minZ) {
//                    c.z = wind.minZ;
//                    System.out.println("  below min ");
//                }
//                if (c.z > wind.maxZ) {
//                    c.z = wind.maxZ;
//                    System.out.println("  above max");
//                }
//                time += dt;
//                loopindex++;
//                history.add(gt.toGlobal(c));
////                if (loopindex % 100 == 0) {
////                    int index = wind.getNearestCoordinateIndex(c);
////                    if (index < 0) {
////                        System.out.println("no near coordinate found for " + c);
////                        break;
////                    }
//////                    System.out.println("nearest coordinate= "+index+"   timeindex: "+wind.time.getTimeIndexDouble(time));
////                    System.out.println(objectid + ") " + loopindex + " \tt:" + df.format(time / (60000f)) + "min\t dist: " + df.format(distance) + "m. \t v:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t PosIndex:" + index + ", TimeIndex: " + wind.time.getTimeIndexDouble(time));
//////                    viewer.recalculateShapes();
//////                    viewer.repaint();
////                }
////                if (((int) (time / intervallNodes)) > historyIndex) {
////                    frame.getMapViewer().addPaintInfoToLayer("Particle", new NodePainting(historyId++, gt.toGlobal(c), chp));
////                    historyIndex = (int) (time / intervallNodes);
////                }
//
//            }
//            System.out.println(objectid + ") " + loopindex + " \tt:" + df.format(time / (3600 * 24 * 365)) + "a\t dist: " + (int) distance + "m. \tv:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t Pos:" + c);
//
//            if (is1m) {
//                LinePainting lp = new LinePainting(objectid++, history.toArray(new Coordinate[history.size()]), chTrace1);
//
//                frame.getMapViewer().addPaintInfoToLayer(layerTrace1, lp);
//            } else {
//                LinePainting lp = new LinePainting(objectid++, history.toArray(new Coordinate[history.size()]), chTrace5);
//
//                frame.getMapViewer().addPaintInfoToLayer(layerTrace2, lp);
//            }
//            frame.getMapViewer().recalculateShapes();
//            frame.getMapViewer().repaint();
//
//        }
//
//    }

}
