/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Geometry;

/**
 *
 * @author saemann
 */
public class ContaminationShape implements OutputIntention {

    public StoringCoordinator.FileFormat fileformat;

    /**
     * -1=all materials
     */
    public int materialIndex;

    /**
     * should the shape consist of all timesteps? if false, the current (last
     * saved) timestep is used.
     */
    public boolean cumulativeShape = true;

    /**
     * factor for multiplication. output is only written, if the number of
     * particles is >= minimumParticleCount. standard=1;
     */
    public int minimumParticleCount = 1;

    private File outputFile = null;

    /**
     *
     * @param fileformat
     * @param materialIndex index of material, or -1 for all
     * @param cumulative true: alltimesteps, false: only last sampled timestep
     */
    public ContaminationShape(StoringCoordinator.FileFormat fileformat, int materialIndex, boolean cumulative) {
        this.fileformat = fileformat;
        this.materialIndex = materialIndex;
        this.cumulativeShape = cumulative;
    }

    @Override
    public File writeOutput(StoringCoordinator sc) {
        File fileRoot = sc.getFileRoot();
        String materialName;
        if (materialIndex < 0) {
            materialName = "all";
        } else {
            materialName = sc.getMaterial(materialIndex).getName();
        }
        String name = "Shape" + (materialIndex < 0 ? "_all" : "_" + materialName) + (minimumParticleCount > 1 ? "_bagatelle" + minimumParticleCount : "");
        ArrayList<Geometry> shapes = null;
        Surface surface = sc.getSurface();
        if (surface == null) {
            System.err.println("No surface to write contamination shapes.");
            return null;
        }
        try {
            //Create surrounding shape of contaminated areas.
            shapes = ShapeTools.createShapesWGS84(surface, 1, 1, materialIndex, minimumParticleCount);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (shapes == null || shapes.isEmpty()) {
            System.out.println("No Shape files created.");
            return null;
        }
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
            SHP_IO_GULLI.writeWGS84(shapes, shpfile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
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
            Geopackage_IO.writeWGS84(shapes, gpckFile.getAbsolutePath(), name, !surface.getGeotools().isGloablLongitudeFirst());
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
        return null;
    }

    @Override
    public String getFileSuffix() {
        return fileformat.name();
    }

    @Override
    public String getFilePath() {
        return "./Shape_#MaterialName#." + fileformat.name().toLowerCase();
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
        return new int[]{materialIndex,minimumParticleCount};
    }

    @Override
    public String[] getParameterNamesInt() {
        return new String[]{"Material index (-1=all)","min. Number of particles"};
    }

    @Override
    public void setParameterValueInt(int index, int value) {
        if(index==0){
            this.materialIndex=value;
        }else if(index==1){
            this.minimumParticleCount=value;
        }
    }
    
      @Override
    public StoringCoordinator.FileFormat getFileFormat() {
        return fileformat;
    }

    @Override
    public void setFileFormat(StoringCoordinator.FileFormat ff) {
        this.fileformat=ff;
    }

    @Override
    public String toString() {
        return "Contamination Shape (."+fileformat+") of "+(materialIndex<0?" all Materials":"Material "+materialIndex);
    }

}
