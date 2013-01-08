/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.api.transfer.mapping;
 
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.measurements.Measurement;
import org.hyperic.hq.api.model.measurements.Metric;
import org.hyperic.hq.api.model.measurements.MetricFilterDefinition;
import org.hyperic.hq.api.model.measurements.MetricFilterRequest;
import org.hyperic.hq.api.model.measurements.RawMetric;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinitioin;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.server.session.DataPoint;
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
        msmt.setAvg(avg);
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
    public MetricFilter<MetricFilteringCondition> toMetricFilter(final MetricFilterDefinition metricFilterDef) {
        Boolean isIndicator = metricFilterDef.getIsIndicator();
        MetricFilteringCondition cond = new MetricFilteringCondition(isIndicator);
        MetricFilter<MetricFilteringCondition> filter = new MetricFilter<MetricFilteringCondition>(this.measurementMgr,cond);
        return filter;
    }
    public List<Filter<MetricNotification,? extends FilteringCondition<?>>> toMetricFilters(final MetricFilterRequest metricFilterReq) {
        List<Filter<MetricNotification,? extends FilteringCondition<?>>> userFilters = new ArrayList<Filter<MetricNotification,? extends FilteringCondition<?>>>();
        ResourceFilterDefinitioin rscFilterDef = metricFilterReq.getResourceFilterDefinition();

        MetricFilterByResource<ResourceFilteringCondition<Resource>> metricFilterByRsc = toMetricFilterByResource(rscFilterDef);
        if (metricFilterByRsc!=null) {
            userFilters.add(metricFilterByRsc);
        }
        MetricFilterDefinition metricFilterDef = metricFilterReq.getMetricFilterDefinition();
        //TODO~ marshal metric filter
        MetricFilter<? extends FilteringCondition<org.hyperic.hq.measurement.server.session.Measurement>> metricFilter = toMetricFilter(metricFilterDef);
        if (metricFilter!=null) {
            userFilters.add(metricFilter);
        }        
        return userFilters;
    }
}
