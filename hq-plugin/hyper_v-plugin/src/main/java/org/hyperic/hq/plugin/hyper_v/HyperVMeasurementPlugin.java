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
package org.hyperic.hq.plugin.hyper_v;


import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

import java.util.Collections;


public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {
    private static Log log =LogFactory.getLog(HyperVMeasurementPlugin.class);
    
    protected Set<String> getWMIObj(Metric metric) throws PluginException {
        return DetectionUtil.getWMIObj(metric.getObjectProperty("namespace"),metric.getObjectProperty("object"),Collections.singletonMap(metric.getAttributeName(),"="),metric.getObjectProperty("column"),metric.getObjectName());
    }
    
  

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        String metricDomain = metric.getDomainName();
        if ("wmic".equals(metricDomain)) {
            // availability of vm
            Set<String> obj = getWMIObj(metric);
            String state = null;
            if (!obj.isEmpty()){
                state = obj.iterator().next();
            }
            if ("2".equals(state)) {
                return new MetricValue(Metric.AVAIL_UP);
            } else {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
        } 
        if  (metric.isAvail()) {
            // availability of vm service
            // availability is on if instance exists (in performance monitor)
           return getAvailMetric(metric);
        }
        
        if ( ("pdh".equals(metricDomain)) || ("pdh_formatted".equals(metricDomain)) ) {
           return getPdhMetric(metric);
        }
        
        if ("collector".equals(metricDomain)) {
            return Collector.getValue(this, metric);
        }
        try {                
           MetricValue metricVal =  super.getValue(metric);
           return metricVal;
        }
        catch(MetricNotFoundException e) {
                // ugly-  assume that if pdh value is not found 
                // probably not found because instance is not available.
                //in case return empty value - so we won't get any errors in the ui
                return MetricValue.NONE;
        }        
    }
    
    private MetricValue getAvailMetric(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        // availability of vm service
        // availability is on if instance exists (in performance monitor)
        String[] instances;
        try {
            instances = Pdh.getInstances(metric.getDomainName());                
            String serviceName = metric.getObjectPropString();
            serviceName = serviceName.replaceAll("%3A", ":");
            log.debug("avail of: " + metric.getDomainName() + " service=" + serviceName);
            for (String instance:instances) {
                log.debug("avail: instance=" + instance);
                if (serviceName.equals(instance)) {
                    log.debug("avail is up for:" + metric.getDomainName() +  ":" + serviceName);
                    return new MetricValue(Metric.AVAIL_UP);                        
                }
            }
            return new MetricValue(Metric.AVAIL_DOWN);
        }catch(Win32Exception e) {
            // TODO Auto-generated catch block
            log.debug("avail is down for:" + metric.getDomainName()  + e.getMessage());
            return new MetricValue(Metric.AVAIL_DOWN);
        }
    }
    
    protected static String getPath(Metric metric) {
        
        String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();        
        obj = obj.replaceAll("%3A", ":");
        return obj;
    }
   
    private MetricValue getPdhMetric(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        String obj = getPath(metric);
        Double val;
        try 
        {   
            if ( ("pdh".equals(metric.getDomainName()))) {
                val = new Pdh().getRawValue(obj);
            }
            else {
                val = new Pdh().getFormattedValue(obj);
            }
            
            return new MetricValue(val);
        }catch(Win32Exception e) {
            // ugly-  assume that if pdh value is not found 
            // probably not found because instance is not available.
            //in case return empty value - so we won't get any errors in the ui
            log.warn("got exception: metric="  + "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName()+ "  " +  e.getMessage());
            return MetricValue.NONE;
        }        
    }
}
