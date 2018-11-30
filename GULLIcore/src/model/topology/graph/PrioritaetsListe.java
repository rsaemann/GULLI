/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.graph;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * @author saemann
 * @param <E> the class of objects that will be sorted in this list.
 */
public class PrioritaetsListe<E> implements Collection<Pair<E, Double>> {

    LinkedList<Pair<E, Double>> list = new LinkedList<>();

    private final Comparator<Pair<E, Double>> comp = new Comparator<Pair<E, Double>>() {

        @Override
        public int compare(Pair<E, Double> t, Pair<E, Double> t1) {
            if (t.second > t1.second) {
                return -1;
            }
            return 0;
        }
    };

    public boolean add(E e, double d) {
        return add(new Pair<>(e, d));
    }

    public boolean add(E e, Double d) {
        return add(new Pair<>(e, d));
    }

    /**
     *
     * @param e
     * @return true if the list has changed
     */
    @Override
    public boolean add(Pair<E, Double> e) {
        if (list.isEmpty()) {
            list.add(e);
            return true;
        }
        // First iteration check if the obejct is already here with a greater second value.
        // if so, replace the value with the smaller one.
        Iterator<Pair<E, Double>> it = list.iterator();
        Pair<E, Double> p;
        while (it.hasNext()) {
            p = it.next();
            if (p.first.equals(e.first)) {
                if (p.second < e.second) {
                    return false;
                }
                p.second = e.second;
                return true;
            }
        }
        //Has not yet been in here, second iteration to find best fitting position.
        if (list.get(0).second > e.second) {
            list.addFirst(e);
            return true;
        }
        if (list.getLast().second <= e.second) {
            list.addLast(e);
            return true;
        }
        ListIterator<Pair<E, Double>> lit = list.listIterator();
        while (lit.hasNext()) {
            p = lit.next();
            if (p.second > e.second) {
                lit.previous();
                lit.add(e);
                return true;
            }
        }
        System.out.println(this.getClass().getSimpleName() + ": add : this should never happen. Pair: "+e);
        for (Pair<E, Double> pa : this) {
            System.out.println(pa);
        }
        throw new NullPointerException();
//        return true;
    }

    /**
     * Removes and returns the first Element (lowest value) from the list.
     *
     * @return
     */
    public Pair<E, Double> poll() {
        return list.removeFirst();
    }

    /**
     * See the first Element (lowest value) from the list. Does NOT remove the
     * element
     *
     * @return
     */
    public Pair<E, Double> peek() {
        return list.getFirst();
    }

    /**
     * See the last Element (highest value) from the list. Does NOT remove the
     * element.
     *
     * @return
     */
    public Pair<E, Double> last() {
        return list.getLast();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<Pair<E, Double>> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return list.toArray(ts);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> clctn) {
        return list.containsAll(clctn);
    }

    @Override
    public boolean addAll(Collection<? extends Pair<E, Double>> clctn) {
        for (Pair<E, Double> p : clctn) {
            this.add(p);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> clctn) {
        return list.removeAll(clctn);
    }

    @Override
    public boolean retainAll(Collection<?> clctn) {
        return list.retainAll(clctn);
    }

    @Override
    public void clear() {
        list.clear();
    }
    
    public Pair<E, Double> pollLast(){
        return list.removeLast();
    }

}
