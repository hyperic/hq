package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.filtering.DestinationEvaluator;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("OutgoingMetricZeventListener")
public class OutgoingMetricZeventListener extends BaseNotificationsZeventListener<MeasurementZevent,MetricNotification> {
    @Autowired
    MetricDestinationEvaluator evaluator;
    @Autowired
    protected MeasurementManager msmtMgr;

    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(MeasurementZevent.class, (ZeventListener<MeasurementZevent>) Bootstrap.getBean(getListenersBeanName()));
        concurrentStatsCollector.register(getConcurrentStatsCollectorType());
    }    
    public List<MetricNotification> extract(List<MeasurementZevent> events) {
        List<MetricNotification> ns = new ArrayList<MetricNotification>();
        for(MeasurementZevent measurementZevent:events) {
            MeasurementZeventSource zEventSource = (MeasurementZeventSource) measurementZevent.getSourceId(); 
            MeasurementZeventPayload zEventPayload = (MeasurementZeventPayload) measurementZevent.getPayload();
            int measurementId = 0;
            if (zEventSource!=null) {
                measurementId = zEventSource.getId();
            }
            MetricValue metricVal = null;
            if (zEventPayload!=null) {
                metricVal = zEventPayload.getValue();
            }
            Integer mid = Integer.valueOf(measurementId);
            Measurement msmt = this.msmtMgr.getMeasurement(mid);
            if (msmt==null) { // the measurement was erased since the event was sent
                continue;
            }
            MeasurementTemplate tmpl = msmt.getTemplate();
            String msmtType = tmpl.getAlias().equals(Metric.ATTR_AVAIL)?Metric.ATTR_AVAIL:null;
            Category cat = tmpl.getCategory();
            String catName = null;
            if (cat!=null) {
                catName=cat.getName();
            }
            Resource rsc = msmt.getResource();
            MetricNotification n = new MetricNotification(rsc.getId(),mid,tmpl.getName(),msmtType,catName,tmpl.getUnits(),metricVal);
            ns.add(n);
        }
        return ns;
    } 
    
    @Override
    public String getListenersBeanName() {
        return "OutgoingMetricZeventListener";
    }
    @Override
    public String getConcurrentStatsCollectorType() {
        return ConcurrentStatsCollector.METRIC_NOTIFICATION_FILTERING_TIME;
    }
    @Override
    public DestinationEvaluator<MetricNotification> getEvaluator() {
        return this.evaluator;
    }
}
