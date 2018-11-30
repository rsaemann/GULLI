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
package view.themelayer.pipe;

import control.Controller;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import model.GeoPosition2D;
import model.topology.Pipe;
import view.GradientColorHolder;
import view.MapViewer;
import view.PaintManager;
import view.shapes.LinePainting;
import view.themelayer.PipeThemeLayer;

/**
 *
 * @author saemann
 */
public class PipeTheme_MaxFill extends PipeThemeLayer {
    
    private static final String name = "Max Waterlevel";
    
    public static GradientColorHolder chPipeMax = new GradientColorHolder(0, 1, Color.BLUE, Color.red, 256, "Max Fill");
    
    public final String layerName = PaintManager.layerPipes + "_MaxFill";
    
    public PipeTheme_MaxFill() {
        chPipeMax.setStroke(new BasicStroke(2));
    }
    
    @Override
    public void initializeTheme(final MapViewer mapViewer, Controller c) {
        for (Pipe pipe : c.getNetwork().getPipes()) {
            float maxWL = 0;
            for (int i = 0; i < pipe.getStatusTimeLine().getNumberOfTimes(); i++) {
                maxWL = Math.max(maxWL, pipe.getStatusTimeLine().getWaterlevel(i));
            }
            final double rate = pipe.getProfile().getFillRate(maxWL);
            LinePainting ap = new LinePainting(pipe.getAutoID(), new GeoPosition2D[]{pipe.getStartConnection().getPosition(), pipe.getEndConnection().getPosition()}, chPipeMax) {
                
                @Override
                public boolean paint(Graphics2D g2) {
                    g2.setColor(chPipeMax.getGradientColor(rate));
                    return super.paint(g2);
                }                
            };
//            System.out.println(maxWL + "/" + pipe.getProfile() + "    " + pipe.getProfile().getFillRate(maxWL) + " -> " + index + "    " + chPipeMax.getGradientColor(rate));
//            ap.setGradientColorIndex(index);
            
            if (pipesAsArrows) {
                ap.arrowheadvisibleFromZoom = 0;
            } else {
                ap.arrowheadvisibleFromZoom = 100;
            }
            mapViewer.addPaintInfoToLayer(layerName, ap);
        }
        mapViewer.recalculateShapes();
        mapViewer.recomputeLegend();
    }
    
    @Override
    public void removeTheme(MapViewer mapviewer) {
        mapviewer.clearLayer(layerName);
        mapviewer.recalculateShapes();
        mapviewer.recomputeLegend();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
}
