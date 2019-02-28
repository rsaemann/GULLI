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
package model.surface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import io.extran.HE_SurfaceIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.java.io.zrz.jgdb.FileGDBFactory;
import main.java.io.zrz.jgdb.GeoDB;
import main.java.io.zrz.jgdb.GeoFeature;
import main.java.io.zrz.jgdb.GeoRndomAccessFileBuffer_RW;
import main.java.io.zrz.jgdb.GeoTable;

/**
 *
 * @author saemann
 */
public class SurfaceEditor {

    private final String originDirectory;
    private Surface surf;
    private int[] vertexOldNewReference;
    private int[] trianglesOldNewReference;
    private String[] usedManholes;
    private int numberNewTriagles = -1;

    public SurfaceEditor(String origindirectory) throws IOException {
        this.originDirectory = origindirectory;
        this.surf = HE_SurfaceIO.loadSurface(new File(origindirectory));
    }

    public SurfaceEditor(Surface srf) {
        this.surf = srf;
        this.originDirectory = surf.fileTriangles.getParent();
    }

    public void removeTrianglesInside(Polygon polygon) {
        int removedCounter = 0;
        for (int i = 0; i < surf.triangleNodes.length; i++) {
            int[] triangleNodes = surf.triangleNodes[i];
            if (triangleNodes == null) {
                continue;
            }
            int inside = 0;
            for (int j = 0; j < 3; j++) {
                double[] position = surf.vertices[triangleNodes[j]];
                if (polygon.contains(polygon.getFactory().createPoint(new Coordinate(position[0], position[1])))) {
                    inside++;
                } else {
                    break;
                }
            }
            if (inside < 3) {
                continue;
            }
            surf.triangleNodes[i] = null;
            removedCounter++;
        }

        System.out.println("Removed " + removedCounter + " triangles inside Polygon");
    }

    public void removeTrianglesOutside(Polygon polygon) {
        int removedCounter = 0;
        for (int i = 0; i < surf.triangleNodes.length; i++) {
            int[] triangleNodes = surf.triangleNodes[i];
            if (triangleNodes == null) {
                continue;
            }
            int inside = 0;
            for (int j = 0; j < 3; j++) {
                double[] position = surf.vertices[triangleNodes[j]];
                if (polygon.contains(polygon.getFactory().createPoint(new Coordinate(position[0], position[1])))) {
                    inside++;
                } else {
                    break;
                }
            }
            if (inside > 2) {
                continue;
            }
            surf.triangleNodes[i] = null;
            removedCounter++;
        }

        System.out.println("Removed " + removedCounter + " triangles outside Polygon");
    }

