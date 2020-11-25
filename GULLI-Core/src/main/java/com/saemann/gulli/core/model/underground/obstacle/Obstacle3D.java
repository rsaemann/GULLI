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
package com.saemann.gulli.core.model.underground.obstacle;

import com.vividsolutions.jts.geom.Coordinate;
import com.saemann.gulli.core.model.topology.Position3D;

/**
 * Obstacles in 3D continuum, to bounce back particle movement
 *
 * @author saemann
 */
public interface Obstacle3D {

    /**
     * Check if movement is blocked by this obstacle. Throws an Error with
     * information of new end position if movement is blocked.
     *
     * @param start particle position last timestep
     * @param target particle position after unblocked movement
     * @throws model.underground.obstacle.Blocked3DMovement new position
     * Information
     */
    public void checkMovement(Coordinate start, Coordinate target, double movementLength) throws Blocked3DMovement;

}
