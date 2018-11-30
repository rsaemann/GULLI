/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.ogs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 * @author Karsten
 */
public class read {

//    public static int [] read_cellpoints(File fileCellInfos) throws FileNotFoundException, IOException {
//        
//        FileReader fr = new FileReader(fileCellInfos);
//        BufferedReader br = new BufferedReader(fr);
//        String line = "";
//        String[] parts;
//        int numberofcells = -1;
//        
//        while (br.ready()) {
//            line = br.readLine();
//            if(line.contains("NumberOfCells")){
//                parts = line.trim().split(" ");
//                numberofcells = Integer.parseInt(parts[1]);
//            }
//            if(line.contains("0")){
//                break;
//            }
//        }
//        
//        int[] cellinfos = new int[numberofcells];
//        
//        return cellinfos;
//    }
    
    public static double[][] concentration(String ic, int ps) throws IOException{
        // Auslesen der outflow-Punkte für die Benchmark
        // Dateiaufbau: x-Koordinate y-Koordinate z-Koordinate
        // eine Zeile = ein Outflowpunkt
        
        double[][] c = new double[ps][3]; // ps Punkte mit x y z
        
        FileReader fr = new FileReader(ic);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        String[] parts;
        int p = 0;
        
        while(br.ready()){
            try{c[p][0] = 0;} catch(Exception e) {break;}
            line = br.readLine();
            parts = line.trim().split(" ");
            
            for (int i = 0; i < 3; i++) {
               c[p][i] = Double.parseDouble(parts[i]); 
            }
            
            p++;
        }
        
        return c;
    }
    
    public static int[] material(int NumberOfCells, File fileGroundwater) throws IOException {
        // Auslesen der Materialgruppe einer Zelle aus der vtu-Datei File
        // File wird in FinLeackagePipes, Abschnitt "Szenarios" definiert
        
        FileReader fr = new FileReader(fileGroundwater);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        String[] parts;
        int[] material = new int[NumberOfCells];
        
        while (br.ready()) {
            line = br.readLine();
            if(line.contains("Name=\"MatGroup")){
                line = br.readLine();
                parts = line.trim().split(" ");
                for (int i=0;i<NumberOfCells; i++){
                    material[i] = Integer.parseInt(parts[i]);
                }
                break;
            }
        }
        br.close();
        fr.close();
//        write.write_tabbed_data(material, "MaterialIDs");
//        System.exit(0);
        return material;
    }
    
    public static int[][] cellpoints (int NumberOfCells, File fileGroundwater) throws FileNotFoundException, IOException{
        // Auslesen, welche Punkte zu einer Zelle gehören
        
        FileReader fr = new FileReader(fileGroundwater);
        BufferedReader br = new BufferedReader(fr);
        String line = "";
        String[] parts;
        
        //voranschreiten bis zum Block "connectivity"
        while (br.ready()) {
            line = br.readLine();
            if (line.contains("Name=\"connectivity")) {
                break;
            }
        }
        // notwendige Variablen erstellen
        int[][] cellpoints = new int[NumberOfCells][];
        int cellakt = 0; // zu betrachtende aktuelle Zelle
        String pointsakt[]; //die Punkte der aktuellen Zelle in Textform
        // reihenweises Einlesen und Zuordnen der
        // Punkte cellpoints[][points] zu  Zellen cellpoints[cell][]
        while (br.ready()) {
            line = br.readLine();
            if (line.contains("</DataArray>")) {
                break;
            }
            pointsakt = line.trim().split(" ");
            //zum Durchlaufen der einzelnen Punkte[0] bis [letzter Punkt]
            cellpoints[cellakt] = new int[pointsakt.length];
            for (int p=0; p < pointsakt.length; p++) {
                // aktuellen Punkt hinzufügen zu Punkte-Liste der aktuellen Zelle
                cellpoints[cellakt][p] = Integer.parseInt(pointsakt[p]);
            }
            cellakt++;
        }
        
        br.close();
        fr.close();
        
        return cellpoints;
    }
    
}
