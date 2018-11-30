/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface.topology;

import model.surface.LocalMinimumPoint;
import model.topology.Manhole;

/**
 *
 * @author saemann
 */
public class LocalManholePoint extends LocalPoolPoint {

    private Manhole manhole;

    public LocalManholePoint(int id, int indexX, int indexY, Manhole manhole) {
        super(id, indexX, indexY);
        this.manhole = manhole;
    }

    public LocalManholePoint(LocalMinimumPoint point, Manhole manhole) {
        super(point);
        this.manhole = manhole;
    }

    public Manhole getManhole() {
        return manhole;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"+this.coordUTM+"; " + manhole + "}";
    }

}
