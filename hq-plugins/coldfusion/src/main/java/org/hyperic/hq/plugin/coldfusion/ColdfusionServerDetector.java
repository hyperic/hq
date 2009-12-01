/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.coldfusion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;

public class ColdfusionServerDetector extends ServerDetector implements
    AutoServerDetector {
    private static Log _log = LogFactory.getLog(ColdfusionServerDetector.class);

    private static final String SERVER_NAME = "Coldfusion";
    private static final String VERSION_6 = "6.x";
    private static final String VERSION_7 = "7.x";
    private static final String FILE_DIV = System.getProperty("file.separator");

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {
        if (isWin32()) {
            return findWindowsServerResources(platformConfig);
        } else {
            List servers = new ArrayList();
            String version = getTypeInfo().getVersion();
            String ptql = getTypeProperty("process.query");
            List paths = getServerProcessList(version, ptql);
            for (int i = 0; i < paths.size(); i++) {
                String dir = (String) paths.get(i);
                List found = getServerList(dir, version);
                if (!found.isEmpty()) {
                    servers.addAll(found);
                }
            }
            return servers;
        }
    }

    private List findWindowsServerResources(ConfigResponse platformConfig) {
        String versionFile = getTypeProperty("version.file");
        List configs;
        try {
            configs = Service
                .getServiceConfigs("Macromedia JRun CFusion Server");
        } catch (Win32Exception e) {
            return null;
        }
        if (configs.size() == 0) {
            return null;
        }

        List servers = new ArrayList();
        for (int i = 0; i < configs.size(); i++) {
            ServiceConfig serviceConfig = (ServiceConfig) configs.get(i);
            String name = serviceConfig.getName();
            File dir = new File(serviceConfig.getExe()).getParentFile();
            File dll = new File(dir, "lib" + FILE_DIV + versionFile);
            if (!dll.exists()) {
                continue;
            }
            dir = dir.getParentFile(); // strip "Binn"

            ServerResource server = createServerResource(
                dir.getAbsolutePath(), name);
            servers.add(server);
        }
        return servers;
    }

    private ServerResource createServerResource(String installpath,
        String name) {
        ServerResource server = createServerResource(installpath);

        String instance = name;
        server.setName(server.getName() + " " + instance);

        ConfigResponse config = new ConfigResponse();
        config.setValue(Win32ControlPlugin.PROP_SERVICENAME, name);
        server.setProductConfig(config);
        server.setMeasurementConfig();
        server.setControlConfig();

        return server;
    }

    private static List getServerProcessList(String version, String ptql)
        throws PluginException {
        List servers = new ArrayList();
        long[] pids = getPids(ptql);
        for (int i = 0; i < pids.length; i++) {
            String exe = getProcExe(pids[i]);
            if (exe == null)
                continue;
            File binary = new File(exe);
            if (!binary.isAbsolute()) {
                String path = getProcCwd(pids[i]);
                if (path != null) {
                    servers.add(path + "/" + binary);
                }
                throw new PluginException("plugin cannot determine process "
                    + binary + "'s absolute path.  Please check permissions.");
            } else {
                servers.add(binary.getAbsolutePath());
            }
        }
        return servers;
    }

    private List getServerList(String path, String version)
        throws PluginException {
        List servers = new ArrayList();
        String installpath = getParentDir(path, 3);
        if (version.equals(VERSION_6)) {
            installpath = getParentDir(path, 2);
        }

        ConfigResponse productConfig = new ConfigResponse();
        productConfig.setValue("installpath", installpath);

        ServerResource server = createServerResource(installpath);
        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        server.setCustomProperties(cprop);
        setProductConfig(server, productConfig);
        // sets a default Measurement Config property with no values
        server.setMeasurementConfig();
        server.setName(getPlatformName() + " " + SERVER_NAME + " " + version);
        servers.add(server);

        return servers;
    }
}
