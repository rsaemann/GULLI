/*
 * The MIT License
 *
 * Copyright 2021 B1.
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
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.view.PaintManager;
import static com.saemann.gulli.view.PaintManager.maximumNumberOfSurfaceShapes;
import com.saemann.gulli.view.themelayer.SurfaceThemeLayer;
import com.saemann.rgis.view.DoubleColorHolder;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.shapes.AreaPainting;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.NodePainting;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 * A Surface theme layer to show the cells of the surface topology.
 *
 * @author Robert
 */
public class SurfaceTheme_Cells extends SurfaceThemeLayer {

    public final static String layerSurfaceGrid = PaintManager.layerTriangle+"_SURFGRID";
    private final DoubleColorHolder chTrianglesGrid = new DoubleColorHolder(Color.orange, new Color(1f, 1f, 1f, 0f), "Surface Triangles");

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {
        mapViewer.clearLayer(layerSurfaceGrid);
        Surface surface = c.getSurface();
        Layer layer_ = mapViewer.getLayer(layerSurfaceGrid);
        if (layer_ == null) {
            layer_ = new Layer(layerSurfaceGrid, chTrianglesGrid);
            mapViewer.getLayers().add(layer_);
        }
        final Layer layer=layer_;
        new Thread() {
            public void run() {
                progress=0;
                if (asNodes) {
                    int id = 0;

                    for (double[] triangleMid : c.getSurface().getTriangleMids()) {
                        try {
                            NodePainting np = new NodePainting(id, c.getSurface().getGeotools().toGlobal(new Coordinate(triangleMid[0], triangleMid[1])), chTrianglesGrid);
                            id++;
                            layer.add(np, false);
                            if (id > maximumNumberOfSurfaceShapes) {
                                break;
                            }
                        } catch (TransformException ex) {
                            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    try {
                        int id = 0;
                        Coordinate[] coords = new Coordinate[4];
                        for (int[] nodes : surface.getTriangleNodes()) {

                            for (int j = 0; j < 3; j++) {
                                coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                            }
                            coords[3] = coords[0];//Close ring
                            AreaPainting ap = new AreaPainting(id, chTrianglesGrid, c.getSurface().getGeotools().gf.createLinearRing(coords));
                            layer.add(ap, false);
                            id++;
                            if (id > maximumNumberOfSurfaceShapes) {
                                break;
                            }
                        }

                    } catch (TransformException ex) {
                        Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                progress=1;
                mapViewer.recalculateShapes();
                mapViewer.recomputeLegend();
                mapViewer.repaint();
            }           
        }.start();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapviewer) {
        mapviewer.clearLayer(layerSurfaceGrid);
    }

    @Override
    public void setDisplayTime(long displayTimeMS) {
        //Is independent from the time
    }

    @Override
    public String getDisplayName() {
        return "Grid";
    }

    

              

}
