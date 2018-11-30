package model.timeline.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/**
 *
 * @author saemann
 * @param <E>
 */
public class ListTimeLine<E extends SimpleStorageStamp> implements TimeLine<E> {

    protected boolean sorted = false;

    protected long lastquery = 0;
    protected Stamp<E> lastAnswer = null;
    protected long nextquery = 0;
    protected Stamp<E> nextAnswer = null;

    protected ArrayList<Stamp<E>> list;
    /**
     * Stores all additional values that are in one of the stanps in this list.
     * For faster detection.
     */
    protected Value[] additionalValues;

    public ListTimeLine(int initSize) {
        list = new ArrayList<>(initSize);
    }

    public ListTimeLine() {
        list = new ArrayList<>();
    }

    public ListTimeLine(Collection<? extends Stamp<E>> clctn) {
        this(clctn.size());
        this.addAll(clctn);
        sort();
    }

    public void sort() {
        Collections.sort(this.list);
        sorted = true;
    }

    @Override
    public boolean set(Stamp<E> e) {
        boolean succ = this.list.add(e);
        if (succ) {
            sorted = false;
        }
        return succ;
    }

    @Override
    public boolean addAll(Collection<? extends Stamp<E>> clctn) {
        boolean succ = this.list.addAll(clctn);
        if (succ) {
            sort();
        }
        return succ;
    }

    /**
     * Returns false if {@link sort()} was not called after the last adiition of
     * an element.
     *
     * @return
     */
    public boolean isSorted() {
        return sorted;
    }

    @Override
    public Stamp<E> getEarlier(long timestamp) {
        Stamp<E> last = null;
        for (Stamp<E> st : this.list) {
            if (st.timeStamp >= timestamp) {
                break;
            }
            last = st;
        }
        return last;
    }

    /**
     * Get the youngest (in the past) or the exact timestamp for NOW.
     *
     * @param timestamp
     * @return
     */
    public Stamp<E> getStampEarlierUntilNow(long timestamp) {
        if (timestamp >= lastquery && timestamp < nextquery) {
            return lastAnswer;
        }
        Stamp<E> last = null;
        Stamp<E> actual = null;
        Iterator<Stamp<E>> it = this.list.iterator();
        while (it.hasNext()) {
            last = actual;
            actual = it.next();
            if (actual.timeStamp > timestamp) {
                lastquery = timestamp;
                lastAnswer = last;
                nextAnswer = actual;
                nextquery = actual.timeStamp;
                if (last == null) {
                    this.sort();
                    System.err.println(this.getClass().getName() + ": Can not find a Date before " + new Date(timestamp).toString() + "  first date is " + new Date(this.list.get(0).timeStamp));
                }
                return lastAnswer;
            }
        }

        if (last == null && !this.list.isEmpty()) {
            this.sort();
            System.err.println(this.getClass().getName() + ": Can not find a Date before " + new Date(timestamp).toString() + "  first date is " + new Date(this.list.get(0).timeStamp));
        }
        lastquery = timestamp;
        lastAnswer = last;
        return last;
    }

    public Stamp<E> getLater(long timestamp) {
        Stamp<E> last = null;
        for (Stamp<E> st : this.list) {
            if (st.timeStamp <= timestamp) {
                break;
            }
            last = st;
        }
        return last;
    }

    public Stamp<E> getInterpolated(long timestamp) {
        Stamp<E> last = null;
        Stamp<E> next = null;

        for (Stamp<E> st : this.list) {
            if (st.timeStamp <= timestamp) {
                next = st;
                break;
            }
            last = st;
        }
        if (last == null) {
            return next;
        }
        if (next == null) {
            return last;
        }

        return new Stamp(timestamp, last.getValues().interpolate(next.getValues(), timestamp));
    }

    @Override
    public Collection<Value> getAdditionalValues() {
        if (additionalValues == null || additionalValues.length == 0) {
            return new ArrayList<>(0);
        }
        return Arrays.asList(additionalValues);
    }

    @Override
    public boolean hasAdditionalValues() {
        if (additionalValues == null || additionalValues.length == 0) {
            return false;
        }
        return true;
    }

    @Override
    public int addAdditionalValue(Value value) {
        if (additionalValues == null) {
            additionalValues = new Value[1];
            additionalValues[0] = value;
            return 0;
        }
        Value[] newValues = new Value[additionalValues.length + 1];
        for (int i = 0; i < additionalValues.length; i++) {
            //Search for gap and set the new Value in here.
            if (additionalValues[i] == null) {
                additionalValues[i] = value;
                return i;
                //Forget about the copied array
            }
            newValues[i] = additionalValues[i];

        }
        newValues[newValues.length - 1] = value;
        additionalValues = newValues;
        return newValues.length - 1;
    }

    @Override
    public Stamp<E> getFirst() {
        if (this.list.size() < 1) {
            return null;
        }
        return this.list.get(0);
    }

    @Override
    public E getEarlierUntilNow(long timestamp) {
        return this.getStampEarlierUntilNow(timestamp).getValues();
    }

    @Override
    public Iterator<Stamp<E>> iterator() {
        return this.list.iterator();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public Stamp<E> getLast() {
        return this.list.get(this.list.size() - 1);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Stamp<E> getStamp(int index) {
        return list.get(index);
    }

    @Override
    public E getValueStamp(int index) {
        return list.get(index).getValues();
    }

    @Override
    public boolean removeAdditionalValues(Value v) {
        if (v == null) {
            return false;
        }
        int index = -1;
        for (int i = 0; i < additionalValues.length; i++) {
            if (v.usedForSameValue(additionalValues[i])) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }
        for (Stamp<E> s : list) {
            s.getValues().removeAdditionalValue(index);
        }

        return true;
    }

    @Override
    public int getIndex(Value v) {
        for (int i = 0; i < additionalValues.length; i++) {
            if (v.usedForSameValue(additionalValues[i])) {
                return i;
            };
        }
        return -1;
    }

    @Override
    public double getTimeIntervallSeconds() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
