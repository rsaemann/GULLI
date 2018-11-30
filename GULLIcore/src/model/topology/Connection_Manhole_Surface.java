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
package model.topology;

import model.surface.SurfaceTriangle;

/**
 *
 * @author saemann
 */
public class Connection_Manhole_Surface implements Connection_Manhole {
    
     protected StorageVolume manhole;
     
     protected Position3D position;
     
     protected SurfaceTriangle surfaceTriangle;

    public Connection_Manhole_Surface(StorageVolume manhole, Position3D position, SurfaceTriangle surfaceTriangle) {
        this.manhole = manhole;
        this.position = position;
        this.surfaceTriangle = surfaceTriangle;
    }

    public SurfaceTriangle getSurfaceTriangle() {
        return surfaceTriangle;
    }

    public void setSurfaceTriangle(SurfaceTriangle surfaceTriangle) {
        this.surfaceTriangle = surfaceTriangle;
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
        return surfaceTriangle;
    }

}
