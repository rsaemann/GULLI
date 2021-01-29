package org.jtikz;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.saemann.gulli.view.timeline.CapacityTimelinePanel;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.geom.*;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Graphics2D replacement for outputting in TikZ/PGF. Texts will be in TikZ
 * definition while all lines and shapes are packed into a PDF.
 *
 * Here is an example:
 * <pre>
 * TikzPDFGraphics2D t = new TikzPDFGraphics2D();
 * panel.paint(t);
 * </pre> or, alternatively,
 * <pre>
 * t.paintComponent(frame);
 * </pre>
 *
 * @ MIT
 * @author E Robert Sämann
 */
public class TikzPDFGraphics2D extends Graphics2D {

    /**
     * The version of JTikZ.
     */
    public static final String VERSION = "0.1";
    /**
     * The revision date for this version of JTikZ.
     */
    public static final String REV_DATE = "2021-01-18";

    Hashtable<Color, String> colors;
    String preamble;
    int colorId;

//    AffineTransform transform;
    /**
     * If true, the coordinates are applied with the reference point in the
     * upper left corner (java coordinates). If false, the coordinates are used
     * with reference point in lower left position (latex coordinates);
     */
    public boolean ytopdown = false;

    public double rotation = 0;
    boolean closed;
//    Shape currentClip;
    Color color = Color.BLACK;
//    Font font;
//    FontMetrics fontmetrics;

    LinkedList<String> commands;
    protected PrintStream out;

    String pdfFileName;
    PdfGraphics2D g2PDF;
    FileOutputStream fosPDF;
    Document docPDF;
    PdfContentByte cbPDF;
    PdfTemplate tpPDF;

