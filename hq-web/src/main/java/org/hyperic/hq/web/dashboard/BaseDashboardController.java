package org.hyperic.hq.web.dashboard;

import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.web.BaseController;
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
		boolean result = false;

		if (value != null) {
			// ...get current preference value...
			String currentValue = configResponse.getValue(key);

			// ...compare values...
			if (currentValue != null) {
				// ...convert value to string...
				String valueString = value.toString();

				if (valueString.isEmpty()) {
					// ...if value string is empty, clear the setting...
					configResponse.unsetValue(key);
				} else if (!currentValue.equals(valueString)) {
					// ...otherwise, update it...
					configResponse.setValue(key, valueString);
				}

				result = true;
			}
		}

		return result;
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