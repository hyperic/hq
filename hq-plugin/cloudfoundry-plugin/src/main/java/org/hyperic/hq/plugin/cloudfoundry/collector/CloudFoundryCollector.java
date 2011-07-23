/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 * 
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.cloudfoundry.collector;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryProxy;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;

import org.cloudfoundry.client.lib.CloudInfo;

public class CloudFoundryCollector extends Collector {

    private static final Log _log = LogFactory.getLog(CloudFoundryCollector.class);

    @Override
    protected void init() throws PluginException {
        Properties props = getProperties();
        
        if (_log.isDebugEnabled()) {
        	_log.debug("[init] props=" + props);
        }
        
        // validate configuration
        CloudFoundryProxy cf = new CloudFoundryProxy(props);

        super.init();
    }
    
    @Override
    public void collect() {
        try {
            CloudFoundryProxy cf = new CloudFoundryProxy(getProperties());
        	CloudInfo info = cf.getCloudInfo();
            
            setAvailability(true);
                    	
        	long memory = (long) info.getUsage().getTotalMemory();
        	double maxMemory = (double) info.getLimits().getMaxTotalMemory();
        	double percentMemory = memory/maxMemory;
        	// FIXME: the cloud foundry api returns the memory value in MB
        	// so need to convert to the proper units
            setValue("MemoryUsed", memory * 1024 * 1024);
            setValue("PercentMemoryUsed", percentMemory);

        	int apps = info.getUsage().getApps();            	
        	double maxApps = (double) info.getLimits().getMaxApps();
        	double percentApps = apps/maxApps;
            setValue("AppsUsed", apps);
            setValue("PercentAppsUsed", percentApps);
            	
        	int services = info.getUsage().getServices();
        	double maxServices = (double) info.getLimits().getMaxServices();
        	double percentServices = services/maxServices;
            setValue("ServicesUsed", services);
            setValue("PercentServicesUsed", percentServices);
            
        } catch (Exception ex) {
            setAvailability(false);
            _log.debug("[collect] "+ex.getMessage(), ex);
        }
    }
}
