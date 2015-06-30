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

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("quickFavoritesPrepareActionNG")
public class QuickFavoritesPrepareActionNG extends BaseActionNG implements
		ViewPreparer {

	private final Log log = LogFactory.getLog(getClass());

	@Autowired
	private AuthzBoss authzBoss;
	@Autowired
	private DashboardManager dashboardManager;
	@Autowired
	private SessionManager sessionManager;

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		try {
			WebUser user = RequestUtils.getWebUser(getServletRequest());

			Boolean isFavorite = Boolean.FALSE;
			Attribute attribute = attributeContext.getAttribute("resource");
			if (attribute != null) {
				AppdefResourceValue arv = (AppdefResourceValue) attribute
						.getValue();

				// check our preferences to see if this resource is in there.

				ConfigResponse dashConfig = DashboardUtils
						.findUserDashboardConfig(user, dashboardManager,
								sessionManager);
				isFavorite = QuickFavoritesUtil.isFavorite(dashConfig,
						arv.getEntityId());

				getServletRequest().setAttribute(Constants.ENTITY_ID_PARAM,
						arv.getEntityId().getAppdefKey());
				getServletRequest().setAttribute(Constants.IS_FAVORITE_PARAM,
						isFavorite);

				List<Dashboard> editableDashboards = dashboardManager
						.findEditableDashboards(user, authzBoss);

				getServletRequest()
						.setAttribute(Constants.EDITABLE_DASHBOARDS_PARAM,
								editableDashboards);
				getServletRequest().setAttribute(
						Constants.HAS_MULTIPLE_DASHBOARDS_PARAM,
						editableDashboards.size() > 1);
			}
		} catch (ServletException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		}

	}
}
