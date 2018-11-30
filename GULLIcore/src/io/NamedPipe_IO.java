/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io;

import control.Controller;
import java.util.HashSet;
import model.topology.Manhole;
import model.topology.Position;

/**
 * Helper class to talk to other Processes via NamedPipe-Inter Process
 * Communication
 *
 * @author saemann
 */
public class NamedPipe_IO extends NamedPipeIO {

    public static NamedPipe_IO instance;

    protected final HashSet<Long> sentIds = new HashSet<>();

    public NamedPipe_IO() {
        this(null);
    }

    public NamedPipe_IO(Controller control) {
        super(control);
        instance = this;
    }

    public void notifyAboutFloodedManhole(Manhole manhole) {
        if (sentIds.contains(manhole.getAutoID())) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("MH;").append(manhole.getAutoID());
        sb.append(";x:").append(manhole.getPosition().getX());
        sb.append(";y:").append(manhole.getPosition().getY());
        addMessageToSend(sb.toString());
        sentIds.add(manhole.getAutoID());
    }

    public void reset() {
        addMessageToSend("RESET");
        sentIds.clear();
    }

    public static void main_(String[] args) {
        NamedPipe_IO io = new NamedPipe_IO();
        io.addMessageToSend("RESET");
        io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 548861, 5799836), "Kreisel", null));
        io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547640, 5800000), "Tönnisberg", null));
        io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547986, 5799563), "Nenndorfer Platz", null));
        io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547657, 5799252), "Wallensteinstraße West", null));

        io.connectToSurface();

    }

}
