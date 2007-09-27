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

package org.hyperic.hq.plugin.mysql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MySQLServerDetector 
    extends ServerDetector
    implements FileServerDetector, 
               RegistryServerDetector,
               AutoServerDetector
{
    // Server Types
    static final String SERVER_NAME = "MySQL";

    // Service Types
    static final String TABLE       = "Table";

    // Versions
    static final String VERSION_3   = "3.x";
    static final String VERSION_4   = "4.x";
    static final String VERSION_5   = "5.x";

    // Discovery queries
    private static final String DATABASE_QUERY = "SHOW DATABASES";
    private static final String TABLE_QUERY    = "SHOW TABLE STATUS";
    
    private Log log = LogFactory.getLog("MySQLServerDetector");

    //likely will only work w/ linux due to permissions
    //and setting of argv[0] to the full binary path.
    private static final String PTQL_QUERY =
        "State.Name.eq=mysqld";
    
    private static final String PTQL_QUERY_WIN32 =
        "State.Name.eq=mysqld-nt";

    private String getProcessQuery() {
        if (isWin32()) {
            return PTQL_QUERY_WIN32;
        }
        else {
            return PTQL_QUERY;
        }
    }

    private List getServerValues(String path, String version) 
        throws PluginException
    {
        List servers = new ArrayList();

        ServerResource server = createServerResource(path);
        
        // Avoid clash with HQ 2.0 AI identifier for MySQL databases
        server.setIdentifier(getTypeInfo().getName() + path);

        // Set PTQL query
        ConfigResponse config = new ConfigResponse();
        config.setValue("process.query", getProcessQuery());
        
        server.setProductConfig(config);
        
        servers.add(server);

        return servers;
    }

    /**
     * Helper method to discover MySQL servers using the process table
     */
    private List getServerProcessList() {
        ArrayList servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);

        for (int i=0; i<pids.length; i++) {
            String exe = getProcExe(pids[i]);
            if (exe == null) {
                continue;
            }

            // mysqld is usually kept in ${install}/bin or
            // ${install}/libexec (gentoo).  move up one directory
            // and check for safe_mysqld (3.x) or mysqld_safe (4.x)
            String installPath = getParentDir(exe, 2);

            File binDir = new File(installPath, "bin");

            // Handle 5.x and 4.x servers
            File mysqld_safe = new File(binDir, "mysqld_safe");
            if (mysqld_safe.exists() &&
                mysqld_safe.isAbsolute()) {
                servers.add(mysqld_safe.getAbsolutePath());
                continue;
            }

            // Handle 3.x servers
            File safe_mysqld = new File(binDir, "safe_mysqld");
            if (safe_mysqld.exists() &&
                safe_mysqld.isAbsolute()) {
                servers.add(safe_mysqld.getAbsolutePath());
                continue;
            }
        }

        return servers;
    }

    /**
     * Auto scan
     */
    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException
    {
        List servers = new ArrayList();
        List paths = getServerProcessList();

        for (int i = 0; i < paths.size(); i++) {
            String dir = (String)paths.get(i);
            List found = getServerResources(platformConfig, dir);
            if (found != null && !found.isEmpty()) {
                servers.addAll(found);
            }
        }   

        return servers;
    }

    /**
     * File scan
     */
    public List getServerResources (ConfigResponse platformConfig, String path) 
        throws PluginException
    {
        if (getTypeInfo().getVersion().equals(VERSION_3)) {
            if (path.endsWith("safe_mysqld")) {
                path = getParentDir(path, 2);
                return getServerValues(path, VERSION_3);
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_4)) {
            if (path.endsWith("mysqld_safe")) {
                path = getParentDir(path, 2);

                // 4.x versions include isamchk
                File bindir = new File(path, "bin");
                File isamchk = new File(bindir, "isamchk");

                if (isamchk.exists()) {
                    return getServerValues(path, VERSION_4);
                }
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_5)) {
            if (path.endsWith("mysqld_safe")) {
                path = getParentDir(path, 2);

                // 5.x no longer includes isamchk
                File bindir = new File(path, "bin");
                File isamchk = new File(bindir, "isamchk");

                if (!isamchk.exists()) {
                    return getServerValues(path, VERSION_5);
                }
            }
        } else {
            // Unable to determine MySQL version
            throw new IllegalArgumentException("Unknown mysql version at: " +
                                               path);
        }
        
        return null;
    }

    /**
     * Registry scan
     */
    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException 
    {
        String version;
        String name = current.getSubKeyName();
        if (name == null) {
            return null;
        }

        if (name.indexOf(SERVER_NAME) == -1) {
            return null;
        }

        if (name.indexOf("3.") != -1) {
            version = VERSION_3;
        }
        else if (name.indexOf("4.") != -1) {
            version = VERSION_4;
        }
        else if (name.indexOf("5.") != -1) {
            version = VERSION_5;
        }
        else {
            this.log.info("Found unsupported MySQL version: " +
                          name);
            return null;
        }

        //4.1
        if (new File(path).exists()) {
            return getServerValues(path, version);
        }

        //4.0
        //e.g. path=="C:\WINDOWS\IsUninst.exe -fC:\mysql\Unint.isu"
        int ix = path.indexOf("-f");
        if (ix == -1) {
            return null;
        }
        
        path = getParentDir(path.substring(ix+2));

        return getServerValues(path, version);
    }

    /**
     * Rather than implement discoverServices() we override discoverServers() 
     * so that we can discover multiple databases within a single MySQL 
     * instance
     */
    protected List discoverServers(ConfigResponse config)
        throws PluginException
    {
        String url = config.getValue(MySQLMeasurementPlugin.PROP_URL);
        String user = config.getValue(MySQLMeasurementPlugin.PROP_USER);
        String pass = config.getValue(MySQLMeasurementPlugin.PROP_PASSWORD);
        String installpath =
            config.getValue(ProductPlugin.PROP_INSTALLPATH);

        this.log.debug("discover using: " + config);

        if (url == null) {
            //url might not be configured (java -jar hq-plugin.jar -m discover)
            return null;
        }

        String baseUrl = url.substring(0, url.lastIndexOf("/")+1);
        String type = SERVER_NAME + " " + getTypeInfo().getVersion();

        List aiservers = new ArrayList();

        try {
            Class.forName(MySQLMeasurementPlugin.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            throw new PluginException("Unable to load JDBC " +
                                      "Driver: " + e.getMessage());
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(DATABASE_QUERY);

            while (rs != null && rs.next()) {
                String database = rs.getString(1);

                ServerResource server = new ServerResource();
                server.setType(type);
                server.setInstallPath(installpath);
                server.setIdentifier(installpath + database);
                server.setName(getPlatformName() + " " + type + " " + 
                               database);

                // Auto-configure
                ConfigResponse productConfig = new ConfigResponse();

                String dbUrl = baseUrl + database;
                productConfig.setValue(MySQLMeasurementPlugin.PROP_URL, dbUrl);
                productConfig.setValue(MySQLMeasurementPlugin.PROP_USER, user);
                productConfig.setValue(MySQLMeasurementPlugin.PROP_PASSWORD,
                                       pass);

                server.setProductConfig(productConfig);
                server.setMeasurementConfig();
                
                this.log.debug("found database: " + productConfig);

                List services = discoverTables(dbUrl, user, pass);
                for (int i=0; i<services.size(); i++) {
                    server.addService((ServiceResource)services.get(i));
                }

                aiservers.add(server);
            }
        } catch (SQLException e) {
            throw new PluginException("Error querying for databases " +
                                      e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        return aiservers;
    }

    /**
     * Discover all MySQL Table services
     */
    protected List  discoverTables(String url, String user, String pass)
        throws PluginException
    {
        try {
            Class.forName(MySQLMeasurementPlugin.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            throw new PluginException("Unable to load JDBC " +
                                      "Driver: " + e.getMessage());
        }

        ArrayList services = new ArrayList();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        // Discover all MySQL tables.
        try {
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(TABLE_QUERY);

            // Loop detecting MySQL tables
            while (rs != null && rs.next()) {
                String tablename = rs.getString(1);
                String engine    = rs.getString(2);

                ServiceResource service = new ServiceResource();
                service.setType(SERVER_NAME + " " + 
                                getTypeInfo().getVersion() + " " +
                                TABLE);
                service.setServiceName(tablename);
                
                // Set custom properties (5.x only)
                if (getTypeInfo().getVersion().
                    equals(MySQLServerDetector.VERSION_5)) {
                    ConfigResponse cprop = new ConfigResponse();
                    cprop.setValue("Engine", engine);
                    service.setCustomProperties(cprop);
                }

                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(MySQLMeasurementPlugin.PROP_TABLE,
                                       tablename);

                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();

                services.add(service);
            }
        } catch (SQLException e) {
            throw new PluginException("Error querying for table " +
                                      "services: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        return services;
    }
}
