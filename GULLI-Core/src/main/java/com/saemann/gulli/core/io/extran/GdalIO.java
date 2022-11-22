/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.core.io.extran;

import com.saemann.gulli.core.model.surface.Surface;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GDAL is an external tool to extract the GDB Database. The package needs to be
 * installed manually. It is required only for very large GDB files, that are
 * somehow decoded and cannot be accessed using the rGDB driver library.
 *
 * @author saemann
 */
public class GdalIO {

    public static boolean verbose = false;

    private static boolean gdalTested = false, gdalProperlyInstalled = false;
    private static boolean requiresStandardPath = false;

    /**
     *
     * @param verbose
     * @return
     * @throws IOException if 'ogr2ogr' is not installed.
     */
    public static boolean testGDALInstallation(boolean verbose) throws IOException {

//        Process child = Runtime.getRuntime().exec();
        Runtime rt = Runtime.getRuntime();
        String commands = new String("ogr2ogr --version");

        Process proc = null;
        try {
            proc = rt.exec(commands);
        } catch (IOException ex) {
            //System.err.println("Cannot find ogr2ogr in standard command interface. Is GDAL istalled and ");
            //Maybe the PATH variable is not set. Try to find command in standard install path:
            //C:\Program Files\GDAL
            try {
                proc = rt.exec("C:\\Program Files\\GDAL\\ogr2ogr --version");
                //If we reache this, GDAL is installed, but the PATH Variable is missing.
                System.out.println("GDAL is installed in C:\\Program Files\\GDAL, but the PATH environment is not directing here. This can cause problems when using the commands in the standard console. Please, add the directory-path to your PATH variable.");
                requiresStandardPath = true;
            } catch (IOException e) {
                System.err.println("GDAL is not installed.");

                throw e;
            }

        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

// read the output from the command
//        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            if (s.isEmpty()) {
                continue;
            }
            if (verbose) {
                System.out.println(s);
            }
            if (s.contains("GDAL") && s.contains("release")) {
                gdalTested = true;
                gdalProperlyInstalled = true;
                return true;
            }
        }

// read any errors from the attempted command
        System.err.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.err.println(s);
        }
        gdalTested = true;
        gdalProperlyInstalled = false;
        return false;
    }

    public static void main0(String[] args) {
        try {
            //Test installation
            verbose = true;
            System.out.println("GDAL installed, testing ogr2ogr: " + testGDALInstallation(true));
            String gdb = "E:\\EVUS\\Testmodell\\E2D1T5R.result\\Result2D.gdb";
            File fgdb = new File(gdb);
            Surface surf = HE_SurfaceIO.loadSurface(new File("E:\\EVUS\\Testmodell\\2DModell_3m2.model"));
            File output = GdalIO.exportVelocites(fgdb, surf, false);
            if (output != null) {
                System.out.println("Created export file " + output);
                if (output != null) {
                    if (output.exists()) {
                        System.out.println("   Filesize: " + output.length());
                    }
                }
            } else {
                System.err.println("No csv created");
            }
        } catch (IOException ex) {
            Logger.getLogger(GdalIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main1(String[] args) throws IOException {
        String gdb = "L:\\Modell2017MaiResultsBora\\2D_Model\\Ex2D_T20-D60-920_pE651_G4.result\\Result2D.gdb";
        String csv = "L:\\Modell2017MaiResultsBora\\2D_Model\\Ex2D_T20-D60-920_pE651_G4.result\\SurfaceWaterLevelKondens.csv";
//        String shp = "C:\\Users\\saemann\\Documents\\NetBeansProjects\\GULLI\\2DModell_Detail_1m2.model\\Result2D.shp";
//        GdalIO.convertGDB2CSV(gdb);
        System.exit(0);
        long start = System.currentTimeMillis();
        try {
            writeCSVSurfaceWaterlevel(gdb, csv, 5, true, null);
        } catch (IOException ex) {
            Logger.getLogger(GdalIO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(GdalIO.class.getName()).log(Level.SEVERE, null, ex);
        }

//        try {
//            testGDALInstallation(true);
//        } catch (IOException ex) {
//            Logger.getLogger(GdalIO.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("ended after " + (System.currentTimeMillis() - start) + "ms.");
    }

    public static String createSHPfilePath(String gdbFilePath) throws FileNotFoundException {
        if (gdbFilePath.endsWith(".gdb")) {
            String shpFilePath = gdbFilePath.substring(0, gdbFilePath.lastIndexOf("gdb")) + "shp";
            return shpFilePath;
        } else {
            throw new FileNotFoundException(gdbFilePath + " does not end with '.gdb'.");
        }
    }

    public static String createCSVfilePath(String gdbFilePath) throws FileNotFoundException {
        if (gdbFilePath.endsWith(".gdb")) {
            String shpFilePath = gdbFilePath.substring(0, gdbFilePath.lastIndexOf("gdb")) + "csv";
            return shpFilePath;
        } else {
            throw new FileNotFoundException(gdbFilePath + " does not end with '.gdb'.");
        }
    }

    public static String convertGDB2SHP(String gdbFilePath) throws FileNotFoundException, IOException {

        String shpFilePath = createSHPfilePath(gdbFilePath);

        if (verbose) {
            System.out.println("Convert from\n" + gdbFilePath + "  to\n" + shpFilePath);
        }
        boolean acc = convertGDB2SHP(gdbFilePath, shpFilePath, false);
        if (verbose) {
            System.out.println("Converting created new file? " + acc);
        }
        return shpFilePath;

    }

    /**
     * Check if the export has already been done in a former simulation. If this
     * returns a valid file, it is recommended to use it instead of creating a
     * new export.
     *
     * @param gdb
     * @return File containing the velocity values.
     */
    public static File getExportedVelocityFile(File gdb) {
        return getCSVFileVelocity(gdb);
    }

//    public static boolean convertGDBtoCSV(File gdb,Surface surface) throws IOException {
//        File velocityFile = getCSVFileVelocity(gdb);
//        return (writeCSVSurfaceVelocities(gdb.getAbsolutePath(), velocityFile.getAbsolutePath(), 5,true,surface));
//    }
//    public static String convertGDB2CSV(String gdbFilePath) throws FileNotFoundException, IOException {
//
//        String csvFilePath = createCSVfilePath(gdbFilePath);
//
//        if (verbose) {
//            System.out.println("Convert from\n" + gdbFilePath + "  to\n" + csvFilePath);
//        }
//        boolean acc = convertGDB2CSV(gdbFilePath, csvFilePath, false);
//        if (verbose) {
//            System.out.println("Converting created new file? " + acc);
//        }
//        return csvFilePath;
//
//    }
    public static File getCSVFileVelocity(File gdbFilePath) {
        if (gdbFilePath == null) {
            return null;
        }
        if (!gdbFilePath.exists()) {
            return null;
        }
        return new File(gdbFilePath.getParentFile(), "velocity.csv");
    }

    public static File exportVelocites(File gdbFile, Surface surf, boolean deleteTemp) throws IOException, FileNotFoundException {
        File csvFile = getCSVFileVelocity(gdbFile);
        if (writeCSVSurfaceVelocities(gdbFile.getAbsolutePath(), csvFile.getAbsolutePath(), 5, deleteTemp, surf)) {
            return csvFile;
        }
        return null;
    }

    public static boolean writeCSVSurfaceWaterlevel(String gdbFilePath, String csvFilePath, int decimals, boolean deletTempCSVfile, Surface surf) throws FileNotFoundException, IOException {

        File gdbFile = new File(gdbFilePath);
        if (!gdbFile.exists()) {
            throw new FileNotFoundException("Can not find file '" + gdbFilePath + "'.");
        }
        File tempFile = new File(gdbFile.getParent() + File.separator + "surfaceTempWaterlevel.csv");
        if (!tempFile.exists()) {
            convertGDB2CSV(gdbFilePath, tempFile.getAbsolutePath(), "Topo_decimated", true);
            //Velocity
            //Topo_decimated
        }
        long start = System.currentTimeMillis();
        File fileCSV = new File(csvFilePath);
        fileCSV.createNewFile();
        //Create Decimalformat
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
//        dfs.setDecimalSeparator('.');
//        dfs.setGroupingSeparator(',');

        DecimalFormat f2 = new DecimalFormat("0.##"); //Used for leading information about shape length & shape area
        f2.setDecimalFormatSymbols(dfs);
        f2.setDecimalSeparatorAlwaysShown(false);

        DecimalFormat f = new DecimalFormat("0.####");
        f.setDecimalFormatSymbols(dfs);
        f.setMaximumFractionDigits(decimals);
        f.setMinimumFractionDigits(0);
        f.setDecimalSeparatorAlwaysShown(false);

        try ( //Create copy reader/writer
                FileInputStream fis = new FileInputStream(tempFile);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("utf-8"));
                BufferedReader br = new BufferedReader(isr);
                FileOutputStream fos = new FileOutputStream(fileCSV);
                OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName("utf-8"));
                BufferedWriter bw = new BufferedWriter(osw)) {
//Write information about Rain Gauge
            // find result.idbf
//            File idbf = null;
//            File parentDir = gdbFile.getParentFile();
//            for (File file : parentDir.listFiles()) {
//                if (file.getName().endsWith(".idbf")) {
//                    if (idbf == null) {
//                        idbf = file;
//                        continue;
//                    }
//                    if (file.getName().startsWith("Ergebnis")) {
//                        continue;
//                    }
//                }else if(file.getName().endsWith(".idbr")){
//                     if (idbf == null) {
//                        idbf = file;
//                        continue;
//                    }
//                }
//            }
//            if (idbf != null) {
//                try{
//                HE_Database db = new HE_Database(idbf, true);
////                try (Connection extran = db.getConnection()) {
////                    Statement st = extran.createStatement();
////                    ResultSet rs;
//                    bw.write("Modelname: " + db.readModelnamePipeNetwork());
//                    bw.newLine();
//                    bw.write("Resultname: " + db.readResultname());
//                    bw.newLine();
//                    bw.write("Conversion Date: " + new Timestamp(System.currentTimeMillis()));
//                    bw.newLine();
//                    //Raingauge
//                    bw.write("***");
//                    bw.newLine();
//                    bw.write("Raingauge");
//                    bw.newLine();
//
//                    rs = st.executeQuery("SELECT REGENREIHE.REGENBEGINN,REGENREIHE.REGENENDE,REGENREIHE.INTERVALLBREITE,REGENREIHE.DATEN FROM REGENREIHE,REGENSCHREIBERZUORDNUNG WHERE REGENREIHE.ID=REGENSCHREIBERZUORDNUNG.REGENREIHEREF");//This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
//                    rs.next();
//                    Timestamp startR = rs.getTimestamp("REGENBEGINN");
//                    Timestamp ende = rs.getTimestamp("REGENENDE");
//                    int duration_min = (int) ((ende.getTime() - startR.getTime()) / 60000);
//                    int intervall_min = rs.getInt("INTERVALLBREITE");
//                    bw.write(" Rain start: " + startR.toString());
//                    bw.newLine();
//                    bw.write(" Rain ended: " + ende);
//                    bw.newLine();
//                    bw.write(" Rain duration: " + duration_min + " min");
//                    bw.newLine();
//                    bw.write(" Rain interval: " + intervall_min + " min");
//                    bw.newLine();
//
//                    byte[] blob = rs.getBytes("DATEN");
//                    double[] precipitation = Raingauge_Firebird.readPrecipitation(blob);
//                    bw.write(" Gauges in mm : " + precipitation.length);
//                    bw.newLine();
//                    for (int i = 0; i < precipitation.length; i++) {
//                        if (i > 0) {
//                            bw.write(";");
//                        }
//                        bw.write(precipitation[i] + "");
//                    }
//                    bw.newLine();
//                    bw.newLine();
//                    bw.flush();
//                    bw.write("***");
//                    bw.newLine();
//                    bw.write("Simulation results");
//                    bw.newLine();
//                } catch (SQLException exc) {
//                    System.err.println("Problems reading ResultDatabase ");
//                    exc.printStackTrace();
//                }
//            }

            //Copy Header
            String line = br.readLine();
            if (surf != null) {
                line = line.replaceAll("Z,", "X,Y,Z,");
            }

            bw.write(line.replace(',', ';'));
            bw.newLine();
            bw.flush();
            String[] s = line.split(",");
            int wls = 0;
            for (String item : s) {
                if (item.startsWith("WL_")) {
                    wls++;
                }
            }
            System.out.println("WL entries: " + wls);
            while (br.ready()) {
                s = br.readLine().split(",");
                //ID 
                bw.append(s[0]).append(';'); //ID
                if (surf != null) {
                    //X,Y
                    int id = Integer.parseInt(s[0]);
                    double[] coords = surf.getTriangleMids()[id];
                    bw.append(f2.format(coords[0])).append(';').append(f2.format(coords[1]) + "").append(';');
                }
                // Z
                bw.append(s[1]).append(';');
                //Length. Area, 
                bw.append(f2.format(Double.parseDouble(s[2]))).append(';');
                bw.append(f2.format(Double.parseDouble(s[3]))).append(';');
                //WLVLMax
                bw.append(f.format(Double.parseDouble(s[4]))).append(';');
                //Waterlevels
                for (int i = 0; i < wls - 1; i++) {
                    bw.append(f.format(Double.parseDouble(s[5 + i]))).append(';');
                }
                bw.append(f.format(Double.parseDouble(s[4 + wls])));
                bw.newLine();
                bw.flush();
            }
            bw.flush();

        }
        System.out.println((System.currentTimeMillis() - start) / 1000 + " s. für Kondensierung.");
        //Delete temp file
        if (deletTempCSVfile) {
            tempFile.delete();
        }
        return true;
    }

    public static boolean writeCSVSurfaceVelocities(String gdbFilePath, String csvFilePath, int decimals, boolean deletTempCSVfile, Surface surf) throws FileNotFoundException, IOException {

        File gdbFile = new File(gdbFilePath);
        if (!gdbFile.exists()) {
            throw new FileNotFoundException("Can not find file '" + gdbFilePath + "'.");
        }
        File tempFile = new File(gdbFile.getParent() + File.separator + "surfaceTempVelocities.csv");
        if (!tempFile.exists()) {
            long s = System.currentTimeMillis();

            convertGDB2CSV(gdbFilePath, tempFile.getAbsolutePath(), "Velocity", true);

            if (verbose) {
                System.out.println("Exporting gdb velocities to temporary file took " + (System.currentTimeMillis() - s) / 60000 + " min.");
            }
            //Velocity
            //Topo_decimated
        }
        long start = System.currentTimeMillis();
        File fileCSV = new File(csvFilePath);
        fileCSV.createNewFile();
        //Create Decimalformat
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
//        dfs.setDecimalSeparator('.');
//        dfs.setGroupingSeparator(',');

        DecimalFormat f2 = new DecimalFormat("0.##"); //Used for leading information about shape length & shape area
        f2.setDecimalFormatSymbols(dfs);
        f2.setDecimalSeparatorAlwaysShown(false);

        DecimalFormat f = new DecimalFormat("0.####");
        f.setDecimalFormatSymbols(dfs);
        f.setMaximumFractionDigits(decimals);
        f.setMinimumFractionDigits(0);
        f.setDecimalSeparatorAlwaysShown(false);

        System.out.println("Write compacted velocity file...");

        try ( //Create copy reader/writer
                FileInputStream fis = new FileInputStream(tempFile);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("utf-8"));
                BufferedReader br = new BufferedReader(isr);
                FileOutputStream fos = new FileOutputStream(fileCSV);
                OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName("utf-8"));
                BufferedWriter bw = new BufferedWriter(osw)) {

            //Copy Header
            String line = br.readLine();
            String[] s = line.split(",");
            int wls = 0;
            for (String item : s) {
                if (item.startsWith("V_X")) {
                    wls++;
                }
            }
            if (verbose) {
                System.out.println("Velocity entries: " + wls);
            }

            bw.write("ID;V_MAX");
            for (int i = 0; i < wls; i++) {
                bw.write(";V_X_" + i + ";V_Y_" + i);
            }
            bw.newLine();
            StringBuffer str = new StringBuffer(40);
            boolean written = false;
            while (br.ready()) {
                s = br.readLine().split(",");
                written = false;
                //ID 
                str.delete(0, str.length());
                str.append(s[0].replaceAll("\"", "")).append(';'); //ID
                //VMax
                str.append(f.format(Double.parseDouble(s[1])));
                //Velocities
                for (int i = 0; i < wls; i++) {
                    double vx = Double.parseDouble(s[5 + i * 4]);
                    double vy = Double.parseDouble(s[5 + i * 4 + 1]);
                    str.append(";" + f.format(vx));
                    str.append(";" + f.format(vy));
                    if (vx > 0.00001 || vy > 0.00001) {
                        bw.append(str.toString());
                        str.delete(0, str.length());
                        written = true;
                    }
                }
                if (written) {
                    bw.newLine();
                    bw.flush();
                }
            }
            bw.flush();

        }
        System.out.println((System.currentTimeMillis() - start) / 1000 + " s. für Kondensierung.");
        //Delete temp file
        if (deletTempCSVfile) {
            tempFile.delete();
        }
        return true;
    }

    public static boolean convertGDB2SHP(String gdbFilePath, String shpFilePath, boolean override) throws IOException {

        if (!override) {
            //Check if already exists
            File shpFile = new File(shpFilePath);
            if (shpFile.exists()) {
                return false;
            }
        }

        if (!gdalTested) {
            testGDALInstallation(false);
        }
        if (!gdalProperlyInstalled) {
            throw new UnsupportedOperationException("GDAL does not seem to be installed correctly. Can not Convert GDB files. Recheck with " + GdalIO.class + ".testGDALInstallation(true).");
        }

        Runtime rt = Runtime.getRuntime();
        String commands = "ogr2ogr -overwrite -f \"ESRI Shapefile\" \"" + shpFilePath + "\"  \"" + gdbFilePath + "\"";
        Process proc = rt.exec(commands);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

// read the output from the command
//        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            if (s.isEmpty()) {
                continue;
            }
            System.out.println(s);
        }

