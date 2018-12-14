/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.web;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;
import java.io.IOException;

/**
 *
 * @author saemann
 */
public class FTP_Client {

    private String host = "";
    private String user = "";
    private String password = "";
    private boolean connected=false;

    private FTPClient client;

    public FTP_Client() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException, FTPListParseException {
        
            client = new FTPClient();
            //        client.setCharset("utf-8");
            client.connect(host);
           
            client.login(user, password);
            System.out.println(client.currentDirectory());
            String exchangeFile = null;
            for (String listName : client.listNames()) {
                System.out.println(" - " + listName);
                if (listName.toLowerCase().contains("ftp")) {
                    exchangeFile = listName;
                }
            }
            if (exchangeFile != null) {
                client.changeDirectory(exchangeFile);

            }
            connected=true;
       
    }

    public FTPClient getClient() {
        return client;
    }

    public boolean isConnected() {
        return connected;
    }

}
