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

package org.hyperic.hq.ui.action.portlet.summaryCounts;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("summaryCountsModifyActionNG")
@Scope("prototype")
public class ModifyActionNG extends BaseActionNG implements ModelDriven<PropertiesFormNG>{
	@Resource
	private ConfigurationProxy configurationProxy;
	@Resource
	private AuthzBoss authzBoss;
	@Resource
	private DashboardManager dashboardManager;
    PropertiesFormNG pForm=new PropertiesFormNG();
	
    public String execute()
			throws Exception {

		HttpSession session = request.getSession();

		String forward = checkSubmit(pForm);

		if (forward != null) {
			return forward;
		}

		WebUser user = RequestUtils.getWebUser(request);
		DashboardConfig dashConfig = dashboardManager
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), user,
						authzBoss);
		ConfigResponse dashPrefs = dashConfig.getConfig();

		String application = Boolean.toString(pForm.isApplication());
		String platform = Boolean.toString(pForm.isPlatform());
		String cluster = Boolean.toString(pForm.isCluster());
		String server = Boolean.toString(pForm.isServer());
		String service = Boolean.toString(pForm.isService());

		dashPrefs.setValue(".ng.dashContent.summaryCounts.application",
				application);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.platform", platform);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.group.cluster", cluster);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.server", server);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.service", service);

		String groupMixed = Boolean.toString(pForm.isGroupMixed());
		String groupGroups = Boolean.toString(pForm.isGroupGroups());
		String groupPlatServerService = Boolean.toString(pForm
				.isGroupPlatServerService());
		String groupApplication = Boolean.toString(pForm.isGroupApplication());    

		dashPrefs
				.setValue(".ng.dashContent.summaryCounts.group.mixed", groupMixed);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.group.groups",
				groupGroups);
		dashPrefs.setValue(
				".ng.dashContent.summaryCounts.group.plat.server.service",
				groupPlatServerService);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.group.application",
				groupApplication);

		String applicationTypes = StringUtil.arrayToString(pForm
				.getApplicationTypes());
		String platformTypes = StringUtil.arrayToString(pForm
				.getPlatformTypes());
		String clusterTypes = StringUtil.arrayToString(pForm.getClusterTypes());
		String serverTypes = StringUtil.arrayToString(pForm.getServerTypes());
		String serviceTypes = StringUtil.arrayToString(pForm.getServiceTypes());

		dashPrefs.setValue(".ng.dashContent.summaryCounts.serviceTypes",
				serviceTypes);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.serverTypes",
				serverTypes);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.group.clusterTypes",
				clusterTypes);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.platformTypes",
				platformTypes);
		dashPrefs.setValue(".ng.dashContent.summaryCounts.applicationTypes",
				applicationTypes);

		configurationProxy.setDashboardPreferences(session, user, dashPrefs);
		LogFactory.getLog("user.preferences").trace(
				"Invoking setUserPrefs" + " in summaryCounts/ModifyAction "
						+ " for " + user.getId() + " at "
						+ System.currentTimeMillis() + " user.prefs = "
						+ dashPrefs.getKeys().toString());
		session.removeAttribute(Constants.USERS_SES_PORTAL);
		return SUCCESS;

	}
	
	@SkipValidation
    public String cancel() throws Exception {
        clearErrorsAndMessages();
        return "cancel";
    }

    @SkipValidation
    public String reset() throws Exception {
    	pForm.reset();
        clearErrorsAndMessages();
        return "reset";
    }

	public PropertiesFormNG getModel() {
		// TODO Auto-generated method stub
		return pForm;
	}

}
