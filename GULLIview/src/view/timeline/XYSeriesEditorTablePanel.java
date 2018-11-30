/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.timeline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author saemann
 */
public class XYSeriesEditorTablePanel extends JPanel {

    private XYSeriesTable table;
    private JScrollPane scrollPane;
    private JPanel panelButtons;
    private JButton buttonContainerindex2Label;
    final JTextField textTitle;
    private SpacelinePanel timelinePanel;

    public XYSeriesEditorTablePanel() {
        this(new XYSeriesTable());
    }

    public XYSeriesEditorTablePanel(final XYSeriesTable table) {
        super(new BorderLayout());
        this.table = table;
        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);
        textTitle = new JTextField();
        textTitle.setToolTipText("Title to display in Chart.");
        this.add(textTitle, BorderLayout.NORTH);
        this.timelinePanel = table.panel;

        textTitle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (timelinePanel != null) {
                    timelinePanel.panelChart.getChart().setTitle(textTitle.getText());
                }
            }
        });

        panelButtons = new JPanel(null);
        BoxLayout box = new BoxLayout(panelButtons, BoxLayout.X_AXIS);
        panelButtons.setLayout(box);
        buttonContainerindex2Label = new JButton("Label&Sort by Index");
        panelButtons.add(buttonContainerindex2Label);
        buttonContainerindex2Label.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                ArrayList<XYSeries> ts = new ArrayList<>(table.collection.getSeriesCount());
                for (int i = 0; i < table.collection.getSeriesCount(); i++) {
                    XYSeries t=table.collection.getSeries(i);
                    ts.add(t);
                    ((SeriesKey)t.getKey()).label=((SeriesKey)t.getKey()).containerIndex+"";
                }
                Collections.sort(ts, new Comparator<XYSeries>() {
                    @Override
                    public int compare(XYSeries t, XYSeries t1) {
                        return ((SeriesKey) t.getKey()).containerIndex - ((SeriesKey) t1.getKey()).containerIndex;
                    }
                });
                table.collection.removeAllSeries();
                for (XYSeries t : ts) {
                    table.collection.addSeries(t);
                }
                
                table.updateTableByCollection();
            }
        });
        this.add(panelButtons,BorderLayout.SOUTH);
    }

    public XYSeriesTable getTable() {
        return table;
    }

    public void setSpacelinePanel(SpacelinePanel spacelinePanel) {
        this.timelinePanel = spacelinePanel;
        table.setSpacelinePanel(spacelinePanel);
    }
    
    

}
