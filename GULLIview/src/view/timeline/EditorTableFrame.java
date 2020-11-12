package view.timeline;

import control.Controller;
import control.StartParameters;
import control.listener.CapacitySelectionListener;
import io.timeline.TimeSeries_IO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.TransferHandler;
import model.topology.Capacity;
import org.jfree.data.time.TimeSeries;
import view.ViewController;
import view.timeline.customCell.StrokeEditor;

/**
 * A frem to display the measured and hydraulic timelines of pipes and manholes
 * together with a table panel to control the layout of the plot.
 *
 * @author saemann
 */
public class EditorTableFrame extends JFrame {

    TimeSeriesEditorTablePanel tablePanel;
    CapacityTimelinePanel timelinePanel;
    JPanel panelSouth;
    JSplitPane splitpane;
    OutflowMinimizerPanel panelMinimizer;
    ViewController viewController;

    public EditorTableFrame() {
        this(null, null, null);
    }

    public EditorTableFrame(String title, Controller control, ViewController vc) throws HeadlessException {
        super("Graphs");
        this.setLayout(new BorderLayout());
        this.viewController = vc;
        tablePanel = new TimeSeriesEditorTablePanel();
        tablePanel.setMinimumSize(new Dimension(100, 100));
        timelinePanel = new CapacityTimelinePanel(title, control);
        timelinePanel.showCheckBoxPanel(false);
        timelinePanel.setMinimumSize(new Dimension(100, 100));
        timelinePanel.collection = tablePanel.getTable().collection;
        tablePanel.setTimelinePanel(timelinePanel);

        panelSouth = new JPanel(new BorderLayout());
        panelSouth.add(tablePanel, BorderLayout.CENTER);
        splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, timelinePanel, panelSouth);
        this.setBounds(200, 200, (int) StartParameters.getTimelinepanelWidth(), (int) StartParameters.getTimelinepanelHeight());
        this.add(splitpane, BorderLayout.CENTER);
        this.setVisible(true);
        splitpane.setDividerLocation((int) StartParameters.getTimelinepanelSplitposition());
        this.revalidate();
//        System.out.println("Timelinepanel.collection="+timelinePanel.getCollection());
//        System.out.println("Editorpanel  .collection=" + tablePanel.getTable().collection);

