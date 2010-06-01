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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class SybaseServerDetector
        extends ServerDetector
    implements AutoServerDetector {
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
    public static String PTQL_QUERY;
    static {
        if (isWin32()) {
            PTQL_QUERY =
                    "State.Name.re=sqlsrvr,State.Name.Pne=$1,Args.0.re=.*sqlsrvr.exe$";
        } else {
            PTQL_QUERY =
                    "State.Name.re=dataserver,State.Name.Pne=$1,Args.0.re=.*dataserver$";
        }
    }

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

    private Log log=LogFactory.getLog(SybaseServerDetector.class);
    public List getServerResources(ConfigResponse config) throws PluginException
    {
        log.debug("[getServerResources] config=" + config);
        List servers = new ArrayList();
        long pids[] = getPids(PTQL_QUERY);
        for (int i = 0; i < pids.length; i++) {
            List found = getServerList(pids[i], config);
            if (!found.isEmpty()) {
                servers.addAll(found);
            }
        }
        return servers;
    }

    public List getServerList(long pid, ConfigResponse config)
        throws PluginException
    {
        String[] args = getProcArgs(pid);
        String path = args[0];
        log.debug("[getServerList] (" + pid + ") args=" + Arrays.asList(args));

        // Only check the binaries if they match the path we expect
        List servers = new ArrayList();

        String name = null;
        String installpath = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-s")) {
                name = args[i + 1];
            } else if (args[i].startsWith("-s")) {
                name = args[i].substring(2);
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-d")) {
                installpath = args[i].substring(2);
            }
        }

        if ((path.indexOf("dataserver") == -1 && path.indexOf("sqlsrvr") == -1) || (name == null)) {
            return servers;
        }
        if (installpath == null) {
            installpath = path + " -s" + name;
        }

        String installdir = getParentDir(path, 3);

        String version = "";


        if (path.indexOf("12_0") != -1) {
            version = VERSION_12_0;
        } else if (path.indexOf("12_5") != -1) {
            version = VERSION_12_5;
        } else if (path.indexOf("15_0") != -1) {
            version = VERSION_15;
        } else {
            return servers;
        }

        // need to check if we are discovering the appropriate
        // server since 12_0 does not include any of the newer
        // versions in the hq-plugin.xml descriptor
        if (!version.equals(getTypeInfo().getVersion())) {
            return servers;
        }

        ConfigResponse measurementConfig = new ConfigResponse();
        measurementConfig.setValue("serverName", name);

        ServerResource server = createServerResource(installpath);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, new ConfigResponse());
        setMeasurementConfig(server,measurementConfig);
        server.setName(getPlatformName() + " " + SERVER_NAME + " " + version + " " + name);
        servers.add(server);

        log.debug("sysbase.aiid.orginal=" + SybaseProductPlugin.isOriginalAIID());
        log.debug("installdir=" + installdir);
        log.debug("installpath=" + installpath);
        if (SybaseProductPlugin.isOriginalAIID()) {
            server.setIdentifier(installdir);
        } else {
            server.setIdentifier(installpath);
        }

        return servers;
    }

    protected List discoverServices(ConfigResponse config)
            throws PluginException
    {
        log.debug("[discoverServices] config=" + config);
        List rtn = new ArrayList();

        String url  = config.getValue(PROP_URL);
        String user = config.getValue(PROP_USER);
        String pass = config.getValue(PROP_PASSWORD);
        String name = config.getValue("serverName");

        if (url != null) {
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
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try
            {
                java.util.Properties props = new java.util.Properties();
                props.put("CHARSET_CONVERTER_CLASS",
                        "com.sybase.jdbc3.utils.TruncationConverter");
                props.put("user", user);
                props.put("password", pass);
                conn = DriverManager.getConnection(url, props);
                // Discover all Sybase DB tables.
                stmt = conn.createStatement();
                setCacheServices(rtn, stmt);
                setSPMonitorConfigServices(rtn, stmt);
                setEngineServices(rtn, stmt);
                setSpaceAvailServices(rtn, stmt);
            }
            catch (SQLException e) {
                String msg = "Error querying for services: "+e.getMessage();
                throw new PluginException(msg, e);
            }
            finally {
                DBUtil.closeJDBCObjects(log, conn, stmt, rs);
            }
        }

        return rtn;
    }

    private void setSpaceAvailServices(List services, Statement stmt)
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
                try {
                    stmt.execute("use "+database);
                    stmt.execute("sp_helpsegment");
                    rs = stmt.getResultSet();
                    while (rs.next())
                    {
                        String segment = rs.getString("name");
                        ServiceResource service = new ServiceResource();
                        service.setType(this, TYPE_STORAGE);
                        service.setServiceName(database + "." + segment);
                        ConfigResponse productConfig = new ConfigResponse();
                        productConfig.setValue(PROP_DATABASE, database);
                        productConfig.setValue(PROP_SEGMENT, segment);
                        productConfig.setValue(PROP_PAGESIZE, pagesize);
                        service.setProductConfig(productConfig);
                        service.setMeasurementConfig();
                        services.add(service);
                    }
                } catch (SQLException e) {
                    if (log.isDebugEnabled()) {
                        log.error("[setSpaceAvailServices] database '" + database + "' > " + e.getMessage(), e);
                    }
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

    private void setEngineServices(List services, Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            rs = stmt.executeQuery(ENGINE_QUERY);
            int engine_col = rs.findColumn("engine");
            while (rs != null && rs.next())
            {
                String engineNum =
                        rs.getString(engine_col).trim().replaceAll("\\s+", "_");
                ServiceResource service = new ServiceResource();
                service.setType(this, TYPE_SP_SYSMON+"Engine");
                service.setServiceName("engine"+engineNum);
                ConfigResponse productConfig = new ConfigResponse();
                productConfig.setValue(PROP_ENGINE, "engine"+engineNum);
                productConfig.setValue("id", engineNum);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig();
                services.add(service);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }

    private void setCacheServices(List services, Statement stmt)
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
                services.add(service);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }

    private void setSPMonitorConfigServices(List services, Statement stmt)
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
                services.add(service);
            }
        }
        finally {
            if (rs != null) rs.close();
        }
    }
}
