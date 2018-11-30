/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.measurement;

import java.util.Collection;
import model.particle.Particle;

/**
 *
 * @author saemann
 */
public interface ParticleMeasurement {

    /**
     * Measure this collection of particles. Sticks to multiple given
     * collections. To do a new measurement, call resetCounter or writeCounter
     * before.
     *
     * @param c
     */
    public void measureParticles(Collection<Particle> c);

    /**
     * Write the actual collected data to the Pipe's timeline at given
     * timestamp. This Method calls {@link resetCounter} after writing.
     *
     * @param timestamp position in timeline where to put the actual data.
     */
    public void writeCounter(long timestamp);

    /**
     * Resets the measurement.
     */
    public void resetCounter();

}
