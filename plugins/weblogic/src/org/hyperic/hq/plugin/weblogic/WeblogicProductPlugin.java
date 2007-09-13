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

package org.hyperic.hq.plugin.weblogic;

import java.io.IOException;
import java.io.File;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;

public class WeblogicProductPlugin extends ProductPlugin {

    private static boolean useJAAS = true;
    private static boolean autoRT  = false;

    public static final String NAME = "weblogic";

    public static final String SERVER_NAME    = "Weblogic";

    public static final String ADMIN_NAME     = "Weblogic Admin";
    public static final String NODEMGR_NAME   = "Weblogic NodeManager";

    public static final String VERSION_61     = "6.1";

    public static final String APP_NAME       = "Application";
    public static final String EXQ_NAME       = "Execute Queue";
    public static final String JDBC_CONN_NAME = "JDBC Connection Pool";
    public static final String JMS_SRV_NAME   = "JMS Server";
    public static final String JMS_DEST_NAME  = "JMS Destination";
    public static final String JTA_RES_NAME   = "JTA Resource";
    public static final String WEBAPP_NAME    = "Webapp";
    public static final String ENTITY_EJB_NAME    = "Entity EJB";
    public static final String MDB_EJB_NAME       = "Message Driven EJB";
    public static final String STATELESS_EJB_NAME = "Stateless EJB";
    public static final String STATEFUL_EJB_NAME  = "Stateful EJB";
    
    public static final String PROP_INSTALLPATH =
        "weblogic." + ProductPlugin.PROP_INSTALLPATH;

    private static Log log = LogFactory.getLog("WeblogicProductPlugin");

    public static boolean useJAAS() {
        return useJAAS;
    }

    public static boolean autoRT() {
        return autoRT;
    }

    public String[] getClassPath(ProductPluginManager manager) {
        Properties props = manager.getProperties();
        String installpath = props.getProperty(PROP_INSTALLPATH);

        //jaas is the default, provide a way to use jndi instead
        //just in case.
        String auth =
            props.getProperty("weblogic.auth.method", "jaas").toLowerCase();

        //can't do jaas w/o login config
        final String loginConfig =
            "java.security.auth.login.config";

        if (System.getProperty(loginConfig) == null) { //cmdline PluginDumper
            String pdk =
                System.getProperty(ProductPluginManager.PROP_PDK_DIR);
            String config;
            String configFile = "jaas.config";
            if (pdk != null) {
                config = pdk + "/../" + configFile;
            }
            else {
                config = configFile;
            }
            if (new File(config).exists()) {
                log.debug("-D" + loginConfig + "=" + config);
                System.setProperty(loginConfig, config);
            }
        }

        if ("jndi".equals(auth) ||
            System.getProperty(loginConfig) == null)
        {
            useJAAS = false;
        }
        else if (! "jaas".equals(auth)) {
            String msg = "Unsupported authentication method: " + auth;
            throw new IllegalArgumentException(msg);
        }

        //e.g. DB has SP2 and SP4 instances managed by 1 agent,
        //weblogic.servlet.internal.dd.ContainerDescriptor
        //serial UID is incompatible
        //XXX not working w/ 9.1, not sure why yet.
        //disabled by default for now.
        if ("true".equals(props.getProperty("weblogic.autort"))) {
            autoRT = true;
        }

        //XXX tmp ssl support solution
        for (Iterator it=props.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            if (!key.startsWith("weblogic.security.")) {
                continue;
            }

            System.setProperty(key, (String)entry.getValue());
        }

        //the jars relative to installpath
        String[] classpath = super.getClassPath(manager);

        if (installpath == null) {
            File path = null;

            //try the process table first
            if (path == null) {
                String dir = 
                    WeblogicDetector.getRunningInstallPath();

                if (dir != null) {
                    path = new File(dir);
                }
            }

            //fallback to registry on win32 only
            if ((path == null) && isWin32()) {
                path = WeblogicFinder.getServiceInstallPath();
            }

            if (path == null) {
                return classpath; //can be adjusted later
            }
            else {
                installpath = path.getAbsolutePath();
                log.info(PROP_INSTALLPATH +
                         " not set, defaulting to: " +
                         installpath);
            }
        }

        //XXX ugh.  required for ssl to find license file.
        //weblogic tries to figure this stuff out based on classpath
        //which does not work with our plugin classloader.
        String wlHome =
            props.getProperty("weblogic.home",
                              new File(installpath, "server").toString());
        System.setProperty("weblogic.home", wlHome);

        String beaHome =
            props.getProperty("bea.home",
                              new File(installpath).getParentFile().toString());

        System.setProperty("bea.home", beaHome);

        File weblogicJar = new File(installpath, "lib/weblogic.jar");

        try {
            if (weblogicJar.getCanonicalFile().exists()) {
                //configured to use 6.1, jaas is not supported.
                useJAAS = false;
            }
        } catch (IOException e) { }

        for (int i=0; i<classpath.length; i++) {
            classpath[i] = installpath + "/" + classpath[i];
        }

        return classpath;
    }
}
