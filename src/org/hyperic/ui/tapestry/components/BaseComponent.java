package org.hyperic.ui.tapestry.components;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry.annotations.InjectObject;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;

public abstract class BaseComponent extends org.apache.tapestry.BaseComponent {

    @InjectObject("service:tapestry.globals.HttpServletRequest")
    public abstract HttpServletRequest getRequest();

    @InjectObject("service:tapestry.globals.ServletContext")
    public abstract ServletContext getServletContext();

    public String getPageName() {
        return getPage().getPageName();
    }

    public String getUserName() {
        WebUser user = getUser();
        if (user == null)
            return "";
        else
            return user.getName();
    }

    public boolean getIsLoggedIn() {
        WebUser user = getUser();
        if (user == null)
            return false;
        else
            return true;
    }

    private WebUser getUser() {
        return (WebUser) getRequest().getSession().getAttribute(
                Constants.WEBUSER_SES_ATTR);
    }
}
