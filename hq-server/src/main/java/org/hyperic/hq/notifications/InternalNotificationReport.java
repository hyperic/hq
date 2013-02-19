package org.hyperic.hq.notifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public class InternalNotificationReport {
    public void setResourceDetailsType(Integer regID, InternalResourceDetailsType resourceDetailsType) {
        this.resourceDetailsType.put(regID,resourceDetailsType);
    }

    public void setNotifications(Integer regID, List<? extends BaseNotification> notifications) {
        this.notifications.put(regID,notifications);
    }

    protected Map<Integer,InternalResourceDetailsType> resourceDetailsType = new HashMap<Integer,InternalResourceDetailsType>();
    protected Map<Integer,List<? extends BaseNotification>> notifications = new HashMap<Integer,List<? extends BaseNotification>>();
    
    public Map<Integer,InternalResourceDetailsType> getResourceDetailsType() {
        return this.resourceDetailsType;
    }

    public Map<Integer,List<? extends BaseNotification>> getNotifications() {
        return this.notifications;
    }
}
