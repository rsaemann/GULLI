/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
package com.saemann.gulli.core.io;

import java.io.File;

/**
 *
 * @author Sämann
 */
public class FileContainer {

    public File pipeResult;
    public File pipeNetwork;

    public File surfaceResult;
    public File surfaceGeometry;
    private File inlets;

    private String crsPipes = null, crsSurface = null;

    private boolean pipeResultLoaded, pipeNetworkLoaded, surfaceResultLoaded, surfaceTopologyLoaded;

    public FileContainer(File pipeResult, File pipeNetwork, File surfaceResult, File surfaceDirectory, File inlets) {
        this.pipeResult = pipeResult;
        this.pipeNetwork = pipeNetwork;
        this.surfaceResult = surfaceResult;
        this.surfaceGeometry = surfaceDirectory;
        this.inlets = inlets;
    }

    public boolean isPipeResultLoaded() {
        return pipeResultLoaded;
    }

    public void setPipeResultLoaded(boolean pipeResultLoaded) {
        this.pipeResultLoaded = pipeResultLoaded;
    }

    public boolean isPipeNetworkLoaded() {
        return pipeNetworkLoaded;
    }

    public void setPipeNetworkLoaded(boolean pipeNetworkLoaded) {
        this.pipeNetworkLoaded = pipeNetworkLoaded;
    }

    public boolean isSurfaceResultLoaded() {
        return surfaceResultLoaded;
    }

    public void setSurfaceResultLoaded(boolean surfaceResultLoaded) {
        this.surfaceResultLoaded = surfaceResultLoaded;
    }

    public boolean isSurfaceTopologyLoaded() {
        return surfaceTopologyLoaded;
    }

    public void setSurfaceTopologyLoaded(boolean surfaceTopologyLoaded) {
        this.surfaceTopologyLoaded = surfaceTopologyLoaded;
    }

    public File getPipeResult() {
        return pipeResult;
    }

    public File getPipeNetwork() {
        return pipeNetwork;
    }

    public File getSurfaceResult() {
        return surfaceResult;
    }

    public File getSurfaceGeometry() {
        return surfaceGeometry;
    }

    public File getInlets() {
        return inlets;
    }

    public String getCrsPipes() {
        return crsPipes;
    }

    public void setCrsPipes(String crsPipes) {
        this.crsPipes = crsPipes;
    }

    public String getCrsSurface() {
        return crsSurface;
    }

    public void setCrsSurface(String crsSurface) {
        this.crsSurface = crsSurface;
    }

    @Override
    public String toString() {
        return "FileContainer{" + "pipeResult=" + pipeResult + ", pipeNetwork=" + pipeNetwork + ", surfaceResult=" + surfaceResult + ", surfaceDirectory=" + surfaceGeometry + ", inlets=" + inlets + ", pipeResultLoaded=" + pipeResultLoaded + ", pipeNetworkLoaded=" + pipeNetworkLoaded + ", surfaceResultLoaded=" + surfaceResultLoaded + ", surfaceTopologyLoaded=" + surfaceTopologyLoaded + '}';
    }
}
