package view.timeline;

import control.Controller;
import control.LocationIDListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import model.timeline.list.Value;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.TimeLinePipe;
import model.topology.Network;
import model.topology.Pipe;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import view.MapViewer;

/**
 *
 * @author saemann
 */
public class MultiplePipesTimelinesPanel extends CapacityTimelinePanel implements LocationIDListener, ListSelectionListener {

    TimeSeriesCollection storeCollection = new TimeSeriesCollection();
    ArrayList<Pipe> list = new ArrayList<>(5);
    String[] valuekeys = initValueKeys();
    CheckTable seriesCheckTable;
    JScrollPane scrollPaneCheckTable;
    JPanel panelPipes;
    PipeTable pipeTable;
    JButton buttonPanels;
    boolean searchForPipes = false;
    private Network network;
    JButton buttonRefresh;

    /*JScrollbar to adjust the axislimits.
     */
    JPanel panelBars;
    JScrollBar[] limitBars;
    JScrollBar limitTimeStart, limitTimeEnd;
    JFrame frameLimitBars;
    JPanel panelLimitTimeBars;
    long timestart, timeend;

    /**
     *
     * @param title
     * @param mapviewer
     * @param nw
     * @param c
     */
    public MultiplePipesTimelinesPanel(String title, final MapViewer mapviewer, Network nw, Controller c) {
        super(title, c);
        this.collection = new TimeSeriesCollection();
        panelChecks.removeAll();
        this.remove(panelChecks);
        seriesCheckTable = new CheckTable();
        seriesCheckTable.refresh();
        scrollPaneCheckTable = new JScrollPane(seriesCheckTable);
        scrollPaneCheckTable.setMaximumSize(new Dimension(20, 150));
        scrollPaneCheckTable.setMinimumSize(new Dimension(20, 40));
        scrollPaneCheckTable.setPreferredSize(new Dimension(20, 20));
        this.add(scrollPaneCheckTable, BorderLayout.SOUTH);

        //Pipes auswählen
        this.network = nw;
        panelPipes = new JPanel(new BorderLayout());
        pipeTable = new PipeTable();
        panelPipes.add(pipeTable, BorderLayout.CENTER);
        buttonPanels = new JButton("Select Pipes");
        panelPipes.add(buttonPanels, BorderLayout.WEST);
        panelPipes.setPreferredSize(new Dimension(20, 24));
        this.add(panelPipes, BorderLayout.NORTH);
        buttonPanels.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                searchForPipes = !searchForPipes;
                if (searchForPipes) {
                    mapviewer.addListener(MultiplePipesTimelinesPanel.this);
                    buttonPanels.setText("Select Pipe in Map\n Click here to stop selection.");
                } else {
                    buttonPanels.setText("Start Selection");
                    mapviewer.removeListener(MultiplePipesTimelinesPanel.this);
                }
            }
        });
        buttonRefresh = new JButton("Refresh");
        buttonRefresh.setToolTipText("<html>Click to reread the values of each pipe.<br>Use after a simulation has run<br> to update the simulated values.</html>");
        panelPipes.add(buttonRefresh, BorderLayout.EAST);
        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                storeCollection.removeAllSeries();
                for (Pipe p : list) {
                    addPipeTimeline(p);
                }
            }
        });

        this.revalidate();
        XYPlot plot = this.panelChart.getChart().getXYPlot();
        LegendTitle lt = new LegendTitle(plot);
        lt.setItemFont(new Font("Dialog", Font.PLAIN, 12));
        lt.setBackgroundPaint(new Color(255, 255, 255, 255));
        lt.setFrame(new BlockBorder(Color.white));
