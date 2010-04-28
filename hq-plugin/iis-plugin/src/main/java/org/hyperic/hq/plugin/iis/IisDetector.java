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

package org.hyperic.hq.plugin.iis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

import org.hyperic.util.config.ConfigResponse;

/**
 * Handles IIS server detection.
 */
public class IisDetector
    extends ServerDetector
    implements RegistryServerDetector {

    static final String VHOST_NAME = "VHost";

    // Registry path's and keys
    private static final String REG_INET = "SOFTWARE\\Microsoft\\InetStp";
    private static final String REG_INET_MAJORVER = "MajorVersion";
    private static final String REG_INET_MINORVER = "MinorVersion";
    
    // PDH Constants
    private static final String PDH_WEB_SERVICE = "Web Service";
    private static final String PDH_TOTAL       = "_Total";

    /**
     * Detect IIS servers using a registry scan
     *
     * @see RegistryServerDetector#getServerResources(ConfigResponse, String, RegistryKey)
     */
    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException
    {
        String pluginVersion = getTypeInfo().getVersion();
        
        path = getParentDir(path);

        try {
            RegistryKey versionInfo = 
                RegistryKey.LocalMachine.openSubKey(REG_INET);
            int majorVersion = 
                versionInfo.getIntValue(REG_INET_MAJORVER);
            int minorVersion = 
                versionInfo.getIntValue(REG_INET_MINORVER);

            String version = majorVersion + ".x";

            if (!pluginVersion.equals(version)) {
                // IIS version does not match the detector version.  Bypass
                // for now, the other detector will pick it up.
                return null;
            }
            
            ServerResource server = createServerResource(path);
            
            server.setControlConfig();

            setProductConfig(server, new ConfigResponse());
            setMeasurementConfig(server, new ConfigResponse());
            
            ConfigResponse cprops = new ConfigResponse();
            cprops.setValue("version", majorVersion + "." + minorVersion);
            server.setCustomProperties(cprops);

            List serverList = new ArrayList();
            serverList.add(server);
            return serverList;
        } catch (Win32Exception e) {
            return null;
        }
    }

    private ConfigResponse getMeasurementConfig(IisMetaBase info) {
        ConfigResponse config = new ConfigResponse();
        config.setValue(Collector.PROP_PORT, info.port);
        config.setValue(Collector.PROP_HOSTNAME, info.ip);
        if (info.hostname != null) {
            config.setValue("hostheader",
                            info.hostname);
        }
        config.setValue(Collector.PROP_SSL,
                        info.requireSSL);
        config.setValue(Collector.PROP_PROTOCOL,
                        getConnectionProtocol(info.port));

        return config;
    }

    private Map getWebSites() {
        try {
            return IisMetaBase.getWebSites();
        } catch (Win32Exception e) {
        } catch (UnsatisfiedLinkError e) {
            // Windows NT
        }
        return new HashMap();
    }
    
    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        ArrayList vhosts = new ArrayList();
        
        // Get the install path from the server resource.  This will be
        // used to auto-configure the IIS VHosts for Response Time collection.
        String installpath = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        
        try {
            String[] instances = Pdh.getInstances(PDH_WEB_SERVICE);

            for (int i = 0; i < instances.length; i++) {
                if (instances[i].equals(PDH_TOTAL)) {
                    continue;
                }
                vhosts.add(instances[i]);
            }
        } catch (Win32Exception e) {
            // Shouldn't happen
            throw new PluginException("Error getting PDH data: " + 
                                      e.getMessage(), e);
        }

        Map websites = getWebSites();
        boolean hasWebsites = websites.size() != 0;

        List services = new ArrayList();

        for (int i=0; i<vhosts.size(); i++) {
            String siteName = (String)vhosts.get(i);

            ServiceResource service = new ServiceResource();
            service.setType(this, VHOST_NAME);
            service.setServiceName(siteName);

            IisMetaBase info = (IisMetaBase)websites.get(siteName);

            ConfigResponse cprops = new ConfigResponse();
            ConfigResponse metricProps;
            Properties rtProps = new Properties();
            if (info != null) {
                metricProps = getMeasurementConfig(info);
                if (info.path != null) {
                    cprops.setValue("docroot", info.path);
                }
            }
            else {
                if (hasWebsites) {
                    //deleting a web site from iis admin does not
                    //delete the performance counter entry.
                    getLog().debug("Configuration not found for site: " +
                                   siteName);
                    continue;
                }
                //XXX iis7
                metricProps = new ConfigResponse();
            }

            // Auto-configure measurement properties.
            metricProps.setValue(IisMeasurementPlugin.PROP_IISHOST,
                                 siteName);
            
            // Auto-configure response-time properties.  IIS 5.x and 6.x put
            // logs by default in system32.  (Even though IIS 5.x installs
            // into C:\Windows\System32\inetsrv).  Should try to get this
            // info from either metabase or the registry, though this will
            // cover most cases.
            if (info != null) {
                rtProps.setProperty(IisRtPlugin.CONFIG_LOGDIR,
                                    "C:\\Windows\\System32\\LogFiles\\W3SVC" +
                                    info.id);
            }
            rtProps.setProperty(IisRtPlugin.CONFIG_INTERVAL, "60");
            rtProps.setProperty(IisRtPlugin.CONFIG_LOGMASK, "*.log");

            service.setProductConfig();
            setMeasurementConfig(service, metricProps);
            service.setCustomProperties(cprops);
            //service.setResponseTimeConfig(new ConfigResponse(rtProps));
            services.add(service);
        }

        return services;
    }
}