// read any errors from the attempted command
//        System.err.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            if (s.isEmpty()) {
                continue;
            }
            System.err.println(s);
        }
        return true;
    }

    public static boolean convertGDB2CSV(String gdbFilePath, String csvFilePath, String layer, boolean override) throws IOException {
        File csvFile = new File(csvFilePath);
        if (!override) {
            //Check if already exists

            if (csvFile.exists()) {
                return false;
            }
        }

        if (!gdalTested) {
            testGDALInstallation(false);
        }
        if (!gdalProperlyInstalled) {
            throw new UnsupportedOperationException("GDAL does not seem to be installed correctly. Can not Convert GDB files. Recheck with " + GdalIO.class + ".testGDALInstallation(true).");
        }

        Runtime rt = Runtime.getRuntime();
        String commands = "ogr2ogr -overwrite -f CSV \"" + csvFilePath + "\"  \"" + gdbFilePath + "\" \"" + layer + "\"";
        if (requiresStandardPath) {
            commands = "C:\\Program Files\\GDAL\\" + commands;
        }
        if (verbose) {
            commands += " -progress";
            System.out.println("Starting " + commands);
        }
        Process proc = rt.exec(commands);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

// read the output from the command
//        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        if (verbose) {
            System.out.println("Read progress on " + csvFile);
        }
        while ((s = stdInput.readLine()) != null) {
            if (s.isEmpty()) {

                continue;
            }
            System.out.println(s);
        }

// read any errors from the attempted command
//        System.err.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            if (s.isEmpty()) {
                continue;
            }
            System.err.println(s);
        }
        return true;
    }

}
