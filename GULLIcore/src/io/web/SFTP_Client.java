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
package io.web;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saemann
 */
public class SFTP_Client {

    private Session session;

    private ChannelSftp channel;

    public static SFTP_Client FromFile(String filepath) throws IOException, JSchException {
        String[] sp = readSecurityLogin(filepath);
        SFTP_Client sftp = new SFTP_Client(sp[2], sp[0], Integer.parseInt(sp[1]), sp[3]);
        return sftp;
    }

    public SFTP_Client(final String user, final String host, final int port, final String password) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        UserInfo userInfo = new UserInfo() {

            @Override
            public String getPassphrase() {
//                System.out.println("getPassphrase()");
                return null;
            }

            @Override
            public String getPassword() {
//                System.out.println("getPassword()");
                return password;
            }

            @Override
            public boolean promptPassword(String string) {
//                System.out.println("promtPassword: " + string);
                return true;
            }

            @Override
            public boolean promptPassphrase(String string) {
//                System.out.println("PromptPassphrase: " + string);
                return true;
            }

            @Override
            public boolean promptYesNo(String string) {
//                System.out.println("PromptYesNo: " + string);
                return true;
            }

            @Override
            public void showMessage(String string) {
//                System.out.println("ShowMessage: " + string);
            }
        };

        session.setUserInfo(userInfo);
//        session.connect(10000);
//        ChannelSftp c = (ChannelSftp) session.openChannel("sftp");
//        c.connect(10000);
//        System.out.println("channel: " + c + "; " + c.getClass());
//        try {
//            System.out.println("pwd:" + c.pwd());
//        } catch (SftpException ex) {
//            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println("-----");
//        try {
//            Vector v = c.ls("isu2");
//            for (Object v1 : v) {
//                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) v1;
//                System.out.println(entry.getFilename() + " " + entry.getAttrs().getAtimeString() + "  " + entry.getAttrs().getMtimeString());
//            }
//        } catch (SftpException ex) {
//            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        session.disconnect();

    }

    private ChannelSftp getChannel() throws JSchException {
        if (channel == null || channel.isClosed()) {
            if (!session.isConnected()) {
                session.connect(10000);
            }
            channel = (ChannelSftp) session.openChannel("sftp");
        }
        if (!channel.isConnected()) {
            if (!session.isConnected()) {

                session.connect(10000);
            }
            channel.connect(10000);
        }
        return channel;
    }

    public String[] listFileNamesRoot() throws JSchException, SftpException {
        return listFileNames("\\.");
    }

    public String[] listFileNames(String remoteDirectory) throws JSchException, SftpException {

        ChannelSftp c = getChannel();
//        System.out.println("c=" + c + "   connected:" + c.isConnected() + " closed:" + c.isClosed());

        Vector v = c.ls(remoteDirectory);
        String[] str = new String[v.size()];
        for (int i = 0; i < str.length; i++) {
            str[i] = ((ChannelSftp.LsEntry) v.get(i)).getFilename();
        }
        return str;
    }

    public ChannelSftp.LsEntry[] listFileInformation(String remoteDirectory) throws JSchException, SftpException {
        Vector v = getChannel().ls(remoteDirectory);
        ChannelSftp.LsEntry[] str = new ChannelSftp.LsEntry[v.size()];
        for (int i = 0; i < str.length; i++) {
            str[i] = ((ChannelSftp.LsEntry) v.get(i));
        }
        return str;
    }

    public boolean download(String remoteDiretory, String remoteFileName, String localFilePath) throws IOException, JSchException, SftpException {
        //Create file at destination 
        File localFile = new File(localFilePath);
        if (!localFile.exists()) {
            localFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(localFile);
        getChannel().cd(remoteDiretory);
        getChannel().get(remoteFileName, fos);
        fos.flush();
        fos.close();
        return true;
    }

    public void close() {
        if (channel != null && channel.isConnected() && !channel.isClosed()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private static String[] readSecurityLogin(String pathToFile) throws IOException {
        File f = new File(pathToFile);
        if (!f.exists()) {
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            f.createNewFile();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                bw.write("SFTP_Host:");
                bw.newLine();
                bw.write("SFTP_Port:");
                bw.newLine();
                bw.write("SFTP_User:");
                bw.newLine();
                bw.write("SFTP_Password:");
                bw.newLine();
                bw.flush();
            }
            System.out.println("Created File for SFTP Passphrase at " + f.getAbsolutePath());
            return null;
        }
        String[] retur = new String[4];
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("SFTP_Host")) {
                    retur[0] = line.substring(10);
                } else if (line.startsWith("SFTP_Port")) {
                    retur[1] = line.substring(10);
                } else if (line.startsWith("SFTP_User")) {
                    retur[2] = line.substring(10);
                } else if (line.startsWith("SFTP_Password")) {
                    retur[3] = line.substring(14);
                }
            }
        }
        if (retur[0] == null || retur[0].isEmpty()) {
            System.err.println("No entry for SFTP_Host");
        }
        if (retur[1] == null || retur[1].isEmpty()) {
            System.err.println("No entry for SFTP_Port");
        }
        if (retur[2] == null || retur[2].isEmpty()) {
            System.err.println("No entry for SFTP_User");
        }
        if (retur[3] == null || retur[3].isEmpty()) {
            System.err.println("No entry for SFTP_Password");
        }
        return retur;
    }

    public File downloadNewestPrecipitationForecast(File localDir) throws JSchException, IOException, SftpException {
        try {
            getChannel().cd("..");
            getChannel().cd("..");
            getChannel().cd("..");
        } catch (SftpException ex) {
            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
        }

        getChannel().cd("wawi");
        Vector v = getChannel().ls(".");
        long newest = 0;
        ChannelSftp.LsEntry newestName = null;
        for (Object v1 : v) {
            ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) v1;
            if(e.getFilename().length()<6)continue;
            int modificationTime = e.getAttrs().getMTime();
            if (modificationTime > newest) {
                newest = modificationTime;
                newestName = e;
            }
        }
        if(newestName==null)return null;
        File localfile = new File(localDir, newestName.getFilename());
        FileOutputStream fos = new FileOutputStream(localfile);
        getChannel().get(newestName.getFilename(), fos);
        fos.flush();
        fos.close();
        return localfile;
    }
    
    public boolean upload(File localFile, String remoteDirectory) throws JSchException, SftpException, FileNotFoundException, IOException{
        getChannel().cd("/"+remoteDirectory);
        
        FileInputStream fis=new FileInputStream(localFile);
        getChannel().put(fis, localFile.getName());
        fis.close();
        return true;
    }

    public static void main_(String[] args) {

        try {

            SFTP_Client client = SFTP_Client.FromFile(".\\SFTP.ini");
            String subfolder="xxx";
            String[] names = client.listFileNames(subfolder);

            for (String name : names) {
                if (name.length() > 3) {
                    System.out.println("Download " + name);
                    client.download(subfolder, name, "C:\\" + name);
                    break;
                }
            }

            client.close();

        } catch (IOException ex) {
            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSchException ex) {
            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SftpException ex) {
            Logger.getLogger(SFTP_Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
