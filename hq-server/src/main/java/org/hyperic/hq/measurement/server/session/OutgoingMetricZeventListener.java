package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.annotation.PostConstruct;

import org.hyperic.hq.measurement.server.session.MeasurementZevent.MeasurementZeventPayload;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl.DummyMsg;
import org.hyperic.hq.measurement.shared.MeasurementManager;
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

