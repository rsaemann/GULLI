/*
 * The MIT License
 *
 * Copyright 2018 saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.saemann.gulli.core.model.material.routing;

import com.saemann.gulli.core.control.maths.RandomArray;
import com.saemann.gulli.core.control.maths.RandomGenerator;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.surface.SurfaceTrianglePath;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Pipe;

/**
 * Flow Calculator for a material, which can deposit and erode.
 *
 * @author saemann
 */
public class Routing_Heterogene extends Routing_Mixed {

    protected double v_deposite, v_erode, v_deposit_quadrat, v_erode_quadrat;
//    protected Random r = new Random();

    public Routing_Heterogene(double v_deposite, double v_erode) {
        this.v_deposite = v_deposite;
        this.v_erode = v_erode;
        this.v_deposit_quadrat = v_deposite * v_deposite;
        this.v_erode_quadrat = v_erode * v_erode;
    }

    @Override
    public boolean particleIsDepositing(Particle particle, Capacity capacity, RandomGenerator random) {
        // P=1-(v_pipe²/v_deposit²)

        if (capacity instanceof Pipe) {
            double r = random.nextDouble();
            double v_p = ((Pipe) capacity).getStatusTimeLine().getVelocity();
            double p = (1 - (v_p * v_p / v_deposit_quadrat));
            boolean d = r < p;
//            if (particle.getId()==0) {
//                System.out.println("vp=" + v_p + " \tP=" + p + "     random=" + r + "\t deposit? " + d);
//            }
            return d;
        }
        if (capacity instanceof SurfaceTrianglePath) {
            double v_p = ((SurfaceTrianglePath) capacity).getActualVelocity();
            return random.nextDouble() < (1 - (v_p * v_p / v_deposit_quadrat));
        }
        return false;
    }

    @Override
    public boolean particleIsEroding(Particle particle, Capacity capacity, RandomGenerator random) {
        if (capacity instanceof Pipe) {
            double r = random.nextDouble();
            double v_p = ((Pipe) capacity).getStatusTimeLine().getVelocity();
            double p = ((v_p * v_p / v_erode_quadrat) - 1);
            boolean e = r < p;
//            if (particle.getId()==0) {
//                System.out.println("vp=" + v_p + " \tP=" + p + "     random=" + r + "\t erode? " + e);
//            }
            return e;
        }
        if (capacity instanceof SurfaceTrianglePath) {
            double v_p = ((SurfaceTrianglePath) capacity).getActualVelocity();
            return random.nextDouble() < ((v_p * v_p / v_erode_quadrat) - 1);
        }
        return false;
    }

}
