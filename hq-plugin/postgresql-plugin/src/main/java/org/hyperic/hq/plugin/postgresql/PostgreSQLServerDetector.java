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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.DBUtil;

public class PostgreSQLServerDetector extends ServerDetector implements AutoServerDetector {

    private Log log = LogFactory.getLog(PostgreSQLServerDetector.class);
    private static final String PTQL_QUERY = "State.Name.re=post(master|gres),State.Name.Pne=$1,Args.0.re=.*post(master|gres)(.exe)?$";
    protected static final String DB_QUERY = "SELECT datname FROM pg_database WHERE datistemplate IS FALSE AND datallowconn IS TRUE";
    private static final String TABLE_QUERY = "SELECT relname, schemaname FROM pg_stat_user_tables";
    private static final String INDEX_QUERY = "SELECT indexrelname, schemaname FROM pg_stat_user_indexes";

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);
        log.debug("[getServerProcessList] pids.length=" + pids.length);

        for (int i = 0; i < pids.length; i++) {
            String exe = getProcExe(pids[i]);
            List args = Arrays.asList(getProcArgs(pids[i]));
            log.debug("[getServerProcessList] pid=" + pids[i] + " exec=" + exe + " args=" + args);
            if (exe != null) {
                String version = getVersion(exe);
                String expectedVersion = getTypeProperty("version");
                String pgData = getArgument("-D", args);
                try {
                    pgData = new File(pgData).getCanonicalPath();
                } catch (IOException ex) {
                    log.debug(ex, ex);
                }
                boolean correctVersion = Pattern.compile(expectedVersion).matcher(version).find();
                log.debug("[getServerProcessList] version=" + version + " correctVersion=" + correctVersion);

                //check for vPostgres and HQ
                final boolean isHQ = exe.indexOf("hqdb") != -1;
                if (correctVersion) {
                    File vflicense = new File(new File(exe).getParent(), "vflicense");
                    if (getTypeInfo().getName().startsWith("vPostgres")) {
                        correctVersion = vflicense.exists();
                    } else if (getTypeInfo().getName().startsWith("HQ")) {
                        correctVersion = isHQ;
                    } else {
                        correctVersion = !(vflicense.exists() || isHQ);
                    }
                }

                if (correctVersion) {
                	ServerResource server = createServerResource(exe);
                    server.setMeasurementConfig();
                    if (!isHQ) {
                        server.setControlConfig();
                    }
                	server.setIdentifier(server.getIdentifier() + "$" + pgData);
                	ConfigResponse cprop = new ConfigResponse();
                	cprop.setValue("version", version);
                	setCustomProperties(server, cprop);
                        ConfigResponse productConfig = prepareConfig(pgData, args);
                        populateListeningPorts(pids[i], productConfig, true);
                	setProductConfig(server, productConfig);                   
                	String basename = getPlatformName() + " " + getTypeInfo().getName();
                	server.setName(prepareName(basename + " " + (isHQ ? PostgreSQL.HQ_SERVER_NAME : PostgreSQL.SERVER_NAME), server.getProductConfig(), null));
                	servers.add(server);
                } else {
                	log.debug("[getServerProcessList] pid='" + pids[i] + "' Is not a '" + getTypeInfo().getName() + "'");
                }
            }
        }
        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        log.debug("[discoverServices] config=" + serverConfig);
        boolean isHQ = getTypeInfo().getName().startsWith("HQ");
        ArrayList services = new ArrayList();
        String user = serverConfig.getValue(PostgreSQL.PROP_USER);
        String pass = serverConfig.getValue(PostgreSQL.PROP_PASS);
        String url = PostgreSQL.prepareUrl(serverConfig.toProperties(), null);

        try {
            Class.forName(ResourceMeasurement.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Unable to load JDBC Driver: " + e.getMessage());
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        List<String> dataBases = new ArrayList<String>();
        try {
            conn = DriverManager.getConnection(url, user, pass);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(DB_QUERY);
            while (rs != null && rs.next()) {
                dataBases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new PluginException("Error querying for DataBases: " + e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
        }

        Pattern table_reg = null;
        boolean table_all = serverConfig.getValue(PostgreSQL.PROP_TABLE_REG).equalsIgnoreCase("ALL");
        boolean table_off = serverConfig.getValue(PostgreSQL.PROP_TABLE_REG).equalsIgnoreCase("OFF");
        if (!table_all && !table_off) {
            table_reg = Pattern.compile(serverConfig.getValue(PostgreSQL.PROP_TABLE_REG));
        }

        Pattern index_reg = null;
        boolean index_all = serverConfig.getValue(PostgreSQL.PROP_INDEX_REG).equalsIgnoreCase("ALL");
        boolean index_off = serverConfig.getValue(PostgreSQL.PROP_INDEX_REG).equalsIgnoreCase("OFF");
        if (!index_all && !index_off) {
            index_reg = Pattern.compile(serverConfig.getValue(PostgreSQL.PROP_INDEX_REG));
        }

        log.debug("[discoverServices] databases: " + dataBases);
        for (int i = 0; i < dataBases.size(); i++) {
            String dataBase = dataBases.get(i);
            ServiceResource db = new ServiceResource();
            db.setType(this, "DataBase");

            ConfigResponse dbPC = new ConfigResponse();
            dbPC.setValue(PostgreSQL.PROP_DB, dataBase);

            db.setProductConfig(dbPC);
            db.setMeasurementConfig();
            db.setControlConfig();

            db.setServiceName(prepareName(isHQ ? PostgreSQL.HQ_DB_NAME : PostgreSQL.DB_NAME, serverConfig, dbPC));

            services.add(db);

            try {
                conn = DriverManager.getConnection(PostgreSQL.prepareUrl(serverConfig.toProperties(), dataBase), user, pass);
                stmt = conn.createStatement();
                rs = stmt.executeQuery(TABLE_QUERY);

                while (rs != null && rs.next()) {
                    String tablename = rs.getString(1);
                    String schemaname = rs.getString(2);
                    if (isValidName(tablename, table_all, table_off, table_reg)) {
                        ServiceResource service = new ServiceResource();
                        service.setType(this, "Table");

                        ConfigResponse tablePC = new ConfigResponse();
                        tablePC.setValue(PostgreSQL.PROP_DB, dataBase);
                        tablePC.setValue(PostgreSQL.PROP_TABLE, tablename);
                        tablePC.setValue(PostgreSQL.PROP_SCHEMA, schemaname);

                        service.setProductConfig(tablePC);
                        service.setMeasurementConfig();
                        service.setControlConfig();

                        service.setServiceName(prepareName(isHQ ? PostgreSQL.HQ_TABLE_NAME : PostgreSQL.TABLE_NAME, serverConfig, tablePC));

                        services.add(service);
                    }
                }
                DBUtil.closeJDBCObjects(this.log, null, stmt, rs);

                stmt = conn.createStatement();
                rs = stmt.executeQuery(INDEX_QUERY);
                while (rs != null && rs.next()) {
                    String indexname = rs.getString(1);
                    String schemaname = rs.getString(2);

                    if (isValidName(indexname, index_all, index_off, index_reg)) {
                        ServiceResource service = new ServiceResource();
                        service.setType(this, "Index");

                        ConfigResponse indexPC = new ConfigResponse();
                        indexPC.setValue(PostgreSQL.PROP_DB, dataBase);
                        indexPC.setValue(PostgreSQL.PROP_INDEX, indexname);
                        indexPC.setValue(PostgreSQL.PROP_SCHEMA, schemaname);

                        service.setProductConfig(indexPC);
                        service.setMeasurementConfig();
                        service.setControlConfig();

                        service.setServiceName(prepareName(isHQ ? PostgreSQL.INDEX_NAME : PostgreSQL.INDEX_NAME, serverConfig, indexPC));

                        services.add(service);
                    }
                }
            } catch (SQLException e) {
                throw new PluginException("Error querying for services: " + e.getMessage(), e);
            } finally {
                DBUtil.closeJDBCObjects(this.log, conn, stmt, rs);
            }
        }
        return services;
    }

    private String prepareName(String pattern, ConfigResponse serverConf, ConfigResponse serviceConf) {
        List<ConfigResponse> props = new ArrayList<ConfigResponse>();
        String res = pattern;
        props.add(serverConf);
        if (serviceConf != null) {
            props.add(serviceConf);
        }

        for (int i = 0; i < props.size(); i++) {
            ConfigResponse cfg = props.get(i);
            for (Iterator<String> it = cfg.getKeys().iterator(); it.hasNext();) {
                String key = it.next();
                String val = cfg.getValue(key);
                if (val == null) {
                    val = "";
                }
                res = res.replace("${" + key + "}", val);
            }
        }
        return res.trim();
    }

    private String getVersion(String exec) {
        String command[] = {exec, "--version"};
        log.debug("[getVersionString] command= '" + Arrays.asList(command) + "'");
        String version = "";
        try {
            Process cmd = Runtime.getRuntime().exec(command);
            cmd.getOutputStream().close();
            cmd.waitFor();
            String out = inputStreamAsString(cmd.getInputStream());
            String err = inputStreamAsString(cmd.getErrorStream());
            if (log.isDebugEnabled()) {
                if (cmd.exitValue() != 0) {
                    log.error("[getVersionString] exit=" + cmd.exitValue());
                    log.error("[getVersionString] out=" + out);
                    log.error("[getVersionString] err=" + err);
                } else {
                    log.debug("[getVersionString] out=" + out);
                }
            }
            version = out;
        } catch (InterruptedException ex) {
            log.debug("[getVersionString] Error:" + ex.getMessage(), ex);
        } catch (IOException ex) {
            log.debug("[getVersionString] Error:" + ex.getMessage(), ex);
        }
        return version;
    }

    private String inputStreamAsString(InputStream stream) throws IOException {
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

    private String getArgument(String opt, List<String> args) {
        String res = "";
        int i = args.indexOf(opt);
        if (i > -1) {
            res = args.get(i + 1);
        }
        return res.trim();
    }

    private String getConfiguration(String regex, String config) {
        String res = "";
        Matcher m = Pattern.compile(regex, Pattern.MULTILINE + Pattern.CASE_INSENSITIVE).matcher(config);
        if (m.find()) {
            res = m.group(1).trim();
            res = res.replaceAll("=", "");
            res = res.replaceAll("'", "");
            res = res.replaceAll("\"", "");
        }
        return res.trim();
    }

    private String loadConfiguration(String pgData) {
        String configuration = "";
        File configFile = new File(pgData, "postgresql.conf");
        if (configFile.exists() && configFile.canRead()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(configFile);
                configuration = inputStreamAsString(in);
            } catch (IOException ex) {
                log.error("Error reading file '" + configFile + "': " + ex.getMessage(), ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.error("Error closing file '" + configFile + "'");
                    }
                }
            }
        }
        return configuration;
    }

    private ConfigResponse prepareConfig(String pgData, List<String> args) {
        ConfigResponse cf = new ConfigResponse();
        cf.setValue(PostgreSQL.PROP_DATA, pgData);
        String configuration = loadConfiguration(pgData);

        String addr = getConfiguration("^ *listen_addresses([^#]*)", configuration);
        if (addr.length() > 0) {
            if (addr.equals("*")) {
                addr = "localhost";
            }
            cf.setValue(PostgreSQL.PROP_HOST, addr);
        } else {
            addr = getArgument("-h", args);
            if (addr.length() > 0) {
                cf.setValue(PostgreSQL.PROP_HOST, addr);
            }
        }

        String port = getConfiguration("^ *port([^#]*)", configuration);
        if (port.length() > 0) {
            cf.setValue(PostgreSQL.PROP_PORT, port);
        } else {
            port = getArgument("-p", args);
            if (port.length() > 0) {
                cf.setValue(PostgreSQL.PROP_PORT, port);
            }
        }
        return cf;
    }

    private boolean isValidName(String name, boolean all, boolean off, Pattern reg) {
        boolean res;
        if (all) {
            res = true;
        } else if (off) {
            res = false;
        } else {
            res = reg.matcher(name).matches();
        }
        return res;
    }
    
        private void populateListeningPorts(long pid, ConfigResponse productConfig, boolean b) {
        try {
            Class du = Class.forName("org.hyperic.hq.product.DetectionUtil");
            Method plp = du.getMethod("populateListeningPorts", long.class, ConfigResponse.class, boolean.class);
            plp.invoke(null, pid, productConfig, b);
        } catch (ClassNotFoundException ex) {
            log.debug("[populateListeningPorts] Class 'DetectionUtil' not found", ex);
        } catch (NoSuchMethodException ex) {
            log.debug("[populateListeningPorts] Method 'populateListeningPorts' not found", ex);
        } catch (Exception ex) {
            log.debug("[populateListeningPorts] Problem with Method 'populateListeningPorts'", ex);
        }
    }
}
