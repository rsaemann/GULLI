package model.surface;

import model.timeline.array.ArrayTimeLineMeasurement;
import model.topology.Capacity;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Position3D;
import model.topology.profile.RectangularProfile;

/**
 *
 * @author saemann
 */
public class SurfaceTrianglePath extends Capacity{

    long startTriangleID,targetTriangleID;
    
    public final float distance;
    
    public int neighbourIndex;
    
    public Surface surface;
    
    public Position3D start,end;
//    float area, length;
//
//    float x, y, z;
//    float maxWaterlvl;
//    int danger;
//    float[] waterlvl;
    
//    public Geometry geomWGS84;

    private SurfaceTrianglePath(long startID, long targetId, float distance) {
        super(new RectangularProfile(1, 1));
        this.startTriangleID = startID;
        this.targetTriangleID= targetId;
        this.distance=distance;
    }

    public SurfaceTrianglePath(long startTriangleID, long targetTriangleID, float distance, Surface surface) {
       this(startTriangleID, targetTriangleID, distance);
        this.surface = surface;
    }
    
    

    public long getStartTriangleID() {
        return startTriangleID;
    }

    public long getTargetTriangleID() {
        return targetTriangleID;
    }

//    public SurfaceTrianglePath(int objectID, int startTriangleID,  float area, float length, float x, float y, float z, float maxWaterlvl, int danger, float[] waterlvl) {
//        this.objectID = objectID;
//        this.startTriangleID = startTriangleID;
//        this.area = area;
//        this.length = length;
//        this.x = x;
//        this.y = y;
//        this.z = z;
//        this.maxWaterlvl = maxWaterlvl;
//        this.danger = danger;
//        this.waterlvl = waterlvl;
//    }
    public float getDistance() {
        return distance;
    }

//    public long getObjectID() {
//        return objectID;
//    }
//    public float getArea() {
//        return area;
//    }
//
//    public float getLength() {
//        return length;
//    }
//
//    public float getX() {
//        return x;
//    }
//
//    public float getY() {
//        return y;
//    }
//
//    public float getZ() {
//        return z;
//    }
//
//    public float getMaxWaterlvl() {
//        return maxWaterlvl;
//    }
//
//    public int getDanger() {
//        return danger;
//    }
//
//    public float[] getWaterlvl() {
//        return waterlvl;
//    }
//
//    /**
//     * Returns an interpolated waterlevel
//     * @param fraction between 0...arraylength-1
//     * @return 
//     */
//    public float interpolateWaterLvl(double fraction) {
//        if (waterlvl == null) {
//            throw new NullPointerException("No array of waterlevel for SurfaceTrianglePath ID:" + startTriangleID);
//        }
//        if (fraction < 0) {
//            return -1;
//        }
//        if ((int) fraction >= waterlvl.length) {
//            return -1;
//        }
//        int floor = (int) fraction;
//        double relative = fraction % 1;
//        float interpoliert = (float) (waterlvl[floor] + relative * (waterlvl[floor + 1] - waterlvl[floor]));
//        return interpoliert;
//    }
    @Override
    public Connection_Manhole_Pipe[] getConnections() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getCapacityVolume() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getFluidVolume() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMeasurementTimeLine(ArrayTimeLineMeasurement tl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayTimeLineMeasurement getMeasurementTimeLine() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getWaterHeight() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return start;
    }

    @Override
    public double getWaterlevel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public double getVelocity(long time){
        return surface.getVelocityToNeighbour(time, (int) startTriangleID, neighbourIndex);
    }
    
    public double getActualVelocity(){
        return surface.getVelocityToNeighbour((int) startTriangleID, neighbourIndex);
    }

}
