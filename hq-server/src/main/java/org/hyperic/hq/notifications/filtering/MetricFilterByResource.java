package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.model.MetricNotification;

public class MetricFilterByResource<C extends ResourceFilteringCondition<Resource>> extends Filter<MetricNotification,C> {
    private MeasurementManager measurementManager;
    protected ResourceManager resourceManager;
    
    public MetricFilterByResource(final MeasurementManager measurementManager, ResourceManager resourceManager, C cond) {
        super(cond);
        this.measurementManager=measurementManager;
        this.resourceManager=resourceManager;
    }
    protected MetricNotification filter(MetricNotification metricNotification) {
        Integer mid = metricNotification.getMeasurementId();
        Measurement msmt = measurementManager.getMeasurement(mid);
        Resource rsc = resourceManager.getResourceById(msmt.getResource().getId());
        return (cond.check(rsc)) ? metricNotification : null;
    }
    @Override
    protected Class<? extends MetricNotification> getHandledNotificationClass() {
        return MetricNotification.class;
    }
}
