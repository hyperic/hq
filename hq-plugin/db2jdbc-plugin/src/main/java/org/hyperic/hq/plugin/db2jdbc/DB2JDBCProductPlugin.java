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
            log.debug("[getClassPath] " + PROP_DB2LS + "=" + DB2LS + ", "
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
                } else {
                    if (log.isWarnEnabled()){
                       log.warn("Unable to locate DB2 jar file: " + jar.getAbsolutePath()); 
                    }
                }
            }
        }
        return res;
    }
}
