/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.underground;

import com.vividsolutions.jts.geom.Coordinate;

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
