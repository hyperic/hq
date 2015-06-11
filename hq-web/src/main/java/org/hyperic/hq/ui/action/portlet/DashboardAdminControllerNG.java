package org.hyperic.hq.ui.action.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.springframework.stereotype.Component;

@Component(value = "dashboardAdminControllerNG")
public class DashboardAdminControllerNG extends BaseActionNG {

	protected final Log log = LogFactory
			.getLog(DashboardAdminControllerNG.class.getName());

	private void setPortal(HttpServletRequest request, String title,
			String content) {
		Portal portal = Portal.createPortal(title, content);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);
	}

	public String savedQueries() throws Exception {
		setPortal(request, "dash.settings.PageTitle.SQ",
				".dashContent.admin.savedQueries");
		return "displaySavedQueries";
	}

	public String resourceHealth() throws Exception {
		setPortal(request, "dash.settings.PageTitle.RH",
				".dashContent.admin.resourceHealth");
		return "displayResourceHealth";
	}

	public String recentlyApproved() throws Exception {
		setPortal(request, "dash.settings.PageTitle.RA",
				".dashContent.admin.recentlyApproved");
		return "displayRecentlyApproved";
	}

	public String criticalAlerts() throws Exception {
		setPortal(request, "dash.settings.PageTitle.A",
				".dashContent.admin.criticalAlerts");
		return "displayCriticalAlerts";
	}

	public String summaryCounts() throws Exception {
		setPortal(request, "dash.settings.PageTitle.SC",
				".dashContent.admin.summaryCounts");
		return "displaySummaryCounts";
	}

	public String autoDiscovery() throws Exception {
		setPortal(request, "dash.settings.PageTitle.AD",
				".dashContent.admin.autoDiscovery");
		this.setHeaderResources();
		return "displayAutoDiscovery";
	}

	public String resourceHealthAddResources() throws Exception {
		setPortal(request, "dash.settings.PageTitle.RH.addResources",
				".dashContent.admin.resourcehealth.addResources");
		return "displayResourceHealthAddResources";
	}

	public String criticalAlertsAddResources() throws Exception {
		setPortal(request, "dash.settings.PageTitle.A.addResources",
				".dashContent.admin.criticalAlerts.addResources");
		return "displayCriticalAlertsAddResources";
	}

	public String changeLayout() throws Exception {
		setPortal(request, "dash.settings.PageTitle.PL",
				".dashContent.admin.changeLayout");
		return "displayChangeLayout";
	}

	public String controlActions()	throws Exception {
		setPortal(request, "dash.settings.PageTitle.CA",
				".dashContent.admin.controlActions");
		return "displayControlActions";
	}

	public String availSummary()
			throws Exception {
		setPortal(request, "dash.settings.PageTitle.AS",
				".dashContent.admin.availSummary");
		return "displayAvailSummary";
	}

	public String availSummaryAddResources() throws Exception {
		setPortal(request, "dash.settings.PageTitle.AS.addResources",
				".dashContent.admin.availSummary.addResources");
		return "availSummaryAddResources";
	}

	public String metricViewer() throws Exception {
		setPortal(request, "dash.settings.PageTitle.MV",
				".dashContent.admin.metricViewer");
		return "displayMetricViewer";
	}

	public String metricViewerAddResources() throws Exception {
		setPortal(request, "dash.settings.PageTitle.MV.addResources",
				".dashContent.admin.metricViewer.addResources");
		return "displayMetricViewerAddResources";
	}
}
