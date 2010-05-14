/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class QuickFavoritesPrepareAction extends WorkflowPrepareAction {

	private AuthzBoss authzBoss;
	private DashboardManager dashboardManager;
	private SessionManager sessionManager;

	@Autowired
	public QuickFavoritesPrepareAction(AuthzBoss authzBoss,
			DashboardManager dashboardManager, SessionManager sessionManager) {
		this.authzBoss = authzBoss;
		this.dashboardManager = dashboardManager;
		this.sessionManager = sessionManager;
	}

	public ActionForward workflow(ComponentContext context,
			ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		WebUser user = RequestUtils.getWebUser(request);
		Boolean isFavorite = Boolean.FALSE;
		AppdefResourceValue arv = (AppdefResourceValue) context
				.getAttribute("resource");

		// check our preferences to see if this resource is in there.

		ConfigResponse dashConfig = DashboardUtils.findUserDashboardConfig(
				user, dashboardManager, sessionManager);
		isFavorite = QuickFavoritesUtil.isFavorite(dashConfig, arv
				.getEntityId());

		request.setAttribute(Constants.ENTITY_ID_PARAM, arv.getEntityId()
				.getAppdefKey());
		request.setAttribute(Constants.IS_FAVORITE_PARAM, isFavorite);

		List<Dashboard> editableDashboards = dashboardManager
				.findEditableDashboards(user, authzBoss);

		request.setAttribute(Constants.EDITABLE_DASHBOARDS_PARAM,
				editableDashboards);
		request.setAttribute(Constants.HAS_MULTIPLE_DASHBOARDS_PARAM,
				editableDashboards.size() > 1);

		return null;
	}
}
