package org.hyperic.hq.ui.action.portlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.SessionUtils;

public class DisplayPortletAction extends TilesAction {
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception 
	{
		ServletContext ctx = getServlet().getServletContext();
        HttpSession session = request.getSession();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
		WebUser user = SessionUtils.getWebUser(session);
		DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
		AuthzSubject guestUser = boss.findSubjectByName(user.getSessionId(), "guest");
		DashboardConfig dashboardConfig = dashManager.getUserDashboard(guestUser, guestUser);
			
		String portletId = request.getParameter("pid");
		Portlet portlet = new Portlet(portletId);

		session.setAttribute("portlet", portlet);
		session.setAttribute(Constants.SELECTED_DASHBOARD_ID, dashboardConfig.getId());
		
		return super.execute(mapping, form, request, response);
	}
}
