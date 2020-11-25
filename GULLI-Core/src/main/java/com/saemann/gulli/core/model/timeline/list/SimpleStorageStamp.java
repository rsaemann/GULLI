/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.model.timeline.list;

import com.saemann.gulli.core.control.GlobalParameter;
import java.util.Arrays;
import java.util.List;

/**
 * Momentaufnahme der Eigenschaften eines StorageObjekt
 *
 * @author saemann
 */
public abstract class SimpleStorageStamp implements StorageStamp{

    protected Value[] additionalValues;

    public SimpleStorageStamp() {
    }

    public abstract SimpleStorageStamp interpolate(SimpleStorageStamp other, long timestamp);

    public List<Value> getAdditionalValuesCollection() {
        if (additionalValues == null) {
            return null;
        }
        if (additionalValues.length == 0) {
            return null;
        }
        return Arrays.asList(additionalValues);
    }

    public abstract boolean valueEquality(SimpleStorageStamp o, double maxTolerance);

    public boolean removeAdditionalValue(Value v) {
        if (additionalValues == null) {
            return true;
        }
       
        return removeAdditionalValue(getIndex(v));
    }

    public int getIndex(Value v) {
        for (int i = 0; i < additionalValues.length; i++) {
            if (v.usedForSameValue(additionalValues[i])) {
                return i;
            };

        }
        return -1;
    }

    public boolean removeAdditionalValue(int index) {
        if (additionalValues == null) {
            return false;
        }
        if (additionalValues[index] == null) {
            return false;
        }
        additionalValues[index] = null;
        return true;
    }

    public boolean addValue(Value v, int index) {

        if (additionalValues[index] != null) {
            additionalValues[index].setValue(additionalValues[index].getValue() + v.getValue());
        } else {
            additionalValues[index] = v;
        }
//        if (additionalValues == null) {
//            additionalValues = new ArrayList<>(1);
//        }
//        for (Value av : additionalValues) {
//            if (av.usedForSameValue(v)) {
//                av.setValue(av.getValue() + v.getValue());
//                return true;
//            }
//        }
//        return additionalValues.add(v);
        return true;
    }

    public boolean setValue(Value v, int index) {
//        if (additionalValues == null) {
//            additionalValues = new ArrayList<>(1);
//        }
//        for (Value av : additionalValues) {
//            if (av.usedForSameValue(v)) {
//                av.setValue(v.getValue());
//                return true;
//            }
//        }
//        return additionalValues.add(v);
        additionalValues[index] = v;
        return true;
    }

    public void setAdditionalValueCount(int numberOfValues) {
        Value[] v = new Value[numberOfValues];
        if (additionalValues != null) {
            System.arraycopy(additionalValues, 0, v, 0, v.length);
        }
        additionalValues = v;
    }
}
