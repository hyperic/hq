package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.notifications.filtering.FilteringCondition;
import org.hyperic.hq.notifications.filtering.MetricDestinationEvaluator;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutgoingMetricZeventListener implements ZeventListener<MeasurementZevent> {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    protected ConcurrentStatsCollector concurrentStatsCollector;
    protected MeasurementManager msmtMgr;
    protected MetricDestinationEvaluator evaluator;
    protected ZeventEnqueuer zEventManager;
    protected Q q;

    @Autowired
    OutgoingMetricZeventListener(Q q,ZeventEnqueuer zEventManager, MetricDestinationEvaluator evaluator, MeasurementManager msmtMgr,ConcurrentStatsCollector concurrentStatsCollector) {
        this.zEventManager=zEventManager;
        this.evaluator = evaluator;
        this.msmtMgr = msmtMgr;
        this.concurrentStatsCollector=concurrentStatsCollector;
        this.q = q;
    }
    
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(MeasurementZevent.class, this);
        concurrentStatsCollector.register(ConcurrentStatsCollector.NOTIFICATION_FILTERING_TIME);
    }    
//    @Transactional(readOnly = true) 
    protected List<MetricNotification> extract(List<MeasurementZevent> events) {
        List<MetricNotification> dtps = new ArrayList<MetricNotification>();
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
            // TODO~ black list should be here
            
            Resource rsc = msmt.getResource();
            MetricNotification dtp = new MetricNotification(rsc.getId(),mid,metricVal);
            dtps.add(dtp);
        }
        return dtps;
    } 
    
    @Transactional(readOnly = true) 
    public void processEvents(List<MeasurementZevent> events) {
        final long start = System.currentTimeMillis();
        List<MetricNotification> dtps = extract(events);
        List<ObjectMessage> msgs;
        try {
            msgs = this.evaluator.evaluate(dtps);
            this.q.publish(msgs);
        }catch(Throwable e) {
            log.error(e);
        }
        final long end = System.currentTimeMillis();
        concurrentStatsCollector.addStat(end-start, ConcurrentStatsCollector.NOTIFICATION_FILTERING_TIME);
    }
}
