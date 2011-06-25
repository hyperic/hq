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
import org.hyperic.hq.plugin.cloudfoundry.util.CloudFoundryFactory;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import org.cloudfoundry.client.lib.ApplicationStats;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudInfo;
import org.cloudfoundry.client.lib.InstanceStats;

public class CloudFoundryMeasurementPlugin extends MeasurementPlugin {

    private static final Log _log = LogFactory.getLog(CloudFoundryMeasurementPlugin.class);

    @Override
    public MetricValue getValue(Metric metric)
    throws PluginException, MetricNotFoundException, MetricUnreachableException {
        try {
        	String metricName = metric.getAttributeName();            
            CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(metric.getObjectProperties());
            
            if (cf == null) {
                if (metric.isAvail()) {
                	return new MetricValue(Metric.AVAIL_DOWN);                	
                }
                
                // TODO: update message
                throw new MetricUnreachableException("Cannot validate connection");
            }
                        
            if (metric.isAvail()) {
            	return new MetricValue(Metric.AVAIL_UP);
            } else if (metricName.equals("MemoryUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	long memory = (long) info.getUsage().getTotalMemory();
            	// FIXME: the cloud foundry api returns the value in MB
            	// so need to convert to the proper units
            	memory = memory * 1024 * 1024;
            	return new MetricValue(memory);
            } else if (metricName.equals("PercentMemoryUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	int memory = info.getUsage().getTotalMemory();
            	double max = (double) info.getLimits().getMaxTotalMemory();
            	double percent = memory/max;
            	return new MetricValue(percent);            	
            } else if (metricName.equals("AppsUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	int apps = info.getUsage().getApps();
            	return new MetricValue(Integer.valueOf(apps));
            } else if (metricName.equals("PercentAppsUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	int apps = info.getUsage().getApps();
            	double max = (double) info.getLimits().getMaxApps();
            	double percent = apps/max;
            	return new MetricValue(percent);
            } else if (metricName.equals("ServicesUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	int services = info.getUsage().getServices();
            	return new MetricValue(Integer.valueOf(services));
            } else if (metricName.equals("PercentServicesUsed")) {
            	CloudInfo info = cf.getCloudInfo();
            	int services = info.getUsage().getServices();
            	double max = (double) info.getLimits().getMaxServices();
            	double percent = services/max;
            	return new MetricValue(percent);
            }
        } catch (PluginException e) {
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
