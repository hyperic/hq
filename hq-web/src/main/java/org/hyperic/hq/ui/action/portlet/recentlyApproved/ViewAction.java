/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import java.util.ArrayList;
import java.util.List;

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
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An <code>Action</code> that loads the <code>Portal</code> identified by the
 * <code>PORTAL_PARAM</code> request parameter (or the default portal, if the
 * parameter is not specified) into the <code>PORTAL_KEY</code> request
 * attribute.
 */
public class ViewAction
    extends TilesAction {

    private final Log log = LogFactory.getLog(ViewAction.class.getName());
    private AppdefBoss appdefBoss;
    private AuthzBoss authzBoss;
    private DashboardManager dashboardManager;

    @Autowired
    public ViewAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, DashboardManager dashboardManager) {
        super();
        this.appdefBoss = appdefBoss;
        this.authzBoss = authzBoss;
        this.dashboardManager = dashboardManager;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        int sessionId = user.getSessionId().intValue();

        Integer range = new Integer(dashPrefs.getValue(PropertiesForm.RANGE));

        try {
            // Hard code to look for platforms created in the last two days
            List<PlatformValue> platforms = appdefBoss.findRecentPlatforms(sessionId, 2 * MeasurementConstants.DAY,
                range.intValue());
            context.putAttribute("recentlyAdded", platforms);
        } catch (Exception e) {
            List<PlatformValue> emptyList = new ArrayList<PlatformValue>();
            context.putAttribute("recentlyApproved", emptyList);
            log.debug("Error getting recent platforms: " + e.getMessage(), e);
        }

        // Store the current time in request
        request.setAttribute("current", new Long(System.currentTimeMillis()));
        return null;
    }
}
