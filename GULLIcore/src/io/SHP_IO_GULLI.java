package io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.surface.Surface;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.profile.CircularProfile;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class SHP_IO_GULLI {

    /**
     *
     * @param nw
     * @param collectionDirectory shp files are stored as files inside this
     * directory.
     * @param networkname Basename for the shapefile. layer names are added
     *
     */
    public static void write(Network nw, File collectionDirectory, String networkname) {
        try {
            String name = collectionDirectory.getName();
            if (name.contains(".")) {
                name = name.substring(0, name.indexOf("."));
                collectionDirectory = new File(collectionDirectory.getParent() + File.separator + name);
                System.out.println("New Directory '" + collectionDirectory.getAbsolutePath() + "'");
            }
            if (!collectionDirectory.exists()) {
                collectionDirectory.mkdirs();
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Transaction createtransaction = new DefaultTransaction("create");
            GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();// new GeometryFactory();
            // Manholes
            File outfile = new File(collectionDirectory + File.separator + networkname + "_manholes.shp");
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Manhole Schema
            final SimpleFeatureType MANHOLE = DataUtilities.createType("Manhole",
                    "the_geom:Point:srid=4326," + // <- the geometry attribute: Point type
                    "name:String," + //+  <- a String attribute
                    "he_id:int,"
                    + "sohle:float,"
                    + "surface:float"
            );

            datastore.createSchema(MANHOLE);
            datastore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(MANHOLE);

            DefaultFeatureCollection collection = new DefaultFeatureCollection();
            for (Manhole manhole : nw.getManholes()) {
                Point p = gf.createPoint(manhole.getPosition().lonLatCoordinate());
//                System.out.println("Point="+p);
                String n = manhole.getName();
//                String type = manhole.getWaterType().name();
                sfb.add(p);
                sfb.add(n);
                sfb.add((int) manhole.getManualID());
                sfb.add(manhole.getSole_height());
                sfb.add(manhole.getSurface_height());

                SimpleFeature f = sfb.buildFeature(null);
//                System.out.println("build " + f);
                collection.add(f);
            }
            String typeName = datastore.getTypeNames()[0];
//            System.out.println("Typename: " + typeName);
            SimpleFeatureSource featureSource = datastore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(createtransaction);
                try {
                    featureStore.addFeatures(collection);
                    createtransaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();

                } finally {
                    createtransaction.close();
                }
//                System.exit(0); // success!
            } else {
                System.out.println(typeName + " does not support read/write access");
//                System.exit(1);
            }

            ////////////////////////////////PIPES
            createtransaction = new DefaultTransaction("create");
            outfile = new File(collectionDirectory + File.separator + networkname + "_pipes.shp");
            params = new HashMap<String, Serializable>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Manhole Schema
            final SimpleFeatureType PIPE = DataUtilities.createType("Pipe",
                    "the_geom:LineString:srid=4326," + // <- the geometry attribute: Point type
                    "name:String," + //+  <- a String attribute
                    "he_id:int,"
                    + "gefaellle%:double,"
                    + "zSohle_st:double,"
                    + "zSohle_end:double,"
                    + "diameter:double"
            );

            datastore.createSchema(PIPE);
            datastore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            sfb = new SimpleFeatureBuilder(PIPE);

            collection = new DefaultFeatureCollection();
            for (Pipe pipe : nw.getPipes()) {

                Position start = pipe.getStartConnection().getPosition();
                Position ende = pipe.getEndConnection().getPosition();
                LineString ls = gf.createLineString(new Coordinate[]{start.lonLatCoordinate(), ende.lonLatCoordinate()});
//                System.out.println("Point="+p);
                String n = pipe.getName();
//                String type = manhole.getWaterType().name();
                sfb.add(ls);
                sfb.add(n);
                sfb.add((int) pipe.getManualID());
                sfb.add((double) (pipe.getDecline() * 100));
                sfb.add(pipe.getStartConnection().getHeight());
                sfb.add(pipe.getEndConnection().getHeight());
                if (pipe.getProfile() instanceof CircularProfile) {
                    sfb.add(((CircularProfile) pipe.getProfile()).getDiameter());
                } else {
                    sfb.add(-1);
                }

                SimpleFeature f = sfb.buildFeature(null);
//                System.out.println("build " + f);
                collection.add(f);
            }
            typeName = datastore.getTypeNames()[0];
//            System.out.println("Typename: " + typeName);
            featureSource = datastore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(createtransaction);
                try {
                    featureStore.addFeatures(collection);
                    createtransaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();

                } finally {
                    createtransaction.close();
                }
                System.exit(0); // success!
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Network readSHPNetwork(File file) throws MalformedURLException, IOException, FactoryException {
        return SHP_IO_GULLI.readSHPNetwork(file, false);
    }

    /**
     *
     * @param file
     * @param switchCoordinates switch x/y coordinate (may be necessary for
     * lat/lon converter
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws FactoryException
     */
    public static Network readSHPNetwork(File file, boolean switchCoordinates) throws MalformedURLException, IOException, FactoryException {
        ShapefileDataStore s = new ShapefileDataStore(file.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();
        HashMap<String, Manhole> volumes = new HashMap<>();
        HashMap<String, Pipe> pipes = new HashMap<>();

        CoordinateReferenceSystem shpCRS = fs.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crs = Network.crsWGS84; //WGS84
        MathTransform transformSHP_WGS84 = CRS.findMathTransform(shpCRS, crs, true);
        MathTransform transformSHP_UTM;
        if (Network.crsUTM != null) {
            transformSHP_UTM = CRS.findMathTransform(shpCRS, Network.crsUTM, true);
        } else {
            Network.crsUTM = shpCRS;
            transformSHP_UTM = CRS.findMathTransform(shpCRS, Network.crsUTM, true);
        }

        // Process shapefile
        FeatureIterator iterator = fc.features();
        String filename = file.getName();
        String name = "";
        try {
            int count = 0;
            while (iterator.hasNext()) {
                count++;

                SimpleFeatureImpl f = (SimpleFeatureImpl) iterator.next();

                Collection<Property> props = f.getProperties();
                Geometry geom = null;//(Geometry) f.getAttribute("the_geom");

                long id = Long.parseLong(f.getAttribute("osm_id").toString());

//                Tags tags = new Tags(props.size() - 2);
                name = "";
                for (Property prop : props) {
                    if (prop == null) {
                        continue;
                    }
//                    System.out.println("   " + prop.getName() + ":" + prop.getValue());
                    if (prop.getValue() instanceof Geometry) {
                        geom = (Geometry) prop.getValue();
                        continue;
                    }
                    if (prop.getName() == null) {
                        continue;
                    }
                    String key = prop.getName().toString();
                    if (prop.getName().toString().contains("geom")) {
                        continue;
                    }
                    if (key.equals("timestamp")) {
                        continue;
                    }
                    if (key.equals("name")) {
                        name = prop.getValue() + "";
                    }
                    if (prop.getValue() == null) {
                        continue;
                    }
                    String value = prop.getValue().toString();
                    if (value == null || value.isEmpty() || value.equals("0") || value.equals("F")) {
                        continue;
                    }

//                    tags.put(key, value);
                }
                if (geom == null) {
                    System.out.println("Keine Geometrie ");
                    Collection<Property> ps = f.getProperties();
                    for (Property p : ps) {
                        System.out.println("  " + p);
                    }
                    continue;
                }
                if (switchCoordinates) {
                    Coordinate[] o = geom.getCoordinates();
                    for (Coordinate o1 : o) {
                        double x = o1.x;
                        o1.x = o1.y;
                        o1.y = x;
                    }
                    geom.geometryChanged();
                }

                if (geom instanceof com.vividsolutions.jts.geom.Point) {
                    com.vividsolutions.jts.geom.Point p = (com.vividsolutions.jts.geom.Point) geom;
                    Point pUTM = transform(p, transformSHP_UTM);
                    Point pWGS = transform(p, transformSHP_WGS84);
                    Position pos = new Position(pWGS.getX(), pWGS.getY(), pUTM.getX(), pUTM.getY());
//                    Node n = new Node(autoID, tags, pos);

                    Manhole mh = new Manhole(pos, name, null);
                    volumes.put(name, mh);
                } else if (geom instanceof com.vividsolutions.jts.geom.MultiPolygon) {
//                    com.vividsolutions.jts.geom.MultiPolygon p = (com.vividsolutions.jts.geom.MultiPolygon) transform(geom, transformSHP_WGS84);
//                    Way w = new Way(autoID, tags, geom, true);
                    Pipe pipe = new Pipe(null, null, null);
                    pipe.setName(name);
//                    pipe.tags = tags;
                    pipes.put(name, pipe);
                } else if (geom instanceof com.vividsolutions.jts.geom.MultiLineString) {
                    Pipe pipe = new Pipe(null, null, null);
                    pipe.setName(name);
//                    pipe.tags = tags;
                    pipes.put(name, pipe);
                } else {
                    System.err.println(SHP_IO_GULLI.class.getSimpleName() + "::readSHP() : Geometrie des Typs " + geom.getClass() + " nicht bekannt um verarbeitet zu werden.");
                }
            }
            iterator.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            s.getFeatureReader().close();
            s.dispose();
        }
        return new Network(pipes.values(), volumes.values());

    }

    /**
     * Reads the easiest geometry or creates a multi-geometry containing all
     * objects.
     *
     * @param file
     * @param switchCoordinates
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws FactoryException
     */
    public static Geometry readSHP_asSingleGeometry(File file, boolean switchCoordinates) throws MalformedURLException, IOException, FactoryException {
        ArrayList<Geometry> list = readSHP(file, switchCoordinates);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            if (list.get(0).getNumGeometries() == 1) {
                return list.get(0).getGeometryN(0);
            }
            return list.get(0);
        }
        ArrayList<Geometry> simpleElements = new ArrayList<>(list.size());
        //Go thorugh all elements and put primitive elements into the list
        for (Geometry lg : list) {
            for (int i = 0; i < lg.getNumGeometries(); i++) {
                Geometry g = lg.getGeometryN(i);
                if (g.getNumGeometries() == 1) {
                    simpleElements.add(g);
                } else {
                    for (int j = 0; j < g.getNumGeometries(); j++) {
                        simpleElements.add(g.getGeometryN(j));
                    }
                }
            }
        }
        if (simpleElements.isEmpty()) {
            return null;
        }
        GeometryFactory gf = simpleElements.get(0).getFactory();
        return gf.buildGeometry(simpleElements);
    }

    /**
     *
     * @param file
     * @param switchCoordinates switch x/y coordinate (may be necessary for
     * lat/lon converter
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws FactoryException
     */
    public static ArrayList<Geometry> readSHP(File file, boolean switchCoordinates) throws MalformedURLException, IOException, FactoryException {
        ShapefileDataStore s = new ShapefileDataStore(file.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();

//        CoordinateReferenceSystem shpCRS = fs.getSchema().getCoordinateReferenceSystem();
        // Process shapefile
        FeatureIterator iterator = fc.features();
//        String filename = file.getName();
        ArrayList<Geometry> geometries = new ArrayList<>(fc.size());
//        String name = "";
        try {
            int count = 0;
            while (iterator.hasNext()) {
                count++;

                SimpleFeatureImpl f = (SimpleFeatureImpl) iterator.next();

                Collection<Property> props = f.getProperties();
                Geometry geom = null;//(Geometry) f.getAttribute("the_geom");

                for (Property prop : props) {
                    if (prop == null) {
                        continue;
                    }
//                    System.out.println("   " + prop.getName() + ":" + prop.getValue());
                    if (prop.getValue() instanceof Geometry) {
                        geom = (Geometry) prop.getValue();
                        break;
                    }

                }
                if (geom == null) {
                    System.out.println("No Geometry in element " + count + " of " + file.getName());
                    continue;
                }
//                System.out.println("Geometry of type: "+geom.getGeometryType());
                if (switchCoordinates) {
                    Coordinate[] o = geom.getCoordinates();
                    for (Coordinate o1 : o) {
                        double x = o1.x;
                        o1.x = o1.y;
                        o1.y = x;
                    }
                    geom.geometryChanged();
                }
                geometries.add(geom);
            }
            iterator.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            s.getFeatureReader().close();
            s.dispose();
        }
        return geometries;

    }

    /**
     * Reads in points from a Shapefile (position of street inlets) and connects
     * them with the neares pipe.
     *
     * @param inletNodesFile
     * @param network
     * @throws FactoryException
     * @throws IOException
     */
    public static void applyStreetInlets(File inletNodesFile, Network network) throws FactoryException, IOException {
        ShapefileDataStore s = new ShapefileDataStore(inletNodesFile.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();
        CoordinateReferenceSystem shpCRS = fs.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crs = Network.crsWGS84; //WGS84
        MathTransform transformSHP_WGS84 = CRS.findMathTransform(shpCRS, crs, true);
        MathTransform transformSHP_UTM;
        if (Network.crsUTM != null) {
            transformSHP_UTM = CRS.findMathTransform(shpCRS, Network.crsUTM, true);
        } else {
            Network.crsUTM = shpCRS;
            transformSHP_UTM = CRS.findMathTransform(shpCRS, Network.crsUTM, true);
        }

        LinkedList<Inlet> inlets = new LinkedList<>();

        //Find attribute indizes
        int indexGeometry = fs.getSchema().indexOf("the_geom");
        for (int i = 0; i < fs.getSchema().getAttributeCount(); i++) {
            if (fs.getSchema().getType(i).getBinding().getCanonicalName().contains("jts.geom.")) {
                indexGeometry = i;
            }
        }

        // Process shapefile
        HashSet<Position> positions = new HashSet(fc.size());
        FeatureIterator iterator = fc.features();
        String name = "";
        try {
            int count = 0;
            while (iterator.hasNext()) {
                count++;

                SimpleFeatureImpl f = (SimpleFeatureImpl) iterator.next();

                Geometry geom = (Geometry) f.getAttribute(indexGeometry);

                if (geom == null) {
                    System.out.println("Keine Geometrie ");
                    continue;
                }

                if (geom instanceof com.vividsolutions.jts.geom.Point) {
                    com.vividsolutions.jts.geom.Point p = (com.vividsolutions.jts.geom.Point) geom;
                    Point pwgs84 = transform(p, transformSHP_WGS84);
                    Point pUTM = transform(p, transformSHP_UTM);
                    positions.add(new Position(pwgs84.getX(), pwgs84.getY(), pUTM.getX(), pUTM.getY()));
                }
            }
            iterator.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            s.getFeatureReader().close();
            s.dispose();
        }

        GeometryFactory gf = new GeometryFactory();
        //Inlets an Pipes matchen
        //TODO: utm may not be consistent. actual only latlon matching (better than nothing)
        long starttime = System.currentTimeMillis();
        for (Position p : positions) {
            double distance = Double.POSITIVE_INFINITY;
            Pipe bestpipe = null;
            Coordinate c = p.lonLatCoordinate();
            Point point = gf.createPoint(c);
            for (Pipe pipe : network.getPipes()) {
                LineString ls = gf.createLineString(new Coordinate[]{pipe.getStartConnection().getPosition().lonLatCoordinate(), pipe.getEndConnection().getPosition().lonLatCoordinate()});
                double dt = ls.distance(point);
                if (dt < distance) {
                    distance = dt;
                    bestpipe = pipe;
                }
            }
//            System.out.println("distance: "+distance+" m to best pipe "+bestpipe);
            Coordinate[] cls = new Coordinate[]{bestpipe.getStartConnection().getPosition().lonLatCoordinate(), bestpipe.getEndConnection().getPosition().lonLatCoordinate()};
            LineString ls = gf.createLineString(cls);

            Coordinate[] cs = DistanceOp.nearestPoints(point, ls);

            double pos1D = (cs[1].distance(cls[0]));
            Inlet inlet = new Inlet(p, bestpipe, pos1D);
            inlets.add(inlet);
        }
//        System.out.println("  Mapping " + inlets.size() + " Streetinlets took " + ((System.currentTimeMillis() - starttime) / 1000) + "s.");

        network.setStreetInlets(inlets);
    }

    public static void readTriangleFileToSurface(File file, Surface surf, boolean verbose) throws MalformedURLException, IOException, Exception {
        surf.fileWaterlevels = file;
        ShapefileDataStore s = new ShapefileDataStore(file.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();

        // Process shapefile
        FeatureIterator iterator = fc.features();
        int zehntelschritte = fc.size() / 10;
        long starttime = System.currentTimeMillis();
        int acount = fc.getSchema().getAttributeCount();
        int wlcount = 0;
        for (int i = 0; i < acount; i++) {
            if (fc.getSchema().getType(i).getName().toString().contains("WL_")) {
                wlcount++;
            }
        }

        int numberofSurfaceTriangles = 0;
        if (surf != null && surf.getTriangleNodes() != null) {
            numberofSurfaceTriangles = surf.getTriangleNodes().length;
        }

        //Has the Surface filtered during loading? 
        boolean filtered = surf.getMapIndizes() != null;

        float[][] waterlevels;
        if (filtered) {
            waterlevels = new float[surf.getTriangleNodes().length][wlcount];
        } else {
            waterlevels = new float[fc.size()][wlcount];
        }
        double[] maxWaterLevels = new double[waterlevels.length];

        int[] indexWL = new int[wlcount];
        for (int i = 0; i < indexWL.length; i++) {
            indexWL[i] = fc.getSchema().indexOf("WL_" + i);

        }
        int indexZ = fc.getSchema().indexOf("Z");
        int indexID = fc.getSchema().indexOf("ID");
        int indexLMaxWlvl = fc.getSchema().indexOf("WLevelMax");
        try {
            int count = 0;
            int id;
            double z, maxWlvl;
            while (iterator.hasNext()) {
                count++;
                if (verbose && count % zehntelschritte == 0) {
                    System.out.println(" " + ((int) (count * 100. / fc.size())) + "%\t" + (int) ((System.currentTimeMillis() - starttime) / 1000) + " s.");
                }
                SimpleFeatureImpl f = (SimpleFeatureImpl) iterator.next();

                id = (int) (f.getAttribute(indexID));
                if (filtered) {
                    //Search for the new ID of the triangle
                    if (!surf.getMapIndizes().containsKey(id)) {
                        //This Triangle is not part of the loaded surface.
                        continue;
                    }
                    id = surf.getMapIndizes().get(id);
                }
                z = ((Double) f.getAttribute(indexZ));

                //This Information of elevation z makes the Waterlevel calculation more accurate than the midpoint calculation during surface loading. (Do not know why)
                surf.getTriangleMids()[id][2] = z;

                maxWlvl = ((Double) f.getAttribute(indexLMaxWlvl));
                maxWaterLevels[id] = maxWlvl;

                //Zeitreihen lesen
                for (int i = 0; i < wlcount; i++) {
                    waterlevels[id][i] = ((Double) f.getAttribute(indexWL[i])).floatValue();
                }
            }
            iterator.close();
//            System.gc();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            s.getFeatureReader().close();
            s.dispose();
        }
        surf.setMaxWaterLevels(maxWaterLevels);
        surf.setWaterlevels(waterlevels);
    }

    public static <G extends Geometry> G transform(G g, MathTransform t) throws MismatchedDimensionException, TransformException {
        return (G) JTS.transform(g, t);
    }

    public static Coordinate transform(Coordinate g, MathTransform t) throws MismatchedDimensionException, TransformException {
        return JTS.transform(g, null, t);
    }

    public static Coordinate[] transform(Coordinate[] c, MathTransform t) throws MismatchedDimensionException, TransformException {
        Coordinate[] cn = new Coordinate[c.length];
        for (int i = 0; i < cn.length; i++) {
            cn[i] = JTS.transform(c[i], null, t);
        }
        return cn;
    }

    public static void writeWGS84(Geometry geom, String filePathName, String layername, boolean switchCoordinates) {
        ArrayList<Geometry> collection = new ArrayList<>(1);
        collection.add(geom);
        writeWGS84(collection, filePathName, layername, switchCoordinates);
    }

    /**
     * Writes out Polygons to a shp file
     *
     * @param collection
     * @param filePathName
     * @param layername
     * @param switchCoordinates
     */
    public static void writeWGS84(Collection<Geometry> collection, String filePathName, String layername, boolean switchCoordinates) {
        if (collection.isEmpty()) {
            System.err.println("Do not create SHP file for empty collection. @" + filePathName);
            return;
        }
        try {
//            String name = filePathName;
//            if (name.contains(".")) {
//                name = name.substring(0, name.indexOf("."));
//                collectionDirectory = new File(collectionDirectory.getParent() + File.separator + name);
//                System.out.println("New Directory '" + collectionDirectory.getAbsolutePath() + "'");
//            }

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".shp")) {
                filePathName += ".shp";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Transaction createtransaction = new DefaultTransaction("create");
//            GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();// new GeometryFactory();
            // Manholes

            Map<String, Serializable> params = new HashMap<>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Decide on the first geometry of which type this shapefile is
            Class<? extends Geometry> type = collection.iterator().next().getClass();
            if (type.getSimpleName().equals("LinearRing")) {
                type = LineString.class;
            }

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(layername,
                    "the_geom:" + type.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
            );

            datastore.createSchema(FEATURE);
            datastore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(FEATURE);

            DefaultFeatureCollection dfcollection = new DefaultFeatureCollection();
            for (Geometry g : collection) {
                //Change coords from lat/long to long/lat
                if (!type.isAssignableFrom(g.getClass())) {
                    System.out.println("Problems may occure when object of '" + g.getGeometryType() + "' is stored in Shapefile for '" + type.getSimpleName() + "'. @" + filePathName);
                }
                if (switchCoordinates) {
                    g = (Geometry) g.clone();
                    for (int i = 0; i < g.getCoordinates().length; i++) {
                        double tempX = g.getCoordinates()[i].x;
                        g.getCoordinates()[i].x = g.getCoordinates()[i].y;
                        g.getCoordinates()[i].y = tempX;
                    }
                    g.geometryChanged();
                }
                sfb.add(g);
                SimpleFeature f = sfb.buildFeature(null);
                dfcollection.add(f);
            }
            String typeName = datastore.getTypeNames()[0];
//            System.out.println("Typename: " + typeName);
            ContentFeatureSource featureSource = datastore.getFeatureSource(typeName);

            try {
                SimpleFeatureStore sfs = (SimpleFeatureStore) featureSource;
                sfs.setTransaction(createtransaction);
                try {
                    sfs.addFeatures(dfcollection);
                    createtransaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();

                } finally {
                    createtransaction.close();
                }
                sfs.getDataStore().dispose();
            } catch (Exception ex) {
                System.err.println("Exception with Shapefile Feature Store of type " + featureSource.getClass() + "  \n" + ex.getLocalizedMessage());
                if (featureSource instanceof SimpleFeatureStore) {
                    SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                    featureStore.setTransaction(createtransaction);
                    try {
                        featureStore.addFeatures(dfcollection);
                        createtransaction.commit();

                    } catch (Exception problem) {
                        problem.printStackTrace();
                        createtransaction.rollback();

                    } finally {
                        createtransaction.close();

                    }
                } else {
                    System.out.println(typeName + " does not support read/write access: " + featureSource);
                }
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeWGS84_Filtered(Collection<Geometry> collection, Class geometryFilter, String filePathName, boolean switchCoordinates) {
        try {
//            String name = filePathName;
//            if (name.contains(".")) {
//                name = name.substring(0, name.indexOf("."));
//                collectionDirectory = new File(collectionDirectory.getParent() + File.separator + name);
//                System.out.println("New Directory '" + collectionDirectory.getAbsolutePath() + "'");
//            }

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".shp")) {
                filePathName += ".shp";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Transaction createtransaction = new DefaultTransaction("create");
//            GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();// new GeometryFactory();
            // Manholes

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(geometryFilter.getSimpleName(),
                    "the_geom:" + geometryFilter.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
            );

            datastore.createSchema(FEATURE);
            datastore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(FEATURE);

            DefaultFeatureCollection dfcollection = new DefaultFeatureCollection();
            for (Geometry g : collection) {
                if (g.getClass().isAssignableFrom(geometryFilter)) {
                    //Change coords from lat/long to long/lat
                    if (switchCoordinates) {
                        g = (Geometry) g.clone();
                        for (int i = 0; i < g.getCoordinates().length; i++) {
                            double tempX = g.getCoordinates()[i].x;
                            g.getCoordinates()[i].x = g.getCoordinates()[i].y;
                            g.getCoordinates()[i].y = tempX;
                        }
                        g.geometryChanged();
                    }
                    sfb.add(g);
                    SimpleFeature f = sfb.buildFeature(null);
                    dfcollection.add(f);
                }
            }
            String typeName = datastore.getTypeNames()[0];
//            System.out.println("Typename: " + typeName);
            SimpleFeatureSource featureSource = datastore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(createtransaction);
                try {
                    featureStore.addFeatures(dfcollection);
                    createtransaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();

                } finally {
                    createtransaction.close();
                }
//                System.exit(0); // success!
            } else {
                System.out.println(typeName + " does not support read/write access");
//                System.exit(1);
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeWGS84_Converted(Collection<Geometry> collection, Class geometryType, String filePathName, boolean switchCoordinates) {
        try {
            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".shp")) {
                filePathName += ".shp";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Transaction createtransaction = new DefaultTransaction("create");

            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(geometryType.getSimpleName(),
                    "the_geom:" + geometryType.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
            );
            datastore.createSchema(FEATURE);
            datastore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(FEATURE);

            DefaultFeatureCollection dfcollection = new DefaultFeatureCollection();
            for (Geometry g : collection) {
                //Change coords from lat/long to long/lat
                if (switchCoordinates) {
                    g = (Geometry) g.clone();
                    for (int i = 0; i < g.getCoordinates().length; i++) {
                        double tempX = g.getCoordinates()[i].x;
                        g.getCoordinates()[i].x = g.getCoordinates()[i].y;
                        g.getCoordinates()[i].y = tempX;
                    }
                    g.geometryChanged();
                }
                sfb.add(g);
                SimpleFeature f = sfb.buildFeature(null);
                dfcollection.add(f);
            }
            String typeName = datastore.getTypeNames()[0];
            SimpleFeatureSource featureSource = datastore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(createtransaction);
                try {
                    featureStore.addFeatures(dfcollection);
                    createtransaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();
                } finally {
                    createtransaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static float[] readMaxWaterLevels(File lvlshpfile) throws MalformedURLException, IOException {
        ShapefileDataStore s = new ShapefileDataStore(lvlshpfile.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();
        float[] lvls = new float[fc.size()];
        FeatureIterator iterator = fc.features();

        int zehntelschritte = fc.size() / 10;
        long starttime = System.currentTimeMillis();
        int acount = fc.getSchema().getAttributeCount();
        int wlcount = 0;
        int indexID = fc.getSchema().indexOf("ID");
        int indexLMaxWlvl = fc.getSchema().indexOf("WLevelMax");
        try {
            int count = 0;
            int id;
            float z, maxWlvl;
            while (iterator.hasNext()) {
                count++;
//                if (verbose && count % zehntelschritte == 0) {
//                    System.out.println(" " + ((int) (count * 100. / fc.size())) + "%\t" + (int) ((System.currentTimeMillis() - starttime) / 1000) + " s.");
//                }
                SimpleFeatureImpl f = (SimpleFeatureImpl) iterator.next();
                id = (int) (f.getAttribute(indexID));
                maxWlvl = ((Double) f.getAttribute(indexLMaxWlvl)).floatValue();
                lvls[id] = maxWlvl;
            }
            iterator.close();
//            System.gc();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            s.getFeatureReader().close();
            s.dispose();
        }
        return lvls;
    }

    /**
     * Stores Found Ponds in the given file. Mainly used for Surface-Pond-Metric
     * output.
     *
     * @param filePathName File to be used. *.shp .
     * @param geoms Polygons to store
     * @param id IDs of Polygons to be added to the attribute table. (Minimums
     * point id)
     * @param srid e.g. 25832
     * @param epsgString e.g. "EPSG:25832
     *
     */
    public static void writePonds(String filePathName, Geometry[] geoms, int[] id, int srid, String epsgString) {
        try {

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".shp")) {
                filePathName += ".shp";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Transaction createtransaction = new DefaultTransaction("create");
//            GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();// new GeometryFactory();
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", outfile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore datastore = (ShapefileDataStore) factory.createNewDataStore(params);

            //Manhole Schema
            final SimpleFeatureType SCHEMETYPE = DataUtilities.createType("Geometry",
                    "the_geom:Polygon:srid=" + srid + "," + // <- the geometry attribute
                    "locmin_id:int,"
                    + "area:float"
            );

            datastore.createSchema(SCHEMETYPE);
//            datastore.forceSchemaCRS(CRS.decode(epsgString));

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(SCHEMETYPE);

            DefaultFeatureCollection collection = new DefaultFeatureCollection();
            int counter1 = 0;
            for (int i = 0; i < geoms.length; i++) {

                Geometry g = geoms[i];
                if (g == null) {
                    continue;
                }
                Polygon[] polys = null;
                if (g instanceof Polygon) {
                    polys = new Polygon[]{(Polygon) g};
//                    System.out.println("Regular Polygon "+i);
                } else if (g instanceof MultiPolygon) {
                    MultiPolygon mp = (MultiPolygon) g;
                    polys = new Polygon[mp.getNumGeometries()];
                    int counter = 0;
                    for (int j = 0; j < g.getNumGeometries(); j++) {
                        Geometry g1 = g.getGeometryN(j);
                        if (g1 instanceof Polygon) {
                            polys[j] = (Polygon) g1;
                            counter++;
                        }
                    }
                    System.out.println("Split Geometry " + i + " into " + counter + " Polygons.");
                } else {
                    System.out.println("Could not build Polygon out of " + g.getGeometryType());
                }
                if (polys != null) {
                    for (Polygon poly : polys) {
                        if (poly == null) {
                            continue;
                        }
                        sfb.add(poly);
                        sfb.add(i);
                        sfb.add(poly.getArea());
                        SimpleFeature f = sfb.buildFeature("" + counter1++);
                        collection.add(f);
                    }
                }
            }
            String typeName = datastore.getTypeNames()[0];
            SimpleFeatureSource featureSource = datastore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(createtransaction);
                try {
                    featureStore.addFeatures(collection);
                    createtransaction.commit();

                } catch (Exception problem) {
                    problem.printStackTrace();
                    createtransaction.rollback();

                } finally {
                    createtransaction.close();
                }
//                System.exit(0); // success!
            } else {
                System.out.println(typeName + " does not support read/write access");
//                System.exit(1);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
