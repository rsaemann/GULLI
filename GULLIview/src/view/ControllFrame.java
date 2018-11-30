package view;

import control.Action.Action;
import control.Controller;
import control.listener.LoadingActionListener;
import control.listener.SimulationActionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import model.surface.Surface;
import model.topology.Network;

/**
 *
 * @author saemann
 */
public class ControllFrame extends JFrame implements ActionListener, LoadingActionListener, SimulationActionListener {

    private JCheckBox checkAlwaysOnTop;
    private JButton buttonHighlightVisualization;
    private Controller controller;

    private JTabbedPane tabs;
    private SingleControllPanel singleControl;
    private MultiControllPanel multiControl;
    private JPanel panelBottomButtons;

    public ControllFrame(Controller controller, PaintManager pm) throws HeadlessException {
        super("Control");
        this.controller = controller;
        this.controller.addActioListener(this);
        this.controller.addSimulationListener(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        this.setUndecorated(true);
        this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        // Tab panel
        tabs = new JTabbedPane();
        this.add(tabs, BorderLayout.CENTER);
//        tabs.setPreferredSize(new Dimension(180, tabs.getPreferredSize().height));
        // tab single control
        singleControl = new SingleControllPanel(controller.getThreadController(), controller, this, pm);
        singleControl.setPreferredSize(new Dimension(170, singleControl.getPreferredSize().height));
        JScrollPane scrollsingle = new JScrollPane(singleControl);

        tabs.add("Single Event", scrollsingle);

        //tab multi controll
        multiControl = new MultiControllPanel(controller);
        JScrollPane scrollMulti = new JScrollPane(multiControl);
        tabs.add("Multiple Events", scrollMulti);

        /// Bottom Buttons
        panelBottomButtons = new JPanel(new GridLayout(2, 1));
        checkAlwaysOnTop = new JCheckBox("Always on top", this.isAlwaysOnTop());
        buttonHighlightVisualization = new JButton("Go to Visualization");
        panelBottomButtons.add(checkAlwaysOnTop);
        panelBottomButtons.add(buttonHighlightVisualization);
        this.add(panelBottomButtons, BorderLayout.SOUTH);
        checkAlwaysOnTop.addActionListener(this);
        buttonHighlightVisualization.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(checkAlwaysOnTop)) {
            this.setAlwaysOnTop(checkAlwaysOnTop.isSelected());
            return;
        }
        if (ae.getSource().equals(buttonHighlightVisualization)) {
//           controller.getMapFrame().requestFocus();
            return;
        }
    }

    public SingleControllPanel getSingleControl() {
        return singleControl;
    }

    public MultiControllPanel getMultiControl() {
        return multiControl;
    }

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
    public void simulationINIT(Object caller) {
        setTitle("Init");
    }

    @Override
    public void simulationSTART(Object caller) {
        setTitle("Plays");
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
    }

    @Override
    public void simulationPAUSED(Object caller) {
        setTitle("II Pause");
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
        setTitle("\u25b6 Plays");
    }

    @Override
    public void simulationSTOP(Object caller) {
        setTitle("\u25A0 Stop");
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
        setTitle("Fin");
    }

    @Override
    public void simulationRESET(Object caller) {
        setTitle("Reset");
    }

}
