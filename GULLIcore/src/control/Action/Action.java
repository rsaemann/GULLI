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
package control.Action;

/**
 * Object to inform other processes about the progress, this process is doing.
 * Originally developed to display loading progress it is also usefull for
 * parallel working processes.
 *
 * @author saemann
 */
public class Action {

    public String description;
    public Action parent;
    public Action child;
    
    public boolean hasProgress = true;
    /**
     * [0,1]
     */
    public float progress;
    public long startTime;

    /**
     *
     * @param name
     * @param parent
     * @param progressing should progress (%) be part of toString?
     */
    public Action(String name, Action parent, boolean progressing) {
        this.description = name;
        this.parent = parent;
        this.hasProgress = progressing;
        this.progress = 0;
        this.startTime = System.currentTimeMillis();
        if (parent != null) {
            parent.child = this;
        }
    }

    @Override
    public String toString() {
        if (child != null) {
            return child.toString();
        }
        if (hasProgress) {
            return (progress * 100) + "% " + description + (parent != null ? " of " + parent.description : "");
        } else {
            return description + (parent != null ? " of " + parent.description : "");
        }
    }
    
    

    public float getActiveProgress() {
        if (child != null) {
            return child.getActiveProgress();
        }
        return progress;
    }

}
