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

package org.hyperic.hq.plugin.system;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;

public class WindowsDetector
    extends SystemServerDetector {

    protected String getServerType() {
        return SystemPlugin.WINDOWS_SERVER_NAME;
    }

    public List getServerResources(ConfigResponse platformConfig) 
        throws PluginException {

        if (isWin32()) {
            return super.getServerResources(platformConfig);
        }

        //should not get invoked if !win32, but just incase.
        return null;
    }

    /**
     * Windows services are created manually.
     */
    protected ArrayList getSystemServiceValues(Sigar sigar, ConfigResponse serverConfig)
        throws SigarException {

        String type = SystemPlugin.SVC_NAME;
        String prop = SystemPlugin.PROP_SVC;

        List serviceConfigs = getServiceConfigs(type);
    
        ArrayList services = new ArrayList();
    
        for (int i=0; i<serviceConfigs.size(); i++) {
            ConfigResponse serviceConfig = 
                (ConfigResponse)serviceConfigs.get(i);
            ConfigResponse cprops = new ConfigResponse();

            String name =
                serviceConfig.getValue(SystemPlugin.PROP_RESOURCE_NAME);
            String serviceName =
                serviceConfig.getValue(prop);
        
            AIServiceValue svc = createSystemService(type, name);

            Service service = null;
            try {
                service = new Service(serviceName);
                ServiceConfig config = service.getConfig();
                String desc = config.getDescription();
                if (desc == null) {
                    desc = config.getDisplayName();
                }
                svc.setDescription(desc);
                cprops.setValue("path", config.getPath());
                cprops.setValue("startupType", config.getStartTypeString());
                cprops.setValue("displayName", config.getDisplayName());
                svc.setCustomProperties(cprops.encode());
            } catch (Win32Exception e) {
                String msg =
                    "Error getting config for service=" +
                    serviceName + ": " + e.getMessage();
                log.error(msg);
            } catch (EncodingException e) {
                log.error("Error encoding cprops: " + e.getMessage());
            } finally {
                if (service != null) {
                    service.close();
                }
            }
            services.add(svc);
        }

        return services;
    }
}
