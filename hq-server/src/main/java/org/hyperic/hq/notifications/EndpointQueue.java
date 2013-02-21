package org.hyperic.hq.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.springframework.stereotype.Component;

@Component
public class EndpointQueue {
    private final Log log = LogFactory.getLog(EndpointQueue.class);
// XXX should make this configurable in some way
    protected final static int QUEUE_LIMIT = 10000;

    // TODO~ change to write through versioning (each node would have versioning -
    // write on one version, read another, then sync between them), w/o will pose problems in scale
    private final Map<Long, AccumulatedRegistrationData> registrationData = new HashMap<Long, AccumulatedRegistrationData>();

    public void register(NotificationEndpoint endpoint) {
        register(endpoint,null);
    }

    public void register(NotificationEndpoint endpoint, InternalResourceDetailsType resourceDetailsType) {
        final AccumulatedRegistrationData data =
            new AccumulatedRegistrationData(endpoint, QUEUE_LIMIT, resourceDetailsType);
        synchronized (registrationData) {
            if (registrationData.containsKey(endpoint.getRegistrationId())) {
                return;
            }
            final long regId = endpoint.getRegistrationId();
            registrationData.put(regId, data);
        }
        if (log.isDebugEnabled()) {
            String s = "a new queue was registered for destination " + endpoint;
            String msg = (data == null) ? s : s + " instead of a previously existing queue";
            log.debug(msg);
        }
    }
    
    public NotificationEndpoint unregister(long registrationID) {
        AccumulatedRegistrationData ard = null;
        synchronized (registrationData) {
            ard = registrationData.remove(registrationID);
        }
        if (log.isDebugEnabled()) { 
            String s =  "there is no queue assigned for destination";
            String msg = (ard == null) ? s : "removing the queue assigned for regId " + registrationID;
            log.debug(msg);
        }
        return ard == null ? null : ard.getNotificationEndpoint();
    }

    public InternalNotificationReport poll(long registrationId) {
        final InternalNotificationReport rtn = new InternalNotificationReport();
        final List<BaseNotification> notifications = new ArrayList<BaseNotification>();
        synchronized (registrationData) {
            AccumulatedRegistrationData data = registrationData.get(registrationId);
            if (data == null) {
                return rtn;
            }
            data.drainTo(notifications);
            rtn.setNotifications(notifications);
            rtn.setResourceDetailsType(data.getResourceContentType());
            return rtn;
        }
    }
    
    public <T extends BaseNotification> void publishAsync(Map<NotificationEndpoint, Collection<T>> map) {
        synchronized (registrationData) {
            for (final Entry<NotificationEndpoint, Collection<T>> entry : map.entrySet()) {
                final NotificationEndpoint endpoint = entry.getKey();
                final Collection<T> list = entry.getValue();
                final AccumulatedRegistrationData data = registrationData.get(endpoint.getRegistrationId());
                if (data != null) {
                    data.addAll(list);
                }
            }
        }
    }

}