package com.saemann.gulli.view.injection;

import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.scenario.injection.InjectionInflowInformation;
import com.saemann.gulli.view.PaintManager;
import static com.saemann.gulli.view.injection.InjectionPanelPointlocation.gmtToLocal;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JPanel;

/**
 * Displays information about InjectionInformation for an areal, diffusive
 * Injection
 *
 * @author saemann
 */
public class InjectionPanelInflow extends JPanel {

    private InjectionInflowInformation info;
    private PaintManager paintManager;
    private final JSpinner spinnerMaterial;
    private final SpinnerDateModel modelInjection;
    private final JSpinner spinnerInjection;
    private final JSpinner.DateEditor dateEditorInjection;
    private final JCheckBox checkInjectionDuration;
    private final SpinnerNumberModel modelDuration;
    private final JSpinner spinnerDuration;
    private final SpinnerNumberModel modelLoad;
    private final JSpinner spinnerLoad;
    private final SpinnerNumberModel modelParticles;
    private final JSpinner spinnerParticles;
    private final SpinnerNumberModel modelConcentration;
    private final JSpinner spinnerCOncentration;
    private final SpinnerNumberModel modelMass;
    private final JSpinner spinnerMass;

    private boolean selfChanging = false;

    protected InjectionPanelInflow(final InjectionInflowInformation info, PaintManager paintManager) {
        super();
        setLayout(new GridLayout(7, 2));
        this.setBorder(new TitledBorder(new LineBorder(Color.blue.darker(), 1, true),"Inflow"));

        this.info = info;
        this.paintManager = paintManager;
        
        //Name
        spinnerMaterial = new JSpinner(new SpinnerNumberModel(info.getMaterial().materialIndex, 0, Integer.MAX_VALUE, 1));
        this.add(spinnerMaterial);
        this.add(new JLabel("Material [" + info.getMaterial().materialIndex + "]:" + info.getMaterial().getName()));
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
        JLabel lp = new JLabel("Particles:");
        try {
            lp.setToolTipText(info.getManholes().length + " manholes");

        } catch (Exception e) {
        }
        this.add(lp);
        this.add(spinnerParticles);

        //Concentration        
        this.add(new JLabel("Concentration [kg/m^3]"));
        modelConcentration = new SpinnerNumberModel(info.getConcentration(), 0, Double.POSITIVE_INFINITY, 0.001);

        spinnerCOncentration = new JSpinner(modelConcentration);
        JSpinner.NumberEditor loadEditor = new JSpinner.NumberEditor(spinnerCOncentration, "0.####");
        f = loadEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerCOncentration.setEditor(loadEditor);
        this.add(spinnerCOncentration);

        //Load
        modelLoad = new SpinnerNumberModel(0., 0., Double.POSITIVE_INFINITY, 0.001);
        modelLoad.setValue(info.getLoad());
        spinnerLoad = new JSpinner(modelLoad);
        spinnerLoad.setPreferredSize(new Dimension(60, 12));
        spinnerLoad.setToolTipText((int)(info.getLoad()*10000)+" kg/ha");
        this.add(new JLabel("Load [kg/m^2]"));
        this.add(spinnerLoad);

        //Mass    
        JLabel lm = new JLabel("Mass [kg]");
        try {
            lm.setToolTipText("TotalinflowVolume: " + info.getTotalvolume() + " m^3");

        } catch (Exception e) {
        }
        this.add(lm);
        modelMass = new SpinnerNumberModel(info.getMass(), 0, Double.POSITIVE_INFINITY, 10.);

        spinnerMass = new JSpinner(modelMass);
        JSpinner.NumberEditor massEditor = new JSpinner.NumberEditor(spinnerMass, "0.###");

        f = massEditor.getFormat();
        f.setDecimalFormatSymbols(new DecimalFormatSymbols(StartParameters.formatLocale));
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        dfs = f.getDecimalFormatSymbols();
        dfs.setGroupingSeparator(' ');
        f.setDecimalFormatSymbols(dfs);
        spinnerMass.setEditor(massEditor);
        this.add(spinnerMass);
        try {
            
            this.setToolTipText("<html><b>Inflow</b>"
                    + "<br> Area: " + (int) info.getTotalArea()+ " m² = " + (int) (info.getTotalArea() / 10000) + " ha"
                    + "<br>Volume: " + (int) info.getTotalvolume() + "m^3 </html>");
        } catch (Exception e) {
        }

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
        this.spinnerInjection.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                info.setStart(InjectionPanelPointlocation.localToGMT(modelInjection.getDate().getTime()) / 1000.);
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }

        });

        this.spinnerDuration.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                info.setDuration(modelDuration.getNumber().doubleValue() * 60.);
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.checkInjectionDuration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selfChanging) {
                    return;
                }
                info.setActive(checkInjectionDuration.isSelected());
                spinnerDuration.setEnabled(checkInjectionDuration.isSelected());
                spinnerInjection.setEnabled(checkInjectionDuration.isSelected());
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
                info.setLoad(modelLoad.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                    spinnerMass.setValue(info.getMass());
                    spinnerCOncentration.setValue(info.getConcentration());
                    spinnerLoad.setValue(info.getLoad());
                }
                selfChanging = false;
            }
        });

        this.spinnerCOncentration.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                if (selfChanging) {
                    return;
                }
                selfChanging = true;
                info.setConcentration(modelConcentration.getNumber().doubleValue());
                if (info.hasChanged()) {
                    setBorder(new TitledBorder("changed"));
                    spinnerMass.setValue(info.getMass());
                    spinnerCOncentration.setValue(info.getConcentration());
                    spinnerLoad.setValue(info.getLoad());
                }
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
                    setBorder(new TitledBorder("changed"));
                    spinnerMass.setValue(info.getMass());
                    spinnerCOncentration.setValue(info.getConcentration());
                    spinnerLoad.setValue(info.getLoad());
                }
                selfChanging = false;
            }
        });

        this.setPreferredSize(new Dimension(160, 150));
        this.setMinimumSize(new Dimension(160, 130));

    }

}
