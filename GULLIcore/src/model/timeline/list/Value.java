/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.timeline.list;

/**
 *
 * @author saemann
 */
public interface Value {

    public String getSymbol();

    public String getName();

    public String getUnit();

    public double getValue();
    
    public String getKey();

    public void setValue(double newvalue);

    /**
     * Indicates if both Objects store the same type of value.
     * @param v
     * @return 
     */
    public boolean usedForSameValue(Value v);
    
    

}
