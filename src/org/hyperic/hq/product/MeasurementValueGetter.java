package org.hyperic.hq.product;

public interface MeasurementValueGetter {
    public MetricValue getValue(String name, Metric metric)
    throws PluginException, PluginNotFoundException,
           MetricNotFoundException, MetricUnreachableException;
}
