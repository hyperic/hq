/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    protected void adjustClassPath(String installpath) {
        //super.init will call this if jboss.installpath is configured
        File servers = new File(installpath, "server");
        if (!servers.exists()) {
            return;
        }

        File[] dirs = servers.listFiles();
        if (dirs == null) {
            return;
        }

        for (int i=0; i<dirs.length; i++) {
            File dir = dirs[i];
            String name = dir.getName();
            //skip server/ dirs already listed in hq-plugin.xml
            if (name.equals("all") ||
                name.equals("default") ||
                name.equals("minimal"))
            {
                continue;
            }

            super.adjustClassPath(dir.toString());
        }
    }

    @Override
    public String[] getClassPath(ProductPluginManager manager) {
        String prop = getName() + "." + ProductPlugin.PROP_INSTALLPATH;
        String sysval = System.getProperty(prop);
        String installDir = manager.getProperties().getProperty(prop, sysval);
        String from;

        //we check the process table to get the installpath
        //of a running JBoss
        if (installDir == null) {
            installDir = getRunningInstallPath();
            from = "running process";
        }
        else {
            from = "properties";
        }

        String[] classpath = super.getClassPath(manager);

        if (installDir == null) {
            getLog().debug(prop + " not configured");
            //may be resolved later by JBossDetector.adjustClassPath
            return classpath;
        }
        else {
            getLog().debug("Setting " + prop + "=" +
                           installDir + ", configured from " + from);
        }

        for (int i=0; i<classpath.length; i++) {
            File jar = new File(installDir, classpath[i]);
            if (jar.exists()) {
                classpath[i] = jar.getPath();
            }
        }

        return classpath;
    }

    private String getRunningInstallPath() {
        String res="/Users/administrator/jboss/jboss-4.2.3.GA";
        try {
            ProcessFinder.find(new Sigar(), "State.Name.re=java|jsvc,State.Name.Pne=jsvc,Args.*.eq=org.jboss.Main");
        } catch (SigarException ex) {
            Logger.getLogger(TomcatProductPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
}
