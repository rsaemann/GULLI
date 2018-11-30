/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.extran;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import model.timeline.RainGauge;

/**
 *
 * @author saemann
 */
public class Raingauge_Firebird extends RainGauge {

    protected int id;
    protected byte[] daten;
//    int intervallMinutes;
//    long beginn, end;
    protected boolean modellregen;
    protected String name, comment;

//    public static final long _2017010100 = 636188256000000000L + 1000L * 60L * 60L * 10000L;
//    public static final GregorianCalendar c2017010100 = new GregorianCalendar(2017, 0, 1, 0, 0, 0);

    /*
     Represents the epoche-milliseconds used in the HYSTEM EXTRAN Database to
     represent JAVA's Date(0) = 1st January 1970 0:0:0.000
     */
    public static final long _19700101 = 621355968000000000L;

    public Raingauge_Firebird(int id, byte[] daten, int intervallMinutes, long beginn, long ende, boolean modellregen, String name, String comment) {
        this.id = id;
        this.daten = daten;
        this.intervallMinutes = intervallMinutes;
        this.beginn = beginn;
        this.end = ende;
        this.modellregen = modellregen;
        this.name = name;
        this.comment = comment;

        if (daten != null) {
            this.time = readTimestamps(daten);
            this.precipitation = readPrecipitation(daten);
        }
    }

    public int getId() {
        return id;
    }

    public byte[] getDaten() {
        return daten;
    }

    public long getBeginn() {
        return beginn;
    }

    public long getEnde() {
        return end;
    }

    public boolean isModellregen() {
        return modellregen;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public static long byteToDate(long t) {
//        long time = t - _2017010100;
//        return c2017010100.getTimeInMillis() + time / 10000L;
        long time = t - _19700101;
        return time / 10000L;
    }

    public static long dateToByte(long t) {
//        long time = (t - c2017010100.getTimeInMillis());
//        return time * 10000L + _2017010100;   
//        System.out.println("convert long "+t+" to byte ");
//        long time = (t - c19700101.getTimeInMillis());
        return t * 10000L + _19700101;

    }

    @Override
    public String toString() {
        if (precipitation == null || time == null) {
            convertByteTofloat();
        }
        Raingauge_Firebird rr = this;
        System.out.println(rr.getName() + "\t ab " + new Date(rr.getBeginn()) + " in " + rr.getIntervallMinutes() + "min Intervall");
        rr.convertByteTofloat();
        for (int i = 0; i < rr.getPrecipitation().length; i++) {
            System.out.println(new Date(rr.getTimes()[i]) + "\t " + rr.getPrecipitation()[i]);
        }
        System.out.println("\tbis " + new Date(rr.getEnde()));
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }

    public static byte[] convertPrecipitationToByte(double[] n, long[] t) {
        ByteBuffer bb = ByteBuffer.allocate(n.length * 16 + 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(0, n.length);
        for (int i = 0; i < n.length; i++) {
            int index = 4 + i * 16;
            bb.putLong(index, dateToByte(t[i]));
            index += 8;
            bb.putDouble(index, n[i]);
        }

        return bb.array();
    }

    public static long[] readTimestamps(byte[] byteBlob) {
        ByteBuffer bb = ByteBuffer.wrap(byteBlob);
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        int number = bb.getInt(0);
        long[] time = new long[number];
//            System.out.println("number=" + number);
        int l = (byteBlob.length - 4) / 16;
        for (int i = 0; i < l; i++) {
            int start = 4 + i * 16;
            long byteLong = bb.getLong(start);
            long dateTime = byteToDate(byteLong);
            time[i] = dateTime;

        }
        return time;
    }

    public static long LocalToGMT(long localtime) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(localtime);
        time.add(Calendar.MILLISECOND, +time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTimeInMillis();
    }

    public static long GMTtoLocal(long gmttime) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(gmttime);
        time.add(Calendar.MILLISECOND, -time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTimeInMillis();
    }

    public void convertByteTofloat() {
        ByteBuffer bb = ByteBuffer.wrap(daten);
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        int number = bb.getInt(0);
        precipitation = new double[number];
        time = new long[number];
        int l = (daten.length - 4) / 16;
        for (int i = 0; i < l; i++) {
            int start = 4 + i * 16;
            double wert = bb.getDouble(start + 8);
            precipitation[i] = wert;
            long byteLong = bb.getLong(start);
            long dateTime = byteToDate(byteLong);
            time[i] = dateTime;
        }
    }
}
