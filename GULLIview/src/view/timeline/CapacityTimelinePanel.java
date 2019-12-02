package view.timeline;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import control.Controller;
import control.listener.CapacitySelectionListener;
import control.multievents.PipeResultData;
import control.threads.ThreadController;
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
import model.surface.Surface;
import model.surface.SurfaceTriangle;
import model.surface.measurement.TriangleMeasurement;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.timeline.array.TimeContainer;
import model.timeline.array.TimeIndexContainer;
import model.timeline.array.TimeLinePipe;
import model.topology.Capacity;
import model.topology.Pipe;
import model.topology.StorageVolume;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

/**
 *
 * @author saemann
 */
public class CapacityTimelinePanel extends JPanel implements CapacitySelectionListener {

    protected ChartPanel panelChart;
    protected TimeSeriesCollection collection;
    protected JCheckBox[] checkboxes;
    protected JPanel panelChecks;
    protected ValueMarker marker;
    protected HashMap<String, Boolean> checks = new HashMap<>(20);
    public boolean showSimulationTime = true;

    public boolean showMarkerLabelTime = true;
    protected String title;

    public boolean showVelocityInformationInputPoints = true;

    protected DateAxis dateAxis;
    HashMap<String, Integer> yAxisMap = new HashMap<>(10);
    protected int numberUsedDataSetSlots = 0;

    /**
     * Try to make JFreechart look like a matlab plot.
     */
    public static boolean matlabStyle = true;

    protected static String directoryPDFsave = ".";
    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    protected final Controller controller;

