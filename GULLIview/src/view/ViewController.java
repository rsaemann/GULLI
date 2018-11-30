/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import control.Controller;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import view.timeline.TimelinePanel;

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
    protected final JFrame timeLineFrame;

    //Frame content
    protected final TimelinePanel timelinePanel;
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
        timeLineFrame = new JFrame("Timelines");
        timeLineFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        timelinePanel = new TimelinePanel("Select Pipe or Manhole", false);
        timeLineFrame.setLayout(new BorderLayout());
        timeLineFrame.add(timelinePanel, BorderLayout.CENTER);
        timeLineFrame.setVisible(true);
        
        //Order Frames
        controlFrame.setBounds(50, 50, 250, 900);
        mapFrame.setBounds(320,50,1200,900);
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