    /**
     * Creates a new Surface without unused vertices and triangles.
     *
     * @param surf
     * @return
     */
    public void trimSurface() {
        //Trim triangles
        int counterNewTriangles = 0;
        for (int i = 0; i < surf.triangleNodes.length; i++) {
            if (surf.triangleNodes[i] != null && surf.triangleNodes[i].length == 3) {
                counterNewTriangles++;
            }
        }
        this.numberNewTriagles = counterNewTriangles;
        System.out.println("using " + counterNewTriangles + " / " + surf.triangleNodes.length + " triangles.");
        int[][] triangleNodes;
        trianglesOldNewReference = new int[surf.triangleNodes.length];
        if (counterNewTriangles == surf.triangleNodes.length) {
            //Nothing to change
            triangleNodes = surf.triangleNodes;
            trianglesOldNewReference = new int[counterNewTriangles];
            for (int i = 0; i < triangleNodes.length; i++) {
                trianglesOldNewReference[i] = i;
            }
        } else {
            //Reorder Triangles
            triangleNodes = new int[counterNewTriangles][3];
            int indexNew = 0;
            for (int i = 0; i < surf.triangleNodes.length; i++) {
                if (surf.triangleNodes[i] == null || surf.triangleNodes[i].length < 3) {
                    trianglesOldNewReference[i] = -1;
                    continue;
                }
                triangleNodes[indexNew] = surf.triangleNodes[i];
                trianglesOldNewReference[i] = indexNew;
                indexNew++;
            }
        }

        //find all vertices that are not used
        boolean[] vertexReferenced = new boolean[surf.vertices.length];
        for (int i = 0; i < triangleNodes.length; i++) {
            if (triangleNodes[i] == null) {
                continue;
            }
            for (int j = 0; j < 3; j++) {
                int index = triangleNodes[i][j];
                vertexReferenced[index] = true;
            }
        }
        //count number of unused Vertices
        int numberUsed = 0;
        int numberUnUsed = 0;
        for (int i = 0; i < vertexReferenced.length; i++) {
            if (vertexReferenced[i]) {
                numberUsed++;
            } else {
                numberUnUsed++;
            }
        };

        double[][] verticesNew = new double[numberUsed][];
        vertexOldNewReference = new int[surf.vertices.length];
        int newIndex = 0;
        for (int i = 0; i < vertexReferenced.length; i++) {
            if (vertexReferenced[i]) {
                verticesNew[newIndex] = surf.vertices[i];
                vertexOldNewReference[i] = newIndex;
                newIndex++;
            } else {
                vertexOldNewReference[i] = -1;
                //Erase unreferenced vertices;
            }
        }
        System.out.println("Vertices: Used: " + numberUsed + ",  Unused: " + numberUnUsed + " / " + surf.vertices.length);
        //Reference new triangles
        int[][] trianglesNew = new int[triangleNodes.length][3];
        for (int i = 0; i < trianglesNew.length; i++) {
            if (triangleNodes[i] == null) {
                continue;
            }
            for (int j = 0; j < 3; j++) {
                int nI = vertexOldNewReference[triangleNodes[i][j]];
                if (nI < 0) {
                    System.err.println("Found a reference to a deleted vertex, this should never happen.");
                }
                trianglesNew[i][j] = nI;
            }
        }

        //trim triangleMids
        double[][] trimmedMids = trimTriangleMids(surf.getTriangleMids(), counterNewTriangles);
        this.surf = new Surface(verticesNew, trianglesNew, trimNeighboursNeumann(surf.neumannNeighbours, trianglesOldNewReference, counterNewTriangles), null, "EPSG:32632");
        this.surf.setTriangleMids(trimmedMids);
    }

    public int[][] trimNeighboursNeumann(int[][] originalNeighbours, int[] triangleOld2NewReferences, int numberOfNewTriangles) {
        int[][] newNB = new int[numberOfNewTriangles][3];
        int index = 0;
        for (int i = 0; i < originalNeighbours.length; i++) {
            if (triangleOld2NewReferences[i] < 0) {
                continue;
            }
            index = triangleOld2NewReferences[i];
            for (int j = 0; j < 3; j++) {
                if (originalNeighbours[i][j] < 0) {
                    newNB[index][j] = -1;
                } else {
                    newNB[index][j] = triangleOld2NewReferences[originalNeighbours[i][j]];
                }
            }
        }
        return newNB;
    }

    public int[][] trimNeighboursMoore(int[][] originalNeighboursMoore, int[] triangleOld2NewReferences, int numberOfNewTriangles) {
        int[][] newNB = new int[numberOfNewTriangles][];
//        int index = 0;
        for (int i = 0; i < originalNeighboursMoore.length; i++) {
            if (triangleOld2NewReferences[i] < 0) {
                continue;
            }
            int[] ids = new int[(originalNeighboursMoore[i].length)];
            int nb = 0;
            for (int j = 0; j < originalNeighboursMoore[i].length; j++) {
                if (originalNeighboursMoore[i][j] < 0) {
                    continue;
                } else {
                    ids[j] = triangleOld2NewReferences[originalNeighboursMoore[i][j]];
                    nb++;
                }
            }
            int[] array = new int[nb];
            for (int j = 0; j < array.length; j++) {
                array[i] = ids[i];
            }
            newNB[triangleOld2NewReferences[i]] = array;
//            index++;
        }
        return newNB;
    }

