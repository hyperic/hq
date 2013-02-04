package org.hyperic.hq.api.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
//@XmlRootElement(name = "notificationsGroup", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="NotificationsGroup Type", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class NotificationsGroup {
    @XmlAttribute
    protected NotificationType type;
    @XmlElementRef
    protected List<Notification> notification;
    
    public NotificationsGroup() {}
        
    public NotificationsGroup(NotificationType type) {
        this.type=type;
    }
    public List<Notification> getNotifications() {
        return this.notification;
    }
    public void setNotifications(List<Notification> notifications) {
        this.notification = notifications;
    }
}
