package model.surface;

import model.timeline.array.ArrayTimeLineMeasurement;
import model.topology.Capacity;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.Position3D;
import model.topology.profile.RectangularProfile;

/**
 *
 * @author saemann
 */
public class SurfaceTriangle extends Capacity {

//    public Inlet inlet;
    public Manhole manhole;

    protected static RectangularProfile fakeprofile = new RectangularProfile(1, 1);

    Position3D position;
    
//    public TriangleMeasurement measurement;

//    /**
//     * Every particle increases the counter. the index is defined by the
//     * material.
//     */
//    public int pariclecount[];

    public SurfaceTriangle(long id) {

        super(fakeprofile);
        this.setManualID(id);
    }

    public void setPosition(Position3D position) {
        this.position = position;
    }

//    public void setInlet(Inlet inlet) {
//        this.inlet = inlet;
//    }

    public void setManhole(Manhole manhole) {
        this.manhole = manhole;
    }

    public long getTriangleID() {
        return manual_ID;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (int) (this.autoID ^ (this.autoID >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SurfaceTriangle other = (SurfaceTriangle) obj;
        if (this.autoID != other.autoID) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + manual_ID + " " + (manhole == null ? "" : "Manhole(" + manhole.getManualID() + ")")/* + (inlet == null ? "" : "Inlet(" + inlet.getNetworkCapacity() + ")")*/;
    }

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
        return position;
    }

    @Override
    public double getWaterlevel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    /**
//     * @deprecated 
//     * @return 
//     */
//    private Inlet getInlet() {
//        return inlet;
//    }

    public Manhole getManhole() {
        return manhole;
    }
    
    

}
