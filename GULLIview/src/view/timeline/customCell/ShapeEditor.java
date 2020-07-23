package view.timeline.customCell;

import java.awt.Component;
import java.awt.Shape;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author saemann
 */
public class ShapeEditor extends AbstractCellEditor
        implements TableCellEditor,
        ItemListener {

//    TimeSeriesCollection collection;
    Shape currentShape;
    SHAPES currentSHAPES;
//    JButton button;
    JComboBox<SHAPES> combobox;
//    StrokeChooserPanel strokeChooser;
//    JDialog dialog;
    int row;
//    protected static final String EDIT = "edit";
    private static float radius_s = 3;
    private static float radius_m = 5;
    private static float radius_l = 8;
    public static final Shape[] availableShapes = initShapes2();
    public static final SHAPES[] available_SHAPES = initShapes();
    private JTable table;

    public ShapeEditor(JTable model) {

        this.table = model;
//        this.collection = collection;
        //Set up the dialog that the button brings up.

        combobox = new JComboBox<>(available_SHAPES);
        if (currentShape != null) {
            combobox.setRenderer(new ShapeListRenderer(currentShape));
        } else if (availableShapes.length > 0) {
            combobox.setRenderer(new ShapeListRenderer(availableShapes[0]));
        } else {
            combobox.setRenderer(new ShapeListRenderer(null));
        }
        combobox.addItemListener(this);

    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            if (combobox.getSelectedItem() instanceof Shape) {
                currentShape = (Shape) combobox.getSelectedItem();
            } else if (combobox.getSelectedItem() instanceof SHAPES) {
                currentSHAPES = (SHAPES) combobox.getSelectedItem();
            }
            fireEditingStopped();
        } else {
            if (combobox.getSelectedItem() == null || combobox.getSelectedItem() == SHAPES.EMPTY) {
                currentShape = null;
                currentSHAPES = SHAPES.EMPTY;
                fireEditingStopped();
            }
        }
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    @Override
    public Object getCellEditorValue() {
        if (currentSHAPES != null) {
            return currentSHAPES;
        }
        return currentShape;
    }

    //Implement the one method defined by TableCellEditor.
    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
        System.out.println("selected shape: "+value);
        boolean foundIndex=false;
        if (value instanceof Shape) {
            currentShape = (Shape) value;
            for (int i = 0; i < availableShapes.length; i++) {
                if (availableShapes[i] == null) {
                    if (value == null) {
                        combobox.setSelectedIndex(i);
                        foundIndex=true;
                        break;
                    }
                } else {
                    if (availableShapes[i].equals(value)) {
                        combobox.setSelectedIndex(i);
                        foundIndex=true;
                        break;
                    }
                }
            }
        } else if (value instanceof SHAPES) {
            currentSHAPES = (SHAPES) value;
            for (int i = 0; i < available_SHAPES.length; i++) {
                if (available_SHAPES[i] == null) {
                    if (value == null) {
                        combobox.setSelectedIndex(i);
                        foundIndex=true;
                        break;
                    }
                } else {
                    if (available_SHAPES[i].equals(value)) {
                        combobox.setSelectedIndex(i);
                        foundIndex=true;
                        break;
                    }
                }
            }
        }
        if(!foundIndex)combobox.setSelectedIndex(0);
        this.row = row;
        return combobox;
    }

    private static Shape[] initShapes2() {

        //Kreuz
        GeneralPath kreuz = new GeneralPath();
        kreuz.moveTo(0, -radius_m);
        kreuz.lineTo(0, radius_m);
        kreuz.moveTo(-radius_m, 0);
        kreuz.lineTo(radius_m, 0);

        //X        
        GeneralPath diagonalkreuz = new GeneralPath();
        diagonalkreuz.moveTo(-radius_m, -radius_m);
        diagonalkreuz.lineTo(radius_m, radius_m);
        diagonalkreuz.moveTo(-radius_m, radius_m);
        diagonalkreuz.lineTo(radius_m, -radius_m);

        //Stern Star
        GeneralPath stern = (GeneralPath) kreuz.clone();
        stern.append(diagonalkreuz, false);

        //Diamant Diamond
        GeneralPath diamant = new GeneralPath();
        diamant.moveTo(0, -4. / 3. * radius_m);
        diamant.lineTo(-radius_m, 0);
        diamant.lineTo(0, 4. / 3. * radius_m);
        diamant.lineTo(radius_m, 0);
        diamant.closePath();

        //Uparrow
        GeneralPath arrowup = new GeneralPath();
        arrowup.moveTo(0, -radius_m);
        arrowup.lineTo(-0.866 * radius_m, 0.5 * radius_m);
        arrowup.lineTo(0.866 * radius_m, 0.5 * radius_m);
        arrowup.closePath();

        //leftarrow
        AffineTransform atf = new AffineTransform();
        atf.setToRotation(Math.PI * 0.5);
        GeneralPath rightarrow = (GeneralPath) arrowup.createTransformedShape(atf);
        atf.setToRotation(Math.PI);
        GeneralPath downarrow = (GeneralPath) arrowup.createTransformedShape(atf);
        atf.setToRotation(-0.5 * Math.PI);
        GeneralPath leftarrow = (GeneralPath) arrowup.createTransformedShape(atf);

        Shape[] availables = new Shape[]{
            null,
            new Ellipse2D.Double(-radius_m, -radius_m, radius_m * 2, radius_m * 2),
            new Ellipse2D.Double(-5.5f, -5.5f, 11, 11),
            new Rectangle2D.Double(-0.5f, -0.5f, 1, 1),
            new Rectangle2D.Double(-radius_m, -radius_m, radius_m * 2, radius_m * 2),
            new Rectangle2D.Double(-5.5f, -5.5f, 11, 11),
            kreuz,
            diagonalkreuz,
            stern,
            diamant,
            arrowup,
            rightarrow,
            downarrow,
            leftarrow
        };

        return availables;

    }

    private static SHAPES[] initShapes() {
//        SHAPES[] enums = SHAPES.values();
//        Shape[] shapes = new Shape[enums.length];
//        for (int i = 0; i < enums.length; i++) {
//            shapes[i] = enums[i].getShape();
//        }
//        return shapes;
        return SHAPES.values();
    }

    public enum SHAPES {

        EMPTY {
                    @Override
                    public Shape getShape() {
                        return null;
                    }
                },
        DOT {
                    @Override
                    public Shape getShape() {
                        return new Ellipse2D.Double(-2, -2, 4, 4);
                    }
                },
        ELLIPSE_S {
                    @Override
                    public Shape getShape() {
                        return new Ellipse2D.Double(-radius_s, -radius_s, radius_s * 2, radius_s * 2);
                    }
                },
        ELLIPSE_M {
                    @Override
                    public Shape getShape() {
                        return new Ellipse2D.Double(-radius_m, -radius_m, radius_m * 2, radius_m * 2);
                    }
                },
        ELLIPSE_L {
                    @Override
                    public Shape getShape() {
                        return new Ellipse2D.Double(-radius_l, -radius_l, radius_l * 2, radius_l * 2);
                    }
                },
        RECTANGLE_S {
                    @Override
                    public Shape getShape() {
                        return new Rectangle2D.Double(-radius_s, -radius_s, radius_s * 2, radius_s * 2);
                    }
                },
        RECTANGLE_M {
                    @Override
                    public Shape getShape() {
                        return new Rectangle2D.Double(-radius_m, -radius_m, radius_m * 2, radius_m * 2);
                    }
                },
        RECTANGLE_L {
                    @Override
                    public Shape getShape() {
                        return new Rectangle2D.Double(-radius_l, -radius_l, radius_l * 2, radius_l * 2);
                    }
                },
        PLUS {
                    @Override
                    public Shape getShape() {
                        GeneralPath kreuz = new GeneralPath();
                        kreuz.moveTo(0, -radius_m);
                        kreuz.lineTo(0, radius_m);
                        kreuz.moveTo(-radius_m, 0);
                        kreuz.lineTo(radius_m, 0);
                        return kreuz;
                    }
                },
        CROSS {
                    @Override
                    public Shape getShape() {
                        GeneralPath diagonalkreuz = new GeneralPath();
                        diagonalkreuz.moveTo(-radius_m, -radius_m);
                        diagonalkreuz.lineTo(radius_m, radius_m);
                        diagonalkreuz.moveTo(-radius_m, radius_m);
                        diagonalkreuz.lineTo(radius_m, -radius_m);
                        return diagonalkreuz;
                    }
                },
        STAR {
                    @Override
                    public Shape getShape() {
                        GeneralPath gp = (GeneralPath) ((GeneralPath) PLUS.getShape()).clone();
                        gp.append(CROSS.getShape(), false);
                        return gp;
                    }
                },
        DIAMOND {
                    @Override
                    public Shape getShape() {
                        GeneralPath diamant = new GeneralPath();
                        diamant.moveTo(0, -4. / 3. * radius_m);
                        diamant.lineTo(-radius_m, 0);
                        diamant.lineTo(0, 4. / 3. * radius_m);
                        diamant.lineTo(radius_m, 0);
                        diamant.closePath();
                        return diamant;
                    }
                },
        ARROW_UP {
                    @Override
                    public Shape getShape() {
                        GeneralPath arrowup = new GeneralPath();
                        arrowup.moveTo(0, -radius_m);
                        arrowup.lineTo(-0.866 * radius_m, 0.5 * radius_m);
                        arrowup.lineTo(0.866 * radius_m, 0.5 * radius_m);
                        arrowup.closePath();
                        return arrowup;
                    }
                },
        ARROW_DOWN {
                    @Override
                    public Shape getShape() {
                        AffineTransform aft = new AffineTransform();
                        aft.setToRotation(Math.PI);
                        return ((GeneralPath) ARROW_UP.getShape()).createTransformedShape(aft);
                    }
                },
        ARROW_RIGHT {
                    @Override
                    public Shape getShape() {
                        AffineTransform aft = new AffineTransform();
                        aft.setToRotation(0.5 * Math.PI);
                        return ((GeneralPath) ARROW_UP.getShape()).createTransformedShape(aft);
                    }
                },
        ARROW_LEFT {
                    @Override
                    public Shape getShape() {
                        AffineTransform aft = new AffineTransform();
                        aft.setToRotation(-0.5 * Math.PI);
                        return ((GeneralPath) ARROW_UP.getShape()).createTransformedShape(aft);
                    }
                };

        public abstract Shape getShape();

    }
}
