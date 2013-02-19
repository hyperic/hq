package org.hyperic.hq.notifications.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NotificationGroup {
    protected List<? extends BaseNotification> ns = new ArrayList<BaseNotification>();
    protected Integer registrationID;
    
    public NotificationGroup(Integer registrationID, List<? extends BaseNotification> ns) {
        this.ns = ns;
        this.registrationID = registrationID;
    }
    public List<? extends BaseNotification> getNotifications() {
        return ns;
    }
    public void setNotifications(List<? extends BaseNotification> ns) {
        this.ns = ns;
    }
    public Integer getRegistrationID() {
        return registrationID;
    }
    public void setRegistrationID(Integer registrationID) {
        this.registrationID = registrationID;
    }
}
