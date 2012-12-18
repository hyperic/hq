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
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutgoingMetricZeventListener implements ZeventListener<MeasurementZevent> {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    protected MeasurementManager msmtMgr;
    protected MetricDestinationEvaluator evaluator;
    protected ZeventEnqueuer zEventManager;
    protected Q q;

    @Autowired
    OutgoingMetricZeventListener(Q q,ZeventEnqueuer zEventManager, MetricDestinationEvaluator evaluator, MeasurementManager msmtMgr) {
        this.zEventManager=zEventManager;
        this.evaluator = evaluator;
        this.msmtMgr = msmtMgr;
        this.q = q;
    }
    
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(MeasurementZevent.class, this);
    }
    
    @Transactional(readOnly = true)
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
    
    public void processEvents(List<MeasurementZevent> events) {
        List<MetricNotification> dtps = extract(events);
        List<ObjectMessage> msgs;
        try {
            msgs = this.evaluator.evaluate(dtps);
            this.q.publish(msgs);
        }catch(JMSException e) {
            log.error(e);
            SystemException sysEx = new SystemException(e);
            throw sysEx;
        }
    }
}
