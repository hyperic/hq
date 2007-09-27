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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.util.config.ConfigResponse;

/**
 * This class is intended for use by Platform types
 * which have service types, but no server types.
 */
public abstract class PlatformServiceDetector
    extends ServerDetector
    implements AutoServerDetector {
    
    public static final String PROP_IPADDRESS = "ipaddress";
    
    protected abstract List discoverServices(ConfigResponse config)
        throws PluginException;
    
    protected ServerResource getServer(ConfigResponse config) {
        ServerResource server = createServerResource("/");
        String fqdn = config.getValue(ProductPlugin.PROP_PLATFORM_FQDN);
        String type = config.getValue(ProductPlugin.PROP_PLATFORM_TYPE);
        server.setName(fqdn + " " + type);
        server.setIdentifier(server.getName());
        server.setProductConfig();
        server.setMeasurementConfig();
        //server.setControlConfig();        
        getLog().debug("PlatformServiceDetector created server=" + server.getName());
        return server;
    }
    
    /**
     * @return platformTypeName + " " + type
     */
    protected String getServiceTypeName(String type) {
        return getTypeInfo().getPlatformTypes()[0] + " " + type;
    }
    
    /**
     * @return ServiceResource with setType(getServiceTypeName(type))
     */
    protected ServiceResource createServiceResource(String type) {
        ServiceResource service = new ServiceResource();
        service.setType(getServiceTypeName(type));
        return service;
    }

    public List getServerResources(ConfigResponse config) {
        List servers = new ArrayList();
        servers.add(getServer(config));
        return servers;
    }
}
