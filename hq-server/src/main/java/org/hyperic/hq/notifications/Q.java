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
import org.hyperic.hq.notifications.model.INotification;
import org.springframework.stereotype.Component;

@Component
public class Q {
    private final Log log = LogFactory.getLog(ReportProcessorImpl.class);
    protected final static int QUEUE_LIMIT = 10000;

    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Destination, LinkedBlockingQueue<INotification>> destinations = new ConcurrentHashMap<Destination, LinkedBlockingQueue<INotification>>();

    public void register(Destination dest) {
        if (this.destinations.containsKey(dest)) {
            return;
        }
        LinkedBlockingQueue<INotification> q = this.destinations.put(dest, new LinkedBlockingQueue<INotification>(QUEUE_LIMIT));
        if (log.isDebugEnabled()) { log.debug(q==null?("a new queue was registered for destination " + dest):("a new queue was registered for destination " + dest + " instead of a previously existing queue")); }
    }
    
    public void unregister(Destination dest) {
        LinkedBlockingQueue<INotification> q = this.destinations.remove(dest);
        if (log.isDebugEnabled()) { log.debug(q==null?"there is no queue assigned for destination ":"removing the queue assigned for destination " + dest); }
    }

    public List<? extends INotification> poll(Destination dest) {
        LinkedBlockingQueue<INotification> topic = this.destinations.get(dest);
        List<INotification> data = new ArrayList<INotification>();
        if (topic==null) {
            if (log.isDebugEnabled()) { log.debug("unable to poll - there is no queue assigned for destination " + dest);}
            return data;
        }
        topic.drainTo(data);
        return data;
    }
    
    @SuppressWarnings("unchecked")
    public void publish(List<ObjectMessage> msgs) throws JMSException {
        for(ObjectMessage msg:msgs) {
            Destination dest = msg.getJMSDestination();
            List<INotification> data = (List<INotification>) msg.getObject();
            LinkedBlockingQueue<INotification> q = destinations.get(dest);
            if (q==null) {
                return;
            }
            try {
                q.addAll(data);
            } catch (IllegalStateException e) {
                log.error(e);
                // TODO~ persist messages to disk in case the Q is full
            }
        }
    }
}
