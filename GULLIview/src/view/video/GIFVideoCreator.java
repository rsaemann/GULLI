/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view.video;

import control.io.AnimatedGIFencoder;
import control.listener.SimulationActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import view.MapViewer;

/**
 *
 * @author saemann
 */
public class GIFVideoCreator implements SimulationActionListener {

    public boolean enabled = false;
    /**
     * After X loops, a new frame will be added.
     */
    public int loopsPerFrame = 200;

    public long videoStartDelay = 0;

    public float framesPerSecond = 12;

    public int width = 100, height = 100;

    /**
     * Repeats 
     */
    public int repeats = 3;

    /**
     * Delay in milliseconds.
     */
    public int delay = 1000;

    protected AnimatedGIFencoder encoder;

    protected int framesCaptured = 0;

    protected MapViewer mapViewer;

    protected boolean ready = false;

    public GIFVideoCreator(MapViewer mv) {
        this.mapViewer = mv;
        this.encoder = new AnimatedGIFencoder();
        reset();
    }

    public void reset() {
        encoder.finish();
        encoder.setDelay((int) videoStartDelay);
        encoder.setFrameRate(framesPerSecond);
        this.width = mapViewer.getWidth();
        this.height = mapViewer.getHeight();
        encoder.setSize(width, height);
    }

    public void finish() {
        if (ready) {
            encoder.finish();
        }
        ready = false;
        framesCaptured = 0;
    }

    public void captureOneFrame() {

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        mapViewer.paintMapView(bi.createGraphics());
        captureOneFrame(bi);
    }

    public void captureOneFrame(BufferedImage image) {
        if (!ready) {
            throw new UnsupportedOperationException("No save Path definde for GIF animation.");
        }
        if (encoder.addFrame(image)) {
            framesCaptured++;
        } else {
            System.err.println("Frame was not captured");
        }
    }

    public boolean saveAs(File file, boolean overwrite) {

        if (file.exists() && file.length() > 100 && !overwrite) {
            throw new java.lang.SecurityException("Not allowed to override existing file " + file.getAbsolutePath());
        }
        width = mapViewer.getWidth();
        height = mapViewer.getHeight();
        encoder.setSize(width, height);
        encoder.setFrameRate(framesPerSecond);
        encoder.setRepeat(repeats);
        ready = encoder.start(file.getAbsolutePath());
        return ready;
    }

    @Override
    public void simulationINIT(Object caller) {

    }

    @Override
    public void simulationSTART(Object caller) {
    }

    @Override
    public void simulationSTEPFINISH(long loop, Object caller) {
        if (!enabled) {
            return;
        }
        if ((loop + 1) % loopsPerFrame == 0) {
            captureOneFrame();
        }
    }

    @Override
    public void simulationPAUSED(Object caller) {
    }

    @Override
    public void simulationRESUMPTION(Object caller) {
    }

    @Override
    public void simulationSTOP(Object caller) {
    }

    @Override
    public void simulationFINISH(boolean timeOut, boolean particlesOut) {
    }

    @Override
    public void simulationRESET(Object caller) {
        reset();
    }

    public int getNumberFramesCaptured() {
        return framesCaptured;
    }

    /**
     * Indicates if the encoder is ready to collect frames.
     *
     * @return
     */
    public boolean isReady() {
        return ready;
    }

}