    public static boolean verbose = false;
    DecimalFormat df = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
    DecimalFormat df3 = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));
    Rectangle originalsize;
    private Color background;

    /**
     * Creates a new TikzGraphics2D object that will output the code to
     * <code>system.out</code>.
     */
    public TikzPDFGraphics2D() throws FileNotFoundException {
        this(null, null, null);
    }

    /**
     * Creates a new TikzGraphics2D object that will output the code to the
     * given output stream. If <code>os</code> is <code>null</code> then it will
     * default to <code>system.out</code>.
     *
     * @param os
     */
    public TikzPDFGraphics2D(File directory, String filename, Rectangle canvassize) throws FileNotFoundException {
        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color, String>();
        this.originalsize = canvassize;
        File tikzFile = new File(directory, filename);
        PrintStream os = new PrintStream(tikzFile);
        if (os instanceof PrintStream) {
            out = (PrintStream) os;
        } else {
            out = new PrintStream(os);
        }

        openPDFWriter(canvassize, directory, filename.replaceAll("\\.tikz", "_bgi.pdf").replaceAll("\\.tex", "_bgi.pdf"), true);
        commands = new LinkedList<>();
    }

    @Override
    public Graphics create() {
        try {
            return new TikzPDFGraphics2D();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TikzPDFGraphics2D.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new NullPointerException("Cannot initialize " + getClass() + " without declaration of outputfile");
    }

    private String openPDFWriter(Rectangle size, File directory, String name, boolean override) {
        try {
            String fileName = new String(name);
            if (!fileName.endsWith(".pdf")) {
                fileName += ".pdf";
            }
            File pdfFile = new File(directory, fileName);
            if (pdfFile.exists()) {
                if (!override) {
                    throw new SecurityException("Not allowed to override " + pdfFile);
                } else {
                    System.out.println("Override existing fiel " + pdfFile);
                }
            }

            docPDF = new Document(new com.itextpdf.text.Rectangle(0, 0, size.width, size.height));
            fosPDF = new FileOutputStream(pdfFile);
            PdfWriter writer = PdfWriter.getInstance(docPDF, fosPDF);
            docPDF.open();
            cbPDF = writer.getDirectContent();
            tpPDF = cbPDF.createTemplate((float) size.getWidth(), (float) size.getHeight());
            g2PDF = new PdfGraphics2D(cbPDF, (float) size.getWidth(), (float) size.getHeight());
//            g2PDF.translate(-surroundingContainer.getX(), 0);// -surroundingContainer.getY());
//            panelChart.getChart().draw(g2d, rec);
//            cb.addTemplate(tp, 25, 200);
//            g2d.dispose();
//            doc.close();
//            fos.close();
            pdfFileName = fileName;
            return fileName;
        } catch (Exception ex) {
            Logger.getLogger(CapacityTimelinePanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {

        }
        return null;
    }

    private void closePDF() throws IOException {
        cbPDF.addTemplate(tpPDF, 25, 200);
        g2PDF.dispose();
        docPDF.close();
        fosPDF.close();
    }

    protected String colorToTikz(Color color) {
        String s = colors.get(color);
        if (s == null) {
            preamble += "\\definecolor{color" + colorId + "}{rgb}{" + df3.format(color.getRed() / 255.0) + "," + df3.format(color.getGreen() / 255.0) + "," + df3.format(color.getBlue() / 255.0) + "}\n";
            s = "color" + (colorId++) + (color.getAlpha() < 255 ? ",opacity=" + df3.format(color.getAlpha() / 255.0) : "");
            colors.put(color, s);
        }
        return s;
    }

    protected void addCommand(String command) {

        commands.addLast(command);

    }

    protected LinkedList<String> getCommands() {
        return commands;
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
//        if (stroke.getDashArray() != null && stroke.getDashArray().length > 1) {
//            String str = "dash pattern=";
//
//            for (int i = 0; i < stroke.getDashArray().length; i++) {
//                if (i % 2 == 0) {
//                    str += " on " + Math.max(0.01, stroke.getDashArray()[i]) + "pt";
//                } else {
//                    str += " off " + stroke.getDashArray()[i] + "pt";
//                }
//            }
//            if (stroke.getEndCap() == BasicStroke.CAP_ROUND) {
//                str += ",line cap=round";
//            } else if (stroke.getEndCap() == BasicStroke.CAP_SQUARE) {
//                str += ",line cap=rect";
//            } else if (stroke.getEndCap() == BasicStroke.CAP_BUTT) {
//                str += ",line cap=butt";
//            }
//            addOption(o, str);
//        }
//        if (stroke.getLineWidth() != 1.0) {
//            addOption(o, "line width=" + stroke.getLineWidth() + "pt");
//        }
        rotation = angleRad(g2PDF.getTransform());
        if (rotation != 0) {
            addOption(o, "rotate=" + df.format(rotation * 180 / Math.PI));
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
        g2PDF.drawRenderedImage(image, xform);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return g2PDF.drawImage(img, xform, obs);
    }

    @Override
    public void draw(Shape s) {
        g2PDF.draw(s);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        g2PDF.drawImage(img, op, x, y);
    }

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        Point2D p1 = g2PDF.getTransform().transform(new Point2D.Double(x, y), null);
//        System.out.println(str+" @"+p1+" <-- "+x+", "+y+"  rotation="+rotation);
        handleDrawString(str, p1.getX(), p1.getY(), true);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);

    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        StringBuilder s = new StringBuilder("");
        for (char c = iterator.first(); c != CharacterIterator.DONE; c = iterator.next()) {
            s.append(c);
        }
        drawString(s.toString(), x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        StringBuilder content = new StringBuilder(g.getNumGlyphs());
        for (int i = 0; i < g.getNumGlyphs(); i++) {
            if (g.getGlyphCode(i) == 240) {
                content.append("$^2$");
            } else if (g.getGlyphCode(i) == 241) {
                content.append("$^3$");
            } else {
                content.append((char) (g.getGlyphCode(i) + 29));
            }
        }
        Rectangle2D bounds = g.getVisualBounds();
        Point2D p1 = g2PDF.getTransform().transform(new Point2D.Double(x + bounds.getWidth() * 0.5, y), null);
//        System.out.println("Glyph transf.pos: " + p1 + "   x=" + x + ", y=" + y);
        handleGlyphString(content.toString(), p1.getX(), p1.getY());

    }

    @Override
    public void fill(Shape s) {
        g2PDF.fill(s);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return g2PDF.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return g2PDF.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        g2PDF.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        g2PDF.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        g2PDF.setStroke(s);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        g2PDF.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return g2PDF.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        g2PDF.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        g2PDF.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return getRenderingHints();
    }

    @Override
    public void translate(int x, int y) {
        g2PDF.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        g2PDF.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        g2PDF.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        g2PDF.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        g2PDF.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        g2PDF.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        g2PDF.transform(Tx);
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        g2PDF.setTransform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return g2PDF.getTransform();
    }

    @Override
    public Paint getPaint() {
        return g2PDF.getPaint();
    }

    @Override
    public Composite getComposite() {
        return g2PDF.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        g2PDF.setBackground(color);
        background = color;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Stroke getStroke() {
        return g2PDF.getStroke();
    }

    @Override
    public void clip(Shape s) {
        g2PDF.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return g2PDF.getFontRenderContext();
    }

    @Override
    public Color getColor() {
        return g2PDF.getColor();
    }

    @Override
    public void setColor(Color c) {
        g2PDF.setColor(color);
        this.color = c;
    }

    @Override
    public void setPaintMode() {
        g2PDF.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        g2PDF.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return g2PDF.getFont();
    }

    @Override
    public void setFont(Font font) {
        g2PDF.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return g2PDF.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return g2PDF.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        g2PDF.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        g2PDF.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        if(g2PDF==null)return null;
        return g2PDF.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        g2PDF.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        g2PDF.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        g2PDF.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        g2PDF.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        g2PDF.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g2PDF.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        g2PDF.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        g2PDF.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        g2PDF.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g2PDF.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        g2PDF.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        g2PDF.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g2PDF.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        g2PDF.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return g2PDF.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return g2PDF.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return g2PDF.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return g2PDF.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return g2PDF.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return g2PDF.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        try {
            closePDF();
        } catch (IOException ex) {
            Logger.getLogger(TikzPDFGraphics2D.class.getName()).log(Level.SEVERE, null, ex);
        }
        flushInternal();
        out.flush();
        out.close();
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        g2PDF.drawRenderableImage(img, xform);
    }

    protected void handleDrawString(String s, double x, double y, boolean center) {
        rotation = angleRad(g2PDF.getTransform())*180/Math.PI;
        String layout = "";
        boolean rotated=false;
        if (center) {
            if (Math.abs(rotation)<1) {
                rotated=true;
                x += g2PDF.getFontMetrics().stringWidth(s) * 0.5;
            }
            layout = handleOptions("anchor=center", true);
        } else {
            layout = handleOptions("anchor=west", true);
        }

        if (ytopdown) {
            if (rotated) {
                addCommand("\\node" + layout + " at (" + df.format(x) + "pt, " + df.format(y - g2PDF.getFont().getSize2D() * 0.4) + "pt) {" + toTeX(s) + "};");
            } else {
                addCommand("\\node" + layout + " at (" + df.format(x /*+ g2PDF.getFontMetrics().stringWidth(s) * 0.4/*+ font.getSize2D() * 0.4*/) + "pt, " + df.format(y /*- fontmetrics.stringWidth(s) * 0.5*/) + "pt) {" + toTeX(s) + "};");

            }
        } else {
            if (rotated) {
                addCommand("\\node" + layout + " at (" + df.format(x) + "pt, " + df.format(originalsize.height - y + g2PDF.getFont().getSize2D() * 0.4) + "pt) {" + toTeX(s) + "};");
            } else {
                addCommand("\\node" + layout + " at (" + df.format(x /*+ g2PDF.getFontMetrics().stringWidth(s) * 0.4/*+ font.getSize2D() * 0.4*/) + "pt, " + df.format(originalsize.height - y /*- fontmetrics.stringWidth(s) * 0.5*/) + "pt) {" + toTeX(s) + "};");

            }
        }
    }

    protected void handleGlyphString(String s, double cx, double cy) {

        if (ytopdown) {

            if (rotation == 0) {
                addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(cx) + "pt, " + df.format(cy) + "pt) {" + toTeX(s) + "};");
            } else {
                addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(cx) + "pt, " + df.format(cy) + "pt) {" + toTeX(s) + "};");

            }
        } else {
            if (rotation == 0) {
                addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(cx) + "pt, " + df.format(originalsize.getHeight() - cy) + "pt) {" + toTeX(s) + "};");
            } else {
                addCommand("\\node" + handleOptions("anchor=center", true) + " at (" + df.format(cx) + "pt, " + df.format(originalsize.getHeight() - cy) + "pt) {" + toTeX(s) + "};");

            }
        }
    }

    protected void flushInternal() {
        if ((preamble != null && !preamble.equals("")) || (getCommands() != null && !getCommands().isEmpty())) {
            out.print(preamble);
            if (ytopdown) {
                out.println("\\begin{tikzpicture}[yscale=-1" + (originalsize != null ? ", xscale=\\textwidth/" + this.originalsize.width + "pt" : "") + "]");
            } else {
                if (originalsize != null) {
                    out.println("\\begin{tikzpicture}[yscale=\\textwidth/" + this.originalsize.width + "pt, xscale=\\textwidth/" + this.originalsize.width + "pt]");
                } else {
                    out.println("\\begin{tikzpicture}");
                }

            }
            out.println("\\begin{scope}");
            out.println("  \\path[clip] (0pt, 0pt) rectangle (" + originalsize.width + "pt," + originalsize.height + "pt);");//    \\fill[color=(1.0,1.0,1.0)] (0pt, 0pt) -- ("+originalsize.width+"pt, 0pt) -- ("+originalsize.width+"pt, "+originalsize.height+"pt) -- (0pt, "+originalsize.height+"pt) -- (0pt, 0pt) -- cycle;" );

            out.println("  \\node at (" + df.format(originalsize.width / 2) + "pt," + df.format(originalsize.height / 2) + "pt){\\includegraphics[width=\\textwidth]{" + pdfFileName + "}};");
            /* close out any existing clipping scopes */
//            setClip(null);
            int indent = 1;
            for (int i = 0; i < indent; i++) {
                out.print("  ");
            }

            for (String c : commands) {

                for (int i = 0; i < indent; i++) {
                    out.print("  ");
                }
                out.println(c);
            }

            out.println("\\end{scope}");

            out.println("\\end{tikzpicture}");
        }

        preamble = "";
        colorId = 0;
        colors = new Hashtable<Color, String>();
        commands.clear();
    }

    public double angleRad(AffineTransform aft) {
        Point2D p0 = new Point(0, 0);
        Point2D p1 = new Point(1, 0);
        Point2D pp0 = aft.transform(p0, null);
        Point2D pp1 = aft.transform(p1, null);
        double dx = pp1.getX() - pp0.getX();
        double dy = pp1.getY() - pp0.getY();
        double angle = -Math.atan2(dy, dx);
        return angle;
    }

}
