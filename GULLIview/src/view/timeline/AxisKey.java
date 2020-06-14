package view.timeline;

import java.util.Objects;

/**
 *
 * @author saemann
 */
public class AxisKey {

    public final String name;

    public String label;

    public boolean manualBounds = false;
    public double lowerBound = 0;
    public double upperBound = 0;

    public int drawInterval = 1;

    public boolean logarithmic = false;

    public AxisKey(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public AxisKey(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AxisKey other = (AxisKey) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
//        System.out.println("label:"+label);
        StringBuilder str = new StringBuilder(name);
        if (logarithmic) {
            str.append("log");
        }
        if (manualBounds) {
            str.append("(" + lowerBound + ";" + upperBound + ")");
        }
        if (label != null && !label.isEmpty() && !label.equals("null")) {
            str.append(",").append(label);
        }

        return str.toString();
    }

    public static AxisKey CONCENTRATION() {
        return new AxisKey("CONCENTRATION", "c Concentration [kg/m³]");
    }
}
