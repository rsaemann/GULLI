package com.saemann.gulli.view;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.ShapeTools;
import com.saemann.gulli.core.control.listener.CapacitySelectionListener;
import com.saemann.gulli.core.control.listener.LoadingActionListener;
import com.saemann.gulli.core.control.listener.ParticleListener;
import com.saemann.gulli.core.control.listener.SimulationActionListener;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import com.saemann.gulli.core.control.scenario.injection.InjectionInformation;
import com.saemann.gulli.core.io.extran.HE_Database;
import com.saemann.gulli.core.io.SparseTimeLineDataProvider;
import com.saemann.gulli.core.io.swmm.SWMM_Out_Reader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import com.saemann.gulli.core.model.GeoPosition;
import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.particle.HistoryParticle;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementTriangleRaster;
import com.saemann.gulli.core.model.surface.SurfacePathStatistics;
import com.saemann.gulli.core.model.surface.SurfaceTriangle;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRaster;
import com.saemann.gulli.core.model.surface.measurement.SurfaceMeasurementRectangleRaster;
import com.saemann.gulli.core.model.surface.measurement.TriangleMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLinePipe;
import com.saemann.gulli.core.model.timeline.array.TimeIndexContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelinePipe;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Inlet;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position3D;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.rgis.control.LocationIDListener;
import com.saemann.rgis.view.ColorHolder;
import com.saemann.rgis.view.DoubleColorHolder;
import com.saemann.rgis.view.GradientColorHolder;
import com.saemann.rgis.view.MapViewer;
import com.saemann.rgis.view.SimpleMapViewerFrame;
import com.saemann.rgis.view.shapes.AreaPainting;
import com.saemann.rgis.view.shapes.ArrowPainting;
import com.saemann.rgis.view.shapes.LabelPainting;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.LinePainting;
import com.saemann.rgis.view.shapes.NodePainting;
import java.util.HashMap;
import java.util.Random;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author saemann
 */
public class PaintManager implements LocationIDListener, LoadingActionListener, SimulationActionListener, ParticleListener {

    private final MapViewer mapViewer;
    private final Controller control;

    private Network network;
    private Surface surface;

    private boolean showParticles = true;
    public final String layerParticle = "Ptc";
    public final String layerParticleSurface = layerParticle + "Surf";
    public final String layerParticleNetwork = layerParticle + "Netw";
    public final String layerHistoryPath = "HistoryPath";
    public final ArrayList<String> layersHistoryPathToOutlets = new ArrayList<>();
    private ColorHolder chTravelPath = new ColorHolder(new Color(0f, 0f, 1f, 0.1f), "Travelled path");
    public final String layerShortcut = "SHORTCUT";
    private ColorHolder chShortcut = new ColorHolder(Color.magenta, "Shortcut");

    private boolean showPipes = true;
    public static final String layerPipes = "Pip";
    public static final String layerPipes1 = "Pip1";
    public static final String layerPipes2 = "Pip2";
    public static final String layerPipes3 = "Pip3";
    public static final String layerPipes4 = "Pip4";
    public static final String layerPipeOverly = "PipOv";

    public static final String layerPipesDir = "Dir";
    public static final String layerPipesDirAnti = "DirAnti";
    public static final String layerPipesDirChange = "DirChange";

    public static final String layerSurfaceBoundary = "SurfBound";

    private boolean showManholes = true;
    public static final String layerManhole = "MH";
    public static final String layerManhole1 = "MH1";
    public static final String layerManhole2 = "MH2";
    public static final String layerManhole3 = "MH3";
    public static final String layerManhole4 = "MH4";
    public static String layerInlets = "INLET";
    public static final String layerInletsPipe = "INLETPIPE";

    public static final String layerTriangle = "TRI";
    public static final String layerTraingleMeasurement = layerTriangle + "M";

    public static final String layerInjectionLocation = "INJ_LOC";
    private final ColorHolder chInjectionLocation = new ColorHolder(Color.green, "Injection");
    public static final String layerManholesOverspilling = "SPILL";
    private final DoubleColorHolder chSpillover = new DoubleColorHolder(Color.RED, Color.lightGray, "Spillover");
    private final HashMap<String, ColorHolder> colorMap = new HashMap<>();

    private final ColorHolder chParticles = new ColorHolder(Color.blue, "Particle");
    private final ColorHolder chParticlesSurface = new ColorHolder(Color.green, "Surface Particle");
    private final ColorHolder chParticlesNetwork = new ColorHolder(Color.blue, "Network Particle");
    private ColorHolder[] chConcentration;
    private boolean paintConcentrationColor = true;
    private ColorHolder[] chVelocity, chWaterlevels;
    private ParticleNodePainting[] particlePaintings;
    private boolean updatingParticleNodePaintings = false;
//    private ArrayList<ParticleNodePainting> arrayListSurface, arrayListNetwork;
    ParticleNodePainting[] arraySurface, arrayNetwork;
    private final Coordinate zeroCoordinate = new Coordinate(0, 0, 0);

    public static int maximumNumberOfParticleShapes = Integer.MAX_VALUE;
    public static int maximumNumberOfSurfaceShapes = Integer.MAX_VALUE;

    private final ColorHolder chPipes = new ColorHolder(Color.GRAY, "Pipe");
    private final ColorHolder chPipesOverlay = new ColorHolder(Color.GRAY, "Pipe Overlay");
    private final ColorHolder chPipes1 = new ColorHolder(Color.BLUE, "Pipe");
    private final ColorHolder chPipes2 = new ColorHolder(Color.orange, "Pipe");
    private final ColorHolder chPipes3 = new ColorHolder(Color.red, "Pipe");
    private final ColorHolder chPipes4 = new ColorHolder(Color.black, "Pipe");
    private final ColorHolder chManhole = new ColorHolder(Color.lightGray, "Manhole");
    private final ColorHolder chManhole1 = new ColorHolder(Color.black, "Manhole1");
    private final ColorHolder chManhole2 = new ColorHolder(Color.green, "Manhole2");
    private final ColorHolder chManhole3 = new ColorHolder(Color.red, "Manhole3");
    private final ColorHolder chManhole4 = new ColorHolder(Color.blue, "Manhole4");

    private final ColorHolder chDirectionPRO = new ColorHolder(Color.GREEN, "  In Direction");
    private final ColorHolder chDirectionANTI = new ColorHolder(Color.RED, "Anti Direction");
    private final ColorHolder chDirectionCHANGING = new ColorHolder(Color.blue, "Chaning Direct.");

    private final BasicStroke pipeStroke = new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final BasicStroke stroke2pRound = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final BasicStroke stroke3pRound = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    String layerArrow = "VELO";
    String layerHistory = "MIDS";
    public final String layerSurfaceContaminated = layerTriangle + "CONT";
    public final String layerSurfaceWaterlevel = layerTriangle + "WL";
    public final String layerSurfaceGrid = layerTriangle + "GRID";
    public final String layerSurfaceSlope = layerTriangle + "SLOPE";
    public final String layerSurfaceMeasurementRaster = layerTriangle + "MRaster";
    private final ColorHolder chTriangleMeasurement = new ColorHolder(Color.orange, "Triangle probe");
    private final DoubleColorHolder chTrianglesContaminated = new DoubleColorHolder(Color.orange, Color.yellow, "Surface contaminated");
    private final DoubleColorHolder chTrianglesGrid = new DoubleColorHolder(Color.orange, new Color(1f, 1f, 1f, 0f), "Surface Triangles");
    private final DoubleColorHolder chCellMeasurements = new DoubleColorHolder(Color.orange.darker(), new Color(1f, 1f, 1f, 0f), "Surface Measurementraster");
    private final DoubleColorHolder chTrianglesWaterlevel = new DoubleColorHolder(Color.white, Color.blue, "Surface Waterlevel");
//    ColorHolder chTriangleMids = new ColorHolder(Color.orange, "Triangle Mids");
    ColorHolder chSurfaceVelocity = new ColorHolder(Color.blue, "Surface Velocity");
    GradientColorHolder chSurfaceHeatMap = new GradientColorHolder(0, 1, Color.yellow, Color.red, 255, "HeatMap");
    public final String layerLabelWaterlevel = "WL_TEXT";

    private final ColorHolder chPolygon = new ColorHolder(Color.magenta, "Polygon");

    private final ColorHolder chContaminatedInlet = new ColorHolder(Color.GREEN, "Contaminated Inlet");
    private int contaminatedInlets = 0;

    public static final DecimalFormat df1 = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
    public static final DecimalFormat df3 = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));

    public boolean drawPipesAsArrows = true;

    private String selectedLayer = null;
    private long selectedID = -1;
    private ColorHolder[] chFillrate;

    private final HashSet<Manhole> affectedManholes = new HashSet<>();
    private final List<InjectionInfo> injections = new ArrayList<>(0);

    private Point2D.Double posTopLeftPipeCS, posBotRightPipeCS;
    private Coordinate posTopLeftSurfaceCS, posBotRightSurfaceCS;
    private int[] shownSurfaceTriangles = new int[0];
    private boolean[] showSurfaceTriangle = null;
    private GeoTools geoToolsNetwork, geoToolsSurface;

    public int repaintPerLoops = 300;

    public final Thread repaintThread;
    public boolean pauseSimulationWhilePaining = false;

//    private final ArrayList<Particle> particles = new ArrayList<>();
    /**
     * Time at which the actual visualization is shown.
     */
    public static long timeToShow = 0;

