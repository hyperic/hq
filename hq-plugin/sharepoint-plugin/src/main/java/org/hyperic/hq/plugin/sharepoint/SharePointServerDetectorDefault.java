/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

public abstract class SharePointServerDetectorDefault extends ServerDetector implements AutoServerDetector {

    private Log log = LogFactory.getLog(SharePointServerDetectorDefault.class);
    /**
     * group , instance , counter
     */
    private final static List<String[]> statsServicesNameList = Arrays.asList(new String[][]{
                {"Records Management Counters", null, "Search results processed / sec base"},
                {"Publishing Cache", "_total", "Total object discards"},
                {"Disk-Based Cache", null, "Old Blob Cache Folders Pending Delete"},
                {"Foundation Search Gatherer Projects", "_total", "Gatherer Master Flag"},
                {"Foundation Search Schema Plugin", "_total", "Total Documents"},
                {"Foundation BDC Online", null, "Total calls failed"},
                {"Foundation Search Gatherer", null, "Filter Processes Terminated 02"},
                {"Foundation Search Indexer Plugin", "_total", "Persistent Indexes Propagated"},
                {"Foundation Search Query Processor", null, "Security Descriptor Cache Misses"},
                {"Foundation Search FAST Content Plugin", null, "Batches Failed Timeout"},
                {"Foundation Search Archival Plugin", "_total", "Queues Committing"},
                {"Foundation BDC Metadata", null, "Cache misses per second"},
                {"Foundation Search Gatherer Databases", "_total", "Documents in the crawl history"}
            });

    @Override
    public final List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("[discoverServices] config=" + platformConfig);
        List<ServerResource> servers = new ArrayList();

        String installPath = null;
        String versionKey = getTypeProperty("version");
        try {
            RegistryKey key = RegistryKey.LocalMachine.openSubKey(versionKey);
            installPath = key.getStringValue("Location");
            key.close();
        } catch (Win32Exception ex) {
            log.debug("version registy key '" + versionKey + "' not found.", ex);
        }

        if (installPath != null) {
            ServerResource server = createServerResource(installPath);
            ConfigResponse pc = new ConfigResponse();
            pc.setValue(SharePoint.PROP_MAINURL, getMainUrl());
            setProductConfig(server, pc);
            setMeasurementConfig(server, new ConfigResponse());


            String websNames = "";
            List<IisMetaBase> webs = getWebServersNames();
            for (int i = 0; i < webs.size(); i++) {
                IisMetaBase web = webs.get(i);
                if (websNames.length() > 0) {
                    websNames += ", ";
                }
                websNames += web.getName();
                log.debug(" => "+web.getName());
            }

            String serviceNames = "";
            List<Service> winServices = getWinServiceList();
            for (int i = 0; i < winServices.size(); i++) {
                Service service = winServices.get(i);
                if (serviceNames.length() > 0) {
                    serviceNames += ", ";
                }
                try {
                    log.debug(" *> "+service.getConfig().getName());
                    serviceNames += service.getConfig().getName();
                } catch (Win32Exception ex) {
                    log.debug("Error", ex);
                }
            }

            log.debug(" -> websNames = "+websNames);
            log.debug(" -> serviceNames = "+serviceNames);
            ConfigResponse cc = new ConfigResponse();
            cc.setValue(SharePoint.PROP_C_WEBS, websNames);
            cc.setValue(SharePoint.PROP_C_SERVICES, serviceNames);
            setProductConfig(server, cc);

            setControlConfig(server, new ConfigResponse());

            if ((webs.size() > 0) || (winServices.size() > 0)) {
                servers.add(server);
            }
        }

