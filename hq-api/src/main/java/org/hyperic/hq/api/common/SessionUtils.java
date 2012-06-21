package org.hyperic.hq.api.common;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.api.services.impl.ApiMessageContext;



public class SessionUtils {

    
    /**
     * The session scope attribute under which the User object
     * for the currently logged in user is stored.
     */
    public static final String WEBUSER_SES_ATTR = "webUser";   
    
    /**
     * Retrieve the cached <code>WebUser</code> representing the user
     * interacting with server.
     * 
     * @param session
     *            the http session
     */
    public static InterfaceUser getWebUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(WEBUSER_SES_ATTR);
        if (attr == null) {
            return null;
        }
        return (InterfaceUser) attr;
    }  
    
    public static Integer getSessionId(HttpSession session) {
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
    public static InterfaceUser getWebUser(HttpServletRequest request)
        throws ServletException {
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {
            // show throw SessionNotFoundException
            throw new ServletException("web session does not exist!");
        }

        return getWebUser(session);
    }
    
    /** Return the <code>WebUser</code> representing the person currently
     * interacting with the product.
     * @exception ServletException if the session cannot be accessed
     */
    public static ApiMessageContext newApiMessageContext(HttpServletRequest request)
        throws ServletException {
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {            
            throw new ServletException("web session does not exist!");
        }

        InterfaceUser webUser = getWebUser(session);
        if (null == webUser) {
            throw new ServletException("Missing user and session details on the web session.");
        }
        return new ApiMessageContext(webUser.getSessionId(), webUser.getSubject());
    }


    /** Extract the BizApp session id as an <code>Integer</code> from
     * the web session.
     * @exception ServletException if the session cannot be accessed
     */
    public static Integer getSessionId(HttpServletRequest request)
        throws ServletException {
        return getWebUser(request).getSessionId();
    }          
}
