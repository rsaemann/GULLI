package org.jtikz;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.geom.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * A Graphics2D replacement for outputting in TikZ/PGF.
 *
 * Here is an example:
 * <pre>
 * TikzGraphics2D t = new TikzGraphics2D();
 * panel.paint(t);
 * </pre> or, alternatively,
 * <pre>
 * t.paintComponent(frame);
 * </pre>
 *
 * @ LGPL 2
 * @author Evan A. Sultanik, Robert Sämann
 */
public class TikzGraphics2D extends AbstractGraphicsInterface {

    /**
     * The version of JTikZ.
     */
    public static final String VERSION = "0.2";
    /**
     * The revision date for this version of JTikZ.
     */
    public static final String REV_DATE = "2020-12-30";

    Hashtable<Color, String> colors;
    String preamble;
    int colorId;

    public static boolean verbose = false;
    DecimalFormat df = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
    Rectangle originalsize;

    /**
     * Creates a new TikzGraphics2D object that will output the code to
     * <code>system.out</code>.
     */
    public TikzGraphics2D() {
        this(null, null);
    }

    /**
     * Creates a new TikzGraphics2D object that will output the code to the
     * given output stream. If <code>os</code> is <code>null</code> then it will
     * default to <code>system.out</code>.
     *
     * @param os
     */
    public TikzGraphics2D(OutputStream os, Rectangle canvassize) {
        super(os);
        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color, String>();
        this.originalsize = canvassize;
    }

    /**
     *
     * @return
     */
    @Override
    protected TikzGraphics2D newInstance() {
        return new TikzGraphics2D();
    }

    protected String colorToTikz(Color color) {
        if (getParent() != null) {
            return ((TikzGraphics2D) getParent()).colorToTikz(color);
        }
        String s = colors.get(color);
        if (s == null) {
            preamble += "\\definecolor{color" + colorId + "}{rgb}{" + (color.getRed() / 255.0) + "," + (color.getGreen() / 255.0) + "," + (color.getBlue() / 255.0) + "}\n";
            s = "color" + (colorId++) + (color.getAlpha() < 255 ? ",opacity=" + (color.getAlpha() / 255.0) : "");
            colors.put(color, s);
        }
        return s;
    }

    String handleOptions() {
        return handleOptions("");
    }

    String handleOptions(String options) {
        return handleOptions(options, false);
    }

    void addOption(StringBuffer oldOptions, String newOption) {
        if (!oldOptions.toString().equals("") && !newOption.equals("")) {
            oldOptions.append(", ");
        }
        oldOptions.append(newOption);
    }

    String handleOptions(String options, boolean isText) {
        StringBuffer o = new StringBuffer(options);
        if (!color.equals(Color.BLACK)) {
            addOption(o, (isText ? "text=" : "") + colorToTikz(color));
        }
        if (color.getAlpha() != 255) {
            addOption(o, (isText ? "text " : "") + "opacity=" + ((double) color.getAlpha() / 255.0));
        }
        if (stroke.getDashArray() != null && stroke.getDashArray().length > 1) {
            String str = "dash pattern=";

            for (int i = 0; i < stroke.getDashArray().length; i++) {
                if (i % 2 == 0) {
                    str += " on " + Math.max(0.01, stroke.getDashArray()[i]) + "pt";
                } else {
                    str += " off " + stroke.getDashArray()[i] + "pt";
                }
            }
            if (stroke.getEndCap() == BasicStroke.CAP_ROUND) {
                str += ",line cap=round";
            }else if(stroke.getEndCap()==BasicStroke.CAP_SQUARE){
                 str += ",line cap=rect";
            }else if(stroke.getEndCap()==BasicStroke.CAP_BUTT){
                 str += ",line cap=butt";
            }
            addOption(o, str);
        }
        if (stroke.getLineWidth() != 1.0) {
            addOption(o, "line width=" + stroke.getLineWidth() + "pt");
        }
        if (rotation != 0) {
            addOption(o, "rotate=" + rotation);
        }
        if (o.toString().equals("")) {
            return "";
        } else {
            return "[" + o + "]";
        }
    }

