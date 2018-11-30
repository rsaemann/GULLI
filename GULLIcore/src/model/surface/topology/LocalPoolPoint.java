package model.surface.topology;

import java.util.ArrayList;
import model.surface.LocalMinimumPoint;
import model.topology.Inlet;

/**
 *
 * @author saemann
 */
public class LocalPoolPoint extends LocalMinimumPoint {

    protected final ArrayList<FlowPath> flowPathsIncoming = new ArrayList<>(0);
    protected final ArrayList<FlowPath> flowPathsOutgoing = new ArrayList<>(0);
    public boolean isSurfaceDeepest,isFlowDeepest;
    public Inlet inlet=null;
    
    
    public LocalPoolPoint(int id, int indexX, int indexY) {
        super(id, indexX, indexY);
    }

    public LocalPoolPoint(LocalMinimumPoint point) {
        this(point.id, point.indexX, point.indexY);
        this.coordUTM = point.coordUTM;
        this.hoehere = point.hoehere;
//        this.hoehereFlood = point.hoehereFlood;
        this.lowerPoint = point.lowerPoint;
        this.lowerPointFlood = point.lowerPointFlood;
        this.minZ = point.minZ;
        this.surfaceArea = point.surfaceArea;
    }

    public void addOutgoingFlowPath(FlowPath path){
        this.flowPathsOutgoing.add(path); 
    }

    public ArrayList<FlowPath> getFlowPathsOutgoing() {
        return flowPathsOutgoing;
    }

    public ArrayList<FlowPath> getFlowPathsIncoming() {
        return flowPathsIncoming;
    }
    
    

}
