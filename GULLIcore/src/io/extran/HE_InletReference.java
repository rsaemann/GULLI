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
package io.extran;

/**
 * Information from SURF-SEWER_NODES.dat
 * @author saemann
 */
public class HE_InletReference {
    
    public final String inlet_Name;
    public final double x,y;
    /**
     * In File index
     */
    public final int index;
    
    public final float height;
    
    public final String capacityName;
    
    public final int triangleID;

    public HE_InletReference(String inlet_Name, double x, double y, int index, float height, String capacityName, int triangleID) {
        this.inlet_Name = inlet_Name;
        this.x = x;
        this.y = y;
        this.index = index;
        this.height = height;
        this.capacityName = capacityName;
        this.triangleID = triangleID;
    }
    
    
}