//        XXYTitleAnnotation ta = new XXYTitleAnnotation(0.98, 0.02, lt, RectangleAnchor.BOTTOM);

    }

    private String[] initValueKeys() {
        return new String[]{"u [m/s]", "Q [m³/s]", "Water lvl [m]", "h up [m]", "h down [m]", "Reynolds Nbr", "Dispersion K [m²/s]", "Particles [1/m³]"};
    }

    @Override
    public void selectLocationID(Object o, String string, long l) {
        for (Pipe pipe : network.getPipes()) {
            if (pipe.getAutoID() == l) {
                pipeTable.addPipe(pipe);
                seriesCheckTable.refresh();
                break;
            }
        }
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public void valueChanged(ListSelectionEvent lse) {
        updateListedTimeSeries();
        updateShownTimeSeries();
    }

    private class CheckTable extends JTable {

        private final DefaultTableModel model = new DefaultTableModel();

        public CheckTable() {
            this.setModel(model);
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent me) {
                    updateListedTimeSeries();
                    updateShownTimeSeries();
                }
            });
        }

        public void refresh() {
            int maxcolumns = 0;
            ArrayList<Value> values = new ArrayList<>(2);

            maxcolumns = valuekeys.length;
            maxcolumns += values.size();
            model.setColumnCount(maxcolumns + 1);
            if (list == null) {
                model.setRowCount(0);
            } else {
                model.setRowCount(list.size());
            }

            String[] columnTitles = new String[maxcolumns + 1];
            columnTitles[0] = "Pipe (id/name)";
            for (int i = 0; i < valuekeys.length; i++) {
                columnTitles[i + 1] = valuekeys[i];
            }
            for (int i = 0; i < values.size(); i++) {
                columnTitles[1 + valuekeys.length + i] = makeTableKey(values.get(i));
            }
            model.setColumnIdentifiers(columnTitles);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    model.setValueAt(list.get(i).getAutoID() + "/" + list.get(i).getName(), i, 0);
                }
            }

            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 1; j < model.getColumnCount(); j++) {
                    model.setValueAt(false, i, j);
                }
            }
            Dimension d = this.getPreferredSize();
            if (scrollPaneCheckTable != null) {
                scrollPaneCheckTable.setPreferredSize(new Dimension(d.width, this.getRowHeight() * model.getRowCount() + 25));
            }
            MultiplePipesTimelinesPanel.this.revalidate();

            final JScrollBar[] newBars = new JScrollBar[columnTitles.length];
            if (limitBars != null) {
                for (int i = 0; i < Math.min(newBars.length, limitBars.length); i++) {
                    newBars[i] = limitBars[i];
                }
            }
            for (int i = 0; i < newBars.length; i++) {
                if (newBars[i] == null) {
                    newBars[i] = new JScrollBar(1);
                    newBars[i].setMaximum(1200);
                    newBars[i].setValue(200);
                    newBars[i].setOrientation(JScrollBar.VERTICAL);
                    newBars[i].setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                }
            }

            if (frameLimitBars == null) {

                frameLimitBars = new JFrame("Axislimits");
                frameLimitBars.setLayout(new BorderLayout());
                frameLimitBars.setBounds(1050, 960, 800, 200);//this.getX() + this.getWidth(), this.getY()+this.getHeight(), 400, 300);
                frameLimitBars.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frameLimitBars.setVisible(true);
                limitTimeStart = new JScrollBar(JScrollBar.HORIZONTAL);
                limitTimeEnd = new JScrollBar(JScrollBar.HORIZONTAL);

                limitTimeEnd.setMaximum(110);
                limitTimeEnd.setValue(110);
                panelLimitTimeBars = new JPanel(new GridLayout(1, 2));
                panelLimitTimeBars.add(limitTimeStart);
                panelLimitTimeBars.add(limitTimeEnd);
                frameLimitBars.add(panelLimitTimeBars, BorderLayout.SOUTH);
                panelBars = new JPanel();
                frameLimitBars.add(panelBars, BorderLayout.CENTER);
                limitTimeEnd.addAdjustmentListener(new AdjustmentListener() {
                    @Override
                    public void adjustmentValueChanged(AdjustmentEvent ae) {
                        updateDomainAxis();
                    }
                });
                limitTimeStart.addAdjustmentListener(limitTimeEnd.getAdjustmentListeners()[0]);
            }
            panelBars.removeAll();
            panelBars.setLayout(new GridLayout(1, newBars.length));
            for (int i = 0; i < newBars.length; i++) {
                JPanel inner = new JPanel(new BorderLayout());
                inner.add(new JLabel(columnTitles[(i)]), BorderLayout.NORTH);
                inner.add(newBars[i], BorderLayout.CENTER);
                panelBars.add(inner, i);
            }

