package org.hyperic.hq.ui.pages;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry.annotations.Component;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.link.GenericLink;

/**
 * Base Hyperic Tapestry page class.
 *
 */
public abstract class BasePage extends org.apache.tapestry.html.BasePage {

    /**
     * Use this link to refer to the Struts Dashobard page.
     * @return a link to the Dashboard page
     */
    @Component(type = "GenericLink", bindings = { "href='Dashobard.do'" })
    public abstract GenericLink getDashboardLink();
    
    @Component(type = "GenericLink", bindings = { "href='admin/user/UserAdmin.do?mode=register'" })
    public abstract GenericLink getRegistrationLink();

    @InjectObject("service:tapestry.globals.ServletContext")
    public abstract ServletContext getServletContext();

    @InjectObject("service:tapestry.globals.HttpServletRequest")
    public abstract HttpServletRequest getRequest();

}
