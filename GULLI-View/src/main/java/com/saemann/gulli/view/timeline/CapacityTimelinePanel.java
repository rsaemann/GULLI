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
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeContainer;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;
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
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jtikz.TikzGraphics2D;
import org.jtikz.TikzPDFGraphics2D;

/**
 *
 * @author saemann
 */
public class CapacityTimelinePanel extends JPanel implements CapacitySelectionListener {

    protected ChartPanel panelChart;
    public TimeSeriesCollection collection;
    protected JCheckBox[] checkboxes;
    protected JPanel panelChecks;
    protected ValueMarker marker;
    protected HashMap<String, Boolean> checks = new HashMap<>(20);
    public boolean showSimulationTime = true;

    protected JPanel panelSouth;
    protected JTextField textTitle;
    protected JComboBox<LEGEND_POSITION> comboLegendPosition;

    public boolean showMarkerLabelTime = true;
    protected String title;

    /**
     * Change this to your locale Locale to have axis number format in your
     * local format. standard is US.
     */
    public static Locale FormatLocale = StartParameters.formatLocale;

    protected DecimalFormat numberFormat;

    // public boolean showVelocityInformationInputPoints = true;
    protected DateAxis dateAxis;
    private boolean showSeconds = true;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected int numberUsedDataSetSlots = 0;

    public enum LEGEND_POSITION {

        HIDDEN, OUTER_BOTTOM, OUTER_RIGHT, INNER_TOP_LEFT, INNER_TOP_CENTER, INNER_TOP_RIGHT, INNER_BOTTOM_LEFT, INNER_BOTTOM_CENTER, INNER_BOTTOM_RIGHT, INNER_MID_RIGHT, INNER_MID_LEFT
    }

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
//    protected BasicStroke stroke0 = new BasicStroke(2);
//    protected BasicStroke stroke1 = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{0.1f, 6}, 0);
//    protected BasicStroke stroke2 = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 3, 7, 3}, 0);
//    protected BasicStroke stroke3 = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{1, 3, 2, 5}, 0);
//    protected BasicStroke stroke5Dot = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{1, Float.POSITIVE_INFINITY}, 0);

    public boolean prepareTimelinesInThread = false;

    public final ArrayList<CollectionChangedListener> collectionListener = new ArrayList<>();

    /**
     * read only
     */
    public Capacity actualShown;

    private Font titleFont = new Font(Font.SERIF, Font.ROMAN_BASELINE, 20);

    //Init Timeseries
    //Status
    //Momentum
    private final TimeSeries moment0_particleMass = new TimeSeries(new SeriesKey("M0 \u03a3Mass Ptcl", "", "kg", Color.red, new AxisKey("Moment0", "Mass [kg] ")));

    private final TimeSeries moment1_refvorgabe = new TimeSeries(new SeriesKey("M1 CoM ref", "", "m", Color.GREEN, new AxisKey("Moment", "1. Moment [m]")));
    private final TimeSeries moment1_messung = new TimeSeries(new SeriesKey("M1 CoM ptcl", "", "m", Color.red, new AxisKey("Moment", "1. Moment [m]")));
    private final TimeSeries moment1_delta = new TimeSeries(new SeriesKey("\u0394 M1 (Ptcl-Ref)", "", "m", Color.red.darker(), new AxisKey("Moment", "1. Moment [m]")));
    private final TimeSeries moment1_delta_relative = new TimeSeries(new SeriesKey("Rel. \u0394 M1", "", "-", Color.red, new AxisKey("Relative Error", "Relative Error [-]")));

    private final TimeSeries moment2_ref = new TimeSeries(new SeriesKey("M2 Var ref", "", "m²", Color.GREEN, new AxisKey("Moment2", "2. Moment [m²]")));
    private final TimeSeries moment2_mess = new TimeSeries(new SeriesKey("M2 Var ptcl", "", "m²", Color.red, new AxisKey("Moment2", "2. Moment [m²]")));
    private final TimeSeries moment2_delta = new TimeSeries(new SeriesKey("\u0394 M2 Var (Ptcl-Ref)", "", "m²", Color.orange.darker(), new AxisKey("Moment2", "2. Moment [m²]")));
    private final TimeSeries moment2_variance = new TimeSeries(new SeriesKey("M2 Var ", "", "m²", Color.blue.darker(), new AxisKey("Moment2", "2. Moment [m²]")));
    private final TimeSeries moment2_delta_relative = new TimeSeries(new SeriesKey("Rel. \u0394 M2", "", "-", Color.orange, new AxisKey("Relative Error", "Relative Error [-]")));

    //Measures auflisten
    private final AxisKey keymassFlux = new AxisKey("Mf", "Massflux [kg/s]");
    private final AxisKey keyConcentration = new AxisKey("C", "Concentration [kg/m³]");
    private final TimeSeries m_p = new TimeSeries(new SeriesKey("#Particles", "n", "-", Color.magenta, new AxisKey("Particle")), "Time", "");
    private final TimeSeries m_p_sum = new TimeSeries(new SeriesKey("Sum Particles", "", "-", Color.red, new AxisKey("Particle")), "Time", "");
    private final TimeSeries m_p_l = new TimeSeries(new SeriesKey("Particles/Length", "", "1/m", Color.orange, new AxisKey("Particle per Length")), "Time", "");
    private final TimeSeries m_p_l_sum = new TimeSeries(new SeriesKey("\u03a3Particles/Length", "", "1/m", Color.orange), "Time", "");
    private final TimeSeries m_m = new TimeSeries(new SeriesKey("p. Mass", "m_p", "kg", Color.red, new AxisKey("Mass")), "Time", "");
    private final TimeSeries m_m_sum = new TimeSeries(new SeriesKey(/*\u03a3*/"Sum p. Mass ", "", "kg", Color.pink, new AxisKey("Mass", "Mass [kg]")), "Time", "");
    private final TimeSeries m_vol = new TimeSeries(new SeriesKey("Volumen", "V", "m³", Color.cyan, new AxisKey("Vol", "Volume [m³]")), "Time", "m³");
    private final TimeSeries m_n = new TimeSeries(new SeriesKey("#Measurements ", "#", "-", Color.DARK_GRAY), "Time", "");
    private final TimeSeries v0 = new TimeSeries(new SeriesKey("Velocity", "u", "m/s", Color.red, new AxisKey("V", "Velocity [m/s]"), 0), "Time", "m/s");
    private final TimeSeries q0 = new TimeSeries(new SeriesKey("Discharge", "q", "m³/s", Color.green, new AxisKey("Q", "Discharge [m³/s]"), 0), "Time", "m³/s");

    private final TimeSeries hpipe0 = new TimeSeries(new SeriesKey("Waterlevel", "h", "m", Color.blue, new AxisKey("lvl"), 0), "Time", "m");
    private final TimeSeries volpipe0 = new TimeSeries(new SeriesKey("Volume", "V", "m³", new Color(100, 0, 255), new AxisKey("Vol", "Volume [m³]"), 0), "Time", "m³");
    private final TimeSeries ref_m_sum = new TimeSeries(new SeriesKey("Sum ref Mass ", "", "kg", Color.orange, new AxisKey("Mass", "Mass [kg]")), "Time", "");

