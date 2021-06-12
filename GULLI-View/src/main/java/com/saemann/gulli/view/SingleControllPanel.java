package com.saemann.gulli.view;

import com.saemann.gulli.view.injection.InjectionOrganisatorPanel;
import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.LoadingCoordinator;
import com.saemann.gulli.core.control.StartParameters;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.particlecontrol.ParticleSurfaceComputing2D;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.Setup;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.io.FileContainer;
import com.saemann.gulli.core.io.Setup_IO;
import com.saemann.gulli.core.io.extran.HE_Database;
import com.saemann.gulli.core.io.extran.Raingauge_Firebird;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;
import org.jfree.data.time.TimeSeries;
import com.saemann.gulli.view.themelayer.PipeThemeLayer;
import com.saemann.gulli.view.timeline.PrecipitationTimelinePanel;
import com.saemann.gulli.view.timeline.SeriesKey;
import com.saemann.gulli.view.timeline.TimeSeriesEditorTablePanel;
import com.saemann.gulli.view.video.GIFVideoCreator;
import com.saemann.rgis.view.MapViewer;
import javax.swing.JSeparator;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This Panel controls one single Simulation and displays information about the
 * simulation time.
 *
 * @author saemann
 */
public class SingleControllPanel extends JPanel implements LoadingActionListener, SimulationActionListener {

    public static boolean advancedOpions = false;

    private MapViewer mapViewer;
    private PaintManager paintManager;
    private final ThreadController controler;
    private final Controller control;
    private JToggleButton buttonRun, buttonStep, buttonReset, buttonPause;
    private JTabbedPane tabs;
    private JPanel panelTabLoading, panelTabSimulation;
    private JTextField textTimeStep;
    private JLabel labelCalculationTime, labelSimulationTime, labelCalculationPerStep, labelCalculationSteps;
    private JPanel panelButtons, panelVideo;//
    private JPanel panelLoading;
    private JPanel panelLoadingStatus, panelLoadingStatusStart, panelLoadingStatusStop;
    private JPanel panelTimeSlide;
    protected GregorianCalendar calStart = new GregorianCalendar(StartParameters.formatTimeZone);
    protected GregorianCalendar calEnd = new GregorianCalendar(StartParameters.formatTimeZone);
    protected GregorianCalendar calActual = new GregorianCalendar(StartParameters.formatTimeZone);
    private JLabel labelStarttime, labelEndtime, labelactualTime;
    private JLabel labelParticleActive, labelParticlesTotal;
    private JProgressBar progressSimulation;
    private JProgressBar progressLoading;
    private boolean longerThan1Day = false;
//    private JCheckBox checkVelocityFunction;
    private JTextField textDispersionPipe, textDispersionSurface;
    private JFormattedTextField textSeed;
    private JButton buttonSetupSave, buttonSetupLoad;
    private JLabel labelSetupName;
    private JButton buttonFileNetwork, buttonFilePipeResult;
    private JCheckBox checkSparsePipeLoading, checkLoadFileSpills;
    private JButton buttonStartLoading, buttonStartReloadingAll, buttonCancelLoading;
    private JButton buttonFileSurface, buttonFileWaterdepths;

    private ButtonGroup group_timestep;
    private JRadioButton radioExplicit, radioStepsplicit, radioCrankNicolson;
    private JCheckBox checkParticleDryMovement, checkEnterdry, checkProjectAtObstacles, checkBlockSlow, checkMeanZigzagVelocity;

    private final JCheckBox checkDrawUpdateIntervall;
    private JPanel panelShapes, panelShapesSurface, panelShapePipe;
    private JSlider sliderTimeShape;
    private JLabel labelSliderTime;

//    private final ArrayList<JComboBox<PaintManager.SURFACESHOW>> combosDurfaceShow = new ArrayList<>();
    final JCheckBox checkTrianglesNodes;

    private JLabel labelScenarioInformation;

    private JButton buttonShowRaingauge;
    private JButton buttonLoadAllPipeTimelines;

    private InjectionOrganisatorPanel injectionOrganisationPanel;
    private MeasurementPanel measurementPanel;
    private ImageIcon iconError, iconLoading, iconPending;

    private JLabel labelCurrentAction;

    JRadioButton radioVelocityGDB, radioVelocityWL;
    ButtonGroup groupVelocity;

    protected Action currentAction;

//    protected final String updatethreadBarrier = new String("UPDATETHREADBARRIERSINGLECONTROLPANEL");
    protected long updateThreadUpdateIntervalMS = 1000;
    protected Thread updateGUIThread, updateSimulationThread;
    protected final String lockGUIThread = new String("GUI"), lockSimulationThread = new String("SIM");

    StringBuilder timeelapsed = new StringBuilder(30);

