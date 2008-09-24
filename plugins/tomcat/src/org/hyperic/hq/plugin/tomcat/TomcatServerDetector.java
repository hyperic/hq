/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc. This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.plugin.tomcat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TomcatServerDetector extends ServerDetector implements
        AutoServerDetector {

    private static final String TOMCAT_PARAMS_KEY = "\\Parameters\\Java";
    private static final String TOMCAT_SERVICE_KEY = "SOFTWARE\\Apache Software Foundation\\Procrun 2.0";
    
    private static final String PTQL_QUERY = "State.Name.eq=java,Args.*.ct=-Dcatalina.home=";
    private static final String PTQL_QUERY_WIN32 = "Pid.Service.eq=%service_name%";

    private static final String CATALINA_BASE_PROP = "-Dcatalina.base=";
    
    private static final String TOMCAT_DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://localhost:6969/jmxrmi";

    private Log log = LogFactory.getLog(TomcatServerDetector.class);

    private ServerResource getServerResource(String path, String win32Service)
            throws PluginException {
        ServerResource server = createServerResource(path);
        // Set PTQL query
        ConfigResponse config = new ConfigResponse();
        config.setValue(MxUtil.PROP_JMX_URL, TOMCAT_DEFAULT_URL);
        if (win32Service == null) {
            config.setValue(SigarMeasurementPlugin.PTQL_CONFIG, PTQL_QUERY
                    + path);
        }
        else { // win32 service
            config.setValue(Win32ControlPlugin.PROP_SERVICENAME, win32Service);
            config.setValue(SigarMeasurementPlugin.PTQL_CONFIG,
                    PTQL_QUERY_WIN32);
            server.setName(server.getName() + " " + win32Service);
        }
        server.setProductConfig(config);
        server.setMeasurementConfig();
        server.setControlConfig();
        return server;
    }

    /**
     * Helper method to discover Tomcat server paths using the process table
     */
    private List getServerProcessList() {
        ArrayList servers = new ArrayList();

        long[] pids = getPids(PTQL_QUERY);
        for (int i = 0; i < pids.length; i++) {
            String exe = getProcExe(pids[i]);
            if (exe == null) {
                continue;
            }

            log.debug("Detected Tomcat process " + exe);

            String catalinaBase = getCatalinaBase(getProcArgs(pids[i]));
            if (catalinaBase != null) {
                File catalinaBaseDir = new File(catalinaBase);
                if (catalinaBaseDir.exists()) {
                    log
                            .debug("Successfully detected Catalina Base for process: "
                                    + catalinaBase);
                    servers.add(catalinaBaseDir.getAbsolutePath());
                }
                else {
                    log.error("Resolved catalina base " + catalinaBase
                            + " is not a valid directory");
                }
            }
        }
        return servers;
    }

    private String[] getServicesFromRegistry() {
        RegistryKey key = null;
        String[] services = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY);
            services = key.getSubKeyNames();
        }
        catch (Win32Exception e) {
            // no tomcat services installed
        }
        finally {
            if (key != null) {
                key.close();
            }
        }
        return services;
    }

    /**
     * Helper method to discover Tomcat server paths using the Windows registry
     */
    private Map getServerRegistryMap() {
        Map serverMap = new HashMap();

        String[] services = getServicesFromRegistry();
        // return empty map if no windows services are found
        if (services == null) {
            return serverMap;
        }

        for (int i = 0; i < services.length; i++) {
            log.debug("Detected Tomcat service " + services[i]);
            List options = new ArrayList();
            RegistryKey key = null;
            try {
                key = RegistryKey.LocalMachine.openSubKey(TOMCAT_SERVICE_KEY
                        + "\\" + services[i] + TOMCAT_PARAMS_KEY);
                key.getMultiStringValue("Options", options);
            }
            catch (Win32Exception e) {
                log.error("Failed to find Java parameters for Tomcat service "
                        + services[i]);
                // skip current service
                continue;
            }
            finally {
                if (key != null) {
                    key.close();
                }
            }

            String catalinaBase = getCatalinaBase((String[]) options
                    .toArray(new String[0]));
            if (catalinaBase != null) {
                File catalinaBaseDir = new File(catalinaBase);
                if (catalinaBaseDir.exists()) {
                    log
                            .debug("Successfully detected Catalina Base for service: "
                                    + catalinaBase);
                    serverMap.put(services[i], catalinaBaseDir
                            .getAbsolutePath());
                }
                else {
                    log
                            .error("Resolved catalina base "
                                    + catalinaBase
                                    + " is not a valid directory. Skipping Tomcat service "
                                    + services[i]);
                }
            }
            // no catalina base found
            else {
                log.error("No Catalina Base found for service " + services[i]
                        + ". Skipping..");
            }
        }
        return serverMap;
    }

    /**
     * Auto scan
     */
    public List getServerResources(ConfigResponse platformConfig)
            throws PluginException {
        List servers = new ArrayList();

        // first, get servers based on process list
        List processPaths = getServerProcessList();
        // convert paths to server value types
        for (int i = 0; i < processPaths.size(); i++) {
            String dir = (String) processPaths.get(i);
            servers.add(getServerResource(dir, null));
        }

        // if we are on windows, take a look at the registry for autodiscovery
        if (isWin32()) {
            Map registryPaths = getServerRegistryMap();
            // convert paths to server value types
            for (Iterator it = registryPaths.keySet().iterator(); it.hasNext();) {
                String serviceName = (String) it.next();
                String dir = (String) registryPaths.get(serviceName);
                servers.add(getServerResource(dir, serviceName));
            }
        }
        return servers;
    }

    private String getCatalinaBase(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(CATALINA_BASE_PROP)) {
                return args[i].substring(CATALINA_BASE_PROP.length());
            }
        }
        return null;
    }
}
