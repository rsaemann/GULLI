package model.topology;

import model.timeline.array.ArrayTimeLineMeasurement;
import model.topology.profile.Medium;
import model.topology.profile.Profile;

/**
 *
 * @author saemann
 */
public abstract class Capacity {

    private static long runningID;
    /**
     * This ID is incremented every instantiation and is therefore unique.
     */
    protected final long autoID = runningID++;

    /**
     * This id is set by io classes to correspond to database ids like HExtran.
     */
    protected long manual_ID;

    protected final Profile profile;

    protected Medium medium = Medium.WATER10;

//    protected double waterlevel = 0;
//    public  Tags tags;

    /**
     * Can manaully set to sign this Capacity as an end-building
     */
    protected boolean setAsOutlet = false;

    public enum SEWER_TYPE {

        DRAIN, SEWER, DRINKABLE, MIX, UNKNOWN
    };

    protected SEWER_TYPE waterType = SEWER_TYPE.UNKNOWN;

    public Capacity(Profile profile) {
        this.profile = profile;
    }

    public Capacity(Profile profile, SEWER_TYPE type) {
        this(profile);
        this.waterType = type;
    }

    public void setManualID(long he_id) {
        this.manual_ID = he_id;
    }

    /**
     * Returns the number id of this object, if the dataprovider (e.g. HYSTEM
     * EXTRAN gave such an id). This might be 0 if it was not read from the
     * loader.
     *
     * @return
     */
    public long getManualID() {
        return manual_ID;
    }

    public abstract Connection[] getConnections();

    public abstract double getCapacityVolume();

    public abstract double getFluidVolume();

//    public abstract void setStatusTimeLine(TimeLine<? extends SimpleStorageStamp> tl);
    public abstract void setMeasurementTimeLine(ArrayTimeLineMeasurement tl);

    /**
     *
     * @return
     */
//    public abstract TimeLine<? extends SimpleStorageStamp> getStatusTimeLine();
//    
    public abstract ArrayTimeLineMeasurement getMeasurementTimeLine();

    /**
     * [m] Water height from sole of this capacity
     *
     * @return
     */
    public abstract double getWaterHeight();

    public Profile getProfile() {
        return profile;
    }

    public SEWER_TYPE getWaterType() {
        return waterType;
    }

//    public Medium getMedium() {
//        return medium;
//    }
    /**
     * Returns the unique id of this object. Every creation of a capacity object
     * increases the static id (autoincrement). Each Capacity gets its own
     * unique id.
     *
     * @return unique capacity id for this object.
     */
    public long getAutoID() {
        return autoID;
    }

    public static long getMaximumID() {
        return runningID;
    }

    @Override
    public int hashCode() {
//        return (int) autoID;
        int hash = 3;
        hash = 89 * hash + (int) (this.autoID ^ (this.autoID >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

//        if (obj.getClass().equals(Integer.class)) {
//            return ((int) obj == this.autoID);
//        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return ((Capacity) obj).autoID == this.autoID;
    }

    public abstract Position3D getPosition3D(double meter);

    /**
     * Returns true if this Capacity has been set as an outlet (leaving network)
     * building by the importer-class. No auto-detection.
     *
     * @return
     */
    public boolean isSetAsOutlet() {
        return setAsOutlet;
    }

    public void setAsOutlet(boolean isOutlet) {
        this.setAsOutlet = isOutlet;
    }

//    public void setWaterlevel(double waterlevel) {
//        this.waterlevel = waterlevel;
//    }
    public abstract double getWaterlevel();

}
