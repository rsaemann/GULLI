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
package com.saemann.gulli.core.model.underground;

import org.locationtech.jts.geom.Coordinate;

/**
 *
 * @author Karsten
 */
public class PointInfo {
    
    public final int Points;
    
    public final Coordinate[] Position; // [PunktID]
    
    public final float[][][] Velocity; // [timeindex][nodeindex][0:x/1:y/2:z]
    
    public final int[][] CellIDs;
    
    public PointInfo (int points, Coordinate[] position, float[][][] velocity, int[][] cellids){
        this.Points = points;
        this.Position = position;
        this.Velocity = velocity;
        this.CellIDs = cellids;
    }
    
    public static int[][] cellpoints_to_pointcells (int NumberOfPoints, int NumberOfCells, int[][] cellpoints){
        
        int[][] pointcells = new int[NumberOfPoints][161]; // Punkte mit dazugehörigen Zellen
        for (int[] pointcell : pointcells) {
            for (int j = 0; j < pointcell.length; j++) {
                pointcell[j] = -1;
            }
        }
        int[] stand = new int[NumberOfPoints]; // 
        for(int i=0; i<stand.length; i++){
            stand[i] = 0;
        }
//        int[] standmax = new int[163];
//        for(int i=0; i<standmax.length; i++){
//            standmax[i] = 0;
//        }
        
        for(int c=0;c<NumberOfCells;c++){ // über alle Zellen
            if (cellpoints[c] == null) {System.out.println("c = "+c); continue;}
            for(int p=0;p<cellpoints[c].length;p++){ // über alle Punkte dieser Zelle
                pointcells[cellpoints[c][p]][stand[cellpoints[c][p]]] = c;
                stand[cellpoints[c][p]]++;
//                standmax[stand[cellpoints[c][p]]]++;
            }
        }
        
        return pointcells;
    }
    
}
