/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.listener;

import java.util.Collection;
import model.particle.Particle;

/**
 *
 * @author saemann
 */
public interface ParticleListener {

    /**
     * Listener shall take the referenced particles.
     *
     * @param particles
     * @param source
     */
    public void setParticles(Collection<Particle> particles, Object source);

    /**
     * Listener is instructed to clear its particles
     *
     * @param source
     */
    public void clearParticles(Object source);
}
