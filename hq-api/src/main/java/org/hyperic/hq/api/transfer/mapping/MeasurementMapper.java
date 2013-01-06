package org.hyperic.hq.api.transfer.mapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinitioin;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricFilter;
import org.hyperic.hq.notifications.filtering.MetricFilterByResource;
import org.hyperic.hq.notifications.filtering.ResourceFilteringCondition;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.MetricValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
@Component
public class MeasurementMapper {
    protected final static int MAX_FRACTION_DIGITS = 3;
    protected final static DecimalFormat df = new DecimalFormat();
    protected final ResourceManager resourceMgr;
    protected final MeasurementManager measurementMgr;
    
    @Autowired
    public MeasurementMapper(final MeasurementManager measurementMgr,ResourceManager resourceMgr) {
        this.measurementMgr=measurementMgr;
        this.resourceMgr=resourceMgr;
    }
    static {
        df.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
    }
    public List<Integer> toIds(final List<ID> ids) {
        List<Integer> ints = new ArrayList<Integer>(ids.size());
        for(ID id:ids) {
            ints.add(id.getId());
        }
        return ints;
    }
    public Measurement toMeasurement(final org.hyperic.hq.measurement.server.session.Measurement hqMsmt) {
        Measurement msmt = new Measurement();
        msmt.setInterval(hqMsmt.getInterval());
        msmt.setAlias(hqMsmt.getTemplate().getAlias());
        msmt.setName(hqMsmt.getTemplate().getName());
        return msmt;
    }
    public Measurement toMeasurementExtendedData(final org.hyperic.hq.measurement.server.session.Measurement hqMsmt, MeasurementTemplate templ) {
        Measurement msmt = toMeasurement(hqMsmt);
        msmt.setIndicator(templ.isDesignate());
        msmt.setEnabled(hqMsmt.isEnabled());
        msmt.setId(hqMsmt.getId());
        return msmt;
    }
    public Measurement toMeasurement(final org.hyperic.hq.measurement.server.session.Measurement hqMsmt, double avg) {
        Measurement msmt = toMeasurement(hqMsmt);
        msmt.setAverage(avg);
        return msmt;
    }
    public List<RawMetric> toMetricsWithId(final List<MetricNotification> mns) {
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
    public List<Metric> toMetrics(final List<HighLowMetricValue> hqMetrics) {
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
    public MetricFilterByResource<ResourceFilteringCondition<Resource>> toMetricFilterByResource(final ResourceFilterDefinitioin rscFilterDef) {
        String nameToCompareTo = rscFilterDef.getName();
        ResourceFilteringCondition<Resource> cond = new ResourceFilteringCondition<Resource>(nameToCompareTo);
        MetricFilterByResource<ResourceFilteringCondition<Resource>> filter = new MetricFilterByResource<ResourceFilteringCondition<Resource>>(this.measurementMgr,this.resourceMgr,cond);
        return filter;
    }
    public List<Filter<MetricNotification,? extends FilteringCondition<?>>> toMetricFilters(final MetricFilterRequest metricFilterReq) {
        List<Filter<MetricNotification,? extends FilteringCondition<?>>> userFilters = new ArrayList<Filter<MetricNotification,? extends FilteringCondition<?>>>();
        ResourceFilterDefinitioin rscFilterDef = metricFilterReq.getResourceFilterDefinition();

        MetricFilterByResource<ResourceFilteringCondition<Resource>> metricFilterByRsc = toMetricFilterByResource(rscFilterDef);
        if (metricFilterByRsc!=null) {
            userFilters.add(metricFilterByRsc);
        }
        //TODO~ marshal metric filter
        MetricFilter<? extends FilteringCondition<org.hyperic.hq.measurement.server.session.Measurement>> metricFilter = null;
        if (metricFilter!=null) {
            userFilters.add(metricFilter);
        }        
        return userFilters;
    }
}
