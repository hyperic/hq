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
package org.hyperic.hq.plugin.cloudfoundry.measurement;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryProxy;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.InstanceStats;

public class CloudApplicationMeasurementPlugin extends MeasurementPlugin {

    private static final Log _log = LogFactory.getLog(CloudApplicationMeasurementPlugin.class);

    @Override
    public MetricValue getValue(Metric metric)
    throws PluginException, MetricNotFoundException, MetricUnreachableException {
        try {
        	String metricName = metric.getAttributeName();
            CloudFoundryProxy cf = null;
            
            try {
            	cf = new CloudFoundryProxy(metric.getObjectProperties());
            } catch (Exception e) {
                if (metric.isAvail()) {
                	return new MetricValue(Metric.AVAIL_DOWN);                	
                }
                // TODO: update message
                throw new MetricUnreachableException("Cannot validate connection");
            }
            
            String appName = metric.getObjectProperties().getProperty("app");
            
            if (metric.isAvail()) {
            	try {
	            	CloudApplication app = cf.getApplication(appName);
	            	String appState = app.getState().toString();
	            	_log.debug("appName=" + appName + ", appState=" + app.getState());
	            	if (appState.equals("STARTED") || appState.equals("UPDATING")) {
	            		return new MetricValue(Metric.AVAIL_UP);
	            	} else if (appState.equals("STOPPED")) {
	            		return new MetricValue(Metric.AVAIL_PAUSED);
	            	} else {
	            		return new MetricValue(Metric.AVAIL_DOWN);
	            	}
            	} catch (CloudFoundryException e) {
            		_log.debug("Error getting app state: " + e.getMessage(), e);
            		return new MetricValue(Metric.AVAIL_DOWN);            		
            	}
            } else if (metricName.equals("AverageMemoryUsed")) {
            	double memory = 0;
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		memory += stat.getUsage().getMem();
            	}
            	// FIXME: the cloud foundry api is returning the value
            	// in KB, so need to convert to the proper units
            	memory = memory * 1024;
            	if (!records.isEmpty()) {
            		memory = memory/records.size();
            	}
            	return new MetricValue(memory);
            } else if (metricName.equals("PercentAverageMemoryUsed")) {
            	double percent = 0;
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		double memory = stat.getUsage().getMem();
                	// FIXME: the cloud foundry api is returning the value
                	// in KB, so need to convert to the proper units
                	memory = memory * 1024;                	
            		long quota = stat.getMemQuota();            		
            		percent += memory/quota;
            	}
            	if (!records.isEmpty()) {
            		percent = percent/records.size();
            	}            	
            	return new MetricValue(percent);
            } else if (metricName.equals("AverageDiskUsage")) {
            	double disk = 0;
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		disk += stat.getUsage().getDisk();
            	}            	
            	if (!records.isEmpty()) {
            		disk = disk/records.size();
            	}  
            	return new MetricValue(disk);
            } else if (metricName.equals("PercentAverageDiskUsage")) {
            	double percent = 0;
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		int disk = stat.getUsage().getDisk();
            		double quota = (double) stat.getDiskQuota();
            		percent += disk/quota;
            	}
            	if (!records.isEmpty()) {
            		percent = percent/records.size();
            	}
            	return new MetricValue(percent);            	
            } else if (metricName.equals("AverageCPUUsage")) {
            	double cpu = 0;
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	for (InstanceStats stat : records) {
            		cpu += stat.getUsage().getCpu() / 100d;
            	}            	
            	if (!records.isEmpty()) {
            		cpu = cpu/records.size();
            	}
            	return new MetricValue(cpu);
            } else if (metricName.equals("MaxUptime")) {
            	ApplicationStats stats = cf.getApplicationStats(appName);
            	List<InstanceStats> records = stats.getRecords();
            	double maxUptime = 0;
            	for (InstanceStats stat : records) {
            		if (stat.getUptime() > maxUptime) {
            			maxUptime = stat.getUptime();
            		}
            	}            	
            	return new MetricValue(maxUptime);            	
            } else if (metricName.equals("TotalInstances")) {
            	CloudApplication app = cf.getApplication(appName);
            	int count = app.getInstances();
            	return new MetricValue(Integer.valueOf(count));            	
            } else if (metricName.equals("RunningInstances")) {
            	CloudApplication app = cf.getApplication(appName);
            	int count = app.getRunningInstances();
            	return new MetricValue(Integer.valueOf(count));
            }
        } catch (Exception e) {
            if (metric.isAvail()) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            throw new MetricUnreachableException(e.getMessage(),e);
        } finally {
            //
        }
        return null;
    }
}
