package model.surface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;
import control.LocationIDListener;
import static io.SHP_IO_GULLI.transform;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import model.GeoPosition;
import model.GeoPosition2D;
import model.surface.topology.FlowPath;
import model.surface.topology.FlowSegment;
import model.surface.topology.LocalManholePoint;
import model.surface.topology.LocalPoolPoint;
import model.topology.Inlet;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.StorageVolume;
import model.topology.graph.Pair;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.gui.jmapviewer.source.MyOSMTileSource;
import view.ColorHolder;
import view.DoubleColorHolder;
import view.SimpleMapViewerFrame;
import view.shapes.AreaPainting;
import view.shapes.ArrowPainting;
import view.shapes.LabelPainting;
import view.shapes.NodePainting;

/**
 * Tools for mapping Surface and Pipenetwork to each other.
 *
 * @author saemann
 */
public class SurfaceTools implements LocationIDListener {

    private JFrame frame_Regler = new JFrame("Wasserhöhe");
    private JSlider slider_Pegel = new JSlider(0, 200, 0);
    private JLabel label_Pegel = new JLabel("Wassertiefe");

    private Thread showThread = null;

    private int lengthX;
    private int lengthY;
    private float[][] minimum;
    private float[][] maximum;
    private int[][] dreieckIDminimum;
    private int[][][] zielKoordinaten;
//    private int[][][] zielKoordinatenFlood;
    private double breite;
    private double hoehe;
    public double utmXmin = 547200;
    public double utmXmax = 550200;
    public double utmYmin = 5798000;
    public double utmYmax = 5801000;
    public double gitterWeiteX = 4;
    public double gitterWeiteY = 4;
    /**
     * Informationen über die geodätische Höhe
     */
    private LocalMinimumPoint[][] punkte;

    private LocalPoolPoint[][] sammelPunkte;
    /**
     * Trocken Flächen mit einem lokalen Minimum.
     */
    public final LinkedList<SurfaceLocalMinimumArea> surfaceAreas = new LinkedList<>();

    /**
     * Zusammenschluss an Trockenflächen, die zu einem Minimum zusammenfließen.
     */
    public final LinkedList<FlowArea> flowAreas = new LinkedList<>();

    /**
     * Flowpaths between pois on the surface.
     */
    LinkedList<FlowPath> paths = new LinkedList<>();

    HashMap<Integer, FlowPath> pathIDMap = new HashMap<>();
//    LinkedList<LocalMinimumTriangle> dreiecke;
    GeometryFactory gf = new GeometryFactory();

//    public CoordinateReferenceSystem utm32 = null;
    MathTransform utm32wgs;
    //Netzwerk
    Network network;
    HashMap<StorageVolume, SurfaceLocalMinimumArea> mhaMapIn, mhaMapOut;
    //Mapviewer
    private SimpleMapViewerFrame frame;
    DoubleColorHolder chInfo = new DoubleColorHolder(Color.WHITE, Color.black, "Info");
    private final String layerFlowArrow = "Flow";
    private final String layerFlowArrowFlood = "FloodFlow";
    private final String layerHeightDot = "Height";
    private final String layerSurfaceArea = "SurfaceArea";
    private final String layerFlowArea = "FlowArea";
    private final String layerFloodArea = "FloodArea";
    private final String layerStreetInlet = "StreetInlet";
    private final String layerStreetInletConnection = "StreetInletConnection";
    private final String layerManhole = "Manhole";
    private final String layerPipe = "Pipe";
    private final String layerDreiecke = "Dreiecke";
    private final String layerEinleitungPipe = "EinleitungRohr";
    private final String layerEinleitung = "Einleitung";
    private final String layerAustritt = "Austritt";
    private final String layerFlowPath = "FlowPath";
    private final String layerFlowPathactive = "FlowPathActive";

    private final DecimalFormat df4 = new DecimalFormat("0.0###");
    private Object c;

    public SurfaceTools() {
        initRegler();
    }

