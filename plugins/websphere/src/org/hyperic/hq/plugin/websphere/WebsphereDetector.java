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

package org.hyperic.hq.plugin.websphere;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.sigar.win32.RegistryKey;

/**
 * Base class for WebSphere 4.0/5.0 Admin Server file scan discovery.
 */

public abstract class WebsphereDetector
    extends ServerDetector 
    implements FileServerDetector,
               RegistryServerDetector,
               AutoServerDetector {

    private static final String[] METRIC_CONNECT_PROPS = {
        WebsphereProductPlugin.PROP_ADMIN_PORT,
        WebsphereProductPlugin.PROP_ADMIN_HOST
    };

    protected Log log = LogFactory.getLog("WebsphereDetector");

    public WebsphereDetector() {
        super();
        setName(WebsphereProductPlugin.SERVER_NAME);
    }

    protected abstract List discoverServers(ConfigResponse config)
        throws PluginException;

    protected abstract String getProcessQuery();

    protected abstract String getAdminHost();

    protected abstract String getAdminPort();

    protected abstract String getNodeName();

    protected abstract String getStartupScript();

    protected Properties loadProps(File file) {
        Properties props = new Properties();
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            props.load(is);
        } catch (IOException e) {
            //ok
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }

        return props;
    }

    protected String getNodeNameFromFQDN(String fqdn) {
        int idx = fqdn.indexOf(".");
        if (idx > 0) {
            return fqdn.substring(0, idx);
        }
        return fqdn;
    }

    protected String getDefaultServerNode() {
        return getNodeNameFromFQDN(getPlatformName());
    }

    protected Properties getProductConfig(File path) {
        Properties productProps = new Properties();

        productProps.setProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                                 getAdminHost());

        productProps.setProperty(WebsphereProductPlugin.PROP_ADMIN_PORT,
                                 getAdminPort());

        String node = getNodeName();
        if (node == null) {
            node = getDefaultServerNode();
        }
        productProps.setProperty(WebsphereProductPlugin.PROP_SERVER_NODE,
                                 node);

        return productProps;
    }

    protected void initDetector(File root) {
    }

    protected boolean isServiceControl() {
        return isWin32();
    }

    static class Process {
        String installRoot; // /opt/WebSphere/AppServer
        String serverRoot; // /opt/WebSphere/AppServer/profiles/default

        boolean isConfigured() {
            return
                (this.installRoot != null) &&
                (this.serverRoot  != null);
        }

        public String toString() {
            return
                "was.install.root=" + this.installRoot +
                ", server.root=" + this.serverRoot;
        }
    }
    
    protected static List getServerProcessList(String query) {
        final String wasProp  = "-Dwas.install.root=";
        final String rootProp = "-Dserver.root="; //5.x
        final String bootProp = "-bootFile"; //4.0

        ArrayList servers = new ArrayList();

        long[] pids = getPids(query);

        for (int i=0; i<pids.length; i++) {
            String[] args = getProcArgs(pids[i]);
            Process process = new Process();

            for (int j=0; j<args.length; j++) {
                String arg = args[j];

                if (arg.startsWith(wasProp)) {
                    process.installRoot =
                        arg.substring(wasProp.length(), arg.length());
                }
                else if (arg.startsWith(rootProp)) {
                    process.serverRoot =
                        arg.substring(rootProp.length(), arg.length());
                }
                else if (arg.equals(bootProp)) {
                    arg = new File(args[j+1]).getParentFile().getParent();
                    process.installRoot = process.serverRoot = arg;
                }

                if (process.isConfigured()) {
                    servers.add(process);
                    break;
                }
            }
        }

        return servers;
    }

    protected static String getRunningInstallPath(String query) {
        List servers = getServerProcessList(query);

        if (servers.size() == 0) {
            return null;
        }

        return ((Process)servers.get(0)).installRoot;
    }

    protected List getServerList(File serverDir, String version)
        throws PluginException {
        File controlScript = null;

        if (!isServiceControl()) {
            controlScript =
                new File(serverDir, getStartupScript());

            if (!controlScript.exists()) {
                this.log.debug(controlScript + " Not found");
                return null;
            }
        }

        //make sure detector version is that of was version
        //else the 5.0 detector will report 6.0 servers
        if (version != null) {
            char majVersion =
                getTypeInfo().getVersion().charAt(0);
            if (version.charAt(0) != majVersion) {
                return null;
            }
        }

        //distinquish between 5.x and 6.x using a unique file
        //since there is no simple way to get the version on disk.
        String uniqueFile =
            getTypeProperty("UNIQUE_FILE");

        if (uniqueFile != null) {
            File file = new File(serverDir, uniqueFile);
            boolean exists = file.exists();

            log.debug(getTypeInfo().getName() + " '" +
                      file + "'.exists()=" + exists);
          
            if (!exists) {
                return null;
            }
        }
        
        List servers = new ArrayList();

        initDetector(serverDir);

        String installpath = serverDir.getAbsolutePath();
        ServerResource server = createServerResource(installpath);

        String type = getTypeInfo().getName();

        //for example, having WebSphere 5.0 and 5.1 installed
        //on the same machine, use version in the name to make
        //them unique.
        if ((version != null) && (version.length() == 3)) {
            if (!type.endsWith(version)) {
                type = type.substring(0, type.length()-3) + version;
            }
        }
        server.setName(getPlatformName() + " " + type);

        ConfigResponse controlConfig = null;

        if (!isServiceControl()) {
            Properties controlProps = new Properties();
            controlProps.setProperty(ServerControlPlugin.PROP_PROGRAM,
                                     controlScript.getAbsolutePath());
            controlConfig = 
                new ConfigResponse(controlProps);
        }

        ConfigResponse productConfig =
            new ConfigResponse(getProductConfig(serverDir));

        //if we find more than one server w/ the same connect config,
        //this will make sure only 1 gets auto-enabled for metrics/ai
        server.setConnectProperties(METRIC_CONNECT_PROPS);

        server.setProductConfig(productConfig);
        server.setMeasurementConfig();
        if (controlConfig != null) {
            server.setControlConfig(controlConfig);
        }

        servers.add(server);

        this.log.debug("Detected " + server.getName() +
                       " in " + serverDir);

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException {

        path = path.trim(); //trim trailing ^@

        log.debug("checking path=" + path);

        String version = current.getSubKeyName();
        //5.0.0.0, 5.1.0.0, etc.
        if ((version != null) && (version.length() > 3)) {
            version = version.trim().substring(0, 3);
        }
        return getServerList(new File(path), version);
    }

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();
        List processes = getServerProcessList(getProcessQuery());

        for (int i=0; i<processes.size(); i++) {
            Process process = (Process)processes.get(i);
            List found = getServerList(new File(process.serverRoot), null);
            if (found != null) {
                servers.addAll(found);
            }
        }

        return servers;
    }

    public List getServerResources(ConfigResponse platformConfig, String path)
        throws PluginException {

        this.log.debug("Looking for " + getName() + " in " + path);

        File jar = new File(path);

        //loose lib/foo.jar defined in etc/cam-server-sigs.properties
        File serverDir = jar.getParentFile().getParentFile();

        return getServerList(serverDir, null);
    }
}