        // Menubar
        JMenuBar menu = new JMenuBar();
        JMenu menu_File = new JMenu("File");
        JMenuItem item_add = new JMenuItem("Add...");
        menu.add(menu_File);
        menu_File.add(item_add);
        this.setJMenuBar(menu);
        item_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser(CapacityTimelinePanel.directoryPDFsave) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory() || file.getName().endsWith(".tse")) {
                            return true;
                        }
                        return false;
                    }
                };
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setMultiSelectionEnabled(true);
                int n = fc.showOpenDialog(EditorTableFrame.this);
                if (n == JFileChooser.APPROVE_OPTION) {
                    try {
                        CapacityTimelinePanel.directoryPDFsave = fc.getSelectedFile().getAbsolutePath();
                        if (fc.getSelectedFiles() == null || fc.getSelectedFiles().length == 1) {
                            TimeSeries ts = TimeSeries_IO.readTimeSeries(fc.getSelectedFile());
                            tablePanel.getTable().collection.addSeries(ts);
                        } else {
                            List<File> list1 = Arrays.asList(fc.getSelectedFiles());
                            ArrayList<File> list = new ArrayList<>(list1);
                            Iterator<File> it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                if (f.getName().contains("DOUBLE") || f.getName().contains("LINEAR")) {
                                    TimeSeries ts = TimeSeries_IO.readTimeSeries(f);
                                    SeriesKey key = (SeriesKey) ts.getKey();
                                    key.label = "lin. Interpol.";
                                    key.stroke = StrokeEditor.availableStrokes[1];
                                    key.lineColor = Color.black;
                                    tablePanel.getTable().collection.addSeries(ts);
                                    it.remove();
                                }
                            }
                            it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                if (f.getName().contains("INTEGER") || (f.getName().contains("STEPS") && !f.getName().contains("SHIFT"))) {
                                    TimeSeries ts = TimeSeries_IO.readTimeSeries(f);
                                    SeriesKey key = (SeriesKey) ts.getKey();
                                    key.label = "steps_start";
                                    key.stroke = StrokeEditor.availableStrokes[0];
                                    key.lineColor = Color.orange;
                                    tablePanel.getTable().collection.addSeries(ts);
                                    it.remove();
                                }
                            }
                            it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                if (f.getName().contains("SHIFT")) {
                                    TimeSeries ts = TimeSeries_IO.readTimeSeries(f);
                                    SeriesKey key = (SeriesKey) ts.getKey();
                                    key.label = "steps_shift";
                                    key.stroke = StrokeEditor.availableStrokes[5];
                                    key.lineColor = Color.blue;
                                    tablePanel.getTable().collection.addSeries(ts);
                                    it.remove();
                                }
                            }
                            it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                if (f.getName().contains("MEAN")) {
                                    TimeSeries ts = TimeSeries_IO.readTimeSeries(f);
                                    SeriesKey key = (SeriesKey) ts.getKey();
                                    key.label = "mean";
                                    key.stroke = StrokeEditor.availableStrokes[4];
                                    key.lineColor = Color.red;
                                    tablePanel.getTable().collection.addSeries(ts);
                                    it.remove();
                                }
                            }
                            it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                if (f.getName().contains("MAXIMUM")) {
                                    TimeSeries ts = TimeSeries_IO.readTimeSeries(f);
                                    SeriesKey key = (SeriesKey) ts.getKey();
                                    key.label = "maximum";
                                    key.stroke = StrokeEditor.availableStrokes[2];
                                    key.lineColor = Color.red;
                                    tablePanel.getTable().collection.addSeries(ts);
                                    it.remove();
                                }
                            }
                            it = list.iterator();
                            while (it.hasNext()) {
                                File f = it.next();
                                {
                                    tablePanel.getTable().collection.addSeries(TimeSeries_IO.readTimeSeries(f));
                                    it.remove();
                                }
                            }
                        }
                        tablePanel.getTable().updateTableByCollection();
                    } catch (IOException ex) {
                        Logger.getLogger(XYSeriesTable.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        this.setVisible(true);

        TransferHandler th = new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }

                List<File> files;
                try {
                    files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    // should never happen (or JDK is buggy)
                    return false;
                }

                for (File file : files) {
                    try {
                        TimeSeries ts = TimeSeries_IO.readTimeSeries(file);
                        tablePanel.getTable().collection.addSeries(ts);
                    } catch (IOException ex) {
                        Logger.getLogger(EditorTableFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                tablePanel.getTable().updateTableByCollection();
                timelinePanel.updateCheckboxPanel();
                timelinePanel.updateShownTimeSeries();
                return true;
            }

        };

        tablePanel.setTransferHandler(th);
        timelinePanel.setTransferHandler(th);

        ComponentAdapter ca = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                StartParameters.setTimelinepanelHeight(getHeight());
                StartParameters.setTimelinepanelWidth(getWidth());
                StartParameters.setTimelinepanelSplitposition(splitpane.getDividerLocation());
            }
        };
        this.addComponentListener(ca);
        timelinePanel.addComponentListener(ca);

        JMenu menu_minimizer = new JMenu("Outflow");
        menu.add(menu_minimizer);
        final JCheckBoxMenuItem checkMinimizer = new JCheckBoxMenuItem("Minimizer", false);
        menu_minimizer.add(checkMinimizer);
        checkMinimizer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkMinimizer.isSelected()) {
                    if (panelMinimizer == null) {
                        panelMinimizer = new OutflowMinimizerPanel();
                        if (viewController != null) {
                            viewController.getPaintManager().addCapacitySelectionListener(new CapacitySelectionListener() {
                                @Override
                                public void selectCapacity(Capacity c, Object caller) {
                                    panelMinimizer.setCapacity(c);
                                }
                            });
                        }
                    }
                    panelSouth.add(panelMinimizer, BorderLayout.EAST);
                    panelMinimizer.panel = timelinePanel;
                    panelMinimizer.setCapacity(timelinePanel.actualShown);
                } else {
                    panelSouth.remove(panelMinimizer);
                }
                panelSouth.revalidate();
            }
        });
        menu.revalidate();
    }

    public CapacityTimelinePanel getTimelinePanel() {
        return timelinePanel;
    }

    public TimeSeriesEditorTablePanel getTablePanel() {
        return tablePanel;
    }

    public static void main(String[] args) {
        final EditorTableFrame frame = new EditorTableFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.splitpane.setDividerLocation(0.7);

        File testTimelineFile = new File(".\\44431__Masse Messung_8192.tse");
        try {
            if (testTimelineFile.exists()) {
                TimeSeries ts = TimeSeries_IO.readTimeSeries(testTimelineFile);
                frame.tablePanel.getTable().collection.addSeries(ts);
                frame.tablePanel.getTable().updateTableByCollection();
            }
        } catch (IOException ex) {
            Logger.getLogger(XYSeriesTable.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void setStorage(Capacity actualShown, String toString) {
        this.timelinePanel.setStorage(actualShown, toString);
    }

}
