package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.timeline.MeasurementTimeline;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.profile.Medium;
import com.saemann.gulli.core.model.topology.profile.Profile;

/**
 * Pipe is a 2D capacities, that connect two storages (e.g. Manholes)
 *
 * @author saemann
 */
public class Pipe extends Capacity {

    /**
     * upper bound suction connection, Einfluss am oberen Ende
     */
    protected final Connection_Manhole_Pipe startConnection;

    /**
     * lower Bound outlet Connection_Manhole_Pipe, Auslass am unteren Rohrende
     */
    protected final Connection_Manhole_Pipe endConnection;

    /**
     * [m]
     */
    protected float length;

    /**
     * Decline from upper to lower bound relative to pipelength (negative
     * value). Gefälle der Leitung von oberer zur unteren Öffnung relativ zur
     * Rohrlänge (normalerweise ein negativer Wert)
     */
    protected float decline;

    /**
     * A definition String
     */
    protected String name;

    /**
     * Roughness k [m]
     *
     * absoluter Rauheitsbeiwert k [m] (wird in Tabellenwerk in mm angegeben)
     */
    protected float roughness_k = 0.001f;

    /**
     * Roughness ks [m^(1/3) / s]
     *
     * absoluter Rauheitsbeiwert kst (wird in Tabellenwerk in [m^(1/3) / s]
     * angegeben);
     */
    protected float roughness_kst = 90;

    private boolean negativeflowDirection;

    /**
     * true if |decline| is less than 0.00001
     */
    private boolean isHorizontal = false;

    private TimeLinePipe timelineStatus;
    private MeasurementTimeline timelineMeasurement;

    public Pipe(Connection_Manhole_Pipe inletConnection, Connection_Manhole_Pipe outletConnection, Profile profile) {
        super(profile);
        this.startConnection = inletConnection;
        if (inletConnection != null) {
            inletConnection.setPipe(this);
            inletConnection.setIsStartOfPipe(true);
        }
        this.endConnection = outletConnection;
        if (outletConnection != null) {
            outletConnection.setPipe(this);
            outletConnection.setIsStartOfPipe(false);
        }
        if (inletConnection != null && outletConnection != null) {
            if (Math.abs(inletConnection.getHeight() - outletConnection.getHeight()) < 0.0001) {
                isHorizontal = true;
            }
        }
    }

    /**
     * Returns the Connection at the pipes local x=0 coordinate. This will not
     * change over time.
     *
     * @return
     */
    public Connection_Manhole_Pipe getStartConnection() {
        return startConnection;
    }

    /**
     * Returns the static Connection at the pipes end. This will not change over
     * time.
     *
     * @return
     */
    public Connection_Manhole_Pipe getEndConnection() {
        return endConnection;
    }

    /**
     * Returns the actual velocity/flow depending Inlet connection. e.g. the
     * pipeend-connection if the velocity is negative
     *
     * @return
     */
    public Connection_Manhole_Pipe getFlowInletConnection() {
        if (negativeflowDirection) {
            return endConnection;
        }
        return startConnection;
    }

    /**
     * Returns the actual velocity/flow depending Outlet connection. e.g. the
     * start-connection if the velocity is negative
     *
     * @return
     */
    public Connection_Manhole_Pipe getFlowOutletConnection() {
        if (negativeflowDirection) {
            return startConnection;
        }
        return endConnection;
    }

    @Override
    public Connection_Manhole_Pipe[] getConnections() {
        return new Connection_Manhole_Pipe[]{startConnection, endConnection};
    }

    public float getDecline() {
        return decline;
    }

