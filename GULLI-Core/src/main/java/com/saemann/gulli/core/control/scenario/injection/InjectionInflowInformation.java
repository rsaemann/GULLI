/*
 * The MIT License
 *
 * Copyright 2021 Saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saemann.gulli.core.control.scenario.injection;

import com.saemann.gulli.core.io.extran.HE_Database;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelineManhole;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Relative concentration of diffusive pollutant per area (kg/m^2) information
 * will be calculated to total mass for injection
 *
 * @author saemann
 */
public class InjectionInflowInformation implements InjectionInfo {

    protected Material material;

    protected int materialID = -1;

    protected int id = -1;

    /**
     * kg
     */
    protected double totalMass;

    /**
     * m^3
     */
    protected double totalvolume;

    /**
     * m^2
     */
    protected double totalArea;
    /**
     * kg/m^3
     */
    protected double concentration;

    /**
     * kg/m^2
     */
    protected double load;

    protected Network network;

    protected Manhole[] manholes;

    /**
     * inflow volume {m^3] per manhole
     */
    protected float[] volume;

    protected int numberOfParticles;

    protected boolean active = true;

    protected boolean changed = true;

    /**
     * seconds after simulation start
     */
    protected double startSeconds = 0;

    /**
     * duration in seconds
     */
    protected double durationSeconds = 0;

    /**
     *
     * @param material
     * @param network
     * @param concentration kg/m^3
     * @param numberOfParticles
     */
    public InjectionInflowInformation(Material material, Network network, double concentration, int numberOfParticles) {
        this.material = material;
        this.concentration = concentration;
//        this.concentration = concentration;
        this.network = network;

        this.numberOfParticles = numberOfParticles;
        findAndLoadManholes();
    }

