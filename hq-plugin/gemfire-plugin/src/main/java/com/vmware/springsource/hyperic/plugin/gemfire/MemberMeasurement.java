package com.vmware.springsource.hyperic.plugin.gemfire;

import org.apache.commons.logging.Log;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class MemberMeasurement extends MeasurementPlugin {

    public MetricValue getValue(Metric metric)
            throws PluginException, MetricNotFoundException, MetricUnreachableException {
        getLog().debug("[getValue] metric=" + metric);
        return new MetricValue((metric.isAvail()) ? 1.0D : 0.0D);
    }
}
