/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.particle;

import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.control.particlecontrol.injection.ParticleInjection;
import com.saemann.gulli.core.model.GeoPosition2D;
import java.util.LinkedList;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Position3D;
import org.locationtech.jts.geom.Coordinate;

/**
 * Particle with a history of visited locations
 *
 * @author saemann
 */
public class HistoryParticle extends Particle {

    protected LinkedList<Capacity> history = new LinkedList<>();
    /**
     * longitude, latitude
     */
    protected LinkedList<Coordinate> positions = new LinkedList<>();

    protected double lastUTMX, lastUTMy;

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
        Position3D pos = cap.getPosition3D(position1d_actual);
        this.addToHistory(pos,pos.getX(),pos.getY());
        this.history.add(cap);
    }

    public void addToHistory(GeoPosition2D longlat, double utmX, double utmY) {
//        if(longlat.getLongitude()==0)return;
        this.positions.add(new Coordinate(longlat.getLongitude(), longlat.getLatitude()));
        this.lastUTMX = utmX;
        this.lastUTMy = utmY;
    }

    public void addToHistory(Coordinate longlat, double utmX, double utmY) {
        this.positions.add(longlat);
        this.lastUTMX = utmX;
        this.lastUTMy = utmY;
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

    public LinkedList<Coordinate> getPositionTrace() {
        return positions;
    }

    @Override
    public boolean tracing() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }

    public double getLastUTMX() {
        return lastUTMX;
    }

    public double getLastUTMy() {
        return lastUTMy;
    }

    @Override
    public void resetMovementLengths() {
        super.resetMovementLengths();
        history.clear();
        positions.clear();
        lastUTMX=-1;
        lastUTMy=-1;
    }
    
    

}
