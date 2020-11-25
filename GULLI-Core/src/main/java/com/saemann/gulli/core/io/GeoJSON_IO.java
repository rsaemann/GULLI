package com.saemann.gulli.core.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;

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
public class GeoJSON_IO {

    private DecimalFormat df;

    public boolean swapXY = false;

    public String crs = null;

    public static char quotes = '\"';

    public StringBuffer actual_indention = new StringBuffer("");

    public static String indention = "    ";

    /**
     * If true String representation of Point lists will check and skip
     * identical coordinate strings.
     */
    public boolean checkDoubleStrings = true;

    public GeoJSON_IO() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        dfs.setDecimalSeparator('.');
        dfs.setGroupingSeparator(',');
        df = new DecimalFormat("0.#####", dfs);
    }

    /**
     * Sets number of shown digits after decimal point.
     *
     * @param decimals
     */
    public void setMaximumFractionDigits(int decimals) {
        df.setMaximumFractionDigits(decimals);
    }

    /**
     * Creates the Stringline that will represents the Geometry
     *
     * @param geom
     * @param crs Coordinate reference like 'EPSG:4326' for WGS84 [lon,lat]
     * @return representtaionstring including crs
     */
    public String stringFromGeometry(Geometry geom, String crs) {
        if (geom.getClass().equals(Polygon.class)) {
            return stringForPolygon((Polygon) geom);
        }
        if (geom.getClass().equals(LineString.class)) {
            return stringForLineString(geom, crs);
        }
        if (geom.getClass().equals(MultiPolygon.class)) {
            return stringForMultiPolygon(geom, crs);
        }
        System.out.println("No decoder for Geometry of type " + geom.getGeometryType() + "\t class:" + geom.getClass() + "\t -> will be treated as Multipoint");
        return stringForMultiPoint(geom, crs);
    }

    private String stringForMultiPoint(Geometry geom, String crs) {

        boolean first = true;
        StringBuilder str = new StringBuilder(""
                + actual_indention + "{\n"
                + actual_indention + "    " + quotes + "type" + quotes + ": " + quotes + "Feature" + quotes + ",\n");
        Object data = geom.getUserData();
        if (data != null) {
            if (data instanceof JSONProperty[]) {
                String propString = prepareProperties((JSONProperty[]) data);
                str.append(actual_indention + propString + ",\n");
            } else {
                System.out.println("Userdata is " + data.getClass());
            }
        }

        str.append(actual_indention + "    " + quotes + "geometry" + quotes + ": {\n"
                + actual_indention + "        " + quotes + "type" + quotes + ": " + quotes + "MultiPoint" + quotes + ",\n"
                + actual_indention + "        " + quotes + "coordinates" + quotes + ": [");
        for (Coordinate c : geom.getCoordinates()) {
            if (!first) {
                str.append(',');
            }
            first = false;
            if (swapXY) {
                str.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
            } else {
                str.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
            }
        }

        str.append("]\n" + actual_indention + "    }\n" + actual_indention + "}");

        return str.toString();
    }

    private String stringForMultiPolygon(Geometry geom, String crs) {

        boolean first = true;
        StringBuilder str = new StringBuilder(""
                + actual_indention + "{\n"
                + actual_indention + "    " + quotes + "type" + quotes + ": " + quotes + "Feature" + quotes + ",\n");
        Object data = geom.getUserData();
        if (data != null) {
            if (data instanceof JSONProperty[]) {
                String propString = prepareProperties((JSONProperty[]) data);
                str.append(actual_indention + propString + ",\n");
            } else {
                System.out.println("Userdata is " + data.getClass());
            }
        }

        str.append(actual_indention + "    " + quotes + "geometry" + quotes + ": {\n"
                + actual_indention + "        " + quotes + "type" + quotes + ": " + quotes + "MultiPolygon" + quotes + ",\n"
                + actual_indention + "        " + quotes + "coordinates" + quotes + ": [\n");
        actual_indention.append(indention).append(indention);
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            if (i == 0) {
                str.append(actual_indention).append("[\n");
            } else {
                str.append(",\n").append(actual_indention).append("[\n");
            }
            Polygon polygon = (Polygon) geom.getGeometryN(i);
            //Exterior ring:

            str.append(actual_indention + "[");
            actual_indention.append(indention);
            LineString exring = polygon.getExteriorRing();
            first = true;
            for (Coordinate c : exring.getCoordinates()) {
                if (!first) {
                    str.append(',');
                }
                first = false;
                if (swapXY) {
                    str.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
                } else {
                    str.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
                }
            }
            str.append(actual_indention).append("]");
            if (polygon.getNumInteriorRing() > 0) {
                for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
                    LineString iring = polygon.getInteriorRingN(j);
                    first = true;
                    str.append(",\n" + actual_indention + "[");
                    for (Coordinate c : iring.getCoordinates()) {
                        if (!first) {
                            str.append(',');
                        }
                        first = false;
                        if (swapXY) {
                            str.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
                        } else {
                            str.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
                        }
                    }
                    str.append("]");
                }
            }
            actual_indention.delete(0, indention.length());
            str.append("\n"+actual_indention+"]");
            
        }
        actual_indention.delete(0, indention.length()*2);
        str.append("]\n" + actual_indention + "    }\n" + actual_indention + "}");

        return str.toString();
    }

    private String stringForLineString(Geometry geom, String crs) {

        boolean first = true;
        StringBuilder str = new StringBuilder(""
                + actual_indention + "{\n"
                + actual_indention + "    \"type\": \"Feature\",\n"
                + actual_indention + "    \"geometry\": {\n"
                + actual_indention + "        \"type\": \"LineString\",\n"
                + actual_indention + "        \"coordinates\": [");
        for (Coordinate c : geom.getCoordinates()) {
            if (!first) {
                str.append(',');
            }
            first = false;
            if (swapXY) {
                str.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
            } else {
                str.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
            }
        }

        str.append("]\n" + actual_indention + "    }\n" + actual_indention + "}");

        return str.toString();
    }