//        frameLimitBars.pack();
            frameLimitBars.revalidate();

            limitBars = newBars;
//            System.out.println("framecomponents: " + frameLimitBars.getComponentCount());
        }

        @Override
        public Class<?> getColumnClass(int i
        ) {
            if (i == 0) {
                return String.class;
            }
            return Boolean.class;
        }

        @Override
        public boolean isCellEditable(int i, int i1
        ) {
            return i1 != 0;
        }

    }

    private String makeTableKey(Value v) {
        return v.getName() + " " + v.getSymbol() + " [" + v.getUnit() + "]";
    }

    private String makeValueKey(Pipe p, String name) {
        return p.getAutoID() + " " + name;
    }

    private String makeValueKey(Pipe p, Value v) {
        return p.getAutoID() + " " + makeTableKey(v);
    }

    private class PipeTable extends JTable {

        DefaultTableModel model = new DefaultTableModel();

        public PipeTable() {
            this.setModel(model);

            this.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent me) {
                    if (me.getButton() != MouseEvent.BUTTON3) {
                        return;
                    }
                    int column = PipeTable.this.columnAtPoint(me.getPoint());
                    pipeTable.removeIndex(column);
                }

            });

        }

        public void refresh() {
            model.setColumnCount(list.size());
            model.setRowCount(1);
            this.setRowHeight(this.getHeight());
            int cwidth = 1;
            if (list.size() > 0) {
                cwidth = this.getWidth() / list.size();
            }
            for (int i = 0; i < list.size(); i++) {
                model.setValueAt(list.get(i).getAutoID() + "/" + list.get(i).getName(), 0, i);
                this.getColumnModel().getColumn(i).setPreferredWidth(cwidth);
                this.getColumnModel().getColumn(i).setMinWidth(cwidth);
            }
        }

        public void addPipe(Pipe p) {
            if (list.contains(p)) {
                return;
            }
            try {
                list.add(p);
                addPipeTimeline(p);
                refresh();
            } catch (Exception e) {
                list.remove(p);
            }
        }

        public void removePipe(Pipe p) {
            list.remove(p);
            refresh();
            seriesCheckTable.refresh();
            updateShownTimeSeries();
        }

        public void removeIndex(int i) {
            try {
                list.remove(i);
            } catch (Exception e) {
            }
            refresh();
            seriesCheckTable.refresh();
            updateShownTimeSeries();
        }
    }

    public void addPipe(Pipe p) {
        pipeTable.addPipe(p);
        seriesCheckTable.refresh();
    }

    private void addPipeTimeline(Pipe p) {
        TimeLinePipe tl = p.getStatusTimeLine();
        TimeSeries v = new TimeSeries(makeValueKey(p, "u [m/s]"), "Time", "m/s");
//        TimeSeries q = new TimeSeries(makeValueKey(p, "Q [m³/s]"), "Time", "m³/s");
        TimeSeries hpipe = new TimeSeries(makeValueKey(p, "Water lvl [m]"), "Time", "m");
        TimeSeries hup = new TimeSeries(makeValueKey(p, "h up [m]"), "Time", "m");
        TimeSeries hdown = new TimeSeries(makeValueKey(p, "h down [m]"), "Time", "m");
        TimeSeries reynolds = new TimeSeries(makeValueKey(p, "Reynolds Nbr"), "Time", "");
        TimeSeries dispersion = new TimeSeries(makeValueKey(p, "Dispersion K [m²/s]"), "Time", "");
//        TimeSeries concentration = new TimeSeries(makeValueKey(p, "Concentration c [kg/m³]"), "Time", "");
        TimeSeries particles = new TimeSeries(makeValueKey(p, "Particles [1/m³]"), "Time", "");
//        this.collection = new TimeSeriesCollection();
        for (int i = 0; i < tl.getNumberOfTimes(); i++) {

            Date d = new Date(p.getStatusTimeLine().getTimeContainer().getTimeMilliseconds(i));
            RegularTimePeriod time = new Minute(d);
            v.addOrUpdate(time, tl.getVelocity(i));
//            q.addOrUpdate(time, tl.getDischarge(i));
//            hup.addOrUpdate(time, tl.getValues().getHup());
//            hdown.addOrUpdate(time, tl.getHdown());
//            reynolds.addOrUpdate(time, tl.getReynolds());
            hpipe.addOrUpdate(time, tl.getWaterlevel(i));
//            dispersion.addOrUpdate(time, s.getValues().getDispersion());
//            concentration.set(time, s.getValues().getSchmutz());
            particles.addOrUpdate(time, tl.getMassflux_reference(i,0));
//            Collection<Value> add = s.getValues().getAdditionalValuesCollection();
//            if (add != null && !add.isEmpty()) {
//                for (Value value : add) {
//                    if (Double.isNaN(value.getValue())) {
//                        continue;
//                    }
//                    String key = makeValueKey(p, value);
//                    TimeSeries ser = storeCollection.getSeries(key);
//                    if (ser == null) {
//                        ser = new TimeSeries(key, "Time", value.getUnit());
////                        System.out.println("added series " + key);
//                        storeCollection.addSeries(ser);
//                    }
//                    ser.addOrUpdate(time, value.getValue());
////                    TimeSeriesDataItem item = ser.getDataItem(time);
////                    if (item != null) {
////                        ser.update(time, item.getValueStamp().doubleValue() + value.getValueStamp());
////                    } else {
////                        ser.set(time, value.getValueStamp());
////                    }
//                }
//            }
        }
        if (this.storeCollection.getSeries(v.getKey()) == null) {
            this.storeCollection.addSeries(v);
        }
//        if (this.storeCollection.getSeries(q.getKey()) == null) {
//            this.storeCollection.addSeries(q);
//        }
        if (this.storeCollection.getSeries(hpipe.getKey()) == null) {
            this.storeCollection.addSeries(hpipe);
        }
        if (reynolds.getMaxY() > 0) {
            if (this.storeCollection.getSeries(reynolds.getKey()) == null) {
                this.storeCollection.addSeries(reynolds);
            }
            if (this.storeCollection.getSeries(dispersion.getKey()) == null) {
                this.storeCollection.addSeries(dispersion);
            }
        } else {
            System.out.println("max Reynolds is 0 -> Do not Load Reynolds & Dispersion timelines");
        }
//        if (this.storeCollection.getSeries(concentration.getKey()) == null) {
//            this.storeCollection.addSeries(concentration);
//        }
        if (particles.getMaxY() > 0) {
            if (this.storeCollection.getSeries(particles.getKey()) == null) {
                this.storeCollection.addSeries(particles);
            }
        }
        if (this.storeCollection.getSeries(hup.getKey()) == null) {
            this.storeCollection.addSeries(hup);
        }
        if (this.storeCollection.getSeries(hdown.getKey()) == null) {
            this.storeCollection.addSeries(hdown);
        }
        seriesCheckTable.refresh();
    }

    protected void updateListedTimeSeries() {

    }

    @Override
    public void updateShownTimeSeries() {
        if (this.collection == null) {
            System.err.println("collection is null");
            return;
        }

        XYPlot plot = panelChart.getChart().getXYPlot();
        plot.clearRangeAxes();
        DefaultTableModel m = (DefaultTableModel) seriesCheckTable.getModel();
        for (int i = 0; i < m.getColumnCount(); i++) {
            plot.setDataset(i, new TimeSeriesCollection());
        }
        collection.removeAllSeries();

        for (int i = 1; i < m.getColumnCount(); i++) {
            for (int j = 0; j < m.getRowCount(); j++) {
                try {
                    boolean b = (boolean) m.getValueAt(j, i);
                    if (!b) {
                        continue;
                    }
                    String key = list.get(j).getAutoID() + " " + m.getColumnName(i);
                    TimeSeries ts = storeCollection.getSeries(key);
                    if (ts == null) {
                        for (Iterator it = storeCollection.getSeries().iterator(); it.hasNext();) {
                            TimeSeries sery = (TimeSeries) it.next();
                        }
                    } else {
                        collection.addSeries(ts);
                        TimeSeriesCollection set = (TimeSeriesCollection) plot.getDataset(i);
                        set.addSeries(ts);
                    }

                } catch (Exception e) {
                    System.err.println("error at " + j + "," + i);
                    System.err.println("m=" + m);
                    System.err.println("v=" + m.getValueAt(j, i));
                    e.printStackTrace();
                }
            }
        }

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        final double[] maxRange = new double[m.getColumnCount()];

//        int index = 0;
        for (int i = 0; i < m.getColumnCount(); i++) {
            renderer = new XYLineAndShapeRenderer(true, false);
            plot.setRenderer(i, renderer);
            String columntitle = m.getColumnName(i);

            Paint rendering = Color.MAGENTA;
            if (columntitle.startsWith("Q")) {
                rendering = (Color.darkGray);
            } else if (columntitle.startsWith("V")) {
                rendering = (Color.MAGENTA);
            } else if (columntitle.startsWith("Water")) {
                rendering = (Color.BLUE);
            } else if (columntitle.startsWith("u")) {
                rendering = (Color.RED);
            } else if (columntitle.startsWith("h_up")) {
                rendering = (Color.CYAN);
            } else if (columntitle.startsWith("h_down")) {
                rendering = (Color.blue.darker());
            } else if (columntitle.startsWith("h ")) {
                rendering = (Color.blue);
            } else if (columntitle.startsWith("Reynolds")) {
                rendering = (Color.green.darker());
            } else if (columntitle.startsWith("HE C")) {
                rendering = new Color(255, 200, 80);//(Color.orange);
            } else if (columntitle.startsWith("HE T")) {
                rendering = new Color(230, 120, 50);//(Color.orange);
            } else if (columntitle.startsWith("Part")) {

                rendering = (Color.GREEN);
                if (columntitle.contains("Ppm")) {
                    rendering = new Color(150, 255, 50);
                } else if (columntitle.contains("Ppcbm")) {
                    rendering = new Color(0, 200, 100);
                }
            } else if (columntitle.startsWith("ana")) {

                rendering = (new Color(165, 230, 75));
            }
            for (int j = 0; j < m.getRowCount(); j++) {
                renderer.setSeriesPaint(j, rendering);

            }
            try {
                limitBars[i].setBackground((Color) rendering);
            } catch (Exception e) {
            }
            if (plot.getDataset(i) != null && plot.getDataset(i).getSeriesCount() > 0) {
                final NumberAxis axis2 = new NumberAxis(columntitle);
                axis2.setAutoRangeIncludesZero(false);
                plot.setRangeAxis(i, axis2);
                plot.mapDatasetToRangeAxis(i, i);
                maxRange[i] = axis2.getRange().getUpperBound();
                final int j = i;
                limitBars[i].addAdjustmentListener(new AdjustmentListener() {
                    @Override
                    public void adjustmentValueChanged(AdjustmentEvent ae) {
//                        axis2.setUpperBound(((limitBars[j].getMaximum() - ae.getValueStamp()) / 1000.) * maxRange[j]);
                        axis2.setUpperBound((1 + (ae.getValue() - 200.) / 1000.) * maxRange[j]);
                    }
                });
            }
        }
        timeend = Long.MIN_VALUE;
        timestart = Long.MAX_VALUE;
        for (Iterator it = collection.getSeries().iterator(); it.hasNext();) {
            TimeSeries ser = (TimeSeries) it.next();
            timeend = Math.max(timeend, ser.getTimePeriod(ser.getItemCount() - 1).getEnd().getTime());
            timestart = Math.min(timestart, ser.getDataItem(0).getPeriod().getStart().getTime());
//            System.out.println("Series "+ser.getDescription()+":  "+ser.getDataItem(0).getPeriod().getStart()+"\t->"+ser.getTimePeriod(ser.getItemCount()-1).getStart());
        }
        updateDomainAxis();
//        timestart = (long) plot.getDomainAxis().getRange().getLowerBound();
//        timeend = (long) plot.getDomainAxis().getRange().getUpperBound();
//        long newStart = timestart;
//        long newend = timeend;
//        long span = timeend - timestart;
//
//        System.out.println("Zeitlich zwischen " + new Date(timestart) + " und " + new Date(timeend));
//        //Zeit Limits anpassen
//        if (limitTimeStart.getValueStamp() > 0) {
//            System.out.println("schneide start weg " + limitTimeStart.getValueStamp() + "/" + limitTimeStart.getMaximum());
//            double fs = limitTimeStart.getValueStamp() / (double) limitTimeStart.getMaximum();
//            long weg = (long) (span * fs);
//            newStart = timestart + weg;
//        }
//        if (limitTimeEnd.getValueStamp() < limitTimeEnd.getMaximum()) {
//            double fs = 1 - (limitTimeEnd.getValueStamp() / (double) limitTimeEnd.getMaximum());
//            long weg = (long) (span * fs);
//            System.out.println("schneide ende weg " + weg + "  " + limitTimeEnd.getValueStamp() + "/" + limitTimeEnd.getMaximum());
//            newend = timeend - weg;
//        }
//
//        System.out.println("Zeitlich nurnoch " + new Date(newStart) + " und " + new Date(newend));
//        if (newStart > newend) {
//            plot.getDomainAxis().setRangeWithMargins((double) newStart, (double) newend);
//        }
//        System.out.println("defaultautorange:"+plot.getDomainAxis().getDefaultAutoRange());
//        System.out.println("Range:"+plot.getDomainAxis().getRange());
//        System.out.println("lowbound:"+plot.getDomainAxis().getLowerBound());
//        System.out.println("lowmargin:"+plot.getDomainAxis().getLowerMargin());
//        plot.getDomainAxis().setLowerBound((double) newStart);
    }

    private void updateDomainAxis() {
        XYPlot plot = panelChart.getChart().getXYPlot();
        long newStart = timestart;
        long newend = timeend;
        long span = timeend - timestart;

//        System.out.println("Zeitlich zwischen " + new Date(timestart) + " und " + new Date(timeend));
        //Zeit Limits anpassen
        if (limitTimeStart.getValue() > 0) {
//            System.out.println("schneide start weg " + limitTimeStart.getValueStamp() + "/" + limitTimeStart.getMaximum());
            double fs = limitTimeStart.getValue() / (double) limitTimeStart.getMaximum();
            long weg = (long) (span * fs);
            newStart = timestart + weg;
        }
        if (limitTimeEnd.getValue() < limitTimeEnd.getMaximum() - limitTimeEnd.getVisibleAmount()) {
            double fs = 1 - (limitTimeEnd.getValue() / (double) (limitTimeEnd.getMaximum() - limitTimeEnd.getVisibleAmount()));
            long weg = (long) (span * fs);
//            System.out.println("schneide ende weg " + weg + "  " + limitTimeEnd.getValueStamp() + "/" + (limitTimeEnd.getMaximum()-limitTimeEnd.getVisibleAmount()));
            newend = timeend - weg;
        }

        if (newStart < newend) {
//            System.out.println("Zeitlich nurnoch " + new Date(newStart) + " und " + new Date(newend));
            plot.getDomainAxis().setRangeWithMargins((double) newStart, (double) newend);
        }
    }
}
