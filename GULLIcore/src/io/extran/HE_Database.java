package io.extran;

import com.vividsolutions.jts.geom.Coordinate;
import control.scenario.injection.HEInjectionInformation;
import io.SHP_IO_GULLI;
import io.SparseTimeLineDataProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.Material;
import model.timeline.array.ArrayTimeLineManhole;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLinePipe;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.timeline.sparse.SparseTimeLineManholeContainer;
import model.timeline.sparse.SparseTimeLinePipeContainer;
import model.timeline.sparse.SparseTimelineManhole;
import model.timeline.sparse.SparseTimelinePipe;
import model.topology.Capacity;
import model.topology.Manhole;
import model.topology.Network;
import model.topology.Pipe;
import model.topology.Position;
import model.topology.StorageVolume;
import model.topology.graph.Pair;
import model.topology.profile.CircularProfile;
import model.topology.profile.Profile;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.sqlite.SQLiteConfig;

/**
 * Class for Reading the ITWH idbf Firebird / SQLite database file.
 *
 * @author saemann
 */
public class HE_Database implements SparseTimeLineDataProvider {

    public static boolean verbose = false;

    public static boolean orientPipesGravityFlown = true;

    public static boolean keepSaemSizeFile = true;

    public static final boolean registered = register();

    public static boolean markCopiedFilesToDeleteAtProgramEnd = true;

    /**
     * Where to save the local copied database files when they were on a remote
     * drive.
     */
    public static File workingdirectory = null;

    /**
     * If false the localCopyName will be used when creating local copy of a
     * remote file.
     */
    public static boolean useOriginalFilenames = true;

    public static String localCopyName = "idbfCopy.idbf";

    /**
     * local working file of database.
     */
    private File localFile = null;

    /**
     * original reference to the database file.
     */
    private File databaseFile = null;

    protected Connection con;

    protected boolean readOnly = false;

    private java.util.Properties connectionProperties;

    protected boolean isSQLite = false;

    private final String synchronizationObject = "sync";

