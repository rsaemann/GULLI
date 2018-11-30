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
package control;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saemann
 */
public class StartParameters {

    private static String streetinletsPath;
    private static String startFilePath;
    private static String pathUndergroundVTU;

    public static boolean JTS_WGS84_LONGITUDE_FIRST = true;

    public static File fileStartParameter = new File(getProgramDirectory(), "GULLI.ini");
    private static boolean isloaded = loadStartParameter();

    public static boolean loadStartParameter() {
        try {
            //Stop that noisy 'location of RGIS is ...' output.
//            StartParameter.verbose = false;
        } catch (Exception e) {
        }
        BufferedReader br = null;
        try {
            if (!fileStartParameter.exists()) {
                saveParameter();
            }
            br = new BufferedReader(new FileReader(fileStartParameter));

            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("streetInlets_ShapefilePath=")) {
                    streetinletsPath = line.substring(line.indexOf("=") + 1);
                } else if (line.startsWith("startFile=")) {
                    startFilePath = line.substring(line.indexOf("=") + 1);
                } else if (line.startsWith("subsurfaceVTU=")) {
                    pathUndergroundVTU = line.substring(line.indexOf("=") + 1);
                }

            }
            br.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private static void saveParameter() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileStartParameter))) {
            bw.write("startFile=" + (startFilePath != null ? startFilePath : ""));
            bw.newLine();
            bw.write("streetInlets_ShapefilePath=" + (streetinletsPath != null ? streetinletsPath : ""));
            bw.newLine();
            bw.write("subsurfaceVTU=" + (pathUndergroundVTU != null ? pathUndergroundVTU : ""));
            bw.flush();
        }
    }

    public static String getStreetinletsPath() {
        return streetinletsPath;
    }

    public static String getStartFilePath() {
        return startFilePath;
    }

    public static String getPathUndergroundVTU() {
        return pathUndergroundVTU;
    }

    private static String getJarName() {
        return new File(LoadingCoordinator.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private static boolean runningFromJAR() {
        String jarName = getJarName();
        return jarName.contains(".jar");
    }

    public static String getProgramDirectory() {
        if (runningFromJAR()) {
            return getCurrentJARDirectory();
        } else {
            return getCurrentProjectDirectory();
        }
    }

    private static String getCurrentProjectDirectory() {
        return new File("").getAbsolutePath();
    }

    private static String getCurrentJARDirectory() {
        try {
            return new File(LoadingCoordinator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

}
