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
package com.saemann.gulli.core.control.output;

import com.saemann.gulli.core.control.StoringCoordinator;
import java.io.File;

/**
 *
 * @author saemann
 */
public interface OutputIntention {

    /**
     * Storing the requested operation and return the File, if successfull.
     *
     * @param sc StoringCoordinator for data requests.
     * @return successfully written file, null otherwise
     */
    public File writeOutput(StoringCoordinator sc);

    public File getOutputFile();

    public void setOutputFile(File output);

    public String getFileSuffix();

    public String getFilePath();

    public double[] getParameterValuesDouble();

    public void setParameterValueDouble(int index, double value);

    public String[] getParameterNamesDouble();

    public int[] getParameterValuesInt();

    public String[] getParameterNamesInt();

    public void setParameterValueInt(int index, int value);

    public void setFileFormat(StoringCoordinator.FileFormat ff);

    public StoringCoordinator.FileFormat getFileFormat();
     
    /**
     * The file format that can be used by this type of saving.
     * @return 
     */
    public StoringCoordinator.FileFormat[] getSupportedFileFormat();
    
}
