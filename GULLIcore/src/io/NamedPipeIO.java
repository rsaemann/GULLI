package io;

import control.Controller;
import control.PipeActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inter Process communication between different software processes on one
 * computer via RAM. Implemented to talk to the Unity 3D visualization - not in
 * development any more.
 *
 * @author saemann
 */
public class NamedPipeIO {

    public static final String pipepath = "\\\\.\\pipe\\";
    protected String pipeNameReading = "surface_output";
    protected String pipeNameSending = "surface_input";

    protected boolean readerStop = false;
    protected boolean writerStop = false;

    public boolean verbose = false;

    protected String statusReader = "Status Reader: Disconnected", statusWriter = "Status Writer: Disconnected";
    protected final String str_Read = "Reading", str_Connected = "Connected", str_Write = "Writing", str_Try = "Try Connect";

    protected LinkedList<String> messagesToSend = new LinkedList<>();

    protected boolean readingConnected, sendingConnected;

    protected final Controller control;

    protected final ArrayList<PipeActionListener> readerListener = new ArrayList<>(2);
    protected final ArrayList<PipeActionListener> senderListener = new ArrayList<>(2);

    public enum ACTION {

        MESSAGE_RECEIVED, MESSAGE_TOSEND, MESSAGE_SENT, CONNECT_READER, CONNECT_SENDER, DISCONNECT_READER, DISCONNECT_SENDER, WAITCONNECTION_READER, WAITCONNECTION_SENDER
    }

    public NamedPipeIO() {
        this(null);
    }

    public NamedPipeIO(Controller control) {
        this.control = control;
    }

    public String getStatusStringReader() {
        return statusReader;
    }

    public String getStatusStringWriter() {
        return statusWriter;
    }

    public void addPipeActionListener(PipeActionListener l) {
        this.readerListener.add(l);
        this.senderListener.add(l);
    }

