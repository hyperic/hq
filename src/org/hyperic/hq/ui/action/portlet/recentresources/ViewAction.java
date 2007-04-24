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

package org.hyperic.hq.ui.action.portlet.recentresources;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class ViewAction extends TilesAction {

    private static Log _log = LogFactory.getLog("DASHBOARD-TIMING");

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        StopWatch timer = new StopWatch();

        ServletContext ctx = getServlet().getServletContext();
        AppdefBoss boss = ContextUtils.getAppdefBoss(ctx);
        WebUser user = (WebUser)
            request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;

        List list;
        try {
            list = getStuff(key, boss, user);
        } catch (Exception e) {
            DashboardUtils.verifyResources(key, ctx, user);
            list = getStuff(key, boss, user);
        }

        context.putAttribute("resources", list);

        _log.debug("ViewRecentResources - timing [" + timer.toString() + "]");
        return null;
    }

    private List getStuff(String key, AppdefBoss boss, WebUser user)
        throws Exception {
        List entityIds = DashboardUtils.preferencesAsEntityIds(key, user);
        Collections.reverse(entityIds);     // Most recent on top

        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = (AppdefEntityID[]) entityIds.toArray(arrayIds);

        return boss.findByIds(user.getSessionId().intValue(), arrayIds);
    }
}
