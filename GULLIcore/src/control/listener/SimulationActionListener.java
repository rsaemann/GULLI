/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.listener;

/**
 *
 * @author saemann
 */
public interface SimulationActionListener {
    
    public void simulationINIT(Object caller);
    
    public void simulationSTART(Object caller);
    
    public void simulationSTEPFINISH(long loop,Object caller);
    
    public void simulationPAUSED(Object caller);
    
    public void simulationRESUMPTION(Object caller);
    
    public void simulationSTOP(Object caller);
    
    public void simulationFINISH(boolean timeOut,boolean particlesOut);
    
    public void simulationRESET(Object caller);
    
}
