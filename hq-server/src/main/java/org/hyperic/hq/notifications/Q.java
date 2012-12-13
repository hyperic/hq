package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.server.session.ReportProcessorImpl;
import org.springframework.stereotype.Component;

@Component
public class Q {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    protected final static int QUEUE_LIMIT = 10000;

    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Destination, LinkedBlockingQueue<Object>> destinations = new ConcurrentHashMap<Destination, LinkedBlockingQueue<Object>>();

    public void register(Destination dest) {
        LinkedBlockingQueue<Object> q = this.destinations.put(dest, new LinkedBlockingQueue<Object>(QUEUE_LIMIT));
        if (log.isDebugEnabled()) {
            if (q==null) {
                log.debug("a new queue was registered for destination " + dest);
            } else {
                log.debug("a new queue was registered for destination " + dest + " instead of a previously existing queue");
            }
        }
    }
    
    public void unregister(Destination dest) {
        LinkedBlockingQueue<Object> q = this.destinations.remove(dest);
        if (log.isDebugEnabled()) {
            if (q==null) {
                log.debug("there is no queue assigned for destination " + dest);
            } else {
                log.debug("removing the queue assigned for destination " + dest);
            }
        }
    }

    public List<?> poll(Destination dest) {
        LinkedBlockingQueue<Object> topic = this.destinations.get(dest);
        List<Object> metrics = new ArrayList<Object>();
        if (topic==null) {
            if (log.isDebugEnabled()) {
                log.debug("unable to poll - there is no queue assigned for destination " + dest);
            }
            return metrics;
        }
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
                log.error(e);
                // TODO~ persist messages to disk in case Q is full
            }
        }
    }
}