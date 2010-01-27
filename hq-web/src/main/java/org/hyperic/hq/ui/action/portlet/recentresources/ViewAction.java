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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class ViewAction
    extends TilesAction {

    private final Log log = LogFactory.getLog("DASHBOARD-TIMING");
    private AuthzBoss authzBoss;
    private AppdefBoss appdefBoss;

    @Autowired
    public ViewAction(AuthzBoss authzBoss, AppdefBoss appdefBoss) {
        super();
        this.authzBoss = authzBoss;
        this.appdefBoss = appdefBoss;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        StopWatch timer = new StopWatch();

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        ConfigResponse userPrefs = user.getPreferences();
        String key = Constants.USERPREF_KEY_RECENT_RESOURCES;
        if (userPrefs.getValue(key, null) != null) {
            Map<AppdefEntityID, Resource> list;
            try {
                list = getStuff(key, user, userPrefs);
            } catch (Exception e) {
                DashboardUtils.verifyResources(key, getServlet().getServletContext(), userPrefs, user, appdefBoss, authzBoss);
                list = getStuff(key, user, userPrefs);
            }

            context.putAttribute("resources", list);
        } else {
            context.putAttribute("resources", new ArrayList());
        }

        log.debug("ViewRecentResources - timing [" + timer.toString() + "]");
        return null;
    }

    private Map<AppdefEntityID, Resource> getStuff(String key, WebUser user, ConfigResponse dashPrefs) throws Exception {
        List<AppdefEntityID> entityIds = DashboardUtils.preferencesAsEntityIds(key, dashPrefs);
        Collections.reverse(entityIds); // Most recent on top

        AppdefEntityID[] arrayIds = new AppdefEntityID[entityIds.size()];
        arrayIds = entityIds.toArray(arrayIds);

        return authzBoss.findResourcesByIds(user.getSessionId().intValue(), arrayIds);
    }
}
