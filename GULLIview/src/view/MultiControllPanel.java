package view;

import control.Controller;
import control.multievents.PipeResultData;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author saemann
 */
public class MultiControllPanel extends JPanel {

    private JPanel panelFiles, panelCenter;

    private final ArrayList<PipeResultData> inputs = new ArrayList<>();

    private final JCheckBox checkAutoLoad = new JCheckBox("Automatic loading", true);

    private Controller control;

    private boolean updating = false, askToUpdate = false;

    public MultiControllPanel(Controller control) {
        super(new BorderLayout());
        this.control = control;
        panelFiles = new JPanel();
        BoxLayout bl = new BoxLayout(panelFiles, BoxLayout.PAGE_AXIS);
        panelFiles.setLayout(bl);
        this.add(checkAutoLoad, BorderLayout.NORTH);
        panelCenter = new JPanel(new BorderLayout());
        panelCenter.setBorder(new TitledBorder("Result Files"));
        panelCenter.setBackground(Color.white);
        panelFiles.setBackground(Color.white);
        this.add(panelCenter, BorderLayout.CENTER);
        panelCenter.add(panelFiles, BorderLayout.NORTH);
        this.updatePanelFiles();
        this.initTransferhandler();
    }

    public void updatePanelFiles() {
        if (updating) {
            askToUpdate = true;
            return;
        }
        askToUpdate=false;
        updating = true;
        if (panelFiles.getComponentCount() > 0) {
            panelFiles.removeAll();
        }
        if (!control.getMultiInputData().isEmpty()) {
            PipeResultData se = control.getSingleEventInputData();
            boolean found = false;
            for (PipeResultData input : inputs) {
                if (input.getFile().equals(se.getFile())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                inputs.add(0, se);
            }
        }
        for (final PipeResultData input : inputs) {
            final JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new TitledBorder(input.getName()));
//            JLabel labelName = new JLabel(input.getName());
            panel.setToolTipText(input.getFile().getAbsolutePath());
//            panel.add(labelName, BorderLayout.NORTH);

            if (input.isLoading()) {
                panel.add(new JLabel("loading..."), BorderLayout.WEST);
            } else if (input.getManholeTimeline() == null || input.getPipeTimeline() == null || input.getPipeTimeline().getNumberOfTimes() < 1) {
                final JButton buttonLoad = new JButton("Load Data");
                buttonLoad.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        buttonLoad.setText("loading...");
                        buttonLoad.setEnabled(false);
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    input.loadData(control);
                                    updatePanelFiles();
                                } catch (Exception ex) {
                                    Logger.getLogger(MultiControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                                    panel.setBackground(Color.red.brighter());
                                    buttonLoad.setEnabled(true);
                                }
                            }
                        }.start();
                    }
                });
                panel.add(buttonLoad, BorderLayout.WEST);
            } else {
                panel.add(new JLabel("Loaded"), BorderLayout.WEST);
            }

            JButton buttonRemove = new JButton("Remove");
            buttonRemove.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    inputs.remove(input);
                    updatePanelFiles();
                }
            });
            panel.add(buttonRemove, BorderLayout.EAST);

            panelFiles.add(panel);
        }
        JButton buttonAdd = new JButton("Add file...");
        buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String folder = ".";

                JFileChooser fc = new JFileChooser(folder);
                fc.setAcceptAllFileFilterUsed(true);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        if (file.getName().endsWith(".idbf")) {
                            return true;
                        }
                        return false; //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public String getDescription() {
                        return "HE Database (.idbf)";
                    }
                });
                fc.setMultiSelectionEnabled(true);
                int n = fc.showOpenDialog(MultiControllPanel.this);
                if (n == fc.APPROVE_OPTION) {
                    for (File selectedFile : fc.getSelectedFiles()) {
                        final PipeResultData input = new PipeResultData(selectedFile, selectedFile.getName(), null, null);
                        for (PipeResultData input1 : inputs) {
                            if (input1.getFile().getAbsolutePath().equals(fc.getSelectedFile().getAbsolutePath())) {
                                return;
                            }
                        }
                        inputs.add(input);
                        if (checkAutoLoad.isSelected()) {
                            new Thread() {

                                @Override
                                public void run() {
                                    try {
                                        input.loadData(control);
                                        updatePanelFiles();
                                    } catch (Exception ex) {
                                        Logger.getLogger(MultiControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                            }.start();
                        }
                    }
                    updatePanelFiles();
                }
            }
        });

        panelFiles.add(buttonAdd);
//        panelFiles.add(new JPanel());
        panelFiles.revalidate();

        ArrayList<PipeResultData> activeList = new ArrayList<>(inputs.size());
        for (PipeResultData input : inputs) {
            if (input.getManholeTimeline() != null && input.getPipeTimeline() != null) {
                activeList.add(input);
            }
        }
        if (!activeList.isEmpty()) {
            control.getMultiInputData().clear();
            control.getMultiInputData().addAll(activeList);
            control.updatedInputData();
        }

        updating = false;
        if (askToUpdate) {
            updatePanelFiles();
        }
    }

    private void initTransferhandler() {
        TransferHandler th = new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }

                List<File> files;
                try {
                    files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                    // should never happen (or JDK is buggy)
                    ex.printStackTrace();
                    return false;
                }
                System.out.println("input: " + files.size() + " files.");
                for (File file : files) {
                    System.out.println(file.getAbsolutePath());
                    if (file.getName().endsWith("idbf")) {
                        boolean found = false;
                        for (PipeResultData input : inputs) {

                            if (input.getFile().equals(file)) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            System.out.println("Already contained " + file.getAbsolutePath());
                            continue;
                        }
                        final PipeResultData in = new PipeResultData(file, file.getName(), null, null);
                        inputs.add(in);
                        if (checkAutoLoad.isSelected()) {

                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        in.loadData(control);
                                        updatePanelFiles();
                                    } catch (Exception ex) {
                                        Logger.getLogger(MultiControllPanel.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }.start();
                        }

                    } else {
                        System.out.println("wrong suffix : " + file.getAbsolutePath());
                    }
                }
                updatePanelFiles();
                return true;
            }

        };

        panelFiles.setTransferHandler(th);
        panelCenter.setTransferHandler(th);
    }

}
