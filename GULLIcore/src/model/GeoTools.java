package model;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Tools for transforming between global (WGs84 long,lat) coordinates to utm
 * (metric) system. The class test itself if WGS84 is Long/lat or Lat/Long
 * defined when transforming Coordinates that only have x,y,z.
 *
 * @author Robert Sämann
 */
public class GeoTools {

//    protected GeometryFactory factory        = new GeometryFactory();
    protected MathTransform transform_wgs2utm, transform_utm2wgs;
    protected String epsg_utm = "";
    protected boolean globalLongitudeFirst = false;

    public GeometryFactory gf;

    public static boolean returnBiggestValidatedPolygon = true;

    public GeoTools() {
        this(false);
    }

    public GeoTools(boolean forcelongitudeFirst) {

        gf = new GeometryFactory();
        try {
            CRSAuthorityFactory af = CRS.getAuthorityFactory(forcelongitudeFirst);
            CoordinateReferenceSystem sourceCRS = null;

            sourceCRS = af.createCoordinateReferenceSystem("EPSG:4326"); // WGS 84 Lat/Lon (X:lat,
            // Y:lon)

            CoordinateReferenceSystem targetCRS = null;

            targetCRS = af.createCoordinateReferenceSystem("EPSG:3395"); //World Mercator
            epsg_utm = "EPSG:3395";
            transform_wgs2utm = CRS.findMathTransform(sourceCRS, targetCRS);

            transform_utm2wgs = CRS.findMathTransform(targetCRS, sourceCRS);
            this.globalLongitudeFirst = testIsLongitudeFirst(sourceCRS);
        } catch (Exception ex) {
            Logger.getLogger(GeoTools.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public GeoTools(MathTransform transform_wgs2utm, MathTransform transform_utm2wgs, boolean isLongitudeFirst) {
        this.transform_wgs2utm = transform_wgs2utm;
        this.transform_utm2wgs = transform_utm2wgs;
        this.gf = new GeometryFactory();
        this.globalLongitudeFirst = isLongitudeFirst;
    }

    public GeoTools(CoordinateReferenceSystem crs_global, CoordinateReferenceSystem crs_utm) throws FactoryException {
        transform_wgs2utm = CRS.findMathTransform(crs_global, crs_utm);
        transform_utm2wgs = CRS.findMathTransform(crs_utm, crs_global);
        this.gf = new GeometryFactory();
        this.globalLongitudeFirst = testIsLongitudeFirst(crs_global);
    }

    public static GeoTools newGeoTools(Coordinate wgs84, boolean longitudeFirst) {
        if (longitudeFirst) {
            return new GeoTools(wgs84.x, wgs84.y);
        } else {
            return new GeoTools(wgs84.y, wgs84.x);
        }
    }

    public GeoTools(GeoPosition2D positionWGS84) {
        this(positionWGS84.getLongitude(), positionWGS84.getLatitude());
    }

    /**
     * Finds the best TOUTM-Transformation at the given WGS84 position.
     *
     * @param longitude
     * @param latitude
     */
    public GeoTools(double longitude, double latitude) {
        gf = new GeometryFactory();
        try {
            CoordinateReferenceSystem targetCRS = null;
            CoordinateReferenceSystem sourceCRS = null;
            sourceCRS = CRS.decode("EPSG:4326"); // WGS 84 Lat/Lon (X:lat,

            //targetCRS = CRS.decode("EPSG:3395");
            //Fallback to Pseudomercator.
            epsg_utm = "EPSG:3857";
            targetCRS = CRS.decode(epsg_utm);
            if (false) {
                if (longitude < 0) {
                    targetCRS = CRS.decode("EPSG:25830");
                    epsg_utm = "EPSG:25830";
//                System.out.println("25830");
                } else if (longitude > 0 && longitude < 5.5) {
                    targetCRS = CRS.decode("EPSG:25831");
                    epsg_utm = "EPSG:25831";
//                System.out.println("25831");
                } else if (longitude > 5.5 && longitude < 12) {
                    targetCRS = CRS.decode("EPSG:25832");
                    epsg_utm = "EPSG:25832";
//               System.out.println("25832");
//                                targetCRS = CRS.decode("EPSG:3395");
//                System.out.println("3395");
                } else if (longitude > 12 && longitude < 21) {
                    targetCRS = CRS.decode("EPSG:25833");
                    epsg_utm = "EPSG:25833";
//                System.out.println("25833");
                } else {
                    //Fallback to Pseudomercator.
                    epsg_utm = "EPSG:3857";
                    targetCRS = CRS.decode(epsg_utm);

//                System.out.println("3395");
                }
            }
//            System.out.println("Use '"+targetCRS.getName()+"' as UTM Reference.");
            transform_wgs2utm = CRS.findMathTransform(sourceCRS, targetCRS);
            transform_utm2wgs = CRS.findMathTransform(targetCRS, sourceCRS);
            this.globalLongitudeFirst = testIsLongitudeFirst(sourceCRS);
        } catch (FactoryException ex) {
            Logger.getLogger(GeoTools.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public GeoTools(String sourceCRScode_global, String targetCRScode_metric) throws FactoryException {
        this(sourceCRScode_global, targetCRScode_metric, false);
    }

    public GeoTools(String sourceCRScode_global, String targetCRScode_metric, boolean forceLongitudeFirst) throws FactoryException {
        this.gf = new GeometryFactory();
        CRSAuthorityFactory af = CRS.getAuthorityFactory(forceLongitudeFirst);
        CoordinateReferenceSystem sourceCRS = null;

        sourceCRS = af.createCoordinateReferenceSystem(sourceCRScode_global); // WGS 84 Lat/Lon 

        CoordinateReferenceSystem targetCRS = null;

        targetCRS = af.createCoordinateReferenceSystem(targetCRScode_metric);

        transform_wgs2utm = CRS.findMathTransform(sourceCRS, targetCRS);
        transform_utm2wgs = CRS.findMathTransform(targetCRS, sourceCRS);
        this.globalLongitudeFirst = testIsLongitudeFirst(sourceCRS);

    }

    public Geometry toUTM(Geometry geomWGS84) throws TransformException {
        Geometry transformedPolygon = JTS.transform(geomWGS84, transform_wgs2utm);
        return transformedPolygon;
    }

    public Geometry toUTM(Geometry geomWGS84, boolean longitudeFirst) throws TransformException {
        if (longitudeFirst == globalLongitudeFirst) {
            Geometry transformedPolygon = JTS.transform(geomWGS84, transform_wgs2utm);
            return transformedPolygon;
        } else {
            //switch input coordinates
            if (geomWGS84.getCoordinates() != null) {
                for (Coordinate coordinate : geomWGS84.getCoordinates()) {
                    double x = coordinate.x;
                    coordinate.x = coordinate.y;
                    coordinate.y = x;
                }
            }
            geomWGS84.geometryChanged();
            return JTS.transform(geomWGS84, transform_wgs2utm);
        }
    }

    public static Coordinate transform(MathTransform transform, Coordinate c) throws TransformException {
        return JTS.transform(c, null, transform);
    }

    public static Point transform(MathTransform transform, Point c) throws TransformException {
        return (Point) JTS.transform(c, transform);
    }

    public static Geometry transform(MathTransform transform, Geometry c) throws TransformException {
        return JTS.transform(c, transform);
    }

    public Coordinate toUTM(Coordinate geomWGS84) throws TransformException {
        return JTS.transform(geomWGS84, null, transform_wgs2utm);
    }

    /**
     * Creates metric coordinates from angles.
     *
     * @param geomWGS84
     * @param longitudeFirst define if you put in x:longitude, y:latitude or not
     * @return
     * @throws TransformException
     */
    public Coordinate toUTM(Coordinate geomWGS84, boolean longitudeFirst) throws TransformException {
        if (longitudeFirst == globalLongitudeFirst) {
            //Nothing to change
            return JTS.transform(geomWGS84, null, transform_wgs2utm);
        } else {
            //switch coordinates
            Coordinate retur = new Coordinate(geomWGS84.y, geomWGS84.x, geomWGS84.z);
            JTS.transform(retur, retur, transform_wgs2utm);
            return retur;
        }
    }

    public Coordinate toUTM(double longitude, double latitude) throws TransformException {
        if (globalLongitudeFirst) {
            //Nothing to change
            return JTS.transform(new Coordinate(longitude, latitude), null, transform_wgs2utm);
        } else {
            //switch coordinates
            Coordinate retur = new Coordinate(latitude, longitude, 0);
            Coordinate c = JTS.transform(retur, null, transform_wgs2utm);
            return c;
        }
    }

    public Coordinate toUTM(Point2D p, boolean longitudeFirst) throws TransformException {
        if (longitudeFirst == globalLongitudeFirst) {
            return JTS.transform(new Coordinate(p.getX(), p.getY()), null, transform_wgs2utm);
        } else {
            return JTS.transform(new Coordinate(p.getY(), p.getX()), null, transform_wgs2utm);
        }
    }

    /**
     * UTM x&y component based on Long/Lat
     *
     * @param geoposition
     * @return
     * @throws org.opengis.referencing.operation.TransformException
     */
    public Coordinate toUTM(GeoPosition2D geoposition) throws TransformException {
        Coordinate c;
        if (globalLongitudeFirst) {
            //Nothing to change
            c = new Coordinate(geoposition.getLongitude(), geoposition.getLatitude());
        } else {
            c = new Coordinate(geoposition.getLatitude(), geoposition.getLongitude());
        }
        return JTS.transform(c, null, transform_wgs2utm);
    }

    public Geometry toGlobal(Geometry geomUTM) throws TransformException {
        Geometry transformedPolygon = JTS.transform(geomUTM, transform_utm2wgs);
        return transformedPolygon;
    }

    /**
     *
     * @param geomUTM geometry to transform.
     * @param longitudefirst if true: coordinates of x:longitude, y=latitude
     * @return
     * @throws TransformException
     */
    public Geometry toGlobal(Geometry geomUTM, boolean longitudefirst) throws TransformException {
        if (longitudefirst == globalLongitudeFirst) {
            Geometry transformedPolygon = JTS.transform(geomUTM, transform_utm2wgs);
            return transformedPolygon;
        } else {
            //Switch output coordinates
            Geometry transformedPolygon = JTS.transform(geomUTM, transform_utm2wgs);
            if (transformedPolygon.getCoordinates() != null) {
                for (Coordinate coordinate : transformedPolygon.getCoordinates()) {
                    double x = coordinate.x;
                    coordinate.x = coordinate.y;
                    coordinate.y = x;
                }
            }
            transformedPolygon.geometryChanged();
            return transformedPolygon;
        }
    }

    public Coordinate toGlobal(Coordinate geomUTM) throws TransformException {
        return JTS.transform(geomUTM, null, transform_utm2wgs);

    }

    public Coordinate toGlobal(Coordinate geomUTM, boolean longitudeFirst) throws TransformException {
        Coordinate wgs84 = JTS.transform(geomUTM, null, transform_utm2wgs);
        if (longitudeFirst == globalLongitudeFirst) {
            //Nothing to change
            return wgs84;
        } else {
            double x = wgs84.x;
            wgs84.x = wgs84.y;
            wgs84.y = x;
            return wgs84;
        }
    }

    public void toGlobal(Coordinate geomUTM, Coordinate global, boolean longitudeFirst) throws TransformException {
        JTS.transform(geomUTM, global, transform_utm2wgs);
        if (longitudeFirst == globalLongitudeFirst) {
            //Nothing to change
        } else {
            double x = global.x;
            global.x = global.y;
            global.y = x;
        }
    }

    /**
     * Coordinates Output from transformation and Input to Transformation shall
     * be x:longitude, y:latitude
     *
     * @return
     */
    public boolean isGloablLongitudeFirst() {
        return globalLongitudeFirst;
    }

    public boolean testIsLongitudeFirst(CoordinateReferenceSystem globalCRS) {
        try {
            CoordinateReferenceSystem local = CRS.decode("EPSG:25832");//UTM_ETRS89/UTM Zone 32N
            MathTransform transform = CRS.findMathTransform(local, globalCRS);
            Coordinate hannover = new Coordinate(551000., 5806000.);
            Coordinate wgs = JTS.transform(hannover, null, transform);
            if ((int) (wgs.x * 10) == 97) {
//                System.out.println("longitude is first");
                return true;
            }
//            System.out.println("WGS:" + wgs);
        } catch (FactoryException ex) {
            Logger.getLogger(GeoTools.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformException ex) {
            Logger.getLogger(GeoTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public Polygon getCircle(double latitude, double longitude, double radiusMeter) throws TransformException {
//        gf = new GeometryFactory();
        Point ll = gf.createPoint(new Coordinate(latitude, longitude));
        Point utm = (Point) this.toUTM(ll);
        Coordinate[] c = new Coordinate[9];
        double cr = Math.sin(0.25 * Math.PI) * radiusMeter; //45°
        for (int i = 0; i < c.length; i++) {
            c[i] = new Coordinate((Coordinate) utm.getCoordinate().clone());
        }
        c[0].y += radiusMeter;
        c[1].x += cr;
        c[1].y += cr;
        c[2].x += radiusMeter;
        c[3].x += cr;
        c[3].y -= cr;
        c[4].y -= radiusMeter;
        c[5].x -= cr;
        c[5].y -= cr;
        c[6].x -= radiusMeter;
        c[7].x -= cr;
        c[7].y += cr;
        c[8] = c[0];

        LinearRing lr = gf.createLinearRing(c);

        LinearRing lrll = (LinearRing) this.toGlobal(lr);
        Polygon p = gf.createPolygon(lrll);
        return p;
    }

    public Polygon getQuadratUTM(double latitude, double longitude, double kantenlaengeMeter) throws TransformException {
        Coordinate utm = transform(transform_wgs2utm, new Coordinate(latitude, longitude));
        double cr = kantenlaengeMeter * 0.5; //45°
        Coordinate[] c = new Coordinate[5];
        c[0] = new Coordinate(utm.x - cr, utm.y - cr);
        c[1] = new Coordinate(utm.x - cr, utm.y + cr);
        c[2] = new Coordinate(utm.x + cr, utm.y + cr);
        c[3] = new Coordinate(utm.x + cr, utm.y - cr);
        c[4] = c[0];
        return gf.createPolygon(c);
    }

    public Polygon getQuadratWGS(double latitude, double longitude, double kantenlaengeMeter) throws TransformException {
        Coordinate utm = transform(transform_wgs2utm, new Coordinate(latitude, longitude));
        double cr = kantenlaengeMeter * 0.5; //45°
        Coordinate[] c = new Coordinate[5];
        c[0] = new Coordinate(utm.x - cr, utm.y - cr);
        c[1] = new Coordinate(utm.x - cr, utm.y + cr);
        c[2] = new Coordinate(utm.x + cr, utm.y + cr);
        c[3] = new Coordinate(utm.x + cr, utm.y - cr);
        c[4] = c[0];
        return (Polygon) transform(transform_utm2wgs, gf.createPolygon(c));
    }

    public String getEpsg_utm() {
        return epsg_utm;
    }

    public static GeoPosition2D calculateMidpoint(List<GeoPosition2D> positions) {
        double area = 0;
        GeoPosition2D last = positions.get(0);

        for (int i = 1; i < positions.size(); i++) {
            GeoPosition2D actual = positions.get(i);
            area += actual.getLatitude() * last.getLongitude() - actual.getLongitude() * last.getLatitude();
            last = actual;
        }
        area = area / 2.;
        // ****
        double lat = 0;
        double lon = 0;
        last = positions.get(0);
        for (int i = 1; i < positions.size(); i++) {
            GeoPosition2D actual = positions.get(i);
            double f = (last.getLatitude() * actual.getLongitude() - actual.getLatitude() * last.getLongitude());
            lat += (last.getLatitude() + actual.getLatitude()) * f;
            lon += (last.getLongitude() + actual.getLongitude()) * f;
            last = actual;
        }
        lat *= 1. / (6. * area);
        lon *= 1. / (6. * area);
        return new GeoPosition(lat, lon);
    }

    /**
     * Get / create a valid version of the geometry given. If the geometry is a
     * polygon or multi polygon, self intersections / inconsistencies are fixed.
     * Otherwise the geometry is returned.
     *
     * @param geom
     * @return a geometry
     */
    public static Geometry validate(Geometry geom) {
        if (geom instanceof Polygon) {
            if (geom.isValid()) {
                geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
                return geom; // If the polygon is valid just return it
            }
            Polygonizer polygonizer = new Polygonizer();
            addPolygon((Polygon) geom, polygonizer);
            Geometry p = toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
//            System.out.println("validate " + geom.getGeometryType() + " to " + p.getGeometryType());
            if (p instanceof Polygon) {
                Polygon pv = (Polygon) p;
//                System.out.println("validated has " + pv.getNumInteriorRing() + " interiors. before: " + ((Polygon) geom).getNumInteriorRing());
            }
//            System.out.println("return " + p);
            return p;
        } else if (geom instanceof MultiPolygon) {
            if (geom.isValid()) {
                geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
                return geom; // If the multipolygon is valid just return it
            }
            Polygonizer polygonizer = new Polygonizer();
            for (int n = geom.getNumGeometries(); n-- > 0;) {
                addPolygon((Polygon) geom.getGeometryN(n), polygonizer);
            }
            return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
        } else {
            return geom; // In my case, I only care about polygon / multipolygon geometries
        }
    }

    /**
     * Add all line strings from the polygon given to the polygonizer given
     *
     * @param polygon polygon from which to extract line strings
     * @param polygonizer polygonizer
     */
    static void addPolygon(Polygon polygon, Polygonizer polygonizer) {
        addLineString(polygon.getExteriorRing(), polygonizer);
        for (int n = polygon.getNumInteriorRing(); n-- > 0;) {
            addLineString(polygon.getInteriorRingN(n), polygonizer);
        }
    }

    /**
     * Add the linestring given to the polygonizer
     *
     * @param linestring line string
     * @param polygonizer polygonizer
     */
    static void addLineString(LineString lineString, Polygonizer polygonizer) {

        if (lineString instanceof LinearRing) { // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing
            lineString = lineString.getFactory().createLineString(lineString.getCoordinateSequence());
        }

        // unioning the linestring with the point makes any self intersections explicit.
        Point point = lineString.getFactory().createPoint(lineString.getCoordinateN(0));
        Geometry toAdd = lineString.union(point);

        //Add result to polygonizer
        polygonizer.add(toAdd);
    }

    /**
     * Get a geometry from a collection of polygons.
     *
     * @param polygons collection
     * @param factory factory to generate MultiPolygon if required
     * @return null if there were no polygons, the polygon if there was only
     * one, or a MultiPolygon containing all polygons otherwise
     */
    static Geometry toPolygonGeometry(Collection<Polygon> polygons, GeometryFactory factory) {
//        System.out.println("to polygon Geometry with " + polygons.size() + " polygons");
        switch (polygons.size()) {
            case 0:
                return null; // No valid polygons!
            case 1:
                return polygons.iterator().next(); // single polygon - no need to wrap
            default:
                //polygons may still overlap! Need to sym difference them
//                if(true){
//                    Iterator<Polygon> iter = polygons.iterator();
//                    iter.next();
//                    iter.next();
//                    iter.next();
//                    return iter.next();
//                }
//                ArrayList<Polygon> al=new ArrayList<>(polygons);
//                for (int i = 0; i < al.size()   ; i++) {
//                    for (int j = i+1; j< al.size(); j++) {
//                       if(al.get(i).equals(al.get(j))){
//                           System.out.println("doppelt vorhanden");
//                       }
//                    }
//                }

                if (returnBiggestValidatedPolygon) {
                    //search for largest shape seems to do the job
                    Polygon biggest = null;
                    double area = 0;
                    for (Polygon polygon : polygons) {
                        if (polygon.getArea() > area) {
                            biggest = polygon;
                            area = biggest.getArea();
                        }
                    }
                    return biggest;
                }
                Iterator<Polygon> iter = polygons.iterator();
                Geometry ret = iter.next();
                Geometry temp;
                double area = ret.getArea();
                double tempArea = 0;
                int cutoutcounter = 0;
                while (iter.hasNext()) {
                    Polygon cutout = iter.next();
                    if (iter.hasNext()) {
                        temp = ret.symDifference(cutout);
//                        tempArea = temp.getArea();
//                        if (tempArea < area) {
                        ret = temp;
                        //Only use diff if area is cut out.
//                        }
//                        System.out.print("loop: " + cutoutcounter + "  " + ret.getGeometryType() + " geoms:" + ret.getNumGeometries() + "   area:" + ret.getArea());
//                        if (ret instanceof Polygon) {
//                            System.out.println(",  " + ((Polygon) ret).getNumInteriorRing() + " interiors");
////                        return ret;
//                        } else if (ret instanceof MultiPolygon) {
//                            Polygon p = (Polygon) ((MultiPolygon) ret).getGeometryN(0);
//                            System.out.println(", inners: " + p.getNumInteriorRing());
//                        }
                        cutoutcounter++;
                    }
                }
//                System.out.println("return polygon with " + cutoutcounter + " cutouts");
                return ret;
        }
    }

}
