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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.util.config.ConfigResponse;

public class AutoinventoryPluginManager extends PluginManager {

    private Map serviceConfigs = new HashMap();
    private Map notes = new HashMap();

    public AutoinventoryPluginManager() {
        super();
    }

    public AutoinventoryPluginManager(Properties props) {
        super(props);
    }

    public String getName() {
        return ProductPlugin.TYPE_AUTOINVENTORY;
    }
 
    public void addServiceConfig(String type,
                                 ConfigResponse config) {

        Map services = (Map)this.serviceConfigs.get(type);
        String name = config.getValue(ProductPlugin.PROP_RESOURCE_NAME);
        if (services == null) {
            services = new HashMap();
            this.serviceConfigs.put(type, services);
        }
        services.put(name, config);
        if (log.isDebugEnabled()) {
            log.debug("Add service config: " + type + "=" + config);
        }
    }

    public List getServiceConfigs(String type) {
        List registered = new ArrayList();
        Map services = (Map)this.serviceConfigs.get(type);
        if (services != null) {
            registered.addAll(services.values());
        }
        if (log.isDebugEnabled()) {
            log.debug("Get service configs " + type + "=" + services);
        }
        return registered;
    }
    
    /**
     * Table for plugins to share discovery data.
     * This table is cleared before each scan is run.
     */
    public Map getNotes() {
        return this.notes;
    }

    public void endScan() {
        this.serviceConfigs.clear();
        getNotes().clear();
        ServerDetector.clearSigarCache();
    }
}