    @Override
    public double getCapacityVolume() {
        return this.profile.getTotalArea() * length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLength(float length) {
        this.length = length;
        this.decline = (endConnection.getHeight() - startConnection.getHeight()) / length;
        if (Math.abs(decline) < 0.00001) {
            isHorizontal = true;
        }
    }

    public float getLength() {
        return length;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getFluidVolume() {
        return this.timelineStatus.getVolume();
    }

    @Override
    public double getWaterHeight() {
        return 0.5 * getFlowInletConnection().getHeight() + 0.5 * getFlowOutletConnection().getHeight() + this.getWaterlevel();
    }

    /**
     * absolute roughness in [m]
     *
     * @return
     */
    public double getRoughness_k() {
        return roughness_k;
    }

    /**
     * absolute rougness in [m] (not [mm]!)
     *
     * @param roughness_k
     */
    public void setRoughness_k(double roughness_k) {
        this.roughness_k = (float) roughness_k;
        this.roughness_kst = (float) getRoughnessKst(roughness_k);
    }

    /**
     * Set Strickler's coefficient in m^(1/3) / s
     *
     * @param kst
     */
    public void setRoughnessKst(double kst) {
        this.roughness_kst = (float) kst;
    }

    public Position3D getPositionAlongAxisAbsolute(double meter) {
        if (meter < -1) {
            throw new IllegalArgumentException("Position in Pipe is to low :" + meter);
        }
        if (meter < 0) {
            return startConnection.getPosition();
        }

//        if (meter > this.length + 1) {
//            throw new IllegalArgumentException("Position in pipe [" + this.getName() + "] is too far :" + meter + "m but pipe is only " + this.length + "m long.");
//        }
        if (meter > this.length) {
            return endConnection.getPosition();
        }

        return this.startConnection.getPosition().getInterpolatedPosition(meter / this.length, endConnection.getPosition());
    }

    public Position getPositionAlongAxisRelative(double ratio) {
        if (ratio < 0) {
            throw new IllegalArgumentException("relative position in pipe is to low :" + ratio);
        }

        if (ratio > 1) {
            throw new IllegalArgumentException("relative position in pipe [" + this.getName() + "] is too big :" + ratio);
        }

        return this.startConnection.getPosition().getInterpolatedPosition(ratio, endConnection.getPosition());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(ID" + autoID + "/HE" + manual_ID + ")['" + this.name + "']";
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return getPositionAlongAxisAbsolute(meter);
    }

    /**
     * Is the pipe decline nearly zero?
     *
     * @return
     */
    public boolean isHorizontal() {
        return isHorizontal;
    }

    public void setWaterType(SEWER_TYPE waterType) {
        this.waterType = waterType;
    }

    public void setMedium(Medium medium) {
        this.medium = medium;
    }

    public void setFlowdirectionNegative(boolean negativFlow) {
        if (negativFlow = negativeflowDirection) {
            return;
        }

        this.negativeflowDirection = negativFlow;
    }

    public boolean isNegativeflowDirection() {
        return negativeflowDirection;
    }

    /**
     * Average velocity_m2p [m/s]
     *
     * @param water_level_in_pipe
     * @return
     */
    public double averageVelocity_gravity(double water_level_in_pipe) {
        if (water_level_in_pipe < 0.0001) {
            return 0;
        }
        double rh = profile.getHydraulicRadius(water_level_in_pipe);
        double lambdainv = (4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
        double velocity = Math.sqrt(8 * 9.81 * rh * Math.abs(decline) * lambdainv);
        if (Double.isNaN(velocity)) {
            if (Double.isNaN(decline)) {
                System.out.println("Decline is NaN: length=" + length);
            }
            System.out.println("Pipe::AVERAGE_hydraulicRadius velocity in pipe " + this.toString() + " is NaN , decline=" + decline + ", rh(h=" + water_level_in_pipe + ")= " + rh + " sqrt(" + (8 * 9.81 * rh * Math.abs(decline) * lambdainv));
            System.out.println("k=" + roughness_k + "   1/lambda=" + lambdainv + " log10(" + (roughness_k / (3.71 * rh)) + ")=" + (Math.log10(roughness_k / (3.71 * profile.getHydraulicRadius(water_level_in_pipe)))));
            System.out.println("log10(X)^2=" + Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
            System.out.println("4xlog10(x)^2=" + 4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2) + " = " + lambdainv);
            try {
                throw new Exception("Kaputt");
            } catch (Exception ex) {
                Logger.getLogger(Pipe.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }
        return velocity;
    }

    /**
     * velocity_m2p calculated from delta-waterlevel, without gravity
     *
     * @param dh difference between inlet and outlet water level
     * @param water_level_in_pipe [m]
     * @return
     */
    public double averageVelocity_horizontal(double dh, double water_level_in_pipe) {
        if (water_level_in_pipe < 0.0001) {
            return 0;
        }
        double rh = profile.getHydraulicRadius(water_level_in_pipe);
        double lambdainv = (4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
        double velocity = Math.sqrt(8 * 9.81 * rh * Math.abs(dh) * lambdainv);
        if (Double.isNaN(velocity)) {
            if (Double.isNaN(dh)) {
                System.out.println("grad(h) is NaN: length=" + length);
            }

            System.out.println("Pipe::AVERAGE_horizontal gradH velocity in pipe " + this.toString() + " is NaN ,dh=" + dh + " decline=" + decline + ", rh(h=" + water_level_in_pipe + ")= " + rh + " sqrt(" + (8 * 9.81 * rh * Math.abs(decline) * lambdainv));
            System.out.println("k=" + roughness_k + "   1/lambda=" + lambdainv + " log10(" + (roughness_k / (3.71 * rh)) + ")=" + (Math.log10(roughness_k / (3.71 * profile.getHydraulicRadius(water_level_in_pipe)))));
            System.out.println("log10(X)^2=" + Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
            System.out.println("4xlog10(x)^2=" + 4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2) + " = " + lambdainv);

        }
        return velocity;
    }

    /**
     * m/s at the actual timestep of the simulation.
     *
     * @return
     */
    public float averageVelocity_actual() {
        if (timelineStatus != null) {
            return timelineStatus.getVelocity();
        }
        return 0;
    }

    /**
     * Get the Strickler's coefficient from a rougness k [m]! for smooth walls.
     *
     * @param rougnessK in [m!]
     * @return k_st [m^(1/3) /s]
     */
    public double getRoughnessKst(double rougnessK) {
        return 26. / (Math.pow(rougnessK, 1. / 6.));

    }

    /**
     *
     * @return
     */
    public TimeLinePipe getStatusTimeLine() {
        return this.timelineStatus;
    }

    public void setStatusTimeLine(TimeLinePipe tl) {
        this.timelineStatus = tl;
    }

    public double getVelocity() {

        return averageVelocity_actual();
    }

    public double getReynoldsNumber_Actual() {
        return Math.abs(getVelocity() * profile.getHydraulicDiameter(getWaterlevel()) / medium.kin_viscosity);
    }

    public double getFillRate() {
        if (this.timelineStatus == null) {
            return -0;
        }
        return this.profile.getFillRate(getWaterlevel());
    }

    @Override
    public void setMeasurementTimeLine(MeasurementTimeline tl) {
        this.timelineMeasurement = tl;
    }

    @Override
    public MeasurementTimeline getMeasurementTimeLine() {
        return this.timelineMeasurement;
    }

    @Override
    public double getWaterlevel() {
        if (this.timelineStatus == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return this.timelineStatus.getWaterlevel();
    }

    /**
     * Flow for the actual time.
     *
     * @return q [m³/s]
     */
    public double getFlowActual() {
        if (this.timelineStatus == null) {
            return 0;
        }
        return this.timelineStatus.getDischarge();
    }

    public ArrayList<Position3D> getGeometry() {
        ArrayList<Position3D> list = new ArrayList<>(2);
        list.add(startConnection.getPosition());
        list.add(endConnection.getPosition());
        return list;
    }
}
