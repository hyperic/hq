package org.hyperic.hq.notifications;

import java.util.List;

import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.zevents.Zevent;

/**
 * only purpose of this mechanism is to run the Notification Zevents in a separate executor so that the
 * ZeventManager doesn't get backed up and it can run in a separate Transactional context
 */
public interface NotificationFilterExecutorRunner<E extends Zevent,N extends BaseNotification> 
extends Runnable {
    
    public void setEvents(List<E> events);
   
    public void setListener(BaseNotificationsZeventListener<E, N> listener);

}
