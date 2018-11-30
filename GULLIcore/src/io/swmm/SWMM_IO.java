package io.swmm;

import com.vividsolutions.jts.geom.Coordinate;
import control.StartParameters;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import model.GeoPosition2D;
import model.timeline.array.TimeContainer;
import model.timeline.array.ArrayTimeLineManhole;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.topology.Capacity;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.SWMMNetwork;
import model.topology.catchment.Catchment;
import model.topology.graph.Pair;
import model.topology.profile.CircularProfile;
import model.topology.profile.Profile;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
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

    /**
     * Orient the start and end of pipes by slope.
     */
    public static boolean orientGravity = false;

    public static boolean swapCoordinates = false;

    private enum FLOW_UNITS {

        LPS, CMS
    };

    private FLOW_UNITS flowunits = FLOW_UNITS.CMS;
    private HashMap<String, Subcatchment> subcatchments;
    private HashMap<String, Junction> junctions;
    private HashMap<String, Outfall> outfalls;
    private HashMap<String, Divider> dividers;
    private HashMap<String, Storage> storages;
    private HashMap<String, Conduit> conduits;
    private HashMap<String, Orifice> orifices;
    private HashMap<String, Weir> weirs;
    private HashMap<String, Outlet> outlets;
    private HashMap<String, Coordinate> coordinates;
    private HashMap<String, List<Coordinate>> polygons;
    private HashMap<String, Profile> profiles;
    private HashMap<String, String> tags;

    public static Network readNetwork(File file) throws FileNotFoundException, IOException, FactoryException {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        SWMM_IO swmm = new SWMM_IO();
        String line = "";
        int linecount = -1;
        try {
            while (br.ready()) {
                line = br.readLine();
                linecount++;
                if (line.toUpperCase().equals("[OPTIONS]")) {
                    swmm.readOptions(br);
                } else if (line.toUpperCase().equals("[SUBCATCHMENTS]")) {
                    swmm.readSubcatchments(br);
                    System.out.println(swmm.subcatchments.size() + " subcatchments read");
                } else if (line.toUpperCase().equals("[JUNCTIONS]")) {
                    swmm.readJunctions(br);
                    System.out.println(swmm.junctions.size() + " junctions read");
                } else if (line.toUpperCase().equals("[OUTFALLS]")) {
                    swmm.readOutfalls(br);
                    System.out.println(swmm.outfalls.size() + " outfalls read");
                } else if (line.toUpperCase().equals("[DIVIDERS]")) {
                    swmm.readDividers(br);
                    System.out.println(swmm.dividers.size() + " dividers read");
                } else if (line.toUpperCase().equals("[STORAGE]")) {
                    swmm.readStorages(br);
                    System.out.println(swmm.storages.size() + " storages read");
                } else if (line.toUpperCase().equals("[CONDUITS]")) {
                    swmm.readConduits(br);
                    System.out.println(swmm.conduits.size() + " conduits read");
                } else if (line.toUpperCase().equals("[ORIFICES]")) {
                    swmm.readOrifices(br);
                    System.out.println(swmm.orifices.size() + " orifices read");
                } else if (line.toUpperCase().equals("[WEIRS]")) {
                    swmm.readWeirs(br);
                    System.out.println(swmm.weirs.size() + " weirs read");
                } else if (line.toUpperCase().equals("[OUTLETS]")) {
                    swmm.readOutlets(br);
                    System.out.println(swmm.outlets.size() + " outlets read");
                } else if (line.toUpperCase().equals("[XSECTIONS]")) {
                    swmm.readProfiles(br);
                    System.out.println(swmm.profiles.size() + " profiles read");

                } else if (line.toUpperCase().equals("[TAGS]")) {
                    swmm.readTags(br);
                    System.out.println(swmm.tags.size() + " Tags read");
                } else if (line.toUpperCase().equals("[COORDINATES]")) {
                    swmm.readCoordinates(br);
                    System.out.println(swmm.coordinates.size() + " coordinates read");
                } else if (line.toUpperCase().equals("[POLYGONS]")) {
                    swmm.readPolygons(br);
                    System.out.println(swmm.polygons.size() + " Polygons read");
                }
            }
        } catch (Exception exception) {
            System.err.println("Exception in line " + linecount);
            throw exception;
        }
        br.close();
        fr.close();

        return swmm.finishNetwork();
    }

    private Network finishNetwork() throws FactoryException {
        if (junctions == null) {
            junctions = new HashMap<>(0);
        }
        if (storages == null) {
            storages = new HashMap<>(0);
        }

        HashMap<String, Manhole> nodes = new HashMap<>(junctions.size() + storages.size());
        HashMap<String, Pipe> pipes = new HashMap<>(conduits.size());
        CRSAuthorityFactory af = CRS.getAuthorityFactory(StartParameters.JTS_WGS84_LONGITUDE_FIRST);
        CoordinateReferenceSystem wgs84CRS = null;
        CoordinateReferenceSystem utmCRS = null, utm4CRS = null;
        CRS.cleanupThreadLocals();
        //Find plausible Coordinates
        if (coordinates != null && !coordinates.isEmpty()) {
            Coordinate c = coordinates.entrySet().iterator().next().getValue();
            if (c.x > 1000000) {
                //Gauss Krüger
                //Zone
                int zone = (int) (c.x / 1000000);
                System.out.println("Found input coordinates to be GK Zone " + zone + " -> EPSG:3146" + zone);
                utmCRS = af.createCoordinateReferenceSystem("EPSG:3146" + zone);
            } else {
                System.out.println("Found input coordinates to be UTM WGS84 32N -> EPSG:25832");
                utmCRS = af.createCoordinateReferenceSystem("EPSG:25832"); //UTM WGS84 32Nord
            }
        }

        System.out.print("Creating Geospatial Transformation...");
        try {

            wgs84CRS = af.createCoordinateReferenceSystem("EPSG:4326");//CRS.decode("EPSG:4326"); //WGS84
//            utm3CRS = CRS.decode("EPSG:31467");//DHDN / 3-degree Gauss-Kruger zone 3
//            utm4CRS = CRS.decode("EPSG:31468");//DHDN / 3-degree Gauss-Kruger zone 4
            CRS.cleanupThreadLocals();
            System.out.println("done.");
        } catch (Exception ex) {
            System.out.println("error.");
            ex.printStackTrace();
        }

        MathTransform utm2wgs = CRS.findMathTransform(utmCRS, wgs84CRS);
//        MathTransform utm2wgs = CRS.findMathTransform(utm4CRS, wgs84CRS);
        /**
         * SCHÄCHTE
         */
        System.out.println("Converting Manholes");
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
                    mh.setTop_height((float) p.getValue().invert);
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

        ArrayList<Catchment> catchments = new ArrayList<>(subcatchments.size());
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
        System.out.println("Converting Conduits");
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
                System.out.println("Reorientate Conduit " + p.getKey());
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
                pipe.setName("Outlet " + p.getKey());
//                pipe.tags = new Tags();
//                pipe.tags.put("Pipe", "Outlet");
//                pipe.tags.put("Water", (p.getKey().substring(0, 1)));
//                pipe.tags.put("ex height", p.getValue().height + "m");
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
        nw.addAll(catchments);
        System.out.println("Network building finished.");
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
            }
        }
    }

    private void readSubcatchments(BufferedReader br) throws IOException {
        String line = "";
        String[] parts;
        String name, outlet, area, impervPercent, width, slopePercent, pondedarea;
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
            outlet = parts[2];
            area = parts[3];
            impervPercent = parts[4];
            width = parts[5];
            slopePercent = parts[6];
            Subcatchment s = new Subcatchment(name, outlet, Double.parseDouble(area), Double.parseDouble(impervPercent) * 0.01, Double.parseDouble(width), Double.parseDouble(slopePercent) * 0.01);
            subcatchments.put(name, s);
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
            if (line.startsWith("[")) {
                br.reset();
                return;
            }
            if (line.startsWith(";;-")) {
                String[] l = line.split(" ");
                partends = new int[l.length];
                int start = 0;
                for (int i = 0; i < l.length; i++) {
                    partends[i] = start + l[i].length();
                    start += l[i].length() + 1;
                }
                break;
            }
        }
        try {
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
                name = parts[0].trim();//line.substring(0, partends[0]).trim();
                invert = parts[1].trim();// line.substring(partends[0] + 1, partends[1]).trim();
                type = parts[2].trim();//line.substring(partends[1] + 1, partends[2]).trim();
                data = parts[3].trim();//line.substring(partends[2] + 1, partends[3]);
                gated = parts[4].trim();//line.substring(partends[3] + 1, Math.min(line.length(), partends[4])).trim();
                boolean gate = false;
                if (gated.toLowerCase().equals("yes")) {
                    gate = true;
                }
//            System.out.println("'"+name+"' '"+invert+"' '"+type+"' '"+data+"' '"+gated+"'");
                Outfall j = new Outfall(name, Double.parseDouble(invert), type, data, gate);
                outfalls.put(name, j);
            }
        } catch (Exception exception) {
            System.err.println("Problem with line '" + line + "'");
            throw exception;
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
            param1 = 0;
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
        String name, elevation, maxdepth, initdepth, param1, param2, param3, param4, pondedarea, evaporFract;
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
            param1 = parts[5];
            param2 = parts[6];
            param3 = parts[7];
            param4 = parts[8];

            pondedarea = parts[9];
            evaporFract = parts[10];
            Storage s = new Storage(name, Double.parseDouble(elevation), Double.parseDouble(maxdepth), Double.parseDouble(initdepth), Double.parseDouble(param1), Double.parseDouble(param2), Double.parseDouble(param3), Double.parseDouble(param4), Double.parseDouble(pondedarea), Double.parseDouble(evaporFract));
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
        String[] parts;
        String name = "", x, y;
        LinkedList<Coordinate> polygon = new LinkedList<>();
        polygons = new HashMap<>();
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

    private class Node {

        public String name;

        public Node(String name) {
            this.name = name;
        }

    }

    private class Subcatchment extends Node {

        public String outletNode;
        public double totalarea, imperviousRatio, width, slopetotal;

        public Subcatchment(String name, String outletNode, double totalarea, double imperviousRatio, double width, double slopetotal) {
            super(name);
            this.outletNode = outletNode;
            this.totalarea = totalarea;
            this.imperviousRatio = imperviousRatio;
            this.width = width;
            this.slopetotal = slopetotal;
        }

    }

    private class Junction extends Node {

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

    private class Outfall extends Node {

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

    private class Divider extends Node {

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

    private class Storage extends Node {

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

    private class Edge {

        String name;
        String fromNode, toNode;

        public Edge(String name, String fromNode, String toNode) {
            this.name = name;
            this.fromNode = fromNode;
            this.toNode = toNode;
        }

    }

    private class Conduit extends Edge {

        double length, roughness;
        double offsetheightIn, offsetheightOut;
        String tag;

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

    private class Orifice extends Edge {

        String type;
        double height;

        public Orifice(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }
    }

    private class Weir extends Edge {

        String type;
        double height;

        public Weir(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }

    }

    private class Outlet extends Edge {

        String type;
        double height;

        public Outlet(String name, String fromNode, String toNode, String type, double height) {
            super(name, fromNode, toNode);
            this.type = type;
            this.height = height;
        }

    }

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
            System.out.println("Nodes: " + nodeCount + "   pipes: " + pipecount + "   subcatchments: " + subcatchmentCount);

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
                    System.out.println("Position: " + line.lastIndexOf(". "));
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
            System.out.println("Start date= " + startdate);
            System.out.println("End   date= " + enddate);
            System.out.println("reporttime = " + reportTime + " ms");
            double timestampsD = ((enddate.getTime() - startdate.getTime()) / (double) reportTime);
            int timestamps = (int) timestampsD;
            System.out.println("timestamp fraction=" + timestampsD);
            if (timestampsD % 1 != 0) {
                System.out.println("mit Rest : " + (timestampsD % 1));
                timestamps = (int) (timestampsD + 1);
            }
            System.out.println("Timestamps: " + timestamps);
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
                        tmh.setWaterZ(Float.parseFloat(line.substring(line.lastIndexOf(" ") + 1)), timecounter);
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

  
}
