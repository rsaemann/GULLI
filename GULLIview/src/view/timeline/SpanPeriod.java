/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.timeline;

import java.util.Calendar;
import org.jfree.data.time.RegularTimePeriod;

/**
 *
 * @author saemann
 */
public class SpanPeriod extends RegularTimePeriod{
    RegularTimePeriod rp;
    int multiplier;

    public SpanPeriod(RegularTimePeriod rp, int multiplier) {
        this.rp = rp;
        this.multiplier = multiplier;
    }
    
    @Override
    public RegularTimePeriod previous() {
       return rp.previous();//new SpanPeriod(rp.previous(),multiplier);
    }

    @Override
    public RegularTimePeriod next() {
        return rp.next();//new SpanPeriod(rp.next(),multiplier);
    }

    @Override
    public long getSerialIndex() {
        return rp.getSerialIndex();
    }

    @Override
    public void peg(Calendar clndr) {
         rp.peg(clndr);
    }

    @Override
    public long getFirstMillisecond() {
       return rp.getFirstMillisecond();
    }

    @Override
    public long getFirstMillisecond(Calendar clndr) {
       return rp.getFirstMillisecond(clndr);
    }

    @Override
    public long getLastMillisecond() {
        long span=rp.getLastMillisecond()-rp.getFirstMillisecond();
        return rp.getLastMillisecond()+(multiplier*span);
    }

    @Override
    public long getLastMillisecond(Calendar clndr) {
        long span=rp.getLastMillisecond(clndr)-rp.getFirstMillisecond(clndr);
        return rp.getLastMillisecond(clndr)+(multiplier*span);
    }

    @Override
    public int compareTo(Object t) {
       return 1;// rp.compareTo(t);
    }
    
}
