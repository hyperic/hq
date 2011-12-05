/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
package org.hyperic.hq.plugin.db2jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public abstract class DefaultServerDetector extends ServerDetector implements AutoServerDetector {

    private final static Pattern regExpInstall = Pattern.compile("([^ ]*) *(\\d*\\.\\d*\\.\\d*\\.\\d*) *([^ ]*)");
    private final static Log log = LogFactory.getLog(DefaultServerDetector.class);

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
    }

    public List getServerResources(ConfigResponse conf) throws PluginException {
        boolean debug = log.isDebugEnabled();

        if (debug) {
            log.debug("[getServerResources] conf=" + conf);
        }

        Iterator paths = getInstallPaths(conf.toProperties(), getTypeInfo().getVersion()).iterator();
        List res = new ArrayList();
        while (paths.hasNext()) {
            String path = (String) paths.next();
            res.addAll(createServers(path));
        }
        return res;
    }

    public static List getInstallPaths(Properties conf, String versionExp) {
        List res = new ArrayList();
        Pattern regExpVersion = Pattern.compile(versionExp.replaceAll("[X|x]", "\\d*"));
        boolean debug = log.isDebugEnabled();
        if (isWin32()) {
            try {
                RegistryKey key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies");
                String instances[] = key.getSubKeyNames();
                key.close();
                for (int n = 0; n < instances.length; n++) {
                    key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies\\" + instances[n] + "\\CurrentVersion");
                    String version = key.getStringValue("Version") + "." + key.getStringValue("Release") + "." + key.getStringValue("Modification") + "." + key.getStringValue("Fix Level");
                    key.close();
                    if (debug) {
                        log.debug(instances[n] + "-->" + version);
                    }
                    if (regExpVersion.matcher(version).find()) {
                        key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies\\" + instances[n]);
                        String path = key.getStringValue("DB2 Path Name");
                        key.close();
                        res.add(path.trim());
                    } else {
                        if (debug) {
                            log.debug("[getInstallPaths] bad version: '" + instances[n] + " " + version + "'");
                        }
                    }
                }
            } catch (Win32Exception ex) {
                if (debug) {
                    log.debug("[getInstallPaths] error: " + ex.getMessage(), ex);
                }
            }
        } else {
            try {
                Process cmd = Runtime.getRuntime().exec(DB2JDBCProductPlugin.DB2LS);
                cmd.waitFor();
                String resultString = inputStreamAsString(cmd.getInputStream());
                if (debug) {
                    log.debug("[getInstallPaths] command result=" + resultString);
                }
                String[] installs = resultString.split("\n");
                for (int n = 0; n < installs.length; n++) {
                    Matcher m = regExpInstall.matcher(installs[n]);
                    if (m.find()) {
                        if (regExpVersion.matcher(m.group(2)).find()) {
                            if (debug) {
                                log.debug("[getInstallPaths] found: '" + m.group() + "'");
                            }
                            res.add(m.group(1));
                        } else {
                            if (debug) {
                                log.debug("[getInstallPaths] bad version: '" + m.group() + "'");
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                if (debug) {
                    log.debug("[getInstallPaths] " + ex.getMessage());
                }
            } catch (Exception e){
                if (debug) {
                    log.debug("[getInstallPaths] error: " + e.getMessage(), e);
                }
            }
        }
        return res;
    }

    protected boolean checkEntryTypes(String type) {
        boolean res = DB2JDBCProductPlugin.ENTRY_TYPES.contains("*") || DB2JDBCProductPlugin.ENTRY_TYPES.contains(type.toLowerCase());
        if (log.isDebugEnabled()) {
            log.debug("[checkEntryTypes] type='" + type + "' res='" + res + "'");
        }
        return res;
    }

    protected String getListDatabaseCommand() {
        return DB2JDBCProductPlugin.LIST_DATABASE;
    }

    protected abstract List createServers(String installPath);

    static final String inputStreamAsString(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    static final Connection getConnection(Properties props) throws SQLException, ClassNotFoundException {
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        String url = "jdbc:db2://" + props.getProperty("db2.jdbc.hostname") + ":"
                + props.getProperty("db2.jdbc.port") + "/"
                + props.getProperty("db2.jdbc.database");
        String user = props.getProperty("db2.jdbc.user");
        String pass = props.getProperty("db2.jdbc.password");
        return DriverManager.getConnection(url, user, pass);
    }
}
