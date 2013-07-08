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


public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {
    private static Log log =LogFactory.getLog("HyperVMeasurementPlugin");
    
    protected Set<String> getWMIObj(Metric metric) throws PluginException {
        return DetectionUtil.getWMIObj(metric.getObjectProperty("object"),metric.getAttributeName(),metric.getObjectProperty("column"),metric.getObjectName());
    }
    
  

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        if ("wmic".equals(metric.getDomainName())) {
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
        } else {
            try {                
                MetricValue metricVal =  super.getValue(metric);
                return metricVal;
            }
            catch(MetricNotFoundException e) {
                // nira : very ugly-  assume that if pdh value is not found 
                // probably not found because instance is not available.
                //in case return empty value - so we won't get any errors in the ui
                return MetricValue.NONE;
            }
        }
    }
}
