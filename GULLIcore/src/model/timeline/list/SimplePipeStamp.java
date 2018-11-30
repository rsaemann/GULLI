package model.timeline.list;

import model.timeline.list.SimpleStorageStamp;

/**
 *
 * @author saemann
 */
public class SimplePipeStamp extends SimpleStorageStamp {

    protected float q, velocity, waterlevel;

    protected float referenceConcentration;//schmutzfracht,;

    /**
     *
     * @param q
     * @param velocity
     * @param lvl_pipe height frome pipesole
     */
    public SimplePipeStamp(double q, double velocity, double lvl_pipe) {
        this.q = (float) q;
        this.velocity = (float) velocity;
        this.waterlevel = (float) lvl_pipe;
    }

    /**
     *
     * @param q
     * @param velocity
     * @param lvl_pipe height frome pipesole
     * @param refConcentration
     */
    public SimplePipeStamp(double q, double velocity, double lvl_pipe, double refConcentration) {
        this.q = (float) q;
        this.velocity = (float) velocity;
        this.waterlevel = (float) lvl_pipe;
        this.referenceConcentration = (float) refConcentration;
    }

    public double getQ() {
        return q;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getWaterLevelPipe() {
        return waterlevel;
    }

    public double getHup() {
        return 0;//hup;
    }

    public double getHdown() {
        return 0;//hdown;
    }

    public double getFill_rate() {
        return 0;//fill_rate;
    }

    public void setReynolds(double reynolds) {
//        this.reynolds = reynolds;
    }

    public double getReynolds() {
        return 0;//reynolds;
    }

    public double getDispersion() {
        return 0;//dispersion;
    }

    public void setDispersion(double dispersion) {
//        this.dispersion = dispersion;
    }

//    public double getSchmutz() {
//        return schmutzfracht;
//    }
//
//    public void setSchmutz(double schmutz) {
//        this.schmutzfracht = schmutz;
//    }
    public double getReferenceConcentration() {
        return referenceConcentration;
    }

    public void setParticlePerCBM(double partsPerCBM) {

        this.referenceConcentration = (float) partsPerCBM;
    }

//    @Override
//    public boolean addValue(Value v) {
//        if (additionalValues == null) {
//            additionalValues = new ArrayList<>(1);
//        }
//        for (Value av : additionalValues) {
//            if (av.usedForSameValue(v)) {
////                System.out.println("update value for "+v.getName());
//                av.setValue(av.getValue() + v.getValue());
//                return true;
//            }
////            if (av.getSymbol().equals(v.getSymbol())) {
////                if (av.getUnit().equals(v.getUnit())) {
////                    if (av.getName().equals(v.getName())) {
//////                        System.err.println("Value like " + v.toString() + " already exists in SimplePipeStamp.");
//////                        if (true) {
//////                            throw new NullPointerException("juhu");
//////                        }
////                        av.setValue(av.getValue() + v.getValue());
////                        return true;
////                    }
////                }
////            }
//        }
//        return additionalValues.add(v);
//    }
//    public boolean setValue(Value v) {
//        if (additionalValues == null) {
//            additionalValues = new ArrayList<>(1);
//        }
//        for (Value av : additionalValues) {
//            if (av.usedForSameValue(v)) {
////                System.out.println("set value "+v.getName()+" to "+v.getValue());
//                av.setValue(v.getValue());
//                return true;
//            }
//        }
//        return additionalValues.add(v);
//    }
    @Override
    public SimpleStorageStamp interpolate(SimpleStorageStamp other, long timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param s
     * @param maxTolerance
     * @return
     */
    @Override
    public boolean valueEquality(SimpleStorageStamp s, double maxTolerance) {
        if (s == null) {
            return false;
        }
        if (s == this) {
            return true;
        }

        if (s.getClass() != this.getClass()) {
            return false;
        }

        SimplePipeStamp o = (SimplePipeStamp) s;

        if (Math.abs(o.q - this.q) > maxTolerance) {
            return false;
        }
        if (Math.abs(o.velocity - this.velocity) > maxTolerance) {
            return false;
        }
        if (Math.abs(o.waterlevel - this.waterlevel) > maxTolerance) {
            return false;
        }

        if (additionalValues == null ^ s.additionalValues == null) {
            return false;
        }
        if (additionalValues != null) {
            for (int i = 0; i < additionalValues.length; i++) {
                if (Math.abs(additionalValues[i].getValue() - s.additionalValues[i].getValue()) > maxTolerance) {
                    return false;
                }
            }
        }
//        if (this.additionalValues != null) {
//            if (s.additionalValues == null) {
//                return false;
//            }
//            for (Value thisvalue : this.additionalValues) {
//                boolean fit = false;
//                for (Value svalue : s.additionalValues) {
//                    if (thisvalue.getName().equals(svalue.getName())) {
//                        if (Math.abs(thisvalue.getValue() - svalue.getValue()) < maxTolerance) {
//                            fit = true;
//                            continue;
//                        }
//                        return false;
//                    }
//                }
//                if (!fit) {
//                    System.err.println(thisvalue.getName() + " (" + thisvalue.getValue() + ") has no corresponding value in the tested SimplePipeStamp.");
//                    for (Value additionalValue : s.additionalValues) {
//                        System.out.println("   - " + additionalValue.getName() + " " + additionalValue.getValue());
//                    }
//                    return false;
//                }
//            }
//        }
        return true;
    }

}
