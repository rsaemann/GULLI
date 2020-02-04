package view.timeline;

import control.Controller;
import io.extran.Raingauge_Firebird;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JScrollPane;
import model.timeline.RainGauge;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import view.timeline.customCell.ShapeEditor;

/**
 *
 * @author saemann
 */
public class PrecipitationTimelinePanel extends CapacityTimelinePanel {

    /**
     * Thread building the timelines in Background.
     */
    private Thread t;

    private Font titleFont = new Font(Font.SERIF, Font.ROMAN_BASELINE, 20);

//    private final ArrayList<Regenreihe_Firebird> regenreihen = new ArrayList<>(5);
    private final AxisKey axisKey = new AxisKey("P", "Precipitation [mm]");

    public boolean autoUpdateGUI = true;

    private final JScrollPane scrollchecks;

    private Shape lineShapeForLegend = new Line2D.Float(-7, 3, 7, 3);

    //Init Timeseries
    //Status
    //Momentum
    public PrecipitationTimelinePanel(String title, Controller c) {
        super(title, c);
        this.remove(panelChecks);
        scrollchecks = new JScrollPane(panelChecks);
        Dimension maxDim = new Dimension(100, 200);
        this.scrollchecks.setMaximumSize(maxDim);
        scrollchecks.setPreferredSize(maxDim);
        this.add(scrollchecks, BorderLayout.SOUTH);
    }

    @Override
    public void showCheckBoxPanel(boolean showPanel) {
        if (!showPanel) {
            this.remove(scrollchecks);
        } else {
            this.add(scrollchecks, BorderLayout.SOUTH);
        }
    }

