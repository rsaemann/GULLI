/*
 * The MIT License
 *
 * Copyright 2020 robert saemann.
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

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.SimulationActionAdapter;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.model.material.Material;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.rgis.view.MapViewer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

/**
 *
 * @author saemann
 */
public class InjectionOrganisatorPanel extends JPanel {

    protected TitledBorder borderMaterials, borderInjections;

    protected JPanel panelMaterialSurrounding, panelInjectionSurrounding;
    protected JPanel panelMaterials, panelInjections,panelInjectionButtons;

    protected JButton buttonNewMaterial;
    private JButton buttonNewInjectionPoint;
    private JButton buttonNewInjectionArea;

    protected Controller control;
    protected MapViewer map;
    protected PaintManager paintManager;

    public InjectionOrganisatorPanel(Controller control, MapViewer map, PaintManager pm) {
        super(new BorderLayout());
        this.control = control;
        this.map = map;
        this.paintManager = pm;
        panelMaterials = new JPanel();
        panelMaterials.setLayout(new BoxLayout(panelMaterials, BoxLayout.Y_AXIS));
        JScrollPane scrollMaterial = new JScrollPane(panelMaterials);
        borderMaterials = new TitledBorder("Materials");
        panelMaterialSurrounding=new JPanel(new BorderLayout());
        panelMaterialSurrounding.setBorder(borderMaterials);
        panelMaterialSurrounding.setPreferredSize(new Dimension(100, 220));
        panelMaterialSurrounding.add(scrollMaterial, BorderLayout.CENTER);
        buttonNewMaterial=new JButton("New Material");
        panelMaterialSurrounding.add(buttonNewMaterial,BorderLayout.NORTH);
        this.add(panelMaterialSurrounding,BorderLayout.NORTH);

        panelInjections = new JPanel();
        panelInjectionSurrounding=new JPanel(new BorderLayout());
        panelInjectionSurrounding.setMaximumSize(new Dimension(250,200));
        panelInjections.setLayout(new BoxLayout(panelInjections, BoxLayout.Y_AXIS));
        JScrollPane scrollInjection = new JScrollPane(panelInjections);
        borderInjections = new TitledBorder("Injections");
        panelInjectionSurrounding.setBorder(borderInjections);
//        scrollInjection.setPreferredSize(new Dimension(100, 900));
        panelInjectionSurrounding.add(scrollInjection, BorderLayout.CENTER);
        this.add(panelInjectionSurrounding,BorderLayout.CENTER);
        panelInjectionButtons=new JPanel(new GridLayout(1, 2));
        panelInjectionSurrounding.add(panelInjectionButtons,BorderLayout.NORTH);
        

        control.addActioListener(new LoadingActionListener() {
            @Override
            public void actionFired(Action action, Object source) {
            }

            @Override
            public void loadNetwork(Network network, Object caller) {
            }

            @Override
            public void loadSurface(Surface surface, Object caller) {
            }

            @Override
            public void loadScenario(Scenario scenario, Object caller) {
                recreatePanels();
            }
        });
        
        control.addSimulationListener(new SimulationActionAdapter(){
            @Override
            public void simulationRESET(Object caller) {
                recreatePanels();
            }

            @Override
            public void simulationSTART(Object caller) {
                updatePanels();
            }
            
        });

        buttonNewMaterial.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Material mat=new Material("neu", 1000, true);
                if(control.getScenario()!=null){
                    control.getScenario().getMaterials().add(mat);
                    recreatePanels();
                }
            }
        });

        //Add Injection via button
        buttonNewInjectionPoint = new JButton("New Point Injection");
        panelInjectionButtons.add(buttonNewInjectionPoint,BorderLayout.WEST);
        buttonNewInjectionPoint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                InjectionInformation ininfo = new InjectionInformation(0, 1, 1000, new Material("neu", 1000, true), 0, 1);
                ininfo.spillOnSurface = control.getSurface() != null;
                control.getLoadingCoordinator().addManualInjection(ininfo);
                control.recalculateInjections();
            }
        });

        buttonNewInjectionArea = new JButton("New Diffusive Injection");
        panelInjectionButtons.add(buttonNewInjectionArea,BorderLayout.EAST);
        buttonNewInjectionArea.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                Material m=new Material("diffusiv", 1000, true);
                m.travellengthToMeasure=100;
                InjectionInformation ininfo = new InjectionInformation(control.getSurface(), 0, 500, 10000, m, 0, 0);
                ininfo.spilldistributed = true;
                ininfo.spillOnSurface = true;
                ininfo.setTriangleID(0);
                control.getLoadingCoordinator().addManualInjection(ininfo);
                control.recalculateInjections();
            }
        });
//
//        panelTabLoading.add(panelInjection, BorderLayout.CENTER);
    }
    
    public void updatePanels(){
        for (Component component : panelMaterials.getComponents()) {
            if(component instanceof MaterialPanel){
                ((MaterialPanel)component).updateValues();                
            }
        }
//        for (Component component : panelInjections.getComponents()) {
//            if(component instanceof InjectionPanelPointlocation){
//                ((InjectionPanelPointlocation)component).;                
//            }
//        }
    }

    public void recreatePanels() {
        panelInjections.removeAll();
        panelMaterials.removeAll();
        createMaterialPanels();
        createInjectionPanels();
    }

    private void createMaterialPanels() {
        if (control.getScenario() == null) {
            borderMaterials.setTitle("No Materials: No Scenario");
            return;
        }
        if (control.getScenario().getMaterials() == null) {
            borderMaterials.setTitle("No Materials");
            return;
        }
        borderMaterials.setTitle((control.getScenario().getMaterials().size()) + " Materials");
        for (Material mat : control.getScenario().getMaterials()) {
            MaterialPanel mp = new MaterialPanel(mat);
            panelMaterials.add(mp);
        }
        panelMaterials.revalidate();
        panelMaterials.repaint();
    }

    private void createInjectionPanels() {
        if (control.getScenario() == null) {
            borderInjections.setTitle("No Injections: No Scenario");
            return;
        }
        if (control.getScenario().getInjections() == null) {
            borderInjections.setTitle("No Injections");
            return;
        }
        borderInjections.setTitle((control.getScenario().getInjections().size()) + " Injections");
        for (InjectionInformation inj : control.getScenario().getInjections()) {
            if (inj.spilldistributed) {
                InjectionPanelAreal ia = new InjectionPanelAreal(inj, map, paintManager);
                panelInjections.add(ia);
            } else {
                InjectionPanelPointlocation ip = new InjectionPanelPointlocation(inj, map, paintManager);
                panelInjections.add(ip);
            }
        }
        panelInjections.revalidate();
        panelInjections.repaint();
    }

}
