package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 *
 * @author saemann
 */
public class ProfilePanel extends JPanel {

    private final double[][] velocities;
    private double vMax;
    private int[] selectedIndex;
    private JLabel labelInfo;
    private String unit = "m/s";

    public ProfilePanel(double[][] velocities, double vMax) {
        super(new BorderLayout());
        this.velocities = velocities;
        this.vMax = vMax;
        labelInfo = new JLabel();
        this.add(labelInfo, BorderLayout.NORTH);
        this.initMouseListener();
    }

    public void setvMax(double vMax) {
        this.vMax = vMax;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        Graphics2D g2 = (Graphics2D) grphcs;
        g2.setColor(Color.white);
        float pw = (this.getWidth()) / (float) velocities.length;
        float ph = (this.getHeight()) / (float) velocities[0].length;
        float middle = (this.getWidth()) * 0.5f;
        for (int i = 0; i < velocities.length; i++) {
            for (int j = 0; j < velocities[0].length; j++) {
                float part = (float) (velocities[i][j] / vMax);
                Color c;
                if (Float.isNaN(part)) {
                    c = Color.yellow;
                } else {
                    part = Math.max(0f, part);
                    part = Math.min(1f, part);
//                            System.out.println("Part [" + i + "," + j + "] is " + part);
                    c = new Color(part, part, part);
                }
                g2.setColor(c);
                if (selectedIndex != null) {
                    if (selectedIndex[0] == i && selectedIndex[1] == j) {
                        g2.setColor(Color.RED);
                    }
                }
                Rectangle2D.Float rect = new Rectangle2D.Float(middle + i * pw, this.getHeight() - j * ph, pw + 0.2f, ph + 0.2f);
                g2.fill(rect);
                rect = new Rectangle2D.Float(middle - i * pw - 0.2f, this.getHeight() - j * ph, pw + 0.2f, ph + 0.2f);
                g2.fill(rect);
            }
        }
    }

    private void initMouseListener() {
        final JPopupMenu popup = new JPopupMenu("Title");
        final JMenuItem item = new JMenuItem("test");
        popup.add(item);
        final DecimalFormat df = new DecimalFormat("0.##");
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent me) {
//                System.out.println("button: "+me.getButton());
                super.mouseReleased(me); //To change body of generated methods, choose Tools | Templates.
                if (me.getButton() == MouseEvent.BUTTON1) {
//                    System.out.println("clicked");
//                    popup.setLocation(me.getLocationOnScreen());
                    float pw = (getWidth()) / (float) velocities.length;
                    int posy = (int) (((me.getX() - (int)(0.5 * getWidth()-pw)-pw) / (double) getWidth()) * velocities.length);
                    if (posy < 0) {
                        posy = -posy;
                    }
                    float ph = (getHeight()) / (float) velocities[0].length;
                    
                    int posz = (int) (((1 - ((me.getY()-ph) / (double) getHeight())) * velocities[0].length));
                    if (posy < 0 || posy >= velocities.length) {
                        return;
                    }
                    if (posz < 0 || posz >= velocities[0].length) {
                        return;
                    }

                    selectedIndex = new int[]{posy, posz};
                    String text;
                    if (Double.isNaN(velocities[posy][posz])) {
                        text = "[" + posy + "," + posz + "]: --";
                    } else {
                        text = "[" + posy + "," + posz + "]: " + df.format(velocities[posy][posz]) + " " + unit;
                    }
                    labelInfo.setText(text);
                    repaint();
//                    System.out.println(text);
//                    item.setText(text);
//
//                    popup.setVisible(true);

                } else {

//                    popup.setVisible(false);
                }
            }
        });
    }
}
