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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;

public class DashboardControllerTest extends BaseDashboardControllerTest {
	private AuthzBoss mockAuthzBoss;
	private ConfigurationProxy mockConfigurationProxy;
	private DashboardManager mockDashboardManager;
	private List<String> mockMulitplePortletsList;
	private DashboardController controller;

	@Before
	public void setUp() {
		super.setUp();

		mockMulitplePortletsList = new ArrayList<String>();

		mockMulitplePortletsList.add("portlet1");
		mockMulitplePortletsList.add("portlet2");
		mockMulitplePortletsList.add("portlet4");
		mockMulitplePortletsList.add("portlet5");

		mockAuthzBoss = getMockAuthzBoss();
		mockConfigurationProxy = getMockConfigurationProxy();
		mockDashboardManager = getMockDashboardManager();
		controller = new DashboardController(mockAuthzBoss,
				mockConfigurationProxy, mockDashboardManager);

		controller.setMultiplePortletsList(mockMulitplePortletsList);
		controller.setServletContext(new MockServletContext());
	}

	@Test
	public void testAddPortletToDashboardHasPortletsNewSinglePortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet100";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet2|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet2|portlet3|portlet100";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingSinglePortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet2|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet2|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.FALSE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardEmptyPortletsNewSinglePortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "";
		final String EXPECTED_RESULT = "|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardNullPortletsNewSinglePortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = null;
		final String EXPECTED_RESULT = "|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsNewMultiPortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet3|portlet1|portlet2";
		final String EXPECTED_RESULT = "|portlet3|portlet1|portlet2|portlet4";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_2";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletX2SequentialWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3|portlet4_2";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_2|portlet4_3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletX2NonSequentialWide()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.TRUE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3|portlet4_4|portlet4_2";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_4|portlet4_2|portlet4_3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsNewSinglePortletNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet100";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet2|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet2|portlet3|portlet100";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingSinglePortletNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet2|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet2|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.FALSE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardEmptyPortletsNewSinglePortletNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "";
		final String EXPECTED_RESULT = "|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardNullPortletsNewSinglePortletNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet3";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = null;
		final String EXPECTED_RESULT = "|portlet3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsNewMultiPortletNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet3|portlet1|portlet2";
		final String EXPECTED_RESULT = "|portlet3|portlet1|portlet2|portlet4";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletNArrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_2";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletX2SequentialNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3|portlet4_2";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_2|portlet4_3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testAddPortletToDashboardHasPortletsExistingMultiPortletX2NonSequentialNarrow()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet4";
		final Boolean IS_PORTLET_WIDE = Boolean.FALSE;
		final String DASHBOARD_PORTLETS = "|portlet1|portlet4|portlet3|portlet4_4|portlet4_2";
		final String EXPECTED_RESULT = "|portlet1|portlet4|portlet3|portlet4_4|portlet4_2|portlet4_3";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testAddPortletToDashboard(DASHBOARD_ID, PORTLET_NAME, IS_PORTLET_WIDE,
				DASHBOARD_PORTLETS, EXPECTED_RESULT, EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveWidePortletFromDashboardPortletExistsNoSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet2|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet8|portlet7|portlet6");
		
		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet2|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet7|portlet6";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, null,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveNarrowPortletFromDashboardPortletExistsNoSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet8|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet5|portlet7|portlet6");
		
		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet5|portlet7|portlet6";
		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, null,
				EXPECTED_TO_UPDATE);
	}


	@Test
	public void testRemoveWidePortletFromDashboardPortletExistsSingleWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet2|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet8|portlet7|portlet6");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet2", "setting");
		SETTINGS.put("portlet3", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet7", "setting");
		SETTINGS.put("portlet6", "setting");
		
		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet2|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet7|portlet6";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet2");
		EXPECTED_SETTING_KEYS.add("portlet3");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet6");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveNarrowPortletFromDashboardPortletExistsSingleWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet8|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet5|portlet7|portlet6");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet5", "setting");
		SETTINGS.put("portlet3", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet7", "setting");
		SETTINGS.put("portlet6", "setting");
		

		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet5|portlet7|portlet6";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet5");
		EXPECTED_SETTING_KEYS.add("portlet3");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet6");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveWidePortletFromDashboardPortletExistsMultipleWithTokenWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8_2";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet2|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet8_2|portlet7|portlet8|portlet8_3");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet2", "setting");
		SETTINGS.put("portlet3", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet8_2", "setting");
		SETTINGS.put("portlet8_3", "setting");
		SETTINGS.put("portlet7", "setting");

		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet2|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet7|portlet8|portlet8_3";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet2");
		EXPECTED_SETTING_KEYS.add("portlet3");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet8");
		EXPECTED_SETTING_KEYS.add("portlet8_3");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveNarrowPortletFromDashboardPortletExistsMultipleWithTokenWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8_2";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet8_2|portlet8|portlet8_3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet5|portlet7|portlet6");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet5", "setting");
		SETTINGS.put("portlet6", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet8_2", "setting");
		SETTINGS.put("portlet8_3", "setting");
		SETTINGS.put("portlet7", "setting");

		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet8|portlet8_3";
		final String EXPECTED_WIDE_RESULT = "|portlet5|portlet7|portlet6";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet5");
		EXPECTED_SETTING_KEYS.add("portlet6");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet8");
		EXPECTED_SETTING_KEYS.add("portlet8_3");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveWidePortletFromDashboardPortletExistsMultipleWithoutTokenWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet2|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet8_2|portlet7|portlet8|portlet8_3");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet2", "setting");
		SETTINGS.put("portlet3", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet8_2", "setting");
		SETTINGS.put("portlet8_3", "setting");
		SETTINGS.put("portlet7", "setting");

		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet2|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet8_2|portlet7|portlet8_3";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet2");
		EXPECTED_SETTING_KEYS.add("portlet3");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet8_2");
		EXPECTED_SETTING_KEYS.add("portlet8_3");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemoveNarrowPortletFromDashboardPortletExistsMultipleWithoutTokenWithSettings()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet8_2|portlet8|portlet8_3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet5|portlet7|portlet6");
		SETTINGS.put("portlet1", "setting");
		SETTINGS.put("portlet5", "setting");
		SETTINGS.put("portlet6", "setting");
		SETTINGS.put("portlet8", "setting");
		SETTINGS.put("portlet8_2", "setting");
		SETTINGS.put("portlet8_3", "setting");
		SETTINGS.put("portlet7", "setting");

		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet8_2|portlet8_3";
		final String EXPECTED_WIDE_RESULT = "|portlet5|portlet7|portlet6";
		final List<String> EXPECTED_SETTING_KEYS = new ArrayList<String>();
		
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.NARROW_PORTLETS);
		EXPECTED_SETTING_KEYS.add(UserPreferenceKeys.WIDE_PORTLETS);
		EXPECTED_SETTING_KEYS.add("portlet1");
		EXPECTED_SETTING_KEYS.add("portlet5");
		EXPECTED_SETTING_KEYS.add("portlet6");
		EXPECTED_SETTING_KEYS.add("portlet7");
		EXPECTED_SETTING_KEYS.add("portlet8_2");
		EXPECTED_SETTING_KEYS.add("portlet8_3");

		final Boolean EXPECTED_TO_UPDATE = Boolean.TRUE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS,
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, EXPECTED_SETTING_KEYS,
				EXPECTED_TO_UPDATE);
	}

	@Test
	public void testRemovePortletFromDashboardPortletDoesNotExist()
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// ...setup inputs and outputs...
		final Integer DASHBOARD_ID = 10000;
		final String PORTLET_NAME = "portlet8";
		final Map<String, String> SETTINGS = new HashMap<String, String>();
		
		SETTINGS.put(UserPreferenceKeys.NARROW_PORTLETS, "|portlet1|portlet2|portlet3");
		SETTINGS.put(UserPreferenceKeys.WIDE_PORTLETS, "|portlet5|portlet7|portlet6");
		
		final String EXPECTED_NARROW_RESULT = "|portlet1|portlet2|portlet3";
		final String EXPECTED_WIDE_RESULT = "|portlet5|portlet7|portlet6";
		final Boolean EXPECTED_TO_UPDATE = Boolean.FALSE;

		testRemovePortletFromDashboard(DASHBOARD_ID, PORTLET_NAME, SETTINGS, 
				EXPECTED_WIDE_RESULT, EXPECTED_NARROW_RESULT, null,
				EXPECTED_TO_UPDATE);
	}

	private void testRemovePortletFromDashboard(Integer dashboardId,
			String portletName, Map<String, String> dashboardSettings,
			String expectedWideResult, String expectedNarrowResult, 
			List<String> expectedSettingKeys, Boolean expectedToUpdate)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException {
		// Change one of the settings to trigger the conditional logic...
		ConfigResponse configResponse = new ConfigResponse(dashboardSettings);
		DashboardConfig dashboardConfig = constructDashboardConfig(configResponse);

		// ...setup our great expectations...
		expect(
				mockDashboardManager.findDashboard(eq(dashboardId),
						isA(WebUser.class), isA(AuthzBoss.class))).andReturn(
				dashboardConfig);

		if (expectedToUpdate) {
			mockConfigurationProxy.setDashboardPreferences(
					isA(HttpSession.class), isA(WebUser.class),
					isA(ConfigResponse.class));

			expectLastCall();
		}

		// ...replay those expectations...
		replay(mockDashboardManager, mockConfigurationProxy);

		// ...test it...
		String result = controller.removePortletFromDashboard(dashboardId,
				portletName, getMockSession());

		// ...verify our expectations...
		verify(mockDashboardManager, mockConfigurationProxy);

		// ...check the result...
		assertEquals("redirect:/app/dashboard/" + dashboardId + "/portlets", result);
		assertEquals(configResponse
				.getValue(UserPreferenceKeys.NARROW_PORTLETS),
				expectedNarrowResult);
		assertEquals(configResponse.getValue(UserPreferenceKeys.WIDE_PORTLETS),
				expectedWideResult);
		
		if (expectedSettingKeys != null) {
			Set<String> settingKeys = configResponse.getKeys();
			
			assertEquals(settingKeys.size(), expectedSettingKeys.size());
			
			for (String settingKey : expectedSettingKeys) {
				assertTrue("Should contain settings key [" + settingKey + "]", settingKeys.contains(settingKey));
			}
		}
	}

	private void testAddPortletToDashboard(Integer dashboardId,
			String portletName, Boolean isWide,
			String delimitedDashboardPortlets, String expectedResult,
			Boolean expectedToUpdate) throws SessionNotFoundException,
			SessionTimeoutException, PermissionException {
		// Change one of the settings to trigger the conditional logic...
		String userPreferenceKey = (isWide) ? UserPreferenceKeys.WIDE_PORTLETS
				: UserPreferenceKeys.NARROW_PORTLETS;
		ConfigResponse configResponse = new ConfigResponse();

		configResponse.setValue(userPreferenceKey, delimitedDashboardPortlets);

		DashboardConfig dashboardConfig = constructDashboardConfig(configResponse);

		// ...setup our great expectations...
		expect(
				mockDashboardManager.findDashboard(eq(dashboardId),
						isA(WebUser.class), isA(AuthzBoss.class))).andReturn(
				dashboardConfig);

		if (expectedToUpdate) {
			mockConfigurationProxy.setDashboardPreferences(
					isA(HttpSession.class), isA(WebUser.class),
					isA(ConfigResponse.class));

			expectLastCall();
		}

		// ...replay those expectations...
		replay(mockDashboardManager, mockConfigurationProxy);

		// ...test it...
		String result = controller.addPortletToDashboard(dashboardId,
				portletName, isWide, getMockSession());

		// ...verify our expectations...
		verify(mockDashboardManager, mockConfigurationProxy);

		// ...check the result...
		assertEquals("redirect:/Dashboard.do", result);
		assertEquals(configResponse.getValue(userPreferenceKey), expectedResult);
	}
}