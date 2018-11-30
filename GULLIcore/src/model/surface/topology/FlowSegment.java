/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface.topology;

import model.surface.LocalMinimumPoint;

/**
 *
 * @author saemann
 */
public class FlowSegment {

    LocalMinimumPoint start, end;
    double length;
    double slope;
    double sealing;

    public FlowSegment(LocalMinimumPoint start, LocalMinimumPoint end, double length, double slope, double sealing) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.slope = slope;
        this.sealing = sealing;
    }

    public FlowSegment(LocalMinimumPoint start, LocalMinimumPoint end) {
        this.start = start;
        this.end = end;
        this.length = start.coordUTM.distance(end.coordUTM);
        double dz =   start.minZ-end.minZ;
        this.slope = dz / length;
    }

    public FlowSegment(LocalMinimumPoint start, LocalMinimumPoint end, double sealing) {
        this(start, end);
        this.sealing = sealing;
    }

    public LocalMinimumPoint getStart() {
        return start;
    }

    public LocalMinimumPoint getEnd() {
        return end;
    }
    
     

}
