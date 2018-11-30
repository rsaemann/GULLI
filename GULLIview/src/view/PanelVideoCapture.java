/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.text.DecimalFormat;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import view.video.GIFVideoCreator;

/**
 * Options for a video to be captured.
 *
 * @author saemann
 */
public class PanelVideoCapture extends JPanel {

    public static boolean verbose = false;

    String filePath = ".\\video.gif";
    JLabel labelCapturedFrames = new JLabel("Captured Frames: 0");
    JLabel labelFPS = new JLabel("Frames / second:");
    JLabel labelLoopsPerFrame = new JLabel("Loops per Frame:");
    JFormattedTextField textFPS = new JFormattedTextField(DecimalFormat.getInstance());
    JFormattedTextField textLoopsPerFrame = new JFormattedTextField(DecimalFormat.getInstance());

    JPanel panelFPS = new JPanel(new BorderLayout());
    JPanel panelLoopsPerFrame = new JPanel(new BorderLayout());
    JPanel panelSave = new JPanel(new BorderLayout());

    JCheckBox checkCapturingEnabled = new JCheckBox("Capture Video", false);
    JButton buttonCaptureSingleFrame = new JButton("Capture Single Frame");
    JButton buttonFile = new JButton("File...");
    JButton buttonFinish = new JButton("Finish");

    GIFVideoCreator gvc;

    public PanelVideoCapture(GIFVideoCreator gvc) {
        super(new GridLayout(5, 1));
        this.gvc = gvc;
        JPanel panelEnabled = new JPanel(new BorderLayout());
        checkCapturingEnabled.setSelected(gvc.enabled);
        panelEnabled.add(checkCapturingEnabled, BorderLayout.WEST);
        panelEnabled.add(buttonFile, BorderLayout.CENTER);

        this.add(panelEnabled);

        this.add(buttonCaptureSingleFrame);
        //Panel Loops per Frame
        panelLoopsPerFrame.add(labelLoopsPerFrame, BorderLayout.WEST);
        panelLoopsPerFrame.add(textLoopsPerFrame, BorderLayout.CENTER);
        this.add(panelLoopsPerFrame);
        //Panel FPS
        panelFPS.add(labelFPS, BorderLayout.WEST);
        panelFPS.add(textFPS, BorderLayout.CENTER);
        this.add(panelFPS);

        //Save 
        panelSave.add(labelCapturedFrames, BorderLayout.CENTER);
        panelSave.add(buttonFinish, BorderLayout.EAST);
        this.add(panelSave);

        this.setPreferredSize(new Dimension(100, 120));
        this.setMinimumSize(new Dimension(80, 100));
        initActions(gvc);
    }

    public void updateGUI() {
        if (!gvc.enabled == checkCapturingEnabled.isSelected()) {
            checkCapturingEnabled.setSelected(gvc.enabled);
        }
        textFPS.setText((int) gvc.framesPerSecond + "");
        textLoopsPerFrame.setText(gvc.loopsPerFrame + "");
        labelCapturedFrames.setText("Captured Frames: " + gvc.getNumberFramesCaptured());

        textFPS.setEditable(gvc.getNumberFramesCaptured() == 0);
        textLoopsPerFrame.setEditable(gvc.getNumberFramesCaptured() == 0);

    }

    private void initActions(final GIFVideoCreator gvc) {
        textFPS.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    float fps = Float.parseFloat(textFPS.getText());
                    gvc.framesPerSecond = fps;
                } catch (Exception exception) {
                    textFPS.setText((int) gvc.framesPerSecond + "");
                }
                updateGUI();
            }
        });
        textFPS.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    float fps = Float.parseFloat(textFPS.getText());
                    gvc.framesPerSecond = fps;
                } catch (Exception exception) {
                    textFPS.setText((int) gvc.framesPerSecond + "");
                }
                updateGUI();
            }
        });
        textLoopsPerFrame.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    int lps = Integer.parseInt(textLoopsPerFrame.getText());
                    gvc.loopsPerFrame = lps;
                } catch (Exception exception) {
                    textLoopsPerFrame.setText((int) gvc.loopsPerFrame + "");
                }
                updateGUI();
            }
        });
        textLoopsPerFrame.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                try {
                    int lps = Integer.parseInt(textLoopsPerFrame.getText());
                    gvc.loopsPerFrame = lps;
                } catch (Exception exception) {
                    textLoopsPerFrame.setText((int) gvc.loopsPerFrame + "");
                }
                updateGUI();
            }
        });

        buttonCaptureSingleFrame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!gvc.isReady()) {
                    findSaveFile();
                }
                gvc.captureOneFrame();
                updateGUI();
            }
        });

        buttonFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                findSaveFile();
                updateGUI();
            }
        });

        buttonFinish.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                gvc.finish();
                updateGUI();
            }
        });

    }

    private boolean findSaveFile() {
        JFileChooser fc = new JFileChooser(filePath);
        int n = fc.showSaveDialog(PanelVideoCapture.this);
        if (n == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists() && file.length() > 100) {
                int m = JOptionPane.showConfirmDialog(PanelVideoCapture.this, "Override File " + file.getName() + " (" + (file.length() / 1048576) + " MB)", "File already exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (m != JOptionPane.OK_OPTION) {
                    return false;
                }
            }
            if (gvc.saveAs(file, true)) {
                if (verbose) {
                    System.out.println("Video saved to " + file.getAbsolutePath());
                }
                filePath = file.getAbsolutePath();
                return true;
            }
        }
        return false;
    }
}
