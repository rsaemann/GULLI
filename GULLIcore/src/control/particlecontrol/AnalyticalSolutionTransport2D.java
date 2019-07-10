/*
 * The MIT License
 *
 * Copyright 2018 riss.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package control.particlecontrol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import model.surface.measurement.TriangleMeasurement;
import model.surface.Surface;

/**
 *
 * @author riss
 */
public class AnalyticalSolutionTransport2D {

    private double[][] Ca; // pollutant concentration from analytical simulation
    private DiffusionCalculator2D D; // Diffusioncalculator for numeric and analytic simulation

    public AnalyticalSolutionTransport2D() {

    }

    public double[][] calculateAnaSol2D(int mass, double kst) throws IOException, DiffusionCalculator2D.NoDiffusionStringException {
        //as in Pathirana (2011) : 2d pollutant transport
        // M/Lz is exchanged with number of particles

        //lade aktuelle numcompare ein:
        File fileI = new File("S:\\Riss_Masterarbeit\\ContaminationNum2D_t180_p1000.dat");

        if (!fileI.exists()) {
            System.err.println("die Vergleichsdatei wurde nicht gefunden!");
        }

        FileReader fr = new FileReader(fileI);
        BufferedReader br = new BufferedReader(fr);

        // hier könnte noch, wenn in der .dat Datei die erste Zeile durch die Länge des Vektors ausgetauscht wird 
        // eine Größenanpassung der NodeNeighbour floats eingefügt werden (wie in SurfaceIO Z. 281).
        int times = 0;
        int trinum = 182;
        double[] xwerte = new double[trinum];
        int length;
        String[] val;

        while (br.ready()) {

                String string = br.readLine();
            val = string.split(" ");
            times = val.length - 2;
            xwerte[0] = Double.parseDouble(val[0]);

            for (int i = 0; i < trinum - 1; i++) {

                String stringganz = br.readLine();
                String[] value = stringganz.split(" ");
                xwerte[i] = Double.parseDouble(value[0]);
            }
        }

        double x_0 = xwerte[0];
        double y_0 = 0;
        double[] x = xwerte;
        double y = 0;
        double[] t = new double[times];
        t[0] = 0;
        double t_0 = 0;
        double v_x = 0.05;
        float v_y = 0;
        double h = 1;

        D = new DiffusionCalculator2D();
        double[] Diff = D.calculateDiffusion((float) v_x, v_y, h, kst);

        for (int k = 1; k < t.length; k++) {
            if (times == 179) {
                t[k] = t[k - 1] + 60;
            } else {
                t[k] = t[k - 1] + 600;
            }
        }

        Ca = new double[x.length][t.length];

        double[] powx = new double[x.length];
        double[] powy = new double[x.length];
        double numpart = (double) mass;

        for (int i = 0; i < x.length; i++) {
            for (int time = 0; time < t.length; time++) {
                powx[i] = (x[i] - x_0 - v_x * (t[time] - t_0));
                powy[i] = (y - y_0 - v_y * (t[time] - t_0));
                Ca[i][time] = (numpart / (4 * Math.PI * (t[time] - t_0) * Math.sqrt(Diff[0] * Diff[1]))) * Math.exp(-(Math.pow(powx[i], 2)) / (4 * Diff[0] * (t[time] - t_0)) - (Math.pow(powy[i], 2)) / ((4 * Diff[1] * (t[time] - t_0))));
            }
        }
        return Ca;
    }

    public void writeContamination(File file, HashMap<Integer, TriangleMeasurement> tri, Surface surface) throws IOException {

        //get Triangle IDs from Measurment
        double[][] trimids = surface.getTriangleMids();
        Set<Integer> intSet = tri.keySet();
        int[] triID = new int[intSet.size()];               // int[] triIDs of TriangleMeasurement
        int index = 0;
        for (Integer i : intSet) {
            triID[index++] = i;
        }

        //get UTM coordinates (x-direction) from trianglemids
        Double[] xwerte = new Double[triID.length];
        for (int t = 0; t < triID.length; t++) {
            xwerte[t] = trimids[triID[t]][0];
        }

        // sorting index for triangle coordinates
        ArrayIndexComparator comparator = new ArrayIndexComparator(xwerte);
        Integer[] Indexes = comparator.createIndexArray();
        Arrays.sort(Indexes, comparator);

        int[] triIdexesSorted = new int[Indexes.length];
        double[] xwertesort = new double[Indexes.length];
        int index2 = 0;
        int index3 = 0;

        for (Integer i : Indexes) {
            triIdexesSorted[index2++] = i;                      // order of Indices for TriangleMeasurement
            xwertesort[index3++] = xwerte[i];                   // double[] xwertesorted of TriangleMeasurement
        }

        //get the particlecounts/masses/areas in right order
        int times = tri.get(triID[0]).getTimes().getNumberOfTimes();
        int[][] partcount = new int[triID.length][times];

        int[][] pc = new int[1][times];
        double[][] mass = new double[1][times];
        double[][] masscount = new double[triID.length][times];
        double[] area = new double[triID.length];
        for (int i = 0; i < triID.length; i++) {

            pc = tri.get(triID[triIdexesSorted[i]]).getParticlecount();
            mass = tri.get(triID[triIdexesSorted[i]]).getMass();
            area[i] = surface.calcTriangleArea(triID[triIdexesSorted[i]]);

            for (int j = 0; j < times; j++) {

                partcount[i][j] = pc[0][j];
                masscount[i][j] = mass[0][j];
            }
        }
        // write particlecounts in txt file: (ID, 1:times particlecounts)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {

            int zähler = 0;
            for (int j = 0; j < triID.length; j++) {
                zähler += 1;
                bw.write(Double.toString(xwertesort[j]) + " " + area[j] + " ");
                for (int i = 0; i < times; i++) {
                    bw.write(Double.toString(masscount[j][i]) + " ");
                }

                bw.newLine();
                if (zähler == 500) {
                    bw.flush();
                    //System.out.println("5000");
                    zähler = 0;
                }
            }
            bw.flush();
            bw.close();

        }
    }

    public class ArrayIndexComparator implements Comparator<Integer> {

        private final Double[] array;

        public ArrayIndexComparator(Double[] array) {
            this.array = array;
        }

        public Integer[] createIndexArray() {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++) {
                indexes[i] = i; // Autoboxing
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2) {
            // Autounbox from Integer to int to use as array indexes
            return array[index1].compareTo(array[index2]);
        }
    }
}
