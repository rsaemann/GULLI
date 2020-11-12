package run;



/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author saemann
 */
public class PackageTest {
    public static void main(String[] args) {

        try {
           Class.forName("org.opengis.referencing.FactoryException");
        } catch (Exception e) {
            System.out.println("Es fehlt das Package gt-opengis for loading System.org.opengis.referencing.FactoryException");
            e.printStackTrace();
        }
        
    }
}