//    /**
//     * Scenario to be used. Holds Timeinformation.
//     */
//    private Scenario scenario;
    public enum PIPESHOW {

        NONE, GREY, VELOCITY, WATERLEVEL, FLOW, CONCENTRATION, MASS, CONTAMINATED, WATERTYPE, DIRECTION, STABILITAETSINDEX, SPARSETIMELINE,/* EXFILTRATION, SPILLOUTORIGIN,*/ MANHOLEOVERSPILL
    };
    private PIPESHOW pipeShow = PIPESHOW.GREY;

    public enum SURFACESHOW {

        NONE, GRID, ANALYSISRASTER,/* WATERLEVEL1, WATERLEVEL10,*/ WATERLEVEL, WATERLEVELMAX, HEATMAP_LIN, HEATMAP_LOG, HEATMAP_LIN_BAGATELL, SPECTRALMAP, CONTAMINATIONCLUSTER, PARTICLETRACE, PARTICLETRACE_OUTLET, VELOCITY, SLOPE, VERTEX_HEIGHT;
    };
    private final ArrayList<SURFACESHOW> surfaceShows = new ArrayList<>();//SURFACESHOW.NONE;
    private boolean drawTrianglesAsNodes = true;

    /**
     * Objects' that needs to know if somwthing is selected.
     */
    private final ArrayList<CapacitySelectionListener> selectionListener = new ArrayList<>(1);

    public PaintManager(Controller con, SimpleMapViewerFrame frame) {

        this.mapViewer = frame.mapViewer;
        this.control = con;
        if (control != null) {
            control.addActioListener(this);
            control.addSimulationListener(this);
            control.addParticleListener(this);
            control.getThreadController().paintingInterval = repaintPerLoops;
        }
        chParticles.setStroke(stroke2pRound);
        chTravelPath.setStroke(stroke3pRound);
        chSpillover.setStroke(chParticles.getStroke());
        chPipes.setStroke(stroke3pRound);

        chPipes1.setStroke(pipeStroke);
        chPipes2.setStroke(pipeStroke);
        chPipes3.setStroke(pipeStroke);
        chPipes4.setStroke(pipeStroke);
        BasicStroke mhstroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        chManhole1.setStroke(mhstroke);
        chManhole2.setStroke(mhstroke);
        chManhole3.setStroke(mhstroke);
        chManhole4.setStroke(mhstroke);

        chInjectionLocation.setStroke(stroke2pRound);
        chDirectionPRO.setStroke(chPipes.getStroke());
        chDirectionANTI.setStroke(chPipes.getStroke());
        chDirectionCHANGING.setStroke(chPipes.getStroke());

        initColoHolderConcentration(Color.green, Color.red);
        initColoHolderVelocity(Color.blue, Color.red);
        initColoHolderWaterlevel(Color.white, Color.blue);
        int numberofParticles = con.getThreadController().getNumberOfTotalParticles();
        particlePaintings = new ParticleNodePainting[Math.max(0, numberofParticles)];
        mapViewer.addListener(this);

        initMenuCheckboxes(frame);
        try {

            MouseAdapter ma = new MouseAdapter() {
                boolean mousedragged = false;

                @Override
                public void mouseDragged(MouseEvent me) {
                    mousedragged = true;
//                    updateUTMReference();
                }

                @Override
                public void mouseReleased(MouseEvent me) {
                    if (mousedragged) {
                        updateUTMReference();
                        updateSurfaceShows();
                    }
                    mousedragged = false;
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent mwe) {
                    updateUTMReference();
                    updateSurfaceShows();
                }
            };
            mapViewer.addMouseListener(ma);
            mapViewer.addMouseMotionListener(ma);
            mapViewer.addMouseWheelListener(ma);

        } catch (Exception ex) {
            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.repaintThread = new Thread("PaintMap Invoker") {

            @Override
            public void run() {
                while (true) {
                    try {
                        orderParticlesPainting();
                        mapViewer.recalculateShapes();
                        updateLabel();
                        mapViewer.repaint();
                        synchronized (this) {
                            try {
                                this.wait(60000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

        };
        repaintThread.start();
    }

    public void setNetwork(Network network) {
        //Clear all layers
        this.mapViewer.clearLayer(null);
        this.network = network;
        if (Network.crsUTM != null) {
            try {
                this.geoToolsNetwork = new GeoTools(CRS.decode("EPSG:4326"), Network.crsUTM);
            } catch (FactoryException ex) {
                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // Draw manholes of the network
        showManholes = true;
        if (showManholes) {
            for (Manhole mh : network.getManholes()) {
//                if (mh.tags != null && mh.tags.containsKey("Water")) 
                {
                    try {
                        int w;//Integer.parseInt(mh.tags.get("Water"));
                        w = mh.getWaterType().ordinal();
//                        if (w == 1) {
//                            NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chManhole1);
//                            mapViewer.addPaintInfoToLayer(layerManhole1, mhp);
//                        } else if (w == 2) {
//                            NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chManhole2);
//                            mapViewer.addPaintInfoToLayer(layerManhole2, mhp);
//                        } else if (w == 3) {
//                            NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chManhole3);
//                            mapViewer.addPaintInfoToLayer(layerManhole3, mhp);
//                        } else if (w == 4) {
//                            NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chManhole4);
//                            mapViewer.addPaintInfoToLayer(layerManhole4, mhp);
//                        } else {
                        NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition().lonLatCoordinate(), chManhole);
                        mapViewer.addPaintInfoToLayer(layerManhole, mhp);
//                        }
                    } catch (NumberFormatException numberFormatException) {
//                        System.err.println("Wert '" + mh.tags.get("Water") + "' for MH" + mh.getName() + " not identified.");
                        chManhole.setColor(Color.orange);
                        chManhole.setStroke(chManhole1.getStroke());
                        NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition().lonLatCoordinate(), chManhole);
                        mapViewer.addPaintInfoToLayer(layerManhole, mhp);

                    }
                }
//                else if (mh.tags != null && mh.tags.containsKey("Type")) {
//                    if (mh.tags.get("Type").equals("Polygon")) {
//                        NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chPolygon);
//                        mapViewer.addPaintInfoToLayer("POLYGON", mhp);
//                    }
//                } 
//                else {
//                    NodePainting mhp = new NodePainting(mh.getAutoID(), mh.getPosition(), chManhole);
//                    mapViewer.addPaintInfoToLayer(layerManhole, mhp);
//                }
            }
        }
        mapViewer.clearLayer(layerPipes);
        mapViewer.clearLayer(layerPipes1);
        mapViewer.clearLayer(layerPipes2);
        mapViewer.clearLayer(layerPipes3);
        mapViewer.clearLayer(layerPipes4);
        mapViewer.clearLayer(layerPipesDir);
        mapViewer.clearLayer(layerPipesDirAnti);
        mapViewer.clearLayer(layerPipesDirChange);
        mapViewer.clearLayer(layerPipeOverly);

        if (showPipes) {
            addPipesPaint();
        }

        mapViewer.recalculateShapes();
        mapViewer.adjustMap();
    }

    /**
     * Set the time, that's status is to be shown.
     *
     * @param timeToShow
     */
    public void setTimeToShow(long timeToShow) {
        if (PaintManager.timeToShow == timeToShow) {
            return;
        }
        PaintManager.timeToShow = timeToShow;
        mapViewer.repaint();
    }

    public void addTriangleMeasurement(TriangleMeasurement tm, com.saemann.gulli.core.model.GeoPosition2D position) {
        NodePainting np = new NodePainting(tm.getTriangleID(), new Coordinate(position.getLongitude(), position.getLatitude()), chTriangleMeasurement);
        np.setRadius(3);
        mapViewer.addPaintInfoToLayer(layerTraingleMeasurement, np);
    }

    public void setInletsPaint(Surface surface, boolean drawConnectionToPipe) {
        //Inlets
        mapViewer.clearLayer(layerInlets);
        mapViewer.clearLayer(layerInletsPipe);
//        System.out.println(getClass() + ":setInletsPaint");
        try {
            if (surface == null) {
//                System.out.println("no surface yet-> return");
                return;
            }
            Inlet[] objects = surface.getInlets();
            if (objects != null) {
                ColorHolder chinlet = new ColorHolder(Color.darkGray, "Inlet");
                ColorHolder chinletPipe = new ColorHolder(Color.cyan, "Inlet's pipe");
//                int counter = 0;
                long start = System.currentTimeMillis();
                for (int i = 0; i < objects.length; i++) {
                    Inlet inlet = objects[i];

                    if (inlet != null) {
                        Inlet in = inlet;
                        NodePainting np = new NodePainting(i, inlet.getPosition().lonLatCoordinate(), chinlet);
                        np.setRadius(1);
                        mapViewer.addPaintInfoToLayer(layerInlets, np);
//                        li.add(np, false);
//                        System.out.println("Add Inlet at Traingle "+tri.getManualID());
                        if (drawConnectionToPipe && in.getNetworkCapacity() != null) {
                            //Position
                            Position3D target = in.getNetworkCapacity().getPosition3D(in.getPipeposition1d());
                            Coordinate c0 = inlet.getPosition().lonLatCoordinate();
                            Coordinate c1 = target.lonLatCoordinate();

                            ArrowPainting ap = new ArrowPainting(i, new Coordinate[]{c0, c1}, chinletPipe);
                            mapViewer.addPaintInfoToLayer(layerInletsPipe, ap);

                        }
//                        counter++;
                    }
                }
//                System.out.println("Adding " + (counter) + " shapes took " + (System.currentTimeMillis() - start) + "ms.");

//                System.out.println("added " + counter + " inlet shapes to view");
            } else {
                System.out.println(getClass() + " surface has no triangles mapped inlets yet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("--ended Paintmanager. setinletspaint");
    }

    protected void addPipesPaint() {
//        System.out.println(getClass() + "::addPipesPaint");
        if (network == null || network.getPipes() == null) {
//            System.out.println(getClass() + "::addPipesPaint -> return because null pointer");
            return;
        }

        try {
            //Draw Pipes of this network
            for (final Pipe pipe : network.getPipes()) {
//                ArrayList<com.saemann.gulli.core.model.GeoPosition2D> listG = new ArrayList<>(2);
//                listG.add(pipe.getFlowInletConnection().getPosition());
//                listG.add(pipe.getFlowOutletConnection().getPosition());
//                ArrayList<com.saemann.rgis.model.GeoPosition2D> list = toRGIS(listG);

                Coordinate[] list = new Coordinate[2];
                list[0] = pipe.getFlowInletConnection().getPosition().lonLatCoordinate();
                list[1] = pipe.getFlowOutletConnection().getPosition().lonLatCoordinate();

                if (pipeShow == pipeShow.GREY) {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes);
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);
                } else if (pipeShow == pipeShow.WATERTYPE) {
//                    if (pipe.tags != null && pipe.tags.containsKey("Water")) 
                    {
                        try {
                            int w = 0;//Integer.parseInt(pipe.tags.get("Water"));
                            if (pipe.getWaterType() == Capacity.SEWER_TYPE.DRAIN) {
                                w = 1;
                            }
                            if (pipe.getWaterType() == Capacity.SEWER_TYPE.MIX) {
                                w = 2;
                            }
                            if (pipe.getWaterType() == Capacity.SEWER_TYPE.SEWER) {
                                w = 3;
                            }

                            if (w == 1) {
                                LinePainting ap;
                                if (drawPipesAsArrows) {
                                    ap = new ArrowPainting(pipe.getAutoID(), list, chPipes1);
                                } else {
                                    ap = new LinePainting(pipe.getAutoID(), list, chPipes1);
                                }
                                mapViewer.addPaintInfoToLayer(layerPipes1, ap);
                            } else if (w == 2) {
                                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes2);
                                mapViewer.addPaintInfoToLayer(layerPipes2, ap);
                            } else if (w == 3) {
                                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes3);
                                mapViewer.addPaintInfoToLayer(layerPipes3, ap);
                            } else if (w == 8) {
                                LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes2);
                                mapViewer.addPaintInfoToLayer(layerPipes4, ap);
                            }
                        } catch (Exception exception) {
                            LinePainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes);
                            mapViewer.addPaintInfoToLayer(layerPipes, ap);
                        }
                    }
                } else if (pipeShow == PIPESHOW.CONCENTRATION) {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            if (ArrayTimeLineMeasurementContainer.instance.getActualTimeIndex() > 1) {
                                double c = 0;
                                if (pipe.getMeasurementTimeLine().hasValues(ArrayTimeLineMeasurementContainer.instance.getActualTimeIndex())) {
                                    c = pipe.getMeasurementTimeLine().getConcentration(ArrayTimeLineMeasurementContainer.instance.getActualTimeIndex());
                                } else {
                                    c = pipe.getMeasurementTimeLine().getConcentration(ArrayTimeLineMeasurementContainer.instance.getActualTimeIndex() - 1);
                                }
                                if (c > 0) {
                                    Color co = getColorHolderConcentrationRelative(c * 100. / pipe.getMeasurementTimeLine().getMaxConcentration_global()).getColor();
                                    g2.setColor(co);
                                } else {
                                    g2.setColor(chPipes.getColor());
                                }
                            }
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);
                } else if (pipeShow == PIPESHOW.CONTAMINATED) {

                    LinePainting ap;
                    {
                        final ColorHolder ch = chPipesOverlay;
                        chPipesOverlay.setColor(Color.red);
                        chPipesOverlay.setStroke(stroke2pRound);
                        chPipesOverlay.setDescription("Pipe contaminated");
                        ap = new LinePainting(pipe.getAutoID(), list, ch) {

                            @Override
                            public boolean paint(Graphics2D g2) {

                                try {
                                    if (pipe.getMeasurementTimeLine().getNumberOfParticlesUntil(pipe.getMeasurementTimeLine().getContainer().getActualTimeIndex()) + pipe.getMeasurementTimeLine().getNumberOfParticles() > 0) {
                                        g2.setColor(ch.getColor());
                                        g2.setStroke(stroke3pRound);
                                        super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                                        return true;
                                    }
                                } catch (Exception e) {

                                }
                                g2.setColor(chPipes.getColor());
                                g2.setStroke(stroke2pRound);
                                super.paint(g2);
                                return false;

                            }
                        };
                        mapViewer.addPaintInfoToLayer(layerPipeOverly, ap);
//                    }
                        if (!drawPipesAsArrows) {
                            ap.arrowheadvisibleFromZoom = 200;
                        }
                    }
                } else if (pipeShow == PIPESHOW.MASS) {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            int index = pipe.getMeasurementTimeLine().getContainer().getActualTimeIndex();
                            if (index > 1) {
                                double m = 0;
                                if (pipe.getMeasurementTimeLine().hasValues(index)) {
                                    m = pipe.getMeasurementTimeLine().getMass(index);
                                } else {
                                    m = pipe.getMeasurementTimeLine().getMass(index - 1);
                                }
                                if (m > 0) {
                                    Color co = getColorHolderConcentrationRelative(m * 100. / pipe.getMeasurementTimeLine().getMaxMass()).getColor();
                                    g2.setColor(co);
                                } else {
                                    g2.setColor(chPipes.getColor());
                                }
                            }
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);

                    try {
                        if (pipe.getEndConnection().getManhole().isSetAsOutlet()) {
                            Position3D pos = pipe.getEndConnection().getPosition();
                            LabelPainting lp = new LabelPainting(pipe.getAutoID(), pos.getLongitude(), pos.getLatitude(), MapViewer.COLORHOLDER_LABEL, "Outlet: " + pipe.getMeasurementTimeLine().getTotalMass(pipe.getStatusTimeLine(), pipe.getLength()) + " kg");
                            mapViewer.addPaintInfoToLayer("OutletMass", lp);
                        }

                    } catch (Exception e) {
                    }
                } else if (pipeShow == PIPESHOW.VELOCITY) {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            double refV = 1;
                            if (pipe.getStatusTimeLine() instanceof ArrayTimeLinePipe) {
                                refV = ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).getV_max();
                            }
                            double v = pipe.getStatusTimeLine().getVelocity();
                            if (v >= 0) {
                                Color co = getColorHolderVelocityRelative((int) (v * 100. / refV)).getColor();
                                g2.setColor(co);
                            } else {
                                Color co = getColorHolderVelocityRelative((int) (-v * 100. / refV)).getColor();
                                g2.setColor(co);
                            }
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);
                } else if (pipeShow == PIPESHOW.WATERLEVEL) {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            double h = pipe.getStatusTimeLine().getWaterlevel();
                            double refh = h;
                            if (pipe.getProfile() instanceof CircularProfile) {
                                refh = ((CircularProfile) pipe.getProfile()).getDiameter();
                            }
                            if (h > 0.01) {
                                Color co = getColorHolderVelocityRelative((int) (h * 100. / refh)).getColor();
                                g2.setColor(co);
                            } else {
                                g2.setColor(chPipes.getColor());
                            }
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);
                } else if (pipeShow == PIPESHOW.DIRECTION) {
                    float max = Float.NEGATIVE_INFINITY;
                    float min = Float.POSITIVE_INFINITY;
                    for (int i = 0; i < pipe.getStatusTimeLine().getNumberOfTimes(); i++) {
                        max = Math.max(max, pipe.getStatusTimeLine().getVelocity(i));
                        min = Math.min(min, pipe.getStatusTimeLine().getVelocity(i));
                    }

                    ColorHolder ch;
                    String layer = layerPipes;
                    if (min >= 0) {
                        ch = chDirectionPRO;
                        layer = layerPipesDir;
                    } else if (max <= 0) {
                        ch = chDirectionANTI;
                        layer = layerPipesDirAnti;
                    } else {
                        ch = chDirectionCHANGING;
                        layer = layerPipesDirChange;
                    }

                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, ch) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            g2.setColor(this.getColor().getColor());
                            g2.setStroke(this.getColor().getStroke());
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layer, ap);
                } else if (pipeShow == PIPESHOW.STABILITAETSINDEX) {

                    ColorHolder ch;
                    String layer = layerPipes;
                    Color color = Color.magenta;
                    int stabilitaetsindex = -1;
//                    try {
//                        stabilitaetsindex = Integer.parseInt(pipe.tags.get("Stabilitaet"));
//                    } catch (Exception exception) {
//                    }
                    if (stabilitaetsindex == 0) {
                        color = Color.white;
                    } else if (stabilitaetsindex > 0 && stabilitaetsindex < 10) {
                        color = Color.yellow;
                    } else if (stabilitaetsindex >= 10 && stabilitaetsindex < 30) {
                        color = Color.orange;
                    } else if (stabilitaetsindex >= 30) {
                        color = Color.red;
                    }
                    final Color c = color;
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            g2.setColor(c);
                            g2.setStroke(this.getColor().getStroke());
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layer, ap);
                } else if (pipeShow == PIPESHOW.SPARSETIMELINE) {
                    String layer = layerPipes;
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                        @Override
                        public boolean paint(Graphics2D g2) {
                            if (pipe.getStatusTimeLine() instanceof SparseTimelinePipe) {
                                if (((SparseTimelinePipe) pipe.getStatusTimeLine()).isInitialized()) {
                                    g2.setColor(Color.green);
                                } else {
                                    g2.setColor(Color.red);
                                }
                            } else {
                                g2.setColor(Color.blue);
                            }

                            g2.setStroke(this.getColor().getStroke());
                            super.paint(g2);
                            return true;
                        }
                    };
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layer, ap);
                } else {
                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, chPipes);
                    if (!drawPipesAsArrows) {
                        ap.arrowheadvisibleFromZoom = 200;
                    }
                    mapViewer.addPaintInfoToLayer(layerPipes, ap);
                }
            }

            //Do overspilling manholes
            if (pipeShow == PIPESHOW.WATERLEVEL) {
                mapViewer.clearLayer(layerManholesOverspilling);
                if (affectedManholes.isEmpty()) {
                    for (Manhole mh : network.getManholes()) {
//                        float topheight = mh.getTop_height();
                        for (int i = 0; i < mh.getStatusTimeLine().getNumberOfTimes(); i++) {
                            if (mh.getStatusTimeLine().getFlowToSurface(i) > 0) {
                                affectedManholes.add(mh);
                                break;
                            }
                        }
                    }
                }
                for (final Manhole mh : affectedManholes) {

                    NodePainting pi = new NodePainting(mh.getAutoID(), mh.getPosition().lonLatCoordinate(), chSpillover) {

                        @Override
                        public boolean paint(Graphics2D g2) {
                            float ueberstau = mh.getStatusTimeLine().getActualFlowToSurface();//.getActualWaterZ() - mh.getTop_height();
                            if (ueberstau > 0) {
                                g2.setColor(chSpillover.getColor());
                            } else {
                                g2.setColor(chSpillover.getFillColor());
                            }
                            g2.draw(getOutlineShape());
                            return true;
                        }

                    };

                    pi.radius = 5;
                    pi.setShapeRound(true);
                    mapViewer.addPaintInfoToLayer(layerManholesOverspilling, pi);
                }
//            } else if (pipeShow == PIPESHOW.EXFILTRATION) {
//                String vtuPath = StartParameters.getPathUndergroundVTU();
//                if (vtuPath == null || vtuPath.isEmpty()) {
//                    return;
//                }
//
//                String layerPotentialExfiltration = "POTEXFiltration";
//                String layerPotentialInfiltration = "POTINFiltration";
//                String layerContaminantExfiltration = "ContEXFiltration";
//                String layerContaminantInfiltration = "ContInFiltration";
//
//                ColorHolder chPotEx = new ColorHolder(Color.yellow, "potential Exfiltration");
//                ColorHolder chPotIn = new ColorHolder(Color.blue.brighter(), "potential Infiltration");
//                ColorHolder chcontEx = new ColorHolder(Color.red, "Contaminant Exfiltration");
//                ColorHolder chContIn = new ColorHolder(Color.yellow, "Contaminant Dilute");
//
//                Domain3D domain = Domain3DIO.read3DFlowFieldVTU(new File(vtuPath), "EPSG:3857", "EPSG:25832");
//                System.out.println("Loaded VTU");
//                GeoTools gt = domain.geotools;
//
//                System.out.println("is longitude first? " + gt.isGloablLongitudeFirst());
//                for (Pipe pipe : network.getPipes()) {
//                    Position3D pos = pipe.getPosition3D(pipe.getLength() * 0.5);
//                    Coordinate utm = gt.toUTM(pos.getLongitude(), pos.getLatitude());
//                    utm.z = pos.z;
//                    int index = domain.getNearestCoordinateIndex(utm);
//                    if (index < 0) {
//                        System.out.println("could not find near coordinate " + pos + " => " + utm);
//                        continue;
//                    }
//
//                    if (index < 0 || index >= domain.groundwaterDistance.length) {
//                        System.out.println("Could not find Position near " + utm);
//                        continue;
//                    }
//                    //Tiefe des Wasserstandes an diesem Knoten:
//                    float gwheight = (float) (domain.position[index].z + domain.groundwaterDistance[index]);
//
//                    ColorHolder ch = chContIn;
//                    String layer = "O";
//                    if (gwheight < pos.getZ()) {
//                        //exfiltration
//                        if (pipe.getMeasurementTimeLine() != null && pipe.getMeasurementTimeLine().getMaxMass() > 0) {
//                            ch = chcontEx;
//                            layer = layerContaminantExfiltration;
//                        } else {
//                            ch = chPotEx;
//                            layer = layerPotentialExfiltration;
//                        }
//                    } else {
//                        ch = chPotIn;
//                        layer = layerPotentialInfiltration;
//                    }
//
//                    ArrayList<GeoPosition2D> list = new ArrayList<>(2);
//                    list.add(pipe.getStartConnection().getPosition());
//                    list.add(pipe.getEndConnection().getPosition());
//
//                    ArrowPainting ap = new ArrowPainting(pipe.getAutoID(), list, ch);
//                    if (!drawPipesAsArrows) {
//                        ap.arrowheadvisibleFromZoom = 200;
//                    }
//                    mapViewer.addPaintInfoToLayer(layer, ap);
//                    System.out.println("added " + pipe + " as " + layer);
//                }
//                System.out.println("All in/exfiltration shapes created");
//            } else if (pipeShow == PIPESHOW.SPILLOUTORIGIN) {
//                //Color Manholes and pipes their count of particles, that spill out to the surface during the 
//                if (control.getSurface() == null || control.getSurface().sourcesForSpilloutParticles == null) {
//                    pipeShow = PIPESHOW.GREY;
//                } else {
//
//                    //First find the maximum count
//                    int maxCounter = 0;
//                    for (Integer count : control.getSurface().sourcesForSpilloutParticles.values()) {
//                        maxCounter = Math.max(maxCounter, count);
//                    }
//                    System.out.println("maximum origin count is: " + maxCounter);
//                    final GradientColorHolder c = new GradientColorHolder(0, maxCounter + 1, Color.green, Color.red, 500, "Origin of spilled out material");
//
//                    int counter = 0;
//                    for (Manhole manhole : control.getNetwork().getManholes()) {
//                        if (control.getSurface().sourcesForSpilloutParticles.containsKey(manhole)) {
//                            final int count = control.getSurface().sourcesForSpilloutParticles.get(manhole);
//                            final int colorIndex = (count * (c.colorsGradient.length - 1)) / maxCounter;
//                            NodePainting np = new NodePainting(manhole.getAutoID(), manhole.getPosition(), c) {
//
//                                @Override
//                                public boolean paint(Graphics2D g2) {
//                                    g2.setColor(c.getGradientColor(colorIndex));
//                                    return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
//                                }
//
//                            };
//                            mapViewer.addPaintInfoToLayer(layerManholesOverspilling, np);
//                            counter++;
//                        }
//                    }
//                    System.out.println("Painted " + counter + " spill source shapes.");
//                }
            } else if (pipeShow == PIPESHOW.MANHOLEOVERSPILL) {
                int times = control.getScenario().getStatusTimesManhole().getNumberOfTimes();
                for (Manhole manhole : network.getManholes()) {
                    if (manhole.getStatusTimeLine() != null) {
                        for (int t = 0; t < times; t++) {
                            if (manhole.getStatusTimeLine().getFlowToSurface(t) > 0) {
                                NodePainting np = new NodePainting(manhole.getAutoID(), manhole.getPosition().lonLatCoordinate(), chSpillover);
                                np.setShapeRound(true);
                                np.setRadius(4);
                                mapViewer.addPaintInfoToLayer(layerManholesOverspilling, np);
                                break;
                            }
                        }
                    }
//                SparseTimeLineDataProvider dataloader = control.getNetwork().getManholes().iterator().next()..getLoadingCoordinator().getSparsePipeDataProvider();
//                System.out.println("Dataloader is "+dataloader);
//                if (dataloader != null) {
//                    if (dataloader instanceof HE_Database) {
//                        HE_Database fb = (HE_Database) dataloader;
//                        String[] names = fb.getOverspillingManholes(0.01);
//                        System.out.println("Found " + names.length + " overspilling manholes.");
//                        for (String name : names) {
//                            Manhole mh = network.getManholeByName(name);
//                            if (mh != null) {
//                                NodePainting np = new NodePainting(mh.getAutoID(), mh.getPosition().lonLatCoordinate(), chSpillover);
//                                np.setShapeRound(true);
//                                np.setRadius(4);
//                                mapViewer.addPaintInfoToLayer(layerManholesOverspilling, np);
//                            }
//                        }
//                    } else if (dataloader instanceof SWMM_Out_Reader) {
//                        
//                        //There is no simple call. have to go through all elements
//                        SWMM_Out_Reader spr = (SWMM_Out_Reader) dataloader;
//                        int times = control.getScenario().getStatusTimesManhole().getNumberOfTimes();
//                        for (Manhole manhole : network.getManholes()) {
//                            if (manhole.getStatusTimeLine() != null) {
//                                for (int t = 0; t < times; t++) {
//                                    if (manhole.getStatusTimeLine().getFlowToSurface(t) > 0) {
//                                        NodePainting np = new NodePainting(manhole.getAutoID(), manhole.getPosition().lonLatCoordinate(), chSpillover);
//                                        np.setShapeRound(true);
//                                        np.setRadius(4);
//                                        mapViewer.addPaintInfoToLayer(layerManholesOverspilling, np);
//                                        break;
//                                    }
//                                }
//                            } else {
//                                //Load information from file
//                                float[] floods = spr.getNodeValues((int) manhole.getManualID(), 5);
//                                for (int i = 0; i < floods.length; i++) {
//                                    if (floods[i] > 0) {
//                                        NodePainting np = new NodePainting(manhole.getAutoID(), manhole.getPosition().lonLatCoordinate(), chSpillover);
//                                        np.setShapeRound(true);
//                                        np.setRadius(4);
//                                        mapViewer.addPaintInfoToLayer(layerManholesOverspilling, np);
//                                        break;
//                                    }
//                                }
//                            }
//
//                        }
//                    }
                }

            } else {
                mapViewer.clearLayer(layerManholesOverspilling);
                affectedManholes.clear();
            }

        } catch (Exception ex) {
            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        mapViewer.recalculateShapes();
        mapViewer.repaint();
    }

    public void updateSurfaceShows() {
        GeometryFactory gf = null;
        if (!drawTrianglesAsNodes) {
            gf = new GeometryFactory();
        }

        for (int s = 0; s < surfaceShows.size(); s++) {
            SURFACESHOW surfaceShow = surfaceShows.get(s);

            if (surfaceShow == SURFACESHOW.GRID) {
                mapViewer.clearLayer(layerSurfaceGrid);
                if (drawTrianglesAsNodes) {
                    int id = 0;
                    if (false) {
                        for (int i = 0; i < shownSurfaceTriangles.length; i++) {
                            id = shownSurfaceTriangles[i];
                            try {
                                NodePainting np = new NodePainting(id, surface.getGeotools().toGlobal(new Coordinate(surface.getTriangleMids()[id][0], surface.getTriangleMids()[id][1])), chTrianglesGrid);
                                id++;
                                mapViewer.addPaintInfoToLayer(layerSurfaceGrid, np);
                                if (id > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            } catch (TransformException ex) {
                                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    if (true) {
                        for (double[] triangleMid : surface.getTriangleMids()) {
                            try {
                                NodePainting np = new NodePainting(id, surface.getGeotools().toGlobal(new Coordinate(triangleMid[0], triangleMid[1])), chTrianglesGrid);
                                id++;
                                mapViewer.addPaintInfoToLayer(layerSurfaceGrid, np);
                                if (id > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            } catch (TransformException ex) {
                                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                } else {
                    try {
                        if (false) {
                            int id = 0;
                            Coordinate[] coords = new Coordinate[4];
                            int[] nodes;
                            for (int i = 0; i < shownSurfaceTriangles.length; i++) {
                                id = shownSurfaceTriangles[i];

                                nodes = surface.getTriangleNodes()[id];
                                for (int j = 0; j < 3; j++) {
                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                }
                                coords[3] = coords[0];//Close ring
                                AreaPainting ap = new AreaPainting(id, chTrianglesGrid, gf.createLinearRing(coords));
                                mapViewer.addPaintInfoToLayer(layerSurfaceGrid, ap);
                                id++;
                                if (id > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            }
                        } else {
                            int id = 0;
                            Coordinate[] coords = new Coordinate[4];
                            for (int[] nodes : surface.getTriangleNodes()) {

                                for (int j = 0; j < 3; j++) {
                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                }
                                coords[3] = coords[0];//Close ring
                                AreaPainting ap = new AreaPainting(id, chTrianglesGrid, gf.createLinearRing(coords));
                                mapViewer.addPaintInfoToLayer(layerSurfaceGrid, ap);
                                id++;
                                if (id > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            }
                        }
                    } catch (TransformException ex) {
                        Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else if (surfaceShow == SURFACESHOW.ANALYSISRASTER) {
                if (surface == null || surface.getMeasurementRaster() == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                mapViewer.clearLayer(layerSurfaceMeasurementRaster);

                int id = 0;
                SurfaceMeasurementRaster raster = surface.getMeasurementRaster();
                if (raster instanceof SurfaceMeasurementRectangleRaster) {
                    SurfaceMeasurementRectangleRaster rectRaster = (SurfaceMeasurementRectangleRaster) raster;
                    if (drawTrianglesAsNodes) {
                        for (int x = 0; x < rectRaster.getNumberXIntervals(); x++) {
                            for (int y = 0; y < rectRaster.getNumberYIntervals(); y++) {
                                try {
                                    Coordinate longlat = surface.getGeotools().toGlobal(rectRaster.getMidCoordinate(x, y), true);
                                    NodePainting np = new NodePainting(x * rectRaster.getNumberYIntervals() + y, longlat, chCellMeasurements);
                                    mapViewer.addPaintInfoToLayer(layerSurfaceMeasurementRaster, np);
                                } catch (TransformException ex) {
                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    } else {
                        for (int x = 0; x < rectRaster.getNumberXIntervals(); x++) {
                            for (int y = 0; y < rectRaster.getNumberYIntervals(); y++) {

                                try {
                                    Coordinate lowleft = new Coordinate(rectRaster.getXmin() + x * rectRaster.getxIntervalWidth(), rectRaster.getYmin() + y * rectRaster.getYIntervalHeight());
                                    Coordinate topRight = new Coordinate(lowleft.x + rectRaster.getxIntervalWidth(), lowleft.y + rectRaster.getYIntervalHeight());
                                    Coordinate topleft = new Coordinate(lowleft.x, topRight.y);
                                    Coordinate lowRight = new Coordinate(topRight.x, lowleft.y);

                                    Coordinate llll = surface.getGeotools().toGlobal(lowleft, true);
                                    Coordinate lltr = surface.getGeotools().toGlobal(topRight, true);
                                    Coordinate lltl = surface.getGeotools().toGlobal(topleft, true);
                                    Coordinate lllr = surface.getGeotools().toGlobal(lowRight, true);

                                    LinearRing ring = gf.createLinearRing(new Coordinate[]{llll, lltl, lltr, lllr, llll});
                                    AreaPainting ap = new AreaPainting(x * rectRaster.getNumberYIntervals() + y, chCellMeasurements, ring);
                                    mapViewer.addPaintInfoToLayer(layerSurfaceMeasurementRaster, ap);
                                } catch (TransformException ex) {
                                    Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }

                    mapViewer.recalculateShapes();
                    mapViewer.recomputeLegend();
                } else if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                    if (drawTrianglesAsNodes) {
                        for (double[] triangleMid : surface.getTriangleMids()) {
                            try {
                                NodePainting np = new NodePainting(id, surface.getGeotools().toGlobal(new Coordinate(triangleMid[0], triangleMid[1])), chCellMeasurements);
                                id++;
                                mapViewer.addPaintInfoToLayer(layerSurfaceMeasurementRaster, np);
                                if (id > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            } catch (TransformException ex) {
                                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    } else {
                        id = 0;
                        Coordinate[] coords = new Coordinate[4];
                        Layer layer = mapViewer.getLayer(layerSurfaceMeasurementRaster);
                        AreaPainting[] aps = new AreaPainting[Math.min(surface.getTriangleNodes().length, maximumNumberOfSurfaceShapes)];
                        for (int[] nodes : surface.getTriangleNodes()) {
                            try {
                                for (int j = 0; j < 3; j++) {
                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                }
                                coords[3] = coords[0];//Close ring
                                AreaPainting ap = new AreaPainting(id, chCellMeasurements, gf.createLinearRing(coords));
                                aps[id] = ap;
                                id++;
                            } catch (TransformException ex) {
                                Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if (id >= maximumNumberOfSurfaceShapes) {
                                break;
                            }
                        }
                        if (layer == null) {
                            layer = new Layer(layerSurfaceMeasurementRaster, chCellMeasurements);
                            mapViewer.getLayers().add(layer);
                        }
                        layer.setPaintElements(aps);
                    }
                }

            } else if (surfaceShow == SURFACESHOW.SPECTRALMAP) {
                try {
                    if (surface.getMeasurementRaster() != null) {
                        if (surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                            synchronized (surface.getMeasurementRaster()) {
                                SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();
                                if (raster.getMeasurements() != null) {
                                    for (int mid = 0; mid < raster.getMeasurements().length; mid++) {
                                        TriangleMeasurement measurement = raster.getMeasurements()[mid];
                                        if (measurement == null || measurement.getParticlecount() == null || measurement.getParticlecount().length == 0) {
                                            continue;
                                        }

                                        int red = 0;
                                        int green = 0;
                                        int blue = 0;
                                        if (measurement.getParticlecount()[0] != null) {
                                            for (int j = 0; j < measurement.getParticlecount()[0].length; j++) {
                                                if (measurement.getParticlecount()[0][j] > 0) {
                                                    red = 255;
                                                    break;
                                                }
                                            }
                                        }
                                        if (measurement.getParticlecount().length > 1) {
                                            if (measurement.getParticlecount()[1] != null) {
                                                for (int j = 0; j < measurement.getParticlecount()[1].length; j++) {
                                                    if (measurement.getParticlecount()[1][j] > 0) {
                                                        green = 255;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        if (measurement.getParticlecount().length > 2) {
                                            if (measurement.getParticlecount()[2] != null) {
                                                for (int j = 0; j < measurement.getParticlecount()[2].length; j++) {
                                                    if (measurement.getParticlecount()[2][j] > 0) {
                                                        blue = 255;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        Color color = new Color(red, green, blue);

                                        if (drawTrianglesAsNodes) {
                                            NodePainting np = new NodePainting(mid, surface.getGeotools().toGlobal(new Coordinate(surface.getTriangleMids()[mid][0], surface.getTriangleMids()[mid][1])), new ColorHolder(color));
                                            np.setRadius(2);
                                            mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, np);
                                        } else {
                                            //Convert Coordinates
                                            try {
                                                int[] nodes = surface.getTriangleNodes()[mid];
                                                Coordinate[] coords = new Coordinate[4];
                                                for (int j = 0; j < 3; j++) {
                                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                                }
                                                coords[3] = coords[0];//Close ring
                                                AreaPainting ap = new AreaPainting(mid, new DoubleColorHolder(color, color, "Contamination"), gf.createLinearRing(coords));
                                                mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                            } catch (Exception exception) {
                                                System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + mid);
                                                System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                                throw exception;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (surface.getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
                            SurfaceMeasurementRectangleRaster raster = (SurfaceMeasurementRectangleRaster) surface.getMeasurementRaster();
                            for (int x = 0; x < raster.getNumberXIntervals(); x++) {
                                if (raster.getParticlecounter()[x] == null) {
                                    continue;
                                }
                                for (int y = 0; y < raster.getNumberYIntervals(); y++) {
                                    if (raster.getParticlecounter()[x][y] == null) {
                                        continue;
                                    }
                                    int red = 0;
                                    int green = 0;
                                    int blue = 0;

                                    for (int m = 0; m < raster.getNumberOfMaterials(); m++) {
                                        int particlesum = 0;
                                        for (int t = 0; t < raster.getNumberOfTimes(); t++) {
                                            particlesum += raster.getParticlecounter()[x][y][t][m];
                                        }
                                        if (particlesum > 0 && m == 0) {
                                            red = 255;
                                        }
                                        if (particlesum > 0 && m == 1) {
                                            green = 255;
                                        }
                                        if (particlesum > 0 && m == 2) {
                                            blue = 255;
                                        }

                                    }

                                    Color color = new Color(red, green, blue);

                                    int id = x + y * raster.getNumberXIntervals();
                                    if (drawTrianglesAsNodes) {
                                        Coordinate c = surface.getGeotools().toGlobal(raster.getMidCoordinate(x, y));
                                        NodePainting np = new NodePainting(id, c, new ColorHolder(color));
                                        np.setRadius(2);
                                        mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, np);

                                    } else {
                                        //Convert Coordinates
                                        try {
//                                            int[] nodes = surface.getTriangleNodes()[i];
                                            Coordinate[] coordsUTM = raster.getRectangleBound(x, y);
                                            Coordinate[] coordsWGS = new Coordinate[5];
                                            for (int j = 0; j < 4; j++) {
                                                coordsWGS[j] = surface.getGeotools().toGlobal(coordsUTM[j]);
                                            }
                                            coordsWGS[4] = coordsWGS[0];//Close ring
                                            AreaPainting ap = new AreaPainting(id, new DoubleColorHolder(color, color, "Contamination"), gf.createLinearRing(coordsWGS));
                                            mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                        } catch (Exception exception) {
                                            System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + id);
                                            System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                            throw exception;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (surfaceShow == SURFACESHOW.HEATMAP_LIN || surfaceShow == SURFACESHOW.HEATMAP_LOG || surfaceShow == SURFACESHOW.HEATMAP_LIN_BAGATELL) {
                try {
                    Layer layer = mapViewer.getLayer(layerSurfaceContaminated);
                    if (layer == null) {
                        layer = new Layer(layerSurfaceContaminated, chSurfaceHeatMap);
                        mapViewer.getLayers().add(layer);
                    }
                    if (surface.getMeasurementRaster() != null && surface.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                        SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surface.getMeasurementRaster();
                        synchronized (raster) {
                            double totalmass = 0;
                            for (InjectionInfo injection : control.getScenario().getInjections()) {
                                totalmass += injection.getMass();//getNumberOfParticles();//getMass();//
                            }
                            boolean logarithmic = surfaceShow == SURFACESHOW.HEATMAP_LOG;
                            /**
                             * count of particles per triangle exceeding this
                             * number will get high color shapes. used to scale
                             * the color between low color to max color.
                             */
                            double highColorCount = 1 * totalmass;
                            if (logarithmic) {
                                highColorCount = Math.log10(totalmass) + 5;
                            }

                            double totalmeasurements = 0;
                            for (int i = 0; i < raster.measurementTimestamp.length; i++) {
                                totalmeasurements += raster.measurementTimestamp[i];
                            }
                            if (totalmeasurements < 1) {
                                System.err.println("No measurements.");
                                return;
                            }

                            double bagatell = (0.00001 * totalmass);
                            int nbMaterials = raster.getNumberOfMaterials();
                            int nbtimes = raster.measurementTimestamp.length;
                            double duration = 0;
                            for (int i = 0; i < raster.durationInTimeinterval.length; i++) {
                                duration += raster.durationInTimeinterval[i];
                            }
                            double normalisation = 1.0 / (double) duration;
//                        System.out.println("Total duration of "+duration+" = "+(duration/60)+" min = "+(duration/3600)+"h");

                            for (int i = 0; i < raster.getMeasurements().length; i++) {
                                TriangleMeasurement triangleMeasurement = raster.getMeasurements()[i];
                                if (triangleMeasurement == null) {
                                    continue;
                                }
                                double cellMass = 0;
                                double timesum = 0;
                                for (int t = 0; t < nbtimes; t++) {
                                    timesum = 0;
                                    for (int m = 0; m < nbMaterials; m++) {
                                        timesum += triangleMeasurement.getMassResidence()[m][t];
                                    }
                                    if (surfaceShow == SURFACESHOW.HEATMAP_LIN_BAGATELL) {
                                        if (timesum <= bagatell) {
                                            continue;
                                        }
                                    }
                                    cellMass += timesum;
                                }
                                if (cellMass == 0) {
                                    continue;
                                }
                                final double frac;
                                if (logarithmic) {
                                    frac = (Math.log10(cellMass * normalisation) + 5) / highColorCount;
                                } else {
                                    //Linear
                                    frac = cellMass * normalisation;
                                }

                                if (drawTrianglesAsNodes) {
                                    Coordinate c = surface.getGeotools().toGlobal(new Coordinate(surface.getTriangleMids()[i][0], surface.getTriangleMids()[i][1]));

                                    NodePainting np = new NodePainting(i, c, chSurfaceHeatMap) {
                                        @Override
                                        public boolean paint(Graphics2D g2) {

                                            g2.setColor(chSurfaceHeatMap.getGradientColor(frac));

                                            return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                                        }
                                    };
//                                    NodePainting np = new NodePainting(i, tri.getPosition3D(0).lonLatCoordinate(), new ColorHolder(color));
                                    np.setRadius(2);
                                    layer.add(np, false);
//                                mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, np);

                                } else {
                                    //Convert Coordinates
                                    try {
                                        int[] nodes = surface.getTriangleNodes()[i];
                                        Coordinate[] coords = new Coordinate[4];
                                        for (int j = 0; j < 3; j++) {
                                            coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                        }
                                        coords[3] = coords[0];//Close ring
                                        AreaPainting ap = new AreaPainting(i, chSurfaceHeatMap, gf.createLinearRing(coords)) {

                                            @Override
                                            public boolean paint(Graphics2D g2) {
                                                try {
                                                    g2.setColor(chSurfaceHeatMap.getGradientColor(frac));
                                                    g2.fill(this.getOutlineShape());
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                                return true;
                                            }

                                        };
                                        layer.add(ap, false);
//                                    mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                    } catch (Exception exception) {
                                        System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + i);
                                        System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                        throw exception;
                                    }
                                }
                            }
                        }
//                    }
                    } else if (surface.getMeasurementRaster() != null && surface.getMeasurementRaster() instanceof SurfaceMeasurementRectangleRaster) {
                        SurfaceMeasurementRectangleRaster raster = (SurfaceMeasurementRectangleRaster) surface.getMeasurementRaster();
                        synchronized (raster) {
                            double totalmass = 0;
                            for (InjectionInfo injection : control.getScenario().getInjections()) {
                                totalmass += injection.getMass();//getNumberOfParticles();//getMass();//
                            }
                            boolean logarithmic = surfaceShow == SURFACESHOW.HEATMAP_LOG;
                            /**
                             * count of particles per triangle exceeding this
                             * number will get high color shapes. used to scale
                             * the color between low color to max color.
                             */
                            double highColorCount = 1 * totalmass;
                            if (logarithmic) {
                                highColorCount = Math.log10(totalmass) + 5;
                            }

                            double totalmeasurements = 0;
                            for (int i = 0; i < raster.measurementTimestamp.length; i++) {
                                totalmeasurements += raster.measurementTimestamp[i];
                            }
                            if (totalmeasurements < 1) {
                                System.err.println("No measurements.");
                                return;
                            }

                            double bagatell = (0.00001 * totalmass);
                            double duration = 0;
                            for (int i = 0; i < raster.durationInTimeinterval.length; i++) {
                                duration += raster.durationInTimeinterval[i];
                            }
                            double normalisation = 1.0 / (double) duration;

//                        double timeScale = ThreadController.getDeltaTimeMS() / (surface.getTimes().getDeltaTimeMS() / 1000.);
                            for (int x = 0; x < raster.getNumberXIntervals(); x++) {
                                if (raster.getParticlecounter()[x] == null) {
                                    continue;
                                }
                                for (int y = 0; y < raster.getNumberYIntervals(); y++) {
                                    if (raster.getParticlecounter()[x][y] == null) {
                                        continue;
                                    }
                                    double massum = 0;
                                    for (int t = 0; t < raster.getNumberOfTimes(); t++) {
                                        for (int m = 0; m < raster.getNumberOfMaterials(); m++) {
                                            massum += raster.getMass()[x][y][t][m];
                                        }
                                    }
                                    if (massum <= bagatell) {
                                        continue;
                                    }
                                    final double frac;
                                    if (massum == 0) {
                                        continue;
                                    } else {

                                        if (logarithmic) {
                                            frac = (Math.log10(massum * normalisation) + 5) / highColorCount;
                                        } else {
                                            //Linear
                                            frac = massum * normalisation;
                                        }
                                    }
                                    int id = x + y * raster.getNumberXIntervals();
                                    if (drawTrianglesAsNodes) {
                                        Coordinate c = surface.getGeotools().toGlobal(raster.getMidCoordinate(x, y));
                                        NodePainting np = new NodePainting(id, c, chSurfaceHeatMap) {
                                            @Override
                                            public boolean paint(Graphics2D g2) {

                                                g2.setColor(chSurfaceHeatMap.getGradientColor(frac));

                                                return super.paint(g2);
                                            }
                                        };
                                        np.setRadius(2);
                                        layer.add(np, false);

                                    } else {
                                        //Convert Coordinates
                                        try {
                                            Coordinate[] coordsUTM = raster.getRectangleBound(x, y);
                                            Coordinate[] coordsWGS = new Coordinate[5];
                                            for (int j = 0; j < 4; j++) {
                                                coordsWGS[j] = surface.getGeotools().toGlobal(coordsUTM[j]);
                                            }
                                            coordsWGS[4] = coordsWGS[0];//Close ring
                                            AreaPainting ap = new AreaPainting(id, chSurfaceHeatMap, gf.createLinearRing(coordsWGS)) {
                                                @Override
                                                public boolean paint(Graphics2D g2) {
                                                    g2.setColor(chSurfaceHeatMap.getGradientColor(frac));
                                                    g2.draw(outlineShape);
                                                    g2.fill(outlineShape);
                                                    return true;
                                                }
                                            };
                                            layer.add(ap, false);
//                                            mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                                        } catch (Exception exception) {
                                            System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + id);
                                            System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                            throw exception;
                                        }
                                    }

                                }
                            }
                        }
                    }

                    mapViewer.recalculateShapes();
                    mapViewer.recomputeLegend();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (surfaceShow == SURFACESHOW.CONTAMINATIONCLUSTER) {
                if (surface == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {
                    ArrayList<Geometry> list = ShapeTools.createShapesWGS84(surface, 10, 5, -1, 1);
                    int i = 0;
                    if (list != null) {
                        for (Geometry g : list) {
                            AreaPainting ap = new AreaPainting(i, chTrianglesContaminated, g);
                            mapViewer.addPaintInfoToLayer(layerSurfaceContaminated, ap);
                            i++;
                        }
                    }
                    mapViewer.recalculateShapes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (surfaceShow == SURFACESHOW.WATERLEVEL) {
                if (surface.getTimes() == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {
                    if (shownSurfaceTriangles == null) {
                        surfaceShow = SURFACESHOW.NONE;
                        return;
                    }
                    Layer layer = mapViewer.getLayer(layerSurfaceWaterlevel);
                    if (layer != null && !layer.isEmpty()) {
                        //No need to update the layer if it already exists.
                        continue;
                    }

                    new Thread("Create layer surfacewaterlevels") {

                        @Override
                        public void run() {
                            int count = 0;
                            GeometryFactory gf = new GeometryFactory();
                            Layer layer = mapViewer.getLayer(layerSurfaceWaterlevel);
                            if (layer == null) {
                                layer = new Layer(layerSurfaceWaterlevel, chTrianglesWaterlevel);
                                mapViewer.getLayers().add(layer);
                            }
                            System.out.println("start Thread adding up to " + surface.getTriangleMids().length + " waterlevel shapes. " + (showSurfaceTriangle != null ? "triangleShow initialized" + shownSurfaceTriangles.length : " no shown triangle information") + "  " + (surface.getMaxWaterlvl() != null ? "hasMaxLevels" : "noMaxLevels"));

//                            for (int i = 0; i < shownSurfaceTriangles.length; i++) {
//                            if (showSurfaceTriangle != null && showSurfaceTriangle[i] == false) {
//                                continue; //do not draw what is not displayed
//                            }
//                                final int idt = shownSurfaceTriangles[i];
//                                if (surface.getMaxWaterLevels() != null && surface.getMaxWaterLevels()[idt] < 0.001) {
//                                    continue;
//                                }
                            for (int i = 0; i < surface.triangleNodes.length; i++) {
                                final int idt = i;
                                count++;
                                if (surface.getMaxWaterLevels() != null && surface.getMaxWaterLevels()[idt] < 0.001) {
                                    //DO not show water levels of less than 1 mm. Skip a cell if this is never reached.
                                    continue;
                                }
                                if (drawTrianglesAsNodes) {
                                    double[] pos = surface.getTriangleMids()[idt];
                                    NodePainting np;
                                    try {
                                        np = new NodePainting(idt, surface.getGeotools().toGlobal(new Coordinate(pos[0], pos[1])), chTrianglesWaterlevel) {

                                            @Override
                                            public boolean paint(Graphics2D g2) {
//                                                if (showSurfaceTriangle != null && showSurfaceTriangle[idt] == false) {
//                                                    return false; //do not draw what is not displayed
//                                                }
                                                int t = surface.getTimes().getTimeIndex(timeToShow/*ArrayTimeLinePipeContainer.instance.getActualTime()*/);
                                                try {
                                                    float[] lvls = surface.getWaterlevels()[idt];
                                                    if (lvls == null) {
                                                        lvls = surface.loadWaterlevels(idt);
                                                    }
                                                    float lvl = lvls[t];
                                                    if (lvl < 0.001f) {
                                                        return false;
                                                    }
//                                                System.out.println("level: " + lvl);
                                                    g2.setColor(interpolateColor(chTrianglesWaterlevel.getColor(), chTrianglesWaterlevel.getFillColor(), lvl / 0.5));
                                                    return super.paint(g2);
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                            }

                                        };
                                        np.setRadius(2);
                                        mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, np);
                                    } catch (TransformException ex) {
                                        Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                                    }

                                } else {
                                    //Convert Coordinates
                                    int[] nodes = null;
                                    try {
                                        nodes = surface.getTriangleNodes()[idt];
                                        Coordinate[] coords = new Coordinate[4];
                                        for (int j = 0; j < 3; j++) {
                                            coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                        }
                                        coords[3] = coords[0];//Close ring
                                        AreaPainting ap = new AreaPainting(idt, chTrianglesWaterlevel, gf.createLinearRing(coords)) {
                                            @Override
                                            public boolean paint(Graphics2D g2) {
                                                if (outlineShape == null) {
                                                    return false;
                                                }
                                                int t = surface.getTimes().getTimeIndex(timeToShow/*ArrayTimeLinePipeContainer.instance.getActualTime()*/);
                                                float lvl = 0;
                                                try {
                                                    lvl = surface.getWaterlevels()[idt][t];
                                                } catch (Exception e) {
                                                    try {
                                                        lvl = surface.loadWaterlevels(idt)[t];
                                                    } catch (Exception ex) {
                                                        System.err.println(ex.getLocalizedMessage());
                                                        //Could not find waterlevel information in file? add zero values
                                                        surface.getWaterlevels()[idt] = new float[surface.getNumberOfTimes()];
                                                        lvl = 0f;
                                                    }
                                                }
                                                if (lvl < 0.01) {
                                                    return false;
                                                }
                                                g2.setColor(interpolateColor(chTrianglesWaterlevel.getColor(), chTrianglesWaterlevel.getFillColor(), lvl / 0.5));
                                                g2.fill(outlineShape);
                                                return true;
//                                    return super.paint(g2);
                                            }
                                        };
                                        mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, ap);
                                    } catch (Exception exception) {
                                        System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + i);

                                        System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                        System.err.println("Vertices= {" + nodes[0] + " , " + nodes[1] + "," + nodes[2]);
                                        System.err.println("number of vertices: " + surface.getVerticesPosition().length);
//                                    throw exception;
                                    }
                                }
                                if (count > maximumNumberOfSurfaceShapes) {
                                    break;
                                }
                            }
                            System.out.println("add Surface shapes waterlevel: " + count);
                        }

                    }.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                mapViewer.recalculateShapes();

                mapViewer.recomputeLegend();

            } else if (surfaceShow == SURFACESHOW.WATERLEVELMAX) {
                if (surface.getTimes() == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {

                    double[] lvls = surface.getMaxWaterLevels();
                    if (lvls == null || lvls.length == 0) {
                        surfaceShow = SURFACESHOW.NONE;
                        return;
                    }
                    System.out.println("add Surface shapes max waterlevels: " + lvls.length);
                    for (int i = 0; i < lvls.length; i++) {

                        final double lvl = lvls[i];
                        if (lvl < 0.02) {
                            continue;
                        }
                        final int idt = i;
                        if (drawTrianglesAsNodes) {
                            double[] pos = surface.getTriangleMids()[i];
                            NodePainting np = new NodePainting(i, surface.getGeotools().toGlobal(new Coordinate(pos[0], pos[1])), chTrianglesWaterlevel) {

                                @Override
                                public boolean paint(Graphics2D g2) {
                                    int t = surface.getTimes().getTimeIndex(timeToShow/*ArrayTimeLinePipeContainer.instance.getActualTime()*/);
                                    try {
                                        g2.setColor(interpolateColor(chTrianglesWaterlevel.getColor(), chTrianglesWaterlevel.getFillColor(), lvl / 0.5));
                                        return super.paint(g2);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }

                            };
                            np.setRadius(2);
                            mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, np);
                        } else {
                            //Convert Coordinates
                            int[] nodes = null;
                            try {
                                nodes = surface.getTriangleNodes()[i];
                                Coordinate[] coords = new Coordinate[4];
                                for (int j = 0; j < 3; j++) {
                                    coords[j] = surface.getGeotools().toGlobal(new Coordinate(surface.getVerticesPosition()[nodes[j]][0], surface.getVerticesPosition()[nodes[j]][1]));
                                }
                                coords[3] = coords[0];//Close ring
                                AreaPainting ap = new AreaPainting(i, chTrianglesWaterlevel, gf.createLinearRing(coords)) {
                                    @Override
                                    public boolean paint(Graphics2D g2) {
                                        if (outlineShape == null) {
                                            return false;
                                        }
                                        g2.setColor(interpolateColor(chTrianglesWaterlevel.getColor(), chTrianglesWaterlevel.getFillColor(), lvl / 0.5));
                                        g2.fill(outlineShape);
                                        return true;
//                                    return super.paint(g2);
                                    }
                                };
                                mapViewer.addPaintInfoToLayer(layerSurfaceWaterlevel, ap);
                            } catch (Exception exception) {
                                System.err.println("Exception in " + getClass() + "::addSurafcePaint for triangle:" + i);

                                System.err.println("number of triangles: " + surface.getTriangleNodes().length);
                                System.err.println("Vertices= {" + nodes[0] + " , " + nodes[1] + "," + nodes[2]);
                                System.err.println("number of vertices: " + surface.getVerticesPosition().length);
                                throw exception;
                            }
                        }

                        double[] p = surface.getTriangleMids()[i];
                        Coordinate longlat = surface.getGeotools().toGlobal(new Coordinate(p[0], p[1]));
                        LabelPainting lp = new LabelPainting(i, MapViewer.COLORHOLDER_LABEL, new com.saemann.rgis.model.GeoPosition(longlat), 20, -5, -5, df3.format(lvl)) {

                            @Override
                            public boolean paint(Graphics2D g2) {
                                if (mapViewer.getZoom() < this.getMinZoom()) {
                                    return false;
                                }
                                return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                            }

                        };
                        lp.setCoronaBackground(true);
                        mapViewer.addPaintInfoToLayer(layerLabelWaterlevel, lp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mapViewer.recalculateShapes();
                mapViewer.recomputeLegend();
            } else if (surfaceShow == SURFACESHOW.PARTICLETRACE) {
                int numberOfTracerParticles = 0;
                mapViewer.clearLayer(layerHistoryPath);
                for (Particle p : control.getThreadController().getParticles()) {
                    if (p == null) {
                        continue;
                    }
                    if (p.tracing()) {
                        numberOfTracerParticles++;
                    }
                }

                if (numberOfTracerParticles == 0) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {
                    for (Particle p : control.getThreadController().getParticles()) {
                        if (p == null || !p.tracing()) {
                            continue;
                        }
                        HistoryParticle hp = (HistoryParticle) p;
                        if (hp.getPositionTrace().size() < 2) {
                            continue;
                        }
                        ArrowPainting ap = new ArrowPainting(p.getId(), hp.getPositionTrace().toArray(new Coordinate[hp.getPositionTrace().size()]), chTravelPath);
                        mapViewer.addPaintInfoToLayer(layerHistoryPath, ap);
                    }
                    mapViewer.recalculateShapes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (surfaceShow == SURFACESHOW.PARTICLETRACE_OUTLET) {
                int numberOfTracerParticles = 0;
                mapViewer.clearLayer(layerHistoryPath);
                if (!layersHistoryPathToOutlets.isEmpty()) {
                    for (String layerkey : layersHistoryPathToOutlets) {
                        this.mapViewer.clearLayer(layerkey);
                    }
                }
                for (Particle p : control.getThreadController().getParticles()) {
                    if (p == null) {
                        continue;
                    }
                    if (p.tracing()) {
                        numberOfTracerParticles++;
                    }
                }

                if (numberOfTracerParticles == 0) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {
                    Random rand = new Random(0);
                    for (Particle p : control.getThreadController().getParticles()) {
                        if (p == null || !p.tracing()) {
                            continue;
                        }
                        HistoryParticle hp = (HistoryParticle) p;
                        if (hp.getPositionTrace().size() < 2) {
                            continue;
                        }

                        ColorHolder ch = chTravelPath;
                        String key = layerHistoryPath;

                        if (hp.getHistory().size() > 0) {
                            if (hp.getHistory().getLast().isSetAsOutlet()) {
                                String name = hp.getHistory().getLast().getName();
                                if (colorMap.containsKey(name)) {
                                    ch = colorMap.get(name);
                                    key = ch.getDescription();

                                } else {
                                    key = "Trace to " + name;
                                    ch = new ColorHolder(new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255), 50), key);
                                    ch.setStroke(chTravelPath.getStroke());
                                    colorMap.put(name, ch);
//                                System.out.println("new color for "+ch.getDescription());
                                }
                                if (!layersHistoryPathToOutlets.contains(key)) {
                                    layersHistoryPathToOutlets.add(key);
                                }
                            }
                        }

                        ArrowPainting ap = new ArrowPainting(p.getId(), hp.getPositionTrace().toArray(new Coordinate[hp.getPositionTrace().size()]), ch);
                        mapViewer.addPaintInfoToLayer(key, ap);
                    }
                    mapViewer.recalculateShapes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (surfaceShow == SURFACESHOW.VELOCITY) {
                //Show Arrows of velocity
                if (surface.getTimes() == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                float velocityFactor = 10f;
                try {
                    mapViewer.clearLayer(layerArrow);
                    mapViewer.clearLayer(layerLabelWaterlevel);
                    double t = surface.getTimes().getTimeIndexDouble(timeToShow);//ex(timeToShow/*ArrayTimeLinePipeContainer.instance.getActualTime()*/);
                    int ti = (int) t;
                    float frac = (float) (t % 1);
                    if (ti >= surface.getNumberOfTimes() - 1) {
                        ti = surface.getNumberOfTimes() - 2;
                        frac = 1;
                    }
//                System.out.println("show velocity timeindex " + t);
//                int counter = 0;
                    if (surface.getTriangleVelocity() != null) {
                        for (int i = 0; i < surface.triangleNodes.length; i++) {
                            if (showSurfaceTriangle != null && showSurfaceTriangle[i] == false) {

                                continue;
                            }
                            //Triangle Arrow
                            //on triangle
                            if (surface.getTriangleVelocity()[i] == null) {
                                continue;
                            }
                            float[] v = surface.getTriangleVelocity()[i][ti];
                            float[] v2 = surface.getTriangleVelocity()[i][ti + 1];
                            if (Math.abs(v[0]) < 0.0001 && Math.abs(v[1]) < 0.0001 && Math.abs(v2[0]) < 0.0001) {
                                continue;
                            }
                            float vx = v[0] + (v2[0] - v[0]) * frac;
                            float vy = v[1] + (v2[1] - v[1]) * frac;

                            double[] mid = surface.getTriangleMids()[i];
                            Coordinate midPoint = surface.getGeotools().toGlobal(new Coordinate(mid[0], mid[1]));
                            Coordinate vTtarget = surface.getGeotools().toGlobal(new Coordinate(mid[0] + velocityFactor * vx, mid[1] + velocityFactor * vy));
                            ArrowPainting av = new ArrowPainting(i, new Coordinate[]{midPoint, vTtarget}, chSurfaceVelocity);
                            mapViewer.addPaintInfoToLayer(layerArrow, av);
                        }
                    } else {
                        System.out.println("trianglevelocity is null");
                    }
                    if (false) {
                        if (surface.isNeighbourVelocityLoaded()) {
                            for (int i = 0; i < surface.triangleNodes.length; i++) {
                                for (int n = 0; n < 3; n++) {

                                    final int idt = i;
                                    final int nb = surface.getNeighbours()[i][n];
                                    if (nb < 0) {
                                        continue;
                                    }
                                    //Triangle Arrow
                                    //on triangle
                                    float v = 0;
                                    try {
                                        v = surface.getVelocityToNeighbour(ti, i, n);
                                    } catch (Exception e) {
                                        System.err.println("Could not Load velocity for traingle " + i + " to neighbour " + n);
                                        continue;
                                    }
                                    if (Math.abs(v) < 0.1) {
                                        continue;
                                    }
                                    double[] midT = surface.getTriangleMids()[i];
                                    double[] midN = surface.getTriangleMids()[nb];
                                    //Build arrwos 
                                    float[] dTN = new float[]{(float) (midN[0] - midT[0]), (float) (midN[1] - midT[1])};
                                    //scale to length1
                                    // WL arrow on mid of link
                                    float length = (float) Math.sqrt(dTN[0] * dTN[0] + dTN[1] * dTN[1]);
                                    dTN[0] /= length;
                                    dTN[1] /= length;

                                    double startX = midT[0] * 0.5 + midN[0] * 0.5;
                                    double startY = midT[1] * 0.5 + midN[1] * 0.5;

                                    Coordinate wlStart = surface.getGeotools().toGlobal(new Coordinate(startX, startY));
                                    Coordinate vTtarget = surface.getGeotools().toGlobal(new Coordinate(startX + velocityFactor * v * dTN[0], startY + velocityFactor * v * dTN[1]));
                                    ArrowPainting av = new ArrowPainting(i, new Coordinate[]{wlStart, vTtarget}, chDirectionPRO);
                                    mapViewer.addPaintInfoToLayer(layerHistory, av);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mapViewer.recalculateShapes();
                mapViewer.recomputeLegend();
            } else if (surfaceShow == SURFACESHOW.SLOPE) {
                //Show Arrows of velocity
                if (surface.triangle_downhilldirection == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                float slopeFactor = 2f;
                try {
                    mapViewer.clearLayer(layerArrow);
                    for (int i = 0; i < surface.triangle_downhilldirection.length; i++) {
                        try {

                            //Triangle Arrow
                            //on triangle
                            if (surface.triangle_downhilldirection[i] == null) {
                                continue;
                            }
                            float[] v = surface.triangle_downhilldirection[i];
                            if (v[0] == 0 && v[1] == 0) {
                                continue;
                            }
                            double[] mid = surface.getTriangleMids()[i];
                            Coordinate midPoint = surface.getGeotools().toGlobal(new Coordinate(mid[0], mid[1]));
                            Coordinate vTtarget = surface.getGeotools().toGlobal(new Coordinate(mid[0] + slopeFactor * v[0], mid[1] + slopeFactor * v[1]));
                            ArrowPainting av = new ArrowPainting(i, new Coordinate[]{midPoint, vTtarget}, chSurfaceVelocity);
                            mapViewer.addPaintInfoToLayer(layerArrow, av);
                        } catch (Exception exception) {
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mapViewer.recalculateShapes();
                mapViewer.recomputeLegend();
            } else if (surfaceShow == SURFACESHOW.VERTEX_HEIGHT) {
                //Show Arrows of velocity
                if (surface.vertices == null) {
                    surfaceShow = SURFACESHOW.NONE;
                    return;
                }
                try {
                    mapViewer.clearLayer(layerArrow);
                    for (int i = 0; i < surface.getTriangleNodes().length; i++) {
                        for (int j = 0; j < 3; j++) {

                            int id = surface.getTriangleNodes()[i][j];
                            try {
                                double[] mid = surface.vertices[id];
                                Coordinate midPoint = surface.getGeotools().toGlobal(new Coordinate(mid[0], mid[1]));
                                LabelPainting av = new LabelPainting(id, chSpillover, new com.saemann.rgis.model.GeoPosition(midPoint), new String[]{mid[2] + ""});
                                mapViewer.addPaintInfoToLayer(layerLabelWaterlevel, av);
                            } catch (Exception exception) {
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        mapViewer.recalculateShapes();
        mapViewer.recomputeLegend();
    }

    public void setLayerPipesVisible(boolean pipesVisible) {
        showPipes = pipesVisible;
        if (pipesVisible) {
            for (Pipe pipe : network.getPipes()) {
                Coordinate[] list = new Coordinate[2];
                list[0] = pipe.getFlowInletConnection().getPosition().lonLatCoordinate();
                list[1] = pipe.getFlowOutletConnection().getPosition().lonLatCoordinate();
                LinePainting lp = new LinePainting(pipe.getAutoID(), list, chPipes);
                mapViewer.addPaintInfoToLayer(layerPipes, lp);
            }
        } else {
            mapViewer.clearLayer(layerPipes);
        }
        mapViewer.repaint();
    }

    /**
     * Splits the particles in two layers: surface / network particles with
     * individual colors and legend entries.
     */
    public void orderParticlesPainting() {
        Layer surfaceLayer = mapViewer.getLayer(layerParticleSurface);
        Layer networkLayer = mapViewer.getLayer(layerParticleNetwork);
        Layer allparticles = mapViewer.getLayer(layerParticle);
        if (allparticles != null) {
            allparticles.setVisibleInLegende(false);
            allparticles.setVisibleInMap(false);
        }
        if (surfaceLayer == null) {
            surfaceLayer = new Layer(layerParticleSurface, chParticlesSurface);
            mapViewer.getLayers().add(surfaceLayer);
            mapViewer.recomputeLegend();
            surfaceLayer.setVisibleInLegende(true);
            surfaceLayer.setVisibleInMap(true);
        }

        if (networkLayer == null) {
            networkLayer = new Layer(layerParticleNetwork, chParticlesNetwork);
            mapViewer.getLayers().add(networkLayer);
            mapViewer.recomputeLegend();
            networkLayer.setVisibleInLegende(true);
            networkLayer.setVisibleInMap(true);
        }
        int nb_surface = 0;
        int nB_network = 0;
        int positionnull = 0;
        Particle[] ps = control.getThreadController().getParticles();
        if (ps != null) {
            if (arraySurface == null || arraySurface.length != ps.length || surfaceLayer.getElements().length != arraySurface.length) {
                arraySurface = new ParticleNodePainting[ps.length];
                arrayNetwork = new ParticleNodePainting[ps.length];
                networkLayer.setPaintElements(arrayNetwork);//arrayListNetwork.toArray(new ParticleNodePainting[arrayListNetwork.size()]));
                surfaceLayer.setPaintElements(arraySurface);//arrayListSurface.toArray(new ParticleNodePainting[arrayListSurface.size()]));
            }
            double[] tempstorage = new double[2];
            Coordinate tempCoordinate = new Coordinate();
            for (int i = 0; i < particlePaintings.length; i++) {
                ParticleNodePainting np = particlePaintings[i];
                if (np == null) {
                    System.out.println("Nodepainting [" + i + "] is null");
                    break;
                }
                Particle p = ps[i];
                if (p.isActive()) {
                    if (p.getPosition3d() == null) {
                        positionnull++;
//                        System.out.println("Position of particle " + p.getId() + " is null. "+p.status);
                        continue;
                    }
                    if (Double.isNaN(p.getPosition3d().x)) {
                        positionnull++;
//                        System.out.println("Position of particle " + p.getId() + " is NaN. "+p.status+"  cell: "+p.surfaceCellID+" pos: "+p.getPosition3d());
                        continue;
                    }
                    if (p.isOnSurface()) {
                        try {
                            if (np == null) {
                                Coordinate c = new Coordinate();
                                geoToolsSurface.toGlobal(p.getPosition3d(), c, tempstorage, true);
                                ParticleNodePainting pnp = new ParticleNodePainting(p, i, c, chParticlesSurface);
//                                System.out.println("create new chape " + i);
                                particlePaintings[i] = pnp;
                                surfaceLayer.add(pnp);
                                arraySurface[nb_surface] = pnp;
                            } else {
//                                Coordinate newCoord=new Coordinate();
                                geoToolsSurface.toGlobal(p.getPosition3d(), tempCoordinate, tempstorage, true);

//                                System.out.println("new surface LatLong is "+newCoord);
                                np.setColor(chParticlesSurface);
                                np.setLongLat(tempCoordinate.x, tempCoordinate.y);
//                                arrayListSurface.add(np);
//System.out.println("Surface np position is "+np.getRefLongitude()+", "+np.getRefLatitude());
                                arraySurface[nb_surface] = np;
                            }

                            nb_surface++;
                        } catch (ProjectionException pe) {
                        } catch (Exception ex) {
                            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        try {
                            if (p.getSurrounding_actual() == null) {
                                continue;
                            }
                            Position3D pos = p.getSurrounding_actual().getPosition3D(p.getPosition1d_actual());
                            if (np == null) {
                                Coordinate coord = new CoordinateXY(pos.getLongitude(), pos.getLatitude());
                                ParticleNodePainting pnp = new ParticleNodePainting(p, i, coord, chParticlesNetwork);
                                particlePaintings[i] = pnp;
                                networkLayer.add(pnp);
                                arrayNetwork[nB_network] = pnp;
                            } else {
                                //only change coordinate
//                                System.out.println("new pipe LatLong is "+pos);
                                np.setLongLat(pos.getLongitude(), pos.getLatitude());
                                np.setColor(chParticlesNetwork);
                                arrayNetwork[nB_network] = np;
                            }

                            nB_network++;
                        } catch (Exception ex) {
                            Logger.getLogger(PaintManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    np.setColor(null);
                }
            }
        }
    }

    public void initColorHolderArray(ColorHolder[] colorholders, Color low, Color high) {

        for (int i = 0; i < colorholders.length; i++) {
            float red = (low.getRed() * (colorholders.length - i) + high.getRed() * i) / colorholders.length;
            float green = (low.getGreen() * (colorholders.length - i) + high.getGreen() * i) / colorholders.length;
            float blue = (low.getBlue() * (colorholders.length - i) + high.getBlue() * i) / colorholders.length;

            float max = Math.max(red, Math.max(green, blue));
            red = red * 1 / max;
            green = green / max;
            blue = blue / max;
            Color c = new Color(red, green, blue);
            colorholders[i] = new ColorHolder(c);
        }
    }

    public static Color interpolateColor(Color low, Color high, double relative) {
        if (relative <= 0) {
            return low;
        }
        if (relative >= 1) {
            return high;
        }
        int red = (int) (low.getRed() + (high.getRed() - low.getRed()) * relative);
        int green = (int) (low.getGreen() + (high.getGreen() - low.getGreen()) * relative);
        int blue = (int) (low.getBlue() + (high.getBlue() - low.getBlue()) * relative);
        if (low.getAlpha() < 255 || high.getAlpha() < 255) {
            int alpha = (int) (low.getAlpha() + (high.getAlpha() - low.getAlpha()) * relative);
            return new Color(red, green, blue, alpha);
        }
        try {
            return new Color(red, green, blue);
        } catch (Exception e) {
            System.err.println("R:" + red + "  G:" + green + "  B:" + blue);
            e.printStackTrace();
        }
        return low;
    }

    private void initColoHolderConcentration(Color low, Color high) {
        chConcentration = new ColorHolder[101];
        this.initColorHolderArray(chConcentration, low, high);
    }

    private void initColoHolderVelocity(Color low, Color high) {
        chVelocity = new ColorHolder[101];

        initColorHolderArray(chVelocity, low, high);
        for (int i = 0; i < chVelocity.length; i++) {
            chVelocity[i].setDescription("Velocity: " + i + "%");
        }
    }

    private void initColoHolderWaterlevel(Color low, Color high) {
        chWaterlevels = new ColorHolder[10];

        initColorHolderArray(chWaterlevels, low, high);
        for (int i = 0; i < chWaterlevels.length; i++) {
            chWaterlevels[i].setDescription("Lvl: " + i * 10 + "cm");
        }
    }

    private void initColoHolderFillrate(Color low, Color high) {
        chFillrate = new ColorHolder[101];
        int nenner = chFillrate.length;
        for (int i = 0; i < chFillrate.length; i++) {
            Color c = null;
            try {
                c = new Color((low.getRed() * (chFillrate.length - i) + high.getRed() * i) / nenner, (low.getGreen() * (chFillrate.length - i) + high.getGreen() * i) / nenner, (low.getBlue() * (chFillrate.length - i) + high.getBlue() * i) / nenner);

            } catch (Exception e) {
                System.out.println("Red: " + (low.getRed() * (chFillrate.length - i)) + " + " + high.getRed() * i + "= " + ((low.getRed() * (chFillrate.length - i) + high.getRed() * i) / nenner));
                System.out.println("Green: " + (low.getGreen() * (chFillrate.length - i)) + " + " + high.getGreen() * i + "= " + ((low.getGreen() * (chFillrate.length - i) + high.getGreen() * i) / nenner));
                System.out.println("Blue: " + (low.getBlue() * (chFillrate.length - i)) + " + " + high.getBlue() * i + "= " + ((low.getBlue() * (chFillrate.length - i) + high.getBlue() * i) / nenner));
            }
            chFillrate[i] = new ColorHolder(c, "Fillrate: " + i + "%");
        }
    }

    public ColorHolder getColorHolderConcentrationRelative(double relConcentration) {
        int index = (int) (relConcentration * 100);
        index = Math.min(index, 100);
        index = Math.max(index, 0);
//        System.out.println("rel:"+relConcentration+"\t index="+index);
        return chConcentration[index];
    }

    public ColorHolder getColorHolderVelocityRelative(int percentofMaxVelocity) {
        return chVelocity[Math.min(100, Math.max(0, percentofMaxVelocity))];
    }

    public ColorHolder getColorHolderWaterLevelRelative(double heightMeter) {
        return chWaterlevels[Math.min(chWaterlevels.length - 1, (int) Math.max(0, heightMeter / 10))];
    }

    public ColorHolder getColorHolderRelative(int percent) {
        return chVelocity[Math.min(100, Math.max(0, percent))];
    }

//    public NodePainting getParticlePainting(long id) {
//        for (NodePainting particlePainting : particlePaintings) {
//            if (particlePainting.getId() == id) {
//                return particlePainting;
//            }
//        }
//        return null;
//    }
    @Override
    public void selectLocationID(Object o, String string, long id) {
        if (selectedLayer != null && selectedLayer.equals(string) && selectedID == id) {
            return;
        }
        selectedLayer = null;
        com.saemann.rgis.model.GeoPosition mousePoint;
        if (mapViewer.clickPoint != null) {
            mousePoint = new com.saemann.rgis.model.GeoPosition(mapViewer.clickPoint.x, mapViewer.clickPoint.y);
        } else {
            mousePoint = new com.saemann.rgis.model.GeoPosition(mapViewer.getCenter().x, mapViewer.getCenter().y);
        }
        try {
            if (string == null) {
                return;
            }
            if (string.startsWith(layerParticle)) {
                if (id > control.getThreadController().getNumberOfTotalParticles()) {
                    return;
                }
                Particle p = control.getThreadController().getParticles()[(int) id];
                String key = mapViewer.LAYER_KEY_LABEL;
//                        GeoPosition2D position = null;
                String information = "Particle " + p.getId() + ";V = " + df3.format(p.getVelocity1d()) + " m/s;Pos " + df3.format(p.getPosition1d_actual()) + "m;Travel:" + (int) p.getTravelledPathLength() + "m";

                if (p.getSurrounding_actual() instanceof Pipe) {
                    Pipe pipe = (Pipe) p.getSurrounding_actual();
                    information = information + ";Pipe " + pipe.getName() + ";relPos " + df1.format(p.getPosition1d_actual() / pipe.getLength()) + "";
//                    Position3D ll = pipe.getPosition3D(p.getPosition1d_actual());
                } else if (p.getSurrounding_actual() instanceof Manhole) {
                    Manhole mh = (Manhole) p.getSurrounding_actual();
                    information += "Manhole " + mh.toString() + ";";
//                            position = mh.getPosition3D(0);
                } else if (p.getSurrounding_actual() instanceof Surface) {
                    information += ";Surface Triangle " + p.surfaceCellID;
                    if (p.toSurface != null) {
                        information += ";spilled @" + p.toSurface;
                    }
                } else if (p.getSurrounding_actual() instanceof SurfaceTriangle) {
                    SurfaceTriangle st = (SurfaceTriangle) p.getSurrounding_actual();
                    information += ";Triangle " + st.getAutoID();
                    if (st.manhole != null) {
                        information += ";spilled @" + st.manhole;

                    }
                }

                if (p.getClass().equals(HistoryParticle.class)) {
                    HistoryParticle hp = (HistoryParticle) p;
                    information = "History" + information + ";tracked:" + hp.getHistory().size();
                    this.showTravelpath(hp);
                }
                String[] text = information.split(";");

                LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, new com.saemann.rgis.model.GeoPosition(mapViewer.clickPoint.x, mapViewer.clickPoint.y), text);
                mapViewer.addPaintInfoToLayer(key, lp);
                selectedLayer = string;
                selectedID = id;
                return;

            } else if (string.startsWith(layerPipes)) {
                for (Pipe p : network.getPipes()) {
                    if (p.getAutoID() == id) {
                        String key = mapViewer.LAYER_KEY_LABEL;
                        String[] text = preparePipeTags(p);
                        //Position
                        LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, mousePoint, text);
                        try {
                            mapViewer.addPaintInfoToLayer(key, lp);
                            selectedLayer = string;
                            selectedID = id;
                            informListener(p);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (string.startsWith(layerManhole)) {
                for (Manhole mh : network.getManholes()) {
                    if (mh.getAutoID() == id) {
                        LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, mousePoint, prepareManholeTags(mh));
                        mapViewer.addPaintInfoToLayer(mapViewer.LAYER_KEY_LABEL, lp);
                        selectedLayer = string;
                        selectedID = id;
                        informListener(mh);
                        return;
                    }
                }
            } else if (string.equals(layerShortcut)) {
                for (SurfacePathStatistics s : control.getSurface().getStatistics()) {
                    if (id == s.id) {
                        LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, mousePoint, prepareShortcutTag(s));
                        mapViewer.addPaintInfoToLayer(mapViewer.LAYER_KEY_LABEL, lp);
                        selectedLayer = string;
                        selectedID = id;
                        return;
                    }
                }
            } else if (string.startsWith(layerTriangle)) {
                StringBuilder str = new StringBuilder("Triangle id:").append(id);
                Surface surf = surface;
                if (surf != null && id >= 0) {
                    Manhole mh = surf.getManhole((int) id);
                    if (mh != null) {
                        str.append(";Manhole ").append(mh.toString());
                        str.append("(fill ").append((int) (mh.getWaterlevel() / (mh.getTop_height() - mh.getSole_height()))).append(" %)");
                    }
                    Inlet inlet = surf.getInlet((int) id);
                    if (mh != null) {
                        str.append(";Inlet ").append(inlet.toString());
                    }
                    float[] wls;
                    if (surf.getWaterlevels() != null && surf.getWaterlevels().length > 0) {
                        wls = surf.getWaterlevels()[(int) id];
                    } else {
                        wls = surf.loadWaterlevels((int) id);
                    }
                    if (wls != null && wls.length > 0) {
                        //Find actual and maximum waterlevel
                        float max = 0;
                        for (int i = 0; i < wls.length; i++) {
                            max = Math.max(wls[i], max);
                        }
                        int timeindex = surf.getTimeIndex(timeToShow);
                        float actualWL = wls[timeindex];
                        str.append(";Max WL: ").append(df3.format(max)).append("m");
                        str.append(";now WL: ").append(df3.format(actualWL)).append("m");

                    }

                    //display Measurements so far
                    if (surf.getMeasurementRaster() != null && surf.getMeasurementRaster() instanceof SurfaceMeasurementTriangleRaster) {
                        SurfaceMeasurementTriangleRaster raster = (SurfaceMeasurementTriangleRaster) surf.getMeasurementRaster();
                        TriangleMeasurement measurement = raster.getMeasurements()[(int) id];
                        //Number of Particles passed.
                        str.append(";Particles: ");
                        if (measurement == null) {
                            str.append("none");
                        } else {
                            double sum = 0;
                            for (int i = 0; i < measurement.getParticlecount().length; i++) {
                                double materialsum = 0;
                                for (int t = 0; t < measurement.getParticlecount()[i].length; t++) {
                                    materialsum += measurement.getParticlecount()[i][t];

                                }
                                str.append(df1.format(materialsum)).append(", ");
                                sum += materialsum;
                            }
                            if (measurement.getParticlecount().length > 1) {
                                str.append("\u03a3 ").append(df1.format(sum));
                            }
                            sum = 0;
                            str.append(";Mass: ");
                            for (int i = 0; i < measurement.getParticlecount().length; i++) {
                                double materialsum = 0;
                                for (int t = 0; t < measurement.getParticlecount()[i].length; t++) {
                                    materialsum += measurement.getMassResidence()[i][t];

                                }
                                str.append(df1.format(materialsum)).append(", ");
                                sum += materialsum;
                            }
                            if (measurement.getParticlecount().length > 1) {
                                str.append("\u03a3 ").append(df1.format(sum));
                            }
                            str.append("kg");

                        }
                    }
                }
                LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, mousePoint, str.toString().split(";"));
                mapViewer.addPaintInfoToLayer(mapViewer.LAYER_KEY_LABEL, lp);
                selectedLayer = string;
                selectedID = id;
                mapViewer.recalculateShapes();
                mapViewer.repaint();
                return;
            } else if (string.equals(layerInjectionLocation)) {
                for (InjectionInfo injection : injections) {
                    if (injection.getId() % injections.size() == id) {
                        if (injection.getPosition() == null) {
                            return;
                        }
                        StringBuilder str = new StringBuilder("Injection id:").append(id);
                        if (injection.spillOnSurface()) {
                            if (injection instanceof InjectionInformation) {
                                str.append(";to Surface triangle").append(((InjectionInformation) injection).getCapacityID());
                            }
                        } else {
                            str.append(";").append(injection.getCapacity());
                        }
                        str.append(";").append(df3.format(injection.getMass())).append(" kg of;").append(injection.getMaterial());
                        str.append("; during ").append(injection.getNumberOfIntervals()).append(" timesteps");
                        LabelPainting lp = new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, new com.saemann.rgis.model.GeoPosition(injection.getPosition().getLatitude(), injection.getPosition().getLongitude()), str.toString().split(";"));
                        mapViewer.addPaintInfoToLayer(mapViewer.LAYER_KEY_LABEL, lp);
                        selectedLayer = string;
                        selectedID = id;
                        mapViewer.recalculateShapes();
                        mapViewer.repaint();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showTravelpath(HistoryParticle hp) {
        mapViewer.clearLayer(layerHistoryPath);

        Coordinate[] list = new Coordinate[hp.getHistory().size()];
//        ArrayList<com.saemann.gulli.core.model.GeoPosition2D> line = new ArrayList<>(hp.getHistory().size());
        int index = 0;
        for (Capacity c : hp.getHistory()) {
            try {
                if (c instanceof Manhole) {
//                    line.add(((Manhole) c).getPosition());
                    list[index] = new CoordinateXY(((Manhole) c).getPosition().getLongitude(), ((Manhole) c).getPosition().getLatitude());
                } else if (c instanceof SurfaceTriangle) {
//                    line.add(((SurfaceTriangle) c).getPosition3D(0));
                    list[index] = new CoordinateXY(c.getPosition3D(0).getLongitude(), c.getPosition3D(0).getLatitude());
                } else {
                    list[index] = new CoordinateXY(c.getPosition3D(0).getLongitude(), c.getPosition3D(0).getLatitude());
                }
                index++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (index < 2) {
            return;
        }
        LinePainting lp = new LinePainting(0, list, new ColorHolder(Color.red, "Travelled path"));
        mapViewer.addPaintInfoToLayer(layerHistoryPath, lp);
    }

    public void updateLabel() {
        if (selectedLayer == null) {
            return;
        }
        this.selectLocationID(this, selectedLayer, selectedID);
    }

    private String[] prepareManholeTags(Manhole mh) {
        String str = mh.toString() + ";";
        str += mh.getWaterType() + ";";
        if (mh.isSetAsOutlet()) {
            str += " Outlet;";
        }
        str += "Triangle: " + mh.getSurfaceTriangleID() + ";";
//        str+="Timeline: "+((SparseTimelineManhole)mh.getStatusTimeLine()).isInitialized()+";";

        str += "Top   " + df3.format(mh.getTop_height()) + " m üNN;"
                + "Tiefe   " + df3.format(mh.getTop_height() - mh.getSole_height()) + " m;"
                + "Sohle " + df3.format(mh.getSole_height()) + " m üNN; ;"
                + "Waterlvl " + df3.format(mh.getWaterlevel()) + "m;"
                + "Water h  " + df3.format(mh.getWaterHeight()) + "m üNN;";

        for (int j = mh.getConnections().length - 1; j >= 0; j--) {
            Connection_Manhole_Pipe connection = mh.getConnections()[j];
//        for (Connection_Manhole_Pipe connection : mh.getConnections()) {
            str += j + " : " + df3.format(mh.getWaterHeight() - connection.getHeight()) + "m ";
            try {
                if (connection.isFlowInletToPipe()) {
                    str += " out - to " + connection.getPipe().getAutoID() + ";";
                } else if (connection.isFlowOutletFromPipe()) {
                    str += " in- from " + connection.getPipe().getAutoID() + ";";
                } else {
                    str += "  =  - " + connection.getPipe().getAutoID() + ";";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (control.getSurface() != null && control.getSurface().sourcesForSpilloutParticles != null) {
            if (control.getSurface().sourcesForSpilloutParticles.containsKey(mh)) {
                str += "Spillsource= " + control.getSurface().sourcesForSpilloutParticles.get(mh) + ";";
            } else {
                str += "No Source for spillsubstance;";
            }
        }

        return str.split(";");
    }

    private String[] preparePipeTags(Pipe p) {
        String str = p.toString() + ";" + p.getProfile().toString() + "; " + p.getWaterType() + ";"
                + "Decline  " + df3.format(p.getDecline()) + ";"
                + "Length   " + df3.format(p.getLength()) + "m;";
        if (p.getStatusTimeLine() != null) {
            if (p.getStatusTimeLine().getTimeContainer() instanceof TimeIndexContainer) {
                str += "TimeIndex: " + ((TimeIndexContainer) p.getStatusTimeLine().getTimeContainer()).getActualTimeIndex() + ";";
            }
            str += "Velocity " + df3.format(p.averageVelocity_actual()) + "m/s;" + "Waterlvl " + df3.format(p.getWaterlevel()) + "m;"
                    + "Fillrate " + df3.format(p.getFillRate()) + "%;";
        }

        str += "--- Manholes ---;"
                + "height start " + df3.format(p.getStartConnection().getHeight()) + "m üNN" + ";"
                + "height end " + df3.format(p.getEndConnection().getHeight()) + "m üNN;";

        return str.split(";");
    }

    private String[] prepareShortcutTag(SurfacePathStatistics s) {
        String str = ""
                + "From: " + s.getStart() + ";"
                + "To  : " + (s.getEndInlet() != null ? s.getEndInlet().toString() : s.getEndManhole().toString()) + ";"
                //                + "Dist: " + df3.format(s.getDistance()) + "m  direct;"
                + "N   : " + s.number_of_particles + ";"
                + "Trvl: " + df3.format(s.sum_travelLength / s.number_of_particles) + "m  \u00D8;"
                + "Time: " + df1.format(s.sum_traveltime / (s.number_of_particles * 1000.)) + "s ~ " + df1.format(s.sum_traveltime / (s.number_of_particles * 60000.)) + "min;"
                + "V   : " + df3.format((s.sum_travelLength / s.number_of_particles) / (s.sum_traveltime / (s.number_of_particles * 1000.))) + "m/s;"
                + "Time: " + df3.format(s.minTravelTime / 60000.) + " - " + df3.format(s.maxTravelTime / 60000.) + "min;"
                + "Dist: " + df3.format(s.minTravelLength) + " - " + df3.format(s.maxTravelLength) + "m;";
        return str.split(";");
    }

    public void setSelectedPipe(long id) {
        for (Pipe pipe : network.getPipes()) {
            if (pipe.getAutoID() == id) {
//                ArrayList<com.saemann.gulli.core.model.GeoPosition2D> list = new ArrayList<>(2);
//                list.add(pipe.getFlowInletConnection().getPosition());
//                list.add(pipe.getFlowOutletConnection().getPosition());
                ColorHolder ch = new ColorHolder(Color.red, "Selected");
                ch.setStroke(new BasicStroke(2));
                Coordinate[] list = new Coordinate[2];
                list[0] = pipe.getFlowInletConnection().getPosition().lonLatCoordinate();
                list[1] = pipe.getFlowOutletConnection().getPosition().lonLatCoordinate();

                LinePainting lp = new LinePainting(0, list, ch);
                mapViewer.addPaintInfoToLayer("SELECT", lp);
                mapViewer.setSelectedObject(lp);
                mapViewer.recalculateShapes();
                mapViewer.repaint();
                return;
            }
        }
    }

    public void showPipeFillRate(int timeIndex) {
        if (chFillrate == null) {
            initColoHolderFillrate(Color.white, Color.BLUE);
        }
        mapViewer.clearLayer(layerPipes);
        for (Pipe pipe : network.getPipes()) {
            double f = pipe.getProfile().getFillRate(pipe.getStatusTimeLine().getWaterlevel(timeIndex));
//            ArrayList<com.saemann.gulli.core.model.GeoPosition2D> list = new ArrayList<>(2);
//            list.add(pipe.getFlowInletConnection().getPosition());
//            list.add(pipe.getFlowOutletConnection().getPosition());
            Coordinate[] list = new Coordinate[2];
            list[0] = pipe.getFlowInletConnection().getPosition().lonLatCoordinate();
            list[1] = pipe.getFlowOutletConnection().getPosition().lonLatCoordinate();
            ArrowPainting p;

            if (f < 1) {
                p = new ArrowPainting(pipe.getAutoID(), list, chPipes) {
                    @Override
                    public boolean paint(Graphics2D g2) {
                        try {

                            if (this.drawShape == null) {
                                return false;
                            }
                            g2.setColor(getColor().getColor());
                            super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                            return true;
                        } catch (Exception e) {
                            if (MapViewer.verboseExceptions) {
                                e.printStackTrace();
                            }
                        }
                        return false;
                    }
                };
            } else {
                ColorHolder c = chFillrate[Math.min(101, Math.max((int) f, 0))];
                p = new ArrowPainting(pipe.getAutoID(), list, c) {
                    @Override
                    public boolean paint(Graphics2D g2) {
                        try {
                            if (this.drawShape == null) {
                                return false;
                            }
                            g2.setColor(getColor().getColor());
                            super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                            return true;
                        } catch (Exception e) {
                            if (MapViewer.verboseExceptions) {
                                e.printStackTrace();
                            }
                        }
                        return false;
                    }
                };
            }
            mapViewer.addPaintInfoToLayer(layerPipes, p);
        }
        mapViewer.repaint();
    }

    private void initMenuCheckboxes(SimpleMapViewerFrame frame) {
        JMenu menu = new JMenu("Shapes");
        final JCheckBoxMenuItem checkParticles = new JCheckBoxMenuItem("Particles", showParticles);
        final JCheckBoxMenuItem checkPipesConcentration = new JCheckBoxMenuItem("Concentration in Pipes", paintConcentrationColor);
        menu.add(checkParticles);
        menu.add(checkPipesConcentration);

        frame.getJMenuBar().add(menu);

        checkParticles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                showParticles = checkParticles.isSelected();
                if (!showParticles) {
                    mapViewer.clearLayer(layerParticle);

                } else {
                    mapViewer.clearLayer(layerParticle);
                    for (NodePainting particlePainting : particlePaintings) {
                        mapViewer.addPaintInfoToLayer(layerParticle, particlePainting);
                    }
                    mapViewer.recalculateShapes();
                }
                mapViewer.repaint();
            }
        });

        checkPipesConcentration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                paintConcentrationColor = checkPipesConcentration.isSelected();
                mapViewer.repaint();
            }
        });
    }

    public void clearParticles() {
        this.particlePaintings = new ParticleNodePainting[0];
        this.mapViewer.clearLayer(layerParticle);
        this.mapViewer.clearLayer(layerParticleNetwork);
        this.mapViewer.clearLayer(layerParticleSurface);
    }

    public void setPipeShow(PIPESHOW pipeShow) {
        this.pipeShow = pipeShow;
        this.mapViewer.clearLayer(layerPipes);
        this.mapViewer.clearLayer(layerPipes1);
        this.mapViewer.clearLayer(layerPipes2);
        this.mapViewer.clearLayer(layerPipes3);
        this.mapViewer.clearLayer(layerPipes4);
        this.mapViewer.clearLayer(layerPipesDir);
        this.mapViewer.clearLayer(layerPipesDirAnti);
        this.mapViewer.clearLayer(layerPipesDirChange);
        this.mapViewer.clearLayer(layerPipeOverly);
        this.mapViewer.clearLayer(layerHistoryPath);
        if (!layersHistoryPathToOutlets.isEmpty()) {
            for (String layerkey : layersHistoryPathToOutlets) {
                this.mapViewer.clearLayer(layerkey);
            }
        }

        this.addPipesPaint();
        mapViewer.recalculateShapes();
        mapViewer.repaint();

    }

    public void addSurfaceShow(SURFACESHOW surfaceshow) {

//        this.surfaceShow = surfaceshow;
//        this.mapViewer.clearLayer(layerSurfaceContaminated);
//        this.mapViewer.clearLayer(layerSurfaceWaterlevel);
//
//        if (surfaceshow != SURFACESHOW.GRID) {
//            this.mapViewer.clearLayer(layerSurfaceGrid);
//        }
//        this.mapViewer.clearLayer(layerSurfaceMeasurementRaster);
//        this.mapViewer.clearLayer(layerLabelWaterlevel);
//
//        if (surface == null) {
//            this.surfaceShow = SURFACESHOW.NONE;
//            return;
//        }
        if (!surfaceShows.contains(surfaceshow)) {
            surfaceShows.add(surfaceshow);
        }
        updateSurfaceShows();

    }

    public ArrayList<SURFACESHOW> getSurfaceShows() {
        return surfaceShows;
    }

    public void clearSurfaceShows() {
        SURFACESHOW[] toclear = surfaceShows.toArray(new SURFACESHOW[surfaceShows.size()]);
        try {
            for (SURFACESHOW surfaceshow : toclear) {
                removeSurfaceShow(surfaceshow);
            }
        } catch (Exception e) {
        }
        surfaceShows.clear();
    }

    public boolean removeSurfaceShow(SURFACESHOW show) {
        try {
            if (show == SURFACESHOW.GRID) {
                mapViewer.clearLayer(layerSurfaceGrid);
            } else if (show == SURFACESHOW.ANALYSISRASTER) {
                mapViewer.clearLayer(layerSurfaceMeasurementRaster);
            } else if (show == SURFACESHOW.HEATMAP_LIN || show == SURFACESHOW.HEATMAP_LOG || show == SURFACESHOW.HEATMAP_LIN_BAGATELL) {
                mapViewer.clearLayer(layerSurfaceContaminated);
            } else if (show == SURFACESHOW.SPECTRALMAP) {
                mapViewer.clearLayer(layerSurfaceContaminated);
            } else if (show == SURFACESHOW.CONTAMINATIONCLUSTER) {
                mapViewer.clearLayer(layerSurfaceContaminated);
            } else if (show == SURFACESHOW.VERTEX_HEIGHT) {
                mapViewer.clearLayer(layerLabelWaterlevel);
            } else if (show == SURFACESHOW.SLOPE) {
                mapViewer.clearLayer(layerSurfaceSlope);
                mapViewer.clearLayer(layerArrow);
            } else if (show == SURFACESHOW.VELOCITY) {
                mapViewer.clearLayer(layerArrow);
            } else if (show == SURFACESHOW.PARTICLETRACE) {
                mapViewer.clearLayer(layerHistoryPath);
            } else if (show == SURFACESHOW.PARTICLETRACE_OUTLET) {
                mapViewer.clearLayer(layerHistoryPath);
                for (String layersHistoryPathToOutlet : layersHistoryPathToOutlets) {
                    mapViewer.clearLayer(layersHistoryPathToOutlet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return surfaceShows.remove(show);
    }

    public SURFACESHOW getSurfaceShow(int index) {
        return this.surfaceShows.get(index);
    }

    public boolean isDrawPipesAsArrows() {
        return drawPipesAsArrows;
    }

    public boolean isDrawingTrianglesAsNodes() {
        return drawTrianglesAsNodes;
    }

    public void setDrawPipesAsArrows(boolean drawPipesAsArrows) {
        this.drawPipesAsArrows = drawPipesAsArrows;
        this.mapViewer.clearLayer(layerPipes);
        this.mapViewer.clearLayer(layerPipes1);
        this.mapViewer.clearLayer(layerPipes2);
        this.mapViewer.clearLayer(layerPipes3);
        this.mapViewer.clearLayer(layerPipes4);
        this.mapViewer.clearLayer(layerPipeOverly);
        this.addPipesPaint();
        mapViewer.recalculateShapes();
        mapViewer.repaint();
    }

    public void resetStatusVisualization() {
        affectedManholes.clear();
        addPipesPaint();
        mapViewer.repaint();
    }

    public void updateUTMReference() {
        Point2D.Double tl = mapViewer.getPosition(0, 0);
        Point2D.Double br = mapViewer.getPosition(mapViewer.getWidth(), mapViewer.getHeight());
        if (posTopLeftPipeCS == null) {
            posBotRightPipeCS = new Point2D.Double();
            posTopLeftPipeCS = new Point2D.Double();
        }
        if (posTopLeftSurfaceCS == null) {
            posBotRightSurfaceCS = new Coordinate();
            posTopLeftSurfaceCS = new Coordinate();
        }
        try {
            if (geoToolsNetwork != null) {
                Coordinate topleft = geoToolsNetwork.toUTM(new Coordinate(tl.y, tl.x));
                Coordinate bottright = geoToolsNetwork.toUTM(new Coordinate(br.y, br.x));
                posTopLeftPipeCS.setLocation(topleft.x, topleft.y);
                posBotRightPipeCS.setLocation(bottright.x, bottright.y);
            }
            if (geoToolsSurface != null) {
                posTopLeftSurfaceCS = geoToolsSurface.toUTM(new Coordinate(tl.y, tl.x));
                posBotRightSurfaceCS = geoToolsSurface.toUTM(new Coordinate(br.y, br.x));

                if (surface != null) {
                    //make a list of all triangles, that are currently shown on the map.
                    ArrayList<Integer> shownTriangleIDs = new ArrayList<>(surface.getMaxTriangleID() + 1);
                    this.showSurfaceTriangle = new boolean[surface.getTriangleMids().length];
                    for (int i = 0; i < surface.getTriangleMids().length; i++) {
                        double[] triangleMid = surface.getTriangleMids()[i];
                        if (triangleMid[0] > posBotRightSurfaceCS.x || triangleMid[1] < posBotRightSurfaceCS.y || triangleMid[0] < posTopLeftSurfaceCS.x || triangleMid[1] > posTopLeftSurfaceCS.y) {
                            continue;
                        } else {
                            shownTriangleIDs.add(i);
                            showSurfaceTriangle[i] = true;
                        }
                    }
                    int[] tris = new int[shownTriangleIDs.size()];
                    int index = 0;
                    for (Integer shownTriangleID : shownTriangleIDs) {
                        tris[index] = shownTriangleID;
                        index++;
                    }
                    this.shownSurfaceTriangles = tris;

                }
            }

        } catch (TransformException ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrowPainting calcArrowPainting(double triangleMidX, double triangleMidY, double vX, double vY, long id, ColorHolder ch) {
        Coordinate start = null;
        try {
            start = geoToolsSurface.toGlobal(new Coordinate(triangleMidX, triangleMidY));

        } catch (TransformException ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        Coordinate target = null;
        try {
            target = geoToolsSurface.toGlobal(new Coordinate(triangleMidX + vX, triangleMidY + vY));
        } catch (TransformException ex) {
            System.out.println("triangleMidX=" + triangleMidX + "  vX=" + vX + "\t triangleMidY=" + triangleMidY + ", vY=" + vY);
            Logger
                    .getLogger(PaintManager.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        ArrowPainting ap = new ArrowPainting(id, new Coordinate[]{start, target}, ch);
        return ap;
    }

    public static LinePainting calcLinePainting(double triangleMidX, double triangleMidY, double vX, double vY, long id, ColorHolder ch, GeoTools geoTools) {
        Coordinate start = null;
        try {
            start = geoTools.toGlobal(new Coordinate(triangleMidX, triangleMidY));

        } catch (TransformException ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        Coordinate target = null;
        try {
            target = geoTools.toGlobal(new Coordinate(triangleMidX + vX, triangleMidY + vY));
        } catch (TransformException ex) {
            System.out.println("triangleMidX=" + triangleMidX + "  vX=" + vX + "\t triangleMidY=" + triangleMidY + ", vY=" + vY);
            Logger
                    .getLogger(PaintManager.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        LinePainting ap = new LinePainting(id, new Coordinate[]{start, target}, ch);
        return ap;
    }

    public void AddVelocityArrrows(int[][] neighbours, double[][] triangleMids, float[][] velocities, int[][] triangleindizes, float[][] vertices) {
//        try {
        ColorHolder ch = new ColorHolder(Color.blue, "Surface Velocity");

//            CRSAuthorityFactory af = CRS.getAuthorityFactory(StartParameters.JTS_WGS84_LONGITUDE_FIRST);
//            CoordinateReferenceSystem crsWGS = af.createCoordinateReferenceSystem("EPSG:4326");
//            CoordinateReferenceSystem crsUTM = af.createCoordinateReferenceSystem("EPSG:25832");
//
//            MathTransform transformWGS_UTM = CRS.findMathTransform(crsWGS, crsUTM);
//            MathTransform transformUTM_WGS = CRS.findMathTransform(crsUTM, crsWGS);
//            double distance = 0;
        for (int i = 0; i < triangleMids.length; i++) {
            try {
                double posX = 0;
                double posY = 0;

                posX += vertices[triangleindizes[i][0]][0] + vertices[triangleindizes[i][1]][0] + vertices[triangleindizes[i][2]][0];
                posY += vertices[triangleindizes[i][0]][1] + vertices[triangleindizes[i][1]][1] + vertices[triangleindizes[i][2]][1];

                posX /= 3.;
                posY /= 3.;

                double vx = 0;
                double vy = 0;
                int outs = 0;
                for (int j = 0; j < 3; j++) {
                    int nb = neighbours[i][j];
                    if (nb < 0) {
                        continue;
                    }

                    double v = velocities[i][j];
                    double sx = triangleMids[nb][0] - posX;
                    double sy = triangleMids[nb][1] - posY;
                    double l = Math.sqrt(sx * sx + sy * sy);

                    if (Math.abs(l) < 0.0001) {
                        continue;
                    }
                    vx += sx * v / l;
                    vy += sy * v / l;
                    outs++;
                }
                if (outs == 0) {
                    outs = 1;
                }
                double vxn = vx / (double) outs;
                double vyn = vy / (double) outs;

                if (Double.isNaN(vyn)) {
                    System.out.println(" outs: " + outs + " vx:" + vx);
                }

                ArrowPainting ap = calcArrowPainting(posX, posY, vxn, vyn, i, ch);
                ap.arrowheadvisibleFromZoom = 40;
                mapViewer.addPaintInfoToLayer(layerPipes3, ap);
//                break;

            } catch (Exception ex) {
                Logger.getLogger(PaintManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        }
//        } catch (FactoryException ex) {
//            Logger.getLogger(PaintManager.class
//                    .getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        try {
            mapViewer.clearLayer(layerSurfaceBoundary);
            mapViewer.clearLayer(layerSurfaceContaminated);
            mapViewer.clearLayer(layerSurfaceWaterlevel);
            mapViewer.clearLayer(layerSurfaceMeasurementRaster);
            this.mapViewer.clearLayer(layerSurfaceGrid);
            if (surface != null) {
                this.geoToolsSurface = surface.getGeotools();
                try {
                    setInletsPaint(surface, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mapViewer.recalculateShapes();
                mapViewer.adjustMap();

            }
        } catch (Exception ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void resetSurfaceShapes() {
        mapViewer.clearLayer(layerArrow);
        mapViewer.clearLayer(layerHistory);
        mapViewer.clearLayer(layerShortcut);
        mapViewer.clearLayer(layerSurfaceContaminated);

    }

    /**
     * Draws paths, representing overland travel shortcuts of particles.
     */
    public void drawShortcuts() {
        mapViewer.clearLayer(layerShortcut);
        Surface surface = control.getSurface();
        if (control.getSurface() != null && control.getSurface().getStatistics() != null) {
            for (SurfacePathStatistics s : control.getSurface().getStatistics()) {

                try {
                    Coordinate c0 = surface.getGeotools().toGlobal(new Coordinate(surface.getTriangleMids()[s.getStart()][0], surface.getTriangleMids()[s.getStart()][1]), false);
                    Coordinate c1 = null;
                    if (s.getEndInlet() != null) {
                        c1 = s.getEndInlet().getPosition().lonLatCoordinate();
                    } else if (s.getEndManhole() != null) {
                        c1 = s.getEndManhole().getPosition().lonLatCoordinate();
                    } else {
                        continue;
                    }
                    ArrowPainting ap = new ArrowPainting(s.id, new Coordinate[]{c0, c1}, chShortcut);
                    mapViewer.addPaintInfoToLayer(layerShortcut, ap);

                } catch (TransformException ex) {
                    Logger.getLogger(PaintManager.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        mapViewer.repaint();
//        System.out.println("drew " + i + " shortcuts");
    }

    /**
     * If true, triangles are represented as dots (faster), if false, triangular
     * shapes are used (realistic).
     *
     * @param asNodes
     */
    public void setSurfaceTrianglesShownAsNodes(boolean asNodes) {
        this.mapViewer.clearLayer(layerSurfaceContaminated);
        this.mapViewer.clearLayer(layerSurfaceWaterlevel);
        this.mapViewer.clearLayer(layerSurfaceGrid);
        this.mapViewer.clearLayer(layerSurfaceMeasurementRaster);

        this.drawTrianglesAsNodes = asNodes;
        this.updateSurfaceShows();
        this.mapViewer.recalculateShapes();
        this.mapViewer.repaint();
    }

    public void clearInjectionLocations() {
        injections.clear();
        mapViewer.clearLayer(layerInjectionLocation);
        mapViewer.recalculateShapes();

    }

    public void setInjectionLocation(Collection<InjectionInfo> injections) throws Exception {
        this.injections.clear();
//        if (injections.size() > 50) {
//            throw new Exception("Will not display more than 50 injection locations. (" + injections.size() + ")");
//        }
        int counter = 0;
        for (InjectionInfo in : injections) {
            counter++;
            if (injections.size() > 100) {
                this.injections.addAll(injections);
                System.err.println("Will only display 100 injection locations.");
                break;
//                throw new Exception("Will not display more than 50 injection locations. (" + injections.size() + ")");
            }
            try {
                com.saemann.gulli.core.model.GeoPosition2D pos = null;
                //Find position to display, if the coordinate is not explicitly given.
                if (in.getPosition() != null) {
                    pos = in.getPosition();
                } else {
                    if (in instanceof InjectionInformation) {
                        if (in.spillOnSurface()) {
                            //Calculate from triangle ID
                            if (((InjectionInformation) in).getCapacityID() >= 0) {
                                double[] tm = control.getSurface().getTriangleMids()[((InjectionInformation) in).getCapacityID()];
                                Coordinate c = control.getSurface().getGeotools().toGlobal(new Coordinate(tm[0], tm[1]), false);
                                pos = new GeoPosition(c.x, c.y);
                            }
                        } else {
                            if (in.getCapacity() != null) {
                                pos = in.getCapacity().getPosition3D(((InjectionInformation) in).getPosition1D());
                            }
                        }
                    }
                }
                //Build Shape
                if (pos != null) {
//                    PaintInfo pi = new ShapePainting(pos, ShapeEditor.SHAPES.CROSS.getShape(), 0, 0, id++, chInjectionLocation, chInjectionLocation.getStroke());
                    NodePainting np = new NodePainting(in.getId(), new com.saemann.rgis.model.GeoPosition(pos.getLatitude(), pos.getLongitude()), chInjectionLocation);
//                    {
//                        @Override
//                        public boolean paint(Graphics2D g2) {
//                            g2.setColor(chInjectionLocation.getColor());
//                            g2.setStroke(chInjectionLocation.getStroke());
//                            g2.translate(refPointX, refPointY);
//                            g2.draw(ShapeEditor.SHAPES.CROSS.getShape());
//                            return true;
//                        }
//                    };
                    np.radius = 5;
                    np.setShapeRound(true);
                    mapViewer.addPaintInfoToLayer(layerInjectionLocation, np);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        this.injections.addAll(injections);
    }

    public static void paintTime(Date date, DateFormat df, Graphics2D g2, Color c, float x, float y) {
        g2.setColor(c);
        g2.drawString(df.format(date), x, y);
    }

    public static void paintTimes(long datetime, long simTime, Graphics2D g2, float width, float height) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.YYYY HH:mm");
        String dt = sdf.format(new Date(datetime));
        int min = (int) (simTime / 60000) % 60;
        int hrs = (int) (simTime / 3600000);
        String st = hrs + " hrs  " + (min < 10 ? "0" + min : min + "") + " min";
        g2.setColor(Color.white);
        g2.fillRect((int) width - 111, 3, 107, 35);

        g2.setColor(Color.black);
        g2.drawRect((int) (width - 110), 4, 104, 32);

        g2.drawString(dt, width - 105, 18);
        g2.drawString(st, width - 83, 31);
    }

    @Override
    public void actionFired(Action action, Object source) {
        if (action.progress == 1) {
            resetSurfaceShapes();
            clearInjectionLocations();
            try {
                if (control != null && control.getScenario() != null && control.getScenario().getInjections() != null) {
                    setInjectionLocation(control.getScenario().getInjections());
                    mapViewer.recomputeLegend();
                    mapViewer.recalculateShapes();

                }
            } catch (Exception ex) {
                Logger.getLogger(PaintManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void loadNetwork(Network network, Object caller) {
        this.setNetwork(network);
        mapViewer.zoomToFitLayer();
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
        this.setSurface(surface);
    }

    @Override
    public void loadScenario(Scenario scenario, Object caller) {
    }

    @Override
    public void simulationINIT(Object caller) {
        resetSurfaceShapes();
        clearInjectionLocations();
        try {
            if (control != null && control.getScenario() != null && control.getScenario().getInjections() != null) {
                setInjectionLocation(control.getScenario().getInjections());
                mapViewer.recomputeLegend();
                mapViewer.recalculateShapes();

            }
        } catch (Exception ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void simulationSTART(Object caller) {
    }

    /**
     *
     * @param loop
     * @param caller
     */
    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
        if (loop % repaintPerLoops == 0) {
            if (pauseSimulationWhilePaining) {
                orderParticlesPainting();
                mapViewer.recalculateShapes();
                updateLabel();
                mapViewer.repaint();
            } else {
                synchronized (repaintThread) {
                    repaintThread.notifyAll();
                }
            }
        }
    }

    @Override
    public void simulationPAUSED(Object caller) {
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
    }

    @Override
    public void simulationSTOP(Object caller) {
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
        synchronized (repaintThread) {
            repaintThread.notifyAll();
        }
    }

    @Override
    public void simulationRESET(Object caller) {
        resetSurfaceShapes();
        clearInjectionLocations();
        try {
            if (control != null && control.getScenario() != null && control.getScenario().getInjections() != null) {
                setInjectionLocation(control.getScenario().getInjections());
                mapViewer.recomputeLegend();
                mapViewer.recalculateShapes();

            }
        } catch (Exception ex) {
            Logger.getLogger(PaintManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setParticles(Collection<Particle> particles, Object source) {
        mapViewer.clearLayer(this.layerParticle);
        mapViewer.clearLayer(this.layerParticleNetwork);
        mapViewer.clearLayer(this.layerParticleSurface);
        int index = 0;
        this.particlePaintings = new ParticleNodePainting[Math.min(particles.size(), maximumNumberOfParticleShapes)];
        for (final Particle p : particles) {
            if (index > maximumNumberOfParticleShapes - 1) {
                break;
            }
            ParticleNodePainting np;

            if (p.getClass().equals(HistoryParticle.class
            )) {
                HistoryParticle hp = (HistoryParticle) p;
                np = new ParticleNodePainting(p, p.getId(), new Coordinate(0, 0), chParticles);

                np.setRadius(4);
                np.setShapeRound(true);
            } else {
                np = new ParticleNodePainting(p, p.getId(), new Coordinate(0, 0), chParticles);
                np.radius = 1;
            }

            this.particlePaintings[index] = np;
            index++;
        }
    }

    @Override
    public void clearParticles(Object source) {
        this.clearParticles();
    }

    public MapViewer getMapViewer() {
        return mapViewer;
    }

    public boolean addCapacitySelectionListener(CapacitySelectionListener listener) {
        if (selectionListener.contains(listener)) {
            return false;
        }
        return selectionListener.add(listener);
    }

    public boolean removeCapacitySelectionListener(CapacitySelectionListener listener) {
        return selectionListener.remove(listener);
    }

    private void informListener(Capacity c) {
        for (CapacitySelectionListener selectionListener1 : selectionListener) {
            selectionListener1.selectCapacity(c, this);

        }
    }

    public class ParticleNodePainting extends NodePainting {

//        public Coordinate longLat;
        Particle p;

        public ParticleNodePainting(Particle p, long id, Coordinate position, ColorHolder color) {
            super(id, position, color);
            this.p = p;
////            this.longLat = position;
//            this.positionWGS84 = new com.saemann.rgis.model.GeoPosition2D() {
//
//                @Override
//                public double getLatitude() {
//                    return refLatitude;
//                }
//
//                @Override
//                public double getLongitude() {
//                    return refLongitude;
//                }
//            };
        }

//        @Override
//        public double getRefLatitude() {
//            return refLatitude;
//        }
//
//        @Override
//        public double getRefLongitude() {
//            return refLongitude;
//        }
//        @Override
//        public com.saemann.rgis.model.GeoPosition2D getPosition() {
//            return positionWGS84;
//        }
        @Override
        public boolean paint(Graphics2D g2) {
            if (p.isInactive()) {
                return false;
            }
            if (this.outlineShape == null || getColor() == null || getColor().getColor() == null) {
                return false;
            }
            if (p.isOnSurface()) {
                if (p.isDrySurfaceMovement()) {
                    g2.setColor(Color.orange);
                } else {
                    g2.setColor(chParticlesSurface.getColor());
                }
            } else {
                g2.setColor(chParticlesNetwork.getColor());
            }

            if (p.isDeposited()) {
                g2.setColor(Color.black);
            }

            try {
                super.paint(g2);
            } catch (OutOfMemoryError e) {
                System.gc();
                control.getThreadController().paintOnMap = false;
                System.err.println("Heap Space exceeded. Map repaint deactivated");
                e.printStackTrace();
            }
            return true;
        }

        public void setLongLat(Coordinate longLat) {
//            this.longLat = longLat;
            this.refLatitude = longLat.y;
            this.refLongitude = longLat.x;
        }

        public void setLongLat(double longitude, double latitude) {
            this.refLatitude = latitude;
            this.refLongitude = longitude;
        }

    }

//    public static ArrayList<com.saemann.rgis.model.GeoPosition2D> toRGIS(ArrayList<com.saemann.gulli.core.model.GeoPosition2D> list) {
//        ArrayList<com.saemann.rgis.model.GeoPosition2D> newlist = new ArrayList<>(list.size());
//        for (com.saemann.gulli.core.model.GeoPosition2D geoPosition : list) {
//            com.saemann.rgis.model.GeoPosition gp = new com.saemann.rgis.model.GeoPosition(geoPosition.getLatitude(), geoPosition.getLongitude());
//            newlist.add(gp);
//        }
//        return newlist;
//    }
}
