/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import control.Action.Action;
import control.Controller;
import control.StartParameters;
import control.listener.LoadingActionListener;
import control.scenario.Scenario;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;
import model.surface.Surface;
import model.topology.Network;
import view.timeline.CapacityTimelinePanel;
import view.timeline.EditorTableFrame;

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
        timeLineFrame = new EditorTableFrame("No pipe or manhole selected yet", control,this);
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
                StartParameters.setMapFrameBounds(mapFrame.getX(), mapFrame.getY(), mapFrame.getWidth(), mapFrame.getHeight());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                StartParameters.setMapFrameBounds(mapFrame.getX(), mapFrame.getY(), mapFrame.getWidth(), mapFrame.getHeight());
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
