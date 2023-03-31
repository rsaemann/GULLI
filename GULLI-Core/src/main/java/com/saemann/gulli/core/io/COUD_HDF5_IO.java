/*
 * The MIT License
 *
 * Copyright 2023 robert.
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
package com.saemann.gulli.core.io;

import com.saemann.gulli.core.model.surface.SurfaceVelocityLoader;
import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Node;
import java.io.File;
import java.util.Map;

/**
 *
 * @author robert
 */
public class COUD_HDF5_IO implements SurfaceVelocityLoader {

    private File file;
    public static HdfFile hdf5;

    public COUD_HDF5_IO(File file) {
        this.file = file;

        hdf5 = new HdfFile(file);

    }

    @Override
    public float[][] loadVelocity(int triangleID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) {
        File f = new File("C:\\Users\\robert\\Downloads\\Temp2D.h5");
        COUD_HDF5_IO hdf = new COUD_HDF5_IO(f);
        System.out.println("HDF5: " + hdf.hdf5.toString());

        System.out.println(" Attributes: " + hdf.hdf5.getAttributes().size());
        for (Map.Entry<String, Attribute> entry : hdf.hdf5.getAttributes().entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println(" Children: " + hdf.hdf5.getChildren().size());
        for (Map.Entry<String, Node> entry : hdf.hdf5.getChildren().entrySet()) {
            System.out.println("  "+entry.getKey()+" = "+entry.getValue());
        }
        
        io.jhdf.GroupImpl q1 = (io.jhdf.GroupImpl) hdf.hdf5.getChild("Q01");
        System.out.println("Q1: "+q1.getClass());
        System.out.println("  File= "+q1.getFile());
        System.out.println("  Attributes="+q1.getAttributes().size());
        System.out.println("  Children: "+q1.getChildren());
        Node x = q1.getChild("Y");
        System.out.println("    Child Y: "+x+"  "+x.getClass());
        io.jhdf.dataset.ContiguousDatasetImpl xdataset=(io.jhdf.dataset.ContiguousDatasetImpl)x;
        System.out.println("      Dimensions: "+xdataset.getDimensions().length);
        double[] xarray=(double[]) xdataset.getData();
        System.out.println("      "+xdataset.getDataType());
        System.out.println("      Length: "+xarray.length);

        System.out.println("end");
    }

}
