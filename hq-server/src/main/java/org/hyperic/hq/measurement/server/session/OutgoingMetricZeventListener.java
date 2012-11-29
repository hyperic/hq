package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;

import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl.DummyMsg;
import org.hyperic.hq.measurement.shared.MeasurementManager;
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
        
        for(OutgoingMetricsZevent event:events) {
            
            MetricValue ptp = ((MeasurementZeventPayload) event.getPayload()).getValue();
//            List<Integer> this.cond.evaluate(ptp);
//            metricValues.add(ptp);
            
            Message msg = new DummyMsg();
            
            msg.setJMSDestination();
        }
        this.q.publish();
    }
}

