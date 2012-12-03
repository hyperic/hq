package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.annotation.PostConstruct;

import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventSource;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OutgoingMetricZeventListener implements ZeventListener<MeasurementZevent> {
    protected MetricDestinationEvaluator evaluator;
    protected ZeventEnqueuer zEventManager;
    protected Q q;

    @Autowired
    OutgoingMetricZeventListener(Q q,ZeventEnqueuer zEventManager, MetricDestinationEvaluator evaluator) {
        this.zEventManager=zEventManager;
        this.evaluator = evaluator;
        this.q = q;
    }
    
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(MeasurementZevent.class, this);
    }
    
    protected static List<DataPoint> extract(List<MeasurementZevent> events) {
        List<DataPoint> dtps = new ArrayList<DataPoint>();
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
            DataPoint dtp = new DataPoint(Integer.valueOf(measurementId),metricVal);
            dtps.add(dtp);
        }
        return dtps;
    }
    
    public void processEvents(List<MeasurementZevent> events) {
        List<DataPoint> dtps = extract(events);
        List<ObjectMessage> msgs;
        try {
            msgs = this.evaluator.evaluate(dtps);
            this.q.publish(msgs);
        }catch(JMSException e) {
            e.printStackTrace();
            RuntimeException r = new RuntimeException(e.getCause());
            throw r;
        }
    }
}