    public void writeTrimmedHYDROMODELdat(File outputFile, Collection<String> manholes) throws IOException {
        if (outputFile.exists()) {
            throw new IOException("File " + outputFile.getAbsolutePath() + " already exists.");
        }
        //List all manholes that are in a possible position

        FileWriter fw = new FileWriter(outputFile);
        BufferedWriter bw = new BufferedWriter(fw);
        //header
        bw.write("m");
        bw.newLine();
        bw.write(manholes.size() + "");
        bw.newLine();
        for (String mh : manholes) {
            bw.write(mh + " 0 0\n");
            bw.flush();
        }
        bw.flush();
        bw.close();
        fw.close();
    }

    public LinkedList<String> writeTrimmedManholes(File targetFile) throws IOException {

        if (targetFile.exists()) {
            throw new IOException("File " + targetFile.getAbsolutePath() + " already exists. Remove before!");
        }
        File fileOriginSewerSurf = new File(originDirectory, "SEWER-SURF_NODES.dat");
        if (!fileOriginSewerSurf.exists()) {
            throw new IOException("Origin File " + fileOriginSewerSurf.getAbsolutePath() + " does not exist.");
        }
        if (targetFile.getParentFile().equals(new File(originDirectory))) {
            System.err.println("Can not override original Files.");
            return null;
        }

        LinkedList<String> usedManholeNames = new LinkedList<>();
// Manholes are sewer-surface file content
        FileReader fr = new FileReader(fileOriginSewerSurf);
        BufferedReader br = new BufferedReader(fr);
        FileWriter fw = new FileWriter(targetFile);
        BufferedWriter bw = new BufferedWriter(fw);
        //skip header
        fw.write(br.readLine());
        int counterThrownOutTriangles = 0;
        while (br.ready()) {
            String line = br.readLine();
            //Find index of triangle start
            int pos0 = line.indexOf("% 1 ") + 4;
            int pos1 = line.indexOf(" ", pos0 + 1);
            int oldTriangleID = Integer.parseInt(line.substring(pos0, pos1));
            int newTriangleID = trianglesOldNewReference[oldTriangleID];
            if (newTriangleID < 0) {
                counterThrownOutTriangles++;
                continue;
            }
            bw.newLine();
            bw.write(line.substring(0, pos0));
            bw.write(newTriangleID + "");
            bw.write(line.substring(pos1));

            //Add name if this manhole to the list
            usedManholeNames.add(line.substring(0, line.indexOf(" ")));
        }
        bw.close();
        fw.close();
        br.close();
        fr.close();
        System.out.println("Wrote new Manholes to " + targetFile.getAbsolutePath() + "  skipping " + counterThrownOutTriangles + " manholes. Now:" + usedManholeNames.size());

        return usedManholeNames;
    }

    public LinkedList<String> writeTrimmedInlets(File targetFile) throws IOException {

        File originalFile = new File(originDirectory, "SURF-SEWER_NODES.dat");
        if (targetFile.getParentFile().equals(originalFile.getParentFile())) {
            System.err.println("Can not override the original file at " + targetFile.getAbsolutePath());
            return null;
        }
        if (!originalFile.exists()) {
            throw new IOException("Original inlet file does not exists at " + originalFile.getAbsolutePath());
        }
        if (targetFile.exists() && targetFile.length() > 1000) {
            throw new IOException("Inlet target file already exists, remove " + targetFile.getAbsolutePath());
        }
        // Inlets are sewer-surface file content
        FileReader fr = new FileReader(originalFile);
        BufferedReader br = new BufferedReader(fr);
        FileWriter fw = new FileWriter(targetFile);
        BufferedWriter bw = new BufferedWriter(fw);
        //skip header
        fw.write(br.readLine());
        int counterThrownOutTriangles = 0;
        LinkedList<String> usedInletsNames = new LinkedList<>();
        while (br.ready()) {
            String line = br.readLine();
            if (line.length() < 10) {
                continue;
            }
            //Find index of triangle start
            int pos = line.indexOf("%") + 4;
            int pos0 = line.indexOf(" 1 ", pos) + 3;
            int pos1 = line.indexOf(" ", pos0 + 1);
            int oldTriangleID = Integer.parseInt(line.substring(pos0, pos1));
            int newTriangleID = trianglesOldNewReference[oldTriangleID];
            if (newTriangleID < 0) {
                counterThrownOutTriangles++;
                continue;
            }
            bw.newLine();
            bw.write(line.substring(0, pos0));
            bw.write(newTriangleID + "");
            bw.write(line.substring(pos1));
            //get Inlet name
            usedInletsNames.add(line.substring(0, line.indexOf(" ")));
        }
        bw.close();
        fw.close();
        br.close();
        fr.close();
        System.out.println("Wrote new Inlets to " + targetFile.getAbsolutePath() + "  skipping " + counterThrownOutTriangles + " inlets. Now:" + usedInletsNames.size());
        return usedInletsNames;
    }

