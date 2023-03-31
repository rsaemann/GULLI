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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.opengis.referencing.operation.TransformException;

/**
 * A Surface theme layer to show the cells of the surface topology.
 *
 * @author Robert
 */
public class SurfaceTheme_Neighbours extends SurfaceThemeLayer {

    public final static String layerSurfaceNeighbours = PaintManager.layerTriangle + "_Neighbours";
    private final DoubleColorHolder chNeighbour1 = new DoubleColorHolder(Color.cyan, new Color(1f, 1f, 1f, 0f), "Surface Neighbours");
    public static int maxShapeCount=1000000;

    @Override
    public boolean initializeTheme(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {
        mapViewer.clearLayer(layerSurfaceNeighbours);
        Surface surface = c.getSurface();
        Layer layer_ = mapViewer.getLayer(layerSurfaceNeighbours);
        if (layer_ == null) {
            layer_ = new Layer(layerSurfaceNeighbours, chNeighbour1);
            mapViewer.getLayers().add(layer_);
        }
        final Layer layer = layer_;
        new Thread() {
            @Override
            public void run() {
                progress = 0;
                if (c.getSurface() != null) {
                    if (c.getSurface().neumannNeighbours != null) {
                        int[][] neumann = c.getSurface().neumannNeighbours;
                        double[][] mids = c.getSurface().getTriangleMids();
                        int index = 0;
                        Coordinate start = new CoordinateXY(), end = new CoordinateXY();
                        Coordinate startWGS = new CoordinateXY(), endWGS = new CoordinateXY();
                        for (int i = 0; i < mids.length; i++) {
                            if(index>maxShapeCount){
                                System.out.println("Limit maximum number of neighbour-shapes to "+getClass()+".maxShapeCount="+maxShapeCount);
                                break;
                            }
                            try {
                                start.x = mids[i][0];
                                start.y = mids[i][1];
                                c.getSurface().getGeotools().toGlobal(start, startWGS, true);
//                                System.out.println("Neighbours of "+i+" are "+neumann[i][0]+", "+neumann[i][1]+", "+neumann[i][2]);
                                for (int j = 0; j < 3; j++) {
                                    int neighbourID=neumann[i][j];
                                    if(neighbourID<0)continue;
                                    
                                 
                                    end.x = mids[neighbourID][0];
                                    end.y = mids[neighbourID][1];
                                    c.getSurface().getGeotools().toGlobal(end, endWGS, true);
                                
                                    //Create line from the center towards the neighbouring cell. ended at the border=at the edge.
                                    LinePainting lp=new LinePainting(index++, startWGS.x, (startWGS.x+endWGS.x)*0.5,startWGS.y,  (startWGS.y+endWGS.y)*0.5, chNeighbour1);//, (startWGS.x+endWGS.x)*0.5, (startWGS.y+endWGS.y)*0.5
                                    lp.arrowheadvisibleFromZoom=21;
                                    layer.add(lp, false);
                                
                                }          
                            } catch (TransformException ex) {
                                Logger.getLogger(SurfaceTheme_Neighbours.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
//                        System.out.println(layer.size()+" Neighbour layer created.");
                    }

//                    if (asNodes) {
//                        int id = 0;
//                        if (c.getSurface().getTriangleMids() != null) {
//                            for (double[] triangleMid : c.getSurface().getTriangleMids()) {
//                                try {
//                                    NodePainting np = new NodePainting(id, c.getSurface().getGeotools().toGlobal(new Coordinate(triangleMid[0], triangleMid[1])), chNeighbour1);
//                                    id++;
//                                    layer.add(np, false);
//                                    if (id > maximumNumberOfSurfaceShapes) {
//                                        break;
//                                    }
//                                } catch (TransformException ex) {
//                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
//                                }
//                            }
//                        } else {
//                            System.err.println("Cell mid vertices of surface not defined yet.");
//                        }
//                    } else {
//                        try {
//                            AreaPainting[] shapes = null;
//                            if (maximumNumberOfSurfaceShapes < surface.getTriangleNodes().length) {
//                                shapes = new AreaPainting[maximumNumberOfSurfaceShapes + 1];
//                            } else {
//                                shapes = new AreaPainting[(surface.getTriangleNodes().length)];
//                            }
//                            int id = 0;
//                            Coordinate[] coords = new Coordinate[4];
//                            for (int[] nodes : surface.getTriangleNodes()) {
//
//                                for (int j = 0; j < 3; j++) {
//                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
//                                }
//                                coords[3] = coords[0];//Close ring
//                                AreaPainting ap = new AreaPainting(id, chNeighbour1, c.getSurface().getGeotools().gf.createLinearRing(coords));
//                                shapes[id] = ap;
//
//                                id++;
//                                if (id > maximumNumberOfSurfaceShapes) {
//                                    break;
//                                }
//                                if (id % 100000 == 0) {
//                                    System.out.println("Gridshape " + (id * 100 / c.getSurface().getTriangleNodes().length) + "% loaded.");
//                                }
//                            }
//                            layer.setPaintElements(shapes);
//                        } catch (TransformException ex) {
//                            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
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
        mapviewer.clearLayer(layerSurfaceNeighbours);
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
