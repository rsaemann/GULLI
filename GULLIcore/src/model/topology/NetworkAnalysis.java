/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology;

import io.extran.HE_Database;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tools for analysing the pipe Network
 *
 * @author saemann
 */
public class NetworkAnalysis {

    public static double calcmeanSlope(Network nw) {
        double length = 0;
        double lengthslope = 0;

        for (Pipe p : nw.getPipes()) {
            length += p.getLength();
            lengthslope += Math.abs(p.getDecline() * p.getLength());
        }
        return lengthslope / length;
    }

    public static double calcLength(Network nw) {
        double length = 0;

        for (Pipe p : nw.getPipes()) {
            length += p.getLength();

        }
        return length;
    }
    
    public static double calcMaxPipeLength(Network nw){
        double length=0;
        for (Pipe pipe : nw.getPipes()) {
            length=Math.max(length, pipe.getLength());
        }
        return length;
    }
    
    public static double calcMinPipeLength(Network nw){
        double length=Double.POSITIVE_INFINITY;
        for (Pipe pipe : nw.getPipes()) {
            length=Math.min(length, pipe.getLength());
        }
        return length;
    }

    public static void main1(String[] args) {
        try {
            File dbfile = new File("C:\\Users\\saemann\\Documents\\NetBeansProjects\\GULLI\\input\\Modell2017Mai\\2D_Model\\Model.idbf");
            Network nw = HE_Database.loadNetwork(dbfile);
            System.out.println("length: " + NetworkAnalysis.calcLength(nw) + "\t with " + nw.getPipes().size()+" pipes");
            System.out.println("length range from "+NetworkAnalysis.calcMinPipeLength(nw)+"m \tto "+NetworkAnalysis.calcMaxPipeLength(nw)+"m.");
            System.out.println("mean: " + NetworkAnalysis.calcmeanSlope(nw) * 100 + "% slope weighted with length");
        } catch (SQLException ex) {
            Logger.getLogger(NetworkAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(NetworkAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NetworkAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