    /**
     * the HYSTEM.dat file contains the reference from the HYSTEM-Rain collector
     * areas to the HE2D raincollector triangles.
     *
     * @param destinationHystemDat
     * @param triangleOldToNewReference
     * @param outputEmpty
     * @throws IOException
     */
    public void writeTrimmedHystemDat(File destinationHystemDat, int[] triangleOldToNewReference, boolean outputEmpty) throws IOException {

        if (destinationHystemDat.exists()) {
            throw new IOException("File " + destinationHystemDat.getAbsolutePath() + " already exists. Remove before!");
        }
        File fileOriginHystemDat = new File(originDirectory, "hystem.dat");
        if (!fileOriginHystemDat.exists()) {
            throw new IOException("Original hystem file does not exist at " + fileOriginHystemDat.getAbsolutePath());
        }
        if (fileOriginHystemDat.equals(destinationHystemDat)) {
            throw new IOException("Overriding original hystem file is not allowed @" + fileOriginHystemDat.getAbsolutePath());
        }

// Manholes are sewer-surface file content
        FileReader fr = new FileReader(fileOriginHystemDat);
        BufferedReader br = new BufferedReader(fr);
        FileWriter fw = new FileWriter(destinationHystemDat);
        BufferedWriter bw = new BufferedWriter(fw);

        while (br.ready()) {
            String line = br.readLine();
            String[] parts = line.split(" ");
            //Find index of triangle start
            if (outputEmpty) {
                bw.write(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    int oldID = Integer.parseInt(parts[i]);
                    int newID = triangleOldToNewReference[oldID];
                    if (newID < 0) {
                        continue;
                    }
                    bw.write(" " + newID);
                }
                bw.newLine();
            } else {
                //Only write line if there is at least one triangle representing this area.
                StringBuilder str = new StringBuilder(100);
                str.append(parts[0]);
                int counter = 0;
                for (int i = 1; i < parts.length; i++) {
                    int oldID = Integer.parseInt(parts[i]);
                    int newID = triangleOldToNewReference[oldID];
                    if (newID < 0) {
                        continue;
                    }
                    counter++;
                    str.append(" ").append(newID);
                }
                if (counter > 0) {
                    bw.write(str.toString());
                    bw.newLine();
                }
            }

            bw.flush();
        }
        bw.close();
        fw.close();
        br.close();
        fr.close();
        System.out.println("Wrote new Hystem.dat Area information to " + destinationHystemDat.getAbsolutePath());

    }

