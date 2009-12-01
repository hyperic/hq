package org.hyperic.hq.ui.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;

public final class BaseSessionBeanImpl implements BaseSessionBean {

    // Used for bean setup
    private ServletContext _context;
    private HttpServletRequest _request;

    private WebUser webUser;
    private Integer sessionId;

    public BaseSessionBeanImpl() {
    }

    /**
     * @deprecated see interface for reason 
     */
    public void init(HttpServletRequest request, ServletContext ctx) {
        if (request == null || ctx == null)
            throw new NullPointerException("init params cannot be null");
        _context = ctx;
        _request = request;
    }

    public WebUser getWebUser() {
        if (webUser == null)
            webUser = (WebUser) _request.getSession().getAttribute(
                    Constants.WEBUSER_SES_ATTR);
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        _request.getSession().setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
        this.webUser = webUser;
    }

    public Integer getSessionId() {
        if(sessionId == null)
            sessionId = getWebUser().getSessionId();
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

}
