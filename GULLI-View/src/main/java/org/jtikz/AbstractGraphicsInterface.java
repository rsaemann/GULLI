package org.jtikz;

import java.text.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

public abstract class AbstractGraphicsInterface extends Graphics2D implements Closeable, Flushable {

    LinkedList<AbstractGraphicsInterface> children;
    AbstractGraphicsInterface parent;
    AffineTransform transform;
    public double rotation = 0;
    boolean closed;
    String preamble;
    Shape currentClip;
    Color color;
    Font font;
    FontMetrics fontmetrics;
    Thread shutdownHook;
    Color background;
    BasicStroke stroke;
    LinkedList<GraphicsCommand> commands;

    protected PrintStream out;

    public static boolean verbose = false;

    protected static enum Action {
        DRAW, FILL, CLIP
    };

    protected void addCommand(Object command) {
        if (parent != null) {
            parent.addCommand(command);
        } else {
            if (verbose) {
                System.err.println("Command: " + command);
            }
            commands.addLast(new GraphicsCommand(command, this));
        }
    }

    protected LinkedList<GraphicsCommand> getCommands() {
        return commands;
    }

    protected abstract AbstractGraphicsInterface newInstance();

    public final AbstractGraphicsInterface create() {
        if (verbose) {
            System.err.println("create()");
        }
        AbstractGraphicsInterface g = newInstance();
        children.add(g);
        g.parent = this;
        g.setClip(getClip());
        g.transform = new AffineTransform(transform);
        if (verbose) {
            System.err.println("Transform: " + g.transform);
        }
        return g;
    }

    public final AbstractGraphicsInterface create(int x, int y, int width, int height) {
        if (verbose) {
            System.err.println("create(" + x + ", " + y + ", " + width + ", " + height + ", " + currentClip + ")");
        }
        AbstractGraphicsInterface g = create();
        g.setClip(x, y, width, height);
        g.translate(x, y);
        return g;
    }

    protected AbstractGraphicsInterface getParent() {
        return parent;
    }

    public AbstractGraphicsInterface() {
        this(System.out);
    }

    public AbstractGraphicsInterface(OutputStream os) {
        if (os == null) {
            os = System.out;
        }
        if (os instanceof PrintStream) {
            out = (PrintStream) os;
        } else {
            out = new PrintStream(os);
        }
        parent = null;
        transform = new AffineTransform();
        closed = false;
        preamble = "";
        shutdownHook = new Thread() {
            public void run() {
                flush();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        flush();
    }

    public void setColor(Color c) {
        this.color = c;
    }

    public Color getColor() {
        return color;
    }

    public void draw(Shape shape) {

//        if(shape == ShapeEditor.SHAPES.ELLIPSE_L.getShape()||shape == ShapeEditor.SHAPES.ELLIPSE_M.getShape()||shape == ShapeEditor.SHAPES.ELLIPSE_S.getShape()){
//            System.out.println("DrawEllipse");
//            handleOval(0, 0, ((Ellipse2D) shape).getWidth(), ((Ellipse2D) shape).getHeight(), false);
//        }
        handlePath(shape.getPathIterator(transform), Action.DRAW);
    }

    public void fill(Shape shape) {
        handlePath(shape.getPathIterator(transform), Action.FILL);
    }

    public void drawPolygon(Polygon p) {
        handlePath(p.getPathIterator(transform), Action.DRAW);
    }

    public void fillPolygon(Polygon p) {
        handlePath(p.getPathIterator(transform), Action.FILL);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        fillPolygon(new Polygon(xPoints, yPoints, nPoints));
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        drawPolygon(new Polygon(xPoints, yPoints, nPoints));
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints < 2) {
            return;
        } else if (xPoints[0] == xPoints[nPoints - 1] && yPoints[0] == yPoints[nPoints - 1]) {
            drawPolygon(xPoints, yPoints, nPoints);
        } else {
            double newx[] = new double[xPoints.length];
            double newy[] = new double[yPoints.length];
            Point2D.Double p1 = new Point2D.Double();
            Point2D.Double p2 = new Point2D.Double();
            for (int i = 0; i < nPoints; i++) {
                p1.x = xPoints[i];
                p1.y = yPoints[i];
                transform.transform(p1, p2);
                newx[i] = p2.getX();
                newy[i] = p2.getY();
            }
            handlePolyline(newx, newy, nPoints);
        }
    }

    protected abstract void handlePath(PathIterator path, Action action);

    /**
     * By default this implementation calls handleLine() for each line segment.
     * You can extend this function if you would like a more intelligent
     * implementation given your specific interface.
     */
    protected void handlePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        for (int i = 1; i < nPoints; i++) {
            handleLine(xPoints[i - 1], yPoints[i - 1], xPoints[i], yPoints[i]);
        }
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        Point2D p1 = transform.transform(new Point2D.Double(x1, y1), null);
        Point2D p2 = transform.transform(new Point2D.Double(x2, y2), null);
        handleLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    protected abstract void handleLine(double x1, double y1, double x2, double y2);

    public void drawOval(int x, int y, int width, int height) {
        handleOval(x, y, width, height, false);
    }

    public void fillOval(int x, int y, int width, int height) {
        handleOval(x, y, width, height, true);
    }

    private void handleOval(int x, int y, int width, int height, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleOval(p1.getX(), p1.getY(), width, height, fill);
    }

    protected abstract void handleOval(double x, double y, double width, double height, boolean fill);

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        handleRoundRect(x, y, width, height, arcWidth, arcHeight, true);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        handleRoundRect(x, y, width, height, arcWidth, arcHeight, false);
    }

