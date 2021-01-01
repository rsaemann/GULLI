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
import static com.saemann.gulli.core.control.StoringCoordinator.verbose;
import com.saemann.gulli.core.io.extran.HE_SurfaceIO;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saemann
 */
public class ContaminationMass implements OutputIntention {

    public StoringCoordinator.FileFormat fileformat = StoringCoordinator.FileFormat.CSV;

    /**
     * -1=all materials
     */
    public int materialIndex = -1;

    /**
     * should the shape consist of all timesteps? if false, the current (last
     * saved) timestep is used.
     */
    public boolean dynamic = true;

    /**
     * factor for multiplication. output is only written, if the number of
     * particles is >= minimumParticleCount. standard=1;
     */
    public int minimumParticleCount = 1;

    private File outputFile = null;

    @Override
    public File writeOutput(StoringCoordinator sc) {
        String materialName;
        if (materialIndex < 0) {
            materialName = "all";
        } else {
            materialName = sc.getMaterial(materialIndex).getName();
        }
        if (materialIndex >= 0) {
            try {
                File output = new File(sc.getFileRoot(), "dyn" + materialName + ".csv");
                HE_SurfaceIO.writeSurfaceContaminationDynamicMassCSV(output, sc.getSurface(), materialIndex);
                if (verbose) {
                    System.out.println("Massdynamic file written to " + output.getAbsolutePath());
                }
                return output;
            } catch (IOException ex) {
                Logger.getLogger(StoringCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public String getFileSuffix() {
        return fileformat.name();
    }

    @Override
    public String getFilePath() {
        return "./dyn#MaterialName#.csv";
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public void setOutputFile(File output) {
        this.outputFile = output;
    }

    @Override
    public double[] getParameterValuesDouble() {
        return null;
    }

    @Override
    public void setParameterValueDouble(int index, double value) {
    }

    @Override
    public String[] getParameterNamesDouble() {
        return null;
    }

    @Override
    public int[] getParameterValuesInt() {
        return new int[]{materialIndex,minimumParticleCount};
    }

    @Override
    public String[] getParameterNamesInt() {
        return new String[]{"Material index (-1=all)","min. Number of particles"};
    }

    @Override
    public void setParameterValueInt(int index, int value) {
        if(index==0){
            this.materialIndex=value;
        }else if(index==1){
            this.minimumParticleCount=value;
        }
    }

    @Override
    public String toString() {
        return "Dynamic Contamination Mass (.csv) of "+(materialIndex<0?" all Materials":"Material "+materialIndex);
    }

    @Override
    public StoringCoordinator.FileFormat getFileFormat() {
        return StoringCoordinator.FileFormat.CSV;
    }

    @Override
    public void setFileFormat(StoringCoordinator.FileFormat ff) {
    }
    
    
    

    
}
