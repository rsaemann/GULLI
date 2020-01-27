/*
 * The MIT License
 *
 * Copyright 2020 saemann.
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
package io;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;

/**
 * Read and write to *.gpkg databases
 * @author saemann
 */
public class Geopackage_IO {
    
    public static void main(String[] args) {
        try {
            //Test creation of a single element
            GeoPackage gp=new GeoPackage(new File("L:\\geppackageDatabase.db"));
            gp.init();
            gp.addCRS(4326);
//            gp.add(new FeatureEntry(), new AbstractFeatureCollection);
        } catch (IOException ex) {
            Logger.getLogger(Geopackage_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
