/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology;

import java.util.ArrayList;
import java.util.Collection;
import model.topology.catchment.Catchment;

/**
 *
 * @author saemann
 */
public class SWMMNetwork extends Network {

    protected ArrayList<Catchment> catchments = new ArrayList<>(0);

    public SWMMNetwork(Collection<Pipe> pipes, Collection<Manhole> manholes) {
        super(pipes, manholes);
    }

    public ArrayList<Catchment> getCatchments() {
        return catchments;
    }

    public boolean add(Catchment catchment) {
        return (catchments.add(catchment));
    }

    public boolean addAll(Collection<Catchment> c) {
        return catchments.addAll(c);
    }

}
