package io.extran;

import com.vividsolutions.jts.geom.Coordinate;
import control.StartParameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.topology.profile.CircularProfile;
import model.topology.Connection_Manhole_Pipe;
import model.GeoTools;
import model.surface.Surface;
import model.topology.Capacity;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;

/**
 * File-Load class to Build up a Network model from CSV-Files
 *
 * @author saemann
 */
public class CSV_IO {

    public static Network loadNetwork(File directory) throws FileNotFoundException {
        if (!directory.isDirectory()) {
            if (directory.getName().toLowerCase().endsWith(".txt")) {
                directory = directory.getParentFile();
                return loadNetwork(directory);
            }
            throw new IllegalArgumentException("File '" + directory.getAbsolutePath() + "' is not a directory.");
        }
        File pipesFile = new File(directory.getAbsolutePath() + "\\ROHR.txt");
        if (!pipesFile.exists()) {
            throw new FileNotFoundException("File '" + pipesFile.getAbsolutePath() + "' not found.");
        }
        File manholeFile = new File(directory.getAbsolutePath() + "\\Schacht.txt");
        if (!manholeFile.exists()) {
            throw new FileNotFoundException("File '" + manholeFile.getAbsolutePath() + "' not found.");
        }
        return CSV_IO.loadNetwork(pipesFile, manholeFile, null);
    }

