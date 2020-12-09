/*
 * The MIT License
 *
 * Copyright 2020 saemann.
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
package com.saemann.gulli.core.model.topology;

import com.saemann.gulli.core.model.timeline.array.TimeLineManhole;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.gulli.core.model.topology.profile.Profile;

/**
 * A replacement for the Connection_Manhole_Surface connection, if no real
 * surface is defined. Within this storage volume, the particle can spill back
 * to the manhole, if the spillot flux is negative.
 *
 * @author saemann
 */
public class Manhole_SurfaceBucket extends StorageVolume implements Connection_ToSurface {

    public static Profile bucketprofile = new CircularProfile(1);

    public static Connection_Manhole_Pipe[] noconnection = new Connection_Manhole_Pipe[0];

    protected StorageVolume manhole;

    protected Position3D position;

    protected Connection_Manhole_Pipe connectionToManhole;

    public Manhole_SurfaceBucket(StorageVolume manhole, Position3D position) {
        super(bucketprofile);
        this.manhole = manhole;
        this.position = position;

        connectionToManhole = new Connection_Manhole_Pipe((Manhole) manhole, 100);
    
        this.addConnection(connectionToManhole);
        timelineStatus = manhole.timelineStatus;
    }

    @Override
    public Position3D getPosition() {
        return position;
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return getPosition();
    }

    @Override
    public float getHeight() {
        return (float) position.z;
    }

    @Override
    public StorageVolume getManhole() {
        return manhole;
    }

    @Override
    public Capacity getConnectedCapacity() {
        return this;
    }

    @Override
    public double getWaterlevel() {
        return 1;
    }

    public TimeLineManhole getTimelineStatus() {
        return manhole.timelineStatus;
    }

    @Override
    public boolean emitsToSurfaceDomain() {
        return false;
    }

    @Override
    public Capacity targetCapacity() {
        return this;
    }

    @Override
    public int targetSurfaceID() {
        return -1;
    }

    @Override
    public Connection_Manhole_Pipe[] getConnections() {
        //DO not show the opportunity to spill back to the manhole, if water is still spilling from the manhole onto the surface
        if (manhole.getStatusTimeLine().getActualFlowToSurface() > 0) {
            return noconnection;
        }
        //If the water is going back, also spill back the particles.
        return connections;
    }

    @Override
    public String toString() {
        return "Manhole_SurfaceBucket on "+manhole;
    }
    
    

}