    public void fillRect(int x, int y, int width, int height) {
        handleRoundRect(x, y, width, height, 0, 0, true);
    }

    public void drawRect(int x, int y, int width, int height) {
        handleRoundRect(x, y, width, height, 0, 0, false);
    }

    private void handleRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleRoundRect(p1.getX(), p1.getY(), width, height, arcWidth, arcHeight, fill);
    }

    protected abstract void handleRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight, boolean fill);

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        handleArc(x, y, width, height, startAngle, arcAngle, false);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        handleArc(x, y, width, height, startAngle, arcAngle, true);
    }

    private void handleArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleArc(p1.getX(), p1.getY(), width, height, startAngle, arcAngle, fill);
    }

    protected abstract void handleArc(double x, double y, double width, double height, int startAngle, int arcAngle, boolean fill);

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        /* TODO: implement this later! */
    }

    public void clearRect(int x, int y, int width, int height) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleClearRect(p1.getX(), p1.getY(), width, height);
    }

    protected abstract void handleClearRect(double x, double y, double width, double height);

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color color) {
        background = color;
    }

    public Composite getComposite() {
        return AlphaComposite.getInstance(AlphaComposite.SRC);
    }

    public void setComposite(Composite composite) {
//         System.err.println(getClass()+"::"+"setComposite"+" not implemented yet");
        /* TODO: implement this later! */
    }

    public Paint getPaint() {
        return color;
    }

    public void setPaint(Paint paint) {
        if (paint instanceof Color) {
            setColor((Color) paint);
        } else //            System.out.println("Set paint is "+paint+", handling not implemented yet");
        {
            return;
            /* TODO: implement this later! */
        }
    }

    public void flush() {
        if (closed) {
            throw new IllegalStateException("This AbstractGraphicsInterface has already been closed!");
        }
        if (parent != null) {
            return;
        }
        flushInternal();
        children = new LinkedList<AbstractGraphicsInterface>();
        font = new Font("Arial", Font.PLAIN, 12);
        fontmetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        stroke = new BasicStroke();
        color = Color.BLACK;
        background = Color.WHITE;
        preamble = "";
        commands = new LinkedList<GraphicsCommand>();
    }

    protected abstract void flushInternal();

    public Font getFont() {
//        if(verbose)System.out.println("getFont");
        return font;
    }

    public void setFont(Font font) {
//        if(verbose)System.out.println("setFont");
        this.font = font;
        fontmetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
//        System.out.println("setFont ");
    }

    public FontMetrics getFontMetrics() {
//        if(verbose)System.out.println("getFontmetrics()");
        return fontmetrics;
    }

    public FontMetrics getFontMetrics(Font font) {
//        if(verbose)System.out.println("getFontmetrics("+font+")");
        return fontmetrics;
//        return Toolkit.getDefaultToolkit().getFontMetrics(font);
//        return null; /* TODO: fix this later! */
    }

    public void setXORMode(Color c1) {
//         if(verbose)System.err.println(getClass()+"::"+"setXORMode"+" not implemented yet");
        /* TODO: implement this later! */
    }

    public void setPaintMode() {
//         if(verbose)System.err.println(getClass()+"::"+"setPaintMode"+" not implemented yet");
        /* TODO: implement this later! */
    }

    public void dispose() {
        close();
    }

    public void close() {
        if (!closed) {
            for (AbstractGraphicsInterface child : children) {
                child.close();
            }
            flush();
            closed = true;
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    public FontRenderContext getFontRenderContext() {
//        if(verbose)System.out.println("getFontRenderingContext");
        return new FontRenderContext(transform, true, true);
    }

    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    public void transform(AffineTransform transform) {
//         System.out.println(getClass()+":: transform ("+transform+")");
//         verbose=true;
        this.transform.concatenate(transform);
        if (this.transform.isIdentity()) {
            rotation = 0;
        } else {
            rotation = angleRad(transform) * 180. / Math.PI;
        }
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

    public void setTransform(AffineTransform transform) {
//        System.out.println(getClass()+":: setTransform ("+transform+")");
//        verbose=false;

        this.transform.setTransform(transform);
        if (this.transform.isIdentity()) {
            rotation = 0;
        } else {
            rotation = angleRad(transform) * 180. / Math.PI;
        }
    }

    public void setClip(Shape clip) {
        if (verbose) {
            System.out.println("setClip");
        }
        /* close off existing clips */
        this.currentClip = clip;
    }

    public void setClip(int x, int y, int width, int height) {
        if (verbose) {
            System.out.println("setClip xywh");
        }
        setClip(new Rectangle2D.Double(x, y, width, height));
    }

    public void clip(Shape clip) {
        /* TODO: fix this such that it actually intersects the clips! */
        if (verbose) {
            System.out.println("clip");
        }
        setClip(clip);
    }

    public void clipRect(int x, int y, int width, int height) {
        if (verbose) {
            System.out.println("clipRect");
        }
        clip(new Rectangle2D.Double(x, y, width, height));
    }

    public Shape getClip() {
        if (verbose) {
            System.out.println("getClip");
        }
        return currentClip;
    }

    public Rectangle getClipBounds() {
        if (verbose) {
            System.out.println("getClipBounds");
        }
        return currentClip == null ? null : currentClip.getBounds();
    }

    public void drawString(String s, int x, int y) {
        if (verbose) {
            System.out.println("drawString");
        }
        drawString(s, (float) x, (float) y);
    }

    public void drawString(String s, float x, float y) {
        Point2D p1 = transform.transform(new Point2D.Double(x, y), null);
        handleDrawString(s, p1.getX(), p1.getY());
    }

    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        StringBuffer s = new StringBuffer("");
        for (char c = iterator.first(); c != CharacterIterator.DONE; c = iterator.next()) {
            s.append(c);
        }
        drawString(s.toString(), x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    /**
     *
     * @param s
     * @param x
     * @param y
     */
    protected abstract void handleDrawString(String s, double x, double y);

    private static class Repainter implements Runnable {

        Component component;
        Graphics g;

        public Repainter(Component component, Graphics g) {
            this.component = component;
            this.g = g;
        }

        public void run() {
            //component.repaint();
            component.paint(g);
        }
    }

    public void paintComponent(Component component) {
        System.out.println(getClass() + "::paintComponent");
        javax.swing.RepaintManager old = javax.swing.RepaintManager.currentManager(component);
        javax.swing.RepaintManager.setCurrentManager(new GraphicsInterfaceRepaintManager(this));
        //component.paint(this);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Repainter(component, this));
        } catch (Exception e) {
        }
        javax.swing.RepaintManager.setCurrentManager(old);
    }

    public void drawRenderedImage(RenderedImage image, AffineTransform xform) {
        if (verbose) {
            System.out.println("drawRenederingImage");
        }
        ColorModel c = image.getColorModel();
        Raster r = image.getData();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                /* TODO: implement this later! */
            }
        }
    }

    public void drawImage(BufferedImage image, BufferedImageOp op, int x, int y) {
        if (verbose) {
            System.out.println("drawImage");
        }
        BufferedImage img1 = op.filter(image, null);
        drawImage(img1, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
    }

    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    public abstract void drawRenderableImage(RenderableImage img, AffineTransform xform);

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        if (verbose) {
            System.out.println("setStroke");
        }
        if (stroke instanceof BasicStroke) {
            this.stroke = (BasicStroke) stroke;
        } else {
            return;
            /* TODO: implement this later! */
        }
    }

    public void shear(double shx, double shy) {
        if (verbose) {
            System.out.println("shear");
        }
        transform.shear(shx, shy);
    }

    public void translate(int x, int y) {
        if (verbose) {
            System.out.println("translate");
        }
        translate((double) x, (double) y);
    }

    public void translate(double tx, double ty) {
        if (verbose) {
            System.out.println("translate");
        }
        transform.translate(tx, ty);
    }

    public void scale(double sx, double sy) {
        if (verbose) {
            System.out.println("scale");
        }
        transform.shear(sx, sy);
    }

    @Override
    public void rotate(double theta) {
        if (verbose) {
            System.out.println("rotate");
        }
        transform.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        if (verbose) {
            System.out.println("rotate xy");
        }
        transform.rotate(theta, x, y);
    }

    public Object getRenderingHint(RenderingHints.Key key) {
//         System.err.println(getClass()+"::"+"getRenderingHint"+" not implemented yet");
        return null;
    }

    public RenderingHints getRenderingHints() {
        return new RenderingHints(null);
    }

    public void addRenderingHints(Map<?, ?> hints) {
//         System.err.println(getClass()+"::"+"addRenderingHints1"+" not implemented yet");
        /* TODO: implement this later ! */
    }

    public void addRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
//         System.err.println(getClass()+"::"+"addRenderingHint2"+" not implemented yet");
        /* TODO: implement this later ! */
    }

    public void setRenderingHints(Map<?, ?> hints) {
//         System.err.println(getClass()+"::"+"setRenderingHints1"+" not implemented yet");
        /* TODO: implement this later ! */
    }

    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