    /**
     * Can parse text based timestamps in sqlite format.
     */
    protected DateFormat sqliteDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static boolean register() {
        try {
            Class.forName("org.sqlite.JDBC");
            Class.forName("org.firebirdsql.jdbc.FBDriver").newInstance();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Firebird driver could not been registered.");
            return false;
        } catch (InstantiationException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    public HE_Database(File databaseFile, boolean readonly) throws IOException {

        String name = databaseFile.getAbsolutePath().toLowerCase();
        if (name.endsWith(".idbf")) {
            //Open Firebird Database

            //Load JDBC Drivers
            if (!registered) {
                if (!register()) {
                    try {
                        throw new ClassNotFoundException("Firebird Database driver not accessable. Please make sure a file like 'jaybird-full-2.2.9.jar' is in the lib folder.");
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            this.readOnly = readonly;
            //Test if Database file is local file
            // throws error if on remote device

            String url = "jdbc:firebirdsql:embedded:" + databaseFile.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();

            String user = "sysdba";
            String password = "masterkey";
            connectionProperties = new java.util.Properties();
            connectionProperties.put("user", user);
            connectionProperties.put("password", password);
            connectionProperties.put("lc_ctype", "UTF8");

            boolean onLocalDrive = false;
            try {
                con = DriverManager.getConnection(url, connectionProperties);
                con.setReadOnly(this.readOnly);
                onLocalDrive = true;
            } catch (SQLException e) {
                if (e.getErrorCode() == 335544721) {
                    //Can not read databases if they are on network drives.
                    onLocalDrive = false;
                } else {
                    e.printStackTrace();
                }
            }
            this.databaseFile = databaseFile;
            if (onLocalDrive) {
                this.localFile = databaseFile;
            } else {
                if (workingdirectory != null) {
                    //Copy to local working directory
                    if (!workingdirectory.isDirectory()) {
                        workingdirectory = workingdirectory.getParentFile();
                    }
                    if (!workingdirectory.exists()) {
                        workingdirectory.mkdirs();
                    }
                    //Build copy file
                    if (!localCopyName.endsWith(".idbf")) {
                        localCopyName += ".idbf";
                    }
                    File f;
                    if (useOriginalFilenames) {
                        f = new File(workingdirectory, databaseFile.getName());
                    } else {
                        f = new File(workingdirectory, localCopyName);
                    }
                    if (markCopiedFilesToDeleteAtProgramEnd) {
                        f.deleteOnExit();
                    }

                    boolean needToCopy = true;
                    if (f.exists()) {
                        if (keepSaemSizeFile && f.length() == databaseFile.length() && f.lastModified() >= databaseFile.lastModified()) {

                            needToCopy = false;
                        }
                    }

                    if (needToCopy) {
                        //Copy remote to local file.
                        Files.copy(databaseFile.toPath(), f.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                        if (verbose) {
                            System.out.println("Copied file to " + f.getAbsolutePath() + " from " + databaseFile.getAbsolutePath());
                        }
                    } else {
                        if (verbose) {
                            System.out.println("Keep local file " + f.getAbsolutePath());
                        }
                    }
                    this.localFile = f;
                    try {
                        url = "jdbc:firebirdsql:embedded:" + f.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();
                        con = DriverManager.getConnection(url, connectionProperties);
                        con.setReadOnly(this.readOnly);
                        return; //Success
                    } catch (SQLException e) {
                        //Can not read databases if they are on network drives.
                    }
                }
                //If not successfull return until now, try to copy to temporary file
                //Create temporary file
                Path temporalPath = Files.createTempFile(databaseFile.getName().replaceAll(".idbf", ""), ".idbf");//new File(dbfile.getName()).toPath();
                if (temporalPath != null) {
                    Files.copy(databaseFile.toPath(), temporalPath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                    if (verbose) {
                        System.out.println("Create temporary file in " + temporalPath + "\torigin: " + databaseFile.getAbsolutePath());
                    }
                    this.localFile = temporalPath.toFile();
                    //Always delete them, the can not be found either to be used again.
                    this.localFile.deleteOnExit();

                    try {
                        url = "jdbc:firebirdsql:embedded:" + this.localFile.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();
                        con = DriverManager.getConnection(url, connectionProperties);
                        con.setReadOnly(this.readOnly);
                        return;
                    } catch (SQLException e) {
                        throw new IOException("Remote File '" + databaseFile.getAbsolutePath() + "' can not be used directly. Copy to local or temporal file " + this.localFile.getAbsolutePath() + " did not work. url:" + url + "\t" + e.getLocalizedMessage());
                    }
                }
                //Nothing worked out
                throw new IOException("Could not create temporal file for opening database file " + databaseFile.getAbsolutePath());
            }
        } else if (name.endsWith(".idbr") || name.endsWith(".idbm")) {
            isSQLite = true;
            try {
                //SQlite

                SQLiteConfig config = new SQLiteConfig();
                config.setEncoding(SQLiteConfig.Encoding.UTF8);
                con = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), config.toProperties());
                this.databaseFile = databaseFile;
                this.localFile = databaseFile;

            } catch (SQLException ex) {
                Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public void close() throws SQLException {
        if (con != null && !con.isClosed()) {
            con.close();
            con = null;
        }
    }

    /**
     * Returns an open or reactivates a closed connection.
     *
     * @return a Read-only Connection
     * @throws SQLException
     * @throws IOException
     */
    public Connection getConnection() throws SQLException, IOException {
        if (con == null || con.isClosed()) {
            if (isSQLite) {
//                try {
//                    Class.forName("org.sqlite.JDBC");
                SQLiteConfig config = new SQLiteConfig();
                config.setEncoding(SQLiteConfig.Encoding.UTF8);
                config.setReadOnly(true);
                con = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), config.toProperties());
                return con;
//                } catch (ClassNotFoundException ex) {
//                    Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
//                }
            } else {
                //Firebird

                try {
                    String url = "jdbc:firebirdsql:embedded:" + this.localFile.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();
                    con = DriverManager.getConnection(url, connectionProperties);
                    con.setReadOnly(this.readOnly);
                    return con;
                } catch (SQLException e) {
                    throw new IOException("Remote File '" + localFile.getAbsolutePath() + "' can not be used directly. Copy to local or temporal file did not work. " + e.getLocalizedMessage());
                }
            }
        }
        return con;

    }

    /**
     * @deprecated @param dbfile
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Network loadNetwork(File dbfile) throws SQLException, ClassNotFoundException, IOException {
        HE_Database db = new HE_Database(dbfile, true);
        return db.loadNetwork();
    }

    /**
     * @deprecated @param dbfile
     * @param crsDB
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Network loadNetwork(File dbfile, CoordinateReferenceSystem crsDB) throws SQLException, ClassNotFoundException, IOException {
        HE_Database db = new HE_Database(dbfile, true);
        return db.loadNetwork(crsDB);
    }

    public String loadCoordinateReferenceSystem() throws SQLException, IOException {
        int crs;
        try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery("SELECT INHALT FROM ITWH$VARIABLEN WHERE NAME='Koordinatenbezugssystem'")) {
            if (!rs.isBeforeFirst()) {
                throw new SQLException("No Coordinate Reference System set.");
            }
            rs.next();
            crs = Integer.parseInt(rs.getString(1));
        }
        return "EPSG:" + crs;
    }

    public Network loadNetwork() throws SQLException, ClassNotFoundException {
        CoordinateReferenceSystem crsDB = null;
        try {
            //Read coordinate reference system from poly.xml next to this idbf file
            String crscode = loadCoordinateReferenceSystem();
            if (verbose) {
                System.out.println("Model DB Network's CRS is " + crscode);
            }
            crsDB = CRS.decode(crscode);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (crsDB == null) {
            System.out.println("Could not decode Database's coordinate reference system. use EPSG:25832");
            try {
                crsDB = CRS.decode("EPSG:25832");
//            throw new IllegalArgumentException("Could not decode Database's coordinate reference system.");
            } catch (FactoryException ex) {
                Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return loadNetwork(crsDB);
    }

    public Network loadNetwork(CoordinateReferenceSystem crsDB) throws SQLException, ClassNotFoundException {
        try {
            HashMap<String, Manhole> smap;
            LinkedList<Pipe> pipes_drain;
            LinkedList<Pipe> pipes_sewer;

            try (Connection con = getConnection()) {
                if (Network.crsUTM == null) {
                    try {
                        if (crsDB != null && crsDB.getCoordinateSystem().getAxis(0).getUnit().toString().equals("m")) {
                            if (verbose) {
                                System.out.println(this.getClass() + "::loadNetwork: Datenbank speichert als UTM " + crsDB.getCoordinateSystem().getName());
                            }
                            Network.crsUTM = crsDB;
                        } else {
                            System.out.println(this.getClass() + "::loadNetwork: Coordinatensystem der Datenbank ist nicht cartesisch: " + crsDB);
                            Network.crsUTM = CRS.decode("EPSG:25832");
                        }
                    } catch (Exception exception) {
                        System.out.println("axis:" + crsDB.getCoordinateSystem().getAxis(0));
                        System.out.println("unit:" + crsDB.getCoordinateSystem().getAxis(0).getUnit());
                        exception.printStackTrace();
                    }
                } else {
//                    System.out.println("Network utm System is " + Network.crsUTM);
                }
                MathTransform transformDB_UTM = CRS.findMathTransform(crsDB, Network.crsUTM);
                MathTransform transformDB_WGS = CRS.findMathTransform(crsDB, Network.crsWGS84);

                //Initialisiere die Speicherobjekte für die fertigen Kanalobjekte
                smap = new HashMap<>();
                pipes_drain = new LinkedList<>();
                pipes_sewer = new LinkedList<>();
                HashMap<Integer, Profile> schachtprofile = new HashMap<>();
                //////////##############################################################
                // Beginne die Abfrage der Schachtelemente
                /////////
                Statement st = con.createStatement();
                ResultSet res = null;// = st.executeQuery("SELECT MSysObjects.Name, MSysObjects.Type FROM MSysObjects WHERE MSysObjects.Name Not Like \"MsyS*\" AND MSysObjects.Type=1 ORDER BY MSysObjects.Name;") ;
//                int c = 0;//Columncount
                if (isSQLite) {

                    res = st.executeQuery("SELECT name,geometry,DURCHMESSER,gelaendehoehe,deckelhoehe,sohlhoehe,ID,KANALART from schacht ORDER BY ID;");
                    while (res.next()) {
                        String name = res.getString(1);
                        byte[] buffer = res.getBytes(2);
                        Coordinate coordDB = decodeCoordinate(buffer);
                        Coordinate coordUTM;
                        try {
                            coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM); //gt.toGlobal(coord);

                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS); //gt.toGlobal(coord);

                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);
                            int durchmesser = res.getInt(3);
                            Profile p = schachtprofile.get(durchmesser);
                            if (p == null) {
                                p = new CircularProfile(durchmesser / 1000.);
                                schachtprofile.put(durchmesser, p);
                            }
                            Manhole m = new Manhole(position, name, p);
                            m.setSurface_height(res.getFloat(4));
                            m.setTop_height(res.getFloat(5));
                            m.setSole_height(res.getFloat(6));
                            m.setManualID(res.getInt(7));
                            int raintype = res.getInt(8);
                            if (raintype == 0) {
                                m.setWaterType(Capacity.SEWER_TYPE.MIX);
                            } else if (raintype == 1) {
                                m.setWaterType(Capacity.SEWER_TYPE.DRAIN);
                            } else if (raintype == 2) {
                                m.setWaterType(Capacity.SEWER_TYPE.SEWER);
                            }
                            smap.put(name, m);
                        } catch (MismatchedDimensionException ex) {
                            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (TransformException ex) {
                            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                } else {
                    try {
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,DURCHMESSER,gelaendehoehe,deckelhoehe,sohlhoehe,ID from schacht ORDER BY ID;");
                    } catch (SQLException sQLException) {
                        System.err.println("File '" + databaseFile.getAbsolutePath() + "' doesnot contain topological information");
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,OBERFLAECHE,gelaendehoehe,deckelhoehe,sohlhoehe,ID from schacht ORDER BY ID;");
                    }
//                    c = res.getMetaData().getColumnCount();

                    while (res.next()) {
                        try {
                            String name = res.getString(1);

                            double x = res.getDouble(2);
                            double y = res.getDouble(3);
                            Coordinate coordDB = new Coordinate(x, y);

                            Coordinate coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM); //gt.toGlobal(coord);
                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS); //gt.toGlobal(coord);

                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);
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
                            m.setManualID(res.getInt(8));
                            smap.put(name, m);
                        } catch (SQLException | MismatchedDimensionException | TransformException exception) {
                            System.err.println("Fehler bei SQL Abfrage.Manhole: name=" + res.getString(1) + ", X=" + res.getString(3) + ", Y=" + res.getString(4) + ", SurfaceHeight=" + res.getString(5));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                res.close();
                // ende Schacht abfrage
                // starte Schacuhtauslass Suche
                if (isSQLite) {

                    res = st.executeQuery("SELECT name,ID,Geometry,gelaendehoehe,scheitelhoehe,sohlhoehe from AUSLASS ORDER BY ID;");
                    while (res.next()) {
                        try {
                            String name = res.getString(1);

                            byte[] geometryblob = res.getBytes(3);

                            Coordinate coordDB = decodeCoordinate(geometryblob);

                            Coordinate coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM);
                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS);
                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);
                            Profile p = schachtprofile.get(0);
                            if (p == null) {
                                p = new CircularProfile(0);
                                schachtprofile.put(0, p);
                            }
                            Manhole m = new Manhole(position, name, p);
                            m.setSurface_height(res.getFloat(4));
                            m.setTop_height(res.getFloat(5));
                            m.setSole_height(res.getFloat(6));
                            m.setManualID(res.getInt(2));
                            m.setAsOutlet(true);
                            smap.put(name, m);

                        } catch (Exception exception) {
                            System.err.println(HE_Database.class
                                    + "::Fehler bei SQL Abfrage. Pipe: name=" + res.getString(1) + ", SurfaceHeight=" + res.getString(4));
                        }
                    }
                } else {
                    //Read Firebird Database
                    try {
                        res = st.executeQuery("SELECT name,ID,XKOORDINATE,YKOORDINATE,gelaendehoehe,scheitelhoehe,sohlhoehe from AUSLASS ORDER BY ID;");

                    } catch (SQLException sQLException) {
                        System.err.println(HE_Database.class
                                + "::File '" + databaseFile.getAbsolutePath() + "' doesnot contain topological information. Seems to be a Resultfile.");
                        res = st.executeQuery("SELECT name,ID,XKOORDINATE,YKOORDINATE,gelaendehoehe,scheitelhoehe,sohlhoehe from AUSLASS ORDER BY ID;");
                    }
//                c = res.getMetaData().getColumnCount();
                    while (res.next()) {
                        try {
                            String name = res.getString(1);

                            double x = res.getDouble(3);
                            double y = res.getDouble(4);
                            Coordinate coordDB = new Coordinate(x, y);

                            Coordinate coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM);
                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS);
//                        System.out.println("source: x="+x+" y="+y);
//                        System.out.println(getClass()+"  x="+coordWGS.x+",   y="+coordWGS.y);
                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);
                            Profile p = schachtprofile.get(0);
                            if (p == null) {
                                p = new CircularProfile(0);
                                schachtprofile.put(0, p);
                            }
                            Manhole m = new Manhole(position, name, p);
                            m.setSurface_height(res.getFloat(5));
                            m.setTop_height(res.getFloat(6));
                            m.setSole_height(res.getFloat(7));
                            m.setManualID(res.getInt(2));
                            m.setAsOutlet(true);
                            smap.put(name, m);

                        } catch (Exception exception) {
                            System.err.println(HE_Database.class
                                    + "::Fehler bei SQL Abfrage. Pipe: name=" + res.getString(1) + ", X=" + res.getString(3) + ", Y=" + res.getString(4) + ", SurfaceHeight=" + res.getString(5));
                        }
                    }
                }
                res.close();
                /////*#################################################################
                ////   Haltungen
                ////
                res = st.executeQuery("SELECT name,schachtoben,schachtunten,SOHLHOEHEOBEN,SOHLHOEHEUNTEN,laenge,kanalart,profiltyp,geometrie1,rauigkeitsbeiwert,ID from ROHR;");
//                c = res.getMetaData().getColumnCount();
                while (res.next()) {
                    String name = res.getString(1);
                    String nameoben = res.getString(2);
                    String nameunten = res.getString(3);
                    Manhole mhoben = smap.get(nameoben);
                    Manhole mhunten = smap.get(nameunten);

                    if (mhoben == null) {
                        System.err.println(HE_Database.class
                                + "::Can not find upper manhole '" + nameoben + "' for pipe " + name);

                        continue;
                    }
                    if (mhunten == null) {
                        System.err.println("Can not find lower manhole '" + nameunten + "' for pipe " + name);
                        continue;
                    }

                    int durchmesser = (int) (res.getDouble(9) * 1000);//ist schon in Meter angegeben
                    Profile p = schachtprofile.get(durchmesser);
                    if (p == null) {
                        p = new CircularProfile(durchmesser / 1000.);
                        schachtprofile.put(durchmesser, p);
                    }
                    //Verbindungen anlegen
                    //Define the Position of Connections
                    model.topology.Connection_Manhole_Pipe upper, lower;
                    if (orientPipesGravityFlown) {
                        if (res.getFloat(4) >= res.getFloat(5)) {
                            upper = new model.topology.Connection_Manhole_Pipe(mhoben.getPosition(), res.getFloat(4));
                            lower = new model.topology.Connection_Manhole_Pipe(mhunten.getPosition(), res.getFloat(5));
                        } else {
                            // Tausche sie so, dass es immer dem Gefälle nach geht
                            lower = new model.topology.Connection_Manhole_Pipe(mhoben.getPosition(), res.getFloat(4));
                            upper = new model.topology.Connection_Manhole_Pipe(mhunten.getPosition(), res.getFloat(5));
                            Manhole temp = mhoben;
                            mhoben = mhunten;
                            mhunten = temp;
                        }
                    } else {
                        //Do not change the direction of Pipes, also not if they are pointed against gravity flow.
                        upper = new model.topology.Connection_Manhole_Pipe(mhoben.getPosition(), res.getFloat(4));
                        lower = new model.topology.Connection_Manhole_Pipe(mhunten.getPosition(), res.getFloat(5));
                    }

                    Pipe pipe = new Pipe(upper, lower, p);
                    pipe.setName(name);

                    mhoben.addConnection(upper);
                    mhunten.addConnection(lower);

                    // fertig Connections zwischen Schacht und haltung eingefügt
                    String watertype = res.getString(7);
                    Capacity.SEWER_TYPE type = Capacity.SEWER_TYPE.UNKNOWN;
                    if (watertype.equals("0") || watertype.equals("KM")) {
                        type = Capacity.SEWER_TYPE.MIX;
                    } else if (watertype.equals("1") || watertype.equals("R")) {
                        type = Capacity.SEWER_TYPE.DRAIN;

                    } else if (watertype.equals("2") || watertype.equals("S")) {
                        type = Capacity.SEWER_TYPE.SEWER;

                    } else {
                        System.out.println("Kanalart '" + watertype + "' ist noch icht bekannt. in " + this.getClass()
                                .getName());
                    }
                    pipe.setWaterType(type);
                    pipe.setLength(res.getFloat(6));
                    pipe.setRoughness_k(res.getFloat(10));
                    pipe.setManualID(res.getInt(11));

                    pipes_sewer.add(pipe);
                }
                res.close();
                // Bei Schächten die Informationen aus der Berechnung hinzufügen
//            try {
//                res = st.executeQuery("SELECT name,ID,UEBERSTAUDAUER,UEBERSTAUVOLUMEN from LAU_MAX_S WHERE UEBERSTAUDAUER>0 ORDER BY ID;");
//                res.close();
//                
//            } catch (SQLException sQLException) {
//            }
            }
            pipes_sewer.addAll(pipes_drain);
            Network nw = new Network(pipes_sewer, new HashSet<Manhole>(smap.values()));
            return nw;

        } catch (FactoryException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @deprecated use applyTimelines (non static)
     * @param file
     * @param net
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SQLException
     */
    public static Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> readTimelines(File file, Network net) throws FileNotFoundException, IOException, SQLException {
        Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> ret;
        HE_Database db = new HE_Database(file, true);

        ret = db.applyTimelines(net);//applyTimelines(con, net);

        return ret;
    }

    public static Coordinate decodeCoordinate(byte[] byteblob) {
        ByteBuffer bb = ByteBuffer.wrap(byteblob);
        bb.order(ByteOrder.LITTLE_ENDIAN);

//        System.out.println("Buffersize:" + byteblob.length + ", capacity:" + bb.capacity());// + " : " + bb.asFloatBuffer().get(0) + " , " + bb.asDoubleBuffer().get(1));
//        for (int i = 0; i < bb.capacity(); i++) {
//            bb.position(i);
//            System.out.println(i + ": " + bb.getDouble());
//        }
        bb.position(6);
        double x = bb.getDouble();
        bb.position(14);
        double y = bb.getDouble();

        return new Coordinate(x, y);
    }

    public Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> applyTimelines(Network net) throws FileNotFoundException, IOException, SQLException {

        ArrayTimeLinePipeContainer container;
        con = getConnection();
        ArrayTimeLineManholeContainer manholeContainer;
        try (Statement st = con.createStatement()) {
            ResultSet res;
//            res= st.executeQuery("SELECT MIN(ZEITPUNKT) FROM LAU_GL_EL;");
//            res.next();
//            long startime = res.getDate(1).getTime();
//            res.close();
//            res = st.executeQuery("SELECT MAX(ZEITPUNKT) FROM LAU_GL_EL;");
//            res.next();
//            long endtime = res.getDate(1).getTime();
//            res.close();
            res = st.executeQuery("SELECT COUNT(DISTINCT ZEITPUNKT) FROM LAU_GL_EL;");
            res.next();
            int zeiteintraege = res.getInt(1);
            res.close();
            //        System.out.println("  Scenario hat " + zeiteintraege + " Zeiteinträge.");
            // ArrayTimelinePipe muss neu initiiert werden
            //Read timesteps
            long[] times = new long[zeiteintraege];
            res = st.executeQuery("SELECT ZEITPUNKT FROM LAU_GL_EL GROUP BY ZEITPUNKT;");
            int i = 0;
            while (res.next()) {
                if (isSQLite) {
                    try {
                        times[i] = sqliteDateTimeFormat.parse(res.getString(1)).getTime();
                    } catch (ParseException ex) {
                        Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    times[i] = res.getTimestamp(1).getTime();
                }
                i++;
            }
            container = new ArrayTimeLinePipeContainer(times, net.getPipes().size());
            manholeContainer = new ArrayTimeLineManholeContainer(times, net.getManholes().size());
//        ArrayTimeLineManholeContainer.instance=manholeContainer;
            i = 0;
            for (Pipe pipe : net.getPipes()) {
                pipe.setStatusTimeLine(new ArrayTimeLinePipe(container, i));
                i++;
            }
            i = 0;
            for (Manhole manhole : net.getManholes()) {
                manhole.setStatusTimeline(new ArrayTimeLineManhole(manholeContainer, i));
                i++;
            }
            //OLD SCHEME WITHOUT 2D SURFACE
            try {
                res = st.executeQuery("SELECT ID,KNOTEN,ZEITPUNKT,DURCHFLUSS,ZUFLUSS,WASSERSTAND,UEBERSTAUVOLUMEN,VOLUMEN,OBERFLAECHE from LAU_GL_S ORDER BY ID,ZEITPUNKT;");
                int id = Integer.MIN_VALUE;
                int timeIndex = 0;
                Manhole mh = null;
                while (res.next()) {
                    int heID = res.getInt(1);
                    String heName = res.getString(2);
                    float h = res.getFloat(6);
                    if (id != heID) {
                        mh = net.getManholeByManualID(heID);
                        id = heID;
                        timeIndex = 0;
                    }
                    if (mh != null) {
                        if (mh.getStatusTimeLine() != null) {
                            ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setWaterZ(h, timeIndex);
                        }
                    }
                    timeIndex++;
                }
            } catch (SQLException sQLException) {
                throw new FileNotFoundException("File is not filled with measurements. " + sQLException.getLocalizedMessage());
            }
            //Finished Waterheight reading
            // Read Spillout Flow from Manhole to Surface
            //NEW SCHEME INCLUDING 2D SURFACE
            try {
                res = st.executeQuery("SELECT ID,"
                        + "KNOTEN, "
                        + "ZEITPUNKT, "
                        + "(ABFLUSS-ZUFLUSS) AS NETTO, "
                        + "WASSERSTAND "
                        + "FROM KNOTENLAUFEND2D "
                        + "ORDER BY ID, ZEITPUNKT;");
                int id = Integer.MIN_VALUE;
                int timeIndex = 0;
                Manhole mh = null;
                while (res.next()) {
                    int heID = res.getInt(1);
                    String heName = res.getString(2);
                    float outflow = res.getFloat(4);
                    if (id != heID) {
                        mh = net.getManholeByManualID(heID);
                        id = heID;
                        timeIndex = 0;
                    }
                    if (mh != null && mh.getStatusTimeLine() != null) {
                        ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setFluxToSurface(outflow, timeIndex);
                    }
                    timeIndex++;
                }
            } catch (SQLException sQLException) {
                throw new FileNotFoundException("Could not apply HE<->2D exchange flow. " + sQLException.getLocalizedMessage());
            }

            res.close();
            ///////////////////////////////////////////////Timeseries in Pipes//////////////////
            // Zeitreihe in Rohren abfragen
            res = st.executeQuery("SELECT ID,KANTE,ZEITPUNKT,DURCHFLUSS,GESCHWINDIGKEIT,WASSERSTAND,AUSLASTUNG,FROUDE,WASSERSTANDOBEN,WASSERSTANDUNTEN from LAU_GL_EL ORDER BY ID,ZEITPUNKT;");
            int id = Integer.MIN_VALUE;
            Pipe pipe = null;
            int timeIndex = 0;
            while (res.next()) {
                int heID = res.getInt(1);
                String heName = res.getString(2);
                if (id != heID) {
                    id = heID;
                    try {
                        pipe = net.getPipeByName(heName);
                        timeIndex = 0;
                    } catch (NullPointerException nullPointerException) {
                        nullPointerException.printStackTrace();
                    }
                }
                float q = res.getFloat(4);
                float v = res.getFloat(5);
                float h = res.getFloat(6);
                if (pipe != null && pipe.getStatusTimeLine() != null) {
                    ArrayTimeLinePipe tl = (ArrayTimeLinePipe) pipe.getStatusTimeLine();
                    tl.setVelocity(v, timeIndex);
                    tl.setWaterlevel(h, timeIndex);
                    tl.setFlux(q, timeIndex);
                }
                timeIndex++;
            }
            res.close();
            //Timelines auf max und mean berechnen
            for (Pipe p : net.getPipes()) {
                if (p.getStatusTimeLine() != null && p.getStatusTimeLine() instanceof ArrayTimeLinePipe) {
                    ((ArrayTimeLinePipe) p.getStatusTimeLine()).calculateMaxMeanValues();
                }
            }
            //Stabilitätsindex auslesen
//            res = st.executeQuery("SELECT KANTE, ID, STABILITAETSINDEX from LAU_MAX_EL");
//            while (res.next()) {
//                String pipename = res.getString(1);
//                Pipe p = net.getPipeByName(pipename);
//                if (p != null) {
//                    if (p.tags == null) {
//                        p.tags = new Tags(1);
//                    }
//                    p.tags.put("Stabilitaet", res.getInt(3) + "");
//                }
//            }
            ///////////////FERTIG ROHRE
            //******************************BEGIN Schmutzfracht******************
//        res = st.executeQuery("SELECT ID,KANTE,ZEITPUNKT,STOFF,KONZENTRATION from KANTESTOFFLAUFEND ORDER BY ID, ZEITPUNKT;");
            res = st.executeQuery("SELECT KANTESTOFFLAUFEND.ID,KANTESTOFFLAUFEND.KANTE, KANTESTOFFLAUFEND.ZEITPUNKT,(KONZENTRATION * DURCHFLUSS)/ 1000 AS FRACHTKGPS,KONZENTRATION, AUSLASTUNG"
                    + " FROM KANTESTOFFLAUFEND INNER JOIN LAU_GL_EL ON KANTESTOFFLAUFEND.KANTE = LAU_GL_EL.KANTE AND KANTESTOFFLAUFEND.ZEITPUNKT = LAU_GL_EL.ZEITPUNKT"
                    + " ORDER BY KANTESTOFFLAUFEND.KANTE,ZEITPUNKT");
            // Konzentration mg/l = g/m^3
            // Frachtrate  kg/s
            id = Integer.MIN_VALUE;
            while (res.next()) {
                int heID = res.getInt(1);
                String heName = res.getString(2);
                long time = 0;
                if (isSQLite) {
                    try {
                        time = sqliteDateTimeFormat.parse(res.getString(3)).getTime();
                    } catch (ParseException ex) {
                        Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    time = res.getTimestamp(3).getTime();
                }
                double frachtrate = res.getDouble(4);
                if (id != heID) {
                    pipe = net.getPipeByName(heName);
                    id = heID;
                }
                timeIndex = ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).container.getTimeIndex(time);
                ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).setMass_reference((float) frachtrate, timeIndex);
            }
        }
        return new Pair<>(container, manholeContainer);
    }

    public ArrayTimeLineManholeContainer applyTimelinesManholes(Collection<? extends StorageVolume> manholes) throws SQLException, IOException, Exception {

        long[] times = loadTimeStepsNetwork();

        ArrayTimeLineManholeContainer manholeContainer = new ArrayTimeLineManholeContainer(times, manholes.size());
        int i = 0;
        for (StorageVolume manhole : manholes) {
            manhole.setStatusTimeline(new ArrayTimeLineManhole(manholeContainer, i));
            i++;
        }
        con = getConnection();
        Statement st = con.createStatement();
        //OLD SCHEME WITHOUT 2D SURFACE
        try {
            ResultSet res = st.executeQuery("SELECT ID,KNOTEN,ZEITPUNKT,DURCHFLUSS,ZUFLUSS,WASSERSTAND,UEBERSTAUVOLUMEN,VOLUMEN,OBERFLAECHE from LAU_GL_S ORDER BY ID,ZEITPUNKT;");
            int id = Integer.MIN_VALUE;
            int timeIndex = 0;
            StorageVolume mh = null;
            while (res.next()) {
                int heID = res.getInt(1);
                String heName = res.getString(2);
                float h = res.getFloat(6);
                if (id != heID) {
                    //Search for manhole
                    mh = null;
                    for (StorageVolume manhole : manholes) {

                        if (manhole.getManualID() == heID) {
                            mh = manhole;
                            break;
                        }
                    }
                    if (mh == null) {
                        System.err.println(getClass() + ": could not find manhole DBid:" + heID + ", name: " + heName + "  in the network.");
                        continue;
                    }
                    id = heID;
                    timeIndex = 0;
                }
                ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setWaterZ(h, timeIndex);
                timeIndex++;
            }
        } catch (SQLException sQLException) {
            throw new Exception("File is not filled with measurements. " + sQLException.getLocalizedMessage());
        }
        //Finished Waterheight reading
        // Read Spillout Flow from Manhole to Surface
        //NEW SCHEME INCLUDING 2D SURFACE
        try {
            ResultSet res = st.executeQuery("SELECT ID,"
                    + "KNOTEN, "
                    + "ZEITPUNKT, "
                    + "(ABFLUSS-ZUFLUSS) AS NETTO, "
                    + "WASSERSTAND "
                    + "FROM KNOTENLAUFEND2D "
                    + "ORDER BY ID, ZEITPUNKT;");
            int id = Integer.MIN_VALUE;
            int timeIndex = 0;
            StorageVolume mh = null;
            while (res.next()) {
                int heID = res.getInt(1);
                String heName = res.getString(2);
                float outflow = res.getFloat(4);
                if (id != heID) {
                    mh = null;
                    for (StorageVolume manhole : manholes) {
                        if (manhole.getManualID() == heID) {
                            mh = manhole;
                            break;
                        }
                    }
                    if (mh == null) {
                        System.err.println(getClass() + ": could not find manhole DBid:" + heID + ", name: " + heName + "  in the network.");
                        continue;
                    }
                    id = heID;
                    timeIndex = 0;
                }
                ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setFluxToSurface(outflow, timeIndex);
                timeIndex++;
            }
            res.close();
            con.close();
        } catch (SQLException sQLException) {
            throw new Exception("Could not apply HE<->2D exchange flow. " + sQLException.getLocalizedMessage());
        }

        return manholeContainer;
    }

    /**
     * @deprecated static request should not be used because handling of remote
     * files is spaceconsuming.
     * @param idbFile
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static ArrayList<HEInjectionInformation> readInjectionInformation(File idbFile) throws SQLException, IOException, ParseException {
        HE_Database db = new HE_Database(idbFile, true);
        return db.readInjectionInformation();
    }

    public ArrayList<HEInjectionInformation> readInjectionInformation() throws SQLException, IOException, ParseException {
        Connection con = getConnection();
        Statement st = con.createStatement();

        // Finde die richtige Anfangszeit
        ResultSet rs = st.executeQuery("SELECT BERICHTANFANG from EXTRANPARAMETERSATZ;");
        if (!rs.isBeforeFirst()) {
            //Resultset is empty
            rs.close();
            st.close();
            con.close();
            return new ArrayList<>(0);
        }
        rs.next();
        GregorianCalendar gcdaystart = new GregorianCalendar();
        if (isSQLite) {
//            System.out.println(rs.getString(1));
            gcdaystart.setTimeInMillis(sqliteDateTimeFormat.parse(rs.getString(1)).getTime());
        } else {
            Timestamp simulationStart = rs.getTimestamp(1);
            gcdaystart.setTimeInMillis(simulationStart.getTime());
        }
        gcdaystart.set(GregorianCalendar.HOUR_OF_DAY, 0);
        gcdaystart.set(GregorianCalendar.MINUTE, 0);
        gcdaystart.set(GregorianCalendar.SECOND, 0);
        gcdaystart.set(GregorianCalendar.MILLISECOND, 0);
        Timestamp daystart = new Timestamp(gcdaystart.getTimeInMillis());
        rs.close();

        ArrayList<HEInjectionInformation> particles = new ArrayList<>();
        String qstring = "SELECT ROHR, ROHRREF,ZUFLUSSDIREKT AS ZUFLUSSDIREKT_LperS,"
                + "FAKTOR AS FAKTOR_von1,KONZENTRATION AS KONZENTRATION_MGperL,"
                + "EINZELMUSTER.Wert AS EINZELWERT_von24,STOFFMUSTER.WERT AS STOFFWERT_von24,"
                + "STOFFMUSTER.KEYWERT AS STARTZEIT, EINZELEINLEITER.NAME,"
                + "EINZELEINLEITER.ZEITMUSTER AS EINZELEINLEITERZEITMUSTER,"
                + "Stoffeinzeleinleiter.Zeitmuster AS STOFFEINLEITERZEITMUSTER "
                + "FROM EINZELEINLEITER JOIN StoffEINZELEINLEITER ON STOFFEINZELEINLEITER.EINZELEINLEITERREF=EINZELEINLEITER.ID\n"
                + "JOIN TABELLENINHALTE AS EINZELMUSTER ON EINZELEINLEITER.ZEITMUSTERREF=EINZELMUSTER.ID\n"
                + "JOIN TABELLENINHALTE AS STOFFMUSTER ON  EINZELMUSTER.KEYWERT=STOFFMUSTER.KEYWERT AND (STOFFEINZELEINLEITER.ZEITMUSTERREF=STOFFMUSTER.ID OR STOFFEINZELEINLEITER.ZEITMUSTERREF IS NULL)\n"
                + "WHERE STOFFMUSTER.WERT>0 AND EINZELMUSTER.WERT>0 AND FAKTOR >0 AND KONZENTRATION>0\n"
                + "ORDER BY EINZELEINLEITER.ID,STOFFEINZELEINLEITER.ID,EINZELMUSTER.ID,STARTZEIT";

        rs = st.executeQuery(qstring);
        Material m = new Material("Stofff", 1000, true);
        while (rs.next()) {
            double wert = rs.getDouble("EINZELWERT_von24") / 24 * rs.getDouble("STOFFWERT_von24") / 24;
            int rf = rs.getInt("Startzeit");
            long starttime = daystart.getTime() + (long) ((rf) * 60 * 60 * 1000);
            long endtime = daystart.getTime() + (long) ((rf + 1) * 60 * 60 * 1000);
            try {
                HEInjectionInformation info = new HEInjectionInformation(rs.getString("ROHR"), starttime, endtime, wert);//p.getPosition3D(0),true, wert, numberofparticles, m, starttime, endtime);
                particles.add(info);
            } catch (Exception exception) {
                System.err.println("Could not find Pipe '" + rs.getString("ROHR") + "'.");
                exception.printStackTrace();
            }
        }
        rs.close();
        st.close();
        return particles;
    }

    public static int readOutputTimeIntervallPipes(File resultIDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(resultIDBF, true);
        return db.readOutputTimeIntervallPipes();
    }

    public int readOutputTimeIntervallPipes() throws SQLException, IOException {
        Connection con = getConnection();
        Statement st = con.createStatement();

        // Finde die richtige Anfangszeit
        ResultSet rs = st.executeQuery("SELECT BERICHTZEITSCHRITT from EXTRANPARAMETERSATZ;");
        rs.next();
        int interval_durarion = rs.getInt(1);
        rs.close();
        st.close();
        return interval_durarion;
    }

//    public int readOutputTimeIntervallSurface() throws SQLException, IOException {
//        return readOutputTimeIntervallSurface(localFile);
//    }
    public static int readOutputTimeIntervallSurface(File resultIDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(resultIDBF, true);
        return db.readOutputTimeIntervallSurface();
    }

    public int readOutputTimeIntervallSurface() throws SQLException, IOException {
        Connection con = getConnection();
        Statement st = con.createStatement();

        // Finde die richtige Anfangszeit
        ResultSet rs = st.executeQuery("SELECT AUSGABEZEITSCHRITT from EXTRAN2DPARAMETERSATZ;");
        rs.next();
        int interval_durarion = rs.getInt(1);
        rs.close();
        st.close();
        return interval_durarion;
    }

    /**
     *
     * @param file
     * @param nameRR
     * @return
     * @throws Exception
     */
    public static Raingauge_Firebird readRegenreihe(File file, String nameRR) throws Exception {
        HE_Database db = new HE_Database(file, true);
        return db.readRegenreihe(nameRR);
    }

    public Raingauge_Firebird readRegenreihe(String nameRR) throws Exception {
        con = getConnection();
        Raingauge_Firebird rrf;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM REGENREIHE WHERE NAME='" + nameRR + "'")) {
            rs.next();
            if (isSQLite) {
                rrf = new Raingauge_Firebird(
                        rs.getInt("ID"),
                        rs.getBytes("DATEN"),
                        rs.getInt("INTERVALLBREITE"),
                        sqliteDateTimeFormat.parse(rs.getString("REGENBEGINN")).getTime(),
                        sqliteDateTimeFormat.parse(rs.getString("REGENENDE")).getTime(),
                        rs.getShort("MODELLREGEN") == 1,
                        rs.getString("NAME"),
                        rs.getString("KOMMENTAR"));
            } else {
                rrf = new Raingauge_Firebird(
                        rs.getInt("ID"),
                        rs.getBytes("DATEN"),
                        rs.getInt("INTERVALLBREITE"),
                        rs.getTimestamp("REGENBEGINN").getTime(),
                        rs.getTimestamp("REGENENDE").getTime(),
                        rs.getShort("MODELLREGEN") == 1,
                        rs.getString("NAME"),
                        rs.getString("KOMMENTAR"));
            }
        }
        return rrf;
    }

    /**
     * @deprecated @param dbfile
     * @return
     * @throws Exception
     */
    public static Raingauge_Firebird readRegenreihe(File dbfile) throws Exception {
        HE_Database db = new HE_Database(dbfile, true);
        return db.readRegenreihe();
    }

    public Raingauge_Firebird readRegenreihe() throws Exception {
        con = getConnection();// openConnection(file, true, false);
        Raingauge_Firebird rrf = null;
        if (isSQLite) {
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT "
                    + "* FROM REGENREIHE,"
                    + "REGENSCHREIBERZUORDNUNG WHERE REGENREIHE.ID=REGENSCHREIBERZUORDNUNG.REGENREIHEREF") //This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
                    ) {
                rs.next();
                rrf = new Raingauge_Firebird(
                        rs.getInt("ID"),
                        rs.getBytes("DATEN"),
                        rs.getInt("INTERVALLBREITE"),
                        this.sqliteDateTimeFormat.parse(rs.getString("REGENBEGINN")).getTime(),
                        this.sqliteDateTimeFormat.parse(rs.getString("REGENENDE")).getTime(),
                        rs.getShort("MODELLREGEN") == 1,
                        rs.getString("NAME"),
                        rs.getString("KOMMENTAR"));

//            String nameRR = rs.getString(1);
            } //This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
        } else {
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT "
                    + "* FROM REGENREIHE,"
                    + "REGENSCHREIBERZUORDNUNG WHERE REGENREIHE.ID=REGENSCHREIBERZUORDNUNG.REGENREIHEREF") //This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
                    ) {
                rs.next();
                rrf = new Raingauge_Firebird(
                        rs.getInt("ID"),
                        rs.getBytes("DATEN"),
                        rs.getInt("INTERVALLBREITE"),
                        rs.getTimestamp("REGENBEGINN").getTime(),
                        rs.getTimestamp("REGENENDE").getTime(),
                        rs.getShort("MODELLREGEN") == 1,
                        rs.getString("NAME"),
                        rs.getString("KOMMENTAR"));

//            String nameRR = rs.getString(1);
            } //This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
        }
        return rrf;
    }

    public Raingauge_Firebird[] readAllRegenreihe() throws Exception {
        con = getConnection();
        LinkedList<Raingauge_Firebird> regen = new LinkedList<>();
        Raingauge_Firebird rrf;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM REGENREIHE")) {
            while (rs.next()) {
                if (isSQLite) {
                    rrf = new Raingauge_Firebird(
                            rs.getInt("ID"),
                            rs.getBytes("DATEN"),
                            rs.getInt("INTERVALLBREITE"),
                            sqliteDateTimeFormat.parse(rs.getString("REGENBEGINN")).getTime(),
                            sqliteDateTimeFormat.parse(rs.getString("REGENENDE")).getTime(),
                            rs.getShort("MODELLREGEN") == 1,
                            rs.getString("NAME"),
                            rs.getString("KOMMENTAR"));
                } else {
                    rrf = new Raingauge_Firebird(
                            rs.getInt("ID"),
                            rs.getBytes("DATEN"),
                            rs.getInt("INTERVALLBREITE"),
                            rs.getTimestamp("REGENBEGINN").getTime(),
                            rs.getTimestamp("REGENENDE").getTime(),
                            rs.getShort("MODELLREGEN") == 1,
                            rs.getString("NAME"),
                            rs.getString("KOMMENTAR"));
                }
                regen.add(rrf);
            }
        }
        return regen.toArray(new Raingauge_Firebird[regen.size()]);
    }

