package model.particle;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A Particle Queue sorts the particles along their position inside a pipe.
 * @author saemann
 */
public class ParticleQueue extends LinkedList<Particle> {

    private static final Comparator<Particle> comparator = new Comparator<Particle>() {

        @Override
        public int compare(Particle t, Particle t1) {
            if (t.getPosition1d_actual() < t1.getPosition1d_actual()) {
                return -1;
            }
            return 0;
        }
    };

    public void insert(Particle p) {
        if (this.size() < 1) {
            this.add(p);
            return;
        }
        if (p.getPosition1d_actual() > peekLast().getPosition1d_actual()) {
            addLast(p);
            return;
        }
        if (p.getPosition1d_actual() < peekFirst().getPosition1d_actual()) {
            addFirst(p);
            return;
        }
        Iterator<Particle> it = descendingIterator();
        int position = size();
        while (it.hasNext()) {
            Particle n = it.next();

            if (n.getPosition1d_actual() > p.getPosition1d_actual()) {
                add(position, p);
                return;
            }
            position--;
        }
        System.err.println("Inserting of Particle was not successfull.");
    }

    public void sort() {
        Collections.sort(this, comparator);
    }

    public double[] getPipeorientedSpaceForParticle(Particle p) {
        double hinten = 0, vorne = Double.POSITIVE_INFINITY;
        Iterator<Particle> it = descendingIterator();
        while (it.hasNext()) {
            Particle now = it.next();
            if (p == now) {
                if (it.hasNext()) {
                    vorne = it.next().getPosition1d_actual();
                    break;
                } else {

                }
                break;
            } else {
                hinten = now.getPosition1d_actual();
            }
        }
        return new double[]{p.getPosition1d_actual() - hinten, vorne - p.getPosition1d_actual()};
    }

    public double[] getParticleorientedSpaceForParticle(Particle p) {
        double hinten = 0, vorne = Double.POSITIVE_INFINITY;
        Iterator<Particle> it = descendingIterator();
        while (it.hasNext()) {
            Particle now = it.next();
            if (p == now) {
                if (it.hasNext()) {
                    vorne = it.next().getPosition1d_actual();
                    break;
                } else {

                }
                break;
            } else {
                hinten = now.getPosition1d_actual();
            }
        }
        if (p.getVelocity1d() > 0) {
            return new double[]{p.getPosition1d_actual() - hinten, vorne - p.getPosition1d_actual()};
        } else {
            return new double[]{vorne - p.getPosition1d_actual(), p.getPosition1d_actual() - hinten};
        }
    }

}
