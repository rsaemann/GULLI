/*
 * The MIT License
 *
 * Copyright 2020 B1.
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
package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.TimeLinePipe;
import model.topology.Pipe;

/**
 * Export of timelines to textfiles
 * 
 * @author Robert Sämann
 */
public class Timeline_IO {
    
    public static void writePipeTimelineCSV_Massflux(File outputfile,Pipe pipe,String simulationName,int materialindex){
        if(pipe==null)throw new NullPointerException("Pipe is null");
        if(pipe.getMeasurementTimeLine()==null){
            throw new NullPointerException("Pipe "+pipe.getName()+" has no timeline.");
        }
        TimeLinePipe tls = pipe.getStatusTimeLine();
        ArrayTimeLineMeasurement tlm = pipe.getMeasurementTimeLine();
        try {
            FileWriter fw=new FileWriter(outputfile);
            BufferedWriter bw=new BufferedWriter(fw);
            bw.write("Pipe:"+pipe.getName());
            bw.newLine();
            bw.write("Simulation:"+simulationName);
            bw.newLine();
            bw.write("Type:Massflux");
            bw.newLine();
            bw.write("Materialindex:"+materialindex);
            bw.newLine();
            long[] times=pipe.getMeasurementTimeLine().getContainer().measurementTimes;
            bw.write("Times:"+times.length);
            bw.newLine();
            bw.write("ContinuousSampling:"+!tlm.getContainer().isTimespotmeasurement());
            bw.newLine();
            bw.write("TimeMS,Massflux[kg/s]");
            bw.newLine();
            bw.append("***");
            for (int i = 0; i < times.length; i++) {
                bw.newLine();
                bw.append(times[i]+";");
                double discharge =tls.getVelocity(tls.getTimeContainer().getTimeIndex(times[i])) / pipe.getLength();//1/s
                bw.append(pipe.getMeasurementTimeLine().getMass(i, materialindex)*discharge+"");
                bw.newLine();
            }
            bw.flush();
            fw.flush();
            bw.close();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Timeline_IO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
