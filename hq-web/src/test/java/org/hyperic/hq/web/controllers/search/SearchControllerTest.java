package org.hyperic.hq.web.controllers.search;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import static org.easymock.EasyMock.*;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.web.controllers.SessionParameterKeys;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

public class SearchControllerTest {
	private final Integer SESSION_ID = 1;
	private final Integer AUTHZ_SUBJECT_VALUE_ID = 1;

	private AppdefBoss mockAppdefBoss;
	private AuthzBoss mockAuthzBoss;

	private SearchController controller;
	private HttpSession session;

	@Before
	public void setUp() {
		// Set up a mock authz subect value object...
		AuthzSubjectValue subject = new AuthzSubjectValue();

		subject.setId(AUTHZ_SUBJECT_VALUE_ID);

		// ...so we can create a web user object...
		WebUser webUser = new WebUser(subject);

		webUser.setSessionId(SESSION_ID);

		// ...and put it into the mock http session. We'll use this session
		// during testing...
		session = new MockHttpSession();

		session.setAttribute(SessionParameterKeys.WEB_USER, webUser);

		// ...now set up the mock services for this controller...
		mockAppdefBoss = createMock(AppdefBoss.class);
		mockAuthzBoss = createMock(AuthzBoss.class);

		controller = new SearchController(mockAppdefBoss, mockAuthzBoss);
	}

	@Test
	public void testListSearchResults() throws SessionNotFoundException,
			SessionTimeoutException, PermissionException {
		final String SEARCH_STRING = "tes";

		// ...set up our great expectations...
		expect(
				mockAuthzBoss.getSubjectsByName(eq(SESSION_ID), eq(SEARCH_STRING),
						isA(PageControl.class))).andReturn(
				constructMockAuthzSubjectsReturnValue(SEARCH_STRING));
		expect(
				mockAppdefBoss.search(eq(SESSION_ID.intValue()), eq(SEARCH_STRING),
						isA(PageControl.class))).andReturn(
				constructMockSearchResultsReturnValue(SEARCH_STRING));

		// ...replay the events, so we can execute the test...
		replay(mockAppdefBoss, mockAuthzBoss);

		// ...test it...
		Map<String, List<Map<String, String>>> result = controller
				.listSearchResults(SEARCH_STRING, session);

		// ...verify the results...
		verify(mockAppdefBoss, mockAuthzBoss);
		
		// ...inspect the result, and make any assertions...
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

		for (int x = 0; x < 10; x++) {
			AuthzSubject subject = new AuthzSubject(true, null, null, null,
					false, "Test_" + x, "User_" + x, "testuser_" + x, null,
					null, false);

			subject.setId(x);

			results.add(subject);
		}

		return results;
	}
}