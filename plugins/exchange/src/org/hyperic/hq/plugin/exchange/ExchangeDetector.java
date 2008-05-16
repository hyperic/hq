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

package org.hyperic.hq.plugin.exchange;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;

import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class ExchangeDetector
    extends ServerDetector
    implements RegistryServerDetector, RuntimeDiscoverer {

    private static final String IMAP4_NAME   = "IMAP4";
    private static final String POP3_NAME    = "POP3";
    private static final String MTA_NAME     = "MTA";
    private static final String WEB_NAME     = "Web";

    private static final String VERSION_2000 = "2000";
    private static final String VERSION_2003 = "2003";
    private static final String VERSION_55   = "5.5";

    private static final String[] INTERNAL_SERVICES = {
        IMAP4_NAME,
        POP3_NAME,
        MTA_NAME,
    };

    private static final String EXCHANGE_KEY =
        "SOFTWARE\\Microsoft\\Exchange\\Setup";

    private static final String[] SCAN_KEYS = {
        EXCHANGE_KEY,
    };

    private static final List SCAN_KEYS_LIST =
        Arrays.asList(SCAN_KEYS);

    static Log log = LogFactory.getLog("ExchangeDetector");

    private Long nowTime;
    private int serverId;

    private boolean isExchangeServiceRunning(String name) {
        if (name.equals(MTA_NAME)) {
            return isWin32ServiceRunning("MSExchangeMTA");
        }
        return isWin32ServiceRunning(name + "Svc");
    }

    private AIServiceValue createInternalService(String name) {
        AIServiceValue service = new AIServiceValue();
        service.setServiceTypeName(getTypeInfo().getName() + " " + name);
        service.setServerId(this.serverId);
        service.setName("%serverName%" + " " + name);
        service.setCTime(this.nowTime);
        service.setMTime(this.nowTime);
        service.setProductConfig(ConfigResponse.EMPTY_CONFIG);
        service.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
        return service;
    }

    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config) 
        throws PluginException {

        this.nowTime = new Long(System.currentTimeMillis());
        this.serverId = serverId;

        ArrayList serviceList = new ArrayList();

        //POP3 + IMAP4 are disabled by default, only report the services
        //if they are enabled and running.
        for (int i=0; i<INTERNAL_SERVICES.length; i++) {
            String service = INTERNAL_SERVICES[i];
            if (!isExchangeServiceRunning(service)) {
                log.debug(service + " is not running");
                continue;
            }
            else {
                log.debug(service + " is running, adding to inventory");
            }

            serviceList.add(createInternalService(INTERNAL_SERVICES[i]));
        }

        serviceList.add(createInternalService(WEB_NAME));

        AIServiceValue[] services =
            (AIServiceValue[])serviceList.toArray(new AIServiceValue[0]);

        AIServerExtValue thisServer = new AIServerExtValue();
        thisServer.setPlaceholder(true);
        thisServer.setId(new Integer(serverId));
        thisServer.setAIServiceValues(services);
        aiplatform.addAIServerValue(thisServer);

        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);
        rrr.addAIPlatform(aiplatform);
        return rrr;
    }

    private static String getExchangeVersion(RegistryKey key) {
        int version;
        try {
            version = key.getIntValue("Services Version");
        } catch (Win32Exception e) {
            return VERSION_55;
        }

        if (version >= 65) {
            return VERSION_2003;
        }

        return VERSION_2000;
    }

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException {

        RegistryKey key = current;

        String version = getExchangeVersion(key);

        if (getName().indexOf(version) == -1) {
            return null;
        }

        ServerResource server = createServerResource(path);

        //dont bother enabling metrics/ai unless its running
        if (isWin32ServiceRunning("MSExchangeIS")) {
            server.setProductConfig();
            server.setMeasurementConfig();
        }

        ConfigResponse cprops = new ConfigResponse();
        try {
            cprops.setValue("version", key.getStringValue("Services Version"));
            cprops.setValue("build", key.getStringValue("NewestBuild"));
            server.setCustomProperties(cprops);
        } catch (Win32Exception e) {
        }

        ArrayList servers = new ArrayList();
        servers.add(server);
        return servers;
    }

    public List getRegistryScanKeys() {
        return SCAN_KEYS_LIST;
    }

    public RuntimeDiscoverer getRuntimeDiscoverer() {
        return this;
    }
}
