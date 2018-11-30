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
public class UniformDistribution extends RandomDistribution{
    
    private double abzug;
    private double breite;

    public UniformDistribution(Random random,double mittelwert, double breite) {
        super(random);
        this.abzug=-mittelwert+breite/2.;
        this.breite=breite;
    }

    @Override
    public double nextDouble() {
        return random.nextDouble()*breite-abzug;
    }
    
}
