package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.topology.profile.Profile;

/**
 *
 * @author saemann
 */
public class Manhole extends StorageVolume {

    protected Position3D position;

//    protected String name;
    protected float surface_height;

    protected boolean pressure_save_cover;

    protected int surfaceTriangleID = -1;

    protected Connection_ToSurface topConnection;

    public Manhole(Position position, String name, Profile profile) {
        super(profile);
        if (!(position instanceof Position3D)) {
            this.position = new Position3D(position);
        } else {
            this.position = (Position3D) position;
        }
        this.name = name;
    }

    public void setTopConnection(Connection_ToSurface topConnection) {
        this.topConnection = topConnection;
    }

    public Connection_ToSurface getTopConnection() {
        return topConnection;
    }

    public Position getPosition() {
        return position;
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
        if (position instanceof Position3D) {
            this.position = (Position3D) position;
        } else {
            this.position = new Position3D(position);
        }
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
        return this.position;
    }

    public int getSurfaceTriangleID() {
        return surfaceTriangleID;
    }

    public void setSurfaceTriangle(int surfaceTriangleID) {
        this.surfaceTriangleID = surfaceTriangleID;
    }

}
