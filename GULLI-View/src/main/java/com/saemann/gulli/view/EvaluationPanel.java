/*
 * The MIT License
 *
 * Copyright 2022 robert.
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
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Manhole;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author robert
 */
public class EvaluationPanel extends JPanel {

    Controller control;

    DefaultTableModel model;
    JTable table;
    JScrollPane scrollTable;

    DefaultTableModel modelWashoff;
    JTable tableWashoff;
    JScrollPane scrollTableWashoff;

    JButton buttonUpdate;

    /**
     * Shows information about the required calculation time.
     */
    private JTextField textcomputtaionTime;

    private final DecimalFormat df1 = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(StartParameters.formatLocale));
    private final DecimalFormat df2 = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(StartParameters.formatLocale));

    public EvaluationPanel(Controller control) {
        super(new BorderLayout());
        this.control = control;
        initLayout();
        updateEvaluation();
    }

    private void initLayout() {
        model = new DefaultTableModel(8, 3);
        table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        scrollTable = new JScrollPane(table);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        modelWashoff = new DefaultTableModel(6, 3);
        tableWashoff = new JTable(modelWashoff) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        scrollTableWashoff = new JScrollPane(tableWashoff);
        tableWashoff.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        scrollTableWashoff.setBorder(new TitledBorder("Washoff behaviour"));
        scrollTableWashoff.setPreferredSize(new Dimension(300, 140));

        textcomputtaionTime = new JTextField();
        textcomputtaionTime.setEditable(false);
        buttonUpdate = new JButton("Refresh");

        JPanel panelNoth = new JPanel(new GridLayout(2, 2));
        panelNoth.add(new JLabel("Calculation Time:"));
        panelNoth.add(textcomputtaionTime);
        panelNoth.add(buttonUpdate);
        this.add(panelNoth, BorderLayout.NORTH);
        this.add(scrollTable, BorderLayout.CENTER);
        this.add(scrollTableWashoff, BorderLayout.SOUTH);

        buttonUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    updateEvaluation();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        });
    }

    /**
     * Fill GUI components with information about the current simulation
     */
    public void updateEvaluation() {
        long time = control.getThreadController().getElapsedCalculationTime();

        if (time < 10000) {
            textcomputtaionTime.setText(time + " ms");
        } else if (time < 60000) {
            textcomputtaionTime.setText(df1.format(time / 1000.) + " s");
        } else {
            int hours = (int) (time / (3600000));
            int minutes = (int) (time / 60000) % 60;
            int seconds = (int) (time / 1000) % 60;
            int milliseconds = (int) (time % 1000);
            StringBuffer str = new StringBuffer();
            if (hours > 0) {
                str.append(hours + " h  ");
            }
            if (minutes > 0 || hours > 0) {
                str.append(minutes + "m  ");
            }
            str.append(seconds + " s");
            textcomputtaionTime.setText(str.toString());
        }

        if (control.getScenario() != null && control.getScenario().getMaterials() != null) {

            int numberMaterials = control.getScenario().getMaterials().size();

            double[][] summarizers;
            double[][] counter;

            model.setRowCount(8);
            model.setValueAt("-  Unreleased", 0, 0);
            model.setValueAt("- + Released", 1, 0);
            model.setValueAt("  - + Completed", 2, 0);
            model.setValueAt("    -  Outlet", 3, 0);
            model.setValueAt("    -  Surface", 4, 0);
            model.setValueAt("  - + Active", 5, 0);
            model.setValueAt("    -  in Pipe", 6, 0);
            model.setValueAt("    -  on Surface", 7, 0);

            double[][] washoff;
            int[][] washoffCounter;

            modelWashoff.setRowCount(6);
            modelWashoff.setValueAt("- + Start on Surface", 0, 0);
            modelWashoff.setValueAt("  - Stay on Surface", 1, 0);
            modelWashoff.setValueAt("  - Washed to Pipe", 2, 0);
            modelWashoff.setValueAt("- + Start in Pipe", 3, 0);
            modelWashoff.setValueAt("  - Stay in Pipe", 4, 0);
            modelWashoff.setValueAt("  - Spilled to Surface", 5, 0);

            if (numberMaterials > 0) {

                summarizers = new double[numberMaterials][8];
                counter = new double[numberMaterials][8];

                washoff = new double[numberMaterials][6];
                washoffCounter = new int[numberMaterials][6];

                for (Particle p : control.getThreadController().getParticles()) {
                    int materialIndex = control.getScenario().getMaterials().indexOf(p.getMaterial());
                    if (materialIndex < 0) {
                        System.err.println("Material " + p.getMaterial() + " was not found in list of materials skip particle " + p.getId());
                        continue;
                    }
                    if (p.isWaiting()) {
                        summarizers[materialIndex][0] += p.getParticleMass();
                        counter[materialIndex][0]++;
                    } else {
                        //Particle was released
                        summarizers[materialIndex][1] += p.getParticleMass();
                        counter[materialIndex][1]++;
                        if (p.hasLeftSimulation()) {
                            //Particle completed journey and left simulaion
                            summarizers[materialIndex][2] += p.getParticleMass();
                            counter[materialIndex][2]++;
                            if (p.isInPipeNetwork()) {
                                //Left through outlet
                                summarizers[materialIndex][3] += p.getParticleMass();
                                counter[materialIndex][3]++;
                            } else if (p.isOnSurface()) {
                                //Left through surface boundary
                                summarizers[materialIndex][4] += p.getParticleMass();
                                counter[materialIndex][4]++;
                            } else if (p.getSurrounding_actual() != null && p.getSurrounding_actual().getClass() == Manhole.class) {
                                //Left through outlet
                                summarizers[materialIndex][3] += p.getParticleMass();
                                counter[materialIndex][3]++;
                            } else if (p.getSurrounding_actual() != null && p.getSurrounding_actual().getClass() == Surface.class) {
                                //Left through outlet
                                summarizers[materialIndex][4] += p.getParticleMass();
                                counter[materialIndex][4]++;
                            } else {
                                System.err.println("unknown left simulation case " + p + " in Pipe?" + p.isInPipeNetwork() + "  on Surface?" + p.isOnSurface() + "  active?" + p.isActive() + " surrounding=" + p.getSurrounding_actual());
                            }
                        } else {
                            //Particle is still in simulation
                            summarizers[materialIndex][5] += p.getParticleMass();
                            counter[materialIndex][5]++;
                            if (p.isInPipeNetwork()) {
                                //Left through outlet
                                summarizers[materialIndex][6] += p.getParticleMass();
                                counter[materialIndex][6]++;
                            } else if (p.isOnSurface()) {
                                //Left through surface boundary
                                summarizers[materialIndex][7] += p.getParticleMass();
                                counter[materialIndex][7]++;
                            } else {
                                System.err.println("unknown inside simulation case " + p);
                            }
                        }

                        // Count for washoff table (only released particles)
                        if (p.getInjectionInformation().spillOnSurface()) {
                            //Start on surface
                            washoffCounter[materialIndex][0]++;
                            washoff[materialIndex][0] += p.getParticleMass();
                            if (p.toPipenetwork == null) {
                                //Nover washed to pipe system -> stayed on the surface
                                washoffCounter[materialIndex][1]++;
                                washoff[materialIndex][1] += p.getParticleMass();
                            } else {
                                //Washed to pipe system at least one time
                                washoffCounter[materialIndex][2]++;
                                washoff[materialIndex][2] += p.getParticleMass();
                            }
                        } else if (p.getInjectionInformation().spillinPipesystem()) {
                            //Released in Pipe system
                            washoffCounter[materialIndex][3]++;
                            washoff[materialIndex][3] += p.getParticleMass();
                            if (p.toSurface == null) {
                                //Nover spilled to surface -> stayed in pipe system
                                washoffCounter[materialIndex][4]++;
                                washoff[materialIndex][4] += p.getParticleMass();
                            } else {
                                //Spilled to surface at least one time
                                washoffCounter[materialIndex][5]++;
                                washoff[materialIndex][5] += p.getParticleMass();
                            }
                        }
                    }
                }
                if (numberMaterials > 1) {
                    model.setColumnCount(3 + numberMaterials * 2);
                    model.setRowCount(8);
                    String[] header = new String[3 + numberMaterials * 2];
                    header[0] = "Case";
                    header[1] = "mass total";
                    header[2] = "n total";
                    for (int i = 0; i < numberMaterials; i++) {
                        Material m = control.getScenario().getMaterials().get(i);
                        header[3 + i * 2] = "mass " + m.getName();
                        header[4 + i * 2] = "n " + m.getName();
                    }
                    model.setColumnIdentifiers(header);
                    for (int i = 0; i < counter[0].length; i++) {
                        int c = 0;
                        double m = 0;
                        for (int material = 0; material < counter.length; material++) {
                            c += counter[material][i];
                            m += summarizers[material][i];
                            model.setValueAt(df2.format(summarizers[material][i]), i, 3 + material * 2);
                            model.setValueAt((int) (counter[material][i]), i, 4 + material * 2);
                        }
                        model.setValueAt(df2.format(m), i, 1);
                        model.setValueAt((int) c + "", i, 2);
                    }

                    //WAshoff entries
                    modelWashoff.setColumnCount(3 + numberMaterials);
                    modelWashoff.setRowCount(6);
//                    header = new String[3 + numberMaterials];
//                    header[0] = "Case";
//                    header[1] = "mass total";
//                    header[2] = "n total";
//                    for (int i = 0; i < numberMaterials; i++) {
//                        Material m = control.getScenario().getMaterials().get(i);
//                        header[1 + i * 2] = "mass " + m.getName();
//                        header[2 + i * 2] = "n " + m.getName();
//                    }
                    modelWashoff.setColumnIdentifiers(header);
                    for (int i = 0; i < washoffCounter[0].length; i++) {
                        int c = 0;
                        double m = 0;
                        for (int j = 0; j < washoffCounter.length; j++) {
                            c += washoffCounter[j][i];
                            m += washoff[j][i];
                            modelWashoff.setValueAt(df2.format(washoff[j][i]), i, 3 + j * 2);
                            modelWashoff.setValueAt((int) (washoffCounter[j][i]), i, 4 + j * 2);
                        }
                        modelWashoff.setValueAt(df2.format(m), i, 1);
                        modelWashoff.setValueAt((int) c + "", i, 2);
                    }

                } else {
                    model.setColumnCount(3);
                    model.setRowCount(8);
                    String[] header = new String[3];
                    header[0] = "Case";
                    header[1] = "mass total";
                    header[2] = "n total";
                    model.setColumnIdentifiers(header);
                    for (int i = 0; i < counter[0].length; i++) {
                        model.setValueAt(df2.format(summarizers[0][i]), i, 1);
                        model.setValueAt((int) counter[0][i], i, 2);
                    }

                    modelWashoff.setColumnCount(3);
                    modelWashoff.setRowCount(6);
                    modelWashoff.setColumnIdentifiers(header);
                    for (int i = 0; i < washoffCounter[0].length; i++) {
                        modelWashoff.setValueAt(df2.format(washoff[0][i]), i, 1);
                        modelWashoff.setValueAt((int) washoffCounter[0][i], i, 2);
                    }
                }

            }

        } else {
            //Scenario is NULL
            model.setColumnCount(3);
            modelWashoff.setColumnCount(3);
            String[] header = new String[3];
            header[0] = "Case";
            header[1] = "mass total";
            header[2] = "n total";
            model.setColumnIdentifiers(header);
            for (int i = 1; i < model.getRowCount(); i++) {
                model.setValueAt("-", i, 1);
                model.setValueAt("-", i, 2);
            }
            modelWashoff.setColumnIdentifiers(header);
            for (int i = 1; i < modelWashoff.getRowCount(); i++) {
                modelWashoff.setValueAt("-", i, 1);
                modelWashoff.setValueAt("-", i, 2);
            }
        }
    }
}