    private void findAndLoadManholes() {
        if (network == null) {
            manholes = null;
            return;
        }
        manholes = network.getManholes().toArray(new Manhole[network.getManholes().size()]);
        volume = new float[manholes.length];
        totalvolume = 0;
        try {
            totalArea = network.getInflowArea();
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean needCalculation = true;
        //Search for a special data provider, that can speed up the process of laoding large files.

        if (manholes[0].getStatusTimeLine() instanceof SparseTimelineManhole) {
            SparseTimelineManhole tlm = (SparseTimelineManhole) manholes[0].getStatusTimeLine();
            if (tlm.getTimeContainer() instanceof SparseTimeLineManholeContainer) {
                SparseTimeLineManholeContainer cont = (SparseTimeLineManholeContainer) tlm.getTimeContainer();
                if (cont.getDataprovider() instanceof HE_Database) {
//                    System.out.println("Take inflow from HE ");
                    HE_Database db = (HE_Database) cont.getDataprovider();
                    try {
                        HashMap<Integer, Double> map = db.loadManholeTotalSurfaceInflow();
                        if (map != null) {
                            System.out.println("Manholes: "+manholes.length+"  inflow: "+map.size());
                            for (int i = 0; i < manholes.length; i++) {
                                Double d = map.get((int) (manholes[i].getManualID()));
                                if (d == null) {
                                    System.err.println("Could not get inflow for manhole " + manholes[i].getName() + " / " + manholes[i].getManualID());
                                } else {
                                    volume[i] = d.floatValue();
                                }
                            }
                            needCalculation = false;
                            totalvolume = 0;
                            for (int i = 0; i < volume.length; i++) {
                                totalvolume += volume[i];
                            }
                            System.out.println("Total inflow volume is " + totalvolume + " from HE Database");
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(InjectionInflowInformation.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(InjectionInflowInformation.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (needCalculation) {
//            System.out.println("Load unique timelines to calculate total inflow");
            for (int m = 0; m < manholes.length; m++) {
                Manhole manhole = manholes[m];
                for (int i = 0; i < manhole.getStatusTimeLine().getNumberOfTimes(); i++) {
                    double v = manhole.getStatusTimeLine().getInflow(i);
                    totalvolume += v;
                    volume[m] += v;
                }
            }
            System.out.println("Total inflow volume is " + totalvolume + " for Manholes");
        }
        if (concentration > 0) {
            totalMass = totalvolume * concentration;
        } else if (totalMass > 0) {
            concentration = totalMass / totalvolume;
        }

        if (totalArea > 0) {
            load = totalMass / totalArea;
        }
        changed = true;
    }

    /**
     * kg/m^2
     *
     * @return
     */
    public double getConcentration() {
        return concentration;
    }

    /**
     *
     * @param concentration kg/m^3
     */
    public void setConcentration(double concentration) {
        if (this.concentration == concentration) {
            return;
        }
        this.concentration = concentration;
        totalMass = totalvolume * concentration;
        if (totalArea > 0) {
            load = totalMass / totalArea;
        }
        changed = true;
    }

    /**
     *
     * @param load kg/m^2
     */
    public void setLoad(double load) {
        if (this.load == load) {
            return;
        }
        this.load = load;
        if (totalArea > 0) {
            totalMass = totalArea * load;
        }
        if (totalvolume > 0) {
            concentration = totalMass / totalvolume;
        }
        changed = true;
    }

    @Override
    public double getStarttimeSimulationsAfterSimulationStart() {
        return startSeconds;
    }

    @Override
    public double getDurationSeconds() {
        return durationSeconds;
    }

    @Override
    public double getMass() {
        return totalMass;
    }

    public double getLoad() {
        return load;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean spillOnSurface() {
        return true;
    }

    @Override
    public boolean spillInManhole() {
        return false;
    }

    @Override
    public GeoPosition2D getPosition() {
        if (network == null) {
            return null;
        }
        return network.getLeaves().iterator().next().getPosition3D(0);
    }

    @Override
    public Capacity getCapacity() {
        return null;
    }

    @Override
    public int getNumberOfIntervals() {
        return 1;
    }

    @Override
    public double getIntervalStart(int interval) {
        return 0;
    }

    @Override
    public double getIntervalEnd(int interval) {
        return 0;
    }

    @Override
    public double getIntervalDuration(int interval) {
        return 0;
    }

    @Override
    public double massInInterval(int interval) {
        return totalMass;
    }

    @Override
    public int particlesInInterval(int interval) {
        return numberOfParticles;
    }

    @Override
    public double getIntensity(int intervalIndex) {
        return totalMass;
    }

    @Override
    public void setCapacity(Capacity capacity) {

    }

    @Override
    public void setId(int id) {
        this.id = id;
        changed = true;
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }

    /**
     * Set the change flag to false. Should be called, after particles have been
     * addapted to the new values.
     */
    @Override
    public void resetChanged() {
        changed = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public int getMaterialID() {
        return materialID;
    }

    @Override
    public void setMaterial(Material material) {
        if (this.material == material) {
            return;
        }
        this.material = material;
        changed = true;
    }

    @Override
    public int getNumberOfParticles() {
        return numberOfParticles;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setMaterialID(int mid) {
        if (this.materialID == mid) {
            return;
        }
        this.materialID = mid;
        this.material = null;
        changed = true;
    }

    public void setStart(double s) {
        if (startSeconds == s) {
            return;
        }
        this.startSeconds = s;
        changed = true;
    }

    public void setDuration(double d) {
        if (durationSeconds == d) {
            return;
        }
        this.durationSeconds = d;
        changed = true;
    }

    @Override
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        changed = true;
    }

    @Override
    public void setNumberOfParticles(int number) {
        if (this.numberOfParticles == number) {
            return;
        }
        this.numberOfParticles = number;
        changed = true;
    }

    public void setMass(double mass) {
        if (this.totalMass == mass) {
            return;
        }
        this.totalMass = mass;
        if (totalvolume > 0) {
            this.concentration = totalMass / totalvolume;
        }
        if (totalArea > 0) {
            load = totalMass / totalArea;
        }

        changed = true;
    }

    public void setNetwork(Network nw) {
        this.network = nw;
        findAndLoadManholes();
        changed = true;
    }

    public Manhole[] getManholes() {
        return manholes;
    }

    public float[] getVolumePerManhole() {
        return volume;
    }

    public double getTotalvolume() {
        return totalvolume;
    }

    public Network getNetwork() {
        return network;
    }

    public double getTotalArea() {
        return totalArea;
    }

}
