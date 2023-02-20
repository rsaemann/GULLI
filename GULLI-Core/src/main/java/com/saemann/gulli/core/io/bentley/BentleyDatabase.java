/*
 * The MIT License
 *
 * Copyright 2023 robert.
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
package com.saemann.gulli.core.io.bentley;

import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.gulli.core.model.topology.profile.Profile;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.sqlite.SQLiteConfig;

/**
 * Bentley Sewer Gems Database wrapper class to read the SQLite file.
 *
 * @author robert
 */
public class BentleyDatabase {

    private File sqliteFile;

    private Connection con;

    public BentleyDatabase(File sqliteFile) {
        this.sqliteFile = sqliteFile;
    }

    public BentleyDatabase(String sqlitepath) {
        this(new File(sqlitepath));
    }

    public static Connection openConnection(String filePath) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.setEncoding(SQLiteConfig.Encoding.UTF8);
        return DriverManager.getConnection("jdbc:sqlite:" + filePath, config.toProperties());
    }

    public Network loadNetwork(String epsgCode) throws ClassNotFoundException, SQLException, FactoryException, TransformException {
        con = openConnection(sqliteFile.getAbsolutePath());
        Statement st = con.createStatement();
        //Read number of manholes
        ResultSet rs = st.executeQuery("SELECT COUNT(DomainElementID) FROM Manhole");
        int numberOfManholes = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfManholes = rs.getInt(1);
        }
        rs.close();
        rs = st.executeQuery("SELECT COUNT(DomainElementID) FROM OUTFALL");
        int numberOfOutlets = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfOutlets = rs.getInt(1);
        }
        rs.close();
        rs = st.executeQuery("SELECT COUNT(DomainElementID) FROM PressureSystemNode");
        int numberOfStorageChambers = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfStorageChambers = rs.getInt(1);
        }
        rs.close();
        rs = st.executeQuery("SELECT COUNT(DomainElementID) FROM StandardPump");
        int numberOfPumpNodes = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfPumpNodes = rs.getInt(1);
        }
        rs.close();
        HashMap<Integer, Manhole> mapManholes = new HashMap<>(numberOfManholes + numberOfOutlets + numberOfStorageChambers+numberOfPumpNodes);
        //Read Manhole IDs
