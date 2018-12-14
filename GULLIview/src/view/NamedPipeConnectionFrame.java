package view;

import control.PipeActionListener;
import io.NamedPipeIO;
import io.NamedPipeIO.PipeActionEvent;
import io.NamedPipe_IO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import model.topology.Manhole;
import model.topology.Position;

/**
 *
 * @author saemann
 */
public class NamedPipeConnectionFrame extends JFrame implements PipeActionListener {

    private JButton buttonConnect, buttonStopConnection;
    private JLabel labelStatusReader, labelStatusWriter;
    private JTextArea textIncoming, textOutgoingWait, textSend;
    private NamedPipe_IO io;

    private JButton buttonSendSzenario;

    public NamedPipeConnectionFrame() throws HeadlessException {
        super("NamedPipe Connection");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.setLayout(new BorderLayout());
        JPanel panelButtons = new JPanel(new BorderLayout());
        buttonConnect = new JButton("Connect");
        buttonStopConnection = new JButton("Stop");

        labelStatusReader = new JLabel("Initializing...");
        labelStatusWriter = new JLabel("Initializing...");
        panelButtons.add(buttonConnect, BorderLayout.WEST);
        panelButtons.add(buttonStopConnection, BorderLayout.EAST);

        JPanel panelStatus = new JPanel(new BorderLayout());
        panelStatus.setBorder(new TitledBorder("Status"));
        JPanel panelWriter = new JPanel(new BorderLayout());
        panelWriter.add(new JLabel("Sender: "), BorderLayout.WEST);
        panelWriter.add(labelStatusWriter, BorderLayout.CENTER);
        panelStatus.add(panelWriter, BorderLayout.NORTH);

        JPanel panelReader = new JPanel(new BorderLayout());
        panelReader.add(new JLabel("Reader: "), BorderLayout.WEST);
        panelReader.add(labelStatusReader, BorderLayout.CENTER);
        panelStatus.add(panelReader, BorderLayout.SOUTH);
        panelButtons.add(panelStatus, BorderLayout.SOUTH);

        buttonConnect.setEnabled(false);
        buttonStopConnection.setEnabled(false);
        labelStatusReader.setText("No IO object referenced.");
        labelStatusWriter.setText("No IO object referenced.");
        JPanel panelNorth = new JPanel(new BorderLayout());
        panelNorth.add(panelButtons, BorderLayout.LINE_START);
        this.add(panelNorth, BorderLayout.NORTH);

        textIncoming = new JTextArea();
        textOutgoingWait = new JTextArea();
        textSend = new JTextArea();

        JScrollPane scrollReader = new JScrollPane(textIncoming);
        JPanel panelText = new JPanel(new GridLayout(1, 2));
        panelText.add(scrollReader);
        scrollReader.setBorder(new TitledBorder("Read messages:"));

        JSplitPane paneloutgoing = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        paneloutgoing.setBorder(new TitledBorder("Send messages:"));
        JScrollPane scrollWaiting = new JScrollPane(textOutgoingWait);
        scrollWaiting.setBorder(new TitledBorder("Waiting:"));
        JScrollPane scrollSent = new JScrollPane(textSend);
        scrollSent.setBorder(new TitledBorder("Sent:"));

        paneloutgoing.add(new JScrollPane(scrollWaiting));
        paneloutgoing.add(new JScrollPane(scrollSent));
        panelText.add(paneloutgoing);

        this.add(panelText, BorderLayout.CENTER);

        buttonSendSzenario = new JButton("SendSzenario");
        this.add(buttonSendSzenario, BorderLayout.SOUTH);

        this.setVisible(true);
        this.setBounds(100, 100, 400, 400);

        initActions();

    }

    public void setIo(NamedPipe_IO io) {
        this.io = io;
        this.buttonConnect.setEnabled(io != null);
        this.buttonStopConnection.setEnabled(io != null);

        if (io == null) {
            labelStatusWriter.setText("IO object is null.");
            labelStatusReader.setText("IO object is null.");
        } else {
            labelStatusWriter.setText(io.getStatusStringWriter());
            labelStatusReader.setText(io.getStatusStringReader());
            io.addPipeActionListener(this);
        }
    }

//    public static void main_(String[] args) {
//        NamedPipeConnectionFrame npcf = new NamedPipeConnectionFrame();
//        npcf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        NamedPipe_IO io = new NamedPipe_IO();
//        npcf.setIo(io);
//
//        try {
//            Controller control = new Controller();
////            control.loadSingleEventFirebirdDatabase(new File("L:\\HE_Ergebnisse\\Modell_flutung.idbf"));
////            HE_Database.isUTMinput = true;
//            control.importNetwork(HE_Database.loadNetwork(new File("L:\\HE_Ergebnisse\\Modell_flutung.idbf")));
////            System.out.println("UTM:"+Network.crsUTM));
////            System.out.println("GLOBAL:"+Network.crsWGS84);
//            NamedPipeInterpreter interpreter = new NamedPipeInterpreter(control);
//            io.addPipeActionListener(interpreter);
//        } catch (Exception ex) {
//            Logger.getLogger(NamedPipeConnectionFrame.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    private void initActions() {

        buttonConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (io != null) {
                    io.connectToSurface();
                } else {
                    buttonConnect.setEnabled(false);
                }
            }
        });
        buttonStopConnection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (io != null) {
                    io.stopConnection();
                } else {
                    buttonStopConnection.setEnabled(false);
                }
            }
        });

        buttonSendSzenario.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (io == null) {
                    return;
                }
                io.addMessageToSend("RESET");
                io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 548861, 5799836), "Kreisel", null));
                io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547640, 5800000), "Tönnisberg", null));
                io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547986, 5799563), "Nenndorfer Platz", null));
                io.notifyAboutFloodedManhole(new Manhole(new Position(0, 0, 547657, 5799252), "Wallensteinstraße West", null));
            }
        });
    }

    @Override
    public void actionPerformed(PipeActionEvent ae) {
        if (ae.action == NamedPipeIO.ACTION.CONNECT_READER) {
            labelStatusReader.setText(io.getStatusStringReader());
        } else if (ae.action == NamedPipeIO.ACTION.CONNECT_SENDER) {
            labelStatusWriter.setText(io.getStatusStringWriter());
        } else if (ae.action == NamedPipeIO.ACTION.DISCONNECT_READER) {
            labelStatusReader.setText(io.getStatusStringReader());
        } else if (ae.action == NamedPipeIO.ACTION.DISCONNECT_SENDER) {
            labelStatusWriter.setText(io.getStatusStringWriter());
        } else if (ae.action == NamedPipeIO.ACTION.MESSAGE_TOSEND) {
            try {
                textOutgoingWait.getDocument().insertString(0, ae.message + "\n", null);
            } catch (BadLocationException ex) {
                Logger.getLogger(NamedPipeConnectionFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (ae.action == NamedPipeIO.ACTION.MESSAGE_SENT) {
            try {
                textSend.getDocument().insertString(0, ae.message + "\n", null);
                textOutgoingWait.getDocument().remove(0, textOutgoingWait.getDocument().getLength());
                textOutgoingWait.getDocument().insertString(0, io.getMessages(true), null);
            } catch (Exception ex) {
                Logger.getLogger(NamedPipeConnectionFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (ae.action == NamedPipeIO.ACTION.MESSAGE_RECEIVED) {
            try {
                textIncoming.getDocument().insertString(0, ae.message + "\n", null);
            } catch (BadLocationException ex) {
                Logger.getLogger(NamedPipeConnectionFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
