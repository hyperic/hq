/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

/**
 *
 * @author administrator
 */
public class TomcatProductPlugin extends ProductPlugin {

    @Override
    public String[] getClassPath(ProductPluginManager manager) {
        String prop = "jbossweb." + ProductPlugin.PROP_INSTALLPATH;
        String sysval = System.getProperty(prop);
        String installDir = manager.getProperties().getProperty(prop, sysval);
        String from;

        //we check the process table to get the installpath
        //of a running JBoss
        if (installDir == null) {
            installDir = getRunningInstallPath();
            from = "running process";
        } else {
            from = "properties";
        }

        String[] classpath = super.getClassPath(manager);

        if (installDir == null) {
            getLog().debug(prop + " not configured");
            //may be resolved later by JBossDetector.adjustClassPath
            return classpath;
        } else {
            getLog().debug("Setting " + prop + "=" + installDir + ", configured from " + from);
        }

        for (int i = 0; i < classpath.length; i++) {
            File jar = new File(installDir, classpath[i]);
            if (jar.exists()) {
                classpath[i] = jar.getPath();
            }
        }

        return classpath;
    }

    private String getRunningInstallPath() {
        String res = null;
        try {
            Sigar sigar = new Sigar();
            long[] pids = ProcessFinder.find(sigar, "State.Name.re=java|jsvc,State.Name.Pne=jsvc,Args.*.eq=org.jboss.Main");
            if (pids.length > 0) {
                res = new File(sigar.getProcExe(pids[0]).getCwd()).getParent();
            }
        } catch (SigarException ex) {
            getLog().debug("[getRunningInstallPath] " + ex.getMessage(), ex);
            res = null;
        }
        return res;
    }
}
