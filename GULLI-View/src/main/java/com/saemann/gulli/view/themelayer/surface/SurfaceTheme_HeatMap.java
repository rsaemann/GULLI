/*
 * The MIT License
 *
 * Copyright 2021 Sämann.
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
package com.saemann.gulli.view.themelayer.surface;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.view.PaintManager;
import com.saemann.gulli.view.themelayer.SurfaceThemeLayer;
import com.saemann.rgis.model.GeoPosition;
import com.saemann.rgis.model.GeoPosition2D;
import com.saemann.rgis.view.GradientColorHolder;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.shapes.AreaPainting;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.NodePainting;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import org.locationtech.jts.geom.Coordinate;

/**
 * Coloring the surface cells by the intensity of the contamination
 *
 * @author Robert Sämann
 */
public class SurfaceTheme_HeatMap extends SurfaceThemeLayer {

    public final String layerSurfaceContaminated = PaintManager.layerTriangle+"HEAT";
    public final GradientColorHolder chSurfaceHeatMap = new GradientColorHolder(0, 1, Color.yellow, Color.red, 255, "HeatMap");

    private boolean logarithmic = false;
    private double bagatelle = 0;
    private String name;

    public SurfaceTheme_HeatMap(boolean logarithmic) {
        this.logarithmic = logarithmic;
        if (logarithmic) {
            name = "log. Heatmap";
        } else {
            name = "linear Heatmap";
        }
    }

//    private final DoubleColorHolder chCellMeasurements = new DoubleColorHolder(Color.orange.darker(), new Color(1f, 1f, 1f, 0f), "Surface Measurementraster");
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller control, PaintManager pm, boolean asNodes) {

        Surface surface = control.getSurface();
        if (surface == null) {
            return false;
        }
        SurfaceMeasurementRaster raster = surface.getMeasurementRaster();
        if (raster == null) {
            return false;
        }

        mapViewer.clearLayer(layerSurfaceContaminated);
        Layer layer_ = mapViewer.getLayer(layerSurfaceContaminated);
        if (layer_ == null) {
            layer_ = new Layer(layerSurfaceContaminated, chSurfaceHeatMap);
            mapViewer.getLayers().add(layer_);
        }
        final Layer layer = layer_;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progress = 0;

                /////////////////////////////////////
                try {
                    double[] tempUTMcoordstorage = new double[2];
                    double[] tempWGScoordstorage = new double[2];
                    Coordinate tempWGScoordinate = new Coordinate();
                    if (raster instanceof SurfaceMeasurementTriangleRaster) {
                        SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();
                        synchronized (raster) {
                            double totalmass = 0;
                            for (InjectionInfo injection : control.getScenario().getInjections()) {
                                totalmass += injection.getMass();//getNumberOfParticles();//getMass();//
                            }
                            /**
                             * count of particles per triangle exceeding this
                             * number will get high color shapes. used to scale
                             * the color between low color to max color.
                             */
                            double highColorCount = 1 * totalmass;
                            if (logarithmic) {
                                highColorCount = Math.log10(totalmass) + 5;
                            }

                            double totalmeasurements = 0;
                            for (int i = 0; i < raster.measurementTimestamp.length; i++) {
                                totalmeasurements += raster.measurementTimestamp[i];
                            }
                            if (totalmeasurements < 1) {
                                System.err.println("No measurements.");
                                return;
                            }

                            double bagatell = (0.00001 * totalmass);
                            int nbMaterials = raster.getNumberOfMaterials();
                            int nbtimes = raster.measurementTimestamp.length;
                            double duration = 0;
                            for (int i = 0; i < raster.durationInTimeinterval.length; i++) {
                                duration += raster.durationInTimeinterval[i];
                            }
                            double normalisation = 1.0 / (double) duration;
//                        System.out.println("Total duration of "+duration+" = "+(duration/60)+" min = "+(duration/3600)+"h");

                            for (int i = 0; i < raster.getMeasurements().length; i++) {
                                TriangleMeasurement triangleMeasurement = raster.getMeasurements()[i];
                                if (triangleMeasurement == null) {
                                    continue;
                                }
                                double cellMass = 0;
                                double timesum = 0;
                                for (int t = 0; t < nbtimes; t++) {
                                    timesum = 0;
                                    for (int m = 0; m < nbMaterials; m++) {
                                        timesum += triangleMeasurement.getMassResidence()[m][t];
                                    }
//                                    if (surfaceShow == PaintManager.SURFACESHOW.HEATMAP_LIN_BAGATELL) {
                                    if (timesum <= bagatelle) {
                                        continue;
                                    }
//                                    }
                                    cellMass += timesum;
                                }
                                if (cellMass == 0) {
                                    continue;
                                }
                                final double frac;
                                if (logarithmic) {
                                    frac = (Math.log10(cellMass * normalisation) + 5) / highColorCount;
                                } else {
                                    //Linear
                                    frac = cellMass * normalisation;
                                }

                                if (asNodes) {
//                                    Coordinate c = surface.getGeotools().toGlobal(new Coordinate(surface.getTriangleMids()[i][0], surface.getTriangleMids()[i][1]));
                                    tempUTMcoordstorage[0] = surface.getTriangleMids()[i][0];
                                    tempUTMcoordstorage[1] = surface.getTriangleMids()[i][1];
                                    surface.getGeotools().toGlobal(tempUTMcoordstorage, tempWGScoordstorage, true);

                                    NodePainting np = new NodePainting(i, tempWGScoordstorage[0], tempWGScoordstorage[1], chSurfaceHeatMap) {
                                        @Override
                                        public boolean paint(Graphics2D g2) {

                                            g2.setColor(chSurfaceHeatMap.getGradientColor(frac));

                                            return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                                        }
                                    };
//                                    NodePainting np = new NodePainting(i, tri.getPosition3D(0).lonLatCoordinate(), new ColorHolder(color));
                                    np.setRadius(2);
                                    layer.add(np, false);
//                                mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, np);

                                } else {
                                    //Convert Coordinates
                                    try {
                                        int[] nodes = surface.getTriangleNodes()[i];
//                                        Coordinate[] coords = new Coordinate[4];
                                        ArrayList<GeoPosition2D> liste = new ArrayList<>(4);
                                        for (int j = 0; j < 3; j++) {
                                            surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]), tempWGScoordinate, tempWGScoordstorage, false);
                                            liste.add(new GeoPosition(tempWGScoordinate.x, tempWGScoordinate.y));
                                        }
//                                        coords[3] = coords[0];//Close ring
                                        liste.add(liste.get(0));
                                        AreaPainting ap = new AreaPainting(i, chSurfaceHeatMap, liste) {

                                            @Override
                                            public boolean paint(Graphics2D g2) {
                                                try {
                                                    g2.setColor(chSurfaceHeatMap.getGradientColor(frac));
                                                    g2.fill(this.getOutlineShape());
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                                return true;
                                            }

                                        };
                                        layer.add(ap, false);
//                                    mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                    } catch (Exception exception) {
                                        System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + i);
                                        System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                        throw exception;
                                    }
                                }
                            }
                        }
