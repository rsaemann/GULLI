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

import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
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
public class TimeSeriesTable extends JTable implements KeyListener, CollectionChangedListener {

    protected final DefaultTableModel model;
    protected TimeSeriesCollection collection;

    protected CapacityTimelinePanel panel;
    private boolean ownchanging;
    private JButton buttonContainer2Label;

    protected StrokeListRenderer strokelistrenderer = new StrokeListRenderer();
    protected StrokeEditor strokeEditor;
    protected ColorEditor colorEditor;

    protected int indexSort = 0;
    protected int indexKey = 1;
    protected int indexLabel = 2;
    protected int indexShape = 7;
    protected int indexIndex = 9;

    public TimeSeriesTable(DefaultTableModel model, TimeSeriesCollection col) {
        super(model);
        this.model = model;
        this.collection = col;

        //Init Tablecolumns
        String ct_show = "Show";
        model.setColumnIdentifiers(new String[]{"Sort", "Series", "Label", "Axis", "View", "Color", "Line", "Shape", "File", "Index"});

        this.setDefaultRenderer(Color.class, new ColorRenderer(false));
        this.colorEditor = new ColorEditor(collection);
        this.setDefaultEditor(Color.class, colorEditor);

        this.setDefaultRenderer(BasicStroke.class, strokelistrenderer);
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
        this.getTableHeader().setReorderingAllowed(false);
    }

    public TimeSeriesTable() {
        this(new DefaultTableModel(), new TimeSeriesCollection());

    }

    public TimeSeriesTable(TimeSeriesCollection collection) {
        this(new DefaultTableModel(), collection);
    }

    public void setColumnSize() {
        this.getColumnModel().getColumn(indexSort).setMaxWidth(50);
        this.getColumnModel().getColumn(indexSort).setMinWidth(30);

        this.getColumnModel().getColumn(4).setMaxWidth(50);
        this.getColumnModel().getColumn(4).setMinWidth(50);
        this.getColumnModel().getColumn(4).setWidth(50);

        this.getColumnModel().getColumn(5).setMaxWidth(60);
        this.getColumnModel().getColumn(5).setMinWidth(40);
        this.getColumnModel().getColumn(5).setWidth(40);

        this.getColumnModel().getColumn(6).setMaxWidth(70);
        this.getColumnModel().getColumn(6).setMinWidth(50);

        this.getColumnModel().getColumn(indexShape).setMaxWidth(50);
        this.getColumnModel().getColumn(indexShape).setMinWidth(50);

        this.getColumnModel().getColumn(indexIndex).setMaxWidth(40);
        this.getColumnModel().getColumn(indexIndex).setMinWidth(40);
    }

