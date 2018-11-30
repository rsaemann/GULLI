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

import static control.StartParameters.fileStartParameter;
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
public class SecurityParameters {

    private static boolean isloaded = loadStartParameter();
    private static File file = initFilePointer();
    private static String sslCertificateTruststorePath, sslCertificateTruststorePassword;
    private static boolean sslCertificateSet;

    public static File initFilePointer() {
        file = new File(StartParameters.getProgramDirectory(), "GULLISecurity.ini");
        return file;
    }

    public static boolean loadStartParameter() {
        if (isloaded) {
            return true;
        }
        BufferedReader br = null;
        try {
            if (file == null) {
                file = initFilePointer();
            }
            if (!file.exists()) {
                initFile();
                System.out.println("init security parameters file");
            }
            br = new BufferedReader(new FileReader(file));

            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("sslTruststorePath=")) {
                    sslCertificateTruststorePath = line.substring(line.indexOf("=") + 1);
                } else if (line.startsWith("sslTruststorePassword=")) {
                    sslCertificateTruststorePassword = line.substring(line.indexOf("=") + 1);
                }

            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(StartParameters.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Initialize ssl certificates
        if (sslCertificateTruststorePath != null && sslCertificateTruststorePassword != null) {
            System.setProperty("javax.net.ssl.trustStore", sslCertificateTruststorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", sslCertificateTruststorePassword);
            sslCertificateSet = true;
        }
        System.setProperty("javax.net.debug", "all");
            

        return true;
    }

    private static void initFile() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("sslTruststorePath=");
            bw.newLine();
            bw.write("sslTruststorePassword=");
            bw.newLine();
            bw.flush();
        }

    }
}
