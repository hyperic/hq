package org.hyperic.hq.api.transfer.mapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.api.model.measurements.MetricGroup;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.MetricValue;
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
    public List<Integer> toIds(List<ID> ids) {
        List<Integer> ints = new ArrayList<Integer>(ids.size());
        for(ID id:ids) {
            ints.add(id.getId());
        }
        return ints;
    }
    public Measurement toMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt) {
        Measurement msmt = new Measurement();
        msmt.setInterval(hqMsmt.getInterval());
        msmt.setAlias(hqMsmt.getTemplate().getAlias());
        msmt.setName(hqMsmt.getTemplate().getName());
        return msmt;
    }
    public Measurement toMeasurementExtendedData(org.hyperic.hq.measurement.server.session.Measurement hqMsmt) {
        Measurement msmt = toMeasurement(hqMsmt);
        msmt.setId(hqMsmt.getId());
        return msmt;
    }
//    public MetricGroup toMetricGroup(org.hyperic.hq.measurement.server.session.Measurement msmt) {
//        Measurement metricGrp = new Measurement();
//        Integer msmtId = msmt.getId();
//        metricGrp.setId(msmtId);
//        MeasurementTemplate tmpl = msmt.getTemplate();
//        metricGrp.setName(tmpl.getName());
//        metricGrp.setAlias(tmpl.getAlias());
//        return metricGrp;
//    }
    public Measurement toMeasurement(org.hyperic.hq.measurement.server.session.Measurement hqMsmt, double avg) {
        Measurement msmt = toMeasurement(hqMsmt);
        msmt.setAvg(avg);
        return msmt;
    }
    public List<RawMetric> toMetricsWithId(List<MetricNotification> mns) {
        List<RawMetric> metrics = new ArrayList<RawMetric>();
        for (MetricNotification mn : mns) {
            RawMetric metric = new RawMetric();
            MetricValue hqMetric = mn.getMetricVal();
            metric.setValue(Double.valueOf(df.format(hqMetric.getValue())));
            metric.setTimestamp(hqMetric.getTimestamp());
            metric.setMeasurementId(mn.getMeasurementId());
            metrics.add(metric);
        }
        return metrics;
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
