/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.resourcehealth;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

public class PrepareAction extends TilesAction {

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception
    {
        ServletContext ctx = getServlet().getServletContext();
        HttpSession session = request.getSession();
        ConfigResponse userDashPrefs = (ConfigResponse) request.getSession().getAttribute(Constants.USER_DASHBOARD_CONFIG);
        WebUser user = (WebUser)
            session.getAttribute(Constants.WEBUSER_SES_ATTR);
        String key = Constants.USERPREF_KEY_FAVORITE_RESOURCES;

        DashboardUtils.verifyResources(key, ctx, userDashPrefs, user);
        // this quarantees that the session dosen't contain any resources it
        // shouldnt
        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
        List resources = DashboardUtils.preferencesAsResources(key, ctx, user, userDashPrefs);

        Pager pendingPager = Pager.getDefaultPager();
        PageList viewableResourses = pendingPager.seek(resources,
                                                       PageControl.PAGE_ALL);

        viewableResourses.setTotalSize(resources.size());

        request.setAttribute(Constants.RESOURCE_HEALTH_LIST, viewableResourses);           
        
        return null;
    }
}