    protected PipeThemeLayer activePipeThemeLayer;
    protected SimpleDateFormat dateFormat;
    protected DecimalFormat dfParticles = new DecimalFormat("#", new DecimalFormatSymbols(StartParameters.formatLocale));
    protected DecimalFormat dfSeconds = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(StartParameters.formatLocale));

    protected JFrame frame;

    public SingleControllPanel(final ThreadController controller, final Controller control, final JFrame frame, PaintManager pm) {
        super();
//        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        BorderLayout layout = new BorderLayout(1, 5);
        this.setLayout(layout);
        this.frame = frame;
        dateFormat = new SimpleDateFormat();
        try {
            dateFormat.setTimeZone(StartParameters.formatTimeZone);
        } catch (Exception e) {
        }
        DecimalFormatSymbols dfsymb = new DecimalFormatSymbols(StartParameters.formatLocale);
        dfsymb.setGroupingSeparator(' ');

        dfParticles = new DecimalFormat("#,###", dfsymb);
        dfParticles.setGroupingSize(3);

        dfSeconds = new DecimalFormat("#,##0.###", dfsymb);
        dfSeconds.setGroupingSize(3);

        initLoadingIcons();
        this.paintManager = pm;
        this.mapViewer = pm.getMapViewer();
        tabs = new JTabbedPane();
        tabs.setMinimumSize(new Dimension(250, 200));
        panelTabLoading = new JPanel(new BorderLayout());
//        BoxLayout layoutLoading = new BoxLayout(panelTabLoading, BoxLayout.Y_AXIS);
//        panelTabLoading.setLayout(layoutLoading);
        tabs.add("Input", panelTabLoading);

        injectionOrganisationPanel = new InjectionOrganisatorPanel(control, pm.getMapViewer(), pm);
        tabs.add("Spills", injectionOrganisationPanel);

        measurementPanel = new MeasurementPanel(control);
        tabs.add("Measure", measurementPanel);

        panelTabSimulation = new JPanel(new BorderLayout());
        BoxLayout layoutSimulation = new BoxLayout(panelTabSimulation, BoxLayout.Y_AXIS);
        panelTabSimulation.setLayout(layoutSimulation);
        panelTabSimulation.setMaximumSize(new Dimension(500, 200));
        tabs.add("Simulation", panelTabSimulation);

        //new BorderLayout());
        this.controler = controller;
        this.control = control;
        this.control.addActioListener(this);
        this.control.getLoadingCoordinator().addActioListener(this);
        this.controler.addSimulationListener(this);
        this.buttonRun = new JToggleButton(">");
        this.buttonRun.setToolTipText("Start Simulation");
        if (control.getNetwork() == null) {
            buttonRun.setEnabled(false);
        }
        if (buttonRun.getFont().canDisplay('\u25ba')) {
            buttonRun.setText("\u25ba");
        } else if (buttonRun.getFont().canDisplay('\u25f5')) {
            buttonRun.setText("\u25f5");
        }
        this.buttonStep = new JToggleButton(">|");
        this.buttonStep.setToolTipText("1 Step");
        if (buttonStep.getFont().canDisplay('\u23ef')) {
            buttonStep.setText("\u23ef");
        }

        this.buttonPause = new JToggleButton("||");
        this.buttonPause.setToolTipText("Pause");
        if (buttonPause.getFont().canDisplay('\u23f8')) {
            buttonPause.setText("\u23f8");
        }
        this.buttonReset = new JToggleButton("|<<");
        this.buttonReset.setToolTipText("Reset");
        if (buttonReset.getFont().canDisplay('\u23ee')) {
            buttonReset.setText("\u23ee");
        }

        progressLoading = new JProgressBar();

        //Video
        this.panelVideo = new PanelVideoCapture(new GIFVideoCreator(mapViewer));
        this.panelVideo.setBorder(new TitledBorder("Video GIF"));

        //Simulation Control buttons        
        panelButtons = new JPanel(new GridLayout(3, 1, 5, 5));
        panelButtons.setBorder(new TitledBorder("Control"));

        JPanel panelFirstRow = new JPanel(new GridLayout(1, 2, 5, 5));
        panelButtons.add(panelFirstRow);
        panelFirstRow.add(buttonRun);
        panelFirstRow.add(buttonStep);
        buttonStep.setEnabled(false);

        JPanel panelSecondRow = new JPanel(new GridLayout(1, 2, 5, 5));
        panelSecondRow.add(buttonPause);
        panelSecondRow.add(buttonReset);
        panelButtons.add(panelSecondRow);
        this.add(panelButtons, BorderLayout.NORTH);

        this.add(tabs, BorderLayout.CENTER);

        //Loading buttons
        buildFilesLoadingPanel();

        // SImulation Parameter
        JPanel panelParameter = new JPanel(new GridLayout(2, 1));
        panelParameter.setBorder(new TitledBorder("Parameter"));
        panelParameter.setMaximumSize(new Dimension(500, 70));
        panelTabSimulation.add(panelParameter);
        //timestep
        JPanel panelParameterTimestep = new JPanel(new BorderLayout());
        panelParameterTimestep.add(new JLabel("Timestep \u0394t :  "), BorderLayout.WEST);
        textTimeStep = new JTextField(controler.getDeltaTime() + "");
        panelParameterTimestep.add(textTimeStep, BorderLayout.CENTER);
        panelParameterTimestep.add(new JLabel("sec."), BorderLayout.EAST);
        panelParameter.add(panelParameterTimestep);
        //TimestepCalculation Explicit/CrankNicolson
        JPanel panelTimestepCalculation = new JPanel(new GridLayout(1, 3, 2, 2));
        panelTimestepCalculation.setBorder(new TitledBorder("Time Integration Scheme"));
        panelTimestepCalculation.setMaximumSize(new Dimension(500, 50));
        group_timestep = new ButtonGroup();
        radioExplicit = new JRadioButton("Explicit", ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.EXPLICIT);
        radioExplicit.setToolTipText("Reference time for velocity is the start of the simulation loop cycle.");
        radioStepsplicit = new JRadioButton("Stepsplicit", ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.STEPSPLICIT);
        radioStepsplicit.setToolTipText("Reference time for velocity calculation is the entrance time of a particle into a new cell.");
        radioCrankNicolson = new JRadioButton("Crank-Nicolson", ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.CRANKNICOLSON);
        radioCrankNicolson.setToolTipText("Use mean velocity in the time interval of the loop cycle.");
        group_timestep.add(radioCrankNicolson);
        group_timestep.add(radioExplicit);
        group_timestep.add(radioStepsplicit);
        panelTimestepCalculation.add(radioExplicit);
        panelTimestepCalculation.add(radioStepsplicit);
        panelTimestepCalculation.add(radioCrankNicolson);
        panelTabSimulation.add(panelTimestepCalculation);
        //SurfaceMovement
        checkParticleDryMovement = new JCheckBox("Dry movement", ParticleSurfaceComputing2D.gradientFlowForDryCells);
        checkParticleDryMovement.setToolTipText("Follow surface slope if velocity is below 5cm/s");
        checkEnterdry = new JCheckBox("Enter dry cells", !ParticleSurfaceComputing2D.preventEnteringDryCell);
        checkEnterdry.setToolTipText("Allow movement into cells with dry and very slow velocity");
        checkProjectAtObstacles = new JCheckBox("Slide at edges", ParticleSurfaceComputing2D.slidealongEdges);
        checkProjectAtObstacles.setToolTipText("Projection of movement vectors along edges to boundaries");
        checkBlockSlow = new JCheckBox("StopSlow", ParticleSurfaceComputing2D.blockVerySlow);
        checkBlockSlow.setToolTipText("Stop and disable movement, if movement is stuck");
        checkMeanZigzagVelocity = new JCheckBox("Smooth ZigZag", ParticleSurfaceComputing2D.meanVelocityAtZigZag);
        checkMeanZigzagVelocity.setToolTipText("Use mean velocity if particles is trapped between two cells");
        JPanel panelMovementAlgorithm = new JPanel(new GridLayout(3, 2, 5, 5));
        panelMovementAlgorithm.setMaximumSize(new Dimension(500, 80));
        panelMovementAlgorithm.setBorder(new TitledBorder("Particle Movement"));
        panelMovementAlgorithm.add(checkEnterdry);
        panelMovementAlgorithm.add(checkProjectAtObstacles);
        panelMovementAlgorithm.add(checkParticleDryMovement);
        panelMovementAlgorithm.add(checkBlockSlow);
        panelMovementAlgorithm.add(checkMeanZigzagVelocity);
        panelTabSimulation.add(panelMovementAlgorithm);

        // Velocity Function instead of Dispersion
//        checkVelocityFunction = new JCheckBox("Velocity function", ParticlePipeComputing.useStreamlineVelocity);
//        checkVelocityFunction.setToolTipText("Use Streamline equivalent velocity instead of turbulent Dispersion.");
        //Seed
        JPanel panelSeed = new JPanel(new BorderLayout());
        panelSeed.add(new JLabel("Seed :"), BorderLayout.WEST);
        textSeed = new JFormattedTextField(DecimalFormat.getIntegerInstance(StartParameters.formatLocale));
        textSeed.setValue(controller.getSeed());
        panelSeed.add(textSeed, BorderLayout.CENTER);
        panelParameter.add(panelSeed);

        ////////////////////
        // Paneltimeslide
        this.panelTimeSlide = new JPanel(new GridLayout(3, 1));
        panelTimeSlide.setMaximumSize(new Dimension(500, 100));
        this.panelTimeSlide.setBorder(new TitledBorder("Simulation time"));
        JPanel panelTimeLabels = new JPanel(new BorderLayout());
        panelTimeLabels.add(new JLabel("Start"), BorderLayout.WEST);
        JLabel lact = new JLabel("Actual");
        lact.setHorizontalAlignment(SwingConstants.CENTER);
        panelTimeLabels.add(lact, BorderLayout.CENTER);
        panelTimeLabels.add(new JLabel("End"), BorderLayout.EAST);

        this.labelStarttime = new JLabel("Start");
        this.labelEndtime = new JLabel("End");
        this.labelEndtime.setHorizontalAlignment(SwingConstants.RIGHT);
        this.labelactualTime = new JLabel("slider");
        this.labelactualTime.setHorizontalAlignment(SwingConstants.CENTER);

        this.progressSimulation = new JProgressBar();
        this.progressSimulation.setMaximum(100);
        this.progressSimulation.setMinimum(0);
        this.progressSimulation.setValue(0);
        JPanel panelTimes = new JPanel(new GridLayout(1, 3));
        this.progressSimulation.setStringPainted(true);
        this.labelSimulationTime = new JLabel();
//        this.panelTimeSlide.add(progressSimulation, BorderLayout.NORTH);
        this.panelTimeSlide.add(panelTimeLabels, BorderLayout.NORTH);
        panelButtons.add(progressSimulation);
        panelTimes.add(labelStarttime, BorderLayout.WEST);
        panelTimes.add(labelactualTime, BorderLayout.CENTER);
        panelTimes.add(labelEndtime, BorderLayout.EAST);

        this.panelTimeSlide.add(panelTimes);
        this.panelTimeSlide.add(labelSimulationTime, BorderLayout.SOUTH);

        this.panelTabSimulation.add(panelTimeSlide);

        // Panel Calculation status
        JPanel panelCalculation = new JPanel(new GridLayout(4, 1));
        panelCalculation.setMaximumSize(new Dimension(500, 120));
        panelCalculation.setBorder(new TitledBorder("Calculation"));
        JPanel panelCalculationTime = new JPanel(new BorderLayout());
        panelCalculationTime.add(new JLabel("used calc. "), BorderLayout.WEST);
        this.labelCalculationTime = new JLabel("0");
        panelCalculationTime.add(labelCalculationTime, BorderLayout.CENTER);
        labelCalculationTime.setHorizontalAlignment(JLabel.CENTER);
        panelCalculationTime.add(new JLabel("    sec."), BorderLayout.EAST);
        panelCalculation.add(panelCalculationTime);

        JPanel panelCalculationPerStep = new JPanel(new BorderLayout());
        panelCalculationPerStep.add(new JLabel("avrg. need "), BorderLayout.WEST);
        this.labelCalculationPerStep = new JLabel("0");
        panelCalculationPerStep.add(labelCalculationPerStep, BorderLayout.CENTER);
        labelCalculationPerStep.setHorizontalAlignment(JLabel.CENTER);
        panelCalculationPerStep.add(new JLabel(" ms/step"), BorderLayout.EAST);
        panelCalculation.add(panelCalculationPerStep);

        JPanel panelCalculationSteps = new JPanel(new BorderLayout());
        panelCalculationSteps.add(new JLabel("steps : "), BorderLayout.WEST);
        this.labelCalculationSteps = new JLabel("0");
        panelCalculationSteps.add(labelCalculationSteps, BorderLayout.CENTER);
        labelCalculationSteps.setHorizontalAlignment(JLabel.CENTER);
        panelCalculation.add(panelCalculationSteps);

        JPanel panelCalculationParticle = new JPanel(new BorderLayout());
        panelCalculationParticle.add(new JLabel("Particles :"), BorderLayout.WEST);
        this.labelParticleActive = new JLabel("0");
        panelCalculationParticle.add(labelParticleActive, BorderLayout.CENTER);
        labelParticleActive.setHorizontalAlignment(JLabel.CENTER);
        this.labelParticlesTotal = new JLabel("/0");
        panelCalculationParticle.add(labelParticlesTotal, BorderLayout.EAST);
        panelCalculation.add(panelCalculationParticle);

        panelTabSimulation.add(panelCalculation);

        /////////////////////
        ///// ACTION Listener
        buttonSetupSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonSetupSave.setForeground(Color.darkGray);
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                fc.setFileFilter(new FileNameExtensionFilter("Project file (*.xml)", "xml"));
                int n = fc.showSaveDialog(SingleControllPanel.this);
                if (n == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (!f.getName().endsWith(".xml")) {
                        f = new File(f.getAbsolutePath() + ".xml");
                    }
                    if (f.exists()) {
                        n = JOptionPane.showConfirmDialog(buttonSetupSave, "Override existing file?", f.getName() + " already exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (n != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                    try {
                        if (control.getLoadingCoordinator().saveSetup(f)) {
                            buttonSetupSave.setForeground(Color.green.darker());
                            StartParameters.setStartFilePath(f.getAbsolutePath());
                            labelSetupName.setText(f.getName());
                            labelSetupName.setToolTipText(f.getAbsolutePath());
                        } else {
                            buttonSetupSave.setForeground(Color.red.darker());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        buttonSetupSave.setForeground(Color.red.darker());
                    }
                }
            }
        });

        buttonSetupLoad.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonSetupLoad.setForeground(Color.darkGray);
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                fc.setFileFilter(new FileNameExtensionFilter("Project file (*.xml)", "xml"));
                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (!f.getName().endsWith(".xml")) {
                        f = new File(f.getAbsolutePath() + ".xml");
                    }
                    try {
                        Setup setup = Setup_IO.load(f);
                        if (setup != null) {
                            control.getLoadingCoordinator().applySetup(setup);
                            buttonSetupLoad.setForeground(Color.green.darker());
                            StartParameters.setStartFilePath(f.getAbsolutePath());
                            labelSetupName.setText(f.getName());
                            labelSetupName.setToolTipText(f.getAbsolutePath());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        buttonSetupLoad.setForeground(Color.red.darker());
                    }
                }
            }
        });

        buttonFileNetwork.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == JFileChooser.APPROVE_OPTION) {
                    control.getLoadingCoordinator().setPipeNetworkFile(fc.getSelectedFile());
                }
                updateGUI();
            }
        });

        buttonFilePipeResult.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = "";
                if (control.getLoadingCoordinator().getFileNetwork() != null) {
                    folder = control.getLoadingCoordinator().getFileNetwork().getAbsolutePath();
                }
                JFileChooser fc = new JFileChooser(folder);
                fc.setFileFilter(new FileNameExtensionFilter("HE / SWMM", new String[]{"idbr", "idbf", "out"}));
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        return file.getName().toLowerCase().endsWith(".idbf") || file.getName().toLowerCase().endsWith(".idbr");
                    }

                    @Override
                    public String getDescription() {
                        return ".idbf/.idbr HESQL";
                    }
                });
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        return file.getName().toLowerCase().endsWith(".out") || file.getName().toLowerCase().endsWith(".rpt");
                    }

                    @Override
                    public String getDescription() {
                        return ".out/.irpt SWMM5";
                    }
                });
                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    //Does Network fit to this result?
                    //Find corresponding Network model file
                    try {
                        FileContainer c = control.getLoadingCoordinator().findDependentFiles(fc.getSelectedFile(), true);

                        //Find corresponding waterlevel file
                        String question = "Load corresponding files?";
                        if (c.getPipeNetwork() != null) {
                            question += "\n" + c.getPipeNetwork().getParentFile().getName() + "/" + c.getPipeNetwork().getName();
                            if (c.isPipeNetworkLoaded()) {
                                question += " LOADED";
                            }
                        }
                        if (c.getSurfaceDirectory() != null) {
                            question += "\n" + c.getSurfaceDirectory().getParentFile().getName();
                            if (c.isSurfaceTopologyLoaded()) {
                                question += " LOADED";
                            }
                        }
                        if (c.getSurfaceResult() != null) {
                            question += "\n" + c.getSurfaceResult().getParentFile().getName() + "/" + c.getSurfaceResult().getName();
                            if (c.isSurfaceResultLoaded()) {
                                question += " LOADED";
                            }
                        }
                        int m = JOptionPane.showConfirmDialog(SingleControllPanel.this, question, "Dependencies found", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                        if (m == JOptionPane.YES_OPTION || m == JOptionPane.NO_OPTION) {
                            control.getLoadingCoordinator().setPipeResultsFile(fc.getSelectedFile(), true);
                        }

                        if (m == JOptionPane.YES_OPTION) {
                            if (!c.isPipeNetworkLoaded() && c.getPipeNetwork() != null) {
                                control.getLoadingCoordinator().setPipeNetworkFile(c.getPipeNetwork());
                            }
                            if (!c.isSurfaceTopologyLoaded() && c.getSurfaceDirectory() != null) {
                                control.getLoadingCoordinator().setSurfaceTopologyDirectory(c.getSurfaceDirectory());
                            }
                            if (!c.isSurfaceResultLoaded() && c.getSurfaceResult() != null) {
                                control.getLoadingCoordinator().setSurfaceFlowfieldFile(c.getSurfaceResult());
                            }
                        }
                        updateGUI();
                    } catch (SQLException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        if (checkSparsePipeLoading != null) {
            checkSparsePipeLoading.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    control.getLoadingCoordinator().sparsePipeLoading = checkSparsePipeLoading.isSelected();
                }
            });
        }

        buttonFileSurface.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";
                if (control.getSurface() != null && control.getSurface().fileTriangles != null) {
                    folder = control.getSurface().fileTriangles.getAbsolutePath();
                }
                final JFileChooser fc = new JFileChooser(folder) {
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

                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    new Thread("Select File Surface geometry") {

                        @Override
                        public void run() {
                            try {
                                control.getLoadingCoordinator().setSurfaceTopologyDirectory(fc.getCurrentDirectory());
                                buttonFileSurface.setToolTipText("Triangles: " + fc.getSelectedFile().getAbsolutePath());
                                updateGUI();
                            } catch (IOException ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }.start();

                }
            }
        });
        initFileButtonPopupMenu(buttonFileSurface);

        buttonFileWaterdepths.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";
                if (control.getSurface() != null && control.getSurface().fileWaterlevels != null) {
                    folder = control.getSurface().fileWaterlevels.getAbsolutePath();
                }
                final JFileChooser fc = new JFileChooser(folder) {
                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".shp")) {
                            return true;
                        }
                        if (file.getName().endsWith(".csv")) {
                            return true;
                        }
                        return false;
                    }
                };
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                int n = fc.showOpenDialog(SingleControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    new Thread("Select waterlevel file") {

                        @Override
                        public void run() {
                            if (control.getSurface() == null) {
                                System.err.println("No surface set in controller.");
                                return;
                            }
                            try {
                                control.getLoadingCoordinator().setSurfaceFlowfieldFile(fc.getSelectedFile());
                                buttonFileWaterdepths.setToolTipText("Waterlevel: " + fc.getSelectedFile().getAbsolutePath());
                                updateGUI();
                            } catch (Exception ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }.start();

                }
            }
        });
        initFileButtonPopupMenu(buttonFileWaterdepths);

        buttonStartLoading.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().startLoadingRequestedFiles(true);
            }
        });

        buttonStartReloadingAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().startReloadingAll(true);
            }
        });

        buttonCancelLoading.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().cancelLoading();
            }
        });

        buttonRun.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(true);
                buttonPause.setSelected(false);
                control.start();
            }
        });

        buttonStep.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                controler.step();
                buttonRun.setSelected(false);
                buttonPause.setSelected(true);
            }
        });

        buttonPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(false);
                buttonPause.setSelected(true);
                controler.stop();
            }
        });

        buttonReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                buttonRun.setSelected(false);
                buttonRun.setEnabled(false);
                buttonPause.setSelected(true);
                panelButtons.repaint();
                new Thread("Reset Simulation") {
                    @Override
                    public void run() {
                        try {
                            controler.reset();
                            control.resetScenario();
                        } catch (OutOfMemoryError e) {
                            JOptionPane.showMessageDialog(buttonReset, "Restart this application with more memory (e.g. -xmx 6G for 6GByte)", e.getLocalizedMessage(), JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                        buttonReset.setSelected(false);
                        buttonRun.setEnabled(true);
                    }

                }.start();

            }
        });

        checkEnterdry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleSurfaceComputing2D.preventEnteringDryCell = !checkEnterdry.isSelected();
            }
        });
        checkProjectAtObstacles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleSurfaceComputing2D.slidealongEdges = checkProjectAtObstacles.isSelected();
            }
        });
        checkParticleDryMovement.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleSurfaceComputing2D.gradientFlowForDryCells = checkParticleDryMovement.isSelected();
            }
        });
        checkBlockSlow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleSurfaceComputing2D.blockVerySlow = checkBlockSlow.isSelected();
            }
        });
        checkMeanZigzagVelocity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleSurfaceComputing2D.meanVelocityAtZigZag = checkMeanZigzagVelocity.isSelected();
            }
        });

        //Update thread to show information about running simulation. e.g. number of active particles
        new Thread("GUI SimulationInformation Update").start();

        //Panel Simulation
        textTimeStep.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent fe) {
                if (!textTimeStep.isEditable()) {
                    return;
                }
                super.focusLost(fe); //To change body of generated methods, choose Tools | Templates.
                try {
                    double dt = Double.parseDouble(textTimeStep.getText());
                    controler.setDeltaTime(dt);
                } catch (NumberFormatException numberFormatException) {
                    textTimeStep.setText(controller.getDeltaTime() + "");
                }
            }

        });
