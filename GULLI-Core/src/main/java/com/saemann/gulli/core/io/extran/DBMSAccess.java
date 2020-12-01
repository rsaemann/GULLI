package com.saemann.gulli.core.io.extran;

import com.saemann.gulli.core.control.StartParameters;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import com.saemann.gulli.core.model.GeoTools;
import com.saemann.gulli.core.model.topology.Capacity;
import com.saemann.gulli.core.model.topology.Connection_Manhole_Pipe;
import com.saemann.gulli.core.model.topology.Manhole;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Pipe;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.core.model.topology.profile.CircularProfile;
import com.saemann.gulli.core.model.topology.profile.Profile;
import org.locationtech.jts.geom.Coordinate;

/**
 *
 * @author saemann
 */
public class DBMSAccess {
    
    public static Network loadNetwork(File dbfile, GeoTools geoTools) throws SQLException, ClassNotFoundException, Exception {
        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        Connection con = DriverManager.getConnection("jdbc:ucanaccess://" + dbfile.getAbsolutePath());

        //Koordinatentransformation vorbereiten
        //Initialize Geotools to convert from Gauss-Krueger-Coordinates to 
        // global Position WGS84 system
        GeoTools gt = geoTools;
        if (gt == null) {
            gt = new GeoTools("EPSG:4326", "EPSG:31467",StartParameters.JTS_WGS84_LONGITUDE_FIRST);// WGS84(lat,lon) <-> Gauss Krüger Zone 3(North,East)
        }
        //Initialisiere die Speicherobjekte für die fertigen Kanalobjekte
        HashMap<String, Manhole> smap = new HashMap<>();
        HashSet<Pipe> pipes_drain = new HashSet<>();
        HashSet<Pipe> pipes_sewer = new HashSet<>();
        HashMap<Integer, Profile> schachtprofile = new HashMap<>();

        //////////##############################################################
        // Beginne die Abfrage der Schachtelemente
        /////////
        Statement st = con.createStatement();
        ResultSet res;// = st.executeQuery("SELECT MSysObjects.Name, MSysObjects.Type FROM MSysObjects WHERE MSysObjects.Name Not Like \"MsyS*\" AND MSysObjects.Type=1 ORDER BY MSysObjects.Name;") ;
        res = st.executeQuery("SELECT [name],[x],[y],[schachtdurchmesser],[geländehöhe],[deckelhöhe],[sohlhöhe] from [schacht];");
        
        int c = res.getMetaData().getColumnCount();
        
        while (res.next()) {
//            StringBuilder str = new StringBuilder();
//            for (int i = 1; i <= c; i++) {
//                str.append(res.getString(i)).append(",\t");
//            }
//            str.append('\n');
//            System.out.println(str.toString());
            String name = res.getString(1);
            
            double x = res.getDouble(2);
            double y = res.getDouble(3);
            Coordinate coord = new Coordinate(y, x);
            coord = gt.toGlobal(coord);
            Position position = new Position(coord.y, coord.x, x, y);
            int durchmesser = res.getInt(4);
            Profile p = schachtprofile.get(durchmesser);
            if (p == null) {
                p = new CircularProfile(durchmesser / 1000.);
                schachtprofile.put(durchmesser, p);
            }
            Manhole m = new Manhole(position, name, p);
            m.setSurface_height(res.getFloat(5));
            m.setTop_height(res.getFloat(6));
            m.setSole_height(res.getFloat(7));
            smap.put(name, m);
        }
        res.close();
        // ende Schacht abfrage

        /////*#################################################################
        ////   Haltungen
        ////
        res = st.executeQuery("SELECT [name],[schacht oben],[schacht unten],[sohle oben],[sohle unten],[länge],[kanalart],[profiltyp],[profilbreite],[rauhigkeitsbeiwert] from [haltung];");
        c = res.getMetaData().getColumnCount();
        
        while (res.next()) {
//            StringBuilder str = new StringBuilder();
//            for (int i = 1; i <= c; i++) {
//                str.append(res.getString(i)).append(",\t");
//            }
//            str.append('\n');
//            System.out.println(str.toString());
            String name = res.getString(1);
            String nameoben = res.getString(2);
            String nameunten = res.getString(3);
            Manhole mhoben = smap.get(nameoben);
            Manhole mhunten = smap.get(nameunten);
            if (mhoben == null) {
                System.err.println("Can not find upper manhole '" + nameoben + "' for pipe " + name);
                continue;
            }
            if (mhunten == null) {
                System.err.println("Can not find lower manhole '" + nameunten + "' for pipe " + name);
                continue;
            }
            
            int durchmesser = res.getInt(9);
            Profile p = schachtprofile.get(durchmesser);
            if (p == null) {
                p = new CircularProfile(durchmesser / 1000.);
                schachtprofile.put(durchmesser, p);
            }
            //Verbindungen anlegen
            //Define the Position of Connections
            Connection_Manhole_Pipe upper, lower;
                upper = new Connection_Manhole_Pipe(mhoben.getPosition(), res.getFloat(4));
                lower = new Connection_Manhole_Pipe(mhunten.getPosition(), res.getFloat(5));

            
            Pipe pipe = new Pipe(upper, lower, p);
            pipe.setName(name);
           
            mhoben.addConnection(upper);
           
            mhunten.addConnection(lower);
            // fertig Connections zwischen Schacht und haltung eingefügt    

            String watertype = res.getString(7);
            Capacity.SEWER_TYPE type = Capacity.SEWER_TYPE.UNKNOWN;

            pipe.setWaterType(type);
            pipe.setLength(res.getFloat(6));
//            pipe.setRoughness_k(res.getFloat(10));
            
            pipes_sewer.add(pipe);
        }
        res.close();
        pipes_drain.addAll(pipes_sewer);
        return new Network(pipes_drain, new HashSet<>(smap.values()));
    }
    
}
