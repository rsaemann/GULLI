package view.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import view.timeline.customCell.ColorEditor;
import view.timeline.customCell.ColorRenderer;
import view.timeline.customCell.ShapeEditor;
import view.timeline.customCell.ShapeEditor.SHAPES;
import view.timeline.customCell.ShapeListRenderer;
import view.timeline.customCell.SortButtonRenderer;
import view.timeline.customCell.StrokeEditor;
import view.timeline.customCell.StrokeListRenderer;

/**
 *
 * @author saemann
 */
public class XYSeriesTable extends JTable implements KeyListener {

    protected final DefaultTableModel model;
    protected final XYSeriesCollection collection;

    protected SpacelinePanel panel;
    private boolean ownchanging;
    private JButton buttonContainer2Label;

    public XYSeriesTable(DefaultTableModel model, XYSeriesCollection col) {
        super(model);
        this.model = model;
        this.collection = col;

        //Init Tablecolumns
        String ct_show = "Show";
        model.setColumnIdentifiers(new String[]{"Sort", "Series", "Label", "Axis", "Color", "Line", "Shape", "File", "Index"});

        this.setDefaultRenderer(Color.class, new ColorRenderer(false));
        this.setDefaultEditor(Color.class, new ColorEditor(collection));

        this.setDefaultRenderer(BasicStroke.class, new StrokeListRenderer());
        StrokeEditor strokeEditor = new StrokeEditor(this);
        this.setDefaultEditor(BasicStroke.class, strokeEditor);
//        strokeEditor.addCellEditorListener(this);
        SortButtonRenderer sbr = new SortButtonRenderer();
        this.setDefaultRenderer(SortButtonRenderer.class, sbr);
        this.setDefaultEditor(SortButtonRenderer.class, sbr);

        this.setDefaultRenderer(Shape.class, new ShapeListRenderer(null));
        this.setDefaultEditor(Shape.class, new ShapeEditor(this));

        this.setDefaultRenderer(SHAPES.class, new ShapeListRenderer(null));
        this.setDefaultEditor(SHAPES.class, new ShapeEditor(this));

        this.addKeyListener(this);
//        this.model.addTableModelListener(this);
        setColumnSize();
//        this.setAutoCreateRowSorter(true);

    }

    public XYSeriesTable() {
        this(new DefaultTableModel(), new XYSeriesCollection());

    }

    public XYSeriesTable(XYSeriesCollection collection) {
        this(new DefaultTableModel(), collection);
    }

    public void setColumnSize() {
        this.getColumnModel().getColumn(0).setMaxWidth(50);
        this.getColumnModel().getColumn(0).setMinWidth(30);

        this.getColumnModel().getColumn(4).setMaxWidth(80);
        this.getColumnModel().getColumn(4).setMinWidth(50);
        this.getColumnModel().getColumn(4).setWidth(50);

        this.getColumnModel().getColumn(5).setMaxWidth(100);
        this.getColumnModel().getColumn(5).setMinWidth(50);
        this.getColumnModel().getColumn(5).setWidth(50);

        this.getColumnModel().getColumn(6).setMaxWidth(80);
        this.getColumnModel().getColumn(6).setMinWidth(50);
    }

