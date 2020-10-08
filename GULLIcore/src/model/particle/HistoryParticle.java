/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.particle;

import control.particlecontrol.injection.ParticleInjection;
import java.util.LinkedList;
import model.topology.Capacity;

/**
 * Particle with a history of visited locations
 *
 * @author saemann
 */
public class HistoryParticle extends Particle {

    protected LinkedList<Capacity> history = new LinkedList<>();

    public HistoryParticle(Material material, ParticleInjection injectionInformation, float mass, long injectionTime) {
        super(material, injectionInformation, mass, injectionTime);
    }

    public HistoryParticle(Material material, ParticleInjection injectionInformation, float mass) {
        super(material, injectionInformation, mass);
    }

    public void addToHistory(Capacity cap) {
        if (!history.isEmpty() && history.getLast().equals(cap)) {
            return;
        }
        this.history.add(cap);
    }

    public void clearHistory() {
        this.history.clear();
    }

    public Capacity getLastVisitedCapacity() {
        if (history.isEmpty()) {
            return null;
        }
        return history.getLast();
    }

    public LinkedList<Capacity> getHistory() {
        return history;
    }

}
