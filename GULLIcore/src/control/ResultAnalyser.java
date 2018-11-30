package control;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.profile.CircularProfile;

/**
 *
 * @author saemann
 */
public class ResultAnalyser {

//    public static String dispersionTimeline(Network nw) {
//        StringBuilder str = new StringBuilder();
//
//        HashSet<Pipe> pipes = new HashSet<>(nw.getPipes());
//        int i = 0;
//        str.append("close all; clear all; clc;");
//        int size = pipes.size() * pipes.iterator().next().getStatusTimeLine().size();
//        str.append("d=zeros(1,").append(size).append(");%diameter\n");
//        str.append("r=zeros(1,").append(size).append(");%Filling ratio\n");
//        str.append("I=zeros(1,").append(size).append(");%declination\n");
//        str.append("K=zeros(1,").append(size).append(");%dispersion\n");
//        double maxdecline = 0.003;
//        double mindecline = 0.001;
//        int pipecount = 0;
//        for (Pipe pipe : pipes) {
//            if (Math.abs(pipe.getDecline()) > maxdecline || Math.abs(pipe.getDecline()) < mindecline) {
//                continue;
//            }
//            pipecount++;
//            for (Stamp<SimplePipeStamp> timeLine : pipe.getStatusTimeLine()) {
//                if (timeLine.getValues().getFill_rate() > 99) {
//                    continue;
//                }
//                i++;
//                str.append("d(").append(i).append(")=").append(Math.abs(((CircularProfile) pipe.getProfile()).getDiameter())).append(";");
//                str.append("I(").append(i).append(")=").append(Math.abs(pipe.getDecline())).append(";");
//                str.append("K(").append(i).append(")=").append(timeLine.getValues().getDispersion()).append(";");
//                str.append("r(").append(i).append(")=").append(timeLine.getValues().getWaterLevelPipe() / ((CircularProfile) pipe.getProfile()).getDiameter()).append(";");
//                if (i % 10 == 0) {
//                    str.append('\n');
//                }
//            }
//
//        }
//        str.append("figure\n");
//        str.append("lines=stem3(d,-I,K,'r:.');\n");
//        str.append("lines.Color(4)=0.3;hold on;\n");
//        str.append("plot3(d,-I,K,'k.');\n");
//
//        str.append("title('Dispersion in ").append(pipecount).append(" Pipes (").append(mindecline).append("<I_{so}<").append(maxdecline).append(")')\n");
//        str.append("xlabel('Diameter d [m]');\n");
//        str.append("ylabel('Decline I [-]');\n");
//        str.append("zlabel('Dispersion K [m^2/s]');\n");
//
//        str.append("figure\n");
//        str.append("lines3=stem3(d,r,K,'r:.');\n");
//        str.append("lines3.Color(4)=0.3;hold on;\n");
//        str.append("plot3(d,r,K,'k.');\n");
//
//        str.append("title('Dispersion by Fillrate in ").append(pipecount).append(" Pipes (").append(mindecline).append("<I_{so}<").append(maxdecline).append(")')\n");
//        str.append("xlabel('Diameter d [m]');\n");
//        str.append("ylabel('Fillrate  [-]');\n");
//        str.append("zlabel('Dispersion [m^2/s]');\n");
//
//        return str.toString();
//    }

//    public static String dispersionOnlyFull(Network nw) {
//        StringBuilder str = new StringBuilder();
//
//        HashSet<Pipe> pipes = new HashSet<>(nw.getPipes());
////        pipes.addAll(nw.getPipesSewer());
//        int i = 0;
//        str.append("close all; clear all; clc;");
//        str.append("d=zeros(1,").append(pipes.size()).append(");%diameter\n");
//        str.append("r=zeros(1,").append(pipes.size()).append(");%Filling ratio\n");
//        str.append("I=zeros(1,").append(pipes.size()).append(");%declination\n");
//        str.append("K=zeros(1,").append(pipes.size()).append(");%dispersion\n");
//
//        for (Pipe pipe : pipes) {
//            if (Math.abs(pipe.getDecline()) > 0.1) {
//                continue;
//            }
//
//            double maxK = 0;
//            double maxR = 0;
//            for (Stamp<SimplePipeStamp> timeLine : pipe.getStatusTimeLine()) {
//                maxK = Math.max(maxK, timeLine.getValues().getDispersion());
//                maxR = Math.max(maxR, timeLine.getValues().getFill_rate());
//            }
//            if (maxR < 99) {
//                continue;
//            }
//            i++;
//            str.append("d(").append(i).append(")=").append(Math.abs(((CircularProfile) pipe.getProfile()).getDiameter())).append(";");
//            str.append("I(").append(i).append(")=").append(Math.abs(pipe.getDecline())).append(";");
//            str.append("K(").append(i).append(")=").append(maxK).append(";").append('\n');
//            str.append("r(").append(i).append(")=").append(maxR).append(";").append('\n');
//        }
//        str.append("figure\n");
//        str.append("lines=stem3(d,-I,K,'r:.');\n");
//        str.append("lines.Color(4)=0.3;hold on;\n");
//        str.append("plot3(d,-I,K,'k.');\n");
//
//        str.append("title('Max. Dispersion in 100% filled Pipes')\n");
//        str.append("xlabel('Diameter d [m]');\n");
//        str.append("ylabel('Decline I [-]');\n");
//        str.append("zlabel('Dispersion K [m^2/s]');\n");
//
//        return str.toString();
//    }

