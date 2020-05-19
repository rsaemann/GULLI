package view.timeline;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import control.multievents.PipeResultData;
import io.timeline.TimeSeries_IO;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

/**
 *
 * @author saemann
 */
public class TimelinePanel extends JPanel {

//    /**
//     * if false the Simulationtime will show 1:00 as start hour
//     */
//    public boolean initializeAsGMTtime = true;
    protected ChartPanel panelChart;
    protected TimeSeriesCollection collection;
    protected JCheckBox[] checkboxes;
    protected JPanel panelChecks;
    protected ValueMarker marker;
    protected HashMap<String, Boolean> checks = new HashMap<>(20);

    public boolean showMarkerLabelTime = true;
    protected String title;

    protected DateAxis dateAxis;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected int numberUsedDataSetSlots = 0;

    public static boolean matlabStyle = true;

    protected static String directoryPDFsave = ".";
    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    ArrayList<PipeResultData> container;
    protected BasicStroke stroke0 = new BasicStroke(2);
    protected BasicStroke stroke1 = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{0.1f, 6}, 0);
    protected BasicStroke stroke2 = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 3, 7, 3}, 0);
    protected BasicStroke stroke3 = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{1, 3, 2, 5}, 0);
//    protected BasicStroke stroke5Dot = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{1, Float.POSITIVE_INFINITY}, 0);

    public boolean prepareTimelinesInThread = false;

    public final ArrayList<CollectionChangedListener> collectionListener = new ArrayList<>();

    private Font titleFont = new Font(Font.SERIF, Font.ROMAN_BASELINE, 20);

    public TimelinePanel(String title, boolean showCheckBoxes) {
        super(new BorderLayout());
        this.title = title;
        this.collection = new TimeSeriesCollection();
        if (showCheckBoxes) {
            initCheckboxpanel();
        }
        initChart(title);
        addPDFexport();
        addEMFexport();
        addTimeSeriesExport();
    }

    public void showCheckBoxPanel(boolean showPanel) {
        if (!showPanel) {
            this.remove(panelChecks);
        } else {
            if (panelChecks == null) {
                initCheckboxpanel();
            }
            this.add(panelChecks, BorderLayout.SOUTH);
        }
    }

    /**
     * Returns the time from starttime as 1.1.1970 00:00:00
     *
     * @param actualTime
     * @param startTime
     * @return
     */
    public static long calcSimulationTime(long actualTime, long startTime) {
        long time = actualTime - startTime;
        int offset = TimeZone.getDefault().getOffset(time);
        time -= offset;
        return time;
    }

    public void markTime(long time) {
        if (showMarkerLabelTime == false && marker == null) {
            return;
        }
        if (marker == null) {
            marker = new ValueMarker(time, new Color(50, 150, 250, 100), new BasicStroke(1.5f));
            marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));
            marker.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            ((XYPlot) this.panelChart.getChart().getPlot()).addDomainMarker(marker);
        }
