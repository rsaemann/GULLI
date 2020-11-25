package com.saemann.gulli.view.view.timeline;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.CapacitySelectionListener;
import com.saemann.gulli.view.org.freehep.graphicsio.emf.EMFGraphics2DX;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelinePipe;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;
import static com.saemann.gulli.view.view.timeline.CapacityTimelinePanel.matlabStyle;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

/**
 *
 * @author saemann
 */
public class SpacelinePanel extends JPanel implements CapacitySelectionListener {

    protected JPanel panelNorth;
    protected JSlider slider;
    protected JSplitPane splitpane;
    protected ChartPanel panelChart;
    protected JPanel panelChartContainer;
    protected XYSeriesCollection collection;
    protected JCheckBox[] checkboxes;
    protected JPanel panelChecks;
    protected ValueMarker marker;
    protected HashMap<String, Boolean> checks = new HashMap<>(20);

    public boolean showmarker = true, showMarkerDistance = true;
    protected String title;

    public static Locale FormatLocale = StartParameters.formatLocale;

//    protected NumberAxis yAxisConcentration, yAxisVelocity, yAxisLvl;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected XYDataset datasetConcentration, datasetVelocity, dataSetLvl;
    protected int numberUsedDataSetSlots = 0;
    protected ArrayTimeLinePipeContainer referenceContainer;
    protected ArrayTimeLineMeasurementContainer measurementContainer;
    XYSeriesEditorTablePanel editorpanel;

    protected static String directoryPDFsave = ".";

    XYSeries v = new XYSeries(new SeriesKey("Velocity", "u", "m/s", Color.red));
//        TimeSeries q = new TimeSeries(new SeriesKey("Flux", "q", "m³/s", Color.DARK_GRAY), "Time", "m³/s");
    XYSeries hpipe = new XYSeries(new SeriesKey("Waterlevel", "h", "m", Color.blue));
    XYSeries refConcentration = new XYSeries(new SeriesKey("Conentration ref.", "", "kg/m³", Color.black, AxisKey.CONCENTRATION()));
    XYSeries refMassFlux = new XYSeries(new SeriesKey("Massflux ref.", "", "kg/s", Color.orange.darker(), new AxisKey("Massflux")));
    XYSeries refMass = new XYSeries(new SeriesKey("Mass ref.", "", "kg", Color.red.darker(), new AxisKey("Mass")));

    XYSeries moment1_ref = new XYSeries(new SeriesKey("1.Moment ref", "1.M ref", "m", Color.GREEN, new AxisKey("Moment", "1. Moment")));
    XYSeries moment1_refvorgabe = new XYSeries(new SeriesKey("1.Moment Matlab Vorgabe ", "1.M vorgabe", "m", Color.BLUE, new AxisKey("Moment", "1. Moment")));
    XYSeries moment1_messung = new XYSeries(new SeriesKey("1.Moment Messung", "1.M messung", "m", Color.red, new AxisKey("Moment", "1. Moment")));
    XYSeries moment1_delta = new XYSeries(new SeriesKey("Delta 1.Moment Messung-Ref", "Fehler 1.M (messung-ref)", "m", Color.red, new AxisKey("Moment", "1. Moment")));

    XYSeries moment2_ref = new XYSeries(new SeriesKey("2.Moment Matlab", "2.M ref", "m", Color.GREEN, new AxisKey("Moment2", "2. Moment")));
    XYSeries moment2_mess = new XYSeries(new SeriesKey("2.Moment Messung", "2.M mess", "m", Color.red, new AxisKey("Moment2", "2. Moment")));

    //Measures auflisten
    XYSeries m_p = new XYSeries(new SeriesKey("#Particles", "n", "-", Color.magenta));
    XYSeries m_c = new XYSeries(new SeriesKey("Concentration ptcl", "", "kg/m³", Color.black, AxisKey.CONCENTRATION()));
    XYSeries m_m = new XYSeries(new SeriesKey("Mass ptcl", "", "kg", Color.red, new AxisKey("Mass")));
    XYSeries m_m_sma = new XYSeries(new SeriesKey("Mass ptcl SMA", "", "kg", new Color(250, 200, 200), new AxisKey("Mass")));
    XYSeries m_MassFlux = new XYSeries(new SeriesKey("Massflux ptcl", "", "kg/s", Color.orange, new AxisKey("Massflux")));