    public static String diameter_decline(Network nw) {
        HashMap<String, Integer> map = new HashMap<>(nw.getPipes().size());
        HashSet<Pipe> pipes = new HashSet<>(nw.getPipes());
//        pipes.addAll(nw.getPipes());
        DecimalFormat f = new DecimalFormat();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator(',');
        dfs.setDecimalSeparator('.');
        f.setDecimalFormatSymbols(dfs);
        f.setMaximumFractionDigits(3);
        for (Pipe pipe : pipes) {
            String key = ((CircularProfile) pipe.getProfile()).getDiameter() + "_" + f.format(Math.abs(pipe.getDecline()));
            Integer anzahl = 0;
            if (map.containsKey(key)) {
                anzahl = map.get(key);
            }
            anzahl++;
            map.put(key, anzahl);
        }
        StringBuilder str = new StringBuilder();
        str.append("d=zeros(1,").append(map.size()).append(");\n");
        str.append("I=zeros(1,").append(map.size()).append(");\n");
        str.append("n=zeros(1,").append(map.size()).append(");\n");
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            double diameter = Double.parseDouble(e.getKey().split("_")[0]);
            double decline = Double.parseDouble(e.getKey().split("_")[1]);
            i++;
            str.append("d(").append(i).append(")=").append(diameter).append(";");
            str.append("I(").append(i).append(")=").append(decline).append(";");
            str.append("n(").append(i).append(")=").append(e.getValue()).append(";\n");
        }
        str.append("figure\n");
        str.append("lines=stem3(d,-I,n,'r:.');\n");
        str.append("lines.Color(4)=0.3;hold on;\n");
        str.append("plot3(d,-I,n,'k.');\n");

        str.append("title('Number of Pipes')\n");
        str.append("xlabel('Diameter d [m]')\n");
        str.append("ylabel('Decline I_{so}[-]')\n");
        str.append("zlabel('Count')\n");
        return str.toString();
    }

