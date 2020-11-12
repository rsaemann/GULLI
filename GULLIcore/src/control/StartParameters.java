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

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

    private static String pictureExportPath;
    private static int controlFrameX = 40, controlFrameY = 50, controlFrameW = 270, controlFrameH = 1000;
    private static int mapFrameX = 330, mapFrameY = 50, mapFrameW = 1100, mapFrameH = 800;
    private static int timelinepanelX = 1200;
    private static int timelinepanelY = 200;
    private static int timelinepanelWidth = 500;
    private static int timelinepanelHeight = 432;
    private static double timelinepanelSplitposition = 0.7;
    private static int timelinePanelLegendPosition = 1;

    public static boolean JTS_WGS84_LONGITUDE_FIRST = true;

    public static HashMap<String, Boolean> timelineVisibility = new HashMap<>(10);

    public static File fileStartParameter = new File(getProgramDirectory(), "GULLI.ini");
    private static boolean isloaded = loadStartParameter();

    public static Locale formatLocale = Locale.US;
    public static TimeZone formatTimeZone = TimeZone.getTimeZone("UTC");

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
                    System.out.println("startfile read from ini is:'" + startFilePath + "'");
                } else if (line.startsWith("subsurfaceVTU=")) {
                    pathUndergroundVTU = line.substring(line.indexOf("=") + 1);
                } else if (line.startsWith("mapFrame")) {
                    try {
                        String[] values = line.substring(line.indexOf("=") + 1).split(",");
                        mapFrameX = Integer.parseInt(values[0]);
                        mapFrameY = Integer.parseInt(values[1]);
                        mapFrameW = Integer.parseInt(values[2]);
                        mapFrameH = Integer.parseInt(values[3]);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                } else if (line.startsWith("controlFrame")) {
                    try {
                        String[] values = line.substring(line.indexOf("=") + 1).split(",");
                        controlFrameX = Integer.parseInt(values[0]);
                        controlFrameY = Integer.parseInt(values[1]);
                        controlFrameW = Integer.parseInt(values[2]);
                        controlFrameH = Integer.parseInt(values[3]);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                } else if (line.startsWith("plotFrame")) {
                    try {
                        String[] values = line.substring(line.indexOf("=") + 1).split(",");
                        timelinepanelX = Integer.parseInt(values[0]);
                        timelinepanelY = Integer.parseInt(values[1]);
                        timelinepanelWidth = Integer.parseInt(values[2]);
                        timelinepanelHeight = Integer.parseInt(values[3]);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                } else if (line.startsWith("pictureExportPath")) {
                    pictureExportPath = line.substring(line.indexOf("=") + 1);
                } else if (line.startsWith("timelineplot.width")) {
                    try {
                        timelinepanelWidth = (int) Double.parseDouble(line.substring(line.indexOf("=") + 1));
                    } catch (Exception exception) {
                        timelinepanelWidth = 200;
                    }
                } else if (line.startsWith("timelineplot.height")) {
                    try {
                        timelinepanelHeight = (int) Double.parseDouble(line.substring(line.indexOf("=") + 1));
                    } catch (Exception exception) {
                        timelinepanelHeight = 200;
                    }
                } else if (line.startsWith("timelineplot.split")) {
                    try {
                        timelinepanelSplitposition = Double.parseDouble(line.substring(line.indexOf("=") + 1));
                    } catch (Exception exception) {
                        timelinepanelSplitposition = 200;
                    }
                } else if (line.startsWith("timelineplot.legend")) {
                    try {
                        timelinePanelLegendPosition = Integer.parseInt(line.substring(line.indexOf("=") + 1));
                    } catch (Exception exception) {
                        timelinepanelSplitposition = 200;
                    }
                } else if (line.startsWith("timeline=")) {
                    try {
                        String text = line.substring(line.indexOf("=") + 1);
                        String name = text.substring(2);
                        boolean visible = text.charAt(0) == '1';
                        timelineVisibility.put(name, visible);
                    } catch (Exception exception) {
                        timelinepanelSplitposition = 200;
                    }
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
            bw.newLine();
            bw.newLine();
            bw.write("## Frame bounds");
            bw.newLine();
            bw.write("mapFrame=" + mapFrameX + "," + mapFrameY + "," + mapFrameW + "," + mapFrameH);
            bw.newLine();
            bw.write("controlFrame=" + controlFrameX + "," + controlFrameY + "," + controlFrameW + "," + controlFrameH);
            bw.newLine();
            bw.write("plotFrame=" + timelinepanelX + "," + timelinepanelY + "," + timelinepanelWidth + "," + timelinepanelHeight);
            bw.newLine();
            bw.newLine();
            bw.write("## Plot properties");
            bw.newLine();
            bw.write("pictureExportPath=" + pictureExportPath);
            bw.newLine();
            bw.write("timelineplot.split=" + timelinepanelSplitposition);
            bw.newLine();
            bw.write("timelineplot.legend=" + timelinePanelLegendPosition);
            bw.newLine();
            bw.write("## Timelines shown");
            for (Map.Entry<String, Boolean> entry : timelineVisibility.entrySet()) {
                bw.newLine();
                bw.write("timeline=" + (entry.getValue() ? "1" : "0") + ":" + entry.getKey());
            }
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

    public static String getPictureExportPath() {
        return pictureExportPath;
    }

    public static void setPictureExportPath(String pictureExportPath) {
        StartParameters.pictureExportPath = pictureExportPath;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double getTimelinepanelWidth() {
        return timelinepanelWidth;
    }

    public static void setTimelinepanelWidth(double timelinepanelWidth) {
        StartParameters.timelinepanelWidth = (int) timelinepanelWidth;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double getTimelinepanelHeight() {
        return timelinepanelHeight;
    }

    public static void setTimelinepanelHeight(double timelinepanelHeight) {
        StartParameters.timelinepanelHeight = (int) timelinepanelHeight;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static int getTimelinePanelLegendPosition() {
        return timelinePanelLegendPosition;
    }

    public static void setTimelinePanelLegendPosition(int timelinePanelLegendPosition) {
        StartParameters.timelinePanelLegendPosition = timelinePanelLegendPosition;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double getTimelinepanelSplitposition() {
        return timelinepanelSplitposition;
    }

    public static void setTimelinepanelSplitposition(double timelinepanelSplitposition) {
        StartParameters.timelinepanelSplitposition = timelinepanelSplitposition;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setControlFrameBounds(int x, int y, int w, int h) {
        controlFrameX = x;
        controlFrameY = y;
        controlFrameW = w;
        controlFrameH = h;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setMapFrameBounds(int x, int y, int w, int h) {
        mapFrameX = x;
        mapFrameY = y;
        mapFrameW = w;
        mapFrameH = h;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setPlotFrameBounds(int x, int y, int w, int h) {
        timelinepanelX = x;
        timelinepanelY = y;
        timelinepanelWidth = w;
        timelinepanelHeight = h;
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Rectangle getMapFrameBounds() {
        return new Rectangle(mapFrameX, mapFrameY, mapFrameW, mapFrameH);
    }

    public static Rectangle getControlFrameBounds() {
        return new Rectangle(controlFrameX, controlFrameY, controlFrameW, controlFrameH);
    }

    public static Rectangle getPlotFrameBounds() {
        return new Rectangle(timelinepanelX, timelinepanelY, timelinepanelWidth, timelinepanelHeight);
    }

    public static boolean containsTimelineVisibilityInfo(String timelineName) {
        boolean found = timelineVisibility.containsKey(timelineName);
        return found;
    }

    public static boolean isTimelineVisible(String timelineName) {
        return timelineVisibility.get(timelineName);
    }

    /**
     * Initializes the tracking of the given name if it is not already in the
     * list.
     *
     * @param timelineName
     * @param visible
     * @return
     */
    public static boolean enableTimelineVisibilitySaving(String timelineName, boolean visible) {
        if (timelineVisibility.containsKey(timelineName)) {
            return false;
        }
        if (timelineName == null) {
            return false;
        }
        timelineVisibility.put(timelineName, visible);
        return true;
    }

    public static void setTimelineVisibility(String timelineName, boolean visible) {
        if (!timelineVisibility.containsKey(timelineName)) {
            return;
        }
        timelineVisibility.put(timelineName, visible);
        try {
            saveParameter();
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
