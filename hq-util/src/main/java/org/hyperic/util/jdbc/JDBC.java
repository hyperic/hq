/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.util.jdbc;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JDBC is a utility class of JDBC helper methods. The primary methods are
 * JDBC.getDriverString(), which given a simple database name like 'oracle'
 * returns the full jdbc driver string 'oracle.jdbc.driver.OracleDriver'.
 */
public final class JDBC
{
    /**
     * Cloudscape JDBC driver class name.
     */
    public static final String CLOUDSCAPE_NAME  = "cloudscape";
    public static final int    CLOUDSCAPE_TYPE  = 1;

    /**
     * Lutris InstantDB JDBC driver class name.
     */
    public static final String INSTANTDB_NAME   = "idb";
    public static final int    INSTANTDB_TYPE   = 2;

    /**
     * Oracle JDBC driver class name.
     */
    public static final String ORACLE_NAME      = "oracle";
    public static final int    ORACLE_TYPE      = 3;

    /**
     * PostgreSQL JDBC driver class name.
     */
    public static final String PGSQL_NAME       = "postgres";
    public static final int    PGSQL_TYPE       = 4;

    /**
     * Oracle JDBC driver class name.
     */
    public static final String ORACLE_THIN_NAME = "oracle-thin";
    public static final int    ORACLE_THIN_TYPE = 5;

    /**
     * Covalent JDBC-Log driver class name
     */
    public static final String COVLOG_NAME      = "covalent-log";
    public static final int    COVLOG_TYPE      = 6;

    /**
     * Pointbase JDBC driver class name.
     */
    public static final String POINTBASE_NAME   = "pointbase";
    public static final int    POINTBASE_TYPE   = 7;

    /**
     * MySQL JDBC driver class name.
     */
    public static final String MYSQL_NAME       = "mysql";
    public static final int    MYSQL_TYPE       = 8;

    private static HashMap driverClasses;
    private static String[] typeToNames;
    static {
        // Map the driver names and types to names
        driverClasses = new HashMap();
        driverClasses.put(CLOUDSCAPE_NAME, "COM.cloudscape.core.JDBCDriver");
        driverClasses.put(INSTANTDB_NAME,  "com.lutris.instantdb.jdbc.idbDriver");
        driverClasses.put(ORACLE_NAME,     "oracle.jdbc.driver.OracleDriver");
        driverClasses.put(PGSQL_NAME,      "org.postgresql.Driver");
        driverClasses.put(ORACLE_THIN_NAME,"oracle.jdbc.driver.OracleDriver");
        driverClasses.put(COVLOG_NAME,     "org.hyperic.util.jdbc.log.LoggerDriver");
        driverClasses.put(POINTBASE_NAME,  "com.pointbase.jdbc.jdbcUniversalDriver");
        driverClasses.put(MYSQL_NAME,      "com.mysql.jdbc.Driver");
        
        typeToNames = new String[driverClasses.size() + 1];
        typeToNames[CLOUDSCAPE_TYPE]  = CLOUDSCAPE_NAME;
        typeToNames[INSTANTDB_TYPE]   = INSTANTDB_NAME;
        typeToNames[ORACLE_TYPE]      = ORACLE_NAME;
        typeToNames[PGSQL_TYPE]       = PGSQL_NAME;
        typeToNames[ORACLE_THIN_TYPE] = ORACLE_THIN_NAME;
        typeToNames[COVLOG_TYPE]      = COVLOG_NAME;
        typeToNames[POINTBASE_TYPE]   = POINTBASE_NAME;
        typeToNames[MYSQL_TYPE]       = MYSQL_NAME;
    }
    
    /**
     * Retrieves the JDBC Connection String for the specified driver. The
     * end of the string including the name of the database to connect to
     * and other options must be appended to the end of the returned string
     * before using the string in a JDBC call.
     *
     * @param driver
     *      The JDBC driver name. The name can be in full name
     *      (e.g., "COM.cloudscape.core.JDBCDriver") or short name
     *      (e.g., "cloudscape") form.
     *
     * @return String
     *      The JDBC connection string.
     */
    public static String getConnectionString(String driver)
    {
        if(driver == null)
            throw new IllegalArgumentException("Parameter \'driver\' cannot be null.");

        String  strResult;

        if(driver.indexOf(JDBC.CLOUDSCAPE_NAME) != -1)
            strResult = "jdbc:cloudscape:";
        else if(driver.indexOf(JDBC.INSTANTDB_NAME) != -1)
             strResult = "jdbc:idb=";
        else if(driver.indexOf(JDBC.ORACLE_THIN_NAME) != -1)
            strResult = "jdbc:oracle:thin:@";
        else if(driver.indexOf(JDBC.ORACLE_NAME) != -1)
            strResult = "jdbc:oracle:oci8:";
        else if(driver.indexOf(JDBC.PGSQL_NAME) != -1)
            strResult = "jdbc:postgresql:";
        else if(driver.indexOf(JDBC.COVLOG_NAME) != -1)
            strResult = "jdbc:covalent-log:";
        else if(driver.toLowerCase().indexOf(JDBC.POINTBASE_NAME) != -1)
            strResult = "jdbc:pointbase:";
        else if(driver.toLowerCase().indexOf(JDBC.MYSQL_NAME) != -1)
            strResult = "jdbc:mysql:";
        else
            strResult = new String();

        return strResult;
    }

