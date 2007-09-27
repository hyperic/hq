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

package org.hyperic.hq.plugin.netdevice;

import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.util.config.ConfigResponse;

public class NetworkHostDetector extends NetworkDeviceDetector {
    
    private static final String STORAGE_NAME   = "Storage";
    private static final String PROP_STORAGE   = STORAGE_NAME.toLowerCase();
    private static final String STORAGE_COLUMN = "hrStorageDescr";
    
    public List discoverServices(ConfigResponse serverConfig)
            throws PluginException {

        List services = super.discoverServices(serverConfig);

        openSession(serverConfig);

        List storageDesc = getColumn(STORAGE_COLUMN);

        for (int i=0; i<storageDesc.size(); i++) {
            ConfigResponse config = new ConfigResponse();
            String name = storageDesc.get(i).toString();

            ServiceResource service = createServiceResource(STORAGE_NAME);

            config.setValue(PROP_STORAGE, name);
            service.setProductConfig(config);
            //required to auto-enable metric
            service.setMeasurementConfig();
            
            service.setServiceName(name.trim() + " " + STORAGE_NAME);

            services.add(service);
        }

        closeSession();
        
        return services;
    }
}
