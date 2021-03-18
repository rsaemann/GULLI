/*
 * The MIT License
 *
 * Copyright 2020 saemann.
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
package com.saemann.gulli.core.io;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

/**
 * Read and write to *.gpkg databases
 *
 * @author saemann
 */
public class Geopackage_IO {

    /**
     *
     * @param nw
     * @param collectionDirectory shp files are stored as files inside this
     * directory.
     * @param networkname Basename for the shapefile. layer names are added
     *
     */
    public static void writeNetwork(Network nw, File collectionDirectory, String networkname) {
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

            // Manholes
            File outfile = new File(collectionDirectory + File.separator + networkname + ".gpkg");
            if (outfile.exists()) {
                if (outfile.delete()) {

                } else {
                    System.err.println("File is in use and cannot be overwritten: " + outfile.getAbsolutePath());
                }
            }

            GeoPackage geopackage = new GeoPackage(outfile);
            geopackage.init(); //Initialize database tables
            geopackage.addCRS(4326); //set Coordinate reference system WGS84

            //Manhole Schema
            final SimpleFeatureType MANHOLE = DataUtilities.createType("Manhole",
                    "the_geom:Point:srid=4326,"
                    + // <- the geometry attribute: Point type
                    "name:String,"
                    + //+  <- a String attribute
                    "he_id:int,"
                    + "sohle:float,"
                    + "surface:float"
            );

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(MANHOLE);
            GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
            DefaultFeatureCollection collection = new DefaultFeatureCollection();
            double maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;
            double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;

            for (Manhole manhole : nw.getManholes()) {
                Point p = gf.createPoint(manhole.getPosition().lonLatCoordinate());
                String n = manhole.getName();
                sfb.add(p);
                sfb.add(n);
                sfb.add((int) manhole.getManualID());
                sfb.add(manhole.getSole_height());
                sfb.add(manhole.getSurface_height());
                maxLat = Math.max(maxLat, p.getY());
                maxLon = Math.max(maxLon, p.getX());
                minLat = Math.min(minLat, p.getY());
                minLon = Math.min(minLon, p.getX());

                SimpleFeature f = sfb.buildFeature(null);

                collection.add(f);
            }
            FeatureEntry fe = new FeatureEntry();
            fe.setDataType(Entry.DataType.Feature);
            fe.setGeometryColumn(MANHOLE.getGeometryDescriptor().getLocalName());
            fe.setLastChange(new Date());
            fe.setGeometryType(Geometries.getForName(MANHOLE.getGeometryDescriptor().getType().getName().getLocalPart()));
            Envelope boundingbox = new Envelope(minLon, maxLon, minLat, maxLat);
            ReferencedEnvelope bbox = new ReferencedEnvelope(boundingbox, CRS.decode("EPSG:4326"));
            fe.setBounds(bbox);

            geopackage.add(fe, collection);

            ////////////////////////////////PIPES
//           
            //Pipe Schema
            final SimpleFeatureType PIPE = DataUtilities.createType("Pipe",
                    "the_geom:LineString:srid=4326,"
                    + // <- the geometry attribute: Point type
                    "name:String,"
                    + //+  <- a String attribute
                    "he_id:int,"
                    + "gefaellle%:double,"
                    + "zSohle_st:double,"
                    + "zSohle_end:double,"
                    + "diameter:double"
            );

            fe = new FeatureEntry();
            fe.setLastChange(new Date());
            fe.setBounds(bbox);
            fe.setDataType(Entry.DataType.Feature);
            fe.setGeometryColumn(MANHOLE.getGeometryDescriptor().getLocalName());
            fe.setLastChange(new Date());
            fe.setGeometryType(Geometries.getForName(MANHOLE.getGeometryDescriptor().getType().getName().getLocalPart()));

            sfb = new SimpleFeatureBuilder(PIPE);
            collection = new DefaultFeatureCollection();
            for (Pipe pipe : nw.getPipes()) {

                Position start = pipe.getStartConnection().getPosition();
                Position ende = pipe.getEndConnection().getPosition();
                LineString ls = gf.createLineString(new Coordinate[]{start.lonLatCoordinate(), ende.lonLatCoordinate()});
                String n = pipe.getName();
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
                collection.add(f);
            }
            geopackage.add(fe, collection);
            geopackage.close();

        } catch (MalformedURLException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SchemaException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(Geopackage_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean write(Geometry geometry, String filePathName, String layername, boolean switchCoordinates, String srid) {
        ArrayList<Geometry> collection = new ArrayList<>(1);
        collection.add(geometry);
        return write(collection, filePathName, layername, switchCoordinates, srid);
    }

    /**
     * Writes out Geometries to a gpkg file
     *
     * @param collection
     * @param filePathName
     * @param layername
     * @param switchCoordinates
     */
    public static boolean write(Collection<Geometry> collection, String filePathName, String layername, boolean switchCoordinates, String srid) {
        if (collection.isEmpty()) {
            System.err.println("Do not create GPKG file for empty collection. @" + filePathName);
            return false;
        }
        try {

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".gpkg")) {
                filePathName += ".gpkg";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            GeoPackage geopackage = new GeoPackage(outfile);
            geopackage.init(); //Initialize database tables
            try {
                geopackage.addCRS(Integer.parseInt(srid)); //set Coordinate reference system WGS84

            } catch (Exception exc) {
                exc.printStackTrace();
            }
            //Decide on the first geometry of which type this shapefile is
            Class<? extends Geometry> type = collection.iterator().next().getClass();
            if (type.getSimpleName().equals("LinearRing")) {
                type = LineString.class;
            }

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(layername,
                    "the_geom:" + type.getSimpleName() + ":srid=" + srid
            );

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

            FeatureEntry fe = new FeatureEntry();
            fe.setLastChange(new Date());
            fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:" + srid)));
            fe.setDataType(Entry.DataType.Feature);
            fe.setGeometryColumn(FEATURE.getGeometryDescriptor().getLocalName());
            //fe.setLastChange(new Date());
            fe.setGeometryType(Geometries.getForName(FEATURE.getGeometryDescriptor().getType().getName().getLocalPart()));

            geopackage.add(fe, dfcollection);
            geopackage.close();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static boolean writeWGS84LineString(Collection<LineString> collection, String filePathName, String layername, boolean switchCoordinates) {
        Collection<Geometry> geoms = new ArrayList<>(collection.size());
        geoms.addAll(collection);
        return writeWGS84(geoms, filePathName, layername, switchCoordinates);
    }

    /**
     * Writes out Geometries to a gpkg file
     *
     * @param collection
     * @param filePathName
     * @param layername
     * @param switchCoordinates
     */
    public static boolean writeWGS84(Collection<Geometry> collection, String filePathName, String layername, boolean switchCoordinates) {
        if (collection.isEmpty()) {
            System.err.println("Do not create GPKG file for empty collection. @" + filePathName);
            return false;
        }
        try {

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".gpkg")) {
                filePathName += ".gpkg";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            try (GeoPackage geopackage = new GeoPackage(outfile)) {
                geopackage.init(); //Initialize database tables
                geopackage.addCRS(4326); //set Coordinate reference system WGS84

                //Decide on the first geometry of which type this shapefile is
                Class<? extends Geometry> type = collection.iterator().next().getClass();
                if (type.getSimpleName().equals("LinearRing")) {
                    type = LineString.class;
                }

                //Manhole Schema
                final SimpleFeatureType FEATURE = DataUtilities.createType(layername,
                        "the_geom:" + type.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
                );

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

                FeatureEntry fe = new FeatureEntry();
                fe.setLastChange(new Date());
                fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
                fe.setDataType(Entry.DataType.Feature);
                fe.setGeometryColumn(FEATURE.getGeometryDescriptor().getLocalName());
                fe.setLastChange(new Date());
                fe.setGeometryType(Geometries.getForName(FEATURE.getGeometryDescriptor().getType().getName().getLocalPart()));

                geopackage.add(fe, dfcollection);
            } //Initialize database tables
            return true;
        } catch (Exception ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * Writes out Geometries to a gpkg file
     *
     * @param collection
     * @param filePathName
     * @param layername
     */
    public static void writeWGS84_Pipes(Collection<Pipe> collection, String filePathName, String layername) {
        if (collection.isEmpty()) {
            System.err.println("Do not create SHP file for empty collection. @" + filePathName);
            return;
        }
        try {

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".gpkg")) {
                filePathName += ".gpkg";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            GeoPackage geopackage = new GeoPackage(outfile);
            geopackage.init(); //Initialize database tables
            geopackage.addCRS(4326); //set Coordinate reference system WGS84

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(layername,
                    "the_geom:LineString:srid=4326," // <- the geometry attribute: Polygon type in WGS84 Latlon
                    + // <- the geometry attribute: Point type
                    "name:String,"
                    + //+  <- a String attribute
                    "he_id:int,"
                    + "slope:double"
            );

            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(FEATURE);
            GeometryFactory gf = new GeometryFactory();
            DefaultFeatureCollection dfcollection = new DefaultFeatureCollection();
            for (Pipe p : collection) {
                Coordinate[] coords = new Coordinate[2];
                coords[0] = new Coordinate(p.getStartConnection().getPosition().getLongitude(), p.getStartConnection().getPosition().getLatitude());
                coords[1] = new Coordinate(p.getEndConnection().getPosition().getLongitude(), p.getEndConnection().getPosition().getLatitude());

                sfb.add(gf.createLineString(coords));
                sfb.add(p.getName());
                sfb.add(p.getManualID());
                sfb.add(p.getDecline());

                SimpleFeature f = sfb.buildFeature(null);
                dfcollection.add(f);
            }

            FeatureEntry fe = new FeatureEntry();
            fe.setLastChange(new Date());
            fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
            fe.setDataType(Entry.DataType.Feature);
            fe.setGeometryColumn(FEATURE.getGeometryDescriptor().getLocalName());
            fe.setLastChange(new Date());
            fe.setGeometryType(Geometries.getForName(FEATURE.getGeometryDescriptor().getType().getName().getLocalPart()));

            geopackage.add(fe, dfcollection);
            geopackage.close();

        } catch (Exception ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeWGS84_Filtered(Collection<Geometry> collection, Class geometryFilter, String filePathName, boolean switchCoordinates) throws SchemaException, FactoryException {
        try {

            File directory = new File(filePathName).getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (!filePathName.endsWith(".gpkg")) {
                filePathName += ".gpkg";
            }
            File outfile = new File(filePathName);
            if (outfile.exists()) {
                outfile.delete();
            }

            GeoPackage geopackage = new GeoPackage(outfile);
            geopackage.init(); //Initialize database tables
            geopackage.addCRS(4326); //set Coordinate reference system WGS84

            //Manhole Schema
            final SimpleFeatureType FEATURE = DataUtilities.createType(geometryFilter.getSimpleName(),
                    "the_geom:" + geometryFilter.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
            );

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
            FeatureEntry fe = new FeatureEntry();
            fe.setLastChange(new Date());
            fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
            fe.setDataType(Entry.DataType.Feature);
            fe.setGeometryColumn(FEATURE.getGeometryDescriptor().getLocalName());
            fe.setLastChange(new Date());
            fe.setGeometryType(Geometries.getForName(FEATURE.getGeometryDescriptor().getType().getName().getLocalPart()));

            geopackage.add(fe, dfcollection);
            geopackage.close();
        } catch (IOException ex) {
            Logger.getLogger(SHP_IO_GULLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeWGS84_Converted(Collection<Geometry> collection, Class geometryType, String filePathName, boolean switchCoordinates) throws SchemaException, IOException, FactoryException {
        File directory = new File(filePathName).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!filePathName.endsWith(".gpkg")) {
            filePathName += ".gpkg";
        }
        File outfile = new File(filePathName);
        if (outfile.exists()) {
            outfile.delete();
        }

        GeoPackage geopackage = new GeoPackage(outfile);
        geopackage.init();
        geopackage.addCRS(4326);

        //the_geom Schema
        final SimpleFeatureType FEATURE = DataUtilities.createType(geometryType.getSimpleName(),
                "the_geom:" + geometryType.getSimpleName() + ":srid=4326" // <- the geometry attribute: Polygon type in WGS84 Latlon
        );

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
        FeatureEntry fe = new FeatureEntry();
        fe.setLastChange(new Date());
        fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
        fe.setDataType(Entry.DataType.Feature);
        fe.setGeometryColumn(FEATURE.getGeometryDescriptor().getLocalName());
        fe.setLastChange(new Date());
        fe.setGeometryType(Geometries.getForName(FEATURE.getGeometryDescriptor().getType().getName().getLocalPart()));

        geopackage.add(fe, dfcollection);
        geopackage.close();

    }

    public static void writePonds(String filePathName, Geometry[] geoms, int[] id, int srid, String epsgString) throws SchemaException, FactoryException, IOException {

        File directory = new File(filePathName).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!filePathName.endsWith(".gpkg")) {
            filePathName += ".gpkg";
        }
        File outfile = new File(filePathName);
        if (outfile.exists()) {
            outfile.delete();
        }

        GeoPackage geopackage = new GeoPackage(outfile);
        geopackage.init();
        geopackage.addCRS(4326);

        //Geometry Schema
        final SimpleFeatureType SCHEMETYPE = DataUtilities.createType("Geometry",
                "the_geom:Polygon:srid=" + srid + ","
                + // <- the geometry attribute
                "locmin_id:int,"
                + "area:float"
        );

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
                //System.out.println("Regular Polygon "+i);
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

        FeatureEntry fe = new FeatureEntry();
        fe.setLastChange(new Date());
        fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:4326")));
        fe.setDataType(Entry.DataType.Feature);
        fe.setGeometryColumn(SCHEMETYPE.getGeometryDescriptor().getLocalName());
        fe.setLastChange(new Date());
        fe.setGeometryType(Geometries.getForName(SCHEMETYPE.getGeometryDescriptor().getType().getName().getLocalPart()));

        geopackage.add(fe, collection);
        geopackage.close();

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
    public static ArrayList<Geometry> read(File file, boolean switchCoordinates) throws MalformedURLException, IOException, FactoryException {

        if (!file.exists() || file.length() < 12) {
            return null;
        }
        GeoPackage gp = new GeoPackage(file);
        gp.init();

        FeatureEntry fe = null;
        try {
            fe = gp.features().get(0); //only look into the first layer

        } catch (Exception e) {
//            try {
//                //fallback scheme with utm 25832 srid
//                
//                final SimpleFeatureType SCHEMETYPE = DataUtilities.createType("MultiPolygon",
//                        "the_geom:MultiPolygon:srid=25832"
//                );
//                fe = new FeatureEntry();
//                fe.setLastChange(new Date());
//                
//                fe.setBounds(ReferencedEnvelope.create(CRS.decode("EPSG:25832")));
//                fe.setDataType(Entry.DataType.Feature);
//                fe.setGeometryColumn(SCHEMETYPE.getGeometryDescriptor().getLocalName());
//                fe.setLastChange(new Date());
//                fe.setGeometryType(Geometries.getForName(SCHEMETYPE.getGeometryDescriptor().getType().getName().getLocalPart()));
//            } catch (SchemaException ex) {
//                Logger.getLogger(Geopackage_IO.class.getName()).log(Level.SEVERE, null, ex);
//                
//            }
            return null;
        }
        SimpleFeatureReader fc = gp.reader(fe, null, null);

//        String filename = file.getName();
        ArrayList<Geometry> geometries = new ArrayList<>();
//        String name = "";
        try {
            int count = 0;
            while (fc.hasNext()) {
                count++;

                SimpleFeatureImpl f = (SimpleFeatureImpl) fc.next();

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
            fc.close();
        } catch (Exception ex) {
            //ex.printStackTrace();
        } finally {

            gp.close();
        }
        return geometries;

    }

}
