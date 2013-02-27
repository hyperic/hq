package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.springframework.stereotype.Component;

@Component
public class Q {
    private final Log log = LogFactory.getLog(Q.class);
    protected final static int QUEUE_LIMIT = 10000;

    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Destination, AccumulatedRegistrationData> destinations = new ConcurrentHashMap<Destination, AccumulatedRegistrationData>();

    public void register(Destination dest) {
        this.register(dest,null);
    }
    public void register(Destination dest, InternalResourceDetailsType resourceDetailsType) {
        if (this.destinations.containsKey(dest)) {
            return;
        }
        AccumulatedRegistrationData ard = this.destinations.put(dest, new AccumulatedRegistrationData(QUEUE_LIMIT,resourceDetailsType));
        if (log.isDebugEnabled()) { log.debug(ard==null?("a new queue was registered for destination " + dest):("a new queue was registered for destination " + dest + " instead of a previously existing queue")); }
    }
    
    public void unregister(Destination dest) {
        if (dest==null) {
            return;
        }
        if (this.destinations.containsKey(dest)) {
            this.destinations.remove(dest);
            if (log.isDebugEnabled()) { log.debug("removing the queue assigned for destination " + dest); }
        } else {
            if (log.isDebugEnabled()) { log.debug("there is no queue assigned for destination "); }
        }
    }

    public InternalNotificationReport poll(Destination dest) {
        AccumulatedRegistrationData ard = this.destinations.get(dest);
        InternalNotificationReport nr = new InternalNotificationReport();
        if (ard==null) {
            if (log.isDebugEnabled()) { log.debug("unable to poll - there is no queue assigned for destination " + dest);}
            return nr;
        }
        
        List<BaseNotification> ns = new ArrayList<BaseNotification>();
        LinkedBlockingQueue<BaseNotification> anq = ard.getAccumulatedNotificationsQueue();
        anq.drainTo(ns);
        nr.setNotifications(ns);
        nr.setResourceDetailsType(ard.getResourceContentType());
        return nr;
    }
    
    @SuppressWarnings("unchecked")
    public void publish(List<ObjectMessage> msgs) throws JMSException {
        for(ObjectMessage msg:msgs) {
            Destination dest = msg.getJMSDestination();
            List<BaseNotification> data = (List<BaseNotification>) msg.getObject();
            AccumulatedRegistrationData ard = destinations.get(dest);
            if (ard==null) {
                return;
            }
            LinkedBlockingQueue<BaseNotification> anq = ard.getAccumulatedNotificationsQueue();
            try {
                anq.addAll(data);
            } catch (IllegalStateException e) {
                log.error(e);
                // TODO~ persist messages to disk in case the Q is full
            }
        }
    }
}