//        checkVelocityFunction.addActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent ae) {
//                ParticlePipeComputing.useStreamlineVelocity = checkVelocityFunction.isSelected();
//                textDispersionPipe.setEnabled(!checkVelocityFunction.isSelected());
//            }
//        });
        radioExplicit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (radioExplicit.isSelected()) {
                    ParticleSurfaceComputing2D.timeIntegration = ParticleSurfaceComputing2D.TIMEINTEGRATION.EXPLICIT;
                }
            }
        });
        radioStepsplicit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (radioStepsplicit.isSelected()) {
                    ParticleSurfaceComputing2D.timeIntegration = ParticleSurfaceComputing2D.TIMEINTEGRATION.STEPSPLICIT;
                }
            }
        });
        radioCrankNicolson.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (radioCrankNicolson.isSelected()) {
                    ParticleSurfaceComputing2D.timeIntegration = ParticleSurfaceComputing2D.TIMEINTEGRATION.CRANKNICOLSON;
                }
            }
        });

        textSeed.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                controller.setSeed(Long.parseLong(textSeed.getText()));
            }
        });
        textSeed.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    //ENTER/RETURN
                    try {
                        long newSeed = Long.parseLong(textSeed.getText());
                        controller.setSeed(newSeed);
                    } catch (Exception exception) {
                        textSeed.setText(controller.getSeed() + "");
                    }
                }
            }

        });

        JPopupMenu popupCRSNetwork = buttonFileNetwork.getComponentPopupMenu();
        if (popupCRSNetwork == null) {
            popupCRSNetwork = new JPopupMenu("Network Topology");
            this.buttonFileNetwork.setComponentPopupMenu(popupCRSNetwork);
        }
        JMenuItem itemSelectCRS = new JMenuItem("CRS");
        popupCRSNetwork.add(itemSelectCRS);
        itemSelectCRS.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String current = control.getLoadingCoordinator().getCrsNetwork();
                String s = JOptionPane.showInputDialog(buttonFileNetwork, "Coordinate Reference System for Pipe Network (" + current + ")", current);
                if (s == null || s.isEmpty()) {
                    buttonFileNetwork.getComponentPopupMenu().setVisible(false);
                    return;
                }
                if (s != null && s.equals(current)) {
                    //nothing changed
                } else {
                    control.getLoadingCoordinator().setCrsNetwork(s);
                    updateLoadingState();
                }
                buttonFileNetwork.getComponentPopupMenu().setVisible(false);
            }
        });

        JPopupMenu popupCRSSurface = buttonFileSurface.getComponentPopupMenu();
        if (popupCRSSurface == null) {
            popupCRSSurface = new JPopupMenu("Surface Topography");
            this.buttonFileSurface.setComponentPopupMenu(popupCRSSurface);
        }
        JMenuItem itemSelectCRSS = new JMenuItem("CRS");
        popupCRSSurface.add(itemSelectCRSS);
        itemSelectCRSS.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String current = control.getLoadingCoordinator().getCrsSurface();
                String s = JOptionPane.showInputDialog(buttonFileNetwork, "Coordinate Reference System for Surface(" + current + ")", current);
                if (s == null || s.isEmpty()) {
                    buttonFileSurface.getComponentPopupMenu().setVisible(false);
                    return;
                }
                if (s != null && s.equals(current)) {
                    //nothing changed
                } else {
                    control.getLoadingCoordinator().setCrsSurface(s);
                    updateLoadingState();
                }
                buttonFileSurface.getComponentPopupMenu().setVisible(false);
            }
        });

        //////////////////////////////////////////////////////////////////////
        ///// Panel VIEW
        JPanel panelView = new JPanel(new BorderLayout());
        panelView.setBorder(new TitledBorder("Draw Update"));
        checkDrawUpdateIntervall = new JCheckBox("Update View 1/", controller.paintOnMap);
        final JTextField textUpdateLoops = new JTextField(controller.paintingInterval + "");
        checkDrawUpdateIntervall.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                controller.paintOnMap = checkDrawUpdateIntervall.isSelected();
                if (checkDrawUpdateIntervall.isSelected()) {
                    try {
                        int loops = Integer.parseInt(textUpdateLoops.getText());
                        controller.paintingInterval = loops;
                        paintManager.repaintPerLoops = loops;
                    } catch (Exception exception) {
                        textUpdateLoops.setText(controller.paintingInterval + "");
                    }
                } else {
                    paintManager.repaintPerLoops = Integer.MAX_VALUE;
                }
            }
        });
        panelView.setPreferredSize(new Dimension(220, 50));
        panelView.setMaximumSize(new Dimension(400, 50));
        panelView.add(checkDrawUpdateIntervall, BorderLayout.WEST);

        textUpdateLoops.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == 10) {
                    try {
                        int loops = Integer.parseInt(textUpdateLoops.getText());
                        controller.paintingInterval = loops;
                        paintManager.repaintPerLoops = loops;
                    } catch (Exception exception) {
                        textUpdateLoops.setText(controller.paintingInterval + "");
                    }
                }
            }
        });
        textUpdateLoops.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    int loops = Integer.parseInt(textUpdateLoops.getText());
                    controller.paintingInterval = loops;
                    paintManager.repaintPerLoops = loops;
                } catch (Exception exception) {
                    textUpdateLoops.setText(controller.paintingInterval + "");
                }
            }
        });
        panelView.add(textUpdateLoops, BorderLayout.CENTER);
        panelView.add(new JLabel("loops"), BorderLayout.EAST);
        panelTabSimulation.add(panelView);

        ////////////////////////////////////////////////////////////////////////
        ////// Panelshapeview
        panelShapes = new JPanel();
        panelShapes.setBorder(new TitledBorder("Shapeview"));
        panelShapes.setLayout(new BoxLayout(panelShapes, BoxLayout.Y_AXIS));
        sliderTimeShape = new JSlider(0, 1000, 0);
        panelShapes.add(sliderTimeShape);
        labelSliderTime = new JLabel();
        labelSliderTime.setAlignmentX(0.5f);
        panelShapes.add(labelSliderTime);

        this.add(panelShapes, BorderLayout.SOUTH);
        panelShapePipe = new JPanel(new GridLayout((advancedOpions ? 4 : 2), 1));
        TitledBorder borderShapePipe = new TitledBorder("Pipe Shapes");
        borderShapePipe.setBorder(new LineBorder(Color.BLUE.darker(), 2, true));
        panelShapePipe.setBorder(borderShapePipe);
        final JComboBox<PaintManager.PIPESHOW> comboPipeShow = new JComboBox<>(PaintManager.PIPESHOW.values());

        final JComboBox<PipeThemeLayer.LAYERS> comboPipeThemes = new JComboBox<>(PipeThemeLayer.LAYERS.values());

        comboPipeShow.setSelectedItem(PaintManager.PIPESHOW.GREY);
        buttonLoadAllPipeTimelines = new JButton("Load all Timelines");
        buttonLoadAllPipeTimelines.setToolTipText("Show values for all pipes & manholes, not only for the subset of affected capacities.");
        if (advancedOpions) {
            panelShapePipe.add(buttonLoadAllPipeTimelines);
        }
        if (advancedOpions) {
            panelShapePipe.add(comboPipeThemes);
        }
        panelShapePipe.add(comboPipeShow);

        panelShapes.add(panelShapePipe);

        panelShapesSurface = new JPanel(new GridLayout(2, 1));
        TitledBorder borderShapeSurface = new TitledBorder("Surface Shapes");
        borderShapeSurface.setBorder(new LineBorder(Color.GREEN.darker(), 2, true));
        panelShapesSurface.setBorder(borderShapeSurface);
        checkTrianglesNodes = new JCheckBox("Triangles as Nodes", paintManager.isDrawingTrianglesAsNodes());
        updatePanelShapes();
        panelShapes.add(panelShapesSurface);

        final JCheckBox checkPipeArrows = new JCheckBox("Pipes as Arrows", paintManager.isDrawPipesAsArrows());
        panelShapePipe.add(checkPipeArrows, BorderLayout.CENTER);
        comboPipeShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setPipeShow((PaintManager.PIPESHOW) comboPipeShow.getSelectedItem());
            }
        });
        comboPipeThemes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (activePipeThemeLayer == PipeThemeLayer.LAYERS.values()[comboPipeThemes.getSelectedIndex()].getTheme()) {
                    //Nothing changed.
                    return;
                }
                if (activePipeThemeLayer != null) {
                    activePipeThemeLayer.removeTheme(mapViewer);
                }
                activePipeThemeLayer = PipeThemeLayer.LAYERS.values()[comboPipeThemes.getSelectedIndex()].getTheme();
                activePipeThemeLayer.initializeTheme(mapViewer, control);
                mapViewer.recalculateShapes();
                mapViewer.repaint();
            }
        });

        buttonLoadAllPipeTimelines.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    control.getLoadingCoordinator().loadTimelinesOfAllElements();
                } catch (IOException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        sliderTimeShape.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                long time = (long) (controller.getSimulationStartTime() + (controller.getSimulationTimeEnd() - controller.getSimulationStartTime()) * (sliderTimeShape.getValue() / (double) sliderTimeShape.getMaximum()));
                paintManager.setTimeToShow(time);
                if (control.getPipeResultData() != null) {
                    control.getPipeResultData().getManholeTimeline().setActualTime(time);
                    control.getPipeResultData().getPipeTimeline().setActualTime(time);
                }
                if (control.getScenario() != null) {
                    control.getScenario().setActualTime(time);
                }
                double seconds = ((time - controller.getSimulationStartTime()) / 1000L);
                double minutes = seconds / 60.;
                double hours = minutes / 60.;
                String timeelapsed = "";
                if (hours > 0) {
                    timeelapsed += (int) hours + "h ";
                }
                if (minutes > 0) {
                    timeelapsed += (int) minutes % 60 + "m ";
                }
                if (seconds > 0) {
                    timeelapsed += (int) seconds % 60 + "s ";
                }
                labelSliderTime.setText(timeelapsed);
                paintManager.updateSurfaceShows(true);
                mapViewer.recalculateShapes();
                mapViewer.needMapUpdate();
                mapViewer.repaint();
            }
        });
        checkPipeArrows.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setDrawPipesAsArrows(checkPipeArrows.isSelected());
            }
        });

        checkTrianglesNodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintManager.setSurfaceTrianglesShownAsNodes(checkTrianglesNodes.isSelected());
            }
        });

        // Zusammenbauen
        if (advancedOpions) {
            this.add(panelVideo);
        }

