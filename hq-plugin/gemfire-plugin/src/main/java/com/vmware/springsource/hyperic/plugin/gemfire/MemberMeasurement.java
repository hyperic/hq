package com.vmware.springsource.hyperic.plugin.gemfire;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.jmx.MxMeasurementPlugin;


public class MemberMeasurement extends MxMeasurementPlugin {

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        return Collector.getValue(this, metric);
    }
}