    /**
     * Returns the correct jdbc adapter class factory for CMP
     *
     * @param driver
     *      The JDBC driver name. The name can be in full name
     *      (e.g., "COM.cloudscape.core.JDBCDriver") or short name
     *      (e.g., "cloudscape") form.
     *
     * @return String
     *      The CMP adapter class factory name
     */
    public static String getCmpAdapterFactory(String driver)
    {
        if(driver == null)
            throw new IllegalArgumentException("Parameter \'driver\' cannot be null.");
        String  strResult;

        if(driver.indexOf(JDBC.CLOUDSCAPE_NAME) != -1)
            strResult = "net.covalent.c3.server.data.jdbc.AdapterFactory";
        else if(driver.indexOf(JDBC.INSTANTDB_NAME) != -1)
             strResult = "net.covalent.c3.server.data.jdbc.idb.AdapterFactory";
        else if(driver.indexOf(JDBC.ORACLE_NAME) != -1)
            strResult = "net.covalent.c3.server.data.jdbc.oracle.AdapterFactory";
        else if(driver.indexOf(JDBC.ORACLE_THIN_NAME) != -1)
            strResult = "net.covalent.c3.server.data.jdbc.oracle.AdapterFactory";
        else if(driver.indexOf(JDBC.PGSQL_NAME) != -1)
            strResult = "net.covalent.c3.server.data.jdbc.AdapterFactory";
        else
            strResult = new String();

        return strResult;
    }

    /**
     * Retrieves the full JDBC Connection String for the specified driver.
     *
     * @param driver
     *      The JDBC driver name. The name can be in full name
     *      (e.g., "COM.cloudscape.core.JDBCDriver") or short name
     *      (e.g., "cloudscape") form.
     * @param database
     *      The database name to append to the JDBC connection string.
     *
     * @return String
     *      The JDBC connection string.
     */
    public static String getConnectionString(String driver, String database)
    {
        if(driver == null || database == null)
            throw new IllegalArgumentException("Parameters \'driver\' and \'database\' cannot be null.");

        String  strResult;

        // If the database string already starts with 'jdbc' we don't have to look it up.
        if(database.startsWith("jdbc") == false)
        {
            strResult = JDBC.getConnectionString(driver);

            strResult = strResult + '@';
        }
        else
        {
            strResult = database;
        }

        return strResult;
    }
    
    /**
     * Retrieves the full JDBC Driver String from the database name. The
     * database name can be a short name like cloudscape or a full jdbc
     * connection string like jdbc:cloudscape:.
     *
     * @param database
     *      The database name.
     *
     * @return String
     *      The full JDBC Driver String (e.g., "COM.cloudscape.core.JDBCDriver")
     *      that corresponds the the short driver name.
     */
    public static String getDriverString(String database)
    {
        if(database == null)
            throw new IllegalArgumentException("Parameter \'database\' cannot be null.");

        // We're using indexOf to compare rather than equals so that we can
        // accept either a short string like cloudscape or a connection string
        // like jdbc:cloudscape:
        for (Iterator it = driverClasses.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            if (database.indexOf(name) != -1) {
                return (String) entry.getValue();
            }
        }

        return database;
    }

    /**
     * Loads the JDBC driver from the driver short name (e.g., oracle) or the
     * driver full name (e.g., oracle.jdbc.driver.OracleDriver).
     *
     * @param driver
     *      The short or full name of the JDBC driver.
     *
     * throws ClassNotFoundException
     *      If the driver name does not specify a valid driver.
     */
    public static void loadDriver(String database) throws ClassNotFoundException
    {
        if(database == null)
            throw new IllegalArgumentException("Parameter \'driver\' cannot be null.");
        String driverClassName = JDBC.getDriverString(database);
        Class.forName(driverClassName);
    }

    /**
     * Prints a SQLException including the error code and child exceptions to
     * System.out .
     *
     * @param e
     *      The SQLException object.
     *
     * @see java.sql.SQLException
     */
    public static void printSQLException(SQLException e)
    {
        if(e == null)
            throw new IllegalArgumentException("Parameter cannot be null.");

        JDBC.printSQLException(e, new PrintWriter(System.out, true));
    }

    /**
     * Prints a SQLException including the error code and child exceptions to
     * System.out .
     *
     * @param e
     *      The SQLException object.
     * @param out
     *      The java.io.PrintWriter to print the to.
     *
     * @see java.io.PrintWriter
     * @see java.sql.SQLException
     */
    public static void printSQLException(SQLException e, PrintWriter out)
    {
        if(e == null || out == null)
            throw new IllegalArgumentException("Parameter cannot be null.");

        do
        {
            out.println("Error: " + e.getErrorCode() + ": " + e);
        } while((e = e.getNextException()) != null);
    }

    /**
     * Retrieves the database name for the specified type id.
     *
     * @param database
     *      The database type id.
     *
     * @return String
     *      The name of the database.
     */
    public static String toName(int database)
    {
        if (database < 1 || database >= typeToNames.length)
            return null;
        
        return typeToNames[database];
    }

    public static String toName(String database)
    {
        if(database == null)
            throw new IllegalArgumentException("Parameter \'database\' cannot be null.");

        // We're using indexOf to compare rather than equals so that we can
        // accept either a short string like cloudscape or a connection string
        // like jdbc:cloudscape:
        
        // Go backwards because of oracle-thin and oracle
        database = database.toLowerCase();
        for (int i = typeToNames.length - 1; i > 0; i--) {
            String name = typeToNames[i];
            if(database.indexOf(name) != -1)
                return name;
        }
        
        return null;
    }
    
    /**
     * Retrieves the type id for the specified database.
     *
     * @param database
     *      The database name.
     *
     * @return int
     *      The type id of the database.
     */
    public static int toType(String database)
    {
        if(database == null)
            throw new IllegalArgumentException("Parameter \'database\' cannot be null.");

        // Go backwards because of oracle-thin and oracle
        database = database.toLowerCase();
        for (int i = typeToNames.length - 1; i > 0; i--) {
            if(database.indexOf(typeToNames[i]) != -1)
                return i;
        }
        
        return -1;
    }
}
