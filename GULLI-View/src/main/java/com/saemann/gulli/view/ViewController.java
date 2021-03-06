/*
 * The MIT License
 *
 * Copyright 2018 Robert Sämann.
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
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.scenario.Scenario;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.view.timeline.CapacityTimelinePanel;
import static com.saemann.gulli.view.timeline.CapacityTimelinePanel.directoryPDFsave;
import com.saemann.gulli.view.timeline.EditorTableFrame;
import com.saemann.rgis.tileloader.source.MyOSMTileSource;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.SimpleMapViewerFrame;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jtikz.TikzGraphics2D;
import org.jtikz.TikzPDFGraphics2D;

/**
 * Controls the interfaces between GUI/view and core-Controller
 *
 * @author saemann
 */
public class ViewController {

    protected final Controller control;
    protected final PaintManager paintManager;

    //Frames
    protected final SimpleMapViewerFrame mapFrame;
    protected final ControllFrame controlFrame;
    public final EditorTableFrame timeLineFrame;

    //Frame content
    public final CapacityTimelinePanel timelinePanel;
    protected final MapViewer mapViewer;

    public ViewController(Controller c) {
        this.control = c;
        mapFrame = new SimpleMapViewerFrame();
        mapViewer = mapFrame.getMapViewer();
        paintManager = new PaintManager(c, mapFrame);

        //Control frame
        controlFrame = new ControllFrame(control, paintManager);
        controlFrame.setVisible(true);

        //TableFrame
        timeLineFrame = new EditorTableFrame("No pipe or manhole selected yet", control, this);
        timeLineFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        timelinePanel = timeLineFrame.getTimelinePanel();//new CapacityTimelinePanel("Nothing selected yet", c);// new TimelinePanel("Select Pipe or Manhole", false);

        paintManager.addCapacitySelectionListener(timelinePanel);

        hideOverlayMenu();
        improveMapFrame();

        //Order Frames
        controlFrame.setBounds(StartParameters.getControlFrameBounds());
        mapFrame.setBounds(StartParameters.getMapFrameBounds());
        timeLineFrame.setBounds(StartParameters.getPlotFrameBounds());
        mapFrame.requestFocus();

        mapFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (mapFrame.getExtendedState() == Frame.NORMAL) {
                    StartParameters.setMapFrameBounds(mapFrame.getX(), mapFrame.getY(), mapFrame.getWidth(), mapFrame.getHeight());
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (mapFrame.getExtendedState() == Frame.NORMAL) {
                    StartParameters.setMapFrameBounds(mapFrame.getX(), mapFrame.getY(), mapFrame.getWidth(), mapFrame.getHeight());
                }
                if (mapFrame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                    StartParameters.setMapFrameFullscreen(true);
                } else {
                    StartParameters.setMapFrameFullscreen(false);
                }
            }
        });

        controlFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                StartParameters.setControlFrameBounds(controlFrame.getX(), controlFrame.getY(), controlFrame.getWidth(), controlFrame.getHeight());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                StartParameters.setControlFrameBounds(controlFrame.getX(), controlFrame.getY(), controlFrame.getWidth(), controlFrame.getHeight());
            }
        });

        timeLineFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                StartParameters.setPlotFrameBounds(timeLineFrame.getX(), timeLineFrame.getY(), timeLineFrame.getWidth(), timeLineFrame.getHeight());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                StartParameters.setPlotFrameBounds(timeLineFrame.getX(), timeLineFrame.getY(), timeLineFrame.getWidth(), timeLineFrame.getHeight());
            }
        });

        control.getLoadingCoordinator().addActioListener(new LoadingActionListener() {
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
                if (scenario != null && scenario.getName() != null) {
                    mapFrame.setTitle(scenario.getName() + " - GULLI");
                }
            }

        });
    }

    public Controller getControl() {
        return control;
    }

    public PaintManager getPaintManager() {
        return paintManager;
    }

    public SimpleMapViewerFrame getMapFrame() {
        return mapFrame;
    }

    public ControllFrame getControlFrame() {
        return controlFrame;
    }

    public JFrame getTimeLineFrame() {
        return timeLineFrame;
    }

    public MapViewer getMapViewer() {
        return mapViewer;
    }

    public void improveMapFrame() {
        //Find correct menu to add more Tileservers
        JMenu tilesMenu = null;
        JMenu printMenu = null;
        for (Component component : mapFrame.getJMenuBar().getComponents()) {
            if (component instanceof JMenu) {
                JMenu menu = (JMenu) component;
                if (menu.getText().equals("Background")) {
                    tilesMenu = menu;
                }
                if (menu.getText().equals("Snapshot")) {
                    printMenu = menu;
                }
            }
        }

        if (tilesMenu != null) {
            tilesMenu.add(new JSeparator());

            JMenuItem itemTonerNoLabel = new JMenuItem("Toner No Label");
            tilesMenu.add(itemTonerNoLabel);
            itemTonerNoLabel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    getMapViewer().setBaseLayer(new MyOSMTileSource("Toner No Label", "http://a.tile.stamen.com/toner-background/", 18) {
                    });
                }
            });

            JMenuItem itemThunderforest = new JMenuItem("Thunderforest");
            tilesMenu.add(itemThunderforest);
            itemThunderforest.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    getMapViewer().setBaseLayer(new MyOSMTileSource("Thunderforest", " 	http://tile.thunderforest.com/landscape/", 18) {
                    });
                }
            });

            tilesMenu.revalidate();
        }

        if (printMenu != null) {
            int indexSeperator = 3;
            for (int i = 0; i < printMenu.getComponents().length; i++) {
                if (printMenu.getComponent(i) instanceof JSeparator) {
                    indexSeperator = i;
                    break;
                }
            }
            JMenuItem itemPrintTikz = new JMenuItem("LaTeX/Tikz...");
            printMenu.add(itemPrintTikz, indexSeperator);
            itemPrintTikz.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JFileChooser fc = new JFileChooser(directoryPDFsave);
                    fc.setFileFilter(new FileNameExtensionFilter("LaTeX File", new String[]{"tex", "tikz"}));
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int n = fc.showSaveDialog(mapFrame);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        if (output.exists()) {
                            if (JOptionPane.showConfirmDialog(mapFrame, "Override existing file?", output.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                                return;
                            }
                        }

                        directoryPDFsave = output.getParent();
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".tex")) {
                            output = new File(output.getAbsolutePath() + ".tex");
                        }
                        try {
                            Rectangle rec = new Rectangle(0, 0, mapFrame.getWidth(), mapFrame.getHeight());
                            try (FileOutputStream fos = new FileOutputStream(output)) {
                                TikzGraphics2D g2d = new TikzGraphics2D(fos, rec);
                                mapFrame.getMapViewer().paintMapView(g2d);
                                g2d.finalize();
                                System.out.println("Created file " + output);
                            }
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            );

            JMenuItem itemPrintPDFTikz = new JMenuItem("Tikz&PDF...");
            printMenu.add(itemPrintPDFTikz, indexSeperator);
            itemPrintPDFTikz.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    JFileChooser fc = new JFileChooser(directoryPDFsave);

                    fc.setFileFilter(new FileNameExtensionFilter("LaTeX File", new String[]{"tex", "tikz"}));
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int n = fc.showSaveDialog(mapFrame);
                    if (n == JFileChooser.APPROVE_OPTION) {
                        File output = fc.getSelectedFile();
                        if (output.exists()) {
                            if (JOptionPane.showConfirmDialog(mapFrame, "Override existing file?", output.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                                return;
                            }
                        }

                        directoryPDFsave = output.getParent();
                        StartParameters.setPictureExportPath(directoryPDFsave);
                        if (!output.getName().endsWith(".tex")) {
                            output = new File(output.getAbsolutePath() + ".tex");
                        }
                        boolean tempzoomcontrolvisible = mapViewer.getZoomContolsVisible();
                        mapViewer.setZoomContolsVisible(mapViewer.isShowZoomslidersinExportFile());
                        boolean tempCopyright = mapViewer.showCopyright;
                        mapViewer.showCopyright = mapViewer.isShowCopyrightsinExportFile();
//        int tempLegendWidth = mapViewer.;
                        boolean tempShowLegend = mapViewer.showLegend;
                        mapViewer.showLegend = mapViewer.isShowLegendInExportFile();
                        boolean tempMapscale = mapViewer.showMapScale;
                        mapViewer.showMapScale = mapViewer.showMapscalesinExportFile;
                        try {

//                            mapViewer.legendWidth += 4; // Vergroeßern, da pdf schrift etwas breiter ist.
                            //Alle Shapes neu berechnen, da einfaches translatieren oft nicht ausreichend schön aussieht
                            Rectangle rec = new Rectangle(0, 0, mapViewer.getWidth(), mapViewer.getHeight());

                            TikzPDFGraphics2D g2d = new TikzPDFGraphics2D(output.getParentFile(), output.getName(), rec);
                            g2d.prefereCenterAnchor=false;
                            mapViewer.recalculateShapes(g2d);
                            mapViewer.recomputeLegend(g2d);
                            mapViewer.paintMapView(g2d);
                            g2d.finalize();
                            System.out.println("Created file " + output);

                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(CapacityTimelinePanel.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            mapViewer.showCopyright = tempCopyright;
                            mapViewer.showLegend = tempShowLegend;
                            mapViewer.setZoomContolsVisible(tempzoomcontrolvisible);
                            mapViewer.showMapScale = tempMapscale;
                            mapViewer.recomputeLegend();
                        }
                    }
                }
            }
            );
        }

        JMenuItem itemFrameReset = new JMenuItem("Reset Frames");
        itemFrameReset.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getTimeLineFrame().setBounds(getMapFrame().getX(), getMapFrame().getY(), 400, 400);
            }
        });
        mapFrame.getMenu_View().add(itemFrameReset);

        //Search menu
        JMenu menuSearch = new JMenu("Search");
        JMenuItem itemSearch = new JMenuItem("Object Name...");
        menuSearch.add(itemSearch);
        itemSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame frame = new JFrame("Search");
                frame.add(new SearchPanel(control, paintManager));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setBounds(itemSearch.getX(), itemSearch.getY(), 200, 150);
                frame.setVisible(true);
            }
        });
        mapFrame.getJMenuBar().add(menuSearch);

        JMenu menuHelp = new JMenu("About");

        mapFrame.getJMenuBar().add(menuHelp);
        JMenuItem itemGULLI = new JMenuItem("GULLI urban pollution transport");
        JMenuItem itemCopyright = new JMenuItem("Robert Sämann 2015-2020");
        JMenuItem itemGithub = new JMenuItem("Github link");
        menuHelp.add(itemGULLI);
        menuHelp.add(itemCopyright);
        menuHelp.add(itemGithub);
        ActionListener action = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JEditorPane ep = new JEditorPane("text/html", "<html><body>Robert Sämann 2015-2020<br>Visit the project homepage on Github:<br><a href=\"https://github.com/rsaemann/GULLI\">https://github.com/rsaemann/GULLI</a>  </body></html>");
                ep.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch (URISyntaxException ex) {
                                Logger.getLogger(ViewController.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(ViewController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                ep.setEditable(false);
                JOptionPane.showMessageDialog(mapFrame, ep);// "<html>Visit the project homepage on Github:<br><a href=\"https://github.com/rsaemann/GULLI\">https://github.com/rsaemann/GULLI</a>  </html>", "About",JOptionPane.PLAIN_MESSAGE);
            }
        };
        itemGULLI.addActionListener(action);
        itemCopyright.addActionListener(action);
        itemGithub.addActionListener(action);

    }

    public void hideOverlayMenu() {
        JMenuBar bar = mapFrame.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu.getText().equals("Overlay")) {
                menu.setVisible(false);
                break;
            }
        }
    }

}
