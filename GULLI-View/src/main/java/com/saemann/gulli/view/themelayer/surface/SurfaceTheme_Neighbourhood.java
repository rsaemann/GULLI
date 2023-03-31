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
import com.saemann.gulli.view.themelayer.SurfaceThemeLayer;
import com.saemann.rgis.view.DoubleColorHolder;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.LinePainting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.opengis.referencing.operation.TransformException;

/**
 * A Surface theme layer to show the neighbour references of the surface
 * topology. This Layer requires the definition of the start cell and the number
 * of surrounding cells.
 *
 * @author Robert
 */
public class SurfaceTheme_Neighbourhood extends SurfaceThemeLayer {

    public final static String layerSurfaceNeighbourhood = PaintManager.layerTriangle + "_Neighbourhood";
    private final DoubleColorHolder chNeighbour1 = new DoubleColorHolder(Color.yellow, new Color(1f, 1f, 1f, 0f), "Surface Neighbourhood");
    public static int laststartCell = 0;
    public static int lastNeighbours=10000;
    
    public int startCell=0;
    public int neighbours=10000;

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {
        mapViewer.clearLayer(layerSurfaceNeighbourhood);
        Surface surface = c.getSurface();
        Layer layer_ = mapViewer.getLayer(layerSurfaceNeighbourhood);
        if (layer_ == null) {
            layer_ = new Layer(layerSurfaceNeighbourhood, chNeighbour1);
            mapViewer.getLayers().add(layer_);
        }
        final Layer layer = layer_;
        new Thread() {
            @Override
            public void run() {
                progress = 0;
                if (c.getSurface() != null) {
                    if (c.getSurface().neumannNeighbours != null) {
                        ArrayList<Integer> list = c.getSurface().getSurroundingCells(startCell, neighbours);
                        int[][] neumann = c.getSurface().neumannNeighbours;
                        double[][] mids = c.getSurface().getTriangleMids();
                        int index = 0;
                        Coordinate start = new CoordinateXY(), end = new CoordinateXY();
                        Coordinate startWGS = new CoordinateXY(), endWGS = new CoordinateXY();
                        for (Integer i : list) {
                            try {
                                start.x = mids[i][0];
                                start.y = mids[i][1];
                                c.getSurface().getGeotools().toGlobal(start, startWGS, true);
                                for (int j = 0; j < 3; j++) {
                                    int neighbourID = neumann[i][j];
                                    if (neighbourID < 0) {
                                        continue;
                                    }

                                    end.x = mids[neighbourID][0];
                                    end.y = mids[neighbourID][1];
                                    c.getSurface().getGeotools().toGlobal(end, endWGS, true);

                                    //Create line from the center towards the neighbouring cell. ended at the border=at the edge.
                                    LinePainting lp = new LinePainting(index++, startWGS.x, (startWGS.x + endWGS.x) * 0.5, startWGS.y, (startWGS.y + endWGS.y) * 0.5, chNeighbour1);//, (startWGS.x+endWGS.x)*0.5, (startWGS.y+endWGS.y)*0.5
                                    lp.arrowheadvisibleFromZoom = 14;
                                    layer.add(lp, false);

                                }
                            } catch (TransformException ex) {
                                Logger.getLogger(SurfaceTheme_Neighbourhood.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                }
                progress = 1;
                mapViewer.recalculateShapes();
                mapViewer.recomputeLegend();
                mapViewer.repaint();
            }
        }.start();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapviewer) {
        mapviewer.clearLayer(layerSurfaceNeighbourhood);
    }

    @Override
    public void setDisplayTime(long displayTimeMS) {
        //Is independent from the time
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "Neighbours";
    }

}
