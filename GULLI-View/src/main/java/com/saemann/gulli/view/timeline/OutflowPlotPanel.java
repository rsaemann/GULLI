package com.saemann.gulli.view.timeline;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.CapacitySelectionListener;
import com.saemann.gulli.core.control.multievents.PipeResultData;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.view.io.timeline.TimeSeries_IO;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.SurfaceTriangle;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.core.model.timeline.analysis.OutletMinimizer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;
import com.saemann.gulli.view.timeline.customCell.ShapeEditor;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import com.saemann.gulli.view.timeline.customCell.StrokeEditor;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jtikz.TikzGraphics2D;
import org.jtikz.TikzPDFGraphics2D;

/**
 *
 * @author saemann
 */
public class OutflowPlotPanel extends JPanel {

    protected ChartPanel panelChart;
    public final XYSeriesCollection collection = new XYSeriesCollection();
    protected JCheckBox[] checkboxes;
    protected JPanel panelChecks;
    protected ValueMarker marker;
    protected HashMap<String, Boolean> checks = new HashMap<>(20);
    public boolean showSimulationTime = true;

    protected JPanel panelSouth;
    protected JTextField textTitle;
    protected JComboBox<LEGEND_POSITION> comboLegendPosition;

//    public boolean showMarkerLabelTime = true;
    protected String title;

    /**
     * Change this to your locale Locale to have axis number format in your
     * local format. standard is US.
     */
    public static Locale FormatLocale = StartParameters.formatLocale;

    protected DecimalFormat numberFormat;

    // public boolean showVelocityInformationInputPoints = true;
//    protected DateAxis dateAxis;
    private NumberAxis xaxisPercent = new NumberAxis("Percent [ % ]");
    private boolean showSeconds = true;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected int numberUsedDataSetSlots = 0;

    protected int intervals = 40;

    public enum LEGEND_POSITION {

        HIDDEN, OUTER_BOTTOM, OUTER_RIGHT, INNER_TOP_LEFT, INNER_TOP_CENTER, INNER_TOP_RIGHT, INNER_BOTTOM_LEFT, INNER_BOTTOM_CENTER, INNER_BOTTOM_RIGHT, INNER_MID_RIGHT, INNER_MID_LEFT
    }

    public enum OPTIMIZATION_STRATEGY {
        CONCENTRATION, MASS
    };

    protected OPTIMIZATION_STRATEGY optimization_strategy = OPTIMIZATION_STRATEGY.CONCENTRATION;

    public float maxLegendwith = 0.4f;

    /**
     * Try to make JFreechart look like a matlab plot.
     */
    public static boolean matlabStyle = true;

    public static String directoryPDFsave = ".";
    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    protected final Controller controller;

    ArrayList<PipeResultData> container;

    public boolean prepareTimelinesInThread = false;

    public final ArrayList<CollectionChangedListener> collectionListener = new ArrayList<>();

    /**
     * read only
     */
    public Capacity actualShown;

    private Font titleFont = new Font(Font.SERIF, Font.ROMAN_BASELINE, 20);

    //Init Series
    //Measures auflisten
    private final AxisKey axiskeymassFlux = new AxisKey("Mf", "Massflux [kg/s]");
    private final AxisKey axiskeymass = new AxisKey("m", "Mass [kg]");
    private final AxisKey axiskeyVolume = new AxisKey("V", "Volume [m³]");
    private final AxisKey axiskeyConcentration = new AxisKey("C", "Concentration [kg/m³]");

    private final SeriesKey key_mass_total_outflow_c = new SeriesKey("Mass outflow by concentration", "", "kg", Color.ORANGE.darker(), axiskeymass, StrokeEditor.stroke2);
    private final SeriesKey key_mass_total_outflow_m = new SeriesKey("Mass outflow by massflux", "", "kg", Color.ORANGE.darker(), axiskeymass, StrokeEditor.dash2);

    private final SeriesKey key_mass_total_holdback_c = new SeriesKey("Mass collected by concentration", "", "kg", Color.green.darker(), axiskeymass, StrokeEditor.stroke2);
    private final SeriesKey key_mass_total_holdback_m = new SeriesKey("Mass collected by massflux", "", "kg", Color.green.darker(), axiskeymass, StrokeEditor.dash2);

    private final SeriesKey key_volume_total_outflow_c = new SeriesKey("total Volume outflow", "V", "m^3", Color.BLUE.darker(), axiskeyVolume, StrokeEditor.stroke2);
    private final SeriesKey key_volume_total_outflow_m = new SeriesKey("total Volume outflow", "V", "m^3", Color.PINK.darker(), axiskeyVolume, StrokeEditor.dash2);

