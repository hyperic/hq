package org.hyperic.hq.web.dashboard;

import org.easymock.EasyMock;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.web.BaseControllerTest;
import org.hyperic.util.config.ConfigResponse;

public abstract class BaseDashboardControllerTest extends BaseControllerTest {
	private ConfigurationProxy mockConfigurationProxy;
	private DashboardManager mockDashboardManager;
	private DashboardPortletBoss mockDashboardPortletBoss;
	private ResourceManager mockResourceManager;

	protected void setUp() {
		super.setUp();
		
		mockConfigurationProxy = EasyMock.createMock(ConfigurationProxy.class);
		mockDashboardManager = EasyMock.createMock(DashboardManager.class);
		mockDashboardPortletBoss = EasyMock.createMock(DashboardPortletBoss.class);
		mockResourceManager = EasyMock.createMock(ResourceManager.class);
	}

	protected AppdefResourcePermissions constructAppdefResourcePermissions() {
		return new AppdefResourcePermissions(null, null, true, true, true,
				true, true, true, true);
	}

	protected DashboardConfig constructDashboardConfig(
			ConfigResponse configResponse) {
		return new MockDashboardConfig(configResponse);
	}

	// Mock dashboard config that sets up a config response of our choosing...
	protected class MockDashboardConfig extends DashboardConfig {
		private ConfigResponse configResponse;

		public MockDashboardConfig(ConfigResponse configResponse) {
			this.configResponse = configResponse;
		}

		public boolean isEditable(AuthzSubject subject) {
	        return true;
	    }
		
		@Override
		public ConfigResponse getConfig() {
			return configResponse;
		}
	}
	
	protected ConfigurationProxy getMockConfigurationProxy() {
		return mockConfigurationProxy;
	}

	protected DashboardManager getMockDashboardManager() {
		return mockDashboardManager;
	}

	protected DashboardPortletBoss getMockDashboardPortletBoss() {
		return mockDashboardPortletBoss;
	}

	protected ResourceManager getMockResourceManager() {
		return mockResourceManager;
	}
}