package view;

import control.scenario.injection.InjectionInformation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import model.GeoPosition;
import model.topology.Manhole;

/**
 * Displays information about InjectionInformation
 *
 * @author saemann
 */
public class InjectionPanel extends JPanel {

    protected InjectionInformation info;

    protected JTextField textname;

    protected JPanel panelMaterial;
    protected JLabel labelCoordinate;
    protected JCheckBox checkSurface;
    protected JLabel labelCapacity;
    protected JButton buttonCoordinate;
    public static final DecimalFormat df = new DecimalFormat("0.###");
    protected JSpinner spinnerInjection, spinnerDuration;
    protected SpinnerDateModel modelInjection;
    protected SpinnerNumberModel modelDuration;
    protected JSpinner.DateEditor dateEditorInjection, dateEditorDuration;

    protected JSpinner spinnerParticles;
    protected SpinnerNumberModel modelParticles;

    protected static GregorianCalendar localCalendar = new GregorianCalendar();
    protected MapViewer map;
    protected JButton buttonSetPosition;

    protected InjectionPanel(final InjectionInformation info, final MapViewer map) {
        super(new GridLayout(5, 2));
        this.setBorder(new LineBorder(Color.darkGray, 1, true));
        this.info = info;
        this.map = map;
        //Name
        textname = new JTextField(info.getMaterial().getName());
        this.add(new JLabel("Material:"));
        this.add(textname);
        //Datespinners
//        JPanel panelSouthDate = new JPanel(new GridLayout(2, 1));
        modelInjection = new SpinnerDateModel(new Date(gmtToLocal((long) (info.getStarttimeSimulationsAfterSimulationStart() * 1000L))), null, null, GregorianCalendar.MINUTE);
        spinnerInjection = new JSpinner(modelInjection);
        dateEditorInjection = new JSpinner.DateEditor(spinnerInjection, "HH'h', mm'm', ss's'");
        dateEditorInjection.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        spinnerInjection.setEditor(dateEditorInjection);
        this.add(new JLabel("Inject @"));
        this.add(spinnerInjection);

        modelDuration = new SpinnerNumberModel(0., 0., Double.POSITIVE_INFINITY, 5);
        spinnerDuration = new JSpinner(modelDuration);
        spinnerDuration.setPreferredSize(new Dimension(60, 12));
        this.add(new JLabel("Duration [min]"));
        this.add(spinnerDuration);

        //Number of particles
        modelParticles = new SpinnerNumberModel(info.getNumberOfParticles(), 1, Integer.MAX_VALUE, 1000);
        spinnerParticles = new JSpinner(modelParticles);
        this.add(new JLabel("Particles:"));
        this.add(spinnerParticles);

        //Surface positionin
        checkSurface = new JCheckBox("Surface");
        buttonSetPosition = new JButton("Set Position");
        this.add(checkSurface);
        this.add(buttonSetPosition);
        if (info != null) {
            checkSurface.setSelected(info.spillOnSurface());
            if (info.getPosition() != null) {
                buttonSetPosition.setText(df.format(info.getPosition().getLatitude()) + "; " + df.format(info.getPosition().getLongitude()));
            }
        }
        buttonSetPosition.setToolTipText("Click here to select position on map.");

        this.spinnerInjection.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
//                System.out.println("model injection time: " + localToGMT(modelInjection.getDate().getTime()));
                info.setStart(localToGMT(modelInjection.getDate().getTime()) / 1000.);
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerDuration.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                info.setDuration(modelDuration.getNumber().doubleValue() * 60.);
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        this.spinnerParticles.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                info.setNumberOfParticles(modelParticles.getNumber().intValue());
                if (info.isChanged()) {
                    setBorder(new TitledBorder("changed"));
                }
            }
        });

        textname.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                info.getMaterial().setName(textname.getText());

            }
        });

        checkSurface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                info.setSpillPipesystem(!checkSurface.isSelected());
                if (info.spillOnSurface())//Switch form capacity to surface position.
                {
                    if (info.getCapacity() != null) {
                        if (info.getCapacity() instanceof Manhole) {
                            Manhole mh = (Manhole) info.getCapacity();
                            info.setPosition(mh.getPosition());
                        } else {
                            info.setCapacity(null);
                            info.setPosition(info.getCapacity().getPosition3D(info.getPosition1D()));
                        }
                    } else {
                        info.setTriangleID(-1);
                    }
                } else {
                    //Find corresponding Manhole to position
                    if (info.getPosition() != null) {
                        //No reference to network yet.
                    }
                }
            }
        });
        //Add actionlistener to listen for clicks on the mapviewer.
        final MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent me) {
                if (me.getButton() == 1) {
                    Point2D.Double latlon = map.clickPoint;
                    GeoPosition p = new GeoPosition(latlon.x, latlon.y);
                    info.setPosition(p);
                    info.setCapacity(null);
                    info.setTriangleID(-1);
                    System.out.println("clicked on " + latlon);
                }
                map.removeMouseListener(this);
                buttonSetPosition.setText(df.format(info.getPosition().getLatitude()) + "; " + df.format(info.getPosition().getLongitude()));
            }
        };
        buttonSetPosition.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                map.addMouseListener(ma);
                buttonSetPosition.setText("Click Map Position");
            }
        });
        this.setPreferredSize(new Dimension(160, 95));
        this.setMinimumSize(new Dimension(160, 90));
    }

    public InjectionPanel(MapViewer map) {
        this(null, map);

    }

    public long localToGMT(long local) {
        return local + localCalendar.get(GregorianCalendar.ZONE_OFFSET);
    }

    public long gmtToLocal(long gmt) {
        return gmt - localCalendar.get(GregorianCalendar.ZONE_OFFSET);
    }
}
