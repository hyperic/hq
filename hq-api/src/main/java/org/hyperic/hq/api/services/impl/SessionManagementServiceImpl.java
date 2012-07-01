package org.hyperic.hq.api.services.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hyperic.hq.api.services.SessionManagementService;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;

public class SessionManagementServiceImpl extends RestApiService implements SessionManagementService {

    public void logout() throws SessionNotFoundException, SessionTimeoutException {
        HttpServletRequest httpServletRequest = messageContext.getHttpServletRequest();
        
        // Invalidate HQ session
        ApiMessageContext apiMessageContext = newApiMessageContext();
        sessionManager.invalidate(apiMessageContext.getSessionId());
        
        // Invalidate http session
        HttpSession session = httpServletRequest.getSession(false);        
        if (session == null) {            
            logger.warn("Web session does not exist for the request!");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;
        }
        session.invalidate();
    }

}
