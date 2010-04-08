/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private Pattern regExpInstall = Pattern.compile("([^ ]*) *(\\d*\\.\\d*\\.\\d*\\.\\d*) *([^ ]*)");
    private boolean ISWin = false;
    private String db2ls;
    private List entry_types;
    private String list_database;

    public List getServerResources(ConfigResponse conf) throws PluginException {
        getLog().debug("[getServerResources] conf=" + conf);
        Pattern regExpVersion = Pattern.compile(getTypeInfo().getVersion().replaceAll("[X|x]", "\\d*"));
        List res = new ArrayList();

        ISWin = "win32".equalsIgnoreCase(conf.getValue("platform.type"));
        if (ISWin) {
            try {
                ISWin = true;
                RegistryKey key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies");
                String instances[] = key.getSubKeyNames();
                key.close();
                for (int n=0;n<instances.length;n++) {
                    key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies\\" + instances[n] + "\\CurrentVersion");
                    String version = key.getStringValue("Version") + "." + key.getStringValue("Release") + "." + key.getStringValue("Modification") + "." + key.getStringValue("Fix Level");
                    key.close();
                    getLog().debug(instances[n] + "-->" + version);
                    if (regExpVersion.matcher(version).find()) {
                        key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\IBM\\DB2\\InstalledCopies\\" + instances[n]);
                        String path = key.getStringValue("DB2 Path Name");
                        key.close();
                        res.addAll(createServers(path.trim()));
                    } else {
                        getLog().debug("[getServerResources] bad version: '" + instances[n] + " " + version + "'");
                    }
                }
            } catch (Win32Exception ex) {
                Logger.getLogger(DefaultServerDetector.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                Process cmd = Runtime.getRuntime().exec(db2ls);
                cmd.waitFor();
                String sal = inputStreamAsString(cmd.getInputStream());
                getLog().debug("[getServerResources] sal=" + sal);
                String[] installs = sal.split("\n");
                for (int n=0;n<installs.length;n++) {
                    Matcher m = regExpInstall.matcher(installs[n]);
                    if (m.find()) {
                        if (regExpVersion.matcher(m.group(2)).find()) {
                            getLog().debug("[getServerResources] found: '" + m.group() + "'");
                            res.addAll(createServers(m.group(1)));
                        } else {
                            getLog().debug("[getServerResources] bad version: '" + m.group() + "'");
                        }
                    }
                }
            } catch (Exception ex) {
                if (getLog().isDebugEnabled()) {
                    getLog().error(ex.getMessage(), ex);
                }
            }
        }
        return res;
    }

    public void init(PluginManager manager) throws PluginException {
        db2ls = manager.getProperties().getProperty("db2.jdbc.db2ls", "db2ls");

        String et = manager.getProperties().getProperty("db2.jdbc.entry_types", "*");
        entry_types = Arrays.asList(et.toLowerCase().split(","));

        list_database = manager.getProperties().getProperty("db2.jdbc.list_database");

        getLog().debug("[getServerResources] db2.jdbc.db2ls=" + db2ls);
        getLog().debug("[getServerResources] db2.jdbc.entry_types=" + entry_types);
        getLog().debug("[getServerResources] db2.jdbc.list_database=" + list_database);

        super.init(manager);
    }

    protected boolean checkEntryTypes(String type) {
        boolean res = entry_types.contains("*") || entry_types.contains(type.toLowerCase());
        getLog().debug("[checkEntryTypes] type='" + type + "' res='" + res + "'");
        return res;
    }

    protected String getListDatabaseCommand(){
        return list_database;
    }

    protected abstract List createServers(String installPath);

    protected final boolean isWin() {
        return ISWin;
    }

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

    static final Connection getConnection(ConfigResponse props) throws SQLException, ClassNotFoundException {
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        String url = "jdbc:db2://" + props.getValue("db2.jdbc.hostname") + ":"
                + props.getValue("db2.jdbc.port") + "/"
                + props.getValue("db2.jdbc.database");
        String user = props.getValue("db2.jdbc.user");
        String pass = props.getValue("db2.jdbc.password");
        return DriverManager.getConnection(url, user, pass);
    }
}
