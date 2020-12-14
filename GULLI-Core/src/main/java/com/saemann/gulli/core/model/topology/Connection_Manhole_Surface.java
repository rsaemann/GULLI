/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.surface.Surface;

/**
 *
 * @author saemann
 */
public class Connection_Manhole_Surface implements Connection_Manhole, Connection_ToSurface {

    protected StorageVolume manhole;

    protected Position3D position;

    protected int surfaceTriangleId;
    protected Surface surf;

    public Connection_Manhole_Surface(StorageVolume manhole, Position3D position, int surfaceTriangleid, Surface surface) {
        this.manhole = manhole;
        this.position = position;
        this.surfaceTriangleId = surfaceTriangleid;
    }

    public int getSurfaceTriangleId() {
        return surfaceTriangleId;
    }

    public void setSurfaceTriangleId(int surfaceTriangle) {
        this.surfaceTriangleId = surfaceTriangle;
    }

    @Override
    public Position3D getPosition() {
        return position;
    }

    @Override
    public float getHeight() {
        return (float) position.z;
    }

    @Override
    public StorageVolume getManhole() {
        return manhole;
    }

    @Override
    public Capacity getConnectedCapacity() {
        return surf;
    }

    @Override
    public boolean emitsToSurfaceDomain() {
       return true;
    }

    @Override
    public Capacity targetCapacity() {
        return surf;
    }

    @Override
    public int targetSurfaceID() {
        return surfaceTriangleId;
    }

}