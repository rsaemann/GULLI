/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import control.Controller;
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
        controlFrame.setBounds(50, 50, 250, 900);
        mapFrame.setBounds(320, 50, 1200, 900);
        timeLineFrame.setBounds(1200, 200, 700, 400);
        mapFrame.requestFocus();
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
