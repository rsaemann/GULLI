package com.saemann.gulli.view;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.Setup;
import com.saemann.gulli.core.io.Setup_IO;
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
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author saemann
 */
public class ControllFrame extends JFrame implements ActionListener, LoadingActionListener, SimulationActionListener {

    private JCheckBox checkAlwaysOnTop;
    private JButton buttonHighlightVisualization;
    private Controller controller;

//    private JTabbedPane tabs;
    private SingleControllPanel singleControl;
//    private MultiControllPanel multiControl;
    private JPanel panelBottomButtons;

    private JMenuBar menubar;
    private JMenu menuProject;
    private JMenuItem menuitem_newProject;
    private JMenuItem menuitem_openProject;
    private JMenuItem menuitem_saveProject;
    private JMenuItem menuitem_saveasProject;

    public ControllFrame(Controller controller, PaintManager pm) throws HeadlessException {
        super("Control");
        this.controller = controller;
        this.controller.addActioListener(this);
        this.controller.addSimulationListener(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        this.setUndecorated(true);
        this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        // Tab panel
//        tabs = new JTabbedPane();
//        this.add(tabs, BorderLayout.CENTER);
//        tabs.setPreferredSize(new Dimension(180, tabs.getPreferredSize().height));
        // tab single control
        singleControl = new SingleControllPanel(controller.getThreadController(), controller, this, pm);
        singleControl.setPreferredSize(new Dimension(170, singleControl.getPreferredSize().height));
        JScrollPane scrollsingle = new JScrollPane(singleControl);

//        tabs.add("Single Event", scrollsingle);
        this.add(scrollsingle, BorderLayout.CENTER);

        //tab multi controll
//        multiControl = new MultiControllPanel(controller);
//        JScrollPane scrollMulti = new JScrollPane(multiControl);
//        tabs.add("Multiple Events", scrollMulti);
        /// Bottom Buttons
        panelBottomButtons = new JPanel(new GridLayout(1, 1));
        checkAlwaysOnTop = new JCheckBox("Always on top", this.isAlwaysOnTop());
        buttonHighlightVisualization = new JButton("Go to Visualization");
        panelBottomButtons.add(checkAlwaysOnTop);
//        panelBottomButtons.add(buttonHighlightVisualization);
        this.add(panelBottomButtons, BorderLayout.SOUTH);
        checkAlwaysOnTop.addActionListener(this);
        buttonHighlightVisualization.addActionListener(this);

        initMenuBar();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(checkAlwaysOnTop)) {
            this.setAlwaysOnTop(checkAlwaysOnTop.isSelected());
            return;
        }
//        if (ae.getSource().equals(buttonHighlightVisualization)) {
//           controller.getMapFrame().requestFocus();
//            return;
//        }
    }

    public SingleControllPanel getSingleControl() {
        return singleControl;
    }

    public MultiControllPanel getMultiControl() {
        return null;//multiControl;
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
    public void loadScenario(Scenario scenario, Object caller) {
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

    private void initMenuBar() {
        menubar = new JMenuBar();
        menuProject = new JMenu("Project");
        menubar.add(menuProject);

        menuitem_newProject = new JMenuItem("New");
        menuitem_openProject = new JMenuItem("Open...");
        menuitem_saveProject = new JMenuItem("Save");
        menuitem_saveasProject = new JMenuItem("Save as...");

        menuProject.add(menuitem_newProject);
        menuProject.add(menuitem_openProject);
        menuProject.add(new JSeparator());
        menuProject.add(menuitem_saveProject);
        menuProject.add(menuitem_saveasProject);

        //New operation clears the current project. not yet implemented
//        menuitem_newProject.setEnabled(false);
        menuitem_newProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.cleanSetup();
            }
        });

        menuitem_openProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSetup();
            }
        });

        menuitem_saveProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSetup();
            }
        });

        menuitem_saveasProject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAsSetup();
            }
        });

        this.setJMenuBar(menubar);
    }

    public void saveSetup() {
        String path = controller.getLoadingCoordinator().getCurrentSetupFilepath();
        if(path==null||path.isEmpty()){
            saveAsSetup();
            return;
        }
        File f = new File(path);
        if (f.exists() && f.canWrite()) {
            try {
                controller.getLoadingCoordinator().saveSetup(f);
                return;
            } catch (Exception ex) {
                Logger.getLogger(ControllFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //If something went wrong... let the user decide which file to use as an alternative
        saveAsSetup();
    }

    public void saveAsSetup() {
//        buttonSetupSave.setForeground(Color.darkGray);
        String folder = "";
        if (controller.getLoadingCoordinator().getFileNetwork() != null) {
            folder = controller.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
        }
        JFileChooser fc = new JFileChooser(folder);
        fc.setFileFilter(new FileNameExtensionFilter("Project file (*.xml)", "xml"));
        int n = fc.showSaveDialog(ControllFrame.this);
        if (n == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".xml")) {
                f = new File(f.getAbsolutePath() + ".xml");
            }
            if (f.exists()) {
                n = JOptionPane.showConfirmDialog(fc, "Override existing file?", f.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (n != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            try {
                if (controller.getLoadingCoordinator().saveSetup(f)) {
//                    buttonSetupSave.setForeground(Color.green.darker());
                    StartParameters.setStartFilePath(f.getAbsolutePath());
//                    labelSetupName.setText(f.getName());
//                    labelSetupName.setToolTipText(f.getAbsolutePath());
                } else {
//                    buttonSetupSave.setForeground(Color.red.darker());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
//                buttonSetupSave.setForeground(Color.red.darker());
            }
        }
    }

    private void loadSetup() {
//        buttonSetupLoad.setForeground(Color.darkGray);
        String folder = "";
        if (controller.getLoadingCoordinator().getFileNetwork() != null) {
            folder = controller.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
        }
        JFileChooser fc = new JFileChooser(folder);
        fc.setFileFilter(new FileNameExtensionFilter("Project file (*.xml)", "xml"));
        int n = fc.showOpenDialog(ControllFrame.this);
        if (n == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".xml")) {
                f = new File(f.getAbsolutePath() + ".xml");
            }
            try {
                Setup setup = Setup_IO.load(f);
                if (setup != null) {
                    controller.getLoadingCoordinator().applySetup(setup);
//                    buttonSetupLoad.setForeground(Color.green.darker());
                    StartParameters.setStartFilePath(f.getAbsolutePath());
//                    labelSetupName.setText(f.getName());
//                    labelSetupName.setToolTipText(f.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
//                buttonSetupLoad.setForeground(Color.red.darker());
            }
        }
    }

}
