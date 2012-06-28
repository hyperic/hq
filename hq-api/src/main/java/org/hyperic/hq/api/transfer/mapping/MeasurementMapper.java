package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.springframework.stereotype.Component;

@Component
public class MeasurementMapper {
    public Measurement toMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt) {
        Measurement msmt = new Measurement();
        msmt.setInterval(hqMsmt.getInterval());
        msmt.setName(hqMsmt.getTemplate().getName());
        return msmt;
    }

    public List<Metric> toMetrics(List<HighLowMetricValue> hqMetrics) {
        List<Metric> metrics = new ArrayList<Metric>();
        for (HighLowMetricValue hqMetric : hqMetrics) {
            Metric metric = new Metric();
            metric.setHighValue(hqMetric.getHighValue());
            metric.setLowValue(hqMetric.getLowValue());
            metric.setValue(hqMetric.getValue());
            metric.setTimestamp(hqMetric.getTimestamp());
            metrics.add(metric);
        }
        return metrics;
    }
}
