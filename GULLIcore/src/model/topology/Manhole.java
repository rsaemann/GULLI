package model.topology;

import model.surface.SurfaceTriangle;
import model.topology.profile.Profile;

/**
 *
 * @author saemann
 */
public class Manhole extends StorageVolume {

    protected Position position;

    protected String name;

    protected float surface_height;

    protected boolean pressure_save_cover;

    protected int surfaceTriangleID = -1;

    protected Connection_Manhole_Surface topConnection;

    public Manhole(Position position, String name, Profile profile) {
        super(profile);
        this.position = position;
        this.name = name;
    }

    public void setTopConnection(Connection_Manhole_Surface topConnection) {
        this.topConnection = topConnection;
    }

    public Connection_Manhole_Surface getTopConnection() {
        return topConnection;
    }

    public Position getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public float getSurface_height() {
        return surface_height;
    }

    public boolean isPressure_save_cover() {
        return pressure_save_cover;
    }

    public String getStreet_name() {
        return "";
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurface_height(float surface_height) {
        this.surface_height = surface_height;
    }

    public void setPressure_save_cover(boolean pressure_save_cover) {
        this.pressure_save_cover = pressure_save_cover;
    }

    public void setStreet_name(String street_name) {
//        this.street_name = street_name;
    }

    public void setSole_height(float sole_height) {
        this.sole_height = sole_height;
    }

    public void setTop_height(float top_height) {
        this.top_height = top_height;
    }

    public void setWaterType(SEWER_TYPE waterType) {
        this.waterType = waterType;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(aiID:" + this.getAutoID() + "/ mID:" + this.getManualID() + ")['" + this.getName() + "']";
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return new Position3D(this.position);
    }

    public int getSurfaceTriangleID() {
        return surfaceTriangleID;
    }

    public void setSurfaceTriangle(int surfaceTriangleID) {
        this.surfaceTriangleID = surfaceTriangleID;
    }

}
