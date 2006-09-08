/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.plugin.jboss;

import java.io.File;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;

public class JBossProductPlugin
    extends ProductPlugin {

    private static boolean ignoreHashCodes = false;

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        ignoreHashCodes =
            "true".equals(manager.getProperty("jboss.ignoreHashCodes"));
    }

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

    public static boolean ignoreHashCodes() {
        return ignoreHashCodes;
    }

    public String[] getClassPath(ProductPluginManager manager) {
        String prop = getName() + "." + ProductPlugin.PROP_INSTALLPATH;
        String sysval = System.getProperty(prop);
        String installDir = manager.getProperties().getProperty(prop, sysval);
        String from;
        
        //we check the process table to get the installpath
        //of a running JBoss
        if (installDir == null) {
            installDir = JBossDetector.getRunningInstallPath(this);
            from = "running process";
        }
        else {
            from = "properties";
        }
        
        if (installDir != null) {
            getLog().debug("Setting " + PROP_INSTALLPATH + "=" +
                           installDir + ", configured from " + from);
        }

        String[] classpath = super.getClassPath(manager);

        for (int i=0; i<classpath.length; i++) {
            classpath[i] = installDir + "/" + classpath[i];
        }

        return classpath;
    }

    protected static boolean isBrandedServer(File installpath, String earDir) {
        String ear = "deploy" + File.separator + earDir;
        return new File(installpath, ear).exists();
    }
}