    public void getHotspotRaster(File triangleFile) throws MalformedURLException, IOException, TransformException, ClassNotFoundException, Exception {
        ShapefileDataStore s = new ShapefileDataStore(triangleFile.toURL());
        s.setCharset(Charset.forName("UTF-8"));
        ContentFeatureSource fs = s.getFeatureSource();
        org.geotools.data.store.ContentFeatureCollection fc = fs.getFeatures();
        boolean verbose = false;
        FeatureIterator iterator = fc.features();
        int zehntelschritte = fc.size() / 10;
        long starttime = System.currentTimeMillis();
        int acount = fc.getSchema().getAttributeCount();
        int wlcount = 0;
        for (int i = 0; i < acount; i++) {
            if (fc.getSchema().getType(i).getName().toString().contains("WL_")) {
                wlcount++;
            }
        }
        int[] indexWL = new int[wlcount];
        for (int i = 0; i < indexWL.length; i++) {
            indexWL[i] = fc.getSchema().indexOf("WL_" + i);
        }
        int indexZ = fc.getSchema().indexOf("Z");
        int indexgeom = fc.getSchema().indexOf("the_geom");

        CRS.cleanupThreadLocals();

        try {
            if (Network.crsUTM == null) {
                Network.crsUTM = CRS.decode("EPSG:25832");//UTM WGS89 Zone 32
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        utm32wgs = CRS.findMathTransform(Network.crsUTM, Network.crsWGS84);
        // Raster aufbauen
        breite = utmXmax - utmXmin;
        hoehe = utmYmax - utmYmin;

        //Testzahlen
        double xmin = Float.POSITIVE_INFINITY;
        double ymin = Float.POSITIVE_INFINITY;
        double xmax = Float.NEGATIVE_INFINITY;
        double ymax = Float.NEGATIVE_INFINITY;
        float zmin = Float.POSITIVE_INFINITY;
        float zmax = Float.NEGATIVE_INFINITY;

        //Raster
        lengthX = (int) (breite / gitterWeiteX);
        lengthY = (int) (hoehe / gitterWeiteY);

        minimum = new float[lengthX][lengthY];
        maximum = new float[lengthX][lengthY];
        dreieckIDminimum = new int[lengthX][lengthY];
        zielKoordinaten = new int[lengthX][lengthY][2];

        //Auslesen
        int count = 0;
        float z;
        double y;
        double x;
        int iX, iY;

        SimpleFeature f;
        MultiPolygon the_geom;
        Coordinate coord;
        while (iterator.hasNext()) {
            count++;
            if (/*verbose &&*/count % zehntelschritte == 0) {
                System.out.println(" " + ((int) (count * 100. / fc.size())) + "%\t" + (int) ((System.currentTimeMillis() - starttime) / 1000) + " s.");
            }
            f = (SimpleFeature) iterator.next();
            the_geom = (MultiPolygon) f.getAttribute(indexgeom);
            coord = the_geom.getCentroid().getCoordinate();
            x = coord.x;
            y = coord.y;
            z = ((Double) f.getAttribute(indexZ)).floatValue();

            xmin = Math.min(xmin, x);
            ymin = Math.min(ymin, y);
            xmax = Math.max(xmax, x);
            ymax = Math.max(ymax, y);
            zmin = Math.min(zmin, z);
            zmax = Math.max(zmax, z);

            // Mittelpunkt betrachten
            iX = getXindex(coord.x);
            iY = getYindex(coord.y);
            if (minimum[iX][iY] < 1) {
                minimum[iX][iY] = z;
                dreieckIDminimum[iX][iY] = count;
            } else {
                if (minimum[iX][iY] > z) {
                    minimum[iX][iY] = z;
                    dreieckIDminimum[iX][iY] = count;
                }
            }
            maximum[iX][iY] = Math.max(maximum[iX][iY], z);
        }
        iterator.close();

        s.dispose();
        System.out.println("xmin=" + xmin + "\txmax=" + xmax + "\tymin=" + ymin + "\tymax=" + ymax + "\t\tzmin=" + zmin + "\tzmax=" + zmax);
        frame = new SimpleMapViewerFrame();
        frame.mapViewer.setBaseLayer(MyOSMTileSource.BaseLayer.CARTO_LIGHT.getSource());
        frame.setBounds(50, 50, 700, 700);
        frame.setVisible(true);
        frame.mapViewer.addListener(this);
        BasicStroke bs1 = new BasicStroke(1);
        BasicStroke bs3 = new BasicStroke(3);
        ColorHolder[] ch = new ColorHolder[1001];
        for (int i = 0; i < ch.length; i++) {
            float c = ((i) / (float) ch.length);
            ch[i] = new ColorHolder(new Color(c, c, c));
            ch[i].setStroke(bs3);
        }
        int punkteCounter = 0;
        for (int ix = 0; ix < lengthX; ix++) {
            for (int iy = 0; iy < lengthY; iy++) {

                z = minimum[ix][iy];
//                System.out.println("["+ix+"]["+iy+"] = "+z);
                if (z < 1) {
                    continue;
                }
                punkteCounter++;
                Coordinate c = new Coordinate((ix * breite / lengthX) + utmXmin, (iy * hoehe / lengthY) + utmYmin);
                Point wgs = transform(gf.createPoint(c), utm32wgs);
                int cindex = (int) (((z - zmin) * ch.length) / (zmax - zmin));

                ColorHolder h = ch[cindex];
//                System.out.println("z="+z+" --> index:"+cindex+"\t c="+h.getColor().getBlue());
                NodePainting np = new NodePainting(ix * lengthY + iy, wgs.getCoordinate(), h) {

                    @Override
                    public boolean paint(Graphics2D g2) {
                        if (this.outlineShape == null) {
                            return false;
                        }
                        g2.setColor(this.getColor().getColor());
                        super.paint(g2);
                        return true;
                    }

                };
                np.radius = 4;

                frame.mapViewer.addPaintInfoToLayer(layerHeightDot, np);
                // Suche fließrichtungsvektoren
                float tzmin = z;
                int rX = ix, rY = iy;

                for (int i = -1; i < 2; i++) {
                    if (ix + i < 0) {
                        continue;
                    }
                    if (ix + i >= lengthX) {
                        continue;
                    }
                    for (int j = -1; j < 2; j++) {
                        if (i == 0 && j == 0) {
                            continue;
                        }
                        if (iy + j < 0) {
                            continue;
                        }
                        if (iy + j >= lengthY) {
                            continue;
                        }
                        if (minimum[ix + i][iy + j] > 0) {
                            if (minimum[ix + i][iy + j] < tzmin) {
                                tzmin = minimum[ix + i][iy + j];
                                rX = ix + i;
                                rY = iy + j;
                            } else if (minimum[ix + i][iy + j] == tzmin) {
                                //If at same minimum level, compare the maximum heights
                                if (maximum[ix + i][iy + j] < maximum[rX][rY]) {
                                    tzmin = minimum[ix + i][iy + j];
                                    rX = ix + i;
                                    rY = iy + j;
                                }
                            }
                        }
                    }
                }

                if (rX != ix || rY != iy) {
                    //Transformiere Ziel

                    zielKoordinaten[ix][iy][0] = rX;
                    zielKoordinaten[ix][iy][1] = rY;
//                    System.out.println("zielkoordinate von " + (ix * lengthY + iy) + "\t" + (rX * lengthY + rY));
                }

            }
        }

        System.out.println("Richtungen bestimmt. " + punkteCounter + " Höhenpunkte mit Werten.");
        System.out.println("PunkteRaster erstellen...");
        //Areas bestimmen
        //Punkte erstellen
        punkte = new LocalMinimumPoint[lengthX][lengthY];

        for (int i = 0; i < punkte.length; i++) {
            for (int j = 0; j < punkte[0].length; j++) {

                if (punkte[i][j] == null) {
                    punkte[i][j] = new LocalMinimumPoint(i * lengthY + j, i, j);
                    punkte[i][j].minZ = minimum[i][j];
                    punkte[i][j].coordUTM = new Coordinate((i * breite / lengthX) + utmXmin, (j * hoehe / lengthY) + utmYmin);
                }
                if (zielKoordinaten[i][j][0] > 0) {
                    int zielX = zielKoordinaten[i][j][0];
                    int zielY = zielKoordinaten[i][j][1];
//                System.out.println("ziel von id=" + i + " ist " + ziel);
                    if (punkte[zielX][zielY] == null) {
                        punkte[zielX][zielY] = new LocalMinimumPoint(zielX * lengthY + zielY, zielX, zielY);
                        punkte[zielX][zielY].minZ = minimum[zielX][zielY];
                        punkte[zielX][zielY].coordUTM = new Coordinate((zielX * breite / lengthX) + utmXmin, (zielY * hoehe / lengthY) + utmYmin);
                    }
                    punkte[i][j].lowerPoint = punkte[zielX][zielY];
                    punkte[zielX][zielY].hoehere.add(punkte[i][j]);
                }
            }
        }
        System.out.println("PunkteRaster erstellt.");
    }

    public void areasSurfaceFind() {
        System.out.println("Areas zusammenfinden...");
        long start = System.currentTimeMillis();
        //Areas bauen
        surfaceAreas.clear();
        for (int i = 0; i < punkte.length; i++) {
            for (int j = 0; j < punkte[0].length; j++) {
                if (punkte[i][j] == null) {
                    continue;
                }
                if (punkte[i][j].surfaceArea != null) {
                    continue;
                }
                LocalMinimumPoint t = punkte[i][j];
                //Untersten Punkt im Baum finden
                while (t.lowerPoint != null) {
                    t = t.lowerPoint;
                }
                //Minimum gefunden
                //Füge alle einlaufenden Dreiecke zu dieser Area hinzu
                SurfaceLocalMinimumArea a = new SurfaceLocalMinimumArea();
                a.points.add(t);
                a.points.addAll(t.addAllHoehereSurface(a.points));
//            System.out.println("Area" + a.id + " hat " + a.points.size() + " Punkte.");
//                ArrayList<Coordinate> cs = new ArrayList<>(a.points.size());
                for (LocalMinimumPoint point : a.points) {
                    point.surfaceArea = a;
//                    if (point.coordUTM != null) {
//                        cs.add(point.coordUTM);
//                    } else {
//                        System.out.println("Koordinate ist null");
//                    }

                }
                if (a.points.size() > 2) {
                    surfaceAreas.add(a);
                }

            }
        }
        System.out.println(surfaceAreas.size() + "Areas erstellt. Dauerte " + (System.currentTimeMillis() - start) + "ms");
    }

    public void areasSurfaceFlowFind() {
        if (surfaceAreas == null || surfaceAreas.isEmpty()) {
            areasSurfaceFind();
        }
        System.out.println("Find Surface Flowpaths when water has a depth of maximum value.");
        long start = System.currentTimeMillis();
        //////////////////////////////////////////////////////////////////////////////
        /// Wasser-stand Flächen finden
        /*
         Fläche auf den ursprünglichen Surface-Punkten finden
         */
        for (SurfaceLocalMinimumArea surface : surfaceAreas) {
//            FloodArea floodA = new FloodArea(surface, maxThreshold);
            //Alle Punkte Suchen, die unterhalb des stehenden Wasserspiegels stehen.
//            double maxZ = minimum[surface.points.getFirst().indexX][surface.points.getFirst().indexY] + maxThreshold;
            surface.overruns.clear();

//            if (surface.ueberlaufWasserstand_uNN < 100) {
//                maxZ = surface.ueberlaufWasserstand_uNN;
//            }
//            for (LocalMinimumPoint point : floodA.surfaceArea.points) {
//                point.lowerPointFlood = null;
//             
//                if (point.minZ < 1) {
//                    continue;
//                }
//                if (point.minZ <= maxZ) {
//                    floodA.floodPoints.add(point);
//                }
//
//            }
//            waterfloodAreas.add(floodA);
            /**
             * Suche die unter Wasser stehenden Flächen auf Überläufe zu
             * Nachbarareas ab
             */
            Pair<LocalMinimumPoint, LocalMinimumPoint> nachbar;
//            float nachbarZ = Float.POSITIVE_INFINITY;
            int x, y;
            LinkedList<Pair<LocalMinimumPoint, LocalMinimumPoint>> nachbarn = new LinkedList<>();

            for (LocalMinimumPoint fp : surface.points) {
                x = fp.indexX;
                y = fp.indexY;
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        if (i == 0 && j == 0) {
                            continue;
                        }
                        int xi = x + i;
                        int yj = y + j;

                        //Nachbarn über den Rand hinaus gibt es nicht
                        if (xi >= lengthX) {
                            continue;
                        }
                        if (yj >= lengthY) {
                            continue;
                        }
                        if (xi < 0 || yj < 0) {
                            continue;
                        }
                        if (minimum[xi][yj] < 1) {
                            continue;
                        }
                        LocalMinimumPoint kandidat = punkte[xi][yj];
                        if (kandidat == null) {
                            continue;
                        }
                        if (kandidat.surfaceArea.id == fp.surfaceArea.id) {
                            //Dieser Nachbar kommt aus der gleichen Area -> uninteressant
                            continue;
                        }
//                        if (kandidat.surfaceArea.flowArea != null && fp.surfaceArea.flowArea != null && kandidat.surfaceArea.flowArea.id == fp.surfaceArea.flowArea.id) {
//                            continue;
//                        }
                        nachbarn.add(new Pair<>(fp, kandidat));
//                        index++;
//                        float newZ = Math.max(fp.minZ, minimum[xi][yj]);
//                        if (newZ < nachbarZ) {
//                            if (surface.points.getFirst().minZ > kandidat.surfaceArea.points.getFirst().minZ) {
//                                nachbarZ = newZ;
//                                nachbar = kandidat;
//                            }
//                        }
                    }
                }
            }
            //Alle angrenzenden Nachbarn wurden gefunden.
            // Werte Überlauf aus.
            while (true) {
                nachbar = null;
                //Beginne mit der niedrigsten Area
                double z = Double.POSITIVE_INFINITY;
                int bestAreaIndex = -1;
                for (Pair<LocalMinimumPoint, LocalMinimumPoint> nb : nachbarn) {
                    double nbz = Math.max(nb.first.minZ, nb.second.minZ);
                    if (nbz < z) {
                        bestAreaIndex = nb.second.surfaceArea.id;
                        z = nbz;
                    }
                }

                if (bestAreaIndex < 0) {
                    //Keine Einträge mehr gefunden. -> nicht weiter suchen.
                    break;
                }
                z = Double.POSITIVE_INFINITY;
                //Suche alle Nachbarn, die zu dieser Area gehören.
                Iterator<Pair<LocalMinimumPoint, LocalMinimumPoint>> it = nachbarn.iterator();
                while (it.hasNext()) {
                    Pair<LocalMinimumPoint, LocalMinimumPoint> pair = it.next();
                    LocalMinimumPoint source = pair.first;
                    LocalMinimumPoint nb = pair.second;
                    if (nb.surfaceArea.id == bestAreaIndex) {
                        double nbz = Math.max(source.minZ, nb.minZ);
                        if (nbz < z) {
                            nachbar = pair;
                            z = nbz;
                        }
                        it.remove();
                    }
                }

                if (nachbar == null) {
                    continue;
                }

                /*Setze das mögliche Überlaufziel für den passenden Wasserstand
                 und füge diesen zur surfacearea hinzu.*/
                SurfaceOverrun overrun = new SurfaceOverrun();
                overrun.surfaceUeberlauf = nachbar.second.surfaceArea;
                overrun.ueberlaufWasserstand_uNN = z;
                overrun.ueberlaufWassertiefe = z - surface.points.getFirst().minZ;
                overrun.potentiellerUeberlaufZielPunkt = nachbar.second;
                surface.overruns.add(overrun);
            }
        }

        System.out.println(
                "Finding all potential overruns took " + (System.currentTimeMillis() - start) + "ms.");
    }

//    public void calculateFlowAreasDepth(SurfaceLocalMinimumArea surfaceArea, double waterdepth) {
//        LocalMinimumPoint tp = surfaceArea.points.getFirst();
//        
//        if (tp == null) {
//            System.out.println("Kein niedrigster Punkt gefunden.");
//            return;
//        }
////        double ueberlaufWassertiefe = Double.POSITIVE_INFINITY;
////        if (!surfaceArea.overruns.isEmpty()) {
//////            System.out.println("Area " + surfaceArea.id + " hat " + surfaceArea.overruns.size() + " overruns verzeichnet.");
////            for (SurfaceOverrun overrun : surfaceArea.overruns) {
////                if (overrun.ueberlaufWassertiefe <= waterdepth) {
////                    ueberlaufWassertiefe = Math.min(ueberlaufWassertiefe, overrun.ueberlaufWassertiefe);
////                }
////            }
////        } else {
//////            System.out.println("calculateFlowAreasDepth: overruns sind leer");
////        }
//        double z = tp.minZ + waterdepth;// Math.min(waterdepth, ueberlaufWassertiefe);
//        calculateFlowAreasHeight(surfaceArea, z);
//    }
    /**
     * Werte der Wasserhöhe in der Surfacearea werden nicht herabgesetzt.
     *
     * @param surfaceArea
     */
    public void calculateFlowAreas(SurfaceLocalMinimumArea surfaceArea) {
        double waterheight_uNN = surfaceArea.waterHeight;
        FlowArea flowArea;

        SurfaceOverrun selectedOverrun = null;
        if (!surfaceArea.overruns.isEmpty()) {
            for (SurfaceOverrun overrun1 : surfaceArea.overruns) {
                overrun1.ueberlauf = false;

                if (overrun1.ueberlaufWasserstand_uNN >= waterheight_uNN) {
                    //Nur auswählen, wenn überlaufhöhe erreicht wurde.
                    continue;
                }
                if (overrun1.surfaceUeberlauf.massgebendOverrun != null && overrun1.surfaceUeberlauf.massgebendOverrun.surfaceUeberlauf.id == surfaceArea.id) {
                    //Es soll sich nicht im Kreis drehen.
                    continue;
                }
                if (overrun1.surfaceUeberlauf.getLowestPoint().minZ > surfaceArea.getLowestPoint().minZ) {
                    //Von niedrigstem Punkt aus nicht weiter laufen.
                    continue;
                }
//                if (overrun1.surfaceUeberlauf.floodArea != null && overrun1.surfaceUeberlauf.floodArea.waterheight > overrun1.ueberlaufWasserstand_uNN) {
//                    //Nicht in höheren Wasserstand fließen.
//                    continue;
//                }
                if (selectedOverrun != null) {
                    //Teste auf niedrigsten wasserstand im ziel
                    if (selectedOverrun.surfaceUeberlauf.waterHeight > overrun1.surfaceUeberlauf.waterHeight) {
                        //Nehme den niedrigeren Wasserstand als Ziel
                        selectedOverrun = overrun1;
                    }
                } else {
                    selectedOverrun = overrun1;
                }

            }
        }

        if (selectedOverrun != null) {

            //Fläche läuft über
//            System.out.println("Überlauf");
            SurfaceLocalMinimumArea ziel = selectedOverrun.surfaceUeberlauf;
            selectedOverrun.ueberlauf = true;
            surfaceArea.massgebendOverrun = selectedOverrun;
            surfaceArea.points.getFirst().lowerPointFlood = selectedOverrun.potentiellerUeberlaufZielPunkt;
            if (ziel == null) {
                throw new NullPointerException("Ziel SurfaceArea" + selectedOverrun.surfaceUeberlauf + " konnte nicht gefunden werden.");
            }
            if (ziel.flowArea != null) {
                flowArea = ziel.flowArea;
//                System.out.println("  nutze Ziel Flowarea");
                flowArea.surfaces.add(surfaceArea);
            } else {
                flowArea = new FlowArea(ziel.id);
                flowArea.surfaces.add(ziel);
                flowArea.surfaces.add(surfaceArea);
                flowAreas.add(flowArea);
                ziel.flowArea = flowArea;
//                System.out.println("  erstelle Ziel Flowarea");
            }
            if (surfaceArea.flowArea != null && surfaceArea.flowArea != flowArea) {
                flowAreas.remove(surfaceArea.flowArea);
//                System.out.println("    ersetze bestehende Flowarea");
                for (SurfaceLocalMinimumArea s : surfaceArea.flowArea.surfaces) {
                    s.flowArea = flowArea;
                    if (!flowArea.surfaces.contains(s)) {
                        flowArea.surfaces.add(s);
                    }
                }
                surfaceArea.flowArea = flowArea;
            } else {
//                System.out.println("    füge Punkte zu flowArea hinzu");
                surfaceArea.flowArea = flowArea;
            }
        } else {
            surfaceArea.points.getFirst().lowerPointFlood = null;
//            System.out.println("Kein Überlauf");
            if (surfaceArea.flowArea == null) {
                flowArea = new FlowArea(surfaceArea.id);
                flowAreas.add(flowArea);
                flowArea.surfaces.add(surfaceArea);
                surfaceArea.flowArea = flowArea;
//                System.out.println("  erstelle Flowarea");
            } else {
//                System.out.println("  Flowarea besteht bereits");
            }
        }
    }