    public boolean addRegenreihe(Raingauge_Firebird regenreihe) {
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            SeriesKey key = (SeriesKey) collection.getSeriesKey(i);
            if (key.name.equals(regenreihe.getName())) {
                return false;
            }
        }
        int index = collection.getSeriesCount();
        if (regenreihe.getTimes() == null) {
            regenreihe.convertByteTofloat();
        }
        TimeSeries ts = createRegenreihe(regenreihe.getName(), index, regenreihe.getTimes(), -regenreihe.getTimes()[0], regenreihe.getPrecipitation(), regenreihe.getName());
        collection.addSeries(ts);
        if (autoUpdateGUI) {
            updateCheckboxPanel();
            updateShownTimeSeries();
        }
        return true;
    }

    public int addRegenreihe(String name, long[] times, double[] precipitations, String file) {
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            SeriesKey key = (SeriesKey) collection.getSeriesKey(i);
            if (key.name.equals(name)) {
                return -1;
            }
        }
        int index = collection.getSeriesCount();
        TimeSeries ts = createRegenreihe(name, index, times, -times[0], precipitations, file, null);
        collection.addSeries(ts);
        if (autoUpdateGUI) {
            updateCheckboxPanel();
            updateShownTimeSeries();
        }
        return index;
    }

    public TimeSeries createRainGaugeTimeSeries(String name, long starttime, int intervallMinutes, double[] precipitations, String file) {
        return PrecipitationTimelinePanel.this.createRainGaugeTimeSeries(name, starttime, 0, intervallMinutes, precipitations, file);
    }

    public TimeSeries createRainGaugeTimeSeries(RainGauge raingauge) {
        return this.createRainGaugeTimeSeries(raingauge.toString(), raingauge.getTimes()[0], 0, raingauge.getIntervallMinutes(), raingauge.getPrecipitation(), null, raingauge);
    }

    public TimeSeries createRainGaugeIntervalTimeSeries(RainGauge raingauge) {
        SeriesKey<RainGauge> key = new SeriesKey<>("Raingauge", "P", "mm", Color.blue, new AxisKey("mm"));
        key.element = raingauge;
        int offset = new GregorianCalendar().getTimeZone().getOffset(raingauge.getTimes()[0]);
//        System.out.println("  dateoffset:"+new GregorianCalendar().getTimeZone().getOffset(raingauge.getTimes()[0]));
//        System.out.println("offset: "+offset);
        key.setVisible(false);
        TimeSeries ts = new TimeSeries(key);
        final long timespan = raingauge.getIntervallMinutes() * 60000L;
        for (int i = 0; i < raingauge.getPrecipitation().length; i++) {
            double niederschlagshoehe = raingauge.getPrecipitation()[i];
            long time = raingauge.getTimes()[i];
            if (showSimulationTime) {
//                time += timesShift;
                time -= offset;
            }
            final long calctime = time;
            RegularTimePeriod s = new RegularTimePeriod() {

                @Override
                public RegularTimePeriod previous() {
                    return new Minute(new Date(calctime - timespan));
                }

                @Override
                public RegularTimePeriod next() {
                    return new Minute(new Date(calctime + timespan));
                }

                @Override
                public long getSerialIndex() {
                    return calctime;
                }

                @Override
                public void peg(Calendar clndr) {

                }

                @Override
                public long getFirstMillisecond() {
                    return calctime;
                }

                @Override
                public long getFirstMillisecond(Calendar clndr) {
                    return calctime;
                }

                @Override
                public long getLastMillisecond() {
                    return calctime + timespan - 1;
                }

                @Override
                public long getLastMillisecond(Calendar clndr) {
                    return calctime + timespan - 1;
                }

                @Override
                public int compareTo(Object t) {
                    if (!(t instanceof RegularTimePeriod)) {
                        return 0;
                    }
                    return (int) (this.getFirstMillisecond() - ((RegularTimePeriod) t).getFirstMillisecond());
                }

            };

            ts.add(s, niederschlagshoehe);
        }
        key.timeseries = ts;

        return ts;

    }

    public TimeSeries createRainGaugeTimeSeries(RainGauge raingauge, int offset) {
        return this.createRainGaugeTimeSeries(raingauge.toString(), raingauge.getTimes()[0], offset, raingauge.getIntervallMinutes(), raingauge.getPrecipitation(), null, raingauge);
    }

    public TimeSeries createRainGaugeTimeSeries(String name, long starttime, int intervalShift, int intervallMinutes, double[] precipitations, String file) {
        return PrecipitationTimelinePanel.this.createRainGaugeTimeSeries(name, starttime, intervalShift, intervallMinutes, precipitations, file, null);
    }

    public <E> TimeSeries createRainGaugeTimeSeries(String name, long starttime, int intervalShift, int intervallMinutes, double[] precipitations, String file, E q) {
//        for (int i = 0; i < collection.getSeriesCount(); i++) {
//            SeriesKey key = (SeriesKey) collection.getSeriesKey(i);
//            if (key.name.equals(name)) {
//                return collection.getSeries(i);
//            }
//        }
        int index = collection.getSeriesCount();
        long[] times = new long[precipitations.length];
        for (int i = 0; i < times.length; i++) {
            times[i] = starttime + i * (intervallMinutes * 60000L);
        }
        GregorianCalendar cal = new GregorianCalendar();
        long offset = 0;
        if (times.length > 0) {
            offset = -times[0];
        }
        TimeSeries ts = createRegenreihe(name, index, times, offset - cal.get(GregorianCalendar.DST_OFFSET) + intervalShift * intervallMinutes * 60000L, precipitations, file, q);
        return ts;

    }

    private TimeSeries createRegenreihe(String name, final int index, long[] times, long timesShift, double[] precipitations, String file) {
        return createRegenreihe(name, index, times, timesShift, precipitations, file, null);
    }

    private <E> TimeSeries createRegenreihe(String name, final int index, long[] times, long timesShift, double[] precipitations, String file, E q) {
        SeriesKey<E> key = new SeriesKey<E>(name, name, "mm", Color.blue, axisKey, index, file);
        key.label = name + " (" + index + ")";

        key.element = q;
        int offset = new GregorianCalendar().getTimeZone().getRawOffset();
        key.setVisible(false);
        TimeSeries ts = new TimeSeries(key);
        for (int i = 0; i < precipitations.length; i++) {
            double niederschlagshoehe = precipitations[i];
            long time = times[i];
            if (showSimulationTime) {
                time += timesShift;
                time -= offset;
            }
            Second s = new Second(new Date(time));
            ts.add(s, niederschlagshoehe);
        }
        key.timeseries = ts;
        return ts;
    }

    public void addRegenreihen(Collection<Raingauge_Firebird> collection) {

        for (Raingauge_Firebird rr : collection) {
            this.addRegenreihe(rr);
        }
        if (autoUpdateGUI) {
            updateCheckboxPanel();
            updateShownTimeSeries();
        }
    }

    @Override
    public void updateCheckboxPanel() {

        super.updateCheckboxPanel();
    }

    @Override
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
        XYItemRenderer renderer = plot.getRenderer();
        int indexDataset = 0;
        int indexSeries = 0;
        LegendItemCollection legendItems = new LegendItemCollection();

        for (int i = 0; i < collection.getSeriesCount(); i++) {
            TimeSeries series = collection.getSeries(i);

            if (series == null) {
                return;
            }
            SeriesKey key = (SeriesKey) series.getKey();
            if (checkboxes != null) {
                key.setVisible(checkboxes[i].isSelected());
            }
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
                dataset = new TimeSeriesCollection(series);
                plot.setDataset(indexDataset, dataset);

//                     System.out.println("Render "+key.name+" as linechart");
                renderer = new XYLineAndShapeRenderer(true, false);
                plot.setRenderer(indexDataset, renderer);

                NumberAxis axis2 = new NumberAxis(key.label);
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
                    yAxisMap.put(yAxis.getLabel(), indexDataset);
                    if (key.renderAsBar) {
//                        System.out.println("Render " + key.name + " as barchart");
                        XYBarRenderer barr = new XYBarRenderer(0);

                        barr.setSeriesOutlinePaint(indexSeries, Color.BLUE);
                        barr.setSeriesFillPaint(indexSeries, key.lineColor);
                        barr.setSeriesOutlineStroke(indexSeries, key.stroke);
//                        barr.setBarAlignmentFactor(5);
                        barr.setDrawBarOutline(true);
                        barr.setShadowVisible(false);
//                        barr.setUseYInterval(true);

//                        series.getDataItem(0).getPeriod().getFirstMillisecond();
                        barr.setBarPainter(new StandardXYBarPainter());

                        renderer = barr;
                        panelChart.getChart().getXYPlot().setRenderer(indexDataset, renderer);
                    } else {
                        renderer = new XYLineAndShapeRenderer(true, false);
                        renderer.setSeriesStroke(indexSeries, key.stroke);
                        plot.setRenderer(indexDataset, renderer);
                    }
                    yAxis.setAutoRangeIncludesZero(false);

                    plot.setRangeAxis(indexDataset, yAxis);
                    plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
                    dataset = new TimeSeriesCollection(this.collection.getSeries(i));
                    plot.setDataset(indexDataset, dataset);
                }
                plot.mapDatasetToRangeAxis(indexDataset, indexDataset);
            }

            if (renderer instanceof XYLineAndShapeRenderer) {
                ((XYLineAndShapeRenderer) renderer).setDrawSeriesLineAsPath(true);
            }
            if (key.lineColor != null) {
                renderer.setSeriesPaint(indexSeries, key.lineColor);
            }
            if (key.stroke != null) {
                renderer.setSeriesStroke(indexSeries, key.stroke);
                renderer.setSeriesVisible(indexSeries, true);
            } else {
                renderer.setSeriesVisible(indexSeries, false);
            }

            if (key.shape != null && key.shape.getShape() != null) {
                renderer.setSeriesShape(indexSeries, key.shape.getShape());
                if (renderer instanceof XYLineAndShapeRenderer) {
                    XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) renderer;
                    r.setSeriesShapesFilled(indexSeries, key.shapeFilled);
                    r.setSeriesShapesVisible(i, true);
                }

                renderer.setSeriesVisible(indexSeries, true);
            } else {
                renderer.setSeriesShape(indexSeries, null);
            }
            indexDataset++;
            LegendItem legendItem = new LegendItem(key.label, key.lineColor) {

                @Override
                public boolean isShapeFilled() {
                    return false;
                }

                @Override
                public boolean isShapeOutlineVisible() {
                    return true;
                }

            };
            legendItem.setLineStroke(key.stroke);
            legendItem.setLine(lineShapeForLegend);
            legendItem.setLinePaint(key.lineColor);
            legendItem.setLineVisible(true);
            if (key.shape != null && key.shape != ShapeEditor.SHAPES.EMPTY) {

                legendItem.setShape(key.getShape());
                legendItem.setShapeVisible(true);
                legendItem.setOutlineStroke(new BasicStroke(1f));
                legendItem.setOutlinePaint(key.lineColor);
            } else {
                legendItem.setShapeVisible(false);
            }

//            System.out.println("filled?" + legendItem.isShapeFilled() + " outline? " + legendItem.isShapeOutlineVisible());
            legendItems.add(legendItem);
//            System.out.println("set description of legend " + i + " to " + key.label);
        }
        plot.setFixedLegendItems(legendItems);
        if (matlabStyle) {

            MatlabLayout.layoutToMatlab(this.panelChart.getChart());
            this.panelChart.getChart().setBackgroundPaint(Color.white);
        } else {

        }

    }
}