//         System.err.println(getClass()+"::"+"setRenderingHint "+hintKey+"="+hintValue+" not implemented yet");
        /* TODO: implement this later ! */
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        if (verbose) {
            System.err.println(getClass() + "::" + "getDeviceConfiguration" + " not implemented yet");
        }
        return null;
        /* TODO: implement this later! */
    }

    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        if (verbose) {
            System.out.println("test for hit");
        }
        return true;
        /* TODO: implement this more intelligently later! */
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        StringBuffer content = new StringBuffer(g.getNumGlyphs());
        for (int i = 0; i < g.getNumGlyphs(); i++) {
            if (g.getGlyphCode(i) == 240) {
                content.append("$^2$");
            } else if (g.getGlyphCode(i) == 241) {
                content.append("$^3$");
            } else {
                content.append((char) (g.getGlyphCode(i) + 29));
            }
//            System.out.println(i+": char:"+g.getGlyphCharIndex(i)+", code: "+g.getGlyphCode(i)+"  candisplay="+font.canDisplay(g.getGlyphCode(i))+"   "+(char)(g.getGlyphCode(i)+29));
//           content.append((char)(g.getGlyphCode(i)+29));
        }
//        System.out.println("Glyphvector: " + content);
        
//        Point2D p1;
//        try {
//            p1 = transform.inverseTransform(new Point2D.Double(x, y), null); 
//            System.out.println(x+","+y+" ->"+transform+" -> "+p1);
//        } catch (NoninvertibleTransformException ex) {
//            Logger.getLogger(AbstractGraphicsInterface.class.getName()).log(Level.SEVERE, null, ex);
//        }
       
handleDrawString(content.toString(), x/**0.95*/, y);
//        handleDrawString(content.toString(), transform.x + font.getSize2D() * 0.5, y - fontmetrics.stringWidth(content.toString()) * 0.5);
        /* TODO: implement this later! */
//        if(verbose){
//            System.err.println(getClass()+"::"+"drawGlyphVector"+" not implemented yet");
//            new Exception("Glyph vector drawing").printStackTrace();
//        }
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return true;
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return true;
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return true;
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return true;
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return true;
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return true;
    }
}
