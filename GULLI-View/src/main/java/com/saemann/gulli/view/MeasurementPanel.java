/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
import com.saemann.gulli.core.control.StoringCoordinator;
import com.saemann.gulli.core.control.output.ContaminationMass;
import com.saemann.gulli.core.control.output.ContaminationShape;
import com.saemann.gulli.core.control.output.OutputIntention;
import com.saemann.gulli.core.control.output.Save_TravelAccumulationRegions;
import com.saemann.gulli.core.control.output.Save_Travelpath;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * Panel to display parameters of the measurements for pipe and surface domain
 *
 * @author saemann
 */
public class MeasurementPanel extends JPanel {

    protected TitledBorder borderPipe, borderSurface, borderOutputs;

    protected JPanel panelPipeSurrounding, panelSurfaceSurrounding, panelOutputsSurrounding, panelOutputs;

    protected Controller control;

    private JFormattedTextField textMeasurementSecondsPipe;
    private JCheckBox checkMeasureContinouslyPipe;
    private JCheckBox checkMeasureResidenceTimePipe;
    private JFormattedTextField textMeasurementSecondsSurface;
    private JCheckBox checkMeasureContinouslySurface;
    private JCheckBox checkMeasureSpatialConsistentSurface;
    private JCheckBox checkMeasureSynchronisedSurface;
    private JCheckBox checkMeasureSynchronisedPipe;

    private JCheckBox checkHistoryParticles;
    private JFormattedTextField textHistoricIth, textHistoricPercent;

    private enum GridType {
        NONE, TRIANGLE, RECTANGLE, OTHER
    };
    private JComboBox<GridType> comboSurfaceGrid;
    private boolean selfChange = false;
    private JTextField textGridSize = new JTextField();

