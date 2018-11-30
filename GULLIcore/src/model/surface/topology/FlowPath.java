/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface.topology;

import java.util.List;
import model.surface.LocalMinimumPoint;

/**
 *
 * @author saemann
 */
public class FlowPath {

    public final double activatingWaterheightAtStart;
    private final LocalMinimumPoint start;
    private final LocalMinimumPoint target;
    private final List<FlowSegment> segments;
    private double length;

    public FlowPath(LocalMinimumPoint start, LocalMinimumPoint target, List<FlowSegment> segments, double activatingWaterheight_uNN) {
        this.start = start;
        this.target = target;
        this.segments = segments;
        this.activatingWaterheightAtStart=activatingWaterheight_uNN;
        length = 0;
        for (FlowSegment segment : segments) {
            length += segment.length;
        }
    }

    public LocalMinimumPoint getStart() {
        return start;
    }

    public LocalMinimumPoint getTarget() {
        return target;
    }

    public List<FlowSegment> getSegments() {
        return segments;
    }

    public double getLength() {
        return length;
    }

}
