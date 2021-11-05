/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.control.StartParameters;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author saemann
 */
public class Network {

    public static CoordinateReferenceSystem crsUTM;

    public static CoordinateReferenceSystem crsWGS84 = initWGS84CRS();

    protected Collection<Pipe> pipes;
    protected HashSet<Manhole> manholes;
    protected HashMap<Long,Manhole> manholeMap;
    protected Collection<Inlet> streetInlets;
    protected String name;

    /**
     * m^2 of runoff-relevant surface
     */
    protected double inflowArea = -1;

    public Network(Collection<Pipe> pipes, Collection<Manhole> manholes) {
        this.pipes = (pipes);
        this.manholes = new HashSet<>(manholes);
        fillManholeMap();
    }

    public Network(HashSet<Pipe> pipes, HashSet<Manhole> manholes) {
        this.pipes = pipes;
        this.manholes = manholes;
        fillManholeMap();
    }
    
    protected void fillManholeMap(){
        if(this.manholeMap==null){
            manholeMap=new HashMap<>(manholes.size());
        }
        manholeMap.clear();
        for (Manhole manhole : manholes) {
            manholeMap.put(manhole.getManualID(), manhole);
        }
    }

    public Collection<Pipe> getPipes() {
        return pipes;
    }

    public void setPipes(HashSet<Pipe> pipes) {
        this.pipes = pipes;
    }

    public HashSet<Manhole> getManholes() {
        return manholes;
    }

    public void setCapacities(HashSet<Manhole> capacities) {
        this.manholes = capacities;
        fillManholeMap();
    }

    public void setStreetInlets(Collection<Inlet> streetInlets) {
        this.streetInlets = streetInlets;
    }

    public Collection<Inlet> getStreetInlets() {
        return streetInlets;
    }

    public Capacity getCapacity(int id) {
        for (Manhole capacity : manholes) {
            if (capacity.getAutoID() == id) {
                return capacity;
            }
        }
        return null;
    }

    public Pipe getPipeNearPositionLAtLon(double latitude, double longitude) {
        double bestdistance = Double.MAX_VALUE;
        Pipe bestPipe = null;

        for (Pipe pipe : pipes) {
            Position start = pipe.getStartConnection().position;
            Position end = pipe.getEndConnection().position;

            double tempdist = (start.getLatitude() - latitude) * (start.getLatitude() - latitude) + (start.getLongitude() - longitude) * (start.getLongitude() - longitude);
            if (tempdist > bestdistance) {
                continue;
            }
            tempdist += (end.getLatitude() - latitude) * (end.getLatitude() - latitude) + (end.getLongitude() - longitude) * (end.getLongitude() - longitude);
            if (tempdist < bestdistance) {
                bestdistance = tempdist;
                bestPipe = pipe;
            }
        }
        return bestPipe;
    }

    public Manhole getManholeNearPositionLatLon(GeoPosition2D position) {
        return getManholeNearPositionLatLon(position.getLatitude(), position.getLongitude());
    }

    public Manhole getManholeNearPositionLatLon(double latitude, double longitude) {
        double bestdistance = Double.MAX_VALUE;
        Manhole bestPipe = null;

        for (Manhole m : manholes) {
            Position start = m.getPosition();
            double tempdist = (start.getLatitude() - latitude) * (start.getLatitude() - latitude) + (start.getLongitude() - longitude) * (start.getLongitude() - longitude);
            if (tempdist < bestdistance) {
                bestdistance = tempdist;
                bestPipe = m;
            }
        }
        return bestPipe;
    }

    public Manhole getManholeByName(String name) throws NullPointerException {
        for (Manhole capacity : manholes) {
            if (capacity.getName().equals(name)) {
                return capacity;
            }
        }
        return null;
    }

    /**
     * Returns a Manhole with the ID given by the original model (e.g. HE-ID)
     *
     * @param manualID
     * @return
     * @throws NullPointerException
     */
    public Manhole getManholeByManualID(long manualID) throws NullPointerException {
        if(manholeMap!=null){
            return manholeMap.get(manualID);
        }
        for (Manhole capacity : manholes) {
            if (capacity.getManualID() == manualID) {
                return capacity;
            }
        }
        return null;
    }

    public Pipe getPipeByName(String name) throws NullPointerException {
        for (Pipe capacity : pipes) {
            if (capacity.getName().equals(name)) {
                return capacity;
            }
        }
        return null;
    }

    public Capacity getCapacityByName(String name) {
        for (Pipe capacity : pipes) {
            if (capacity.getName().equals(name)) {
                return capacity;
            }
        }
        for (Manhole capacity : manholes) {
            if (capacity.getName().equals(name)) {
                return capacity;
            }
        }
        return null;
    }

    public Pipe getPipeByID(long id) throws NullPointerException {
        for (Pipe capacity : pipes) {
            if (capacity.getAutoID() == id) {
                return capacity;
            }
        }
        return null;
    }
    
     public Pipe getPipeByManualID(long id) throws NullPointerException {
        for (Pipe capacity : pipes) {
            if (capacity.getManualID() == id) {
                return capacity;
            }
        }
        return null;
    }

    public HashSet<Manhole> getLeaves() {
        HashSet<Manhole> set = new HashSet<>(20);
        for (Manhole mh : manholes) {
            if (!mh.hasIncomings()) {
                set.add(mh);
            }
        }

        return set;
    }

    /**
     * Returns all Manholes that have no outgoing pipe and therefore are outles
     * off the network. Takes only the orientation of pipes into account. Does
     * NOT consider negative flow directions.
     *
     * @param sewer
     * @param drain
     * @return manholes without outgoing pipes
     */
    public HashSet<Manhole> getRoots() {
        HashSet<Manhole> set = new HashSet<>(20);

        for (Manhole mh : manholes) {
            if (!mh.hasOutgoings()) {
                set.add(mh);
            }
        }

        return set;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static CoordinateReferenceSystem initWGS84CRS() {
        try {
            CRSAuthorityFactory af = CRS.getAuthorityFactory(StartParameters.JTS_WGS84_LONGITUDE_FIRST);
            return af.createCoordinateReferenceSystem("EPSG:4326");
        } catch (FactoryException ex) {
            Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * m^2 of runoff-relevant surface, that is used for the hydrologic part
     * (inflow at manholes from external run-off model) e.g. HYSTEM , if -1 =
     * not initialized.
     *
     * @return
     */
    public double getInflowArea() {
        return inflowArea;
    }

    /**
     * m^2 of runoff-relevant surface, that is used for the hydrologic part
     * (inflow at manholes from external run-off model) e.g. HYSTEM
     *
     * @param inflowArea
     */
    public void setInflowArea(double inflowArea) {
        this.inflowArea = inflowArea;
    }

}
