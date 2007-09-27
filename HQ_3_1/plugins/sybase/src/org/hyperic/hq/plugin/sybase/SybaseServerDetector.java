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

package org.hyperic.hq.plugin.sybase;

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

public class SybaseServerDetector
    extends ServerDetector
    implements FileServerDetector, RegistryServerDetector, AutoServerDetector
{
    private static final boolean DEBUG = SybasePluginUtil.DEBUG;

    static final String JDBC_DRIVER        = SybasePluginUtil.JDBC_DRIVER,
                        DEFAULT_URL        = SybasePluginUtil.DEFAULT_URL,
                        PROP_ENGINE        = SybasePluginUtil.PROP_ENGINE,
                        PROP_PAGESIZE      = SybasePluginUtil.PROP_PAGESIZE,
                        PROP_DATABASE      = SybasePluginUtil.PROP_DATABASE,
                        PROP_SEGMENT       = SybasePluginUtil.PROP_SEGMENT,
                        PROP_CACHE_NAME    = SybasePluginUtil.PROP_CACHE_NAME,
                        PROP_CONFIG_OPTION = SybasePluginUtil.PROP_CONFIG_OPTION,
                        TYPE_SP_MONITOR_CONFIG =
                                SybasePluginUtil.TYPE_SP_MONITOR_CONFIG,
                        TYPE_SP_SYSMON = SybasePluginUtil.TYPE_SP_SYSMON,
                        TYPE_STORAGE  = SybasePluginUtil.TYPE_STORAGE,
                        PROP_TABLE    = SybaseMeasurementPlugin.PROP_TABLE,
                        PROP_URL      = SybaseMeasurementPlugin.PROP_URL,
                        PROP_USER     = SybaseMeasurementPlugin.PROP_USER,
                        PROP_PASSWORD = SybaseMeasurementPlugin.PROP_PASSWORD;

    private static Log log = LogFactory.getLog("SybaseServerDetector");

    private static final String SYBASE_VERSION = "(Sybase)";
    private static final String PTQL_QUERY =
        "State.Name.re=dataserver,State.Name.Pne=$1,Args.0.re=.*dataserver$";

    // Config Value Usage Query
    private static final String SP_MONITOR_CONFIG = "sp_monitorconfig 'all'";
    // Data Cache discovery query
    private static final String CACHE_QUERY =
        "select name from sysconfigures "+
        "where lower(comment) = lower('User Defined Cache')";

    private static final String ENGINE_QUERY = "select engine from sysengines";

    // Resource types and versions
    static final String SERVER_NAME = "Sybase",
                        TABLE       = "Table",
                        INDEX       = "Index",
                        VERSION_15  = "15.x",
                        VERSION_12_5 = "12.5.x",
                        VERSION_12_0 = "12.x";

    private static List getServerProcessList()
    {
        List servers = new ArrayList();
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
        productConfig.setValue(PROP_USER, "sa");
        productConfig.setValue(PROP_PASSWORD, "");

        List servers = new ArrayList();
        String version = "";

        // Only check the binaries if they match the path we expect
        if (path.indexOf("dataserver") == -1)
            return servers;

        if (path.indexOf("12_0") != -1)
            version = VERSION_12_0;
        else if (path.indexOf("12_5") != -1)
            version = VERSION_12_5;
        else if (path.indexOf("15_0") != -1)
            version = VERSION_15;
        else
            return servers;

        String installdir = getParentDir(path, 3);
        ServerResource server = createServerResource(installdir);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, productConfig);
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

        pass = (pass == null) ? "" : pass;
        pass = (pass.matches("^\\s*$")) ? "" : pass;

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
            // Discover all Sybase DB tables.
            stmt = conn.createStatement();
            setCacheServices(rtn, conn, stmt);
            setSPMonitorConfigServices(rtn, conn, stmt);
            setEngineServices(rtn, conn, stmt);
            setSpaceAvailServices(rtn, conn, stmt);
        }
        catch (SQLException e) {
            String msg = "Error querying for services: "+e.getMessage();
            throw new PluginException(msg, e);
        }
        finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }
        return rtn;
    }

    private void setSpaceAvailServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery("select pagesize=@@maxpagesize");
            int pagesize = -1;
            if (rs.next())
                pagesize = rs.getInt("pagesize");
            else
                throw new SQLException();
            rs.close();
            List databases = getDatabases(stmt);
            for (int i=0; i<databases.size(); i++)
            {
                String database = (String)databases.get(i);
                stmt.execute("use "+database);
                stmt.execute("sp_helpsegment");
                rs = stmt.getResultSet();
                while (rs.next())
                {
                    String segment = rs.getString("name");
                    ServiceResource service = new ServiceResource();
                    service.setType(this, TYPE_STORAGE);
                    service.setServiceName(database+"."+segment);
                    ConfigResponse productConfig = new ConfigResponse();
                    productConfig.setValue(PROP_DATABASE, database);
                    productConfig.setValue(PROP_SEGMENT, segment);
                    productConfig.setValue(PROP_PAGESIZE, pagesize);
                    service.setProductConfig(productConfig);
                    service.setMeasurementConfig();
                    service.setControlConfig();
                    services.add(service);
                }
            }
        }
        finally
        {
            if (rs != null) rs.close();
            stmt.execute("use master");
        }
    }

    private List getDatabases(Statement stmt) throws SQLException
    {
        List rtn = new ArrayList();
        ResultSet rs = null;
        try
        {
            stmt.execute("use master");
            rs = stmt.executeQuery("select name from sysdatabases");
            int name_col = rs.findColumn("name");
            while (rs.next())
            {
                String database = rs.getString(name_col);
                rtn.add(database);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
        return rtn;
    }

    private void setEngineServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(ENGINE_QUERY);
            int engine_col = rs.findColumn("engine");
            while (rs != null && rs.next())
            {
                String engineNum = rs.getString(engine_col).trim().replaceAll("\\s+", "_");
                ServiceResource service = new ServiceResource();
                service.setType(this, TYPE_SP_SYSMON+"Engine");
                service.setServiceName("engine"+engineNum);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_ENGINE, "engine"+engineNum);
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

    private void setCacheServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(CACHE_QUERY);
            int name_col = rs.findColumn("name");
            while (rs != null && rs.next())
            {
                String cacheName = rs.getString(name_col).trim().replaceAll("\\s+", "_");
                ServiceResource service = new ServiceResource();
                service.setType(this, TYPE_SP_SYSMON+"Cache");
                service.setServiceName("Cachename="+cacheName);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue("cachename", cacheName);
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

    private void setSPMonitorConfigServices(List services, Connection conn, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(SP_MONITOR_CONFIG);
            while (rs != null && rs.next())
            {
//need to figure out how to get the full option name instead
//of the truncated text
                String configOption = rs.getString("Name").trim();
                ServiceResource service = new ServiceResource();
                service.setType(this, TYPE_SP_MONITOR_CONFIG);
                service.setServiceName("ConfigOption="+configOption);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_CONFIG_OPTION, configOption);
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
}
