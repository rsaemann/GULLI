/*
 * The MIT License
 *
 * Copyright 2022 Sämann.
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
package com.saemann.gulli.view.injection;

import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.scenario.injection.HEAreaInflow1DInformation;
import com.saemann.gulli.view.PaintManager;
import static com.saemann.gulli.view.injection.InjectionPanelPointlocation.gmtToLocal;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * Panel to specify parameters for the Area washoff Injection information using
 * HYSTEM EXTRAN 1D Area washoff parameters Mass per Area.
 *
 * @author Robert Sämann 2022
 */
class InjectionPanelAreaWashoff extends JPanel {

    private HEAreaInflow1DInformation info;
    private PaintManager paintManager;
    private final JSpinner spinnerMaterial;
    private final SpinnerDateModel modelInjection;
//    private final JSpinner spinnerInjection;
//    private final JSpinner.DateEditor dateEditorInjection;
    private final JCheckBox checkInjection;
//    private final SpinnerNumberModel modelDuration;
//    private final JSpinner spinnerDuration;
//    private final JTextField textRunoffParameter;
    private final JComboBox comboRunoffParameter;

    private final SpinnerNumberModel modelParticles;
    private final JSpinner spinnerParticles;
    private final SpinnerNumberModel modelLoad;
    private final JSpinner spinnerLoad;
    private final SpinnerNumberModel modelWashoff;
    private final JSpinner spinnerWashoff;

    private boolean selfChanging = false;

    protected InjectionPanelAreaWashoff(final HEAreaInflow1DInformation info, PaintManager paintManager) {
        super();
        setLayout(new GridLayout(5, 2));
        this.setBorder(new TitledBorder(new LineBorder(Color.green.darker(), 1, true), "Area Washoff 1D"));

        this.info = info;
        this.paintManager = paintManager;
        
        //Statistics string used for tooltip
        String tooltip=new String("<html>"+info.numberAreaObjects+" Area elements<br>"+(int)(info.effectiveArea)+"m² <br>"+(int)info.effectiveVolume+"m³ volume");
        this.setToolTipText(tooltip);
        //Name
        spinnerMaterial = new JSpinner(new SpinnerNumberModel(info.getMaterial().materialIndex, -1, Integer.MAX_VALUE, 1));
        this.add(spinnerMaterial);
        this.add(new JLabel("Material [" + info.getMaterial().materialIndex + "]:" + info.getMaterial().getName()));
        //Datespinners
//        JPanel panelSouthDate = new JPanel(new GridLayout(2, 1));
        modelInjection = new SpinnerDateModel(new Date(gmtToLocal((long) (info.getStarttimeSimulationsAfterSimulationStart() * 1000L))), null, null, GregorianCalendar.MINUTE);
        //Parameter
        comboRunoffParameter = new JComboBox<String>(HEAreaInflow1DInformation.runoffParameterList);
        if (info.getRunoffParameterName() != null) {
            comboRunoffParameter.setSelectedItem(info.getRunoffParameterName());
        } else {
            comboRunoffParameter.setSelectedItem("All");
        }
        this.add(new JLabel("Runoff parameter"));
        this.add(comboRunoffParameter);
        comboRunoffParameter.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getItem() == null || e.getItem().toString().isEmpty() || e.getItem().toString().equals("All")) {
                    info.setRunoffParameterName(null);
                } else {
                    info.setRunoffParameterName(e.getItem().toString());
                }
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        //Load        
        checkInjection = new JCheckBox("Areal load [kg/ha] ", info.isActive());
        checkInjection.setToolTipText("<html>Enables the spill at this start time after simulation start. <br>If disabled: no spill is released.</html>");
        this.add(checkInjection);
//        this.add(new JLabel(""));
        modelLoad = new SpinnerNumberModel(info.getMassload() * 10000., 0, Double.POSITIVE_INFINITY, 1);

        spinnerLoad = new JSpinner(modelLoad);
        JSpinner.NumberEditor loadEditor = new JSpinner.NumberEditor(spinnerLoad, "0.####");
        spinnerLoad.setToolTipText((int) (info.getMassload()) + " kg/m²");
        DecimalFormat f = loadEditor.getFormat();
        f = loadEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        DecimalFormatSymbols dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');

        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        f.setDecimalFormatSymbols(dfs);
        spinnerLoad.setEditor(loadEditor);
        this.add(spinnerLoad);

        //Number of particles
        modelParticles = new SpinnerNumberModel(info.getNumberOfParticles(), 0, Integer.MAX_VALUE, 5000);
        spinnerParticles = new JSpinner(modelParticles);
        JSpinner.NumberEditor particlesEditor = new JSpinner.NumberEditor(spinnerParticles, "# ##0");
//        DecimalFormat f = particlesEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);

        f.setDecimalFormatSymbols(dfs);
        spinnerParticles.setEditor(particlesEditor);
        this.add(new JLabel("Particles:"));
        this.add(spinnerParticles);

        //Washoff constant s [1/mm]        
        this.add(new JLabel("Washoff fraction [1/mm]"));
        modelWashoff = new SpinnerNumberModel(info.getWashoffConstant(), 0, 10, 0.05);

        spinnerWashoff = new JSpinner(modelWashoff);
        JSpinner.NumberEditor washoffEditor = new JSpinner.NumberEditor(spinnerWashoff, "0.###");

        f = washoffEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerWashoff.setEditor(washoffEditor);
        this.add(spinnerWashoff);

//        spinnerDuration.setEnabled(checkInjection.isSelected());
//        spinnerInjection.setEnabled(checkInjection.isSelected());
        this.spinnerMaterial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (selfChanging) {
                    return;
                }
                info.setMaterialID((int) spinnerMaterial.getValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.checkInjection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selfChanging) {
                    return;
                }
                info.setActive(checkInjection.isSelected());

                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerParticles.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                info.setNumberOfParticles(modelParticles.getNumber().intValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerLoad.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                selfChanging = true;
                info.setMassload(modelLoad.getNumber().doubleValue() * 0.0001);
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                    spinnerWashoff.setValue(info.getMass());
                }
                spinnerLoad.setToolTipText((info.getMassload()) + " kg/m²");

                selfChanging = false;
            }
        });

        this.spinnerWashoff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                selfChanging = true;
                info.setWashoffConstant(modelWashoff.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
                selfChanging = false;
            }
        });

        this.setPreferredSize(new Dimension(160, 120));
        this.setMinimumSize(new Dimension(160, 110));

    }

}