//        JPanel panelstretch = new JPanel(new BorderLayout());
//        this.add(panelstretch);
        startGUIUpdateThread();
    }

    public void updateGUI() {
//        System.out.println(getClass() + " updateGUI");
        synchronized (lockGUIThread) {
            lockGUIThread.notifyAll();
        }
    }

    private void updatePanelShapes() {
        panelShapesSurface.removeAll();
        int number = paintManager.getSurfaceShows().size();
        panelShapesSurface.setLayout(new GridLayout(number + 2, 1, 3, 3));
        for (int i = 0; i <= number; i++) {
            PaintManager.SURFACESHOW s = null;
            if (i < paintManager.getSurfaceShows().size()) {
                s = paintManager.getSurfaceShows().get(i);
            }
            JComboBox<PaintManager.SURFACESHOW> combo = new JComboBox<>(PaintManager.SURFACESHOW.values());
            if (s == null) {
                combo.setSelectedItem(PaintManager.SURFACESHOW.NONE);
            } else {
                combo.setSelectedItem(s);
            }
            final PaintManager.SURFACESHOW oldvalue = s;
            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (combo.getSelectedItem() == PaintManager.SURFACESHOW.NONE) {
                        paintManager.removeSurfaceShow(oldvalue);
                    } else {
                        paintManager.removeSurfaceShow(oldvalue);
                        paintManager.addSurfaceShow((PaintManager.SURFACESHOW) combo.getSelectedItem());
                    }
                    mapViewer.recalculateShapes();
                    mapViewer.repaint();
                    updatePanelShapes();
                }
            });
            panelShapesSurface.add(combo);
        }
        panelShapesSurface.add(checkTrianglesNodes);
        panelShapesSurface.revalidate();
        panelShapesSurface.repaint();
    }

    private void initTransferHandlerFile(JPanel panelFile) {
        TransferHandler th = new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }

                List<File> files;
                try {
                    files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    // should never happen (or JDK is buggy)
                    ex.printStackTrace();
                    return false;
                }
                System.out.println("input: " + files.size() + " files.");
                for (File file : files) {
                    System.out.println(file.getAbsolutePath());
                    if (file.getName().endsWith("idbf") || file.getName().endsWith("idbr") || file.getName().endsWith("idbm")) {
                        try {
                            control.getLoadingCoordinator().requestDependentFiles(file, true, true);
                        } catch (SQLException ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    } else {
                        System.out.println("wrong suffix : " + file.getAbsolutePath());
                    }
                }
                return true;
            }
        };

        panelFile.setTransferHandler(th);
        panelLoading.setTransferHandler(th);
    }

    private Color getLoadingColor(LoadingCoordinator.LOADINGSTATUS status) {
        if (status == LoadingCoordinator.LOADINGSTATUS.ERROR) {
            return new Color(255, 150, 150);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADING) {
            return new Color(200, 230, 255);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADED) {
            return new Color(200, 255, 200);
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.NOT_REQUESTED) {
            return Color.lightGray;
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.REQUESTED) {
            return Color.yellow;
        }

        return Color.magenta;

    }

    private void initLoadingIcons() {
        int size = 12;
        URL res = null;
        try {
            res = this.getClass().getResource("icons/cross_darkred.png");

            iconError = new ImageIcon(new ImageIcon(this.getClass().getClassLoader().getResource("icons/cross_darkred.png")).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));

            iconLoading = new ImageIcon(new ImageIcon(this.getClass().getClassLoader().getResource("icons/working_white.png")).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
            iconPending = new ImageIcon(new ImageIcon(this.getClass().getClassLoader().getResource("icons/3dots_black.png")).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.err.println("cannot load pictures from " + res);
            e.printStackTrace();
        }
    }

    private Icon getLoadingIcon(LoadingCoordinator.LOADINGSTATUS status) {
        if (status == LoadingCoordinator.LOADINGSTATUS.ERROR) {
            return iconError;
        }
        if (status == LoadingCoordinator.LOADINGSTATUS.LOADING) {
            return iconLoading;
        }

        if (status == LoadingCoordinator.LOADINGSTATUS.REQUESTED) {
            return iconPending;
        }

        return null;
    }

    private void initFileButtonPopupMenu(final JButton button) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemDeactivate = new JMenuItem("Do NOT use");
        itemDeactivate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (button == buttonFileSurface) {
                    try {
                        control.getLoadingCoordinator().setSurfaceTopologyDirectory(null);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    control.loadSurface(null, SingleControllPanel.this);
                } else if (button == buttonFileWaterdepths) {
                    try {
                        control.getLoadingCoordinator().setSurfaceFlowfieldFile(null);

                    } catch (Exception ex) {
                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else {
                    System.out.println("Button " + button.getText() + " can not be deactivated.");
                }
                updateGUI();
            }
        });
        menu.add(itemDeactivate);
        button.setComponentPopupMenu(menu);

    }

    private JPanel buildFilesLoadingPanel() {
        //Pipenetwork File 
        panelLoading = panelTabLoading;
        panelLoading.setMaximumSize(new Dimension(500, 250));
        panelLoading.setLayout(new BoxLayout(panelLoading, BoxLayout.PAGE_AXIS));
        panelLoading.setBorder(new TitledBorder("Files & Loading"));

        this.buttonStartLoading = new JButton("Start Loading");
        this.buttonStartReloadingAll = new JButton("Reload all");
        this.buttonCancelLoading = new JButton("Stop");
        this.buttonCancelLoading.setToolTipText("Interrupts the loading Thread");

        //Buttons to select files
        //Setup
        JPanel panelSetup = new JPanel(new BorderLayout());
        panelLoading.add(panelSetup);
        buttonSetupLoad = new JButton("Load Project...");
        buttonSetupSave = new JButton("Save Project...");
        panelSetup.add(buttonSetupLoad, BorderLayout.WEST);
        panelSetup.add(buttonSetupSave, BorderLayout.EAST);
        labelSetupName = new JLabel();
        if (control != null && control.getScenario() != null) {
            labelSetupName.setText(control.getScenario().getName());
        }
        panelSetup.add(labelSetupName, BorderLayout.CENTER);

        //Pipe Network 
        JPanel panelNetwork = new JPanel(new GridLayout(3, 1));
        panelNetwork.setPreferredSize(new Dimension(500, 120));
        TitledBorder borderPipe = new TitledBorder("Pipe Network");
        borderPipe.setBorder(new LineBorder(Color.blue.darker(), 2, true));
        panelNetwork.setBorder(borderPipe);
        this.buttonFileNetwork = new JButton("Network Topology");
        panelNetwork.add(buttonFileNetwork);
        panelLoading.add(panelNetwork);

        //Pipe velocities
        this.buttonFilePipeResult = new JButton("Pipe Velocity");
        panelNetwork.add(buttonFilePipeResult);
        if (control != null) {
            JPanel panelcheks = new JPanel(new GridLayout(1, 2));
            this.checkSparsePipeLoading = new JCheckBox("Sparse Loading", control.getLoadingCoordinator().sparsePipeLoading);
            this.checkSparsePipeLoading.setToolTipText("Sparse loading requires less memory but takes more time to load flow field during the simulation.");
            panelcheks.add(checkSparsePipeLoading);

            this.checkLoadFileSpills = new JCheckBox("Spills from File", control.getLoadingCoordinator().loadResultInjections);
            this.checkLoadFileSpills.setToolTipText("If checked, the spill definitions from the Flow field file are loaded. If false, all spills must be defined manually");
            panelcheks.add(checkLoadFileSpills);
            panelNetwork.add(panelcheks);
        }

        //Surface Panel
        JPanel panelSurface = new JPanel(new GridLayout(2, 1));
        panelSurface.setPreferredSize(new Dimension(200, 90));

        TitledBorder borderSurface = new TitledBorder("Surface");
        borderSurface.setBorder(new LineBorder(Color.GREEN.darker(), 2, true));
        panelSurface.setBorder(borderSurface);

        //Surface File
        this.buttonFileSurface = new JButton("Surface Grid");
        panelSurface.add(buttonFileSurface);

        //Surface Velocity/Waterlevel
        this.buttonFileWaterdepths = new JButton("Velocity / Waterlevel");
        panelSurface.add(buttonFileWaterdepths);
        // Row Velocity loading gdb/WL
        radioVelocityGDB = new JRadioButton("GDB", control.getLoadingCoordinator().isLoadGDBVelocity());
        radioVelocityGDB.setToolTipText("Prioritise loading surface velocities directly from GDB file.");
        radioVelocityWL = new JRadioButton("WaterLevel", !control.getLoadingCoordinator().isLoadGDBVelocity());
        radioVelocityWL.setToolTipText("Prioritise calculating of surface velocities from waterlevels.");
        groupVelocity = new ButtonGroup();
        groupVelocity.add(radioVelocityGDB);
        groupVelocity.add(radioVelocityWL);
        JPanel panelRadiobuttons = new JPanel(new BorderLayout());
        panelRadiobuttons.add(radioVelocityWL, BorderLayout.WEST);
        panelRadiobuttons.add(radioVelocityGDB, BorderLayout.EAST);
        radioVelocityGDB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().setUseGDBVelocity(radioVelocityGDB.isSelected());
            }
        });

        radioVelocityWL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                control.getLoadingCoordinator().setUseGDBVelocity(!radioVelocityWL.isSelected());
            }
        });
        this.panelLoading.add(panelSurface);
        panelLoadingStatus = new JPanel(new BorderLayout());
        panelLoadingStatus.setPreferredSize(new Dimension(200, 30));
        JPanel panelLoadingActionLabel = new JPanel(new BorderLayout());
        labelCurrentAction = new JLabel("initializing");
        labelCurrentAction.setAlignmentX(0);
        labelCurrentAction.setPreferredSize(new Dimension(150, 20));
        panelLoadingActionLabel.add(labelCurrentAction);
        panelLoading.add(panelLoadingActionLabel);
        panelLoading.add(panelLoadingStatus);

        //Build subpanels that can be inside the status panel.
        panelLoadingStatusStop = new JPanel(new BorderLayout());
        panelLoadingStatusStop.add(progressLoading, BorderLayout.CENTER);
        panelLoadingStatusStop.add(buttonCancelLoading, BorderLayout.EAST);

        panelLoadingStatusStart = new JPanel(new BorderLayout());
        panelLoadingStatusStart.add(buttonStartReloadingAll, BorderLayout.WEST);
        panelLoadingStatusStart.add(buttonStartLoading, BorderLayout.EAST);
        panelLoadingStatus.add(panelLoadingStatusStart);

        JPanel panelScenarioInfo = new JPanel(new BorderLayout());
        panelScenarioInfo.setBorder(new TitledBorder("Scenario"));
        panelLoading.add(new JSeparator(JSeparator.VERTICAL));
        labelScenarioInformation = new JLabel();
        labelScenarioInformation.setPreferredSize(new Dimension(300, 120));

        panelScenarioInfo.add(labelScenarioInformation, BorderLayout.CENTER);
        panelLoading.add(panelScenarioInfo);

        //Button Raingauge
        buttonShowRaingauge = new JButton("Raingauge");
        buttonShowRaingauge.setToolTipText("Show Raingauge of current scenario.");
        panelScenarioInfo.add(buttonShowRaingauge, BorderLayout.SOUTH);
        buttonShowRaingauge.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new JFrame("Raingauge");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setBounds(SingleControllPanel.this.getBounds().x + SingleControllPanel.this.getBounds().width, SingleControllPanel.this.getBounds().y + 30, 400, 300);
                //Timelinepanel
                PrecipitationTimelinePanel timelinePanel = new PrecipitationTimelinePanel("Precipitation", control);
                timelinePanel.startAtZero = true;
                frame.add(timelinePanel, BorderLayout.CENTER);
                if (control.getLoadingCoordinator().getFiletype() == LoadingCoordinator.FILETYPE.HYSTEM_EXTRAN_7 || control.getLoadingCoordinator().getFiletype() == LoadingCoordinator.FILETYPE.HYSTEM_EXTRAN_8) {
                    File file = control.getLoadingCoordinator().getFilePipeFlowfield();
                    if (file != null) {
                        try {
                            try {
                                frame.setTitle("Raingauge of Piperesults '" + HE_Database.readResultname(file) + "'");
                            } catch (Exception exception) {
                                frame.setTitle("Exception reading file: " + exception.getLocalizedMessage());
                            }
                            Raingauge_Firebird raingauge = HE_Database.readRegenreihe(file);
                            TimeSeries ts = timelinePanel.createRainGaugeIntervalTimeSeries(raingauge);
                            ((SeriesKey) ts.getKey()).renderAsBar = true;
                            timelinePanel.getCollection().addSeries(ts);

                        } catch (Exception ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (control.getLoadingCoordinator().getFiletype() == LoadingCoordinator.FILETYPE.SWMM_5_1) {
                    File file = control.getLoadingCoordinator().getFilePipeFlowfield();
                    if (file != null) {
                        try {
                            frame.setTitle("Cannot read Raingauge from " + file.getName());
                            timelinePanel.collection.removeAllSeries();
                        } catch (Exception ex) {
                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                timelinePanel.showCheckBoxPanel(false);
                TimeSeriesEditorTablePanel panelTable = new TimeSeriesEditorTablePanel(timelinePanel);
                panelTable.setPreferredSize(new Dimension(30, 100));
                frame.add(panelTable, BorderLayout.SOUTH);
                frame.setVisible(true);
                panelTable.getTable().collectionChanged();
            }
        });

        if (checkSparsePipeLoading != null) {
            checkSparsePipeLoading.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    control.getLoadingCoordinator().sparsePipeLoading = checkSparsePipeLoading.isSelected();
                }
            });
        }

        if (checkLoadFileSpills != null) {
            checkLoadFileSpills.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    control.getLoadingCoordinator().loadResultInjections = checkLoadFileSpills.isSelected();
                }
            });
        }

        return panelLoading;
    }

    public void updateScenarioLabel() {
        StringBuilder str = new StringBuilder("<html>");
        Scenario sc = control.getScenario();
        if (control.getNetwork() != null) {
            Network nw = control.getNetwork();
            str.append("Pipes:    " + nw.getPipes().size() + "<br>");
            str.append("Manholes: " + nw.getManholes().size() + "<br>");
            if (sc != null && sc.getStatusTimesPipe() != null) {
                str.append("Timestep: " + ((sc.getStatusTimesPipe().getEndTime() - sc.getStatusTimesPipe().getStartTime()) / (sc.getStatusTimesPipe().getNumberOfTimes() - 1) / 1000) + " s<br><br>");
            }
        }
        if (control.getSurface() != null) {
            str.append("Surface:  " + control.getSurface().getTriangleMids().length + " cells<br>");
            if (sc != null && sc.getStatusTimesSurface() != null) {
                str.append("Timestep: " + ((sc.getStatusTimesSurface().getEndTime() - sc.getStatusTimesSurface().getStartTime()) / (sc.getStatusTimesSurface().getNumberOfTimes() - 1) / 1000) + " s<br><br>");
            }

        }
        str.append("</html>");
        labelScenarioInformation.setText(str.toString());
    }

