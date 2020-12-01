/*
 * The MIT License
 *
 * Copyright 2018 saemann.
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
package com.saemann.gulli.core.control.particlecontrol;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.underground.Domain3D;
import com.saemann.gulli.core.model.underground.obstacle.Blocked3DMovement;
import com.saemann.gulli.core.model.underground.obstacle.Obstacle3D;
import org.locationtech.jts.geom.Coordinate;

/**
 *
 * @author saemann
 */
public class ParticleSoilComputing {

    /**
     * Domain storing nodes' coordinates and velocities
     */
    private Domain3D underground;

    /**
     * Standard timestep in seconds
     */
    private double deltaTime = 60 * 60 * 24 * 365;

    /**
     * Set simulation timestep in Seconds.
     *
     * @param deltaTime [s]
     */
    public void setDeltaTime(double deltaTime) {
        this.deltaTime = deltaTime;
    }

    /**
     * Set underground domain with nodes and velocities.
     *
     * @param underground
     */
    public void setUnderground(Domain3D underground) {
        this.underground = underground;
    }

    /**
     * Simulation tiestep in Seconds.
     *
     * @return
     */
    public double getDeltaTime() {
        return deltaTime;
    }

    /**
     * Underground domain with coodinates and velocities
     *
     * @return
     */
    public Domain3D getUnderground() {
        return underground;
    }

    /**
     * Moves the given particle in the 3dDomain for one timestep.
     *
     * @param p Particle in 3D Domain
     */
    public void moveParticle(Particle p) {
        Coordinate c = p.getPosition3d();

        int index = underground.getNearestCoordinateIndex(c);
        if (index < 0) {
            System.out.println("no near coordinate found for " + c);
        }
        float[] velocity = underground.velocity[0][index];
        double tx = c.x + velocity[0] * deltaTime;
        double ty = c.y + velocity[1] * deltaTime;
        double tz = c.z + velocity[2] * deltaTime;
        if (tz < underground.minZ) {
            tz = underground.minZ;
        }
        if (tz > underground.maxZ) {
            tz = underground.maxZ;
        }

        if (!underground.obstacles.isEmpty()) {
            //check collision
            Coordinate target = new Coordinate(tx, ty, tz);
            double distance = p.getPosition3d().distance(target);
            for (Obstacle3D obstacle : underground.obstacles) {
                try {
                    obstacle.checkMovement(p.getPosition3d(), target, distance);
                } catch (Blocked3DMovement ex) {
                    target = ex.deflected_Position;
                    Logger.getLogger(ParticleSoilComputing.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Todo: after deflection, recheck with already passed obstacles.
            }
        }

        p.getPosition3d().z = tz;
        p.getPosition3d().x = tx;
        p.getPosition3d().y = ty;
    }

}
