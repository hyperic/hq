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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(value = "dashboardAdminControllerNG")
@Scope("prototype")
public class DashboardAdminControllerNG extends BaseActionNG {

	protected final Log log = LogFactory
			.getLog(DashboardAdminControllerNG.class.getName());
	
	private String ff;
	private String token;
	private String key;

	protected void setPortal(HttpServletRequest request, String title, String content) {
		Portal portal = Portal.createPortal(title, content);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
	}

	public String savedQueries() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.SQ",
				".ng.dashContent.admin.savedQueries");
		return "displaySavedQueries";
	}

	public String resourceHealth() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.RH",
				".ng.dashContent.admin.resourceHealth");
		return "displayResourceHealth";
	}

	public String recentlyApproved() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.RA",
				".ng.dashContent.admin.recentlyApproved");
		return "displayRecentlyApproved";
	}

	public String criticalAlerts() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.A",
				".ng.dashContent.admin.criticalAlerts");
		
		handleToken(token);
		
		return "displayCriticalAlerts";
	}

	public String summaryCounts() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.SC",
				".ng.dashContent.admin.summaryCounts");
		return "displaySummaryCounts";
	}

	public String autoDiscovery() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.AD",
				".ng.dashContent.admin.autoDiscovery");
		this.setHeaderResources();
		return "displayAutoDiscovery";
	}

	public String resourceHealthAddResources() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.RH.addResources",
				".ng.dashContent.admin.resourcehealth.addResources");
		return "displayResourceHealthAddResources";
	}

	public String criticalAlertsAddResources() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.A.addResources",
				".ng.dashContent.admin.criticalAlerts.addResources");
		return "displayCriticalAlertsAddResources";
	}

	public String changeLayout() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.PL",
				".ng.dashContent.admin.changeLayout");
		return "displayChangeLayout";
	}

	public String controlActions()	throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.CA",
				".ng.dashContent.admin.controlActions");
		return "displayControlActions";
	}

	public String availSummary()
			throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.AS",
				".ng.dashContent.admin.availSummary");

		handleToken(token);
		
		return "displayAvailSummary";
	}

	public String availSummaryAddResources() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.AS.addResources",
				".ng.dashContent.admin.availSummary.addResources");
		
		return "displayAvailSummaryAddResources";
	}

	public String metricViewer() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.MV",
				".ng.dashContent.admin.metricViewer");
		handleToken(token);
		return "displayMetricViewer";
	}

	public String metricViewerAddResources() throws Exception {
		setHeaderResources();
		setPortal(request, "dash.settings.PageTitle.MV.addResources",
				".ng.dashContent.admin.metricViewer.addResources");
		this.request.setAttribute("appdefType", ff);
		return "displayMetricViewerAddResources";
	}
	
	public String getFf() {
		return ff;
	}

	public void setFf(String ff) {
		this.ff = ff;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	private void handleToken(String token){
		if (token != null){
			if (token.equals("1")) {
				// This means this portlet is not a cloned portlet as part of the mutliple portlets option
				// This is an identifier of the baseline portlet, no close
				request.setAttribute("portletIdentityToken", "");
			} else {
				if (token.equals("")) {
					HttpSession session = request.getSession();
					String currentToken = (String) session.getAttribute("currentPortletToken");
					request.setAttribute("portletIdentityToken", currentToken);
				} else {
					request.setAttribute("portletIdentityToken", token);
				}
			}
		} else {
			// This means this portlet is not a cloned portlet as part of the mutliple portlets option
			HttpSession session = request.getSession();
			String currentToken = (String) session.getAttribute("currentPortletToken");
			request.setAttribute("portletIdentityToken", currentToken);
		}
	}
}
