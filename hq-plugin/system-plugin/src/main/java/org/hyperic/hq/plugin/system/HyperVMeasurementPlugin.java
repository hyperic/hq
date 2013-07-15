package org.hyperic.hq.plugin.system;


import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {   
    
    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        try 
        {
            MetricValue metricValue = null;
            if (metric.isAvail()) {
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
            else if ( ("pdh".equals(metric.getDomainName())) || ("pdh_formatted".equals(metric.getDomainName())) ) {
                String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();
                double val;                
                obj = obj.replaceAll("%3A", ":");
                getLog().debug("metric=" + obj);
                if ( ("pdh".equals(metric.getDomainName()))) {
                    val = new Pdh().getRawValue(obj);
                }
                else {
                    val = new Pdh().getFormattedValue(obj);
                }
                if (metric.getAttributeName().startsWith("%")) {
                    // want t use units=percentage (so graph will have units 1-100
                    val = val/100.0;
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
}