    public void writeTrimmedCityXML(File targetCityXML, Collection<String> usedManholes, Collection<String> usedInlets) throws IOException {
        if (targetCityXML.exists()) {
            throw new IOException("File " + targetCityXML.getAbsolutePath() + " already exists.");
        }
        File origin = new File(originDirectory, "city.xml");

        FileReader fr = new FileReader(origin);
        BufferedReader br = new BufferedReader(fr);
        FileWriter fw = new FileWriter(targetCityXML);
        BufferedWriter bw = new BufferedWriter(fw);
        //skip header
        fw.write(br.readLine());
        int counterThrownOutTriangles = 0;
        int status = 0;
        while (br.ready()) {
            String line = br.readLine();
            if (status == 0) {
                if (line.contains("<Manholes>")) {
                    status = 1;
                } else if (line.contains("<Inlets_risosurf>")) {
                    status = 2;
                }
                bw.write(line);
                bw.newLine();
            } else if (status == 1) {
                if (line.contains("</Manholes>")) {
                    status = 0;
                    bw.write(line);
                    bw.newLine();
                } else {
                    //get manhole name
                    int pos = line.indexOf("name=") + 6;
                    int posE = line.indexOf("\"", pos + 1);
                    String name = line.substring(pos, posE);
                    if (usedManholes.contains(name)) {
                        bw.write(line);
                        bw.newLine();
                    } else {
                        //Skip this Manhole. Its position was cut out with the surface.
                    }
                }
            } else if (status == 2) {
                if (line.contains("</Inlets_risosurf>")) {
                    status = 0;
                    bw.write(line);
                    bw.newLine();
                } else {
                    //get inlet name
                    int pos = line.indexOf("name=") + 6;
                    int posE = line.indexOf("\"", pos + 1);
                    String name = line.substring(pos, posE);
                    if (usedInlets.contains(name)) {

                        //is neares manhole included?
                        pos = line.indexOf("nearestmanhole=") + 16;
                        posE = line.indexOf("\"", pos + 1);
                        name = line.substring(pos, posE);
                        if (usedManholes.contains(name)) {
                            bw.write(line);
                            bw.newLine();
                        }
                    } else {
                        //Skip this Manhole. Its position was cut out with the surface.
                    }
                }
            }
            bw.flush();
        }
        bw.flush();
        bw.close();
        br.close();
        fw.close();
        fr.close();
        System.out.println("Trimmed Manholes and Imnlets written to " + targetCityXML.getAbsolutePath());
    }

