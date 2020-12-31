/*
 * The MIT License
 *
 * Copyright 2020 B1.
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
package com.saemann.gulli.view;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.border.TitledBorder;

/**
 * Panel to display parameters of the measurements for pipe and surface domain
 *
 * @author saemann
 */
public class MeasurementPanel extends JPanel {

    protected TitledBorder borderPipe, borderSurface;

    protected JPanel panelPipeSurrounding, panelSurfaceSurrounding, panelOutputsSurrounding, panelOutputs;
//    protected JPanel panelPipe, panelSurface, panelInjectionButtons;

    protected Controller control;

//    private JPanel panelMeasurement;
    private JFormattedTextField textMeasurementSecondsPipe;
    private JCheckBox checkMeasureContinouslyPipe;
    private JCheckBox checkMeasureResidenceTimePipe;
    private JFormattedTextField textMeasurementSecondsSurface;
    private JCheckBox checkMeasureContinouslySurface;
    private JCheckBox checkMeasureSynchronisedSurface;
    private JCheckBox checkMeasureSynchronisedPipe;

    protected DecimalFormat dfSeconds = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(StartParameters.formatLocale));
    protected JButton buttonNewOutput;

    public MeasurementPanel(Controller c) {
        super(new BorderLayout());
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.control = c;
//        panelPipe = new JPanel();

        DecimalFormatSymbols dfsymb = new DecimalFormatSymbols(StartParameters.formatLocale);
        dfsymb.setGroupingSeparator(' ');
        dfSeconds = new DecimalFormat("#,##0.###", dfsymb);
        dfSeconds.setGroupingSize(3);

//        panelPipe.setLayout(new BoxLayout(panelPipe, BoxLayout.Y_AXIS));
//        JScrollPane scrollMaterial = new JScrollPane(panelPipe);
        borderPipe = new TitledBorder("Pipe");
        panelPipeSurrounding = new JPanel(new BorderLayout());
        panelPipeSurrounding.setBorder(borderPipe);
        panelPipeSurrounding.setMaximumSize(new Dimension(500, 120));
//        panelPipeSurrounding.add(scrollMaterial, BorderLayout.CENTER);
        this.add(panelPipeSurrounding, BorderLayout.NORTH);

//        panelSurface = new JPanel();
        panelSurfaceSurrounding = new JPanel(new BorderLayout());
        panelSurfaceSurrounding.setMaximumSize(new Dimension(500, 120));
//        panelSurface.setLayout(new BoxLayout(panelSurface, BoxLayout.Y_AXIS));
//        JScrollPane scrollInjection = new JScrollPane(panelSurface);
        borderSurface = new TitledBorder("Surface");
        panelSurfaceSurrounding.setBorder(borderSurface);
//        scrollInjection.setPreferredSize(new Dimension(100, 900));
//        panelSurfaceSurrounding.add(scrollInjection, BorderLayout.CENTER);
        this.add(panelSurfaceSurrounding, BorderLayout.CENTER);
        this.add(new JPanel());
//        panelInjectionButtons = new JPanel(new GridLayout(1, 2));
//        panelSurfaceSurrounding.add(panelInjectionButtons, BorderLayout.NORTH);

        //Panel Measurement/Sampling options
        JPanel panelMeasurementsPipe = panelPipeSurrounding;
        panelMeasurementsPipe.setBorder(new TitledBorder("Pipe Network"));
        JPanel panelMsec = new JPanel(new BorderLayout());

        panelMsec.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsec.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsPipe = new JFormattedTextField(dfSeconds);
        textMeasurementSecondsPipe.setToolTipText("Length of measurement interval in seconds.");

        panelMsec.add(textMeasurementSecondsPipe, BorderLayout.CENTER);
        panelMeasurementsPipe.add(panelMsec, BorderLayout.NORTH);
        JPanel panelMcheck = new JPanel(new GridLayout(1, 3));
        checkMeasureContinouslyPipe = new JCheckBox("Time contin.", false);
        checkMeasureContinouslyPipe.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");

        checkMeasureResidenceTimePipe = new JCheckBox("Space contin.", false);
        checkMeasureResidenceTimePipe.setToolTipText("<html><b>true</b>: Sample all visited capacities. <br><b>false</b>: Sample Only in final capacity at end of simulation step</html>");

        checkMeasureSynchronisedPipe = new JCheckBox("Synchronize", ArrayTimeLineMeasurement.synchronizeMeasures);
        checkMeasureSynchronisedPipe.setToolTipText("<html><b>true</b>: slow, accurate measurement for every sampling<br><b>false</b>: fast sampling can override parallel results!</html>");

        panelMcheck.add(checkMeasureContinouslyPipe);
        panelMcheck.add(checkMeasureResidenceTimePipe);
        panelMcheck.add(checkMeasureSynchronisedPipe);

        panelMeasurementsPipe.add(panelMcheck, BorderLayout.SOUTH);

        if (control != null && control.getScenario() != null) {
            if (control.getScenario().getMeasurementsPipe() != null) {
                ArrayTimeLineMeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                if (mpc.isTimespotmeasurement()) {
                    checkMeasureContinouslyPipe.setSelected(false);
                } else {
                    checkMeasureContinouslyPipe.setSelected(true);
                }
                checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
                textMeasurementSecondsPipe.setValue(mpc.getDeltaTimeS());
            }
        }
        JPanel panelMeasurementsSurface = panelSurfaceSurrounding;
        panelMeasurementsSurface.setBorder(new TitledBorder("Surface"));
        JPanel panelMsecS = new JPanel(new BorderLayout());

        panelMsecS.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsecS.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsSurface = new JFormattedTextField(dfSeconds);
        textMeasurementSecondsSurface.setToolTipText("Length of measurement interval in seconds.");

        panelMsecS.add(textMeasurementSecondsSurface, BorderLayout.CENTER);
        panelMeasurementsSurface.add(panelMsecS, BorderLayout.NORTH);
        JPanel panelMcheckSurface = new JPanel(new GridLayout(1, 1));
        checkMeasureContinouslySurface = new JCheckBox("Time continous", false);
        checkMeasureContinouslySurface.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");
        checkMeasureSynchronisedSurface = new JCheckBox("Synchronize", SurfaceMeasurementRaster.synchronizeMeasures);
        checkMeasureSynchronisedSurface.setToolTipText("<html><b>true</b>: slow, accurate measurement for every sampling<br><b>false</b>: fast sampling can override parallel results!</html>");

//        checkMeasureResidenceTimeSurface = new JCheckBox("Residence", false);
//        checkMeasureResidenceTimeSurface.setToolTipText("<html><b>true</b>: Sample all visited capacities. <br><b>false</b>: Sample Only in final capacity at end of simulation step</html>");
        panelMcheckSurface.add(checkMeasureContinouslySurface);
        panelMcheckSurface.add(checkMeasureSynchronisedSurface);
//        panelMcheckSurface.add(checkMeasureResidenceTimeSurface);

        this.add(new JSeparator());
//Outputs
        buttonNewOutput = new JButton("new Output");
        panelOutputsSurrounding = new JPanel(new BorderLayout());
        panelOutputsSurrounding.setBorder(new TitledBorder("0 Outputs"));
        panelOutputsSurrounding.add(buttonNewOutput,BorderLayout.NORTH);
        panelOutputs=new JPanel();
        panelOutputs.setLayout(new BoxLayout(panelOutputs, BoxLayout.Y_AXIS));
        JScrollPane scroll=new JScrollPane(panelOutputs);
        panelOutputsSurrounding.add(scroll,BorderLayout.CENTER);
        this.add(panelOutputsSurrounding);

        panelMeasurementsSurface.add(panelMcheckSurface, BorderLayout.SOUTH);
        if (control.getScenario() != null) {
            if (control.getScenario().getMeasurementsSurface() != null) {
                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                if (mpc.continousMeasurements) {
                    checkMeasureContinouslySurface.setSelected(true);
                } else {
                    checkMeasureContinouslySurface.setSelected(false);
                }
                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);

            }
        }

        /////Measurements panel
        checkMeasureContinouslyPipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
                    if (checkMeasureContinouslyPipe.isSelected()) {
                        double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();

                        control.getScenario().getMeasurementsPipe().setSamplesPerTimeindex(seconds / ThreadController.getDeltaTime());
                    } else {
                        control.getScenario().getMeasurementsPipe().OnlyRecordOncePerTimeindex();
                    }