    public void connectToSurface() {
        new Thread() {

            @Override
            public void run() {
                boolean sendingBroken = false, readingBroken = false;
                writerStop = false;
                readerStop = false;
                try {
                    while (true) {
                        try {
                            if (!writerStop && !sendingConnected) {
                                try {
                                    statusWriter = "Try connect to pipe '" + pipeNameSending + "'";

                                    connectSendingPipe();

                                    sendingBroken = false;
                                } catch (FileNotFoundException ex) {

                                    if (!sendingBroken) {
                                        statusWriter = "Can not connect: " + ex.getLocalizedMessage();
                                        Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                                        notifyReaderListener(new PipeActionEvent(this, ACTION.DISCONNECT_SENDER, pipeNameSending));
                                    }
                                    sendingBroken = true;
                                }
                            }
                            if (!readerStop && !readingConnected) {
                                try {
                                    statusReader = "Try connect to pipe '" + pipeNameReading + "'";
                                    connectReadingPipe();
                                    readingBroken = false;
                                } catch (FileNotFoundException ex) {
                                    if (!readingBroken) {
                                        statusReader = "Can not connect: " + ex.getLocalizedMessage();
                                        Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                                        notifyReaderListener(new PipeActionEvent(this, ACTION.DISCONNECT_READER, pipeNameReading));
                                    }
                                    readingBroken = true;
                                }
                            }
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } catch (Exception e) {
                } finally {
                    notifyReaderListener(new PipeActionEvent(this, ACTION.DISCONNECT_READER, pipeNameReading));
                }

            }
        }.start();

    }

    public void connectReadingPipe() throws FileNotFoundException {

        final RandomAccessFile pipe = new RandomAccessFile(pipepath + pipeNameReading, "r");
        readingConnected = true;
        new Thread() {

            @Override
            public void run() {

                try {

                    try (BufferedInputStream bis = new BufferedInputStream(Channels.newInputStream(pipe.getChannel())); BufferedReader br = new BufferedReader(new InputStreamReader(bis))) {
                        if (verbose) {
                            System.out.println("Reading Pipe connected.");
                        }
                        statusReader = "Connected";
                        notifyReaderListener(new PipeActionEvent(this, ACTION.CONNECT_READER, pipeNameReading));
                        String line;

                        while (!readerStop) {
                            try {
                                while (br.ready()) {
                                    statusReader = str_Read;
                                    line = br.readLine();
                                    notifyReaderListener(new PipeActionEvent(this, ACTION.MESSAGE_RECEIVED, line));
                                    if (verbose) {
                                        System.out.println("Message from Surface: " + line);
                                    }
                                    if (line.equals("CLOSEPIPE")) {
                                        readerStop = true;
                                        break;
                                    }

                                    if (line.equals("CLOSE SERVER")) {
                                        statusReader = str_Try;
                                        notifyReaderListener(new PipeActionEvent(this, ACTION.DISCONNECT_READER, pipeNameReading));
                                        readingConnected = false;
                                        return;
                                    }

                                    statusReader = str_Connected;
                                }
                                Thread.sleep(100);
                            } catch (IOException ex) {
                                Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                                break;
                            } catch (InterruptedException ex) {
                                Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        statusReader = "Disconnected (regular)";
                    }

                } catch (IOException ex) {
                    Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                    statusReader = "Disconnected (Exception) " + ex.getLocalizedMessage();
                } finally {
                    readingConnected = false;
                    notifyReaderListener(new PipeActionEvent(this, ACTION.DISCONNECT_READER, pipeNameReading));
                }
            }
        }.start();
    }

    private void notifyReaderListener(PipeActionEvent ae) {
        for (PipeActionListener l : readerListener) {
            try {
                l.actionPerformed(ae);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notifySenderListener(PipeActionEvent ae) {
        for (PipeActionListener l : senderListener) {
            try {
                l.actionPerformed(ae);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void connectSendingPipe() throws FileNotFoundException {

        final RandomAccessFile pipe = new RandomAccessFile(pipepath + pipeNameSending, "rw");
        sendingConnected = true;
        new Thread() {
            @Override
            public void run() {
                BufferedOutputStream bos = new BufferedOutputStream(Channels.newOutputStream(pipe.getChannel()));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(bos));
                if (verbose) {
                    System.out.println("Sending Pipe connected.");
                }
                statusWriter = str_Connected;
                notifySenderListener(new PipeActionEvent(this, ACTION.CONNECT_SENDER, pipeNameSending));
                int loop = 0;
                while (true && !writerStop) {
                    try {
                        if (!messagesToSend.isEmpty()) {
                            statusWriter = str_Write;
                            while (!messagesToSend.isEmpty()) {
                                String line = messagesToSend.pollFirst();
                                bw.write(line);
                                bw.newLine();
                                notifySenderListener(new PipeActionEvent(this, ACTION.MESSAGE_SENT, line));
                            }
                            bw.flush();
                            statusWriter = str_Connected;
                        }
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        Logger.getLogger(NamedPipeIO.class.getName()).log(Level.SEVERE, null, ex);
                        statusWriter = "Disconnected (Exception) " + ex.getLocalizedMessage();
                        break;
                    }
                }
                if (verbose) {
                    System.out.println("Stop Sending Pipe");
                }
                statusWriter = "Disconnected (regular)";
                sendingConnected = false;
                notifySenderListener(new PipeActionEvent(this, ACTION.DISCONNECT_SENDER, pipeNameSending));
            }

        }.start();

    }

    public void addMessageToSend(String message) {
        messagesToSend.add(message);
        notifySenderListener(new PipeActionEvent(this, ACTION.MESSAGE_TOSEND, message));
    }

    public String getMessages(boolean newestFirst) {
        StringBuilder str = new StringBuilder(messagesToSend.size() * 30);
        Object[] a = messagesToSend.toArray();
        if (newestFirst) {
            for (int i = a.length - 1; i >= 0; i--) {
                if (a[i] != null) {
                    str.append(a[i].toString()).append('\n');
                }
            }
        } else {
            for (int i = 0; i < a.length; i++) {
                if (a[i] != null) {
                    str.append(a[i]).append('\n');
                }
            }
        }
        return str.toString();
    }

    public void stopConnection() {
        readerStop = true;
        writerStop = true;
    }

    public class PipeActionEvent {

        public final Object source;
        public final String message;
        public final ACTION action;

        public PipeActionEvent(Object source, ACTION act, String string) {
            this.source = source;
            this.message = string;
            this.action = act;
        }

    }
}
