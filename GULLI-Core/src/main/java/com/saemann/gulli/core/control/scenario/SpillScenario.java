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
package com.saemann.gulli.core.control.scenario;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.scenario.injection.InjectionInfo;
import java.util.ArrayList;
import java.util.Date;
import com.saemann.gulli.core.model.timeline.array.TimeIndexCalculator;

/**
 * Informtion about the time and spill sources of one simulation
 *
 * @author saemann
 */
public class SpillScenario extends Scenario {

    private final ArrayList<InjectionInfo> injections;

    public SpillScenario(TimeIndexCalculator container, ArrayList<InjectionInfo> injections) {
        super();
        if (injections == null) {
            this.injections = new ArrayList<>(1);
        } else {
            this.injections = injections;
        }

//        if (container instanceof ArrayTimeLinePipeContainer) {
        if (container != null) {
            this.starttime = container.getStartTime();
            this.endtime = container.getEndTime();
            this.setStatusTimesPipe(container);
        }
//        }else if(container instanceof SparseTimeLinePipeContainer){
//            this.setStatusTimesPipe(timesPipe);
//        }
    }

    @Override
    public ArrayList<InjectionInfo> getInjections() {
        return injections;
    }

    @Override
    public void init(Controller c) {
    }

    @Override
    public void reset() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + new Date(starttime) + ", " + injections.size() + " injections}";
    }

}
