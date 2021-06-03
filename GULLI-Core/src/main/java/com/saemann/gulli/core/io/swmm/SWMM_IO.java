/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package com.saemann.gulli.core.io.swmm;

import com.saemann.gulli.core.control.StartParameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineManhole;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.SWMMNetwork;
import com.saemann.gulli.core.model.topology.StorageVolume;
import com.saemann.gulli.core.model.topology.catchment.Catchment;
import com.saemann.gulli.core.model.topology.graph.Pair;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.gulli.core.model.topology.profile.Profile;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Importing SWMM inpout files (*.INP) and converting it to Networkstructure.
 *
 * @author saemann
 */
public class SWMM_IO {

    public static boolean verbose = false;
    /**
     * Orient the start and end of pipes by slope.
     */
    public static boolean orientGravity = false;

    public static boolean swapCoordinates = false;

    private enum FLOW_UNITS {

        LPS, CMS
    };

    public FLOW_UNITS flowunits = FLOW_UNITS.CMS;
    public GregorianCalendar calStart = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    public GregorianCalendar calEnd = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    public HashMap<String, Raingage> raingages;
    public HashMap<String, Subcatchment> subcatchments;
    public HashMap<String, SubArea> subareas;

    public HashMap<String, Junction> junctions;
    public HashMap<String, Outfall> outfalls;
    public HashMap<String, Divider> dividers;
    public HashMap<String, Storage> storages;
    public HashMap<String, Conduit> conduits;
    public HashMap<String, Orifice> orifices;
    public HashMap<String, Weir> weirs;
    public HashMap<String, Outlet> outlets;
    public HashMap<String, DryWeatherFlow> dryWeatherFlows;
    public HashMap<String, Coordinate> coordinates;
    public HashMap<String, List<Coordinate>> polygons;
    public HashMap<String, Profile> profiles;
    public HashMap<String, XSection> xSections;
    public HashMap<String, Timeseries> timeseries;
    public HashMap<String, String> tags;
    public HashMap<String, Infiltration> infiltrations;

    /**
     * Creates a GULLI Network from a SWMM *.inp file
     *
     * @param file *.inp file with definition of SWMM networlk topology
     * @return Network for GULLI
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FactoryException
     */
    public static SWMMNetwork readNetwork(File file) throws FileNotFoundException, IOException, FactoryException {
        SWMM_IO swmm = new SWMM_IO();
        swmm.readFile(file);
        return swmm.finishNetwork();
    }
    
    /**
     * Creates a GULLI Network from a SWMM *.inp file
     *
     * @param file *.inp file with definition of SWMM networlk topology
     * @param crs CoordinateReferenceString e.g. "EPSG:25832"
     * @return Network for GULLI
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FactoryException
     */
    public static SWMMNetwork readNetwork(File file, String crs) throws FileNotFoundException, IOException, FactoryException {
        SWMM_IO swmm = new SWMM_IO();
        swmm.readFile(file);
        return swmm.finishNetwork(crs);
    }

    /**
     * Decode a *.inp file with SWMM topology information and stores the
     * findings in this object to have access to the information. A Network can
     * be created calling the "finischNetwork() method.
     *
     * @param file INP file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FactoryException
     */
    public void readFile(File file) throws FileNotFoundException, IOException, FactoryException {
        readFile(file, Charset.defaultCharset());
    }

    public void readFile(File file, Charset encoding) throws FileNotFoundException, IOException, FactoryException {
        try (//        FileReader fr = new FileReader(file);
                FileInputStream fis = new FileInputStream(file); InputStreamReader isr = new InputStreamReader(fis, encoding); BufferedReader br = new BufferedReader(isr)) {
            String line;
            int linecount = -1;
            try {
                while (br.ready()) {
                    line = br.readLine();
                    linecount++;
                    if (line.toUpperCase().equals("[OPTIONS]")) {
                        readOptions(br);
                    } else if (line.toUpperCase().equals("[RAINGAGES]")) {
                        readRaingages(br);
                        if (verbose) {
                            System.out.println(raingages.size() + " raingages read:");
                        }
                    } else if (line.toUpperCase().equals("[SUBCATCHMENTS]")) {
                        readSubcatchments(br);
                        if (verbose) {
                            System.out.println(subcatchments.size() + " subcatchments read");
                        }
                    } else if (line.toUpperCase().equals("[JUNCTIONS]")) {
                        readJunctions(br);
                        if (verbose) {
                            System.out.println(junctions.size() + " junctions read");
                        }
                    } else if (line.toUpperCase().equals("[OUTFALLS]")) {
                        readOutfalls(br);
                        if (verbose) {
                            System.out.println(outfalls.size() + " outfalls read");
                        }
                    } else if (line.toUpperCase().equals("[DIVIDERS]")) {
                        readDividers(br);
                        if (verbose) {
                            System.out.println(dividers.size() + " dividers read");
                        }
                    } else if (line.toUpperCase().equals("[STORAGE]")) {
                        readStorages(br);
                        if (verbose) {
                            System.out.println(storages.size() + " storages read");
                        }
                    } else if (line.toUpperCase().equals("[CONDUITS]")) {
                        readConduits(br);
                        if (verbose) {
                            System.out.println(conduits.size() + " conduits read");
                        }
                    } else if (line.toUpperCase().equals("[ORIFICES]")) {
                        readOrifices(br);
                        if (verbose) {
                            System.out.println(orifices.size() + " orifices read");
                        }
                    } else if (line.toUpperCase().equals("[WEIRS]")) {
                        readWeirs(br);
                        if (verbose) {
                            System.out.println(weirs.size() + " weirs read");
                        }
                    } else if (line.toUpperCase().equals("[OUTLETS]")) {
                        readOutlets(br);
                        if (verbose) {
                            System.out.println(outlets.size() + " outlets read");
                        }
                    } else if (line.toUpperCase().equals("[XSECTIONS]")) {

                        readXSections(br);
                        createProfiles();
//                        readProfiles(br);
                        if (verbose) {
                            System.out.println(profiles.size() + " profiles + " + xSections.size() + " xsections read");
                        }
                    } else if (line.toUpperCase().equals("[TIMESERIES]")) {
                        readTimeseries(br);
                        if (raingages != null) {
                            if (verbose) {
                                System.out.println(raingages.size() + " timeseries read");
                            }
                        }
                    } else if (line.toUpperCase().equals("[TAGS]")) {
                        readTags(br);
                        if (tags != null) {
                            if (verbose) {
                                System.out.println(tags.size() + " Tags read");
                            }
                        }
                    } else if (line.toUpperCase().equals("[COORDINATES]")) {
                        readCoordinates(br);
                        if (verbose) {
                            System.out.println(coordinates.size() + " coordinates read");
                        }
                    } else if (line.toUpperCase().equals("[POLYGONS]")) {
                        readPolygons(br);
                        if (verbose) {
                            System.out.println(polygons.size() + " Polygons read");
                        }
                    } else if (line.toUpperCase().equals("[INFILTRATION]")) {
                        readInfiltrations(br);
                        if (verbose) {
                            System.out.println(infiltrations.size() + " infiltrations read");
                        }
                    } else if (line.toUpperCase().equals("[SUBAREAS]")) {
                        readSubAreas(br);
                        if (verbose) {
                            System.out.println(subareas.size() + " subareas read");
                        }
                    } else if (line.toUpperCase().equals("[DWF]")) {
                        readDryWeaterFlow(br);
                        if (verbose) {
                            System.out.println(dryWeatherFlows.size() + " dry-weather-flows read");
                        }
                    }
                }
            } catch (Exception exception) {
                System.err.println("Exception in line " + linecount);
                throw exception;
            }
        }
    }

    public SWMMNetwork finishNetwork() throws FactoryException {
        return finishNetwork(null);
    }

