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

package org.hyperic.hq.plugin.postgresql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.jdbc.DBUtil;

import org.hyperic.sigar.win32.RegistryKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PostgreSQLServerDetector
    extends ServerDetector
    implements FileServerDetector,
               RegistryServerDetector,
               AutoServerDetector {

    private Log log =  LogFactory.getLog("PostgreSQLServerDetector");

    private static final String POSTGRESQL_VERSION = "(PostgreSQL)";
    //likely will only work w/ linux due to permissions
    //and setting of argv[0] to the full binary path.
    //State.Name == 'postgres' on OSX, 'postmaster' elsewhere
    private static final String PTQL_QUERY =
        "State.Name.re=post(master|gres),State.Name.Pne=$1,Args.0.re=.*post(master|gres)$";

    // Table discovery query
    private static final String TABLE_QUERY = 
        "SELECT relname FROM pg_stat_user_tables";
    // Index discovery query
    private static final String INDEX_QUERY =
        "SELECT indexrelname FROM pg_stat_user_indexes";

    // Resource types and versions
    static final String SERVER_NAME = "PostgreSQL";
    static final String TABLE       = "Table";
    static final String INDEX       = "Index";

    static final String VERSION_74 = "7.4";
    static final String VERSION_80 = "8.0";
    static final String VERSION_81 = "8.1";
    static final String VERSION_82 = "8.2";
    static final String VERSION_83 = "8.3";

    static final String HQ_SERVER_DB = "HQ PostgreSQL";
    static final String HQ_SERVER_DB81 = "HQ PostgreSQL 8.1";
    static final String HQ_SERVER_DB82 = "HQ PostgreSQL 8.2";
    static final String HQ_SERVER_DB83 = "HQ PostgreSQL 8.3";

    private static List getServerProcessList() {
        ArrayList servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);

        for (int i=0; i<pids.length; i++) {
            String exe = getProcExe(pids[i]);

            if (exe == null) {
                continue;
            }

            File binary = new File(exe);

            if (!binary.isAbsolute()) {
                continue;
            }
            servers.add(binary.getAbsolutePath());
        }

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();
        List paths = getServerProcessList();

        for (int i = 0; i < paths.size(); i++) {
            String dir = (String)paths.get(i);
            List found = getServerList(dir);
            if (!found.isEmpty()) {
                servers.addAll(found);
            }
        }   

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path)
        throws PluginException
    {
        // Normal file scan
        return getServerList(path);
    }

    public List getServerResources (ConfigResponse platformConfig, 
                                    String path, RegistryKey current) 
        throws PluginException
    {
        return getServerList(path);
    }

    public List getServerList(String path) 
        throws PluginException {

        List servers = new ArrayList();
        String version;

        // Only check the binaries if they match the path we expect
        if ((path.indexOf("pg_ctl.exe") == -1) &&
            (path.indexOf("postgres") == -1) &&
            (path.indexOf("postmaster") == -1))
        {
            return servers;
        }

        /**
         * Determine version by locating the version string in the
         * binary.  If binary is stripped, this would fail.  May be
         * good to fall back to finding differences in files between
         * the two versions
         */
        String binary;
        if (path.indexOf("pg_ctl.exe") != -1) {
            // Windows path includes arguments.. strip them first
            binary = path.substring(0, path.indexOf("pg_ctl.exe") + 10);

            // Some versions of windows quote the image path
            if (binary.charAt(0) == '"') {
                binary = binary.substring(1);
            }
        } else {
            binary = path;
        }

        String errmsg =
            "Unable to find '" + POSTGRESQL_VERSION + 
            "' in: " + binary;
        
        String line;
        try {
            line = FileUtil.findString(binary, POSTGRESQL_VERSION);
        } catch (IOException e) {
            this.log.error(errmsg + ": " + e);
            return servers;
        }

        if (line == null) {
            // Unable to detrmine version
            this.log.error(errmsg);
            return servers;
        } else {
            int ix = line.lastIndexOf(" ");
            if (ix != -1) {
                version = line.substring(ix + 1);
                this.log.debug("Found PostgreSQL version " + version);
            } else {
                // Unable to find version
                this.log.error("Unable to determine PostgreSQL version " +
                               "from version: " + line);
                return servers;
            }
        }

        String installPath;
        if(binary.endsWith("postgres") ||
           binary.endsWith("postmaster") ||
           binary.endsWith("pg_ctl.exe")) {
            // Move up 2 dirs
            installPath = getParentDir(binary, 2);
        } else {
            // nothing to detect
            return servers;
        }
        
        ServerResource server = createServerResource(installPath);

        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);

        // Auto-configure if this is HQ's internal database
        if (installPath.endsWith("hqdb")) {
            ConfigResponse productConfig = new ConfigResponse();
                
            productConfig.setValue(PostgreSQLMeasurementPlugin.PROP_URL,
                                   "jdbc:postgresql://localhost:9432/hqdb");
            productConfig.setValue(PostgreSQLMeasurementPlugin.PROP_USER, 
                                   "hqadmin");
            productConfig.setValue(PostgreSQLMeasurementPlugin.PROP_PASSWORD,
                                   "hqadmin");

            server.setProductConfig(productConfig);
            server.setControlConfig();
            server.setMeasurementConfig();
        }

        // Ensure 8.0 detector does not return 7.4 servers and vice versa
        if (getTypeInfo().getVersion().equals(VERSION_74)) {
            if (version.indexOf(VERSION_74) != -1) {
                // Name is set here since we tack on the real version.
                String name = getPlatformName() + " " +
                    SERVER_NAME + " " + version;
                server.setName(name);
                servers.add(server);
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_80)) {
            if (version.indexOf(VERSION_80) != -1) {
                // Name is set here since we tack on the real version.
                String name;
                if (installPath.indexOf("hqdb") != -1) {
                    // 8.0 could be our backend database.  Set the
                    // identifier and name.
                    name = getPlatformName() + " " +
                        HQ_SERVER_DB;
                    server.setIdentifier(HQ_SERVER_DB);
                } else {
                    name = getPlatformName() + " " +
                        SERVER_NAME + " " + version;
                }

                server.setName(name);
                servers.add(server);
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_81)) {
            if (version.indexOf(VERSION_81) != -1) {
                // Name is set here since we tack on the real version.
                String name;
                if (installPath.indexOf("hqdb") != -1) {
                    // 8.1 could be our backend database.  Set the
                    // identifier and name.
                    name = getPlatformName() + " " +
                        HQ_SERVER_DB81;
                    server.setIdentifier(HQ_SERVER_DB81);
                } else {
                    name = getPlatformName() + " " +
                        SERVER_NAME + " " + version;
                }

                server.setName(name);
                servers.add(server);
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_82)) {
            if (version.indexOf(VERSION_82) != -1) {
                String name;
                if (installPath.indexOf("hqdb") != -1) {
                    name = getPlatformName() + " " +
                            HQ_SERVER_DB82;
                    server.setIdentifier(HQ_SERVER_DB82);
                } else {
                    name = getPlatformName() + " " +
                        SERVER_NAME + " " + version;
                }

                server.setName(name);
                servers.add(server);
            }
        } else if (getTypeInfo().getVersion().equals(VERSION_83)) {
            if (version.indexOf(VERSION_83) != -1) {
                String name;
                if (installPath.indexOf("hqdb") != -1) {
                    name = getPlatformName() + " " +
                            HQ_SERVER_DB83;
                    server.setIdentifier(HQ_SERVER_DB83);
                } else {
                    name = getPlatformName() + " " +
                        SERVER_NAME + " " + version;
                }

                server.setName(name);
                servers.add(server);
            }
        }

        return servers;
    }

    protected List discoverServices(ConfigResponse config) 
        throws PluginException
    {
        String url = config.getValue(PostgreSQLMeasurementPlugin.PROP_URL);
        String user = config.getValue(PostgreSQLMeasurementPlugin.PROP_USER);
        String pass = config.getValue(PostgreSQLMeasurementPlugin.PROP_PASSWORD);

        try {
            Class.forName(PostgreSQLMeasurementPlugin.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            throw new PluginException("Unable to load JDBC " +
                                      "Driver: " + e.getMessage());
        }

        ArrayList services = new ArrayList();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(url, user, pass);
            // Discover all PostgreSQL tables.
            stmt = conn.createStatement();
            rs = stmt.executeQuery(TABLE_QUERY);

            while (rs != null && rs.next()) {
                String tablename = rs.getString(1);

                ServiceResource service = new ServiceResource();
                service.setType(this, TABLE);
                service.setServiceName(tablename);

                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PostgreSQLMeasurementPlugin.PROP_TABLE,
                                       tablename);

                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();

                services.add(service);
            }

            /* Discovery of PostgreSQL indexes is currently disabled.
            rs.close();
            rs = stmt.executeQuery(INDEX_QUERY);

            while (rs != null && rs.next()) {
                String indexname = rs.getString(1);

                ServiceResource service = new ServiceResource();
                service.setType(this, INDEX);
                service.setServiceName(indexname);

                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PostgreSQLMeasurementPlugin.PROP_INDEX,
                                       indexname);

                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();

                services.add(service);
            }
            */

        } catch (SQLException e) {
            throw new PluginException("Error querying for " +
                                      "services: " + e.getMessage());
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        return services;
    }
}
