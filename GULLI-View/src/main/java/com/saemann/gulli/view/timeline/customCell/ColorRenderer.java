/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.view.timeline.customCell;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author saemann
 */
public class ColorRenderer extends JLabel implements TableCellRenderer {

    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    public ColorRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    /**
     *
     * @param table
     * @param color
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return
     */
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object color,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        Color newColor = (Color) color;
        if (newColor == null) {
            newColor = Color.BLACK;
        }
        setBackground(newColor);
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

        setToolTipText("RGB value: " + newColor.getRed() + ", "
                + newColor.getGreen() + ", "
                + newColor.getBlue());
        return this;
    }
}
