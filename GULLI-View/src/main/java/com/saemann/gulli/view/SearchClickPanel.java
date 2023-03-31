/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.view;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.model.GeoPosition;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.saemann.rgis.view.MapViewer;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class SearchClickPanel extends JPanel {

    private JTextField textCellID;
    private JTextField textUTM;
    private JTextField textWGS;

//    private JButton buttonSearch;
    private JButton buttonSearchCellID;

    private Controller c;

//    PaintManager paintManager;

    MapViewer map;

    public SearchClickPanel(Controller c,  MapViewer map) {
        super(new BorderLayout());
        this.c = c;
//        this.paintManager = manager;
        this.map=map;
        initLayout();
    }

    private void initLayout() {
        textWGS = new JTextField();
        textUTM = new JTextField();
        textCellID = new JTextField();

        JPanel panelOutGrid = new JPanel(new GridLayout(3, 2));
        panelOutGrid.add(new JLabel("Long. / Lat. :"));
        panelOutGrid.add(textWGS);
        panelOutGrid.add(new JLabel("UTM x / y :"));
        panelOutGrid.add(textUTM);
        panelOutGrid.add(new JLabel("Cell ID :"));
        panelOutGrid.add(textCellID);

        this.add(panelOutGrid, BorderLayout.CENTER);
//        buttonSearch = new JButton("Search Surface Elemen");
        buttonSearchCellID = new JButton("Search Surface Cell");
//        this.add(buttonSearch, BorderLayout.CENTER);
        this.add(buttonSearchCellID, BorderLayout.NORTH);

//Add actionlistener to listen for clicks on the mapviewer.
        final MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent me) {
                if (me.getButton() == 1) {
                    try {
                        map.removeMouseListener(this);
                        Point2D.Double latlon = map.clickPoint;
                        GeoPosition p = new GeoPosition(latlon.x, latlon.y);
                        System.out.println("Clicked at " + latlon.x + ", " + latlon.y);
                        textWGS.setText(latlon.y + " ,  " + latlon.x);

                        if (c.getSurface() != null) {
                            Coordinate utm = c.getSurface().getGeotools().toUTM(p.getLongitude(), p.getLatitude());
                            textUTM.setText(utm.x + " ,  " + utm.y);
                            int cellID = c.getSurface().findContainingTriangle(utm.x, utm.y, 50);
                            textCellID.setText(cellID + "");
                        } else {
                            textUTM.setText("No surface defined.");
                            textCellID.setText("No surface defined.");
                        }

                    } catch (TransformException ex) {
                        Logger.getLogger(SearchClickPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        };
        buttonSearchCellID.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                map.addMouseListener(ma);
                buttonSearchCellID.setText("Click Map Position");
            }
        });

    }
}
