package io;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

/**
 *
 * @author saemann
 */
public class SQLite_IO {

    private Connection con;
    private File databaseFile;

    public static Connection openConnection(File filePath) throws SQLException, ClassNotFoundException {
        return openConnection(filePath.getAbsolutePath());
    }

    public static Connection openConnection(String filePath) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.setEncoding(SQLiteConfig.Encoding.UTF8);
        return DriverManager.getConnection("jdbc:sqlite:" + filePath, config.toProperties());
    }

    public SQLite_IO(File dbfile) throws SQLException, ClassNotFoundException {
        this.databaseFile=dbfile;
        con = openConnection(dbfile);
    }

}
