/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.view;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.model.topology.Manhole;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class SearchPanel extends JPanel {

    private JTextField text;

    private JButton buttonSearch;
    
    private JButton buttonSearchCellID;

    private Controller c;

    PaintManager paintManager;

    public SearchPanel(Controller c, PaintManager manager) {
        super(new BorderLayout());
        this.c=c;
        this.paintManager = manager;
        initLayout();
    }

    private void initLayout() {
        text = new JTextField();
        this.add(text, BorderLayout.NORTH);
        buttonSearch = new JButton("Search Network Element");
        buttonSearchCellID = new JButton("Search Surface Cell");
        this.add(buttonSearch, BorderLayout.CENTER);
        this.add(buttonSearchCellID,BorderLayout.SOUTH);

        buttonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String str = text.getText();
                if (str == null || str.isEmpty()) {
                    return;
                }

                for (Pipe manholesDrain : c.getNetwork().getPipes()) {
                    if (manholesDrain.getName().contains(str) || (manholesDrain.getAutoID() + "").contains(str)) {
                        System.out.println("Found a pipe " + manholesDrain.getName() + " (" + manholesDrain.getAutoID() + ")");
                        paintManager.setSelectedPipe(manholesDrain.getAutoID());
                        return;
                    }
                }
                
                 for (Manhole manholesDrain : c.getNetwork().getManholes()) {
                    if (manholesDrain.getName().contains(str) || (manholesDrain.getAutoID() + "").contains(str)) {
                        System.out.println("Found a Manhole " + manholesDrain.getName() + " (" + manholesDrain.getAutoID() + ")");
                        paintManager.setSelectedManhole(manholesDrain.getAutoID());
                        return;
                    }
                }
//                for (Pipe manholesDrain : network.getPipesSewer()) {
//                    if (manholesDrain.getName().contains(str) || (manholesDrain.getAutoID() + "").contains(str)) {
//                        System.out.println("Found a pipe " + manholesDrain.getName() + " (" + manholesDrain.getAutoID() + ")");
//                        System.out.println("from "+manholesDrain.getConnections()[0].getPosition()+" to "+manholesDrain.getConnections()[1].getPosition()+" length: "+manholesDrain.getLength());
//                        paintManager.setSelectedPipe(manholesDrain.getAutoID());
//                        return;
//                    }
//                }
            }
        });
        
        buttonSearchCellID.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String str = text.getText();
                if (str == null || str.isEmpty()) {
                    return;
                }
                long cellID=Long.parseLong(str);
                paintManager.selectLocationID(this, paintManager.layerSurfaceGrid, cellID);
                double[] mid = c.getSurface().getTriangleMids()[(int)cellID];
                try {
                    Coordinate longlat = c.getSurface().getGeotools().toGlobal(new Coordinate(mid[0], mid[1]),true);
                     paintManager.getMapViewer().setDisplayPositionByLatLon(longlat.y,longlat.x,22);
                } catch (TransformException ex) {
                    Logger.getLogger(SearchPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
               
            }
        });
    }
}
