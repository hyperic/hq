/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util.unittest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;

public class TableExport
{
    private static String _url,
                          _user,
                          _passwd,
                          _destDir = "./";
    private static Connection _conn;
    private static List _tables = new ArrayList();
    private static String _FS = File.separator;

    public static void main(String[] args) throws Exception
    {
        getArgs(args);
        _conn = getConnection();
        exportPartialDataSet();
    }

    private static void getArgs(String[] args)
    {
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
            } else if (args[i].equals("--dest")) {
                _destDir = args[++i];
            }
        }
    }

    private static void setTables(String tables)
    {
        String[] toks = tables.split(",");
        for (int i=0; i<toks.length; i++) {
            _tables.add(toks[i]);
        }
    }

    private static void exportPartialDataSet() throws Exception {
        IDatabaseConnection connection = new DatabaseConnection(_conn);
        for (Iterator i=_tables.iterator(); i.hasNext(); )
        {
            String table = (String) i.next();
            QueryDataSet dataSet = new QueryDataSet(connection);
            dataSet.addTable(table);
            String file = _destDir + _FS + table + ".xml.gz";
            GZIPOutputStream gstream = new GZIPOutputStream(
                new FileOutputStream(file));
            long start = System.currentTimeMillis();
            System.out.print("writing " + file + "...");
            FlatXmlDataSet.write(dataSet, gstream);
            gstream.finish();
            System.out.println("done " + (System.currentTimeMillis() - start)
                + " ms");
        }
    }

    private static Connection getConnection() throws SQLException
    {
        Properties props = new Properties();
        props.setProperty("user",_user);
        props.setProperty("password",_passwd);
        return DriverManager.getConnection(_url, props);
    }
}
