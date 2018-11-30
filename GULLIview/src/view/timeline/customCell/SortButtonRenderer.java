package view.timeline.customCell;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author saemann
 */
public class SortButtonRenderer extends AbstractCellEditor implements ActionListener, TableCellRenderer, TableCellEditor {

    protected JPanel panel;
    protected JButton buttonUP, buttonDown;

    public enum DIRECTION {

        UP, DOWN, NO
    };
    protected DIRECTION direction = DIRECTION.NO;

    public SortButtonRenderer() {
        panel = new JPanel(new GridLayout(1, 2));
        buttonUP = new JButton() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                super.paintComponent(grphcs);
                grphcs.drawString("\u2191", this.getWidth() / 2 - 3, (this.getHeight() + this.getFont().getSize()) / 2);
            }
        };
        buttonDown = new JButton() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                super.paintComponent(grphcs);
                grphcs.setColor(Color.darkGray);
                grphcs.drawString("\u2193", this.getWidth() / 2 - 3, (this.getHeight() + this.getFont().getSize()) / 2);//
            }
        };
        buttonUP.addActionListener(this);
        buttonDown.addActionListener(this);
        panel.add(buttonUP);
        panel.add(buttonDown);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        this.buttonUP.setSelected(false);
        this.buttonDown.setSelected(false);
        return panel;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
//        if (value != null) {
//            if (value instanceof SortButtonRenderer) {
//                SortButtonRenderer in = (SortButtonRenderer) value;
////                if(in.direction==DIRECTION.UP){
////                    this.buttonUP.setSelected(true);
////                    this.buttonUP.requestFocus();
////                    this.buttonDown.setSelected(false);                    
////                }else if(in.direction==DIRECTION.DOWN){
////                    this.buttonDown.setSelected(true);
////                    this.buttonDown.requestFocus();
////                    this.buttonUP.setSelected(false);      
////                }else{
//                      
////                }
//            } else {
//                System.out.println(getClass() + "::input is of Class " + value.getClass());
//            }
//        } 
        this.buttonUP.setSelected(false);
        this.buttonDown.setSelected(false);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return this;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        direction = DIRECTION.NO;
        if (ae.getSource().equals(buttonUP)) {
            direction = DIRECTION.UP;
        } else if (ae.getSource().equals(buttonDown)) {
            direction = DIRECTION.DOWN;
        }
//        System.out.println("action event: " + ae);
        fireEditingStopped();
    }

    public DIRECTION getDirection() {
        return direction;
    }

}
