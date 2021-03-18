/*
 * The MIT License
 *
 * Copyright 2021 Robert Sämann.
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

import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.StoringCoordinator;
import com.saemann.gulli.core.control.output.OutputIntention;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author saemann
 */
public class OutputPanel extends JPanel {

    protected OutputIntention output;

    protected TitledBorder border;
    protected JButton buttonSave;

//    protected JTextField textname;
    protected JLabel labelIndex;
    protected JButton buttonCoordinate;
//    public static final DecimalFormat df = new DecimalFormat("0.###", new DecimalFormatSymbols(StartParameters.formatLocale));

    protected JComboBox<StoringCoordinator.FileFormat> comboFileFormat;
    protected JLabel labelFileName;
    protected JPanel panelIntParameters;
    protected JPanel panelDoubleParameters;

    protected boolean selfSelection = false;

    protected OutputPanel(OutputIntention mat, int number, StoringCoordinator sc) {
        super();
        this.setMinimumSize(new Dimension(100, 150));
        this.setPreferredSize(this.getMinimumSize());
//        this.setSize(this.getPreferredSize());
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.output = mat;
        border = new TitledBorder("");
        if (number % 2 == 0) {
            border.setBorder(new LineBorder(Color.BLACK, 2));
        } else {
            border.setBorder(new LineBorder(Color.orange.darker(), 2));
        }
        this.setBorder(border);//new LineBorder(Color.darkGray, 1, true));
//        df.getDecimalFormatSymbols().setGroupingSeparator(' ');
        //Name
//        textname = new JTextField(output.toString());
        labelIndex = new JLabel(output.toString());
        JPanel panelName = new JPanel(new BorderLayout());
        panelName.add(labelIndex, BorderLayout.WEST);
        buttonSave = new JButton("Save");
        buttonSave.setToolTipText("Save now");
        panelName.add(buttonSave, BorderLayout.EAST);
//        panelName.add(textname, BorderLayout.CENTER);
        this.add(panelName);
        this.add(new JSeparator(JSeparator.HORIZONTAL));

        JPanel panelRoutingChooser = new JPanel(new BorderLayout());
        comboFileFormat = new JComboBox<>(StoringCoordinator.FileFormat.values());
        panelRoutingChooser.add(new JLabel("Format: "), BorderLayout.WEST);
        panelRoutingChooser.add(comboFileFormat, BorderLayout.CENTER);
        this.add(panelRoutingChooser);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        panelIntParameters = new JPanel();
        panelDoubleParameters = new JPanel();
        this.add(panelIntParameters);
        this.add(new JSeparator(JSeparator.HORIZONTAL));
        this.add(panelDoubleParameters);

        updateValues();

        comboFileFormat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selfSelection) {
                    return;
                }
                output.setFileFormat((StoringCoordinator.FileFormat) comboFileFormat.getSelectedItem());
                updateValues();
            }
        });

        buttonSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.writeOutput(sc);
            }
        });

        this.setPreferredSize(new Dimension(300, 100));
        this.revalidate();
    }

    public void updateValues() {
        selfSelection = true;
//        textname.setText(output.toString());
        labelIndex.setText(output.getFilePath());
        border.setTitle(output.toString());
        comboFileFormat.setSelectedItem(output.getFileFormat());
//        labelFileName=new JLabel("");
//        labelFileName.setText(output.getFilePath());

        panelIntParameters.removeAll();
        panelDoubleParameters.removeAll();
        final double[] parameters = output.getParameterValuesDouble();
        final int[] parametersInt = output.getParameterValuesInt();
        if (parametersInt != null) {
            panelIntParameters.setLayout(new GridLayout(parametersInt.length, 2));
            String[] names = output.getParameterNamesInt();
            for (int i = 0; i < parametersInt.length; i++) {
                JFormattedTextField editParameter = new JFormattedTextField(DecimalFormat.getIntegerInstance(StartParameters.formatLocale));
                editParameter.setValue(parametersInt[i]);
                JLabel labelName = null;
                if (names != null && names.length > i) {
                    labelName = new JLabel(names[i]);
                    labelName.setToolTipText(names[i]);
                    editParameter.setToolTipText(names[i]);
                } else {
                    labelName = new JLabel();
                }
                labelName = new JLabel(names[i]);
                labelName.setMaximumSize(new Dimension(100, 30));
                labelName.setToolTipText(labelName.getText());
                panelIntParameters.add(labelName);
                panelIntParameters.add(editParameter);
                int index = i;
                editParameter.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int newvalue = ((Number) editParameter.getValue()).intValue();
                        output.setParameterValueInt(index, newvalue);
                        updateValues();
                    }
                });
            }
        }
        if (parameters != null) {
            panelDoubleParameters.setLayout(new GridLayout(parameters.length, 2));
            String[] names = output.getParameterNamesDouble();
            for (int i = 0; i < parameters.length; i++) {
                DecimalFormat df4 = new DecimalFormat("0.#####", new DecimalFormatSymbols(StartParameters.formatLocale));
                JFormattedTextField editParameter = new JFormattedTextField(df4);

                editParameter.setValue(parameters[i]);
                JLabel labelName = null;
                if (names != null && names.length > i) {
                    labelName = new JLabel(names[i]);
                    labelName.setToolTipText(names[i]);
                    editParameter.setToolTipText(names[i]);
                } else {
                    labelName = new JLabel();
                }
                labelName = new JLabel(names[i]);
                labelName.setMaximumSize(new Dimension(100, 30));
                labelName.setToolTipText(labelName.getText());
                panelDoubleParameters.add(labelName);
                panelDoubleParameters.add(editParameter);
                int index = i;
                editParameter.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        double newvalue = ((Number) editParameter.getValue()).doubleValue();
                        output.setParameterValueDouble(index, newvalue);
                        updateValues();
                    }
                });
            }
        }

        panelIntParameters.revalidate();
        panelDoubleParameters.revalidate();

        selfSelection = false;

    }

}
