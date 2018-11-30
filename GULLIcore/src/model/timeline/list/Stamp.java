/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.timeline.list;

import model.timeline.list.SimpleStorageStamp;

/**
 *
 * @author saemann
 * @param <E>
 */
public class Stamp<E extends SimpleStorageStamp> implements Comparable<Stamp> {

    protected long timeStamp;
    protected E values;

    public Stamp(long timeStamp, E values) {
        this.timeStamp = timeStamp;
        this.values = values;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public E getValues() {
        return values;
    }

    @Override
    public int compareTo(Stamp t) {
        if (t.timeStamp < this.timeStamp) {
            return 1;
        }
        if (t.timeStamp > this.timeStamp) {
            return -1;
        }
        return 0;
    }

}
