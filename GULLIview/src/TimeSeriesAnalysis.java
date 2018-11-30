

import io.timeline.TimeSeries_IO;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import view.timeline.SeriesKey;


/**
 *
 * @author saemann
 */
public class TimeSeriesAnalysis {

    public static double calculateRMSE(TimeSeries t0, TimeSeries t1) {
        double summe = 0;
        if (t0.getItemCount() != t1.getItemCount()) {
            System.out.println("Timeseries do NOT have the same number of DataItems. " + t1.getKey() + "(" + t1.getItemCount() + ") / " + t0.getKey() + "(" + t0.getItemCount() + ")");
        }

        for (int i = 0; i < t0.getItemCount(); i++) {
            TimeSeriesDataItem d0 = t0.getDataItem(i);
            TimeSeriesDataItem d1 = t1.getDataItem(i);
            double diff = d0.getValue().doubleValue() - d1.getValue().doubleValue();
            summe += diff * diff;
        }

        return Math.sqrt(summe / (double) t0.getItemCount());
    }

    public static String RMSE_sameType(File directory, String filenamecontainedString) {
        LinkedList<File> fileList = new LinkedList<>();
        for (File df : directory.listFiles()) {
            if (df.isDirectory()) {
                for (File ff : df.listFiles()) {
                    if (ff.isFile() && ff.getName().contains(filenamecontainedString)) {
                        fileList.add(ff);
                    }
                }
            } else if (df.getName().contains(filenamecontainedString)) {
                fileList.add(df);
            }
        }

        File refFile = fileList.removeFirst();
        File[] ffs = fileList.toArray(new File[fileList.size()]);

        return RMSE_sameType(refFile, ffs);
    }

    public static String RMSE_sameType(TimeSeries ref, TimeSeries[] tss) {
        StringBuilder str = new StringBuilder();

        System.out.println("ts=zeros(" + tss.length + ",1);");
        str.append("ts=zeros(" + tss.length + ",1);hold off\n");
        int i = 1;
        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (TimeSeries ts : tss) {
            double rmse = calculateRMSE(ref, ts);
            sum += rmse;
            System.out.println("%" + ts.getKey() + "Particles \t RMSE= " + rmse);
            System.out.println("ts(" + i + ",1)=" + rmse + ";");
            str.append("ts(" + i + ",1)=" + rmse + ";\n");
            if (rmse > max) {
                max = rmse;
            }
            i++;
        }
        double mean = sum / (double) tss.length;
        System.out.println("");
        System.out.println("plot(ts(:,1),'o-b');");
        System.out.println("Max  = " + max);
        System.out.println("Mean = " + mean);

        str.append("plot(ts(:,1),'o-b');");
        str.append("hold on;\nplot([1," + tss.length + "],[" + mean + "," + mean + "],'-k');");
        SeriesKey key = (SeriesKey) ref.getKey();
        String title = "";
        if (key.file != null && !key.file.isEmpty()) {
            title = key.file;
        } else {
            title = key.label;
        }
        title = title.replaceAll("_", "\\\\_");
        str.append("title('" + title + "');");

        return str.toString();
    }

    public static String RMSE_sameType(File refFile, File[] files) {
        try {
            TimeSeries ref = TimeSeries_IO.readTimeSeries(refFile);
            TimeSeries[] tss = new TimeSeries[files.length];
            for (int i = 0; i < tss.length; i++) {
                tss[i] = TimeSeries_IO.readTimeSeries(files[i]);
            }
            return RMSE_sameType(ref, tss);
        } catch (IOException ex) {
            Logger.getLogger(TimeSeriesAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String RMSE_varyingNumberOfPArticles(File directory) {
        StringBuilder str = new StringBuilder();
        LinkedList<TimeSeries> timeseries = new LinkedList<>();
        int largestnumberofparticles = 0;
        TimeSeries refSeries = null;
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            if (file.getName().contains("Masse Messung") && !file.getName().contains("100")) {
                int numberofparticles = Integer.parseInt(file.getName().substring(0, file.getName().indexOf("P")));
                try {
                    TimeSeries ts = TimeSeries_IO.readTimeSeries(file);
                    SeriesKey key = (SeriesKey) ts.getKey();
                    key.label = numberofparticles + "";
                    key.containerIndex = numberofparticles;
                    timeseries.add(ts);
                    if (numberofparticles > largestnumberofparticles) {
                        largestnumberofparticles = numberofparticles;
                        refSeries = ts;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TimeSeriesAnalysis.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                continue;
            }
        }

        //Liste Sortieren
        Collections.sort(timeseries, new Comparator<TimeSeries>() {

            @Override
            public int compare(TimeSeries t, TimeSeries t1) {
                SeriesKey k0 = (SeriesKey) t.getKey();
                SeriesKey k1 = (SeriesKey) t1.getKey();
                return k0.containerIndex - k1.containerIndex;
            }
        });

        System.out.println("Begin RMSE calculation. Use reference timeseries of " + largestnumberofparticles + " particles.");
        System.out.println(":");
        System.out.println("ts=zeros(" + timeseries.size() + ",2);");
        str.append("ts=zeros(" + timeseries.size() + ",2);\n");
        int i = 1;
        for (TimeSeries t1 : timeseries) {
            double rmse = TimeSeriesAnalysis.calculateRMSE(refSeries, t1);
            System.out.println("%" + t1.getKey() + "Particles \t RMSE= " + rmse);
            System.out.println("ts(" + i + ",1)=" + t1.getKey() + ";");
            System.out.println("ts(" + i + ",2)=" + rmse + ";");
            str.append("ts(" + i + ",1)=" + t1.getKey() + ";\nts(" + i + ",2)=" + rmse + ";\n");
            i++;
        }
        System.out.println("");
        System.out.println("semilogx(ts(:,1),ts(:,2),'o-b');");
        str.append("semilogx(ts(:,1),ts(:,2),'o-b');");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str.toString()), null);
        return str.toString();
    }

    public static String RMSE_againstRef(File fileRef, File directory, String namecontainment) {
        LinkedList<File> fileList = new LinkedList<>();
        for (File df : directory.listFiles()) {
            if (df.isDirectory()) {
                for (File ff : df.listFiles()) {
                    if (ff.isFile() && ff.getName().contains(namecontainment)) {
                        fileList.add(ff);
                    }
                }
            } else if (df.getName().contains(namecontainment)) {
                fileList.add(df);
            }
        }
        File[] ffs = fileList.toArray(new File[fileList.size()]);
        return RMSE_sameType(fileRef, ffs);
    }
}
