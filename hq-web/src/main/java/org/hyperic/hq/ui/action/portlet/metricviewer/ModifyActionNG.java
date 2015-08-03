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

package org.hyperic.hq.ui.action.portlet.metricviewer;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("metricViewerModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements
		ModelDriven<PropertiesFormNG> {

	private final Log log = LogFactory.getLog(ModifyActionNG.class);
	
	@Resource
	private ConfigurationProxy configurationProxy;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;

	PropertiesFormNG pForm = new PropertiesFormNG();

	public String update() throws Exception {

		HttpSession session = request.getSession();
		WebUser user = RequestUtils.getWebUser(request);

		String forwardStr = SUCCESS;
		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);
		ConfigResponse dashPrefs = dashConfig.getConfig();

		if (pForm.isRemoveClicked()) {
			DashboardUtils.removeResources(pForm.getIds(),
					PropertiesFormNG.RESOURCES, dashPrefs);
			configurationProxy
					.setDashboardPreferences(session, user, dashPrefs);
			forwardStr = "review";
		}

        String forward = checkSubmit(pForm);
		
        if (forward != null) {
            return forward;
        }

        String token = (String) session.getAttribute("currentPortletToken");

		// For multi-portlet configuration
		String numKey = PropertiesFormNG.NUM_TO_SHOW;
		String resKey = PropertiesFormNG.RESOURCES;
		String resTypeKey = PropertiesFormNG.RES_TYPE;
		String metricKey = PropertiesFormNG.METRIC;
		String descendingKey = PropertiesFormNG.DECSENDING;
		String titleKey = PropertiesFormNG.TITLE;

		if (token != null) {
			numKey += token;
			resKey += token;
			resTypeKey += token;
			metricKey += token;
			descendingKey += token;
			titleKey += token;
		}

		Integer numberToShow = pForm.getNumberToShow();
		String resourceType = pForm.getResourceType();
		String metric = pForm.getMetric();
		String descending = pForm.getDescending();

		// If the selected resource type does not match the previous value,
		// clear out the resources
		try {
			if (!resourceType.equals(dashPrefs.getValue(resTypeKey))) {
				dashPrefs.setValue(resKey, "");
			}
		} catch (InvalidOptionException e) {
			log.error(e);
			// Ok, not set yet..
		}

		dashPrefs.setValue(resTypeKey, resourceType);
		dashPrefs.setValue(numKey, numberToShow.toString());
		dashPrefs.setValue(metricKey, metric);
		dashPrefs.setValue(descendingKey, descending);
		dashPrefs.setValue(titleKey, pForm.getTitle());

		configurationProxy.setDashboardPreferences(session, user, dashPrefs);

		session.removeAttribute(Constants.USERS_SES_PORTAL);

		if (!pForm.isOkClicked()) {
			forwardStr = "review";
			return forwardStr;
		}
		
        removeValueInSession("currentPortletKey");
        removeValueInSession("currentPortletToken"); 
        
		return forwardStr;

	}

	@SkipValidation
	public String cancel() throws Exception {
		clearErrorsAndMessages();
		clearCustomErrorMessages();
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		pForm.reset();
		clearErrorsAndMessages();
		clearCustomErrorMessages();
		return "reset";
	}

	public PropertiesFormNG getModel() {
		return pForm;
	}

	public PropertiesFormNG getPForm() {
		return pForm;
	}

	public void setPForm(PropertiesFormNG pForm) {
		this.pForm = pForm;
	}
}
