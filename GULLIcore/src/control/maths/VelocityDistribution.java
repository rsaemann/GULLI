
package control.maths;

import java.util.Random;

/**
 * Computes the Velocity factor as 1.25*r^(19/6).
 * @author saemann
 */
public class VelocityDistribution extends RandomDistribution {

    protected double[] lookup;

    public VelocityDistribution(Random random) {
        super(random);
        lookup = new double[500];
        double nenner=lookup.length;
        for (int i = 0; i < lookup.length; i++) {
            double y = i/nenner;
            lookup[i] = 1.25 * Math.pow(y, 19. / 6.);
        }
    }

    @Override
    public double nextDouble() {
        return lookup[(int) (random.nextDouble() * lookup.length)];
    }

}
