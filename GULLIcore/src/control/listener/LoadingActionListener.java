/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.listener;

import control.Action.Action;
import model.surface.Surface;
import model.topology.Network;

/**
 *
 * @author saemann
 */
public interface LoadingActionListener {

    public void actionFired(Action action, Object source);
    
    public void loadNetwork(Network network, Object caller);
    
    public void loadSurface(Surface surface, Object caller);
}
