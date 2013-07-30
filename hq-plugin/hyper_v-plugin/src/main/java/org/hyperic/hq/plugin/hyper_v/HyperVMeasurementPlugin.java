package org.hyperic.hq.plugin.hyper_v;


import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        if ("wmic".equals(metric.getDomainName())) {
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
        } else  if  (metric.isAvail()) {
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
        } else if ( ("pdh".equals(metric.getDomainName())) || ("pdh_formatted".equals(metric.getDomainName())) ) {
            String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();
            Double val;
            try 
            {          
                obj = obj.replaceAll("%3A", ":");
                if ( ("pdh".equals(metric.getDomainName()))) {
                    val = new Pdh().getRawValue(obj);
                }
                else {
                    val = new Pdh().getFormattedValue(obj);
                }
                
                if (metric.getAttributeName().startsWith("%")) {
                    // divide by 100 so percentage units will be displayed correctly
                    val = val/100.0;
                }
                return new MetricValue(val);
            }catch(Win32Exception e) {
                // ugly-  assume that if pdh value is not found 
                // probably not found because instance is not available.
                //in case return empty value - so we won't get any errors in the ui
                log.warn("got exception: metric="  + "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName()+ "  " +  e.getMessage());
                return MetricValue.NONE;
            }        
        } else {
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
    }
}
