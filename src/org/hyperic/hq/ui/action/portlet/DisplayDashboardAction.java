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

package org.hyperic.hq.ui.action.portlet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.InvalidOptionException;

public class DisplayDashboardAction extends TilesAction {

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception{

        HttpSession session = request.getSession();
        ServletContext ctx = getServlet().getServletContext();
        
        Portal portal =
            (Portal) session.getAttribute(Constants.USERS_SES_PORTAL);
        if (portal == null) {
            portal = new Portal();
            portal.setName("dashboard.template.title");
            portal.setColumns(2);
            // construct from user preferences.
            WebUser user =
                (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
            portal.addPortletsFromString(
                user.getPreference(Constants.USER_PORTLETS_FIRST), 1);
            portal.addPortletsFromString(
                user.getPreference(Constants.USER_PORTLETS_SECOND), 2);

            // Go through the portlets and see if they have descriptions
            for (Iterator pit = portal.getPortlets().iterator(); pit.hasNext();)
            {
                Collection portlets = (Collection) pit.next();
                for (Iterator it = portlets.iterator(); it
                        .hasNext();) {
                    Portlet portlet = (Portlet) it.next();
                    String titleKey = portlet.getUrl() + ".title" +
                        (portlet.getToken() != null ? portlet.getToken() : "");
                    portlet.setDescription(user.getPreference(titleKey, ""));
                }
            }
            
            session.setAttribute(Constants.USERS_SES_PORTAL, portal);

            // Make sure there's a valid RSS auth token
            String rssToken;
            try {
                rssToken = user.getPreference(Constants.RSS_TOKEN);
            } catch (InvalidOptionException e) {
                rssToken = String.valueOf(session.hashCode());
 
                // Now store the RSS auth token
                AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
                user.setPreference(Constants.RSS_TOKEN, rssToken);
                boss.setUserPrefs(user.getSessionId(), user.getId(),
                                  user.getPreferences());
            }
            session.setAttribute("rssToken", rssToken);

        }
        request.setAttribute(Constants.PORTAL_KEY, portal); 
        
        Map userOpsMap =
            (Map) session.getAttribute(Constants.USER_OPERATIONS_ATTR);

        if (userOpsMap.containsKey(AuthzConstants.rootOpCAMAdmin)) {
            // Now check for updates
            UpdateBoss uboss = ContextUtils.getUpdateBoss(ctx);

            try {
                RequestUtils.getStringParameter(request, "update");
                uboss.ignoreUpdate();
            } catch (ParameterNotFoundException e) {
                String update = uboss.getUpdateReport();
                if (update != null) {
                    request.setAttribute("HQUpdateReport", update);
                }
            }
        }

        return null;
    }
}