    private final TimeSeries refMassfluxTotal = new TimeSeries(new SeriesKey("ref total Massflux", "mf", "kg/s", Color.orange.darker().darker(), keymassFlux, 0), "Time", "");
    private final TimeSeries m_massflux = new TimeSeries(new SeriesKey("p. total Massflux", "mf", "kg/s", Color.orange.darker(), keymassFlux, StrokeEditor.dash1), "Time", "");

    private final TimeSeries refConcentrationTotal = new TimeSeries(new SeriesKey("ref total Concentration", "c", "kg/m³", Color.darkGray.darker(), keyConcentration, 0), "Time", "");
    private final TimeSeries m_c = new TimeSeries(new SeriesKey("p. total Concentration", "c", "kg/m³", Color.darkGray, keyConcentration, StrokeEditor.dash1), "Time", "");

//    private final TimeSeries n_sampl = new TimeSeries(new SeriesKey("Samples taken", "n", "-", Color.magenta, new AxisKey("Samp", "SAmples"), StrokeEditor.stroke2), "Time", "-");
    private final ArrayList<TimeSeries> ref_massFlux_Type = new ArrayList<>(2);
    private final ArrayList<TimeSeries> mes_massFlux_Type = new ArrayList<>(2);

    private final ArrayList<TimeSeries> ref_Concentration_Type = new ArrayList<>(2);
    private final ArrayList<TimeSeries> mes_concentration_Type = new ArrayList<>(2);

    //Manhole only
    private final TimeSeries inflow = new TimeSeries(new SeriesKey("Inflow", "q", "m³/s", Color.green, new AxisKey("Q", "Inflow [m³/s]"), 0), "Time", "m³/s");

    public static final ValueMarker zero_Marker = new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1));

    public CapacityTimelinePanel(String title, Controller c/*, PipeResultData... input*/) {

        super(new BorderLayout());
        panelSouth = new JPanel(new BorderLayout());
        this.title = title;
        this.controller = c;
        Locale.setDefault(FormatLocale);
        this.collection = new TimeSeriesCollection();
        numberFormat = new DecimalFormat();
        numberFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(FormatLocale));
        numberFormat.setMaximumFractionDigits(4);
        initCheckboxpanel();
        setStorage(null, title);
        initChart(title);
        addPDFexport(panelChart, this);
        addLaTeX_TikzExport(panelChart, this);
        addLaTeX_TikzPDFExport(panelChart, this);
        addEMFexport();
        addMatlabSeriesExport();
        addTimeSeriesExport();
        initCollection();
        try {
            if (StartParameters.getPictureExportPath() != null) {
                directoryPDFsave = StartParameters.getPictureExportPath();
            }
        } catch (Exception e) {
        }

        textTitle = new JTextField();
        textTitle.setToolTipText("Title to display in Chart.");

        panelSouth.add(textTitle, BorderLayout.CENTER);
        this.add(panelSouth, BorderLayout.SOUTH);
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

    public void initCollection() {
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_vol.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_m.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_p.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_p_l.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_c.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) refConcentrationTotal.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) refMassfluxTotal.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_massflux.getKey()).name, false);

        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) m_m_sum.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) ref_m_sum.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) hpipe0.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) volpipe0.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) v0.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) q0.getKey()).name, false);

        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment0_particleMass.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment1_delta.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment1_delta_relative.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment1_messung.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment1_refvorgabe.getKey()).name, false);

        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment2_delta.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment2_delta_relative.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment2_variance.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment2_mess.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) moment2_ref.getKey()).name, false);
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

    public void showCheckBoxPanel(boolean showPanel) {
        if (!showPanel) {
            this.remove(panelChecks);
        } else {
            this.add(panelChecks, BorderLayout.SOUTH);
        }
    }

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
//        if (container == null || (container.length == 1 && container[0] == null)) {
//            this.container = new ArrayTimeLinePipeContainer[]{ArrayTimeLinePipeContainer.instance};
//        }
        container = new ArrayList<PipeResultData>(1);
        container.add(controller.getPipeResultData());
