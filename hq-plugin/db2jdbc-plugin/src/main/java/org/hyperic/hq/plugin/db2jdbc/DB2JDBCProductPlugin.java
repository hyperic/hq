/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;

public class DB2JDBCProductPlugin extends ProductPlugin {

    public static String DB2LS;
    public static List ENTRY_TYPES;
    public static String LIST_DATABASE;
    public static String INSTALL_PATH;
    public static String CLASSPATH;
    private static final String PROP_DB2LS = "db2.jdbc.db2ls";
    private static final String PROP_ENTRY_TYPES = "db2.jdbc.entry_types";
    private static final String PROP_LIST_DATABASE = "db2.jdbc.list_database";
    private static final String PROP_INSTALL_PATH = "db2.jdbc.installpath";
    private static final String PROP_CLASSPATH = "db2.jdbc.classpath";
    private final static Log log = LogFactory.getLog(DB2JDBCProductPlugin.class);

    public String[] getClassPath(ProductPluginManager manager) {
        String[] res = super.getClassPath(manager);

        DB2LS = manager.getProperties().getProperty(PROP_DB2LS, "db2ls");

        String et = manager.getProperties().getProperty(PROP_ENTRY_TYPES, "*");
        ENTRY_TYPES = Arrays.asList(et.toLowerCase().split(","));

        LIST_DATABASE = manager.getProperties().getProperty(PROP_LIST_DATABASE);
        INSTALL_PATH = manager.getProperties().getProperty(PROP_INSTALL_PATH);
        CLASSPATH = manager.getProperties().getProperty(PROP_CLASSPATH);

        if (log.isDebugEnabled()) {
            log.debug("[getServerResources] " + PROP_DB2LS + "=" + DB2LS + ", "
                    + PROP_ENTRY_TYPES + "=" + ENTRY_TYPES + ", "
                    + PROP_LIST_DATABASE + "=" + LIST_DATABASE + ", "
                    + PROP_INSTALL_PATH + "=" + INSTALL_PATH + ", "
                    + PROP_CLASSPATH + "=" + CLASSPATH);
        }

        if (CLASSPATH != null) {
            res = CLASSPATH.split(",");
        } else {
            List paths = new ArrayList();
            if (INSTALL_PATH != null) {
                paths.add(INSTALL_PATH);
            } else {
                paths.addAll(DefaultServerDetector.getInstallPaths(manager.getProperties(), ".*"));
            }

            if (paths.size() > 0) {
                File jar = new File((String) paths.get(0), "java/db2jcc.jar");
                if (jar.exists()) {
                    res = new String[]{jar.getAbsolutePath()};
                }
            }
        }
        return res;
    }
}
