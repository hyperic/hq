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
