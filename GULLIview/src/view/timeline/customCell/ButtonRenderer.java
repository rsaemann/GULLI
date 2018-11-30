package view.timeline.customCell;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author saemann
 */
public class ButtonRenderer extends JButton implements TableCellRenderer{

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.setSelected(isSelected);
        this.setFocusPainted(hasFocus);
        return this;
    }
    
}
