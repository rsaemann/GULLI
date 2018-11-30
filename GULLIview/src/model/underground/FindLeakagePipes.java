/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.underground;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import control.LocationIDListener;
import control.StartParameters;
import io.extran.HE_Database;
import io.ogs.Domain3DIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.JFrame;
import model.GeoPosition;
import model.GeoTools;
import model.particle.Particle;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position3D;
import model.underground.Domain3D;
import view.ColorHolder;
import view.DoubleColorHolder;
import view.MapViewer;
import view.SimpleMapViewerFrame;
import view.shapes.AreaPainting;
import view.shapes.LabelPainting;
import view.shapes.LinePainting;
import view.shapes.NodePainting;

/**
 *
 * @author saemann
 */
public class FindLeakagePipes {

    public static void main(String[] args) throws Exception {

        File fileGroundwater = new File("Y:\\\\EVUS\\Knotengeschwindigkeiten_GWModell\\Knotengeschwindigkeiten_QuasiSteadyState.vtu");//3D_RICHARDS_FLOW36_august2017.vtu");
        File fileNetwork = new File("C:\\Users\\saemann\\Documents\\NetBeansProjects\\GULLI\\input\\Modell2017Mai\\2D_Model\\Model.idbf");

        Network network = HE_Database.loadNetwork(fileNetwork);
        System.out.println("network loaded");
        Domain3D soil = Domain3DIO.read3DFlowFieldVTU(fileGroundwater, "EPSG:3857", "EPSG:25832");
        final GeoTools gt = new GeoTools("EPSG:4326", "EPSG:25832", StartParameters.JTS_WGS84_LONGITUDE_FIRST);

        System.out.println("soil loaded");
        final SimpleMapViewerFrame frame = new SimpleMapViewerFrame();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(200, 100, 500, 400);
        ColorHolder chIn = new ColorHolder(Color.green, "inflow");
        final ColorHolder chOut = new ColorHolder(Color.red, "outflow");
        ColorHolder chTrace = new ColorHolder(Color.orange, "Trace");
        ColorHolder chp = new ColorHolder(Color.blue, "Years");

        final ColorHolder chSelected = new ColorHolder(Color.red, "Outflow source");
        chSelected.setStroke(new BasicStroke(3));

        final DoubleColorHolder chcloud = new DoubleColorHolder(Color.orange, new Color(255, 200, 100, 40), "End Contamination");
        final String layerEndContamination = "END";

        DecimalFormat df = new DecimalFormat("0.###");

        String layerIn = "In", layerOut = "Out", layerTrace = "Trace";
        final LinkedList<Coordinate> outflows = new LinkedList<>();
        final HashMap<Long, String> pipeInfo = new HashMap<>(network.getPipes().size());
        final Point p = new Point();
        MouseMotionAdapter mml = new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent me) {
                p.x = me.getX();
                p.y = me.getY();
               
            }

        };

        frame.getMapViewer()
                .addMouseMotionListener(mml);

        for (Pipe pipe
                : network.getPipes()) {
            StringBuilder infoString = new StringBuilder(pipe.getName() + ";");
            infoString.append(pipe.toString() + ";");
            Position3D pos = pipe.getPosition3D(pipe.getLength() * 0.5);
//            System.out.println("Pipe: "+pipe.getName()+"\t"+pipe.getPosition3D(0));
            Coordinate utm = gt.toUTM(new Coordinate(pos.getLongitude(), pos.getLatitude()));
            infoString.append("hig: ").append(df.format(pipe.getStartConnection().getPosition().z)).append(";");
            infoString.append("mid: ").append(df.format(pos.z)).append(";");
            infoString.append("low: ").append(df.format(pipe.getEndConnection().getPosition().z)).append(";");

            utm.z = pos.z;
            int index = soil.getNearestCoordinateIndex(utm);
            if (index < 0) {
                continue;
            }
//            //List points at same location but in different heights
            //All points along z axis show the same GW height.
//            LinkedList<Integer> indices = new LinkedList<>();
//            Coordinate ref = soil.position[index];
//            for (int i = 0; i < soil.position.length; i++) {
//                Coordinate c = soil.position[i];
//                if (Math.abs(ref.x - c.x) < 0.1 && Math.abs(ref.y - c.y) < 0.1) {
//                    indices.add(i);
//                }
//            }
//            System.out.println(pipe.toString()+" found " + indices.size());
//            for (Integer ix : indices) {
//                System.out.println(ix + ": z=" + df.format(soil.position[ix].z) + "\tdGW=" + df.format(soil.groundwaterDistance[ix]) + "\t GWz=" + df.format(soil.position[ix].z + soil.groundwaterDistance[ix]));
//            }

            if (index < 0 || index >= soil.groundwaterDistance.length) {
                System.out.println("Could not find Position near " + utm);
                continue;
            }
            //Tiefe des Wasserstandes an diesem Knoten:
            float gwheight = (float) (soil.position[index].z + soil.groundwaterDistance[index]);
            Coordinate coordinate = soil.position[index];
//            System.out.println("convert " + pos +" \tnearest: "+coordinate);
            Coordinate global = gt.toGlobal(coordinate);

            infoString.append(";");
            infoString.append("Pipep:  " + df.format(utm.x) + " / " + df.format(utm.y)).append(";");
            infoString.append("Gridp:  " + df.format(coordinate.x) + " / " + df.format(coordinate.y)).append(";");
            infoString.append("GridZ:  " + df.format(coordinate.z) + ";");
            infoString.append("GridIdx:" + index + ";");

            infoString.append("GW ds:  " + soil.groundwaterDistance[index] + ";");
            infoString.append("GW hi:  " + gwheight).append(";");
            infoString.append("leckage:" + (gwheight < pos.z));
            pipeInfo.put(pipe.getAutoID(), infoString.toString());

            if (gwheight > pos.z) {
                NodePainting np = new NodePainting(pipe.getAutoID(), global, chIn);
                np.setRadius(4);

                frame.getMapViewer().addPaintInfoToLayer(layerIn, np);
            } else {
                NodePainting np = new NodePainting(pipe.getAutoID(), global, chOut);
                np.setRadius(3);
                np.setShapeRound(true);
                frame.getMapViewer().addPaintInfoToLayer(layerOut, np);
                outflows.add(coordinate);
            }
        }
        GeometryFactory gf = new GeometryFactory();

        frame.getMapViewer()
                .addListener(new LocationIDListener() {
                    @Override
                    public void selectLocationID(Object o, String string, long l) {

                        Point2D.Double ll = frame.mapViewer.getPosition(p);
                        try {
                            if (string.equals(layerEndContamination)) {
                                Coordinate out = outflows.get((int) l);
                                out = gt.toGlobal(out);
                                frame.getMapViewer().addPaintInfoToLayer(frame.getMapViewer().LAYER_KEY_LABEL, new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, new GeoPosition(ll.x, ll.y), "Outflow " + l));
                                NodePainting np = new NodePainting(-1, out, chSelected) {

                                    @Override
                                    public boolean paint(Graphics2D g2) {
                                        g2.setStroke(chSelected.getStroke());
                                        return super.paint(g2); //To change body of generated methods, choose Tools | Templates.
                                    }

                                };
                                np.setRadius(6);
                                np.setShapeRound(true);

                                frame.getMapViewer().addPaintInfoToLayer(string, np);
                            } else {

                                String info = pipeInfo.get(l);
                                frame.getMapViewer().addPaintInfoToLayer(frame.getMapViewer().LAYER_KEY_LABEL, new LabelPainting(0, MapViewer.COLORHOLDER_LABEL, new GeoPosition(ll.x, ll.y), info.split(";")));
                            }
                        } catch (Exception e) {
                        }

                        frame.getMapViewer().recalculateShapes();
                        frame.getMapViewer().repaint();
                    }
                });

        frame.getMapViewer()
                .recomputeLegend();
        frame.getMapViewer()
                .zoomToFitLayer();
        double dt = 60 * 60 * 24 * 14;
        double intervallNodes = 60 * 60 * 24 * 365 * 10;
        double maxT = 60 * 60 * 24 * 365 * 10;
        int objectid = 0;
        int historyId = 0;
        double diffusion = 0.00002;
        double dispersivity = 10;
        double jumplength = Math.sqrt(2 * diffusion * dt);

        System.out.println(
                "Sprungweite: " + jumplength + "m /dt");
        int numberOfParticles = 50;
        Random random = new Random();
        double vmax = soil.getHighestVelocity();
        System.out.println("vmax=" + vmax);
