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
package com.saemann.gulli.core.control.threads;

/**
 * An object, that is notified when a thread has finished its job.
 *
 * @author saemann
 * @param <T>
 */
public abstract class ThreadBarrier<T extends Thread> implements ThreadListener<T> {

    protected final ThreadController notifyWhenReady;

    protected final String name;

    protected boolean isinitialized = false;

    protected long stepStartTime = 0;

    protected long stepEndTime = 0;

    public ThreadBarrier(String name, ThreadController controller) {
        this.name = name;
        notifyWhenReady = controller;
    }

    public void startover() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    public abstract void initialize();

    /**
     * The time in milliseconds in the current scenario, which is valid at the
     * start of the simulation loop.
     * 
     * @param stepstartMS
     */
    public void setStepStartTime(long stepstartMS) {
        this.stepStartTime = stepstartMS;
    }

    /**
     * The time in milliseconds in the current scenario, which is valid at the
     * start of the simulation loop.
     * @return 
     */
    public long getStepStartTime() {
        return stepStartTime;
    }

     /**
     * The time in milliseconds in the current scenario, which is valid at the
     * end of the simulation loop.
     * 
     * @param stependMS
     */
    public void setStepEndTime(long stependMS) {
        this.stepEndTime = stependMS;
    }

    /**
     * The time in milliseconds in the current scenario, which is valid at the
     * end of the simulation loop.
     * @return 
     */
    public long getStepEndTime() {
        return stepEndTime;
    }
    
    
    public String getName() {
        return name;
    }

    public ThreadController getThreadController() {
        return notifyWhenReady;
    }

    @Override
    public String toString() {
        return getName();
    }
}
