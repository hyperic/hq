package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.hq.notifications.model.NotificationGroup;
import org.springframework.stereotype.Component;

@Component
public class Q {
    private final Log log = LogFactory.getLog(Q.class);
    protected final static int QUEUE_LIMIT = 10000;

    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Integer, AccumulatedRegistrationData> registrations = new ConcurrentHashMap<Integer, AccumulatedRegistrationData>();
    protected Map<Integer,Destination> regToDst = new ConcurrentHashMap<Integer,Destination>();
    
    public void register(Destination dest, Integer regID, InternalResourceDetailsType resourceDetailsType) {
        if (this.registrations.containsKey(regID)) {
            return;
        }
        AccumulatedRegistrationData ard = this.registrations.put(regID, new AccumulatedRegistrationData(QUEUE_LIMIT,resourceDetailsType));
        this.regToDst.put(regID, dest);
        if (log.isDebugEnabled()) { log.debug(ard==null?("a new queue was registered for destination " + regID):("a new queue was registered for destination " + regID + " instead of a previously existing queue")); }
    }
    
    public void unregister(Integer regID) {
        AccumulatedRegistrationData ard = this.registrations.remove(regID);
        this.regToDst.remove(regID);
        if (log.isDebugEnabled()) { log.debug(ard==null?"there is no queue assigned for destination ":"removing the queue assigned for destination " + regID); }
    }

    public InternalNotificationReport poll(Integer regID) {
        AccumulatedRegistrationData ard = this.registrations.get(regID);
        InternalNotificationReport nr = new InternalNotificationReport();
        if (ard==null) {
            if (log.isDebugEnabled()) { log.debug("unable to poll - there is no queue assigned for destination " + regID);}
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
    public void publish(List<NotificationGroup> nsGrpList) {
        for(NotificationGroup nsGrp:nsGrpList) {
            Integer regID = nsGrp.getRegistrationID();
            List<BaseNotification> data = (List<BaseNotification>) nsGrp.getNotifications();
            AccumulatedRegistrationData ard = registrations.get(regID);
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