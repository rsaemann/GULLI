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
package com.saemann.gulli.view.themelayer;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.view.PaintManager;
import com.saemann.rgis.view.MapViewer;

/**
 *
 * @author saemann
 */
public abstract class SurfaceThemeLayer implements ThemeLayer {

//    protected boolean trianglesAsNodes = false;
    protected long showTime = 0;
    /**
     * Initialization progress between 0.0 and 1.0 when initializing many shapes
     * as a thread.
     */
    public float progress = -1;

    /**
     * Is it worth to repaint the layer with changing time?
     *
     * @return
     */
    public boolean isDynamic() {
        return false;
    }

    /**
     * Display name used in the UserInterface (e.g. combo box for chosing theme)
     *
     * @return
     */
    public abstract String getDisplayName();

    /**
     * Initilizes a Theme by adding the shapes to layers in the MapViewer. This
     * should be done inside a seperate Thread. The progress should be readable
     * from the main Thread via the **getInitializationProgress** method.
     *
     * @param mapviewer
     * @param c controller
     * @param pm
     * @param asNodes
     * @return true for successfull initilization, false for missing elements
     */
    public abstract boolean initializeTheme(MapViewer mapviewer, Controller c, PaintManager pm, boolean asNodes);

    @Override
    public float getInitializationProgress() {
        return progress;
    }

    /**
     * Set if the shape should be a single location node (true) or a complex 2D
     * shape (false).
     *
     * @param mapViewer
     * @param c
     * @param pm
     * @param asNodes
     */
    public void setDrawnAsNodes(MapViewer mapViewer, Controller c, PaintManager pm, boolean asNodes) {
        removeTheme(mapViewer);
        initializeTheme(mapViewer, c, pm, asNodes);
    }

}
