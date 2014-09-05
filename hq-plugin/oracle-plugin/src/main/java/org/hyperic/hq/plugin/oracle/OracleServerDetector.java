/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2014], Hyperic, Inc.
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

package org.hyperic.hq.plugin.oracle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OracleServerDetector extends ServerDetector implements AutoServerDetector {

    private final transient Log log =  LogFactory.getLog("OracleServerDetector");
        
    private static final String PTQL_QUERY = "State.Name.eq=oracle";
    
    private static final String TNS_PTQL_QUERY = "State.Name.eq=tnslsnr";

    private static final String PROP_PROC_PTQL = "process.ptql";
    
    private static final String PROP_TNSNAMES = "tnsnames";

    private static final String ORATAB = "/etc/oratab";
    private static final String ORATAB2 = "/var/opt/oracle/oratab";

    private static final Pattern _serviceNameEx =
        Pattern.compile("\\(\\s*service_name\\s*=", Pattern.CASE_INSENSITIVE);

    // Versions
    static final String VERSION_8i = "8i";
    static final String VERSION_9i = "9i";
    static final String VERSION_10g = "10g";
    static final String VERSION_11g = "11g";
    static final String VERSION_12g = "12g";

    // User instance
    static final String USER_INSTANCE = "User Instance";
    static final String USER_QUERY =
        "SELECT UNIQUE username FROM V$SESSION WHERE username IS NOT NULL";
    static final String DBA_USER_QUERY =
        "SELECT * FROM DBA_USERS WHERE USERNAME = ";

    // Tablespace
    static final String TABLESPACE = "Tablespace";
    static final String TABLESPACE_QUERY = "SELECT * FROM DBA_TABLESPACES";

    // Table
    static final String SEGMENT = "Segment";
    static final String SEGMENT_QUERY = "select SEGMENT_NAME, TABLESPACE_NAME" +
                                        " FROM USER_SEGMENTS" +
                                        " WHERE SEGMENT_NAME not like 'BIN$%'" +
                                        " and SEGMENT_NAME not like 'SYS_%'";

    // Server custom props
    static final String VERSION_QUERY = 
        "SELECT * FROM V$VERSION";
    
    private List<OracleInfo> getOraclesInfoFromProcess() {
        ArrayList<OracleInfo> servers = new ArrayList<OracleInfo>();

        long[] pids = getPids(PTQL_QUERY);

        for (long pid : pids) {
            String exe = getProcExe(pid);
            String[] args = getProcArgs(pid);
            if ((exe != null) && (args != null)) {
                File oracleExe = new File(exe);
                File binDirectory = oracleExe.getParentFile();
                if (binDirectory.getName().equals("bin")) {
                    String home = binDirectory.getParent();
                    String sid = args[1];
                    log.debug("[getServerProcessList] Found SID='" + sid + "' ORACLE_HOME='" + home + "'");
                    servers.add(new OracleInfo(home, sid));
                } else {
                    log.debug("[getServerProcessList] Unable to locate oracle home for PID='" + pid + "' exe='" + exe + "'");
                }
            } else {
                log.debug("[getServerProcessList] Unable to get info for oracle PID='" + pid + "'");
            }
        }

        return servers;
    }

    private List<OracleInfo> getOraclesInfoFromOratab() {
        ArrayList<OracleInfo> servers = new ArrayList<OracleInfo>();
        String oratabStr = "";

        File oratab = new File(ORATAB);
        if (!oratab.exists()) {
            oratab = new File(ORATAB2);
        }

        InputStream in = null;
        try {
            in = new FileInputStream(oratab);
            oratabStr = inputStreamAsString(in);
        } catch (IOException ex) {
            log.debug("[getOraclesInfoFromOratab] Error: '" + oratab + "' " + ex, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    log.debug("[getOraclesInfoFromOratab] Error: '" + oratab + "' " + ex, ex);
                }
            }
        }

        Pattern regex = Pattern.compile("(^[^#][^:]*):([^:]*):[YyNnWw]:?$", Pattern.MULTILINE);
        Matcher m = regex.matcher(oratabStr);
        while (m.find()) {
            String home = m.group(2);
            String sid = m.group(1);
            log.debug("[getOraclesInfoFromOratab] Found SID='" + sid + "' ORACLE_HOME='" + home + "'");
            servers.add(new OracleInfo(home, sid));
        }

        return servers;
    }
        
    private ServerResource getOracleServer(OracleInfo oracle) throws PluginException {
        log.debug("[getOracleServer] oracle=" + oracle);
        String version = getTypeInfo().getVersion();

        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);

        ConfigResponse productConfig = new ConfigResponse();
        setListeningPorts(productConfig);
        productConfig.setValue("jdbcUrl", "jdbc:oracle:thin:@"+oracle.host+":"+oracle.port+":" + oracle.sid);

        ServerResource oracleServer = createServerResource(oracle.home);
        oracleServer.setIdentifier(oracle.home);
        setCustomProperties(oracleServer, cprop);
        setProductConfig(oracleServer, productConfig);
        oracleServer.setMeasurementConfig();
        if (!version.equals(VERSION_9i) && !version.equals(VERSION_8i)) {
            oracleServer.setControlConfig();
        }

        // HHQ-3577 allow listener names in tnsnames.ora to be used in the url
        String fs = File.separator;
        String tnsDir = getTnsNamesDir(oracle.home, "network" + fs + "admin" + fs + "tnsnames.ora");
        if (log.isDebugEnabled()) {
            log.debug("[getOracleServer] using tns dir as " + tnsDir);
        }
        System.setProperty("oracle.net.tns_admin", tnsDir);

        return oracleServer;
    }

    private void setListeningPorts(ConfigResponse productConfig) {		
    	Set<Long> allPids = new HashSet<Long>();
    	for (long pid : getPids(PTQL_QUERY)) {
    		allPids.add(pid);
    	}
    	for (long pid : getPids(TNS_PTQL_QUERY)) {
    		allPids.add(pid);
    	}
    	populateListeningPorts(allPids, productConfig);
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException
    {
        List servers = new ArrayList();
        List<OracleInfo> oracles = new ArrayList<OracleInfo>();
        Set<OracleInfo> validOracles = new HashSet<OracleInfo>();
        
        if (isWin32()) {
            oracles.addAll(getOraclesInfoFromProcess());
        } else {
            oracles.addAll(getOraclesInfoFromOratab());
        }

        for (OracleInfo oracle : oracles) {
            if (isValidVersion(oracle.home)) {
                if (readListenerInfo(oracle)) {
                    log.debug("[getServerResources] Valid Oracle='" + oracle + "'");
                    validOracles.add(oracle);
                } else {
                    log.debug("[getServerResources] Listener DOWN Oracle='" + oracle + "'");
                }
            } else {
                log.debug("[getServerResources] Incorrect version Oracle='" + oracle + "'");
            }
        }

        for (OracleInfo oracle : validOracles) {
            ServerResource oracleServer = getOracleServer(oracle);
            servers.add(oracleServer);
        }

        return servers;
    }

    // Discover Oracle services
    @Override
    protected List discoverServices(ConfigResponse config)
        throws PluginException
    {
        // HHQ-3577 allow listener names in tnsnames.ora to be used in the url
        String tnsDir = getTnsNamesDir(
            config.getValue(ProductPlugin.PROP_INSTALLPATH),
            config.getValue(PROP_TNSNAMES));
        if (log.isDebugEnabled()) log.debug("using tns dir as " + tnsDir);
        System.setProperty("oracle.net.tns_admin", tnsDir);
        
        String url = config.getValue(OracleMeasurementPlugin.PROP_URL);
        if (url == null) {
        	log.warn("No value for config property " + OracleMeasurementPlugin.PROP_URL +
        			", no services will be discovered.");
        	return null;
        }

        String user = config.getValue(OracleMeasurementPlugin.PROP_USER);
        if (user == null) {
        	log.info("No value for config property " + OracleMeasurementPlugin.PROP_USER);
        }

        String pass = config.getValue(OracleMeasurementPlugin.PROP_PASSWORD);
        if (pass == null) {
        	log.info("No value for config property " + OracleMeasurementPlugin.PROP_PASSWORD);
        }

        ArrayList services = new ArrayList();
        Connection conn = null;
        Statement stmt = null;
        try
        {
            String instance = url.substring(url.lastIndexOf(':') + 1);
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            services.addAll(getUserServices(stmt, instance));
            services.addAll(getTablespaceServices(stmt, instance));
            // turning this off by default.
            // There are too many table that this will discover
            // most of which the user probably won't care about.
            // Also work needs to be done by the user to enable
            // scheduled control actions to do an Anaylze per table
            // so that the system info gets updated before the
            // size is calc'd.
            //services.addAll(getSegmentServices(stmt, instance));
            services.addAll(getProcessServices(config));
            // this requires extra config on the user side to set ORACLE_HOME
            // env var, so to avoid confusion disabling
            //services.addAll(getTnsServices(config));
            setCustomProps(stmt);
        }
        catch (SQLException e)
        {
            // Try to do some investigation of what went wrong
            if (e.getMessage().indexOf("table or view does not exist") != -1)
            {
                log.error("System table does not exist, make sure that " +
                          " the Oracle user specified has the correct " +
                          " privileges.  See the HQ server configuration " +
                          " page for more information");
                return services;
            }
            // Otherwise, dump the error.
            throw new PluginException("Error querying for Oracle " +
                                      "services: " + e.getMessage(), e);
        }
        finally {
            DBUtil.closeJDBCObjects(log, conn, stmt, null);
        }
        return services;
    }

    private void setCustomProps(Statement stmt)
        throws SQLException
    {
        ResultSet rs = null;
        try
        {
            // Query for server inventory properties
            ConfigResponse props = new ConfigResponse();
            rs = stmt.executeQuery(VERSION_QUERY);
            if (rs != null && rs.next()) {
                String version = rs.getString(1);
                props.setValue("version", version);
            }
            setCustomProperties(props);
        }
        finally {
            DBUtil.closeResultSet(log, rs);
        }
    }

    private List getUserServices(Statement stmt, String instance)
        throws SQLException
    {
        // Discover the user instances, for user instances to be
        // discovered, the user must be connected to the database.
        List rtn = new ArrayList();
        ResultSet rs = null;
        try
        {
            // Set server description
            setDescription("Oracle " + instance + " database instance");

            // Discover user instances
            ArrayList users = new ArrayList();
            rs = stmt.executeQuery(USER_QUERY);
            while (rs.next()) {
                String username = rs.getString(1);
                users.add(username);
            }
            rs.close();
            for (int i=0; i<users.size(); i++)
            {
                String username = (String)users.get(i);
                ServiceResource service = new ServiceResource();
                service.setType(this, USER_INSTANCE);
                service.setServiceName(username);
                service.setDescription("User of the " + instance + 
                                       " database instance");
                ConfigResponse productConfig = new ConfigResponse();
                ConfigResponse metricConfig = new ConfigResponse();
                productConfig.setValue(OracleMeasurementPlugin.PROP_USERNAME,
                                       username);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig(metricConfig);
                // Query for service inventory properties
                rs = stmt.executeQuery(DBA_USER_QUERY + "'" + username + "'");
                if (rs != null && rs.next())
                {
                    ConfigResponse svcProps = new ConfigResponse();
                    svcProps.setValue("status",
                                      rs.getString("ACCOUNT_STATUS"));
                    svcProps.setValue("default_tablespace",
                                      rs.getString("DEFAULT_TABLESPACE"));
                    svcProps.setValue("temp_tablespace",
                                      rs.getString("TEMPORARY_TABLESPACE"));
                    service.setCustomProperties(svcProps);
                }
                rtn.add(service);
            }
        }
        finally {
            DBUtil.closeResultSet(log, rs);
        }
        return rtn;
    }

    private List getSegmentServices(Statement stmt, String instance)
        throws SQLException
    {
        List rtn = new ArrayList();
        ResultSet rs = null;
        try
        {
            // Discover tables
            rs = stmt.executeQuery(SEGMENT_QUERY);
            int segment_col = rs.findColumn("SEGMENT_NAME");
            int ts_col = rs.findColumn("TABLESPACE_NAME");
            while (rs.next())
            {
                String segment = rs.getString(segment_col);
                String tablespace = rs.getString(ts_col);
                ServiceResource service = new ServiceResource();
                service.setType(this, SEGMENT);
                service.setServiceName(segment);
                service.setDescription("Segment in the " + instance +
                                       " database instance");
                ConfigResponse productConfig = new ConfigResponse();
                ConfigResponse metricConfig = new ConfigResponse();
                productConfig.setValue(OracleMeasurementPlugin.PROP_SEGMENT,
                                       segment);
                productConfig.setValue(OracleMeasurementPlugin.PROP_TABLESPACE,
                                       tablespace);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig(metricConfig);
                service.setControlConfig();
                rtn.add(service);
            }
        }
        finally {
            DBUtil.closeResultSet(log, rs);
        }
        return rtn;
    }

    private List getTablespaceServices(Statement stmt, String instance)
        throws SQLException
    {
        List rtn = new ArrayList();
        ResultSet rs = null;
        try
        {
            // Discover tablespaces
            rs = stmt.executeQuery(TABLESPACE_QUERY);
            int ts_col = rs.findColumn("TABLESPACE_NAME");
            while (rs.next())
            {
                String tablespace = rs.getString(ts_col);
                ServiceResource service = new ServiceResource();
                service.setType(this, TABLESPACE);
                service.setServiceName(tablespace);
                service.setDescription("Tablespace on the " + instance +
                                       " database instance");
                ConfigResponse productConfig = new ConfigResponse();
                ConfigResponse metricConfig = new ConfigResponse();
                productConfig.setValue(OracleMeasurementPlugin.PROP_TABLESPACE,
                                       tablespace);
                service.setProductConfig(productConfig);
                service.setMeasurementConfig(metricConfig);
                ConfigResponse svcProps = new ConfigResponse();
                // 9i and 10g only
                if (!getTypeInfo().getVersion().equals(VERSION_8i)) {
                    svcProps.setValue("block_size",
                                      rs.getString("BLOCK_SIZE"));
                    svcProps.setValue("allocation_type",
                                      rs.getString("ALLOCATION_TYPE"));
                    svcProps.setValue("space_management",
                                      rs.getString("SEGMENT_SPACE_MANAGEMENT"));
                }
                svcProps.setValue("contents",
                                  rs.getString("CONTENTS"));
                svcProps.setValue("logging",
                                  rs.getString("LOGGING"));
                service.setCustomProperties(svcProps);
                rtn.add(service);
            }
        }
        finally {
            DBUtil.closeResultSet(log, rs);
        }
        return rtn;
    }

    private List getProcessServices(ConfigResponse config)
    {
        List rtn = new ArrayList();
        String ptql = config.getValue(PROP_PROC_PTQL);
        if (log.isDebugEnabled())
            log.debug("using ptql, "+ptql+", to retrieve processes");
        List processes = getProcesses(ptql);
        for (Iterator i=processes.iterator(); i.hasNext(); )
        {
            String process = (String)i.next();
            if (log.isDebugEnabled())
                log.debug("adding Process Metrics "+process+" service");
            ServiceResource service = new ServiceResource();
            service.setType(this, "Process Metrics");
            service.setServiceName(process+" process");
            ConfigResponse productConfig = new ConfigResponse();
            ptql = "State.Name.eq=oracle,Args.0.sw="+process;
            productConfig.setValue("process.query", ptql);
            service.setProductConfig(productConfig);
            service.setMeasurementConfig();
            rtn.add(service);
        }
        return rtn;
    }

    private List getProcesses(String ptql)
    {
        long[] pids = getPids(ptql);

        List rtn = new ArrayList();
        for (int i=0; i<pids.length; i++)
        {
            String[] args = getProcArgs(pids[i]);
            if (args.length == 0 || args[0] == null) {
                continue;
            }
            rtn.add(args[0]);
        }
        return rtn;
    }
    
    private String getTnsNamesDir(String installpath, String tnsnames) {
       if (installpath == null || tnsnames == null) {
           return "";
        }
        String fs = File.separator;
        String[] toks = tnsnames.split(Pattern.quote(fs));
        StringBuilder rtn = new StringBuilder();
        for (int i=0; i<toks.length-1; i++) {
            rtn.append(toks[i]).append(fs);
        }
        return installpath + fs + rtn.toString();
    }

    private List getTnsServices(ConfigResponse config)
    {
        String line;
        BufferedReader reader = null;
        String tnsnames = config.getValue(PROP_TNSNAMES),
               installpath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        List rtn = new ArrayList();
        try
        {
            String fs = File.separator;
            if (log.isDebugEnabled()) {
                log.debug("READING tnsnames.ora FILE: "+installpath+fs+tnsnames);
            }
            reader = new BufferedReader(new FileReader(installpath+fs+tnsnames));
            while (null != (line = reader.readLine()))
            {
                if (_serviceNameEx.matcher(line).find())
                {
                    String[] toks = line.split("=");
                    if (toks[1] == null)
                        continue;
                    String tnslistener =
                        toks[1].replaceAll("\\s*\\)", "").trim();
                    if (log.isDebugEnabled())
                        log.debug("Configuring TNS Listener "+tnslistener);
                    ServiceResource service = new ServiceResource();
                    service.setType(this, "TNS Ping");
                    service.setServiceName(tnslistener+" TNS Ping");
                    ConfigResponse productConfig = new ConfigResponse();
                    productConfig.setValue("tnslistener", tnslistener);
                    service.setProductConfig(productConfig);
                    service.setMeasurementConfig();
                    rtn.add(service);
                }
            }
        }
        catch (IOException e) {
            log.error("Error reading "+tnsnames);
        }
        finally {
            close(reader);
        }
        return rtn;
    }

    private void close(Reader reader)
    {
        if (reader == null)
            return;
        try {
            reader.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    private void populateListeningPorts(Set<Long> pids, ConfigResponse productConfig) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class);
            plp.invoke(null, pids, productConfig);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (IllegalAccessException ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        } catch (IllegalArgumentException ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        } catch (SecurityException ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        } catch (InvocationTargetException ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
    
    private boolean isValidVersion(String oracleHome){
        boolean found = false;
        File sqlPlus;
        if (isWin32()) {
            sqlPlus = new File(new File(oracleHome,"bin"), "sqlplus.exe");
        } else {
            sqlPlus = new File(new File(oracleHome,"bin"), "sqlplus");
        }

        if (sqlPlus.exists()) {
            String[] cmdarray = {sqlPlus.getAbsolutePath(), "-v"};
            String[] envp = {"ORACLE_HOME=" + oracleHome};
            Process cmd;
            try {
                cmd = Runtime.getRuntime().exec(cmdarray, envp);
                cmd.waitFor();
                int r = cmd.exitValue();
                String resultString = inputStreamAsString(cmd.getInputStream()) + inputStreamAsString(cmd.getErrorStream());
                log.debug("[isValidVersion] command '" + sqlPlus + "' result=(" + r + ")'" + resultString + "'");
                if (r == 0) {
                    Pattern reg = Pattern.compile("(\\d+\\.[\\d|\\.]+)");
                    Matcher m = reg.matcher(resultString);
                    if (m.find()) {
                        String v = m.group(1);
                        found = v.startsWith(getTypeInfo().getVersion().replace("g", "."));
                        log.debug("[isValidVersion] Version detected '" + v + "'");
                    }
                }

            } catch (IOException ex) {
                log.debug("[isValidVersion] command '" + sqlPlus + "' error:" + ex, ex);
            } catch (InterruptedException ex) {
                log.debug("[isValidVersion] command '" + sqlPlus + "' error:" + ex, ex);
            }
        } else {
            log.debug("[isValidVersion] Oracle '" + sqlPlus + "' can't be execute (permissions)");
        }
        return found;
    }
    
    /**
     * Check if the listener is up using tnsping and then read the listener
     * Host and Port for the JDBC URL.
     * @param oracle detected oracle (home and sid)
     * @return true if the listener is up.
    */
    private boolean readListenerInfo(OracleInfo oracle) {
        boolean ok = false;
        
        File tnsPing;
        if (isWin32()) {
            tnsPing = new File(new File(oracle.home,"bin"), "tnsping.exe");
        } else {
            tnsPing = new File(new File(oracle.home,"bin"), "tnsping");
        }

        if (tnsPing.exists()) {
            String[] cmdarray = {tnsPing.getAbsolutePath(), oracle.sid};
            String[] envp = {"ORACLE_HOME=" + oracle.home};
            Process cmd;
            try {
                cmd = Runtime.getRuntime().exec(cmdarray, envp);
                cmd.waitFor();
                int r = cmd.exitValue();
                String resultString = inputStreamAsString(cmd.getInputStream()) + inputStreamAsString(cmd.getErrorStream());
                log.debug("[tnsPing] command '" + tnsPing + "' result=(" + r + ")'" + resultString + "'");
                if (r == 0) {
                    ok = true;
                    Pattern reg = Pattern.compile("\\(HOST = ([^\\)]*)\\)");
                    Matcher m = reg.matcher(resultString);
                    if (m.find()) {
                        oracle.host = m.group(1);
                        log.debug("[tnsPing] Host detected '" + oracle.host + "'");
                    } else {
                        log.debug("[tnsPing] Host not found, using '" + oracle.host + "'");
                    }
                    
                    reg = Pattern.compile("\\(PORT = ([^\\)]*)\\)");
                    m = reg.matcher(resultString);
                    if (m.find()) {
                        oracle.port = m.group(1);
                        log.debug("[tnsPing] Port detected '" + oracle.port + "'");
                    } else {
                        log.debug("[tnsPing] Port not found, using '" + oracle.port + "'");
                    }
                }
            } catch (IOException ex) {
                log.debug("[tnsPing] command '" + tnsPing + "' error:" + ex, ex);
            } catch (InterruptedException ex) {
                log.debug("[tnsPing] command '" + tnsPing + "' error:" + ex, ex);
            }

        } else {
            log.debug("[tnsPing] Oracle '" + tnsPing + "' can't be execute (permissions)");
        }
        return ok;
    }
    
    static final String inputStreamAsString(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            br.close();
        }
        return sb.toString().trim();
    }

    private static final class OracleInfo {

        protected String home, sid;
        protected String host = "localhost";
        protected String port = "1521";
        

        public OracleInfo(String home, String sid) {
            this.home = home;
            this.sid = sid;
        }

        @Override
        public String toString() {
            return "OracleInfo{" + "home=" + home + ", sid=" + sid + ", host=" + host + ", port=" + port + '}';
        }
    }
}
