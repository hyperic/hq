package org.hyperic.hq.api.services.impl;

import javax.ws.rs.core.Response;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.services.NotificationsService;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.notifications.UnregisteredException;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationsServiceImpl implements NotificationsService {
    @Autowired
    protected NotificationsTransfer notificationsTransfer;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler ; 

    public NotificationsReport poll() throws SessionNotFoundException, SessionTimeoutException {
        try {
            return notificationsTransfer.poll();
        } catch (UnregisteredException e) {
            errorHandler.log(e);
            throw errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.UNREGISTERED_FOR_NOTIFICATIONS, e.getMessage());
        }
    }
    public void unregister() throws SessionNotFoundException, SessionTimeoutException {
        notificationsTransfer.unregister();
    }
}
