package control.maths;

import java.util.Random;

/**
 *
 * @author saemann
 */
public class GaussDistribution extends RandomDistribution {

//    public int[] verteilung;
//    private double[] xe;
//    private int index = 0;
    public GaussDistribution(Random random) {
        super(random);
//        index = 0;
//        xe = new double[10000];
//        for (int i = 0; i < xe.length; i++) {
//            xe[i] = random.nextGaussian();
//        }
////        verteilung=new int[xe.length];
//        for (int i = 0; i < xe.length; i++) {
//            xe[i]=-4+(i/(double)xe.length)*8.;            
//        }

    }

    @Override
    public double nextDouble() {
        return super.random.nextDouble();
//        double r= random.nextGaussian();
//        if(r<xe[0]){
//            verteilung[0]++;
//        }else if(r>=xe[xe.length-1]){
//            verteilung[verteilung.length-1]++;
//        }else{
//            verteilung[(int)(((r+4.)/8.)*(double)verteilung.length)]++;
//        }
//        return r;
    }

    /**
     * Returns a normal-distributed random number
     *
     * @return
     */
    public double nextGaussian() {
        return super.random.nextGaussian();
//        if (index >= xe.length) {
//            index = 0;
//        }
//        return xe[index++];
    }

    public void setRandom(Random random) {
        this.random = random;
//        index = 0;
//        xe = new double[10005];
//        for (int i = 0; i < xe.length; i++) {
//            xe[i] = random.nextGaussian();
//        }
    }

    @Override
    public void setRandomGenerator(Random rand) {
        this.setRandom(random);
    }

}
