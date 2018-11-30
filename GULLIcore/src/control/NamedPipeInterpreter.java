package control;

import io.NamedPipeIO;
import java.awt.BasicStroke;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.topology.Manhole;
import model.topology.Pipe;
import model.topology.Position;
import org.opengis.referencing.operation.TransformException;

/**
 * This Class decodes received Messages from the surface pipe and informs the
 * controller about what happened.
 *
 * @author saemann
 */
public class NamedPipeInterpreter implements PipeActionListener {

    private final Controller control;
//    private final HashMap<Long, ColorHolder> colorMap = new HashMap<>();
    private int nodeCounter = 0;

    private BasicStroke stroke2p = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public NamedPipeInterpreter(Controller control) {
        this.control = control;
    }

    @Override
    public void actionPerformed(NamedPipeIO.PipeActionEvent ae) {
        if (ae.action != NamedPipeIO.ACTION.MESSAGE_RECEIVED) {
            return;
        }

        if (control != null) {
            String line = ae.message;
            if (line.equals("CLEAR")) {
//                if (control.getMapFrame() != null) {
//                    control.getMapFrame().mapViewer.clearLayer("CInlet");
//                    control.getMapFrame().mapViewer.clearLayer("CNode");
//                    control.getMapFrame().mapViewer.clearLayer("CPipe");
//
//                }
            }
            if (line.startsWith("INLET")) {
                double x = 0, y = 0;
                String[] sp = line.split(";");
                for (int i = 1; i < sp.length; i++) {
                    if (sp[i].startsWith("x:")) {
                        x = Double.parseDouble(sp[i].substring(2));
                    } else if (sp[i].startsWith("y:")) {
                        y = Double.parseDouble(sp[i].substring(2));
                    }
                }
                try {
                    Position pos = control.getPositionFromGK(x, y);
                    Manhole mh = control.getNetwork().getManholeNearPositionLatLon(pos.getLatitude(), pos.getLongitude());
                    if (mh == null) {
                        System.out.println("Manhole near Inlet @" + pos + " could not be found.");
                        return;
                    } else {
//                        if (control.getMapFrame() != null) {
////                                control.paintManager.addContaminatedInlet(pos);
//                            MapViewer viewer = control.getMapFrame().getMapViewer();
//                            ColorHolder color = null;
//                            if (colorMap.containsKey(mh.getAutoID())) {
//                                color = colorMap.get(mh.getAutoID());
//                            }
//                            if (color == null) {
//                                final Color co = new Color((int) (Math.random() * 16777216));
//                                color = new ColorHolder(co, mh.getAutoID() + "");
//                                color.setStroke(stroke2p);
//                                colorMap.put(mh.getAutoID(), color);
//
//                                NodePainting lp = new NodePainting(mh.getAutoID(), mh.getPosition(), color) {
//
//                                    @Override
//                                    public boolean paint(Graphics2D g2) {
//                                        try {
//                                            g2.setColor(co);
//                                            super.paint(g2);
//                                            return true;
//                                        } catch (Exception e) {
//                                            if (MapViewer.verboseExceptions) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                        return false;
//                                    }
//
//                                };
//
//                                viewer.addPaintInfoToLayer("CNode", lp);
//
//                            }
//                            if (color != null) {
//                                NodePainting np = new NodePainting(nodeCounter++, pos, color);
//                                viewer.addPaintInfoToLayer("CInlet", np);
//                            }
//                            viewer.repaint();
//                        }
                    }

                    if (false) {
                        Pipe pipe = control.getNetwork().getPipeNearPositionLAtLon(pos.getLatitude(), pos.getLongitude());
                        if (pipe == null) {
                            System.out.println("Pipe near Inlet @" + pos + " could not be found.");
                            return;
                        } else {
//                            if (control.getMapFrame() != null) {
////                                control.paintManager.addContaminatedInlet(pos);
//                                MapViewer viewer = control.getMapFrame().getMapViewer();
//                                ColorHolder color = null;
//                                if (colorMap.containsKey(pipe.getAutoID())) {
//                                    color = colorMap.get(pipe.getAutoID());
//                                }
//                                if (color == null) {
//                                    final Color co = new Color((int) (Math.random() * 16777216));
//                                    color = new ColorHolder(co, pipe.getAutoID() + "");
//                                    color.setStroke(stroke2p);
//                                    colorMap.put(pipe.getAutoID(), color);
//
//                                    ArrayList<GeoPosition2D> c = new ArrayList<>(2);
//                                    c.add(pipe.getStartConnection().getPosition());
//                                    c.add(pipe.getEndConnection().getPosition());
//
//                                    LinePainting lp = new LinePainting(pipe.getAutoID(), c, color) {
//
//                                        @Override
//                                        public boolean paint(Graphics2D g2) {
//                                            g2.setColor(co);
//                                            super.paint(g2);
//                                            return true;
//                                        }
//
//                                    };
//
//                                    viewer.addPaintInfoToLayer("CPipe", lp);
//
//                                }
//                                if (color != null) {
//                                    NodePainting np = new NodePainting(nodeCounter++, pos, color);
//                                    viewer.addPaintInfoToLayer("CInlet", np);
//                                }
//                                viewer.repaint();
//                            }
                        }
                    }
                } catch (TransformException ex) {
                    Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
