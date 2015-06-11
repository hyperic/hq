package org.hyperic.hq.ui.action.portlet.addcontent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.tiles.Attribute;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.escalation.crud.SaveActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.stereotype.Component;

@Component("addContentViewActionNG")
public class ViewActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(ViewActionNG.class);

	@Resource
	private AuthzBoss authzBoss;

	@Resource
	private DashboardManager dashboardManager;

	public void execute(TilesRequestContext reqContext,
			AttributeContext attrContext) {
		// TODO Auto-generated method stub
		try {
			List<Attribute> portlets = (List<Attribute>) attrContext.getAttribute("portlets").getValue();
			HttpServletRequest request = ServletActionContext.getRequest();
			WebUser user = null;

			user = RequestUtils.getWebUser(request);

			List<String> availablePortlets = new ArrayList<String>();
			String userPortlets = new String();
			Boolean wide = new Boolean((String) attrContext
					.getAttribute("wide").getValue());
			HttpSession session = request.getSession();

			DashboardConfig dashConfig = dashboardManager.findDashboard(
					(Integer) session
							.getAttribute(Constants.SELECTED_DASHBOARD_ID),
					user, authzBoss);
			ConfigResponse dashPrefs = dashConfig.getConfig();
			List<String> multi;

			if (wide.booleanValue()) {
				userPortlets = dashPrefs
						.getValue(Constants.USER_PORTLETS_SECOND);
				multi = (List<String>) attrContext.getAttribute("multi.wide").getValue();
				session.setAttribute("multi.wide", multi);
			} else {
				userPortlets = dashPrefs
						.getValue(Constants.USER_PORTLETS_FIRST);
				multi = (List<String>) attrContext.getAttribute("multi.narrow").getValue();
				session.setAttribute("multi.narrow", multi);
			}

			// Populate available portlets list...
			for (Attribute portlet : portlets) {
				// Add portlet to the list if...
				String protletName = (String) portlet.getValue();
				// ...user doesn't have any portlets, or...
				if (userPortlets == null ||
				// ...user doesn't have this particular portlet, or...
						userPortlets.indexOf(protletName) == -1 ||
						// ...this portlet can be added more than once
						(multi != null && multi.contains(protletName))) {
					availablePortlets.add(protletName);
				}
			}

			attrContext.putAttribute("availablePortlets", new Attribute(availablePortlets));
		} catch (Exception ex) {
			log.equals(ex);
			return;
		}
	}
}
