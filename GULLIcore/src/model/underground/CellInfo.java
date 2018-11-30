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
public class CellInfo {
    
    public final int Cells; // NumberOfCells
    
    public final int[] Material; // [cell]
    
    public final int[][] PointIDs; // [cell][point]
    
    public CellInfo (int cells, int[] material, int[][] pointids){
        this.Cells = cells;
        this.Material = material;
        this.PointIDs = pointids;
    }
    
    public static boolean isinCell (Coordinate point, int cellid, CellInfo ci, PointInfo pi, int dim) {
        
        boolean isinbound = false;
        
        int iswest = 0;
        int iseast = 0;
        int isnorth = 0;
        int issouth = 0;
        int isabove = 0;
        int isunder = 0;
        
        if (dim == 2){ // für den zweidimensionalen Fall in x-y-Ebene
            isabove = 1; // letztendlich: Zellzugehörigkeit nicht auf z-Koordinate prüfen
            isunder = 1;
        }
                
        for (int i = 0; i < ci.PointIDs[cellid].length; i++) { // über jeden Punkt i der Zelle cellid
            if (pi.Position[ci.PointIDs[cellid][i]].x <= point.x){iseast++;} else
                if (pi.Position[ci.PointIDs[cellid][i]].x >= point.x){iswest++;}
            if (pi.Position[ci.PointIDs[cellid][i]].y <= point.y){isnorth++;} else
                if (pi.Position[ci.PointIDs[cellid][i]].y >= point.y){issouth++;}
            if (pi.Position[ci.PointIDs[cellid][i]].z <= point.z){isabove++;} else
                if (pi.Position[ci.PointIDs[cellid][i]].z >= point.z){isunder++;}
        }
        // für die 2D-Benchmark
//        isnorth = 1;
        if ((iseast > 0) && (iswest > 0) && (isnorth > 0) && (issouth > 0) && (isabove > 0) && (isunder > 0)){
            isinbound = true;
        }
        
        return isinbound;        
    }
    

    public static double getCellVolume(int cellid, CellInfo ci, PointInfo pi) {
        // nach Satz des Heron als Grundfläche
        double vol = 0;
        double h = 1;
        
        double[][] xy = new double[3][2]; // [PunktID] [x y]
        try {
            h = Math.abs( pi.Position[ci.PointIDs[cellid][0]].z - pi.Position[ci.PointIDs[cellid][3]].z );
        } catch (Exception e) {
//            System.out.println("Zellhöhe: " +h);
        }
//        System.out.println("h " + h);
        
        // Koordinaten x y dreier Punkte i der cellid auslesen
        for (int i = 0; i < 2; i++) {
            xy[i][0] = pi.Position[ci.PointIDs[cellid][i]].x;
            xy[i][1] = pi.Position[ci.PointIDs[cellid][i]].y;
//            System.out.println("xy[0]:" + xy[i][0]+"   xy[1]:"+xy[i][1]);
        }
        
        // Seitenlängen der Grundfläche berechnen
        double a = Math.sqrt( Math.pow( xy[1][0]-xy[2][0],2 ) + Math.pow( xy[1][1]-xy[2][1],2 ) );
        double b = Math.sqrt( Math.pow( xy[2][0]-xy[0][0],2 ) + Math.pow( xy[2][1]-xy[0][1],2 ) );
        double c = Math.sqrt( Math.pow( xy[0][0]-xy[1][0],2 ) + Math.pow( xy[0][1]-xy[1][1],2 ) );
//        System.out.println("a: "+ a+"   b: "+b+"   c: "+c);
        
        // Grundfläche des Prismas berechnen
        double s = (a + b + c) / 2;
//        System.out.println("s " + s);
        double F = Math.sqrt( s * (s-a) * (s-b) * (s-c) );
//        System.out.println("F " + F);
        
        // Volumen berechnen und zurückgeben
        return vol = F * h;
    }
}