    public double[] readPrecipitation(String scenarioName) throws Exception {
        byte[] blob;
        Connection co = getConnection();
        try (Statement st = co.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT REGENREIHE.DATEN  FROM REGENREIHE WHERE NAME='" + scenarioName + "'");

            if (!rs.isBeforeFirst()) {
                //Resultset is empty
                System.out.println("Last: no Regenreihe with name '" + scenarioName + "'");
                rs = st.executeQuery("SELECT REGENREIHE.DATEN FROM REGENREIHE");
            }
            rs.next();

            blob = rs.getBytes(1);
            rs.close();
        }

        return Raingauge_Firebird.readPrecipitation(blob);

    }

    public static double[] readPrecipitation(File extranResultDB) throws Exception {
        HE_Database db = new HE_Database(extranResultDB, true);
        return db.readPrecipitation();
    }

    public double[] readPrecipitation() throws Exception {
        byte[] blob;
        try (Connection extran = getConnection(); Statement st = extran.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT REGENREIHE.DATEN FROM REGENREIHE,REGENSCHREIBERZUORDNUNG WHERE REGENREIHE.ID=REGENSCHREIBERZUORDNUNG.REGENREIHEREF");//This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.

            if (!rs.isBeforeFirst()) {
                //Resultset is empty
                System.out.println("Last: no Regenreihenschreiberzuordnung use raw Regenreihe");
                rs = st.executeQuery("SELECT REGENREIHE.DATEN FROM REGENREIHE");
            }
            rs.next();

            blob = rs.getBytes(1);
            rs.close();
        }
        return Raingauge_Firebird.readPrecipitation(blob);

    }

