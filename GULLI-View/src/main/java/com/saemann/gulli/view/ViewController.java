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
import com.saemann.gulli.view.timeline.EditorTableFrame;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.SimpleMapViewerFrame;
import java.awt.Frame;

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
//            @Override
//            public void s(Object caller) {
//                System.out.println("Network name is "+control.getNetwork().getName());
//                mapFrame.setTitle(control.getNetwork().getName()+" - GULLI");
//            }
//            
//        });
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

}