    @Override
    public Class<?> getColumnClass(int i) {
        if (i == 0) {
            return SortButtonRenderer.class;
        }
        if (i == 1) {
            return SeriesKey.class;
        }
        if (i == 2) {
            return String.class; //Display name
        }
        if (i == 3) {
            return AxisKey.class;
        }
        if (i == 4) {
            return Color.class; //Line Color
        }
        if (i == 5) {
            return BasicStroke.class;
        }
        if (i == 6) {
            return Shape.class;
        }
        if (i == 7) {//FileName
            return String.class;
        }
        return super.getColumnClass(i);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == 1 || col == 7) {
            return false;
        }
        return true;
    }

    public void setCollection(XYSeriesCollection collection) {
        this.collection.removeAllSeries();
        for (Object sery : collection.getSeries()) {
            this.collection.addSeries((XYSeries) sery);
        }
        this.updateTableByCollection();
    }

    @Override
    public void setValueAt(Object o, int row, int col) {
        if (row < 0 || row >= model.getRowCount()) {
            return;
        }
        super.setValueAt(o, row, col);
    }

    public void updateTableByCollection() {
        this.ownchanging = true;
//        System.out.println(this.getClass()+":: has "+this.collection.getSeriesCount()+" series.");
        model.setRowCount(collection.getSeriesCount());
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            XYSeries ts = collection.getSeries(i);
            SeriesKey key = (SeriesKey) ts.getKey();
            model.setValueAt(null, i, 0);
            model.setValueAt(key.toString(), i, 1);
            model.setValueAt(key.label, i, 2);
            model.setValueAt(key.axisKey, i, 3);
            model.setValueAt(key.lineColor, i, 4);
            model.setValueAt(key.stroke, i, 5);
            model.setValueAt(key.shape, i, 6);
            model.setValueAt(key.file, i, 7);
        }
        setColumnSize();
        this.revalidate();
        this.repaint();
        updatePanel();
        this.ownchanging = false;
    }

    private void updatePanel() {
        if (panel != null) {
            panel.updateCheckboxPanel();
            panel.updateShownTimeSeries();
        }
    }

    public void setSpacelinePanel(SpacelinePanel panel) {
        this.panel = panel;
    }

    @Override
    public void keyTyped(KeyEvent ke) {

    }

    @Override
    public void keyPressed(KeyEvent ke) {

    }

    @Override
    public void keyReleased(KeyEvent ke) {
//        System.out.println("Keycode: " + ke.getKeyCode() + "\t char=" + ke.getKeyChar() + "\taction:" + ke.isActionKey());
        if (ke.getKeyCode() == 127) {
            //Pressed delete
            int[] selected = this.getSelectedRows();
            XYSeries[] tss = new XYSeries[selected.length];
            for (int i = 0; i < tss.length; i++) {
                tss[i] = collection.getSeries(selected[i]);
            }
            this.getSelectionModel().clearSelection();
            for (int i = 0; i < tss.length; i++) {
//                System.out.println("Remove row " + selected[i] + "\t" + tss[i].getKey());
                collection.removeSeries(tss[i]);
            }
            this.panel.requestFocus();
            this.updateTableByCollection();
        }

    }

    @Override
    public void tableChanged(TableModelEvent tme) {
        if (ownchanging) {
            return;
        }
//        System.out.println("TableModelEvent: " + tme);
        super.tableChanged(tme);
        int column = getSelectedColumn();
        int row = getSelectedRow();
        if (row >= getRowCount() || row < 0) {
            return;
        }

        if (column == 0) {
            if (getRowCount() < 2) {
                return;
            }
            SortButtonRenderer sbr = (SortButtonRenderer) model.getValueAt(row, column);
            if (sbr.getDirection() == SortButtonRenderer.DIRECTION.UP && row > 0) {
                swapSeries(collection, row, row - 1);
                this.updateTableByCollection();
            } else if (sbr.getDirection() == SortButtonRenderer.DIRECTION.DOWN && row < getRowCount() - 1) {
                swapSeries(collection, row, row + 1);
                this.updateTableByCollection();
            }
//            System.out.println("verschieben in Richtung "+sbr.getDirection());
        } else if (column == 2) {
            //Label
            ((SeriesKey) collection.getSeries(row).getKey()).label = model.getValueAt(row, column) + "";
        } else if (column == 3) {
            //axis
            String text = model.getValueAt(row, column).toString();
            AxisKey oldKey = ((SeriesKey) collection.getSeries(row).getKey()).axisKey;
            AxisKey newKey = null;
            if (text.contains(",")) {
                String[] splits = text.split(",", 2);
                newKey = new AxisKey(splits[0], splits[1]);
            } else {
                if (oldKey.name.equals(text) && oldKey.label == null) {
                    //Nothing to do. the existing Key is identical to the new text
                } else {
                    newKey = new AxisKey(text);
                }
            }
            if (newKey != null) {
                ((SeriesKey) collection.getSeries(row).getKey()).axisKey = newKey;
            }
        } else if (column == 5) {
            //Stroke
            ((SeriesKey) collection.getSeries(row).getKey()).stroke = (BasicStroke) model.getValueAt(row, column);
//            System.out.println("set stroke for series "+row+" -> "+((SeriesKey) collection.getSeries(row).getKey()).stroke);
        } else if (column == 6) {
            //Stroke
            ((SeriesKey) collection.getSeries(row).getKey()).shape = (SHAPES) model.getValueAt(row, column);
//            System.out.println("set shape for series "+row+" -> "+((SeriesKey) collection.getSeries(row).getKey()).shape);
        }
        updatePanel();
    }

    public void swapSeries(XYSeriesCollection collection, int index0, int index1) {
        XYSeries[] ts = new XYSeries[collection.getSeriesCount()];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = collection.getSeries(i);
        }
        XYSeries atrow = ts[index0];
        ts[index0] = ts[index1];
        ts[index1] = atrow;
        collection.removeAllSeries();

        updatePanel();
        for (int i = 0; i < ts.length; i++) {
            collection.addSeries(ts[i]);
        }
        updatePanel();
    }

}
