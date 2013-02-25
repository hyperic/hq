package org.hyperic.hq.notifications.filtering;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.model.MetricNotification;

public class MetricFilterByResource<C extends ResourceFilteringCondition<Resource>> extends Filter<MetricNotification,C> {
    private static final long serialVersionUID = 4732695338821377227L;
    private MeasurementManager measurementManager;
    protected ResourceManager resourceManager;
    
    public MetricFilterByResource() {
        super(null);
    }
    public MetricFilterByResource(final MeasurementManager measurementManager, ResourceManager resourceManager, C cond) {
        super(cond);
        this.measurementManager=measurementManager;
        this.resourceManager=resourceManager;
    }
    protected MetricNotification filter(MetricNotification metricNotification) {
        Integer mid = metricNotification.getMeasurementId();
        Measurement msmt = this.measurementManager.getMeasurement(mid);
        Resource rsc = this.resourceManager.getResourceById(msmt.getResource().getId());
        if (this.cond.check(rsc)) {
            return metricNotification;
        }
        return null;
    }
    @Override
    protected Class<? extends MetricNotification> getHandledNotificationClass() {
        return MetricNotification.class;
    }
//    @Override
//    protected String initFilterType() {
//        return "METRIC_FILTER_BY_RESOURCE";
//    }
}
