package io.extran;

import com.vividsolutions.jts.geom.Coordinate;
import control.scenario.injection.HEInjectionInformation;
import control.scenario.injection.HE_AreaInjection;
import control.scenario.injection.HE_MessdatenInjection;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.particle.Material;
import model.timeline.TimedValue;
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

    public static boolean registeredFirebird = false;

    public static boolean registeredSQLite = false;

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

    protected final ArrayList<ThreadConnection> threadConnections = new ArrayList<>(2);

    private final HEConnectionSerializer serializer = new HEConnectionSerializer();

    protected boolean readOnly = false;

    private java.util.Properties connectionProperties;

    protected boolean isSQLite = false;

    protected int numberOfMaterials = -1;

//    private final String synchronizationObject = "sync";
    /**
     * Can parse text based timestamps in sqlite format.
     */
    protected DateFormat sqliteDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static boolean register() {

        return true;
    }

    public static void registerFirebirdDriver() {
        try {
            Class.forName("org.firebirdsql.jdbc.FBDriver").newInstance();
            registeredFirebird = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Firebird driver could not be registered. Maybe jar file is not given.");

        } catch (InstantiationException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void registerSQLDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            registeredSQLite = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    public HE_Database(File databaseFile) throws IOException {
        this(databaseFile, true);
    }

    public HE_Database(File databaseFile, boolean readonly) throws IOException {

        String name = databaseFile.getAbsolutePath().toLowerCase();
        if (name.endsWith(".idbf")) {
            //Open Firebird Database

            //Load JDBC Drivers
            if (!registeredFirebird) {
                registerFirebirdDriver();
                if (!registeredFirebird) {
                    try {
                        throw new ClassNotFoundException("Firebird Database driver not accessable. Please make sure a file like 'jaybird-full-3.0.5.jar' is in the lib folder.");
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
                        if (f.exists() && !f.canWrite()) {
                            f = new File(workingdirectory, localCopyName + "_01.idbf");
                        }
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
//                        System.out.println(f.getAbsolutePath()+" readable?"+f.canRead()+"  write?"+f.canWrite());
                        boolean successCopy = false;
//                        if (f.canWrite() && databaseFile.canRead()) {
//                        System.out.println("start copy file to " + f.getAbsolutePath() + " from " + databaseFile);
//                        long start = System.currentTimeMillis();
                        try {
                            Files.copy(databaseFile.toPath(), f.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                            if (verbose) {
                                System.out.println("Copied file to " + f.getAbsolutePath() + " from " + databaseFile.getAbsolutePath());
                            }
                            successCopy = true;
                        } catch (Exception exception) {
//                            exception.printStackTrace();
                            System.out.println("cannot access temporal file '" + f.toPath() + "'  readable:" + f.canRead() + ", writable:" + f.canWrite() + ", deletable:" + f.delete());
                            successCopy = false;
                        }
//                        }
                        if (!successCopy) {
                            for (int i = 1; i < 10; i++) {
                                File testf = new File(f.getParentFile(), f.getName() + "_" + i + "." + f.getName().substring(f.getName().length() - 4));
                                if (!testf.exists()) {
                                    f = testf;
                                    break;
                                } else {
                                    if (testf.delete()) {
                                        f = testf;
                                        break;
                                    }
                                }
                            }

                            System.err.println("Need to create new temporary file: " + f);
                            try {
                                Files.copy(databaseFile.toPath(), f.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                                if (verbose) {
                                    System.out.println("Copied file to " + f.getAbsolutePath() + " from " + databaseFile.getAbsolutePath());
                                }
                                successCopy = true;
                            } catch (Exception exception) {

                                exception.printStackTrace();
                                successCopy = false;
                            }
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
//                    System.out.println("start copy file to " + temporalPath.toAbsolutePath() + " from " + databaseFile);
//                    long start = System.currentTimeMillis();
                    Files.copy(databaseFile.toPath(), temporalPath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
//                    System.out.println("Copied file in " + ((System.currentTimeMillis() - start) / 1000) + "s.");

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
//                System.out.println("open SQLite connection...");
//                long start = System.currentTimeMillis();
                if (!registeredSQLite) {
                    registerSQLDriver();
                    if (!registeredSQLite) {
                        try {
                            throw new ClassNotFoundException("SQLite Database driver not accessable. Please make sure a file like 'sqlite-jdbc-3.16.1.jar' is in the lib folder.");
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                SQLiteConfig config = new SQLiteConfig();
                config.setEncoding(SQLiteConfig.Encoding.UTF8);
                con = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), config.toProperties());
                this.databaseFile = databaseFile;
                this.localFile = databaseFile;
//                System.out.println("Opened SQLite connection after " + ((System.currentTimeMillis() - start) / 1000) + "s.");
            } catch (SQLException ex) {
                Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            throw new IOException("Can not identify '" + databaseFile.getName() + "' as HYSTEM EXTRAN database file");
        }
    }

    public void close() throws SQLException {
        try {
            if (con != null && !con.isClosed()) {
//            System.out.println("close main connection");
                con.close();
                con = null;
            }
            for (ThreadConnection threadConnection : threadConnections) {
                if (threadConnection != null && threadConnection.con != null && !threadConnection.con.isClosed()) {
//                System.out.println("close threadconnection");
                    threadConnection.con.close();
                    threadConnection.con = null;
                }
            }
            threadConnections.clear();
        } catch (Exception exception) {
            exception.printStackTrace();
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
        synchronized (this) {
            if (con == null || con.isClosed()) {
                if (isSQLite) {
//                    System.out.println("reopen SQLite connection...");
//                    long start = System.currentTimeMillis();
//                try {
//                    Class.forName("org.sqlite.JDBC");
                    SQLiteConfig config = new SQLiteConfig();
                    config.setEncoding(SQLiteConfig.Encoding.UTF8);
                    config.setReadOnly(this.readOnly);
                    con = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), config.toProperties());
//                    System.out.println("Opened SQLite connection after " + ((System.currentTimeMillis() - start) / 1000) + "s.");

                    return con;
//                } catch (ClassNotFoundException ex) {
//                    Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
//                }
                } else {
                    //Firebird

                    try {
                        if (this.localFile == null) {
                            System.err.println("local File is null: ");
                        }
                        String url = "jdbc:firebirdsql:embedded:" + this.localFile.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();
                        con = DriverManager.getConnection(url, connectionProperties);
                        con.setReadOnly(this.readOnly);
//                        System.out.println("created new FNConnection " + con);
                        return con;
                    } catch (SQLException e) {
                        throw new IOException("Remote File '" + localFile.getAbsolutePath() + "' can not be used directly. Copy to local or temporal file did not work. " + e.getLocalizedMessage());
                    }
                }
            }

        }
        return con;
    }

    /**
     * Returns an open or reactivates a closed connection. connection is already
     * locked for htreadsave queries. has to be unlocked manually at the end of
     * the transaction.
     *
     * @param idToLoad (e.g. PipeID /ManholeID)
     * @return a Read-only Connection
     * @throws SQLException
     * @throws IOException
     */
    public ThreadConnection getUnusedConnection(long idToLoad) throws SQLException, IOException {
//        synchronized (serializer) {
        for (ThreadConnection threadConnection : threadConnections) {
//            if (threadConnection.con == null) {
//                continue;
//            }
            if (threadConnection.id == idToLoad) {
//                    System.out.println(" return inuse connection for "+idToLoad);
                threadConnection.lock.lock();
                return threadConnection;
            }
        }
        for (ThreadConnection threadConnection : threadConnections) {
//            if (threadConnection.con == null) {
//                continue;
//            }
            if (threadConnection.lock.tryLock()) {
                threadConnection.id = idToLoad;
//                    System.out.println(" return free connection for "+idToLoad);
                return threadConnection;
            }
        }
//            System.out.println("need another connection on top of the existing " + threadConnections.size());
        //No free threadconnection was found. create a new one
        ThreadConnection toAdd = null;
        if (isSQLite) {
            SQLiteConfig config = new SQLiteConfig();
            config.setEncoding(SQLiteConfig.Encoding.UTF8);
            config.setReadOnly(true);
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath(), config.toProperties());
            if (c == null) {
//                System.err.println("Problem with connection to " + databaseFile.getAbsolutePath());
                throw new NullPointerException("Cannot create Connection to 'jdbc:sqlite:" + databaseFile.getAbsolutePath() + "'");
            } else {
                toAdd = new ThreadConnection(c);
            }
        } else {
            //Firebird
            try {
                if (this.localFile == null) {
                    System.err.println("local File is null: ");
                }
                String url = "jdbc:firebirdsql:embedded:" + this.localFile.getAbsolutePath().replaceAll("\\\\", "/").toLowerCase();
                Connection c = DriverManager.getConnection(url, connectionProperties);
                c.setReadOnly(true);
                toAdd = new ThreadConnection(c);
            } catch (Exception e) {
                throw new IOException("Remote File '" + localFile.getAbsolutePath() + "' can not be used directly. Copy to local or temporal file did not work. " + e.getLocalizedMessage());
            }
        }
        if (toAdd != null) {
            toAdd.lock.lock();
            synchronized (threadConnections) {
                this.threadConnections.add(toAdd);
                toAdd.id = idToLoad;
            }
//            System.out.println("add new HE Connection" + this.threadConnections.size());
            return toAdd;
        }
//        }
        throw new NullPointerException("Could not create a new Connection to the HYSTEM EXTRAN database.");
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
        if (!isSQLite) {
            //Firebird database does not provide CRS information.
            return null;
        }
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
            if (verbose) {
                exception.printStackTrace();
            }
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
//                    System.out.println("Axis: "+crsDB.getCoordinateSystem().getAxis(0).toString());
                    try {
                        if (crsDB != null && crsDB.getCoordinateSystem().toString().contains("UoM: m.") || crsDB.getCoordinateSystem().toString().contains("UTM")) {
                            if (verbose) {
                                System.out.println(this.getClass() + "::loadNetwork: Datenbank speichert als UTM " + crsDB.getCoordinateSystem().getName());
                            }
                            Network.crsUTM = crsDB;
                        } else {
                            System.out.println("tested Sring:'" + crsDB.getCoordinateSystem().toString() + "'");
                            System.out.println(this.getClass() + "::loadNetwork: Coordinatensystem der Datenbank ist nicht cartesisch: " + crsDB);
                            Network.crsUTM = CRS.decode("EPSG:25832");
                        }
                    } catch (Exception exception) {
                        System.out.println("axis:" + crsDB.getCoordinateSystem().getAxis(0));
                        System.out.println("unit:" + crsDB.getCoordinateSystem());
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
                    try {
                        res = st.executeQuery("SELECT name,geometry,DURCHMESSER,gelaendehoehe,deckelhoehe,sohlhoehe,ID,KANALART from schacht ORDER BY ID;");
                    } catch (SQLException sqlex) {
                        res = st.executeQuery("SELECT name,geometry,Oberflaeche,gelaendehoehe,deckelhoehe,sohlhoehe,ID,KANALART from schacht ORDER BY ID;");
                    }
                    if (!res.isBeforeFirst()) {
                        System.err.println("Database " + databaseFile + "has no Manhole elements.");
                    }

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
                    boolean hasDiameter = true;
                    try {
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,DURCHMESSER,gelaendehoehe,deckelhoehe,sohlhoehe,ID from schacht ORDER BY ID;");
                    } catch (SQLException sQLException) {
                        System.err.println("File '" + databaseFile.getAbsolutePath() + "' doesnot contain topological information");
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,OBERFLAECHE,gelaendehoehe,deckelhoehe,sohlhoehe,ID from schacht ORDER BY ID;");
                        hasDiameter = false;
                        System.err.println("Networkmodel has no information about manhole diameter.");
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

                            Profile p;
                            if (hasDiameter) {
                                int durchmesser = res.getInt(4);
                                p = schachtprofile.get(durchmesser);
                                if (p == null) {
                                    p = new CircularProfile(durchmesser / 1000.);
                                    schachtprofile.put(durchmesser, p);
                                }
                            } else {
                                //standarddiameter 1m
                                p = schachtprofile.get(1);
                                if (p == null) {
                                    p = new CircularProfile(1);
                                    schachtprofile.put(1, p);
                                }
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
                //Begin Speicherschacht Storagevolume
                if (isSQLite) {
                    try {
                        res = st.executeQuery("SELECT name,geometry,TYP,gelaendehoehe,HoeheVollfuellung,sohlhoehe,ID,ART from SPEICHERSCHACHT ORDER BY ID;");
                    } catch (SQLException sqlex) {
//                        res = st.executeQuery("SELECT name,geometry,Oberflaeche,gelaendehoehe,scheitelhoehe,sohlhoehe,ID,KANALART from SPEICHERSCHACHT ORDER BY ID;");
                        sqlex.printStackTrace();
                    }
                    if (!res.isBeforeFirst()) {
//                        System.err.println("Database has no SPEICHERSCHACHT elements.");
                    }

                    while (res.next()) {
                        String name = res.getString(1);
                        byte[] buffer = res.getBytes(2);
                        Coordinate coordDB = decodeCoordinate(buffer);
                        Coordinate coordUTM;
                        try {
                            coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM); //gt.toGlobal(coord);

                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS); //gt.toGlobal(coord);

                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);

                            Profile p = schachtprofile.get(1);
                            if (p == null) {
                                p = new CircularProfile(1);
                                schachtprofile.put(1, p);
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
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,gelaendehoehe,HoeheVollfuellung,sohlhoehe,ID from SPEICHERSCHACHT ORDER BY ID;");
                    } catch (SQLException sQLException) {
                        System.err.println("Networkmodel has no information about manhole diameter.");
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

                            Profile p;
                            //standarddiameter 1m
                            p = schachtprofile.get(1);
                            if (p == null) {
                                p = new CircularProfile(1);
                                schachtprofile.put(1, p);
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
                // ende SpeicherSchacht abfrage
                //Begin Versickerungsschacht Storagevolume
                if (isSQLite) {
                    try {
                        res = st.executeQuery("SELECT name,geometry,TYP,gelaendehoehe,HoeheVollfuellung,sohlhoehe,ID,ART from Versickerungselement ORDER BY ID;");
                    } catch (SQLException sqlex) {
//                        res = st.executeQuery("SELECT name,geometry,Oberflaeche,gelaendehoehe,scheitelhoehe,sohlhoehe,ID,KANALART from SPEICHERSCHACHT ORDER BY ID;");
                        sqlex.printStackTrace();
                    }
                    if (!res.isBeforeFirst()) {
//                        System.err.println("Database has no Verickerungselements.");
                    }

                    while (res.next()) {
                        String name = res.getString(1);
                        byte[] buffer = res.getBytes(2);
                        Coordinate coordDB = decodeCoordinate(buffer);
                        Coordinate coordUTM;
                        try {
                            coordUTM = SHP_IO_GULLI.transform(coordDB, transformDB_UTM); //gt.toGlobal(coord);

                            Coordinate coordWGS = SHP_IO_GULLI.transform(coordDB, transformDB_WGS); //gt.toGlobal(coord);

                            Position position = new Position(coordWGS.x, coordWGS.y, coordUTM.x, coordUTM.y);

                            Profile p = schachtprofile.get(1);
                            if (p == null) {
                                p = new CircularProfile(1);
                                schachtprofile.put(1, p);
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
                        res = st.executeQuery("SELECT name,XKOORDINATE,YKOORDINATE,gelaendehoehe,HoeheVollfuellung,sohlhoehe,ID from Versickerungselement ORDER BY ID;");
                    } catch (SQLException sQLException) {
                        System.err.println("Networkmodel has no information about manhole diameter.");
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

                            Profile p;
                            //standarddiameter 1m
                            p = schachtprofile.get(1);
                            if (p == null) {
                                p = new CircularProfile(1);
                                schachtprofile.put(1, p);
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
                // ende Versickerungsschacht abfrage
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

                ////   Pumps
                ////
                res = st.executeQuery("SELECT name,schachtoben,schachtunten,typ,ID from PUMPE;");
//                c = res.getMetaData().getColumnCount();
                while (res.next()) {
                    String name = res.getString(1);
                    String nameoben = res.getString(2);
                    String nameunten = res.getString(3);
                    Manhole mhoben = smap.get(nameoben);
                    Manhole mhunten = smap.get(nameunten);

                    if (mhoben == null) {
                        System.err.println(HE_Database.class
                                + "::Can not find upper manhole '" + nameoben + "' for pump " + name);

                        continue;
                    }
                    if (mhunten == null) {
                        System.err.println("Can not find lower manhole '" + nameunten + "' for pump " + name);
                        continue;
                    }

                    Profile p = schachtprofile.get(1);
                    if (p == null) {
                        p = new CircularProfile(1);
                        schachtprofile.put(1, p);
                    }
                    //Verbindungen anlegen
                    //Define the Position of Connections
                    model.topology.Connection_Manhole_Pipe upper, lower;
                    //Do not change the direction of Pipes, also not if they are pointed against gravity flow.
                    upper = new model.topology.Connection_Manhole_Pipe(mhoben.getPosition(), mhoben.getSole_height());
                    lower = new model.topology.Connection_Manhole_Pipe(mhunten.getPosition(), mhunten.getSole_height());

                    Pipe pipe = new Pipe(upper, lower, p);
                    pipe.setName(name);

                    mhoben.addConnection(upper);
                    mhunten.addConnection(lower);

                    // fertig Connections zwischen Schacht und haltung eingefügt
                    String watertype = res.getString(4);
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
                    //Database does not give a length for a pump because there is almost no storage in such pipes.
                    pipe.setLength(1f);//(float) upper.getPosition().distance(lower.getPosition()));
                    pipe.setManualID(res.getInt(5));

                    pipes_sewer.add(pipe);
                }
                res.close();
                ////   Weirs
                ////
                res = st.executeQuery("SELECT name,schachtoben,schachtunten,typ,ID,SCHWELLENHOEHE from WEHR;");
//                c = res.getMetaData().getColumnCount();
                while (res.next()) {
                    String name = res.getString(1);
                    String nameoben = res.getString(2);
                    String nameunten = res.getString(3);
                    Manhole mhoben = smap.get(nameoben);
                    Manhole mhunten = smap.get(nameunten);

                    if (mhoben == null) {
                        System.err.println(HE_Database.class
                                + "::Can not find upper manhole '" + nameoben + "' for weir " + name);

                        continue;
                    }
                    if (mhunten == null) {
                        System.err.println("Can not find lower manhole '" + nameunten + "' for weir " + name);
                        continue;
                    }

                    Profile p = schachtprofile.get(1);
                    if (p == null) {
                        p = new CircularProfile(1);
                        schachtprofile.put(1, p);
                    }
                    //Verbindungen anlegen
                    //Define the Position of Connections
                    model.topology.Connection_Manhole_Pipe upper, lower;
                    //Do not change the direction of Pipes, also not if they are pointed against gravity flow.
                    upper = new model.topology.Connection_Manhole_Pipe(mhoben.getPosition(), res.getFloat(6));
                    lower = new model.topology.Connection_Manhole_Pipe(mhunten.getPosition(), mhunten.getSole_height());

                    Pipe pipe = new Pipe(upper, lower, p);
                    pipe.setName(name);

                    mhoben.addConnection(upper);
                    mhunten.addConnection(lower);

                    // fertig Connections zwischen Schacht und haltung eingefügt
                    String watertype = res.getString(4);
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
                    pipe.setLength((float) upper.getPosition().distance(lower.getPosition()));
                    pipe.setManualID(res.getInt(5));

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

    /**
     * Reads the coordinate of a SQLite Geometry Point
     *
     * @param byteblob
     * @return
     */
    public static Coordinate decodeCoordinate(byte[] byteblob) {
        ByteBuffer bb = ByteBuffer.wrap(byteblob);
        bb.order(ByteOrder.LITTLE_ENDIAN);

//        System.out.println("Buffersize:" + byteblob.length + ", capacity:" + bb.capacity());// + " : " + bb.asFloatBuffer().get(0) + " , " + bb.asDoubleBuffer().get(1));
        for (int i = 0; i < bb.capacity(); i++) {
            bb.position(i);
//            System.out.println(i + ": " + bb.getDouble() + "\t" + bb.getInt(i) + "\t" + bb.getFloat(i) + "\t" + bb.getLong(i) + "\t" + bb.getShort(i));
        }
        bb.position(6);
        double x = bb.getDouble();
        bb.position(14);
        double y = bb.getDouble();

        return new Coordinate(x, y);
    }

    /**
     * Encodes the coordinate into a SQLite byte blob representing the SQLite
     * Geometry Point
     *
     * @param x
     * @param y
     * @return
     */
    public static byte[] encodeCoordinate(double x, double y) {
        ByteBuffer bb = ByteBuffer.allocate(60);//I don't know why we have to allocate 60 bytes und write x and y coordinate 3 times.
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putDouble(6, x);
        bb.putDouble(14, y);
        bb.putDouble(22, x);
        bb.putDouble(30, y);
        bb.putDouble(43, x);
        bb.putDouble(51, y);

        return bb.array();
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
                            ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setWaterLevel(h - mh.getSole_height(), timeIndex);
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
                    tl.setDischarge(q, timeIndex);
                    tl.setVolume((float) (pipe.getProfile().getFlowArea(h) * pipe.getLength()), timeIndex);
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
            res = st.executeQuery("SELECT KANTESTOFFLAUFEND.ID,KANTESTOFFLAUFEND.KANTE, KANTESTOFFLAUFEND.ZEITPUNKT,(KONZENTRATION * DURCHFLUSS)/ 1000 AS FRACHTKGPS,KONZENTRATION, AUSLASTUNG, STOFF"
                    + " FROM KANTESTOFFLAUFEND INNER JOIN LAU_GL_EL ON KANTESTOFFLAUFEND.KANTE = LAU_GL_EL.KANTE AND KANTESTOFFLAUFEND.ZEITPUNKT = LAU_GL_EL.ZEITPUNKT"
                    + " ORDER BY KANTESTOFFLAUFEND.KANTE,STOFF,LAU_GL_EL.ZEITPUNKT");
            // Konzentration mg/l = g/m^3
            // Frachtrate  kg/s
            id = Integer.MIN_VALUE;
            int indexMaterial = 0;
            String materialName = "";
//            System.out.println("Leses schmutzfracht ein");
            while (res.next()) {
                int heID = res.getInt(1);
                String heName = res.getString(2);
                String stoffName = res.getString(7);
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
                double concentration = res.getDouble(5);
                if (id != heID) {
                    pipe = net.getPipeByName(heName);
                    id = heID;
                    indexMaterial = 0;
                    materialName = stoffName;
                }
                if (!stoffName.equals(materialName)) {
                    indexMaterial++;
                    materialName = stoffName;
                }

                timeIndex = ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).container.getTimeIndex(time);
                ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).setMassflux_reference((float) frachtrate, timeIndex, indexMaterial);
                ((ArrayTimeLinePipe) pipe.getStatusTimeLine()).setConcentration_Reference((float) concentration, timeIndex, indexMaterial);
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
                ((ArrayTimeLineManhole) mh.getStatusTimeLine()).setWaterLevel(h - mh.getSole_height(), timeIndex);
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
        ResultSet rs = st.executeQuery("SELECT BERICHTANFANG,BERICHTENDE from EXTRANPARAMETERSATZ;");
        if (!rs.isBeforeFirst()) {
            //Resultset is empty
            rs.close();
            st.close();
            con.close();
            return new ArrayList<>(0);
        }
        rs.next();
        GregorianCalendar gcdaystart = new GregorianCalendar();
        GregorianCalendar simulationStart = new GregorianCalendar();
        GregorianCalendar simulationEnde = new GregorianCalendar();
        if (isSQLite) {
//            System.out.println(rs.getString(1));
            long time = sqliteDateTimeFormat.parse(rs.getString(1)).getTime();
            gcdaystart.setTimeInMillis(time);
            simulationStart.setTimeInMillis(time);
            time = sqliteDateTimeFormat.parse(rs.getString(2)).getTime();
            simulationEnde.setTimeInMillis(time);
        } else {
            Timestamp time = rs.getTimestamp(1);
            gcdaystart.setTimeInMillis(time.getTime());
            simulationStart.setTimeInMillis(time.getTime());
            time = rs.getTimestamp(2);
            simulationEnde.setTimeInMillis(time.getTime());
        }
        gcdaystart.set(GregorianCalendar.HOUR_OF_DAY, 0);
        gcdaystart.set(GregorianCalendar.MINUTE, 0);
        gcdaystart.set(GregorianCalendar.SECOND, 0);
        gcdaystart.set(GregorianCalendar.MILLISECOND, 0);
        Timestamp daystart = new Timestamp(gcdaystart.getTimeInMillis());
        rs.close();

        ArrayList<HEInjectionInformation> particles = new ArrayList<>();
        HashMap<Integer, Material> materialMap = new HashMap<>();
//        System.out.println("Load injections from Database . sql?"+isSQLite);
        if (isSQLite) {
            rs = st.executeQuery("SELECT ID,NAME,ABSETZWIRKUNG FROM STOFFGROESSE");
            if (!rs.isBeforeFirst()) {
                //Is empty
                return particles;
            }

            while (rs.next()) {
                Material m = new Material(rs.getString(2), 1000, true);
                materialMap.put(rs.getInt(1), m);
            }
            rs.close();

            //get joins of substance and injectionlocation
            String qstring = "SELECT ROHR, ROHRREF,ZUFLUSS,ZUFLUSSDIREKT,EINZELEINLEITER.ZEITMUSTERREF as EINZELMUSTERREF,STOFFRef,KONZENTRATION,STOFFEINZELEINLEITEr.ZEITMUSTERREF AS STOFFMUSTERREF, Messdaten, MessdatenRef FROM EINZELEINLEITER JOIN StoffEINZELEINLEITER ON Stoffeinzeleinleiter.EinzeleinleiterRef=Einzeleinleiter.id WHERE KONZENTRATION>0";
            rs = st.executeQuery(qstring);
            if (!rs.isBeforeFirst()) {
//                System.out.println("Database has no soluteSpill elements defined");
                //Is empty
                return particles;
            }
            ArrayList<EinzelStoffeinleiter> einleiter = new ArrayList<>(3);
            while (rs.next()) {
                EinzelStoffeinleiter eze = new EinzelStoffeinleiter();
                eze.pipename = rs.getString("ROHR");
                eze.discharge = rs.getDouble("ZUFLUSS");
                eze.concentration = rs.getDouble("KONZENTRATION"); //mg/L
                int stoffHEID = rs.getInt("STOFFRef");
                eze.material = materialMap.get(stoffHEID);
                eze.refIDTimelineVolume = rs.getInt("EINZELMUSTERREF");
                eze.refIDTimelineSolute = rs.getInt("STOFFMUSTERREF");
                eze.refIDMessdaten = rs.getInt("MESSDATENREF");
                einleiter.add(eze);
            }
            rs.close();
            //Request the time lines for volume and solute injection intensity
            for (EinzelStoffeinleiter ele : einleiter) {

                if (ele.refIDMessdaten > 0) {
                    TimedValue[] tv = readMessdaten(ele.refIDMessdaten, con);
                    HE_MessdatenInjection heinj = new HE_MessdatenInjection(ele.pipename, ele.material, simulationStart.getTimeInMillis(), tv, ele.concentration * 0.001);
                    if (verbose) {
                    System.out.println("Scenario messdata injection: c=" + ele.concentration + "mg/l,  q=" + ele.discharge + "L/s, duration:" + heinj.getDurationSeconds() + "s = total: " + heinj.getMass() + "kg = " + heinj.getTotalVolume() + "m³ in " + heinj.getNumberOfIntervals() + " intervals, " + ele.material + " in " + ele.pipename);
                    }

                    particles.add(heinj);

                } else {
//                    if(true) continue;
                    double[] volumefactor = new double[24];
                    double[] solutefactor = new double[24];

                    boolean hasTimeFactors = false;
                    boolean writtenVolumes = false;
                    if (ele.refIDTimelineVolume > 0) {
                        qstring = "SELECT * FROM TABELLENINHALTe WHERE ID=" + ele.refIDTimelineVolume + " ORDER BY REIHENFOLGE";
                        rs = st.executeQuery(qstring);
                        int index = 0;
                        if (rs.isBeforeFirst()) {
                            while (rs.next()) {
                                volumefactor[index] = rs.getDouble("WERT");
                                index++;
                                writtenVolumes = true;
                                hasTimeFactors = true;
                            }
                        }
                        rs.close();
                    } else {
//                    System.out.println(" ohne TimelineVolume");
                    }
                    if (!writtenVolumes) {
                        for (int i = 0; i < volumefactor.length; i++) {
                            volumefactor[i] = 1.;
                        }
                    }
                    boolean writtenSolute = false;
                    if (ele.refIDTimelineSolute > 0) {
                        qstring = "SELECT * FROM TABELLENINHALTe WHERE ID=" + ele.refIDTimelineSolute + " ORDER BY REIHENFOLGE";
                        rs = st.executeQuery(qstring);
                        int index = 0;
                        if (rs.isBeforeFirst()) {
                            while (rs.next()) {
                                solutefactor[index] = rs.getDouble("WERT");
                                index++;
                                writtenSolute = true;
                                hasTimeFactors = true;
                            }
                        } else {
//                        System.out.println("zeitreihe stoff " + ele.refIDTimelineSolute + " is empty");
                        }
                        rs.close();
                    } else {
//                    System.out.println("ohne Zeitreihe Stoff");
                    }
                    if (!writtenSolute) {
                        for (int i = 0; i < solutefactor.length; i++) {
                            solutefactor[i] = 1.;
                        }
                    }

                    if (hasTimeFactors) {
                        int start = -1;
                        int duration = -1;
                        double intensity = -1;
                        //Search for the right starttime
                        for (int i = 0; i < solutefactor.length; i++) {
                            double factor = solutefactor[i] * volumefactor[i];
                            if (factor > 0) {
                                if (intensity <= 0) {
                                    //this is the first interval with information
                                    start = i;
                                    duration = 1;
                                    intensity = factor;
                                } else {
                                    //this is an ongoing interval
                                    if (Math.abs(intensity - factor) < 0.001) {
                                        //Ongoing injection
                                        duration++;
                                    } else {
                                        //TODO: terminate the old spill and start a new one
                                        duration++;
                                        intensity = factor;
                                    }
                                }
                            }
                        }
                        //Create a injection information
                        if (start >= 0 && intensity > 0) {
                            long starttime = daystart.getTime() + (long) ((start) * 60 * 60 * 1000);
                            long endtime = starttime + (long) ((duration) * 60 * 60 * 1000);
                            double injectionTimeSeconds = (endtime - starttime) / 1000;
                            double wert = intensity * ele.concentration * ele.discharge * injectionTimeSeconds / 1000000.;

                            if (verbose) {
                                System.out.println("Scenario injection: c=" + ele.concentration + "mg/l,  q=" + ele.discharge + "L/s, timevariation:" + intensity + ", start:" + start + " (index), duration:" + injectionTimeSeconds + "s = total: " + wert + "kg" + " in " + ele.pipename);
                            }

                            try {
                                HEInjectionInformation info = new HEInjectionInformation(ele.pipename, ele.material, starttime, endtime, wert);//p.getPosition3D(0),true, wert, numberofparticles, m, starttime, endtime);

                                particles.add(info);
                            } catch (Exception exception) {
                                System.err.println("Could not find Pipe '" + ele.pipename + "' to inject " + ele.material.getName());
                                exception.printStackTrace();
                            }
                        }
                    } else {
                        //has no timefactory: constant discharge.
                        //Create a injection information

                        long starttime = simulationStart.getTimeInMillis();
                        long duration = simulationEnde.getTimeInMillis() - simulationStart.getTimeInMillis();//length of simulation
                        long endtime = starttime + duration;
                        double injectionTimeSeconds = duration / 1000;
                        double wert = ele.concentration * ele.discharge * injectionTimeSeconds / 1000000.;

                        if (verbose) {
                            System.out.println("Scenario constant injection: c=" + ele.concentration + "mg/l,  q=" + ele.discharge + "L/s, duration:" + injectionTimeSeconds + "s = total: " + wert + "kg, " + ele.material + " in " + ele.pipename);
                        }

                        try {
                            HEInjectionInformation info = new HEInjectionInformation(ele.pipename, ele.material, starttime, endtime, wert);//p.getPosition3D(0),true, wert, numberofparticles, m, starttime, endtime);
                            particles.add(info);
                        } catch (Exception exception) {
                            System.err.println("Could not find Pipe '" + ele.pipename + "' to inject " + ele.material);
                            exception.printStackTrace();
                        }

                    }

                }

            }

        } else {
            rs = st.executeQuery("SELECT ID,NAME,ABSETZWIRKUNG FROM STOFFGROESSE");
            if (!rs.isBeforeFirst()) {
                //Is empty
                return particles;
            }

            while (rs.next()) {
                Material m = new Material(rs.getString(2), 1000, true);
                materialMap.put(rs.getInt(1), m);
            }
            rs.close();

            String qstring = "SELECT ROHR, ROHRREF,ZUFLUSSDIREKT AS ZUFLUSSDIREKT_LperS,"
                    + "FAKTOR AS FAKTOR_von1,KONZENTRATION AS KONZENTRATION_MGperL,"
                    + "EINZELMUSTER.Wert AS EINZELWERT_von24,STOFFMUSTER.WERT AS STOFFWERT_von24,"
                    + "STOFFMUSTER.KEYWERT AS STARTZEIT, EINZELEINLEITER.NAME,"
                    + "STOFFREF,"
                    + "EINZELEINLEITER.ZEITMUSTER AS EINZELEINLEITERZEITMUSTER,"
                    + "Stoffeinzeleinleiter.Zeitmuster AS STOFFEINLEITERZEITMUSTER "
                    + "FROM EINZELEINLEITER JOIN StoffEINZELEINLEITER ON STOFFEINZELEINLEITER.EINZELEINLEITERREF=EINZELEINLEITER.ID\n"
                    + "JOIN TABELLENINHALTE AS EINZELMUSTER ON EINZELEINLEITER.ZEITMUSTERREF=EINZELMUSTER.ID\n"
                    + "JOIN TABELLENINHALTE AS STOFFMUSTER ON  EINZELMUSTER.KEYWERT=STOFFMUSTER.KEYWERT AND (STOFFEINZELEINLEITER.ZEITMUSTERREF=STOFFMUSTER.ID OR STOFFEINZELEINLEITER.ZEITMUSTERREF IS NULL)\n"
                    + "WHERE STOFFMUSTER.WERT>0 AND EINZELMUSTER.WERT>0 AND FAKTOR >0 AND KONZENTRATION>0\n"
                    + "ORDER BY EINZELEINLEITER.ID,STOFFEINZELEINLEITER.ID,EINZELMUSTER.ID,STARTZEIT";

            rs = st.executeQuery(qstring);
            Material m = new Material("Stofff", 1000, true);
            if (!rs.isBeforeFirst()) {
                System.out.println("no injections in database");
            }
            while (rs.next()) {
                Material mat = materialMap.get(rs.getInt("STOFFREF"));
                double tfactor = rs.getDouble("EINZELWERT_von24") * rs.getDouble("STOFFWERT_von24");
                double lps = rs.getDouble("ZUFLUSSDIREKT_LperS");
                double c = rs.getDouble("KONZENTRATION_MGperL");

                int rf = rs.getInt("Startzeit");
                long starttime = daystart.getTime() + (long) ((rf) * 60 * 60 * 1000);
                long endtime = daystart.getTime() + (long) ((rf + 1) * 60 * 60 * 1000);
                double injectionTime = (endtime - starttime) / 1000;
                double wert = tfactor * c * lps * injectionTime / 1000000.;

                System.out.println("Scenario injection: c=" + c + "mg/l,  q=" + lps + "L/s, timevariation:" + tfactor + ", duration:" + injectionTime + "s = total: " + wert + "kg");

                try {
                    HEInjectionInformation info = new HEInjectionInformation(rs.getString("ROHR"), mat, starttime, endtime, wert);//p.getPosition3D(0),true, wert, numberofparticles, m, starttime, endtime);
                    particles.add(info);
                } catch (Exception exception) {
                    System.err.println("Could not find Pipe '" + rs.getString("ROHR") + "'.");
                    exception.printStackTrace();
                }
            }
        }
        rs.close();
        st.close();

//        readFLAECHENVERSCHMUTZUNG();
        return particles;
    }

    public ArrayList<HE_AreaInjection> readFLAECHENVERSCHMUTZUNG() throws SQLException, IOException {

        ArrayList<HE_AreaInjection> list = new ArrayList<>();

        String qstring = "SELECT KONZENTRATION,ROHRREF,STOFFGROESSE,STOFFGROESSEREF,GESAMTFRACHT,SCHACHTUNTENREF,SCHACHTOBENREF,ROHR FROM FRACHTABFLUSSOBERFLAECHE JOIN ROHR ON ROHRREF=ROHR.ID where GESAMTFRACHT>0";
        Connection c = getConnection();
        Statement st = c.createStatement();

        HashMap<Integer, Material> materialMap = new HashMap<>();
        ResultSet rs = st.executeQuery("SELECT ID,NAME,ABSETZWIRKUNG FROM STOFFGROESSE");
        if (!rs.isBeforeFirst()) {
            //Is empty
            return list;
        }

        while (rs.next()) {
            Material m = new Material(rs.getString(2), 1000, true);
            int ref = rs.getInt(1);
            materialMap.put(ref, m);
            System.out.println("put " + m.getName() + " with id " + ref);
        }
        rs.close();

        rs = st.executeQuery(qstring);
        DecimalFormat df3=new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));
        while (rs.next()) {
            int stoffgroesseref = rs.getInt(4);
            int piperef = rs.getInt(2);
            double concentration = rs.getDouble(1);
            double totalmass = rs.getDouble(5);
            String rohrname = rs.getString(8);
//            System.out.println("Inject " + df3.format(concentration) + " kg/m³ of (" + stoffgroesseref + ")" + materialMap.get(stoffgroesseref).getName() + " in " + rohrname);
            HE_AreaInjection ae = new HE_AreaInjection(rohrname, materialMap.get(stoffgroesseref), 0, 0, totalmass);
            ae.constant_concentration = concentration;
            ae.rohr_DBID = piperef;
            list.add(ae);
        }

        return list;
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

    public int readOutletID(String outletName) throws SQLException, IOException {
        Connection c = getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT ID FROM AUSLASS WHERE NAME='" + outletName + "'");
        int id = -1;
        if (rs.isBeforeFirst()) {
            rs.next();
            id = rs.getInt(1);
        }
        rs.close();
        st.close();
        c.close();
        return id;
    }

    public double readOutletVolume(String outletName) throws IOException, SQLException {
        Connection c = getConnection();
        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery("SELECT SUMMEABFLUSS,KNOTEN FROM LAU_MAX_S WHERE LAU_MAX_S.KNOTEN='" + outletName + "'");
        double q = -1;
        if (rs.isBeforeFirst()) {
            rs.next();
            q = rs.getDouble(1);
        }
        rs.close();
        st.close();
        c.close();
        return q;
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
                if (!rs.isBeforeFirst()) {
                    System.err.println("has no Regenreihe: " + getDatabaseFile());
                    con.close();
                    return null;
                }
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
                //This will only return 1 result row, because Regenschreiberzuordnung only contains one row after a single simulation.

            } finally {

            }
        }
        con.close();
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
        if (!rs.isBeforeFirst()) {
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

    public static double read2DExportTimeStep(File extranResultDBF) throws SQLException, IOException {
        HE_Database db = new HE_Database(extranResultDBF, true);
        return db.read2DExportTimeStep();
    }

    /**
     *
     * @return export timestep for surface values (waterlevel&velocity) in GDB
     * in minutes.
     * @throws SQLException
     * @throws IOException
     */
    public double read2DExportTimeStep() throws SQLException, IOException {
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

    /**
     *
     * @return export timestep for pipes and manholes in minutes.
     * @throws SQLException
     * @throws IOException
     */
    public double readEXTRANExportTimeStep() throws SQLException, IOException {
        double dt = -1;
        try (Connection extran = getConnection()) {
            Statement st = extran.createStatement();
            ResultSet rs = st.executeQuery("SELECT BERICHTZEITSCHRITT FROM EXTRANPARAMETERSATZ");//This will only return 1 result row, because EXTRAN2DPARAMETERSATZ only contains one row after a single simulation.
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
//            ThreadConnection uc = getUnusedConnection();

            Connection c = getConnection();
            synchronized (c) {
                try {
                    Statement st = c.createStatement();
                    //in HE Database only use Pipe ID.
                    ResultSet rs = st.executeQuery("SELECT " + columname + ",ID,ZEITPUNKT FROM LAU_GL_EL WHERE ID=" + pipeMaualID + " ORDER BY ZEITPUNKT ");
                    if (!rs.isBeforeFirst()) {
                        //No Data
                        System.err.println(getClass() + " could not load " + columname + " values for pipe id:" + pipeMaualID + ". It is not found in the database.");
//                        uc.inUse = false;
                        return values;
                    }
                    int index = 0;
                    while (rs.next()) {
                        values[index] = rs.getFloat(1);
                        index++;
                    }
//                    uc.inUse = false;
                    return values;

                } catch (SQLException ex) {
                    Logger.getLogger(HE_Database.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
//                uc.inUse = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

    /**
     *
     * @param pipeMaualID
     * @param pipeName
     * @param numberOfTimes
     * @return
     */
    @Override
    public float[][] loadTimeLineMass(long pipeMaualID, String pipeName, int numberOfTimes) {
        //TODO: calculate the mass from the concentration.
        float[][] values = new float[numberOfTimes][getNumberOfMaterials()];
//        System.out.println("load timeline mass to float[" + numberOfTimes + "][" + getNumberOfMaterials() + "]");
        try {
//            ThreadConnection uc = getUnusedConnection();

            Connection c = getConnection();//uc.con;
            synchronized (c) {
                Statement st = c.createStatement();
                //in HE Database only use Pipe ID.

//                ResultSet rs = st.executeQuery("SELECT (KONZENTRATION * DURCHFLUSS)/ 1000 AS FRACHTKGPS,KANTESTOFFLAUFEND.ID,KANTESTOFFLAUFEND.ZEITPUNKT "
//                        + "FROM KANTESTOFFLAUFEND INNER JOIN LAU_GL_EL "
//                        + "ON KANTESTOFFLAUFEND.KANTE = LAU_GL_EL.KANTE "
//                        + "AND KANTESTOFFLAUFEND.ZEITPUNKT = LAU_GL_EL.ZEITPUNKT "
//                        + "AND KANTESTOFFLAUFEND.ID=" + pipeMaualID + " "
//                        + "ORDER BY KANTESTOFFLAUFEND.KANTE,KANTESTOFFLAUFEND.ZEITPUNKT");
                String query = "SELECT (KONZENTRATION * DURCHFLUSS)/ 1000 AS FRACHTKGPS,STOFF,KANTESTOFFLAUFEND.ID,KANTESTOFFLAUFEND.ZEITPUNKT "
                        + " FROM Stoffgroesse,KANTESTOFFLAUFEND INNER JOIN LAU_GL_EL "
                        + " ON KANTESTOFFLAUFEND.KANTE = LAU_GL_EL.KANTE "
                        + " AND KANTESTOFFLAUFEND.ZEITPUNKT = LAU_GL_EL.ZEITPUNKT "
                        + " AND STOFF=STOFFGROESSE.NAME "
                        + " AND KANTESTOFFLAUFEND.ID=" + pipeMaualID + " "
                        + " ORDER BY STOFFGROESSE.ID,KANTESTOFFLAUFEND.ZEITPUNKT";
                ResultSet rs = st.executeQuery(query);
//           
                if (!rs.isBeforeFirst()) {
                    //No Data
                    System.err.println(getClass() + " could not load KANTESTOFFLAUFEND values for pipe id:" + pipeMaualID + ". It is not found in the database.");
//                uc.inUse = false;
                    return values;
                }
//                System.out.println(query);
                int stoffindex = 0;
                int timeindex = 0;
//                float[] stoffmass = new float[numberOfTimes];
                while (rs.next()) {
                    values[timeindex][stoffindex] = rs.getFloat(1);
//                    System.out.println("   [" + timeindex + "][" + stoffindex + "]=" + rs.getFloat(1));
                    timeindex++;
                    if (timeindex >= numberOfTimes) {
                        timeindex = 0;
                        stoffindex++;
                    }
                }
                st.close();
//            uc.inUse = false;
                return values;
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public float[][] loadTimeLineConcentration(long pipeManualID, String pipeName, int numberOfTimes) {
        float[][] concentration = new float[numberOfTimes][getNumberOfMaterials()];
        try {
//            ThreadConnection uc = getUnusedConnection();

            Connection c = getConnection();
            synchronized (c) {
                //in HE Database only use Pipe ID.
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT KONZENTRATION,KANTESTOFFLAUFEND.ID,ZEITPUNKT,STOFF FROM KANTESTOFFLAUFEND JOIN STOFFGROESSE ON KANTESTOFFLAUFEND.STOFF=STOFFGROESSE.NAME WHERE KANTESTOFFLAUFEND.ID=" + pipeManualID + " ORDER BY STOFFGROESSE.ID,ZEITPUNKT ASC");
//comes in [mg/l]
                if (!rs.isBeforeFirst()) {
                    //Result set is empty, there are no concentrations set

//                    uc.inUse = false;
                    return concentration;
                }
                int stoffindex = 0;
                int timeindex = 0;
//                float[] stoffmass = new float[numberOfTimes];
                while (rs.next()) {
                    concentration[timeindex][stoffindex] = rs.getFloat(1) * 0.001f;
//                    System.out.println("   [" + timeindex + "][" + stoffindex + "]=" + rs.getFloat(1));
                    timeindex++;
                    if (timeindex >= numberOfTimes) {
                        timeindex = 0;
                        stoffindex++;
                    }
                }
//
//                int index = 0;
//                while (rs.next()) {
//                    if (index < concentration.length) {
//                        concentration[index] = rs.getFloat(1) * 0.001f;
//                    }
//                    index++;
//                }
//                if (index > concentration.length) {
//                    System.out.println("Konzentration information for " + index + " timesteps. but numberofTimes is only " + numberOfTimes + " can only load concnetration for the first material so far. " + this.getClass().getSimpleName());
//                }
                st.close();
//                uc.inUse = false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return concentration;
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
        ThreadConnection tc = null;
        try {
            tc = getUnusedConnection(manholeMaualID);
            Connection c = tc.con;
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
            st.close();
            return values;

        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tc != null) {
                tc.lock.unlock();
            }
        }
        return null;
    }

    public float[] loadTimeLineOutflowManhole(long manholeDatabaseID, int numberOfTimes) {
        float[] values = new float[numberOfTimes];
        try {

            Connection c = getConnection();
            synchronized (c) {
                Statement st = c.createStatement();
                //in HE Database only use Manhole ID.
                ResultSet rs = st.executeQuery("SELECT DURCHFLUSS,ID,ZEITPUNKT FROM LAU_GL_S WHERE ID=" + manholeDatabaseID + " ORDER BY ZEITPUNKT ");
                if (!rs.isBeforeFirst()) {
                    //No Data
                    System.err.println(getClass() + " could not load outflow for manhole id:" + manholeDatabaseID + ". It was not found in the database.");

                    return values;
                }
                int index = 0;
                while (rs.next()) {
                    values[index] = rs.getFloat(1);
                    index++;
                }
                st.close();
                return values;
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public float[] loadTimeLineOutflowManhole(String manholeName, int numberOfTimes) {
        float[] values = new float[numberOfTimes];
        try {

            Connection c = getConnection();
            synchronized (c) {
                Statement st = c.createStatement();
                //in HE Database only use Manhole ID.
                ResultSet rs = st.executeQuery("SELECT DURCHFLUSS,ID,ZEITPUNKT FROM LAU_GL_S WHERE KNOTEN='" + manholeName + "' ORDER BY ZEITPUNKT ");
                if (!rs.isBeforeFirst()) {
                    //No Data
                    System.err.println(getClass() + " could not load outflow for manhole name:" + manholeName + ". It was not found in the database.");

                    return values;
                }
                int index = 0;
                while (rs.next()) {
                    values[index] = rs.getFloat(1);
                    index++;
                }
                st.clearBatch();
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
        //NEW SCHEME INCLUDING 2D SURFACEx
        ThreadConnection tc = null;
        try {
            tc = getUnusedConnection(manholeID);
            Connection c = tc.con;
            float[] flux;
            //ABFLUSS: Flow from Surface to Pipesystem
            try (Statement st = c.createStatement()) {
                ResultSet res = st.executeQuery("SELECT ID,"
                        + "KNOTEN, "
                        + "ZEITPUNKT, "
                        + "(ABFLUSS-ZUFLUSS) AS NETTO "
                        + "FROM KNOTENLAUFEND2D "
                        + "WHERE ID=" + manholeID + " "
                        + "ORDER BY  ZEITPUNKT;");
                flux = new float[numberOfTimes];
                int index = 0;
                while (res.next()) {
                    flux[index] = res.getFloat(4);
                    index++;
                }
            }
            return flux;

        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tc != null) {
                tc.lock.unlock();
            }
        }

        return new float[numberOfTimes];

    }

    public String[] getOverspillingManholes(double minFlux) {
        LinkedList<String> names = new LinkedList<>();
        try {

            Connection c = getConnection();
            synchronized (c) {
                Statement st = c.createStatement();
                //ABFLUSS: Flow from Surface to Pipesystem
                ResultSet res = st.executeQuery("SELECT DISTINCT ID,KNOTEN FROM KNOTENLAUFEND2D WHERE ABFLUSS-ZUFLUSS>" + minFlux + " ORDER BY ID");
                while (res.next()) {
                    names.add(res.getString(2));
                }
                st.close();
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(HE_Database.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return names.toArray(new String[names.size()]);
    }

    @Override
    public SparseTimelineManhole loadTimelineManhole(long manholeManualID, String manholeName, float soleHeight, SparseTimeLineManholeContainer container) {
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
            SparseTimelineManhole stlm = new SparseTimelineManhole(container, manholeManualID, manholeName, soleHeight);
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
    public SparseTimelinePipe loadTimelinePipe(Pipe pipe, SparseTimeLinePipeContainer container) {
        SparseTimelinePipe tl = new SparseTimelinePipe(container, pipe);
        fillTimelinePipe(pipe.getManualID(), pipe.getName(), tl);
        return tl;

    }

    @Override
    public boolean fillTimelineManhole(long manholeManualID, String manholeName, SparseTimelineManhole timeline) {

        ThreadConnection tc = null;

        try {
            tc = getUnusedConnection(manholeManualID);
            Connection c = tc.con;
            if (timeline.isInitialized()) {
                //Does not need to be loaded any more. another Thread did that for us, while this thread was waiting for the monitor.
                return true;
            }

            float[] flux = new float[timeline.getNumberOfTimes()];
            float[] waterheight = new float[flux.length];
            Statement st = c.createStatement();
            //in HE Database only use Manhole ID.
            ResultSet rs = st.executeQuery("SELECT WASSERSTAND,ID,ZEITPUNKT FROM LAU_GL_S WHERE ID=" + manholeManualID + " ORDER BY ZEITPUNKT ");
            if (!rs.isBeforeFirst()) {
                //No Data
                System.err.println(getClass() + " could not load wasserstand for manhole id:" + manholeManualID + ". It is not found in the database.");
            }
            int index = 0;
            while (rs.next()) {
                waterheight[index] = rs.getFloat(1);
                index++;
            }
            rs.close();
            //ABFLUSS: Flow from Surface to Pipesystem

            ResultSet res = st.executeQuery("SELECT ID,"
                    + "KNOTEN, "
                    + "ZEITPUNKT, "
                    + "(ABFLUSS-ZUFLUSS) AS NETTO "
                    + "FROM KNOTENLAUFEND2D "
                    + "WHERE ID=" + manholeManualID + " "
                    + "ORDER BY  ZEITPUNKT;");
            index = 0;
            while (res.next()) {
                flux[index] = res.getFloat(4);
                index++;
            }

            timeline.setSpilloutFlux(flux);
            timeline.setWaterHeight(waterheight);
            rs.close();
            st.close();

            return true;

        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tc != null) {
                tc.lock.unlock();
            }
        }
        return false;
    }

    @Override
    public boolean fillTimelinePipe(long pipeManualID, String pipeName, SparseTimelinePipe timeline) {

        ThreadConnection tc = null;

        try {
            tc = getUnusedConnection(pipeManualID);
            Connection c = tc.con;
            if (timeline.isInitialized()) {
                //Does not need to be loaded any more. another Thread did that for us, while this thread was waiting for the monitor.
                return true;
            }
            try {

                try ( //in HE Database only use Pipe ID.
                        Statement st = c.createStatement();
                        ResultSet rs = st.executeQuery("SELECT GESCHWINDIGKEIT,DURCHFLUSS,WASSERSTAND,ID,ZEITPUNKT FROM LAU_GL_EL WHERE ID=" + pipeManualID + " ORDER BY ZEITPUNKT ")) {
                    int times = timeline.getNumberOfTimes();
                    float[] velocity, flux, waterlevel, volume;
                    velocity = new float[times];
                    flux = new float[times];
                    waterlevel = new float[times];
                    volume = new float[times];
                    int index = 0;
                    while (rs.next()) {
                        velocity[index] = rs.getFloat(1);
                        flux[index] = rs.getFloat(2);
                        waterlevel[index] = rs.getFloat(3);

                        if (velocity[index] == 0 && flux[index] != 0) {
                            //This seems to be a pump
                            velocity[index] = (float) Math.abs(flux[index] / 0.07);
                            waterlevel[index] = 0.1f;
                        }

                        index++;
                    }

                    timeline.setVelocity(velocity);
                    timeline.setFlux(flux);
                    timeline.setWaterlevel(waterlevel);
                    st.close();
                }
                return true;
            } catch (SQLException ex) {
                Logger.getLogger(HE_Database.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            return false;

        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tc != null) {
                tc.lock.unlock();
            }
        }
        return false;
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

    public boolean isIsSQLite() {
        return isSQLite;
    }

    @Override
    public boolean hasTimeLineMass() {
        try {
            con = getConnection();
            int count = 0;
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM KANTESTOFFLAUFEND")) {
                while (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            con.close();
            return count > 0;
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public class ThreadConnection {

        private final ReentrantLock lock = new ReentrantLock();
//        public boolean inUse = false;
        public Connection con;
        public long id = -1;

        public ThreadConnection(Connection con) {
            this.con = con;
        }

    }

    public class HEConnectionSerializer {
    };

    public class EinzelStoffeinleiter {

        public String pipename;
        public double discharge;
        public double concentration;
        public Material material;
        public int refIDTimelineVolume;
        public int refIDTimelineSolute;
        public int refIDMessdaten;
    }

    public static long[] decodeTimestamps(byte[] byteBlob) {
        ByteBuffer bb = ByteBuffer.wrap(byteBlob);
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        int number = bb.getInt(0);
        long[] time = new long[number];
//        System.out.println("number=" + number);

//         for (int i = 0; i < bb.limit(); i++) {
//             System.out.println(i+": "+bb.getLong(i));
//        }
        int l = (byteBlob.length - 4) / 16;
        for (int i = 0; i < l; i++) {
            int start = 4 + i * 16;
//            System.out.println(bb.getInt(start)+", +1:"+bb.getInt(start+1)+", +2:"+bb.getInt(start+2)+", +3:"+bb.getInt(start+3)+",\t -1:"+bb.getInt(start-1)+", -2:"+bb.getInt(start-2));
            long byteLong = bb.getLong(start);
            if (byteLong < 0) {
                System.err.println("Timeindex " + i + " of Messdaten seems not to be correctly formatted. Cannot decode Timevalue from bytelong " + byteLong + ". Check the HE entry for the Timestamp and save it without second and millisecond information.");
            }
            long dateTime = byteToDate(byteLong);
            time[i] = dateTime;

        }
        return time;
    }

    /**
     * Decodes the Information of a DATEN field in the Extran.idbf result
     * database.
     *
     * @param byteBlob
     * @return
     */
    public static double[] decodeValues(byte[] byteBlob) {
        ByteBuffer bb = ByteBuffer.wrap(byteBlob);
        bb = bb.order(ByteOrder.LITTLE_ENDIAN);
        int number = bb.getInt(0);
        double[] niederschlagshoehe = new double[number];
        int l = (byteBlob.length - 4) / 16;
        for (int i = 0; i < l; i++) {
            int start = 4 + i * 16;
            double wert = bb.getDouble(start + 8);
            niederschlagshoehe[i] = wert;
        }
        return niederschlagshoehe;
    }

    public TimedValue[] readMessdaten(String name, Connection con) throws SQLException, IOException {
        if (con == null || con.isClosed()) {
            con = getConnection();
        }
        ResultSet rs = con.createStatement().executeQuery("SELECT DATEN,NAME FROM MESSDATEN WHERE NAME='" + name + "'");
        if (!rs.isBeforeFirst()) {
            System.err.println("No Values for Messdaten '" + name + "'");
            rs.close();
            return null;
        }
        rs.next();
        byte[] blob = rs.getBytes(1);
        rs.close();
        long[] times = decodeTimestamps(blob);
        double[] values = decodeValues(blob);
        if (times.length != values.length) {
            System.err.println("Number of values of Messdaten '" + name + "' are not at same length " + times.length + " / " + values.length);
            return null;
        }
        TimedValue[] tv = new TimedValue[times.length];
        for (int i = 0; i < tv.length; i++) {
            tv[i] = new TimedValue(times[i], values[i]);
        }
        return tv;
    }

    /**
     * Pairs of time and value. if discharge or injection values are calculated
     * as [cbm/s]. If it is a waterheight, then in [m aSl].
     *
     * @param id
     * @param con
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public TimedValue[] readMessdaten(int id, Connection con) throws SQLException, IOException {
        if (con == null || con.isClosed()) {
            con = getConnection();
        }
        ResultSet rs = con.createStatement().executeQuery("SELECT DATEN,ID,Typ FROM MESSDATEN WHERE ID=" + id);
        //typ:0= Waterheight [m aSl], 1 = Zufluss [L/s], 2: Discharge [cbm/s]
        if (!rs.isBeforeFirst()) {
            System.err.println("No Values for Messdaten '" + id + "'");
            rs.close();
            return null;
        }
        rs.next();
        byte[] blob = rs.getBytes(1);
        int type = rs.getInt(3);
        rs.close();
        long[] times = decodeTimestamps(blob);
        double[] values = decodeValues(blob);
        if (times.length != values.length) {
            System.err.println("Number of values of Messdaten '" + id + "' are not at same length " + times.length + " / " + values.length);
            return null;
        }
        if (type == 1) {
            //L/s -> cbm/s
            for (int i = 0; i < values.length; i++) {
                values[i] *= 0.001;
            }
        }
        TimedValue[] tv = new TimedValue[times.length];
        for (int i = 0; i < tv.length; i++) {
            tv[i] = new TimedValue(times[i], values[i]);
        }
        return tv;
    }

    public static long byteToDate(long t) {
        long time = t - 621355968000000000L;
        return time / 10000L;
    }

    public static void main(String[] args) {
        try {
            HE_Database he = new HE_Database(new File("C:\\Users\\saemann\\Documents\\Hystem-Extran 8.1\\Hystem-Extran\\test_zeitmuster.idbm"), true);
            Connection c = he.getConnection();
            ResultSet rs = c.createStatement().executeQuery("SELECT * FROM MESSDATEN");
            rs.next();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                System.out.println(i + ": " + rs.getString(i));
            }
            byte[] data = rs.getBytes("Daten");
            System.out.println("Daten: " + data.length + " bytes");
            long[] times = decodeTimestamps(data);
            double[] values = decodeValues(data);
            System.out.println("times:" + times.length + ",    values:" + values.length);
            for (int i = 0; i < values.length; i++) {
                System.out.println(i + ": " + new Date(times[i]).toGMTString() + " , " + values[i]);

            }
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getNumberOfMaterials() {
        if (numberOfMaterials < 0) {
            try {
                ResultSet rs = getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM STOFFGROESSE");
                rs.next();
                numberOfMaterials = rs.getInt(1);
            } catch (SQLException | IOException ex) {
                Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
                numberOfMaterials = 0;
            }
        }

        return numberOfMaterials;
    }

    @Override
    public String[] loadNamesMaterials() {
        try {
            ResultSet rs = getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM STOFFGROESSE");
            rs.next();
            numberOfMaterials = rs.getInt(1);
            String[] names = new String[numberOfMaterials];
            rs.close();
            rs = getConnection().createStatement().executeQuery("SELECT NAME FROM STOFFGROESSE");
            int index = 0;
            while (rs.next()) {
                names[index] = rs.getString(1);
                index++;
            }
            return names;
        } catch (SQLException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HE_Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
