package org.hyperic.hq.api.services.impl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.hyperic.hq.api.common.InterfaceUser;
import org.hyperic.hq.api.services.ResourceService;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;

public class RestApiService {

    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler;
    @javax.ws.rs.core.Context
    protected MessageContext messageContext;
    
    @Autowired 
    SessionManager sessionManager;

    /**
     * The session scope attribute under which the User object
     * for the currently logged in user is stored.
     */
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
    protected ApiMessageContext newApiMessageContext(HttpServletRequest request) throws SessionNotFoundException, SessionTimeoutException {
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {            
//            throw new ServletException("web session does not exist!");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.UNAUTHORIZED, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;
        }

        InterfaceUser webUser = getWebUser(session);
        if (null == webUser) {
//            throw new ServletException("Missing user and session details on the web session.");
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.UNAUTHORIZED, ExceptionToErrorCodeMapper.ErrorCode.INVALID_SESSION);            
            throw webApplicationException;            
        }       
        
        Integer sessionId = webUser.getSessionId();
        return new ApiMessageContext(sessionId, sessionManager.getSubject(sessionId));
    }    
    
    
    /**
     * Retrieve the cached <code>WebUser</code> representing the user
     * interacting with server.
     * 
     * @param session
     *            the http session
     */
    protected static InterfaceUser getWebUser(HttpSession session) {

        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(WEBUSER_SES_ATTR);
        if (attr == null) {
            return null;
        }
        return (InterfaceUser) attr;
    }  
    
    protected static Integer getSessionId(HttpSession session) {
        InterfaceUser user = getWebUser(session);
        if (null != user) {
            return user.getSessionId();
        }
        return null;
    }
    

    /** Return the <code>WebUser</code> representing the person currently
     * interacting with the product.
     * @exception ServletException if the session cannot be accessed
     */
    protected static InterfaceUser getWebUser(HttpServletRequest request)
        throws ServletException {
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {
            // show throw SessionNotFoundException
            throw new ServletException("web session does not exist!");
        }

        return getWebUser(session);
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

}