package com.saemann.gulli.core.model.timeline.array;

/**
 *
 * @author saemann
 */
public class ArrayTimeLinePipe implements TimeLinePipe {

    public final ArrayTimeLinePipeContainer container;

    private final int startIndex;

    protected float v_max = 0, v_mean = 0;
    protected float q_max = 0, q_mean = 0;
    protected float h_max = 0, h_mean = 0;
    protected float mf_max = 0, mf_mean = 0; //massflux [kg/s]
    protected float c_max = 0, c_mean = 0; //concentration [kg/m³]
    private long lastcallTime=Long.MIN_VALUE;
    private float vLastCall;

    public long getTimeMilliseconds(int timeIndex) {
        return container.getTimeMilliseconds(timeIndex);
    }

    private int getIndex(int temporalIndex) {
        return startIndex + temporalIndex;
    }

    private double getIndexDouble(double temporalIndex) {
        return startIndex + temporalIndex;
    }

    public ArrayTimeLinePipe(ArrayTimeLinePipeContainer container, int spatialIndex) {
//        this.spatialIndex = spatialIndex;
        this.container = container;
        this.startIndex = container.getNumberOfTimes() * spatialIndex;
    }

    @Override
    public float getVelocity(int temporalIndex) {
        return container.velocity[getIndex(temporalIndex)];
    }

    private double getValue_DoubleIndex(float[] array, double temporalIndexDouble) {
        double i = getIndexDouble(temporalIndexDouble);
        if (((int) i) >= array.length - 1) {
            return array[array.length - 1];
        }
        double v0 = array[(int) i];
        double v1 = array[(int) i + 1];
        double ratio = i % 1;
        return (v0 + (v1 - v0) * ratio);
    }

    private double getValue_DoubleIndex(float[][] array, double temporalIndexDouble, int secondIndex) {
        double i = getIndexDouble(temporalIndexDouble);
        if (((int) i) >= array.length - 1) {
            return array[array.length - 1][secondIndex];
        }
        double v0 = array[(int) i][secondIndex];
        double v1 = array[(int) i + 1][secondIndex];
        double ratio = i % 1;
        return (v0 + (v1 - v0) * ratio);
    }

    public float getVelocity_DoubleIndex(double temporalIndex) {
        return (float) getValue_DoubleIndex(container.velocity, temporalIndex);
    }

    public float getWaterlevel_DoubleIndex(double temporalIndex) {
        return (float) getValue_DoubleIndex(container.waterlevel, temporalIndex);
    }

    public float getVolume_DoubleIndex(double temporalIndex) {
        return (float) getValue_DoubleIndex(container.volume, temporalIndex);
    }

    public float getDischarge_DoubleIndex(double temporalIndex) {
        return (float) getValue_DoubleIndex(container.discharge, temporalIndex);
    }

    public float getMassFlux_reference_DoubleIndex(double temporalIndex, int materialIndex) {
        return (float) getValue_DoubleIndex(container.massflux_reference, temporalIndex, materialIndex);
    }

    public float getConcentration_reference_DoubleIndex(double temporalIndex, int materialIndex) {
        return (float) getValue_DoubleIndex(container.concentration_reference, temporalIndex,materialIndex);
    }
    
     @Override
    public float getVolume(int temporalIndex) {
        return container.volume[getIndex(temporalIndex)];
    }

    @Override
    public float getWaterlevel(int temporalIndex) {
        return container.waterlevel[getIndex(temporalIndex)];
    }

    public void setVelocity(float value, int temporalIndex) {
        container.velocity[getIndex(temporalIndex)] = value;
    }

    public void setWaterlevel(float value, int temporalIndex) {
        container.waterlevel[getIndex(temporalIndex)] = value;
    }

    public void setVolume(float value, int temporalIndex) {
        container.volume[getIndex(temporalIndex)] = value;
    }

    /**
     * Get volume flow in pipe in [m³/s].
     *
     * @param temporalIndex
     * @return flow [qm/s]
     */
    @Override
    public float getDischarge(int temporalIndex) {
        return container.discharge[getIndex(temporalIndex)];
    }

    public void setDischarge(float value, int temporalIndex) {
        container.discharge[getIndex(temporalIndex)] = value;
    }

    @Override
    public float getMassflux_reference(int temporalIndex, int materialIndex) {
        return container.massflux_reference[getIndex(temporalIndex)][materialIndex];
    }

    public void setMassflux_reference(float value, int temporalIndex, int materialIndex) {

        if (container.massflux_reference == null) {
            synchronized (container) {
                if (container.massflux_reference == null) {
                    container.initMass_Reference();
                }
            }
        }
        container.massflux_reference[getIndex(temporalIndex)][materialIndex] = value;
    }

