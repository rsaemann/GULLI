/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.topology.Network;
import model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class SearchPanel extends JPanel {

    private JTextField text;

    private JButton buttonSearch;

    private final Network network;

    PaintManager paintManager;

    public SearchPanel(Network network, PaintManager manager) {
        super(new BorderLayout());
        this.network = network;
        this.paintManager = manager;
        initLayout();
    }

    private void initLayout() {
        text = new JTextField();
        this.add(text, BorderLayout.NORTH);
        buttonSearch = new JButton("Search");
        this.add(buttonSearch, BorderLayout.CENTER);

        buttonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String str = text.getText();
                if (str == null || str.isEmpty()) {
                    return;
                }

                for (Pipe manholesDrain : network.getPipes()) {
                    if (manholesDrain.getName().contains(str) || (manholesDrain.getAutoID() + "").contains(str)) {
                        System.out.println("Found a pipe " + manholesDrain.getName() + " (" + manholesDrain.getAutoID() + ")");
                        paintManager.setSelectedPipe(manholesDrain.getAutoID());
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
    }
}
