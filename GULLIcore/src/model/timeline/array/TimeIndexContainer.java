/*
 * The MIT License
 *
 * Copyright 2017 saemann.
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
package model.timeline.array;

/**
 * Contains timestamps and calculates the int/double index for given Datetimes.
 *
 * @author saemann
 */
public class TimeIndexContainer extends TimeContainer implements TimeIndexCalculator {

    private long actualTime;
    private int actualTimeIndex;
    private double actualTimeIndex_double;

    public TimeIndexContainer(TimeContainer cont) {
        super(cont);
    }

    public TimeIndexContainer(long[] times) {
        super(times);
    }
    
    @Override
    public int getActualTimeIndex() {
        return actualTimeIndex;
    }

    @Override
    public double getActualTimeIndex_double() {
        return actualTimeIndex_double;
    }

    @Override
    public void setActualTime(long actualTime) {
        this.actualTime = actualTime;
        actualTimeIndex = getTimeIndex(actualTime);
        actualTimeIndex_double = getTimeIndexDouble(actualTime);
    }

    @Override
    public long getActualTime() {
        return actualTime;
    }

    @Override
    public long getStartTime() {
        return times[0];
    }

    @Override
    public long getEndTime() {
        return times[times.length - 1];
    }

}
