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


import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {
    
    protected static String getPath(Metric metric) {
        String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();                
        obj = obj.replaceAll("%3A", ":");        
        return obj;
    }
    
    private MetricValue getAvailability(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        // availabilty is on if instance exist
        String[] instances;
        try {
            instances = Pdh.getInstances(metric.getDomainName());                
            String serviceName = metric.getObjectPropString();
            serviceName = serviceName.replaceAll("%3A", ":");
            getLog().debug("avail of: " + metric.getDomainName() + " service=" + serviceName);
            for (String instance:instances) {
                getLog().debug("avail: instance=" + instance);
                if (serviceName.equals(instance)) {
                    getLog().debug("avail is up for:" + metric.getDomainName() +  ":" + serviceName);
                    return new MetricValue(Metric.AVAIL_UP);                        
                }
            }
            return new MetricValue(Metric.AVAIL_DOWN);
        }catch(Win32Exception e) {
            // TODO Auto-generated catch block
            getLog().warn("avail is down for:" + metric.getDomainName()  + e.getMessage());
            return new MetricValue(Metric.AVAIL_DOWN);
        }

    }
    
    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        try 
        {
            MetricValue metricValue = null;
            if (metric.isAvail()) {
                return getAvailability(metric);
            }            
            String   metricDomain =  metric.getDomainName();
            getLog().debug("getValue: domain = "  + metricDomain + " path=" + getPath(metric));
            if ("collector".equals(metricDomain)) {
                return Collector.getValue(this, metric);
            }
            
            if ( ("pdh".equals(metricDomain)) || ("pdh_formatted".equals(metricDomain)) ) {
                String obj = getPath(metric);
                getLog().debug("metric=" + obj + " domain=" + metricDomain);
                double val;                
                if ( ("pdh".equals(metric.getDomainName()))) {
                    val = new Pdh().getRawValue(obj);
                }
                else {
                    val = new Pdh().getFormattedValue(obj);
                }
                metricValue = new MetricValue(val);
            }
            else {
                 metricValue =   super.getValue(metric);
            }
            
            return metricValue;
         }catch(Win32Exception e) { 
             getLog().warn("failed to get hyper-v metric:" +  metric.getObjectPropString() + "\\" + metric.getAttributeName() + " " + e.getMessage());
             return MetricValue.NONE;
         } 
    }
    
    @Override
    public Collector getNewCollector() {
        getLog().debug("[---------------]" + getTypeInfo().getName());
        if (getPluginData().getPlugin(TYPE_COLLECTOR, getTypeInfo().getName()) == null) {            
            getPluginData().addPlugin(TYPE_COLLECTOR, SystemPlugin.HYPERV_LOGICAL_PROCESSOR, HypervCollector.class.getName());
        }
        return super.getNewCollector();
    }

}
