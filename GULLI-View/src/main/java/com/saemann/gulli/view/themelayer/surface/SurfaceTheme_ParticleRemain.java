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
import com.saemann.gulli.core.control.particlecontrol.injection.ArealInjection;
import com.saemann.gulli.core.control.particlecontrol.injection.SurfaceInjection;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.view.PaintManager;
import com.saemann.gulli.view.themelayer.SurfaceThemeLayer;
import com.saemann.rgis.model.GeoPosition;
import com.saemann.rgis.model.GeoPosition2D;
import com.saemann.rgis.view.DoubleColorHolder;
import com.saemann.rgis.view.GradientColorHolder;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.NodePainting;
import com.saemann.rgis.view.shapes.PaintInfo;
import com.saemann.rgis.view.shapes.TrianglePainting;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 * Coloring the surface cells by the intensity of the contamination
 *
 * @author Robert Sämann
 */
public class SurfaceTheme_ParticleRemain extends SurfaceThemeLayer {

    public final String layerkeyPIPE = PaintManager.layerTriangle+"_REMAIN_PIPE";
    public final String layerkeyLEFT = PaintManager.layerTriangle+"_REMAIN_OUT";
    public final String layerkeySURF = PaintManager.layerTriangle+"_REMAIN_SURFACE";
    public final DoubleColorHolder chLeft = new DoubleColorHolder(Color.GREEN.darker(), Color.GREEN, "Left catchment");
    public final DoubleColorHolder chReachedPipe = new DoubleColorHolder(Color.BLUE.darker(), Color.BLUE, "Reached pipe");
    public final GradientColorHolder chstayOnSurface = new GradientColorHolder(0, 2000, Color.RED, Color.YELLOW, 5, "Stay on surface");

    private boolean logarithmic = false;
//    private double bagatelle = 0;
    private String name;

