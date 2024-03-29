/*
 * The MIT License
 *
 * Copyright 2019 saemann.
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
package com.saemann.gulli.core.io;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Class for number decoding from tetfiles without creation of STring-Objects.
 * This class can decode numbers from a filereader without mass creation of
 * char[] through split() operation.
 *
 * @author saemann
 */
public class NumberConverter {

    private Reader reader;

    private char[] buffer = new char[256];

    private char splitter = ' ';

    private char c;
    private int index, laststart;
    private boolean lastWasSplitter;
    private boolean searchForNumbers;

    public static boolean verbose = false;

    long sum = 0;
    long indexl = 1;
    double digitindex = 0;
    private boolean negative;

    public NumberConverter(Reader in) {
        this.reader = in;
    }

    /**
     *
     * @param toFill
     * @return true if the line contained any values, false if it was empty
     * @throws IOException
     */
    public boolean readNextLineDoubles(double[] toFill) throws IOException {

        lastWasSplitter = true;

        index = 0;
        laststart = 0;
        searchForNumbers = true;
        for (int i = 0; i < buffer.length; i++) {
//            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }
                    if (searchForNumbers) {
                        toFill[index] = parseDoubleFromToInclude(buffer, laststart, i - 1);
                        index++;
                    }
                    break;//\n & \r
                }
                buffer[i] = c;
                if (searchForNumbers) {
                    if (c == splitter) {
                        //found position to split the string
                        if (lastWasSplitter == false) {
                            //end a pattern here
                            toFill[index] = parseDoubleFromToInclude(buffer, laststart, i - 1);
                            index++;
                            if (index >= toFill.length) {
                                searchForNumbers = false;
                            }
                            lastWasSplitter = true;
                        }

                    } else {
                        if (lastWasSplitter) {
                            laststart = i;
                            lastWasSplitter = false;
                        }

                    }
                }
//            }
        }
//        if (index == 0) {
//            for (int i = 0; i < toFill.length; i++) {
//                toFill[i] = 0;
//            }
//            return false;
//        }
        //fill all the non read values with zero
        for (int i = index; i < toFill.length; i++) {
            toFill[i] = 0;
        }
        //only return true, if there were numbers to read
        return index > 0;
    }

    /**
     * 
     * @param toFill
     * @return number of values read. if 0 -> no values contained
     * @throws IOException 
     */
    public int readNextLineFloats(float[] toFill) throws IOException {

        lastWasSplitter = true;

        index = 0;
        laststart = 0;
        searchForNumbers = true;
        for (int i = 0; i < buffer.length; i++) {
            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }
                    if (searchForNumbers) {
                        toFill[index] = parseFloatFromToInclude(buffer, laststart, i - 1);
                        index++;
                    }
                    break;//\n & \r
                }
                buffer[i] = c;
                if (searchForNumbers) {
                    if (c == splitter) {
                        //found position to split the string
                        if (lastWasSplitter == false) {
                            //end a pattern here
                            toFill[index] = parseFloatFromToInclude(buffer, laststart, i - 1);
                            index++;
                            if (index >= toFill.length) {
                                searchForNumbers = false;
                            }
                            lastWasSplitter = true;
                        }

                    } else {
                        if (lastWasSplitter) {
                            laststart = i;
                            lastWasSplitter = false;
                        }

                    }
                }
            }
        }
//        if (index == 0) {
//            for (int i = 0; i < toFill.length; i++) {
//                toFill[i] = 0;
//            }
//            return false;
//        }
        //fill all the non read values with zero
        for (int i = index; i < toFill.length; i++) {
            toFill[i] = 0;
        }
        //Return true, if there were numbers to read.
        return index;
    }

    public boolean readNextLineInteger(int[] toFill) throws IOException {

        lastWasSplitter = true;
        index = 0;
        searchForNumbers = true;
        laststart = 0;
        for (int i = 0; i < buffer.length; i++) {
//            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }
                    if (searchForNumbers) {
                        toFill[index] = parseIntegerFromToInclude(buffer, laststart, i - 1);
                        index++;
                    }
                    break;//\n & \r
                }
                buffer[i] = c;

                if (searchForNumbers) {
                    if (c == splitter) {

                        //found position to split the string
                        if (lastWasSplitter == false) {
                            //end a pattern here
                            toFill[index] = parseIntegerFromToInclude(buffer, laststart, i - 1);
                            index++;
                            if (index >= toFill.length) {
                                searchForNumbers = false;
                            }
                            lastWasSplitter = true;
                        }

                    } else {
                        if (lastWasSplitter) {
                            laststart = i;
                            lastWasSplitter = false;
                        }

                    }
                }
//            }
        }
//        if (index == 0) {
//            for (int i = 0; i < toFill.length; i++) {
//                toFill[i] = 0;
//            }
//            return false;
//        } else {
            //fill all the non read values with zero
            for (int i = index; i < toFill.length; i++) {
//                if (toFill[i] != 0) {
                    toFill[i] = 0;
//                }
            }
            return index!=0;//true;