//        int[] manholeIDs = new int[numberOfManholes];
//        rs = st.executeQuery("SELECT DomainElementID FROM Manhole");
//        int i = 0;
//        while (rs.next()) {
//            manholeIDs[i] = rs.getInt(1);
//            i++;
//        }
//        rs.close();
        //Read ManholeCoordinates
        Profile profile = new CircularProfile(1);
        GeoTools gt = new GeoTools("EPSG:4326", epsgCode);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[21]);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        //Manholes
        rs = st.executeQuery("SELECT Manhole.DomainElementID,HMIGeometry,Label FROM BaseNode_HmiDataSetGeometry_Data,HMIModelingElement, MANHOLE WHERE BaseNode_HmiDataSetGeometry_Data.DomainElementID=Manhole.DomainElementID AND Manhole.DomainElementID=HMIModelingElement.ElementID ");
        while (rs.next()) {
            int id = rs.getInt(1);
            buffer.clear();
            buffer.put(rs.getBytes(2));
//            System.out.println("  " + id + " '" + rs.getString(3) + "'   : " + rs.getString(2) + "  ### " + buffer.getInt(1) + ": " + buffer.getDouble(5) * 0.3048 + ", " + buffer.getDouble(13) * 0.3048);
            double x = buffer.getDouble(5) * 0.3048;
            double y = buffer.getDouble(13) * 0.3048;
            String name = rs.getString(3);
            Coordinate global = gt.toGlobal(new CoordinateXY(x, y), true);
            Position pos = new Position(global.x, global.y, x, y);
            Manhole mh = new Manhole(pos, name, profile);
            mapManholes.put(id, mh);
        }
        rs.close();

        //Outlets
        rs = st.executeQuery("SELECT OUTFALL.DomainElementID,HMIGeometry,Label FROM BaseNode_HmiDataSetGeometry_Data,HMIModelingElement, OUTFALL WHERE BaseNode_HmiDataSetGeometry_Data.DomainElementID=OUTFALL.DomainElementID AND OUTFALL.DomainElementID=HMIModelingElement.ElementID ");
        while (rs.next()) {
            int id = rs.getInt(1);
            buffer.clear();
            buffer.put(rs.getBytes(2));
//            System.out.println("  " + id + " '" + rs.getString(3) + "'   : " + rs.getString(2) + "  ### " + buffer.getInt(1) + ": " + buffer.getDouble(5) * 0.3048 + ", " + buffer.getDouble(13) * 0.3048);
            double x = buffer.getDouble(5) * 0.3048;
            double y = buffer.getDouble(13) * 0.3048;
            String name = rs.getString(3);
            Coordinate global = gt.toGlobal(new CoordinateXY(x, y), true);
            Position pos = new Position(global.x, global.y, x, y);
            Manhole mh = new Manhole(pos, name, profile);
            mh.setAsOutlet(true);
            mapManholes.put(id, mh);
        }
        rs.close();

        //StorageChambers
        rs = st.executeQuery("SELECT PressureSystemNode.DomainElementID,HMIGeometry,Label FROM BaseNode_HmiDataSetGeometry_Data,HMIModelingElement, PressureSystemNode WHERE BaseNode_HmiDataSetGeometry_Data.DomainElementID=PressureSystemNode.DomainElementID AND PressureSystemNode.DomainElementID=HMIModelingElement.ElementID ");
        while (rs.next()) {
            int id = rs.getInt(1);
            buffer.clear();
            buffer.put(rs.getBytes(2));
//            System.out.println("  " + id + " '" + rs.getString(3) + "'   : " + rs.getString(2) + "  ### " + buffer.getInt(1) + ": " + buffer.getDouble(5) * 0.3048 + ", " + buffer.getDouble(13) * 0.3048);
            double x = buffer.getDouble(5) * 0.3048;
            double y = buffer.getDouble(13) * 0.3048;
            String name = rs.getString(3);
            Coordinate global = gt.toGlobal(new CoordinateXY(x, y), true);
            Position pos = new Position(global.x, global.y, x, y);
            Manhole mh = new Manhole(pos, name, profile);
            mh.setAsOutlet(false);
            mapManholes.put(id, mh);
        }
        rs.close();
        
        //PumpNodes from StandardPumps
        rs = st.executeQuery("SELECT StandardPump.DomainElementID,HMIGeometry,Label FROM BaseDirectedNode_HmiDataSetGeometry_Data,HMIModelingElement, StandardPump WHERE BaseDirectedNode_HmiDataSetGeometry_Data.DomainElementID=StandardPump.DomainElementID AND StandardPump.DomainElementID=HMIModelingElement.ElementID ");
        while (rs.next()) {
            int id = rs.getInt(1);
            buffer.clear();
            buffer.put(rs.getBytes(2));
//            System.out.println("  " + id + " '" + rs.getString(3) + "'   : " + rs.getString(2) + "  ### " + buffer.getInt(1) + ": " + buffer.getDouble(5) * 0.3048 + ", " + buffer.getDouble(13) * 0.3048);
            double x = buffer.getDouble(5) * 0.3048;
            double y = buffer.getDouble(13) * 0.3048;
            String name = rs.getString(3);
            Coordinate global = gt.toGlobal(new CoordinateXY(x, y), true);
            Position pos = new Position(global.x, global.y, x, y);
            Manhole mh = new Manhole(pos, name, profile);
            mh.setAsOutlet(false);
            mapManholes.put(id, mh);
        }
        rs.close();

        ////////////////////////////////////////////////////////////////////////
        ///// Pipes
        //Number of pipes
        rs = st.executeQuery("SELECT COUNT (*) FROM CONDUIT");
        int numberOfPipes = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfPipes = rs.getInt(1);
        }
        rs.close();

        rs = st.executeQuery("SELECT COUNT (*) FROM PressurePipe");
        int numberOfPressurePipes = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfPressurePipes = rs.getInt(1);
        }
        rs.close();

        rs = st.executeQuery("SELECT COUNT (*) FROM BasePump");
        int numberOfBasePumps = 0;
        if (rs.isBeforeFirst()) {
            rs.next();
            numberOfBasePumps = rs.getInt(1);
        }
        rs.close();

        HashMap<Integer, Pipe> mapPipes = new HashMap<>(numberOfPipes + numberOfPressurePipes + numberOfBasePumps);
        int counterSuccess = 0, counterFail = 0;

        //Conduits
        rs = st.executeQuery("SELECT Conduit.DomainElementID, HMITopologyStartNodeID, HMITopologyStopNodeID,Label FROM CONDUIT,BaseLink_HMIDataSetTopology_Data, HMIModelingElement WHERE CONDUIT.DomainElementID=BaseLink_HMIDataSetTopology_Data.DomainElementID AND HMIModelingElement.ElementID=CONDUIT.DomainElementID");

        while (rs.next()) {
            int pipeID = rs.getInt(1);
            int startNode = rs.getInt(2);
            int endNode = rs.getInt(3);
            String name = rs.getString(4);
            Manhole mh_start = mapManholes.get(startNode);
            Manhole mh_end = mapManholes.get(endNode);
            if (mh_start == null || mh_end == null) {
                System.out.println("Manholes for Pipe " + name + " could not be found: " + startNode + "=" + mh_start + "\t " + endNode + "=" + mh_end);
                counterFail++;
                continue;
            } else {
//                System.out.println("Found both for "+name);
                counterSuccess++;
            }
            Connection_Manhole_Pipe startCon = new Connection_Manhole_Pipe(mh_start, 0);
            Connection_Manhole_Pipe endCon = new Connection_Manhole_Pipe(mh_end, 0);

            Pipe pipe = new Pipe(startCon, endCon, profile);
            pipe.setManualID(pipeID);
            pipe.setName(name);
            mapPipes.put(pipeID, pipe);
        }
        
        //PressurePipe
        rs = st.executeQuery("SELECT PressurePipe.DomainElementID, HMITopologyStartNodeID, HMITopologyStopNodeID,Label FROM PressurePipe,BaseLink_HMIDataSetTopology_Data, HMIModelingElement WHERE PressurePipe.DomainElementID=BaseLink_HMIDataSetTopology_Data.DomainElementID AND HMIModelingElement.ElementID=PressurePipe.DomainElementID");

        while (rs.next()) {
            int pipeID = rs.getInt(1);
            int startNode = rs.getInt(2);
            int endNode = rs.getInt(3);
            String name = rs.getString(4);
            Manhole mh_start = mapManholes.get(startNode);
            Manhole mh_end = mapManholes.get(endNode);
            if (mh_start == null || mh_end == null) {
                System.out.println("Manholes for PressurePipe " + name + " could not be found: " + startNode + "=" + mh_start + "\t " + endNode + "=" + mh_end);
                counterFail++;
                continue;
            } else {
//                System.out.println("Found both for "+name);
                counterSuccess++;
            }
            Connection_Manhole_Pipe startCon = new Connection_Manhole_Pipe(mh_start, 0);
            Connection_Manhole_Pipe endCon = new Connection_Manhole_Pipe(mh_end, 0);

            Pipe pipe = new Pipe(startCon, endCon, profile);
            pipe.setManualID(pipeID);
            pipe.setName(name);
            mapPipes.put(pipeID, pipe);
        }
        
