package com.saemann.gulli.view;

import com.saemann.gulli.core.control.StartParameters;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Calculator;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Constant;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Calculator;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Constant;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Fischer;
import com.saemann.gulli.core.model.material.dispersion.surface.Dispersion2D_Waterlevel;
import java.awt.BorderLayout;
import java.text.DecimalFormatSymbols;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JSeparator;

/**
 * Displays information about InjectionInformation
 *
 * @author saemann
 */
public class MaterialPanel extends JPanel {

    protected Material material;

    protected JTextField textname;

    protected JLabel labelIndex;
    protected JButton buttonCoordinate;
    public static final DecimalFormat df = new DecimalFormat("0.###", new DecimalFormatSymbols(StartParameters.formatLocale));

    protected JComboBox<Material.ROUTING> comboRouting;
    protected JComboBox<Material.DISPERSION_PIPE> comboDispersionPipe;
    protected JPanel panelDispersionPipeParameters;
    protected JComboBox<Material.DISPERSION_SURFACE> comboDispersionSurface;
    protected JPanel panelDispersionSurfaceParameters;

    protected MaterialPanel(Material mat) {
        super();
        this.setMinimumSize(new Dimension(100, 150));
        this.setPreferredSize(this.getMinimumSize());
//        this.setSize(this.getPreferredSize());
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.material = mat;
        this.setBorder(new LineBorder(Color.darkGray, 1, true));
        df.getDecimalFormatSymbols().setGroupingSeparator(' ');
        //Name
        textname = new JTextField(material.getName());
        labelIndex = new JLabel("Index: " + material.materialIndex + "  Name:");
        JPanel panelName = new JPanel(new BorderLayout());
        panelName.add(labelIndex, BorderLayout.WEST);
        panelName.add(textname, BorderLayout.CENTER);
        this.add(panelName);
        this.add(new JSeparator(JSeparator.HORIZONTAL));

        JPanel panelRoutingChooser = new JPanel(new BorderLayout());
        comboRouting = new JComboBox<>(Material.ROUTING.values());
        panelRoutingChooser.add(new JLabel("Routing: "), BorderLayout.WEST);
        panelRoutingChooser.add(comboRouting, BorderLayout.CENTER);
        this.add(panelRoutingChooser);
        this.add(new JSeparator(JSeparator.HORIZONTAL));

        JPanel panelPipeChooser = new JPanel(new BorderLayout());
        panelPipeChooser.add(new JLabel("Pipe Dispersion:   "), BorderLayout.WEST);
        comboDispersionPipe = new JComboBox<>(Material.DISPERSION_PIPE.values());
        panelPipeChooser.add(comboDispersionPipe, BorderLayout.CENTER);
        this.add(panelPipeChooser);
        panelDispersionPipeParameters = new JPanel();
        this.add(panelDispersionPipeParameters);
        this.add(new JSeparator(JSeparator.HORIZONTAL));

        JPanel panelSurfaceChooser = new JPanel(new BorderLayout());
        panelSurfaceChooser.add(new JLabel("Surface Dispersion:"), BorderLayout.WEST);
        comboDispersionSurface = new JComboBox<>(Material.DISPERSION_SURFACE.values());
        panelSurfaceChooser.add(comboDispersionSurface, BorderLayout.CENTER);
        this.add(panelSurfaceChooser);
        panelDispersionSurfaceParameters = new JPanel();
        this.add(panelDispersionSurfaceParameters);

        textname.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                material.setName(textname.getText());
            }
        });

        comboDispersionSurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboDispersionSurface.getSelectedItem().equals(Material.DISPERSION_SURFACE.CONSTANT)) {
                    if (!(material.getDispersionCalculatorSurface() instanceof Dispersion2D_Constant)) {
                        Dispersion2D_Constant dcc = new Dispersion2D_Constant();
                        material.setDispersionCalculatorSurface(dcc);
                        updateValues();
                    }
                } else if (comboDispersionSurface.getSelectedItem().equals(Material.DISPERSION_SURFACE.FISCHER)) {
                    if (!(material.getDispersionCalculatorSurface() instanceof Dispersion2D_Fischer)) {
                        Dispersion2D_Fischer dcc = new Dispersion2D_Fischer();
                        material.setDispersionCalculatorSurface(dcc);
                        updateValues();
                    }
                } else if (comboDispersionSurface.getSelectedItem().equals(Material.DISPERSION_SURFACE.WATERLEVEL)) {
                    if (!(material.getDispersionCalculatorSurface() instanceof Dispersion2D_Waterlevel)) {
                        Dispersion2D_Waterlevel dcc = new Dispersion2D_Waterlevel(1);
                        material.setDispersionCalculatorSurface(dcc);
                        updateValues();
                    }
                } else {
                    System.out.println("Unknown enum for Materiel.DISPERSION_SURFACE '" + comboDispersionSurface.getSelectedItem() + "'");
                }
            }
        });
        updateValues();
