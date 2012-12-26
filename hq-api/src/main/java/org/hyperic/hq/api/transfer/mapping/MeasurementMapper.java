package org.hyperic.hq.api.transfer.mapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.springframework.stereotype.Component;

@Component
public class MeasurementMapper {
    protected final static int MAX_FRACTION_DIGITS = 3;
    protected final static DecimalFormat df = new DecimalFormat();
    
    static {
        df.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
    }
    
    
    
    public Measurement toMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt) {
        Measurement msmt = new Measurement();
        msmt.setInterval(hqMsmt.getInterval());
        msmt.setAlias(hqMsmt.getTemplate().getAlias());
        msmt.setName(hqMsmt.getTemplate().getName());
        return msmt;
    }

    public Measurement toMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt, double avg) {
        Measurement msmt = toMeasurement(hqMsmt);
        msmt.setAverage(avg);
        return msmt;
    }

    public List<Metric> toMetrics(List<HighLowMetricValue> hqMetrics) {
        List<Metric> metrics = new ArrayList<Metric>();
        for (HighLowMetricValue hqMetric : hqMetrics) {
            Metric metric = new Metric();
            metric.setHighValue(Double.valueOf(df.format(hqMetric.getHighValue())));
            metric.setLowValue(Double.valueOf(df.format(hqMetric.getLowValue())));
            metric.setValue(Double.valueOf(df.format(hqMetric.getValue())));
            metric.setTimestamp(hqMetric.getTimestamp());
            metrics.add(metric);
        }
        return metrics;
    }
}
