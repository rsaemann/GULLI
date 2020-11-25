/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.profile.Profile;

/**
 *
 * @author saemann
 */
public class StorageVolume extends Capacity {

    /**
     * all connections from manhole AND outgoing pipes and cover Alle
     * Verbindungen sowohl der ein- und ausgehenden Rohre und der Schachtdeckel
     */
    protected Connection_Manhole_Pipe[] connections;

    /**
     * sole above sea level
     *
     * Höhe der Sole über Normalnull
     */
    protected float sole_height;

    protected float top_height;

    /**
     * [m] water height above sea level [üNN]
     */
//    protected float water_height;

    protected int numberOutgoings, numberIncomings;
    
    protected TimeLineManhole timelineStatus;
    
    protected String name;

    public StorageVolume(Profile profile) {
        super(profile);
        this.connections = new Connection_Manhole_Pipe[0];
    }

    /**
     * Adds a Connection_Manhole_Pipe to this Capacity's list if not already
     * contained.
     *
     * @param con
     * @return true if adding was succesfull, false if already contained.
     */
    public boolean addConnection(Connection_Manhole_Pipe con) {
//        System.out.println(this.getClass()+":addConnection isStart?"+con.isStartOfPipe);
        int position = connections.length;
        boolean found = false;
        con.setManhole(this);
        for (int i = 0; i < connections.length; i++) {
            if (connections[i].equals(con)) {
                //Do not add if this connection is already attached.
                return false;
            }
            if (!found && connections[i].getHeight() > con.getHeight()) {
                position = i;
                found = true;
            }
        }
        Connection_Manhole_Pipe[] newConnections = new Connection_Manhole_Pipe[connections.length + 1];
        for (int i = 0; i < position; i++) {
            newConnections[i] = connections[i];
        }
        newConnections[position] = con;
        for (int i = position + 1; i < newConnections.length; i++) {
            newConnections[i] = connections[i - 1];
        }
        connections = newConnections;
        
        if (con.isStartOfPipe()) {
            numberOutgoings++;
        }else {
            numberIncomings++;
        }
        return true;
    }

    @Override
    public Connection_Manhole_Pipe[] getConnections() {
        return connections;
    }

    @Override
    public double getCapacityVolume() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * returns Water level above sea level
     * Wasserhöhe üNN.
     * @return
     */
    @Override
    public double getWaterHeight() {
        return timelineStatus.getActualWaterZ();
    }
    
//    /**
//     * Waterlevel from sole of this volume.
//     * Wasserstand ab Grundsohle.
//     * @return 
//     */
//    public double getWaterLevel(){
//        return timelineStatus.getWaterZ(ArrayTimeLineManhole.getActualTimeIndex())-this.sole_height;
//    }

    @Override
    public double getFluidVolume() {
        return profile.getTotalArea() * (getWaterlevel());
    }

    public float getSole_height() {
        return sole_height;
    }

    public float getTop_height() {
        return top_height;
    }

    @Override
    public Position3D getPosition3D(double meter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean hasOutgoings() {
        return numberOutgoings > 0;
    }

    public boolean hasIncomings() {
        return numberIncomings > 0;
    }

    public int getNumberIncomings() {
        return numberIncomings;
    }

    public int getNumberOutgoings() {
        return numberOutgoings;
    }


    public String getName() {
        return name;
    }
    
    


    @Override
    public void setMeasurementTimeLine(ArrayTimeLineMeasurement tl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayTimeLineMeasurement getMeasurementTimeLine() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public TimeLineManhole getStatusTimeLine() {
        return this.timelineStatus;
    }

    public void setStatusTimeline(TimeLineManhole statusTimeline) {
        this.timelineStatus = statusTimeline;
    }

    @Override
    public double getWaterlevel() {
        return timelineStatus.getActualWaterLevel();//timelineStatus.getActualWaterZ()-this.getSole_height();
    }
    
    

}
