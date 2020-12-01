/*
 * The MIT License
 *
 * Copyright 2020 Robert Sämann.
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
package com.saemann.gulli.core.control.output;

import com.saemann.gulli.core.control.StoringCoordinator;
import com.saemann.gulli.core.io.extran.HE_SurfaceIO;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saemann
 */
public class ContaminationParticles implements OutputIntention {

    public StoringCoordinator.FileFormat fileformat = StoringCoordinator.FileFormat.CSV;

    /**
     * -1=all materials
     */
    public int materialIndex = -1;

    /**
     * should the shape consist of all timesteps? if false, the current (last
     * saved) timestep is used.
     */
    public boolean cumulative = true;

    /**
     * factor for multiplication. output is only written, if the number of
     * particles is >= minimumParticleCount. standard=1;
     */
    public int minimumParticleCount = 1;

    @Override
    public File writeOutput(StoringCoordinator sc) {
        try {
            File file = new File(sc.getFileRoot(), "Contamination.csv");
            HE_SurfaceIO.writeSurfaceContaminationCSV(file, sc.getSurface());
            return file;
        } catch (IOException ex) {
            Logger.getLogger(ContaminationParticles.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
