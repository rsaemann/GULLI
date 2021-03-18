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
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * To save the area generated from multiple travel paths that reached an outlet.
 * This combines the linestring travel paths at an outlet to a shapelike polygn
 * by adding a buffer to each path.
 *
 * @author saemann
 */
public class Save_TravelAccumulationRegions implements OutputIntention {

    public StoringCoordinator.FileFormat fileformat;

    /**
     * -1=all materials
     */
    public int materialIndex = -1;
    
    /**
     * Buffer to each side in ° 0.0004 = 25m buffer to each side
     */
    public double buffer=0.0004;

    /**
     * Only traces of particles that reached an outlet.
     */
    public boolean outfall_only = true;

    /**
     * if true, seperate files will be created for each outfall
     */
    public boolean seperateFilnemaeByOutlet = true;

    private File outputFile = null;

    /**
     *
     * @param fileformat
     * @param materialIndex index of material, or -1 for all
     */
    public Save_TravelAccumulationRegions(StoringCoordinator.FileFormat fileformat, int materialIndex) {
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
            String name = "OutletSourceRegion" + (materialIndex < 0 ? "_all" : "_" + materialName);

            HashMap<String, ArrayList<LineString>> savemap = new HashMap<>(map.size());

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
                
                Geometry area=shapes.get(0).buffer(buffer, 1);
                for (LineString shape : shapes) {
                    area=area.union(shape.buffer(buffer, 1, 0));
                }
                area=TopologyPreservingSimplifier.simplify(area, buffer*0.1);
//                area=area.buffer(buffer,1,0);
//                area=area.buffer(-buffer,1,0);
                
                name = entry.getKey();
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
                    SHP_IO_GULLI.writeWGS84(area, shpfile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
                    if (verbose) {
                        System.out.println("Shapefiles written to " + shpfile.getAbsolutePath());
                    }
                } else if (fileformat == StoringCoordinator.FileFormat.CSV) {
                    File csvFile = new File(fileRoot, name + ".csv");
                    try {
                        HE_SurfaceIO.writeSurfaceContaminationCSV(csvFile, surface);
                        if (verbose) {
                            System.out.println("Shapefiles written to " + csvFile.getAbsolutePath());
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (fileformat == StoringCoordinator.FileFormat.GeoJSON) {
                    File jsonfile = new File(fileRoot, name + ".json");
                    GeoJSON_IO geojson = new GeoJSON_IO();
                    geojson.setMaximumFractionDigits(5);
                    //GeoJSON convention is: longitude first;
                    geojson.swapXY = !StartParameters.JTS_WGS84_LONGITUDE_FIRST;
                    Geometry mercator = area;
//                    for (int i = 0; i < mercator.length; i++) {
//                        Geometry geom = shapes.get(i);
//                        mercator[i] = geom;

                        GeoJSON_IO.JSONProperty[] props = new GeoJSON_IO.JSONProperty[3];
                        props[0] = new GeoJSON_IO.JSONProperty("stoff", materialName);
                        props[1] = new GeoJSON_IO.JSONProperty("eventid", 0);
                        props[2] = new GeoJSON_IO.JSONProperty("part", 0);
                        mercator.setUserData(props);
//                    }
                    try {
                        geojson.write(jsonfile, new Geometry[]{mercator});
                        if (verbose) {
                            System.out.println("Shapefiles written to " + jsonfile.getAbsolutePath());
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (fileformat == StoringCoordinator.FileFormat.GeoPKG) {
                    File gpckFile = new File(fileRoot, name + ".gpkg");
                    if (gpckFile.exists()) {
                        gpckFile.delete();
                    }
                    ArrayList<Geometry> geoms=new ArrayList<>(1);
                    geoms.add(area);
                    Geopackage_IO.writeWGS84(geoms, gpckFile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
                    if (verbose) {
                        System.out.println("Shapefiles written to " + gpckFile.getAbsolutePath());
                    }
                } else {
                    System.err.println("Fileformat " + fileformat + " is not yet implemented for " + getClass() + " outputs.");
                    File csvFile = new File(fileRoot, name + ".csv");
                    try {
                        HE_SurfaceIO.writeSurfaceContaminationCSV(csvFile, surface);
                    } catch (IOException ex) {
                        Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);

                    }
                }
            }
            System.out.println("Created all Travel path area files in "+fileRoot.getAbsolutePath());
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
        return "./OutletSourceRegion_#MaterialName#." + fileformat.name().toLowerCase();
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
        return new double[]{buffer};
    }

    @Override
    public void setParameterValueDouble(int index, double value) {
        if(index==0){
            buffer=value;
        }
    }

    @Override
    public String[] getParameterNamesDouble() {
        return new String[]{"Buffer[°]"};
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
        return "ImpactRegion Shape (." + fileformat + ") of " + (materialIndex < 0 ? " all Materials" : "Material " + materialIndex);
    }

}