//        }
    }

    /**
     * Returns the Integer in the text until the next seperator
     *
     * @return -1 as error
     */
    public int readNextInteger() throws IOException {
        lastWasSplitter = true;
        index = 0;
        searchForNumbers = true;
        laststart = 0;
        for (int i = 0; i < buffer.length; i++) {
//            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }
                    if (searchForNumbers) {
                        return parseIntegerFromToInclude(buffer, laststart, i - 1);

                    }
                    break;//\n & \r
                }
                buffer[i] = c;

                if (searchForNumbers) {
                    if (c == splitter) {

                        //found position to split the string
                        if (lastWasSplitter == false) {
                            //end a pattern here
                            return parseIntegerFromToInclude(buffer, laststart, i - 1);
                        }

                    } else {
                        if (lastWasSplitter) {
                            laststart = i;
                            lastWasSplitter = false;
                        }

                    }
                }
//            }
        }
        return -1;
    }

    /**
     * Returns the Integer in the text until the next seperator
     *
     * @return NaN as error
     */
    public double readNextDouble() throws IOException {
        lastWasSplitter = true;
        index = 0;
        searchForNumbers = true;
        laststart = 0;
        for (int i = 0; i < buffer.length; i++) {
//            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }
                    if (searchForNumbers) {
                        return parseDoubleFromToInclude(buffer, laststart, i - 1);

                    }
                    break;//\n & \r
                }
                buffer[i] = c;

                if (searchForNumbers) {
                    if (c == splitter) {

                        //found position to split the string
                        if (lastWasSplitter == false) {
                            //end a pattern here
                            return parseDoubleFromToInclude(buffer, laststart, i - 1);
                        }

                    } else {
                        if (lastWasSplitter) {
                            laststart = i;
                            lastWasSplitter = false;
                        }

                    }
                }
//            }
        }
        return Double.NaN;
    }

    /**
     * Read and decode indices for mooreNeighbours when loading surface
     * information
     *
     * @param toFill
     * @return
     * @throws IOException
     */
    public int[][] fillMooreNeighbours(int[][] toFill) throws IOException {
        char c;
//        boolean lastWasSplitter = true;
        int linelength = -1;
        char splitID = ',';
        char splitNeighbours = ' ';
        int indexComma = -1;
        int neighbours = 0;
        int index;
        for (int i = 0; i < buffer.length; i++) {
            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        continue;
                    }

                    linelength = i;
                    break;//\n & \r
                } else if (c == splitID) {
                    indexComma = i;
                } else if (c == splitNeighbours) {
                    neighbours++;
                }
                buffer[i] = c;
            }
        }
        if (indexComma < 0) {
            if (linelength > 0) {
                //return number of elements
                int[][] retur = new int[1][1];
                retur[0][0] = parseIntegerFromToInclude(buffer, 0, linelength);
                return retur;
            } else {
                //no line here, it was the end of the file
                return null;
            }
        }
        int[] neighbourIDs = new int[neighbours];
        int lastBlank = indexComma + 1;
        index = 0;
        for (int i = lastBlank + 1; i < linelength; i++) {
            if (buffer[i] == splitNeighbours) {
                neighbourIDs[index] = parseIntegerFromToInclude(buffer, lastBlank + 1, i - 1);
                index++;
                lastBlank = i;
            }
        }
        int[][] retur = new int[2][];
        retur[0] = new int[1];// for the ID
        retur[0][0] = parseIntegerFromToInclude(buffer, 0, indexComma - 1);
        retur[1] = neighbourIDs;
        return retur;
    }

    /**
     * Reads the information of a line from the NODE2TRIANGLE.dat file. Lines
     * like Node;#Triangles;TriangleIDs... *0;3;1050963;1051612;1051614
     * 1;3;1087492;1088149;1088790
     *
     * @return int[2][] where int[0][0] node ID, int[1] = array of triangleIDs
     * @throws IOException
     */
    public int[][] getNodeToTriangleLine() throws IOException {
        char c;
//        boolean lastWasSplitter = true;
//        int linelength = -1;
        char split = ';';
        int lastSplitIndex = -1;
        int index = 0;
        int nodeID = -1;
        int[] triangleIDs = null;
        for (int i = 0; i < buffer.length; i++) {
            if (reader.ready()) {
                c = (char) reader.read();
                if (c == 10 || c == 13) {
                    if (i == 0) {
                        //this LF still is part of the old line. we need to skip this to get the next line
                        i--;
                        return null;
                    }

//                    linelength = i;
                    break;//\n & \r
                } else if (c == split) {
                    if (index == 0) {
                        //here comes the node ID
                        nodeID = parseIntegerFromToInclude(buffer, 0, i - 1);
                    } else if (index == 1) {
                        //Here comes the number of triangleIDs following
                        int numberOfEntries = parseIntegerFromToInclude(buffer, lastSplitIndex + 1, i - 1);
                        triangleIDs = new int[numberOfEntries];
                    } else {
                        //one of the triangleIDs
                        int triangleID = parseIntegerFromToInclude(buffer, lastSplitIndex + 1, i - 1);
                        triangleIDs[index - 2] = triangleID;
                    }
                    index++;
                    lastSplitIndex = i;
                }
                buffer[i] = c;
            }
        }

        int[][] retur = new int[2][];
        retur[0] = new int[1];// for the ID
        retur[0][0] = nodeID;
        retur[1] = triangleIDs;
        return retur;
    }

    public double parseDoubleLength(char[] string, int fromIncluded, int length) {
        return parseDoubleFromToInclude(string, fromIncluded, fromIncluded + length - 1);
    }

    public double parseDoubleFromToExluded(char[] string, int fromIncluded, int toexcluded) {
        return parseDoubleFromToInclude(string, fromIncluded, toexcluded - 1);
    }

    public double parseDoubleFromToInclude(char[] string, int fromIncluded, int toIncluded) {

//        long sum = 0;
//        long indexl = 1;
//        double digitindex = 0;
        sum = 0;
        indexl = 1;
        digitindex = 0;

        for (int i = toIncluded; i >= fromIncluded; i--) {
            char c = string[i];
            if (c == 46/*'.'*/) {
                digitindex = indexl;
                continue;
            }

            int d = c - 48;//Character.digit(c, 10);
            if (d < 0 || d > 9) {
                continue;
            }
            sum += indexl * d;
            indexl *= 10;
//            System.out.println("'" + c + "' : " + d + ", at index=" + i + ", factor: " + index + " ,\t sum=" + sum);
        }
        if (digitindex == 0) {
            //is an integer without .
            return sum;
        }
        double result = sum / digitindex;
//        System.out.println("number=" + result);
        return result;

    }

    public float parseFloatFromToInclude(char[] string, int fromIncluded, int toIncluded) {

//        long sum = 0;
//        long indexl = 1;
//        double digitindex = 0;
        sum = 0;
        indexl = 1;
        digitindex = 0;

        for (int i = toIncluded; i >= fromIncluded; i--) {
            char c = string[i];
            if (c == 46/*'.'*/) {
                digitindex = indexl;
                continue;
            }

            int d = c - 48;//Character.digit(c, 10);
            if (d < 0 || d > 9) {
                continue;
            }
            sum += indexl * d;
            indexl *= 10;
//            System.out.println("'" + c + "' : " + d + ", at index=" + i + ", factor: " + index + " ,\t sum=" + sum);
        }
        if (digitindex == 0) {
            //is an integer without .
            return sum;
        }
        float result = (float) (sum / digitindex);
//        System.out.println("number=" + result);
        return result;

    }

    public int parseIntegerFromToInclude(char[] string, int fromIncluded, int toIncluded) {

//        long sum = 0;
//        long indexl = 1;
//        long digitindex = -1;
//        boolean negative = false;
        sum = 0;
        indexl = 1;
        digitindex = -1;
        negative = false;

        for (int i = toIncluded; i >= fromIncluded; i--) {
            char c = string[i];
            if (c == 46/*'.'*/) {
                digitindex = indexl;
                if (verbose) {
                    System.err.println("Integer String to parse has '.' character.");
                }
                continue;
            }

            int d = c - 48;//Character.digit(c, 10);
            if (d < 0 || d > 9) {
                if (d == -3) {
                    negative = true;
                }
                continue;
            }
            sum += indexl * d;
            indexl *= 10;
        }
        if (digitindex < 1) {
            //is an integer without .
            if (negative) {
                return (int) -sum;
            }
            return (int) sum;
        }

        int result = (int) (sum / digitindex);
        if (negative) {
            return -result;
        }
        return result;

    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public void setSplitter(char splitter) {
        this.splitter = splitter;
    }

    public char getSplitter() {
        return splitter;
    }

    public void setBufferLength(int length) {
        this.buffer = new char[length];
    }

    public int getBufferLength() {
        return buffer.length;
    }

    public static void main0(String[] args) throws FileNotFoundException, IOException {

        FileReader fr = new FileReader("L:\\GULLI_Input\\Modell2017Mai\\2D_Model\\2DModell_10cm_3m2_ohne_BK.model\\X.dat");
        NumberConverter nc = new NumberConverter(fr);

        nc.parseDoubleFromToInclude(new char[]{'8', '7', ',', '6', '5'}, 0, 4);
        System.out.println('.' == 46);
        System.out.println("'0'=" + (int) '0');
        System.out.println("'\r'=" + (int) '\r');
        System.out.println("' '=" + (int) ' ');

        double[] numbers = new double[3];
        for (int i = 0; i < 3; i++) {
            nc.readNextLineDoubles(numbers);
            for (int j = 0; j < numbers.length; j++) {
                System.out.println(j + ":  " + numbers[j]);

            }
        }
    }
}
