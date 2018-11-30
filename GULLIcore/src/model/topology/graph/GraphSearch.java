/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.topology.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import model.topology.Connection_Manhole_Pipe;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.StorageVolume;

/**
 *
 * @author saemann
 */
public class GraphSearch {

    public static ArrayList<Pair<Manhole, Double>> findLongestPaths(Manhole root) {

        HashSet<StorageVolume> seen = new HashSet<>();

        PrioritaetsListe<Manhole> bestfound = new PrioritaetsListe<>();
        PrioritaetsListe<Manhole> candidates = new PrioritaetsListe<>();
        candidates.add(root, 0);

        while (!candidates.isEmpty()) {

            Pair<Manhole, Double> c = candidates.poll();
//            System.out.println("poll " + c + "\t still " + candidates.size() + " in candidates, "+bestfound.size()+" bestfound, "+seen.size()+"seen.");
            for (Connection_Manhole_Pipe connection : c.first.getConnections()) {
                Pipe p = connection.getPipe();
                StorageVolume e;
                if (connection.isStartOfPipe()) {
                    e = p.getEndConnection().getManhole();
                } else {
                    e = p.getStartConnection().getManhole();
                }
                if (seen.contains(e)) {
                    //Wurde bereits untersucht. Braucht nicht weiter angesehen zu werden.
                    continue;
                }
                candidates.add((Manhole) e, c.second + p.getLength());
            }
            seen.add(c.first);
            bestfound.add(c);
        }

//        System.out.println("Kandidaten leer.");
        //Liste umdrehen. Längste nach vorne.
        ArrayList<Pair<Manhole, Double>> retur = new ArrayList<>(bestfound.size());
        while (!bestfound.isEmpty()) {
            retur.add(bestfound.pollLast());
        }

        return retur;
    }