    /**
     * Converts an arbitrary string to TeX. This will strip/replace/escape all
     * necessary TeX commands. For example, "\n" will be replaced by "\\".
     *
     * @param s
     * @return
     */
    public String toTeX(String s) {
        return s.replaceAll("\n", "\\\\")
                .replaceAll("&", "\\&")
                .replaceAll("#", "\\#")
                .replaceAll("²", "\\$^2\\$")
                .replaceAll("³", "\\$^3\\$")
                .replaceAll("_", "\\_");
    }

    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform xform) {
        ColorModel c = image.getColorModel();
        Raster r = image.getData();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                /* TODO: implement this later! */
            }
        }
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        /* TODO: implement the affine transform! */
        img.getSource().startProduction(new PixelConsumer(img, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, true));
        return true;
    }

    private class PixelConsumer extends PixelGrabber {

        public PixelConsumer(Image img, int x, int y, int w, int h, boolean forceRGB) {
            super(img, x, y, w, h, forceRGB);
        }

        public void handlesinglepixel(int x, int y, int pixel) {
            int alpha = (pixel >> 24) & 0xff;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;
            addCommand("{\\pgfsys@color@rgb{" + (red / 255.0) + "}{" + (green / 255.0) + "}{" + (blue / 255.0) + "}\\fill (" + (x - 0.5) + "pt, " + (y - 0.5) + "pt) rectangle (" + (x + 0.5) + "pt, " + (y + 0.5) + "pt);}");
        }
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        /* TODO: implement this later! */
//        System.out.println("drawRenderableImage not yet implemented");
    }

    @Override
    protected void handleDrawString(String s, double x, double y) {
        if (rotation == 0) {
            addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(x + fontmetrics.stringWidth(s) * 0.5) + "pt, " + df.format(y - font.getSize2D() * 0.4) + "pt) {" + toTeX(s) + "};");
        } else {
            addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(x + fontmetrics.stringWidth(s) * 0.4/*+ font.getSize2D() * 0.4*/) + "pt, " + df.format(y /*- fontmetrics.stringWidth(s) * 0.5*/) + "pt) {" + toTeX(s) + "};");

        }
    }

    @Override
    protected void handlePath(PathIterator i, Action action
    ) {
        handlePathInternal(i, action);
    }

    String handlePathInternal(PathIterator i, Action action
    ) {
        double s[] = new double[6];
        StringBuffer tikz = new StringBuffer();
        if (action.equals(Action.CLIP)) {
            tikz.append("\\path[clip]");
        } else {
            tikz.append("\\" + (action.equals(Action.FILL) ? "fill" : "draw") + handleOptions());
        }
        while (!i.isDone()) {
            int type = i.currentSegment(s);
            i.next();
            switch (type) {
                case PathIterator.SEG_LINETO:
                    tikz.append(" --");
                case PathIterator.SEG_MOVETO:
                    tikz.append(" (").append(df.format(s[0])).append("pt, ").append(df.format(s[1])).append("pt)");
                    break;
                case PathIterator.SEG_QUADTO:
//                    System.out.println("Quadto");
                    // TODO: Implement this later!
                    break;
                case PathIterator.SEG_CUBICTO:
//                     tikz.append(" [smooth] coordinates {(").append(df.format(s[0])).append("pt, ").append(df.format(s[1])).append("pt) (").append(df.format(s[2])).append("pt, ").append(df.format(s[3])).append("pt)  (").append(df.format(s[4])).append("pt, ").append(df.format(s[5])).append("pt)} ");
//                    break;
                    tikz.append(" -- (").append(df.format(s[0])).append("pt, ").append(df.format(s[1])).append("pt) -- (").append(df.format(s[2])).append("pt, ").append(df.format(s[3])).append("pt) -- (").append(df.format(s[4])).append("pt, ").append(df.format(s[5])).append("pt) ");
                    break;
//                    tikz.append(" .. (").append(df.format(s[0])).append("pt, ").append(df.format(s[1])).append("pt) and (").append(df.format(s[2])).append("pt, ").append(df.format(s[3])).append("pt) .. (").append(df.format(s[4])).append("pt, ").append(df.format(s[5])).append("pt)");
//                    break;
                case PathIterator.SEG_CLOSE:
                    tikz.append(" -- cycle");
                    break;
            }
        }
        tikz.append(";");
        if (!action.equals(Action.CLIP)) {
            addCommand(tikz.toString());
        }
        return tikz.toString();
    }

    protected void drawEllipse(double centerX, double centerY, double width, double height) {
        addCommand("\\draw" + handleOptions() + " (" + df.format(centerX) + "pt, " + df.format(centerY) + "pt) ellipse (" + df.format(width) + "pt and " + df.format(height) + "pt);");
    }

    @Override
    protected void handleLine(double x1, double y1, double x2, double y2) {
        addCommand("\\draw" + handleOptions() + " (" + df.format(x1) + "pt, " + df.format(y1) + "pt) -- (" + df.format(x2) + "pt, " + df.format(y2) + "pt);");
    }

    @Override
    protected void handleOval(double x, double y, double width, double height, boolean fill) {
        double rw = width / 2.0;
        double rh = height / 2.0;
        double cx = x + rw;
        double cy = y + rh;
        addCommand("\\" + (fill ? "fill" : "draw") + handleOptions() + " (" + df.format(cx) + "pt, " + df.format(cy) + "pt) ellipse (" + df.format(rw) + "pt and " + df.format(rh) + "pt);");
    }

    @Override
    protected void handleArc(double x, double y, double width, double height, int startAngle, int arcAngle, boolean fill) {
        double radiusx = width / 2.0;
        double radiusy = height / 2.0;
        double centerx = x + radiusx;
        double centery = y + radiusy;

        double startx = centerx + radiusx * Math.cos(Math.toRadians(-startAngle));
        double starty = centery + radiusy * Math.sin(Math.toRadians(-startAngle));

        addCommand("\\" + (fill ? "fill" : "draw") + handleOptions() + " (" + df.format(centerx) + "pt, " + df.format(centery) + "pt) -- (" + df.format(startx) + "pt, " + df.format(starty) + "pt) arc (" + df.format(-startAngle) + ":" + df.format(((-startAngle) - arcAngle)) + ":" + df.format(radiusx) + "pt and " + df.format(radiusy) + "pt) -- cycle;");
    }

    @Override
    protected void flushInternal() {
        if ((preamble != null && !preamble.equals("")) || (getCommands() != null && !getCommands().isEmpty())) {
            out.print(preamble);
            out.println("\\begin{tikzpicture}[yscale=-1" + (originalsize != null ? ", xscale=\\textwidth/" + this.originalsize.width + "pt" : "") + "]");
            /* close out any existing clipping scopes */
            setClip(null);
            //out.print(tikz);
            int indent = 1;
            Shape lastClip = null;
//            String tikz = "";
            for (GraphicsCommand c : commands) {
                if (!(c.getClip() == lastClip || c.getClip().equals(lastClip))) {
                    while (indent > 1) {
                        indent--;
                        for (int i = 0; i < indent; i++) {
                            out.print("  ");
                        }
                        out.println("\\end{scope}");
                    }
                    for (int i = 0; i < indent; i++) {
                        out.print("  ");
                    }
                    out.println("\\begin{scope}");
                    indent++;
                    for (int i = 0; i < indent; i++) {
                        out.print("  ");
                    }
                    out.print(handlePathInternal(c.getClip().getPathIterator(transform), Action.CLIP));
                    lastClip = c.clip;
                }
                for (int i = 0; i < indent; i++) {
                    out.print("  ");
                }
                out.println(c.command);
            }
            while (indent > 1) {
                indent--;
                for (int i = 0; i < indent; i++) {
                    out.print("  ");
                }
                out.println("\\end{scope}");
            }
            out.println("\\end{tikzpicture}");
        }

        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color, String>();
    }

    @Override
    protected void handlePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        StringBuilder tikz = new StringBuilder();
        tikz.append("\\draw (").append(df.format(xPoints[0])).append("pt, ").append(df.format(yPoints[0])).append("pt)");
        for (int i = 1; i < nPoints; i++) {
            tikz.append(" -- (").append(df.format(xPoints[i])).append("pt, ").append(df.format(yPoints[i])).append("pt)");
        }
        tikz.append(";");
        addCommand(tikz.toString());
    }

    @Override
    protected void handleRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight, boolean fill) {
        String tikz = "";
        double angle = (double) (arcWidth + arcHeight) / 2.0;
        String options = "";
        if (angle != 0.0) {
            options = "rounded corners =" + angle + "pt";
        }
        tikz += "\\" + (fill ? "fill" : "draw") + handleOptions(options) + " (" + df.format(x) + "pt, " + df.format(y) + "pt) -- (" + df.format(x + width - 1) + "pt, " + df.format(y) + "pt) -- (" + df.format(x + width - 1) + "pt, " + df.format(y + height - 1) + "pt) -- (" + df.format(x) + "pt, " + df.format(y + height - 1) + "pt) -- cycle;";
        addCommand(tikz);
    }

    @Override
    protected void handleClearRect(double x, double y, double width, double height) {
        addCommand("\\fill[" + colorToTikz(background) + "] (" + df.format(x) + "pt, " + df.format(y) + "pt) -- (" + df.format(x + width - 1) + "pt, " + df.format(y) + "pt) -- (" + df.format(x + width - 1) + "pt, " + df.format(y + height - 1) + "pt) -- (" + df.format(x) + "pt, " + df.format(y + height - 1) + "pt) -- cycle;");
    }
}