//        container = controller.getMultiInputData();
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
//                    if (CapacityTimelinePanel.this.collection != null) {
//                        CapacityTimelinePanel.this.collection.removeAllSeries();
//                    }
                    CapacityTimelinePanel.this.title = title;
                    if (c == null) {

                    } else {
                        CapacityTimelinePanel.this.updateChart("Preparing... " + c);
                        if (c instanceof Pipe) {
                            if (((Pipe) c).getStatusTimeLine() != null || c.getMeasurementTimeLine() != null) {
                                CapacityTimelinePanel.this.buildPipeTimeline(((Pipe) c).getStatusTimeLine(), c.getMeasurementTimeLine(), ((Pipe) c).getLength());
                            }
                        } else if (c instanceof StorageVolume) {
                            CapacityTimelinePanel.this.buildManholeTimeline((StorageVolume) c);
                        } else if (c instanceof SurfaceTriangle) {
                            SurfaceTriangle st = (SurfaceTriangle) c;
//                            if (st.measurement != null) {
//                                CapacityTimelinePanel.this.buildTriangleMeasurementTimeline(st.measurement);
//                            } else {
                            CapacityTimelinePanel.this.collection.removeAllSeries();
                            CapacityTimelinePanel.this.title = "Triangle " + st.getManualID() + " has no measurements.";

//                            }
                        } else {
                            System.out.println(this.getClass() + "::setStorage() : Type " + c.getClass() + "is not known to handle for building Timelines.");
                        }
                    }

                    if (this.isInterrupted()) {
                        System.out.println("Stop Plot preparation (alive? " + isAlive() + ", interrupted? " + isInterrupted() + ")");
                        return;
                    }

                    CapacityTimelinePanel.this.updateCheckboxPanel();

                    CapacityTimelinePanel.this.updateChart(title);

                    CapacityTimelinePanel.this.updateShownTimeSeries();

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

    /**
     * Returns the time from starttime as 1.1.1970 00:00:00
     *
     * @param actualTime
     * @param startTime
     * @return
     */
    public static long calcSimulationTime(long actualTime, long startTime) {
        long time = actualTime - startTime;
        //       int offset = TimeZone.getDefault().getOffset(time);

//        System.out.println("offset: "+offset+" .... input: "+actualTime+"    start: "+startTime+" \t subtracted="+time);
//        time -= offset;
        return time;
    }

    public void markTime(long time) {

//        if (showSimulationTime && controller != null) {
//            time = calcSimulationTime(time, controller.getThreadController().getSimulationStartTime());
//        }
        if (showMarkerLabelTime == false && marker == null) {
//            System.out.println(" out 1");
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

    public void updateDateAxis(TimeContainer tc) {
        if (showSimulationTime) {

            final long start = tc.getFirstTime();
            final long end = tc.getLastTime();
            if (end - start < 1000L * 60 * 60 * 120) {
                //If the simulation is less than 120 minutes, only show minutes and hide hours
                dateAxis.setLabel("Time [min:sec]");
                showSeconds = true;

                dateAxis.setDateFormatOverride(new DateFormat() {
                    @Override
                    public StringBuffer format(Date date, StringBuffer buff, FieldPosition fieldPosition) {
                        //System.out.println("append "+fieldPosition);
                        int seconds = (int) (date.getTime() / 1000);
//                        System.out.println("seconds to display:"+seconds+"   date:"+date.getTime()+"    start0="+start);
                        buff.append(seconds / 60);
                        //System.out.println("Tickunit="+dateAxis.getTickUnit().getUnitType());
                        //System.out.println("time:"+date.getTime()+"\t start:"+start+" -> "+seconds);
                        if (dateAxis.getTickUnit().getUnitType() == DateTickUnitType.SECOND || dateAxis.getTickUnit().getUnitType() == DateTickUnitType.MILLISECOND) {
                            buff.append(":");
                            int zehner = seconds % 60;
                            if (zehner < 10) {
                                buff.append("0");
                            }
                            buff.append(zehner);
                            if (!showSeconds) {
                                showSeconds = true;
                                dateAxis.setLabel("Time [min:sec]");
                            }
                        } else {
                            if (showSeconds) {
                                //Switch back to only minute display
                                showSeconds = false;
                                dateAxis.setLabel("Time [min]");
                            }
                        }
                        return buff;
                    }

                    @Override
                    public Date parse(String source, ParsePosition pos) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                });
            } else {
                dateAxis.setLabel("Time [hour:min]");
                dateAxis.setDateFormatOverride(new SimpleDateFormat());
            }
        }
    }

    private void buildPipeTimeline(TimeLinePipe tl, ArrayTimeLineMeasurement tlm, double pipeLength) {

//        System.out.println("go through timeseries: " + collection.getSeriesCount());
//        for (Iterator it = collection.getSeries().iterator(); it.hasNext();) {
//            TimeSeries se = (TimeSeries) it.next();
//            System.out.println(se.getKey()+"  persists?"+((SeriesKey) se.getKey()).persist);
//            if (((SeriesKey) se.getKey()).persist) {
//                try {
//                    persistent.add((TimeSeries) se.clone());
//                    System.out.println("added persistent timeseries "+se.getKey());
//                } catch (CloneNotSupportedException ex) {
//                    Logger.getLogger(CapacityTimelinePanel.class.getName()).log(Level.SEVERE, null, ex);
//                }
//
//                continue;
//            }
////            System.out.println("setNotify(false) for "+se.getKey());
//            se.setNotify(false);
//            se.clear();
//        }
        for (int i = 0; i < panelChart.getChart().getXYPlot().getRendererCount(); i++) {
            XYItemRenderer r = panelChart.getChart().getXYPlot().getRenderer(i);
            XYDataset ds = panelChart.getChart().getXYPlot().getDataset(i);
            if (r != null && ds != null) {
                for (int j = 0; j < ds.getSeriesCount(); j++) {
                    r.setSeriesVisible(j, false);
                }
            }
        }
        ArrayList<TimeSeries> persistent = new ArrayList<>();
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            TimeSeries series = collection.getSeries(i);
            SeriesKey sk = (SeriesKey) series.getKey();
            if (sk.persist) {
                try {
                    if (sk.containerIndex < 1) {
                        TimeSeries cl = (TimeSeries) series.clone();
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

//                    System.out.println("added persistent timeseries " + cl.getKey());
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(CapacityTimelinePanel.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                series.setNotify(false);
                series.clear();
            }
        }

        this.collection.removeAllSeries();
        this.panelChart.getChart().setNotify(false);
        collection.setNotify(false);

        updateDateAxis(tl.getTimeContainer());

        TimeSeries v, q, vol;
        TimeSeries hpipe;
        v = v0;
        q = q0;
        vol = volpipe0;
        v.clear();
        q.clear();
        vol.clear();
        hpipe = hpipe0;
        hpipe.clear();
        refMassfluxTotal.clear();
        refConcentrationTotal.clear();
        m_massflux.clear();
        for (TimeSeries timeSeries : mes_massFlux_Type) {
            timeSeries.clear();
        }
        for (TimeSeries timeSeries : mes_concentration_Type) {
            timeSeries.clear();
        }
        m_m.clear();
        m_c.clear();
        m_m_sum.clear();
        m_n.clear();
        m_p.clear();
        m_p_sum.clear();
        m_p_l_sum.clear();
        m_p_l.clear();
        m_vol.clear();
        ref_m_sum.clear();
//        n_sampl.clear();

        moment0_particleMass.clear();
        moment1_messung.clear();
        moment1_refvorgabe.clear();
        moment1_delta.clear();
        moment1_delta_relative.clear();
        moment2_ref.clear();
        moment2_mess.clear();
        moment2_delta.clear();
        moment2_delta_relative.clear();
        moment2_variance.clear();

        // other TimeLinePipe implementation
        if (tl != null && tl.getTimeContainer() != null) {
            String[] materialnames = tl.getMaterialNames();
            if (materialnames != null) {
                for (int j = 0; j < materialnames.length; j++) {
                    if (ref_massFlux_Type.size() < j + 1) {
                        SeriesKey key = new SeriesKey("ref Massflux " + materialnames[j], "mf_ref_" + j, "kg/s", Color.orange.darker().darker(), keymassFlux, StrokeEditor.availableStrokes[(j + StrokeEditor.availableStrokes.length + 1) % StrokeEditor.availableStrokes.length]);
                        key.setVisible(((SeriesKey) refMassfluxTotal.getKey()).isVisible());
                        ref_massFlux_Type.add(new TimeSeries(key, "Time", "kg/s"));

                    }
                    if (ref_Concentration_Type.size() < j + 1) {
                        SeriesKey key = new SeriesKey("ref Concentration " + materialnames[j], "c_ref_" + j, "kg/m³", Color.black, keyConcentration, StrokeEditor.availableStrokes[(j + StrokeEditor.availableStrokes.length + 1) % StrokeEditor.availableStrokes.length]);
                        key.setVisible(((SeriesKey) refConcentrationTotal.getKey()).isVisible());
                        ref_Concentration_Type.add(new TimeSeries(key, "Time", "kg/m³"));
                    }
                }
            } else {
                ref_massFlux_Type.clear();
                ref_Concentration_Type.clear();
            }
            if (false) {
                //Calculate moment in time (only for benchmark output. Should be disabled in regular calculation
                //0. Moment
                double m0 = 0;//[kg/s]
                for (int i = 0; i < tl.getTimeContainer().getNumberOfTimes(); i++) {
                    m0 += tl.getMassflux_reference(i, 0);
                }
                //1. moment
                double m1 = 0; //[kg]
                for (int i = 0; i < tl.getTimeContainer().getNumberOfTimes(); i++) {
                    m1 += tl.getMassflux_reference(i, 0) * tl.getTimeContainer().getTimeMilliseconds(i) / 1000.;
                }
                m1 = m1 / m0; //->[s] arrival time of centre of mass
                //2. momentum (breakthrough curve width)
                double m2 = 0; //[kg*s]
                for (int i = 0; i < tl.getTimeContainer().getNumberOfTimes(); i++) {
                    double tterm = tl.getTimeContainer().getTimeMilliseconds(i) / 1000. - m1;
                    m2 += tl.getMassflux_reference(i, 0) * tterm * tterm;
                }
                m2 = m2 / m0;//-> [s^2]
                System.out.println(pipeLength + " m: ref. Mt0: " + m0 + " kg/s\tMT1: " + m1 / 60 + " s\tMt2: " + m2 / 3600 + "min^2");
            }

            double refMassSum = 0;
            for (int i = 0; i < tl.getTimeContainer().getNumberOfTimes(); i++) {
                Date d;
                long timeMilliseconds = tl.getTimeContainer().getTimeMilliseconds(i);
//                timeMilliseconds -= tl.getTimeContainer().getDeltaTimeMS() / 2;

                if (showSimulationTime) {
                    d = new Date(calcSimulationTime(timeMilliseconds, tl.getTimeContainer().getTimeMilliseconds(0)));
                } else {
                    d = new Date(timeMilliseconds);
                }
//                System.out.println("milliseconds: "+d.getTime()+"   "+d);
                RegularTimePeriod time = new Second(d);
                try {
                    v.addOrUpdate(time, tl.getVelocity(i));
                } catch (Exception e) {
                    System.out.println("container.numberoftimes=" + tl.getTimeContainer().getNumberOfTimes());
                    System.out.println("tl.container.numberoftimes=" + tl.getTimeContainer().getNumberOfTimes());
                    System.out.println("i= " + i);

                }
                q.addOrUpdate(time, tl.getDischarge(i));
                hpipe.addOrUpdate(time, tl.getWaterlevel(i));
                vol.addOrUpdate(time, tl.getVolume(i));

                try {
                    moment1_refvorgabe.addOrUpdate(time, ((ArrayTimeLinePipeContainer) tl.getTimeContainer()).moment1[i]);
                    moment2_ref.addOrUpdate(time, ((ArrayTimeLinePipeContainer) tl.getTimeContainer()).moment2[i]);
                } catch (Exception e) {
                }

                double massflux_total = 0;
                for (int j = 0; j < ref_massFlux_Type.size(); j++) {
                    try {
                        ref_massFlux_Type.get(j).addOrUpdate(time, Math.abs(tl.getMassflux_reference(i, j)));
                        massflux_total += Math.abs(tl.getMassflux_reference(i, j));
                    } catch (Exception e) {
                    }
                }
                refMassSum += massflux_total * tl.getTimeContainer().getDeltaTimeMS() / 1000f;
//                System.out.println("refsum: " + refMassSum + " (" + massflux_total + ")");
                ref_m_sum.addOrUpdate(time, refMassSum);

                double concentration_total = 0;
                try {
                    for (int j = 0; j < ref_Concentration_Type.size(); j++) {
                        ref_Concentration_Type.get(j).addOrUpdate(time, Math.abs(tl.getConcentration_reference(i, j)));
                        concentration_total += Math.abs(tl.getConcentration_reference(i, j));
                    }
                } catch (Exception e) {
                }
                if (tl.hasMassflux_reference()) {
                    refConcentrationTotal.addOrUpdate(time, concentration_total);
                    refMassfluxTotal.addOrUpdate(time, massflux_total);
                }
            }

        }

        if (tlm != null && tlm.getContainer() != null) {
            float mass_sum = 0;
            double offset = 0;
            double offset2 = 0;

            for (int j = 0; j < tlm.getContainer().getNumberOfContaminants(); j++) {
                String name;
                Material m = controller.getScenario().getMaterialByIndex(j);
                if (m != null) {
                    name = m.getName();
                } else {
                    name = "" + j;
                }
                if (mes_massFlux_Type.size() < j + 1) {
                    SeriesKey key = new SeriesKey("p. " + name + " Massflux [" + j + "]", "mf", "kg/s", Color.orange.darker(), keymassFlux, StrokeEditor.availableStrokes[(j + StrokeEditor.availableStrokes.length + 1) % StrokeEditor.availableStrokes.length]);
                    key.setVisible(((SeriesKey) m_massflux.getKey()).isVisible());
                    mes_massFlux_Type.add(new TimeSeries(key, "Time", "kg/s"));
                }
                if (mes_concentration_Type.size() < j + 1) {
                    SeriesKey key = new SeriesKey("p. " + name + " Concentration [" + j + "]", "c", "kg/m³", Color.darkGray, keyConcentration, StrokeEditor.availableStrokes[(j + StrokeEditor.availableStrokes.length + 4) % StrokeEditor.availableStrokes.length]);
                    key.setVisible(((SeriesKey) m_c.getKey()).isVisible());
                    mes_concentration_Type.add(new TimeSeries(key, "Time", "kg/m³"));
                }
            }

            if (false) {
                //Calculate moment in time (only for benchmark output. Should be disabled in regular calculation
                //0. Moment
                double m0 = 0;//[kg/s]
                for (int i = 0; i < tlm.getContainer().getNumberOfTimes(); i++) {
                    m0 += tlm.getMass(i, 0);
                }
                //1. moment
                double m1 = 0; //[kg]
                for (int i = 0; i < tlm.getContainer().getNumberOfTimes(); i++) {
                    m1 += tlm.getMass(i, 0) * tlm.getContainer().getTimeMillisecondsAtIndex(i) / 1000.;
                }
                m1 = m1 / m0; //->[s] arrival time of centre of mass
                //2. momentum (breakthrough curve width)
                double m2 = 0; //[kg*s]
                for (int i = 0; i < tlm.getContainer().getNumberOfTimes(); i++) {
                    double tterm = tlm.getContainer().getTimeMillisecondsAtIndex(i) / 1000. - m1;
                    m2 += tlm.getMass(i, 0) * tterm * tterm;
                }
                m2 = m2 / m0;//-> [s^2]
                System.out.println(pipeLength + " m: mes. Mt0: " + m0 + " kg/s\tMT1: " + m1 / 60 + " s\tMt2: " + m2 / 3600 + "min^2");
            }

            long moveVisiblePointToIntervalMid = 0;
            if (!tlm.getContainer().isTimespotmeasurement()) {
                moveVisiblePointToIntervalMid = (long) (-tlm.getContainer().getDeltaTimeS() * 500);
            }

            for (int i = 0; i < tlm.getContainer().getNumberOfTimes(); i++) {
                Date d;
                long timeMeasurement = tlm.getContainer().getMeasurementTimestampAtTimeIndex(i);
                if (timeMeasurement == 0) {
                    timeMeasurement = tlm.getContainer().getTimeMillisecondsAtIndex(i);
                }
                if (!tlm.getContainer().isTimespotmeasurement() && i != 0) {
                    timeMeasurement += moveVisiblePointToIntervalMid;
                }
                int statusTimeIndex = tl.getTimeContainer().getTimeIndex(timeMeasurement);
                double statusTimeIndexDouble = tl.getTimeContainer().getTimeIndexDouble(timeMeasurement);
                if (showSimulationTime) {
                    long timefromstart = calcSimulationTime(timeMeasurement, controller.getThreadController().getStartOffset());
                    d = new Date(timefromstart);//calcSimulationTime(tlm.getContainer().getTimeMillisecondsAtIndex(i), controller.getThreadController().getStartOffset()) + (i == 0 ? 0 : moveVisiblePointToIntervalMid));
//                    System.out.println(getClass()+" show simulation time: ORG: "+statustime+"="+new Date(tlm.getContainer().getTimeMillisecondsAtIndex(i))+"   -> "+d+"  offset: "+controller.getThreadController().getStartOffset()+"="+new Date(controller.getThreadController().getStartOffset())+"   \tdiff:"+timefromstart);
                } else {
                    d = new Date(timeMeasurement);//tlm.getContainer().getTimeMillisecondsAtIndex(i) + (i == 0 ? 0 : moveVisiblePointToIntervalMid));
                }

                RegularTimePeriod time = new Second(d);

                if (tlm.getContainer().distance != null && tl instanceof ArrayTimeLinePipe) {
                    ArrayTimeLinePipeContainer cont = ((ArrayTimeLinePipe) tl).container;

                    //Moment 0 = Total mass
                    double mass = 0.0;
                    for (int j = 0; j < cont.distance.length; j++) {
                        int ti = j * tlm.getContainer().getNumberOfTimes() + i;
                        double m = tlm.getContainer().mass_total[ti];
                        mass += m;
                    }
                    mass /= tlm.getContainer().samplesInTimeInterval[i];
                    moment0_particleMass.addOrUpdate(time, mass);

                    double m1 = tlm.getContainer().getMomentum1_xm(i);
                    if (!Double.isNaN(m1) && m1 > 0) {
                        moment1_messung.addOrUpdate(time, m1);
                        if (cont.moment1 != null) {
                            //Get reference Momentum index to calculate difference and statistics
                            double refTimeIndex = tl.getTimeContainer().getTimeIndexDouble(timeMeasurement);// i * ((TimeIndexContainer) tl.getTimeContainer()).getActualTimeIndex() / (double) tlm.getContainer().getNumberOfTimes();
                            int refTimeIndexInt = (int) refTimeIndex;

                            double refFrac = refTimeIndex % 1;
                            if (refTimeIndex >= cont.moment1.length - 0.5) {
                                //out of bounds
                                continue;
                            }
                            double mref;
                            double m2ref;
                            if (refTimeIndexInt >= cont.moment1.length - 1) {
                                mref = cont.moment1[cont.moment1.length - 1];
                                m2ref = cont.moment2[cont.moment2.length - 1];
                            } else {
                                mref = cont.moment1[refTimeIndexInt] * (1 - refFrac) + cont.moment1[refTimeIndexInt + 1] * (refFrac);
                                m2ref = cont.moment2[refTimeIndexInt] * (1 - refFrac) + cont.moment2[refTimeIndexInt + 1] * (refFrac);
                            }
                            if (i > -1 && mref > 0 && !Double.isNaN(mref)) {
                                moment1_delta.addOrUpdate(time, m1 - mref);
                                moment1_delta_relative.addOrUpdate(time, (m1 - mref) / mref);

                                double m2 = tlm.getContainer().getMomentum2_xm(i, m1);
                                moment2_mess.addOrUpdate(time, m2);

                                moment2_ref.addOrUpdate(time, m2ref);
                                double delta2 = m2 - m2ref;

                                moment2_delta.addOrUpdate(time, delta2);
                                if (i >= 0 && m2ref != 0) {
                                    moment2_delta_relative.addOrUpdate(time, delta2 / m2ref);
                                }
                            }
                        }
                    }
                }

                float vol_c = tlm.getVolume(i);
                if (!Double.isNaN(vol_c)) {
                    m_vol.addOrUpdate(time, vol_c);
                }

                /**
                 * [m^3/s]
                 */
                double discharge = tl.getDischarge((int) statusTimeIndexDouble) * (1 - statusTimeIndexDouble % 1.) + tl.getDischarge(((int) statusTimeIndexDouble) + 1) * (statusTimeIndexDouble % 1.);

                double massFluxSum = 0;
                double massSum = 0;
                /**
                 * Measurement interval [s]
                 */
                double dt = 0;
                if (i > 0) {
                    dt = (tlm.getContainer().getTimeMillisecondsAtIndex(i) - tlm.getContainer().getTimeMillisecondsAtIndex(i - 1)) / 1000.;
                }
                for (int j = 0; j < tlm.getContainer().getNumberOfContaminants(); j++) {
                    double c = tlm.getConcentrationOfType(i, j);
                    if (Double.isNaN(c)) {
                        c = 0;
                    } else if (Double.isInfinite(c)) {
                        c = 0;
                    }
                    double mf = c * discharge;
                    mes_massFlux_Type.get(j).addOrUpdate(time, mf);
                    mes_concentration_Type.get(j).addOrUpdate(time, c);

                    massFluxSum += mf;
                    massSum += c * vol_c;
                }

                mass_sum += massFluxSum * dt;
//                if (massFluxSum > 0) {
//                    System.out.println("Add " + massFluxSum + " * " + dt + "s = " + (massFluxSum * dt) + "\tsum:" + mass_sum);
//                }

                m_massflux.addOrUpdate(time, massFluxSum);
                m_m.addOrUpdate(time, massSum);
                m_m_sum.addOrUpdate(time, mass_sum);

                float c = tlm.getConcentration(i);
                if (Double.isNaN(c)) {
                    m_c.addOrUpdate(time, 0);
                } else if (Double.isInfinite(c)) {
                    m_c.addOrUpdate(time, 0);
                } else {
                    m_c.addOrUpdate(time, c);
                }

                float p = tlm.getParticles(i);

                if (Double.isNaN(p)) {
                    m_p.addOrUpdate(time, 0);
                } else {
                    m_p.addOrUpdate(time, p);
                    m_p_l.addOrUpdate(time, p / pipeLength);

                }
                try {
                    int n = tlm.getParticles_Visited(i);
                    if (n > 0) {
                        m_p_sum.addOrUpdate(time, n);
                    } else {
                        if (Double.isNaN(m_p_sum.getMaxY())) {
                            m_p_sum.addOrUpdate(time, 0);
                        }
                    }
                } catch (Exception e) {
                }

//                double samples = tlm.getContainer().getSamplesInTimeInterval(i);
//                n_sampl.addOrUpdate(time, samples);
            }
        } else {
//            System.out.println("Timeline is initialized?" + ArrayTimeLineMeasurementContainer.isInitialized());
        }

        this.collection.addSeries(v);
        this.collection.addSeries(q);
        this.collection.addSeries(hpipe);
        this.collection.addSeries(vol);
//        if (n_sampl.getMaxY() > 0) {
//            this.collection.addSeries(n_sampl);
//        }

        if (refMassfluxTotal.getMaxY() > 0 && ref_massFlux_Type.size() > 1) {
            this.collection.addSeries(refMassfluxTotal);
        }
        for (int i = 0; i < ref_massFlux_Type.size(); i++) {
            TimeSeries ts = ref_massFlux_Type.get(i);
            if (ts != null && ts.getMaxY() > 0) {
                this.collection.addSeries(ts);
            }
        }

        if (m_massflux.getMaxY() > 0 && mes_massFlux_Type.size() > 1) {
            this.collection.addSeries(m_massflux);
        }
        for (int i = 0; i < mes_massFlux_Type.size(); i++) {
            TimeSeries ts = mes_massFlux_Type.get(i);
            if (ts != null && ts.getMaxY() > 0) {
                this.collection.addSeries(ts);
            }
        }

        if (refConcentrationTotal.getMaxY() > 0 && ref_Concentration_Type.size() > 1) {
            this.collection.addSeries(refConcentrationTotal);
        }
        for (int i = 0; i < ref_Concentration_Type.size(); i++) {
            TimeSeries ts = ref_Concentration_Type.get(i);
            if (ts != null && ts.getMaxY() > 0) {
                this.collection.addSeries(ts);
            }
        }

        if (m_c.getMaxY() > 0 && mes_concentration_Type.size() > 1) {
            this.collection.addSeries(m_c);
        }
        for (int i = 0; i < mes_concentration_Type.size(); i++) {
            TimeSeries ts = mes_concentration_Type.get(i);
            if (ts != null && ts.getMaxY() > 0) {
                this.collection.addSeries(ts);
            }
        }

        if (!moment0_particleMass.isEmpty() && (moment0_particleMass.getMaxY() != 0 || moment0_particleMass.getMinY() != 0)) {
            this.collection.addSeries(moment0_particleMass);
        }

        if (!moment1_refvorgabe.isEmpty() && (moment1_refvorgabe.getMaxY() != 0 || moment1_refvorgabe.getMinY() != 0)) {
            this.collection.addSeries(moment1_refvorgabe);
        }

        if (!moment1_messung.isEmpty() && (moment1_messung.getMaxY() != 0 || moment1_messung.getMinY() != 0)) {
            this.collection.addSeries(moment1_messung);
        }

        if (!moment1_delta.isEmpty() && (moment1_delta.getMaxY() != 0 || moment1_delta.getMinY() != 0)) {
            this.collection.addSeries(moment1_delta);
        }
        if (!moment1_delta_relative.isEmpty() && (moment1_delta_relative.getMaxY() != 0 || moment1_delta_relative.getMinY() != 0)) {
            this.collection.addSeries(moment1_delta_relative);
        }

        if (!moment2_ref.isEmpty() && moment2_ref.getMaxY() != 0) {
            this.collection.addSeries(moment2_ref);
        }

        if (!moment2_mess.isEmpty() && moment2_mess.getMaxY() != 0) {
            this.collection.addSeries(moment2_mess);
        }

        if (!moment2_delta.isEmpty()) {
            this.collection.addSeries(moment2_delta);
        }

        if (!moment2_delta_relative.isEmpty()) {
            this.collection.addSeries(moment2_delta_relative);
        }

        if (!moment2_variance.isEmpty()) {
            this.collection.addSeries(moment2_variance);

        }

        if (m_n.getMaxY() > 0) {
            this.collection.addSeries(m_n);
        }

        if (ref_m_sum.getMaxY() > 0) {
            this.collection.addSeries(ref_m_sum);
        }

        if (m_m_sum.getMaxY() > 0) {
            this.collection.addSeries(m_m_sum);
        }

        if (m_p.getMaxY() > 0) {
            this.collection.addSeries(m_p);
        }

        if (m_p_sum.getMaxY() > 0) {
            this.collection.addSeries(m_p_sum);
        }

        if (m_p_l.getMaxY() > 0) {
            this.collection.addSeries(m_p_l);
        }

        if (m_p_l_sum.getMaxY() > 0) {
            this.collection.addSeries(m_p_l_sum);
        }

        if (m_vol.getMaxY() > 0) {
            this.collection.addSeries(m_vol);
        }

        if (m_m.getMaxY() > 0) {
            this.collection.addSeries(m_m);
        }

        if (!persistent.isEmpty()) {
            for (TimeSeries ts : persistent) {
                this.collection.addSeries(ts);
//                System.out.println("added persistent timeseries " + ts.getKey());
            }
        } else {
//            System.out.println("no peristent timeseries");
        }

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
    }

    private void buildManholeTimeline(StorageVolume vol) {
        TimeSeries h = new TimeSeries(new SeriesKey("Waterheight", "h", "m", Color.BLUE, new AxisKey("h")), "m", "Time");
        TimeSeries lvl = new TimeSeries(new SeriesKey("Waterlvl", "lvl", "m", Color.cyan), "m", "Time");
        TimeSeries lflow = new TimeSeries(new SeriesKey("Flux to Surface", "q_spill", "m³/s", Color.magenta), "m³/s", "Time");

        TimeSeries inflow = new TimeSeries(new SeriesKey("Inflow", "q", "m³/s", Color.green), "m³/s", "Time");
        TimeSeries topHeight = new TimeSeries(new SeriesKey("Top", "z", "m", Color.BLACK, new AxisKey("h")), "m", "Time");
        TimeContainer cont = vol.getStatusTimeLine().getTimeContainer();
        for (int i = 0; i < cont.getNumberOfTimes(); i++) {
            Date d;
            if (showSimulationTime) {
                d = new Date(calcSimulationTime(cont.getTimeMilliseconds(i), cont.getTimeMilliseconds(0))/*controller.getThreadController().getStartOffset()*/);
            } else {
                d = new Date(cont.getTimeMilliseconds(i));
            }
            RegularTimePeriod time = new Millisecond(d);
            h.add(time, vol.getStatusTimeLine().getWaterZ(i));
            lvl.add(time, vol.getStatusTimeLine().getWaterZ(i) - vol.getSole_height());
            lflow.add(time, vol.getStatusTimeLine().getFlowToSurface(i));
            topHeight.add(time, vol.getTop_height());
            inflow.add(time, vol.getStatusTimeLine().getInflow(i));
        }
        ArrayList<TimeSeries> persistent = new ArrayList<>();
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            TimeSeries series = collection.getSeries(i);
            SeriesKey sk = (SeriesKey) series.getKey();
            if (sk.persist) {
                try {
                    if (sk.containerIndex < 1) {
                        TimeSeries cl = (TimeSeries) series.clone();
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

//                    System.out.println("added persistent timeseries " + cl.getKey());
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(CapacityTimelinePanel.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                series.setNotify(false);
                series.clear();
            }
        }

        this.collection.removeAllSeries();
        this.collection.addSeries(h);
        this.collection.addSeries(lvl);
        this.collection.addSeries(lflow);
        this.collection.addSeries(inflow);
        this.collection.addSeries(topHeight);

        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) h.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) lvl.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) lflow.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) inflow.getKey()).name, false);
        StartParameters.enableTimelineVisibilitySaving(((SeriesKey) topHeight.getKey()).name, false);

        if (!persistent.isEmpty()) {
            for (TimeSeries ts : persistent) {
                this.collection.addSeries(ts);
            }
        }

    }

    private void buildSurfaceCellTimeline(int sampleID, int triangleID, Surface surface) {
        this.collection.removeAllSeries();
        if (sampleID >= 0) {
            // Measurements
            int numberOfMaterials = surface.getMeasurementRaster().getNumberOfMaterials();

            TimeSeries[] mass = new TimeSeries[numberOfMaterials];//(new SeriesKey("Mass", "m", "kg", Color.orange, new AxisKey("m")), "kg", "Time");
            TimeSeries[] count = new TimeSeries[numberOfMaterials];//(new SeriesKey("Particles", "N", " ", Color.orange, new AxisKey("N")), " ", "Time");

            TimeSeries mass_sum = new TimeSeries(new SeriesKey("Mass \u03a3", "\u03a3m", "kg", new Color(255, 120, 0), new AxisKey("m", "Mass [kg]")), "kg", "Time");
            TimeSeries count_sum = new TimeSeries(new SeriesKey("Particles \u03a3", "\u03a3N", " ", Color.magenta, new AxisKey("N", "Count")), " ", "Time");
            for (int i = 0; i < count.length; i++) {
                count[i] = new TimeSeries(new SeriesKey("Particles (" + i + ")", "N(" + i + ")", " ", Color.orange, new AxisKey("N", "Count")), " ", "Time");
                mass[i] = new TimeSeries(new SeriesKey("Mass (" + i + ")", "m(" + i + ")", "kg", Color.PINK, new AxisKey("m", "Mass")), "kg", "Time");
            }
            TimeIndexContainer timecontainer = surface.getMeasurementRaster().getIndexContainer();
            SurfaceMeasurementRaster raster = surface.getMeasurementRaster();

            double timescale = 1;
            if (raster.continousMeasurements) {
                timescale = ThreadController.getDeltaTime() / (timecontainer.getDeltaTimeMS() / 1000.);
            }

            for (int i = 0; i < timecontainer.getNumberOfTimes(); i++) {
                Date d;
                if (showSimulationTime) {
                    d = new Date(calcSimulationTime(timecontainer.getTimeMilliseconds(i), timecontainer.getTimeMilliseconds(0)));
                } else {
                    d = new Date(timecontainer.getTimeMilliseconds(i));
                }
                RegularTimePeriod time = new Millisecond(d);
                double mass_s = 0;
                int count_s = 0;
                for (int m = 0; m < numberOfMaterials; m++) {
                    double t_m = raster.getRawMassInCell(sampleID, i, m) * timescale;
                    double t_n = raster.getRawNumberOfParticlesInCell(sampleID, i, m) * timescale;
                    mass[m].addOrUpdate(time, t_m);
                    count[m].addOrUpdate(time, t_n);
                    mass_s += t_m;
                    count_s += t_n;
                }
                mass_sum.addOrUpdate(time, mass_s * timescale);
                count_sum.addOrUpdate(time, count_s * timescale);
            }

            if (numberOfMaterials > 1) {
                this.collection.addSeries(mass_sum);
                this.collection.addSeries(count_sum);
            }
            for (int i = 0; i < numberOfMaterials; i++) {
                this.collection.addSeries(mass[i]);
                this.collection.addSeries(count[i]);
            }
        }
        if (triangleID >= 0) {
            if (controller != null && controller.getSurface() != null) {
                //Status timeline of triangle might have another timecontainer than measurements
                TimeSeries lvl = new TimeSeries(new SeriesKey("Waterlvl", "lvl", "m", Color.cyan, new AxisKey("h", "Waterlevel [m]")), "m", "Time");
                TimeSeries v = new TimeSeries(new SeriesKey("Velocity", "v", "m/s", Color.red, new AxisKey("v", "Velocity [m/s]")), "m/s", "Time");
                Surface surf = controller.getSurface();
                float[] wl = null;
                if (controller.getSurface().getWaterlevels() != null) {
                    wl = controller.getSurface().getWaterlevels()[triangleID];
                }
                float[][] vxy = null;
                if (controller.getSurface().getTriangleVelocity() != null) {
                    vxy = controller.getSurface().getTriangleVelocity()[triangleID];
                }
                if (wl != null) {
                    for (int i = 0; i < wl.length; i++) {
                        Date d;
                        if (showSimulationTime) {
                            d = new Date(calcSimulationTime(surf.getTimes().getTimeMilliseconds(i), surf.getTimes().getTimeMilliseconds(0)));
                        } else {
                            d = new Date(surf.getTimes().getTimeMilliseconds(i));
                        }
                        RegularTimePeriod time = new Millisecond(d);
                        if (wl != null) {
                            lvl.addOrUpdate(time, wl[i]);
                        }
                        if (vxy != null) {
                            //Calculate result velocity
                            float[] vexy = vxy[i];
                            double vres = Math.sqrt(vexy[0] * vexy[0] + vexy[1] * vexy[1]);
                            v.addOrUpdate(time, vres);
                        }

                    }
                }
                if (!lvl.isEmpty()) {
                    this.collection.addSeries(lvl);
                    StartParameters.enableTimelineVisibilitySaving(((SeriesKey) lvl.getKey()).name, false);

                }
                if (!v.isEmpty()) {
                    this.collection.addSeries(v);
                    StartParameters.enableTimelineVisibilitySaving(((SeriesKey) v.getKey()).name, false);
                }
            }
        }

    }

    private void buildTriangleMeasurementTimeline(TriangleMeasurement triM, Surface surface) {

        //Status timeline of triangle might have another timecontainer than measurements
        TimeSeries lvl = new TimeSeries(new SeriesKey("Waterlvl", "lvl", "m", Color.cyan, new AxisKey("h", "Waterlevel [m]")), "m", "Time");
        TimeSeries v = new TimeSeries(new SeriesKey("Velocity", "v", "m/s", Color.red, new AxisKey("v", "Velocity [m/s]")), "m/s", "Time");
        // Measurements
        int numberOfMaterials = triM.getParticlecount().length;

        TimeSeries[] mass = new TimeSeries[numberOfMaterials];//(new SeriesKey("Mass", "m", "kg", Color.orange, new AxisKey("m")), "kg", "Time");
        TimeSeries[] count = new TimeSeries[numberOfMaterials];//(new SeriesKey("Particles", "N", " ", Color.orange, new AxisKey("N")), " ", "Time");

        TimeSeries mass_sum = new TimeSeries(new SeriesKey("Mass \u03a3", "\u03a3m", "kg", new Color(255, 120, 0), new AxisKey("m", "Mass [kg]")), "kg", "Time");
        TimeSeries count_sum = new TimeSeries(new SeriesKey("Particles \u03a3", "\u03a3N", " ", Color.magenta, new AxisKey("N", "Count")), " ", "Time");
        for (int i = 0; i < count.length; i++) {
            count[i] = new TimeSeries(new SeriesKey("Particles (" + i + ")", "N(" + i + ")", " ", Color.orange, new AxisKey("N", "Count")), " ", "Time");
            mass[i] = new TimeSeries(new SeriesKey("Mass (" + i + ")", "m(" + i + ")", "kg", Color.PINK, new AxisKey("m", "Mass")), "kg", "Time");
        }
        TimeIndexContainer timecontainer = surface.getMeasurementRaster().getIndexContainer();

        double timescale = ThreadController.getDeltaTime() / (timecontainer.getDeltaTimeMS() / 1000.);

        for (int i = 0; i < timecontainer.getNumberOfTimes(); i++) {
            Date d;
            if (showSimulationTime) {
                d = new Date(calcSimulationTime(timecontainer.getTimeMilliseconds(i), timecontainer.getTimeMilliseconds(0)));
            } else {
                d = new Date(timecontainer.getTimeMilliseconds(i));
            }
            RegularTimePeriod time = new Millisecond(d);
            double mass_s = 0;
            int count_s = 0;
            for (int j = 0; j < numberOfMaterials; j++) {
                mass[j].addOrUpdate(time, triM.getMassResidence()[j][i] * timescale);
                count[j].addOrUpdate(time, triM.getParticlecount()[j][i] * timescale);
                mass_s += triM.getMassResidence()[j][i];
                count_s += triM.getParticlecount()[j][i];
            }
            mass_sum.addOrUpdate(time, mass_s * timescale);
            count_sum.addOrUpdate(time, count_s * timescale);
        }

        if (controller != null && controller.getSurface() != null) {
            Surface surf = controller.getSurface();
            int id = triM.getTriangleID();
            float[] wl = null;
            if (controller.getSurface().getWaterlevels() != null) {
                wl = controller.getSurface().getWaterlevels()[triM.getTriangleID()];
            }
            float[][] vxy = null;
            if (controller.getSurface().getTriangleVelocity() != null) {
                vxy = controller.getSurface().getTriangleVelocity()[triM.getTriangleID()];
            }
            if (wl != null) {
                for (int i = 0; i < wl.length; i++) {
                    Date d;
                    if (showSimulationTime) {
                        d = new Date(calcSimulationTime(surf.getTimes().getTimeMilliseconds(i), surf.getTimes().getTimeMilliseconds(0)));
                    } else {
                        d = new Date(surf.getTimes().getTimeMilliseconds(i));
                    }
                    RegularTimePeriod time = new Millisecond(d);
                    if (wl != null) {
                        lvl.addOrUpdate(time, wl[i]);
                    }
                    if (vxy != null) {
                        //Calculate result velocity
                        float[] vexy = vxy[i];
                        double vres = Math.sqrt(vexy[0] * vexy[0] + vexy[1] * vexy[1]);
                        v.addOrUpdate(time, vres);
                    }

                }
            }
        }
        this.collection.removeAllSeries();
        if (numberOfMaterials > 1) {
            this.collection.addSeries(mass_sum);
            this.collection.addSeries(count_sum);
        }
        for (int i = 0; i < numberOfMaterials; i++) {
            this.collection.addSeries(mass[i]);
            this.collection.addSeries(count[i]);
        }
        if (!lvl.isEmpty()) {
            this.collection.addSeries(lvl);
        }
        if (!v.isEmpty()) {
            this.collection.addSeries(v);
        }

    }

    private void initCheckboxpanel() {
        panelChecks = new JPanel();
        this.add(panelChecks, BorderLayout.SOUTH);
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

        numberUsedDataSetSlots = 0;
        yAxisMap.clear();

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
//        System.out.println("checkboxes: "+checkboxes.length);
        for (int i = 0; i < checkboxes.length; i++) {
            try {
                if (checkboxes[i].isSelected()) {
                    if (this.collection.getSeries(i) == null) {
                        continue;
                    }
                    SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
                    /**
                     * Baue neues Dataset wenn keine Wiederekennung zu finden
                     * ist
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
                            dataset = (TimeSeriesCollection) plot.getDataset(indexDataset);
                            indexSeries = dataset.getSeriesCount();
                            TimeSeries ts = (TimeSeries) this.collection.getSeries(i);
                            if (key.logarithmic) {
                                makeTimeSeriesAbsolute(ts);
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
                            dataset = new TimeSeriesCollection(this.collection.getSeries(i));

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
                }
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
        JFreeChart chart;
        if (showSimulationTime) {
            chart = ChartFactory.createTimeSeriesChart(title, "Time [hrs:min]", "", collection, true, true, true);
        } else {
            chart = ChartFactory.createTimeSeriesChart(title, "Tageszeit", "", collection, true, true, true);
        }

        XYPlot plot = chart.getXYPlot();

        try {
            dateAxis = (DateAxis) plot.getDomainAxis();
            dateAxis.setTimeZone(TimeZone.getTimeZone("UTC"));
        } catch (Exception e) {
        }
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
                            Logger.getLogger(CapacityTimelinePanel.class
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
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
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
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
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
                    int n = fc.showSaveDialog(CapacityTimelinePanel.this);
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
                                Rectangle rec = CapacityTimelinePanel.this.getBounds();
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
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
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
                    int n = fc.showSaveDialog(CapacityTimelinePanel.this);
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
                            TimeSeries_IO.saveTimeSeriesCollection(output2, prefix, collection);

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addMatlabSeriesExport() {
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
            JMenuItem item = new JMenuItem("Matlab...");
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
                    int n = fc.showSaveDialog(CapacityTimelinePanel.this);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        directoryPDFsave = output.getAbsolutePath();

                        StartParameters.setPictureExportPath(directoryPDFsave);
                        File output2 = new File(output.getAbsolutePath());
                        try {
                            String prefix = "";
                            String capacityname = null;
                            try {
                                if (actualShown instanceof Pipe) {
                                    prefix += "Pipe_" + ((Pipe) actualShown).getName();
                                    capacityname = ((Pipe) actualShown).getName();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            TimeSeries_IO.saveTimeSeriesCollectionAsMatlab(output2, prefix, collection, capacityname, true);

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);

                        } catch (IOException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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

//    /**
//     * Because in step mode every timereries contains every value twice, it is
//     * necessary to skip every second value
//     *
//     * @param ts
//     * @param maxinvolvedPeriods
//     * @param name
//     * @param c
//     * @return
//     */
//    public static TimeSeries createMovingaverageCentral_Steps(TimeSeries ts, int maxinvolvedPeriods, String name, Color c) {
//
//        SeriesKey oldKey = (SeriesKey) ts.getKey();
//        Color colorNew = c;
//        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, maxinvolvedPeriods + " mean " + oldKey.symbol, oldKey.unit, colorNew, oldKey.axisKey);
//        System.out.println("build mean steps " + oldKey.name);
//        newKey.shape = ShapeEditor.SHAPES.DOT;
//        TimeSeries average = new TimeSeries(newKey);
//        int minIndex = maxinvolvedPeriods + 1;
//        int maxIndex = ts.getItemCount() - maxinvolvedPeriods;
//        int radius = maxinvolvedPeriods;
//        double nenner = (radius + 1.);
////        boolean verbose = name.startsWith("c_mat");
//        for (int i = minIndex; i < maxIndex; i = i + 2) {
//            double sum = 0;
//            RegularTimePeriod p = ts.getTimePeriod(i);
//            for (int j = i - radius; j < i + radius; j = j + 2) {
//                sum += ts.getDataItem(j).getValue().doubleValue();
//            }
//            double wert = sum / nenner;
//            average.add(p, wert);
//        }
//        return average;
//    }
    public TimeSeries createConcentrationMovingaverageCentral(ArrayTimeLineMeasurement mtm, int maxinvolvedPeriods) {

        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of Concentration", maxinvolvedPeriods + " mean c", "kg/m³", Color.GREEN, AxisKey.CONCENTRATION());
        TimeSeries average = new TimeSeries(newKey);
        int minIndex = maxinvolvedPeriods / 2 + 1;
        int maxIndex = mtm.getContainer().getNumberOfTimes() - maxinvolvedPeriods / 2;
        int radius = maxinvolvedPeriods / 2;
        double nenner = (2. * radius + 1.);
        for (int i = minIndex; i < maxIndex; i++) {
            double sum = 0;
            Date d;
            if (showSimulationTime) {
                d = new Date(calcSimulationTime(mtm.getContainer().getTimeMillisecondsAtIndex(i), mtm.getContainer().getTimeMillisecondsAtIndex(0))/*controller.getThreadController().getStartOffset()*/);
            } else {
                d = new Date(mtm.getContainer().getTimeMillisecondsAtIndex(i));
            }

            RegularTimePeriod p = new Millisecond(d);
            double counter = 0;
            for (int j = i - radius; j < i + radius; j++) {
                if (mtm.hasValues(j)) {
                    sum += mtm.getConcentration(j);
                }

                counter++;
            }
            double wert = sum / counter;
            average.add(p, wert);
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

    @Override
    public void selectCapacity(Capacity c, Object caller) {
        this.setStorage(c, c.toString());
    }

}
