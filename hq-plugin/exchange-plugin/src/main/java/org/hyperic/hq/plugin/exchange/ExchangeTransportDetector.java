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

package org.hyperic.hq.plugin.exchange;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.*;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

//2007 - Edge role does not have MSExchangeIS or most of the other services
//both Edge and Hub have MSExchangeTransport
public class ExchangeTransportDetector
    extends ServerDetector
    implements AutoServerDetector {

    private static final Log log =
            LogFactory.getLog(ExchangeTransportDetector.class.getName());

    private static final String TRANSPORT =
        ExchangeDetector.EX + "Transport";

    private static final String SMTP_SEND =
        "SmtpSend";

    private static final String SMTP_RECEIVE =
        "SmtpReceive";

    private static final String[] SERVICES = {
        SMTP_SEND, SMTP_RECEIVE
    };

    public List getServerResources(ConfigResponse platformConfig)
        throws PluginException {

        List servers = new ArrayList();

        String exe;
        Service exch = null;
        try {
            exch = new Service(TRANSPORT);
            if (exch.getStatus() != Service.SERVICE_RUNNING) {
                log.debug("[getServerResources] service '" + TRANSPORT
                        + "' is not RUNNING (status='" + exch.getStatusString() + "')");
                return null;
            }
            exe = exch.getConfig().getExe().trim();
        } catch (Win32Exception e) {
            log.debug("[getServerResources] Error getting '" + TRANSPORT
                    + "' service information " + e, e);
            return null;
        } finally {
            if (exch != null) {
                exch.close();
            }
        }

        File bin = new File(exe).getParentFile();
        if (!isInstallTypeVersion(bin.getPath())) {
            log.debug("[getServerResources] exchange on '" + bin
                    + "' IS NOT a " + getTypeInfo().getName());
            return null;
        } else {
            log.debug("[getServerResources] exchange on '" + bin
                    + "' IS a " + getTypeInfo().getName());
        }
        
        ConfigResponse cprops = new ConfigResponse();
        ConfigResponse productProps = new ConfigResponse();
        String roleRegKeyStr = getTypeProperty(ExchangeUtils.EXCHANGE_ROLE_REG_KEY);
        if (roleRegKeyStr != null) {
            if (!ExchangeUtils.checkRoleConfiguredAndSetVersion(roleRegKeyStr, cprops)) {
                if (log.isDebugEnabled()) {
                    log.debug("role configured  - but not found in registry - ignoring server:" + roleRegKeyStr);
                }
                return null;
            }
            String discoverSite = getTypeProperty(ExchangeUtils.SITE_DISCOVERY);
            if (discoverSite!=null){
                String adSiteName = ExchangeUtils.fetchActiveDirectorySiteName();
                if (adSiteName!=null){
                    productProps.setValue(ExchangeUtils.AD_SITE_PROP, adSiteName);
                }
            }           
        }
        
        ServerResource server = createServerResource(exe);
        server.setCustomProperties(cprops);
        server.setIdentifier(TRANSPORT);
        setProductConfig(server,productProps);
        server.setMeasurementConfig();
        servers.add(server);
        return servers;
    }

    private List discoverPerfServices(String type) {
        List services = new ArrayList();

        try {
            String[] instances =
                Pdh.getInstances(TRANSPORT + " " + type);

            for (int i=0; i<instances.length; i++) {
                String name = instances[i];
                if (name.equalsIgnoreCase("_Total")) {
                    continue;
                }
                
                ServiceResource service = new ServiceResource();
                service.setType(this, type);
                service.setName(type + " " + name);

                ConfigResponse config = new ConfigResponse();
                config.setValue("name", name);
                service.setProductConfig(config);
                service.setMeasurementConfig();

                services.add(service);
            }
        } catch (Win32Exception e) {
            log.debug(e, e);
        }

        return services;
        
    }

    @Override
    protected List discoverServices(ConfigResponse config)
            throws PluginException {

        List services = new ArrayList();
        for (int i=0; i<SERVICES.length; i++) {
            services.addAll(discoverPerfServices(SERVICES[i]));
        }
        return services;
    }
}
