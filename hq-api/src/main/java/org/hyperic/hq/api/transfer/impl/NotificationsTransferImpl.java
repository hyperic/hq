package org.hyperic.hq.api.transfer.impl;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.NotificationsMapper;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.notifications.EndpointQueue;
import org.hyperic.hq.notifications.InternalNotificationReport;
import org.hyperic.hq.notifications.UnregisteredException;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("notificationsTransfer")
public class NotificationsTransferImpl implements NotificationsTransfer {
    @Autowired
    private NotificationsMapper mapper;
    @Autowired
    private EndpointQueue endpointQueue;
    @Autowired
    private ResourceTransfer rscTransfer;
    @Autowired
    private MeasurementTransfer msmtTransfer;
    // will be replaced by the destinations the invokers of this API will pass when registering

    @Transactional (readOnly=true)
    public NotificationsReport poll(long registrationId, ApiMessageContext apiMessageContext)
    throws UnregisteredException {
        InternalNotificationReport inr = endpointQueue.poll(registrationId);
        AuthzSubject subject = apiMessageContext.getAuthzSubject();
        InternalResourceDetailsType internalResourceDetailsType = inr.getResourceDetailsType();
        ResourceDetailsType resourceDetailsType = null;
        resourceDetailsType = ResourceDetailsType.valueOf(internalResourceDetailsType);
        return mapper.toNotificationsReport(subject, rscTransfer, resourceDetailsType, inr.getNotifications());
    }
    
    @Transactional (readOnly=false)
    public void unregister(RegistrationID id) {
        endpointQueue.unregister(id.getId());
        rscTransfer.unregister(id);
        msmtTransfer.unregister(id);
    }

}
