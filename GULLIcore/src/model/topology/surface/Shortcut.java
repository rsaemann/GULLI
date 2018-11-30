package model.topology.surface;

import model.topology.Capacity;

/**
 * Describes a shortcut between two manholes via the surface.
 * 
 * @author saemann
 */
public class Shortcut {
    protected Capacity startCapacity, targetCapacity;
    protected long traveltime;

    public Shortcut(Capacity startCapacity, Capacity targetCapacity, long traveltime) {
        this.startCapacity = startCapacity;
        this.targetCapacity = targetCapacity;
        this.traveltime = traveltime;
    }

    public Capacity getStartCapacity() {
        return startCapacity;
    }

    public Capacity getTargetCapacity() {
        return targetCapacity;
    }

    public long getTraveltime() {
        return traveltime;
    }
    
    
    
   
}
