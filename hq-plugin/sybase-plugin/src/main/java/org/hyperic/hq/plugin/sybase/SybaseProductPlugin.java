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
package org.hyperic.hq.plugin.sybase;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.file.FileUtil;

/**
 *
 * @author laullon
 */
public class SybaseProductPlugin extends ProductPlugin {

    private static String[] jars = {"jConnect-6_0/classes/jconn3.jar",
        "jConnect-7_0/classes/jconn4.jar"};
    private static String saRole;
    private static boolean originalAIID = true;
    private static String PROP_CLASSPATH = "sybase.classpath";
    private static String PROP_INSTALLPATH = "sybase.installpath";
    Log log = getLog();

    protected static boolean isOriginalAIID() {
        return originalAIID;
    }

    protected static String getSaRole(){
        return saRole;
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        Properties props = manager.getProperties();
        originalAIID = "true".equals(props.getProperty("sysbase.aiid.orginal", "true").toLowerCase());
        saRole = props.getProperty("sysbase.sa_role", "sa_role");
    }

    public String[] getClassPath(ProductPluginManager manager) {
        String[] res=_getClassPath(manager);
        log.debug("[getClassPath] res="+Arrays.asList(res));
        return res;
    }

    private String[] _getClassPath(ProductPluginManager manager) {
        Properties props = manager.getProperties();

        if (props.getProperty(PROP_CLASSPATH) != null) {
            return props.getProperty(PROP_CLASSPATH).split(",");
        }

        if (props.getProperty(PROP_INSTALLPATH) != null) {
            return findJDBCJar(props.getProperty(PROP_INSTALLPATH));
        }

        String[] res = null;
        try {
            Sigar sigar = new Sigar();
            long[] pids = ProcessFinder.find(sigar, SybaseServerDetector.PTQL_QUERY);
            for (int n = 0; ((n < pids.length) && (res == null)); n++) {
                String installPath = sigar.getProcArgs(pids[n])[0];
                installPath = FileUtil.getParentDir(installPath, 3);
                res = findJDBCJar(installPath);
            }
        } catch (SigarException ex) {
            ex.printStackTrace();
        }
        if (res == null) {
            res = new String[]{};
        }
        return res;
    }

    private String[] findJDBCJar(String installPath) {
        log.debug("[getClassPath] testing " + PROP_INSTALLPATH + "='" + installPath + "'");
        String[] res = null;
        File ip = new File(installPath);
        if (ip.exists()) {
            for (int i = 0; ((i < jars.length) && (res == null)); i++) {
                File jar = new File(ip, jars[i]);
                if (jar.exists() && jar.canRead()) {
                    res = new String[]{jar.getAbsolutePath()};
                }
            }
        }
        return res;
    }
}