    public SurfaceTheme_ParticleRemain() {
        this.logarithmic = logarithmic;
        this.name = "Particles remain";
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

        Runnable r = new Runnable() {
            @Override
            public void run() {
                progress = 0;

                double[] tempUTMcoordstorage = new double[2];
                double[] tempWGScoordstorage = new double[2];
                Coordinate tempWGScoordinate = new Coordinate();
                /////////////////////////////////////
                int numberOfTracerParticles = 0;
                mapViewer.clearLayer(layerkeyLEFT);
                mapViewer.clearLayer(layerkeyPIPE);
                mapViewer.clearLayer(layerkeySURF);
                if (surface == null || surface.triangleNodes == null) {
                    return;
                }
                if (control.getThreadController().getParticles() == null) {
                    return;
                }
                int[][] counter = new int[surface.triangleNodes.length][2 + chstayOnSurface.getColorsGradient().length];
                //0: out (outlet)
                //1: still on surface
                //2: still in Pipe

                for (Particle p : control.getThreadController().getParticles()) {
                    if (p == null) {
                        continue;
                    }
                    if (!p.getInjectionInformation().spillOnSurface()) {
                        continue;
                    }
                    long surfaceID = -1;
                    if (p.getInjectionInformation() instanceof SurfaceInjection) {
                        surfaceID = ((SurfaceInjection) p.getInjectionInformation()).getInjectionCellID(p.getId());
                    } else if (p.getInjectionInformation() instanceof ArealInjection) {
                        surfaceID = ((ArealInjection) p.getInjectionInformation()).getInjectionCellID(p.getId());
                    }

                    if (surfaceID < 0) {
                        System.err.println("DO not know how to get Cell ID from Injection definition " + p.getInjectionInformation());
                    }

                    if (p.hasLeftSimulation()) {
                        //Reached outlet
                        counter[(int) surfaceID][0]++;
                    } else if (p.isInPipeNetwork() || p.toPipenetwork != null) {
                        counter[(int) surfaceID][1]++;
                    } else if (p.isOnSurface() && p.toPipenetwork == null) {
                        int index = Math.max(0, Math.min(chstayOnSurface.getColorsGradient().length - 1, (int) (p.getTravelledPathLength() / 1000)));
                        counter[(int) surfaceID][2 + index]++;
                    } else {
                        System.out.println("Do not know how to treat particle with status " + p.status + " for the remaining-coloring");
                    }
                }
                Layer layerOut = mapViewer.getLayer(layerkeyLEFT);
                if (layerOut == null) {
                    layerOut = new Layer(layerkeyLEFT, chLeft);
                    mapViewer.getLayers().add(layerOut);
                }
                Layer layerSurf = mapViewer.getLayer(layerkeySURF);
                if (layerSurf == null) {
                    layerSurf = new Layer(layerkeySURF, chstayOnSurface);
                    mapViewer.getLayers().add(layerSurf);
                }
                Layer layerPipe = mapViewer.getLayer(layerkeyPIPE);
                if (layerPipe == null) {
                    layerPipe = new Layer(layerkeyPIPE, chReachedPipe);
                    mapViewer.getLayers().add(layerPipe);
                }

                for (int id = 0; id < counter.length; id++) {
                    for (int j = 0; j < counter[0].length; j++) {
                        if (counter[id][j] > 0) {
                            //Create shape
                            PaintInfo pi = null;
                            if (asNodes) {
                                try {
                                    tempUTMcoordstorage[0] = surface.getTriangleMids()[id][0];
                                    tempUTMcoordstorage[1] = surface.getTriangleMids()[id][1];
                                    surface.getGeotools().toGlobal(tempUTMcoordstorage, tempWGScoordstorage, true);

                                    pi = new NodePainting(id, tempWGScoordstorage[0], tempWGScoordstorage[1], chLeft) {
                                        @Override
                                        public boolean paint(Graphics2D g2) {
                                            if (this.getColor() == chstayOnSurface) {
                                                g2.setColor(chstayOnSurface.getColorsGradient()[gradientColorIndex]);
                                            }
                                            return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                                        }

                                    };

                                } catch (TransformException ex) {
                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                try {
                                    int[] nodes = surface.getTriangleNodes()[id];
//                                Coordinate[] coords = new Coordinate[4];
//                                for (int n = 0; n < 3; n++) {
//                                    try {
//                                        coords[n] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[n]][0], surface.getVerticesPosition()[nodes[n]][1]));
//
//                                    } catch (TransformException ex) {
//                                        Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
//
//                                }
//                                coords[3] = coords[0];//Close ring
//                                LinearRing ring = surface.getGeotools().gf.createLinearRing(coords);
                                    ArrayList<GeoPosition2D> liste = new ArrayList<>(4);
                                    for (int n = 0; n < 3; n++) {
                                        tempUTMcoordstorage[0] = surface.getVerticesPosition()[nodes[n]][0];
                                        tempUTMcoordstorage[1] = surface.getVerticesPosition()[nodes[n]][1];
                                        surface.getGeotools().toGlobal(tempUTMcoordstorage, tempWGScoordstorage, true);
                                        liste.add(new GeoPosition(tempWGScoordstorage[1], tempWGScoordstorage[0]));
                                    }
                                    liste.add(liste.get(0));

                                    TrianglePainting ap = new TrianglePainting(id, chLeft, liste);/* {
                                        @Override
                                        public boolean paint(Graphics2D g2) {
                                            if (this.getColor() == chstayOnSurface) {
                                                g2.setColor(chstayOnSurface.getColorsGradient()[this.getGradientColorIndex()]);
                                                g2.fill(outlineShape);
                                                return true;
                                            }
                                            return super.paint(g2);
                                        }
                                    };*/
                                    //ap.setGradientColorIndex(id);
                                    pi = ap;
                                } catch (TransformException ex) {
                                    Logger.getLogger(SurfaceTheme_ParticleRemain.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            if (pi != null) {
                                //Sort and put the shape to the right color and layer
                                if (j == 0) {
                                    //RemainOutlet
                                    layerOut.add(pi, false);
                                } else if (j == 1) {
                                    pi.setColor(chReachedPipe);
                                    pi.setGradientColorIndex(j);
                                    layerPipe.add(pi, false);
                                } else if (j > 1) {
                                    pi.setColor(chstayOnSurface);
                                    pi.setGradientColorIndex(j - 2);
                                    layerSurf.add(pi, false);
                                }
                            }
                            break;//only use the lowest index and do not paint it like the other categories
                        }
                    }
                }
                mapViewer.recalculateShapes();
                mapViewer.repaint();
            }
        };
        new Thread(r, "Init " + getDisplayName()).start();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapViewer) {
        mapViewer.clearLayer(layerkeyLEFT);
        mapViewer.clearLayer(layerkeyPIPE);
        mapViewer.clearLayer(layerkeySURF);
    }

    @Override
    public void setDisplayTime(long displayTimeMS) {
        //Not required as the paint method inside the shapes already calculates the new color.
    }

}
