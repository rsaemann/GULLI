/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.timeline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author saemann
 */
public class RainGauge {

//    protected byte[] daten;
    protected int intervallMinutes;
    protected long beginn, end;
    protected double[] precipitation;
    protected long[] time;

    public RainGauge() {

    }

    public RainGauge(double[] niederschlagshoehe, long[] niederschlagsstarttime) {
        this.precipitation = niederschlagshoehe;
        this.time = niederschlagsstarttime;
        if (niederschlagsstarttime.length > 1) {
            beginn = niederschlagsstarttime[0];
            intervallMinutes = (int) ((niederschlagsstarttime[1] - niederschlagsstarttime[0]) / 60000);
            end = niederschlagsstarttime[niederschlagsstarttime.length - 1] + intervallMinutes * 60000;
        }
    }

    public RainGauge(long startTime, long intervall, double[] precipitation) {
        this.precipitation = precipitation;
        this.time = new long[precipitation.length];
        for (int i = 0; i < time.length; i++) {
            time[i] = startTime + i * intervall;
        }
        this.intervallMinutes = (int) (intervall / 60000);
        beginn = startTime;
        if (time.length < 1) {
            end = beginn;
        } else {
            end = time[time.length - 1] + intervall;
        }
    }

    public int getIntervallMinutes() {
        return intervallMinutes;
    }

    public static RainGauge MirroredCopy(RainGauge rg) {
        double[] preci = new double[rg.precipitation.length];
        for (int i = 0; i < preci.length; i++) {
            preci[i] = rg.precipitation[rg.precipitation.length - 1 - i];
        }
        RainGauge retur = new RainGauge(preci, rg.time);
        return retur;
    }

    /**
     * Decodes the Information of a DATEN field in the Extran.idbf result
     * database.
     *
     * @param byteBlob
     * @return
     */
    public static double[] readPrecipitation(byte[] byteBlob) {
        ByteBuffer bb = ByteBuffer.wrap(byteBlob);
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        int number = bb.getInt(0);
        double[] niederschlagshoehe = new double[number];
        int l = (byteBlob.length - 4) / 16;
        for (int i = 0; i < l; i++) {
            int start = 4 + i * 16;
            double wert = bb.getDouble(start + 8);
            niederschlagshoehe[i] = wert;
        }
        return niederschlagshoehe;
    }

    public double[] getPrecipitation() {
        return precipitation;
    }

    public long[] getTimes() {
        return time;
    }

    /**
     * Reduces the length of entries so that the first and last entry contain
     * precipitation >0.
     */
    public void trim() {
        //search for the first entry with precipitation
        int startindex = -1;
        for (int i = 0; i < precipitation.length; i++) {
            if (precipitation[i] > 0.00001) {
                startindex = i;
                break;
            }
        }
        int endIndex = -1;
        //search for last precipitation index
        for (int i = precipitation.length - 1; i >= 0; i--) {
            if (precipitation[i] > 0.00001) {
                endIndex = i;
                break;
            }
        }
        if (startindex < 0) {
            //Empty raingauge
            precipitation = new double[0];
            time = new long[0];
            this.end = beginn;
        } else {
            int newsize = endIndex - startindex + 1;
            double[] newPrecipitation = new double[newsize];
            long[] newtimes = new long[newsize];
            for (int i = 0; i < newtimes.length; i++) {
                newPrecipitation[i] = precipitation[i + startindex];
                newtimes[i] = time[i + startindex];
            }
            this.precipitation = newPrecipitation;
            this.time = newtimes;
            this.beginn = time[0];
            this.end = time[time.length - 1] + intervallMinutes * 60000;
        }
    }

    public RainGauge copyTrimmed() {
        RainGauge rg = new RainGauge(precipitation, time);
        rg.trim();
        return rg;
    }

    public void setData(double[] niederschlagshoehe, long[] niederschlagsstarttime) {
        this.time = niederschlagsstarttime;
        this.precipitation = niederschlagshoehe;
        if (niederschlagsstarttime.length > 1) {
            beginn = niederschlagsstarttime[0];
            end = niederschlagsstarttime[niederschlagsstarttime.length - 1] + intervallMinutes * 60000;
            intervallMinutes = (int) ((niederschlagsstarttime[1] - niederschlagsstarttime[0]) / 60000);
        }
    }

    /**
     * Create a sublist with elements from this raingauge.
     *
     * @param fromIndex first index to be included
     * @param toIndex last index to be included
     * @return
     */
    public RainGauge subList(int fromIndex, int toIndex) {
        int entryCount = toIndex - fromIndex + 1;
        long[] newTimes = new long[entryCount];
        double[] newPrecipitation = new double[entryCount];
        int index = 0;
        for (int i = fromIndex; i <= toIndex; i++) {
            newPrecipitation[index] = this.precipitation[i];
            newTimes[index] = this.time[i];
            index++;
        }
        RainGauge sub = new RainGauge(newPrecipitation, newTimes);
        sub.intervallMinutes = this.intervallMinutes;
        return sub;
    }

    /**
     * Create sublist with elements from this raingauge.
     *
     * @param from
     * @param to
     * @return
     */
    public RainGauge subList(long from, long to) {
        int fromIndex = 0, toIndex = time.length - 1;
        for (int i = 0; i < time.length; i++) {
            if (time[i] > from) {
                fromIndex = i - 1;
                break;
            }
        }
        for (int i = 0; i < time.length; i++) {
            if (time[i] > to) {
                toIndex = i;
                break;
            }
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        return subList(fromIndex, toIndex);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (int) (this.beginn ^ (this.beginn >>> 32));
        hash = 71 * hash + Arrays.hashCode(this.precipitation);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RainGauge other = (RainGauge) obj;
        if (this.intervallMinutes != other.intervallMinutes) {
            return false;
        }
        if (this.beginn != other.beginn) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if (this.precipitation.length > 0 && other.precipitation.length > 0) {
            if (!Arrays.equals(this.precipitation, other.precipitation)) {
                return false;
            }
        }
        if (time.length > 0 && other.time.length > 0) {
            if (!Arrays.equals(this.time, other.time)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (precipitation == null || time == null || time.length == 0) {
            return getClass().getSimpleName() + "(empty)";
        }
        String hoehen = "";
        if (precipitation != null) {
            DecimalFormat df3 = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));
            for (int i = 0; i < precipitation.length; i++) {
                hoehen += ", " + df3.format(precipitation[i]);

            }
        }
        return getClass().getSimpleName() + "{" + new Date(time[0]) + " - " + new Date(time[time.length - 1] + intervallMinutes * 60000) + ", " + time.length + "x" + intervallMinutes + "min} : " + hoehen;
    }

    public long getBeginn() {
        return beginn;
    }

    
    
    public long getEnd() {
        return end;
    }
    
    

}
