package org.hyperic.hq.api.transfer.mapping;
 
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.api.model.measurements.MetricFilterDefinition;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinition;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.filtering.Filter;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricFilter;
import org.hyperic.hq.notifications.filtering.MetricFilterByResource;
import org.hyperic.hq.notifications.filtering.ResourceFilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricFilteringCondition;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.MetricValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
@Component
public class MeasurementMapper {
    protected final static int MAX_FRACTION_DIGITS = 3;
    protected final static DecimalFormat df = new DecimalFormat();
    @Autowired
    protected MeasurementManager measurementMgr;
    @Autowired
    private ExceptionToErrorCodeMapper errorHandler ;
    
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
    public RawMetric toMetricWithId(final MetricNotification mn) {
        RawMetric metric = new RawMetric();
        MetricValue hqMetric = mn.getMetricVal();
        metric.setValue(Double.valueOf(df.format(hqMetric.getValue())));
        metric.setTimestamp(hqMetric.getTimestamp());
        metric.setResourceID(mn.getResourceID());
        metric.setMeasurementID(mn.getMeasurementId());
        metric.setMeasurementName(mn.getMeasurementName());
        metric.setMeasurementType(mn.getMeasurementType());
        metric.setCategory(mn.getCategory());
        metric.setUnits(mn.getUnits());
        return metric;
    }
    public List<RawMetric> toMetricsWithId(final List<MetricNotification> mns) {
        List<RawMetric> metrics = new ArrayList<RawMetric>();
        for (MetricNotification mn : mns) {
            metrics.add(toMetricWithId(mn));
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
    public MetricFilterByResource<ResourceFilteringCondition> toMetricFilterByResource(final ResourceFilterDefinition rscFilterDef) {
        if (rscFilterDef==null) {
            return null;
        }
        Set<Integer> resourceIds = rscFilterDef.getResourceIds();
        if (resourceIds==null) {
            return null;
        }
        ResourceFilteringCondition cond = new ResourceFilteringCondition(resourceIds);
        MetricFilterByResource<ResourceFilteringCondition> filter = new MetricFilterByResource<ResourceFilteringCondition>(this.measurementMgr,cond);
        return filter;
    }
    public MetricFilter<MetricFilteringCondition> toMetricFilter(final MetricFilterDefinition metricFilterDef) {
        if (metricFilterDef==null) {
            return null;
        }
        Boolean isIndicator = metricFilterDef.getIsIndicator();
        MetricFilteringCondition cond = new MetricFilteringCondition(isIndicator);
        MetricFilter<MetricFilteringCondition> filter = new MetricFilter<MetricFilteringCondition>(this.measurementMgr,cond);
        return filter;
    }
    public List<Filter<MetricNotification,? extends FilteringCondition<?>>> toMetricFilters(final MetricFilterRequest metricFilterReq) {
        List<Filter<MetricNotification,? extends FilteringCondition<?>>> userFilters = new ArrayList<Filter<MetricNotification,? extends FilteringCondition<?>>>();
        ResourceFilterDefinition rscFilterDef = metricFilterReq.getResourceFilterDefinition();
        MetricFilterByResource<ResourceFilteringCondition> metricFilterByRsc = toMetricFilterByResource(rscFilterDef);
        if (metricFilterByRsc!=null) {
            userFilters.add(metricFilterByRsc);
        }
        
        MetricFilterDefinition metricFilterDef = metricFilterReq.getMetricFilterDefinition();
        MetricFilter<? extends FilteringCondition<org.hyperic.hq.measurement.server.session.Measurement>> metricFilter = toMetricFilter(metricFilterDef);
        if (metricFilter!=null) {
            userFilters.add(metricFilter);
        }
        return userFilters;
    }
}