    @Override
    public float getConcentration_reference(int temporalIndex, int materialIndex) {
        return container.concentration_reference[getIndex(temporalIndex)][materialIndex];
    }

    public void setConcentration_Reference(float value, int temporalIndex, int materialIndex) {

        if (container.concentration_reference == null) {
            synchronized (container) {
                if (container.concentration_reference == null) {
                    container.initConcentration_Reference();
                }
            }
        }
        container.concentration_reference[getIndex(temporalIndex)][materialIndex] = value;
    }

    public void calculateMaxMeanValues() {
        double v_sum = 0;
        v_max = 0;
        double v_min = 0;
        double h_sum = 0;
        h_max = 0;
        double m_sum = 0;
        mf_max = 0;

        boolean seemass = container.massflux_reference != null;
        for (int i = 0; i < container.getNumberOfTimes(); i++) {
            try {
                float velocity = container.velocity[i + startIndex];
                v_sum += velocity;
                v_max = Math.max(v_max, velocity);
                v_min = Math.min(v_min, velocity);
                float lvl = container.waterlevel[i + startIndex];
                h_sum += lvl;
                h_max = Math.max(h_max, lvl);
                if (seemass) {
                    float mass = container.massflux_reference[i + startIndex][0];
                    m_sum += mass;
                    mf_max = Math.max(mf_max, mass);
                }
            } catch (Exception e) {
                System.out.println(" start: " + startIndex + "  + i: " + i + "\t=" + (i + startIndex) + "\t length:" + container.velocity.length);
            }
        }

        v_mean = (float) (v_sum / (double) container.getNumberOfTimes());
        h_mean = (float) (h_sum / (double) container.getNumberOfTimes());
        mf_mean = (float) (m_sum / (double) container.getNumberOfTimes());

        if (Math.abs(v_min) > Math.abs(v_max)) {
            v_max = (float) v_min;
        }
    }

    /**
     * Water level in pipe at the actual timestep set in
     * ArrayTimeLinePipeContainer.
     *
     * @return
     */
    @Override
    public double getWaterlevel() {
        if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
            return this.getWaterlevel(container.getActualTimeIndex());
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
            return this.getWaterlevel_DoubleIndex(container.getActualTimeIndex_double());
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
            return h_max;
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
            return h_mean;
        }
        return this.getWaterlevel(container.getActualTimeIndex());
    }

    /**
     * VOlume flow in pipe at the actual timestep set in
     * ArrayTimeLinePipeContainer.
     *
     * @return
     */
    @Override
    public double getDischarge() {
        if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
            return this.getDischarge(container.getActualTimeIndex());
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
            return this.getDischarge_DoubleIndex(container.getActualTimeIndex_double());
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
            return q_max;
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
            return q_mean;
        }
        return this.getWaterlevel(container.getActualTimeIndex());
    }

    /**
     * Velocity at the actual timestep set in ArrayTimeLinePipeContainer.
     *
     * @return
     */
    @Override
    public float getVelocity() {

        if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.LINEAR_INTERPOLATE) {
            if (container.getActualTime() == lastcallTime) {
                return vLastCall;
            }
            vLastCall = this.getVelocity_DoubleIndex(container.getActualTimeIndex_double());
            lastcallTime = container.getActualTime();
            return vLastCall;
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.STEPS) {
            return this.getVelocity(container.getActualTimeIndex());
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MAXIMUM) {
            return v_max;
        } else if (container.calculaion_Method == ArrayTimeLinePipeContainer.CALCULATION.MEAN) {
            return v_mean;
        }
        throw new IllegalArgumentException("Calculation Method '" + container.calculaion_Method + "' is not yet implemented in " + this.getClass().getSimpleName() + "::getVelocity().");
    }

    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public int getNumberOfTimes() {
        return container.getNumberOfTimes();
    }

    public float getV_max() {
        return v_max;
    }

    public float getV_mean() {
        return v_mean;
    }

    public float getH_max() {
        return h_max;
    }

    public float getH_mean() {
        return h_mean;
    }

    public float getM_max() {
        return mf_max;
    }

    public float getM_mean() {
        return mf_mean;
    }

    @Override
    public TimeContainer getTimeContainer() {
        return container;
    }

    @Override
    public boolean hasMassflux_reference() {
        return container.massflux_reference != null;
    }

    public boolean hasConcentration_reference() {
        return container.concentration_reference != null;
    }

//    @Override
//    public float getConcentration_reference(int temporalIndex) {
//        return container.massflux_reference[startIndex + temporalIndex];
////        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    @Override
    public double getVolume() {
       return  container.volume[getIndex(container.getActualTimeIndex())];
    }

    @Override
    public String[] getMaterialNames() {
        return new String[1];
    }
}