    public static String readResultname(File extranResultDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranResultDBF, true);
        return db.readResultname();
    }

    public String readResultname() throws SQLException, IOException {
        String name = "";
        Connection extran = getConnection();
        Statement st = extran.createStatement();
        ResultSet rs = st.executeQuery("SELECT RESULTNAME FROM EXTRAN2DPARAMETERSATZ");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
        if (rs.isBeforeFirst()) {
            //Resultset is empty
            rs.close();
            st.close();
            return null;
        }
        rs.next();
        name = rs.getString(1);
        rs.close();
        st.close();
        return name;
    }

    public static String readModelname(File extranResultDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranResultDBF, true);
        return db.readSurfaceModelname();
    }

    public String readSurfaceModelname() throws SQLException, IOException {
        String name = null;
        try (Connection extran = getConnection(); Statement st = extran.createStatement(); ResultSet rs = st.executeQuery("SELECT MODELNAME FROM EXTRAN2DPARAMETERSATZ") //This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
                ) {
            if (rs.isBeforeFirst()) {
                rs.next();
                name = rs.getString(1);

            }

        }
        return name;
    }

    public static double readExportTimeStep(File extranResultDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranResultDBF, true);
        return db.readExportTimeStep();
    }

    public double readExportTimeStep() throws SQLException, IOException {
        double dt = -1;
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs = st.executeQuery("SELECT AUSGABEZEITSCHRITT FROM EXTRAN2DPARAMETERSATZ");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            rs.next();
            dt = rs.getDouble(1);
            rs.close();
            st.close();
        }
        return dt;
    }

    public static int readNumberOfPipes(File extranDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranDBF, true);
        return db.readNumberOfPipes();
    }

    public int readNumberOfPipes() throws SQLException, IOException {
        int dt = -1;
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ROHR");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            rs.next();
            dt = rs.getInt(1);
            rs.close();
            st.close();
        }
        return dt;
    }

    public static int readNumberOfManholes(File extranDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranDBF, true);
        return db.readNumberOfManholes();
    }

    public int readNumberOfManholes() throws SQLException, IOException {
        int dt = -1;
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM SCHACHT");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            rs.next();
            dt = rs.getInt(1);
            rs.close();
            st.close();
        }
        return dt;
    }

    public static String readModelnamePipeNetwork(File extranDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranDBF, true);
        return db.readModelnamePipeNetwork();
    }

    public String readModelnamePipeNetwork() throws SQLException, IOException {
        String r = null;
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs = st.executeQuery("SELECT Kanalnetzdatei FROM EXTRANPARAMETERSATZ");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            if (rs.isBeforeFirst()) {
                rs.next();
                r = rs.getString(1);
            } else {
                System.err.println("Resultset is empty. No infomation about Kanalnetzdatei in Table EXTRANPARAMETERSATZ in Databasefile " + databaseFile.getAbsolutePath());
            }
            rs.close();
            st.close();
        }
        return r;
    }

    public static void convertTimelinesToCSV(File idbfResult, File outputFile) throws FileNotFoundException, IOException, SQLException {
        HE_Database db = new HE_Database(idbfResult, true);
        db.convertTimelinesToCSV(outputFile);
    }

    public void convertTimelinesToCSV(File outputFile) throws FileNotFoundException, IOException, SQLException {
//todo: Timestamps can not be read in directly from SQLite database. Parse with sqliteDateTimeFormat.parse(rs.getString(ColumNAME))
        // Output file
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            if (!outputFile.createNewFile()) {
                throw new FileNotFoundException("Can not create File '" + outputFile.getAbsolutePath() + "'.");
            }
        }
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs;

            //Override existing file.
            FileOutputStream fos = new FileOutputStream(outputFile, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName("utf-8"));
            BufferedWriter bw = new BufferedWriter(osw);
            StringBuilder str = new StringBuilder();

            bw.write("OriginalFile: " + databaseFile.getAbsolutePath());
            bw.newLine();
            bw.write("TargetFile: " + outputFile.getAbsolutePath());
            bw.newLine();
            bw.write("Convertion Date: " + new Timestamp(System.currentTimeMillis()));
            bw.newLine();
            //Raingauge
            bw.write("***");
            bw.newLine();
            bw.write("Raingauge");
            bw.newLine();

            rs = st.executeQuery("SELECT REGENREIHE.REGENBEGINN,REGENREIHE.REGENENDE,REGENREIHE.INTERVALLBREITE,REGENREIHE.DATEN FROM REGENREIHE,REGENSCHREIBERZUORDNUNG WHERE REGENREIHE.ID=REGENSCHREIBERZUORDNUNG.REGENREIHEREF");//This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.
            rs.next();
            Timestamp start = rs.getTimestamp("REGENBEGINN");
            Timestamp ende = rs.getTimestamp("REGENENDE");
            int duration_min = (int) ((ende.getTime() - start.getTime()) / 60000);
            int intervall_min = rs.getInt("INTERVALLBREITE");
            bw.write(" Rain start: " + start.toString());
            bw.newLine();
            bw.write(" Rain end: " + ende);
            bw.newLine();
            bw.write(" Rain interval: " + intervall_min + " min");
            bw.newLine();

            byte[] blob = rs.getBytes("DATEN");
            double[] precipitation = Raingauge_Firebird.readPrecipitation(blob);
            bw.write(" Gauge [mm]: " + precipitation.length);
            bw.newLine();
            for (int i = 0; i < precipitation.length; i++) {
                if (i > 0) {
                    bw.write(";");
                }
                bw.write(precipitation[i] + "");
            }
            bw.newLine();
            bw.newLine();
            bw.flush();
            bw.write("***");
            bw.newLine();
            bw.write("Simulation results");
            bw.newLine();
            bw.newLine();
            //Simulation Date
            //Starttime
            rs = st.executeQuery("SELECT FIRST 1 ZEITPUNKT FROM LAU_GL_EL WHERE ID=(SELECT FIRST 1 ID FROM LAU_GL_EL )ORDER BY ZEITPUNKT");
            rs.next();
            Timestamp starttime = rs.getTimestamp(1);
            bw.write("Scenario Start Date: " + starttime.toString());
            bw.newLine();
            rs.close();
            //Endtime
            rs = st.executeQuery("SELECT FIRST 1 ZEITPUNKT FROM LAU_GL_EL WHERE ID=(SELECT FIRST 1 ID FROM LAU_GL_EL )ORDER BY ZEITPUNKT DESC");
            rs.next();
            Timestamp endtime = rs.getTimestamp(1);
            bw.write("Scenario End Date: " + endtime.toString());
            bw.newLine();
            rs.close();
            //Timesteps
            rs = st.executeQuery("SELECT COUNT(ZEITPUNKT) FROM LAU_GL_EL WHERE ID=(SELECT FIRST 1 ID FROM LAU_GL_EL )");
            rs.next();
            int timesteps = rs.getInt(1);
            bw.write("Timesteps: " + timesteps);
            bw.newLine();
            rs.close();

            // Pipes 
            rs = st.executeQuery("SELECT COUNT(*) FROM ROHR");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            rs.next();
            bw.write("Pipes: " + rs.getInt(1));
            rs.close();