    XYSeries m_vol = new XYSeries(new SeriesKey("Volumen", "V", "m³", Color.cyan));
    XYSeries m_n = new XYSeries(new SeriesKey("#Samples per Interval", "", "-", Color.lightGray));
    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    public SpacelinePanel(ArrayTimeLinePipeContainer referenceContainer, ArrayTimeLineMeasurementContainer tl, String title) {
        super(new BorderLayout());
        this.title = title;
        panelNorth = new JPanel(new BorderLayout());
        this.add(panelNorth, BorderLayout.NORTH);
        this.panelChartContainer = new JPanel(new BorderLayout());
        initCheckboxpanel();
        splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.add(splitpane, BorderLayout.CENTER);
        editorpanel = new XYSeriesEditorTablePanel();
        editorpanel.setSpacelinePanel(this);

//        if (!(tl instanceof ArrayTimeLinePipe)) {
//            throw new NullPointerException("Timeline must be of Type " + ArrayTimeLinePipe.class + " to get 1. and 2. momentum of mass. is: "+tl);
//        }
        this.referenceContainer = referenceContainer;
        this.measurementContainer = tl;
        try {
            setTimeToShow(referenceContainer.getTimeIndex(0));
        } catch (Exception e) {
        }

        initChart(title);
        addPDFexport();
        addEMFexport();
        splitpane.add(editorpanel);
        splitpane.setDividerLocation(0.7);
    }

    public final void setTimeToShow(final long time) {
//        this.referenceContainer = ArrayTimeLinePipeContainer.referenceContainer;
        if (t != null && t.isAlive()) {
            System.out.println(this.getClass() + ":: interrupt TimelineThread " + t.toString());
            try {
                t.interrupt();
            } catch (Exception e) {
            }
        }
        this.t = new Thread("SpacelinePanel") {
            @Override
            public void run() {

//                if (SpacelinePanel.this.collection != null) {
//                    SpacelinePanel.this.collection.removeAllSeries();
//
//                }
//                SpacelinePanel.this.title = title;
                SpacelinePanel.this.updateChart("Preparing... " + new Date(time));

                SpacelinePanel.this.buildPipeSpaceline(referenceContainer, time);

                SpacelinePanel.this.updateCheckboxPanel();

                boolean showWholeDate = false;
                if (referenceContainer != null && referenceContainer.getEndTime() > 24L * 3600L * 1000L) {
                    showWholeDate = true;
                } else if (measurementContainer != null && measurementContainer.getEndTime() > 24L * 3600L * 1000L) {
                    showWholeDate = true;
                }
                if (showWholeDate) {
//                    GregorianCalendar gc=new BuddhistCalendar(TimeZone.getTimeZone("UTC"));
//                    gc.setTimeInMillis(time);
                    SimpleDateFormat sdf = new SimpleDateFormat();
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    SpacelinePanel.this.updateChart(sdf.format(time) + "");
                } else {
                    StringBuffer str = new StringBuffer(10);
                    if (time >= 3600_000L) {
                        //need Hours
                        str.append(time / 3600_000L).append("h: ");
                    }
                    int minutes = (int) (time % 3600_000) / 60000;
                    if (str.length() > 1 && minutes < 10) {
                        //führende 0
                        str.append("0");
                    }
                    str.append(minutes).append("m: ");
                    int seconds = (int) (time % 60000) / 1000;
                    if (seconds < 10) {
                        str.append("0");
                    }
                    str.append(seconds).append("s");
                    SpacelinePanel.this.updateChart(str.toString());
                }

                SpacelinePanel.this.updateShownTimeSeries();

                SpacelinePanel.this.editorpanel.getTable().updateTableByCollection();

            }

        };
        t.start();
    }

    public void markDistance(double distance) {
        if (showMarkerDistance == false && marker == null) {
            return;
        }
        if (marker == null) {
            marker = new ValueMarker(distance, new Color(50, 150, 250, 100), new BasicStroke(1.5f));
            marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));
            marker.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            ((XYPlot) this.panelChart.getChart().getPlot()).addDomainMarker(marker);
        }
        marker.setValue(distance);
        if (showMarkerDistance) {
            marker.setLabel(distance + "m");
        } else {
            marker.setLabel("");
        }
        if (!showmarker) {
            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
            marker = null;
        }
    }

    public void removeMarker() {
        if (marker != null) {
            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
            marker = null;
        }
    }

    private void buildPipeSpaceline(ArrayTimeLinePipeContainer instance, long time) {
        this.referenceContainer = instance;

        double dtm = 1;

//        dtm = (tlm.getEndTime() - tlm.getStartTime()) / (1000. * (tlm.lengthTimes() - 1));
        if (editorpanel != null && editorpanel.getTable() != null) {
            this.collection = editorpanel.getTable().collection;
        } else {
            this.collection = new XYSeriesCollection();
            System.out.println(this.getClass() + ":: editorpanel not yet initialized. create new XYSeriesCollection.");
        }
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            collection.getSeries(i).setNotify(false);
        }

        collection.removeAllSeries();

        hpipe.clear();
        v.clear();
        refConcentration.clear();
        refMass.clear();
        refMassFlux.clear();
        m_c.clear();
        m_MassFlux.clear();
        m_m.clear();
        m_m_sma.clear();
        m_n.clear();
        m_p.clear();
        m_vol.clear();