    public void writeTrimmedResult2Dgdb(File targetDirectory) throws FileNotFoundException, IOException {
//Copy Result2D.gdb
        File originDirectory = new File(this.originDirectory, "Result2D.gdb");
        if (originDirectory.equals(targetDirectory)) {
            throw new IOException("Overriding the original GDB is not allowed");
        }
        copyDirectory(originDirectory, targetDirectory);

        GeoDB db = FileGDBFactory.open(targetDirectory.toPath());
        GeoRndomAccessFileBuffer_RW.verbose = true;

//        for (String layer : db.getLayers()) {
//            System.out.println("gdb layer: " + layer);
//        }
        GeoTable table = new GeoTable((GeoTable) db.layer("Topo_decimated"), true);
        table.open();
        int indexID = table.getFieldId("ID");
        int indexShape = table.getFieldId("shape");

//        //547617, 5799911), new Coordinate(548998, 5799788), new Coordinate(549013, 5799471), new Coordinate(547502, 5799471), new Coordinate(547617, 5799911
//        java.awt.Polygon poly = new java.awt.Polygon(new int[]{547617, 548998, 549013, 547617}, new int[]{5799911, 5799788, 5799471, 5799911}, 4);
//       
//        List<GeoField> fids = table.getFields();
//        for (int i = 0; i < fids.size(); i++) {
//            System.out.println(i + ": " + fids.get(i).getName() + " " + fids.get(i).getType());
//        }
//        System.out.println("featureIDField: " + table.getFeatureIdField());
        int skipcounter = 0;
        for (GeoFeature f : table) {
            try {
                int triangleOldID = f.getValue(indexID).intValue();
                int triangleNewID = trianglesOldNewReference[triangleOldID];
                if (triangleNewID < 0) {
                    skipcounter++;
                }

                //Skip the trimmed out triangles. replace them with the used ones.
                if (triangleNewID >= 0) {
                    long oldid = table.writeFeatureValueInt32(f.getFeatureId(), indexID, triangleNewID);

                    //featureid is always one higher than triangle id.
                    table.moveFeature(f.getFeatureId(), triangleNewID + 1);
//                    table.writeFeatureValuePolygon(triangleNewID + 1, indexShape, poly);

                    if (triangleNewID == numberNewTriagles - 1) {
                        //Last triangle was written. Now cut off the file here
                        System.out.println("set last feature: " + (triangleNewID + 1) + " was old:" + oldid);
                        table.setLastFeature(triangleNewID + 1);
                        table.close();
                        db.close();
                        return;
                    }
                } else {

                }
                if (triangleOldID % 100000 == 0) {
                    System.out.println(triangleOldID + " / " + trianglesOldNewReference.length);
                }
            } catch (IOException ex) {
                Logger.getLogger(SurfaceEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        table.close();
        db.close();
        System.out.println("changed " + skipcounter + " triangles in " + targetDirectory + " to id=-1.");
    }

    private double[][] trimTriangleMids(double[][] originalTriangleMids, int countNewTriangles) {
        double[][] newTriangleMids = new double[countNewTriangles][3];
        for (int i = 0; i < trianglesOldNewReference.length; i++) {
            if (trianglesOldNewReference[i] < 0) {
                continue;
            }
            newTriangleMids[trianglesOldNewReference[i]] = originalTriangleMids[i];
        }
        return newTriangleMids;
    }

    public Surface getSurface() {
        return surf;
    }

    public void writeAllFilesTo(File targetDirectory, boolean overrideExisting) throws IOException {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdir();
        }
        File x_dat = new File(targetDirectory, "X.dat");
        File trimod1_dat = new File(targetDirectory, "TRIMOD1.dat");
        File trimod2_dat = new File(targetDirectory, "TRIMOD2.dat");

        File mesh_ply = new File(targetDirectory, "mesh.ply");
        File zTriang_dat = new File(targetDirectory, "Ztriang.dat");
        File decim_dat = new File(targetDirectory, "decim.dat");
        File hystem_dat = new File(targetDirectory, "hystem.dat");
        File triboundary_dat = new File(targetDirectory, "TRIBOUNDARY.dat");
        File sewer_surf_nodes_dat = new File(targetDirectory, "SEWER-SURF_NODES.dat");
        File surf_sewer_nodes_dat = new File(targetDirectory, "SURF-SEWER_NODES.dat");
        File hydromodel_dat_trg = new File(targetDirectory, "HYDROMODEL.dat");

        File city_xml_trg = new File(targetDirectory, "city.xml");

        File resul2d_gdb_trg = new File(targetDirectory, "Result2D.gdb");

        if (overrideExisting) {
            //Delete exisitng files
            if (x_dat.exists()) {
                x_dat.delete();
            }
            if (trimod1_dat.exists()) {
                trimod1_dat.delete();
            }
            if (trimod2_dat.exists()) {
                trimod2_dat.delete();
            }
            if (mesh_ply.exists()) {
                mesh_ply.delete();
            }
            if (zTriang_dat.exists()) {
                zTriang_dat.delete();
            }
            if (decim_dat.exists()) {
                decim_dat.delete();
            }
            if (hystem_dat.exists()) {
                hystem_dat.delete();
            }
            if (hydromodel_dat_trg.exists()) {
                hydromodel_dat_trg.delete();
            }
            if (triboundary_dat.exists()) {
                triboundary_dat.delete();
            }
            if (sewer_surf_nodes_dat.exists()) {
                sewer_surf_nodes_dat.delete();
            }
            if (surf_sewer_nodes_dat.exists()) {
                surf_sewer_nodes_dat.delete();
            }

            if (city_xml_trg.exists()) {
                city_xml_trg.delete();
            }

        }

        HE_SurfaceIO.writeSurfaceFiles(surf, (targetDirectory));
        HE_SurfaceIO.writeTriangleZ(surf.getTriangleMids(), zTriang_dat);
        HE_SurfaceIO.writeMeshPly(surf, mesh_ply);
        HE_SurfaceIO.writeDecimDat(surf.triangleNodes.length, decim_dat);
        HE_SurfaceIO.writeTRIBOUNDARYdat(surf.triangleNodes.length, triboundary_dat);
        LinkedList<String> usedmanholes = writeTrimmedManholes(sewer_surf_nodes_dat);
        LinkedList<String> usedInlets = writeTrimmedInlets(surf_sewer_nodes_dat);
        writeTrimmedHYDROMODELdat(hydromodel_dat_trg, usedmanholes);
        writeTrimmedCityXML(city_xml_trg, usedmanholes, usedInlets);
        writeTrimmedHystemDat(hystem_dat, trianglesOldNewReference, false);
        writeTrimmedResult2Dgdb(resul2d_gdb_trg);
        //Copy other files
        File boundary_xy_org = new File(originDirectory, "boundary.xy");
        File boundary_xy_trg = new File(targetDirectory, boundary_xy_org.getName());
        if (boundary_xy_trg.exists()) {
            boundary_xy_trg.delete();
        }
        copyFileUsingStream(boundary_xy_org, boundary_xy_trg);

        File config_xml_org = new File(originDirectory, "config.xml");
        File config_xml_trg = new File(targetDirectory, config_xml_org.getName());
        if (config_xml_trg.exists()) {
            config_xml_trg.delete();
        }
        copyFileUsingStream(config_xml_org, config_xml_trg);

        File polyg_xml_org = new File(originDirectory, "polyg.xml");
        File polyg_xml_trg = new File(targetDirectory, polyg_xml_org.getName());
        if (polyg_xml_trg.exists()) {
            polyg_xml_trg.delete();
        }
        copyFileUsingStream(polyg_xml_org, polyg_xml_trg);

        File table_xml_org = new File(originDirectory, "table.xml");
        File table_xml_trg = new File(targetDirectory, table_xml_org.getName());
        if (table_xml_trg.exists()) {
            table_xml_trg.delete();
        }
        copyFileUsingStream(table_xml_org, table_xml_trg);

    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static void copyDirectory(File source, File target) throws FileNotFoundException {
        if (!source.exists()) {
            throw new FileNotFoundException("Could not find source " + source);
        }
        if (!target.exists()) {
            target.mkdirs();
        }
        //override exisitng files in new directory
        for (File sourceFile : source.listFiles()) {
            File targetfile = new File(target, sourceFile.getName());
            try {
                copyFileUsingStream(sourceFile, targetfile);
            } catch (IOException ex) {
                Logger.getLogger(SurfaceEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String[] args) {
        try {
            String originDirectory = "L:\\EVUS_Hannover_gesamt2DAB\\EVUS_Hannover_gesamt2DAB\\2D_Modell_25m2_0214.model";
            String targetDirectory = "L:\\atestsurfaceOutput\\hannover_Nord2.model";

            GeometryFactory gf = new GeometryFactory();
            Polygon poly = gf.createPolygon(new Coordinate[]{
                new Coordinate(3.254641969758113E7, 5809033.427154528),
                new Coordinate(3.2547282366208993E7, 5809146.438597795),
                new Coordinate(3.254790104537323E7, 5809140.786811013),
                new Coordinate(3.2549468015120894E7, 5808853.580447861),
                new Coordinate(3.2552747843052845E7, 5808881.863388747),
                new Coordinate(3.2554097537339445E7, 5809280.770216975),
                new Coordinate(3.255473340310197E7, 5809305.275646091),
                new Coordinate(3.2555494961687546E7, 5809051.839986324),
                new Coordinate(3.2556670758709297E7, 5808314.177503035),
                new Coordinate(3.2557669227983117E7, 5807295.0483763),
                new Coordinate(3.255740306805364E7, 5807088.090171556),
                new Coordinate(3.2556850069083523E7, 5806941.8830954265),
                new Coordinate(3.2556308181680184E7, 5806842.458153253),
                new Coordinate(3.255591919854634E7, 5806645.808600426),
                new Coordinate(3.2555538456564E7, 5806233.749078615),
                new Coordinate(3.2554947346528944E7, 5806349.437842925),
                new Coordinate(3.2554292167705886E7, 5806452.836779003),
                new Coordinate(3.2553800945983708E7, 5806517.337322566),
                new Coordinate(3.255314688604261E7, 5806521.8762810705),
                new Coordinate(3.255266264921056E7, 5806481.719346483),
                new Coordinate(3.2551910313405313E7, 5806392.174589416),
                new Coordinate(3.2551566188737612E7, 5806353.605733549),
                new Coordinate(3.2551303500171043E7, 5806345.034067374),
                new Coordinate(3.255080641312392E7, 5806415.5978136705),
                new Coordinate(3.2549979059420574E7, 5806809.016009482),
                new Coordinate(3.254870504873084E7, 5807512.570745765),
                new Coordinate(3.2547836116511337E7, 5808016.476144717),
                new Coordinate(3.2547384320305075E7, 5808256.65333762),
                new Coordinate(3.2546869701643627E7, 5808356.524233778),
                new Coordinate(3.254641969758113E7, 5809033.427154528)
            });

            SurfaceEditor editor = new SurfaceEditor(originDirectory);
            editor.removeTrianglesOutside(poly);
            editor.trimSurface();
            editor.writeAllFilesTo(new File(targetDirectory), true);
//            HE_SurfaceIO.writeSurfaceFiles(editor.surf, new File(targetDirectory));
//            HE_SurfaceIO.writeTriangleZ(editor.surf.getTriangleMids(), new File(targetDirectory, "Ztriang.dat"));
//            HE_SurfaceIO.writeMeshPly(editor.surf, new File(targetDirectory, "mesh.ply"));
//            HE_SurfaceIO.writeDecimDat(editor.surf.vertices.length, new File(targetDirectory, "decim.dat"));
//            HE_SurfaceIO.writeTRIBOUNDARYdat(editor.surf.triangleNodes.length, new File(targetDirectory, "TRIBOUNDARY.dat"));
//            editor.writeTrimmedManholes(new File(originDirectory, "SEWER-SURF_NODES.dat"), new File(originDirectory, "SURF-SEWER_NODES.dat"), new File(targetDirectory));
//            editor.writeTrimmedHystemDat(new File(originDirectory, "hystem.dat"), new File(targetDirectory, "hystem.dat"), editor.trianglesOldNewReference);
        } catch (IOException ex) {
            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

//    public static void main1(String[] args) {
//        try {
//            String originDirectory = "L:\\GULLI_Input\\Modell2017Mai\\2D_Model\\2DModell_10cm_3m2_ohne_BK.model";
//            String targetDirectory = "L:\\atestsurfaceOutput\\test2.model";
//
//            GeometryFactory gf = new GeometryFactory();
//            Polygon poly = gf.createPolygon(new Coordinate[]{new Coordinate(547617, 5799911), new Coordinate(548998, 5799788), new Coordinate(549013, 5799471), new Coordinate(547502, 5799471), new Coordinate(547617, 5799911)});
//
//            SurfaceEditor editor = new SurfaceEditor(originDirectory);
//            editor.removeTrianglesInside(poly);
//            editor.trimSurface();
//            editor.writeAllFilesTo( new File(targetDirectory), true);
////            HE_SurfaceIO.writeSurfaceFiles(editor.surf, new File(targetDirectory));
////            HE_SurfaceIO.writeTriangleZ(editor.surf.getTriangleMids(), new File(targetDirectory, "Ztriang.dat"));
////            HE_SurfaceIO.writeMeshPly(editor.surf, new File(targetDirectory, "mesh.ply"));
////            HE_SurfaceIO.writeDecimDat(editor.surf.vertices.length, new File(targetDirectory, "decim.dat"));
////            HE_SurfaceIO.writeTRIBOUNDARYdat(editor.surf.triangleNodes.length, new File(targetDirectory, "TRIBOUNDARY.dat"));
////            editor.writeTrimmedManholes(new File(originDirectory, "SEWER-SURF_NODES.dat"), new File(originDirectory, "SURF-SEWER_NODES.dat"), new File(targetDirectory));
////            editor.writeTrimmedHystemDat(new File(originDirectory, "hystem.dat"), new File(targetDirectory, "hystem.dat"), editor.trianglesOldNewReference);
//        } catch (IOException ex) {
//            Logger.getLogger(HE_SurfaceIO.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
}