//        //StandardPump
//        rs = st.executeQuery("SELECT StandardPump.DomainElementID, HMITopologyDownstreamLinkID, HMIGeometry,Label "
//                + "FROM StandardPump,BaseDirectedNode_HMIDataSetTopology_Data,BaseDirectedNode_HmiDataSetGeometry_Data, HMIModelingElement "
//                + "WHERE StandardPump.DomainElementID=BaseDirectedNode_HMIDataSetTopology_Data.DomainElementID "
//                + "AND StandardPump.DomainElementID=BaseDirectedNode_HmiDataSetGeometry_Data.DomainElementID "
//                + "AND HMIModelingElement.ElementID=StandardPump.DomainElementID");
//        while (rs.next()) {
//            byte[] bytes=rs.getBytes(3);
//            System.out.println("Bytes length in BAsedirectedode: "+bytes.length);
//            int pipeID = rs.getInt(1);
//            int startNode = rs.getInt(2);
//            int endNode = rs.getInt(3);
//            String name = rs.getString(4);
//            Manhole mh_start = mapManholes.get(startNode);
//            Manhole mh_end = mapManholes.get(endNode);
//            if (mh_start == null || mh_end == null) {
//                System.out.println("Manholes for BasePump " + name + " could not be found: " + startNode + "=" + mh_start + "\t " + endNode + "=" + mh_end);
//                counterFail++;
//                continue;
//            } else {
////                System.out.println("Found both for "+name);
//                counterSuccess++;
//            }
//            Connection_Manhole_Pipe startCon = new Connection_Manhole_Pipe(mh_start, 0);
//            Connection_Manhole_Pipe endCon = new Connection_Manhole_Pipe(mh_end, 0);
//
//            Pipe pipe = new Pipe(startCon, endCon, profile);
//            pipe.setManualID(pipeID);
//            pipe.setName(name);
//            mapPipes.put(pipeID, pipe);
//        }
        
        System.out.println("Pipes created: " + counterSuccess + ",  Fails: " + counterFail);
        st.close();
        con.close();

        Network network = new Network(mapPipes.values(), mapManholes.values());
        return network;
    }

    public static void main(String[] args) {
        File bentleyFile = new File("C:\\Users\\robert\\Desktop\\Model Zlewni Oczyszczalni Czajka etap 2a.stsw.sqlite");
        BentleyDatabase bentleyDB = new BentleyDatabase(bentleyFile);
        try {
            bentleyDB.loadNetwork("EPSG:2178");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BentleyDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(BentleyDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(BentleyDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformException ex) {
            Logger.getLogger(BentleyDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
