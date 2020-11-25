/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.view.view.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleInsets;

/**
 *
 * @author saemann
 */
public class MatlabLayout {

    public static void layoutToMatlab(JFreeChart chart) {
        chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        XYPlot plot = chart.getXYPlot();
        chart.setBackgroundPaint(new Color(240, 240, 240));
        BasicStroke stroke1 = new BasicStroke(1);

        DecimalFormat df = new DecimalFormat();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        df.setDecimalFormatSymbols(dfs);
        //Axis directly attached to plot area
        plot.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
        Font axisfont = new Font(/*"Gisha"*/Font.SANS_SERIF, Font.PLAIN, 12);
        Font axisfontLabel = new Font(/*"Gisha"*/Font.SANS_SERIF, Font.PLAIN, 14);

        int yaxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < yaxisCount; i++) {
            ValueAxis yaxis = plot.getRangeAxis(i);
            plot.getRangeAxis().setAxisLinePaint(Color.black);
            yaxis.setTickMarkOutsideLength(0);
            yaxis.setTickMarkInsideLength(5);
            yaxis.setAxisLineStroke(stroke1);
            yaxis.setTickMarkStroke(stroke1);
            yaxis.setLabelFont(axisfontLabel);
            yaxis.setTickLabelFont(axisfont);
            //((NumberAxis)yaxis).setNumberFormatOverride(df);
        }

        plot.getDomainAxis().setAxisLinePaint(Color.black);

        plot.getDomainAxis().setTickMarkOutsideLength(0);

        plot.getDomainAxis().setTickMarkInsideLength(5);

        plot.getDomainAxis().setAxisLineStroke(stroke1);

        plot.getDomainAxis().setTickMarkStroke(stroke1);

        plot.getDomainAxis().setLabelFont(axisfontLabel);
        plot.getDomainAxis().setTickLabelFont(axisfont);

        plot.setOutlineStroke(stroke1);

    }

    public static void main1(String[] args) {

        String fonts[]
                = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        for (int i = 0; i < fonts.length; i++) {
            System.out.println(fonts[i]);
        }
    }

}
