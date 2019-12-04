package view.timeline.customCell;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import view.timeline.customCell.ShapeEditor.SHAPES;

/**
 *
 * @author saemann
 */
/**
 * A panel that displays a stroke sample.
 *
 * @author David Gilbert
 */
public class ShapeListRenderer extends JLabel implements ListCellRenderer, TableCellRenderer {

    /**
     * The stroke being displayed (may be null).
     */
    private Shape shape;

    /**
     * The preferred size of the component.
     */
    private Dimension preferredSize;

    private boolean isSelected = false;
    private boolean isBordered;
    private Border selectedBorder;
    private Border unselectedBorder;

    private boolean handleEnum = false;

    /**
     * Creates a StrokeSample for the specified stroke.
     *
     * @param shape the sample stroke (<code>null</code> permitted).
     */
    public ShapeListRenderer(final Shape shape) {
        this.shape = shape;
        this.preferredSize = new Dimension(80, 18);
        setPreferredSize(this.preferredSize);
    }

    /**
     * Returns the current Stroke object being displayed.
     *
     * @return The stroke (possibly <code>null</code>).
     */
    public Shape getShape() {
        return this.shape;
    }

    /**
     * Sets the stroke object being displayed and repaints the component.
     *
     * @param shape the stroke (<code>null</code> permitted).
     */
    public void setShape(final Shape shape) {
        this.shape = shape;
        handleEnum = false;
        repaint();
    }

    /**
     * Sets the stroke object being displayed and repaints the component.
     *
     * @param shape the stroke (<code>null</code> permitted).
     */
    public void setShape(final SHAPES shape) {
        this.shape = shape.getShape();
        handleEnum = true;
        repaint();
    }

    /**
     * Returns the preferred size of the component.
     *
     * @return the preferred size of the component.
     */
    @Override
    public Dimension getPreferredSize() {
        return this.preferredSize;
    }

    /**
     * Draws a line using the sample stroke.
     *
     * @param g the graphics device.
     */
    @Override
    public void paintComponent(final Graphics g) {

        final Graphics2D g2 = (Graphics2D) g;

//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                RenderingHints.VALUE_ANTIALIAS_ON);
        final Dimension size = getSize();
        final Insets insets = getInsets();
        final double xx = insets.left;
        final double yy = insets.top;
        final double ww = size.getWidth() - insets.left - insets.right;
        final double hh = size.getHeight() - insets.top - insets.bottom;
//        System.out.println("width: "+this.getWidth()+" \t insets.width="+(insets.right-insets.left));
//        if (isSelected) {
        g2.setColor(Color.white);
        if (isSelected) {
            g2.setColor(Color.lightGray);
        }
        g2.fill(new Rectangle2D.Double(xx, yy, ww, hh));
//        }

        if (this.shape != null) {
            g2.setColor(Color.black);
            final Point2D mid = new Point2D.Double(xx + shape.getBounds().width * 0.5 + 2, yy + hh / 2);
            g2.translate(mid.getX(), mid.getY());
            g2.draw(shape);
        }

    }

    /**
     * Returns a list cell renderer for the stroke, so the sample can be
     * displayed in a list or combo.
     *
     * @param list the list.
     * @param value the value.
     * @param index the index.
     * @param isSelected selected?
     * @param cellHasFocus focussed?
     *
     * @return the component for rendering.
     */
    public Component getListCellRendererComponent(JList<? extends Shape> list, Shape value,
            int index, boolean isSelected, boolean cellHasFocus) {
        this.isSelected = isSelected;
        setShape(value);
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object shape,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (shape instanceof Shape) {
            this.shape = (Shape) shape;
            handleEnum = false;
        } else if (shape instanceof SHAPES) {
            this.shape = ((SHAPES) shape).getShape();
            handleEnum = true;
        }

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
    public Component getListCellRendererComponent(JList jlist, Object shape, int index, boolean isSelected, boolean cellHasFocus) {
        if (shape instanceof Shape) {
            this.shape = (Shape) shape;
            handleEnum = false;
        } else {
            this.shape = ((SHAPES) shape).getShape();
            handleEnum = true;
        }
        this.isSelected=isSelected;
        repaint();
        return this;
    }

}
