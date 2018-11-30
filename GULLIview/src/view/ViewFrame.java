package view;

import control.LoadingCoordinator;
import view.timeline.CapacityTimelinePanel;
import control.LocationIDListener;
import io.extran.HE_Database;
import model.topology.graph.ErrorChecker;
import model.topology.graph.ErrorNote;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import model.GeoPosition;
import model.topology.Capacity;
import model.GeoPosition2D;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.graph.GraphSearch;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.gui.jmapviewer.source.MyOSMTileSource;
import view.shapes.ArrowPainting;
import view.shapes.Layer;
import view.shapes.LinePainting;
import view.shapes.NodePainting;
import view.shapes.PaintInfo;

/**
 *
 * @author saemann
 */
public class ViewFrame extends JFrame {

    final SimpleMapViewerFrame smvf;
    CapacityTimelinePanel timelinePanel;
    Network network;

    BasicStroke bs;

    String filePath = "";

    //Put line-shapes for the pipes into the viewer
    final String layerPipeR = "PR";
    ColorHolder chPipeR = new ColorHolder(new Color(100, 100, 255), "Haltung RegenW");

    final String layerPipeS = "PS";
    ColorHolder chPipeS = new ColorHolder(new Color(255, 100, 100), "Haltung SchmutzW");

    final String layerPipeM = "PM";
    ColorHolder chPipeM = new ColorHolder(new Color(155, 155, 100), "Haltung MischW");

    final String layerManHoleR = "MHR";
    ColorHolder chManholeR = new ColorHolder(Color.lightGray, "Schacht");

    final String layerManHoleS = "MHS";
    ColorHolder chManholeS = new ColorHolder(Color.DARK_GRAY, "Schacht");

    final String layerError = "Err";
    ColorHolder chErr = new ColorHolder(Color.orange, "Fehler");
    HashMap<Long, ErrorNote> errornotes;

    final String layerDivider = "Div";
    ColorHolder chDiv = new ColorHolder(Color.red, "Wasserscheide");

    final String layerUpstream = "UP";
    ColorHolder chUp = new ColorHolder(Color.green, "Upstream linked");

    final String layerDownstream = "DOWN";
    ColorHolder chDown = new ColorHolder(Color.blue, "Downstream linked");

    final String layerTurnOff = "Off";
    ColorHolder chOff = new ColorHolder(Color.magenta, "Turn off");

    final String layerTurnIn = "In";
    ColorHolder chIn = new ColorHolder(Color.orange, "Add in");

    final String layerSwitchedDirection = "SW";
    ColorHolder chSwitch = new ColorHolder(Color.RED, "Redirected");

    Pipe selectedPipe;
    private DecimalFormat format2 = new DecimalFormat("0.00");