    public static ArrayList<Pipe> findUpstreamPipes(Manhole start) {
        if (start == null) {
            return null;
        }
        /**
         * Begin search upstream
         */
        LinkedList<Manhole> manholes = new LinkedList<>();
        manholes.add(start);
        LinkedList<Pipe> alreadySeenPipes = new LinkedList<>();
        LinkedList<Pipe> turnOffs = new LinkedList<>();
        LinkedList<Manhole> alreadySeen = new LinkedList<>();
        while (!manholes.isEmpty()) {
            Manhole mh = manholes.pollLast();
            alreadySeen.add(mh);
//            System.out.println(" use Manhole " + mh);
            if (mh == null) {
//                System.out.println("  -> null");
                continue;
            }
//            System.out.println("  connections: " + mh.getConnections().length);
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.isStartOfPipe()) {
//                    System.out.println("-> is start of pipe ! Not a Upstream connection");
                    //If slope is very less, water might go in opposite direction
                    Pipe p = connection.getPipe();
                    if (p.isHorizontal()) {
                        System.out.println("upstream Pipe is horizontal " + p.getDecline() + " \thorizontal? " + p.isHorizontal());
                        if (alreadySeenPipes.contains(p)) {
                            continue;
                        }
                        alreadySeenPipes.add(p);
                        Manhole upstreamManhole = (Manhole) p.getEndConnection().getManhole();
                        if (!alreadySeen.contains(upstreamManhole)) {
                            manholes.addLast(upstreamManhole);
                        }
                    }
                    continue;
                }
                Pipe p = connection.getPipe();
//                System.out.println("found upstream pipe " + p);
                if (alreadySeenPipes.contains(p)) {
                    continue;
                }
                //Neue Pipe gefunden. Erstelle Shape
//                ArrayList<GeoPosition2D> list = new ArrayList<>(2);
//                list.add(p.getStartConnection().getPosition());
//                list.add(p.getEndConnection().getPosition());
//                smvf.getMapViewer().addLineStringsColored(layerUpstream, p.getId(), list, chUp, bs);
                alreadySeenPipes.add(p);
                Manhole upstreamManhole = (Manhole) p.getStartConnection().getManhole();
                if (!alreadySeen.contains(upstreamManhole)) {
                    manholes.addLast(upstreamManhole);
                }
            }
        }
        return new ArrayList<>(alreadySeenPipes);
    }

    public static ArrayList<Pipe> findDownstreamPipes(Manhole start) {
        if (start == null) {
            return null;
        }
        /**
         * Begin search upstream
         */
        LinkedList<Manhole> manholes = new LinkedList<>();
        manholes.add(start);
        LinkedList<Pipe> alreadySeenPipes = new LinkedList<>();
        LinkedList<Pipe> turnOffs = new LinkedList<>();
        LinkedList<Manhole> alreadySeen = new LinkedList<>();
        while (!manholes.isEmpty()) {
            Manhole mh = manholes.pollLast();
            alreadySeen.add(mh);
//            System.out.println(" use Manhole " + mh);
            if (mh == null) {
//                System.out.println("  -> null");
                continue;
            }
//            System.out.println("  connections: " + mh.getConnections().length);
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                if (connection.isEndOfPipe()) {
//                    System.out.println("-> is End of pipe ! Not a Downstream connection");
                    Pipe p = connection.getPipe();
                    if (p.isHorizontal()) {
                        if (alreadySeenPipes.contains(p)) {
                            continue;
                        }
                        alreadySeenPipes.add(p);
                        Manhole downstreamManhole = (Manhole) p.getStartConnection().getManhole();
                        if (!alreadySeen.contains(downstreamManhole)) {
                            manholes.addLast(downstreamManhole);
                        }
                    }
                    continue;
                }
                Pipe p = connection.getPipe();
//                System.out.println("found upstream pipe " + p);
                if (alreadySeenPipes.contains(p)) {
                    continue;
                }
                //Neue Pipe gefunden. Erstelle Shape
//                ArrayList<GeoPosition2D> list = new ArrayList<>(2);
//                list.add(p.getStartConnection().getPosition());
//                list.add(p.getEndConnection().getPosition());
//                smvf.getMapViewer().addLineStringsColored(layerUpstream, p.getId(), list, chUp, bs);
                alreadySeenPipes.add(p);
                Manhole upstreamManhole = (Manhole) p.getEndConnection().getManhole();
                if (!alreadySeen.contains(upstreamManhole)) {
                    manholes.addLast(upstreamManhole);
                }
            }
        }
        return new ArrayList<>(alreadySeenPipes);
    }

    public static Pipe[] findDownstreamPipes(Network netzwerk, StorageVolume injectionManhole, float[][] minmaxVelocity) {
        HashMap<Integer, float[]> map = new HashMap<>(minmaxVelocity.length);
        for (float[] minmaxVelocity1 : minmaxVelocity) {
            map.put((int) minmaxVelocity1[0], minmaxVelocity1);
        }
        LinkedList<Pipe> alreadySeenPipes = new LinkedList<>();
        LinkedList<Pipe> downstreamPipes = new LinkedList<>();
        LinkedList<Pipe> turnOffs = new LinkedList<>();
        LinkedList<StorageVolume> alreadySeen = new LinkedList<>();
        LinkedList<StorageVolume> manholes = new LinkedList<>();
        manholes.add(injectionManhole);
//        int verboseid=5380;
        while (!manholes.isEmpty()) {
            StorageVolume mh = manholes.pollLast();
            alreadySeen.add(mh);
            if (mh == null) {
                continue;
            }
            for (Connection_Manhole_Pipe connection : mh.getConnections()) {
                //Check Reverse Flown pipes
                if (connection.isEndOfPipe()) {
                    Pipe p = connection.getPipe();

                    //is backward flown?
                    float[] minmax = map.get((int) p.getManualID());

//                    if (p.getManualID() == verboseid) {
//                        System.out.println(p + "  minmax=" + minmax[0] + "," + minmax[1] + "," + minmax[2]);
//                    }
                    if (minmax == null) {
//                        System.err.println(GraphSearch.class + ":: Could not read min/max Velocity for Pipe " + p);
                        continue;
                    }
                    if (minmax[1] > -0.01) {
                        //Pipe is never reverse flown
                        continue;
                    }

                    if (alreadySeenPipes.contains(p)) {
                        continue;
                    }

//                    if (p.getManualID() == verboseid) {
//                        System.out.println(p + " add to list -");
//                    }
                    alreadySeenPipes.add(p);
                    downstreamPipes.add(p);
                    Manhole downstreamManhole = (Manhole) p.getStartConnection().getManhole();
                    if (!alreadySeen.contains(downstreamManhole)) {
                        manholes.addLast(downstreamManhole);
                    }
                } else {
                    //Check straight forward flown pipes 
                    Pipe p = connection.getPipe();
                    float[] minmax = map.get((int) p.getManualID());
                    if (minmax == null) {
//                        System.err.println(GraphSearch.class + ":: Could not read min/max Velocity for Pipe " + p);
                        continue;
                    }
                    if (minmax[2] < 0.001) {
                        //Pipe is ONLY reverse flown
                        continue;
                    }
                    if (alreadySeenPipes.contains(p)) {
                        continue;
                    }
//                     if (p.getManualID() == verboseid) {
//                        System.out.println(p + " add to list +");
//                    }
                    alreadySeenPipes.add(p);
                    Manhole upstreamManhole = (Manhole) p.getEndConnection().getManhole();
                    if (!alreadySeen.contains(upstreamManhole)) {
                        manholes.addLast(upstreamManhole);
                    }
                }
            }
        }
        Pipe[] pipes = new Pipe[alreadySeenPipes.size()];
        Collections.sort(alreadySeenPipes, new Comparator<Pipe>() {
            @Override
            public int compare(Pipe t, Pipe t1) {
                if (t.getManualID() < t1.getManualID()) {
                    return -1;
                }
                if (t.getManualID() == t1.getManualID()) {
                    return 0;
                }
                return 0;
            }
        });
        int i = 0;
        for (Pipe alreadySeenPipe : alreadySeenPipes) {
            pipes[i] = alreadySeenPipe;
            i++;
        }
        return pipes;
    }

    public static ArrayList<Pair<Manhole, Double>> findDownstreamManholes(Manhole start) {
        HashSet<StorageVolume> seen = new HashSet<>();

        PrioritaetsListe<Manhole> bestfound = new PrioritaetsListe<>();
        PrioritaetsListe<Manhole> candidates = new PrioritaetsListe<>();
        candidates.add(start, 0);

        while (!candidates.isEmpty()) {

            Pair<Manhole, Double> c = candidates.poll();
//            System.out.println("poll " + c + "\t still " + candidates.size() + " in candidates, "+bestfound.size()+" bestfound, "+seen.size()+"seen.");
            for (Connection_Manhole_Pipe connection : c.first.getConnections()) {
                Pipe p = connection.getPipe();
                StorageVolume e;
                if (connection.isStartOfPipe()) {
                    e = p.getEndConnection().getManhole();
                } else {
                    continue;
                }
                if (seen.contains(e)) {
                    //Wurde bereits untersucht. Braucht nicht weiter angesehen zu werden.
                    continue;
                }
                candidates.add((Manhole) e, c.second + p.getLength());
            }
            seen.add(c.first);
            bestfound.add(c);
        }

        //Liste umdrehen. Längste nach vorne.
        ArrayList<Pair<Manhole, Double>> retur = new ArrayList<>(bestfound.size());
        while (!bestfound.isEmpty()) {
            retur.add(bestfound.pollLast());
        }

        return retur;
    }

}
