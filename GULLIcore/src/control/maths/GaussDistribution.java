package control.maths;

import java.util.Random;

/**
 *
 * @author saemann
 */
public class GaussDistribution extends RandomDistribution {

//    public int[] verteilung;
//    public double[] xe;

    public GaussDistribution(Random random) {
        super(random);
        
//        xe=new double[100];
////        verteilung=new int[xe.length];
//        for (int i = 0; i < xe.length; i++) {
//            xe[i]=-4+(i/(double)xe.length)*8.;            
//        }
        
    }

    @Override
    public double nextDouble() {
        double r= random.nextGaussian();
//        if(r<xe[0]){
//            verteilung[0]++;
//        }else if(r>=xe[xe.length-1]){
//            verteilung[verteilung.length-1]++;
//        }else{
//            verteilung[(int)(((r+4.)/8.)*(double)verteilung.length)]++;
//        }
        return r;
    }

}
