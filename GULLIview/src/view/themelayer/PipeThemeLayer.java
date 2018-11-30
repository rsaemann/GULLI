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
package view.themelayer;

import control.Controller;
import view.MapViewer;
import view.themelayer.pipe.PipeTheme_GreyPipes;
import view.themelayer.pipe.PipeTheme_MaxFill;

/**
 * A Layer to display something about the Pipenetwork.
 * @author saemann
 */
public abstract class PipeThemeLayer implements ThemeLayer {

    protected boolean pipesAsArrows = false;

    public enum LAYERS {

        NONE(new PipeThemeLayer() {

            @Override
            public void initializeTheme(MapViewer mapviewer, Controller c) {
            }

            @Override
            public void removeTheme(MapViewer mapviewer) {
            }

            @Override
            public String getName() {
                return "";
            }
        }),
        PIPES(new PipeTheme_GreyPipes()), 
        MAX_WL(new PipeTheme_MaxFill());

        public final PipeThemeLayer ptl;

        private LAYERS(PipeThemeLayer ptl) {
            this.ptl = ptl;
        }

        public PipeThemeLayer getTheme() {
            return ptl;
        }

    };

    @Override
    public abstract void initializeTheme(MapViewer mapviewer, Controller c);

    @Override
    public abstract void removeTheme(MapViewer mapviewer);

    public abstract String getName();
    
    public void setTimeToShow(long timeToShow){
    };

}
