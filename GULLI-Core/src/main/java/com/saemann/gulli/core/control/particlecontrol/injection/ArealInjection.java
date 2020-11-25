/*
 * The MIT License
 *
 * Copyright 2020 B1.
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
package com.saemann.gulli.core.control.particlecontrol.injection;

import com.saemann.gulli.core.model.surface.Surface;

/**
 * Stores the injection position of multiple particles. e.g. for diffusive pollution
 * @author saemann
 */
public class ArealInjection extends SurfaceInjection {
    
    protected int firstID=-1,lastID=-1;
    
    public ArealInjection(Surface surface) {
        super(surface, 0);
    }
    
    public void setParticleIDs(int firstID, int lastID){
        this.firstID=firstID;
        this.lastID=lastID;
    }
    
    @Override
    public double[] getInjectionPosition(int particleID) {
        return surface.getTriangleMids()[(int)getInjectionCellID(particleID)];
    }

    @Override
    public long getInjectionCellID(int particleID) {
//        System.out.println("Request Particle "+particleID+"'s Cell iD. first: "+firstID);
        try {
            return (long) ((surface.getTriangleMids().length-1) * ((particleID - firstID) / (double) (lastID - firstID)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    

   
    
    
    
}
