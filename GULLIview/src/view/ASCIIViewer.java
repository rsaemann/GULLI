package view;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

/**
 * Class to view the content of ascii files
 *
 * @author saemann
 */
public class ASCIIViewer {

    public static BufferedImage loadImage(File asciifile) throws IOException {
        BufferedImage bi = null;

        FileReader fr = new FileReader(asciifile);
        BufferedReader br = new BufferedReader(fr);
        br.mark(1);
        String line = br.readLine();
        int filetype = 0;
        if (line.startsWith("-999")) {
            filetype = 1;
            //Picture can be read directly from the first line
            br.reset();
        } else if (line.startsWith("ncols")) {
            filetype = 2;
            //Need to search for the last line that declares the data construct.
            while (!line.startsWith("NODATA")) {
                line = br.readLine();
                System.out.println("überspringe '" + line + "'");
            }
        } else if (line.startsWith("NCOLS")) {
            filetype = 3;
            //Need to search for the last line that declares the data construct.
            while (!line.startsWith("NODATA")) {
                line = br.readLine();
                System.out.println("überspringe '" + line + "'");
            }
        } else {

            System.err.println("unknown Filetype " + asciifile.getAbsolutePath());
            throw new IOException("Unknown Filetype in file " + asciifile.getAbsolutePath());
        }

        // Reader is set to the start of the Image data
        //Find bounds of the Image to set the Raster image to the correct scale
        int most_north = -1;
        int northX = -1;
        int most_south = -1;
        int southX = -1;
        int most_west = Integer.MAX_VALUE;
        int westY = -1;
        int most_east = Integer.MIN_VALUE;
        int eastY = -1;
        String[] values = null;
        double value = 0;
        int linecount = -1;
        boolean hasValue = false;
        long startSearching = System.currentTimeMillis();
        double lowestvalue = Double.POSITIVE_INFINITY, highestvalue = Double.NEGATIVE_INFINITY;
        while (br.ready()) {
            line = br.readLine();
            linecount++;
            if (filetype == 1) {
                values = line.split("(?<=\\G.{4})");
                if (!values[0].matches("\\s?-?\\d+")) {
                    System.out.println("no match for line '" + line + "'");
                    continue;
                }
            } else if (filetype == 2) {
                values = line.split("(?<=\\G.{6})");
            } else if (filetype == 3) {
                values = line.split("(?<=\\G.{5})");
            }
            for (int i = 0; i < values.length; i++) {
                if (filetype == 1) {
                    if (values[i].startsWith("-999")) {
                        hasValue = false;
                    } else {
                        hasValue = true;
                        lowestvalue = Math.min(lowestvalue, Double.parseDouble(values[i]));
                        highestvalue = Math.max(highestvalue, Double.parseDouble(values[i]));
                    }
                } else if (filetype == 2 || filetype == 3) {
                    if (values[i].startsWith(" -999")) {
                        hasValue = false;
                    } else {
                        hasValue = true;
                        lowestvalue = Math.min(lowestvalue, Double.parseDouble(values[i]));
                        highestvalue = Math.max(highestvalue, Double.parseDouble(values[i]));
                    }
                }
                if (most_north < 0) {
                    if (hasValue) {
                        most_north = linecount;
                        northX = i;
//                            System.out.println("North line: " + linecount + "  Position:" + linecount + "|" + northX);
                    }
                }
                if (i < most_west && hasValue) {
                    most_west = i;
                    westY = linecount;
//                        System.out.println("new best west value (" + i + "|" + linecount + ") : " + values[i]);
                }
                if (i > most_east && hasValue) {
                    most_east = i;
                    eastY = linecount;
                }
                if (hasValue) {
                    most_south = linecount;
                    southX = i;
                }
            }
        }

        double valueintervall = highestvalue - lowestvalue;
        br.close();
        fr.close();
        System.out.println("Filetype: " + filetype);
        System.out.println("North Position:" + northX + "|" + most_north);
        System.out.println("South Position:" + southX + "|" + most_south);
        System.out.println("West  Position:" + most_west + "|" + westY);
        System.out.println("East  Position:" + most_east + "|" + eastY);
        System.out.println("------------------------------------");
        System.out.println("Rows:" + (most_south - most_north) + "  -> " + 866. / (double) (most_south - most_north) + "km/cell");
        System.out.println("Cols:" + (most_east - most_west) + "  -> " + 640. / (double) (most_east - most_west) + "km/cell");
        System.out.println("... elapsed time: " + (System.currentTimeMillis() - startSearching) + "ms.");
        System.out.println("Start building image");

        bi = new BufferedImage((most_east - most_west) + 1, (most_south - most_north) + 1, BufferedImage.TYPE_INT_RGB);
        fr = new FileReader(asciifile);
        br = new BufferedReader(fr);
        if (filetype == 1) {

            linecount = -1;
            while (br.ready()) {
                line = br.readLine();
                linecount++;
                if (linecount > most_south) {
                    break;
                }
                values = line.split("(?<=\\G.{4})");
                for (int i = most_west; i <= most_east; i++) {
                    if (!values[i].startsWith("-999")) {
                        value = Double.parseDouble(values[i]);
                        double factor = (value - lowestvalue) / valueintervall;
                        try {
                            if (factor > 0.5) {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{255, (int) (255 - (factor - 0.5) / 0.5 * 255.), (int) (255 - (factor - 0.5) / 0.5 * 255.)});
                            } else {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{(int) (255 * factor / 0.5), (int) (255 * factor / 0.5), 255});
                            }
                        } catch (Exception e) {
                            System.err.println("Koordinate :" + (i - most_west) + " | " + (linecount - most_north));
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (filetype == 2) {
            linecount = -1;
            while (!line.startsWith("NODATA_value")) {
                line = br.readLine();
            }
            while (br.ready()) {
                line = br.readLine();
                linecount++;
                if (linecount > most_south) {
                    break;
                }
                values = line.split("(?<=\\G.{6})");
                for (int i = most_west; i <= most_east; i++) {
                    if (!values[i].startsWith(" -999")) {
                        value = Double.parseDouble(values[i]);
                        double factor = (value - lowestvalue) / valueintervall;
                        try {
                            if (factor > 0.5) {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{255, (int) (255 - (factor - 0.5) / 0.5 * 255.), (int) (255 - (factor - 0.5) / 0.5 * 255.)});
                            } else {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{(int) (255 * factor / 0.5), (int) (255 * factor / 0.5), 255});
                            }
                        } catch (Exception e) {
                            System.err.println("Koordinate :" + (i - most_west) + " | " + (linecount - most_north));
                            e.printStackTrace();
                        }
                    }
                }
            }

        } else if (filetype == 3) {
            linecount = -1;
            while (!line.startsWith("NODATA")) {
                line = br.readLine();
            }
            while (br.ready()) {
                line = br.readLine();
                linecount++;
                if (linecount > most_south) {
                    break;
                }
                values = line.split("(?<=\\G.{5})");
                for (int i = most_west; i <= most_east; i++) {
                    if (!values[i].startsWith(" -999")) {
                        value = Double.parseDouble(values[i]);
                        double factor = (value - lowestvalue) / valueintervall;
                        try {
                            if (factor > 0.5) {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{255, (int) (255 - (factor - 0.5) / 0.5 * 255.), (int) (255 - (factor - 0.5) / 0.5 * 255.)});
                            } else {
                                bi.getRaster().setPixel(i - most_west, linecount - most_north, new int[]{(int) (255 * factor / 0.5), (int) (255 * factor / 0.5), 255});
                            }
                        } catch (Exception e) {
                            System.err.println("Koordinate :" + (i - most_west) + " | " + (linecount - most_north));
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
//            while (br.ready()) {
//                line = br.readLine();
//                linecount++;
//                if (filetype == 1) {
//                    values = line.split("(?<=\\G.{4})");
//                    if (values[0].equals("    ")) {
//                        continue;
//                    }
//                } else if (filetype == 2) {
//                    values = line.split("(?<=\\G.{6})");
//                }
//                for (int i = 0; i < values.length; i++) {
//                    if (filetype == 1) {
//                        if (values[i].startsWith("-999")) {
//                            hasValue = false;
//                        } else {
//                            hasValue = true;
//                            lowestvalue = Math.min(lowestvalue, Double.parseDouble(values[i]));
//                            highestvalue = Math.max(highestvalue, Double.parseDouble(values[i]));
//                        }
//                    } else if (filetype == 2) {
//                        if (values[i].startsWith(" -999")) {
//                            hasValue = false;
//                        } else {
//                            hasValue = true;
//                            lowestvalue = Math.min(lowestvalue, Double.parseDouble(values[i]));
//                            highestvalue = Math.max(highestvalue, Double.parseDouble(values[i]));
//                        }
//                    }
//                }
//            }

        return bi;
    }

    public static void dropAndShowFrame() {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setBounds(new Rectangle(200, 100, 700, 1000));
        frame.setLayout(new BorderLayout());
        final JLabel label = new JLabel("Drag & Drop file here to show content.");
        label.setHorizontalAlignment(JLabel.CENTER);
        frame.add(label, BorderLayout.CENTER);
        label.setTransferHandler(new TransferHandler(null) {
            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                // we only import FileList
                if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport info) {
                if (!info.isDrop()) {
                    return false;
                }

                // Check for FileList flavor
                if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    displayDropLocation("List doesn't accept a drop of this type.");
                    return false;
                }

                // Get the fileList that is being dropped.
                Transferable t = info.getTransferable();
                List<File> data;
                try {
                    data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    BufferedImage bi = ASCIIViewer.loadImage(data.get(0));
                    ImageIcon ii = new ImageIcon(bi);
                    label.setIcon(ii);
                    label.setText("Drag & Drop file here to show content.");
                    frame.setTitle(data.get(0).getAbsolutePath());
                } catch (Exception e) {
                    label.setIcon(null);
                    label.setText(e.getLocalizedMessage());
                    return false;
                }

                return true;
            }

            private void displayDropLocation(String string) {
                System.out.println(string);
            }
        });
    }

    public static void main(String[] args) {

        ASCIIViewer.dropAndShowFrame();

    }

}
