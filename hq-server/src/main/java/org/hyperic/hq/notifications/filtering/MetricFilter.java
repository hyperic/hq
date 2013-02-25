package org.hyperic.hq.notifications.filtering;


import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.model.MetricNotification;

public class MetricFilter<C extends FilteringCondition<Measurement>> extends Filter<MetricNotification,C> {
    private static final long serialVersionUID = 8136231851084098091L;
    private MeasurementManager measurementManager;
    
    public MetricFilter() {
        super(null);
    }
    public MetricFilter(final MeasurementManager measurementManager,C cond) {
        super(cond);
        this.measurementManager=measurementManager;
    }

    protected MetricNotification filter(MetricNotification metricNotification) {
        Integer mid = metricNotification.getMeasurementId();
        Measurement msmt = this.measurementManager.getMeasurement(mid);
        if (this.cond.check(msmt)) {
            return metricNotification;
        }
        return null;
    }

    @Override
    protected Class<? extends MetricNotification> getHandledNotificationClass() {
        return MetricNotification.class;
    }

    @Override
    protected String initFilterType() {
        return "METRIC_FILTER";
    }
}
