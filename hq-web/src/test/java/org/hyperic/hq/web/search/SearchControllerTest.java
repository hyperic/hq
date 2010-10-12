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

package org.hyperic.hq.web.search;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.web.BaseControllerTest;
import org.hyperic.hq.web.search.SearchController;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;

public class SearchControllerTest extends BaseControllerTest {
	private SearchController controller;
	private AppdefBoss mockAppdefBoss;
	private AuthzBoss mockAuthzBoss;
	private HttpSession mockSession;

	@Before
	public void setUp() {
		// ...set up the base functionality...
		super.setUp();

		mockAppdefBoss = getMockAppdefBoss();
		mockAuthzBoss = getMockAuthzBoss();
		mockSession = getMockSession();
		controller = new SearchController(mockAppdefBoss, mockAuthzBoss);
	}

	@Test
	public void testListSearchResults() throws SessionNotFoundException,
			SessionTimeoutException, PermissionException {
		final String SEARCH_STRING = "tes";

		// ...set up our great expectations...
		expect(
				mockAuthzBoss.getSubjectsByName(eq(SESSION_ID),
						eq(SEARCH_STRING), isA(PageControl.class))).andReturn(
				constructMockAuthzSubjectsReturnValue(SEARCH_STRING));
		expect(
				mockAppdefBoss.search(eq(SESSION_ID.intValue()),
						eq(SEARCH_STRING), isA(PageControl.class))).andReturn(
				constructMockSearchResultsReturnValue(SEARCH_STRING));

		// ...replay the expectations, so we can execute the test...
		replay(mockAppdefBoss, mockAuthzBoss);

		// ...test it...
		Map<String, List<Map<String, String>>> result = controller
				.listSearchResults(SEARCH_STRING, mockSession);

		// ...verify our expectations...
		verify(mockAppdefBoss, mockAuthzBoss);

		// ...inspect the result, and make any assertions...
		assertTrue("Result should contain a key called 'resources'", result
				.containsKey("resources"));
		assertEquals("There should be 10 resources in the result", 10, result
				.get("resources").size());
		assertTrue("Result should contain a key called 'users'", result
				.containsKey("users"));
		assertEquals("There should be 5 users in the result", 5, result.get(
				"users").size());
	}

	private PageList<SearchResult> constructMockSearchResultsReturnValue(
			String root) {
		PageList<SearchResult> results = new PageList<SearchResult>();

		for (int x = 0; x < 10; x++) {
			results
					.add(new SearchResult(root + "_" + x, null, "resource_" + x));
		}

		return results;
	}

	private PageList<AuthzSubject> constructMockAuthzSubjectsReturnValue(
			String root) {
		PageList<AuthzSubject> results = new PageList<AuthzSubject>();

		for (int x = 0; x < 5; x++) {
			AuthzSubject subject = new AuthzSubject(true, null, null, null,
					false, "Test_" + x, "User_" + x, "testuser_" + x, null,
					null, false);

			subject.setId(x);

			results.add(subject);
		}

		return results;
	}
}