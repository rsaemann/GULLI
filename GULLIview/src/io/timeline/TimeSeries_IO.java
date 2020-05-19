package io.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import view.timeline.AxisKey;
import view.timeline.SeriesKey;
import view.timeline.customCell.ShapeEditor.SHAPES;

/**
 * Load an Save Timeseries for JFreeChart Panel. Used in the TimelinePanel to
 * save timeseries of different scenarios.
 *
 * @author saemann
 */
public class TimeSeries_IO {

    public static void saveTimeSeries(File file, TimeSeries series) throws FileNotFoundException, IOException {
        if (!(series.getKey() instanceof SeriesKey)) {
            throw new IllegalArgumentException("TimeSeries '" + series.getKey() + "' has no SeriesKey. Can not be saved to '" + file.getAbsolutePath() + "'.");
        }
        SeriesKey key = (SeriesKey) series.getKey();
        OutputStream os = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(key.toString() + "\n");
        bw.write("name ;" + key.name + "\n");
        bw.write("symbl;" + key.symbol + "\n");
        bw.write("unit ;" + key.unit + "\n");
        bw.write("label;" + key.label + "\n");
        if (key.axisKey == null) {
            bw.write("axis ;" + key.symbol + ";\n");
        } else {
            bw.write("axis ;" + key.axisKey.name + ";" + key.axisKey.label + "\n");
        }
        bw.write("index;" + key.containerIndex + "\n");
        bw.write("show ;" + key.isVisible() + "\n");
        bw.write("color;" + key.lineColor + ";" + key.lineColor.getRGB() + "\n");
        bw.write("strok;");
        if (key.stroke == null) {
            bw.write("null\n");
        } else {
            bw.write(key.stroke.getLineWidth() + ";" + key.stroke.getEndCap() + ";" + key.stroke.getLineJoin() + ";" + key.stroke.getMiterLimit() + ";{");
            if (key.stroke.getDashArray() != null) {
                for (int i = 0; i < key.stroke.getDashArray().length; i++) {
                    if (i > 0) {
                        bw.write(",");
                    }
                    bw.write(key.stroke.getDashArray()[i] + "");
                }
            }
            bw.write("};" + key.stroke.getDashPhase() + "\n");
        }
        bw.write("shape;");
        if (key.shape == null) {
            bw.write(SHAPES.EMPTY + "\n");
        } else {
            bw.write(key.shape.name() + "\n");
        }
        bw.write("value;" + series.getItemCount() + "\n");
        for (int i = 0; i < series.getItemCount(); i++) {
            TimeSeriesDataItem item = series.getDataItem(i);
            bw.write(item.getPeriod() + ";" + item.getPeriod().getFirstMillisecond() + ";" + item.getValue() + "\n");
        }
        bw.write("end");
        bw.flush();
        bw.close();
        osw.close();
        os.close();
    }

