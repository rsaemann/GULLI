package view.timeline;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import control.listener.CapacitySelectionListener;
import org.freehep.graphicsio.emf.EMFGraphics2DX;
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
import java.util.Date;
import java.util.HashMap;
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
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.ArrayTimeLineMeasurementContainer;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.topology.Capacity;
import model.topology.Pipe;
import model.topology.StorageVolume;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import static view.timeline.CapacityTimelinePanel.matlabStyle;

/**
 *
 * @author saemann
 */
public class SpacelinePanel extends JPanel implements CapacitySelectionListener {

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

//    protected NumberAxis yAxisConcentration, yAxisVelocity, yAxisLvl;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected XYDataset datasetConcentration, datasetVelocity, dataSetLvl;
    protected int numberUsedDataSetSlots = 0;
    protected ArrayTimeLinePipeContainer referenceContainer;
    protected ArrayTimeLineMeasurementContainer measurementContainer;
    XYSeriesEditorTablePanel editorpanel;

    protected static String directoryPDFsave = ".";
    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    public SpacelinePanel(ArrayTimeLinePipeContainer referenceContainer, ArrayTimeLineMeasurementContainer tl, String title) {
        super(new BorderLayout());
        this.title = title;

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

                if (SpacelinePanel.this.collection != null) {
                    SpacelinePanel.this.collection.removeAllSeries();

                }
//                SpacelinePanel.this.title = title;

                SpacelinePanel.this.updateChart("Preparing... " + new Date(time));

                SpacelinePanel.this.buildPipeSpaceline(referenceContainer, time);

                SpacelinePanel.this.updateCheckboxPanel();

                SpacelinePanel.this.updateChart(new Date(time) + "");

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

        XYSeries v = new XYSeries(new SeriesKey("Velocity", "u", "m/s", Color.red));
//        TimeSeries q = new TimeSeries(new SeriesKey("Flux", "q", "m³/s", Color.DARK_GRAY), "Time", "m³/s");
        XYSeries hpipe = new XYSeries(new SeriesKey("Waterlevel", "h", "m", Color.blue));
        XYSeries refConcentration = new XYSeries(new SeriesKey("ref. Konzentration", "c_mat", "kg/m³", Color.red, AxisKey.CONCENTRATION()));
        XYSeries refMass = new XYSeries(new SeriesKey("ref. Masse", "m_ref", "kg", Color.black, new AxisKey("Mass")));

        XYSeries moment1_ref = new XYSeries(new SeriesKey("1.Moment Matlab", "1.M ref", "m", Color.GREEN, new AxisKey("Moment", "1. Moment")));
        XYSeries moment1_refvorgabe = new XYSeries(new SeriesKey("1.Moment Matlab Vorgabe ", "1.M vorgabe", "m", Color.BLUE, new AxisKey("Moment", "1. Moment")));
        XYSeries moment1_messung = new XYSeries(new SeriesKey("1.Moment Messung", "1.M messung", "m", Color.red, new AxisKey("Moment", "1. Moment")));
        XYSeries moment1_delta = new XYSeries(new SeriesKey("Delta 1.Moment Messung-Ref", "Fehler 1.M (messung-ref)", "m", Color.red, new AxisKey("Moment", "1. Moment")));

        XYSeries moment2_ref = new XYSeries(new SeriesKey("2.Moment Matlab", "2.M ref", "m", Color.GREEN, new AxisKey("Moment2", "2. Moment")));
        XYSeries moment2_mess = new XYSeries(new SeriesKey("2.Moment Messung", "2.M mess", "m", Color.red, new AxisKey("Moment2", "2. Moment")));

        //Measures auflisten
        XYSeries m_p = new XYSeries(new SeriesKey("Particles", "n", "-", Color.magenta));
        XYSeries m_c = new XYSeries(new SeriesKey("Konzentration Messung", "c_measure", "kg/m³", Color.black, AxisKey.CONCENTRATION()));
        XYSeries m_m = new XYSeries(new SeriesKey("Masse Messung", "m_mess", "kg", Color.red, new AxisKey("Mass")));
        XYSeries m_vol = new XYSeries(new SeriesKey("Volumen", "V", "m³", Color.cyan));
        XYSeries m_n = new XYSeries(new SeriesKey("Messungen ", "#", "-", Color.DARK_GRAY));

        double dtm = 1;

//        dtm = (tlm.getEndTime() - tlm.getStartTime()) / (1000. * (tlm.lengthTimes() - 1));
        if (editorpanel != null && editorpanel.getTable() != null) {
            this.collection = editorpanel.getTable().collection;
        } else {
            this.collection = new XYSeriesCollection();
            System.out.println(this.getClass() + ":: editorpanel not yet initialized. create new XYSeriesCollection.");
        }
//        this.collection.removeAllSeries();
        int indexTimePipe = instance.getTimeIndex(time);
        int indexTimeMeasure = ArrayTimeLineMeasurementContainer.instance.getIndexForTime(time);
        int ind = indexTimePipe;
        float[] vs = instance.getVelocityForTimeIndex(ind);
        float[] hs = instance.getWaterlevelsForTimeIndex(ind);
        float[] ms = instance.getMassFluxForTimeIndex(ind, 0);
//        System.out.println("timeindex: " + ind);
        System.out.println(getClass() + ":: ArrayTimeLinePipeContainer.distance=" + instance.distance);
        if (instance.distance != null) {
            for (int i = 0; i < instance.distance.length; i++) {
//            System.out.println(i + "\t" + hs[i] + "m");
                hpipe.addOrUpdate(instance.distance[i], hs[i]);

                v.addOrUpdate(instance.distance[i], vs[i]);
                refMass.addOrUpdate(instance.distance[i], ms[i]);
                refConcentration.addOrUpdate(instance.distance[i], ms[i] / (hs[i]));
////            q.addOrUpdate(time, tl.getFlux(i));
//            hpipe.addOrUpdate(time, tl.getWaterlevel(i));
//            refConcentration.addOrUpdate(time, tl.getMass_reference(i) / tl.getWaterlevel(i));
//            refMass.addOrUpdate(time, tl.getMass_reference(i));
//            if (ArrayTimeLinePipe.distance != null) {
//                moment1_ref.addOrUpdate(time, ArrayTimeLinePipe.getMomentum1_xc(i));
//                moment2_ref.addOrUpdate(time, ArrayTimeLinePipe.getMomentum2_xc(i));
//            }
//            if (ArrayTimeLinePipe.moment1 != null) {
//                moment1_refvorgabe.add(time, ArrayTimeLinePipe.moment1[i]);
//            }
            }
        }

        this.collection.addSeries(v);
//        this.collection.addSeries(q);
        this.collection.addSeries(hpipe);

        if (refConcentration.getMaxY() > 0) {
            this.collection.addSeries(refConcentration);
            this.collection.addSeries(createMovingaverageCentral(refConcentration, 100, "100er Mittel " + refConcentration.getKey()));
        }

        if (refMass.getMaxY() > 0) {
            this.collection.addSeries(refMass);
            XYSeries mr100 = createMovingaverageCentral(refMass, 100, "100er Mittel Masse ref");
            ((SeriesKey) mr100.getKey()).lineColor = Color.GRAY;
            this.collection.addSeries(mr100);
        }

        if (ArrayTimeLineMeasurementContainer.instance != null) {
            ArrayTimeLineMeasurementContainer container = ArrayTimeLineMeasurementContainer.instance;
            ind = container.getIndexForTime(time);
            ms = container.getMassForTimeIndex(ind);
            float[] ps = container.getNumberOfParticlesForTimeIndex(ind);
            float[] cs = container.getConcentrationForTimeIndex(ind);
            int[] ns = container.getNumberOfMeasurementsPerTimestepForTimeIndex(ind);
            System.out.println("distances: " + container.distance.length);
            for (int i = 0; i < ArrayTimeLineMeasurementContainer.distance.length; i++) {
                float d = ArrayTimeLineMeasurementContainer.distance[i];
                m_p.addOrUpdate(d, ps[i]);
                m_m.addOrUpdate(d, ms[i]);
                m_c.addOrUpdate(d, cs[i]);
                m_n.addOrUpdate(d, ns[i]);

            }

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
                this.collection.addSeries(m_m);
                XYSeries m100 = createMovingaverageCentral(m_m, 100, "100 mean Mass");
                ((SeriesKey) m100.getKey()).lineColor = Color.magenta;
                this.collection.addSeries(m100);
            }
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
        panelChartContainer.add(panelChecks, BorderLayout.SOUTH);
    }