    private final SeriesKey key_volume_total_holdback_c = new SeriesKey("total Volume hold back", "V", "m^3", Color.CYAN.darker(), axiskeyVolume, StrokeEditor.stroke2);
    private final SeriesKey key_volume_total_holdback_m = new SeriesKey("total Volume hold back", "V", "m^3", Color.MAGENTA.darker(), axiskeyVolume, StrokeEditor.dash2);

    private final XYSeries seriesVolumeHoldback_C = new XYSeries(key_volume_total_holdback_c);
    private final XYSeries seriesVolumeOutflow_C = new XYSeries(key_volume_total_outflow_c);
    private final XYSeries seriesMassHoldback_C = new XYSeries(key_mass_total_holdback_c);
    private final XYSeries seriesMassOutflow_C = new XYSeries(key_mass_total_outflow_c);

    private final XYSeries seriesVolumeHoldback_M = new XYSeries(key_volume_total_holdback_m);
    private final XYSeries seriesVolumeOutflow_M = new XYSeries(key_volume_total_outflow_m);
    private final XYSeries seriesMassHoldback_M = new XYSeries(key_mass_total_holdback_m);
    private final XYSeries seriesMassOutflow_M = new XYSeries(key_mass_total_outflow_m);

    private final XYSeries seriesConcentrationMaxOutflow_C = new XYSeries(new SeriesKey("outflow by concentration", "c_max", "kg/m^3", Color.BLACK, axiskeyConcentration, StrokeEditor.stroke2));
    private final XYSeries seriesConcentrationMaxOutflow_M = new XYSeries(new SeriesKey("outflow by massflux", "c_max", "kg/m^3", Color.BLACK, axiskeyConcentration, StrokeEditor.dash2));

    public static final ValueMarker zero_Marker = new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1));

    public OutflowPlotPanel(String title, Controller c) {

        super(new BorderLayout());
        panelSouth = new JPanel(new BorderLayout());
        this.title = title;
        this.controller = c;

        Locale.setDefault(FormatLocale);
        numberFormat = new DecimalFormat();
        numberFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(FormatLocale));
        numberFormat.setMaximumFractionDigits(4);

        collection.addSeries(seriesMassOutflow_C);
        collection.addSeries(seriesVolumeOutflow_C);

        collection.addSeries(seriesMassHoldback_C);
        collection.addSeries(seriesVolumeHoldback_C);

        key_volume_total_holdback_c.setVisible(false);
        key_volume_total_holdback_m.setVisible(false);
        key_volume_total_outflow_c.setVisible(false);
        key_volume_total_outflow_m.setVisible(false);
        key_mass_total_holdback_c.setVisible(false);
        key_mass_total_holdback_m.setVisible(false);

        initCheckboxpanel();
        setStorage(null, title);
        initChart(title);
        addPDFexport(panelChart, this);
        addLaTeX_TikzExport(panelChart, this);
        addLaTeX_TikzPDFExport(panelChart, this);
        addEMFexport();
//        addMatlabSeriesExport();
        addTimeSeriesExport();
        try {
            if (StartParameters.getPictureExportPath() != null) {
                directoryPDFsave = StartParameters.getPictureExportPath();
            }
        } catch (Exception e) {
        }

        textTitle = new JTextField();
        textTitle.setToolTipText("Title to display in Chart.");

        panelSouth.add(textTitle, BorderLayout.CENTER);
        if (panelChecks == null) {
            this.add(panelSouth, BorderLayout.SOUTH);
        }
        textTitle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (panelChart != null) {
                    panelChart.getChart().setTitle(textTitle.getText());
                }
            }
        });
        panelChart.getChart().getXYPlot().clearAnnotations();
        comboLegendPosition = new JComboBox<>(LEGEND_POSITION.values());
        comboLegendPosition.setSelectedIndex(StartParameters.getTimelinePanelLegendPosition());
        panelSouth.add(comboLegendPosition, BorderLayout.EAST);
        comboLegendPosition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLegendPosition((LEGEND_POSITION) comboLegendPosition.getSelectedItem());
            }
        });
        try {
            setLegendPosition(LEGEND_POSITION.values()[StartParameters.getTimelinePanelLegendPosition()]);
        } catch (Exception e) {
        }

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelChart.setMaximumDrawHeight(getHeight());
                panelChart.setMaximumDrawWidth(getWidth());

            }

        });

    }

    public void setLegendPosition(LEGEND_POSITION pos) {
        panelChart.getChart().getXYPlot().clearAnnotations();

        if (pos == LEGEND_POSITION.HIDDEN) {
            panelChart.getChart().getLegend().setVisible(false);
        } else if (pos == LEGEND_POSITION.OUTER_BOTTOM) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.BOTTOM);
            panelChart.getChart().getLegend().setVisible(true);
        } else if (pos == LEGEND_POSITION.OUTER_RIGHT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(true);
        } else if (pos == LEGEND_POSITION.INNER_TOP_RIGHT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(1, 1, panelChart.getChart().getLegend(), RectangleAnchor.TOP_RIGHT);
            annotation.setMaxWidth(maxLegendwith);
            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_TOP_CENTER) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(0.5, 1, panelChart.getChart().getLegend(), RectangleAnchor.TOP);
            annotation.setMaxWidth(maxLegendwith);
            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_TOP_LEFT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(0, 1, panelChart.getChart().getLegend(), RectangleAnchor.TOP_LEFT);
            annotation.setMaxWidth(maxLegendwith);
            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_BOTTOM_RIGHT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(1, 0, panelChart.getChart().getLegend(), RectangleAnchor.BOTTOM_RIGHT);
            annotation.setMaxWidth(maxLegendwith);
            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_BOTTOM_CENTER) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(0.5, 0, panelChart.getChart().getLegend(), RectangleAnchor.BOTTOM);
            annotation.setMaxWidth(maxLegendwith);
            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_BOTTOM_LEFT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(0, 0, panelChart.getChart().getLegend(), RectangleAnchor.BOTTOM_LEFT);
            annotation.setMaxWidth(maxLegendwith);

            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_MID_LEFT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.LEFT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(0, 0.5, panelChart.getChart().getLegend(), RectangleAnchor.LEFT);
            annotation.setMaxWidth(maxLegendwith);

            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        } else if (pos == LEGEND_POSITION.INNER_MID_RIGHT) {
            panelChart.getChart().getLegend().setPosition(RectangleEdge.RIGHT);
            panelChart.getChart().getLegend().setVisible(false);
            XYTitleAnnotation annotation = new XYTitleAnnotation(1, 0.5, panelChart.getChart().getLegend(), RectangleAnchor.RIGHT);
            annotation.setMaxWidth(maxLegendwith);

            panelChart.getChart().getXYPlot().addAnnotation(annotation);
        }
        StartParameters.setTimelinePanelLegendPosition(pos.ordinal());
    }

