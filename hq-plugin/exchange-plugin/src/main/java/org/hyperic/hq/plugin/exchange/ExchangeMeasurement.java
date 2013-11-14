package org.hyperic.hq.plugin.exchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;

public class ExchangeMeasurement extends Win32MeasurementPlugin {

    private static Log log = LogFactory.getLog(ExchangeMeasurement.class);

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        log.debug("[getValue] metric="+metric);
        if (metric.getDomainName().equalsIgnoreCase("collector")) {
            return Collector.getValue(this, metric);
        } else {
            return super.getValue(metric);
        }
    }
}
