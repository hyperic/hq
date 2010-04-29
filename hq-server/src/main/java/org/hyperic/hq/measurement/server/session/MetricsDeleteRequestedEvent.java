package org.hyperic.hq.measurement.server.session;

import java.util.Collection;

import org.springframework.context.ApplicationEvent;

/**
 * Sent before metrics are deleted.
 * @author jhickey
 */
public class MetricsDeleteRequestedEvent extends ApplicationEvent {

    public MetricsDeleteRequestedEvent(Collection<Integer> metricIds) {
        super(metricIds);
    }
    
    public Collection<Integer> getMetricIds() {
        return (Collection<Integer>) getSource();
    }

}