    /**
     * Create a GULLI Network from previously read in INP SWMM definition
     * (readNetwork(file) method).
     *
     * @return
     * @throws FactoryException
     */
    public SWMMNetwork finishNetwork(String epsgCodeUTM) throws FactoryException {
        if (junctions == null) {
            junctions = new HashMap<>(0);
        }
        if (storages == null) {
            storages = new HashMap<>(0);
        }

        HashMap<String, Manhole> nodes = new HashMap<>(junctions.size() + storages.size());
        HashMap<String, Pipe> pipes;
        if (conduits == null) {
            pipes = new HashMap<>();
        } else {
            pipes = new HashMap<>(conduits.size());
        }
        CRSAuthorityFactory af = CRS.getAuthorityFactory(StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        CoordinateReferenceSystem wgs84CRS = null;
        CoordinateReferenceSystem utmCRS = null, utm4CRS = null;
        CRS.cleanupThreadLocals();
        //Find plausible Coordinates
        if (epsgCodeUTM == null || epsgCodeUTM.isEmpty()) {
            //Try to find a suitable epsg code
            if (coordinates != null && !coordinates.isEmpty()) {
                Coordinate c = coordinates.entrySet().iterator().next().getValue();
                if (c.x > 1000000) {
                    //Gauss Krüger
                    //Zone
                    int zone = (int) (c.x / 1000000);
                    if (verbose) {
                        System.out.println("Found input coordinates to be GK Zone " + zone + " -> EPSG:3146" + zone);
                    }
                    utmCRS = af.createCoordinateReferenceSystem("EPSG:3146" + zone);
                } else {
                    if (verbose) {
                        System.out.println("Found input coordinates to be UTM WGS84 32N -> EPSG:25832");
                    }
                    utmCRS = af.createCoordinateReferenceSystem("EPSG:25832"); //UTM WGS84 32Nord
                }
            }
        }

        if (verbose) {
            System.out.print("Creating Geospatial Transformation...");
        }
        try {
            if(epsgCodeUTM==null||epsgCodeUTM.isEmpty()){
                epsgCodeUTM="EPSG:25832";
                
                System.out.println("use fallback UTM CRS: "+epsgCodeUTM);
            }else{
                System.out.println("CRS: "+epsgCodeUTM);
            }
            utmCRS = af.createCoordinateReferenceSystem(epsgCodeUTM);
            wgs84CRS = af.createCoordinateReferenceSystem("EPSG:4326");//CRS.decode("EPSG:4326"); //WGS84
//            utm3CRS = CRS.decode("EPSG:31467");//DHDN / 3-degree Gauss-Kruger zone 3
//            utm4CRS = CRS.decode("EPSG:31468");//DHDN / 3-degree Gauss-Kruger zone 4
            CRS.cleanupThreadLocals();
            if (verbose) {
                System.out.println("done.");
            }
        } catch (Exception ex) {
            if (verbose) {
                System.out.println("error.");
            }
            ex.printStackTrace();
            return null;
        }

        MathTransform utm2wgs = CRS.findMathTransform(utmCRS, wgs84CRS);
//        MathTransform utm2wgs = CRS.findMathTransform(utm4CRS, wgs84CRS);
        /**
         * SCHÄCHTE
         */
        int manualID = 0;
        if (verbose) {
            System.out.println("Converting Manholes");
        }
        CircularProfile mh_profile = new CircularProfile(1.2);
        for (Map.Entry<String, Junction> p : junctions.entrySet()) {
            try {
                Coordinate utm = coordinates.get(p.getKey());
                if (utm == null) {

//                    if (p.getKey().equals("3-28902")) {
//                        utm = new Coordinate(4399829, 5793459);
//                    } else {
                    System.err.println("No Coordinate Values for Junction'" + p.getKey() + "'");
                    utm = new Coordinate(0, 0);
//                    }

                }
                Manhole mh = new Manhole(buildPosition(utm, utm2wgs/*, utm42wgs*/), p.getKey(), mh_profile);
                mh.setManualID(manualID);
                manualID++;
                mh.setTop_height((float) (p.getValue().elevation + p.getValue().maxdepth));
                mh.setSurface_height(mh.getTop_height());
                mh.setSole_height((float) (p.getValue().elevation));
                nodes.put(p.getKey(), mh);
//                mh.tags = new Tags();
//                mh.tags.put("Node", "Junction");
//                mh.tags.put("Water", (p.getKey().substring(0, 1)));
            } catch (TransformException ex) {
                Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Map.Entry<String, Outfall> p : outfalls.entrySet()) {
            try {
                Coordinate utm = coordinates.get(p.getKey());
                if (utm == null) {
                    System.err.println("No Coordinate Values for Outfall'" + p.getKey() + "'");
                    continue;
                }
                Manhole mh = new Manhole(buildPosition(utm, utm2wgs/*, utm42wgs*/), p.getKey(), mh_profile);
                mh.setManualID(manualID);
                manualID++;
                mh.setAsOutlet(true);
                mh.setName(p.getKey());
                mh.setTop_height((float) p.getValue().invert);
                mh.setSurface_height(mh.getTop_height());
                mh.setSole_height((float) (p.getValue().invert));
//                mh.tags = new Tags();
//                mh.tags.put("Node", "Outfall");
//                mh.tags.put("Water", (p.getKey().substring(0, 1)));
//                mh.tags.put("Outfall", p.getValue().type);
                nodes.put(p.getKey(), mh);
            } catch (TransformException ex) {
                Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (dividers != null) {
            for (Map.Entry<String, Divider> p : dividers.entrySet()) {
                try {
                    Coordinate utm = coordinates.get(p.getKey());
                    if (utm == null) {
                        System.err.println("No Coordinate Values for Divider'" + p.getKey() + "'");
                        continue;
                    }
                    Manhole mh = new Manhole(buildPosition(utm, utm2wgs/*, utm42wgs*/), p.getKey(), mh_profile);
                    mh.setManualID(manualID);
                    manualID++;
                    if (p.getValue().type.equals("OVERFLOW")) {
                        mh.setTop_height((float) (p.getValue().invert + p.getValue().param1));
                    } else if (p.getValue().type.equals("WEIR")) {
                        mh.setTop_height((float) (p.getValue().invert + p.getValue().param4));
                    }
                    mh.setSurface_height(mh.getTop_height());
                    mh.setSole_height((float) (p.getValue().invert));
                    nodes.put(p.getKey(), mh);
//                    mh.tags = new Tags();
//                    mh.tags.put("Node", "Divider");
//                    mh.tags.put("Water", (p.getKey().substring(0, 1)));
//                    mh.tags.put("Divider", p.getValue().type);
                } catch (TransformException ex) {
                    Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        for (Map.Entry<String, Storage> p : storages.entrySet()) {
            try {
                Coordinate utm = coordinates.get(p.getKey());
                if (utm == null) {
                    System.err.println("No Coordinate Values for Storage'" + p.getKey() + "'");
                    continue;
                }
                Manhole mh = new Manhole(buildPosition(utm, utm2wgs/*, utm42wgs*/), p.getKey(), mh_profile);
                mh.setManualID(manualID);
                manualID++;
                mh.setTop_height((float) (p.getValue().elevation + p.getValue().maxdepth));
                mh.setSurface_height(mh.getTop_height());
                mh.setSole_height((float) (p.getValue().elevation));
                nodes.put(p.getKey(), mh);
//                mh.tags = new Tags();
//                mh.tags.put("Node", "Storage");
//                mh.tags.put("Water", (p.getKey().substring(0, 1)));
            } catch (TransformException ex) {
                Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        ArrayList<Catchment> catchments = null;
        if (subcatchments != null) {
            catchments = new ArrayList<>(subcatchments.size());
            for (Map.Entry<String, Subcatchment> s : subcatchments.entrySet()) {
                Catchment catchment = new Catchment(s.getKey());
                List<Coordinate> liste = polygons.get(s.getKey());
                if (liste == null) {
                    System.err.println("Polygon for Subcatchment '" + s.getKey() + "' is NULL.");
                    continue;
                } else if (liste.size() < 1) {
                    System.err.println("Polygon for Subcatchment '" + s.getKey() + "' is empty.");
                    continue;
                } else if (liste.size() == 1) {
                    try {
                        ArrayList<GeoPosition2D> position = new ArrayList<>(liste.size());
                        position.add(buildPosition(liste.get(0), utm2wgs/*, utm42wgs*/));
                        catchment.setGeometry(position);
                    } catch (TransformException ex) {
                        Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    int i = 0;
                    ArrayList<GeoPosition2D> position = new ArrayList<>(liste.size());
                    for (Coordinate c : liste) {
                        try {
                            position.add(buildPosition(c, utm2wgs/*, utm42wgs*/));
                        } catch (TransformException ex) {
                            Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    catchment.setGeometry(position);
                }
                Manhole outlet = nodes.get(s.getValue().outletNode);
                if (outlet != null) {
                    catchment.setOutlet(outlet);
                }
                catchment.setImperviousRate(s.getValue().imperviousRatio);
                catchment.setArea(s.getValue().totalarea);
                catchment.setSlope(s.getValue().slopetotal);
//            tags.put("impervious_Rate", s.getValue().imperviousRatio + "");
//            tags.put("slope_Rate", s.getValue().slopetotal + "");
//            tags.put("width", s.getValue().width + "");
//            tags.put("outlet", s.getValue().outletNode + "");
                catchments.add(catchment);
            }

        }

//        for (Map.Entry<String, List<Coordinate>> p : polygons.entrySet()) {
//            try {
//                int i=0;
//                for (Coordinate utm : p.getValue()) {
//
//                    if (utm == null) {
//                        System.err.println("No Coordinate Values for Storage'" + p.getKey() + "'");
//                        continue;
//                    }
//                    Manhole mh = new Manhole(buildPosition(utm, utm32wgs, utm42wgs), p.getKey()+"-"+i, mh_profile);
//
//                    mh.setWaterlevel(0);
//                    nodes.put(mh.getName(), mh);
//                    mh.tags = new Tags();
//                    mh.tags.put("Type", "Polygon");
//                    i++;
//                }
//            } catch (TransformException ex) {
//                Logger.getLogger(SWMM_IO.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        /**
         * ROHRE
         */
        if (verbose) {
            System.out.println("Converting Conduits");
        }
        CircularProfile fallbackProfile = new CircularProfile(0.3);
        for (Map.Entry<String, Conduit> p : conduits.entrySet()) {
            Manhole from = nodes.get(p.getValue().fromNode);
            if (from == null) {
                System.err.println("No From-Capacity '" + p.getValue().fromNode + "' found.");
                continue;
            }
            Manhole to = nodes.get(p.getValue().toNode);
            if (to == null) {
                System.err.println("No To-Capacity '" + p.getValue().toNode + "' found.");
                continue;
            }
            Profile profile = profiles.get(p.getKey());
            if (profile == null) {
                System.err.println("Pipe '" + p.getKey() + "' has no profile information -> use fallback DN300.");
                profile = fallbackProfile;
            }
            float inHeight = (float) (from.getSole_height() + p.getValue().offsetheightIn);
            float outHeight = (float) (to.getSole_height() + p.getValue().offsetheightOut);

            if (orientGravity && inHeight < outHeight) {
                //Falschherum orientiert. -> setze Einfluss=ausfluss
                Manhole temp = from;
                from = to;
                to = temp;
                float tempHeight = inHeight;
                inHeight = outHeight;
                outHeight = tempHeight;
                if (verbose) {
                    System.out.println("Reorientate Conduit " + p.getKey());
                }
            }
            Connection_Manhole_Pipe cf = new Connection_Manhole_Pipe(from.getPosition(), inHeight);
            Connection_Manhole_Pipe ct = new Connection_Manhole_Pipe(to.getPosition(), outHeight);
            cf.setManhole(from);
            ct.setManhole(to);

            Pipe pipe = new Pipe(cf, ct, profile);
//            cf.setIsStartOfPipe(true);
            from.addConnection(cf);
//            ct.setIsStartOfPipe(false);
            to.addConnection(ct);
            pipe.setLength((float) p.getValue().length);
            pipe.setName(p.getKey());
            pipes.put(p.getKey(), pipe);
//            pipe.tags = new Tags();
//            pipe.tags.put("Pipe", "Conduit");
//            pipe.tags.put("Water", (p.getKey().substring(0, 1)));
        }
        if (orifices != null) {
            for (Map.Entry<String, Orifice> p : orifices.entrySet()) {
                Manhole from = nodes.get(p.getValue().fromNode);
                if (from == null) {
                    System.err.println("No From-Capacity '" + p.getValue().fromNode + "' found.");
                    continue;
                }
                Manhole to = nodes.get(p.getValue().toNode);
                if (to == null) {
                    System.err.println("No To-Capacity '" + p.getValue().toNode + "' found.");
                    continue;
                }
                Profile profile = profiles.get(p.getKey());
                if (profile == null) {
                    System.err.println("Orifice '" + p.getKey() + "' has no profile information -> use fallback DN300.");
                    profile = fallbackProfile;
                }
                Connection_Manhole_Pipe cf = new Connection_Manhole_Pipe(from.getPosition(), from.getSole_height());
                Connection_Manhole_Pipe ct = new Connection_Manhole_Pipe(to.getPosition(), to.getSole_height());
                cf.setManhole(from);
                ct.setManhole(to);

                Pipe pipe = new Pipe(cf, ct, profile);
                from.addConnection(cf);
                to.addConnection(ct);
                pipe.setName(p.getKey());
                pipe.setLength((float) ((Position) cf.getPosition()).distance(((Position) ct.getPosition())));
//                pipe.tags = new Tags();
//                pipe.tags.put("Pipe", "Orifice");
//                pipe.tags.put("Orifice", p.getValue().type);
//                pipe.tags.put("Water", (p.getKey().substring(0, 1)));
//                pipe.tags.put("ex height", p.getValue().height + "m");
                pipes.put(p.getKey(), pipe);
            }
        }
        if (weirs != null) {
            for (Map.Entry<String, Weir> p : weirs.entrySet()) {
                Manhole from = nodes.get(p.getValue().fromNode);
                if (from == null) {
                    System.err.println("No From-Capacity '" + p.getValue().fromNode + "' found.");
                    continue;
                }
                Manhole to = nodes.get(p.getValue().toNode);
                if (to == null) {
                    System.err.println("No To-Capacity '" + p.getValue().toNode + "' found.");
                    continue;
                }
                Profile profile = profiles.get(p.getKey());
                if (profile == null) {
                    System.err.println("Weir '" + p.getKey() + "' has no profile information -> use fallback DN300.");
                    profile = fallbackProfile;
                }

                Connection_Manhole_Pipe cf = new Connection_Manhole_Pipe(from.getPosition(), from.getSole_height());
                Connection_Manhole_Pipe ct = new Connection_Manhole_Pipe(to.getPosition(), to.getSole_height());
                cf.setManhole(from);
                ct.setManhole(to);

                Pipe pipe = new Pipe(cf, ct, profile);
                from.addConnection(cf);
                to.addConnection(ct);
                pipe.setName(p.getKey());
                pipe.setLength((float) ((Position) cf.getPosition()).distance(((Position) ct.getPosition())));
//                pipe.tags = new Tags();
//                pipe.tags.put("Pipe", "Weir");
//                pipe.tags.put("Water", (p.getKey().substring(0, 1)));
//                pipe.tags.put("ex height", p.getValue().height + "m");
                pipes.put(p.getKey(), pipe);
            }
        }
        if (outlets != null) {
            for (Map.Entry<String, Outlet> p : outlets.entrySet()) {
                Manhole from = nodes.get(p.getValue().fromNode);
                if (from == null) {
                    System.err.println("No From-Capacity '" + p.getValue().fromNode + "' found.");
                    continue;
                }
                Manhole to = nodes.get(p.getValue().toNode);
                if (to == null) {
                    System.err.println("No To-Capacity '" + p.getValue().toNode + "' found.");
                    continue;
                }
                Profile profile = profiles.get(p.getKey());
                if (profile == null) {
//                System.err.println("Outlet '" + p.getKey() + "' has none profile information -> use fallback DN300.");
                    profile = fallbackProfile;
                }
                Connection_Manhole_Pipe cf = new Connection_Manhole_Pipe(from.getPosition(), from.getSole_height());
                Connection_Manhole_Pipe ct = new Connection_Manhole_Pipe(to.getPosition(), to.getSole_height());
                cf.setManhole(from);
                ct.setManhole(to);

                Pipe pipe = new Pipe(cf, ct, profile);
                from.addConnection(cf);
                to.addConnection(ct);
                pipe.setLength((float) ((Position) cf.getPosition()).distance(((Position) ct.getPosition())));
                pipe.setName(p.getKey());
                pipes.put(p.getKey(), pipe);
            }
        }
        if (tags != null) {
            for (Map.Entry<String, String> e : tags.entrySet()) {
                Pipe p = pipes.get(e.getKey());
                if (p == null) {
                    System.err.println("No Pipe '" + e.getKey() + "' found to attach Tag '" + e.getValue() + "'.");
                    continue;
                }
//                p.tags.put("Watertype", e.getValue());
                if (e.getValue().contains("R")) {
                    p.setWaterType(Capacity.SEWER_TYPE.DRAIN);
                } else if (e.getValue().contains("S")) {
                    p.setWaterType(Capacity.SEWER_TYPE.SEWER);
                } else if (e.getValue().contains("M")) {
                    p.setWaterType(Capacity.SEWER_TYPE.MIX);
                }
            }
        }
        SWMMNetwork nw = new SWMMNetwork(pipes.values(), nodes.values());
        if (catchments != null && catchments.size() > 0) {
            nw.addAll(catchments);
            nw.setInflowArea(calculateEffectiveHydrologicalArea());
        }
        if (verbose) {
            System.out.println("Network building finished.");
        }
        return nw;
    }

    private Position buildPosition(Coordinate utm, MathTransform utm32wgs/*, MathTransform utm42wgs*/) throws TransformException {
        Coordinate wgs84;
//        if (utm.x > 4000000) {
//            wgs84 = JTS.transform(new Coordinate(utm.y, utm.x), null, utm42wgs);
//        } else {
        if (swapCoordinates) {
            wgs84 = JTS.transform(new Coordinate(utm.y, utm.x), null, utm32wgs);
        } else {
            wgs84 = JTS.transform(new Coordinate(utm.x, utm.y), null, utm32wgs);
        }
//        }

        return new Position(wgs84.x, wgs84.y, utm.x, utm.y);
    }

    private void readOptions(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String key, value;
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            parts = line.split(" ");
            if (parts.length < 2) {
                continue;
            }
//            System.out.println("parts:" + parts.length);
            key = parts[0];
            value = parts[parts.length - 1];

            if (key.startsWith("FLOW_UNITS")) {
                if (value.equals("LPS")) {
                    flowunits = FLOW_UNITS.LPS;
                } else if (value.equals("CMS")) {
                    flowunits = FLOW_UNITS.CMS;
                } else {
                    System.err.println(key + "=" + value + " not recognized.");
                }
            } else if (key.startsWith("START_DATE")) {
                String[] p = value.split("/");
                calStart.set(Calendar.YEAR, Integer.parseInt(p[2]));
                calStart.set(Calendar.MONTH, Integer.parseInt(p[0]) - 1);
                calStart.set(Calendar.DAY_OF_MONTH, Integer.parseInt(p[1]));
            } else if (key.startsWith("START_TIME")) {
                String[] p = value.split(":");
                calStart.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
                calStart.set(Calendar.MINUTE, Integer.parseInt(p[1]));
                calStart.set(Calendar.SECOND, Integer.parseInt(p[2]));
                calStart.set(Calendar.MILLISECOND, 0);
            } else if (key.startsWith("END_DATE")) {
                String[] p = value.split("/");
                calEnd.set(Calendar.YEAR, Integer.parseInt(p[2]));
                calEnd.set(Calendar.MONTH, Integer.parseInt(p[0]) - 1);
                calEnd.set(Calendar.DAY_OF_MONTH, Integer.parseInt(p[1]));
            } else if (key.startsWith("END_TIME")) {
                String[] p = value.split(":");
                calEnd.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
                calEnd.set(Calendar.MINUTE, Integer.parseInt(p[1]));
                calEnd.set(Calendar.SECOND, Integer.parseInt(p[2]));
                calEnd.set(Calendar.MILLISECOND, 0);
            }
        }
    }

    private void readRaingages(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, format, interval, scf, source, sourcename;
        raingages = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            try {
                String newline = line.replaceAll(" +", " ").trim();
                parts = newline.split(" ");
                if (parts.length < 5) {
                    continue;
                }
                name = parts[0];
                format = parts[1];
                interval = parts[2];
                scf = parts[3];
                source = parts[4];
                if (parts.length > 5) {
                    sourcename = parts[5];
                } else {
                    sourcename = "";
                }

                int intervallMinutes = 0;
                String[] ip = interval.split(":");
                intervallMinutes = Integer.parseInt(ip[1]);//Minutes
                intervallMinutes += Integer.parseInt(ip[0]) * 60;//hours
                Raingage r = new Raingage(name, format, intervallMinutes, Double.parseDouble(scf), source, sourcename);
                raingages.put(name, r);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void readTimeseries(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, filepath;
        timeseries = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            try {
                String newline = line.replaceAll(" +", " ").trim();
                parts = newline.split(" ");
                if (parts.length < 2) {
                    continue;
                }
                name = parts[0];
                filepath = parts[1];
                Timeseries r = new Timeseries(name, filepath);
                timeseries.put(name, r);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void readSubcatchments(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, gage, outlet, area, impervPercent, width, slopePercent, pondedarea;
        subcatchments = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 8) {
                continue;
            }
            name = parts[0];
            gage = parts[1];
            outlet = parts[2];
            area = parts[3]; //stored in [ha]
            impervPercent = parts[4]; //Stored in %
            width = parts[5];
            slopePercent = parts[6];
            Subcatchment s = new Subcatchment(name, gage, outlet, Double.parseDouble(area) * 10000., Double.parseDouble(impervPercent) * 0.01, Double.parseDouble(width), Double.parseDouble(slopePercent) * 0.01);
            subcatchments.put(name, s);
        }
    }

    private void readSubAreas(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, nImperv, nPerv, sImperv, sPerv, PctZero, RouteTo, PctRouted;
        subareas = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 7) {
                continue;
            }
            name = parts[0];
            nImperv = parts[1];
            nPerv = parts[2];
            sImperv = parts[3];
            sPerv = parts[4];
            PctZero = parts[5];
            RouteTo = parts[6];
            if (parts.length > 7) {
                PctRouted = parts[7];
            } else {
                PctRouted = "100";
            }

            SubArea sa = new SubArea(name, RouteTo, Double.parseDouble(nImperv), Double.parseDouble(nPerv), Double.parseDouble(sImperv), Double.parseDouble(sPerv), Double.parseDouble(PctZero), Double.parseDouble(PctRouted));

            subareas.put(name, sa);
        }
    }

    private void readInfiltrations(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name = "", curvenum, hydcon, drytime;
        LinkedList<Coordinate> polygon = new LinkedList<>();
        infiltrations = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 4) {
                continue;
            }

            name = parts[0];
            curvenum = parts[1];
            hydcon = parts[2];
            drytime = parts[3];

            int number = Integer.parseInt(curvenum);
            float hydroCon = Float.parseFloat(hydcon);
            float dryduration = Float.parseFloat(drytime);

            Infiltration infil = new Infiltration(name, number, hydroCon, dryduration);
            infiltrations.put(name, infil);
        }
    }

    private void readJunctions(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, elevation, maxdepth, initdepth, surchargedepth, pondedarea;
        junctions = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            } if (line.startsWith(";")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            elevation = parts[1];
            maxdepth = parts[2];
            initdepth = parts[3];
            surchargedepth = parts[4];
            pondedarea = parts[5];
            Junction j = new Junction(name, Double.parseDouble(elevation), Double.parseDouble(maxdepth), Double.parseDouble(initdepth), Double.parseDouble(surchargedepth), Double.parseDouble(pondedarea));
            junctions.put(name, j);
        }
    }

    private void readOutfalls(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, invert, type, data, gated;
        outfalls = new HashMap<>();
        int[] partends = null;
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();

//            if (line.startsWith(";;-")) {
//                String[] l = line.split(" ");
//                partends = new int[l.length];
//                int start = 0;
//                for (int i = 0; i < l.length; i++) {
//                    partends[i] = start + l[i].length();
//                    start += l[i].length() + 1;
//                }
//                break;
//            }
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }

            line = line.replaceAll(" +", " ").trim();
            parts = line.split(" ");
            boolean gate = false;
            try {
                name = parts[0].trim();//line.substring(0, partends[0]).trim();
                invert = parts[1].trim();// line.substring(partends[0] + 1, partends[1]).trim();
                type = parts[2].trim();//line.substring(partends[1] + 1, partends[2]).trim();
                data = parts[3].trim();//line.substring(partends[2] + 1, partends[3]);
                if (parts.length > 4) {
                    gated = parts[4].trim();//line.substring(partends[3] + 1, Math.min(line.length(), partends[4])).trim();
                    if (gated.toLowerCase().equals("yes")) {
                        gate = true;
                    }
                }

//            System.out.println("'"+name+"' '"+invert+"' '"+type+"' '"+data+"' '"+gated+"'");
                Outfall j = new Outfall(name, Double.parseDouble(invert), type, data, gate);
                outfalls.put(name, j);
            } catch (Exception exception) {
                System.err.println("Problem with line '" + line + "' <-" + parts.length + " parts of '" + line + "'");
            }

        }
    }

    private void readDividers(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, elevation, link, type;
        double param1, param2, param3, param4, param5, param6;
        dividers = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 4) {
                continue;
            }

            name = parts[0];
            elevation = parts[1];
            link = parts[2];
            type = parts[3];
            param1 = 0; //depth
            param2 = 0;
            param3 = 0;
            param4 = 0;
            param5 = 0;
            param6 = 0;
            if (parts.length > 4) {
                param1 = Double.parseDouble(parts[4]);
            }
            if (parts.length > 5) {
                param2 = Double.parseDouble(parts[5]);
            }
            if (parts.length > 6) {
                param3 = Double.parseDouble(parts[6]);
            }
            if (parts.length > 7) {
                param4 = Double.parseDouble(parts[7]);
            }

            Divider s = new Divider(name, Double.parseDouble(elevation), link, type, param1, param2, param3, param4, param5, param6);
            dividers.put(name, s);
        }
    }

    private void readStorages(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, elevation, maxdepth, initdepth, shape, param1, param2, param3 = "0", param4 = "0", pondedarea = "0", evaporFract = "0";
        storages = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            elevation = parts[1];
            maxdepth = parts[2];
            initdepth = parts[3];
            shape = parts[4];
            param1 = parts[5];
            param2 = parts[6];
            if (parts.length > 7) {
                param3 = parts[7];
                if (parts.length > 8) {
                    param4 = parts[8];
                    if (parts.length > 9) {
                        pondedarea = parts[9];
                        if (parts.length > 10) {
                            evaporFract = parts[10];
                        }
                    }
                }
            }

            Storage s;
            if (shape.toLowerCase().equals("tabular")) {
                s=new Storage(name, Double.parseDouble(elevation), Double.parseDouble(maxdepth), Double.parseDouble(initdepth),0,0,0,0,0,0);
            } else {
                s = new Storage(name, Double.parseDouble(elevation), Double.parseDouble(maxdepth), Double.parseDouble(initdepth), Double.parseDouble(param1), Double.parseDouble(param2), Double.parseDouble(param3), Double.parseDouble(param4), Double.parseDouble(pondedarea), Double.parseDouble(evaporFract));
            }
            storages.put(name, s);
        }
    }

    private void readConduits(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, fromNaode, toNode, length, roughness, addHeightIn, addHeightOut;
        conduits = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            fromNaode = parts[1];
            toNode = parts[2];
            length = parts[3];
            roughness = parts[4];
            addHeightIn = parts[5];
            addHeightOut = parts[6];
            String tag = "";
            if (parts.length > 9) {
                for (int i = 9; i < parts.length; i++) {
                    tag += parts[i];
                }
            }
            Conduit c = new Conduit(name, fromNaode, toNode, Double.parseDouble(length), Double.parseDouble(roughness), Double.parseDouble(addHeightIn), Double.parseDouble(addHeightOut));
            c.tag = tag;
            conduits.put(name, c);
        }
    }

    private void readOrifices(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, fromNaode, toNode, type, ht;
        orifices = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            fromNaode = parts[1];
            toNode = parts[2];
            type = parts[3];
            ht = parts[4];
            Orifice c = new Orifice(name, fromNaode, toNode, type, Double.parseDouble(ht));
            orifices.put(name, c);
        }
    }

    private void readWeirs(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, fromNaode, toNode, type, ht;
        weirs = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            fromNaode = parts[1];
            toNode = parts[2];
            type = parts[3];
            ht = parts[4];
            Weir c = new Weir(name, fromNaode, toNode, type, Double.parseDouble(ht));
            weirs.put(name, c);
        }
    }

