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

package org.hyperic.hq.plugin.mysql_stats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.JDBCMeasurementPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.jdbc.DBUtil;

public class MySqlServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    private static final String _logCtx = MySqlServerDetector.class.getName();
    private final Log _log = LogFactory.getLog(_logCtx);
    // generic process name, generic server daemon
    private static final String PROCESS_NAME = "mysqld";
    // this PTQL query matches the PROCESS_NAME and returns the parent process
    // id.  An example process path is /usr/sbin/mysqld
    private static final String PTQL_QUERY = "State.Name.re="+PROCESS_NAME+
        ",State.Name.Pne=$1,Args.0.re=.*"+PROCESS_NAME+"$";
    private static final String VERSION_4_0_x = "4.0.x",
                                VERSION_4_1_x = "4.1.x",
                                VERSION_5_0_x = "5.0.x",
                                VERSION_5_1_x = "4.1.x";
    private static final Pattern REGEX_VER_4_0 = Pattern.compile("Ver 4.0.[0-9]+"),
                                 REGEX_VER_4_1 = Pattern.compile("Ver 4.1.[0-9]+"),
                                 REGEX_VER_5_0 = Pattern.compile("Ver 5.0.[0-9]+"),
                                 REGEX_VER_5_1 = Pattern.compile("Ver 5.1.[0-9]+");

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

    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException
    {
        final List rtn = new ArrayList();
        try {
            setGlobalStatusService(rtn, serverConfig);
            setSlaveStatusService(rtn, serverConfig);
            setMasterSlaveStatusService(rtn, serverConfig);
        } catch (SQLException e) {
            String msg = "Error querying for services: "+e.getMessage();
            throw new PluginException(msg, e);
        }
        return rtn;
    }
    
    private static final Connection getConnection(String url, String user,
                                                  String pass,
                                                  ConfigResponse config)
        throws SQLException
    {
        final String d = MySqlStatsMeasurementPlugin.DEFAULT_DRIVER;
        try {
            Driver driver = (Driver)Class.forName(d).newInstance();
            final Properties props = new Properties();
            props.put("user", user);
            props.put("password", pass);
            return driver.connect(url, props);
        } catch (InstantiationException e) {
            throw new SQLException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new SQLException(e.getMessage());
        }
    }

    private void setSlaveStatusService(List services,
                                       ConfigResponse serverConfig) {
        String url  = serverConfig.getValue(JDBCMeasurementPlugin.PROP_URL);
        String user = serverConfig.getValue(JDBCMeasurementPlugin.PROP_USER);
        String pass = serverConfig.getValue(JDBCMeasurementPlugin.PROP_PASSWORD);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection(url, user, pass, serverConfig);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("show slave status");
            if (rs.next()) {
                ServiceResource service = new ServiceResource();
                service.setType(this, "Show Slave Status");
                service.setServiceName("Show Slave Status");
                ConfigResponse productConfig = new ConfigResponse();
                service.setProductConfig(productConfig);
                service.setMeasurementConfig(serverConfig);
//                service.setControlConfig();
                services.add(service);
            }
        } catch (SQLException e) {
            // This is most likely a permissions thing.
            // The issue is that if you have permissions to run the
            // command, replication still may not be enabled.
            // Therefore just return and ignore service.
            return;
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, conn, stmt, rs);
        }
    }

    private void setMasterSlaveStatusService(List services,
                                       ConfigResponse serverConfig) {
        String url  = serverConfig.getValue(JDBCMeasurementPlugin.PROP_URL);
        String user = serverConfig.getValue(JDBCMeasurementPlugin.PROP_USER);
        String pass = serverConfig.getValue(JDBCMeasurementPlugin.PROP_PASSWORD);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection(url, user, pass, serverConfig);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("show full processlist");
            int userCol = rs.findColumn("User"),
                addrCol = rs.findColumn("Host");
            while (rs.next()) {
                String pUser = rs.getString(userCol);
                if (!pUser.equalsIgnoreCase("slave")) {
                    continue;
                }
                ServiceResource service = new ServiceResource();
                service.setType(this, "Slave Status");
                service.setServiceName("Slave Status");
                ConfigResponse productConfig = new ConfigResponse();
                service.setProductConfig(productConfig);
                ConfigResponse replConfig = new ConfigResponse();
                String addr = rs.getString(addrCol);
                replConfig.setValue("slaveAddress", addr);
                service.setMeasurementConfig(replConfig);
//                service.setControlConfig();
                services.add(service);
            }
        } catch (SQLException e) {
            return;
        } finally {
            DBUtil.closeJDBCObjects(_logCtx, conn, stmt, rs);
        }
    }

    private void setGlobalStatusService(List services,
                                        ConfigResponse serverConfig)
        throws SQLException
    {
        ServiceResource service = new ServiceResource();
        service.setType(this, "Show Global Status");
        service.setServiceName("Show Global Status");
        ConfigResponse productConfig = new ConfigResponse();
        service.setProductConfig(productConfig);
        service.setMeasurementConfig(serverConfig);
//        service.setControlConfig();
        services.add(service);
    }

    private static List getServerProcessList()
    {
        List servers = new ArrayList();
        long[] pids = getPids(PTQL_QUERY);
        for (int i=0; i<pids.length; i++)
        {
            String exe = getProcExe(pids[i]);
            if (exe == null) {
                continue;
            }
            File binary = new File(exe);
            if (!binary.isAbsolute())
                continue;
            servers.add(binary.getAbsolutePath());
        }
        return servers;
    }

    private List getServerList(String path)
        throws PluginException
    {
        ConfigResponse productConfig = new ConfigResponse();
        List servers = new ArrayList();
        String installdir = getParentDir(path, 1);
        String version = getVersion(path);
        // ensure this instance of ServerDetector is associated with the
        // correct version
        if (!getTypeInfo().getVersion().equals(version)) {
            return Collections.EMPTY_LIST;
        }
        ServerResource server = createServerResource(installdir);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        // server.setProductConfig(productConfig);
        setProductConfig(server, productConfig);
        // sets a default Measurement Config property with no values
        server.setMeasurementConfig();
        server.setName("MySQL Stats "+version);
        servers.add(server);
        return servers;
    }
    
    private String getVersion(String executable) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Execute exec = new Execute(new PumpStreamHandler(output));
            exec.setCommandline(
                new String[] {executable, "--help"});
            int res = exec.execute();
            if (res != 0) {
                return null;
            }
            String[] toks = output.toString().split("\n");
            String out = toks[0];
            if (_log.isDebugEnabled()) {
                _log.debug("first output line of " + executable +
                           " --help: " + out);
            }
            // 12/17/2008, did not test with 4.0.x.  Can't download from mysql
            // archives anymore
            if (REGEX_VER_4_0.matcher(out).find()) {
                return VERSION_4_0_x;
            } else if (REGEX_VER_4_1.matcher(out).find()) {
                return VERSION_4_1_x;
            } else if (REGEX_VER_5_0.matcher(out).find()) {
                return VERSION_5_0_x;
            } else if (REGEX_VER_5_1.matcher(out).find()) {
                return VERSION_5_1_x;
            }
        } catch (Exception e) {
            _log.warn("Could not get the version of mysql: " + e.getMessage(), e);
        }
        return null;
    }
}
