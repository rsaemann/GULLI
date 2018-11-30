package io.ogs;



import java.io.*;
import model.underground.CellInfo;
import model.underground.PointInfo;

/**
 *
 * @author Karsten
 */
public class write {

    public static void write_particlehistory(int outflowid, double[][][] data, String filename) throws IOException{
        // zum Übertragen in den Matlab-Plot

        // data = [particleNummer][Zeitindex][Richtung]
        String speicherort = ("output\\ParticleHistory\\" + filename + ".txt");
        FileWriter fw = new FileWriter(speicherort, true); //true: datei nicht überschreiben
        
        try(BufferedWriter bw = new BufferedWriter(fw)){
        
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    //OutflowID ParticleID TimeID [xyzRichtung]
                    bw.write(outflowid +" "+ i +" "+ j +" "+ data[i][j][0] +" "+ data[i][j][1] +" "+ data[i][j][2]);
                    bw.newLine();
                }
            }
            bw.close();
        }
    }
    
    public static void write_concentration(int[][] data, CellInfo ci,PointInfo pi, String filename) throws IOException {
        // zum Übertragen in den Matlab-Plot
        
        // Zellmitte --> Position des ersten Punktes einer Zelle
        // data = [outputindex][cellID]Partikelanzahl
        String speicherort = ("output\\ParticleHistory\\" + filename + ".txt");
        FileWriter fw = new FileWriter(speicherort, true); //true: datei nicht überschreiben
        
        try(BufferedWriter bw = new BufferedWriter(fw)){
        
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    // verwende die Koordinaten von Punkt 1 als Zellpositionrepräsentant
                    int px = (int) pi.Position[ci.PointIDs[j][1]].x;
                    int py = (int) pi.Position[ci.PointIDs[j][1]].y;
                    int pz = (int) pi.Position[ci.PointIDs[j][1]].z;
                    //write: outputID cellID AnzahlPartikel x y z
                    if (data[i][j] != 0){
                        bw.write(i +" "+ j +" "+ data[i][j] +" "+ px +" "+ py +" "+ pz);
                        bw.newLine();
                    }
                }
            }
            bw.close();
        }
    }
    
    public static void write_data(String[] data, String filename) throws IOException{
        // Daten des Arrays data Zeilenweise in Textdatei schreiben
        String speicherort = ("output\\" + filename + ".txt");
        FileWriter fw = new FileWriter(speicherort);
        
        try(BufferedWriter bw = new BufferedWriter(fw)){
        
            for (String data1 : data) {
                bw.write(data1);
                bw.newLine();
            }
            bw.close();
        }
    }
    
    
//    public static void write_tabbed_data(int[] data, String filename) throws IOException{
//        String speicherort = ("output\\" + filename + ".txt");
//        FileWriter fw = new FileWriter(speicherort);
//        
//        try(BufferedWriter bw = new BufferedWriter(fw)){
//            
//            for (int i = 0; i < data.length; i++) {
//                bw.write(data[i]+"\t");
//            }
//            
//            bw.close();
//        }
//    }

}