//    public void showCheckBoxPanel(boolean showPanel) {
//        if (!showPanel) {
//            this.remove(panelChecks);
//        } else {
//            this.add(panelChecks, BorderLayout.SOUTH);
//        }
//    }
    public void showZeroLine(boolean show) {
        if (show) {
            panelChart.getChart().getXYPlot().addRangeMarker(0, zero_Marker, Layer.BACKGROUND);
        } else {
            panelChart.getChart().getXYPlot().removeRangeMarker(zero_Marker);
        }
    }

    public void setStorage(final Capacity c, final String title) {
        if (c == null) {
            return;
        }
        this.actualShown = c;

        if (t != null && t.isAlive()) {
            System.out.println(this.getClass() + ":: interrupt TimelineThread " + t.toString());
            try {
                t.interrupt();
            } catch (Exception e) {
            }
        }
        this.t = new Thread("Capacity Timeline Panel") {
            @Override
            public void run() {

                try {
                    OutflowPlotPanel.this.title = title;
                    if (c == null) {

                    } else {
                        OutflowPlotPanel.this.updateChart("Preparing... " + c);
                        if (c instanceof Pipe) {
                            if (((Pipe) c).getStatusTimeLine() != null || c.getMeasurementTimeLine() != null) {
                                OutflowPlotPanel.this.buildPipePlot(((Pipe) c));
                            }
                        } else {
                            System.out.println(this.getClass() + "::setStorage() : Type " + c.getClass() + "is not known to handle for building Timelines.");
                        }
                    }

                    if (this.isInterrupted()) {
                        System.out.println("Stop Plot preparation (alive? " + isAlive() + ", interrupted? " + isInterrupted() + ")");
                        return;
                    }

                    OutflowPlotPanel.this.updateCheckboxPanel();
                    OutflowPlotPanel.this.updateChart(title);

                    OutflowPlotPanel.this.updateShownTimeSeries();

                    for (CollectionChangedListener ci : collectionListener) {
                        ci.collectionChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        if (prepareTimelinesInThread) {
            t.start();
        } else {
            t.run();
        }
    }

    public void markValue(double value) {

        if (marker == null) {
            marker = new ValueMarker(value, new Color(50, 150, 250, 100), new BasicStroke(1.5f));
            marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));
            marker.setLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            ((XYPlot) this.panelChart.getChart().getPlot()).addDomainMarker(marker);
        }
        marker.setValue(value);
//        if (showMarkerLabelTime) {
//            Date d = new Date(time);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//            marker.setLabel(sdf.format(d));
//        } else {
//            marker.setLabel("");
//        }
//        if (!showMarkerLabelTime) {
//            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
////            System.out.println("removemarker");
//            marker = null;
//        }
    }

    public void removeMarker() {
        if (marker != null) {
            ((XYPlot) this.panelChart.getChart().getPlot()).removeDomainMarker(marker);
            marker = null;
        }
    }

    private void buildPipePlot(Pipe pipe) {

        double maxVolume = OutletMinimizer.totalVolumeDischarge(pipe.getStatusTimeLine());
        double maxMass = OutletMinimizer.totalMassDischarge(pipe.getStatusTimeLine(), pipe.getMeasurementTimeLine());
        OutletMinimizer om = new OutletMinimizer(pipe);

        if (optimization_strategy == OPTIMIZATION_STRATEGY.CONCENTRATION) {
            om.orderByConcentration();
        } else if (optimization_strategy == OPTIMIZATION_STRATEGY.MASS) {
            om.orderByPollutionMass();
        } else {
            System.err.println("Unknown Optimization startegy " + optimization_strategy);
        }

        for (int i = 0; i < panelChart.getChart().getXYPlot().getRendererCount(); i++) {
            XYItemRenderer r = panelChart.getChart().getXYPlot().getRenderer(i);
            XYDataset ds = panelChart.getChart().getXYPlot().getDataset(i);
            if (r != null && ds != null) {
                for (int j = 0; j < ds.getSeriesCount(); j++) {
                    r.setSeriesVisible(j, false);
                }
            }
        }
        ArrayList<XYSeries> persistent = new ArrayList<>();
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            XYSeries series = collection.getSeries(i);
            SeriesKey sk = (SeriesKey) series.getKey();
            if (sk.persist) {
                try {
                    if (sk.containerIndex < 1) {
                        XYSeries cl = (XYSeries) series.clone();
                        SeriesKey skcl = (SeriesKey) sk.clone();
                        skcl.containerIndex = 1 + persistent.size();
                        skcl.persist = true;
                        cl.setKey(skcl);
                        persistent.add(cl);
                        sk.persist = false;
                    } else {
                        persistent.add(series);
                        ((SeriesKey) series.getKey()).containerIndex = persistent.size();
                    }
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(OutflowPlotPanel.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                series.setNotify(false);

            }
            series.clear();
        }

        this.collection.removeAllSeries();
        this.panelChart.getChart().setNotify(false);
        collection.setNotify(false);

        //Prepare supporting values
        double[] targetVolumes = new double[intervals + 1];
        for (int i = 0; i < targetVolumes.length; i++) {
            targetVolumes[i] = maxVolume * i / (double) (intervals);
        }
        targetVolumes[targetVolumes.length - 1] = maxVolume;

        // calculate values ordered by concentration
        for (int i = 0; i < targetVolumes.length; i++) {
            double targetVolume = targetVolumes[i];
            om.findMaximumIntervals(targetVolume);
            seriesMassHoldback_C.add(targetVolume, om.getContainedMass());
            seriesMassOutflow_C.add(targetVolume, om.getEmittedMass());
            seriesVolumeHoldback_C.add(targetVolume, om.getContainedVolume());
            seriesVolumeOutflow_C.add(targetVolume, maxVolume - om.getContainedVolume());
            seriesConcentrationMaxOutflow_C.add(targetVolume, om.getEmittedConcentrationMaximum());
        }
        System.out.println("MaxMass traspassing is " + maxMass);

        System.out.println("Max Y value for mass outflow: " + seriesMassOutflow_C.getMaxY() + "  min: " + seriesMassOutflow_C.getMinY());
        ((SeriesKey) seriesMassOutflow_C.getKey()).shape = ShapeEditor.SHAPES.RECTANGLE_S;

        // calculate values ordered by mass
        om.orderByPollutionMass();

        for (int i = 0; i < targetVolumes.length; i++) {
            double targetVolume = targetVolumes[i];
            om.findMaximumIntervals(targetVolume);
            seriesMassHoldback_M.add(targetVolume, om.getContainedMass());
            seriesMassOutflow_M.add(targetVolume, om.getEmittedMass());
            seriesVolumeHoldback_M.add(targetVolume, om.getContainedVolume());
            seriesVolumeOutflow_M.add(targetVolume, maxVolume - om.getContainedVolume());
            seriesConcentrationMaxOutflow_M.add(targetVolume, om.getEmittedConcentrationMaximum());
        }

        collection.addSeries(seriesMassOutflow_C);
        collection.addSeries(seriesMassOutflow_M);

        collection.addSeries(seriesVolumeOutflow_C);
        collection.addSeries(seriesVolumeOutflow_M);

        collection.addSeries(seriesMassHoldback_C);
        collection.addSeries(seriesMassHoldback_M);

        collection.addSeries(seriesVolumeHoldback_C);
        collection.addSeries(seriesVolumeHoldback_M);

        collection.addSeries(seriesConcentrationMaxOutflow_C);
        collection.addSeries(seriesConcentrationMaxOutflow_M);

        //Layout
        for (int i = 0; i < panelChart.getChart().getXYPlot().getRendererCount(); i++) {
            XYItemRenderer r = panelChart.getChart().getXYPlot().getRenderer(i);
            XYDataset ds = panelChart.getChart().getXYPlot().getDataset(i);
            if (r != null && ds != null) {
                for (int j = 0; j < ds.getSeriesCount(); j++) {
                    r.setSeriesVisible(j, ((SeriesKey) ds.getSeriesKey(j)).isVisible(), false);
                }
            }
        }
        
        showZeroLine(true);
        this.panelChart.getChart().setNotify(true);
        this.panelChart.getChart().fireChartChanged();
        updateCheckboxPanel();
    }

    private void initCheckboxpanel() {
        panelChecks = new JPanel();
        this.add(panelChecks, BorderLayout.SOUTH);
    }

    public void updateCheckboxPanel() {
//        System.out.println("updateCheckBoxPanel");
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
        panelChecks.setLayout(new GridLayout(checkboxes.length / maxcolumns + 2, maxcolumns));
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i] == null || !checkboxes[i].getText().equals(this.collection.getSeriesKey(i).toString())) {
                SeriesKey key = ((SeriesKey) this.collection.getSeriesKey(i));
                Boolean shallBeChecked = checks.get(key.toString());
                if (shallBeChecked == null) {
                    if (key.isVisible()) {
                        shallBeChecked = true;
                    } else {
                        shallBeChecked = false;
                    }
                }
                key.setVisible(shallBeChecked);

                checkboxes[i] = new JCheckBox(key.toString(), shallBeChecked);
                String containeraddition = "";
                if (((SeriesKey) this.collection.getSeriesKey(i)).containerIndex > 0) {
                    containeraddition = " {" + ((SeriesKey) this.collection.getSeriesKey(i)).containerIndex + "}";
                }
                checkboxes[i].setToolTipText(((SeriesKey) this.collection.getSeriesKey(i)).name + " (" + ((SeriesKey) this.collection.getSeriesKey(i)).symbol + ") [" + ((SeriesKey) this.collection.getSeriesKey(i)).unit + "]" + containeraddition);
                final int index = i;
                checkboxes[i].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        JCheckBox c = (JCheckBox) ae.getSource();
                        ((SeriesKey) collection.getSeries(index).getKey()).setVisible(c.isSelected());
                        checks.put(c.getText(), c.isSelected());
                        updateShownTimeSeries();

                    }
                });
            } else {
                checkboxes[i].setSelected(((SeriesKey) this.collection.getSeriesKey(i)).isVisible());
            }
            panelChecks.add(checkboxes[i], i);
        }
        panelChecks.add(textTitle);
        panelChecks.add(comboLegendPosition);
        this.revalidate();
    }

    public void updateShownTimeSeries() {
        if (this.collection == null) {
            return;
        }
//        if (checkboxes == null) {
//            return;
//        }
        XYPlot plot = panelChart.getChart().getXYPlot();
        plot.clearRangeAxes();
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            try {
                plot.setDataset(i, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        numberUsedDataSetSlots = 0;
        yAxisMap.clear();

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
//        System.out.println("checkboxes: "+checkboxes.length);
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            try {

                if (this.collection.getSeries(i) == null) {
                    continue;
                }
                SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();

//                if (checkboxes != null) {
//                    key.setVisible(checkboxes[i].isSelected());
//                }
                if (!key.isVisible()) {
                    continue;
                }
                /**
                 * Baue neues Dataset wenn keine Wiederekennung zu finden ist
                 */
//                TimeSeriesCollection dataset = null;
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

                    NumberAxis axis2 = new NumberAxis(key.label);
                    axis2.setNumberFormatOverride(numberFormat);//NumberFormat.getNumberInstance(FormatLocale));
                    yAxisMap.put(axis2.getLabel(), indexDataset);
                    axis2.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(indexDataset, axis2);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                } else {
                    NumberAxis yAxis;
                    if (yAxisMap.containsKey(key.axisKey.toString())) {
                        indexDataset = yAxisMap.get(key.axisKey.toString());
                        yAxis = (NumberAxis) plot.getRangeAxis(indexDataset);
//                        yAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance(Locale.US));
                        dataset = (XYSeriesCollection) plot.getDataset(indexDataset);
                        indexSeries = dataset.getSeriesCount();
                        XYSeries ts = this.collection.getSeries(i);
//                        if (key.logarithmic) {
//                            makeTimeSeriesAbsolute(ts);
//                        }
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
//                        yAxis.getNumberFormatOverride()..setNumberFormatOverride(numberFormat);//NumberFormat.getNumberInstance(FormatLocale));
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
                                if (s instanceof TimeSeries) {
                                    TimeSeries ts = (TimeSeries) s;
                                    makeTimeSeriesAbsolute(ts);
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

            } catch (Exception ex) {
                ex.printStackTrace();
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
    private void makeTimeSeriesAbsolute(TimeSeries ts) {
        if (ts.getMinY() <= 0) {
            for (int j = 0; j < ts.getItemCount(); j++) {
                double v = ts.getValue(j).doubleValue();
                if (v == 0) {
                    ts.update(j, Double.NaN);
                } else if (v < 0) {
                    ts.update(j, Math.abs(v));
                }
            }
        }
    }

    private void initChart(String title) {
        if (title == null) {
            title = "";
        }
        JFreeChart chart = ChartFactory.createXYLineChart(title, "Volume [m³]", "", collection, PlotOrientation.VERTICAL, true, true, true);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        chart.getLegend().setFrame(new BlockBorder(Color.lightGray));//, new BasicStroke(2), RectangleInsets.ZERO_INSETS));

        this.panelChart = new ChartPanel(chart) {
            @Override
            public void paintComponent(Graphics g) {
                try {
                    super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.

                } catch (Exception e) {
                    System.err.println("Paintexception in Chartpanel catched: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        };
        this.add(panelChart, BorderLayout.CENTER);
        panelChart.setMaximumDrawHeight((int) (250));
        panelChart.setMaximumDrawWidth((int) (500));
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

    public static void addPDFexport(final ChartPanel panelChart, final JComponent surroundingContainer) {
        JPopupMenu menu = panelChart.getPopupMenu();
        try {
            try {
                File dir = new File(directoryPDFsave);
                if (dir.exists()) {
                    if (dir.isFile()) {
                        dir = dir.getParentFile();
                    }
                    panelChart.setDefaultDirectoryForSaveAs(dir);
                }
            } catch (Exception e) {
                System.out.println("Save directory:'" + directoryPDFsave + "'");
                e.printStackTrace();
            }
            int index = 3; //usually at the 3rd position
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    String label = m.getActionCommand().toLowerCase();
                    if (label.contains("save") || label.contains("speich")) {
                        index = i;
                    }
                }
            }
            JMenuItem item = new JMenuItem("PDF...");
            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
                //Add at the very end if the correct position could not be found
                menu.add(item);
            } else {
                JMenu m = (JMenu) menu.getComponent(index);
                m.add(item, 0);
            }

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
                    int n = fc.showSaveDialog(panelChart);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        if (output.exists()) {
                            if (JOptionPane.showConfirmDialog(panelChart, "Override existing file?", output.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                                return;
                            }
                        }
                        if (output.exists() && !output.canWrite()) {
                            if (JOptionPane.showOptionDialog(panelChart, "Cannot write on File", "Close process that locks " + output.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Retry", "Cancle"}, "Retry") != 0) {
                                System.err.println("Do not write PDF");
                                return;
                            }
                        }
                        directoryPDFsave = output.getParent();
                        panelChart.setDefaultDirectoryForSaveAs(output.getParentFile());
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".pdf")) {
                            output = new File(output.getAbsolutePath() + ".pdf");
                        }
                        Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                        try {
                            panelChart.getChart().setBackgroundPaint(Color.white);
                            Rectangle rec = new Rectangle(0, 0, panelChart.getMaximumDrawWidth(), panelChart.getMaximumDrawHeight());

                            System.out.println("craw in size " + rec + " instead of " + panelChart.getMaximumSize());

                            Document doc = new Document(new com.itextpdf.text.Rectangle(0, 0, rec.width, rec.height));
                            FileOutputStream fos = new FileOutputStream(output);
                            PdfWriter writer = PdfWriter.getInstance(doc, fos);
                            doc.open();
                            PdfContentByte cb = writer.getDirectContent();
                            PdfTemplate tp = cb.createTemplate((float) rec.getWidth(), (float) rec.getHeight());
                            PdfGraphics2D g2d = new PdfGraphics2D(cb, (float) rec.getWidth(), (float) rec.getHeight());
                            g2d.translate(-surroundingContainer.getX(), 0);// -surroundingContainer.getY());
                            panelChart.getChart().draw(g2d, rec);
                            cb.addTemplate(tp, 25, 200);
                            g2d.dispose();
                            doc.close();
                            fos.close();

                        } catch (Exception ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                            JOptionPane.showMessageDialog(panelChart, ex.getLocalizedMessage(), "PDF writer exception", JOptionPane.ERROR_MESSAGE);
                        } finally {

                        }
                        panelChart.getChart().setBackgroundPaint(formerBackground);
                    }
                }
            });

        } catch (NoClassDefFoundError e) {
            System.err.println("itextpdf libraries not found. PDF export for Timeline Panel disabled.");
        }
    }

    public static void addLaTeX_TikzExport(final ChartPanel panelChart, final JComponent surroundingContainer) {
        JPopupMenu menu = panelChart.getPopupMenu();
        try {
//            panelChart.setDefaultDirectoryForSaveAs(new File(directoryPDFsave));
            int index = 3; //usually at the 3rd position
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    String label = m.getActionCommand().toLowerCase();
                    if (label.contains("save") || label.contains("speich")) {
                        index = i;
                    }
                }
            }
            JMenuItem item = new JMenuItem("LaTeX/TikZ...");
            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
                //Add at the very end if the correct position could not be found
                menu.add(item);
            } else {
                JMenu m = (JMenu) menu.getComponent(index);
                m.add(item, 0);
            }

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JFileChooser fc = new JFileChooser(directoryPDFsave);
                    fc.setFileFilter(new FileNameExtensionFilter("LaTeX File", new String[]{"tex", "tikz"}));
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int n = fc.showSaveDialog(panelChart);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        if (output.exists()) {
                            if (JOptionPane.showConfirmDialog(panelChart, "Override existing file?", output.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                                return;
                            }
                        }

                        directoryPDFsave = output.getParent();
                        panelChart.setDefaultDirectoryForSaveAs(output.getParentFile());
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".tex")) {
                            output = new File(output.getAbsolutePath() + ".tex");
                        }
                        Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                        try {
                            panelChart.getChart().setBackgroundPaint(Color.white);
                            Rectangle rec = new Rectangle(0, 0, panelChart.getMaximumDrawWidth(), panelChart.getMaximumDrawHeight());

//                            System.out.println("craw in size " + rec + " instead of " + panelChart.getMaximumSize());
                            try (FileOutputStream fos = new FileOutputStream(output)) {
                                TikzGraphics2D g2d = new TikzGraphics2D(fos, rec);
//                            g2d.translate(-surroundingContainer.getX(), 0);// -surroundingContainer.getY());
                                panelChart.getChart().draw(g2d, rec);

                                g2d.finalize();
                                System.out.println("Created file " + output);
                            }

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } finally {

                        }
                        panelChart.getChart().setBackgroundPaint(formerBackground);
                    }
                }
            }
            );
        } catch (NoClassDefFoundError e) {
            System.err.println("itextpdf libraries not found. PDF export for Timeline Panel disabled.");
        }
    }

    public static void addLaTeX_TikzPDFExport(final ChartPanel panelChart, final JComponent surroundingContainer) {
        JPopupMenu menu = panelChart.getPopupMenu();
        try {
            int index = 3; //usually at the 3rd position
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    String label = m.getActionCommand().toLowerCase();
                    if (label.contains("save") || label.contains("speich")) {
                        index = i;
                    }
                }
            }
            JMenuItem item = new JMenuItem("TikZ&PDF...");
            item.setToolTipText("Create a tikz picture with every but text packed into a PDF.");
            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
                //Add at the very end if the correct position could not be found
                menu.add(item);
            } else {
                JMenu m = (JMenu) menu.getComponent(index);
                m.add(item, 0);
            }

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JFileChooser fc = new JFileChooser(directoryPDFsave);

                    fc.setFileFilter(new FileNameExtensionFilter("LaTeX File", new String[]{"tex", "tikz"}));
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int n = fc.showSaveDialog(panelChart);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        if (output.exists()) {
                            if (JOptionPane.showConfirmDialog(panelChart, "Override existing file?", output.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                                return;
                            }
                        }

                        directoryPDFsave = output.getParent();
                        panelChart.setDefaultDirectoryForSaveAs(output.getParentFile());
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".tex")) {
                            output = new File(output.getAbsolutePath() + ".tex");
                        }
                        Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                        try {
                            panelChart.getChart().setBackgroundPaint(Color.white);
                            Rectangle rec = new Rectangle(0, 0, panelChart.getWidth(), panelChart.getHeight());

                            TikzPDFGraphics2D g2d = new TikzPDFGraphics2D(output.getParentFile(), output.getName(), rec);

                            panelChart.setMaximumDrawWidth((int) rec.getWidth());
                            panelChart.setMinimumDrawWidth((int) rec.getWidth());
                            panelChart.setMaximumDrawHeight((int) rec.getHeight());
                            panelChart.setMinimumDrawHeight((int) rec.getHeight());

                            panelChart.getChart().draw(g2d, rec);

                            g2d.finalize();
                            System.out.println("Created file " + output);

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } finally {

                        }
                        panelChart.getChart().setBackgroundPaint(formerBackground);
                    }
                }
            }
            );
        } catch (NoClassDefFoundError e) {
            System.err.println("itextpdf libraries not found. PDF export for Timeline Panel disabled.");
        }
    }

    private void addEMFexport() {
        try {
            JPopupMenu menu = this.panelChart.getPopupMenu();
            int index = 3; //usually at the 3rd position
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    String label = m.getActionCommand().toLowerCase();
                    if (label.contains("save") || label.contains("speich")) {
                        index = i;
                    }
                }
            }
            JMenuItem item = new JMenuItem("EMF...");
            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
                //Add at the very end if the correct position could not be found
                menu.add(item);
            } else {
                JMenu m = (JMenu) menu.getComponent(index);
                m.add(item, 0);
            }
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
                    int n = fc.showSaveDialog(OutflowPlotPanel.this);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        directoryPDFsave = output.getParent();
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".emf")) {
                            output = new File(output.getAbsolutePath() + ".emf");
                        }
                        Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                        try {
                            panelChart.getChart().setBackgroundPaint(Color.white);
                            try (OutputStream out = new java.io.FileOutputStream(output)) {
                                Rectangle rec = OutflowPlotPanel.this.getBounds();
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
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                        panelChart.getChart().setBackgroundPaint(formerBackground);
                    }
                }
            });

        } catch (NoClassDefFoundError e) {
            System.err.println("No libraries for emfGraphics found. Disable emf graphics export in " + getClass());
        }
    }

    private void addTimeSeriesExport() {
        try {
            JPopupMenu menu = this.panelChart.getPopupMenu();
            int index = 3; //usually at the 3rd position
            for (int i = 0; i < menu.getComponentCount(); i++) {
                if (menu.getComponent(i) instanceof JMenu) {
                    JMenu m = (JMenu) menu.getComponent(i);
                    String label = m.getActionCommand().toLowerCase();
                    if (label.contains("save") || label.contains("speich")) {
                        index = i;
                    }
                }
            }
            JMenuItem item = new JMenuItem("Series...");
            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
                //Add at the very end if the correct position could not be found
                menu.add(item);
            } else {
                JMenu m = (JMenu) menu.getComponent(index);
                m.add(item, 0);
            }
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
                    int n = fc.showSaveDialog(OutflowPlotPanel.this);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        directoryPDFsave = output.getAbsolutePath();
                        File output2 = new File(output.getAbsolutePath());
                        try {
                            String prefix = "";
                            try {
                                if (actualShown instanceof Pipe) {
                                    prefix += "_" + ((Pipe) actualShown).getName();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            TimeSeries_IO.saveXYSeriesCollection(output2, prefix, collection);

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(OutflowPlotPanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    private void addMatlabSeriesExport() {
//        try {
//            JPopupMenu menu = this.panelChart.getPopupMenu();
//            int index = 3; //usually at the 3rd position
//            for (int i = 0; i < menu.getComponentCount(); i++) {
//                if (menu.getComponent(i) instanceof JMenu) {
//                    JMenu m = (JMenu) menu.getComponent(i);
//                    String label = m.getActionCommand().toLowerCase();
//                    if (label.contains("save") || label.contains("speich")) {
//                        index = i;
//                    }
//                }
//            }
//            JMenuItem item = new JMenuItem("Matlab...");
//            if (index < 0 || !(menu.getComponent(index) instanceof JMenu)) {
//                //Add at the very end if the correct position could not be found
//                menu.add(item);
//            } else {
//                JMenu m = (JMenu) menu.getComponent(index);
//                m.add(item, 0);
//            }
//            item.addActionListener(new ActionListener() {
//
//                @Override
//                public void actionPerformed(ActionEvent ae) {
//                    JFileChooser fc = new JFileChooser(directoryPDFsave) {
//
//                        @Override
//                        public boolean accept(File file) {
//                            if (file.isDirectory()) {
//                                return true;
//                            }
//                            return false;
//                        }
//                    };
//                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//                    int n = fc.showSaveDialog(OutflowPlotPanel.this);
//                    if (n == JFileChooser.APPROVE_OPTION) {
//                        File output = fc.getSelectedFile();
//                        directoryPDFsave = output.getAbsolutePath();
//
//                        StartParameters.setPictureExportPath(directoryPDFsave);
//                        File output2 = new File(output.getAbsolutePath());
//                        try {
//                            String prefix = "";
//                            String capacityname = null;
//                            try {
//                                if (actualShown instanceof Pipe) {
//                                    prefix += "Pipe_" + ((Pipe) actualShown).getName();
//                                    capacityname = ((Pipe) actualShown).getName();
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            TimeSeries_IO.saveDatasetAsMatlab(output2, prefix, collection, capacityname, true);
//
//                        } catch (FileNotFoundException ex) {
//                            Logger.getLogger(OutflowPlotPanel.class
//                                    .getName()).log(Level.SEVERE, null, ex);
//
//                        } catch (IOException ex) {
//                            Logger.getLogger(OutflowPlotPanel.class
//                                    .getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    public XYSeriesCollection getCollection() {

        return collection;
    }

    public void setCapacity(Capacity c) {
        if (c == null) {
            return;
        }
        this.setStorage(c, c.toString());
    }

    public void setOptimizationStartegy(OPTIMIZATION_STRATEGY strategy) {
        this.optimization_strategy = strategy;
        this.setStorage(actualShown, title);
    }

}
