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

import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudServiceMeasurementPlugin extends MeasurementPlugin {

    private static final Log _log = LogFactory.getLog(CloudServiceMeasurementPlugin.class);

    @Override
    public MetricValue getValue(Metric metric)
    throws PluginException, MetricNotFoundException, MetricUnreachableException {
        try {
        	String metricName = metric.getAttributeName();        	        	        	
            final long start = System.currentTimeMillis();
            
            CloudFoundryClient cf = CloudFoundryFactory.getCloudFoundryClient(metric.getObjectProperties());
            
            if (cf == null) {
                if (metric.isAvail()) {
                	return new MetricValue(Metric.AVAIL_DOWN);                	
                }
                
                throw new MetricUnreachableException("Cannot validate connection");
            }
            
            String serviceName = metric.getObjectProperties().getProperty("service");
        	CloudService cs = cf.getService(serviceName);

            if (metric.isAvail()) {
            	if (cs == null) {
            		return new MetricValue(Metric.AVAIL_DOWN);
            	} else {
            		return new MetricValue(Metric.AVAIL_UP);
            	}
            } else if (metricName.equals("Uptime")) {
            	if (cs == null) {
            		throw new MetricUnreachableException("No service found with name=" + serviceName);
            	}
            	long now = System.currentTimeMillis();
            	long uptime = (now - cs.getMeta().getCreated().getTime())/1000;
            	
            	return new MetricValue(uptime);
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
