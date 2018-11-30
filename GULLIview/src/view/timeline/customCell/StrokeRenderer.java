/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.timeline.customCell;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 *@deprecated 
 * @author saemann
 */
public class StrokeRenderer extends JLabel
        implements TableCellRenderer, ListCellRenderer<BasicStroke> {

    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;
    BasicStroke stroke = new BasicStroke(1);

    public StrokeRenderer() {
//            this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object stroke,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        BasicStroke newstroke = (BasicStroke) stroke;
//        if (newstroke == null) {
//            newstroke = new BasicStroke(1);
//        }
        this.stroke = newstroke;
        setBackground(Color.white);
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                            table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                            table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
        g.setColor(Color.black);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(stroke);
        g2.drawLine(0, this.getHeight() / 2, this.getWidth(), this.getHeight() / 2);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends BasicStroke> jlist, BasicStroke e, int i, boolean bln, boolean bln1) {
        return this;
    }

}
