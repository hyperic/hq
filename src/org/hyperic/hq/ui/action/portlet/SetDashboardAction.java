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

package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;

public class SetDashboardAction extends org.hyperic.hq.ui.action.BaseAction {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	
		HttpSession session = request.getSession(false);
		DashboardForm dForm = (DashboardForm) form;
		WebUser user = (WebUser) request.getSession().getAttribute(
				Constants.WEBUSER_SES_ATTR);
		AuthzBoss authzBoss = ContextUtils.getAuthzBoss(request.getSession()
				.getServletContext());
		if (!isPropertyEmpty(dForm.getSelectedDashboardId())) {
			//assign a selected dashboard
			session.setAttribute(Constants.SELECTED_DASHBOARD_ID, 
					new Integer(dForm.getSelectedDashboardId()));
		}
		if (!isPropertyEmpty(dForm.getDefaultDashboard())) {
			user.setPreference(Constants.DEFAULT_DASHBOARD_ID, dForm
					.getDefaultDashboard());
			session.setAttribute(Constants.SELECTED_DASHBOARD_ID,
					dForm.getDefaultDashboard());
			authzBoss.setUserPrefs(user.getSessionId(), user.getSubject()
					.getId(), user.getPreferences());
		}
		return mapping.findForward(Constants.SUCCESS_URL);
	}
	
	private boolean isPropertyEmpty(String property) {
		if (property == null) {
			return true;
		} else if (property.equals("")) {
			return true;
		} else
			return false;
	}
}
