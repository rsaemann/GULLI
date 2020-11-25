package com.saemann.gulli.view.view.timeline.customCell;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author saemann
 */
public class StrokeEditor extends AbstractCellEditor
        implements TableCellEditor, ItemListener {

//    AbstractXYDataset collection;
    public static BasicStroke stroke1 = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public static BasicStroke stroke2 = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static BasicStroke stroke4 = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static BasicStroke dash1 = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0);

    public static BasicStroke dash2 = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 3}, 0);
    public static BasicStroke dotline2 = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2, 4, 8, 4}, 0);
    public static BasicStroke dots3 = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{0, 6}, 0);
public static BasicStroke dots4 = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{0, 8}, 0);

    BasicStroke currentStroke;
    JComboBox<BasicStroke> combobox;
    JDialog dialog;
    int row;

    public static final BasicStroke[] availableStrokes = initStrokes();
    private JTable table;

    public StrokeEditor(JTable model) {

        this.table = model;
//        this.collection = collection;
        //Set up the dialog that the button brings up.
        currentStroke = stroke2;
        combobox = new JComboBox<>(availableStrokes);
        combobox.setRenderer(new StrokeListRenderer(currentStroke));
        combobox.addItemListener(this);

    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        if (ie.getStateChange() == ItemEvent.SELECTED) {
            currentStroke = (BasicStroke) combobox.getSelectedItem();
            fireEditingStopped();
        } else if (combobox.getSelectedItem() == null) {
            currentStroke = null;
            fireEditingStopped();
        }

    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    @Override
    public Object getCellEditorValue() {
        return currentStroke;
    }

    //Implement the one method defined by TableCellEditor.
    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
        currentStroke = (BasicStroke) value;
        boolean found = false;
        for (int i = 0; i < availableStrokes.length; i++) {
            if (availableStrokes[i] == null) {
                if (value == null) {
                    combobox.setSelectedIndex(i);
                    found = true;
                    break;
                }
            } else if (availableStrokes[i].equals(value)) {
                combobox.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            combobox.setSelectedIndex(2);
        }
//        System.out.println(" found index: "+found+"  "+combobox.getSelectedIndex());
        this.row = row;
        return combobox;
    }

    private static BasicStroke[] initStrokes() {
        BasicStroke[] availablet = new BasicStroke[]{
            null,
            stroke1, stroke2, stroke4,
            dash1, dash2,
            dotline2,
            dots3,dots4};
//            new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
//            new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
//            new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
//            new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0),
//            new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 3}, 0),
//            new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2, 4, 8, 4}, 0),
//            new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{0, 7}, 0),};
        return availablet;
    }

}
