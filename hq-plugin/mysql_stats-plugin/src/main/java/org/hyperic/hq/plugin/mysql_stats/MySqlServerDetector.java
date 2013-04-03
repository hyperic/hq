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
 *
 */
 
package org.hyperic.hq.plugin.mysql_stats;
 
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.jdbc.DBUtil;
import org.jboss.remoting.detection.util.DetectorUtil;
 
public class MySqlServerDetector
 
    extends ServerDetector
 
    implements AutoServerDetector
{
 
    private static final String _logCtx = MySqlServerDetector.class.getName();
 
    private final Log _log = LogFactory.getLog(_logCtx);
 
    private Connection _conn;
 
    private static final List _ptqlQueries = new ArrayList();
 
    private String _validQuery;
 
    static {
 
        _ptqlQueries.add("State.Name.eq=mysqld");
 
        if (isWin32()) {
 
            _ptqlQueries.add("State.Name.eq=mysqld-nt");
 
        }
}
 
    private static final String VERSION_4_0_x = "4.0.x",
 
                                VERSION_4_1_x = "4.1.x",
 
                                VERSION_5_0_x = "5.0.x",
 
                                VERSION_5_1_x = "5.1.x",
 
                                VERSION_5_5_x = "5.5.x",
 
                                TABLE_SERVICE = "Table",
 
                                SLAVE_STATUS  = "Slave Status",
 
                                SHOW_SLAVE_STATUS  = "Show Slave Status";
 
    private static final Pattern
 
        REGEX_VER_4_0 = Pattern.compile("Ver 4.0.[0-9]+"),
 
        REGEX_VER_4_1 = Pattern.compile("Ver 4.1.[0-9]+"),
 
        REGEX_VER_5_0 = Pattern.compile("Ver 5.0.[0-9]+"),
 
        REGEX_VER_5_1 = Pattern.compile("Ver 5.1.[0-9]+"),
 
        REGEX_VER_5_5 = Pattern.compile("Ver 5.5.[0-9]+");
 
 
    public List getServerResources(ConfigResponse platformConfig)
 
        throws PluginException
{
 
        List servers = new ArrayList();
 
        Map paths = getServerProcessMap();
 
        for (Iterator it=paths.entrySet().iterator(); it.hasNext(); ) {
 
            Map.Entry entry = (Map.Entry)it.next();
 
            Long pid = (Long)entry.getKey();
 
            String dir = (String)entry.getValue();
 
            _log.debug("pid="+pid+", dir="+dir);
 
            // no need to create unique ptql if there is only one mysqld process
 
            // TODO scottmf, need to augment this to automatically find
 
            // uniqueness in the process args and apply only one or two of them
 
            // to the ptql instead of the brute force approach of appending all
 
            // args to the query
 
            String[] args = (paths.size() == 1) ?
 
                new String[0] :
 
                getProcArgs(pid.longValue());
 
            List found = getServerList(dir, args,pid);
 
            if (!found.isEmpty())
 
                servers.addAll(found);
 
        }
 
        return servers;
}
 
 
    protected List discoverServices(ConfigResponse serverConfig)
 
        throws PluginException
{
 
        final List rtn = new ArrayList();
 
        String url  = serverConfig.getValue(JDBCMeasurementPlugin.PROP_URL);
 
        String user = serverConfig.getValue(JDBCMeasurementPlugin.PROP_USER);
 
        String pass = serverConfig.getValue(JDBCMeasurementPlugin.PROP_PASSWORD);
 
        pass = (pass == null) ? "" : pass;
 
        pass = (pass.matches("^\\s*$")) ? "" : pass;
 
        try {
 
            _conn = getConnection(url, user, pass, serverConfig);
 
            setTableServices(rtn, serverConfig);
 
            setSlaveStatusService(rtn, serverConfig);
 
            setMasterSlaveStatusService(rtn, serverConfig);
 
        } catch (SQLException e) {
 
            throw new PluginException(e);
 
        } finally {
 
            DBUtil.closeConnection(_logCtx, _conn);
 
        }
 
        return rtn;
}
    
 
    private void setTableServices(List services, ConfigResponse serverConfig) {
 
        final String tableRegex = serverConfig.getValue("tableRegex", "");
 
        if (tableRegex.trim().length() <= 0) {
 
            _log.debug("Table config is blank, skipping table AI");
 
            return;
 
        }
 
        _log.debug("Discovering tables with regex " + tableRegex);
 
        final Pattern regex =
 
            Pattern.compile(tableRegex, Pattern.CASE_INSENSITIVE);
 
        Statement stmt = null;
 
        ResultSet rs = null;
 
        final String sql =
 
            "SELECT table_name, table_schema " +
 
            "FROM information_schema.tables " +
 
            "WHERE lower(table_schema) != 'information_schema' " +
 
            "AND engine is not null";
 
        try {
 
            stmt = _conn.createStatement();
 
            rs = stmt.executeQuery(sql);
 
            final int tableNameCol = rs.findColumn("table_name"),
 
                      dbNameCol    = rs.findColumn("table_schema");
 
            final boolean debug = _log.isDebugEnabled();
 
            while (rs.next()) {
 
                final String table = rs.getString(tableNameCol);
 
                final String dbName = rs.getString(dbNameCol);
 
                if (regex.matcher(table).find()) {
 
                    if (debug) {
 
                        _log.debug("Adding table " + table);
 
                    }
 
                    ServiceResource service = new ServiceResource();
 
                    service.setType(this, TABLE_SERVICE);
 
                    service.setServiceName(dbName + "/" + table);
 
                    ConfigResponse productConfig = new ConfigResponse();
 
                    productConfig.setValue("table", table);
 
                    productConfig.setValue("database", dbName);
 
                    service.setProductConfig(productConfig);
 
                    service.setMeasurementConfig(serverConfig);
 
                    service.setControlConfig(productConfig);
 
                    services.add(service);
 
                }
 
            }
 
        } catch (SQLException e) {
 
            _log.warn(e.getMessage(), e);
 
        } finally {
 
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
 
        }
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
 
            pass = (pass == null) ? "" : pass;
 
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
 
        Statement stmt = null;
 
        ResultSet rs = null;
 
        try {
 
            stmt = _conn.createStatement();
 
            rs = stmt.executeQuery(SHOW_SLAVE_STATUS.toLowerCase());
 
            if (rs.next()) {
 
                ServiceResource service = new ServiceResource();
 
                service.setType(this, SHOW_SLAVE_STATUS);
 
                service.setServiceName(SHOW_SLAVE_STATUS);
 
                ConfigResponse productConfig = new ConfigResponse();
 
                service.setProductConfig(productConfig);
 
                service.setMeasurementConfig(serverConfig);
 
                services.add(service);
 
            }
 
        } catch (SQLException e) {
 
            // This is most likely a permissions thing.
 
            // The issue is that if you have permissions to run the
 
            // command, replication still may not be enabled.
 
            // Therefore just return and ignore service.
 
            return;
 
        } finally {
 
            DBUtil.closeJDBCObjects(_logCtx, null, stmt, rs);
 
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
 
                final String addr = rs.getString(addrCol);
 
                ServiceResource service = new ServiceResource();
 
                service.setType(this, SLAVE_STATUS);
 
                service.setServiceName(SLAVE_STATUS + " " + addr);
 
                ConfigResponse productConfig = new ConfigResponse();
 
                service.setProductConfig(productConfig);
 
                ConfigResponse replConfig = new ConfigResponse();
 
                replConfig.setValue("slaveAddress", addr);
 
                service.setMeasurementConfig(replConfig);
 
                services.add(service);
 
            }
 
        } catch (SQLException e) {
 
            return;
 
        } finally {
 
            DBUtil.closeJDBCObjects(_logCtx, conn, stmt, rs);
 
        }
}
 
 
    private Map getServerProcessMap()
{
 
        final Map servers = new HashMap();
 
        final List pidArray = new ArrayList();
 
        for (final Iterator it=_ptqlQueries.iterator(); it.hasNext(); ) {
 
            final String ptql = (String)it.next();
 
            final long[] pids = getPids(ptql);
 
            if (pids.length > 0) {
 
                // [HHQ-3218] this is hacky, but since we don't know which
 
                // version of mysql is running until the ptql runs, we don't
 
                // know what to set for the "process.query" config prop
 
                //  http://dev.mysql.com/doc/refman/5.1/en/windows-select-server.html
 
                _validQuery = ptql;
 
                pidArray.add(pids);
 
            }
 
        }
 
        for (final Iterator it=pidArray.iterator(); it.hasNext(); ) {
 
            final long[] pids = (long[])it.next();
 
            for (int i=0; i<pids.length; i++) {
 
                final String exe = getProcExe(pids[i]);
 
                _log.debug("exe="+exe+" pid="+pids[i]);
 
                if (exe == null) {
 
                    continue;
 
                }
 
                final File binary = new File(exe);
 
                if (!binary.isAbsolute()) {
 
                    continue;
 
                }
 
                servers.put(new Long(pids[i]), binary.getAbsolutePath());
 
            }
 
        }
 
        return servers;
}
 
 
    private List getServerList(String path, String[] args, long pid)
 
        throws PluginException
{
 
        List servers = new ArrayList();
 
        String installdir = getParentDir(path, 2);
 
        String version = getVersion(path, "--version");
 
 
        if (version == null) {
 
	 _log.debug("Version returned null, looking for version in --version output. Trying --help");
 
            version = getVersion(path, "--help");
 
        }
 
 
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
 
        ConfigResponse productConfig = new ConfigResponse();
 
        productConfig.setValue("process.query", _validQuery + getPtqlArgs(args));
 
        populateListeningPorts(pid, productConfig, true);
 
        setProductConfig(server, productConfig);
 
        // sets a default Measurement Config property with no values
 
        setMeasurementConfig(server, new ConfigResponse());
 
        server.setName(getPlatformName() + " MySQL Stats "+version);
 
        servers.add(server);
 
        return servers;
}
    
 
    private String getPtqlArgs(String[] args) {
 
        StringBuffer rtn = new StringBuffer();
 
        for (int i=0; i<args.length; i++) {
 
            if (args[i] == null) {
 
                continue;
 
            }
 
            String[] toks = args[i].split("=", 2);
 
            if (toks.length < 2) {
 
                continue;
 
            }
 
            rtn.append(",Args.*.ct=" + toks[1]);
 
        }
 
        return rtn.toString();
}
    
 
    private String getVersion(String executable, String arg) {
 
        try {
 
            ByteArrayOutputStream output = new ByteArrayOutputStream();
 
            Execute exec = new Execute(new PumpStreamHandler(output));
 
            exec.setCommandline(new String[] {executable, arg});
 
            int res = exec.execute();
 
            if (res != 0) {
 
                return null;
 
            }
 
            String out = output.toString();
 
            if (_log.isDebugEnabled()) {
 
	 _log.debug("Version detected from output of " + executable + " " + arg +":\n" + out);
 
            }
 
            if (REGEX_VER_4_0.matcher(out).find()) {
 
                return VERSION_4_0_x;
 
            } else if (REGEX_VER_4_1.matcher(out).find()) {
 
                return VERSION_4_1_x;
 
            } else if (REGEX_VER_5_0.matcher(out).find()) {
 
                return VERSION_5_0_x;
 
            } else if (REGEX_VER_5_1.matcher(out).find()) {
 
                return VERSION_5_1_x;
 
            } else if (REGEX_VER_5_5.matcher(out).find()) {
 
                return VERSION_5_5_x;
 
            }
 
        } catch (Exception e) {
 
            _log.warn("Could not get the version of mysql: " + e.getMessage(), e);
 
        }
 
        return null;
}
    
    private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            _log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            _log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            _log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}