        return servers;
    }

    private List<IisMetaBase> getWebServersNames() {
        List<IisMetaBase> list = new ArrayList<IisMetaBase>();
        try {
            Map<String, IisMetaBase> websites = IisMetaBase.getWebSites();
            for (Iterator<String> it = websites.keySet().iterator(); it.hasNext();) {
                String siteName = it.next();
                if (siteName.startsWith("SharePoint")) {
                    IisMetaBase web = websites.get(siteName);
                    try {
                        if (testWebServer(web.toUrlString())) {
                            log.debug("web '" + siteName + "' is Running");
                            web.setName(siteName);
                            list.add(web);
                        } else {
                            log.debug("web '" + siteName + "' is NOT running");
                        }
                    } catch (MalformedURLException ex) {
                        log.debug("Error formating URL for Webserver '" + siteName + "':'" + web + "'", ex);
                    }
                }
            }
        } catch (Win32Exception ex) {
            log.debug("Error looking for Webservers", ex);
        }
        return list;
    }

    private List<Service> getWinServiceList() {
        List<Service> services = new ArrayList<Service>();
        try {
            List<String> winServices = Service.getServiceNames();
            for (int i = 0; i < winServices.size(); i++) {
                String name = winServices.get(i);
                try {
                    Service winService = new Service(name);
                    String fullName = winService.getConfig().getDisplayName();
                    if (fullName.startsWith(servicesPrefix())) {
                        if (winService.getStatus() == Service.SERVICE_RUNNING) {
                            log.debug(fullName + " (" + name + ") is RUNNING");
                            services.add(winService);
                        } else {
                            log.debug(fullName + " (" + name + ") is NOT running");
                        }
                    }
                } catch (Win32Exception ex) {
                    log.debug("Service '" + name + "' ignored: " + ex.getMessage());
                }
            }
        } catch (Win32Exception ex) {
            log.debug("Error looking for Windows Services :" + ex, ex);
        }
        return services;
    }

    @Override
    protected final List discoverServices(ConfigResponse serverConfig) throws PluginException {
        log.debug("[discoverServices] config=" + serverConfig);
        ArrayList<ServiceResource> services = new ArrayList();

        List<IisMetaBase> webs = getWebServersNames();
        for (int i = 0; i < webs.size(); i++) {
            IisMetaBase web = webs.get(i);
            ServiceResource service = new ServiceResource();
            service.setType(this, "Webserver");
            service.setServiceName("Webserver " + web.getName());
            services.add(service);

            try {
                ConfigResponse pc = new ConfigResponse();
                pc.setValue("url", web.toUrlString());
                pc.setValue("name", web.getName());
                setProductConfig(service, pc);
            } catch (MalformedURLException ex) {
                log.debug("Error formating URL for Webserver '" + web.getName() + "':'" + web + "'", ex);
            }
            setMeasurementConfig(service, new ConfigResponse());
        }

        List<Service> winServices = getWinServiceList();
        for (int i = 0; i < winServices.size(); i++) {
            try {
                Service winService = winServices.get(i);
                ServiceResource service = new ServiceResource();
                service.setType(this, "WindowsService");
                service.setServiceName("Windows Service " + winService.getConfig().getDisplayName());
                ConfigResponse pc = new ConfigResponse();
                pc.setValue("service_name", winService.getConfig().getName());
                setProductConfig(service, pc);
                service.setMeasurementConfig();
                service.setControlConfig();
                services.add(service);
            } catch (Win32Exception ex) {
                log.debug("Error creating Window Service :" + ex, ex);
            }
        }

        try {
            Pdh pdh = new Pdh();
            for (int i = 0; i < getStatsServicesNameList().size(); i++) {
                String[] counter = getStatsServicesNameList().get(i);
                try {
                    StringBuilder obj = new StringBuilder("\\SharePoint ");
                    obj.append(counter[0]);
                    if (counter[1] != null) {
                        obj.append("(").append(counter[1]).append(")");
                    }
                    obj.append("\\").append(counter[2]);
                    log.debug("obj='" + obj + "'");
                    // checking if there is metrics, if not we get a Win32Exception 
                    pdh.getFormattedValue(obj.toString());
                    ServiceResource service = new ServiceResource();
                    service.setType(this, counter[0]);
                    service.setServiceName(counter[0]);
                    service.setMeasurementConfig();
                    service.setProductConfig();
                    services.add(service);
                } catch (Win32Exception ex) {
                    log.debug("ignoring service '" + counter[0] + "' error:" + ex, ex);
                }
            }
        } catch (Win32Exception ex) {
            log.debug("Error accesing perfomance counters :" + ex, ex);
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

    private boolean testWebServer(String url) {
        boolean res = false;
        HttpGet get = new HttpGet(url);
        AgentKeystoreConfig ksConfig = new AgentKeystoreConfig();
        HQHttpClient client = new HQHttpClient(ksConfig, new HttpConfig(5000, 5000, null, 0), ksConfig.isAcceptUnverifiedCert());
        try {
            HttpResponse response = client.execute(get, new BasicHttpContext());
            int r = response.getStatusLine().getStatusCode();
            log.debug("[testWebServer] url='" + get.getURI() + "' statusCode='" + r + "' " + response.getStatusLine().getReasonPhrase());
            res = (r < 500);
        } catch (IOException ex) {
            log.debug(ex.getMessage(), ex);
        }
        return res;
    }

    protected abstract String servicesPrefix();

    protected abstract List<String[]> getStatsServicesNameList();
}
