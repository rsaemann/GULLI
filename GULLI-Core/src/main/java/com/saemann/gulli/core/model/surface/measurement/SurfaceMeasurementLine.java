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
package com.saemann.gulli.core.model.surface.measurement;

import com.vividsolutions.jts.geom.Coordinate;
import com.saemann.gulli.core.control.maths.GeometryTools;
import java.util.LinkedList;
import com.saemann.gulli.core.model.GeoPosition;
import com.saemann.gulli.core.model.GeoPosition2D;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class SurfaceMeasurementLine {

    TriangleMeasurement[] measurements;

    GeoPosition2D start, end;

    public static SurfaceMeasurementLine createLine(Surface surf, int startTriangleID, int endTriangleID, TimeIndexContainer times) throws TransformException {
        double startX = surf.getTriangleMids()[startTriangleID][0];
        double startY = surf.getTriangleMids()[startTriangleID][1];
        double endX = surf.getTriangleMids()[endTriangleID][0];
        double endY = surf.getTriangleMids()[endTriangleID][1];
        return createLine(surf, startX, startY, endX, endY, times);
    }

    public static SurfaceMeasurementLine createLine(Surface surf, double utmXStart, double utmYStart, double utmXEnd, double utmYEnd, TimeIndexContainer times) throws TransformException {
        if (surf.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
            double distanceSquare = ((utmXStart - utmXEnd) * (utmXStart - utmXEnd) + (utmYStart - utmYEnd) * (utmYStart - utmYEnd));
            double distance = Math.sqrt(distanceSquare);
            double[] linemid = new double[]{(utmXStart + utmXEnd) * 0.5, (utmYStart + utmYEnd) * 0.5};
            //Test wich triangle cuts the line
            LinkedList<Integer> triangleIDs = new LinkedList<>();
            for (int i = 0; i < surf.getTriangleMids().length; i++) {
                double[] mid = surf.getTriangleMids()[i];
                //Test for bounding box
                double dx = (mid[0] - linemid[0]);
                if (Math.abs(dx) > distance) {
                    continue;
                }
                double dy = (mid[1] - linemid[1]);
                if (Math.abs(dy) > distance) {
                    continue;
                }
                //test for triangle's sides
                for (int j = 0; j < 3; j++) {
                    double[] startVertex = surf.getVerticesPosition()[surf.getTriangleNodes()[i][j]];
                    double[] endVertex = surf.getVerticesPosition()[surf.getTriangleNodes()[i][(j + 1) % 3]];
                    //Search for intersection between triangle side and measurement line
                    double[] st = GeometryTools.lineIntersectionST(startVertex[0], startVertex[1], endVertex[0], endVertex[1], utmXStart, utmYStart, utmXEnd, utmYEnd);
                    if (st[0] >= 0 && st[0] <= 1 && st[1] >= 0 && st[1] <= 1) {
                        //Intersection hit
                        triangleIDs.add(i);
                        break;
                    }
                }
            }

            TriangleMeasurement[] ms = new TriangleMeasurement[triangleIDs.size()];
            int index = 0;
            for (Integer triangleID : triangleIDs) {
                TriangleMeasurement m;

                m = ((SurfaceMeasurementTriangleRaster) surf.getMeasurementRaster()).createMeasurement(triangleID);
                //new TriangleMeasurement(times, triangleID);
                ms[index] = m;
                index++;

            }
            Coordinate startC = surf.getGeotools().toGlobal(new Coordinate(utmXStart, utmYStart));
            Coordinate endC = surf.getGeotools().toGlobal(new Coordinate(utmXEnd, utmYEnd));

            return new SurfaceMeasurementLine(ms, new GeoPosition(startC), new GeoPosition(endC));
        }
        throw new UnsupportedOperationException("Can naot Build a line measurement for someting else than SurfaceMeasurementTriangleRaster");
    }

    public SurfaceMeasurementLine(TriangleMeasurement[] measurements, GeoPosition2D startPosition, GeoPosition2D endPosition) {
        this.measurements = measurements;
        this.start = startPosition;
        this.end = endPosition;
    }

    public TriangleMeasurement[] getMeasurements() {
        return measurements;
    }

    public GeoPosition2D getEnd() {
        return end;
    }

    public GeoPosition2D getStart() {
        return start;
    }

}
