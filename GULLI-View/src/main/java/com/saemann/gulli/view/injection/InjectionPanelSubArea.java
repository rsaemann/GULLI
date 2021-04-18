package com.saemann.gulli.view.injection;

import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.scenario.injection.InjectionSubArealInformation;
import static com.saemann.gulli.view.injection.InjectionPanelPointlocation.gmtToLocal;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Displays information about InjectionInformation for an areal, diffusive
 * Injection
 *
 * @author saemann
 */
public class InjectionPanelSubArea extends JPanel {

    private InjectionSubArealInformation info;
//    private PaintManager paintManager;
    private final JSpinner spinnerMaterial;
    private final SpinnerDateModel modelInjection;
    private final JSpinner spinnerInjection;
    private final JSpinner.DateEditor dateEditorInjection;
    private final JTextField textContains;
    private final JCheckBox checkInjectionDuration;
//    private final SpinnerNumberModel modelDuration;
//    private final JSpinner spinnerDuration;
    private final SpinnerNumberModel modelParticles;
    private final JSpinner spinnerParticles;
    private final SpinnerNumberModel modelLoad;
    private final JSpinner spinnerLoad;
    private final SpinnerNumberModel modelMass;
    private final JSpinner spinnerMass;

    private boolean selfChanging = false;

    protected InjectionPanelSubArea(final InjectionSubArealInformation info) {
        super();
        setLayout(new GridLayout(6, 2));
        this.setBorder(new TitledBorder(new LineBorder(Color.green.darker(), 1, true), "SubArea"));

        this.info = info;
//        this.paintManager = paintManager;
        //Name
        spinnerMaterial = new JSpinner(new SpinnerNumberModel(info.getMaterial().materialIndex, -1, Integer.MAX_VALUE, 1));
        this.add(spinnerMaterial);
        this.add(new JLabel("Material [" + info.getMaterial().materialIndex + "]:" + info.getMaterial().getName()));

        //Contains-Text filter
        textContains = new JTextField(20);
        textContains.setToolTipText("The subarea name should contain this text.");
        if (info.getNameFilter() != null) {
            textContains.setText(info.getNameFilter());
        }
        this.add(new JLabel("Filter string:"));
        this.add(textContains);

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

//        modelDuration = new SpinnerNumberModel(0., 0., Double.POSITIVE_INFINITY, 5);
//        modelDuration.setValue(info.getDurationSeconds() / 60);
//        spinnerDuration = new JSpinner(modelDuration);
//        spinnerDuration.setPreferredSize(new Dimension(60, 12));
//        this.add(new JLabel("Duration [min]"));
//        this.add(spinnerDuration);
        //Number of particles
        modelParticles = new SpinnerNumberModel(info.getNumberOfParticles(), 0, Integer.MAX_VALUE, 5000);
        spinnerParticles = new JSpinner(modelParticles);
        JSpinner.NumberEditor particlesEditor = new JSpinner.NumberEditor(spinnerParticles, "# ##0");
        DecimalFormat f = particlesEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        DecimalFormatSymbols dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerParticles.setEditor(particlesEditor);
        this.add(new JLabel("Particles:"));
        this.add(spinnerParticles);

        //Load        
        this.add(new JLabel("Areal load [kg/m^2]"));
        modelLoad = new SpinnerNumberModel(info.getLoad(), 0, Double.POSITIVE_INFINITY, 0.001);

        spinnerLoad = new JSpinner(modelLoad);
        JSpinner.NumberEditor loadEditor = new JSpinner.NumberEditor(spinnerLoad, "0.####");
        spinnerLoad.setToolTipText((int) (info.getLoad() * 10000) + " kg/ha");
        f = loadEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerLoad.setEditor(loadEditor);
        this.add(spinnerLoad);

        //Mass        
        this.add(new JLabel("Mass [kg]"));
        modelMass = new SpinnerNumberModel(info.getMass(), 0, Double.POSITIVE_INFINITY, 10.);

        spinnerMass = new JSpinner(modelMass);
        JSpinner.NumberEditor massEditor = new JSpinner.NumberEditor(spinnerMass, "0.###");
        try {
            if (this.info.getSurface() != null) {
                massEditor.setToolTipText((int) this.info.getArea() + " m² = " + (int) (this.info.getArea() / 10000.) + " ha");
                spinnerMass.setToolTipText(massEditor.getToolTipText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        f = massEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerMass.setEditor(massEditor);
        this.add(spinnerMass);
        this.setToolTipText("<html><b>2D SubArea</b>"
                + "<br> Area: " + (int) info.getArea() + " m² = " + (int) (info.getArea() / 10000) + " ha"
                + " </html>");

        spinnerInjection.setEnabled(checkInjectionDuration.isSelected());

        this.spinnerMaterial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (selfChanging) {
                    return;
                }
                info.setMaterialID((int) spinnerMaterial.getValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));

                }
            }
        });

        this.textContains.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                info.setNameFilter(textContains.getText());
                selfChanging = true;
                info.setMass(modelMass.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
                    spinnerLoad.setValue(info.getLoad());
                    spinnerLoad.setToolTipText((int) (info.getLoad() * 10000) + " kg/ha");
                    spinnerMass.setValue(info.getMass());
                    InjectionPanelSubArea.this.setToolTipText("<html><b>2D SubArea</b>"
                            + "<br> Area: " + (int) info.getArea() + " m² = " + (int) (info.getArea() / 10000) + " ha"
                            + " </html>");
                }
                selfChanging = false;
            }
        });

        this.checkInjectionDuration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selfChanging) {
                    return;
                }
                info.setActive(checkInjectionDuration.isSelected());
                spinnerInjection.setEnabled(checkInjectionDuration.isSelected());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
                }
            }
        });

        this.spinnerInjection.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                info.setStart(InjectionPanelPointlocation.localToGMT(modelInjection.getDate().getTime()) / 1000.);
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
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
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
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
                info.setLoad(modelLoad.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
                    spinnerMass.setValue(info.getMass());
                }
                spinnerLoad.setToolTipText((int) (info.getLoad() * 10000) + " kg/ha");

                selfChanging = false;
            }
        });

        this.spinnerMass.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                selfChanging = true;
                info.setMass(modelMass.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed - Press Reset button to recalculate (takes a while)"));
                    spinnerLoad.setValue(info.getLoad());
                    spinnerLoad.setToolTipText((int) (info.getLoad() * 10000) + " kg/ha");

                }
                selfChanging = false;
            }
        });

        this.setPreferredSize(new Dimension(160, 125));
        this.setMinimumSize(new Dimension(160, 110));

    }

}
