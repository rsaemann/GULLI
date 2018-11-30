package model.timeline.list;

import model.timeline.list.Value;
import model.timeline.list.Stamp;
import model.timeline.list.SimpleStorageStamp;
import java.util.Collection;

/**
 *
 * @author saemann
 * @param <E> Type of Stamps containing predifined values
 */
public interface TimeLine<E extends SimpleStorageStamp> extends Iterable<Stamp<E>> {
    
    public double getTimeIntervallSeconds();

    public Stamp<E> getEarlier(long timestamp);

    public E getEarlierUntilNow(long timestamp);

    public Stamp<E> getFirst();

    public Stamp<E> getLast();

    public boolean set(Stamp<E> e);

    public boolean addAll(Collection<? extends Stamp<E>> clctn);

    public int size();

    public boolean isEmpty();

    public Stamp<E> getStamp(int index);

    public E getValueStamp(int index);

    public boolean removeAdditionalValues(Value v);

    public Iterable<Value> getAdditionalValues();

    public int addAdditionalValue(Value value);
    
    public int getIndex(Value additionalValue);

    public boolean hasAdditionalValues();
}
