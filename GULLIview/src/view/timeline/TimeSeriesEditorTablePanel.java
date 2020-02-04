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
import org.jfree.data.time.TimeSeries;

/**
 *
 * @author saemann
 */
public class TimeSeriesEditorTablePanel extends JPanel {

    private TimeSeriesTable table;
    private JScrollPane scrollPane;
    private JPanel panelButtons;
    private JButton buttonContainerindex2Label, buttonName2Label, buttonSymbol2Label;
    
    private CapacityTimelinePanel timelinePanel;

    public TimeSeriesEditorTablePanel() {
        this(new TimeSeriesTable());
    }

    public TimeSeriesEditorTablePanel(CapacityTimelinePanel timelinePanel) {
        this();
        table.setCollection(timelinePanel.getCollection());
        this.setTimelinePanel(timelinePanel);
        table.updateTableByCollection();
    }

    public TimeSeriesEditorTablePanel(final TimeSeriesTable table) {
        super(new BorderLayout());
        this.table = table;
        scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);
        
        this.timelinePanel = table.panel;

        

        panelButtons = new JPanel(null);

        BoxLayout box = new BoxLayout(panelButtons, BoxLayout.X_AXIS);
        panelButtons.setLayout(box);
        buttonContainerindex2Label = new JButton("Label&Sort by Index");
        panelButtons.add(buttonContainerindex2Label);
        buttonContainerindex2Label.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                ArrayList<TimeSeries> ts = new ArrayList<>(table.collection.getSeriesCount());
                for (int i = 0; i < table.collection.getSeriesCount(); i++) {
                    TimeSeries t = table.collection.getSeries(i);
                    ts.add(t);
                    ((SeriesKey) t.getKey()).label = ((SeriesKey) t.getKey()).containerIndex + "";
                }
                Collections.sort(ts, new Comparator<TimeSeries>() {
                    @Override
                    public int compare(TimeSeries t, TimeSeries t1) {
                        return ((SeriesKey) t.getKey()).containerIndex - ((SeriesKey) t1.getKey()).containerIndex;
                    }
                });
                table.collection.removeAllSeries();
                for (TimeSeries t : ts) {
                    table.collection.addSeries(t);
                }
                table.updateTableByCollection();
            }
        });
        buttonName2Label = new JButton("Label by Name");
        panelButtons.add(buttonName2Label);
        buttonName2Label.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                ArrayList<TimeSeries> ts = new ArrayList<>(table.collection.getSeriesCount());
                for (int i = 0; i < table.collection.getSeriesCount(); i++) {
                    TimeSeries t = table.collection.getSeries(i);
                    ts.add(t);
                    ((SeriesKey) t.getKey()).label = ((SeriesKey) t.getKey()).name + "";
                }

                table.updateTableByCollection();
            }
        });
        buttonSymbol2Label = new JButton("Label by Symbol");
        panelButtons.add(buttonSymbol2Label);
        buttonSymbol2Label.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                ArrayList<TimeSeries> ts = new ArrayList<>(table.collection.getSeriesCount());
                for (int i = 0; i < table.collection.getSeriesCount(); i++) {
                    TimeSeries t = table.collection.getSeries(i);
                    ts.add(t);
                    ((SeriesKey) t.getKey()).label = ((SeriesKey) t.getKey()).symbol + "";
                }

                table.updateTableByCollection();
            }
        });
        this.add(panelButtons, BorderLayout.SOUTH);

    
    }

    public TimeSeriesTable getTable() {
        return table;
    }

    public void setTimelinePanel(CapacityTimelinePanel timelinePanel) {
        this.timelinePanel = timelinePanel;
        table.setTimelinePanel(timelinePanel);
    }

}
