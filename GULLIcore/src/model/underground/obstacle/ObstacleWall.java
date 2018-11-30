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
package model.underground.obstacle;

import control.maths.GeometryTools;
import java.awt.geom.Line2D;
import model.topology.Position3D;

/**
 * 2D Wall (no depth) in 3D Space that blocks particle movement
 *
 * @author saemann
 */
public class ObstacleWall implements Obstacle3D {

    protected Line2D.Double line;
    protected double lowerZ, upperZ;

    @Override
    public void checkMovement(Position3D start, Position3D target, double length) throws Blocked3DMovement {
        if (line.intersectsLine(start.getX(), start.getY(), target.getX(), target.getZ())) {
            //Intersection of streamline and wall
            //check if movement is blocked in height
            if (start.getZ() > upperZ && target.getZ() > upperZ) {
                //Movement is above wall -> no blocking
                return;
            }
            if (start.getZ() < lowerZ && target.getZ() < lowerZ) {
                //Movement is below wall -> no blocking
                return;
            }
            //Movement is bloked by wall. -> transform back to position before wall
            double s = GeometryTools.lineIntersectionS(start.getX(), start.getY(), target.getX(), target.getY(), line.getX1(), line.getY1(), line.getX2(), line.getY2());
            double factor1mm = 0.001 / length;
            s -= factor1mm; //particle shall stop 1mm before obstacle position to prevent point-in-obstacle-case.
            //Create inteprolated position.
            double x_new = start.getX() + s * (target.getX() - start.getX());
            double y_new = start.getY() + s * (target.getY() - start.getY());
            double z_new = start.getZ() + s * (target.getZ() - start.getZ());

            double lat_new = start.getLatitude() + s * (target.getLatitude() - start.getLatitude());
            double lon_new = start.getLongitude() + s * (target.getLongitude() - start.getLongitude());

            throw new Blocked3DMovement(this, new Position3D(lon_new, lat_new, x_new, y_new, z_new));

        } else {
            //No intersection -> do nothing
        }
    }

}
