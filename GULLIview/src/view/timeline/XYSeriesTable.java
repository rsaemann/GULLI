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
import org.jfree.data.xy.IntervalXYDataset;

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

    protected int indexSort = 0;
    protected int indexKey = 1;
    protected int indexLabel = 2;
    protected int indexAxis;
    protected int indexLogarithmic;
    protected int indexView;
    protected int indexColor;
    protected int indexLine;
    protected int indexShape = 7;
    protected int indexInterval;
//    protected int indexFile;
//    protected int indexIndex = 9;

    public XYSeriesTable(DefaultTableModel model, XYSeriesCollection col) {
        super(model);
        this.model = model;
        this.collection = col;

        //Init Tablecolumns
//        String ct_show = "Show";
//        model.setColumnIdentifiers(new String[]{"Sort", "Series", "Label", "Axis", "Color", "Line", "Shape", "File", "Index"});
        int column = 0;
        indexSort = column++;
        indexKey = column++;
        indexLabel = column++;
        indexAxis = column++;
        indexLogarithmic = column++;
        indexView = column++;
        indexColor = column++;
        indexLine = column++;
        indexShape = column++;
        indexInterval = column++;
//        indexFile = column++;
//        indexIndex = column++;

        model.setColumnCount(column);
        String[] header = new String[column];
        header[indexSort] = "Sort";
        header[indexKey] = "Series";
        header[indexLabel] = "Label";
        header[indexAxis] = "Axis";
        header[indexLogarithmic] = "Logarithmic";
        header[indexView] = "View";
        header[indexColor] = "Color";
        header[indexLine] = "Line";
        header[indexShape] = "Shape";
        header[indexInterval] = "Interval";
//        header[indexFile] = "File";
//        header[indexIndex] = "Index";

        model.setColumnIdentifiers(header);

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

//    public void setColumnSize() {
//        this.getColumnModel().getColumn(0).setMaxWidth(50);
//        this.getColumnModel().getColumn(0).setMinWidth(30);
//
//        this.getColumnModel().getColumn(4).setMaxWidth(80);
//        this.getColumnModel().getColumn(4).setMinWidth(50);
//        this.getColumnModel().getColumn(4).setWidth(50);
//
//        this.getColumnModel().getColumn(5).setMaxWidth(100);
//        this.getColumnModel().getColumn(5).setMinWidth(50);
//        this.getColumnModel().getColumn(5).setWidth(50);
//
//        this.getColumnModel().getColumn(6).setMaxWidth(80);
//        this.getColumnModel().getColumn(6).setMinWidth(50);
//    }
//
//    @Override
//    public Class<?> getColumnClass(int i) {
//        if (i == 0) {
//            return SortButtonRenderer.class;
//        }
//        if (i == 1) {
//            return SeriesKey.class;
//        }
//        if (i == 2) {
//            return String.class; //Display name
//        }
//        if (i == 3) {
//            return AxisKey.class;
//        }
//        if (i == 4) {
//            return Color.class; //Line Color
//        }
//        if (i == 5) {
//            return BasicStroke.class;
//        }
//        if (i == 6) {
//            return Shape.class;
//        }
//        if (i == 7) {//FileName
//            return String.class;
//        }
//        return super.getColumnClass(i);
//    }
//
//    @Override
//    public boolean isCellEditable(int row, int col) {
//        if (col == 1 || col == 7) {
//            return false;
//        }
//        return true;
//    }
    public void setColumnSize() {
        this.getColumnModel().getColumn(indexKey).setMaxWidth(0);
        this.getColumnModel().getColumn(indexKey).setWidth(0);

        this.getColumnModel().getColumn(indexSort).setMaxWidth(50);
        this.getColumnModel().getColumn(indexSort).setMinWidth(30);

        this.getColumnModel().getColumn(indexView).setMaxWidth(50);
        this.getColumnModel().getColumn(indexView).setMinWidth(50);
        this.getColumnModel().getColumn(indexView).setWidth(50);

        this.getColumnModel().getColumn(indexLogarithmic).setMaxWidth(50);
        this.getColumnModel().getColumn(indexLogarithmic).setMinWidth(50);
        this.getColumnModel().getColumn(indexLogarithmic).setWidth(50);

        this.getColumnModel().getColumn(indexColor).setMaxWidth(60);
        this.getColumnModel().getColumn(indexColor).setMinWidth(40);
        this.getColumnModel().getColumn(indexColor).setWidth(40);

        this.getColumnModel().getColumn(indexLine).setMaxWidth(70);
        this.getColumnModel().getColumn(indexLine).setMinWidth(50);

        this.getColumnModel().getColumn(indexShape).setMaxWidth(50);
        this.getColumnModel().getColumn(indexShape).setMinWidth(50);

        this.getColumnModel().getColumn(indexInterval).setMaxWidth(30);
        this.getColumnModel().getColumn(indexInterval).setMinWidth(30);
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
        if (i == indexAxis) {
            return AxisKey.class;
        }
        if (i == indexLogarithmic) {
            return Boolean.class; //Show
        }
        if (i == indexView) {
            return Boolean.class; //Show
        }
        if (i == indexColor) {
            return Color.class; //Line Color
        }
        if (i == indexLine) {
            return BasicStroke.class;
        }
        if (i == indexShape) {
            return Shape.class;
        }
        if (i == indexInterval) {
            return Integer.class;
        }
        return super.getColumnClass(i);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == indexKey) {
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

//    public void updateTableByCollection() {
//        this.ownchanging = true;
////        System.out.println(this.getClass()+":: has "+this.collection.getSeriesCount()+" series.");
//        model.setRowCount(collection.getSeriesCount());
//        for (int i = 0; i < collection.getSeriesCount(); i++) {
//            XYSeries ts = collection.getSeries(i);
//            SeriesKey key = (SeriesKey) ts.getKey();
//            model.setValueAt(null, i, 0);
//            model.setValueAt(key.toString(), i, 1);
//            model.setValueAt(key.label, i, 2);
//            model.setValueAt(key.axisKey, i, 3);
//            model.setValueAt(key.lineColor, i, 4);
//            model.setValueAt(key.stroke, i, 5);
//            model.setValueAt(key.shape, i, 6);
//            model.setValueAt(key.file, i, 7);
//        }
//        setColumnSize();
//        this.revalidate();
//        this.repaint();
//        updatePanel();
//        this.ownchanging = false;
//    }
    public void updateTableByCollection() {
        this.ownchanging = true;
        model.setRowCount(collection.getSeriesCount());
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            XYSeries ts = collection.getSeries(i);
            SeriesKey key = (SeriesKey) ts.getKey();
            model.setValueAt(null, i, indexSort);
            model.setValueAt(key.toString(), i, indexKey);
            model.setValueAt(key.label, i, indexLabel);
            model.setValueAt(key.axisKey, i, indexAxis);
            model.setValueAt(key.logarithmic, i, indexLogarithmic);
            model.setValueAt(key.isVisible(), i, indexView);
            model.setValueAt(key.lineColor, i, indexColor);
            model.setValueAt(key.stroke, i, indexLine);
            model.setValueAt(key.shape, i, indexShape);
            if (key.axisKey != null) {
                model.setValueAt(key.axisKey.drawInterval, i, indexInterval);
            }
//            model.setValueAt(key.file, i, indexFile);
//            model.setValueAt(key.containerIndex, i, indexIndex);
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
        } else {
            System.err.println("There is no Plotpanel to repaint the collection");
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
//        if (ownchanging) {
//            return;
//        }
////        System.out.println("TableModelEvent: " + tme);
//        super.tableChanged(tme);
//        int column = getSelectedColumn();
//        int row = getSelectedRow();
//        if (row >= getRowCount() || row < 0) {
//            return;
//        }
//
//        if (column == 0) {
//            if (getRowCount() < 2) {
//                return;
//            }
//            SortButtonRenderer sbr = (SortButtonRenderer) model.getValueAt(row, column);
//            if (sbr.getDirection() == SortButtonRenderer.DIRECTION.UP && row > 0) {
//                swapSeries(collection, row, row - 1);
//                this.updateTableByCollection();
//            } else if (sbr.getDirection() == SortButtonRenderer.DIRECTION.DOWN && row < getRowCount() - 1) {
//                swapSeries(collection, row, row + 1);
//                this.updateTableByCollection();
//            }
////            System.out.println("verschieben in Richtung "+sbr.getDirection());
//        } else if (column == 2) {
//            //Label
//            ((SeriesKey) collection.getSeriesKey(row)).label = model.getValueAt(row, column) + "";
//        } else if (column == 3) {
//            //axis
//            String text = model.getValueAt(row, column).toString();
//            AxisKey oldKey = ((SeriesKey) collection.getSeriesKey(row)).axisKey;
//            AxisKey newKey = null;
//            if (text.contains(",")) {
//                String[] splits = text.split(",", 2);
//                newKey = new AxisKey(splits[0], splits[1]);
//            } else {
//                if (oldKey.name.equals(text) && oldKey.label == null) {
//                    //Nothing to do. the existing Key is identical to the new text
//                } else {
//                    newKey = new AxisKey(text);
//                }
//            }
//            if (newKey != null) {
//                ((SeriesKey) collection.getSeries(row).getKey()).axisKey = newKey;
//            }
//        } else if (column == 5) {
//            //Stroke
//            ((SeriesKey) collection.getSeries(row).getKey()).stroke = (BasicStroke) model.getValueAt(row, column);
////            System.out.println("set stroke for series "+row+" -> "+((SeriesKey) collection.getSeries(row).getKey()).stroke);
//        } else if (column == 6) {
//            //Stroke
//            ((SeriesKey) collection.getSeries(row).getKey()).shape = (SHAPES) model.getValueAt(row, column);
////            System.out.println("set shape for series "+row+" -> "+((SeriesKey) collection.getSeries(row).getKey()).shape);
//        }
//        updatePanel();
        if (ownchanging) {
            return;
        }
        super.tableChanged(tme);
        int column = getSelectedColumn();
        int row = getSelectedRow();
        if (row >= getRowCount() || row < 0) {
            return;
        }
        try {
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
            } else if (column == indexAxis) {
                //axis
                String text = model.getValueAt(row, column).toString();
                AxisKey oldKey = ((SeriesKey) collection.getSeries(row).getKey()).axisKey;
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

                        String[] values;
                        if (value.contains(";")) {
                            values = value.split(";");
                        } else {
                            values = value.split(",");
                        }
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
                    ((SeriesKey) collection.getSeries(row).getKey()).axisKey = newKey;
                }
            } else if (column == indexLogarithmic) {
                //Visible?
                ((SeriesKey) collection.getSeries(row).getKey()).logarithmic = ((Boolean) model.getValueAt(row, indexLogarithmic));
                ((SeriesKey) collection.getSeries(row).getKey()).axisKey.logarithmic = ((Boolean) model.getValueAt(row, indexLogarithmic));

            } else if (column == indexView) {
                //Visible?
                ((SeriesKey) collection.getSeries(row).getKey()).setVisible((Boolean) model.getValueAt(row, indexView));
            } else if (column == indexLine) {
                //Stroke
                ((SeriesKey) collection.getSeries(row).getKey()).stroke = (BasicStroke) model.getValueAt(row, indexLine);
            } else if (column == indexShape) {
                //Stroke
                ((SeriesKey) collection.getSeries(row).getKey()).shape = (SHAPES) model.getValueAt(row, indexShape);
            } else if (column == indexInterval) {
                //Plot interval for shapes
                int interval = 1;
                try {
                    interval = (int) model.getValueAt(row, indexInterval);
                } catch (Exception e) {
                }
                if (((SeriesKey) collection.getSeries(row).getKey()).axisKey != null) {
                    ((SeriesKey) collection.getSeries(row).getKey()).axisKey.drawInterval = interval;
                }
            }
            updatePanel();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