//        this.collection.removeAllSeries();
        int indexTimePipe = instance.getTimeIndex(time);
        int indexTimeMeasure = ArrayTimeLineMeasurementContainer.instance.getIndexForTime(time);
        int ind = indexTimePipe;
        float[] vs = instance.getVelocityForTimeIndex(ind);
        float[] hs = instance.getWaterlevelsForTimeIndex(ind);
        float[] massFlux = instance.getMassFluxForTimeIndex(ind, 0);
        float[] cs = instance.getConcentrationForTimeIndex(ind, 0);
        float[] vol = instance.getVolumesForTimeIndex(ind);
        float[] q = instance.getDischargeForTimeIndex(ind);
//        System.out.println("timeindex: " + ind);
//        System.out.println(getClass() + ":: ArrayTimeLinePipeContainer.distance=" + instance.distance);
        if (instance.distance != null) {
            for (int i = 0; i < instance.distance.length; i++) {
//            System.out.println(i + "\t" + hs[i] + "m,\t"+vs[i]+"m/s"+"\tC:"+cs[i]);
                hpipe.addOrUpdate(instance.distance[i], hs[i]);
                v.addOrUpdate(instance.distance[i], vs[i]);
                refMassFlux.addOrUpdate(instance.distance[i], massFlux[i]);
                refMass.addOrUpdate(instance.distance[i], vol[i] * cs[i]);
                refConcentration.addOrUpdate(instance.distance[i], cs[i]);
            }
            this.collection.addSeries(v);
            this.collection.addSeries(hpipe);

            if (refConcentration.getMaxY() > 0) {
                this.collection.addSeries(refConcentration);
//            this.collection.addSeries(createMovingaverageCentral(refConcentration, 100, "SMA100 " + refConcentration.getKey()));
            }

            if (refMass.getMaxY() > 0) {
                this.collection.addSeries(refMass);
//            this.collection.addSeries(createMovingaverageCentral(refMass, 100, "SMA100 " + refMass.getKey()));
            }

            if (refMassFlux.getMaxY() != 0) {
                this.collection.addSeries(refMassFlux);
//                System.out.println("add refmassflux in space");
//            XYSeries mr100 = createMovingaverageCentral(refMassFlux, 10, "SMA10 Massflux ref");
//            ((SeriesKey) mr100.getKey()).lineColor = Color.GRAY;
//            this.collection.addSeries(mr100);
            }
        } else {
            System.out.println("no reference value container");
        }

        if (ArrayTimeLineMeasurementContainer.instance != null) {
            ArrayTimeLineMeasurementContainer container = ArrayTimeLineMeasurementContainer.instance;
            ind = container.getIndexForTime(time);
            float[] mass = container.getMassForTimeIndex(ind);
            float[] ps = container.getNumberOfParticlesForTimeIndex(ind);
            int[] ns = container.getNumberOfMeasurementsPerTimestepForTimeIndex(ind);
            //System.out.println("distances: " + container.distance.length);
            for (int i = 0; i < ArrayTimeLineMeasurementContainer.distance.length; i++) {
                float d = ArrayTimeLineMeasurementContainer.distance[i];

                m_p.addOrUpdate(d, ps[i]);
                m_m.addOrUpdate(d, mass[i]);
                m_c.addOrUpdate(d, cs[i]);
                m_n.addOrUpdate(d, ns[i]);
                m_MassFlux.addOrUpdate(d, mass[i] * q[i]);
            }
            m_m_sma = createMovingaverageCentral(m_m, 10, "10 mean Mass", m_m_sma);

            if (moment1_refvorgabe.getMaxY() > 0) {
                this.collection.addSeries(moment1_refvorgabe);
            }
            if (moment1_ref.getMaxY() > 0) {
                this.collection.addSeries(moment1_ref);
            }

            if (moment1_messung.getMaxY() > 0) {
                this.collection.addSeries(moment1_messung);
            }

            if (moment1_delta.getMaxY() > 0) {
                this.collection.addSeries(moment1_delta);
            }

            if (!moment2_ref.isEmpty() && moment2_ref.getMaxY() > 0) {
                this.collection.addSeries(moment2_ref);
            }

            if (!moment2_mess.isEmpty() && moment2_mess.getMaxY() > 0) {
                this.collection.addSeries(moment2_mess);
            }

            if (m_n.getMaxY() > 0) {
                this.collection.addSeries(m_n);
            }

            if (m_p.getMaxY() > 0) {
                this.collection.addSeries(m_p);
            }

            if (m_vol.getMaxY() > 0) {
                this.collection.addSeries(m_vol);
            }

//            if (m_c.getMaxY() > 0) {
//                this.collection.addSeries(m_c);
//                XYSeries c100 = createConcentrationMovingaverageCentral(, 100);
//                ((SeriesKey) c100.getKey()).lineColor = Color.GRAY;
//                this.collection.addSeries(c100);
//            }
            if (m_m.getMaxY() > 0) {

                this.collection.addSeries(m_MassFlux);
                this.collection.addSeries(m_m);
                this.collection.addSeries(m_m_sma);
//                XYSeries m100 = createMovingaverageCentral(m_m, 10, "10 mean Mass");
//                ((SeriesKey) m100.getKey()).lineColor = Color.magenta;
//                this.collection.addSeries(m100);
            }
            this.collection.seriesChanged(null);
//        beforeWasNull = false;
        } else {
            System.out.println(getClass() + ":: MeasurementContainer is not initialized.");
        }
    }

    private void buildManholeTimeline(StorageVolume vol) {
        XYSeries h = new XYSeries(new SeriesKey("Waterheight", "h", "m", Color.BLUE));
        XYSeries lvl = new XYSeries(new SeriesKey("Waterlvl", "lvl", "m", Color.cyan));
//        TimeSeries q = new TimeSeries(new SeriesKey("Flow", "Q", "m³/s", Color.LIGHT_GRAY), "m³/s", "Time");
//        TimeSeries volumen = new TimeSeries(new SeriesKey("Volume", "V", "m³", Color.LIGHT_GRAY), "m³", "Time");
        for (int i = 0; i < vol.getStatusTimeLine().getNumberOfTimes(); i++) {

            h.add(i, vol.getStatusTimeLine().getWaterZ(i));
            lvl.add(i, vol.getStatusTimeLine().getWaterZ(i) - vol.getSole_height());
        }

        this.collection = new XYSeriesCollection();
        this.collection.addSeries(h);
        this.collection.addSeries(lvl);

    }

    private void initCheckboxpanel() {
        panelChecks = new JPanel();
//        panelChartContainer.add(panelChecks, BorderLayout.SOUTH);
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
        panelChecks.removeAll();
        int maxcolumns = 7;
        panelChecks.setLayout(new GridLayout(checkboxes.length / maxcolumns + 1, maxcolumns));
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i] == null || !checkboxes[i].getText().equals(this.collection.getSeriesKey(i).toString())) {
                Boolean shallBeChecked = checks.get(this.collection.getSeriesKey(i).toString());
                if (shallBeChecked == null) {
                    shallBeChecked = false;
                }
                checkboxes[i] = new JCheckBox(this.collection.getSeriesKey(i).toString(), shallBeChecked);
                checkboxes[i].setToolTipText(((SeriesKey) this.collection.getSeriesKey(i)).name + " (" + ((SeriesKey) this.collection.getSeriesKey(i)).symbol + ") [" + ((SeriesKey) this.collection.getSeriesKey(i)).unit + "]");
                checkboxes[i].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        updateShownTimeSeries();
                        JCheckBox c = (JCheckBox) ae.getSource();
                        checks.put(c.getText(), c.isSelected());
                    }
                });
            }
            panelChecks.add(checkboxes[i], i);
        }
        this.revalidate();
    }

    public void updateShownTimeSeries() {
        if (this.collection == null) {
            return;
        }
        if (checkboxes == null) {
            return;
        }
        XYPlot plot = panelChart.getChart().getXYPlot();
        plot.clearRangeAxes();
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            try {
                plot.setDataset(i, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        System.out.println("updateXYSpacelinePlot");

        numberUsedDataSetSlots = 0;
        yAxisMap.clear();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
//        System.out.println("checkboxes: "+checkboxes.length);
        for (int i = 0; i < collection.getSeriesCount() /*checkboxes.length*/; i++) {
            if (true || checkboxes[i].isSelected() || !checkboxes[i].isVisible()) {
//                if (this.collection.getSeries(i) == null) {
//                    continue;
//                }
                SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
                if (!key.isVisible()) {
                    continue;
                }
                /**
                 * Baue neues Dataset wenn keine Wiederekennung zu finden ist
                 */
                XYSeriesCollection dataset = null;
                if (key.axisKey == null || key.axisKey.name == null) {
                    /*
                     * No recognition (mapping to other dataset) required.
                     * Build a new Dataset+Yaxis for this TimeSeries
                     */
                    indexDataset = numberUsedDataSetSlots;
                    numberUsedDataSetSlots++;
                    dataset = new XYSeriesCollection(this.collection.getSeries(i));
                    plot.setDataset(indexDataset, dataset);
                    renderer = new XYLineAndShapeRenderer(true, false);
                    plot.setRenderer(indexDataset, renderer);

                    NumberAxis axis2 = new NumberAxis(checkboxes[i].getText());
                    axis2.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
                    yAxisMap.put(axis2.getLabel(), indexDataset);
                    axis2.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(indexDataset, axis2);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                } else {
                    NumberAxis yAxis;
                    if (yAxisMap.containsKey(key.axisKey.toString())) {
                        indexDataset = yAxisMap.get(key.axisKey.toString());
                        yAxis = (NumberAxis) plot.getRangeAxis(indexDataset);
                        dataset = (XYSeriesCollection) plot.getDataset(indexDataset);
                        indexSeries = dataset.getSeriesCount();
                        XYSeries ts = (XYSeries) this.collection.getSeries(i);
                        if (key.logarithmic) {
                            makeSeriesAbsolute(ts);
                        }
                        dataset.addSeries(ts);
                        renderer = (XYLineAndShapeRenderer) plot.getRenderer(indexDataset);
                        renderer.setSeriesStroke(indexSeries, key.stroke);
                    } else {
                        // Axis key not yet in use. Build new Dataset for this Yaxis
                        indexDataset = numberUsedDataSetSlots;
                        numberUsedDataSetSlots++;
                        yAxisMap.put(key.axisKey.toString(), indexDataset);
                        indexSeries = 0;
                        String label = key.axisKey.label;
                        if (label == null || label.isEmpty()) {
                            label = "[" + key.unit + "]";
                        }
                        if (key.logarithmic) {
                            yAxis = new LogarithmicAxis(label);
                        } else {
                            yAxis = new NumberAxis(label);
                        }
                        yAxis.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
//                        if (key.axisKey.label != null) {
//                            yAxis = new NumberAxis(key.axisKey.label);
//                        } else {
//                            yAxis = new NumberAxis("[" + key.unit + "]");
//                        }
                        if (key.axisKey != null) {
                            if (key.axisKey.manualBounds) {
                                yAxis.setLowerBound(key.axisKey.lowerBound);
                                yAxis.setUpperBound(key.axisKey.upperBound);
                            } else {
                                key.axisKey.lowerBound = yAxis.getLowerBound();
                                key.axisKey.upperBound = yAxis.getUpperBound();
                            }
                        }
//                        yAxisMap.put(yAxis.getLabel(), indexDataset);
                        XYIntervalRenderer intervalRenderer = new XYIntervalRenderer();
                        intervalRenderer.drawinterval = true;
                        if (key.axisKey != null) {
                            intervalRenderer.interval = key.axisKey.drawInterval;
                        }
                        renderer = intervalRenderer;// new XYIntervalRenderer();//new XYLineAndShapeRenderer(true, false);
                        renderer.setSeriesStroke(indexSeries, key.stroke);
                        plot.setRenderer(indexDataset, renderer);

                        yAxis.setAutoRangeIncludesZero(false);

                        plot.setRangeAxis(indexDataset, yAxis);
                        plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                        dataset = new XYSeriesCollection(this.collection.getSeries(i));

                        if (key.logarithmic) {
                            for (Object s : dataset.getSeries()) {
                                if (s instanceof XYSeries) {
                                    XYSeries ts = (XYSeries) s;
                                    makeSeriesAbsolute(ts);
                                }
                            }
                        }
                        try {
                            plot.setDataset(indexDataset, dataset);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                    } catch (Exception e) {
                    }
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
                    if (key.axisKey != null && key.axisKey.drawInterval > 1) {
                        if (renderer instanceof XYIntervalRenderer) {
                            ((XYIntervalRenderer) renderer).interval = key.axisKey.drawInterval;
                        }
                    }
//                    System.out.println("Series "+key.label+" shape: "+key.shape);
                } else {
                    renderer.setSeriesShape(indexSeries, null);
                    renderer.setSeriesShapesVisible(indexSeries, false);
//                    System.out.println("Series "+key.label+" without shape");
                }
                indexDataset++;
            }
        }
        if (matlabStyle) {
            MatlabLayout.layoutToMatlab(this.panelChart.getChart());
        }
    }

    /**
     * Logarithic axis can only plot, when all values are positive. So update
     * all elements of this timeseries to absolute values. 0 becomes NaN, this
     * will hide the point completely
     *
     * @param ts
     */
    private void makeSeriesAbsolute(XYSeries ts) {
        if (ts.getMinY() <= 0) {
            for (int j = 0; j < ts.getItemCount(); j++) {
                double v = ts.getY(j).doubleValue();
                if (v == 0) {
                    ts.updateByIndex(j, Double.NaN);
                } else if (v < 0) {
                    ts.updateByIndex(j, Math.abs(v));
                }
            }
        }
    }

    /**
     * @deprecated
     */
    protected void updateShownTimeSeriesOld() {
        if (this.collection == null) {
            return;
        }
        XYPlot plot = panelChart.getChart().getXYPlot();
//        System.out.println("Plot: " + plot.getClass());
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            plot.setDataset(i, null);
        }
        plot.clearRangeAxes();
        numberUsedDataSetSlots = 0;
        yAxisMap.clear();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;

        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i].isSelected()) {
                if (this.collection.getSeries(i) == null) {
                    continue;
                }
                SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
                /**
                 * Baue neues Dataset wenn keine Wiederekennung zu finden ist
                 */
                XYSeriesCollection dataset = null;
                if (key.axisKey == null || key.axisKey.name == null) {
                    /*
                     * No recognition (mapping to other dataset) required.
                     * Build a new Dataset+Yaxis for this TimeSeries
                     */
                    indexDataset = numberUsedDataSetSlots;
                    numberUsedDataSetSlots++;
                    dataset = new XYSeriesCollection(this.collection.getSeries(i));
                    plot.setDataset(indexDataset, dataset);
//                    System.out.println("Neues Dataset " + key.toString() + " an indexDataset:" + indexDataset);
                    renderer = new XYLineAndShapeRenderer(true, false);
                    plot.setRenderer(indexDataset, renderer);
                    NumberAxis axis2 = new NumberAxis(checkboxes[i].getText());

                    axis2.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
                    yAxisMap.put(axis2.getLabel(), indexDataset);
                    axis2.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(indexDataset, axis2);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                } else {
                    NumberAxis yAxis;
                    if (yAxisMap.containsKey(key.axisKey.name)) {
                        indexDataset = yAxisMap.get(key.axisKey.name);
                        yAxis = (NumberAxis) plot.getRangeAxis(indexDataset);
                        dataset = (XYSeriesCollection) plot.getDataset(indexDataset);
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
                        yAxis.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
                        yAxisMap.put(yAxis.getLabel(), indexDataset);
                        renderer = new XYLineAndShapeRenderer(true, false);
                        renderer.setSeriesStroke(indexSeries, key.stroke);
                        plot.setRenderer(indexDataset, renderer);

                        yAxis.setAutoRangeIncludesZero(false);

                        plot.setRangeAxis(indexDataset, yAxis);
                        plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                        dataset = new XYSeriesCollection(this.collection.getSeries(i));
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
//                     System.out.println("Stroke for "+key.label+" is null");
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

        }
        if (matlabStyle) {
            MatlabLayout.layoutToMatlab(this.panelChart.getChart());
        }
    }

    /**
     * @deprecated
     */
    protected void updateShownTimeSeries2() {
        if (this.collection == null || panelChart == null) {
            return;
        }
        XYPlot plot = panelChart.getChart().getXYPlot();
//        System.out.println("Plot: " + plot.getClass());
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            plot.setDataset(i, null);
        }
        plot.clearRangeAxes();
        numberUsedDataSetSlots = 0;
        yAxisMap.clear();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i].isSelected()) {
                if (this.collection.getSeries(i) == null) {
                    continue;
                }
                SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
                /**
                 * Baue neues Dataset wenn keine Wiederekennung zu finden ist
                 */
                XYSeriesCollection dataset = null;
                if (key.axisKey == null || key.axisKey.name == null) {
                    /*
                     * No recognition (mapping to other dataset) required.
                     * Build a new Dataset+Yaxis for this TimeSeries
                     */
                    indexDataset = numberUsedDataSetSlots;
                    numberUsedDataSetSlots++;
                    dataset = new XYSeriesCollection(this.collection.getSeries(i));
                    plot.setDataset(indexDataset, dataset);
//                    System.out.println("Neues Dataset " + key.toString() + " an indexDataset:" + indexDataset);
                    renderer = new XYLineAndShapeRenderer(true, false);
                    plot.setRenderer(indexDataset, renderer);

                    NumberAxis axis2 = new NumberAxis(checkboxes[i].getText());
                    axis2.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
                    yAxisMap.put(axis2.getLabel(), indexDataset);
                    axis2.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(indexDataset, axis2);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                } else {
                    NumberAxis yAxis;
                    if (yAxisMap.containsKey(key.axisKey.name)) {
                        indexDataset = yAxisMap.get(key.axisKey.name);
//                        System.out.println("Platz für Dataset " + key.toString() + " an index " + indexDataset);
                        yAxis = (NumberAxis) plot.getRangeAxis(indexDataset);
                        dataset = (XYSeriesCollection) plot.getDataset(indexDataset);
                        indexSeries = dataset.getSeriesCount();
                        dataset.addSeries(this.collection.getSeries(i));

                    } else {
                        // Axis key not yet in use. Build new Dataset for this Yaxis
                        indexDataset = numberUsedDataSetSlots;
                        numberUsedDataSetSlots++;
                        yAxisMap.put(key.axisKey.name, indexDataset);
//                        System.out.println("Platziere neues Dataset " + key.toString() + " an index " + indexDataset);
                        indexSeries = 0;
                        if (key.axisKey.label != null) {
                            yAxis = new NumberAxis(key.axisKey.label);
                        } else {
                            yAxis = new NumberAxis("[" + key.unit + "]");
                        }
                        yAxis.setNumberFormatOverride(NumberFormat.getNumberInstance(FormatLocale));
                        yAxisMap.put(yAxis.getLabel(), indexDataset);
                        renderer = new XYLineAndShapeRenderer(true, false);
                        plot.setRenderer(indexDataset, renderer);
                        yAxis.setAutoRangeIncludesZero(false);

                        plot.setRangeAxis(indexDataset, yAxis);
                        plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                        dataset = new XYSeriesCollection(this.collection.getSeries(i));
                        plot.setDataset(indexDataset, dataset);
                    }
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                }
                renderer = (XYLineAndShapeRenderer) plot.getRenderer(indexDataset);
                if (key.lineColor != null) {
                    renderer.setSeriesPaint(indexSeries, key.lineColor);
                }
                indexDataset++;
            }
        }
    }

    private void initChart(String title) {
        if (title == null) {
            title = "";
        }
        JFreeChart chart = ChartFactory.createXYLineChart(title, "Position x [m]", "", collection, PlotOrientation.VERTICAL, true, true, true);
        XYPlot plot = (XYPlot) chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
//        plot.setDomainAxis(new NumberAxis("Position x [m]"));
        this.panelChart = new ChartPanel(chart);
        panelChartContainer.add(panelChart, BorderLayout.CENTER);
        splitpane.add(panelChartContainer);
        updateTimeSlider();

    }

    public void updateTimeSlider() {
        boolean init = false;
        try {
            if (referenceContainer.getNumberOfTimes() > 0) {
                init = true;
            }
        } catch (Exception e) {
            init = false;
        }
        if (!init) {
            JLabel label = new JLabel("Statusvalues for Pipes not yet initialized.");
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(label, BorderLayout.CENTER);
            JButton buttonUpdate = new JButton("Refresh");
            panel.add(buttonUpdate, BorderLayout.EAST);
            panelNorth.removeAll();
            panelNorth.add(panel, BorderLayout.CENTER);
            buttonUpdate.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    SpacelinePanel.this.updateTimeSlider();
                }
            });
            panelNorth.revalidate();
        } else {
            if (slider != null && slider.getMaximum() == referenceContainer.getNumberOfTimes() - 1) {
                return;
            }
            //Slider has to be initialized.
            slider = new JSlider(0, referenceContainer.getNumberOfTimes() - 1);
            slider.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent me) {
//                    System.out.println("SpacelinePanel::slider.mouseReleased: released @ " + new Date(ArrayTimeLinePipe.getTimeMilliseconds(slider.getValue()))+"\t value="+slider.getValue()+" / Times:"+ArrayTimeLinePipe.getNumberOfTimes());
                    SpacelinePanel.this.setTimeToShow(referenceContainer.getTimeMilliseconds(slider.getValue()));
                }
            });
            panelNorth.removeAll();
            panelNorth.add(slider, BorderLayout.CENTER);
            panelNorth.revalidate();
        }
    }

    private void updateChart(String title) {
        if (title == null) {
            title = "";
        }
        if (panelChart != null && panelChart.getChart() != null) {
            panelChart.getChart().setTitle(title);
        }
    }

    private void addPDFexport() {
        JPopupMenu menu = this.panelChart.getPopupMenu();
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
                            int n = fc.showSaveDialog(SpacelinePanel.this);
                            if (n == JFileChooser.APPROVE_OPTION) {
                                File output = fc.getSelectedFile();
                                directoryPDFsave = output.getParent();
                                if (!output.getName().endsWith(".pdf")) {
                                    output = new File(output.getAbsolutePath() + ".pdf");
                                }
                                try {

                                    Rectangle rec = SpacelinePanel.this.getBounds();
                                    Document doc = new Document(new com.itextpdf.text.Rectangle(0, 0, rec.width, rec.height));
                                    FileOutputStream fos = new FileOutputStream(output);
                                    PdfWriter writer = PdfWriter.getInstance(doc, fos);
                                    doc.open();
                                    PdfContentByte cb = writer.getDirectContent();
                                    PdfTemplate tp = cb.createTemplate((float) rec.getWidth(), (float) rec.getHeight());
                                    PdfGraphics2D g2d = new PdfGraphics2D(cb, (float) rec.getWidth(), (float) rec.getHeight());
                                    panelChart.getChart().draw(g2d, rec);
                                    cb.addTemplate(tp, 25, 200);
                                    g2d.dispose();
                                    doc.close();
                                    fos.close();

                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(SpacelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                } catch (DocumentException ex) {
                                    Logger.getLogger(SpacelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(SpacelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private void addEMFexport() {
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
                                    if (file.isFile() && file.getName().endsWith(".emf")) {
                                        return true;
                                    }
                                    return false;
                                }
                            };
                            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            int n = fc.showSaveDialog(SpacelinePanel.this);
                            if (n == JFileChooser.APPROVE_OPTION) {
                                File output = fc.getSelectedFile();
                                directoryPDFsave = output.getParent();
                                if (!output.getName().endsWith(".emf")) {
                                    output = new File(output.getAbsolutePath() + ".emf");
                                }
                                try (OutputStream out = new java.io.FileOutputStream(output)) {
                                    Rectangle rec = SpacelinePanel.this.getBounds();

//                                    System.out.println("Rect.bounds="+rec);
                                    int width = rec.width;
                                    int height = rec.height;
                                    EMFGraphics2DX g2d = new EMFGraphics2DX(out, new Dimension((int) (width), height));
                                    g2d.writeHeader();
//                                    Graphics2D g2r = null;
                                    try {
//                                        System.out.println("g2d:"+g2d);
//                                        g2r = (Graphics2D) g2d.create();
                                        panelChart.getChart().draw(g2d, panelChart.getBounds());
                                    } catch (Exception e) {
                                        System.err.println("rect:" + width + "x" + height);
                                        System.err.println("g2d:" + g2d);
//                                        System.err.println("g2r:" + g2r);
                                        System.err.println("chart:" + panelChart.getChart());
                                        e.printStackTrace();
                                    }
                                    g2d.closeStream();

                                    out.flush();

                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(SpacelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(SpacelinePanel.class
                                            .getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public static XYSeries createMovingaverageCentral(XYSeries original, int maxinvolvedPeriods, String name, XYSeries target) {
        if (target == null) {
            SeriesKey oldKey = (SeriesKey) original.getKey();
            Color colorNew = null;
            if (oldKey.lineColor != null) {
                colorNew = new Color(oldKey.lineColor.getRGB() * 300000);
//            System.out.println("Color Old: " + oldKey.lineColor.toString() + " -> new: " + colorNew.toString());
            }
            SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, "", oldKey.unit, colorNew, oldKey.axisKey);

            XYSeries average = new XYSeries(newKey);
            target = average;
        }
        int minIndex = maxinvolvedPeriods / 2 + 1;
        int maxIndex = original.getItemCount() - maxinvolvedPeriods / 2;
        int radius = maxinvolvedPeriods / 2;
        double nenner = (2. * radius + 1.);
//        boolean verbose = name.startsWith("c_mat");
        for (int i = minIndex; i < maxIndex; i++) {
            double sum = 0;
            for (int j = i - radius; j < i + radius; j++) {
                sum += original.getDataItem(j).getY().doubleValue();

            }
            double wert = sum / nenner;
//            if (verbose) {
//                System.out.println(name + " " + p + " :\t" + sum + "/" + nenner + " = " + wert);
//            }
            target.add(original.getDataItem(i).getX(), wert);
        }

        return target;
    }

    public static XYSeries createConcentrationMovingaverageCentral(ArrayTimeLineMeasurement mtm, int maxinvolvedPeriods) {

        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of Concentration", maxinvolvedPeriods + " mean c", "kg/m³", Color.GREEN, AxisKey.CONCENTRATION());
        XYSeries average = new XYSeries(newKey);
        int minIndex = maxinvolvedPeriods / 2 + 1;
        int maxIndex = mtm.getContainer().getNumberOfTimes() - maxinvolvedPeriods / 2;
        int radius = maxinvolvedPeriods / 2;
        double nenner = (2. * radius + 1.);
//        boolean verbose = name.startsWith("c_mat");
//        System.out.println("matlab concentration " + maxinvolvedPeriods + " intervalls: von " + minIndex + " bis " + maxIndex);
        for (int i = minIndex; i < maxIndex; i++) {
            double sum = 0;

            double counter = 0;
            for (int j = i - radius; j < i + radius; j++) {
                if (mtm.hasValues(j)) {
                    sum += mtm.getConcentration(j);
                }

                counter++;
            }
            double wert = sum / counter;
            average.add(i, wert);
//            if (i == 100) {
//                System.out.println(CapacityTimelinePanel.class + ":: used " + counter + " values to calculate average. Nenner: " + nenner);
//            }
        }

        return average;
    }

    public void setSplitDivider(double relpos) {
        this.splitpane.setDividerLocation(relpos);
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

    @Override
    public void selectCapacity(Capacity c, Object caller) {
        if (c != null) {
            if (c instanceof Pipe) {
                Pipe p = (Pipe) c;
                this.measurementContainer = p.getMeasurementTimeLine().getContainer();
                if (p.getStatusTimeLine() instanceof ArrayTimeLinePipe) {
                    this.referenceContainer = ((ArrayTimeLinePipe) p.getStatusTimeLine()).container;
                    buildPipeSpaceline(referenceContainer, 0);
                }
            }
        }
    }

    public void setDividerlocation(double ratio) {
        this.splitpane.setDividerLocation(ratio);
    }

}
