/*
 * The MIT License
 *
 * Copyright 2021 Robert Sämann.
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
package com.saemann.gulli.core.control.output;

import com.saemann.gulli.core.control.ShapeTools;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.StoringCoordinator;
import static com.saemann.gulli.core.control.StoringCoordinator.verbose;
import com.saemann.gulli.core.io.GeoJSON_IO;
import com.saemann.gulli.core.io.Geopackage_IO;
import com.saemann.gulli.core.io.SHP_IO_GULLI;
import com.saemann.gulli.core.io.extran.HE_SurfaceIO;
import com.saemann.gulli.core.model.surface.Surface;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

/**
 * To save the travel paths as line shapes in a geometry file.
 *
 * @author saemann
 */
public class Save_Travelpath implements OutputIntention {

    public StoringCoordinator.FileFormat fileformat;

    /**
     * -1=all materials
     */
    public int materialIndex = -1;

    /**
     * Only traces of particles that reached an outlet.
     */
    public boolean outfall_only = true;

    /**
     * if true, seperate files will be created for each outfall
     */
    public boolean seperateFilnemaeByOutlet = false;

    private File outputFile = null;

    /**
     *
     * @param fileformat
     * @param materialIndex index of material, or -1 for all
     */
    public Save_Travelpath(StoringCoordinator.FileFormat fileformat, int materialIndex) {
        this.fileformat = fileformat;
        this.materialIndex = materialIndex;
    }

