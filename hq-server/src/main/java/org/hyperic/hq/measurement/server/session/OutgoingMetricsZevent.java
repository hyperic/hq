package org.hyperic.hq.measurement.server.session;


import org.hyperic.hq.product.MetricValue;

public class OutgoingMetricsZevent extends MeasurementZevent {

    public OutgoingMetricsZevent(int measId, MetricValue val) {
        super(measId, val);
    }
}
