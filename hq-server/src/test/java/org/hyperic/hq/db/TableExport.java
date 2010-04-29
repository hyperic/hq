/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.db;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hyperic.util.jdbc.DBUtil;

public class TableExport {
    private static String _url,
                          _user,
                          _passwd,
                          _destFilename;
    private static boolean _import = false;
    private static Connection _conn;
    private static List<String> _tables = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        getArgs(args);
        _conn = getConnection();
        if (_import) {
            importDataSet();
        } else {
            exportPartialDataSet();
        }
    }

    private static void getArgs(String[] args) {
        for (int i=0; i<args.length; i++)
        {
            if (args[i].equals("--url")) {
                _url = args[++i];
            } else if (args[i].equals("--user")) {
                _user = args[++i];
            } else if (args[i].equals("--passwd")) {
                _passwd = args[++i];
            } else if (args[i].equals("--tables")) {
                String tables = args[++i];
                setTables(tables);
            } else if (args[i].equals("--file")) {
                _destFilename = args[++i];
            } else if (args[i].equals("--import")) {
                _import = true;
            }
        }
    }

    private static void setTables(String tables) {
        String[] toks = tables.split(",");
        for (int i=0; i<toks.length; i++) {
            _tables.add(toks[i]);
        }
    }
    
    private static final void importDataSet() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            IDatabaseConnection idbConn = new DatabaseConnection(conn);
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            // this is done for MySQL via another method
            if (DBUtil.isPostgreSQL(conn)) {
                stmt.execute("set constraints all deferred");
            } else if (DBUtil.isOracle(conn)) {
                stmt.execute("alter session set constraints = deferred");
            }
            IDataSet dataset = new FlatXmlDataSet(new GZIPInputStream(
                new FileInputStream(_destFilename)));
            DatabaseOperation.CLEAN_INSERT.execute(idbConn, dataset);
            conn.commit();
        } finally {
            DBUtil.closeJDBCObjects(TableExport.class.getName(), conn, stmt, null);
        }
    }

    private static void exportPartialDataSet() throws Exception {
        IDatabaseConnection connection = new DatabaseConnection(_conn);
        QueryDataSet dataSet = new QueryDataSet(connection);
        for (String table : _tables) {
            dataSet.addTable(table);
        }
        if (!_destFilename.endsWith(".xml.gz")) {
            _destFilename = _destFilename + ".xml.gz";
        }
        GZIPOutputStream gstream = new GZIPOutputStream(new FileOutputStream(_destFilename));
        long start = System.currentTimeMillis();
        System.out.print("writing " + _destFilename + "...");
        FlatXmlDataSet.write(dataSet, gstream);
        gstream.finish();
        System.out.println("done " + (System.currentTimeMillis() - start) + " ms");
    }

    private static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user",_user);
        props.setProperty("password",_passwd);
        return DriverManager.getConnection(_url, props);
    }
}
