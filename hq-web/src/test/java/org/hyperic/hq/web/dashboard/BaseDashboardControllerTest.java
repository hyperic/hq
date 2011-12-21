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

import org.easymock.EasyMock;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
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
    private ResourceGroupManager mockResourceGroupManager;

	protected void setUp() {
		super.setUp();
		
		mockConfigurationProxy = EasyMock.createMock(ConfigurationProxy.class);
		mockDashboardManager = EasyMock.createMock(DashboardManager.class);
		mockDashboardPortletBoss = EasyMock.createMock(DashboardPortletBoss.class);
		mockResourceManager = EasyMock.createMock(ResourceManager.class);
        mockResourceGroupManager = EasyMock.createMock(ResourceGroupManager.class);
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

    protected ResourceGroupManager getMockResourceGroupManager() {
        return mockResourceGroupManager;
    }
}