    public void updateCheckboxPanel() {
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

    protected void updateShownTimeSeries() {
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
            this.add(panel, BorderLayout.NORTH);
            buttonUpdate.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    SpacelinePanel.this.updateTimeSlider();
                }
            });

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
            this.add(slider, BorderLayout.NORTH);
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

    public static XYSeries createMovingaverageCentral(XYSeries ts, int maxinvolvedPeriods, String name) {
        SeriesKey oldKey = (SeriesKey) ts.getKey();
        Color colorNew = null;
        if (oldKey.lineColor != null) {
            colorNew = new Color(oldKey.lineColor.getRGB() * 300000);
//            System.out.println("Color Old: " + oldKey.lineColor.toString() + " -> new: " + colorNew.toString());
        }
        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, maxinvolvedPeriods + " mean " + oldKey.symbol, oldKey.unit, colorNew, oldKey.axisKey);
        XYSeries average = new XYSeries(newKey);
        int minIndex = maxinvolvedPeriods / 2 + 1;
        int maxIndex = ts.getItemCount() - maxinvolvedPeriods / 2;
        int radius = maxinvolvedPeriods / 2;
        double nenner = (2. * radius + 1.);
        boolean verbose = name.startsWith("c_mat");
        for (int i = minIndex; i < maxIndex; i++) {
            double sum = 0;
            for (int j = i - radius; j < i + radius; j++) {
                sum += ts.getDataItem(j).getY().doubleValue();

            }
            double wert = sum / nenner;
//            if (verbose) {
//                System.out.println(name + " " + p + " :\t" + sum + "/" + nenner + " = " + wert);
//            }
            average.add(ts.getDataItem(i).getX(), wert);
        }

        return average;
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
                this.referenceContainer=((ArrayTimeLinePipe)p.getStatusTimeLine()).container;
                buildPipeSpaceline(referenceContainer, 0);
            }
        }
    }

}
