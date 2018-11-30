/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.maths;

import java.util.Random;

/**
 *
 * @author saemann
 */
public class TriangleDistribution extends RandomDistribution {

    private final double abzug;
    private final double breite;
//    private double mittelwert;
//    private double a, b;
//    private double a2, b2;
    public int[] verteilung;
    public double[] xe;
    private final double[] projection;

    public TriangleDistribution(Random random, double mittelwert, double breite) {
        super(random);
//        this.mittelwert = mittelwert;
        this.abzug = -mittelwert + breite / 2.;
        this.breite = breite;
//        this.a = mittelwert - breite / 2.;
//        this.b = mittelwert + breite / 2.;
//        a2 = a * a;
//        b2 = b * b;
        verteilung = new int[101];
        xe = new double[verteilung.length];
        for (int i = 0; i < xe.length; i++) {
            xe[i] = (i/(double)xe.length) * breite - (breite - mittelwert) / 2.;
        }
        /* Fill the projection array with precalculated values so the 
         calculation is not neede in the future.*/
        projection = new double[7000];
        for (int i = 0; i < projection.length; i++) {
            double x = i / (double) projection.length;
            double r;
            if (x < 0.5) {
                r = Math.sqrt(x * 0.5);
            } else {
                r = 1. - Math.sqrt(0.5 * (1. - x));
            }
            double retur = r * breite - abzug;
            projection[i] = retur;
        }

    }

    @Override
    public double nextDouble() {
        double x = random.nextDouble();
//        double r;
//        if (x < 0.5) {
//            r = Math.sqrt(x * 0.5);
//        } else {
//            r = 1 - Math.sqrt(0.5 * (1 - x));
//        }
//       if(x<0.5){
//           r=x*x*2.;
//       }else{
//           r=1-(x*x-2*x+1)*2.;
//       }

//        double retur = r * breite - abzug;
        double retur = projection[(int) (projection.length * x)];
        verteilung[(int) ((retur + (breite) / 2.) / breite*(double)verteilung.length)]++;
//        System.out.println("abzug="+abzug);
//        System.out.println("x="+x+"\t r="+r+"\t s="+retur);
        return retur;
    }

}
