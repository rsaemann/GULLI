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
 *
 * @author saemann
 */
public class Blocked3DMovement extends Exception {

    public final Obstacle3D obstacle;

    /**
     * New position representing the position when blocked by the obstacle.
     */
    public final Coordinate deflected_Position;

    public Blocked3DMovement(Obstacle3D obstacle, Coordinate deflected_Position, String string) {
        super(string);
        this.obstacle = obstacle;
        this.deflected_Position = deflected_Position;
    }

    public Blocked3DMovement(Obstacle3D obstacle, Coordinate deflected_Position) {
        super();
        this.obstacle = obstacle;
        this.deflected_Position = deflected_Position;
    }

}
