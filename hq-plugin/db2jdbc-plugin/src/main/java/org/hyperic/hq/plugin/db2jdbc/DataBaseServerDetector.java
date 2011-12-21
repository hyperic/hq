/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ExecutableProcess;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class DataBaseServerDetector extends DefaultServerDetector {
    private Pattern regExpDataBases = Pattern.compile("Database (\\d*) entry:");
    //private Pattern regExpDataBases = Pattern.compile("Database name[^=]*= (\\S*)[^L]*Local database directory[^=]*= (\\S*)");

    protected List discoverServices(ConfigResponse config) throws PluginException {
        getLog().debug("discoverServices config=" + config);
        List res = new ArrayList();
        String type = getTypeInfo().getName();
        //String dbName = config.getValue("db2.jdbc.database");

        String user = config.getValue("db2.jdbc.user");
        String pass = config.getValue("db2.jdbc.password");
        if (user == null || pass == null) {
            return res;
        }

        /**
         * Table Space
         */
        String schema = user;
        Iterator tbl = getList(config, "SELECT TABNAME FROM SYSIBMADM.ADMINTABINFO WHERE TABSCHEMA='" + schema + "'").iterator();
        while (tbl.hasNext()) {
            String tbName = (String) tbl.next();
            if (!tbName.toUpperCase().startsWith("SYS")) {
                ServiceResource tb = new ServiceResource();
                tb.setType(type + " Table");
                tb.setServiceName("Table " + schema + "." + tbName);

                ConfigResponse conf = new ConfigResponse();
                conf.setValue("table", tbName);
                conf.setValue("schema", schema);
                setProductConfig(tb, conf);
                tb.setMeasurementConfig();
                tb.setResponseTimeConfig(new ConfigResponse());
                tb.setControlConfig();

                res.add(tb);
            }
        }

        /**
         * Table Space
         */
        Iterator tbspl = getList(config, "SELECT TBSP_NAME FROM SYSIBMADM.TBSP_UTILIZATION where TBSP_TYPE='DMS'").iterator();
        while (tbspl.hasNext()) {
            String tbspName =(String) tbspl.next();
            ServiceResource bpS = new ServiceResource();
            bpS.setType(type + " Table Space");
            bpS.setServiceName("Table Space " + tbspName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("tablespace", tbspName);
            setProductConfig(bpS, conf);
            bpS.setMeasurementConfig();
            bpS.setResponseTimeConfig(new ConfigResponse());
            bpS.setControlConfig();

            res.add(bpS);
        }

        /**
         * Buffer Pool
         */
        Iterator bpl = getList(config, "SELECT BP_NAME FROM SYSIBMADM.BP_HITRATIO").iterator();
        while (bpl.hasNext()) {
            String bpName = (String) bpl.next();
            ServiceResource bpS = new ServiceResource();
            bpS.setType(type + " Buffer Pool");
            bpS.setServiceName("Buffer Pool " + bpName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("bufferpool", bpName);
            setProductConfig(bpS, conf);
            bpS.setMeasurementConfig();
            bpS.setResponseTimeConfig(new ConfigResponse());
            bpS.setControlConfig();

            res.add(bpS);
        }

        /**
         * Mempory Pool
         */
        Iterator mpl = getList(config, "SELECT concat(concat(POOL_ID, '|'), COALESCE(POOL_SECONDARY_ID,'')) as name FROM SYSIBMADM.SNAPDB_MEMORY_POOL where POOL_SECONDARY_ID is NULL or POOL_ID='BP'").iterator();
        while (mpl.hasNext()) {
            String mpN = (String) mpl.next();
            String[] names = mpN.split("\\|");
            String mpId = names[0].trim();
            String mpSId = (names.length == 2) ? names[1].trim() : "";
            String mpName = (mpId + " " + mpSId).trim();

            ServiceResource mpS = new ServiceResource();
            mpS.setType(type + " Memory Pool");
            mpS.setServiceName("Memory Pool " + mpName);

            ConfigResponse conf = new ConfigResponse();
            conf.setValue("pool_id", mpId);
            conf.setValue("sec_pool_id", mpSId);
            setProductConfig(mpS, conf);
            mpS.setMeasurementConfig();
            mpS.setResponseTimeConfig(new ConfigResponse());
            mpS.setControlConfig();

            res.add(mpS);
        }

        return res;
    }

    protected List createServers(String installPath) {
        List res = new ArrayList();
        
            String command = getListDatabaseCommand();
            File db2exe = null;
            // http://www.db2ude.com/?q=node/121 for wind db2.exe
            if (command == null) {
                db2exe = new File(installPath + (installPath.endsWith(File.separator) ? "" : File.separator) + "bin" + File.separator + "db2" + (isWin32() ? "cmd.exe" : ""));
                command = db2exe.getAbsolutePath() + (isWin32() ? " /c /i /w db2 " : " ") + "list database directory";
            }
        if (db2exe.isFile()) {
            try {
                getLog().debug("[createDataBases] command= '" + command + "'");
                Process cmd = Runtime.getRuntime().exec(command);
                String sal = inputStreamAsString(cmd.getInputStream());
                String err = inputStreamAsString(cmd.getErrorStream());
                cmd.waitFor();

                if (getLog().isDebugEnabled()) {
                    if (cmd.exitValue() != 0) {
                        getLog().error("[createDataBases] exit=" + cmd.exitValue());
                        getLog().error("[createDataBases] sal=" + sal);
                    } else {
                        getLog().debug("[createDataBases] sal=" + sal);
                    }
                    if (sal.length() == 0) {
                        getLog().debug("[createDataBases] (" + cmd.exitValue() + ") err=" + err);
                    }
                }

                Matcher m = regExpDataBases.matcher(sal);
                int ini = 0, end = 0;
                if (m.find()) {
                    ini = m.start();
                    do {
                        if (m.find()) {
                            end = m.end();
                        } else {
                            end = sal.length();
                        }

                        String db = sal.substring(ini, end).trim();
                        Properties db_props = parseProperties(db);
                        getLog().debug("db_props --> " + db_props);
                        ServerResource svr = createDataBase(db_props);
                        if (svr != null) {
                            res.add(svr);
                        }
                        ini = end;
                    } while (end != sal.length());
                }
            } catch (Exception ex) {
                getLog().error(ex.getMessage(), ex);
            }
        } else {
            getLog().debug("DB2 executable was not found: " + db2exe);
        }
        return res;
    }

    private Properties parseProperties(String s) {
        Properties props = new Properties();
        String[] lines = s.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line[] = lines[i].split("=");
            if (line.length > 1) {
                props.setProperty(line[0].trim(), line[1].trim());
            }
        }
        return props;
    }

    ServerResource createDataBase(Properties props) {
        if (!checkEntryTypes(props.getProperty("Directory entry type"))) {
            return null;
        }

        String name = props.getProperty("Database name");
        String iPath = props.getProperty("Local database directory");
        if (iPath == null) {
            iPath = props.getProperty("Directory entry type");
        }
        getLog().debug("[createDataBase] name='" + name + "' [" + iPath + "]");

        ServerResource res = new ServerResource();
        res.setType(getTypeInfo().getName());
        //res.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + name);
        res.setName(getPlatformName() + " DB2 " + name);
        res.setInstallPath(iPath);
        res.setIdentifier(res.getName());

        ConfigResponse conf = new ConfigResponse();
        conf.setValue("db2.jdbc.database", name);
        conf.setValue("db2.jdbc.version", getTypeInfo().getVersion());
        setProductConfig(res, conf);
        return res;
    }

    List getList(ConfigResponse props, String query) {
        List res = new ArrayList();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            getLog().debug("getList props=" + props);
            conn = getConnection(props.toProperties());
            ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(rs.getString(1));
            }
            getLog().debug("getList '" + query + "' => res= " + res);
        } catch (Exception ex) {
            getLog().debug("getList '" + query + "' => " + ex.getMessage(),ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                getLog().error("ERROR", ex);
            }
        }
        return res;
    }
}
