/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.particle;

import java.util.LinkedList;
import model.topology.Capacity;

/**
 * Particle with a history of visited locations
 *
 * @author saemann
 */
public class HistoryParticle extends Particle {

    protected LinkedList<Capacity> history = new LinkedList<>();

    public HistoryParticle(Capacity injectionSurrounding, double injectionPosition1D) {
        super(injectionSurrounding, injectionPosition1D);
    }

    public HistoryParticle(Capacity injectionSurrounding, double injectionPosition1D, long injectionTime) {
        super(injectionSurrounding, injectionPosition1D, injectionTime);
    }

    public HistoryParticle(Capacity injectionSurrounding, double injectionPosition1D, long injectionTime, float mass_kg) {
        super(injectionSurrounding, injectionPosition1D, injectionTime, mass_kg);
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