    protected DecimalFormat dfSeconds = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(StartParameters.formatLocale));
    protected JButton buttonNewContaminationOutput, buttonNewContaminationShapeOutput, buttonNewTraceOutput, buttonNewTraceArea;

    public MeasurementPanel(Controller c) {
        super(new BorderLayout());
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.control = c;

        DecimalFormatSymbols dfsymb = new DecimalFormatSymbols(StartParameters.formatLocale);
        dfsymb.setGroupingSeparator(' ');
        dfSeconds = new DecimalFormat("#,##0.###", dfsymb);
        dfSeconds.setGroupingSize(3);

        borderPipe = new TitledBorder("Pipe");
        borderPipe.setBorder(new LineBorder(Color.blue.darker(), 2, true));
        panelPipeSurrounding = new JPanel(new BorderLayout());
        panelPipeSurrounding.setBorder(borderPipe);
        panelPipeSurrounding.setMaximumSize(new Dimension(500, 120));
        this.add(panelPipeSurrounding, BorderLayout.NORTH);

//        panelSurface = new JPanel();
        panelSurfaceSurrounding = new JPanel();
        panelSurfaceSurrounding.setLayout(new BoxLayout(panelSurfaceSurrounding, BoxLayout.Y_AXIS));
        panelSurfaceSurrounding.setMaximumSize(new Dimension(500, 120));
        borderSurface = new TitledBorder("Surface");
        borderSurface.setBorder(new LineBorder(Color.GREEN.darker(), 2, true));
        panelSurfaceSurrounding.setBorder(borderSurface);
        this.add(panelSurfaceSurrounding);

        //Panel Measurement/Sampling options
        JPanel panelMeasurementsPipe = panelPipeSurrounding;
        panelMeasurementsPipe.setBorder(borderPipe);
        JPanel panelMsec = new JPanel(new BorderLayout());

        panelMsec.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsec.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsPipe = new JFormattedTextField(dfSeconds);
        textMeasurementSecondsPipe.setHorizontalAlignment(SwingConstants.RIGHT);
        textMeasurementSecondsPipe.setToolTipText("Length of measurement interval in seconds.");

        panelMsec.add(textMeasurementSecondsPipe, BorderLayout.CENTER);
        panelMeasurementsPipe.add(panelMsec, BorderLayout.NORTH);
        JPanel panelMcheck = new JPanel(new GridLayout(1, 3));
        checkMeasureContinouslyPipe = new JCheckBox("Time continuous", false);
        checkMeasureContinouslyPipe.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");

        checkMeasureResidenceTimePipe = new JCheckBox("Spatial consistency", false);
        checkMeasureResidenceTimePipe.setToolTipText("<html><b>true</b>: Sample all visited capacities, weighted by the residence time. <br><b>false</b>: Sample Only in final capacity at end of simulation step</html>");

        checkMeasureSynchronisedPipe = new JCheckBox("Synchronize", MeasurementContainer.synchronizeMeasures);
        checkMeasureSynchronisedPipe.setToolTipText("<html><b>true</b>: slow, accurate measurement for every sampling<br><b>false</b>: fast sampling can override parallel results!</html>");
        panelMcheck.add(checkMeasureContinouslyPipe);
        panelMcheck.add(checkMeasureResidenceTimePipe);
        panelMcheck.add(checkMeasureSynchronisedPipe);

        panelMeasurementsPipe.add(panelMcheck, BorderLayout.SOUTH);

        if (control != null && control.getScenario() != null) {
            if (control.getScenario().getMeasurementsPipe() != null) {

                MeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                checkMeasureContinouslyPipe.setSelected(mpc.timecontinuousMeasures);
                checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
                textMeasurementSecondsPipe.setValue(mpc.getTimes().getDeltaTimeMS() / 1000);
            }
        }
        panelSurfaceSurrounding.setBorder(borderSurface);
        JPanel panelMsecS = new JPanel(new BorderLayout());

        panelMsecS.add(new JLabel("Measure interval: "), BorderLayout.WEST);
        panelMsecS.add(new JLabel("sec."), BorderLayout.EAST);
        textMeasurementSecondsSurface = new JFormattedTextField(dfSeconds);
        textMeasurementSecondsSurface.setHorizontalAlignment(SwingConstants.RIGHT);
        textMeasurementSecondsSurface.setToolTipText("Length of measurement interval in seconds.");

        panelMsecS.add(textMeasurementSecondsSurface, BorderLayout.CENTER);
        panelSurfaceSurrounding.add(panelMsecS, BorderLayout.NORTH);
        JPanel panelMcheckSurface = new JPanel(new GridLayout(1, 1));
        checkMeasureContinouslySurface = new JCheckBox("Time continuous", false);
        checkMeasureContinouslySurface.setToolTipText("<html><b>true</b>: slow, accurate measurement in every simulation timestep, mean calculated for the interval. <br><b>false</b>: fast sampling only at the end of an interval.</html>");
        checkMeasureSpatialConsistentSurface = new JCheckBox("Spatial consistency", false);
        checkMeasureSpatialConsistentSurface.setToolTipText("<html><b>true</b>: Sample all visited cells, weighted by the residence time. <br><b>false</b>: Sample Only in final cells at end of simulation step</html>");

        checkMeasureSynchronisedSurface = new JCheckBox("Synchronize", SurfaceMeasurementRaster.synchronizeMeasures);
        checkMeasureSynchronisedSurface.setToolTipText("<html><b>true</b>: slow, accurate measurement for every sampling<br><b>false</b>: fast sampling can override parallel results!</html>");
        panelMcheckSurface.add(checkMeasureContinouslySurface);
        panelMcheckSurface.add(checkMeasureSpatialConsistentSurface);
        panelMcheckSurface.add(checkMeasureSynchronisedSurface);

        //Grid
        comboSurfaceGrid = new JComboBox<>(GridType.values());
        comboSurfaceGrid.setToolTipText("Type of Raster for surface measurements");
        textGridSize.setEnabled(false);

        JPanel panelHistoryParticles = new JPanel();
        panelHistoryParticles.setMaximumSize(new Dimension(500, 50));
        panelHistoryParticles.setToolTipText("Select surface view 'PARTICLETRACE' after simulation to show trace on the map. Actual: " + control.getNumberTracerParticles());
        panelHistoryParticles.setBorder(new TitledBorder(new LineBorder(Color.orange, 2), "Particle trace"));
        panelHistoryParticles.setLayout(new BoxLayout(panelHistoryParticles, BoxLayout.X_AXIS));
        checkHistoryParticles = new JCheckBox("Trace", control.isTraceParticles());
        textHistoricIth = new JFormattedTextField(DecimalFormat.getIntegerInstance(StartParameters.formatLocale));
        textHistoricIth.setValue(control.intervallHistoryParticles);
        textHistoricIth.setMinimumSize(new Dimension(50, 20));
        textHistoricIth.setHorizontalAlignment(SwingConstants.RIGHT);
        panelHistoryParticles.add(checkHistoryParticles);
        panelHistoryParticles.add(textHistoricIth);
        panelHistoryParticles.add(new JLabel("th particle"));

        this.add(panelHistoryParticles);

        this.add(new JSeparator());
        //Outputs
        JPanel panelButtonsNewOutputs = new JPanel(new GridLayout(2, 2, 4, 4));
        buttonNewContaminationOutput = new JButton("+ Contamination");
        panelButtonsNewOutputs.add(buttonNewContaminationOutput);
        buttonNewContaminationShapeOutput = new JButton("+ Shape");
        panelButtonsNewOutputs.add(buttonNewContaminationShapeOutput);

        buttonNewTraceOutput = new JButton("+ TraceLines");
        panelButtonsNewOutputs.add(buttonNewTraceOutput);

        buttonNewTraceArea = new JButton("+ TraceAreas");
        panelButtonsNewOutputs.add(buttonNewTraceArea);

        panelOutputsSurrounding = new JPanel(new BorderLayout());
        borderOutputs = new TitledBorder("0 Outputs");
        panelOutputsSurrounding.setBorder(borderOutputs);
        panelOutputsSurrounding.add(panelButtonsNewOutputs, BorderLayout.NORTH);

        panelOutputs = new JPanel();
        panelOutputs.setLayout(new BoxLayout(panelOutputs, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(panelOutputs);
        panelOutputsSurrounding.add(scroll, BorderLayout.CENTER);
        this.add(panelOutputsSurrounding);

        panelSurfaceSurrounding.add(panelMcheckSurface, BorderLayout.SOUTH);
        panelSurfaceSurrounding.add(comboSurfaceGrid);
        panelSurfaceSurrounding.add(textGridSize);

        if (control.getScenario() != null) {
            if (control.getScenario().getMeasurementsSurface() != null) {
                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                if (mpc.continousMeasurements) {
                    checkMeasureContinouslySurface.setSelected(true);
                } else {
                    checkMeasureContinouslySurface.setSelected(false);
                }
                if (mpc.spatialConsistency) {
                    checkMeasureSpatialConsistentSurface.setSelected(true);
                } else {
                    checkMeasureSpatialConsistentSurface.setSelected(false);
                }
                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);

            }
        }

        /////Measurements panel
        checkMeasureContinouslyPipe.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MeasurementContainer.timecontinuousMeasures=checkMeasureContinouslyPipe.isSelected();
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
                MeasurementContainer.synchronizeMeasures = checkMeasureSynchronisedPipe.isSelected();

            }
        });

        textMeasurementSecondsPipe.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    textMeasurementSecondsPipe.setValue(control.getScenario().getMeasurementsPipe().getTimes().getDeltaTimeMS() / 1000);
                } catch (Exception e) {
                }
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
                            if (seconds == control.getScenario().getMeasurementsPipe().getTimes().getDeltaTimeMS() / 1000) {
                                return; //DO not change, as the values correspond
                            }
                        }
                        try {
                            control.getScenario().getMeasurementsPipe().setIntervalSeconds(seconds, control.getScenario().getStartTime(), control.getScenario().getEndTime());
                        } catch (Exception e) {
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
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

        checkMeasureSpatialConsistentSurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (control != null && control.getScenario() != null && control.getScenario().getMeasurementsSurface() != null) {
                    control.getScenario().getMeasurementsSurface().spatialConsistency = checkMeasureSpatialConsistentSurface.isSelected();
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
                double seconds = ((Number) textMeasurementSecondsSurface.getValue()).doubleValue();//((Number) textMeasurementSecondsSurface.getValue()).doubleValue();
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

        comboSurfaceGrid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selfChange) {
                    return;
                }
                if (control.getSurface() == null) {
                    selfChange = true;
                    comboSurfaceGrid.setSelectedItem(GridType.NONE);
                    selfChange = false;
                    return;
                }
                if (comboSurfaceGrid.getSelectedItem() == GridType.NONE) {
                } else if (comboSurfaceGrid.getSelectedItem() == GridType.TRIANGLE) {
                    SurfaceMeasurementTriangleRaster smr = SurfaceMeasurementTriangleRaster.init(control);
                    control.getSurface().setMeasurementRaster(smr);
                    if (control.getScenario() != null) {
                        control.getScenario().setMeasurementsSurface(smr);
                    }
                } else if (comboSurfaceGrid.getSelectedItem() == GridType.RECTANGLE) {
                    double dx = 50;//Double.parseDouble(textGridSize.getText().replaceAll("[^0-9]", "").replaceAll(",", "."));
                    SurfaceMeasurementRectangleRaster smr = SurfaceMeasurementRectangleRaster.SurfaceMeasurementRectangleRaster(control.getSurface(), dx, dx);
                    control.getSurface().setMeasurementRaster(smr);
                    if (control.getScenario() != null) {
                        control.getScenario().setMeasurementsSurface(smr);
                    }
                }
                updateParameters();
            }
        });

        textGridSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboSurfaceGrid.getSelectedItem() == GridType.RECTANGLE) {
                    double dx = Double.parseDouble(textGridSize.getText().replaceAll("[^0-9]", "").replaceAll(",", "."));
                    SurfaceMeasurementRectangleRaster smr = SurfaceMeasurementRectangleRaster.SurfaceMeasurementRectangleRaster(control.getSurface(), dx, dx);
                    control.getSurface().setMeasurementRaster(smr);
                    if (control.getScenario() != null) {
                        control.getScenario().setMeasurementsSurface(smr);
                    }
                }
                updateParameters();
                repaint();
            }
        });

        checkHistoryParticles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    control.intervallHistoryParticles = ((Number) textHistoricIth.getValue()).intValue();

                } catch (Exception ex) {
                    textHistoricIth.setValue(control.intervallHistoryParticles);
                    ex.printStackTrace();
                }
                control.setTraceParticles(checkHistoryParticles.isSelected());
            }
        });

        textHistoricIth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.intervallHistoryParticles = ((Number) textHistoricIth.getValue()).intValue();

            }
        });

        buttonNewContaminationOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.getStoringCoordinator().addFinalOuput(new ContaminationMass(-1));
                updateParameters();
            }
        });

        buttonNewContaminationShapeOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.getStoringCoordinator().addFinalOuput(new ContaminationShape(StoringCoordinator.FileFormat.GeoPKG, -1, true));
                updateParameters();
            }
        });

        buttonNewTraceOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.getStoringCoordinator().addFinalOuput(new Save_Travelpath(StoringCoordinator.FileFormat.SHP, -1));
                updateParameters();
            }
        });

        buttonNewTraceArea.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.getStoringCoordinator().addFinalOuput(new Save_TravelAccumulationRegions(StoringCoordinator.FileFormat.SHP, -1));
                updateParameters();
            }
        });
    }

    public void updateParameters() {
        selfChange = true;
        if (control != null && control.getScenario() != null) {
            if (control.getScenario().getMeasurementsPipe() != null) {
                MeasurementContainer mpc = control.getScenario().getMeasurementsPipe();
                    checkMeasureContinouslyPipe.setSelected(mpc.timecontinuousMeasures);

                textMeasurementSecondsPipe.setValue(mpc.getTimes().getDeltaTimeMS() / 1000);
            }
            checkMeasureResidenceTimePipe.setSelected(!ParticlePipeComputing.measureOnlyFinalCapacity);
            checkMeasureSynchronisedPipe.setSelected(MeasurementContainer.synchronizeMeasures);

            if (control.getScenario().getMeasurementsSurface() != null) {
                SurfaceMeasurementRaster mpc = control.getScenario().getMeasurementsSurface();
                if (mpc.continousMeasurements) {
                    checkMeasureContinouslySurface.setSelected(true);
                } else {
                    checkMeasureContinouslySurface.setSelected(false);
                }
                if (mpc.spatialConsistency) {
                    checkMeasureSpatialConsistentSurface.setSelected(true);
                } else {
                    checkMeasureSpatialConsistentSurface.setSelected(false);
                }
                checkMeasureSynchronisedSurface.setSelected(mpc.synchronizeMeasures);
                textMeasurementSecondsSurface.setValue(mpc.getIndexContainer().getDeltaTimeMS() / 1000.);
            }

        }
        panelOutputs.removeAll();
        if (control != null && control.getStoringCoordinator() != null) {
            StoringCoordinator sc = control.getStoringCoordinator();
            int counter = 0;
            for (OutputIntention fout : sc.getFinalOutputs()) {
                OutputPanel op = new OutputPanel(fout, counter++, control.getStoringCoordinator());

                panelOutputs.add(op);
                JPopupMenu popup = new JPopupMenu();
                JMenuItem itemdelete = new JMenuItem("Remove");
                popup.add(itemdelete);
                itemdelete.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        control.getStoringCoordinator().getFinalOutputs().remove(fout);
                        popup.setVisible(false);
                        updateParameters();
                    }
                });
                op.setComponentPopupMenu(popup);
            }
            borderOutputs.setTitle(sc.getFinalOutputs().size() + " Outputs");
        }

        if (control != null && control.getSurface() != null) {

            if (control.getSurface().getMeasurementRaster() == null) {
                comboSurfaceGrid.setSelectedItem(GridType.NONE);
            } else {
                if (control.getSurface().getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                    comboSurfaceGrid.setSelectedItem(GridType.TRIANGLE);
                    textGridSize.setText(control.getSurface().getMeasurementRaster().getNumberOfCells() + " triangles");
                    textGridSize.setToolTipText("Number of cells");
                } else if (control.getSurface().getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
                    comboSurfaceGrid.setSelectedItem(GridType.RECTANGLE);
                    textGridSize.setText(((SurfaceMeasurementRectangleRaster) control.getSurface().getMeasurementRaster()).getxIntervalWidth() + " m");
                    textGridSize.setToolTipText("Grid interval [m]");
                } else {
                    textGridSize.setText(control.getSurface().getMeasurementRaster().getClass().getSimpleName());
                    textGridSize.setToolTipText("Unknown type of Raster");
                }
                checkMeasureContinouslySurface.setSelected(control.getSurface().getMeasurementRaster().continousMeasurements);
                checkMeasureSpatialConsistentSurface.setSelected(control.getSurface().getMeasurementRaster().spatialConsistency);
            }

        }
        if (control != null) {
            checkHistoryParticles.setSelected(control.isTraceParticles());
            textHistoricIth.setText(control.intervallHistoryParticles + "");
            checkHistoryParticles.setSelected(control.isTraceParticles());
        }
        panelOutputs.revalidate();
        panelOutputs.repaint();
        selfChange = false;

    }

    public void setEditable(boolean editable) {
        selfChange = true;
        textMeasurementSecondsPipe.setEditable(editable);
        textMeasurementSecondsSurface.setEditable(editable);
        checkMeasureContinouslyPipe.setEnabled(editable);
        checkMeasureContinouslySurface.setEnabled(editable);
        checkMeasureResidenceTimePipe.setEnabled(editable);
        checkMeasureSpatialConsistentSurface.setEnabled(editable);
        checkMeasureContinouslyPipe.setEnabled(editable);
        checkMeasureSynchronisedSurface.setEnabled(editable);
        checkMeasureSynchronisedPipe.setEnabled(editable);

        checkHistoryParticles.setEnabled(editable);
        textHistoricIth.setEnabled(editable);

        textGridSize.setEnabled(editable);

        comboSurfaceGrid.setEnabled(editable);
        selfChange = false;
    }
}
