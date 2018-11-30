/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface;

/**
 *
 * @author saemann
 */
public class SurfaceOverrun {

    /**
     * Wasserstand üNN, oberhalb dessen das Wasser weiter in eine andere Area
     * läuft.
     */
    public double ueberlaufWasserstand_uNN = Double.POSITIVE_INFINITY;

    /**
     * Wassertiefe vom tiefsten Punkt, oberhalb dessen das Wasser weiter in eine
     * andere Area läuft.
     */
    public double ueberlaufWassertiefe = Double.POSITIVE_INFINITY;
    /**
     * ID der surfacearea, in welche das Wasser bei überschreiten des
     * Wasserstandes abgegeben wird.
     */
    public SurfaceLocalMinimumArea surfaceUeberlauf;

    public LocalMinimumPoint potentiellerUeberlaufZielPunkt;
    public boolean ueberlauf = false;
}
