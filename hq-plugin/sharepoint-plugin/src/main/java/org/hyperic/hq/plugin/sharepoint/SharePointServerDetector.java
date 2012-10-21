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
package org.hyperic.hq.plugin.sharepoint;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class SharePointServerDetector extends ServerDetector implements AutoServerDetector {

    private Log log = LogFactory.getLog(SharePointServerDetector.class);
    private final static List<String> servicesNameList = Arrays.asList(new String[]{
                "Records Management Counters",
                "Publishing Cache",
                "Disk-Based Cache",
                "Foundation Search Gatherer Projects",
                "Foundation Search Schema Plugin",
                "Foundation BDC Online",
                "Foundation Search Gatherer",
                "Foundation Search Indexer Plugin",
                "Foundation Search Query Processor",
                "Foundation Search FAST Content Plugin",
                "Foundation Search Archival Plugin",
                "Foundation BDC Metadata",
                "Foundation Search Gatherer Databases"});

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("[discoverServices] config=" + platformConfig);
        List<ServerResource> servers = new ArrayList();

        String versionKey = getTypeProperty("version");
        try {
            RegistryKey key = RegistryKey.LocalMachine.openSubKey(versionKey);

            String installPath = key.getStringValue("Location");
            ServerResource server = createServerResource(installPath);

            ConfigResponse pc = new ConfigResponse();
            pc.setValue(SharePoint.PROP_MAINURL, getMainUrl());
            setProductConfig(server, pc);
            setMeasurementConfig(server, new ConfigResponse());

            servers.add(server);

            key.close();
        } catch (Win32Exception ex) {
            log.debug("version registy key '" + versionKey + "' not found.", ex);
        }
        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
        log.debug("[discoverServices] config=" + serverConfig);
        ArrayList<ServiceResource> services = new ArrayList();

        try {
            Map<String, IisMetaBase> websites = IisMetaBase.getWebSites();
            for (Iterator<String> it = websites.keySet().iterator(); it.hasNext();) {
                String siteName = it.next();
                if (siteName.startsWith("SharePoint")) {
                    IisMetaBase web = websites.get(siteName);
                    ServiceResource service = new ServiceResource();
                    service.setType(this, "Webserver");
                    service.setServiceName("Webserver " + siteName);
                    services.add(service);

                    try {
                        ConfigResponse pc = new ConfigResponse();
                        pc.setValue("url", web.toUrlString());
                        setProductConfig(service, pc);
                        setMeasurementConfig(service, new ConfigResponse());
                    } catch (MalformedURLException ex) {
                        log.debug("Error formating URL for Webserver '" + siteName + "':'" + web + "'", ex);
                    }
                }
            }
        } catch (Win32Exception ex) {
            log.debug("Error looking for Webservers", ex);
        }

        List<String> winServices;
        try {
            winServices = Service.getServiceNames();
        } catch (Win32Exception ex) {
            winServices = new ArrayList();
            log.debug("Error looking for Services", ex);
        }

        for (int i = 0; i < winServices.size(); i++) {
            String name = winServices.get(i);
            try {
                Service winService = new Service(name);
                String fullName = winService.getConfig().getDisplayName();
                if (fullName.startsWith("SharePoint ") || name.equals("MSSQL$SHAREPOINT")) {
                    log.debug(name + " : " + fullName);
                    ServiceResource service = new ServiceResource();
                    service.setType(this, "WindowsService");
                    service.setServiceName("Windows Service " + fullName);
                    services.add(service);
                    ConfigResponse pc = new ConfigResponse();
                    pc.setValue("service_name", name);
                    setProductConfig(service, pc);
                    service.setMeasurementConfig();
                }
            } catch (Win32Exception ex) {
                log.debug("Service '" + name + "' ignored: " + ex.getMessage());
            }
        }

        for (int i = 0; i < servicesNameList.size(); i++) {
            String name = servicesNameList.get(i);
            ServiceResource service = new ServiceResource();
            service.setType(this, name);
            service.setServiceName(name);
            service.setMeasurementConfig();
            service.setProductConfig();
            services.add(service);
        }
        return services;
    }

    private String getMainUrl() {
        log.debug("[getMainUrl]");
        String urlStr = null;
        try {
            Map<String, IisMetaBase> websites = IisMetaBase.getWebSites();
            for (Iterator<String> it = websites.keySet().iterator(); it.hasNext();) {
                String name = it.next();
                IisMetaBase web = websites.get(name);
                if (name.startsWith("SharePoint -")) { // todo: put it in a configurable regex?
                    try {
                        urlStr = web.toUrlString();
                    } catch (MalformedURLException ex) {
                        log.debug("Error formating URL for Webserver '" + name + "':'" + web + "'", ex);
                    }
                }
            }
        } catch (Win32Exception ex) {
            log.debug("Error looking for URLs", ex);
        }

        return urlStr;
    }
}
