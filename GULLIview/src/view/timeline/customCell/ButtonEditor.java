package view.timeline.customCell;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;

/**
 *
 * @author saemann
 */
public class ButtonEditor extends AbstractCellEditor implements ActionListener{

    protected JButton button;

    public ButtonEditor(String label) {
        super();
        this.button=new JButton(label);
        this.button.addActionListener(this);
    }

    @Override
    public Object getCellEditorValue() {
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        fireEditingStopped();
    }
    
}
