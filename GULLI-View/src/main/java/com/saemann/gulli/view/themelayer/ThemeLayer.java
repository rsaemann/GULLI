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
import com.saemann.rgis.view.MapViewer;

/**
 * Creating a visualization of a selected theme. A Theme can conatin multiple
 * layer definitions.
 *
 * @author saemann
 */
public interface ThemeLayer {

    

    /**
     * Remove this theme by clearing the used layers from the mapviewer.
     *
     * @param mapviewer
     */
    public void removeTheme(MapViewer mapviewer);

    /**
     * Update the shapes and color for dynmic themes
     * @param displayTimeMS 
     */
    public abstract void setDisplayTime(long displayTimeMS);

    /**
     * The Progress of initialization between 0.0 and 1.0
     * @return 
     */
    public abstract float getInitializationProgress();
}