    private void readOutlets(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, fromNaode, toNode, type, ht;
        outlets = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 6) {
                continue;
            }
            name = parts[0];
            fromNaode = parts[1];
            toNode = parts[2];
            ht = parts[3];
            type = parts[4];
            Outlet c = new Outlet(name, fromNaode, toNode, type, Double.parseDouble(ht));
            outlets.put(name, c);
        }
    }

    private void readDryWeaterFlow(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String node, parameter, baseDWF, patterns = null;
        dryWeatherFlows = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }
            node = parts[0];
            parameter = parts[1];
            baseDWF = parts[2];
            if (parts.length > 3) {
                patterns = parts[3];
            }
            DryWeatherFlow dwf = new DryWeatherFlow(node, parameter, Float.parseFloat(baseDWF));
            dryWeatherFlows.put(node, dwf);
        }
    }

    private void readProfiles(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String link, key, shape;
        profiles = new HashMap<>();
        HashMap<String, Profile> storage = new HashMap<>(30);
        Profile profile = null;
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }
            link = parts[0];
            key = parts[1] + parts[2];
            profile = storage.get(key);
            if (profile == null) {
                double d = Double.parseDouble(parts[2].replaceAll("[^\\d.]", ""));
                profile = new CircularProfile(d);
                storage.put(key, profile);
            }
            profiles.put(link, profile);
        }
    }

    private void createProfiles() throws IOException {

        String link, key;
        profiles = new HashMap<>();
        HashMap<String, Profile> storage = new HashMap<>(30);
        Profile profile = null;
        for (Map.Entry<String, XSection> entry : xSections.entrySet()) {
            link = entry.getKey();
            key = entry.getValue().shape + entry.getValue().geom1;
            profile = storage.get(key);
            if (profile == null) {
                profile = new CircularProfile(entry.getValue().geom1);
                storage.put(key, profile);
            }
            profiles.put(link, profile);
        }
    }

    private void readXSections(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String link, shape, geom1 = null, geom2 = null, geom3 = null, geom4 = null, barrels = null;
        xSections = new HashMap<>();

        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }
            link = parts[0];
            shape = parts[1];
            geom1 = parts[2];
            if (parts.length > 3) {
                geom2 = parts[3];
                if (parts.length > 4) {
                    geom3 = parts[4];
                    if (parts.length > 5) {
                        geom4 = parts[5];
                        if (parts.length > 6) {
                            barrels = parts[6];
                        }
                    }
                }
            }
            double g1 = 0, g2 = 0, g3 = 0, g4 = 0;
            int b = 0;
            try {
                g1 = Double.parseDouble(geom1);
                g2 = Double.parseDouble(geom2);
                g3 = Double.parseDouble(geom3);
                g4 = Double.parseDouble(geom4);
                b = Integer.parseInt(barrels);
            } catch (Exception numberFormatException) {
            }
            XSection xs = new XSection(link, shape, g1, g2, g3, g4, b);
            xSections.put(link, xs);
        }
    }

    private void readTags(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String key, tag;
        tags = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line;
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }

            key = null;
            tag = "";
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].isEmpty()) {
                    continue;
                }

                if (key == null) {
                    key = parts[i];
                } else {
                    tag += parts[i] + " ";
                }

            }
            tag = tag.trim();
