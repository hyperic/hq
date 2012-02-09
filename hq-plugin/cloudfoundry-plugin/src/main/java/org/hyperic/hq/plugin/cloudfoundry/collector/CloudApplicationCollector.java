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

import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryProxy;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.InstanceStats;

public class CloudApplicationCollector extends Collector {

    private static final Log _log = LogFactory.getLog(CloudApplicationCollector.class);

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
            Properties props = getProperties();
            String appName = props.getProperty("app");

            CloudFoundryProxy cf = new CloudFoundryProxy(props);            
        	CloudApplication app = cf.getApplication(appName);
        	
        	if (app == null) {
        		setAvailability(false);        		
        	} else {
        		setAvailability(app);

            	double totalMemory = 0;
            	double percentMemory = 0;
            	double totalDisk = 0;
            	double percentDisk = 0;
            	double totalCpu = 0;
            	double maxUptime = 0;

            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		InstanceStats.Usage usage = stat.getUsage();
            		
            		if (usage != null) {
	            		double memory = usage.getMem();
	            		long memoryQuota = stat.getMemQuota();            		
	            		double disk = usage.getDisk();
	            		double diskQuota = (double) stat.getDiskQuota();
	            		double cpu = usage.getCpu() / 100d;
	
	            		// FIXME: the cloud foundry api is returning the value
	                	// in KB, so need to convert to the proper units
	                	memory = memory * 1024;
	            		totalMemory += memory;
	            		percentMemory += memory/memoryQuota;
	            		totalDisk += disk;
	            		percentDisk += disk/diskQuota;
	            		totalCpu += cpu;
            		}
            		
            		if (stat.getUptime() > maxUptime) {
            			maxUptime = stat.getUptime();
            		}
            	}
            	if (!records.isEmpty()) {
            		int recordSize = records.size();
            		totalMemory = totalMemory/recordSize;
            		percentMemory = percentMemory/recordSize;
            		totalDisk = totalDisk/recordSize;
            		percentDisk = percentDisk/recordSize;
            		totalCpu = totalCpu/recordSize;
            	}
                setValue("AverageMemoryUsed", totalMemory);
                setValue("PercentAverageMemoryUsed", percentMemory);             
                setValue("AverageDiskUsage", totalDisk);
                setValue("PercentAverageDiskUsage", percentDisk);                	
                setValue("AverageCPUUsage", totalCpu);
                setValue("MaxUptime", maxUptime);                
                setValue("TotalInstances", app.getInstances());
                setValue("RunningInstances", app.getRunningInstances());
        	}           
        } catch (Exception ex) {
            setAvailability(false);
            _log.debug("[collect] "+ex.getMessage(), ex);
        }
    }
    
    private void setAvailability(CloudApplication app) {
		double avail;
    	String appState = app.getState().toString();
    	if (appState.equals("STARTED") || appState.equals("UPDATING")) {
    		avail = Metric.AVAIL_UP;
    	} else if (appState.equals("STOPPED")) {
    		avail = Metric.AVAIL_PAUSED;
    	} else {
    		avail = Metric.AVAIL_DOWN;
    	}
    	
    	if (_log.isDebugEnabled()) {
    		_log.debug("appName=" + app.getName() 
    					+ ", appState=" + app.getState()
    					+ ", avail=" + avail);
    	}
    	
        setValue(Metric.ATTR_AVAIL, avail);    	
    }
}
