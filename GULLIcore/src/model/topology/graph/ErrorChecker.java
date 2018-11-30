/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.graph;

import java.util.HashSet;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class ErrorChecker {

    public static HashSet<ErrorNote> checkForErrors(Network network) {
        HashSet<ErrorNote> notes = new HashSet<>();
        for (Manhole mh : network.getManholes()) {
            if(mh.getTop_height()<mh.getSole_height()){
                 notes.add(new ErrorNote(mh.getPosition(), new String[]{mh.toString(), "Top is below Sole","Top: "+mh.getTop_height()+"m","Sole MH: "+mh.getSole_height()+"m"}));
                 continue;
            }
            for (Connection_Manhole_Pipe c : mh.getConnections()) {
                if (c.getHeight() < mh.getSole_height()) {
                    if (c.isFlowInletToPipe()) {
                        notes.add(new ErrorNote(c.getPosition(), new String[]{mh.toString(),"Outflow into " + c.getPipe().toString(), " is below sole-height.","Top MH: "+mh.getTop_height()+"m","Sole Pp: "+c.getHeight()+"m","Sole MH: "+mh.getSole_height()+"m"}));
                    } else {
                        notes.add(new ErrorNote(c.getPosition(), new String[]{mh.toString(),"inflow from " + c.getPipe().toString(), " is below sole-height.","Top MH: "+mh.getTop_height()+"m","Sole Pp: "+c.getHeight()+"m","Sole MH: "+mh.getSole_height()+"m"}));
                    }
                }
                if (c.getHeight() > mh.getTop_height()) {
                    if (c.isFlowInletToPipe()) {
                        notes.add(new ErrorNote(c.getPosition(), new String[]{mh.toString(),"Outflow into " + c.getPipe().toString(), " is above top-height.","Top MH: "+mh.getTop_height()+"m","Sole Pp: "+c.getHeight()+"m","Sole MH: "+mh.getSole_height()+"m"}));
                    } else {
                        notes.add(new ErrorNote(c.getPosition(), new String[]{mh.toString(),"inflow from " + c.getPipe().toString(), " is above top-height.","Top MH: "+mh.getTop_height()+"m","Sole Pp: "+c.getHeight()+"m","Sole MH: "+mh.getSole_height()+"m"}));
                    }
                }
            }
        }
        for (Pipe pipe : network.getPipes()) {
//            if(pipe.getFlowInletConnection().getHeight()<pipe.getFlowOutletConnection().getHeight()){
//                notes.add(new ErrorNote(pipe.getPositionAlongAxisRelative(0.5), new String[]{"Wrong directed Pipe"}));
//            }
            if(pipe.getDecline()>0.5){
                notes.add(new ErrorNote(pipe.getPositionAlongAxisRelative(0.4), new String[]{"positive decline"}));
            }
        }
        return notes;
    }
}
