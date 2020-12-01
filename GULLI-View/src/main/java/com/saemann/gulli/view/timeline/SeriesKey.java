package com.saemann.gulli.view.timeline;

import com.saemann.gulli.core.control.StartParameters;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import com.saemann.gulli.core.model.timeline.list.Value;
import org.jfree.data.general.Series;
import com.saemann.gulli.view.timeline.customCell.ShapeEditor;

/**
 *
 * @author saemann
 * @param <E>
 */
public class SeriesKey<E> implements Comparable<SeriesKey> {

    public enum YAXIS {

        OWN, CONCENTRATION, VELOCITY, WATERLVL
    };

    private static BasicStroke defaultstroke = new BasicStroke(1.5f);

    public final String name, symbol, unit;
    public Color lineColor;
    public AxisKey axisKey = null;
    public int containerIndex = 0;
    private boolean visible = true;
    public String label;
    public String file;
    public final String eventID;
    public boolean renderAsBar = false;
    public boolean logarithmic = false;
    public BasicStroke stroke = defaultstroke;
    public ShapeEditor.SHAPES shape = null;
    public boolean shapeFilled = false;
    public E element;
    public Series timeseries;

    public SeriesKey(Value v, Color lineColor) {
        this(v.getName(), v.getSymbol(), v.getUnit(), lineColor);
    }

    public SeriesKey(E e, Value v, Color lineColor) {
        this(e.toString(), v.getSymbol(), v.getUnit(), lineColor);
    }

    public SeriesKey(Value v, Color lineColor, AxisKey yaxis) {
        this(v, lineColor);
        this.axisKey = yaxis;
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor) {
        this(name, symbol, unit, lineColor, 0);
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor, int containerIndex) {
        this(name, symbol, unit, lineColor, null, containerIndex);
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor, AxisKey yaxis) {
        this(name, symbol, unit, lineColor, yaxis, 0);
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor, AxisKey yaxis, BasicStroke stroke) {
        this(name, symbol, unit, lineColor, yaxis, 0);
        this.stroke = stroke;
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor, AxisKey yaxis, int containerIndex) {
        this(name, symbol, unit, lineColor, yaxis, containerIndex, "");
    }

    public SeriesKey(String name, String symbol, String unit, Color lineColor, AxisKey yaxis, int containerIndex, String file) {
        this.name = name;
        this.symbol = symbol;
        this.unit = unit;
        this.lineColor = lineColor;
        this.axisKey = yaxis;
        this.containerIndex = containerIndex;
        this.file = file;
        this.eventID = extractEventID(name);

        this.label = symbol + " " + name + " [" + unit + "]";
        if (containerIndex > 0) {
            label = symbol + " [" + unit + "](" + containerIndex + ")";
        }

        if (StartParameters.containsTimelineVisibilityInfo(name)) {
            this.visible = StartParameters.isTimelineVisible(name);
        }
    }

    public Shape getShape() {
        if (this.shape == null) {
            return null;
        }
        return this.shape.getShape();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        StartParameters.setTimelineVisibility(name, visible);
    }

    @Override
    public int compareTo(SeriesKey t) {
        if (true) {
            return 0;
        }
        if (t == null) {
            return -1;
        }
        if (t == this) {
            return 0;
        }
        if (this.name == null) {
            return -1;
        }

        if (file != null) {
            return file.compareTo(t.file);
        }

        if (name.equals(t.name)) {
            return this.containerIndex - t.containerIndex;
        }

        return name.compareTo(t.name);
    }

    public String extractEventID(String name) {
        if (name.contains("_p")) {
            return name.substring(name.indexOf("_p") + 2);
        }
        return name;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {

        return label;
    }

}