    public void calculateFlowAreas1(SurfaceLocalMinimumArea surfaceArea) {
//        double z = waterheight_uNN;
        double waterheight_uNN = surfaceArea.waterHeight;
        FlowArea flowArea;

        SurfaceOverrun overrun = null;
        if (!surfaceArea.overruns.isEmpty()) {
            for (SurfaceOverrun overrun1 : surfaceArea.overruns) {
                if (overrun1.surfaceUeberlauf.massgebendOverrun != null && overrun1.surfaceUeberlauf.massgebendOverrun.surfaceUeberlauf.id == surfaceArea.id) {
                    //Es soll sich nicht im Kreis drehen.
                    continue;
                }
                if (overrun1.surfaceUeberlauf.floodArea != null && overrun1.surfaceUeberlauf.floodArea.waterheight > overrun1.ueberlaufWasserstand_uNN) {
                    //Nicht in höheren Wasserstand fließen.
                    continue;
                }

                if (overrun1.ueberlaufWasserstand_uNN < waterheight_uNN) {

                    if (!overrun1.surfaceUeberlauf.overruns.isEmpty()) {

                        if (overrun1.surfaceUeberlauf.floodArea != null && overrun1.surfaceUeberlauf.floodArea.waterheight < waterheight_uNN) {
                            //Wasser kann Problemlos in anderes Becken hinabfließen.
                            overrun = overrun1;
                            break;
                        }

                        if (overrun1.surfaceUeberlauf.overruns.get(0).ueberlaufWasserstand_uNN <= overrun1.ueberlaufWasserstand_uNN) {
                            //Nur wenn dieser FLuss nicht gleich wieder zurück fließt
                            overrun = overrun1;
                            break;
                        }
//                        else if (overrun1.surfaceUeberlauf.overruns.get(0).ueberlaufWasserstand_uNN == overrun1.ueberlaufWasserstand_uNN) {
//                            //Wechselseitiges Einlaufen, soll dann in Richtung der größeren Fläche laufen
//                            if (overrun1.surfaceUeberlauf.points.size() > surfaceArea.points.size()) {
//                                //Nur wenn dieser FLuss nicht gleich wieder zurück fließt
//                                overrun = overrun1;
//                                break;
//                            }
//                        }
                    } else {
                        //Oder auswählen, wenn das Ziel ein Endpunkt ist.
                        overrun = overrun1;
                        break;
                    }

                }
            }
        }

        if (overrun != null) {

            //Fläche läuft über
//            System.out.println("Überlauf");
            SurfaceLocalMinimumArea ziel = overrun.surfaceUeberlauf;
            overrun.ueberlauf = true;
            surfaceArea.massgebendOverrun = overrun;
            surfaceArea.points.getFirst().lowerPointFlood = overrun.potentiellerUeberlaufZielPunkt;
            if (ziel == null) {
                throw new NullPointerException("Ziel SurfaceArea" + overrun.surfaceUeberlauf + " konnte nicht gefunden werden.");
            }
            if (ziel.flowArea != null) {
                flowArea = ziel.flowArea;
//                System.out.println("  nutze Ziel Flowarea");
                flowArea.surfaces.add(surfaceArea);
            } else {
                flowArea = new FlowArea(ziel.id);
                flowArea.surfaces.add(ziel);
                flowArea.surfaces.add(surfaceArea);
                flowAreas.add(flowArea);
                ziel.flowArea = flowArea;
//                System.out.println("  erstelle Ziel Flowarea");
            }
            if (surfaceArea.flowArea != null && surfaceArea.flowArea != flowArea) {
                flowAreas.remove(surfaceArea.flowArea);
//                System.out.println("    ersetze bestehende Flowarea");
                for (SurfaceLocalMinimumArea s : surfaceArea.flowArea.surfaces) {
                    s.flowArea = flowArea;
                    if (!flowArea.surfaces.contains(s)) {
                        flowArea.surfaces.add(s);
                    }
                }
                surfaceArea.flowArea = flowArea;
            } else {
//                System.out.println("    füge Punkte zu flowArea hinzu");
                surfaceArea.flowArea = flowArea;
            }
        } else {
            surfaceArea.points.getFirst().lowerPointFlood = null;
//            System.out.println("Kein Überlauf");
            if (surfaceArea.flowArea == null) {
                flowArea = new FlowArea(surfaceArea.id);
                flowAreas.add(flowArea);
                flowArea.surfaces.add(surfaceArea);
                surfaceArea.flowArea = flowArea;
//                System.out.println("  erstelle Flowarea");
            } else {
//                System.out.println("  Flowarea besteht bereits");
            }
        }
    }

//    public void calculateFloodAreasDepth(SurfaceLocalMinimumArea surfaceArea, double waterdepth) {
//        LocalMinimumPoint tp = surfaceArea.points.getFirst();
//
//        if (tp == null) {
//            return;
//        }
//        double z = tp.minZ + waterdepth;
//        calculateFloodAreasHeight(surfaceArea, z);
//    }
    public void calculateFloodAreas(SurfaceLocalMinimumArea surfaceArea) {
        double z = surfaceArea.waterHeight;
//        boolean ueberlauf = false;
//        SurfaceOverrun overrun = null;
        if (!surfaceArea.overruns.isEmpty()) {
            for (SurfaceOverrun run : surfaceArea.overruns) {
//                z = Math.min(z, run.ueberlaufWasserstand_uNN);
                if (run.ueberlaufWasserstand_uNN <= z) {
//                    overrun = run;
                    break;
                }
            }
        }

        FloodArea floodArea = new FloodArea(surfaceArea, z);
        for (LocalMinimumPoint point : surfaceArea.points) {
            if (point.minZ <= z) {
                floodArea.floodPoints.add(point);
//                if (overrun != null) {
//                    point.lowerPointFlood = overrun.potentiellerUeberlaufZielPunkt;
//                } else {
//                    point.lowerPointFlood = null;
//                }
            }
        }
        surfaceArea.floodArea = floodArea;
    }