    public static void saveTimeSeriesCollection(File directory, String nameprefix, TimeSeriesCollection collection) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String dir = directory.getAbsolutePath() + File.separator;
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            try {
                TimeSeries s = collection.getSeries(i);
                SeriesKey key = (SeriesKey) s.getKey();
                File file;
                if (key.file == null || key.file.isEmpty() || !key.file.endsWith("tse")) {
                    file = new File(dir + nameprefix + (key.name + "_" + key.containerIndex).replaceAll("/", "") + ".tse");
                } else {
                    file = new File(dir + key.file);
                }

                saveTimeSeries(file, s);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public static TimeSeries readTimeSeries(File tseFile) throws FileNotFoundException, IOException {

        String name, symbol, unit;
        FileInputStream fis = new FileInputStream(tseFile);
        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        String line = "";
        //Keystring can be dumped
        br.readLine();
        //Every header is 6 digits long
        int head = 6;
        //2.Row: name
        name = br.readLine().substring(head);
        //3.Row: symbol
        symbol = br.readLine().substring(head);
        // uni
        unit = br.readLine().substring(head);
        String label = br.readLine().substring(head);
        String[] axis = br.readLine().substring(head).split(";");
        int index = Integer.parseInt(br.readLine().substring(head));
        boolean visible = Boolean.parseBoolean(br.readLine().substring(head));
        String colorstring = br.readLine().substring(head);
        String strokeString = br.readLine().substring(head);
        String shapeString = null;
        while (br.ready()) {
            line = br.readLine();
            if (line.startsWith("shape")) {
                shapeString = line.substring(head);
            } else if (line.startsWith("value")) {
                //Dump
                break;
            }
        }

        //Decode Axis Key
        AxisKey axisKey = new AxisKey(axis[0]);
        if (axis.length > 1 && axis[1] != null && !axis[1].isEmpty() && !axis[1].equals("null")) {
            axisKey.label = axis[1];
        }

        SeriesKey key = new SeriesKey(name, symbol, unit, Color.black, axisKey, index);
        key.setVisible(visible);
        key.label = label;

        TimeSeries ts = new TimeSeries(key);

        //Read listed values.
        String[] lines;
        DateFormat df = new SimpleDateFormat();
        while (br.ready()) {
            line = br.readLine();
            if (line.startsWith("end")) {
                break;
            }
            lines = line.split(";");
            try {
                Date date = new Date(Long.parseLong(lines[1]));
                RegularTimePeriod t = new Millisecond(date);
                double v = Double.parseDouble(lines[2]);
                ts.addOrUpdate(t, v);
            } catch (Exception exception) {
                System.out.println("++++" + exception.getLocalizedMessage());
                System.out.println("line    :'" + line + "'");
                System.out.println("lines[0]:'" + lines[0] + "'");
                System.out.println("lines[1]:'" + lines[1] + "'");
                System.out.println("lines[2]:'" + lines[2] + "'");
                exception.printStackTrace();
                break;
            }
        }

        //decode Color
        String rgbstring = colorstring.split(";")[1];
        Color color = new Color(Integer.parseInt(rgbstring));
        key.lineColor = color;

        //decode stroke
        BasicStroke stroke;

        String[] s = strokeString.split(";");
        //dasharray [4]
        if (s.length >= 5) {
            if (s[4].length() < 3) {
                //Create Stroke without dasharray
                stroke = new BasicStroke(Float.parseFloat(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Float.parseFloat(s[3]));
            } else {
                //{} abschneiden
                String s4 = s[4];
                s4 = s4.substring(1, s4.length() - 2);
                String[] dashs = s4.split(",");
                float[] dasharray = new float[dashs.length];
                for (int i = 0; i < dashs.length; i++) {
                    if (dashs[i].equals("Infinit")) {
                        dasharray[i] = Float.POSITIVE_INFINITY;
                    } else {
                        dasharray[i] = Float.parseFloat(dashs[i]);
                    }
                }
                stroke = new BasicStroke(Float.parseFloat(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Float.parseFloat(s[3]), dasharray, Float.parseFloat(s[5]));
            }
            key.stroke = stroke;
        } else {
            key.stroke = null;
        }
        //Shape
        try {
            if (shapeString != null) {
                SHAPES shp = SHAPES.valueOf(shapeString);
                key.shape = shp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        key.file = tseFile.getName();
        return ts;
    }

    public static void saveTimeSeriesAsMatlabFigure(TimeSeries series, File outputfile, String capacity_name) throws IOException {
        if (!(series.getKey() instanceof SeriesKey)) {
            throw new IllegalArgumentException("TimeSeries '" + series.getKey() + "' has no SeriesKey. Can not be saved to '" + outputfile.getAbsolutePath() + "'.");
        }
        SeriesKey key = (SeriesKey) series.getKey();
        OutputStream os = new FileOutputStream(outputfile);
        OutputStreamWriter osw = new OutputStreamWriter(os, Charset.forName("ASCII"));
        BufferedWriter bw = new BufferedWriter(osw);
        if (capacity_name != null) {
            bw.write("% Plot Timeseries '" + key.name + "' in '" + capacity_name + "'\n");
        } else {
            bw.write("% Plot Timeseries '" + key.name + "'\n");
        }
        bw.write("% Label '" + key.label + "'\n");
        bw.write("% File '" + key.file + "'\n");
        bw.write("% Created " + new Date().toLocaleString() + " with GULLI\n");

        bw.write("figure(1)\n");

        StringBuilder stbX = new StringBuilder("x=[");
        StringBuilder stbY = new StringBuilder("y=[");
        for (int i = 0; i < series.getItemCount(); i++) {
            TimeSeriesDataItem item = series.getDataItem(i);
            if (i > 0) {
                stbX.append(",");
                stbY.append(",");
            }
            stbX.append((item.getPeriod().getFirstMillisecond() - series.getDataItem(0).getPeriod().getFirstMillisecond()) / 60000.);
            stbY.append(item.getValue().doubleValue());
        }
        stbX.append("];");
        stbY.append("];");
        bw.write(stbX + "\n");
        bw.write(stbY + "\n");
        
        String name=key.label.replaceAll("_", "\\\\_").replaceAll("³", "^3").replaceAll("²", "^2");

        bw.write("plot(x,y,'b-','DisplayName','"+name+"');\n");

       // bw.write("legend('" + key.label.replaceAll("_", "\\\\_").replaceAll("³", "^3").replaceAll("²", "^2") + "')\n");
        bw.write("xlabel('Time [min]');\n");
        bw.write("ylabel('" + key.unit.replaceAll("³", "^3") + "');\n");
         bw.write("legend SHOW\n");

        if (capacity_name != null) {
            bw.write("title('" + key.name + " in " + capacity_name + "');\n");
        } else {
            bw.write("title('" + key.name + "');\n");
        }

        bw.flush();
        bw.close();
        osw.close();
        os.close();
    }

    public static void saveTimeSeriesCollectionAsMatlab(File directory, String nameprefix, TimeSeriesCollection collection, String capacityName, boolean onlyVisible) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String dir = directory.getAbsolutePath() + File.separator;
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            try {
                TimeSeries s = collection.getSeries(i);
                SeriesKey key = (SeriesKey) s.getKey();
                if (onlyVisible && !key.isVisible()) {
                    System.out.println("Skip export of non-visible series " + key.toString());
                    continue;
                }
                File file;
                if (key.file == null || key.file.isEmpty() || !key.file.endsWith(".m")) {
                    file = new File(dir + (nameprefix + key.name.replaceAll("/", "").replaceAll("\\.", "").replaceAll(" ", "") + ".m").replaceAll("-", ""));
                } else {
                    file = new File(dir + key.file);
                }

                saveTimeSeriesAsMatlabFigure(s, file, capacityName);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     *
     * @param file
     * @param dataset
     * @param x_axisLabel
     * @param xIsString if true, xa xis values are printed out in ''
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void saveDatasetAsMatlab(File file, DefaultBoxAndWhiskerCategoryDataset dataset, String x_axisLabel, boolean xIsString) throws FileNotFoundException, IOException {
        DecimalFormat df4=new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));
        OutputStream os = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(os, Charset.forName("ASCII"));
        BufferedWriter bw = new BufferedWriter(osw);

        bw.write("% Plot Dataset \n");

        bw.write("% Created " + new Date().toLocaleString() + " with GULLI\n");

        bw.write("figure(1)\n");
        bw.write("hold off\n\n");
        
        bw.flush();
        for (int r = 0; r < dataset.getRowCount(); r++) {
            bw.append("%% ").append(dataset.getRowKey(r).toString()).append("\n");
            StringBuilder stbX = new StringBuilder("x=[");
            StringBuilder stbY = new StringBuilder("y=[");

            for (int i = 0; i < dataset.getColumnCount(); i++) {
                BoxAndWhiskerItem item = dataset.getItem(r, i);
                if (i > 0) {
                    stbX.append(",");
                    stbY.append(",");
                }
                if (xIsString) {
                    stbX.append("\'").append(dataset.getColumnKey(i)).append("\'");
                } else {
                    try {
                        stbX.append(df4.format(Double.parseDouble((String) dataset.getColumnKey(i))));
                    } catch (Exception e) {
                        e.printStackTrace();
                        xIsString=true;
                        stbX.append("\'").append(dataset.getColumnKey(i)).append("\'");
                    }
                }
                
                if (item == null) {
                    System.out.println("Item [" + r + "," + i + "] is null");
                    stbY.append("NaN");
                } else {
                    stbY.append(df4.format(item.getMedian()));
                }
            }
            stbX.append("];");
            stbY.append("];");
            bw.write(stbX + "\n");
            bw.write(stbY + "\n");

            bw.write("plot(x,y,'x-','DisplayName','" + dataset.getRowKey(r).toString().replaceAll("_", "\\\\_").replaceAll("³", "^3").replaceAll("²", "^2") + "');\n");

            bw.write("xlabel('" + x_axisLabel + "');\n");
            if(r==0){
                bw.write("hold on;\n");
            }

            bw.newLine();
            bw.newLine();
            bw.flush();
        }
        bw.write("title('Median');\n");
        bw.write("legend SHOW");

        bw.flush();
        bw.close();
        osw.close();
        os.close();

    }
}
