package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.zevents.ZeventListener;

public class OutgoingMetricZeventListener implements ZeventListener<OutgoingMetricsZevent> {
    protected MetricDestinationEvaluator evaluator;
    protected Q q;
    
    OutgoingMetricZeventListener(Q q, MetricDestinationEvaluator evaluator) {
        this.evaluator = evaluator;
        this.q = q;
    }
    
    public void processEvents(List<OutgoingMetricsZevent> events) {
        List<MetricValue> metricValues = new ArrayList<MetricValue>();
        try {
            for(OutgoingMetricsZevent event:events) {
                List<DataPoint> dtps = event.getDtps();
                List<ObjectMessage> msgs = this.evaluator.evaluate(dtps);
                this.q.publish(msgs);
            }
        }catch(JMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

