/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import control.Controller;
import control.StartParameters;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;
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
    protected final EditorTableFrame timeLineFrame;

    //Frame content
    protected final CapacityTimelinePanel timelinePanel;
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
        timeLineFrame = new EditorTableFrame("No pipe or manhole selected yet", control);
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
