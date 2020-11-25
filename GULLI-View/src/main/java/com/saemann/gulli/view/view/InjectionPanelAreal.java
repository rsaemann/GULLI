package com.saemann.gulli.view.view;

import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import view.MapViewer;

/**
 * Displays information about InjectionInformation for an areal, diffusive
 * Injection
 *
 * @author saemann
 */
public class InjectionPanelAreal extends InjectionPanelPointlocation {

    protected InjectionPanelAreal(final InjectionInformation info, final MapViewer map, PaintManager paintManager) {
        super();
        setLayout(new GridLayout(5, 2));
        this.setBorder(new LineBorder(Color.darkGray, 1, true));

        this.info = info;
        this.map = map;
        this.paintManager = paintManager;
        //Name
        textname = new JTextField(info.getMaterial().getName());
        this.add(new JLabel("Material [" + info.getMaterial().materialIndex + "]:"));
        this.add(textname);
        //Datespinners
//        JPanel panelSouthDate = new JPanel(new GridLayout(2, 1));
        modelInjection = new SpinnerDateModel(new Date(gmtToLocal((long) (info.getStarttimeSimulationsAfterSimulationStart() * 1000L))), null, null, GregorianCalendar.MINUTE);
        spinnerInjection = new JSpinner(modelInjection);
        dateEditorInjection = new JSpinner.DateEditor(spinnerInjection, "HH'h', mm'm', ss's'");
        dateEditorInjection.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        spinnerInjection.setEditor(dateEditorInjection);
        checkInjectionDuration = new JCheckBox("Inject ", info.isActive());
        checkInjectionDuration.setToolTipText("<html>Enables the spill at this start time after simulation start. <br>If disabled: no spill is released.</html>");
        this.add(checkInjectionDuration);
        this.add(spinnerInjection);

        modelDuration = new SpinnerNumberModel(0., 0., Double.POSITIVE_INFINITY, 5);
        modelDuration.setValue(info.getDurationSeconds() / 60);
        spinnerDuration = new JSpinner(modelDuration);
        spinnerDuration.setPreferredSize(new Dimension(60, 12));
        this.add(new JLabel("Duration [min]"));
        this.add(spinnerDuration);

        //Number of particles
        modelParticles = new SpinnerNumberModel(info.getNumberOfParticles(), 1, Integer.MAX_VALUE, 1000);
        spinnerParticles = new JSpinner(modelParticles);
        this.add(new JLabel("Particles:"));
        this.add(spinnerParticles);

        this.add(new JLabel("Distributed"));

        this.spinnerInjection.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
//                System.out.println("model injection time: " + localToGMT(modelInjection.getDate().getTime()));
                info.setStart(localToGMT(modelInjection.getDate().getTime()) / 1000.);
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerDuration.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                info.setDuration(modelDuration.getNumber().doubleValue() * 60.);
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.checkInjectionDuration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                info.setActive(checkInjectionDuration.isSelected());
                spinnerDuration.setEnabled(checkInjectionDuration.isSelected());
                spinnerInjection.setEnabled(checkInjectionDuration.isSelected());
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerParticles.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                info.setNumberOfParticles(modelParticles.getNumber().intValue());
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        textname.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                info.getMaterial().setName(textname.getText());
            }
        });

        this.setPreferredSize(new Dimension(160, 95));
        this.setMinimumSize(new Dimension(160, 90));

    }

    public InjectionPanelAreal(MapViewer map) {
        this(null, map, null);

    }

    public long localToGMT(long local) {
        return local + localCalendar.get(GregorianCalendar.ZONE_OFFSET);
    }

    public long gmtToLocal(long gmt) {
        return gmt - localCalendar.get(GregorianCalendar.ZONE_OFFSET);
    }

}
