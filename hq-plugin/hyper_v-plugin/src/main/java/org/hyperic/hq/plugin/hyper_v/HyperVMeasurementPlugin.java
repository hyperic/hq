package org.hyperic.hq.plugin.hyper_v;

import java.util.Map;

import org.hyperic.hq.product.DetectionUtil;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {
    
    
    protected Map<String,String> getWMIObj(Metric metric) throws PluginException {
        return DetectionUtil.getWMIObj(metric.getObjectProperty("object"),metric.getAttributeName(),metric.getObjectProperty("column"),metric.getObjectName());
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        if ("wmic".equals(metric.getDomainName())) {
            Map<String,String> obj = getWMIObj(metric);
            String state = obj.get("EnabledState");
            if ("2".equals(state)) {
                return new MetricValue(Metric.AVAIL_UP);
            } else {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
        } else if ("pdh".equals(metric.getDomainName())) {
            String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();
            Double val;
            try {
                val = new Pdh().getFormattedValue(obj.replaceAll("%3A", ":"));
                return new MetricValue(val);
            }catch(Win32Exception e) {
                throw new PluginException(e);
            }
        } else {
            return super.getValue(metric);
        }
    }
}