    ArrayList<PipeResultData> container;
    protected BasicStroke stroke0 = new BasicStroke(2);
    protected BasicStroke stroke1 = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{0.1f, 6}, 0);
    protected BasicStroke stroke2 = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 3, 7, 3}, 0);
    protected BasicStroke stroke3 = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{1, 3, 2, 5}, 0);
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
    private final TimeSeries moment1_refvorgabe = new TimeSeries(new SeriesKey("1.Moment Referenz", "1.M ref", "m", Color.GREEN, new AxisKey("Moment", "1. Moment")));
    private final TimeSeries moment1_messung = new TimeSeries(new SeriesKey("1.Moment Messung", "1.M messung", "m", Color.red, new AxisKey("Moment", "1. Moment")));
    private final TimeSeries moment1_delta = new TimeSeries(new SeriesKey("Delta 1.Moment Messung-Ref", "Fehler 1.M (messung-ref)", "m", Color.red, new AxisKey("Moment", "1. Moment")));
    private final TimeSeries moment1_delta_relative = new TimeSeries(new SeriesKey("Relative Error 1.Moment", "rel. Error 1. Momentum", "-", Color.red, new AxisKey("Relative Error")));

    private final TimeSeries moment2_ref = new TimeSeries(new SeriesKey("2.Moment Matlab", "2.M ref", "m", Color.GREEN, new AxisKey("Moment2", "2. Moment")));
    private final TimeSeries moment2_mess = new TimeSeries(new SeriesKey("2.Moment Messung", "2.M mess", "m", Color.red, new AxisKey("Moment2", "2. Moment")));
    private final TimeSeries moment2_delta = new TimeSeries(new SeriesKey("Delta 2.Moment Messung-Ref", "Fehler 2.M (messung-ref)", "m", Color.orange, new AxisKey("Moment2", "2. Moment")));
    private final TimeSeries moment2_delta_relative = new TimeSeries(new SeriesKey("Rel. Error 2.Moment Messung-Ref", "rel. Fehler 2.M (messung-ref)", "-", Color.orange, new AxisKey("Moment2", "2. Moment")));

    //Measures auflisten
    private final AxisKey keymassFlux = new AxisKey("Massflux [kg/s]");
    private final AxisKey keyConcentration = new AxisKey("C [kg/m³]");
    private final TimeSeries m_p = new TimeSeries(new SeriesKey("#Particles", "n", "-", Color.magenta, new AxisKey("Particle")), "Time", "");
    private final TimeSeries m_p_sum = new TimeSeries(new SeriesKey("Sum Particles", "Sum(n)", "-", Color.red, new AxisKey("Particle")), "Time", "");
    private final TimeSeries m_p_l = new TimeSeries(new SeriesKey("Particles/Length", "n/L", "1/m", Color.orange, new AxisKey("Particle per Length")), "Time", "");
    private final TimeSeries m_p_l_sum = new TimeSeries(new SeriesKey("\u03a3Particles/Length", "\u03a3n/L", "1/m", Color.orange), "Time", "");
    private final TimeSeries m_c = new TimeSeries(new SeriesKey("Konzentration Messung", "c_measure", "kg/m³", Color.lightGray, keyConcentration), "Time", "");
    private final TimeSeries m_m = new TimeSeries(new SeriesKey("Mass", "m_mess", "kg", Color.red, new AxisKey("Mass")), "Time", "");
    private final TimeSeries m_m_sum = new TimeSeries(new SeriesKey("\u03a3 Mass ", "m_mess", "kg", Color.orange, new AxisKey("Mass")), "Time", "");
    private final TimeSeries m_vol = new TimeSeries(new SeriesKey("Volumen", "V", "m³", Color.cyan,new AxisKey("Vol", "vol")), "Time", "m³");
    private final TimeSeries m_n = new TimeSeries(new SeriesKey("#Measurements ", "#", "-", Color.DARK_GRAY), "Time", "");
    private final TimeSeries v0 = new TimeSeries(new SeriesKey("Velocity", "u", "m/s", Color.red, new AxisKey("V"), 0), "Time", "m/s");
    private final TimeSeries q0 = new TimeSeries(new SeriesKey("Discharge", "q", "m³/s", Color.blue, new AxisKey("Q"), 0), "Time", "m³/s");
    private final TimeSeries massflux = new TimeSeries(new SeriesKey("mes. Massflux", "mf_mes", "kg/s", Color.orange.darker(), keymassFlux, 0), "Time", "");

    private final TimeSeries hpipe0 = new TimeSeries(new SeriesKey("Waterlevel", "h", "m", Color.green, new AxisKey("lvl"), 0), "Time", "m");

    private final TimeSeries refMassflux0 = new TimeSeries(new SeriesKey("ref. Massflux", "mf_ref", "kg/s", Color.orange, keymassFlux, 0), "Time", "");

    private final TimeSeries refConcentration = new TimeSeries(new SeriesKey("ref. Concentration", "c_ref", "kg/m³", Color.darkGray, keyConcentration, 0), "Time", "");

    public CapacityTimelinePanel(String title, Controller c/*, PipeResultData... input*/) {
        super(new BorderLayout());
        this.title = title;
        this.controller = c;

//        this.container = new ArrayTimeLinePipeContainer[input.length];
//        for (int i = 0; i < container.length; i++) {
//            this.container[i]=input[i].getPipeTimeline();            
//        }
        this.collection = new TimeSeriesCollection();
        initCheckboxpanel();
        setStorage(null, title);
        initChart(title);
        addPDFexport();
        addEMFexport();
        addTimeSeriesExport();
    }

    public void showCheckBoxPanel(boolean showPanel) {
        if (!showPanel) {
            this.remove(panelChecks);
        } else {
            this.add(panelChecks, BorderLayout.SOUTH);
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

        container = controller.getMultiInputData();
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
                    if (CapacityTimelinePanel.this.collection != null) {
                        CapacityTimelinePanel.this.collection.removeAllSeries();
                    }
                    CapacityTimelinePanel.this.title = title;
                    if (c == null) {

                    } else {
                        CapacityTimelinePanel.this.updateChart("Preparing... " + c);
                        if (c instanceof Pipe) {
                            CapacityTimelinePanel.this.buildPipeTimeline(((Pipe) c).getStatusTimeLine(), c.getMeasurementTimeLine(), ((Pipe) c).getLength());
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
        int offset = TimeZone.getDefault().getOffset(time);

//        System.out.println("offset: "+offset+" .... input: "+actualTime+"    start: "+startTime+" \t subtracted="+time);
        time -= offset;
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

    private void buildPipeTimeline(TimeLinePipe tl, ArrayTimeLineMeasurement tlm, double pipeLength) {
        this.collection.removeAllSeries();
        moment1_refvorgabe.clear();
        moment1_messung.clear();
        moment1_delta.clear();
        moment1_delta_relative.clear();
        moment2_ref.clear();
        moment2_mess.clear();
        moment2_delta.clear();
        m_p.clear();
        m_p_l.clear();
        m_c.clear();
        m_m.clear();
        m_m_sum.clear();
        m_vol.clear();
        m_n.clear();
        m_p_sum.clear();
        m_p_l_sum.clear();
        massflux.clear();

        TimeSeries v, q;
        TimeSeries hpipe;
        v = v0;
        q = q0;
        v.clear();
        q.clear();
        hpipe = hpipe0;
        hpipe.clear();
        refMassflux0.clear();
        refConcentration.clear();

        // other TimeLinePipe implementation
        if (tl != null && tl.getTimeContainer() != null) {
            for (int i = 0; i < tl.getTimeContainer().getNumberOfTimes(); i++) {
                Date d;
                long timeMilliseconds = tl.getTimeContainer().getTimeMilliseconds(i);
//                timeMilliseconds -= tl.getTimeContainer().getDeltaTimeMS() / 2;

                if (showSimulationTime) {
                    d = new Date(calcSimulationTime(timeMilliseconds, tl.getTimeContainer().getTimeMilliseconds(0)));
                } else {
                    d = new Date(timeMilliseconds);
                }
                RegularTimePeriod time = new Millisecond(d);
                try {
                    v.addOrUpdate(time, tl.getVelocity(i));
                } catch (Exception e) {
                    System.out.println("container.numberoftimes=" + tl.getTimeContainer().getNumberOfTimes());
                    System.out.println("tl.container.numberoftimes=" + tl.getTimeContainer().getNumberOfTimes());
                    System.out.println("i= " + i);

                }
                q.addOrUpdate(time, tl.getDischarge(i));
                hpipe.addOrUpdate(time, tl.getWaterlevel(i));
                if (tl.hasMassflux_reference()) {
//                        refConcentration.addOrUpdate(time, tl.getMassflux_reference(index) / tl.getWaterlevel(index));
                    refConcentration.addOrUpdate(time, tl.getConcentration_reference(i));
                    refMassflux0.addOrUpdate(time, Math.abs(tl.getMassflux_reference(i)));
                }

                try {
                    moment1_refvorgabe.addOrUpdate(time, ((ArrayTimeLinePipeContainer) tl.getTimeContainer()).moment1[i]);
                    moment2_ref.addOrUpdate(time, ((ArrayTimeLinePipeContainer) tl.getTimeContainer()).moment2[i]);
                } catch (Exception e) {
                }
            }
        }

        this.collection.addSeries(v);
        this.collection.addSeries(q);
        this.collection.addSeries(hpipe);

        if (refMassflux0.getMaxY() > 0) {
            this.collection.addSeries(refMassflux0);
        }

        if (refConcentration.getMaxY() > 0) {
            this.collection.addSeries(refConcentration);
//                    this.collection.addSeries(createMovingaverageCentral(refMass, 100, "100 mean Mass ref", Color.gray, isStepTimeSeries));
        }

        if (tlm != null && tlm.getContainer() != null) {
            float mass_sum = 0;
            double offset = -1;
            for (int i = 0; i < tlm.getContainer().getNumberOfTimes(); i++) {
                Date d;
                if (showSimulationTime) {
                    d = new Date(calcSimulationTime(tlm.getContainer().getTimeMillisecondsAtIndex(i), controller.getThreadController().getStartOffset()));
//                    System.out.println(getClass()+" show simulation time: ORG: "+new Date(tlm.getContainer().getTimeMillisecondsAtIndex(i))+"   -> "+d);
                } else {
                    d = new Date(tlm.getContainer().getTimeMillisecondsAtIndex(i));
                }
                RegularTimePeriod time = new Millisecond(d);
//                System.out.println(getClass()+".container.distance="+tlm.getContainer().distance+"\t Timeline:"+tl.getClass());
                if (tlm.getContainer().distance != null && tl instanceof ArrayTimeLinePipe) {
                    ArrayTimeLinePipeContainer cont = ((ArrayTimeLinePipe) tl).container;
                    double m1 = tlm.getContainer().getMomentum1_xc(i);
//                    System.out.println("Moment1 (t="+i+")= "+m);
                    if (!Double.isNaN(m1) && m1 > 0) {
                        moment1_messung.addOrUpdate(time, m1);
                        int refTimeIndex = i * ((TimeIndexContainer) tl.getTimeContainer()).getActualTimeIndex() / tlm.getContainer().getNumberOfTimes();
//                        System.out.println(i + "/" + tlm.getContainer().getNumberOfTimes() + "    measurem: " + refTimeIndex + "/" + tl.getTimeContainer().getNumberOfTimes());
                        double mref = cont.moment1[refTimeIndex];
                        if (i > 0) {
                            if (i == 1) {
                                offset = mref - m1;
                            }
                            moment1_delta.addOrUpdate(time, m1 - mref + offset);
                            moment1_delta_relative.addOrUpdate(time, (m1 - mref + offset) / mref);

                            double m2 = tlm.getContainer().getMomentum2_xc(i, m1);
                            moment2_mess.addOrUpdate(time, m2);
                            moment2_delta.addOrUpdate(time, m2 - cont.getMomentum2_xc(refTimeIndex));
                        }
                    }

                }

                float vol = tlm.getVolume(i);
                if (!Double.isNaN(vol)) {
                    m_vol.addOrUpdate(time, vol);
                }

                float c = tlm.getConcentration(i);
                if (Double.isNaN(c)) {
                    m_c.addOrUpdate(time, 0);
                } else {
                    m_c.addOrUpdate(time, c);
                }

                float mass = tlm.getMass(i);
                if (Double.isNaN(mass)) {
                    m_m.addOrUpdate(time, 0);
                    massflux.addOrUpdate(time, 0);
                } else {
                    m_m.addOrUpdate(time, mass);
                    massflux.add(time, mass * Math.abs(tl.getVelocity(tl.getTimeContainer().getTimeIndex(tlm.getContainer().getTimeMillisecondsAtIndex(i)))) / pipeLength);
                    mass_sum += mass;
                }
                m_m_sum.addOrUpdate(time, mass_sum);

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
            }
        } else {
//            System.out.println("Timeline is initialized?" + ArrayTimeLineMeasurementContainer.isInitialized());
        }
        
         if (massflux.getMaxY() > 0) {
            this.collection.addSeries(massflux);
        }
         if (m_c.getMaxY() > 0) {
            this.collection.addSeries(m_c);
        }

        if (moment1_refvorgabe.getMaxY() > 0) {
            this.collection.addSeries(moment1_refvorgabe);
        }

        if (moment1_messung.getMaxY() > 0) {
            this.collection.addSeries(moment1_messung);
        }

        if (moment1_delta.getMaxY() > 0) {
            this.collection.addSeries(moment1_delta);
        }
        if (moment1_delta_relative.getMaxY() > 0) {
            this.collection.addSeries(moment1_delta_relative);
        }

        if (!moment2_ref.isEmpty() && moment2_ref.getMaxY() > 0) {
            this.collection.addSeries(moment2_ref);
        }

        if (!moment2_mess.isEmpty() && moment2_mess.getMaxY() > 0) {
            this.collection.addSeries(moment2_mess);
        }

        if (!moment2_delta.isEmpty()) {
            this.collection.addSeries(moment2_delta);
        }

        if (m_n.getMaxY() > 0) {
            this.collection.addSeries(m_n);
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

       
//        beforeWasNull = false;

    }

    private void buildManholeTimeline(StorageVolume vol) {
        TimeSeries h = new TimeSeries(new SeriesKey("Waterheight", "h", "m", Color.BLUE, new AxisKey("h")), "m", "Time");
        TimeSeries lvl = new TimeSeries(new SeriesKey("Waterlvl", "lvl", "m", Color.cyan), "m", "Time");
        TimeSeries lflow = new TimeSeries(new SeriesKey("Flux to Surface", "spillout", "m³/s", Color.magenta), "m³/s", "Time");
        TimeSeries topHeight = new TimeSeries(new SeriesKey("Top", "Top", "m", Color.BLACK, new AxisKey("h")), "m", "Time");
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
        }

        this.collection.removeAllSeries();
        this.collection.addSeries(h);
        this.collection.addSeries(lvl);
        this.collection.addSeries(lflow);
        this.collection.addSeries(topHeight);

    }

    private void buildTriangleMeasurementTimeline(TriangleMeasurement triM,Surface surface) {

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
                mass[j].addOrUpdate(time, triM.getMass()[j][i] * timescale);
                count[j].addOrUpdate(time, triM.getParticlecount()[j][i] * timescale);
                mass_s += triM.getMass()[j][i];
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
                if (((SeriesKey) this.collection.getSeriesKey(i)).isVisible) {
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
                        ((SeriesKey) collection.getSeries(index).getKey()).isVisible = c.isSelected();
                    }
                });
            } else {
                checkboxes[i].setSelected(((SeriesKey) this.collection.getSeriesKey(i)).isVisible);
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
        for (int i = 0; i < plot.getDatasetCount(); i++) {
            plot.setDataset(i, null);
        }
        plot.clearRangeAxes();
        numberUsedDataSetSlots = 0;
        yAxisMap.clear();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
//        System.out.println("checkboxes: "+checkboxes.length);
        for (int i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i].isSelected()) {
                if (this.collection.getSeries(i) == null) {
                    continue;
                }
                SeriesKey key = (SeriesKey) collection.getSeries(i).getKey();
                /**
                 * Baue neues Dataset wenn keine Wiederekennung zu finden ist
                 */
                TimeSeriesCollection dataset = null;
                if (key.axis == null || key.axis.name == null) {
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
                    if (yAxisMap.containsKey(key.axis.name)) {
                        indexDataset = yAxisMap.get(key.axis.name);
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
                        yAxisMap.put(key.axis.name, indexDataset);
                        indexSeries = 0;
                        if (key.axis.label != null) {
                            yAxis = new NumberAxis(key.axis.label);
                        } else {
                            yAxis = new NumberAxis("[" + key.unit + "]");
                        }
                        if (key.axis != null) {
                            if (key.axis.manualBounds) {
                                yAxis.setLowerBound(key.axis.lowerBound);
                                yAxis.setUpperBound(key.axis.upperBound);
                            } else {
                                key.axis.lowerBound = yAxis.getLowerBound();
                                key.axis.upperBound = yAxis.getUpperBound();
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
        } catch (Exception e) {
        }
        plot.setBackgroundPaint(Color.WHITE);
        chart.setBackgroundPaint(Color.WHITE);
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
                                int n = fc.showSaveDialog(CapacityTimelinePanel.this);
                                if (n == JFileChooser.APPROVE_OPTION) {
                                    File output = fc.getSelectedFile();
                                    directoryPDFsave = output.getParent();
                                    if (!output.getName().endsWith(".pdf")) {
                                        output = new File(output.getAbsolutePath() + ".pdf");
                                    }
                                    Paint formerBackground = panelChart.getChart().getBackgroundPaint();
                                    try {
                                        panelChart.getChart().setBackgroundPaint(Color.white);
                                        Rectangle rec = CapacityTimelinePanel.this.getBounds();
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
                                        Logger.getLogger(CapacityTimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } catch (DocumentException ex) {
                                        Logger.getLogger(CapacityTimelinePanel.class
                                                .getName()).log(Level.SEVERE, null, ex);
                                    } catch (IOException ex) {
                                        Logger.getLogger(CapacityTimelinePanel.class
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
        } catch (NoClassDefFoundError e) {
            System.err.println("itextpdf libraries not found. PDF export for Timeline Panel disabled.");
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
                                int n = fc.showSaveDialog(CapacityTimelinePanel.this);
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
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            System.err.println("No libraries for emfGraphics found. Disable emf graphics export in " + getClass());
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
        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, maxinvolvedPeriods + " mean " + oldKey.symbol, oldKey.unit, colorNew, oldKey.axis);
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
//        SeriesKey newKey = new SeriesKey(maxinvolvedPeriods + " mean of " + oldKey.name, maxinvolvedPeriods + " mean " + oldKey.symbol, oldKey.unit, colorNew, oldKey.axis);
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