//    public static String ReynoldsVerteilung(Network n) {
//        double maxRe = 0;
//        for (Pipe pipe : n.getPipes()) {
//            for (Stamp<SimplePipeStamp> tl : pipe.getStatusTimeLine()) {
//                maxRe = Math.max(maxRe, tl.getValues().getReynolds());
//            }
//        }
//        int threshold = 10000;
//        int deltaReHigh = 25000;
//        int deltaReLow = 1000;
//        double low2High=deltaReLow/(double)deltaReHigh;
//        int nbLow = threshold / deltaReLow;
//        int nbHigh = (int) ((maxRe) / deltaReHigh);
//        int[] rek = new int[nbLow + nbHigh+2];
//        int[] remax = new int[nbLow + nbHigh+2];
//        int counts = 0;
//        
//        System.out.println("Low: "+nbLow+"   high: "+nbHigh +"    array:"+rek.length);
//
//        for (Pipe pipe : n.getPipes()) {
//            double maxlocalRe = 0;
//            for (Stamp<SimplePipeStamp> tl : pipe.getStatusTimeLine()) {
//                if (tl.getValues().getFill_rate() > 0) {
//                    if (tl.getValues().getReynolds() < threshold) {
//                        rek[(int) (tl.getValues().getReynolds() / deltaReLow)]++;
//                    } else {
//                        try {
//                            rek[(int) (nbLow + (tl.getValues().getReynolds()-threshold) / deltaReHigh)]++;
//                        } catch (Exception e) {
//                            System.err.println("value:"+tl.getValues().getReynolds()+"      re/dre="+(tl.getValues().getReynolds()) / deltaReHigh);
//                        }
//                    }
//                    maxlocalRe = Math.max(maxlocalRe, tl.getValues().getReynolds());
//                    counts++;
//                }
//            }
//            if (maxlocalRe < threshold) {
//                remax[(int) (maxlocalRe / deltaReLow)]++;
//            } else {
//                remax[(int) (nbLow + (maxlocalRe-threshold) / deltaReHigh)]++;
//            }
//        }
//
//        StringBuilder str = new StringBuilder();
//        str.append("close all; clear all; clc;");
//        str.append("re=zeros(1,").append(rek.length).append(");%Reynoldszahl \n");
//        str.append("count=zeros(1,").append(rek.length).append(");%Count\n");
//        str.append("countmax=zeros(1,").append(rek.length).append(");%Count\n");
//        for (int i = 0; i < nbLow; i++) {
//            str.append("re(").append(i + 1).append(")=").append(i * deltaReLow).append(";   %").append(deltaReLow * i).append("-").append(deltaReLow * (i + 1) - 1).append("\n");
//            str.append("count(").append(i + 1).append(")=").append(rek[i]).append(";\n");
//            str.append("countmax(").append(i + 1).append(")=").append(remax[i]).append(";\n");
//        }
//        for (int j = 0; j < nbHigh; j++) {
//            int i = j + nbLow;
//            str.append("re(").append(i + 1).append(")=").append(threshold+j * deltaReHigh).append(";   %").append(threshold+j * deltaReHigh).append("-").append(threshold+(j+1) * deltaReHigh-1).append("\n");
//            str.append("count(").append(i + 1).append(")=").append(rek[i]*low2High).append(";\n");
//            str.append("countmax(").append(i + 1).append(")=").append(remax[i]).append(";\n");
//        }
//        for (int j = nbHigh+nbLow+1; j < rek.length; j++) {
//            str.append("re(").append(j).append(")=NaN;\n");
//            str.append("count(").append(j).append(")=0;\n");
//            str.append("countmax(").append(j).append(")=0;\n");
//        }
//        str.append("figure\n");
//        str.append("pm=plot(re,100*countmax/").append(n.getPipes().size()).append(",'r:.');\n");
//
//        str.append("title('Distribution of maximum Reynoldsnumber');\n");
//        str.append("xlabel('Reynoldsnumber Re [-]');\n");
//        str.append("ylabel('Quantity [%]');\n");
//
//        str.append("figure\n");
//        str.append("p=plot(re,count,'r:.');\n");
//
//        str.append("title('Distribution of Reynoldsnumber');\n");
//        str.append("xlabel('Reynoldsnumber Re [-]');\n");
//        str.append("ylabel('Quantity [-]');\n");
//        return str.toString();
//    }
}
