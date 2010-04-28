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

package org.hyperic.hq.plugin.informix;

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

public class InformixServerDetector
    extends ServerDetector
    implements FileServerDetector, RegistryServerDetector, AutoServerDetector
{
    private static final boolean DEBUG = InformixPluginUtil.DEBUG;

    static final String PROP_DBNAME    = InformixPluginUtil.PROP_DBNAME,
                        PROP_TABLENAME = InformixPluginUtil.PROP_TABLENAME,
                        JDBC_DRIVER    = InformixPluginUtil.JDBC_DRIVER,
                        PROP_CHUNK     = InformixPluginUtil.PROP_CHUNK,
                        PROP_TABLE     = InformixMeasurementPlugin.PROP_TABLE,
                        PROP_URL       = InformixMeasurementPlugin.PROP_URL,
                        PROP_USER      = InformixMeasurementPlugin.PROP_USER,
                        PROP_PASSWORD  = InformixMeasurementPlugin.PROP_PASSWORD;

    private Log log = LogFactory.getLog("InformixServerDetector");

    private static final String INFORMIX_VERSION = "(Informix)";
    //likely will only work w/ linux due to permissions
    //and setting of argv[0] to the full binary path.
    //State.Name == 'postgres' on OSX, 'postmaster' elsewhere
    private static final String PTQL_QUERY =
        "State.Name.re=oninit,State.Name.Pne=$1,Args.0.re=.*oninit$";

    // Table discovery query
    private static final String TABLE_QUERY = 
        "select dbsname, tabname from sysptprof";
    // Data Chunk discovery query
    private static final String CHUNK_QUERY =
        "select name datachunk from sysdbstab";

    // Resource types and versions
    static final String SERVER_NAME = "Informix",
                        TABLE       = "Table",
                        INDEX       = "Index",
                        VERSION_10  = "10.0";

    private static List getServerProcessList()
    {
        ArrayList servers = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        for (int i=0; i<pids.length; i++)
        {
            String exe = getProcExe(pids[i]);
            if (exe == null)
                continue;
            File binary = new File(exe);
            if (!binary.isAbsolute())
                continue;
            servers.add(binary.getAbsolutePath());
        }
        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException
    {
        List servers = new ArrayList();
        List paths = getServerProcessList();
        for (int i=0; i<paths.size(); i++)
        {
            String dir = (String)paths.get(i);
            List found = getServerList(dir);
            if (!found.isEmpty())
                servers.addAll(found);
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

    public List getServerList(String path) throws PluginException
    {
        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("hostname", "localhost");
        productConfig.setValue("port", "3500");
        productConfig.setValue("servername", "test_shm");
        productConfig.setValue(PROP_USER, "informix");
        productConfig.setValue(PROP_PASSWORD, "sainformix");

        List servers = new ArrayList();

        // Only check the binaries if they match the path we expect
        if (path.indexOf("oninit") == -1)
            return servers;

        String installdir = getParentDir(path, 2);
        ServerResource server = createServerResource(installdir);
        String version = getTypeInfo().getVersion();
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        server.setProductConfig(productConfig);
        server.setMeasurementConfig();
        server.setName(SERVER_NAME+" "+version);
        servers.add(server);

        return servers;
    }

    protected List discoverServices(ConfigResponse config) 
        throws PluginException
    {
        String url  = config.getValue(PROP_URL);
        String user = config.getValue(PROP_USER);
        String pass = config.getValue(PROP_PASSWORD);

        try {
            Class.forName(JDBC_DRIVER);
        }
        catch (ClassNotFoundException e) {
            // No driver.  Should not happen.
            String msg = "Unable to load JDBC "+"Driver: "+e.getMessage();
            throw new PluginException(msg);
        }
        List rtn = new ArrayList();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DriverManager.getConnection(url, user, pass);
            // Discover all Informix DB tables.
            stmt = conn.createStatement();
            setTableServices(rtn, conn, stmt);
            setChunkServices(rtn, conn, stmt);
        }
        catch (SQLException e) {
            String msg = "Error querying for services: "+e.getMessage();
            throw new PluginException(msg);
        }
        finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }
        return rtn;
    }

    private void setChunkServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(CHUNK_QUERY);
            while (rs != null && rs.next())
            {
                String chunkName = rs.getString(1).trim();
                ServiceResource service = new ServiceResource();
                service.setType(this, "DBSpace");
                service.setServiceName("chunk="+chunkName);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_CHUNK, chunkName);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }

    private void setTableServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(TABLE_QUERY);
            while (rs != null && rs.next())
            {
                String tablename = rs.getString(2).trim(),
                       dbname = rs.getString(1).trim();
                if (!isValidTablename(tablename))
                    continue;
                ServiceResource service = new ServiceResource();
                service.setType(this, TABLE);
                service.setServiceName("dbname="+dbname+", tablename="+tablename);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_TABLE, tablename);
                productConfig.setValue(PROP_DBNAME, dbname);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }

    private boolean isValidTablename(String tablename)
    {
        return (tablename.matches("[A-Za-z_][A-Za-z0-9_]*")) ? true : false;
    }
}
