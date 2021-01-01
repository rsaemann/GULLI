/*
 * The MIT License
 *
 * Copyright 2021 saemann.
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
package com.saemann.gulli.core.control.listener;

import com.saemann.gulli.core.control.Action.Action;
import com.saemann.gulli.core.control.scenario.Scenario;
import com.saemann.gulli.core.model.surface.Surface;
import com.saemann.gulli.core.model.topology.Network;

/**
 * Standard class implementation of LoadingActionListener without actions.
 *
 * @author saemann
 */
public class LoadingActionAdapter implements LoadingActionListener {

    @Override
    public void actionFired(Action action, Object source) {
    }

    @Override
    public void loadNetwork(Network network, Object caller) {
    }

    @Override
    public void loadSurface(Surface surface, Object caller) {
    }

    @Override
    public void loadScenario(Scenario scenario, Object caller) {
    }

}
