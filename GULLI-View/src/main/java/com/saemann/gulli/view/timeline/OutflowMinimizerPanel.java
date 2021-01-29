/*
 * The MIT License
 *
 * Copyright 2020 Saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saemann.gulli.view.timeline;

import com.saemann.gulli.core.control.StartParameters;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.saemann.gulli.core.model.timeline.analysis.OutletMinimizer;
import com.saemann.gulli.core.model.timeline.array.TimeLinePipe;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Pipe;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;

/**
 * Options to be displayed next to the Capacity timeline Panel. Changes here
 * will call the update of the Outlet Minimizer analysis and will show colorfull
 * areas on the timeline plot.
 *
 * @author saemann
 */
public class OutflowMinimizerPanel extends JPanel {

    public OutletMinimizer minimizer;
    CapacityTimelinePanel panel;
    protected Pipe pipe;
    protected double maxVolume, targetVolume;
    public Color paint;

    JSlider slider;
    JTextField textMaxVolume;
    JPanel panelTargetValue;
    JRadioButton radioConcentration;
    JRadioButton radioMass;
    ButtonGroup groupTarget;

    JLabel labelPollution;
    DecimalFormat df=new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(StartParameters.formatLocale));

    public OutflowMinimizerPanel() {
        super(new BorderLayout());
        slider = new JSlider(JSlider.VERTICAL);
        textMaxVolume = new JTextField();
        textMaxVolume.setEditable(false);
        panelTargetValue = new JPanel(new GridLayout(2, 1));
        groupTarget = new ButtonGroup();
        radioConcentration = new JRadioButton("Concentration", true);
        radioMass = new JRadioButton("Massflux", false);
        groupTarget.add(radioMass);
        groupTarget.add(radioConcentration);
        panelTargetValue.add(radioConcentration);
        panelTargetValue.add(radioMass);
        paint = new Color(255, 200, 20, 140);

        this.add(panelTargetValue, BorderLayout.NORTH);

        JPanel panelCenter = new JPanel(new BorderLayout());
        panelCenter.add(slider, BorderLayout.CENTER);
        panelCenter.add(textMaxVolume, BorderLayout.SOUTH);
        this.add(panelCenter, BorderLayout.CENTER);

        labelPollution = new JLabel("Pollution mass in Region.");
        this.add(labelPollution, BorderLayout.SOUTH);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                textMaxVolume.setText(slider.getValue() + "   ("+df.format(slider.getValue()*100./slider.getMaximum())+"%)");
            }
        });

        ActionListener updateListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateOutput();
            }
        };

        radioConcentration.addActionListener(updateListener);
        radioMass.addActionListener(updateListener);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                targetVolume = slider.getValue();
                textMaxVolume.setText(targetVolume + "   ("+df.format(slider.getValue()*100./slider.getMaximum())+"%)");
                updateOutput();
            }
        });
    }

    public void setCapacity(Capacity c) {
        if (c == null) {
            setPipe(null);
            return;
        }
        if (c instanceof Pipe) {
            setPipe((Pipe) c);
        }
        setPipe(null);
    }

    public void setPipe(Pipe p) {
        if (p == null) {
            pipe = null;
            if (panel != null) {
                panel.panelChart.getChart().getXYPlot().clearDomainMarkers();
            }
            return;
        }
        this.pipe = p;
        maxVolume = totalDischarge(pipe.getStatusTimeLine());
        this.slider.setMaximum((int) maxVolume);

        if (minimizer == null) {
            minimizer = new OutletMinimizer(pipe, targetVolume);
        }
        minimizer.setPipe(pipe);
        minimizer.analyseIntervals();

        updateOutput();
    }

    public void updateOutput() {
        if (minimizer != null && panel != null) {
            if (radioConcentration.isSelected()) {
                minimizer.orderByConcentration();
            } else if (radioMass.isSelected()) {
                minimizer.orderByPollutionMass();
            } else {
                throw new UnsupportedOperationException("Do not know what to optimize");
            }
            minimizer.findMaximumIntervals(targetVolume);
            ArrayList<OutletMinimizer.PollutionDischargeInterval> regions = minimizer.getMaximumIntervals();

            ChartPanel chartPanel = panel.panelChart;
            XYPlot plot = chartPanel.getChart().getXYPlot();
            plot.clearDomainMarkers();
//            int index = 0;
//            System.out.println("Produced new minimizer results for target volume: " + targetVolume + " -> " + regions.size() + " regions.");
            for (OutletMinimizer.PollutionDischargeInterval region : regions) {
//                System.out.println(region.intervalIndex + " intervals from " + new Date(region.start) + "  to " + new Date(region.end));
                IntervalMarker marker = new IntervalMarker(region.start, region.end, paint);
                plot.addDomainMarker(marker);
            }

            labelPollution.setText(df.format(minimizer.getContainedMass()) + " / " + df.format(minimizer.getMaximumMass()) + " kg (" + (int)((minimizer.getContainedMass() * 100) / minimizer.getMaximumMass()) + "%)");
        }
    }

    public static double totalDischarge(TimeLinePipe timeline) {
        double sum = 0;
        for (int i = 1; i < timeline.getNumberOfTimes(); i++) {
            sum += (timeline.getDischarge(i - 1) + timeline.getDischarge(i)) * 0.5 * (timeline.getTimeContainer().getTimeMilliseconds(i) - timeline.getTimeContainer().getTimeMilliseconds(i - 1)) / 1000.;
        }
        return sum;
    }

}