    public static Network loadNetwork(File pipesFile, File manholeFile, GeoTools geoTools) {
        //Containers in which the Manholes and pipes will be stored
        HashMap<String, Manhole> smap = new HashMap<>();
        HashSet<Pipe> pipes_drain = new HashSet<>();

        HashSet<Pipe> pipes_sewer = new HashSet<>();
        HashSet<String> usedNodes_drain = new HashSet<>();
        HashSet<String> usedNodes_sewer = new HashSet<>();

        try {
            //Initialize Geotools to convert from Gauss-Krueger-Coordinates to 
            // global Position WGS84 system
            GeoTools gt = geoTools;
            if (gt == null) {
                gt = new GeoTools("EPSG:4326", "EPSG:31467",StartParameters.JTS_WGS84_LONGITUDE_FIRST);// WGS84(lat,lon) <-> Gauss Krüger Zone 3(North,East)
            }
            //Initialize Filereader manholes
            FileReader fr = new FileReader(manholeFile);
            BufferedReader br = new BufferedReader(fr);
//Count number of manholes
            int count_manholes = 0;
            while (br.ready()) {
                br.readLine();
                count_manholes++;
            }
            //Initialize the NodeMap with the expected size
            smap = new HashMap<>(count_manholes);
            //Reset the FileReader and start from the beginning to build specific
            //manhole-objects
            br.close();
            fr.close();

            fr = new FileReader(manholeFile);
            br = new BufferedReader(fr);
            String line = br.readLine();//FirstLine contains Header Information;

            String[] values = line.split(",");
            int columnCount = values.length;
            //Definition of columns where the data is stored
            int colName = -1, colCoverHeight = -1, colSurfaceHeight = -1, colSoleHeight = -1, colWaterType = -1,
                    colPressure = -1, colX = -1, colY = -1, colStreet = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i].contains("Name")) {
                    colName = i;
                } else if (values[i].contains("Deckelh")) {
                    colCoverHeight = i;
                } else if (values[i].contains("ndeh")) {
                    colSurfaceHeight = i;
                } else if (values[i].contains("Sohlh")) {
                    colSoleHeight = i;
                } else if (values[i].contains("Kanalart")) {
                    colWaterType = i;
                } else if (values[i].contains("Druckdichter")) {
                    colPressure = i;
                } else if (values[i].contains("X-Koordinate")) {
                    colX = i;
                } else if (values[i].contains("Y-Koordinate")) {
                    colY = i;
                } else if (values[i].contains("Stra")) {
                    colStreet = i;
                }
            }
            CircularProfile DN1200 = new CircularProfile(1.2);
            int linecount = 1;
            while (br.ready()) {
                linecount++;
                line = br.readLine();
                line = line.replaceAll("\"", "");
                values = line.split(",", -1);//-1 will give empty Strings instead of skipping an entry in case of no signs between two komma
                if (values.length != columnCount) {
                    System.err.println("Manholes: Line " + linecount + " has not " + columnCount + " columns. (only " + values.length + ")");
                }

                double xe = Double.parseDouble(values[colX].replaceFirst("\\.", "").replaceFirst("\\.", ""));
                double yn = Double.parseDouble(values[colY].replaceFirst("\\.", "").replaceFirst("\\.", ""));
                Coordinate c = new Coordinate(yn, xe);
                c = gt.toGlobal(c);
                Position position = new Position(c.x, c.y, xe, yn);
                Manhole m = new Manhole(position, values[colName], DN1200);
//Set height information
                m.setSurface_height(Float.parseFloat(values[colSurfaceHeight]));
                m.setSole_height(Float.parseFloat(values[colSoleHeight]));
                m.setTop_height(Float.parseFloat(values[colCoverHeight]));

                //Read additional information about this manhole
                m.setStreet_name(values[colStreet]);
                if (!values[colPressure].equals("Nein")) {
                    m.setPressure_save_cover(true);
                    System.out.println("kein Inlet über deckel für " + m.getName());
                } else {

                    Connection_Manhole_Pipe topInlet = new Connection_Manhole_Pipe(position, m.getTop_height());
//                    topInlet.flow_m2p = 77;
//                    topInlet.velocity_m2p = 1;
                    topInlet.setManhole(m);
                    topInlet.name = "Deckel";
                    m.addConnection(topInlet);
                    if (m.getName().equals("WE07N146")) {
                        System.out.println("Connection hinzugefügt. jetzt:" + m.getConnections().length);
                    }
                }

                //if the top is NOT SEALED, set a TopInletConnection
                if (values[colPressure].contains("Nein")) {

                }
                //add this manhole to the network
                smap.put(m.getName(), m);
            }
            br.close();
            fr.close();
            //End reading manhole file

            ///Read Pipe information
            fr = new FileReader(pipesFile);
            br = new BufferedReader(fr);
            line = br.readLine();//FirstLine contains Header Information;
            values = line.split(",");
            int colProfilType = -1, colHeight = -1, colWidth = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i].contains("Profiltyp")) {
                    colProfilType = i;
                } else if (values[i].contains("Profilh")) {
                    colHeight = i;
                } else if (values[i].contains("Profilbreite")) {
                    colWidth = i;
                }

            }
            System.out.println("Profiltyp at " + colProfilType);
            System.out.println("Profilhöhe at" + colHeight);
            HashMap<String, CircularProfile> profiles = new HashMap<>(20);
            linecount = 1;
            while (br.ready()) {
                line = br.readLine();
                linecount++;
                //clear all quote signs
                // line = line.replaceAll("\"", "");
                values = line.split("\",\"", -1);//-1 will give empty Strings instead of skipping an entry in case of no signs between two komma

                //Search for the right Profile
                String profileKey = values[colProfilType] + values[colHeight];
                CircularProfile profile = profiles.get(profileKey);
                if (profile == null) {
                    //Create new Profile if not yet in map
                    try {
                        profile = new CircularProfile(Double.parseDouble(values[colHeight].replaceAll("\\.", "")) * 0.001);

                    } catch (NumberFormatException numberFormatException) {
                        System.err.println("Line " + linecount + ". Can not read number from column " + colHeight + ". String is '" + values[colHeight] + "'. Profile was " + values[colProfilType]);
                    }
                    profiles.put(profileKey, profile);
                }

                //Search for start and end points
                Manhole up = smap.get(values[2]);
                Manhole low = smap.get(values[3]);
                if (up == null) {
                    System.err.println("upper Manhole '" + values[2] + " was not found.");
                }
                if (low == null) {
                    System.err.println("lower Manhole '" + values[3] + " was not found.");
                }

                //Define the Position of Connections
                Connection_Manhole_Pipe upper = new Connection_Manhole_Pipe(up.getPosition(), Float.parseFloat(values[4]));
                Connection_Manhole_Pipe lower = new Connection_Manhole_Pipe(low.getPosition(), Float.parseFloat(values[5]));

                //Swap up/down in order to get a pipe that goes from higher to lower position
//                if(upper.getHeight()<lower.getHeight()){
//                    Connection_Manhole_Pipe temp=lower;
//                    lower=upper;
//                    upper=temp;
//                }
                //Create Pipe to connect upper and lower manhole
                Pipe p = new Pipe(upper, lower, profile);

                //Connect Manholes and Pipe via Connections
                up.addConnection(upper);

                low.addConnection(lower);

                //Read additional pipe information
                p.setName(values[1]);

                //Get measured length
                p.setLength(Float.parseFloat(values[6]));

                //Set Watertype
                String type = values[9];
                if (type.equals("Schmutzwasser")) {
                    p.setWaterType(Capacity.SEWER_TYPE.SEWER);
                } else if (type.equals("Regenwasser")) {
                    p.setWaterType(Capacity.SEWER_TYPE.DRAIN);
                } else if (type.equals("Mischwasser")) {
                    p.setWaterType(Capacity.SEWER_TYPE.MIX);
                } else {
                    System.out.println("Unknown Watertype '" + type + "' in line " + linecount);
                }
                if (p.getWaterType() == Capacity.SEWER_TYPE.DRAIN) {
                    usedNodes_drain.add(up.getName());
                    usedNodes_drain.add(low.getName());
                    pipes_drain.add(p);
                } else {
                    usedNodes_sewer.add(up.getName());
                    usedNodes_sewer.add(low.getName());
                    pipes_sewer.add(p);
                }

                //Add the new Pipe to the Network
//                w.putTag("manmade", "pipeline");
//                w.putTag("name", values[1]);
//                w.putTag("sohlhoehe_oben", values[4]);
//                w.putTag("sohlhoehe_unten", values[5]);
//                w.putTag("length", values[6]);
//                w.putTag("length_calculated", values[7]);
//                w.putTag("decline", values[8]);
//                w.putTag("type", values[9]);
//                w.putTag("exists", (values[10].equals("Vorhanden") ? "true" : "false"));
//                w.putTag("street", values[11]);
//                w.putTag("profile", values[12]);
//                w.putTag("profile_height", values[14]);
//                w.putTag("profile_width", values[15]);
//                w.putTag("rauheit", values[21]);
//                ways.set(w);
            }
            br.close();
            fr.close();

        } catch (IOException ex) {
            Logger.getLogger(CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(CSV_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
        HashSet<Manhole> manholes = new HashSet<>(usedNodes_drain.size());
        for (String name : usedNodes_drain) {
            manholes.add(smap.get(name));
        }
        HashSet<Manhole> manholes_sewer = new HashSet<>(usedNodes_sewer.size());
        for (String name : usedNodes_sewer) {
            manholes_sewer.add(smap.get(name));
        }
        pipes_drain.addAll(pipes_sewer);
        manholes.addAll(manholes_sewer);
        Network network = new Network(pipes_drain, manholes);
        System.out.println("Network contains " + network.getManholes().size() + " manholes & " + network.getPipes().size() + " pipes.");
        return network;
    }

    public static GregorianCalendar parseDate(String datestring) {
        GregorianCalendar cal = new GregorianCalendar();
        datestring = datestring.replaceAll("\"", "");
        String[] d2 = datestring.split(" ");
        // Datum
        String[] date = d2[0].split("\\.");
        cal.set(GregorianCalendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
        cal.set(GregorianCalendar.MONTH, Integer.parseInt(date[1]) - 1);
        cal.set(GregorianCalendar.YEAR, Integer.parseInt(date[2]));
        // Zeit
        String[] time = d2[1].split(":");
        cal.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        cal.set(GregorianCalendar.MINUTE, Integer.parseInt(time[1]));
        cal.set(GregorianCalendar.SECOND, Integer.parseInt(time[2]));
        cal.set(GregorianCalendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Method to read Waterlevels stored in GDAL generated CSVs.
     *
     * @param surf
     * @param csvFile
     */
    public static void readTriangleWaterlevels(Surface surf, File csvFile) throws FileNotFoundException, IOException, Exception {
        surf.loadingWaterlevels = true;
        try ( //Read header to detect timesteps
                FileReader fr = new FileReader(csvFile); BufferedReader br = new BufferedReader(fr)) {
            String line = "";
            float[][] waterlevels = null;
            double[] maxWaterlevels = null;
            int wl0index = 5;
            int zIndex = 1;
            int wlmaxIndex = 4;
            String[] split;
            boolean filteredSurface = false; //if true: IDs of triangles have to be mapped to the surface IDs.
            int numberOfTimes = 0;
            String splitchar = ",";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("ID")) {
                    if (line.contains(";")) {
                        splitchar = ";";
                    } else {
                        splitchar = ",";
                    }
                    split = line.split(splitchar);
                    //reached header of simulation results.
                    //read number of timeresults

                    for (int i = 0; i < split.length; i++) {
                        if (split[i].equals("WL_0")) {
                            wl0index = i;
                        }
                        if (split[i].startsWith("WL_")) {
                            numberOfTimes++;
                        }
                        if (split[i].toLowerCase().equals("z")) {
                            zIndex = i;
                        }
                        if (split[i].equals("WLevelMax")) {
                            wlmaxIndex = i;
                        }

                    }
                    if (surf.mapIndizes != null && !surf.mapIndizes.isEmpty()) {
                        //needs special treatment for ID mapping
                        waterlevels = new float[surf.mapIndizes.size()][numberOfTimes];
                        maxWaterlevels = new double[surf.mapIndizes.size()];
                    } else {
                        //every triangle is on this surface:
                        waterlevels = new float[surf.triangleNodes.length][numberOfTimes];
                        maxWaterlevels = new double[surf.triangleNodes.length];
                    }
                    break;
                }
            }
            if (waterlevels == null) {
                throw new Exception("Number of 'WL_*' could not be found. Waterlevels can not be initialized.");
            }

            //Start going through all lines.
            while (br.ready()) {
                line = br.readLine();
                split = line.split(splitchar);
                int id = Integer.parseInt(split[0]);
                if (filteredSurface) {
                    if (!surf.mapIndizes.containsKey(id)) {
                        //This triangle is not part of the Surface. 
                        continue;
                    }
                    id = surf.mapIndizes.get(id); //Map triangle to surface's triangle id.
                }
                float z = Float.parseFloat(split[zIndex]);
                maxWaterlevels[id] = Float.parseFloat(split[wlmaxIndex]);
                surf.getTriangleMids()[id][2] = z;
                for (int i = 0; i < numberOfTimes; i++) {
                    waterlevels[id][i] = Float.parseFloat(split[i + wl0index]);
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("   LoadingThread is interrupted -> break CSV_IO");
                surf.loadingWaterlevels = false;
                return;
            }
            surf.setWaterlevels(waterlevels);
            surf.setMaxWaterLevels(maxWaterlevels);
            surf.loadingWaterlevels = false;
        }
    }
    
    

}
