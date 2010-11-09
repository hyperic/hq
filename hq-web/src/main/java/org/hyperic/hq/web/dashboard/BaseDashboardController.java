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

package org.hyperic.hq.web.dashboard;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.web.BaseController;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * This abstract class provides base functionality used in most dashboard
 * controllers.
 * 
 * @author David Crutchfield
 * 
 */
public abstract class BaseDashboardController extends BaseController implements ServletContextAware {
	protected final static String DELIMITER = "|";
	protected final static String PAYLOAD = "payload";

	private ConfigurationProxy configurationProxy;
	private DashboardManager dashboardManager;
	private DashboardPortletBoss dashboardPortletBoss;
	private ResourceManager resourceManager;
	private ServletContext servletContext;
	
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

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	// ...helper function to convert a delimited string to an
	// String array...
	protected String[] deconstructDelimitedStringToStringArray(
			String delimitedString) {
		if (delimitedString == null) {
			return new String[0];
		}

		if (delimitedString.startsWith(DELIMITER)) {
			delimitedString = delimitedString.substring(1);
		}

		return StringUtils.delimitedListToStringArray(delimitedString,
				DELIMITER);
	}

	// ...helper function to convert a delimited string to an
	// Integer array...
	protected Integer[] deconstructDelimitedStringToIntegerArray(
			String delimitedString) {
		String[] resourceIds = deconstructDelimitedStringToStringArray(delimitedString);
		Integer[] result = new Integer[resourceIds.length];

		// ...convert strings to integers...
		for (int x = 0; x < resourceIds.length; x++) {
			result[x] = Integer.valueOf(resourceIds[x]);
		}

		return result;
	}

	// ...helper function to convert an String array to a
	// delimited string, default behavior is to sort the values...
	protected String constructDelimitedString(String[] values) {
		return constructDelimitedString(values, true);
	}

	// ...helper function to convert an String array to a
	// delimited string, sorting is up to caller...
	protected String constructDelimitedString(String[] values, boolean sorted) {
		String result = null;

		if (values != null && values.length > 0) {
			if (sorted) {
				Arrays.sort(values);
			}

			result = DELIMITER + StringUtils.arrayToDelimitedString(values, DELIMITER);
		}

		return result;
	}

	// ...Helper function to evaluates whether or not to update a given
	// ConfigResponse key with the value provided. If the value is an
	// empty string, we clear the value...
	protected boolean compareAndUpdateSettings(HttpSession session,
			ConfigResponse configResponse, Map<String, Object> settings)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		boolean updated = false;

		for (Map.Entry<String, Object> entry : settings.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
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
			} else {
				// ...otherwise, get current preference value...
				String currentValue = configResponse.getValue(key);

				// ...otherwise compare values and update accordingly...
				if (currentValue == null || !currentValue.equals(valueString)) {
					// ...update it...
					configResponse.setValue(key, valueString);

					updated = true;
				}
			}
		}

		if (updated) {
			// ...apply defaults if available and necessary...
			ConfigResponse defaults = (ConfigResponse) servletContext.getAttribute(Constants.DEF_USER_DASH_PREFS);
			
			if (defaults != null) {
				configResponse.merge(defaults, false);
			}
			
			// ...if an update is needed, update...
			// TODO we use dashboardManager to get the portlet settings, but
			// use configurationProxy to set them
			// should combine this into a single service class...
			configurationProxy.setDashboardPreferences(session,
					getWebUser(session), configResponse);
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