//        System.exit(0);

//        outflows.clear();
        int outflowNumber = -1;
        for (Coordinate outflow : outflows) {
            outflowNumber++;
            long starttime = System.currentTimeMillis();
            long start = System.currentTimeMillis();
            //Find index to this coordinate

            Coordinate[] particles = new Coordinate[numberOfParticles];
            ArrayList<Coordinate> validparticles = new ArrayList<>(numberOfParticles);
            LinkedList<Coordinate>[] historys = new LinkedList[numberOfParticles];
            for (int i = 0; i < particles.length; i++) {
                particles[i] = new Coordinate(outflow);
                historys[i] = new LinkedList<>();
            }
            for (int i = 0; i < numberOfParticles; i++) {
                double time = 0;
                Coordinate c = particles[i];
                LinkedList<Coordinate> history = historys[i];
                history.add(gt.toGlobal(c));
                int historyIndex = 0;
                int loopindex = 0;
                double distance = 0;
                float[] velocity = null;
                while (time < maxT) {
                    int index = soil.getNearestCoordinateIndex(c);
                    if (index < 0) {
                        System.out.println("2no near coordinate found for " + c);
                        break;
                    }
                    velocity = soil.velocity[0][index]; //timeindex 0: is stationary flowfield
//                    System.out.println("v[" + index + "] = " + velocity[0] + "\t," + velocity[1]);
                    Object tempC = c.clone();

                    c.x += velocity[0] * dt;
                    c.y += velocity[1] * dt;
                    c.z += velocity[2] * dt;

                    c.x += random.nextGaussian() * Math.sqrt(velocity[0] * dispersivity * dt * 2);
                    c.y += random.nextGaussian() * Math.sqrt(velocity[1] * dispersivity * dt * 2);
                    // c.z+=random.nextGaussian()*jumplength;

                    double ds = Math.sqrt((velocity[0] * dt) * ((velocity[0] * dt)) + (velocity[1] * dt) * ((velocity[1] * dt)) + (velocity[2] * dt) * ((velocity[2] * dt)));
                    if (ds * 100 < 1) {
                        break;
                    }
                    distance += ds;
                    if (c.z < soil.minZ) {
                        c.z = soil.minZ;
                        System.out.println("  below min ");
                    }
                    if (c.z > soil.maxZ) {
                        c.z = soil.maxZ;
                        System.out.println("  above max");
                    }
                    index = soil.getNearestCoordinateIndex(c);
                    if (index < 0) {
                        System.out.println("1no near coordinate found for " + tempC);
                        c = (Coordinate) tempC;
                        break;
                    }

                    time += dt;
                    loopindex++;

//                    if (loopindex % 600 == 0) {
//                        System.out.println(objectid + ") " + loopindex + " \tt:" + df.format(time / (3600 * 24 * 365)) + "a\t dist: " + (int) distance + "m. \tv:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t Pos:" + index + " :" + c);
////                    viewer.recalculateShapes();
////                    viewer.repaint();
//                    }
                    if (((int) (time / intervallNodes)) > historyIndex) {
                        try {
                            frame.getMapViewer().addPaintInfoToLayer("Particle", new NodePainting(historyId++, gt.toGlobal(c), chp));

                            historyIndex = (int) (time / intervallNodes);
                            history.add(gt.toGlobal(c));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }

                }
//                System.out.println(objectid + ") " + loopindex + " \tt:" + df.format(time / (3600 * 24 * 365)) + "a\t dist: " + (int) distance + "m. \tv:" + velocity[0] + " " + velocity[1] + " " + velocity[2] + "\t Pos:" + c);

                LinePainting lp = new LinePainting(objectid++ * numberOfParticles + i, history.toArray(new Coordinate[history.size()]), chTrace);
                lp.arrowheadvisibleFromZoom = 17;
                frame.getMapViewer().addPaintInfoToLayer(layerTrace, lp);
                frame.getMapViewer().recalculateShapes();
                frame.getMapViewer().repaint();

                if (Double.isNaN(c.x) || Double.isNaN(c.y)) {

                } else {
                    validparticles.add(c);
                }
            }
            //Create shape containing the whole clod
            MultiPoint mp = gf.createMultiPoint(validparticles.toArray(new Coordinate[validparticles.size()]));
            Geometry ch = mp.convexHull();

            Geometry g2 = mp.union(gf.createPoint(outflow));
            Geometry ch2 = g2.convexHull();

            try {
                AreaPainting ap = new AreaPainting(outflowNumber, (DoubleColorHolder) chcloud, gt.toGlobal(ch));
                frame.getMapViewer().addPaintInfoToLayer(layerEndContamination, ap);
            } catch (Exception exception) {
            }
            System.out.println("Calculating " + outflow + " took " + ((System.currentTimeMillis() - starttime) / 1000) + "s.");
        }

    }
}
