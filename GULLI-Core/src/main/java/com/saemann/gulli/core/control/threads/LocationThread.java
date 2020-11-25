/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.control.threads;

import com.saemann.gulli.core.model.particle.Particle;

/**
 * This Thread updates the 2d-position of Particles. It only has to be status,
 * when the View is status.
 *
 * @author saemann
 */
public class LocationThread extends Thread {

    private final ThreadBarrier barrier;
//    private PaintManager paintManager = null;

    protected Particle[] particles;
    private boolean runendless = true;

    public LocationThread(String name, ThreadBarrier barrier) {
        super(name);
        this.barrier = barrier;
    }

    public void setParticles(Particle[] particles) {
        this.particles = particles;
    }

//    public void setPaintManager(PaintManager paintManager) {
//        this.paintManager = paintManager;
//    }
    public void updateParticlePositions() {
//        if (paintManager != null && paintManager.getParticlePaintings() != null) {
//            try {
//                synchronized (paintManager.getParticlePaintings()) {
//                    for (NodePainting particlePainting : paintManager.getParticlePaintings()) {
//                        try {
//                            Particle p = (Particle) particlePainting.getPosition();
//        if (particles != null) {
//            for (Particle p : particles) {

//                if (p.getSurrounding_actual() == null || !p.isActive()) {
//                    continue;
//                }
                            // Wenn 2D position schon gesetzt{
                // nur in lat/long umrechnen.  
                //}else{
//                if (p.getPosition2d_actual() != null) {
//                    Coordinate utm = null;
//                    try {
//                        utm = p.getPosition2d_actual().get3DCoordinate();
//                        if (!Double.isNaN(utm.x)) {
//                            Coordinate longlat = barrier.notifyWhenReady.control.getSurface().getGeotools().toGlobal(utm, true);
//                            p.setPosition3d(new Position3D(longlat.x, longlat.y, utm.x, utm.y, 0));
//                        }
//                    } catch (TransformException transformException) {
//                        System.err.println(getClass() + " Particle: " + p.getId() + ". Wrong surface transformation for UTM " + utm + "   in " + p.getSurrounding_actual() + "  Triangle:" + p.surfaceCellID);
//                    }
//                } else {
//                    p.setPosition3d(p.getSurrounding_actual().getPosition3D(p.getPosition1d_actual()));
//                }
//                            int relV = (int) (p.getVelocity1d() * 100 / 2);
//                            particlePainting.setColor(paintManager.getColorHolderVelocityRelative(relV));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            paintManager.updateLabel();
//            }
//        }

    }

    @Override
    public void run() {
        barrier.initialized(this);
        while (runendless) {
            updateParticlePositions();
            barrier.loopfinished(this);
        }

    }

    public void stopThread() {
        this.runendless = false;
    }

    public void reset() {
//        if (paintManager != null && paintManager.getParticlePaintings() != null) {
//            try {
//                for (NodePainting particlePainting : paintManager.getParticlePaintings()) {
//                    try {
//                        Particle p = (Particle) particlePainting.getPosition();
//                        if (p.getSurrounding_actual() == null) {
//
//                            continue;
//                        }
//                        p.setPosition3d(p.getSurrounding_actual().getPosition3D(p.getPosition1d_actual()));
//
//                        int relV = (int) (p.getVelocity1d() * 100 / 5f);
//                        particlePainting.setColor(paintManager.getColorHolderVelocityRelative(relV));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}
