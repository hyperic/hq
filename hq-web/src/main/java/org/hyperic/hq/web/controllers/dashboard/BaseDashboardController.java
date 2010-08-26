/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.web.controllers.dashboard;

import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.web.controllers.BaseController;
import org.hyperic.util.config.ConfigResponse;

/**
 * This abstract class provides base functionality used in most dashboard
 * controllers.
 * 
 * @author David Crutchfield
 * 
 */
public abstract class BaseDashboardController extends BaseController {
	protected final static String DELIMITER = "|";
	protected final static String PAYLOAD = "payload";

	private ConfigurationProxy configurationProxy;
	private DashboardManager dashboardManager;
	private DashboardPortletBoss dashboardPortletBoss;
	private ResourceManager resourceManager;

	public BaseDashboardController(AppdefBoss appdefBoss, AuthzBoss authzBoss,
			ConfigurationProxy configurationProxy,
			DashboardManager dashboardManager,
			DashboardPortletBoss dashboardPortletBoss,
			ResourceManager resourceManager) {
		super(appdefBoss, authzBoss);
		
		this.configurationProxy = configurationProxy;
		this.dashboardManager = dashboardManager;
		this.dashboardPortletBoss = dashboardPortletBoss;
		this.resourceManager = resourceManager;
	}

	// ...Helper function to evaluates whether or not to update a given
	// ConfigResponse key with the value provided. If the value is an
	// empty string, we clear the value...
	protected boolean compareAndUpdate(ConfigResponse configResponse,
			String key, Object value) {
		boolean updated = false;
		String valueString = "";
		
		// ...if value is null, we automatically treat it as an empty 
		// string otherwise we convert it to a string...
		if (value != null) {
			valueString = value.toString();
		}

		// ...if value is empty, we need to unset the perference...
		if (valueString.isEmpty()) {
			// ...if value string is empty, clear the setting...
			configResponse.unsetValue(key);
			
			updated = true;
		}
		
		if (!updated) {
			// ...get current preference value...
			String currentValue = configResponse.getValue(key);
			
			// ...otherwise compare values and update accordingly...
			if (currentValue == null || !currentValue.equals(valueString)) {
				// ...update it...
				configResponse.setValue(key, valueString);
				
				updated = true;
			}
		}
		
		return updated;
	}

	protected ConfigResponse getDashboardSettings(Integer dashboardId,
			WebUser webUser) {
		// ...grab the dashbaordConfig, which leads to the ConfigResponse
		// which contains the actual settings...
		DashboardConfig dashboardConfig = getDashboardManager().findDashboard(
				dashboardId, webUser, getAuthzBoss());

		return dashboardConfig.getConfig();
	}

	protected ConfigurationProxy getConfigurationProxy() {
		return configurationProxy;
	}

	protected DashboardManager getDashboardManager() {
		return dashboardManager;
	}

	protected DashboardPortletBoss getDashboardPortletBoss() {
		return dashboardPortletBoss;
	}

	protected ResourceManager getResourceManager() {
		return resourceManager;
	}
}