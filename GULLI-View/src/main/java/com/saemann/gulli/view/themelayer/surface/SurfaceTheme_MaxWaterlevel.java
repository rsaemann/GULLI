/*
 * The MIT License
 *
 * Copyright 2018 saemann.
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
import java.awt.Color;
import java.awt.Graphics2D;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.view.PaintManager;
import com.saemann.rgis.view.shapes.AreaPainting;
import com.saemann.rgis.view.shapes.LabelPainting;
import com.saemann.rgis.view.shapes.NodePainting;
import com.saemann.gulli.view.themelayer.SurfaceThemeLayer;
import com.saemann.rgis.view.GradientColorHolder;
import com.saemann.rgis.view.MapViewer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 *
 * @author saemann
 */
public class SurfaceTheme_MaxWaterlevel extends SurfaceThemeLayer {

    private static String name = "Max Waterlevel";

//    public final DoubleColorHolder chTrianglesWaterlevel = new DoubleColorHolder(Color.white, Color.blue, "Max Waterlevel");
    public final GradientColorHolder chTrianglesWaterlevel = new GradientColorHolder(0, 0.5, Color.white, Color.blue, 255, "Max Waterlevel");

    public final String layerSurfaceWaterlevel = PaintManager.layerTriangle+"_WLMax";

    public final String layerLabelWaterlevel = "TXT_WLMax";

    public final float maxWaterlevel = 0.5f;

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {
        final Surface surface = c.getSurface();
        if (surface == null) {
            throw new NullPointerException("Surface is null.");
        }
        if (surface.getTimes() == null) {
//            this.surfaceShow = PaintManager.SURFACESHOW.NONE;
            throw new NullPointerException("Surface is not initialized (no TimeContainer).");
//            return;
        }
        try {
            GeometryFactory gf = new GeometryFactory();
            double[] lvls = surface.getMaxWaterLevels();
//            System.out.println("add Surface shapes max waterlevels: " + lvls.length);
            for (int i = 0; i < lvls.length; i++) {

                final double lvl = lvls[i];
                if (lvl < 0.02) {
                    continue;
                }
                if (asNodes) {
                    double[] pos = surface.getTriangleMids()[i];
                    NodePainting np = new NodePainting(i, surface.getGeotools().toGlobal(new Coordinate(pos[0], pos[1])), chTrianglesWaterlevel);
                    np.setGradientColorIndex((int) (lvl / maxWaterlevel));
                    np.setRadius(2);
                    mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, np);
                } else {
                    //Convert Coordinates
                    int[] nodes = null;
                    try {
                        nodes = surface.getTriangleNodes()[i];
                        Coordinate[] coords = new Coordinate[4];
                        for (int j = 0; j < 3; j++) {
                            coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                        }
                        coords[3] = coords[0];//Close ring
                        AreaPainting ap = new AreaPainting(i, chTrianglesWaterlevel, gf.createLinearRing(coords)) {
                            @Override
                            public boolean paint(Graphics2D g2) {
                                if (outlineShape == null) {
                                    return false;
                                }
                                g2.setColor(PaintManager.interpolateColor(chTrianglesWaterlevel.getColor(), chTrianglesWaterlevel.getFillColor(), lvl / maxWaterlevel));
                                g2.fill(outlineShape);
                                return true;
//                                    return super.paint(g2);
                            }
                        };
                        mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, ap);
                    } catch (Exception exception) {
                        System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + i);

                        System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                        System.err.println("Vertices= {" + nodes[0] + " , " + nodes[1] + "," + nodes[2]);
                        System.err.println("number of vertices: " + surface.getVerticesPosition().length);
                        throw exception;
                    }
                }

                double[] p = surface.getTriangleMids()[i];
                Coordinate longlat = surface.getGeotools().toGlobal(new Coordinate(p[0], p[1]));
                LabelPainting lp = new LabelPainting(i, MapViewer.COLORHOLDER_LABEL, new com.saemann.rgis.model.GeoPosition(longlat), 20, -5, -5, PaintManager.df3.format(lvl)) {

                    @Override
                    public boolean paint(Graphics2D g2) {
                        if (mapViewer.getZoom() < this.getMinZoom()) {
                            return false;
                        }
                        return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                    }

                };
                lp.setCoronaBackground(true);
                mapViewer.addPaintInfoToLayer(layerLabelWaterlevel, lp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapViewer.recalculateShapes();
        mapViewer.recomputeLegend();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapviewer) {
        mapviewer.clearLayer(layerSurfaceWaterlevel);
        mapviewer.clearLayer(layerLabelWaterlevel);
        mapviewer.recalculateShapes();
        mapviewer.recomputeLegend();
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void setDisplayTime(long displayTimeMS) {
        
    }

    @Override
    public String getDisplayName() {
        return "Max Waterlevel";
    }

}
