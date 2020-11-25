/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.control.listener;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;

/**
 *
 * @author saemann
 */
public interface LoadingActionListener {

    public void actionFired(Action action, Object source);
    
    public void loadNetwork(Network network, Object caller);
    
    public void loadSurface(Surface surface, Object caller);
    
    public void loadScenario(Scenario scenario, Object caller);
}
