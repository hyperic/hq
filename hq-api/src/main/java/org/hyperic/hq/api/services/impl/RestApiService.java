package org.hyperic.hq.api.services.impl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.hyperic.hq.api.common.InterfaceUser;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class RestApiService {

    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler;
    @javax.ws.rs.core.Context
    protected MessageContext messageContext;
    
    @Autowired 
    SessionManager sessionManager;
    
    @Autowired
    @Qualifier("restApiLogger")
    Log logger;    
 

    /**
     * The session scope attribute under which the User object
     * for the currently logged in user is stored.
     */
    public static final String APIUSER_SES_ATTR = "apiUser";   
    
    // If the request is from the UI, then it is web user, and not api
    public static final String WEBUSER_SES_ATTR = "webUser";
   
    
    public RestApiService() {
        super();
    }

    protected WebApplicationException createWebApplicationException(ServletException e, Response.Status status, String errorCode) {
        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        builder.status(status);
        builder.entity(errorCode);
        Response response = builder.build();
    
        WebApplicationException webApplicationException = new WebApplicationException(e, response);
        return webApplicationException;
    }

    /** Return the <code>ApiMessageContext</code> representing the person currently
     * interacting with the product.
     * @throws SessionTimeoutException 
     * @throws SessionNotFoundException 
     * @exception WebApplicationException if the session cannot be accessed or does not contain correct information
     */
    protected ApiMessageContext newApiMessageContext() throws SessionNotFoundException, SessionTimeoutException {
        if (null == messageContext) {
            logger.error("Message context is not initialized for the service!");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.FORBIDDEN, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;            
        }
        HttpServletRequest request = messageContext.getHttpServletRequest();
        assert(null != request);
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {            
            logger.error("Web session does not exist for the request!");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.FORBIDDEN, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;
        }

        InterfaceUser apiUser = getApiUser(session);
        // If request comes from UI, no API user, but web user
        apiUser = (null == apiUser ? getWebUser(session) : apiUser);
        
        if (null == apiUser) {
            logger.error("Missing user and session details on the web session.");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.UNAUTHORIZED, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;            
        }       
        
        Integer sessionId = apiUser.getSessionId();
        return new ApiMessageContext(sessionId, sessionManager.getSubject(sessionId));
    }    
    
    
    /**
     * Retrieve the cached <code>ApiUser</code> representing the user
     * interacting with server.
     * 
     * @param session
     *            the http session
     */
    public static InterfaceUser getApiUser(HttpSession session) {

        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(APIUSER_SES_ATTR);
        if (attr == null) {
            return null;
        }
        return (InterfaceUser) attr;
    }  
    
    private static InterfaceUser getWebUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(WEBUSER_SES_ATTR);
        return (attr == null) ? null : (InterfaceUser) attr;
    }    
    
    public static Integer getSessionId(HttpSession session) {
        InterfaceUser user = getApiUser(session);
        if (null != user) {
            return user.getSessionId();
        }
        return null;
    }
           
    
    public MessageContext getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }

    public ExceptionToErrorCodeMapper getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ExceptionToErrorCodeMapper errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    public Log getLogger() {
        return logger;
    }    
    public void setLogger(Log logger) {
        this.logger = logger;
    }   
    
    protected HttpSession getSession() {
        HttpServletRequest request = messageContext.getHttpServletRequest();
        assert(null != request);
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        return session;
    }


}