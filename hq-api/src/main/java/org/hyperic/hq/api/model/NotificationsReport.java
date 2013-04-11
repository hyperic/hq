package org.hyperic.hq.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.resources.BatchResponseBase;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "notificationsReport", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="NotificationsReport Type", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class NotificationsReport extends BatchResponseBase {
    protected List<NotificationsGroup> notifications;
    @XmlAttribute(namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private String registrationId;

    public NotificationsReport(final ExceptionToErrorCodeMapper exceptionToErrorCodeMapper, String regId) {
        super(exceptionToErrorCodeMapper) ;
        this.notifications = new ArrayList<NotificationsGroup>();
        this.registrationId = regId;
    }

    public NotificationsReport() {
        this.notifications = new ArrayList<NotificationsGroup>();
    }
    
    public List<NotificationsGroup> getNotificationsGroupList() {
        return notifications;
    }

    public void setNotificationsGroupList(List<NotificationsGroup> notificationsGroupList) {
        this.notifications = notificationsGroupList;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

}
