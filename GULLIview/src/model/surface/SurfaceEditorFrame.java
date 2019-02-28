/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.surface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import io.extran.HE_SurfaceIO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import model.GeoTools;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import view.DoubleColorHolder;
import view.PaintManager;
import view.SimpleMapViewerFrame;
import view.shapes.AreaPainting;

/**
 *
 * @author saemann
 */
public class SurfaceEditorFrame extends SimpleMapViewerFrame {

    SurfaceEditor editor;
    PaintManager manager;
    public String filepath = ".";
    public Polygon boundary = null;

    private LinkedList<Coordinate> polygoncoordinates = null;

    public SurfaceEditorFrame() {
        super();
        manager = new PaintManager(null, this);
        manager.maximumNumberOfSurfaceShapes = 1000000;
        initFileMenu();
        initPolygonMenu();
    }

    private void initFileMenu() {
        JMenu menuFile = new JMenu("File");
        JMenuItem itemFileOpenSurface = new JMenuItem("Open Surface...");
        menuFile.add(itemFileOpenSurface);
        getJMenuBar().add(menuFile);

        JMenuItem itemFileOpenBoundary = new JMenuItem("Open Boundary...");
        menuFile.add(itemFileOpenBoundary);

        itemFileOpenSurface.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser(filepath) {

                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".dat")) {
                            return true;
                        }
                        return false;
                    }
                };
                int n = fc.showOpenDialog(fc);
                if (n == JFileChooser.APPROVE_OPTION) {
                    try {
                        Surface surf = HE_SurfaceIO.loadSurface(fc.getSelectedFile());
                        if (surf != null) {
                            filepath = fc.getSelectedFile().getAbsolutePath();
                            System.out.println("Loaded Surface from " + fc.getSelectedFile());
                            manager.setSurfaceTrianglesShownAsNodes(false);
                            manager.setSurface(surf);
                            manager.setSurfaceShow(PaintManager.SURFACESHOW.GRID);
                            System.out.println("Display Surface");
                            getMapViewer().zoomToFitLayer();
                            editor = new SurfaceEditor(surf);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        });

        itemFileOpenBoundary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser(filepath) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".xy")) {
                            return true;
                        }
                        return false;
                    }
                };
                int n = fc.showOpenDialog(fc);
                if (n == JFileChooser.APPROVE_OPTION) {
                    try {
                        boundary = HE_SurfaceIO.loadBoundary(fc.getSelectedFile());
                        if (boundary != null) {
                            filepath = fc.getSelectedFile().getAbsolutePath();
                            GeoTools gt;
                            if (editor != null && editor.getSurface() != null) {
                                gt = editor.getSurface().getGeotools();
                            } else {
                                gt = new GeoTools("EPSG:4326", "EPSG:32632", true);
                            }
                            DoubleColorHolder dch = new DoubleColorHolder(Color.orange, new Color(255, 255, 255, 0), "Boundary.xy");
                            AreaPainting ap = new AreaPainting(0, dch, gt.toGlobal(boundary));
                            getMapViewer().addPaintInfoToLayer("BOUNDARY", ap);
                            getMapViewer().recalculateShapes();
                            getMapViewer().zoomToFitLayer();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (FactoryException ex) {
                        Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (TransformException ex) {
                        Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    private void initPolygonMenu() {
        JMenu menuPolygon = new JMenu("Polygon");
        JMenuItem itemClickPolygon = new JMenuItem("select Polygon...");
        menuPolygon.add(itemClickPolygon);
        getJMenuBar().add(menuPolygon);
        itemClickPolygon.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                PointFrame pf = new PointFrame();
                pf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                pf.setBounds(SurfaceEditorFrame.this.getX() + SurfaceEditorFrame.this.getWidth() - 20, SurfaceEditorFrame.this.getY(), 200, 300);
                pf.setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        SurfaceEditorFrame frame = new SurfaceEditorFrame();
        frame.filepath = "L:\\atestsurfaceOutput";
    }

    public class PointFrame extends JFrame {

        JTextArea tf = new JTextArea();
        GeometryFactory gf = new GeometryFactory();
        DoubleColorHolder ch = new DoubleColorHolder(Color.orange, new Color(100, 100, 0, 30), "Selection Polygon");
        String layerKey = "POLYGON";

        public PointFrame() throws HeadlessException {
            super("Polygon");
            this.setLayout(new BorderLayout());
            this.add(tf, BorderLayout.CENTER);
            if (SurfaceEditorFrame.this.polygoncoordinates == null) {
                SurfaceEditorFrame.this.polygoncoordinates = new LinkedList<>();
            } else {
                SurfaceEditorFrame.this.polygoncoordinates.clear();
            }
            SurfaceEditorFrame.this.mapViewer.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent me) {
                    try {
                        if (!PointFrame.this.isDisplayable()) {
                            SurfaceEditorFrame.this.mapViewer.removeMouseListener(this);
                            SurfaceEditorFrame.this.getMapViewer().clearLayer(layerKey);
                            return;
                        }

                        if (me.getButton() == MouseEvent.BUTTON1) {
//                        System.out.println("Mouseclick:" + me.getPoint());
                            Point2D.Double lonlat = SurfaceEditorFrame.this.mapViewer.getPosition(me.getPoint());
//                        System.out.println("clicked on " + lonlat);
                            if (SurfaceEditorFrame.this.editor == null || SurfaceEditorFrame.this.editor.getSurface() == null) {
                                System.err.println("Select Surface first");
                                tf.setText("Please open a Surface first. \n\nCoordinate system of the surface needs to be known.");
                                return;
                            }
                            Coordinate utm = SurfaceEditorFrame.this.editor.getSurface().getGeotools().toUTM(new Coordinate(lonlat.x, lonlat.y), false);
                            SurfaceEditorFrame.this.polygoncoordinates.add(utm);
                        } else {
                            if (!SurfaceEditorFrame.this.polygoncoordinates.isEmpty()) {
                                SurfaceEditorFrame.this.polygoncoordinates.removeLast();
                            }
                        }
                        updatePoints();
                    } catch (TransformException ex) {
                        Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            });

        }

        public void updatePoints() {
            StringBuilder str = new StringBuilder();
            Coordinate[] coords = new Coordinate[SurfaceEditorFrame.this.polygoncoordinates.size() + 1];
            int i = 0;
            for (Coordinate c : SurfaceEditorFrame.this.polygoncoordinates) {
                str.append(c.x).append(",  ").append(c.y).append('\n');
                Coordinate longlat;
                try {
                    longlat = SurfaceEditorFrame.this.editor.getSurface().getGeotools().toGlobal(c, true);
                    coords[i] = longlat;
                } catch (TransformException ex) {
                    Logger.getLogger(SurfaceEditorFrame.class.getName()).log(Level.SEVERE, null, ex);
                }

                i++;
            }
            coords[coords.length - 1] = coords[0];
            this.tf.setText(str.toString());
            if (coords.length > 3) {
                Polygon polygon = gf.createPolygon(coords);
                AreaPainting ap = new AreaPainting(0, ch, polygon);
                SurfaceEditorFrame.this.getMapViewer().addPaintInfoToLayer(layerKey, ap);
                SurfaceEditorFrame.this.getMapViewer().recalculateShapes();
                SurfaceEditorFrame.this.getMapViewer().repaint();
            }
        }

    }

}