//                    }
                    } else if (surface.getMeasurementRaster() != null && surface.getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
                        SurfaceMeasurementRectangleRaster raster = (SurfaceMeasurementRectangleRaster) surface.getMeasurementRaster();
                        synchronized (raster) {
                            double totalmass = 0;
                            for (InjectionInfo injection : control.getScenario().getInjections()) {
                                totalmass += injection.getMass();
                            }
                            /**
                             * count of particles per triangle exceeding this
                             * number will get high color shapes. used to scale
                             * the color between low color to max color.
                             */
                            double highColorCount = 1 * totalmass;
                            if (logarithmic) {
                                highColorCount = Math.log10(totalmass) + 5;
                            }

                            double totalmeasurements = 0;
                            for (int i = 0; i < raster.measurementTimestamp.length; i++) {
                                totalmeasurements += raster.measurementTimestamp[i];
                            }
                            if (totalmeasurements < 1) {
                                System.err.println("No measurements.");
                                return;
                            }

                            double bagatell = (0.00001 * totalmass);
                            double duration = 0;
                            for (int i = 0; i < raster.durationInTimeinterval.length; i++) {
                                duration += raster.durationInTimeinterval[i];
                            }
                            double normalisation = 1.0 / (double) duration;

//                        double timeScale = ThreadController.getDeltaTimeMS() / (surface.getTimes().getDeltaTimeMS() / 1000.);
                            for (int x = 0; x < raster.getNumberXIntervals(); x++) {
                                if (raster.getParticlecounter()[x] == null) {
                                    continue;
                                }
                                for (int y = 0; y < raster.getNumberYIntervals(); y++) {
                                    if (raster.getParticlecounter()[x][y] == null) {
                                        continue;
                                    }
                                    double massum = 0;
                                    for (int t = 0; t < raster.getNumberOfTimes(); t++) {
                                        for (int m = 0; m < raster.getNumberOfMaterials(); m++) {
                                            try {
                                                massum += raster.getMass()[x][y][t][m];

                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                    if (massum <= bagatell) {
                                        continue;
                                    }
                                    final double frac;
                                    if (massum == 0) {
                                        continue;
                                    } else {

                                        if (logarithmic) {
                                            frac = (Math.log10(massum * normalisation) + 5) / highColorCount;
                                        } else {
                                            //Linear
                                            frac = massum * normalisation;
                                        }
                                    }
                                    int id = x + y * raster.getNumberXIntervals();
                                    if (asNodes) {
                                        Coordinate c = surface.getGeotools().toGlobal(raster.getMidCoordinate(x, y));
                                        NodePainting np = new NodePainting(id, c, chSurfaceHeatMap) {
                                            @Override
                                            public boolean paint(Graphics2D g2) {

                                                g2.setColor(chSurfaceHeatMap.getGradientColor(frac));

                                                return super.paint(g2);
                                            }
                                        };
                                        np.setRadius(2);
                                        layer.add(np, false);

                                    } else {
                                        //Convert Coordinates
                                        try {
                                            Coordinate[] coordsUTM = raster.getRectangleBound(x, y);
//                                            Coordinate[] coordsWGS = new Coordinate[5];
//                                            for (int j = 0; j < 4; j++) {
//                                                coordsWGS[j] = surface.getGeotools().toGlobal(coordsUTM[j]);
//                                            }
//                                            coordsWGS[4] = coordsWGS[0];//Close ring

                                            ArrayList<GeoPosition2D> liste = new ArrayList<>(5);
                                            for (int j = 0; j < 4; j++) {
                                                surface.getGeotools().toGlobal(coordsUTM[j], tempWGScoordinate, tempWGScoordstorage, false);
                                                liste.add(new GeoPosition(tempWGScoordinate.x, tempWGScoordinate.y));
                                            }

                                            AreaPainting ap = new AreaPainting(id, chSurfaceHeatMap, liste){//surface.getGeotools().gf.createLinearRing(coordsWGS)) {
                                                @Override
                                                public boolean paint(Graphics2D g2) {
                                                    g2.setColor(chSurfaceHeatMap.getGradientColor(frac));
                                                    g2.draw(outlineShape);
                                                    g2.fill(outlineShape);
                                                    return true;
                                                }
                                            };
                                            layer.add(ap, false);
//                                            mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                        } catch (Exception exception) {
                                            System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + id);
                                            System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                            throw exception;
                                        }
                                    }

                                }
                            }
                        }
                    }

                    mapViewer.recalculateShapes();
                    mapViewer.recomputeLegend();
                    mapViewer.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(r, "Init " + getDisplayName()).start();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapviewer) {
        mapviewer.clearLayer(layerSurfaceContaminated);
    }

    @Override
    public void setDisplayTime(long displayTimeMS) {
        //Not required as the paint method inside the shapes already calculates the new color.
    }

}
