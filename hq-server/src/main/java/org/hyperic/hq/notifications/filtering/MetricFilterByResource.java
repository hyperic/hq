package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.model.MetricNotification;

public class MetricFilterByResource<C extends ResourceFilteringCondition<Resource>> extends Filter<MetricNotification,C> {
    private MeasurementManager measurementManager;
    
    public MetricFilterByResource(final MeasurementManager measurementManager,C cond) {
        super(cond);
        this.measurementManager=measurementManager;
    }
    protected MetricNotification filter(MetricNotification metricNotification) {
        Integer mid = metricNotification.getMeasurementId();
        Measurement msmt = this.measurementManager.getMeasurement(mid);
        Resource rsc = msmt.getResource();
        if (this.cond.check(rsc)) {
            return metricNotification;
        }
        return null;
    }
}