    @Override
    public Class<?> getColumnClass(int i) {
        if (i == indexSort) {
            return SortButtonRenderer.class;
        }
        if (i == indexKey) {
            return SeriesKey.class;
        }
        if (i == indexLabel) {
            return String.class; //Display name
        }
        if (i == 3) {
            return AxisKey.class;
        }
        if (i == 4) {
            return Boolean.class; //Show
        }
        if (i == 5) {
            return Color.class; //Line Color
        }
        if (i == 6) {
            return BasicStroke.class;
        }
        if (i == indexShape) {
            return Shape.class;
        }
        if (i == 8) {//FileName
            return String.class;
        }
        if (i == indexIndex) {
            return Integer.class;
        }
        return super.getColumnClass(i);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == indexKey || col == 8 || col == indexIndex) {
            return false;
        }
        return true;
    }

    public void setCollection(TimeSeriesCollection collection) {
        this.collection = collection;//.removeAllSeries();
//        for (Object sery : collection.getSeries()) {
//            this.collection.addSeries((TimeSeries) sery);
//        }
//        this.updateTableByCollection();
        this.colorEditor.setCollection(collection);
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
        model.setRowCount(collection.getSeriesCount());
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            TimeSeries ts = collection.getSeries(i);
            SeriesKey key = (SeriesKey) ts.getKey();
            model.setValueAt(null, i, indexSort);
            model.setValueAt(key.toString(), i, indexKey);
            model.setValueAt(key.label, i, indexLabel);
            model.setValueAt(key.axis, i, 3);
            model.setValueAt(key.isVisible, i, 4);
            model.setValueAt(key.lineColor, i, 5);
            model.setValueAt(key.stroke, i, 6);
            model.setValueAt(key.shape, i, indexShape);
            model.setValueAt(key.file, i, 8);
            model.setValueAt(key.containerIndex, i, indexIndex);
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

    public SeriesKey getSeriesKey(int row) {
        return (SeriesKey) collection.getSeries(row).getKey();
    }

    public boolean isMarkedAsVisible(int row) {
        return (boolean) model.getValueAt(row, 4);
    }

    public void setTimelinePanel(CapacityTimelinePanel panel) {
        this.panel = panel;
        this.panel.collectionListener.add(this);
    }

    @Override
    public void keyTyped(KeyEvent ke) {

    }

    @Override
    public void keyPressed(KeyEvent ke) {

    }

    @Override
    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == 127) {
            //Pressed delete
            int[] selected = this.getSelectedRows();
            TimeSeries[] tss = new TimeSeries[selected.length];
            for (int i = 0; i < tss.length; i++) {
                tss[i] = collection.getSeries(selected[i]);
            }
            this.getSelectionModel().clearSelection();
            for (TimeSeries ts : tss) {
                collection.removeSeries(ts);
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
        super.tableChanged(tme);
        int column = getSelectedColumn();
        int row = getSelectedRow();
        if (row >= getRowCount() || row < 0) {
            return;
        }
        if (column == indexSort) {
            if (getRowCount() < 2) {
                return;
            }
            SortButtonRenderer sbr = (SortButtonRenderer) model.getValueAt(row, indexSort);
            if (sbr.getDirection() == SortButtonRenderer.DIRECTION.UP && row > 0) {
                swapSeries(collection, row, row - 1);
                this.updateTableByCollection();
            } else if (sbr.getDirection() == SortButtonRenderer.DIRECTION.DOWN && row < getRowCount() - 1) {
                swapSeries(collection, row, row + 1);
                this.updateTableByCollection();
            }
        } else if (column == indexLabel) {
            //Label
            ((SeriesKey) collection.getSeries(row).getKey()).label = model.getValueAt(row, column) + "";
        } else if (column == 3) {
            //axis
            String text = model.getValueAt(row, column).toString();
            AxisKey oldKey = ((SeriesKey) collection.getSeries(row).getKey()).axis;
            AxisKey newKey = null;
            try {
                if (text.contains(",")) {
                    String[] splits = text.split(",", 2);
                    newKey = new AxisKey(splits[0], splits[1]);
                } else {
                    try {
                        if (oldKey.name.equals(text) && oldKey.label == null) {
                            //Nothing to do. the existing Key is identical to the new text
                        } else {
                            newKey = new AxisKey(text);
                        }
                    } catch (Exception e) {
                        newKey = new AxisKey(text);
                    }
                }

                if (text.contains("(")) {
                    String value = text.substring(text.indexOf("(") + 1);
                    value = value.substring(0, value.indexOf(")"));
                    String[] values = value.split(";");
                    AxisKey key = oldKey;
                    if (newKey != null) {
                        key = newKey;
                    }
                    if (values.length > 1) {
                        key.lowerBound = Double.parseDouble(values[0]);
                        key.upperBound = Double.parseDouble(values[1]);
                    } else {
                        key.upperBound = Double.parseDouble(values[0]);
                    }
                    key.manualBounds = true;
                } else {
                    oldKey.manualBounds = false;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (newKey != null) {
                ((SeriesKey) collection.getSeries(row).getKey()).axis = newKey;
            }
        } else if (column == 4) {
            //Visible?
            ((SeriesKey) collection.getSeries(row).getKey()).isVisible = (Boolean) model.getValueAt(row, column);
        } else if (column == 6) {
            //Stroke
            ((SeriesKey) collection.getSeries(row).getKey()).stroke = (BasicStroke) model.getValueAt(row, column);
        } else if (column == indexShape) {
            //Stroke
            ((SeriesKey) collection.getSeries(row).getKey()).shape = (SHAPES) model.getValueAt(row, indexShape);
        }
        updatePanel();
    }

    public void swapSeries(TimeSeriesCollection collection, int index0, int index1) {
        TimeSeries[] ts = new TimeSeries[collection.getSeriesCount()];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = collection.getSeries(i);
        }
        TimeSeries atrow = ts[index0];
        ts[index0] = ts[index1];
        ts[index1] = atrow;
        collection.removeAllSeries();

//        updatePanel();
        for (TimeSeries t : ts) {
            collection.addSeries(t);
        }
        updatePanel();
    }

    @Override
    public void collectionChanged() {
        this.updateTableByCollection();
    }

}
