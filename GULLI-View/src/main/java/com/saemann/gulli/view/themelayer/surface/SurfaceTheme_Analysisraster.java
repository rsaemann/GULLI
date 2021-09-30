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
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.view.PaintManager;
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
import org.locationtech.jts.geom.LinearRing;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Robert Sämann
 */
public class SurfaceTheme_Analysisraster extends SurfaceThemeLayer {

    public final String layerSurfaceMeasurementRaster = "SURFMRaster";

    private final DoubleColorHolder chCellMeasurements = new DoubleColorHolder(Color.orange.darker(), new Color(1f, 1f, 1f, 0f), "Surface Measurementraster");

    @Override
    public String getDisplayName() {
        return "Analysis Raster";
    }

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {

        Surface surface = c.getSurface();
        if (surface == null) {
            return false;
        }
        SurfaceMeasurementRaster raster = surface.getMeasurementRaster();
        if (raster == null) {
            return false;
        }

        mapViewer.clearLayer(layerSurfaceMeasurementRaster);
        Layer layer_ = mapViewer.getLayer(layerSurfaceMeasurementRaster);
        if (layer_ == null) {
            layer_ = new Layer(layerSurfaceMeasurementRaster, chCellMeasurements);
            mapViewer.getLayers().add(layer_);
        }
        final Layer layer = layer_;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progress = 0;
                if (raster instanceof SurfaceMeasurementRectangleRaster) {
                    SurfaceMeasurementRectangleRaster rectRaster = (SurfaceMeasurementRectangleRaster) raster;
                    if (asNodes) {
                        for (int x = 0; x < rectRaster.getNumberXIntervals(); x++) {
                            for (int y = 0; y < rectRaster.getNumberYIntervals(); y++) {
                                try {
                                    Coordinate longlat = surface.getGeotools().toGlobal(rectRaster.getMidCoordinate(x, y), true);
                                    NodePainting np = new NodePainting(x * rectRaster.getNumberYIntervals() + y, longlat, chCellMeasurements);
//                                    mapViewer.addPaintInfoToLayer(layerSurfaceMeasurementRaster, np);
                                    layer.add(np, false);
                                } catch (TransformException ex) {
                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    } else {
                        for (int x = 0; x < rectRaster.getNumberXIntervals(); x++) {
                            for (int y = 0; y < rectRaster.getNumberYIntervals(); y++) {

                                try {
                                    Coordinate lowleft = new Coordinate(rectRaster.getXmin() + x * rectRaster.getxIntervalWidth(), rectRaster.getYmin() + y * rectRaster.getYIntervalHeight());
                                    Coordinate topRight = new Coordinate(lowleft.x + rectRaster.getxIntervalWidth(), lowleft.y + rectRaster.getYIntervalHeight());
                                    Coordinate topleft = new Coordinate(lowleft.x, topRight.y);
                                    Coordinate lowRight = new Coordinate(topRight.x, lowleft.y);

                                    Coordinate llll = surface.getGeotools().toGlobal(lowleft, true);
                                    Coordinate lltr = surface.getGeotools().toGlobal(topRight, true);
                                    Coordinate lltl = surface.getGeotools().toGlobal(topleft, true);
                                    Coordinate lllr = surface.getGeotools().toGlobal(lowRight, true);

                                    LinearRing ring = surface.getGeotools().gf.createLinearRing(new Coordinate[]{llll, lltl, lltr, lllr, llll});
                                    AreaPainting ap = new AreaPainting(x * rectRaster.getNumberYIntervals() + y, chCellMeasurements, ring);
                                    layer.add(ap, false);
                                } catch (TransformException ex) {
                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                    progress = 1;
                    mapViewer.recalculateShapes();
                    mapViewer.recomputeLegend();

                    mapViewer.repaint();

                }
            }
        };
        new Thread(r, "Init " + getDisplayName()).start();
        return true;
    }

    @Override
    public void removeTheme(MapViewer mapviewer
    ) {
        mapviewer.clearLayer(layerSurfaceMeasurementRaster);
    }

    @Override
    public void setDisplayTime(long displayTimeMS
    ) {

    }

   

}
