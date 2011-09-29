/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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
package org.hyperic.hq.plugin.activemq;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxQuery;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * Server detector for activemq brokers embedded in Tomcat/tc Server instances.
 * Exists to optimize recursive file scanning and to set unique identifiers for
 * any embedded activemq instances that are discovered
 * @author jhickey
 * 
 */
public class EmbeddedActiveMQServerDetector
        extends MxServerDetector {

    Log log = getLog();
    boolean recursive = false;
    private final static String RECURSIVE_PROP = "activemq.search.recursive";

    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        recursive = "true".equalsIgnoreCase(manager.getProperty(RECURSIVE_PROP, "false"));
        getLog().debug(RECURSIVE_PROP + "=" + recursive);
    }

    @Override
    protected File findVersionFile(File dir, Pattern pattern) {
        File res = null;
        log.debug("[findVersionFile] dir=" + dir + " pattern=" + pattern);
        if (!dir.exists()) {
            log.debug("File '" + dir + "' Not Found");
            return null;
        }
        // In an Embedded ActiveMQ instance, we know we are starting with
        // CATALINA_BASE
        // Give preferential search treatment to webapps/*/WEB-INF/lib for
        // performance gains
        File libDir = new File(dir, "lib");
        if (libDir.exists()) {
            File versionFile = super.findVersionFile(libDir, pattern);
            if (versionFile != null) {
                res = versionFile;
            }
        }

        if (res == null) {
            File webappsDir = new File(dir, "webapps");
            if (webappsDir.exists()) {
                for (File app : webappsDir.listFiles()) {
                    if (app.isDirectory()) {
                        File wlibDir = new File(app, "WEB-INF" + File.separator + "lib");
                        if (wlibDir.exists()) {
                            File versionFile = super.findVersionFile(wlibDir, pattern);
                            if (versionFile != null) {
                                res = versionFile;
                            }
                        }
                    } else if (app.getName().endsWith(".war")) {
                        try {
                            JarFile war = new JarFile(app);
                            Enumeration<JarEntry> files = war.entries();
                            while (files.hasMoreElements()) {
                                final String fileName = files.nextElement().toString();
                                if (pattern.matcher(fileName).find()) {
                                    res = new File(app + "!" + fileName);
                                    break;
                                }
                            }
                            war.close();
                        } catch (IOException ex) {
                            log.debug("Error: '" + app + "': " + ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

        if ((res == null) && recursive) {
            res = super.findVersionFile(dir, pattern);
        }

        log.debug("[findVersionFile] res=" + res);
        return res;
    }

    @Override
    protected ServerResource getServerResource(MxProcess process) {
        ServerResource server = super.getServerResource(process);
        String catalinaBase = server.getInstallPath();

        File hq = findVersionFile(new File(catalinaBase), Pattern.compile("hq-common.*\\.jar"));
        if (hq != null) {
            server.setName(getPlatformName() + " HQ ActiveMQ Embedded " + getTypeInfo().getVersion());
            server.setIdentifier("Embedded ActiveMQ");
        } else {
            server.setIdentifier(catalinaBase + " Embedded ActiveMQ");
        }
        return server;
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig)
            throws PluginException {

        JMXConnector connector = null;
        MBeanServerConnection mServer;

        try {
            connector = MxUtil.getMBeanConnector(serverConfig.toProperties());
            mServer = connector.getMBeanServerConnection();
        } catch (Exception e) {
            MxUtil.close(connector);
            throw new PluginException(e.getMessage(), e);
        }

        try {
            return _discoverMxServices(mServer, serverConfig);
        } finally {
            MxUtil.close(connector);
        }
    }

    protected List _discoverMxServices(MBeanServerConnection mServer,
            ConfigResponse serverConfig)
            throws PluginException {

        String url = serverConfig.getValue(MxUtil.PROP_JMX_URL);
        String objName = getTypeProperty(MxQuery.PROP_OBJECT_NAME);
        log.debug("--> url="+url);
        log.debug("--> objName="+objName);
        return discoverMxServices(mServer, serverConfig);
    }
}