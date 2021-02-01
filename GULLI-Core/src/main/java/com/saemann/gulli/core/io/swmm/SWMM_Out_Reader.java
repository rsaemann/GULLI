/*
 * The MIT License
 *
 * Copyright 2020 saemann.
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
package com.saemann.gulli.core.io.swmm;

import com.saemann.gulli.core.io.SparseTimeLineDataProvider;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLineManholeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimeLinePipeContainer;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelineManhole;
import com.saemann.gulli.core.model.timeline.sparse.SparseTimelinePipe;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.StorageVolume;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read *.out files of SWMM 5 simulations Read Timelines of node (manhole) and
 * link (pipe) objects
 *
 * @author Sämann
 */
public class SWMM_Out_Reader implements SparseTimeLineDataProvider {

    public static boolean verbose = false;

    private File file;
    int offset;
    int startPointer;
    int nbPeriods;
    int codeError;
    int endchecksum;
    int nbsubcatchments = -1;
    int nbNodes = -1;
    int nbLinks = -1;
    int nbPollutants;
    int periodBytes = 0;
    private int sizeSubcatchments;
    private int sizeNodes;
    private int sizeLinks;
    private int sizeSystemVaraibles;
    private String[] subcatchmentNames;
    private String[] nodeNames;
    private String[] linkNames;
    private String[] pollutantNames;
    private int reportstep;
    private double startDate;
    private long[] times;

    public SWMM_Out_Reader(File file) throws IOException {
        this.file = file;
        readFileParameters();
        readHeader();
    }