    public void areasSurfaceDarstellen() {
        System.out.println("Surface Areas Shapes erstellen...");
        Color transparent = new Color(0, 0, 0, 0);
        DoubleColorHolder chi = new DoubleColorHolder(Color.white, transparent, "Oberflächen");
        long start = System.currentTimeMillis();
        int counter = 0;
        Iterator<SurfaceLocalMinimumArea> it = surfaceAreas.iterator();
        while (it.hasNext()) {
            SurfaceLocalMinimumArea a = it.next();
            try {
                if (a.points.isEmpty()) {
                    it.remove();
                } else {
                    if (a.geomUTMOuterBoundary == null) {
                        a.geomUTMOuterBoundary = buildOuterPolygon(a.points);
                    }
                    AreaPainting ap = new AreaPainting(a.id, chi, transform(a.geomUTMOuterBoundary, utm32wgs));
                    frame.mapViewer.addPaintInfoToLayer(layerSurfaceArea, ap);
                    counter++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        System.out.println(counter + " Surface Areas Shapes erstellt. Dauerte " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void niedrigsteSurfacePunkteDarstellen() {
        int counter = 0;
        Iterator<SurfaceLocalMinimumArea> it = surfaceAreas.iterator();
        while (it.hasNext()) {
            try {
                SurfaceLocalMinimumArea a = it.next();
                //Niedrigsten Punkt bauen
                ColorHolder chn = new ColorHolder(Color.MAGENTA);
                NodePainting np = new NodePainting(a.id, transform(gf.createPoint(a.points.getFirst().coordUTM), utm32wgs).getCoordinate(), chn);
                frame.mapViewer.addPaintInfoToLayer("N", np);

            } catch (MismatchedDimensionException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (TransformException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void niedrigsteFloodFlowPunkteDarstellen() {
        System.out.println("Niedrigste Punkte markieren...");
        int counter = 0;
        ColorHolder chn = new ColorHolder(Color.MAGENTA, "Niedrigste Punkte");
        chn.setStroke(new BasicStroke(2));
        Iterator<FlowArea> it = flowAreas.iterator();
        while (it.hasNext()) {
            try {
                FlowArea a = it.next();
//                System.out.println("flowarea: "+a);
                if (a == null) {
                    continue;
                }
                //Niedrigsten Punkt finden
                LocalMinimumPoint nip = a.surfaces.getFirst().points.getFirst();
                double z = Double.POSITIVE_INFINITY;
//                for (SurfaceLocalMinimumArea surface : a.surfaces) {
//                    LocalMinimumPoint testp = surface.points.getFirst();
//                    if (testp.minZ < z) {
//                        nip = testp;
//                        z = testp.minZ;
//                    }
//                }
                //Niedrigsten Punkt bauen
                NodePainting np = new NodePainting(a.id, transform(nip.coordUTM, utm32wgs), chn);
//                System.out.println("niedrigster Punkt");
                frame.mapViewer.addPaintInfoToLayer("FN", np);

            } catch (MismatchedDimensionException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (TransformException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Niedrigste Punkte markiert.");
    }

    public void areasFlowDarstellen(boolean onlyCalculateConvexHull) {
        System.out.println("Flow Areas Shapes erstellen...");
        long start = System.currentTimeMillis();
        int counter = 0;
        int counterflood = 0;
        Iterator<FlowArea> it = flowAreas.iterator();
        while (it.hasNext()) {
            FlowArea a = it.next();
            try {
                LinkedList<LocalMinimumPoint> points = new LinkedList<>();
                for (SurfaceLocalMinimumArea surface : a.surfaces) {
                    points.addAll(surface.points);
                }
                if (points.isEmpty()) {
                    it.remove();
                } else {

                    Color ac = new Color((int) (Math.random() * Integer.MAX_VALUE));
                    ac = new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 100);
                    DoubleColorHolder chi = new DoubleColorHolder(Color.white, ac, "Abflussflächen");
                    if (onlyCalculateConvexHull) {
                        a.geomUTMOuterBoundary = buildConvexHull(points);
                        AreaPainting ap = new AreaPainting(a.id, chi, transform(a.geomUTMOuterBoundary, utm32wgs));
                        frame.mapViewer.addPaintInfoToLayer(layerFlowArea, ap);
                    } else {
                        a.geomUTMOuterBoundary = buildOuterPolygon(points);
                        AreaPainting ap = new AreaPainting(a.id, chi, transform(((Polygon) a.geomUTMOuterBoundary).getExteriorRing(), utm32wgs));
                        frame.mapViewer.addPaintInfoToLayer(layerFlowArea, ap);
                    }

                    //Niedrigsten Punkt bauen
//                    ColorHolder chn = new ColorHolder(chi.getFillColor().darker());
//                    NodePainting np = new NodePainting(a.id, transform(gf.createPoint(a.points.getFirst().coordUTM), utm32wgs).getCoordinate(), chn);
//                    frame.mapViewer.addPaintInfoToLayer("N", np);
                    counterflood++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        System.out.println(counterflood + " Flow Areas dargestellt " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void areasFloodDarstellen() {
        System.out.println("Areas Flood darstellen...");
        long start = System.currentTimeMillis();
        int zaehler = 0;
        Color fillColor = new Color(0, 100, 230, 150);
        ColorHolder chflood = new ColorHolder(fillColor, "Flooded Points");
        chflood.setStroke(new BasicStroke(2));
        for (SurfaceLocalMinimumArea s : surfaceAreas) {
            FloodArea wfa = s.floodArea;
            if (wfa == null) {
                continue;
            }
            try {
                if (wfa.floodPoints.size() > 0) {
                    wfa.geomUTM = buildOuterPolygon(wfa.floodPoints);
                    DoubleColorHolder chi = new DoubleColorHolder(Color.CYAN, fillColor, "Pfützen");
                    AreaPainting ap = new AreaPainting(wfa.id, chi, transform(wfa.geomUTM, utm32wgs));
                    frame.mapViewer.addPaintInfoToLayer(layerFloodArea, ap);
                    zaehler++;

                }
            } catch (MismatchedDimensionException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (TransformException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        frame.mapViewer.repaint();
        System.out.println(zaehler + " Flood Areas Shapes erstellt. Dauerte " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void netzwerkMappen(Network network) throws ClassNotFoundException, Exception {
        System.out.println("Lese Netwerk...");
        long start = System.currentTimeMillis();
        this.network = network;
        System.out.println("Mappe " + network.getManholes().size() + " Manholes auf Flächen");
        mhaMapIn = new HashMap<>(network.getManholes().size());
        mhaMapOut = new HashMap<>(network.getManholes().size());
//        HashSet<SurfaceLocalMinimumArea> areas = surfaceAreas;
        System.out.println("  netzwerk gelesen. " + (System.currentTimeMillis() - start) + "ms");
//        for (SurfaceLocalMinimumArea a : surfaceAreas) {
//            if (a.points.size() < 3) {
//                continue;
//            }
//            if (a.geomUTMOuterBoundary == null) {
//                a.geomUTMOuterBoundary = buildOuterPolygon(a.points);
//            }

        //Tiefster Punkt
//            LocalMinimumPoint tiefst = a.points.getFirst();
//            Point point = gf.createPoint(tiefst.coordUTM);
        //Suche Straßeneinlauf (Pipe in der Nähe)
//            Pipe bestPipe = null;
//            double bestDistance = Double.POSITIVE_INFINITY;
//            for (Pipe pipe : network.getPipes()) {
//                LineString ls = gf.createLineString(new Coordinate[]{
//                    new Coordinate(pipe.getStartConnection().getPosition().x, pipe.getStartConnection().getPosition().y),
//                    new Coordinate(pipe.getEndConnection().getPosition().x, pipe.getEndConnection().getPosition().y)});
//                double d = point.distance(ls);
//                if (d < bestDistance) {
//                    //Maximalabstand 30m sonst kommt diese Pipe nicht in Frage
//                    if (d < 20) {
//                        bestDistance = d;
//                        bestPipe = pipe;
//
//                    }
//                }
//            }
//            a.einlaufPipe = bestPipe;
//        }
        for (Manhole mh : network.getManholes()) {
            Coordinate c = new Coordinate(mh.getPosition().x, mh.getPosition().y);
//            Point p = gf.createPoint(c);

            for (SurfaceLocalMinimumArea a : surfaceAreas) {
                if (a.points.size() < 3) {
                    continue;
                }
                if (a.geomUTMOuterBoundary.contains(gf.createPoint(c))) {
                    if (a.manholeList == null) {
                        a.manholeList = new LinkedList<>();
                    }
                    a.manholeList.add(mh);
//                    System.out.println(" ist drin! ");
                    break;
                }
            }
        }
        System.out.println("  manholes found. " + (System.currentTimeMillis() - start) + "ms");

    }

    public void overflowPathsFinding() {

        System.out.println("Oberflächenwege zwischen Manholes bestimmen...");
        long start = System.currentTimeMillis();
        sammelPunkte = new LocalPoolPoint[lengthX][lengthY];

        //Manholes in Raster einsetzen
        for (Manhole manhole : network.getManholes()) {
            Coordinate c = new Coordinate(manhole.getPosition().x, manhole.getPosition().y);
            int iX = getXindex(c.x);
            int iY = getYindex(c.y);
            if (sammelPunkte[iX][iY] != null) {
                System.out.println("Punkt an " + iX + "," + iY + " ist bereits belegt mit " + sammelPunkte[iX][iY] + ". Kann hier nicht platzieren:" + manhole);
            } else {
                LocalManholePoint mhp = new LocalManholePoint(punkte[iX][iY], manhole);
                sammelPunkte[iX][iY] = mhp;
            }
        }
        //Inlets in Raster einsetzen
        if (network.getStreetInlets() != null) {
            ColorHolder chStreetInlet = new ColorHolder(Color.cyan, "Straßeneinlauf");
            int i = 0;
            for (Inlet in : network.getStreetInlets()) {
//                Coordinate c = new Coordinate(in.getPosition().y, in.getPosition().x);
                int iX = getXindex(in.getPosition().x);
                int iY = getYindex(in.getPosition().y);
                if (sammelPunkte[iX][iY] != null) {
//                    System.out.println("Punkt an " + iX + "," + iY + " ist bereits belegt mit " + sammelPunkte[iX][iY] + ". Kann hier Inlet nicht platzieren:" + in);
                } else {
                    LocalPoolPoint mhp = new LocalPoolPoint(punkte[iX][iY]);
                    sammelPunkte[iX][iY] = mhp;
                }
                sammelPunkte[iX][iY].inlet = in;

                NodePainting np = new NodePainting(i++, in.getPosition(), chStreetInlet);
                frame.mapViewer.addPaintInfoToLayer(layerStreetInlet, np);

                Position pos2 ;
//                if(in.getNetworkCapacity() instanceof Pipe){
//                    pos2= ((Pipe) in.getNetworkCapacity()).getPositionAlongAxisAbsolute(in.getPipeposition1d());
//                }else{
                    pos2=in.getNetworkCapacity().getPosition3D(in.getPipeposition1d());
//                }
                ArrowPainting ap = new ArrowPainting(i++, new Coordinate[]{new Coordinate(in.getPosition().getLatitude(), in.getPosition().getLongitude()), new Coordinate(pos2.getLatitude(), pos2.getLongitude())}, chStreetInlet);
                frame.mapViewer.addPaintInfoToLayer(layerStreetInletConnection, ap);

            }
        }

        //Weitere Tiefpunkte erstellen
        for (SurfaceLocalMinimumArea s : surfaceAreas) {
            Coordinate c = s.points.getFirst().coordUTM;
            int iX = getXindex(c.x);
            int iY = getYindex(c.y);
            if (sammelPunkte[iX][iY] == null) {
                LocalPoolPoint lp = new LocalPoolPoint(punkte[iX][iY]);
                sammelPunkte[iX][iY] = lp;
            }
            sammelPunkte[iX][iY].isSurfaceDeepest = true;
            if (punkte[iX][iY].surfaceArea.overruns.isEmpty()) {
                sammelPunkte[iX][iY].isFlowDeepest = true;
            }
        }

        //FlowPaths erstellen
        paths.clear();
        /**
         * Liste an Startpunkten von denen aus die Pfade erstellt werden.
         */
        LinkedList<LocalPoolPoint> pathStartPoints = new LinkedList<>();
        LinkedList<LocalPoolPoint> checkedPoints = new LinkedList<>();
        //Ausgangspunkt ist jeweils ein Manhole
        for (Manhole manhole : network.getManholes()) {
            int iX = getXindex(manhole.getPosition().x);
            int iY = getYindex(manhole.getPosition().y);
            pathStartPoints.add(sammelPunkte[iX][iY]);
        }
        while (!pathStartPoints.isEmpty()) {
            LocalPoolPoint poolStart = pathStartPoints.removeLast();
            if (poolStart == null) {
                continue;
            }
            checkedPoints.add(poolStart);
            LinkedList<Pair<LocalMinimumPoint, Double>> secondPoints = new LinkedList<>();
            if (poolStart.lowerPoint != null) {
                secondPoints.add(new Pair<>(poolStart.lowerPoint.lowerPoint, (double) poolStart.minZ));
            } else {
                for (SurfaceOverrun o : poolStart.surfaceArea.overruns) {
                    secondPoints.add(new Pair<>(o.potentiellerUeberlaufZielPunkt, o.ueberlaufWasserstand_uNN));
                }
            }
            while (!secondPoints.isEmpty()) {
                LocalMinimumPoint aktuell, naechster;
                Pair<LocalMinimumPoint, Double> pair = secondPoints.removeLast();
                aktuell = pair.first;

                if (aktuell == null) {
                    continue;
                }
                double ueberlauf = pair.second;

                if (aktuell == null) {
                    throw new NullPointerException("Startpunkt für " + poolStart + " fehlt.");
                }
                LinkedList<FlowSegment> segments = new LinkedList<>();
                segments.add(new FlowSegment(poolStart, aktuell));

                LocalPoolPoint poolEnd = null;

//                if (aktuell.lowerPoint == null) {
////                System.out.println("nachfolger unter start ist null");
//                    continue;
//                }
//            System.out.println(" neuer Flowpath");
                while (true) {
                    naechster = aktuell.lowerPoint;

                    if (naechster == null) {
//                        System.out.println("Fehler naechster ist null");
                        break;
                    }
                    //teste Verbindung auf Sammelpunkt
                    if (sammelPunkte[naechster.indexX][naechster.indexY] != null) {
                        poolEnd = sammelPunkte[naechster.indexX][naechster.indexY];
                        segments.add(new FlowSegment(aktuell, poolEnd));
                        break;
                    }

                    //nichts besonderes. 
                    // erstelle Segment
                    segments.add(new FlowSegment(aktuell, naechster));
                    if (naechster.lowerPoint == null) {
                        LocalPoolPoint lp = new LocalPoolPoint(naechster);
                        sammelPunkte[naechster.indexX][naechster.indexY] = lp;
                        poolEnd = lp;
//                    System.out.println("naechster.lowerpoint=null");
                        break;
                    } else {
//                    System.out.println("naechster.lowerpoint="+naechster.lowerPoint);
                    }
                    aktuell = naechster;
                }
                if (poolEnd == null) {
                    //Nichts tun falls nichts gefunden wurde.
                    continue;
                }
                //Flowpath für diesen LocalManhole Punkt setzen.

                FlowPath path = new FlowPath(poolStart, poolEnd, segments, ueberlauf);

                poolStart.addOutgoingFlowPath(path);
                poolEnd.getFlowPathsIncoming().add(path);
                paths.add(path);

                if (!checkedPoints.contains(poolEnd) && !pathStartPoints.contains(poolEnd)) {
                    pathStartPoints.addLast(poolEnd);
                }
            }
        }
//        while(false){
////Flowpath ab Ende dieses Flowpaths weiterverfolgen
//            while (punkte[poolEnd.indexX][poolEnd.indexY].lowerPointFlood != null || punkte[poolEnd.indexX][poolEnd.indexY].lowerPoint != null) {
//                //Weiteren Pfad erstellen
//                segments = new LinkedList<>();
//                poolStart = poolEnd;
//                poolEnd = null;
//                aktuell = poolStart;
//                if (aktuell == null) {
//                    throw new NullPointerException("Startpunkt für Weiterverfolgung fehlt.");
//                }
//                if (aktuell.lowerPoint == null && aktuell.lowerPointFlood == null) {
//                    System.out.println("nachfolger der weitervefolgung ist null");
//                    continue;
//                }
//                if (aktuell.lowerPoint != null) {
//                    naechster = aktuell.lowerPoint;
//                } else {
//                    //Ist ein tiefpunkt
//                    naechster = aktuell.lowerPointFlood;
//                }
//
////                System.out.println(" weiterfolgender Flowpath");
//                while (true) {
//
//                    if (naechster == null) {
//                        System.out.println("Fehler naechster ist null");
//                    }
//                    //teste Verbindung auf Sammelpunkt
//                    if (sammelPunkte[naechster.indexX][naechster.indexY] != null) {
//                        poolEnd = sammelPunkte[naechster.indexX][naechster.indexY];
//                        segments.add(new FlowSegment(aktuell, poolEnd));
//                        break;
//                    }
//
//                    //nichts besonderes. 
//                    // erstelle Segment
//                    segments.add(new FlowSegment(aktuell, naechster));
//                    if (naechster.lowerPoint == null) {
//                        LocalPoolPoint lp = new LocalPoolPoint(naechster);
//                        sammelPunkte[naechster.indexX][naechster.indexY] = lp;
//                        poolEnd = lp;
//                        System.out.println("naechster.lowerpoint=null");
//                        break;
//                    } else {
////                    System.out.println("naechster.lowerpoint="+naechster.lowerPoint);
//                    }
//                    aktuell = naechster;
//                    naechster = aktuell.lowerPoint;
//                }
//                //Flowpath für diesen LocalManhole Punkt setzen.
//                path = new FlowPath(poolStart, poolEnd, segments, poolStart.minZ);
//
//                //Test if such path already exists
//                boolean alreadyFound = false;
//                for (FlowPath o : poolStart.getFlowPathsOutgoing()) {
//                    if (o.getStart().equals(path.getStart()) && o.getTarget().equals(path.getTarget())) {
//                        alreadyFound = true;
//                        System.out.println("  Diesen Pfad gibt es schon.");
//                        break;
//                    }
//                }
//                if (!alreadyFound) {
//                    poolStart.addOutgoingFlowPath(path);
//                    poolEnd.getFlowPathsIncoming().add(path);
//                    paths.add(path);
//                } else {
//                    break;
//                }
//            }
//        }
    }

    public void networkOverflowPathsDarstellen() throws MismatchedDimensionException, TransformException {
        System.out.println("Overflow Paths darstellen...");
        pathIDMap.clear();
        frame.mapViewer.clearLayer(layerFlowPath);
        long start = System.currentTimeMillis();
        BasicStroke bs2 = new BasicStroke(2);
//        ColorHolder chEinleitungRohr = new ColorHolder(Color.magenta, "Einleitung Rohr");
//        ColorHolder chEinleitung = new ColorHolder(Color.green, "Einleitung");
//        chEinleitungRohr.setStroke(bs2);
//        chEinleitung.setStroke(bs2);
        ColorHolder chAustritt = new ColorHolder(Color.orange, "Austritt");
        chAustritt.setStroke(bs2);

        ColorHolder chManhole = new ColorHolder(Color.red, "Schacht");
        ColorHolder chPipe = new ColorHolder(Color.red, "Rohr");

        chManhole.setStroke(bs2);

        for (SurfaceLocalMinimumArea a : surfaceAreas) {
            if (a.manholeList != null) {
                for (Manhole mh : a.manholeList) {
                    Coordinate cmh = new Coordinate(mh.getPosition().x, mh.getPosition().y);
                    if (a.floodArea != null && a.floodArea.geomUTM != null && a.floodArea.geomUTM.contains(gf.createPoint(cmh))) {
                        //Einleitung
                        ArrowPainting ap = new ArrowPainting(mh.getAutoID(), new Coordinate[]{transform(a.floodArea.floodPoints.getFirst().coordUTM, utm32wgs), transform(cmh, utm32wgs)}, chAustritt);
                        frame.mapViewer.addPaintInfoToLayer(layerEinleitung, ap);
                    } else {
                        //Ausfluss
                        //Verfolge Ausfluss bis zum niedrigsten Punkt
                        int x = getXindex(cmh.x);
                        int y = getYindex(cmh.y);
                        LocalMinimumPoint lm = punkte[x][y];
                        if (lm != null) {
                            LinkedList<Coordinate> coordinates = new LinkedList<>();
                            coordinates.add(cmh);
                            coordinates.add(lm.coordUTM);
                            int counter = 0;
                            while (true) {
                                counter++;
                                if (lm.lowerPointFlood != null) {
                                    lm = lm.lowerPointFlood;
                                } else {
                                    lm = lm.lowerPoint;
                                }
                                if (counter > 1000) {
                                    System.out.println("Pfad ist abgebrochen nach " + counter + " Teilen.");
                                }
                                if (lm == null || lm.coordUTM == null || counter > 1000) {
                                    //Tiefster Punkt gefunden
                                    LineString ls = gf.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
                                    ArrowPainting ap = new ArrowPainting(mh.getAutoID(), transform(ls, utm32wgs), chAustritt);
                                    frame.mapViewer.addPaintInfoToLayer(layerAustritt, ap);
                                    break;
                                }
                                coordinates.add(lm.coordUTM);
                            }
                        } else {
                            ArrowPainting ap = new ArrowPainting(mh.getAutoID(), new Coordinate[]{transform(cmh, utm32wgs), transform(a.points.getFirst().coordUTM, utm32wgs)}, chAustritt);
                            frame.mapViewer.addPaintInfoToLayer(layerAustritt, ap);
                        }
                    }
                }
            }
        }
        System.out.println(" Ausflussverfolgungspfade gezeichnet. " + (System.currentTimeMillis() - start) + "ms");
        for (Manhole manhole : network.getManholes()) {
            NodePainting np = new NodePainting(manhole.getAutoID(), manhole.getPosition(), chManhole);
            frame.mapViewer.addPaintInfoToLayer(layerManhole, np);
        }
        for (Pipe pipe : network.getPipes()) {
            ArrayList<GeoPosition2D> list = new ArrayList<>(2);
            list.add(pipe.getStartConnection().getPosition());
            list.add(pipe.getEndConnection().getPosition());
            ArrowPainting np = new ArrowPainting(pipe.getAutoID(), list, chPipe);
            frame.mapViewer.addPaintInfoToLayer(layerPipe, np);
        }

        //Darstellen
        ColorHolder ch = new ColorHolder(Color.magenta, "Fließwege potential");
        ColorHolder cha = new ColorHolder(Color.GREEN, "Fließwege aktiv");
        cha.setStroke(bs2);
        this.pathIDMap = new HashMap<>(paths.size());
        int id = 0;
        for (FlowPath path : paths) {
            try {
                Coordinate[] pos = new Coordinate[path.getSegments().size() + 1];
                int i = 0;
                for (FlowSegment s : path.getSegments()) {
                    pos[i++] = (s.getStart().coordUTM);
                }
                pos[i] = path.getTarget().coordUTM;
                pathIDMap.put(id, path);
                ArrowPainting ap = new ArrowPainting(id, transform(pos, utm32wgs), ch);
                if (path.getStart().surfaceArea.waterHeight >= path.activatingWaterheightAtStart) {
                    //Overrun Path is active
                    ap.setColor(cha);
                    frame.mapViewer.addPaintInfoToLayer(layerFlowPathactive, ap);
                } else {
                    //Overrun is not active
                    frame.mapViewer.addPaintInfoToLayer(layerFlowPath, ap);
                }

                id++;
            } catch (MismatchedDimensionException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (TransformException ex) {
                Logger.getLogger(SurfaceTools.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("Oberflächen Netzwerk erstellt in " + (System.currentTimeMillis() - start) + "ms.");

    }

    private void flowArrowsSurfaceDarstellen() throws MismatchedDimensionException, TransformException {
        ColorHolder chArrow = new ColorHolder(Color.yellow, "Strömung");
        chArrow.setStroke(new BasicStroke(1));
        for (int i = 0; i < zielKoordinaten.length; i++) {
            for (int j = 0; j < zielKoordinaten[0].length; j++) {
                if (zielKoordinaten[i][j][0] == 0 && zielKoordinaten[i][j][1] == 0) {
                    continue;
                }
                LocalMinimumPoint zielt = punkte[zielKoordinaten[i][j][0]][zielKoordinaten[i][j][1]];
                if (zielt == null) {
                    continue;
                }

                Coordinate start = punkte[i][j].coordUTM;
                Coordinate ziel = zielt.coordUTM;
                start = transform(start, utm32wgs);
                ziel = transform(ziel, utm32wgs);

                ArrowPainting ap = new ArrowPainting(i * lengthY + j, new Coordinate[]{start, ziel}, chArrow);
                frame.mapViewer.addPaintInfoToLayer(layerFlowArrow, ap);
            }
        }
        frame.mapViewer.repaint();
//
//        Coordinate c2 = new Coordinate((rX * breite / lengthX) + utmXmin, (rY * hoehe / lengthY) + utmYmin);

    }

    private void flowArrowsFloodDarstellen() throws MismatchedDimensionException, TransformException {
        ColorHolder chArrow = new ColorHolder(Color.cyan, "Überlauf");
        chArrow.setStroke(new BasicStroke(3));

        for (SurfaceLocalMinimumArea surfaceFlowArea : surfaceAreas) {
            if (surfaceFlowArea.flowArea != null && surfaceFlowArea.points.getFirst().lowerPointFlood != null) {
                Coordinate start = surfaceFlowArea.points.getFirst().coordUTM;

                Coordinate ziel = surfaceFlowArea.points.getFirst().lowerPointFlood.coordUTM;
                start = transform(start, utm32wgs);
                ziel = transform(ziel, utm32wgs);

                ArrowPainting ap = new ArrowPainting(surfaceFlowArea.id, new Coordinate[]{start, ziel}, chArrow);
                frame.mapViewer.addPaintInfoToLayer(layerFlowArrowFlood, ap);
            }
        }
        frame.mapViewer.repaint();
//
//        Coordinate c2 = new Coordinate((rX * breite / lengthX) + utmXmin, (rY * hoehe / lengthY) + utmYmin);

    }

    private Polygon buildOuterPolygon(List<LocalMinimumPoint> points) {
        double xb = gitterWeiteX / 2. + 0.01;
        double yb = gitterWeiteY / 2. + 0.01;
        Coordinate[] cs = new Coordinate[5];
        ArrayList<Geometry> rechtecke = new ArrayList<>(points.size());
        for (LocalMinimumPoint point : points) {
            cs[0] = new Coordinate(point.coordUTM.x - xb, point.coordUTM.y - yb);
            cs[1] = new Coordinate(point.coordUTM.x - xb, point.coordUTM.y + yb);
            cs[2] = new Coordinate(point.coordUTM.x + xb, point.coordUTM.y + yb);
            cs[3] = new Coordinate(point.coordUTM.x + xb, point.coordUTM.y - yb);
            cs[4] = cs[0];

            Geometry p0 = gf.createPolygon(cs).buffer(0);

            rechtecke.add(p0);

        }

        Geometry geo = CascadedPolygonUnion.union(rechtecke);

        if (geo instanceof Polygon) {
            return ((Polygon) geo);
        } else {
            System.out.println("Created Geometry is " + geo);
            System.out.println("   points: " + points.size());
        }
        return null;
    }

    private Geometry buildConvexHull(List<LocalMinimumPoint> points) {
        double xb = gitterWeiteX / 2. + 0.01;
        double yb = gitterWeiteY / 2. + 0.01;
        Coordinate[] cs = new Coordinate[points.size()];
        ArrayList<Geometry> rechtecke = new ArrayList<>(points.size());
        int i = 0;
        for (LocalMinimumPoint point : points) {
            cs[i++] = new Coordinate(point.coordUTM.x, point.coordUTM.y);
        }

        Geometry geo = gf.createMultiPoint(cs).convexHull();

        return geo;
    }

    public final String labelInfoString = "info";

    @Override
    public void selectLocationID(Object o, String layer, long l) {
        frame.mapViewer.clearLayer(labelInfoString);
        if (layer.equals(layerHeightDot)) {
            int x = (int) (l / lengthY);
            int y = (int) (l % lengthY);

//                    LabelPainting lp = new LabelPainting(0, chInfo, new GeoPosition(frame.mapViewer.clickPoint), str.split(";"));
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    try {
                        Coordinate c = new Coordinate(((x + i) * breite / lengthX) + utmXmin, ((y + j) * hoehe / lengthY) + utmYmin);
                        Coordinate c1 = transform(c, utm32wgs);
                        String str = "HeightPoint(" + l + ");(" + (x + i) + "," + (y + j) + ");;maxH = " + df4.format(maximum[x + i][y + j]) + ";minH = " + df4.format(minimum[x + i][y + j]);
                        LabelPainting lp = new LabelPainting((i + 1) * 3 + j, chInfo, new GeoPosition(c1), str.split(";"));
                        frame.mapViewer.addPaintInfoToLayer(labelInfoString, lp);

                    } catch (MismatchedDimensionException ex) {
                        Logger.getLogger(SurfaceTools.class
                                .getName()).log(Level.SEVERE, null, ex);
                    } catch (TransformException ex) {
                        Logger.getLogger(SurfaceTools.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            int id = dreieckIDminimum[x][y];
//                    System.out.println("suche id "+id);
//            for (LocalMinimumTriangle d : dreiecke) {
////                        System.out.println(""+d.id);
//                if (d.id == id) {
////                            System.out.println("gefunden!");
//                    AreaPainting ap = new AreaPainting(0, new DoubleColorHolder(Color.RED, new Color(0, 0, 0, 0), null), d.geom);
//                    frame.mapViewer.addPaintInfoToLayer("infoD", ap);
////                            System.out.println("add shape for minimum triangle");
//                    break;
//                }
//            }

            frame.mapViewer.recalculateShapes();
            frame.mapViewer.repaint();
        } else if (layer.equals(layerDreiecke)) {
//            for (LocalMinimumTriangle d : dreiecke) {
//                if (d.id == l) {
//                    String str = "Dreieck " + l + ";;z=" + d.z;
//                    LabelPainting lp = new LabelPainting(0, chInfo, new GeoPosition(frame.mapViewer.clickPoint), str.split(";"));
//                    frame.mapViewer.addPaintInfoToLayer("info", lp);
//                    break;
//                }
//            }

//        } else if (layer.equals(layerFloodArea)) {
//            for (SurfaceLocalMinimumArea wfa : surfaceAreas) {
//                if (wfa.id == l) {
//                    String str = "Fläche " + l + ";";
//                    if (wfa.flowArea != null) {
//                        str += " Flächen:" + wfa.flowArea.surfaces.size() + ";";
////                        for (SurfaceLocalMinimumArea surface : wfa.flowArea.surfaces) {
////                            str += " Fläche:" + surface.id + ";";
////                        }
//                    } else {
////                        str += " keine Fließfläche;";
//                    }
//                    for (SurfaceOverrun overrun : wfa.overruns) {
//                        str += ";Wasserhöhe=" + df4.format(overrun.ueberlaufWasserstand_uNN) + "m üNN;Pfützentiefe=" + df4.format(overrun.ueberlaufWassertiefe) + " m";
//                    }
//
//                    LabelPainting lp = new LabelPainting(0, chInfo, new GeoPosition(frame.mapViewer.clickPoint), str.split(";"));
//                    frame.mapViewer.addPaintInfoToLayer("info", lp);
//                    break;
//                }
//            }
        } else if (layer.equals(layerFlowArea) || layer.equals(layerSurfaceArea) || layer.equals(layerFloodArea)) {
            for (SurfaceLocalMinimumArea wfa : surfaceAreas) {
                if (wfa.id == l) {
                    String str = "Fläche " + l + ";";
                    if (wfa.flowArea != null) {
                        str += " Flächen:" + wfa.flowArea.surfaces.size() + ";";
//                        for (SurfaceLocalMinimumArea surface : wfa.flowArea.surfaces) {
//                            str += " Fläche:" + surface.id + ";";
//                        }
                    }
                    if (wfa.floodArea != null) {
                        str += " Wasserhöhe: " + df4.format(wfa.floodArea.waterheight) + "m;";
                        str += " Wasserhöhe: " + df4.format(wfa.floodArea.waterheight - wfa.points.getFirst().minZ) + "m;";
                    }
                    for (SurfaceOverrun overrun : wfa.overruns) {
                        str += ";Wasserhöhe=" + df4.format(overrun.ueberlaufWasserstand_uNN) + "m üNN;Pfützentiefe=" + df4.format(overrun.ueberlaufWassertiefe) + " m;";
                        if (overrun.ueberlauf) {
                            str += "ü=> Area " + overrun.surfaceUeberlauf.id + ";;";
                        } else {
                            str += "  > Area " + overrun.surfaceUeberlauf.id + ";;";
                        }
                    }
                    LabelPainting lp = new LabelPainting(0, chInfo, new GeoPosition(frame.mapViewer.clickPoint), str.split(";"));
                    frame.mapViewer.addPaintInfoToLayer(labelInfoString, lp);
                    break;
                }
            }
        } else if (layer.equals(layerFlowPath) || layer.equals(layerFlowPathactive)) {
            FlowPath p = pathIDMap.get((int) l);
            if (p != null) {
                String str = "Flowpath " + l + ";;Aktiv ab " + df4.format(p.activatingWaterheightAtStart) + " üNN;Aktiv ab " + df4.format(p.activatingWaterheightAtStart - p.getStart().minZ) + " m";
                LabelPainting lp = new LabelPainting(0, chInfo, new GeoPosition(frame.mapViewer.clickPoint), str.split(";"));
                frame.mapViewer.addPaintInfoToLayer(labelInfoString, lp);
            }
            System.out.println("No Flowpath found for ID:" + l);
//            System.out.println("   -> "+pathIDMap.get((int)l));
        }
        frame.mapViewer.recalculateShapes();
        frame.mapViewer.repaint();
    }

    public int getXindex(double x) {
        double fX = ((x - utmXmin) + gitterWeiteX * 0.5) / breite;
        return (int) (fX * lengthX);

    }

    public int getYindex(double y) {
        double fY = ((y - utmYmin) + gitterWeiteY * 0.5) / hoehe;
        return (int) (fY * lengthY);
    }

    private void initRegler() {
        frame_Regler.setBounds(1200, 100, 300, 100);
        frame_Regler.setLayout(new BorderLayout());
        frame_Regler.add(slider_Pegel, BorderLayout.NORTH);
        frame_Regler.add(label_Pegel, BorderLayout.CENTER);
        final JButton button_Pegel = new JButton("Show");
        frame_Regler.add(button_Pegel, BorderLayout.SOUTH);
        button_Pegel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ce) {
                button_Pegel.setEnabled(false);
                Thread s = new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (this.isInterrupted()) {
                                return;
                            }
                            double value = slider_Pegel.getValue() / 200.;
                            System.out.println("neue Höhe= " + value + "m.   Slider value=" + slider_Pegel.getValue());
                            label_Pegel.setText("Wasserstand: " + value + "m...");

                            frame.mapViewer.clearLayer(layerFloodArea);
                            frame.mapViewer.clearLayer(layerFlowArea);
                            frame.mapViewer.clearLayer(layerFlowPath);
                            frame.mapViewer.clearLayer(layerFlowPathactive);
                            frame.mapViewer.clearLayer(layerAustritt);
                            frame.mapViewer.clearLayer("FN");
                            resetHeightspecificFlowValues();
                            int i = 0;
                            for (SurfaceLocalMinimumArea sfa : surfaceAreas) {
                                sfa.flowArea = null;
                                sfa.waterHeight = sfa.getLowestPoint().minZ + value;
                            }

                            for (SurfaceLocalMinimumArea sfa : surfaceAreas) {
                                try {
                                    calculateFlowAreas(sfa);
                                    calculateFloodAreas(sfa);
//                                    System.out.println((i++)+": "+flowAreas.size());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            System.out.println(flowAreas.size() + " Flow & " + surfaceAreas.size() + " Flood Areas erstellt.");
                            label_Pegel.setText("Wasserstand: " + value + "m. Flowareas: " + flowAreas.size());
                            if (this.isInterrupted()) {
                                return;
                            }
                            areasFlowDarstellen(true);
                            if (this.isInterrupted()) {
                                return;
                            }
                            areasFloodDarstellen();
                            if (this.isInterrupted()) {
                                return;
                            }
                            niedrigsteFloodFlowPunkteDarstellen();
                            if (this.isInterrupted()) {
                                return;
                            }
//                            overflowPathsFinding();
                            networkOverflowPathsDarstellen();
                            flowArrowsFloodDarstellen();
                            label_Pegel.setText("Wasserstand: " + value + "m. Flowareas: " + flowAreas.size());
                            frame.mapViewer.recalculateShapes();
                            frame.mapViewer.recomputeLegend();
                            frame.mapViewer.repaint();

                            areasFlowDarstellen(false);
                            frame.mapViewer.recalculateShapes();
                            frame.mapViewer.recomputeLegend();
                            frame.mapViewer.repaint();

                        } catch (MismatchedDimensionException ex) {
                            Logger.getLogger(SurfaceTools.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SurfaceTools.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            button_Pegel.setEnabled(true);
                        }
                    }
                };
                if (showThread != null) {
                    showThread.interrupt();

                }
                showThread = s;
                showThread.start();

            }
        });
        slider_Pegel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent ce) {
                button_Pegel.setText("Calculate h=" + (slider_Pegel.getValue() / 200.) + "m");
            }
        });
        button_Pegel.setText("Calculate h=" + (slider_Pegel.getValue() / 200.) + "m");
        frame_Regler.setVisible(true);
    }

    private void resetHeightspecificFlowValues() {
        flowAreas.clear();
        for (SurfaceLocalMinimumArea s : surfaceAreas) {
            s.floodArea = null;
            s.flowArea = null;
            s.waterHeight = s.getLowestPoint().minZ;
            for (LocalMinimumPoint point : s.points) {
                point.lowerPointFlood = null;
            }
            for (SurfaceOverrun overrun : s.overruns) {
                overrun.ueberlauf = false;
            }
            s.massgebendOverrun = null;
        }
    }
    
     public static void saveSurfaces(SurfaceTools sft, File file) throws IOException {

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Charset charset = Charset.forName("UTF-8");
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        FileOutputStream fo = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(fo, charset);
        BufferedWriter bw = new BufferedWriter(osw);

        bw.write("charset=" + charset.displayName() + "\n");
        ReferenceIdentifier rid = Network.crsUTM.getIdentifiers().iterator().next();
        bw.write("utmsrid=" + rid.getCodeSpace() + ":" + rid.getCode());
        bw.newLine();
        bw.write("utmRS=" + Network.crsUTM.getName());
        bw.newLine();
        bw.write("#Area-------------------------");
        bw.newLine();
        bw.write("north=" + sft.utmYmax + "\n");
        bw.write("south=" + sft.utmYmin + "\n");
        bw.write("west=" + sft.utmXmin + "\n");
        bw.write("east=" + sft.utmXmax + "\n");
        bw.write("#Raster-----------------------");
        bw.newLine();
        bw.write("dx=" + sft.gitterWeiteX);
        bw.newLine();
        bw.write("dy=" + sft.gitterWeiteY);
        bw.newLine();
        bw.write("#Surfaces---------------------");
        bw.newLine();
        bw.write("#id;maxWaterheight;targetID;lowestPointCoord;surfaceGeometry;");
        bw.newLine();
        for (SurfaceLocalMinimumArea surfaceArea : sft.surfaceAreas) {
            bw.write(surfaceArea.id + ";");
            bw.write(surfaceArea.points.getFirst().coordUTM + ";");
            bw.write(surfaceArea.geomUTMOuterBoundary.toText());
            bw.newLine();
        }

        bw.close();
        osw.close();
        fo.close();
//        System.out.println("Fertig geschrieben in " + file.getAbsolutePath());
    }

}
