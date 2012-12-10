package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.springframework.stereotype.Component;

@Component
public class Q {
    protected Map<Destination, LinkedBlockingQueue<Object>> destinations = new HashMap<Destination, LinkedBlockingQueue<Object>>();

    public void register(Destination dest) {
        this.destinations.put(dest, new LinkedBlockingQueue<Object>());
    }
    
    public List<?> poll(Destination dest) {
        LinkedBlockingQueue<Object> topic = this.destinations.get(dest);
        List<Object> metrics = new ArrayList<Object>();
        topic.drainTo(metrics);
        return metrics;
    }
    
    public void publish(List<ObjectMessage> msgs) throws JMSException {
        for(ObjectMessage msg:msgs) {
            Destination dest = msg.getJMSDestination();
            List<Object> data = (List<Object>) msg.getObject();
            LinkedBlockingQueue<Object> q = destinations.get(dest);
            if (q==null) {
                return;
            }
            try {
                q.addAll(data);
            } catch (IllegalStateException e) {
                log(e);
            }
        }
    }
}