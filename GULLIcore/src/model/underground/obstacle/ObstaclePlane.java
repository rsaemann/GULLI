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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import model.topology.Position3D;

/**
 *
 * @author saemann
 */
public class ObstaclePlane implements Obstacle3D {

    protected Polygon polygon;
    /**
     * Calculate a raw unsafe version of distance before creating Geometries for
     * detailed intersection calculation.
     */
    public static boolean preDistance = true;

    /**
     * Passing of movement from both sides is blocked.
     * @param polygon 
     */
    public ObstaclePlane(Polygon polygon) {
        this.polygon = polygon;
    }
    
    @Override
    public void checkMovement(Position3D start, Position3D target, double length) throws Blocked3DMovement {

        if (preDistance) {
            double sqdist = (start.getX() - polygon.getCoordinate().x) * (start.getX() - polygon.getCoordinate().x) + (start.getY() - polygon.getCoordinate().y) * (start.getY() - polygon.getCoordinate().y) + (start.getZ() - polygon.getCoordinate().z) * (start.getZ() - polygon.getCoordinate().z);
            if (sqdist > (3 * length + polygon.getLength())) {
            //particle is not near to this polygon
                // ToDo find something more robust 
                return;
            }
        }
        //Need more detailed checking.
        LineString movementline = polygon.getFactory().createLineString(new Coordinate[]{start.get3DCoordinate(), target.get3DCoordinate()});
        if (polygon.intersects(movementline)) {
            Geometry hit = polygon.intersection(movementline);
            //Move a bit away from the obstacle towards the start point. 
            // Particle shall stop 1mm before the obstacle
            double s = start.distanceUTM3D(hit.getCoordinate().x, hit.getCoordinate().y, hit.getCoordinate().z);
            //Shift s back by 1 mm.
            s -= 0.001 / length;
            //Interpolate new Particles Position
            double x_new = start.getX() + s * (target.getX() - start.getX());
            double y_new = start.getY() + s * (target.getY() - start.getY());
            double z_new = start.getZ() + s * (target.getZ() - start.getZ());

            double lat_new = start.getLatitude() + s * (target.getLatitude() - start.getLatitude());
            double lon_new = start.getLongitude() + s * (target.getLongitude() - start.getLongitude());

            throw new Blocked3DMovement(this, new Position3D(lon_new, lat_new, x_new, y_new, z_new));
        } else {
            // no intersection
        }
    }

}