//            st.close();

            bw.newLine();
            //Manholes
            rs = st.executeQuery("SELECT COUNT(*) FROM SCHACHT");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
            rs.next();
            bw.write("Manholes: " + rs.getInt(1));
            bw.newLine();
            rs.close();
//            st.close();
            bw.flush();
            bw.newLine();
            //Read Values Pipes
            bw.write("***");
            bw.newLine();
            bw.write("PipeID;PipeName; velocities [m/s]");
            bw.newLine();
            rs = st.executeQuery("SELECT ZEITPUNKT, ID, KANTE, GESCHWINDIGKEIT FROM LAU_GL_EL ORDER BY ID,ZEITPUNKT");
            int lastID = -1;
            while (rs.next()) {
                int actID = rs.getInt(2);
                if (actID != lastID) {
                    //New information is here. Write old information
                    bw.write(str.toString());
                    bw.newLine();
                    bw.flush();
                    lastID = actID;
                    str = new StringBuilder(rs.getInt(2) + ";" + rs.getString(3));
                }
                str.append(';').append(rs.getFloat(4));
            }
            bw.write(str.toString());
            bw.newLine();
            bw.flush();
            str = new StringBuilder();
            rs.close();
            //Read Values Manholes
            bw.newLine();
            bw.write("***");
            bw.newLine();
            bw.write("ManholeID;ManholeName; waterheight [m]");
            bw.newLine();
            rs = st.executeQuery("SELECT ZEITPUNKT, ID, KNOTEN, WASSERSTAND FROM LAU_GL_S ORDER BY ID,ZEITPUNKT");
            lastID = -1;
            while (rs.next()) {
                int actID = rs.getInt(2);
                if (actID != lastID) {
                    //New information is here. Write old information
                    bw.write(str.toString());
                    bw.newLine();
                    bw.flush();
                    lastID = actID;
                    str = new StringBuilder(rs.getInt(2) + ";" + rs.getString(3));
                }
                str.append(';').append(rs.getFloat(4));
            }
            bw.write(str.toString());
            bw.newLine();
            bw.flush();
            rs.close();
            st.close();
            bw.close();
            osw.close();
            fos.close();

        }
    }

    /**
     * Returns the minimum and maximum velocity in the given ResultDB. First
     * index is the runningIndex, The ID of the Pipe can be found in 0st column
     * (must ba cast to integer),
     *
     * [row],[0:ID,1:min, 2:max]
     *
     * @return [row],[0:ID,1:min, 2:max]
     * @throws java.sql.SQLException
     */
    public float[][] getMinMaxVelocity() throws SQLException {
        if (con.isClosed()) {
            try {
                con = getConnection();
            } catch (IOException ex) {
                Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return getMinMaxVelocity(con.createStatement());
    }

    /**
     * Returns the minimum and maximum velocity in the given ResultDB. First
     * index is the runningIndex, The ID of the Pipe can be found in 0st column
     * (must ba cast to integer),
     *
     * [row],[0:ID,1:min, 2:max]
     *
     * @param st Statement
     * @return
     * @throws java.sql.SQLException
     */
    public static float[][] getMinMaxVelocity(Statement st) throws SQLException {
        ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT ID) FROM LAU_GL_EL");
        rs.next();
        int rows = rs.getInt(1);
        float[][] minmax = new float[rows][3];
        rs.close();
        rs = st.executeQuery("SELECT ID,MIN(GESCHWINDIGKEIT),MAX(GESCHWINDIGKEIT) FROM LAU_GL_EL GROUP BY ID");
        int i = 0;
        while (rs.next()) {
            int id = rs.getInt(1);
            float min = rs.getFloat(2);
            float max = rs.getFloat(3);
            minmax[i][0] = id + 0.1f;
            minmax[i][1] = min;
            minmax[i][2] = max;
            i++;
        }
        return minmax;
    }

    public float[] loadTimeLineValuesPipe(long pipeMaualID, String pipeName, int numberOfTimes, String columname) {
        float[] values = new float[numberOfTimes];
        try {
            synchronized (synchronizationObject) {
                Connection c = getConnection();
                Statement st = c.createStatement();
                //in HE Database only use Pipe ID.
                ResultSet rs = st.executeQuery("SELECT " + columname + ",ID,ZEITPUNKT FROM LAU_GL_EL WHERE ID=" + pipeMaualID + " ORDER BY ZEITPUNKT ");
                if (!rs.isBeforeFirst()) {
                    //No Data
                    System.err.println(getClass() + " could not load " + columname + " values for pipe id:" + pipeMaualID + ". It is not found in the database.");
                    return values;
                }
                int index = 0;
                while (rs.next()) {
                    values[index] = rs.getFloat(1);
                    index++;
                }
                return values;

            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public float[] loadTimeLineVelocity(long pipeMaualID, String pipeName, int numberOfTimes) {
        return loadTimeLineValuesPipe(pipeMaualID, pipeName, numberOfTimes, "GESCHWINDIGKEIT");
    }

    @Override
    public float[] loadTimeLineWaterlevel(long pipeMaualID, String pipeName, int numberOfTimes) {
        return loadTimeLineValuesPipe(pipeMaualID, pipeName, numberOfTimes, "WASSERSTAND");
    }

    @Override
    public float[] loadTimeLineFlux(long pipeMaualID, String pipeName, int numberOfTimes) {
        return loadTimeLineValuesPipe(pipeMaualID, pipeName, numberOfTimes, "DURCHFLUSS");
    }

    @Override
    public float[] loadTimeLineMass(long pipeMaualID, String pipeName, int numberOfTimes) {
        System.err.println(getClass() + ": loadTimeLineMass Not supported yet.");
        return new float[numberOfTimes];
    }

    @Override
    public long[] loadTimeStepsNetwork() {
        try {
            Statement st = getConnection().createStatement();
            ResultSet res;
            res = st.executeQuery("SELECT COUNT(DISTINCT ZEITPUNKT) FROM LAU_GL_EL;");
            res.next();
            int zeiteintraege = res.getInt(1);
            if (zeiteintraege < 1) {
                throw new NullPointerException("Database '" + databaseFile.getAbsolutePath() + "' has no timesteps for pipe elements.");
            }
            res.close();
            //Read timesteps
            long[] times = new long[zeiteintraege];
            res = st.executeQuery("SELECT ZEITPUNKT FROM LAU_GL_EL GROUP BY ZEITPUNKT;");
            int i = 0;
            while (res.next()) {
                if (isSQLite) {
                    times[i] = sqliteDateTimeFormat.parse(res.getString(1)).getTime();
                } else {
                    times[i] = res.getTimestamp(1).getTime();
                }
                i++;
            }
            return times;
        } catch (IOException ex) {
            throw new NullPointerException(ex.getMessage());
        } catch (SQLException ex) {
            throw new NullPointerException(ex.getMessage());
        } catch (ParseException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public float[] loadTimeLineWaterheightManhole(long manholeMaualID, String manholeName, int numberOfTimes) {
        float[] values = new float[numberOfTimes];
        try {
            synchronized (synchronizationObject) {
                Connection c = getConnection();
                Statement st = c.createStatement();
                //in HE Database only use Manhole ID.
                ResultSet rs = st.executeQuery("SELECT WASSERSTAND,ID,ZEITPUNKT FROM LAU_GL_S WHERE ID=" + manholeMaualID + " ORDER BY ZEITPUNKT ");
                if (!rs.isBeforeFirst()) {
                    //No Data
                    System.err.println(getClass() + " could not load wasserstand for manhole id:" + manholeMaualID + ". It is not found in the database.");
                    return values;
                }
                int index = 0;
                while (rs.next()) {
                    values[index] = rs.getFloat(1);
                    index++;
                }
                return values;

            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public float[] loadTimeLineSpilloutFlux(long manholeID, String manholeName, int numberOfTimes) {
        // Read Spillout Flow from Manhole to Surface
        //NEW SCHEME INCLUDING 2D SURFACE
        synchronized (synchronizationObject) {
            try {
                Connection c = getConnection();
                Statement st = c.createStatement();
                //ABFLUSS: Flow from Surface to Pipesystem
                ResultSet res = st.executeQuery("SELECT ID,"
                        + "KNOTEN, "
                        + "ZEITPUNKT, "
                        + "(ABFLUSS-ZUFLUSS) AS NETTO "
                        + "FROM KNOTENLAUFEND2D "
                        + "WHERE ID=" + manholeID + " "
                        + "ORDER BY  ZEITPUNKT;");
                float[] flux = new float[numberOfTimes];
                int index = 0;
                while (res.next()) {
                    flux[index] = res.getFloat(4);
                    index++;
                }
                return flux;

            } catch (SQLException | IOException ex) {
                Logger.getLogger(HE_Database.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return new float[numberOfTimes];
    }

    public String[] getOverspillingManholes(double minFlux) {
        LinkedList<String> names = new LinkedList<>();
        synchronized (synchronizationObject) {
            try {
                Connection c = getConnection();
                Statement st = c.createStatement();
                //ABFLUSS: Flow from Surface to Pipesystem
                ResultSet res = st.executeQuery("SELECT DISTINCT ID,KNOTEN FROM KNOTENLAUFEND2D WHERE ABFLUSS-ZUFLUSS>" + minFlux + " ORDER BY ID");
                while (res.next()) {
                    names.add(res.getString(2));
                }
            } catch (SQLException | IOException ex) {
                Logger.getLogger(HE_Database.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    @Override
    public SparseTimelineManhole loadTimelineManhole(long manholeManualID, String manholeName, SparseTimeLineManholeContainer container) {
        try {
            Connection c = getConnection();
            Statement st = c.createStatement();
            float[] waterZ = new float[container.getNumberOfTimes()];
            float[] spillflux = new float[container.getNumberOfTimes()];
            ResultSet rs = st.executeQuery("SELECT WASSERSTAND,ID,ZEITPUNKT FROM LAU_GL_S WHERE ID=" + manholeManualID + " ORDER BY ZEITPUNKT ");
            if (!rs.isBeforeFirst()) {
                //No Data
                System.err.println(getClass() + " could not load wasserstand for manhole id:" + manholeManualID + ". It is not found in the database.");
            }
            int index = 0;
            while (rs.next()) {
                waterZ[index] = rs.getFloat(1);
                index++;
            }
            rs = st.executeQuery("SELECT ID, KNOTEN,ZEITPUNKT, (ABFLUSS-ZUFLUSS) AS NETTO FROM KNOTENLAUFEND2D WHERE ID=" + manholeManualID + " ORDER BY  ZEITPUNKT;");
            if (!rs.isBeforeFirst()) {
                //No Data
                System.err.println(getClass() + " could not load spillflux for manhole id:" + manholeManualID + ". It is not found in the database.");
            }
            index = 0;
            while (rs.next()) {
                spillflux[index] = rs.getFloat(4);
                index++;
            }
            SparseTimelineManhole stlm = new SparseTimelineManhole(container, manholeManualID, manholeName);
            stlm.setSpilloutFlux(spillflux);
            stlm.setWaterHeight(waterZ);
            return stlm;

        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void loadTimelinePipes(Collection<Pipe> pipes, SparseTimeLinePipeContainer container) {
        if (pipes == null || pipes.isEmpty()) {
            return;
        }
        //Order Pipes
        ArrayList<Pipe> list = new ArrayList<>(pipes);
        Collections.sort(list, new Comparator<Pipe>() {
            @Override
            public int compare(Pipe t, Pipe t1) {
                if (t.getManualID() == t1.getManualID()) {
                    return 0;
                }
                if (t.getManualID() < t1.getManualID()) {
                    return -1;
                }
                return 1;
            }
        });

        try {
            Connection c = getConnection();
            Statement st = c.createStatement();
            //in HE Database only use Pipe ID.
            ResultSet rs = st.executeQuery("SELECT GESCHWINDIGKEIT,DURCHFLUSS,WASSERSTAND,KANTE,ID,ZEITPUNKT FROM LAU_GL_EL ORDER BY ID,ZEITPUNKT ");

            int lastID;

            float[] velocity = null, flux = null, waterlevel = null;
            Iterator<Pipe> it = list.iterator();
            Pipe p = null;

            //Initialize Timeline for first pipe
            p = it.next();
            velocity = new float[container.getNumberOfTimes()];
            flux = new float[container.getNumberOfTimes()];
            waterlevel = new float[container.getNumberOfTimes()];
            SparseTimelinePipe tl = new SparseTimelinePipe(container, p);
            p.setStatusTimeLine(tl);
            tl.setFlux(flux);
            tl.setVelocity(velocity);
            tl.setWaterlevel(waterlevel);

            //Go through the pipes to assemble and load only their values
//            rs.next();
            lastID = -1;//rs.getInt(5);

            int index = 0;
            boolean lastpipeTLset = false;
            while (true) {
                //Test if actual pair is matching 
                if (lastID == p.getManualID()) {
                    //If pipe id = resultset id. Read timeline values and store them
                    velocity[index] = rs.getFloat(1);
                    flux[index] = rs.getFloat(2);
                    waterlevel[index] = rs.getFloat(3);
                    lastpipeTLset = true;
                    index++;
                    if (!rs.next()) {
                        //this was the last resultset entry.
                        break;
                    }
                    lastID = rs.getInt(5);
                } else {
                    //If pipe ID != resultset id. Find next matching pair
                    if (p.getManualID() > lastID) {
                        while (rs.next()) {
                            lastID = rs.getInt(5);
                            if (lastID == p.getManualID()) {
                                //Found resultset that represents pipe
                                //continue loop and read timestamp values
                                break;
                            }
                        }
                    } else {
                        //pipe.manualid<resultset.id
                        // pipe is ready. 
                        // go to next pipe
                        if (it.hasNext()) {
                            if (!lastpipeTLset) {
                                System.out.println("No timeline set for Pipe " + p);
                            }
                            lastpipeTLset = false;
                            p = it.next();
                            tl = new SparseTimelinePipe(container, p);
                            p.setStatusTimeLine(tl);
                            velocity = new float[container.getNumberOfTimes()];
                            flux = new float[container.getNumberOfTimes()];
                            waterlevel = new float[container.getNumberOfTimes()];
                            tl.setFlux(flux);
                            tl.setVelocity(velocity);
                            tl.setWaterlevel(waterlevel);
                            index = 0;
                        } else {
                            //All pipes set
                            break;

                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void loadTimelineManholes(Collection<StorageVolume> manholes, SparseTimeLineManholeContainer container) {
        if (manholes == null || manholes.isEmpty()) {
            return;
        }
        boolean verboseNotFound = false;
        //Order Pipes
        ArrayList<StorageVolume> list = new ArrayList<>(manholes);
        Collections.sort(list, new Comparator<Capacity>() {
            @Override
            public int compare(Capacity t, Capacity t1) {
                if (t.getManualID() == t1.getManualID()) {
                    return 0;
                }
                if (t.getManualID() < t1.getManualID()) {
                    return -1;
                }
                return 1;
            }
        });

        try {
            Connection connection = getConnection();
            Statement st = connection.createStatement();
            //in HE Database only use Pipe ID.
            ResultSet rs = st.executeQuery("SELECT ID,ZEITPUNKT,WASSERSTAND FROM LAU_GL_S ORDER BY ID,ZEITPUNKT ");

            int lastID;

            float[] flux = null, waterlevel = null;
            Iterator<StorageVolume> it = list.iterator();
            Capacity c;

            //Go through the pipes to assemble and load only their values
//            rs.next();
            lastID = -1;
            c = it.next();
            SparseTimelineManhole tl = new SparseTimelineManhole(container, ((Manhole) c));
            ((Manhole) c).setStatusTimeline(tl);
            waterlevel = new float[container.getNumberOfTimes()];
            tl.setWaterHeight(waterlevel);

            int index = 0;
            boolean lastpipeTLset = false;
            while (true) {
                //Test if actual pair is matching 
                if (lastID == c.getManualID()) {
                    //If pipe id = resultset id. Read timeline values and store them
                    waterlevel[index] = rs.getFloat(3);
                    lastpipeTLset = true;
                    index++;
                    if (!rs.next()) {
                        //this was the last resultset entry.
                        break;
                    }
                    lastID = rs.getInt(1);
                } else {
                    //If pipe ID != resultset id. Find next matching pair
                    if (c.getManualID() > lastID) {
                        while (rs.next()) {
                            lastID = rs.getInt(1);
                            if (lastID == c.getManualID()) {
                                //Found resultset that represents pipe
                                //continue loop and read timestamp values
                                break;
                            }
                        }
                    } else {
                        //pipe.manualid<resultset.id
                        // pipe is ready. 
                        // go to next pipe
                        if (it.hasNext()) {
                            if (!lastpipeTLset) {
                                if (verboseNotFound) {
                                    System.out.println("No timeline.waterheight set for Manhole " + c);
                                }
                            }
                            lastpipeTLset = false;
                            c = it.next();
                            tl = new SparseTimelineManhole(container, ((Manhole) c));
                            ((Manhole) c).setStatusTimeline(tl);

                            waterlevel = new float[container.getNumberOfTimes()];
                            tl.setWaterHeight(waterlevel);
                            index = 0;
                        } else {
                            //All pipes set
                            break;
                        }
                    }
                }
            }
            rs.close();
            //Next: spillout fluxes
            {
                ResultSet rsF = st.executeQuery("SELECT ID,ZEITPUNKT, (ABFLUSS-ZUFLUSS) AS NETTO FROM KNOTENLAUFEND2D ORDER BY ID,ZEITPUNKT ");
                it = list.iterator();

                //Go through the pipes to assemble and load only their values
//            rs.next();
                lastID = -1;
                //Initialize Timeline for first capacity
                c = it.next();
                tl = (SparseTimelineManhole) ((Manhole) c).getStatusTimeLine();
                if (tl == null) {
                    tl = new SparseTimelineManhole(container, (StorageVolume) c);
                    ((Manhole) c).setStatusTimeline(tl);
                }
                flux = new float[container.getNumberOfTimes()];
                tl.setSpilloutFlux(flux);

                index = 0;
                lastpipeTLset = false;
                while (true) {
                    //Test if actual pair is matching 
                    if (lastID == c.getManualID()) {
                        //If pipe id = resultset id. Read timeline values and store them
                        try {
                            flux[index] = rsF.getFloat(3);
                        } catch (SQLException sQLException) {
                            System.err.println("lastID:" + lastID + "  manhole.id=" + c.getManualID());
                            sQLException.printStackTrace();
                        }
                        lastpipeTLset = true;
                        index++;
                        if (!rsF.next()) {
                            //this was the last resultset entry.
                            break;
                        }
                        lastID = rsF.getInt(1);
                    } else {
                        //If pipe ID != resultset id. Find next matching pair
                        if (c.getManualID() > lastID) {
                            while (rsF.next()) {
                                lastID = rsF.getInt(1);
                                if (lastID >= c.getManualID()) {
                                    //Found resultset that represents pipe
                                    //continue loop and read timestamp values
                                    break;
                                }
                            }
                            if (rsF.isClosed()) {
                                //Result set empty
                                System.out.println(getClass() + "::loadTimelineManholes: resultset is empty when searching for manhole with id:" + c.getManualID() + "  (resultset.id:" + lastID + ")");
                                break;
                            }
                        } else {
                            //pipe.manualid<resultset.id
                            // pipe is ready. 
                            // go to next pipe
                            if (it.hasNext()) {
                                if (!lastpipeTLset) {
                                    if (verboseNotFound) {
                                        System.out.println("No timeline.spilloutFlux set for Manhole " + c);
                                    }
                                }
                                lastpipeTLset = false;
                                c = it.next();
                                tl = (SparseTimelineManhole) ((Manhole) c).getStatusTimeLine();
                                flux = new float[container.getNumberOfTimes()];
                                tl.setSpilloutFlux(flux);
                                index = 0;
                            } else {
                                //All pipes set
                                break;

                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SparseTimelinePipe loadTimelinePipe(long pipeManualID, String pipeName, SparseTimeLinePipeContainer container) {
        SparseTimelinePipe tl = new SparseTimelinePipe(container, pipeManualID, pipeName);

        fillTimelinePipe(pipeManualID, pipeName, tl);
        return tl;

    }

//    private boolean workingonpipe = false;
    @Override
    public boolean fillTimelinePipe(long pipeManualID, String pipeName, SparseTimelinePipe timeline) {
//        if (workingonpipe) {
//            System.out.println(Thread.currentThread().getName() + " has to wait to load Pipe " + pipeManualID);
//        }
        synchronized (synchronizationObject) {
            if (timeline.isInitialized()) {
                //Does not need to be loaded any more. another Thread did that for us, while this thread was waiting for the monitor.
                return true;
            }
//            workingonpipe = true;
//            System.out.println(Thread.currentThread().getName() + " starts loading Pipe " + pipeManualID);
            try {
                Connection c = getConnection();
                try ( //in HE Database only use Pipe ID.
                        Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT GESCHWINDIGKEIT,DURCHFLUSS,WASSERSTAND,ID,ZEITPUNKT FROM LAU_GL_EL WHERE ID=" + pipeManualID + " ORDER BY ZEITPUNKT ")) {
                    int times = timeline.getNumberOfTimes();
                    float[] velocity, flux, waterlevel;
                    velocity = new float[times];
                    flux = new float[times];
                    waterlevel = new float[times];
                    int index = 0;
                    while (rs.next()) {
                        velocity[index] = rs.getFloat(1);
                        flux[index] = rs.getFloat(2);
                        waterlevel[index] = rs.getFloat(3);
                        index++;
                    }
                    timeline.setVelocity(velocity);
                    timeline.setFlux(flux);
                    timeline.setWaterlevel(waterlevel);
                }
//                workingonpipe = false;
                return true;

            } catch (SQLException ex) {
                Logger.getLogger(HE_Database.class
                        .getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(HE_Database.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
//            workingonpipe = false;
            return false;
        }
    }

    public String[] readExtran2DscenarioNames() throws SQLException, IOException {
        LinkedList<String> names = new LinkedList<>();

        con = getConnection();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT NAME FROM EXTRAN2DPARAMETERSATZ ORDER BY NAME ASC")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        con.close();
        return names.toArray(new String[names.size()]);
    }

}
