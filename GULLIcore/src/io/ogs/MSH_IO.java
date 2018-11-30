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
package io.ogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import model.surface.Surface;

/**
 * Loader for OGS Mesh data to create a Surface.
 *
 * @author saemann
 */
public class MSH_IO {

    public static Surface loadSurface(File mshFile) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(mshFile));
        //Go until find $NODES entry point
        String line;
        float[][] vertices;
        while (br.ready()) {
            line = br.readLine();
            if (line.contains("$NODES")) {
                break;
            }
        }
        //Read number of Vertices
        line = br.readLine();
        line = line.replaceAll(" ", "");
        vertices = new float[Integer.parseInt(line)][3];
        //Start collecting vertices' coordinate
        String[] split;
        while (br.ready()) {
            line = br.readLine();
            if (line.length() < 15) {
                if (line.toUpperCase().contains("$ELEMENTS")) {
                    break;
                }
            }

            split = line.split(" ");
            int id = Integer.parseInt(split[0]);
            for (int i = 0; i < 3; i++) {
                vertices[id][i] = Float.parseFloat(split[i + 1]);
            }
        }
        // Start creation of Elements
        int[][] triangles;
        line = br.readLine();
        line = line.replaceAll(" ", "");
        triangles = new int[Integer.parseInt(line)][3];
        line = br.readLine();
        split = line.split(" ");
        if (split.length != 6 || !split[2].equals("tri")) {
            throw new IOException("mesh file does not consists of triangles. Only triangles can be used to create a Surface.");
        }
        triangles[0][0] = Integer.parseInt(split[3]);
        triangles[0][1] = Integer.parseInt(split[4]);
        triangles[0][2] = Integer.parseInt(split[5]);
        while (br.ready()) {
            line = br.readLine();
            if (line.length() < 10) {
                if (line.toUpperCase().contains("$STOP")) {
                    break;
                }
            }
            split = line.split(" ");
            int id = Integer.parseInt(split[0]);
            for (int i = 0; i < 3; i++) {
                triangles[id][i] = Integer.parseInt(split[i + 3]);
            }
        }
        br.close();
        Surface surf = new Surface(vertices, triangles, null, null, null);
        return surf;
    }

}
