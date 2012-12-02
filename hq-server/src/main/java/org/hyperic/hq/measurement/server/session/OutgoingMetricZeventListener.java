package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.annotation.PostConstruct;

import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;

public class OutgoingMetricZeventListener implements ZeventListener<OutgoingMetricsZevent> {
    protected MetricDestinationEvaluator evaluator;
    protected Q q;
    protected ZeventEnqueuer zEventManager;

    OutgoingMetricZeventListener(Q q,ZeventEnqueuer zEventManager, MetricDestinationEvaluator evaluator) {
        this.zEventManager=zEventManager;
        this.evaluator = evaluator;
        this.q = q;
    }
    
    @PostConstruct
    public void init() {
        zEventManager.addBufferedListener(OutgoingMetricsZevent.class, this);
    }
    public void processEvents(List<OutgoingMetricsZevent> events) {
        List<MetricValue> metricValues = new ArrayList<MetricValue>();
        List<ObjectMessage> msgs;
        try {
            msgs = this.evaluator.evaluate(metricValues);
            this.q.publish(msgs);
        }catch(JMSException e) {
            e.printStackTrace();
            RuntimeException r = new RuntimeException(e.getCause());
            throw r;
        }
    }
}