//    public void startUpdateThread() {
//        if (control.getLoadingCoordinator().action != null) {
//            final Action action = control.getLoadingCoordinator().action;
//            if (action.hasProgress) {
//                Thread update = new Thread("UpdateProgress_" + action.description) {
//                    int loops = 0;
//                    int lastprogress = -1;
//
//                    @Override
//                    public void run() {
//                        while (loops++ < 100) {
//                            //Only update if this is the current working 
//                            if (action == control.getLoadingCoordinator().action) {
//                                if ((int) (action.progress * 100) > lastprogress) {
//                                    loops = 0;
//                                    lastprogress = (int) (action.progress * 100);
//                                    progressLoading.setIndeterminate(false);
//                                    progressLoading.setValue(lastprogress);
//                                    try {
//                                        Thread.sleep(200);
//                                    } catch (InterruptedException ex) {
//                                        Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
//                                    }
//                                }
//                            } else {
//                                break;
//                            }
//                        }
//                        System.out.println("Died update thread for " + action.description);
//                    }
//
//                };
//                update.start();
//            } else {
//                progressLoading.setIndeterminate(true);
//            }
//        }
//    }
    @Override
    public void actionFired(Action action, Object source) {
        this.currentAction = action;
        updateEditableState();
        updateLoadingState();
        updateScenarioInformation();
        if (action.parent == null && action.progress == 1) {
            updatePanelInjections();
        }
    }

    @Override
    public void loadNetwork(Network network, Object caller) {
        updateLoadingState();
        updateScenarioInformation();

        updateScenarioLabel();
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
        updateLoadingState();
        updateScenarioInformation();
        updateScenarioLabel();
    }

    @Override
    public void loadScenario(Scenario scenario, Object caller) {
        updateEditableState();
        updateLoadingState();
        updateScenarioInformation();
        updatePanelInjections();
        updateSimulationRunInformation();

        updateScenarioLabel();
        if (control != null && control.getScenario() != null) {
            labelSetupName.setText(control.getScenario().getName());
        }
    }

    public void updateLoadingState() {
        LoadingCoordinator lc = control.getLoadingCoordinator();

        buttonFileNetwork.setBackground(getLoadingColor(lc.getLoadingpipeNetwork()));
        buttonFileNetwork.setIcon(getLoadingIcon(lc.getLoadingpipeNetwork()));
        if (lc.getFileNetwork() != null) {
            buttonFileNetwork.setToolTipText(lc.getLoadingSurface() + ": " + lc.getFileNetwork().getAbsolutePath());
        } else {
            buttonFileNetwork.setToolTipText("Not set");
        }

        buttonFilePipeResult.setBackground(getLoadingColor(lc.getLoadingPipeResult()));
        buttonFilePipeResult.setIcon(getLoadingIcon(lc.getLoadingPipeResult()));
        if (lc.getFilePipeFlowfield() != null) {
            buttonFilePipeResult.setToolTipText(lc.getLoadingPipeResult() + ": " + lc.getFilePipeFlowfield().getAbsolutePath());
            if (lc.getLoadingPipeResult() == LoadingCoordinator.LOADINGSTATUS.LOADED) {
            }
        } else {
            buttonFilePipeResult.setToolTipText("Not set");
        }
        if (checkSparsePipeLoading != null) {
            checkSparsePipeLoading.setSelected(control.getLoadingCoordinator().sparsePipeLoading);
        }
        if (checkLoadFileSpills != null) {
            checkLoadFileSpills.setSelected(control.getLoadingCoordinator().loadResultInjections);
        }

        buttonFileSurface.setBackground(getLoadingColor(lc.getLoadingSurface()));
        buttonFileSurface.setIcon(getLoadingIcon(lc.getLoadingSurface()));
        if (lc.getFileSurfaceTriangleIndicesDAT() != null) {
            buttonFileSurface.setToolTipText(lc.getLoadingSurface() + ": " + lc.getFileSurfaceTriangleIndicesDAT().getParent());
        } else {
            buttonFileSurface.setToolTipText("Not set");
        }

//        buttonFileStreetinlets.setBackground(getLoadingColor(lc.getLoadingStreetInlets()));
//        buttonFileStreetinlets.setIcon(getLoadingIcon(lc.getLoadingStreetInlets()));
//        if (lc.getFileStreetInletsSHP() != null) {
//            buttonFileStreetinlets.setToolTipText(lc.getLoadingStreetInlets() + ": " + lc.getFileStreetInletsSHP().getAbsolutePath());
//        } else {
//            buttonFileStreetinlets.setToolTipText("Not set");
//        }
        buttonFileWaterdepths.setBackground(getLoadingColor(lc.getLoadingSurfaceVelocity()));
        buttonFileWaterdepths.setIcon(getLoadingIcon(lc.getLoadingSurfaceVelocity()));
        if (lc.getFileSurfaceWaterlevels() != null) {
            buttonFileWaterdepths.setToolTipText(lc.getLoadingSurfaceVelocity() + ": " + lc.getFileSurfaceWaterlevels().getAbsolutePath());
        } else {
            buttonFileWaterdepths.setToolTipText("Not set");
        }

        if (lc.isLoading()) {

            progressLoading.setVisible(true);
            if (currentAction != null) {
                labelCurrentAction.setText(currentAction.toString());
                progressLoading.setIndeterminate(!currentAction.hasProgress);
                if (currentAction.hasProgress) {
                    progressLoading.setValue((int) (currentAction.progress * 100));
                }
            } else {
                labelCurrentAction.setText("No Action. Still Loading.");
                progressLoading.setIndeterminate(true);
            }
            if (!panelLoadingStatusStop.isShowing()) {
                panelLoadingStatus.removeAll();
                panelLoadingStatus.add(panelLoadingStatusStop);
                panelLoadingStatusStop.revalidate();
                panelLoadingStatus.revalidate();
                panelLoadingStatus.repaint();
            }

//            revalidate();
//            try {
//                sleep(updateThreadUpdateIntervalMS);
//            } catch (Exception e) {
//
//            }
        } else {
            //Reset the layout to non-working style
            progressLoading.setIndeterminate(false);
            progressLoading.setValue(0);
            progressLoading.setStringPainted(false);
            labelCurrentAction.setText("");
            progressLoading.setVisible(false);
            if (!panelLoadingStatusStart.isShowing()) {
                panelLoadingStatus.removeAll();
                panelLoadingStatus.add(panelLoadingStatusStart);
                panelLoadingStatusStart.revalidate();
                panelLoadingStatus.revalidate();
            }
            panelLoadingStatus.repaint();
//            revalidate();

        }

//        panelLoadingStatusStart.revalidate();
//        panelLoadingStatusStop.revalidate();
        panelLoading.revalidate();
        panelButtons.revalidate();
    }

    /**
     * Update of the GUI elements showing the fixed scenario information. Time +
     * Injection information. This only needs to be called before the simulation
     * starts, because all information should be unchanged during the simulation
     */
    public void updateScenarioInformation() {
        calStart.setTimeInMillis(controler.getSimulationStartTime());
        calEnd.setTimeInMillis(controler.getSimulationTimeEnd());
        longerThan1Day = false;
        if (calStart.get(GregorianCalendar.DAY_OF_YEAR) != calEnd.get(GregorianCalendar.DAY_OF_YEAR) || calStart.get(GregorianCalendar.YEAR) != calEnd.get(GregorianCalendar.YEAR)) {
            longerThan1Day = true;
        }
        labelStarttime.setToolTipText(dateFormat.format(controler.getSimulationStartTime()));
        labelEndtime.setToolTipText(dateFormat.format(controler.getSimulationTimeEnd()));
        if (longerThan1Day) {
            labelStarttime.setText(calStart.get(GregorianCalendar.DAY_OF_MONTH) + "." + (calStart.get(GregorianCalendar.MONTH) + 1) + " ");
            labelEndtime.setText(calEnd.get(GregorianCalendar.DAY_OF_MONTH) + "." + (calEnd.get(GregorianCalendar.MONTH) + 1) + " ");
        } else {
            labelStarttime.setText("");
            labelEndtime.setText("");
        }
        labelStarttime.setText(labelStarttime.getText() + calStart.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (calStart.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (calStart.get(GregorianCalendar.MINUTE)) + " ");
        labelEndtime.setText(labelEndtime.getText() + calEnd.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (calEnd.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (calEnd.get(GregorianCalendar.MINUTE)) + " ");

        textSeed.setText(control.getThreadController().getSeed() + "");

        textTimeStep.setText(ThreadController.getDeltaTime() + "");
        checkDrawUpdateIntervall.setSelected(controler.paintOnMap);
        checkEnterdry.setSelected(!ParticleSurfaceComputing2D.preventEnteringDryCell);
        checkProjectAtObstacles.setSelected(ParticleSurfaceComputing2D.slidealongEdges);
        checkParticleDryMovement.setSelected(ParticleSurfaceComputing2D.gradientFlowForDryCells);
        checkBlockSlow.setSelected(ParticleSurfaceComputing2D.blockVerySlow);
        checkMeanZigzagVelocity.setSelected(ParticleSurfaceComputing2D.meanVelocityAtZigZag);
        labelParticlesTotal.setText("/ " + dfParticles.format(control.getThreadController().getNumberOfTotalParticles()));
//        textDispersionPipe.setText(ParticlePipeComputing.getDispersionCoefficient() + "");
//        try {
//            Dispersion2D_Calculator sc2d = control.getScenario().getMaterialByIndex(control.getScenario().getMaxMaterialID()).getDispersionCalculatorSurface();
//            textDispersionSurface.setText(sc2d.getParameterValues()[0] + "");
//            textDispersionSurface.setToolTipText(sc2d.getParameterOrderDescription()[0]);
//        } catch (Exception e) {
//            textDispersionSurface.setText("");
//            textDispersionSurface.setToolTipText(e.getLocalizedMessage());
//        }
        if (ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.EXPLICIT) {
            radioExplicit.setSelected(true);
        } else if (ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.CRANKNICOLSON) {
            radioCrankNicolson.setSelected(true);
        } else if (ParticleSurfaceComputing2D.timeIntegration == ParticleSurfaceComputing2D.TIMEINTEGRATION.STEPSPLICIT) {
            radioStepsplicit.setSelected(true);
        }

        measurementPanel.updateParameters();
    }

    public void updateSimulationRunInformation() {
        labelParticleActive.setText(dfParticles.format(control.getThreadController().getNumberOfActiveParticles()));

        labelCalculationPerStep.setText(controler.getAverageCalculationTime() + "");
        labelCalculationTime.setText(controler.getElapsedCalculationTime() / 1000 + "");
        labelCalculationSteps.setText(dfParticles.format(controler.getSteps()));
        long seconds = ((controler.getSimulationTime() - controler.getSimulationStartTime()) / 1000L);
        double minutes = seconds / 60.;
        double hours = minutes / 60.;
        synchronized (timeelapsed) {
            try {
                timeelapsed.delete(0, timeelapsed.capacity());

            } catch (Exception e) {
                timeelapsed = new StringBuilder(30);
            }
            if (hours > 0) {
                timeelapsed.append((int) hours).append("h ");
            }
            if (minutes > 0) {
                timeelapsed.append((int) minutes % 60).append("m ");
            }
            if (seconds > 0) {
                if ((int) seconds % 60 < 10) {
                    timeelapsed.append("0");
                }
                timeelapsed.append((int) seconds % 60).append("s ");
            }
            int percent = (int) (0.5 + 100 * (controler.getSimulationTime() - controler.getSimulationStartTime()) / (double) ((controler.getSimulationTimeEnd() - controler.getSimulationStartTime())));
            progressSimulation.setValue(percent);
            timeelapsed.append(" = ").append(seconds).append("s");
            labelSimulationTime.setText(timeelapsed.toString());

            calActual.setTimeInMillis(controler.getSimulationTime());
            labelactualTime.setText(calActual.get(GregorianCalendar.HOUR_OF_DAY) + ":" + (calActual.get(GregorianCalendar.MINUTE) < 10 ? "0" : "") + (calActual.get(GregorianCalendar.MINUTE)) + " ");

            if (frame != null) {
                if (controler.isSimulating()) {
                    frame.setTitle(">" + percent + "% Run");
                }
            }
        }
    }

    /**
     * Changes the editable state of buttons according to the current state of
     * the simulation. Some Values should net be changed during the simulation.
     */
    public void updateEditableState() {
        buttonRun.setEnabled((control.getNetwork() != null || control.getSurface() != null) && !control.getLoadingCoordinator().isLoading());

        if (controler.isSimulating()) {
//            textTimeStep.setEditable(false);
//            textDispersionPipe.setEditable(false);
//            textDispersionSurface.setEditable(false);

            textSeed.setEditable(false);
            textTimeStep.setEditable(false);

            buttonPause.setSelected(false);
            buttonRun.setSelected(true);

            measurementPanel.setEditable(false);

        } else {
//            textTimeStep.setEditable(true);
//            textDispersionPipe.setEditable(true);
//            textDispersionSurface.setEditable(true);
            textSeed.setEditable(true);
            textTimeStep.setEditable(true);

            buttonRun.setSelected(false);
            buttonPause.setSelected(true);
            measurementPanel.setEditable(true);
        }

    }

    private void startGUIUpdateThread() {
        if (updateGUIThread != null) {
            if (updateGUIThread.isAlive()) {
                lockGUIThread.notifyAll();
            } else {
                updateGUIThread.interrupt();
                updateGUIThread = null;
            }
        }
        if (updateGUIThread == null) {
            updateGUIThread = new Thread("GUI Repaint SinglecontrolPanel") {
                @Override
                public void run() {
                    while (!isInterrupted()) {
                        try {
                            updateLoadingState();
                            updateScenarioInformation();
                            updateSimulationRunInformation();
//                        updatePanelInjections();
                            updateEditableState();
                            //Information about shapes
                            if (control.getNetwork() != null && control.getNetwork().getPipes() != null) {
                                if (panelShapePipe.getBorder() instanceof TitledBorder) {
                                    ((TitledBorder) panelShapePipe.getBorder()).setTitle("Pipe Shapes (" + control.getNetwork().getPipes().size() + ")");
                                }
                            }
                            if (control.getSurface() != null && control.getSurface().getTriangleMids() != null) {
                                if (panelShapesSurface.getBorder() instanceof TitledBorder) {
                                    ((TitledBorder) panelShapesSurface.getBorder()).setTitle("Surface Shapes (" + control.getSurface().getTriangleMids().length + ")");
                                }

                            }
                            synchronized (lockGUIThread) {
                                lockGUIThread.wait();

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Update Thread is interrupted and terminates here.");
                }
            };
            updateGUIThread.start();
        }

    }

    private void updatePanelInjections() {
        injectionOrganisationPanel.recreatePanels();
    }

    @Override
    public void simulationINIT(Object caller) {
        updateEditableState();
        if (frame != null) {
            frame.setTitle("|| Stop");
        }
        buttonRun.setSelected(false);
        buttonPause.setSelected(true);
        updatePanelInjections();
    }

    @Override
    public void simulationSTART(Object caller) {
        updateEditableState();
        if (frame != null) {
            frame.setTitle(">");
        }
        buttonPause.setSelected(false);
        buttonRun.setSelected(true);
        startUpdateSimulationThread();
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
    }

    @Override
    public void simulationPAUSED(Object caller) {
        updateEditableState();
        if (frame != null) {
            frame.setTitle("|| Pause");
        }
        buttonRun.setSelected(false);
        buttonPause.setSelected(true);
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
        updateEditableState();
        buttonPause.setSelected(false);
        buttonRun.setSelected(true);
    }

    @Override
    public void simulationSTOP(Object caller) {
        updateEditableState();
        if (frame != null) {
            frame.setTitle("|| Stop");
        }
        buttonRun.setSelected(false);
        buttonPause.setSelected(true);
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
        updateEditableState();
        updateScenarioInformation();
        updateSimulationRunInformation();
        if (frame != null) {
            frame.setTitle(">| Fin");
        }
        buttonRun.setSelected(false);
        buttonPause.setSelected(true);
    }

    @Override
    public void simulationRESET(Object caller) {
        updateEditableState();
        updateScenarioInformation();
        updateSimulationRunInformation();
        updatePanelInjections();
        if (frame != null) {
            frame.setTitle("|< Reset");
        }
        buttonRun.setSelected(false);
        buttonPause.setSelected(true);
    }

    private void startUpdateSimulationThread() {
        if (updateSimulationThread != null && updateSimulationThread.isAlive()) {
            synchronized (lockSimulationThread) {
//                System.out.println("Revoke Simulationstate update Thread");
                lockSimulationThread.notifyAll();
            }
//            try {
//                updateSimulationThread.interrupt();
//            } catch (Exception e) {
//            }
//            updateSimulationThread = null;
        }
        if (updateSimulationThread == null || !updateSimulationThread.isAlive()) {
//            System.out.println("Create new SimStateUpdate Thread");
            updateSimulationThread = new Thread("Update Simulation GUI") {
                @Override
                public void run() {
                    while (!isInterrupted()) {
                        updateSimulationRunInformation();
                        if (!controler.isSimulating()) {
                            try {
                                synchronized (lockSimulationThread) {
                                    lockSimulationThread.wait();
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        try {
                            sleep(500);
                        } catch (InterruptedException ex) {
//                            Logger.getLogger(SingleControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if (updateSimulationThread != this) {
                            this.interrupt();
                        }
                    }
                }

            };
            updateSimulationThread.start();
        }
    }

}