    private void readFileParameters() throws IOException {
        FileInputStream is = new FileInputStream(file);
        byte[] buffer;
        ByteBuffer bb;
        buffer = new byte[4];
        //pointer für wichtige Sprungmarken sind ganz am ende codiert
        long toskip = file.length() - 5 * 4;

//        System.out.println("File: " + file.length() + "   pos: " + raf.getFilePointer());
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        offset = bb.getInt();
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        startPointer = bb.getInt();
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        nbPeriods = bb.getInt();
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        codeError = bb.getInt();
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        endchecksum = bb.getInt();

        //Time information
        is.close();
        is = new FileInputStream(file);
        toskip = startPointer - 3 * 4;
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        byte[] doubleBuffer = new byte[8];
        is.read(doubleBuffer);
        bb = ByteBuffer.wrap(doubleBuffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        startDate = bb.getDouble();
        System.out.println(System.currentTimeMillis());
        if (verbose) {
            System.out.println("Time: long:" + startDate + " *86400 = " + (startDate * 86400) + "  " + new Date((long) (startDate * 86400) * 1000));
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        reportstep = bb.getInt();
        times = new long[nbPeriods];
        for (int i = 0; i < times.length; i++) {
            times[i] = i * reportstep * 1000;

        }
        if (verbose) {
            System.out.println("Report step: " + reportstep + " s.");

            System.out.println("Offset :" + offset);
            System.out.println("Start  :" + startPointer);
            System.out.println("Periods:" + nbPeriods);
            System.out.println("Error  :" + codeError);
            System.out.println("Checks :" + endchecksum);
        }
        is.close();
    }

    private void readHeader() throws FileNotFoundException, IOException {
//        FileReader fr=new FileReader(file);
        FileInputStream is = new FileInputStream(file);
//        RandomAccessFile raf = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[4]; //Standard size for integer values
        ByteBuffer bb;
        is = new FileInputStream(file);
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        if (verbose) {
            System.out.println("1:" + bb.getInt());
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        if (verbose) {
            System.out.println("Version:\t" + bb.getInt());
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        if (verbose) {
            System.out.println("FlowUnits:\t" + bb.getInt());
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        nbsubcatchments = bb.getInt();
        if (verbose) {
            System.out.println("Subcatchm:\t" + nbsubcatchments);
        }

        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        nbNodes = bb.getInt();
        if (verbose) {
            System.out.println("Nodes:  \t" + nbNodes);
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        nbLinks = bb.getInt();
        if (verbose) {
            System.out.println("Links:  \t" + nbLinks);
        }
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        nbPollutants = bb.getInt();
        if (verbose) {
            System.out.println("Pollutants:\t" + nbPollutants);
        }

        //Subcatchments:
        if (verbose) {
            System.out.println("Subcatchments: " + nbsubcatchments);
        }
        subcatchmentNames = new String[nbsubcatchments];
        for (int i = 0; i < nbsubcatchments; i++) {
            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int size = bb.getInt();
            byte[] strbuffer = new byte[size];
            is.read(strbuffer);
            ByteBuffer strb = ByteBuffer.wrap(strbuffer);
            strb.order(ByteOrder.LITTLE_ENDIAN);

            subcatchmentNames[i] = new String(strb.array());
            if (verbose) {
                System.out.println(" " + i + ":" + subcatchmentNames[i]);
            }
        }
        //NodeNames
        if (verbose) {
            System.out.println("Nodes: " + nbNodes);
        }
        nodeNames = new String[nbNodes];
        for (int i = 0; i < nbNodes; i++) {
            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int size = bb.getInt();
            byte[] strbuffer = new byte[size];
            is.read(strbuffer);
            ByteBuffer strb = ByteBuffer.wrap(strbuffer);
            strb.order(ByteOrder.LITTLE_ENDIAN);

            nodeNames[i] = new String(strb.array());
            if (verbose) {
                System.out.println(" " + i + ":" + nodeNames[i]);
            }
        }
        //LinkNames
        if (verbose) {
            System.out.println("Links: " + nbLinks);
        }
        linkNames = new String[nbLinks];
        for (int i = 0; i < nbLinks; i++) {
            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int size = bb.getInt();
            byte[] strbuffer = new byte[size];
            is.read(strbuffer);
            ByteBuffer strb = ByteBuffer.wrap(strbuffer);
            strb.order(ByteOrder.LITTLE_ENDIAN);

            linkNames[i] = new String(strb.array());
            if (verbose) {
                System.out.println(" " + i + ":" + linkNames[i]);
            }
        }
        //Pollutantnames
        if (verbose) {
            System.out.println("Pollutants: " + nbPollutants);
        }
        pollutantNames = new String[nbPollutants];
        for (int i = 0; i < nbPollutants; i++) {
            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int size = bb.getInt();
            byte[] strbuffer = new byte[size];
            is.read(strbuffer);
            ByteBuffer strb = ByteBuffer.wrap(strbuffer);
            strb.order(ByteOrder.LITTLE_ENDIAN);

            pollutantNames[i] = new String(strb.array());
            if (verbose) {
                System.out.println(" " + i + ":" + pollutantNames[i]);
            }
        }

        //Skip input values
        long toskip = (nbsubcatchments + 2) * 4 + (3 * nbNodes + 4) * 4 + (5 * nbLinks + 6) * 4 + 4 * (nbPollutants);
        if (verbose) {
            System.out.println("Skip " + toskip + " bytes.");
        }
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        //Number of subcatchment values
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sizeSubcatchments = bb.getInt();
        if (verbose) {
            System.out.println("Subcatchment values: " + sizeSubcatchments);
        }
        toskip = sizeSubcatchments * 4;
        if (verbose) {
            System.out.println(" Skip " + toskip + " bytes over subcatchments");
        }
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        //Number of node values
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sizeNodes = bb.getInt();
        if (verbose) {
            System.out.println("Node values: " + sizeNodes);
        }
        toskip = sizeNodes * 4;
        if (verbose) {
            System.out.println(" Skip " + toskip + " bytes over nodes");
        }
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        //Number of link values
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sizeLinks = bb.getInt();
        if (verbose) {
            System.out.println("Link values: " + sizeLinks);
        }
        toskip = sizeLinks * 4;
        if (verbose) {
            System.out.println(" Skip " + toskip + " bytes over links");
        }
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        //Number of System varaibles
        is.read(buffer);
        bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sizeSystemVaraibles = bb.getInt();
        if (verbose) {
            System.out.println("SysVars values: " + sizeSystemVaraibles);
        }
        toskip = sizeSystemVaraibles * 4;
        if (verbose) {
            System.out.println(" Skip " + toskip + " bytes over system variables");
        }
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }

        //Time information
        //bytes per period
        periodBytes = 2 * 4 + 4 * (nbsubcatchments * sizeSubcatchments + nbNodes * sizeNodes + nbLinks * sizeLinks + sizeSystemVaraibles);
        if (verbose) {
            System.out.println("PeriodBytes: " + periodBytes);
        }
        is.close();

    }

    /**
     *
     * @param nodeName Name of the node (manhole/outfall/divider/storage...)
     * @param valueIndex 0:Depth, 1:Head, 2:Volume, 3: Lateral Inflow, 4:Total
     * Inflow, 5:Flooding
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public float[] getNodeValues(String nodeName, int valueIndex) throws FileNotFoundException, IOException {
        int index = -1;
        for (int i = 0; i < nodeNames.length; i++) {
            if (nodeNames[i].equals(nodeName)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new NullPointerException("There is no node with name '" + nodeName + "'");
        }
        return getNodeValues(index, valueIndex);
    }

    /**
     *
     * @param nodeIndex
     * @param valueIndex 0:Depth, 1:Head, 2:Volume, 3: Lateral Inflow, 4:Total
     * Inflow, 5:Flooding
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public float[] getNodeValues(int nodeIndex, int valueIndex) throws FileNotFoundException, IOException {
        float[] values = new float[nbPeriods];
        byte[] buffer = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        FileInputStream is = new FileInputStream(file);
        long toskip = startPointer + (0 * periodBytes + 2 * 4) + (4 * nbsubcatchments * sizeSubcatchments) + (4 * nodeIndex * sizeNodes) + (4 * valueIndex);
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        for (int i = 0; i < values.length; i++) {

            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            values[i] = bb.getFloat();
            toskip = periodBytes - 4;
            while (toskip > 0) {
                toskip = toskip - is.skip(toskip);
            }
        }
        return values;
    }

    /**
     *
     * @param name Name of the link (pipe/conduit/pump/outlet...)
     * @param valueIndex flow parameter 0:Flow, 1:Depth, 2:Velocity, 3: Volume,
     * 4:capacity
     * @return reported timeseries of the requested flow parameter
     * @throws FileNotFoundException
     * @throws IOException
     */
    public float[] getLinkValues(String name, int valueIndex) throws FileNotFoundException, IOException {
        int index = -1;
        for (int i = 0; i < linkNames.length; i++) {
            if (linkNames[i].equals(name)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new NullPointerException("There is no link with name '" + name + "'");
        }
        return getLinkValues(index, valueIndex);
    }

    /**
     *
     * @param linkIndex
     * @param valueIndex 0:Flow, 1:Depth, 2:Velocity, 3: Volume, 4:capacity
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public float[] getLinkValues(int linkIndex, int valueIndex) throws FileNotFoundException, IOException {
        float[] values = new float[nbPeriods];
        byte[] buffer = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        FileInputStream is = new FileInputStream(file);
        long toskip = startPointer + (0 * periodBytes + 2 * 4) + (4 * nbsubcatchments * sizeSubcatchments) + 4 * (nbNodes * sizeNodes) + (4 * linkIndex * sizeLinks) + (4 * valueIndex);
        while (toskip > 0) {
            toskip = toskip - is.skip(toskip);
        }
        for (int i = 0; i < values.length; i++) {

            is.read(buffer);
            bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            values[i] = bb.getFloat();
            toskip = periodBytes - 4;
            while (toskip > 0) {
                toskip = toskip - is.skip(toskip);
            }
        }
        return values;
    }

    public static void main1(String[] args) {
        try {
            SWMM_Out_Reader reader = new SWMM_Out_Reader(new File("C:\\Users\\B1\\Desktop\\SwmmModell", "artificial_SWMM_model - Kopie.out"));
            float[] values = reader.getLinkValues(0, 1);
            for (int i = 0; i < values.length; i++) {
                System.out.println(i + ": " + values[i]);
            }
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public float[] loadTimeLineVelocity(long pipeMaualID, String pipeName, int numberOfTimes) {
        try {
            return getLinkValues((int) pipeMaualID, 2);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

    @Override
    public float[] loadTimeLineWaterlevel(long pipeMaualID, String pipeName, int numberOfTimes) {
        try {
            return getLinkValues((int) pipeMaualID, 1);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

    @Override
    public float[] loadTimeLineWaterheightManhole(long manholeID, String manholeName, int numberOfTimes) {
        try {
            return getNodeValues(manholeName, 1);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

    @Override
    public float[] loadTimeLineFlux(long pipeMaualID, String pipeName, int numberOfTimes) {
        try {
            return getLinkValues(pipeName, 0);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

    @Override
    public float[] loadTimeLineSpilloutFlux(long ManholeID, String manholeName, int numberOfTimes) {
        try {
            return getNodeValues(manholeName, 5);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

    /**
     * kg/s in pipe
     * @param pipeMaualID
     * @param pipeName
     * @param numberOfTimes
     * @return
     */
    @Override
    public float[][] loadTimeLineMassflux(long pipeMaualID, String pipeName, int numberOfTimes) {
        float[][] values=new float[nbPeriods][nbPollutants];
        try {
            float[] discharge=getLinkValues(pipeName,0);//m^3/s?
        
            for (int i = 0; i < nbPollutants; i++) {
                float[] c=getLinkValues(pipeName, i+5);//mg/L
                for (int t = 0; t < nbPeriods; t++) {
                    values[t][i]=c[t]*discharge[t]/1000f;// -> kg/s
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return values;
    }

    @Override
    public float[][] loadTimeLineConcentration(long pipeMaualID, String pipeName, int numberOfTimes) {
        float[][] values=new float[nbPeriods][nbPollutants];
        try {
            
            for (int i = 0; i < nbPollutants; i++) {
                float[] c=getLinkValues(pipeName, i+5);//mg/L
                for (int t = 0; t < nbPeriods; t++) {
                    values[t][i]=c[t]/1000f; //kg/m^3
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return values;
    }

    @Override
    public String[] loadNamesMaterials() {
        return pollutantNames;
    }

    @Override
    public boolean fillTimelineManhole(long manholeManualID, String manholeName, SparseTimelineManhole timeline) {
        try {
            timeline.setWaterHeight(getNodeValues(manholeName, 1));
            timeline.setSpilloutFlux(getNodeValues(manholeName, 5));
            return true;
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public boolean hasTimeLineMass() {
        return false;
    }

    @Override
    public void loadTimelineManholes(Collection<StorageVolume> manholes, SparseTimeLineManholeContainer container) {

    }

    @Override
    public SparseTimelinePipe loadTimelinePipe(Pipe pipe, SparseTimeLinePipeContainer container) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean fillTimelinePipe(long pipeManualID, String pipeName, SparseTimelinePipe timeline) {
        try {
            timeline.setFlux(getLinkValues(pipeName, 0));
            timeline.setVelocity(getLinkValues(pipeName, 2));
            timeline.setWaterlevel(getLinkValues(pipeName, 1));
            return true;
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public void loadTimelinePipes(Collection<Pipe> pipesToLoad, SparseTimeLinePipeContainer container) {

    }

    @Override
    public SparseTimelineManhole loadTimelineManhole(long manholeManualID, String manholeName, float soleheight, SparseTimeLineManholeContainer container) {
           throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
     
    }

    @Override
    public long[] loadTimeStepsNetwork(boolean startAtZero) {
        return times;
    }

    @Override
    public float[] loadTimeLineInlflow(long ManholeID, String manholeName, int numberOfTimes) {
        try {
            return getNodeValues(manholeName, 3);
        } catch (IOException ex) {
            Logger.getLogger(SWMM_Out_Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new float[numberOfTimes];
    }

}
