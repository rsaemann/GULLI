/*
 * The MIT License
 *
 * Copyright 2020 B1.
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
package control.particlecontrol.injection;

import model.topology.Pipe;

/**
 * Holds the information about the position of a injection into a pipe for
 * multiple particles.
 *
 * @author Sämann
 */
public class PipeInjection implements ParticleInjection{
    
    public Pipe pipe;
    public double distanceAlongPipeMeter;

    public PipeInjection(Pipe pipe, float distanceALongPipeMeter) {
        this.pipe = pipe;
        this.distanceAlongPipeMeter = distanceALongPipeMeter;
    }
    
     public PipeInjection(Pipe pipe, double distanceALongPipeMeter) {
        this.pipe = pipe;
        this.distanceAlongPipeMeter = distanceALongPipeMeter;
    }

    @Override
    public boolean spillOnSurface() {
        return false;
    }

    @Override
    public boolean spillinPipesystem() {
       return true;
    }

    @Override
    public Pipe getInjectionCapacity() {
        return pipe;
    }

    public double getDistanceAlongPipeMeter() {
        return distanceAlongPipeMeter;
    }
    
   
}
