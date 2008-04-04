package org.hyperic.hq.ui.session;

import org.hyperic.hq.ui.WebUser;

public interface BaseSessionBean {

    public abstract WebUser getWebUser();

    public abstract void setWebUser(WebUser webUser);

    public abstract Integer getSessionId();

    public abstract void setSessionId(Integer sessionId);

}