//        this.setPreferredSize(new Dimension(160, 95));
//        this.setMinimumSize(new Dimension(160, 90));
    }

    public void updateValues() {
        textname.setText(material.getName());
        labelIndex.setText("Index: " + material.materialIndex);

        //Pipe dispersion
        Dispersion1D_Calculator pc = material.getDispersionCalculatorPipe();
        if (pc instanceof Dispersion1D_Constant) {
            comboDispersionPipe.setSelectedItem(Material.DISPERSION_PIPE.CONSTANT);
        } else {
            System.out.println("Unknown Dispersion calculator 1d '" + pc.getClass() + " for material " + material.toString());
        }

        panelDispersionPipeParameters.removeAll();
        final double[] parameters = pc.getParameterValues();
        panelDispersionPipeParameters.setLayout(new GridLayout(1, pc.getNumberOfParameters() * 3));
        for (int i = 0; i < pc.getNumberOfParameters(); i++) {
            JLabel labelName = new JLabel(pc.getParameterDescription()[i]);
            labelName.setMaximumSize(new Dimension(100, 30));
            labelName.setToolTipText(labelName.getText());
            JFormattedTextField editParameter = new JFormattedTextField(DecimalFormat.getNumberInstance(StartParameters.formatLocale));
            editParameter.setValue(parameters[i]);
            editParameter.setToolTipText(labelName.getText());
            JLabel labelUnit = new JLabel(pc.getParameterUnits()[i]);
            panelDispersionPipeParameters.add(labelName);
            panelDispersionPipeParameters.add(editParameter);
            panelDispersionPipeParameters.add(labelUnit);
            int index = i;
            editParameter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double newvalue = ((Number) editParameter.getValue()).doubleValue();
                    parameters[index] = newvalue;
                    pc.setParameterValues(parameters);
                    updateValues();
                }
            });
        }
        panelDispersionPipeParameters.revalidate();

        //Surface dispersion
        Dispersion2D_Calculator sc = material.getDispersionCalculatorSurface();
        if (sc instanceof Dispersion2D_Constant) {
            comboDispersionSurface.setSelectedItem(Material.DISPERSION_SURFACE.CONSTANT);
        } else if (sc instanceof Dispersion2D_Fischer) {
            comboDispersionSurface.setSelectedItem(Material.DISPERSION_SURFACE.FISCHER);
        } else if (sc instanceof Dispersion2D_Waterlevel) {
            comboDispersionSurface.setSelectedItem(Material.DISPERSION_SURFACE.WATERLEVEL);
        } else {
            System.out.println("Unknown Dispersion calculator 2d '" + sc.getClass() + " for material " + material.toString());
        }

        panelDispersionSurfaceParameters.removeAll();
        final double[] surfParams = sc.getParameterValues();
        panelDispersionSurfaceParameters.setLayout(new GridLayout(1, surfParams.length * 3));
        for (int i = 0; i < surfParams.length; i++) {
            JLabel labelName = new JLabel(sc.getParameterOrderDescription()[i]);
            labelName.setMaximumSize(new Dimension(100, 30));
            labelName.setToolTipText(labelName.getText());
            JFormattedTextField editParameter = new JFormattedTextField(DecimalFormat.getNumberInstance(StartParameters.formatLocale));
            editParameter.setValue(surfParams[i]);
            editParameter.setToolTipText(labelName.getText());
            JLabel labelUnit = new JLabel(sc.getParameterUnits()[i]);
            panelDispersionSurfaceParameters.add(labelName);
            panelDispersionSurfaceParameters.add(editParameter);
            panelDispersionSurfaceParameters.add(labelUnit);
            int index = i;
            editParameter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double newvalue = ((Number) editParameter.getValue()).doubleValue();
                    surfParams[index] = newvalue;
                    sc.setParameterValues(surfParams);
                    updateValues();
                }
            });
        }
        panelDispersionSurfaceParameters.revalidate();
    }

}