//           System.out.println(key+":\t"+tag);
            tags.put(key, tag);
        }
    }

    private void readCoordinates(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, x, y;
        coordinates = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.startsWith(";")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }
            name = parts[0];
            x = parts[1];
            y = parts[2];
            Coordinate c = new Coordinate(Double.parseDouble(x), Double.parseDouble(y));
            coordinates.put(name, c);
        }
    }

    private void readPolygons(BufferedReader br) throws IOException {
        String line = "";
        String[] parts = null;
        String name = "", x, y;
        LinkedList<Coordinate> polygon = new LinkedList<>();
        polygons = new HashMap<>();
        while (br.ready()) {
            br.mark(4000);
            line = br.readLine();
            if (line.startsWith("[")) {
                //finish last polygon
                if (name != null && polygon != null && !polygon.isEmpty()) {
                    polygons.put(name, polygon);
                }

                br.reset();
                return;
            }
            if (line.startsWith(";;")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String newline = line.replaceAll(" +", " ").trim();
            parts = newline.split(" ");
            if (parts.length < 3) {
                continue;
            }
            if (!parts[0].equals(name)) {
                if (!polygon.isEmpty()) {
                    polygons.put(name, polygon);
                }
                polygon = new LinkedList<>();
            }
            name = parts[0];
            x = parts[1];
            y = parts[2];
            polygon.add(new Coordinate(Double.parseDouble(x), Double.parseDouble(y)));
        }
    }

    public class Node {

        public String name;

        public Node(String name) {
            this.name = name;
        }

    }

    public class Raingage {

        public String name, format, source, sourceName;
        public int intervallMinutes;
        public double scf;

        public Raingage(String name, String format, int intervallMinutes, double scf, String source, String sourceName) {
            this.name = name;
            this.format = format;
            this.source = source;
            this.sourceName = sourceName;
            this.intervallMinutes = intervallMinutes;
            this.scf = scf;
        }

    }

    public class Subcatchment extends Node {

        public String outletNode, gage;
        public double totalarea, imperviousRatio, width, slopetotal;

        /**
         *
         * @param name
         * @param gage
         * @param outletNode
         * @param totalarea m^2
         * @param imperviousRatio [0..1]
         * @param width
         * @param slopetotal
         */
        public Subcatchment(String name, String gage, String outletNode, double totalarea, double imperviousRatio, double width, double slopetotal) {
            super(name);
            this.gage = gage;
            this.outletNode = outletNode;
            this.totalarea = totalarea;
            this.imperviousRatio = imperviousRatio;
            this.width = width;
            this.slopetotal = slopetotal;
        }

    }

    public class SubArea {

        public String subcatchment, routeTo;
        public double nImpervious, nPervious, sImpervious, sPervious, percentZero, percentRouted;

        public SubArea(String subcatchment, String routeTo, double nImpervious, double nPervious, double sImpervious, double sPervious, double percentZero, double percentRouted) {
            this.subcatchment = subcatchment;
            this.routeTo = routeTo;
            this.nImpervious = nImpervious;
            this.nPervious = nPervious;
            this.sImpervious = sImpervious;
            this.sPervious = sPervious;
            this.percentZero = percentZero;
            this.percentRouted = percentRouted;
        }

    }

    public class Junction extends Node {

        public double elevation, maxdepth, initdepth, surchargedepth, pondedarea;

        public Junction(String name, double elevation, double maxdepth, double initdepth, double surchargedepth, double pondedarea) {
            super(name);
            this.elevation = elevation;
            this.maxdepth = maxdepth;
            this.initdepth = initdepth;
            this.surchargedepth = surchargedepth;
            this.pondedarea = pondedarea;
        }
    }

    public class Outfall extends Node {

        public double invert;
        public String type, stageData;
        public boolean gated;

        public Outfall(String name, double invert, String type, String stageData, boolean gated) {
            super(name);
            this.invert = invert;
            this.type = type;
            this.stageData = stageData;
            this.gated = gated;
        }

    }

    public class Divider extends Node {

        public double invert;
        public String link, type;
        public double param1, param2, param3, param4, param5, param6;

        public Divider(String name, double invert, String link, String type, double param1, double param2, double param3, double param4, double param5, double param6) {
            super(name);
            this.invert = invert;
            this.link = link;
            this.type = type;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
            this.param4 = param4;
            this.param5 = param5;
            this.param6 = param6;
        }

    }

    public class Storage extends Node {

        public double elevation, maxdepth, initdepth, param1, param2, param3, param4, pondedarea, evaporFract;

        public Storage(String name, double elevation, double maxdepth, double initdepth, double param1, double param2, double param3, double param4, double pondedarea, double evaporFract) {
            super(name);
            this.elevation = elevation;
            this.maxdepth = maxdepth;
            this.initdepth = initdepth;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
            this.param4 = param4;
            this.pondedarea = pondedarea;
            this.evaporFract = evaporFract;
        }
    }

    public class Edge {

        public String name;
        public String fromNode, toNode;

        public Edge(String name, String fromNode, String toNode) {
            this.name = name;
            this.fromNode = fromNode;
            this.toNode = toNode;
        }

    }

    public class Conduit extends Edge {

        public double length, roughness;
        public double offsetheightIn, offsetheightOut;
        public String tag;

//        public Conduit(String name, String fromNode, String toNode, double length, double roughness) {
//            super(name, fromNode, toNode);
//            this.name = name;
//            this.fromNode = fromNode;
//            this.toNode = toNode;
//            this.length = length;
//            this.roughness = roughness;
//        }
        public Conduit(String name, String fromNode, String toNode, double length, double roughness, double offsetheightIn, double offsetheightOut) {
            super(name, fromNode, toNode);
            this.length = length;
            this.roughness = roughness;
            this.offsetheightIn = offsetheightIn;
            this.offsetheightOut = offsetheightOut;
        }

    }

    public class Orifice extends Edge {

        String type;
        double height;

        public Orifice(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }
    }

    public class Weir extends Edge {

        String type;
        double height;

        public Weir(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }

    }

    public class Outlet extends Edge {

        public String type;
        public double height;

        public Outlet(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }

    }

    public class Infiltration {

        String subCatchment;
        int curveNumber;
        float hydraulicConductivity;
        float dryTime;

        public Infiltration(String subCatchment, int curveNumber, float hydraulicConductivity, float dryTime) {
            this.subCatchment = subCatchment;
            this.curveNumber = curveNumber;
            this.hydraulicConductivity = hydraulicConductivity;
            this.dryTime = dryTime;
        }

    }

    public class DryWeatherFlow {

        String node, parameter;
        float baseFlow;

        public DryWeatherFlow(String node, String parameter, float baseFlow) {
            this.node = node;
            this.parameter = parameter;
            this.baseFlow = baseFlow;
        }

    }

    public class XSection {

        public String link, shape;
        public double geom1, geom2, geom3, geom4;
        public int barrels;

        public XSection(String link, String shape, double geom1, double geom2, double geom3, double geom4, int barrels) {
            this.link = link;
            this.shape = shape;
            this.geom1 = geom1;
            this.geom2 = geom2;
            this.geom3 = geom3;
            this.geom4 = geom4;
            this.barrels = barrels;
        }

    }

    public class Timeseries {

        public String name;
        public String filepath;
        public boolean accessible = false;
        public long[] times;
        public double[] precipitation;

        public Timeseries(String name, String filepath) {
            this.name = name;
            this.filepath = filepath;
            try {
                if (new File(filepath).exists()) {
                    accessible = true;
                }
            } catch (Exception e) {
            }
        }

    }

    /**
     * Read Results from a SWMM result file (*.rpt) and add timelines to the
     * network (read before from the '.inp file)
     *
     * @param fileTimelines
     * @param network
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    public static Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> readTimeLines(File fileTimelines, Network network) throws FileNotFoundException, IOException, ParseException {
        ArrayTimeLineManholeContainer manholecontaineR = null;
        ArrayTimeLinePipeContainer pipeContainer = null;
        try (FileInputStream fis = new FileInputStream(fileTimelines); InputStreamReader isr = new InputStreamReader(fis, Charset.forName("utf-8")); BufferedReader br = new BufferedReader(isr)) {
            //Find information about Topology count
            int nodeCount = -1;
            int pipecount = -1;
            int subcatchmentCount = -1;
            while (br.ready()) {
                String line = br.readLine();
                if (line.contains("Number of nodes")) {
//                    System.out.println("last index of ' '=" + line.lastIndexOf(" ") + "   '.'=" + line.lastIndexOf('.'));
                    line = line.substring(line.lastIndexOf(' ')).replaceAll(" ", "");
                    nodeCount = Integer.parseInt(line);
                } else if (line.contains("Number of links")) {
                    line = line.substring(line.lastIndexOf(' ')).replaceAll(" ", "");
                    pipecount = Integer.parseInt(line);
                } else if (line.contains("Number of subcatchments")) {
                    line = line.substring(line.lastIndexOf(' ')).replaceAll(" ", "");
                    subcatchmentCount = Integer.parseInt(line);
                } else if (line.contains("Subcatchment Summary")) {
                    //Break Topology search
                    break;
                }
            }
            if (verbose) {
                System.out.println("Nodes: " + nodeCount + "   pipes: " + pipecount + "   subcatchments: " + subcatchmentCount);
            }

            // Find information about times in section 'Analysis Options'
            while (br.ready()) {
                String line = br.readLine();
                if (line.contains("Analysis Options")) {
                    break;
                }
            }
            //Find information about times
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            SimpleDateFormat sdfreport = new SimpleDateFormat("HH:mm:ss");

            String startdateString = null, enddateString = null, reportStepString = null;
            Date startdate = null, enddate = null;
            long reportTime = 0;
            while (br.ready()) {
                String line = br.readLine();
                if (line.contains("Starting Date")) {
                    if (verbose) {
                        System.out.println("Position: " + line.lastIndexOf(". "));
                    }
                    startdateString = line.substring(line.lastIndexOf(". ") + 2);
                    startdate = sdf.parse(startdateString);
                } else if (line.contains("Ending Date")) {
                    enddateString = line.substring(line.lastIndexOf(". ") + 2);
                    enddate = sdf.parse(enddateString);
                } else if (line.contains("Report Time Step")) {
//                    enddateString= line.substring(line.lastIndexOf(' '));
                    Date d = sdfreport.parse(line.substring(line.lastIndexOf(' ')));
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.setTime(d);
//                    System.out.println("Reporttime: "+d);
                    reportTime = gc.get(Calendar.SECOND) * 1000 + gc.get(GregorianCalendar.MINUTE) * 60000 + gc.get(GregorianCalendar.HOUR) * 3600000;
                } else if (line.length() < 4) {
                    break;
                }
            }
            if (verbose) {
                System.out.println("Start date= " + startdate);
            }
            if (verbose) {
                System.out.println("End   date= " + enddate);
            }
            if (verbose) {
                System.out.println("reporttime = " + reportTime + " ms");
            }
            double timestampsD = ((enddate.getTime() - startdate.getTime()) / (double) reportTime);
            int timestamps = (int) timestampsD;
            if (verbose) {
                System.out.println("timestamp fraction=" + timestampsD);
            }
            if (timestampsD % 1 != 0) {
                if (verbose) {
                    System.out.println("mit Rest : " + (timestampsD % 1));
                }
                timestamps = (int) (timestampsD + 1);
            }
            if (verbose) {
                System.out.println("Timestamps: " + timestamps);
            }
            long[] times = new long[timestamps];
            for (int i = 0; i < times.length; i++) {
                times[i] = startdate.getTime() + i * reportTime;
            }
            TimeContainer timeContainer = new TimeContainer(times);
            manholecontaineR = new ArrayTimeLineManholeContainer(timeContainer, nodeCount);
            pipeContainer = new ArrayTimeLinePipeContainer(timeContainer, pipecount);

            // fertig initialisieren
            //search for Results Nodes first
            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("  Node Results")) {
                    break;
                }
            }
            //Reached Node results. now alayse results
            String[] parts;
            int mhcounter = -1;
            while (br.ready()) {
                br.mark(4000);
                line = br.readLine();
                String id = null;
                if (line.contains("<<< Node")) {
                    int index = line.indexOf("Node") + 5;
                    int toIndex = line.indexOf(">") - 1;
                    id = line.substring(index, toIndex);
                    Manhole mh = network.getManholeByName(id);
                    if (mh == null) {
                        System.err.println("Could not find Manhole (" + id + ") in Network.");
                        continue;
                    }
                    //Skip header
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    // Read Values
                    mhcounter++;
                    ArrayTimeLineManhole tmh = new ArrayTimeLineManhole(manholecontaineR, mhcounter);
                    mh.setStatusTimeline(tmh);
                    int timecounter = 0;
                    while (br.ready()) {
                        line = br.readLine();
                        if (line.length() < 10) {
                            break;
                        }
                        float hz = Float.parseFloat(line.substring(line.lastIndexOf(" ") + 1));
                        tmh.setWaterZ(hz, timecounter);
                        tmh.setWaterLevel(hz - mh.getSole_height(), timecounter);
                        timecounter++;
                    }
                } else if (line.contains("Link Results")) {
                    br.reset();
                    break;
                }
            }
            //Finished Nodes 
            // Go to Links (Pipes)
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("Link Results")) {
                    break;
                }
            }
            int pipecounter = -1;
            while (br.ready()) {
                br.mark(4000);
                line = br.readLine();
                String id = null;
                if (line.contains("<<< Link")) {
                    int index = line.indexOf("Link") + 5;
                    int toIndex = line.indexOf(">") - 1;
                    id = line.substring(index, toIndex);
                    Pipe pipe = network.getPipeByName(id);
                    if (pipe == null) {
                        System.err.println("Could not find Pipe (" + id + ") in Network.");
                        continue;
                    }
                    //Skip header
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    // Read Values
                    pipecounter++;
                    ArrayTimeLinePipe tmh = new ArrayTimeLinePipe(pipeContainer, pipecounter);
                    pipe.setStatusTimeLine(tmh);
                    int timecounter = 0;
                    while (br.ready()) {
                        line = br.readLine();
                        if (line.length() < 10) {
                            break;
                        }
                        parts = line.trim().replaceAll(" +", " ").split(" ");
//                        System.out.println("Pipes split to "+parts.length+" parts");
                        tmh.setVelocity(Float.parseFloat(parts[3]), timecounter);
                        tmh.setWaterlevel(Float.parseFloat(parts[4]), timecounter);
                        tmh.setVolume((float) (pipe.getProfile().getFlowArea(Float.parseFloat(parts[4])) * pipe.getLength()), timecounter);

                        timecounter++;
                    }
                } else if (line.contains("Link Results")) {
                    br.reset();
                    break;
                }
            }
        }
        return new Pair<>(pipeContainer, manholecontaineR);
    }

    /**
     * A HAshmap with <NodeName, Flood volume [m³] from surcharging>
     *
     * @param resultFile
     * @return
     */
    public static HashMap<String, Float> getOutflowVolumePerNode(File resultFile) throws FileNotFoundException, IOException {
        HashMap<String, Float> map = new HashMap<>();
        try (FileReader fr = new FileReader(resultFile)) {
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("Node Flooding Summary")) {
                    break;
                }
            }
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("Flooded")) {
                    br.readLine();
                    break;
                }
            }
            //Parse content
            while (br.ready()) {
                line = br.readLine();
                if (line.contains("**********")) {
                    //End of content reached
                    break;
                }
                String[] splits = line.trim().replaceAll(" +", " ").split(" ");
                if (splits == null || splits.length < 6) {
                    continue;
                }
                String name = splits[0];
                float volume = Float.parseFloat(splits[5]);
                map.put(name, volume);
            }
            br.close();
        }
        return map;
    }

    /**
     * Calculates the area, that accumulates and sends water into the pipe
     * system. Regards pervious fraction the area of the subcatchment
     *
     * @return m^2
     */
    public double calculateEffectiveHydrologicalArea() {
        double area = 0;
        for (Subcatchment catchment : subcatchments.values()) {
            area += catchment.totalarea * catchment.imperviousRatio;
        }
        return area;
    }

    public static boolean writeINP(Network network, File targetFile) throws IOException {
        SWMMNetwork swmm = null;
        if (network instanceof SWMMNetwork) {
            swmm = (SWMMNetwork) network;
        }

        try (FileWriter fw = new FileWriter(targetFile, false)) {
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("[TITLE]\n"
                    + ";;Project Title/Notes\n"
                    + network.getName() + "\n"
                    + "[OPTIONS]\n"
                    + ";;Option             Value\n"
                    + "FLOW_UNITS           CMS\n"
                    + "INFILTRATION         Horton\n"
                    + "FLOW_ROUTING         DYNWAVE\n"
                    + "LINK_OFFSETS         DEPTH\n"
                    + "MIN_SLOPE            0\n"
                    + "ALLOW_PONDING        NO\n"
                    + "SKIP_STEADY_STATE    NO\n"
                    + "\n"
                    + "IGNORE_RDII          YES\n"
                    + "IGNORE_SNOWMELT      YES\n"
                    + "IGNORE_GROUNDWATER   YES\n"
                    + "START_DATE           01/01/2018\n"
                    + "START_TIME           00:00:00\n"
                    + "REPORT_START_DATE    01/01/2018\n"
                    + "REPORT_START_TIME    00:00:00\n"
                    + "END_DATE             01/01/2018\n"
                    + "END_TIME             10:00:00\n"
                    + "SWEEP_START          1/1\n"
                    + "SWEEP_END            12/31\n"
                    + "DRY_DAYS             14\n"
                    + "REPORT_STEP          0:01:00\n"
                    + "WET_STEP             0:01:00\n"
                    + "DRY_STEP             1:00:00\n"
                    + "ROUTING_STEP         0:00:01 \n"
                    + "\n"
                    + "INERTIAL_DAMPING     NONE\n"
                    + "NORMAL_FLOW_LIMITED  BOTH\n"
                    + "FORCE_MAIN_EQUATION  H-W\n"
                    + "VARIABLE_STEP        0.00\n"
                    + "LENGTHENING_STEP     0\n"
                    + "MIN_SURFAREA         0\n"
                    + "MAX_TRIALS           8\n"
                    + "HEAD_TOLERANCE       0.0015\n"
                    + "SYS_FLOW_TOL         5\n"
                    + "LAT_FLOW_TOL         5\n"
                    + "MINIMUM_STEP         0.5\n"
                    + "THREADS              1\n"
                    + "\n"
                    + "[EVAPORATION]\n"
                    + ";;Data Source    Parameters\n"
                    + ";;-------------- ----------------\n"
                    + "CONSTANT         0\n"
                    + "DRY_ONLY         YES\n"
                    + "\n"
                    + "[RAINGAGES]\n"
                    + ";;Name           Format    Interval SCF      Source    \n"
                    + ";;-------------- --------- ------ ------ ----------\n"
                    + "default          CUMULATIVE 00:05    1.0      TIMESERIES default_rain.dat\n"
                    + "\n"
                    + "[SUBCATCHMENTS]\n"
                    + ";;Name           Rain Gage        Outlet           Area     %Imperv  Width    %Slope   CurbLen  SnowPack        \n"
                    + ";;-------------- ---------------- ---------------- -------- -------- -------- -------- -------- ----------------");
            if (swmm != null) {
                for (Catchment c : swmm.getCatchments()) {
                    //Store area in [ha]
                    bw.write("\n" + c.getName() + " default          " + c.getOutlet().getName() + " " + c.getArea() / 10000. + " " + c.getImperviousRate() * 100 + " " + Math.sqrt(c.getArea()) + " " + c.getSlope() + " 1 0");
                }
            }
            bw.write("\n\n[SUBAREAS]\n"
                    + ";;Subcatchment   N-Imperv   N-Perv     S-Imperv   S-Perv     PctZero    RouteTo    PctRouted \n"
                    + ";;-------------- ---------- ---------- ---------- ---------- ---------- ---------- ----------");
            if (swmm != null) {
                for (Catchment c : swmm.getCatchments()) {
                    bw.write("\n" + c.getName() + " 0.01       0.1        0.05       0.05       25         OUTLET    100");
                }
            }
            bw.write("\n\n[INFILTRATION]\n"
                    + ";;Subcatchment   MaxRate    MinRate    Decay      DryTime    MaxInfil  \n"
                    + ";;-------------- ---------- ---------- ---------- ---------- ----------");
            if (swmm != null) {
                for (Catchment c : swmm.getCatchments()) {
                    bw.write("\n" + c.getName() + " 60         6.12       0.03       1          0         ");
                }
            }

            bw.write("\n\n[JUNCTIONS]\n"
                    + ";;Name           Elevation  MaxDepth   InitDepth  SurDepth   Aponded   \n"
                    + ";;-------------- ---------- ---------- ---------- ---------- ----------");
            for (Manhole m : network.getManholes()) {
                if (m.isSetAsOutlet()) {
                    continue;
                }
                bw.write("\n" + m.getName() + " " + m.getSole_height() + " " + (m.getTop_height() - m.getSole_height()) + " 0 0 0");
            }

            bw.write("\n\n[OUTFALLS]\n"
                    + ";;Name           Elevation  Type       Stage Data       Gated    Route To        \n"
                    + ";;-------------- ---------- ---------- ---------------- -------- ----------------");

            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (Manhole m : network.getManholes()) {
                minX = Math.min(minX, m.getPosition().getX());
                maxX = Math.max(maxX, m.getPosition().getX());
                minY = Math.min(minY, m.getPosition().getY());
                maxY = Math.max(maxY, m.getPosition().getY());

                if (!m.isSetAsOutlet()) {
                    continue;
                }
                bw.write("\n" + m.getName() + " " + m.getSole_height() + " FREE NO");
            }

            bw.write("\n\n[CONDUITS]\n"
                    + ";;Name           From Node        To Node          Length     Roughness  InOffset   OutOffset  InitFlow   MaxFlow   \n"
                    + ";;-------------- ---------------- ---------------- ---------- ---------- ---------- ---------- ---------- ----------");
            // n=0.013 is equal to kst=77 equal to kb=1.5 mm (HE default)
            for (Pipe p : network.getPipes()) {
                StorageVolume s = p.getStartConnection().getManhole();
                StorageVolume e = p.getEndConnection().getManhole();
                bw.write("\n" + p.getName() + " " + s.getName() + " " + e.getName() + " " + p.getLength() + " 0.013 " + (p.getStartConnection().getHeight() - s.getSole_height()) + " " + (p.getEndConnection().getHeight() - e.getSole_height()) + " 0 0");
            }

            bw.write("\n\n[XSECTIONS]\n"
                    + ";;Link           Shape        Geom1            Geom2      Geom3      Geom4      Barrels    Culvert   \n"
                    + ";;-------------- ------------ ---------------- ---------- ---------- ---------- ---------- ----------\n");
            for (Pipe p : network.getPipes()) {
                if (p.getProfile() instanceof CircularProfile) {
                    bw.write("\n" + p.getName() + "  CIRCULAR     " + ((CircularProfile) p.getProfile()).getDiameter() + "             0          0          0          1                    ");
                } else {
                    //Fallback: circular 0.5
                    bw.write("\n" + p.getName() + "  CIRCULAR     0.5             0          0          0          1 ");

                }
            }

            bw.write("\n\n[TIMESERIES]\n"
                    + ";;Name           Date       Time       Value     \n"
                    + ";;-------------- ---------- ---------- ----------\n"
                    + "default_rain.dat FILE \"default_rain.dat\"\n"
                    + "\n"
                    + "[REPORT]\n"
                    + ";;Reporting Options\n"
                    + "INPUT      NO\n"
                    + "CONTROLS   YES\n"
                    + "SUBCATCHMENTS ALL\n"
                    + "NODES ALL\n"
                    + "LINKS ALL\n"
                    + "\n"
                    + "[TAGS]\n"
                    + "\n"
                    + "[MAP]\n"
                    + "DIMENSIONS " + minX + " " + minY + " " + maxX + " " + maxY
                    + "\nUnits      m");

            bw.write("\n\n[COORDINATES]\n"
                    + ";;Node           X-Coord            Y-Coord           \n"
                    + ";;-------------- ------------------ ------------------");
            for (Manhole m : network.getManholes()) {
                bw.write("\n" + m.getName() + " " + m.getPosition().getX() + " " + m.getPosition().getY());
            }
            bw.write("\n");
            bw.close();
        }
        return true;
    }
}
