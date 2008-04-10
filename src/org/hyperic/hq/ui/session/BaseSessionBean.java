package org.hyperic.hq.ui.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.WebUser;

public interface BaseSessionBean {
    
    /**
     * @deprecated ditch this interface method and impls when struts is removed 
     * for legacy support only 
     */
    public abstract void init(HttpServletRequest r, ServletContext c);

    public abstract WebUser getWebUser();

    public abstract void setWebUser(WebUser webUser);

    public abstract Integer getSessionId();

    public abstract void setSessionId(Integer sessionId);

}