    public ViewFrame() throws HeadlessException {
        smvf = new SimpleMapViewerFrame();
        smvf.getMapViewer().setBaseLayer(MyOSMTileSource.BaseLayer.CARTO_LIGHT.getSource());
        bs = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        LocationIDListener listener;
        listener = new LocationIDListener() {

            @Override
            public void selectLocationID(Object o, String layer, long l) {
                if (o == this) {
                    return;
                }
                if (network == null) {
                    return;
                }
                smvf.mapViewer.clearLayer(smvf.mapViewer.LAYER_KEY_LABEL);
                if (layer.startsWith("P")) {
                    for (Pipe pipe : network.getPipes()) {
                        if (pipe.getAutoID() == l) {
                            Point2D.Double cp = smvf.getMapViewer().clickPoint;
                            GeoPosition2D pos = new GeoPosition(cp);
                            smvf.getMapViewer().addLabelPainting(smvf.mapViewer.LAYER_KEY_LABEL, 0, smvf.mapViewer.COLORHOLDER_LABEL, pos, new String[]{pipe.toString(), pipe.getWaterType() + "", "up    : " + format2.format(pipe.getFlowInletConnection().getHeight()) + "m üNN", "down: " + format2.format(pipe.getFlowOutletConnection().getHeight()) + "m üNN", "Length: " + format2.format(pipe.getLength()) + "m", "Slope : " + format2.format(pipe.getDecline() * 100.) + "%", pipe.getProfile().toString()});
                            selectedPipe = pipe;
                            return;
                        }
                    }
                } else if (layer.equals(layerPipeS) || layer.equals(layerPipeM)) {

                } else if (layer.equals(layerManHoleR)) {
                    for (Manhole mh : network.getManholes()) {
                        if (mh.getAutoID() == l) {
                            smvf.getMapViewer().addLabelPainting(smvf.mapViewer.LAYER_KEY_LABEL, 0, smvf.mapViewer.COLORHOLDER_LABEL, ((Manhole) mh).getPosition(), new String[]{"Manhole", mh.toString(), "Connections: " + mh.getConnections().length, "Incomings: " + mh.getNumberIncomings(), "Outgoings: " + mh.getNumberOutgoings()});
//                            showTimeline(mh);
                            return;
                        }
                    }
                } else if (layer.equals(layerManHoleS)) {

                } else if (layer.equals(layerError)) {
                    if (errornotes.containsKey(l)) {
                        ErrorNote e = errornotes.get(l);
                        smvf.getMapViewer().addLabelPainting(smvf.mapViewer.LAYER_KEY_LABEL, 0, smvf.mapViewer.COLORHOLDER_LABEL, e.getPosition(), e.getInformation());
                        return;
                    }
                } else {
//                    showTimeline(null);
                }
            }
        };
        smvf.getMapViewer().addListener(listener);

        final JPopupMenu popup;
        popup = new JPopupMenu("Flowsearch");
        final JMenuItem search = new JMenuItem("PipeID");
        popup.add(search);
        smvf.getMapViewer().add(popup);
        smvf.getMapViewer().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getButton() == MouseEvent.BUTTON3) {
                    if (selectedPipe != null) {
                        search.setText(selectedPipe.toString());
                    } else {
                        search.setText("clear catchment");
                    }
                    popup.setLocation(me.getLocationOnScreen());
                    popup.setVisible(true);
//                    }
                } else {
                    popup.setVisible(false);
                }
            }
        });

        search.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                popup.setVisible(false);
                searchFlowCatchment(selectedPipe);
                smvf.getMapViewer().repaint();
            }
        });

        chUp.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        chOff.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        chSwitch.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        JMenuBar bar = smvf.getJMenuBar();
        JMenu menuFile = new JMenu("File");
        bar.add(menuFile, 0);
        JMenuItem itemOpen = new JMenuItem("Open...");
        menuFile.add(itemOpen);
        itemOpen.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser(filePath);
                fc.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().toLowerCase().endsWith(".inp")) {
                            return true;
                        }
                        if (file.getName().toLowerCase().endsWith(".idbf")) {
                            return true;
                        }
                        if (file.getName().toLowerCase().endsWith(".txt")) {
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return ".inp/*.idbf/*.txt";
                    }
                });
                int n = fc.showOpenDialog(ViewFrame.this);
                if (n == fc.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (f == null) {
                        return;
                    }
                    filePath = f.getAbsolutePath();
                    try {
                        Network nw = LoadingCoordinator.readNetwork(f);
                        setNetwork(nw);
                    } catch (FactoryException ex) {
                        Logger.getLogger(ViewFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(ViewFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    public void setNetwork(Network network) {
        this.network = network;
        for (Capacity capacity : network.getManholes()) {
            NodePainting np = new NodePainting(capacity.getAutoID(), ((Manhole) capacity).getPosition(), chManholeR);
            np.setRadius(3);
            smvf.getMapViewer().addPaintInfoToLayer(layerManHoleR, np);
        }

        for (Pipe pipe : network.getPipes()) {

            ArrayList<GeoPosition2D> list = new ArrayList<>(2);
            list.add(pipe.getFlowInletConnection().getPosition());
            list.add(pipe.getFlowOutletConnection().getPosition());

            if (pipe.getWaterType() == Capacity.SEWER_TYPE.DRAIN) {
                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipeR);
                smvf.getMapViewer().addPaintInfoToLayer(layerPipeR, ap);
            } else if (pipe.getWaterType() == Capacity.SEWER_TYPE.MIX) {
                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipeM);
                smvf.getMapViewer().addPaintInfoToLayer(layerPipeM, ap);
            } else if (pipe.getWaterType() == Capacity.SEWER_TYPE.SEWER) {
                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipeS);
                smvf.getMapViewer().addPaintInfoToLayer(layerPipeS, ap);
            } else {
                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipeM);
                smvf.getMapViewer().addPaintInfoToLayer(layerPipeM, ap);
            }

        }

        HashSet<ErrorNote> en = ErrorChecker.checkForErrors(network);
        errornotes = new HashMap<>(en.size());
        //Put Node-shapes for every found error into the view

        long id = Capacity.getMaximumID();
        for (ErrorNote e : en) {
            id++;
            NodePainting np = new NodePainting(id, e.getPosition(), chErr);
            smvf.getMapViewer().addPaintInfoToLayer(layerError, np);//.addNodeColored(layerError, id, e.getPosition(), chErr, bs, null);
            errornotes.put(id, e);
        }

        //Find water dividing points 
        for (Manhole capacity
                : network.getManholes()) {
            boolean found = false;
            if (capacity.getConnections().length < 2) {
                continue;
            }
            for (Connection_Manhole_Pipe connection : capacity.getConnections()) {
                if (connection.isEndOfPipe()) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            NodePainting np = new NodePainting(id++, capacity.getPosition(), chDiv);
            smvf.getMapViewer().addPaintInfoToLayer(layerDivider, np);
//            smvf.getMapViewer().addNodeColored(layerDivider, id++, capacity.getPosition(), chDiv, bs, null);
        }

        applySearchForID(smvf.menuBar, smvf.getMapViewer());

        smvf.getMapViewer()
                .recomputeLegend();
        smvf.getMapViewer()
                .zoomToFitLayer();
        smvf.repaint();
    }

    public void searchFlowCatchment(Capacity capacity) {
        smvf.getMapViewer().clearLayer(layerUpstream);
        smvf.getMapViewer().clearLayer(layerDownstream);
        smvf.getMapViewer().clearLayer(layerTurnOff);
        smvf.getMapViewer().clearLayer(layerTurnIn);
        System.out.println("search for trees of " + capacity);
        if (capacity == null) {
            return;
        }
        Manhole startUp = null;
        Manhole startDown = null;
        if (capacity instanceof Manhole) {
            startUp = (Manhole) capacity;
            startDown = (Manhole) capacity;
        } else if (capacity instanceof Pipe) {
            startUp = (Manhole) ((Pipe) capacity).getStartConnection().getManhole();
            startDown = (Manhole) ((Pipe) capacity).getEndConnection().getManhole();
        } else {
            throw new UnsupportedOperationException("Type " + capacity.getClass().getSimpleName() + " from Object " + capacity.toString() + " is not yet implemented for graph search.");
        }
        ArrayList<Pipe> upstreams = GraphSearch.findUpstreamPipes(startUp);
        for (Pipe pup : upstreams) {
            ArrayList<GeoPosition2D> list = new ArrayList<>(2);
            list.add(pup.getStartConnection().getPosition());
            list.add(pup.getEndConnection().getPosition());
            LinePainting lp = new LinePainting(pup.getAutoID(), list, chUp);
            smvf.getMapViewer().addPaintInfoToLayer(layerUpstream, lp);
//            smvf.getMapViewer().addLineStringsColored(layerUpstream, pup.getAutoID(), list, chUp, chUp.getStroke());
        }
        ArrayList<Pipe> downstreams = GraphSearch.findDownstreamPipes(startDown);
        for (Pipe pup : downstreams) {
            ArrayList<GeoPosition2D> list = new ArrayList<>(2);
            list.add(pup.getStartConnection().getPosition());
            list.add(pup.getEndConnection().getPosition());
            LinePainting lp = new LinePainting(pup.getAutoID(), list, chDown);
            smvf.getMapViewer().addPaintInfoToLayer(layerDownstream, lp);
//            smvf.getMapViewer().addLineStringsColored(layerDownstream, pup.getAutoID(), list, chDown, chUp.getStroke());
        }
    }

    public void searchCatchments() {
        smvf.getMapViewer().clearLayer(layerUpstream);
        smvf.getMapViewer().clearLayer(layerDownstream);
        smvf.getMapViewer().clearLayer(layerTurnOff);
        smvf.getMapViewer().clearLayer(layerTurnIn);

        ArrayList<Manhole> startManholes = new ArrayList<>();
        try {
            startManholes.add(network.getManholeByName("RI04N909"));
//            startManholes.add(network.getManholeByName("RI15S501"));
            startManholes.add(network.getManholeByName("RI04S547"));
            startManholes.add(network.getManholeByName("DO09N801"));
            startManholes.add(network.getManholeByName("RI12S801"));
        } catch (NullPointerException nullPointerException) {
            nullPointerException.printStackTrace();
        }

        ArrayList<Pipe> turnOffs = new ArrayList<>(30);
        ArrayList<Manhole> alreadySeen = new ArrayList<>(network.getManholes().size() / 3);

        String layerkeyAll = "IHME";
        ColorHolder chAll = new ColorHolder(new Color(155, 155, 00), "Einleitung Gemischt ");
        /**
         * Begin search upstream
         */
        int number = 0;
        boolean calculateMixed = false;
        for (Manhole startManhole : startManholes) {
            do {
                number++;
                String layerkey = number + "";
                ColorHolder ch = null;
                if (number == 1) {
                    ch = new ColorHolder(new Color(0, 250, 0), "Einleitung 1 Distelkamp");
                } else if (number == 2) {
                    ch = new ColorHolder(new Color(250, 150, 050), "Einleitung 2 Düsternstraße");
                } else if (number == 3) {
                    ch = new ColorHolder(new Color(0, 0, 250), "Einleitung 3 Schwimmbad");
                } else if (number == 4) {
                    ch = new ColorHolder(new Color(250, 0, 0), "Einleitung 4 Schnellweg");
                }
                ArrayList<Manhole> manholes = new ArrayList<>();
                manholes.add(startManhole);
                while (!manholes.isEmpty()) {
                    Manhole mh = manholes.remove(manholes.size() - 1);
                    alreadySeen.add(mh);
                }
                if (calculateMixed) {
                    break;
                }
                if (startManhole == startManholes.get(startManholes.size() - 1)) {
                    calculateMixed = true;

                }

            } while (calculateMixed);
        }

        smvf.getMapViewer()
                .repaint();
    }

    public static void main(String[] args) throws FactoryException, Exception {
        ViewFrame frame = new ViewFrame();
        try {
            File f = new File(".\\Model.idbf");
            if (f.exists()) {
                CoordinateReferenceSystem crsDB = CRS.decode("EPSG:31466",true);
                HE_Database fbdb=new HE_Database(f, true);
                final Network network =fbdb.loadNetwork(crsDB);
//                 LoadingCoordinator.readNetwork(f);//CSV_IO.loadNetwork(file_rohr, file_schacht, null);
                frame.setNetwork(network);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void applySearchForID(JMenuBar menubar, final MapViewer viewer) {
        JMenu menuSearch = null;
//        System.out.println("ViewFrame::serach for search menu ");
        for (MenuElement subElement : menubar.getSubElements()) {
            if (subElement.toString().toLowerCase().contains("search")) {
                try {
                    menuSearch = (JMenu) subElement;
                } catch (Exception e) {
                }
                break;
            }
        }
        if (menuSearch == null) {
            menuSearch = new JMenu("Search");
            menubar.add(menuSearch);
//            System.out.println("added search to menubar");
        }
        final JMenuItem itemShapeID = new JMenuItem("ShapeID");
        itemShapeID.setToolTipText("ShapeID often represents the object's id.");
        menuSearch.add(itemShapeID);
        itemShapeID.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                final JFrame frame = new JFrame("Shape ID Search");
                final JTextField textID = new JTextField();
                textID.setToolTipText("ID of shape");
                frame.setLayout(new BorderLayout());
                frame.add(textID, BorderLayout.NORTH);
                JButton buttonSearch = new JButton("Search for ID");
                frame.add(buttonSearch, BorderLayout.SOUTH);
                Point p = viewer.getLocationOnScreen();
                frame.setBounds(p.x, p.y, 200, 100);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setVisible(true);//Create Table
                final DefaultTableModel dtm = new DefaultTableModel();
                dtm.setColumnCount(2);
                dtm.setColumnIdentifiers(new String[]{"Layer", "Object"});
                final JTable table = new JTable(dtm);

                frame.add(new JScrollPane(table), BorderLayout.CENTER);
                frame.revalidate();
                table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent lse) {
                        int row = table.getSelectedRow();
                        PaintInfo pi = (PaintInfo) dtm.getValueAt(row, 1);
                        if (pi == null) {
                            return;
                        }
                        viewer.setSelectedObject(pi);
                        viewer.setDisplayPositionByLatLon(pi.getRefLatitude(), pi.getRefLongitude(), viewer.getZoom());
                    }
                });
                buttonSearch.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        long id;
                        try {
                            id = Long.parseLong(textID.getText());
                        } catch (NumberFormatException numberFormatException) {
                            textID.setText("0");
                            return;
                        }
                        //Search in every layer
                        ArrayList<String> containingLayers = new ArrayList<>(viewer.getLayers().size());
                        ArrayList<PaintInfo> shapes = new ArrayList<>(viewer.getLayers().size());

                        for (Layer layer : viewer.getLayers()) {
                            for (PaintInfo element : layer.getElements()) {
                                if (element != null && element.getId() == id) {
                                    containingLayers.add(layer.getKey());
                                    shapes.add(element);
                                    break;
                                }
                            }
                        }

                        dtm.setRowCount(containingLayers.size() + 1);
                        try {
                            if (!containingLayers.isEmpty()) {
                                for (int i = 0; i < dtm.getRowCount(); i++) {
                                    dtm.setValueAt(containingLayers.get(i), i, 0);
                                    dtm.setValueAt(shapes.get(i), i, 1);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dtm.setValueAt("", containingLayers.size(), 0);
                        dtm.setValueAt(null, containingLayers.size(), 1);

                        table.revalidate();
                        frame.pack();
                        frame.revalidate();
                    }
                });

            }
        });

    }
}
