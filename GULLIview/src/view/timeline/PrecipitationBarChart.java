/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.timeline;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYBarDataset;

/**
 *
 * @author saemann
 */
public class PrecipitationBarChart extends JPanel {

    IntervalXYDataset barcollection;
    TimeSeriesCollection collection;
    JFreeChart chart;
    XYBarRenderer renderer;
    ChartPanel panel;

    public PrecipitationBarChart() {
        super(new BorderLayout());
        collection = new TimeSeriesCollection();
        barcollection = new XYBarDataset(collection, 50);
        chart = ChartFactory.createXYBarChart("Precipitation", "Time", true, "mm", collection, PlotOrientation.VERTICAL, true, true, true);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.red);
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        panel = new ChartPanel(chart, true, true, true, true, true);
        renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setSeriesOutlinePaint(0, Color.BLUE);
        renderer.setSeriesPaint(0, Color.blue.brighter());
        renderer.setSeriesStroke(0, new BasicStroke(10));
        renderer.setDrawBarOutline(true);
        this.add(panel, BorderLayout.CENTER);
    }

//    public static void main(String[] args) {
//        JFrame frame = new JFrame("Precipitationtest");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setBounds(200, 100, 500, 500);
//        frame.setVisible(true);
//        PrecipitationBarChart barpanel = new PrecipitationBarChart();
//        frame.setLayout(new BorderLayout());
//        frame.add(barpanel, BorderLayout.CENTER);
//        frame.revalidate();
//        TimeSeries series=new TimeSeries("Niederschlag 1", "Zeit", "Niederschlag [mm]");
//        series.add(new SpanPeriod(new Minute(0,0,1,1,2000),5),2);
//        series.add(new SpanPeriod(new Minute(5,0,1,1,2000),5),5);
//        series.add(new SpanPeriod(new Minute(7,0,1,1,2000),5),2);
//        barpanel.collection.addSeries(series);
//    }

}
