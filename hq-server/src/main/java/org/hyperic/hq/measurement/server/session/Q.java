package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.hyperic.hq.product.MetricValue;
import org.springframework.stereotype.Component;

@Component
public class Q {
    private static Q instance = new Q();
    private Q() {}

    protected Map<Integer, LinkedBlockingQueue<MetricValue>> topics = new HashMap<Integer, LinkedBlockingQueue<MetricValue>>();
    public Q getInstance() {
        return instance;
    }
    public void register(Integer sessionId) {
        this.topics.put(sessionId, new LinkedBlockingQueue<MetricValue>());
    }
    public List<MetricValue> poll(Integer sessionId) {
        LinkedBlockingQueue<MetricValue> topic = this.topics.get(sessionId);
        List<MetricValue> metrics = new ArrayList<MetricValue>();
        topic.drainTo(metrics);
        return metrics;
    }
    public void publish(List<MetricValue> metricValues) {
        Set<Entry<Integer, LinkedBlockingQueue<MetricValue>>> topics = this.topics.entrySet();
        for(Entry<Integer, LinkedBlockingQueue<MetricValue>> topicE:topics) {
            LinkedBlockingQueue<MetricValue> topic = topicE.getValue();
            topic.addAll(metricValues);
        }
    }
}
