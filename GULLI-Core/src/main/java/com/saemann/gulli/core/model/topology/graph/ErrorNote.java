/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.topology.graph;

import com.saemann.gulli.core.model.topology.Position;

/**
 *
 * @author saemann
 */
public class ErrorNote {

    protected final Position position;
    protected final String[] information;

    public ErrorNote(Position position, String[] information) {
        this.position = position;
        this.information = information;
    }

    public String[] getInformation() {
        return information;
    }

    public Position getPosition() {
        return position;
    }
    
    
}