    @Override
    public File writeOutput(StoringCoordinator sc) {
        try {
            File fileRoot = sc.getFileRoot();
            String materialName;
            if (materialIndex < 0) {
                materialName = "all";
            } else {
                materialName = sc.getMaterial(materialIndex).getName();
            }

            Surface surface = sc.getSurface();
            if (surface == null) {
                System.err.println("No surface to write contamination shapes.");
                return null;
            }

            //Create surrounding shape of contaminated areas.
            HashMap<String, ArrayList<LineString>> map = ShapeTools.createTravelPathsToOutlet(sc.getSurface(), sc.getNetwork(), sc.getParticles());
            String name = "Travelpaths" + (materialIndex < 0 ? "_all" : "_" + materialName);

            HashMap<String, ArrayList<LineString>> savemap = new HashMap<>(map.size());

//            ArrayList<Geometry> shapes = null;
            int saves = 1;
            if (seperateFilnemaeByOutlet) {
                //One file per outlet
                for (Map.Entry<String, ArrayList<LineString>> entry : map.entrySet()) {
                    if (outfall_only && entry.getKey().equals("inside")) {
                        continue;
                    }
                    String filename = name + "_" + entry.getKey();
                    savemap.put(filename, entry.getValue());
                }
            } else {
                //All shapes into one file.
                saves = 1;
                ArrayList<LineString> shapes = new ArrayList<>();
                for (Map.Entry<String, ArrayList<LineString>> entry : map.entrySet()) {
                    if (outfall_only && entry.getKey().equals("inside")) {
                        continue;
                    }
                    shapes.addAll(entry.getValue());
                }
                savemap.put(name, shapes);
            }

            for (Map.Entry<String, ArrayList<LineString>> entry : savemap.entrySet()) {

                ArrayList<LineString> shapes = entry.getValue();
                if (shapes == null || shapes.isEmpty()) {
                    System.out.println("No Travelpath traces files created.");
                    continue;
                }
                name=entry.getKey();
                if (fileformat == StoringCoordinator.FileFormat.SHP) {
                    File shpfile = new File(fileRoot, name + ".shp");
                    File shxfile = new File(fileRoot, name + ".shx");
                    //Delete old files
                    if (shpfile.exists()) {
                        shpfile.delete();
                    }
                    if (shxfile.exists()) {
                        shxfile.delete();
                    }
                    SHP_IO_GULLI.writeWGS84Linestring(shapes, shpfile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
                    if (verbose) {
                        System.out.println("Shapefiles written to " + shpfile.getAbsolutePath());
                    }
                    return shpfile;
                } else if (fileformat == StoringCoordinator.FileFormat.CSV) {
                    File csvFile = new File(fileRoot, name + ".csv");
                    try {
                        HE_SurfaceIO.writeSurfaceContaminationCSV(csvFile, surface);
                        if (verbose) {
                            System.out.println("Shapefiles written to " + csvFile.getAbsolutePath());
                        }
                        return csvFile;
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (fileformat == StoringCoordinator.FileFormat.GeoJSON) {
                    File jsonfile = new File(fileRoot, name + ".json");
                    GeoJSON_IO geojson = new GeoJSON_IO();
                    geojson.setMaximumFractionDigits(5);
                    //GeoJSON convention is: longitude first;
                    geojson.swapXY = !StartParameters.JTS_WGS84_LONGITUDE_FIRST;
                    Geometry[] mercator = new Geometry[shapes.size()];
                    for (int i = 0; i < mercator.length; i++) {
                        Geometry geom = shapes.get(i);
                        mercator[i] = geom;

                        GeoJSON_IO.JSONProperty[] props = new GeoJSON_IO.JSONProperty[3];
                        props[0] = new GeoJSON_IO.JSONProperty("stoff", materialName);
                        props[1] = new GeoJSON_IO.JSONProperty("eventid", 0);
                        props[2] = new GeoJSON_IO.JSONProperty("part", i);
                        mercator[i].setUserData(props);
                    }
                    try {
                        geojson.write(jsonfile, mercator);
                        if (verbose) {
                            System.out.println("Shapefiles written to " + jsonfile.getAbsolutePath());
                        }
                        return jsonfile;
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (fileformat == StoringCoordinator.FileFormat.GeoPKG) {
                    File gpckFile = new File(fileRoot, name + ".gpkg");
                    if (gpckFile.exists()) {
                        gpckFile.delete();
                    }
                    Geopackage_IO.writeWGS84LineString(shapes, gpckFile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
                    if (verbose) {
                        System.out.println("Shapefiles written to " + gpckFile.getAbsolutePath());
                    }
                } else {
                    System.err.println("Fileformat " + fileformat + " is not yet implemented for " + getClass() + " outputs.");
                    File csvFile = new File(fileRoot, name + ".csv");
                    try {
                        HE_SurfaceIO.writeSurfaceContaminationCSV(csvFile, surface);
                        return csvFile;
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);

                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public String getFileSuffix() {
        return fileformat.name();
    }

    @Override
    public String getFilePath() {
        return "./Travelpaths_#MaterialName#." + fileformat.name().toLowerCase();
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public void setOutputFile(File output) {
        this.outputFile = output;
    }

    @Override
    public double[] getParameterValuesDouble() {
        return null;
    }

    @Override
    public void setParameterValueDouble(int index, double value) {
    }

    @Override
    public String[] getParameterNamesDouble() {
        return null;
    }

    @Override
    public int[] getParameterValuesInt() {
        return new int[]{materialIndex};
    }

    @Override
    public String[] getParameterNamesInt() {
        return new String[]{"Material index (-1=all)"};
    }

    @Override
    public void setParameterValueInt(int index, int value) {
        if (index == 0) {
            this.materialIndex = value;
        }
    }

    @Override
    public StoringCoordinator.FileFormat getFileFormat() {
        return fileformat;
    }

    @Override
    public void setFileFormat(StoringCoordinator.FileFormat ff) {
        this.fileformat = ff;
    }

    @Override
    public StoringCoordinator.FileFormat[] getSupportedFileFormat() {
        return new StoringCoordinator.FileFormat[]{StoringCoordinator.FileFormat.CSV, StoringCoordinator.FileFormat.GeoJSON, StoringCoordinator.FileFormat.GeoPKG, StoringCoordinator.FileFormat.SHP};
    }

    @Override
    public String toString() {
        return "Travelpaths Shape (." + fileformat + ") of " + (materialIndex < 0 ? " all Materials" : "Material " + materialIndex);
    }

}