//                    System.out.println("Sample " + control.getScenario().getMeasurementsPipe().samplesPerTimeinterval + "x per interval");
                }
            }
        });

        checkMeasureResidenceTimePipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticlePipeComputing.measureOnlyFinalCapacity = !checkMeasureResidenceTimePipe.isSelected();

            }
        });

        checkMeasureSynchronisedPipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayTimeLineMeasurement.synchronizeMeasures = checkMeasureSynchronisedPipe.isSelected();

            }
        });

        textMeasurementSecondsPipe.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    textMeasurementSecondsPipe.setValue(control.getScenario().getMeasurementsPipe().getDeltaTimeS());
                } catch (Exception e) {
                }
//                System.out.println("Focus lost");
//                if (textMeasurementSecondsPipe == null || textMeasurementSecondsPipe.getValue() == null) {
//                    return;
//                }
//                fe.
//                double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();
//                System.out.println("New timestep for MesaurementPipe: " + seconds + " s. (Focus lost)");
//                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
//                    if (seconds == control.getScenario().getMeasurementsPipe().getDeltaTimeS()) {
//                        return; //DO not change, as the values correspond
//                    }
//                }
//                try {
//                    control.getScenario().getMeasurementsPipe().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
//                } catch (Exception e) {
//                }
            }
        });
        textMeasurementSecondsPipe.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    try {
                        double seconds = ((Number) textMeasurementSecondsPipe.getValue()).doubleValue();
                        System.out.println("New timestep for MesaurementPipe: " + seconds + " s. (Enter)");
                        if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsPipe() != null) {
                            if (seconds == control.getScenario().getMeasurementsPipe().getDeltaTimeS()) {
                                return; //DO not change, as the values correspond
                            }
                        }
                        try {
                            control.getScenario().getMeasurementsPipe().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                        } catch (Exception e) {
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
//                        textMeasurementSeconds.setValue(control.getScenario().getMeasurementsPipe().getDeltaTimeS());
                    }
                }
            }

        });

        /////Measurements panel
        checkMeasureContinouslySurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                    control.getScenario().getMeasurementsSurface().continousMeasurements = checkMeasureContinouslySurface.isSelected();
                }
            }
        });

        checkMeasureSynchronisedSurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SurfaceMeasurementRaster.synchronizeMeasures = checkMeasureSynchronisedSurface.isSelected();
            }
        });

        textMeasurementSecondsSurface.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                if (textMeasurementSecondsSurface == null || textMeasurementSecondsSurface.getValue() == null) {
                    return;
                }
                double seconds = Double.parseDouble(textMeasurementSecondsSurface.getText());//((Number) textMeasurementSecondsSurface.getValue()).doubleValue();
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                    if (seconds == control.getScenario().getMeasurementsSurface().getIndexContainer().getDeltaTimeMS() / 1000.) {
                        return;
                    }
                }
                try {
                    control.getScenario().getMeasurementsSurface().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                } catch (Exception e) {
                }
            }
        });
        textMeasurementSecondsSurface.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    if (textMeasurementSecondsSurface == null || textMeasurementSecondsSurface.getValue() == null) {
                        return;
                    }
                    double seconds = Double.parseDouble(textMeasurementSecondsSurface.getText());//((Number) textMeasurementSecondsSurface.getValue()).doubleValue();
                    if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                        if (seconds == control.getScenario().getMeasurementsSurface().getIndexContainer().getDeltaTimeMS() / 1000.) {
                            return;
                        }
                    }
                    try {
                        control.getScenario().getMeasurementsSurface().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                    } catch (Exception e) {
                    }
                }
            }

        });
    }

    public void updateParameters() {
        if (control != null && control.getScenario() != null) {
            if (control.getScenario().getMeasurementsPipe() != null) {
                ArrayTimeLineMeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                if (mpc.isTimespotmeasurement()) {
                    checkMeasureContinouslyPipe.setSelected(false);
                } else {
                    checkMeasureContinouslyPipe.setSelected(true);
                }
                checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
                textMeasurementSecondsPipe.setValue(mpc.getDeltaTimeS());
            }
            if (control.getScenario().getMeasurementsSurface() != null) {
                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                if (mpc.continousMeasurements) {
                    checkMeasureContinouslySurface.setSelected(true);
                } else {
                    checkMeasureContinouslySurface.setSelected(false);
                }
                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);
            }
        }
    }

    public void setEditable(boolean editable) {
        textMeasurementSecondsPipe.setEditable(editable);
        textMeasurementSecondsSurface.setEditable(editable);
        checkMeasureContinouslyPipe.setEnabled(editable);
        checkMeasureContinouslySurface.setEnabled(editable);
        checkMeasureResidenceTimePipe.setEnabled(editable);
        checkMeasureSynchronisedSurface.setEnabled(editable);
        checkMeasureSynchronisedPipe.setEnabled(editable);

    }
}
