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

package org.hyperic.hq.plugin.ntds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.IntegerConfigOption;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

public class ActiveDirectoryDetector
    extends ServerDetector
    implements RegistryServerDetector {

    private static Log log =
        LogFactory.getLog("ActiveDirectoryDetector");

    private static final String[] SERVICES = {
        "LDAP", "Authentication"      
    };
    
    public List getServerResources(ConfigResponse platformConfig,
                                   String path,
                                   RegistryKey current) 
        throws PluginException {

        /**
         * HHQ-5553
         */
        String pluginServerVersionStr = getTypeInfo().getVersion();
        int pluginServerVersion=-1;
        if (pluginServerVersionStr!=null) {
            try {
                pluginServerVersion = Integer.valueOf(pluginServerVersionStr);
            } catch (NumberFormatException e) {
            }
        }
        
        if (!new File(path).exists()) {
            log.debug(path + " does not exist");
            return null;
        }

        ConfigResponse cprops = new ConfigResponse();
        List cpropKeys = getCustomPropertiesSchema().getOptions();

        for (int i=0; i<cpropKeys.size(); i++) {
            ConfigOption option = (ConfigOption)cpropKeys.get(i);
            String key = option.getName();
            String value;
            try {
                if (option instanceof IntegerConfigOption) {
                    value = String.valueOf(current.getIntValue(key));
                }
                else {
                    value = current.getStringValue(key).trim();
                }
            } catch (Win32Exception e) {
                continue;
            }
            cprops.setValue(key, value);
        }
        
        // we use the AD schema version to decide which server plugin to use, because of a bug in sigar which causes it to identify win 2012 as win 2008
        int schemaVersion = -1;
        try {
            schemaVersion = Integer.valueOf(cprops.getValue("Schema Version"));
        } catch (NumberFormatException e) {
        }
        switch (schemaVersion) {
        case 13:    // 2000
            if (pluginServerVersion!=2000) {
                return null;
            }
            break;
        case 30:    // 2003 RTM
        case 31:    // 2003 R2
            if (pluginServerVersion!=2003) {
                return null;
            }
            break;
        case 44:    // 2008 RTM
        case 47:    // 2008 R2
        case 52:
            if (pluginServerVersion!=2008) {
                return null;
            }
            break;
        case 56:    // 2012 RTM
            if (pluginServerVersion!=2012) {
                return null;
            }
            break;
        default:
            log.error("unknown AD schema version number - " + schemaVersion);
        }
        ServerResource server = createServerResource(path);
        server.setProductConfig();
        server.setMeasurementConfig();
        server.setCustomProperties(cprops);

        List servers = new ArrayList();
        servers.add(server);
        return servers;
    }
    
    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {

        List services = new ArrayList();

        for (int i=0; i<SERVICES.length; i++) {
            ServiceResource service = new ServiceResource();
            service.setType(this, SERVICES[i]);
            service.setServiceName(SERVICES[i]);
            service.setProductConfig();
            service.setMeasurementConfig();
            services.add(service);
        }

        return services;
    }
}
