/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.timeline.customCell;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import org.jfree.data.xy.AbstractXYDataset;
import view.timeline.SeriesKey;

/**
 *
 * @author saemann
 */
public class ColorEditor extends AbstractCellEditor
            implements TableCellEditor,
            ActionListener {
        AbstractXYDataset collection;
        Color currentColor;
        JButton button;
        JColorChooser colorChooser;
        JDialog dialog;
        int row;
        protected static final String EDIT = "edit";

        public ColorEditor(AbstractXYDataset collection) {
            //Set up the editor (from the table's point of view),
            //which is a button.
            //This button brings up the color chooser dialog,
            //which is the editor from the user's point of view.
            button = new JButton();
            button.setActionCommand(EDIT);
          
            button.setBorderPainted(false);
            this.collection=collection;
            //Set up the dialog that the button brings up.
            colorChooser = new JColorChooser();
            colorChooser.setColor(Color.black);
            try {
                dialog = JColorChooser.createDialog(button,
                        "Pick a Color",
                        true, //modal
                        colorChooser,
                        this, //OK button handler
                        null); //no CANCEL button handler  
            } catch (NullPointerException nullPointerException) {
                nullPointerException.printStackTrace();
            }
            button.addActionListener(this);
        }

        /**
         * Handles events from the editor button and from the dialog's OK
         * button.
         */
        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                //The user has clicked the cell, so
                //bring up the dialog.
                button.setBackground(currentColor);
                colorChooser.setColor(currentColor);
                dialog.setVisible(true);
               ((SeriesKey)collection.getSeriesKey(row)).lineColor=currentColor;
                //Make the renderer reappear.
                fireEditingStopped();

            } else { //User pressed dialog's "OK" button.
                currentColor = colorChooser.getColor();
                ((SeriesKey)collection.getSeriesKey(row)).lineColor=currentColor;
//                ((SeriesKey)collection.getSeries(row).getKey()).lineColor=currentColor;
            }
        }

        //Implement the one CellEditor method that AbstractCellEditor doesn't.
        public Object getCellEditorValue() {
            return currentColor;
        }

        //Implement the one method defined by TableCellEditor.
        public Component getTableCellEditorComponent(JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            currentColor = (Color) value;
            this.row=row;
            return button;
        }

    public void setCollection(AbstractXYDataset collection) {
        this.collection = collection;
    }
        
        
    }