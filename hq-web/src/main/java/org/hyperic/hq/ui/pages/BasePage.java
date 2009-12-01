/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.ui.pages;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry.annotations.Component;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.InjectState;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.link.GenericLink;
import org.hyperic.hq.ui.session.BaseSessionBean;

/**
 * Base Hyperic Tapestry page class. Has common service injections and 
 * struts page definitions.
 *
 */
public abstract class BasePage extends org.apache.tapestry.html.BasePage implements PageBeginRenderListener{

    @InjectState("BaseSessionBean")
    public abstract BaseSessionBean getBaseSessionBean();
    
    /**
     * Use this link to refer to the Struts Dashobard page.
     * @return a link to the Dashboard page
     */
    @Component(type = "GenericLink", bindings = { "href='Dashobard.do'" })
    public abstract GenericLink getDashboardLink();
    
    @Component(type = "GenericLink", bindings = { "href='Resource.do'" })
    public abstract GenericLink getResourceLink();
    
    @Component(type = "GenericLink", bindings = { "href='admin/user/UserAdmin.do?mode=register'" })
    public abstract GenericLink getRegistrationLink();

    @InjectObject("service:tapestry.globals.ServletContext")
    public abstract ServletContext getServletContext();

    @InjectObject("service:tapestry.globals.HttpServletRequest")
    public abstract HttpServletRequest getRequest();

    public void pageBeginRender(PageEvent evt) {
        getBaseSessionBean().init(getRequest(), getServletContext());
    }
    //TODO add injection to base state object
    //to contain things like the session id for bosses
}