//    private String stringForPolygon(Polygon geom, String crs) {
//        return stringForPolygon(geom, crs, 0);
//    }
    private String stringForPolygon(Polygon geom) {

        boolean firstNode = true;
//        boolean firstring = true;
        String laststring = "";
        StringBuilder actString = new StringBuilder("   ");

        StringBuilder str = new StringBuilder(""
                + actual_indention + "{\n"
                + actual_indention + indention + quotes + "type" + quotes + ": " + quotes + "Feature" + quotes + ",\n");
        actual_indention.append(indention);
        Object data = geom.getUserData();
        if (data != null) {
            if (data instanceof JSONProperty[]) {
                String propString = prepareProperties((JSONProperty[]) data);
                str.append(actual_indention + propString + ",\n");
            } else {
                System.out.println("Userdata is " + data.getClass());
            }
        }

        str.append(actual_indention).append(quotes).append("geometry").append(quotes).append(": {\n");

        actual_indention.append(indention);
        str.append(actual_indention).append(quotes).append("type").append(quotes).append(": ").append(quotes).append("Polygon").append(quotes).append(",\n")
                .append(actual_indention).append(quotes).append("coordinates").append(quotes).append(": [\n");
        actual_indention.append(indention);
        str.append(actual_indention).append("[");

//        StringBuilder str = new StringBuilder(""
//                + actual_indention + "{\n"
//                + actual_indention + "    \"type\": \"Feature\",\n"
//                + actual_indention + "    \"geometry\": {\n"
//                + actual_indention + "        \"type\": \"Polygon\",\n"
//                + actual_indention + "        \"coordinates\": [\n"
//                + actual_indention + "            [");
        //First is the exterior Ring
        LineString ext = geom.getExteriorRing();
        int count = 0;
        for (Coordinate c : ext.getCoordinates()) {
            count++;

            actString.delete(0, actString.length());
            if (swapXY) {
                actString.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
            } else {
                actString.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
            }
            if (checkDoubleStrings) {
                if (actString.toString().equals(laststring)) {
                    continue;
                }
            }
            if (!firstNode) {
                str.append(',');
            }
            firstNode = false;
            laststring = actString.toString();
            str.append(actString);
        }
        if (!ext.getCoordinates()[0].equals2D(ext.getCoordinates()[ext.getCoordinates().length - 1])) {
            //First and last point have to be the same
            if (swapXY) {
                str.append(",[").append(df.format(ext.getCoordinates()[0].y)).append(",").append(df.format(ext.getCoordinates()[0].x)).append("]");
            } else {
                str.append(",[").append(df.format(ext.getCoordinates()[0].x)).append(",").append(df.format(ext.getCoordinates()[0].y)).append("]");
            }
        }
        str.append(" ]");
        if (geom.getNumInteriorRing() > 0) {
            //Innere holes after exterior ring
            for (int i = 0; i < geom.getNumInteriorRing(); i++) {
                LineString inner = geom.getInteriorRingN(i);
                if (inner.getNumPoints() < 6) {
                    continue;
                }
                firstNode = true;
                str.append(",\n"
                        + actual_indention + "[");
                count = 0;
                for (Coordinate c : inner.getCoordinates()) {
                    count++;

                    if (!firstNode) {
                        str.append(',');
                    }
                    firstNode = false;
                    if (swapXY) {
                        str.append("[").append(df.format(c.y)).append(",").append(df.format(c.x)).append("]");
                    } else {
                        str.append("[").append(df.format(c.x)).append(",").append(df.format(c.y)).append("]");
                    }
                }
                if (!inner.getCoordinates()[0].equals2D(inner.getCoordinates()[inner.getCoordinates().length - 1])) {
                    //First and last point have to be the same
                    if (swapXY) {
                        str.append(",[").append(df.format(inner.getCoordinates()[0].y)).append(",").append(df.format(inner.getCoordinates()[0].x)).append("]");
                    } else {
                        str.append(",[").append(df.format(inner.getCoordinates()[0].x)).append(",").append(df.format(inner.getCoordinates()[0].y)).append("]");
                    }
                }
                str.append(" ]");
            }
        }
        actual_indention.delete(0, indention.length());
        str.append("\n" + actual_indention + "]\n");// \coordinates
        actual_indention.delete(0, indention.length());
        str.append(actual_indention + "}\n"); // \geometry
        actual_indention.delete(0, indention.length());
        str.append(actual_indention + "}");//feature

        return str.toString();
    }

    public String prepareProperties(JSONProperty[] props) {
        StringBuffer str = new StringBuffer(
                quotes + "properties" + quotes + ":{\n");
        for (int i = 0; i < props.length; i++) {
            JSONProperty prop = props[i];
            str.append(actual_indention).append(indention).append(prop.toString());
            if (i < props.length - 1) {
                str.append(",");
            }
            str.append("\n");
        }
        str.append(actual_indention).append("}");

        return str.toString();
    }

    public void write(File file, GeometryCollection collection) throws IOException {
        Geometry[] array = new Geometry[collection.getNumGeometries()];
        for (int i = 0; i < collection.getNumGeometries(); i++) {
            array[i] = collection.getGeometryN(i);
        }
        write(file, array);
    }

    public void write(File file, Collection<Geometry> collection) throws IOException {
        write(file, collection.toArray(new Geometry[collection.size()]));
    }

    public void write(File file, Geometry[] collection) throws IOException {
        if (collection == null) {
            return;
        }
        try (FileWriter fw = new FileWriter(file); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("{\n"
                    + "    \"type\": \"FeatureCollection\",\n"
            );
            if (crs != null && !crs.isEmpty()) {
                bw.write("    \"crs\": {\n"
                        + "        \"type\": \"name\",\n"
                        + "        \"properties\": {\n"
                        + "            \"name\": \"" + crs + "\"\n"
                        + "        }\n"
                        + "    },\n");
            }
            bw.write("    \"features\": [");
            actual_indention.append(indention);
            boolean firstGeometry = true;
            for (int i = 0; i < collection.length; i++) {
                Geometry geom = collection[i];
                if (geom == null) {
                    continue;
                }
                String str = stringFromGeometry(geom, crs);
                if (!firstGeometry) {
                    bw.append(',');
                    bw.newLine();
                }
                firstGeometry = false;
                bw.write(str);
                bw.flush();
            }
            bw.newLine();
            bw.write("    ]\n}");
            bw.flush();
        }
    }

    public static class JSONProperty {

        public String key;
        public String value;

        public JSONProperty(String key_, Object value_, boolean needQuotes) {
            this.key = quotes + key_ + quotes;
            if (needQuotes) {
                this.value = quotes + value_.toString() + quotes;
            } else {
                this.value = value_.toString();
            }
        }

        public JSONProperty(String key_, Object value_) {
            this.key = quotes + key_ + quotes;
//            System.out.println(value + " of Class " + value_.getClass());
            if (value_ instanceof Number) {
                this.value = value_.toString();
            } else {
                this.value = quotes + value_.toString() + quotes;
            }
        }

        @Override
        public String toString() {
            return key + " : " + value;
        }

    }

}
