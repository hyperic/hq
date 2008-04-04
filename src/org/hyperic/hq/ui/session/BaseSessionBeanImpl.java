package org.hyperic.hq.ui.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;

final class BaseSessionBeanImpl implements BaseSessionBean{

    //Used for bean setup
    private ServletContext context;
    private HttpServletRequest request;
    
    private WebUser webUser;
    private Integer sessionId;

    public BaseSessionBeanImpl() {
        webUser = (WebUser) getRequest().getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        sessionId = webUser.getSessionId();
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        request.getSession().setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
        this.webUser = webUser;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /*
     * These are called by Hivemind upon object creation
     */
    private ServletContext getServletContext() {
        return context;
    }

    public void setServletContext(ServletContext context) {
        this.context = context;
    }

    private HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
