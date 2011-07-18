package org.hyperic.hq.plugin.gflog;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;

public class GFLogMeasurementPlugin extends SigarMeasurementPlugin {
    
    public MetricValue getValue(Metric metric) 
    throws PluginException,
    MetricNotFoundException,
    MetricUnreachableException {
        
        String domain = metric.getDomainName();
        
        if(domain.equals("gflog.avail")) {
            String attr = metric.getAttributeName();
            double avail;

            try {
                double val =
                    super.getValue(metric).getValue();
                avail = Metric.AVAIL_UP;

            } catch (MetricNotFoundException e) {
                //SigarFileNotFoundException
                avail = Metric.AVAIL_DOWN;
            }

            return new MetricValue(avail);

        }

        return super.getValue(metric);
    }
    

}