//        System.out.println("  set value "+time/(60000)+"min");
        marker.setValue(time);
        if (showMarkerLabelTime) {
            Date d = new Date(time);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            marker.setLabel(sdf.format(d));
        } else {
            marker.setLabel("");
        }
        if (!showMarkerLabelTime) {
            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
//            System.out.println("removemarker");
            marker = null;
        }
    }

    public void removeMarker() {
        if (marker != null) {
            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
            marker = null;
        }
    }

    public ChartPanel getChartPanel() {
        return panelChart;
    }

    private void initCheckboxpanel() {
        panelChecks = new JPanel();
        this.add(panelChecks, BorderLayout.SOUTH);
    }

    public void updateCheckboxPanel() {
        if (panelChecks == null) {
            return;
        }
        panelChecks.removeAll();
        if (this.collection == null || this.collection.getSeries().isEmpty()) {
            panelChecks.setLayout(new BorderLayout());
            panelChecks.add(new JLabel("No timeseries found."), BorderLayout.CENTER);
            checkboxes = new JCheckBox[0];
            return;
        }
        if (checkboxes == null || checkboxes.length != this.collection.getSeriesCount()) {
            checkboxes = new JCheckBox[this.collection.getSeriesCount()];
        }
        int maxcolumns = 7;
        panelChecks.setLayout(new GridLayout(checkboxes.length / maxcolumns + 1, maxcolumns));
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i] == null || !checkboxes[i].getText().equals(this.collection.getSeriesKey(i).toString())) {
                Boolean shallBeChecked = checks.get(this.collection.getSeriesKey(i).toString());
                if (shallBeChecked == null) {
                    shallBeChecked = false;
                }
                if (((SeriesKey) this.collection.getSeriesKey(i)).isVisible()) {
                    shallBeChecked = true;
                }
                checkboxes[i] = new JCheckBox(this.collection.getSeriesKey(i).toString(), shallBeChecked);
                String containeraddition = "";
                if (((SeriesKey) this.collection.getSeriesKey(i)).containerIndex > 0) {
                    containeraddition = " {" + ((SeriesKey) this.collection.getSeriesKey(i)).containerIndex + "}";
                }
                checkboxes[i].setToolTipText(((SeriesKey) this.collection.getSeriesKey(i)).name + " (" + ((SeriesKey) this.collection.getSeriesKey(i)).symbol + ") [" + ((SeriesKey) this.collection.getSeriesKey(i)).unit + "]" + containeraddition);
                final int index = i;
                checkboxes[i].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        updateShownTimeSeries();
                        JCheckBox c = (JCheckBox) ae.getSource();
                        checks.put(c.getText(), c.isSelected());
                        ((SeriesKey) collection.getSeries(index).getKey()).setVisible(c.isSelected());
                    }
                });
            } else {
                checkboxes[i].setSelected(((SeriesKey) this.collection.getSeriesKey(i)).isVisible());
            }
            panelChecks.add(checkboxes[i], i);
        }
        this.revalidate();
    }

    public void updateShownTimeSeries() {
        if (this.collection == null) {
            return;
        }
        XYPlot plot = panelChart.getChart().getXYPlot();
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            plot.setDataset(i, null);
        }
        plot.clearRangeAxes();
        numberUsedDataSetSlots = 0;
        yAxisMap.clear();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;

        for (int i = 0; i < collection.getSeriesCount(); i++) {
            SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
            /**
             * Baue neues Dataset wenn keine Wiederekennung zu finden ist
             */
            TimeSeriesCollection dataset = null;
            if (key.axisKey == null || key.axisKey.name == null) {
                /*
                 * No recognition (mapping to other dataset) required.
                 * Build a new Dataset+Yaxis for this TimeSeries
                 */
                indexDataset = numberUsedDataSetSlots;
                numberUsedDataSetSlots++;
                dataset = new TimeSeriesCollection(this.collection.getSeries(i));
                plot.setDataset(indexDataset, dataset);
                renderer = new XYLineAndShapeRenderer(true, false);
                plot.setRenderer(indexDataset, renderer);

                NumberAxis axis2 = new NumberAxis(checkboxes[i].getText());
                yAxisMap.put(axis2.getLabel(), indexDataset);
                axis2.setAutoRangeIncludesZero(false);
                plot.setRangeAxis(indexDataset, axis2);
                plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
            } else {
                NumberAxis yAxis;
                if (yAxisMap.containsKey(key.axisKey.name)) {
                    indexDataset = yAxisMap.get(key.axisKey.name);
                    yAxis = (NumberAxis) plot.getRangeAxis(indexDataset);
                    dataset = (TimeSeriesCollection) plot.getDataset(indexDataset);
                    indexSeries = dataset.getSeriesCount();
                    dataset.addSeries(this.collection.getSeries(i));
                    renderer = (XYLineAndShapeRenderer) plot.getRenderer(indexDataset);
                    renderer.setSeriesStroke(indexSeries, key.stroke);
                } else {
                    // Axis key not yet in use. Build new Dataset for this Yaxis
                    indexDataset = numberUsedDataSetSlots;
                    numberUsedDataSetSlots++;
                    yAxisMap.put(key.axisKey.name, indexDataset);
                    indexSeries = 0;
                    if (key.axisKey.label != null) {
                        yAxis = new NumberAxis(key.axisKey.label);
                    } else {
                        yAxis = new NumberAxis("[" + key.unit + "]");
                    }
                    if (key.axisKey != null) {
                        if (key.axisKey.manualBounds) {
                            yAxis.setLowerBound(key.axisKey.lowerBound);
                            yAxis.setUpperBound(key.axisKey.upperBound);
                        } else {
                            key.axisKey.lowerBound = yAxis.getLowerBound();
                            key.axisKey.upperBound = yAxis.getUpperBound();
                        }
                    }
                    yAxisMap.put(yAxis.getLabel(), indexDataset);
                    renderer = new XYLineAndShapeRenderer(true, false);
                    renderer.setSeriesStroke(indexSeries, key.stroke);
                    plot.setRenderer(indexDataset, renderer);

                    yAxis.setAutoRangeIncludesZero(false);

                    plot.setRangeAxis(indexDataset, yAxis);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                    dataset = new TimeSeriesCollection(this.collection.getSeries(i));
                    plot.setDataset(indexDataset, dataset);
                }
                plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
            }
            renderer = (XYLineAndShapeRenderer) plot.getRenderer(indexDataset);
            renderer.setDrawSeriesLineAsPath(true);
            if (key.lineColor != null) {
                renderer.setSeriesPaint(indexSeries, key.lineColor);
            }
            if (key.stroke != null) {
                renderer.setSeriesStroke(indexSeries, key.stroke);
                renderer.setSeriesLinesVisible(indexSeries, true);
            } else {
                renderer.setSeriesLinesVisible(indexSeries, false);
            }
            if (key.shape != null && key.shape.getShape() != null) {
                renderer.setSeriesShape(indexSeries, key.shape.getShape());
                renderer.setSeriesShapesFilled(indexSeries, key.shapeFilled);
                renderer.setSeriesShapesVisible(indexSeries, true);
            } else {
                renderer.setSeriesShape(indexSeries, null);
                renderer.setSeriesShapesVisible(indexSeries, false);
            }
            indexDataset++;
        }

        if (matlabStyle) {
            MatlabLayout.layoutToMatlab(this.panelChart.getChart());
        }
    }

    private void initChart(String title) {
        if (title == null) {
            title = "";
        }
        JFreeChart chart;

        chart = ChartFactory.createTimeSeriesChart(title, "Time", "", collection, true, true, true);

        XYPlot plot = chart.getXYPlot();

        try {
            dateAxis = (DateAxis) plot.getDomainAxis();
        } catch (Exception e) {
        }
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);

        this.panelChart = new ChartPanel(chart) {
            @Override
            public void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
                } catch (Exception e) {
                    System.err.println("Paintexception in Chartpanel catched: " + e.getLocalizedMessage());
                }
            }
        };
        this.add(panelChart, BorderLayout.CENTER);
    }

    private void updateChart(String title) {
        if (title == null) {
            title = "";
        }
        if (panelChart != null && panelChart.getChart() != null) {
            panelChart.getChart().setTitle(title);
            panelChart.getChart().getTitle().setFont(titleFont);
        }
    }

    private void addPDFexport() {
        JPopupMenu menu = this.panelChart.getPopupMenu();
        try {
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    if (m.getActionCommand().equals("Save as")) {
                        JMenuItem item = new JMenuItem("PDF...");
                        m.add(item, 0);
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                JFileChooser fc = new JFileChooser(directoryPDFsave) {
                                    @Override
                                    public boolean accept(File file) {
                                        if (file.isDirectory()) {
                                            return true;
                                        }
                                        if (file.isFile() && file.getName().endsWith(".pdf")) {
                                            return true;
                                        }
                                        return false;
                                    }
                                };
                                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                                int n = fc.showSaveDialog(TimelinePanel.this);
                                if (n == JFileChooser.APPROVE_OPTION) {
                                    File output = fc.getSelectedFile();
                                    directoryPDFsave = output.getParent();
                                    if (!output.getName().endsWith(".pdf")) {
                                        output = new File(output.getAbsolutePath() + ".pdf");
                                    }
                                    Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                                    try {
                                        panelChart.getChart().setBackgroundPaint(Color.white);
                                        Rectangle rec = TimelinePanel.this.getBounds();
                                        Document doc = new Document(new com.itextpdf.text.Rectangle(0, 0, rec.width, rec.height));
                                        FileOutputStream fos = new FileOutputStream(output);
                                        PdfWriter writer = PdfWriter.getInstance(doc, fos);
                                        doc.open();
                                        PdfContentByte cb = writer.getDirectContent();
                                        PdfTemplate tp = cb.createTemplate((float) rec.getWidth(), (float) rec.getHeight());
                                        PdfGraphics2D g2d = new PdfGraphics2D(cb, (float) rec.getWidth(), (float) rec.getHeight());
                                        g2d.translate(-getX(), -getY());
                                        panelChart.getChart().draw(g2d, rec);
                                        cb.addTemplate(tp, 25, 200);
                                        g2d.dispose();
                                        doc.close();
                                        fos.close();

                                    } catch (FileNotFoundException ex) {
                                        Logger.getLogger(TimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } catch (DocumentException ex) {
                                        Logger.getLogger(TimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } catch (IOException ex) {
                                        Logger.getLogger(TimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } finally {

                                    }
                                    panelChart.getChart().setBackgroundPaint(formerBackground);
                                }
                            }
                        });
                    }
                }
            }
        } catch (java.lang.NoClassDefFoundError e) {
            //If itexpPDF libraries are not loaded. do not break creation of this panel.
            System.err.println("Missing libraries for enabling plot pdf export: itextpdf_5.5.1.jar");
            e.printStackTrace();
        }
    }

    private void addEMFexport() {
        try {
            JPopupMenu menu = this.panelChart.getPopupMenu();
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    if (m.getActionCommand().equals("Save as")) {
                        JMenuItem item = new JMenuItem("EMF...");
                        m.add(item, 0);
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                JFileChooser fc = new JFileChooser(directoryPDFsave) {
                                    @Override
                                    public boolean accept(File file) {
                                        if (file.isDirectory()) {
                                            return true;
                                        }
                                        return file.isFile() && file.getName().endsWith(".emf");
                                    }
                                };
                                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                                int n = fc.showSaveDialog(TimelinePanel.this);
                                if (n == JFileChooser.APPROVE_OPTION) {
                                    File output = fc.getSelectedFile();
                                    directoryPDFsave = output.getParent();
                                    if (!output.getName().endsWith(".emf")) {
                                        output = new File(output.getAbsolutePath() + ".emf");
                                    }
                                    Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                                    try {
                                        panelChart.getChart().setBackgroundPaint(Color.white);
                                        try (OutputStream out = new java.io.FileOutputStream(output)) {
                                            Rectangle rec = TimelinePanel.this.getBounds();
                                            int width = rec.width;// * 10;
                                            int height = rec.height;// * 10;
                                            EMFGraphics2D g2d = new EMFGraphics2D(out, new Dimension((int) (width), height));
                                            g2d.setDeviceIndependent(true);
                                            //                                    g2d.writeHeader();
                                            g2d.startExport();
                                            //                                    g2d.writeHeader();
                                            try {
                                                panelChart.getChart().draw(g2d, new Rectangle(width, height));
                                            } catch (Exception e) {
                                                System.err.println("rect:" + width + "x" + height);
                                                System.err.println("g2d:" + g2d);
                                                System.err.println("chart:" + panelChart.getChart());
                                                e.printStackTrace();
                                            }
                                            g2d.endExport();
                                            //                                    g2d.closeStream();
                                            out.flush();

                                        }
                                    } catch (FileNotFoundException ex) {
                                        Logger.getLogger(TimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } catch (IOException ex) {
                                        Logger.getLogger(TimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    }
                                    panelChart.getChart().setBackgroundPaint(formerBackground);
                                }
                            }
                        });
                    }
                }
            }
        } catch (java.lang.NoClassDefFoundError e) {
            //IF library for EMF export is not there, do not break creation of GUI as a whole.
            System.err.println("Missing libraries for plot emf export: freehep-graphicsio-emf-2.1.2.jar,freehep-graphicsio-2.1.2.jar,freehep-graphics2d-2.1.2.jar");
            e.printStackTrace();
        }
    }

    private void addTimeSeriesExport() {
        JPopupMenu menu = this.panelChart.getPopupMenu();
        for (int i = 0; i < menu.getComponentCount(); i++) {
            if (menu.getComponent(i) instanceof JMenu) {
                JMenu m = (JMenu) menu.getComponent(i);
                if (m.getActionCommand().equals("Save as")) {
                    JMenuItem item = new JMenuItem("Series...");
                    m.add(item, 0);
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            JFileChooser fc = new JFileChooser(directoryPDFsave) {

                                @Override
                                public boolean accept(File file) {
                                    if (file.isDirectory()) {
                                        return true;
                                    }
                                    return false;
                                }
                            };
                            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            int n = fc.showSaveDialog(TimelinePanel.this);
                            if (n == JFileChooser.APPROVE_OPTION) {
                                File output = fc.getSelectedFile();
                                directoryPDFsave = output.getAbsolutePath();
                                File output2 = new File(output.getAbsolutePath());
                                try {
                                    String prefix = "";

                                    TimeSeries_IO.saveTimeSeriesCollection(output2, prefix, collection);

                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(TimelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(TimelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public static TimeSeries createMovingaverageCentral(TimeSeries ts, int maxinvolvedPeriods, String name, boolean originIsShiftTimeSeries) {
        SeriesKey oldKey = (SeriesKey) ts.getKey();
        Color colorNew = null;
        if (oldKey.lineColor != null) {
            colorNew = new Color(oldKey.lineColor.getRGB() * 300000);
        }
        return createMovingaverageCentral(ts, maxinvolvedPeriods, name, colorNew, originIsShiftTimeSeries);
    }

    public static TimeSeries createMovingaverageCentral(TimeSeries ts, int maxinvolvedPeriods, String name, Color c, boolean originIsShiftTimeSeries) {
        SeriesKey oldKey = (SeriesKey) ts.getKey();
        Color colorNew = c;
        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, maxinvolvedPeriods + " mean " + oldKey.symbol, oldKey.unit, colorNew, oldKey.axisKey);
        TimeSeries average = new TimeSeries(newKey);
        if (!originIsShiftTimeSeries) {
            int minIndex = maxinvolvedPeriods / 2 + 1;
            int maxIndex = ts.getItemCount() - maxinvolvedPeriods / 2;
            int radius = maxinvolvedPeriods / 2;
            double nenner = (2. * radius + 1.);
            for (int i = minIndex; i < maxIndex; i++) {
                double sum = 0;
                RegularTimePeriod p = ts.getTimePeriod(i);
                for (int j = i - radius; j < i + radius; j++) {
                    sum += ts.getDataItem(j).getValue().doubleValue();

                }
                double wert = sum / nenner;
                average.add(p, wert);
            }
        } else {
            int minIndex = maxinvolvedPeriods + 1;
            int maxIndex = ts.getItemCount() - maxinvolvedPeriods;
            int radius = maxinvolvedPeriods;
            double nenner = (radius + 1.);
            for (int i = minIndex; i < maxIndex; i = i + 2) {
                double sum = 0;
                RegularTimePeriod p = ts.getTimePeriod(i);
                for (int j = i - radius; j < i + radius; j = j + 2) {
                    sum += ts.getDataItem(j).getValue().doubleValue();
                }
                double wert = sum / nenner;
                average.add(p, wert);
            }
        }
        return average;
    }

    public TimeSeriesCollection getCollection() {
        return collection;
    }

    public static void main1(String[] args) {
        Color old = Color.white;

        System.out.println("Old:   " + old);
        System.out.println("10x:   " + new Color(old.getRGB() * 10));
        System.out.println("100x:  " + new Color(old.getRGB() * 100));
        System.out.println("1000:  " + new Color(old.getRGB() * 1000));
        System.out.println("10000: " + new Color(old.getRGB() * 10000));
        System.out.println("100000:" + new Color(old.getRGB() * 100000));
        System.out.println("1000000:" + new Color(old.getRGB() * 1000000));
    }

}
