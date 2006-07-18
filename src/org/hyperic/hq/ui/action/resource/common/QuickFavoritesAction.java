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

package org.hyperic.hq.ui.action.resource.common;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class QuickFavoritesAction extends BaseAction {

    private static final Log log
        = LogFactory.getLog(QuickFavoritesPrepareAction.class.getName());

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        WebUser user = SessionUtils.getWebUser(request.getSession());
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        String mode   = request.getParameter(Constants.MODE_PARAM);

        HashMap forwardParams = new HashMap(2);
        forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());

        if (mode == null) return returnFailure(request, mapping, forwardParams);

        Boolean isFavorite = QuickFavoritesUtil.isFavorite(user, aeid);
        if (mode.equals(Constants.MODE_ADD)) {
            // Is this already in the favorites list?  Should not happen
            if (isFavorite.booleanValue()) {
                // Just return, it's already there
                return returnSuccess(request, mapping, forwardParams, 
                                     BaseAction.YES_RETURN_PATH);
            }
            // Add to favorites and save
            DashboardUtils.addEntityToPreferences(
                Constants.USERPREF_KEY_FAVORITE_RESOURCES, user, aeid,
                Integer.MAX_VALUE);
        } else if (mode.equals(Constants.MODE_REMOVE) ) {
            // Is this not in the favorites list?  Should not happen
            if (!isFavorite.booleanValue()) {
                // Already removed, just return
                return returnSuccess(request, mapping, forwardParams, 
                                     BaseAction.YES_RETURN_PATH);
            }
            // Remove from favorites and save
            DashboardUtils
                .removeResources(new String[] { aeid.getAppdefKey() },
                                 Constants.USERPREF_KEY_FAVORITE_RESOURCES,
                                 user);
        } else {
            // Not an add or remove, what the heck is it?  It's an error.
            return returnFailure(request, mapping, forwardParams);
        }

        boss.setUserPrefs(user.getSessionId(), user.getId(), 
                          user.getPreferences());
        return returnSuccess(request, mapping, forwardParams, 
                             BaseAction.YES_RETURN_PATH);
    }
}
